/*
 * $Id: TestRepositoryNodeImpl.java,v 1.39 2004-04-09 06:54:46 tlipkis Exp $
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
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.RangeCachedUrlSetSpec;
import org.lockss.plugin.AuUrl;

/**
 * This is the test class for org.lockss.repository.RepositoryNodeImpl
 */
public class TestRepositoryNodeImpl extends LockssTestCase {
  static final String TREE_SIZE_PROPERTY =
    RepositoryNodeImpl.TREE_SIZE_PROPERTY;
  static final String CHILD_COUNT_PROPERTY =
    RepositoryNodeImpl.CHILD_COUNT_PROPERTY;

  private MockLockssDaemon theDaemon;
  private LockssRepository repo;
  private String tempDirPath;
  MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    mau = new MockArchivalUnit();

    theDaemon = new MockLockssDaemon();
    repo = theDaemon.getLockssRepository(mau);
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    repo.stopService();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  public void testGetNodeUrl() {
    RepositoryNode node = new RepositoryNodeImpl("testUrl", "testDir", null);
    assertEquals("testUrl", node.getNodeUrl());
    node = new RepositoryNodeImpl("testUrl/test.txt", "testUrl/test.txt", null);
    assertEquals("testUrl/test.txt", node.getNodeUrl());
  }

