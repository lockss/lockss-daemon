/*
 * $Id: TestNodeManagerImpl.java,v 1.79 2003-05-08 01:19:41 claire Exp $
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

public class TestNodeManagerImpl
    extends LockssTestCase {
  public static final String TEST_URL = "http://www.example.com";
  private static Logger log = Logger.getLogger("TestNMI");
  private String tempDirPath;
  private MockArchivalUnit mau = null;
  private NodeManagerImpl nodeManager;
  private PollManager pollManager;
  private HistoryRepository historyRepo;
  private List urlList = null;
  private Poll namePoll = null;
  private Poll contentPoll = null;
  private Random random = new Random();

  private MockLockssDaemon theDaemon;
  MockIdentityManager idManager;

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = new MockLockssDaemon();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties p = new Properties();
    p.setProperty(LockssRepositoryServiceImpl.PARAM_CACHE_LOCATION, tempDirPath);
    p.setProperty(HistoryRepositoryImpl.PARAM_HISTORY_LOCATION, tempDirPath);
    p.setProperty(NodeManagerImpl.PARAM_NODESTATE_CACHE_SIZE, "10");
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.1.2.3");
    p.setProperty(NodeManagerImpl.PARAM_RECALL_DELAY, "5s");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    mau = new MockArchivalUnit();
    mau.setAUCachedUrlSet(makeFakeCachedUrlSet(mau, TEST_URL, 2, 2));
    theDaemon.getPluginManager();
    PluginUtil.registerArchivalUnit(mau);

    theDaemon.setCrawlManager(new MockCrawlManager());

    pollManager = new MockPollManager();
    theDaemon.setPollManager(pollManager);
    idManager = new MockIdentityManager();
    idManager.initService(theDaemon);
    theDaemon.setIdentityManager(idManager);
//    theDaemon.setLockssRepositoryService(new MockLockssRepositoryService());
    pollManager.initService(theDaemon);
    pollManager.startService();

    // create au state so thread doesn't throw null pointers
    theDaemon.getLockssRepository(mau).createNewNode(TEST_URL);

    nodeManager = new NodeManagerImpl(mau);
    nodeManager.initService(theDaemon);
    historyRepo = new HistoryRepositoryImpl(tempDirPath);
    historyRepo.startService();
    theDaemon.setHistoryRepository(historyRepo);
    nodeManager.historyRepo = historyRepo;
    nodeManager.startService();
    // don't need the thread
    nodeManager.killTreeWalk();

    theDaemon.getHashService().startService();

    loadNodeStates(mau, nodeManager);
  }

  public void tearDown() throws Exception {
    nodeManager.stopService();
    pollManager.stopService();
    theDaemon.getHistoryRepository().stopService();
    theDaemon.getHashService().stopService();
    PluginUtil.unregisterAllArchivalUnits();
    theDaemon.stopDaemon();
    TimeBase.setReal();
    super.tearDown();
  }

  public void testGetNodeState() throws Exception {
    CachedUrlSet cus = getCUS(mau, TEST_URL);
    NodeState node = nodeManager.getNodeState(cus);
    assertNotNull(node);
    assertEquals(cus, node.getCachedUrlSet());
    assertNotNull(node.getCrawlState());
    assertEquals(-1, node.getCrawlState().getType());
    assertEquals(CrawlState.FINISHED, node.getCrawlState().getStatus());

    NodeState node2 = nodeManager.getNodeState(cus);
    assertSame(node, node2);

    cus = getCUS(mau, TEST_URL + "/branch1");
    node = nodeManager.getNodeState(cus);
    assertNotNull(node);
    assertEquals(cus, node.getCachedUrlSet());

    // null, since not in repository
    cus = getCUS(mau, TEST_URL + "/branch3");
    node = nodeManager.getNodeState(cus);
    assertNull(node);

    // should find it now that it's added
    theDaemon.getLockssRepository(mau).createNewNode(TEST_URL + "/branch3");
    node = nodeManager.getNodeState(cus);
    assertNotNull(node);
  }

  public void testLoadingFromHistoryRepository() throws Exception {
    CachedUrlSet cus = getCUS(mau, TEST_URL + "/branch3");
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
    CachedUrlSet cus = getCUS(mau, TEST_URL + "/branch3");
    // add to lockss repository
    theDaemon.getLockssRepository(mau).createNewNode(TEST_URL + "/branch3");
    // store in history repository
    ArrayList polls = new ArrayList(1);
    polls.add(new PollState(1, "lwr1", "upr1", 1, 0, Deadline.MAX, false));

    // start the poll
    pollManager.sendPollRequest(LcapMessage.CONTENT_POLL_REQ, new PollSpec(cus));

    NodeStateImpl node = new NodeStateImpl(cus, 123, new CrawlState(-1, -1, -1),
                                           polls, historyRepo);
    historyRepo.storeNodeState(node);

    // should keep poll active
    node = (NodeStateImpl)nodeManager.getNodeState(cus);
    assertEquals(1, node.polls.size());
    assertFalse(node.getPollHistories().hasNext());

    cus = getCUS(mau, TEST_URL + "/branch4");
    // add to lockss repository
    theDaemon.getLockssRepository(mau).createNewNode(TEST_URL + "/branch4");
    // store in history repository
    polls = new ArrayList(1);
    polls.add(new PollState(1, "lwr1", "upr1", 1, 0, Deadline.MAX, false));
    // don't start the poll

    node = new NodeStateImpl(cus, 123, new CrawlState(-1, -1, -1),
                             polls, historyRepo);
    historyRepo.storeNodeState(node);

    // should transfer poll to history
    node = (NodeStateImpl)nodeManager.getNodeState(cus);
    assertNotNull(node);
    assertEquals(TEST_URL + "/branch4", node.getCachedUrlSet().getUrl());
    assertEquals(123, node.getAverageHashDuration());
    assertEquals(0, node.polls.size());
    assertEquals(1, node.pollHistories.size());
  }

  public void testGetAuState() {
    AuState auState = nodeManager.getAuState();
    assertEquals(mau.getAUId(), auState.getArchivalUnit().getAUId());
  }

  public void testMapErrorCodes() {
    assertEquals(PollState.ERR_HASHING,
                 nodeManager.mapResultsErrorToPollError(Poll.ERR_HASHING));
    assertEquals(PollState.ERR_IO,
                 nodeManager.mapResultsErrorToPollError(Poll.ERR_IO));
    assertEquals(PollState.ERR_SCHEDULE_HASH,
                 nodeManager.mapResultsErrorToPollError(Poll.ERR_SCHEDULE_HASH));
    assertEquals(PollState.ERR_UNDEFINED,
                 nodeManager.mapResultsErrorToPollError(1));
  }

  public void testShouldStartPoll() throws Exception {
    contentPoll = createPoll(TEST_URL, true, false, 15, 5);
    PollTally results = contentPoll.getVoteTally();
    // let's generate some history
    CachedUrlSet cus = results.getCachedUrlSet();
    Vector subFiles = new Vector(2);
    subFiles.add(getCUS(mau, TEST_URL));
    ( (MockCachedUrlSet) cus).setHashItSource(subFiles);
    NodeState node = nodeManager.getNodeState(cus);
    for (int i = 0; i < 3; i++) {
      ( (NodeStateImpl) node).closeActivePoll(new PollHistory(Poll.CONTENT_POLL,
          "", "", PollHistory.WON, 100, 123, null, false));
    }

    // if we got a valid node we should return true
    assertTrue(nodeManager.shouldStartPoll(cus, results));

    // if we haven't got one we should return false
    assertFalse(nodeManager.shouldStartPoll(getCUS(mau, TEST_URL + "/bogus"),
                                            results));

    // if we have a damaged node we return false
    nodeManager.damagedNodes.add(cus.getUrl());
    assertFalse(nodeManager.shouldStartPoll(cus, results));
    nodeManager.damagedNodes.remove(cus.getUrl());
  }

  public void testStartPoll() throws Exception {
    contentPoll = createPoll(TEST_URL, true, false, 15, 5);
    PollTally results = contentPoll.getVoteTally();
    // let's generate some history
    CachedUrlSet cus = results.getCachedUrlSet();
    Vector subFiles = new Vector(2);
    subFiles.add(getCUS(mau, TEST_URL));
    ( (MockCachedUrlSet) cus).setHashItSource(subFiles);

    assertNull(nodeManager.activeNodes.get(results.getPollKey()));
    nodeManager.startPoll(cus, results, false);
    assertSame(nodeManager.activeNodes.get(results.getPollKey()),
               nodeManager.getNodeState(cus));
    nodeManager.updatePollResults(cus, results);
    assertNull(nodeManager.activeNodes.get(results.getPollKey()));
  }

  public void testHandleContentPoll() throws Exception {
    contentPoll = createPoll(TEST_URL, true, true, 15, 5);
    PollTally results = contentPoll.getVoteTally();
    PollSpec spec = results.getPollSpec();

    NodeState nodeState = nodeManager.getNodeState(getCUS(mau, TEST_URL));
    // won content poll
    // - running
    PollState pollState = new PollState(results.getType(),
                                        spec.getLwrBound(),
                                        spec.getUprBound(),
                                        PollState.RUNNING,
                                        results.getStartTime(),
                                        Deadline.MAX,
                                        false);
    nodeManager.handleContentPoll(pollState, results, nodeState);
    assertEquals(PollState.WON, pollState.getStatus());
    reputationChangeTest(results);

    // - repairing
    pollState = new PollState(results.getType(),
                              spec.getLwrBound(),
                              spec.getUprBound(),
                              PollState.REPAIRING,
                              results.getStartTime(),
                              Deadline.MAX, false
                              );
    spec = results.getPollSpec();
    nodeManager.handleContentPoll(pollState, results, nodeState);
    assertEquals(PollState.REPAIRED, pollState.getStatus());
    reputationChangeTest(results);

    // lost content poll
    contentPoll = createPoll(TEST_URL + "/branch1", true, true, 5, 15);
    results = contentPoll.getVoteTally();
    spec = results.getPollSpec();
    // - repairing
    pollState = new PollState(results.getType(),
                              spec.getLwrBound(),
                              spec.getUprBound(),

                              PollState.REPAIRING,
                              results.getStartTime(),
                              Deadline.MAX,
                              false);
    nodeManager.handleContentPoll(pollState, results, nodeState);
    assertEquals(PollState.UNREPAIRABLE, pollState.getStatus());
    reputationChangeTest(results);

    // - internal
    nodeState = nodeManager.getNodeState(getCUS(mau, TEST_URL + "/branch1"));
    pollState = new PollState(results.getType(),
                              spec.getLwrBound(),
                              spec.getUprBound(),
                              PollState.RUNNING,
                              results.getStartTime(),
                              Deadline.MAX,
                              false);
    nodeManager.handleContentPoll(pollState, results, nodeState);
    assertEquals(PollState.LOST, pollState.getStatus());
    // assert name poll requested
    String url = nodeState.getCachedUrlSet().getUrl();
    assertNotNull(url);
    assertEquals(MockPollManager.NAME_REQUESTED,
                 ( (MockPollManager) theDaemon.getPollManager()).getPollStatus(
        url));

    // - leaf
    nodeState = nodeManager.getNodeState(getCUS(mau,
                                                TEST_URL + "/branch1/file1.doc"));
    pollState = new PollState(results.getType(), spec.getLwrBound(),
                              spec.getUprBound(),
                              PollState.RUNNING,
                              results.getStartTime(),
                              Deadline.MAX,
                              false);
    nodeManager.handleContentPoll(pollState, results, nodeState);
    assertEquals(PollState.REPAIRING, pollState.getStatus());
    // assert repair call scheduled
    assertEquals(MockCrawlManager.SCHEDULED,
                 ( (MockCrawlManager) theDaemon.getCrawlManager()).getUrlStatus(
        nodeState.getCachedUrlSet().getUrl()));
    // assert poll suspended
    assertEquals(MockPollManager.SUSPENDED,
                 ( (MockPollManager) theDaemon.getPollManager()).getPollStatus(
        results.getPollKey()));

    // - internal SingleNodeCachedUrlSetSpec
    contentPoll = createPoll(TEST_URL + "/branch1",
                             PollSpec.SINGLE_NODE_LWRBOUND, null,
                             true, true, 5, 15);
    results = contentPoll.getVoteTally();
    spec = results.getPollSpec();
    nodeState = nodeManager.getNodeState(getCUS(mau, TEST_URL+"/branch1"));
    pollState = new PollState(results.getType(), spec.getLwrBound(),
                              spec.getUprBound(),
                              PollState.RUNNING,
                              results.getStartTime(),
                              Deadline.MAX,
                              false);
    nodeManager.handleContentPoll(pollState, results, nodeState);
    assertEquals(PollState.REPAIRING, pollState.getStatus());
    // assert repair call scheduled
    assertEquals(MockCrawlManager.SCHEDULED,
                 ((MockCrawlManager)theDaemon.getCrawlManager()).getUrlStatus(
        nodeState.getCachedUrlSet().getUrl()));
    // assert poll suspended
    assertEquals(MockPollManager.SUSPENDED,
                 ((MockPollManager)theDaemon.getPollManager()).getPollStatus(
        results.getPollKey()));
  }

  public void testHandleNamePoll() throws Exception {
    namePoll = createPoll(TEST_URL + "/branch2", false, true, 15, 5);
    PollTally results = namePoll.getVoteTally();
    PollSpec spec = results.getPollSpec();
    NodeState nodeState =
        nodeManager.getNodeState(getCUS(mau, TEST_URL + "/branch2"));
    // won name poll
    PollState pollState = new PollState(results.getType(),
                                        spec.getLwrBound(),
                                        spec.getUprBound(),
                                        PollState.RUNNING,
                                        results.getStartTime(),
                                        Deadline.MAX,
                                        false);
    MockCachedUrlSet mcus = (MockCachedUrlSet) results.getCachedUrlSet();
    Vector subFiles = new Vector(2);
    subFiles.add(getCUS(mau, TEST_URL + "/branch2/file1.doc"));
    subFiles.add(getCUS(mau, TEST_URL + "/branch2/file2.doc"));

    mcus.setFlatItSource(subFiles);
    nodeManager.handleNamePoll(pollState, results, nodeState);
    // we should call a content poll on the subnodes here
    assertEquals(MockPollManager.CONTENT_REQUESTED,
                 ( (MockPollManager) theDaemon.getPollManager()).getPollStatus(
        TEST_URL + "/branch2/file1.doc"));
    assertEquals(MockPollManager.CONTENT_REQUESTED,
                 ( (MockPollManager) theDaemon.getPollManager()).getPollStatus(
        TEST_URL + "/branch2/file2.doc"));

    // and single node call
    assertEquals(MockPollManager.CONTENT_REQUESTED,
                 ((MockPollManager)theDaemon.getPollManager()).getPollStatus(
        TEST_URL + "/branch2"));
    assertEquals(PollState.WON, pollState.getStatus());

    // Test a repair
    String deleteUrl = TEST_URL + "/branch2/testentry2.html";
    String repairUrl = TEST_URL + "/branch2/testentry4.html";
    String repairUrl2 = TEST_URL + "/branch2/testentry5.html";
    // unsuccessful repair
    namePoll = createPoll(TEST_URL + "/branch2", false, true, 5, 15);
    results = namePoll.getVoteTally();
    spec = results.getPollSpec();
    pollState = new PollState(results.getType(),
                              spec.getLwrBound(),
                              spec.getUprBound(),
                              PollState.REPAIRING,
                              results.getStartTime(),
                              Deadline.MAX,
                              false);
    nodeManager.handleNamePoll(pollState, results, nodeState);
    assertEquals(PollState.UNREPAIRABLE, pollState.getStatus());
    assertNull(( (MockCrawlManager) theDaemon.getCrawlManager()).getUrlStatus(
        repairUrl));

    // attempted repair
    pollState = new PollState(results.getType(),
                              spec.getLwrBound(),
                              spec.getUprBound(),
                              PollState.RUNNING,
                              results.getStartTime(),
                              Deadline.MAX,
                              false);

    // add url to delete to repository (need to really add it, so it can be
    // deactivated)
    RepositoryNode delNode = TestRepositoryNodeImpl.createLeaf(
        theDaemon.getLockssRepository(mau), deleteUrl, "test stream", null);
    assertFalse(delNode.isInactive());
    assertNull(theDaemon.getLockssRepository(mau).getNode(repairUrl));

    nodeManager.handleNamePoll(pollState, results, nodeState);
    assertEquals(PollState.REPAIRING, pollState.getStatus());
    assertEquals(MockCrawlManager.SCHEDULED,
                 ( (MockCrawlManager) theDaemon.getCrawlManager()).getUrlStatus(
        repairUrl));
    // only one repair per poll
    assertNull(( (MockCrawlManager) theDaemon.getCrawlManager()).getUrlStatus(
        repairUrl2));

    // deleted node is marked inactive
    assertTrue(delNode.isInactive());
    // node is created for the repair
    //assertNotNull(theDaemon.getLockssRepository(mau).getNode(repairUrl));
  }

  public void testHandleAuPolls() throws Exception {
    // have to change the AUCUS for the MockArchivalUnit here
    // to properly test handling AuNode content polls
    String auUrl = AuUrl.PROTOCOL_COLON;
    MockCachedUrlSet mcus = (MockCachedUrlSet) getCUS(mau, auUrl);
    Vector auChildren = new Vector();
    mcus.setFlatItSource(auChildren);
    mau.setAUCachedUrlSet(mcus);

    nodeManager.createNodeState(mcus);

    contentPoll = createPoll(auUrl, true, true, 15, 5);
    PollTally results = contentPoll.getVoteTally();
    PollSpec spec = results.getPollSpec();

    AuState auState = nodeManager.getAuState();
    assertEquals( -1, auState.getLastTopLevelPollTime());

    NodeStateImpl nodeState = (NodeStateImpl)
        nodeManager.getNodeState(getCUS(mau, auUrl));
    // test that error polls don't update last poll time
    PollState pollState = new PollState(results.getType(),
                                        spec.getLwrBound(),
                                        spec.getUprBound(),
                                        PollState.ERR_IO,
                                        results.getStartTime(),
                                        Deadline.MAX,
                                        false);
    nodeState.addPollState(pollState);
    nodeManager.updateState(nodeState, results);
    assertEquals(-1, auState.getLastTopLevelPollTime());

    // test that a finished top-level poll sets the time right
    TimeBase.setSimulated(TimeBase.nowMs());
    nodeState = (NodeStateImpl)nodeManager.getNodeState(getCUS(mau, auUrl));
    pollState = new PollState(results.getType(),
                              spec.getLwrBound(),
                              spec.getUprBound(),
                              PollState.RUNNING,
                              results.getStartTime(),
                              Deadline.MAX,
                              false);
    nodeState.addPollState(pollState);
    nodeManager.updateState(nodeState, results);
    assertEquals(PollState.WON, pollState.getStatus());
    assertEquals(TimeBase.nowMs(), auState.getLastTopLevelPollTime());

    // add some fake children
    auChildren.add(getCUS(mau, "testDir1"));
    auChildren.add(getCUS(mau, "testDir2"));
    mcus.setFlatItSource(auChildren);

    // test that won name poll calls content polls on Au children
    contentPoll = createPoll(auUrl, false, true, 15, 5);
    results = contentPoll.getVoteTally();
    MockCachedUrlSet mcus2 = (MockCachedUrlSet) results.getCachedUrlSet();
    Vector subFiles = new Vector(2);
    subFiles.add(getCUS(mau, "testDir1"));
    subFiles.add(getCUS(mau, "testDir2"));
    mcus2.setFlatItSource(subFiles);
    pollState = new PollState(results.getType(),
                              spec.getLwrBound(),
                              spec.getUprBound(),
                              PollState.RUNNING,
                              results.getStartTime(),
                              Deadline.MAX,
                              false);
    nodeState.addPollState(pollState);
    nodeManager.updateState(nodeState, results);
    assertEquals(PollState.WON, pollState.getStatus());
    assertEquals(TimeBase.nowMs(), auState.getLastTopLevelPollTime());
    assertEquals(MockPollManager.CONTENT_REQUESTED,
                 ( (MockPollManager) theDaemon.getPollManager()).getPollStatus(
        "testDir1"));
    assertEquals(MockPollManager.CONTENT_REQUESTED,
                 ( (MockPollManager) theDaemon.getPollManager()).getPollStatus(
        "testDir2"));
  }

  public void testCheckLastHistory() throws Exception {
    TimeBase.setSimulated(10000);

    NodeState nodeState = nodeManager.getNodeState(getCUS(mau, TEST_URL));

    // these are true if the pollmanager doesn't know about them
    historyCheckTest(PollState.SCHEDULED, nodeState, true);
    historyCheckTest(PollState.SCHEDULED, nodeState, false);
    ( (MockPollManager) theDaemon.getPollManager()).thePolls.remove(TEST_URL);

    historyCheckTest(PollState.RUNNING, nodeState, true);
    historyCheckTest(PollState.RUNNING, nodeState, false);
    ( (MockPollManager) theDaemon.getPollManager()).thePolls.remove(TEST_URL);

    historyCheckTest(PollState.REPAIRING, nodeState, true);
    historyCheckTest(PollState.REPAIRING, nodeState, false);
    ( (MockPollManager) theDaemon.getPollManager()).thePolls.remove(TEST_URL);

    // shouldn't act if it isn't our poll
    historyCheckTest(Poll.NAME_POLL, PollState.UNFINISHED, nodeState, false, false);
    historyCheckTest(Poll.NAME_POLL, PollState.UNFINISHED, nodeState, true, true);
    ( (MockPollManager) theDaemon.getPollManager()).thePolls.remove(TEST_URL);


    // these are false
    historyCheckTest(PollState.WON, nodeState, false);
    ( (MockPollManager) theDaemon.getPollManager()).thePolls.remove(TEST_URL);
    historyCheckTest(PollState.REPAIRED, nodeState, false);
    ( (MockPollManager) theDaemon.getPollManager()).thePolls.remove(TEST_URL);

    // this is true because we're going to try and fix unrepairable
    historyCheckTest(PollState.UNREPAIRABLE, nodeState, true);
    ( (MockPollManager) theDaemon.getPollManager()).thePolls.remove(TEST_URL);

    // this true for lost polls
    historyCheckTest(Poll.CONTENT_POLL, PollState.LOST, nodeState, true);
    ( (MockPollManager) theDaemon.getPollManager()).thePolls.remove(TEST_URL);
    historyCheckTest(Poll.NAME_POLL, PollState.LOST, nodeState, true);
    ( (MockPollManager) theDaemon.getPollManager()).thePolls.remove(TEST_URL);

    // this is true, since we called the poll
    historyCheckTest(PollState.ERR_IO, nodeState, true);
    ( (MockPollManager) theDaemon.getPollManager()).thePolls.remove(TEST_URL);
    TimeBase.setReal();
  }

  private void historyCheckTest(int pollType, int pollState, NodeState node,
                                boolean shouldSchedule) {
    historyCheckTest(pollType, pollState, node, true, shouldSchedule);
  }

  private void historyCheckTest(int pollType, int pollState, NodeState node,
                                boolean ourPoll, boolean shouldSchedule) {
    PollHistory history = new PollHistory(pollType, null, null,
                                          pollState, 123,
                                          123, null,
                                          ourPoll);
    if (shouldSchedule) {
      assertTrue(nodeManager.checkLastHistory(history, node, true));
      assertNull(( (MockPollManager) theDaemon.getPollManager()).
                   getPollStatus(TEST_URL));
      assertTrue(nodeManager.checkLastHistory(history, node, false));
      assertEquals(MockPollManager.NAME_REQUESTED,
                   ( (MockPollManager) theDaemon.getPollManager()).
                   getPollStatus(
          TEST_URL));
    }
    else {
      assertFalse(nodeManager.checkLastHistory(history, node, false));
    }
  }

  private void historyCheckTest(int pollState, NodeState node,
                                boolean shouldSchedule) {
    historyCheckTest(Poll.NAME_POLL, pollState, node, shouldSchedule);
  }

  private void reputationChangeTest(PollTally results) {
    Iterator voteIt = results.getPollVotes().iterator();
    while (voteIt.hasNext()) {
      Vote vote = (Vote) voteIt.next();
      int repChange = IdentityManager.AGREE_VOTE;
      if (!vote.isAgreeVote()) {
        repChange = IdentityManager.DISAGREE_VOTE;
      }
      assertEquals(repChange, idManager.lastChange(vote.getIDAddress()));
    }
  }

  static CachedUrlSet getCUS(MockArchivalUnit mau, String url) throws Exception {
    return new MockCachedUrlSet(mau, new RangeCachedUrlSetSpec(url));
  }

  static CachedUrlSet makeFakeCachedUrlSet(MockArchivalUnit mau,
                                           String startUrl,
                                           int numBranches,
                                           int numFiles) throws Exception {
    Vector files = new Vector(numFiles * numBranches);
    Vector branches = new Vector(numBranches);

    for (int ib = 1; ib <= numBranches; ib++) {
      String b_url = TEST_URL + File.separator + "branch" + ib;
      MockCachedUrlSet mcus = new MockCachedUrlSet(mau,
          new RangeCachedUrlSetSpec(b_url));
      Vector subFiles = new Vector(numFiles);
      Vector subBranches = new Vector(numFiles);
      for (int ix = 1; ix <= numFiles; ix++) {
        String f_url = b_url + File.separator + "file" + ix + ".doc";
        MockCachedUrl cu = new MockCachedUrl(f_url);
        files.add(cu);
        subFiles.add(cu);
        MockCachedUrlSet subMcus = new MockCachedUrlSet(mau,
            new RangeCachedUrlSetSpec(f_url));
        subBranches.add(subMcus);
        subMcus.addUrl("test string" + ix, f_url);
        subMcus.setFlatItSource(new ArrayList());
        subMcus.setHashItSource(new ArrayList());
      }
      mcus.addUrl("test string" + ib, b_url);
      mcus.setFlatItSource(subBranches);
      mcus.setHashItSource(subFiles);
      branches.add(mcus);
    }
    MockCachedUrlSet cus = new MockCachedUrlSet(mau,
                                                new RangeCachedUrlSetSpec(
        startUrl));
    cus.addUrl("test string", startUrl);
    cus.setHashItSource(files);
    cus.setFlatItSource(branches);
    return cus;
  }

  static void loadNodeStates(ArchivalUnit au, NodeManagerImpl nodeManager) {
    // recurse through au cachedurlsets
    CachedUrlSet cus = au.getAUCachedUrlSet();
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
          CachedUrlSet newSet = ( (BaseArchivalUnit) au).makeCachedUrlSet(rSpec);
          nodeManager.createNodeState(newSet);
      }
    }
  }

  private Poll createPoll(String url, boolean isContentPoll, boolean isLocal,
                          int numAgree, int numDisagree) throws Exception {
    return createPoll(url, null, null, isContentPoll, isLocal, numAgree,
                      numDisagree);
  }
  private Poll createPoll(String url, String lwrBound, String uprBound,
                          boolean isContentPoll, boolean isLocal,
                          int numAgree, int numDisagree) throws Exception {
    LcapIdentity testID = null;
    LcapMessage testmsg = null;
    if (isLocal) {
      testID = idManager.getLocalIdentity();
    }
    else {
      try {
        InetAddress testAddr = InetAddress.getByName("123.3.4.5");
        testID = idManager.findIdentity(testAddr);
      }
      catch (UnknownHostException ex) {
        fail("can't open test host");
      }
    }

    byte[] bytes = new byte[20];
    random.nextBytes(bytes);

    try {

      testmsg = LcapMessage.makeRequestMsg(
          new PollSpec(mau.getAUId(),
                       url, lwrBound, uprBound, null),
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
    String[] testCaseList = {
        TestNodeManagerImpl.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
