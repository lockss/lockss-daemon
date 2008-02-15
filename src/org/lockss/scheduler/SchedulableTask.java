/*
 * $Id: SchedulableTask.java,v 1.14 2008-02-15 09:14:41 tlipkis Exp $
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

import java.io.*;
import java.util.*;
import java.text.*;
import org.lockss.daemon.*;
import org.lockss.util.*;

/** Description of a computation to be scheduled and executed */
public class SchedulableTask {
  //  protected static Logger log = Logger.getLogger("Task");

  Deadline earliestStart;
  Deadline latestFinish;
  Interval window;

  // estimate, estRem
  long origEst;
  protected TaskCallback callback;
  protected Object cookie;

  long timeUsed = 0;
  protected long unaccountedTime = 0;
  protected Exception e;
  boolean overrunAllowed = false;
  boolean isAccepted = false;
  boolean hasBeenNotified = false;
  boolean hasStarted = false;
  TaskRunner runner = null;

  int schedSeq = -1;
  Date schedDate;
  Date finishDate = null;
  boolean isDropped = false;

  public SchedulableTask(Deadline earliestStart,
		     Deadline latestFinish,
		     long estimatedDuration,
		     TaskCallback callback,
		     Object cookie) {

    window = new Interval(earliestStart, latestFinish);
    this.earliestStart = earliestStart;
    this.latestFinish = latestFinish;
    this.origEst = estimatedDuration;
    this.callback = callback;
    this.cookie = cookie;
  }

  public SchedulableTask(TimeInterval interval,
		     long estimatedDuration,
		     TaskCallback callback,
		     Object cookie) {
    this(Deadline.at(interval.getBeginTime()),
	 Deadline.at(interval.getEndTime()),
	 estimatedDuration, callback, cookie);
  }

  public Interval getWindow() {
    return window;
  }

  public Deadline getEarliestStart() {
    return earliestStart;
  }

  public Deadline getLatestFinish() {
    return latestFinish;
  }

  public Date getFinishDate() {
    return finishDate;
  }

  public Date getSchedDate() {
    return schedDate;
  }

  public int getSchedSeq() {
    return schedSeq;
  }

  public boolean isProperWindow() {
    return earliestStart.before(latestFinish);
  }

  public long getOrigEst() {
    return origEst;
  }

  public synchronized long getTimeUsed() {
    return timeUsed + unaccountedTime;
  }

  /** Gets called periodically while running task.  Used to update global
   * statistics */
  protected synchronized void updateStats() {
    timeUsed += unaccountedTime;
    unaccountedTime = 0;
  }

  public synchronized void setUnaccountedTime(long time) {
    unaccountedTime = time;
  }

  public Exception getExcption() {
    return e;
  }

  public long curEst() {
    long t = origEst - timeUsed;
    return (t > 0 ? t : 0);
  }

  public boolean isFinished() {
    return finishDate != null;
  }

  protected void setFinished() {
    finishDate = TimeBase.nowDate();
  }

  public boolean isDropped() {
    return isDropped;
  }

  void setDropped() {
    isDropped = true;
  }

  public boolean hasOverrun() {
    return timeUsed > origEst;
  }

  public boolean isExpired() {
    return latestFinish.expired();
  }

  public void setOverrunAllowed(boolean val) {
    overrunAllowed = val;
  }

  public boolean isOverrunAllowed() {
    return overrunAllowed;
  }

  public void setAccepted(boolean val) {
    isAccepted = val;
  }

  public boolean isAccepted() {
    return isAccepted;
  }

  public boolean hasBeenNotified() {
    return hasBeenNotified;
  }

  void setNotified() {
    hasBeenNotified = true;
  }

  public boolean hasStarted() {
    return hasStarted;
  }

  void setStarted() {
    hasStarted = true;
  }

  void setTaskRunner(TaskRunner runner) {
    this.runner = runner;
  }

  TaskRunner getTaskRunner() {
    return runner;
  }

  public void cancel() {
    if (runner != null) {
      runner.cancelTask(this);
    }
  }

  public String getShortText() {
    if (schedSeq != -1) {
      return "Task " + schedSeq;
    } else {
      return "Task";
    }
  }

  /** Return true iff this is a background task.  By default, tasks are not
   * background tasks */
  public boolean isBackgroundTask() {
    return false;
  }

  /** Comparator for ordering tasks by ending deadline */
  private static final Comparator _latestFinishComparator =
    new Comparator() {
      public int compare(Object o1, Object o2) {
	SchedulableTask t1 = (SchedulableTask)o1;
	SchedulableTask t2 = (SchedulableTask)o2;
	int res = t1.getLatestFinish().compareTo(t2.getLatestFinish());
	if (res == 0) {
	  // If the end times are equal, sort by schedule order.
	  res = t1.schedSeq - t2.schedSeq;
	  if (res == 0) {
	    // Don't ever return 0, as these are used in TreeSets, which
	    // consider sort order equality to mean object equality

	    // tk - This is still not sufficient, as java doesn't guarantee
	    // that different objects have different hash codes.  As
	    // currently used these will always have a schedSeq by the time
	    // they're put in a TreeSet, but this needs a real solution
	    res = System.identityHashCode(t2) - System.identityHashCode(t1);
	  }
	}
	return res;
      }};

  /** Return a comparator for ordering tasks by ending deadline */
  public static Comparator latestFinishComparator() {
    return _latestFinishComparator;
  }

  static final DateFormat dfDeadline = new SimpleDateFormat("HH:mm:ss");

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[Task:");
    toStringCommon(sb);
    sb.append("]");
    return sb.toString();
  }

  public void toStringCommon(StringBuffer sb) {
    if (isFinished()) {
      sb.append("F");
    } else if (isExpired()) {
      sb.append("X");
    }
    if (hasOverrun()) {
      sb.append("O");
    } else if (isOverrunAllowed()) {
      sb.append("A");
    }
    if (schedSeq != -1) {
      sb.append("#");
      sb.append(schedSeq);
    }
    if (cookie instanceof String) {
      sb.append(" \"");
      sb.append(cookie);
      sb.append("\"");
    }
    sb.append(" ");
    if (timeUsed != 0) {
      sb.append(StringUtil.timeIntervalToString(timeUsed));
      sb.append("/");
    }
    sb.append(StringUtil.timeIntervalToString(origEst));
    sb.append(" between ");
    sb.append(earliestStart.shortString());
    sb.append(" and ");
    sb.append(latestFinish.shortString());
    //        sb.append(dfDeadline.format(latestFinish.getExpiration()));
  }

}


