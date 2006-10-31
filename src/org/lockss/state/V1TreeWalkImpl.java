/*
 * $Id: V1TreeWalkImpl.java,v 1.11 2006-10-31 02:33:36 smorabito Exp $
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

import java.io.IOException;
import java.util.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;
import org.lockss.crawler.CrawlManager;
import org.lockss.app.LockssDaemon;
import org.lockss.poller.Poll;

/**
 * The V1 treewalk algorithm, Performs a single treealk, independent of any
 * scheduling or threading mechanisms.
 */
public class V1TreeWalkImpl implements TreeWalker {
  private static Logger log = Logger.getLogger("V1TreeWalkImpl");

  // Managers, AU.  (All but the PluginManager are required for testing.)
  TreeWalkManager twm;
  NodeManager nodeMgr;
  private CrawlManager crawlMgr;
  private ActivityRegulator actRegulator;
  private PluginManager pluginMgr;
  private ArchivalUnit theAu;

  ActivityRegulator.Lock activityLock;
  LockssWatchdog wdog = null;

  long sleepDuration;
  Deadline nextSleep;

  volatile boolean treeWalkAborted;
  PollHistory cachedHistory = null;
  NodeState cachedNode = null;
  boolean didFullTreewalk;


  V1TreeWalkImpl(LockssDaemon daemon, ArchivalUnit au) {
    theAu = au;
    twm = daemon.getTreeWalkManager();
    nodeMgr = daemon.getNodeManager(au);
    crawlMgr = daemon.getCrawlManager();
    actRegulator = daemon.getActivityRegulator(theAu);
    pluginMgr = daemon.getPluginManager();
  }

  /** Set the watchdog I should poke occasionally during the treewalk */
  public void setWDog(LockssWatchdog wdog) {
    this.wdog = wdog;
  }

  /**
   * Determines the sleep duration from the desired interval and cpu load.
   * @param interval desired interval
   * @param load desired cpu load, as percentage
   * @return long proper sleep duration
   */
  private long calculateSleepDuration(long interval, double load) {
    return (long)(interval/load) - interval;
  }

  /** Return true iff the treewalk did not end early for any reason */
  public boolean didFullTreewalk() {
    return didFullTreewalk;
  }

  /**
   * The full treewalk only proceeds if no new content crawls or top level
   * polls are needed on the content.  As part of checking whether it
   * should execute, it triggers these actions by the CrawlManager and the
   * NodeManager before aborting its treewalk attempt.
   * @return true if the treewalk actually happened (even if aborted early
   * because a crawl or poll was started), false if it couldn't start
   * because it couldn't get the activity lock
   */
  public boolean doTreeWalk(Deadline finishBy) {    
    boolean doFull = true;
    switch (AuUtil.getProtocolVersion(theAu)) {
    case Poll.V1_PROTOCOL:
      doFull =  "full".equalsIgnoreCase(twm.paramV1Mode);
      break;
    case Poll.V3_PROTOCOL:
      doFull =  "full".equalsIgnoreCase(twm.paramV3Mode);
      break;
    }
    
    // XXX: A temporary measure until the treewalk is eliminated.
    // Skip treewalk entirely for V3 AUs
    if (AuUtil.getProtocolVersion(theAu) == Poll.V3_PROTOCOL) {
      // Get the node state for the AU.
      NodeState node =
        nodeMgr.getNodeState(theAu.getAuCachedUrlSet());
      PollHistory lastHistory = node.getLastPollHistory();
      try {
        nodeMgr.callNecessaryPolls(lastHistory, node);
        if (doFull) {
          didFullTreewalk = true;
        }
        treeWalkAborted = false;
        nodeMgr.getAuState().setLastTreeWalkTime();
        return true;
      } catch (IOException ex) {
        log.warning("Error while attempting to call top level V3 poll.", ex);
        return false;
      }
    }

    didFullTreewalk = false;
    long estDuration = finishBy.getRemainingTime();
    sleepDuration = calculateSleepDuration(twm.paramSleepInterval,
					   twm.paramLoadFactor);
    treeWalkAborted = false;

    log.debug("Attempting tree walk: " + theAu.getName());

    // check with regulator to see if treewalk can proceed
    activityLock =
      actRegulator.getAuActivityLock(ActivityRegulator.TREEWALK,
				     2 * estDuration);
    if (activityLock == null) {
      log.debug2("Treewalk couldn't start due to activity lock.");
      return false;
    }
    try {
      // do we need to do a new content crawl?
      if (theAu.shouldCrawlForNewContent(nodeMgr.getAuState())) {
	treeWalkAborted = true;
	if (crawlMgr.isCrawlStarterEnabled()) {
	  log.debug("New content crawl needed.  Ending treewalk.");
	} else {
	  log.debug("Starting new content crawl.  Ending treewalk.");
	  CrawlManager.Callback rc = null;
	  if (theAu instanceof RegistryArchivalUnit) {
	    log.debug("AU " + theAu.getName() +
		      " is a registry, adding callback.");
	    rc = new PluginManager.RegistryCallback(pluginMgr, theAu);
	  }
	  crawlMgr.startNewContentCrawl(theAu, rc, null, activityLock);
	}
      } else {
	if (doFull) {
	  if (nodeMgr.repairsNeeded()) {
	    // schedule repairs if needed
	    treeWalkAborted = true;
	    log.debug("Requesting node nodeMgr repairs.  Ending treewalk.");
	    nodeMgr.scheduleRepairs(activityLock);
	  } else {
	    // do the actual treewalk
	    log.debug("Tree walk started: " + theAu.getName());
	    nodeTreeWalk();
	    didFullTreewalk = !treeWalkAborted;
	  }
	}
      }
    } catch (Exception e) {
      log.error("Exception in treewalk", e);
    } finally {
      // release the lock on the treewalk
      // need to check, since it may have been passed to the crawler
      if (activityLock.getActivity()==ActivityRegulator.TREEWALK) {
	activityLock.expire();
      }
      treeWalkAborted = false;
    }
    //alert the AuState (it writes through)
    nodeMgr.getAuState().setLastTreeWalkTime();
    log.debug("Tree walk finished: " + theAu.getName());
    return true;
  }

