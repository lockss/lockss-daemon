/*
 * $Id: NodeManagerImpl.java,v 1.44 2003-03-01 02:56:06 aalto Exp $
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
import org.lockss.crawler.CrawlManager;
import org.lockss.plugin.PluginManager;
import org.lockss.protocol.LcapMessage;
import org.lockss.protocol.IdentityManager;
import org.lockss.repository.LockssRepository;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.app.*;
import java.net.URL;
import org.lockss.plugin.base.*;
import org.lockss.plugin.*;

/**
 * Implementation of the NodeManager.
 */
public class NodeManagerImpl implements NodeManager {
  /**
   * Configuration parameter name for duration, in ms, for which the treewalk
   * test should run.
   */
  public static final String PARAM_TREEWALK_TEST_DURATION =
      Configuration.PREFIX + "treewalk.test.duration";
  static final int DEFAULT_TREEWALK_TEST_DURATION = 1000;

  /**
   * Configuration parameter name for interval, in ms, between treewalks.
   */
  public static final String PARAM_TREEWALK_INTERVAL =
      Configuration.PREFIX + "treewalk.interval";
  static final int DEFAULT_TREEWALK_INTERVAL = 14*24*60*60*1000; //2 weeks

  /**
   * Configuration parameter name for default interval, in ms, after which
   * a new top level poll should be called.
   */
  public static final String PARAM_TOP_LEVEL_POLL_INTERVAL =
      Configuration.PREFIX + "top.level.poll.duration";
  static final int DEFAULT_TOP_LEVEL_POLL_INTERVAL = 14*24*60*60*1000; //2 weeks

  private static LockssDaemon theDaemon;
  private static NodeManager theManager = null;
  static HistoryRepository repository;
  private static CrawlManager theCrawlManager = null;
  private static HashMap auEstimateMap = new HashMap();
  long treeWalkInterval;
  long topPollInterval;
  long treeWalkTestDuration;

  private ArchivalUnit managerAu;
  private AuState auState;
  private TreeMap nodeMap = new TreeMap();
  private static Logger logger = Logger.getLogger("NodeManager");
  TreeWalkThread treeWalkThread;

  NodeManagerImpl(ArchivalUnit au) {
    managerAu = au;
  }

  /**
   * For testing only!  Returns a NodeManager.
   * @param au the ArchivalUnit
   * @return the NodeManager
   */
  public static NodeManager getTestNodeManager(ArchivalUnit au) {
    return new NodeManagerImpl(au);
  }

  /**
   * init the plugin manager.
   * @param daemon the LockssDaemon instance
   * @throws LockssDaemonException if we already instantiated this manager
   * @see org.lockss.app.LockssManager#initService(LockssDaemon daemon)
   */
  public void initService(LockssDaemon daemon) throws LockssDaemonException {
    if (theManager == null) {
      theDaemon = daemon;
      theManager = this;
      theCrawlManager = theDaemon.getCrawlManager();
      repository = theDaemon.getHistoryRepository();
      loadStateTree();

      treeWalkInterval = Configuration.getIntParam(
          PARAM_TREEWALK_INTERVAL, DEFAULT_TREEWALK_INTERVAL);
      topPollInterval = Configuration.getIntParam(
          PARAM_TOP_LEVEL_POLL_INTERVAL, DEFAULT_TOP_LEVEL_POLL_INTERVAL);
      treeWalkTestDuration = Configuration.getIntParam(
          PARAM_TREEWALK_TEST_DURATION, DEFAULT_TREEWALK_TEST_DURATION);
    } else {
      throw new LockssDaemonException("Multiple Instantiation.");
    }
  }

