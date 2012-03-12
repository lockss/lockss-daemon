/*
 * $Id: TestPrunedCachedUrlSetSpec.java,v 1.1.2.1 2012-03-12 07:04:45 tlipkis Exp $
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

//   public void testEquals() {
//     CachedUrlSetSpec cuss1 = new PrunedCachedUrlSetSpec("foo", null, null);
//     CachedUrlSetSpec cuss2 = new PrunedCachedUrlSetSpec("foo");
//     CachedUrlSetSpec cuss3 = new PrunedCachedUrlSetSpec("foo", "a", null);
//     CachedUrlSetSpec cuss4 = new PrunedCachedUrlSetSpec("foo", "a", null);
//     CachedUrlSetSpec cuss5 = new PrunedCachedUrlSetSpec("foo", "a", "b");
//     CachedUrlSetSpec cuss6 = new PrunedCachedUrlSetSpec("foo", "a", "b");
//     CachedUrlSetSpec cuss7 = new PrunedCachedUrlSetSpec("foo", null, "b");
//     CachedUrlSetSpec cuss8 = new PrunedCachedUrlSetSpec("foo", null, "b");
//     CachedUrlSetSpec cuss9 = new PrunedCachedUrlSetSpec("foo", "a", "a");
//     CachedUrlSetSpec cuss10 = new PrunedCachedUrlSetSpec("foo", "", "a");
//     CachedUrlSetSpec cuss11 = new PrunedCachedUrlSetSpec("bar");
//     assertEquals(cuss1, cuss2);
//     assertNotEquals(cuss1, cuss3);
//     assertNotEquals(cuss1, cuss11);
//     assertEquals(cuss3, cuss4);
//     assertNotEquals(cuss3, cuss5);
//     assertNotEquals(cuss3, cuss7);
//     assertNotEquals(cuss5, cuss7);
//     assertEquals(cuss5, cuss6);
//     assertEquals(cuss7, cuss8);
//     assertNotEquals(cuss5, cuss9);
//     assertNotEquals(cuss9, cuss10);

//     assertNotEquals(cuss1, new AuCachedUrlSetSpec());
//     assertNotEquals(cuss1, new SingleNodeCachedUrlSetSpec("foo"));
//   }

//   public void testHashCode() throws Exception {
//     CachedUrlSetSpec cuss1 = new PrunedCachedUrlSetSpec("foo", "/abc", "/xyz");
//     CachedUrlSetSpec cuss2 = new PrunedCachedUrlSetSpec("foo", "/abc", "/xyz");
//     assertTrue(cuss1.hashCode() == cuss2.hashCode());
//   }

//   public void testTypePredicates() {
//     CachedUrlSetSpec cuss1 = new PrunedCachedUrlSetSpec("foo");
//     assertFalse(cuss1.isSingleNode());
//     assertFalse(cuss1.isAu());
//     assertFalse(cuss1.isRangeRestricted());

//     CachedUrlSetSpec cuss2 = new PrunedCachedUrlSetSpec("foo", "a", null);
//     assertFalse(cuss2.isSingleNode());
//     assertFalse(cuss2.isAu());
//     assertTrue(cuss2.isRangeRestricted());

//     CachedUrlSetSpec cuss3 = new PrunedCachedUrlSetSpec("foo", null, "b");
//     assertFalse(cuss3.isSingleNode());
//     assertFalse(cuss3.isAu());
//     assertTrue(cuss3.isRangeRestricted());

//     CachedUrlSetSpec cuss4 = new PrunedCachedUrlSetSpec("foo", "a", "b");
//     assertFalse(cuss4.isSingleNode());
//     assertFalse(cuss4.isAu());
//     assertTrue(cuss4.isRangeRestricted());
//   }

  public void testMatchIncludeSubTree() {
    PrunedCachedUrlSetSpec cuss1 =
      PrunedCachedUrlSetSpec.includeMatchingSubTrees("http://foo/bar/",
						     "http://foo/bar/abc");
    assertFalse(cuss1.matches("http://xfoo/bar/abc"));
    assertTrue(cuss1.matches("http://foo/bar/abc"));
    assertTrue(cuss1.matches("http://foo/bar/abcd"));
    assertTrue(cuss1.matches("http://foo/bar/abc/"));
    assertTrue(cuss1.matches("http://foo/bar/abc/xx"));
    assertTrue(cuss1.matches("http://foo/bar/"));
    assertFalse(cuss1.matches("http://foo/bar/def"));
  }

  public void testMatchExcludeSubTree() {
    PrunedCachedUrlSetSpec cuss1 =
      PrunedCachedUrlSetSpec.excludeMatchingSubTrees("http://foo/bar/",
						     "http://foo/bar/abc");
    assertFalse(cuss1.matches("http://xfoo/bar/"));
    assertTrue(cuss1.matches("http://foo/bar/"));
    assertTrue(cuss1.matches("http://foo/bar/abd"));
    assertFalse(cuss1.matches("http://foo/bar/abc"));
    assertFalse(cuss1.matches("http://foo/bar/abc/"));
    assertFalse(cuss1.matches("http://foo/bar/abc/xx"));
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
