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
import org.lockss.util.TimeZoneUtil;

/**
 * <p>
 * This plugin currently has variable crawl rates that emulate a crawl window,
 * to allow deep recrawls longer than the crawl window to succeed.
 * </p>
 */
public class TestTafCrawlWindow extends LockssTestCase {

  protected DefinableArchivalUnit au;
  
  protected SimpleDateFormat newYorkSdf;
  
  protected SimpleDateFormat gmtSdf;
  
  public String slowRate = "1/3500";
  public String fastRate = "1/2s";
  
  public void setUp() throws Exception {
    super.setUp();
    // Set up date/time tools
    TimeZone.setDefault(TimeZoneUtil.getExactTimeZone("GMT"));
    newYorkSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    newYorkSdf.setTimeZone(TimeZoneUtil.getExactTimeZone("America/New_York"));
    gmtSdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    gmtSdf.setTimeZone(TimeZoneUtil.getExactTimeZone("GMT"));
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

    // 2014-01-13 through 2014-01-17 are a Monday through Friday
    for (int d = 13 /* Monday */ ; d <= 17 /* Friday */ ; ++d) {
      TimeBase.setSimulated(gmtSdf.format(newYorkSdf.parse("2014-01-" + d + " 02:59:59")));
      assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
      TimeBase.setSimulated(gmtSdf.format(newYorkSdf.parse("2014-01-" + d + " 03:00:00")));
      assertEquals(slowRate, crl.getRateLimiterFor("foo", null).getRate());
      TimeBase.setSimulated(gmtSdf.format(newYorkSdf.parse("2014-01-" + d + " 14:59:59")));
      assertEquals(slowRate, crl.getRateLimiterFor("foo", null).getRate());
      TimeBase.setSimulated(gmtSdf.format(newYorkSdf.parse("2014-01-" + d + " 15:00:00")));
      assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    }
    // 2014-01-18 is a Saturday
    TimeBase.setSimulated(gmtSdf.format(newYorkSdf.parse("2014-01-18 02:59:59")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(newYorkSdf.parse("2014-01-18 03:00:00")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(newYorkSdf.parse("2014-01-18 14:59:59")));
    assertEquals(fastRate, crl.getRateLimiterFor("foo", null).getRate());
    TimeBase.setSimulated(gmtSdf.format(newYorkSdf.parse("2014-01-18 15:00:00")));
  }

}
