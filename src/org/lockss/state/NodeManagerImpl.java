/*
 * $Id: NodeManagerImpl.java,v 1.12 2003-01-25 02:22:20 aalto Exp $
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
import gnu.regexp.*;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.poller.*;
import org.lockss.plugin.Plugin;
import org.lockss.protocol.LcapMessage;
import org.lockss.protocol.IdentityManager;
import org.lockss.repository.LockssRepository;
import org.lockss.repository.LockssRepositoryImpl;

/**
 * Implementation of the NodeManager.
 */
public class NodeManagerImpl implements NodeManager {
  private static NodeManager nodeManager = null;
  static HistoryRepository repository =
      HistoryRepositoryImpl.getHistoryRepository();
  private static HashMap auMaps = new HashMap();
  private static HashMap auEstimateMap = new HashMap();

  private ArchivalUnit managerAu;
  private TreeMap nodeMap = new TreeMap();
  private static Logger logger = Logger.getLogger("NodeManager");

  /**
   * Factory method to retrieve NodeManager.
   * @param au the ArchivalUnit being managed
   * @return the current NodeManager
   */
  public synchronized static NodeManager getNodeManager(ArchivalUnit au) {
    nodeManager = (NodeManager)auMaps.get(au);
    if (nodeManager==null) {
      nodeManager = new NodeManagerImpl(au);
      auMaps.put(au, nodeManager);
    }
    return nodeManager;
  }

  NodeManagerImpl(ArchivalUnit au) {
    managerAu = au;
    loadStateTree();
  }

  public void updatePollResults(CachedUrlSet cus, Poll.VoteTally results) {
    NodeState state = getNodeState(cus);
    updateState(state, results);
    repository.storePollHistories(state);
  }

  public NodeState getNodeState(CachedUrlSet cus) {
    return (NodeState)nodeMap.get(cus.getIdString());
  }

  public Iterator getActiveCrawledNodes(CachedUrlSet cus) {
    Iterator keys = nodeMap.keySet().iterator();
    Vector stateV = new Vector();
    while (keys.hasNext()) {
      String key = (String)keys.next();
      if (cus.containsUrl(key)) {
        NodeState state = (NodeState)nodeMap.get(key);
        int status = state.getCrawlState().getStatus();
        if ((status != CrawlState.FINISHED) &&
            (status != CrawlState.NODE_DELETED)) {
          stateV.addElement(state);
        }
      }
    }
    return stateV.iterator();
  }

  public Iterator getFilteredPolledNodes(CachedUrlSet cus, int filter) {
    Iterator keys = nodeMap.keySet().iterator();
    Vector stateV = new Vector();
    while (keys.hasNext()) {
      String key = (String)keys.next();
      if (cus.containsUrl(key)) {
        NodeState state = (NodeState)nodeMap.get(key);
        Iterator polls = state.getActivePolls();
        while (polls.hasNext()) {
          PollState pollState = (PollState)polls.next();
          if ((pollState.getStatus() & filter) != 0) {
            stateV.addElement(state);
            break;
          }
        }
      }
    }
    return stateV.iterator();
  }

  public Iterator getNodeHistories(CachedUrlSet cus, int maxNumber) {
    Iterator keys = nodeMap.keySet().iterator();
    Vector historyV = new Vector();
    while (keys.hasNext()) {
      String key = (String)keys.next();
      if (cus.containsUrl(key)) {
        NodeState state = (NodeState)nodeMap.get(key);
        Iterator pollHistories = state.getPollHistories();
        while (pollHistories.hasNext()) {
          PollHistory history = (PollHistory)pollHistories.next();
          historyV.addElement(history);
          if (historyV.size() >= maxNumber) {
            return historyV.iterator();
          }
        }
      }
    }
    return historyV.iterator();
  }

  public Iterator getNodeHistoriesSince(CachedUrlSet cus, Deadline since) {
    Iterator keys = nodeMap.keySet().iterator();
    Vector historyV = new Vector();
    while (keys.hasNext()) {
      String key = (String)keys.next();
      if (cus.containsUrl(key)) {
        NodeState state = (NodeState)nodeMap.get(key);
        Iterator pollHistories = state.getPollHistories();
        while (pollHistories.hasNext()) {
          PollHistory history = (PollHistory)pollHistories.next();
          Deadline started = Deadline.at(history.getStartTime());
          if (!started.before(since)) {
            historyV.addElement(history);
          }
        }
      }
    }
    return historyV.iterator();
  }

