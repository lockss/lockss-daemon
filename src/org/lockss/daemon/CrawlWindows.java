/*
 * $Id: CrawlWindows.java,v 1.12 2006-11-11 06:52:53 tlipkis Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.app.LockssApp;
import org.lockss.util.*;

/**
 * Several useful CrawlWindow implementations.
 */
public class CrawlWindows {
  // fields to include in calendar comparisons. Bit-wise additive.
  // the first two are not public, to avoid confusion with 'TIME' and useless
  // short intervals (using strictly 'MINUTE' is always less than an hour,
  // which is probably too short)
  /** Field indicating hour of day (24-hour).  Includes end point
   * (i.e. 5-6 matches up to 6:59).
   */
  static final int HOUR_OF_DAY = 1;
  /** Field indicating minute */
  static final int MINUTE = 2;

  /** Convenience value indicating hour and minute comparison. Does not
   * include seconds, so 5->6:00 allows up to 6:00:59.
   */
  public static final int TIME = HOUR_OF_DAY + MINUTE;
  /** Field indicating day of week (i.e. Monday) */
  public static final int DAY_OF_WEEK = 4;
  /** Field indicating day of month (1st, 2nd, etc.) */
  public static final int DAY_OF_MONTH = 8;
  /** Field indicating week of month */
  public static final int WEEK_OF_MONTH = 16;
  /** Field indicating month */
  public static final int MONTH = 32;

  static final int MAX_INTERVAL_LIST_SIZE = 100000;
  private static Logger logger = Logger.getLogger("CrawlWindows");

  /**
   * Abstract base window which handles timezone issues.
   */
  protected abstract static class BaseCrawlWindow implements CrawlWindow {
    protected transient TimeZone timeZone;
    protected String timeZoneId;
    transient Calendar windowCal;

    /**
     * Constructor takes a {@link TimeZone} for the window.  Null defaults
     * to the system time zone.
     * @param windowTZ the window's TimeZone
     */
    public BaseCrawlWindow(TimeZone windowTZ) {
      setWindowTimeZone(windowTZ);
    }

    /**
     * Sets the window's {@link TimeZone}.  Null defaults to the system time
     * zone.  Used only by tests.
     * @param windowTZ the new TimeZone
     */
    void setWindowTimeZone(TimeZone windowTZ) {
      if (windowTZ==null) {
        timeZone = TimeZone.getDefault();
      } else {
        timeZone = windowTZ;
      }
      timeZoneId = timeZone.getID();
      windowCal = Calendar.getInstance(timeZone);
    }

    public boolean canCrawl() {
      return canCrawl(TimeBase.nowDate());
    }

    public boolean canCrawl(Date date) {
      // set to the date to test
      synchronized (windowCal) {
	windowCal.setTime(date);
	return isMatch(windowCal);
      }
    }

    /**
     * Primary function to be implemented.  Returns true if the calendar
     * matches its criteria.
     * @param cal the time on the server
     * @return true iff matches criteria
     */
    protected abstract boolean isMatch(Calendar cal);

    protected void postUnmarshal(LockssApp lockssContext) {
      if (!StringUtil.isNullString(timeZoneId)) {
        timeZone = TimeZone.getTimeZone(timeZoneId);
      }
      else {
        timeZone = TimeZone.getDefault();
        timeZoneId = timeZone.getID();
      }
      windowCal = Calendar.getInstance(timeZone);
    }
  }


  /**
   * A window which represents an interval (from start to end, inclusive).  It
   * takes two {@link Calendar}s, and a field bit-mask for comparison.  This is
   * constructed from the CrawlWindows fields. For example,
   * to set a window from Monday->Thursday the start Calendar would have
   * 'DAY_OF_WEEK' set to Monday, the end to Thursday, and the field would be
   * CrawlWindows.DAY_OF_WEEK.  To compare day of the week and month (i.e.
   * M-F, June-Dec), the mask would be DAY_OF_WEEK + MONTH.
   */
  public static class Interval extends BaseCrawlWindow {
    Calendar start;
    Calendar end;
    int fieldMask;

    /**
     * Usage: to set a window from Monday->Thursday the start Calendar would have
     * 'DAY_OF_WEEK' set to Monday, the end to Thursday, and the field would be
     * Calendar.DAY_OF_WEEK.  The 'CrawlWindows.TIME' field examines both HOUR and
     * MINUTE.  Both start and end are inclusive, except for the final minute
     * when 'TIME' is chosen.
     *
     * @param start the Calendar with the start of the interval
     * @param end the Calendar with the end of the interval
     * @param fieldMask a bit-mask of the Calendar fields to examine
     * @param windowTZ the time zone of the server
     */
    public Interval(Calendar start, Calendar end, int fieldMask,
                    TimeZone windowTZ) {
      super(windowTZ);
      if ((start==null) || (end==null)) {
        throw new NullPointerException("CrawlWindows.Interval with null calendar");
      }
      this.start = start;
      this.end = end;
      this.fieldMask = fieldMask;
    }

