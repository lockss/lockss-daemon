/*
 * $Id: NodeManagerImpl.java,v 1.59 2003-03-15 02:53:29 aalto Exp $
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
import org.lockss.protocol.LcapMessage;
import org.lockss.protocol.IdentityManager;
import org.lockss.repository.LockssRepository;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.app.*;
import java.net.URL;
import org.lockss.plugin.base.*;
import org.lockss.plugin.*;
import org.apache.commons.collections.LRUMap;
import org.lockss.repository.RepositoryNode;
import java.net.MalformedURLException;

/**
 * Implementation of the NodeManager.
 */
public class NodeManagerImpl implements NodeManager {
    public static final String PREFIX = Configuration.PREFIX + "treewalk.";

    /**
     * Configuration parameter name for duration, in ms, for which the treewalk
     * estimation should run.
     */
    public static final String PARAM_TREEWALK_ESTIMATE_DURATION =
        PREFIX + "estimate.duration";
    static final int DEFAULT_TREEWALK_ESTIMATE_DURATION = 1000;

    /**
     * Configuration parameter name for interval, in ms, between treewalks.
     */
    public static final String PARAM_TREEWALK_INTERVAL = PREFIX + "interval";
    static final int DEFAULT_TREEWALK_INTERVAL = 60 * 60 * 1000; //1 hour

    /**
     * Configuration parameter name for default interval, in ms, after which
     * a new top level poll should be called.
     */
    public static final String PARAM_TOP_LEVEL_POLL_INTERVAL =
        Configuration.PREFIX + "top.level.poll.interval";
    static final int DEFAULT_TOP_LEVEL_POLL_INTERVAL =
        14 * 24 * 60 * 60 * 1000; //2 weeks

    /**
     * Configuration parameter name for whether the treewalk thread should
     * be started right away
     */
    public static final String PARAM_START_TREEWALK_THREAD =
      PREFIX + "start_treewalk_thread";

  static final double MAX_DEVIATION = 0.1;

  private static LockssDaemon theDaemon;
  private NodeManager theManager = null;
  static HistoryRepository historyRepo;
  private LockssRepository lockssRepo;
  private static CrawlManager theCrawlManager = null;
  long treeWalkEstimate = -1;

