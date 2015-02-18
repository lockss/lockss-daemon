/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;
import java.text.*;
import org.lockss.util.*;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockCrawlWindow;

/**
 * This is the test class for org.lockss.daemon.CrawlWindowRule.Window
 */
public class TestCrawlWindows extends LockssTestCase {
  
  Calendar start;
  Calendar end;
  Calendar testCal;

  protected static final TimeZone GMT = TimeZone.getTimeZone("GMT");
  protected static final TimeZone GMT_PLUS_1 = TimeZone.getTimeZone("GMT+1:00");
  protected static final TimeZone EASTERN = TimeZone.getTimeZone("US/Eastern");
  protected static final TimeZone PACIFIC = TimeZone.getTimeZone("US/Pacific");

  public void setUp() throws Exception {
    super.setUp();
    start = Calendar.getInstance();
    end = Calendar.getInstance();
    testCal = Calendar.getInstance();
  }

  public void testDefaultTimeZone() {
    CrawlWindows.BaseCalCrawlWindow interval =
        new CrawlWindows.Interval(start, end, CrawlWindows.HOUR_OF_DAY, null);
    assertEquals(TimeZone.getDefault().getID(), interval.getTimeZoneId());
  }

  public void testTimeZone() {
    setTime(start, GMT, 11, 30);
    setTime(end, GMT, 12, 31); // end point non-inclusive: end a minute after
    
    CrawlWindows.BaseCalCrawlWindow interval = null;
    
    // From 11:30am to 12:30pm inclusive GMT
    interval = new CrawlWindows.Interval(start, end, CrawlWindows.TIME, GMT);
    setTime(testCal, GMT, 11, 29);
    assertFalse(interval.canCrawl(testCal.getTime()));
    setTime(testCal, GMT, 11, 30);
    assertTrue(interval.canCrawl(testCal.getTime()));
    setTime(testCal, GMT, 12, 30);
    assertTrue(interval.canCrawl(testCal.getTime()));
    setTime(testCal, GMT, 12, 31);
    assertFalse(interval.canCrawl(testCal.getTime()));

    // From 11:30am to 12:30pm inclusive GMT+1:00 (start and end were GMT)
    interval = new CrawlWindows.Interval(start, end, CrawlWindows.TIME, GMT_PLUS_1);
    // which is from 10:30am to 11:30am inclusive GMT
    setTime(testCal, GMT, 10, 29);
    assertFalse(interval.canCrawl(testCal.getTime()));
    setTime(testCal, GMT, 10, 30);
    assertTrue(interval.canCrawl(testCal.getTime()));
    setTime(testCal, GMT, 11, 30);
    assertTrue(interval.canCrawl(testCal.getTime()));
    setTime(testCal, GMT, 11, 31);
    assertFalse(interval.canCrawl(testCal.getTime()));
    
    // From 11:30am to 12:30pm inclusive GMT
    interval = new CrawlWindows.Interval(start, end, CrawlWindows.TIME, GMT);
    // which is from 6:30am to 7:30am inclusive EST
    setDate(testCal, EASTERN, 2014, Calendar.JANUARY, 1);
    setTime(testCal, EASTERN, 6, 29);
    assertFalse(interval.canCrawl(testCal.getTime()));
    setTime(testCal, EASTERN, 6, 30);
    assertTrue(interval.canCrawl(testCal.getTime()));
    setTime(testCal, EASTERN, 7, 30);
    assertTrue(interval.canCrawl(testCal.getTime()));
    setTime(testCal, EASTERN, 7, 31);
    assertFalse(interval.canCrawl(testCal.getTime()));
    // ...and from 7:30am to 8:30am inclusive EDT
    setDate(testCal, EASTERN, 2014, Calendar.JULY, 1);
    setTime(testCal, EASTERN, 7, 29);
    assertFalse(interval.canCrawl(testCal.getTime()));
    setTime(testCal, EASTERN, 7, 30);
    assertTrue(interval.canCrawl(testCal.getTime()));
    setTime(testCal, EASTERN, 8, 30);
    assertTrue(interval.canCrawl(testCal.getTime()));
    setTime(testCal, EASTERN, 8, 31);
    assertFalse(interval.canCrawl(testCal.getTime()));

    // From 11:30am to 12:30pm inclusive US/Eastern (start and end were GMT)
    interval = new CrawlWindows.Interval(start, end, CrawlWindows.TIME, EASTERN);
    // which is from 4:30pm to 5:30pm inclusive GMT in Winter
    setDate(testCal, GMT, 2014, Calendar.JANUARY, 1);
    setTime(testCal, GMT, 16, 29);
    assertFalse(interval.canCrawl(testCal.getTime()));
    setTime(testCal, GMT, 16, 30);
    assertTrue(interval.canCrawl(testCal.getTime()));
    setTime(testCal, GMT, 17, 30);
    assertTrue(interval.canCrawl(testCal.getTime()));
    setTime(testCal, GMT, 17, 31);
    assertFalse(interval.canCrawl(testCal.getTime()));
    // ...and from 3:30pm to 4:30pm inclusive GMT in Summer
    setDate(testCal, GMT, 2014, Calendar.JULY, 1);
    setTime(testCal, GMT, 15, 29);
    assertFalse(interval.canCrawl(testCal.getTime()));
    setTime(testCal, GMT, 15, 30);
    assertTrue(interval.canCrawl(testCal.getTime()));
    setTime(testCal, GMT, 16, 30);
    assertTrue(interval.canCrawl(testCal.getTime()));
    setTime(testCal, GMT, 16, 31);
    assertFalse(interval.canCrawl(testCal.getTime()));
    
    // From 11:30am to 12:30pm inclusive US/Eastern (start and end were GMT)
    interval = new CrawlWindows.Interval(start, end, CrawlWindows.TIME, EASTERN);
    // which is from 8:30am to 9:30am inclusive US/Pacific in Winter
    setDate(testCal, PACIFIC, 2014, Calendar.JANUARY, 1);
    setTime(testCal, PACIFIC, 8, 29);
    assertFalse(interval.canCrawl(testCal.getTime()));
    setTime(testCal, PACIFIC, 8, 30);
    assertTrue(interval.canCrawl(testCal.getTime()));
    setTime(testCal, PACIFIC, 9, 30);
    assertTrue(interval.canCrawl(testCal.getTime()));
    setTime(testCal, PACIFIC, 9, 31);
    assertFalse(interval.canCrawl(testCal.getTime()));
    // ...and also in Summer
    setDate(testCal, PACIFIC, 2014, Calendar.JULY, 1);
    setTime(testCal, PACIFIC, 8, 29);
    assertFalse(interval.canCrawl(testCal.getTime()));
    setTime(testCal, PACIFIC, 8, 30);
    assertTrue(interval.canCrawl(testCal.getTime()));
    setTime(testCal, PACIFIC, 9, 30);
    assertTrue(interval.canCrawl(testCal.getTime()));
    setTime(testCal, PACIFIC, 9, 31);
    assertFalse(interval.canCrawl(testCal.getTime()));
  }

