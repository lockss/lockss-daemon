/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ojs2;

import java.io.IOException;
import java.text.*;
import java.util.*;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.crawler.CrawlSeed;
import org.lockss.daemon.*;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestOJS2CrawlSeedFactory extends LockssTestCase {
  
  protected MockLockssDaemon theDaemon;
  protected MockArchivalUnit mau = null;
  protected MockArchivalUnit dual_mau = null;
  protected MockAuState aus = new MockAuState();
  protected MockCrawlRule crawlRule = null;
  protected List<String> permissionUrls;
  protected String startUrl = "http://www.example.com/index.php/jid/gateway/lockss?year=2020";
  protected List<String> startUrls;
  protected MockLinkExtractor extractor = new MockLinkExtractor();
  protected OJS2CrawlSeedFactory csf;
  protected CrawlSeed cs, dual_cs;
  protected MockServiceProvider msp;
  protected Configuration config;
  protected DateFormat df;
  
  public void setUp() throws Exception {
    super.setUp();
    
    theDaemon = getMockLockssDaemon();
    mau = new MockArchivalUnit();
    mau.setPlugin(new MockPlugin(theDaemon));
    mau.setAuId("MyMockTestAu");
    startUrls = ListUtil.list(startUrl);
    mau.setStartUrls(startUrls);
    
    dual_mau = new MockArchivalUnit();
    dual_mau.setPlugin(new MockPlugin(theDaemon));
    dual_mau.setAuId("MyMockTestAu");
    
    // %sindex.php/%s/gateway/lockss?year=%d", base_url, journal_id, year
    config = ConfigManager.newConfiguration();
    config.put(ConfigParamDescr.JOURNAL_ID.getKey(), "jid");
    config.put(ConfigParamDescr.YEAR.getKey(), "2020");
    config.put(ConfigParamDescr.BASE_URL.getKey(), "http://www.example.com/");
    mau.setConfiguration(config);
    csf = new OJS2CrawlSeedFactory();
    cs = csf.createCrawlSeed(new MockCrawler().new MockCrawlerFacade(mau));
    config.put(ConfigParamDescr.BASE_URL.getKey(), "https://ejournals.library.ualberta.ca/");
    dual_mau.setConfiguration(config);
    dual_cs = csf.createCrawlSeed(new MockCrawler().new MockCrawlerFacade(dual_mau));
  }
  
  public void testNullAu() throws PluginException, ConfigurationException {
    try {
      // XXX fail("should throw because there is no au");
    } catch(IllegalArgumentException e) {
      assertMatchesRE("Valid ArchivalUnit", e.getMessage());
    }
  }
  
  public void testStartUrl() 
      throws ConfigurationException, PluginException, IOException {
    assertEquals(startUrls, cs.getStartUrls());
  }
  
  public void testDualStartUrl() 
      throws ConfigurationException, PluginException, IOException {
    dual_mau.setStartUrls(ListUtil.list("https://ejournals.library.ualberta.ca/index.php/jid/gateway/lockss?year=2020"));
    List<String> startUrls = ListUtil.list("http://ejournals.library.ualberta.ca/index.php/jid/gateway/lockss?year=2020", 
                                          "https://ejournals.library.ualberta.ca/index.php/jid/gateway/lockss?year=2020");
    assertEquals(startUrls, dual_cs.getStartUrls());
  }
  
  public void testIsFailOnStartUrl() {
    assertTrue(cs.isFailOnStartUrlError());
  }
  
}
