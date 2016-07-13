/*

 Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.plugin.*;
import org.lockss.repository.*;
import org.lockss.config.Configuration;
import org.lockss.crawler.*;
import org.lockss.alert.*;
import java.util.ArrayList;

/**
 * Implementation of the NodeManager.
 */
public class NodeManagerImpl
  extends BaseLockssDaemonManager implements NodeManager {

  public static final String PARAM_ENABLE_V3_POLLER = "";
  public static final boolean DEFAULT_ENABLE_V3_POLLER = false;
  
  // the various necessary managers
  LockssDaemon theDaemon;
  HistoryRepository historyRepo;
  private LockssRepository lockssRepo;
  static SimpleDateFormat sdf = new SimpleDateFormat();

  // state and caches for this AU
  ArchivalUnit managedAu;
  AuState auState;
  UniqueRefLruCache nodeCache;
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
    if (logger.isDebug2()) logger.debug2("Starting: " + managedAu);
    theDaemon = getDaemon();
    historyRepo = theDaemon.getHistoryRepository(managedAu);
    lockssRepo = theDaemon.getLockssRepository(managedAu);
    // initializes the state info
    if (getNodeManagerManager().isGlobalNodeCache()) {
      nodeCache = getNodeManagerManager().getGlobalNodeCache();
    } else {
      nodeCache = new UniqueRefLruCache(getNodeManagerManager().paramNodeStateCacheSize);
    }

    auState = historyRepo.loadAuState();

    // damagedNodes not used for V3, avoid file lookup per AU
//     damagedNodes = historyRepo.loadDamagedNodeSet();
    damagedNodes = new DamagedNodeSet(managedAu, historyRepo);

    logger.debug2("NodeManager successfully started");
  }

  public void stopService() {
    if (logger.isDebug()) logger.debug("Stopping: " + managedAu);
    if (activeNodes != null) {
      activeNodes.clear();
    }
    if (damagedNodes != null) {
      damagedNodes.clear();
    }
    if (nodeCache != null) {
      nodeCache.clear();
    }

    super.stopService();
    logger.debug2("NodeManager successfully stopped");
  }

  NodeManagerManager getNodeManagerManager() {
    return theDaemon.getNodeManagerManager();
  }

  AlertManager getAlertManager() {
    return theDaemon.getAlertManager();
  }

  ActivityRegulator getActivityRegulator() {
    return theDaemon.getActivityRegulator(managedAu);
  }

  synchronized Map getActiveNodes() {
    if (activeNodes == null) {
      activeNodes = new HashMap();
    }
    return activeNodes;
  }

  boolean isGlobalNodeCache() {
    return nodeCache == getNodeManagerManager().getGlobalNodeCache();
  }

  public void setNodeStateCacheSize(int size) {
    if (nodeCache != null && !isGlobalNodeCache() &&
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
      for (int ii=0; ii<pollsToRemove.size(); ii++) {
        logger.debug("Dead poll being removed for CUS '" + cus.getUrl() + "'");
      }
    }

    nodeCache.put(nodeCacheKey(cus.getUrl()), state);
    return state;
  }

  Object nodeCacheKey(String canonUrl) {
    if (isGlobalNodeCache()) {
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
    if (isGlobalNodeCache()) {
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

  // Callers should call AuState directly when NodeManager goes.
  public void newContentCrawlFinished() {
    newContentCrawlFinished(Crawler.STATUS_SUCCESSFUL, null);
  }

  // Callers should call AuState directly when NodeManager goes.
  public void newContentCrawlFinished(int result, String msg) {
    // notify and checkpoint the austate (it writes through)
    AuState aus = getAuState();
    if (aus == null) {
      // Can happen in testing
      logger.warning("newContentCrawlFinished with null AU state");
      return;
    }
    aus.newCrawlFinished(result, msg);

    if (result == Crawler.STATUS_SUCCESSFUL) {
      // checkpoint the top-level nodestate
      NodeState topState = getNodeState(managedAu.getAuCachedUrlSet());
      CrawlState crawl = topState.getCrawlState();
      crawl.status = CrawlState.FINISHED;
      crawl.type = CrawlState.NEW_CONTENT_CRAWL;
      crawl.startTime = getAuState().getLastCrawlTime();
      historyRepo.storeNodeState(topState);
    }
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

  public HashMap getLastTopLevelVoteHistory() {
    HashMap voteMap = new HashMap();
    NodeState node = getNodeState(managedAu.getAuCachedUrlSet());
    Iterator history_it = node.getPollHistories();
    return voteMap;
  }

  /**
   * Convenience method to call a top-level poll.
   */
  void callTopLevelPoll() {
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
    return (spec.isAu());
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
            (ActivityRegulator.CusLock)getActivityRegulator().changeAuLockToCusLock(
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
        List lockList = getActivityRegulator().changeAuLockToCusLocks(auLock,
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
					    CrawlerStatus status) {
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
      } else {
        // if we need to release our lock, make sure we do it.
        if(pollCookie.lock != null) {
          pollCookie.lock.expire();
        }
        if (pollCookie.isNamePoll) {
          logger.debug("Calling new name poll...");
          state.setState(NodeState.WRONG_NAMES);
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
