/*
 * $Id: TestTreeWalkThread.java,v 1.1 2003-03-18 01:28:57 aalto Exp $
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

public class TestTreeWalkThread extends LockssTestCase {
  public static final String TEST_URL = "http://www.example.com";
  private static Logger log = Logger.getLogger("TestNMI");
  private String tempDirPath;
  private MockArchivalUnit mau = null;
  private NodeManagerImpl nodeManager;
  private TreeWalkThread treeWalkThread;
  private PollManager pollManager;
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
        "=" + tempDirPath + "\n" + TreeWalkThread.PARAM_TREEWALK_INTERVAL +
        "=100";
    TestConfiguration.setCurrentConfigFromString(s);

    mau = new MockArchivalUnit();
    mau.setAUCachedUrlSet(TestNodeManagerImpl.makeFakeCachedUrlSet(mau,
        TEST_URL, 2, 2));
    theDaemon.getPluginManager();
    PluginUtil.registerArchivalUnit(mau);

    theDaemon.setCrawlManager(new MockCrawlManager());

    pollManager = new MockPollManager();
    theDaemon.setPollManager(pollManager);
    theDaemon.setIdentityManager(new MockIdentityManager());
    theDaemon.useMockLockssService(true);

    nodeManager = new NodeManagerImpl(mau);
    nodeManager.initService(theDaemon);
    nodeManager.historyRepo = new HistoryRepositoryImpl(tempDirPath);
    // don't start nodemanager service because of treewalk thread
    pollManager.initService(theDaemon);

    treeWalkThread = new TreeWalkThread("test thread", nodeManager,
                                        theDaemon.getCrawlManager());

    TestNodeManagerImpl.loadNodeStates(mau, nodeManager);
  }

  public void tearDown() throws Exception {
    PluginUtil.unregisterAllArchivalUnits();
    treeWalkThread.end();
    nodeManager.stopService();
    pollManager.stopService();
    theDaemon.stopDaemon();
    TimeBase.setReal();
    super.tearDown();
  }

  public void testTreeWalkStartNoCrawlNoPoll() throws Exception {
    MockPollManager pollMan = (MockPollManager)theDaemon.getPollManager();
    MockCrawlManager crawlMan = (MockCrawlManager)theDaemon.getCrawlManager();
    pollMan.startService();
    crawlMan.startService();

    //should allow walk to start if last crawl time >= 0
    //should not schedule a top level poll
    crawlMan.setShouldCrawlNewContent(false);
    AuState auState = nodeManager.getAuState();
    auState.lastTopLevelPoll = TimeBase.nowMs();

    treeWalkThread.doTreeWalk();
    assertNull(crawlMan.getAuStatus(mau));
    assertNull(pollMan.getPollStatus(mau.getAUCachedUrlSet().getUrl()));
    pollMan.stopService();
    crawlMan.stopService();
  }

  public void testTreeWalkStartNoCrawlYesPoll() {
    MockPollManager pollMan = (MockPollManager)theDaemon.getPollManager();
    MockCrawlManager crawlMan = (MockCrawlManager)theDaemon.getCrawlManager();
    pollMan.startService();
    crawlMan.startService();

    //should allow walk but schedule top level poll
    AuState auState = nodeManager.getAuState();
    auState.lastTopLevelPoll = -123;
    crawlMan.setShouldCrawlNewContent(false);

    treeWalkThread.doTreeWalk();
    assertNull(crawlMan.getAuStatus(mau));
    assertEquals(MockPollManager.CONTENT_REQUESTED,
		 pollMan.getPollStatus(mau.getAUCachedUrlSet().getUrl()));
  }

  public void testTreeWalkPollBlocking() throws Exception {
    MockPollManager pollMan = (MockPollManager)theDaemon.getPollManager();
    MockCrawlManager crawlMan = (MockCrawlManager)theDaemon.getCrawlManager();
    theDaemon.getHashService().startService();
    pollMan.startService();
    crawlMan.startService();

    // set up with proper Au level CUS
    String auUrl = AuUrl.PROTOCOL_COLON;
    MockCachedUrlSet mcus = (MockCachedUrlSet)TestNodeManagerImpl.getCUS(mau, auUrl);
    mcus.setFlatIterator((new Vector()).iterator());
    mau.setAUCachedUrlSet(mcus);
    theDaemon.getLockssRepository(mau).createNewNode(mcus.getUrl());

    nodeManager = new NodeManagerImpl(mau);
    nodeManager.initService(theDaemon);
    nodeManager.historyRepo = new HistoryRepositoryImpl(tempDirPath);
    treeWalkThread.manager = nodeManager;

    // finish top-level poll
    TimeBase.setSimulated(TimeBase.nowMs());
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
    assertEquals(TimeBase.nowMs(),
                 nodeManager.getAuState().getLastTopLevelPollTime());
    //XXX not testing thread here

    theDaemon.getHashService().stopService();
    pollMan.stopService();
    crawlMan.stopService();
  }

  public void testTreeWalkStartYesCrawl() {
    MockPollManager pollMan = (MockPollManager)theDaemon.getPollManager();
    MockCrawlManager crawlMan = (MockCrawlManager)theDaemon.getCrawlManager();
    pollMan.startService();
    crawlMan.startService();

    //should abort walk and schedule crawl
    crawlMan.setShouldCrawlNewContent(true);
    treeWalkThread.doTreeWalk();
    assertEquals(MockCrawlManager.SCHEDULED, crawlMan.getAuStatus(mau));

    pollMan.stopService();
    crawlMan.stopService();
  }

  /*XXX fix
  public void testTreeWalkThreadStarting() throws Exception {
    MockCrawlManager crawlMan = (MockCrawlManager)theDaemon.getCrawlManager();
    crawlMan.startService();
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
    crawlMan.stopService();
  }
*/

  public void testTreeWalkShouldStartIfNoneHaveRun() {
    assertTrue(treeWalkThread.shouldTreeWalkStart());
  }

  public void testTreeWalkShouldntStartIfOneJustRan() throws IOException {
    String configString = "org.lockss.treewalk.interval=12w";
    TestConfiguration.setCurrentConfigFromString(configString);
    treeWalkThread.doTreeWalk();
    assertFalse(treeWalkThread.shouldTreeWalkStart());
  }

  public void testTreeWalkShouldStartIfIntervalElapsed() throws IOException {
    String configString = "org.lockss.treewalk.interval=100";
    TestConfiguration.setCurrentConfigFromString(configString);
    treeWalkThread.doTreeWalk();

    TimerUtil.guaranteedSleep(100);

    assertTrue(treeWalkThread.shouldTreeWalkStart());
  }

  public void testWalkNodeStateCrawling() throws Exception {
    theDaemon.getPollManager().startService();
    ((MockCrawlManager)theDaemon.getCrawlManager()).startService();

    NodeStateImpl node = (NodeStateImpl)nodeManager.getNodeState(
        TestNodeManagerImpl.getCUS(mau, TEST_URL));

    // with a normal scheduled state, nothing should happen
    testWalkCrawling(CrawlState.NEW_CONTENT_CRAWL, CrawlState.SCHEDULED, 123,
                     false, node);

    // should be ignored if deleted
    testWalkCrawling(CrawlState.NODE_DELETED, -1, 123, false, node);

    // should do nothing if recent crawl (time >= 0)
    testWalkCrawling(CrawlState.NEW_CONTENT_CRAWL, CrawlState.FINISHED, 123,
                     false, node);

    // should schedule background crawl if no recent crawl (time < 0)
    testWalkCrawling(CrawlState.NEW_CONTENT_CRAWL, CrawlState.FINISHED, -123,
                     true, node);

    theDaemon.getPollManager().stopService();
    ((MockCrawlManager)theDaemon.getCrawlManager()).stopService();
  }

  private void testWalkCrawling(int type, int status, long startTime,
                                boolean shouldSchedule, NodeStateImpl node) {
    MockPollManager pollMan = (MockPollManager)theDaemon.getPollManager();
    MockCrawlManager crawlMan = (MockCrawlManager)theDaemon.getCrawlManager();

    CrawlState crawlState = node.getCrawlState();
    crawlState.type = type;
    crawlState.status = status;
    crawlState.startTime = startTime;
    treeWalkThread.walkNodeState(node);
    assertNull(pollMan.getPollStatus(node.getCachedUrlSet().getUrl()));
    if (shouldSchedule) {
      //XXX uncomment when CrawlManager ready
      //assertTrue(crawlMan.getUrlStatus(
      //node.getCachedUrlSet().getUrl())==MockCrawlManager.SCHEDULED);
    } else {
      assertNull(crawlMan.getUrlStatus(TEST_URL));
    }
  }

  public void testWalkNodeStatePolling() throws Exception {
    theDaemon.getPollManager().startService();
    ((MockCrawlManager)theDaemon.getCrawlManager()).startService();
    MockPollManager pollMan = (MockPollManager)theDaemon.getPollManager();
    MockCrawlManager crawlMan = (MockCrawlManager)theDaemon.getCrawlManager();

    NodeStateImpl node = (NodeStateImpl)nodeManager.getNodeState(
        TestNodeManagerImpl.getCUS(mau, TEST_URL));

    // should ignore if active poll
    PollState pollState = new PollState(1, "", "", PollState.RUNNING, 1, null);
    node.addPollState(pollState);
    treeWalkThread.walkNodeState(node);
    // no poll in manager since we just created a PollState
    assertNull(pollMan.getPollStatus(TEST_URL));
    assertNull(crawlMan.getUrlStatus(TEST_URL));

    // should do nothing if last poll not LOST or REPAIRING
    testWalkPolling(PollState.REPAIRED, 123, false, node);
    testWalkPolling(PollState.SCHEDULED, 234, false, node);
    testWalkPolling(PollState.UNREPAIRABLE, 345, false, node);
    testWalkPolling(PollState.WON, 456, false, node);
    testWalkPolling(PollState.ERR_IO, 567, false, node);

    // should schedule name poll if last history is LOST or REPAIRING
    testWalkPolling(PollState.LOST, 678, true, node);
    testWalkPolling(PollState.REPAIRING, 789, true, node);

    pollMan.stopService();
    crawlMan.stopService();
  }

  private void testWalkPolling(int pollState, long startTime,
                               boolean shouldSchedule, NodeStateImpl node) {
    MockPollManager pollMan = (MockPollManager)theDaemon.getPollManager();
    MockCrawlManager crawlMan = (MockCrawlManager)theDaemon.getCrawlManager();

    PollHistory pollHist = new PollHistory(1, "", "", pollState, startTime, 1,
                                           null);
    // doesn't clear old histories, so startTime must be used appropriately
    node.closeActivePoll(pollHist);
    treeWalkThread.walkNodeState(node);
    if (shouldSchedule) {
      assertEquals(pollMan.getPollStatus(node.getCachedUrlSet().getUrl()),
		   MockPollManager.NAME_REQUESTED);
    } else {
      assertNull(pollMan.getPollStatus(node.getCachedUrlSet().getUrl()));
    }
    assertNull(crawlMan.getUrlStatus(node.getCachedUrlSet().getUrl()));
  }

  public void testEstimatedTreeWalk() {
    //XXX fix using simulated time
    long estimate = treeWalkThread.getEstimatedTreeWalkDuration();
    assertTrue(estimate > 0);
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
    Poll p = TestPoll.createCompletedPoll(theDaemon,
                                          testmsg, numAgree, numDisagree);
    TestHistoryRepositoryImpl.configHistoryParams(tempDirPath);
    return p;
  }

  public static void main(String[] argv) {
    String[] testCaseList = {
        TestTreeWalkThread.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
