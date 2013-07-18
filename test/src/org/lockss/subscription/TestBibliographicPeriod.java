/*
 * $Id: TestBibliographicPeriod.java,v 1.2 2013-07-18 16:55:06 fergaloy-sf Exp $
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
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null]]",
	new BibliographicPeriod("-").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null]]",
	new BibliographicPeriod(" -").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null]]",
	new BibliographicPeriod("- ").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null]]",
	new BibliographicPeriod(" - ").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null]]",
	new BibliographicPeriod("1954").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null]]",
	new BibliographicPeriod(" 1954").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null]]",
	new BibliographicPeriod("1954 ").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null]]",
	new BibliographicPeriod(" 1954 ").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null]]",
	new BibliographicPeriod("19 54").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null]]",
	new BibliographicPeriod("1 9 5 4").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null]]",
	new BibliographicPeriod(" 1 9 5 4 ").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null]]",
	new BibliographicPeriod("1954-").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null]]",
	new BibliographicPeriod("-1954").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null]]",
	new BibliographicPeriod("1954-1954").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=1988, volume=null, issue=null]]",
	new BibliographicPeriod("1954-1988").toString());
  }

  /**
   * Check the behavior of BibliographicPeriod(String, String).
   */
  public final void testConstructor2Strings() {
    String nullString = null;
    assertEquals("BibliographicPeriod [startEdge=null, endEdge=null]",
	new BibliographicPeriod(nullString, nullString).toString());
    assertEquals("BibliographicPeriod [startEdge=null, endEdge=null]",
	new BibliographicPeriod(null, "").toString());
    assertEquals("BibliographicPeriod [startEdge=null, endEdge=null]",
	new BibliographicPeriod(null, " ").toString());
    assertEquals("BibliographicPeriod [startEdge=null, endEdge=null]",
	new BibliographicPeriod("", null).toString());
    assertEquals("BibliographicPeriod [startEdge=null, endEdge=null]",
	new BibliographicPeriod(" ", null).toString());
    assertEquals("BibliographicPeriod [startEdge=null, endEdge=null]",
	new BibliographicPeriod("", "").toString());
    assertEquals("BibliographicPeriod [startEdge=null, endEdge=null]",
	new BibliographicPeriod(" ", " ").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null]]",
	new BibliographicPeriod("1954", "1954").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null]]",
	new BibliographicPeriod(" 1954", "1954 ").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null]]",
	new BibliographicPeriod(" 1954 ", "1 9 5 4").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=1988, volume=null, issue=null]]",
	new BibliographicPeriod("1954", "1988").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=1988, volume=null, issue=null]]",
	new BibliographicPeriod("1954 ", " 1988").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=1988, volume=null, issue=null]]",
	new BibliographicPeriod("1 9 5 4", " 1 9 8 8 ").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=-, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null]]",
	new BibliographicPeriod("-", null).toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=-, volume=null, issue=null]]",
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
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null]]",
	new BibliographicPeriod("1954", "", "", "", "", "").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=1954, volume=4, issue=null], endEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null]]",
	new BibliographicPeriod(" 1954", "4", "", "", "", "").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=1954, volume=4, issue=2], endEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null]]",
	new BibliographicPeriod("1954 ", " 4", "2", "", "", "").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=null, volume=4, issue=2], endEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null]]",
	new BibliographicPeriod(" ", "4 ", " 2", "", "", "").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=2], endEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null]]",
	new BibliographicPeriod(" 1954 ", "", " 2", "", "", "").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null]]",
	new BibliographicPeriod("", "", "", "1954", "", "").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=1954, volume=4, issue=null]]",
	new BibliographicPeriod("", "", "", " 1954", "4", "").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=1954, volume=4, issue=2]]",
	new BibliographicPeriod("", "", "", "1954 ", " 4", "2").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=null, volume=4, issue=2]]",
	new BibliographicPeriod("", "", "", " ", "4 ", " 2").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=2]]",
	new BibliographicPeriod("", "", "", " 1954 ", "", " 2").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=1988, volume=null, issue=null]]",
	new BibliographicPeriod("1954", "", "", "1988", "", "").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=1954, volume=4, issue=null], endEdge=BibliographicPeriodEdge [year=1988, volume=12, issue=null]]",
	new BibliographicPeriod(" 1954", "4", "", "1988", "12", "").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=1954, volume=4, issue=2], endEdge=BibliographicPeriodEdge [year=1988, volume=12, issue=28]]",
	new BibliographicPeriod("1954 ", " 4", "2", "1988", "12", "28")
    .toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=null, volume=4, issue=2], endEdge=BibliographicPeriodEdge [year=null, volume=12, issue=28]]",
	new BibliographicPeriod(" ", "4 ", " 2", "", "12", "28").toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=1954, volume=null, issue=2], endEdge=BibliographicPeriodEdge [year=1988, volume=null, issue=28]]",
	new BibliographicPeriod(" 1954 ", "", " 2", "1988", "", "28")
    .toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=-, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null]]",
	new BibliographicPeriod("-", null, null, null, null, null).toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=null, volume=-, issue=null], endEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null]]",
	new BibliographicPeriod(null, "-", null, null, null, null).toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=null, volume=null, issue=-], endEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null]]",
	new BibliographicPeriod(null, null, "-", null, null, null).toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=-, volume=null, issue=null]]",
	new BibliographicPeriod(null, null, null, "-", null, null).toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=null, volume=-, issue=null]]",
	new BibliographicPeriod(null, null, null, null, "-", null).toString());
    assertEquals("BibliographicPeriod [startEdge=BibliographicPeriodEdge [year=null, volume=null, issue=null], endEdge=BibliographicPeriodEdge [year=null, volume=null, issue=-]]",
	new BibliographicPeriod(null, null, null, null, null, "-").toString());
  }

  /**
   * Check the behavior of createCollection().
   */
  public final void testCreateCollection() {
    assertEquals(1, BibliographicPeriod.createCollection(null).size());
    assertEquals(1, BibliographicPeriod.createCollection("").size());
    assertEquals(1, BibliographicPeriod.createCollection(" ").size());
    assertEquals(1, BibliographicPeriod.createCollection("-").size());
    assertEquals(1, BibliographicPeriod.createCollection(" -").size());
    assertEquals(1, BibliographicPeriod.createCollection("- ").size());
    assertEquals(1, BibliographicPeriod.createCollection(" - ").size());
    assertEquals(1, BibliographicPeriod.createCollection("1954").size());
    assertEquals(1, BibliographicPeriod.createCollection(" 1954").size());
    assertEquals(1, BibliographicPeriod.createCollection("1954 ").size());
    assertEquals(1, BibliographicPeriod.createCollection(" 1954 ").size());
    assertEquals(1, BibliographicPeriod.createCollection(" 1 9 5 4 ").size());
    assertEquals(1, BibliographicPeriod.createCollection("1954(4)").size());
    assertEquals(1, BibliographicPeriod.createCollection("1954(4)(2)").size());
    assertEquals(1, BibliographicPeriod.createCollection("(4)(2)").size());
    assertEquals(1, BibliographicPeriod.createCollection("1954()(2)").size());
    assertEquals(1, BibliographicPeriod.createCollection("1954-").size());
    assertEquals(1, BibliographicPeriod.createCollection("-1954").size());
    assertEquals(1, BibliographicPeriod.createCollection("1954-1954").size());
    assertEquals(1, BibliographicPeriod.createCollection("1954-1988").size());
    assertEquals(1,
	BibliographicPeriod.createCollection("1954(4)-1988(12)(28)").size());
    assertEquals(1,
	BibliographicPeriod.createCollection("(4)(2)-1988()(28)").size());
    assertEquals(0, BibliographicPeriod.createCollection(",").size());
    assertEquals(0, BibliographicPeriod.createCollection(" ,").size());
    assertEquals(0, BibliographicPeriod.createCollection(", ").size());
    assertEquals(0, BibliographicPeriod.createCollection(" , ").size());
    assertEquals(1, BibliographicPeriod.createCollection("-,").size());
    assertEquals(2, BibliographicPeriod.createCollection(",-").size());
    assertEquals(2, BibliographicPeriod.createCollection("-,1900").size());
    assertEquals(2, BibliographicPeriod.createCollection("1900,-").size());
    assertEquals(3,
	BibliographicPeriod.createCollection("1900,1954,1988").size());
  }

  /**
   * Check the behavior of createList().
   */
  public final void testCreateList() {
    assertEquals(1, BibliographicPeriod.createList(null).size());
    assertEquals(1, BibliographicPeriod.createList("").size());
    assertEquals(1, BibliographicPeriod.createList(" ").size());
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
    assertEquals(2, BibliographicPeriod.createList(",-").size());
    assertEquals(2, BibliographicPeriod.createList("-,1900").size());
    assertEquals(2, BibliographicPeriod.createList("1900,-").size());
    assertEquals(3, BibliographicPeriod.createList("1900,1954,1988").size());
  }

  /**
   * Check the behavior of rangesAsString and createList().
   */
  public final void testStringListStringConversion() {
    assertEquals("", rangesAsString(null));
    assertEquals("", rangesAsString(""));
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
    assertEquals("1900,1910,1920", rangesAsString("1920,1910,1900"));
  }

  private String rangesAsString(String ranges) {
    return BibliographicPeriod
	.rangesAsString(BibliographicPeriod.createList(ranges));
  }

  /**
   * Check the behavior of coalesce().
   */
  public final void testCoalesce() {
    assertEquals(1, coalesce(null).size());
    assertEquals(1, coalesce("").size());
    assertEquals(1, coalesce(" ").size());
    assertEquals(1, coalesce("-").size());
    assertEquals(1, coalesce("1954").size());
    assertEquals(1, coalesce("1954,1954").size());
    assertEquals(2, coalesce("1954,1988").size());
    assertEquals("1954", coalesce("1954,1988").get(0).toDisplayableString());
    assertEquals("1988", coalesce("1954,1988").get(1).toDisplayableString());
    assertEquals(1, coalesce("1954,1955").size());
    assertEquals("1954-1955",
	coalesce("1954,1955").get(0).toDisplayableString());
    assertEquals(1, coalesce("1954-1955,1954-1955").size());
    assertEquals(2, coalesce("1954-1955,1988-1990").size());
    assertEquals("1954-1955",
	coalesce("1954-1955,1988-1990").get(0).toDisplayableString());
    assertEquals("1988-1990",
	coalesce("1954-1955,1988-1990").get(1).toDisplayableString());
    assertEquals(1, coalesce("1954-1955,1955").size());
    assertEquals("1954-1955",
	coalesce("1954-1955,1955").get(0).toDisplayableString());
    assertEquals(1, coalesce("1954-1955,1955-").size());
    assertEquals("1954-",
	coalesce("1954-1955,1955-").get(0).toDisplayableString());
    assertEquals(1, coalesce("1954-1955,1955-1955").size());
    assertEquals("1954-1955",
	coalesce("1954-1955,1955-1955").get(0).toDisplayableString());
    assertEquals(1, coalesce("1954-1955,1955-1988").size());
    assertEquals("1954-1988",
	coalesce("1954-1955,1955-1988").get(0).toDisplayableString());
    assertEquals(1, coalesce("1954-1954,1955-1988").size());
    assertEquals("1954-1988",
	coalesce("1954-1954,1955-1988").get(0).toDisplayableString());
    assertEquals(1, coalesce("1954,1954-1988").size());
    assertEquals("1954-1988",
	coalesce("1954,1954-1988").get(0).toDisplayableString());
    assertEquals(1, coalesce("1954,1955-1988").size());
    assertEquals("1954-1988",
	coalesce("1954,1955-1988").get(0).toDisplayableString());
    assertEquals(1, coalesce("1954-1955,1955-1988,1988-1990").size());
    assertEquals("1954-1990",
	coalesce("1954-1955,1955-1988,1988-1990").get(0).toDisplayableString());
    assertEquals(1, coalesce("1954-1955,1955-1988,1988-").size());
    assertEquals("1954-",
	coalesce("1954-1955,1955-1988,1988-").get(0).toDisplayableString());
    assertEquals(1, coalesce("1954-1955,1956-1987,1988-").size());
    assertEquals("1954-",
	coalesce("1954-1955,1956-1987,1988-").get(0).toDisplayableString());
    assertEquals(2, coalesce("1954(4),1988(12)").size());
    assertEquals("1954(4)",
	coalesce("1954(4),1988(12)").get(0).toDisplayableString());
    assertEquals("1988(12)",
	coalesce("1954(4),1988(12)").get(1).toDisplayableString());
    assertEquals(1, coalesce("1954(4),1954(5)").size());
    assertEquals("1954(4)-1954(5)",
	coalesce("1954(4),1954(5)").get(0).toDisplayableString());
    assertEquals(1, coalesce("1954(4),1955(5)").size());
    assertEquals("1954(4)-1955(5)",
	coalesce("1954(4),1955(5)").get(0).toDisplayableString());
  }

  private List<BibliographicPeriod> coalesce(String ranges) {
    return BibliographicPeriod.coalesce(BibliographicPeriod.createList(ranges));
  }

  /**
   * Check the behavior of intersects().
   */
  public final void testIntersects() {
    assertFalse(intersects("", "2000"));
    assertTrue(intersects("2000", "2000"));
    assertFalse(intersects("2000", "1999"));
    assertFalse(intersects("2000", "2001"));
    assertTrue(intersects("2000", "2000-2000"));
    assertTrue(intersects("2000", "1999-2000"));
    assertTrue(intersects("2000", "1999-2001"));
    assertTrue(intersects("2000", "2000-2001"));
    assertFalse(intersects("2000", "1998-1999"));
    assertFalse(intersects("2000", "2001-2002"));
    assertTrue(intersects("2000", "-2001"));
    assertTrue(intersects("2000", "-2000"));
    assertFalse(intersects("2000", "-1999"));
    assertTrue(intersects("2000", "1999-"));
    assertTrue(intersects("2000", "2000-"));
    assertFalse(intersects("2000", "2001-"));
    assertTrue(intersects("2000", "-"));
    assertTrue(intersects("2000(3)", "2000"));
    assertFalse(intersects("2000(3)", "1999"));
    assertFalse(intersects("2000(3)", "2001"));
    assertTrue(intersects("2000(3)", "2000-2000"));
    assertTrue(intersects("2000(3)", "1999-2000"));
    assertTrue(intersects("2000(3)", "1999-2001"));
    assertTrue(intersects("2000(3)", "2000-2001"));
    assertFalse(intersects("2000(3)", "1998-1999"));
    assertFalse(intersects("2000(3)", "2001-2002"));
    assertTrue(intersects("2000(3)", "-2001"));
    assertTrue(intersects("2000(3)", "-2000"));
    assertFalse(intersects("2000(3)", "-1999"));
    assertTrue(intersects("2000(3)", "1999-"));
    assertTrue(intersects("2000(3)", "2000-"));
    assertFalse(intersects("2000(3)", "2001-"));
    assertTrue(intersects("2000(3)", "-"));
    assertTrue(intersects("2000(3)", "2000(3)-2000(3)"));
    assertTrue(intersects("2000(3)", "2000(1)-2000(5)"));
    assertTrue(intersects("2000(3)", "2000(3)-2000(5)"));
    assertTrue(intersects("2000(3)", "2000(1)-2000(3)"));
    assertFalse(intersects("2000(3)", "1998(1)-1999(7)"));
    assertFalse(intersects("2000(3)", "2001(1)-2002(9)"));
    assertTrue(intersects("2000(3)", "1999(1)-2001(5)"));
    assertTrue(intersects("2000(3)", "1999(7)-2001(1)"));
    assertTrue(intersects("2000(3)", "2000(1)-2001(2)"));
    assertTrue(intersects("2000(3)", "2000(1)-2001"));
    assertTrue(intersects("2000(3)", "2000(1)-2001()(1)"));
    assertTrue(intersects("2000(3)", "2000(1)-2000"));
    assertTrue(intersects("2000(3)",
	"2000()(10)-2001()(1)"));
    assertTrue(intersects("2000(3)(5)", "2000(1)-2001(5)"));
    assertTrue(intersects("2000(3)(5)",
	"2000(1)(20)-2001(5)(25)"));
    assertTrue(intersects("2000(3)(5)",
	"2000(1)(20)-2001(3)(25)"));
    assertTrue(intersects("2000(3)(5)",
	"2000(1)(20)-2001(3)(5)"));
    assertTrue(intersects("2000(3)(5)",
	"2000(3)(5)-2001(5)(25)"));
    assertFalse(intersects("2000(3)(5)",
	"2000(3)(6)-2001(5)(25)"));
    assertFalse(intersects("2000(3)(5)",
	"2000(1)(1)-2000(3)(4)"));
  }

  private boolean intersects(String period, String ranges) {
    return new BibliographicPeriod(period)
    	.intersects(BibliographicPeriod.createCollection(ranges));
  }

  /**
   * Check the behavior of matchRangeEdgesToEdge().
   */
  public final void testMatchRangeEdgesToEdge() {
    assertEquals("1900-2000",
	matchStringRangeEdgesToEdge("1900-2000", "2000"));
    assertEquals("1900-2000",
	matchStringRangeEdgesToEdge("1900-2000", " 2000"));
    assertEquals("1900-2000",
	matchStringRangeEdgesToEdge("1900-2000", "2000 "));
    assertEquals("1900-2000",
	matchStringRangeEdgesToEdge("1900-2000", " 2000 "));
    assertEquals("1900-2000",
	matchStringRangeEdgesToEdge("1900-2000", " 2 0 0 0 "));
    assertEquals("1900(0)-2000(9999)",
	matchStringRangeEdgesToEdge("1900-2000", "2000(1)"));
    assertEquals("1900()(0)-2000()(9999)",
	matchStringRangeEdgesToEdge("1900-2000", "2000()(1)"));
    assertEquals("1900(0)(0)-2000(9999)(9999)",
	matchStringRangeEdgesToEdge("1900-2000", "2000(3)(1)"));
    assertEquals("1900(11)-2000(12)",
	matchStringRangeEdgesToEdge("1900(11)-2000(12)", "2000"));
    assertEquals("1900(11)-2000(12)",
	matchStringRangeEdgesToEdge("1900(11)-2000(12)", "2000(1)"));
    assertEquals("1900(11)(0)-2000(12)(9999)",
	matchStringRangeEdgesToEdge("1900(11)-2000(12)", "2000()(1)"));
    assertEquals("1900(11)(0)-2000(12)(9999)",
	matchStringRangeEdgesToEdge("1900(11)-2000(12)", "2000(6)(1)"));
    assertEquals("1900()(5)-2000()(3)",
	matchStringRangeEdgesToEdge("1900()(5)-2000()(3)", "2000"));
    assertEquals("1900(0)(5)-2000(9999)(3)",
	matchStringRangeEdgesToEdge("1900()(5)-2000()(3)", "2000(4)"));
    assertEquals("1900()(5)-2000()(3)",
	matchStringRangeEdgesToEdge("1900()(5)-2000()(3)", "2000()(1)"));
    assertEquals("1900(0)(5)-2000(9999)(3)",
	matchStringRangeEdgesToEdge("1900()(5)-2000()(3)", "2000(4)(2)"));
    assertEquals("1900(8)(5)-2000(12)(3)",
	matchStringRangeEdgesToEdge("1900(8)(5)-2000(12)(3)", "2000"));
    assertEquals("1900(8)(5)-2000(12)(3)",
	matchStringRangeEdgesToEdge("1900(8)(5)-2000(12)(3)", "2000(10)"));
    assertEquals("1900(8)(5)-2000(12)(3)",
	matchStringRangeEdgesToEdge("1900(8)(5)-2000(12)(3)",
	    "2000()(3)"));
    assertEquals("1900(8)(5)-2000(12)(3)",
	matchStringRangeEdgesToEdge("1900(8)(5)-2000(12)(3)",
	    "2000(1)(2)"));
    assertEquals("", matchStringRangeEdgesToEdge(null, "-"));
    assertEquals("", matchStringRangeEdgesToEdge(null, "(-)"));
    assertEquals("", matchStringRangeEdgesToEdge(null, "()(-)"));
  }

  private String matchStringRangeEdgesToEdge(String range,
      String matchingEdge) {
    return BibliographicPeriod
	.matchRangeEdgesToEdge(new BibliographicPeriod(range),
	    new BibliographicPeriodEdge(matchingEdge)).toCanonicalString();
  }

  /**
   * Check the behavior of extendFuture().
   */
  public final void testExtendFuture() {
    assertEquals("", extendFuture(null).get(0).toDisplayableString());
    assertEquals("", extendFuture("").get(0).toDisplayableString());
    assertEquals("", extendFuture(" ").get(0).toDisplayableString());
    assertEquals("-", extendFuture("-").get(0).toDisplayableString());
    assertEquals("1954-", extendFuture("1954").get(0).toDisplayableString());
    assertEquals("1988-",
	extendFuture("1954,1988").get(1).toDisplayableString());
    assertEquals("1988-",
	extendFuture("1954-1955,1988-1990").get(1).toDisplayableString());
    assertEquals("1988-",
	extendFuture("1954-1955,1955-1988,1988-1990").get(2)
	.toDisplayableString());
  }

  private List<BibliographicPeriod> extendFuture(String ranges) {
    List<BibliographicPeriod> periods = BibliographicPeriod.createList(ranges);
    BibliographicPeriod.extendFuture(periods);
    return periods;
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
   * Check the behavior of toCanonicalString().
   */
  public final void testToCanonicalString() {
    assertEquals("", new BibliographicPeriod(null).toCanonicalString());
    assertEquals("", new BibliographicPeriod("").toCanonicalString());
    assertEquals("", new BibliographicPeriod(" ").toCanonicalString());
    assertEquals("-", new BibliographicPeriod("-").toCanonicalString());
    assertEquals("-", new BibliographicPeriod(" -").toCanonicalString());
    assertEquals("-", new BibliographicPeriod("- ").toCanonicalString());
    assertEquals("-", new BibliographicPeriod(" - ").toCanonicalString());
    assertEquals("1954-1954",
	new BibliographicPeriod("1954").toCanonicalString());
    assertEquals("1954-1954",
	new BibliographicPeriod(" 1954").toCanonicalString());
    assertEquals("1954-1954",
	new BibliographicPeriod("1954 ").toCanonicalString());
    assertEquals("1954-1954",
	new BibliographicPeriod(" 1954 ").toCanonicalString());
    assertEquals("1954-1954",
	new BibliographicPeriod(" 1 9 5 4 ").toCanonicalString());
    assertEquals("1954-", new BibliographicPeriod("1954-").toCanonicalString());
    assertEquals("1954-",
	new BibliographicPeriod(" 1954-").toCanonicalString());
    assertEquals("1954-",
	new BibliographicPeriod("1954 -").toCanonicalString());
    assertEquals("1954-",
	new BibliographicPeriod("1954- ").toCanonicalString());
    assertEquals("1954-",
	new BibliographicPeriod(" 1954 - ").toCanonicalString());
    assertEquals("1954-",
	new BibliographicPeriod(" 1 9 5 4 - ").toCanonicalString());
    assertEquals("-1954", new BibliographicPeriod("-1954").toCanonicalString());
    assertEquals("-1954",
	new BibliographicPeriod(" -1954").toCanonicalString());
    assertEquals("-1954",
	new BibliographicPeriod("- 1954").toCanonicalString());
    assertEquals("-1954",
	new BibliographicPeriod("-1954 ").toCanonicalString());
    assertEquals("-1954",
	new BibliographicPeriod(" - 1954 ").toCanonicalString());
    assertEquals("-1954",
	new BibliographicPeriod(" - 1 9 5 4 ").toCanonicalString());
    assertEquals("1954-1954",
	new BibliographicPeriod("1954-1954").toCanonicalString());
    assertEquals("1954-1954",
	new BibliographicPeriod(" 1954-1954").toCanonicalString());
    assertEquals("1954-1954",
	new BibliographicPeriod("1954-1954 ").toCanonicalString());
    assertEquals("1954-1954",
	new BibliographicPeriod(" 1954-1954 ").toCanonicalString());
    assertEquals("1954-1954",
	new BibliographicPeriod(" 1 9 5 4-1954 ").toCanonicalString());
    assertEquals("1954-1954",
	new BibliographicPeriod(" 1954-1 9 5 4 ").toCanonicalString());
    assertEquals("1954-1954",
	new BibliographicPeriod(" 1 9 5 4 - 1 9 5 4 ").toCanonicalString());
    assertEquals("1954-1988",
	new BibliographicPeriod("1954-1988").toCanonicalString());
    assertEquals("1954-1988",
	new BibliographicPeriod(" 1954-1988").toCanonicalString());
    assertEquals("1954-1988",
	new BibliographicPeriod("1954-1988 ").toCanonicalString());
    assertEquals("1954-1988",
	new BibliographicPeriod(" 1954-1988 ").toCanonicalString());
    assertEquals("1954-1988",
	new BibliographicPeriod(" 1 9 5 4-1988 ").toCanonicalString());
    assertEquals("1954-1988",
	new BibliographicPeriod(" 1954-1 9 8 8 ").toCanonicalString());
    assertEquals("1954-1988",
	new BibliographicPeriod(" 1 9 5 4 - 1 9 8 8 ").toCanonicalString());
  }

  /**
   * Check the behavior of normalizeRange().
   */
  public final void testNormalize() {
    assertEquals("", normalizeRangetoString(null));
    assertEquals("", normalizeRangetoString(""));
    assertEquals("0001-0001", normalizeRangetoString("1"));
    assertEquals("0012-0012", normalizeRangetoString("12"));
    assertEquals("0123-0123", normalizeRangetoString("123"));
    assertEquals("1999-1999", normalizeRangetoString("1999"));
    assertEquals("0000-9999", normalizeRangetoString("-"));
    assertEquals("0001-9999", normalizeRangetoString("1-"));
    assertEquals("1999-9999", normalizeRangetoString("1999-"));
    assertEquals("0000-0012", normalizeRangetoString("-12"));
    assertEquals("0000-2000", normalizeRangetoString("-2000"));
    assertEquals("0001-0012", normalizeRangetoString("1-12"));
    assertEquals("0012-0123", normalizeRangetoString("12-123"));
    assertEquals("0123-1234", normalizeRangetoString("123-1234"));
    assertEquals("1999-2000", normalizeRangetoString("1999-2000"));
    assertEquals("1999(0001)-9999(9999)",
	normalizeRangetoString("1999(1)-"));
    assertEquals("0000(0000)-2000(0001)",
	normalizeRangetoString("-2000(1)"));
    assertEquals("1999(0001)-2000(0001)",
	normalizeRangetoString("1999(1)-2000(1)"));
    assertEquals("1999(0012)-9999(9999)",
	normalizeRangetoString("1999(12)-"));
    assertEquals("1999(0012)-2000(0001)",
	normalizeRangetoString("1999(12)-2000(1)"));
    assertEquals("0000(0000)-2000(0012)",
	normalizeRangetoString("-2000(12)"));
    assertEquals("1999(0001)-2000(0012)",
	normalizeRangetoString("1999(1)-2000(12)"));
    assertEquals("1999(0011)-2000(0012)",
	normalizeRangetoString("1999(11)-2000(12)"));
    assertEquals("1999()(0001)-9999()(9999)",
	normalizeRangetoString("1999()(1)-"));
    assertEquals("0000()(0000)-2000()(0001)",
	normalizeRangetoString("-2000()(1)"));
    assertEquals("1999()(0001)-2000()(0001)",
	normalizeRangetoString("1999()(1)-2000()(1)"));
    assertEquals("1999()(0012)-9999()(9999)",
	normalizeRangetoString("1999()(12)-"));
    assertEquals("1999()(0012)-2000()(0001)",
	normalizeRangetoString("1999()(12)-2000()(1)"));
    assertEquals("0000()(0000)-2000()(0012)",
	normalizeRangetoString("-2000()(12)"));
    assertEquals("1999()(0001)-2000()(0012)",
	normalizeRangetoString("1999()(1)-2000()(12)"));
    assertEquals("1999()(0011)-2000()(0012)",
	normalizeRangetoString("1999()(11)-2000()(12)"));
    assertEquals("1999(0002)(0001)-9999(9999)(9999)",
	normalizeRangetoString("1999(2)(1)-"));
    assertEquals("0000(0000)(0000)-2000(0002)(0001)",
	normalizeRangetoString("-2000(2)(1)"));
    assertEquals("1999(0002)(0001)-2000(0002)(0001)",
	normalizeRangetoString("1999(2)(1)-2000(2)(1)"));
    assertEquals("1999(0003)(0012)-9999(9999)(9999)",
	normalizeRangetoString("1999(3)(12)-"));
    assertEquals("1999(0003)(0012)-2000(0003)(0001)",
	normalizeRangetoString("1999(3)(12)-2000(3)(1)"));
    assertEquals("0000(0000)(0000)-2000(0011)(0012)",
	normalizeRangetoString("-2000(11)(12)"));
    assertEquals("1999(0011)(0001)-2000(0011)(0012)",
	normalizeRangetoString("1999(11)(1)-2000(11)(12)"));
    assertEquals("1999(0012)(0011)-2000(0011)(0012)",
	normalizeRangetoString("1999(12)(11)-2000(11)(12)"));
  }

  private String normalizeRangetoString(String range) {
    return new BibliographicPeriod(range).normalize().toCanonicalString();
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
    assertFalse(new BibliographicPeriod(nullString, nullString).isAllTime());
    assertFalse(new BibliographicPeriod(null, "").isAllTime());
    assertFalse(new BibliographicPeriod(null, " ").isAllTime());
    assertFalse(new BibliographicPeriod("", null).isAllTime());
    assertFalse(new BibliographicPeriod(" ", null).isAllTime());
    assertFalse(new BibliographicPeriod("", "").isAllTime());
    assertFalse(new BibliographicPeriod(" ", " ").isAllTime());
    assertFalse(new BibliographicPeriod("1954", "1954").isAllTime());
    assertFalse(new BibliographicPeriod(" 1954", "1954 ").isAllTime());
    assertFalse(new BibliographicPeriod(" 1954 ", "1 9 5 4").isAllTime());
    assertFalse(new BibliographicPeriod("1954", "1988").isAllTime());
    assertFalse(new BibliographicPeriod("1954 ", " 1988").isAllTime());
    assertFalse(new BibliographicPeriod("1 9 5 4", " 1 9 8 8 ").isAllTime());
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
}
