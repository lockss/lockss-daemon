/*
 * $Id: TestLockssRepositoryImpl.java,v 1.42 2004-03-27 02:37:24 eaalto Exp $
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

import java.io.*;
import java.util.*;
import java.net.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

/**
 * This is the test class for org.lockss.daemon.LockssRepositoryImpl
 */

public class TestLockssRepositoryImpl extends LockssTestCase {
  private LockssRepositoryImpl repo;
  private MockArchivalUnit mau;
  private String tempDirPath;

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    mau = new MockArchivalUnit();
    repo = (LockssRepositoryImpl)LockssRepositoryImpl.createNewLockssRepository(
        mau);
  }

  public void tearDown() throws Exception {
    repo.stopService();
    super.tearDown();
  }

  public void testFileLocation() throws Exception {
    String cachePath = LockssRepositoryImpl.mapAuToFileLocation(
        LockssRepositoryImpl.extendCacheLocation(tempDirPath),
        mau);
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
    createLeaf("http://www.example.com/testDir?leaf=2",
               "test stream", null);

    RepositoryNode node = repo.getNode("http://www.example.com/testDir");
    assertNull(node);
    node = repo.getNode("http://www.example.com/testDir?leaf=2");
    assertTrue(node.hasContent());
    assertEquals("http://www.example.com/testDir?leaf=2", node.getNodeUrl());
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
    assertEquals(0, repoImpl.getCacheHits());
    assertEquals(2, repoImpl.getCacheMisses());
    RepositoryNode leaf = repo.getNode("http://www.example.com/testDir/leaf1");
    assertEquals(1, repoImpl.getCacheHits());
    RepositoryNode leaf2 = repo.getNode("http://www.example.com/testDir/leaf1");
    assertSame(leaf, leaf2);
    assertEquals(2, repoImpl.getCacheHits());
    assertEquals(2, repoImpl.getCacheMisses());
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
      leaf.getNodeContents();
      fail("Should have thrown state exception.");
    } catch (LockssRepository.RepositoryStateException rse) { }

    assertTrue(leaf.cacheLocationFile.exists());
    assertEquals(RepositoryNodeImpl.INACTIVE_VERSION, leaf.getCurrentVersion());
    assertFalse(leaf.hasContent());
  }

  // test static naming calls

  public void testGetNewPluginDir() {
    // call this to 'reblank' after the effects of setUp()
    repo.stopService();

    // should start with the char before 'a'
    assertEquals(""+(char)('a'-1), LockssRepositoryImpl.lastPluginDir);
    LockssRepositoryImpl.getNewPluginDir();
    assertEquals("a", LockssRepositoryImpl.lastPluginDir);
    LockssRepositoryImpl.getNewPluginDir();
    assertEquals("b", LockssRepositoryImpl.lastPluginDir);

    LockssRepositoryImpl.lastPluginDir = "z";
    LockssRepositoryImpl.getNewPluginDir();
    assertEquals("aa", LockssRepositoryImpl.lastPluginDir);
    LockssRepositoryImpl.getNewPluginDir();
    assertEquals("ab", LockssRepositoryImpl.lastPluginDir);
    LockssRepositoryImpl.lastPluginDir = "az";
    LockssRepositoryImpl.getNewPluginDir();
    assertEquals("ba", LockssRepositoryImpl.lastPluginDir);
    LockssRepositoryImpl.lastPluginDir = "czz";
    LockssRepositoryImpl.getNewPluginDir();
    assertEquals("daa", LockssRepositoryImpl.lastPluginDir);

    LockssRepositoryImpl.lastPluginDir = ""+ (char)('a'-1);
  }

  public void testGetAuDirFromMap() {
    HashMap newNameMap = new HashMap();
    newNameMap.put(mau.getAuId(), "testDir");
    LockssRepositoryImpl.nameMap = newNameMap;
    StringBuffer buffer = new StringBuffer();
    LockssRepositoryImpl.getAuDir(mau, buffer);
    assertEquals("testDir", buffer.toString());
  }

  public void testSaveAndLoadNames() {
    Properties newProps = new Properties();
    newProps.setProperty(LockssRepositoryImpl.AU_ID_PROP, mau.getAuId());

    HashMap newNameMap = new HashMap();
    newNameMap.put(mau.getAuId(), "testDir");
    LockssRepositoryImpl.nameMap = newNameMap;
    String location = LockssRepositoryImpl.mapAuToFileLocation(
        LockssRepositoryImpl.cacheLocation, mau);

    LockssRepositoryImpl.saveAuIdProperties(location, newProps);
    File idFile = new File(location + LockssRepositoryImpl.AU_ID_FILE);
    assertTrue(idFile.exists());

    newProps = LockssRepositoryImpl.getAuIdProperties(location);
    assertNotNull(newProps);
    assertEquals(mau.getAuId(),
                 newProps.getProperty(LockssRepositoryImpl.AU_ID_PROP));
  }

  public void testLoadNameMap() {
    Properties newProps = new Properties();
    newProps.setProperty(LockssRepositoryImpl.AU_ID_PROP, mau.getAuId());
    String location = LockssRepositoryImpl.cacheLocation + "ab";
    LockssRepositoryImpl.saveAuIdProperties(location, newProps);

    LockssRepositoryImpl.loadNameMap(LockssRepositoryImpl.cacheLocation);
    assertEquals("ab", repo.nameMap.get(mau.getAuId()));
  }

  public void testLoadNameMapSkipping() {
    // clear the prop file from setUp()
    String propsLoc = LockssRepositoryImpl.cacheLocation + "a" + File.separator +
        LockssRepositoryImpl.AU_ID_FILE;
    File propsFile = new File(propsLoc);
    propsFile.delete();

    LockssRepositoryImpl.loadNameMap(LockssRepositoryImpl.cacheLocation);
    assertNull(LockssRepositoryImpl.nameMap.get(mau.getAuId()));
  }

  public void testMapAuToFileLocation() {
    LockssRepositoryImpl.lastPluginDir = "ca";
    String expectedStr = LockssRepositoryImpl.cacheLocation + "root/cb/";
    assertEquals(FileUtil.sysDepPath(expectedStr),
                 LockssRepositoryImpl.mapAuToFileLocation(
        LockssRepositoryImpl.cacheLocation+"root", new MockArchivalUnit()));
  }

  public void testGetAuDirSkipping() {
    String location = LockssRepositoryImpl.cacheLocation + "root/ab";
    File dirFile = new File(location);
    dirFile.mkdirs();

    LockssRepositoryImpl.lastPluginDir = "aa";
    String expectedStr = LockssRepositoryImpl.cacheLocation + "root/ac/";
    assertEquals(FileUtil.sysDepPath(expectedStr),
                 LockssRepositoryImpl.mapAuToFileLocation(
        LockssRepositoryImpl.cacheLocation+"root", new MockArchivalUnit()));
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
    String testStr = "http://www.example.com/"+URLEncoder.encode("#")+"nodestate.xml";
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

}
