/*
 * $Id: TestRepositoryManager.java,v 1.1 2004-08-22 02:05:49 tlipkis Exp $
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

package org.lockss.repository;

import java.util.*;
import org.lockss.app.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

public class TestRepositoryManager extends LockssTestCase {
  private MockArchivalUnit mau;
  private RepositoryManager mgr;

  private MockLockssDaemon theDaemon;

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = getMockLockssDaemon();
    mgr = new RepositoryManager();
    theDaemon.setRepositoryManager(mgr);
    mgr.initService(theDaemon);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  MockLockssRepositoryImpl makeRepo(String root) {
    MockArchivalUnit mau = new MockArchivalUnit();
    MockLockssRepositoryImpl repo = new MockLockssRepositoryImpl(root);
    theDaemon.setLockssRepository(repo, mau);
    repo.initService(theDaemon);
    repo.startService();
    return repo;
  }

  public void testConfig() throws Exception {
    MockLockssRepositoryImpl repo1 = makeRepo("foo");
    assertEquals(RepositoryManager.DEFAULT_MAX_LRUMAP_SIZE,
		 repo1.nodeCacheSize);

    ConfigurationUtil.setFromArgs(RepositoryManager.PARAM_MAX_LRUMAP_SIZE,
				  "4");
    MockLockssRepositoryImpl repo2 = makeRepo("bar");
    assertEquals(4, repo1.nodeCacheSize);
    assertEquals(4, repo2.nodeCacheSize);

    repo1.cnt = 0;
    ConfigurationUtil.setFromArgs(RepositoryManager.PARAM_MAX_LRUMAP_SIZE,
				  "37");
    assertEquals(37, repo1.nodeCacheSize);
    assertEquals(37, repo2.nodeCacheSize);
    assertEquals(1, repo1.cnt);
    // ensure setNodeCacheSize doesn't get called if param doesn't change
    ConfigurationUtil.setFromArgs(RepositoryManager.PARAM_MAX_LRUMAP_SIZE,
				  "37",
				  "org.lockss.somethingElse", "bar");
    assertEquals(1, repo1.cnt);
  }

  class MockLockssRepositoryImpl extends LockssRepositoryImpl {
    int nodeCacheSize = 0;
    int cnt = 0;

    public MockLockssRepositoryImpl(String root) {
      super(root);
    }

    public void setNodeCacheSize(int size) {
      nodeCacheSize = size;
      cnt++;
    }
  }
}
