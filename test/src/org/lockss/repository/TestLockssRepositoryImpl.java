/*
 * $Id: TestLockssRepositoryImpl.java,v 1.13 2002-12-31 00:14:02 aalto Exp $
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
  private String tempDirPath;

  public TestLockssRepositoryImpl(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    configCacheLocation(tempDirPath);
    MockArchivalUnit mau = new MockArchivalUnit();
    repo = LockssRepositoryImpl.repositoryFactory(mau);
  }

  public void testFileLocation() throws Exception {
    tempDirPath += "cache/mock/none/";
    File testFile = new File(tempDirPath);
    assertTrue(!testFile.exists());

    createLeaf("http://www.example.com/testDir/branch1/leaf1",
               "test stream", null);
    assertTrue(testFile.exists());
    tempDirPath += "www.example.com/http/";
    testFile = new File(tempDirPath);
    assertTrue(testFile.exists());
    tempDirPath += "testDir/branch1/leaf1/";
    testFile = new File(tempDirPath);
    assertTrue(testFile.exists());
  }

  public void testGetRepositoryNode() throws Exception {
    createLeaf("http://www.example.com/testDir/branch1/leaf1",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch1/leaf2",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch2/leaf3",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf4", "test stream", null);

    RepositoryNode node =
        repo.getNode("http://www.example.com/testDir");
    assertTrue(!node.hasContent());
    assertEquals("http://www.example.com/testDir", node.getNodeUrl());
    node = repo.getNode("http://www.example.com/testDir/branch1");
    assertTrue(!node.hasContent());
    assertEquals("http://www.example.com/testDir/branch1", node.getNodeUrl());
    node =
        repo.getNode("http://www.example.com/testDir/branch2/leaf3");
    assertTrue(node.hasContent());
    assertEquals("http://www.example.com/testDir/branch2/leaf3",
                 node.getNodeUrl());
    node = repo.getNode("http://www.example.com/testDir/leaf4");
    assertTrue(node.hasContent());
    assertEquals("http://www.example.com/testDir/leaf4", node.getNodeUrl());
  }

  public void testCaching() throws Exception {
    createLeaf("http://www.example.com/testDir/leaf1", null, null);
    createLeaf("http://www.example.com/testDir/leaf2", null, null);

    LockssRepositoryImpl repoImpl = (LockssRepositoryImpl)repo;
    assertEquals(0, repoImpl.getCacheHits());
    assertEquals(2, repoImpl.getCacheMisses());
    RepositoryNode leaf =
        repo.getNode("http://www.example.com/testDir/leaf1");
    assertEquals(1, repoImpl.getCacheHits());
    RepositoryNode leaf2 =
        repo.getNode("http://www.example.com/testDir/leaf1");
    assertSame(leaf, leaf2);
    assertEquals(2, repoImpl.getCacheHits());
    assertEquals(2, repoImpl.getCacheMisses());
  }

  public void testWeakReferenceCaching() throws Exception {
    createLeaf("http://www.example.com/testDir/leaf1", null, null);

    LockssRepositoryImpl repoImpl = (LockssRepositoryImpl)repo;
    RepositoryNode leaf =
        repo.getNode("http://www.example.com/testDir/leaf1");
    RepositoryNode leaf2 = null;
    int loopSize = 1;
    int refHits = 0;
    // create leafs in a loop until fetching an leaf1 creates a cache miss
    while (true) {
      loopSize *= 2;
      for (int ii=0; ii<loopSize; ii++) {
        createLeaf("http://www.example.com/testDir/testleaf"+ii, null, null);
      }
      int misses = repoImpl.getCacheMisses();
      refHits = repoImpl.getRefHits();
      leaf2 = repo.getNode("http://www.example.com/testDir/leaf1");
      if (repoImpl.getCacheMisses() == misses+1) {
        break;
      }
    }
    assertSame(leaf, leaf2);
    assertEquals(refHits+1, repoImpl.getRefHits());
  }

  public void testMapUrlToCacheLocation() throws MalformedURLException {
    String testStr = "http://www.example.com/branch1/branch2/index.html";
    String expectedStr = "root/www.example.com/http/branch1/branch2/index.html";
    assertEquals(expectedStr,
                 FileLocationUtil.mapUrlToFileLocation("root", testStr));

    try {
      testStr = ":/brokenurl.com/branch1/index/";
      FileLocationUtil.mapUrlToFileLocation("root", testStr);
      fail("Should have thrown MalformedURLException");
    } catch (MalformedURLException mue) { }
  }

  static final String PARAM_CACHE_LOCATION =
    LockssRepositoryImpl.PARAM_CACHE_LOCATION;

  public static void configCacheLocation(String location)
    throws IOException {
    String s = PARAM_CACHE_LOCATION + "=" + location;
    TestConfiguration.setCurrentConfigFromUrlList(ListUtil.list(FileUtil.urlOfString(s)));
  }

  private RepositoryNode createLeaf(String url, String content,
                                    Properties props) throws Exception {
    return TestRepositoryNodeImpl.createLeaf(repo, url, content, props);
  }

}
