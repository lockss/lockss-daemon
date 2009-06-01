/*
 * $Id: PsychiatryOnlineCrawlWindow.java,v 1.3 2009-06-01 07:30:32 tlipkis Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.psychiatryonline;

import java.util.*;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.definable.DefinableArchivalUnit.ConfigurableCrawlWindow;

public class PsychiatryOnlineCrawlWindow implements ConfigurableCrawlWindow {
  private static Logger log = Logger.getLogger("PsychiatryOnlineCrawlWindow");

  public static final String CRAWL_WINDOW_TIME_ZONE = "US/Eastern";
  public static final String DESCRIPTION = "Midnight to 7:00AM US/Eastern";

  public CrawlWindow makeCrawlWindow() throws PluginException {
    /*
     * Only allow crawls from midnight to 7am Eastern.
     */
    
    /*
     * There is no commitment anywhere in the Java 5 API for
     * java.util.TimeZone that any particular well-known time zone
     * will be supported by name. Using only
     * java.util.TimeZone.getTimeZone(String) is insufficient because
     * it always returns at least the GMT time zone.
     */
    TimeZone eastern = TimeZone.getTimeZone(CRAWL_WINDOW_TIME_ZONE);
    if (   !"GMT".equals(CRAWL_WINDOW_TIME_ZONE)
        && eastern.equals(TimeZone.getTimeZone("GMT"))) {
      throw new PluginException("Unavailable time zone: " + CRAWL_WINDOW_TIME_ZONE);
    }
    
    Calendar start = Calendar.getInstance();
    start.set(Calendar.HOUR_OF_DAY, 0);
    start.set(Calendar.MINUTE, 0);
    Calendar end = Calendar.getInstance();
    end.set(Calendar.HOUR_OF_DAY, 7);
    end.set(Calendar.MINUTE, 0);
    return new CrawlWindows.Interval(start,
				     end,
				     CrawlWindows.TIME,
				     eastern) {
      public String toString() {
	return DESCRIPTION;
      }
    };
  }

}
