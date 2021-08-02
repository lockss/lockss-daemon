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

package org.lockss.plugin.highwire.bmj;

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

public class TestBMJJCorePlugin extends LockssTestCase {
  
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  
  private MockLockssDaemon theDaemon;
  private DefinablePlugin plugin;
  
  public TestBMJJCorePlugin(String msg) {
    super(msg);
  }
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
        "org.lockss.plugin.highwire.bmj.BMJDrupalPlugin");
    
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
    props.setProperty(VOL_KEY, "313");
    props.setProperty(BASE_URL_KEY, "https://www.example.com/");
    
    String starturl[] = {
        "http://www.example.com/lockss-manifest/vol_313_manifest.html",
        "https://www.example.com/lockss-manifest/vol_313_manifest.html", 
    };
    DefinableArchivalUnit au = makeAuFromProps(props);
    assertEquals("BMJ Plugin, Base URL https://www.example.com/, Volume 313",
        au.getName());
    assertEquals(ListUtil.list(starturl), au.getStartUrls());
  }
  
  public void testGetPluginId() {
    assertEquals("org.lockss.plugin.highwire.bmj.BMJDrupalPlugin",
        plugin.getPluginId());
  }
  
  public void testGetAuConfigProperties() {
    assertEquals(ListUtil.list(ConfigParamDescr.BASE_URL,
        ConfigParamDescr.VOLUME_NAME),
        plugin.getLocalAuConfigDescrs());
  }
  
  public void testHandles500Result() throws Exception {
    Properties props = new Properties();
    props.setProperty(VOL_KEY, "313");
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    
    String starturl =
        "http://www.example.com/lockss-manifest/vol_313_manifest.html";
    DefinableArchivalUnit au = makeAuFromProps(props);
    MockLockssUrlConnection conn = new MockLockssUrlConnection();
    conn.setURL("http://uuu17/");
    CacheException exc =
        ((HttpResultMap)plugin.getCacheResultMap()).mapException(au, conn,
            500, "foo");
    assertClass(CacheException.RetryDeadLinkException.class, exc);
    
    conn.setURL(starturl);
    exc = ((HttpResultMap)plugin.getCacheResultMap()).mapException(au, conn,
        500, "foo");
    assertClass(CacheException.RetrySameUrlException.class, exc);
    
  }
  
  // Test the crawl rules for BMJ
  public void testShouldCacheProperPages() throws Exception {
    String ROOT_URL = "http://www.bmj.com/";
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, ROOT_URL);
    props.setProperty(VOL_KEY, "321");
    DefinableArchivalUnit au = null;
    try {
      au = makeAuFromProps(props);
    }
    catch (ConfigurationException ex) {
    }
    theDaemon.getLockssRepository(au);
    
    // permission page/start url
    shouldCacheTest(ROOT_URL + "lockss-manifest/vol_321_manifest.html", true, au);
    // should not get crawled - wrong journal/vol
    shouldCacheTest(ROOT_URL + "lockss-manifest/vol_347_manifest.html", false, au);
    // toc page for an issue
    shouldCacheTest(ROOT_URL + "content/321/7915.toc", true, au);
    shouldCacheTest(ROOT_URL + "content/321/7915", true, au);
    shouldCacheTest(ROOT_URL + "content/347/7915.toc", true, au);
    // article files
    shouldCacheTest(ROOT_URL + "content/321/bmj.f6056.long", true, au);
    shouldCacheTest(ROOT_URL + "content/321/bmj.f4270", true, au);
    shouldCacheTest(ROOT_URL.replace("http:", "https:") + "content/321/bmj.f4270", true, au);
    shouldCacheTest(ROOT_URL + "content/321/bmj.f4270.full", false, au);
    shouldCacheTest(ROOT_URL + "content/321/bmj.f4270.full.pdf", true, au);
    shouldCacheTest(ROOT_URL + "content/321/bmj.f4270.full.pdf+html", true, au);
    // only for data supplements
    shouldCacheTest(ROOT_URL + "content/347/bmj.f4547/related", true, au);
    