  private ArchivalUnit managedAu;
  private AuState auState;
  private Map nodeMap;
  private Logger logger = Logger.getLogger("NodeManager");
  TreeWalkThread treeWalkThread;
  private boolean treeWalkRunning = false;
  long treeWalkInterval;
  long topPollInterval;
  long treeWalkTestDuration;
  boolean shouldStartTreeWalkThread;

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
      historyRepo = theDaemon.getHistoryRepository();
      lockssRepo = theDaemon.getLockssRepository(managedAu);
      nodeMap = new NodeStateMap(historyRepo);
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
        public void configurationChanged(Configuration oldConfig,
                                         Configuration newConfig,
                                         Set changedKeys) {
          setConfig(newConfig, oldConfig);
        }
      };
    Configuration.registerConfigurationCallback(configCallback);

    historyRepo = theDaemon.getHistoryRepository();
    theCrawlManager = theDaemon.getCrawlManager();

    if (shouldStartTreeWalkThread) {
      treeWalkThread = new TreeWalkThread("TreeWalk: " + managedAu.getName(),
                                          getAuState().getLastTreeWalkTime());
      treeWalkThread.start();
    }
    logger.debug("NodeManager sucessfully started");
  }

  /**
   * stop the plugin manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    logger.debug("NodeManager being stopped");
    // checkpoint here
    if (treeWalkThread!=null) {
      treeWalkThread.interrupt();
      treeWalkThread = null;
    }
    Configuration.unregisterConfigurationCallback(configCallback);
    theManager = null;
    logger.debug("NodeManager sucessfully stopped");
  }

  private void setConfig(Configuration config, Configuration oldConfig) {
    treeWalkInterval = config.getTimeInterval(
        PARAM_TREEWALK_INTERVAL, DEFAULT_TREEWALK_INTERVAL);
    logger.debug3("treeWalkInterval reset to "+treeWalkInterval);
    topPollInterval = config.getTimeInterval(
        PARAM_TOP_LEVEL_POLL_INTERVAL, DEFAULT_TOP_LEVEL_POLL_INTERVAL);
    treeWalkTestDuration = config.getTimeInterval(
        PARAM_TREEWALK_ESTIMATE_DURATION, DEFAULT_TREEWALK_ESTIMATE_DURATION);
    shouldStartTreeWalkThread = config.getBoolean(PARAM_START_TREEWALK_THREAD,
                                                  true);
  }

  public void startPoll(CachedUrlSet cus, Poll.VoteTally state) {
    NodeState nodeState = getNodeState(cus);
    if (nodeState == null) {
      logger.error("Failed to find a valid node state for: " + cus);
      return;
    }
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
  }

  public void newContentCrawlFinished() {
    getAuState().newCrawlFinished();
  }

  public NodeState getNodeState(CachedUrlSet cus) {
    String url = cus.getUrl();
    logger.debug3("Getting " + url);
    NodeState node = (NodeState)nodeMap.get(url);
    if (node==null) {
      // if in repository, add
      try {
        if (lockssRepo.getNode(url) != null) {
          node = addNodeState(cus);
        }
      } catch (MalformedURLException mue) {
        logger.error("Can't get NodeState due to bad CachedUrlSet: "+cus);
      }
    }
    return node;
  }

  public AuState getAuState() {
    if (auState==null) {
      auState = historyRepo.loadAuState(managedAu);
    }
    return auState;
  }



  public Iterator getActiveCrawledNodes(CachedUrlSet cus) {
    Iterator entries = nodeMap.entrySet().iterator();
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
    Iterator entries = nodeMap.entrySet().iterator();
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
    Iterator entries = nodeMap.entrySet().iterator();
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
    Iterator entries = nodeMap.entrySet().iterator();
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

  public long getEstimatedTreeWalkDuration() {
    if (treeWalkEstimate==-1) {
      // estimate via short walk
      // this is not a fake walk; it functionally walks part of the tree
      long timeTaken = 0;
      int nodesWalked = 0;
      long startTime = TimeBase.nowMs();
      Iterator nodesIt = nodeMap.values().iterator();
      Deadline deadline = Deadline.in(treeWalkTestDuration);
      while (!deadline.expired() && nodesIt.hasNext()) {
        //XXX fix to be non-functional
        walkNodeState((NodeState)nodesIt.next());
        nodesWalked++;
      }
      timeTaken = TimeBase.nowMs() - startTime;
      if (timeTaken == 0) {
        logger.warning("Test finished in zero time after walking " +
                       nodesWalked + " nodes.");
        // returning a harmless constant
        return 1;
      }
      // calculate
      double nodesPerMs = ((double)timeTaken / nodesWalked);
      // check size (node count) of tree
      int nodeCount = nodeMap.size();
      treeWalkEstimate = (long)(nodeCount * nodesPerMs);
    }
    return treeWalkEstimate;
  }

  public void startTreeWalk() {
    doTreeWalk();
  }

  void doTreeWalk() {
    treeWalkRunning = true;
    logger.debug("Attempting tree walk.");
    if (theCrawlManager.canTreeWalkStart(managedAu, null, null)) {
      long startTime = TimeBase.nowMs();

      NodeState topNode = getNodeState(managedAu.getAUCachedUrlSet());
      Iterator activePolls = topNode.getActivePolls();
      // only continue if no top level poll being considered
      if (!activePolls.hasNext()) {
        // if it's been too long, start a top level poll
        if ((startTime - getAuState().getLastTopLevelPollTime())
            > topPollInterval) { //XXX get from plugin
          callTopLevelPoll();
        } else {
          logger.debug("Tree walk started.");
          nodeTreeWalk(nodeMap);
          long elapsedTime = TimeBase.nowMs() - startTime;
          updateEstimate(elapsedTime);
        }
      }
    }
    //alert the thread and set the AuState
    getAuState().treeWalkFinished();
    //    treeWalkThread.treeWalkFinished();
    logger.debug("Tree walk finished.");
    treeWalkRunning = false;
  }

  void updateEstimate(long elapsedTime) {
    if (treeWalkEstimate==-1) {
      treeWalkEstimate = elapsedTime;
    } else {
      // average with current estimate
      treeWalkEstimate = (treeWalkEstimate + elapsedTime) / 2;
    }
  }

  private void nodeTreeWalk(Map nodeMap) {
    //XXX use true tree walk
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
          PollSpec spec = new PollSpec(node.getCachedUrlSet(),
                                       lastHistory.lwrBound,
                                       lastHistory.uprBound);
          callNamePoll(spec.getCachedUrlSet());
          break;
        default:
          break;
      }
      historyRepo.storePollHistories(node);
    }
  }

  NodeState addNodeState(CachedUrlSet cus) {
    logger.debug3("Loading NodeState: " + cus.toString());
    // load from file cache, or get a new one
    NodeStateImpl state = (NodeStateImpl)historyRepo.loadNodeState(cus);
    state.setNodeManagerImpl(this);
    nodeMap.put(cus.getUrl(), state);
    return state;
  }

  void updateState(NodeState state, Poll.VoteTally results) {
    PollState pollState = getPollState(state, results);
    if (pollState == null) {
      logger.error("Results updated for a non-existent poll.");
      throw new UnsupportedOperationException("Results updated for a "
                                              +"non-existent poll.");
    }
    if (results.getErr() < 0) {
      pollState.status = mapResultsErrorToPollError(results.getErr());
      logger.info("Poll didn't finish fully.  Error code: "
                  + pollState.status);
      return;
    }

    if (results.getType() == Poll.CONTENT_POLL) {
      handleContentPoll(pollState, results, state);
      closePoll(pollState, results.getDuration(), results.getPollVotes(),
                state);
    } else if (results.getType() == Poll.NAME_POLL) {
      handleNamePoll(pollState, results, state);
      closePoll(pollState, results.getDuration(), results.getPollVotes(),
                state);
    } else {
      logger.error("Updating state for invalid results type: " +
                   results.getType());
      throw new UnsupportedOperationException("Updating state for invalid "
                                              +"results type.");
    }
  }

  void handleContentPoll(PollState pollState, Poll.VoteTally results,
                                 NodeState nodeState) {
    logger.debug("handling content poll results: " + results);
    if (results.didWinPoll()) {
      // if agree
      if (pollState.getStatus() == PollState.RUNNING) {
        logger.debug2("won content poll, state = won.");
        // if normal poll, we won!
        pollState.status = PollState.WON;
      } else if (pollState.getStatus() == PollState.REPAIRING) {
        // if repair poll, we're repaired
        logger.debug2("won repair poll, state = repaired.");
        pollState.status = PollState.REPAIRED;
      }
      updateReputations(results);
    } else {
      // if disagree
      if (pollState.getStatus() == PollState.REPAIRING) {
        logger.debug2("lost repair poll, state = unrepairable");
        // if repair poll, can't be repaired
        pollState.status = PollState.UNREPAIRABLE;
        updateReputations(results);
      } else if (nodeState.isInternalNode()) {
        logger.debug2("lost content poll, state = lost, calling name poll.");
        // if internal node, we need to call a name poll
        pollState.status = PollState.LOST;
        callNamePoll(results.getCachedUrlSet());
      } else {
        logger.debug2("lost content poll, state = repairing, node marked for repair.");
        // if leaf node, we need to repair
        pollState.status = PollState.REPAIRING;
        try {
          markNodeForRepair(nodeState.getCachedUrlSet(), results);
        } catch (IOException ioe) {
          logger.error("Repair attempt failed.", ioe);
          // the treewalk will fix this eventually
        }
      }
    }
  }

  void handleNamePoll(PollState pollState, Poll.VoteTally results,
                              NodeState nodeState) {
    logger.debug2("handling name poll results " + results);
    if (results.didWinPoll()) {
      // if agree
      if (results.isMyPoll()) {
        // if poll is mine
        try {
          logger.debug2("won name poll, calling content poll on subnodes.");
          callContentPollOnSubNodes(nodeState, results);
          pollState.status = PollState.WON;
        } catch (Exception e) {
          logger.error("Error scheduling content polls.", e);
          pollState.status = PollState.ERR_IO;
        }
      } else {
        logger.debug2("won name poll, setting state to WON");
        // if poll is not mine stop - set to WON
        pollState.status = PollState.WON;
      }
    } else {
      // if disagree
      logger.debug2("lost name poll, collecting repair info.");
      String baseUrl = nodeState.getCachedUrlSet().getUrl() + "/";
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
            logger.debug3("marking missing node for repair: " + url);
            CachedUrlSet newCus = au.makeCachedUrlSet(baseUrl+url, null, null);
            markNodeForRepair(newCus, results);
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
        logger.debug3("deleting node: " + url);
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
    historyRepo.storePollHistories(nodeState);
    logger.debug3("Closing poll for url '" +
                  nodeState.getCachedUrlSet().getUrl() + "'");
    // if this is an AU top-level content poll
    // update the AuState to indicate the poll is finished
    if ((AuUrl.isAuUrl(nodeState.getCachedUrlSet().getUrl())) &&
        (pollState.getType() == Poll.CONTENT_POLL)) {
      getAuState().newPollFinished();
      logger.info("Top level poll finished.");
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
    theDaemon.getCrawlManager().scheduleRepair(managedAu,
        new URL(cus.getUrl()), new ContentRepairCallback(),
                                 tally.getPollKey());
  }

  private void deleteNode(CachedUrlSet cus) throws IOException {
    // delete the node from the LockssRepository
    LockssRepository repository = theDaemon.getLockssRepository(managedAu);
    repository.deleteNode(cus.getUrl());
  }

  private void callContentPollOnSubNodes(NodeState state,
                                         Poll.VoteTally results)
      throws IOException {
    ArrayList childList = new ArrayList();

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
          cus = ((BaseArchivalUnit)managedAu).makeCachedUrlSet(rSpec);
      }
      childList.add(cus);
    }
    // Divide the list in two and call two new content polls
    if(childList.size() > 4) {
      String base = results.getCachedUrlSet().getSpec().getUrl();
      int mid = childList.size() / 2;
      String lwr = ( (CachedUrlSet) childList.get(0)).getSpec().getUrl();
      lwr = lwr.startsWith(base) ? lwr.substring(base.length()) : lwr;
      String upr = ( (CachedUrlSet) childList.get(mid)).getSpec().getUrl();
      upr = upr.startsWith(base) ? upr.substring(base.length()) : upr;
      PollSpec pspec = new PollSpec(results.getCachedUrlSet(), lwr, upr);
      logger.debug2("calling first content poll on " + pspec);
      theDaemon.getPollManager().requestPoll(LcapMessage.CONTENT_POLL_REQ,
                                             pspec);

      lwr = ( (CachedUrlSet) childList.get(mid + 1)).getSpec().getUrl();
      lwr = lwr.startsWith(base) ? lwr.substring(base.length()) : lwr;
      upr = ( (CachedUrlSet) childList.get(childList.size() - 1)).getSpec().
          getUrl();
      upr = upr.startsWith(base) ? upr.substring(base.length()) : upr;
      pspec = new PollSpec(results.getCachedUrlSet(), lwr, upr);
      logger.debug2("calling second content poll on " + pspec);
      theDaemon.getPollManager().requestPoll(LcapMessage.CONTENT_POLL_REQ,
                                             pspec);
    }
    else {
      logger.debug2("less than 4 items, calling content poll on all items.");
      for(int i=0; i< childList.size(); i++) {
        theDaemon.getPollManager().requestPoll(LcapMessage.CONTENT_POLL_REQ,
        new PollSpec((CachedUrlSet)childList.get(i)));
      }
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

  void removeReference(String urlKey) {
//XXX    nodeMap.remove(urlKey);
  }


  boolean shouldTreeWalkStart() {
    if (treeWalkRunning) {
      return false;
    }
    long lastTreeWalkTime = getAuState().getLastTreeWalkTime();
    long timeSinceLastTW = TimeBase.nowMs() - lastTreeWalkTime;
    logger.debug3(timeSinceLastTW+" since last treewalk");
    logger.debug("Treewalks should happen every "+treeWalkInterval);
    if (timeSinceLastTW >= treeWalkInterval) {
      return true;
    }
    return false;
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
    boolean goOn = true;
    Deadline deadline;

    private TreeWalkThread(String name, long lastRun) {
      super(name);
      this.lastRun = lastRun;
    }


    public void end() {
      goOn = false;
      if (deadline != null) {
        deadline.expire();
      }
    }

    public void run() {
      while (goOn) {
        if (shouldTreeWalkStart()) {
          doTreeWalk();
        } else {
          long delta = (long)((double)MAX_DEVIATION*treeWalkInterval);
          logger.debug3("Creating a deadline for "+treeWalkInterval
                        +" with delta of "+delta);
          deadline =
            Deadline.inRandomDeviation(treeWalkInterval, delta);
          try {
            deadline.sleep();
          } catch (InterruptedException ie) {
          }
        }
      }
    }
  }
}
