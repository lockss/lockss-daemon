/*
 * $Id: CrawlWindows.java,v 1.2 2003-10-24 07:40:25 eaalto Exp $
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
import org.lockss.util.*;

/**
 * Several useful CrawlWindow implementations.
 */
public class CrawlWindows {

  /**
   * Abstract base window which handles timezone issues.
   */
  protected abstract static class BaseCrawlWindow implements CrawlWindow {
    protected TimeZone timeZone;

    // fields to include in calendar comparisons. Bit-wise additive.
    /** Field indicating hour of day (24-hour).  Includes  end point
     * (i.e. 5-6pm matches up to 6:59pm).
     */
    public static final int HOUR_OF_DAY = 1;
    /** Field indicating minute */
    public static final int MINUTE = 2;
    /** Field indicating day of week (i.e. Monday) */
    public static final int DAY_OF_WEEK = 4;
    /** Field indicating day of month (1st, 2nd, etc.) */
    public static final int DAY_OF_MONTH = 8;
    /** Field indicating week of month */
    public static final int WEEK_OF_MONTH = 16;
    /** Field indicating month */
    public static final int MONTH = 32;

    /** Convenience value indicating hour and minute comparison. Does not
     * include seconds, so 5->6:00 allows up to 6:00:59.
     */
    public static final int TIME = HOUR_OF_DAY + MINUTE;

    public BaseCrawlWindow(TimeZone serverTZ) {
      this.timeZone = serverTZ; // null uses default
    }

    public void setServerTimeZone(TimeZone serverTimeZone) {
      this.timeZone = serverTimeZone;
    }

    public boolean canCrawl() {
      return canCrawl(TimeBase.nowDate());
    }

    public boolean canCrawl(Date serverDate) {
      Calendar serverCal;
      if (timeZone != null) {
        serverCal = Calendar.getInstance(timeZone);
      }
      else {
        serverCal = Calendar.getInstance();
      }
      // set to the date to test
      serverCal.setTime(serverDate);
      return isMatch(serverCal);
    }

    /**
     * Primary function to be implemented.  Returns true if the calendar
     * matches its criteria.
     * @param cal the time on the server
     * @return true iff matches criteria
     */
    protected abstract boolean isMatch(Calendar cal);
  }


  /**
   * A window which represents an interval (from start to end, inclusive).  It
   * takes two {@link Calendar}s, and a field bit-mask for comparison.  This is
   * constructed from the BaseCrawlWindow fields. For example,
   * to set a window from Monday->Thursday the start Calendar would have
   * 'DAY_OF_WEEK' set to Monday, the end to Thursday, and the field would be
   * BaseCrawlWindow.DAY_OF_WEEK.  To compare day of the week and month (i.e.
   * M-F, June-Dec), the mask would be DAY_OF_WEEK + MONTH.
   */
  public static class Interval extends BaseCrawlWindow {
    Calendar start;
    Calendar end;
    int fieldMask;

    /**
     * Usage: to set a window from Monday->Thursday the start Calendar would have
     * 'DAY_OF_WEEK' set to Monday, the end to Thursday, and the field would be
     * Calendar.DAY_OF_WEEK.  The 'Interval.TIME' field examines both HOUR and
     * MINUTE.  Both start and end are inclusive, except for the final minute
     * when 'TIME' is chosen.
     *
     * @param start the Calendar with the start of the interval
     * @param end the Calendar with the end of the interval
     * @param fieldMask a bit-mask of the Calendar fields to examine
     * @param serverTZ the time zone of the server
     */
    public Interval(Calendar start, Calendar end, int fieldMask,
                    TimeZone serverTZ) {
      super(serverTZ);
      if ((start==null) || (end==null)) {
        throw new NullPointerException("CrawlWindows.Interval with null calendar");
      }
      this.start = start;
      this.end = end;
      this.fieldMask = fieldMask;
    }

    public boolean isMatch(Calendar cal) {
      if ((fieldMask & MONTH) > 0) {
        if (!fieldMatches(Calendar.MONTH, cal)) {
          return false;
        }
      }
      if ((fieldMask & WEEK_OF_MONTH) > 0) {
        if (!fieldMatches(Calendar.WEEK_OF_MONTH, cal)) {
          return false;
        }
      }
      if ((fieldMask & DAY_OF_MONTH) > 0) {
        if (!fieldMatches(Calendar.DAY_OF_MONTH, cal)) {
          return false;
        }
      }
      if ((fieldMask & DAY_OF_WEEK) > 0) {
        if (!fieldMatches(Calendar.DAY_OF_WEEK, cal)) {
          return false;
        }
      }
      if ((fieldMask & HOUR_OF_DAY) > 0) {
        if ((fieldMask & MINUTE) > 0) {
          // hours and minutes must be considered together
          return matchesTime(cal);
        } else {
          // if just hours are included treat normally
          if (!fieldMatches(Calendar.HOUR_OF_DAY, cal)) {
            return false;
          }
        }
      } else {
        // if just minutes are included treat normally
        if ((fieldMask & MINUTE) > 0) {
          if (!fieldMatches(Calendar.MINUTE, cal)) {
            return false;
          }
        }
      }

      return true;
    }

