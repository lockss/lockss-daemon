/*
 * $Id: TestPsychiatryOnlineCrawlWindow.java,v 1.1 2008-02-11 21:16:50 thib_gc Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

import java.text.*;
import java.util.TimeZone;

import org.lockss.daemon.CrawlWindow;
import org.lockss.test.LockssTestCase;

public class TestPsychiatryOnlineCrawlWindow extends LockssTestCase {

  protected CrawlWindow crawlWindow;

  protected DateFormat dateFormat;

  protected static final String EXPECTED_TIME_ZONE = "US/Eastern";

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    crawlWindow = new PsychiatryOnlineCrawlWindow().makeCrawlWindow();
    dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  }

  public void testTimeZone() {
    // Check that the time zone is what we think it is
    assertEquals(EXPECTED_TIME_ZONE,
                 PsychiatryOnlineCrawlWindow.CRAWL_WINDOW_TIME_ZONE);

    /*
     * There is no commitment anywhere in the Java 5 API for
     * java.util.TimeZone that any particular well-known time zone
     * will be supported by name. Using
     * java.util.Zone.getTimeZone(String) is insufficient because it
     * always returns at least the GMT time zone; iterate through the
     * time zones of java.util.TimeZone.getAvailableIDs() instead.
     */
    for (String tz : TimeZone.getAvailableIDs()) {
      if (tz.equals(EXPECTED_TIME_ZONE)) {
        return; // succeed
      }
    }
    fail(PsychiatryOnlineCrawlWindow.class.getName()
         + "requests the time zone \""
         + PsychiatryOnlineCrawlWindow.CRAWL_WINDOW_TIME_ZONE
         + "\" but this name is not recognized");
  }

  public void testCrawlWindowEastern() throws ParseException {
    final String tz = "US/Eastern";
    dateFormat.setTimeZone(TimeZone.getTimeZone(tz));
    assertFalse(crawlWindow.canCrawl(dateFormat.parse("2008-02-09 23:59:59")));
    assertTrue(crawlWindow.canCrawl(dateFormat.parse("2008-02-10 00:00:00")));
    assertTrue(crawlWindow.canCrawl(dateFormat.parse("2008-02-10 00:00:01")));
    assertTrue(crawlWindow.canCrawl(dateFormat.parse("2008-02-10 06:59:59")));
    assertFalse(crawlWindow.canCrawl(dateFormat.parse("2008-02-10 07:00:00")));
    assertFalse(crawlWindow.canCrawl(dateFormat.parse("2008-02-10 07:00:01")));
  }

  public void testCrawlWindowPacific() throws ParseException {
    final String tz = "US/Pacific";
    dateFormat.setTimeZone(TimeZone.getTimeZone(tz));
    assertFalse(crawlWindow.canCrawl(dateFormat.parse("2008-02-09 20:59:59")));
    assertTrue(crawlWindow.canCrawl(dateFormat.parse("2008-02-09 21:00:00")));
    assertTrue(crawlWindow.canCrawl(dateFormat.parse("2008-02-09 21:00:01")));
    assertTrue(crawlWindow.canCrawl(dateFormat.parse("2008-02-10 03:59:59")));
    assertFalse(crawlWindow.canCrawl(dateFormat.parse("2008-02-10 04:00:00")));
    assertFalse(crawlWindow.canCrawl(dateFormat.parse("2008-02-10 04:00:01")));
  }

  public void testCrawlWindowGMT() throws ParseException {
    final String tz = "GMT";
    dateFormat.setTimeZone(TimeZone.getTimeZone(tz));
    assertFalse(crawlWindow.canCrawl(dateFormat.parse("2008-02-10 04:59:59")));
    assertTrue(crawlWindow.canCrawl(dateFormat.parse("2008-02-10 05:00:00")));
    assertTrue(crawlWindow.canCrawl(dateFormat.parse("2008-02-10 05:00:01")));
    assertTrue(crawlWindow.canCrawl(dateFormat.parse("2008-02-10 11:59:59")));
    assertFalse(crawlWindow.canCrawl(dateFormat.parse("2008-02-10 12:00:00")));
    assertFalse(crawlWindow.canCrawl(dateFormat.parse("2008-02-10 12:00:01")));
  }

}
