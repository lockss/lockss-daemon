/*
 * $Id: NodeManagerImpl.java,v 1.74 2003-04-01 00:08:12 aalto Exp $
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

import java.net.*;
import java.util.*;
import java.io.File;
import java.io.IOException;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.poller.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.crawler.CrawlManager;
import org.lockss.protocol.LcapMessage;
import org.lockss.protocol.IdentityManager;
import org.lockss.repository.LockssRepository;
import gnu.regexp.*;
import org.apache.commons.collections.LRUMap;
import org.lockss.daemon.status.*;

/**
 * Implementation of the NodeManager.
 */
public class NodeManagerImpl implements NodeManager {
  /**
   * This parameter indicates the size of the {@link NodeStateMap} used by the
   * node manager.
   */
  public static final String PARAM_NODESTATE_MAP_SIZE =
      Configuration.PREFIX + "state.nodemap.size";
  static final int DEFAULT_MAP_SIZE = 100;

  private static LockssDaemon theDaemon;
  private NodeManager theManager = null;
  static HistoryRepository historyRepo;
  private LockssRepository lockssRepo;
  PollManager pollManager;

  ArchivalUnit managedAu;
  AuState auState;
  NodeStateCache nodeCache;
  int maxMapSize;
  private static Logger logger = Logger.getLogger("NodeManager");
  TreeWalkHandler treeWalkHandler;

  Configuration.Callback configCallback;

  NodeManagerImpl(ArchivalUnit au) {
    managedAu = au;
  }

  /**
   * init the plugin manager.
   * @param daemon the LockssDaemon instance
   * @throws LockssDaemonException if we already instantiated this manager
   * @see org.lockss.app.LockssManager#initService(LockssDaemon daemon)
   */
  public void initService(LockssDaemon daemon) throws LockssDaemonException {
    logger.debug("NodeManager being inited");
    if (theManager == null) {
      theDaemon = daemon;
      theManager = this;
    } else {
      throw new LockssDaemonException("Multiple Instantiation.");
    }
    logger.debug("NodeManager sucessfully initied");
  }

