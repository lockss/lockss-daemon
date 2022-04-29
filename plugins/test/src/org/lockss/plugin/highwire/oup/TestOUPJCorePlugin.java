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

package org.lockss.plugin.highwire.oup;

import java.net.MalformedURLException;
import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.definable.*;
import org.lockss.test.*;
import org.lockss.util.ListUtil;
import org.lockss.util.urlconn.CacheException;
import org.lockss.util.urlconn.HttpResultMap;

public class TestOUPJCorePlugin extends LockssTestCase {
  
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  
  private DefinablePlugin plugin;
  private MockLockssDaemon theDaemon;
  
  public TestOUPJCorePlugin(String msg) {
    super(msg);
  }
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
        "org.lockss.plugin.highwire.oup.OUPDrupalPlugin");
  }
  
  public void testGetAuNullConfig()
      throws ArchivalUnit.ConfigurationException {
    try {
      plugin.configureAu(null, null);
      fail("Didn't throw ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
  public void testCreateAu() throws ConfigurationException {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    props.setProperty(VOL_KEY, "32");
    makeAuFromProps(props);
  }
  
  private DefinableArchivalUnit makeAuFromProps(Properties props)
      throws ArchivalUnit.ConfigurationException {
    Configuration config = ConfigurationUtil.fromProps(props);
    return (DefinableArchivalUnit)plugin.configureAu(config, null);
  }
  
  public void testGetAuConstructsProperAu()
      throws ArchivalUnit.ConfigurationException, MalformedURLException {
    Properties props = new Properties();
    props.setProperty(VOL_KEY, "303");
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    
    String starturl[] = {
        "http://www.example.com/lockss-manifest/vol_303_manifest.html", 
        "https://www.example.com/lockss-manifest/vol_303_manifest.html", 
    };
    DefinableArchivalUnit au = makeAuFromProps(props);
    assertEquals("Oxford University Press Journals Plugin (Legacy), Base URL http://www.example.com/, Volume 303",
        au.getName());
    assertEquals(ListUtil.list(starturl[0],starturl[1]), au.getStartUrls());
  }
  
  public void testGetPluginId() {
    assertEquals("org.lockss.plugin.highwire.oup.OUPDrupalPlugin",
        plugin.getPluginId());
  }
  
  public void testGetAuConfigProperties() {
    assertEquals(ListUtil.list(ConfigParamDescr.BASE_URL,
        ConfigParamDescr.VOLUME_NAME),
        plugin.getLocalAuConfigDescrs());
  }
  
  public void testHandles500Result() throws Exception {
    Properties props = new Properties();
    props.setProperty(VOL_KEY, "322");
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    
    String starturl =
        "http://www.example.com/lockss-manifest/vol_322_manifest.html";
    DefinableArchivalUnit au = makeAuFromProps(props);
    MockLockssUrlConnection conn = new MockLockssUrlConnection();
    conn.setURL("http://uuu17/");
    CacheException exc =
        ((HttpResultMap)plugin.getCacheResultMap()).mapException(au, conn,
            500, "foo");
    assertClass(CacheException.RetryDeadLinkException.class, exc);
    //
    conn.setURL(starturl);
    exc = ((HttpResultMap)plugin.getCacheResultMap()).mapException(au, conn,
        500, "foo");
    assertClass(CacheException.RetrySameUrlException.class, exc);
    
  }
  /*
http://jinsectscience.oxfordjournals.org/content/1/1/1.figures-only
http://jinsectscience.oxfordjournals.org/sites/default/files/cdn/css/http/css__ksIsPG2Vk7BP82i7eOMzW-mf5Lre730iah97Cj8_Ac.css
http://jinsectscience.oxfordjournals.org/content/1/1.toc
http://jinsectscience.oxfordjournals.org/highwire/article_citation_preview/61258
   */
  // Test the crawl rules for APS
  public void testShouldCacheProperPages() throws Exception {
    String ROOT_URL = "http://ex.oxfordjournals.org/";
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, ROOT_URL);
    props.setProperty(VOL_KEY, "303");
    DefinableArchivalUnit au = null;
    try {
      au = makeAuFromProps(props);
    }
    catch (ConfigurationException ex) {
    }
    theDaemon.getLockssRepository(au);
    
    // Test for pages that should get crawled or not
    // permission page/start url
    shouldCacheTest(ROOT_URL + "lockss-manifest/vol_303_manifest.html", true, au);
    shouldCacheTest(ROOT_URL + "clockss-manifest/vol_303_manifest.html", true, au);
    shouldCacheTest(ROOT_URL + "manifest/year=2013", false, au);
    // vol and issue
    // shouldCacheTest(ROOT_URL + "content/303", false, au); changed crawl rule as there are some volume only tocs (iwa) so this test is true
    shouldCacheTest(ROOT_URL + "content/303/2", true, au);
    shouldCacheTest(ROOT_URL + "content/303/2.toc", true, au);
    // article files
    shouldCacheTest(ROOT_URL + "content/303/2/X3", true, au);
    shouldCacheTest(ROOT_URL.replace("http:", "https:") + "content/303/2/X3", true, au);
    shouldCacheTest(ROOT_URL + "content/303/2/X3.abstract", false, au);
    shouldCacheTest(ROOT_URL + "content/303/2/X3.extract", false, au);
    shouldCacheTest(ROOT_URL + "content/303/2/X3.full", false, au);
    shouldCacheTest(ROOT_URL + "content/303/2/X3.full.pdf", true, au);
    shouldCacheTest(ROOT_URL + "content/303/2/X3.full.pdf+html", true, au);
    shouldCacheTest(ROOT_URL + "content/303/2/X3.long", true, au);
    shouldCacheTest(ROOT_URL + "content/303/2/X3/article-info", false, au);
    shouldCacheTest(ROOT_URL + "content/303/2/X3.figures-only", true, au);
    shouldCacheTest(ROOT_URL + "content/303/2/X3.", false, au);
    
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/jnl_ex_tab_data/node:433/1", false, au);
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/jnl_ex_tab_info/node:433/1", false, au);
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/jnl_ex_tab_pdf/node:433/1", false, au);
    
    shouldCacheTest(ROOT_URL + "highwire/markup/58493/expansion", false, au);
    shouldCacheTest(ROOT_URL + "highwire/article_citation_preview/19403", false, au);
    
    shouldCacheTest(ROOT_URL + "node/34", false, au);
// XXX   shouldCacheTest(ROOT_URL + "content/by/year", false, au);
    shouldCacheTest(ROOT_URL + "highwire/powerpoint/2311", false, au);
    shouldCacheTest(ROOT_URL + "content/early/2012/11/09/ex.00163.2012", false, au);
    shouldCacheTest(ROOT_URL + "lookup/external-ref?link_type=GEN", false, au);
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/jnl_ex_tab_art/node:433/1", false, au);
    shouldCacheTest(ROOT_URL + "content/303/2/X3.article-info", false, au);
    
    shouldCacheTest("http://cdn.mathjax.org/mathjax/latest/MathJax.js", false, au);
    shouldCacheTest("https://ajax.googleapis.com/ajax/libs/jquery/1.8.2/jquery.min.js", false, au);
    shouldCacheTest("", false, au);
    
    // should not get crawled - LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, au);
    
  }
  
  private void shouldCacheTest(String url, boolean shouldCache, ArchivalUnit au) {
    log.info ("shouldCacheTest url: " + url);
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
}