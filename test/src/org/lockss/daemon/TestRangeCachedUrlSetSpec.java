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

package org.lockss.daemon;
import java.util.*;
import junit.framework.TestCase;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * This is the test class for org.lockss.daemon.RangeCachedUrlSetSpec
 */

public class TestRangeCachedUrlSetSpec extends LockssTestCase {

  public void testIll() {
    try {
      new RangeCachedUrlSetSpec(null);
      fail("RangeCachedUrlSetSpec with null url should throw");
    } catch (NullPointerException e) {
    }
    try {
      new RangeCachedUrlSetSpec(null, "foo", "bar");
      fail("RangeCachedUrlSetSpec with null url should throw");
    } catch (NullPointerException e) {
    }
    try {
      new RangeCachedUrlSetSpec("a", "34", "12");
      fail("RangeCachedUrlSetSpec with null url should throw");
    } catch (IllegalArgumentException e) {
    }
    try {
      new RangeCachedUrlSetSpec("b", "34", "");
      fail("RangeCachedUrlSetSpec with null url should throw");
    } catch (IllegalArgumentException e) {
    }
    // Ok if lower == upper
      new RangeCachedUrlSetSpec("b", "34", "34");
  }

  public void testGets() {
    RangeCachedUrlSetSpec cuss1 = new RangeCachedUrlSetSpec("1/2/3");
    RangeCachedUrlSetSpec cuss2 = new RangeCachedUrlSetSpec("a/", "b", "d");

    assertEquals("1/2/3", cuss1.getUrl());
    assertEquals(null, cuss1.getLowerBound());
    assertEquals(null, cuss1.getUpperBound());

    assertEquals("a/", cuss2.getUrl());
    assertEquals("b", cuss2.getLowerBound());
    assertEquals("d", cuss2.getUpperBound());
  }

  public void testEquals() {
    CachedUrlSetSpec cuss1 = new RangeCachedUrlSetSpec("foo", null, null);
    CachedUrlSetSpec cuss2 = new RangeCachedUrlSetSpec("foo");
    CachedUrlSetSpec cuss3 = new RangeCachedUrlSetSpec("foo", "a", null);
    CachedUrlSetSpec cuss4 = new RangeCachedUrlSetSpec("foo", "a", null);
    CachedUrlSetSpec cuss5 = new RangeCachedUrlSetSpec("foo", "a", "b");
    CachedUrlSetSpec cuss6 = new RangeCachedUrlSetSpec("foo", "a", "b");
    CachedUrlSetSpec cuss7 = new RangeCachedUrlSetSpec("foo", null, "b");
    CachedUrlSetSpec cuss8 = new RangeCachedUrlSetSpec("foo", null, "b");
    CachedUrlSetSpec cuss9 = new RangeCachedUrlSetSpec("foo", "a", "a");
    CachedUrlSetSpec cuss10 = new RangeCachedUrlSetSpec("foo", "", "a");
    CachedUrlSetSpec cuss11 = new RangeCachedUrlSetSpec("bar");
    assertEquals(cuss1, cuss2);
    assertNotEquals(cuss1, cuss3);
    assertNotEquals(cuss1, cuss11);
    assertEquals(cuss3, cuss4);
    assertNotEquals(cuss3, cuss5);
    assertNotEquals(cuss3, cuss7);
    assertNotEquals(cuss5, cuss7);
    assertEquals(cuss5, cuss6);
    assertEquals(cuss7, cuss8);
    assertNotEquals(cuss5, cuss9);
    assertNotEquals(cuss9, cuss10);

    assertNotEquals(cuss1, new AuCachedUrlSetSpec());
    assertNotEquals(cuss1, new SingleNodeCachedUrlSetSpec("foo"));
  }

  public void testHashCode() throws Exception {
    CachedUrlSetSpec cuss1 = new RangeCachedUrlSetSpec("foo", "/abc", "/xyz");
    CachedUrlSetSpec cuss2 = new RangeCachedUrlSetSpec("foo", "/abc", "/xyz");
    assertTrue(cuss1.hashCode() == cuss2.hashCode());
  }

