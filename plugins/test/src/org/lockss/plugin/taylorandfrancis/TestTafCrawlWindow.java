/*
 * $Id: TestGPOFDSysCrawlWindow.java 39864 2015-02-18 09:10:24Z thib_gc $
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

package org.lockss.plugin.taylorandfrancis;

import java.text.*;
import java.util.*;

import org.lockss.config.Configuration;
import org.lockss.crawler.CrawlRateLimiter;
import org.lockss.daemon.*;
import org.lockss.plugin.RateLimiterInfo;
import org.lockss.plugin.base.BaseArchivalUnit;
import org.lockss.plugin.definable.*;
import org.lockss.test.*;
import org.lockss.util.TimeBase;

/**
 * <p>
 * This plugin currently has variable crawl rates that emulate a crawl window,
 * to allow deep recrawls longer than the crawl window to succeed.
 * </p>
 */
public class TestTafCrawlWindow extends LockssTestCase {

  protected DefinableArchivalUnit au;
  
  protected SimpleDateFormat cestSdf;
  
  protected SimpleDateFormat gmtSdf;
  
  public String slowRate = "1/5s";
  public String fastRate = "1/2s";
  
  public void setUp() throws Exception {
    super.setUp();
    // Set up date/time tools
    TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    cestSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    cestSdf.setTimeZone(TimeZone.getTimeZone("CET"));
    gmtSdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    gmtSdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    // Set min fetch delay lower than the plugin's lowest, to test default
    ConfigurationUtil.addFromArgs(BaseArchivalUnit.PARAM_MIN_FETCH_DELAY, "123");
    Configuration config = ConfigurationUtil.fromArgs(ConfigParamDescr.BASE_URL.getKey(),
                                                      "http://www.example.com/",
                                                      ConfigParamDescr.JOURNAL_ID.getKey(),
                                                      "fizz",
                                                      ConfigParamDescr.VOLUME_NAME.getKey(),
                                                      "13");
    DefinablePlugin dp = new DefinablePlugin();
    dp.initPlugin(getMockLockssDaemon(), "org.lockss.plugin.taylorandfrancis.TaylorAndFrancisPlugin");
    au = (DefinableArchivalUnit)dp.createAu(config);
  }
  
  public void testWindowingCrawlRates() throws Exception {
    RateLimiterInfo rli = au.getRateLimiterInfo();
    assertEquals(slowRate, rli.getDefaultRate()); // Ignored
    CrawlRateLimiter crl = CrawlRateLimiter.Util.forRli(rli);

    /*
     * PST
     */
    // 2014-01-13 through 2014-01-17 are a Monday through Friday
    for (int d = 13 /* Monday */ ; d <= 17 /* Friday */ ; ++d) {
      TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-01-" + d + " 08:59:59")));
      assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
      TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-01-" + d + " 09:00:00")));
      assertEquals(slowRate, crl.getRateLimiterFor("foo", null).getRate());
      TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-01-" + d + " 20:59:59")));
      assertEquals(slowRate, crl.getRateLimiterFor("foo", null).getRate());
      TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-01-" + d + " 21:00:00")));
      assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    }
    // 2014-01-18 is a Saturday
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-01-18 01:59:59")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-01-18 02:00:00")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-01-18 07:59:59")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-01-18 08:00:00")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-01-18 09:59:59")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-01-18 10:00:00")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-01-18 29:59:59")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-01-18 20:00:00")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    // 2014-01-19 is a Sunday
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-01-19 01:59:59")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-01-19 02:00:00")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-01-19 07:59:59")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-01-19 08:00:00")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-01-19 09:59:59")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-01-19 10:00:00")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-01-19 29:59:59")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-01-19 20:00:00")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    
    /*
     * PDT
     */
    // 2014-08-11 through 2014-08-15 are a Monday through Friday
    for (int d = 11 /* Monday */ ; d <= 15 /* Friday */ ; ++d) {
      TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-08-" + d + " 08:59:59")));
      assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
      TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-08-" + d + " 09:00:00")));
      assertEquals(slowRate, crl.getRateLimiterFor("foo", null).getRate());
      TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-08-" + d + " 20:59:59")));
      assertEquals(slowRate, crl.getRateLimiterFor("foo", null).getRate());
      TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-08-" + d + " 21:00:00")));
      assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    }
    // 2014-08-16 is a Saturday
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-08-16 01:59:59")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-08-16 02:00:00")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-08-16 07:59:59")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-08-16 08:00:00")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-08-16 09:59:59")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-08-16 10:00:00")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-08-16 19:59:59")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-08-16 20:00:00")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    // 2014-08-17 is a Sunday
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-08-17 01:59:59")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-08-17 02:00:00")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-08-17 07:59:59")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-08-17 08:00:00")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-08-17 09:59:59")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-08-17 10:00:00")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-08-17 19:59:59")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(cestSdf.parse("2014-08-17 20:00:00")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
  }

}
