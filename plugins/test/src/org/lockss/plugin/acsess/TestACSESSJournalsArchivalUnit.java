/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.acsess;

import java.net.URL;
import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.RangeCachedUrlSetSpec;
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
import org.lockss.util.ListUtil;
import org.lockss.util.TimeBase;

public class TestACSESSJournalsArchivalUnit  extends LockssTestCase {

  private MockLockssDaemon theDaemon;
  private DefinableArchivalUnit aau;
  private URL baseUrl;
  private static final String PLUGIN_ID = 
      "org.lockss.plugin.acsess.ClockssACSESSJournalsPlugin";
  private static final String PLUGIN_NAME = 
      "Alliance of Crop, Soil, and Environmental Science Societies Journals Plugin (CLOCKSS)";
  private static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  private static final String JID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  private static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private static final String ROOT_URL = "https://www.example.com/";
  private static final String JID = "xxxjid";  
  private static final String VOL = "106";

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
    baseUrl = new URL(ROOT_URL);
    aau = makeAu(baseUrl, JID, VOL);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(URL url, String jid, String volume) 
      throws Exception {
    Properties props = new Properties();
    props.setProperty(JID_KEY, jid);
    props.setProperty(VOL_KEY, volume);
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
      makeAu(null, "aaa", "999");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
  
  // Test the crawl rules for National Weather Association
  public void testShouldCacheProperPages() throws Exception {
    theDaemon.getLockssRepository(aau);
    theDaemon.getNodeManager(aau);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(
                             aau,
                             new RangeCachedUrlSetSpec(baseUrl.toString()));
    // Test for pages that should get crawled
    // manifest/permission/start url page
    // https://dl.sciencesocieties.org/publications/aj/tocs/103
    shouldCacheTest(ROOT_URL + "publications/" + JID + "/tocs/" + VOL, true, aau, cus);
    // toc page for an issue
    // https://dl.sciencesocieties.org/publications/aj/tocs/103/1
    shouldCacheTest(ROOT_URL + "publications/" + JID + "/tocs/" + VOL + "/1", true, aau, cus);
    // article files:
    //  abs: https://dl.sciencesocieties.org/publications/aj/abstracts/106/1/57
    //  preview pdf (abs 2): https://dl.sciencesocieties.org/publications/cns/abstracts/47/1/20/preview
    //  html full text: https://dl.sciencesocieties.org/publications/aj/articles/106/1/57
    //  pdf: https://dl.sciencesocieties.org/publications/aj/pdfs/106/1/57
    //  tables only: https://dl.sciencesocieties.org/publications/aj/articles/106/1/57?show-t-f=tables&wrapper=no
    //  figures only: https://dl.sciencesocieties.org/publications/aj/articles/106/1/57?show-t-f=figures&wrapper=no
    shouldCacheTest(ROOT_URL + "publications/" + JID + "/abstracts/" + VOL + "/1/57", true, aau, cus);
    shouldCacheTest(ROOT_URL + "publications/" + JID + "/abstracts/" + VOL + "/1/57/preview", true, aau, cus);
    shouldCacheTest(ROOT_URL + "publications/" + JID + "/articles/" + VOL + "/1/57", true, aau, cus);
    shouldCacheTest(ROOT_URL + "publications/" + JID + "/pdfs/" + VOL + "/1/57", true, aau, cus);
    shouldCacheTest(ROOT_URL + "publications/" + JID + "/articles/" + VOL + "/1/57?show-t-f=tables&wrapper=no", true, aau, cus);
    shouldCacheTest(ROOT_URL + "publications/" + JID + "/articles/" + VOL + "/1/57?show-t-f=figures&wrapper=no", true, aau, cus);
    // citation files:    
    //  EndNote: https://dl.sciencesocieties.org/publications/citation-manager/down/en/aj/106/5/1677
    //  ProCite Ris: https://dl.sciencesocieties.org/publications/citation-manager/down/pc/aj/106/5/1677
    //  Zotero Ris: https://dl.sciencesocieties.org/publications/citation-manager/down/zt/aj/106/5/1677
    //  MARC: https://dl.sciencesocieties.org/publications/citation-manager/down/marc/aj/106/5/1677
    //  RefWorks: https://dl.sciencesocieties.org/publications/citation-manager/down/refworks/aj/106/5/1677
   shouldCacheTest(ROOT_URL + "publications/citation-manager/down/en/" + JID + "/" + VOL + "/1/57", true, aau, cus);
   shouldCacheTest(ROOT_URL + "publications/citation-manager/down/pc/" + JID + "/" + VOL + "/1/57", true, aau, cus);
   shouldCacheTest(ROOT_URL + "publications/citation-manager/down/zt/" + JID + "/" + VOL + "/1/57", true, aau, cus);
   shouldCacheTest(ROOT_URL + "publications/citation-manager/down/marc/" + JID + "/" + VOL + "/1/57", true, aau, cus);
   shouldCacheTest(ROOT_URL + "publications/citation-manager/down/refworks/" + JID + "/" + VOL + "/1/57", true, aau, cus);
   // should not get crawled
   // missing journal id 
   // abs: https://dl.sciencesocieties.org/publications/aj/abstracts/106/1/57
   shouldCacheTest(ROOT_URL + "publications/abstracts/" + VOL + "/1/57", false, aau, cus); 
    // LOCKSS
    shouldCacheTest("https://lockss.stanford.edu", false, aau, cus);
  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
                               ArchivalUnit au, CachedUrlSet cus) {
    log.info ("shouldCacheTest url: " + url);
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
  // https://dl.sciencesocieties.org/publications/aj/tocs/103
  // shouldCacheTest(ROOT_URL + "publications/" + JID + "/tocs/103", true, aau, cus);
  public void testStartUrlConstruction() throws Exception {
    String expected = ROOT_URL + "publications/" + JID + "/tocs/" + VOL;
    assertEquals(ListUtil.list(expected), aau.getStartUrls());
  }
  
  public void testShouldNotCachePageFromOtherSite() throws Exception {
    theDaemon.getLockssRepository(aau);
    theDaemon.getNodeManager(aau);
    assertFalse(aau.shouldBeCached(
        "https://shadow2.stanford.edu/publications" + JID + "/tocs/" + VOL));
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(aau.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlFor0() throws Exception {
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(aau.shouldCrawlForNewContent(aus));
  }

  // test au_name:
  // Alliance of Crop, Soil, and Environmental Science Societies Journals Plugin,
  //    Base URL %s, Journal ID %s, Volume %s", base_url, journal_id, volume_name
  public void testgetName() throws Exception {
    DefinableArchivalUnit au1 =
      makeAu(new URL("https://www.example1.com/"), "aaajid", "000");
    assertEquals(PLUGIN_NAME + ", Base URL https://www.example1.com/"
                 + ", Journal ID aaajid, Volume 000", au1.getName());
    DefinableArchivalUnit au2 =
      makeAu(new URL("https://www.example2.com/"), "bbbjid", "999");
    assertEquals(PLUGIN_NAME + ", Base URL https://www.example2.com/"
                 + ", Journal ID bbbjid, Volume 999", au2.getName());
  }

}

