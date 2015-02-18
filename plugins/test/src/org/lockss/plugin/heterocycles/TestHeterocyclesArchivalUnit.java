/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.heterocycles;

import java.net.URL;
import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.RangeCachedUrlSetSpec;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.plugin.UrlCacher;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.state.AuState;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockAuState;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.ListUtil;
import org.lockss.util.Logger;
import org.lockss.util.TimeBase;

/* 
 * Tests archival unit for Heterocycles Archival site
 */

public class TestHeterocyclesArchivalUnit extends LockssTestCase {
  
  private static Logger log = 
      Logger.getLogger(TestHeterocyclesArchivalUnit.class);
  
  private MockLockssDaemon theDaemon;
  private DefinableArchivalUnit heterocyclesAu;
  private URL baseUrl;

  private static final String PLUGIN_ID = 
      "org.lockss.plugin.heterocycles.ClockssHeterocyclesPlugin";
  
  private static final String PLUGIN_NAME = "Heterocycles Plugin (CLOCKSS)";

  private static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  private static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  
  private static final String ROOT_URL = "http://www.example.com/";
  private static final String VOLUME_NAME = "87";  

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();

    baseUrl = new URL(ROOT_URL);
    heterocyclesAu = makeAu(baseUrl, VOLUME_NAME);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(URL url, String volumeName) 
      throws Exception {
 
    Properties props = new Properties();
    props.setProperty(VOL_KEY, volumeName);
    if (url != null) {
      props.setProperty(BASE_URL_KEY, url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    
    DefinablePlugin heterocyclesDefinablePlugin = new DefinablePlugin();
    heterocyclesDefinablePlugin.initPlugin(getMockLockssDaemon(), PLUGIN_ID);
    DefinableArchivalUnit au = 
        (DefinableArchivalUnit)heterocyclesDefinablePlugin.createAu(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, "83");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
  
  // Test the crawl rules for Heterocycles Archives site
  public void testShouldCacheProperPages() throws Exception {
    theDaemon.getLockssRepository(heterocyclesAu);
    theDaemon.getNodeManager(heterocyclesAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(heterocyclesAu,
                             new RangeCachedUrlSetSpec(baseUrl.toString()));
    // Test for pages that should get crawled
    // permission page / start url 
    // http://www.heterocycles.jp/clockss/manifest/vol/87
    shouldCacheTest(ROOT_URL + "clockss/manifest/vol/" + VOLUME_NAME, 
                    true, heterocyclesAu, cus);  
    // toc page for an issue
    // http://www.heterocycles.jp/clockss/libraries/journal/87/3
    shouldCacheTest(ROOT_URL + "clockss/libraries/journal/87/3", 
                    true, heterocyclesAu, cus);
    // article files
    // http://www.heterocycles.jp/clockss/libraries/fulltext/23371/87/8
    // http://www.heterocycles.jp/clockss/downloads/PDF/22935/87/3
    // http://www.heterocycles.jp/clockss/downloads/PDFwithLinks/22935/87/3
    // http://www.heterocycles.jp/clockss/downloads/PDFsi/22998/87/3
    shouldCacheTest(ROOT_URL + "clockss/libraries/fulltext/23371/87/8",
                    true, heterocyclesAu, cus);
    shouldCacheTest(ROOT_URL + "clockss/downloads/PDF/22935/87/3", 
                    true, heterocyclesAu, cus);
    shouldCacheTest(ROOT_URL + "clockss/downloads/PDFwithLinks/22935/87/3", 
                    true, heterocyclesAu, cus);
    shouldCacheTest(ROOT_URL + "clockss/downloads/PDFsi/22935/87/3", 
                    true, heterocyclesAu, cus);
    // images figures and tables can live under
    // http://www.heterocycles.jp/clockss/img/*
    // http://www.heterocycles.jp/scimg/*
    shouldCacheTest(ROOT_URL + "clockss/img/cycle.gif", 
                    true, heterocyclesAu, cus);
    shouldCacheTest(ROOT_URL + "scimg/0/0000059345?style=opaque&width=450", 
                    true, heterocyclesAu, cus);
    
    // should not get crawled - missing volume name 
    shouldCacheTest(ROOT_URL + "clockss/manifest/vol/", 
                    false, heterocyclesAu, cus);  
    // LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", 
                    false, heterocyclesAu, cus);
  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
                               ArchivalUnit au, CachedUrlSet cus) {
    log.info ("shouldCacheTest url: " + url);
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
  public void testStartUrlConstruction() throws Exception {
    String expected = ROOT_URL + "clockss/manifest/vol/" + VOLUME_NAME;
    assertEquals(ListUtil.list(expected), 
                 heterocyclesAu.getStartUrls());
  }
  
  public void testShouldNotCachePageFromOtherSite() throws Exception {
    theDaemon.getLockssRepository(heterocyclesAu);
    theDaemon.getNodeManager(heterocyclesAu);
    assertFalse(heterocyclesAu.shouldBeCached(
        "http://shadow2.stanford.edu/lockss/manifest/vol/2"));
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(heterocyclesAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlFor0() throws Exception {
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(heterocyclesAu.shouldCrawlForNewContent(aus));
  }

  public void testgetName() throws Exception {
    DefinableArchivalUnit au1 =
      makeAu(new URL("http://www.example1.com/"), "20");
    assertEquals(PLUGIN_NAME + ", Base URL http://www.example1.com/, Volume 20", 
                 au1.getName());
    DefinableArchivalUnit au2 =
      makeAu(new URL("http://www.example2.com/"), "44");
    assertEquals(PLUGIN_NAME + ", Base URL http://www.example2.com/, Volume 44",
                 au2.getName());
  }

}