  public void testAndSetNull() {
    try {
      CrawlWindow cw = new CrawlWindows.And(null);
      fail("CrawlWindows.And with null list should throw");
    }
    catch (NullPointerException expected) {
      // Expected
    }
    CrawlWindow cw = new CrawlWindows.And(Collections.EMPTY_SET);
    assertTrue(cw.canCrawl(testCal.getTime()));
  }

  public void testOrSetNull() {
    try {
      CrawlWindow cw = new CrawlWindows.Or(null);
      fail("CrawlWindows.Or with null list should throw");
    }
    catch (NullPointerException expected) {
      // Expected
    }
    CrawlWindow cw = new CrawlWindows.Or(Collections.EMPTY_SET);
    assertFalse(cw.canCrawl(testCal.getTime()));
  }

  public void testNotNull() {
    try {
      CrawlWindow cw = new CrawlWindows.Not(null);
      fail("CrawlWindows.Not with null window should throw");
    }
    catch (NullPointerException expected) {
      // Expected
    }
  }

  public void testWindowSetWrongClass() {
    try {
      CrawlWindow cw = new CrawlWindows.And(SetUtil.set("foo"));
      fail("CrawlWindows.WindowSet with set of non-CrawlWindows should throw");
    }
    catch (ClassCastException expected) {
      // Expected
    }
  }

  public void testAndSet() {
    MockCrawlWindow win1 = new MockCrawlWindow();
    win1.setAllowCrawl(false);
    MockCrawlWindow win2 = new MockCrawlWindow();
    win2.setAllowCrawl(false);

    Set s = SetUtil.set(win1, win2);
    CrawlWindow andWin = new CrawlWindows.And(s);
    assertFalse(andWin.canCrawl(testCal.getTime()));
    win1.setAllowCrawl(true);
    assertFalse(andWin.canCrawl(testCal.getTime()));
    win1.setAllowCrawl(false);
    win2.setAllowCrawl(true);
    assertFalse(andWin.canCrawl(testCal.getTime()));
    win1.setAllowCrawl(true);
    assertTrue(andWin.canCrawl(testCal.getTime()));
  }

  public void testOrSet() {
    MockCrawlWindow win1 = new MockCrawlWindow();
    win1.setAllowCrawl(false);
    MockCrawlWindow win2 = new MockCrawlWindow();
    win2.setAllowCrawl(false);

    Set s = SetUtil.set(win1, win2);
    CrawlWindow orWin = new CrawlWindows.Or(s);
    assertFalse(orWin.canCrawl(testCal.getTime()));
    win1.setAllowCrawl(true);
    assertTrue(orWin.canCrawl(testCal.getTime()));
    win1.setAllowCrawl(false);
    win2.setAllowCrawl(true);
    assertTrue(orWin.canCrawl(testCal.getTime()));
    win1.setAllowCrawl(true);
    assertTrue(orWin.canCrawl(testCal.getTime()));
  }

