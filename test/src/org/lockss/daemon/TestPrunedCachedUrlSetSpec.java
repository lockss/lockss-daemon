/*
 * $Id$
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
import java.util.regex.*;
import junit.framework.TestCase;

import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestPrunedCachedUrlSetSpec extends LockssTestCase {

  public void assertEquals(Object... values) {
    assertEquals((String)null, values);
  }

  public void assertEquals(String msg, Object... values) {
    for (int ix = 0; ix < values.length; ix++) {
      for (int iy = 0; iy < values.length; iy++) {
	assertEquals((msg != null ? msg : "") + "[" + ix + "," + iy + "]",
		     values[ix], values[iy]);
      }
    }
  }

  public void testIll() {
    try {
      PrunedCachedUrlSetSpec.includeMatchingSubTrees(null, "pat");
      fail("PrunedCachedUrlSetSpec with null url should throw");
    } catch (NullPointerException e) {
    }
    try {
      PrunedCachedUrlSetSpec.excludeMatchingSubTrees(null, "pat");
      fail("PrunedCachedUrlSetSpec with null url should throw");
    } catch (NullPointerException e) {
    }
  }

  public void testEquals() {
    CachedUrlSetSpec cin11 =
      PrunedCachedUrlSetSpec.includeMatchingSubTrees("pref1", "pat1");
    CachedUrlSetSpec cin110 =
      PrunedCachedUrlSetSpec.includeMatchingSubTrees("pref1", "pat1", 0);
    CachedUrlSetSpec cin11c =
      PrunedCachedUrlSetSpec.includeMatchingSubTrees("pref1", "pat1",
						     Pattern.CASE_INSENSITIVE);
    CachedUrlSetSpec cin1p1 =
      PrunedCachedUrlSetSpec.includeMatchingSubTrees("pref1",
						     Pattern.compile("pat1"));
    CachedUrlSetSpec cin1p10 =
      PrunedCachedUrlSetSpec.includeMatchingSubTrees("pref1",
						     Pattern.compile("pat1",
								     0));
    CachedUrlSetSpec cin1p1c =
      PrunedCachedUrlSetSpec.includeMatchingSubTrees("pref1",
						     Pattern.compile("pat1",
								     Pattern.CASE_INSENSITIVE));

    assertEquals(cin11, cin1p1);
    assertEquals(cin11, cin110, cin1p1, cin1p10);
    assertNotEquals(cin110, cin11c);
    assertEquals(cin11, cin1p1);
    assertEquals(cin11c, cin1p1c);

    CachedUrlSetSpec cin12 =
      PrunedCachedUrlSetSpec.includeMatchingSubTrees("pref1", "pat2");
    CachedUrlSetSpec cin21 =
      PrunedCachedUrlSetSpec.includeMatchingSubTrees("pref2", "pat1");

    assertNotEquals(cin11, cin12);
    assertNotEquals(cin11, cin21);

    CachedUrlSetSpec cex11 =
      PrunedCachedUrlSetSpec.excludeMatchingSubTrees("pref1", "pat1");
    CachedUrlSetSpec cex110 =
      PrunedCachedUrlSetSpec.excludeMatchingSubTrees("pref1", "pat1", 0);
    CachedUrlSetSpec cex11c =
      PrunedCachedUrlSetSpec.excludeMatchingSubTrees("pref1", "pat1",
						     Pattern.CASE_INSENSITIVE);
    CachedUrlSetSpec cex1p1 =
      PrunedCachedUrlSetSpec.excludeMatchingSubTrees("pref1",
						     Pattern.compile("pat1"));
    CachedUrlSetSpec cex1p10 =
      PrunedCachedUrlSetSpec.excludeMatchingSubTrees("pref1",
						     Pattern.compile("pat1",
								     0));
    CachedUrlSetSpec cex1p1c =
      PrunedCachedUrlSetSpec.excludeMatchingSubTrees("pref1",
						     Pattern.compile("pat1",
								     Pattern.CASE_INSENSITIVE));

    assertEquals(cex11, cex1p1);
    assertEquals(cex11, cex110, cex1p1, cex1p10);
    assertNotEquals(cex110, cex11c);
    assertEquals(cex11, cex1p1);
    assertEquals(cex11c, cex1p1c);

    CachedUrlSetSpec cex12 =
      PrunedCachedUrlSetSpec.excludeMatchingSubTrees("pref1", "pat2");
    CachedUrlSetSpec cex21 =
      PrunedCachedUrlSetSpec.excludeMatchingSubTrees("pref2", "pat1");

    assertNotEquals(cex11, cex12);
    assertNotEquals(cex11, cex21);

    assertNotEquals(cin11, cex11);
    assertNotEquals(cin1p1c, cex1p1c);

    assertNotEquals(cin11, new AuCachedUrlSetSpec());
    assertNotEquals(cin11, new RangeCachedUrlSetSpec("pref1"));
    assertNotEquals(cin11, new RangeCachedUrlSetSpec("pref1", "pat1", null));
    assertNotEquals(cin11, new SingleNodeCachedUrlSetSpec("pref1"));
  }

  public void assertIncls(PrunedCachedUrlSetSpec cuss) {
    assertFalse(cuss.matches("http://xfoo/bar/abc"));
    assertTrue(cuss.matches("http://foo/bar/abc"));
    assertTrue(cuss.matches("http://foo/bar/abcd"));
    assertTrue(cuss.matches("http://foo/bar/abc/"));
    assertTrue(cuss.matches("http://foo/bar/abc/xx"));
    assertTrue(cuss.matches("http://foo/bar/"));
    assertFalse(cuss.matches("http://foo/bar/def"));
  }

  public void testMatchIncludeSubTree() {
    assertIncls(PrunedCachedUrlSetSpec
		.includeMatchingSubTrees("http://foo/bar/",
					 "http://foo/bar/abc"));
    assertIncls(PrunedCachedUrlSetSpec
		.includeMatchingSubTrees(AuCachedUrlSetSpec.URL,
					 "http://foo/bar/abc"));
  }

  public void assertExcls(PrunedCachedUrlSetSpec cuss) {
    assertTrue(cuss.matches("http://foo/bar/"));
    assertTrue(cuss.matches("http://foo/bar/abd"));
    assertFalse(cuss.matches("http://foo/bar/abc"));
    assertFalse(cuss.matches("http://foo/bar/abc/"));
    assertFalse(cuss.matches("http://foo/bar/abc/xx"));
  }

  public void testMatchExcludeSubTree() {
    PrunedCachedUrlSetSpec cuss1 =
      PrunedCachedUrlSetSpec .excludeMatchingSubTrees("http://foo/bar/",
						      "http://foo/bar/abc");
    assertFalse(cuss1.matches("http://xfoo/bar/"));
    assertExcls(cuss1);

    assertExcls(PrunedCachedUrlSetSpec
		.excludeMatchingSubTrees(AuCachedUrlSetSpec.URL,
					 "http://foo/bar/abc"));
  }

  public void testDisjoint() {
    PrunedCachedUrlSetSpec cuss1 =
      PrunedCachedUrlSetSpec.includeMatchingSubTrees("http://foo/bar/",
						     "http://foo/bar/abc");
    PrunedCachedUrlSetSpec cuss2 =
      PrunedCachedUrlSetSpec.includeMatchingSubTrees("http://foo/aaa/",
						     "http://foo/aaa/abc");
    try {
      cuss1.isDisjoint(cuss2);
      fail("PrunedCachedUrlSetSpec.disjoint() should throw");
    } catch (UnsupportedOperationException e) {
    }
  }

  public void testSubsumes() {
    PrunedCachedUrlSetSpec cuss1 =
      PrunedCachedUrlSetSpec.includeMatchingSubTrees("http://foo/bar/",
						     "http://foo/bar/abc");
    PrunedCachedUrlSetSpec cuss2 =
      PrunedCachedUrlSetSpec.includeMatchingSubTrees("http://foo/aaa/",
						     "http://foo/aaa/abc");
    try {
      cuss1.subsumes(cuss2);
      fail("PrunedCachedUrlSetSpec.subsumes() should throw");
    } catch (UnsupportedOperationException e) {
    }
  }

}