  public void testTypePredicates() {
    CachedUrlSetSpec cuss1 = new RangeCachedUrlSetSpec("foo");
    assertFalse(cuss1.isSingleNode());
    assertFalse(cuss1.isAu());
    assertFalse(cuss1.isRangeRestricted());

    CachedUrlSetSpec cuss2 = new RangeCachedUrlSetSpec("foo", "a", null);
    assertFalse(cuss2.isSingleNode());
    assertFalse(cuss2.isAu());
    assertTrue(cuss2.isRangeRestricted());

    CachedUrlSetSpec cuss3 = new RangeCachedUrlSetSpec("foo", null, "b");
    assertFalse(cuss3.isSingleNode());
    assertFalse(cuss3.isAu());
    assertTrue(cuss3.isRangeRestricted());

    CachedUrlSetSpec cuss4 = new RangeCachedUrlSetSpec("foo", "a", "b");
    assertFalse(cuss4.isSingleNode());
    assertFalse(cuss4.isAu());
    assertTrue(cuss4.isRangeRestricted());
  }

  public void testMatchNoRange() {
    RangeCachedUrlSetSpec cuss1 = new RangeCachedUrlSetSpec("foo");
    assertTrue(cuss1.matches("foo"));// not ranged, should match prefix
    assertFalse(cuss1.matches("foobar")); // no path separator
    assertTrue(cuss1.matches("foo/bar"));
    assertTrue(cuss1.matches("foo!/bar"));
    assertTrue(cuss1.matches("foo/"));
    assertTrue(cuss1.matches("foo!/"));
    assertFalse(cuss1.matches("fo"));
    assertFalse(cuss1.matches("1foo"));

    cuss1 = new RangeCachedUrlSetSpec("foo/");
    assertFalse(cuss1.matches("foo"));// not ranged, should match prefix
    assertFalse(cuss1.matches("foobar"));
    assertTrue(cuss1.matches("foo/bar")); // path separator in prefix
  }

  public void testMatchLower() {
    RangeCachedUrlSetSpec cuss2 =
      new RangeCachedUrlSetSpec("foo", "/123", null);
    assertTrue(cuss2.matches("foo/123"));
    assertTrue(cuss2.matches("foo/123/x"));
    assertTrue(cuss2.matches("foo/123.x"));
    assertFalse(cuss2.matches("foo"));
    assertFalse(cuss2.matches("foo/12"));
    assertFalse(cuss2.matches("foo/122"));
    assertFalse(cuss2.matches("foo/0"));
    assertFalse(cuss2.matches("/123foo"));
  }

  public void testMatchUpper() {
    RangeCachedUrlSetSpec cuss3 =
      new RangeCachedUrlSetSpec("bar/", null, "123");
    assertTrue(cuss3.matches("bar/0"));
    assertTrue(cuss3.matches("bar/123"));
    assertFalse(cuss3.matches("foo/123."));
    assertFalse(cuss3.matches("foo/123/"));
    assertFalse(cuss3.matches("bar/"));  // ranged, shouldn't match prefix
    assertFalse(cuss3.matches("bar/123/4"));
    assertFalse(cuss3.matches("bar/124"));
    assertFalse(cuss3.matches("bar"));
    assertFalse(cuss3.matches("foo"));
  }

  public void testMatchBoth() {
    RangeCachedUrlSetSpec cuss4 =
      new RangeCachedUrlSetSpec("bar/", "222", "555");
    assertFalse(cuss4.matches("bar/"));
    assertFalse(cuss4.matches("bar/0"));
    assertTrue(cuss4.matches("bar/222"));
    assertTrue(cuss4.matches("bar/223"));
    assertTrue(cuss4.matches("bar/3333"));
    assertTrue(cuss4.matches("bar/555"));
    assertTrue(cuss4.matches("bar/24"));
    assertFalse(cuss4.matches("bar/556"));
    assertFalse(cuss4.matches("bar/5555"));
    assertFalse(cuss4.matches("bar/22"));
  }

