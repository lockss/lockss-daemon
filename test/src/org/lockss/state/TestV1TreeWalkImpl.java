/*
 * $Id: TestV1TreeWalkImpl.java,v 1.9 2007-06-08 21:18:57 smorabito Exp $
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
import java.util.*;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.repository.*;

public class TestV1TreeWalkImpl extends LockssTestCase {
  public static final String TEST_URL = "http://www.example.com";
  private String tempDirPath;
  private MockArchivalUnit mau = null;
  private NodeManagerImpl nodeManager;
  private MyTreeWalkImpl twi;
  private MockPollManager pollMan;
  private MockCrawlManager crawlMan;

  private MockLockssDaemon theDaemon;

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = getMockLockssDaemon();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props.setProperty(HistoryRepositoryImpl.PARAM_HISTORY_LOCATION,
		      tempDirPath);
    props.setProperty(TreeWalkManager.PARAM_TREEWALK_INTERVAL_MIN, "100");
    props.setProperty(TreeWalkManager.PARAM_TREEWALK_INTERVAL_MAX, "100");
    props.setProperty(NodeManagerManager.PARAM_RECALL_DELAY, "5s");
    props.setProperty(AuUtil.PARAM_POLL_PROTOCOL_VERSION, "1");
    ConfigurationUtil.setCurrentConfigFromProps(props);

    mau = new MockArchivalUnit();
    mau.setPlugin(new MockPlugin());
    mau.setAuCachedUrlSet(TestNodeManagerImpl.makeFakeAuCus(mau,
	TEST_URL, 2, 2));
    theDaemon.getPluginManager();
    PluginTestUtil.registerArchivalUnit(mau);

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
    theDaemon.setNodeManager(nodeManager, mau);

    HistoryRepository historyRepo =
      new HistoryRepositoryImpl(mau, tempDirPath);
    theDaemon.setHistoryRepository(historyRepo, mau);

    nodeManager.historyRepo = historyRepo;
    historyRepo.startService();
    nodeManager.startService();

    theDaemon.getActivityRegulator(mau).startService();

    twi = new MyTreeWalkImpl(theDaemon, mau);

    TestNodeManagerImpl.loadNodeStates(mau, nodeManager);
  }

  public void tearDown() throws Exception {
    nodeManager.stopService();
    pollMan.stopService();
    crawlMan.stopService();
    PluginTestUtil.unregisterAllArchivalUnits();
    theDaemon.stopDaemon();
    TimeBase.setReal();
    super.tearDown();
  }

  public void testTreeWalkStartNoCrawlNoPoll() throws Exception {
    //should allow walk to start if last crawl time >= 0
    //should not schedule a top level poll
    mau.setShouldCrawlForNewContent(false);
    mau.setShouldCallTopLevelPoll(false);

    twi.doTreeWalk();
    twi.doDeferredAction();
    assertTrue(twi.didFullTreewalk());
    assertNull(crawlMan.getAuStatus(mau));
    assertNull(pollMan.getPollStatus(mau.getAuCachedUrlSet().getUrl()));
  }

  public void testTreeWalkV1Full() throws Exception {
    ConfigurationUtil.addFromArgs(AuUtil.PARAM_POLL_PROTOCOL_VERSION, "1",
				  TreeWalkManager.PARAM_TREEWALK_V1_MODE,
				  "full");
    //should allow walk to start if last crawl time >= 0
    //should not schedule a top level poll
    mau.setShouldCrawlForNewContent(false);
    mau.setShouldCallTopLevelPoll(false);

    twi.doTreeWalk();
    twi.doDeferredAction();
    assertTrue(twi.didFullTreewalk());
  }

  public void testTreeWalkV1PollOnly() throws Exception {
    ConfigurationUtil.addFromArgs(AuUtil.PARAM_POLL_PROTOCOL_VERSION, "1",
				  TreeWalkManager.PARAM_TREEWALK_V1_MODE,
				  "pollonly");
    //should allow walk to start if last crawl time >= 0
    //should not schedule a top level poll
    mau.setShouldCrawlForNewContent(false);
    mau.setShouldCallTopLevelPoll(false);

    twi.doTreeWalk();
    twi.doDeferredAction();
    assertFalse(twi.didFullTreewalk());
  }

  public void testTreeWalkV1PollOnlyV3() throws Exception {
    ConfigurationUtil.addFromArgs(AuUtil.PARAM_POLL_PROTOCOL_VERSION, "3",
				  TreeWalkManager.PARAM_TREEWALK_V1_MODE,
				  "pollonly");
    //should allow walk to start if last crawl time >= 0
    //should not schedule a top level poll
    mau.setShouldCrawlForNewContent(false);
    mau.setShouldCallTopLevelPoll(false);

    twi.doTreeWalk();
    twi.doDeferredAction();
    assertTrue(twi.didFullTreewalk());
  }

  public void testTreeWalkV3Full() throws Exception {
    ConfigurationUtil.addFromArgs(AuUtil.PARAM_POLL_PROTOCOL_VERSION, "3",
				  TreeWalkManager.PARAM_TREEWALK_V3_MODE,
				  "full");
    //should allow walk to start if last crawl time >= 0
    //should not schedule a top level poll
    mau.setShouldCrawlForNewContent(false);
    mau.setShouldCallTopLevelPoll(false);

    twi.doTreeWalk();
    twi.doDeferredAction();
    assertTrue(twi.didFullTreewalk());
  }

  public void testTreeWalkV3PollOnly() throws Exception {
    ConfigurationUtil.addFromArgs(AuUtil.PARAM_POLL_PROTOCOL_VERSION, "3",
				  TreeWalkManager.PARAM_TREEWALK_V3_MODE,
				  "pollonly");
    //should allow walk to start if last crawl time >= 0
    //should not schedule a top level poll
    mau.setShouldCrawlForNewContent(false);
    mau.setShouldCallTopLevelPoll(false);

    twi.doTreeWalk();
    twi.doDeferredAction();
    assertFalse(twi.didFullTreewalk());
  }

  public void testTreeWalkV3PollOnlyV1() throws Exception {
    ConfigurationUtil.addFromArgs(AuUtil.PARAM_POLL_PROTOCOL_VERSION, "1",
				  TreeWalkManager.PARAM_TREEWALK_V3_MODE,
				  "pollonly");
    //should allow walk to start if last crawl time >= 0
    //should not schedule a top level poll
    mau.setShouldCrawlForNewContent(false);
    mau.setShouldCallTopLevelPoll(false);

    twi.doTreeWalk();
    twi.doDeferredAction();
    assertTrue(twi.didFullTreewalk());
  }

  public void testTreeWalkStartNoCrawlYesPoll() {
    //should allow walk but schedule top level poll
    mau.setShouldCrawlForNewContent(false);
    mau.setShouldCallTopLevelPoll(true);

    twi.doTreeWalk();
    twi.doDeferredAction();
    assertNull(crawlMan.getAuStatus(mau));
    assertEquals(MockPollManager.CONTENT_REQUESTED,
		 pollMan.getPollStatus(mau.getAuCachedUrlSet().getUrl()));
  }

  public void testTreeWalkSkipTopLevelPoll() {
    TimeBase.setSimulated(10000);
    //should allow walk but schedule top level poll
    mau.setShouldCrawlForNewContent(false);
    mau.setShouldCallTopLevelPoll(true);

    // set up damage in tree
    CachedUrlSet subCus = (CachedUrlSet)
	mau.getAuCachedUrlSet().flatSetIterator().next();
    NodeState node = nodeManager.getNodeState(subCus);
    node.setState(NodeState.CONTENT_LOST);

    // should find damage and schedule
    twi.doTreeWalk();
    twi.doDeferredAction();
    assertNull(crawlMan.getAuStatus(mau));
    assertEquals(pollMan.getPollStatus(subCus.getUrl()),
		 MockPollManager.NAME_REQUESTED);
    // no top-level poll run
    assertNull(pollMan.getPollStatus(mau.getAuCachedUrlSet().getUrl()));

    TimeBase.setReal();
  }

  public void testTreeWalkStartYesCrawl() {
    //should abort walk and schedule crawl
    mau.setShouldCrawlForNewContent(true);
    twi.doTreeWalk();
    twi.doDeferredAction();
    assertEquals(MockCrawlManager.SCHEDULED, crawlMan.getAuStatus(mau));
  }

  public void testCheckNodeStateCrawling() throws Exception {
    NodeStateImpl node = (NodeStateImpl)nodeManager.getNodeState(
	TestNodeManagerImpl.getCus(mau, TEST_URL));

    twi.checkNodeState(node);
    twi.doDeferredAction();
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

    // give it a lock to avoid null pointer
    twi.activityLock =
	theDaemon.getActivityRegulator(mau).getAuActivityLock(
	ActivityRegulator.TREEWALK, 123321);

    NodeStateImpl node = (NodeStateImpl)nodeManager.getNodeState(
	TestNodeManagerImpl.getCus(mau, TEST_URL));

    // should ignore if active poll
    PollState pollState = new PollState(Poll.V1_NAME_POLL, "", "",
					PollState.RUNNING, 1,
					Deadline.MAX, true);
    node.addPollState(pollState);
    twi.checkNodeState(node);
    twi.doDeferredAction();
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
    // reset treewalkaborted
    twi.treeWalkAborted = false;
    node.setState(nodeState);
    assertEquals(!shouldSchedule, twi.checkNodeState(node));
    twi.doDeferredAction();
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

    // get lock to avoid null pointer
    twi.activityLock =
	theDaemon.getActivityRegulator(mau).getAuActivityLock(
	ActivityRegulator.TREEWALK, 123321);

    CachedUrlSet cus = mau.getAuCachedUrlSet();
    CachedUrlSet subCus = (CachedUrlSet)cus.flatSetIterator().next();
    NodeStateImpl node = (NodeStateImpl)nodeManager.getNodeState(cus);
    NodeStateImpl subNode = (NodeStateImpl)nodeManager.getNodeState(subCus);

    // set parent node to be running a poll
    PollHistory pollHist = new PollHistory(Poll.V1_NAME_POLL, "", "",
					   PollState.RUNNING, 123, 1,
					   null, true);
    node.closeActivePoll(pollHist);
    node.setState(NodeState.NAME_RUNNING);

    // should act on the parent node
    assertFalse(twi.recurseTreeWalk(cus));
    twi.doDeferredAction();
    assertEquals(pollMan.getPollStatus(cus.getUrl()),
		 MockPollManager.NAME_REQUESTED);
    assertNull(pollMan.getPollStatus(subCus.getUrl()));
    pollMan.thePolls.remove(cus.getUrl());
    // reset treewalk
    twi.activityLock.expire();
    twi.treeWalkAborted = false;

    // get lock to avoid null pointer
    twi.activityLock =
	theDaemon.getActivityRegulator(mau).getAuActivityLock(
	ActivityRegulator.TREEWALK, 123321);

    // set both nodes to be running a poll
    pollHist = new PollHistory(Poll.V1_NAME_POLL, "", "",
			       PollState.RUNNING, 456, 1,
			       null, true);
    node.closeActivePoll(pollHist);
    node.setState(NodeState.NAME_RUNNING);
    subNode.closeActivePoll(pollHist);
    subNode.setState(NodeState.NAME_RUNNING);

    // should act on the sub-node, not the node
    assertFalse(twi.recurseTreeWalk(cus));
    twi.doDeferredAction();
    assertNull(pollMan.getPollStatus(cus.getUrl()));
    assertEquals(pollMan.getPollStatus(subCus.getUrl()),
		 MockPollManager.NAME_REQUESTED);

    TimeBase.setReal();
  }

  class MyTreeWalkImpl extends V1TreeWalkImpl {
    public MyTreeWalkImpl(LockssDaemon daemon, ArchivalUnit au) {
      super(daemon, au);
    }

    public boolean doTreeWalk() {
      return doTreeWalk(Deadline.in(10000000));
    }
  }
}
