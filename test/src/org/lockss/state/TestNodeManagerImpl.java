/*
 * $Id: TestNodeManagerImpl.java,v 1.8 2003-01-30 02:05:16 aalto Exp $
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
import org.lockss.plugin.Plugin;
import org.lockss.poller.*;
import org.lockss.protocol.*;

public class TestNodeManagerImpl extends LockssTestCase {
  public static final String TEST_URL = "http://www.example.com";
  private String tempDirPath;
  private MockArchivalUnit mau = null;
  private NodeManagerImpl nodeManager;
  private List urlList = null;
  private Poll namePoll = null;
  private Poll contentPoll = null;
  private Random random = new Random();

  public TestNodeManagerImpl(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    TestHistoryRepositoryImpl.configHistoryParams(tempDirPath);

    mau = new MockArchivalUnit();
    mau.setAUCachedUrlSet(makeFakeCachedUrlSet(TEST_URL, 2, 2));
    Plugin.registerArchivalUnit(mau);

    nodeManager = new NodeManagerImpl(mau);
    nodeManager.repository = new HistoryRepositoryImpl(tempDirPath);
  }

  public void tearDown() throws Exception {
    super.tearDown();
    Iterator auIt = Plugin.getArchivalUnits();
    ArrayList auList = new ArrayList(Plugin.getNumArchivalUnits());
    while (auIt.hasNext()) {
      auList.add(auIt.next());
    }
    auIt = auList.iterator();
    while (auIt.hasNext()) {
      Plugin.unregisterArchivalUnit((ArchivalUnit)auIt.next());
    }
  }

  public void testGetNodeManager() {
    assertNotNull(NodeManagerImpl.getNodeManager(mau));
  }

  public void testGetNodeState() throws Exception {
    CachedUrlSet cus = getCUS("http://www.example.com");
    NodeState node = nodeManager.getNodeState(cus);
    assertNotNull(node);
    assertTrue(cus.equals(node.getCachedUrlSet()));
    assertNotNull(node.getCrawlState());
    assertEquals(-1, node.getCrawlState().getType());
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
    assertTrue(node==null);
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
      node = (NodeState)nodeIt.next();
      nodeL.add(node.getCachedUrlSet().getPrimaryUrl());
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
        PollState.RUNNING, 123, null));
    cus = getCUS("http://www.example.com/branch2/file1.doc");
    node = nodeManager.getNodeState(cus);
    ((NodeStateImpl)node).addPollState(new PollState(Poll.NAME_POLL, "",
        PollState.WON, 123, null));
    nodeIt = nodeManager.getFilteredPolledNodes(mau.getAUCachedUrlSet(),
        PollState.RUNNING + PollState.WON);
    ArrayList nodeL = new ArrayList(2);
    while (nodeIt.hasNext()) {
      node = (NodeState)nodeIt.next();
      nodeL.add(node.getCachedUrlSet().getPrimaryUrl());
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
      nodeL.add(node.getCachedUrlSet().getPrimaryUrl());
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
    ((NodeStateImpl)node).closeActivePoll(new PollHistory(1, "", 1, 123, 123, null));
    ((NodeStateImpl)node).closeActivePoll(new PollHistory(2, "", 2, 123, 123, null));
    ((NodeStateImpl)node).closeActivePoll(new PollHistory(3, "", 3, 123, 123, null));
    ((NodeStateImpl)node).closeActivePoll(new PollHistory(4, "", 4, 123, 123, null));
    histIt = nodeManager.getNodeHistories(mau.getAUCachedUrlSet(), 3);
    ArrayList histL = new ArrayList(3);
    while (histIt.hasNext()) {
      PollHistory hist = (PollHistory)histIt.next();
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
    ((NodeStateImpl)node).closeActivePoll(new PollHistory(1, "", 1, 100, 123, null));
    ((NodeStateImpl)node).closeActivePoll(new PollHistory(2, "", 2, 150, 123, null));
    ((NodeStateImpl)node).closeActivePoll(new PollHistory(3, "", 3, 200, 123, null));
    ((NodeStateImpl)node).closeActivePoll(new PollHistory(4, "", 4, 250, 123, null));
    histIt = nodeManager.getNodeHistoriesSince(mau.getAUCachedUrlSet(),
        Deadline.at(200));
    ArrayList histL = new ArrayList(2);
    while (histIt.hasNext()) {
      PollHistory hist = (PollHistory)histIt.next();
      histL.add("type=" + hist.type + ":start=" + hist.startTime);
    }
    String[] expectedA = new String[] {
      "type=3:start=200",
      "type=4:start=250"
    };
    assertIsomorphic(expectedA, histL);
  }

  public void testTreeWalk() {

  }

  public void testEstimatedTreeWalk() {

  }

  public void testMapErrorCodes() {
    assertEquals(PollState.ERR_HASHING, nodeManager.mapResultsErrorToPollError(Poll.ERR_HASHING));
    assertEquals(PollState.ERR_IO, nodeManager.mapResultsErrorToPollError(Poll.ERR_IO));
    assertEquals(PollState.ERR_NO_QUORUM, nodeManager.mapResultsErrorToPollError(Poll.ERR_NO_QUORUM));
    assertEquals(PollState.ERR_SCHEDULE_HASH, nodeManager.mapResultsErrorToPollError(Poll.ERR_SCHEDULE_HASH));
    assertEquals(PollState.ERR_UNDEFINED, nodeManager.mapResultsErrorToPollError(1));
  }

  public void testHandleContentPoll() throws Exception {
    contentPoll = createPoll(TEST_URL, true, 10, 5);
    Poll.VoteTally results = contentPoll.getVoteTally();
    NodeState nodeState = nodeManager.getNodeState(getCUS(TEST_URL));
    // won content poll
    // - running
    PollState pollState = new PollState(results.getType(), results.getRegExp(),
                                        PollState.RUNNING,
                                        results.getStartTime(),
                                        null);
    nodeManager.handleContentPoll(pollState, results, nodeState);
    assertEquals(PollState.WON, pollState.getStatus());
    // - repairing
    pollState = new PollState(results.getType(), results.getRegExp(),
                              PollState.REPAIRING,
                              results.getStartTime(),
                              null);
    nodeManager.handleContentPoll(pollState, results, nodeState);
    assertEquals(PollState.REPAIRED, pollState.getStatus());

    // lost content poll
    contentPoll = createPoll(TEST_URL+"/branch1", true, 5, 10);
    results = contentPoll.getVoteTally();
    // - repairing
    pollState = new PollState(results.getType(), results.getRegExp(),
                              PollState.REPAIRING,
                              results.getStartTime(),
                              null);
    nodeManager.handleContentPoll(pollState, results, nodeState);
    assertEquals(PollState.UNREPAIRABLE, pollState.getStatus());
    // - internal
    pollState = new PollState(results.getType(), results.getRegExp(),
                              PollState.RUNNING,
                              results.getStartTime(),
                              null);
    nodeManager.handleContentPoll(pollState, results, nodeState);
    assertEquals(PollState.LOST, pollState.getStatus());
    // - leaf
    nodeState = nodeManager.getNodeState(getCUS(TEST_URL+"/branch1/file1.doc"));
    pollState = new PollState(results.getType(), results.getRegExp(),
                              PollState.RUNNING,
                              results.getStartTime(),
                              null);
    nodeManager.handleContentPoll(pollState, results, nodeState);
    assertEquals(PollState.REPAIRING, pollState.getStatus());
  }

  public void testHandleNamePoll() throws Exception {
    contentPoll = createPoll(TEST_URL+"/branch2", false, 10, 5);
    Poll.VoteTally results = contentPoll.getVoteTally();
    NodeState nodeState = nodeManager.getNodeState(getCUS(TEST_URL));
    // won name poll
    PollState pollState = new PollState(results.getType(), results.getRegExp(),
                                        PollState.RUNNING,
                                        results.getStartTime(),
                                        null);
    nodeManager.handleNamePoll(pollState, results, nodeState);
    // since it will try to call a content poll and fail, this becomes an error
    assertEquals(PollState.WON, pollState.getStatus());

    // lost name poll
    contentPoll = createPoll(TEST_URL+"/branch2/file1.doc", false, 5, 10);
    results = contentPoll.getVoteTally();
    pollState = new PollState(results.getType(), results.getRegExp(),
                              PollState.RUNNING,
                              results.getStartTime(),
                              null);
    nodeManager.handleNamePoll(pollState, results, nodeState);
    // since there are no entries in the results object, nothing much will
    // happen and it should be marked 'REPAIRED'
    assertEquals(PollState.REPAIRED, pollState.getStatus());
  }

  private CachedUrlSet getCUS(String url) throws Exception {
    return new MockCachedUrlSet(mau, new RECachedUrlSetSpec(url, (String)null));
  }

  private CachedUrlSet makeFakeCachedUrlSet(String startUrl, int numBranches,
      int numFiles) throws Exception {
    Vector files = new Vector(numFiles * numBranches);
    Vector branches = new Vector(numBranches);

    for (int ib = 1; ib <= numBranches; ib++) {
      String b_url = TEST_URL + File.separator + "branch" + ib;
      MockCachedUrlSet mcus = new MockCachedUrlSet(mau,
          new RECachedUrlSetSpec(b_url, (String)null));
      Vector subFiles = new Vector(numFiles);
      Vector subBranches = new Vector(numFiles);
      for (int ix=1; ix <= numFiles; ix++) {
        String f_url = b_url + File.separator + "file" + ix + ".doc";
        MockCachedUrl cu = new MockCachedUrl(f_url);
        files.add(cu);
        subFiles.add(cu);
        MockCachedUrlSet subMcus = new MockCachedUrlSet(mau,
            new RECachedUrlSetSpec(f_url, (String)null));
        subBranches.add(subMcus);
        subMcus.addUrl("test string"+ix, f_url);
        subMcus.setFlatItSource(new ArrayList());
        subMcus.setLeafItSource(new ArrayList());
      }
      mcus.addUrl("test string"+ib, b_url);
      mcus.setFlatItSource(subBranches);
      mcus.setLeafItSource(subFiles);
      branches.add(mcus);
    }
    MockCachedUrlSet cus = new MockCachedUrlSet(mau,
        new RECachedUrlSetSpec(startUrl, (String)null));
    cus.addUrl("test string", startUrl);
    cus.setLeafItSource(files);
    cus.setFlatItSource(branches);
    return cus;
  }

  private Poll createPoll(String url, boolean isContentPoll, int numAgree,
                          int numDisagree)
      throws Exception {
    LcapIdentity testID = null;
    LcapMessage testmsg = null;
    try {
      InetAddress testAddr = InetAddress.getByName("127.0.0.1");
      testID = IdentityManager.getIdentityManager().getIdentity(testAddr);
    } catch (UnknownHostException ex) {
      fail("can't open test host");
    }
    byte[] bytes = new byte[20];
    random.nextBytes(bytes);
    try {
      testmsg =  LcapMessage.makeRequestMsg(
        url,
        "^.*doc",
        null,
        bytes,
        bytes,
        (isContentPoll ? LcapMessage.CONTENT_POLL_REQ : LcapMessage.NAME_POLL_REQ),
        123321,
        testID);
    } catch (IOException ex) {
      fail("can't create test name message" + ex.toString());
    }
    Poll p = TestPoll.createCompletedPoll(testmsg, numAgree, numDisagree);
    TestHistoryRepositoryImpl.configHistoryParams(tempDirPath);
    return p;
  }


  public static void main(String[] argv) {
    String[] testCaseList = {TestNodeManagerImpl.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
