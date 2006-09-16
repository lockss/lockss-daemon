/*
 * $Id: TaskRunner.java,v 1.37 2006-09-16 22:59:42 tlipkis Exp $
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
import org.lockss.config.Configuration;
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.util.*;

class TaskRunner {
  // Sort options for displaying queue
  static final int PEND_REV = 1;
  static final int HIST_REV = 2;
  static final int HIST_FIRST = 4;

  static final String PREFIX = Configuration.PREFIX + "taskRunner.";

  static final String PARAM_DROP_TASK_MAX = PREFIX + "maxTaskDrop";
  static final int DEFAULT_DROP_TASK_MAX = 0;

  static final String PARAM_MIN_CLEANUP_INTERVAL =
    PREFIX + "minCleanupInterval";
  static final long DEFAULT_MIN_CLEANUP_INTERVAL = Constants.SECOND;

  static final String PARAM_HISTORY_MAX = PREFIX + "historySize";
  static final int DEFAULT_HISTORY_MAX = 50;

  static final String PARAM_STATS_UPDATE_INTERVAL =
    PREFIX + "statsUpdateInterval";
  static final long DEFAULT_STATS_UPDATE_INTERVAL = 10 * Constants.SECOND;

  static final String PARAM_SORT_SCHEME = PREFIX + "tableSort";
  static final int DEFAULT_SORT_SCHEME = PEND_REV | HIST_REV;

  // thread watchdog interval is set very high because the stepper thread
  // runs at low priority and may legitimately be blocked for a long time.
  static final String WDOG_PARAM_STEPPER = "TaskRunner";
  static final long WDOG_DEFAULT_STEPPER = 1 * Constants.DAY;

  static final String PRIORITY_PARAM_STEPPER = "TaskRunner";
  static final int PRIORITY_DEFAULT_STEPPER = Thread.NORM_PRIORITY - 1;

  static final String PRIORITY_PARAM_NOTIFIER = "Notifier";
  static final int PRIORITY_DEFAULT_NOTIFIER = Thread.NORM_PRIORITY + 1;

  static final String WDOG_PARAM_NOTIFIER = "Notifier";
  static final long WDOG_DEFAULT_NOTIFIER = Constants.HOUR;

  protected static Logger log = Logger.getLogger("TaskRunner");

  private LockssDaemon daemon;
  private final SchedulerFactory schedulerFactory;

  // config params
  private int maxDrop = DEFAULT_DROP_TASK_MAX;
  private long minCleanupInterval = DEFAULT_MIN_CLEANUP_INTERVAL;
  private long statsUpdateInterval = DEFAULT_STATS_UPDATE_INTERVAL;
  private int sortScheme = DEFAULT_SORT_SCHEME;

  private FifoQueue notifyQueue = new FifoQueue();

  // all accesses synchronized
  private List acceptedTasks = new ArrayList();
  private Schedule currentSchedule;
  // overrun tasks, sorted by finish deadline
  private TreeSet overrunTasks =
    new TreeSet(SchedulableTask.latestFinishComparator());
  // last n completed requests
  private HistoryList history = new HistoryList(DEFAULT_HISTORY_MAX);
  private StepThread stepThread;
  private NotifyThread notifyThread;

  private int taskCtr = 0;
  private long totalTime = 0;

  private static final int STAT_NONE = -1;
  private static final int STAT_ACCEPTED = 0;
  private static final int STAT_REFUSED = 1;
  private static final int STAT_COMPLETED = 2;
  private static final int STAT_EXPIRED = 3;
  private static final int STAT_OVERRUN = 4;
  private static final int STAT_CANCELLED = 5;
  private static final int STAT_DROPPED = 6;
  private static final int STAT_WAITING = 7;
  private static final int NUM_STATS = 8;

  private int backgroundStats[] = new int[NUM_STATS];
  private int foregroundStats[] = new int[NUM_STATS];


  TaskRunner(SchedulerFactory schedFactory) {
    if (schedFactory == null) {
      throw new NullPointerException("TaskRunner requires a SchedulerFactory");
    }
    this.schedulerFactory = schedFactory;
  }

  // This isn't a LockssManager; these are called from SchedService

  void initService(LockssDaemon daemon) {
    this.daemon = daemon;
  }

  void startService() {
    registerConfigCallback();
  }

  void stopService() {
    stopStepThread();
  }

  private void registerConfigCallback() {
    daemon.getConfigManager().registerConfigurationCallback(new Configuration.Callback() {
	public void configurationChanged(Configuration newConfig,
					 Configuration oldConfig,
					 Configuration.Differences changedKeys) {
	  setConfig(newConfig, changedKeys);
	}
      });
  }

  private void setConfig(Configuration config,
			 Configuration.Differences changedKeys) {
    maxDrop = config.getInt(PARAM_DROP_TASK_MAX, DEFAULT_DROP_TASK_MAX);

    minCleanupInterval = config.getTimeInterval(PARAM_MIN_CLEANUP_INTERVAL,
						DEFAULT_MIN_CLEANUP_INTERVAL);

    statsUpdateInterval =
      config.getTimeInterval(PARAM_STATS_UPDATE_INTERVAL,
			     DEFAULT_STATS_UPDATE_INTERVAL);
    sortScheme = config.getInt(PARAM_SORT_SCHEME, DEFAULT_SORT_SCHEME);

    if (changedKeys.contains(PARAM_HISTORY_MAX) ) {
      int cMax = config.getInt(PARAM_HISTORY_MAX, DEFAULT_HISTORY_MAX);
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
      pokeStepThread();
      if (log.isDebug()) log.debug("Scheduled task: " + task);
      return true;
    } else {
      if (log.isDebug2()) log.debug2("Can't schedule task: " + task);
      incrStats(task, STAT_REFUSED);
      return false;
    }
  }

  private void incrStats(SchedulableTask task, int stat) {
    if (task.isBackgroundTask()) {
      backgroundStats[stat]++;
    } else {
      foregroundStats[stat]++;
    }
  }

  private void decrStats(SchedulableTask task, int stat) {
    if (task.isBackgroundTask()) {
      backgroundStats[stat]--;
    } else {
      foregroundStats[stat]--;
    }
  }

  /** Return true iff the task could be scheduled, but doesn't actually
   * schedule the task. */
  public synchronized boolean isTaskSchedulable(SchedulableTask task) {
    Scheduler scheduler = schedulerFactory.createScheduler();
    boolean res = canAddToSchedule(scheduler, task, false);
    if (log.isDebug2()) {
      log.debug2((res ? "Task is schedulable: " : "Task is not schedulable: ")
		 + task);
    }
    return res;
  }

  boolean canAddToSchedule(Scheduler scheduler, SchedulableTask task,
			   boolean doDrop) {
    Collection tasks = getCombinedTasks(task);
    if (scheduler.createSchedule(tasks)) {
      return true;
    }
    if (!cleanupSchedule(scheduler, doDrop)) {
      return false;
    }
    return scheduler.createSchedule(tasks);
  }

  private long lastCleanup = 0;

  boolean cleanupSchedule(Scheduler scheduler, boolean doDrop) {
    if (currentSchedule == null) {
      log.debug("cleanupSchedule(): currentSchedule = null");
      return false;
    }
    // don't do this too often
    if (TimeBase.msSince(lastCleanup) < minCleanupInterval) {
      return false;
    }
    lastCleanup = TimeBase.nowMs();
    List unexpired = getUnexpiredTasks();
    if (scheduler.createSchedule(unexpired)) {
      // no cleanup necessary, nothing pruned
      log.debug("cleanupSchedule(): no cleanup necessary");
      return false;
    }
    HashSet remainingTasks = new HashSet(unexpired);
    HashSet dropped = new HashSet();

    for (int ix = maxDrop; ix > 0; ix--) {
      SchedulableTask droppable = findDroppableTask(dropped);
      if (droppable == null) {
	log.error("Failed to cleanup schedule: " + currentSchedule +
		  " after dropping " + dropped);
	return false;
      }
      if (log.isDebug3()) log.debug3("Considering dropping: " + droppable);
      remainingTasks.remove(droppable);
      dropped.add(droppable);
      if (scheduler.createSchedule(remainingTasks)) {
	log.debug3("succeeded");
	if (doDrop) {
	  for (Iterator iter = dropped.iterator(); iter.hasNext(); ) {
	    SchedulableTask task = (SchedulableTask)iter.next();
	    log.warning("Dropped " + task);
	    task.setDropped();
	    addOverrunner(task, STAT_DROPPED);
	  }
	}
	return true;
      }
    }
    return false;
  }

  /** Find a task worth dropping */
  SchedulableTask findDroppableTask(Set alreadyDropped) {
    for (Iterator iter = currentSchedule.getEvents().iterator();
	 iter.hasNext(); ) {
      Schedule.Event event = (Schedule.Event)iter.next();
      if (!event.isBackgroundEvent()) {
	Schedule.Chunk chunk = (Schedule.Chunk)event;
	StepTask task = chunk.getTask();
	if (alreadyDropped.contains(task)) {
	  continue;
	}
	if (task.getEarliestStart().expired()) {
	  return task;
	}
      }
    }
    return null;
  }



  /** Find the earliest possible time a background task could be scheduled.
   * This is only a hint; it may not be possible to schedule the task then.
   * @param task a Background task specifying the duration, load factor and
   * earliest desired start time.
   * @return The BackgroundTask (the same one) with possibly updated start
   * and finish times when it might be schedulable. */
  public synchronized BackgroundTask scheduleHint(BackgroundTask task) {
    if (currentSchedule == null) {
      return task;
    }
    return currentSchedule.scheduleHint(task);
  }

  private List getCombinedTasks(SchedulableTask newTask) {
    return removeExpiredAndPossiblyAdd(newTask);
  }

  private List getUnexpiredTasks() {
    return removeExpiredAndPossiblyAdd(null);
  }

  private List removeExpiredAndPossiblyAdd(SchedulableTask newTask) {
    List tasks = null;
    if (newTask != null) {
      tasks = new ArrayList(acceptedTasks.size() + 1);
    }
    for (Iterator iter = acceptedTasks.listIterator(); iter.hasNext(); ) {
      SchedulableTask task = (SchedulableTask)iter.next();
      if (task.isExpired()) {
	if (task.isBackgroundTask()) {
	  Schedule.BackgroundEvent event =
	    new Schedule.BackgroundEvent((BackgroundTask)task, Deadline.in(0),
					 Schedule.EventType.FINISH);
	  extraBackgroundEvents.add(event);
	  pokeStepThread(false);
	} else {
	  addOverrunner(task, STAT_NONE);
	}

	// Also remove expired tasks from acceptedTasks here, so don't have
	// to look at them again.

	iter.remove();

      } else if (tasks != null) {
	tasks.add(task);
      }
    }
    if (tasks != null) {
      tasks.add(newTask);
      return tasks;
    } else {
      return acceptedTasks;
    }
  }

  /** Try a create a new Schedule from the union of acceptedTasks and task.
   * If successful, replace the currentSchedule and add the task to
   * acceptedTasks. */
  synchronized boolean addToSchedule(SchedulableTask task) {
    if (task.isBackgroundTask()) {
      // If a deferrable background task, get hint about when it might be
      // schedulable.   XXX Move into scheduler.
      BackgroundTask btask = (BackgroundTask)task;
      if (btask.getStart().before(btask.getLatestStart())) {
	btask = scheduleHint(btask);
	if (btask.getLatestStart().before(btask.getStart())) {
	  // hint is after latest start, fail
	  return false;
	}
      }
    }
    Scheduler scheduler = schedulerFactory.createScheduler();
    if (canAddToSchedule(scheduler, task, true)) {
      currentSchedule = scheduler.getSchedule();
      acceptedTasks = new ArrayList(scheduler.getTasks());
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
      incrStats(task, STAT_ACCEPTED);
      incrStats(task, STAT_WAITING);
      return true;
    } else {
      return false;
    }
  }

  /** Called by BackgroundTask.taskIsFinished() to inform task runner that
   * a background task has finished before its end time. */
  synchronized void backgroundTaskIsFinished(BackgroundTask task) {
    if (task.isFinished()) {
      // ignore if task already finished/finishing
      if (log.isDebug3()) {
	log.debug3("Background task finished redundantly: " + task);
      }
      return;
    }
    if (log.isDebug2()) log.debug2("Background task finished early: " + task);
    Schedule.BackgroundEvent event =
      new Schedule.BackgroundEvent(task, Deadline.in(0),
				   Schedule.EventType.FINISH);
    if (true) {
      backgroundTaskEvent(event);
    } else {
      // put the event where the stepper thread will find it
      extraBackgroundEvents.add(event);
      pokeStepThread(false);
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
      pokeStepThread();
    }
    if (res) {
      incrStats(task, STAT_CANCELLED);
    }
    return res;
  }

  synchronized void stopStepThread() {
    if (stepThread != null) {
      log.info("Stopping Q runner");
      stepThread.stopStepper();
      stepThread = null;
    }
  }

  synchronized void pokeStepThread() {
    if (stepThread == null) {
      log.info("Starting Q runner");
      stepThread = new StepThread("TaskRunner");
      stepThread.start();
      stepThread.waitRunning();
    } else {
      stepThread.pokeStepper();
    }
  }

  void pokeStepThread(boolean startIfNotRunning) {
    if (startIfNotRunning || stepThread != null) {
      // Avoid starting thread in unit tests.  In practice, the thread will
      // have been created once anything has been scheduled
      pokeStepThread();
    }
  }

  synchronized void stopNotifyThread() {
    if (notifyThread != null) {
      log.info("Stopping notifier");
      notifyThread.stopNotifier();
      notifyThread = null;
    }
  }

  synchronized void pokeNotifyThread() {
    if (notifyThread == null) {
      log.info("Starting notifier");
      notifyThread = new NotifyThread("TaskNotifier");
      notifyThread.start();
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

  synchronized boolean isIdle() {
    return acceptedTasks.isEmpty() && notifyQueue.isEmpty();
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
    Scheduler scheduler = schedulerFactory.createScheduler();
    if (scheduler.createSchedule(acceptedTasks)) {
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
	if (task.getEarliestStart().expired()) {
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

  // Cause a task event callback to be notified, by adding an element to
  // the queue processed by the notify task.
  void notify(SchedulableTask task, Schedule.EventType eventType) {
    notifyQueue.put(new Notification(task, eventType));
    pokeNotifyThread();
  }

  /** Cause a background task to stert or finish, according to event. */
  void backgroundTaskEvent(Schedule.BackgroundEvent event) {
    BackgroundTask task = event.getTask();
    Schedule.EventType et = event.getType();
    if (log.isDebug2()) log.debug2("Bkgnd event: " + event);
    if (Schedule.EventType.START == et) {
      // Silently absorb redundant start events, which appear whenever the
      // schedule is recalculated after the task has started.  (Which is
      // necessary to ensure no start events are missed.)
      if (!task.isFinished() && addToBackgroundTasks(task)) {
	// Must add to backgroundTasks *before* signalling the taskEvent
	// callback, as that might cause the task to run and finish, and
	// remove itself from backgroundTasks, before we get to run again.
	decrStats(task, STAT_WAITING);
	try {
	  notify(task, et);
// 	} catch (TaskCallback.Abort e) {
// 	  // task doesn't want to run, remove from backgroundTasks
// 	  event.setError(e);
// 	  removeFromBackgroundTasks(task);
// 	  acceptedTasks.remove(task);
// 	  addToHistory(event);
// 	  return;
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
	if (task.isExpired()) {
	  incrStats(task, STAT_EXPIRED);
	} else {
	  incrStats(task, STAT_COMPLETED);
	}
	addToHistory(event);
	try {
	  notify(task, event.getType());
	} catch (Exception e) {
	  log.error("Background task finish callback threw", e);
	  event.setError(e);
	}
      } else {
	// if it was not in backgroundTasks, it never started, thus it is
	// now no longer waiting
	decrStats(task, STAT_WAITING);
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
      // This is expected if schedule is recomputed while background task is
      // running
      if (log.isDebug()) log.debug("Already active background task: " + task);
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
      StepTask task = (StepTask)overrunTasks.first();
      if (task.isExpired()) {
	removeTask(task);
      } else {
	break;
      }
    }
  }

  void removeChunk(Schedule.Chunk chunk) {
    if (log.isDebug3()) log.debug3("Removing " + chunk);
    StepTask task = chunk.getTask();
    if (task.isFinished() || task.isExpired()) {
	removeTask(task);
    } else if (chunk.isTaskEnd()) {
      if (task.isOverrunAllowed()) {
	addOverrunner(task, task.hasOverrun() ? STAT_OVERRUN : STAT_DROPPED);
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
    addOverrunner(task, STAT_OVERRUN);
  }

  void addOverrunner(SchedulableTask task, int statidx) {
    if (overrunTasks.add(task)) {
      if (statidx != STAT_NONE) {
	incrStats(task, statidx);
	if (log.isDebug2()) log.debug2("New overruner (" + statidx + "): " +
				       task);
      } else {
	if (log.isDebug2()) log.debug2("New overruner: " + task);
      }
    }
  }

  void removeTask(StepTask task) {
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

  private void removeAndNotify(StepTask task, String msg) {
    task.setFinished();
    if (task.e instanceof SchedService.Timeout) {
      incrStats(task, STAT_EXPIRED);
    } else {
      incrStats(task, STAT_COMPLETED);
    }
    if (log.isDebug()) {
      log.debug(msg + ((task.e != null) ? (task.e + ": ") : "") + task);
    }
    if (task == runningTask) {
      runningTask = null;
    }
    synchronized (this) {
      acceptedTasks.remove(task);	// do this before callback
      decrStats(task, STAT_WAITING);
    }
    overrunTasks.remove(task);
    doCallback(task);
  }

  void doCallback(StepTask task) {
    if (task.callback != null) {
      try {
	notify(task, Schedule.EventType.FINISH);
      } catch (Exception e) {
	log.error("Task callback threw", e);
      }
    }
  }

  void runSteps(MutableBoolean continueStepping, LockssWatchdog wdog) {
    StepTask task = runningTask;
    Deadline until = runningDeadline;
    boolean overOk = task.isOverrunAllowed();
    long timeDelta = 0;
    long statsStartTime = TimeBase.nowMs();
    long statsUpdateTime = statsStartTime + statsUpdateInterval;

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
    private BinarySemaphore sem = new BinarySemaphore();
    private boolean exit = false;

    private StepThread(String name) {
      super(name);
    }

    public void lockssRun() {
      triggerWDogOnExit(true);
      setPriority(PRIORITY_PARAM_STEPPER, PRIORITY_DEFAULT_STEPPER);
      startWDog(WDOG_PARAM_STEPPER, WDOG_DEFAULT_STEPPER);
      nowRunning();

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

  // Notify thread
  private class NotifyThread extends LockssThread {
    private boolean exit = false;

    private NotifyThread(String name) {
      super(name);
    }

    public void lockssRun() {
      triggerWDogOnExit(true);
      setPriority(PRIORITY_PARAM_NOTIFIER, PRIORITY_DEFAULT_NOTIFIER);
      startWDog(WDOG_PARAM_NOTIFIER, WDOG_DEFAULT_NOTIFIER);
      nowRunning();

      Notification note;
      try {
	while (!exit) {
	  Deadline timeout = Deadline.in(30 * Constants.MINUTE);
	  if ((note = (Notification)notifyQueue.get(timeout)) != null) {
	    try {
	      note.doNotify();
	    } catch (Exception e) {
	      log.warning("Exception in task callback", e);
	    }
	  }
	  pokeWDog();
	}
	stopWDog();
	triggerWDogOnExit(false);
      } catch (InterruptedException e) {
 	// no action - expected when stopping
      } finally {
	notifyThread = null;
      }
    }

    private void stopNotifier() {
      triggerWDogOnExit(false);
      exit = true;
      interrupt();
    }
  }

  /** Structure put on notification queue */
  static class Notification {
    private SchedulableTask task;
    private Schedule.EventType eventType;

    Notification(SchedulableTask task, Schedule.EventType eventType) {
      this.task = task;
      this.eventType = eventType;
    }

    void doNotify() {
      if (task.callback != null) {
	try {
	  task.callback.taskEvent(task, eventType);
	} finally {
	  if (eventType == Schedule.EventType.FINISH) {
	    // task will stay on history list for a while; don't hold on to
	    // caller's objects
	    task.callback = null;
	    task.cookie = null;
	  }
	}
      }
    }
  }

  /** Factory supplied to constructor to create new Scheduler instances. */
  interface SchedulerFactory {
    public Scheduler createScheduler();
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
		  new ColumnDescriptor("in", "In",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor("dur", "Dur",
				       ColumnDescriptor.TYPE_STRING),
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
      table.setResortable(false);
      String key = table.getKey();
      int scheme = parseSortScheme(key);

      table.setTitleFootnote(getTitleFootnote(scheme));
      if (!table.getOptions().get(StatusTable.OPTION_NO_ROWS)) {
	table.setColumnDescriptors(statusColDescs);
	table.setDefaultSortRules(statusSortRules);
	table.setRows(getRows(scheme));
      }
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
      long nowMs = TimeBase.nowMs();
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
	  row.put("in", timeDelta(nowMs, cbe.getStart().getExpirationTime()));
	  row.put("dur", timeDelta(cbe.getStart().getExpirationTime(),
				   cbe.getFinish().getExpirationTime()));
	  row.put("start", cbe.getStart());
	  row.put("stop", cbe.getFinish());
	} else if (Schedule.EventType.START == be.getType()) {
	  row.put("start", be.getStart());
	  row.put("in", timeDelta(nowMs, be.getStart().getExpirationTime()));
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
	row.put("in", timeDelta(nowMs, chunk.getStart().getExpirationTime()));
	row.put("dur", timeDelta(chunk.getStart().getExpirationTime(),
				 chunk.getFinish().getExpirationTime()));
	row.put("start", chunk.getStart());
	row.put("stop", chunk.getFinish());
      }
      return row;
    }

    private String timeDelta(long now, long then) {
      long delta = then - now;
      if (delta > 1000) {
	delta = ((delta + 500) / 1000) * 1000; // round to nearest second
      }
      return StringUtil.timeIntervalToString(delta);
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

    String taskStatsString(int stats[]) {
      List lst = new ArrayList();
      stxt(lst, stats[STAT_ACCEPTED], " scheduled", true);
      stxt(lst, stats[STAT_REFUSED], " refused");
      if (stats == backgroundStats) {
	stxt(lst, backgroundTasks.size(), " running");
      }
      stxt(lst, stats[STAT_WAITING], " waiting");
      stxt(lst, stats[STAT_COMPLETED], " completed");
      stxt(lst, stats[STAT_EXPIRED], " expired");
      if (stats == foregroundStats) {
	stxt(lst, stats[STAT_DROPPED], " delayed");
	stxt(lst, stats[STAT_OVERRUN], " overrun");
	stxt(lst, overrunTasks.size(), " owait");
      }
      return StringUtil.separatedString(lst, ", ");
    }

    private void stxt(List lst, int val, String label) {
      stxt(lst, val, label, false);
    }

    private void stxt(List lst, int val, String label, boolean always) {
      if (always || val != 0) {
	lst.add(val + label);
      }
    }

    private List getSummaryInfo(String key) {
      List res = new ArrayList();
      res.add(new StatusTable.SummaryInfo("Foreground time",
					  ColumnDescriptor.TYPE_TIME_INTERVAL,
					  new Long(totalTime)));
      res.add(new StatusTable.SummaryInfo("Foreground Tasks",
					  ColumnDescriptor.TYPE_STRING,
					  taskStatsString(foregroundStats)));
      res.add(new StatusTable.SummaryInfo("Background Tasks",
					  ColumnDescriptor.TYPE_STRING,
					  taskStatsString(backgroundStats)));
      return res;
    }

    private boolean bittest(int x, int y) {
      return ((x & y) != 0);
    }

    private int sortOrder(int scheme, int ix, boolean isHistory) {
      int res = ix;
      if ((isHistory && bittest(scheme, HIST_REV)) ||
	  (!isHistory && bittest(scheme, PEND_REV))) {
	res = -res;
      }
      if (bittest(scheme, HIST_FIRST) != isHistory) {
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
