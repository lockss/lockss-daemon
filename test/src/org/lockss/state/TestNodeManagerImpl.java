/*
 * $Id: TestNodeManagerImpl.java,v 1.2 2003-01-16 01:44:45 aalto Exp $
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

import org.lockss.poller.Poll;
import org.lockss.daemon.*;
import org.lockss.test.*;
import java.util.*;
import org.lockss.util.TimeBase;
import org.lockss.util.ListUtil;
import java.io.*;
import org.lockss.plugin.Plugin;
import org.lockss.util.Deadline;

public class TestNodeManagerImpl extends LockssTestCase {
  public static final String TEST_URL = "http://www.example.com";
  private MockArchivalUnit mau = null;
  private NodeManagerImpl nodeManager;
  private List urlList = null;

  public TestNodeManagerImpl(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    TestHistoryRepositoryImpl.configHistoryParams(tempDirPath);

    mau = new MockArchivalUnit();
    mau.setAUCachedUrlSet(makeFakeCachedUrlSet(TEST_URL, 2, 2));
    Plugin.registerArchivalUnit(mau);

    nodeManager = new NodeManagerImpl();
  }

  public void testGetNodeManager() {
    assertNotNull(NodeManagerImpl.getNodeManager());
  }

  public void testGetNodeState() throws Exception {
    CachedUrlSet cus = getCUS("http://www.example.com");
    NodeState node = nodeManager.getNodeState(cus);
    assertNotNull(node);
    assertTrue(cusEquals(cus, node.getCachedUrlSet()));
    assertNotNull(node.getCrawlState());
    assertEquals(-1, node.getCrawlState().getType());
    assertEquals(CrawlState.FINISHED, node.getCrawlState().getStatus());

    cus = getCUS("http://www.example.com/branch1");
    node = nodeManager.getNodeState(cus);
    assertNotNull(node);
    assertTrue(cusEquals(cus, node.getCachedUrlSet()));

    cus = getCUS("http://www.example.com/branch2/file2");
    node = nodeManager.getNodeState(cus);
    assertNotNull(node);
    assertTrue(cusEquals(cus, node.getCachedUrlSet()));

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
    cus = getCUS("http://www.example.com/branch2/file1");
    node = nodeManager.getNodeState(cus);
    node.getCrawlState().type = CrawlState.BACKGROUND_CRAWL;
    node.getCrawlState().status = CrawlState.RUNNING;
    nodeIt = nodeManager.getActiveCrawledNodes(mau.getAUCachedUrlSet());
    ArrayList nodeL = new ArrayList(2);
    while (nodeIt.hasNext()) {
      node = (NodeState)nodeIt.next();
      nodeL.add(nodeManager.getCusKey(node.getCachedUrlSet()));
    }
    String[] expectedA = new String[] {
      "http://www.example.com/branch1",
      "http://www.example.com/branch2/file1"
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
    cus = getCUS("http://www.example.com/branch2/file1");
    node = nodeManager.getNodeState(cus);
    ((NodeStateImpl)node).addPollState(new PollState(Poll.NAME_POLL, "",
        PollState.WON, 123, null));
    nodeIt = nodeManager.getFilteredPolledNodes(mau.getAUCachedUrlSet(),
        PollState.RUNNING + PollState.WON);
    ArrayList nodeL = new ArrayList(2);
    while (nodeIt.hasNext()) {
      node = (NodeState)nodeIt.next();
      nodeL.add(nodeManager.getCusKey(node.getCachedUrlSet()));
    }
    String[] expectedA = new String[] {
      "http://www.example.com/branch1",
      "http://www.example.com/branch2/file1"
    };
    assertIsomorphic(expectedA, nodeL);

    nodeIt = nodeManager.getFilteredPolledNodes(mau.getAUCachedUrlSet(),
        PollState.RUNNING);
    nodeL = new ArrayList(1);
    while (nodeIt.hasNext()) {
      node = (NodeState)nodeIt.next();
      nodeL.add(nodeManager.getCusKey(node.getCachedUrlSet()));
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


  public void testGetKeys() throws Exception {
    mau.setPluginId("mock2");
    mau.setAuId("none2");
    String expectedStr = "mock2:none2";
    assertEquals(expectedStr, nodeManager.getAuKey(mau));

    CachedUrlSet mcus = getCUS("http://www.foo.com");
    expectedStr = "http://www.foo.com";
    assertEquals(expectedStr, nodeManager.getCusKey(mcus));
  }

  public void testMapErrorCodes() {
    assertEquals(PollState.ERR_HASHING, nodeManager.mapResultsErrorToPollError(Poll.ERR_HASHING));
    assertEquals(PollState.ERR_IO, nodeManager.mapResultsErrorToPollError(Poll.ERR_IO));
    assertEquals(PollState.ERR_NO_QUORUM, nodeManager.mapResultsErrorToPollError(Poll.ERR_NO_QUORUM));
    assertEquals(PollState.ERR_SCHEDULE_HASH, nodeManager.mapResultsErrorToPollError(Poll.ERR_SCHEDULE_HASH));
    assertEquals(PollState.ERR_UNDEFINED, nodeManager.mapResultsErrorToPollError(1));
  }

  private boolean cusEquals(CachedUrlSet cus1, CachedUrlSet cus2) {
    String url1 = (String)cus1.getSpec().getPrefixList().get(0);
    String url2 = (String)cus2.getSpec().getPrefixList().get(0);
    return url1.equals(url2);
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
        String f_url = b_url + File.separator + "file"+ ix;
        MockCachedUrl cu = new MockCachedUrl(f_url);
        files.add(cu);
        subFiles.add(cu);
        MockCachedUrlSet subMcus = new MockCachedUrlSet(mau,
            new RECachedUrlSetSpec(f_url, (String)null));
        subBranches.add(subMcus);
        subMcus.setFlatIterator(new ArrayList().iterator());
        subMcus.setLeafIterator(new ArrayList().iterator());
      }
      mcus.setFlatIterator(subBranches.iterator());
      mcus.setLeafIterator(subFiles.iterator());
      branches.add(mcus);
    }
    MockCachedUrlSet cus = new MockCachedUrlSet(mau,
        new RECachedUrlSetSpec(startUrl, (String)null));
    cus.setLeafIterator(files.iterator());
    cus.setFlatIterator(branches.iterator());
    return cus;
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestNodeManagerImpl.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
