/*
 * $Id: TestInternalNodeImpl.java,v 1.1 2002-10-31 01:52:41 aalto Exp $
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
import java.util.*;
import java.io.File;
import junit.framework.TestCase;
import org.lockss.test.*;
import java.net.MalformedURLException;

/**
 * This is the test class for org.lockss.daemon.DirectoryNodeImpl
 * It also tests some RepositoryNodeImpl calls.
 */

public class TestInternalNodeImpl extends LockssTestCase {
  public TestInternalNodeImpl(String msg) {
    super(msg);
  }

  public void testRepositoryImpl() {
    RepositoryNode entry = new InternalNodeImpl("testUrl", "testDir", "");
    assertTrue(entry.getNodeUrl().equals("testUrl"));
    assertTrue(!entry.isLeaf());
    entry = new LeafNodeImpl("testUrl/test.txt", "testUrl/test.txt");
    assertTrue(entry.getNodeUrl().equals("testUrl/test.txt"));
    assertTrue(entry.isLeaf());
  }

  public void testGetState() {
    //XXX implement
  }

  public void testStoreState() {
    //XXX implement
  }

  public void testListEntries() throws MalformedURLException {
    String tempDirPath = "";
    try {
      tempDirPath = super.getTempDir().getAbsolutePath() + File.separator;
    } catch (Exception ex) { fail("Couldn't create tempDir."); }
    LockssRepository repo = new LockssRepositoryImpl(tempDirPath);
    LeafNode leaf = repo.createLeafNode("http://www.example.com/testDir/branch1/leaf1");
    leaf.makeNewVersion();
    leaf.sealNewVersion();
    leaf = repo.createLeafNode("http://www.example.com/testDir/branch1/leaf2");
    leaf.makeNewVersion();
    leaf.sealNewVersion();
    leaf = repo.createLeafNode("http://www.example.com/testDir/branch2/leaf3");
    leaf.makeNewVersion();
    leaf.sealNewVersion();
    leaf = repo.createLeafNode("http://www.example.com/testDir/leaf4");
    leaf.makeNewVersion();
    leaf.sealNewVersion();

    String cacheLocation = tempDirPath + LockssRepositoryImpl.mapUrlToCacheLocation("http://www.example.com/testDir");
    InternalNode dirEntry = new InternalNodeImpl("http://www.example.com/testDir", cacheLocation, tempDirPath);
    Iterator childIt = dirEntry.listNodes(null);
    int count = 0;
    while (childIt.hasNext()) {
      RepositoryNode entry = (RepositoryNode)childIt.next();
      if (entry.getNodeUrl().equals("http://www.example.com/testDir/branch1")) {
        count += 1;
        assertTrue(!entry.isLeaf());
      } else if (entry.getNodeUrl().equals("http://www.example.com/testDir/branch2")) {
        count += 2;
        assertTrue(!entry.isLeaf());
      } else if (entry.getNodeUrl().equals("http://www.example.com/testDir/leaf4")) {
        count += 4;
        assertTrue(entry.isLeaf());
      }
    }
    assertTrue(count==7);

    cacheLocation = tempDirPath + LockssRepositoryImpl.mapUrlToCacheLocation("http://www.example.com/testDir/branch1");
    dirEntry = new InternalNodeImpl("http://www.example.com/testDir/branch1", cacheLocation, tempDirPath);
    childIt = dirEntry.listNodes(null);
    count = 0;
    while (childIt.hasNext()) {
      RepositoryNode entry = (RepositoryNode)childIt.next();
      if (entry.getNodeUrl().equals("http://www.example.com/testDir/branch1/leaf1")) {
        count += 1;
        assertTrue(entry.isLeaf());
      } else if (entry.getNodeUrl().equals("http://www.example.com/testDir/branch1/leaf2")) {
        count += 2;
        assertTrue(entry.isLeaf());
      }
    }
    assertTrue(count==3);
  }
  public void testEntrySort() throws MalformedURLException {
    String tempDirPath = "";
    try {
      tempDirPath = super.getTempDir().getAbsolutePath() + File.separator;
    } catch (Exception ex) { fail("Couldn't get tempDir."); }
    LockssRepository repo = new LockssRepositoryImpl(tempDirPath);
    LeafNode leaf = repo.createLeafNode("http://www.example.com/testDir/branch1/leaf1");
    leaf.makeNewVersion();
    leaf.sealNewVersion();
    leaf = repo.createLeafNode("http://www.example.com/testDir/branch2/leaf2");
    leaf.makeNewVersion();
    leaf.sealNewVersion();
    leaf = repo.createLeafNode("http://www.example.com/testDir/leaf4");
    leaf.makeNewVersion();
    leaf.sealNewVersion();
    leaf = repo.createLeafNode("http://www.example.com/testDir/leaf3");
    leaf.makeNewVersion();
    leaf.sealNewVersion();

    InternalNode dirEntry = (InternalNode)repo.getRepositoryNode("http://www.example.com/testDir");
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
}
