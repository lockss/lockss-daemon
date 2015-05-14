/*
 * $Id: TestProjectMuseArchivalUnit.java 41810 2015-05-01 18:18:04Z etenbrink $
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

import java.net.URL;
import java.util.*;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.plugin.wrapper.*;
import org.lockss.plugin.definable.*;
import org.lockss.state.AuState;

public class TestProjectMuseBooksArchivalUnit extends LockssTestCase {
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String EISBN_KEY = "eisbn";

  private MockLockssDaemon theDaemon;

  static final String HTTP_ROOT = "http://muse.jhu.edu/";
  static final String HTTPS_ROOT = "https://muse.jhu.edu/";

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();

    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(URL baseUrl, String eisbn)
      throws Exception {
    Properties props = new Properties();
    props.setProperty(EISBN_KEY, eisbn);
    if (baseUrl!=null) {
      props.setProperty(BASE_URL_KEY, baseUrl.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(theDaemon,"org.lockss.plugin.projmuse.ProjectMuseBooksPlugin");
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, "99123499");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }

  public void testShouldCacheProperPages() throws Exception {
    URL base = new URL(HTTP_ROOT);
    ArchivalUnit pmAu = makeAu(base, "99123499");
    theDaemon.getLockssRepository(pmAu);
    theDaemon.getNodeManager(pmAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(pmAu,
        new RangeCachedUrlSetSpec(base.toString()));

    String baseUrl = HTTP_ROOT + "books/99123499/";

    // root page
    shouldCacheTest(HTTP_ROOT + "books/lockss_books.html", true, pmAu, cus);

    // book chapters landing page
    shouldCacheTest(baseUrl+"99123499", true, pmAu, cus);
    shouldCacheTest(baseUrl+"99123499/", true, pmAu, cus);

    // article pdf
    shouldCacheTest(baseUrl+"99123499-1.pdf", true, pmAu, cus);

    // should not cache these
    // index links
    shouldCacheTest("http://www.press.jhu.edu/cgi-bin/redirect.pl", false,
                    pmAu, cus);
    shouldCacheTest(HTTP_ROOT+"books/indexold.html", false, pmAu, cus);

    shouldCacheTest(HTTP_ROOT+"journals", false, pmAu, cus);

    // LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, pmAu, cus);

    // other site
    shouldCacheTest("http://muse2.jhu.edu/", false, pmAu, cus);
  }

  private void shouldCacheTest(String url, boolean shouldCache,
			       ArchivalUnit au, CachedUrlSet cus) {
    assertTrue(au.shouldBeCached(url)==shouldCache);
  }

  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(HTTP_ROOT);
    String expectedPath;
    DefinableArchivalUnit pmAu;
    
    expectedPath = "books/lockss_books.html";
    pmAu = makeAu(url, "123");
    assertEquals(Arrays.asList(HTTP_ROOT + expectedPath, HTTPS_ROOT + expectedPath),
                 pmAu.getStartUrls());

    pmAu = makeAu(url, "9999999999");
    assertEquals(Arrays.asList(HTTP_ROOT + expectedPath, HTTPS_ROOT + expectedPath),
                 pmAu.getStartUrls());
  }

  public void testGetUrlStems() throws Exception {
    String stem1a = "http://muse.jhu.edu/";
    String stem1b = "https://muse.jhu.edu/";
    DefinableArchivalUnit pmAu1 = makeAu(new URL(stem1a + "foo/"), "60");
    assertSameElements(Arrays.asList(stem1a, stem1b), pmAu1.getUrlStems());
    String stem2a = "http://muse.jhu.edu:8080/";
    String stem2b = "https://muse.jhu.edu:8080/";
    DefinableArchivalUnit pmAu2 = makeAu(new URL(stem2a), "60");
    assertSameElements(Arrays.asList(stem2a, stem2b), pmAu2.getUrlStems());
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    ArchivalUnit pmAu = makeAu(new URL(HTTP_ROOT), "60");
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(pmAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlForZero() throws Exception {
    ArchivalUnit pmAu = makeAu(new URL(HTTP_ROOT), "60");
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(pmAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlEachMonth() throws Exception {
    ArchivalUnit pmAu = makeAu(new URL(HTTP_ROOT), "60");
    AuState aus = new MockAuState(null, 4 * Constants.WEEK, -1, -1, null);
    assertTrue(pmAu.shouldCrawlForNewContent(aus));
  }

  public void testGetName() throws Exception {
    DefinableArchivalUnit au = makeAu(new URL(HTTP_ROOT), "60");
    assertEquals("Project Muse Books Plugin, Base URL http://muse.jhu.edu/, eISBN 60", au.getName());
    DefinableArchivalUnit au1 =
        makeAu(new URL("http://www.bmj.com/"), "61");
    assertEquals("Project Muse Books Plugin, Base URL http://www.bmj.com/, eISBN 61", au1.getName());
  }

  public void testGetFilterRules() throws Exception {
    DefinableArchivalUnit au = makeAu(new URL(HTTP_ROOT), "60");
    assertTrue(WrapperUtil.unwrap(au.getHashFilterFactory("text/html"))
	       instanceof ProjectMuseHtmlHashFilterFactory);
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestProjectMuseBooksArchivalUnit.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
