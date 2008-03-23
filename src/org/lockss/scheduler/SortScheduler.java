/*
 * $Id: SortScheduler.java,v 1.15 2008-03-23 00:53:52 tlipkis Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.scheduler;

import java.util.*;

import org.lockss.config.*;
import org.lockss.util.*;

/** This scheduler assigns tasks to intervals preferentially based on their
 * finish time */
public class SortScheduler implements Scheduler {
  protected static Logger log = Logger.getLogger("Scheduler");

  static final String PREFIX = Configuration.PREFIX + "scheduler.";

  /** Maximum cumulative load, over time, that background tasks are allowed
   * to consume */
  public static final String PARAM_MAX_BACKGROUND_LOAD =
    PREFIX + "maxBackgroundLoad";
  public static final double DEFAULT_MAX_BACKGROUND_LOAD = 1.0;

  /** Fraction of CPU time that is assumed to be consumed by threads other
   * than StepThread */
  public static final String PARAM_OVERHEAD_LOAD = PREFIX + "overheadLoad";
  public static final double DEFAULT_OVERHEAD_LOAD = 0.2;

  // Vars that should not be reset between invocations
  final double maxBackgroundLoad;
  final double overheadLoad;

  // Vars that should be reset between invocations
  Collection<SchedulableTask> rawTasks;
  TaskData tdarr[];
  SchedInterval intervals[];
  int nIntervals;
  List ranges = new ArrayList();
  Set unscheduledTasks;
  boolean scheduleCreated;
  Deadline scheduledAt;

  private void resetVars() {
    rawTasks = null;
    tdarr = null;
    intervals = null;
    nIntervals = 0;
    ranges = new ArrayList();
    unscheduledTasks = null;
    scheduleCreated = false;
    scheduledAt = null;
  }

  /** Create a scheduler */
  public SortScheduler() {
    Configuration config = CurrentConfig.getCurrentConfig();
    maxBackgroundLoad = config.getPercentage(PARAM_MAX_BACKGROUND_LOAD,
					     DEFAULT_MAX_BACKGROUND_LOAD);
    overheadLoad = config.getPercentage(PARAM_OVERHEAD_LOAD,
					DEFAULT_OVERHEAD_LOAD);
  }

  /** Create a scheduler to schedule the collection of tasks */
  SortScheduler(Collection tasks) {
    this();
    rawTasks = tasks;
  }

  void setTasks(Collection tasks) {
    resetVars();
    rawTasks = tasks;
  }

  /** Store list of tasks to schedule, check validity, move times in the
   * past up to now. */
  private boolean initTasks() {
    unscheduledTasks = new HashSet(rawTasks);
    tdarr = new TaskData[rawTasks.size()];
    if (scheduledAt == null) {
      // can happen during testing
      scheduledAt = Deadline.in(0);
    }
    Deadline now = scheduledAt ;
    int tdix = 0;
    for (SchedulableTask rawTask : rawTasks) {
      if (!rawTask.isProperWindow()) {
	log.warning("Task has improper window: " + rawTask);
	throw new IllegalArgumentException("Task has improper window");
      }

      // make a TaskData whose start time is no earlier than now
      TaskData td;
      if (rawTask.getEarliestStart().before(now)) {
	td = new TaskData(rawTask, now);
      } else {
	td = new TaskData(rawTask);
      }
      // fail now if task can't finish
      if (td.getWSize() <= 0) {
	log.warning("initTasks: expired task: " + td);
	return false;
      }
      tdarr[tdix++] = td;
    }
    scheduleCreated = false;
    return true;
  }

  public boolean createSchedule(Collection tasks) {
    setTasks(tasks);
    return createSchedule();
  }

  public boolean createSchedule() {
    if (log.isDebug2())
      log.debug2("createSchedule: " + rawTasks.size() + " tasks");
    scheduledAt = Deadline.in(0);
    scheduleCreated = initIntervals() &&
      scheduleAll();
    return scheduleCreated;
  }

  public Collection<SchedulableTask> getTasks() {
    return rawTasks;
  }

