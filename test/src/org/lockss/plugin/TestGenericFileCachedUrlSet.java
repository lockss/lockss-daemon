/*
 * $Id: TestGenericFileCachedUrlSet.java,v 1.36 2003-05-01 20:02:39 tal Exp $
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
import org.lockss.state.*;
import org.lockss.test.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.util.StreamUtil;
import org.lockss.hasher.HashService;

/**
 * This is the test class for
 * org.lockss.plugin.simulated.GenericFileCachedUrlSet.
 *
 * @author  Emil Aalto
 * @version 0.0
 */
public class TestGenericFileCachedUrlSet extends LockssTestCase {
  private LockssRepository repo;
  private NodeManager nodeMan;
  private MockGenericFileArchivalUnit mgfau;
  private MockLockssDaemon theDaemon;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    String s = LockssRepositoryServiceImpl.PARAM_CACHE_LOCATION + "=" +
        tempDirPath +"\n";
    String s2 = HistoryRepositoryImpl.PARAM_HISTORY_LOCATION + "=" +
        tempDirPath;
    TestConfiguration.setCurrentConfigFromString(s + s2);

    theDaemon = new MockLockssDaemon();
    theDaemon.getLockssRepositoryService().startService();
    theDaemon.getHistoryRepository().startService();
    theDaemon.getHashService().startService();
    theDaemon.getSystemMetrics().startService();

