/*
 * $Id: TestSpiderCrawlSpec.java,v 1.3 2007-03-17 21:31:31 dshr Exp $
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

package org.lockss.daemon;

import java.util.*;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * This is the test class for org.lockss.daemon.CrawlSpec
 */

public class TestSpiderCrawlSpec extends LockssTestCase {

  private CrawlRule rule = new MockCrawlRule();

  public TestSpiderCrawlSpec(String msg){
    super(msg);
  }

  public void testNullStaringUrls() throws LockssRegexpException {
    try {
      SpiderCrawlSpec cs = new SpiderCrawlSpec((String)null, rule);
      fail("SpiderCrawlSpec with null starting point should throw");
    } catch (NullPointerException e) { }
    try {
      SpiderCrawlSpec cs = new SpiderCrawlSpec((List)null, rule);
      fail("SpiderCrawlSpec with null starting point should throw");
    } catch (NullPointerException e) { }
    try {
      SpiderCrawlSpec cs = new SpiderCrawlSpec(Collections.EMPTY_LIST, rule);
      fail("SpiderCrawlSpec with null starting point should throw");
    } catch (IllegalArgumentException e) { }
    String foo[] = {"foo"};
    SpiderCrawlSpec cs1 = new SpiderCrawlSpec(foo[0], rule);
    assertIsomorphic(foo, cs1.getStartingUrls());
    SpiderCrawlSpec cs2 = new SpiderCrawlSpec(ListUtil.fromArray(foo), rule);
    assertIsomorphic(foo, cs2.getStartingUrls());
    String foobar[] = {"foo", "bar"};
    SpiderCrawlSpec cs3 = new SpiderCrawlSpec(ListUtil.fromArray(foobar), rule);
    assertIsomorphic(foobar, cs3.getStartingUrls());
  }

  public void testNoModify() {
    List l1 = ListUtil.list("one", "two");
    SpiderCrawlSpec cs = new SpiderCrawlSpec(l1, rule);
    List l2 = cs.getStartingUrls();
    assertEquals(l1, l2);
    try {
      l2.add("foo");
      fail("Shouldn't be able to modify list returned by getStartingUrls()");
    } catch (UnsupportedOperationException e) { }
    l1.add("bar");
    assertEquals("Modifying passed-in list modified getStartingUrls()",
		 l2, cs.getStartingUrls());
  }

  public void testThrowsIfRecrawlDepthLessThanOne() {
    try {
      SpiderCrawlSpec cs = new SpiderCrawlSpec("blah", rule, 0);
      fail("Trying to construct a CrawlSpec with a RecrawlDepth less "
	   +"than 1 should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testRecrawlDepthDefaultsTo1() {
    SpiderCrawlSpec cs = new SpiderCrawlSpec("blah", rule);
    assertEquals(1, cs.getRefetchDepth());
  }

  public void testArcFilePattern() {
    List l1 = ListUtil.list("one", "two");
    List l2 = ListUtil.list("three", "four");
    {
      SpiderCrawlSpec cs = new SpiderCrawlSpec(l1, l2, rule, 1, null, null);
      assertNull(cs.arcFilePattern());
    }
    {
      String pattern = "pattern";
      SpiderCrawlSpec cs = new SpiderCrawlSpec(l1, l2, rule, 2, null, null,
					       pattern);
      assertEquals(cs.arcFilePattern(), pattern);
    }
  }
    
}

