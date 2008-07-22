/*
 * $Id: TestEmlsArchivalUnit.java,v 1.7 2008-07-22 06:43:13 thib_gc Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.emls;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.state.AuState;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.plugin.definable.*;

public class TestEmlsArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NUMBER.getKey();


  static final String ROOT_URL = "http://extra.shu.ac.uk/emls/";

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

  private DefinableArchivalUnit makeAu(URL url, int volume)
      throws Exception {
    Properties props = new Properties();
    props.setProperty(VOL_KEY, ""+volume);
    if (url!=null) {
      props.setProperty(BASE_URL_KEY, url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(theDaemon,"org.lockss.plugin.emls.EmlsPlugin");
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, 3);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }

  public void testConstructBadVolume() throws Exception {
    try {
      makeAu(new URL(ROOT_URL), -1);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }

  public void testShouldCacheProperPages() throws Exception {
    URL base = new URL(ROOT_URL);
    ArchivalUnit pmAu = makeAu(base, 3);
    theDaemon.getLockssRepository(pmAu);
    theDaemon.getNodeManager(pmAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(pmAu,
        new RangeCachedUrlSetSpec(base.toString()));

    // root page
    shouldCacheTest(ROOT_URL+"lockss-volume3.html", true, pmAu, cus);

    // toc html
    shouldCacheTest(ROOT_URL+"03-1/03-1toc.htm", true, pmAu, cus);
    shouldCacheTest(ROOT_URL+"03-2/03-2toc.htm", true, pmAu, cus);

    // contents html
    shouldCacheTest(ROOT_URL+"03-1/abstracts.htm", true, pmAu, cus);
    shouldCacheTest(ROOT_URL+"03-2/fishball.html", true, pmAu, cus);

    // images
    shouldCacheTest(ROOT_URL+"header2.gif", true, pmAu, cus);
    shouldCacheTest(ROOT_URL+"03-1/button.gif", true, pmAu, cus);

    // should not cache these

    // other archived root page
    shouldCacheTest(ROOT_URL+"lockss-volume2.html", false, pmAu, cus);

    // other toc page
    shouldCacheTest(ROOT_URL+"02-1/02-1toc.htm", false, pmAu, cus);

    // home page
    shouldCacheTest(ROOT_URL+"emlshome.html", false, pmAu, cus);

    // masthead page
    shouldCacheTest(ROOT_URL+"emlsmast.html", false, pmAu, cus);

    // LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, pmAu, cus);

    // other sites
    shouldCacheTest("http://purl.oclc.org/emls/09-1/wagnblaz.htm", false, pmAu, cus);
  }

  private void shouldCacheTest(String url, boolean shouldCache,
                               ArchivalUnit au, CachedUrlSet cus) {
    UrlCacher uc = au.makeUrlCacher(url);
    assertTrue(uc.shouldBeCached()==shouldCache);
  }

  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(ROOT_URL);
    String expectedStr = ROOT_URL+"lockss-volume3.html";
    DefinableArchivalUnit eAu = makeAu(url, 3);
    assertEquals(expectedStr, eAu.getProperties().getString(ArchivalUnit.KEY_AU_START_URL, null));
  }

  public void testGetUrlStems() throws Exception {
    String stem1 = "http://muse.jhu.edu/";
    DefinableArchivalUnit eAu1 = makeAu(new URL(stem1 + "emls/"), 3);
    assertEquals(ListUtil.list(stem1), eAu1.getUrlStems());
    String stem2 = "http://muse.jhu.edu:8080/";
    DefinableArchivalUnit eAu2 = makeAu(new URL(stem2 + "emls/"), 3);
    assertEquals(ListUtil.list(stem2), eAu2.getUrlStems());
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    ArchivalUnit pmAu = makeAu(new URL(ROOT_URL), 3);
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(pmAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlForZero() throws Exception {
    ArchivalUnit pmAu = makeAu(new URL(ROOT_URL), 3);
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(pmAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlEachMonth() throws Exception {
    ArchivalUnit pmAu = makeAu(new URL(ROOT_URL), 3);
    AuState aus = new MockAuState(null, 4 * Constants.WEEK, -1, -1, null);
    assertTrue(pmAu.shouldCrawlForNewContent(aus));
  }

  public void testGetName() throws Exception {
    DefinableArchivalUnit au = makeAu(new URL(ROOT_URL), 3);
    assertEquals("Early Modern Literary Studies Plugin, Base URL http://extra.shu.ac.uk/emls/, Volume 3", au.getName());
  }

  public void testDefPauseTime() throws Exception {
    DefinableArchivalUnit au = makeAu(new URL(ROOT_URL), 3);
    assertEquals("1/6000ms", au.findFetchRateLimiter().getRate());
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestEmlsArchivalUnit.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
