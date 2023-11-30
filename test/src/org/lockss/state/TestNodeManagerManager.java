/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.state;

import java.io.*;
import java.util.*;

import org.lockss.test.*;
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
      nodeStateCacheSize = getNodeManagerManager().paramNodeStateCacheSize;
    }

    public void setNodeStateCacheSize(int size) {
      nodeStateCacheSize = size;
      cnt++;
    }
  }
}
