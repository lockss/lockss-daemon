/*
 * $Id: NodeManagerImpl.java,v 1.4 2003-01-10 23:02:25 claire Exp $
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
import org.lockss.daemon.CachedUrlSet;
import org.lockss.poller.Poll;
import org.lockss.daemon.ArchivalUnit;
import org.lockss.util.Deadline;
import org.lockss.plugin.Plugin;
import org.lockss.util.Logger;
import org.lockss.poller.PollManager;
import org.lockss.protocol.LcapMessage;
import java.net.InetAddress;
import java.io.IOException;

/**
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

  private NodeManagerImpl() {
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
          if ((pollState.getStatus() & filter) == 1) {
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
    // traverse the tree
    // at each node, check for crawl state
    // schedule crawls if it's been too long
    // check with plugin for scheduling
    // convert finished polls to histories?
    // update poll states?

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
    // get the nodeState for this cus
    NodeState state = new NodeStateImpl(cus, new CrawlState(
        CrawlState.NEW_CONTENT_CRAWL, CrawlState.FINISHED, 0), new ArrayList(),
        repository);
    nodeMap.put(getCusKey(cus), state);
    // recurse the set's children
    Iterator children = cus.flatSetIterator();
    while (children.hasNext()) {
      CachedUrlSet child = (CachedUrlSet)children.next();
      recurseLoadCachedUrlSets(child, nodeMap);
    }
  }

  private String getAuKey(ArchivalUnit au) {
    return au.getPluginId() + ":" + au.getAUId();
  }

  private String getCusKey(CachedUrlSet cus) {
    return (String)cus.getSpec().getPrefixList().get(0);
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

  private void handleContentPoll(PollState pollState, Poll.VoteTally results, NodeState nodeState) {
    if (results.didWinPoll()) {
      // if agree
      if (pollState.getStatus() == PollState.RUNNING) {
        // if normal poll, we won!
        pollState.status = PollState.WON;
      } else if (pollState.getStatus() == PollState.REPAIRING) {
        // if repair poll, we're repaired
        pollState.status = PollState.REPAIRED;
      }
      //XXX update reputation
    } else {
      // if disagree
      if (pollState.getStatus() == PollState.REPAIRING) {
        // if repair poll, can't be repaired
        pollState.status = PollState.UNREPAIRABLE;
        //XXX update reputation
      } else if (isInternalNode(nodeState)) {
        // if internal node, we need to call a name poll
        pollState.status = PollState.LOST;
        //XXX call name poll
        long duration = 0; //XXX calculate
//        PollManager.getPollManager().makePollRequest(results, duration, LcapMessage.NAME_POLL_REQ);
      } else {
        // if leaf node, we need to repair
        pollState.status = PollState.REPAIRING;
        repairNode(nodeState);
        Deadline deadline = null; //XXX determine
        results.replayAllVotes(deadline);
      }
    }
  }

  private void handleNamePoll(PollState pollState, Poll.VoteTally results, NodeState nodeState) {
    if (results.didWinPoll()) {
      // if agree
      if (results.isMyPoll()) {
        // if poll is mine
        try {
          callContentPollOnSubNodes(nodeState, results);
          pollState.status = PollState.WON;
        } catch (IOException ioe) {
          logger.error("Error scheduling content polls.", ioe);
          pollState.status = PollState.ERR_IO;
        }
      } else {
        // if poll is not mine stop - set to WON
        pollState.status = PollState.WON;
      }
    } else {
      // if disagree
      if (results.isMyPoll()) {
        // if poll is mine
        pollState.status = PollState.REPAIRING;
        //XXX iterate through master list
        // compare against my list
        // if you're missing item - create and repair
        // if extra item - deletion
        pollState.status = PollState.REPAIRED;
      } else {
        // poll is not mine ????
        //XXX do something?
        pollState.status = PollState.LOST;
      }
    }
  }

  private PollState getPollState(NodeState state, Poll.VoteTally results) {
    Iterator polls = state.getActivePolls();
    while (polls.hasNext()) {
      PollState pollState = (PollState)polls.next();
      if ((pollState.getRegExp() == results.regExp) &&
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

  private int mapResultsErrorToPollError(int resultsErr) {
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

  private void repairNode(NodeState state) {
    //XXX move old version
    // fetch new version
  }

  private void callContentPollOnSubNodes(NodeState state,
      Poll.VoteTally results) throws IOException {
    Iterator children = state.getCachedUrlSet().flatSetIterator();
    while (children.hasNext()) {
      CachedUrlSet child = (CachedUrlSet)children.next();
      String url = (String)child.getSpec().getPrefixList().get(0);
      long duration = 0; //XXX calculate
      PollManager.getPollManager().makePollRequest(url, null,
          LcapMessage.CONTENT_POLL_REQ, duration);
    }
  }
}
