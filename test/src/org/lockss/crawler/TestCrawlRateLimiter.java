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

package org.lockss.crawler;

import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.crawler.*;
import org.lockss.plugin.*;

public class TestCrawlRateLimiter extends LockssTestCase {

  public void testAccessors() {
    RateLimiterInfo rli = new RateLimiterInfo("key1", 50000);
    CrawlRateLimiter crl = CrawlRateLimiter.Util.forRli(rli);
    assertEquals(0, crl.getCrawlerCount());
    assertEquals(0, crl.getNewContentCount());
    assertEquals(0, crl.getRepairCount());
    MockCrawler mc1 = new MockCrawler();
    mc1.setIsWholeAU(false);
    crl.addCrawler(mc1);
    assertEquals(1, crl.getCrawlerCount());
    assertEquals(0, crl.getNewContentCount());
    assertEquals(1, crl.getRepairCount());

    MockCrawler mc2 = new MockCrawler();
    mc2.setIsWholeAU(true);
    crl.addCrawler(mc2);
    assertEquals(2, crl.getCrawlerCount());
    assertEquals(1, crl.getNewContentCount());
    assertEquals(1, crl.getRepairCount());

    MockCrawler mc3 = new MockCrawler();
    mc3.setIsWholeAU(false);
    crl.addCrawler(mc3);
    assertEquals(3, crl.getCrawlerCount());
    assertEquals(1, crl.getNewContentCount());
    assertEquals(2, crl.getRepairCount());

    crl.removeCrawler(mc2);
    assertEquals(2, crl.getCrawlerCount());
    assertEquals(0, crl.getNewContentCount());
    assertEquals(2, crl.getRepairCount());

    crl.removeCrawler(mc2);
    assertEquals(2, crl.getCrawlerCount());
    assertEquals(0, crl.getNewContentCount());
    assertEquals(2, crl.getRepairCount());

    crl.removeCrawler(mc1);
    assertEquals(1, crl.getCrawlerCount());
    assertEquals(0, crl.getNewContentCount());
    assertEquals(1, crl.getRepairCount());

    crl.removeCrawler(mc3);
    assertEquals(0, crl.getCrawlerCount());
    assertEquals(0, crl.getNewContentCount());
    assertEquals(0, crl.getRepairCount());
  }

  public void testMime() {
    RateLimiterInfo rli = new RateLimiterInfo("key1", 50000);
    Map<String,String> mimes =
      MapUtil.map("text/html,text/x-html,application/pdf", "10/1m",
		  "image/*", "5/1s");
    rli.setMimeRates(mimes);
    CrawlRateLimiter crl = CrawlRateLimiter.Util.forRli(rli);
    RateLimiter limiter = crl.getRateLimiterFor("url", "text/html");
    assertEquals("10/1m", limiter.getRate());
    assertSame(limiter, crl.getRateLimiterFor("url",
					      "text/html; charset=utf-8"));
    assertSame(limiter, crl.getRateLimiterFor("url", "text/x-html"));
    assertSame(limiter, crl.getRateLimiterFor("url", "application/pdf"));
    RateLimiter defLimiter = crl.getRateLimiterFor("url", "text/xml");
    assertEquals("1/50000", defLimiter.getRate());
    assertSame(defLimiter, crl.getRateLimiterFor("url", "foo/bar"));
    assertSame(defLimiter, crl.getRateLimiterFor("url", null));

    limiter = crl.getRateLimiterFor("url", "image/gif");
    assertEquals("5/1s", limiter.getRate());
    assertSame(limiter, crl.getRateLimiterFor("url", "image/png"));

    limiter = crl.getRateLimiterFor("url", "noimage/gif");
    assertEquals("1/50000", limiter.getRate());
    assertSame(limiter, crl.getRateLimiterFor("url", "application/*"));
    assertSame(limiter, crl.getRateLimiterFor("url", null));
  }

