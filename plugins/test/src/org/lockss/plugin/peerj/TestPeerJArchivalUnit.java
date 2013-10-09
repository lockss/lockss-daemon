/*
 * $Id: TestPeerJArchivalUnit.java,v 1.2 2013-10-09 23:01:38 ldoan Exp $
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

package org.lockss.plugin.peerj;

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
 * Tests archival unit for PeerJ Archival site
 */

public class TestPeerJArchivalUnit extends LockssTestCase {
  
  private static Logger log = Logger.getLogger(TestPeerJArchivalUnit.class);
  
  private MockLockssDaemon theDaemon;
  private DefinableArchivalUnit peerjArchivesAu;
  private DefinableArchivalUnit peerjPreprintsAu;
  private URL baseUrl;

  private static final String PLUGIN_ID = "org.lockss.plugin.peerj.PeerJPlugin";
  private static final String PLUGIN_NAME = "PeerJ Plugin";

  private static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  private static final String PEERJ_SITE_KEY = "peerj_site";
  private static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  
  private static final String ROOT_URL = "http://www.example.com/";
  private static final String PEERJ_ARCHIVES_SITE = "xxxxSite";
  private static final String PEERJ_PREPRINTS_SITE = "xxxxPreprintsSite";
  private static final String VOLUME_NAME = "2013";
  

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();

