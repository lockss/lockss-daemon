/*
 * $Id: TestCanadianYoungScientistArchivalUnit.java,v 1.2 2014-11-12 20:11:59 wkwilson Exp $
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

package org.lockss.plugin.atypon.canadianyoungscientist;

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
 * Tests archival unit for Canadian Young Scientist.
 */

public class TestCanadianYoungScientistArchivalUnit extends LockssTestCase {
  
  private static Logger log = 
      Logger.getLogger(TestCanadianYoungScientistArchivalUnit.class);
  
  private MockLockssDaemon theDaemon;
  private DefinableArchivalUnit cysAu;
  private URL baseUrl;

  private static final String PLUGIN_ID = "org.lockss.plugin.atypon" +
  		".canadianyoungscientist.ClockssCanadianYoungScientistPlugin";
  
  private static final String PLUGIN_NAME = 
                "Canadian Young Scientist Plugin (CLOCKSS)";

  private static final String 
                        BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  private static final String 
                        JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  private static final String 
                        VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
    
  private static final String ROOT_URL = "http://www.example.com/";
  private static final String JOURNAL_ID = "xxxjid";  
  private static final String VOLUME_NAME = "2013";  
  

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();

    baseUrl = new URL(ROOT_URL);
    cysAu = makeAu(baseUrl, JOURNAL_ID, VOLUME_NAME);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(URL url, 
                                       String journalId, String volumeName) 
      throws Exception {
 
    Properties props = new Properties();
    props.setProperty(JOURNAL_ID_KEY, journalId);
    props.setProperty(VOL_KEY, volumeName);
    
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
      makeAu(null, "yyyjid", "2013");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
  
  // Test the crawl rules for Canadian Young Scientist
  public void testShouldCacheProperPages() throws Exception {
    theDaemon.getLockssRepository(cysAu);
    theDaemon.getNodeManager(cysAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(cysAu,
                             new RangeCachedUrlSetSpec(baseUrl.toString()));
    // Test for pages that should get crawled
    // permission page/start url
    // <cysbase>/clockss/cysj/2013/index.html
    shouldCacheTest(ROOT_URL + "clockss/" + JOURNAL_ID + "/" + VOLUME_NAME 
                    + "/index.html", true, cysAu, cus);  
    // toc page for an issue - <cysbase>/toc/cysj/2013/2
    shouldCacheTest(ROOT_URL + "toc/" + JOURNAL_ID +"/" + VOLUME_NAME + "/2",
                    true, cysAu, cus);
    // article files
    // abstract  - <cysbase>/doi/abs/10.13034/cysj-2013-004
    // full text - <cysbase>/doi/full/10.13034/cysj-2013-004
    // pdf       - <cysbase>/doi/pdf/10.13034/cysj-2013-004
    // pdfplus   - <cysbase>/doi/pdfplus/10.13034/cysj-2013-004

    shouldCacheTest(ROOT_URL + "doi/abs/10.13034/cysj-2013-004", 
                    true, cysAu, cus);
    shouldCacheTest(ROOT_URL + "doi/full/10.13034/cysj-2013-004", 
                    true, cysAu, cus);
    shouldCacheTest(ROOT_URL + "doi/pdf/10.13034/cysj-2013-004",
                    true, cysAu, cus);
    shouldCacheTest(ROOT_URL + "doi/pdfplus/10.13034/cysj-2013-004",
                    true, cysAu, cus);
    
    // should not get crawled - missing doi
    shouldCacheTest(ROOT_URL + "/full/cysj-2013-004", false, cysAu, cus);  
    // should not get crawled - LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, cysAu, cus);
  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
                               ArchivalUnit au, CachedUrlSet cus) {
    log.info ("shouldCacheTest url: " + url);
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
  public void testStartUrlConstruction() throws Exception {
    String expected = ROOT_URL + "clockss/" + JOURNAL_ID + "/" 
                      + VOLUME_NAME + "/index.html";
    assertEquals(ListUtil.list(expected), 
                 cysAu.getStartUrls());
  }
  
  public void testShouldNotCachePageFromOtherSite() throws Exception {
    theDaemon.getLockssRepository(cysAu);
    theDaemon.getNodeManager(cysAu);
    assertFalse(cysAu.shouldBeCached("http://shadow2.stanford.edu/toc/cysj/2013/2"));
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(cysAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlFor0() throws Exception {
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(cysAu.shouldCrawlForNewContent(aus));
  }

  public void testgetName() throws Exception {
    DefinableArchivalUnit au1 =
      makeAu(new URL("http://www.example1.com/"), "aaajid", "2012");
    assertEquals(PLUGIN_NAME + ", Base URL http://www.example1.com/,"
                 + " Journal ID aaajid, Volume 2012", au1.getName());
    DefinableArchivalUnit au2 =
      makeAu(new URL("http://www.example2.com/"), "bbbjid", "2013");
    assertEquals(PLUGIN_NAME + ", Base URL http://www.example2.com/,"
                 + " Journal ID bbbjid, Volume 2013", au2.getName());
  }

}

