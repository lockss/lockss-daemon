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
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.repository.*;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestNodeManagerImpl extends LockssTestCase {
  public static final String TEST_URL = "http://www.example.com";
  private String tempDirPath;
  private MockArchivalUnit mau = null;
  private NodeManagerImpl nodeManager;
  private MockCrawlManager crawlManager;
  private HistoryRepository historyRepo;
  private Random random = new Random();
  private MockLockssDaemon theDaemon;
  static Logger log = Logger.getLogger("TestNodeManagerImpl");

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = getMockLockssDaemon();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties p = new Properties();
    p.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    p.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    p.setProperty(HistoryRepositoryImpl.PARAM_HISTORY_LOCATION, tempDirPath);
    p.setProperty(NodeManagerManager.PARAM_MAX_PER_AU_CACHE_SIZE, "10");
    p.setProperty(NodeManagerManager.PARAM_RECALL_DELAY, "5s");
    p.setProperty(AuUtil.PARAM_POLL_PROTOCOL_VERSION, "1");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    mau = new MockArchivalUnit();
    mau.setPlugin(new MockPlugin());
    mau.setAuCachedUrlSet(makeFakeCus(mau, TEST_URL, 2, 2));
    theDaemon.getNodeManagerManager();
    theDaemon.getPluginManager();
    PluginTestUtil.registerArchivalUnit(mau);
    crawlManager = new MockCrawlManager();
    theDaemon.setCrawlManager(crawlManager);
    log.debug("Starting the Identity Manager");
    log.debug("Identity Manager started");
    // create au state so thread doesn't throw null pointers
    theDaemon.getLockssRepository(mau).createNewNode(TEST_URL);
    nodeManager = new NodeManagerImpl(mau);
    nodeManager.initService(theDaemon);
    historyRepo = HistoryRepositoryImpl.createNewHistoryRepository(mau);
    theDaemon.setHistoryRepository(historyRepo, mau);
    nodeManager.historyRepo = historyRepo;
    historyRepo.startService();
    nodeManager.startService();
    theDaemon.getHashService().startService();
    loadNodeStates(mau, nodeManager);
  }

  public void tearDown() throws Exception {
    nodeManager.stopService();
    historyRepo.stopService();
    theDaemon.getLockssRepository(mau).stopService();
    theDaemon.getHashService().stopService();
    PluginTestUtil.unregisterAllArchivalUnits();
    theDaemon.stopDaemon();
    TimeBase.setReal();
    super.tearDown();
  }

  public void testGetNodeState() throws Exception {
    CachedUrlSet cus = getCus(mau, TEST_URL);
    NodeState node = nodeManager.getNodeState(cus);
    assertNotNull(node);
    assertEquals(cus, node.getCachedUrlSet());
    assertNotNull(node.getCrawlState());
    assertEquals(-1, node.getCrawlState().getType());
    assertEquals(CrawlState.FINISHED, node.getCrawlState().getStatus());
    NodeState node2 = nodeManager.getNodeState(cus);
    assertSame(node, node2);
    cus = getCus(mau, TEST_URL + "/branch1");
    node = nodeManager.getNodeState(cus);
    assertNotNull(node);
    assertEquals(cus, node.getCachedUrlSet());
    // null, since not in repository
    cus = getCus(mau, TEST_URL + "/branch3");
    node = nodeManager.getNodeState(cus);
    assertNull(node);
    // should find it now that it's added
    theDaemon.getLockssRepository(mau).createNewNode(TEST_URL + "/branch3");
    node = nodeManager.getNodeState(cus);
    assertNotNull(node);
  }

  public void testLoadingFromHistoryRepository() throws Exception {
    CachedUrlSet cus = getCus(mau, TEST_URL + "/branch3");
    // add to lockss repository
    theDaemon.getLockssRepository(mau).createNewNode(TEST_URL + "/branch3");
    // store in history repository
    NodeState node = new NodeStateImpl(cus, 123, new CrawlState(-1, -1, -1),
        new ArrayList(), historyRepo);
    historyRepo.storeNodeState(node);
    // should load correctly
    node = nodeManager.getNodeState(cus);
    assertNotNull(node);
    assertEquals(TEST_URL + "/branch3", node.getCachedUrlSet().getUrl());
    assertEquals(123, node.getAverageHashDuration());
  }

  public void testDeadPollRemoval() throws Exception {
    CachedUrlSet cus = getCus(mau, TEST_URL + "/branch3");
    // add to lockss repository
    theDaemon.getLockssRepository(mau).createNewNode(TEST_URL + "/branch3");
    // store in history repository
    ArrayList polls = new ArrayList(1);
    // start the poll
    NodeStateImpl node = new NodeStateImpl(cus, 123,
        new CrawlState(-1, -1, -1), polls, historyRepo);
    historyRepo.storeNodeState(node);
    // should keep poll active
    node = (NodeStateImpl) nodeManager.getNodeState(cus);
    assertEquals(1, node.polls.size());
    assertFalse(node.getPollHistories().hasNext());
    cus = getCus(mau, TEST_URL + "/branch4");
    // add to lockss repository
    theDaemon.getLockssRepository(mau).createNewNode(TEST_URL + "/branch4");
    // store in history repository
    polls = new ArrayList(1);
    // don't start the poll
    node = new NodeStateImpl(cus, 123, new CrawlState(-1, -1, -1), polls,
        historyRepo);
    historyRepo.storeNodeState(node);
    // should transfer poll to history
    node = (NodeStateImpl) nodeManager.getNodeState(cus);
    assertNotNull(node);
    assertEquals(TEST_URL + "/branch4", node.getCachedUrlSet().getUrl());
    assertEquals(123, node.getAverageHashDuration());
    assertEquals(0, node.polls.size());
    assertEquals(1, node.pollHistories.size());
  }

  public void testGetAuState() {
    AuState auState = nodeManager.getAuState();
    assertEquals(mau.getAuId(), auState.getArchivalUnit().getAuId());
  }

  public void testMapErrorCodes() {
  }

  public void testShouldStartPoll() throws Exception {
    TimeBase.setSimulated(10000);
    // don't need crawl
    mau.setShouldCrawlForNewContent(false);
    // let's generate some history
    Vector subFiles = new Vector(2);
    subFiles.add(getCus(mau, TEST_URL));
    // if we have a damaged leaf, we return false
    String leaf = TEST_URL + "file1.txt";
    mau.setShouldCrawlForNewContent(true);
    TimeBase.setReal();
  }

  public void testStartPoll() throws Exception {
    // let's generate some history
    Vector subFiles = new Vector(2);
    subFiles.add(getCus(mau, TEST_URL));
  }

  public void testNonLocalStartPollStates() throws Exception {
    // let's generate some history
    Vector subFiles = new Vector(2);
    subFiles.add(getCus(mau, TEST_URL));
  }

  public void testHandleStandardContentPoll() throws Exception {
    NodeState nodeState = nodeManager.getNodeState(getCus(mau, TEST_URL));
    // won content poll
    // - running
    nodeState.setState(NodeState.CONTENT_RUNNING);
    assertEquals(NodeState.OK, nodeState.getState());
    // - repairing
    nodeState.setState(NodeState.CONTENT_REPLAYING);
    assertEquals(NodeState.OK, nodeState.getState());
    // - running
    nodeState.setState(NodeState.CONTENT_RUNNING);
    assertEquals(NodeState.CONTENT_LOST, nodeState.getState());
    // - repairing
    nodeState.setState(NodeState.CONTENT_REPLAYING);
    assertEquals(NodeState.DAMAGE_AT_OR_BELOW, nodeState.getState());
  }

  public void testHandleSingleNodeContentPoll() throws Exception {
    NodeState nodeState = nodeManager.getNodeState(getCus(mau, TEST_URL));
    // - running
  }

  public void testHandleNamePoll() throws Exception {
    NodeState nodeState = nodeManager.getNodeState(getCus(mau, TEST_URL));
  }

  public void testHandleWrongNames() throws Exception {
    Vector masterV = new Vector();
    List localL = new ArrayList();
    String sharedUrl = "/test";
    String repairUrl = "/test2";
    String repairUrl2 = "/test3";
    String createUrl = "/test4";
    String sharedUrl2 = "/test5";
    String deleteUrl = "/test6";
    // create node to be deleted
    RepositoryNode delNode = TestRepositoryNodeImpl.createLeaf(theDaemon
        .getLockssRepository(mau), TEST_URL + deleteUrl, "test stream", null);
    assertFalse(delNode.isDeleted());
    assertNull(theDaemon.getLockssRepository(mau).getNode(TEST_URL + createUrl));
    // make sure repair list is empty
    assertTrue(nodeManager.damagedNodes.getNodesToRepair().isEmpty());
    // handle names
    // first repair scheduled
    assertEquals(MockCrawlManager.SCHEDULED, crawlManager.getUrlStatus(TEST_URL
        + repairUrl));
    // second repair scheduled
    assertEquals(MockCrawlManager.SCHEDULED, crawlManager.getUrlStatus(TEST_URL
        + repairUrl2));
    // check that both registered for repair
    assertIsomorphic(
        ListUtil.list(TEST_URL + repairUrl, TEST_URL + repairUrl2),
        (Collection) nodeManager.damagedNodes.getNodesToRepair().get(TEST_URL));
    // node created
    assertNotNull(theDaemon.getLockssRepository(mau).getNode(
        TEST_URL + createUrl));
    // node deleted
    assertTrue(delNode.isDeleted());
  }

  public void testHandleSNNamePollErrorNoContent() throws Exception {
    CachedUrlSet cus = getCus(mau, TEST_URL);
    NodeState nodeState = nodeManager.getNodeState(cus);
    // XXX what state should this node be in?
    log.debug("5 node state " + nodeState.getStateString());
    assertEquals(NodeState.INITIAL, nodeState.getState());
    // shouldn't call SNCUSS with no content
    log.debug("6 node state " + nodeState.getStateString());
    assertEquals(NodeState.POSSIBLE_DAMAGE_BELOW, nodeState.getState());
  }

  public void testHandleSNNamePollErrorContent() throws Exception {
    CachedUrlSet cus = getCus(mau, TEST_URL);
    NodeState nodeState = nodeManager.getNodeState(cus);
    // set content
    ((MockCachedUrlSet) nodeState.getCachedUrlSet()).setHasContent(true);
    // should call SNCUSS
    assertEquals(NodeState.POSSIBLE_DAMAGE_HERE, nodeState.getState());
  }

  public void testRepairOnStart() throws Exception {
    String url1 = TEST_URL + "test1";
    String url2 = TEST_URL + "test2";
    HashMap repairNodes = nodeManager.damagedNodes.getNodesToRepair();
    // repair list empty
    assertTrue(repairNodes.isEmpty());
    // nothing scheduled
    assertNull(crawlManager.getUrlStatus(url1));
    assertNull(crawlManager.getUrlStatus(url2));
    // populate list
    repairNodes.put(TEST_URL, ListUtil.list(url1, url2));
    nodeManager.markNodesForRepair(ListUtil.list(url1, url2), null,
        new MockCachedUrlSet(TEST_URL), true, null);
    // first repair scheduled
    assertEquals(MockCrawlManager.SCHEDULED, crawlManager.getUrlStatus(url1));
    // second repair scheduled
    assertEquals(MockCrawlManager.SCHEDULED, crawlManager.getUrlStatus(url2));
  }

  public void testRepairAtParentClearsChildren() throws Exception {
    String url1 = TEST_URL + "/branch1/file1.doc";
    String url2 = TEST_URL + "/branch2/file1.doc";
    CachedUrlSet cus = getCus(mau, TEST_URL);
    DamagedNodeSet damagedNodes = nodeManager.damagedNodes;
    // populate the damagedNodes
    damagedNodes.addToDamage(url1);
    damagedNodes.addToDamage(url2);
    damagedNodes.addToDamage(TEST_URL);
    assertTrue(damagedNodes.containsWithDamage(url1));
    assertTrue(damagedNodes.containsWithDamage(url2));
    assertTrue(damagedNodes.containsWithDamage(cus.getUrl()));
    // clear our parent
    damagedNodes.clearDamage(cus);
    assertFalse(damagedNodes.containsWithDamage(url1));
    assertFalse(damagedNodes.containsWithDamage(url2));
    assertFalse(damagedNodes.containsWithDamage(cus.getUrl()));
  }

  public void testHandleAuPolls() throws Exception {
    // have to change the AUCUS for the MockArchivalUnit here
    // to properly test handling AuNode content polls
    MockCachedUrlSet mcus = (MockCachedUrlSet) getCus(mau,
        new AuCachedUrlSetSpec());
    Vector auChildren = new Vector();
    mcus.setFlatItSource(auChildren);
    mau.setAuCachedUrlSet(mcus);
    nodeManager.createNodeState(mcus);
    // test with an inconclusive poll
    AuState auState = nodeManager.getAuState();
    assertEquals(-1, auState.getLastTopLevelPollTime());
    NodeStateImpl nodeState = (NodeStateImpl) nodeManager.getNodeState(mcus);
    // test that error polls don't update last poll time
    assertEquals(-1, auState.getLastTopLevelPollTime());
    // test that a finished top-level poll sets the time right
    TimeBase.setSimulated(TimeBase.nowMs());
    nodeState = (NodeStateImpl) nodeManager.getNodeState(mcus);
    assertEquals(TimeBase.nowMs(), auState.getLastTopLevelPollTime());
    // add some fake children
    auChildren.add(getCus(mau, "testDir1"));
    auChildren.add(getCus(mau, "testDir2"));
    mcus.setFlatItSource(auChildren);
    // test that won name poll calls content polls on Au children
    Vector subFiles = new Vector(2);
    subFiles.add(getCus(mau, "testDir1"));
    subFiles.add(getCus(mau, "testDir2"));
    nodeState.setState(NodeState.NAME_RUNNING);
    assertEquals(TimeBase.nowMs(), auState.getLastTopLevelPollTime());
  }

  public void testUpdateInconclusivePolls() throws Exception {
    NodeStateImpl nodeState = (NodeStateImpl) nodeManager.getNodeState(getCus(
        mau, TEST_URL));
    // inconclusive poll
    nodeState.setState(NodeState.NAME_RUNNING);
    assertEquals(NodeState.CONTENT_LOST, nodeState.getState());
    // no quorum
    nodeState.setState(NodeState.CONTENT_RUNNING);
    assertEquals(NodeState.NEEDS_POLL, nodeState.getState());
  }

  public void testCheckLastHistory() throws Exception {
    TimeBase.setSimulated(10000);
    NodeState nodeState = nodeManager.getNodeState(getCus(mau, TEST_URL));
    // these are true if the pollmanager doesn't know about them
    // shouldn't do anything to this poll since it's expiration date isn't up
    // (created with starttime 123 and duration 123)
    TimeBase.setSimulated(150);
    TimeBase.setReal();
  }

  public void testCheckCurrentState() throws Exception {
    TimeBase.setSimulated(10000);
    NodeStateImpl nodeState = (NodeStateImpl) nodeManager.getNodeState(getCus(
        mau, TEST_URL));
    assertEquals(NodeState.POSSIBLE_DAMAGE_HERE, nodeState.getState());
    TimeBase.setReal();
  }

  private void stateCheckTest(NodeState node, int pollType,
                              boolean shouldScheduleName,
                              boolean shouldScheduleContent,
                              boolean shouldRepair, boolean isHistory,
                              boolean isOurPoll) throws Exception {
    if (shouldScheduleName || shouldScheduleContent || shouldRepair) {
      if (shouldScheduleName) {
      } else if (shouldScheduleContent) {
      } else if (shouldRepair) {
        if (node.getState() == NodeState.NEEDS_REPAIR) {
          assertEquals(MockCrawlManager.SCHEDULED, crawlManager
              .getUrlStatus(TEST_URL));
        } else if (node.getState() == NodeState.WRONG_NAMES) {
          String repairUrl = TEST_URL + "/testentry4.html";
          String repairUrl2 = TEST_URL + "/testentry5.html";
          assertEquals(MockCrawlManager.SCHEDULED, crawlManager
              .getUrlStatus(repairUrl));
          assertNull(crawlManager.getUrlStatus(repairUrl2));
        }
      }
    }
  }

  private void historyCheckTestName(int pollState, NodeState node,
                                    boolean shouldSchedule, boolean dontRemove) {
  }

  private void historyCheckTest(int pollType, int pollState, NodeState node,
                                boolean shouldSchedule) {
    historyCheckTestName(pollType, pollState, node, true, shouldSchedule, false);
  }

  private void historyCheckTestName(int pollType, int pollState,
                                    NodeState node, boolean ourPoll,
                                    boolean shouldSchedule, boolean dontRemove) {
  }

  static CachedUrlSet getCus(MockArchivalUnit mau, String url) {
    return new MockCachedUrlSet(mau, new RangeCachedUrlSetSpec(url));
  }

  static CachedUrlSet getCus(MockArchivalUnit mau, CachedUrlSetSpec spec) {
    return new MockCachedUrlSet(mau, spec);
  }

  static CachedUrlSet makeFakeCus(MockArchivalUnit mau, String startUrl,
                                  int numBranches, int numFiles)
      throws Exception {
    Vector files = new Vector(numFiles * numBranches);
    Vector branches = new Vector(numBranches);
    for (int ib = 1; ib <= numBranches; ib++) {
      String b_url = TEST_URL + "/branch" + ib;
      MockCachedUrlSet mcus = new MockCachedUrlSet(mau,
          new RangeCachedUrlSetSpec(b_url));
      Vector subFiles = new Vector(numFiles);
      Vector subBranches = new Vector(numFiles);
      for (int ix = 1; ix <= numFiles; ix++) {
        String f_url = b_url + "/file" + ix + ".doc";
        MockCachedUrl cu = new MockCachedUrl(f_url);
        files.add(cu);
        subFiles.add(cu);
        MockCachedUrlSet subMcus = new MockCachedUrlSet(mau,
            new RangeCachedUrlSetSpec(f_url));
        subBranches.add(subMcus);
        //         subMcus.addUrl("test string" + ix, f_url);
        mau.addUrl(f_url);
        subMcus.setFlatItSource(new ArrayList());
        subMcus.setHashItSource(new ArrayList());
      }
      //       mcus.addUrl("test string" + ib, b_url);
      mau.addUrl(b_url);
      mcus.setFlatItSource(subBranches);
      mcus.setHashItSource(subFiles);
      branches.add(mcus);
    }
    MockCachedUrlSet cus = new MockCachedUrlSet(mau, new RangeCachedUrlSetSpec(
        startUrl));
    //     cus.addUrl("test string", startUrl);
    mau.addUrl(startUrl);
    cus.setHashItSource(files);
    cus.setFlatItSource(branches);
    return cus;
  }

  static CachedUrlSet makeFakeAuCus(MockArchivalUnit mau, String startUrl,
                                    int numBranches, int numFiles)
      throws Exception {
    MockCachedUrlSet auCus = new MockCachedUrlSet(mau, new AuCachedUrlSetSpec());
    CachedUrlSet mcus = makeFakeCus(mau, startUrl, numBranches, numFiles);
    Vector childV = new Vector(1);
    childV.addElement(mcus);
    Vector treeV = new Vector(numBranches * numFiles + 1);
    Iterator treeIt = mcus.contentHashIterator();
    treeV.addElement(mcus);
    while (treeIt.hasNext()) {
      treeV.addElement(treeIt.next());
    }
    mau.addUrl(AuUrl.PROTOCOL_COLON, false, false, null);
    auCus.setHashItSource(treeV);
    auCus.setFlatItSource(childV);
    return auCus;
  }

  static void loadNodeStates(ArchivalUnit au, NodeManagerImpl nodeManager) {
    // recurse through au cachedurlsets
    CachedUrlSet cus = au.getAuCachedUrlSet();
    recurseLoadCachedUrlSets(nodeManager, cus, au);
  }

  static void recurseLoadCachedUrlSets(NodeManagerImpl nodeManager,
                                       CachedUrlSet cus, ArchivalUnit au) {
    // add the nodeState for this cus
    nodeManager.createNodeState(cus);
    // recurse the set's children
    Iterator children = cus.flatSetIterator();
    while (children.hasNext()) {
      CachedUrlSetNode child = (CachedUrlSetNode) children.next();
      switch (child.getType()) {
      case CachedUrlSetNode.TYPE_CACHED_URL_SET:
        recurseLoadCachedUrlSets(nodeManager, (CachedUrlSet) child, au);
        break;
      case CachedUrlSetNode.TYPE_CACHED_URL:
        CachedUrlSetSpec rSpec = new RangeCachedUrlSetSpec(child.getUrl());
        CachedUrlSet newSet = au.makeCachedUrlSet(rSpec);
        nodeManager.createNodeState(newSet);
      }
    }
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestNodeManagerImpl.class.getName() };
    junit.swingui.TestRunner.main(testCaseList);
  }
//     /**
//      * Change the the reputation of the peer
//      * @param peer the PeerIdentity
//      * @param reputation the new reputation
//      */
//     public void setReputation(PeerIdentity peer, int reputation) {
//       try {
// 	LcapIdentity lid = findLcapIdentity(peer, peer.getIdString());
// 	lid.changeReputation(reputation - lid.getReputation());
//       } catch (IdentityManager.MalformedIdentityKeyException e) {
// 	throw new RuntimeException(e.toString());
//       }
//     }
}
