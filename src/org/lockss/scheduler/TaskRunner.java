/*
 * $Id: TaskRunner.java,v 1.13 2004-02-09 22:10:58 tlipkis Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

// tk - todo
// backgroundLoadFactor is adjusted incrementally; roundoff error will cause
//   long term drift.  (hack: reset to min when backgroundTasks empty.)
// OVERHEAD_LOAD_FACTOR (here and in Schedulers (or in Schedule?))
// don't let expired in acceptedTasks prevent new schedule

package org.lockss.scheduler;
import java.io.*;
import java.util.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.util.*;

class TaskRunner implements Serializable {
  static final String PREFIX = Configuration.PREFIX + "taskRunner.";
  static final String PARAM_PRIORITY = PREFIX + "priority";
  static final int DEFAULT_PRIORITY = Thread.NORM_PRIORITY - 1;

  static final String PARAM_HISTORY_MAX = PREFIX + "historySize";
  static final int DEFAULT_HISTORY_MAX = 50;

  static final String PARAM_STATS_UPDATE_INTERVAL =
    PREFIX + "statsUpdateInterval";
  static final long DEFAULT_STATS_UPDATE_INTERVAL = 10 * Constants.SECOND;

  static final String PARAM_SORT_SCHEME = PREFIX + "tableSort";
  final int DEFAULT_SORT_SCHEME = PEND_REV | HIST_REV;

  // thread watchdog interval is set very high because the stepper thread
  // runs at low priority and may legitimately be blocked for a long time.
  static final String WDOG_PARAM_STEPPER = "TaskRunner";
  static final long WDOG_DEFAULT_STEPPER = 1 * Constants.DAY;

  static final int PEND_REV = 1;
  static final int HIST_REV = 2;
  static final int HIST_FIRST = 4;

  protected static Logger log = Logger.getLogger("TaskRunner");

  private SchedulerFactory schedulerFactory;
  private Collection acceptedTasks;
  private Schedule currentSchedule;
  // overrun tasks, sorted by finish deadline
  private TreeSet overrunTasks =
    new TreeSet(SchedulableTask.latestFinishComparator());
  // last n completed requests
  private HistoryList history = new HistoryList(DEFAULT_HISTORY_MAX);
  private StepThread stepThread;
  private BinarySemaphore sem = new BinarySemaphore();
  private int stepPriority = -1;
  private long statsUpdateInterval = DEFAULT_STATS_UPDATE_INTERVAL;;
  private int sortScheme = DEFAULT_SORT_SCHEME;

  private int taskCtr = 0;
  private long totalTime = 0;

  private static final int STAT_ACCEPTED = 0;
  private static final int STAT_REFUSED = 1;
  private static final int STAT_COMPLETED = 2;
  private static final int STAT_EXPIRED = 3;
  private static final int STAT_OVERRUN = 4;
  private static final int STAT_CANCELLED = 5;
  private static final int NUM_STATS = 6;

  private int backgroundStats[] = new int[NUM_STATS];
  private int foregroundStats[] = new int[NUM_STATS];


  TaskRunner(SchedulerFactory schedFactory) {
    if (schedFactory == null) {
      throw new NullPointerException("TaskRunner requires a SchedulerFactory");
    }
    this.schedulerFactory = schedFactory;
  }

  void init() {
    registerConfigCallback();
  }

  private void registerConfigCallback() {
    Configuration.registerConfigurationCallback(new Configuration.Callback() {
	public void configurationChanged(Configuration newConfig,
					 Configuration oldConfig,
					 Set changedKeys) {
	  setConfig(newConfig, changedKeys);
	}
      });
  }

  private void setConfig(Configuration config, Set changedKeys) {
    stepPriority = config.getInt(PARAM_PRIORITY, DEFAULT_PRIORITY);
    statsUpdateInterval =
      config.getTimeInterval(PARAM_STATS_UPDATE_INTERVAL,
			     DEFAULT_STATS_UPDATE_INTERVAL);
    sortScheme = config.getInt(PARAM_SORT_SCHEME, DEFAULT_SORT_SCHEME);

    int cMax = config.getInt(PARAM_HISTORY_MAX, DEFAULT_HISTORY_MAX);
    if (changedKeys.contains(PARAM_HISTORY_MAX) ) {
      synchronized (history) {
	history.setMax(cMax);
      }
    }
  }

  /** Attempt to add a task to the schedule.
   * @param task the new task
   * @return true if the task was added to the schedule.
   */
  boolean scheduleTask(SchedulableTask task) {
    if (addToSchedule(task)) {
      pokeThread();
      log.debug("Scheduled task: " + task);
      return true;
    } else {
      log.debug("Can't schedule task: " + task);
      addStats(task, STAT_REFUSED);
      return false;
    }
  }

  private void addStats(SchedulableTask task, int stat) {
    if (task.isBackgroundTask()) {
      backgroundStats[stat]++;
    } else {
      foregroundStats[stat]++;
    }
  }

  /** Return true iff the task could be scheduled, but doesn't actually
   * schedule the task. */
  public boolean isTaskSchedulable(SchedulableTask task) {
    Collection tasks = getCombinedTasks(task);
    Scheduler scheduler = schedulerFactory.createScheduler(tasks);
    boolean res = scheduler.createSchedule();
    log.debug2((res ? "Task is schedulable: " : "Task is not schedulable: ")
	       + task);
    return res;
  }

  private Collection getCombinedTasks(SchedulableTask newTask) {
    Collection tasks =
      (acceptedTasks != null) ? new ArrayList(acceptedTasks) : new ArrayList();
    tasks.add(newTask);
    return tasks;
  }

  /** Try a create a new Schedule from the union of acceptedTasks and task.
   * If successful, replace the currentSchedule and add the task to
   * acceptedTasks. */
  synchronized boolean addToSchedule(SchedulableTask task) {
    Collection tasks = getCombinedTasks(task);
    Scheduler scheduler = schedulerFactory.createScheduler(tasks);
    if (scheduler.createSchedule()) {
      currentSchedule = scheduler.getSchedule();
      acceptedTasks = tasks;
      task.setTaskRunner(this);
      Collection schedOverron = currentSchedule.getOverrunTasks();
      if (schedOverron != null && !schedOverron.isEmpty()) {
	for (Iterator iter = schedOverron.iterator(); iter.hasNext(); ) {
	  SchedulableTask otask = (SchedulableTask)iter.next();
	  if (otask instanceof StepTask) {
	    addOverrunner(otask);
	  } else {
	    log.error("Non step task in Schedule.overrunTasks: " + otask);
	  }
	}
      }
      task.schedSeq = ++taskCtr;
      task.schedDate = TimeBase.nowDate();
      addStats(task, STAT_ACCEPTED);
      return true;
    } else {
      return false;
    }
  }

  /** Called by BackgroundTask.taskIsFinished() to inform task runner that
   * a background task has finished before its end time. */
  synchronized void backgroundTaskIsFinished(BackgroundTask task) {
    Schedule.BackgroundEvent event = 
      new Schedule.BackgroundEvent(task, Deadline.in(0),
				   Schedule.EventType.FINISH);
    log.debug2("Background task finished early: " + task);
    if (true) {
      backgroundTaskEvent(event);
    } else {
      // put the event where the stepper thread will find it
      extraBackgroundEvents.add(event);
      if (stepThread != null) {
	// Avoid starting thread in unit tests.  In practice, the thread will
	// have been created if any background tasks are running.
	pokeThread();
      }
    }
  }

  /** Remove a previously scheduled task from the schedule.
   * @param task the previously schedule task
   * @return true if the task was remove from the schedule, false if it
   * wasn't already in the schedule..
   */
  // Called by SchedulableTask.cancel()
  synchronized boolean cancelTask(SchedulableTask task) {
    if (log.isDebug2()) {
      log.debug2("Cancel task: " + task);
    }
    // prevent any pending events from taking action
    task.setFinished();
    task.setNotified();
    // remove all traces of task    
    boolean res = acceptedTasks.remove(task);
    if (task.isBackgroundTask()) {
      removeFromBackgroundTasks((BackgroundTask)task);
    } else {
      // poke thread so it will recompute schedule
      pokeThread();
    }
    if (res) {
      addStats(task, STAT_CANCELLED);
    }
    return res;
  }

  synchronized void stopThread() {
    if (stepThread != null) {
      log.info("Stopping Q runner");
      stepThread.stopStepper();
      stepThread = null;
    }
  }

  // tk add watchdog
  synchronized void pokeThread() {
    if (stepThread == null) {
      log.info("Starting Q runner");
      stepThread = new StepThread("TaskRunner");
      stepThread.start();
    } else {
      stepThread.pokeStepper();
    }
  }

  // debugging accessors  (tk - remove?)

  Schedule getCurrentSchedule() {
    return currentSchedule;
  }

  Collection getAcceptedTasks() {
    return acceptedTasks;
  }

  double getBackgroundLoadFactor() {
    return backgroundLoadFactor;
  }

  TreeSet getOverrunTasks() {
    return overrunTasks;
  }

  List getHistorySnapshot() {
    synchronized (history) {
      return new ArrayList(history);
    }
  }

  synchronized List getSchedSnapshot() {
    if (currentSchedule == null) {
      return Collections.EMPTY_LIST;
    }
    List events = currentSchedule.getEvents();
    List res = new ArrayList(events.size());
    for (Iterator iter = events.iterator(); iter.hasNext();) {
      Schedule.Event event = (Schedule.Event)iter.next();
      // tk - hack to prevent finish event of already finished background
      // tasks, or redundant start events, from appearing in queue display.
      if (event.isBackgroundEvent()) {
	Schedule.BackgroundEvent be = (Schedule.BackgroundEvent)event;
	BackgroundTask bt = be.getTask();
	if (event.isTaskFinished() ||
	    (be.getType() == Schedule.EventType.START &&
	     backgroundTasks.contains(bt))) {
	  continue;
	}
      }
      res.add(event);
    }
    return res;
  }

  boolean isIdle() {
    return acceptedTasks == null || acceptedTasks.isEmpty();
  }

  // *******************************************************************
  // Following code is normally called only from the stepper thread.  It
  // resides in the main class for ease of synchronization and unit testing.

  Schedule.Chunk runningChunk = null;
  StepTask runningTask = null;
  Collection backgroundTasks = new HashSet();
  Deadline runningDeadline = null;
  double backgroundLoadFactor = 0.0;
  LinkedList extraBackgroundEvents = new LinkedList();

  void reschedule() {
    Scheduler scheduler = schedulerFactory.createScheduler(acceptedTasks);
    if (scheduler.createSchedule()) {
      currentSchedule = scheduler.getSchedule();
    }
  }

  /** Find the task that should be running and set up locals for that task
   * and chunk. Return true if found a task.  Process any background task
   * events in the schedule.  Synchronized for access to
   * currentSchedule. */
  synchronized boolean findTaskToRun() {
    while (!extraBackgroundEvents.isEmpty()) {
      Schedule.BackgroundEvent event =
	(Schedule.BackgroundEvent)extraBackgroundEvents.removeFirst();
      backgroundTaskEvent(event);
    }
    if (currentSchedule == null) {
      return false;
    }
    if (findTaskToRun0()) {
      return true;
    }
    Schedule.Chunk chunk = findRunnableChunk();
    if (chunk != null) {
      // runningDeadline should still be what findTaskToRun0 found - the
      // next event in the schedule
      runningChunk = chunk;
      runningTask = chunk.getTask();
      return true;
    }
    return false;
  }

  /** Find a chunk with a runnable task (one whose earliest start has been
   * reached). */
  Schedule.Chunk findRunnableChunk() {
    for (Iterator iter = currentSchedule.getEvents().iterator();
	 iter.hasNext(); ) {
      Schedule.Event event = (Schedule.Event)iter.next();
      if (!event.isBackgroundEvent()) {
	Schedule.Chunk chunk = (Schedule.Chunk)event;
	StepTask task = chunk.getTask();
	if (task.getEarlistStart().expired()) {
	  return chunk;
	}
      }
    }
    return null;
  }

  /** Find the task that should be running and set up locals for that task
   * and chunk. Return true if found a task.  Process any background task
   * events in the schedule. */
  boolean findTaskToRun0() {
    Schedule.Event event = null;
    while ((event = currentSchedule.getFirstEvent()) != null) {
      if (log.isDebug3()) {
	log.debug3("Examining " + event +
		   (event.getStart().expired() ? "[START]" : ""));
      }
      if (event.getStart().expired()) {
	if (event.isBackgroundEvent()) {
	  backgroundTaskEvent((Schedule.BackgroundEvent)event);
	  currentSchedule.removeFirstEvent(event);
	} else {
	  Schedule.Chunk chunk = (Schedule.Chunk)event;
	  if (chunk.getFinish().expired()) {
	    removeChunk(chunk);
	    continue;
	  }
	  // found a chunk that should be running
	  if (log.isDebug2()) log.debug2("Running " + chunk);
	  // (It is possible that this task has already finished, and this
	  // chunk is unnecessary.  In that case runSteps() will notice and
	  // remove the chunk immediately
	  runningChunk = chunk;
	  runningTask = (StepTask)chunk.getTask();
	  runningDeadline = chunk.getFinish();
	  // make sure we notice expired overrunners
	  notifyExpiredOverrunners();
	  return true;
	}
      } else {
	// exit loop when find first non-expired chunk
	break;
      }
    }
    runningChunk = null;
    // run overrun task
    notifyExpiredOverrunners();
    if (!overrunTasks.isEmpty()) {
      runningTask = (StepTask)overrunTasks.first();
      runningDeadline = (event == null ? runningTask.getLatestFinish()
			 : Deadline.earliest(runningTask.getLatestFinish(),
					     event.getStart())
			 );
      return true;
    } else {
      // no current event, no overrun tasks, sleep until next event.

      runningTask = null;
      runningDeadline = (event == null) ? Deadline.MAX : event.getStart();
      return false;
    }
  }

  void addToHistory(Schedule.Event event) {
    synchronized (history) {
      history.add(event);
    }
  }

  /** Cause a background task to stert or finish, according to event. */
  void backgroundTaskEvent(Schedule.BackgroundEvent event) {
    BackgroundTask task = event.getTask();
    Schedule.EventType et = event.getType();
    if (log.isDebug2()) log.debug2("Bkgnd event: " + event);
    if (Schedule.EventType.START == et) {
      // Redundant start events will be generated whenever the schedule is
      // recalculated after the task has started.  (Which is necessary to
      // ensure no start events are missed.)
      if (!task.isFinished() && addToBackgroundTasks(task)) {
	// Must provisionally add to backgroundTasks, as it might run and
	// finish before we get to run again after calling its taskEvent
	try {
	  task.callback.taskEvent(task, et);
	} catch (TaskCallback.Abort e) {
	  // task doesn't want to run, remove from backgroundTasks
	  event.setError(e);
	  removeFromBackgroundTasks(task);
	  acceptedTasks.remove(task);
	  addToHistory(event);
	  return;
	} catch (Exception e) {
	  log.error("Background task start callback threw", e);
	  event.setError(e);
	}
	addToHistory(event);
      }
    } else if (Schedule.EventType.FINISH == et) {
      acceptedTasks.remove(task);	// do this before callback
      if (removeFromBackgroundTasks(task)) {
	task.setFinished();
	addStats(task, STAT_COMPLETED);
	addToHistory(event);
	try {
	  task.callback.taskEvent(task, event.getType());
	} catch (Exception e) {
	  log.error("Background task finish callback threw", e);
	  event.setError(e);
	}
      }
    }
  }
      
  boolean addToBackgroundTasks(BackgroundTask task) {
    if (backgroundTasks.add(task)) {
      backgroundLoadFactor += task.getLoadFactor();
      if (backgroundLoadFactor > 1.0) {
	log.error("background load factor > 1.0: " + backgroundLoadFactor);
      }
      return true;
    } else {
      log.error("Already active background task: " + task);
      return false;
    }
  }

  boolean removeFromBackgroundTasks(BackgroundTask task) {
    if (backgroundTasks.remove(task)) {
      task.setTaskRunner(null);
      backgroundLoadFactor -= task.getLoadFactor();
      if (backgroundLoadFactor < 0.0) {
	log.error("background load factor < 0.0: " + backgroundLoadFactor);
      }
      return true;
    } else {
      return false;
    }
  }

  void notifyExpiredOverrunners() {
    while (!overrunTasks.isEmpty()) {
      SchedulableTask task = (SchedulableTask)overrunTasks.first();
      if (task.isExpired()) {
	removeTask(task);
      } else {
	break;
      }
    }
  }

  void removeChunk(Schedule.Chunk chunk) {
    if (log.isDebug3()) log.debug3("Removing " + chunk);
    SchedulableTask task = chunk.getTask();
    if (task.isFinished() || task.isExpired()) {
	removeTask(task);
    } else if (chunk.isTaskEnd()) {
      if (task.isOverrunAllowed()) {
	addOverrunner(task);
      } else {
	removeTask(task);
      }
    }
    currentSchedule.removeEvent(chunk);
    addToHistory(chunk);
    if (chunk == runningChunk) {
      runningChunk = null;
    }
  }

  void addOverrunner(SchedulableTask task) {
    if (overrunTasks.add(task)) {
      addStats(task, STAT_OVERRUN);
      log.debug2("New overrun: " + task);
    }
  }

  void removeTask(SchedulableTask task) {
    if (task.hasBeenNotified()) {
      // this can happen for a variety of reasons: task finished early but
      // has more chunks in the schedule; a new schedule is created while a
      // task is running, and the task finishes during the old chunk, etc.
      return;
    }
    if (log.isDebug3()) log.debug3("Removing " + task);
    if (task.e != null) {
      removeAndNotify(task, "Errored: ");
    } else if (task.isFinished()) {
      removeAndNotify(task, "Finished: ");
    } else if (task.isExpired()) {
      task.e = new SchedService.Timeout("task not finished before deadline");
      removeAndNotify(task, "Expired: ");
    } else {
      task.e = new RuntimeException("Impossible task state");
      log.error("Impossible task state: " + task, task.e);
      removeAndNotify(task, "Shouldn't: ");
    }
    task.setNotified();
  }

  private void removeAndNotify(SchedulableTask task, String msg) {
    task.setFinished();
    if (task.e instanceof SchedService.Timeout) {
      addStats(task, STAT_EXPIRED);
    } else {
      addStats(task, STAT_COMPLETED);
    }
    if (log.isDebug()) {
      log.debug(msg + ((task.e != null) ? (task.e + ": ") : "") + task);
    }
    if (task == runningTask) {
      runningTask = null;
    }
    synchronized (this) {
      acceptedTasks.remove(task);	// do this before callback
    }
    overrunTasks.remove(task);
    doCallback(task);
  }

  void doCallback(SchedulableTask task) {
    if (task.callback != null) {
      try {
	task.callback.taskEvent(task, Schedule.EventType.FINISH);
      } catch (Exception e) {
	log.error("Task callback threw", e);
      }
    }
    // history list for status only, don't hold on to caller's objects
    task.callback = null;
    task.cookie = null;
  }

  void runSteps(MutableBoolean continueStepping, LockssWatchdog wdog) {
    StepTask task = runningTask;
    Deadline until = runningDeadline;
    boolean overOk = task.isOverrunAllowed();
    long timeDelta = 0;
    long statsUpdateTime = TimeBase.nowMs() + statsUpdateInterval;
    long statsStartTime = TimeBase.nowMs();

    task.setStarted();
    task.setStepping(true);
    try {
       // step until reach deadline, or told to stop
       while (continueStepping.booleanValue() && !until.expired()) {
	 // MUST check this first, as it's possible chunks will still exist
	 // for an already-finished task.
	 if (task.isFinished()) {
	   break;
	 }
	 if (log.isDebug3()) log.debug3("taskStep: " + task);
	 // tk - step size?
	 task.step(0);
	 if (wdog != null) {
	   wdog.pokeWDog();
	 }
	 timeDelta =
	   (long)(TimeBase.msSince(statsStartTime) *
		  (1.0 - backgroundLoadFactor));
	 task.setUnaccountedTime(timeDelta);

	 if (task.isFinished()) {
	   task.setFinished();
	   break;
	 }
	 if (!overOk && (task.getTimeUsed() > task.origEst)) {
	   throw
	     new SchedService.Overrun("task not finished within estimate");
	 }
	 // 	Thread.yield();
	 if (TimeBase.nowMs() > statsUpdateTime) {
	   totalTime += timeDelta;
	   task.updateStats();
	   statsStartTime = TimeBase.nowMs();
	   statsUpdateTime = statsStartTime + statsUpdateInterval;
	 }
       }
       if (!task.isFinished() && task.isExpired()) {
	 if (log.isDebug()) log.debug("Expired: " + task);
	 throw
	   new SchedService.Timeout("task not finished before deadline");
       }
    } catch (Exception e) {
      // tk - should this catch all Throwable?
      task.e = e;
      task.setFinished();
    }
    totalTime += timeDelta;
    task.updateStats();
    task.setStepping(false);

    if (runningChunk != null) {
      if (runningChunk.getFinish().expired() || task.isFinished()) {
	removeChunk(runningChunk);
      }
    } else if (task.isFinished()) {
      removeTask(task);
    }
  }

  // Step thread
  private class StepThread extends LockssThread {
    private MutableBoolean continueStepping = new MutableBoolean(false);
    private boolean exit = false;

    private StepThread(String name) {
      super(name);
    }

    public void lockssRun() {
      triggerWDogOnExit(true);
      if (stepPriority > 0) {
	Thread.currentThread().setPriority(stepPriority);
      }
      startWDog(WDOG_PARAM_STEPPER, WDOG_DEFAULT_STEPPER);

      try {
	while (!exit) {
	  continueStepping.setValue(true);
	  if (findTaskToRun()) {
	    runSteps(continueStepping, this);
	  } else {
	    stopWDog();
	    sem.take(runningDeadline);
	    startWDog(WDOG_PARAM_STEPPER, WDOG_DEFAULT_STEPPER);
	  }
	}
      } catch (InterruptedException e) {
 	// no action - expected when stopping
      } catch (Exception e) {
	log.error("Unexpected exception caught in task stepper thread", e);
      } finally {
	stepThread = null;
      }
    }

    private void pokeStepper() {
      continueStepping.setValue(false);
      sem.give();
    }

    private void stopStepper() {
      triggerWDogOnExit(false);
      exit = true;
      pokeStepper();
    }
  }

  /** Factory supplied to constructor to create new Scheduler instances. */
  interface SchedulerFactory {
    public Scheduler createScheduler(Collection tasks);
  }

  // status table

  StatusAccessor getStatusAccessor() {
    return new Status();
  }

  private static final List statusSortRules =
    ListUtil.list(new StatusTable.SortRule("sort", true));

  private static final List statusColDescs =
    ListUtil.list(
// 		  new ColumnDescriptor("tasknum", "Task",
// 				       ColumnDescriptor.TYPE_INT),
		  new ColumnDescriptor("type", "Type",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor("task", "Task",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor("load", "%CPU",
				       ColumnDescriptor.TYPE_PERCENT),
		  new ColumnDescriptor("start", "Start",
				       ColumnDescriptor.TYPE_DATE),
		  new ColumnDescriptor("stop", "Stop",
				       ColumnDescriptor.TYPE_DATE)
		  );


  private class Status implements StatusAccessor {

    public String getDisplayName() {
      return "Scheduler Queue";
    }

    public void populateTable(StatusTable table) {
      String key = table.getKey();
      int scheme = parseSortScheme(key);

      table.setTitleFootnote(getTitleFootnote(scheme));
      table.setColumnDescriptors(statusColDescs);
      table.setDefaultSortRules(statusSortRules);
      table.setRows(getRows(scheme));
      table.setSummaryInfo(getSummaryInfo(key));
    }

    public boolean requiresKey() {
      return false;
    }

    private List getRows(int scheme) {
      List table = new ArrayList();
      List pend = getSchedSnapshot();
      List hist = getHistorySnapshot();
      int ix = 0;
      for (ListIterator iter = pend.listIterator(); iter.hasNext();) {
	Schedule.Event event = getDisplayEvent(iter);
	table.add(makeRow(event, scheme, ix++, iter, false));
      }
      ix = 0;
      for (ListIterator iter = hist.listIterator(); iter.hasNext();) {
	Schedule.Event event = getDisplayEvent(iter);
	table.add(makeRow(event, scheme, ix++, iter, true));
      }
      return table;
    }

    private Map makeRow(Schedule.Event event, int scheme, int ix,
			Iterator iter, boolean isHist) {
      Map row = new HashMap();
      if (isSepr(scheme, ix, !iter.hasNext(), isHist)) {
	row.put(StatusTable.ROW_SEPARATOR, "");
      }
      row.put("sort", new Integer(sortOrder(scheme, ix, isHist)));
      if (event.isBackgroundEvent()) {
	Schedule.BackgroundEvent be = (Schedule.BackgroundEvent)event;
	BackgroundTask bt = be.getTask();
	row.put("type", "  Back");
// 	row.put("tasknum", new Integer(bt.schedSeq));
	row.put("task", bt.schedSeq + ":" + bt.getShortText());
	row.put("load", new Double(bt.getLoadFactor()));
	if (be instanceof CombinedBackgroundEvent) {
	  CombinedBackgroundEvent cbe = (CombinedBackgroundEvent)be;
	  row.put("start", cbe.getStart());
	  row.put("stop", cbe.getFinish());
	} else if (Schedule.EventType.START == be.getType()) {
	  row.put("start", be.getStart());
	} else if (Schedule.EventType.FINISH == be.getType()) {
	  row.put("stop", be.getStart());
	}
      } else {
	Schedule.Chunk chunk = (Schedule.Chunk)event;
	StepTask st = chunk.getTask();
	row.put("type", (chunk == runningChunk) ? "*Fore" : "Fore");
// 	row.put("tasknum", new Integer(st.schedSeq));
	row.put("task", st.schedSeq + ":" + st.getShortText());
	row.put("load", new Double(chunk.getLoadFactor()));
	row.put("start", chunk.getStart());
	row.put("stop", chunk.getFinish());
      }
      return row;
    }

    private class CombinedBackgroundEvent extends Schedule.BackgroundEvent {
      private Deadline finish;
      private CombinedBackgroundEvent(BackgroundTask task, Deadline start,
				      Deadline finish) {
	super(task, start, Schedule.EventType.START);
	this.finish = finish;
      }

      Deadline getFinish() {
	return finish;
      }
    }

    private Schedule.Event getDisplayEvent(ListIterator eventIter) {
      Schedule.Event event = (Schedule.Event)eventIter.next();
      if (!event.isBackgroundEvent() || !eventIter.hasNext()) {
	return event;
      }
      Schedule.BackgroundEvent bevent = (Schedule.BackgroundEvent)event;
      Schedule.Event event2 = (Schedule.Event)eventIter.next();
      if (event2.isBackgroundEvent()) {
	Schedule.BackgroundEvent bevent2 = (Schedule.BackgroundEvent)event2;
	if (bevent.getTask().equals(bevent2.getTask()) &&
	    bevent.getType() != bevent2.getType()) {
	  if (bevent.getType() == Schedule.EventType.START) {
	    return new CombinedBackgroundEvent(bevent.getTask(),
					       bevent.getStart(),
					       bevent2.getStart());
	  } else {
	    return new CombinedBackgroundEvent(bevent.getTask(),
					       bevent2.getStart(),
					       bevent.getStart());
	  }
	}
      }
      eventIter.previous();
      return event;
    }

    private String combStats(int stat) {
      StringBuffer sb = new StringBuffer();
      sb.append(foregroundStats[stat]);
      sb.append(" foreground, ");
      sb.append(backgroundStats[stat]);
      sb.append(" background");
      return sb.toString();
    }

    private List getSummaryInfo(String key) {
      List res = new ArrayList();
      res.add(new StatusTable.SummaryInfo("Tasks accepted",
					  ColumnDescriptor.TYPE_STRING,
					  combStats(STAT_ACCEPTED)));
      res.add(new StatusTable.SummaryInfo("Tasks refused",
					  ColumnDescriptor.TYPE_STRING,
					  combStats(STAT_REFUSED)));
      res.add(new StatusTable.SummaryInfo("Tasks completed",
					  ColumnDescriptor.TYPE_STRING,
					  combStats(STAT_COMPLETED)));
      res.add(new StatusTable.SummaryInfo("Tasks expired",
					  ColumnDescriptor.TYPE_STRING,
					  combStats(STAT_EXPIRED)));
      res.add(new StatusTable.SummaryInfo("Tasks overrun",
					  ColumnDescriptor.TYPE_STRING,
					  combStats(STAT_OVERRUN)));
      res.add(new StatusTable.SummaryInfo("Total foreground time",
					  ColumnDescriptor.TYPE_TIME_INTERVAL,
					  new Long(totalTime)));
      if (!overrunTasks.isEmpty()) {
	res.add(new StatusTable.SummaryInfo("Active overrun tasks",
					    ColumnDescriptor.TYPE_INT,
					    new Long(overrunTasks.size())));
      }
      return res;
    }

    private boolean bittest(int x, int y) {
      return ((x & y) != 0);
    }

    private int sortOrder(int scheme, int ix, boolean history) {
      int res = ix;
      if ((history && bittest(scheme, HIST_REV)) ||
	  (!history && bittest(scheme, PEND_REV))) {
	res = -res;
      }
      if (bittest(scheme, HIST_FIRST) != history) {
	res += 99999;
      }
      return res;
    }

    private boolean isSepr(int scheme, int ix, boolean isLast,
			   boolean isHist) {
      if (isHist != bittest(scheme, HIST_FIRST)) {
	if (isHist) {
	  if (bittest(scheme, HIST_REV)) {
	    return isLast;
	  }
	} else {
	  if (bittest(scheme, PEND_REV)) {
	    return isLast;
	  }
	}
	return ix == 0;
      }
      return false;
    }

    private String getOrderWords(boolean isHist, int scheme) {
      if (isHist) {
	return (bittest(scheme, HIST_REV)
		? "most recent to oldest" : "oldest to most recent");
      } else {
	return (bittest(scheme, PEND_REV) ? "last to first" : "first to last");
      }
    }

    private String getSectionWord(boolean isHist) {
      return isHist ? "Completed" : "Pending";
    }

    private String getTitleFootnote(int scheme) {
      StringBuffer sb = new StringBuffer();
      boolean histFirst = bittest(scheme, HIST_FIRST);
      sb.append(getSectionWord(histFirst));
      sb.append(" events are at top of table, ");
      sb.append(getOrderWords(histFirst, scheme));
      sb.append(". ");
      sb.append(getSectionWord(!histFirst));
      sb.append(" events follow, ");
      sb.append(getOrderWords(!histFirst, scheme));
      switch (scheme) {
      case PEND_REV | HIST_REV:
	sb.append(".  (I.e., time moves backward down the page.)");
	break;
      case HIST_FIRST:
	sb.append(".  (I.e., time moves forward down the page.)");
	break;
      default:
	sb.append(".");
      }
      return sb.toString();
    }

    private int parseSortScheme(String key) {
      if (StringUtil.isNullString(key)) {
	return sortScheme;
      }
      try {
	return Integer.parseInt(key);
      } catch (Exception e) {
	return sortScheme;
      }
    }
  }

}