  public long getEstimatedTreeWalkDuration() {
    Long estimateL = (Long)auEstimateMap.get(managerAu);
    if (estimateL==null) {
      // check size (node count) of tree
      int nodeCount = nodeMap.size();
      // estimate via short walk
      // this is not a fake walk; it functionally walks part of the tree
      long startTime = TimeBase.nowMs();
      Iterator nodesIt = nodeMap.entrySet().iterator();
      String deleteSub = null;
      int NUMBER_OF_NODES_TO_TEST = 200; //XXX fix
      //XXX do for set time?
      for (int ii=0; ii<NUMBER_OF_NODES_TO_TEST; ii++) {
        Map.Entry entry = (Map.Entry)nodesIt.next();
        deleteSub = walkEntry(entry, deleteSub);
      }
      long elapsedTime = TimeBase.nowMs() - startTime;

      // calculate
      double nodesPerMs = ((double)elapsedTime / NUMBER_OF_NODES_TO_TEST);
      estimateL = new Long((long)(nodeCount * nodesPerMs));

      auEstimateMap.put(managerAu, estimateL);
    }
    return estimateL.longValue();
  }

  private void doTreeWalk() {
    long startTime = TimeBase.nowMs();
    nodeTreeWalk(nodeMap);
    long elapsedTime = TimeBase.nowMs() - startTime;
    updateEstimate(elapsedTime);
  }

  private void updateEstimate(long elapsedTime) {
    if (auEstimateMap==null) {
      auEstimateMap = new HashMap();
    }
    Long estimateL = (Long)auEstimateMap.get(managerAu);
    if (estimateL==null) {
      auEstimateMap.put(managerAu, new Long(elapsedTime));
    } else {
      long newEstimate = (estimateL.longValue() + elapsedTime)/2;
      auEstimateMap.put(managerAu, new Long(newEstimate));
    }
  }

  private void nodeTreeWalk(TreeMap nodeMap) {
    Iterator nodesIt = nodeMap.entrySet().iterator();
    String deleteStr = null;
    while (nodesIt.hasNext()) {
      Map.Entry entry = (Map.Entry)nodesIt.next();
      // the deleteStr string is used to keep track of when the next
      // entry is a sub-node of a deleted one
      // -if it is, deleteStr stays the same and the entry is skipped
      // -if it isn't, but the entry itself is deleted, deleteStr = entry key
      // -otherwise, deleteStr is null
      deleteStr = walkEntry(entry, deleteStr);
    }
  }

  private String walkEntry(Map.Entry entry, String deleteStr) {
    String key = (String)entry.getKey();
    NodeState node = (NodeState)entry.getValue();

    // if it is in a directory under a deleted directory, skip it
    if ((deleteStr!=null) && (key.startsWith(deleteStr))) {
      //XXX mark deleted?
      // still under same 'deleteStr'
      return deleteStr;
    }
    // at each node, check for crawl state
    switch (node.getCrawlState().getType()) {
      case CrawlState.NODE_DELETED:
        if (!key.endsWith(File.separator)) {
          key += File.separator;
        }
        // the new 'deleteStr'
        return key;
      case CrawlState.BACKGROUND_CRAWL:
      case CrawlState.NEW_CONTENT_CRAWL:
      case CrawlState.REPAIR_CRAWL:

        //XXX schedule crawls if it's been too long
        // check with plugin for scheduling
    }
    // permanently store poll histories
    repository.storePollHistories(node);
    // null 'deleteStr'
    return null;
  }

  private void loadStateTree() {
    // get list of aus
    // recurse through au cachedurlsets
    CachedUrlSet cus = managerAu.getAUCachedUrlSet();
    recurseLoadCachedUrlSets(cus);
  }

  private void recurseLoadCachedUrlSets(CachedUrlSet cus) {
    // add the nodeState for this cus
    addNewNodeState(cus);
    // recurse the set's children
    Iterator children = cus.flatSetIterator();
    while (children.hasNext()) {
      CachedUrlSet child = (CachedUrlSet)children.next();
      recurseLoadCachedUrlSets(child);
    }
  }

  private void addNewNodeState(CachedUrlSet cus) {
    NodeState state = new NodeStateImpl(cus, new CrawlState(-1,
        CrawlState.FINISHED, 0), new ArrayList(), repository);
    nodeMap.put(cus.getIdString(), state);
  }

