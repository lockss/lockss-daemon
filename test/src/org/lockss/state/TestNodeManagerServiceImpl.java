/*
 * $Id: TestNodeManagerServiceImpl.java,v 1.6 2003-03-24 23:52:24 aalto Exp $
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

import java.io.File;
import java.util.Vector;
import org.lockss.test.*;
import org.lockss.util.Logger;
import org.lockss.daemon.RangeCachedUrlSetSpec;
import org.lockss.repository.LockssRepositoryServiceImpl;
import org.lockss.daemon.TestConfiguration;

public class TestNodeManagerServiceImpl extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  private NodeManagerService nms;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    String s = LockssRepositoryServiceImpl.PARAM_CACHE_LOCATION + "=" +
        tempDirPath + "\n" + HistoryRepositoryImpl.PARAM_HISTORY_LOCATION +
        "=" + tempDirPath;
    TestConfiguration.setCurrentConfigFromString(s);

    mau = new MockArchivalUnit();
    MockCachedUrlSet mcus =
        new MockCachedUrlSet(mau, new RangeCachedUrlSetSpec("none"));
    mcus.setFlatItSource(new Vector());
    mau.setAUCachedUrlSet(mcus);

    theDaemon = new MockLockssDaemon();
    theDaemon.getLockssRepositoryService().startService();
    theDaemon.setHistoryRepository(new HistoryRepositoryImpl(tempDirPath));

    nms = new NodeManagerServiceImpl();
    nms.initService(theDaemon);
  }

  public void tearDown() throws Exception {
    nms.stopService();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  public void testGetNodeManager() {
    String auId = mau.getAUId();

    try {
      nms.getNodeManager(mau);
      fail("Should throw IllegalArgumentException.");
    } catch (IllegalArgumentException iae) { }

    nms.addNodeManager(mau);
    NodeManager node1 = nms.getNodeManager(mau);
    assertNotNull(node1);

    mau.setAuId(auId + "test");
    nms.addNodeManager(mau);
    NodeManager node2 = nms.getNodeManager(mau);
    assertNotSame(node1, node2);

    mau.setAuId(auId);
    node2 = nms.getNodeManager(mau);
    assertSame(node1, node2);

    mau.setAuId(auId + "test2");
    try {
      nms.getNodeManager(mau);
      fail("Should throw IllegalArgumentException.");
    } catch (IllegalArgumentException iae) { }
  }

  public void testNodeManagerStartAndStop() {
    nms.addNodeManager(mau);
    NodeManagerImpl node = (NodeManagerImpl)nms.getNodeManager(mau);
    // if the NodeManagerImpl has started, the thread is non-null
    assertNotNull(node.treeWalkThread);

    nms.stopService();
    // once stopped, the thread should be null
    assertNull(node.treeWalkThread);
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestNodeManagerServiceImpl.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
