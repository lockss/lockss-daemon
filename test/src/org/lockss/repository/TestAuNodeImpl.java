/*
 * $Id: TestAuNodeImpl.java,v 1.2 2003-03-04 00:16:12 aalto Exp $
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
import org.lockss.util.*;
import org.lockss.plugin.AuUrl;

/**
 * This is the test class for org.lockss.repostiory.RepositoryNodeImpl
 */
public class TestAuNodeImpl extends LockssTestCase {
  private LockssRepository repo;
  private String tempDirPath;

  public TestAuNodeImpl(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    TestLockssRepositoryServiceImpl.configCacheLocation(tempDirPath);
    MockArchivalUnit mau = new MockArchivalUnit(null);
    LockssRepositoryServiceImpl lrsi = new LockssRepositoryServiceImpl();
    repo = lrsi.addLockssRepository(mau);
  }

  public void testListEntries() throws Exception {
    TestRepositoryNodeImpl.createLeaf(repo,
                                      "http://www.example.com/testDir/leaf1",
                                      "test stream", null);
    TestRepositoryNodeImpl.createLeaf(repo,
                                      "http://www.example.com/testDir/leaf2",
                                      "test stream", null);
    TestRepositoryNodeImpl.createLeaf(repo,
                                      "http://image.example.com/image1.gif",
                                      "test stream", null);
    TestRepositoryNodeImpl.createLeaf(repo, "ftp://www.example.com/file1",
                                      "test stream", null);

    RepositoryNode auNode = repo.getNode(AuUrl.PROTOCOL_COLON+"//test.com");
    Iterator childIt = auNode.listNodes(null, false);
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

  public void testIllegalOperations() throws Exception {
    RepositoryNode auNode = new AuNodeImpl("lockssAu:test", "", null);
    assertTrue(!auNode.hasContent());
    assertTrue(!auNode.isLeaf());
    assertTrue(!auNode.isInactive());
    try {
      auNode.makeNewVersion();
      fail("Cannot make version for AuNode.");
    } catch (UnsupportedOperationException uoe) { }
    try {
      auNode.deactivate();
      fail("Cannot deactivate AuNode.");
    } catch (UnsupportedOperationException uoe) { }
  }
}
