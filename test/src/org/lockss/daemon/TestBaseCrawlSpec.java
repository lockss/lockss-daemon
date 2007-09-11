/*
 * $Id: TestBaseCrawlSpec.java,v 1.6.10.1 2007-09-11 19:14:59 dshr Exp $
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

public class TestBaseCrawlSpec extends LockssTestCase {

  private CrawlRule rule = new MockCrawlRule();

  public TestBaseCrawlSpec(String msg){
    super(msg);
  }

  public void testNullPermissionUrls() throws LockssRegexpException {
    try {
      BaseCrawlSpec cs =
	new TestableBaseCrawlSpec((List) null, rule, null, null);
      fail("BaseCrawlSpec with null permission url should throw");
    } catch (NullPointerException e) { }
    try {
      BaseCrawlSpec cs = new TestableBaseCrawlSpec(Collections.EMPTY_LIST,
						   rule , null, null);
      fail("BaseCrawlSpec with null permission url should throw");
    } catch (IllegalArgumentException e) { }
  }

  public void testGetPermissionPages() throws LockssRegexpException {
    String foo[] = {"foo"};
    BaseCrawlSpec cs1 =
      new TestableBaseCrawlSpec(ListUtil.list(foo[0]), rule, null, null);
    assertIsomorphic(foo, cs1.getPermissionPages());
    BaseCrawlSpec cs2 =
      new TestableBaseCrawlSpec(ListUtil.fromArray(foo), rule, null, null);
    assertIsomorphic(foo, cs2.getPermissionPages());
    String foobar[] = {"foo", "bar"};
    BaseCrawlSpec cs3 =
      new TestableBaseCrawlSpec(ListUtil.fromArray(foobar), rule, null, null);
    assertIsomorphic(foobar, cs3.getPermissionPages());
  }

  public void testGetPermissionCheckers() throws LockssRegexpException {
    List foo = ListUtil.list("foo");
//    String checkers[] = {"one"};
    PermissionChecker permissionChecker = new MockPermissionChecker(99);
    BaseCrawlSpec cs1 =
      new TestableBaseCrawlSpec(foo, rule, permissionChecker, null);
//    assertIsomorphic(checkers, cs1.getPermissionChecker());
    assertSame(permissionChecker, cs1.getPermissionChecker());
//    BaseCrawlSpec cs2 =
//      new TestableBaseCrawlSpec(foo, rule, ListUtil.fromArray(checkers), null);
//    assertIsomorphic(checkers, cs2.getPermissionChecker());
//    String otherCheckers[] = {"one", "two"};
//    BaseCrawlSpec cs3 =
//      new TestableBaseCrawlSpec(foo, rule,
//				ListUtil.fromArray(otherCheckers), null);
//    assertIsomorphic(otherCheckers, cs3.getPermissionChecker());
  }

  public void testNoModifyPermissionList() {
    List l1 = ListUtil.list("one", "two");
    BaseCrawlSpec cs = new TestableBaseCrawlSpec(l1, rule, null, null);
    List l2 = cs.getPermissionPages();
    assertEquals(l1, l2);
    try {
      l2.add("foo");
      fail("Shouldn't be able to modify list returned by getPermissionPages()");
    } catch (UnsupportedOperationException e) { }
    l1.add("bar");
    assertEquals("Modifying passed-in list modified getPermissionPages()",
		 l2, cs.getPermissionPages());
  }

//   public void testNullCrawlRule() throws LockssRegexpException {
//     try {
//       BaseCrawlSpec cs = new TestableBaseCrawlSpec(ListUtil.list("foo"), null, null);
//       fail("BaseCrawlSpec with null crawl rule should throw");
//     } catch (IllegalArgumentException e) { }
//     BaseCrawlSpec cs1 = new TestableBaseCrawlSpec(ListUtil.list("foo"), rule, null);
//     assertTrue(cs1.isIncluded(null));
//     assertTrue(cs1.isIncluded("foo"));
//     assertTrue(cs1.isIncluded("bar"));
//   }

  public void testIncluded() throws LockssRegexpException {
    BaseCrawlSpec cs1 =
      new TestableBaseCrawlSpec(ListUtil.list("foo"),
				new CrawlRules.RE("foo[12]*",
						  CrawlRules.RE.MATCH_INCLUDE),
				null, null);
    try {
      assertFalse(cs1.isIncluded(null));
      fail("CrawlSpec.inIncluded(null) should throw");
    } catch (NullPointerException e) { }
    assertTrue(cs1.isIncluded("foo"));
    assertTrue(cs1.isIncluded("foo22"));
    assertFalse(cs1.isIncluded("bar"));
  }

  public void testCrawlWindow() throws LockssRegexpException {
    MockCrawlWindow window = new MockCrawlWindow();
    BaseCrawlSpec cs1 =
      new TestableBaseCrawlSpec(ListUtil.list("foo"),
				new CrawlRules.RE("foo[12]*",
						  CrawlRules.RE.MATCH_INCLUDE),
				null, null);
    cs1.setCrawlWindow(window);
    assertTrue(cs1.inCrawlWindow());
    window.setAllowCrawl(false);
    assertFalse(cs1.inCrawlWindow());
  }

  public void testArcFilePattern() {
    List l1 = ListUtil.list("one", "two");
    BaseCrawlSpec cs = new TestableBaseCrawlSpec(l1, rule, null, null);
    assertNull(cs.getExploderPattern());
  }
    
  private static class TestableBaseCrawlSpec extends BaseCrawlSpec {
    protected TestableBaseCrawlSpec(List permissionUrls, CrawlRule rule,
				    PermissionChecker permissionChecker,
				    LoginPageChecker loginPageChecker)
	throws ClassCastException {
      super(permissionUrls, rule, permissionChecker, loginPageChecker);
    }
  }

}

