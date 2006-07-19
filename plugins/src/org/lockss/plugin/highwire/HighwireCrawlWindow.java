/*
 * $Id: HighwireCrawlWindow.java,v 1.3 2006-07-19 16:44:43 thib_gc Exp $
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

package org.lockss.plugin.highwire;

import java.util.*;

import org.lockss.daemon.*;
import org.lockss.plugin.definable.*;

public class HighwireCrawlWindow
    implements DefinableArchivalUnit.ConfigurableCrawlWindow {
  public HighwireCrawlWindow() {}

  public CrawlWindow makeCrawlWindow() {
    // disallow crawls from 6am->9am
    Calendar start = Calendar.getInstance();
    start.set(Calendar.HOUR_OF_DAY, 5);
    start.set(Calendar.MINUTE, 0);
    Calendar end = Calendar.getInstance();
    end.set(Calendar.HOUR_OF_DAY, 11);
    end.set(Calendar.MINUTE, 0);
    CrawlWindow interval =
      new CrawlWindows.Interval(start, end, CrawlWindows.TIME,
				TimeZone.getTimeZone("America/Los_Angeles"));
    // disallow using 'NOT'
    return new CrawlWindows.Not(interval);
  }

}
