/*
 * $Id: TestIeeeArchivalUnit.java,v 1.5 2008-08-17 08:43:20 tlipkis Exp $
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

package org.lockss.plugin.ieee;

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

public class TestIeeeArchivalUnit
    extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String PUNUM_KEY = TestIeeePlugin.PU_NUMBER.getKey();
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();

  static final String ROOT_URL = "http://ieeexplore.ieee.org/";
  static final int PUB_NUMBER = 8;
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
                                 int pub,
                                 int year) throws Exception {

    Properties props = new Properties();

    if (volUrl != null) {
      props.setProperty(BASE_URL_KEY, volUrl.toString());
    }
    props.setProperty(PUNUM_KEY, Integer.toString(pub));
    props.setProperty(YEAR_KEY, Integer.toString(year));

    Configuration config = ConfigurationUtil.fromProps(props);
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(theDaemon,"org.lockss.plugin.ieee.IeeePlugin");
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    au.setConfiguration(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {


    try {
      makeAu(null, PUB_NUMBER, VOL_YEAR);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    }
    catch (ArchivalUnit.ConfigurationException e) {}
  }

  public void testConstructNegativePubNumber() throws Exception {
    URL base = new URL(ROOT_URL);

    try {
      makeAu(base, -1, VOL_YEAR);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    }
    catch (ArchivalUnit.ConfigurationException e) {}
  }

  public void testConstructNegativeYear() throws Exception {
    URL base = new URL(ROOT_URL);

    try {
      makeAu(base, PUB_NUMBER, -1);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    }
    catch (ArchivalUnit.ConfigurationException e) {}
  }

  public void testShouldCacheProperPages() throws Exception {
    URL base = new URL(ROOT_URL);
    String b_root = base.toString();
    String url;

    DefinableArchivalUnit ieeeAu = makeAu(base, PUB_NUMBER, VOL_YEAR);

    theDaemon.getLockssRepository(ieeeAu);
    theDaemon.getNodeManager(ieeeAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(ieeeAu,
        new RangeCachedUrlSetSpec(base.toString()));

    // start url - should be cached
    shouldCacheTest(ieeeAu.getNewContentCrawlUrls().get(0), true, ieeeAu, cus);


    // issue index page - should be cached
    url = b_root +"xpl/tocresult.jsp?isNumber=27564";
    shouldCacheTest(url, true, ieeeAu, cus);

    // printable issue index page - should be cached

    //images and style sheets - should be cached
    url = b_root + "images/small-spacer.gif";
    shouldCacheTest(url, true, ieeeAu, cus);

    url = b_root.substring(0,b_root.length() - 1);
    url = url + ":80/images/small-spacer.gif";
    shouldCacheTest(url,true, ieeeAu, cus);

    url = b_root + "archives/images/rule.jpg";
    shouldCacheTest(url, true, ieeeAu, cus);

    // the pdf of articles should be cached
    url = b_root + "iel5/" + PUB_NUMBER +
        "/27564/01229882.pdf?isNumber=27564&arnumber=1229882";
    shouldCacheTest(url, true, ieeeAu, cus);

    // we cache abstracts
    url = b_root.substring(0,b_root.length()-1);
    url = url + ":80/xpls/abs_all.jsp?isNumber=27564";
    shouldCacheTest(url, true, ieeeAu, cus);

  }

  private void shouldCacheTest(String url, boolean shouldCache,
			       ArchivalUnit au, CachedUrlSet cus) {
    UrlCacher uc = au.makeUrlCacher(url);
    assertEquals(shouldCache, uc.shouldBeCached());
  }

  public void testStartUrlConstruction() throws Exception {
    String expected = ROOT_URL +
        "xpl/RecentIssue.jsp?puNumber=" + PUB_NUMBER + "&year=" + VOL_YEAR;
    URL base = new URL(ROOT_URL);
    DefinableArchivalUnit ieeeAu = makeAu(base, PUB_NUMBER, VOL_YEAR);
    assertEquals(ListUtil.list(expected), ieeeAu.getNewContentCrawlUrls());
  }

  public void testGetUrlStems() throws Exception {
    URL base = new URL(ROOT_URL);
    String stem = "http://ieeexplore.ieee.org/";
    DefinableArchivalUnit ieeeAu = makeAu(base, PUB_NUMBER, VOL_YEAR);
    assertEquals(ListUtil.list(stem), ieeeAu.getUrlStems());

  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    URL base = new URL(ROOT_URL);
    ArchivalUnit ieeeAu = makeAu(base, PUB_NUMBER, VOL_YEAR);
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(ieeeAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlForZero() throws Exception {
    URL base = new URL(ROOT_URL);
    ArchivalUnit ieeeAu = makeAu(base, PUB_NUMBER, VOL_YEAR);
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(ieeeAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlEachMonth() throws Exception {
    URL base = new URL(ROOT_URL);
    ArchivalUnit ieeeAu = makeAu(base, PUB_NUMBER, VOL_YEAR);
    AuState aus = new MockAuState(null, 4 * Constants.WEEK, -1, -1, null);
    assertTrue(ieeeAu.shouldCrawlForNewContent(aus));
  }

  public void testGetName() throws Exception {
    URL base = new URL(ROOT_URL);
    DefinableArchivalUnit au = makeAu(base, PUB_NUMBER, VOL_YEAR);
    assertEquals("ieeexplore.ieee.org, puNumber 8, 2003", au.getName());
  }

  public static void main(String[] argv) {
    String[] testCaseList = {
        TestIeeeArchivalUnit.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
