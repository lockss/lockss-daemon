/*
 * $Id: TreeWalkHandler.java,v 1.50 2003-12-12 00:57:19 tlipkis Exp $
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
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;
import org.lockss.crawler.CrawlManager;
import org.lockss.poller.PollSpec;
import org.lockss.app.LockssDaemon;
import org.lockss.scheduler.*;

/**
 * The treewalk thread handler in the NodeManager.  This starts a thread which
 * executes treewalks on a regular basis.
 */
public class TreeWalkHandler {
  public static final String TREEWALK_PREFIX =
      Configuration.PREFIX + "treewalk.";

  /**
   * Configuration parameter name for interval, in ms, between treewalks.
   */
  public static final String PARAM_TREEWALK_INTERVAL =
      TREEWALK_PREFIX + "interval";
  static final long DEFAULT_TREEWALK_INTERVAL = Constants.HOUR;

  /**
   * Configuration parameter name for interval, in ms, between treewalks.
   */
  public static final String PARAM_TREEWALK_START_DELAY =
      TREEWALK_PREFIX + "start.delay";
  static final long DEFAULT_TREEWALK_START_DELAY = Constants.MINUTE;

  /**
   * Configuration parameter name for initial estimate, in ms.
   */
  public static final String PARAM_TREEWALK_INITIAL_ESTIMATE =
      TREEWALK_PREFIX + "initial.estimate";
  static final long DEFAULT_TREEWALK_INITIAL_ESTIMATE = 30 * Constants.MINUTE;
  static final long MIN_TREEWALK_ESTIMATE = 10 * Constants.SECOND;

  /**
   * Configuration parameter name for growing insufficient estimates, as multiplier.
   */
  public static final String PARAM_TREEWALK_ESTIMATE_GROWTH_MULTIPLIER =
      TREEWALK_PREFIX + "estimate.growth.multiplier";
  static final double DEFAULT_TREEWALK_ESTIMATE_GROWTH_MULTIPLIER = 1.5;

  /**
   * Configuration parameter name for padding of estimates, as multiplier.
   */
  public static final String PARAM_TREEWALK_ESTIMATE_PADDING_MULTIPLIER =
      TREEWALK_PREFIX + "estimate.padding.multiplier";
  static final double DEFAULT_TREEWALK_ESTIMATE_PADDING_MULTIPLIER = 1.25;

  /**
   * Configuration parameter name for padding of first estimates, as multiplier.
   */
  public static final String PARAM_TREEWALK_USE_SCHEDULER =
      TREEWALK_PREFIX + "use.scheduler";
  static final boolean DEFAULT_TREEWALK_USE_SCHEDULER = false;

  /**
   * Configuration parameter name for occasional sleep interval, when
   * treewalk rests to maintain a proper load factor (assumes 100% when running).
   */
  public static final String PARAM_TREEWALK_SLEEP_INTERVAL =
      TREEWALK_PREFIX + "sleep.interval";
  static final long DEFAULT_TREEWALK_SLEEP_INTERVAL = Constants.SECOND;
  static final long MIN_TREEWALK_SLEEP_INTERVAL = 500;

  /**
   * Configuration parameter name for overall load factor (assumes 100% when running).
   */
  public static final String PARAM_TREEWALK_LOAD_FACTOR =
      TREEWALK_PREFIX + "load.factor";
  static final double DEFAULT_TREEWALK_LOAD_FACTOR = .90;

  static final double MAX_DEVIATION = 0.4;
  static final long MIN_SCHEDULE_ADJUSTMENT = 10 * Constants.MINUTE;

  NodeManagerImpl manager;
  private LockssDaemon theDaemon;
  private CrawlManager theCrawlManager;
  private ActivityRegulator theRegulator;
  ActivityRegulator.Lock activityLock = null;
  private ArchivalUnit theAu;

  private Logger logger = Logger.getLogger("TreeWalkHandler");

  TreeWalkThread treeWalkThread;
  long treeWalkInterval;
  long topPollInterval;
  long treeWalkTestDuration;
  long startDelay;
  long initialEstimate;
  float estGrowth;
  float estPadding;
  boolean useScheduler;

  long sleepInterval;
  long sleepDuration; // this is calculated
  Deadline nextSleep;
  float loadFactor;

  boolean treeWalkAborted;
  boolean forceTreeWalk = false;
  PollHistory cachedHistory = null;
  NodeState cachedNode = null;

  long treeWalkEstimate = -1;

  Configuration.Callback configCallback;