  /** Set all the tasks in this scheduler as having been accepted */
  public void acceptTasks() {
    for (SchedulableTask task : rawTasks) {
      task.setAccepted(true);
    }
  }

  public Schedule getSchedule() {
    if (!scheduleCreated) {
      throw new IllegalStateException("Attempt to get nonexistent schedule");
    }
    List<Schedule.Event> events = new ArrayList();
    for (int xintr = 0; xintr < nIntervals; xintr++) {
      SchedInterval intrvl = intervals[xintr];
      intrvl.addEvents(events);
    }
    if (log.isDebug2()) {
      for (Schedule.Event event : events) {
	log.debug2("  " + event.toString());
      }
    }
    Schedule res = new Schedule(events, unscheduledTasks);
    res.setScheduler(this);
    return res;
  }

  // find all task boundaries and create intervals between them
  boolean initIntervals() {
    if (!initTasks()) {
      return false;
    }
    if (tdarr == null || tdarr.length == 0) {
      intervals = null;
      return true;
    }
    TreeMap bdTasks = new TreeMap();
    for (TaskData td : tdarr) {
      getBoundaryTasks(bdTasks, td.getWStart()).addStartingTask(td);
      getBoundaryTasks(bdTasks, td.getWEnd()).addEndingTask(td);
    }
    nIntervals = bdTasks.size() - 1;
    intervals = new SchedInterval[nIntervals];

    long cumBackTime = 0;		// cumulative background time
    Set<TaskData> cumBackTasks = new HashSet();
    Set<TaskData> cumForeTasks = new HashSet();
    boolean seenNewTask = false;
    int ix = 0;
    Iterator beIter = bdTasks.entrySet().iterator();
    Map.Entry lentry = (Map.Entry)beIter.next();
    Deadline lower = (Deadline)lentry.getKey();
    BoundaryTasks lbt = (BoundaryTasks)lentry.getValue();
    while (beIter.hasNext()) {
      Map.Entry uentry = (Map.Entry)beIter.next();
      Deadline upper = (Deadline)uentry.getKey();
      BoundaryTasks ubt = (BoundaryTasks)uentry.getValue();
      SchedInterval intrvl = new SchedInterval(lower, upper);
      intervals[ix++] = intrvl;
      if (!cumBackTasks.isEmpty()) {
	for (TaskData btd : cumBackTasks) {
	  BackgroundTask btask = (BackgroundTask)btd.task;
	  intrvl.loadFactor -= btask.getLoadFactor();
	  intrvl.backgroundTaskList.add(btd);
	}
      }
      if (lbt.startingTasks != null) {
	for (TaskData std : lbt.startingTasks) {
	  if (!std.isAccepted()) {
	    seenNewTask = true;
	  }
	  if (std.task.isBackgroundTask()) {
	    BackgroundTask btask = (BackgroundTask)std.task;
	    intrvl.loadFactor -= btask.getLoadFactor();
	    if (log.isDebug3())
	      log.debug3("reduced " + intrvl + " by " + btask.getLoadFactor());
	    intrvl.backgroundTaskList.add(std);
	    cumBackTasks.add(std);
	  } else {
	    cumForeTasks.add(std);
	  }
	}
      }
      intrvl.competingTaskList = new ArrayList(cumForeTasks);
      intrvl.seenNewTask = seenNewTask;
      if (intrvl.loadFactor < 0.0) {
	if (log.isDebug2())
	  log.debug2("initIntrTskList: loadFactor < 0: " + intrvl);
	return false;
      }

      if (ubt.endingTasks != null) {
	for (TaskData etd : ubt.endingTasks) {
	  if (etd.task.isBackgroundTask()) {
	    cumBackTasks.remove(etd);
	  } else {
	    intrvl.addEndingTask(etd);
	    cumForeTasks.remove(etd);
	  }
	}
      }
      intrvl.cumBackTime = cumBackTime;
      cumBackTime += intrvl.backgroundTime();

      lower = upper;
      lbt = ubt;
    }
    if (log.isDebug3()) {
      log.debug3(nIntervals + " intervals: " + arrayString(intervals));
    }
    return true;
  }

