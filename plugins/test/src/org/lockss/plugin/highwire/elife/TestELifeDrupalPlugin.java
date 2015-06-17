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

public class TestELifeDrupalPlugin extends LockssTestCase {
  
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  
  private MockLockssDaemon theDaemon;
  private DefinablePlugin plugin;
  
  public TestELifeDrupalPlugin(String msg) {
    super(msg);
  }
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
        "org.lockss.plugin.highwire.elife.ELifeDrupalPlugin");
    
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
    
    String starturl =
        "http://www.example.com/lockss-manifest/elife_2013.html";
    DefinableArchivalUnit au = makeAuFromProps(props);
    assertEquals("eLife Sciences Plugin, Base URL http://www.example.com/, Volume 2013",
        au.getName());
    assertEquals(ListUtil.list(starturl), au.getStartUrls());
  }
  
  public void testGetPluginId() {
    assertEquals("org.lockss.plugin.highwire.elife.ELifeDrupalPlugin",
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
    assertClass(CacheException.UnexpectedNoRetryNoFailException.class, exc);
    
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
    shouldCacheTest(ROOT_URL + "lockss-manifest/elife_2013.html", true, au);
    shouldCacheTest(ROOT_URL + "clockss-manifest/elife_2013.html", false, au);
    shouldCacheTest(ROOT_URL + "manifest/year=2013", false, au);
    // toc page for an issue, there is no issue
    shouldCacheTest(ROOT_URL + "content/1", false, au);
    // article files
    shouldCacheTest(ROOT_URL + "content/1/e00002", true, au);
    shouldCacheTest(ROOT_URL + "content/1/e00003/article-data", true, au);
    shouldCacheTest(ROOT_URL + "content/1/e00003/article-info", false, au);
    shouldCacheTest(ROOT_URL + "content/1/e00003.abstract", true, au);
    shouldCacheTest(ROOT_URL + "content/1/e00003.full.pdf", true, au);
    shouldCacheTest(ROOT_URL + "content/1/2/e00003.full.pdf", false, au);
    shouldCacheTest(ROOT_URL + "content/elife/1/e00003/F1.large.jpg", true, au);
    shouldCacheTest(ROOT_URL + "content/elife/1/e00003/F3/F4.large.jpg", true, au);
    shouldCacheTest(ROOT_URL + "content/elife/1/e00003.full.pdf", true, au);
    shouldCacheTest(ROOT_URL + "highwire/citation/12/ris", true, au);
    shouldCacheTest(ROOT_URL + "highwire/citation/9/1/ris", false, au);
    shouldCacheTest(ROOT_URL + "highwire/markup/113/expansion", true, au);
    shouldCacheTest(ROOT_URL + "content/1/e00011/DC5", true, au);
    shouldCacheTest(ROOT_URL + "sites/all/libraries/modernizr/modernizr.min.js", true, au);
    shouldCacheTest(ROOT_URL + "sites/default/files/js/js_0j8_f76rvZ212f4rg.js", true, au);
    shouldCacheTest(ROOT_URL + "sites/default/themes/elife/font/fontawesome-webfont.eot", true, au);
    shouldCacheTest(ROOT_URL + "sites/default/themes/font/fontawesome-webfont.eot", true, au);
    
    shouldCacheTest(ROOT_URL + "content/1/e00003/article-metrics", false, au);
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/jnl_elife_article_article/node:8375/0", false, au);
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/jnl_elife_article_figdata/node:8375/1", true, au);
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
    
    shouldCacheTest("http://cdn-site.elifesciences.org/content/elife/1/e00003/F1.medium.gif", true, au);
    shouldCacheTest("http://cdn-site.elifesciences.org/content/elife/1/e00003/F3/F4.small.gif", true, au);
    shouldCacheTest("http://cdn-site.elifesciences.org/misc/draggable.png", true, au);
    shouldCacheTest("http://cdn-site.elifesciences.org/sites/all/libraries/cluetip/images/arrowdown.gif", true, au);
    shouldCacheTest("http://cdn-site.elifesciences.org/sites/default/files/cdn/css/http/css_2iejb0.css", true, au);
    shouldCacheTest("http://cdn-site.elifesciences.org/sites/default/themes/elife/css/PIE/PIE.htc", true, au);
    shouldCacheTest("http://cdn-site.elifesciences.org/sites/default/themes/elife/font/fontawesome-webfont.woff", true, au);
    shouldCacheTest("http://cdn.elifesciences.org/elife-articles/00003/figures-pdf/elife00003-figures.pdf", true, au);
    shouldCacheTest("http://cdn.mathjax.org/mathjax/latest/MathJax.js", true, au);
    shouldCacheTest("https://ajax.googleapis.com/ajax/libs/jquery/1.8.2/jquery.min.js", true, au);
    shouldCacheTest("", false, au);
    
    // should not get crawled - LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, au);
    
  }
  
  private void shouldCacheTest(String url, boolean shouldCache, ArchivalUnit au) {
    log.info ("shouldCacheTest url: " + url);
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
}