  public void testMimeWithDefault() {
    RateLimiterInfo rli = new RateLimiterInfo("key1", 50000);
    Map<String,String> mimes =
      MapUtil.map("text/html,text/x-html,application/pdf", "10/1m",
		  "text/*", "31/4159",
		  "image/*", "5/1s", "*/*", "22/23");
    rli.setMimeRates(mimes);
    CrawlRateLimiter crl = CrawlRateLimiter.Util.forRli(rli);
    RateLimiter limiter = crl.getRateLimiterFor("url", "text/html");
    assertEquals("10/1m", limiter.getRate());
    assertSame(limiter, crl.getRateLimiterFor("url", "text/html"));
    assertSame(limiter, crl.getRateLimiterFor("url",
					      "text/html; charset=utf-8"));
    assertSame(limiter, crl.getRateLimiterFor("url", "text/x-html"));
    assertSame(limiter, crl.getRateLimiterFor("url", "application/pdf"));
    limiter = crl.getRateLimiterFor("url", "text/xml");
    assertEquals("31/4159", limiter.getRate());
    assertSame(limiter, crl.getRateLimiterFor("url", "text/bar"));

    limiter = crl.getRateLimiterFor("url", "image/gif");
    assertEquals("5/1s", limiter.getRate());
    assertSame(limiter, crl.getRateLimiterFor("url", "image/png"));

    limiter = crl.getRateLimiterFor("url", "noimage/gif");
    assertEquals("22/23", limiter.getRate());
    assertSame(limiter, crl.getRateLimiterFor("url", "application/*"));
  }

  public void testUrl() {
    RateLimiterInfo rli = new RateLimiterInfo("key1", 50000);
    Map<String,String> urlPats =
      MapUtil.map("(\\.gif$)|(\\.jpeg$)|(\\.png$)", "5/1s",
		  "(\\.html$)|(\\.pdf$)", "10/1m");
    rli.setUrlRates(urlPats);
    CrawlRateLimiter crl = CrawlRateLimiter.Util.forRli(rli);
    RateLimiter limiter = crl.getRateLimiterFor("http://foo.bar/x.png",
						"text/html");
    assertEquals("5/1s", limiter.getRate());
    assertSame(limiter, crl.getRateLimiterFor("http://foo.bar/x.jpeg",
					      "text/html"));
    assertSame(limiter, crl.getRateLimiterFor("http://foo.bar/x.gif",
					      "text/html"));
    RateLimiter defLimiter = crl.getRateLimiterFor("http://foo.bar/x.toc",
						   "text/xml");
    assertEquals("1/50000", defLimiter.getRate());
    assertSame(defLimiter, crl.getRateLimiterFor("http://foo.bar/y.toc",
						 "foo/bar"));
    assertSame(defLimiter, crl.getRateLimiterFor("url", null));
    // null URL shouldn't happen, but ensure it doesn't throw
    assertSame(defLimiter, crl.getRateLimiterFor(null, null));
  }

  public void testPause() {
    TimeBase.setSimulated(1000);
    RateLimiterInfo rli = new RateLimiterInfo("key1", 50000);
    Map<String,String> urlPats =
      MapUtil.map("\\.(gif)|(jpeg)|(png)$", "5/1s",
		  "\\.(html)|(pdf)$", "10/1m");
    rli.setUrlRates(urlPats);
    CrawlRateLimiter crl = new MyCrawlRateLimiter(rli);
    MockRateLimiter defLimiter =
      (MockRateLimiter)crl.getRateLimiterFor("random.file", null);
    MockRateLimiter imageLimiter =
      (MockRateLimiter)crl.getRateLimiterFor("foo.png", null);
    MockRateLimiter artLimiter =
      (MockRateLimiter)crl.getRateLimiterFor("bar.pdf", null);
    assertEmpty(defLimiter.eventList);
    assertEquals(0, crl.getPauseCounter());
    crl.pauseBeforeFetch("foo.bar", null);
    assertEquals(1, crl.getPauseCounter());
    assertEquals(ListUtil.list("fifoWaitAndSignalEvent"), defLimiter.eventList);
    assertEmpty(imageLimiter.eventList);
    assertEmpty(artLimiter.eventList);
    crl.pauseBeforeFetch("Mao.jpeg", null);
    assertEquals(2, crl.getPauseCounter());
    assertEquals(ListUtil.list("fifoWaitAndSignalEvent"), defLimiter.eventList);
    assertEquals(ListUtil.list("fifoWaitAndSignalEvent"),
		 imageLimiter.eventList);
    crl.pauseBeforeFetch("bar.foo", null);
    assertEquals(ListUtil.list("fifoWaitAndSignalEvent"),
		 imageLimiter.eventList);
    assertEquals(ListUtil.list("fifoWaitAndSignalEvent",
			       "fifoWaitAndSignalEvent"),
		 defLimiter.eventList);
    crl.pauseBeforeFetch("Lenin.png", null);
    assertEquals(ListUtil.list("fifoWaitAndSignalEvent",
			       "fifoWaitAndSignalEvent"),
		 defLimiter.eventList);
    assertEquals(ListUtil.list("fifoWaitAndSignalEvent",
			       "fifoWaitAndSignalEvent"),
		 imageLimiter.eventList);
    assertEmpty(artLimiter.eventList);
    crl.pauseBeforeFetch("Lenin.html", null);
    assertEquals(5, crl.getPauseCounter());
    assertEquals(ListUtil.list("fifoWaitAndSignalEvent"),
		 artLimiter.eventList);
  }

