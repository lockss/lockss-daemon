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

public class TestBMJDrupalPlugin extends LockssTestCase {
  
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  
  private MockLockssDaemon theDaemon;
  private DefinablePlugin plugin;
  
  public TestBMJDrupalPlugin(String msg) {
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
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    
    String starturl =
        "http://www.example.com/lockss-manifest/vol_313_manifest.html";
    DefinableArchivalUnit au = makeAuFromProps(props);
    assertEquals("BMJ Plugin, Base URL http://www.example.com/, Volume 313",
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
    shouldCacheTest(ROOT_URL + "content/347/7915.toc", false, au);
    // article files
    shouldCacheTest(ROOT_URL + "content/321/bmj.f6056.long", true, au);
    shouldCacheTest(ROOT_URL + "content/321/bmj.f4270", true, au);
    shouldCacheTest(ROOT_URL + "content/321/bmj.f4270.full", true, au);
    shouldCacheTest(ROOT_URL + "content/321/bmj.f4270.full.pdf", true, au);
    shouldCacheTest(ROOT_URL + "content/321/bmj.f4270.full.pdf+html", true, au);
    shouldCacheTest(ROOT_URL + "content/321/bmj.f4270/peer-review", true, au);
    // only for data supplements
    shouldCacheTest(ROOT_URL + "content/347/bmj.f4547/related", true, au);
    
    shouldCacheTest(ROOT_URL + "content/suppl/2014/05/16/bmj.f6123.DC1", true, au);
    shouldCacheTest(ROOT_URL + "highwire/markup/185154/expansion", true, au);
    
    shouldCacheTest(ROOT_URL + "sites/default/themes/bmj/the_bmj/css/fonts.css", true, au);
    shouldCacheTest(ROOT_URL + "sites/default/files/cdn/css/http/css_nZpqy9LysFSwGKJ7v4z11U9YB9kpVSWL_JhlIW3O5FI.css", true, au);
    
    // should not get crawled - LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, au);
    
    shouldCacheTest(ROOT_URL + "bmj/section-pdf/724572/0", false, au);
    shouldCacheTest(ROOT_URL + "content/347/bmj.f4547/article-info", false, au);
    shouldCacheTest(ROOT_URL + "content/347/bmj.f4547/rapid-responses", false, au);
    shouldCacheTest(ROOT_URL + "content/347/bmj.f4547/submit-a-rapid-response", false, au);
    shouldCacheTest(ROOT_URL + "highwire/powerpoint/185149", false, au);
    shouldCacheTest(ROOT_URL + "lookup/doi/10.1136/bmj.6444", false, au);
    shouldCacheTest(ROOT_URL + "node/207663", false, au);
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/bmj_rapid_responses_form/node:185147/1", false, au);
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/jnl_bmj_tab_art/node:725123/1", false, au);
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/jnl_bmj_tab_info/node:728958/1", false, au);
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/jnl_bmj_tab_related_art/node:735423/1", true, au);
    shouldCacheTest(ROOT_URL + "panels_ajax_tab/jnl_bmj_tab_peer_review/node:735423/1", true, au);
    shouldCacheTest(ROOT_URL + "", false, au);
  }
  
  private void shouldCacheTest(String url, boolean shouldCache, ArchivalUnit au) {
    log.info ("shouldCacheTest url: " + url);
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
}