  /**
   * start the plugin manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    logger.debug("NodeManager being started");

    configCallback = new Configuration.Callback() {
        public void configurationChanged(Configuration newConfig,
                                         Configuration oldConfig,
                                         Set changedKeys) {
          setConfig(newConfig, oldConfig);
        }
      };
    Configuration.registerConfigurationCallback(configCallback);

    historyRepo = theDaemon.getHistoryRepository();
    lockssRepo = theDaemon.getLockssRepository(managedAu);
    pollManager = theDaemon.getPollManager();

    nodeCache = new NodeStateCache(historyRepo, maxMapSize);

    auState = historyRepo.loadAuState(managedAu);

    treeWalkHandler = new TreeWalkHandler(this, theDaemon.getCrawlManager());
    treeWalkHandler.start();
    logger.debug("NodeManager sucessfully started");
  }

  /**
   * stop the plugin manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    logger.debug("NodeManager being stopped");
    // checkpoint here
    if (treeWalkHandler!=null) {
      treeWalkHandler.end();
      treeWalkHandler = null;
    }
    Configuration.unregisterConfigurationCallback(configCallback);
    theManager = null;
    logger.debug("NodeManager sucessfully stopped");
  }

  private void setConfig(Configuration config, Configuration oldConfig) {
    maxMapSize = config.getInt(PARAM_NODESTATE_MAP_SIZE, DEFAULT_MAP_SIZE);
  }

  public void startPoll(CachedUrlSet cus, PollTally state) {
    NodeState nodeState = getNodeState(cus);
    PollSpec spec = state.getPollSpec();
    PollState pollState = new PollState(state.getType(), spec.getLwrBound(),
                                        spec.getUprBound(),
                                        PollState.RUNNING, state.getStartTime(),
                                        null);
    ((NodeStateImpl)nodeState).addPollState(pollState);
  }

  public boolean shouldStartPoll(CachedUrlSet cus, PollTally state) {
    NodeState nodeState = getNodeState(cus);

    if (nodeState == null) {
      logger.error("Failed to find a valid node state for: " + cus);
      return false;
    }

    if(!state.isMyPoll() && hasDamage(cus, state.getType()))  {
      logger.info("Poll has damaged node: " + cus);
      return false;
    }

    return true;
  }

  public void updatePollResults(CachedUrlSet cus, PollTally results) {
    NodeState state = getNodeState(cus);
    updateState(state, results);
  }

  public void newContentCrawlFinished() {
    getAuState().newCrawlFinished();
    historyRepo.storeAuState(getAuState());
  }

  public synchronized NodeState getNodeState(CachedUrlSet cus) {
    String url = cus.getUrl();
    logger.debug3("Getting " + url);
    NodeState node = nodeCache.get(url);
    if (node==null) {
      // if in repository, add
      try {
        if (lockssRepo.getNode(url) != null) {
          node = createNodeState(cus);
        }
      } catch (MalformedURLException mue) {
        logger.error("Can't get NodeState due to bad CachedUrlSet: "+cus);
      }
    }
    return node;
  }

  NodeState createNodeState(CachedUrlSet cus) {
    logger.debug2("Loading NodeState: " + cus.toString());
    // load from file cache, or get a new one
    NodeState state = historyRepo.loadNodeState(cus);
    nodeCache.put(cus.getUrl(), state);
    return state;
  }


  public AuState getAuState() {
    return auState;
  }

  public Iterator getActiveCrawledNodes(CachedUrlSet cus) {
    //XXX this only returns nodes currently in the LRUMap
    //XXX use snapshot method
    Iterator entries = nodeCache.lruMap.entrySet().iterator();
    Vector stateV = new Vector();
    while (entries.hasNext()) {
      Map.Entry entry = (Map.Entry)entries.next();
      String key = (String)entry.getKey();
      if (cus.containsUrl(key)) {
        NodeState state = (NodeState)entry.getValue();
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
    //XXX this only returns nodes currently in the LRUMap
    Iterator entries = nodeCache.lruMap.entrySet().iterator();
    Vector stateV = new Vector();
    while (entries.hasNext()) {
      Map.Entry entry = (Map.Entry)entries.next();
      String key = (String)entry.getKey();
      if (cus.containsUrl(key)) {
        NodeState state = (NodeState)entry.getValue();
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
    //XXX this only returns nodes currently in the LRUMap
    Iterator entries = nodeCache.lruMap.entrySet().iterator();
    Vector historyV = new Vector();
    while (entries.hasNext()) {
      Map.Entry entry = (Map.Entry)entries.next();
      String key = (String)entry.getKey();
      if (cus.containsUrl(key)) {
        NodeState state = (NodeState)entry.getValue();
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
    //XXX this only returns nodes currently in the LRUMap
    Iterator entries = nodeCache.lruMap.entrySet().iterator();
    Vector historyV = new Vector();
    while (entries.hasNext()) {
      Map.Entry entry = (Map.Entry)entries.next();
      String key = (String)entry.getKey();
      if (cus.containsUrl(key)) {
        NodeState state = (NodeState)entry.getValue();
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

  void updateState(NodeState state, PollTally results) {
    PollState pollState = getPollState(state, results);
    if (pollState == null) {
      logger.error("Results updated for a non-existent poll.");
      throw new UnsupportedOperationException("Results updated for a "
                                              +"non-existent poll.");
    }
    try {
      if (results.getErr() < 0) {
        pollState.status = mapResultsErrorToPollError(results.getErr());
        logger.info("Poll didn't finish fully.  Error code: "
                    + pollState.status);
        return;
      }

      if (results.getType() == Poll.CONTENT_POLL) {
        handleContentPoll(pollState, results, state);
      }
      else if (results.getType() == Poll.NAME_POLL) {
        handleNamePoll(pollState, results, state);
      }
      else {
        logger.error("Updating state for invalid results type: " +
                     results.getType());
        throw new UnsupportedOperationException("Updating state for invalid "
                                                + "results type.");
      }
    } finally {
      // close the poll and update the node state
      closePoll(pollState, results.getDuration(), results.getPollVotes(),
                state);
    }
  }

  void handleContentPoll(PollState pollState, PollTally results,
                                 NodeState nodeState) {
    logger.debug("handling content poll results: " + results);
    if (results.didWinPoll()) {
      // if agree
      switch (pollState.getStatus()) {
        case PollState.RUNNING:
          // if normal poll, we won!
          logger.debug2("won content poll, state = won.");
          pollState.status = PollState.WON;
          break;
        case PollState.REPAIRING:
          // if repair poll, we're repaired
          logger.debug2("won repair poll, state = repaired.");
          pollState.status = PollState.REPAIRED;
      }
      updateReputations(results);
    } else {
      // if disagree
      if (pollState.getStatus() == PollState.REPAIRING) {
        logger.debug2("lost repair content poll, state = unrepairable");
        // if repair poll, can't be repaired
        pollState.status = PollState.UNREPAIRABLE;
        updateReputations(results);
      } else if (nodeState.isInternalNode()) {
        logger.debug2("lost content poll, state = lost, calling name poll.");
        // if internal node, we need to call a name poll
        pollState.status = PollState.LOST;
        callNamePoll(results.getCachedUrlSet());
      } else {
        logger.debug2("lost content poll, state = repairing, marking for repair.");
        // if leaf node, we need to repair
        pollState.status = PollState.REPAIRING;
        markNodeForRepair(nodeState.getCachedUrlSet(), results);
      }
    }
  }

  void handleNamePoll(PollState pollState, PollTally results,
                              NodeState nodeState) {
    logger.debug2("handling name poll results " + results);
    if (results.didWinPoll()) {
      // if agree
      if (results.isMyPoll()) {
        // if poll is mine
        try {
          logger.debug2("won name poll, calling content poll on subnodes.");
          callContentPollsOnSubNodes(nodeState, results);
          pollState.status = PollState.WON;
        } catch (IOException e) {
          logger.error("Error scheduling content polls.", e);
          pollState.status = PollState.ERR_IO;
        }
      } else {
        logger.debug2("won name poll, setting state to WON");
        pollState.status = PollState.WON;
      }
    } else {
      // if disagree
      logger.debug2("lost name poll, collecting repair info.");
      String baseUrl = nodeState.getCachedUrlSet().getUrl();
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
        }
        else {
          // if not found locally, fetch
          logger.debug2("marking missing node for repair: " + url);
          CachedUrlSet newCus = au.makeCachedUrlSet(baseUrl + url, null, null);
          markNodeForRepair(newCus, results);
        }
      }
      localIt = localSet.iterator();
      while (localIt.hasNext()) {
        // for extra items - deletion
        String url = (String)localIt.next();
        logger.debug2("deleting node: " + url);
        try {
          CachedUrlSet oldCus = au.makeCachedUrlSet(baseUrl + url, null, null);
          deleteNode(oldCus);
          //set crawl status to DELETED
          NodeState oldState = getNodeState(oldCus);
          oldState.getCrawlState().type = CrawlState.NODE_DELETED;
        } catch (Exception e) {
          logger.error("Couldn't delete node.", e);
          // the treewalk will fix this eventually
        }
      }
      pollState.status = PollState.REPAIRED;
    }
  }

  /**
   * Looks at the last history from a node (during a treewalk), and schedules
   * new polls if necessary.
   * @param lastHistory the {@link PollHistory}
   * @param node the {@link NodeState}
   * @return true if poll scheduled
   */
  boolean checkLastHistory(PollHistory lastHistory, NodeState node) {
    switch (lastHistory.status) {
      // if latest is PollState.LOST or PollState.REPAIRING
      // call a name poll to finish the repair which ended early
      case PollState.LOST:
      case PollState.REPAIRING:
        PollSpec spec = new PollSpec(node.getCachedUrlSet(),
                                     lastHistory.lwrBound,
                                     lastHistory.uprBound);
        callNamePoll(spec.getCachedUrlSet());
        return true;
      case PollState.UNREPAIRABLE:
        //XXX determine what to do
        return false;
      default:
        return false;
    }
  }