  public void testNotWindow() {
    MockCrawlWindow win1 = new MockCrawlWindow();
    win1.setAllowCrawl(false);

    CrawlWindow notWin = new CrawlWindows.Not(win1);
    assertTrue(notWin.canCrawl(testCal.getTime()));
    win1.setAllowCrawl(true);
    assertFalse(notWin.canCrawl(testCal.getTime()));
  }

  public void testIntervalFieldStandard() {
    // Tue->Fri
    start.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
    end.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);

    CrawlWindows.Interval interval = new CrawlWindows.Interval(start, end,
        CrawlWindows.DAY_OF_WEEK, null);

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

  public void testIntervalFieldWrapAround() {
    // invert interval
    // Fri->Tue
    start.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
    end.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
    CrawlWindows.Interval interval = new CrawlWindows.Interval(start, end,
        CrawlWindows.DAY_OF_WEEK, null);

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

  /**
   * 
   */
  public void testIntervalTimeStandard() {
    // interval from 7:30->15:45
    setTime(start, 7, 30);
    setTime(end, 15, 45);
    CrawlWindows.Interval interval = new CrawlWindows.Interval(start, end,
        CrawlWindows.TIME, null);

    setTime(testCal, 6, 0);
    assertFalse(interval.isMatch(testCal));
    setTime(testCal, 7, 25);
    assertFalse(interval.isMatch(testCal));
    setTime(testCal, 7, 30);
    assertTrue(interval.isMatch(testCal));
    setTime(testCal, 7, 47);
    assertTrue(interval.isMatch(testCal));
    setTime(testCal, 12, 47);
    assertTrue(interval.isMatch(testCal));
    setTime(testCal, 15, 5);
    assertTrue(interval.isMatch(testCal));
    setTime(testCal, 15, 44);
    assertTrue(interval.isMatch(testCal));
    setTime(testCal, 15, 45);
    assertFalse(interval.isMatch(testCal));
    setTime(testCal, 17, 0);
    assertFalse(interval.isMatch(testCal));
  }

  public void testIntervalTimeWrapAround() {
    // interval from 15:30->7:45
    setTime(start, 15, 30);
    setTime(end, 7, 45);
    CrawlWindows.Interval interval = new CrawlWindows.Interval(start, end,
        CrawlWindows.TIME, null);

    setTime(testCal, 14, 0);
    assertFalse(interval.isMatch(testCal));
    setTime(testCal, 15, 25);
    assertFalse(interval.isMatch(testCal));
    setTime(testCal, 15, 30);
    assertTrue(interval.isMatch(testCal));
    setTime(testCal, 15, 47);
    assertTrue(interval.isMatch(testCal));
    setTime(testCal, 17, 47);
    assertTrue(interval.isMatch(testCal));
    setTime(testCal, 1, 47);
    assertTrue(interval.isMatch(testCal));
    setTime(testCal, 7, 5);
    assertTrue(interval.isMatch(testCal));
    setTime(testCal, 7, 44);
    assertTrue(interval.isMatch(testCal));
    setTime(testCal, 7, 45);
    assertFalse(interval.isMatch(testCal));
    setTime(testCal, 8, 0);
    assertFalse(interval.isMatch(testCal));
  }

  public void testMultipleFields() {
    // Tue->Fri, 1st week of month
    start.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
    end.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
    start.set(Calendar.WEEK_OF_MONTH, 1);
    end.set(Calendar.WEEK_OF_MONTH, 1);

    CrawlWindows.Interval interval = new CrawlWindows.Interval(start, end,
        CrawlWindows.WEEK_OF_MONTH + CrawlWindows.DAY_OF_WEEK,
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

  public void testAndIntervals() {
    // interval from 7->15
    start.set(Calendar.HOUR_OF_DAY, 7);
    end.set(Calendar.HOUR_OF_DAY, 15);
    end.set(Calendar.MINUTE, 0);
    CrawlWindows.Interval interval = new CrawlWindows.Interval(start, end,
        CrawlWindows.TIME, null);

    // interval from 7:30->15:45
    Calendar start2 = Calendar.getInstance();
    start2.set(Calendar.DAY_OF_MONTH, 1);
    Calendar end2 = Calendar.getInstance();
    end2.set(Calendar.DAY_OF_MONTH, 5);
    CrawlWindows.Interval interval2 = new CrawlWindows.Interval(start2, end2,
        CrawlWindows.DAY_OF_MONTH, null);

    Set intSet = SetUtil.set(interval, interval2);
    CrawlWindows.And andPair = new CrawlWindows.And(intSet);
    CrawlWindows.Or orPair = new CrawlWindows.Or(intSet);

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

  public void testFieldSet() {
    // M,W,F enum
    Calendar cal1 = Calendar.getInstance();
    Calendar cal2 = Calendar.getInstance();
    Calendar cal3 = Calendar.getInstance();
    cal1.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    cal2.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
    cal3.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);

    CrawlWindows.FieldSet fieldSet =
        new CrawlWindows.FieldSet(SetUtil.set(cal1, cal2, cal3),
                                   CrawlWindows.DAY_OF_WEEK, null);
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    assertTrue(fieldSet.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
    assertFalse(fieldSet.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
    assertTrue(fieldSet.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
    assertFalse(fieldSet.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
    assertTrue(fieldSet.isMatch(testCal));
  }

  public void testMultipleFieldSet() {
    // M-1st,W-2nd enum
    Calendar cal1 = Calendar.getInstance();
    Calendar cal2 = Calendar.getInstance();
    cal1.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    cal1.set(Calendar.WEEK_OF_MONTH, 1);
    cal2.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
    cal2.set(Calendar.WEEK_OF_MONTH, 2);

    CrawlWindows.FieldSet fieldSet =
        new CrawlWindows.FieldSet(SetUtil.set(cal1, cal2),
                                   CrawlWindows.DAY_OF_WEEK +
                                   CrawlWindows.WEEK_OF_MONTH, null);

    testCal.set(Calendar.WEEK_OF_MONTH, 1);
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    assertTrue(fieldSet.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
    assertFalse(fieldSet.isMatch(testCal));

    testCal.set(Calendar.WEEK_OF_MONTH, 2);
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    assertFalse(fieldSet.isMatch(testCal));
    testCal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
    assertTrue(fieldSet.isMatch(testCal));
  }

  public void testGetCrawlIntervals() {
    /*
     * This test is fragile. TimeInterval is implemented as a pair of doubles
     * which are epoch milliseconds, but the granularity of getCrawlIntervals()
     * is one minute and the granularity of crawl windows is one second.
     * Removing any of the alignments on 'start' introduces mysterious
     * intermittent failures between the 'expected' list and the 'actual' list
     * of TimeIntervals that end up being noise in the milliseconds.
     */
    
    // 1:55pm->2:00pm
    setTime(start, 13, 55);
    end.setTime(start.getTime());
    setTime(end, 14, 0);
    CrawlWindow interval = new CrawlWindows.Interval(start, end, CrawlWindows.TIME, null);
    
    // 2:05pm->2:15pm
    Calendar start2 = Calendar.getInstance();
    start2.setTime(start.getTime());
    setTime(start2, 14, 5);
    Calendar end2 = Calendar.getInstance();
    end2.setTime(start.getTime());
    setTime(end2, 14, 15);
    CrawlWindow interval2 = new CrawlWindows.Interval(start2, end2, CrawlWindows.TIME, null);

    // Union of 1:55pm->2:00pm and 2:05pm->2:15pm
    CrawlWindow orWin = new CrawlWindows.Or(SetUtil.set(interval, interval2));

    // 1:50pm->2:10pm
    Calendar start3 = Calendar.getInstance();
    start3.setTime(start.getTime());
    setTime(start3, 13, 50);
    Calendar end3 = Calendar.getInstance();
    end3.setTime(start.getTime());
    setTime(end3, 14, 10);

    // The expected crawl intervals in the union of 1:55pm->2:00pm and
    // 2:05pm->2:15pm overlapping with 1:50pm->2:10pm are 1:55pm->2:00pm and
    // 2:05->2:10pm, for a total of 10 minutes
    List expectedList = ListUtil.list(
        new TimeInterval(start.getTime(), end.getTime()),
        // last interval ends early
        new TimeInterval(start2.getTime(), end3.getTime()));
    List results = CrawlWindows.getCrawlIntervals(orWin,
                                                  start3.getTime(),
                                                  end3.getTime());
    assertIsomorphic(expectedList, results);
    long numMinutes = TimeInterval.getTotalTime(results) / (Constants.MINUTE);
    assertEquals(10, numMinutes);
  }

  public void testDaily() throws Exception {
    CrawlWindows.Daily win;

    // Open from 2:00am to 7:00am GMT
    SimpleDateFormat gmtSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    gmtSdf.setTimeZone(GMT);
    win = new CrawlWindows.Daily("2:00", "7:00", GMT.getID());
    assertEquals("Daily from 2:00 to 7:00, GMT", win.toString());
    assertFalse(win.canCrawl(gmtSdf.parse("2014-01-01 00:00")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-07-01 00:00")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-01-01 01:59")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-07-01 01:59")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-01-01 02:00")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-07-01 02:00")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-01-01 06:59")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-07-01 06:59")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-01-01 07:00")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-07-01 07:00")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-01-01 23:59")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-07-01 23:59")));

    // Open from 7:00am to 10:00pm GMT+01:00
    // which is 6am-9pm GMT
    win = new CrawlWindows.Daily("7:00", "22:00", GMT_PLUS_1.getID());
    assertEquals("Daily from 7:00 to 22:00, GMT+01:00", win.toString());
    assertFalse(win.canCrawl(gmtSdf.parse("2014-01-01 00:00")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-07-01 00:00")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-01-01 05:59")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-07-01 05:59")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-01-01 06:00")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-07-01 06:00")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-01-01 20:59")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-07-01 20:59")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-01-01 21:00")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-07-01 21:00")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-01-01 23:59")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-07-01 23:59")));

    // Open from 10:00pm to 6:00am GMT
    win = new CrawlWindows.Daily("22:00", "6:00", GMT.getID());
    assertEquals("Daily from 22:00 to 6:00, GMT", win.toString());
    assertTrue(win.canCrawl(gmtSdf.parse("2014-01-01 00:00")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-07-01 00:00")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-01-01 05:59")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-07-01 05:59")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-01-01 06:00")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-07-01 06:00")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-01-01 21:59")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-07-01 21:59")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-01-01 22:00")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-07-01 22:00")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-01-01 23:59")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-07-01 23:59")));

