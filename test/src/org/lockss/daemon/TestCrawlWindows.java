/*
 * $Id: TestCrawlWindows.java,v 1.2 2003-10-24 07:40:25 eaalto Exp $
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
 * This is the test class for org.lockss.daemon.CrawlWindowRule.Window
 */
public class TestCrawlWindows extends LockssTestCase {
  Calendar start;
  Calendar end;
  Calendar testCal;

  public void setUp() throws Exception {
    super.setUp();

    start = Calendar.getInstance();
    end = Calendar.getInstance();
    testCal = Calendar.getInstance();
  }

  public void testTimeZone() {
    TimeZone gmt = TimeZone.getTimeZone("GMT");
    start = Calendar.getInstance(gmt);
    start.set(Calendar.HOUR_OF_DAY, 7);
    end = Calendar.getInstance(gmt);
    end.set(Calendar.HOUR_OF_DAY, 8);

    // same time zone
    CrawlWindow interval = new CrawlWindows.Interval(start, end,
        CrawlWindows.Interval.HOUR_OF_DAY, gmt);

    testCal = Calendar.getInstance(gmt);
    testCal.set(Calendar.HOUR_OF_DAY, 8);
    assertTrue(interval.canCrawl(testCal.getTime()));
    testCal.set(Calendar.HOUR_OF_DAY, 6);
    assertFalse(interval.canCrawl(testCal.getTime()));

    // different time zone
    ((CrawlWindows.BaseCrawlWindow)interval).setServerTimeZone(
        TimeZone.getTimeZone("GMT+1:00"));
    testCal.set(Calendar.HOUR_OF_DAY, 8);
    assertFalse(interval.canCrawl(testCal.getTime()));
    testCal.set(Calendar.HOUR_OF_DAY, 6);
    assertTrue(interval.canCrawl(testCal.getTime()));
  }

  public void testANDSetNull() {
    try {
      CrawlWindow cw = new CrawlWindows.AND(null);
      fail("CrawlWindows.AND with null list should throw");
    } catch (NullPointerException e) { }
    CrawlWindow cw = new CrawlWindows.AND(Collections.EMPTY_SET);
    assertTrue(cw.canCrawl(testCal.getTime()));
  }

  public void testORSetNull() {
    try {
      CrawlWindow cw = new CrawlWindows.OR(null);
      fail("CrawlWindows.OR with null list should throw");
    } catch (NullPointerException e) { }
    CrawlWindow cw = new CrawlWindows.OR(Collections.EMPTY_SET);
    assertFalse(cw.canCrawl(testCal.getTime()));
  }

  public void testNOTNull() {
    try {
      CrawlWindow cw = new CrawlWindows.NOT(null);
      fail("CrawlWindows.NOT with null window should throw");
    } catch (NullPointerException e) { }
  }

  public void testWindowSetWrongClass() {
    try {
      CrawlWindow cw = new CrawlWindows.AND(SetUtil.set("foo"));
      fail("CrawlWindows.WindowSet with list of non-CrawlWindows should throw");
    } catch (ClassCastException e) { }
  }