    private boolean fieldMatches(int field, Calendar cal) {
      int serverVal = cal.get(field);
      int startVal = start.get(field);
      int endVal = end.get(field);
      boolean startIsLowest = (startVal < endVal);

      if (startIsLowest) {
        return ((serverVal >= startVal) && (serverVal <= endVal));
      } else {
        return ((serverVal >= startVal) || (serverVal <= endVal));
      }
    }

    private boolean matchesTime(Calendar cal) {
      int serverHour = cal.get(Calendar.HOUR_OF_DAY);
      int startHour = start.get(Calendar.HOUR_OF_DAY);
      int endHour = end.get(Calendar.HOUR_OF_DAY);
      int serverMin = cal.get(Calendar.MINUTE);
      int startMin = start.get(Calendar.MINUTE);
      int endMin = end.get(Calendar.MINUTE);

      boolean startIsLowest;
      if (startHour != endHour) {
        startIsLowest = (startHour < endHour);
      } else {
        startIsLowest = (startMin < endMin);
      }

      boolean aboveStart = ( (serverHour > startHour) ||
                             (serverHour == startHour) &&
                             (serverMin >= startMin));
      boolean belowEnd = ( (serverHour < endHour) ||
                           (serverHour == endHour) &&
                           (serverMin < endMin));
      if (startIsLowest) {
        return (aboveStart && belowEnd);
      } else {
        return (aboveStart || belowEnd);
      }
    }

    public String toString() {
      return "[CrawlWindows.Interval: field: " + fieldMask +
          ", "+start+", "+end+"]";
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
  public static class FieldEnum extends BaseCrawlWindow {
    Set calendarSet;
    int fieldMask;

    public FieldEnum(Set calendarSet, int fieldMask, TimeZone serverTZ) {
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
   * Base 'set' class for AND and OR (used by NOT).
   */
  private static class WindowSet implements CrawlWindow {
    protected Set windows;
    protected int type;

    /** the 'AND' type operation */
    static final int AND_TYPE = 0;
    /** the 'OR' type operation */
    static final int OR_TYPE = 1;
    /** the 'NOT' type operation */
    static final int NOT_TYPE = 2;

    /**
     * Create a window that matches against the given list of windows, with a
     * particular boolean operation.
     * @param windows set of {@link CrawlWindow}s
     * @param type the type of operation
     * @throws NullPointerException if the list is null.
     */
    public WindowSet(Set windows, int type) {
      if (windows == null) {
        throw new NullPointerException("CrawlWindows.WindowSet with null set");
      }
      this.windows = SetUtil.immutableSetOfType(windows, CrawlWindow.class);
      this.type = type;
    }

    public boolean canCrawl() {
      return canCrawl(TimeBase.nowDate());
    }

    public boolean canCrawl(Date serverDate) {
      if (windows.size()==0) {
        switch (type) {
          case AND_TYPE:
            // 'AND' with no arguments traditionally returns true
            return true;
          case OR_TYPE:
          default:
            return false;
        }
      }

      Iterator iter = windows.iterator();
      while (iter.hasNext()) {
        boolean match = ((CrawlWindow)iter.next()).canCrawl(serverDate);
        switch (type) {
          case NOT_TYPE:
            // NOT only evaluates the first window
            return !match;
          case AND_TYPE:
            // exit early if false found
            if (!match) {
              return false;
            }
            break;
          case OR_TYPE:
            // exit early if true found
            if (match) {
              return true;
            }
            break;
        }
      }

      // if didn't exit previously, true for AND and false for OR
      return (type==AND_TYPE);
    }

    public String toString() {
      return "[CrawlWindows.WindowSet: type " + type +", "+ windows + "]";
    }
  }

  /**
   * The 'AND' operation window.  It takes a set of CrawlWindows, which can be
   * empty (returns true).  Otherwise, it does an 'AND' operation on their
   * 'canCrawl()' functions.
   */
  public static class AND extends WindowSet {
    public AND(Set windowSet) {
      super(windowSet, AND_TYPE);
    }
  }

  /**
   * The 'OR' operation window.  It takes a set of CrawlWindows, which can be
   * empty (returns false).  Otherwise, it does an 'OR' operation on their
   * 'canCrawl()' functions.
   */
  public static class OR extends WindowSet {
    public OR(Set windowSet) {
      super(windowSet, OR_TYPE);
    }
  }

  /**
   * The 'NOT' operation window.  It takes a single CrawlWindow, and does a
   * 'NOT' operation on its 'canCrawl()' function.
   */
  public static class NOT extends WindowSet {
    public NOT(CrawlWindow window) {
      super(SetUtil.set(window), NOT_TYPE);
    }
  }
}
