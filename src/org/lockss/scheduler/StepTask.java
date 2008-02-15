/*
 * $Id: StepTask.java,v 1.3 2008-02-15 09:14:41 tlipkis Exp $
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
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.util.*;

/** Description of a computation to be scheduled and executed.  Abstract -
 * the methods {@link #step(int)} and {@link #step(int)} must be defined by
 * a subcless.
 * @see StepperTask
 */
public abstract class StepTask extends SchedulableTask {
  private boolean isStepping = false;

  public StepTask(Deadline earliestStart,
		  Deadline latestFinish,
		  long estimatedDuration,
		  TaskCallback callback,
		  Object cookie) {

    super(earliestStart, latestFinish, estimatedDuration, callback, cookie);
  }

  public StepTask(TimeInterval window,
		  long estimatedDuration,
		  TaskCallback callback,
		  Object cookie) {

    this(Deadline.at(window.getBeginTime()), Deadline.at(window.getEndTime()),
	 estimatedDuration, callback, cookie);
  }

  /** Perform a step of the task.
   * @param n a metric for the size of the step to be performed.
   * @return a metric proportional to the amount of work performed.
   */
  abstract public int step(int n);

  //  abstract public boolean isFinished();

  void setStepping(boolean val) {
    isStepping = val;
  }

  public boolean isStepping() {
    return isStepping;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[STask:");
    toStringCommon(sb);
    sb.append("]");
    return sb.toString();
  }
}
