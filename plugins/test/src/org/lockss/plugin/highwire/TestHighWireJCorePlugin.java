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

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.definable.*;
import org.lockss.test.*;
import org.lockss.util.ListUtil;
import org.lockss.util.RegexpUtil;
import org.lockss.util.urlconn.CacheException;
import org.lockss.util.urlconn.HttpResultMap;

public class TestHighWireJCorePlugin extends LockssTestCase {
  
  private static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  private static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  
  private static final String HW_BASE_URL = "http://ajp.highwire.org/";
  
  /**
   * Configuration method. 
   * @return
   */
  Configuration auConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", HW_BASE_URL);
    return conf;
  }
  
  private MockLockssDaemon theDaemon;
  private DefinablePlugin plugin;
  
  public TestHighWireJCorePlugin(String msg) {
    super(msg);
  }
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
        "org.lockss.plugin.highwire.HighWireJCorePlugin");
    
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
        "http://www.example.com/lockss-manifest/vol_303_manifest.html" 
    };
    DefinableArchivalUnit au = makeAuFromProps(props);
    assertEquals("Parent HighWire Press Journals Plugin (JCore), Base URL http://www.example.com/, Volume 303",
        au.getName());
    assertEquals(ListUtil.list(starturl[0]), au.getStartUrls());
  }
  
  public void testGetPluginId() {
    assertEquals("org.lockss.plugin.highwire.HighWireJCorePlugin",
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
    
    conn.setURL(starturl);
    exc = ((HttpResultMap)plugin.getCacheResultMap()).mapException(au, conn, 500, "foo");
    assertClass(CacheException.RetrySameUrlException.class, exc);
    
  }
  
  // Test the crawl rules for HW Drupal
  public void testShouldCacheProperPages0() throws Exception {
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
    shouldCacheTest(ROOT_URL + "clockss-manifest/vol_1_manifest.html", false, au);
    shouldCacheTest(ROOT_URL + "lockss-manifest/vol_os-86_manifest.html", false, au);
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
    shouldCacheTest(ROOT_URL + "content/2015/1.long", true, au);
    
    // article files with journal id
    shouldCacheTest(ROOT_URL + "content/jid/2015/1/2.abstract", false, au);
    shouldCacheTest(ROOT_URL + "content/jid/2015/1/2.full", false, au);
    shouldCacheTest(ROOT_URL + "content/jid/2015/1/2.full.pdf", true, au);
    shouldCacheTest(ROOT_URL + "content/jid/2015/1/2.full.pdf+html", true, au);
    shouldCacheTest(ROOT_URL + "content/jid/2015/1/2.full-text.pdf+html", true, au);
    shouldCacheTest(ROOT_URL + "content/jid/2015/1/DC1", false, au);
    shouldCacheTest(ROOT_URL + "content/jid/2015/1.long", true, au);
    
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
    
    // other pages
    shouldCacheTest(ROOT_URL + "content/2015/11/2293.figures-only", true, au);
    shouldCacheTest(ROOT_URL + "content/2015/11/2293/tab-figures-only", true, au);
    shouldCacheTest(ROOT_URL + "content/2015/11/2293/tab-figures-data", true, au);
    shouldCacheTest(ROOT_URL + "content/2015/11/2293/tab-article-info", false, au);
    shouldCacheTest(ROOT_URL + "content/2015/11/2293/tab-metrics", false, au);
    shouldCacheTest("http://dzfiakl78wcmk.cloudfront.net/sites/all/modules/contrib/panels_ajax_tab/images/loading.gif", false, au);
    shouldCacheTest(ROOT_URL + "highwire/filestream/124406/field_highwire_adjunct_files/2/JEM_20160800_sm.pdf", true, au);
    shouldCacheTest(ROOT_URL + "highwire/filestream/124406/field_highwire_adjunct_files/0/JEM_20160800_V1.mp4", true, au);
    shouldCacheTest("http://static-movie-usa.glencoesoftware.com/jpg/10.1084/255/99e5615849629455e656275ea23db2f09a4b4e9f/JEM_20160800_V1.jpg", true, au);
    shouldCacheTest("http://movie.rupress.org/video/10.1084/jem.20160800/video-1", false, au);
    shouldCacheTest("http://static-movie-usa.glencoesoftware.com/source/10.1084/255/99e5615849629455e656275ea23db2f09a4b4e9f/JEM_20160800_V1.mp4", true, au);
    
    // search links on TOC page for meeting abstracts
    shouldCacheTest(ROOT_URL + "search/volume%3A2015%20issue%3A1%2BSupplement?facet%5Btoc-section-id%5D%5B0%5D=Cell%20Division", true, au);
    shouldCacheTest(ROOT_URL + "search/volume%3A2015%20issue%3A1%2BSupplement?page=1&facet%5Btoc-section-id%5D%5B0%5D=Cell%20Division", true, au);
    
    // should not get crawled - LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, au);
    
  }
  
  // Test the crawl rules for HW Drupal
  public void testShouldCacheProperPages1() throws Exception {
    String ROOT_URL = "http://highwire.org/";
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, ROOT_URL);
    props.setProperty(VOL_KEY, "1");
    DefinableArchivalUnit au = null;
    try {
      au = makeAuFromProps(props);
    }
    catch (ConfigurationException ex) {
    }
    theDaemon.getLockssRepository(au);
    
    // Test for pages that should get crawled or not
    // permission page/start url
    shouldCacheTest(ROOT_URL + "lockss-manifest/vol_1_manifest.html", true, au);
    // new inclusive crawl_rule
    shouldCacheTest(ROOT_URL + "clockss-manifest/vol_1_manifest.html", true, au);
    // toc page for a volume only
    shouldCacheTest(ROOT_URL + "content/1", true, au);
    shouldCacheTest(ROOT_URL + "content/1.toc", true, au);
    // toc page for a volume, issue
    shouldCacheTest(ROOT_URL + "content/1/1", true, au);
    shouldCacheTest(ROOT_URL + "content/1/2.toc", true, au);
    shouldCacheTest(ROOT_URL.replace("http:", "https:") + "content/1/2.toc", true, au);
    // article files
    shouldCacheTest(ROOT_URL + "content/1/1/2005.0001", true, au);
    shouldCacheTest(ROOT_URL + "content/1/1/2005.0001.full.pdf", true, au);
    shouldCacheTest(ROOT_URL + "content/1/1/ivx.1", true, au);
    shouldCacheTest(ROOT_URL + "content/1/1/ivx.1.full.pdf", true, au);
    shouldCacheTest(ROOT_URL + "content/1/1/ivx.article-info", false, au);
    shouldCacheTest(ROOT_URL + "content/1/1/e123.2", true, au);
    shouldCacheTest(ROOT_URL + "content/1/1/e123.2.long", true, au);
    shouldCacheTest(ROOT_URL + "content/1/1/e123.2.data", true, au);
    shouldCacheTest(ROOT_URL + "content/1/1/e123.2.full", false, au);
    shouldCacheTest(ROOT_URL + "content/1/1/e123.2.full.txt", false, au);
    shouldCacheTest(ROOT_URL + "content/1/1/e123.2.full.pdf", true, au);
    shouldCacheTest(ROOT_URL + "content/1/1/e123.2.full.pdf+html", true, au);
    shouldCacheTest(ROOT_URL + "content/1/1/e123.2.full-text.pdf+html", true, au);
    shouldCacheTest(ROOT_URL + "content/1/Supplement_2/1234S2.1.full.pdf", true, au);
    
    // full pdf article files with journal id
    shouldCacheTest(ROOT_URL + "content/jid/1/1/2005.0001", false, au);
    shouldCacheTest(ROOT_URL + "content/jid/1/1/2005.0001.full.pdf", true, au);
    shouldCacheTest(ROOT_URL + "content/jid/1/1/ivx.1", false, au);
    shouldCacheTest(ROOT_URL + "content/jid/1/1/ivx.1.full.pdf", true, au);
    
    shouldCacheTest(ROOT_URL + "content/2013/1/X3", false, au);
    shouldCacheTest(ROOT_URL + "content/2013a/1/3", true, au);
    
    // http://imaging.onlinejacc.org/content/jimg/6/11/1129/DC1/embed/media-1.docx
    shouldCacheTest(ROOT_URL + "content/jid/1/1/1/DC1/embed/doc-1.docx", true, au);
    shouldCacheTest(ROOT_URL + "content/jid/1/1/1/DC1/embed/doc-1.pdf", true, au);
    shouldCacheTest(ROOT_URL + "content/jid/1/1/DC1/embed/doc-1.docx", true, au);
    shouldCacheTest(ROOT_URL + "content/1/1/1/DC1/embed/doc-1.pdf", true, au);
    shouldCacheTest(ROOT_URL + "content/1/1/DC1/embed/doc-1.docx", true, au);
    
  }
  
  // Test the crawl rules for HW Drupal
  public void testShouldCacheProperPages2() throws Exception {
    String ROOT_URL = "http://highwire.org/";
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, ROOT_URL);
    props.setProperty(VOL_KEY, "os-86");
    DefinableArchivalUnit au = null;
    try {
      au = makeAuFromProps(props);
    }
    catch (ConfigurationException ex) {
    }
    theDaemon.getLockssRepository(au);
    
    // Test for pages that should get crawled or not
    // permission page/start url
    shouldCacheTest(ROOT_URL + "lockss-manifest/vol_os-86_manifest.html", true, au);
    // new inclusive crawl_rule
    shouldCacheTest(ROOT_URL + "clockss-manifest/vol_os-86_manifest.html", true, au);
    // toc page for a volume only
    shouldCacheTest(ROOT_URL + "content/os-86", true, au);
    shouldCacheTest(ROOT_URL + "content/os-86.toc", true, au);
    // toc page for a volume, issue
    shouldCacheTest(ROOT_URL + "content/os-86/1", true, au);
    shouldCacheTest(ROOT_URL + "content/os-86/2.toc", true, au);
    shouldCacheTest(ROOT_URL.replace("http:", "https:") + "content/os-86/2.toc", true, au);
    // article files
    shouldCacheTest(ROOT_URL + "content/os-86/1_suppl_2/2.abstract", false, au);
    shouldCacheTest(ROOT_URL + "content/os-86/1_suppl_2/2.long", true, au);
    shouldCacheTest(ROOT_URL + "content/os-86/1_suppl_2/2.extract", false, au);
    shouldCacheTest(ROOT_URL + "content/os-86/1_suppl_2/2.full", false, au);
    shouldCacheTest(ROOT_URL + "content/os-86/1_suppl_2/2.full.pdf", true, au);
    shouldCacheTest(ROOT_URL + "content/os-86/1_suppl_2/2.full.pdf+html", true, au);
    shouldCacheTest(ROOT_URL + "content/os-86/1_suppl_2/2.full-text.pdf+html", true, au);
    
  }
  
  private void shouldCacheTest(String url, boolean shouldCache, ArchivalUnit au) {
    log.info ("shouldCacheTest url: " + url);
    assertEquals(shouldCache, au.shouldBeCached(url));
  }

  public void testShouldCrawlSubDomain() throws Exception {

    String BASE_URL = "http://dev.biologists.org";
    String VOLUME_NUMBER = "145";

    String SAMPLE_VALID_VIDEO_URL = "http://movie.biologists.com/video/10.1242/dev.135319/video-1";

    log.debug3("shouldCrawlSubDomain url: " + SAMPLE_VALID_VIDEO_URL);

    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, BASE_URL);
    props.setProperty(VOL_KEY, VOLUME_NUMBER);

    DefinableArchivalUnit au = null;
    try {
      au = makeAuFromProps(props);
    }
    catch (ConfigurationException ex) {
    }
    theDaemon.getLockssRepository(au);

    //Real sample video url
    shouldCacheTest(SAMPLE_VALID_VIDEO_URL, true, au);
    shouldCacheTest("http://www.example.com/video/video-1", false, au);
    shouldCacheTest("http://movie.biologists.com/fakevideo/video-1", false, au);
    shouldCacheTest("http://fakemovie.biologists.com/videos/video-1", false, au);
  }

  // from au_exclude_urls_from_polls_pattern in plugins/src/org/lockss/plugin/highwire/HighWireJCorePlugin.xml
  // if it changes in the plugin, you will likely need to change the test, so verify
  //    ^http(?!.*/highwire/filestream/.*)(?!.*\.pdf)(?!.*/content/[^/]+/suppl/.*)|html$
  
  static final String HW_EXCLUDE_FROM_POLLS_REGEXP[] = 
    {
        "^https?(?!.*/highwire/filestream/.*)(?!.*\\.pdf)(?!.*/content/[^/]+/suppl/.*)|html$",
        "^https?(.+)/twi[ls]\\.",
        "^https?(.+)/findings\\.",
    };

  public void testPollSpecial() throws Exception {
    String ROOT_URL = "http://www.example.com/";
    Properties props = new Properties();
    props.setProperty(VOL_KEY, "322");
    props.setProperty(BASE_URL_KEY, ROOT_URL);
    DefinableArchivalUnit au = makeAuFromProps(props);
    
    // if it changes in the plugin, you will likely need to change the test, so verify
    assertEquals(Arrays.asList(
        HW_EXCLUDE_FROM_POLLS_REGEXP),
        RegexpUtil.regexpCollection(au.makeExcludeUrlsFromPollsPatterns()));
    
    // make sure that's the regexp that will match to the expected url string
    // this also tests the regexp (which is the same) for the exclude from poll map
    // Add to pattern these urls? Has not been seen as problem, yet
    
    List <String> excludeList = ListUtil.list(
        ROOT_URL + "clockss-manifest/vol_114_manifest.html",
        ROOT_URL + "content/114/1",
        ROOT_URL + "content/114/1/1",
        ROOT_URL + "content/114/1/1.e-letters",

        ROOT_URL + "content/114/1/1.full.pdf+html",
        ROOT_URL.replace("http:", "https:") + "content/114/1/107",
        ROOT_URL + "content/1/1/2005.0001",
        ROOT_URL + "content/114/1.toc",
        ROOT_URL + "highwire/article_citation_preview/37911",
        ROOT_URL + "highwire/markup/97175/expansion",
        ROOT_URL + "misc/ajax.js",
        ROOT_URL + "panels_ajax_tab/jnl_foo_tab_data/node:37911/1",
        ROOT_URL + "panels_ajax_tab/jnl_foo_tab_info/node:38276/1",
        ROOT_URL + "panels_ajax_tab/jnl_foo_tab_pdf/node:37898/1",
        ROOT_URL + "sites/all/modules/highwire/highwire/js/highwire_toggle_download_pdf.js",
        ROOT_URL + "sites/all/modules/highwire/highwire/plugins/content_types/js/highwire_article_comments.js",
        ROOT_URL + "sites/all/modules/highwire/highwire/plugins/highwire_markup_process/js/highwire_at_symbol.js",
        ROOT_URL + "sites/all/modules/highwire/highwire/plugins/highwire_markup_process/js/highwire_openurl.js",
        ROOT_URL + "css/9.2.6/print.css",
        ROOT_URL + "js/9.3.2/lib/functional.min.js",
        ROOT_URL + "css/8.10.4/custom-theme/images/ui-bg_flat_100_006eb2_40x100.png",
        "http://cdn.mathjax.org/mathjax/latest/MathJax.js",
        "http://egbdf.cloudfront.net/content/foo/114/1/107/F1.large.jpg",
        "http://egbdf.cloudfront.net/content/foo/114/1/107/F1.medium.gif",
        "http://egbdf.cloudfront.net/misc/draggable.png",
        "http://egbdf.cloudfront.net/misc/progress.gif",
        "http://egbdf.cloudfront.net/misc/ui/images/ui-bg_glass_75_e6e6e6_1x400.png",
        "http://egbdf.cloudfront.net/sites/all/libraries/chosen/chosen-sprite.png",
        "http://egbdf.cloudfront.net/sites/all/libraries/modernizr/modernizr.min.js",
        "http://egbdf.cloudfront.net/sites/all/modules/contrib/colorbox/styles/default/images/loading_animation.gif",
        "http://egbdf.cloudfront.net/sites/all/modules/contrib/ctools/images/status-active.gif",
        "http://egbdf.cloudfront.net/sites/all/modules/contrib/nice_menus/images/arrow-right.png",
        "http://egbdf.cloudfront.net/sites/all/modules/contrib/panels_ajax_tab/images/loading.gif",
        "http://egbdf.cloudfront.net/sites/all/modules/highwire/highwire/highwire.style.highwire.css",
        "http://egbdf.cloudfront.net/sites/all/modules/highwire/highwire/highwire_theme_tools/fonts/hwicons.eot",
        "http://egbdf.cloudfront.net/sites/all/modules/highwire/highwire/highwire_theme_tools/fonts/hwicons.svg",
        "http://egbdf.cloudfront.net/sites/all/modules/highwire/highwire/highwire_theme_tools/fonts/hwicons.ttf",
        "http://egbdf.cloudfront.net/sites/all/modules/highwire/highwire/highwire_theme_tools/fonts/hwicons.woff",
        "http://egbdf.cloudfront.net/sites/all/themes/contrib/omega/omega/images/button.png",
        "http://egbdf.cloudfront.net/sites/default/files/advagg_css/css__wDWgsVUy1vHKJk5IOxMeWaglKzLoOtZILn17SMUIKNc__JeEmC_ds8zYAzXrROHaKwkPeIUtroNPLownDI-CNYj4__ZBV1Na1y5UYxD8pZQl12ZuMt0MUtDuHKkc3Rs78lmMI.css",
        "http://egbdf.cloudfront.net/sites/default/files/advagg_js/js__1PtAOZ8x7l6QAi8D7X6Kj_Y7HcUOFzvWRvvYHUcCT98__7itiXlZly3E1jShBthrww-DLF4Hn0cFXb_K4ha96l3k__ZBV1Na1y5UYxD8pZQl12ZuMt0MUtDuHKkc3Rs78lmMI.js",
        "http://egbdf.cloudfront.net/sites/default/files/cdn/css/http/css_-hbrzZIeBluvkRZKunL2_uBR1NG16FPt2ZxP2e8Ukss.css",
        "http://egbdf.cloudfront.net/sites/default/files/cdn/css/http/css_C66gTr00v8C78pnyy16Gmr-Q8G3lCnOnByxK8F8i-nc_highwire.style.highwire.css.css",
        "http://egbdf.cloudfront.net/sites/default/files/favicon_1.ico",
        "http://egbdf.cloudfront.net/sites/default/files/highwire/foo/114/10/1451/F2/embed/mml-math-14.gif",
        "http://egbdf.cloudfront.net/sites/default/files/js/js_-dUp1-d3TYhpxjvks0wAgzk3N1TNQRUoPuOh6OaYUig.js"
        );
    
    Pattern p0 = Pattern.compile(HW_EXCLUDE_FROM_POLLS_REGEXP[0]);
    Matcher m0;
    for (String urlString : excludeList) {
      m0 = p0.matcher(urlString);
      assertEquals(urlString, true, m0.find());
    }
    
    List <String> includeList = ListUtil.list(
        ROOT_URL + "content/114/1/1.full.pdf",
        ROOT_URL + "content/foo/114/1/1.full.pdf",
        ROOT_URL + "content/1/1/2005.0001.full.pdf",
        ROOT_URL + "content/foo/suppl/2013/01/28/fooplphysiol.01341.2012.DC1/Supplement_Table.pdf",
        ROOT_URL + "content/foo/suppl/2013/01/28/fooplphysiol.01430.2011.DC1/tableS1.pdf",
        ROOT_URL + "content/foo/suppl/2013/02/07/fooplphysiol.00747.2012.DC1/matlab.docx",
        ROOT_URL + "content/foo/suppl/2013/03/21/fooplphysiol.00106.2013.DC1/tableS1.doc",
        ROOT_URL + "highwire/filestream/108240/field_highwire_adjunct_files/0/Supplement_Table.pdf",
        ROOT_URL + "highwire/filestream/108244/field_highwire_adjunct_files/0/tableS1.pdf",
        ROOT_URL.replace("http:", "https:") + "highwire/filestream/108250/field_highwire_adjunct_files/0/matlab.docx",
        ROOT_URL + "highwire/filestream/108260/field_highwire_adjunct_files/0/tableS1.doc"
        );
    
    for (String urlString : includeList) {
      m0 = p0.matcher(urlString);
      assertEquals(urlString, false, m0.find());
    }
  }
  

  public void testCheckSubstanceRules() throws Exception {
    String ROOT_URL = "http://www.example.com/";
    Properties props = new Properties();
    props.setProperty(VOL_KEY, "322");
    props.setProperty(BASE_URL_KEY, ROOT_URL);
    DefinableArchivalUnit au = makeAuFromProps(props);
    
    List<String> substanceList = ListUtil.list(
        ROOT_URL + "content/322/1/23.full.pdf",
        ROOT_URL + "content/322/125.full",
        ROOT_URL + "content/322/125.full.pdf",
        ROOT_URL + "content/jid/322/1/23.full.pdf",
        ROOT_URL + "content/jid/322/125.full",
        ROOT_URL + "content/jid/322/125.full.pdf"
        );
    
    List<String> notSubstanceList = ListUtil.list(
        ROOT_URL + "content/322/1/23.long",
        ROOT_URL + "content/322/1/23",
        ROOT_URL + "content/322/1/23.figures-only",
        ROOT_URL + "content/322/1.toc",
        ROOT_URL + "content/322/125",
        ROOT_URL + "content/322/125.abstract",
        ROOT_URL + "content/322/125.article-info",
        ROOT_URL + "content/322/1/1.full.pdf+html",
        ROOT_URL + "highwire/filestream/57879/field_highwire_adjunct_files/0/ds147561.pdf",
        ROOT_URL + "",
        ROOT_URL + ""
        );
    
    
    assertTrue(au.makeSubstanceUrlPatterns().size() == 2);
    boolean found = false;
    
    // List<Pattern> pat_list = au.makeSubstanceUrlPatterns();
    // The above Pattern is org.apache.oro.text.regex.Pattern, not compatible with java.util.regex.Pattern
    // <string>"^https?://%s/content(/[^/.]+){1,3}/(((?:([ivx]+)\.)?[^/.]+?(\.\d+)?))(\.(?:full([.]pdf)?)?)$", url_host(base_url)</string>
    // <string>"^https?://%s/content(/[^/.]+){1,3}/((ENEURO|wpt)\.[0-9.-]+)(\.(?:full([.]pdf)?)?)$", url_host(base_url)</string>
    
    String strPat = "^" + ROOT_URL + "content(/[^/.]+){1,3}/(((?:([ivx]+)\\.)?[^/.]+?(\\.\\d+)?))(\\.(?:full([.]pdf)?)?)$";
    Pattern thisPat = Pattern.compile(strPat);
    String lastUrl = "";
    
    for (String nextUrl : substanceList) {
      log.debug("testing for substance: "+ nextUrl);
      Matcher m = thisPat.matcher(nextUrl);
      lastUrl = nextUrl;
      found = m.matches();
      if (!found) break;
    }
    assertEquals(lastUrl, true, found);
    
    for (String nextUrl : notSubstanceList) {
      log.debug("testing for not substance: "+ nextUrl);
      Matcher m = thisPat.matcher(nextUrl);
      lastUrl = nextUrl;
      found = m.matches();
      if (found) break;
    }
    assertEquals(lastUrl, false, found);
  }
  
}