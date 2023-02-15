/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.usdocspln.gov.gpo.fdsys;

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
import org.lockss.util.TimeZoneUtil;

/**
 * <p>
 * This plugin currently has variable crawl rates that emulate a crawl window,
 * to allow deep recrawls longer than the crawl window to succeed.
 * </p>
 */
public class TestGPOFDSysCrawlWindow extends LockssTestCase {

  protected DefinableArchivalUnit au;
  
  protected SimpleDateFormat easternSdf;
  
  protected SimpleDateFormat gmtSdf;
  
  public void setUp() throws Exception {
    super.setUp();
    // Set up date/time tools
    TimeZone.setDefault(TimeZoneUtil.getExactTimeZone("GMT"));
    easternSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    easternSdf.setTimeZone(TimeZoneUtil.getExactTimeZone("US/Eastern"));
    gmtSdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    gmtSdf.setTimeZone(TimeZoneUtil.getExactTimeZone("GMT"));
    // Set min fetch delay lower than the plugin's lowest, to test default
    ConfigurationUtil.addFromArgs(BaseArchivalUnit.PARAM_MIN_FETCH_DELAY, "123");
    Configuration config = ConfigurationUtil.fromArgs(ConfigParamDescr.BASE_URL.getKey(),
                                                      "http://www.example.com/",
                                                      "collection_id",
                                                      "GOVXYZ",
                                                      ConfigParamDescr.YEAR.getKey(),
                                                      "2013");
    DefinablePlugin dp = new DefinablePlugin();
    dp.initPlugin(getMockLockssDaemon(), "org.lockss.plugin.usdocspln.gov.gpo.fdsys.GPOFDSysSitemapsPlugin");
    au = (DefinableArchivalUnit)dp.createAu(config);
  }
  
  public void testWindowingCrawlRates() throws Exception {
    RateLimiterInfo rli = au.getRateLimiterInfo();
    assertEquals("GPO", rli.getCrawlPoolKey());
    assertEquals("1/3s", rli.getDefaultRate()); // Ignored
    CrawlRateLimiter crl = CrawlRateLimiter.Util.forRli(rli);

    /*
     * PST
     */
    // 2014-01-13 through 2014-01-17 are a Monday through Friday
    for (int d = 13 /* Monday */ ; d <= 17 /* Friday */ ; ++d) {
      TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-01-" + d + " 08:59:59")));
      assertEquals("1/3s", crl.getRateLimiterFor("foo", null).getRate());
      TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-01-" + d + " 09:00:00")));
      assertEquals("1/1h", crl.getRateLimiterFor("foo", null).getRate());
      TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-01-" + d + " 20:59:59")));
      assertEquals("1/1h", crl.getRateLimiterFor("foo", null).getRate());
      TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-01-" + d + " 21:00:00")));
      assertEquals("1/3s", crl.getRateLimiterFor("foo", null).getRate());
    }
    // 2014-01-18 is a Saturday
    TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-01-18 01:59:59")));
    assertEquals("1/3s", crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-01-18 02:00:00")));
    assertEquals("1/3s", crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-01-18 08:59:59")));
    assertEquals("1/3s", crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-01-18 09:00:00")));
    assertEquals("1/3s", crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-01-18 09:59:59")));
    assertEquals("1/3s", crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-01-18 10:00:00")));
    assertEquals("1/3s", crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-01-18 20:59:59")));
    assertEquals("1/3s", crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-01-18 21:00:00")));
    assertEquals("1/3s", crl.getRateLimiterFor("foo", null).getRate());
    // 2014-01-19 is a Sunday
    TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-01-19 01:59:59")));
    assertEquals("1/3s", crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-01-19 02:00:00")));
    assertEquals("1/1h", crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-01-19 09:59:59")));
    assertEquals("1/1h", crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-01-19 10:00:00")));
    assertEquals("1/3s", crl.getRateLimiterFor("foo", null).getRate());
    
    /*
     * PDT
     */
    // 2014-08-11 through 2014-08-15 are a Monday through Friday
    for (int d = 11 /* Monday */ ; d <= 15 /* Friday */ ; ++d) {
      TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-08-" + d + " 08:59:59")));
      assertEquals("1/3s", crl.getRateLimiterFor("foo", null).getRate());
      TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-08-" + d + " 09:00:00")));
      assertEquals("1/1h", crl.getRateLimiterFor("foo", null).getRate());
      TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-08-" + d + " 20:59:59")));
      assertEquals("1/1h", crl.getRateLimiterFor("foo", null).getRate());
      TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-08-" + d + " 21:00:00")));
      assertEquals("1/3s", crl.getRateLimiterFor("foo", null).getRate());
    }
    // 2014-08-16 is a Saturday
    TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-08-16 01:59:59")));
    assertEquals("1/3s", crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-08-16 02:00:00")));
    assertEquals("1/3s", crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-08-16 08:59:59")));
    assertEquals("1/3s", crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-08-16 09:00:00")));
    assertEquals("1/3s", crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-08-16 09:59:59")));
    assertEquals("1/3s", crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-08-16 10:00:00")));
    assertEquals("1/3s", crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-08-16 20:59:59")));
    assertEquals("1/3s", crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-08-16 21:00:00")));
    assertEquals("1/3s", crl.getRateLimiterFor("foo", null).getRate());
    // 2014-08-17 is a Sunday
    TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-08-17 01:59:59")));
    assertEquals("1/3s", crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-08-17 02:00:00")));
    assertEquals("1/1h", crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-08-17 09:59:59")));
    assertEquals("1/1h", crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(easternSdf.parse("2014-08-17 10:00:00")));
    assertEquals("1/3s", crl.getRateLimiterFor("foo", null).getRate());
  }

}