  public void testANDSet() {
    start.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
    end.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
    CrawlWindows.Interval interval = new CrawlWindows.Interval(start, end,
        CrawlWindows.Interval.DAY_OF_WEEK, null);

    Calendar cal1 = Calendar.getInstance();
    Calendar cal2 = Calendar.getInstance();
    cal1.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    cal2.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
    CrawlWindows.Interval interval2 = new CrawlWindows.Interval(cal1, cal2,
        CrawlWindows.Interval.DAY_OF_WEEK, null);

    Set s = SetUtil.set(interval, interval2);
    CrawlWindow cw = new CrawlWindows.AND(s);

    testCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    assertFalse(cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
    assertFalse(cw.canCrawl(testCal.getTime()));

    testCal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
    assertTrue(cw.canCrawl(testCal.getTime()));

    testCal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
    assertFalse(cw.canCrawl(testCal.getTime()));
  }

  public void testORSet() {
    start.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
    end.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
    CrawlWindows.Interval interval = new CrawlWindows.Interval(start, end,
        CrawlWindows.Interval.DAY_OF_WEEK, null);

    Calendar cal1 = Calendar.getInstance();
    Calendar cal2 = Calendar.getInstance();
    cal1.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    cal2.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
    CrawlWindows.Interval interval2 = new CrawlWindows.Interval(cal1, cal2,
        CrawlWindows.Interval.DAY_OF_WEEK, null);

    Set s = SetUtil.set(interval, interval2);
    CrawlWindow cw = new CrawlWindows.OR(s);

    testCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    assertTrue(cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
    assertTrue(cw.canCrawl(testCal.getTime()));

    testCal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
    assertTrue(cw.canCrawl(testCal.getTime()));

    testCal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
    assertFalse(cw.canCrawl(testCal.getTime()));
  }

  public void testNOTWindow() {
    start.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
    end.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
    CrawlWindows.Interval interval = new CrawlWindows.Interval(start, end,
        CrawlWindows.Interval.DAY_OF_WEEK, null);

    CrawlWindow cw = new CrawlWindows.NOT(interval);

    testCal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
    assertFalse(cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
    assertFalse(cw.canCrawl(testCal.getTime()));

    testCal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
    assertTrue(cw.canCrawl(testCal.getTime()));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    assertTrue(cw.canCrawl(testCal.getTime()));
  }

  public void testIntervalFieldStandard() {
    // Tue->Fri
    start.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
    end.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);

    CrawlWindows.Interval interval = new CrawlWindows.Interval(start, end,
        CrawlWindows.Interval.DAY_OF_WEEK, null);

    testCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    assertFalse(interval.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
    assertTrue(interval.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
    assertTrue(interval.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
    assertTrue(interval.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
    assertFalse(interval.isMatch(testCal));
  }

  public void testIntervalFieldInverse() {
    // invert interval
    // Fri->Tue
    start.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
    end.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
    CrawlWindows.Interval interval = new CrawlWindows.Interval(start, end,
        CrawlWindows.Interval.DAY_OF_WEEK, null);

    testCal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
    assertTrue(interval.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
    assertTrue(interval.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
    assertTrue(interval.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
    assertFalse(interval.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
    assertFalse(interval.isMatch(testCal));
  }

  public void testIntervalTimeStandard() {
    // interval from 7:30->15:45
    start.set(Calendar.HOUR_OF_DAY, 7);
    start.set(Calendar.MINUTE, 30);
    end.set(Calendar.HOUR_OF_DAY, 15);
    end.set(Calendar.MINUTE, 45);
    CrawlWindows.Interval interval = new CrawlWindows.Interval(start, end,
        CrawlWindows.Interval.TIME, null);

    testCal.set(Calendar.HOUR_OF_DAY, 6);
    assertFalse(interval.isMatch(testCal));
    testCal.set(Calendar.HOUR_OF_DAY, 7);
    testCal.set(Calendar.MINUTE, 25);
    assertFalse(interval.isMatch(testCal));
    testCal.set(Calendar.MINUTE, 30);
    assertTrue(interval.isMatch(testCal));
    testCal.set(Calendar.MINUTE, 47);
    assertTrue(interval.isMatch(testCal));
    testCal.set(Calendar.HOUR_OF_DAY, 12);
    assertTrue(interval.isMatch(testCal));
    testCal.set(Calendar.HOUR_OF_DAY, 15);
    testCal.set(Calendar.MINUTE, 5);
    assertTrue(interval.isMatch(testCal));
    testCal.set(Calendar.MINUTE, 44);
    assertTrue(interval.isMatch(testCal));
    testCal.set(Calendar.MINUTE, 45);
    assertFalse(interval.isMatch(testCal));
    testCal.set(Calendar.HOUR_OF_DAY, 17);
    testCal.set(Calendar.MINUTE, 0);
    assertFalse(interval.isMatch(testCal));
  }

  public void testIntervalTimeInverse() {
    // interval from 15:30->7:45
    start.set(Calendar.HOUR_OF_DAY, 15);
    start.set(Calendar.MINUTE, 30);
    end.set(Calendar.HOUR_OF_DAY, 7);
    end.set(Calendar.MINUTE, 45);

    CrawlWindows.Interval interval = new CrawlWindows.Interval(start, end,
        CrawlWindows.Interval.TIME, null);

    testCal.set(Calendar.HOUR_OF_DAY, 14);
    assertFalse(interval.isMatch(testCal));
    testCal.set(Calendar.HOUR_OF_DAY, 15);
    testCal.set(Calendar.MINUTE, 25);
    assertFalse(interval.isMatch(testCal));
    testCal.set(Calendar.MINUTE, 30);
    assertTrue(interval.isMatch(testCal));
    testCal.set(Calendar.MINUTE, 47);
    assertTrue(interval.isMatch(testCal));
    testCal.set(Calendar.HOUR_OF_DAY, 17);
    assertTrue(interval.isMatch(testCal));
    testCal.set(Calendar.HOUR_OF_DAY, 1);
    assertTrue(interval.isMatch(testCal));
    testCal.set(Calendar.HOUR_OF_DAY, 7);
    testCal.set(Calendar.MINUTE, 5);
    assertTrue(interval.isMatch(testCal));
    testCal.set(Calendar.MINUTE, 44);
    assertTrue(interval.isMatch(testCal));
    testCal.set(Calendar.MINUTE, 45);
    assertFalse(interval.isMatch(testCal));
    testCal.set(Calendar.HOUR_OF_DAY, 8);
    testCal.set(Calendar.MINUTE, 0);
    assertFalse(interval.isMatch(testCal));
  }

  public void testMultipleFields() {
    // Tue->Fri, 1st week of month
    start.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
    end.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
    start.set(Calendar.WEEK_OF_MONTH, 1);
    end.set(Calendar.WEEK_OF_MONTH, 1);

    CrawlWindows.Interval interval = new CrawlWindows.Interval(start, end,
        CrawlWindows.Interval.WEEK_OF_MONTH + CrawlWindows.Interval.DAY_OF_WEEK,
        null);

    testCal.set(Calendar.WEEK_OF_MONTH, 1);
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    assertFalse(interval.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
    assertTrue(interval.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
    assertTrue(interval.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
    assertTrue(interval.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
    assertFalse(interval.isMatch(testCal));

    testCal.set(Calendar.WEEK_OF_MONTH, 2);
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    assertFalse(interval.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
    assertFalse(interval.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
    assertFalse(interval.isMatch(testCal));
  }

  public void testANDIntervals() {
    // interval from 7->15
    start.set(Calendar.HOUR_OF_DAY, 7);
    end.set(Calendar.HOUR_OF_DAY, 15);
    end.set(Calendar.MINUTE, 0);
    CrawlWindows.Interval interval = new CrawlWindows.Interval(start, end,
        CrawlWindows.Interval.TIME, null);

    // interval from 7:30->15:45
    Calendar start2 = Calendar.getInstance();
    start2.set(Calendar.DAY_OF_MONTH, 1);
    Calendar end2 = Calendar.getInstance();
    end2.set(Calendar.DAY_OF_MONTH, 5);
    CrawlWindows.Interval interval2 = new CrawlWindows.Interval(start2, end2,
        CrawlWindows.Interval.DAY_OF_MONTH, null);

    Set intSet = SetUtil.set(interval, interval2);
    CrawlWindows.AND andPair = new CrawlWindows.AND(intSet);
    CrawlWindows.OR orPair = new CrawlWindows.OR(intSet);

    // both true
    testCal.set(Calendar.HOUR_OF_DAY, 8);
    testCal.set(Calendar.DAY_OF_MONTH, 1);
    assertTrue(andPair.canCrawl(testCal.getTime()));
    assertTrue(orPair.canCrawl(testCal.getTime()));

    // one false
    testCal.set(Calendar.HOUR_OF_DAY, 6);
    assertFalse(andPair.canCrawl(testCal.getTime()));
    assertTrue(orPair.canCrawl(testCal.getTime()));
    testCal.set(Calendar.HOUR_OF_DAY, 8);
    testCal.set(Calendar.DAY_OF_MONTH, 16);
    assertFalse(andPair.canCrawl(testCal.getTime()));
    assertTrue(orPair.canCrawl(testCal.getTime()));

    // both false
    testCal.set(Calendar.HOUR_OF_DAY, 6);
    testCal.set(Calendar.DAY_OF_MONTH, 16);
    assertFalse(andPair.canCrawl(testCal.getTime()));
    assertFalse(orPair.canCrawl(testCal.getTime()));
  }

  public void testFieldEnum() {
    // M,W,F enum
    Calendar cal1 = Calendar.getInstance();
    Calendar cal2 = Calendar.getInstance();
    Calendar cal3 = Calendar.getInstance();
    cal1.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    cal2.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
    cal3.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);

    CrawlWindows.FieldEnum fieldEnum =
        new CrawlWindows.FieldEnum(SetUtil.set(cal1, cal2, cal3),
                                   CrawlWindows.Interval.DAY_OF_WEEK, null);
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    assertTrue(fieldEnum.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
    assertFalse(fieldEnum.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
    assertTrue(fieldEnum.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
    assertFalse(fieldEnum.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
    assertTrue(fieldEnum.isMatch(testCal));
  }

  public void testMultipleFieldEnum() {
    // M-1st,W-2nd enum
    Calendar cal1 = Calendar.getInstance();
    Calendar cal2 = Calendar.getInstance();
    cal1.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    cal1.set(Calendar.WEEK_OF_MONTH, 1);
    cal2.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
    cal2.set(Calendar.WEEK_OF_MONTH, 2);

    CrawlWindows.FieldEnum fieldEnum =
        new CrawlWindows.FieldEnum(SetUtil.set(cal1, cal2),
                                   CrawlWindows.Interval.DAY_OF_WEEK +
                                   CrawlWindows.Interval.WEEK_OF_MONTH, null);

    testCal.set(Calendar.WEEK_OF_MONTH, 1);
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    assertTrue(fieldEnum.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
    assertFalse(fieldEnum.isMatch(testCal));

    testCal.set(Calendar.WEEK_OF_MONTH, 2);
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    assertFalse(fieldEnum.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
    assertTrue(fieldEnum.isMatch(testCal));
  }
}

