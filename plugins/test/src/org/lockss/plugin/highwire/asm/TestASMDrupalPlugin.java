/*
 * $Id$
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire.asm;

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
import org.lockss.util.urlconn.CacheException.RetrySameUrlException;
import org.lockss.util.urlconn.HttpResultMap;

public class TestASMDrupalPlugin extends LockssTestCase {
  
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  
  private MockLockssDaemon theDaemon;
  private DefinablePlugin plugin;
  
  public TestASMDrupalPlugin(String msg) {
    super(msg);
  }
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
        "org.lockss.plugin.highwire.asm.ASMDrupalPlugin");
    
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
    props.setProperty(VOL_KEY, "1");
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
    props.setProperty(VOL_KEY, "2");
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    
    String starturl[] = {
        "http://www.example.com/lockss-manifest/vol_2_manifest.html",
    };
    DefinableArchivalUnit au = makeAuFromProps(props);
    assertEquals("American Society for Microbiology Plugin, Base URL http://www.example.com/, Volume 2",
        au.getName());
    assertEquals(ListUtil.list(starturl[0]), au.getStartUrls());
  }
  
  public void testGetPluginId() {
    assertEquals("org.lockss.plugin.highwire.asm.ASMDrupalPlugin",
        plugin.getPluginId());
  }
  
  public void testGetAuConfigProperties() {
    assertEquals(ListUtil.list(ConfigParamDescr.BASE_URL,
        ConfigParamDescr.VOLUME_NAME),
        plugin.getLocalAuConfigDescrs());
  }
  
  public void testHandles500Result() throws Exception {
    Properties props = new Properties();
    props.setProperty(VOL_KEY, "3");
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    
    String starturl =
        "http://www.example.com/lockss-manifest/vol_3_manifest.html";
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
    assertClass(RetrySameUrlException.class, exc);
    
  }
  
  // Test the crawl rules for APS
  public void testShouldCacheProperPages() throws Exception {
    String ROOT_URL = "http://ex.asm.org/";
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, ROOT_URL);
    props.setProperty(VOL_KEY, "3");
    DefinableArchivalUnit au = null;
    try {
      au = makeAuFromProps(props);
    }
    catch (ConfigurationException ex) {
    }
    theDaemon.getLockssRepository(au);
    /*
http://msystems.asm.org/content/1/6/e00128-16
http://msystems.asm.org/content/1/6/e00128-16.figures-only
http://msystems.asm.org/content/1/6/e00128-16.full.pdf
     */
    // Test for pages that should get crawled or not
    // permission page/start url
    shouldCacheTest(ROOT_URL + "lockss-manifest/vol_3_manifest.html", true, au);
    shouldCacheTest(ROOT_URL + "clockss-manifest/vol_3_manifest.html", true, au);
// FIXME    shouldCacheTest(ROOT_URL + "clockss-manifest/vol_123_manifest.html", false, au);
    shouldCacheTest(ROOT_URL + "manifest/year=2013", false, au);
    // vol and issue
    // shouldCacheTest(ROOT_URL + "content/3", false, au); changed crawl rule as there are some volume only tocs (iwa) so this test is true
    shouldCacheTest(ROOT_URL + "content/3/2", true, au);
    shouldCacheTest(ROOT_URL + "content/3/2.toc", true, au);
    // article files
    shouldCacheTest(ROOT_URL + "content/3/2/X3", true, au);
    shouldCacheTest(ROOT_URL.replace("http:", "https:") + "content/3/2/X3", true, au);
    shouldCacheTest(ROOT_URL + "content/3/2/X3.abstract", false, au);
    shouldCacheTest(ROOT_URL + "content/3/2/X3.extract", false, au);
    shouldCacheTest(ROOT_URL + "content/3/2/X3.full", false, au);
    shouldCacheTest(ROOT_URL + "content/3/2/X3.full.pdf", true, au);
    shouldCacheTest(ROOT_URL + "content/3/2/X3.full.pdf+html", true, au);
    shouldCacheTest(ROOT_URL + "content/3/2/X3.long", true, au);
    shouldCacheTest(ROOT_URL + "content/3/2/X3.figures-only", true, au);
    shouldCacheTest(ROOT_URL + "content/3/2/X3.", false, au);
    
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/jnl_ex_tab_data/node:433/1", false, au);
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/jnl_ex_tab_info/node:433/1", false, au);
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/jnl_ex_tab_pdf/node:433/1", false, au);
    
    shouldCacheTest(ROOT_URL + "highwire/markup/58493/expansion", false, au);
    shouldCacheTest(ROOT_URL + "highwire/article_citation_preview/19403", false, au);
    
    shouldCacheTest(ROOT_URL + "node/34", false, au);
    shouldCacheTest(ROOT_URL + "content/by/year", false, au);
    shouldCacheTest(ROOT_URL + "highwire/powerpoint/2311", false, au);
    shouldCacheTest(ROOT_URL + "content/early/2012/11/09/ex.00163.2012", false, au);
    shouldCacheTest(ROOT_URL + "lookup/external-ref?link_type=GEN", false, au);
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/jnl_ex_tab_art/node:433/1", false, au);
    shouldCacheTest(ROOT_URL + "content/3/2/X3.article-info", false, au);
    shouldCacheTest(ROOT_URL + "content/1/1/masthead.1-1", true, au);
    shouldCacheTest(ROOT_URL + "content/1/1/masthead.1-1.full.pdf", true, au);
    
    shouldCacheTest("http://cdn-site.elifesciences.org/content/elife/1/e00003/F1.medium.gif", false, au);
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