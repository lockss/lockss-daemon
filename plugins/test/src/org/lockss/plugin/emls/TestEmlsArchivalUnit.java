/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.emls;

import java.net.URL;
import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.state.AuState;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.plugin.definable.*;

public class TestEmlsArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NUMBER.getKey();


  static final String ROOT_URL = "http://extra.shu.ac.uk/emls/";

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();

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
    assertTrue(au.shouldBeCached(url)==shouldCache);
  }

  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(ROOT_URL);
    String expectedStr = ROOT_URL+"emlsjour.html";
    DefinableArchivalUnit eAu = makeAu(url, 3);
    assertEquals(ListUtil.list(expectedStr), eAu.getStartUrls());
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
    assertEquals("1/6000", au.getRateLimiterInfo().getDefaultRate());
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestEmlsArchivalUnit.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
