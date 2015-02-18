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
  
  private static Logger log = Logger.getLogger(TestPeerJArchivalUnit.class);

  private static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  private static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private static final String BASE_URL = "http://www.example.com/";
  private static final String VOLUME_NAME = "2013";

  public DefinableArchivalUnit createAu(URL url, String volume, 
      String pluginId) throws Exception {
    Properties props = new Properties();
    props.setProperty(VOL_KEY, volume);
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
  
  // Test the crawl rules for PeerJ Archives (main) site
  public void testShouldCacheProperPages(DefinableArchivalUnit au, 
      String peerjSite, String baseConstant) throws Exception {
    daemon.getLockssRepository(au);
    daemon.getNodeManager(au);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(au,
        new RangeCachedUrlSetSpec(BASE_URL));
    // permission page
    shouldCacheTest(BASE_URL + "lockss.txt", true, au, cus);  
    // volume page - https://peerj.com/archives/?year=2013
    // volume page - https://peerj.com/archives-preprints/?year=2013
    shouldCacheTest(BASE_URL + peerjSite + "/?year=2013", true, au, cus);  
    // issue toc - https://peerj.com/articles/index.html?month=2013-09
    shouldCacheTest(BASE_URL + baseConstant + "/index.html?month=2013-09", 
                    true, au, cus);
    // article from Archives (main) site - https://peerj.com/articles/55/
    //   other files: .bib, .pdf, .ris, .xml, .rdf, .json, .unixref, /reviews
    // article from Preprints site - https://peerj.com/preprints/55/
    //   other files: .bib, .pdf, .ris, .xml, .rdf, .json
    shouldCacheTest(BASE_URL + baseConstant + "/55/", true, au, cus);
    shouldCacheTest(BASE_URL + baseConstant + "/55.bib", true, au, cus);
    shouldCacheTest(BASE_URL + baseConstant + "/55.pdf", true, au, cus);
    shouldCacheTest(BASE_URL + baseConstant + "/55.ris", true, au, cus);
    shouldCacheTest(BASE_URL + baseConstant + "/55.xml", true, au, cus);
    shouldCacheTest(BASE_URL + baseConstant + "/55.html", true, au, cus);
    shouldCacheTest(BASE_URL + baseConstant + "/55.rdf", true, au, cus);
    shouldCacheTest(BASE_URL + baseConstant + "/55.json", true, au, cus);
    // not exists on Preprints site
    if (baseConstant == "archives") {
      shouldCacheTest(BASE_URL + baseConstant + "/55.unixref", true, au, cus);
      shouldCacheTest(BASE_URL + baseConstant + "/55/reviews/", true, au, cus);
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
    // missing peerj site string - should not get crawled
    shouldCacheTest(BASE_URL + "?year=2012", false, au, cus);  
    // LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, au, cus);
  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
      ArchivalUnit au, CachedUrlSet cus) {
    //log.info ("shouldCacheTest url: " + url);
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
  public void testStartUrlConstruction(DefinableArchivalUnit au,
      String peerjSite) throws Exception {
    String expected = BASE_URL + peerjSite + "/?year=2013";
    assertEquals(ListUtil.list(expected), au.getStartUrls());
  }
  
  public void testShouldNotCachePageFromOtherSite(DefinableArchivalUnit au,
      String peerjSite) throws Exception {
    daemon.getLockssRepository(au);
    daemon.getNodeManager(au);
    assertFalse(au.shouldBeCached("http://shadow2.stanford.edu/" +
        peerjSite + "/?year=2013"));
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
  
  public void testConstructNullUrl(String volume, String pluginId) 
      throws Exception {
    try {
      createAu(null, volume, pluginId);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }

  public void testGetName(String url, String volume, String pluginId, 
      String pluginName) throws Exception {
    DefinableArchivalUnit au = createAu(new URL(url), volume, pluginId);
    assertEquals(pluginName + ", Base URL " + url + ", Volume " +
        volume, au.getName());
  }
  
  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = setUpDiskSpace();
    daemon = getMockLockssDaemon();
    pluginMgr = daemon.getPluginManager();
    pluginMgr.setLoadablePluginsReady(true);
    daemon.setDaemonInited(true);
    pluginMgr.startService();
    daemon.getAlertManager();
    daemon.getCrawlManager();
  }

  // Variant to test PeerJ Archives (main) site
  public static class TestArchives extends TestPeerJArchivalUnit {   
    public void testArchivalUnit() throws Exception {
      variantPluginName = "PeerJ Plugin";
      variantPluginId =  "org.lockss.plugin.peerj.PeerJPlugin";
      variantPeerjSite = "archives";
      variantBaseConstant = "articles";
      variantPeerjAu = createAu(new URL("http://www.example.com/"), 
        VOLUME_NAME, variantPluginId);
      
      testGetName("http://www.example1.com/", "2010", variantPluginId, 
          variantPluginName);
      testConstructNullUrl("2011", variantPluginId);
      testShouldDoNewContentCrawlFor0(variantPeerjAu);
      testShouldDoNewContentCrawlTooEarly(variantPeerjAu);
      testShouldNotCachePageFromOtherSite(variantPeerjAu, variantPeerjSite);
      testStartUrlConstruction(variantPeerjAu, variantPeerjSite);
      testShouldCacheProperPages(variantPeerjAu, variantPeerjSite,
          variantBaseConstant);
    }
  }
  
  // Variant to test PeerJ Preprints site
  public static class TestPreprints extends TestPeerJArchivalUnit {   
    public void testArchivalUnit() throws Exception {
      variantPluginName = "PeerJ Preprints Plugin (CLOCKSS)";
      variantPluginId =  "org.lockss.plugin.peerj.ClockssPeerJPreprintsPlugin";
      variantPeerjSite = "archives-preprints";
      variantBaseConstant = "preprints";
      variantPeerjAu = createAu(new URL("http://www.example.com/"), 
          VOLUME_NAME, variantPluginId);
      
      testGetName("http://www.example1.com/", "2010", variantPluginId, 
          variantPluginName);
      testConstructNullUrl("2011", variantPluginId);
      testShouldDoNewContentCrawlFor0(variantPeerjAu);
      testShouldDoNewContentCrawlTooEarly(variantPeerjAu);
      testShouldNotCachePageFromOtherSite(variantPeerjAu, variantPeerjSite);
      testStartUrlConstruction(variantPeerjAu, variantPeerjSite);
      testShouldCacheProperPages(variantPeerjAu, variantPeerjSite,
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

