/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clogic;

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

public class TestCulturalLogicArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();

  static final String ROOT_URL = "http://eserver.org/clogic/";

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();

    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  private DefinableArchivalUnit makeAu(URL url, String year)
      throws Exception {
    Properties props = new Properties();
    props.setProperty(YEAR_KEY, year);
    if (url!=null) {
      props.setProperty(BASE_URL_KEY, url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(getMockLockssDaemon(),
                      "org.lockss.plugin.clogic.CulturalLogicPlugin");
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, "2000");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }

  public void testConstructBadYear() throws Exception {
    URL url = new URL(ROOT_URL);
    try {
      makeAu(url, "123");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }

  public void testShouldCacheProperPages() throws Exception {
    URL base = new URL(ROOT_URL);
    String year = "2003";
    ArchivalUnit clAu = makeAu(base, year);
    theDaemon.getLockssRepository(clAu);
    theDaemon.getNodeManager(clAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(clAu,
        new RangeCachedUrlSetSpec(base.toString()));

    String baseUrl = ROOT_URL + "2003/";

    // root pages
    shouldCacheTest(baseUrl+"2003.html", true, clAu, cus);

    // article html
    shouldCacheTest(baseUrl+"sanjuan.html", true, clAu, cus);
    shouldCacheTest(baseUrl+"sharpe.html", true, clAu, cus);

    // images and movies
    shouldCacheTest(baseUrl+"images/rwandan_boy.jpg", true, clAu, cus);
    shouldCacheTest(baseUrl+"Sh-Boom.mov", true, clAu, cus);
    shouldCacheTest(baseUrl+"anti-war%20protest--raleigh/one_world.gif", true, clAu, cus);

    // should not cache these

    // current issue
    shouldCacheTest(ROOT_URL, false, clAu, cus);
    shouldCacheTest(ROOT_URL+"2004.html", false, clAu, cus);

    // info pages
    shouldCacheTest(ROOT_URL+"editors.html", false, clAu, cus);

    // archived volume page
    shouldCacheTest(ROOT_URL+"2002/2002.html", false, clAu, cus);

    // main server
    shouldCacheTest("http://eserver.org", false, clAu, cus);

    // LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, clAu, cus);

    // other site
    shouldCacheTest("http://exo.com/~noid/ConspiracyNet/satan.html", false, clAu, cus);
  }

  private void shouldCacheTest(String url, boolean shouldCache,
			       ArchivalUnit au, CachedUrlSet cus) {
    assertTrue(au.shouldBeCached(url)==shouldCache);
  }

  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(ROOT_URL);

    String expectedStr = ROOT_URL+"lockss-2003.html";
    DefinableArchivalUnit clAu = makeAu(url, "2003");
    assertEquals(ListUtil.list(expectedStr), clAu.getStartUrls());
  }

  public void testGetUrlStems() throws Exception {
    String stem1 = "http://eserver.org/";
    DefinableArchivalUnit clAu1 = makeAu(new URL(stem1 + "clogic/"), "2003");
    assertEquals(ListUtil.list(stem1), clAu1.getUrlStems());
    String stem2 = "http://eserver.org:8080/";
    DefinableArchivalUnit clAu2 = makeAu(new URL(stem2 + "clogic/"), "2003");
    assertEquals(ListUtil.list(stem2), clAu2.getUrlStems());
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    ArchivalUnit clAu = makeAu(new URL(ROOT_URL), "2003");
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(clAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlForZero() throws Exception {
    ArchivalUnit clAu = makeAu(new URL(ROOT_URL), "2003");
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(clAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlEachMonth() throws Exception {
    ArchivalUnit clAu = makeAu(new URL(ROOT_URL), "2003");
    AuState aus = new MockAuState(null, 4 * Constants.WEEK, -1, -1, null);
    assertTrue(clAu.shouldCrawlForNewContent(aus));
  }

  public void testGetName() throws Exception {
    DefinableArchivalUnit au = makeAu(new URL(ROOT_URL), "2003");
    assertEquals("eserver.org/clogic/, 2003", au.getName());
    DefinableArchivalUnit au1 =
        makeAu(new URL("http://www.bmj.com/clogic/"), "2004");
    assertEquals("www.bmj.com/clogic/, 2004", au1.getName());
  }

  public void testGetFilterRules() throws Exception {
    DefinableArchivalUnit au = makeAu(new URL(ROOT_URL), "2003");
    assertNull(au.getFilterRule(null));
    assertNull(au.getFilterRule("jpg"));
    assertNull(au.getFilterRule("text/html"));
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestCulturalLogicArchivalUnit.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
