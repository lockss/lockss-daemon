/*
 * $Id: TestHighWireCrawlWindow.java,v 1.1 2006-06-20 23:34:12 troberts Exp $
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

package org.lockss.plugin.highwire;

import java.io.*;
import java.text.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.test.LockssTestCase;

public class TestHighWireCrawlWindow extends LockssTestCase {

  public void setup() {
  }

  public void testCrawlWindowCA() throws ParseException {
    TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

    HighwireCrawlWindow windowFactory = new HighwireCrawlWindow();
    CrawlWindow window = windowFactory.makeCrawlWindow();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
    sdf.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));

    assertTrue(window.canCrawl(sdf.parse("2006-06-19 04:59:00.0")));
    assertTrue(window.canCrawl(sdf.parse("2006-12-19 04:59:00.0")));
    assertFalse(window.canCrawl(sdf.parse("2006-06-19 05:00:00.0")));
    assertFalse(window.canCrawl(sdf.parse("2006-12-19 05:00:00.0")));
    assertFalse(window.canCrawl(sdf.parse("2006-06-19 05:01:00.0")));
    assertFalse(window.canCrawl(sdf.parse("2006-12-19 05:01:00.0")));

    assertFalse(window.canCrawl(sdf.parse("2006-06-19 10:59:00.0")));
    assertFalse(window.canCrawl(sdf.parse("2006-12-19 10:59:00.0")));
    assertTrue(window.canCrawl(sdf.parse("2006-06-19 11:00:00.0")));
    assertTrue(window.canCrawl(sdf.parse("2006-12-19 11:00:00.0")));
    assertTrue(window.canCrawl(sdf.parse("2006-06-19 11:01:00.0")));
    assertTrue(window.canCrawl(sdf.parse("2006-12-19 11:01:00.0")));
  }

  public void testCrawlWindowNY() throws ParseException {
    TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

    HighwireCrawlWindow windowFactory = new HighwireCrawlWindow();
    CrawlWindow window = windowFactory.makeCrawlWindow();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
    sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));

    assertTrue(window.canCrawl(sdf.parse("2006-06-19 07:59:00.0")));
    assertTrue(window.canCrawl(sdf.parse("2006-12-19 07:59:00.0")));
    assertFalse(window.canCrawl(sdf.parse("2006-06-19 08:00:00.0")));
    assertFalse(window.canCrawl(sdf.parse("2006-12-19 08:00:00.0")));
    assertFalse(window.canCrawl(sdf.parse("2006-06-19 08:01:00.0")));
    assertFalse(window.canCrawl(sdf.parse("2006-12-19 08:01:00.0")));

    assertFalse(window.canCrawl(sdf.parse("2006-06-19 13:59:00.0")));
    assertFalse(window.canCrawl(sdf.parse("2006-12-19 13:59:00.0")));
    assertTrue(window.canCrawl(sdf.parse("2006-06-19 14:00:00.0")));
    assertTrue(window.canCrawl(sdf.parse("2006-12-19 14:00:00.0")));
    assertTrue(window.canCrawl(sdf.parse("2006-06-19 14:01:00.0")));
    assertTrue(window.canCrawl(sdf.parse("2006-12-19 14:01:00.0")));
  }

  public void testCrawlWindowDSTCheck() throws ParseException {
    TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

    HighwireCrawlWindow windowFactory = new HighwireCrawlWindow();
    CrawlWindow window = windowFactory.makeCrawlWindow();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S z");
//     sdf.setTimeZone(TimeZone.getTimeZone("PDT"));

    assertTrue(window.canCrawl(sdf.parse("2006-06-19 04:59:00.0 PDT")));
    assertTrue(window.canCrawl(sdf.parse("2006-12-19 04:59:00.0 PST")));
    assertFalse(window.canCrawl(sdf.parse("2006-06-19 05:00:00.0 PDT")));
    assertFalse(window.canCrawl(sdf.parse("2006-12-19 05:00:00.0 PST")));
    assertFalse(window.canCrawl(sdf.parse("2006-06-19 05:01:00.0 PDT")));
    assertFalse(window.canCrawl(sdf.parse("2006-12-19 05:01:00.0 PST")));

    assertFalse(window.canCrawl(sdf.parse("2006-06-19 10:59:00.0 PDT")));
    assertFalse(window.canCrawl(sdf.parse("2006-12-19 10:59:00.0 PST")));
    assertTrue(window.canCrawl(sdf.parse("2006-06-19 11:00:00.0 PDT")));
    assertTrue(window.canCrawl(sdf.parse("2006-12-19 11:00:00.0 PST")));
    assertTrue(window.canCrawl(sdf.parse("2006-06-19 11:01:00.0 PDT")));
    assertTrue(window.canCrawl(sdf.parse("2006-12-19 11:01:00.0 PST")));
  }


}
