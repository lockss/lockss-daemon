/*
 * $Id: TestLockssRepositoryImpl.java,v 1.2 2002-10-31 01:53:30 aalto Exp $
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
 * This is the test class for org.lockss.daemon.LockssRepositoryImpl
 */

public class TestLockssRepositoryImpl extends LockssTestCase {

  public TestLockssRepositoryImpl(String msg) {
    super(msg);
  }


  public void testGetRepositoryEntry() throws MalformedURLException {
    String tempDirPath = "";
    try {
      tempDirPath = super.getTempDir().getAbsolutePath() + File.separator;
    } catch (Exception ex) { assertTrue("Couldn't get tempDir.", false); }
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

    RepositoryNode entry = repo.getRepositoryNode("http://www.example.com/testDir");
    assertTrue(!entry.isLeaf());
    assertTrue(entry instanceof InternalNode);
    entry = repo.getRepositoryNode("http://www.example.com/testDir/branch1");
    assertTrue(!entry.isLeaf());
    assertTrue(entry instanceof InternalNode);
    entry = repo.getRepositoryNode("http://www.example.com/testDir/branch2/leaf3");
    assertTrue(entry.isLeaf());
    assertTrue(entry instanceof LeafNode);
    entry = repo.getRepositoryNode("http://www.example.com/testDir/leaf4");
    assertTrue(entry.isLeaf());
    assertTrue(entry instanceof LeafNode);

  }

  public void testMapUrlToCacheLocation() throws MalformedURLException {
    String testStr = "http://www.example.com/branch1/branch2/index.html";
    String expectedStr = LockssRepositoryImpl.CACHE_ROOT_NAME +
                         "/www.example.com/http/branch1/branch2/index.html";
    assertTrue(LockssRepositoryImpl.mapUrlToCacheLocation(testStr).equals(expectedStr));

    try {
      testStr = ":/brokenurl.com/branch1/index/";
      LockssRepositoryImpl.mapUrlToCacheLocation(testStr);
      fail("Should have thrown MalformedURLException");
    } catch (MalformedURLException mue) { }
  }
}
