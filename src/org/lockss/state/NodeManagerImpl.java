/*
 * $Id: NodeManagerImpl.java,v 1.143 2003-07-09 19:25:19 clairegriffin Exp $
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
import org.lockss.daemon.status.*;
import org.lockss.poller.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.protocol.*;
import org.lockss.repository.*;
import org.lockss.crawler.CrawlManager;
import org.apache.commons.collections.LRUMap;

/**
 * Implementation of the NodeManager.
 */
public class NodeManagerImpl extends BaseLockssManager implements NodeManager {
  /**
   * This parameter indicates the size of the {@link NodeStateCache} used by the
   * node manager.
   */
  public static final String PARAM_NODESTATE_CACHE_SIZE =
      Configuration.PREFIX + "state.cache.size";

  static final int DEFAULT_NODESTATE_CACHE_SIZE = 100;

  /**
   * This parameter indicates how long since the last unsuccessful poll on a
   * node the node manager should wait before recalling that poll.
   */
  public static final String PARAM_RECALL_DELAY =
      Configuration.PREFIX + "state.recall.delay";

  static final long DEFAULT_RECALL_DELAY = Constants.DAY;

  /**
   * This parameter indicates whether or not polls should be restarted if their
   * deadline hasn't expired yet.
   */
  public static final String PARAM_RESTART_NONEXPIRED_POLLS =
      Configuration.PREFIX + "state.restart.nonexpired.polls";

  static final boolean DEFAULT_RESTART_NONEXPIRED_POLLS = false;

  static HistoryRepository historyRepo;
  private LockssRepository lockssRepo;
  static PollManager pollManager;
  static ActivityRegulator regulator;
  static SimpleDateFormat sdf = new SimpleDateFormat();

  static boolean registeredAccessors = false;

  ArchivalUnit managedAu;
  AuState auState;
  NodeStateCache nodeCache;
  HashMap activeNodes;
  int maxCacheSize = DEFAULT_NODESTATE_CACHE_SIZE;
  long recallDelay = DEFAULT_RECALL_DELAY;
  boolean restartNonexpiredPolls = DEFAULT_RESTART_NONEXPIRED_POLLS;
  /**
   * the set of nodes marked damaged (these are nodes which have lost content
   * poll).
   */
  DamagedNodeSet damagedNodes;

  private static Logger logger = Logger.getLogger("NodeManager");
  TreeWalkHandler treeWalkHandler;

  NodeManagerImpl(ArchivalUnit au) {
    managedAu = au;
  }

  public void startService() {
    super.startService();
    logger.debug("NodeManager being started");
    historyRepo = theDaemon.getHistoryRepository();
    lockssRepo = theDaemon.getLockssRepository(managedAu);
    pollManager = theDaemon.getPollManager();
    regulator = theDaemon.getActivityRegulator(managedAu);

    nodeCache = new NodeStateCache(maxCacheSize);
    activeNodes = new HashMap();

    auState = historyRepo.loadAuState(managedAu);
    damagedNodes = historyRepo.loadDamagedNodeSet(managedAu);

    treeWalkHandler = new TreeWalkHandler(this, theDaemon);
    treeWalkHandler.start();

    // register our status (only once)
    if (!registeredAccessors) {
      StatusService statusServ = theDaemon.getStatusService();
      NodeManagerStatus nmStatus = new NodeManagerStatus(theDaemon);

      statusServ.registerStatusAccessor(NodeManagerStatus.SERVICE_STATUS_TABLE_NAME,
                                        new NodeManagerStatus.ServiceStatus());
      statusServ.registerStatusAccessor(NodeManagerStatus.MANAGER_STATUS_TABLE_NAME,
                                        new NodeManagerStatus.ManagerStatus());

      statusServ.registerStatusAccessor(NodeManagerStatus.POLLHISTORY_STATUS_TABLE_NAME,
                                        new NodeManagerStatus.PollHistoryStatus());
      registeredAccessors = true;
      logger.debug2("Status accessors registered.");
    }

    logger.debug("NodeManager successfully started");
  }

