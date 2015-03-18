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

package org.lockss.plugin.projmuse;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.plugin.wrapper.*;
import org.lockss.plugin.definable.*;
import org.lockss.state.AuState;
import org.lockss.repository.LockssRepositoryImpl;

public class TestProjectMuseArchivalUnit extends LockssTestCase {
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JRNL_KEY = ConfigParamDescr.JOURNAL_DIR.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NUMBER.getKey();

  private MockLockssDaemon theDaemon;

  static final String ROOT_URL = "http://muse.jhu.edu/";
  static final String DIR = "american_imago";

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();

    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(URL url, int volume, String journalDir)
      throws Exception {
    Properties props = new Properties();
    props.setProperty(VOL_KEY, Integer.toString(volume));
    if (url!=null) {
      props.setProperty(BASE_URL_KEY, url.toString());
    }
    if (journalDir!=null) {
      props.setProperty(JRNL_KEY, journalDir);
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(theDaemon,"org.lockss.plugin.projmuse.ProjectMusePlugin");
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
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
    shouldCacheTest(baseUrl+"toc/similartoaim60.1.html", true, pmAu, cus);
    shouldCacheTest(baseUrl+"toc/similartoaim.60.1.html", true, pmAu, cus);

    // other site
    shouldCacheTest("http://muse2.jhu.edu/", false, pmAu, cus);
  }

  private void shouldCacheTest(String url, boolean shouldCache,
			       ArchivalUnit au, CachedUrlSet cus) {
    assertTrue(au.shouldBeCached(url)==shouldCache);
  }

  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(ROOT_URL);

    // 2 digit
    String expectedStr = ROOT_URL+"journals/"+DIR+"/v060/";
    DefinableArchivalUnit pmAu = makeAu(url, 60, DIR);
    assertEquals(ListUtil.list(expectedStr), pmAu.getStartUrls());

    // 3 digit
    expectedStr = ROOT_URL+"journals/"+DIR+"/v601/";
    pmAu = makeAu(url, 601, DIR);
    assertEquals(ListUtil.list(expectedStr), pmAu.getStartUrls());

    // 1 digit
    expectedStr = ROOT_URL+"journals/"+DIR+"/v006/";
    pmAu = makeAu(url, 6, DIR);
    assertEquals(ListUtil.list(expectedStr), pmAu.getStartUrls());
  }

  public void testGetUrlStems() throws Exception {
    String stem1 = "http://muse.jhu.edu/";
    DefinableArchivalUnit pmAu1 = makeAu(new URL(stem1 + "foo/"), 60, DIR);
    assertEquals(ListUtil.list(stem1), pmAu1.getUrlStems());
    String stem2 = "http://muse.jhu.edu:8080/";
    DefinableArchivalUnit pmAu2 = makeAu(new URL(stem2), 60, DIR);
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
    DefinableArchivalUnit au = makeAu(new URL(ROOT_URL), 60, DIR);
    assertEquals("Project Muse Journals Plugin, Base URL http://muse.jhu.edu/, Journal ID american_imago, Volume 60", au.getName());
    DefinableArchivalUnit au1 =
        makeAu(new URL("http://www.bmj.com/"), 61, "bmj");
    assertEquals("Project Muse Journals Plugin, Base URL http://www.bmj.com/, Journal ID bmj, Volume 61", au1.getName());
  }

  public void testGetFilterRules() throws Exception {
    DefinableArchivalUnit au = makeAu(new URL(ROOT_URL), 60, DIR);
    assertTrue(WrapperUtil.unwrap(au.getHashFilterFactory("text/html"))
	       instanceof ProjectMuseHtmlHashFilterFactory);
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestProjectMuseArchivalUnit.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
