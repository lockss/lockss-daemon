/*
 * $Id: TestRangeCachedUrlSetSpec.java,v 1.6 2003-06-03 05:49:32 tal Exp $
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

package org.lockss.daemon;
import java.util.*;
import junit.framework.TestCase;
import gnu.regexp.*;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * This is the test class for org.lockss.daemon.RangeCachedUrlSetSpec
 */

public class TestRangeCachedUrlSetSpec extends LockssTestCase {

  public void testIll() throws REException {
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
  }

  public void testEquals() throws REException {
    CachedUrlSetSpec cuss1 = new RangeCachedUrlSetSpec("foo", null, null);
    CachedUrlSetSpec cuss2 = new RangeCachedUrlSetSpec("foo");
    CachedUrlSetSpec cuss3 = new RangeCachedUrlSetSpec("foo", "a", null);
    CachedUrlSetSpec cuss4 = new RangeCachedUrlSetSpec("foo", "a", null);
    CachedUrlSetSpec cuss5 = new RangeCachedUrlSetSpec("foo", "a", "b");
    CachedUrlSetSpec cuss6 = new RangeCachedUrlSetSpec("foo", "a", "b");
    CachedUrlSetSpec cuss7 = new RangeCachedUrlSetSpec("foo", null, "b");
    CachedUrlSetSpec cuss8 = new RangeCachedUrlSetSpec("foo", null, "b");
    CachedUrlSetSpec cuss9 = new RangeCachedUrlSetSpec("foo", "b", "a");
    CachedUrlSetSpec cuss10 = new RangeCachedUrlSetSpec("bar");
    assertEquals(cuss1, cuss2);
    assertNotEquals(cuss1, cuss3);
    assertNotEquals(cuss1, cuss10);
    assertEquals(cuss3, cuss4);
    assertNotEquals(cuss3, cuss5);
    assertNotEquals(cuss3, cuss7);
    assertEquals(cuss3, cuss4);
    assertEquals(cuss5, cuss6);
    assertEquals(cuss7, cuss8);
    assertNotEquals(cuss5, cuss9);
  }

  public void testNoRange() {
    RangeCachedUrlSetSpec cuss1 = new RangeCachedUrlSetSpec("foo");
    assertEquals("foo", cuss1.getUrl());
    assertEquals(null, cuss1.getLowerBound());
    assertEquals(null, cuss1.getUpperBound());
    assertTrue(cuss1.matches("foo"));
    assertTrue(cuss1.matches("foobar"));
    assertTrue(cuss1.matches("foo/bar"));
    assertFalse(cuss1.matches("1foo"));
  }

  public void testLower() {
    RangeCachedUrlSetSpec cuss2 =
      new RangeCachedUrlSetSpec("foo", "/123", null);
    assertEquals("foo", cuss2.getUrl());
    assertEquals("/123", cuss2.getLowerBound());
    assertEquals(null, cuss2.getUpperBound());
    assertFalse(cuss2.matches("foo"));	// ranged, shouldn't match prefix
    assertFalse(cuss2.matches("/123foo"));
    assertTrue(cuss2.matches("foo/123"));
    assertTrue(cuss2.matches("foo/123/x"));
    assertFalse(cuss2.matches("foo/122"));
    assertFalse(cuss2.matches("foo/0"));
  }

  public void testUpper() {
    RangeCachedUrlSetSpec cuss3 =
      new RangeCachedUrlSetSpec("bar/", null, "123");
    assertEquals("bar/", cuss3.getUrl());
    assertEquals(null, cuss3.getLowerBound());
    assertEquals("123", cuss3.getUpperBound());
    // Our statement of what a RangeCachedUrlSetSpec means when it has a range
    // suggests that this should succeed.  It's not implemented that way
    // though, and not clear whether it should be changed
//     assertFalse(cuss3.matches("bar/"));  // ranged, shouldn't match prefix
    assertTrue(cuss3.matches("bar/"));
    assertTrue(cuss3.matches("bar/0"));
    assertTrue(cuss3.matches("bar/123"));
    assertFalse(cuss3.matches("bar/123/4"));
    assertFalse(cuss3.matches("bar/124"));
  }

