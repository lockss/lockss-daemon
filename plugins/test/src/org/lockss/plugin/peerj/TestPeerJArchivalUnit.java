/*
 * $Id: TestPeerJArchivalUnit.java,v 1.4 2014-10-11 00:44:52 ldoan Exp $
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

package org.lockss.plugin.peerj;

import java.net.URL;
import java.util.Properties;
import junit.framework.Test;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.RangeCachedUrlSetSpec;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.plugin.PluginManager;
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
  
  String variantPluginId;
  String variantPluginName;
  DefinableArchivalUnit variantPeerjAu;
  String variantPeerjSite;
  String variantBaseConstant;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;
  URL baseUrl;
  
  private static Logger log = Logger.getLogger(TestPeerJArchivalUnit.class);

  private static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  private static final String PEERJ_SITE_KEY = "peerj_site";
  private static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private static final String ROOT_URL = "http://www.example.com/";
  private static final String VOLUME_NAME = "2013";

  public DefinableArchivalUnit createAu(URL url, String volume, 
      String peerjSite, String pluginId) throws Exception {
    Properties props = new Properties();
    props.setProperty(VOL_KEY, volume);
    props.setProperty(PEERJ_SITE_KEY, peerjSite);
    if (url != null) {
      props.setProperty(BASE_URL_KEY, url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    DefinablePlugin peerjDefinablePlugin = new DefinablePlugin();
    peerjDefinablePlugin.initPlugin(getMockLockssDaemon(), pluginId);
    DefinableArchivalUnit au = 
        (DefinableArchivalUnit)peerjDefinablePlugin.createAu(config);
    return au;
  }
  
  // Test the crawl rules for PeerJ Archives site
  public void testShouldCacheProperPages(DefinableArchivalUnit au,
      String peerjSite, URL baseUrl, String baseConstant) throws Exception {
    daemon.getLockssRepository(au);
    daemon.getNodeManager(au);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(au,
        new RangeCachedUrlSetSpec(baseUrl.toString()));
    // permission page
    shouldCacheTest(ROOT_URL + "lockss.txt", true, au, cus);  
    // volume page - https://peerj.com/archives/?year=2013
    shouldCacheTest(ROOT_URL + peerjSite + "/?year=2013", true, au, cus);  
    // issue toc - https://peerj.com/articles/index.html?month=2013-09
    shouldCacheTest(ROOT_URL + baseConstant + "/index.html?month=2013-09", 
                    true, au, cus);
    // article from Archives site - https://peerj.com/articles/55/
    //   other files: .bib, .pdf, .ris, .xml, .rdf, .json, .unixref, /reviews
    // article from Preprints site - https://peerj.com/preprints/55/
    //   other files: .bib, .pdf, .ris, .xml, .rdf, .json
    shouldCacheTest(ROOT_URL + baseConstant + "/55/", true, au, cus);
    shouldCacheTest(ROOT_URL + baseConstant + "/55.bib", true, au, cus);
    shouldCacheTest(ROOT_URL + baseConstant + "/55.pdf", true, au, cus);
    shouldCacheTest(ROOT_URL + baseConstant + "/55.ris", true, au, cus);
    shouldCacheTest(ROOT_URL + baseConstant + "/55.xml", true, au, cus);
    shouldCacheTest(ROOT_URL + baseConstant + "/55.html", true, au, cus);
    shouldCacheTest(ROOT_URL + baseConstant + "/55.rdf", true, au, cus);
    shouldCacheTest(ROOT_URL + baseConstant + "/55.json", true, au, cus);
    // not exists on Preprints site
    if (baseConstant == "archives") {
      shouldCacheTest(ROOT_URL + baseConstant + "/55.unixref", true, au, cus);
      shouldCacheTest(ROOT_URL + baseConstant + "/55/reviews/", true, au, cus);
    }
    // images figures and tables can live here
    // https://dfzljdn9uc3pi.cloudfront.net/2013/55/1/fig-1-2x.jpg
    // https://dfzljdn9uc3pi.cloudfront.net/2013/55/1/fig-1-full.png
    // https://d3amtssd1tejdt.cloudfront.net/2013/21/1/figure1.png
    // https://d2pdyyx74uypu5.cloudfront.net/2013/22/2/figure2.jpg
    shouldCacheTest("https://dfzljdn9uc3pi.cloudfront.net"
    		    + "/2013/55/1/fig-1-2x.jpg", true, au, cus);   
    shouldCacheTest("https://dfzljdn9uc3pi.cloudfront.net"
                    + "/2013/55/1/fig-1-full.png", true, au, cus);   
    shouldCacheTest("https://d3amtssd1tejdt.cloudfront.net"
                    + "/2013/21/1/figure1.png", true, au, cus);
    shouldCacheTest("https://d2pdyyx74uypu5.cloudfront.net"
                   + "/2013/22/2/figure2.jpg", true, au, cus);   
    // missing peerj_site param - should not get crawled
    shouldCacheTest(ROOT_URL + "?year=2012", false, au, cus);  
    // LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, au, cus);
  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
      ArchivalUnit au, CachedUrlSet cus) {
    //log.info ("shouldCacheTest url: " + url);
    UrlCacher uc = au.makeUrlCacher(url);
    assertEquals(shouldCache, uc.shouldBeCached());
  }
  
  public void testStartUrlConstruction(DefinableArchivalUnit au,
      String peerjSite) throws Exception {
    String expected = ROOT_URL + peerjSite + "/?year=2013";
    assertEquals(ListUtil.list(expected), au.getNewContentCrawlUrls());
  }
  
  public void testShouldNotCachePageFromOtherSite(DefinableArchivalUnit au,
      String peerjSite) throws Exception {
    daemon.getLockssRepository(au);
    daemon.getNodeManager(au);
    UrlCacher uc = au.makeUrlCacher("http://shadow2.stanford.edu/" +
    		peerjSite + "/?year=2013");
    assertFalse(uc.shouldBeCached());
  }

  public void testShouldDoNewContentCrawlTooEarly(DefinableArchivalUnit au) 
      throws Exception {
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(au.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlFor0(DefinableArchivalUnit au)
      throws Exception {
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(au.shouldCrawlForNewContent(aus));
  }
  
  public void testConstructNullUrl(String volume, String peerjSite, 
      String pluginId) throws Exception {
    try {
      createAu(null, volume, peerjSite, pluginId);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }

  public void testGetName(String baseUrl, String volume, String peerjSite, 
      String pluginId, String pluginName) throws Exception {
    DefinableArchivalUnit au = createAu(
        new URL(baseUrl), volume, peerjSite, pluginId);
    assertEquals(pluginName + ", Base URL " + baseUrl + ", PeerJ Site " +
        peerjSite + ", Volume " + volume, au.getName());
  }
  
  public void startMockDaemon() {
    daemon = getMockLockssDaemon();
    pluginMgr = daemon.getPluginManager();
    pluginMgr.setLoadablePluginsReady(true);
    daemon.setDaemonInited(true);
    pluginMgr.startService();
    daemon.getAlertManager();
    daemon.getCrawlManager();
  }
          		
  // Variant to test PeerJ Archives site
  public static class TestArchives extends TestPeerJArchivalUnit {   
    public void setUp() throws Exception {
      super.setUp();
      tempDirPath = setUpDiskSpace();
      startMockDaemon();
      variantPluginName = "PeerJ Plugin";
      variantPluginId =  "org.lockss.plugin.peerj.PeerJPlugin";
      variantPeerjSite = "xxxxArchives";
      variantBaseConstant = "articles";
      variantPeerjAu = createAu(new URL("http://www.example.com/"), 
          VOLUME_NAME, variantPeerjSite, variantPluginId);
      baseUrl = new URL(ROOT_URL);
    }
    public void testArchivalUnit() throws Exception {
      testGetName("http://www.example1.com/", "2010", variantPeerjSite, 
          variantPluginId, variantPluginName);
      testConstructNullUrl("2011", "peerjSiteBad", variantPluginId);
      testShouldDoNewContentCrawlFor0(variantPeerjAu);
      testShouldDoNewContentCrawlTooEarly(variantPeerjAu);
      testShouldNotCachePageFromOtherSite(variantPeerjAu, variantPeerjSite);
      testStartUrlConstruction(variantPeerjAu, variantPeerjSite);
      testShouldCacheProperPages(variantPeerjAu, variantPeerjSite, baseUrl, 
          variantBaseConstant);
    }
  }
  
  // Variant to test PeerJ Preprints site
  public static class TestPreprints extends TestPeerJArchivalUnit {   
    public void setUp() throws Exception {
      super.setUp();
      tempDirPath = setUpDiskSpace();
      startMockDaemon();
      variantPluginName = "PeerJ Preprints Plugin (CLOCKSS)";
      variantPluginId =  "org.lockss.plugin.peerj.ClockssPeerJPreprintsPlugin";
      variantPeerjSite = "xxxxPreprints";
      variantBaseConstant = "preprints";
      variantPeerjAu = createAu(new URL("http://www.example.com/"), 
          VOLUME_NAME, variantPeerjSite, variantPluginId);
      baseUrl = new URL(ROOT_URL);
    }
    public void testArchivalUnit() throws Exception {
      testGetName("http://www.example1.com/", "2010", variantPeerjSite, 
          variantPluginId, variantPluginName);
      testConstructNullUrl("2011", "peerjSiteBad", variantPluginId);
      testShouldDoNewContentCrawlFor0(variantPeerjAu);
      testShouldDoNewContentCrawlTooEarly(variantPeerjAu);
      testShouldNotCachePageFromOtherSite(variantPeerjAu, variantPeerjSite);
      testStartUrlConstruction(variantPeerjAu, variantPeerjSite);
      testShouldCacheProperPages(variantPeerjAu, variantPeerjSite, baseUrl, 
          variantBaseConstant);
    }
  }
  
  public static Test suite() {
    return variantSuites(new Class[] {
        TestArchives.class,
        TestPreprints.class
    });
  }
  
}

