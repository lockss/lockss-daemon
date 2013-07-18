/*
 * $Id: TestBibliographicPeriodEdge.java,v 1.1 2013-07-18 16:51:04 fergaloy-sf Exp $
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

import org.lockss.test.LockssTestCase;

public class TestBibliographicPeriodEdge extends LockssTestCase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
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
   * Check the behavior of matchEdgeToEdge().
   */
  public final void testMatchEdgeToEdge() {
    assertEquals("", matchEdgeToEdge("", true, null));
    assertEquals("", matchEdgeToEdge(" ", true, null));
    assertEquals("", matchEdgeToEdge("", false, null));
    assertEquals("", matchEdgeToEdge(" ", false, null));
    assertEquals("", matchEdgeToEdge("", true,
	new BibliographicPeriodEdge("")));
    assertEquals("", matchEdgeToEdge("", true,
	new BibliographicPeriodEdge(" ")));
    assertEquals("", matchEdgeToEdge(" ", true,
	new BibliographicPeriodEdge("")));
    assertEquals("", matchEdgeToEdge(" ", true,
	new BibliographicPeriodEdge(" ")));
    assertEquals("", matchEdgeToEdge("", false,
	new BibliographicPeriodEdge("")));
    assertEquals("", matchEdgeToEdge(" ", false,
	new BibliographicPeriodEdge("")));
    assertEquals("", matchEdgeToEdge("", false,
	new BibliographicPeriodEdge(" ")));
    assertEquals("", matchEdgeToEdge(" ", false,
	new BibliographicPeriodEdge(" ")));
    assertEquals("", matchEdgeToEdge("", true,
	new BibliographicPeriodEdge("2000")));
    assertEquals("", matchEdgeToEdge("", false,
	new BibliographicPeriodEdge("2000")));
    assertEquals("(0)",
	matchEdgeToEdge("", true, new BibliographicPeriodEdge("2000(3)")));
    assertEquals("(9999)",
	matchEdgeToEdge("", false, new BibliographicPeriodEdge("2000(3)")));
    assertEquals("(0)",
	matchEdgeToEdge("", true, new BibliographicPeriodEdge("2000(12)")));
    assertEquals("(9999)",
	matchEdgeToEdge("", false, new BibliographicPeriodEdge("2000(12)")));
    assertEquals("()(0)",
	matchEdgeToEdge("", true, new BibliographicPeriodEdge("2000()(3)")));
    assertEquals("()(9999)",
	matchEdgeToEdge("", false, new BibliographicPeriodEdge("2000()(3)")));
    assertEquals("()(0)",
	matchEdgeToEdge("", true, new BibliographicPeriodEdge("2000()(12)")));
    assertEquals("()(9999)",
	matchEdgeToEdge("", false, new BibliographicPeriodEdge("2000()(12)")));
    assertEquals("(0)(0)",
	matchEdgeToEdge("", true, new BibliographicPeriodEdge("2000(3)(4)")));
    assertEquals("(9999)(9999)",
	matchEdgeToEdge("", false, new BibliographicPeriodEdge("2000(3)(4)")));
    assertEquals("(0)(0)",
	matchEdgeToEdge("", true, new BibliographicPeriodEdge("2000(3)(12)")));
    assertEquals("(9999)(9999)",
	matchEdgeToEdge("", false, new BibliographicPeriodEdge("2000(3)(12)")));
    assertEquals("(0)(0)",
	matchEdgeToEdge("", true, new BibliographicPeriodEdge("2000(11)(4)")));
    assertEquals("(9999)(9999)",
	matchEdgeToEdge("", false, new BibliographicPeriodEdge("2000(11)(4)")));
    assertEquals("(0)(0)",
	matchEdgeToEdge("", true, new BibliographicPeriodEdge("2000(11)(12)")));
    assertEquals("(9999)(9999)",
	matchEdgeToEdge("", false,
	    new BibliographicPeriodEdge("2000(11)(12)")));
    assertEquals("1900",
	matchEdgeToEdge("1900", true, null));
    assertEquals("1900",
	matchEdgeToEdge(" 1900", true, null));
    assertEquals("1900",
	matchEdgeToEdge("1900 ", true, null));
    assertEquals("1900",
	matchEdgeToEdge(" 1900 ", true, null));
    assertEquals("1900",
	matchEdgeToEdge(" 1 9 0 0 ", true, null));
    assertEquals("1900",
	matchEdgeToEdge("1900", false, null));
    assertEquals("1900",
	matchEdgeToEdge(" 1900", false, null));
    assertEquals("1900",
	matchEdgeToEdge("1900 ", false, null));
    assertEquals("1900",
	matchEdgeToEdge(" 1900 ", false, null));
    assertEquals("1900",
	matchEdgeToEdge(" 1 9 0 0 ", false, null));
    assertEquals("1900",
	matchEdgeToEdge("1900", true, new BibliographicPeriodEdge("")));
    assertEquals("1900",
	matchEdgeToEdge("1900", false, new BibliographicPeriodEdge("")));
    assertEquals("1900",
	matchEdgeToEdge("1900", true, new BibliographicPeriodEdge("2000")));
    assertEquals("1900",
	matchEdgeToEdge("1900", false, new BibliographicPeriodEdge("2000")));
    assertEquals("1900(0)",
	matchEdgeToEdge("1900", true, new BibliographicPeriodEdge("2000(3)")));
    assertEquals("1900(9999)",
	matchEdgeToEdge("1900", false, new BibliographicPeriodEdge("2000(3)")));
    assertEquals("1900(0)",
	matchEdgeToEdge("1900", true, new BibliographicPeriodEdge("2000(12)")));
    assertEquals("1900(9999)",
	matchEdgeToEdge("1900", false,
	    new BibliographicPeriodEdge("2000(12)")));
    assertEquals("1900()(0)",
	matchEdgeToEdge("1900", true,
	    new BibliographicPeriodEdge("2000()(6)")));
    assertEquals("1900()(9999)",
	matchEdgeToEdge("1900", false,
	    new BibliographicPeriodEdge("2000()(6)")));
    assertEquals("1900()(0)",
	matchEdgeToEdge("1900", true,
	    new BibliographicPeriodEdge("2000()(11)")));
    assertEquals("1900()(9999)",
	matchEdgeToEdge("1900", false,
	    new BibliographicPeriodEdge("2000()(11)")));
    assertEquals("1900(0)(0)",
	matchEdgeToEdge("1900", true,
	    new BibliographicPeriodEdge("2000(3)(4)")));
    assertEquals("1900(9999)(9999)",
	matchEdgeToEdge("1900", false,
	    new BibliographicPeriodEdge("2000(3)(4)")));
    assertEquals("1900(0)(0)",
	matchEdgeToEdge("1900", true,
	    new BibliographicPeriodEdge("2000(3)(10)")));
    assertEquals("1900(9999)(9999)",
	matchEdgeToEdge("1900", false,
	    new BibliographicPeriodEdge("2000(3)(10)")));
    assertEquals("1900(0)(0)",
	matchEdgeToEdge("1900", true,
	    new BibliographicPeriodEdge("2000(12)(5)")));
    assertEquals("1900(9999)(9999)",
	matchEdgeToEdge("1900", false,
	    new BibliographicPeriodEdge("2000(12)(5)")));
    assertEquals("1900(0)(0)",
	matchEdgeToEdge("1900", true,
	    new BibliographicPeriodEdge("2000(10)(11)")));
    assertEquals("1900(9999)(9999)",
	matchEdgeToEdge("1900", false,
	    new BibliographicPeriodEdge("2000(10)(11)")));
    assertEquals("\"-\"", matchEdgeToEdge("-", true, null));
  }

  private String matchEdgeToEdge(String edgeText, boolean isStart,
      BibliographicPeriodEdge matchingEdge) {
    return new BibliographicPeriodEdge(edgeText)
    .matchEdgeToEdge(isStart, matchingEdge).toDisplayableString();
  }

  /**
   * Check the behavior of follows().
   */
  public final void testFollows() {
    assertFalse(new BibliographicPeriodEdge("").follows(null));
    assertFalse(follows("", ""));
    assertFalse(follows("1954", "1988"));
    assertFalse(follows("1988", "1988"));
    assertTrue(follows("1989", "1988"));
    assertFalse(follows("1954(10)", "1988(1)"));
    assertTrue(follows("1989(1)", "1988(12)"));
    assertFalse(follows("1954(3)", "1954(4)"));
    assertFalse(follows("1954(4)", "1954(4)"));
    assertTrue(follows("1954(5)", "1954(4)"));
  }

  private boolean follows(String second, String first) {
    return new BibliographicPeriodEdge(second)
    .follows(new BibliographicPeriodEdge(first));
  }

  /**
   * Check the behavior of compareTo().
   */
  public final void testCompareTo() {
    assertEquals(0, new BibliographicPeriodEdge("").compareTo(null));
    assertEquals(0, compareTo("", ""));
    assertEquals(-1, compareTo("1954", "1988"));
    assertEquals(0, compareTo("1988", "1988"));
    assertEquals(1, compareTo("1989", "1988"));
    assertEquals(-1, compareTo("1954(10)", "1988(1)"));
    assertEquals(1, compareTo("1989(1)", "1988(12)"));
    assertEquals(-1, compareTo("1954(3)", "1954(4)"));
    assertEquals(0, compareTo("1954(4)", "1954(4)"));
    assertEquals(1, compareTo("1954(5)", "1954(4)"));
  }

  private int compareTo(String second, String first) {
    return new BibliographicPeriodEdge(second)
    .compareTo(new BibliographicPeriodEdge(first));
  }
}
