/*
 * $Id: TestRepositoryNodeImpl.java,v 1.2 2002-11-21 21:07:56 aalto Exp $
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
import org.lockss.test.*;
import org.lockss.util.StreamUtil;

/**
 * This is the test class for org.lockss.daemon.LeafEntryImpl
 */

public class TestRepositoryNodeImpl extends LockssTestCase {
  private LockssRepository repo;

  public TestRepositoryNodeImpl(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = "";
    try {
      tempDirPath = super.getTempDir().getAbsolutePath() + File.separator;
    } catch (Exception e) { fail("Couldn't get tempDir."); }
    TestLockssRepositoryImpl.configCacheLocation(tempDirPath);
    MockArchivalUnit mau = new MockArchivalUnit(null);
    repo = LockssRepositoryImpl.repositoryFactory(mau);
  }

  public void testGetNodeUrl() {
    RepositoryNode node = new RepositoryNodeImpl("testUrl", "testDir", null);
    assertTrue(node.getNodeUrl().equals("testUrl"));
    node = new RepositoryNodeImpl("testUrl/test.txt", "testUrl/test.txt", null);
    assertTrue(node.getNodeUrl().equals("testUrl/test.txt"));
  }

  public void testGetState() {
    //XXX implement
  }

  public void testStoreState() {
    //XXX implement
  }

  public void testListEntries() throws Exception {
    RepositoryNode leaf =
        repo.createNewNode("http://www.example.com/testDir/branch1/leaf1");
    leaf.makeNewVersion();
    OutputStream os = leaf.getNewOutputStream();
    InputStream is = new StringInputStream("testing stream");
    StreamUtil.copy(is, os);
    os.close();
    is.close();
    leaf.sealNewVersion();
    leaf = repo.createNewNode("http://www.example.com/testDir/branch1/leaf2");
    leaf.makeNewVersion();
    os = leaf.getNewOutputStream();
    is = new StringInputStream("testing stream");
    StreamUtil.copy(is, os);
    os.close();
    is.close();
    leaf.sealNewVersion();
    leaf = repo.createNewNode("http://www.example.com/testDir/branch2/leaf3");
    leaf.makeNewVersion();
    os = leaf.getNewOutputStream();
    is = new StringInputStream("testing stream");
    StreamUtil.copy(is, os);
    os.close();
    is.close();
    leaf.sealNewVersion();
    leaf = repo.createNewNode("http://www.example.com/testDir/leaf4");
    leaf.makeNewVersion();
    os = leaf.getNewOutputStream();
    is = new StringInputStream("testing stream");
    StreamUtil.copy(is, os);
    os.close();
    is.close();
    leaf.sealNewVersion();

    RepositoryNode dirEntry =
        repo.getRepositoryNode("http://www.example.com/testDir");
    Iterator childIt = dirEntry.listNodes(null);
    int count = 0;
    while (childIt.hasNext()) {
      RepositoryNode node = (RepositoryNode)childIt.next();
      if (node.getNodeUrl().equals("http://www.example.com/testDir/branch1")) {
        count += 1;
        assertTrue(!node.hasContent());
      } else if (node.getNodeUrl().equals("http://www.example.com/testDir/branch2")) {
        count += 2;
        assertTrue(!node.hasContent());
      } else if (node.getNodeUrl().equals("http://www.example.com/testDir/leaf4")) {
        count += 4;
        assertTrue(node.hasContent());
      }
    }
    assertTrue(count==7);

    dirEntry = repo.getRepositoryNode("http://www.example.com/testDir/branch1");
    childIt = dirEntry.listNodes(null);
    count = 0;
    while (childIt.hasNext()) {
      RepositoryNode node = (RepositoryNode)childIt.next();
      if (node.getNodeUrl().equals("http://www.example.com/testDir/branch1/leaf1")) {
        count += 1;
        assertTrue(node.hasContent());
      } else if (node.getNodeUrl().equals("http://www.example.com/testDir/branch1/leaf2")) {
        count += 2;
        assertTrue(node.hasContent());
      }
    }
    assertTrue(count==3);
  }

  public void testEntrySort() throws Exception {
    RepositoryNode leaf =
        repo.createNewNode("http://www.example.com/testDir/branch1/leaf1");
    leaf.makeNewVersion();
    leaf.sealNewVersion();
    leaf = repo.createNewNode("http://www.example.com/testDir/branch2/leaf2");
    leaf.makeNewVersion();
    leaf.sealNewVersion();
    leaf = repo.createNewNode("http://www.example.com/testDir/leaf4");
    leaf.makeNewVersion();
    leaf.sealNewVersion();
    leaf = repo.createNewNode("http://www.example.com/testDir/leaf3");
    leaf.makeNewVersion();
    leaf.sealNewVersion();

    RepositoryNode dirEntry =
        repo.getRepositoryNode("http://www.example.com/testDir");
    Iterator childIt = dirEntry.listNodes(null);
    assertTrue(childIt.hasNext());
    RepositoryNode entry = (RepositoryNode)childIt.next();
    assertTrue(entry.getNodeUrl().equals("http://www.example.com/testDir/branch1"));
    assertTrue(childIt.hasNext());
    entry = (RepositoryNode)childIt.next();
    assertTrue(entry.getNodeUrl().equals("http://www.example.com/testDir/branch2"));
    assertTrue(childIt.hasNext());
    entry = (RepositoryNode)childIt.next();
    assertTrue(entry.getNodeUrl().equals("http://www.example.com/testDir/leaf3"));
    assertTrue(childIt.hasNext());
    entry = (RepositoryNode)childIt.next();
    assertTrue(entry.getNodeUrl().equals("http://www.example.com/testDir/leaf4"));
    assertTrue(!childIt.hasNext());
  }

