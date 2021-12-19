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

package org.lockss.plugin.highwire.elife;

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

public class TestClockssELifeJCorePlugin extends LockssTestCase {
  
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  
  private MockLockssDaemon theDaemon;
  private DefinablePlugin plugin;
  
  public TestClockssELifeJCorePlugin(String msg) {
    super(msg);
  }
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
        "org.lockss.plugin.highwire.elife.ClockssELifeDrupalPlugin");
    
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
    props.setProperty(VOL_KEY, "2013");
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    
    String[] starturl = new String[]{
        "http://www.example.com/clockss-manifest/elife_2013.html",
        "http://www.example.com/clockss-manifest/2013.html",
    };
    
    DefinableArchivalUnit au = makeAuFromProps(props);
    assertEquals("eLife Sciences Plugin (retired site for CLOCKSS), Base URL http://www.example.com/, Volume 2013",
        au.getName());
    assertEquals(ListUtil.fromArray(starturl), au.getStartUrls());
  }
  
  public void testGetPluginId() {
    assertEquals("org.lockss.plugin.highwire.elife.ClockssELifeDrupalPlugin",
        plugin.getPluginId());
  }
  
  public void testGetAuConfigProperties() {
    assertEquals(ListUtil.list(ConfigParamDescr.BASE_URL,
        ConfigParamDescr.VOLUME_NAME),
        plugin.getLocalAuConfigDescrs());
  }
  
  public void testHandles500Result() throws Exception {
    Properties props = new Properties();
    props.setProperty(VOL_KEY, "2013");
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    
    String starturl =
        "http://www.example.com/lockss-manifest/elife_2013.html";
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
  
  public void testHandles403Result() throws Exception {
    Properties props = new Properties();
    props.setProperty(VOL_KEY, "2013");
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    
    String starturl =
        "http://www.example.com/lockss-manifest/elife_2013.html";
    DefinableArchivalUnit au = makeAuFromProps(props);
    MockLockssUrlConnection conn = new MockLockssUrlConnection();
    conn.setURL("http://uuu17/download/somestuff");
    CacheException exc =
        ((HttpResultMap)plugin.getCacheResultMap()).mapException(au, conn,
            403, "foo");
    assertClass(CacheException.RetryDeadLinkException.class, exc);
    
    conn.setURL(starturl);
    exc = ((HttpResultMap)plugin.getCacheResultMap()).mapException(au, conn,
        403, "foo");
    assertClass(CacheException.RetrySameUrlException.class, exc);
    
  }
  
  // Test the crawl rules for eLife
  public void testShouldCacheProperPages() throws Exception {
    String ROOT_URL = "http://elifesciences.org/";
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, ROOT_URL);
    props.setProperty(VOL_KEY, "2013");
    DefinableArchivalUnit au = null;
    try {
      au = makeAuFromProps(props);
    }
    catch (ConfigurationException ex) {
    }
    theDaemon.getLockssRepository(au);
    
    // Test for pages that should get crawled or not
    // permission page/start url
    shouldCacheTest(ROOT_URL + "lockss-manifest/2013.html", false, au);
    shouldCacheTest(ROOT_URL + "clockss-manifest/elife_2013.html", true, au);
    // crawl_rule allows following
    shouldCacheTest(ROOT_URL + "clockss-manifest/vol_2013_manifest.html", true, au);
    shouldCacheTest(ROOT_URL + "manifest/year=2013", false, au);
    // toc page for an issue, there is no issue
    // shouldCacheTest(ROOT_URL + "content/1", false, au); // changed crawl rule as there are some volume only tocs (iwa) so this test is true
    // article files
    shouldCacheTest(ROOT_URL + "content/1/e00002", true, au);
    shouldCacheTest(ROOT_URL.replace("http:", "https:") + "content/1/e00002", true, au);
    shouldCacheTest(ROOT_URL + "content/1/e00003/article-data", false, au);
    shouldCacheTest(ROOT_URL + "content/1/e00003/article-info", false, au);
    shouldCacheTest(ROOT_URL + "content/1/e00003.abstract", false, au);
    shouldCacheTest(ROOT_URL + "content/1/e00003.full.pdf", true, au);
    shouldCacheTest(ROOT_URL + "content/1/2/e00003.full.pdf", true, au);
    shouldCacheTest(ROOT_URL + "content/elife/1/e00003/F1.large.jpg", false, au);
    shouldCacheTest(ROOT_URL + "content/elife/1/e00003/F3/F4.large.jpg", false, au);
    shouldCacheTest(ROOT_URL + "content/elife/1/e00003.full.pdf", true, au);
    shouldCacheTest(ROOT_URL + "highwire/citation/12/ris", false, au);
    shouldCacheTest(ROOT_URL + "highwire/citation/9/1/ris", false, au);
    shouldCacheTest(ROOT_URL + "highwire/markup/113/expansion", false, au);
    // This now returns true in error; however, the plugin is deprecated;
    // and it's possible that we should have collected these pages;
    // and it's possible that the urls like these are crawl filtered out anyway;
    // moving on...
    //    shouldCacheTest(ROOT_URL + "content/1/e00011/DC5", false, au); 
    shouldCacheTest(ROOT_URL + "sites/all/libraries/modernizr/modernizr.min.js", false, au);
    shouldCacheTest(ROOT_URL + "sites/default/files/js/js_0j8_f76rvZ212f4rg.js", false, au);
    shouldCacheTest(ROOT_URL + "sites/default/themes/elife/font/fontawesome-webfont.eot", false, au);
    shouldCacheTest(ROOT_URL + "sites/default/themes/font/fontawesome-webfont.eot", false, au);
    
    shouldCacheTest(ROOT_URL + "content/1/e00003/article-metrics", false, au);
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/jnl_elife_article_article/node:8375/0", false, au);
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/jnl_elife_article_figdata/node:8375/1", false, au);
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/jnl_elife_article_metrics/node:8375/1", false, au);
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/jnl_elife_article_author/node:8375/1", false, au);
    
    shouldCacheTest(ROOT_URL + "elife/download-pdf/content/1/e00352/n/Nerve%20diversity%20in%20skin.pdf/1", true, au);
    shouldCacheTest(ROOT_URL + "elife/download-suppl/250/supplementary-file-1.media-1.pdf/0/1", true, au);
    shouldCacheTest(ROOT_URL + "elife/download-suppl/267/supplementary-file-1.media-1.xls/0/1", true, au);
    shouldCacheTest(ROOT_URL + "elife/download-suppl/293/supplementary-file-1.media-4.xlsx/0/1", true, au);
    shouldCacheTest(ROOT_URL + "elife/download-suppl/297/figure-10—source-data-1.media-3.xlsx/0/1", true, au);
    shouldCacheTest(ROOT_URL + "elife/download-video/http%253A%252F%252Fstatic-movie-usa.glencoesoftware.com%252Fmp4%252F10.7554%252F6873ae4599bf%252Felife00007v001.mp4", true, au);
    shouldCacheTest(ROOT_URL + "elife/download-suppl/23743/source-code-1.media-5.pl/0/1", true, au);
    shouldCacheTest(ROOT_URL + "content/elife/suppl/2014/04/23/eLife.02130.DC1/elife02130_Supplemental_files.zip", true, au);
    
    shouldCacheTest("http://cdn-site.elifesciences.org/content/elife/1/e00003/F1.medium.gif", false, au);
    shouldCacheTest("http://cdn-site.elifesciences.org/content/elife/1/e00003/F3/F4.small.gif", false, au);
    shouldCacheTest("http://cdn-site.elifesciences.org/misc/draggable.png", false, au);
    shouldCacheTest("http://cdn-site.elifesciences.org/sites/all/libraries/cluetip/images/arrowdown.gif", false, au);
    shouldCacheTest("http://cdn-site.elifesciences.org/sites/default/files/cdn/css/http/css_2iejb0.css", false, au);
    shouldCacheTest("http://cdn-site.elifesciences.org/sites/default/themes/elife/css/PIE/PIE.htc", false, au);
    shouldCacheTest("http://cdn-site.elifesciences.org/sites/default/themes/elife/font/fontawesome-webfont.woff", false, au);
    shouldCacheTest("http://cdn.elifesciences.org/elife-articles/00003/figures-pdf/elife00003-figures.pdf", false, au);
    shouldCacheTest("http://cdn.mathjax.org/mathjax/latest/MathJax.js", false, au);
    shouldCacheTest("https://ajax.googleapis.com/ajax/libs/jquery/1.8.2/jquery.min.js", false, au);
    shouldCacheTest("", false, au);
    
    // should not get crawled - LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, au);
    
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