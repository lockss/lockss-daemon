/*
 * $Id: TestBlackwellArchivalUnit.java,v 1.8 2007-05-08 23:54:21 troberts Exp $
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
  static final String JOURNAL_ID_KEY = "journal_id";
  static final String JOURNAL_ISSN_KEY = "journal_issn";
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();

  static final String BASE_URL = "http://blackwell-synergy.com/";
  static final String JOURNAL_ID = "pace";
  static final String ISSN = "1540-8159";
  static final String VOLUME = "78";
  static final String YEAR = "2005";

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

  private Properties stdConfig() {
    return makeConfig(BASE_URL, JOURNAL_ID, ISSN, YEAR, VOLUME);
  }

  private Properties makeConfig(String url, String journal, String issn,
				String year, String volume) {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, url);
    props.setProperty(JOURNAL_ID_KEY, journal);
    props.setProperty(JOURNAL_ISSN_KEY, issn);
    props.setProperty(YEAR_KEY, year);
    props.setProperty(VOLUME_NAME_KEY, volume);
    return props;
  }

  private DefinableArchivalUnit makeAu(String url, String journal,
				       String issn, String year, String volume)
      throws Exception {
    return makeAu(makeConfig(url, journal, issn, year, volume));
  }

  private DefinableArchivalUnit makeAu(Properties props) throws Exception {
    Configuration config = ConfigurationUtil.fromProps(props);
    DefinablePlugin plugin = new DefinablePlugin();
    plugin.initPlugin(theDaemon,"org.lockss.plugin.blackwell.BlackwellPlugin");
    DefinableArchivalUnit au = (DefinableArchivalUnit)plugin.createAu(config);
    return au;
  }

  public void testGoodConfig() throws Exception {
    ArchivalUnit au = makeAu(stdConfig());
    Configuration auconf = au.getConfiguration();
  }


  public void testNullParam(String key) throws Exception {
    testIllParam(key, null);
  }

  public void testIllParam(String key, String val) throws Exception {
    Properties p = stdConfig();
    if (val == null) {
      p.remove(key);
    } else {
      p.put(key, val);
    }
    try {
      makeAu(p);
      fail("Shouldn't create AU with null param: " + key);
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }

  public void testNullParams() throws Exception {
    testNullParam(BASE_URL_KEY);
    testNullParam(JOURNAL_ID_KEY);
    testNullParam(JOURNAL_ISSN_KEY);
    testNullParam(VOLUME_NAME_KEY);
  }

  public void testIllParams() throws Exception {
    testIllParam(BASE_URL_KEY, "not a url");
    testIllParam(JOURNAL_ID_KEY, "");
    testIllParam(JOURNAL_ISSN_KEY, "");
    testIllParam(VOLUME_NAME_KEY, "");
  }


  public void testShouldCacheProperPages() throws Exception {
    DefinableArchivalUnit au = makeAu(BASE_URL, JOURNAL_ID, ISSN,
				      YEAR, "42");

    theDaemon.getLockssRepository(au);
    theDaemon.getNodeManager(au);
    BaseCachedUrlSet cus =
      new BaseCachedUrlSet(au, new RangeCachedUrlSetSpec(BASE_URL));

    String baseUrl = BASE_URL;

    // manifest page
    List manifests = ((SpiderCrawlSpec)au.getCrawlSpec()).getStartingUrls();
    for (Iterator iter = manifests.iterator(); iter.hasNext();) {
      String url = (String)iter.next();
      assertShouldCache(url, au, cus);
    }
    assertShouldCache(baseUrl+"clockss/pace/42/manifest.html", au, cus);
    assertShouldNotCache(baseUrl+"clockss/pace/41/manifest.html", au, cus);

    // issue toc pages
    assertShouldCache(baseUrl+"toc/pace/42/6", au, cus);
    assertShouldCache(baseUrl+"toc/pace/42/1", au, cus);
    assertShouldCache(baseUrl+"toc/pace/42/2s", au, cus);
    assertShouldNotCache(baseUrl+"toc/pace/41/5", au, cus);
    assertShouldNotCache(baseUrl+"toc/pace/43", au, cus);

    // abs, full text, etc.
    assertShouldCache(baseUrl+"doi/abs/10.1111/j.1540-8159.2005.00355",
		      au, cus);
    assertShouldCache(baseUrl+"doi/ref/10.1111/j.1540-8159.2005.00355",
		      au, cus);
    assertShouldCache(baseUrl+"doi/full/10.1111/j.1540-8159.2005.00355",
		      au, cus);
    assertShouldCache(baseUrl+"doi/pdf/10.1111/j.1540-8159.2005.00355",
		      au, cus);
    // we now ignore the ISSN
    assertShouldCache(baseUrl+"doi/pdf/10.1111/j.1540-7234.2005.00355",
			 au, cus);

    //Now we ignore the year
    assertShouldCache(baseUrl+"doi/pdf/10.1111/j.1540-8159.2006.00355",
			 au, cus);

    assertShouldNotCache(baseUrl+"template", au, cus);
    assertShouldNotCache(baseUrl+"help", au, cus);
    assertShouldNotCache(baseUrl+"template/foo", au, cus);
    assertShouldNotCache(baseUrl+"help/foo", au, cus);


    assertShouldNotCache(baseUrl+"feedback", au, cus);
    assertShouldNotCache(baseUrl+"servlet", au, cus);
    assertShouldNotCache(baseUrl+"search", au, cus);
    assertShouldNotCache(baseUrl+"feedback/", au, cus);
    assertShouldNotCache(baseUrl+"servlet/", au, cus);
    assertShouldNotCache(baseUrl+"search/", au, cus);

    // images, css, others by file extension
    assertShouldCache(baseUrl+"images/spacer.gif", au, cus);
    assertShouldCache(baseUrl+"foo/bar.css", au, cus);
    assertShouldCache(baseUrl+"foo/bar.ico", au, cus);
    assertShouldCache(baseUrl+"foo.gif", au, cus);
    assertShouldCache(baseUrl+"foo/bar.jpg", au, cus);
    assertShouldCache(baseUrl+"foo/bar.jpeg", au, cus);
    assertShouldCache(baseUrl+"foo/bar.png", au, cus);

    assertShouldNotCache(baseUrl+"images/spacer.gof", au, cus);
    assertShouldNotCache(baseUrl+"foo/bar.noncss", au, cus);
  }

  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(BASE_URL);

    String expectedStr = BASE_URL+"clockss/pace/25/manifest.html";
    DefinableArchivalUnit au = makeAu(BASE_URL, JOURNAL_ID, ISSN,
				      YEAR, "25");
    assertEquals(expectedStr,
		 ((SpiderCrawlSpec)au.getCrawlSpec()).getStartingUrls().get(0));
  }

  public void testGetUrlStems() throws Exception {
    String stem1 = "http://www.blackwell.com/";
    DefinableArchivalUnit au1 = makeAu(stem1 + "foo/", JOURNAL_ID, ISSN, YEAR, "2");
    assertEquals(ListUtil.list(stem1), au1.getUrlStems());
    String stem2 = "http://www.blackwell.com:8080/";
    DefinableArchivalUnit au2 = makeAu(stem2, JOURNAL_ID, ISSN, YEAR, "2");
    assertEquals(ListUtil.list(stem2), au2.getUrlStems());
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    ArchivalUnit au = makeAu(BASE_URL, JOURNAL_ID, ISSN, YEAR, "2");
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(au.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlForZero() throws Exception {
    ArchivalUnit au = makeAu(BASE_URL, JOURNAL_ID, ISSN, YEAR, "2");
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(au.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlEachMonth() throws Exception {
    ArchivalUnit au = makeAu(BASE_URL, JOURNAL_ID, ISSN, YEAR, "2");
    AuState aus = new MockAuState(null, 4 * Constants.WEEK, -1, -1, null);
    assertTrue(au.shouldCrawlForNewContent(aus));
  }

  public void testGetName() throws Exception {
    ArchivalUnit au = makeAu(BASE_URL, JOURNAL_ID, ISSN, YEAR, "7");
    assertEquals("pace, vol. 7", au.getName());
    ArchivalUnit au1 = makeAu(BASE_URL, "jopy", ISSN, YEAR, "3");
    assertEquals("jopy, vol. 3", au1.getName());
  }

  public void testGetFilterFactory() throws Exception {
    ArchivalUnit au = makeAu(BASE_URL, JOURNAL_ID, ISSN, YEAR, "2");
    assertNull(au.getFilterFactory(null));
    assertNull(au.getFilterFactory("jpg"));
    assertNotNull(au.getFilterFactory("text/html"));
  }

  public void testCrawlWindow() throws Exception {
    ArchivalUnit au = makeAu(BASE_URL, JOURNAL_ID, ISSN, YEAR, "2");
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
