/*
 * $Id: TestTreeWalkHandler.java,v 1.28 2003-07-17 19:02:44 eaalto Exp $
 */

/*
 Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
import java.net.*;
import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.hasher.HashService;
import org.lockss.repository.*;
import org.lockss.plugin.base.*;

public class TestTreeWalkHandler extends LockssTestCase {
  public static final String TEST_URL = "http://www.example.com";
  private static Logger log = Logger.getLogger("TestNMI");
  private String tempDirPath;
  private MockArchivalUnit mau = null;
  private NodeManagerImpl nodeManager;
  private TreeWalkHandler treeWalkHandler;
  private MockPollManager pollMan;
  private MockCrawlManager crawlMan;
  private List urlList = null;
  private Random random = new Random();

  private MockLockssDaemon theDaemon;

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = new MockLockssDaemon();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props.setProperty(HistoryRepositoryImpl.PARAM_HISTORY_LOCATION,
                      tempDirPath);
    props.setProperty(TreeWalkHandler.PARAM_TREEWALK_INTERVAL, "100");
    props.setProperty(NodeManagerImpl.PARAM_RECALL_DELAY, "5s");
    ConfigurationUtil.setCurrentConfigFromProps(props);

    mau = new MockArchivalUnit();
    mau.setAUCachedUrlSet(TestNodeManagerImpl.makeFakeAuCachedUrlSet(mau,
        TEST_URL, 2, 2));
    theDaemon.getPluginManager();
    PluginUtil.registerArchivalUnit(mau);

    crawlMan = new MockCrawlManager();
    theDaemon.setCrawlManager(crawlMan);
    crawlMan.initService(theDaemon);
    crawlMan.startService();

    pollMan = new MockPollManager();
    theDaemon.setPollManager(pollMan);
    theDaemon.setIdentityManager(new MockIdentityManager());
    pollMan.initService(theDaemon);
    pollMan.startService();

    nodeManager = new NodeManagerImpl(mau);
    nodeManager.initService(theDaemon);
    HistoryRepository historyRepo = new HistoryRepositoryImpl(tempDirPath);
    historyRepo.startService();
    nodeManager.historyRepo = historyRepo;
    theDaemon.setHistoryRepository(historyRepo);

    // can't call 'startService()' since thread can't start
    nodeManager.nodeCache = new NodeStateCache(10);
    nodeManager.activeNodes = new HashMap();
    nodeManager.damagedNodes = new DamagedNodeSet(mau, historyRepo);
    nodeManager.auState = historyRepo.loadAuState(mau);
    nodeManager.pollManager = pollMan;


    treeWalkHandler = new TreeWalkHandler(nodeManager, theDaemon);

    TestNodeManagerImpl.loadNodeStates(mau, nodeManager);
  }

  public void tearDown() throws Exception {
    treeWalkHandler.end();
    nodeManager.stopService();
    pollMan.stopService();
    crawlMan.stopService();
    PluginUtil.unregisterAllArchivalUnits();
    theDaemon.stopDaemon();
    TimeBase.setReal();
    super.tearDown();
  }

  public void testTreeWalkStartNoCrawlNoPoll() throws Exception {
    //should allow walk to start if last crawl time >= 0
    //should not schedule a top level poll
    mau.setShouldCrawlForNewContent(false);
    mau.setShouldCallTopLevelPoll(false);

    treeWalkHandler.doTreeWalk();
    assertNull(crawlMan.getAuStatus(mau));
    assertNull(pollMan.getPollStatus(mau.getAUCachedUrlSet().getUrl()));
  }

  public void testTreeWalkStartNoCrawlYesPoll() {
    //should allow walk but schedule top level poll
    mau.setShouldCrawlForNewContent(false);
    mau.setShouldCallTopLevelPoll(true);

    treeWalkHandler.doTreeWalk();
    assertNull(crawlMan.getAuStatus(mau));
    assertEquals(MockPollManager.CONTENT_REQUESTED,
		 pollMan.getPollStatus(mau.getAUCachedUrlSet().getUrl()));
  }

  public void testTreeWalkSkipTopLevelPoll() {
    TimeBase.setSimulated(10000);
    //should allow walk but schedule top level poll
    mau.setShouldCrawlForNewContent(false);
    mau.setShouldCallTopLevelPoll(true);

    // set up damage in tree
    CachedUrlSet subCus = (CachedUrlSet)
        mau.getAUCachedUrlSet().flatSetIterator().next();
    NodeState node = nodeManager.getNodeState(subCus);
    node.setState(NodeState.CONTENT_LOST);

    // should find damage and schedule
    treeWalkHandler.doTreeWalk();
    assertNull(crawlMan.getAuStatus(mau));
    assertEquals(pollMan.getPollStatus(subCus.getUrl()),
                 MockPollManager.NAME_REQUESTED);
    // no top-level poll run
    assertNull(pollMan.getPollStatus(mau.getAUCachedUrlSet().getUrl()));

    TimeBase.setReal();
  }

  public void testTreeWalkStartYesCrawl() {
    //should abort walk and schedule crawl
    mau.setShouldCrawlForNewContent(true);
    treeWalkHandler.doTreeWalk();
    assertEquals(MockCrawlManager.SCHEDULED, crawlMan.getAuStatus(mau));
  }

  /**
   * After initialized with no treewalk run, the thread should be ready to
   * immediately start a treewalk.
   */
  public void testTreeWalkShouldStartIfNoneHaveRun() {
    assertTrue(treeWalkHandler.timeUntilTreeWalkStart() <= 0);
  }

  public void testTreeWalkShouldntStartIfOneJustRan() throws IOException {
    String configString = "org.lockss.treewalk.interval=10d";
    ConfigurationUtil.setCurrentConfigFromString(configString);
    treeWalkHandler.doTreeWalk();
    assertFalse(treeWalkHandler.timeUntilTreeWalkStart() <= 0);
  }

  public void testTreeWalkShouldStartIfIntervalElapsed() throws IOException {
    String configString = "org.lockss.treewalk.interval=100";
    ConfigurationUtil.setCurrentConfigFromString(configString);
    treeWalkHandler.doTreeWalk();

    TimerUtil.guaranteedSleep(100);

    assertTrue(treeWalkHandler.timeUntilTreeWalkStart() <= 0);
  }

  public void testCheckNodeStateCrawling() throws Exception {
    NodeStateImpl node = (NodeStateImpl)nodeManager.getNodeState(
        TestNodeManagerImpl.getCUS(mau, TEST_URL));

    treeWalkHandler.checkNodeState(node);
    assertNull(pollMan.getPollStatus(node.getCachedUrlSet().getUrl()));
    if (node.cus.hasContent()) {
      //XXX uncomment when CrawlManager ready
      //assertTrue(crawlMan.getUrlStatus(
      //node.getCachedUrlSet().getUrl())==MockCrawlManager.SCHEDULED);
    } else {
      assertNull(crawlMan.getUrlStatus(TEST_URL));
    }
  }

  public void testCheckNodeStatePolling() throws Exception {
    TimeBase.setSimulated(10000);
    NodeStateImpl node = (NodeStateImpl)nodeManager.getNodeState(
        TestNodeManagerImpl.getCUS(mau, TEST_URL));

    // should ignore if active poll
    PollState pollState = new PollState(Poll.NAME_POLL, "", "",
                                        PollState.RUNNING, 1,
                                        Deadline.MAX, true);
    node.addPollState(pollState);
    treeWalkHandler.checkNodeState(node);
    // no poll in manager since we just created a PollState
    assertNull(pollMan.getPollStatus(TEST_URL));
    assertNull(crawlMan.getUrlStatus(TEST_URL));

    // call new content poll
    checkPollTest(NodeState.NEEDS_POLL, 123, true, true, node);
    checkPollTest(NodeState.NEEDS_REPLAY_POLL, 123, true, true, node);

    // behavior from treewalk differs due to no results
    // calls a content poll to clear
    checkPollTest(NodeState.POSSIBLE_DAMAGE_BELOW, 123, true, true, node);

    // just calls name polls
    checkPollTest(NodeState.CONTENT_LOST, 123, true, false, node);
    checkPollTest(NodeState.WRONG_NAMES, 123, true, false, node);

    TimeBase.setReal();
  }

  private void checkPollTest(int nodeState, long startTime,
                             boolean shouldSchedule, boolean isContent,
                             NodeStateImpl node) {
    node.setState(nodeState);
    assertEquals(!shouldSchedule, treeWalkHandler.checkNodeState(node));
    if (shouldSchedule) {
      assertEquals(pollMan.getPollStatus(node.getCachedUrlSet().getUrl()),
		   (isContent ? MockPollManager.CONTENT_REQUESTED :
                    MockPollManager.NAME_REQUESTED));
      pollMan.thePolls.remove(TEST_URL);
    } else {
      assertNull(pollMan.getPollStatus(node.getCachedUrlSet().getUrl()));
    }
    assertNull(crawlMan.getUrlStatus(node.getCachedUrlSet().getUrl()));
  }

  public void testCheckNodeStateRecursion() throws Exception {
    TimeBase.setSimulated(10000);
    CachedUrlSet cus = mau.getAUCachedUrlSet();
    CachedUrlSet subCus = (CachedUrlSet)cus.flatSetIterator().next();
    NodeStateImpl node = (NodeStateImpl)nodeManager.getNodeState(cus);
    NodeStateImpl subNode = (NodeStateImpl)nodeManager.getNodeState(subCus);

    // set parent node to be running a poll
    PollHistory pollHist = new PollHistory(Poll.NAME_POLL, "", "",
                                           PollState.RUNNING, 123, 1,
                                           null, true);
    node.closeActivePoll(pollHist);
    node.setState(NodeState.NAME_RUNNING);

    // get lock to avoid null pointer
    treeWalkHandler.activityLock =
        theDaemon.getActivityRegulator(mau).startAuActivity(
        ActivityRegulator.TREEWALK, 10500);

    // should act on the parent node
    assertFalse(treeWalkHandler.recurseTreeWalk(cus));
    assertEquals(pollMan.getPollStatus(cus.getUrl()),
                 MockPollManager.NAME_REQUESTED);
    assertNull(pollMan.getPollStatus(subCus.getUrl()));
    pollMan.thePolls.remove(cus.getUrl());
    // reset treewalk
    treeWalkHandler.treeWalkAborted = false;

    // set both nodes to be running a poll
    pollHist = new PollHistory(Poll.NAME_POLL, "", "",
                               PollState.RUNNING, 456, 1,
                               null, true);
    node.closeActivePoll(pollHist);
    node.setState(NodeState.NAME_RUNNING);
    subNode.closeActivePoll(pollHist);
    subNode.setState(NodeState.NAME_RUNNING);

    // should act on the sub-node, not the node
    assertFalse(treeWalkHandler.recurseTreeWalk(cus));
    assertNull(pollMan.getPollStatus(cus.getUrl()));
    assertEquals(pollMan.getPollStatus(subCus.getUrl()),
                 MockPollManager.NAME_REQUESTED);

    TimeBase.setReal();
  }

  public void testAverageTreeWalkDuration() {
    assertEquals(-1, treeWalkHandler.getAverageTreeWalkDuration());
    treeWalkHandler.updateEstimate(100);
    assertEquals(100, treeWalkHandler.getAverageTreeWalkDuration());
    treeWalkHandler.updateEstimate(200);
    assertEquals(150, treeWalkHandler.getAverageTreeWalkDuration());
  }

  private Poll createPoll(String url, boolean isContentPoll, int numAgree,
                          int numDisagree) throws Exception {
    LcapIdentity testID = null;
    LcapMessage testmsg = null;
    try {
      InetAddress testAddr = InetAddress.getByName("127.0.0.1");
      testID = theDaemon.getIdentityManager().findIdentity(testAddr);
    }
    catch (UnknownHostException ex) {
      fail("can't open test host");
    }
    byte[] bytes = new byte[20];
    random.nextBytes(bytes);
    try {

      testmsg = LcapMessage.makeRequestMsg(
          new PollSpec(mau.getAUId(),
                       url, "lwr", "upr", null),
          null,
          bytes,
          bytes,
          (isContentPoll ? LcapMessage.CONTENT_POLL_REQ :
           LcapMessage.NAME_POLL_REQ),
          123321,
          testID);
    }
    catch (IOException ex) {
      fail("can't create test name message" + ex.toString());
    }
    log.debug("daemon = " + theDaemon);
    Poll p = TestPoll.createCompletedPoll(theDaemon, mau,
                                          testmsg, numAgree, numDisagree);
    TestHistoryRepositoryImpl.configHistoryParams(tempDirPath);
    return p;
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestTreeWalkHandler.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
