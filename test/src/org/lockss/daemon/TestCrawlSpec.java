/*
 * $Id: TestCrawlSpec.java,v 1.6 2003-10-10 19:21:44 eaalto Exp $
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
import gnu.regexp.REException;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * This is the test class for org.lockss.daemon.CrawlSpec
 */

public class TestCrawlSpec extends LockssTestCase {
  public TestCrawlSpec(String msg){
    super(msg);
  }

  public void testEmptySpec() throws REException {
    try {
      CrawlSpec cs = new CrawlSpec((String)null, null);
      fail("CrawlSpec with null starting point should throw");
    } catch (NullPointerException e) { }
    try {
      CrawlSpec cs = new CrawlSpec((List)null, null);
      fail("CrawlSpec with null starting point should throw");
    } catch (NullPointerException e) { }
    try {
      CrawlSpec cs = new CrawlSpec(Collections.EMPTY_LIST, null);
      fail("CrawlSpec with null starting point should throw");
    } catch (IllegalArgumentException e) { }
    String foo[] = {"foo"};
    CrawlSpec cs1 = new CrawlSpec(foo[0], null);
    assertIsomorphic(foo, cs1.getStartingUrls());
    CrawlSpec cs2 = new CrawlSpec(ListUtil.fromArray(foo), null);
    assertIsomorphic(foo, cs2.getStartingUrls());
    String foobar[] = {"foo", "bar"};
    CrawlSpec cs3 = new CrawlSpec(ListUtil.fromArray(foobar), null);
    assertIsomorphic(foobar, cs3.getStartingUrls());
  }

  public void testNoModify() {
    List l1 = ListUtil.list("one", "two");
    CrawlSpec cs = new CrawlSpec(l1, null);
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

  public void testNullRule() throws REException {
    CrawlSpec cs1 = new CrawlSpec("foo", null);
    assertTrue(cs1.isIncluded(null));
    assertTrue(cs1.isIncluded("foo"));
    assertTrue(cs1.isIncluded("bar"));
  }

  public void testIncluded() throws REException {
    CrawlSpec cs1 =
      new CrawlSpec("foo",
                    new CrawlRules.RE("foo[12]*", CrawlRules.RE.MATCH_INCLUDE));
    try {
      assertFalse(cs1.isIncluded(null));
      fail("CrawlSpec.inIncluded(null) should throw");
    } catch (NullPointerException e) { }
    assertTrue(cs1.isIncluded("foo"));
    assertTrue(cs1.isIncluded("foo22"));
    assertFalse(cs1.isIncluded("bar"));
  }

  public void testCrawlWindow() throws REException {
    MyMockCrawlWindowRule window = new MyMockCrawlWindowRule();
    CrawlSpec cs1 =
      new CrawlSpec("foo",
                    new CrawlRules.RE("foo[12]*", CrawlRules.RE.MATCH_INCLUDE));
    cs1.setCrawlWindowRule(window);
    assertTrue(cs1.canCrawl());
    window.setAllowCrawl(false);
    assertFalse(cs1.canCrawl());
  }

  public void testThrowsIfRecrawlDepthLessThanOne() {
    try {
      CrawlSpec cs = new CrawlSpec("blah", null, 0);
      fail("Trying to construct a CrawlSpec with a RecrawlDepth less "
	   +"than 1 should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  public static class MyMockCrawlWindowRule implements CrawlWindowRule {
    boolean allowCrawl = true;

    public MyMockCrawlWindowRule() { }

    public int canCrawl() {
      if (allowCrawl) {
        return INCLUDE;
      } else {
        return EXCLUDE;
      }
    }

    public int canCrawl(Date serverDate) {
      return canCrawl();
    }

    public void setAllowCrawl(boolean allowCrawl) {
      this.allowCrawl = allowCrawl;
    }
  }

}

