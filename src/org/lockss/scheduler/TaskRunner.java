/*
 * $Id: TaskRunner.java,v 1.3 2003-11-13 11:16:16 tlipkis Exp $
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
// status table
// backgroundLoadFactor is adjusted incrementally; roundoff error will cause
//   long term drift.  (hack: reset to min when backgroundTasks empty.)
// resched if nothing to do (if min(accepted step task start) - now > k)
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

  static final String PARAM_COMPLETED_MAX = PREFIX + "historySize";
  static final int DEFAULT_COMPLETED_MAX = 50;

  protected static Logger log = Logger.getLogger("TaskRunner");

  private SchedulerFactory schedulerFactory;
  private Collection acceptedTasks;
  private Schedule currentSchedule;
  // overrun tasks, sorted by finish deadline
  private TreeSet overrunTasks =
    new TreeSet(SchedulableTask.latestFinishComparator());
  // last n completed requests
  private HistoryList completed = new HistoryList(50);
  private StepThread stepThread;
  private BinarySemaphore sem = new BinarySemaphore();
  private int stepPriority = -1;

  private int taskCtr = 0;
  private long totalTime = 0;

  private int tasksAccepted = 0;
  private int tasksRefused = 0;
  private int tasksFinished = 0;
  private int tasksOverrun = 0;

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
    int cMax = config.getInt(PARAM_COMPLETED_MAX, DEFAULT_COMPLETED_MAX);
    if (changedKeys.contains(PARAM_COMPLETED_MAX) ) {
      synchronized (completed) {
	completed.setMax(config.getInt(PARAM_COMPLETED_MAX, 50));
      }
    }
  }

  /** Attempt to add a task to the schedule.
   * @param task the new task
   * @return true if the task was added to the schedule.
   */
  public synchronized boolean scheduleTask(SchedulableTask task) {
    if (addToSchedule(task)) {
      pokeThread();
      log.debug("Scheduled task: " + task);
      return true;
    } else {
      log.debug("Can't schedule task: " + task);
      tasksRefused++;
      return false;
    }
  }

  /** Return true iff the task could be scheduled, but doesn't actually
   * schedule the task. */
  public boolean isTaskSchedulable(SchedulableTask task) {
    Collection tasks = getCombinedTasks(task);
    Scheduler scheduler = schedulerFactory.createScheduler(tasks);
    return scheduler.createSchedule();
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
      task.schedSeq = ++taskCtr;
      task.schedDate = TimeBase.nowDate();
      acceptedTasks = tasks;
      tasksAccepted++;
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
    extraBackgroundEvents.add(event);
    if (stepThread != null) {
      // Avoid starting thread in unit tests.  In practice, the thread will
      // have been created if any background tasks are running.
      pokeThread();
    }
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

  List getCompletedSnapshot() {
    synchronized (completed) {
      return new ArrayList(completed);
    }
  }

  synchronized List getSchedSnapshot() {
    if (currentSchedule == null) {
      return Collections.EMPTY_LIST;
    }
    return new ArrayList(currentSchedule.getEvents());
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

  /** Return the earliest starting time of any step task, or null if
   * none */
  Deadline findEarlistStepTaskTime() {
    if (acceptedTasks == null || acceptedTasks.isEmpty()) {
      return null;
    }
    Deadline earliest = null;
    for (Iterator iter = acceptedTasks.iterator(); iter.hasNext();) {
      SchedulableTask task = (SchedulableTask)iter.next();
      if (!task.isBackgroundTask()) {
	if (earliest == null) {
	  earliest = task.getEarlistStart();
	} else {
	  earliest = Deadline.earliest(earliest, task.getEarlistStart());
	}
      }
    }
    return earliest;
  }
    
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
    Deadline earliest = findEarlistStepTaskTime();
    if (earliest == null || runningDeadline.minus(earliest) < 10) {
      return false;
    }
    reschedule();
    return findTaskToRun0();
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

  void addToCompleted(Schedule.Event event) {
    synchronized (completed) {
      completed.add(event);
    }
  }

  /** Cause a background task to stert or finish, according to event. */
  void backgroundTaskEvent(Schedule.BackgroundEvent event) {
    BackgroundTask task = event.getTask();
    Schedule.EventType et = event.getType();
    if (Schedule.EventType.START == et) {
      try {
	task.callback.taskEvent(task, event.getType());
      } catch (TaskCallback.Abort e) {
	// task doesn't want to run, don't add to running background tasks
	event.setError(e);
	acceptedTasks.remove(task);
	addToCompleted(event);
	return;
      } catch (Exception e) {
	log.error("Background task start callback threw", e);
	event.setError(e);
      }
      addToBackgroundTasks(task);
      addToCompleted(event);
    } else if (Schedule.EventType.FINISH == et) {
      acceptedTasks.remove(task);	// do this before callback
      if (removeFromBackgroundTasks(task)) {
	addToCompleted(event);
	try {
	  task.callback.taskEvent(task, event.getType());
	} catch (Exception e) {
	  log.error("Background task finish callback threw", e);
	  event.setError(e);
	}
      }
    }
  }
      
  void addToBackgroundTasks(BackgroundTask task) {
    if (backgroundTasks.contains(task)) {
      log.error("Already active background task: " + task);
      return;
    }
    backgroundTasks.add(task);
    task.setTaskRunner(this);
    backgroundLoadFactor += task.getLoadFactor();
    if (backgroundLoadFactor > 1.0) {
      log.error("background load factor > 1.0: " + backgroundLoadFactor);
    }
  }

  boolean removeFromBackgroundTasks(BackgroundTask task) {
    if (backgroundTasks.contains(task)) {
      backgroundTasks.remove(task);
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
    currentSchedule.removeFirstEvent(chunk);
    addToCompleted(chunk);
    if (chunk == runningChunk) {
      runningChunk = null;
    }
  }

  void addOverrunner(SchedulableTask task) {
    log.debug2("New overrun: " + task);
    overrunTasks.add(task);
    tasksOverrun++;
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
      log.error("Impossible task state: " + task);
      task.e = new RuntimeException("Impossible task state");
      removeAndNotify(task, "Shouldn't: ");
    }
    task.setNotified();
  }

  private void removeAndNotify(SchedulableTask task, String msg) {
    task.setFinished();
    tasksFinished++;
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
    // completed list for status only, don't hold on to caller's objects
    task.callback = null;
    task.cookie = null;
  }

  void runSteps(Boolean continueStepping) {
    StepTask task = runningTask;
    Deadline until = runningDeadline;
    boolean overOk = task.isOverrunAllowed();
    long timeDelta = 0;
    long startTime = TimeBase.nowMs();

    task.setStarted();
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
	 timeDelta =
	   (long)(TimeBase.msSince(startTime) * (1.0 - backgroundLoadFactor));

	 if (task.isFinished()) {
	   task.setFinished();
	   break;
	 }
	 if (!overOk && ((task.timeUsed + timeDelta) > task.origEst)) {
	   throw
	     new SchedService.Overrun("task not finished within estimate");
	 }
// 	Thread.yield();
      }
      if (!task.isFinished() && task.isExpired()) {
	if (log.isDebug()) log.debug("Expired: " + task);
	throw
	  new SchedService.Timeout("task not finished before deadline");
      }
    } catch (Exception e) {
      // tk - should this catch all Throwable?
      task.e = e;
    }
    task.timeUsed += timeDelta;
    totalTime += timeDelta;

    if (runningChunk != null) {
      if (runningChunk.getFinish().expired() || task.isFinished()) {
	removeChunk(runningChunk);
      }
    } else if (task.isFinished()) {
      removeTask(task);
    }
  }

  // Step thread
  private class StepThread extends Thread {
    private Boolean continueStepping;
    private boolean exit = false;

    private StepThread(String name) {
      super(name);
    }

    public void run() {
      if (stepPriority > 0) {
	Thread.currentThread().setPriority(stepPriority);
      }

      try {
	while (!exit) {
	  continueStepping = Boolean.TRUE;
	  if (findTaskToRun()) {
	    runSteps(continueStepping);
	  } else {
	    sem.take(runningDeadline);
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
      continueStepping = Boolean.FALSE;
      sem.give();
    }

    private void stopStepper() {
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

  static final String FOOT_TITLE =
    "Pending events are first in table, in the order they will occur."+
    "  Completed events follow, most recent first.";

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
    public void populateTable(StatusTable table) {
      String key = table.getKey();
      table.setTitle("Scheduler Queue");
      table.setTitleFootnote(FOOT_TITLE);
      table.setColumnDescriptors(statusColDescs);
      table.setDefaultSortRules(statusSortRules);
      table.setRows(getRows(key));
      table.setSummaryInfo(getSummaryInfo(key));
    }

    public boolean requiresKey() {
      return false;
    }

    private List getRows(String key) {
      List table = new ArrayList();
      int ix = 0;
      for (ListIterator iter = getSchedSnapshot().listIterator();
	   iter.hasNext();) {
	Schedule.Event event = (Schedule.Event)iter.next();
	// tk - hack to prevent finish event of already finished background
	// tasks from appearing in queue.  Should mark task done instead?
	if (!event.isBackgroundEvent() ||
	    acceptedTasks.contains(((Schedule.BackgroundEvent)event).getTask())) {
	  table.add(makeRow(event, ix++));
	}
      }
      int seprIx = ix + 1;
      List complet = getCompletedSnapshot();
      Collections.reverse(complet);
      for (Iterator iter = complet.iterator(); iter.hasNext();) {
	Map row = makeRow((Schedule.Event)iter.next(), ix++);
	// if both parts of the table are present , add a separator before
	// the first completed event
	if (ix == seprIx) {
	  row.put(StatusTable.ROW_SEPARATOR, "");
	}
	table.add(row);
      }
      return table;
    }

    private Map makeRow(Schedule.Event event, int sort) {
      Map row = new HashMap();
      row.put("sort", new Integer(sort));
      if (event.isBackgroundEvent()) {
	Schedule.BackgroundEvent be = (Schedule.BackgroundEvent)event;
	BackgroundTask bt = be.getTask();
	row.put("type", "B");
// 	row.put("tasknum", new Integer(bt.schedSeq));
	row.put("task", bt.schedSeq + ":" + bt.getShortText());
	row.put("load", new Double(bt.getLoadFactor()));
	if (Schedule.EventType.START == be.getType()) {
	  row.put("start", be.getStart());
	} else if (Schedule.EventType.FINISH == be.getType()) {
	  row.put("stop", be.getStart());
	}
      } else {
	Schedule.Chunk chunk = (Schedule.Chunk)event;
	StepTask st = chunk.getTask();
	row.put("type", "S");
// 	row.put("tasknum", new Integer(st.schedSeq));
	row.put("task", st.schedSeq + ":" + st.getShortText());
	row.put("load", new Double(chunk.getLoadFactor()));
	row.put("start", chunk.getStart());
	row.put("stop", chunk.getFinish());
      }
      return row;
    }

    private List getSummaryInfo(String key) {
      List res = new ArrayList();
      res.add(new StatusTable.SummaryInfo("Tasks accepted",
					  ColumnDescriptor.TYPE_INT,
					  new Long(tasksAccepted)));
      res.add(new StatusTable.SummaryInfo("Tasks refused",
					  ColumnDescriptor.TYPE_INT,
					  new Long(tasksRefused)));
      res.add(new StatusTable.SummaryInfo("Tasks finished",
					  ColumnDescriptor.TYPE_INT,
					  new Long(tasksFinished)));
      res.add(new StatusTable.SummaryInfo("Tasks overrun",
					  ColumnDescriptor.TYPE_INT,
					  new Long(tasksOverrun)));
      res.add(new StatusTable.SummaryInfo("Total task time",
					  ColumnDescriptor.TYPE_TIME_INTERVAL,
					  new Long(totalTime)));
      return res;
    }

  }

}