// XXX   shouldCacheTest(ROOT_URL + "content/suppl/2014/05/16/bmj.f6123.DC1", false, au);
    shouldCacheTest(ROOT_URL + "highwire/markup/185154/expansion", false, au);
    
    shouldCacheTest(ROOT_URL + "sites/default/themes/bmj/the_bmj/css/fonts.css", false, au);
    shouldCacheTest(ROOT_URL + "sites/default/files/cdn/css/http/css_nZpqy9LysFSwGKJ7v4z11U9YB9kpVSWL_JhlIW3O5FI.css", false, au);
    
    // should not get crawled - LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, au);
    
    shouldCacheTest(ROOT_URL + "bmj/section-pdf/724572/0", false, au);
    shouldCacheTest(ROOT_URL + "content/347/bmj.f4547/article-info", false, au);
    shouldCacheTest(ROOT_URL + "content/321/bmj.f4270/peer-review", false, au);
    shouldCacheTest(ROOT_URL + "content/347/bmj.f4547/rapid-responses", false, au);
    shouldCacheTest(ROOT_URL + "content/347/bmj.f4547/submit-a-rapid-response", false, au);
    shouldCacheTest(ROOT_URL + "highwire/powerpoint/185149", false, au);
    shouldCacheTest(ROOT_URL + "lookup/doi/10.1136/bmj.6444", false, au);
    shouldCacheTest(ROOT_URL + "node/207663", false, au);
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/bmj_rapid_responses_form/node:185147/1", false, au);
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/jnl_bmj_tab_art/node:725123/1", false, au);
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/jnl_bmj_tab_info/node:728958/1", false, au);
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/jnl_bmj_tab_related_art/node:735423/1", false, au);
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/jnl_bmj_tab_peer_review/node:735423/1", false, au);
    shouldCacheTest(ROOT_URL + "", false, au);
  }
  
  // Test the crawl rules for HW Drupal
  public void testShouldCacheProperPagesParent() throws Exception {
    String ROOT_URL = "http://highwire.org/";
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, ROOT_URL);
    props.setProperty(VOL_KEY, "2015");
    DefinableArchivalUnit au = null;
    try {
      au = makeAuFromProps(props);
    }
    catch (ConfigurationException ex) {
    }
    theDaemon.getLockssRepository(au);
    
    // Test for pages that should get crawled or not
    // permission page/start url
    shouldCacheTest(ROOT_URL + "lockss-manifest/vol_2015_manifest.html", true, au);
    // new inclusive crawl_rule
    shouldCacheTest(ROOT_URL + "clockss-manifest/vol_2015_manifest.html", true, au);
    shouldCacheTest(ROOT_URL + "manifest/year=2015", false, au);
    // toc page for a volume only
    shouldCacheTest(ROOT_URL + "content/2015", true, au);
    shouldCacheTest(ROOT_URL + "content/2015.toc", true, au);
    // toc page for a volume, issue
    shouldCacheTest(ROOT_URL + "content/2015/1", true, au);
    shouldCacheTest(ROOT_URL + "content/2015/2.toc", true, au);
    shouldCacheTest(ROOT_URL.replace("http:", "https:") + "content/2015/2.toc", true, au);
    // article files
    shouldCacheTest(ROOT_URL + "content/2015/1/2", true, au);
    shouldCacheTest(ROOT_URL.replace("http:", "https:") + "content/2015/1/2", true, au);
    shouldCacheTest(ROOT_URL + "content/2015/1/2.abstract", false, au);
    shouldCacheTest(ROOT_URL + "content/2015/1/2.long", true, au);
    shouldCacheTest(ROOT_URL + "content/2015/1/2.extract", false, au);
    shouldCacheTest(ROOT_URL + "content/2015/1/2.full", false, au);
    shouldCacheTest(ROOT_URL + "content/2015/1/2.full.pdf", true, au);
    shouldCacheTest(ROOT_URL + "content/2015/1/2.full.pdf+html", true, au);
    shouldCacheTest(ROOT_URL + "content/2015/1/2.full-text.pdf+html", true, au);
    shouldCacheTest(ROOT_URL + "content/2015/1/DC1", true, au);
    shouldCacheTest(ROOT_URL + "content/1/1/2005.0001", true, au);
    shouldCacheTest(ROOT_URL + "content/1/1/2005.0001.full.pdf", true, au);
    shouldCacheTest(ROOT_URL + "content/1/1/ivx.1", true, au);
    shouldCacheTest(ROOT_URL + "content/1/1/ivx.1.full.pdf", true, au);
    shouldCacheTest(ROOT_URL + "content/1/1/e123.2", true, au);
    shouldCacheTest(ROOT_URL + "content/1/1/e123.2.long", true, au);
    shouldCacheTest(ROOT_URL + "content/1/1/e123.2.data", true, au);
    shouldCacheTest(ROOT_URL + "content/1/1/e123.2.full", false, au);
    shouldCacheTest(ROOT_URL + "content/1/1/e123.2.full.txt", false, au);
