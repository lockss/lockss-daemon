/*
 * $Id: SortScheduler.java,v 1.6.2.1 2004-10-05 19:54:32 tlipkis Exp $
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

package org.lockss.scheduler;

import java.util.*;
import org.lockss.util.*;
import org.lockss.daemon.*;

/** This scheduler assigns tasks to intervals preferentially based on their
 * finish time */
public class SortScheduler implements Scheduler {
  protected static Logger log = Logger.getLogger("Scheduler");

  static final String PREFIX = Configuration.PREFIX + "scheduler.";
  static final String PARAM_MAX_BACKGROUND_LOAD = PREFIX + "maxBackgroundLoad";
  static final double DEFAULT_MAX_BACKGROUND_LOAD = 1.0;

  final double maxBackgroundLoad;

  Collection rawTasks;
  List tasks;
  SchedInterval intervals[];
  int nIntervals;
  List ranges = new ArrayList();
  Set unscheduledTasks;
  boolean scheduleCreated;
  Deadline scheduledAt;

  private void resetVars() {
    rawTasks = null;
    intervals = null;
    nIntervals = 0;
    ranges = new ArrayList();
    unscheduledTasks = null;
    scheduleCreated = false;
    scheduledAt = null;
  }

  /** Create a scheduler */
  public SortScheduler() {
    Configuration config = Configuration.getCurrentConfig();
    maxBackgroundLoad = config.getPercentage(PARAM_MAX_BACKGROUND_LOAD,
					     DEFAULT_MAX_BACKGROUND_LOAD);
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
    tasks = new ArrayList(rawTasks.size());
    if (scheduledAt == null) {
      // can happen during testing
      scheduledAt = Deadline.in(0);
    }
    Deadline now = scheduledAt ;
    for (Iterator iter = rawTasks.iterator(); iter.hasNext(); ) {
      SchedulableTask rawTask = (SchedulableTask)iter.next();
      if (!rawTask.isProperWindow()) {
	log.warning("Task has improper window: " + rawTask);
	throw new IllegalArgumentException("Task has improper window");
      }

      // make a TaskData whose start time is no earlier than now
      TaskData td;
      if (rawTask.getEarlistStart().before(now)) {
	td = new TaskData(rawTask, now);
      } else {
	td = new TaskData(rawTask);
      }
      // fail now if task can't finish
      if (td.getWSize() <= 0) {
	log.warning("initTasks: expired task: " + td);
	return false;
      }
      tasks.add(td);
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
      initIntervalTaskLists() &&
      scheduleAll();
    return scheduleCreated;
  }

  public Collection getTasks() {
    return rawTasks;
  }

  public Schedule getSchedule() {
    if (!scheduleCreated) {
      throw new IllegalStateException("Attempt to get nonexistent schedule");
    }
    List events = new ArrayList();
    for (int xintr = 0; xintr < nIntervals; xintr++) {
      SchedInterval intrvl = intervals[xintr];
      intrvl.addEvents(events);
    }
    if (log.isDebug2()) {
      for (Iterator iter = events.iterator(); iter.hasNext(); ) {
	Schedule.Event event = (Schedule.Event)iter.next();
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
    if (tasks == null || tasks.isEmpty()) {
      intervals = null;
      return true;
    }
    TreeSet boundaries = new TreeSet();
    for (Iterator iter = tasks.iterator(); iter.hasNext(); ) {
      TaskData td = (TaskData)iter.next();
      boundaries.add(td.getWStart());
      boundaries.add(td.getWEnd());
    }
    nIntervals = boundaries.size() - 1;
    intervals = new SchedInterval[nIntervals];
    int ix = 0;
    Iterator biter = boundaries.iterator();
    Deadline lower = (Deadline)biter.next();
    while (biter.hasNext()) {
      Deadline upper = (Deadline)biter.next();
      SchedInterval intrvl = new SchedInterval(lower, upper);
      intervals[ix++] = intrvl;
      lower = upper;
    }
    if (log.isDebug3()) {
      log.debug3(nIntervals + " intervals: " + arrayString(intervals));
    }
    return true;
  }

  // Assign candidate step tasks to intervals
  // Adjust interval loadFactor to account for background tasks
  // Reject if sum of background loadFactor > 100% in any interval
  // XXX doesn't yet - Reject if overall background loadFactor > param
  boolean initIntervalTaskLists() {
    if (intervals == null || intervals.length == 0) {
      return true;
    }
    long cumBackTime = 0;		// cumulative background time
    for (int xintr = 0; xintr < nIntervals; xintr++) {
      SchedInterval intrvl = intervals[xintr];
      // tk - could make this more efficient by first sorting the tasks by
      // start time
      for (Iterator iter = tasks.iterator(); iter.hasNext(); ) {
	TaskData td = (TaskData)iter.next();
	if (!intrvl.isDisjoint(td.getWindow())) {
	  if (!td.getWindow().subsumes(intrvl)) {
	    String msg = "Intersecting task " + td.task + " doesn't subsume "
	      + intrvl;
	    log.error(msg);
	    throw new RuntimeException(msg);
	  }
	  if (td.task.isBackgroundTask()) {
	    BackgroundTask btask = (BackgroundTask)td.task;
	    intrvl.loadFactor -= btask.getLoadFactor();
	    if (log.isDebug3())
	      log.debug3("reduced " + intrvl + " by " + btask.getLoadFactor());
	    if (intrvl.loadFactor < 0.0) {
	      if (log.isDebug2())
		log.debug2("initIntrTskList: loadFactor < 0: " + intrvl);
	      return false;
	    }
	    intrvl.backgroundTaskList.add(td);
	  } else {
	    intrvl.competingTaskList.add(td);
	    if (td.getWEnd().equals(intrvl.getEnd())) {
	      intrvl.endingTaskList.add(td);
	    }
	  }
	}
      }
      intrvl.cumBackTime = cumBackTime;
      cumBackTime += intrvl.backgroundTime();
    }
    return true;
  }

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

  /** Struct representing an interval in which tasks can be placed, and
   * during which task order is immaterial, because no task window
   * boundaries occur during the interval. */
  class SchedInterval extends Interval {
    long duration;			// length of interval
    long unscheduledTime;		// available cpu time (adjusted for
					// load factor)
    long cumBackTime;			// cumulative scheduled background
					// time at the start of this
					// interval, for calculating
					// average background time
    List competingTaskList = new ArrayList();
    List backgroundTaskList = new ArrayList();
    List endingTaskList = new ArrayList();
    TaskData[] endingTasks;
    List chunks;			// scheduled chunks (result)

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
	events.addAll(chunks);
      }
      addEndingBackgroundEvents(events);
    }

    void addStartingBackgroundEvents(List events) {
      for (Iterator iter = backgroundTaskList.iterator(); iter.hasNext(); ) {
	TaskData td = (TaskData)iter.next();
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
      for (Iterator iter = backgroundTaskList.iterator(); iter.hasNext(); ) {
	TaskData td = (TaskData)iter.next();
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
      unscheduledTime = (long)(duration * loadFactor);
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
	  long duration = (long)(taskTime / loadFactor);
	  if (log.isDebug3())
	    log.debug3("add task " + tix + "(" + stask.cookie +
		       "), time = " + taskTime + ", duration = " + duration);
	  td.unschedTaskTime -= taskTime;
	  unscheduledTime -= taskTime;
	  // these times may have past if creating a schedule with tasks
	  // starting immediately, so suppres unreasonable deadline
	  // warning.  All passed-in deadlines have been checked for sanity.
	  Schedule.Chunk chunk = new
	    Schedule.Chunk(stask,
			   Deadline.restoreDeadlineAt(chunkStart +
						      chunkOffset),
			   Deadline.restoreDeadlineAt(chunkStart +
						      chunkOffset + duration),
			   taskTime);
	  if (td.unschedTaskTime <= 0) {
	    chunk.setTaskEnd();
	  }
	  chunks.add(chunk);
	  chunkOffset += duration;
	  unscheduledTasks.remove(stask);
	}
      }
      // if any tasks ending at this interval have unschedTaskTime, fail
      for (int eix = 0; eix < endingTasks.length; eix++) {
	TaskData etd = endingTasks[eix];
	if (etd.unschedTaskTime > 0) {
	  log.debug("No schedule found");
	  return false;
	}
      }
      return true;
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
    SchedInterval intervals[];		// the entire set of intervals
    int first;				// index of first interval in the range
    int last;				// index of last interval in the range
    TaskData tasks[];			// tasks to schedule during this range
    int ntasks;				// number of tasks

    IntervalRange(SchedInterval intervals[], int first, int last) {
      this.intervals = intervals;
      this.first = first;
      this.last = last;
    }

    void initIntervalTasks() {
      Set taskset = new HashSet();
      for (int ix = first; ix <= last; ix++) {
	SchedInterval intrvl = intervals[ix];
	taskset.addAll(intrvl.competingTaskList);
	intrvl.endingTasks =
	  (TaskData[])intrvl.endingTaskList.toArray(EMPTY_TASKDATA_ARRAY);
      }
      ntasks = taskset.size();
      log.debug(ntasks + " tasks");
      tasks = (TaskData[])taskset.toArray(EMPTY_TASKDATA_ARRAY);
      Arrays.sort(tasks, TaskData.endTimeComparator());
      if (log.isDebug3()) log.debug3("tasks: " + arrayString(tasks));
    }

    boolean scheduleIntervalRange() {
      log.debug("sched " + first + " - " + last + " of " + intervals.length +
		" intervals");
      initIntervalTasks();

      for (int iix = first; iix <= last; iix++) {
	SchedInterval intrvl = intervals[iix];
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
