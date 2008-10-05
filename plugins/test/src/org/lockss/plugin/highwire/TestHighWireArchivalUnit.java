/*
 * $Id: TestHighWireArchivalUnit.java,v 1.10 2008-08-17 08:43:20 tlipkis Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.plugin.definable.*;
import org.lockss.plugin.wrapper.WrapperUtil;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.state.AuState;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestHighWireArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NUMBER.getKey();

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

  public void testGetVolumeNum() {

  }

  private DefinableArchivalUnit makeAu(URL url, int volume)
      throws Exception {
    Properties props = new Properties();
    props.setProperty(VOL_KEY, Integer.toString(volume));
    if (url != null) {
      props.setProperty(BASE_URL_KEY, url.toString());
    }
//    props.setProperty(BaseArchivalUnit.USE_CRAWL_WINDOW, ""+true);
//     props.setProperty(ConfigParamDescr.YEAR.getKey(), String.valueOf(year));
    Configuration config = ConfigurationUtil.fromProps(props);
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(theDaemon, "org.lockss.plugin.highwire.HighWirePlugin");
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, 1);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }

/*
  public void testConstructNegativeVolume() throws Exception {
    URL url = new URL("http://www.example.com/");
    try {
      makeAu(url, -1);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
*/

  public void testShouldNotCacheRootPage() throws Exception {
    URL base = new URL("http://shadow1.stanford.edu/");
    int volume = 322;
    ArchivalUnit hwAu = makeAu(base, volume);

    theDaemon.getLockssRepository(hwAu);
    theDaemon.getNodeManager(hwAu);
    CachedUrlSetSpec spec = new RangeCachedUrlSetSpec(base.toString());
    BaseCachedUrlSet cus = new BaseCachedUrlSet(hwAu, spec);
    UrlCacher uc = hwAu.makeUrlCacher("http://shadow1.stanford.edu/");
    assertFalse(uc.shouldBeCached());
  }

  public void testShouldNotCachePageFromOtherSite() throws Exception {
    URL base = new URL("http://shadow1.stanford.edu/");
    int volume = 322;
    ArchivalUnit hwAu = makeAu(base, volume);

    theDaemon.getLockssRepository(hwAu);
    theDaemon.getNodeManager(hwAu);
    CachedUrlSetSpec spec = new RangeCachedUrlSetSpec(base.toString());
    BaseCachedUrlSet cus = new BaseCachedUrlSet(hwAu, spec);
    UrlCacher uc = hwAu.makeUrlCacher("http://shadow2.stanford.edu/lockss-manifest/vol_322_manifest.dtl");

    assertFalse(uc.shouldBeCached());
  }

  public void testStartUrlConstruction() throws Exception {
    URL url = new URL("http://www.example.com/");
    String expectedStr = "http://www.example.com/lockss-manifest/vol_10_manifest.dtl";
    DefinableArchivalUnit hwau = makeAu(url, 10);
    assertEquals(ListUtil.list(expectedStr), hwau.getNewContentCrawlUrls());
  }

  public void testPermissionPageConstruction() throws Exception {
    URL url = new URL("http://www.example.com/");
    String expectedStr = "http://www.example.com/lockss-manifest/vol_10_manifest.dtl";

    DefinableArchivalUnit hwau = makeAu(url, 10);
    assertEquals(ListUtil.list(expectedStr), hwau.getNewContentCrawlUrls());
  }

  public void testGetUrlStems() throws Exception {
    String stem1 = "http://www.example.com/";
    DefinableArchivalUnit hwau1 = makeAu(new URL(stem1 + "foo/"), 10);
    assertEquals(ListUtil.list(stem1), hwau1.getUrlStems());
    String stem2 = "http://www.example.com:8080/";
    DefinableArchivalUnit hwau2 = makeAu(new URL(stem2), 10);
    assertEquals(ListUtil.list(stem2), hwau2.getUrlStems());
  }

  public void testGetNewContentCrawlUrls() throws Exception {
    URL url = new URL("http://www.example.com/");
    String expectedStr = "http://www.example.com/lockss-manifest/vol_10_manifest.dtl";
    DefinableArchivalUnit hwau = makeAu(url, 10);
    assertEquals(expectedStr, hwau.getNewContentCrawlUrls().get(0));
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    ArchivalUnit hwAu =
      makeAu(new URL("http://shadow1.stanford.edu/"), 322);

    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);

    assertFalse(hwAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlFor0() throws Exception {
    ArchivalUnit hwAu =
      makeAu(new URL("http://shadow1.stanford.edu/"), 322);

    AuState aus = new MockAuState(null, 0, -1, -1, null);

    assertTrue(hwAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlEachMonth() throws Exception {
    ArchivalUnit hwAu =
      makeAu(new URL("http://shadow1.stanford.edu/"), 322);

    AuState aus = new MockAuState(null, 4 * Constants.WEEK, -1, -1, null);

    assertTrue(hwAu.shouldCrawlForNewContent(aus));
  }

  public void testgetName() throws Exception {
    DefinableArchivalUnit au =
      makeAu(new URL("http://shadow1.stanford.edu/"), 42);
    assertEquals("HighWire Press Plugin (Legacy), Base URL http://shadow1.stanford.edu/, Volume 42", au.getName());
    DefinableArchivalUnit au1 =
      makeAu(new URL("http://www.bmj.com/"), 42);
    assertEquals("HighWire Press Plugin (Legacy), Base URL http://www.bmj.com/, Volume 42", au1.getName());
  }

 public void testGetFilterRuleNoContentTytpe() throws Exception {
    DefinableArchivalUnit au =
      makeAu(new URL("http://shadow1.stanford.edu/"), 42);
    assertNull(au.getFilterRule(null));
  }

  public void testGetFilterRuleNonHtmlContentType() throws Exception {
    DefinableArchivalUnit au =
      makeAu(new URL("http://shadow1.stanford.edu/"), 42);
    assertNull(au.getFilterRule("jpg"));
  }

  public void testGetFilterRuleHtmlContentType() throws Exception {
    DefinableArchivalUnit au =
      makeAu(new URL("http://shadow1.stanford.edu/"), 42);
    assertTrue(WrapperUtil.unwrap(au.getFilterRule("text/html"))
	       instanceof HighWireFilterRule);
  }

  /**
   * Real tests in TestHighwireArchivalUnit
   */
  public void testCrawlWindowNone() throws Exception {
    Properties props = new Properties();
    props.put(BaseArchivalUnit.PARAM_USE_CRAWL_WINDOW_BY_DEFAULT, "false");
    ConfigurationUtil.setCurrentConfigFromProps(props);

    DefinableArchivalUnit au =
      makeAu(new URL("http://shadow1.stanford.edu/"), 42);
    CrawlWindow window = au.getCrawlSpec().getCrawlWindow();
    assertNull(window);
  }

  public void testCrawlWindow() throws Exception {
    Properties props = new Properties();
    props.put(BaseArchivalUnit.PARAM_USE_CRAWL_WINDOW_BY_DEFAULT, "true");
    ConfigurationUtil.setCurrentConfigFromProps(props);

    DefinableArchivalUnit au =
      makeAu(new URL("http://shadow1.stanford.edu/"), 42);
    CrawlWindow window = au.getCrawlSpec().getCrawlWindow();
    assertNotNull(window);
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestHighWireArchivalUnit.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