  public void testMakeNewCache() throws IOException {
    RepositoryNode leaf = repo.createNewNode("http://www.example.com/testDir/test.cache");
    assertTrue(!leaf.hasContent());
    assertTrue(leaf.getCurrentVersion()==0);
    leaf.makeNewVersion();

    OutputStream os = leaf.getNewOutputStream();
    InputStream is = new StringInputStream("testing stream");
    StreamUtil.copy(is, os);
    os.close();
    is.close();
    leaf.sealNewVersion();
    assertTrue(leaf.getCurrentVersion()==1);
    assertTrue(leaf.hasContent());
  }

  public void testMakeNewVersion() throws IOException {
    RepositoryNode leaf = repo.createNewNode("http://www.example.com/testDir/test.cache");
    leaf.makeNewVersion();
    OutputStream os = leaf.getNewOutputStream();
    Properties props = new Properties();
    props.setProperty("test 1", "value 1");
    leaf.setNewProperties(props);
    InputStream is = new StringInputStream("testing stream 1");
    StreamUtil.copy(is, os);
    os.close();
    is.close();

    leaf.sealNewVersion();
    assertTrue(leaf.getCurrentVersion()==1);
    leaf.makeNewVersion();
    props = new Properties();
    props.setProperty("test 1", "value 2");
    leaf.setNewProperties(props);
    os = leaf.getNewOutputStream();
    is = new StringInputStream("testing stream 2");
    StreamUtil.copy(is, os);
    os.close();
    is.close();
    leaf.sealNewVersion();
    assertTrue(leaf.getCurrentVersion()==2);

    is = leaf.getInputStream();
    OutputStream baos = new ByteArrayOutputStream(16);
    StreamUtil.copy(is, baos);
    is.close();
    String resultStr = baos.toString();
    baos.close();
    assertTrue(resultStr.equals("testing stream 2"));
    props = leaf.getProperties();
    assertTrue(props.getProperty("test 1").equals("value 2"));
  }

  public void testGetInputStream() throws IOException {
    RepositoryNode leaf = repo.createNewNode("http://www.example.com/testDir/test.cache");
    leaf.makeNewVersion();
    OutputStream os = leaf.getNewOutputStream();
    InputStream is = new StringInputStream("testing stream");
    StreamUtil.copy(is, os);
    os.close();
    is.close();
    leaf.sealNewVersion();

    is = leaf.getInputStream();
    OutputStream baos = new ByteArrayOutputStream(14);
    StreamUtil.copy(is, baos);
    is.close();
    String resultStr = baos.toString();
    baos.close();
    assertTrue(resultStr.equals("testing stream"));
  }

  public void testGetProperties() throws IOException {
    RepositoryNode leaf = repo.createNewNode("http://www.example.com/testDir/test.cache");
    leaf.makeNewVersion();
    Properties props = new Properties();
    props.setProperty("test 1", "value 1");
    leaf.setNewProperties(props);
    OutputStream os = leaf.getNewOutputStream();
    InputStream is = new StringInputStream("testing stream");
    StreamUtil.copy(is, os);
    os.close();
    is.close();
    leaf.sealNewVersion();

    props = leaf.getProperties();
    assertTrue(props.getProperty("test 1").equals("value 1"));

    leaf.makeNewVersion();
    props = new Properties();
    props.setProperty("test 1", "value 2");
    leaf.setNewProperties(props);
    os = leaf.getNewOutputStream();
    is = new StringInputStream("testing stream");
    StreamUtil.copy(is, os);
    os.close();
    is.close();
    leaf.sealNewVersion();

    props = leaf.getProperties();
    assertTrue(props.getProperty("test 1").equals("value 2"));
  }

  public void testDirContent() throws IOException {
    RepositoryNode leaf = repo.createNewNode("http://www.example.com/testDir/test.cache");
    leaf.makeNewVersion();
    OutputStream os = leaf.getNewOutputStream();
    InputStream is = new StringInputStream("testing stream");
    StreamUtil.copy(is, os);
    os.close();
    is.close();
    leaf.sealNewVersion();
    assertTrue(leaf.hasContent());

    RepositoryNode dir = repo.getRepositoryNode("http://www.example.com/testDir");
    dir.makeNewVersion();
    os = dir.getNewOutputStream();
    is = new StringInputStream("testing stream");
    StreamUtil.copy(is, os);
    os.close();
    is.close();
    dir.sealNewVersion();
    assertTrue(dir.hasContent());

    dir = repo.createNewNode("http://www.example.com/testDir/test.cache/new.test");
    dir.makeNewVersion();
    os = dir.getNewOutputStream();
    is = new StringInputStream("testing stream");
    StreamUtil.copy(is, os);
    os.close();
    is.close();
    dir.sealNewVersion();
    assertTrue(dir.hasContent());
  }

}
