/*
 * $Id: TreeWalkHandler.java,v 1.10 2003-04-04 23:50:11 aalto Exp $
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
  boolean forceTreeWalk = false;

  long treeWalkEstimate = -1;


  Configuration.Callback configCallback;

  TreeWalkHandler(NodeManagerImpl manager, CrawlManager theCrawlManager) {
    this.manager = manager;
    this.theCrawlManager = theCrawlManager;
    theAu = manager.managedAu;

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
    logger.debug3("treeWalkInterval reset to "+treeWalkInterval);
  }

  /**
   * The full treewalk only proceeds if no new content crawls or top level polls
   * are needed on the content.  As part of checking whether it should execute,
   * it triggers these actions by the CrawlManager and the NodeManager before
   * aborting its treewalk attempt.
   */
  void doTreeWalk() {
    logger.debug("Attempting tree walk: "+theAu.getName());
    NodeState topNode = manager.getNodeState(theAu.getAUCachedUrlSet());
    Iterator activePolls = topNode.getActivePolls();
    // only continue if no top level poll scheduled or running
    if (!activePolls.hasNext()) {
      // check with crawl manager
      if (!theCrawlManager.isCrawlingAU(theAu, null, null)) {
        // query the AU if a top level poll should be started
        if (theAu.shouldCallTopLevelPoll(manager.getAuState())) {
          manager.callTopLevelPoll();
          logger.debug("Requested top level poll.  Aborting...");
        } else {
          // do the actual treewalk
          logger.debug("Tree walk started: "+theAu.getName());
          long startTime = TimeBase.nowMs();
          nodeTreeWalk();
          long elapsedTime = TimeBase.msSince(startTime);
          updateEstimate(elapsedTime);
        }
      } else {
        logger.debug("Crawl manager active.  Aborting...");
      }
    } else {
      logger.debug("Top level poll active.  Aborting...");
    }
    //alert the AuState
    manager.getAuState().setLastTreeWalkTime();
    manager.historyRepo.storeAuState(manager.getAuState());
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
    // if treewalk is forced
    if (forceTreeWalk) {
      logger.debug("Forcing treewalk start-in time of -1.");
      forceTreeWalk = false;
      return -1;
    }
    long lastTreeWalkTime = manager.getAuState().getLastTreeWalkTime();
    long timeSinceLastTW = TimeBase.msSince(lastTreeWalkTime);
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
    Configuration.unregisterConfigurationCallback(configCallback);
  }

  /**
   * Starts the treewalk process.
   */
  public void start() {
    logger.debug3("TreeWalkHandler started.");
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
    if (treeWalkThread!=null) {
      treeWalkThread = new TreeWalkThread();
      threadWasNull = true;
    }
    if (!treeWalkThread.doingTreeWalk) {
      forceTreeWalk = true;
      if (threadWasNull) {
        treeWalkThread.start();
      } else {
        // just wake it up
        treeWalkThread.deadline.expire();
      }
    }
  }

  /**
   * The thread which handles the treewalk itself.
   */
  class TreeWalkThread extends Thread {
    private boolean goOn = true;
    boolean doingTreeWalk = false;
    Deadline deadline;

    public TreeWalkThread() {
      super("TreeWalk: "+theAu.getName());
    }

    public void run() {
      while (goOn) {
        long timeToStart = timeUntilTreeWalkStart();
        if (timeToStart <= 0) {
          doingTreeWalk = true;
          doTreeWalk();
          doingTreeWalk = false;
        } else {
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

  /*
   * Returns the current treewalk average.  -1 until a treewalk is run.
   * @return the estimate, in ms
   */
  long getAverageTreeWalkDuration() {
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

}
