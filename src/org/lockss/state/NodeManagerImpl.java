/*
 * $Id: NodeManagerImpl.java,v 1.9 2003-01-23 01:27:00 aalto Exp $
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
import java.io.File;

/**
 * Implementation of the NodeManager.
 */
public class NodeManagerImpl implements NodeManager {
  private static NodeManager nodeManager = null;
  private HistoryRepository repository;
  private HashMap auMaps;
  private static Logger logger = Logger.getLogger("NodeManager");

  /**
   * Factory method to retrieve NodeManager.
   * @return the current NodeManager
   */
  public static NodeManager getNodeManager() {
    if (nodeManager==null) {
      nodeManager = new NodeManagerImpl();
    }
    return nodeManager;
  }

  NodeManagerImpl() {
    repository = HistoryRepositoryImpl.getHistoryRepository();
    loadStateTree();
  }

  public void updatePollResults(CachedUrlSet cus, Poll.VoteTally results) {
    NodeState state = getNodeState(cus);
    updateState(state, results);
    repository.storePollHistories(state);
  }

  public NodeState getNodeState(CachedUrlSet cus) {
    TreeMap auMap = (TreeMap)auMaps.get(getAuKey(cus.getArchivalUnit()));
    if (auMap==null) return null;
    return (NodeState)auMap.get(getCusKey(cus));
  }

  public Iterator getActiveCrawledNodes(CachedUrlSet cus) {
    TreeMap auMap = (TreeMap)auMaps.get(getAuKey(cus.getArchivalUnit()));
    Iterator keys = auMap.keySet().iterator();
    Vector stateV = new Vector();
    while (keys.hasNext()) {
      String key = (String)keys.next();
      if (cus.containsUrl(key)) {
        NodeState state = (NodeState)auMap.get(key);
        if (state.getCrawlState().getStatus() != CrawlState.FINISHED) {
          stateV.addElement(state);
        }
      }
    }
    return stateV.iterator();
  }

