/*
 * $Id: TreeWalkThread.java,v 1.2 2003-03-20 00:01:35 aalto Exp $
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
 * The treewalk thread in the NodeManager
 */
public class TreeWalkThread extends Thread {
  public static final String PREFIX = Configuration.PREFIX + "treewalk.";

  /**
   * Configuration parameter name for duration, in ms, for which the treewalk
   * estimation should run.
   */
  public static final String PARAM_TREEWALK_ESTIMATE_DURATION =
      PREFIX + "estimate.duration";
  static final int DEFAULT_TREEWALK_ESTIMATE_DURATION = 1000;

  /**
   * Configuration parameter name for interval, in ms, between treewalks.
   */
  public static final String PARAM_TREEWALK_INTERVAL = PREFIX + "interval";
  static final int DEFAULT_TREEWALK_INTERVAL = 60 * 60 * 1000; //1 hour

  /**
   * Configuration parameter name for default interval, in ms, after which
   * a new top level poll should be called.
   */
  public static final String PARAM_TOP_LEVEL_POLL_INTERVAL =
      Configuration.PREFIX + "top.level.poll.interval";
  static final int DEFAULT_TOP_LEVEL_POLL_INTERVAL =
      14 * 24 * 60 * 60 * 1000; //2 weeks

  static final double MAX_DEVIATION = 0.1;

  NodeManagerImpl manager;
  private static CrawlManager theCrawlManager;
  private ArchivalUnit theAu;

  private Logger logger = Logger.getLogger("TreeWalkThread");
  TreeWalkThread treeWalkThread;
  boolean treeWalkRunning = false;
  long treeWalkInterval;
  long topPollInterval;
  long treeWalkTestDuration;
  boolean shouldStartTreeWalkThread;
  boolean goOn = true;
  Deadline deadline;

  Configuration.Callback configCallback;

  TreeWalkThread(String name, NodeManagerImpl manager,
                 CrawlManager theCrawlManager) {
    super(name);
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
    topPollInterval = config.getTimeInterval(
        PARAM_TOP_LEVEL_POLL_INTERVAL, DEFAULT_TOP_LEVEL_POLL_INTERVAL);
    treeWalkTestDuration = config.getTimeInterval(
        PARAM_TREEWALK_ESTIMATE_DURATION, DEFAULT_TREEWALK_ESTIMATE_DURATION);
  }

  public long getEstimatedTreeWalkDuration() {
    // estimate via short walk
    logger.debug("Estimating treewalk for: "+theAu.getName());
    long timeTaken = 0;
    long startTime = TimeBase.nowMs();
    // start with top-level cus
    CachedUrlSet cus = theAu.getAUCachedUrlSet();
    Deadline estDeadline = Deadline.in(treeWalkTestDuration);
    int nodesWalked = recurseEstimate(cus, estDeadline, 0);

    timeTaken = TimeBase.nowMs() - startTime;
    logger.debug("Treewalk estimate finished in time "+timeTaken+"ms with "+
                 nodesWalked + " nodes walked.");
    if (timeTaken == 0) {
      logger.warning("Test finished in zero time after walking " +
                     nodesWalked + " nodes.");
      // returning a harmless constant
      return 1;
    }
    // calculate
    double nodesPerMs = ((double)timeTaken / nodesWalked);
    // check size (node count) of tree
    int nodeCount = manager.nodeMap.size();
    return (long)(nodeCount * nodesPerMs);
  }

