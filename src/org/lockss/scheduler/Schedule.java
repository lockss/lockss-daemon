/*
 * $Id: Schedule.java,v 1.8 2008-02-15 09:14:41 tlipkis Exp $
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
import org.lockss.util.*;

/** A Schedule describes a sequence of task executions.  It is created by a
 * {@link Scheduler}, and interpreted by the TaskRunner. */
public class Schedule {
  protected static Logger log = Logger.getLogger("Schedule");

  private List events;
  private Collection overrunTasks;
  private SortScheduler scheduler;

  public Schedule(List events) {
    this.events = events;
  }

  public Schedule(List events, Collection overrunTasks) {
    this.events = events;
    this.overrunTasks = overrunTasks;
  }

  /** Record the scheduler that created this schedule.  Hack that should
   * either be incorporated in Scheduler interface or removed */
  public void setScheduler(SortScheduler scheduler) {
    this.scheduler = scheduler;
  }

  /** Find the earliest possible time a background task could be
   * scheduled.  This is only a hint; it may not be possible to schedule
   * the task then.
   * @param task a Background task specifying the duration, load factor and
   * earliest desired start time.
   * @return The BackgroundTask with possibly updated start and finish
   * times when it might be schedulable. */
  public BackgroundTask scheduleHint(BackgroundTask task) {
    if (scheduler != null) {
      return scheduler.scheduleHint(task);
    }
    return task;
  }

  /** Return the list of {@link org.lockss.scheduler.Schedule.Event}s */
  public List getEvents() {
    return events;
  }

  /** Return the first event in the link, or null if no events. */
  public synchronized Event getFirstEvent() {
    return events.isEmpty() ? null : (Event)events.get(0);
  }

  /** Remove the first event from the schedule.
   * @param event the expected first event, just to be sure.
   * @return true if the event was removed.
   */
  public synchronized boolean removeFirstEvent(Event event) {
    if (!events.isEmpty() && event.equals((Event)events.get(0))) {
      events.remove(0);
      return true;
    }
    return false;
  }

  /** Remove an event from the schedule.
   * @param event the event to remove.
   * @return true if the event was removed.
   */
  public synchronized boolean removeEvent(Event event) {
    return events.remove(event);
  }

  /** Return the list of overrun tasks */
  public Collection getOverrunTasks() {
    return overrunTasks;
  }

  public static class EventType {
    /** A StepTask is starting, or a BackgroundTask should start. */
    public static final EventType START = new EventType(1);
    /** A StepTask has finished, or a BackgroundTask should end. */
    public static final EventType FINISH = new EventType(2);

    int type;

    private EventType(int type) {
      this.type = type;
    }
    public String toString() {
      switch (type) {
      case 1: return "Start";
      case 2: return "Finish";
      default: return "[Unknown event type]";
      }
    }
  }

  abstract static class Event {
    protected Deadline start;
    private Exception error;

    abstract public boolean isBackgroundEvent();

    abstract public boolean isTaskFinished();

    public Deadline getStart() {
      return start;
    }

    public void setError(Exception e) {
      error = e;
    }

    public Exception getError() {
      return error;
    }
  }

  public static class BackgroundEvent extends Event {
    private BackgroundTask task;
    private EventType type;

    public BackgroundEvent(BackgroundTask task,
			   Deadline start,
			   EventType type) {
      this.task = task;
      this.start = start;
      this.type = type;
    }

    public boolean isBackgroundEvent() {
      return true;
    }

    public BackgroundTask getTask() {
      return task;
    }

    public EventType getType() {
      return type;
    }

    public boolean isTaskFinished() {
      return getTask().isFinished();
    }

    public boolean equals(Object o) {
      if (o instanceof BackgroundEvent) {
	BackgroundEvent oe = (BackgroundEvent)o;
	return (task.equals(oe.getTask()) &&
		start.equals(oe.getStart()) &&
		type == oe.getType());
      }
      return false;
    }

    public int hashCode() {
      throw new UnsupportedOperationException();
    }

    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("[BEvnt:");
      sb.append(type);
      sb.append(" s=");
      sb.append(start.shortString());
      sb.append(", t=");
      sb.append(task);
      sb.append("]");
      return sb.toString();
    }

  }

  /** An executable chunk of computation that has been scheduled.  If task
   is a StepThread, this represents a chunk of time to be spent stepping.
   If task is a BackgroundTask, this */
  public static class Chunk extends Event {

    private Deadline finish;
    private StepTask task;
    private long runTime;
    private Interval interval = null;
    private boolean taskEnd = false;

    /** Create a Chunk that says to run task <code>task</code> from time
     * <code>start</code> to time <code>finish</code>.  Due to other system
     * load, it is expected to actually get <code>runTime</code> ms of time
     * during this period. */
    public Chunk(StepTask task,
		 Deadline start,
		 Deadline finish,
		 long runTime) {
      this.task = task;
      this.start = start;
      this.finish = finish;
      this.runTime = runTime;
    }

    public boolean isBackgroundEvent() {
      return false;
    }

    public StepTask getTask() {
      return task;
    }

    public Deadline getStart() {
      return start;
    }

    public Deadline getFinish() {
      return finish;
    }

    public boolean isTaskFinished() {
      return getTask().isFinished();
    }

    public Interval getInterval() {
      if (interval == null) {
	interval = new Interval(start, finish);
      }
      return interval;
    }

    public long getRunTime() {
      return runTime;
    }

    public double getLoadFactor() {
      return ((double)runTime) / ((double)finish.minus(start));
    }

    public void setTaskEnd() {
      taskEnd = true;
    }

    public boolean isTaskEnd() {
      return taskEnd;
    }

    public boolean extend(Chunk ch) {
      if (isContiguous(ch)) {
	if (log.isDebug3()) {
	  log.debug3("extending " + this + ", " + ch);
	}
	finish = ch.getFinish();
	runTime += ch.getRunTime();
	taskEnd = ch.isTaskEnd();
	interval = null;
	return true;
      } else {
	return false;
      }
    }

    public boolean isContiguous(Chunk ch) {
      return task == ch.getTask() && finish.equals(ch.getStart());
    }

    public boolean equals(Object o) {
      if (o instanceof Chunk) {
	Chunk oc = (Chunk)o;
	return (task.equals(oc.getTask()) &&
		taskEnd == oc.isTaskEnd() &&
		start.equals(oc.getStart()) &&
		finish.equals(oc.getFinish()) &&
		runTime == oc.getRunTime());
      }
      return false;
    }

    public int hashCode() {
      throw new UnsupportedOperationException();
    }

    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("[Chunk:");
      if (isTaskEnd()) {
	sb.append("E");
      }
      sb.append(" s=");
      sb.append(start.shortString());
      sb.append(", e=");
      sb.append(finish.shortString());
      sb.append(", run=");
      sb.append(runTime);
      sb.append(", t=");
      sb.append(task);
      sb.append("]");
      return sb.toString();
    }
  }
}
