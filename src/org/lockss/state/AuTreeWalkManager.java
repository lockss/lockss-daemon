/*
 * $Id: AuTreeWalkManager.java,v 1.14 2006-08-07 18:47:48 tlipkis Exp $
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

package org.lockss.state;

import java.util.*;
import EDU.oswego.cs.dl.util.concurrent.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.scheduler.*;

/** Per-AU treewalk manager; schedules a background task for the treewalk
 * and responds to task start & finish events by executing the treewalk
 * (in a pool thread)  */
public class AuTreeWalkManager
  extends BaseLockssDaemonManager implements LockssAuManager, TaskCallback {
  private static Logger log = Logger.getLogger("TreeWalkManager");

  ArchivalUnit au;
  TreeWalkManager twm;
  SchedService schedSvc;

  // The currently scheduled or running task.  If null, there is no task
  // scheduld, but there should be a timer event pending which will attempt
  // to schedule a task.  There is only one task present at a time; the
  // current task is always completed or cancelled before another task is
  // scheduled.
  volatile BackgroundTask curTask;
  volatile TreeWalkRunner runningRunner = null;

  long treeWalkEstimate = -1;
  long lastStartTime = 0;

  AuTreeWalkManager(ArchivalUnit au) {
    this.au = au;
  }

  public void startService() {
    super.startService();
    LockssDaemon daemon = getDaemon();
    twm = daemon.getTreeWalkManager();
    schedSvc = daemon.getSchedService();

    scheduleFirst();
  }

  public void stopService() {
    cancelTask();
    super.stopService();
  }

  synchronized void cancelTask() {
    if (curTask != null) {
      curTask.cancel();
      curTask = null;
    }
  }

  void removeTask(BackgroundTask task) {
    // tell scheduler we're done.  Necessary only if we ended early
    // (FINISHED event hasn't happened), but that's almost always
    // the case.  DO NOT do this while holding lock - it calls the scheduler
    task.taskIsFinished();
    synchronized (this) {
      if (curTask == task) {
	log.debug3("Removing curTask");
	curTask = null;
      } else {
	log.debug("curTask != task; " + curTask + ", " + task);
      }
    }
  }

  public void setAuConfig(Configuration auConfig) {
  }

  /**
   * The runnable object which calls the treewalk itself.  A new instance
   * is created for each treewalk attempt.
   */
  class TreeWalkRunner extends LockssRunnable {
    BackgroundTask task;
    volatile TreeWalker walker;
    volatile boolean walkerRunning = false;
    volatile Thread thread;

    public TreeWalkRunner(ArchivalUnit au, BackgroundTask task) {
      super("TreeWalk: " + au.getName());
      this.task = task;
    }

    public void lockssRun() {
      boolean happened = false;
      try {
	thread = Thread.currentThread();
	Deadline finishBy = task.getFinish();
	setPriority(TreeWalkManager.PRIORITY_PARAM_TREEWALK,
		    TreeWalkManager.PRIORITY_DEFAULT_TREEWALK);
	if (task.isFinished() || finishBy.expired()) {
	  return;
	}
	triggerWDogOnExit(true);
//       nowRunning();
	startWDog(TreeWalkManager.WDOG_PARAM_TREEWALK,
		  TreeWalkManager.WDOG_DEFAULT_TREEWALK);
	happened = runWalker(finishBy);
      } finally {
	// we are done running, in the sense that we should no longer be
	// aborted if the FINISHED taskEvent occurs.  (In fact, we're about
	// to cause that.)
	runningRunner = null;
	try {
	  removeTask(task);
	  // Do any action that was deferred until after the task ended
	  if (happened) {
	    walker.doDeferredAction();
	  }
	} catch (Exception e) {
	  log.warning("Unexpected exception", e);
	}
	stopWDog();
	if (happened) {
	  scheduleNext();
	} else {
	  scheduleSoon();
	}
      }
      triggerWDogOnExit(false);
    }

    public boolean runWalker(Deadline finishBy) {
      boolean happened = false;
      walker = newWalker(getDaemon(), au);
      walkerRunning = true;
      walker.setWDog(this);
      try {
	long startTime = TimeBase.nowMs();
	happened = walker.doTreeWalk(finishBy);
	if (walker.didFullTreewalk()) {
	  // only update if a full treewalk occurred.
	  updateEstimate(TimeBase.msSince(startTime));
	}
	return happened;
      } catch (Exception e) {
	log.warning("Treewalker threw", e);
	return false;
      } finally {
	// don't allow the walker to be aborted after this point. (We may
	// still call its doDeferredAction().)
	walkerRunning = false;
      }
    }

    public synchronized void abort() {
      log.debug2("Runner.abort()");
      triggerWDogOnExit(false);
      if (walkerRunning && walker != null) {
	walker.abort();
	if (thread != null) {
	  thread.interrupt();
	}
      }
    }
  }

  void scheduleFirst() {
    // allow the au to override the global value
    TypedEntryMap auMap = au.getProperties();
    long delay = auMap.getLong(TreeWalkManager.PARAM_TREEWALK_START_DELAY,
			       twm.paramStartDelay);
    scheduleIn(delay);
  }

  void scheduleNext() {
    scheduleTaskAfter(chooseTimeToRun());
  }

  void scheduleSoon() {
    scheduleIn(twm.paramExecuteFailRetryTime);
  }

  void scheduleIn(long delay) {
    scheduleTaskAfter(TimeBase.nowMs() + delay);
  }

  /**
   * Try to schedule the treewalk for some time after earliestStart.  Sets
   * curTask if successful, otherwise echedules a TimerQueue event to try
   * again in a little while.
   * @param earliestStart earliest time to run
   */
  void scheduleTaskAfter(final long earliestStart) {
    long start = Math.max(TimeBase.nowMs(), earliestStart);
    long est = getEstimatedTreeWalkDuration();
    BackgroundTask task = null;

    // loop trying to find a time
    while (true) {
      Deadline startDeadline = Deadline.at(start);
      // Create task.  Callback object is 'this'.
      task = new BackgroundTask(startDeadline, Deadline.at(start + est),
				twm.paramLoadFactor, this) {
	  public String getShortText() {
	    return "TreeWalk: " + au.getName();
	  }
	};
      task.setLatestStart(Deadline.in(twm.paramMaxFutureSched));
      if (log.isDebug3()) {
	log.debug3("Scheduling " + task);
      }
      if (schedSvc.scheduleTask(task)) {
	// task is scheduled. this.taskEvent() will be called at the start
	// and end times
	log.debug2("Scheduled successfully for " +
		      task.getStart().shortString());
	synchronized (this) {
	  if (curTask == null) {
	    curTask = task;
	  } else {
	    log.debug("Someone else beat us to it, cancelling");
	    task.cancel();
	  }
	}
	break;
      } else {
	// Can't fit into existing schedule.  Try for a later time.
	startDeadline = task.getStart();
	start = startDeadline.getExpirationTime();
	if (TimeBase.msUntil(startDeadline.getExpirationTime()) <
	    twm.paramMaxFutureSched) {
	  log.debug3("Couldn't schedule.  Trying new time.");
	  start += Math.max(est, twm.paramSchedFailIncrement);
	} else {
	  // If can't fit it into schedule in next 3 weeks, wait and
	  // try again in an hour.
	  // XXX This is partly because the scheduler can get into a
	  // state where no schedule can be created until the task
	  // runner gets a chance to run.  Looping continuously here
	  // can prevent the task runner from running.  Even when
	  // that's fixed, waiting and starting over seems like a good
	  // idea.
	  log.debug("Can't schedule, waiting for an hour");
	  TimerQueue.schedule(Deadline.in(twm.paramSchedFailRetryTime),
			      new TimerQueue.Callback() {
				public void timerExpired(Object cookie) {
				  scheduleTaskAfter(earliestStart);
				}}
			      , null);
	  break;
	}
      }
    }
  }

  /**
   * Picks the time to next run the treewalk
   * @return start time
   */
  long chooseTimeToRun() {
    long now = TimeBase.nowMs();
    // Don't completely rely on time in state, which might not get updated
    // in unit test
    long lastTreeWalkTime =
      Math.min(AuUtil.getAuState(au).getLastTreeWalkTime(),
	       lastStartTime);
    if ((now - lastTreeWalkTime) > twm.paramTreeWalkIntervalMax) {
      return now + Constants.HOUR;
    }
    try {
      Deadline target =
	Deadline.atRandomRange(lastTreeWalkTime + twm.paramTreeWalkIntervalMin,
			       lastTreeWalkTime + twm.paramTreeWalkIntervalMax);
      return target.getExpirationTime();
    } catch (Exception e) {
      log.warning("Computing deadline, min: " + twm.paramTreeWalkIntervalMin +
		  ", max: " + twm.paramTreeWalkIntervalMax, e);
      return now + twm.paramTreeWalkIntervalMin;
    }
  }

  /*
   * Returns the current treewalk average.  INITIAL_ESTIMATE until a treewalk
   * is run.  This estimate is typically padded by ESTIMATE_PADDING_MULTIPLIER.
   * @return the estimate, in ms
   */
  long getEstimatedTreeWalkDuration() {
    // initial estimate from parameter
    if (treeWalkEstimate < 0) {
      treeWalkEstimate = twm.paramInitialEstimate;
    }
    // always return at least minimum estimate
    return treeWalkEstimate > twm.paramMinSchedDuration
      ? treeWalkEstimate : twm.paramMinSchedDuration;
  }

  /**
   * Update the treewalk duration estimate for next time.  Pads by
   * 'paramEstPadding', and sets a minimum of paramMinSchedDuration.
   * @param elapsedTime the time the treewalk actually took
   */
  void updateEstimate(long elapsedTime) {
    // no averaging, just padding
    treeWalkEstimate = (long)(twm.paramEstPadding * elapsedTime);
    if (treeWalkEstimate < twm.paramMinSchedDuration) {
      treeWalkEstimate = twm.paramMinSchedDuration;
    }
  }

  void startThread(BackgroundTask task) {
    TreeWalkRunner runner = newRunner(au, task);
    lastStartTime = task.getStart().getExpirationTime();
    try {
      executeRunner(runner);
      runningRunner = runner;
    } catch (InterruptedException e) {
      // exiting, just return
    } catch (RuntimeException e) {
      log.warning("RuntimeException starting pool thread", e);
      removeTask(task);
      scheduleSoon();
    }
  }

  protected void executeRunner(TreeWalkRunner runner)
      throws InterruptedException {
    twm.execute(runner);
  }

  protected TreeWalkRunner newRunner(ArchivalUnit au, BackgroundTask task) {
    return new TreeWalkRunner(au, task);
  }

  protected TreeWalker newWalker(LockssDaemon daemon, ArchivalUnit au) {
    return new V1TreeWalkImpl(daemon, au);
  }

  /**
   * Treewalk task event handler
   */
  public synchronized void taskEvent(SchedulableTask task,
				     Schedule.EventType event) {
    if (task != curTask) {
      // XXX this happens routinely.  Eliminate possibly redundant callback?
      if (log.isDebug2()) {
	log.debug("Ignoring unexpected taskEvent(" + task + ", " + event +
		  "), curTask: " + curTask);
      }
      return;
    }
    if (event == Schedule.EventType.START) {
      log.debug3("Treewalk task start.");
      if (runningRunner == null) {
	// start background activity in a thread.
	startThread((BackgroundTask)task);
      } else {
	log.warning("Impossible: treewalk already running.  Igoring event.");
      }
    } else if (event == Schedule.EventType.FINISH) {
      log.debug3("Treewalk task finish.");
      // must stop background activity if it's still running
      if (runningRunner != null) {
	log.debug2("Treewalk still running, aborting.");
	// couldn't finish in time; pad estimate
	// update state *before* telling thread to abort
	treeWalkEstimate *= twm.paramEstGrowth;
	TreeWalkRunner runner = runningRunner;
	runningRunner = null;
	runner.abort();
      }
    }
  }

  /**
   * Factory class to create AuTreeWalkManager
   */
  public static class Factory implements LockssAuManager.Factory {
    public LockssAuManager createAuManager(ArchivalUnit au) {
      return new AuTreeWalkManager(au);
    }
  }
}
