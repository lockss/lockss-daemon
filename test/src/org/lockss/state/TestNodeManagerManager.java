/*
 * $Id: TestNodeManagerManager.java,v 1.2 2004-10-18 03:40:31 tlipkis Exp $
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
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.repository.*;

public class TestNodeManagerManager extends LockssTestCase {
  private MockArchivalUnit mau;
  private NodeManagerManager mgr;

  private MockLockssDaemon theDaemon;

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = getMockLockssDaemon();
    mgr = new NodeManagerManager();
    theDaemon.setNodeManagerManager(mgr);
    mgr.initService(theDaemon);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  MyMockNodeManager makeNodeMgr() {
    MockArchivalUnit mau = new MockArchivalUnit();
    MyMockNodeManager nodeMgr = new MyMockNodeManager(mau);
    theDaemon.setNodeManager(nodeMgr, mau);
    nodeMgr.initService(theDaemon);
    nodeMgr.startService();
    return nodeMgr;
  }

  void setConfig(String param, String val) throws Exception {
    setConfig(param, val, null, null);
  }

  void setConfig(String param1, String val1, String param2, String val2)
      throws Exception {
    Properties p = new Properties();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    p.put(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    if (param1 != null) {
      p.put(param1, val1);
    }
    if (param2 != null) {
      p.put(param2, val2);
    }
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }

  public void testConfig() throws Exception {
    setConfig(null, null);
    MyMockNodeManager nm1 = makeNodeMgr();
    assertEquals(NodeManagerManager.DEFAULT_MAX_PER_AU_CACHE_SIZE,
		 nm1.nodeStateCacheSize);

    setConfig(NodeManagerManager.PARAM_MAX_PER_AU_CACHE_SIZE, "4");
    MyMockNodeManager nm2 = makeNodeMgr();
    assertEquals(4, nm1.nodeStateCacheSize);
    assertEquals(4, nm2.nodeStateCacheSize);

    nm1.cnt = 0;
    setConfig(NodeManagerManager.PARAM_MAX_PER_AU_CACHE_SIZE, "37");
    assertEquals(37, nm1.nodeStateCacheSize);
    assertEquals(37, nm2.nodeStateCacheSize);
    assertEquals(1, nm1.cnt);
    // ensure setNodeCacheSize doesn't get called if param doesn't change
    setConfig(NodeManagerManager.PARAM_MAX_PER_AU_CACHE_SIZE, "37",
	      "org.lockss.somethingElse", "bar");
    assertEquals(1, nm1.cnt);
  }

  class MyMockNodeManager extends NodeManagerImpl {
    int nodeStateCacheSize = 0;
    int cnt = 0;

    public MyMockNodeManager(ArchivalUnit au) {
      super(au);
    }

    public void startService() {
      super.startService();
      nodeStateCacheSize = nodeMgrMgr.paramNodeStateCacheSize;
    }

    public void setNodeStateCacheSize(int size) {
      nodeStateCacheSize = size;
      cnt++;
    }
  }
}
