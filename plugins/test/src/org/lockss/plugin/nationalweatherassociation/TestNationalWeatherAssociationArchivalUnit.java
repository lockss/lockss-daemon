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

package org.lockss.plugin.nationalweatherassociation;

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

public class TestNationalWeatherAssociationArchivalUnit 
  extends LockssTestCase {
  
  private static Logger log = 
      Logger.getLogger(TestNationalWeatherAssociationArchivalUnit.class);
  
  private MockLockssDaemon theDaemon;
  private DefinableArchivalUnit nwaAu;
  private URL baseUrl;

  private static final String PLUGIN_ID = "org.lockss.plugin."
      + "nationalweatherassociation.NationalWeatherAssociationPlugin";
  
  private static final String PLUGIN_NAME = 
      "National Weather Association Plugin";

  private static final String BASE_URL_KEY = 
      ConfigParamDescr.BASE_URL.getKey();
  private static final String JOURNAL_ID_KEY = 
      ConfigParamDescr.JOURNAL_ID.getKey();
  private static final String YEAR_KEY = 
      ConfigParamDescr.YEAR.getKey();
  
  private static final String ROOT_URL = "http://www.example.com/";
  private static final String JID = "xxxjid";  
  private static final String YR = "2013";  

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();

    baseUrl = new URL(ROOT_URL);
    nwaAu = makeAu(baseUrl, JID, YR);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(URL url, String jid, String year) 
      throws Exception {
 
    Properties props = new Properties();
    props.setProperty(JOURNAL_ID_KEY, jid);
    props.setProperty(YEAR_KEY, year);
    if (url != null) {
      props.setProperty(BASE_URL_KEY, url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    
    DefinablePlugin nwaDefinablePlugin = new DefinablePlugin();
    nwaDefinablePlugin.initPlugin(getMockLockssDaemon(), PLUGIN_ID);
    DefinableArchivalUnit au = 
        (DefinableArchivalUnit)nwaDefinablePlugin.createAu(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, "aaa", "2013");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
  
  // Test the crawl rules for National Weather Association
  public void testShouldCacheProperPages() throws Exception {
    theDaemon.getLockssRepository(nwaAu);
    theDaemon.getNodeManager(nwaAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(nwaAu,
                             new RangeCachedUrlSetSpec(baseUrl.toString()));
    // Test for pages that should get crawled
    // permission page / start url, same as volume/year page, 
    // this journal does not have issue toc page
    // the volume page lists all the articles for the year/volume
    // <nwabase>.org/<journal_id>/include/publications2013.php
    shouldCacheTest(ROOT_URL + JID + "/include/publications" + YR + ".php", 
                    true, nwaAu, cus);  

    // article files
    // <nwabase>.org/<journal_id>/abstracts/2013/2013-JOM22/abstract.php
    // <nwabase>.org/<journal_id>/articles/2013/2013-JOM12/2013-JOM12.pdf
    shouldCacheTest(ROOT_URL + JID + "/abstracts/" + YR + "/" + YR + 
        "-XXXJID22/abstract.php", true, nwaAu, cus);
    shouldCacheTest(ROOT_URL + JID + "/articles/" + YR + "/" + YR + 
        "-XXXJID12/" + YR + "-XXXJID12.pdf", true, nwaAu, cus);
    // images figures and tables can live under
    // <nwabase>.org/Img/*
    shouldCacheTest(ROOT_URL + "Img/cycle.gif", 
                    true, nwaAu, cus);
        
    // should not get crawled - missing journal id 
    shouldCacheTest(ROOT_URL + "/include/publications" + YR + ".php",
                    false, nwaAu, cus);  
 
    // LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", 
                    false, nwaAu, cus);
  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
                               ArchivalUnit au, CachedUrlSet cus) {
    log.info ("shouldCacheTest url: " + url);
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
  public void testStartUrlConstruction() throws Exception {
    String expected = ROOT_URL + JID + "/include/publications" + YR + ".php";
    assertEquals(ListUtil.list(expected), nwaAu.getStartUrls());
  }
  
  public void testShouldNotCachePageFromOtherSite() throws Exception {
    theDaemon.getLockssRepository(nwaAu);
    theDaemon.getNodeManager(nwaAu);
    assertFalse(nwaAu.shouldBeCached("http://shadow2.stanford.edu/"
        + JID + "/include/publications" + YR + ".php"));
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(nwaAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlFor0() throws Exception {
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(nwaAu.shouldCrawlForNewContent(aus));
  }

  public void testgetName() throws Exception {
    DefinableArchivalUnit au1 =
      makeAu(new URL("http://www.example1.com/"), "aaajid", "2010");
    assertEquals(PLUGIN_NAME + ", Base URL http://www.example1.com/"
                 + ", Journal ID aaajid, Year 2010", au1.getName());
    DefinableArchivalUnit au2 =
      makeAu(new URL("http://www.example2.com/"), "bbbjid", "2012");
    assertEquals(PLUGIN_NAME + ", Base URL http://www.example2.com/"
                 + ", Journal ID bbbjid, Year 2012", au2.getName());
  }

}

