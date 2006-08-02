/*
 * $Id: TestBlackwellArchivalUnit.java,v 1.2 2006-08-02 02:14:51 tlipkis Exp $
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

package org.lockss.plugin.blackwell;

import java.io.File;
import java.net.URL;
import java.util.*;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.state.AuState;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.plugin.definable.*;

public class TestBlackwellArchivalUnit extends LockssPluginTestCase {
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_KEY = "journal_id";
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();

  static final String ROOT_URL = "http://blackwell-synergy.com/";

  private MockLockssDaemon theDaemon;
  
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  private DefinableArchivalUnit makeAu(String url, String journal, int volume)
      throws Exception {
    return makeAu(url, journal, Integer.toString(volume));
  }

  private DefinableArchivalUnit makeAu(String url, String journal,
				       String volume)
      throws Exception {
    Properties props = new Properties();
    if (url!=null) {
      props.setProperty(BASE_URL_KEY, url);
    }
    props.setProperty(JOURNAL_KEY, journal);
    props.setProperty(VOL_KEY, volume);
    Configuration config = ConfigurationUtil.fromProps(props);
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(theDaemon,"org.lockss.plugin.blackwell.BlackwellPlugin");
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, "pace", 78);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }
/*
  public void testConstructNullVolume() throws Exception {
    try {
      makeAu(ROOT_URL, "pace", null);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }
*/

  public void testShouldCacheProperPages() throws Exception {
    ArchivalUnit bwAu = makeAu(ROOT_URL, "pace", 42);
    theDaemon.getLockssRepository(bwAu);
    theDaemon.getNodeManager(bwAu);
    BaseCachedUrlSet cus =
      new BaseCachedUrlSet(bwAu, new RangeCachedUrlSetSpec(ROOT_URL));

    String baseUrl = ROOT_URL;

    // manifest page
    List manifests = ((SpiderCrawlSpec)bwAu.getCrawlSpec()).getStartingUrls();
    for (Iterator iter = manifests.iterator(); iter.hasNext();) {
      String url = (String)iter.next();
      log.info("man: " + url);
      assertShouldCache(url, bwAu, cus);
    }
    assertShouldCache(baseUrl+"clockss/pace/42/manifest.html", bwAu, cus);
    assertShouldNotCache(baseUrl+"clockss/pace/41/manifest.html", bwAu, cus);

    // issue toc pages
    assertShouldCache(baseUrl+"toc/pace/42/6", bwAu, cus);
    assertShouldCache(baseUrl+"toc/pace/42/1", bwAu, cus);
    assertShouldCache(baseUrl+"toc/pace/42/2s", bwAu, cus);
    assertShouldNotCache(baseUrl+"toc/pace/41/5", bwAu, cus);
    assertShouldNotCache(baseUrl+"toc/pace/43", bwAu, cus);

    // abs, full text, etc.
    assertShouldCache(baseUrl+"doi/abs/10.1111/j.1467-6494.2005.00355",
		      bwAu, cus);
    assertShouldCache(baseUrl+"doi/ref/10.1111/j.1467-6494.2005.00355",
		      bwAu, cus);
    assertShouldCache(baseUrl+"doi/full/10.1111/j.1467-6494.2005.00355",
		      bwAu, cus);
    assertShouldCache(baseUrl+"doi/pdf/10.1111/j.1467-6494.2005.00355",
		      bwAu, cus);

    assertShouldCache(baseUrl+"template", bwAu, cus);
    assertShouldCache(baseUrl+"help", bwAu, cus);
    assertShouldCache(baseUrl+"template/foo", bwAu, cus);
    assertShouldCache(baseUrl+"help/foo", bwAu, cus);


    assertShouldNotCache(baseUrl+"feedback", bwAu, cus);
    assertShouldNotCache(baseUrl+"servlet", bwAu, cus);
    assertShouldNotCache(baseUrl+"search", bwAu, cus);
    assertShouldNotCache(baseUrl+"feedback/", bwAu, cus);
    assertShouldNotCache(baseUrl+"servlet/", bwAu, cus);
    assertShouldNotCache(baseUrl+"search/", bwAu, cus);

    // images, css, others by file extension
    assertShouldCache(baseUrl+"images/spacer.gif", bwAu, cus);
    assertShouldCache(baseUrl+"foo/bar.css", bwAu, cus);
    assertShouldCache(baseUrl+"foo/bar.ico", bwAu, cus);
    assertShouldCache(baseUrl+"foo.gif", bwAu, cus);
    assertShouldCache(baseUrl+"foo/bar.jpg", bwAu, cus);
    assertShouldCache(baseUrl+"foo/bar.jpeg", bwAu, cus);
    assertShouldCache(baseUrl+"foo/bar.png", bwAu, cus);

    assertShouldNotCache(baseUrl+"images/spacer.gof", bwAu, cus);
    assertShouldNotCache(baseUrl+"foo/bar.noncss", bwAu, cus);
  }

  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(ROOT_URL);

    String expectedStr = ROOT_URL+"clockss/pace/2/manifest.html";
    DefinableArchivalUnit bwAu = makeAu(ROOT_URL, "pace", 2);
    assertEquals(expectedStr,
		 ((SpiderCrawlSpec)bwAu.getCrawlSpec()).getStartingUrls().get(0));
  }

  public void testGetUrlStems() throws Exception {
    String stem1 = "http://www.blackwell.com";
    DefinableArchivalUnit bwAu1 = makeAu(stem1 + "/", "pace", 2);
    assertEquals(ListUtil.list(stem1), bwAu1.getUrlStems());
    String stem2 = "http://www.blackwell.com:8080";
    DefinableArchivalUnit bwAu2 = makeAu(stem2 + "/", "pace", 2);
    assertEquals(ListUtil.list(stem2), bwAu2.getUrlStems());
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    ArchivalUnit bwAu = makeAu(ROOT_URL, "pace", 2);
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(bwAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlForZero() throws Exception {
    ArchivalUnit bwAu = makeAu(ROOT_URL, "pace", 2);
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(bwAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlEachMonth() throws Exception {
    ArchivalUnit bwAu = makeAu(ROOT_URL, "pace", 2);
    AuState aus = new MockAuState(null, 4 * Constants.WEEK, -1, -1, null);
    assertTrue(bwAu.shouldCrawlForNewContent(aus));
  }

  public void testGetName() throws Exception {
    DefinableArchivalUnit au = makeAu(ROOT_URL, "pace", 7);
    assertEquals("pace, vol. 7", au.getName());
    DefinableArchivalUnit au1 = makeAu("http://www.foo.com/", "jopy", 3);
    assertEquals("jopy, vol. 3", au1.getName());
  }

  public void testGetFilterRules() throws Exception {
    DefinableArchivalUnit au = makeAu(ROOT_URL, "pace", 2);
    assertNull(au.getFilterRule(null));
    assertNull(au.getFilterRule("jpg"));
    assertNotNull(au.getFilterRule("text/html"));
  }

  public void testCrawlWindow() throws Exception {
    DefinableArchivalUnit au = makeAu(ROOT_URL, "pace", 2);
    CrawlWindow window = au.getCrawlSpec().getCrawlWindow();
    assertNotNull(window);
    log.info("window: " + window.getClass());
    assertTrue(window instanceof CrawlWindows.Or);
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestBlackwellArchivalUnit.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
