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
 * Test class for org.lockss.subscription.BibliographicPeriodEdge.
 * 
 * @author Fernando Garcia-Loygorri
 * @version 1.0
 */
package org.lockss.subscription;

import java.util.Arrays;
import java.util.List;
import org.lockss.test.LockssTestCase;

public class TestBibliographicPeriodEdge extends LockssTestCase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  /**
   * Check the behavior of BibliographicPeriodEdge(String).
   */
  public final void testConstructor1String() {
    assertEquals("BibliographicPeriodEdge [year=null, volume=null, issue=null]",
	new BibliographicPeriodEdge(null).toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=null, issue=null]",
	new BibliographicPeriodEdge("").toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=null, issue=null]",
	new BibliographicPeriodEdge(" ").toString());
    assertEquals("BibliographicPeriodEdge [year=1954, volume=null, issue=null]",
	new BibliographicPeriodEdge("1954").toString());
    assertEquals("BibliographicPeriodEdge [year=1954, volume=null, issue=null]",
	new BibliographicPeriodEdge(" 1954").toString());
    assertEquals("BibliographicPeriodEdge [year=1954, volume=null, issue=null]",
	new BibliographicPeriodEdge("1954 ").toString());
    assertEquals("BibliographicPeriodEdge [year=1954, volume=null, issue=null]",
	new BibliographicPeriodEdge(" 1954 ").toString());
    assertEquals("BibliographicPeriodEdge [year=1954, volume=null, issue=null]",
	new BibliographicPeriodEdge("19 54").toString());
    assertEquals("BibliographicPeriodEdge [year=1954, volume=null, issue=null]",
	new BibliographicPeriodEdge("1 9 5 4").toString());
    assertEquals("BibliographicPeriodEdge [year=1954, volume=null, issue=null]",
	new BibliographicPeriodEdge(" 1 9 5 4 ").toString());
    assertEquals("BibliographicPeriodEdge [year=1954, volume=4, issue=null]",
	new BibliographicPeriodEdge("1954(4)").toString());
    assertEquals("BibliographicPeriodEdge [year=1954, volume=4, issue=2]",
	new BibliographicPeriodEdge("1954(4)(2)").toString());
    assertEquals("BibliographicPeriodEdge [year=1954, volume=null, issue=2]",
	new BibliographicPeriodEdge("1954()(2)").toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=null, issue=2]",
	new BibliographicPeriodEdge("()(2)").toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=null, issue=2]",
	new BibliographicPeriodEdge("( )(2)").toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=4, issue=2]",
	new BibliographicPeriodEdge("(4)(2)").toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=4, issue=null]",
	new BibliographicPeriodEdge("(4)").toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=4, issue=null]",
	new BibliographicPeriodEdge("( 4)").toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=4, issue=null]",
	new BibliographicPeriodEdge("(4 )").toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=4, issue=null]",
	new BibliographicPeriodEdge("( 4 )").toString());

    String message =
      "Should be illegal to create a period edge with unbalanced parentheses";

    try {
      new BibliographicPeriodEdge("(");
      fail(message);
    } catch (Exception e) {
      // Expected.
    }

    try {
      new BibliographicPeriodEdge(")");
      fail(message);
    } catch (Exception e) {
      // Expected.
    }

    try {
      new BibliographicPeriodEdge("()(");
      fail(message);
    } catch (Exception e) {
      // Expected.
    }

    try {
      new BibliographicPeriodEdge(")()");
      fail(message);
    } catch (Exception e) {
      // Expected.
    }
  }

  /**
   * Check the behavior of BibliographicPeriodEdge(String, String, String).
   */
  public final void testConstructor3Strings() {
    assertEquals("BibliographicPeriodEdge [year=null, volume=null, issue=null]",
	new BibliographicPeriodEdge(null, null, null).toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=null, issue=null]",
	new BibliographicPeriodEdge("", null, null).toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=null, issue=null]",
	new BibliographicPeriodEdge(" ", null, null).toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=null, issue=null]",
	new BibliographicPeriodEdge(null, "", null).toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=null, issue=null]",
	new BibliographicPeriodEdge(null, " ", null).toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=null, issue=null]",
	new BibliographicPeriodEdge(null, null, "").toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=null, issue=null]",
	new BibliographicPeriodEdge(null, null, " ").toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=null, issue=null]",
	new BibliographicPeriodEdge("", " ", null).toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=null, issue=null]",
	new BibliographicPeriodEdge(" ", "", null).toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=null, issue=null]",
	new BibliographicPeriodEdge(null, "", " ").toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=null, issue=null]",
	new BibliographicPeriodEdge(null, " ", "").toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=null, issue=null]",
	new BibliographicPeriodEdge(" ", null, "").toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=null, issue=null]",
	new BibliographicPeriodEdge("", null, " ").toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=null, issue=null]",
	new BibliographicPeriodEdge("", "", "").toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=null, issue=null]",
	new BibliographicPeriodEdge(" ", " ", " ").toString());
    assertEquals("BibliographicPeriodEdge [year=1954, volume=null, issue=null]",
	new BibliographicPeriodEdge("1954", "", "").toString());
    assertEquals("BibliographicPeriodEdge [year=1954, volume=4, issue=null]",
	new BibliographicPeriodEdge(" 1954", "4", "").toString());
    assertEquals("BibliographicPeriodEdge [year=1954, volume=4, issue=2]",
	new BibliographicPeriodEdge("1954 ", " 4", "2").toString());
    assertEquals("BibliographicPeriodEdge [year=1954, volume=null, issue=2]",
	new BibliographicPeriodEdge(" 1954 ", "", " 2").toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=4, issue=2]",
	new BibliographicPeriodEdge(" ", "4 ", "2 ").toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=4, issue=null]",
	new BibliographicPeriodEdge(" ", " 4 ", "").toString());
    assertEquals("BibliographicPeriodEdge [year=null, volume=null, issue=2]",
	new BibliographicPeriodEdge("", " ", " 2 ").toString());
  }

  /**
   * Check the behavior of isInfinity().
   */
  public final void testIsInfinity() {
    assertTrue(new BibliographicPeriodEdge(null).isInfinity());
    assertTrue(new BibliographicPeriodEdge("").isInfinity());
    assertTrue(new BibliographicPeriodEdge(" ").isInfinity());
    assertFalse(new BibliographicPeriodEdge("1954").isInfinity());
    assertFalse(new BibliographicPeriodEdge("1954(4)").isInfinity());
    assertFalse(new BibliographicPeriodEdge("1954()(2)").isInfinity());
    assertFalse(new BibliographicPeriodEdge("1954(4)(2)").isInfinity());
    assertFalse(new BibliographicPeriodEdge("(12)(28)").isInfinity());
    assertFalse(new BibliographicPeriodEdge("(12)").isInfinity());
    assertFalse(new BibliographicPeriodEdge("()(28)").isInfinity());
    assertTrue(new BibliographicPeriodEdge(null, null, null).isInfinity());
    assertTrue(new BibliographicPeriodEdge(null, null, "").isInfinity());
    assertTrue(new BibliographicPeriodEdge(null, null, " ").isInfinity());
    assertTrue(new BibliographicPeriodEdge(null, "", null).isInfinity());
    assertTrue(new BibliographicPeriodEdge(null, " ", null).isInfinity());
    assertTrue(new BibliographicPeriodEdge("", null, null).isInfinity());
    assertTrue(new BibliographicPeriodEdge(" ", null, null).isInfinity());
    assertTrue(new BibliographicPeriodEdge(null, "", "").isInfinity());
    assertTrue(new BibliographicPeriodEdge("", null, "").isInfinity());
    assertTrue(new BibliographicPeriodEdge("", "", null).isInfinity());
    assertTrue(new BibliographicPeriodEdge("", "", "").isInfinity());
    assertFalse(new BibliographicPeriodEdge("1954", null, null).isInfinity());
    assertFalse(new BibliographicPeriodEdge("1954", "4", null).isInfinity());
    assertFalse(new BibliographicPeriodEdge("1954", null, "2").isInfinity());
    assertFalse(new BibliographicPeriodEdge("1954", "4", "2").isInfinity());
    assertFalse(new BibliographicPeriodEdge(null, "4", null).isInfinity());
    assertFalse(new BibliographicPeriodEdge(null, "4", "2").isInfinity());
    assertFalse(new BibliographicPeriodEdge(null, null, "2").isInfinity());
  }

  /**
   * Check the behavior of extractEdgeYear().
   */
  public final void testExtractEdgeYear() {
    assertNull(BibliographicPeriodEdge.extractEdgeYear(null));
    assertNull(BibliographicPeriodEdge.extractEdgeYear(""));
    assertNull(BibliographicPeriodEdge.extractEdgeYear(" "));
    assertNull(BibliographicPeriodEdge.extractEdgeYear("()(1)"));
    assertNull(BibliographicPeriodEdge.extractEdgeYear("()(10)"));
    assertNull(BibliographicPeriodEdge.extractEdgeYear(" ()(1)"));
    assertNull(BibliographicPeriodEdge.extractEdgeYear("\"\"()(1)"));
    assertNull(BibliographicPeriodEdge.extractEdgeYear("(1)"));
    assertNull(BibliographicPeriodEdge.extractEdgeYear("(10)"));
    assertNull(BibliographicPeriodEdge.extractEdgeYear("(1)(2)"));
    assertNull(BibliographicPeriodEdge.extractEdgeYear("(1)(10)"));
    assertNull(BibliographicPeriodEdge.extractEdgeYear("(10)(1)"));
    assertNull(BibliographicPeriodEdge.extractEdgeYear("(10)(11)"));
    assertEquals("1999", BibliographicPeriodEdge.extractEdgeYear("1999"));
    assertEquals("1999", BibliographicPeriodEdge.extractEdgeYear("\"1999\""));
    assertEquals("1999", BibliographicPeriodEdge.extractEdgeYear(" \"1999\" "));
    assertEquals("1999", BibliographicPeriodEdge.extractEdgeYear("\" 1999 \""));
    assertEquals("1999", BibliographicPeriodEdge.extractEdgeYear(" \" 1999\""));
    assertEquals("1999", BibliographicPeriodEdge.extractEdgeYear("1999(2)"));
    assertEquals("1999", BibliographicPeriodEdge.extractEdgeYear("1999(12)"));
    assertEquals("1999", BibliographicPeriodEdge.extractEdgeYear("1999()(1)"));
    assertEquals("1999", BibliographicPeriodEdge.extractEdgeYear("1999()(12)"));
    assertEquals("1999", BibliographicPeriodEdge.extractEdgeYear("1999(2)(5)"));
    assertEquals("1999",
	BibliographicPeriodEdge.extractEdgeYear("1999(2)(15)"));
    assertEquals("1999",
	BibliographicPeriodEdge.extractEdgeYear("1999(12)(5)"));
    assertEquals("1999",
	BibliographicPeriodEdge.extractEdgeYear("1999(12)(15)"));
    assertNull(BibliographicPeriodEdge.extractEdgeYear("\"\""));
    assertNull(BibliographicPeriodEdge.extractEdgeYear("\"\""));
    assertEquals("-", BibliographicPeriodEdge.extractEdgeYear("-"));
    assertEquals("-", BibliographicPeriodEdge.extractEdgeYear("\"-\""));
    assertEquals("1954-A", BibliographicPeriodEdge.extractEdgeYear("1954-A"));
    assertEquals("1954-A",
	BibliographicPeriodEdge.extractEdgeYear("\"1954-A\""));
  }

  /**
   * Check the behavior of extractEdgeVolume().
   */
  public final void testExtractEdgeVolume() {
    assertNull(BibliographicPeriodEdge.extractEdgeVolume(null));
    assertNull(BibliographicPeriodEdge.extractEdgeVolume(""));
    assertNull(BibliographicPeriodEdge.extractEdgeVolume(" "));
    assertNull(BibliographicPeriodEdge.extractEdgeVolume("()(1)"));
    assertNull(BibliographicPeriodEdge.extractEdgeVolume("()(10)"));
    assertNull(BibliographicPeriodEdge.extractEdgeVolume("( )(1)"));
    assertNull(BibliographicPeriodEdge.extractEdgeVolume("(\"\")(1)"));
    assertEquals("1", BibliographicPeriodEdge.extractEdgeVolume("(1)"));
    assertEquals("10", BibliographicPeriodEdge.extractEdgeVolume("(10)"));
    assertEquals("1", BibliographicPeriodEdge.extractEdgeVolume("(1)"));
    assertEquals("1", BibliographicPeriodEdge.extractEdgeVolume("(\"1\")(2)"));
    assertEquals("1", BibliographicPeriodEdge.extractEdgeVolume("(1)(10)"));
    assertEquals("10", BibliographicPeriodEdge.extractEdgeVolume("(10)(1)"));
    assertEquals("10", BibliographicPeriodEdge.extractEdgeVolume("(10)(11)"));
    assertEquals("10",
	BibliographicPeriodEdge.extractEdgeVolume("(\"10\")(11)"));
    assertNull(BibliographicPeriodEdge.extractEdgeVolume("1999"));
    assertEquals("2", BibliographicPeriodEdge.extractEdgeVolume("1999(2)"));
    assertEquals("12", BibliographicPeriodEdge.extractEdgeVolume("1999(12)"));
    assertNull(BibliographicPeriodEdge.extractEdgeVolume("1999()(2)"));
    assertNull(BibliographicPeriodEdge.extractEdgeVolume("1999()(12)"));
    assertEquals("2", BibliographicPeriodEdge.extractEdgeVolume("1999(2)(5)"));
    assertEquals("2", BibliographicPeriodEdge.extractEdgeVolume("1999(2)(15)"));
    assertEquals("12",
	BibliographicPeriodEdge.extractEdgeVolume("1999(12)(5)"));
    assertEquals("12",
	BibliographicPeriodEdge.extractEdgeVolume("1999(12)(15)"));
    assertEquals("-", BibliographicPeriodEdge.extractEdgeVolume("(-)"));
    assertEquals("-", BibliographicPeriodEdge.extractEdgeVolume("(\"-\")"));
    assertEquals("1-A", BibliographicPeriodEdge.extractEdgeVolume("(1-A)"));
    assertEquals("1-A", BibliographicPeriodEdge.extractEdgeVolume("(\"1-A\")"));
  }

  /**
   * Check the behavior of extractEdgeIssue().
   */
  public final void testExtractEdgeIssue() {
    assertNull(BibliographicPeriodEdge.extractEdgeIssue(null));
    assertNull(BibliographicPeriodEdge.extractEdgeIssue(""));
    assertNull(BibliographicPeriodEdge.extractEdgeIssue(" "));
    assertEquals("1", BibliographicPeriodEdge.extractEdgeIssue("()(1)"));
    assertEquals("10", BibliographicPeriodEdge.extractEdgeIssue("()(10)"));
    assertEquals("10", BibliographicPeriodEdge.extractEdgeIssue("()( 10 )"));
    assertEquals("10", BibliographicPeriodEdge.extractEdgeIssue("()(\"10\")"));
    assertNull(BibliographicPeriodEdge.extractEdgeIssue("(1)"));
    assertNull(BibliographicPeriodEdge.extractEdgeIssue("(10)"));
    assertEquals("2", BibliographicPeriodEdge.extractEdgeIssue("(1)(2)"));
    assertEquals("10", BibliographicPeriodEdge.extractEdgeIssue("(1)(10)"));
    assertEquals("1", BibliographicPeriodEdge.extractEdgeIssue("(10)(1)"));
    assertEquals("10", BibliographicPeriodEdge.extractEdgeIssue("(11)(10)"));
    assertNull(BibliographicPeriodEdge.extractEdgeIssue("1999"));
    assertNull(BibliographicPeriodEdge.extractEdgeIssue("1999(2)"));
    assertNull(BibliographicPeriodEdge.extractEdgeIssue("1999(12)"));
    assertEquals("1", BibliographicPeriodEdge.extractEdgeIssue("1999()(1)"));
    assertEquals("12", BibliographicPeriodEdge.extractEdgeIssue("1999()(12)"));
    assertEquals("5", BibliographicPeriodEdge.extractEdgeIssue("1999(1)(5)"));
    assertEquals("15", BibliographicPeriodEdge.extractEdgeIssue("1999(1)(15)"));
    assertEquals("5", BibliographicPeriodEdge.extractEdgeIssue("1999(12)(5)"));
    assertEquals("15",
	BibliographicPeriodEdge.extractEdgeIssue("1999(12)(15)"));
    assertEquals("-", BibliographicPeriodEdge.extractEdgeIssue("()(-)"));
    assertEquals("-", BibliographicPeriodEdge.extractEdgeIssue("()(\"-\")"));
    assertEquals("1-A", BibliographicPeriodEdge.extractEdgeIssue("()(1-A)"));
    assertEquals("1-A",
	BibliographicPeriodEdge.extractEdgeIssue("()(\"1-A\")"));
  }

  /**
   * Check the behavior of toDisplayableString().
   */
  public final void testToDisplayableString() {
    assertEquals("",
	new BibliographicPeriodEdge(null, null, null).toDisplayableString());
    assertEquals("",
	new BibliographicPeriodEdge("", null, null).toDisplayableString());
    assertEquals("",
	new BibliographicPeriodEdge(null, "", null).toDisplayableString());
    assertEquals("",
	new BibliographicPeriodEdge(null, null, "").toDisplayableString());
    assertEquals("",
	new BibliographicPeriodEdge("", "", null).toDisplayableString());
    assertEquals("",
	new BibliographicPeriodEdge(null, "", "").toDisplayableString());
    assertEquals("",
	new BibliographicPeriodEdge("", null, "").toDisplayableString());
    assertEquals("1900",
	new BibliographicPeriodEdge("1900", null, null).toDisplayableString());
    assertEquals("1900",
	new BibliographicPeriodEdge("1900", "", null).toDisplayableString());
    assertEquals("1900",
	new BibliographicPeriodEdge("1900", null, "").toDisplayableString());
    assertEquals("1900",
	new BibliographicPeriodEdge("1900", "", "").toDisplayableString());
    assertEquals("1900(1)",
	new BibliographicPeriodEdge("1900", "1", null).toDisplayableString());
    assertEquals("1900(1)",
	new BibliographicPeriodEdge("1900", "1", "").toDisplayableString());
    assertEquals("1900(11)",
	new BibliographicPeriodEdge("1900", "11", null).toDisplayableString());
    assertEquals("1900(11)",
	new BibliographicPeriodEdge("1900", "11", "").toDisplayableString());
    assertEquals("1900(2)(3)",
	new BibliographicPeriodEdge("1900", "2", "3").toDisplayableString());
    assertEquals("1900(2)(10)",
	new BibliographicPeriodEdge("1900", "2", "10").toDisplayableString());
    assertEquals("1900(11)(2)",
	new BibliographicPeriodEdge("1900", "11", "2").toDisplayableString());
    assertEquals("1900(11)(12)",
	new BibliographicPeriodEdge("1900", "11", "12").toDisplayableString());
    assertEquals("1900()(4)",
	new BibliographicPeriodEdge("1900", null, "4").toDisplayableString());
    assertEquals("1900()(4)",
	new BibliographicPeriodEdge("1900", "", "4").toDisplayableString());
    assertEquals("1900()(11)",
	new BibliographicPeriodEdge("1900", null, "11").toDisplayableString());
    assertEquals("1900()(11)",
	new BibliographicPeriodEdge("1900", "", "11").toDisplayableString());
    assertEquals("(1)",
	new BibliographicPeriodEdge(null, "1", null).toDisplayableString());
    assertEquals("(1)",
	new BibliographicPeriodEdge("", "1", null).toDisplayableString());
    assertEquals("(1)",
	new BibliographicPeriodEdge(null, "1", "").toDisplayableString());
    assertEquals("(1)",
	new BibliographicPeriodEdge("", "1", "").toDisplayableString());
    assertEquals("(11)",
	new BibliographicPeriodEdge(null, "11", null).toDisplayableString());
    assertEquals("(11)",
	new BibliographicPeriodEdge("", "11", null).toDisplayableString());
    assertEquals("(11)",
	new BibliographicPeriodEdge(null, "11", "").toDisplayableString());
    assertEquals("(11)",
	new BibliographicPeriodEdge("", "11", "").toDisplayableString());
    assertEquals("(2)(3)",
	new BibliographicPeriodEdge(null, "2", "3").toDisplayableString());
    assertEquals("(2)(3)",
	new BibliographicPeriodEdge("", "2", "3").toDisplayableString());
    assertEquals("(2)(13)",
	new BibliographicPeriodEdge(null, "2", "13").toDisplayableString());
    assertEquals("(2)(13)",
	new BibliographicPeriodEdge("", "2", "13").toDisplayableString());
    assertEquals("(11)(2)",
	new BibliographicPeriodEdge(null, "11", "2").toDisplayableString());
    assertEquals("(11)(2)",
	new BibliographicPeriodEdge("", "11", "2").toDisplayableString());
    assertEquals("(11)(12)",
	new BibliographicPeriodEdge(null, "11", "12").toDisplayableString());
    assertEquals("(11)(12)",
	new BibliographicPeriodEdge("", "11", "12").toDisplayableString());
    assertEquals("()(4)",
	new BibliographicPeriodEdge(null, null, "4").toDisplayableString());
    assertEquals("()(4)",
	new BibliographicPeriodEdge("", null, "4").toDisplayableString());
    assertEquals("()(4)",
	new BibliographicPeriodEdge(null, "", "4").toDisplayableString());
    assertEquals("()(4)",
	new BibliographicPeriodEdge("", "", "4").toDisplayableString());
    assertEquals("()(11)",
	new BibliographicPeriodEdge(null, null, "11").toDisplayableString());
    assertEquals("()(11)",
	new BibliographicPeriodEdge("", null, "11").toDisplayableString());
    assertEquals("()(11)",
	new BibliographicPeriodEdge(null, "", "11").toDisplayableString());
    assertEquals("()(11)",
	new BibliographicPeriodEdge("", "", "11").toDisplayableString());
    assertEquals("\"-\"",
	new BibliographicPeriodEdge("-", null, null).toDisplayableString());
    assertEquals("(-)",
	new BibliographicPeriodEdge(null, "-", null).toDisplayableString());
    assertEquals("()(-)",
	new BibliographicPeriodEdge(null, null, "-").toDisplayableString());
    assertEquals("\"-\"(-)",
	new BibliographicPeriodEdge("-", "-", null).toDisplayableString());
    assertEquals("(-)(-)",
	new BibliographicPeriodEdge(null, "-", "-").toDisplayableString());
    assertEquals("\"-\"()(-)",
	new BibliographicPeriodEdge("-", null, "-").toDisplayableString());
    assertEquals("\"-\"(-)(-)",
	new BibliographicPeriodEdge("-", "-", "-").toDisplayableString());
  }

  /**
   * Check the behavior of isFullYear().
   */
  public final void testIsFullYear() {
    assertTrue(new BibliographicPeriodEdge(null).isFullYear());
    assertTrue(new BibliographicPeriodEdge("").isFullYear());
    assertTrue(new BibliographicPeriodEdge(" ").isFullYear());
    assertTrue(new BibliographicPeriodEdge("1954").isFullYear());
    assertFalse(new BibliographicPeriodEdge("1954(4)").isFullYear());
    assertFalse(new BibliographicPeriodEdge("1954(4)(2)").isFullYear());
    assertFalse(new BibliographicPeriodEdge("(12)(28)").isFullYear());
    assertFalse(new BibliographicPeriodEdge("(12)").isFullYear());
    assertFalse(new BibliographicPeriodEdge("()(28)").isFullYear());
    assertTrue(new BibliographicPeriodEdge(null, null, null).isFullYear());
    assertTrue(new BibliographicPeriodEdge("", "", "").isFullYear());
    assertTrue(new BibliographicPeriodEdge(" ", " ", " ").isFullYear());
    assertTrue(new BibliographicPeriodEdge("1954", null, null).isFullYear());
    assertTrue(new BibliographicPeriodEdge("1954", "", "").isFullYear());
    assertTrue(new BibliographicPeriodEdge("1954", " ", " ").isFullYear());
    assertFalse(new BibliographicPeriodEdge("1954", "4", null).isFullYear());
    assertFalse(new BibliographicPeriodEdge("1954", "4", "").isFullYear());
    assertFalse(new BibliographicPeriodEdge("1954", "4", " ").isFullYear());
    assertFalse(new BibliographicPeriodEdge("1954", "4", "2").isFullYear());
    assertFalse(new BibliographicPeriodEdge(null, "12", "28").isFullYear());
    assertFalse(new BibliographicPeriodEdge("", "12", "28").isFullYear());
    assertFalse(new BibliographicPeriodEdge(" ", "12", "28").isFullYear());
    assertFalse(new BibliographicPeriodEdge(null, "12", null).isFullYear());
    assertFalse(new BibliographicPeriodEdge("", "12", "").isFullYear());
    assertFalse(new BibliographicPeriodEdge(" ", "12", " ").isFullYear());
    assertFalse(new BibliographicPeriodEdge(null, null, "28").isFullYear());
    assertFalse(new BibliographicPeriodEdge("", "", "28").isFullYear());
    assertFalse(new BibliographicPeriodEdge(" ", " ", "28").isFullYear());
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

    assertFalse(new BibliographicPeriodEdge("1953").matches(ranges));
    assertTrue(new BibliographicPeriodEdge("1954").matches(ranges));
    assertFalse(new BibliographicPeriodEdge("1955").matches(ranges));
    assertFalse(new BibliographicPeriodEdge("1987").matches(ranges));
    assertTrue(new BibliographicPeriodEdge("1988").matches(ranges));
    assertFalse(new BibliographicPeriodEdge("1989").matches(ranges));

    assertFalse(new BibliographicPeriodEdge("1953(4)(2)").matches(ranges));
    assertFalse(new BibliographicPeriodEdge("1954(4)").matches(ranges));
    assertFalse(new BibliographicPeriodEdge("1954()(2)").matches(ranges));
    assertFalse(new BibliographicPeriodEdge("1954()(4)").matches(ranges));
    assertFalse(new BibliographicPeriodEdge("1954()(6)").matches(ranges));
    assertFalse(new BibliographicPeriodEdge("1954(4)(1)").matches(ranges));
    assertTrue(new BibliographicPeriodEdge("1954(4)(2)").matches(ranges));
    assertFalse(new BibliographicPeriodEdge("1954(4)(3)").matches(ranges));
    assertTrue(new BibliographicPeriodEdge("1954(4)(4)").matches(ranges));
    assertFalse(new BibliographicPeriodEdge("1954(4)(5)").matches(ranges));
    assertTrue(new BibliographicPeriodEdge("1954(4)(6)").matches(ranges));
    assertFalse(new BibliographicPeriodEdge("1954(4)(7)").matches(ranges));
    assertFalse(new BibliographicPeriodEdge("1954(5)()").matches(ranges));
    assertFalse(new BibliographicPeriodEdge("1955(4)(2)").matches(ranges));
    assertFalse(new BibliographicPeriodEdge("1987(12)(28)").matches(ranges));
    assertFalse(new BibliographicPeriodEdge("1988(11)(28)").matches(ranges));
    assertFalse(new BibliographicPeriodEdge("1988(12)").matches(ranges));
    assertFalse(new BibliographicPeriodEdge("1988()(28)").matches(ranges));
    assertFalse(new BibliographicPeriodEdge("1988(12)(27)").matches(ranges));
    assertTrue(new BibliographicPeriodEdge("1988(12)(28)").matches(ranges));
    assertFalse(new BibliographicPeriodEdge("1988(12)(29)").matches(ranges));
    assertFalse(new BibliographicPeriodEdge("1988(13)(1)").matches(ranges));
    assertFalse(new BibliographicPeriodEdge("1989(12)(28)").matches(ranges));
  }
}