    public Calendar getStartCalendar(){
	return start;
    }

    public Calendar getEndCalendar(){
	return end;
    }

    public TimeZone getTimeZone(){
	return timeZone;
    }

    public boolean isMatch(Calendar cal) {
      if (bitTest(fieldMask, MONTH)) {
        if (!fieldMatches(Calendar.MONTH, cal)) {
          return false;
        }
      }
      if (bitTest(fieldMask, WEEK_OF_MONTH)) {
        if (!fieldMatches(Calendar.WEEK_OF_MONTH, cal)) {
          return false;
        }
      }
      if (bitTest(fieldMask, DAY_OF_MONTH)) {
        if (!fieldMatches(Calendar.DAY_OF_MONTH, cal)) {
          return false;
        }
      }
      if (bitTest(fieldMask, DAY_OF_WEEK)) {
        if (!fieldMatches(Calendar.DAY_OF_WEEK, cal)) {
          return false;
        }
      }

      // minute no longer considered separately
      if (bitTest(fieldMask, HOUR_OF_DAY)) {
        return matchesTime(cal);
      }

      return true;
    }

    private boolean fieldMatches(int field, Calendar cal) {
      int testVal = cal.get(field);
      int startVal = start.get(field);
      int endVal = end.get(field);

      if (startVal < endVal) {
        return ((testVal >= startVal) && (testVal <= endVal));
      } else if (startVal==endVal) {
        return (testVal == startVal);
      } else {
        return ((testVal >= startVal) || (testVal <= endVal));
      }
    }

    private boolean matchesTime(Calendar cal) {
      int testVal = 60 * cal.get(Calendar.HOUR_OF_DAY) +
          cal.get(Calendar.MINUTE);
      int startVal = 60 * start.get(Calendar.HOUR_OF_DAY) +
          start.get(Calendar.MINUTE);
      int endVal = 60 * end.get(Calendar.HOUR_OF_DAY) +
          end.get(Calendar.MINUTE);

      if (startVal < endVal) {
        return ((testVal >= startVal) && (testVal < endVal));
      } else {
        return ((testVal >= startVal) || (testVal < endVal));
      }
    }

    public String toString() {
      return "[CrawlWindows.Interval: field: " + fieldMask
             + ", start: " + start + ", end: " + end + "]";
    }
  }

  /**
   * A window which represents a collection of field values, such as
   * { Mon, Wed, Fri }.  The date matches if it matches one of the field values.
   * The 'TIME' designator makes little sense for this window, since a one
   * minute window is useless.  Usage is via a Set of Calendar objects, with
   * the field value set from the BaseCrawlWindow selection.  The value
   * may be bitwise additive.
   */
  public static class FieldSet extends BaseCrawlWindow {
    Set calendarSet;
    int fieldMask;

    public FieldSet(Set calendarSet, int fieldMask, TimeZone serverTZ) {
      super(serverTZ);
      if (calendarSet==null) {
        throw new NullPointerException("CrawlWindows.FieldEnum with null set");
      }
      this.calendarSet = calendarSet;
      this.fieldMask = fieldMask;
    }

    public boolean isMatch(Calendar cal) {
      Iterator calIter = calendarSet.iterator();
      while (calIter.hasNext()) {
        Calendar enumCal = (Calendar)calIter.next();
        if (matchesFields(cal, enumCal)) {
          return true;
        }
      }

      return false;
    }

    public boolean matchesFields(Calendar testCal, Calendar enumCal) {
      if ((fieldMask & MONTH) > 0) {
        if (testCal.get(Calendar.MONTH) != enumCal.get(Calendar.MONTH)) {
          return false;
        }
      }
      if ((fieldMask & WEEK_OF_MONTH) > 0) {
        if (testCal.get(Calendar.WEEK_OF_MONTH) !=
            enumCal.get(Calendar.WEEK_OF_MONTH)) {
          return false;
        }
      }
      if ((fieldMask & DAY_OF_MONTH) > 0) {
        if (testCal.get(Calendar.DAY_OF_MONTH) !=
            enumCal.get(Calendar.DAY_OF_MONTH)) {
          return false;
        }
      }
      if ((fieldMask & DAY_OF_WEEK) > 0) {
        if (testCal.get(Calendar.DAY_OF_WEEK) !=
            enumCal.get(Calendar.DAY_OF_WEEK)) {
          return false;
        }
      }
      if ((fieldMask & HOUR_OF_DAY) > 0) {
        if (testCal.get(Calendar.HOUR_OF_DAY) !=
            enumCal.get(Calendar.HOUR_OF_DAY)) {
          return false;
        }
      }
      if ((fieldMask & MINUTE) > 0) {
        if (testCal.get(Calendar.MINUTE) != enumCal.get(Calendar.MINUTE)) {
          return false;
        }
      }

      return true;
    }
  }

