/*
 * $Id: TestNodeManagerImpl.java,v 1.34 2003-03-01 03:21:30 aalto Exp $
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
import org.lockss.plugin.PluginManager;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.hasher.HashService;
import org.lockss.plugin.*;

public class TestNodeManagerImpl extends LockssTestCase {
  public static final String TEST_URL = "http://www.example.com";
  private static Logger log = Logger.getLogger("TestNMI");
  private String tempDirPath;
  private MockArchivalUnit mau = null;
  private NodeManagerImpl nodeManager;
  private PollManager pollManager;
  private List urlList = null;
  private Poll namePoll = null;
  private Poll contentPoll = null;
  private Random random = new Random();

  private MockLockssDaemon theDaemon = new MockLockssDaemon(null);

  public TestNodeManagerImpl(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    TestHistoryRepositoryImpl.configHistoryParams(tempDirPath);

    mau = new MockArchivalUnit();
    mau.setAUCachedUrlSet(makeFakeCachedUrlSet(TEST_URL, 2, 2));
    theDaemon.getPluginManager();
    PluginUtil.registerArchivalUnit(mau);
    theDaemon.setCrawlManager(new MockCrawlManager());
    pollManager = new MockPollManager();
    theDaemon.setPollManager(pollManager);
    theDaemon.setIdentityManager(new MockIdentityManager());

    nodeManager = new NodeManagerImpl(mau);
    nodeManager.initService(theDaemon);
    nodeManager.repository = new HistoryRepositoryImpl(tempDirPath);
    // don't start nodemanager service because of treewalk thread
    pollManager.initService(theDaemon);
  }

  public void tearDown() throws Exception {
    PluginUtil.unregisterAllArchivalUnits();
    nodeManager.stopService();
    pollManager.stopService();
    theDaemon.stopDaemon();
    TimeBase.setReal();
    super.tearDown();
  }

  public void testGetNodeState() throws Exception {
    CachedUrlSet cus = getCUS("http://www.example.com");
    NodeState node = nodeManager.getNodeState(cus);
    assertNotNull(node);
    assertTrue(cus.equals(node.getCachedUrlSet()));
    assertNotNull(node.getCrawlState());
    assertEquals( -1, node.getCrawlState().getType());
    assertEquals(CrawlState.FINISHED, node.getCrawlState().getStatus());

    cus = getCUS("http://www.example.com/branch1");
    node = nodeManager.getNodeState(cus);
    assertNotNull(node);
    assertTrue(cus.equals(node.getCachedUrlSet()));

    cus = getCUS("http://www.example.com/branch2/file2.doc");
    node = nodeManager.getNodeState(cus);
    assertNotNull(node);
    assertTrue(cus.equals(node.getCachedUrlSet()));

    cus = getCUS("http://www.example.com/branch3");
    node = nodeManager.getNodeState(cus);
    assertTrue(node == null);
  }

  public void testGetAuState() {
    AuState auState = nodeManager.getAuState();
    assertEquals(mau.getAUId(), auState.getArchivalUnit().getAUId());
  }

  public void testGetActiveCrawledNodes() throws Exception {
    Iterator nodeIt = nodeManager.getActiveCrawledNodes(mau.getAUCachedUrlSet());
    assertTrue(!nodeIt.hasNext());

    CachedUrlSet cus = getCUS("http://www.example.com/branch1");
    NodeState node = nodeManager.getNodeState(cus);
    node.getCrawlState().type = CrawlState.NEW_CONTENT_CRAWL;
    node.getCrawlState().status = CrawlState.SCHEDULED;
    cus = getCUS("http://www.example.com/branch2/file1.doc");
    node = nodeManager.getNodeState(cus);
    node.getCrawlState().type = CrawlState.BACKGROUND_CRAWL;
    node.getCrawlState().status = CrawlState.RUNNING;
    nodeIt = nodeManager.getActiveCrawledNodes(mau.getAUCachedUrlSet());
    ArrayList nodeL = new ArrayList(2);
    while (nodeIt.hasNext()) {
      node = (NodeState) nodeIt.next();
      nodeL.add(node.getCachedUrlSet().getUrl());
    }
    String[] expectedA = new String[] {
        "http://www.example.com/branch1",
        "http://www.example.com/branch2/file1.doc"
    };
    assertIsomorphic(expectedA, nodeL);
  }

  public void testGetFilteredPolledNodes() throws Exception {
    Iterator nodeIt = nodeManager.getFilteredPolledNodes(mau.getAUCachedUrlSet(),
        PollState.RUNNING + PollState.WON);
    assertTrue(!nodeIt.hasNext());

    CachedUrlSet cus = getCUS("http://www.example.com/branch1");
    NodeState node = nodeManager.getNodeState(cus);
    ((NodeStateImpl)node).addPollState(new PollState(Poll.CONTENT_POLL, "",
        "",
        PollState.RUNNING, 123, null));
    cus = getCUS("http://www.example.com/branch2/file1.doc");
    node = nodeManager.getNodeState(cus);
    ((NodeStateImpl)node).addPollState(new PollState(Poll.NAME_POLL, "", "",
        PollState.WON, 123, null));
    nodeIt = nodeManager.getFilteredPolledNodes(mau.getAUCachedUrlSet(),
                                                PollState.RUNNING +
                                                PollState.WON);
    ArrayList nodeL = new ArrayList(2);
    while (nodeIt.hasNext()) {
      node = (NodeState) nodeIt.next();
      nodeL.add(node.getCachedUrlSet().getUrl());
    }
    String[] expectedA = new String[] {
        "http://www.example.com/branch1",
        "http://www.example.com/branch2/file1.doc"
    };
    assertIsomorphic(expectedA, nodeL);

    nodeIt = nodeManager.getFilteredPolledNodes(mau.getAUCachedUrlSet(),
                                                PollState.RUNNING);
    nodeL = new ArrayList(1);
    while (nodeIt.hasNext()) {
      node = (NodeState)nodeIt.next();
      nodeL.add(node.getCachedUrlSet().getUrl());
    }
    expectedA = new String[] {
        "http://www.example.com/branch1"
    };
    assertIsomorphic(expectedA, nodeL);
  }

  public void testGetHistories() throws Exception {
    Iterator histIt = nodeManager.getNodeHistories(mau.getAUCachedUrlSet(), 3);
    assertTrue(!histIt.hasNext());

    CachedUrlSet cus = getCUS("http://www.example.com/branch1");
    NodeState node = nodeManager.getNodeState(cus);
    ( (NodeStateImpl) node).closeActivePoll(new PollHistory(1, "", "", 1, 123,
        123, null));
    ( (NodeStateImpl) node).closeActivePoll(new PollHistory(2, "", "", 2, 123,
        123, null));
    ( (NodeStateImpl) node).closeActivePoll(new PollHistory(3, "", "", 3, 123,
        123, null));
    ( (NodeStateImpl) node).closeActivePoll(new PollHistory(4, "", "", 4, 123,
        123, null));
    histIt = nodeManager.getNodeHistories(mau.getAUCachedUrlSet(), 3);
    ArrayList histL = new ArrayList(3);
    while (histIt.hasNext()) {
      PollHistory hist = (PollHistory) histIt.next();
      histL.add("type=" + hist.type + ":status=" + hist.status);
    }
    String[] expectedA = new String[] {
        "type=1:status=1",
        "type=2:status=2",
        "type=3:status=3",
    };
    assertIsomorphic(expectedA, histL);
  }

  public void testGetHistoriesSince() throws Exception {
    Iterator histIt = nodeManager.getNodeHistoriesSince(mau.getAUCachedUrlSet(),
        Deadline.at(200));
    assertTrue(!histIt.hasNext());

    CachedUrlSet cus = getCUS("http://www.example.com/branch1");
    NodeState node = nodeManager.getNodeState(cus);
    ( (NodeStateImpl) node).closeActivePoll(new PollHistory(1, "", "", 1, 100,
        123, null));
    ( (NodeStateImpl) node).closeActivePoll(new PollHistory(2, "", "", 2, 150,
        123, null));
    ( (NodeStateImpl) node).closeActivePoll(new PollHistory(3, "", "", 3, 200,
        123, null));
    ( (NodeStateImpl) node).closeActivePoll(new PollHistory(4, "", "", 4, 250,
        123, null));
    histIt = nodeManager.getNodeHistoriesSince(mau.getAUCachedUrlSet(),
                                               Deadline.at(200));
    ArrayList histL = new ArrayList(2);
    while (histIt.hasNext()) {
      PollHistory hist = (PollHistory) histIt.next();
      histL.add("type=" + hist.type + ":start=" + hist.startTime);
    }
    String[] expectedA = new String[] {
        "type=3:start=200",
        "type=4:start=250"
    };
    assertIsomorphic(expectedA, histL);
  }

  public void testTreeWalkStart() throws Exception {
    MockPollManager pollMan = (MockPollManager)theDaemon.getPollManager();
    MockCrawlManager crawlMan = (MockCrawlManager)theDaemon.getCrawlManager();
    pollMan.startService();
    crawlMan.startService();

    AuState auState = nodeManager.getAuState();

    //should allow walk to start if last crawl time >= 0
    //should not schedule a top level poll
    auState.lastCrawlTime = 123;
    auState.lastTopLevelPoll = TimeBase.nowMs();
    nodeManager.doTreeWalk();
    assertTrue(crawlMan.getAuStatus(mau)==null);
    assertTrue(pollMan.getPollStatus(mau.getAUCachedUrlSet().getUrl())==null);

    //should allow walk but schedule top level poll
    auState.lastTopLevelPoll = 0;
    nodeManager.doTreeWalk();
    assertTrue(crawlMan.getAuStatus(mau)==null);
    assertTrue(pollMan.getPollStatus(
        mau.getAUCachedUrlSet().getUrl()) == MockPollManager.CONTENT_REQUESTED);


    //should abort walk and schedule crawl if last crawl time < 0
    auState.lastCrawlTime = -123;
    nodeManager.doTreeWalk();
    assertTrue(crawlMan.getAuStatus(mau)==MockCrawlManager.SCHEDULED);

    pollMan.stopService();
    crawlMan.stopService();
  }

  public void testTreeWalkThread() throws Exception {
    MockCrawlManager crawlMan = (MockCrawlManager)theDaemon.getCrawlManager();
    crawlMan.startService();

    assertTrue(nodeManager.treeWalkThread == null);
    AuState auState = nodeManager.getAuState();

    // should start thread, but thread should sleep
    auState.lastTreeWalk = TimeBase.nowMs();
    nodeManager.startService();
    NodeManagerImpl.TreeWalkThread thread = nodeManager.treeWalkThread;
    assertNotNull(thread);
    assertTrue(!thread.treeWalkRunning);
    assertEquals(auState.lastTreeWalk, thread.lastRun);

    nodeManager.stopService();
    assertTrue(nodeManager.treeWalkThread == null);
/*
    // the thread should trigger a treewalk, and the treewalk should
    // trigger a crawl scheduled
    auState.lastTreeWalk = -123;
    auState.lastCrawlTime = -123;
    nodeManager.startService();

    assertTrue(crawlMan.getAuStatus(mau)==MockCrawlManager.SCHEDULED);
    nodeManager.stopService();
 */
    crawlMan.stopService();
  }

  public void testWalkNodeStateCrawling() throws Exception {
    theDaemon.getPollManager().startService();
    ((MockCrawlManager)theDaemon.getCrawlManager()).startService();

    NodeStateImpl node =
        (NodeStateImpl)nodeManager.getNodeState(getCUS(TEST_URL));

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
    nodeManager.walkNodeState(node);
    assertTrue(pollMan.getPollStatus(node.getCachedUrlSet().getUrl())==null);
    if (shouldSchedule) {
      //XXX uncomment when CrawlManager ready
      //assertTrue(crawlMan.getUrlStatus(
      //node.getCachedUrlSet().getUrl())==MockCrawlManager.SCHEDULED);
    } else {
      assertTrue(crawlMan.getUrlStatus(TEST_URL)==null);
    }
  }

  public void testWalkNodeStatePolling() throws Exception {
    theDaemon.getPollManager().startService();
    ((MockCrawlManager)theDaemon.getCrawlManager()).startService();
    MockPollManager pollMan = (MockPollManager)theDaemon.getPollManager();
    MockCrawlManager crawlMan = (MockCrawlManager)theDaemon.getCrawlManager();

    NodeStateImpl node =
        (NodeStateImpl)nodeManager.getNodeState(getCUS(TEST_URL));
    // should ignore if active poll
    PollState pollState = new PollState(1, "", "", PollState.RUNNING, 1, null);
    node.addPollState(pollState);
    nodeManager.walkNodeState(node);
    // no poll in manager since we just created a PollState
    assertTrue(pollMan.getPollStatus(TEST_URL)==null);
    assertTrue(crawlMan.getUrlStatus(TEST_URL)==null);

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
    nodeManager.walkNodeState(node);
    if (shouldSchedule) {
      assertTrue(pollMan.getPollStatus(
          node.getCachedUrlSet().getUrl()) == MockPollManager.NAME_REQUESTED);
    } else {
      assertTrue(pollMan.getPollStatus(
          node.getCachedUrlSet().getUrl()) == null);
    }
    assertTrue(crawlMan.getUrlStatus(
        node.getCachedUrlSet().getUrl())==null);
  }

  public void testEstimatedTreeWalk() {
    //XXX fix using simulated time
    long estimate = nodeManager.getEstimatedTreeWalkDuration();
    assertTrue(estimate > 0);
    long newEstimate = 100;
    nodeManager.updateEstimate(newEstimate);
    long expectedEst = (estimate + newEstimate) / 2;
    assertEquals(expectedEst, nodeManager.getEstimatedTreeWalkDuration());
  }

  public void testMapErrorCodes() {
    assertEquals(PollState.ERR_HASHING,
                 nodeManager.mapResultsErrorToPollError(Poll.ERR_HASHING));
    assertEquals(PollState.ERR_IO,
                 nodeManager.mapResultsErrorToPollError(Poll.ERR_IO));
    assertEquals(PollState.ERR_NO_QUORUM,
                 nodeManager.mapResultsErrorToPollError(Poll.ERR_NO_QUORUM));
    assertEquals(PollState.ERR_SCHEDULE_HASH,
                 nodeManager.mapResultsErrorToPollError(Poll.ERR_SCHEDULE_HASH));
    assertEquals(PollState.ERR_UNDEFINED,
                 nodeManager.mapResultsErrorToPollError(1));
  }

  public void testHandleContentPoll() throws Exception {
    theDaemon.getHashService().startService();
    theDaemon.getPollManager().startService();
    contentPoll = createPoll(TEST_URL, true, 10, 5);
    Poll.VoteTally results = contentPoll.getVoteTally();
    PollSpec spec = results.getPollSpec();
    NodeState nodeState = nodeManager.getNodeState(getCUS(TEST_URL));
    // won content poll
    // - running
    PollState pollState = new PollState(results.getType(),
                                        spec.getLwrBound(),
                                        spec.getUprBound(),
                                        PollState.RUNNING,
                                        results.getStartTime(),
                                        null);
    nodeManager.handleContentPoll(pollState, results, nodeState);
    assertEquals(PollState.WON, pollState.getStatus());
    testReputationChanges(results);

    // - repairing
    pollState = new PollState(results.getType(),
                              spec.getLwrBound(),
                              spec.getUprBound(),
                              PollState.REPAIRING,
                              results.getStartTime(),
                              null);
    spec = results.getPollSpec();
    nodeManager.handleContentPoll(pollState, results, nodeState);
    assertEquals(PollState.REPAIRED, pollState.getStatus());
    testReputationChanges(results);

    // lost content poll
    contentPoll = createPoll(TEST_URL + "/branch1", true, 5, 10);
    results = contentPoll.getVoteTally();
    spec = results.getPollSpec();
    // - repairing
    pollState = new PollState(results.getType(),
                              spec.getLwrBound(),
                              spec.getUprBound(),

                              PollState.REPAIRING,
                              results.getStartTime(),
                              null);
    nodeManager.handleContentPoll(pollState, results, nodeState);
    assertEquals(PollState.UNREPAIRABLE, pollState.getStatus());
    testReputationChanges(results);

    // - internal
    nodeState = nodeManager.getNodeState(getCUS(TEST_URL + "/branch1"));
    pollState = new PollState(results.getType(),
                              spec.getLwrBound(),
                              spec.getUprBound(),
                              PollState.RUNNING,
                              results.getStartTime(),
                              null);
    nodeManager.handleContentPoll(pollState, results, nodeState);
    assertEquals(PollState.LOST, pollState.getStatus());
    // assert name poll requested
    String url = nodeState.getCachedUrlSet().getUrl();
    assertNotNull(url);
    assertEquals(MockPollManager.NAME_REQUESTED,
                 ((MockPollManager)theDaemon.getPollManager()).getPollStatus(
        url));

    // - leaf
    nodeState = nodeManager.getNodeState(getCUS(TEST_URL + "/branch1/file1.doc"));
    pollState = new PollState(results.getType(), spec.getLwrBound(),
                              spec.getUprBound(),
                              PollState.RUNNING,
                              results.getStartTime(),
                              null);
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

    theDaemon.getHashService().stopService();
    theDaemon.getPollManager().stopService();
  }

  public void testHandleAuContentPoll() throws Exception {
    theDaemon.getHashService().startService();
    theDaemon.getPollManager().startService();
    nodeManager.stopService();

    // have to change the AUCUS for the MockArchivalUnit here
    // to properly test handling AuNode content polls
    String auUrl = AuUrl.PROTOCOL_COLON;
    MockCachedUrlSet mcus = (MockCachedUrlSet)getCUS(auUrl);
    mcus.setFlatIterator((new Vector()).iterator());
    mau.setAUCachedUrlSet(mcus);

    nodeManager = new NodeManagerImpl(mau);
    nodeManager.initService(theDaemon);
    nodeManager.repository = new HistoryRepositoryImpl(tempDirPath);

    contentPoll = createPoll(auUrl, true, 10, 5);
    Poll.VoteTally results = contentPoll.getVoteTally();
    PollSpec spec = results.getPollSpec();
    AuState auState = nodeManager.getAuState();
    assertEquals(-1, auState.getLastTopLevelPollTime());

    TimeBase.setSimulated(TimeBase.nowMs());
    NodeState nodeState = nodeManager.getNodeState(getCUS(auUrl));
    PollState pollState = new PollState(results.getType(),
                                        spec.getLwrBound(),
                                        spec.getUprBound(),
                                        PollState.RUNNING,
                                        results.getStartTime(),
                                        null);
    nodeManager.handleContentPoll(pollState, results, nodeState);
    assertEquals(PollState.WON, pollState.getStatus());
    assertEquals(TimeBase.nowMs(), auState.getLastTopLevelPollTime());

    theDaemon.getHashService().stopService();
    theDaemon.getPollManager().stopService();
  }

  public void testHandleNamePoll() throws Exception {
    theDaemon.getHashService().startService();
    theDaemon.getPollManager().startService();
    contentPoll = createPoll(TEST_URL + "/branch2", false, 10, 5);
    Poll.VoteTally results = contentPoll.getVoteTally();
    PollSpec spec = results.getPollSpec();
    NodeState nodeState = nodeManager.getNodeState(getCUS(TEST_URL + "/branch2"));
    // won name poll
    PollState pollState = new PollState(results.getType(),
                                        spec.getLwrBound(),
                                        spec.getUprBound(),

                                        PollState.RUNNING,
                                        results.getStartTime(),
                                        null);
    nodeManager.handleNamePoll(pollState, results, nodeState);
    // test poll request
    assertEquals(MockPollManager.CONTENT_REQUESTED,
                 ((MockPollManager)theDaemon.getPollManager()).getPollStatus(
        TEST_URL + "/branch2/file1.doc"));
    assertEquals(MockPollManager.CONTENT_REQUESTED,
                 ((MockPollManager)theDaemon.getPollManager()).getPollStatus(
        TEST_URL+"/branch2/file2.doc"));
    //XXX check the error which used to happen here when the normal PollManager
    // was used (should have created an IO error, but didn't)
    assertEquals(PollState.WON, pollState.getStatus());

    // lost name poll
    contentPoll = createPoll(TEST_URL + "/branch2/file1.doc", false, 5, 10);
    results = contentPoll.getVoteTally();
    spec = results.getPollSpec();
    pollState = new PollState(results.getType(),
                              spec.getLwrBound(),
                              spec.getUprBound(),

                              PollState.RUNNING,
                              results.getStartTime(),
                              null);
    nodeManager.handleNamePoll(pollState, results, nodeState);
    // since there are no entries in the results object, nothing much will
    // happen and it should be marked 'REPAIRED'
    assertEquals(PollState.REPAIRED, pollState.getStatus());
    theDaemon.getHashService().stopService();
    theDaemon.getPollManager().stopService();
  }

  private void testReputationChanges(Poll.VoteTally results) {
    Iterator voteIt = results.getPollVotes().iterator();
    MockIdentityManager idManager =
        (MockIdentityManager) theDaemon.getIdentityManager();
    while (voteIt.hasNext()) {
      Vote vote = (Vote) voteIt.next();
      int repChange = IdentityManager.AGREE_VOTE;
      if (!vote.isAgreeVote()) {
        repChange = IdentityManager.DISAGREE_VOTE;
      }
      assertEquals(repChange, idManager.lastChange(vote.getIDAddress()));
    }
  }

  private CachedUrlSet getCUS(String url) throws Exception {
    return new MockCachedUrlSet(mau, new RangeCachedUrlSetSpec(url));
  }

  private CachedUrlSet makeFakeCachedUrlSet(String startUrl, int numBranches,
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
        subMcus.setTreeItSource(new ArrayList());
      }
      mcus.addUrl("test string" + ib, b_url);
      mcus.setFlatItSource(subBranches);
      mcus.setTreeItSource(subFiles);
      branches.add(mcus);
    }
    MockCachedUrlSet cus = new MockCachedUrlSet(mau,
                                                new RangeCachedUrlSetSpec(
        startUrl));
    cus.addUrl("test string", startUrl);
    cus.setTreeItSource(files);
    cus.setFlatItSource(branches);
    return cus;
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
        TestNodeManagerImpl.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