  BoundaryTasks getBoundaryTasks(TreeMap bdTasks, Deadline time) {
    BoundaryTasks bt = (BoundaryTasks)bdTasks.get(time);
    if (bt == null) {
      bt = new BoundaryTasks();
      bdTasks.put(time, bt);
    }
    return bt;
  }

  static class BoundaryTasks {
    List<TaskData> startingTasks;
    List<TaskData> endingTasks;

    void addStartingTask(TaskData td) {
      if (startingTasks == null) {
	startingTasks = new ArrayList();
      }
      startingTasks.add(td);
    }

    void addEndingTask(TaskData td) {
      if (endingTasks == null) {
	endingTasks = new ArrayList();
      }
      endingTasks.add(td);
    }
  }

  // Divide into groups of intervals with no overlapping tasks, schedule
  // each group
  boolean scheduleAll() {
    SchedInterval prev = null;
    int xfirst = -1;
    for (int ix = 0; ix < nIntervals; ix++) {
      SchedInterval intrvl = intervals[ix];
      if (prev == null) {
	if (intrvl.hasTasks()) {
	  xfirst = ix;
	  prev = intrvl;
	}
      } else {
	if ((!intrvl.hasTasks()) ||
	    CollectionUtil.isDisjoint(prev.competingTaskList,
				      intrvl.competingTaskList)) {
	  if (! scheduleIntervalRange(xfirst, ix - 1)) {
	    return false;
	  }
	  prev = null;
	  xfirst = -1;
	} else {
	  prev = intrvl;
	}
      }
    }
    if (prev != null) {
      if (!scheduleIntervalRange(xfirst, nIntervals - 1)) {
	return false;
      }
    }
    return true;
  }

  boolean scheduleIntervalRange(int first, int last) {
    IntervalRange rng = new IntervalRange(intervals, first, last);
    ranges.add(rng);
    return rng.scheduleIntervalRange();
  }

  /** Using the previously created intervals, find the earliest possible
   * time a background task could be scheduled.  This is only a hint; it
   * may not be possible to schedule the task then.
   * @param task a Background task specifying the duration, load factor and
   * earliest desired start time.
   * @return The BackgroundTask (the same one) with possibly updated start
   * and finish times when it might be schedulable. */
  public BackgroundTask scheduleHint(BackgroundTask task) {
    long start = task.getStart().getExpirationTime();
    long finish = task.getFinish().getExpirationTime();
    long duration = finish - start;
    double loadFactor = task.getLoadFactor();
    for (int xintr = 0; xintr < nIntervals; xintr++) {
      SchedInterval intrvl = intervals[xintr];
      if (intrvl.getEnd().getExpirationTime() <= start) {
	continue;
      }
      if (intrvl.getBegin().getExpirationTime() >= finish) {
	break;
      }
      if (intrvl.loadFactor < loadFactor) {
	// can't run at all during this interval, next possible start is
	// end of interval
	start = intrvl.getEnd().getExpirationTime();
	finish = start + duration;
      }
      double cumBackLoad = 0.0;
      if (intrvl.cumBackTime != 0) {
	cumBackLoad = ((double)intrvl.cumBackTime /
		       ((double)intrvl.getBegin().minus(scheduledAt)));
      }
      if (cumBackLoad >= maxBackgroundLoad) {
	// average background load too high, push start back until load
	// would be low enough
	start =
	  Math.max(start, (scheduledAt.getExpirationTime() +
			   (long)((intrvl.cumBackTime +
				   intrvl.backgroundTime()) /
				  maxBackgroundLoad)));
	finish = start + duration;
	if (log.isDebug2()) {
	  log.debug2("Hint, delaying from " + intrvl.getEnd().shortString() +
		     " to " + Deadline.at(start).shortString());
	}
      }
    }
    task.setInterval(Deadline.at(start), Deadline.at(finish));
    return task;
  }

