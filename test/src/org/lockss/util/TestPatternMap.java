/*

Copyright (c) 2000-2022 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.function.*;
import java.util.regex.*;
import org.apache.commons.lang3.tuple.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestPatternMap extends LockssTestCase {

  static Function ID = Function.identity();

  private PatternMap makeMap() {
    return PatternMap.fromPairs(ListUtil.list(Pair.of("a.*b", 2),
                                              Pair.of("ccc", "foo")));
  }

  public void testToString() {
    PatternMap ppm1 = makeMap();
    assertEquals("[pm: [a.*b: 2], [ccc: foo]]", ppm1.toString());
  }

  public void testGetMatch() {
    PatternMap ppm1 = makeMap();
    assertEquals(null, ppm1.getMatch("a123c"));
    assertEquals("df", ppm1.getMatch("a123c", "df"));
    assertEquals(2, ppm1.getMatch("a123b"));
    assertEquals(2, ppm1.getMatch("accccb"));
    assertEquals("foo", ppm1.getMatch("bccccb"));
    assertEquals("foo", ppm1.getMatch("bccccb", "not"));
  }

  public void testEmpty() {
    PatternMap ppm = PatternMap.EMPTY;
    assertEquals(null, ppm.getMatch("a"));
    assertEquals("42", ppm.getMatch("a", "42"));
    assertEquals("[pm: EMPTY]", ppm.toString());
  }

  public void testIsEmpty() {
    assertTrue(PatternMap.EMPTY.isEmpty());
    assertTrue(PatternMap.fromPairs(Collections.emptyList()).isEmpty());
    assertFalse(makeMap().isEmpty());
  }

  public void testIll() {
    try {
      PatternMap ppm1 =
        PatternMap.fromPairs(ListUtil.list(Pair.of("a[)2", "foo")));
      fail("Should throw: Malformed");
    } catch (IllegalArgumentException e) {
      assertClass(PatternSyntaxException.class, e.getCause());
    }
  }

  public void testListMap() {
    PatternMap<List> plm =
      PatternMap.fromPairs(ListUtil.list(Pair.of("a.*b", ListUtil.list(1,2)),
                                               Pair.of("ccc", ListUtil.list(3,3))));
    assertEquals(ListUtil.list(1,2), plm.getMatch("a123cb"));
  }
}