    // Open from 12:30am to 3:30am US/Eastern
    // which is 5:30-8:30am in Winter (EST)
    // and 4:30-7:30am in Summer (EDT)
    SimpleDateFormat easternSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    easternSdf.setTimeZone(EASTERN);
    win = new CrawlWindows.Daily("0:30", "3:30", EASTERN.getID());
    assertEquals("Daily from 0:30 to 3:30, US/Eastern", win.toString());
    // EST
    assertFalse(win.canCrawl(easternSdf.parse("2014-01-01 00:29")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-01-01 05:29")));
    assertTrue(win.canCrawl(easternSdf.parse("2014-01-01 00:30")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-01-01 05:30")));
    assertTrue(win.canCrawl(easternSdf.parse("2014-01-01 03:29")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-01-01 08:29")));
    assertFalse(win.canCrawl(easternSdf.parse("2014-01-01 03:30")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-01-01 08:30")));
    // EDT
    assertFalse(win.canCrawl(easternSdf.parse("2014-07-01 00:29")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-07-01 04:29")));
    assertTrue(win.canCrawl(easternSdf.parse("2014-07-01 00:30")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-07-01 04:30")));
    assertTrue(win.canCrawl(easternSdf.parse("2014-07-01 03:29")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-07-01 07:29")));
    assertFalse(win.canCrawl(easternSdf.parse("2014-07-01 03:30")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-07-01 07:30")));
    // Spring forward (on March 9, 2014)
    // In US/Eastern, the boundaries are 3 hours apart
    // In GMT, one can see that this window is actually only 2 hours long
    assertFalse(win.canCrawl(easternSdf.parse("2014-03-09 00:29")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-03-09 05:29")));
    assertTrue(win.canCrawl(easternSdf.parse("2014-03-09 00:30")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-03-09 05:30")));
    assertTrue(win.canCrawl(easternSdf.parse("2014-03-09 03:29")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-03-09 07:29")));
    assertFalse(win.canCrawl(easternSdf.parse("2014-03-09 03:30")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-03-09 07:30")));
    // Fall back (on November 2, 2014)
    // In US/Eastern, the boundaries are 3 hours apart
    // In GMT, one can see that this window is actually 4 hours long
    assertFalse(win.canCrawl(easternSdf.parse("2014-11-02 00:29")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-11-02 04:29")));
    assertTrue(win.canCrawl(easternSdf.parse("2014-11-02 00:30")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-11-02 04:30")));
    assertTrue(win.canCrawl(easternSdf.parse("2014-11-02 03:29")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-11-02 08:29")));
    assertFalse(win.canCrawl(easternSdf.parse("2014-11-02 03:30")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-11-02 08:30")));
  }

  public void testDailyWithDays() throws Exception {
    SimpleDateFormat gmtSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    gmtSdf.setTimeZone(GMT);
    CrawlWindows.Daily win;
    win = new CrawlWindows.Daily("7:00", "22:00", null, GMT.getID());
    assertEquals("Daily from 7:00 to 22:00, GMT", win.toString());

    try {
      new CrawlWindows.Daily("7:00", "22:00", "", GMT.getID());
    }
    catch (IllegalArgumentException expected) {
      // Expected
    }

    try {
      new CrawlWindows.Daily("7:00", "22:00", "not;days", GMT.getID());
    }
    catch (IllegalArgumentException expected) {
      // Expected
    }

    // weekdays
    win = new CrawlWindows.Daily("7:00", "22:00", "2;3;4;5;6", GMT.getID());
    assertEquals("Days 2;3;4;5;6 from 7:00 to 22:00, GMT", win.toString());
    // 2014-08-11 is a Monday
    for (int d = 11 /* Monday */ ; d <= 17 /* Sunday */ ; ++d) {
      assertFalse(win.canCrawl(gmtSdf.parse("2014-08-" + d + " 06:59")));
      assertEquals(d <= 15 /* Monday-Friday */, win.canCrawl(gmtSdf.parse("2014-08-" + d + " 07:00")));
      assertEquals(d <= 15 /* Monday-Friday */, win.canCrawl(gmtSdf.parse("2014-08-" + d + " 21:59")));
      assertFalse(win.canCrawl(gmtSdf.parse("2014-08-" + d + " 22:00")));
    }

    // weekends
    win = new CrawlWindows.Daily("7:00", "22:00", "1;7;1", GMT.getID());
    assertEquals("Days 1;7 from 7:00 to 22:00, GMT", win.toString());
    // 2014-08-11 is a Monday
    for (int d = 11 /* Monday */ ; d <= 17 /* Sunday */ ; ++d) {
      assertFalse(win.canCrawl(gmtSdf.parse("2014-08-" + d + " 06:59")));
      assertEquals(d >= 16 /* Saturday-Sunday */, win.canCrawl(gmtSdf.parse("2014-08-" + d + " 07:00")));
      assertEquals(d >= 16 /* Saturday-Sunday */, win.canCrawl(gmtSdf.parse("2014-08-" + d + " 21:59")));
      assertFalse(win.canCrawl(gmtSdf.parse("2014-08-" + d + " 22:00")));
    }
    
    // Eastern
    SimpleDateFormat easternSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    easternSdf.setTimeZone(EASTERN);
    win = new CrawlWindows.Daily("7:00", "8:00", "2;3;4;5;6", EASTERN.getID());
    assertEquals("Days 2;3;4;5;6 from 7:00 to 8:00, US/Eastern", win.toString());
    // 2014-01-13 is a Monday (7-8am Eastern = noon-1pm GMT)
    for (int d = 13 /* Monday */ ; d <= 19 /* Sunday */ ; ++d) {
      assertFalse(win.canCrawl(easternSdf.parse("2014-01-" + d + " 06:59")));
      assertFalse(win.canCrawl(gmtSdf.parse("2014-01-" + d + " 11:59")));
      assertEquals(d <= 17 /* Monday-Friday */, win.canCrawl(easternSdf.parse("2014-01-" + d + " 07:00")));
      assertEquals(d <= 17 /* Monday-Friday */, win.canCrawl(gmtSdf.parse("2014-01-" + d + " 12:00")));
      assertEquals(d <= 17 /* Monday-Friday */, win.canCrawl(easternSdf.parse("2014-01-" + d + " 07:59")));
      assertEquals(d <= 17 /* Monday-Friday */, win.canCrawl(gmtSdf.parse("2014-01-" + d + " 12:59")));
      assertFalse(win.canCrawl(easternSdf.parse("2014-01-" + d + " 08:00")));
      assertFalse(win.canCrawl(gmtSdf.parse("2014-01-" + d + " 13:00")));
    }
    // 2014-08-11 is a Monday (7-8am Eastern = 11am-noon GMT)
    for (int d = 11 /* Monday */ ; d <= 17 /* Sunday */ ; ++d) {
      assertFalse(win.canCrawl(easternSdf.parse("2014-08-" + d + " 06:59")));
      assertFalse(win.canCrawl(gmtSdf.parse("2014-08-" + d + " 10:59")));
      assertEquals(d <= 15 /* Monday-Friday */, win.canCrawl(easternSdf.parse("2014-08-" + d + " 07:00")));
      assertEquals(d <= 15 /* Monday-Friday */, win.canCrawl(gmtSdf.parse("2014-08-" + d + " 11:00")));
      assertEquals(d <= 15 /* Monday-Friday */, win.canCrawl(easternSdf.parse("2014-08-" + d + " 07:59")));
      assertEquals(d <= 15 /* Monday-Friday */, win.canCrawl(gmtSdf.parse("2014-08-" + d + " 11:59")));
      assertFalse(win.canCrawl(easternSdf.parse("2014-08-" + d + " 08:00")));
      assertFalse(win.canCrawl(gmtSdf.parse("2014-08-" + d + " 12:00")));
    }

  }

  static String daily1 =
    "<org.lockss.daemon.CrawlWindows-Daily>\n" +
    "  <from>8:00</from>\n" +
    "  <to>22:00</to>\n" +
    "  <timeZoneId>US/Eastern</timeZoneId>\n" +
    "</org.lockss.daemon.CrawlWindows-Daily>\n";

  static String daily2 =
    "<org.lockss.daemon.CrawlWindows-Daily>\n" +
    "  <from>20:00</from>\n" +
    "  <to>6:00</to>\n" +
    "  <timeZoneId>GMT</timeZoneId>\n" +
    "  <daysOfWeek>2;3;4;5;6</daysOfWeek>\n" +
    "</org.lockss.daemon.CrawlWindows-Daily>\n";

  CrawlWindow deserWindow(String input) throws Exception {
    ObjectSerializer deser = new XStreamSerializer(getMockLockssDaemon());
    return (CrawlWindow)deser.deserialize(new StringReader(input));
  }

  public void testDeserDaily() throws Exception {
    SimpleDateFormat gmtSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    gmtSdf.setTimeZone(GMT);
    SimpleDateFormat easternSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    easternSdf.setTimeZone(EASTERN);
    CrawlWindows.Daily win;
    
    win = (CrawlWindows.Daily)deserWindow(daily1);
    assertEquals("Daily from 8:00 to 22:00, US/Eastern", win.toString());
    // ...which is 1pm-3am GMT in Winter
    assertFalse(win.canCrawl(easternSdf.parse("2014-01-01 07:59")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-01-01 12:59")));
    assertTrue(win.canCrawl(easternSdf.parse("2014-01-01 08:00")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-01-01 13:00")));
    assertTrue(win.canCrawl(easternSdf.parse("2014-01-01 21:59")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-01-02 02:59")));
    assertFalse(win.canCrawl(easternSdf.parse("2014-01-01 22:00")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-01-02 03:00")));
    // ...and noon-2am GMT in Summer
    assertFalse(win.canCrawl(easternSdf.parse("2014-07-01 07:59")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-07-01 11:59")));
    assertTrue(win.canCrawl(easternSdf.parse("2014-07-01 08:00")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-07-01 12:00")));
    assertTrue(win.canCrawl(easternSdf.parse("2014-07-01 21:59")));
    assertTrue(win.canCrawl(gmtSdf.parse("2014-07-02 01:59")));
    assertFalse(win.canCrawl(easternSdf.parse("2014-07-01 22:00")));
    assertFalse(win.canCrawl(gmtSdf.parse("2014-07-02 02:00")));

    win = (CrawlWindows.Daily)deserWindow(daily2);
    assertEquals("Days 2;3;4;5;6 from 20:00 to 6:00, GMT", win.toString());
    // The resulting window is Mon0-6, Mon20-Tue6, Tue20-Wed6, Wed20-Thu6, Thu20-Fri6, Fri20-Fri24,
    // not Mon20-Tue6, Tue20-Wed6, Wed20-Thu6, Thu20-Fri6, Fri20-Sat6 or similar
    // 2014-08-11 is a Monday
    for (int d = 11 /* Monday */ ; d <= 17 /* Sunday */ ; ++d) {
      assertEquals(d <= 15 /* Monday-Friday */, win.canCrawl(gmtSdf.parse("2014-08-" + d + " 00:00")));
      assertEquals(d <= 15 /* Monday-Friday */, win.canCrawl(gmtSdf.parse("2014-08-" + d + " 05:59")));
      assertFalse(win.canCrawl(gmtSdf.parse("2014-08-" + d + " 06:00")));
      assertFalse(win.canCrawl(gmtSdf.parse("2014-08-" + d + " 19:59")));
      assertEquals(d <= 15 /* Monday-Friday */, win.canCrawl(gmtSdf.parse("2014-08-" + d + " 20:00")));
      assertEquals(d <= 15 /* Monday-Friday */, win.canCrawl(gmtSdf.parse("2014-08-" + d + " 23:59")));
    }
  }

  static String and1 =
    "<org.lockss.daemon.CrawlWindows-And>\n" +
    "<windows>\n" +
    daily1 +
    daily2 +
    "</windows>\n" +
    "</org.lockss.daemon.CrawlWindows-And>\n";

  public void testDeserAnd() throws Exception {
    CrawlWindows.And win;
    
    win = (CrawlWindows.And)deserWindow(and1);
    assertEquals(2, win.windows.size());
    for (Object x : win.windows) {
      assertClass(CrawlWindows.Daily.class, x);
    }
  }

  static String or1 =
    "<org.lockss.daemon.CrawlWindows-Or>\n" +
    "<windows>\n" +
    daily2 +
    daily1 +
    "</windows>\n" +
    "</org.lockss.daemon.CrawlWindows-Or>\n";

  public void testDeserOr() throws Exception {
    CrawlWindows.Or win;
    
    win = (CrawlWindows.Or)deserWindow(or1);
    assertEquals(2, win.windows.size());
    for (Object x : win.windows) {
      assertClass(CrawlWindows.Daily.class, x);
    }
  }

  static String not1 =
    "<org.lockss.daemon.CrawlWindows-Not>\n" +
    "<window class=\"org.lockss.daemon.CrawlWindows-Daily\">\n" +
    "  <from>8:00</from>\n" +
    "  <to>22:00</to>\n" +
    "  <timeZoneId>GMT-0600</timeZoneId>\n" +
    "</window>\n" +
    "</org.lockss.daemon.CrawlWindows-Not>\n";

  public void testDeserNot() throws Exception {
    CrawlWindows.Not win;
    
    win = (CrawlWindows.Not)deserWindow(not1);
    assertClass(CrawlWindows.Daily.class, win.window);
  }


  public void testAlways() {
    CrawlWindow win = new CrawlWindows.Always();
    assertTrue(win.canCrawl());
    assertTrue(win.canCrawl(new Date(0)));
    assertTrue(win.canCrawl(new Date(Constants.HOUR)));
    assertTrue(win.canCrawl(new Date(2 * Constants.HOUR)));
    assertTrue(win.canCrawl(new Date(12 * Constants.HOUR)));
    assertTrue(win.canCrawl(new Date(23 * Constants.HOUR)));
    assertTrue(win.canCrawl(new Date(Constants.DAY + 2 * Constants.HOUR)));
    assertTrue(win.canCrawl(new Date(Constants.YEAR + 2 * Constants.HOUR)));
    assertTrue(win.canCrawl(new Date(100 * Constants.YEAR
				     + 2 * Constants.HOUR)));
  }

  public void testNever() {
    CrawlWindow win = new CrawlWindows.Never();
    assertFalse(win.canCrawl());
    assertFalse(win.canCrawl(new Date(0)));
    assertFalse(win.canCrawl(new Date(Constants.HOUR)));
    assertFalse(win.canCrawl(new Date(2 * Constants.HOUR)));
    assertFalse(win.canCrawl(new Date(12 * Constants.HOUR)));
    assertFalse(win.canCrawl(new Date(23 * Constants.HOUR)));
    assertFalse(win.canCrawl(new Date(Constants.DAY + 2 * Constants.HOUR)));
    assertFalse(win.canCrawl(new Date(Constants.YEAR + 2 * Constants.HOUR)));
    assertFalse(win.canCrawl(new Date(100 * Constants.YEAR
				      + 2 * Constants.HOUR)));
  }

  void assertEqualWin(CrawlWindow w1, CrawlWindow w2) {
    assertTrue(w1.equals(w2));
    assertTrue(w2.equals(w1));
    assertEquals(w1.hashCode(), w2.hashCode());
  }

  void assertNotEqualWin(CrawlWindow w1, CrawlWindow w2) {
    assertFalse(w1.equals(w2));
    assertFalse(w2.equals(w1));
    // might fail if hashCode() or HashCodeBuilder changes
    assertNotEquals(w1.hashCode(), w2.hashCode());
  }

  public void testEquals() {
    CrawlWindow wnever = new CrawlWindows.Never();
    CrawlWindow walways = new CrawlWindows.Always();
    assertEqualWin(wnever, wnever);
    assertNotEqualWin(wnever, walways);
    assertEqualWin(walways, walways);
    assertEqualWin(walways, new CrawlWindows.Always());

    CrawlWindow wdaily = new CrawlWindows.Daily("2:00", "7:00", "GMT");
    assertEqualWin(wdaily, wdaily);
    assertEqualWin(wdaily, new CrawlWindows.Daily("2:00", "7:00", "GMT"));
    assertNotEqualWin(wdaily, new CrawlWindows.Daily("2:01", "7:00", "GMT"));
    assertNotEqualWin(wdaily, new CrawlWindows.Daily("2:00", "7:01", "GMT"));
    assertNotEqualWin(wdaily, new CrawlWindows.Daily("2:00", "7:00", "PST"));

    assertNotEqualWin(wdaily, wnever);
    assertNotEqualWin(wdaily, walways);

    CrawlWindow wwkends = new CrawlWindows.Daily("2:00", "7:00", "1;7", "GMT");
    assertEqualWin(wwkends, wwkends);
    assertEqualWin(wwkends, new CrawlWindows.Daily("2:00", "7:00", "7;1",
						   "GMT"));
    assertNotEqualWin(wwkends, new CrawlWindows.Daily("2:00", "7:00", "1;2;7",
						      "GMT"));

    start.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
    end.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
    CrawlWindow winterval =
      new CrawlWindows.Interval(start, end, CrawlWindows.DAY_OF_WEEK, GMT);
    assertEqualWin(winterval, winterval);
    assertEqualWin(winterval,
		   new CrawlWindows.Interval(start, end,
					     CrawlWindows.DAY_OF_WEEK, GMT));
    assertNotEqualWin(winterval,
		      new CrawlWindows.Interval(start, start,
						CrawlWindows.DAY_OF_WEEK, GMT));
    assertNotEqualWin(winterval,
		      new CrawlWindows.Interval(end, end,
						CrawlWindows.DAY_OF_WEEK, GMT));
    assertNotEqualWin(winterval,
		      new CrawlWindows.Interval(start, end,
						CrawlWindows.DAY_OF_MONTH, GMT));
    assertNotEqualWin(winterval,
		      new CrawlWindows.Interval(start, end,
						CrawlWindows.DAY_OF_WEEK,
						TimeZone.getTimeZone("PST")));
  }
  
  /** Utility */
  protected static final void setDate(Calendar cal,
                                      TimeZone tz,
                                      int year,
                                      int month,
                                      int dayOfMonth) {
    cal.setTimeZone(tz);
    cal.set(Calendar.YEAR, year);
    cal.set(Calendar.MONTH, month);
    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
  }
  
  /** Utility */
  protected static final void setTime(Calendar cal,
                                      TimeZone tz,
                                      int hourOfDay,
                                      int minute) {
    cal.setTimeZone(tz);
    setTime(cal, hourOfDay, minute);
  }

  /** Utility */
  protected static final void setTime(Calendar cal,
                                      int hourOfDay,
                                      int minute) {
    cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
    cal.set(Calendar.MINUTE, minute);
  }

}