  void callNamePoll(CachedUrlSet cus) {
    try {
      pollManager.requestPoll(LcapMessage.NAME_POLL_REQ,
                                             new PollSpec(cus));
    } catch (IOException ioe) {
      logger.error("Couldn't make name poll request.", ioe);
      // the treewalk will fix this eventually
    }
  }

  void callTopLevelPoll() {
    try {
      pollManager.requestPoll(LcapMessage.CONTENT_POLL_REQ,
      new PollSpec(managedAu.getAUCachedUrlSet()));
      logger.info("Top level poll started.");
    } catch (IOException ioe) {
      logger.error("Couldn't make top level poll request.", ioe);
      // the treewalk will fix this eventually
    }
  }

  private void closePoll(PollState pollState, long duration, Collection votes,
                         NodeState nodeState) {
    PollHistory history = new PollHistory(pollState, duration, votes);
    ((NodeStateImpl)nodeState).closeActivePoll(history);
    logger.debug2("Closing poll for url '" +
                  nodeState.getCachedUrlSet().getUrl() + " " +
                  pollState.getLwrBound() + "-" + pollState.getUprBound() + "'");
    // if this is an AU top-level content poll
    // update the AuState to indicate the poll is finished
    if ((AuUrl.isAuUrl(nodeState.getCachedUrlSet().getUrl())) &&
        (pollState.getType() == Poll.CONTENT_POLL)) {
      getAuState().newPollFinished();
      historyRepo.storeAuState(getAuState());
      logger.info("Top level poll finished.");
    }
  }

