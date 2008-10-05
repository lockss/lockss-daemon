/*
 * $Id: HighWireCrawlWindow.java,v 1.1 2007-12-06 23:47:45 thib_gc Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

public class HighWireCrawlWindow
    implements DefinableArchivalUnit.ConfigurableCrawlWindow {
  public HighWireCrawlWindow() {}

  public CrawlWindow makeCrawlWindow() {
    // disallow crawls from 6am->9am
    Calendar start = Calendar.getInstance();
    start.set(Calendar.HOUR_OF_DAY, 5);
    start.set(Calendar.MINUTE, 0);
    Calendar end = Calendar.getInstance();
    end.set(Calendar.HOUR_OF_DAY, 11);
    end.set(Calendar.MINUTE, 0);
    CrawlWindow timeInterval =
      new CrawlWindows.Interval(start, end, CrawlWindows.TIME,
				TimeZone.getTimeZone("America/Los_Angeles"));
    // disallow using 'NOT'
    timeInterval = new CrawlWindows.Not(timeInterval);

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