//    shouldCacheTest(ROOT_URL + "content/1/1/e123.2.full.pdf", true, au);
//    shouldCacheTest(ROOT_URL + "content/1/1/e123.2.full.pdf+html", true, au);
//    shouldCacheTest(ROOT_URL + "content/1/1/e123.2.full-text.pdf+html", true, au);
//    shouldCacheTest(ROOT_URL + "content/1/Supplement_2/1234S2.1.full.pdf", true, au);
    
    shouldCacheTest(ROOT_URL + "content/os-86/1_suppl_2/2.abstract", false, au);
    shouldCacheTest(ROOT_URL + "content/os-86/1_suppl_2/2.long", true, au);
    shouldCacheTest(ROOT_URL + "content/os-86/1_suppl_2/2.extract", false, au);
    shouldCacheTest(ROOT_URL + "content/os-86/1_suppl_2/2.full", false, au);
    shouldCacheTest(ROOT_URL + "content/os-86/1_suppl_2/2.full.pdf", true, au);
    shouldCacheTest(ROOT_URL + "content/os-86/1_suppl_2/2.full.pdf+html", true, au);
    shouldCacheTest(ROOT_URL + "content/os-86/1_suppl_2/2.full-text.pdf+html", true, au);
    
    // full pdf article files with journal id
    shouldCacheTest(ROOT_URL + "content/jid/2015/1/2.full", false, au);
    shouldCacheTest(ROOT_URL + "content/jid/2015/1/2.full.pdf", true, au);
    shouldCacheTest(ROOT_URL + "content/jid/2015/1/2.full.pdf+html", true, au);
    shouldCacheTest(ROOT_URL + "content/jid/2015/1/2.full-text.pdf+html", true, au);
    shouldCacheTest(ROOT_URL + "content/jid/2015/1/DC1", false, au);
    shouldCacheTest(ROOT_URL + "content/jid/1/1/2005.0001", false, au);
    shouldCacheTest(ROOT_URL + "content/jid/1/1/2005.0001.full.pdf", true, au);
    shouldCacheTest(ROOT_URL + "content/jid/1/1/ivx.1", false, au);
    shouldCacheTest(ROOT_URL + "content/jid/1/1/ivx.1.full.pdf", true, au);
    
    shouldCacheTest(ROOT_URL + "content/2015/1/2.print", false, au);
    shouldCacheTest(ROOT_URL + "content/2015/1/2.explore", false, au);
    shouldCacheTest(ROOT_URL + "content/2015/1/2/article-info", false, au);
    shouldCacheTest(ROOT_URL + "content/2015/1/2/submit?param=12", false, au);
    
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/hw_tab_data/node:80746/1", false, au);
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/hw_tab_art/node:80746/1", false, au);
    
    shouldCacheTest(ROOT_URL + "highwire/citation/12/ris", false, au);
    shouldCacheTest(ROOT_URL + "highwire/citation/9/1/ris", false, au);
    shouldCacheTest(ROOT_URL + "highwire/markup/113/expansion", false, au);
    
    shouldCacheTest(ROOT_URL + "sites/all/libraries/modernizr/modernizr.min.js", false, au);
    shouldCacheTest(ROOT_URL + "sites/default/files/js/js_0j8_f76rvZ212f4rg.js", false, au);
    shouldCacheTest(ROOT_URL + "sites/default/themes/hw/font/fontawesome-webfont.eot", false, au);
    shouldCacheTest(ROOT_URL + "sites/default/themes/font/fontawesome-webfont.eot", false, au);
    
    shouldCacheTest(ROOT_URL + "content/hw/suppl/2014/04/23/hw.02130.DC1/hw02130_Supplemental_files.zip", true, au);
    shouldCacheTest(ROOT_URL.replace("http:", "https:") + "content/hw/suppl/2014/04/23/hw.02130.DC1/hw02130_Supplemental_files.zip", true, au);
    shouldCacheTest("http://cdn.cloudfront.net/content/2015/1/3/F1.medium.gif", false, au);
    shouldCacheTest("https://cdn.cloudfront.net/content/2015/1/3/F1.medium.gif", false, au);
    shouldCacheTest("http://cdn.cloudfront.net/content/2015/1/3/F1.medium.gif?width=400", false, au);
    shouldCacheTest("http://cdn.mathjax.org/mathjax/latest/MathJax.js", false, au);
    shouldCacheTest("https://ajax.googleapis.com/ajax/libs/jquery/1.8.2/jquery.min.js", false, au);
    shouldCacheTest("", false, au);
    shouldCacheTest(ROOT_URL + "content/by/year", false, au);
    shouldCacheTest(ROOT_URL + "content/current", false, au);
    
    shouldCacheTest(ROOT_URL + "content/213/11/2293/tab-figures-data", true, au);
    shouldCacheTest(ROOT_URL + "content/213/11/2293/tab-article-info", false, au);
    shouldCacheTest(ROOT_URL + "content/213/11/2293/tab-metrics", false, au);
    shouldCacheTest("http://dzfiakl78wcmk.cloudfront.net/sites/all/modules/contrib/panels_ajax_tab/images/loading.gif", false, au);
    shouldCacheTest(ROOT_URL + "highwire/filestream/124406/field_highwire_adjunct_files/2/JEM_20160800_sm.pdf", true, au);
    shouldCacheTest(ROOT_URL + "highwire/filestream/124406/field_highwire_adjunct_files/0/JEM_20160800_V1.mp4", true, au);
    shouldCacheTest("http://static-movie-usa.glencoesoftware.com/jpg/10.1084/255/99e5615849629455e656275ea23db2f09a4b4e9f/JEM_20160800_V1.jpg", true, au);
    shouldCacheTest("http://movie.rupress.org/video/10.1084/jem.20160800/video-1", false, au);
    shouldCacheTest("http://static-movie-usa.glencoesoftware.com/source/10.1084/255/99e5615849629455e656275ea23db2f09a4b4e9f/JEM_20160800_V1.mp4", true, au);
    
    // should not get crawled - LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, au);
    
  }
  
  private void shouldCacheTest(String url, boolean shouldCache, ArchivalUnit au) {
    log.info ("shouldCacheTest url: " + url);
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
}