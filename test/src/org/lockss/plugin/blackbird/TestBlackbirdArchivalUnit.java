/*
 * $Id: TestBlackbirdArchivalUnit.java,v 1.1 2003-12-06 00:57:25 eaalto Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.blackbird;

import java.io.File;
import java.net.URL;
import java.util.Properties;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.state.AuState;
import org.lockss.repository.LockssRepositoryImpl;

public class TestBlackbirdArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

  static final String ROOT_URL = "http://www.blackbird.vcu.edu/";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    theDaemon = new MockLockssDaemon();
    theDaemon.getHashService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private BlackbirdArchivalUnit makeAu(URL url, int volume)
      throws ArchivalUnit.ConfigurationException {
    Properties props = new Properties();
    props.setProperty(BlackbirdPlugin.AUPARAM_VOL, Integer.toString(volume));
    if (url!=null) {
      props.setProperty(BlackbirdPlugin.AUPARAM_BASE_URL, url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    BlackbirdArchivalUnit au = new BlackbirdArchivalUnit(
        new BlackbirdPlugin());
    au.getPlugin().initPlugin(theDaemon);
    au.setConfiguration(config);
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
    ArchivalUnit pmAu = makeAu(base, volume);
    theDaemon.getLockssRepository(pmAu);
    theDaemon.getNodeManager(pmAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(pmAu,
        new RangeCachedUrlSetSpec(base.toString()));

    String baseUrl = ROOT_URL + "v2n1/";

    // root pages
    shouldCacheTest(baseUrl+"index.htm", true, pmAu, cus);
    shouldCacheTest(ROOT_URL+"v2n2/index.htm", true, pmAu, cus);

    // volume pages
    shouldCacheTest(baseUrl+"poetry.htm", true, pmAu, cus);
    shouldCacheTest(baseUrl+"gallery.htm", true, pmAu, cus);

    // info pages
    shouldCacheTest(baseUrl+"new.htm", true, pmAu, cus);
    shouldCacheTest(baseUrl+"editorial_policy.htm", true, pmAu, cus);
    shouldCacheTest(ROOT_URL+"v2n2/acknowledgements.htm", true, pmAu, cus);

    // article html
    shouldCacheTest(baseUrl+"gallery/burnside_c/index.htm", true, pmAu, cus);
    shouldCacheTest(baseUrl+"gallery/burnside_c/retrospective.htm", true,
                    pmAu, cus);
    shouldCacheTest(baseUrl+"nonfiction/dillard_r/going.htm", true, pmAu, cus);
    shouldCacheTest(baseUrl+"poetry/black_s/index.htm", true, pmAu, cus);

    // images
    shouldCacheTest(baseUrl+"images/spacer.gif", true, pmAu, cus);
    shouldCacheTest(baseUrl+"images/audio.gif", true, pmAu, cus);
    shouldCacheTest(baseUrl+"gallery/burnside_c/burnside6_125.jpg", true,
                    pmAu, cus);

    // stylesheet
    shouldCacheTest(baseUrl+"blkbird.css", true, pmAu, cus);
    shouldCacheTest(baseUrl+"poetry/blkbird.css", true, pmAu, cus);


    // should not cache these

    //XXX temporary!
    // ram files
    shouldCacheTest(baseUrl+"gallery/burnside_c/interview_part1.ram", false,
                    pmAu, cus);
    shouldCacheTest(baseUrl+"gallery/burnside_c/interview_part1.rm", false,
                    pmAu, cus);
    shouldCacheTest(baseUrl+"gallery/burnside_c_091603/lucy1.ram", false,
                    pmAu, cus);

    // current issue
    shouldCacheTest(ROOT_URL, false, pmAu, cus);
    shouldCacheTest(ROOT_URL+"index.htm", false, pmAu, cus);

    // index links

    // archived root page
    shouldCacheTest(ROOT_URL+"v1n1/index.htm", false, pmAu, cus);

    // archived volume page
    shouldCacheTest(ROOT_URL+"v1n2/gallery/example.htm", false, pmAu, cus);

    // LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, pmAu, cus);

    // other site
    shouldCacheTest("http://www.real.com/", false, pmAu, cus);
  }

  private void shouldCacheTest(String url, boolean shouldCache,
			       ArchivalUnit au, CachedUrlSet cus) {
    UrlCacher uc = au.getPlugin().makeUrlCacher(cus, url);
    assertTrue(uc.shouldBeCached()==shouldCache);
  }

  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(ROOT_URL);

    String expectedStr = ROOT_URL+"lockss.htm";
    BlackbirdArchivalUnit pmAu = makeAu(url, 2);
    assertEquals(expectedStr, pmAu.makeStartUrl());
  }

  public void testPathInUrlThrowsException() throws Exception {
    URL url = new URL(ROOT_URL+"path");
    try {
      makeAu(url, 2);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch(ArchivalUnit.ConfigurationException e) { }
  }

  public void testGetUrlStems() throws Exception {
    String stem1 = "http://www.blackbird.vcu.edu";
    BlackbirdArchivalUnit pmAu1 = makeAu(new URL(stem1 + "/"), 2);
    assertEquals(ListUtil.list(stem1), pmAu1.getUrlStems());
    String stem2 = "http://www.blackbird.vcu.edu:8080";
    BlackbirdArchivalUnit pmAu2 = makeAu(new URL(stem2 + "/"), 2);
    assertEquals(ListUtil.list(stem2), pmAu2.getUrlStems());
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    ArchivalUnit pmAu = makeAu(new URL(ROOT_URL), 2);
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(pmAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlForZero() throws Exception {
    ArchivalUnit pmAu = makeAu(new URL(ROOT_URL), 2);
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(pmAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlEachMonth() throws Exception {
    ArchivalUnit pmAu = makeAu(new URL(ROOT_URL), 2);
    AuState aus = new MockAuState(null, 4 * Constants.WEEK, -1, -1, null);
    assertTrue(pmAu.shouldCrawlForNewContent(aus));
  }

  public void testGetName() throws Exception {
    BlackbirdArchivalUnit au = makeAu(new URL(ROOT_URL), 2);
    assertEquals("www.blackbird.vcu.edu, vol. 2", au.getName());
    BlackbirdArchivalUnit au1 =
        makeAu(new URL("http://www.bmj.com/"), 3);
    assertEquals("www.bmj.com, vol. 3", au1.getName());
  }

  public void testGetFilterRules() throws Exception {
    BlackbirdArchivalUnit au = makeAu(new URL(ROOT_URL), 60);
    assertNull(au.getFilterRule(null));
    assertNull(au.getFilterRule("jpg"));
    assertNull(au.getFilterRule("text/html"));
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestBlackbirdArchivalUnit.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