  public void testBoth() {
    RangeCachedUrlSetSpec cuss4 =
      new RangeCachedUrlSetSpec("bar/", "222", "555");
    assertEquals("bar/", cuss4.getUrl());
    assertEquals("222", cuss4.getLowerBound());
    assertEquals("555", cuss4.getUpperBound());
    assertFalse(cuss4.matches("bar/"));
    assertFalse(cuss4.matches("bar/0"));
    assertTrue(cuss4.matches("bar/222"));
    assertTrue(cuss4.matches("bar/223"));
    assertTrue(cuss4.matches("bar/24"));
    assertFalse(cuss4.matches("bar/556"));
  }

  public void testHashCode() throws Exception {
    String lwrb1 = "/abc";
    String uprb1 = "/xyz";

    String lwrb2 = "/bcd";
    String uprb2 = "/zyx";

    CachedUrlSetSpec spec1 = new RangeCachedUrlSetSpec("foo", lwrb1, uprb1);
    CachedUrlSetSpec spec2 = new RangeCachedUrlSetSpec("bar", lwrb1, uprb1);
    assertTrue(spec1.hashCode() != spec2.hashCode());
    assertTrue(spec1 != spec2);

    spec2 = new RangeCachedUrlSetSpec("foo", lwrb2, uprb2);
    assertTrue(spec1.hashCode() != spec2.hashCode());
    assertTrue(spec1 != spec2);

    spec2 = new RangeCachedUrlSetSpec("foo", lwrb1, uprb1);
    assertEquals(spec1.hashCode(), spec2.hashCode());
    assertEquals(spec1, spec2);
  }

  public void testTypePredicates() {
    CachedUrlSetSpec cuss1 = new RangeCachedUrlSetSpec("foo");
    assertFalse(cuss1.isSingleNode());
    assertFalse(cuss1.isAU());
    assertFalse(cuss1.isRangeRestricted());

    CachedUrlSetSpec cuss2 = new RangeCachedUrlSetSpec("foo", "a", null);
    assertFalse(cuss2.isSingleNode());
    assertFalse(cuss2.isAU());
    assertTrue(cuss2.isRangeRestricted());

    CachedUrlSetSpec cuss3 = new RangeCachedUrlSetSpec("foo", null, "b");
    assertFalse(cuss3.isSingleNode());
    assertFalse(cuss3.isAU());
    assertTrue(cuss3.isRangeRestricted());
  }

