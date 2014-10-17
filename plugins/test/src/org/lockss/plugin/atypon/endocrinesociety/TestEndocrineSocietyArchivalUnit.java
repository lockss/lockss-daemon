/*
 * $Id: TestEndocrineSocietyArchivalUnit.java,v 1.1 2014-10-17 21:33:11 ldoan Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.endocrinesociety;

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
 * Tests archival unit for Endocrine Society.
 */

public class TestEndocrineSocietyArchivalUnit 
  extends LockssTestCase {
  
  private static Logger log = Logger.getLogger(
      TestEndocrineSocietyArchivalUnit.class);
  
  private MockLockssDaemon theDaemon;
  private DefinableArchivalUnit esAu;
  private URL baseUrl;

  private static final String PLUGIN_ID = 
      "org.lockss.plugin.atypon.endocrinesociety.ClockssEndocrineSocietyPlugin";
  
  private static final String PLUGIN_NAME = 
      "Endocrine Society Plugin (Atypon for CLOCKSS)";

  private static final String BASE_URL_KEY = 
      ConfigParamDescr.BASE_URL.getKey();
  private static final String JOURNAL_ID_KEY = 
      ConfigParamDescr.JOURNAL_ID.getKey();
  private static final String VOL_KEY = 
      ConfigParamDescr.VOLUME_NAME.getKey();
    
  private static final String ROOT_URL = "http://www.example.com/";
  private static final String JOURNAL_ID = "xxxjid";  
  private static final String VOLUME_NAME = "154";  
  

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();

    baseUrl = new URL(ROOT_URL);
    esAu = makeAu(baseUrl, JOURNAL_ID, VOLUME_NAME);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(URL url, String journalId, 
      String volumeName) throws Exception {
    Properties props = new Properties();
    props.setProperty(JOURNAL_ID_KEY, journalId);
    props.setProperty(VOL_KEY, volumeName);
    
    if (url != null) {
      props.setProperty(BASE_URL_KEY, url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    DefinablePlugin esDefinablePlugin = new DefinablePlugin();
    esDefinablePlugin.initPlugin(getMockLockssDaemon(), PLUGIN_ID);
    DefinableArchivalUnit au = 
        (DefinableArchivalUnit)esDefinablePlugin.createAu(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, "yyyjid", "99");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
  
  // Test the crawl rules for Endocrine Society
  public void testShouldCacheProperPages() throws Exception {
    theDaemon.getLockssRepository(esAu);
    theDaemon.getNodeManager(esAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(esAu, 
                              new RangeCachedUrlSetSpec(baseUrl.toString()));
    // Test for pages that should get crawled
    // permission page/start url
    // <esbase>/clockss/jid/1/index.html
    // <esbase>/clockss/jid/154/index.html
    shouldCacheTest(ROOT_URL + "clockss/" + JOURNAL_ID + "/" + VOLUME_NAME + 
        "/index.html", true, esAu, cus);  
    // toc page for an issue - <esbase>/toc/jid/154/4
    shouldCacheTest(ROOT_URL + "toc/" + JOURNAL_ID +"/" + VOLUME_NAME + "/4",
                    true, esAu, cus);
    // article files
    // abstract  - <esbase>/doi/abs/11.1111/xx.2013-1111
    // full text - <esbase>/doi/full/11.1111/xx.2013-1111
    // pdf       - <esbase>/doi/pdf/11.1111/xx.2013-1111
    // ref       - <esbase>/doi/ref/11.1111/xx.2013-1111
    // suppl     - <esbase>/doi/suppl/11.1111/xx.2013-1111
    shouldCacheTest(ROOT_URL + "doi/abs/11.1111/xx.2013-1111", 
                    true, esAu, cus);
    shouldCacheTest(ROOT_URL + "doi/full/11.1111/xx.2013-1111", 
                    true, esAu, cus);
    shouldCacheTest(ROOT_URL + "doi/pdf/11.1111/xx.2013-1111",
                    true, esAu, cus);
    shouldCacheTest(ROOT_URL + "doi/ref/11.1111/xx.2013-1111",
                    true, esAu, cus);
    shouldCacheTest(ROOT_URL + "doi/suppl/11.1111/xx.2013-1111",
                    true, esAu, cus);
    // should not get crawled - missing doi
    shouldCacheTest(ROOT_URL + "/full/xx.2013-1111", false, esAu, cus);  
    // should not get crawled - LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, esAu, cus);
  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
                               ArchivalUnit au, CachedUrlSet cus) {
    //log.info ("shouldCacheTest url: " + url);
    UrlCacher uc = au.makeUrlCacher(url);
    assertEquals(shouldCache, uc.shouldBeCached());
  }
  
  public void testStartUrlConstruction() throws Exception {
    String expected = ROOT_URL + "clockss/" + JOURNAL_ID + "/" 
                      + VOLUME_NAME + "/index.html";
    assertEquals(ListUtil.list(expected), 
                 esAu.getNewContentCrawlUrls());
  }
  
  public void testShouldNotCachePageFromOtherSite() throws Exception {
    theDaemon.getLockssRepository(esAu);
    theDaemon.getNodeManager(esAu);
    UrlCacher uc = esAu.makeUrlCacher(
        "http://shadow2.stanford.edu/toc/esj/154/2");
    assertFalse(uc.shouldBeCached());
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(esAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlFor0() throws Exception {
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(esAu.shouldCrawlForNewContent(aus));
  }

  public void testgetName() throws Exception {
    DefinableArchivalUnit au1 =
      makeAu(new URL("http://www.example1.com/"), "aaajid", "25");
    assertEquals(PLUGIN_NAME + ", Base URL http://www.example1.com/,"
                 + " Journal ID aaajid, Volume 25", au1.getName());
    DefinableArchivalUnit au2 =
      makeAu(new URL("http://www.example2.com/"), "bbbjid", "44");
    assertEquals(PLUGIN_NAME + ", Base URL http://www.example2.com/,"
                 + " Journal ID bbbjid, Volume 44", au2.getName());
  }

}

