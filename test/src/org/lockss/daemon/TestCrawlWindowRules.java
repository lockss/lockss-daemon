/*
 * $Id: TestCrawlWindowRules.java,v 1.1.2.1 2003-10-09 23:20:05 eaalto Exp $
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
import org.lockss.util.SetUtil;
import org.lockss.test.LockssTestCase;

/**
 * This is the test class for org.lockss.daemon.CrawlWindowRule
 */

public class TestCrawlWindowRules extends LockssTestCase {
  static final int incl = CrawlWindowRules.BaseCrawlWindow.MATCH_INCLUDE;
  static final int excl = CrawlWindowRules.BaseCrawlWindow.MATCH_EXCLUDE;
  static final int notincl = CrawlWindowRules.BaseCrawlWindow.NO_MATCH_INCLUDE;
  static final int notexcl = CrawlWindowRules.BaseCrawlWindow.NO_MATCH_EXCLUDE;
  static final int inclexcl = CrawlWindowRules.BaseCrawlWindow.MATCH_INCLUDE_ELSE_EXCLUDE;
  static final int exclincl = CrawlWindowRules.BaseCrawlWindow.MATCH_EXCLUDE_ELSE_INCLUDE;

  Calendar start;
  Calendar end;
  Calendar testCal;
  CrawlWindowRule.Window interval;

  public void setUp() throws Exception {
    super.setUp();

    start = Calendar.getInstance();
    end = Calendar.getInstance();
    testCal = Calendar.getInstance();

    start.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
    end.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
    interval = new CrawlWindows.Interval(start, end, Calendar.DAY_OF_WEEK);
  }

  public void testNullWindow() {
    try {
      CrawlWindowRule cw =
          new CrawlWindowRules.BaseCrawlWindow(null, incl, null);
      fail("CrawlWindowRules.BaseCrawlWindow with null Window should throw");
    } catch (NullPointerException e) { }
  }

