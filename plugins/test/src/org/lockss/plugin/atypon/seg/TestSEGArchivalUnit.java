/*
 * $Id$
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

package org.lockss.plugin.atypon.seg;

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
 * Tests archival unit for Society Of Exploration Geophysicists.
 */

public class TestSEGArchivalUnit 
  extends LockssTestCase {
  
  private static Logger log = Logger.getLogger(
      TestSEGArchivalUnit.class);
  
  private MockLockssDaemon theDaemon;
  private DefinableArchivalUnit segAu;
  private URL baseUrl;

  private static final String PLUGIN_ID = 
      "org.lockss.plugin.atypon.seg.ClockssSEGPlugin";
  
  private static final String PLUGIN_NAME = 
                "Society of Exploration Geophysicists (CLOCKSS)";

  private static final String BASE_URL_KEY = 
      ConfigParamDescr.BASE_URL.getKey();
  private static final String JOURNAL_ID_KEY = 
      ConfigParamDescr.JOURNAL_ID.getKey();
  private static final String VOL_KEY = 
      ConfigParamDescr.VOLUME_NAME.getKey();
    
  private static final String ROOT_URL = "http://www.example.com/";
  private static final String JOURNAL_ID = "xxxjid";  
  private static final String VOLUME_NAME = "78";  
  

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();

    baseUrl = new URL(ROOT_URL);
    segAu = makeAu(baseUrl, JOURNAL_ID, VOLUME_NAME);
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
    
    DefinablePlugin segDefinablePlugin = new DefinablePlugin();
    segDefinablePlugin.initPlugin(getMockLockssDaemon(), PLUGIN_ID);
    DefinableArchivalUnit au = 
        (DefinableArchivalUnit)segDefinablePlugin.createAu(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, "yyyjid", "99");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
  
  // Test the crawl rules for Society Of Exploration Geophysicists
  public void testShouldCacheProperPages() throws Exception {
    theDaemon.getLockssRepository(segAu);
    theDaemon.getNodeManager(segAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(segAu,
                             new RangeCachedUrlSetSpec(baseUrl.toString()));
    // Test for pages that should get crawled
    // permission page/start url
    // <segbase>/clockss/jid/1/index.html
    shouldCacheTest(ROOT_URL + "clockss/" + JOURNAL_ID + "/" + VOLUME_NAME 
                    + "/index.html", true, segAu, cus);  
    // toc page for an issue - <segbase>/toc/jid/2013/2
    shouldCacheTest(ROOT_URL + "toc/" + JOURNAL_ID +"/" + VOLUME_NAME + "/2",
                    true, segAu, cus);
    // article files
    // abstract  - <segbase>/doi/abs/11.1111/XXX-2013-0041.1
    // full text - <segbase>/doi/full/11.1111/XXX-2013-0041.1
    // pdf       - <segbase>/doi/pdf/11.1111/XXX-2013-0041.1
    // pdfplus   - <segbase>/doi/pdfplus/11.1111/XXX-2013-0041.1

    shouldCacheTest(ROOT_URL + "doi/abs/11.1111/XXX-2013-0041.1", 
                    true, segAu, cus);
    shouldCacheTest(ROOT_URL + "doi/full/11.1111/XXX-2013-0041.1", 
                    true, segAu, cus);
    shouldCacheTest(ROOT_URL + "doi/pdf/11.1111/XXX-2013-0041.1",
                    true, segAu, cus);
    shouldCacheTest(ROOT_URL + "doi/pdfplus/11.1111/XXX-2013-0041.1",
                    true, segAu, cus);
    
    // should not get crawled - missing doi
    shouldCacheTest(ROOT_URL + "/full/XXX-2013-0041.1", false, segAu, cus);  
    // should not get crawled - LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, segAu, cus);
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
                 segAu.getStartUrls());
  }
  
  public void testShouldNotCachePageFromOtherSite() throws Exception {
    theDaemon.getLockssRepository(segAu);
    theDaemon.getNodeManager(segAu);
    assertFalse(segAu.shouldBeCached(
        "http://shadow2.stanford.edu/toc/segj/2013/2"));
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(segAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlFor0() throws Exception {
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(segAu.shouldCrawlForNewContent(aus));
  }

  public void testGetName() throws Exception {
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

