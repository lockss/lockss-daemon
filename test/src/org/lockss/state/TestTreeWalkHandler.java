/*
 * $Id: TestTreeWalkHandler.java,v 1.2 2003-03-27 21:47:17 troberts Exp $
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
import org.lockss.repository.LockssRepositoryServiceImpl;
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
  private Poll namePoll = null;
  private Poll contentPoll = null;
  private Random random = new Random();

  private MockLockssDaemon theDaemon;

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = new MockLockssDaemon();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    String s = LockssRepositoryServiceImpl.PARAM_CACHE_LOCATION + "=" +
        tempDirPath + "\n" + HistoryRepositoryImpl.PARAM_HISTORY_LOCATION +
        "=" + tempDirPath + "\n" + TreeWalkHandler.PARAM_TREEWALK_INTERVAL +
        "=100";
    TestConfiguration.setCurrentConfigFromString(s);

    mau = new MockArchivalUnit();
    mau.setAUCachedUrlSet(TestNodeManagerImpl.makeFakeCachedUrlSet(mau,
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
    theDaemon.setLockssRepositoryService(new MockLockssRepositoryService());
    pollMan.initService(theDaemon);
    pollMan.startService();

    nodeManager = new NodeManagerImpl(mau);
    nodeManager.initService(theDaemon);
    HistoryRepository historyRepo = new HistoryRepositoryImpl(tempDirPath);
    nodeManager.historyRepo = historyRepo;
    theDaemon.setHistoryRepository(historyRepo);

    // can't call 'startService()' since thread can't start
    nodeManager.nodeMap = new NodeStateMap(historyRepo, 10);

    treeWalkHandler = new TreeWalkHandler(nodeManager,
                                         theDaemon.getCrawlManager());

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
    crawlMan.setShouldCrawlNewContent(false);
    mau.setShouldCallTopLevelPoll(false);

    treeWalkHandler.doTreeWalk();
    assertNull(crawlMan.getAuStatus(mau));
    assertNull(pollMan.getPollStatus(mau.getAUCachedUrlSet().getUrl()));
  }

  public void testTreeWalkStartNoCrawlYesPoll() {
    //should allow walk but schedule top level poll
    mau.setShouldCallTopLevelPoll(true);
    crawlMan.setShouldCrawlNewContent(false);

    treeWalkHandler.doTreeWalk();
    assertNull(crawlMan.getAuStatus(mau));
    assertEquals(MockPollManager.CONTENT_REQUESTED,
		 pollMan.getPollStatus(mau.getAUCachedUrlSet().getUrl()));
  }

  public void testTreeWalkStartYesCrawl() {
    //should abort walk and schedule crawl
    crawlMan.setShouldCrawlNewContent(true);
    treeWalkHandler.doTreeWalk();
    assertEquals(MockCrawlManager.SCHEDULED, crawlMan.getAuStatus(mau));
  }

  /*XXX fix
  public void testTreeWalkPollBlocking() throws Exception {
    theDaemon.getHashService().startService();

    // set up with proper Au level CUS
    String auUrl = AuUrl.PROTOCOL_COLON;
    MockCachedUrlSet mcus = (MockCachedUrlSet)TestNodeManagerImpl.getCUS(mau, auUrl);
    mcus.setFlatIterator((new Vector()).iterator());
    mau.setAUCachedUrlSet(mcus);
    theDaemon.getLockssRepository(mau).createNewNode(mcus.getUrl());

    // finish top-level poll
    TimeBase.setSimulated(123);
    contentPoll = createPoll(auUrl, true, 10, 5);
    Poll.VoteTally results = contentPoll.getVoteTally();
    PollSpec spec = results.getPollSpec();
    NodeStateImpl nodeState = (NodeStateImpl)nodeManager.getNodeState(mcus);
    PollState pollState = new PollState(results.getType(),
                                        spec.getLwrBound(),
                                        spec.getUprBound(),
                                        PollState.RUNNING,
                                        results.getStartTime(),
                                        null);
    nodeState.addPollState(pollState);
    nodeManager.updateState(nodeState, results);
    assertEquals(123, nodeManager.getAuState().getLastTopLevelPollTime());
    //XXX not testing thread here

    theDaemon.getHashService().stopService();
  }


  /*XXX fix
  public void testTreeWalkThreadStarting() throws Exception {
    crawlMan.setShouldCrawlNewContent(true);

    AuState auState = nodeManager.getAuState();

    // should start thread, but thread should sleep
    auState.lastTreeWalk = TimeBase.nowMs();
    treeWalkThread.run();
    assertFalse(treeWalkThread.treeWalkRunning);
    treeWalkThread.end();

    crawlMan.setShouldCrawlNewContent(false);
    // the thread should trigger a treewalk, and the treewalk should
    // trigger a crawl scheduled
    auState.lastTreeWalk = -123;

    treeWalkThread.run();
    assertTrue(crawlMan.getAuStatus(mau)==MockCrawlManager.SCHEDULED);
  }
*/

  /**
   * After initialized with no treewalk run, the thread should be ready to
   * immediately start a treewalk.
   */
  public void testTreeWalkShouldStartIfNoneHaveRun() {
    assertTrue(treeWalkHandler.timeUntilTreeWalkStart() <= 0);
  }

  public void testTreeWalkShouldntStartIfOneJustRan() throws IOException {
    String configString = "org.lockss.treewalk.interval=12w";
    TestConfiguration.setCurrentConfigFromString(configString);
    treeWalkHandler.doTreeWalk();
    assertFalse(treeWalkHandler.timeUntilTreeWalkStart() <= 0);
  }

  public void testTreeWalkShouldStartIfIntervalElapsed() throws IOException {
    String configString = "org.lockss.treewalk.interval=100";
    TestConfiguration.setCurrentConfigFromString(configString);
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
    NodeStateImpl node = (NodeStateImpl)nodeManager.getNodeState(
        TestNodeManagerImpl.getCUS(mau, TEST_URL));

    // should ignore if active poll
    PollState pollState = new PollState(1, "", "", PollState.RUNNING, 1, null);
    node.addPollState(pollState);
    treeWalkHandler.checkNodeState(node);
    // no poll in manager since we just created a PollState
    assertNull(pollMan.getPollStatus(TEST_URL));
    assertNull(crawlMan.getUrlStatus(TEST_URL));

    // should do nothing if last poll not LOST or REPAIRING
    checkPollingTest(PollState.REPAIRED, 123, false, node);
    checkPollingTest(PollState.SCHEDULED, 234, false, node);
    checkPollingTest(PollState.UNREPAIRABLE, 345, false, node);
    checkPollingTest(PollState.WON, 456, false, node);
    checkPollingTest(PollState.ERR_IO, 567, false, node);

    // should schedule name poll if last history is LOST or REPAIRING
    checkPollingTest(PollState.LOST, 678, true, node);
    checkPollingTest(PollState.REPAIRING, 789, true, node);
  }

  private void checkPollingTest(int pollState, long startTime,
                               boolean shouldSchedule, NodeStateImpl node) {
    PollHistory pollHist = new PollHistory(1, "", "", pollState, startTime, 1,
                                           null);
    // doesn't clear old histories, so startTime must be used appropriately
    node.closeActivePoll(pollHist);
    treeWalkHandler.checkNodeState(node);
    if (shouldSchedule) {
      assertEquals(pollMan.getPollStatus(node.getCachedUrlSet().getUrl()),
		   MockPollManager.NAME_REQUESTED);
    } else {
      assertNull(pollMan.getPollStatus(node.getCachedUrlSet().getUrl()));
    }
    assertNull(crawlMan.getUrlStatus(node.getCachedUrlSet().getUrl()));
  }
/*
  public void testEstimatedTreeWalk() {
    //XXX fix using simulated time
    TreeWalkHandler.EstimationThread estThread = treeWalkHandler.getTestThread();
    estThread.run();
    long estimate = treeWalkHandler.getEstimatedTreeWalkDuration();
    assertTrue(estimate > 0);
    long newEstimate = 100;
    treeWalkHandler.updateEstimate(newEstimate);
    long expectedEst = (estimate + newEstimate) / 2;
    assertEquals(expectedEst, treeWalkHandler.getEstimatedTreeWalkDuration());
  }
*/
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
          new PollSpec(mau.getPluginId(),
                       mau.getAUId(),
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
