/*
 * $Id: GPOFDSysCrawlWindow.java,v 1.1 2014-08-08 18:33:46 thib_gc Exp $
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

package org.lockss.plugin.usdocspln.gov.gpo.fdsys;

import java.util.*;

import org.lockss.daemon.*;
import org.lockss.plugin.definable.DefinableArchivalUnit.ConfigurableCrawlWindow;
import org.lockss.util.SetUtil;

public class GPOFDSysCrawlWindow implements ConfigurableCrawlWindow {

  public static final String DESCRIPTION =
      "All times except Monday-Friday 9am-9pm and Sunday 2-10am (US/Eastern)";
  
  protected static final String TIME_ZONE = "US/Eastern";
  
  private static final CrawlWindow INSTANCE;
  
  static {
    TimeZone eastern = TimeZone.getTimeZone(TIME_ZONE);
    if (eastern == null) {
      throw new ExceptionInInitializerError(String.format("Time zone not found: %s", TIME_ZONE));
    }
    Calendar monday = Calendar.getInstance();
    monday.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    Calendar friday = Calendar.getInstance();
    friday.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
    Calendar x9am = Calendar.getInstance();
    x9am.set(Calendar.HOUR_OF_DAY, 9);
    x9am.set(Calendar.MINUTE, 0);
    Calendar x9pm = Calendar.getInstance();
    x9pm.set(Calendar.HOUR_OF_DAY, 21);
    x9pm.set(Calendar.MINUTE, 0);
    CrawlWindow monday_thru_friday =
        new CrawlWindows.Interval(monday, friday, CrawlWindows.DAY_OF_WEEK, eastern);
    CrawlWindow x9am_thru_9pm =
        new CrawlWindows.Interval(x9am, x9pm, CrawlWindows.TIME, eastern);
    CrawlWindow monday_thru_friday_9am_thru_9pm =
        new CrawlWindows.And(SetUtil.set(monday_thru_friday, x9am_thru_9pm));
    
    Calendar sunday = Calendar.getInstance();
    sunday.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
    Calendar x2am = Calendar.getInstance();
    x2am.set(Calendar.HOUR_OF_DAY, 2);
    x2am.set(Calendar.MINUTE, 0);
    Calendar x10am = Calendar.getInstance();
    x10am.set(Calendar.HOUR_OF_DAY, 10);
    x10am.set(Calendar.MINUTE, 0);
    CrawlWindow sunday_thru_sunday =
        new CrawlWindows.Interval(sunday, sunday, CrawlWindows.DAY_OF_WEEK, eastern);
    CrawlWindow x2am_thru_10am =
        new CrawlWindows.Interval(x2am, x10am, CrawlWindows.TIME, eastern);
    CrawlWindow sunday_thru_sunday_2am_thru_10am =
        new CrawlWindows.And(SetUtil.set(sunday_thru_sunday, x2am_thru_10am));
    
    CrawlWindow monday_thru_friday_9am_thru_9pm_and_sunday_2am_thru_10am =
        new CrawlWindows.Or(SetUtil.set(monday_thru_friday_9am_thru_9pm, sunday_thru_sunday_2am_thru_10am));
    
    INSTANCE = new CrawlWindows.Not(monday_thru_friday_9am_thru_9pm_and_sunday_2am_thru_10am) {
      public String toString() {
        return DESCRIPTION;
      }
    };
  }
  
  @Override
  public CrawlWindow makeCrawlWindow() throws PluginException {
    return INSTANCE;
  }

}