  TreeWalkHandler(NodeManagerImpl manager, LockssDaemon theDaemon) {
    this.manager = manager;
    theAu = manager.managedAu;
    this.theCrawlManager = theDaemon.getCrawlManager();
    this.theRegulator = theDaemon.getActivityRegulator(theAu);
    this.theDaemon = theDaemon;

    configCallback = new Configuration.Callback() {
      public void configurationChanged(Configuration newConfig,
                                       Configuration oldConfig,
                                       Set changedKeys) {
        setConfig(newConfig, oldConfig);
      }
    };
    Configuration.registerConfigurationCallback(configCallback);
  }

  private void setConfig(Configuration config, Configuration oldConfig) {
    treeWalkInterval = config.getTimeInterval(
        PARAM_TREEWALK_INTERVAL, DEFAULT_TREEWALK_INTERVAL);
    startDelay = config.getTimeInterval(
        PARAM_TREEWALK_START_DELAY, DEFAULT_TREEWALK_START_DELAY);
    initialEstimate = config.getTimeInterval(
        PARAM_TREEWALK_INITIAL_ESTIMATE, DEFAULT_TREEWALK_INITIAL_ESTIMATE);
    estGrowth = config.getPercentage(
        PARAM_TREEWALK_ESTIMATE_GROWTH_MULTIPLIER,
        DEFAULT_TREEWALK_ESTIMATE_GROWTH_MULTIPLIER);
    estPadding = config.getPercentage(
        PARAM_TREEWALK_ESTIMATE_PADDING_MULTIPLIER,
        DEFAULT_TREEWALK_ESTIMATE_PADDING_MULTIPLIER);
    useScheduler = config.getBoolean(PARAM_TREEWALK_USE_SCHEDULER,
        DEFAULT_TREEWALK_USE_SCHEDULER);
    sleepInterval = config.getTimeInterval(
        PARAM_TREEWALK_SLEEP_INTERVAL, DEFAULT_TREEWALK_SLEEP_INTERVAL);
    if (sleepInterval < MIN_TREEWALK_SLEEP_INTERVAL) {
      sleepInterval = DEFAULT_TREEWALK_SLEEP_INTERVAL;
    }
    loadFactor = config.getPercentage(
        PARAM_TREEWALK_LOAD_FACTOR, DEFAULT_TREEWALK_LOAD_FACTOR);
    sleepDuration = calculateSleepDuration(sleepInterval, loadFactor);
    logger.debug3("treeWalkInterval reset to "+treeWalkInterval);
  }

  private long calculateSleepDuration(long interval, double load) {
    // duration must be sufficient for overall load to equal the load factor
    // if interval is assumed 100% and sleep duration is 0%
    return (long)(interval/load) - interval;
  }

/**
 * The full treewalk only proceeds if no new content crawls or top level polls
 * are needed on the content.  As part of checking whether it should execute,
 * it triggers these actions by the CrawlManager and the NodeManager before
 * aborting its treewalk attempt.
 */
  void doTreeWalk() {
    treeWalkAborted = false;

    logger.debug("Attempting tree walk: " + theAu.getName());

    //get expiration time
    long expiration = treeWalkInterval;
    if (getEstimatedTreeWalkDuration() > 0) {
      expiration = 2 * getEstimatedTreeWalkDuration();
    }
    // check with regulator to see if treewalk can proceed
    activityLock = theRegulator.getAuActivityLock(ActivityRegulator.TREEWALK,
                                                expiration);
    if (activityLock != null) {
      try {
        // check with crawl manager
        if (theAu.shouldCrawlForNewContent(manager.getAuState())) {
          treeWalkAborted = true;
          theCrawlManager.startNewContentCrawl(theAu, null, null, activityLock);
          logger.debug("Requested new content crawl.  Aborting...");
        } else {
          // do the actual treewalk
          logger.debug("Tree walk started: " + theAu.getName());
          long startTime = TimeBase.nowMs();
          nodeTreeWalk();
          long elapsedTime = TimeBase.msSince(startTime);
          if (!treeWalkAborted) {
            // only update if a full treewalk occurred.
            updateEstimate(elapsedTime);
          }
        }
      } catch (Exception e) {
        logger.error("Error in treewalk: ", e);
      } finally {
        // release the lock on the treewalk
        // need to check, since it may have been passed to the crawler
        if (activityLock.getActivity()==ActivityRegulator.TREEWALK) {
          activityLock.expire();
        }
        treeWalkAborted = false;
      }
    } else {
      logger.debug2("Treewalk couldn't start due to activity lock.");
    }

    //alert the AuState (it writes through)
    manager.getAuState().setLastTreeWalkTime();
    logger.debug("Tree walk finished.");
  }

