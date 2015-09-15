/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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
  protected MockAuState aus = new MockAuState();
  protected MockCrawlRule crawlRule = null;
  protected String permissionUrl = "http://www.example.com/permission.html";
  protected List<String> permissionUrls;
  protected MockLinkExtractor extractor = new MockLinkExtractor();
  protected HighWirePressH20CrawlSeedFactory csf;
  protected CrawlSeed cs;
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
    mau.setPermissionUrls(permissionUrls);    
    
    config = ConfigManager.newConfiguration();
    config.put(ConfigParamDescr.YEAR.getKey(), "1988");
    config.put(ConfigParamDescr.BASE_URL.getKey(), "http://www.example.com/");
    mau.setConfiguration(config);
    csf = new HighWirePressH20CrawlSeedFactory();
    cs = csf.createCrawlSeed(new MockCrawler().new MockCrawlerFacade(mau));
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
  
  public void testIsFailOnStartUrl() {
    assertTrue(cs.isFailOnStartUrlError());
  }
  
}