  /**
   * Starts the actual treewalk.
   */
  private void nodeTreeWalk() {
    // initialize sleep interval
    nextSleep = Deadline.in(twm.paramSleepInterval);

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
      log.debug("Lost activity lock, aborting");
      treeWalkAborted = true;
    }
    if (treeWalkAborted) {
      // treewalk has been terminated
      return false;
    }

    // poke the watchdog each loop
    if (wdog != null) {
      wdog.pokeWDog();
    }

    boolean pContinue = true;
    // get the node state for the cus
    if (log.isDebug3()) log.debug3("Visiting node: " + cus.getUrl());
    NodeState parent = nodeMgr.getNodeState(cus);
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
	  nodeMgr.getNodeState(theAu.makeCachedUrlSet(cuss));
	pContinue = checkNodeState(state);
      }
      if (treeWalkAborted) {
	return false;
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
      } catch (InterruptedException ie) {
	// ignored - check treeWalkAborted below
      }
      nextSleep = Deadline.in(twm.paramSleepInterval);
    }

    // check after sleep
    if (treeWalkAborted) {
      // treewalk has been terminated
      return false;
    }

    // at each node, check for recrawl needed
//     if (node.getCachedUrlSet().hasContent()) {
//       //XXX if (crawlMgr.shouldRecrawl(managerAu, node)) {
//       // then CrawlManager.scheduleBackgroundCrawl()
//       // return false;
//     }

    // check recent histories to see if something needs fixing
    PollHistory lastHistory = node.getLastPollHistory();
    try {
      if (nodeMgr.checkCurrentState(lastHistory, node)) {
	// mark node for action
	log.debug2("Need poll.  Ending, will do deferred action on: " +
		   node.getCachedUrlSet().getUrl());
	cachedHistory = lastHistory;
	cachedNode = node;

	// abort treewalk
	treeWalkAborted = true;
	return false;
      }
    } catch (java.io.IOException ie) {
      log.error("Error in checkCurrentState", ie);
    }
    return true;
  }

  public void abort() {
    treeWalkAborted = true;
  }

  /**
   * Checks to see if the treewalk has saved a node to call a poll on.
   * This is done after the treewalk has fully completed, so that the scheduler
   * can schedule properly.
   */
  public void doDeferredAction() {
    if (cachedNode != null) {
      log.debug2("Calling poll on node '" +
		 cachedNode.getCachedUrlSet().getUrl() + "'");
      try {
	nodeMgr.callNecessaryPolls(cachedHistory, cachedNode);
      } catch (Exception e) {
	log.error("Error calling poll", e);
      }
      // null these values for next treewalk
      cachedHistory = null;
      cachedNode = null;
    }
  }
}
