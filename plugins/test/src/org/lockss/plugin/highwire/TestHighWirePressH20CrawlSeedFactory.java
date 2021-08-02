/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.highwire;

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

public class TestHighWirePressH20CrawlSeedFactory extends LockssTestCase {
  
  protected MockLockssDaemon theDaemon;
  protected MockArchivalUnit mau = null;
  protected MockArchivalUnit oup_mau = null;
  protected MockAuState aus = new MockAuState();
  protected MockCrawlRule crawlRule = null;
  protected String permissionUrl = "http://www.example.com/permission.html";
  protected List<String> permissionUrls;
  protected String startUrl = "http://www.example.com/lockss-manifest/vol_20_manifest.dtl";
  protected List<String> startUrls;
  protected MockLinkExtractor extractor = new MockLinkExtractor();
  protected HighWirePressH20CrawlSeedFactory csf;
  protected CrawlSeed cs, oup_cs;
  protected MockServiceProvider msp;
  protected Configuration config;
  protected DateFormat df;
  
  public void setUp() throws Exception {
    super.setUp();
    
    theDaemon = getMockLockssDaemon();
    mau = new MockArchivalUnit();
    mau.setPlugin(new MockPlugin(theDaemon));
    mau.setAuId("MyMockTestAu");
    permissionUrls = ListUtil.list(permissionUrl);
    startUrls = ListUtil.list(startUrl);
    mau.setPermissionUrls(permissionUrls);
    mau.setStartUrls(startUrls);
    
    oup_mau = new MockArchivalUnit();
    oup_mau.setPlugin(new MockPlugin(theDaemon));
    oup_mau.setAuId("MyMockTestAu");
    
    config = ConfigManager.newConfiguration();
    config.put(ConfigParamDescr.YEAR.getKey(), "1988");
    config.put(ConfigParamDescr.BASE_URL.getKey(), "http://www.example.com/");
    mau.setConfiguration(config);
    csf = new HighWirePressH20CrawlSeedFactory();
    cs = csf.createCrawlSeed(new MockCrawler().new MockCrawlerFacade(mau));
    config.put(ConfigParamDescr.BASE_URL.getKey(), "http://www.oxfordjournals.org/");
    oup_mau.setConfiguration(config);
    oup_cs = csf.createCrawlSeed(new MockCrawler().new MockCrawlerFacade(oup_mau));
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
    assertEquals(permissionUrls, cs.getPermissionUrls());
  }
  
  public void testStartUrl() 
      throws ConfigurationException, PluginException, IOException {
    assertEquals(startUrls, cs.getStartUrls());
  }
  
  public void testOupStartUrl() 
      throws ConfigurationException, PluginException, IOException {
    List<String> startUrls = ListUtil.list("http://www.oxfordjournals.org/lockss-manifest/vol_20_manifest.dtl", 
                                          "https://www.oxfordjournals.org/lockss-manifest/vol_20_manifest.dtl");
    oup_mau.setStartUrls(ListUtil.list("http://www.oxfordjournals.org/lockss-manifest/vol_20_manifest.dtl"));
    assertEquals(startUrls, oup_cs.getStartUrls());
  }
  
  public void testIsFailOnStartUrl() {
    assertTrue(cs.isFailOnStartUrlError());
  }
  
}
