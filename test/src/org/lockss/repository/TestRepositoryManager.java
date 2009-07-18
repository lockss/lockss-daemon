/*
 * $Id: TestRepositoryManager.java,v 1.8.24.1 2009-07-18 01:28:28 edwardsb1 Exp $
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
  private MyRepositoryManager mgr;

  private MockLockssDaemon theDaemon;

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = getMockLockssDaemon();
    mgr = new MyRepositoryManager();
    theDaemon.setRepositoryManager(mgr);
    mgr.initService(theDaemon);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  MyMockLockssRepositoryImpl makeRepo(String root) {
    MockArchivalUnit mau = new MockArchivalUnit();
    MyMockLockssRepositoryImpl repo = new MyMockLockssRepositoryImpl(root);
    theDaemon.setLockssRepository(repo, mau);
    repo.initService(theDaemon);
    repo.startService();
    return repo;
  }

  public void testConfig() throws Exception {
    MyMockLockssRepositoryImpl repo1 = makeRepo("foo");
    assertEquals(RepositoryManager.DEFAULT_MAX_PER_AU_CACHE_SIZE,
		 repo1.nodeCacheSize);

    ConfigurationUtil.setFromArgs(RepositoryManager.PARAM_MAX_PER_AU_CACHE_SIZE,
				  "4");
    MyMockLockssRepositoryImpl repo2 = makeRepo("bar");
    assertEquals(4, repo1.nodeCacheSize);
    assertEquals(4, repo2.nodeCacheSize);

    repo1.cnt = 0;
    ConfigurationUtil.setFromArgs(RepositoryManager.PARAM_MAX_PER_AU_CACHE_SIZE,
				  "37");
    assertEquals(37, repo1.nodeCacheSize);
    assertEquals(37, repo2.nodeCacheSize);
    assertEquals(1, repo1.cnt);
    // ensure setNodeCacheSize doesn't get called if param doesn't change
    ConfigurationUtil.setFromArgs(RepositoryManager.PARAM_MAX_PER_AU_CACHE_SIZE,
				  "37",
				  "org.lockss.somethingElse", "bar");
    assertEquals(1, repo1.cnt);

    PlatformUtil.DF warn = mgr.getDiskWarnThreshold();
    PlatformUtil.DF full = mgr.getDiskFullThreshold();
    assertEquals(5000 * 1024, warn.getAvail());
    assertEquals(0.98, warn.getPercent(), .00001);
    assertEquals(100 * 1024, full.getAvail());
    assertEquals(0.99, full.getPercent(), .00001);

    Properties p = new Properties();
    p.put(RepositoryManager.PARAM_DISK_WARN_FRRE_MB, "17");
    p.put(RepositoryManager.PARAM_DISK_WARN_FRRE_PERCENT, "20");
    p.put(RepositoryManager.PARAM_DISK_FULL_FRRE_MB, "7");
    p.put(RepositoryManager.PARAM_DISK_FULL_FRRE_PERCENT, "10");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    warn = mgr.getDiskWarnThreshold();
    full = mgr.getDiskFullThreshold();
    assertEquals(17 * 1024, warn.getAvail());
    assertEquals(0.80, warn.getPercent(), .00001);
    assertEquals(7 * 1024, full.getAvail());
    assertEquals(0.90, full.getPercent(), .00001);
  }

  public void testGetRepositoryList() throws Exception {
    assertEmpty(mgr.getRepositoryList());
    ConfigurationUtil.setFromArgs("org.lockss.platform.diskSpacePaths",
				  "/foo/bar");
    assertEquals(ListUtil.list("local:/foo/bar"), mgr.getRepositoryList());
    ConfigurationUtil.setFromArgs("org.lockss.platform.diskSpacePaths",
				  "/foo/bar;/cache2");
    assertEquals(ListUtil.list("local:/foo/bar", "local:/cache2"),
		 mgr.getRepositoryList());
  }

  public void testGetRepositoryDF () throws Exception {
    PlatformUtil.DF df = mgr.getRepositoryDF("local:.");
    assertNotNull(df);
  }

  public void testFindLeastFullRepository () throws Exception {
    Map repoMap = MapUtil.map("local:one", new MyDF("/one", 1000),
			      "local:two",  new MyDF("/two", 3000),
			      "local:three",  new MyDF("/three", 2000));
    mgr.setRepoMap(repoMap);

    assertEquals("local:two", mgr.findLeastFullRepository());
  }

  public void testSizeCalc () throws Exception {
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    mgr.setSem(sem);
    RepositoryNode node1 = new RepositoryNodeImpl("url1", "testDir", null, mau);
    RepositoryNode node2 = new RepositoryNodeImpl("url2", "testDir", null, mau);
    RepositoryNode node3 = new RepositoryNodeImpl("url3", "testDir", null, mau);
    mgr.queueSizeCalc(node1);
    assertTrue(sem.take(TIMEOUT_SHOULDNT));
    assertEquals(ListUtil.list(node1), mgr.getNodes());
    mgr.queueSizeCalc(node2);
    mgr.queueSizeCalc(node3);
    assertTrue(sem.take(TIMEOUT_SHOULDNT));
    if (mgr.getNodes().size() < 3) {
      assertTrue(sem.take(TIMEOUT_SHOULDNT));
    }
    assertSameElements(ListUtil.list(node1, node2, node3), mgr.getNodes());
  }

  public void testSleepCalc () throws Exception {
    assertEquals(90, mgr.sleepTimeToAchieveLoad(10L, .1F));
    assertEquals(40, mgr.sleepTimeToAchieveLoad(10L, .2F));
    assertEquals(10, mgr.sleepTimeToAchieveLoad(10L, .5F));
    assertEquals(50, mgr.sleepTimeToAchieveLoad(150L, .75F));
  }

  class MyRepositoryManager extends RepositoryManager {
    List nodes = new ArrayList();
    SimpleBinarySemaphore sem;
    List repos;
    Map repoMap;

    void setSem(SimpleBinarySemaphore sem) {
      this.sem = sem;
    }
    List getNodes() {
      return nodes;
    }
    void doSizeCalc(RepositoryNode node) {
      TimerUtil.guaranteedSleep(10);
      nodes.add(node);
      sem.give();
    }    
    public List<String> getRepositoryList() {
      if (repos != null) return repos;
      return super.getRepositoryList();
    }
    public void setRepos(List repos) {
      this.repos = repos;
    }
    public PlatformUtil.DF getRepositoryDF(String repoName) {
      if (repoMap != null) return (PlatformUtil.DF)repoMap.get(repoName);
      return super.getRepositoryDF(repoName);
    }
    public void setRepoMap(Map<String,PlatformUtil.DF> repoMap) {
      List repos = new ArrayList();
      this.repoMap = repoMap;
      for (String repo : repoMap.keySet()) {
	repos.add(repo);
      }
      setRepos(repos);
    }
  }

  class MyMockLockssRepositoryImpl extends LockssRepositoryImpl {
    int nodeCacheSize = 0;
    int cnt = 0;

    public MyMockLockssRepositoryImpl(String root) {
      super(root, mau);
    }

    public void setNodeCacheSize(int size) {
      nodeCacheSize = size;
      cnt++;
    }
  }
  class MyDF extends PlatformUtil.DF {
    MyDF(String path, int avail) {
      super();
      this.path = path;
      this.avail = avail;
    }
  }
}
