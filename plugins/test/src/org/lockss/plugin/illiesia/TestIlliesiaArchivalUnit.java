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

package org.lockss.plugin.illiesia;

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
 * Tests archival unit for Illiesia plugin.
 */

public class TestIlliesiaArchivalUnit extends LockssTestCase {
  
  private static Logger log = 
      Logger.getLogger(TestIlliesiaArchivalUnit.class);
  
  private MockLockssDaemon theDaemon;
  private DefinableArchivalUnit iau;
  private URL baseUrl;

  private static final String PLUGIN_ID = 
      "org.lockss.plugin.illiesia.IlliesiaPlugin";
  
  private static final String PLUGIN_NAME = "Illiesia Plugin";

  private static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  private static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  private static final String BASE_URL = "http://www.example.com/";
  private static final String YEAR = "2013";  
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
    baseUrl = new URL(BASE_URL);
    iau = makeAu(baseUrl, YEAR);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(URL url, String year) 
      throws Exception {
    Properties props = new Properties();
    props.setProperty(YEAR_KEY, year);
    if (url != null) {
      props.setProperty(BASE_URL_KEY, url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    DefinablePlugin cysDefinablePlugin = new DefinablePlugin();
    cysDefinablePlugin.initPlugin(getMockLockssDaemon(), PLUGIN_ID);
    DefinableArchivalUnit au = 
        (DefinableArchivalUnit)cysDefinablePlugin.createAu(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, "2013");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
  
  // Test the crawl rules
  public void testShouldCacheProperPages() throws Exception {
    theDaemon.getLockssRepository(iau);
    theDaemon.getNodeManager(iau);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(iau,
                             new RangeCachedUrlSetSpec(baseUrl.toString()));
    // Test for pages that should get crawled
    // permission page/start url  ???not know yet
    // <illiesiabase>/
    //shouldCacheTest(BASE_URL + YEAR + ".html", true, iau, cus);  
    // toc page for an issue - <illiesiabase>/html/2013.html
    shouldCacheTest(BASE_URL + "html/" + YEAR + ".html", true, iau, cus);
    // article files
    // full text pdf - <illiesiabase>/papers/Illiesia09-01.pdf
    shouldCacheTest(BASE_URL + "papers/Illiesia09-01.pdf", true, iau, cus);
    shouldCacheTest(BASE_URL + "papers/Illiesia09-02.pdf", true, iau, cus);
    // should not get crawled - missing cgi
    shouldCacheTest(BASE_URL + "Illiesia09-02.pdf", false, iau, cus);
    // should not get crawled - LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, iau, cus);
  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
                               ArchivalUnit au, CachedUrlSet cus) {
    log.debug3 ("shouldCacheTest url: " + url);
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
  public void testStartUrlConstruction() throws Exception {
    String expected = BASE_URL + "html/" + YEAR + ".html";
    assertEquals(ListUtil.list(expected), iau.getStartUrls());
  }
  
  public void testShouldNotCachePageFromOtherSite() throws Exception {
    theDaemon.getLockssRepository(iau);
    theDaemon.getNodeManager(iau);
    assertFalse(iau.shouldBeCached(
        "http://shadow2.stanford.edu/toc/i/2013/2"));
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(iau.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlFor0() throws Exception {
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(iau.shouldCrawlForNewContent(aus));
  }

  public void testgetName() throws Exception {
    DefinableArchivalUnit au1 =
      makeAu(new URL("http://www.example1.com/"), "2012");
    assertEquals(PLUGIN_NAME + ", Base URL http://www.example1.com/,"
                 + " Year 2012", au1.getName());
    DefinableArchivalUnit au2 =
      makeAu(new URL("http://www.example2.com/"), "2013");
    assertEquals(PLUGIN_NAME + ", Base URL http://www.example2.com/,"
                 + " Year 2013", au2.getName());
  }

}