  public void stopService() {
    logger.debug("NodeManager being stopped");
    if (treeWalkHandler != null) {
      treeWalkHandler.end();
      treeWalkHandler = null;
    }
    activeNodes.clear();
    damagedNodes.clear();
    nodeCache.clear();

    // unregister our status accessors
    if (registeredAccessors) {
      StatusService statusServ = theDaemon.getStatusService();
      statusServ.unregisterStatusAccessor(NodeManagerStatus.
                                          SERVICE_STATUS_TABLE_NAME);
      statusServ.unregisterStatusAccessor(NodeManagerStatus.
                                          MANAGER_STATUS_TABLE_NAME);
      statusServ.unregisterStatusAccessor(NodeManagerStatus.
                                          POLLHISTORY_STATUS_TABLE_NAME);
      registeredAccessors = false;
      logger.debug2("Status accessors unregistered.");
    }

    super.stopService();
    logger.debug("NodeManager successfully stopped");
  }

  protected void setConfig(Configuration newConfig,
                           Configuration prevConfig,
                           Set changedKeys) {
    recallDelay = newConfig.getTimeInterval(PARAM_RECALL_DELAY,
                                            DEFAULT_RECALL_DELAY);
    restartNonexpiredPolls =
        newConfig.getBoolean(PARAM_RESTART_NONEXPIRED_POLLS,
                             DEFAULT_RESTART_NONEXPIRED_POLLS);
    if (changedKeys.contains(PARAM_NODESTATE_CACHE_SIZE)) {
      maxCacheSize = newConfig.getInt(PARAM_NODESTATE_CACHE_SIZE,
                                      DEFAULT_NODESTATE_CACHE_SIZE);
      if (nodeCache != null) {
        nodeCache.setCacheSize(maxCacheSize);
      }
    }
  }

  public synchronized NodeState getNodeState(CachedUrlSet cus) {
    String url = cus.getUrl();
    NodeState node = nodeCache.getState(url);
    if (node == null) {
      // if in repository, add
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
                                         poll.getUprBound());
        // if poll isn't running at it was our poll
        if (!pollManager.isPollRunning(poll.getType(), pollSpec)) {
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
        ( (NodeStateImpl) state).closeActivePoll(history);
      }
    }