    baseUrl = new URL(ROOT_URL);
    peerjArchivesAu = makeAu(baseUrl, VOLUME_NAME, PEERJ_ARCHIVES_SITE);
    peerjPreprintsAu = makeAu(baseUrl, VOLUME_NAME, 
                                      PEERJ_PREPRINTS_SITE);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(URL url, String volumeName, 
                                          String peerjSite) throws Exception {
    Properties props = new Properties();
    props.setProperty(VOL_KEY, volumeName);
    props.setProperty(PEERJ_SITE_KEY, peerjSite);
    if (url != null) {
      props.setProperty(BASE_URL_KEY, url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    
    DefinablePlugin peerjDefinablePlugin = new DefinablePlugin();
    peerjDefinablePlugin.initPlugin(getMockLockssDaemon(), PLUGIN_ID);
    DefinableArchivalUnit au = 
        (DefinableArchivalUnit)peerjDefinablePlugin.createAu(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, "2011", "peerjSiteBad");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
  
  // Test the crawl rules for PeerJ Archives site
  public void testShouldCacheProperArchivesPages() throws Exception {
    theDaemon.getLockssRepository(peerjArchivesAu);
    theDaemon.getNodeManager(peerjArchivesAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(peerjArchivesAu,
                               new RangeCachedUrlSetSpec(baseUrl.toString()));
    // Test for pages that should get crawled
    // permission page
    shouldCacheTest(ROOT_URL + "lockss.txt", true, peerjArchivesAu, cus);  
    // volume page
    // https://peerj.com/archives/?year=2013
    shouldCacheTest(ROOT_URL + "xxxxSite/?year=2013", 
                    true, peerjArchivesAu, cus);  
    // toc page for an issue
    // https://peerj.com/articles/index.html?month=2013-09
    shouldCacheTest(ROOT_URL + "articles/index.html?month=2013-09", 
                    true, peerjArchivesAu, cus);
    // article files
    // https://peerj.com/articles/55/
    // https://peerj.com/articles/55.bib
    // https://peerj.com/articles/55.pdf
    // https://peerj.com/articles/55.ris
    // https://peerj.com/articles/55.xml
    // https://peerj.com/articles/55.html
    // https://peerj.com/articles/55.rdf
    // https://peerj.com/articles/55.json
    // https://peerj.com/articles/55.unixref
    shouldCacheTest(ROOT_URL+"articles/55/", true, peerjArchivesAu, cus);
    shouldCacheTest(ROOT_URL+"articles/55.bib", true, peerjArchivesAu, cus);
    shouldCacheTest(ROOT_URL+"articles/55.pdf", true, peerjArchivesAu, cus);
    shouldCacheTest(ROOT_URL+"articles/55.ris", true, peerjArchivesAu, cus);
    shouldCacheTest(ROOT_URL+"articles/55.xml", true, peerjArchivesAu, cus);
    shouldCacheTest(ROOT_URL+"articles/55.html", true, peerjArchivesAu, cus);
    shouldCacheTest(ROOT_URL+"articles/55.rdf", true, peerjArchivesAu, cus);
    shouldCacheTest(ROOT_URL+"articles/55.json", true, peerjArchivesAu, cus);
    shouldCacheTest(ROOT_URL+"articles/55.unixref", true, peerjArchivesAu, cus);
    // images figures and tables can live here
    // https://dfzljdn9uc3pi.cloudfront.net/2013/55/1/fig-1-2x.jpg
    // https://dfzljdn9uc3pi.cloudfront.net/2013/55/1/fig-1-full.png
    shouldCacheTest("https://dfzljdn9uc3pi.cloudfront.net"
    		    + "/2013/55/1/fig-1-2x.jpg", true, peerjArchivesAu, cus);   
    shouldCacheTest("https://dfzljdn9uc3pi.cloudfront.net"
                    + "/2013/55/1/fig-1-full.png", true, peerjArchivesAu, cus);   
    // missing peerj_site param - should not get crawled
    shouldCacheTest(ROOT_URL + "?year=2012", false, peerjArchivesAu, cus);  
    // LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, peerjArchivesAu, cus);
  }
  
    // Test the crawl rules for PeerJ Preprints site
  public void testShouldCacheProperPreprintsPages() throws Exception {
    theDaemon.getLockssRepository(peerjPreprintsAu);
    theDaemon.getNodeManager(peerjPreprintsAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(peerjPreprintsAu,
                               new RangeCachedUrlSetSpec(baseUrl.toString()));
    // Test for pages that should get crawled
    // permission page
    shouldCacheTest(ROOT_URL + "lockss.txt", true, peerjPreprintsAu, cus);  
    // volume page
    // https://peerj.com/archives-preprints/?year=2013
    shouldCacheTest(ROOT_URL + "xxxxPreprintsSite/?year=2013", 
                    true, peerjPreprintsAu, cus);  
    // toc page for an issue
    // https://peerj.com/preprints/index.html?month=2013-09
    shouldCacheTest(ROOT_URL + "preprints/index.html?month=2013-09", 
                    true, peerjPreprintsAu, cus);
    // preprints files
    // https://peerj.com/preprints/14/
    // https://peerj.com/preprints/14.bib
    // https://peerj.com/preprints/14.pdf
    // https://peerj.com/preprints/14.ris
    // https://peerj.com/preprints/14.xml
    // https://peerj.com/preprints/14.html
    // https://peerj.com/preprints/14.rdf
    // https://peerj.com/preprints/14.json
    shouldCacheTest(ROOT_URL+"preprints/14/", 
                    true, peerjPreprintsAu, cus);
    shouldCacheTest(ROOT_URL+"preprints/14.bib", 
                    true, peerjPreprintsAu, cus);
    shouldCacheTest(ROOT_URL+"preprints/14.pdf", 
                    true, peerjPreprintsAu, cus);
    shouldCacheTest(ROOT_URL+"preprints/14.ris", 
                    true, peerjPreprintsAu, cus);
    shouldCacheTest(ROOT_URL+"preprints/14.xml", 
                    true, peerjPreprintsAu, cus);
    shouldCacheTest(ROOT_URL+"preprints/14.html", 
                    true, peerjPreprintsAu, cus);
    shouldCacheTest(ROOT_URL+"preprints/14.rdf", 
                    true, peerjPreprintsAu, cus);
    shouldCacheTest(ROOT_URL+"preprints/14.json", 
                    true, peerjPreprintsAu, cus);
    // images figures and tables can live here
    // https://dfzljdn9uc3pi.cloudfront.net/2013/55/1/fig-1-2x.jpg
    // https://dfzljdn9uc3pi.cloudfront.net/2013/55/1/fig-1-full.png
    // https://d3amtssd1tejdt.cloudfront.net/2013/21/1/figure1.png
    // https://d2pdyyx74uypu5.cloudfront.net/lockss.txt
    shouldCacheTest("https://dfzljdn9uc3pi.cloudfront.net"
    		    + "/2013/55/1/fig-1-2x.jpg", true, peerjPreprintsAu, cus);   
    shouldCacheTest("https://dfzljdn9uc3pi.cloudfront.net"
                    + "/2013/55/1/fig-1-full.png", true, peerjPreprintsAu, cus);   
    shouldCacheTest("https://d3amtssd1tejdt.cloudfront.net"
                    + "/2013/21/1/figure1.png", true, peerjPreprintsAu, cus);   
    shouldCacheTest("https://d2pdyyx74uypu5.cloudfront.net/lockss.txt",
                    true, peerjPreprintsAu, cus);   
    // missing peerj_site param - should not get crawled
    shouldCacheTest(ROOT_URL + "?year=2012", false, peerjPreprintsAu, cus);  
    // LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, peerjPreprintsAu, cus);
  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
                               ArchivalUnit au, CachedUrlSet cus) {
    log.info ("shouldCacheTest url: " + url);

    UrlCacher uc = au.makeUrlCacher(url);
    assertEquals(shouldCache, uc.shouldBeCached());
  }
  
  public void testStartUrlConstruction() throws Exception {
    String expected = ROOT_URL + "xxxxSite/?year=2013";
    assertEquals(ListUtil.list(expected), peerjArchivesAu.getNewContentCrawlUrls());
  }
  
  public void testShouldNotCachePageFromOtherSite() throws Exception {
    theDaemon.getLockssRepository(peerjArchivesAu);
    theDaemon.getNodeManager(peerjArchivesAu);
    UrlCacher uc = peerjArchivesAu.makeUrlCacher(
        "http://shadow2.stanford.edu/xxxxSite/?year=2013");
    assertFalse(uc.shouldBeCached());
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(peerjArchivesAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlFor0() throws Exception {
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(peerjArchivesAu.shouldCrawlForNewContent(aus));
  }

  public void testgetName() throws Exception {
    DefinableArchivalUnit au1 =
      makeAu(new URL("http://www.example1.com/"), "2010", "xxxxSite1");
    assertEquals(PLUGIN_NAME + ", Base URL http://www.example1.com/, "
    		 + "PeerJ Site xxxxSite1, Volume 2010", au1.getName());
    DefinableArchivalUnit au2 =
      makeAu(new URL("http://www.example2.com/"), "2012", "xxxxSite2");
    assertEquals(PLUGIN_NAME + ", Base URL http://www.example2.com/, "
    		 + "PeerJ Site xxxxSite2, Volume 2012", au2.getName());
  }

}