  private void nodeTreeWalk() {
    // initialize sleep interval
    nextSleep = Deadline.in(sleepInterval);

    CachedUrlSet cus = theAu.getAuCachedUrlSet();
    recurseTreeWalk(cus);
  }

  /**
   * Recursive function which checks a CUS's children, then checks the state of
   * the given CUS (if no action was taken below it).
   * @param cus the {@link CachedUrlSet} to walk
   * @return true if the treewalk should continue
   */
  boolean recurseTreeWalk(CachedUrlSet cus) {
    if (activityLock.isExpired()) {
      // lost lock, so abort treewalk
      treeWalkAborted = true;
    }
    if (treeWalkAborted) {
      // treewalk has been terminated
      return false;
    }

    boolean pContinue = true;
    // get the node state for the cus
    logger.debug3("Recursing treewalk on cus: "+cus.getUrl());
    NodeState parent = manager.getNodeState(cus);
    // walk the node's children first to process deepest damage first
    Iterator children = cus.flatSetIterator();
    while (children.hasNext()) {
      CachedUrlSetNode node = (CachedUrlSetNode)children.next();
      if (node.getType()==CachedUrlSetNode.TYPE_CACHED_URL_SET) {
        // recurse on the child cus
        //XXX recursion should be aware of total data size, and avoid
        // stack overflow
        // returns false if the treewalk shouldn't continue
        pContinue = recurseTreeWalk((CachedUrlSet)node);
      } else if (node.getType()==CachedUrlSetNode.TYPE_CACHED_URL) {
        // open a new state for the leaf and walk
	Plugin plugin = theAu.getPlugin();
	CachedUrlSetSpec cuss = new RangeCachedUrlSetSpec(node.getUrl());
        NodeState state =
	  manager.getNodeState(plugin.makeCachedUrlSet(theAu, cuss));
        pContinue = checkNodeState(state);
      }
      if (!pContinue) {
        break;
      }
    }

    // if we took no action below here, check this node
    if (pContinue) {
      return checkNodeState(parent);
    }
    return false;
  }

  /**
   * Checks the state of a specific {@link NodeState}.  Returns true if no
   * state was detected (or action to be taken) which would interfere with
   * continuing the treewalk below this node.  If it finds something which needs
   * doing, it saves the node and history and aborts the treewalk.  The action
   * is taken by the thread after 'doTreeWalk()' has returned.
   * @param node the {@link NodeState} to check
   * @return true if treewalk can continue below
   */
  boolean checkNodeState(NodeState node) {
    // if it's time to sleep, sleep and reset deadline
    if ((nextSleep!=null) && (nextSleep.expired())) {
      try {
        Deadline.in(sleepDuration).sleep();
      } catch (InterruptedException ie) { }
      nextSleep = Deadline.in(sleepInterval);
    }

    // check after sleep
    if (treeWalkAborted) {
      // treewalk has been terminated
      return false;
    }

    // at each node, check for recrawl needed
    if (node.getCachedUrlSet().hasContent()) {
      //XXX if (theCrawlManager.shouldRecrawl(managerAu, node)) {
      // then CrawlManager.scheduleBackgroundCrawl()
      // return false;
    }

    // check recent histories to see if something needs fixing
    PollHistory lastHistory = node.getLastPollHistory();
    try {
      if (manager.checkCurrentState(lastHistory, null, node, true)) {
        // mark node for action
        logger.debug3("Found necessary poll. Caching info and aborting treewalk...");
        cachedHistory = lastHistory;
        cachedNode = node;

        // abort treewalk
        treeWalkAborted = true;
        return false;
      }
    } catch (java.io.IOException ie) {
      logger.error("Error in checkCurrentState: ", ie);
    }
    return true;
  }

  /**
   * Checks to see if the treewalk has cached a node to call a poll on.
   * This is done after the treewalk has fully completed, so that the scheduler
   * can schedule properly.
   */
  void callPollIfNecessary() {
    if (cachedNode != null) {
      logger.debug3("Calling poll on node '" +
                    cachedNode.getCachedUrlSet().getUrl() + "'");
      try {
        manager.checkCurrentState(cachedHistory, null, cachedNode, false);
      } catch (Exception e) {
        logger.error("Error calling poll: ", e);
      }
      // null these values for next treewalk
      cachedHistory = null;
      cachedNode = null;
    }
  }