    nodeCache.putState(cus.getUrl(), state);
    return state;
  }

  public AuState getAuState() {
    return auState;
  }

  Iterator getCacheEntries() {
    // this only returns nodes currently in the cache, but it's only used
    // by the UI so that's not a problem
    return nodeCache.snapshot().iterator();
  }

  public void forceTreeWalk() {
    logger.debug2("Forcing treewalk...");
    if (treeWalkHandler == null) {
      treeWalkHandler = new TreeWalkHandler(this, theDaemon);
      treeWalkHandler.start();
    }
    treeWalkHandler.forceTreeWalk();
  }

  public void killTreeWalk() {
    logger.debug2("Killing treewalk thread...");
    if (treeWalkHandler != null) {
      treeWalkHandler.end();
    }
  }

  public void forceTopLevelPoll() {
    logger.info("Forcing top level poll...");
    NodeState topNode = getNodeState(managedAu.getAUCachedUrlSet());
    Iterator activePolls = topNode.getActivePolls();
    if (!activePolls.hasNext()) {
      callTopLevelPoll();
    }
  }

  public boolean shouldStartPoll(CachedUrlSet cus, PollTally state) {
    NodeState nodeState = getNodeState(cus);

    if (nodeState == null) {
      logger.error("Failed to find a valid node state for: " + cus);
      return false;
    }

    if (!state.isMyPoll() && hasDamage(cus, state.getType())) {
      logger.info("CUS has damage, not starting poll: " + cus);
      return false;
    }

    return true;
  }

  public void startPoll(CachedUrlSet cus, PollTally tally,
                         boolean isReplayPoll) {
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
      logger.debug3("Prev node state: "+nodeState.getStateString());
      switch (nodeState.getState()) {
        case NodeState.INITIAL:
        case NodeState.OK:

          // these are polls started by another cache
          status = PollState.RUNNING;
          nodeState.setState((tally.getType() == Poll.CONTENT_POLL) ?
                             NodeState.CONTENT_RUNNING :
                             NodeState.NAME_RUNNING);
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
          status = PollState.REPAIRING;
          break;
        case NodeState.POSSIBLE_DAMAGE_HERE:
        case NodeState.UNREPAIRABLE_SNCUSS_NEEDS_POLL:
          nodeState.setState(NodeState.SNCUSS_POLL_RUNNING);
          status = PollState.RUNNING;
          break;
        case NodeState.NEEDS_REPAIR:
          nodeState.setState(NodeState.SNCUSS_POLL_REPLAYING);
          status = PollState.REPAIRING;
          break;
        default:
          logger.error("Invalid nodestate: " + nodeState.getStateString());
          status = PollState.RUNNING;
          if (tally.getType() == Poll.CONTENT_POLL) {
            nodeState.setState(NodeState.CONTENT_RUNNING);
          }
          else {
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
    logger.debug2("Starting poll for url '" +
                  nodeState.getCachedUrlSet().getUrl() + " " +
                  pollState.getLwrBound() + "-" +
                  pollState.getUprBound() + "'");
    logger.debug3("New node state: "+nodeState.getStateString());
  }

  public void updatePollResults(CachedUrlSet cus, PollTally results) {
    NodeState nodeState = (NodeState)activeNodes.get(results.getPollKey());
    if (nodeState==null) {
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
    NodeState topState = getNodeState(managedAu.getAUCachedUrlSet());
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
    }
    else {
      logger.debug3("Hash finished for CUS '" + cus.getUrl() + "'");
      ( (NodeStateImpl) state).setLastHashDuration(hashDuration);
    }
  }

  void updateState(NodeState nodeState, PollTally results) {
    logger.debug3("Prior node state: "+nodeState.getStateString());

    PollState pollState = getPollState(nodeState, results);
    if (pollState == null) {
      logger.error("Results updated for a non-existent poll.");
      throw new UnsupportedOperationException("Results updated for a "
                                              + "non-existent poll.");
    }

    boolean isRangedPoll =
        results.getCachedUrlSet().getSpec().isRangeRestricted();
    int lastState = -1;

    if (!isRangedPoll) {
      if (!checkValidStatesForResults(pollState, nodeState,
                                      results.getCachedUrlSet().getSpec())) {
        logger.error("Invalid node state for poll state: " +
                     nodeState.getStateString() + " for " +
                     pollState.getTypeString());
      }
    }

    try {
      boolean notFinished = false;
      switch (results.getStatus()) {
        case PollTally.STATE_ERROR:
          pollState.status = mapResultsErrorToPollError(results.getErr());
          logger.info("Poll didn't finish fully.  Error: "
                      + results.getErrString());
          notFinished = true;
          break;
        case PollTally.STATE_NOQUORUM:
          pollState.status = PollState.INCONCLUSIVE;
          logger.info("Poll finished without quorum");
          notFinished = true;
          break;
        case PollTally.STATE_RESULTS_TOO_CLOSE:
        case PollTally.STATE_RESULTS_UNTRUSTED:
          pollState.status = PollState.INCONCLUSIVE;
          logger.warning("Poll concluded with suspect result - "
                         + pollState.getStatusString());
          notFinished = true;
          break;
        case PollTally.STATE_LOST:
        case PollTally.STATE_WON:
          // set last state in case it changes
          lastState = nodeState.getState();
          if (results.getType() == Poll.CONTENT_POLL) {
            handleContentPoll(pollState, results, nodeState);
          } else if (results.getType() == Poll.NAME_POLL) {
            handleNamePoll(pollState, results, nodeState);
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
      }


      // if this is an AU top-level content poll
      // update the AuState to indicate the poll is finished if it has
      // the proper status
      if (isTopLevelPollFinished(nodeState.getCachedUrlSet().getSpec(),
                               pollState.getType(), pollState.getStatus())) {
        getAuState().newPollFinished();
        logger.info("Top level poll finished.");
      }

      // take next appropriate action on node if needed
      // only check state for ranged polls if they changed the state
      boolean checkState = !isRangedPoll ||
          ((lastState != -1) && (lastState != nodeState.getState()));
      if (checkState) {
        try {
          logger.debug3("New node state: " + nodeState.getStateString());
          checkCurrentState(pollState, results, nodeState, false);
        }
        catch (IOException ie) {
          logger.error("Unable to continue actions on node: ", ie);
          pollState.status = PollState.ERR_IO;
        }
      }
    }
    finally {
      if ((isRangedPoll) && (lastState != -1)) {
        // if it was a ranged poll, may have temporarily set state to
        // CONTENT_LOST, so restore last state
        nodeState.setState(lastState);
      }
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
  boolean handleWrongNames(Iterator masterIt, List localList, NodeState nodeState,
                           PollTally results) {
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
      markNodesForRepair(repairCol, results.getPollKey());
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
        //set crawl status to DELETED
        logger.debug3("marking node: " + url + " deleted.");
        NodeState extraState = getNodeState(extraCus);
        extraState.getCrawlState().type = CrawlState.NODE_DELETED;
      } catch (Exception e) {
        logger.error("Couldn't delete node.", e);
        // the treewalk will fix this eventually
      }
    }

    // return false if any repairs were scheduled
    return repairsDone;
  }

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

  void handleContentPoll(PollState pollState, PollTally results,
                         NodeState nodeState) {
    logger.debug("handling content poll results: " + results);

    // only update NodeState if not ranged poll
    boolean isRangedPoll =
        results.getCachedUrlSet().getSpec().isRangeRestricted();

    if (results.getStatus() == PollTally.STATE_WON) {
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
          pollState.status = PollState.REPAIRED;
      }

      if (!isRangedPoll) {
        // change node state accordingly
        switch (nodeState.getState()) {
          case NodeState.CONTENT_RUNNING:
          case NodeState.CONTENT_REPLAYING:
            nodeState.setState(NodeState.OK);
            if (damagedNodes.contains(nodeState.getCachedUrlSet().getUrl())) {
              // nodes are no longer damaged when a normal content poll succeeds
              logger.debug2("removing from damaged node list");
              damagedNodes.remove(nodeState.getCachedUrlSet().getUrl());
            }
            break;
          case NodeState.SNCUSS_POLL_RUNNING:
          case NodeState.SNCUSS_POLL_REPLAYING:
            if (results.isMyPoll()) {
              nodeState.setState(
                  NodeState.POSSIBLE_DAMAGE_BELOW);
            }
            else {
              // not mine, so done
              nodeState.setState(NodeState.OK);
            }
        }
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
        updateReputations(results);
      } else {
        // otherwise, schedule repair (if SNCUS) or name poll
        if (results.getCachedUrlSet().getSpec().isSingleNode()) {
          logger.debug2("lost single node content poll, marking for repair.");
          // if leaf node, we need to repair
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
            nodeState.setState(NodeState.CONTENT_LOST);
            // nodes are damaged when a normal content poll fails
            logger.debug2("adding to damaged node list");
            damagedNodes.add(nodeState.getCachedUrlSet().getUrl());
            break;
          case NodeState.CONTENT_REPLAYING:
            nodeState.setState(NodeState.DAMAGE_AT_OR_BELOW);
            break;
          case NodeState.SNCUSS_POLL_RUNNING:
            nodeState.setState(NodeState.NEEDS_REPAIR);
            break;
          case NodeState.SNCUSS_POLL_REPLAYING:
            nodeState.setState(NodeState.UNREPAIRABLE_SNCUSS);
            break;
        }
      } else {
        // if ranged, set temporary state
        nodeState.setState(NodeState.CONTENT_LOST);
      }
    }
  }

  void handleNamePoll(PollState pollState, PollTally results,
                      NodeState nodeState) {
    logger.debug2("handling name poll results " + results);

    // only update NodeState if not ranged poll
    boolean isRangedPoll =
        results.getCachedUrlSet().getSpec().isRangeRestricted();

    if (results.getStatus() == PollTally.STATE_WON) {
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
              nodeState.setState(NodeState.DAMAGE_AT_OR_BELOW);
            } else {
              nodeState.setState(NodeState.OK);
            }
            break;
          case NodeState.NAME_REPLAYING:
            //XXX should replay content poll, but set to NEEDS_POLL for now
            nodeState.setState(NodeState.NEEDS_REPLAY_POLL);
        }
      }
    } else {
      // if disagree

      // change poll state accordingly
      if (pollState.getStatus() == PollState.REPAIRING) {
        logger.debug2("lost repair name poll, state = unrepairable");
        // if repair poll, can't be repaired and we leave it in our damage table
        //XXX currently, this makes it difficult to do multiple repairs, since
        // the poll calls only one repair, and repeats only once.
        // the treewalk will have to handle the rest
        pollState.status = PollState.UNREPAIRABLE;
      } else {
        pollState.status = PollState.REPAIRING;
        logger.debug2("lost name poll, state = repairing");
      }

      if (!isRangedPoll) {
        // change node state accordingly
        switch (nodeState.getState()) {
          case NodeState.NAME_RUNNING:
            nodeState.setState(NodeState.WRONG_NAMES);
            break;
          case NodeState.NAME_REPLAYING:
            nodeState.setState(NodeState.UNREPAIRABLE_NAMES);
        }
      } else {
        // if ranged, set temporary state
        nodeState.setState(NodeState.WRONG_NAMES);
      }
    }
  }

  /**
   * Looks at the state of the node, and takes appropriate action (if
   * 'reportOnly' is false).
   * @param lastOrCurrentPoll the most recent poll (could be active)
   * @param results the {@link PollTally}, if available
   * @param nodeState the {@link NodeState}
   * @param reportOnly if true, nothing is actually scheduled
   * @return true if action taken
   * @throws IOException
   */
  boolean checkCurrentState(PollState lastOrCurrentPoll, PollTally results,
                            NodeState nodeState, boolean reportOnly)
      throws IOException {
    logger.debug2("Checking node: "+nodeState.getCachedUrlSet().getUrl());
    logger.debug2("State: "+nodeState.getStateString());
    switch (nodeState.getState()) {
      case NodeState.NEEDS_POLL:
      case NodeState.NEEDS_REPLAY_POLL:
        // call content poll
        if (!reportOnly) {
          logger.debug2("calling content poll");
          callContentPoll(new PollSpec(nodeState.getCachedUrlSet()));
        }
        return true;
      case NodeState.CONTENT_LOST:
        if (!reportOnly) {
          if ((results!=null) &&
              (results.getCachedUrlSet().getSpec().isRangeRestricted())) {
              // if ranged, we know the names are accurate from the previous
              // name poll, so continue as if it had won
              logger.debug2("sub-dividing under lost ranged content poll");
              callContentPollsOnSubNodes(nodeState, results.getCachedUrlSet());
          } else {
            // call name poll if not ranged or no results to use
            logger.debug2(
                "lost content poll, state = lost, calling name poll.");
            callNamePoll(new PollSpec(nodeState.getCachedUrlSet()));
          }
        }
        return true;
      case NodeState.UNREPAIRABLE_NAMES_NEEDS_POLL:
        // call name poll
        if (!reportOnly) {
          logger.debug2("calling name poll");
          callNamePoll(new PollSpec(nodeState.getCachedUrlSet()));
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
                                               lastHistory.uprBound);
          if ((lastHistory.isOurPoll())) {
            // if should recall and is our incomplete poll
            if (!pollManager.isPollRunning(lastHistory.getType(), lastPollSpec)) {
              // if this poll should be running make sure it is running.
              if (restartNonexpiredPolls ||
                  ((lastHistory.startTime + lastHistory.duration) <
                  TimeBase.nowMs())) {
                // only restart if it has expired, since other caches will still be
                // running it otherwise
                if (!reportOnly) {
                  logger.debug("Re-calling last unfinished poll.");
                  logger.debug2("lastHistory: " + lastHistory.getTypeString() +
                                ", " +
                                lastHistory.getStatusString() + ", " +
                                sdf.format(new Date(lastHistory.startTime)));
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
              } else {
                logger.debug2("Unfinished poll's duration not elapsed, so not"+
                              " recalling.");
              }
            } else {
              logger.debug2("Unfinished poll already running, so not"+
                            " recalling.");
            }
          } else {
            logger.debug2("Unfinished poll not mine, so not recalling.");
            nodeState.setState(NodeState.INITIAL);
          }
        }
        return false;
      case NodeState.WRONG_NAMES:
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
            callNamePoll(new PollSpec(nodeState.getCachedUrlSet()));
          }
        }
        return true;
      case NodeState.DAMAGE_AT_OR_BELOW:
        // subdivide or recurse, plus SNCUSS if not ranged
        if (!reportOnly) {
          logger.debug2("won name poll (mine), calling content poll on subnodes.");
          // call a content poll for this node's content if we haven't started
          // sub-dividing yet (and not an AU url, since they never have content)
          if ((lastOrCurrentPoll.getLwrBound() == null) &&
              (!nodeState.getCachedUrlSet().getSpec().isAU())) {
            logger.debug2("calling single node content poll on node's contents");
            //XXX once we're checking the content bit in the name poll,
            // only call SNCP if has content
            nodeState.setState(NodeState.POSSIBLE_DAMAGE_HERE);
            callSingleNodeContentPoll(nodeState.getCachedUrlSet());
          } else {
            // no damage here since ranged or top-level
            logger.debug2("setting to 'possible damage below'");
            nodeState.setState(NodeState.POSSIBLE_DAMAGE_BELOW);
          }
          // call a content poll on this node's subnodes
          CachedUrlSet cusToUse = (results!=null ? results.getCachedUrlSet() :
                                   nodeState.getCachedUrlSet());
          callContentPollsOnSubNodes(nodeState, cusToUse);
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
          //XXX make new poll ahead of time?
          String pollKey = (results==null ? null : results.getPollKey());
          markNodesForRepair(ListUtil.list(nodeState.getCachedUrlSet().getUrl()),
                             pollKey);
        }
        return true;
      case NodeState.UNREPAIRABLE_SNCUSS:
        //XXX check times, and changed to 'UNREPAIRABLE_XXX_NEEDS_POLL" if long enough
        // if long enough since first damage, change to 'UNKNOWN'
        nodeState.setState(NodeState.UNREPAIRABLE_SNCUSS_NEEDS_POLL);
        return false;
      case NodeState.UNREPAIRABLE_NAMES:
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
        // check time, switch to NEEDS_POLL if necessary and call toplevel poll
        if (nodeState.getCachedUrlSet().getSpec().isAU()) {
          // query the AU if a top level poll should be started
          if (managedAu.shouldCallTopLevelPoll(auState)) {
            if (!reportOnly) {
              nodeState.setState(NodeState.NEEDS_POLL);
              callTopLevelPoll();
              logger.debug("Requested top level poll...");
            }
            return true;
          }
        }
        return false;
    }
    return false;
  }

  boolean checkValidStatesForResults(PollState poll, NodeState node,
                           CachedUrlSetSpec spec) {
    boolean isContent = poll.getType()==Poll.CONTENT_POLL;
    boolean isName = poll.getType()==Poll.NAME_POLL;
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
                                         lastHistory.uprBound);
    boolean shouldRecallLastPoll =
        ((lastHistory.getStartTime()  + lastHistory.getDuration() + recallDelay)
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
        if (!pollManager.isPollRunning(lastHistory.getType(), lastPollSpec)) {
          // if should recall and is our incomplete poll
          if ((lastHistory.isOurPoll())) {
            // only restart if it has expired, since other caches will still be
            // running it otherwise
            if (restartNonexpiredPolls ||
                ((lastHistory.startTime + lastHistory.duration) <
                TimeBase.nowMs())) {
              if (!reportOnly) {
                logger.debug("Re-calling last unfinished poll.");
                logger.debug2("lastHistory: " + lastHistory.getTypeString() +
                              ", " +
                              lastHistory.getStatusString() + ", " +
                              sdf.format(new Date(lastHistory.startTime)));
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
                                                   null, null);
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
                                               prevHistory.uprBound);
          CachedUrlSet cus2 = prevPollSpec.getCachedUrlSet();
          if ((lockssRepo.cusCompare(cus1, cus2) ==
               LockssRepository.SAME_LEVEL_NO_OVERLAP)) {
            if (prevHistory.getStatus()==PollState.LOST) {
              // found the failed poll, rerunning
              if (!reportOnly) {
                logger.debug("Re-calling previous poll.");
                logger.debug2("lastHistory: "+lastHistory.getTypeString() +
                              ", " + lastHistory.getStatusString() + ", " +
                              sdf.format(new Date(lastHistory.startTime)));
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
/*
        // since we didn't find anything below this, recall the name poll
        if (!reportOnly) {
          logger.debug("Calling name poll for lost poll.");
          logger.debug2("lastHistory: "+lastHistory.getTypeString() + ", " +
                       lastHistory.getStatusString() + ", " +
                       sdf.format(new Date(lastHistory.startTime)));
          callNamePoll(lastPollSpec);
        }
        return true;
  */

        // since we didn't find anything below this, it's time to recall the
        // upper poll.  But 'checkCurrentState()' will take care of that
        return false;
      case PollState.UNREPAIRABLE:
        // TODO: this needs to be fixed eventually but for now we only get
        // our stuff from the publisher we're going to keep trying until we
        // fail.
        if (!reportOnly) {
          logger.debug("Re-calling poll on unrepairable node to trigger repair");
          logger.debug2("lastHistory: "+lastHistory.getTypeString() + ", " +
                       lastHistory.getStatusString() + ", " +
                       sdf.format(new Date(lastHistory.startTime)));
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
            logger.debug2("lastHistory: "+lastHistory.getTypeString() + ", " +
                         lastHistory.getStatusString() + ", " +
                         sdf.format(new Date(lastHistory.startTime)));
            callLastPoll(lastPollSpec, lastHistory);
          }
          return true;
        }
    }
    return false;
  }

  String findKey(PollHistory history) {
    Iterator votes = history.getVotes();
    if (votes.hasNext()) {
      Vote a_vote = (Vote) votes.next();
      return a_vote.getPollKey();
    }
    return "";
  }


  void callTopLevelPoll() {
    PollSpec spec = new PollSpec(managedAu.getAUCachedUrlSet());
    try {
      logger.debug2("Calling a top level poll on " + spec);
      pollManager.sendPollRequest(LcapMessage.CONTENT_POLL_REQ, spec);
    }
    catch (IOException ioe) {
      logger.error("Exception calling top level poll on " + spec, ioe);
    }
  }

  void closePoll(PollState pollState, long duration, Collection votes,
                         NodeState nodeState) {
    PollHistory history = new PollHistory(pollState, duration, votes);
    ( (NodeStateImpl) nodeState).closeActivePoll(history);
    logger.debug2("Closing poll for url '" +
                  nodeState.getCachedUrlSet().getUrl() + " " +
                  pollState.getLwrBound() + "-" +
                  pollState.getUprBound() + "'");
  }

  private PollState getPollState(NodeState state, PollTally results) {
    Iterator polls = state.getActivePolls();
    PollSpec spec = results.getPollSpec();
    logger.debug2("Getting poll state for spec: " + spec);
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

  static int mapResultsErrorToPollError(int resultsErr) {
    switch (resultsErr) {
      case BasePoll.ERR_HASHING:
        return PollState.ERR_HASHING;
      case BasePoll.ERR_IO:
        return PollState.ERR_IO;
      case BasePoll.ERR_SCHEDULE_HASH:
        return PollState.ERR_SCHEDULE_HASH;
    }
    return PollState.ERR_UNDEFINED;
  }

  private void markNodesForRepair(Collection urls, String pollKey) {
    if (pollKey!=null) {
      logger.debug2("suspending poll " + pollKey);
      pollManager.suspendPoll(pollKey);
    } else {
      logger.debug2("no poll found to suspend");
    }

    if (urls.size() == 1) {
      logger.debug2("scheduling repair");
    } else {
      logger.debug2("scheduling "+urls.size()+" repairs");
    }
    theDaemon.getCrawlManager().startRepair(managedAu, urls,
					    new ContentRepairCallback(),
					    pollKey);
  }

  private void deleteNode(CachedUrlSet cus) throws IOException {
    // delete the node from the LockssRepository
    LockssRepository repository = theDaemon.getLockssRepository(managedAu);
    repository.deleteNode(cus.getUrl());
  }

  private void deactivateNode(CachedUrlSet cus) throws IOException {
    // deactivate the node from the LockssRepository
    LockssRepository repository = theDaemon.getLockssRepository(managedAu);
    repository.deactivateNode(cus.getUrl());
  }

  private void callContentPollsOnSubNodes(NodeState state, CachedUrlSet cus)
      throws IOException {
    Iterator children = cus.flatSetIterator();
    List childList = convertChildrenToCUSList(children);
    // Divide the list in two and call two new content polls
    if (childList.size() > 4) {
      logger.debug2("more than 4 children, calling ranged content polls.");
      int mid = childList.size() / 2;

      // the first half of the list
      String lwr = ( (CachedUrlSet) childList.get(0)).getUrl();
      String upr = ( (CachedUrlSet) childList.get(mid)).getUrl();
      callContentPoll(cus, lwr, upr);

      // the second half of the list
      lwr = ( (CachedUrlSet) childList.get(mid + 1)).getUrl();
      upr = ( (CachedUrlSet) childList.get(childList.size() - 1)).getUrl();
      callContentPoll(cus, lwr, upr);
    }
    else if (childList.size() > 0) {
      logger.debug2("less than 4 children, calling content poll on each.");
      for (int i = 0; i < childList.size(); i++) {
        callContentPoll((CachedUrlSet) childList.get(i), null, null);
      }
    } else {
      logger.debug2("0 children, calling no content polls.");
    }
  }

  private void callContentPoll(CachedUrlSet cus, String lwr, String upr) throws
      IOException {
    String base = cus.getUrl();
    if(lwr != null) {
      lwr = lwr.startsWith(base) ? lwr.substring(base.length()) : lwr;
    }
    if(upr != null) {
      upr = upr.startsWith(base) ? upr.substring(base.length()) : upr;
    }
    CachedUrlSet newCus = cus.getArchivalUnit().makeCachedUrlSet(
        new RangeCachedUrlSetSpec(base, lwr, upr));
    PollSpec spec = new PollSpec(newCus, lwr, upr);
    logger.debug2("Calling a content poll on " + spec);
    pollManager.sendPollRequest(LcapMessage.CONTENT_POLL_REQ, spec);
  }

  private void callSingleNodeContentPoll(CachedUrlSet cus) throws IOException {
    // create a 'single node' CachedUrlSet
    CachedUrlSet newCus = cus.getArchivalUnit().makeCachedUrlSet(
        new SingleNodeCachedUrlSetSpec(cus.getUrl()));
    PollSpec spec = new PollSpec(newCus);
    logger.debug2("Calling a content poll on " + spec);
    pollManager.sendPollRequest(LcapMessage.CONTENT_POLL_REQ, spec);
  }


  private void callLastPoll(PollSpec spec, PollState lastPoll) {
    try {
      if (lastPoll.type == Poll.CONTENT_POLL) {
        logger.debug2("Calling a content poll on " + spec);
        pollManager.sendPollRequest(LcapMessage.CONTENT_POLL_REQ, spec);
      }
      else if (lastPoll.type == Poll.NAME_POLL) {
        logger.debug2("Calling a name poll on " + spec);
        pollManager.sendPollRequest(LcapMessage.NAME_POLL_REQ, spec);
      }
    }
    catch (IOException ioe) {
      logger.error("Exception calling poll on " + spec, ioe);
    }
  }


  private void callNamePoll(PollSpec spec) {
    try {
      logger.debug2("Calling a name poll on " + spec);
      pollManager.sendPollRequest(LcapMessage.NAME_POLL_REQ, spec);
    }
    catch (IOException ioe) {
      logger.error("Exception calling name poll on " + spec, ioe);
    }
  }

  private void callContentPoll(PollSpec spec) {
    try {
      logger.debug2("Calling a content poll on " + spec);
      pollManager.sendPollRequest(LcapMessage.CONTENT_POLL_REQ, spec);
    }
    catch (IOException ioe) {
      logger.error("Exception calling content poll on " + spec, ioe);
    }
  }

  private List convertChildrenToCUSList(Iterator children) {
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
            cus = ( (BaseArchivalUnit) managedAu).makeCachedUrlSet(rSpec);
        }
        childList.add(cus);
      }
    }
    return childList;
  }

  private boolean hasDamage(CachedUrlSet cus, int pollType) {
    boolean hasDamage = false;
    synchronized (damagedNodes) {
      if (damagedNodes.contains(cus.getUrl())) {
        return true;
      }
      Iterator damagedIt = damagedNodes.iterator();
      while (damagedIt.hasNext()) {
        String url = (String) damagedIt.next();
        if (cus.containsUrl(url)) {
          return true;
        }
      }
    }
    return hasDamage;
  }

  private void updateReputations(PollTally results) {
    IdentityManager idManager = theDaemon.getIdentityManager();
    Iterator voteIt = results.getPollVotes().iterator();
    int agreeChange;
    int disagreeChange;

    if (results.getStatus() == PollTally.STATE_WON) {
      agreeChange = IdentityManager.AGREE_VOTE;
      disagreeChange = IdentityManager.DISAGREE_VOTE;
    }
    else {
      agreeChange = IdentityManager.DISAGREE_VOTE;
      disagreeChange = IdentityManager.AGREE_VOTE;
    }
    while (voteIt.hasNext()) {
      Vote vote = (Vote) voteIt.next();
      int repChange = vote.isAgreeVote() ? agreeChange : disagreeChange;

      idManager.changeReputation(idManager.findIdentity(vote.getIDAddress()),
                                 repChange);
    }
  }

  private List createUrlListFromCusIterator(Iterator cusIt) {
    List list = new ArrayList();
    while (cusIt.hasNext()) {
      list.add(cusIt.next());
    }
    return list;
  }

  boolean isTopLevelPollFinished(CachedUrlSetSpec spec, int type, int status) {
    return (spec.isAU() && (type == Poll.CONTENT_POLL) &&
            ((status==PollState.WON) || (status==PollState.LOST)));
  }


  static class ContentRepairCallback
      implements CrawlManager.Callback {
    /**
     * @param success whether the repair was successful or not
     * @param cookie object used by callback to designate which repair
     * attempt this is
     */
    public void signalCrawlAttemptCompleted(boolean success, Object cookie) {
      logger.debug("Content crawl completed repair on " + cookie);
      if (cookie!=null) {
        pollManager.resumePoll(success, cookie);
      } else {
        //XXX recall SNCUSS poll
      }
    }
  }

  /**
   * Factory method to create new NodeManager instances.
   * @param au the {@link ArchivalUnit}
   * @return the new NodeManager instance
   */
  public static NodeManager createNewNodeManager(ArchivalUnit au) {
    return new NodeManagerImpl(au);
  }
}
