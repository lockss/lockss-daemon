/*
 * $Id: TestHistoryCooperativeArchivalUnit.java,v 1.6 2010-03-12 00:50:10 thib_gc Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.histcoop;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.state.AuState;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.plugin.definable.*;

public class TestHistoryCooperativeArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JRNL_KEY = ConfigParamDescr.JOURNAL_DIR.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NUMBER.getKey();

  static final String ROOT_URL = "http://www.historycooperative.org/";
  static final String DIR = "ahr";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(URL url, int volume, String journalDir)
      throws Exception {
    Properties props = new Properties();
    props.setProperty(VOL_KEY, Integer.toString(volume));
    if (url!=null) {
      props.setProperty(BASE_URL_KEY, url.toString());
    }
    if (journalDir!=null) {
      props.setProperty(JRNL_KEY, journalDir);
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(theDaemon,"org.lockss.plugin.histcoop.HistoryCooperativePlugin");
    DefinableArchivalUnit au =(DefinableArchivalUnit)ap.createAu(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, 1, DIR);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }

  public void testConstructNegativeVolume() throws Exception {
    URL url = new URL(ROOT_URL);
    try {
      makeAu(url, -1, DIR);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }

  public void testConstructNullDir() throws Exception {
    URL url = new URL(ROOT_URL);
    try {
      makeAu(url, 1, null);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }

  public void testShouldCacheProperPages() throws Exception {
    URL base = new URL(ROOT_URL);
    int volume = 108;
    ArchivalUnit hcAu = makeAu(base, volume, DIR);
    theDaemon.getLockssRepository(hcAu);
    theDaemon.getNodeManager(hcAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(hcAu,
        new RangeCachedUrlSetSpec(base.toString()));

    String baseUrl = ROOT_URL + "journals/"+DIR+"/";

    // root page
    shouldCacheTest(baseUrl+"lockss-volume108.html", true, hcAu, cus);

    // volume page
    shouldCacheTest(baseUrl+"108.1/", true, hcAu, cus);
    shouldCacheTest(baseUrl+"108.1/index.html", true, hcAu, cus);
    shouldCacheTest(baseUrl+"108.1/iti.html", true, hcAu, cus);
    // any other toc in this journal volume
    shouldCacheTest(baseUrl+"108.2/", true, hcAu, cus);

    // article html
    shouldCacheTest(baseUrl+"108.1/ah0103000001.html", true, hcAu, cus);
    shouldCacheTest(baseUrl+"108.2/walkowitz.html", true, hcAu, cus);

    // images
    shouldCacheTest(baseUrl+"images/vol108_iss1.gif", true, hcAu, cus);
    shouldCacheTest(baseUrl+"108.1/images/hunt_fig1b.jpg", true, hcAu, cus);
    shouldCacheTest(ROOT_URL+"gifs/ahrmast.gif", true,
                    hcAu, cus);

    // should not cache these
    // archived root page
    shouldCacheTest(baseUrl+"lockss-volume107.html", false, hcAu, cus);

    // archived volume page
    shouldCacheTest(baseUrl+"109.1/", false, hcAu, cus);

    // button destinations
    shouldCacheTest(ROOT_URL+"partners.html", false, hcAu, cus);
    shouldCacheTest(ROOT_URL+"journals.html", false, hcAu, cus);

    // LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, hcAu, cus);

    // other site
    shouldCacheTest("http://www2.historycooperative.org/", false, hcAu, cus);
  }

  private void shouldCacheTest(String url, boolean shouldCache,
			       ArchivalUnit au, CachedUrlSet cus) {
    UrlCacher uc = au.makeUrlCacher(url);
    assertTrue(uc.shouldBeCached()==shouldCache);
  }

  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(ROOT_URL);

    String expectedStr = ROOT_URL+"journals/"+DIR+"/lockss-volume108.html";
    DefinableArchivalUnit hcAu = makeAu(url, 108, DIR);
    assertEquals(ListUtil.list(expectedStr), hcAu.getNewContentCrawlUrls());
  }

  public void testGetUrlStems() throws Exception {
    String stem1 = "http://www.historycooperative.org/";
    DefinableArchivalUnit hcAu1 = makeAu(new URL(stem1 + "foo/"), 108, DIR);
    assertEquals(ListUtil.list(stem1), hcAu1.getUrlStems());
    String stem2 = "http://www.historycooperative.org:8080/";
    DefinableArchivalUnit hcAu2 = makeAu(new URL(stem2), 108, DIR);
    assertEquals(ListUtil.list(stem2), hcAu2.getUrlStems());
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    ArchivalUnit hcAu = makeAu(new URL(ROOT_URL), 108, DIR);
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(hcAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlForZero() throws Exception {
    ArchivalUnit hcAu = makeAu(new URL(ROOT_URL), 108, DIR);
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(hcAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlEachMonth() throws Exception {
    ArchivalUnit hcAu = makeAu(new URL(ROOT_URL), 108, DIR);
    AuState aus = new MockAuState(null, 4 * Constants.WEEK, -1, -1, null);
    assertTrue(hcAu.shouldCrawlForNewContent(aus));
  }

  public void testGetName() throws Exception {
    DefinableArchivalUnit au = makeAu(new URL(ROOT_URL), 108, DIR);
    assertEquals("History Cooperative Plugin, Base URL http://www.historycooperative.org/, Journal Directory ahr, Volume 108", au.getName());
    DefinableArchivalUnit au1 =
        makeAu(new URL("http://www.bmj.com/"), 109, "bmj");
    assertEquals("History Cooperative Plugin, Base URL http://www.bmj.com/, Journal Directory bmj, Volume 109", au1.getName());
  }

  public void testGetFilterRules() throws Exception {
    DefinableArchivalUnit au = makeAu(new URL(ROOT_URL), 108, DIR);
    assertNull(au.getFilterRule(null));
    assertNull(au.getFilterRule("jpg"));
    assertNull(au.getFilterRule("text/html"));
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestHistoryCooperativeArchivalUnit.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
