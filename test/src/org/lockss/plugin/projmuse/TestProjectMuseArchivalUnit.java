/*
 * $Id: TestProjectMuseArchivalUnit.java,v 1.9 2004-01-23 00:00:06 eaalto Exp $
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

package org.lockss.plugin.projmuse;

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

public class TestProjectMuseArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;

  static final String ROOT_URL = "http://muse.jhu.edu/";
  static final String DIR = "american_imago";

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

  private ProjectMuseArchivalUnit makeAu(URL url, int volume, String journalDir)
      throws ArchivalUnit.ConfigurationException {
    Properties props = new Properties();
    props.setProperty(ProjectMusePlugin.AUPARAM_VOL, Integer.toString(volume));
    if (url!=null) {
      props.setProperty(ProjectMusePlugin.AUPARAM_BASE_URL, url.toString());
    }
    if (journalDir!=null) {
      props.setProperty(ProjectMusePlugin.AUPARAM_JOURNAL_DIR, journalDir);
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    ProjectMuseArchivalUnit au = new ProjectMuseArchivalUnit(
        new ProjectMusePlugin());
    au.getPlugin().initPlugin(theDaemon);
    au.setConfiguration(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, 1, DIR);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }

  public void testConstructNegativeVolume() throws Exception {
    URL url = new URL(ROOT_URL);
    try {
      makeAu(url, -1, DIR);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }

  public void testConstructNullDir() throws Exception {
    URL url = new URL(ROOT_URL);
    try {
      makeAu(url, 1, null);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }

  public void testShouldCacheProperPages() throws Exception {
    URL base = new URL(ROOT_URL);
    int volume = 60;
    ArchivalUnit pmAu = makeAu(base, volume, DIR);
    theDaemon.getLockssRepository(pmAu);
    theDaemon.getNodeManager(pmAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(pmAu,
        new RangeCachedUrlSetSpec(base.toString()));

    String baseUrl = ROOT_URL + "journals/"+DIR+"/";

    // root page
    shouldCacheTest(baseUrl+"v060/", true, pmAu, cus);

    // volume page
    shouldCacheTest(baseUrl+"toc/aim60.1.html", true, pmAu, cus);
    // any other toc in this journal volume
    shouldCacheTest(baseUrl+"toc/sdf60.1.html", true, pmAu, cus);

    // article html
    shouldCacheTest(baseUrl+"v060/60.2zimmerman.html", true, pmAu, cus);

    // article pdf
    shouldCacheTest(baseUrl+"v060/60.2zimmerman.pdf", true, pmAu, cus);

    // images
    shouldCacheTest(ROOT_URL+"images/toolbar/barjournal.gif", true, pmAu, cus);
    shouldCacheTest(ROOT_URL+"images/journals/banners/aim.gif", true,
                    pmAu, cus);

    // cover material
    shouldCacheTest(baseUrl+"v060/60.1cover_art.html", true, pmAu, cus);
    shouldCacheTest(ROOT_URL+"images/journals/covers/aim60.1.gif", true,
                    pmAu, cus);

    // should not cache these
    // index links
    shouldCacheTest("http://www.press.jhu.edu/cgi-bin/redirect.pl", false,
                    pmAu, cus);
    shouldCacheTest(ROOT_URL+"journals/indexold.html", false, pmAu, cus);

    // archived root page
    shouldCacheTest(baseUrl+"v059/", false, pmAu, cus);

    // archived volume page
    shouldCacheTest(baseUrl+"toc/aim59.1.html", false, pmAu, cus);

    // button destinations
    shouldCacheTest(ROOT_URL+"ordering/index.html", false, pmAu, cus);
    shouldCacheTest(ROOT_URL+"journals", false, pmAu, cus);
    shouldCacheTest(ROOT_URL+"proj_descrip/contact.html", false, pmAu, cus);

    // LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, pmAu, cus);

    // substring matches
    shouldCacheTest(baseUrl+"v0601/", false, pmAu, cus);
    shouldCacheTest(baseUrl+"toc/aim601.1.html", false, pmAu, cus);
    shouldCacheTest(baseUrl+"toc/aim360.1.html", false, pmAu, cus);

    // other site
    shouldCacheTest("http://muse2.jhu.edu/", false, pmAu, cus);
  }

  private void shouldCacheTest(String url, boolean shouldCache,
			       ArchivalUnit au, CachedUrlSet cus) {
    UrlCacher uc = au.getPlugin().makeUrlCacher(cus, url);
    assertTrue(uc.shouldBeCached()==shouldCache);
  }

  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(ROOT_URL);

    // 2 digit
    String expectedStr = ROOT_URL+"journals/"+DIR+"/v060/";
    ProjectMuseArchivalUnit pmAu = makeAu(url, 60, DIR);
    assertEquals(expectedStr, pmAu.makeStartUrl());

    // 3 digit
    expectedStr = ROOT_URL+"journals/"+DIR+"/v601/";
    pmAu = makeAu(url, 601, DIR);
    assertEquals(expectedStr, pmAu.makeStartUrl());

    // 1 digit
    expectedStr = ROOT_URL+"journals/"+DIR+"/v006/";
    pmAu = makeAu(url, 6, DIR);
    assertEquals(expectedStr, pmAu.makeStartUrl());
  }

  public void testPathInUrlThrowsException() throws Exception {
    URL url = new URL(ROOT_URL+"path");
    try {
      makeAu(url, 60, DIR);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch(ArchivalUnit.ConfigurationException e) { }
  }

  public void testGetUrlStems() throws Exception {
    String stem1 = "http://muse.jhu.edu";
    ProjectMuseArchivalUnit pmAu1 = makeAu(new URL(stem1 + "/"), 60, DIR);
    assertEquals(ListUtil.list(stem1), pmAu1.getUrlStems());
    String stem2 = "http://muse.jhu.edu:8080";
    ProjectMuseArchivalUnit pmAu2 = makeAu(new URL(stem2 + "/"), 60, DIR);
    assertEquals(ListUtil.list(stem2), pmAu2.getUrlStems());
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    ArchivalUnit pmAu = makeAu(new URL(ROOT_URL), 60, DIR);
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(pmAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlForZero() throws Exception {
    ArchivalUnit pmAu = makeAu(new URL(ROOT_URL), 60, DIR);
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(pmAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlEachMonth() throws Exception {
    ArchivalUnit pmAu = makeAu(new URL(ROOT_URL), 60, DIR);
    AuState aus = new MockAuState(null, 4 * Constants.WEEK, -1, -1, null);
    assertTrue(pmAu.shouldCrawlForNewContent(aus));
  }

  public void testGetName() throws Exception {
    ProjectMuseArchivalUnit au = makeAu(new URL(ROOT_URL), 60, DIR);
    assertEquals("muse.jhu.edu, american_imago, vol. 60", au.getName());
    ProjectMuseArchivalUnit au1 =
        makeAu(new URL("http://www.bmj.com/"), 61, "bmj");
    assertEquals("www.bmj.com, bmj, vol. 61", au1.getName());
  }

  public void testGetFilterRules() throws Exception {
    ProjectMuseArchivalUnit au = makeAu(new URL(ROOT_URL), 60, DIR);
    assertNull(au.getFilterRule(null));
    assertNull(au.getFilterRule("jpg"));
    assertTrue(au.getFilterRule("text/html") instanceof ProjectMuseFilterRule);
  }

  public void testRefetchDepth() throws Exception {
    ProjectMuseArchivalUnit au = makeAu(new URL(ROOT_URL), 60, DIR);
    assertEquals(2, au.getCrawlSpec().getRefetchDepth());
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestProjectMuseArchivalUnit.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