  private PollState getPollState(NodeState state, PollTally results) {
    Iterator polls = state.getActivePolls();
    while (polls.hasNext()) {
      PollState pollState = (PollState)polls.next();
      PollSpec spec = results.getPollSpec();
      logger.debug2("Getting poll state for spec: " + spec);
      if (StringUtil.equalStrings(pollState.getLwrBound(),spec.getLwrBound()) &&
          StringUtil.equalStrings(pollState.getUprBound(),spec.getUprBound()) &&
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

  private void markNodeForRepair(CachedUrlSet cus, PollTally tally) {
    logger.debug2("suspending poll " + tally.getPollKey());
    pollManager.suspendPoll(tally.getPollKey());
    logger.debug2("scheduling repair");
    try {
      theDaemon.getCrawlManager().scheduleRepair(managedAu,
                                                 new URL(cus.getUrl()),
                                                 new ContentRepairCallback(),
                                                 tally.getPollKey());
    } catch (MalformedURLException mue) {
      // this shouldn't happen
      // if it does, let the tree walk catch the repair
    }
  }

  private void deleteNode(CachedUrlSet cus) throws IOException {
    // delete the node from the LockssRepository
    LockssRepository repository = theDaemon.getLockssRepository(managedAu);
    repository.deleteNode(cus.getUrl());
  }

  private void callContentPollsOnSubNodes(NodeState state, PollTally results)
      throws IOException {

    Iterator children = results.getCachedUrlSet().flatSetIterator();
    List childList = convertChildrenToCUSList(children);
    // Divide the list in two and call two new content polls
    if (childList.size() > 4) {
      String base = results.getCachedUrlSet().getUrl();
      int mid = childList.size() / 2;
      String lwr = ( (CachedUrlSet) childList.get(0)).getUrl();
      lwr = lwr.startsWith(base) ? lwr.substring(base.length()) : lwr;
      String upr = ( (CachedUrlSet) childList.get(mid)).getUrl();
      upr = upr.startsWith(base) ? upr.substring(base.length()) : upr;
      PollSpec pspec = new PollSpec(results.getCachedUrlSet(), lwr, upr);
      logger.debug2("calling first content poll on " + pspec);
      pollManager.requestPoll(LcapMessage.CONTENT_POLL_REQ,
                                             pspec);

      lwr = ( (CachedUrlSet) childList.get(mid + 1)).getUrl();
      lwr = lwr.startsWith(base) ? lwr.substring(base.length()) : lwr;
      upr = ( (CachedUrlSet) childList.get(childList.size() - 1)).
          getUrl();
      upr = upr.startsWith(base) ? upr.substring(base.length()) : upr;
      pspec = new PollSpec(results.getCachedUrlSet(), lwr, upr);
      logger.debug2("calling second content poll on " + pspec);
      pollManager.requestPoll(LcapMessage.CONTENT_POLL_REQ,
                                             pspec);
    }
    else if (childList.size() > 0) {
      logger.debug2("less than 4 items, calling content poll on all items.");
      for (int i = 0; i < childList.size(); i++) {
        PollSpec pspec = new PollSpec( (CachedUrlSet) childList.get(i));
        logger.debug2("calling content poll on " + pspec);
        pollManager.requestPoll(LcapMessage.CONTENT_POLL_REQ,
                                               pspec);
      }
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
    Iterator it = null;
    switch (pollType) {
      case Poll.CONTENT_POLL:
        it = cus.treeIterator();
        break;
      case Poll.NAME_POLL:
        it = cus.flatSetIterator();
        break;
    }
    List childList = convertChildrenToCUSList(it);
    Iterator childIt = childList.iterator();
    while (childIt.hasNext()) {
      CachedUrlSet child_cus = (CachedUrlSet) childIt.next();
      NodeState nodeState = getNodeState(child_cus);
      PollHistory pollHistory = nodeState.getLastPollHistory();
      if (pollHistory != null &&
        ((pollHistory.status == pollHistory.UNREPAIRABLE) ||
         (pollHistory.status == pollHistory.REPAIRING))) {
          return true;
      }
    }
    return hasDamage;
  }

  private void updateReputations(PollTally results) {
    IdentityManager idManager = theDaemon.getIdentityManager();
    Iterator voteIt = results.getPollVotes().iterator();
    int agreeChange;
    int disagreeChange;

    if(results.didWinPoll()) {
      agreeChange = IdentityManager.AGREE_VOTE;
      disagreeChange = IdentityManager.DISAGREE_VOTE;
    }
    else {
      agreeChange = IdentityManager.DISAGREE_VOTE;
      disagreeChange = IdentityManager.AGREE_VOTE;
    }
    while (voteIt.hasNext()) {
      Vote vote = (Vote)voteIt.next();
      int repChange = vote.isAgreeVote() ? agreeChange : disagreeChange;

      idManager.changeReputation(idManager.findIdentity(vote.getIDAddress()),
                                 repChange);
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

  static class ContentRepairCallback implements CrawlManager.Callback {
    /**
     * @param success whether the repair was successful or not
     * @param cookie object used by callback to designate which repair
     * attempt this is
     */
    public void signalCrawlAttemptCompleted(boolean success, Object cookie) {
      logger.debug("Content crawl completed repair on " + cookie);
      theDaemon.getPollManager().resumePoll(success, cookie);
    }
  }

  public void startTreeWalk() {
    logger.info("Starting treewalk");
    treeWalkHandler = new TreeWalkHandler(this, theDaemon.getCrawlManager());
    treeWalkHandler.doTreeWalk();
  }
}