  public void testMatchIncl() {
    CrawlWindowRule cw =
        new CrawlWindowRules.BaseCrawlWindow(interval, incl, null);
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    assertEquals(CrawlWindowRule.IGNORE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
    assertEquals(CrawlWindowRule.INCLUDE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
    assertEquals(CrawlWindowRule.INCLUDE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
    assertEquals(CrawlWindowRule.INCLUDE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
    assertEquals(CrawlWindowRule.IGNORE, cw.canCrawl(testCal.getTime()));
  }

  public void testMatchExcl() {
    CrawlWindowRule cw =
        new CrawlWindowRules.BaseCrawlWindow(interval, excl, null);
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    assertEquals(CrawlWindowRule.IGNORE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
    assertEquals(CrawlWindowRule.EXCLUDE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
    assertEquals(CrawlWindowRule.EXCLUDE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
    assertEquals(CrawlWindowRule.EXCLUDE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
    assertEquals(CrawlWindowRule.IGNORE, cw.canCrawl(testCal.getTime()));
  }

  public void testMatchNoMatchIncl() {
    CrawlWindowRule cw =
        new CrawlWindowRules.BaseCrawlWindow(interval, notincl, null);
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    assertEquals(CrawlWindowRule.INCLUDE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
    assertEquals(CrawlWindowRule.IGNORE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
    assertEquals(CrawlWindowRule.IGNORE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
    assertEquals(CrawlWindowRule.IGNORE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
    assertEquals(CrawlWindowRule.INCLUDE, cw.canCrawl(testCal.getTime()));
  }

  public void testMatchNoMatchExcl() {
    CrawlWindowRule cw =
        new CrawlWindowRules.BaseCrawlWindow(interval, notexcl, null);
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    assertEquals(CrawlWindowRule.EXCLUDE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
    assertEquals(CrawlWindowRule.IGNORE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
    assertEquals(CrawlWindowRule.IGNORE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
    assertEquals(CrawlWindowRule.IGNORE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
    assertEquals(CrawlWindowRule.EXCLUDE, cw.canCrawl(testCal.getTime()));
  }

  public void testMatchInclElseExcl() {
    CrawlWindowRule cw =
        new CrawlWindowRules.BaseCrawlWindow(interval, inclexcl, null);
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    assertEquals(CrawlWindowRule.EXCLUDE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
    assertEquals(CrawlWindowRule.INCLUDE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
    assertEquals(CrawlWindowRule.INCLUDE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
    assertEquals(CrawlWindowRule.INCLUDE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
    assertEquals(CrawlWindowRule.EXCLUDE, cw.canCrawl(testCal.getTime()));
  }

  public void testMatchEnclElseIncl() {
    CrawlWindowRule cw =
        new CrawlWindowRules.BaseCrawlWindow(interval, exclincl, null);
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    assertEquals(CrawlWindowRule.INCLUDE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
    assertEquals(CrawlWindowRule.EXCLUDE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
    assertEquals(CrawlWindowRule.EXCLUDE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
    assertEquals(CrawlWindowRule.EXCLUDE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
    assertEquals(CrawlWindowRule.INCLUDE, cw.canCrawl(testCal.getTime()));
  }

  public void testTimeZone() {
    TimeZone gmt = TimeZone.getTimeZone("GMT");
    start = Calendar.getInstance(gmt);
    start.set(Calendar.HOUR_OF_DAY, 7);
    end = Calendar.getInstance(gmt);
    end.set(Calendar.HOUR_OF_DAY, 8);
    interval = new CrawlWindows.Interval(start, end, Calendar.HOUR_OF_DAY);

    testCal = Calendar.getInstance(gmt);
    // same time zone
    CrawlWindowRule cw =
        new CrawlWindowRules.BaseCrawlWindow(interval, incl, gmt);
    testCal.set(Calendar.HOUR_OF_DAY, 8);
    assertEquals(CrawlWindowRule.INCLUDE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.HOUR_OF_DAY, 6);
    assertEquals(CrawlWindowRule.IGNORE, cw.canCrawl(testCal.getTime()));

    // different time zone
    ((CrawlWindowRules.BaseCrawlWindow)cw).setServerTimeZone(
        TimeZone.getTimeZone("GMT+1:00"));
    testCal.set(Calendar.HOUR_OF_DAY, 8);
    assertEquals(CrawlWindowRule.IGNORE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.HOUR_OF_DAY, 6);
    assertEquals(CrawlWindowRule.INCLUDE, cw.canCrawl(testCal.getTime()));
  }

  public void testWindowSetNull() {
    try {
      CrawlWindowRule cw = new CrawlWindowRules.WindowSet(null);
      fail("CrawlWindowRules.WindowSet with null list should throw");
    } catch (NullPointerException e) { }
    CrawlWindowRule cw = new CrawlWindowRules.WindowSet(Collections.EMPTY_SET);
    assertEquals(CrawlWindowRule.IGNORE, cw.canCrawl(testCal.getTime()));
  }

  public void testWindowSetWrongClass() {
    try {
      CrawlWindowRule cw = new CrawlWindowRules.WindowSet(SetUtil.set("foo"));
      fail("CrawlWindowRules.WindowSet with list of non-CrawlWindows should throw");
    } catch (ClassCastException e) { }
  }

  public void testWindowSet() {
    Calendar cal1 = Calendar.getInstance();
    Calendar cal2 = Calendar.getInstance();
    cal1.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    cal2.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
    CrawlWindows.Interval interval2 =
        new CrawlWindows.Interval(cal1, cal2, Calendar.DAY_OF_WEEK);

    Set s = SetUtil.set(new CrawlWindowRules.BaseCrawlWindow(interval, incl, null),
                        new CrawlWindowRules.BaseCrawlWindow(interval2, excl, null));
    CrawlWindowRule cw = new CrawlWindowRules.WindowSet(s);

    testCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    assertEquals(CrawlWindowRule.EXCLUDE, cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
    assertEquals(CrawlWindowRule.EXCLUDE, cw.canCrawl(testCal.getTime()));

    testCal.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
    assertEquals(CrawlWindowRule.INCLUDE, cw.canCrawl(testCal.getTime()));

    testCal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
    assertEquals(CrawlWindowRule.IGNORE, cw.canCrawl(testCal.getTime()));

    try {
      cw.canCrawl(null);
      fail("CrawlWindowRules.WindowSet.canCrawl(null) should throw");
    } catch (NullPointerException e) { }
  }
}

