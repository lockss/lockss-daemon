/*
 * $Id: TestGenericFileCachedUrlSet.java,v 1.26 2003-04-10 01:24:35 aalto Exp $
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

package org.lockss.plugin;

import java.io.*;
import java.util.*;
import java.net.MalformedURLException;
import org.lockss.daemon.*;
import org.lockss.repository.*;
import org.lockss.test.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.util.StreamUtil;
import org.lockss.repository.TestRepositoryNodeImpl;
import org.lockss.state.*;

/**
 * This is the test class for
 * org.lockss.plugin.simulated.GenericFileCachedUrlSet.
 *
 * @author  Emil Aalto
 * @version 0.0
 */
public class TestGenericFileCachedUrlSet extends LockssTestCase {
  private LockssRepository repo;
  private MockGenericFileArchivalUnit mgfau;
  private MockLockssDaemon theDaemon;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    TestLockssRepositoryServiceImpl.configCacheLocation(tempDirPath);

    theDaemon = new MockLockssDaemon();
    theDaemon.getLockssRepositoryService().startService();
    theDaemon.setNodeManagerService(new MockNodeManagerService());

    mgfau = new MockGenericFileArchivalUnit();
    repo = theDaemon.getLockssRepository(mgfau);
    theDaemon.getNodeManager(mgfau);
  }

  public void tearDown() throws Exception {
    theDaemon.getLockssRepositoryService().stopService();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  public void testFlatSetIterator() throws Exception {
    createLeaf("http://www.example.com/testDir/leaf4", null, null);
    createLeaf("http://www.example.com/testDir/branch1/leaf1", null, null);
    createLeaf("http://www.example.com/testDir/branch2/leaf3", null, null);
    createLeaf("http://www.example.com/testDir/branch1/leaf2", null, null);

    CachedUrlSetSpec rSpec =
        new RangeCachedUrlSetSpec("http://www.example.com/testDir");
    CachedUrlSet fileSet = mgfau.makeCachedUrlSet(rSpec);
    Iterator setIt = fileSet.flatSetIterator();
    ArrayList childL = new ArrayList(3);
    while (setIt.hasNext()) {
      childL.add(((CachedUrlSetNode)setIt.next()).getUrl());
    }
    // should be sorted
    String[] expectedA = new String[] {
      "http://www.example.com/testDir/branch1",
      "http://www.example.com/testDir/branch2",
      "http://www.example.com/testDir/leaf4"
      };
    assertIsomorphic(expectedA, childL);
  }

  public void testFlatSetIteratorClassCreation() throws Exception {
    createLeaf("http://www.example.com/testDir/leaf1", "test stream", null);
    createLeaf("http://www.example.com/testDir/branch1/leaf2", null, null);
    createLeaf("http://www.example.com/testDir/branch2/leaf3",
               "test stream", null);

    CachedUrlSetSpec rSpec =
        new RangeCachedUrlSetSpec("http://www.example.com/testDir");
    CachedUrlSet fileSet = mgfau.makeCachedUrlSet(rSpec);
    Iterator setIt = fileSet.flatSetIterator();
    testRightClass((CachedUrlSetNode)setIt.next(),
                   "http://www.example.com/testDir/branch1", true);
    testRightClass((CachedUrlSetNode)setIt.next(),
                   "http://www.example.com/testDir/branch2", true);
    testRightClass((CachedUrlSetNode)setIt.next(),
                   "http://www.example.com/testDir/leaf1", false);
  }

  public void testTreeIterator() throws Exception {
    createLeaf("http://www.example.com/testDir/branch1/leaf2",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf4", "test stream", null);
    createLeaf("http://www.example.com/testDir/branch2/leaf3",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch1/leaf1",
               "test stream", null);

    CachedUrlSetSpec rSpec =
        new RangeCachedUrlSetSpec("http://www.example.com/testDir");
    CachedUrlSet fileSet = mgfau.makeCachedUrlSet(rSpec);
    Iterator setIt = fileSet.treeIterator();
    ArrayList childL = new ArrayList(4);
    while (setIt.hasNext()) {
      childL.add(((CachedUrlSetNode)setIt.next()).getUrl());
    }
    // should be sorted
    String[] expectedA = new String[] {
      "http://www.example.com/testDir/branch1",
      "http://www.example.com/testDir/branch1/leaf1",
      "http://www.example.com/testDir/branch1/leaf2",
      "http://www.example.com/testDir/branch2",
      "http://www.example.com/testDir/branch2/leaf3",
      "http://www.example.com/testDir/leaf4"
      };
    assertIsomorphic(expectedA, childL);

    // add content to an internal node
    createLeaf("http://www.example.com/testDir/branch1", "test stream", null);
    rSpec = new RangeCachedUrlSetSpec("http://www.example.com/testDir/branch1");
    fileSet = mgfau.makeCachedUrlSet(rSpec);
    setIt = fileSet.treeIterator();
    childL = new ArrayList(4);
    while (setIt.hasNext()) {
      childL.add(((CachedUrlSetNode)setIt.next()).getUrl());
    }
    // should be sorted
    // no longer includes itself as CachedUrl
    expectedA = new String[] {
      "http://www.example.com/testDir/branch1/leaf1",
      "http://www.example.com/testDir/branch1/leaf2",
      };
    assertIsomorphic(expectedA, childL);
  }

  public void testTreeIteratorClassCreation() throws Exception {
    createLeaf("http://www.example.com/testDir/branch1", "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf3", "test stream", null);
    createLeaf("http://www.example.com/testDir/branch2/branch3",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch2/branch3/leaf2",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch1/leaf1",
               "test stream", null);

    CachedUrlSetSpec rSpec =
        new RangeCachedUrlSetSpec("http://www.example.com/testDir");
    CachedUrlSet fileSet = mgfau.makeCachedUrlSet(rSpec);
    Iterator setIt = fileSet.treeIterator();
    testRightClass((CachedUrlSetNode)setIt.next(),
                   "http://www.example.com/testDir/branch1", true);
    testRightClass((CachedUrlSetNode)setIt.next(),
                   "http://www.example.com/testDir/branch1/leaf1", false);
    testRightClass((CachedUrlSetNode)setIt.next(),
                   "http://www.example.com/testDir/branch2", true);
    testRightClass((CachedUrlSetNode)setIt.next(),
                   "http://www.example.com/testDir/branch2/branch3", true);
    testRightClass((CachedUrlSetNode)setIt.next(),
                   "http://www.example.com/testDir/branch2/branch3/leaf2", false);
    testRightClass((CachedUrlSetNode)setIt.next(),
                   "http://www.example.com/testDir/leaf3", false);
  }

  private void testRightClass(CachedUrlSetNode element, String url, boolean isCus) {
    assertEquals(url, element.getUrl());
    if (isCus) {
      assertEquals(CachedUrlSetNode.TYPE_CACHED_URL_SET, element.getType());
      assertTrue(element instanceof CachedUrlSet);
      assertFalse(element.isLeaf());
    } else {
      assertEquals(CachedUrlSetNode.TYPE_CACHED_URL, element.getType());
      assertTrue(element instanceof CachedUrl);
      assertTrue(element.isLeaf());
    }
  }

  public void testNodeCounting() throws Exception {
    createLeaf("http://www.example.com/testDir/branch1/leaf1",
               "test streamAA", null);
    createLeaf("http://www.example.com/testDir/branch1/leaf2",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch2/leaf3",
               "test streamB", null);
    createLeaf("http://www.example.com/testDir/leaf4", "test streamC", null);

    CachedUrlSetSpec rSpec =
        new RangeCachedUrlSetSpec("http://www.example.com/testDir");
    CachedUrlSet fileSet = mgfau.makeCachedUrlSet(rSpec);
    fileSet.treeIterator();
    assertEquals(4, ((GenericFileCachedUrlSet)fileSet).contentNodeCount);
    assertEquals(48, ((GenericFileCachedUrlSet)fileSet).totalNodeSize);
  }

  public void testHashEstimation() throws Exception {
    MockNodeManager nodeMan = (MockNodeManager)theDaemon.getNodeManager(mgfau);

    byte[] bytes = new byte[100];
    Arrays.fill(bytes, (byte)1);
    String testString = new String(bytes);
    for (int ii=0; ii<10; ii++) {
      createLeaf("http://www.example.com/testDir/leaf"+ii, testString, null);
    }
    CachedUrlSetSpec rSpec =
        new RangeCachedUrlSetSpec("http://www.example.com/testDir");
    CachedUrlSet fileSet = mgfau.makeCachedUrlSet(rSpec);
    long estimate = fileSet.estimatedHashDuration();
    assertTrue(estimate > 0);

    assertNull(nodeMan.hashCalls.get("http://www.example.com/testDir"));

    fileSet.storeActualHashDuration(estimate, null);
    // test return of stored duration
    long estimate2 = fileSet.estimatedHashDuration();
    assertEquals(estimate, estimate2);
    assertEquals(estimate,
                 ((Long)nodeMan.hashCalls.get(
        "http://www.example.com/testDir")).intValue());

    // test averaging of durations
    fileSet.storeActualHashDuration(estimate2 + 200, null);
    long estimate3 = fileSet.estimatedHashDuration();
    assertEquals(estimate2 + 100, estimate3);
    assertEquals(estimate3,
                 ((Long)nodeMan.hashCalls.get(
        "http://www.example.com/testDir")).intValue());
  }

  private RepositoryNode createLeaf(String url, String content,
                                    Properties props) throws Exception {
    return TestRepositoryNodeImpl.createLeaf(repo, url, content, props);
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestGenericFileCachedUrlSet.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }


}
