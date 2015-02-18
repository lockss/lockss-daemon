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
import junit.framework.TestCase;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * This is the test class for org.lockss.daemon.SingleNodeCachedUrlSetSpec
 */

public class TestSingleNodeCachedUrlSetSpec extends LockssTestCase {

  public void testIll() {
    try {
      new SingleNodeCachedUrlSetSpec(null);
      fail("SingleNodeCachedUrlSetSpec with null url should throw");
    } catch (NullPointerException e) {
    }
  }

  public void testGetUrl() {
    CachedUrlSetSpec cuss = new SingleNodeCachedUrlSetSpec("1/2/3");
    assertEquals("1/2/3", cuss.getUrl());
  }

  public void testMatches() {
    CachedUrlSetSpec cuss1 = new SingleNodeCachedUrlSetSpec("foo");
    assertTrue(cuss1.matches("foo"));
    assertFalse(cuss1.matches("fo"));
    assertFalse(cuss1.matches("foobar"));
    assertFalse(cuss1.matches("bar"));
    assertFalse(cuss1.matches(""));
  }

  public void testEquals() throws Exception {
    CachedUrlSetSpec cuss1 = new SingleNodeCachedUrlSetSpec("foo");
    CachedUrlSetSpec cuss2 = new SingleNodeCachedUrlSetSpec("foo");
    CachedUrlSetSpec cuss3 = new SingleNodeCachedUrlSetSpec("bar");
    assertEquals(cuss1, cuss2);
    assertNotEquals(cuss1, cuss3);
    assertNotEquals(cuss1, new AuCachedUrlSetSpec());
    assertNotEquals(cuss1, new RangeCachedUrlSetSpec("foo"));
  }

  public void testHashCode() throws Exception {
    CachedUrlSetSpec spec1 = new SingleNodeCachedUrlSetSpec("foo");
    assertEquals("foo".hashCode(), spec1.hashCode());
  }

  public void testTypePredicates() {
    CachedUrlSetSpec cuss = new SingleNodeCachedUrlSetSpec("foo");
    assertTrue(cuss.isSingleNode());
    assertFalse(cuss.isAu());
    assertFalse(cuss.isRangeRestricted());
  }

  public void testDisjoint() {
    CachedUrlSetSpec cuss1 = new SingleNodeCachedUrlSetSpec("a/b");

    assertFalse(cuss1.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b")));
    assertTrue(cuss1.isDisjoint(new SingleNodeCachedUrlSetSpec("a")));
    assertTrue(cuss1.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b1")));
    assertTrue(cuss1.isDisjoint(new SingleNodeCachedUrlSetSpec("a/c")));

    assertFalse(cuss1.isDisjoint(new AuCachedUrlSetSpec()));

    assertFalse(cuss1.isDisjoint(new RangeCachedUrlSetSpec("a/")));
    assertFalse(cuss1.isDisjoint(new RangeCachedUrlSetSpec("a/", "b", null)));
    assertFalse(cuss1.isDisjoint(new RangeCachedUrlSetSpec("a/", null, "b")));
    assertTrue(cuss1.isDisjoint(new RangeCachedUrlSetSpec("a/", "c", null)));
    assertTrue(cuss1.isDisjoint(new RangeCachedUrlSetSpec("a/", null, "a")));
    assertTrue(cuss1.isDisjoint(new RangeCachedUrlSetSpec("a/c")));

    // not disjoint with unrestricted RCUSS at same node, disjoint with a
    // restricted one
    assertFalse(cuss1.isDisjoint(new RangeCachedUrlSetSpec("a/b")));
    assertTrue(cuss1.isDisjoint(new RangeCachedUrlSetSpec("a/b", "", null)));
    assertTrue(cuss1.isDisjoint(new RangeCachedUrlSetSpec("a/b", null, "z")));
  }

  public void testSubsumes() {
    CachedUrlSetSpec cuss = new SingleNodeCachedUrlSetSpec("foo");

    assertTrue(cuss.subsumes(new SingleNodeCachedUrlSetSpec("foo")));
    assertFalse(cuss.subsumes(new SingleNodeCachedUrlSetSpec("foo/bar")));
    assertFalse(cuss.subsumes(new SingleNodeCachedUrlSetSpec("fo")));
    assertFalse(cuss.subsumes(new SingleNodeCachedUrlSetSpec("bar")));

    assertFalse(cuss.subsumes(new RangeCachedUrlSetSpec("foo")));
    assertFalse(cuss.subsumes(new RangeCachedUrlSetSpec("bar")));
    assertFalse(cuss.subsumes(new RangeCachedUrlSetSpec("foo", "1", "2")));
    assertFalse(cuss.subsumes(new RangeCachedUrlSetSpec("foo", null, "")));

    assertFalse(cuss.subsumes(new AuCachedUrlSetSpec()));
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestSingleNodeCachedUrlSetSpec.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