  /**
   * The amount of time, in ms, before the next treewalk should start.
   * @return time in ms
   */
  long timeUntilTreeWalkStart() {
    // if treewalk is forced
    if (forceTreeWalk) {
      logger.debug("Forcing treewalk start-in time of 0.");
      forceTreeWalk = false;
      return 0;
    }
    long lastTreeWalkTime = manager.getAuState().getLastTreeWalkTime();
    long timeSinceLastTW = TimeBase.msSince(lastTreeWalkTime);
    logger.debug3(StringUtil.timeIntervalToString(timeSinceLastTW) +
                  " since last treewalk");
    return (treeWalkInterval > timeSinceLastTW
            ? treeWalkInterval - timeSinceLastTW
            : 0);
  }

  /*
   * Returns the current treewalk average.  INITIAL_ESTIMATE until a treewalk
   * is run.  This estimate is typically padded by ESTIMATE_PADDING_MULTIPLIER.
   * @return the estimate, in ms
   */
  long getEstimatedTreeWalkDuration() {
    // initial estimate from parameter
    if (treeWalkEstimate < 0) {
      treeWalkEstimate = initialEstimate;
    }
    // always at least minimum estimate
    if (treeWalkEstimate < MIN_TREEWALK_ESTIMATE) {
      treeWalkEstimate = MIN_TREEWALK_ESTIMATE;
    }
    return treeWalkEstimate;
  }

  void updateEstimate(long elapsedTime) {
    // no averaging, just padding
    treeWalkEstimate = (long)(estPadding * elapsedTime);
    if (treeWalkEstimate < MIN_TREEWALK_ESTIMATE) {
      treeWalkEstimate = MIN_TREEWALK_ESTIMATE;
    }
  }

  /**
   * Ends the treewalk process.
   */
  public void end() {
    logger.debug3("TreeWalkHandler terminated.");
    if (treeWalkThread!=null) {
      treeWalkThread.end();
    }
    Configuration.unregisterConfigurationCallback(configCallback);
  }

  /**
   * Starts the treewalk process.
   */
  public void start() {
    logger.debug2("TreeWalkHandler started.");
    logger.debug2("Treewalks should happen every "+
                  StringUtil.timeIntervalToString(treeWalkInterval));
    treeWalkAborted = false;
    if (treeWalkThread==null) {
      treeWalkThread = new TreeWalkThread();
      treeWalkThread.start();
    }
  }

  /**
   * Used for testing.  Starts a treewalk if one isn't already running.
   */
  void forceTreeWalk() {
    boolean threadWasNull = false;
    if (treeWalkThread==null) {
      treeWalkThread = new TreeWalkThread();
      threadWasNull = true;
    }
    if (!treeWalkThread.doingTreeWalk) {
      forceTreeWalk = true;
      if (threadWasNull) {
        treeWalkThread.start();
      } else {
        // trigger the semaphore to curtail any wait
        treeWalkThread.treeWalkSemaphore.give();
      }
    }
  }

  /**
   * The thread which handles the treewalk itself.
   */
  class TreeWalkThread extends Thread {
    private boolean goOn = true;
    boolean doingTreeWalk = false;
    private static final long SMALL_SLEEP = Constants.SECOND;
    BinarySemaphore treeWalkSemaphore = new BinarySemaphore();

    public TreeWalkThread() {
      super("TreeWalk: " + theAu.getName());
    }

    public void run() {
      while (!theDaemon.isDaemonRunning()) {
        // if the daemon isn't up yet, do a short sleep
        logger.debug2("Daemon not running yet. Sleeping...");
        try {
          Deadline.in(SMALL_SLEEP).sleep();
        } catch (InterruptedException ie) { }
      }
      long extraDelay = startDelay;
      while (goOn) {
        long start = chooseTimeToRun(extraDelay);
        extraDelay = 0;
        long est = getEstimatedTreeWalkDuration();

        BackgroundTask task = scheduleTreeWalk(start, est);

        // wait on the semaphore (the callback will 'give()')
        try {
          if (treeWalkSemaphore.take(Deadline.in(start + Constants.DAY))) {
	    // semaphore was posted, do treewalk
	    try {
	      doingTreeWalk = true;
	      doTreeWalk();
	    } finally {
	      doingTreeWalk = false;
	    }
	    // tell scheduler we're done.  Necessary only if we ended early
	    // (FINISHED event hasn't happened), but that's almost always
	    // the case, and harmless otherwise.
	    if (task != null) {
	      task.taskIsFinished();
	      task = null;
	    }
	    // Now, if we found a poll to run, run it after background task
	    // has ended.
	    callPollIfNecessary();
	  } else {
	    // semaphore timed out.  log it and cancel task (in finally)
	    // in case it's really still there somewhere
	    logger.error("Semaphore timed out.  Task lost or event delayed");
	  }
	} catch (InterruptedException ie) {
	  // semaphore was interrupted.  Probably exiting, cancel task and
	  // exit if goOn false
	  logger.warning("TreeWalkThread semaphore was interrupted:" + ie);
	} finally {
	  // cancel task if it didn't end normally
	  if (task != null) {
	    task.cancel();
	    task = null;
	  }
	}
      }
    }