  public void testConditional() throws Exception {
    // Large numerators prevent pauseBeforeFetch() from sleeping, allowing
    // use of simulated time
    RateLimiterInfo rli = new RateLimiterInfo("bar", "5/13000");
    CrawlWindow win1 = new CrawlWindows.Daily("8:00", "19:00", "GMT");
    CrawlWindow win2 = new CrawlWindows.Daily("19:00", "2:00", "GMT");
    CrawlWindow win3 = new CrawlWindows.Always();
    RateLimiterInfo rli1 = new RateLimiterInfo("one", "3/300");
    RateLimiterInfo rli2 = new RateLimiterInfo("two", "4/500")
      .setUrlRates(MapUtil.map(".*\\.pdf", "10/2s", ".*/images/.*", "20/1"));
    RateLimiterInfo rli3 = new RateLimiterInfo("three", "5/600");
    LinkedHashMap map = new LinkedHashMap();
    map.put(win1, rli1);
    map.put(win2, rli2);
    map.put(win3, rli3);
    rli.setCond(map);
    CrawlRateLimiter crl = CrawlRateLimiter.Util.forRli(rli);

    // win1
    TimeBase.setSimulated("1970/1/1 13:00:00");
    assertEquals("3/300", crl.getRateLimiterFor("foo.pdf", null).getRate());
    assertEquals("3/300", crl.getRateLimiterFor("foo.bar", null).getRate());
    crl.pauseBeforeFetch("foo.pdf", null);
    assertEquals(1, crl.getPauseCounter());
    crl.pauseBeforeFetch("foo.pdf", null);
    assertEquals(2, crl.getPauseCounter());

    // Change to win2
    TimeBase.setSimulated("1970/1/1 22:00:00");
    assertEquals("10/2s", crl.getRateLimiterFor("foo.pdf", null).getRate());
    assertEquals("4/500", crl.getRateLimiterFor("foo.bar", null).getRate());
    crl.pauseBeforeFetch("foo.pdf", null);
    // window change should cause two pauses
    assertEquals(4, crl.getPauseCounter());
    assertEquals("10/2s", crl.getRateLimiterFor("foo.pdf", null).getRate());
    assertEquals("4/500", crl.getRateLimiterFor("foo.bar", null).getRate());
    // Stay in win2
    TimeBase.setSimulated("1970/1/1 22:30:00");
    crl.pauseBeforeFetch("foo.pdf", null);
    assertEquals(5, crl.getPauseCounter());
    assertEquals("10/2s", crl.getRateLimiterFor("foo.pdf", null).getRate());
    // Stay in win2
    TimeBase.setSimulated("1970/1/1 1:45:0");
    crl.pauseBeforeFetch("foo.pdf", null);
    assertEquals(6, crl.getPauseCounter());
    assertEquals("10/2s", crl.getRateLimiterFor("foo.pdf", null).getRate());

    // Change to Always window
    TimeBase.setSimulated("1970/1/1 4:30:0");
    crl.pauseBeforeFetch("foo.pdf", null);
    assertEquals(8, crl.getPauseCounter());
    assertEquals("5/600", crl.getRateLimiterFor("foo.pdf", null).getRate());
    assertEquals("5/600", crl.getRateLimiterFor("foo.bar", null).getRate());

    // Back to win2
    TimeBase.setSimulated("1970/1/2 1:45:0");
    crl.pauseBeforeFetch("foo.pdf", null);
    assertEquals(10, crl.getPauseCounter());
    assertEquals("10/2s", crl.getRateLimiterFor("foo.pdf", null).getRate());
  }

  static class MyCrawlRateLimiter extends FileTypeCrawlRateLimiter {
  
    MyCrawlRateLimiter(RateLimiterInfo rli) {
      super(rli);
    }

    @Override
    protected RateLimiter newRateLimiter(String rate) {
      return new MockRateLimiter(rate);
    }

    @Override
    protected RateLimiter newRateLimiter(int events, long interval) {
      return new MockRateLimiter(events, interval);
    }
  }


}