    mgfau = new MockGenericFileArchivalUnit();
    MockPlugin plugin = new MockPlugin();
    plugin.setDefiningConfigKeys(Collections.EMPTY_LIST);
    mgfau.setPlugin(plugin);
    repo = theDaemon.getLockssRepository(mgfau);
    nodeMan = theDaemon.getNodeManager(mgfau);
    ((NodeManagerImpl)nodeMan).killTreeWalk();
  }

  public void tearDown() throws Exception {
    repo.stopService();
    nodeMan.stopService();
    theDaemon.getLockssRepositoryService().stopService();
    theDaemon.getHistoryRepository().stopService();
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

  public void testFlatSetIteratorSingleNodeSpec() throws Exception {
    createLeaf("http://www.example.com/testDir/leaf4", null, null);
    createLeaf("http://www.example.com/testDir/branch1/leaf1", null, null);
    createLeaf("http://www.example.com/testDir/branch2/leaf3", null, null);
    createLeaf("http://www.example.com/testDir/branch1/leaf2", null, null);

    CachedUrlSetSpec spec =
        new SingleNodeCachedUrlSetSpec("http://www.example.com/testDir");
    CachedUrlSet fileSet = mgfau.makeCachedUrlSet(spec);
    Iterator setIt = fileSet.flatSetIterator();
    assertFalse(setIt.hasNext());
  }

  public void testFlatSetIteratorThrowsOnBogusURL() throws Exception {
    createLeaf("http://www.example.com/testDir/leaf4", null, null);

    CachedUrlSetSpec rSpec =
        new RangeCachedUrlSetSpec("no_such_protoco://www.example.com/testDir");
    CachedUrlSet fileSet = mgfau.makeCachedUrlSet(rSpec);
    try {
      fileSet.flatSetIterator();
      fail("Call to flatSetIterator() should have thrown when given "
	   +"malformed url");
    } catch(RuntimeException e){
    }
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
    assertRightClass((CachedUrlSetNode)setIt.next(),
                   "http://www.example.com/testDir/branch1", true);
    assertRightClass((CachedUrlSetNode)setIt.next(),
                   "http://www.example.com/testDir/branch2", true);
    assertRightClass((CachedUrlSetNode)setIt.next(),
                   "http://www.example.com/testDir/leaf1", false);
  }

  public void testHashIterator() throws Exception {
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
    Iterator setIt = fileSet.contentHashIterator();
    ArrayList childL = new ArrayList(7);
    while (setIt.hasNext()) {
      childL.add(((CachedUrlSetNode)setIt.next()).getUrl());
    }
    // should be sorted
    String[] expectedA = new String[] {
      "http://www.example.com/testDir",
      "http://www.example.com/testDir/branch1",
      "http://www.example.com/testDir/branch1/leaf1",
      "http://www.example.com/testDir/branch1/leaf2",
      "http://www.example.com/testDir/branch2",
      "http://www.example.com/testDir/branch2/leaf3",
      "http://www.example.com/testDir/leaf4"
      };
    assertIsomorphic(expectedA, childL);

    // add content to an internal node
    // should behave normally
    createLeaf("http://www.example.com/testDir/branch1", "test stream", null);
    rSpec = new RangeCachedUrlSetSpec("http://www.example.com/testDir/branch1");
    fileSet = mgfau.makeCachedUrlSet(rSpec);
    setIt = fileSet.contentHashIterator();
    childL = new ArrayList(3);
    while (setIt.hasNext()) {
      childL.add(((CachedUrlSetNode)setIt.next()).getUrl());
    }
    assertFalse(setIt.hasNext());
    try {
      setIt.next();
      fail("setIt.next() should have thrown when it has no elements");
    } catch (NoSuchElementException e) {
    }

    // should be sorted
    expectedA = new String[] {
      "http://www.example.com/testDir/branch1",
      "http://www.example.com/testDir/branch1/leaf1",
      "http://www.example.com/testDir/branch1/leaf2"
      };
    assertIsomorphic(expectedA, childL);
  }

  public void testHashIteratorSingleNode() throws Exception {
    createLeaf("http://www.example.com/testDir/branch1/leaf2",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf4", "test stream", null);
    createLeaf("http://www.example.com/testDir/branch2/leaf3",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch1/leaf1",
               "test stream", null);

    CachedUrlSetSpec spec =
      new SingleNodeCachedUrlSetSpec("http://www.example.com/testDir");
    CachedUrlSet fileSet = mgfau.makeCachedUrlSet(spec);
    Iterator setIt = fileSet.contentHashIterator();
    ArrayList childL = new ArrayList(1);
    while (setIt.hasNext()) {
      childL.add(((CachedUrlSetNode)setIt.next()).getUrl());
    }
    // should be sorted
    String[] expectedA = new String[] {
      "http://www.example.com/testDir",
    };
    assertIsomorphic(expectedA, childL);
  }

  public void testHashIteratorThrowsOnBogusUrl() throws Exception {
    createLeaf("http://www.example.com/testDir/branch1/leaf2",
               "test stream", null);

    CachedUrlSetSpec spec =
      new RangeCachedUrlSetSpec("bad_protocol://www.example.com/testDir");
    CachedUrlSet fileSet = mgfau.makeCachedUrlSet(spec);
    try {
      Iterator setIt = fileSet.contentHashIterator();
      fail("Bogus url should have caused a RuntimeException");
    } catch (RuntimeException e){
    }
  }

  public void testHashIteratorVariations() throws Exception {
    createLeaf("http://www.example.com/testDir/branch1/leaf2",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch1/leaf1",
               "test stream", null);

    CachedUrlSetSpec rSpec =
        new RangeCachedUrlSetSpec("http://www.example.com/testDir/branch1",
                                  "/leaf1", "/leaf2");
    CachedUrlSet fileSet = mgfau.makeCachedUrlSet(rSpec);
    Iterator setIt = fileSet.contentHashIterator();
    ArrayList childL = new ArrayList(2);
    while (setIt.hasNext()) {
      childL.add(((CachedUrlSetNode)setIt.next()).getUrl());
    }
    // should exclude 'branch1'
    String[] expectedA = new String[] {
      "http://www.example.com/testDir/branch1/leaf1",
      "http://www.example.com/testDir/branch1/leaf2"
    };
    assertIsomorphic(expectedA, childL);

    CachedUrlSetSpec snSpec =
        new SingleNodeCachedUrlSetSpec("http://www.example.com/testDir/branch1");
    fileSet = mgfau.makeCachedUrlSet(snSpec);
    setIt = fileSet.contentHashIterator();
    childL = new ArrayList(1);
    while (setIt.hasNext()) {
      childL.add(((CachedUrlSetNode)setIt.next()).getUrl());
    }
    // should include only 'branch1'
    expectedA = new String[] {
      "http://www.example.com/testDir/branch1"
      };
    assertIsomorphic(expectedA, childL);
  }

  public void testHashIteratorClassCreation() throws Exception {
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
    Iterator setIt = fileSet.contentHashIterator();
    assertRightClass((CachedUrlSetNode)setIt.next(),
		     "http://www.example.com/testDir", true);
    assertRightClass((CachedUrlSetNode)setIt.next(),
		     "http://www.example.com/testDir/branch1", true);
    assertRightClass((CachedUrlSetNode)setIt.next(),
		     "http://www.example.com/testDir/branch1/leaf1", false);
    assertRightClass((CachedUrlSetNode)setIt.next(),
		     "http://www.example.com/testDir/branch2", true);
    assertRightClass((CachedUrlSetNode)setIt.next(),
		     "http://www.example.com/testDir/branch2/branch3", true);
    assertRightClass((CachedUrlSetNode)setIt.next(),
		     "http://www.example.com/testDir/branch2/branch3/leaf2",
		     false);
    assertRightClass((CachedUrlSetNode)setIt.next(),
		     "http://www.example.com/testDir/leaf3", false);
  }

  private void assertRightClass(CachedUrlSetNode element,
				String url, boolean isCus) {
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
    GenericFileCachedUrlSet fileSet =
        (GenericFileCachedUrlSet)mgfau.makeCachedUrlSet(rSpec);
    fileSet.calculateNodeSize();
 //   assertEquals(4, ((GenericFileCachedUrlSet)fileSet).contentNodeCount);
    assertEquals(48, ((GenericFileCachedUrlSet)fileSet).totalNodeSize);
  }

  public void testHashEstimation() throws Exception {
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

    assertEquals(estimate,
                 nodeMan.getNodeState(fileSet).getAverageHashDuration());

    // test return of stored duration
    long estimate2 = fileSet.estimatedHashDuration();
    assertEquals(estimate, estimate2);
    assertEquals(estimate,
                 nodeMan.getNodeState(fileSet).getAverageHashDuration());

    // test averaging of durations
    fileSet.storeActualHashDuration(estimate2 + 200, null);
    long estimate3 = fileSet.estimatedHashDuration();
    assertEquals(estimate2 + 100, estimate3);
    assertEquals(estimate3,
                 nodeMan.getNodeState(fileSet).getAverageHashDuration());
  }

  public void testSingleNodeHashEstimation() throws Exception {
    byte[] bytes = new byte[1000];
    Arrays.fill(bytes, (byte)1);
    String testString = new String(bytes);
    // check that estimation is special for single nodes, and isn't stored
    createLeaf("http://www.example.com/testDir", testString, null);
    CachedUrlSetSpec sSpec =
        new SingleNodeCachedUrlSetSpec("http://www.example.com/testDir");
    CachedUrlSet fileSet = mgfau.makeCachedUrlSet(sSpec);
    long estimate = fileSet.estimatedHashDuration();
    // tk - fix this with a MockSystemMetrics so we can set the hash speed
//     assertTrue(estimate > 0);
    long expectedEstimate = 1000 /
        SystemMetrics.getSystemMetrics().getBytesPerMsHashEstimate();
    assertEquals(expectedEstimate, estimate);
    // check that estimation isn't stored for single node sets
    assertEquals(-1, nodeMan.getNodeState(fileSet).getAverageHashDuration());
  }

  public void testIrregularHashStorage() throws Exception {
    // check that estimation isn't changed for single node sets
    createLeaf("http://www.example.com/testDir", null, null);
    CachedUrlSetSpec sSpec =
        new SingleNodeCachedUrlSetSpec("http://www.example.com/testDir");
    CachedUrlSet fileSet = mgfau.makeCachedUrlSet(sSpec);
    fileSet.storeActualHashDuration(123, null);
    assertEquals(-1, nodeMan.getNodeState(fileSet).getAverageHashDuration());

    // check that estimation isn't changed for ranged sets
    CachedUrlSetSpec rSpec =
        new RangeCachedUrlSetSpec("http://www.example.com/testDir", "ab", "yz");
    fileSet = mgfau.makeCachedUrlSet(rSpec);
    fileSet.storeActualHashDuration(123, null);
    assertEquals(-1, nodeMan.getNodeState(fileSet).getAverageHashDuration());

    // check that estimation isn't changed for exceptions
    rSpec = new RangeCachedUrlSetSpec("http://www.example.com/testDir");
    fileSet = mgfau.makeCachedUrlSet(rSpec);
    fileSet.storeActualHashDuration(123, new Exception("bad"));
    assertEquals(-1, nodeMan.getNodeState(fileSet).getAverageHashDuration());

    // check that estimation is grown for timeout exceptions
    rSpec = new RangeCachedUrlSetSpec("http://www.example.com/testDir");
    fileSet = mgfau.makeCachedUrlSet(rSpec);
    fileSet.storeActualHashDuration(100, null);
    assertEquals(100, nodeMan.getNodeState(fileSet).getAverageHashDuration());
    // simulate a timeout
    fileSet.storeActualHashDuration(200, new HashService.Timeout("test"));
    assertEquals(300, nodeMan.getNodeState(fileSet).getAverageHashDuration());
    // and another,less than current estimate, shouldn't change it
    fileSet.storeActualHashDuration(100, new HashService.Timeout("test"));
    assertEquals(300, nodeMan.getNodeState(fileSet).getAverageHashDuration());
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
