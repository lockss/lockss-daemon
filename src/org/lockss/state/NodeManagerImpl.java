/*
 * $Id: NodeManagerImpl.java,v 1.97 2003-04-15 02:21:22 claire Exp $
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
public class NodeManagerImpl extends BaseLockssManager implements NodeManager {
  /**
   * This parameter indicates the size of the {@link NodeStateCache} used by the
   * node manager.
   */
  public static final String PARAM_NODESTATE_CACHE_SIZE =
      Configuration.PREFIX + "state.cache.size";
  static final int DEFAULT_CACHE_SIZE = 100;
  static HistoryRepository historyRepo;
  private LockssRepository lockssRepo;
  static PollManager pollManager;

  ArchivalUnit managedAu;
  AuState auState;
  NodeStateCache nodeCache;
  int maxCacheSize = DEFAULT_CACHE_SIZE;
  private static Logger logger = Logger.getLogger("NodeManager");
  TreeWalkHandler treeWalkHandler;

  NodeManagerImpl(ArchivalUnit au) {
    managedAu = au;
  }

  /**
   * start the plugin manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    super.startService();
    logger.debug("NodeManager being started");
    historyRepo = theDaemon.getHistoryRepository();
    lockssRepo = theDaemon.getLockssRepository(managedAu);
    pollManager = theDaemon.getPollManager();

    nodeCache = new NodeStateCache(maxCacheSize);

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
    if (treeWalkHandler != null) {
      treeWalkHandler.end();
      treeWalkHandler = null;
    }
    super.stopService();
    logger.debug("NodeManager sucessfully stopped");
  }

  public void forceTreeWalk() {
    logger.info("Forcing treewalk...");
    if (treeWalkHandler == null) {
      treeWalkHandler = new TreeWalkHandler(this, theDaemon.getCrawlManager());
      treeWalkHandler.start();
    }
    treeWalkHandler.forceTreeWalk();
  }

  public void forceTopLevelPoll() {
    logger.info("Forcing top level poll...");
    NodeState topNode = getNodeState(managedAu.getAUCachedUrlSet());
    Iterator activePolls = topNode.getActivePolls();
    if (!activePolls.hasNext()) {
      callTopLevelPoll();
    }
  }

  public void startPoll(CachedUrlSet cus, PollTally state,
                        boolean isReplayPoll) {
    NodeState nodeState = getNodeState(cus);
    PollSpec spec = state.getPollSpec();
    int status = isReplayPoll ? PollState.REPAIRING : PollState.RUNNING;
    PollState pollState = new PollState(state.getType(), spec.getLwrBound(),
                                        spec.getUprBound(),
                                        status, state.getStartTime(),
                                        Deadline.in(state.getDuration()),
                                        state.isMyPoll());
    ( (NodeStateImpl) nodeState).addPollState(pollState);
  }

  public boolean shouldStartPoll(CachedUrlSet cus, PollTally state) {
    NodeState nodeState = getNodeState(cus);

    if (nodeState == null) {
      logger.error("Failed to find a valid node state for: " + cus);
      return false;
    }

    if (!state.isMyPoll() && hasDamage(cus, state.getType())) {
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
      ( (NodeStateImpl) state).setLastHashDuration(hashDuration);
    }
  }

  public synchronized NodeState getNodeState(CachedUrlSet cus) {
    String url = cus.getUrl();
    logger.debug3("Getting " + url);
    NodeState node = nodeCache.getState(url);
    if (node == null) {
      // if in repository, add
      try {
        if (lockssRepo.getNode(url) != null) {
          node = createNodeState(cus);
        }
      }
      catch (MalformedURLException mue) {
        logger.error("Can't get NodeState due to bad CachedUrlSet: " + cus);
      }
    }
    return node;
  }

  NodeState createNodeState(CachedUrlSet cus) {
    logger.debug2("Loading NodeState: " + cus.toString());
    // load from file cache, or get a new one
    NodeState state = historyRepo.loadNodeState(cus);
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

  void updateState(NodeState state, PollTally results) {
    PollState pollState = getPollState(state, results);
    if (pollState == null) {
      logger.error("Results updated for a non-existent poll.");
      throw new UnsupportedOperationException("Results updated for a "
                                              + "non-existent poll.");
    }
    try {
      if (results.isErrorState()) {
        pollState.status = mapResultsErrorToPollError(results.getErr());
        logger.info("Poll didn't finish fully.  Error: "
                    + results.getErrString());
        return;
      }

      if (results.getType() == Poll.CONTENT_POLL) {
        handleContentPoll(pollState, results, state);
      }
      else if (results.getType() == Poll.NAME_POLL) {
        handleNamePoll(pollState, results, state);
      }
      else {
        String err = "Request to update state for unknown type: " +
            results.getType();
        logger.error(err);
        throw new UnsupportedOperationException(err);
      }
    }
    finally {
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
    }
    else {
      // if disagree
      if (pollState.getStatus() == PollState.REPAIRING) {
        logger.debug2("lost repair content poll, state = unrepairable");
        // if repair poll, can't be repaired
        pollState.status = PollState.UNREPAIRABLE;
        updateReputations(results);
      } else {
        // otherwise, schedule repair or name poll
        boolean doRepair = true;
        if (nodeState.isInternalNode()) {
          // if internal node, repair only if a 'single node' spec
          doRepair = (results.getCachedUrlSet().getSpec()
                      instanceof SingleNodeCachedUrlSetSpec);
        }
        if (doRepair) {
          logger.debug2("lost content poll, state = repairing, marking for repair.");
          // if leaf node, we need to repair
          pollState.status = PollState.REPAIRING;
          markNodeForRepair(nodeState.getCachedUrlSet(), results);
        } else {
          logger.debug2("lost content poll, state = lost, calling name poll.");
          // if internal node, we need to call a name poll
          pollState.status = PollState.LOST;
          callNamePoll(new PollSpec(results.getCachedUrlSet()));
        }
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
        logger.debug2("won name poll, calling content poll on subnodes.");
        try {
          // call a content poll for this node's content if we haven't started
          // sub-dividing yet
          CachedUrlSetSpec spec = results.getCachedUrlSet().getSpec();
          if ((spec instanceof RangeCachedUrlSetSpec) &&
              (((RangeCachedUrlSetSpec)spec).getLowerBound()==null)) {
            logger.debug2("calling content poll on node's contents");
            callSingleNodeContentPoll(results.getCachedUrlSet());
          }
          // call a content poll on this node's subnodes
          callContentPollsOnSubNodes(nodeState, results.getCachedUrlSet());
          pollState.status = PollState.WON;
        }
        catch (IOException ex) {
          logger.debug2("unable to start content poll, setting status to ERR_IO");
          pollState.status = PollState.ERR_IO;
        }
      }
      else {
        logger.debug2("won name poll, setting state to WON");
        pollState.status = PollState.WON;
      }
    }
    else {
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
        String url = (String) masterIt.next();
        // compare against my list
        if (localSet.contains(url)) {
          // removing from the set to leave only files for deletion
          localSet.remove(url);
        }
        else {
          // if not found locally, fetch
          logger.debug2("marking missing node for repair: " + url);
          CachedUrlSet newCus = au.makeCachedUrlSet(
              new RangeCachedUrlSetSpec(baseUrl + url));
          markNodeForRepair(newCus, results);
        }
      }
      localIt = localSet.iterator();
      while (localIt.hasNext()) {
        // for extra items - deletion
        String url = (String) localIt.next();
        logger.debug2("deleting node: " + url);
        try {
          CachedUrlSet oldCus = au.makeCachedUrlSet(
              new RangeCachedUrlSetSpec(baseUrl + url));
          deleteNode(oldCus);
          //set crawl status to DELETED
          NodeState oldState = getNodeState(oldCus);
          oldState.getCrawlState().type = CrawlState.NODE_DELETED;
        }
        catch (Exception e) {
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
    PollSpec lastPollSpec = new PollSpec(node.getCachedUrlSet(),
                                         lastHistory.lwrBound,
                                         lastHistory.uprBound);
    switch (lastHistory.status) {
      case PollState.REPAIRING:
      case PollState.SCHEDULED:
      case PollState.RUNNING:
        // if this poll should be running make sure it is running.
        if (!pollManager.isPollRunning(lastHistory.getType(), lastPollSpec)) {
          logger.debug2("treewalk - re-calling last running poll");
          callLastPoll(lastPollSpec, lastHistory);
          return true;
        }
        break;
      case PollState.WON:
      case PollState.REPAIRED:
        // if this is a poll with a range make sure we don't have
        // a lost poll lurking underneath this one.
        break;
      case PollState.LOST:
        if (lastHistory.getType() == Poll.CONTENT_POLL) {
          logger.debug2("treewalk - calling namepoll for lost content poll");
          callNamePoll(lastPollSpec);
          return true;
        }
        // for name polls the important poll is just below this so it's
        // safe to skip over it.
      case PollState.UNREPAIRABLE:
        // we need to do something here
        break;
      case PollState.ERR_SCHEDULE_HASH:
      case PollState.ERR_HASHING:
      case PollState.ERR_NO_QUORUM:
      case PollState.ERR_IO:
      case PollState.ERR_UNDEFINED:
        // if we ended with an error and it was our poll,
        // we need to recall this poll.
        if (lastHistory.getOurPoll()) {
          logger.debug2("treewalk - re-calling last error poll");
          callLastPoll(lastPollSpec, lastHistory);
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

  private void closePoll(PollState pollState, long duration, Collection votes,
                         NodeState nodeState) {
    PollHistory history = new PollHistory(pollState, duration, votes);
    ( (NodeStateImpl) nodeState).closeActivePoll(history);
    logger.debug2("Closing poll for url '" +
                  nodeState.getCachedUrlSet().getUrl() + " " +
                  pollState.getLwrBound() + "-" +
                  pollState.getUprBound() + "'");
    // if this is an AU top-level content poll
    // update the AuState to indicate the poll is finished
    if ( (AuUrl.isAuUrl(nodeState.getCachedUrlSet().getUrl())) &&
        (pollState.getType() == Poll.CONTENT_POLL)) {
      getAuState().newPollFinished();
      logger.info("Top level poll finished.");
    }
  }

  private PollState getPollState(NodeState state, PollTally results) {
    Iterator polls = state.getActivePolls();
    while (polls.hasNext()) {
      PollState pollState = (PollState) polls.next();
      PollSpec spec = results.getPollSpec();
      logger.debug2("Getting poll state for spec: " + spec);
      if (StringUtil.equalStrings(pollState.getLwrBound(), spec.getLwrBound()) &&
          StringUtil.equalStrings(pollState.getUprBound(), spec.getUprBound()) &&
          (pollState.getType() == results.getType())) {
        return pollState;
      }
    }
    return null;
  }

  protected void setConfig(Configuration newConfig,
                           Configuration oldConfig,
                           Set changedKeys) {
    if (changedKeys.contains(PARAM_NODESTATE_CACHE_SIZE)) {
      maxCacheSize = newConfig.getInt(PARAM_NODESTATE_CACHE_SIZE,
                                      DEFAULT_CACHE_SIZE);
      if (nodeCache != null) {
        nodeCache.setCacheSize(maxCacheSize);
      }
    }
  }

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

  private void markNodeForRepair(CachedUrlSet cus, PollTally tally) {
    logger.debug2("suspending poll " + tally.getPollKey());
    pollManager.suspendPoll(tally.getPollKey());
    logger.debug2("scheduling repair");
    try {
      theDaemon.getCrawlManager().scheduleRepair(managedAu,
                                                 new URL(cus.getUrl()),
                                                 new ContentRepairCallback(),
                                                 tally.getPollKey());
    }
    catch (MalformedURLException mue) {
      // this shouldn't happen
      // if it does, let the tree walk catch the repair
    }
  }

  private void deleteNode(CachedUrlSet cus) throws IOException {
    // delete the node from the LockssRepository
    LockssRepository repository = theDaemon.getLockssRepository(managedAu);
    repository.deleteNode(cus.getUrl());
  }

  private void callContentPollsOnSubNodes(NodeState state, CachedUrlSet cus) throws
      IOException {
    Iterator children = cus.flatSetIterator();
    List childList = convertChildrenToCUSList(children);
    // Divide the list in two and call two new content polls
    if (childList.size() > 4) {
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
      logger.debug2("less than 4 items, calling content poll on all items.");
      for (int i = 0; i < childList.size(); i++) {
        callContentPoll((CachedUrlSet) childList.get(i), null, null);
      }
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
    PollSpec spec = new PollSpec(cus, lwr, upr);
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
      logger.error("Excption calling name poll on " + spec, ioe);
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
    Iterator childIt = null;
    switch (pollType) {
      case Poll.CONTENT_POLL:
        childIt = cus.contentHashIterator();
        break;
      case Poll.NAME_POLL:
        childIt = cus.flatSetIterator();
        break;
    }
    while (childIt.hasNext()) {
      CachedUrlSetNode child = (CachedUrlSetNode) childIt.next();
      CachedUrlSet child_cus;
      if(child instanceof CachedUrlSet) {
        child_cus = (CachedUrlSet) child;
      }
      else{
        CachedUrlSetSpec rSpec = new RangeCachedUrlSetSpec(child.getUrl());
        child_cus = ( (BaseArchivalUnit) managedAu).makeCachedUrlSet(rSpec);
      }
      NodeState nodeState = getNodeState(child_cus);
      PollHistory pollHistory = nodeState.getLastPollHistory();
      if (pollHistory != null &&
          ( (pollHistory.status == pollHistory.UNREPAIRABLE) ||
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

    if (results.didWinPoll()) {
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

  private Set createUrlSetFromCusIterator(Iterator cusIt) {
    Set set = new HashSet();
    while (cusIt.hasNext()) {
      String key = (String) cusIt.next();
      set.add(key);
    }
    return set;
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
      pollManager.resumePoll(success, cookie);
    }
  }
}