  static protected String arrayString(Object[] a) {
    return StringUtil.separatedString(a, ", ");
  }
  static protected String arrayString(long[] a) {
    return StringUtil.separatedString(objArray(a), ", ");
  }

  static protected Object[] objArray(long[] a) {
    Object[] o = new Object[a.length];
    for (int ix = 0; ix < a.length; ix++) {
      o[ix] = new Long(a[ix]);
    }
    return o;
  }

  static protected Object[] objArray(int[] a) {
    Object[] o = new Object[a.length];
    for (int ix = 0; ix < a.length; ix++) {
      o[ix] = new Integer(a[ix]);
    }
    return o;
  }

  //********************* Nested classes *********************

  /** Struct to keep track of task time remaining to be scheduled.  (Need
      per task, and can't modify actual SchedulableTask. */
  static class TaskData {
    private static int nextSeq = 0;	 // See _endTimeComparator.

    SchedulableTask task;
    Interval window;
    long totalTime;
    long unschedTaskTime;		 // remaining unscheduled time
    int seq = nextSeq++;		 // See _endTimeComparator.

    TaskData(SchedulableTask t, Deadline earliestStart) {
      task = t;
      window = new Interval(earliestStart, t.getLatestFinish());
      reset();
    }

    TaskData(SchedulableTask t) {
      task = t;
      window = t.getWindow();
      reset();
    }

    /** Reset unschedTaskTime to totalTime */
    void reset() {
      this.totalTime = task.curEst();
      unschedTaskTime = totalTime;
      if (task.isDropped()) {
	unschedTaskTime = 0;
      }
    }

    /** Return (possibly modified) execution window */
    Interval getWindow() {
      return window;
    }

    /** Return (possibly modified) start of execution window */
    Deadline getWStart() {
      return (Deadline)window.getLB();
    }

    /** Return (possibly modified) end of execution window */
    Deadline getWEnd() {
      return (Deadline)window.getUB();
    }

    /** Return size of (possibly modified) execution window */
    long getWSize() {
      return getWEnd().minus(getWStart());
    }

    public boolean isAccepted() {
      return task.isAccepted();
    }

    public String toString() {
      return "[TD: t=" + task + "]";
    }

    public int hashCode() {
      return task.hashCode();
    }

    public boolean equals(Object o) {
      if (o instanceof TaskData) {
	TaskData td = (TaskData)o;
	return task.equals(td.task);
      }
      return false;
    }

    /** Comparator for ordering tasks by ending deadline */
    private static final Comparator _endTimeComparator =
      new Comparator() {
	public int compare(Object o1, Object o2) {
	  TaskData t1 = (TaskData)o1;
	  TaskData t2 = (TaskData)o2;
	  int res = t1.getWEnd().compareTo(t2.getWEnd());
	  if (res == 0) {
	    // If the end times are equal, sort by creation order.  A
	    // consistent (repeatable) sort order is useful so unit tests
	    // can predict which of several possible schedules will be
	    // created.
	    res = t1.seq - t2.seq;
	  }
	  return res;
	}};

    /** Return a comparator for ordering tasks by ending deadline */
    public static Comparator endTimeComparator() {
      return _endTimeComparator;
    }
  }

  /** Struct representing an interval in which tasks can be placed, during
   * which no task window boundaries occur. */
  class SchedInterval extends Interval {
    long duration;			// length of interval
    long unscheduledTime;		// available cpu time (adjusted for
					// load factor)
    long cumBackTime;			// cumulative scheduled background
					// time at the start of this
					// interval, for calculating
					// average background time
    List competingTaskList;
    List<TaskData> backgroundTaskList = new ArrayList();
    List endingTaskList;
    TaskData[] endingTasks;
    List<Schedule.Chunk> chunks;	// scheduled chunks (result)
    boolean seenNewTask = false;

    double loadFactor = 1.0;

    SchedInterval(Deadline begin, Deadline end) {
      super(begin, end);
      duration = end.minus(begin);
      if (duration < 0) {
	throw new RuntimeException("Illegal SchedInterval, "
				   + end + " before " + begin);
      }
    }

