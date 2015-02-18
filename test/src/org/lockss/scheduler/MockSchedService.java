/*
 * $Id$
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
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.config.*;

public class MockSchedService extends SchedService {

  public MockSchedService() {
  }

  /**
   * Start the compute service.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
  }

  public void stopService() {
  }

  /** Attempt to add a task to the schedule.
   * @param task the new task
   * @return true if the task was added to the schedule.
   */
  public boolean scheduleTask(SchedulableTask task) {
    throw new UnsupportedOperationException();
  }

  /** Return true iff the task could be scheduled, but doesn't actually
   * schedule the task.
   * @param task the hypothetical task
   * @return true if such a task could be inserted in the schedule
   */
  public boolean isTaskSchedulable(SchedulableTask task) {
    throw new UnsupportedOperationException();
  }

  /** Find the earliest possible time a background task could be scheduled.
   * This is only a hint; it may not be possible to schedule the task then.
   * @param task a Background task specifying the duration, load factor and
   * earliest desired start time.
   * @return A BackgroundTask (possibly the same one) with possibly updated
   * start and finish times when it might be schedulable. */
  public BackgroundTask scheduleHint(BackgroundTask task) {
    throw new UnsupportedOperationException();
  }

  /** Return true if the SchedService has nothing to do.  Useful in unit
   * tests. */
  public boolean isIdle() {
    throw new UnsupportedOperationException();
  }
}
