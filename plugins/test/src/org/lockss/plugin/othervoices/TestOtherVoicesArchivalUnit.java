/*
 * $Id: TestOtherVoicesArchivalUnit.java,v 1.6 2010-03-27 01:05:53 thib_gc Exp $
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

package org.lockss.plugin.othervoices;

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

public class TestOtherVoicesArchivalUnit extends LockssTestCase {
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NUMBER.getKey();

  private MockLockssDaemon theDaemon;

  static final String ROOT_URL = "http://www.othervoices.org/";

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
    props.setProperty(VOL_KEY, Integer.toString(volume));
    if (url!=null) {
      props.setProperty(BASE_URL_KEY, url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(theDaemon,"org.lockss.plugin.othervoices.OtherVoicesPlugin");
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, 1);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }

  public void testConstructNegativeVolume() throws Exception {
    URL url = new URL(ROOT_URL);
    try {
      makeAu(url, -1);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }

  public void testShouldCacheProperPages() throws Exception {
    URL base = new URL(ROOT_URL);
    int volume = 2;
    ArchivalUnit ovAu = makeAu(base, volume);
    theDaemon.getLockssRepository(ovAu);
    theDaemon.getNodeManager(ovAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(ovAu,
        new RangeCachedUrlSetSpec(base.toString()));

    String baseUrl = ROOT_URL + "2.1/";

    // root pages
    shouldCacheTest(baseUrl+"index.html", true, ovAu, cus);
    shouldCacheTest(ROOT_URL+"2.2/index.html", true, ovAu, cus);

    // info pages
    shouldCacheTest(baseUrl+"authors.html", true, ovAu, cus);

    // article html
    shouldCacheTest(baseUrl+"marcuse/tolerance.html", true, ovAu, cus);
    shouldCacheTest(baseUrl+"baum/seeunderlove.html", true, ovAu, cus);
    shouldCacheTest(baseUrl+"adlevy/poetry/index.html", true, ovAu, cus);
    shouldCacheTest(ROOT_URL+"2.2/rickels/index.html", true, ovAu, cus);

    // images
    shouldCacheTest(baseUrl+"marcuse/ill1.jpg", true, ovAu, cus);
    shouldCacheTest(baseUrl+"images/dachau.jpg", true, ovAu, cus);
    shouldCacheTest(baseUrl+"adlevy/exp/10_17/10_31", true, ovAu, cus);
    shouldCacheTest(ROOT_URL+"images/backthin.gif", true, ovAu, cus);

    // stylesheet
    shouldCacheTest(baseUrl+"blkbird.css", true, ovAu, cus);
    shouldCacheTest(baseUrl+"poetry/blkbird.css", true, ovAu, cus);


    // should not cache these

    //XXX temporary!
    // ram files
    shouldCacheTest("http://mediamogul.seas.upenn.edu:8080/ramgen/writershouse/theorizing/rabinbach/lecture.rm",
                    false, ovAu, cus);
    shouldCacheTest("http://slought.net/toc/archives/residue.php?play1=1049",
                    false, ovAu, cus);

    // current issue
    shouldCacheTest(ROOT_URL, false, ovAu, cus);
    shouldCacheTest(ROOT_URL+"index2.html", false, ovAu, cus);

    // cgi
    shouldCacheTest(ROOT_URL+"cgi/ov/search.cgi", false, ovAu, cus);

    // other links
    shouldCacheTest(ROOT_URL+"statement.html", false, ovAu, cus);
    shouldCacheTest(ROOT_URL+"forums/", false, ovAu, cus);

    // archived root page
    shouldCacheTest(ROOT_URL+"1.1/index.html", false, ovAu, cus);

    // archived volume page
    shouldCacheTest(ROOT_URL+"1.2/gallery/example.html", false, ovAu, cus);

    // LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, ovAu, cus);

    // other site
    shouldCacheTest("http://www.icaap.org/", false, ovAu, cus);
  }

  private void shouldCacheTest(String url, boolean shouldCache,
			       ArchivalUnit au, CachedUrlSet cus) {
    UrlCacher uc = au.makeUrlCacher(url);
    assertTrue(uc.shouldBeCached()==shouldCache);
  }

  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(ROOT_URL);

    String expectedStr = ROOT_URL+"lockss-volume2.html";
    DefinableArchivalUnit ovAu = makeAu(url, 2);
    assertEquals(ListUtil.list(expectedStr), ovAu.getNewContentCrawlUrls());
  }

  public void testGetUrlStems() throws Exception {
    String stem1 = "http://www.othervoices.org/";
    DefinableArchivalUnit ovAu1 = makeAu(new URL(stem1 + "foo/"), 2);
    assertEquals(ListUtil.list(stem1), ovAu1.getUrlStems());
    String stem2 = "http://www.othervoices.org:8080/";
    DefinableArchivalUnit ovAu2 = makeAu(new URL(stem2), 2);
    assertEquals(ListUtil.list(stem2), ovAu2.getUrlStems());
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    ArchivalUnit ovAu = makeAu(new URL(ROOT_URL), 2);
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(ovAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlForZero() throws Exception {
    ArchivalUnit ovAu = makeAu(new URL(ROOT_URL), 2);
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(ovAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlEachMonth() throws Exception {
    ArchivalUnit ovAu = makeAu(new URL(ROOT_URL), 2);
    AuState aus = new MockAuState(null, 4 * Constants.WEEK, -1, -1, null);
    assertTrue(ovAu.shouldCrawlForNewContent(aus));
  }

  public void testGetName() throws Exception {
    DefinableArchivalUnit au = makeAu(new URL(ROOT_URL), 2);
    assertEquals("Other Voices Plugin, Base URL http://www.othervoices.org/, Volume 2", au.getName());
    DefinableArchivalUnit au1 =
        makeAu(new URL("http://www.bmj.com/"), 3);
    assertEquals("Other Voices Plugin, Base URL http://www.bmj.com/, Volume 3", au1.getName());
  }

  public void testGetFilterRules() throws Exception {
    DefinableArchivalUnit au = makeAu(new URL(ROOT_URL), 2);
    assertNull(au.getFilterRule(null));
    assertNull(au.getFilterRule("jpg"));
    assertNull(au.getFilterRule("text/html"));
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestOtherVoicesArchivalUnit.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
