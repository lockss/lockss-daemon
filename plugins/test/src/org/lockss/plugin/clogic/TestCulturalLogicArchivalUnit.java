/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.clogic;

import java.net.URL;
import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.state.AuState;
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
    assertEquals("Cultural Logic Plugin, Base URL http://eserver.org/clogic/, Year 2003", au.getName());
    DefinableArchivalUnit au1 =
        makeAu(new URL("http://www.bmj.com/clogic/"), "2004");
    assertEquals("Cultural Logic Plugin, Base URL http://www.bmj.com/clogic/, Year 2004", au1.getName());
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