  public void testDisjoint() {
    CachedUrlSetSpec cuss1 = new RangeCachedUrlSetSpec("a/b/");
    CachedUrlSetSpec cuss2 = new RangeCachedUrlSetSpec("a/b/", "c", null);
    CachedUrlSetSpec cuss3 = new RangeCachedUrlSetSpec("a/b/", null, "d");
    CachedUrlSetSpec cuss4 = new RangeCachedUrlSetSpec("a/b/", "c", "d");

    // RCUSS is never disjoint from AUCUSS
    assertFalse(cuss1.isDisjoint(new AuCachedUrlSetSpec()));
    assertFalse(cuss2.isDisjoint(new AuCachedUrlSetSpec()));
    assertFalse(cuss3.isDisjoint(new AuCachedUrlSetSpec()));
    assertFalse(cuss4.isDisjoint(new AuCachedUrlSetSpec()));

    // SNCUSS above is disjoint
    assertTrue(cuss1.isDisjoint(new SingleNodeCachedUrlSetSpec("a")));
    assertTrue(cuss2.isDisjoint(new SingleNodeCachedUrlSetSpec("a")));
    assertTrue(cuss3.isDisjoint(new SingleNodeCachedUrlSetSpec("a")));
    assertTrue(cuss4.isDisjoint(new SingleNodeCachedUrlSetSpec("a")));

    assertTrue(cuss1.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b")));
    assertTrue(cuss2.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b")));
    assertTrue(cuss3.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b")));
    assertTrue(cuss4.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b")));

    // SNCUSS at same node is disjoint only if RCUSS is range-restricted
    assertFalse(cuss1.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b/")));
    assertTrue(cuss2.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b/")));
    assertTrue(cuss3.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b/")));
    assertTrue(cuss4.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b/")));

    // sibling SNCUSS is disjoint
    assertTrue(cuss1.isDisjoint(new SingleNodeCachedUrlSetSpec("a/c")));
    assertTrue(cuss2.isDisjoint(new SingleNodeCachedUrlSetSpec("a/c")));
    assertTrue(cuss3.isDisjoint(new SingleNodeCachedUrlSetSpec("a/c")));
    assertTrue(cuss4.isDisjoint(new SingleNodeCachedUrlSetSpec("a/c")));

    // child SNCUSS is not disjoint
    assertFalse(cuss1.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b/c")));
    assertFalse(cuss2.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b/c")));
    assertFalse(cuss3.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b/c")));
    assertFalse(cuss4.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b/c")));

    // child SNCUSS outside range is disjoint
    assertTrue(cuss2.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b/a")));
    assertTrue(cuss3.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b/e")));
    assertTrue(cuss4.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b/a")));
    assertTrue(cuss4.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b/e")));

    // not disjoint with self, nor with these overlapping ranges
    assertFalse(cuss1.isDisjoint(cuss1));
    assertFalse(cuss1.isDisjoint(cuss2));
    assertFalse(cuss1.isDisjoint(cuss3));
    assertFalse(cuss1.isDisjoint(cuss4));
    assertFalse(cuss2.isDisjoint(cuss1));
    assertFalse(cuss2.isDisjoint(cuss2));
    assertFalse(cuss2.isDisjoint(cuss3));
    assertFalse(cuss2.isDisjoint(cuss4));
    assertFalse(cuss3.isDisjoint(cuss1));
    assertFalse(cuss3.isDisjoint(cuss2));
    assertFalse(cuss3.isDisjoint(cuss3));
    assertFalse(cuss3.isDisjoint(cuss4));
    assertFalse(cuss4.isDisjoint(cuss1));
    assertFalse(cuss4.isDisjoint(cuss2));
    assertFalse(cuss4.isDisjoint(cuss3));
    assertFalse(cuss4.isDisjoint(cuss4));

    // not disjoint with unrestricted parent RCUSS
    assertFalse(cuss1.isDisjoint(new RangeCachedUrlSetSpec("a/")));
    assertFalse(cuss2.isDisjoint(new RangeCachedUrlSetSpec("a/")));
    assertFalse(cuss3.isDisjoint(new RangeCachedUrlSetSpec("a/")));
    assertFalse(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/")));

    // test disjointness with restricted parent RCUSS
    assertTrue(cuss1.isDisjoint(new RangeCachedUrlSetSpec("a/", null, "b")));
    assertFalse(cuss1.isDisjoint(new RangeCachedUrlSetSpec("a/", null, "c")));
    assertFalse(cuss1.isDisjoint(new RangeCachedUrlSetSpec("a/", "b", "c")));
    assertTrue(cuss1.isDisjoint(new RangeCachedUrlSetSpec("a/", "c", "d")));

    // not disjoint with same node RCUSS
    assertFalse(cuss1.isDisjoint(new RangeCachedUrlSetSpec("a/b/")));
    assertFalse(cuss2.isDisjoint(new RangeCachedUrlSetSpec("a/b/")));
    assertFalse(cuss3.isDisjoint(new RangeCachedUrlSetSpec("a/b/")));
    assertFalse(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/b/")));

    // not disjoint with child RCUSS
    assertFalse(cuss1.isDisjoint(new RangeCachedUrlSetSpec("a/b/c")));
    assertFalse(cuss2.isDisjoint(new RangeCachedUrlSetSpec("a/b/c")));
    assertFalse(cuss3.isDisjoint(new RangeCachedUrlSetSpec("a/b/c")));
    assertFalse(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/b/c")));

    // not disjoint with children in range
    assertFalse(cuss2.isDisjoint(new RangeCachedUrlSetSpec("a/b/cc")));
    assertFalse(cuss3.isDisjoint(new RangeCachedUrlSetSpec("a/b/cc")));
    assertFalse(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/b/cc")));

    // disjoint with children not in range
    assertTrue(cuss2.isDisjoint(new RangeCachedUrlSetSpec("a/b/b")));
    assertTrue(cuss3.isDisjoint(new RangeCachedUrlSetSpec("a/b/e")));
    assertTrue(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/b/b")));
    assertTrue(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/b/e")));


    // test overlapping, non-overlapping ranges
    assertFalse(cuss2.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "b",null)));
    assertFalse(cuss2.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "d",null)));
    assertFalse(cuss2.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "d", "e")));
    assertFalse(cuss2.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "c", "d")));
    assertTrue(cuss2.isDisjoint(new RangeCachedUrlSetSpec("a/b/", null, "b")));
    assertTrue(cuss2.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "a", "b")));

    assertFalse(cuss3.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "b",null)));
    assertFalse(cuss3.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "d",null)));
    assertFalse(cuss3.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "d","e")));
    assertFalse(cuss3.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "b","d")));
    assertTrue(cuss3.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "e", null)));
    assertTrue(cuss3.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "e", "f")));

    assertFalse(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "b",null)));
    assertFalse(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "d",null)));
    assertFalse(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "c", "d")));
    assertFalse(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "a", "c")));
    assertFalse(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "a", "e")));
    assertFalse(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "d", "e")));
    assertTrue(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/b/", null, "b")));
    assertTrue(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "a", "b")));
    assertTrue(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "e", null)));
    assertTrue(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "e", "f")));
  }

  public void testSubsumes() {
    CachedUrlSetSpec cuss1 = new RangeCachedUrlSetSpec("a/b/");
    CachedUrlSetSpec cuss2 = new RangeCachedUrlSetSpec("a/b/", "c", null);
    CachedUrlSetSpec cuss3 = new RangeCachedUrlSetSpec("a/b/", null, "d");
    CachedUrlSetSpec cuss4 = new RangeCachedUrlSetSpec("a/b/", "c", "d");

    // RCUSS never subsumes AUCUSS
    assertFalse(cuss1.subsumes(new AuCachedUrlSetSpec()));
    assertFalse(cuss2.subsumes(new AuCachedUrlSetSpec()));
    assertFalse(cuss3.subsumes(new AuCachedUrlSetSpec()));
    assertFalse(cuss4.subsumes(new AuCachedUrlSetSpec()));

    // child SNCUSS is subsumed if within range
    assertTrue(cuss1.subsumes(new SingleNodeCachedUrlSetSpec("a/b/c")));
    assertTrue(cuss2.subsumes(new SingleNodeCachedUrlSetSpec("a/b/c")));
    assertTrue(cuss3.subsumes(new SingleNodeCachedUrlSetSpec("a/b/c")));
    assertTrue(cuss4.subsumes(new SingleNodeCachedUrlSetSpec("a/b/c")));

    // child SNCUSS is not subsumed if not within range
    assertFalse(cuss2.subsumes(new SingleNodeCachedUrlSetSpec("a/b/b")));
    assertFalse(cuss3.subsumes(new SingleNodeCachedUrlSetSpec("a/b/e")));
    assertFalse(cuss4.subsumes(new SingleNodeCachedUrlSetSpec("a/b/b")));
    assertFalse(cuss4.subsumes(new SingleNodeCachedUrlSetSpec("a/b/e")));

    // SNCUSS at same node is subsumed only if RCUSS is not range-restricted
    assertTrue(cuss1.subsumes(new SingleNodeCachedUrlSetSpec("a/b/")));
    assertFalse(cuss2.subsumes(new SingleNodeCachedUrlSetSpec("a/b/")));
    assertFalse(cuss3.subsumes(new SingleNodeCachedUrlSetSpec("a/b/")));
    assertFalse(cuss4.subsumes(new SingleNodeCachedUrlSetSpec("a/b/")));

    // parent RCUSS never subsumed
    assertFalse(cuss1.subsumes(new RangeCachedUrlSetSpec("a/")));
    assertFalse(cuss2.subsumes(new RangeCachedUrlSetSpec("a/")));
    assertFalse(cuss3.subsumes(new RangeCachedUrlSetSpec("a/")));
    assertFalse(cuss4.subsumes(new RangeCachedUrlSetSpec("a/")));

    // sibling RCUSS never subsumed
    assertFalse(cuss1.subsumes(new RangeCachedUrlSetSpec("a/c")));
    assertFalse(cuss2.subsumes(new RangeCachedUrlSetSpec("a/c")));
    assertFalse(cuss3.subsumes(new RangeCachedUrlSetSpec("a/c")));
    assertFalse(cuss4.subsumes(new RangeCachedUrlSetSpec("a/c")));

    // child RCUSS is subsumed if within range
    assertTrue(cuss1.subsumes(new RangeCachedUrlSetSpec("a/b/c")));
    assertTrue(cuss2.subsumes(new RangeCachedUrlSetSpec("a/b/c")));
    assertTrue(cuss3.subsumes(new RangeCachedUrlSetSpec("a/b/c")));
    assertTrue(cuss4.subsumes(new RangeCachedUrlSetSpec("a/b/c")));

    // child RCUSS is not subsumed if not within range
    assertFalse(cuss2.subsumes(new RangeCachedUrlSetSpec("a/b/b")));
    assertFalse(cuss3.subsumes(new RangeCachedUrlSetSpec("a/b/e")));
    assertFalse(cuss4.subsumes(new RangeCachedUrlSetSpec("a/b/b")));
    assertFalse(cuss4.subsumes(new RangeCachedUrlSetSpec("a/b/e")));

    // non range-restricted subsumes all RCUSS at same node
    assertTrue(cuss1.subsumes(cuss1));
    assertTrue(cuss1.subsumes(cuss2));
    assertTrue(cuss1.subsumes(cuss3));
    assertTrue(cuss1.subsumes(cuss4));

    // range-restricted does not subsume non range-restricted at same node
    assertFalse(cuss2.subsumes(cuss1));
    assertFalse(cuss3.subsumes(cuss1));
    assertFalse(cuss4.subsumes(cuss1));

    // any range-restricted subsumes itself
    assertTrue(cuss2.subsumes(cuss2));
    assertTrue(cuss3.subsumes(cuss3));
    assertTrue(cuss4.subsumes(cuss4));

    // range-restricted subsumes same node RCUSS if range is included
    assertTrue(cuss2.subsumes(new RangeCachedUrlSetSpec("a/b/", "c", "d")));
    assertTrue(cuss2.subsumes(new RangeCachedUrlSetSpec("a/b/", "d", "e")));
    assertTrue(cuss2.subsumes(new RangeCachedUrlSetSpec("a/b/", "d", null)));

    assertFalse(cuss2.subsumes(new RangeCachedUrlSetSpec("a/b/", "b", null)));
    assertFalse(cuss2.subsumes(new RangeCachedUrlSetSpec("a/b/", "b", "d")));
    assertFalse(cuss2.subsumes(new RangeCachedUrlSetSpec("a/b/", "a", "b")));

    assertTrue(cuss3.subsumes(new RangeCachedUrlSetSpec("a/b/", "c", "d")));
    assertTrue(cuss3.subsumes(new RangeCachedUrlSetSpec("a/b/", "b", "d")));
    assertTrue(cuss3.subsumes(new RangeCachedUrlSetSpec("a/b/", null, "c")));

    assertFalse(cuss3.subsumes(new RangeCachedUrlSetSpec("a/b/", "c", null)));
    assertFalse(cuss3.subsumes(new RangeCachedUrlSetSpec("a/b/", "c", "e")));
    assertFalse(cuss3.subsumes(new RangeCachedUrlSetSpec("a/b/", "e", "f")));

    assertTrue(cuss4.subsumes(new RangeCachedUrlSetSpec("a/b/", "c", "d")));
    assertTrue(cuss4.subsumes(new RangeCachedUrlSetSpec("a/b/", "cc", "d")));
    assertTrue(cuss4.subsumes(new RangeCachedUrlSetSpec("a/b/", "c", "cz")));

    assertFalse(cuss4.subsumes(new RangeCachedUrlSetSpec("a/b/", "c", null)));
    assertFalse(cuss4.subsumes(new RangeCachedUrlSetSpec("a/b/", null, "d")));
    assertFalse(cuss4.subsumes(new RangeCachedUrlSetSpec("a/b/", "a", "b")));
    assertFalse(cuss4.subsumes(new RangeCachedUrlSetSpec("a/b/", "e", "f")));
  }

  private CachedUrlSetSpec makeCuss(int ix) {
    switch (ix) {
    case 1: return new AuCachedUrlSetSpec();
    case 2: return new SingleNodeCachedUrlSetSpec("a");
    case 3: return new SingleNodeCachedUrlSetSpec("a/b");
    case 4: return new SingleNodeCachedUrlSetSpec("a/c");
    case 5: return new RangeCachedUrlSetSpec("a");
    case 6: return new RangeCachedUrlSetSpec("a/b");
    case 7: return new RangeCachedUrlSetSpec("a/c");
    case 8: return new RangeCachedUrlSetSpec("a", null, "/b");
    case 9: return new RangeCachedUrlSetSpec("a", null, "/c");
    case 10: return new RangeCachedUrlSetSpec("a/b", null, "/b");
    case 11: return new RangeCachedUrlSetSpec("a/b", null, "/c");
    case 12: return new RangeCachedUrlSetSpec("a/b", "/a", null);
    case 13: return new RangeCachedUrlSetSpec("a/b", "/b", null);
    case 14: return new RangeCachedUrlSetSpec("a/b", "/a", "/b");
    case 15: return new RangeCachedUrlSetSpec("a/b", "/a", "/c");
    case 16: return new RangeCachedUrlSetSpec("a/b", "/b", "/b");
    case 17: return new RangeCachedUrlSetSpec("a/b", "/c", "/c");
    }
    return null;
  }
  static final int LAST = 17;

  public void testCombinations() {
    for (int x1 = 1; x1 <= LAST; x1++) {
      for (int x2 = 1; x2 <= LAST; x2++) {
	CachedUrlSetSpec c1 = makeCuss(x1);
	CachedUrlSetSpec c2 = makeCuss(x2);
	if (x1 == x2) {
	  // equals() and subsumes() are both reflexive
	  assertTrue(c1.equals(c2));
	  assertTrue(c2.equals(c1));
	  assertTrue(c1.subsumes(c2));
	  assertTrue(c2.subsumes(c1));
	} else {
	  // different CUSSes are not equal, and cannot each subsume the other
	  assertFalse(c1.equals(c2));
	  assertFalse(c2.equals(c1));
	  assertFalse(c1.subsumes(c2) && c2.subsumes(c1));
	}
	// isDisjoint is commutative
	assertEquals(c1.isDisjoint(c2), c2.isDisjoint(c1));
	// cannot both be disjoint and have subsumption relationship
	assertFalse(c1.isDisjoint(c2) && (c1.subsumes(c2) || c2.subsumes(c1)));
      }
    }
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestRangeCachedUrlSetSpec.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
