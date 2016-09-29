/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.metapress;


import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.RangeCachedUrlSetSpec;
import org.lockss.plugin.*;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.state.AuState;
import org.lockss.test.*;
import org.lockss.util.Constants;
import org.lockss.util.ListUtil;
import org.lockss.util.PatternFloatMap;
import org.lockss.util.RegexpUtil;
import org.lockss.util.TimeBase;

public class TestMetapressPlugin extends LockssPluginTestCase {
  
  protected MockLockssDaemon daemon;
  private final String PLUGIN_NAME = "org.lockss.plugin.metapress.ClockssMetaPressPlugin";
  private final String BASE_URL = "http://uksg.metapress.com/";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ISSN_KEY = ConfigParamDescr.JOURNAL_ISSN.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private final String VOLUME = "21";
  private final String JOURNAL_ISSN = "1546-2234";
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
      BASE_URL_KEY, BASE_URL,
      VOLUME_NAME_KEY, VOLUME,
      JOURNAL_ISSN_KEY, JOURNAL_ISSN);
  
  static final String  METAPRESS_REPAIR_FROM_PEER_REGEXP = "/dynamic-file[.]axd[?]id=";

  public void setUp() throws Exception {
    super.setUp();
  }
  
  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return
        createAu(AU_CONFIG);
  }
  
  protected ArchivalUnit createAu(Configuration config) throws ArchivalUnit.ConfigurationException {
    return
        PluginTestUtil.createAndStartAu(PLUGIN_NAME, config);
  }
  
  public void testCreateAu() {
    
    try {
      createAu(ConfigurationUtil.fromArgs(
          BASE_URL_KEY, BASE_URL,
          VOLUME_NAME_KEY, VOLUME));
      fail("Bad AU configuration should throw configuration exception");
    }
    catch (ConfigurationException ex) {
    }
    try {
      createAu();
    }
    catch (ConfigurationException ex) {
      fail("Unable to create AU from valid configuration");
    }
  }
  
  public void testShouldCacheProperPages() throws Exception {
    ArchivalUnit au = createAu();
    BaseCachedUrlSet cus = new BaseCachedUrlSet(au, new RangeCachedUrlSetSpec(BASE_URL));
    
    // start page
    assertShouldCache(BASE_URL + "openurl.asp?genre=volume&eissn=" + JOURNAL_ISSN + "&volume=" + VOLUME, true, au, cus);
    
    assertShouldCache(BASE_URL + "openurl.asp?genre=volume&eissn=" + JOURNAL_ISSN + "&volume=99", false, au, cus);
    
    // issue and article pages
    assertShouldCache(BASE_URL + "content/rt0gdjk28uhh", true, au, cus);
    assertShouldCache(BASE_URL + "content/rt0gdjk28uhh/?p_o=10", true, au, cus);
    assertShouldCache(BASE_URL + "content/rt0gdjk28uhh/?print=true", true, au, cus);
    
    assertShouldCache(BASE_URL + "content/rt0gdjk28uhh/?sortorder=foo&p_o=0", false, au, cus);
    assertShouldCache(BASE_URL + "content/rt0gdjk28uhh/?sortorder=foo&print=true", false, au, cus);
    assertShouldCache(BASE_URL + "content/tkhv5whb5mlq8cdp/content/h755877p17/?export=rss", false, au, cus);
    assertShouldCache(BASE_URL + "content/tkhv5whb5mlq8cdp/content/m4882271nv33/Article+Category=Editorial&export=rss&v=condensed", false, au, cus);

    // html/pdf pages
    assertShouldCache(BASE_URL + "content/tkhv5whb5mlq8cdp", true, au, cus);
    assertShouldCache(BASE_URL + "content/tkhv5whb5mlq8cdp/?print=true", true, au, cus);
    assertShouldCache(BASE_URL + "content/tkhv5whb5mlq8cdp/fulltext.pdf", true, au, cus);
    assertShouldCache(BASE_URL + "dynamic-file.axd?id=82060661-bf24-464e-9256-98cd87e0f38a&m=True", true, au, cus);
    
    assertShouldCache(BASE_URL + "export.mpx?code=0262087P5LL2W052&mode=ris", true, au, cus);
    assertShouldCache(BASE_URL + "export.mpx?code=163W8295619W7180&mode=txt", true, au, cus);
    
    // images, css, js
    assertShouldCache(BASE_URL + "content/2102k5223007m657/dec1013_IJRIS-36896_pg_0001.jpg", true, au, cus);
    assertShouldCache(BASE_URL + "ajaxpro/core.ashx", true, au, cus);
    assertShouldCache(BASE_URL + "content/121476/cover-medium.jpg", true, au, cus);
    assertShouldCache(BASE_URL + "favicon.ico", true, au, cus);
    assertShouldCache(BASE_URL + "images/sprites.gif", true, au, cus);
    assertShouldCache(BASE_URL + "jsMath/plugins/noImageFonts.js", true, au, cus);
    assertShouldCache(BASE_URL + "ajaxpro/MetaPress.Products.Reader.Application.ashx", true, au, cus);
    
    
    // images, css, js with bad formatting
    assertShouldCache(BASE_URL + "ajaxpro/MetaPress.Products.Reader.Application.ashx?param=foo", false, au, cus);
    assertShouldCache(BASE_URL + "App_Themes/HeatherStyles/IGIMain.css?v=02242012b", false, au, cus);
    assertShouldCache(BASE_URL + "Scripts/gateway.js?v=02162012", false, au, cus);
    
    // specified bad pages
    assertShouldCache(BASE_URL + "content/tkhv5whb5mlq8cdp?referencesMode=Show&print=true", false, au, cus);
    assertShouldCache(BASE_URL + "content/tkhv5whb5mlq8cdp/offerings/", false, au, cus);
    assertShouldCache(BASE_URL + "content/tkhv5whb5mlq8cdp/fulltext.pdf?page=1", false, au, cus);
    
    // facebook
    assertShouldCache("http://www.facebook.com/pages/138206739534176?ref=sgm", false, au, cus);
    
    // LOCKSS
    assertShouldCache("http://lockss.stanford.edu", false, au, cus);
    
    // other site
    assertShouldCache("http://exo.com/~noid/ConspiracyNet/satan.html", false, au, cus);
    
  }
  
  private void assertShouldCache(String url, boolean shouldCache,
      ArchivalUnit au, CachedUrlSet cus) {
    assertEquals("AU crawl rules applied incorrectly to " + url + " ",
        shouldCache, au.shouldBeCached(url));
  }
  
  public void testStartUrlConstruction() throws Exception {
    //<string>"%sopenurl.asp?genre=volume&amp;eissn=%s&amp;volume=%s", base_url, journal_issn, volume_name</string>
    String expectedStartUrl = BASE_URL + "openurl.asp?genre=volume&eissn=" + JOURNAL_ISSN + "&volume=" + VOLUME;
    ArchivalUnit au = createAu();
    assertEquals(ListUtil.list(expectedStartUrl), au.getStartUrls());
  }
  
  public void testGetUrlStems() throws Exception {
    ArchivalUnit au = createAu();
    assertEquals(ListUtil.list(BASE_URL), au.getUrlStems());
  }
  
  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    ArchivalUnit au = createAu();
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(au.shouldCrawlForNewContent(aus));
  }
  
  public void testShouldDoNewContentCrawlForZero() throws Exception {
    ArchivalUnit au = createAu();
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(au.shouldCrawlForNewContent(aus));
  }
  
  public void testShouldDoNewContentCrawlEachMonth() throws Exception {
    ArchivalUnit au = createAu();
    AuState aus = new MockAuState(null, 4 * Constants.WEEK, -1, -1, null);
    assertTrue(au.shouldCrawlForNewContent(aus));
  }
  
  public void testGetName() throws Exception {
    ArchivalUnit au = createAu();
    assertEquals("Metapress Plugin (CLOCKSS), Base URL " + BASE_URL + ", ISSN " + JOURNAL_ISSN + ", Volume " + VOLUME, au.getName());
  }
  
  public void testPollSpecial() throws Exception {
    ArchivalUnit au = createAu();
    
    // if it changes in the plugin, you will likely need to change the test, so verify
    assertEquals(ListUtil.list(
        METAPRESS_REPAIR_FROM_PEER_REGEXP),
        RegexpUtil.regexpCollection(au.makeRepairFromPeerIfMissingUrlPatterns()));
    
    // make sure that's the regexp that will match to the expected url string
    // this also tests the regexp (which is the same) for the weighted poll map
    List <String> repairList = ListUtil.list(
        BASE_URL + "dynamic-file.axd?id=19782f32-57da-4786-91d5-5976de921a91&m=True",
        BASE_URL + "dynamic-file.axd?id=3817bfc6-0fd8-49bb-aa3c-feb24ef0813b&m=True");
    Pattern p = Pattern.compile(METAPRESS_REPAIR_FROM_PEER_REGEXP);
    for (String urlString : repairList) {
      Matcher m = p.matcher(urlString);
      assertEquals(urlString, true, m.find());
    }
    
    //and this one should fail - it needs to be weighted correctly and repaired from publisher if possible
    String notString = BASE_URL + "print.css";
    Matcher m = p.matcher(notString);
    assertEquals(false, m.find());
    
    PatternFloatMap urlPollResults = au.makeUrlPollResultWeightMap();
    assertNotNull(urlPollResults);
    for (String urlString : repairList) {
      assertEquals(0.0, urlPollResults.getMatch(urlString, (float) 1), .0001);
    }
    assertEquals(1.0, urlPollResults.getMatch(notString, (float) 1), .0001);
  }
  
}
