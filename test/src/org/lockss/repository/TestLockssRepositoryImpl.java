/*
 * $Id: TestLockssRepositoryImpl.java,v 1.9 2002-11-23 03:40:49 aalto Exp $
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

package org.lockss.repository;

import java.io.*;
import java.util.*;
import java.net.MalformedURLException;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.Configuration;
import org.lockss.daemon.TestConfiguration;

/**
 * This is the test class for org.lockss.daemon.LockssRepositoryImpl
 */

public class TestLockssRepositoryImpl extends LockssTestCase {
  private LockssRepository repo;

  public TestLockssRepositoryImpl(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = "";
    try {
      tempDirPath = super.getTempDir().getAbsolutePath() + File.separator;
    } catch (Exception e) { fail("Couldn't get tempDir."); }
    configCacheLocation(tempDirPath);
    MockArchivalUnit mau = new MockArchivalUnit(null);
    repo = LockssRepositoryImpl.repositoryFactory(mau);
  }

  public void testGetRepositoryEntry() throws Exception {
    createLeaf("http://www.example.com/testDir/branch1/leaf1",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch1/leaf2",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch2/leaf3",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf4", "test stream", null);

    RepositoryNode node =
        repo.getRepositoryNode("http://www.example.com/testDir");
    assertTrue(!node.hasContent());
    node = repo.getRepositoryNode("http://www.example.com/testDir/branch1");
    assertTrue(!node.hasContent());
    node =
        repo.getRepositoryNode("http://www.example.com/testDir/branch2/leaf3");
    assertTrue(node.hasContent());
    node = repo.getRepositoryNode("http://www.example.com/testDir/leaf4");
    assertTrue(node.hasContent());
  }

  public void testCaching() throws Exception {
    createLeaf("http://www.example.com/testDir/leaf1", null, null);
    createLeaf("http://www.example.com/testDir/leaf2", null, null);

    RepositoryNode leaf =
        repo.getRepositoryNode("http://www.example.com/testDir/leaf1");
    try {
      leaf.sealNewVersion();
      fail("Should have thrown UnsupportedOperationException");
    } catch (UnsupportedOperationException uoe) { }
    leaf = repo.getRepositoryNode("http://www.example.com/testDir/leaf2");
    RepositoryNode leaf2 =
        repo.getRepositoryNode("http://www.example.com/testDir/leaf2");
    assertEquals(leaf, leaf2);
    try {
      leaf.makeNewVersion();
      TestRepositoryNodeImpl.writeToLeaf(leaf, "test stream");
      leaf.setNewProperties(new Properties());
      leaf2.sealNewVersion();
    } catch (UnsupportedOperationException uoe) {
      fail("Leaf2 couldn't finish leaf's version.");
    }
  }

  public void testWeakReferenceCaching() throws Exception {
    createLeaf("http://www.example.com/testDir/leaf1", null, null);

    RepositoryNode leaf =
        repo.getRepositoryNode("http://www.example.com/testDir/leaf1");
    RepositoryNode leaf2 = null;
    for (int ii=0; ii<LockssRepositoryImpl.MAX_LRUMAP_SIZE; ii++) {
      createLeaf("http://www.example.com/testDir/testleaf"+ii, null, null);
    }
    leaf2 = repo.getRepositoryNode("http://www.example.com/testDir/leaf1");
    assertEquals(leaf, leaf2);
  }

  public void testMapUrlToCacheLocation() throws MalformedURLException {
    String testStr = "http://www.example.com/branch1/branch2/index.html";
    String expectedStr = LockssRepositoryImpl.CACHE_ROOT_NAME +
                         "/www.example.com/http/branch1/branch2/index.html";
    assertEquals(expectedStr,
                 LockssRepositoryImpl.mapUrlToCacheLocation(testStr));

    try {
      testStr = ":/brokenurl.com/branch1/index/";
      LockssRepositoryImpl.mapUrlToCacheLocation(testStr);
      fail("Should have thrown MalformedURLException");
    } catch (MalformedURLException mue) { }
  }

  public static void configCacheLocation(String location)
    throws IOException {
    String s = Configuration.PREFIX + "cache.location=" + location;
    TestConfiguration.setCurrentConfigFromUrlList(ListUtil.list(FileUtil.urlOfString(s)));
  }

  private RepositoryNode createLeaf(String url, String content,
                                    Properties props) throws Exception {
    return TestRepositoryNodeImpl.createLeaf(repo, url, content, props);
  }

}
