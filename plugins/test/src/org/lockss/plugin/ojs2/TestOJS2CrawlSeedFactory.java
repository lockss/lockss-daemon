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

import org.apache.commons.collections.ListUtils;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.crawler.CrawlSeed;
import org.lockss.daemon.*;
import org.lockss.daemon.PluginException.InvalidDefinition;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestOJS2CrawlSeedFactory extends LockssTestCase {
  
  protected MockLockssDaemon theDaemon;
  protected MockArchivalUnit mau = null;
  protected MockArchivalUnit dual_mau = null;
  protected MockArchivalUnit noindex_mau = null;
  protected MockAuState aus = new MockAuState();
  protected MockCrawlRule crawlRule = null;
  protected String permissionUri = "index.php/jid/about/editorialPolicies";
  protected String permissionNoIndexUri = "jid/about/editorialPolicies";
  protected List<String> permissionUrls;
  protected String startUrl = "http://www.example.com/index.php/jid/gateway/lockss?year=2020";
  protected List<String> startUrls;
  protected MockLinkExtractor extractor = new MockLinkExtractor();
  protected OJS2CrawlSeedFactory csf;
  protected CrawlSeed cs, dual_cs, noindex_cs;
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

    // to test the other type of crawl seed 
    noindex_mau = new MockArchivalUnit();
    noindex_mau.setPlugin(new MockPlugin(theDaemon));
    noindex_mau.setAuId("MyMockTestAu");
    
    // %sindex.php/%s/gateway/lockss?year=%d", base_url, journal_id, year
    config = ConfigManager.newConfiguration();
    config.put(ConfigParamDescr.JOURNAL_ID.getKey(), "jid");
    config.put(ConfigParamDescr.YEAR.getKey(), "2020");
    config.put(ConfigParamDescr.BASE_URL.getKey(), "http://www.example.com/");
    mau.setConfiguration(config);
    csf = new OJS2CrawlSeedFactory();
    cs = csf.createCrawlSeed(new MockCrawler().new MockCrawlerFacade(mau));
    // currently the only host that causes a special CrawlSeed to be created is ejournals.library.ualberta.ca
    config.put(ConfigParamDescr.BASE_URL.getKey(), "https://ejournals.library.ualberta.ca/");
    dual_mau.setConfiguration(config);
    dual_cs = csf.createCrawlSeed(new MockCrawler().new MockCrawlerFacade(dual_mau));
    // currently the only host that causes a special CrawlSeed without index.php
    config.put(ConfigParamDescr.BASE_URL.getKey(), "https://mulpress.mcmaster.ca/");
    noindex_mau.setConfiguration(config);
    noindex_cs = csf.createCrawlSeed(new MockCrawler().new MockCrawlerFacade(noindex_mau));

  }
  
  public void testNullAu() throws PluginException, ConfigurationException {
    try {
      // XXX fail("should throw because there is no au");
    } catch(IllegalArgumentException e) {
      assertMatchesRE("Valid ArchivalUnit", e.getMessage());
    }
  }
  
  public void testBadPermissionUrlListThrows() 
      throws ConfigurationException, PluginException, IOException {
    
    mau.setStartUrls(null);
    mau.setPermissionUrls(null);
    try {
      cs.getPermissionUrls();
      fail("null permission url list should have thrown");
    } catch(InvalidDefinition e) {
      assertMatchesRE("non-null permission URL list", e.getMessage());
    }
    
    mau.setPermissionUrls(ListUtils.EMPTY_LIST);
    try {
      cs.getPermissionUrls();
      fail("empty permission url list should have thrown");
    } catch(InvalidDefinition e) {
      assertMatchesRE("non-null permission URL list", e.getMessage());
    }
  }
  
  public void testPermissionUrl() 
      throws ConfigurationException, PluginException, IOException {
    assertEquals(startUrls, cs.getPermissionUrls());
  }
  
  public void testDualPermissionUrl() 
      throws ConfigurationException, PluginException, IOException {
    permissionUrls = ListUtil.list("https://ejournals.library.ualberta.ca/" + permissionUri);
    dual_mau.setPermissionUrls(permissionUrls);
    List<String> dualUrls = ListUtil.list("http://ejournals.library.ualberta.ca/" + permissionUri,
                                         "https://ejournals.library.ualberta.ca/" + permissionUri);
    assertEquals(dualUrls, dual_cs.getPermissionUrls());
  }
  
  public void testNoIndexPermissionUrl() 
      throws ConfigurationException, PluginException, IOException {
    permissionUrls = ListUtil.list("https://mulpress.mcmaster.ca/" + permissionUri);
    noindex_mau.setPermissionUrls(permissionUrls);
    List<String> permUrls = ListUtil.list("https://mulpress.mcmaster.ca/" + permissionNoIndexUri);
    assertEquals(permUrls, noindex_cs.getPermissionUrls());
  }
  
  public void testStartUrl() 
      throws ConfigurationException, PluginException, IOException {
    assertEquals(startUrls, cs.getStartUrls());
  }
  
  public void testDualStartUrl() 
      throws ConfigurationException, PluginException, IOException {
    dual_mau.setStartUrls(ListUtil.list("https://ejournals.library.ualberta.ca/index.php/jid/gateway/lockss?year=2020"));
    List<String> dualUrls = ListUtil.list("http://ejournals.library.ualberta.ca/index.php/jid/gateway/lockss?year=2020", 
                                          "https://ejournals.library.ualberta.ca/index.php/jid/gateway/lockss?year=2020");
    assertEquals(dualUrls, dual_cs.getStartUrls());
  }

  public void testNoIndexStartUrl() 
      throws ConfigurationException, PluginException, IOException {
    noindex_mau.setStartUrls(ListUtil.list("https://mulpress.mcmaster.ca/index.php/jid/gateway/lockss?year=2020"));
    List<String> noindexUrls = ListUtil.list("https://mulpress.mcmaster.ca/jid/gateway/lockss?year=2020");
    assertEquals(noindexUrls, noindex_cs.getStartUrls());
  }
  
  public void testIsFailOnStartUrl() {
    assertTrue(cs.isFailOnStartUrlError());
  }
  
}
