/*
 * $Id: TestWrappedArchivalUnit.java,v 1.5 2004-09-29 18:58:19 tlipkis Exp $
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

package org.lockss.plugin.wrapper;

/** Test suite for WrappedArchivalUnit.  Based on TestHighWireArchivalUnit */

import java.io.File;
import java.net.*;
import java.util.*;
import gnu.regexp.*;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.state.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.*;
import org.lockss.plugin.base.*;
import org.lockss.plugin.highwire.*;
import org.lockss.repository.LockssRepositoryImpl;

public class TestWrappedArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;
  private WrappedArchivalUnit wau;

  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NUMBER.getKey();

  static final int TEST_VOL = 322;
  static final int TEST_YEAR = 2004;

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

  private WrappedArchivalUnit makeAu(URL url, int volume, int year)
      throws Exception {
    Properties props = new Properties();
    props.setProperty(VOL_KEY, Integer.toString(volume));
    props.setProperty(YEAR_KEY, Integer.toString(year));
    if (url != null) {
      props.setProperty(BASE_URL_KEY, url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    DefinablePlugin hplug = new DefinablePlugin();
    hplug.initPlugin(theDaemon,"org.lockss.plugin.highwire.HighWirePlugin");
    WrappedPlugin wplug = (WrappedPlugin)WrapperState.getWrapper(hplug);
    WrappedArchivalUnit madeau = (WrappedArchivalUnit)wplug.createAu(config);
    return madeau;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, 1,TEST_YEAR);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }

  public void testConstructNegativeVolume() throws Exception {
    URL url = new URL("http://www.example.com/");
    try {
      makeAu(url, -1,TEST_YEAR);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }


  WrappedUrlCacher makeUrlCacher(String url)
      throws Exception {
    URL base = new URL("http://shadow1.stanford.edu/");
    int volume = TEST_VOL;
    WrappedArchivalUnit hwAu = makeAu(base, volume, TEST_YEAR);
    theDaemon.getLockssRepository(hwAu);
    theDaemon.getNodeManager(hwAu);
    CachedUrlSetSpec spec = new RangeCachedUrlSetSpec(base.toString());
    BaseCachedUrlSet cus = new BaseCachedUrlSet(
        (DefinableArchivalUnit) hwAu.getOriginal(), spec);
     WrappedCachedUrlSet wcus = (WrappedCachedUrlSet)
         WrapperState.getWrapper(cus);
     return (WrappedUrlCacher)hwAu.getPlugin().makeUrlCacher(wcus,url);
  }

  public void testShouldCacheRootPage() throws Exception {
    WrappedUrlCacher uc = makeUrlCacher(
        "http://shadow1.stanford.edu/contents-by-date." + TEST_YEAR + ".shtml");
    assertTrue(uc.shouldBeCached());
  }

  public void testShouldNotCachePageFromOtherSite() throws Exception {
    WrappedUrlCacher uc = makeUrlCacher(
      "http://shadow2.stanford.edu/lockss-volume" + TEST_VOL+ ".shtml");
    assertFalse(uc.shouldBeCached());
  }

  public void testGetUrlStems() throws Exception {
    String stem1 = "http://www.example.com";
    WrappedArchivalUnit hwau1 = makeAu(new URL(stem1 + "/"), 10, TEST_YEAR);
    assertEquals(ListUtil.list(stem1), hwau1.getUrlStems());
    String stem2 = "http://www.example.com:8080";
    WrappedArchivalUnit hwau2 = makeAu(new URL(stem2 + "/"), 10, TEST_YEAR);
    assertEquals(ListUtil.list(stem2), hwau2.getUrlStems());
  }

  public void testGetNewContentCrawlUrls() throws Exception {
    URL url = new URL("http://www.example.com/");
    String expectedStr = "http://www.example.com/contents-by-date."
        + TEST_YEAR + ".shtml";
    WrappedArchivalUnit hwau = makeAu(url, 10, TEST_YEAR);
    assertEquals(expectedStr, hwau.getNewContentCrawlUrls().get(0));
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    WrappedArchivalUnit hwAu =
      makeAu(new URL("http://shadow1.stanford.edu/"), TEST_VOL, TEST_YEAR);

    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);

    assertFalse(hwAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlFor0() throws Exception {
    WrappedArchivalUnit hwAu =
      makeAu(new URL("http://shadow1.stanford.edu/"), TEST_VOL, TEST_YEAR);

    AuState aus = new MockAuState(null, 0, -1, -1, null);

    assertTrue(hwAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlEachMonth() throws Exception {
    WrappedArchivalUnit hwAu =
      makeAu(new URL("http://shadow1.stanford.edu/"), TEST_VOL, TEST_YEAR);

    AuState aus = new MockAuState(null, 4 * Constants.WEEK, -1, -1, null);

    assertTrue(hwAu.shouldCrawlForNewContent(aus));
  }

  public void testgetName() throws Exception {
    WrappedArchivalUnit au =
      makeAu(new URL("http://shadow1.stanford.edu/"), 42, TEST_YEAR);
    assertEquals("Wrapped shadow1.stanford.edu, vol. 42", au.getName());
    WrappedArchivalUnit au1 =
      makeAu(new URL("http://www.bmj.com/"), 42, TEST_YEAR);
    assertEquals("Wrapped www.bmj.com, vol. 42", au1.getName());
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestWrappedArchivalUnit.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
