/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.base;

import java.io.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.repository.*;
import org.lockss.state.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.hasher.HashService;
import org.lockss.scheduler.*;

/**
 * This is the test class for
 * org.lockss.plugin.base.BaseCachedUrlSet.
 *
 * @author  Emil Aalto
 * @version 0.0
 */
public class TestBaseCachedUrlSet extends LockssTestCase {
  private LockssRepository repo;
  private NodeManager nodeMan;
  private HashService hashService;
  private MockArchivalUnit mau;
  private MockLockssDaemon theDaemon;
  private MockPlugin plugin;
  private SystemMetrics metrics;

  static final int HASH_SPEED = 100;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
		      tempDirPath);
    props.setProperty(SystemMetrics.PARAM_DEFAULT_HASH_SPEED,
		      Integer.toString(HASH_SPEED));
    ConfigurationUtil.setCurrentConfigFromProps(props);

    theDaemon = getMockLockssDaemon();
    hashService = theDaemon.getHashService();
    hashService.startService();
    metrics = theDaemon.getSystemMetrics();
    metrics.startService();

    mau = new MyMockArchivalUnit();
    plugin = new MockPlugin();
    plugin.initPlugin(theDaemon);
    mau.setPlugin(plugin);

    theDaemon.getHistoryRepository(mau).startService();
    repo = theDaemon.getLockssRepository(mau);
    nodeMan = theDaemon.getNodeManager(mau);
    nodeMan.startService();
  }

  public void tearDown() throws Exception {
    repo.stopService();
    nodeMan.stopService();
    hashService.stopService();
    theDaemon.getHistoryRepository(mau).stopService();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  public void testExcludeByDate() throws Exception {
    String base = "http://www.example.com/testDir";
    createLeaf(base + "/leaf1", null, null);
    createLeaf(base + "/leaf2", null,
	       PropUtil.fromArgs(CachedUrl.PROPERTY_FETCH_TIME, "55555"));
    createLeaf(base + "/leaf3", null,
	       PropUtil.fromArgs(CachedUrl.PROPERTY_FETCH_TIME, "33333"));
    createLeaf(base + "/leaf4", null,
	       PropUtil.fromArgs(CachedUrl.PROPERTY_FETCH_TIME, "bogus"));

    CachedUrlSetSpec rSpec = new RangeCachedUrlSetSpec(base);
    BaseCachedUrlSet cus = (BaseCachedUrlSet)mau.makeCachedUrlSet(rSpec);
    assertFalse(cus.isExcludedByDate(mau.makeCachedUrl(base + "/leaf1")));
    assertFalse(cus.isExcludedByDate(mau.makeCachedUrl(base + "/leaf2")));
    assertFalse(cus.isExcludedByDate(mau.makeCachedUrl(base + "/leaf3")));
    assertFalse(cus.isExcludedByDate(mau.makeCachedUrl(base + "/leaf4")));
    cus.setExcludeFilesUnchangedAfter(44444);
    assertFalse(cus.isExcludedByDate(mau.makeCachedUrl(base + "/leaf1")));
    assertFalse(cus.isExcludedByDate(mau.makeCachedUrl(base + "/leaf2")));
    assertTrue(cus.isExcludedByDate(mau.makeCachedUrl(base + "/leaf3")));
    assertFalse(cus.isExcludedByDate(mau.makeCachedUrl(base + "/leaf4")));
  }

  public void testFlatSetIterator() throws Exception {
    createLeaf("http://www.example.com/testDir/leaf4", null, null);
    createLeaf("http://www.example.com/testDir/branch1/leaf1", null, null);
    createLeaf("http://www.example.com/testDir/branch2/leaf3", null, null);
    createLeaf("http://www.example.com/testDir/branch1/leaf2", null, null);

    CachedUrlSetSpec rSpec =
        new RangeCachedUrlSetSpec("http://www.example.com/testDir");
    CachedUrlSet fileSet = mau.makeCachedUrlSet(rSpec);
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
    CachedUrlSet fileSet = mau.makeCachedUrlSet(spec);
    Iterator setIt = fileSet.flatSetIterator();
    assertFalse(setIt.hasNext());
  }

//   public void testFlatSetIteratorThrowsOnBogusURL() throws Exception {
//     CachedUrlSetSpec rSpec =
//         new RangeCachedUrlSetSpec("no_such_protoco://www.example.com/testDir");
//     CachedUrlSet fileSet = plugin.makeCachedUrlSet(mau, rSpec);
//     try {
//       fileSet.flatSetIterator();
//       fail("Call to flatSetIterator() should have thrown when given "
// 	   +"malformed url");
//     } catch(RuntimeException e){
//     }
//   }

  public void testFlatSetIteratorClassCreation() throws Exception {
    createLeaf("http://www.example.com/testDir/leaf1", "test stream", null);
    createLeaf("http://www.example.com/testDir/branch1/leaf2", null, null);
    createLeaf("http://www.example.com/testDir/branch2/leaf3",
               "test stream", null);

    CachedUrlSetSpec rSpec =
        new RangeCachedUrlSetSpec("http://www.example.com/testDir");
    CachedUrlSet fileSet = mau.makeCachedUrlSet(rSpec);
    Iterator setIt = fileSet.flatSetIterator();
    assertRightClass((CachedUrlSetNode)setIt.next(),
                   "http://www.example.com/testDir/branch1", true);
    assertRightClass((CachedUrlSetNode)setIt.next(),
                   "http://www.example.com/testDir/branch2", true);
    assertRightClass((CachedUrlSetNode)setIt.next(),
                   "http://www.example.com/testDir/leaf1", false);
  }

  public void testHashIterator() throws Exception {
    String lurl1 = "http://www.example.com/testDir/branch1/leaf1";
    String lurl2 = "http://www.example.com/testDir/branch1/leaf2";
    String lurl3 = "http://www.example.com/testDir/branch2/leaf3";
    String lurl4 = "http://www.example.com/testDir/leaf4";

    createLeaf(lurl3, "test stream", null);
    createLeaf(lurl2, "test stream", null);
    createLeaf(lurl1, "test stream", null);
    createLeaf(lurl4, "test stream", null);

    CachedUrlSetSpec rSpec =
        new RangeCachedUrlSetSpec("http://www.example.com/testDir");
    CachedUrlSet fileSet = mau.makeCachedUrlSet(rSpec);
    Iterator setIt = fileSet.contentHashIterator();
    ArrayList childL = new ArrayList(7);
    while (setIt.hasNext()) {
      childL.add(((CachedUrlSetNode)setIt.next()).getUrl());
    }
    // should be sorted
    String[] expectedA = new String[] {
      "http://www.example.com/testDir",
      "http://www.example.com/testDir/branch1",
      lurl1,
      lurl2,
      "http://www.example.com/testDir/branch2",
      lurl3,
      lurl4,
      };
    assertIsomorphic(expectedA, childL);

    // test getCuIterator and getCuIterable
    assertEquals(ListUtil.list(lurl1, lurl2, lurl3, lurl4),
		 PluginTestUtil.urlsOf(ListUtil.fromIterator(fileSet.getCuIterator())));
    assertEquals(PluginTestUtil.urlsOf(ListUtil.fromIterator(fileSet.getCuIterator())),
		 PluginTestUtil.urlsOf(ListUtil.fromIterable(fileSet.getCuIterable())));


    // add content to an internal node
    // should behave normally
    createLeaf("http://www.example.com/testDir/branch1", "test stream", null);
    rSpec = new RangeCachedUrlSetSpec("http://www.example.com/testDir/branch1");
    fileSet = mau.makeCachedUrlSet(rSpec);
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
    assertEquals(ListUtil.fromArray(expectedA),
		 PluginTestUtil.urlsOf(ListUtil.fromIterator(fileSet.getCuIterator())));
    assertEquals(PluginTestUtil.urlsOf(ListUtil.fromIterator(fileSet.getCuIterator())),
		 PluginTestUtil.urlsOf(ListUtil.fromIterable(fileSet.getCuIterable())));
  }

  public void testHashIteratorPruned() throws Exception {
    createLeaf("http://www.example.com/testDir/branch1/leaf2",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf4", "test stream", null);
    createLeaf("http://www.example.com/testDir/branch2/leaf3",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch1/leaf1",
               "test stream", null);

    CachedUrlSetSpec rSpec =
      PrunedCachedUrlSetSpec.includeMatchingSubTrees("http://www.example.com/testDir",
						     "http://www.example.com/testDir/branch1");
    CachedUrlSet fileSet = mau.makeCachedUrlSet(rSpec);
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
    CachedUrlSet fileSet = mau.makeCachedUrlSet(spec);
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

  String expMultSchemeHostPort[] = {
    "lockssau:",
    "http://aa",
    "http://aa/leaf",
    "http://aa.com",
    "http://aa.com/leaf",
    "http://aa.com:7000",
    "http://aa.com:7000/leaf",
    "http://aa.com:8000",
    "http://aa.com:8000/leaf",
    "http://aa.comx",
    "http://aa.comx/leaf",
    "http://aa:7000",
    "http://aa:7000/leaf",
    "http://aa:8000",
    "http://aa:8000/leaf",
    "http://bb",
    "http://bb/leaf",
    "http://bb:7000",
    "http://bb:7000/leaf",
    "http://bb:8000",
    "http://bb:8000/leaf",
    "https://aa",
    "https://aa/leaf",
    "https://aa:7000",
    "https://aa:7000/leaf",
    "https://aa:8000",
    "https://aa:8000/leaf",
    "https://bb",
    "https://bb/leaf",
    "https://bb:7000",
    "https://bb:7000/leaf",
    "https://bb:8000",
    "https://bb:8000/leaf",
  };

  public void testHashIteratorMultipleSchemeHostPort() throws Exception {
    Set preOrderHasContent = new TreeSet(StringUtil.PRE_ORDER_COMPARATOR);
    for (String s : expMultSchemeHostPort) {
      if (s.endsWith("leaf")) {
	createLeaf(s, "test stream", null);
	preOrderHasContent.add(s);
      }
    }

    CachedUrlSet fileSet = mau.getAuCachedUrlSet();
    Iterator setIt = fileSet.contentHashIterator();
    ArrayList nodes = new ArrayList();
    ArrayList contentNodes = new ArrayList();

    while (setIt.hasNext()) {
      CachedUrlSetNode node = ((CachedUrlSetNode)setIt.next());
      String url = node.getUrl();
      nodes.add(url);
      if (node.hasContent()) {
	contentNodes.add(url);
      }
    }
    // should be sorted
    assertIsomorphic(expMultSchemeHostPort, nodes);
    assertIsomorphic(preOrderHasContent, contentNodes);

  }

  public void testHashIteratorThrowsOnBogusUrl() throws Exception {
    createLeaf("http://www.example.com/testDir/branch1/leaf2",
               "test stream", null);

    CachedUrlSetSpec spec =
      new RangeCachedUrlSetSpec("bad_protocol://www.example.com/testDir");
    CachedUrlSet fileSet = mau.makeCachedUrlSet(spec);
    try {
      Iterator setIt = fileSet.contentHashIterator();
      fail("Bogus url should have caused a RuntimeException");
    } catch (RuntimeException e){
    }
  }

  // ensure accesses have proper null (empty) bahavior on non-existent nodes
  public void testNonExistentNode() throws Exception {
    String url = "http://no.such.host/foopath";
    assertNull(repo.getNode(url));
    doNonExistentNode(new RangeCachedUrlSetSpec(url), false);
    doNonExistentNode(new RangeCachedUrlSetSpec(url, "a", "z"), true);
    doNonExistentNode(new SingleNodeCachedUrlSetSpec(url), false);
    // make sure it didn't get created by one of the tests
    assertNull(repo.getNode(url));
  }

  void doNonExistentNode(CachedUrlSetSpec spec, boolean isRanged)
      throws Exception {
    CachedUrlSet cus = mau.makeCachedUrlSet(spec);

    Iterator flatIter = cus.flatSetIterator();
    assertFalse(flatIter.hasNext());

    Iterator hashIter = cus.contentHashIterator();
    if (!isRanged) {
      assertTrue(hashIter.hasNext());
      CachedUrlSet first = (CachedUrlSet)hashIter.next();
      assertEquals(cus, first);
    }
    assertFalse(hashIter.hasNext());

    assertFalse(cus.isLeaf());
    assertFalse(cus.hasContent());
    // Estimate won't be 0 because of padding.  Just ensure it doesn't throw
    cus.estimatedHashDuration();
  }

  public void testHashIteratorVariations() throws Exception {
    createLeaf("http://www.example.com/testDir/branch1/leaf2",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch1/leaf1",
               "test stream", null);

    CachedUrlSetSpec rSpec =
        new RangeCachedUrlSetSpec("http://www.example.com/testDir/branch1",
                                  "/leaf1", "/leaf2");
    CachedUrlSet fileSet = mau.makeCachedUrlSet(rSpec);
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
    fileSet = mau.makeCachedUrlSet(snSpec);
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
    CachedUrlSet fileSet = mau.makeCachedUrlSet(rSpec);
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
    BaseCachedUrlSet fileSet =
        (BaseCachedUrlSet)mau.makeCachedUrlSet(rSpec);
    fileSet.calculateNodeSize();
 //   assertEquals(4, ((BaseCachedUrlSet)fileSet).contentNodeCount);
    assertEquals(48, ((BaseCachedUrlSet)fileSet).totalNodeSize);
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
    CachedUrlSet fileSet = mau.makeCachedUrlSet(rSpec);
    NodeState node = nodeMan.getNodeState(fileSet);
    long estimate = fileSet.estimatedHashDuration();
    assertTrue(estimate > 0);

    assertEquals(hashService.padHashEstimate(node.getAverageHashDuration()),
		 estimate);

    // test return of stored duration
    long estimate2 = fileSet.estimatedHashDuration();
    assertEquals(estimate, estimate2);

    // test averaging of durations
    long lastActual = node.getAverageHashDuration();
    fileSet.storeActualHashDuration(lastActual + 200, null);
    assertEquals(hashService.padHashEstimate(lastActual + 100),
		 fileSet.estimatedHashDuration());

    // test special SetEstimate marker exception
    lastActual = node.getAverageHashDuration();
    fileSet.storeActualHashDuration(lastActual + 12345,
				    new HashService.SetEstimate());
    assertEquals(hashService.padHashEstimate(lastActual + 12345),
		 fileSet.estimatedHashDuration());
  }

  public void testSingleNodeHashEstimation() throws Exception {
    byte[] bytes = new byte[1000];
    Arrays.fill(bytes, (byte)1);
    String testString = new String(bytes);
    // check that estimation is special for single nodes, and isn't stored
    createLeaf("http://www.example.com/testDir", testString, null);
    CachedUrlSetSpec sSpec =
        new SingleNodeCachedUrlSetSpec("http://www.example.com/testDir");
    CachedUrlSet fileSet = mau.makeCachedUrlSet(sSpec);
    long estimate = fileSet.estimatedHashDuration();
    // SystemMetrics set speed to avoid slow machine problems
    assertTrue(estimate > 0);
    long expectedEstimate = 1000 / HASH_SPEED;
    assertEquals(estimate, hashService.padHashEstimate(expectedEstimate));
    // check that estimation isn't stored for single node sets
    assertEquals(-1, nodeMan.getNodeState(fileSet).getAverageHashDuration());
  }

  public void testIrregularHashStorage() throws Exception {
    // check that estimation isn't changed for single node sets
    createLeaf("http://www.example.com/testDir", null, null);
    CachedUrlSetSpec sSpec =
        new SingleNodeCachedUrlSetSpec("http://www.example.com/testDir");
    CachedUrlSet fileSet = mau.makeCachedUrlSet(sSpec);
    fileSet.storeActualHashDuration(123, null);
    assertEquals(-1, nodeMan.getNodeState(fileSet).getAverageHashDuration());

    // check that estimation isn't changed for ranged sets
    CachedUrlSetSpec rSpec =
        new RangeCachedUrlSetSpec("http://www.example.com/testDir", "ab", "yz");
    fileSet = mau.makeCachedUrlSet(rSpec);
    fileSet.storeActualHashDuration(123, null);
    assertEquals(-1, nodeMan.getNodeState(fileSet).getAverageHashDuration());

    // check that estimation isn't changed for exceptions
    rSpec = new RangeCachedUrlSetSpec("http://www.example.com/testDir");
    fileSet = mau.makeCachedUrlSet(rSpec);
    fileSet.storeActualHashDuration(123, new Exception("bad"));
    assertEquals(-1, nodeMan.getNodeState(fileSet).getAverageHashDuration());

    // check that estimation is grown for timeout exceptions
    rSpec = new RangeCachedUrlSetSpec("http://www.example.com/testDir");
    fileSet = mau.makeCachedUrlSet(rSpec);
    fileSet.storeActualHashDuration(100, null);
    assertEquals(100, nodeMan.getNodeState(fileSet).getAverageHashDuration());
    // simulate a timeout
    fileSet.storeActualHashDuration(200, new SchedService.Timeout("test"));
    assertEquals(300, nodeMan.getNodeState(fileSet).getAverageHashDuration());
    // and another,less than current estimate, shouldn't change it
    fileSet.storeActualHashDuration(100, new HashService.Timeout("test"));
    assertEquals(300, nodeMan.getNodeState(fileSet).getAverageHashDuration());
  }

  public void testCusCompare() throws Exception {
    CachedUrlSetSpec spec1 =
        new RangeCachedUrlSetSpec("http://www.example.com/test");
    CachedUrlSetSpec spec2 =
        new RangeCachedUrlSetSpec("http://www.example.com");
    MockCachedUrlSet cus1 = new MockCachedUrlSet(mau, spec1);
    MockCachedUrlSet cus2 = new MockCachedUrlSet(mau, spec2);
    assertEquals(CachedUrlSet.BELOW, cus1.cusCompare(cus2));

    spec1 = new RangeCachedUrlSetSpec("http://www.example.com/test");
    spec2 = new RangeCachedUrlSetSpec("http://www.example.com/test/subdir");
    cus1 = new MockCachedUrlSet(mau, spec1);
    cus2 = new MockCachedUrlSet(mau, spec2);
    assertEquals(CachedUrlSet.ABOVE, cus1.cusCompare(cus2));

    spec1 = new RangeCachedUrlSetSpec("http://www.example.com/test", "/a", "/b");
    spec2 = new RangeCachedUrlSetSpec("http://www.example.com/test", "/c", "/d");
    cus1 = new MockCachedUrlSet(mau, spec1);
    cus2 = new MockCachedUrlSet(mau, spec2);
    assertEquals(CachedUrlSet.SAME_LEVEL_NO_OVERLAP,
                 cus1.cusCompare(cus2));

    spec2 = new RangeCachedUrlSetSpec("http://www.example.com/test", "/b", "/d");
    cus2 = new MockCachedUrlSet(mau, spec2);
    assertEquals(CachedUrlSet.SAME_LEVEL_OVERLAP,
                 cus1.cusCompare(cus2));

    spec1 = new RangeCachedUrlSetSpec("http://www.example.com/test/subdir2");
    spec2 = new RangeCachedUrlSetSpec("http://www.example.com/subdir");
    cus1 = new MockCachedUrlSet(mau, spec1);
    cus2 = new MockCachedUrlSet(mau, spec2);
    assertEquals(CachedUrlSet.NO_RELATION, cus1.cusCompare(cus2));

    // test for single node specs
    spec1 = new SingleNodeCachedUrlSetSpec("http://www.example.com");
    spec2 = new RangeCachedUrlSetSpec("http://www.example.com/test");
    cus1 = new MockCachedUrlSet(mau, spec1);
    cus2 = new MockCachedUrlSet(mau, spec2);
    assertEquals(CachedUrlSet.SAME_LEVEL_NO_OVERLAP, cus1.cusCompare(cus2));
    // reverse
    assertEquals(CachedUrlSet.SAME_LEVEL_NO_OVERLAP, cus2.cusCompare(cus1));

    // test for Au urls
    spec1 = new AuCachedUrlSetSpec();
    spec2 = new AuCachedUrlSetSpec();
    cus1 = new MockCachedUrlSet(mau, spec1);
    cus2 = new MockCachedUrlSet(mau, spec2);
    assertEquals(CachedUrlSet.SAME_LEVEL_OVERLAP, cus1.cusCompare(cus2));

    spec2 = new RangeCachedUrlSetSpec("http://www.example.com");
    cus2 = new MockCachedUrlSet(mau, spec2);
    assertEquals(CachedUrlSet.ABOVE, cus1.cusCompare(cus2));
    // reverse
    assertEquals(CachedUrlSet.BELOW, cus2.cusCompare(cus1));

    // test for different AUs
    spec1 = new RangeCachedUrlSetSpec("http://www.example.com");
    spec2 = new RangeCachedUrlSetSpec("http://www.example.com");
    cus1 = new MockCachedUrlSet(mau, spec1);
    cus2 = new MockCachedUrlSet(new MockArchivalUnit(), spec2);
    assertEquals(CachedUrlSet.NO_RELATION, cus1.cusCompare(cus2));

    // test for exclusive ranges
    spec1 = new RangeCachedUrlSetSpec("http://www.example.com", "/abc", "/xyz");
    spec2 = new RangeCachedUrlSetSpec("http://www.example.com/test");
    cus1 = new MockCachedUrlSet(mau, spec1);
    cus2 = new MockCachedUrlSet(mau, spec2);
    // this range is inclusive, so should be parent
    assertEquals(CachedUrlSet.ABOVE, cus1.cusCompare(cus2));
    assertEquals(CachedUrlSet.BELOW, cus2.cusCompare(cus1));
    spec1 = new RangeCachedUrlSetSpec("http://www.example.com", "/abc", "/mno");
    cus1 = new MockCachedUrlSet(mau, spec1);
    // this range is exclusive, so should be no relation
    assertEquals(CachedUrlSet.NO_RELATION, cus1.cusCompare(cus2));
    // reverse
    assertEquals(CachedUrlSet.NO_RELATION, cus2.cusCompare(cus1));
  }


  private RepositoryNode createLeaf(String url, String content,
                                    Properties props) throws Exception {
    return TestRepositoryNodeImpl.createLeaf(repo, url, content, props);
  }


  private class MyMockArchivalUnit extends MockArchivalUnit {

    public CachedUrlSet makeCachedUrlSet(CachedUrlSetSpec cuss) {
      return new BaseCachedUrlSet(this, cuss);
    }

    public CachedUrl makeCachedUrl(String url) {
      return new BaseCachedUrl(this, url);
    }

    public UrlCacher makeUrlCacher(UrlData ud) {
      return new DefaultUrlCacher(this, ud);
    }

    public FilterRule getFilterRule(String mimeType) {
      return null;
    }

    public boolean shouldBeCached(String url) {
      return true;
    }
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestBaseCachedUrlSet.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