    /**
     * This function creates a BackgroundTask to schedule the treewalk.  If the
     * scheduler is not in use, it uses the TimerQueue and returns null.
     * @param start start time
     * @param est estimated length
     * @return a BackgroundTask
     */
    BackgroundTask scheduleTreeWalk(long start, long est) {
      if (useScheduler) {
        BackgroundTask task = null;
        // using SchedService
        logger.debug2("Scheduling of treewalk...");
        SchedService schedSvc = theDaemon.getSchedService();
        TaskCallback cb = new TreeWalkTaskCallback();

        // loop trying to find a time
        while (true) {
          Deadline startDeadline = Deadline.at(start);
          if (logger.isDebug3()) {
            logger.debug3("Trying to schedule for " +
                          startDeadline.toString());
            logger.debug3("Using estimate of " + est + "ms.");
          }
          task = new BackgroundTask(startDeadline, Deadline.at(start + est),
                                    loadFactor, cb) {
            public String getShortText() {
              return "TreeWalk: " + theAu.getName();
            }
          };
          if (schedSvc.scheduleTask(task)) {
            // task is scheduled, your taskEvent callback will be called at the
            // start and end times
            logger.debug2("Scheduled successfully for " +
                          startDeadline.toString());
            break;
          } else {
            if (TimeBase.msUntil(startDeadline.getExpirationTime()) >
                (3 * Constants.WEEK)) {
              // If can't fit it into schedule in next 3 weeks, give up
              // and try again in an hour.  Prevents infinite looping
              // trying to create a schedule.
              logger.debug("Can't schedule, waiting for an hour");
              try {
                Deadline.in(Constants.HOUR).sleep();
              } catch (InterruptedException ie) { }
              start = chooseTimeToRun(0);  // no delay needed
            } else {
              // can't fit into existing schedule.  try adjusting by
              // estimate length
              logger.debug3("Schedule failed.  Trying new time.");
              start += Math.max(est, MIN_SCHEDULE_ADJUSTMENT);
            }
          }
        }
        return task;
      } else {
        // use TimerQueue
        logger.debug2("Setting treewalk in TimerQueue...");
        TimerQueue.schedule(Deadline.at(start),
                            new TreeWalkTimerCallback(), null);
        return null;
      }
    }

    long chooseTimeToRun(long additionalDelay) {
      long timeToStart = timeUntilTreeWalkStart() + additionalDelay;

      // default 'start now'
      long newStart = TimeBase.nowMs() + additionalDelay;
      // if starting in the future, create a randomized value
      if (timeToStart > additionalDelay) {
        long delta = (long)((double)MAX_DEVIATION * timeToStart);
        logger.debug3("Creating a deadline for " +
                      StringUtil.timeIntervalToString(timeToStart) +
                      " with delta of " +
                      StringUtil.timeIntervalToString(delta));
        Deadline deadline = Deadline.inRandomRange(timeToStart,
            timeToStart + delta);
        newStart = deadline.getExpirationTime();
      }
      return newStart;
    }

    public void end() {
      goOn = false;
      treeWalkAborted = true;
    }

    private class TreeWalkTaskCallback implements TaskCallback {
      public void taskEvent(SchedulableTask task, Schedule.EventType event)
          throws Abort {
        if (event == Schedule.EventType.START) {
          if (!doingTreeWalk) {
            // start background activity, not in this thread.
            logger.debug3("Giving semaphore...");
            treeWalkSemaphore.give();
          } else {
            throw new TaskCallback.Abort();
          }
        } else if (event == Schedule.EventType.FINISH) {
          // must stop background activity if it's still running
          logger.debug3("Task finished.");
          // abort current treewalk, but don't stop the thread
          if (doingTreeWalk) {
            logger.debug2("Treewalk task timed out.");
            // free the lock
	    treeWalkAborted = true;
            // couldn't finish in time; pad estimate
            treeWalkEstimate *= estGrowth;
          }
        }
      }
    }

    class TreeWalkTimerCallback implements TimerQueue.Callback {
      /**
       * Called when the timer expires.
       * @param cookie data supplied by caller to schedule()
       */
      public void timerExpired(Object cookie) {
        treeWalkSemaphore.give();
      }
    }

  }
}