  public void testFileLocation() throws Exception {
    RepositoryNode leaf =
        createLeaf("http://www.example.com/testDir/branch1/leaf1",
                   "test stream", null);
    tempDirPath += LockssRepositoryImpl.CACHE_ROOT_NAME;
    tempDirPath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath, mau);
    tempDirPath = LockssRepositoryImpl.mapUrlToFileLocation(tempDirPath,
        "http://www.example.com/testDir/branch1/leaf1");
    File testFile = new File(tempDirPath);
    assertTrue(testFile.exists());
    testFile = new File(tempDirPath + "/#content/current");
    assertTrue(testFile.exists());
    testFile = new File(tempDirPath + "/#content/current.props");
    assertTrue(testFile.exists());
    testFile = new File(tempDirPath + "/#node_props");
    assertFalse(testFile.exists());
  }

  public void testVersionFileLocation() throws Exception {
    RepositoryNode leaf =
        createLeaf("http://www.example.com/testDir/branch1/leaf1",
        "test stream", null);
    tempDirPath += LockssRepositoryImpl.CACHE_ROOT_NAME;
    tempDirPath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath, mau);
    tempDirPath = LockssRepositoryImpl.mapUrlToFileLocation(tempDirPath,
        "http://www.example.com/testDir/branch1/leaf1");
    File testFile = new File(tempDirPath + "/#content/1");
    assertFalse(testFile.exists());
    testFile = new File(tempDirPath + "/#content/1.props");
    assertFalse(testFile.exists());

    leaf.makeNewVersion();
    OutputStream os = leaf.getNewOutputStream();
    InputStream is = new StringInputStream("test stream 2");
    StreamUtil.copy(is, os);
    is.close();
    os.close();
    leaf.setNewProperties(new Properties());
    leaf.sealNewVersion();
    testFile = new File(tempDirPath + "/#content/1");
    assertTrue(testFile.exists());
    testFile = new File(tempDirPath + "/#content/1.props");
    assertTrue(testFile.exists());
  }

  public void testInactiveFileLocation() throws Exception {
    RepositoryNode leaf =
        createLeaf("http://www.example.com/testDir/branch1/leaf1",
                   "test stream", null);
    tempDirPath += LockssRepositoryImpl.CACHE_ROOT_NAME;
    tempDirPath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath, mau);
    tempDirPath = LockssRepositoryImpl.mapUrlToFileLocation(tempDirPath,
        "http://www.example.com/testDir/branch1/leaf1");
    File curFile = new File(tempDirPath + "/#content/current");
    File curPropsFile = new File(tempDirPath + "/#content/current.props");
    File inactFile = new File(tempDirPath + "/#content/inactive");
    File inactPropsFile = new File(tempDirPath + "/#content/inactive.props");
    assertTrue(curFile.exists());
    assertTrue(curPropsFile.exists());
    assertFalse(inactFile.exists());
    assertFalse(inactPropsFile.exists());

    leaf.deactivateContent();
    assertFalse(curFile.exists());
    assertFalse(curPropsFile.exists());
    assertTrue(inactFile.exists());
    assertTrue(inactPropsFile.exists());

    //reactivate
    leaf.restoreLastVersion();
    assertTrue(curFile.exists());
    assertTrue(curPropsFile.exists());
    assertFalse(inactFile.exists());
    assertFalse(inactPropsFile.exists());

    leaf.deactivateContent();
    assertFalse(curFile.exists());
    assertFalse(curPropsFile.exists());
    assertTrue(inactFile.exists());
    assertTrue(inactPropsFile.exists());

    // make new version
    leaf.makeNewVersion();
    OutputStream os = leaf.getNewOutputStream();
    InputStream is = new StringInputStream("test stream 2");
    StreamUtil.copy(is, os);
    is.close();
    os.close();
    leaf.setNewProperties(new Properties());
    leaf.sealNewVersion();
    assertTrue(curFile.exists());
    assertTrue(curPropsFile.exists());
    assertFalse(inactFile.exists());
    assertFalse(inactPropsFile.exists());
  }

  public void testDeleteFileLocation() throws Exception {
    RepositoryNode leaf =
        createLeaf("http://www.example.com/testDir/branch1/leaf1",
                   "test stream", null);
    tempDirPath += LockssRepositoryImpl.CACHE_ROOT_NAME;
    tempDirPath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath, mau);
    tempDirPath = LockssRepositoryImpl.mapUrlToFileLocation(tempDirPath,
        "http://www.example.com/testDir/branch1/leaf1");
    File curFile = new File(tempDirPath + "/#content/current");
    File curPropsFile = new File(tempDirPath + "/#content/current.props");
    File inactFile = new File(tempDirPath + "/#content/inactive");
    File inactPropsFile = new File(tempDirPath + "/#content/inactive.props");
    assertTrue(curFile.exists());
    assertTrue(curPropsFile.exists());
    assertFalse(inactFile.exists());
    assertFalse(inactPropsFile.exists());

    leaf.markAsDeleted();
    assertFalse(curFile.exists());
    assertFalse(curPropsFile.exists());
    assertTrue(inactFile.exists());
    assertTrue(inactPropsFile.exists());

    //reactivate
    leaf.restoreLastVersion();
    assertTrue(curFile.exists());
    assertTrue(curPropsFile.exists());
    assertFalse(inactFile.exists());
    assertFalse(inactPropsFile.exists());

    leaf.markAsDeleted();
    assertFalse(curFile.exists());
    assertFalse(curPropsFile.exists());
    assertTrue(inactFile.exists());
    assertTrue(inactPropsFile.exists());

    // make new version
    leaf.makeNewVersion();
    OutputStream os = leaf.getNewOutputStream();
    InputStream is = new StringInputStream("test stream 2");
    StreamUtil.copy(is, os);
    is.close();
    os.close();
    leaf.setNewProperties(new Properties());
    leaf.sealNewVersion();
    assertTrue(curFile.exists());
    assertTrue(curPropsFile.exists());
    assertFalse(inactFile.exists());
    assertFalse(inactPropsFile.exists());
  }

  public void testListEntries() throws Exception {
    createLeaf("http://www.example.com/testDir/branch1/leaf1",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch1/leaf2",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch2/leaf3",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch2", "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf4", "test stream", null);

    // root branch
    RepositoryNode dirEntry =
        repo.getNode("http://www.example.com/testDir");
    Iterator childIt = dirEntry.listChildren(null, false);
    ArrayList childL = new ArrayList(3);
    while (childIt.hasNext()) {
      RepositoryNode node = (RepositoryNode)childIt.next();
      childL.add(node.getNodeUrl());
    }
    String[] expectedA = new String[] {
      "http://www.example.com/testDir/branch1",
      "http://www.example.com/testDir/branch2",
      "http://www.example.com/testDir/leaf4"
      };
    assertIsomorphic(expectedA, childL);

    // sub-branch
    dirEntry = repo.getNode("http://www.example.com/testDir/branch1");
    childL.clear();
    childIt = dirEntry.listChildren(null, false);
    while (childIt.hasNext()) {
      RepositoryNode node = (RepositoryNode)childIt.next();
      childL.add(node.getNodeUrl());
    }
    expectedA = new String[] {
      "http://www.example.com/testDir/branch1/leaf1",
      "http://www.example.com/testDir/branch1/leaf2",
      };
    assertIsomorphic(expectedA, childL);

    // sub-branch with content
    dirEntry = repo.getNode("http://www.example.com/testDir/branch2");
    childL.clear();
    childIt = dirEntry.listChildren(null, false);
    while (childIt.hasNext()) {
      RepositoryNode node = (RepositoryNode)childIt.next();
      childL.add(node.getNodeUrl());
    }
    expectedA = new String[] {
      "http://www.example.com/testDir/branch2/leaf3",
      };
    assertIsomorphic(expectedA, childL);

    // leaf node
    dirEntry = repo.getNode("http://www.example.com/testDir/branch1/leaf1");
    childL.clear();
    childIt = dirEntry.listChildren(null, false);
    while (childIt.hasNext()) {
      RepositoryNode node = (RepositoryNode)childIt.next();
      childL.add(node.getNodeUrl());
    }
    expectedA = new String[] { };
    assertIsomorphic(expectedA, childL);
  }

  public void testEntrySort() throws Exception {
    createLeaf("http://www.example.com/testDir/branch2/leaf1", null, null);
    createLeaf("http://www.example.com/testDir/leaf4", null, null);
    createLeaf("http://www.example.com/testDir/branch1/leaf1", null, null);
    createLeaf("http://www.example.com/testDir/leaf3", null, null);

    RepositoryNode dirEntry =
        repo.getNode("http://www.example.com/testDir");
    Iterator childIt = dirEntry.listChildren(null, false);
    ArrayList childL = new ArrayList(4);
    while (childIt.hasNext()) {
      RepositoryNode node = (RepositoryNode)childIt.next();
      childL.add(node.getNodeUrl());
    }
    String[] expectedA = new String[] {
      "http://www.example.com/testDir/branch1",
      "http://www.example.com/testDir/branch2",
      "http://www.example.com/testDir/leaf3",
      "http://www.example.com/testDir/leaf4"
      };
    assertIsomorphic(expectedA, childL);
  }

  public void testIllegalOperations() throws Exception {
    RepositoryNode leaf =
      repo.createNewNode("http://www.example.com/testDir/test.cache");
    assertFalse(leaf.hasContent());
    try {
      leaf.getCurrentVersion();
      fail("Cannot get current version if no content.");
    } catch (UnsupportedOperationException uoe) { }
    try {
      leaf.getContentSize();
      fail("Cannot get content size if no content.");
    } catch (UnsupportedOperationException uoe) { }
    try {
      leaf.sealNewVersion();
      fail("Cannot seal version if not open.");
    } catch (UnsupportedOperationException uoe) { }
    leaf.makeNewVersion();
    try {
      leaf.sealNewVersion();
      fail("Cannot seal version if getNewOutputStream() uncalled.");
    } catch (UnsupportedOperationException uoe) { }
    leaf.makeNewVersion();
    try {
      leaf.deactivateContent();
      fail("Cannot deactivate if currently open for writing.");
    } catch (UnsupportedOperationException uoe) { }
    writeToLeaf(leaf, "test stream");
    try {
      leaf.sealNewVersion();
      fail("Cannot seal version if setNewProperties() uncalled.");
    } catch (UnsupportedOperationException uoe) { }
    leaf.makeNewVersion();
    writeToLeaf(leaf, "test stream");
    leaf.setNewProperties(new Properties());
    leaf.sealNewVersion();
    assertEquals(1, leaf.getCurrentVersion());
    assertTrue(leaf.hasContent());
  }

  public void testVersionTimeout() throws Exception {
    TimeBase.setSimulated();
    RepositoryNode leaf =
      repo.createNewNode("http://www.example.com/testDir/test.cache");
    RepositoryNode leaf2 =
      repo.getNode("http://www.example.com/testDir/test.cache");
    leaf.makeNewVersion();
    try {
      leaf2.makeNewVersion();
      fail("Can't make new version while version open.");
    } catch (UnsupportedOperationException e) { }
    TimeBase.step(RepositoryNodeImpl.DEFAULT_VERSION_TIMEOUT/2);
    try {
      leaf2.makeNewVersion();
      fail("Can't make new version while version not timed out.");
    } catch (UnsupportedOperationException e) { }
    TimeBase.step(RepositoryNodeImpl.DEFAULT_VERSION_TIMEOUT/2);
    leaf2.makeNewVersion();
  }

  public void testMakeNewCache() throws Exception {
    RepositoryNode leaf =
      repo.createNewNode("http://www.example.com/testDir/test.cache");
    assertFalse(leaf.hasContent());
    try {
      leaf.getCurrentVersion();
      fail("Cannot get current version if no content.");
    } catch (UnsupportedOperationException uoe) { }
    leaf.makeNewVersion();
    writeToLeaf(leaf, "test stream");
    leaf.setNewProperties(new Properties());
    leaf.sealNewVersion();
    assertTrue(leaf.hasContent());
    assertEquals(1, leaf.getCurrentVersion());
  }

  public void testMakeNodeLocation() throws Exception {
    RepositoryNodeImpl leaf = (RepositoryNodeImpl)
        repo.createNewNode("http://www.example.com/testDir");
    String nodeLoc = tempDirPath + LockssRepositoryImpl.CACHE_ROOT_NAME;
    nodeLoc = LockssRepositoryImpl.mapAuToFileLocation(nodeLoc, mau);
    nodeLoc = LockssRepositoryImpl.mapUrlToFileLocation(nodeLoc,
        "http://www.example.com/testDir");
    File testFile = new File(nodeLoc);
    assertFalse(testFile.exists());
    leaf.createNodeLocation();
    assertTrue(testFile.exists());
    assertTrue(testFile.isDirectory());
  }

  public void testMakeNewVersion() throws Exception {
    Properties props = new Properties();
    props.setProperty("test 1", "value 1");
    RepositoryNode leaf =
        createLeaf("http://www.example.com/testDir/test.cache",
        "test stream 1", props);
    assertEquals(1, leaf.getCurrentVersion());

    props = new Properties();
    props.setProperty("test 1", "value 2");
    leaf.makeNewVersion();
    leaf.setNewProperties(props);
    writeToLeaf(leaf, "test stream 2");
    leaf.sealNewVersion();
    assertEquals(2, leaf.getCurrentVersion());

    String resultStr = getLeafContent(leaf);
    assertEquals("test stream 2", resultStr);
    props = leaf.getNodeContents().getProperties();
    assertEquals("value 2", props.getProperty("test 1"));
  }

  public void testMakeNewVersionWithoutClosingStream() throws Exception {
    RepositoryNode leaf =
        createLeaf("http://www.example.com/testDir/test.cache",
        "test stream 1", new Properties());

    leaf.makeNewVersion();
    leaf.setNewProperties(new Properties());
    OutputStream os = leaf.getNewOutputStream();
    InputStream is = new StringInputStream("test stream 2");
    StreamUtil.copy(is, os);
    is.close();
    // don't close outputstream
    leaf.sealNewVersion();
    assertEquals(2, leaf.getCurrentVersion());
    String resultStr = getLeafContent(leaf);
    assertEquals("test stream 2", resultStr);
  }

  public void testMakeNewIdenticalVersion() throws Exception {
    Properties props = new Properties();
    props.setProperty("test 1", "value 1");
    MyMockRepositoryNode leaf = new MyMockRepositoryNode(
        (RepositoryNodeImpl)createLeaf(
        "http://www.example.com/testDir/test.cache", "test stream", props));
    assertEquals(1, leaf.getCurrentVersion());
    // set the file extension
    leaf.dateValue = 123321;

    props = new Properties();
    props.setProperty("test 1", "value 2");
    leaf.makeNewVersion();
    leaf.setNewProperties(props);
    writeToLeaf(leaf, "test stream");
    leaf.sealNewVersion();
    assertEquals(1, leaf.getCurrentVersion());

    String resultStr = getLeafContent(leaf);
    assertEquals("test stream", resultStr);
    props = leaf.getNodeContents().getProperties();
    assertEquals("value 2", props.getProperty("test 1"));

    // make sure proper files exist
    tempDirPath += LockssRepositoryImpl.CACHE_ROOT_NAME;
    tempDirPath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath, mau);
    tempDirPath = LockssRepositoryImpl.mapUrlToFileLocation(tempDirPath,
        "http://www.example.com/testDir/test.cache");

    File testFileDir = new File(tempDirPath + "/#content");
    File[] files = testFileDir.listFiles();
    assertEquals(3, files.length);
    File testFile = new File(testFileDir, "current");
    assertTrue(testFile.exists());
    testFile = new File(testFileDir, "current.props");
    assertTrue(testFile.exists());
    testFile = new File(testFileDir, "1.props-123321");
    assertTrue(testFile.exists());
  }

  public void testIdenticalVersionFixesVersionError() throws Exception {
    Properties props = new Properties();
    MyMockRepositoryNode leaf = new MyMockRepositoryNode(
        (RepositoryNodeImpl)createLeaf(
        "http://www.example.com/testDir/test.cache", "test stream", props));
    assertEquals(1, leaf.getCurrentVersion());

    props = new Properties();
    leaf.makeNewVersion();
    leaf.setNewProperties(props);
    // set to error state
    leaf.currentVersion = 0;
    writeToLeaf(leaf, "test stream");
    assertEquals(0, leaf.currentVersion);
    leaf.sealNewVersion();
    // fixes error state, even though identical
    assertEquals(1, leaf.getCurrentVersion());
  }

  public void testMakeNewVersionFixesVersionError() throws Exception {
    Properties props = new Properties();
    MyMockRepositoryNode leaf = new MyMockRepositoryNode(
        (RepositoryNodeImpl)createLeaf(
        "http://www.example.com/testDir/test.cache", "test stream", props));
    assertEquals(1, leaf.getCurrentVersion());

    props = new Properties();
    leaf.makeNewVersion();
    // set to error state
    leaf.currentVersion = -1;
    leaf.setNewProperties(props);
    writeToLeaf(leaf, "test stream2");
    leaf.sealNewVersion();
    // fixes error state
    assertEquals(1, leaf.getCurrentVersion());
  }

  public void testGetInputStream() throws Exception {
    RepositoryNode leaf =
        createLeaf("http://www.example.com/testDir/test.cache",
        "test stream", null);
    String resultStr = getLeafContent(leaf);
    assertEquals("test stream", resultStr);
  }

  public void testGetProperties() throws Exception {
    Properties props = new Properties();
    props.setProperty("test 1", "value 1");
    RepositoryNode leaf =
        createLeaf("http://www.example.com/testDir/test.cache",
        "test stream", props);

    RepositoryNode.RepositoryNodeContents contents = leaf.getNodeContents();
    props = contents.getProperties();
    // close stream to allow the file to be renamed later
    contents.release();

    assertEquals("value 1", props.getProperty("test 1"));

    leaf.makeNewVersion();
    props = new Properties();
    props.setProperty("test 1", "value 2");
    leaf.setNewProperties(props);
    writeToLeaf(leaf, "test stream 2");
    leaf.sealNewVersion();

    props = leaf.getNodeContents().getProperties();
    assertEquals("value 2", props.getProperty("test 1"));
  }

  public void testDirContent() throws Exception {
    RepositoryNode leaf =
        createLeaf("http://www.example.com/testDir/test.cache",
        "test stream", null);
    assertTrue(leaf.hasContent());

    RepositoryNode dir =
        repo.getNode("http://www.example.com/testDir");
    dir.makeNewVersion();
    writeToLeaf(dir, "test stream");
    dir.setNewProperties(new Properties());
    dir.sealNewVersion();
    assertTrue(dir.hasContent());

    dir = createLeaf("http://www.example.com/testDir/test.cache/new.test",
                     "test stream", null);
    assertTrue(dir.hasContent());
  }

  public void testNodeSize() throws Exception {
    RepositoryNode leaf =
        createLeaf("http://www.example.com/testDir/test.cache",
        "test stream", null);
    assertTrue(leaf.hasContent());
    assertEquals(11, (int)leaf.getContentSize());
  }

  public void testTreeSize() throws Exception {
    createLeaf("http://www.example.com/testDir", "test", null);
    createLeaf("http://www.example.com/testDir/test1", "test1", null);
    createLeaf("http://www.example.com/testDir/test2", "test2", null);
    createLeaf("http://www.example.com/testDir/test3/branch1",
               "test33", null);
    createLeaf("http://www.example.com/testDir/test3/branch2",
               "test33", null);

    RepositoryNode leaf = repo.getNode("http://www.example.com/testDir");
    assertEquals(26, leaf.getTreeContentSize(null));
    leaf = repo.getNode("http://www.example.com/testDir/test1");
    assertEquals(5, leaf.getTreeContentSize(null));
    leaf = repo.getNode("http://www.example.com/testDir/test3");
    assertEquals(12, leaf.getTreeContentSize(null));
    assertEquals(6, leaf.getTreeContentSize(new RangeCachedUrlSetSpec(
        "http://www.example.com/testDir/test3", "/branch1", "/branch1")));
  }

  public void testDetermineParentNode() throws Exception {
    repo.createNewNode("http://www.example.com");
    repo.createNewNode("http://www.example.com/test");
    assertNotNull(repo.getNode("http://www.example.com/test"));
    RepositoryNodeImpl node = (RepositoryNodeImpl)repo.createNewNode(
      "http://www.example.com/test/branch");
    assertEquals("http://www.example.com/test/branch", node.getNodeUrl());
    node = node.determineParentNode();
    assertEquals("http://www.example.com/test", node.getNodeUrl());
    node = node.determineParentNode();
    assertEquals("http://www.example.com", node.getNodeUrl());
    node = node.determineParentNode();
    assertEquals(AuUrl.PROTOCOL, node.getNodeUrl());
    node = node.determineParentNode();
    assertEquals(AuUrl.PROTOCOL, node.getNodeUrl());
  }

  public void testCacheInvalidation() throws Exception {
    RepositoryNodeImpl root =
        (RepositoryNodeImpl)createLeaf("http://www.example.com",
                                       "test", null);
    RepositoryNodeImpl branch =
        (RepositoryNodeImpl)createLeaf("http://www.example.com/branch",
                                       "test", null);
    RepositoryNodeImpl branch2 =
        (RepositoryNodeImpl)createLeaf("http://www.example.com/branch/branch2",
                                       "test", null);
    // This one has directory level with no node prop file, to check that
    // cache invalidation traverses them correctly
    RepositoryNodeImpl leaf =
        (RepositoryNodeImpl)createLeaf("http://www.example.com/branch/branch2/a/b/c/leaf",
                                       "test", null);
    assertNull(branch.nodeProps.getProperty(TREE_SIZE_PROPERTY));
    assertNull(leaf.nodeProps.getProperty(TREE_SIZE_PROPERTY));
    // force invalidation to happen
    branch.nodeProps.setProperty(TREE_SIZE_PROPERTY, "789");
    branch.invalidateCachedValues(true);
    // should now be explicitly marked invalid
    assertEquals(branch.INVALID,
		 branch.nodeProps.getProperty(TREE_SIZE_PROPERTY));
    assertEquals(branch.INVALID,
		 branch.nodeProps.getProperty(CHILD_COUNT_PROPERTY));
    // fake prop set at root to check invalidation stops properly
    root.nodeProps.setProperty(TREE_SIZE_PROPERTY, "789");
    root.nodeProps.setProperty(CHILD_COUNT_PROPERTY, "3");
    // don't set branch so the invalidate stops there
    branch2.nodeProps.setProperty(TREE_SIZE_PROPERTY, "456");
    branch2.nodeProps.setProperty(CHILD_COUNT_PROPERTY, "1");
    leaf.nodeProps.setProperty(TREE_SIZE_PROPERTY, "123");
    leaf.nodeProps.setProperty(CHILD_COUNT_PROPERTY, "0");

    leaf.invalidateCachedValues(true);
    // shoulddn't be set here anymore
    assertFalse(isPropValid(leaf.nodeProps.getProperty(TREE_SIZE_PROPERTY)));
    assertFalse(isPropValid(leaf.nodeProps.getProperty(CHILD_COUNT_PROPERTY)));
    // or here (requires recursing up through dirs that have no node props
    // file)
    assertFalse(isPropValid(branch2.nodeProps.getProperty(TREE_SIZE_PROPERTY)));
    assertFalse(isPropValid(branch2.nodeProps.getProperty(CHILD_COUNT_PROPERTY)));
    // still invalid, recursion should have stopped here
    assertFalse(isPropValid(branch.nodeProps.getProperty(TREE_SIZE_PROPERTY)));
    assertFalse(isPropValid(branch.nodeProps.getProperty(CHILD_COUNT_PROPERTY)));
    // so not cleared these
    assertTrue(isPropValid(root.nodeProps.getProperty(TREE_SIZE_PROPERTY)));
    assertTrue(isPropValid(root.nodeProps.getProperty(CHILD_COUNT_PROPERTY)));
    assertEquals("789", root.nodeProps.getProperty(TREE_SIZE_PROPERTY));
    assertEquals("3", root.nodeProps.getProperty(CHILD_COUNT_PROPERTY));
  }

  boolean isPropValid(String val) {
    return RepositoryNodeImpl.isPropValid(val);
  }

  boolean isPropInvalid(String val) {
    return RepositoryNodeImpl.isPropInvalid(val);
  }

  public void testTreeSizeCaching() throws Exception {
    createLeaf("http://www.example.com/testDir", "test", null);

    RepositoryNodeImpl leaf =
        (RepositoryNodeImpl)repo.getNode("http://www.example.com/testDir");
    assertNull(leaf.nodeProps.getProperty(TREE_SIZE_PROPERTY));
    assertEquals(4, leaf.getTreeContentSize(null));
    assertEquals("4", leaf.nodeProps.getProperty(TREE_SIZE_PROPERTY));
    leaf.markAsDeleted();
    assertTrue(isPropInvalid(leaf.nodeProps.getProperty(TREE_SIZE_PROPERTY)));
    assertEquals(0, leaf.getTreeContentSize(null));
    assertEquals("0", leaf.nodeProps.getProperty(TREE_SIZE_PROPERTY));
  }

  public void testChildCount() throws Exception {
    createLeaf("http://www.example.com/testDir", "test", null);

    RepositoryNodeImpl leaf =
        (RepositoryNodeImpl)repo.getNode("http://www.example.com/testDir");
    assertNull(leaf.nodeProps.getProperty(CHILD_COUNT_PROPERTY));
    assertEquals(0, leaf.getChildCount());
    assertEquals("0", leaf.nodeProps.getProperty(CHILD_COUNT_PROPERTY));

    createLeaf("http://www.example.com/testDir/test1", "test1", null);
    createLeaf("http://www.example.com/testDir/test2", "test2", null);
    assertEquals(2, leaf.getChildCount());
    assertEquals("2", leaf.nodeProps.getProperty(CHILD_COUNT_PROPERTY));
  }

  public void testDeactivate() throws Exception {
    RepositoryNodeImpl leaf =
        (RepositoryNodeImpl)createLeaf("http://www.example.com/test1",
                                       "test stream", null);
    assertTrue(leaf.hasContent());
    assertFalse(leaf.isContentInactive());
    assertEquals(1, leaf.getCurrentVersion());
    assertNull(leaf.nodeProps.getProperty(leaf.INACTIVE_CONTENT_PROPERTY));

    leaf.deactivateContent();
    assertFalse(leaf.hasContent());
    assertTrue(leaf.isContentInactive());
    assertEquals(RepositoryNodeImpl.INACTIVE_VERSION, leaf.getCurrentVersion());
    assertEquals("true", leaf.nodeProps.getProperty(leaf.INACTIVE_CONTENT_PROPERTY));
  }

  public void testDelete() throws Exception {
    RepositoryNodeImpl leaf =
        (RepositoryNodeImpl)createLeaf("http://www.example.com/test1",
                                       "test stream", null);
    assertTrue(leaf.hasContent());
    assertFalse(leaf.isDeleted());
    assertEquals(1, leaf.getCurrentVersion());
    assertNull(leaf.nodeProps.getProperty(leaf.DELETION_PROPERTY));

    leaf.markAsDeleted();
    assertFalse(leaf.hasContent());
    assertTrue(leaf.isDeleted());
    assertEquals(RepositoryNodeImpl.DELETED_VERSION, leaf.getCurrentVersion());
    assertEquals("true", leaf.nodeProps.getProperty(leaf.DELETION_PROPERTY));
  }

  public void testUnDelete() throws Exception {
    RepositoryNodeImpl leaf =
        (RepositoryNodeImpl)createLeaf("http://www.example.com/test1",
                                       "test stream", null);
    leaf.markAsDeleted();
    assertTrue(leaf.isDeleted());
    assertEquals(RepositoryNodeImpl.DELETED_VERSION, leaf.getCurrentVersion());

    leaf.markAsNotDeleted();
    assertFalse(leaf.isContentInactive());
    assertFalse(leaf.isDeleted());
    assertEquals(1, leaf.getCurrentVersion());
    // make to null, not 'false'
    assertNull(leaf.nodeProps.getProperty(leaf.DELETION_PROPERTY));
    String resultStr = getLeafContent(leaf);
    assertEquals("test stream", resultStr);
  }

  public void testRestoreLastVersion() throws Exception {
    Properties props = new Properties();
    props.setProperty("test 1", "value 1");
    RepositoryNode leaf =
        createLeaf("http://www.example.com/test1", "test stream 1", props);
    assertEquals(1, leaf.getCurrentVersion());

    props = new Properties();
    props.setProperty("test 1", "value 2");
    leaf.makeNewVersion();
    leaf.setNewProperties(props);
    writeToLeaf(leaf, "test stream 2");
    leaf.sealNewVersion();
    assertEquals(2, leaf.getCurrentVersion());

    leaf.restoreLastVersion();
    assertEquals(1, leaf.getCurrentVersion());

    String resultStr = getLeafContent(leaf);
    assertEquals("test stream 1", resultStr);
    props = leaf.getNodeContents().getProperties();
    assertEquals("value 1", props.getProperty("test 1"));
  }

  public void testReactivateViaRestore() throws Exception {
    RepositoryNodeImpl leaf =
        (RepositoryNodeImpl)createLeaf("http://www.example.com/test1",
                                       "test stream", null);
    leaf.deactivateContent();
    assertTrue(leaf.isContentInactive());
    assertEquals(RepositoryNodeImpl.INACTIVE_VERSION, leaf.getCurrentVersion());

    leaf.restoreLastVersion();
    assertFalse(leaf.isContentInactive());
    assertEquals(1, leaf.getCurrentVersion());
    // back to null, not 'false'
    assertNull(leaf.nodeProps.getProperty(leaf.INACTIVE_CONTENT_PROPERTY));
    String resultStr = getLeafContent(leaf);
    assertEquals("test stream", resultStr);
  }

  public void testReactivateViaNewVersion() throws Exception {
    RepositoryNodeImpl leaf =
        (RepositoryNodeImpl)createLeaf("http://www.example.com/test1",
                                       "test stream", null);
    leaf.deactivateContent();
    assertTrue(leaf.isContentInactive());
    assertEquals(RepositoryNodeImpl.INACTIVE_VERSION, leaf.getCurrentVersion());

    Properties props = new Properties();
    props.setProperty("test 1", "value 2");
    leaf.makeNewVersion();
    leaf.setNewProperties(props);
    writeToLeaf(leaf, "test stream 2");
    leaf.sealNewVersion();
    assertFalse(leaf.isContentInactive());
    assertEquals(2, leaf.getCurrentVersion());
    String resultStr = getLeafContent(leaf);
    assertEquals("test stream 2", resultStr);

    File lastProps = new File(leaf.cacheLocationFile, "1.props");
    assertTrue(lastProps.exists());
    InputStream is =
        new BufferedInputStream(new FileInputStream(lastProps));
    props.load(is);
    is.close();
    // make sure the 'was inactive' property hasn't been lost
    assertEquals("true", props.getProperty(leaf.NODE_WAS_INACTIVE_PROPERTY));
  }

  public void testAbandonReactivateViaNewVersion() throws Exception {
    RepositoryNode leaf =
        createLeaf("http://www.example.com/test1", "test stream", null);
    leaf.deactivateContent();
    assertTrue(leaf.isContentInactive());
    assertEquals(RepositoryNodeImpl.INACTIVE_VERSION, leaf.getCurrentVersion());

    Properties props = new Properties();
    props.setProperty("test 1", "value 2");
    leaf.makeNewVersion();
    leaf.setNewProperties(props);
    writeToLeaf(leaf, "test stream 2");
    leaf.abandonNewVersion();
    assertTrue(leaf.isContentInactive());
    assertEquals(RepositoryNodeImpl.INACTIVE_VERSION, leaf.getCurrentVersion());
  }

  public void testIsLeaf() throws Exception {
    createLeaf("http://www.example.com/testDir/test1", "test stream", null);
    createLeaf("http://www.example.com/testDir/branch1", "test stream", null);
    createLeaf("http://www.example.com/testDir/branch1/test4", "test stream", null);

    RepositoryNode leaf = repo.getNode("http://www.example.com/testDir/test1");
    assertTrue(leaf.isLeaf());
    leaf = repo.getNode("http://www.example.com/testDir/branch1");
    assertFalse(leaf.isLeaf());
  }

  public void testListInactiveNodes() throws Exception {
    createLeaf("http://www.example.com/testDir/test1", "test stream", null);
    createLeaf("http://www.example.com/testDir/test2", "test stream", null);
    createLeaf("http://www.example.com/testDir/test3", "test stream", null);
    createLeaf("http://www.example.com/testDir/branch1", "test stream", null);
    createLeaf("http://www.example.com/testDir/branch1/test4", "test stream", null);
    createLeaf("http://www.example.com/testDir/branch2", "test stream", null);
    createLeaf("http://www.example.com/testDir/branch2/test5", "test stream", null);

    RepositoryNode dirEntry = repo.getNode("http://www.example.com/testDir");
    Iterator childIt = dirEntry.listChildren(null, false);
    ArrayList childL = new ArrayList(3);
    while (childIt.hasNext()) {
      RepositoryNode node = (RepositoryNode)childIt.next();
      childL.add(node.getNodeUrl());
    }
    String[] expectedA = new String[] {
      "http://www.example.com/testDir/branch1",
      "http://www.example.com/testDir/branch2",
      "http://www.example.com/testDir/test1",
      "http://www.example.com/testDir/test2",
      "http://www.example.com/testDir/test3"
      };
    assertIsomorphic(expectedA, childL);

    RepositoryNode leaf = repo.getNode("http://www.example.com/testDir/test2");
    leaf.deactivateContent();
    // this next shouldn't be excluded since it isn't a leaf node
    leaf = repo.getNode("http://www.example.com/testDir/branch1");
    leaf.deactivateContent();
    // this next should be excluded because it's deleted
    leaf = repo.getNode("http://www.example.com/testDir/branch2");
    leaf.markAsDeleted();

    childIt = dirEntry.listChildren(null, false);
    childL = new ArrayList(2);
    while (childIt.hasNext()) {
      RepositoryNode node = (RepositoryNode)childIt.next();
      childL.add(node.getNodeUrl());
    }
    expectedA = new String[] {
      "http://www.example.com/testDir/branch1",
      "http://www.example.com/testDir/test1",
      "http://www.example.com/testDir/test3"
      };
    assertIsomorphic("Excluding inactive nodes failed.", expectedA, childL);

    childIt = dirEntry.listChildren(null, true);
    childL = new ArrayList(3);
    while (childIt.hasNext()) {
      RepositoryNode node = (RepositoryNode)childIt.next();
      childL.add(node.getNodeUrl());
    }
    expectedA = new String[] {
      "http://www.example.com/testDir/branch1",
      "http://www.example.com/testDir/test1",
      "http://www.example.com/testDir/test2",
      "http://www.example.com/testDir/test3"
      };
    assertIsomorphic("Including inactive nodes failed.", expectedA, childL);
  }

  public void testDeleteInnerNode() throws Exception {
    createLeaf("http://www.example.com/testDir/test1", "test stream", null);
    createLeaf("http://www.example.com/testDir/test2", "test stream", null);

    RepositoryNode dirEntry = repo.getNode("http://www.example.com/testDir");
    assertFalse(dirEntry.isDeleted());
    dirEntry.markAsDeleted();
    assertTrue(dirEntry.isDeleted());
    dirEntry.markAsNotDeleted();
    assertFalse(dirEntry.isDeleted());
  }


  public void testGetFileStrings() throws Exception {
    RepositoryNodeImpl node = (RepositoryNodeImpl) repo.createNewNode(
        "http://www.example.com/test.url");
    node.initNodeRoot();
    String contentStr = FileUtil.sysDepPath(node.nodeLocation + "/#content/");
    assertEquals(contentStr, node.getContentDirBuffer().toString());
    String expectedStr = contentStr + "123";
    assertEquals(expectedStr,
                 node.getVersionedCacheFile(123).getAbsolutePath());
    expectedStr = contentStr + "123.props";
    assertEquals(expectedStr,
                 node.getVersionedPropsFile(123).getAbsolutePath());
    expectedStr = contentStr + "inactive";
    assertEquals(expectedStr, node.getInactiveCacheFile().getAbsolutePath());
    expectedStr = contentStr + "inactive.props";
    assertEquals(expectedStr, node.getInactivePropsFile().getAbsolutePath());
  }

  private RepositoryNode createLeaf(String url, String content,
                                    Properties props) throws Exception {
    return createLeaf(repo, url, content, props);
  }

  public static RepositoryNode createLeaf(LockssRepository repo, String url,
                                   String content, Properties props)
      throws Exception {
    RepositoryNode leaf = repo.createNewNode(url);
    leaf.makeNewVersion();
    writeToLeaf(leaf, content);
    if (props==null) {
      props = new Properties();
    }
    leaf.setNewProperties(props);
    leaf.sealNewVersion();
    return leaf;
  }

  public static void writeToLeaf(RepositoryNode leaf, String content)
      throws Exception {
    if (content==null) {
      content = "";
    }
    OutputStream os = leaf.getNewOutputStream();
    InputStream is = new StringInputStream(content);
    StreamUtil.copy(is, os);
    os.close();
    is.close();
  }

  public static String getLeafContent(RepositoryNode leaf) throws IOException {
    InputStream is = leaf.getNodeContents().getInputStream();
    OutputStream baos = new ByteArrayOutputStream(20);
    StreamUtil.copy(is, baos);
    is.close();
    String resultStr = baos.toString();
    baos.close();
    return resultStr;
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestRepositoryNodeImpl.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

  // this class only overrides 'getDatedVersionedPropsFile()' so I can
  // manipulate the file names for testing
  static class MyMockRepositoryNode extends RepositoryNodeImpl {
    long dateValue;
    MyMockRepositoryNode(RepositoryNodeImpl nodeImpl) {
      super(nodeImpl.url, nodeImpl.nodeLocation, nodeImpl.repository);
    }

    File getDatedVersionedPropsFile(int version, long date) {
      StringBuffer buffer = getContentDirBuffer();
      buffer.append(version);
      buffer.append(".");
      buffer.append(PROPS_FILENAME);
      buffer.append("-");
      // don't use the passed in date
      // be careful not to use identical dates here, as it will loop while
      // trying to increment the 'date' value
      buffer.append(dateValue);
      return new File(buffer.toString());
    }
  }
}
