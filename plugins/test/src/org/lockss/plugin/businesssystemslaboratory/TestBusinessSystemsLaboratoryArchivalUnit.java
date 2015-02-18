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

package org.lockss.plugin.businesssystemslaboratory;

import java.net.URL;
import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.RangeCachedUrlSetSpec;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.state.AuState;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockAuState;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.*;

/* 
 * Tests archival unit for Business Systems Laboratory
 */

public class TestBusinessSystemsLaboratoryArchivalUnit extends LockssTestCase {
  
  private static Logger log = 
      Logger.getLogger(TestBusinessSystemsLaboratoryArchivalUnit.class);
  
  private MockLockssDaemon theDaemon;
  private DefinableArchivalUnit bslAu;
  private URL baseUrl;

  private static final String PLUGIN_ID = 
      "org.lockss.plugin."
        + "businesssystemslaboratory.BusinessSystemsLaboratoryPlugin";
  
  private static final String PLUGIN_NAME = 
      "Business Systems Laboratory Plugin";

  private static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  private static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  
  private static final String ROOT_URL = "http://www.example.com/";
  private static final String VOLUME_NAME = "2";  
  private static final String YEAR = "2013";  

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();

    baseUrl = new URL(ROOT_URL);
    bslAu = makeAu(baseUrl, VOLUME_NAME, YEAR);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(URL url, String volumeName, String year) 
      throws Exception {
 
    Properties props = new Properties();
    props.setProperty(VOL_KEY, volumeName);
    props.setProperty(YEAR_KEY, year);
    if (url != null) {
      props.setProperty(BASE_URL_KEY, url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    
    DefinablePlugin bslDefinablePlugin = new DefinablePlugin();
    bslDefinablePlugin.initPlugin(getMockLockssDaemon(), PLUGIN_ID);
    DefinableArchivalUnit au = 
        (DefinableArchivalUnit)bslDefinablePlugin.createAu(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, "2", "2013");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
  
  // Test the crawl rules for Business Systems Laboratory
  public void testShouldCacheProperPages() throws Exception {
    theDaemon.getLockssRepository(bslAu);
    theDaemon.getNodeManager(bslAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(bslAu,
                             new RangeCachedUrlSetSpec(baseUrl.toString()));
    // Test for pages that should get crawled
    // permission page / start url 
    // <bslbase>/bsr_archive.htm
    shouldCacheTest(ROOT_URL + "bsr_archive.htm", true, bslAu, cus);  
    // toc page for an issue - 
    // <bslbase>/BSR.2.1.January-June.2013.htm
    shouldCacheTest(ROOT_URL + "BSR." + VOLUME_NAME + ".1.January-June."
                    + YEAR + ".htm", true, bslAu, cus);
    // article files
    // <bslbase>/Bardy.&.Massaro.(2013).Sustainability.Value.Index.2.1.htm
    // <bslbase>/BSR.Vol.2-Iss.1-Massaro.et.al.Organising.Innovation.pdf
    // <bslbase>/BSR-Vol.2-Iss.1-2013-complete-issue.pdf
    // <bslbase>/BSR.Vol.2-Iss.2-Symposium.Valencia.2013.Complete.Issue.pdf
    // <bslbase>/Business_Systems_Review-Vol.2-Issue3-2013-full-issue.pdf
    shouldCacheTest(ROOT_URL + "Bardy.&.Massaro.(2013).Sustainability."
                    + "Value.Index.2.1.htm", true, bslAu, cus);
    shouldCacheTest(ROOT_URL + "BSR.Vol.2-Iss.1-Massaro.et.al.Organising."
                    + "Innovation.pdf", true, bslAu, cus);
    shouldCacheTest(ROOT_URL + "BSR-Vol.2-Iss.1-2013-complete-issue.pdf", 
                    true, bslAu, cus);
    shouldCacheTest(ROOT_URL + "BSR.Vol.2-Iss.2-Symposium.Valencia.2013."
                    + "Complete.Issue.pdf", true, bslAu, cus);
    shouldCacheTest(ROOT_URL + "Business_Systems_Review-Vol.2-Issue3-2013-"
                    + "full-issue.pdf", true, bslAu, cus);
    
    // should not get crawled - missing volume name and year
    shouldCacheTest(ROOT_URL + "BSR.January-June.htm", false, bslAu, cus);  
    // should not get crawled - LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, bslAu, cus);
  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
                               ArchivalUnit au, CachedUrlSet cus) {
    log.info ("shouldCacheTest url: " + url);
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
  public void testStartUrlConstruction() throws Exception {
    String expected = ROOT_URL + "bsr_archive.htm";
    assertEquals(ListUtil.list(expected), 
                 bslAu.getStartUrls());
  }
  
  public void testShouldNotCachePageFromOtherSite() throws Exception {
    theDaemon.getLockssRepository(bslAu);
    theDaemon.getNodeManager(bslAu);
    assertFalse(bslAu.shouldBeCached(
        "http://shadow2.stanford.edu/bsr_archive.htm"));
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(bslAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlFor0() throws Exception {
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(bslAu.shouldCrawlForNewContent(aus));
  }

  public void testgetName() throws Exception {
    DefinableArchivalUnit au1 =
      makeAu(new URL("http://www.example1.com/"), "5", "2010");
    assertEquals(PLUGIN_NAME + ", Base URL http://www.example1.com/,"
                 + " Volume 5, Year 2010", au1.getName());
    DefinableArchivalUnit au2 =
      makeAu(new URL("http://www.example2.com/"), "8", "2008");
    assertEquals(PLUGIN_NAME + ", Base URL http://www.example2.com/,"
                 + " Volume 8, Year 2008", au2.getName());
  }
  
  public void testHtmlLinkExtractor() throws Exception {
    assertTrue(bslAu.getLinkExtractor(Constants.MIME_TYPE_HTML) instanceof GoslingHtmlLinkExtractor);
  }

}

