/*
 * $Id: TestHighWireArchivalUnit.java,v 1.30 2003-09-19 22:39:52 eaalto Exp $
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

package org.lockss.plugin.highwire;

import java.io.File;
import java.net.*;
import java.util.*;
import gnu.regexp.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.state.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.repository.LockssRepositoryImpl;

public class TestHighWireArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

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

  public void testGetVolumeNum() {

  }

  private HighWireArchivalUnit makeAU(URL url, int volume)
      throws ArchivalUnit.ConfigurationException {
    Properties props = new Properties();
    props.setProperty(HighWirePlugin.AUPARAM_VOL, Integer.toString(volume));
    if (url != null) {
      props.setProperty(HighWirePlugin.AUPARAM_BASE_URL, url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    HighWireArchivalUnit au = new HighWireArchivalUnit(new HighWirePlugin());
    au.getPlugin().initPlugin(theDaemon);
    au.setConfiguration(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAU(null, 1);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }

  public void testConstructNegativeVolume() throws Exception {
    URL url = new URL("http://www.example.com/");
    try {
      makeAU(url, -1);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }


  public void testShouldCacheRootPage() throws Exception {
    URL base = new URL("http://shadow1.stanford.edu/");
    int volume = 322;
    ArchivalUnit hwAu = makeAU(base, volume);
    Plugin plugin = hwAu.getPlugin();
    theDaemon.getLockssRepository(hwAu);
    theDaemon.getNodeManager(hwAu);
    CachedUrlSetSpec spec = new RangeCachedUrlSetSpec(base.toString());
    BaseCachedUrlSet cus = new BaseCachedUrlSet(hwAu, spec);
    UrlCacher uc =
        plugin.makeUrlCacher(cus, "http://shadow1.stanford.edu/lockss-volume322.shtml");
    assertTrue(uc.shouldBeCached());
  }

  public void testShouldNotCachePageFromOtherSite() throws Exception {
    URL base = new URL("http://shadow1.stanford.edu/");
    int volume = 322;
    ArchivalUnit hwAu = makeAU(base, volume);
    Plugin plugin = hwAu.getPlugin();
    theDaemon.getLockssRepository(hwAu);
    theDaemon.getNodeManager(hwAu);
    CachedUrlSetSpec spec = new RangeCachedUrlSetSpec(base.toString());
    BaseCachedUrlSet cus = new BaseCachedUrlSet(hwAu, spec);
    UrlCacher uc =
      plugin.makeUrlCacher(cus, "http://shadow2.stanford.edu/lockss-volume322.shtml");
    assertFalse(uc.shouldBeCached());
  }

  public void testStartURLConstruction() throws Exception {
    URL url = new URL("http://www.example.com/");
    String expectedStr = "http://www.example.com/lockss-volume123.shtml";
    HighWireArchivalUnit hwau = makeAU(url, 10);
    assertEquals(expectedStr, hwau.makeStartUrl(url, 123));
  }

  public void testPathInUrlThrowsException() throws Exception {
    URL url = new URL("http://www.example.com/path");
    try {
      makeAU(url, 10);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch(ArchivalUnit.ConfigurationException e) {
    }
  }

  public void testGetUrlStems() throws Exception {
    String stem1 = "http://www.example.com";
    HighWireArchivalUnit hwau1 = makeAU(new URL(stem1 + "/"), 10);
    assertEquals(ListUtil.list(stem1), hwau1.getUrlStems());
    String stem2 = "http://www.example.com:8080";
    HighWireArchivalUnit hwau2 = makeAU(new URL(stem2 + "/"), 10);
    assertEquals(ListUtil.list(stem2), hwau2.getUrlStems());
  }

  public void testGetNewContentCrawlUrls() throws Exception {
    URL url = new URL("http://www.example.com/");
    String expectedStr = "http://www.example.com/lockss-volume10.shtml";
    HighWireArchivalUnit hwau = makeAU(url, 10);
    assertEquals(expectedStr, hwau.getNewContentCrawlUrls().get(0));
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    ArchivalUnit hwAu =
      makeAU(new URL("http://shadow1.stanford.edu/"), 322);

    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);

    assertFalse(hwAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlFor0() throws Exception {
    ArchivalUnit hwAu =
      makeAU(new URL("http://shadow1.stanford.edu/"), 322);

    AuState aus = new MockAuState(null, 0, -1, -1, null);

    assertTrue(hwAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlEachMonth() throws Exception {
    ArchivalUnit hwAu =
      makeAU(new URL("http://shadow1.stanford.edu/"), 322);

    AuState aus = new MockAuState(null, 4 * Constants.WEEK, -1, -1, null);

    assertTrue(hwAu.shouldCrawlForNewContent(aus));
  }

  public void testgetName() throws Exception {
    HighWireArchivalUnit au =
      makeAU(new URL("http://shadow1.stanford.edu/"), 42);
    assertEquals("shadow1.stanford.edu, vol. 42", au.getName());
    HighWireArchivalUnit au1 =
      makeAU(new URL("http://www.bmj.com/"), 42);
    assertEquals("www.bmj.com, vol. 42", au1.getName());
  }

 public void testGetFilterRuleNoContentType() throws Exception {
    HighWireArchivalUnit au =
      makeAU(new URL("http://shadow1.stanford.edu/"), 42);
    assertNull(au.getFilterRule(null));
  }

  public void testGetFilterRuleNonHtmlContentType() throws Exception {
    HighWireArchivalUnit au =
      makeAU(new URL("http://shadow1.stanford.edu/"), 42);
    assertNull(au.getFilterRule("jpg"));
  }

  public void testGetFilterRuleHtmlContentType() throws Exception {
    HighWireArchivalUnit au =
      makeAU(new URL("http://shadow1.stanford.edu/"), 42);
    assertTrue(au.getFilterRule("text/html") instanceof HighWireFilterRule);
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestHighWireArchivalUnit.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