    Deadline getBegin() {
      return (Deadline)getLB();
    }

    Deadline getEnd() {
      return (Deadline)getUB();
    }

    long backgroundTime() {
      return (long)(duration * (1.0 - loadFactor));
    }

    /** Return true if any tasks are available to run during this interval */
    boolean hasTasks() {
      return !competingTaskList.isEmpty();
    }

    void addEndingTask(TaskData td) {
      if (endingTaskList == null) {
	endingTaskList = new ArrayList();
      }
      endingTaskList.add(td);
    }

    /** Add Schedule.Events for all background events and chunks in this
     * interval.  Starting background tasks first (because they must happen
     * at the beginning of the interval), then step chunks, then ending
     * background events * (because they must happen at the end of the
     * interval).
     * @param events the event list to add to.
     */
    void addEvents(List events) {
      addStartingBackgroundEvents(events);
      if (chunks != null) {
	for (Schedule.Chunk chunk : chunks) {
	  addChunk(events, chunk);
	}
      }
      addEndingBackgroundEvents(events);
    }

    void addStartingBackgroundEvents(List events) {
      for (TaskData td : backgroundTaskList) {
	BackgroundTask task = (BackgroundTask)td.task;
	// important to use td start, not task start, to ensure produce
	// start event for task whose start is in the past.
	if (td.getWStart().equals(getBegin())) {
	  Schedule.BackgroundEvent event =
	    new Schedule.BackgroundEvent(task, getBegin(),
					 Schedule.EventType.START);
	  events.add(event);
	  unscheduledTasks.remove(task);
	}
      }
    }

    void addEndingBackgroundEvents(List events) {
      for (TaskData td : backgroundTaskList) {
	BackgroundTask task = (BackgroundTask)td.task;
	if (task.getFinish().equals(getEnd())) {
	  Schedule.BackgroundEvent event =
	    new Schedule.BackgroundEvent(task, getEnd(),
					 Schedule.EventType.FINISH);
	  events.add(event);
	}
      }
    }


    /** Add intersecting tasks to interval until interval is full.
	@return true iff no ending tasks have remaining unscheduled time.
    */
    boolean scheduleInterval(TaskData tasks[]) {
      double foreLoad = Math.min(loadFactor, 1.0 - overheadLoad);
      unscheduledTime = (long)(duration * foreLoad);
      chunks = new ArrayList();
      long chunkOffset = 0;
      long chunkStart = getBegin().getExpirationTime();
      if (log.isDebug3()) log.debug3("Scheduling " + toString());
      int ntasks = tasks.length;
      for (int tix = 0;
	   tix < ntasks && unscheduledTime > 0;
	   tix++) {
	TaskData td = tasks[tix];
	StepTask stask = (StepTask)td.task;
	if (log.isDebug3())
	  log.debug3("examining task " + tix + "(" + stask.cookie +
		     "), unsched = " + td.unschedTaskTime);
	if (td.unschedTaskTime > 0 && !isDisjoint(td.getWindow())) {
	  long taskTime = Math.min(unscheduledTime, td.unschedTaskTime);
	  long taskDur = (long)(taskTime / foreLoad);
	  if (log.isDebug3())
	    log.debug3("add task " + tix + "(" + stask.cookie +
		       "), time = " + taskTime + ", duration = " + taskDur);

	  // these times may have past if creating a schedule with tasks
	  // starting immediately, so suppres unreasonable deadline
	  // warning.  All passed-in deadlines have been checked for sanity.
	  Deadline start =
	    Deadline.restoreDeadlineAt(chunkStart + chunkOffset);
	  // roundoff error may leave chunkStart + chunkOffset + taskDur
	  // short of the end of the interval.  The this task got all the
	  // remaining time in the intern, ensure it ends exactly at the
	  // end of the interval
	  Deadline stop =
	    (taskTime == unscheduledTime
	     ? getEnd()
	     : Deadline.restoreDeadlineAt(chunkStart + chunkOffset + taskDur));

	  Schedule.Chunk chunk =
	     new Schedule.Chunk(stask, start, stop, taskTime);
	  td.unschedTaskTime -= taskTime;
	  unscheduledTime -= taskTime;
	  if (td.unschedTaskTime <= 0) {
	    chunk.setTaskEnd();
	  }
	  chunks.add(chunk);
	  chunkOffset += taskDur;
	  unscheduledTasks.remove(stask);
	}
      }
      // if any tasks ending at this interval have unschedTaskTime, fail
      if (endingTasks != null) {
	for (TaskData etd : endingTasks) {
	  if (etd.unschedTaskTime > 0) {
	    if (!seenNewTask) {
	      // don't fail to generate a schedule because no longer time
	      // to satisfy already-accepted tasks
	      if (log.isDebug2()) {
		log.debug("No longer time to finish, proceeding: " + etd);
	      }
	    } else {
	      log.debug("No schedule found: " + etd);
	      return false;
	    }
	  }
	}
      }
      return true;
    }

