/*
 * $Id: TreeWalkHandler.java,v 1.2 2003-03-27 21:47:17 troberts Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.crawler.CrawlManager;
import org.lockss.poller.PollSpec;
import org.lockss.daemon.Configuration;

/**
 * The treewalk thread handler in the NodeManager.  This starts a thread which
 * executes treewalks on a regular basis.
 */
public class TreeWalkHandler {
  public static final String TREEWALK_PREFIX =
      Configuration.PREFIX + "treewalk.";

  /**
   * Configuration parameter name for duration, in ms, for which the treewalk
   * estimation should run.

  public static final String PARAM_TREEWALK_ESTIMATE_DURATION =
      TREEWALK_PREFIX + "estimate.duration";
  static final long DEFAULT_TREEWALK_ESTIMATE_DURATION = Constants.SECOND;
   */

  /**
   * Configuration parameter name for interval, in ms, between treewalks.
   */
  public static final String PARAM_TREEWALK_INTERVAL =
      TREEWALK_PREFIX + "interval";
  static final long DEFAULT_TREEWALK_INTERVAL = Constants.HOUR;

  static final double MAX_DEVIATION = 0.1;

  NodeManagerImpl manager;
  private static CrawlManager theCrawlManager;
  private ArchivalUnit theAu;

  private Logger logger = Logger.getLogger("TreeWalkHandler");

  TreeWalkThread treeWalkThread;
  long treeWalkInterval;
  long topPollInterval;
  long treeWalkTestDuration;

  boolean treeWalkAborted;

//  long treeWalkEstimate = -1;
 // EstimationThread estThread = null;

  Configuration.Callback configCallback;

  TreeWalkHandler(NodeManagerImpl manager, CrawlManager theCrawlManager) {
    this.manager = manager;
    this.theCrawlManager = theCrawlManager;
    theAu = manager.managedAu;

    configCallback = new Configuration.Callback() {
      public void configurationChanged(Configuration oldConfig,
                                       Configuration newConfig,
                                       Set changedKeys) {
        setConfig(newConfig, oldConfig);
      }
    };
    Configuration.registerConfigurationCallback(configCallback);
  }

  private void setConfig(Configuration config, Configuration oldConfig) {
    treeWalkInterval = config.getTimeInterval(
        PARAM_TREEWALK_INTERVAL, DEFAULT_TREEWALK_INTERVAL);
    logger.debug3("treeWalkInterval reset to "+treeWalkInterval);
//    treeWalkTestDuration = config.getTimeInterval(
  //      PARAM_TREEWALK_ESTIMATE_DURATION, DEFAULT_TREEWALK_ESTIMATE_DURATION);
  }

  /**
   * The full treewalk only proceeds if no new content crawls or top level polls
   * are needed on the content.  As part of checking whether it should execute,
   * it triggers these actions by the CrawlManager and the NodeManager before
   * aborting its treewalk attempt.
   */
  void doTreeWalk() {
    logger.debug("Attempting tree walk: "+theAu.getName());
    if (!theCrawlManager.isCrawlingAU(theAu, null, null)) {
//      long startTime = TimeBase.nowMs();

      NodeState topNode = manager.getNodeState(theAu.getAUCachedUrlSet());
      Iterator activePolls = topNode.getActivePolls();
      // only continue if no top level poll scheduled or running
      if (!activePolls.hasNext()) {
        // query the AU if a top level poll should be started
        if (theAu.shouldCallTopLevelPoll(manager.getAuState())) {
          manager.callTopLevelPoll();
        } else {
          logger.debug("Tree walk started: "+theAu.getName());
          nodeTreeWalk();
       //   long elapsedTime = TimeBase.nowMs() - startTime;
       //   updateEstimate(elapsedTime);
        }
      }
    }
    //alert the AuState
    manager.getAuState().setLastTreeWalkTime();
    logger.debug("Tree walk finished.");
  }

  private void nodeTreeWalk() {
    CachedUrlSet cus = theAu.getAUCachedUrlSet();
    recurseTreeWalk(cus);
  }