  private void updateState(NodeState state, Poll.VoteTally results) {
    PollState pollState = getPollState(state, results);
    if (pollState == null) {
      logger.error("Results updated for a non-existent poll.");
      throw new UnsupportedOperationException("Results updated for a non-existent poll.");
    }
    if (results.getErr() < 0) {
      pollState.status = mapResultsErrorToPollError(results.getErr());
      logger.info("Poll didn't finish fully.  Error code: " + pollState.status);
      return;
    }

    if (results.getType() == Poll.CONTENT_POLL) {
      handleContentPoll(pollState, results, state);
    } else if (results.getType() == Poll.NAME_POLL) {
      handleNamePoll(pollState, results, state);
    } else {
      logger.error("Updating state for invalid results type: "+results.getType());
      throw new UnsupportedOperationException("Updating state for invalid results type.");
    }
  }

  void handleContentPoll(PollState pollState, Poll.VoteTally results,
                                 NodeState nodeState) {
    if (results.didWinPoll()) {
      // if agree
      if (pollState.getStatus() == PollState.RUNNING) {
        // if normal poll, we won!
        pollState.status = PollState.WON;
      } else if (pollState.getStatus() == PollState.REPAIRING) {
        // if repair poll, we're repaired
        pollState.status = PollState.REPAIRED;
      }
      closePoll(pollState, results.getDuration(), results.getPollVotes(),
                nodeState);
      updateReputations(results);
    } else {
      // if disagree
      if (pollState.getStatus() == PollState.REPAIRING) {
        // if repair poll, can't be repaired
        pollState.status = PollState.UNREPAIRABLE;
        closePoll(pollState, results.getDuration(), results.getPollVotes(),
                  nodeState);
        updateReputations(results);
      } else if (nodeState.isInternalNode()) {
        // if internal node, we need to call a name poll
        pollState.status = PollState.LOST;
        closePoll(pollState, results.getDuration(), results.getPollVotes(),
                  nodeState);
        long duration = calculateNamePollDuration(nodeState.getCachedUrlSet());
        try {
          PollManager.getPollManager().makePollRequest(results.getUrl(),
              results.getRegExp(), LcapMessage.NAME_POLL_REQ, duration);
        } catch (IOException ioe) {
          logger.error("Couldn't make name poll request.", ioe);
          //XXX schedule something
        }
      } else {
        // if leaf node, we need to repair
        pollState.status = PollState.REPAIRING;
        try {
          markNodeForRepair(nodeState.getCachedUrlSet(), pollState);
          closePoll(pollState, results.getDuration(), results.getPollVotes(),
                    nodeState);
          //XXX PollManager.suspendPoll(pollState);
          // callback from fetch queue
          // PollManager.resumePoll(poll info);
          //  -Deadline deadline = Deadline.in(results.getDuration() * 2);
          //  -results.replayAllVotes(deadline);
        } catch (IOException ioe) {
          logger.error("Repair attempt failed.", ioe);
          //XXX schedule something?
        }
      }
    }
  }

  void handleNamePoll(PollState pollState, Poll.VoteTally results,
                              NodeState nodeState) {
    if (results.didWinPoll()) {
      // if agree
      if (results.isMyPoll()) {
        // if poll is mine
        try {
          callContentPollOnSubNodes(nodeState, results);
          pollState.status = PollState.WON;
        } catch (Exception e) {
          logger.error("Error scheduling content polls.", e);
          pollState.status = PollState.ERR_IO;
        }
      } else {
        // if poll is not mine stop - set to WON
        pollState.status = PollState.WON;
      }
      closePoll(pollState, results.getDuration(), results.getPollVotes(),
                nodeState);
    } else {
      // if disagree
      pollState.status = PollState.REPAIRING;
      Iterator masterIt = results.getCorrectEntries();
      Iterator localIt = results.getLocalEntries();
      Set localSet = createUrlSetFromCusIterator(localIt);
      ArchivalUnit au = nodeState.getCachedUrlSet().getArchivalUnit();
      // iterate through master list
      while (masterIt.hasNext()) {
        String url = (String)masterIt.next();
        // compare against my list
        if (localSet.contains(url)) {
          // removing from the set to leave only files for deletion
          localSet.remove(url);
        } else {
          // if not found locally, fetch
          try {
            CachedUrlSet newCus = au.makeCachedUrlSet(url, null);
            markNodeForRepair(newCus, pollState);
            //add to NodeState list
            addNewNodeState(newCus);
          } catch (Exception e) {
            logger.error("Couldn't fetch new node.", e);
            //XXX schedule something
          }
        }
      }
      localIt = localSet.iterator();
      while (localIt.hasNext()) {
        // for extra items - deletion
        String url = (String)localIt.next();
        try {
          CachedUrlSet oldCus = au.makeCachedUrlSet(url, null);
          deleteNode(oldCus);
          //set crawl status to DELETED
          NodeState oldState = getNodeState(oldCus);
          oldState.getCrawlState().type = CrawlState.NODE_DELETED;
        } catch (Exception e) {
          logger.error("Couldn't delete node.", e);
          //XXX schedule something
        }
      }
      pollState.status = PollState.REPAIRED;
      closePoll(pollState, results.getDuration(), results.getPollVotes(), nodeState);
    }
  }

