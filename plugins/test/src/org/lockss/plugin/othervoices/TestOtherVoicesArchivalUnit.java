/*

Copyright (c) 2000-2020, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.othervoices;

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

public class TestOtherVoicesArchivalUnit extends LockssTestCase {
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NUMBER.getKey();

  private MockLockssDaemon theDaemon;

  static final String ROOT_URL = "http://www.othervoices.org/";

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
    assertTrue(au.shouldBeCached(url)==shouldCache);
  }

  public void testStartUrlPermissionUrl() throws Exception {
    URL url = new URL(ROOT_URL);
    DefinableArchivalUnit ovAu = makeAu(url, 2);
    assertEquals(ListUtil.list(ROOT_URL+"archives.php"), ovAu.getStartUrls());
    assertEquals(ListUtil.list(ROOT_URL+"lockss-volume2.html"), ovAu.getPermissionUrls());
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
