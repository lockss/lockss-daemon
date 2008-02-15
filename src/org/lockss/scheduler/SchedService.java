/*
 * $Id: SchedService.java,v 1.14 2008-02-15 09:14:41 tlipkis Exp $
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
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.config.*;
import org.lockss.util.*;

/**
 * SchedService schedules and executes requests for computations.
 * Currently a single TaskRunner is used; this could be changed to take
 * advantage of multiprocessors.
 */
public class SchedService extends BaseLockssDaemonManager {
  protected static Logger log = Logger.getLogger("SchedService");

  public static final String PREFIX = Configuration.PREFIX + "sched.";

  /**
   * Initial offset into future at which to begin newly created schedule,
   * to allow for time scheduler takes to run.  Not yet implemented.
   */
  public static final String PARAM_INITIAL_OFFSET =
    PREFIX + "initialOffset";
  private static final long DEFAULT_INITIAL_OFFSET =
    1 * Constants.MINUTE;

  private TaskRunner runner = null;
  private long initialOffset = DEFAULT_INITIAL_OFFSET;

  public SchedService() {}

  /**
   * Start the compute service.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    super.startService();
    log.debug("startService()");
    runner = new TaskRunner(new TaskRunner.SchedulerFactory () {
	public Scheduler createScheduler() {
	  return new SortScheduler();
	}});
    runner.initService(getDaemon());
    runner.startService();
    getApp().getStatusService().
      registerStatusAccessor("SchedQ", runner.getStatusAccessor());
  }

  /**
   * Stop the compute service
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    // TODO: checkpoint here.
    if (runner != null) {
      getApp().getStatusService().unregisterStatusAccessor("TaskRunner");
      getApp().getStatusService().unregisterStatusAccessor("SchedQ");
      runner.stopService();
    }
    runner = null;

    super.stopService();
  }

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    if (changedKeys.contains(PREFIX)) {
      initialOffset =
	config.getTimeInterval(PARAM_INITIAL_OFFSET,
			       DEFAULT_INITIAL_OFFSET);
    }
  }

  /** Attempt to add a task to the schedule.
   * @param task the new task
   * @return true if the task was added to the schedule.
   */
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

  /** Find the earliest possible time a background task could be scheduled.
   * This is only a hint; it may not be possible to schedule the task then.
   * @param task a Background task specifying the duration, load factor and
   * earliest desired start time.
   * @return A BackgroundTask (possibly the same one) with possibly updated
   * start and finish times when it might be schedulable. */
  public BackgroundTask scheduleHint(BackgroundTask task) {
    checkRunner();
    return runner.scheduleHint(task);
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
