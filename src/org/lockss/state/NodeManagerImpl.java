/*
 * $Id: NodeManagerImpl.java,v 1.1 2002-12-17 23:35:14 aalto Exp $
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

/**
 */
public class NodeManagerImpl implements NodeManager {
  private static NodeManager nodeManager = null;
  private StateRepository repository;
  private HashMap auMaps;

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
    repository = new StateRepositoryImpl();
    loadStateTree();
  }

  public void updatePollResults(CachedUrlSet cus, Poll.VoteTally results) {
    NodeState state = getNodeState(cus);
    updateState(state, results);
    repository.storeNodeState(state);
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
    return 1000;
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
    NodeState state = repository.loadNodeState(cus);
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
    //XXX whee!!
  }

}
