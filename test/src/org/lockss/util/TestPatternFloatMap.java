/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;

import java.util.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestPatternFloatMap extends LockssTestCase {

  public void testToString() {
    PatternFloatMap ppm1 = PatternFloatMap.fromSpec("a.*b,2;ccc,4.3");
    assertEquals("[pm: [a.*b: 2.0], [ccc: 4.3]]", ppm1.toString());

    PatternFloatMap ppm2 =
      PatternFloatMap.fromSpec(ListUtil.list("xx,1", "yy,2"));
    assertEquals("[pm: [xx: 1.0], [yy: 2.0]]", ppm2.toString());
  }

  public void testGetMatch() {
    testGetMatch(PatternFloatMap.fromSpec("a.*b,2.5;ccc,-4;ddd,5e2"));
    testGetMatch(PatternFloatMap.fromSpec(ListUtil.list("a.*b,2.5", "ccc,-4",
                                                        "ddd,5e2")));
  }

  public void testGetMatchDeprecated() {
    testGetMatch(new PatternFloatMap("a.*b,2.5;ccc,-4;ddd,5e2"));
    testGetMatch(new PatternFloatMap(ListUtil.list("a.*b,2.5", "ccc,-4",
                                                   "ddd,5e2")));
  }

  public void testGetMatch(PatternFloatMap ppm1) {
    assertEquals(0.0F, ppm1.getMatch("a123c"));
    assertEquals(123.0F, ppm1.getMatch("a123c", 123));
    assertEquals(2.5F, ppm1.getMatch("a123b"));
    assertEquals(2.5F, ppm1.getMatch("accccb"));
    assertEquals(-4.0F, ppm1.getMatch("bccccb"));
    assertEquals(-4.0F, ppm1.getMatch("bccccb", 111));
    assertEquals(-4F, ppm1.getMatch("bccccb", 111, 10));
    assertEquals(111F, ppm1.getMatch("bccccb", 111, -10));
    assertEquals(500F, ppm1.getMatch("bdddb"));
  }

  public void testEmpty() {
    PatternFloatMap ppm = PatternFloatMap.EMPTY;
    assertEquals(0F, ppm.getMatch("a"));
    assertEquals(42.5F, ppm.getMatch("a", 42.5F));
    assertEquals("[pm: EMPTY]", ppm.toString());
  }

  public void testIsEmpty() {
    assertTrue(PatternFloatMap.EMPTY.isEmpty());
    assertTrue(PatternFloatMap.fromSpec("").isEmpty());
    assertFalse(PatternFloatMap.fromSpec("a.*b,2;ccc,-4").isEmpty());
  }

  public void testIll() {
    try {
      PatternFloatMap ppm1 = PatternFloatMap.fromSpec("a[)2");
      fail("Should throw: Malformed");
    } catch (IllegalArgumentException e) {
      assertMatchesRE("no comma", e.getMessage());
    }
    try {
      PatternFloatMap ppm1 = PatternFloatMap.fromSpec("a,1;bbb");
      fail("Should throw: Malformed");
    } catch (IllegalArgumentException e) {
      assertMatchesRE("no comma", e.getMessage());
    }
    try {
      PatternFloatMap ppm1 = PatternFloatMap.fromSpec("a.*b,2;ccc,xyz");
      fail("Should throw: illegal RHS");
    } catch (IllegalArgumentException e) {
      assertMatchesRE("Illegal RHS.*xyz", e.getMessage());
    }
    try {
      PatternFloatMap ppm1 = PatternFloatMap.fromSpec("a[),2");
      fail("Should throw: illegal regexp");
    } catch (IllegalArgumentException e) {
      assertMatchesRE("Illegal regexp", e.getMessage());
    }
  }

}
