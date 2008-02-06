/*
 * $Id: TestClockssAmericanChemicalSocietyArchivalUnit.java,v 1.1 2008-02-06 18:10:57 thib_gc Exp $
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

package org.lockss.plugin.acs;

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

public class TestClockssAmericanChemicalSocietyArchivalUnit
    extends LockssTestCase {
  private MockLockssDaemon theDaemon;

  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String JRNL_KEY = TestClockssAmericanChemicalSocietyPlugin.JOURNAL_KEY.getKey();
  static final String ARTICAL_KEY = TestClockssAmericanChemicalSocietyPlugin.ARTICLE_URL.getKey();

  static final String ROOT_URL = "http://pubs3.acs.org/";
  static final String ARTICLE_ROOT = "http://pubs.acs.org/";
  static final String JOURNAL_KEY = "jcisd8";
  static final String VOL_ID = "43";
  static final int VOL_YEAR = 2003;

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

  private DefinableArchivalUnit makeAu(URL volUrl,
				       URL issueUrl,
				       String jkey,
				       String volumeStr) throws Exception {

    Properties props = new Properties();

    if (volUrl != null) {
      props.setProperty(BASE_URL_KEY, volUrl.toString());
    }
    if (issueUrl != null) {
      props.setProperty(ARTICAL_KEY, issueUrl.toString());
    }
    if (jkey != null) {
      props.setProperty(JRNL_KEY, jkey);
    }
    if (volumeStr != null) {
      props.setProperty(VOL_KEY, volumeStr);
    }

    Configuration config = ConfigurationUtil.fromProps(props);
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(theDaemon, "org.lockss.plugin.acs.ClockssAmericanChemicalSocietyPlugin");
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {

    try {
      URL a_url = new URL(ARTICLE_ROOT);
      makeAu(null, a_url, JOURNAL_KEY, VOL_ID);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    }
    catch (ArchivalUnit.ConfigurationException e) {}

    try {
      URL base = new URL(ROOT_URL);
      makeAu(base, null, JOURNAL_KEY, VOL_ID);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    }
    catch (ArchivalUnit.ConfigurationException e) {}
  }

  public void testContstructNullJournalKey() throws Exception {
    URL a_url = new URL(ARTICLE_ROOT);
    URL base = new URL(ROOT_URL);
    try {
      makeAu(base, a_url, null, VOL_ID);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    }
    catch (ArchivalUnit.ConfigurationException e) {}
  }

  public void testShouldCacheProperPages() throws Exception {
    URL art_url = new URL(ARTICLE_ROOT);
    URL base_url = new URL(ROOT_URL);
    String a_root = art_url.toString();
    String b_root = base_url.toString();
    String url;

    DefinableArchivalUnit acsAu =
      makeAu(base_url, art_url, JOURNAL_KEY, VOL_ID);

    theDaemon.getLockssRepository(acsAu);
    theDaemon.getNodeManager(acsAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(acsAu,
        new RangeCachedUrlSetSpec(base_url.toString()));

    // start url - should be cached
    shouldCacheTest(acsAu.getProperties().getString(ArchivalUnit.KEY_AU_START_URL, null),
                    true, acsAu, cus);

    // issue index page - should be cached
    url = b_root +"acs/journals/toc.page?incoden=" +
        JOURNAL_KEY +"&involume=" + VOL_ID + "&inissue=1";
    shouldCacheTest(url, true, acsAu, cus);

    //images and style sheets - should be cached
    url = a_root + "images/small-spacer.gif";
    shouldCacheTest(url, true, acsAu, cus);

    url = a_root + "archives/images/rule.jpg";
    shouldCacheTest(url, true, acsAu, cus);

    url = a_root + "archives/styles/toc.css";
    shouldCacheTest(url, true, acsAu, cus);

    // the articles should be cached
    url = a_root + "cgi-bin/article.cgi/" + JOURNAL_KEY + "/" + VOL_YEAR + "/"
        + VOL_ID + "/i01/pdf/ci010133j.pdf";
    shouldCacheTest(url, true, acsAu, cus);

    url = a_root + "cgi-bin/article.cgi/" + JOURNAL_KEY +"/" + VOL_YEAR + "/"
        + VOL_ID + "/i01/html/ci010133j.html";
    shouldCacheTest(url, true, acsAu, cus);

    // we don't cache abstracts
    url = a_root + "cgi-bin/abstract.cgi/" + JOURNAL_KEY +"/" + VOL_YEAR + "/"
        + VOL_ID + "/i01/abs/ci010133j.html";
    shouldCacheTest(url, true, acsAu, cus);

    // we do cache supporting info

    url = b_root +
        "acs/journals/supporting_information.page?in_manuscript=ci020047z";
    shouldCacheTest(url, true, acsAu, cus);

    // we don't cache payment pages
    url = a_root +"UADB/xppview/cgi-bin/sample.cgi/" +
        JOURNAL_KEY +"/" + VOL_YEAR + "/" + VOL_ID + "i01/pdf/ci010133j.pdf";
    shouldCacheTest(url, false, acsAu, cus);

    url = b_root + "cgi-bin/article.cgi/" + JOURNAL_KEY +"/" + VOL_YEAR + "/" +
        VOL_ID + "/i01/pdf/ci010133j.pdf";
    shouldCacheTest(url, true, acsAu, cus);
  }

  private void shouldCacheTest(String url, boolean shouldCache,
			       ArchivalUnit au, CachedUrlSet cus) {
    UrlCacher uc = au.makeUrlCacher(url);
    assertEquals(shouldCache, uc.shouldBeCached());
  }

  public void testStartUrlConstruction() throws Exception {
    String expected = ROOT_URL +
        "acs/journals/toc.clockss_manifest?incoden=" +
        JOURNAL_KEY + "&involume=" + VOL_ID;
    URL a_url = new URL(ARTICLE_ROOT);
    URL base = new URL(ROOT_URL);
    DefinableArchivalUnit acsAu = makeAu(base, a_url, JOURNAL_KEY, VOL_ID);
    assertEquals(expected, acsAu.getProperties().getString(ArchivalUnit.KEY_AU_START_URL, null));
  }

  public void testGetUrlStems() throws Exception {
    System.err.println("Stop1");
    URL a_url = new URL(ARTICLE_ROOT);
    URL base = new URL(ROOT_URL);
    String stem1 = "http://pubs3.acs.org/";
    String stem2 = "http://pubs.acs.org/";
    DefinableArchivalUnit acsAu1 = makeAu(base, a_url, JOURNAL_KEY, VOL_ID);
    assertEquals(ListUtil.list(stem1, stem2), acsAu1.getUrlStems());
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    URL a_url = new URL(ARTICLE_ROOT);
    URL base = new URL(ROOT_URL);
    ArchivalUnit acsAu = makeAu(base, a_url, JOURNAL_KEY, VOL_ID);
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(acsAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlForZero() throws Exception {
    URL a_url = new URL(ARTICLE_ROOT);
    URL base = new URL(ROOT_URL);
    ArchivalUnit acsAu = makeAu(base, a_url, JOURNAL_KEY, VOL_ID);
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(acsAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlEachMonth() throws Exception {
    URL a_url = new URL(ARTICLE_ROOT);
    URL base = new URL(ROOT_URL);
    ArchivalUnit acsAu = makeAu(base, a_url, JOURNAL_KEY, VOL_ID);
    AuState aus = new MockAuState(null, 4 * Constants.WEEK, -1, -1, null);
    assertTrue(acsAu.shouldCrawlForNewContent(aus));
  }

  public void testGetName() throws Exception {
    URL a_url = new URL(ARTICLE_ROOT);
    URL base = new URL(ROOT_URL);
    DefinableArchivalUnit au = makeAu(base, a_url, JOURNAL_KEY, VOL_ID);
    assertEquals("American Chemical Society Plugin (CLOCKSS), Base URL http://pubs3.acs.org/, Journal Key jcisd8, Volume 43", au.getName());
    DefinableArchivalUnit au1 =
      makeAu(new URL("http://www.bmj.com/"),
	     new URL("http://www.bmj.com/"), "bmj", "61");
    assertEquals("American Chemical Society Plugin (CLOCKSS), Base URL http://www.bmj.com/, Journal Key bmj, Volume 61", au1.getName());
  }

  public void testRefetchDepth() throws Exception {
    URL a_url = new URL(ARTICLE_ROOT);
    URL base = new URL(ROOT_URL);
    DefinableArchivalUnit au = makeAu(base, a_url, JOURNAL_KEY, VOL_ID);
    SpiderCrawlSpec cs = (SpiderCrawlSpec) au.getCrawlSpec();
    assertEquals(1, cs.getRefetchDepth());
  }

  public void testDefPauseTime() throws Exception {
    URL a_url = new URL(ARTICLE_ROOT);
    URL base = new URL(ROOT_URL);
    DefinableArchivalUnit au = makeAu(base, a_url, JOURNAL_KEY, VOL_ID);
    assertEquals("1/6000ms", au.findFetchRateLimiter().getRate());
  }

  public static void main(String[] argv) {
    String[] testCaseList = {
        TestClockssAmericanChemicalSocietyArchivalUnit.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
