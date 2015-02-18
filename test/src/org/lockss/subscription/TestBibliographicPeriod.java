/*
 * $Id$
 */

/*

 Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * Test class for org.lockss.subscription.BibliographicPeriod.
 * 
 * @author Fernando Garcia-Loygorri
 * @version 1.0
 */
package org.lockss.subscription;

import java.util.Arrays;
import java.util.List;
import org.lockss.test.LockssTestCase;

public class TestBibliographicPeriod extends LockssTestCase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  /**
   * Check the behavior of BibliographicPeriod(String).
   */
  public final void testConstructor1String() {
    assertEquals("BibliographicPeriod [startEdge=null, endEdge=null]",
	new BibliographicPeriod(null).toString());
    assertEquals("BibliographicPeriod [startEdge=null, endEdge=null]",
	new BibliographicPeriod("").toString());
    assertEquals("BibliographicPeriod [startEdge=null, endEdge=null]",
	new BibliographicPeriod(" ").toString());
    assertEquals(expected("null", "null", "null", "null", "null", "null"),
	new BibliographicPeriod("-").toString());
    assertEquals(expected("null", "null", "null", "null", "null", "null"),
	new BibliographicPeriod(" -").toString());
    assertEquals(expected("null", "null", "null", "null", "null", "null"),
	new BibliographicPeriod("- ").toString());
    assertEquals(expected("null", "null", "null", "null", "null", "null"),
	new BibliographicPeriod(" - ").toString());
    assertEquals(expected("1954", "null", "null", "1954", "null", "null"),
	new BibliographicPeriod("1954").toString());
    assertEquals(expected("1954", "null", "null", "1954", "null", "null"),
	new BibliographicPeriod(" 1954").toString());
    assertEquals(expected("1954", "null", "null", "1954", "null", "null"),
	new BibliographicPeriod("1954 ").toString());
    assertEquals(expected("1954", "null", "null", "1954", "null", "null"),
	new BibliographicPeriod(" 1954 ").toString());
    assertEquals(expected("1954", "null", "null", "1954", "null", "null"),
	new BibliographicPeriod("19 54").toString());
    assertEquals(expected("1954", "null", "null", "1954", "null", "null"),
	new BibliographicPeriod("1 9 5 4").toString());
    assertEquals(expected("1954", "null", "null", "1954", "null", "null"),
	new BibliographicPeriod(" 1 9 5 4 ").toString());
    assertEquals(expected("1954", "null", "null", "null", "null", "null"),
	new BibliographicPeriod("1954-").toString());
    assertEquals(expected("null", "null", "null", "1954", "null", "null"),
	new BibliographicPeriod("-1954").toString());
    assertEquals(expected("1954", "null", "null", "1954", "null", "null"),
	new BibliographicPeriod("1954-1954").toString());
    assertEquals(expected("1954", "null", "null", "1988", "null", "null"),
	new BibliographicPeriod("1954-1988").toString());
    assertEquals(expected("1954", "4", "null", "1988", "null", "null"),
	new BibliographicPeriod("1954(4)-1988").toString());
    assertEquals(expected("1954", "4", "2", "1988", "null", "null"),
	new BibliographicPeriod("1954(4)(2)-1988").toString());
    assertEquals(expected("1954", "null", "2", "1988", "null", "null"),
	new BibliographicPeriod("1954()(2)-1988").toString());
    assertEquals(expected("null", "null", "2", "1988", "null", "null"),
	new BibliographicPeriod("()(2)-1988").toString());
    assertEquals(expected("null", "4", "2", "1988", "null", "null"),
	new BibliographicPeriod("(4)(2)-1988").toString());
    assertEquals(expected("null", "4", "null", "1988", "null", "null"),
	new BibliographicPeriod("(4)-1988").toString());
    assertEquals(expected("1954", "null", "null", "1988", "12", "null"),
	new BibliographicPeriod("1954-1988(12)").toString());
    assertEquals(expected("1954", "null", "null", "1988", "12", "28"),
	new BibliographicPeriod("1954-1988(12)(28)").toString());
    assertEquals(expected("1954", "null", "null", "1988", "null", "28"),
	new BibliographicPeriod("1954-1988()(28)").toString());
    assertEquals(expected("1954", "null", "null", "null", "null", "28"),
	new BibliographicPeriod("1954-()(28)").toString());
    assertEquals(expected("1954", "null", "null", "null", "12", "28"),
	new BibliographicPeriod("1954-(12)(28)").toString());
    assertEquals(expected("1954", "null", "null", "null", "12", "null"),
	new BibliographicPeriod("1954-(12)").toString());

    try {
      new BibliographicPeriod("(");
      fail("Should be illegal to create a period with unbalanced parentheses");
    } catch (Exception e) {
      // Expected.
    }

    try {
      new BibliographicPeriod(")");
      fail("Should be illegal to create a period with unbalanced parentheses");
    } catch (Exception e) {
      // Expected.
    }

    assertEquals(expected("null", "-", "null", "null", "-", "null"),
	new BibliographicPeriod("(-)").toString());

    try {
      new BibliographicPeriod("()(");
      fail("Should be illegal to create a period with unbalanced parentheses");
    } catch (Exception e) {
      // Expected.
    }

    try {
      new BibliographicPeriod(")()");
      fail("Should be illegal to create a period with unbalanced parentheses");
    } catch (Exception e) {
      // Expected.
    }

    assertEquals(expected("null", "null", "-", "null", "null", "-"),
	new BibliographicPeriod("()(-)").toString());
  }

  private String expected(String year1, String volume1, String issue1,
      String year2, String volume2, String issue2) {
    return "BibliographicPeriod [startEdge=BibliographicPeriodEdge [year="
      + year1 + ", volume=" + volume1 + ", issue=" + issue1
      + "], endEdge=BibliographicPeriodEdge [year="
      + year2 + ", volume=" + volume2 + ", issue=" + issue2 + "]]";
  }

  /**
   * Check the behavior of BibliographicPeriod(String, String).
   */
  public final void testConstructor2Strings() {
    String nullString = null;
    assertEquals(expected("null", "null", "null", "null", "null", "null"),
	new BibliographicPeriod(nullString, nullString).toString());
    assertEquals(expected("null", "null", "null", "null", "null", "null"),
	new BibliographicPeriod(null, "").toString());
    assertEquals(expected("null", "null", "null", "null", "null", "null"),
	new BibliographicPeriod(null, " ").toString());
    assertEquals(expected("null", "null", "null", "null", "null", "null"),
	new BibliographicPeriod("", null).toString());
    assertEquals(expected("null", "null", "null", "null", "null", "null"),
	new BibliographicPeriod(" ", null).toString());
    assertEquals(expected("null", "null", "null", "null", "null", "null"),
	new BibliographicPeriod("", "").toString());
    assertEquals(expected("null", "null", "null", "null", "null", "null"),
	new BibliographicPeriod(" ", " ").toString());
    assertEquals(expected("1954", "null", "null", "1954", "null", "null"),
	new BibliographicPeriod("1954", "1954").toString());
    assertEquals(expected("1954", "null", "null", "1954", "null", "null"),
	new BibliographicPeriod(" 1954", "1954 ").toString());
    assertEquals(expected("1954", "null", "null", "1954", "null", "null"),
	new BibliographicPeriod(" 1954 ", "1 9 5 4").toString());
    assertEquals(expected("1954", "null", "null", "1988", "null", "null"),
	new BibliographicPeriod("1954", "1988").toString());
    assertEquals(expected("1954", "null", "null", "1988", "null", "null"),
	new BibliographicPeriod("1954 ", " 1988").toString());
    assertEquals(expected("1954", "null", "null", "1988", "null", "null"),
	new BibliographicPeriod("1 9 5 4", " 1 9 8 8 ").toString());
    assertEquals(expected("-", "null", "null", "null", "null", "null"),
	new BibliographicPeriod("-", null).toString());
    assertEquals(expected("null", "null", "null", "-", "null", "null"),
	new BibliographicPeriod(null, "-").toString());
  }

  /**
   * Check the behavior of BibliographicPeriod(String, String, String, String,
   * String, String).
   */
  public final void testConstructor6Strings() {
    assertEquals("BibliographicPeriod [startEdge=null, endEdge=null]",
	new BibliographicPeriod(null, null, null, null, null, null).toString());
    assertEquals("BibliographicPeriod [startEdge=null, endEdge=null]",
	new BibliographicPeriod(null, "", null, "", null, "").toString());
    assertEquals("BibliographicPeriod [startEdge=null, endEdge=null]",
	new BibliographicPeriod(null, " ", null, " ", null, " ").toString());
    assertEquals("BibliographicPeriod [startEdge=null, endEdge=null]",
	new BibliographicPeriod("", null, "", null, "", null).toString());
    assertEquals("BibliographicPeriod [startEdge=null, endEdge=null]",
	new BibliographicPeriod(" ", null, " ", null, " ", null).toString());
    assertEquals("BibliographicPeriod [startEdge=null, endEdge=null]",
	new BibliographicPeriod("", "", "", "", "", "").toString());
    assertEquals("BibliographicPeriod [startEdge=null, endEdge=null]",
	new BibliographicPeriod(" ", " ", " ", " ", " ", " ").toString());
    assertEquals(expected("1954", "null", "null", "null", "null", "null"),
	new BibliographicPeriod("1954", "", "", "", "", "").toString());
    assertEquals(expected("1954", "4", "null", "null", "null", "null"),
	new BibliographicPeriod(" 1954", "4", "", "", "", "").toString());
    assertEquals(expected("1954", "4", "2", "null", "null", "null"),
	new BibliographicPeriod("1954 ", " 4", "2", "", "", "").toString());
    assertEquals(expected("null", "4", "2", "null", "null", "null"),
	new BibliographicPeriod(" ", "4 ", " 2", "", "", "").toString());
    assertEquals(expected("1954", "null", "2", "null", "null", "null"),
	new BibliographicPeriod(" 1954 ", "", " 2", "", "", "").toString());
    assertEquals(expected("null", "null", "null", "1954", "null", "null"),
	new BibliographicPeriod("", "", "", "1954", "", "").toString());
    assertEquals(expected("null", "null", "null", "1954", "4", "null"),
	new BibliographicPeriod("", "", "", " 1954", "4", "").toString());
    assertEquals(expected("null", "null", "null", "1954", "4", "2"),
	new BibliographicPeriod("", "", "", "1954 ", " 4", "2").toString());
    assertEquals(expected("null", "null", "null", "null", "4", "2"),
	new BibliographicPeriod("", "", "", " ", "4 ", " 2").toString());
    assertEquals(expected("null", "null", "null", "1954", "null", "2"),
	new BibliographicPeriod("", "", "", " 1954 ", "", " 2").toString());
    assertEquals(expected("1954", "null", "null", "1988", "null", "null"),
	new BibliographicPeriod("1954", "", "", "1988", "", "").toString());
    assertEquals(expected("1954", "4", "null", "1988", "12", "null"),
	new BibliographicPeriod(" 1954", "4", "", "1988", "12", "").toString());
    assertEquals(expected("1954", "4", "2", "1988", "12", "28"),
	new BibliographicPeriod("1954 ", " 4", "2", "1988", "12", "28")
    .toString());
    assertEquals(expected("null", "4", "2", "null", "12", "28"),
	new BibliographicPeriod(" ", "4 ", " 2", "", "12", "28").toString());
    assertEquals(expected("1954", "null", "2", "1988", "null", "28"),
	new BibliographicPeriod(" 1954 ", "", " 2", "1988", "", "28")
    .toString());
    assertEquals(expected("-", "null", "null", "null", "null", "null"),
	new BibliographicPeriod("-", null, null, null, null, null).toString());
    assertEquals(expected("null", "-", "null", "null", "null", "null"),
	new BibliographicPeriod(null, "-", null, null, null, null).toString());
    assertEquals(expected("null", "null", "-", "null", "null", "null"),
	new BibliographicPeriod(null, null, "-", null, null, null).toString());
    assertEquals(expected("null", "null", "null", "-", "null", "null"),
	new BibliographicPeriod(null, null, null, "-", null, null).toString());
    assertEquals(expected("null", "null", "null", "null", "-", "null"),
	new BibliographicPeriod(null, null, null, null, "-", null).toString());
    assertEquals(expected("null", "null", "null", "null", "null", "-"),
	new BibliographicPeriod(null, null, null, null, null, "-").toString());
  }

  /**
   * Check the behavior of BibliographicPeriod(BibliographicPeriodEdge,
   * BibliographicPeriodEdge).
   */
  public final void testConstructor2BibliographicPeriodEdges() {
    BibliographicPeriodEdge nullEdge = null;

    assertEquals("BibliographicPeriod [startEdge=null, endEdge=null]",
	new BibliographicPeriod(nullEdge, nullEdge).toString());

    try {
      new BibliographicPeriod(nullEdge, new BibliographicPeriodEdge(""));
      fail("Should be illegal to create a period with one null edge");
    } catch (IllegalArgumentException iae) {
      // Expected.
    }

    try {
      new BibliographicPeriod(new BibliographicPeriodEdge(""), nullEdge);
      fail("Should be illegal to create a period with one null edge");
    } catch (IllegalArgumentException iae) {
      // Expected.
    }

    assertEquals(expected("null", "null", "null", "null", "null", "null"),
	new BibliographicPeriod(new BibliographicPeriodEdge(""),
	    new BibliographicPeriodEdge("")).toString());
    assertEquals(expected("null", "null", "null", "null", "null", "null"),
	new BibliographicPeriod(new BibliographicPeriodEdge(" "),
	    new BibliographicPeriodEdge(" ")).toString());
    assertEquals(expected("1954", "null", "null", "1954", "null", "null"),
	new BibliographicPeriod(new BibliographicPeriodEdge("1954"),
	    new BibliographicPeriodEdge("1954")).toString());
    assertEquals(expected("1954", "null", "null", "1954", "null", "null"),
	new BibliographicPeriod(new BibliographicPeriodEdge(" 1954"),
	    new BibliographicPeriodEdge("1954 ")).toString());
    assertEquals(expected("1954", "null", "null", "1954", "null", "null"),
	new BibliographicPeriod(new BibliographicPeriodEdge(" 1954 "),
	    new BibliographicPeriodEdge("1 9 5 4")).toString());
    assertEquals(expected("1954", "null", "null", "1988", "null", "null"),
	new BibliographicPeriod(new BibliographicPeriodEdge("1954"),
	    new BibliographicPeriodEdge("1988")).toString());
    assertEquals(expected("1954", "null", "null", "1988", "null", "null"),
	new BibliographicPeriod(new BibliographicPeriodEdge("1954 "),
	    new BibliographicPeriodEdge(" 1988")).toString());
    assertEquals(expected("1954", "null", "null", "1988", "null", "null"),
	new BibliographicPeriod(new BibliographicPeriodEdge("1 9 5 4"),
	    new BibliographicPeriodEdge(" 1 9 8 8 ")).toString());
    assertEquals(expected("-", "null", "null", "null", "null", "null"),
	new BibliographicPeriod(new BibliographicPeriodEdge("-"),
	    new BibliographicPeriodEdge("")).toString());
    assertEquals(expected("null", "null", "null", "-", "null", "null"),
	new BibliographicPeriod(new BibliographicPeriodEdge(""),
	    new BibliographicPeriodEdge("-")).toString());
  }

  /**
   * Check the behavior of createList().
   */
  public final void testCreateList() {
    assertEquals(0, BibliographicPeriod.createList(null).size());
    assertEquals(0, BibliographicPeriod.createList("").size());
    assertEquals(0, BibliographicPeriod.createList(" ").size());
    assertEquals(1, BibliographicPeriod.createList("-").size());
    assertEquals(1, BibliographicPeriod.createList(" -").size());
    assertEquals(1, BibliographicPeriod.createList("- ").size());
    assertEquals(1, BibliographicPeriod.createList(" - ").size());
    assertEquals(1, BibliographicPeriod.createList("1954").size());
    assertEquals(1, BibliographicPeriod.createList(" 1954").size());
    assertEquals(1, BibliographicPeriod.createList("1954 ").size());
    assertEquals(1, BibliographicPeriod.createList(" 1954 ").size());
    assertEquals(1, BibliographicPeriod.createList(" 1 9 5 4 ").size());
    assertEquals(1, BibliographicPeriod.createList("1954(4)").size());
    assertEquals(1, BibliographicPeriod.createList("1954(4)(2)").size());
    assertEquals(1, BibliographicPeriod.createList("(4)(2)").size());
    assertEquals(1, BibliographicPeriod.createList("1954()(2)").size());
    assertEquals(1, BibliographicPeriod.createList("1954-").size());
    assertEquals(1, BibliographicPeriod.createList("-1954").size());
    assertEquals(1, BibliographicPeriod.createList("1954-1954").size());
    assertEquals(1, BibliographicPeriod.createList("1954-1988").size());
    assertEquals(1,
	BibliographicPeriod.createList("1954(4)-1988(12)(28)").size());
    assertEquals(1, BibliographicPeriod.createList("(4)(2)-1988()(28)").size());
    assertEquals(0, BibliographicPeriod.createList(",").size());
    assertEquals(0, BibliographicPeriod.createList(" ,").size());
    assertEquals(0, BibliographicPeriod.createList(", ").size());
    assertEquals(0, BibliographicPeriod.createList(" , ").size());
    assertEquals(1, BibliographicPeriod.createList("-,").size());
    assertEquals(1, BibliographicPeriod.createList(",-").size());
    assertEquals(2, BibliographicPeriod.createList("-,1900").size());
    assertEquals(2, BibliographicPeriod.createList("1900,-").size());
    assertEquals(3, BibliographicPeriod.createList("1900,1954,1988").size());
  }

  /**
   * Check the behavior of rangesAsString and createList().
   */
  public final void testStringListStringConversion() {
    assertNull(rangesAsString(null));
    assertNull(rangesAsString(""));
    assertEquals("-", rangesAsString("-"));
    assertEquals("1954", rangesAsString("1954"));
    assertEquals("1954(4)", rangesAsString("1954(4)"));
    assertEquals("1954(4)(2)", rangesAsString("1954(4)(2)"));
    assertEquals("(4)(2)", rangesAsString("(4)(2)"));
    assertEquals("1954()(2)", rangesAsString("1954()(2)"));
    assertEquals("1900-", rangesAsString("1900-"));
    assertEquals("-2000", rangesAsString("-2000"));
    assertEquals("1900-2000", rangesAsString("1900-2000"));
    assertEquals("1900,1910,1920", rangesAsString("1900,1910,1920"));
    assertEquals("1920,1910,1900", rangesAsString("1920,1910,1900"));
  }

  private String rangesAsString(String ranges) {
    return BibliographicPeriod
	.rangesAsString(BibliographicPeriod.createList(ranges));
  }

  /**
   * Check the behavior of toDisplayableString().
   */
  public final void testToDisplayableString() {
    assertEquals("", new BibliographicPeriod(null).toDisplayableString());
    assertEquals("", new BibliographicPeriod("").toDisplayableString());
    assertEquals("", new BibliographicPeriod(" ").toDisplayableString());
    assertEquals("-", new BibliographicPeriod("-").toDisplayableString());
    assertEquals("-", new BibliographicPeriod(" -").toDisplayableString());
    assertEquals("-", new BibliographicPeriod("- ").toDisplayableString());
    assertEquals("-", new BibliographicPeriod(" - ").toDisplayableString());
    assertEquals("1954", new BibliographicPeriod("1954").toDisplayableString());
    assertEquals("1954",
	new BibliographicPeriod(" 1954").toDisplayableString());
    assertEquals("1954",
	new BibliographicPeriod("1954 ").toDisplayableString());
    assertEquals("1954",
	new BibliographicPeriod(" 1954 ").toDisplayableString());
    assertEquals("1954",
	new BibliographicPeriod(" 1 9 5 4 ").toDisplayableString());
    assertEquals("1954-",
	new BibliographicPeriod("1954-").toDisplayableString());
    assertEquals("1954-",
	new BibliographicPeriod(" 1954-").toDisplayableString());
    assertEquals("1954-",
	new BibliographicPeriod("1954 -").toDisplayableString());
    assertEquals("1954-",
	new BibliographicPeriod("1954- ").toDisplayableString());
    assertEquals("1954-",
	new BibliographicPeriod(" 1954 - ").toDisplayableString());
    assertEquals("1954-",
	new BibliographicPeriod(" 1 9 5 4 - ").toDisplayableString());
    assertEquals("-1954",
	new BibliographicPeriod("-1954").toDisplayableString());
    assertEquals("-1954",
	new BibliographicPeriod(" -1954").toDisplayableString());
    assertEquals("-1954",
	new BibliographicPeriod("- 1954").toDisplayableString());
    assertEquals("-1954",
	new BibliographicPeriod("-1954 ").toDisplayableString());
    assertEquals("-1954",
	new BibliographicPeriod(" - 1954 ").toDisplayableString());
    assertEquals("-1954",
	new BibliographicPeriod(" - 1 9 5 4 ").toDisplayableString());
    assertEquals("1954",
	new BibliographicPeriod("1954-1954").toDisplayableString());
    assertEquals("1954",
	new BibliographicPeriod(" 1954-1954").toDisplayableString());
    assertEquals("1954",
	new BibliographicPeriod("1954-1954 ").toDisplayableString());
    assertEquals("1954",
	new BibliographicPeriod(" 1954-1954 ").toDisplayableString());
    assertEquals("1954",
	new BibliographicPeriod(" 1 9 5 4-1954 ").toDisplayableString());
    assertEquals("1954",
	new BibliographicPeriod(" 1954-1 9 5 4 ").toDisplayableString());
    assertEquals("1954",
	new BibliographicPeriod(" 1 9 5 4 - 1 9 5 4 ").toDisplayableString());
    assertEquals("1954-1988",
	new BibliographicPeriod("1954-1988").toDisplayableString());
    assertEquals("1954-1988",
	new BibliographicPeriod(" 1954-1988").toDisplayableString());
    assertEquals("1954-1988",
	new BibliographicPeriod("1954-1988 ").toDisplayableString());
    assertEquals("1954-1988",
	new BibliographicPeriod(" 1954-1988 ").toDisplayableString());
    assertEquals("1954-1988",
	new BibliographicPeriod(" 1 9 5 4-1988 ").toDisplayableString());
    assertEquals("1954-1988",
	new BibliographicPeriod(" 1954-1 9 8 8 ").toDisplayableString());
    assertEquals("1954-1988",
	new BibliographicPeriod(" 1 9 5 4 - 1 9 8 8 ").toDisplayableString());
  }

  /**
   * Check the behavior of includesFullYears().
   */
  public final void testIncludesFullYears() {
    assertTrue(new BibliographicPeriod("").includesFullYears());
    assertTrue(new BibliographicPeriod(" ").includesFullYears());
    assertTrue(new BibliographicPeriod("-").includesFullYears());
    assertTrue(new BibliographicPeriod("1954-").includesFullYears());
    assertTrue(new BibliographicPeriod("-1988").includesFullYears());
    assertTrue(new BibliographicPeriod("1954").includesFullYears());
    assertTrue(new BibliographicPeriod("1954-1988").includesFullYears());
    assertFalse(new BibliographicPeriod("1954-1988(12)").includesFullYears());
    assertFalse(new BibliographicPeriod("1954-1988(12)(28)")
    .includesFullYears());
    assertFalse(new BibliographicPeriod("1954-(12)(28)").includesFullYears());
    assertFalse(new BibliographicPeriod("1954-(12)").includesFullYears());
    assertFalse(new BibliographicPeriod("1954-()(28)").includesFullYears());
    assertFalse(new BibliographicPeriod("1954(4)-1988").includesFullYears());
    assertFalse(new BibliographicPeriod("1954(4)(2)-1988").includesFullYears());
    assertFalse(new BibliographicPeriod("(12)(28)-1988").includesFullYears());
    assertFalse(new BibliographicPeriod("(12)-1988").includesFullYears());
    assertFalse(new BibliographicPeriod("()(28)-1988").includesFullYears());
    assertFalse(new BibliographicPeriod("1954(4)").includesFullYears());
    assertFalse(new BibliographicPeriod("1954(4)(2)").includesFullYears());
    assertFalse(new BibliographicPeriod("(12)(28)").includesFullYears());
    assertFalse(new BibliographicPeriod("(12)").includesFullYears());
    assertFalse(new BibliographicPeriod("()(28)").includesFullYears());
    assertFalse(new BibliographicPeriod("1954(4)-1988(12)(28)")
    .includesFullYears());
    assertFalse(new BibliographicPeriod("1954(4)(2)-1988(12)")
    .includesFullYears());
    assertFalse(new BibliographicPeriod("(4)-(12)(28)").includesFullYears());
    assertFalse(new BibliographicPeriod("(4)(2)-(12)").includesFullYears());
    assertFalse(new BibliographicPeriod("()(2)-()(28)").includesFullYears());
  }

  /**
   * Check the behavior of isAllTime().
   */
  public final void testIsAllTime() {
    String nullString = null;

    assertFalse(new BibliographicPeriod(null).isAllTime());
    assertFalse(new BibliographicPeriod("").isAllTime());
    assertFalse(new BibliographicPeriod(" ").isAllTime());
    assertTrue(new BibliographicPeriod("-").isAllTime());
    assertTrue(new BibliographicPeriod(" -").isAllTime());
    assertTrue(new BibliographicPeriod("- ").isAllTime());
    assertTrue(new BibliographicPeriod(" - ").isAllTime());
    assertFalse(new BibliographicPeriod("1954").isAllTime());
    assertFalse(new BibliographicPeriod(" 1954").isAllTime());
    assertFalse(new BibliographicPeriod("1954 ").isAllTime());
    assertFalse(new BibliographicPeriod(" 1954 ").isAllTime());
    assertFalse(new BibliographicPeriod("19 54").isAllTime());
    assertFalse(new BibliographicPeriod("1 9 5 4").isAllTime());
    assertFalse(new BibliographicPeriod(" 1 9 5 4 ").isAllTime());
    assertFalse(new BibliographicPeriod("1954-").isAllTime());
    assertFalse(new BibliographicPeriod("-1954").isAllTime());
    assertFalse(new BibliographicPeriod("1954-1954").isAllTime());
    assertFalse(new BibliographicPeriod("1954-1988").isAllTime());
    assertTrue(new BibliographicPeriod(new BibliographicPeriodEdge(nullString),
	new BibliographicPeriodEdge(nullString)).isAllTime());
    assertTrue(new BibliographicPeriod(new BibliographicPeriodEdge(""),
	new BibliographicPeriodEdge("")).isAllTime());
    assertTrue(new BibliographicPeriod(new BibliographicPeriodEdge(" "),
	new BibliographicPeriodEdge(" ")).isAllTime());
    assertFalse(new BibliographicPeriod(new BibliographicPeriodEdge("1954"),
	new BibliographicPeriodEdge("1954")).isAllTime());
    assertFalse(new BibliographicPeriod(new BibliographicPeriodEdge(" 1954"),
	new BibliographicPeriodEdge("1954 ")).isAllTime());
    assertFalse(new BibliographicPeriod(new BibliographicPeriodEdge(" 1954 "),
	new BibliographicPeriodEdge("1 9 5 4")).isAllTime());
    assertFalse(new BibliographicPeriod(new BibliographicPeriodEdge("1954"),
	new BibliographicPeriodEdge("1988")).isAllTime());
    assertFalse(new BibliographicPeriod(new BibliographicPeriodEdge("1954 "),
	new BibliographicPeriodEdge(" 1988")).isAllTime());
    assertFalse(new BibliographicPeriod(new BibliographicPeriodEdge("1 9 5 4"),
	new BibliographicPeriodEdge(" 1 9 8 8 ")).isAllTime());
    assertFalse(new BibliographicPeriod(null, null, null, null, null, null)
    .isAllTime());
    assertFalse(new BibliographicPeriod(null, "", null, "", null, "")
    .isAllTime());
    assertFalse(new BibliographicPeriod(null, " ", null, " ", null, " ")
    .isAllTime());
    assertFalse(new BibliographicPeriod("", null, "", null, "", null)
    .isAllTime());
    assertFalse(new BibliographicPeriod(" ", null, " ", null, " ", null)
    .isAllTime());
    assertFalse(new BibliographicPeriod("", "", "", "", "", "").isAllTime());
    assertFalse(new BibliographicPeriod(" ", " ", " ", " ", " ", " ")
    .isAllTime());
    assertFalse(new BibliographicPeriod("1954", "", "", "", "", "")
    .isAllTime());
    assertFalse(new BibliographicPeriod(" 1954", "4", "", "", "", "")
    .isAllTime());
    assertFalse(new BibliographicPeriod("1954 ", " 4", "2", "", "", "")
    .isAllTime());
    assertFalse(new BibliographicPeriod(" ", "4 ", " 2", "", "", "")
    .isAllTime());
    assertFalse(new BibliographicPeriod(" 1954 ", "", " 2", "", "", "")
    .isAllTime());
    assertFalse(new BibliographicPeriod("", "", "", "1954", "", "")
    .isAllTime());
    assertFalse(new BibliographicPeriod("", "", "", " 1954", "4", "")
    .isAllTime());
    assertFalse(new BibliographicPeriod("", "", "", "1954 ", " 4", "2")
    .isAllTime());
    assertFalse(new BibliographicPeriod("", "", "", " ", "4 ", " 2")
    .isAllTime());
    assertFalse(new BibliographicPeriod("", "", "", " 1954 ", "", " 2")
    .isAllTime());
    assertFalse(new BibliographicPeriod("1954", "", "", "1988", "", "")
    .isAllTime());
    assertFalse(new BibliographicPeriod(" 1954", "4", "", "1988", "12", "")
    .isAllTime());
    assertFalse(new BibliographicPeriod("1954 ", " 4", "2", "1988", "12", "28")
    .isAllTime());
    assertFalse(new BibliographicPeriod(" ", "4 ", " 2", "", "12", "28")
    .isAllTime());
    assertFalse(new BibliographicPeriod(" 1954 ", "", " 2", "1988", "", "28")
    .isAllTime());
  }

  /**
   * Check the behavior of isEmpty().
   */
  public final void testIsEmpty() {
    BibliographicPeriodEdge nullEdge = null;
    String nullString = null;

    assertTrue(new BibliographicPeriod(null).isEmpty());
    assertTrue(new BibliographicPeriod("").isEmpty());
    assertTrue(new BibliographicPeriod(" ").isEmpty());
    assertTrue(new BibliographicPeriod(null, null, null, null, null, null)
    .isEmpty());
    assertTrue(new BibliographicPeriod(null, "", null, "", null, "").isEmpty());
    assertTrue(new BibliographicPeriod(null, " ", null, " ", null, " ")
    .isEmpty());
    assertTrue(new BibliographicPeriod("", null, "", null, "", null).isEmpty());
    assertTrue(new BibliographicPeriod(" ", null, " ", null, " ", null)
    .isEmpty());
    assertTrue(new BibliographicPeriod("", "", "", "", "", "").isEmpty());
    assertTrue(new BibliographicPeriod(" ", " ", " ", " ", " ", " ").isEmpty());
    assertTrue(new BibliographicPeriod(nullEdge, nullEdge).isEmpty());
    assertFalse(new BibliographicPeriod("-").isEmpty());
    assertFalse(new BibliographicPeriod(" -").isEmpty());
    assertFalse(new BibliographicPeriod("- ").isEmpty());
    assertFalse(new BibliographicPeriod(" - ").isEmpty());
    assertFalse(new BibliographicPeriod("1954").isEmpty());
    assertFalse(new BibliographicPeriod(" 1954").isEmpty());
    assertFalse(new BibliographicPeriod("1954 ").isEmpty());
    assertFalse(new BibliographicPeriod(" 1954 ").isEmpty());
    assertFalse(new BibliographicPeriod("19 54").isEmpty());
    assertFalse(new BibliographicPeriod("1 9 5 4").isEmpty());
    assertFalse(new BibliographicPeriod(" 1 9 5 4 ").isEmpty());
    assertFalse(new BibliographicPeriod("1954-").isEmpty());
    assertFalse(new BibliographicPeriod("-1954").isEmpty());
    assertFalse(new BibliographicPeriod("1954-1954").isEmpty());
    assertFalse(new BibliographicPeriod("1954-1988").isEmpty());
    assertFalse(new BibliographicPeriod(new BibliographicPeriodEdge(nullString),
	new BibliographicPeriodEdge(nullString)).isEmpty());
    assertFalse(new BibliographicPeriod(new BibliographicPeriodEdge(""),
	new BibliographicPeriodEdge("")).isEmpty());
    assertFalse(new BibliographicPeriod(new BibliographicPeriodEdge(" "),
	new BibliographicPeriodEdge(" ")).isEmpty());
    assertFalse(new BibliographicPeriod(new BibliographicPeriodEdge("1954"),
	new BibliographicPeriodEdge("")).isEmpty());
    assertFalse(new BibliographicPeriod("1954", null, null, null, null, null)
    .isEmpty());
    assertFalse(new BibliographicPeriod("1954", "", "", "", "", "").isEmpty());
    assertFalse(new BibliographicPeriod("1954", " ", " ", " ", " ", " ")
    .isEmpty());
    assertFalse(new BibliographicPeriod(" 1954", "4", "", "", "", "")
    .isEmpty());
    assertFalse(new BibliographicPeriod("1954 ", " 4", "2", "", "", "")
    .isEmpty());
    assertFalse(new BibliographicPeriod(" ", "4 ", " 2", "", "", "").isEmpty());
    assertFalse(new BibliographicPeriod(" 1954 ", "", " 2", "", "", "")
    .isEmpty());
    assertFalse(new BibliographicPeriod("", "", "", "1954", "", "").isEmpty());
    assertFalse(new BibliographicPeriod("", "", "", " 1954", "4", "")
    .isEmpty());
    assertFalse(new BibliographicPeriod("", "", "", "1954 ", " 4", "2")
    .isEmpty());
    assertFalse(new BibliographicPeriod("", "", "", " ", "4 ", " 2").isEmpty());
    assertFalse(new BibliographicPeriod("", "", "", " 1954 ", "", " 2")
    .isEmpty());
    assertFalse(new BibliographicPeriod("1954", "", "", "1988", "", "")
    .isEmpty());
    assertFalse(new BibliographicPeriod(" 1954", "4", "", "1988", "12", "")
    .isEmpty());
    assertFalse(new BibliographicPeriod("1954 ", " 4", "2", "1988", "12", "28")
    .isEmpty());
    assertFalse(new BibliographicPeriod(" ", "4 ", " 2", "", "12", "28")
    .isEmpty());
    assertFalse(new BibliographicPeriod(" 1954 ", "", " 2", "1988", "", "28")
    .isEmpty());
  }

  /**
   * Check the behavior of matches().
   */
  public final void testMatches() {
    List<BibliographicPeriod> ranges =
	Arrays.asList(new BibliographicPeriod("1954(4)(2)"),
	    new BibliographicPeriod(new BibliographicPeriodEdge("1954(4)(4)"),
		new BibliographicPeriodEdge("1954(4)(6)")),
	    new BibliographicPeriod(new BibliographicPeriodEdge("1988(12)(28)"),
		new BibliographicPeriodEdge("1988(12)(28)")));

    assertTrue(new BibliographicPeriod("-").matches(ranges));
    assertFalse(new BibliographicPeriod("1900-1953").matches(ranges));
    assertTrue(new BibliographicPeriod("1953-").matches(ranges));
    assertFalse(new BibliographicPeriod("-1953").matches(ranges));
    assertFalse(new BibliographicPeriod("1953").matches(ranges));
    assertTrue(new BibliographicPeriod("1953-1954").matches(ranges));
    assertTrue(new BibliographicPeriod("1953-1955").matches(ranges));
    assertTrue(new BibliographicPeriod("1953-1987").matches(ranges));
    assertTrue(new BibliographicPeriod("1953-1988").matches(ranges));
    assertTrue(new BibliographicPeriod("1953-1989").matches(ranges));
    assertTrue(new BibliographicPeriod("1954-").matches(ranges));
    assertTrue(new BibliographicPeriod("-1954").matches(ranges));
    assertTrue(new BibliographicPeriod("1954").matches(ranges));
    assertTrue(new BibliographicPeriod("1954-1955").matches(ranges));
    assertTrue(new BibliographicPeriod("1954-1987").matches(ranges));
    assertTrue(new BibliographicPeriod("1954-1988").matches(ranges));
    assertTrue(new BibliographicPeriod("1954-1989").matches(ranges));
    assertTrue(new BibliographicPeriod("1955-").matches(ranges));
    assertTrue(new BibliographicPeriod("-1955").matches(ranges));
    assertFalse(new BibliographicPeriod("1955").matches(ranges));
    assertFalse(new BibliographicPeriod("1955-1987").matches(ranges));
    assertTrue(new BibliographicPeriod("1955-1988").matches(ranges));
    assertTrue(new BibliographicPeriod("1955-1989").matches(ranges));
    assertTrue(new BibliographicPeriod("1987-").matches(ranges));
    assertTrue(new BibliographicPeriod("-1987").matches(ranges));
    assertFalse(new BibliographicPeriod("1987").matches(ranges));
    assertTrue(new BibliographicPeriod("1987-1988").matches(ranges));
    assertTrue(new BibliographicPeriod("1987-1989").matches(ranges));
    assertTrue(new BibliographicPeriod("1988-").matches(ranges));
    assertTrue(new BibliographicPeriod("-1988").matches(ranges));
    assertTrue(new BibliographicPeriod("1988").matches(ranges));
    assertTrue(new BibliographicPeriod("1988-1989").matches(ranges));
    assertFalse(new BibliographicPeriod("1989-").matches(ranges));
    assertTrue(new BibliographicPeriod("-1989").matches(ranges));
    assertFalse(new BibliographicPeriod("1989").matches(ranges));
    assertFalse(new BibliographicPeriod("1989-2000").matches(ranges));

    assertFalse(new BibliographicPeriod("1953-1954(4)(3)").matches(ranges));
    assertTrue(new BibliographicPeriod("1953-1954(4)(4)").matches(ranges));
    assertFalse(new BibliographicPeriod("1953(4)(2)").matches(ranges));
    assertFalse(new BibliographicPeriod("1954(4)").matches(ranges));
    assertFalse(new BibliographicPeriod("1954()(2)").matches(ranges));
    assertFalse(new BibliographicPeriod("1954()(4)").matches(ranges));
    assertFalse(new BibliographicPeriod("1954()(6)").matches(ranges));
    assertFalse(new BibliographicPeriod("1954(4)(1)").matches(ranges));
    assertTrue(new BibliographicPeriod("1954(4)(2)").matches(ranges));
    assertTrue(new BibliographicPeriod("1954(4)(2)-1954(4)(3)")
    .matches(ranges));
    assertFalse(new BibliographicPeriod("1954(4)(3)").matches(ranges));
    assertTrue(new BibliographicPeriod("1954(4)(3)-1954(4)(4)")
    .matches(ranges));
    assertFalse(new BibliographicPeriod("1954(4)(3)-1954(4)(5)")
    .matches(ranges));
    assertTrue(new BibliographicPeriod("1954(4)(3)-1954(4)(6)")
    .matches(ranges));
    assertTrue(new BibliographicPeriod("1954(4)(4)").matches(ranges));
    assertFalse(new BibliographicPeriod("1954(4)(5)").matches(ranges));
    assertTrue(new BibliographicPeriod("1954(4)(6)").matches(ranges));
    assertTrue(new BibliographicPeriod("1954(4)(4)-1954(4)(6)")
    .matches(ranges));
    assertFalse(new BibliographicPeriod("1954(4)(7)").matches(ranges));
    assertFalse(new BibliographicPeriod("1954(5)()").matches(ranges));
    assertFalse(new BibliographicPeriod("1955(4)(2)").matches(ranges));
    assertFalse(new BibliographicPeriod("1955-1988(12)(27)").matches(ranges));
    assertFalse(new BibliographicPeriod("1987(12)(28)").matches(ranges));
    assertFalse(new BibliographicPeriod("1988(11)(28)").matches(ranges));
    assertFalse(new BibliographicPeriod("1988(12)").matches(ranges));
    assertFalse(new BibliographicPeriod("1988()(28)").matches(ranges));
    assertFalse(new BibliographicPeriod("1988(12)(27)").matches(ranges));
    assertTrue(new BibliographicPeriod("1988(12)(28)").matches(ranges));
    assertFalse(new BibliographicPeriod("1988(12)(29)").matches(ranges));
    assertFalse(new BibliographicPeriod("1988(13)(1)").matches(ranges));
    assertFalse(new BibliographicPeriod("1989(12)(28)").matches(ranges));
    assertFalse(new BibliographicPeriod("(4)").matches(ranges));
    assertFalse(new BibliographicPeriod("(4)(2)").matches(ranges));
    assertFalse(new BibliographicPeriod("(4)(4)").matches(ranges));
    assertFalse(new BibliographicPeriod("(4)(6)").matches(ranges));
    assertFalse(new BibliographicPeriod("()(2)").matches(ranges));
    assertFalse(new BibliographicPeriod("()(4)").matches(ranges));
    assertFalse(new BibliographicPeriod("()(6)").matches(ranges));
    assertFalse(new BibliographicPeriod("(12)").matches(ranges));
    assertFalse(new BibliographicPeriod("(12)(28)").matches(ranges));
    assertFalse(new BibliographicPeriod("()(28)").matches(ranges));
  }
}