  public void testDisjoint() {
    CachedUrlSetSpec cuss1 = new RangeCachedUrlSetSpec("a/b/");
    CachedUrlSetSpec cuss2 = new RangeCachedUrlSetSpec("a/b/", "c", null);
    CachedUrlSetSpec cuss3 = new RangeCachedUrlSetSpec("a/b/", null, "d");
    CachedUrlSetSpec cuss4 = new RangeCachedUrlSetSpec("a/b/", "c", "d");

    assertFalse(cuss1.isDisjoint(new AUCachedUrlSetSpec()));
    assertFalse(cuss2.isDisjoint(new AUCachedUrlSetSpec()));
    assertFalse(cuss3.isDisjoint(new AUCachedUrlSetSpec()));
    assertFalse(cuss4.isDisjoint(new AUCachedUrlSetSpec()));

    assertTrue(cuss1.isDisjoint(new SingleNodeCachedUrlSetSpec("a")));
    assertTrue(cuss2.isDisjoint(new SingleNodeCachedUrlSetSpec("a")));
    assertTrue(cuss3.isDisjoint(new SingleNodeCachedUrlSetSpec("a")));
    assertTrue(cuss4.isDisjoint(new SingleNodeCachedUrlSetSpec("a")));

    assertTrue(cuss1.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b")));
    assertTrue(cuss2.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b")));
    assertTrue(cuss3.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b")));
    assertTrue(cuss4.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b")));

    assertFalse(cuss1.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b/c")));
    assertFalse(cuss2.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b/c")));
    assertFalse(cuss3.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b/c")));
    assertFalse(cuss4.isDisjoint(new SingleNodeCachedUrlSetSpec("a/b/c")));

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

    assertFalse(cuss1.isDisjoint(new RangeCachedUrlSetSpec("a/")));
    assertFalse(cuss2.isDisjoint(new RangeCachedUrlSetSpec("a/")));
    assertFalse(cuss3.isDisjoint(new RangeCachedUrlSetSpec("a/")));
    assertFalse(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/")));

    assertFalse(cuss1.isDisjoint(new RangeCachedUrlSetSpec("a/b/")));
    assertFalse(cuss2.isDisjoint(new RangeCachedUrlSetSpec("a/b/")));
    assertFalse(cuss3.isDisjoint(new RangeCachedUrlSetSpec("a/b/")));
    assertFalse(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/b/")));

    assertFalse(cuss1.isDisjoint(new RangeCachedUrlSetSpec("a/b/c")));
    assertFalse(cuss2.isDisjoint(new RangeCachedUrlSetSpec("a/b/c")));
    assertFalse(cuss3.isDisjoint(new RangeCachedUrlSetSpec("a/b/c")));
    assertFalse(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/b/c")));

    assertTrue(cuss2.isDisjoint(new RangeCachedUrlSetSpec("a/b/b")));
    assertTrue(cuss3.isDisjoint(new RangeCachedUrlSetSpec("a/b/e")));
    assertTrue(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/b/b")));
    assertTrue(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/b/e")));


    assertFalse(cuss2.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "b",null)));
    assertFalse(cuss2.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "d",null)));
    assertFalse(cuss2.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "d", "e")));
    assertTrue(cuss2.isDisjoint(new RangeCachedUrlSetSpec("a/b/", null, "b")));
    assertTrue(cuss2.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "a", "b")));

    assertFalse(cuss3.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "b",null)));
    assertFalse(cuss3.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "d",null)));
    assertFalse(cuss3.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "d","e")));
    assertFalse(cuss3.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "b","d")));
    assertTrue(cuss3.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "e", null)));
    assertTrue(cuss3.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "e", "f")));

    assertFalse(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "b",null)));
    assertFalse(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "c",null)));
    assertFalse(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "c", "d")));
    assertFalse(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "a", "c")));
    assertFalse(cuss4.isDisjoint(new RangeCachedUrlSetSpec("a/b/", "a", "e")));
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

    assertFalse(cuss1.subsumes(new AUCachedUrlSetSpec()));
    assertFalse(cuss2.subsumes(new AUCachedUrlSetSpec()));
    assertFalse(cuss3.subsumes(new AUCachedUrlSetSpec()));
    assertFalse(cuss4.subsumes(new AUCachedUrlSetSpec()));

    assertTrue(cuss1.subsumes(new SingleNodeCachedUrlSetSpec("a/b/c")));
    assertTrue(cuss2.subsumes(new SingleNodeCachedUrlSetSpec("a/b/c")));
    assertTrue(cuss3.subsumes(new SingleNodeCachedUrlSetSpec("a/b/c")));
    assertTrue(cuss4.subsumes(new SingleNodeCachedUrlSetSpec("a/b/c")));

    assertFalse(cuss1.subsumes(new SingleNodeCachedUrlSetSpec("a/b/")));
    assertFalse(cuss2.subsumes(new SingleNodeCachedUrlSetSpec("a/b/")));
    assertFalse(cuss3.subsumes(new SingleNodeCachedUrlSetSpec("a/b/")));
    assertFalse(cuss4.subsumes(new SingleNodeCachedUrlSetSpec("a/b/")));

    assertFalse(cuss2.subsumes(new SingleNodeCachedUrlSetSpec("a/b/b")));
    assertFalse(cuss3.subsumes(new SingleNodeCachedUrlSetSpec("a/b/e")));
    assertFalse(cuss4.subsumes(new SingleNodeCachedUrlSetSpec("a/b/b")));
    assertFalse(cuss4.subsumes(new SingleNodeCachedUrlSetSpec("a/b/e")));

    assertTrue(cuss1.subsumes(new RangeCachedUrlSetSpec("a/b/", "1", "2")));
    assertTrue(cuss1.subsumes(new RangeCachedUrlSetSpec("a/b/", null, "2")));
    assertTrue(cuss1.subsumes(new RangeCachedUrlSetSpec("a/b/", "1", null)));
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestRangeCachedUrlSetSpec.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