  /**
   * start the plugin manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    Configuration.registerConfigurationCallback(new Configuration.Callback() {
      public void configurationChanged(Configuration oldConfig,
                                       Configuration newConfig,
                                       Set changedKeys) {
        setConfig(newConfig, oldConfig);
      }
    });

    treeWalkThread = new TreeWalkThread(managerAu.getPluginId() + ":" +
                                        managerAu.getAUId() + ":TreeWalkThread",
                                        getAuState().getLastTreeWalkTime());
  }

  /**
   * stop the plugin manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    // checkpoint here
    if (treeWalkThread!=null) {
      treeWalkThread.interrupt();
    }
    theManager = null;
  }

  private void setConfig(Configuration config, Configuration oldConfig) {
    treeWalkInterval = Configuration.getIntParam(
        PARAM_TREEWALK_INTERVAL, DEFAULT_TREEWALK_INTERVAL);
    topPollInterval = Configuration.getIntParam(
        PARAM_TOP_LEVEL_POLL_INTERVAL, DEFAULT_TOP_LEVEL_POLL_INTERVAL);
    treeWalkTestDuration = Configuration.getIntParam(
        PARAM_TREEWALK_TEST_DURATION, DEFAULT_TREEWALK_TEST_DURATION);
  }

  public void startPoll(CachedUrlSet cus, Poll.VoteTally state) {
    NodeState nodeState = getNodeState(cus);
    PollSpec spec = state.getPollSpec();
    PollState pollState = new PollState(state.getType(), spec.getLwrBound(),
                                        spec.getUprBound(),
                                        PollState.RUNNING, state.getStartTime(),
                                        null);
    ((NodeStateImpl)nodeState).addPollState(pollState);
  }

  public void updatePollResults(CachedUrlSet cus, Poll.VoteTally results) {
    NodeState state = getNodeState(cus);
    updateState(state, results);
    repository.storePollHistories(state);
  }

  public void newTopLevelCrawlFinished() {
    getAuState().newCrawlFinished();
  }

  public NodeState getNodeState(CachedUrlSet cus) {
    logger.debug("Getting " + cus.getUrl());
    return (NodeState)nodeMap.get(cus.getUrl());
  }

  public AuState getAuState() {
    if (auState==null) {
      auState = repository.loadAuState(managerAu);
    }
    return auState;
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
      // estimate via short walk
      // this is not a fake walk; it functionally walks part of the tree
      long timeTaken = 0;
      int nodesWalked = 0;
      long startTime = TimeBase.nowMs();
      Iterator nodesIt = nodeMap.values().iterator();
      Deadline deadline = Deadline.in(treeWalkTestDuration);
      while (!deadline.expired() && nodesIt.hasNext()) {
        walkNodeState((NodeState)nodesIt.next());
        nodesWalked++;
      }
      timeTaken = TimeBase.nowMs() - startTime;
      if (timeTaken == 0) {
        logger.error("Test finished in zero time: using nodesWalked estimate.");
        return nodesWalked;
      }
      // calculate
      double nodesPerMs = ((double)timeTaken / nodesWalked);
      // check size (node count) of tree
      int nodeCount = nodeMap.size();
      estimateL = new Long((long)(nodeCount * nodesPerMs));

      auEstimateMap.put(managerAu, estimateL);
    }
    return estimateL.longValue();
  }

  public void startTreeWalk() {
    doTreeWalk();
  }

  void doTreeWalk() {
    if (theCrawlManager.canTreeWalkStart(managerAu,
					 getAuState(), null, null)) {
      long startTime = TimeBase.nowMs();
      // if it's been too long, start a top level poll
      //XXX ask the Plugin
      if ((startTime - getAuState().getLastTopLevelPollTime())
	  > topPollInterval) {
        callTopLevelPoll();
      } else {
        nodeTreeWalk(nodeMap);
        long elapsedTime = TimeBase.nowMs() - startTime;
        updateEstimate(elapsedTime);
      }
    }
    //alert the thread and set the AuState
    getAuState().treeWalkFinished();

  }

  void updateEstimate(long elapsedTime) {
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
    Iterator nodesIt = nodeMap.values().iterator();
    while (nodesIt.hasNext()) {
      walkNodeState((NodeState)nodesIt.next());
    }
  }

  void walkNodeState(NodeState node) {
    CrawlState crawlState = node.getCrawlState();

    // determine if there are active polls
    Iterator polls = node.getActivePolls();
    while (polls.hasNext()) {
      PollState poll = (PollState)polls.next();
      if ((poll.getStatus()==PollState.RUNNING) ||
          (poll.getStatus()==PollState.REPAIRING) ||
          (poll.getStatus()==PollState.SCHEDULED)) {
        // if there are active polls, don't interfere
        return;
      }
    }
    // at each node, check for crawl state
    switch (crawlState.getType()) {
      case CrawlState.NODE_DELETED:
        // skip node if deleted
        return;
      case CrawlState.BACKGROUND_CRAWL:
      case CrawlState.NEW_CONTENT_CRAWL:
      case CrawlState.REPAIR_CRAWL:
        if (crawlState.getStatus() == CrawlState.FINISHED) {
          // if node is cached
          if (node.getCachedUrlSet().hasContent()) {
            // if (theCrawlManager.shouldRecrawl(managerAu, node)) {
            // then CrawlManager.scheduleBackgroundCrawl()
          }
        }
      default:
        break;
    }
    // check recent histories to see if something needs fixing
    // if there are no current polls
    Iterator historyIt = node.getPollHistories();
    PollHistory lastHistory = null;
    while (historyIt.hasNext()) {
      PollHistory thisHistory = (PollHistory) historyIt.next();
      if ( (lastHistory == null) ||
          (thisHistory.startTime > lastHistory.startTime)) {
        lastHistory = thisHistory;
      }
    }
    if (lastHistory != null) {
      switch (lastHistory.status) {
        // if latest is PollState.LOST or PollState.REPAIRING
        // call a name poll to finish the repair which ended early
        case PollState.LOST:
        case PollState.REPAIRING:
          callNamePoll(node.getCachedUrlSet());
          break;
        default:
          break;
      }
      repository.storePollHistories(node);
    }
  }

  private void loadStateTree() {
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
      CachedUrlSetNode child = (CachedUrlSetNode)children.next();
      switch (child.getType()) {
        case CachedUrlSetNode.TYPE_CACHED_URL_SET:
          recurseLoadCachedUrlSets((CachedUrlSet)child);
          break;
        case CachedUrlSetNode.TYPE_CACHED_URL:
          CachedUrlSetSpec rSpec = new RangeCachedUrlSetSpec(child.getUrl());
          CachedUrlSet newSet = ((BaseArchivalUnit)managerAu).makeCachedUrlSet(
              rSpec);
          addNewNodeState(newSet);
      }
    }
  }

  private void addNewNodeState(CachedUrlSet cus) {
    logger.debug("Adding NewNodeState: " + cus.toString());
    NodeState state = new NodeStateImpl(cus, new CrawlState(-1,
        CrawlState.FINISHED, 0), new ArrayList(), repository);
    nodeMap.put(cus.getUrl(), state);
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
      logger.error("Updating state for invalid results type: " +
                   results.getType());
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
        callNamePoll(nodeState.getCachedUrlSet());
      } else {
        // if leaf node, we need to repair
        pollState.status = PollState.REPAIRING;
        try {
          markNodeForRepair(nodeState.getCachedUrlSet(), results);
          closePoll(pollState, results.getDuration(), results.getPollVotes(),
                    nodeState);
        } catch (IOException ioe) {
          logger.error("Repair attempt failed.", ioe);
          // the treewalk will fix this eventually
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
            CachedUrlSet newCus = au.makeCachedUrlSet(url, null, null);
            markNodeForRepair(newCus, results);
            //add to NodeState list
            addNewNodeState(newCus);
          } catch (Exception e) {
            logger.error("Couldn't fetch new node.", e);
            // the treewalk will fix this eventually
          }
        }
      }
      localIt = localSet.iterator();
      while (localIt.hasNext()) {
        // for extra items - deletion
        String url = (String)localIt.next();
        try {
          CachedUrlSet oldCus = au.makeCachedUrlSet(url, null, null);
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
      closePoll(pollState, results.getDuration(), results.getPollVotes(),
                nodeState);
    }
  }

  private void callNamePoll(CachedUrlSet cus) {
    try {
      theDaemon.getPollManager().requestPoll(LcapMessage.NAME_POLL_REQ,
                                             new PollSpec(cus));
    } catch (IOException ioe) {
      logger.error("Couldn't make name poll request.", ioe);
      // the treewalk will fix this eventually
    }
  }

  private void callTopLevelPoll() {
    try {
      theDaemon.getPollManager().requestPoll(LcapMessage.CONTENT_POLL_REQ,
      new PollSpec(managerAu.getAUCachedUrlSet()));

    } catch (IOException ioe) {
      logger.error("Couldn't make top level poll request.", ioe);
      // the treewalk will fix this eventually
    }
  }

  private void closePoll(PollState pollState, long duration, Collection votes,
                         NodeState nodeState) {
    PollHistory history = new PollHistory(pollState, duration, votes);
    ((NodeStateImpl)nodeState).closeActivePoll(history);
    repository.storePollHistories(nodeState);
    // if this is an AU top-level content poll
    // update the AuState to indicate the poll is finished
    if ((AuUrl.isAuUrl(nodeState.getCachedUrlSet().getUrl())) &&
        (pollState.getType() == Poll.CONTENT_POLL)) {
      getAuState().newPollFinished();
    }
  }

  private PollState getPollState(NodeState state, Poll.VoteTally results) {
    Iterator polls = state.getActivePolls();
    while (polls.hasNext()) {
      PollState pollState = (PollState)polls.next();
      PollSpec spec = results.getPollSpec();
      if ((pollState.getLwrBound() == spec.getLwrBound()) &&
          (pollState.getUprBound() == spec.getUprBound()) &&
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

  private void markNodeForRepair(CachedUrlSet cus, Poll.VoteTally tally)
      throws IOException {
    theDaemon.getPollManager().suspendPoll(tally.getPollKey());
    theDaemon.getCrawlManager().scheduleRepair(managerAu,
        new URL(cus.getUrl()), new ContentRepairCallback(),
                                 tally.getPollKey());
  }

  private void deleteNode(CachedUrlSet cus) throws IOException {
    // delete the node from the LockssRepository
    LockssRepository repository =
        theDaemon.getLockssRepository(managerAu).repositoryFactory(
        cus.getArchivalUnit());
    repository.deleteNode(cus.getUrl());
  }

  private void callContentPollOnSubNodes(NodeState state,
      Poll.VoteTally results) throws IOException {
    Iterator children = state.getCachedUrlSet().flatSetIterator();
    while (children.hasNext()) {
      CachedUrlSetNode child = (CachedUrlSetNode)children.next();
      CachedUrlSet cus = null;
      switch (child.getType()) {
        case CachedUrlSetNode.TYPE_CACHED_URL_SET:
          cus = (CachedUrlSet)child;
          break;
        case CachedUrlSetNode.TYPE_CACHED_URL:
          CachedUrlSetSpec rSpec = new RangeCachedUrlSetSpec(child.getUrl());
          cus = ((BaseArchivalUnit)managerAu).makeCachedUrlSet(rSpec);
      }
      theDaemon.getPollManager().requestPoll(LcapMessage.CONTENT_POLL_REQ,
                                             new PollSpec(cus));
    }
  }

  private void updateReputations(Poll.VoteTally results) {
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
      theDaemon.getPollManager().resumePoll(success, cookie);
    }
  }

  /**
   * The thread which runs the treeWalk().
   */
  class TreeWalkThread extends Thread {
    long lastRun;
    boolean treeWalkRunning = false;

    private TreeWalkThread(String name, long lastRun) {
      super(name);
      this.lastRun = lastRun;
    }

    public void run() {
      try {
        while (true) {
          // if it's been more than the treeWalkInterval
          long curInterval = TimeBase.nowMs() - lastRun;
          if (curInterval > treeWalkInterval) {
            // if there's no treewalk running
            if (!treeWalkRunning) {
              treeWalkRunning = true;
              doTreeWalk();
            } else {
              // sleep for the same interval again
              // this should gradually lengthen if the treewalk
              // is taking forever
              sleep(curInterval);
            }
          } else {
            // sleep until it's time for the next treewalk
            sleep(treeWalkInterval-curInterval);
          }
        }
      } catch (Exception e) {
        logger.error("Unexpected exception caught in treewalk thread", e);
      }
    }

    public void treeWalkFinished() {
      treeWalkRunning = false;
      lastRun = getAuState().getLastTreeWalkTime();
    }
  }
}
