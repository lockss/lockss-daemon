/*
 * $Id: CrawlWindows.java,v 1.1 2003-10-09 22:55:11 eaalto Exp $
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
 * Several useful CrawlWindow.Window implementations.
 */
public class CrawlWindows {
  /**
   * A window which represents an interval (from start to end, inclusive).  It
   * takes to {@link Calendar}s, and a Calendar field to examine.  For example,
   * to set a window from Monday->Thursday the start Calendar would have
   * 'DAY_OF_WEEK' set to Monday, the end to Thursday, and the field would be
   * Calendar.DAY_OF_WEEK.  The provided 'TIME' field examines both HOUR and
   * MINUTE.
   */
  public static class Interval extends CrawlWindowRule.Window {
    /** The field for hour and minute comparison. */
    public static final int TIME = -1;

    Calendar start;
    Calendar end;
    int field;
    boolean startIsLowest;

    /**
     * Usage: to set a window from Monday->Thursday the start Calendar would have
     * 'DAY_OF_WEEK' set to Monday, the end to Thursday, and the field would be
     * Calendar.DAY_OF_WEEK.  The 'Interval.TIME' field examines both HOUR and
     * MINUTE.  Both start and end are inclusive, except for the final minute
     * when 'TIME' is chosen.
     *
     * @param start the Calendar with the start of the interval
     * @param end the Calendar with the end of the interval
     * @param field the Calendar field to examine (or Interval.TIME)
     */
    public Interval(Calendar start, Calendar end, int field) {
      if ((start==null) || (end==null)) {
        throw new NullPointerException("CrawlWindows.Interval with null calendar");
      }
      this.start = start;
      this.end = end;
      this.field = field;

      if (field==TIME) {
        int startHour = start.get(Calendar.HOUR_OF_DAY);
        int endHour = end.get(Calendar.HOUR_OF_DAY);
        if (startHour != endHour) {
          startIsLowest = (startHour < endHour);
        } else {
          startIsLowest = (start.get(Calendar.MINUTE) <
                           end.get(Calendar.MINUTE));
        }
      } else {
        startIsLowest = (start.get(field) < end.get(field));
      }
    }

    public boolean isMatch(Calendar serverCal) {
      if (field==TIME) {
        int serverHour = serverCal.get(Calendar.HOUR_OF_DAY);
        int serverMin = serverCal.get(Calendar.MINUTE);
        int startHour = start.get(Calendar.HOUR_OF_DAY);
        int startMin = start.get(Calendar.MINUTE);
        int endHour = end.get(Calendar.HOUR_OF_DAY);
        int endMin = end.get(Calendar.MINUTE);

        boolean aboveStart = ((serverHour > startHour) ||
                              (serverHour==startHour) &&
                              (serverMin>=startMin));
        boolean belowEnd = ((serverHour < endHour) ||
                            (serverHour==endHour) &&
                            (serverMin < endMin));
        if (startIsLowest) {
          return (aboveStart && belowEnd);
        } else {
          return (aboveStart || belowEnd);
        }
      } else {
        int serverField = serverCal.get(field);

        if (startIsLowest) {
          return ((serverField >= start.get(field)) &&
                  (serverField <= end.get(field)));
        } else {
          return ((serverField >= start.get(field)) ||
                  (serverField <= end.get(field)));
        }
      }
    }

    public String toString() {
      return "[CrawlWindows.Interval: field: " + field +
          ", "+start+", "+end+"]";
    }

  }

  /**
   * A window which represents the logical 'and' or 'or' of a pair of windows.
   */
  public static class Pair extends CrawlWindowRule.Window {
    CrawlWindowRule.Window win1;
    CrawlWindowRule.Window win2;
    int operation;

    /** Boolean 'and' operation for windows */
    public static final int AND = 0;
    /** Boolean 'or' operation for windows */
    public static final int OR = 1;

    public Pair(CrawlWindowRule.Window win1, CrawlWindowRule.Window win2,
                int operation) {
      if ((win1==null) || (win2==null)) {
        throw new NullPointerException("CrawlWindows.Pair with null window");
      }
      this.win1 = win1;
      this.win2 = win2;
      this.operation = operation;
    }

    public boolean isMatch(Calendar serverCal) {
      if (operation==AND) {
        return (win1.isMatch(serverCal) && win2.isMatch(serverCal));
      } else if (operation==OR) {
        return (win1.isMatch(serverCal) || win2.isMatch(serverCal));
      } else {
        return false;
      }
    }
  }

  /**
   * A window which represents a collection of field values, such as
   * { Mon, Wed, Fri }.  The date matches if it matches one of the field values.
   * The 'TIME' designator makes little sense for this window, since a one
   * minute window is useless.  Usage is via a Set of Calendar objects, with
   * the appropriate fields set.
   */
  public static class FieldEnum extends CrawlWindowRule.Window {
    Set calendarSet;
    int field;

    public FieldEnum(Set calendarSet, int field) {
      if (calendarSet==null) {
        throw new NullPointerException("CrawlWindows.FieldEnum with null set");
      }
      this.calendarSet = calendarSet;
      this.field = field;
    }

    public boolean isMatch(Calendar serverCal) {
      Iterator calIter = calendarSet.iterator();
      while (calIter.hasNext()) {
        Calendar cal = (Calendar)calIter.next();
        if (cal.get(field) == serverCal.get(field)) {
          return true;
        }
      }

      return false;
    }
  }

}