  /**
   * The 'AND' operation window.  It takes a set of CrawlWindows, which can be
   * empty (returns true).  Otherwise, it returns true iff all of their
   * 'canCrawl()' functions return true.
   */
  public static class And implements CrawlWindow {
    protected Set windows;

    public And(Set windowSet) {
      if (windowSet == null) {
        throw new NullPointerException("CrawlWindows.And with null set");
      }
      this.windows = SetUtil.immutableSetOfType(windowSet, CrawlWindow.class);
    }

    public boolean canCrawl() {
      return canCrawl(TimeBase.nowDate());
    }

    public boolean canCrawl(Date date) {
      for (Iterator iter = windows.iterator(); iter.hasNext(); ) {
        CrawlWindow cw = (CrawlWindow)iter.next();
        if (!cw.canCrawl(date)) {
          return false;
        }
      }
      return true;
    }

    public String toString() {
      return "[CrawlWindows.And: " + windows + "]";
    }
  }

  /**
   * The 'OR' operation window.  It takes a set of CrawlWindows, which can be
   * empty (returns false).  Otherwise, it returns true if any of their
   * 'canCrawl()' functions returns true.
   */
  public static class Or implements CrawlWindow {
    protected Set windows;

    public Or(Set windowSet) {
      if (windowSet == null) {
        throw new NullPointerException("CrawlWindows.Or with null set");
      }
      this.windows = SetUtil.immutableSetOfType(windowSet, CrawlWindow.class);
    }

    public boolean canCrawl() {
      return canCrawl(TimeBase.nowDate());
    }

    public boolean canCrawl(Date date) {
      for (Iterator iter = windows.iterator(); iter.hasNext(); ) {
        CrawlWindow cw = (CrawlWindow)iter.next();
        if (cw.canCrawl(date)) {
          return true;
        }
      }
      return false;
    }

    public String toString() {
      return "[CrawlWindows.Or: " + windows + "]";
    }
  }

  /**
   * The 'NOT' operation window.  It takes a single CrawlWindow, and returns
   * the opposite of it's 'canCrawl()' function.
   */
  public static class Not implements CrawlWindow {
    CrawlWindow window;

    public Not(CrawlWindow window) {
      if (window == null) {
        throw new NullPointerException("CrawlWindows.Not with null window");
      }
      this.window = window;
    }

    public CrawlWindow getCrawlWindow(){
	return window;
    }

    public boolean canCrawl() {
      return canCrawl(TimeBase.nowDate());
    }

    public boolean canCrawl(Date date) {
      return !window.canCrawl(date);
    }

    public String toString() {
      return "[CrawlWindows.Not: " + window + "]";
    }
  }

  /**
   * Tests if a specific bit is set to 1.
   * @param mask the mask int
   * @param bit the bit to test
   * @return true iff that bit is not zero.
   */
  public static boolean bitTest(int mask, int bit) {
    return (mask & bit) != 0;
  }

  /**
   * Returns a List of {@link TimeInterval}s for the crawl window between a given
   * start and end point.  Each represents a period when the window is open.
   * @param window the CrawlWindow
   * @param start the start Date
   * @param end the end Date
   * @return a list of TimeIntervals
   */
  public static synchronized List getCrawlIntervals(CrawlWindow window,
						    Date start, Date end) {
    List intervals = new ArrayList();
    boolean isOpen = false;
    Calendar startCal = Calendar.getInstance();
    startCal.setTime(start);
    Date testDate = startCal.getTime();
    Date intStartDate = null;
    while (testDate.compareTo(end) < 0) {
      boolean canCrawl = window.canCrawl(testDate);
      if (canCrawl != isOpen) {
        if (!isOpen) {
          // cache the start of the next interval
          intStartDate = testDate;
        } else {
          // close the current interval and add to list
          intervals.add(new TimeInterval(intStartDate, testDate));
          if (intervals.size() == MAX_INTERVAL_LIST_SIZE) {
            logger.warning("Maximum interval list size reached: "+
                           MAX_INTERVAL_LIST_SIZE);
            break;
          }
          intStartDate = null;
        }
        isOpen = canCrawl;
      }
      // increment by one minute
      startCal.add(Calendar.MINUTE, 1);
      testDate = startCal.getTime();
    }

    if (intStartDate!=null) {
      intervals.add(new TimeInterval(intStartDate, end));
    }

    return intervals;
  }
}
