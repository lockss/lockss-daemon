/*
 * $Id: NodeManagerImpl.java,v 1.210 2006-11-11 06:56:29 tlipkis Exp $
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

import java.io.*;
import java.util.*;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.poller.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.repository.*;
import org.lockss.config.Configuration;
import org.lockss.config.CurrentConfig;
import org.lockss.crawler.CrawlManager;
import org.lockss.alert.*;
import java.util.ArrayList;

import sun.security.krb5.internal.*;

/**
 * Implementation of the NodeManager.
 */
public class NodeManagerImpl
  extends BaseLockssDaemonManager implements NodeManager {

  public static final String PARAM_DISABLE_V3_POLLER =
    Configuration.PREFIX + "poll.v3.disableV3Poller";
  public static final boolean DEFAULT_DISABLE_V3_POLLER = false;
  
  // the various necessary managers
  NodeManagerManager nodeMgrMgr;
  HistoryRepository historyRepo;
  AlertManager alertMgr;
  private LockssRepository lockssRepo;
  PollManager pollManager;
  ActivityRegulator regulator;
  static SimpleDateFormat sdf = new SimpleDateFormat();

  // state and caches for this AU
  ArchivalUnit managedAu;
  AuState auState;
  UniqueRefLruCache nodeCache;
  private boolean isGlobalNodeCache =
    NodeManagerManager.DEFAULT_GLOBAL_CACHE_ENABLED;
  HashMap activeNodes;

   //the set of nodes marked damaged (these are nodes which have lost content
   // poll).
  DamagedNodeSet damagedNodes;

  private static Logger logger = Logger.getLogger("NodeManager");

  NodeManagerImpl(ArchivalUnit au) {
    managedAu = au;
  }

  public void startService() {
    super.startService();
    // gets all the managers
    if (logger.isDebug()) logger.debug("Starting: " + managedAu);
    LockssDaemon theDaemon = getDaemon();
    nodeMgrMgr = theDaemon.getNodeManagerManager();
    historyRepo = theDaemon.getHistoryRepository(managedAu);
    lockssRepo = theDaemon.getLockssRepository(managedAu);
    pollManager = theDaemon.getPollManager();
    regulator = theDaemon.getActivityRegulator(managedAu);
    alertMgr = theDaemon.getAlertManager();
    // initializes the state info
    isGlobalNodeCache = nodeMgrMgr.isGlobalNodeCache();
    if (isGlobalNodeCache) {
      nodeCache = nodeMgrMgr.getGlobalNodeCache();
    } else {
      nodeCache = new UniqueRefLruCache(nodeMgrMgr.paramNodeStateCacheSize);
    }
    activeNodes = new HashMap();

    auState = historyRepo.loadAuState();
    damagedNodes = historyRepo.loadDamagedNodeSet();

    logger.debug2("NodeManager successfully started");
  }

  public void stopService() {
    if (logger.isDebug()) logger.debug("Stopping: " + managedAu);
    activeNodes.clear();
    damagedNodes.clear();
    nodeCache.clear();

    super.stopService();
    logger.debug2("NodeManager successfully stopped");
  }

  public void setNodeStateCacheSize(int size) {
    if (nodeCache != null && !isGlobalNodeCache &&
	nodeCache.getMaxSize() != size) {
      nodeCache.setMaxSize(size);
    }
  }

  public void setAuConfig(Configuration auConfig) {
  }

  public DamagedNodeSet getDamagedNodes() {
    return damagedNodes;
  }


  public synchronized NodeState getNodeState(CachedUrlSet cus) {
    String url = cus.getUrl();
    NodeState node = (NodeState)nodeCache.get(nodeCacheKey(url));
    if (node == null) {
      // if in repository, add to our state list
      try {
        if (lockssRepo.getNode(url) != null) {
          node = createNodeState(cus);
        } else {
          logger.debug("URL '"+cus.getUrl()+"' not found in cache.");
        }
      }
      catch (MalformedURLException mue) {
        logger.error("Can't get NodeState due to bad CUS '" + cus.getUrl()+"'");
      }
    }
    return node;
  }

  /**
   * Creates or loads a new NodeState instance (not in cache) and runs a
   * dead poll check against it.
   * @param cus CachedUrlSet
   * @return NodeState
   */
  NodeState createNodeState(CachedUrlSet cus) {
    // load from file cache, or get a new one
    NodeState state = historyRepo.loadNodeState(cus);

    // check for dead polls
    Iterator activePolls = state.getActivePolls();
    if (activePolls.hasNext()) {
      ArrayList pollsToRemove = new ArrayList();
      while (activePolls.hasNext()) {
        PollState poll = (PollState) activePolls.next();
        PollSpec pollSpec = new PollSpec(state.getCachedUrlSet(),
                                         poll.getLwrBound(),
                                         poll.getUprBound(),
					 poll.type);
        // if poll isn't running and it was our poll
        if (!pollManager.isPollRunning(pollSpec)) {
          // transfer dead poll to history and let treewalk handle it
          // set duration to the deadline of the dead poll
          // this is important because it could still be running in other caches
          long duration = poll.getDeadline().getExpirationTime() -
              poll.getStartTime();
          PollHistory history =
              new PollHistory(poll.type, poll.lwrBound, poll.uprBound,
                              PollState.UNFINISHED, poll.startTime, duration,
                              new ArrayList(), poll.ourPoll);
          pollsToRemove.add(history);
        }
      }
      for (int ii=0; ii<pollsToRemove.size(); ii++) {
        logger.debug("Dead poll being removed for CUS '" + cus.getUrl() + "'");
        PollHistory history = (PollHistory)pollsToRemove.get(ii);
        ((NodeStateImpl)state).closeActivePoll(history);
      }
    }

    nodeCache.put(nodeCacheKey(cus.getUrl()), state);
    return state;
  }

  Object nodeCacheKey(String canonUrl) {
    if (isGlobalNodeCache) {
      return new KeyPair(this, canonUrl);
    }
    return canonUrl;
  }

  public AuState getAuState() {
    return auState;
  }

  /**
   * Returns a list of cached NodeStates.  This only returns nodes currently in
   * the cache, rather than a full list, but it's only used by the UI so that's
   * not a problem.
   * @return Iterator the NodeStates
   */
  Iterator getCacheEntries() {
    if (isGlobalNodeCache) {
      // must return just our entries from global cache
      Collection auEntries = new ArrayList();
      for (Iterator iter = nodeCache.snapshot().iterator(); iter.hasNext(); ) {
        NodeState state = (NodeState)iter.next();
	if (managedAu == state.getCachedUrlSet().getArchivalUnit()) {
	  auEntries.add(state);
	}
      }
      return auEntries.iterator();
    } else {
      return nodeCache.snapshot().iterator();
    }
  }

  public void forceTopLevelPoll() {
    logger.info("Forcing top level poll...");
    NodeState topNode = getNodeState(managedAu.getAuCachedUrlSet());
    Iterator activePolls = topNode.getActivePolls();
    if (!activePolls.hasNext()) {
      callTopLevelPoll();
    }
  }

  public boolean shouldStartPoll(CachedUrlSet cus, Tallier tally) {
    NodeState nodeState = getNodeState(cus);

    // check if node exists
    if (nodeState == null) {
      logger.error("Failed to find a valid node state for: " + cus);
      return false;
    }

    // make sure not damaged (if not my poll)
    if (!tally.isMyPoll() && damagedNodes.hasLocalizedDamage(cus)) {
      logger.info("CUS has damage, not starting poll: " + cus);
      return false;
    }

    // make sure crawl isn't needed
    if (managedAu.shouldCrawlForNewContent(auState)) {
      logger.info("New content crawl needed, not starting poll: " + managedAu);
      return false;
    }

    return true;
  }

  /**
   * XXX: V3
   *
   * This method is only called by V1PollFactory.  It is not called by V3.
   */
  public void startPoll(CachedUrlSet cus, Tallier tally, boolean isReplayPoll) {
    NodeState nodeState = getNodeState(cus);
    PollSpec spec = tally.getPollSpec();
    int status;

    // if this is a ranged poll (and not an SNCUSS), don't change NodeState
    if (cus.getSpec().isRangeRestricted()) {
      if (isReplayPoll) {
        status = PollState.REPAIRING;
      } else {
        status = PollState.RUNNING;
      }
    } else {
      // normal poll

      // set states correctly
      logger.debug2("Prev node state: "+nodeState.getStateString());
      switch (nodeState.getState()) {
        case NodeState.INITIAL:
        case NodeState.OK:

          // these are polls started by another cache
          status = PollState.RUNNING;
          if (cus.getSpec().isSingleNode()) {
            nodeState.setState(NodeState.SNCUSS_POLL_RUNNING);
          } else {
            nodeState.setState((tally.getType() == Poll.V1_CONTENT_POLL) ?
                               NodeState.CONTENT_RUNNING :
                               NodeState.NAME_RUNNING);
          }
          break;
        case NodeState.NEEDS_POLL:
        case NodeState.POSSIBLE_DAMAGE_BELOW:
          nodeState.setState(NodeState.CONTENT_RUNNING);
          status = PollState.RUNNING;
          break;
        case NodeState.NEEDS_REPLAY_POLL:
          nodeState.setState(NodeState.CONTENT_REPLAYING);
          status = PollState.REPAIRING;
          break;
        case NodeState.CONTENT_LOST:
        case NodeState.UNREPAIRABLE_NAMES_NEEDS_POLL:
          nodeState.setState(NodeState.NAME_RUNNING);
          status = PollState.RUNNING;
          break;
        case NodeState.WRONG_NAMES:
          nodeState.setState(NodeState.NAME_REPLAYING);
          // Fall through to NAME_REPLAYING to set status
        case NodeState.NAME_REPLAYING:
          status = PollState.REPAIRING;
          break;
        case NodeState.POSSIBLE_DAMAGE_HERE:
        case NodeState.UNREPAIRABLE_SNCUSS_NEEDS_POLL:
          nodeState.setState(NodeState.SNCUSS_POLL_RUNNING);
          status = PollState.RUNNING;
          break;
        case NodeState.NEEDS_REPAIR:
          nodeState.setState(NodeState.SNCUSS_POLL_REPLAYING);
           // Fall through to SNCUSS_POLL_REPLAYING to set status
        case NodeState.SNCUSS_POLL_REPLAYING:
          status = PollState.REPAIRING;
          break;
        default:
          logger.error("Invalid nodestate: " + nodeState.getStateString());
          // set to 'default' states for new polls
          status = PollState.RUNNING;
          if (cus.getSpec().isSingleNode()) {
            nodeState.setState(NodeState.SNCUSS_POLL_RUNNING);
          } else if (tally.getType() == Poll.V1_CONTENT_POLL) {
            nodeState.setState(NodeState.CONTENT_RUNNING);
          } else {
            nodeState.setState(NodeState.NAME_RUNNING);
          }
      }
    }
    PollState pollState = new PollState(tally.getType(), spec.getLwrBound(),
                                        spec.getUprBound(),
                                        status, tally.getStartTime(),
                                        Deadline.in(tally.getDuration()),
                                        tally.isMyPoll());
    ((NodeStateImpl)nodeState).addPollState(pollState);
    // store nodestate for active poll
    activeNodes.put(tally.getPollKey(), nodeState);
    if (logger.isDebug2()) {
      logger.debug2("Starting poll for url '" +
                    nodeState.getCachedUrlSet().getUrl() + " " +
                    pollState.getLwrBound() + "-" +
                    pollState.getUprBound() + "'");
      logger.debug2("New node state: " + nodeState.getStateString());
    }
  }

  public void updatePollResults(CachedUrlSet cus, Tallier results) {
    NodeState nodeState = (NodeState)activeNodes.get(results.getPollKey());
    if (nodeState == null) {
      nodeState = getNodeState(cus);
    }
    updateState(nodeState, results);
    // remove from active nodes
    activeNodes.remove(results.getPollKey());
  }

  public void newContentCrawlFinished() {
    // notify and checkpoint the austate (it writes through)
    getAuState().newCrawlFinished();

    // checkpoint the top-level nodestate
    NodeState topState = getNodeState(managedAu.getAuCachedUrlSet());
    CrawlState crawl = topState.getCrawlState();
    crawl.status = CrawlState.FINISHED;
    crawl.type = CrawlState.NEW_CONTENT_CRAWL;
    crawl.startTime = getAuState().getLastCrawlTime();
    historyRepo.storeNodeState(topState);
  }

  public void hashFinished(CachedUrlSet cus, long hashDuration) {
    if (hashDuration < 0) {
      logger.warning("Tried to update hash with negative duration.");
      return;
    }
    NodeState state = getNodeState(cus);
    if (state == null) {
      logger.error("Updating state on non-existant node: " + cus.getUrl());
      throw new IllegalArgumentException(
          "Updating state on non-existant node.");
    } else {
      logger.debug3("Hash finished for CUS '" + cus.getUrl() + "'");
      ((NodeStateImpl)state).setLastHashDuration(hashDuration);
    }
  }

  /**
   * Update the PollState and NodeState to reflect the finished poll.  Sets the
   * node to be ready for its next action.
   * @param nodeState NodeState
   * @param results Tallier
   */
  void updateState(NodeState nodeState, Tallier results) {
    logger.debug3("Prior node state: "+nodeState.getStateString());

    PollState pollState = getPollState(nodeState, results);
    if (pollState == null) {
      logger.error("Results updated for a non-existent poll.");
      throw new UnsupportedOperationException("Results updated for a "
                                              + "non-existent poll.");
    }

    CachedUrlSetSpec spec = results.getCachedUrlSet().getSpec();
    // determine if ranged, as those don't update as much

    boolean isRangedPoll = spec.isRangeRestricted();
    if(isRangedPoll) {
      logger.debug3("range poll- ignoring state flags: " + spec);
    }
    // log if current state is invalid for finishing poll
    if (!isRangedPoll) {
      if (!checkValidStatesForResults(pollState, nodeState,spec)) {
        StringBuffer buffer = new StringBuffer();
        if (spec.isSingleNode()) {
          buffer.append("Single Node ");
        }
        buffer.append(pollState.getTypeString());
        buffer.append(" Poll, ");
        buffer.append(pollState.getStatusString());
        logger.error("Invalid node state for poll state: " +
                     nodeState.getStateString() + " for " +
                     buffer.toString());
      }
    }

    // update poll state properly for errors
    int stateToUse = -1;
    try {
      boolean notFinished = false;
      switch (results.getTallyResult()) {
        case Tallier.RESULT_ERROR:
          pollState.status = mapResultsErrorToPollError(results.getErr());
          logger.info("Poll didn't finish fully.  Error: "
                      + results.getErrString());
          notFinished = true;
          break;
        case Tallier.RESULT_NOQUORUM:
          pollState.status = PollState.INCONCLUSIVE;
          logger.info("Poll finished without quorum");
          notFinished = true;
          break;
        case Tallier.RESULT_TOO_CLOSE:
        case Tallier.RESULT_UNTRUSTED:
          pollState.status = PollState.INCONCLUSIVE;
          logger.warning("Poll concluded with suspect result - "
                         + pollState.getStatusString());
          notFinished = true;
          break;
        case Tallier.RESULT_WON:
        case Tallier.RESULT_LOST:
          // update depending on poll type if successful
          if (results.getType() == Poll.V1_CONTENT_POLL) {
            stateToUse = handleContentPoll(pollState, results, nodeState);
          } else if (results.getType() == Poll.V1_NAME_POLL) {
            stateToUse = handleNamePoll(pollState, results, nodeState);
          } else {
            String err = "Request to update state for unknown type: " +
                results.getType();
            logger.error(err);
            throw new UnsupportedOperationException(err);
          }
          break;
      }

      // if error state occurred, set node state accordingly
      if (notFinished) {
          // only update NodeState if not a ranged poll
        if (!isRangedPoll) {
          if (!results.isMyPoll()) {
            // if not mine, ignore and go back to ok
            nodeState.setState(NodeState.OK);
          } else {
            // if mine, reset to proper state
            switch (nodeState.getState()) {
              case NodeState.CONTENT_RUNNING:
              case NodeState.CONTENT_REPLAYING:
                nodeState.setState(NodeState.NEEDS_POLL);
                break;
              case NodeState.NAME_RUNNING:
              case NodeState.NAME_REPLAYING:
                nodeState.setState(NodeState.CONTENT_LOST);
                break;
              case NodeState.SNCUSS_POLL_RUNNING:
              case NodeState.SNCUSS_POLL_REPLAYING:
                nodeState.setState(NodeState.POSSIBLE_DAMAGE_HERE);
                break;
            }
          }
        }
      } else {
        // finished properly

        // if this is an AU top-level content poll
        // update the AuState to indicate the poll is finished if it has
        // the proper status
        if (isTopLevelPollFinished(nodeState.getCachedUrlSet().getSpec(),
                                   pollState.getType())) {
          getAuState().newPollFinished();
          logger.info("Top level poll finished.");
        }

        // take next appropriate action on node if needed
        // only check state for ranged polls if they changed the state
        try {
          logger.debug3("New node state: " + nodeState.getStateString());
          callNecessaryPolls(pollState, results, nodeState, stateToUse);
        } catch (IOException ie) {
          logger.error("Unable to continue actions on node: ", ie);
          pollState.status = PollState.ERR_IO;
        }
      }
    } finally {
      // close the poll and update the node state
      closePoll(pollState, results.getDuration(), results.getPollVotes(),
                nodeState);
    }
  }

  /**
   * Handles the lists of correct names and local entries.  Schedules repairs,
   * deletes extra entries, creates missing nodes.
   * @param masterIt the master iterator of entries
   * @param localList the List of local entries
   * @param nodeState the NodeState
   * @param results the PollTally
   * @return true iff a repair was scheduled
   */
  boolean handleWrongNames(Iterator masterIt, List localList,
                           NodeState nodeState, Tallier results) {
    // iterate through master list
    Collection repairCol = new ArrayList();
    while (masterIt.hasNext()) {
      PollTally.NameListEntry entry =
          (PollTally.NameListEntry) masterIt.next();
      String url = entry.name;
      boolean hasContent = entry.hasContent;
      logger.debug3("checking " + url + ", hasContent=" + hasContent);
      // compare against my list
      int index = localList.indexOf(entry);
      if (index != -1) {
        logger.debug3("found entry, assessing content: " + url);
        PollTally.NameListEntry myEntry =
            (PollTally.NameListEntry)localList.get(index);
        if (myEntry.hasContent != entry.hasContent) {
          if (myEntry.hasContent) {
            // has content and shouldn't, so deactivate
            logger.debug("deactivating extra content at node: " + url);
            try {
              CachedUrlSet faultyCus = getChildCus(url, nodeState);
              deactivateNode(faultyCus);
            } catch (Exception e) {
              logger.error("Couldn't deactivate node.", e);
              // the treewalk will fix this eventually
            }
          } else {
            // doesn't have content, and should, so repair
            logger.debug("marking content-less node for repair: " + url);
            CachedUrlSet faultyCus = getChildCus(url, nodeState);
            // add to repair crawl list
            repairCol.add(faultyCus.getUrl());
          }
        } else {
          logger.debug3("content matches");
        }
        // removing from the set to leave only files for deletion
        logger.debug3("removing entry...");
        localList.remove(index);
      } else {
        // if not found locally, fetch
        logger.debug("marking missing node for repair: " + url);
        CachedUrlSet newCus = getChildCus(url, nodeState);

        // check content status, and only mark for repair if
        // should have content.  Else just create in repository.
        if (hasContent) {
          // add to repair crawl list
          repairCol.add(newCus.getUrl());
        } else {
          // create node in repository and (eventually) call name poll on it
          try {
            RepositoryNodeImpl repairNode =
                (RepositoryNodeImpl) lockssRepo.createNewNode(newCus.getUrl());
            repairNode.createNodeLocation();
            logger.debug("Node created in repository.");
          } catch (MalformedURLException mue) {
            // this shouldn't happen
          }
        }
      }
    }

    // schedule repair crawls if needed
    boolean repairsDone = false;
    if (repairCol.size() > 0) {
      String key = results.getPollKey();
      markNodesForRepair(repairCol, key, results.getCachedUrlSet(), true,
                         pollManager.acquirePollLock(key));
      repairsDone = true;
    }

    // check for deletion
    Iterator localIt = localList.iterator();
    while (localIt.hasNext()) {
      // for extra items - deletion
      PollTally.NameListEntry entry =
          (PollTally.NameListEntry) localIt.next();
      String url = entry.name;
      try {
        CachedUrlSet extraCus = getChildCus(url, nodeState);
        logger.debug("deleting node: " + url);
        deleteNode(extraCus);
      } catch (Exception e) {
        logger.error("Couldn't delete node.", e);
        // the treewalk will fix this eventually
      }
    }

    // return false if any repairs were scheduled
    return repairsDone;
  } 

  /**
   * Creates a CUS from the child url (typically a simple name) and its parent
   * NodeState, usually be appending the two together.
   * @param url String
   * @param nodeState NodeState
   * @return CachedUrlSet
   */
  private CachedUrlSet getChildCus(String url, NodeState nodeState) {
    ArchivalUnit au = nodeState.getCachedUrlSet().getArchivalUnit();
    String baseUrl = nodeState.getCachedUrlSet().getUrl();
    String childUrl;
    if (AuUrl.isAuUrl(baseUrl)) {
      // don't append AU url
      childUrl = url;
    } else {
      // append base url
      childUrl = baseUrl + url;
    }
    return au.makeCachedUrlSet(new RangeCachedUrlSetSpec(childUrl));
  }

  /**
   * Handles state for successful content polls.  Sets the PollState and
   * NodeState appropriately, and returns the state to be used for
   * 'checkCurrentState'.
   * @param pollState PollState
   * @param results Tallier
   * @param nodeState NodeState
   * @return int for the state to use
   */
  int handleContentPoll(PollState pollState, Tallier results,
                        NodeState nodeState) {
    logger.debug("handling content poll results: " + results);

    // only update NodeState if not ranged poll
    boolean isRangedPoll =
        results.getCachedUrlSet().getSpec().isRangeRestricted();
    int state = NodeState.OK;

    if (results.getTallyResult() == Tallier.RESULT_WON) {
      // if agree

      // change poll state accordingly
      switch (pollState.getStatus()) {
        case PollState.RUNNING:
          // if normal poll, we won!
          logger.debug2("won content poll, state = won.");
          pollState.status = PollState.WON;
          // remove if in damaged nodes from previous poll
          break;
        case PollState.REPAIRING:
          // if repair poll, we're repaired
          logger.debug2("won repair poll, state = repaired.");
          alertMgr.raiseAlert(Alert.auAlert(Alert.REPAIR_COMPLETE,managedAu)
                    .setAttribute(Alert.ATTR_TEXT,"Repair of " +
                                  results.getCachedUrlSet().getUrl()
                                  + " successful"));

          pollState.status = PollState.REPAIRED;
      }

      if (!isRangedPoll) {
        // change node state accordingly
        switch (nodeState.getState()) {
          case NodeState.CONTENT_RUNNING:
          case NodeState.CONTENT_REPLAYING:
            state = NodeState.OK;
            damagedNodes.clearDamage(nodeState.getCachedUrlSet());
           break;
          case NodeState.SNCUSS_POLL_RUNNING:
          case NodeState.SNCUSS_POLL_REPLAYING:
            if (results.isMyPoll()) {
              state = NodeState.POSSIBLE_DAMAGE_BELOW;
            } else {
              // not mine, so done
              state = NodeState.OK;
            }
        }
        nodeState.setState(state);
      }
      // for ranged polls, do nothing if won

      updateReputations(results);
    } else {
        // if disagree

      // change poll state accordingly
      if (pollState.getStatus() == PollState.REPAIRING) {
        logger.debug2("lost repair content poll, state = unrepairable");
        // if repair poll, can't be repaired and we leave it our damaged table
        pollState.status = PollState.UNREPAIRABLE;
        alertMgr.raiseAlert(Alert.auAlert(Alert.PERSISTENT_DAMAGE,managedAu)
                            .setAttribute(Alert.ATTR_TEXT,"Repair of " +
                                          results.getCachedUrlSet().getUrl()
                                          + " fails to resolve poll."));
        updateReputations(results);
      } else {
        // otherwise, schedule repair (if SNCUS) or name poll
        if (results.getCachedUrlSet().getSpec().isSingleNode()) {
          logger.debug2("lost single node content poll, marking for repair.");
          // if SNCUSS, we need to repair
          pollState.status = PollState.REPAIRING;
        } else {
          logger.debug2("lost content poll, state = lost.");
          pollState.status = PollState.LOST;
        }
      }

      if (!isRangedPoll) {
        // change node state accordingly
        switch (nodeState.getState()) {
          case NodeState.CONTENT_RUNNING:
            state = NodeState.CONTENT_LOST;
            // nodes are damaged when a normal content poll fails
            logger.debug2("adding to damaged node list");
            damagedNodes.addToDamage(nodeState.getCachedUrlSet().getUrl());
            break;
          case NodeState.CONTENT_REPLAYING:
            state = NodeState.DAMAGE_AT_OR_BELOW;
            break;
          case NodeState.SNCUSS_POLL_RUNNING:
            state = NodeState.NEEDS_REPAIR;
            break;
          case NodeState.SNCUSS_POLL_REPLAYING:
            state = NodeState.UNREPAIRABLE_SNCUSS;
            break;
        }
        nodeState.setState(state);
      } else {
        // if ranged, set temporary state
        state = NodeState.RANGED_CONTENT_LOST;
      }
    }
    return state;
  }

  /**
   * Handles state for successful name polls.  Updates PollState and NodeState
   * appropriately, and returns the state to be used for
   * 'checkCurrentState'.
   * @param pollState PollState
   * @param results Tallier
   * @param nodeState NodeState
   * @return int the state to use
   */
  int handleNamePoll(PollState pollState, Tallier results,
                     NodeState nodeState) {
    logger.debug2("handling name poll results " + results);

    // only update NodeState if not ranged poll
    boolean isRangedPoll =
        results.getCachedUrlSet().getSpec().isRangeRestricted();

    int state = NodeState.OK;
    if (results.getTallyResult() == Tallier.RESULT_WON) {
      // if agree

      // change poll state accordingly
      if (results.isMyPoll()) {
        // if poll is mine
        if (pollState.status == PollState.REPAIRING) {
          pollState.status = PollState.REPAIRED;
        } else {
          pollState.status = PollState.WON;
        }
      } else {
        logger.debug2("won name poll (not mine), setting state to WON");
        pollState.status = PollState.WON;
        // not my poll, so done
      }

      if (!isRangedPoll) {
        // change node state accordingly
        switch (nodeState.getState()) {
          case NodeState.NAME_RUNNING:
            if (results.isMyPoll()) {
              state = NodeState.DAMAGE_AT_OR_BELOW;
            } else {
              state = NodeState.OK;
            }
            break;
          case NodeState.NAME_REPLAYING:
            //XXX should replay content poll, but treated as NEEDS_POLL for now
            state = NodeState.NEEDS_REPLAY_POLL;
        }
        nodeState.setState(state);
      }
    } else {
      // if disagree

      // change poll state accordingly
      if (pollState.getStatus() == PollState.REPAIRING) {
        logger.debug2("lost repair name poll, state = unrepairable");
        // if repair poll, can't be repaired and we leave it in our damage table
        pollState.status = PollState.UNREPAIRABLE;
      } else {
        pollState.status = PollState.REPAIRING;
        logger.debug2("lost name poll, state = repairing");
      }

      if (!isRangedPoll) {
        // change node state accordingly
        switch (nodeState.getState()) {
          case NodeState.NAME_RUNNING:
            state = NodeState.WRONG_NAMES;
            break;
          case NodeState.NAME_REPLAYING:
            state = NodeState.UNREPAIRABLE_NAMES;
        }
        nodeState.setState(state);
      } else {
        // if ranged, set temporary state
        state = NodeState.RANGED_WRONG_NAMES;
      }
    }
    return state;
  }

  /**
   * Looks at the state of the node, and takes appropriate action.  Called
   * from the treewalk.
   * @param lastOrCurrentPoll the most recent poll (could be active)
   * @param nodeState the {@link NodeState}
   * @throws IOException
   */
  public void callNecessaryPolls(PollState lastOrCurrentPoll,
				 NodeState nodeState)
      throws IOException {
    callNecessaryPolls(lastOrCurrentPoll, null, nodeState,
                       nodeState.getState());
  }

  /**
   * Looks at the state of the node, and takes appropriate action.
   * Uses 'stateToUse' instead of 'nodeState.getState()' to allow special cases
   * such as ranged polls.  Called from the node manager.
   * @param lastOrCurrentPoll the most recent poll (could be active)
   * @param results the {@link Tallier}, if available
   * @param nodeState the {@link NodeState}
   * @param stateToUse the int state to use for checking
   * @throws IOException
   */
  private void callNecessaryPolls(PollState lastOrCurrentPoll, Tallier results,
                                  NodeState nodeState, int stateToUse)
      throws IOException {

    int protocolVersion = AuUtil.getProtocolVersion(managedAu);

    // Here's where we sneak in V3.  If this is a V1 AU, go ahead and
    // checkCurrentState.  If it's a V3 AU, and the node we're checking is
    // the AU node, and there's no current V3 poll running on this AU,
    // call a new poll.

    switch (protocolVersion) {
    case Poll.V1_PROTOCOL:
      checkCurrentState(lastOrCurrentPoll, results, nodeState, stateToUse, false);
      return;
    case Poll.V3_PROTOCOL:
      // If calling V3 polls has been disabled through the appropriate
      // parameter, just return.  Having this call here is less than ideal,
      // but will be moved out when we ditch the NodeManager.
      boolean disableV3Poller =
        CurrentConfig.getBooleanParam(PARAM_DISABLE_V3_POLLER,
                                      DEFAULT_DISABLE_V3_POLLER);
      if (disableV3Poller) {
        logger.debug("Skipping V3 poll on AU " + managedAu.getName() + 
                     " due to configuration.");
        return;
      }
      CachedUrlSet cus = nodeState.getCachedUrlSet();
      if (cus.getSpec().isAu()) {
        PollSpec spec = new PollSpec(cus, Poll.V3_POLL);
        // Don't call a poll on this if we're already running a V3 poll on it.
        if (!pollManager.isV3PollerRunning(spec) &&
            getAuState().getLastCrawlTime() > 0) {
          logger.debug("Starting V3 poll for " + managedAu.getName());
          callV3ContentPoll();
        } else {
          logger.debug("Not calling poll on " + managedAu.getName());
        }
      }
      return;
    default:
      logger.critical("Unsupported protocol version: " + protocolVersion);
      // XXX: Alerts!
      return;
    }
  }

  /**
   * Looks at the state of the node, and indicates if a poll needs to be called.
   * It does not schedule polls, which should be done via
   * 'callNecessaryPolls()'.  Called from the treewalk
   * @param lastOrCurrentPoll the most recent poll (could be active)
   * @param nodeState the {@link NodeState}
   * @return true if action should be taken
   * @throws IOException
   */
  public boolean checkCurrentState(PollState lastOrCurrentPoll,
				   NodeState nodeState)
      throws IOException {
    return checkCurrentState(lastOrCurrentPoll, null, nodeState,
                             nodeState.getState(), true);
  }

  /**
   * Looks at the state of the node, and takes appropriate action.  It calls
   * polls only if 'reportOnly' is false, though some conditions, such as
   * needing a top-level poll, do cause the state to change regardless.
   * Uses 'stateToUse' instead of 'nodeState.getState()' to allow special cases
   * such as ranged polls.
   * @param lastOrCurrentPoll the most recent poll (could be active)
   * @param results the {@link Tallier}, if available
   * @param nodeState the {@link NodeState}
   * @param stateToUse the int state to use for checking
   * @param reportOnly if true, nothing is actually scheduled
   * @return true if action taken (or to be taken)
   * @throws IOException
   */
  boolean checkCurrentState(PollState lastOrCurrentPoll,
                            Tallier results, NodeState nodeState,
                            int stateToUse, boolean reportOnly)
      throws IOException {
    switch (stateToUse) {
      case NodeState.NEEDS_POLL:
      case NodeState.NEEDS_REPLAY_POLL:
        // call content poll
        if (!reportOnly) {
          if (nodeState.getCachedUrlSet().getSpec().isAu()) {
            logger.debug2("calling top-level poll");
            callTopLevelPoll();
          } else {
            logger.debug2("calling content poll");
            callContentPoll(new PollSpec(nodeState.getCachedUrlSet(),
					 Poll.V1_CONTENT_POLL));
          }
        }
        return true;
      case NodeState.CONTENT_LOST:
      case NodeState.RANGED_CONTENT_LOST:
        if (!reportOnly) {
          if ((results!=null) &&
              (results.getCachedUrlSet().getSpec().isRangeRestricted())) {
              // if ranged, we know the names are accurate from the previous
              // name poll, so continue as if it had won
              logger.debug2("sub-dividing under lost ranged content poll");
              callContentPollsOnSubNodes(results.getCachedUrlSet());
          } else {
            // call name poll if not ranged or no results to use
            logger.debug2(
                "lost content poll, state = lost, calling name poll.");
            callNamePoll(new PollSpec(nodeState.getCachedUrlSet(),
				      Poll.V1_NAME_POLL));
          }
        }
        return true;
      case NodeState.UNREPAIRABLE_NAMES_NEEDS_POLL:
        // call name poll
        if (!reportOnly) {
          logger.debug2("calling name poll");
          callNamePoll(new PollSpec(nodeState.getCachedUrlSet(),
				    Poll.V1_NAME_POLL));
        }
        return true;
      case NodeState.CONTENT_RUNNING:
      case NodeState.CONTENT_REPLAYING:
      case NodeState.NAME_RUNNING:
      case NodeState.NAME_REPLAYING:
      case NodeState.SNCUSS_POLL_RUNNING:
      case NodeState.SNCUSS_POLL_REPLAYING:
        // replay poll if mine and not active
        if (lastOrCurrentPoll instanceof PollHistory) {
          PollHistory lastHistory = (PollHistory)lastOrCurrentPoll;
          PollSpec lastPollSpec = new PollSpec(nodeState.getCachedUrlSet(),
                                               lastHistory.lwrBound,
                                               lastHistory.uprBound,
					       lastHistory.getType());
          if ((lastHistory.isOurPoll())) {
            // if should recall and is our incomplete poll
            if (pollManager.isPollRunning(lastPollSpec)) {
              logger.debug2("unfinished poll already running, so not"+
                            " recalling.");
            } else {
              // if this poll should be running make sure it is running.
              if (!nodeMgrMgr.paramRestartNonexpiredPolls &&
                  ((lastHistory.startTime + lastHistory.duration) >
                  TimeBase.nowMs())) {
               logger.debug2("unfinished poll's duration not elapsed, so not"+
                             " recalling.");
              } else {
                // only restart if it has expired, since other caches will still be
                // running it otherwise
                if (!reportOnly) {
                  logger.debug("re-calling last unfinished poll.");
                  if (logger.isDebug2()) {
                    logger.debug2("lastHistory: " + lastHistory.getTypeString() +
                                  ", " +
                                  lastHistory.getStatusString() + ", " +
                                  sdf.format(new Date(lastHistory.startTime)));
                  }
                  // set state appropriately for restart
                  switch (nodeState.getState()) {
                    case NodeState.CONTENT_RUNNING:
                    case NodeState.CONTENT_REPLAYING:
                      nodeState.setState(NodeState.NEEDS_POLL);
                      break;
                    case NodeState.NAME_RUNNING:
                    case NodeState.NAME_REPLAYING:
                      nodeState.setState(NodeState.CONTENT_LOST);
                      break;
                    case NodeState.SNCUSS_POLL_RUNNING:
                    case NodeState.SNCUSS_POLL_REPLAYING:
                      nodeState.setState(NodeState.POSSIBLE_DAMAGE_HERE);
                  }
                  callLastPoll(lastPollSpec, lastHistory);
                }
                return true;
              }
            }
          } else {
            if (reportOnly) {
              // don't log twice if top-level poll is called next
              logger.debug2("unfinished poll not mine, so not recalling.");
            }

            // check if au-node which needs top-level poll
            // query the AU if a top level poll should be started
            if ((nodeState.getCachedUrlSet().getSpec().isAu()) &&
                (managedAu.shouldCallTopLevelPoll(auState))) {
              // switch to NEEDS_POLL to call toplevel poll next time.
              // this changes the state regardless of the 'reportOnly' setting
              // so as to avoid having to call 'shouldCallTopLevelPoll()' twice
              nodeState.setState(NodeState.NEEDS_POLL);
              logger.debug("set state to call top level poll...");
              return true;
            } else {
              // reset and do nothing
              nodeState.setState(NodeState.INITIAL);
            }
          }
        }
        return false;
      case NodeState.WRONG_NAMES:
      case NodeState.RANGED_WRONG_NAMES:
        // go through names, fixing (if have list)
        // if not active, recall name poll
        if (!reportOnly) {
          logger.debug2("lost name poll, collecting repair info.");
          if (results!=null) {
            logger.debug2("repairing name list");
            Iterator masterIt = results.getCorrectEntries();
            List localList = createUrlListFromCusIterator(results.getLocalEntries());
            boolean repairMarked = handleWrongNames(masterIt, localList, nodeState,
                results);
            if (!repairMarked) {
              lastOrCurrentPoll.status = PollState.REPAIRED;
            }
          } else {
            logger.debug2("no results found, so recalling name poll");
            callNamePoll(new PollSpec(nodeState.getCachedUrlSet(),
				      Poll.V1_NAME_POLL));
          }
        }
        return true;
      case NodeState.DAMAGE_AT_OR_BELOW:
        // subdivide or recurse, plus SNCUSS if not ranged
        if (!reportOnly) {
          logger.debug2("won name poll (mine), calling content poll on subnodes.");
          // call a content poll for this node's content if we haven't started
          // sub-dividing yet (and not an AU url, since they never have content)
          // (bug fix here to ignore any name polls which manage to get range
          // information)
          if (((lastOrCurrentPoll.getType() == Poll.V1_NAME_POLL) ||
               (lastOrCurrentPoll.getLwrBound() == null)) &&
              nodeState.getCachedUrlSet().hasContent()) {
            // only call SNCP if has content, since presence of content was
            // checked in the name poll
            logger.debug2("calling single node content poll on node's contents");
            nodeState.setState(NodeState.POSSIBLE_DAMAGE_HERE);
            callSingleNodeContentPoll(nodeState.getCachedUrlSet());
          } else {
            // no damage here since ranged or no content
            logger.debug2("setting to 'possible damage below'");
            nodeState.setState(NodeState.POSSIBLE_DAMAGE_BELOW);
          }
          // call a content poll on this node's subnodes
          CachedUrlSet cusToUse = (results!=null ? results.getCachedUrlSet() :
                                   nodeState.getCachedUrlSet());
          callContentPollsOnSubNodes(cusToUse);
        }
        return true;
      case NodeState.POSSIBLE_DAMAGE_HERE:
      case NodeState.UNREPAIRABLE_SNCUSS_NEEDS_POLL:
        // call SNCUSS poll
        if (!reportOnly) {
          logger.debug2("calling sncuss content poll");
          callSingleNodeContentPoll(nodeState.getCachedUrlSet());
        }
        return true;
      case NodeState.NEEDS_REPAIR:
        //schedule repair, replay poll (if exists)
        if (!reportOnly) {
          logger.debug2("scheduling repair");
          List repairUrl = ListUtil.list(nodeState.getCachedUrlSet().getUrl());
          if (results != null) {
            // give poll info for replay
            String key = results.getPollKey();
            markNodesForRepair(repairUrl, key, nodeState.getCachedUrlSet(),
                               false, pollManager.acquirePollLock(key));
          }
          else {
            // no poll info to replay
            markNodesForRepair(repairUrl, null, nodeState.getCachedUrlSet(),
                               false, null);
          }
        }
        return true;
      case NodeState.UNREPAIRABLE_SNCUSS:
        //XXX delay by at least one treewalk via state shifting
        nodeState.setState(NodeState.UNREPAIRABLE_SNCUSS_WAITING);
        return false;
      case NodeState.UNREPAIRABLE_SNCUSS_WAITING:
        //XXX check times, and changed to 'UNREPAIRABLE_XXX_NEEDS_POLL" if long enough
        // if long enough since first damage, change to 'UNKNOWN'
        nodeState.setState(NodeState.UNREPAIRABLE_SNCUSS_NEEDS_POLL);
        return false;
      case NodeState.UNREPAIRABLE_NAMES:
        //XXX delay by at least one treewalk via state shifting
        nodeState.setState(NodeState.UNREPAIRABLE_NAMES_WAITING);
        return false;
      case NodeState.UNREPAIRABLE_NAMES_WAITING:
        //XXX check times, and changed to 'UNREPAIRABLE_XXX_NEEDS_POLL" if long enough
        // if long enough since first damage, change to 'UNKNOWN'
        nodeState.setState(NodeState.UNREPAIRABLE_NAMES_NEEDS_POLL);
        return false;
      case NodeState.POSSIBLE_DAMAGE_BELOW:
        // waiting state
        // if no results, then in treewalk and should recall any necessary
        // ranged histories, or clear damage
        if (results == null) {
          if (lastOrCurrentPoll instanceof PollHistory) {
            // check through history to see if action needed on ranged polls
            PollHistory lastHistory = (PollHistory) lastOrCurrentPoll;
            if (lastHistory.getLwrBound() != null) {
              // if ranged, check to see if anything needed
              logger.debug2("ranged history found.  Checking previous histories");
              if (checkLastHistory(lastHistory, nodeState, reportOnly)) {
                // if action needed, stop here
                logger.debug2("action taken on histories");
                return true;
              }
            }
          }

          // otherwise, recall poll to clear damage
          if (!reportOnly) {
            logger.debug2("re-calling content poll");
            callContentPoll(nodeState.getCachedUrlSet(), null, null);
          }
          return true;
        }
        return false;
      case NodeState.INITIAL:
      case NodeState.OK:
        // check if toplevel poll needed when AU node
        if (nodeState.getCachedUrlSet().getSpec().isAu()) {
          // query the AU if a top level poll should be started.
          // this changes the state regardless of the 'reportOnly' setting so
          // as to avoid having to call 'shouldCallTopLevelPoll()' twice
          if (managedAu.shouldCallTopLevelPoll(auState)) {
            // switch to NEEDS_POLL if necessary to call toplevel poll
            nodeState.setState(NodeState.NEEDS_POLL);
            logger.debug("set state to call top level poll...");
            return true;
          }
        }
        return false;
    }
    return false;
  }

  public HashMap getLastTopLevelVoteHistory() {
    HashMap voteMap = new HashMap();
    NodeState node = getNodeState(managedAu.getAuCachedUrlSet());
    Iterator history_it = node.getPollHistories();
    while (history_it.hasNext()) {
      PollHistory history = (PollHistory) history_it.next();
      Iterator votes_it = history.getVotes();
      while(votes_it.hasNext()) {
        Vote vote = (Vote) votes_it.next();
        PeerIdentity voterID = vote.getVoterIdentity();
        ArrayList list = (ArrayList) voteMap.get(voterID);
        if(list == null) {
          list = new ArrayList();
          voteMap.put(voterID,list);
        }
        list.add(vote);
      }
    }
    return voteMap;
  }

  /**
   * Determines if the current state is appropriate for the type of poll
   * currently finishing.
   * @param poll PollState
   * @param node NodeState
   * @param spec CachedUrlSetSpec
   * @return boolean true iff the states are consistent
   */
  boolean checkValidStatesForResults(PollState poll, NodeState node,
                                     CachedUrlSetSpec spec) {
    boolean isContent = poll.getType()==Poll.V1_CONTENT_POLL;
    boolean isName = poll.getType()==Poll.V1_NAME_POLL;
    boolean isSNCUSS = spec.isSingleNode();
    // these have to be fairly open-ended to permit error states
    boolean notRunning = poll.getStatus() != PollState.RUNNING;
    boolean notRepairing = poll.getStatus() != PollState.REPAIRING;

    switch (node.getState()) {
      case NodeState.CONTENT_RUNNING:
        // running content poll ok
        return (isContent && notRepairing && !isSNCUSS);
      case NodeState.CONTENT_REPLAYING:
        // repairing content poll ok
        return (isContent && notRunning && !isSNCUSS);
      case NodeState.SNCUSS_POLL_RUNNING:
        // running sncuss poll ok
        return (isContent && notRepairing && isSNCUSS);
      case NodeState.SNCUSS_POLL_REPLAYING:
        // repairing sncuss poll ok
        return (isContent && notRunning && isSNCUSS);
      case NodeState.NAME_RUNNING:
        // running name poll ok
        return (isName && notRepairing && !isSNCUSS);
      case NodeState.NAME_REPLAYING:
        // repairing name poll ok
        return (isName && notRunning && !isSNCUSS);
      case NodeState.INITIAL:
      case NodeState.OK:
      case NodeState.NEEDS_POLL:
      case NodeState.NEEDS_REPLAY_POLL:
      case NodeState.CONTENT_LOST:
      case NodeState.UNREPAIRABLE_NAMES_NEEDS_POLL:
      case NodeState.WRONG_NAMES:
      case NodeState.DAMAGE_AT_OR_BELOW:
      case NodeState.POSSIBLE_DAMAGE_HERE:
      case NodeState.UNREPAIRABLE_SNCUSS_NEEDS_POLL:
      case NodeState.NEEDS_REPAIR:
      case NodeState.UNREPAIRABLE_SNCUSS:
      case NodeState.UNREPAIRABLE_NAMES:
      case NodeState.POSSIBLE_DAMAGE_BELOW:
      default:
        // no polls should be finishing when the node is in one of these states
        return false;
    }
  }

  /**
   * Looks at the last history from a node (during a treewalk), and returns true
   * if action is necessary (and taken, if 'reportOnly' is false).
   * @param lastHistory the {@link PollHistory}
   * @param node the {@link NodeState}
   * @param reportOnly if true, nothing is actually scheduled
   * @return true if poll scheduled
   */
  boolean checkLastHistory(PollHistory lastHistory, NodeState node,
                           boolean reportOnly) {
    PollSpec lastPollSpec = new PollSpec(node.getCachedUrlSet(),
                                         lastHistory.lwrBound,
                                         lastHistory.uprBound,
					 lastHistory.type);
    boolean shouldRecallLastPoll =
        ((lastHistory.getStartTime() + lastHistory.getDuration() +
	  nodeMgrMgr.paramRecallDelay)
         <= TimeBase.nowMs());
    if (!shouldRecallLastPoll) {
      // too early to act, regardless of state
      return false;
    }
    switch (lastHistory.status) {
      case PollState.REPAIRING:
      case PollState.SCHEDULED:
      case PollState.RUNNING:
      case PollState.UNFINISHED:
        // if this poll should be running make sure it is running.
        if (!pollManager.isPollRunning(lastPollSpec)) {
          // if should recall and is our incomplete poll
          if ((lastHistory.isOurPoll())) {
            // only restart if it has expired, since other caches will still be
            // running it otherwise
            if (nodeMgrMgr.paramRestartNonexpiredPolls ||
                ((lastHistory.startTime + lastHistory.duration) <
                TimeBase.nowMs())) {
              if (!reportOnly) {
                logger.debug("Re-calling last unfinished poll.");
                if (logger.isDebug2()) {
                  logger.debug2("lastHistory: " + lastHistory.getTypeString() +
                                ", " +
                                lastHistory.getStatusString() + ", " +
                                sdf.format(new Date(lastHistory.startTime)));
                }
                callLastPoll(lastPollSpec, lastHistory);
              }
              return true;
            }
          }
        }
        break;
      case PollState.WON:
      case PollState.REPAIRED:
/* only need consider if there are lurking polls
        if (lastHistory.getType() == Poll.NAME_POLL) {
          // since this is post-order, we must have dealt with all children
          // already.  Since name polls are only called when content polls fail,
          // we should rerun a content poll on this node (if our name poll).
          if ((lastHistory.isOurPoll())) {
            if (!reportOnly) {
              logger.debug("Calling new content poll on won name poll.");
              logger.debug2("lastHistory: "+lastHistory.getTypeString() + ", " +
                           lastHistory.getStatusString() + ", " +
                           sdf.format(new Date(lastHistory.startTime)));
              PollSpec newPollSpec = new PollSpec(node.getCachedUrlSet(),
                                                   null, null,
						   Poll.CONTENT_POLL);
              callContentPoll(newPollSpec);
            }
            return true;
          }
        } else if (lastHistory.lwrBound != null &&
                   lastHistory.uprBound != null) {
 */
        // since this is a poll with a range make sure we don't have
        // a lost poll lurking underneath this one.
        // look at the polls below this one for a range poll that failed
        Iterator history_it = node.getPollHistories();
        while (history_it.hasNext()) {
          PollHistory prevHistory = (PollHistory)history_it.next();
          CachedUrlSet cus1 = lastPollSpec.getCachedUrlSet();
          PollSpec prevPollSpec = new PollSpec(node.getCachedUrlSet(),
                                               prevHistory.lwrBound,
                                               prevHistory.uprBound,
					       prevHistory.type);
          CachedUrlSet cus2 = prevPollSpec.getCachedUrlSet();
          if ((cus1.cusCompare(cus2) == CachedUrlSet.SAME_LEVEL_NO_OVERLAP)) {
            if (prevHistory.getStatus()==PollState.LOST) {
              // found the failed poll, rerunning
              if (!reportOnly) {
                logger.debug("Re-calling previous poll.");
                if (logger.isDebug2()) {
                  logger.debug2("lastHistory: " + lastHistory.getTypeString() +
                                ", " + lastHistory.getStatusString() + ", " +
                                sdf.format(new Date(lastHistory.startTime)));
                }
                callLastPoll(prevPollSpec, prevHistory);
              }
              return true;
            }
          } else {
            // found overlapping poll, so done perusing ranged divisions
            break;
          }
        }
        break;
      case PollState.LOST:
        // since we didn't find anything below this, it's time to recall the
        // upper poll.  But 'checkCurrentState()' will take care of that
        return false;
      case PollState.UNREPAIRABLE:
        // XXX this needs to be fixed eventually but for now we only get
        // our stuff from the publisher we're going to keep trying until we
        // fail.
        if (!reportOnly) {
          logger.debug("Re-calling poll on unrepairable node to trigger repair");
          if (logger.isDebug2()) {
            logger.debug2("lastHistory: " + lastHistory.getTypeString() + ", " +
                          lastHistory.getStatusString() + ", " +
                          sdf.format(new Date(lastHistory.startTime)));
          }
          callLastPoll(lastPollSpec, lastHistory);
        }
        return true;
      case PollState.INCONCLUSIVE:
      case PollState.ERR_SCHEDULE_HASH:
      case PollState.ERR_HASHING:
      case PollState.ERR_IO:
      case PollState.ERR_UNDEFINED:
        // if we ended with an error and it was our poll,
        // we need to recall this poll.
        if (lastHistory.isOurPoll()) {
          if (!reportOnly) {
            logger.debug("Recalling last unsuccessful poll");
            if (logger.isDebug2()) {
              logger.debug2("lastHistory: " + lastHistory.getTypeString() +
                            ", " + lastHistory.getStatusString() + ", " +
                            sdf.format(new Date(lastHistory.startTime)));
            }
            callLastPoll(lastPollSpec, lastHistory);
          }
          return true;
        }
    }
    return false;
  }

  /**
   * Gets the poll key from a PollHistory.
   * @param history PollHistory
   * @return String the key
   */
  String findKey(PollHistory history) {
    Iterator votes = history.getVotes();
    if (votes.hasNext()) {
      Vote a_vote = (Vote) votes.next();
      return a_vote.getPollKey();
    }
    return "";
  }

  /**
   * Convenience method to call a top-level V3 content poll.
   */
  void callV3ContentPoll() {
    PollSpec spec = new PollSpec(managedAu.getAuCachedUrlSet(), Poll.V3_POLL);
    logger.debug2("Calling a V3 Content Poll on " + spec);
    if (pollManager.callPoll(spec) == null) {
      if (logger.isDebug2()) {
        logger.debug2("Failed to call a top level poll on " + spec);
      }
    }
  }

  /**
   * Convenience method to call a top-level poll.
   */
  void callTopLevelPoll() {
    PollSpec spec = new PollSpec(managedAu.getAuCachedUrlSet(),
				 Poll.V1_CONTENT_POLL);
    if (logger.isDebug2()) {
      logger.debug2("Calling a top level poll on " + spec);
    }
    if (pollManager.callPoll(spec) == null) {
      if (logger.isDebug2()) {
	logger.debug2("Failed to call a top level poll on " + spec);
      }
    }
  }

  /**
   * Closes the poll described in the PollState, creating a new PollHistory in
   * the NodeState.
   * @param pollState PollState
   * @param duration long
   * @param votes Collection
   * @param nodeState NodeState
   */
  void closePoll(PollState pollState, long duration, Collection votes,
                         NodeState nodeState) {
    PollHistory history = new PollHistory(pollState, duration, votes);
    ((NodeStateImpl)nodeState).closeActivePoll(history);
    if (logger.isDebug2()) {
      logger.debug2("Closing poll for url '" +
                    nodeState.getCachedUrlSet().getUrl() + " " +
                    pollState.getLwrBound() + "-" +
                    pollState.getUprBound() + "'");
    }
  }

  /**
   * Finds a PollState in the NodeState which matches the results spec.
   * @param state NodeState
   * @param results Tallier
   * @return PollState null if not found
   */
  private PollState getPollState(NodeState state, Tallier results) {
    Iterator polls = state.getActivePolls();
    PollSpec spec = results.getPollSpec();
    if (logger.isDebug2()) {
      logger.debug2("Getting poll state for spec: " + spec);
    }
    while (polls.hasNext()) {
      PollState pollState = (PollState) polls.next();
      if (StringUtil.equalStrings(pollState.getLwrBound(), spec.getLwrBound()) &&
          StringUtil.equalStrings(pollState.getUprBound(), spec.getUprBound()) &&
          (pollState.getType() == results.getType())) {
        return pollState;
      }
    }
    return null;
  }

  /**
   * Maps from Poll error codes to PollState error codes.
   * @param resultsErr the Poll.ERR_xxx
   * @return int the PollState.ERR_xxx
   */
  static int mapResultsErrorToPollError(int resultsErr) {
    switch (resultsErr) {
      case Poll.ERR_HASHING:
        return PollState.ERR_HASHING;
      case Poll.ERR_IO:
        return PollState.ERR_IO;
      case Poll.ERR_SCHEDULE_HASH:
        return PollState.ERR_SCHEDULE_HASH;
    }
    return PollState.ERR_UNDEFINED;
  }

  /**
   * Marks the given urls for repair, associating them with the repairing CUS.
   * Then calls the CrawlManager and starts a repair crawl on them.  Requires
   * an activity lock on the repairing CUS.
   * @param urls the urls to repair
   * @param pollKey the active poll
   * @param cus the repairing CUS
   * @param isNamePoll true iff a name poll
   * @param lock the activity lock
   */
  void markNodesForRepair(Collection urls, String pollKey,
      CachedUrlSet cus, boolean isNamePoll, ActivityRegulator.Lock lock) {
    if (pollKey!=null) {
      logger.debug2("suspending poll " + pollKey);
      pollManager.suspendPoll(pollKey);
    } else {
      logger.debug2("no poll found to suspend");
    }

    if (logger.isDebug2()) {
      if (urls.size() == 1) {
        logger.debug2("scheduling repair");
      } else {
        logger.debug2("scheduling " + urls.size() + " repairs");
      }

      Iterator iter = urls.iterator();
      while (iter.hasNext()) {
        String url = (String)iter.next();
        if (!damagedNodes.containsToRepair(cus, url)) {
	  //XXX bogus, we aren't doing anything here
          logger.debug2("Adding '" + url + "' to repair list...");
        }
      }
    }

    damagedNodes.addToRepair(cus, urls);

    PollCookie cookie = new PollCookie(cus, pollKey, isNamePoll, urls, lock);
    getDaemon().getCrawlManager().startRepair(managedAu, urls,
        new ContentRepairCallback(), cookie, lock);
  }

  /**
   * Deletes the node from the LockssRepository.
   * @param cus CachedUrlSet
   * @throws IOException
   */
  public void deleteNode(CachedUrlSet cus) throws IOException {
    LockssRepository repository = getDaemon().getLockssRepository(managedAu);
    repository.deleteNode(cus.getUrl());
    NodeState extraState = getNodeState(cus);
    extraState.getCrawlState().type = CrawlState.NODE_DELETED;
  }

  /**
   * Deactivates the node in the LockssRepository.
   * @param cus CachedUrlSet
   * @throws IOException
   */
  private void deactivateNode(CachedUrlSet cus) throws IOException {
    LockssRepository repository = getDaemon().getLockssRepository(managedAu);
    repository.deactivateNode(cus.getUrl());
  }

  /**
   * Calls content polls on the children of the CUS.  Subdivides via two ranged
   * polls if the child count is greater than four.
   * @param cus CachedUrlSet
   * @throws IOException
   */
  private void callContentPollsOnSubNodes(CachedUrlSet cus) throws IOException {
    Iterator children = cus.flatSetIterator();
    List childList = convertChildrenToCusList(children);
    // Divide the list in two and call two new content polls
    if (childList.size() > 4) {
      logger.debug2("more than 4 children, calling ranged content polls.");
      int mid = childList.size() / 2;

      // the first half of the list
      String lwr = ((CachedUrlSet) childList.get(0)).getUrl();
      String upr = ((CachedUrlSet) childList.get(mid)).getUrl();
      callContentPoll(cus, lwr, upr);

      // the second half of the list
      lwr = ((CachedUrlSet) childList.get(mid + 1)).getUrl();
      upr = ((CachedUrlSet) childList.get(childList.size() - 1)).getUrl();
      callContentPoll(cus, lwr, upr);
    } else if (childList.size() > 0) {
      logger.debug2("less than 4 children, calling content poll on each.");
      for (int i = 0; i < childList.size(); i++) {
        callContentPoll((CachedUrlSet)childList.get(i), null, null);
      }
    } else {
      logger.debug2("0 children, calling no content polls.");
    }
  }

  /**
   * Calls a content poll on the CUS with the given bounds (can be null).
   * @param cus CachedUrlSet
   * @param lwr lower bound
   * @param upr upper bound
   */
  private void callContentPoll(CachedUrlSet cus, String lwr, String upr) {
    String base = cus.getUrl();
    // check the bounds and trim the base url, if present
    if (lwr != null) {
      lwr = lwr.startsWith(base) ? lwr.substring(base.length()) : lwr;
    }
    if (upr != null) {
      upr = upr.startsWith(base) ? upr.substring(base.length()) : upr;
    }
    ArchivalUnit au = cus.getArchivalUnit();
    CachedUrlSet newCus =
      au.makeCachedUrlSet(new RangeCachedUrlSetSpec(base, lwr, upr));
    PollSpec spec = new PollSpec(newCus, lwr, upr, Poll.V1_CONTENT_POLL);
    callPoll(spec);
  }

  /**
   * Calls a single node content poll on the CUS.
   * @param cus CachedUrlSet
   */
  private void callSingleNodeContentPoll(CachedUrlSet cus) {
    // create a 'single node' CachedUrlSet
    ArchivalUnit au = cus.getArchivalUnit();
    CachedUrlSet newCus =
      au.makeCachedUrlSet(new SingleNodeCachedUrlSetSpec(cus.getUrl()));
    PollSpec spec = new PollSpec(newCus, Poll.V1_CONTENT_POLL);
    callPoll(spec);
  }

  /**
   * Recalls a poll matching the last poll given by the PollState.
   * @param spec new spec
   * @param lastPoll last poll to recall
   */
  private void callLastPoll(PollSpec spec, PollState lastPoll) {
    if (spec.getPollType() != lastPoll.type)
      logger.error("Re-calling a " + Poll.POLL_NAME[lastPoll.type] +
		   " poll but spec is " + Poll.POLL_NAME[spec.getPollType()]);
    if (logger.isDebug2()) {
      logger.debug2("Re-calling a " + Poll.POLL_NAME[lastPoll.type] +
		    " poll on " + spec);
    }
    if (pollManager.callPoll(spec) == null) {
      if (logger.isDebug2()) {
	logger.debug2("Failed to re-call a " + Poll.POLL_NAME[lastPoll.type] +
		      " poll on " + spec);
      }
    }
  }

  /**
   * Calls a poll with the given spec.
   * @param spec PollSpec
   */
  private void callPoll(PollSpec spec) {
    if (logger.isDebug2()) {
      logger.debug2("Calling a poll on " + spec);
    }
    if (pollManager.callPoll(spec) == null) {
      if (logger.isDebug2()) {
	logger.debug2("Failed to call a poll on " + spec);
      }
    }
  }

  /**
   * Calls a name poll with the given spec.
   * @param spec PollSpec
   */
  private void callNamePoll(PollSpec spec) {
    if (spec.getPollType() != Poll.V1_NAME_POLL) {
      logger.error("callNamePoll on spec for " +
		   Poll.POLL_NAME[spec.getPollType()]);
    }
    callPoll(spec);
  }

  /**
   * Calls a content poll with the given spec.
   * @param spec PollSpec
   */
  private void callContentPoll(PollSpec spec) {
    if (spec.getPollType() != Poll.V1_CONTENT_POLL) {
      logger.error("Calling a content poll on spec for " +
		   Poll.POLL_NAME[spec.getPollType()]);
    }
    callPoll(spec);
  }

  /**
   * Converts a list of CachedUrlSetNodes into a list of CachedUrlSets.
   * @param children Iterator of CUSNodes
   * @return List of CUSs
   */
  private List convertChildrenToCusList(Iterator children) {
    ArrayList childList = new ArrayList();
    if (children != null) {
      while (children.hasNext()) {
        CachedUrlSetNode child = (CachedUrlSetNode) children.next();
        CachedUrlSet cus = null;
        switch (child.getType()) {
          case CachedUrlSetNode.TYPE_CACHED_URL_SET:
            cus = (CachedUrlSet) child;
            break;
          case CachedUrlSetNode.TYPE_CACHED_URL:
            CachedUrlSetSpec rSpec = new RangeCachedUrlSetSpec(child.getUrl());
            cus = managedAu.makeCachedUrlSet(rSpec);
        }
        childList.add(cus);
      }
    }
    return childList;
  }

  /**
   * Returns true if the CUS has damage.
   * @param cus CachedUrlSet
   * @return boolean true iff has damage.
   */
  boolean hasDamage(CachedUrlSet cus) {
    return damagedNodes.hasDamage(cus);
  }

  /**
   * Update reputations based on the results.
   * @param results Tallier
   */
  private void updateReputations(Tallier results) {
    IdentityManager idManager = getDaemon().getIdentityManager();
    Iterator voteIt = results.getPollVotes().iterator();
    int agreeChange = 0;
    int disagreeChange = 0;

    if (results.getTallyResult() == Tallier.RESULT_WON) {
      agreeChange = IdentityManager.AGREE_VOTE;
      disagreeChange = IdentityManager.DISAGREE_VOTE;
    } else if (results.getTallyResult() == Tallier.RESULT_LOST) {
      agreeChange = IdentityManager.DISAGREE_VOTE;
      disagreeChange = IdentityManager.AGREE_VOTE;
    }
    while (voteIt.hasNext()) {
      Vote vote = (Vote) voteIt.next();
      int repChange = vote.isAgreeVote() ? agreeChange : disagreeChange;

      PeerIdentity originatorID = vote.getVoterIdentity();
      idManager.changeReputation(originatorID, repChange);
      if (results.getTallyResult() == Tallier.RESULT_WON) {
	if (vote.isAgreeVote()) {
	  //check if is top level poll
	  if (results.getCachedUrlSet().getSpec().isAu()) {
	    idManager.signalAgreed(originatorID, managedAu);
 	  }
	} else {
	  idManager.signalDisagreed(originatorID, managedAu);
	}
      }
    }
  }

  private List createUrlListFromCusIterator(Iterator cusIt) {
    List list = new ArrayList();
    while (cusIt.hasNext()) {
      list.add(cusIt.next());
    }
    return list;
  }

  /**
   * Checks if the given spec was a content poll on the top level.
   * @param spec CachedUrlSetSpec
   * @param type poll type
   * @return boolean true iff content on AU spec
   */
  boolean isTopLevelPollFinished(CachedUrlSetSpec spec, int type) {
    return (spec.isAu() && (type == Poll.V1_CONTENT_POLL));
  }

  public boolean repairsNeeded() {
    return !damagedNodes.getNodesToRepair().isEmpty();
  }

  public void scheduleRepairs(ActivityRegulator.Lock auLock) {
    HashMap repairs = damagedNodes.getNodesToRepair();
    if (!repairs.isEmpty()) {
      logger.debug("Found nodes needing repair; scheduling repairs...");

      // if there's only one lock needed, use the streamlined code
      if (repairs.keySet().size()==1) {
        // get cus
        Iterator cusKeys = repairs.keySet().iterator();
        String cusUrl = (String)cusKeys.next();
        CachedUrlSet cus = managedAu.makeCachedUrlSet(
            new RangeCachedUrlSetSpec(cusUrl));

        // get lock
        NodeState node = getNodeState(cus);
        boolean isNamePoll = (node.getState() == NodeState.WRONG_NAMES);
        int activity = (isNamePoll ? ActivityRegulator.STANDARD_NAME_POLL
            : ActivityRegulator.REPAIR_CRAWL);
        ActivityRegulator.CusLock cusLock =
            (ActivityRegulator.CusLock)regulator.changeAuLockToCusLock(
            auLock, cus, activity, Constants.HOUR);
        if (cusLock!=null) {
          // schedule repair
          Collection localCol =
              new ArrayList((Collection)repairs.get(cus.getUrl()));
          markNodesForRepair(localCol, null, cus, isNamePoll, cusLock);
        } else {
          logger.debug("Unable to obtain lock for repairs on CUS '"+cusUrl+"'");
        }
      } else {
        // a little more involved if there are several locks needed
        List cusReqList = new ArrayList();
        Iterator cusKeys = repairs.keySet().iterator();
        // make a list of cus activity requests
        while (cusKeys.hasNext()) {
          String cusUrl = (String)cusKeys.next();
          CachedUrlSet cus = managedAu.makeCachedUrlSet(
              new RangeCachedUrlSetSpec(cusUrl));
          NodeState node = getNodeState(cus);
          boolean isNamePoll = (node.getState() == NodeState.WRONG_NAMES);
          int activity = (isNamePoll ? ActivityRegulator.STANDARD_NAME_POLL
              : ActivityRegulator.REPAIR_CRAWL);
          cusReqList.add(new ActivityRegulator.CusLockRequest(cus,
              activity, Constants.HOUR));
        }

        // get the locks back and start repairs on them
        // if some CUSets were denied, they'll be tried again eventually
        List lockList = regulator.changeAuLockToCusLocks(auLock,
            cusReqList);
        Iterator lockIter = lockList.iterator();
        while (lockIter.hasNext()) {
          ActivityRegulator.CusLock cusLock =
              (ActivityRegulator.CusLock)lockIter.next();
          CachedUrlSet cus = cusLock.getCachedUrlSet();
          Collection localCol =
              new ArrayList((Collection)repairs.get(cus.getUrl()));
          NodeState node = getNodeState(cus);
          boolean isNamePoll = (node.getState() == NodeState.WRONG_NAMES);
          markNodesForRepair(localCol, null, cus, isNamePoll, cusLock);
        }
      }
    }
  }

  /**
   * Callback for a content repair crawl.
   */
  class ContentRepairCallback implements CrawlManager.Callback {
    /**
     * @param success whether the repair was successful or not
     * @param cookie object used by callback to designate which repair
     * attempt this is
     */
    public void signalCrawlAttemptCompleted(boolean success,
					    Object cookie,
					    Crawler.Status status) {
      PollCookie pollCookie = (PollCookie)cookie;
      CachedUrlSet cus = pollCookie.cus;

      //XXX should check success (or get passed in fetched url list)

      Iterator urlIter = pollCookie.urlsToRepair.iterator();
      while (urlIter.hasNext()) {
        String url = (String)urlIter.next();
        logger.debug2("Removing '"+url+"' from repair list...");
        damagedNodes.removeFromRepair(cus, url);
      }

      logger.debug("Content crawl completed repair on " + cus.getUrl());
      // set state properly
      NodeState state = getNodeState(cus);

      // resume poll (or call new one)
      if (pollCookie.pollKey!=null) {
        logger.debug("Resuming poll...");
        // set state to replaying, since resumed polls don't call 'startPoll()'
        if (pollCookie.isNamePoll) {
          state.setState(NodeState.NAME_REPLAYING);
        } else {
          state.setState(NodeState.SNCUSS_POLL_REPLAYING);
        }
        pollManager.resumePoll(success, pollCookie.pollKey, pollCookie.lock);
      } else {
        // if we need to release our lock, make sure we do it.
        if(pollCookie.lock != null) {
          pollCookie.lock.expire();
        }
        if (pollCookie.isNamePoll) {
          logger.debug("Calling new name poll...");
          state.setState(NodeState.WRONG_NAMES);
          callNamePoll(new PollSpec(cus, Poll.V1_NAME_POLL));
        } else {
          logger.debug("Calling new SNCUSS poll...");
          state.setState(NodeState.POSSIBLE_DAMAGE_HERE);
	  callSingleNodeContentPoll(cus);
        }
      }
    }
  }

  /**
   * Cookie object for content repair crawl.  Contains the CUS, poll key,
   * type, and list of urls to repair.
   */
  static class PollCookie {
    CachedUrlSet cus;
    String pollKey;
    boolean isNamePoll;
    Collection urlsToRepair;
    ActivityRegulator.Lock lock;

    PollCookie(CachedUrlSet cus, String pollKey, boolean isNamePoll,
        Collection urlsToRepair, ActivityRegulator.Lock lock) {
      this.cus = cus;
      this.pollKey = pollKey;
      this.isNamePoll = isNamePoll;
      this.urlsToRepair = urlsToRepair;
      this.lock = lock;
    }
  }

  /**
   * Factory to create new NodeManager instances.
   */
  public static class Factory implements LockssAuManager.Factory {
    public LockssAuManager createAuManager(ArchivalUnit au) {
      return new NodeManagerImpl(au);
    }
  }
}
