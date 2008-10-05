/*
 * $Id: BlackwellCrawlWindow.java,v 1.1 2006-08-01 05:21:51 tlipkis Exp $
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

package org.lockss.plugin.blackwell;

import java.util.*;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.definable.*;

public class BlackwellCrawlWindow
    implements DefinableArchivalUnit.ConfigurableCrawlWindow {
  public BlackwellCrawlWindow() {}

  /** Return a CrawlWindow that disallows crawls from 12am - 12pm
   * weekdays */
  public CrawlWindow makeCrawlWindow() {
    // Make an interval from 12pm - 12am
    Calendar stime = Calendar.getInstance();
    stime.set(Calendar.HOUR_OF_DAY, 12);
    stime.set(Calendar.MINUTE, 0);
    Calendar etime = Calendar.getInstance();
    etime.set(Calendar.HOUR_OF_DAY, 0);
    etime.set(Calendar.MINUTE, 0);
    CrawlWindow timeInterval =
      new CrawlWindows.Interval(stime, etime,
				CrawlWindows.TIME,
				TimeZone.getTimeZone("America/Los_Angeles"));

    // Make an interval from sat - sun
    Calendar sday = Calendar.getInstance();
    sday.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
    Calendar eday = Calendar.getInstance();
    eday.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

    CrawlWindow dayInterval =
      new CrawlWindows.Interval(sday, eday,
				CrawlWindows.DAY_OF_WEEK,
				TimeZone.getTimeZone("America/Los_Angeles"));
    // Assemble OR(sat-sun, 12pm - 12am)
    return new CrawlWindows.Or(SetUtil.set(timeInterval, dayInterval));
  }

}