  /**
   * Recursive function which checks the state of a given CUS, then calls
   * itself on the CUS's children.  It aborts without recursing if an action
   * was taken on the CUS itself.
   * @param cus the {@link CachedUrlSet} to walk
   */
  void recurseTreeWalk(CachedUrlSet cus) {
    if (treeWalkAborted) {
      // treewalk has been terminated
      return;
    }
    // get the node state for the cus
    logger.debug3("Recursing treewalk on cus: "+cus.getUrl());
    NodeState state = manager.getNodeState(cus);
    // walk the node
    boolean pContinue = checkNodeState(state);
    if (!pContinue) {
      // this node is busy, so abort without walking its children
      return;
    }
    Iterator children = cus.flatSetIterator();
    while (children.hasNext()) {
      CachedUrlSetNode node = (CachedUrlSetNode)children.next();
      if (node.getType()==CachedUrlSetNode.TYPE_CACHED_URL_SET) {
        // recurse on the child cus
        //XXX recursion should be aware of total data size, and avoid
        // stack overflow
        recurseTreeWalk((CachedUrlSet)node);
      } else if (node.getType()==CachedUrlSetNode.TYPE_CACHED_URL) {
        // open a new state for the leaf and walk
        state = manager.getNodeState(
            theAu.makeCachedUrlSet(node.getUrl(), null, null));
        checkNodeState(state);
      }
    }
  }

  /**
   * Checks the state of a specific {@link NodeState}.  Returns true if no
   * state was detected (or action taken) which would interfere with continuing
   * the treewalk below this node.
   * @param node the {@link NodeState} to check
   * @return true if treewalk can continue below
   */
  boolean checkNodeState(NodeState node) {
    // determine if there are active polls
    Iterator polls = node.getActivePolls();
    while (polls.hasNext()) {
      PollState poll = (PollState)polls.next();
      if (poll.isActive()) {
        // if there are active polls, don't interfere
        return false;
      }
    }
    // at each node, check for recrawl needed
    if (node.getCachedUrlSet().hasContent()) {
      // if (theCrawlManager.shouldRecrawl(managerAu, node)) {
      // then CrawlManager.scheduleBackgroundCrawl()
      // return false;
    }
    // check recent histories to see if something needs fixing
    PollHistory lastHistory = node.getLastPollHistory();
    if (lastHistory != null) {
      // give the last history to the manager to check for consistency,
      // and take appropriate action
      if (manager.checkLastHistory(lastHistory, node)) {
        return false;
      }
    }
    return true;
  }

  /**
   * The amount of time, in ms, before the next treewalk should start.
   * @return time in ms
   */
  long timeUntilTreeWalkStart() {
    long lastTreeWalkTime = manager.getAuState().getLastTreeWalkTime();
    long timeSinceLastTW = TimeBase.nowMs() - lastTreeWalkTime;
    logger.debug3(timeSinceLastTW+" since last treewalk");
    logger.debug("Treewalks should happen every "+treeWalkInterval);
    return treeWalkInterval - timeSinceLastTW;
  }

  /**
   * Ends the treewalk process.
   */
  public void end() {
    logger.debug3("TreeWalkHandler terminated.");
    if (treeWalkThread!=null) {
      treeWalkThread.end();
    }
//    if (estThread!=null) {
  //    estThread.end();
    //}
    Configuration.unregisterConfigurationCallback(configCallback);
  }

  /**
   * Starts the treewalk process.
   */
  public void start() {
    logger.debug3("TreeWalkHandler started.");
    if (treeWalkThread==null) {
      treeWalkThread = new TreeWalkThread();
      treeWalkThread.start();
    }
  }

  /**
   * The thread which handles the treewalk itself.
   */
  class TreeWalkThread extends Thread {
    private boolean goOn = true;
    Deadline deadline;

