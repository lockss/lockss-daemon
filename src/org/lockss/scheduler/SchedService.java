/*
 * $Id: SchedService.java,v 1.3 2003-11-13 11:16:16 tlipkis Exp $
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
import java.io.*;
import java.util.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.util.*;
import org.lockss.app.*;

/**
 * SchedService schedules and executes requests for computations.
 * Currently a single TaskRunner is used; this could be changed to take
 * advantage of multiprocessors.
 */
public class SchedService extends BaseLockssManager {
  protected static Logger log = Logger.getLogger("SchedService");

  private TaskRunner runner = null;

  public SchedService() {}

  /**
   * Start the compute service.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    super.startService();
    log.debug("startService()");
    runner = new TaskRunner(new TaskRunner.SchedulerFactory () {
	public Scheduler createScheduler(Collection tasks) {
	  return new SortScheduler(tasks);
	}});
    runner.init();
    getDaemon().getStatusService().
      registerStatusAccessor("TaskQ", runner.getStatusAccessor());
  }

  /**
   * Stop the compute service
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    // TODO: checkpoint here.
    if (runner != null) {
      getDaemon().getStatusService().unregisterStatusAccessor("TaskRunner");
      runner.stopThread();
    }
    runner = null;

    super.stopService();
  }

  protected void setConfig(Configuration config, Configuration prevConfig,
			   Set changedKeys) {
  }

  public boolean scheduleTask(SchedulableTask task) {
    checkRunner();
    return runner.scheduleTask(task);
  }

  /** Return true iff the task could be scheduled, but doesn't actually
   * schedule the task.
   * @param task the hypothetical task
   * @return true if such a task could be inserted in the schedule
   */
  public boolean isTaskSchedulable(SchedulableTask task) {
    checkRunner();
    return runner.isTaskSchedulable(task);
  }

  /** Return true if the SchedService has nothing to do.  Useful in unit
   * tests. */
  public boolean isIdle() {
    return runner.isIdle();
  }

  private void checkRunner() {
    if (runner == null) {
      throw
	new IllegalStateException("SchedService has not been initialized");
    }
  }

  /** Exception thrown if a task could not be completed by its deadline. */
  public static class Timeout extends Exception {
    public Timeout(String msg) {
      super(msg);
    }
  }

  /** Exception thrown if a task does not finish within its estimated run
   * time. */
  public static class Overrun extends Exception {
    public Overrun(String msg) {
      super(msg);
    }
  }

}