  public Iterator getFilteredPolledNodes(CachedUrlSet cus, int filter) {
    TreeMap auMap = (TreeMap)auMaps.get(getAuKey(cus.getArchivalUnit()));
    Iterator keys = auMap.keySet().iterator();
    Vector stateV = new Vector();
    while (keys.hasNext()) {
      String key = (String)keys.next();
      if (cus.containsUrl(key)) {
        NodeState state = (NodeState)auMap.get(key);
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
    TreeMap auMap = (TreeMap)auMaps.get(getAuKey(cus.getArchivalUnit()));
    Iterator keys = auMap.keySet().iterator();
    Vector historyV = new Vector();
    while (keys.hasNext()) {
      String key = (String)keys.next();
      if (cus.containsUrl(key)) {
        NodeState state = (NodeState)auMap.get(key);
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
    TreeMap auMap = (TreeMap)auMaps.get(getAuKey(cus.getArchivalUnit()));
    Iterator keys = auMap.keySet().iterator();
    Vector historyV = new Vector();
    while (keys.hasNext()) {
      String key = (String)keys.next();
      if (cus.containsUrl(key)) {
        NodeState state = (NodeState)auMap.get(key);
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

  public long getEstimatedTreeWalkDuration(ArchivalUnit au) {
    // check size (node count) of tree
    // estimate via short walk
    // multiply
    return 1000;
  }

  private void doTreeWalk() {
    Iterator nodeMapIt = auMaps.entrySet().iterator();
    while (nodeMapIt.hasNext()) {
      TreeMap nodeMap = (TreeMap)nodeMapIt.next();
      nodeTreeWalk(nodeMap);
    }
  }

  private void nodeTreeWalk(TreeMap nodeMap) {
    Iterator nodesIt = nodeMap.entrySet().iterator();
    String deleteSub = null;
    while (nodesIt.hasNext()) {
      Map.Entry entry = (Map.Entry)nodesIt.next();
      String key = (String)entry.getKey();
      NodeState node = (NodeState)entry.getValue();

      // if it is in a directory under a deleted directory, skip it
      if ((deleteSub!=null) && (key.startsWith(deleteSub))) {
        //XXX mark deleted?
        continue;
      } else {
        deleteSub = null;
      }
      // at each node, check for crawl state
      switch (node.getCrawlState().getType()) {
        case CrawlState.NODE_DELETED:
          deleteSub = key;
          if (!deleteSub.endsWith(File.separator)) {
            deleteSub += File.separator;
          }
          continue;
        case CrawlState.BACKGROUND_CRAWL:
        case CrawlState.NEW_CONTENT_CRAWL:
        case CrawlState.REPAIR_CRAWL:

          //XXX schedule crawls if it's been too long
          // check with plugin for scheduling
      }
      repository.storePollHistories(node);
      Iterator pollsIt = node.getActivePolls();
      while (pollsIt.hasNext()) {
        PollState poll = (PollState)pollsIt.next();
        if ((poll.getStatus()==PollState.LOST) ||
            (poll.getStatus()==PollState.REPAIRED) ||
            (poll.getStatus()==PollState.UNREPAIRABLE) ||
            (poll.getStatus()==PollState.WON)) {

          //XXX update poll states?
        }
      }
    }
  }

  private void loadStateTree() {
    // get list of aus
    auMaps = new HashMap(Plugin.getNumArchivalUnits());
    Iterator auIter = Plugin.getArchivalUnits();
    while (auIter.hasNext()) {
      ArchivalUnit au = (ArchivalUnit)auIter.next();
      TreeMap nodeMap = new TreeMap();
      // recurse through au cachedurlsets
      CachedUrlSet cus = au.getAUCachedUrlSet();
      recurseLoadCachedUrlSets(cus, nodeMap);
      auMaps.put(getAuKey(au), nodeMap);
    }
  }

  private void recurseLoadCachedUrlSets(CachedUrlSet cus, TreeMap nodeMap) {
    // add the nodeState for this cus
    addNewNodeState(cus, nodeMap);
    // recurse the set's children
    Iterator children = cus.flatSetIterator();
    while (children.hasNext()) {
      CachedUrlSet child = (CachedUrlSet)children.next();
      recurseLoadCachedUrlSets(child, nodeMap);
    }
  }

  private void addNewNodeState(CachedUrlSet cus, TreeMap nodeMap) {
    NodeState state = new NodeStateImpl(cus, new CrawlState(-1,
        CrawlState.FINISHED, 0), new ArrayList(), repository);
    nodeMap.put(getCusKey(cus), state);
  }

  private void addNewNodeState(CachedUrlSet cus) {
    TreeMap nodeMap = (TreeMap)auMaps.get(getAuKey(cus.getArchivalUnit()));
    addNewNodeState(cus, nodeMap);
  }

  static String getAuKey(ArchivalUnit au) {
    return au.getPluginId() + ":" + au.getAUId();
  }

  static String getCusKey(CachedUrlSet cus) {
    String key = (String)cus.getSpec().getPrefixList().get(0);
    if (key.endsWith(File.separator)) {
      key = key.substring(0, key.length()-1);
    }
    return key;
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

    if (results.type == Poll.CONTENT_POLL) {
      handleContentPoll(pollState, results, state);
    } else if (results.type == Poll.NAME_POLL) {
      handleNamePoll(pollState, results, state);
    } else {
      logger.error("Updating state for invalid results type: "+results.type);
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
      closePoll(pollState, results.duration, results.pollVotes, nodeState);
      updateReputations(results);
    } else {
      // if disagree
      if (pollState.getStatus() == PollState.REPAIRING) {
        // if repair poll, can't be repaired
        pollState.status = PollState.UNREPAIRABLE;
        closePoll(pollState, results.duration, results.pollVotes, nodeState);
        updateReputations(results);
      } else if (isInternalNode(nodeState)) {
        // if internal node, we need to call a name poll
        pollState.status = PollState.LOST;
        closePoll(pollState, results.duration, results.pollVotes, nodeState);
        long duration = calculateDuration(nodeState.getCachedUrlSet(), false);
        try {
          PollManager.getPollManager().makePollRequest(results.getUrl(),
              results.getRegExp(), LcapMessage.NAME_POLL_REQ, duration);
        } catch (IOException ioe) {
          logger.error("Couldn't make name poll request.", ioe);
          //XXX throw something
        }
      } else {
        // if leaf node, we need to repair
        pollState.status = PollState.REPAIRING;
        try {
          repairNode(nodeState.getCachedUrlSet());
          Deadline deadline = Deadline.in(results.duration * 2);
          results.replayAllVotes(deadline);
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
      closePoll(pollState, results.duration, results.pollVotes, nodeState);
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
            repairNode(newCus);
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
      closePoll(pollState, results.duration, results.pollVotes, nodeState);
    }
  }

  private void closePoll(PollState pollState, long duration, Collection votes,
                         NodeState nodeState) {
    PollHistory history = new PollHistory(pollState, duration, votes);
    ((NodeStateImpl)nodeState).closeActivePoll(history);
  }

  private PollState getPollState(NodeState state, Poll.VoteTally results) {
    Iterator polls = state.getActivePolls();
    while (polls.hasNext()) {
      PollState pollState = (PollState)polls.next();
      if ((pollState.getRegExp() == results.getRegExp()) &&
          (pollState.getType() == results.type)) {
        return pollState;
      }
    }
    return null;
  }

  private boolean isInternalNode(NodeState state) {
    Iterator children = state.getCachedUrlSet().flatSetIterator();
    return children.hasNext();
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

  private void repairNode(CachedUrlSet cus) throws IOException {
    // fetch new version; automatically backs up old version
    cus.makeUrlCacher(getCusKey(cus)).cache();
  }

  private void deleteNode(CachedUrlSet cus) throws IOException {
    // delete the node from the LockssRepository
    String url = getCusKey(cus);
    //XXX change to get from RunDaemon
    LockssRepository repository = LockssRepositoryImpl.repositoryFactory(cus.getArchivalUnit());
    repository.deleteNode(url);
  }

  private void callContentPollOnSubNodes(NodeState state,
      Poll.VoteTally results) throws IOException {
    Iterator children = state.getCachedUrlSet().flatSetIterator();
    while (children.hasNext()) {
      CachedUrlSet child = (CachedUrlSet)children.next();
      String url = getCusKey(child);
      long duration = calculateDuration(child, true);
      PollManager.getPollManager().makePollRequest(url, null,
          LcapMessage.CONTENT_POLL_REQ, duration);
    }
  }

  private long calculateDuration(CachedUrlSet cus, boolean isContentPoll) {
    //XXX implement!
    return 100000;
  }

  private void updateReputations(Poll.VoteTally results) {
    IdentityManager idManager = IdentityManager.getIdentityManager();
    Iterator voteIt = results.pollVotes.iterator();
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