    void addChunk(List<Schedule.Event> events, Schedule.Chunk chunk) {
      if (!events.isEmpty()) {
	Schedule.Event prevEvent = events.get(events.size() - 1);
	if (prevEvent instanceof Schedule.Chunk) {
	  Schedule.Chunk prevChunk = (Schedule.Chunk)prevEvent;
	  if (prevChunk.extend(chunk)) {
	    return;
	  }
	}
      }
      events.add(chunk);
    }

    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("[");
      sb.append(getBegin().shortString());
      sb.append("-");
      sb.append(getEnd().shortString());
      if (loadFactor != 1.0) {
	sb.append("*");
	sb.append(loadFactor);
      }
      sb.append(",");
      sb.append(unscheduledTime);
      sb.append("]");
      return sb.toString();
    }

  }

  // prototype for List.toArray() calls
  static final TaskData[] EMPTY_TASKDATA_ARRAY = new TaskData[0];

  /** A set of contiguous intervals over which the scheduling algorithm
   * runs.  (This exists in order to break divisible scheduling problems
   * into smaller, non-divisible pieces, to reduce combinatorics.  In this
   * version of the scheduler, which sorts but does no other combinatorics,
   * it helps only a little.) */
  static class IntervalRange {
    SortScheduler.SchedInterval intervals[]; // the entire set of intervals
    int first;				// index of first interval in the range
    int last;				// index of last interval in the range
    TaskData tasks[];			// tasks to schedule during this range
    int ntasks;				// number of tasks

    IntervalRange(SortScheduler.SchedInterval intervals[],
		  int first, int last) {
      this.intervals = intervals;
      this.first = first;
      this.last = last;
    }

    void initIntervalTasks() {
      Set taskset = new HashSet();
      for (int ix = first; ix <= last; ix++) {
	SortScheduler.SchedInterval intrvl = intervals[ix];
	taskset.addAll(intrvl.competingTaskList);
	if (intrvl.endingTaskList != null) {
	  intrvl.endingTasks =
	    (TaskData[])intrvl.endingTaskList.toArray(EMPTY_TASKDATA_ARRAY);
	}
      }
      ntasks = taskset.size();
      log.debug2(ntasks + " tasks");
      tasks = (TaskData[])taskset.toArray(EMPTY_TASKDATA_ARRAY);
      Arrays.sort(tasks, TaskData.endTimeComparator());
      if (log.isDebug3()) log.debug3("tasks: " + arrayString(tasks));
    }

    boolean scheduleIntervalRange() {
      log.debug2("sched " + first + " - " + last + " of " + intervals.length +
		" intervals");
      initIntervalTasks();

      for (int iix = first; iix <= last; iix++) {
	SortScheduler.SchedInterval intrvl = intervals[iix];
	if (!intrvl.scheduleInterval(tasks)) {
	  if (log.isDebug2()) {
	    log.debug2("schedIntrvlRng: can't schedule interval: " +
		       iix +"; " + intrvl);
	  }
	  return false;
	}
      }
      log.debug2("Schedule found");
      return true;
    }
  }

}
