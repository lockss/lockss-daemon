/*
 * $Id: TestRepositoryNodeImpl.java,v 1.29 2003-09-16 23:48:24 eaalto Exp $
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

/**
 * This is the test class for org.lockss.repostiory.RepositoryNodeImpl
 */
public class TestRepositoryNodeImpl extends LockssTestCase {
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

  public void testGetState() throws Exception {
    //XXX implement
  }

  public void testStoreState() {
    //XXX implement
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
    testFile = new File(tempDirPath + "/#content/node_props");
    assertFalse(testFile.exists());
    leaf.deactivateContent();
    assertTrue(testFile.exists());
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
    InputStream is = new StringInputStream("test stream");
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
    File nodePropsFile = new File(tempDirPath + "/#content/node_props");
    assertTrue(curFile.exists());
    assertTrue(curPropsFile.exists());
    assertFalse(inactFile.exists());
    assertFalse(inactPropsFile.exists());
    assertFalse(nodePropsFile.exists());

    leaf.deactivateContent();
    assertFalse(curFile.exists());
    assertFalse(curPropsFile.exists());
    assertTrue(inactFile.exists());
    assertTrue(inactPropsFile.exists());
    assertTrue(nodePropsFile.exists());

    //reactivate
    leaf.restoreLastVersion();
    assertTrue(curFile.exists());
    assertTrue(curPropsFile.exists());
    assertFalse(inactFile.exists());
    assertFalse(inactPropsFile.exists());
    assertTrue(nodePropsFile.exists());

    leaf.deactivateContent();
    assertFalse(curFile.exists());
    assertFalse(curPropsFile.exists());
    assertTrue(inactFile.exists());
    assertTrue(inactPropsFile.exists());
    assertTrue(nodePropsFile.exists());

    // make new version
    leaf.makeNewVersion();
    OutputStream os = leaf.getNewOutputStream();
    InputStream is = new StringInputStream("test stream");
    StreamUtil.copy(is, os);
    is.close();
    os.close();
    leaf.setNewProperties(new Properties());
    leaf.sealNewVersion();
    assertTrue(curFile.exists());
    assertTrue(curPropsFile.exists());
    assertFalse(inactFile.exists());
    assertFalse(inactPropsFile.exists());
    assertTrue(nodePropsFile.exists());
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
    File nodePropsFile = new File(tempDirPath + "/#content/node_props");
    assertTrue(curFile.exists());
    assertTrue(curPropsFile.exists());
    assertFalse(inactFile.exists());
    assertFalse(inactPropsFile.exists());
    assertFalse(nodePropsFile.exists());

    leaf.markAsDeleted();
    assertFalse(curFile.exists());
    assertFalse(curPropsFile.exists());
    assertTrue(inactFile.exists());
    assertTrue(inactPropsFile.exists());
    assertTrue(nodePropsFile.exists());

    //reactivate
    leaf.restoreLastVersion();
    assertTrue(curFile.exists());
    assertTrue(curPropsFile.exists());
    assertFalse(inactFile.exists());
    assertFalse(inactPropsFile.exists());
    assertTrue(nodePropsFile.exists());

    leaf.markAsDeleted();
    assertFalse(curFile.exists());
    assertFalse(curPropsFile.exists());
    assertTrue(inactFile.exists());
    assertTrue(inactPropsFile.exists());
    assertTrue(nodePropsFile.exists());

    // make new version
    leaf.makeNewVersion();
    OutputStream os = leaf.getNewOutputStream();
    InputStream is = new StringInputStream("test stream");
    StreamUtil.copy(is, os);
    is.close();
    os.close();
    leaf.setNewProperties(new Properties());
    leaf.sealNewVersion();
    assertTrue(curFile.exists());
    assertTrue(curPropsFile.exists());
    assertFalse(inactFile.exists());
    assertFalse(inactPropsFile.exists());
    assertTrue(nodePropsFile.exists());
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
    Iterator childIt = dirEntry.listNodes(null, false);
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
    childIt = dirEntry.listNodes(null, false);
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
    childIt = dirEntry.listNodes(null, false);
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
    childIt = dirEntry.listNodes(null, false);
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
    Iterator childIt = dirEntry.listNodes(null, false);
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
    TimeBase.step(RepositoryNodeImpl.VERSION_TIMEOUT/2);
    try {
      leaf2.makeNewVersion();
      fail("Can't make new version while version not timed out.");
    } catch (UnsupportedOperationException e) { }
    TimeBase.step(RepositoryNodeImpl.VERSION_TIMEOUT/2);
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
    props = leaf.getNodeContents().props;
    assertEquals("value 2", props.getProperty("test 1"));
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
    props = contents.props;
    // close these to allow the file to be renamed later
    contents.input.close();
    contents.reader.close();

    assertEquals("value 1", props.getProperty("test 1"));

    leaf.makeNewVersion();
    props = new Properties();
    props.setProperty("test 1", "value 2");
    leaf.setNewProperties(props);
    writeToLeaf(leaf, "test stream 2");
    leaf.sealNewVersion();

    props = leaf.getNodeContents().props;
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

  public void testDeactivate() throws Exception {
    RepositoryNodeImpl leaf =
        (RepositoryNodeImpl)createLeaf("http://www.example.com/test1",
                                       "test stream", null);
    assertTrue(leaf.hasContent());
    assertFalse(leaf.isInactive());
    assertEquals(1, leaf.getCurrentVersion());
    assertNull(leaf.nodeProps.getProperty(leaf.INACTIVE_CONTENT_PROPERTY));

    leaf.deactivateContent();
    assertFalse(leaf.hasContent());
    assertTrue(leaf.isInactive());
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
    assertFalse(leaf.isInactive());
    assertFalse(leaf.isDeleted());
    assertEquals(1, leaf.getCurrentVersion());
    assertEquals("false", leaf.nodeProps.getProperty(leaf.DELETION_PROPERTY));
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
    props = leaf.getNodeContents().props;
    assertEquals("value 1", props.getProperty("test 1"));
  }

  public void testReactivateViaRestore() throws Exception {
    RepositoryNodeImpl leaf =
        (RepositoryNodeImpl)createLeaf("http://www.example.com/test1",
                                       "test stream", null);
    leaf.deactivateContent();
    assertTrue(leaf.isInactive());
    assertEquals(RepositoryNodeImpl.INACTIVE_VERSION, leaf.getCurrentVersion());

    leaf.restoreLastVersion();
    assertFalse(leaf.isInactive());
    assertEquals(1, leaf.getCurrentVersion());
    assertEquals("false", leaf.nodeProps.getProperty(leaf.INACTIVE_CONTENT_PROPERTY));
    String resultStr = getLeafContent(leaf);
    assertEquals("test stream", resultStr);
  }

  public void testReactivateViaNewVersion() throws Exception {
    RepositoryNode leaf =
        createLeaf("http://www.example.com/test1", "test stream", null);
    leaf.deactivateContent();
    assertTrue(leaf.isInactive());
    assertEquals(RepositoryNodeImpl.INACTIVE_VERSION, leaf.getCurrentVersion());

    Properties props = new Properties();
    props.setProperty("test 1", "value 2");
    leaf.makeNewVersion();
    leaf.setNewProperties(props);
    writeToLeaf(leaf, "test stream 2");
    leaf.sealNewVersion();
    assertFalse(leaf.isInactive());
    assertEquals(2, leaf.getCurrentVersion());
    String resultStr = getLeafContent(leaf);
    assertEquals("test stream 2", resultStr);
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
    Iterator childIt = dirEntry.listNodes(null, false);
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

    childIt = dirEntry.listNodes(null, false);
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

    childIt = dirEntry.listNodes(null, true);
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

  public void testGetFileStrings() throws Exception {
    RepositoryNodeImpl node = (RepositoryNodeImpl) repo.createNewNode(
        "http://www.example.com/test.url");
    node.loadNodeRoot();
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
    InputStream is = leaf.getNodeContents().input;
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
}
