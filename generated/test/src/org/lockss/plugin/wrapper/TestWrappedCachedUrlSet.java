/*
 * $Id: TestWrappedCachedUrlSet.java,v 1.2 2004-01-27 00:41:49 tyronen Exp $
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

package org.lockss.plugin.wrapper;

import java.util.*;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.plugin.*;

/**
 * This is the test class for
 * org.lockss.plugin.wrapper.WrappedCachedUrlSet.
 *
 * Most code from TestGenericFileCachedUrlSet
 *
 * @author  Tyrone Nicholas
 */
public class TestWrappedCachedUrlSet extends LockssTestCase {
  private MockCachedUrlSet mset;
  private WrappedCachedUrlSet wset;
  private final String SOURCE = "Url source";

  public void setUp() throws Exception {
    super.setUp();
    MockArchivalUnit mau = new MockArchivalUnit();
    WrappedArchivalUnit wau = (WrappedArchivalUnit)WrapperState.getWrapper(mau);
    WrappedPlugin plugin = (WrappedPlugin)WrapperState.getWrapper(
        new MockPlugin());
    mau.setPlugin(((MockPlugin)plugin.getOriginal()));
    wset = (WrappedCachedUrlSet)wau.getAuCachedUrlSet();
    mset = (MockCachedUrlSet)wset.getOriginal();
    assertSame(wau,wset.getArchivalUnit());
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testSpec() {
    CachedUrlSetSpec rSpec =
        new RangeCachedUrlSetSpec("http://www.example.com/testDir");
    mset.setSpec(rSpec);
    assertSame(wset.getSpec(),rSpec);
  }

  public void testHashers() {
    MockCachedUrlSetHasher name = new MockCachedUrlSetHasher();
    MockCachedUrlSetHasher content = new MockCachedUrlSetHasher();
    mset.setNameHasher(name);
    mset.setContentHasher(content);
    MockMessageDigest digest = new MockMessageDigest();
    assertSame(wset.getNameHasher(digest), name);
    assertSame(wset.getContentHasher(digest), content);
    assertEquals(wset.hashCode(),mset.hashCode());
  }

  public void testEquals() {
  MockCachedUrlSet second = new MockCachedUrlSet();
  CachedUrlSetSpec rSpec =
      new RangeCachedUrlSetSpec("http://www.example.com/testDir");
  mset.setSpec(rSpec);
  second.setSpec(rSpec);
  WrappedCachedUrlSet wsecond = (WrappedCachedUrlSet)WrapperState.getWrapper(second);
  assertEquals(wset,wsecond);
  }

  Set makeUrlSet() {
    Set urls = new HashSet();
    urls.add(new MockCachedUrl("http://www.example.com/testDir/leaf4", mset));
    urls.add(new MockCachedUrl("http://www.example.com/testDir/branch1/leaf1",
                               mset));
    urls.add(new MockCachedUrl("http://www.example.com/testDir/branch2/leaf3",
                               mset));
    urls.add(new MockCachedUrl("http://www.example.com/testDir/branch1/leaf2",
                               mset));
    return urls;
  }

  void checkIterator(Iterator setIt) {
    ArrayList childL = new ArrayList(3);
    while (setIt.hasNext()) {
      childL.add( ( (CachedUrlSetNode) setIt.next()).getUrl());
    }
    // should be sorted
    String[] expectedA = new String[] {
        "http://www.example.com/testDir/branch1/leaf1",
        "http://www.example.com/testDir/branch1/leaf2",
        "http://www.example.com/testDir/branch2/leaf3",
        "http://www.example.com/testDir/leaf4"
    };
    assertContainsAll(childL, expectedA);

  }

  public void testFlatSetIterator() throws Exception {
    Set urls = makeUrlSet();
    mset.setFlatItSource(urls);
    Iterator setIt = wset.flatSetIterator();
    checkIterator(setIt);
  }

  public void testHashIterator() throws Exception {
    Set urls = makeUrlSet();
    mset.setHashItSource(urls);
    Iterator setIt = wset.contentHashIterator();
    checkIterator(setIt);
  }

  public void testDuration() throws Exception {
    mset.setEstimatedHashDuration(3);
    assertEquals(wset.estimatedHashDuration(), 3);
    wset.storeActualHashDuration(4,new Exception());
    assertEquals(mset.getActualHashDuration(),4);
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestWrappedCachedUrlSet.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