    public void run() {
      while (goOn) {
        long timeToStart = timeUntilTreeWalkStart();
        if (timeToStart <= 0) {

	  treeWalkAborted = false;
          doTreeWalk();
        }
        else {
          long delta = (long) ( (double) MAX_DEVIATION * timeToStart);
          logger.debug3("Creating a deadline for " + timeToStart +
                        " with delta of " + delta);
          deadline = Deadline.inRandomRange(timeToStart, timeToStart + delta);
          try {
            deadline.sleep();
          } catch (InterruptedException ie) { }
        }
      }
    }

    public void end() {
      goOn = false;
      treeWalkAborted = true;
      if (deadline != null) {
        deadline.expire();
      }
    }
  }

  // Estimation code removed until needed.

  /*
   * Starts calculating a new estimated treewalk duration by starting up an
   * EstimationThread.

  synchronized void calculateEstimatedTreeWalkDuration() {
    if ((estThread==null) && (treeWalkEstimate==-1)) {
      logger.debug("Estimating treewalk for: " + theAu.getName());
      estThread = new EstimationThread();
      estThread.start();
    } else {
      if (estThread!=null) {
        logger.debug3("Calculation already in progress.");
      } else {
        logger.debug3("Treewalk estimation already concluded.");
      }
    }
  }

  /*
   * Returns the current treewalk estimate.
   * @return the estimate, in ms

  long getEstimatedTreeWalkDuration() {
    return treeWalkEstimate;
  }

  void updateEstimate(long elapsedTime) {
    if (treeWalkEstimate==-1) {
      treeWalkEstimate = elapsedTime;
    } else {
      // average with current estimate
      treeWalkEstimate = (treeWalkEstimate + elapsedTime) / 2;
    }
  }

  // for testing only
  EstimationThread getTestThread() {
    return new EstimationThread();
  }
 */
  /**
   * The thread which handles treewalk estimation

  class EstimationThread extends Thread {
    private Deadline estDeadline = null;

    public void run() {
      startEstimateCalculation();
    }

    // estimate via short walk
    void startEstimateCalculation() {
      long timeTaken = 0;
      long startTime = TimeBase.nowMs();
      // start with top-level cus
      CachedUrlSet cus = theAu.getAUCachedUrlSet();
      estDeadline = Deadline.in(treeWalkTestDuration);
      int nodesWalked = recurseEstimate(cus, 0);

      timeTaken = TimeBase.nowMs() - startTime;
      logger.debug("Treewalk estimate finished in time " + timeTaken + "ms with " +
                   nodesWalked + " nodes walked.");
      if (timeTaken == 0) {
        logger.warning("Test finished in zero time after walking " +
                       nodesWalked + " nodes.");
        // set to a harmless constant
        treeWalkEstimate = 1;
        return;
      }
      // calculate
      double nodesPerMs = ( (double) timeTaken / nodesWalked);
      // check size (node count) of tree
      int nodeCount = manager.nodeMap.size();
      treeWalkEstimate = (long) (nodeCount * nodesPerMs);
    }

    private int recurseEstimate(CachedUrlSet cus, int nodesWalked) {
      // get the node state for the cus
      // this should be the most expensive operation, since there
      // are likely to be file operations involved
      logger.debug3("Recursing estimate on cus: "+cus.getUrl());
      NodeState state = manager.getNodeState(cus);
      nodesWalked++;
      Iterator children = cus.flatSetIterator();
      while (!estDeadline.expired() && children.hasNext()) {
        CachedUrlSetNode node = (CachedUrlSetNode)children.next();
        if (node.getType()==CachedUrlSetNode.TYPE_CACHED_URL_SET) {
          // recurse on the child cus
          nodesWalked = recurseEstimate((CachedUrlSet)node, nodesWalked);
        } else if (node.getType()==CachedUrlSetNode.TYPE_CACHED_URL) {
          // open a new state for the leaf and increment
          state = manager.getNodeState(
              theAu.makeCachedUrlSet(node.getUrl(), null, null));
          nodesWalked++;
        }
      }
      return nodesWalked;
    }

    void end() {
      if (estDeadline!=null) {
        estDeadline.expire();
      }
    }
  }
      */
}
