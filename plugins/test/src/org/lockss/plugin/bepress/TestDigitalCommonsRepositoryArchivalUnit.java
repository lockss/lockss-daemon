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

package org.lockss.plugin.bepress;

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
 * Tests archival unit for Digital Commons Repository.
 */

public class TestDigitalCommonsRepositoryArchivalUnit extends LockssTestCase {
  
  private static Logger log = 
      Logger.getLogger(TestDigitalCommonsRepositoryArchivalUnit.class);
  
  private MockLockssDaemon theDaemon;
  private DefinableArchivalUnit dcrAu;
  private URL baseUrl;

  private static final String PLUGIN_ID = 
      "org.lockss.plugin.bepress.DigitalCommonsRepositoryPlugin";
  
  private static final String PLUGIN_NAME = 
      "Digital Commons Plugin (Series, ETDs, Galleries, Book Series)";

  private static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  private static final String COLLECTION_TYPE_KEY = "collection_type";
  private static final String COLLECTION_KEY = "collection";
  private static final String COLLECTION_HEADING_KEY = "collection_heading";
  private static final String ROOT_URL = "http://www.example.com/";
  private static final String COLLECTION_TYPE = "xxxtype";  
  private static final String COLLECTION = "xxxdept";  
  private static final String COLLECTION_HEADING = "xxxheading";  
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
    baseUrl = new URL(ROOT_URL);
    dcrAu = makeAu(baseUrl, COLLECTION, COLLECTION_TYPE, COLLECTION_HEADING);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(URL url, String collection, 
                                       String collectionType, String year) 
      throws Exception {
    Properties props = new Properties();
    props.setProperty(COLLECTION_KEY, collection);
    props.setProperty(COLLECTION_TYPE_KEY, collectionType);
    props.setProperty(COLLECTION_HEADING_KEY, year);
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
      makeAu(null, "yyydept", "yyytype", "yyyheading");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
  
  // Test the crawl rules
  public void testShouldCacheProperPages() throws Exception {
    theDaemon.getLockssRepository(dcrAu);
    theDaemon.getNodeManager(dcrAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(dcrAu,
                             new RangeCachedUrlSetSpec(baseUrl.toString()));
    // Test for pages that should get crawled
    // permission page/start url
    // <dcrbase>/<collection>/lockss-<collection_type>-<collection>.html
    //    <dcrbase>/statistics/lockss-ir_series-statistics.html
    //    <dcrbase>/dissertations/lockss-ir_etd-dissertations.html
    shouldCacheTest(ROOT_URL + COLLECTION + "/lockss-" + COLLECTION_TYPE 
                    + "-" + COLLECTION + ".html", true, dcrAu, cus);  
    // toc page for an issue - <dcrbase>/statistics/
    shouldCacheTest(ROOT_URL + COLLECTION +"/", true, dcrAu, cus);
    // article files
    // abstract      - <dcrbase>/statistics/122/
    // full text pdf - <dcrbase>/cgi/viewcontent.cgi?article=1108
    //                                                  &context=statistics
    shouldCacheTest(ROOT_URL + COLLECTION + "/122/", true, dcrAu, cus);
    shouldCacheTest(ROOT_URL + "cgi/viewcontent.cgi?article=1108&context="
                    + COLLECTION, true, dcrAu, cus);
    // should not get crawled - missing cgi
    shouldCacheTest(ROOT_URL + "/viewcontent.cgi?article=1108&context"
                    + COLLECTION, false, dcrAu, cus);  
    // should not get crawled - LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, dcrAu, cus);
  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
                               ArchivalUnit au, CachedUrlSet cus) {
    log.debug3 ("shouldCacheTest url: " + url);
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
  public void testStartUrlConstruction() throws Exception {
    String expected = ROOT_URL + COLLECTION + "/lockss-" + COLLECTION_TYPE 
                      + "-" + COLLECTION + ".html";
    assertEquals(ListUtil.list(expected), 
                 dcrAu.getStartUrls());
  }
  
  public void testShouldNotCachePageFromOtherSite() throws Exception {
    theDaemon.getLockssRepository(dcrAu);
    theDaemon.getNodeManager(dcrAu);
    assertFalse(dcrAu.shouldBeCached(
        "http://shadow2.stanford.edu/toc/dcr/2013/2"));
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(dcrAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlFor0() throws Exception {
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(dcrAu.shouldCrawlForNewContent(aus));
  }

  public void testGetName() throws Exception {
    DefinableArchivalUnit au1 =
      makeAu(new URL("http://www.example1.com/"), "aaadept", "aaatype", "aaaheading");
    assertEquals(PLUGIN_NAME + ", Base URL http://www.example1.com/,"
                 + " Collection Type aaatype, Collection aaadept,"
                 + " Collection Heading aaaheading", au1.getName());
    DefinableArchivalUnit au2 =
      makeAu(new URL("http://www.example2.com/"), "bbbdept", "bbbtype", "bbbheading");
    assertEquals(PLUGIN_NAME + ", Base URL http://www.example2.com/,"
                 + " Collection Type bbbtype, Collection bbbdept,"
                 + " Collection Heading bbbheading", au2.getName());
  }

}