  int recurseEstimate(CachedUrlSet cus, Deadline estDeadline,
                       int nodesWalked) {
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
        nodesWalked = recurseEstimate((CachedUrlSet)node, estDeadline, nodesWalked);
      } else if (node.getType()==CachedUrlSetNode.TYPE_CACHED_URL) {
        // open a new state for the leaf and increment
        state = manager.getNodeState(
            theAu.makeCachedUrlSet(node.getUrl(), null, null));
        nodesWalked++;
      }
    }
    return nodesWalked;
  }

  void doTreeWalk() {
    treeWalkRunning = true;
    logger.debug("Attempting tree walk: "+theAu.getName());
    if (theCrawlManager.canTreeWalkStart(theAu, null, null)) {
      long startTime = TimeBase.nowMs();

      NodeState topNode = manager.getNodeState(theAu.getAUCachedUrlSet());
      Iterator activePolls = topNode.getActivePolls();
      // only continue if no top level poll being considered
      if (!activePolls.hasNext()) {
        // if it's been too long, start a top level poll
        if ((startTime - manager.getAuState().getLastTopLevelPollTime())
            > topPollInterval) { //XXX get from plugin
          manager.callTopLevelPoll();
        } else {
          logger.debug("Tree walk started: "+theAu.getName());
          nodeTreeWalk(manager.nodeMap);
          long elapsedTime = TimeBase.nowMs() - startTime;
          manager.updateEstimate(elapsedTime);
        }
      }
    }
    //alert the AuState
    manager.getAuState().treeWalkFinished();
    logger.debug("Tree walk finished.");
    treeWalkRunning = false;
  }

  private void nodeTreeWalk(Map nodeMap) {
    CachedUrlSet cus = theAu.getAUCachedUrlSet();
    recurseTreeWalk(cus);
  }

  void recurseTreeWalk(CachedUrlSet cus) {
    // get the node state for the cus
    logger.debug3("Recursing treewalk on cus: "+cus.getUrl());
    NodeState state = manager.getNodeState(cus);
    // walk the node
    boolean pContinue = walkNodeState(state);
    if (!pContinue) {
      // this node is busy, so abort without walking its children
      return;
    }
    Iterator children = cus.flatSetIterator();
    while (children.hasNext()) {
      CachedUrlSetNode node = (CachedUrlSetNode)children.next();
      if (node.getType()==CachedUrlSetNode.TYPE_CACHED_URL_SET) {
        // recurse on the child cus
        recurseTreeWalk((CachedUrlSet)node);
      } else if (node.getType()==CachedUrlSetNode.TYPE_CACHED_URL) {
        // open a new state for the leaf and walk
        state = manager.getNodeState(
            theAu.makeCachedUrlSet(node.getUrl(), null, null));
        walkNodeState(state);
      }
    }
  }

  boolean walkNodeState(NodeState node) {
    CrawlState crawlState = node.getCrawlState();

    // determine if there are active polls
    Iterator polls = node.getActivePolls();
    while (polls.hasNext()) {
      PollState poll = (PollState)polls.next();
      if ((poll.getStatus()==PollState.RUNNING) ||
          (poll.getStatus()==PollState.REPAIRING) ||
          (poll.getStatus()==PollState.SCHEDULED)) {
        // if there are active polls, don't interfere
        return false;
      }
    }
    // at each node, check for crawl state
    switch (crawlState.getType()) {
      case CrawlState.NODE_DELETED:
        // skip node if deleted
        return true;
      case CrawlState.BACKGROUND_CRAWL:
      case CrawlState.NEW_CONTENT_CRAWL:
      case CrawlState.REPAIR_CRAWL:
        if (crawlState.getStatus() == CrawlState.FINISHED) {
          // if node is cached
          if (node.getCachedUrlSet().hasContent()) {
            // if (theCrawlManager.shouldRecrawl(managerAu, node)) {
            // then CrawlManager.scheduleBackgroundCrawl()
            return false;
          }
        }
      default:
        break;
    }
    // check recent histories to see if something needs fixing
    // if there are no current polls
    PollHistory lastHistory = node.getLastPollHistory();
    if (lastHistory != null) {
      switch (lastHistory.status) {
        // if latest is PollState.LOST or PollState.REPAIRING
        // call a name poll to finish the repair which ended early
        case PollState.LOST:
        case PollState.REPAIRING:
          PollSpec spec = new PollSpec(node.getCachedUrlSet(),
                                       lastHistory.lwrBound,
                                       lastHistory.uprBound);
          manager.callNamePoll(spec.getCachedUrlSet());
          return false;
        case PollState.UNREPAIRABLE:
          //XXX determine what to do
          return false;
        default:
          break;
      }
    }
    return true;
  }

  boolean shouldTreeWalkStart() {
    if (treeWalkRunning) {
      return false;
    }
    long lastTreeWalkTime = manager.getAuState().getLastTreeWalkTime();
    long timeSinceLastTW = TimeBase.nowMs() - lastTreeWalkTime;
    logger.debug3(timeSinceLastTW+" since last treewalk");
    logger.debug("Treewalks should happen every "+treeWalkInterval);
    if (timeSinceLastTW >= treeWalkInterval) {
      return true;
    }
    return false;
  }

  public void end() {
    goOn = false;
    if (deadline != null) {
      deadline.expire();
    }
  }

  public void run() {
    while (goOn) {
      if (shouldTreeWalkStart()) {
        doTreeWalk();
      } else {
        long delta = (long)((double)MAX_DEVIATION*treeWalkInterval);
        logger.debug3("Creating a deadline for " + treeWalkInterval +
                      " with delta of " + delta);
        deadline = Deadline.inRandomDeviation(treeWalkInterval, delta);
        try {
          deadline.sleep();
        } catch (InterruptedException ie) { }
      }
    }
  }
}
