/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire;

import java.util.*;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.definable.*;

public class HighWirePressCrawlWindow implements DefinableArchivalUnit.ConfigurableCrawlWindow {

  public static final String DESCRIPTION =
    "All times except 5 to 11 a.m. weekdays, US/Pacific";

  public HighWirePressCrawlWindow() {}

  public CrawlWindow makeCrawlWindow() {
    // Disallow crawls from 5 to 11 a.m.
    Calendar start_time = Calendar.getInstance();
    start_time.set(Calendar.HOUR_OF_DAY, 5);
    start_time.set(Calendar.MINUTE, 0);
    Calendar end_time = Calendar.getInstance();
    end_time.set(Calendar.HOUR_OF_DAY, 11);
    end_time.set(Calendar.MINUTE, 0);
    CrawlWindow mon_thru_fri_5_to_11_am = new CrawlWindows.Interval(start_time,
                                                         end_time,
                                                         CrawlWindows.TIME,
                                                         TimeZone.getTimeZone("America/Los_Angeles"));
    CrawlWindow not_mon_thru_fri_5_to_11_am = new CrawlWindows.Not(mon_thru_fri_5_to_11_am);

    // Allow crawls on Saturday and Sunday
    Calendar start_day = Calendar.getInstance();
    start_day.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
    Calendar end_day = Calendar.getInstance();
    end_day.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

    CrawlWindow sat_thru_sun = new CrawlWindows.Interval(start_day,
                                                         end_day,
                                                         CrawlWindows.DAY_OF_WEEK,
                                                         TimeZone.getTimeZone("America/Los_Angeles"));

    // Assemble with an Or (and add a description)
    return new CrawlWindows.Or(SetUtil.set(not_mon_thru_fri_5_to_11_am,
                                           sat_thru_sun)) {
      public String toString() {
	return DESCRIPTION;
      }
    };

  }

}
