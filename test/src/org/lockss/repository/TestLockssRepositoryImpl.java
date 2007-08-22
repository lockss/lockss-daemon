/*
 * $Id: TestLockssRepositoryImpl.java,v 1.63 2007-08-22 06:47:00 tlipkis Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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
import java.net.*;
import org.lockss.test.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

/**
 * This is the test class for org.lockss.daemon.LockssRepositoryImpl
 */

public class TestLockssRepositoryImpl extends LockssTestCase {
  private static Logger logger = Logger.getLogger("LockssRepository");
  private MockLockssDaemon daemon;
  private LockssRepositoryImpl repo;
  private MockArchivalUnit mau;
  private String tempDirPath;

  public void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    mau = new MockArchivalUnit();
    repo = (LockssRepositoryImpl)LockssRepositoryImpl.createNewLockssRepository(
        mau);
    // set small node cache; one test needs to fill it up
    repo.setNodeCacheSize(17);
    repo.initService(daemon);
  }

  public void tearDown() throws Exception {
    repo.stopService();
    super.tearDown();
  }

  String getCacheLocation() {
    return LockssRepositoryImpl.getCacheLocation();
  }

  public void testGetLocalRepository() throws Exception {
    LockssRepositoryImpl.LocalRepository localRepo =
      LockssRepositoryImpl.getLocalRepository(mau);
    assertNotNull("Failed to create LocalRepository for: " + mau, localRepo);
    assertEquals(tempDirPath, localRepo.getRepositoryPath());

    String tempDir2 = getTempDir().getAbsolutePath() + File.separator;
    MockArchivalUnit mau2 = new MockArchivalUnit();
    mau2.setConfiguration(ConfigurationUtil.fromArgs
			  (PluginManager.AU_PARAM_REPOSITORY,
			   repoSpec(tempDir2)));
    LockssRepositoryImpl.LocalRepository localRepo2 =
      LockssRepositoryImpl.getLocalRepository(mau2);
    assertNotNull("Failed to create LocalRepository for: " + mau2, localRepo2);
    assertNotSame(localRepo2, localRepo);
    assertEquals(tempDir2, localRepo2.getRepositoryPath());

  }

  String repoSpec(String path) {
    return "local:" + path;
  }

  public void testGetLocalRepositoryPath() throws Exception {
    assertEquals("foo",
		 LockssRepositoryImpl.getLocalRepositoryPath("local:foo"));
    assertEquals("/cache/foo",
		 LockssRepositoryImpl.getLocalRepositoryPath("local:/cache/foo"));
    assertNull(LockssRepositoryImpl.getLocalRepositoryPath("other:foo"));
    assertNull(LockssRepositoryImpl.getLocalRepositoryPath("foo"));
  }

  public void testLocalRepository_GetAuMap() {
    Properties newProps = new Properties();
    mau.setAuId("barfoo");
    newProps.setProperty(LockssRepositoryImpl.AU_ID_PROP, mau.getAuId());
    String location = getCacheLocation() + "ab";
    LockssRepositoryImpl.saveAuIdProperties(location, newProps);

    LockssRepositoryImpl.LocalRepository localRepo =
      LockssRepositoryImpl.getLocalRepository(mau);
    localRepo.auMap = null;
    Map aumap = localRepo.getAuMap();
    assertEquals(addSlash(location), aumap.get(mau.getAuId()));
  }

  String addSlash(String s) {
    return (s.endsWith(File.separator)) ? s : s + File.separator;
  }

  public void testGetRepositoryRoot() throws Exception {
    assertEquals(tempDirPath, LockssRepositoryImpl.getRepositoryRoot(mau));

    Configuration auconf =
      ConfigurationUtil.fromArgs(PluginManager.AU_PARAM_REPOSITORY,
				 "local:/foo/bar");
    mau.setConfiguration(auconf);
    assertEquals("/foo/bar", LockssRepositoryImpl.getRepositoryRoot(mau));
  }

  // The whole point of isDirInRepository() is to resolve symbolic links,
  // but testing that would require using Runtime.exec() to create such a
  // link.  So we test only that isDirInRepository() is canonicalizing the
  // path.
  public void testIsDirInRepository() throws Exception {
    assertTrue(LockssRepositoryImpl.isDirInRepository("/foo/bar", "/foo"));
    assertTrue(LockssRepositoryImpl.isDirInRepository("/foo/bar", "/foo/"));
    assertTrue(LockssRepositoryImpl.isDirInRepository("/foo/../bar/a",
						      "/bar"));
    assertFalse(LockssRepositoryImpl.isDirInRepository("/foo/bar", "/bar"));
  }

  public void testFileLocation() throws Exception {
    String cachePath =
      LockssRepositoryImpl.mapAuToFileLocation(tempDirPath, mau);
    File testFile = new File(cachePath);

    createLeaf("http://www.example.com/testDir/branch1/leaf1",
               "test stream", null);
    assertTrue(testFile.exists());
    cachePath += "www.example.com/http/";
    testFile = new File(cachePath);
    assertTrue(testFile.exists());
    cachePath += "testDir/branch1/leaf1/";
    testFile = new File(cachePath);
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

    RepositoryNode node = repo.getNode("http://www.example.com/testDir");
    assertFalse(node.hasContent());
    assertEquals("http://www.example.com/testDir", node.getNodeUrl());
    node = repo.getNode("http://www.example.com/testDir/branch1");
    assertFalse(node.hasContent());
    assertEquals("http://www.example.com/testDir/branch1", node.getNodeUrl());
    node = repo.getNode("http://www.example.com/testDir/branch2/leaf3");
    assertTrue(node.hasContent());
    assertEquals("http://www.example.com/testDir/branch2/leaf3",
                 node.getNodeUrl());
    node = repo.getNode("http://www.example.com/testDir/leaf4");
    assertTrue(node.hasContent());
    assertEquals("http://www.example.com/testDir/leaf4", node.getNodeUrl());
  }

  public void testGetNodeWithQuery() throws Exception {
    // check if '?' allowed on system
    if (FileUtil.canWriteToFileWithChar(tempDirPath, '?')) {
      createLeaf("http://www.example.com/testDir?leaf=2",
          "test stream", null);

      RepositoryNode node = repo.getNode("http://www.example.com/testDir");
      assertNull(node);
      node = repo.getNode("http://www.example.com/testDir?leaf=2");
      assertTrue(node.hasContent());
      assertEquals("http://www.example.com/testDir?leaf=2", node.getNodeUrl());
    }
  }

  public void testGetNodeWithPort() throws Exception {
    createLeaf("http://www.example.com:22/testDir", "test stream", null);
    assertNull(repo.getNode("http://www.example.com/testDir"));
    RepositoryNode node =
      repo.getNode("http://www.example.com:22/testDir");
    assertTrue(node.hasContent());
    assertEquals("http://www.example.com:22/testDir", node.getNodeUrl());
  }

  public void testDotUrlHandling() throws Exception {
    //testing correction of nodes with bad '..'-including urls,
    //filtering the first '..' but resolving the second
    RepositoryNode node = repo.createNewNode(
        "http://www.example.com/branch/test/../test2");
    assertEquals("http://www.example.com/branch/test2", node.getNodeUrl());

    //remove single '.' references
    node = repo.createNewNode(
        "http://www.example.com/branch/./test/");
    assertEquals("http://www.example.com/branch/test", node.getNodeUrl());

    try {
      node = repo.createNewNode("http://www.example.com/..");
      fail("Should have thrown MalformedURLException.");
    } catch (MalformedURLException mue) { }
    try {
      node = repo.createNewNode("http://www.example.com/test/../../test2");
      fail("Should have thrown MalformedURLException.");
    } catch (MalformedURLException mue) { }
  }

  public void testCanonicalizePath() throws Exception {
    assertEquals("http://www.example.com/test",
		 repo.canonicalizePath("http://www.example.com/test/"));
    assertEquals("http://foo.com/test",
		 repo.canonicalizePath("http://foo.com/bar/../test/"));
    assertEquals("http://foo.com:20/test",
		 repo.canonicalizePath("http://foo.com:20/test/"));
    assertEquals("http://foo.com:20/test",
		 repo.canonicalizePath("http://foo.com:20/bar/../test/"));
  }

  public void testGetAuNode() throws Exception {
    createLeaf("http://www.example.com/testDir1/leaf1", "test stream", null);
    createLeaf("http://www.example.com/testDir2/leaf2", "test stream", null);
    createLeaf("http://image.example.com/testDir3/leaf3", "test stream", null);
    createLeaf("ftp://www.example.com/file", "test stream", null);

    RepositoryNode auNode = repo.getNode(AuUrl.PROTOCOL_COLON+"//www.example.com");
    assertFalse(auNode.hasContent());
    assertEquals(AuUrl.PROTOCOL, auNode.getNodeUrl());
    Iterator childIt = auNode.listChildren(null, false);
    ArrayList childL = new ArrayList(3);
    while (childIt.hasNext()) {
      RepositoryNode node = (RepositoryNode)childIt.next();
      childL.add(node.getNodeUrl());
    }
    String[] expectedA = new String[] {
      "ftp://www.example.com",
      "http://image.example.com",
      "http://www.example.com",
      };
    assertIsomorphic(expectedA, childL);
  }

  public void testDeleteNode() throws Exception {
    createLeaf("http://www.example.com/test1", "test stream", null);

    RepositoryNode node = repo.getNode("http://www.example.com/test1");
    assertTrue(node.hasContent());
    assertFalse(node.isDeleted());
    repo.deleteNode("http://www.example.com/test1");
    assertFalse(node.hasContent());
    assertTrue(node.isDeleted());
  }

  public void testDeactivateNode() throws Exception {
    createLeaf("http://www.example.com/test1", "test stream", null);

    RepositoryNode node = repo.getNode("http://www.example.com/test1");
    assertTrue(node.hasContent());
    assertFalse(node.isContentInactive());
    repo.deactivateNode("http://www.example.com/test1");
    assertFalse(node.hasContent());
    assertTrue(node.isContentInactive());
  }

  public void testCaching() throws Exception {
    createLeaf("http://www.example.com/testDir/leaf1", null, null);
    createLeaf("http://www.example.com/testDir/leaf2", null, null);
    LockssRepositoryImpl repoImpl = (LockssRepositoryImpl)repo;

    // initial values are strange because creating each child node
    // causes invalidateCachedValues() to be called nodes up to the root
    int hits = repoImpl.getCacheHits();
    int misses = repoImpl.getCacheMisses();
    RepositoryNode leaf = repo.getNode("http://www.example.com/testDir/leaf1");
    assertEquals(hits + 1, repoImpl.getCacheHits());
    RepositoryNode leaf2 = repo.getNode("http://www.example.com/testDir/leaf1");
    assertSame(leaf, leaf2);
    assertEquals(hits + 2, repoImpl.getCacheHits());
    assertEquals(misses, repoImpl.getCacheMisses());
  }

  public void testWeakReferenceCaching() throws Exception {
    createLeaf("http://www.example.com/testDir/leaf1", null, null);

    LockssRepositoryImpl repoImpl = (LockssRepositoryImpl)repo;
    RepositoryNode leaf = repo.getNode("http://www.example.com/testDir/leaf1");
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

  public void testConsistencyCheck() throws Exception {
    createLeaf("http://www.example.com/testDir/leaf1", "test stream", null);

    RepositoryNodeImpl leaf = (RepositoryNodeImpl)
        repo.getNode("http://www.example.com/testDir/leaf1");
    assertTrue(leaf.hasContent());

    // delete content directory
    leaf.currentCacheFile.delete();
    // version still indicates content
    assertEquals(1, leaf.getCurrentVersion());

    try {
      leaf.getNodeContents().getInputStream();
      fail("Should have thrown state exception.");
    } catch (LockssRepository.RepositoryStateException rse) { }

    assertTrue(leaf.contentDir.exists());
    assertEquals(RepositoryNodeImpl.INACTIVE_VERSION, leaf.getCurrentVersion());
    assertFalse(leaf.hasContent());
  }

  public void testRecursiveConsistencyCheck() throws Exception {
    createLeaf("http://www.example.com", "test stream", null);
    createLeaf("http://www.example.com/testDir", "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf", "test stream", null);

    RepositoryNodeImpl dir = (RepositoryNodeImpl)
      repo.getNode("http://www.example.com");

    // set leaves inconsistent
    RepositoryNodeImpl leaf = (RepositoryNodeImpl)
      repo.getNode("http://www.example.com/testDir");
    MyMockRepositoryNode mockLeaf = new MyMockRepositoryNode(leaf);
    mockLeaf.isConsistent = false;
    repo.nodeCache.put("http://www.example.com/testDir", mockLeaf);
    // set leaf inconsistent
    leaf = (RepositoryNodeImpl)
        repo.getNode("http://www.example.com/testDir/leaf");
    MyMockRepositoryNode mockLeaf2 = new MyMockRepositoryNode(leaf);
    mockLeaf.isConsistent = false;
    repo.nodeCache.put("http://www.example.com/testDir/leaf", mockLeaf);

    // everything starts active
    assertFalse(dir.isContentInactive());
    assertFalse(mockLeaf.isContentInactive());
    assertFalse(mockLeaf2.isContentInactive());

    // run check
    repo.nodeConsistencyCheck();

    // leaf, but not its child, should be deactivated
    assertFalse(dir.isContentInactive());
    assertTrue(mockLeaf.isContentInactive());
    assertFalse(mockLeaf2.isContentInactive());
  }

  // test static naming calls

  public void testGetNextDirName() {
    assertEquals("a", LockssRepositoryImpl.getNextDirName(""));
    assertEquals("b", LockssRepositoryImpl.getNextDirName("a"));
    assertEquals("c", LockssRepositoryImpl.getNextDirName("b"));
    assertEquals("z", LockssRepositoryImpl.getNextDirName("y"));
    assertEquals("aa", LockssRepositoryImpl.getNextDirName("z"));
    assertEquals("ab", LockssRepositoryImpl.getNextDirName("aa"));
    assertEquals("ba", LockssRepositoryImpl.getNextDirName("az"));
    assertEquals("aaa", LockssRepositoryImpl.getNextDirName("zz"));
  }

  public void testGetAuDirFromMap() {
    LockssRepositoryImpl.LocalRepository localRepo =
      LockssRepositoryImpl.getLocalRepository("/foo");
    Map aumap = localRepo.getAuMap();
    aumap.put(mau.getAuId(), "/foo/bar/testDir");
    assertEquals("/foo/bar/testDir",
		 LockssRepositoryImpl.getAuDir(mau, "/foo", false));
  }

  public void testGetAuDirFromMapNoCacheWrongRepo() {
    LockssRepositoryImpl.LocalRepository localRepo =
      LockssRepositoryImpl.getLocalRepository("/foo");
    Map aumap = localRepo.getAuMap();
    aumap.put(mau.getAuId(), "/foo/bar/testDir");
    assertNull(LockssRepositoryImpl.getAuDir(mau, "/other/repo", false));
    assertEquals("/foo/bar/testDir",
		 LockssRepositoryImpl.getAuDir(mau, "/foo", false));
    assertNull(LockssRepositoryImpl.getAuDir(mau, "/other/repo", false));
  }

  public void testGetAuDirNoCreate() {
    mau.setAuId("foobar23");
    assertNull(LockssRepositoryImpl.getAuDir(mau, "", false));
  }

  public void testSaveAndLoadNames() {
    String location =
      LockssRepositoryImpl.mapAuToFileLocation(tempDirPath, mau);

    File idFile = new File(location + LockssRepositoryImpl.AU_ID_FILE);
    assertTrue(idFile.exists());

    Properties props = LockssRepositoryImpl.getAuIdProperties(location);
    assertNotNull(props);
    assertEquals(mau.getAuId(),
                 props.getProperty(LockssRepositoryImpl.AU_ID_PROP));
  }

  public void testMapAuToFileLocation() {
    LockssRepositoryImpl.lastPluginDir = "ba";
    String expectedStr = getCacheLocation() + "bb/";
    assertEquals(FileUtil.sysDepPath(expectedStr),
		 LockssRepositoryImpl.mapAuToFileLocation(tempDirPath,
							  new MockArchivalUnit()));
  }

  public void testDoesAuDirExist() {
    MockArchivalUnit mau = new MockArchivalUnit();
    String auid = "sdflkjsd";
    mau.setAuId(auid);
    assertFalse(LockssRepositoryImpl.doesAuDirExist(auid, tempDirPath));
    // ensure asking doesn't create it
    assertFalse(LockssRepositoryImpl.doesAuDirExist(auid, tempDirPath));
    LockssRepositoryImpl.lastPluginDir = "ga";
    String expectedStr = getCacheLocation() + "gb/";
    assertEquals(FileUtil.sysDepPath(expectedStr),
		 LockssRepositoryImpl.mapAuToFileLocation(tempDirPath, mau));
    assertTrue(LockssRepositoryImpl.doesAuDirExist(auid, tempDirPath));
  }

  public void testGetAuDirSkipping() {
    String location = getCacheLocation() + "ab";
    File dirFile = new File(location);
    dirFile.mkdirs();

    LockssRepositoryImpl.lastPluginDir = "aa";
    String expectedStr = getCacheLocation() + "ac/";
    assertEquals(FileUtil.sysDepPath(expectedStr),
                 LockssRepositoryImpl.mapAuToFileLocation(tempDirPath,
							  new MockArchivalUnit()));
  }

  public void testMapUrlToFileLocation() throws MalformedURLException {
    String testStr = "http://www.example.com/branch1/branch2/index.html";
    String expectedStr = "root/www.example.com/http/branch1/branch2/index.html";
    assertEquals(FileUtil.sysDepPath(expectedStr),
                 LockssRepositoryImpl.mapUrlToFileLocation("root", testStr));

    testStr = "hTTp://www.exaMPLE.com/branch1/branch2/index.html";
    expectedStr = "root/www.example.com/http/branch1/branch2/index.html";
    assertEquals(FileUtil.sysDepPath(expectedStr),
                 LockssRepositoryImpl.mapUrlToFileLocation("root", testStr));

    try {
      testStr = ":/brokenurl.com/branch1/index/";
      LockssRepositoryImpl.mapUrlToFileLocation("root", testStr);
      fail("Should have thrown MalformedURLException");
    } catch (MalformedURLException mue) {}
  }

  public void testCharacterEscaping() throws MalformedURLException {
    String testStr = "http://www.example.com/"+UrlUtil.encodeUrl("#")+"nodestate.xml";
    String expectedStr = "root/www.example.com/http/##nodestate.xml";
//    assertEquals(FileUtil.sysDepPath(expectedStr),
  //               LockssRepositoryImpl.mapUrlToFileLocation("root", testStr));
    assertEquals("root/www.example.com/http/#nodestate.xml",
                 LockssRepositoryImpl.unescape(expectedStr));

    testStr = "http://www.example.com/index.html?leaf=bad"+File.separator+
        "query"+File.separator;
    expectedStr = "root/www.example.com/http/index.html?leaf=bad#squery#s";
    assertEquals(FileUtil.sysDepPath(expectedStr),
                 LockssRepositoryImpl.mapUrlToFileLocation("root", testStr));
    assertEquals("root/www.example.com/http/index.html?leaf=bad"+
                 File.separator+"query"+File.separator,
                 LockssRepositoryImpl.unescape(expectedStr));
  }

  private RepositoryNode createLeaf(String url, String content,
                                    Properties props) throws Exception {
    return TestRepositoryNodeImpl.createLeaf(repo, url, content, props);
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestLockssRepositoryImpl.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

  /**
   * This class overrides 'checkNodeConsistency()' so I can manipulate it.
   */
  static class MyMockRepositoryNode extends RepositoryNodeImpl {
    boolean isConsistent = true;
    MyMockRepositoryNode(RepositoryNodeImpl nodeImpl) {
      super(nodeImpl.url, nodeImpl.nodeLocation, nodeImpl.repository);
    }

    boolean checkNodeConsistency() {
      return isConsistent;
    }
  }
}