  private void closePoll(PollState pollState, long duration, Collection votes,
                         NodeState nodeState) {
    //XXX checkpoint correctly
    PollHistory history = new PollHistory(pollState, duration, votes);
    ((NodeStateImpl)nodeState).closeActivePoll(history);
    repository.storePollHistories(nodeState);
  }

  private PollState getPollState(NodeState state, Poll.VoteTally results) {
    Iterator polls = state.getActivePolls();
    while (polls.hasNext()) {
      PollState pollState = (PollState)polls.next();
      if ((pollState.getRegExp() == results.getRegExp()) &&
          (pollState.getType() == results.getType())) {
        return pollState;
      }
    }
    return null;
  }

  static int mapResultsErrorToPollError(int resultsErr) {
    switch (resultsErr) {
      case Poll.ERR_HASHING:
        return PollState.ERR_HASHING;
      case Poll.ERR_IO:
        return PollState.ERR_IO;
      case Poll.ERR_NO_QUORUM:
        return PollState.ERR_NO_QUORUM;
      case Poll.ERR_SCHEDULE_HASH:
        return PollState.ERR_SCHEDULE_HASH;
    }
    return PollState.ERR_UNDEFINED;
  }

  private void markNodeForRepair(CachedUrlSet cus, PollState pollState)
      throws IOException {
    // fetch new version; automatically backs up old version
    //XXX fix to asynchronize
    // pass info to fetch queue instead
    cus.makeUrlCacher(cus.getPrimaryUrl()).cache();
  }

  private void deleteNode(CachedUrlSet cus) throws IOException {
    // delete the node from the LockssRepository
    //XXX change to get from RunDaemon
    LockssRepository repository = LockssRepositoryImpl.repositoryFactory(cus.getArchivalUnit());
    repository.deleteNode(cus.getPrimaryUrl());
  }

  private void callContentPollOnSubNodes(NodeState state,
      Poll.VoteTally results) throws IOException {
    Iterator children = state.getCachedUrlSet().flatSetIterator();
    while (children.hasNext()) {
      CachedUrlSet child = (CachedUrlSet)children.next();
      long duration = calculateContentPollDuration(child);
      PollManager.getPollManager().makePollRequest(child.getPrimaryUrl(), null,
          LcapMessage.CONTENT_POLL_REQ, duration);
    }
  }

  private long calculateContentPollDuration(CachedUrlSet cus) {
    //XXX implement!
    return 100000;
  }

  private long calculateNamePollDuration(CachedUrlSet cus) {
    //XXX implement!
    return 100000;
  }

  private void updateReputations(Poll.VoteTally results) {
    IdentityManager idManager = IdentityManager.getIdentityManager();
    Iterator voteIt = results.getPollVotes().iterator();
    while (voteIt.hasNext()) {
      Vote vote = (Vote)voteIt.next();
      int repChange = IdentityManager.AGREE_VOTE;
      if (!vote.isAgreeVote()) {
        repChange = IdentityManager.DISAGREE_VOTE;
      }
      idManager.changeReputation(vote.getIdentity(), repChange);
    }
  }

  private Set createUrlSetFromCusIterator(Iterator cusIt) {
    Set set = new HashSet();
    while (cusIt.hasNext()) {
      String key = (String) cusIt.next();
      set.add(key);
    }
    return set;
  }
}
