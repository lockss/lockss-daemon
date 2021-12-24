/*
 * $Id: TestScDispatchingUrlFetcher.java 39864 2015-02-18 09:10:24Z thib_gc $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.plugin.silverchair;

import org.lockss.config.ConfigManager;
import org.lockss.daemon.CachedUrlSetSpec;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.plugin.UrlCacher;
import org.lockss.plugin.UrlData;
import org.lockss.plugin.base.BaseCachedUrl;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.plugin.base.PassiveUrlConsumerFactory;
import org.lockss.test.*;
import org.lockss.util.CIProperties;
import org.lockss.util.Logger;
import org.lockss.util.TimeBase;
import org.lockss.util.TimeZoneUtil;
import org.lockss.util.urlconn.CacheException;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.regex.Matcher;

public class TestScUrlFetcher extends LockssTestCase {
  protected static Logger logger = Logger.getLogger("TestScDispatchingUrlFetcher");

  private static final SimpleDateFormat GMT_DATE_PARSER =
    new SimpleDateFormat ("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
  static {
    GMT_DATE_PARSER.setTimeZone(TimeZoneUtil.getExactTimeZone("GMT"));
  }

  MockCachedUrlSet mcus;
  MockPlugin plugin;

  private MyMockArchivalUnit mau;
  private MockLockssDaemon theDaemon;
  private MockCrawler.MockCrawlerFacade mcf;

  private static final String TEST_URL = "http://www.example.com/testDir/leaf1";
  private boolean saveDefaultSuppressStackTrace;
  private ScUrlFetcher fetcher;

  public void setUp() throws Exception {
    super.setUp();

    String tempDirPath = setUpDiskSpace();

    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();

    mau = new MyMockArchivalUnit();

    mau.setConfiguration(ConfigManager.newConfiguration());

    plugin = new MockPlugin();
    plugin.initPlugin(theDaemon);
    mau.setPlugin(plugin);

    mcus = new MockCachedUrlSet(TEST_URL);
    mcus.setArchivalUnit(mau);
    mau.setAuCachedUrlSet(mcus);
    mcf = new MockCrawler().new MockCrawlerFacade(mau);
    fetcher = new ScUrlFetcher(mcf, TEST_URL);
    fetcher.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    saveDefaultSuppressStackTrace =
      CacheException.setDefaultSuppressStackTrace(false);
    getMockLockssDaemon().getAlertManager();
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    CacheException.setDefaultSuppressStackTrace(saveDefaultSuppressStackTrace);
    super.tearDown();
  }


  public void testPostMatcher() throws Exception {
    Matcher postMatch;
    // test a simple url
    String url = "http://www.example.com/issue.aspx?resourceId=12345";
    postMatch = ScUrlFetcher.PATTERN_POST.matcher(url);
    assertFalse(postMatch.find());

    //test jama
    String url_jama = "http://www.example.com/issue.aspx/SetArticlePDFLinkBasedOnAccess" +
                      "?iArticleId=12345&post=json";
    postMatch = ScUrlFetcher.PATTERN_POST.matcher(url_jama);
    assertTrue(postMatch.find());

    // test spie
    String url_spie = "http://www.example.com/volume.aspx/SetPDFLinkBasedOnAccess" +
                      "?'resourceId=12345&resourceType=type&post=json";
    postMatch =ScUrlFetcher.PATTERN_POST.matcher(url_spie);
    assertTrue(postMatch.find());

  }

  public void testQueryToJsonString() throws Exception {
    String base = "http://www.example.com/issue.aspx/SetArticlePDFLinkBasedOnAccess";
    String expected = "{'iArticleID' : 202091958}";
    // test JAMA
    String jsonUrl = base +
                     "?json=%7B%27iArticleID%27+%3A+202091958%7D&post=json";

    String postData = fetcher.queryToJsonString(jsonUrl);
    assertEquals(expected, postData);

    //test SPIE
    jsonUrl = base +
      "?json=%7B%27resourceId%27+%3A+2136060%2C+%27resourceType%27+%3A+%27Article%27+%7D&post=json";
    expected = "{'resourceId' : 2136060, 'resourceType' : 'Article' }";
    postData = fetcher.queryToJsonString(jsonUrl);

    assertEquals(expected, postData);

  }


  private class MyMockArchivalUnit extends MockArchivalUnit {
    boolean returnRealCachedUrl = false;

    public CachedUrlSet makeCachedUrlSet(CachedUrlSetSpec cuss) {
      return new BaseCachedUrlSet(this, cuss);
    }

    public UrlCacher makeUrlCacher(UrlData ud) {
      return new MockUrlCacher(mau, ud);
    }

    public CachedUrl makeCachedUrl(String url) {
      if (returnRealCachedUrl) {
        return new BaseCachedUrl(this, url);
      } else {
        return super.makeCachedUrl(url);
      }
    }
  }

}
