/*
 * $Id: TestRangeCachedUrlSetSpec.java,v 1.4 2003-03-04 01:02:06 aalto Exp $
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
 * This is the test class for org.lockss.daemon.CrawlSpec
 */

public class TestRangeCachedUrlSetSpec extends LockssTestCase {
  public TestRangeCachedUrlSetSpec(String msg){
    super(msg);
  }

  public void testIllRangeCachedUrlSetSpec() throws REException {
    try {
      new RangeCachedUrlSetSpec(null, "foo", "bar");
      fail("RangeCachedUrlSetSpec with null url should throw");
    } catch (NullPointerException e) {
    }
  }

  public void testRangeCachedUrlSetSpecEquivalence() throws REException {
    CachedUrlSetSpec cuss1 = new RangeCachedUrlSetSpec("foo", null, null);
    CachedUrlSetSpec cuss2 = new RangeCachedUrlSetSpec("foo");
    CachedUrlSetSpec cuss3 = new RangeCachedUrlSetSpec("bar");
    assertEquals(cuss1, cuss2);
    assertNotEquals(cuss2, cuss3);

    String uprb1 = "xyz";
    String uprb2 = "zyx";
    String lwrb1 = "abc";
    String lwrb2 = "bcd";

    RangeCachedUrlSetSpec cuss4 =
      new RangeCachedUrlSetSpec("xxx", lwrb1, uprb1);
    assertEquals(lwrb1, cuss4.getLowerBound());
    assertEquals(uprb1, cuss4.getUpperBound());
    CachedUrlSetSpec cuss5 = new RangeCachedUrlSetSpec("xxx", lwrb2, uprb2);
    CachedUrlSetSpec cuss6 = new RangeCachedUrlSetSpec("xxx", lwrb1, uprb2);
    CachedUrlSetSpec cuss7 = new RangeCachedUrlSetSpec("xxx", lwrb2, uprb1);
    CachedUrlSetSpec cuss8 = new RangeCachedUrlSetSpec("xxx", lwrb1, uprb1);

    assertEquals(cuss4,cuss8);

    assertNotEquals(cuss4, cuss5);
    assertNotEquals(cuss5, cuss6);
    assertNotEquals(cuss6, cuss7);
    assertNotEquals(cuss7, cuss4);
    assertNotEquals(cuss7, cuss5);
    assertNotEquals(cuss6, cuss4);
  }

  public void testRangeCachedUrlSetSpec() throws REException {
    String lwrb1 = "/abc";
    String uprb1 = "/xyz";

    // no range anything that begins with foo is a match
    CachedUrlSetSpec cuss1 = new RangeCachedUrlSetSpec("foo", null, null);
    assertEquals("foo", cuss1.getUrl());
    assertTrue(cuss1.matches("foo"));
    assertTrue(cuss1.matches("foobar"));
    assertTrue(cuss1.matches("foo/bar"));
    assertFalse(cuss1.matches("1foo"));

    // has a range must match upper and lower range
    CachedUrlSetSpec cuss2 = new RangeCachedUrlSetSpec("foo", lwrb1, uprb1);
    assertTrue(cuss2.matches("foo/camel"));
    assertFalse(cuss2.matches("foo/aardvark"));
    assertFalse(cuss2.matches("foo/zebra"));
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

  public void testGetUrl() throws Exception {
    CachedUrlSetSpec spec1 = new RangeCachedUrlSetSpec("foo", null, null);
    assertEquals("foo", spec1.getUrl());

    CachedUrlSetSpec spec2 = new RangeCachedUrlSetSpec("bar", "/abc", "/xyz");
    Set s = SetUtil.set(spec1, spec2);
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestRangeCachedUrlSetSpec.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
