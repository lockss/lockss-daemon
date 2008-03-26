/*
 * $Id: TestFollowLinkCrawler.java,v 1.28 2008-03-26 04:51:06 tlipkis Exp $
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

package org.lockss.crawler;
import java.util.*;
import java.io.*;

import org.apache.commons.collections.set.*;

import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.test.*;
import org.lockss.state.*;
import org.lockss.util.urlconn.*;

public class TestFollowLinkCrawler extends LockssTestCase {

  private MyMockArchivalUnit mau = null;
  private MockCachedUrlSet mcus = null;
  private CrawlSpec spec = null;
  private MockAuState aus = new MockAuState();
  private static List testUrlList = ListUtil.list("http://example.com");
  private MockCrawlRule crawlRule = null;
  private String startUrl = "http://www.example.com/index.html";
  private List startUrls;
  private TestableFollowLinkCrawler crawler = null;
  private MockLinkExtractor extractor = new MockLinkExtractor();

  private static final String PARAM_RETRY_TIMES =
    BaseCrawler.PARAM_DEFAULT_RETRY_COUNT;
  private static final int DEFAULT_RETRY_TIMES =
    BaseCrawler.DEFAULT_DEFAULT_RETRY_COUNT;;

  public void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated(10);

    getMockLockssDaemon().getAlertManager();

    MockPlugin plug = new MockPlugin(getMockLockssDaemon());
    plug.initPlugin(getMockLockssDaemon());
    mau = new MyMockArchivalUnit();
    mau.setPlugin(plug);
    mau.setAuId("MyMockTestAu");
    startUrls = ListUtil.list(startUrl);
    mcus = (MockCachedUrlSet)mau.getAuCachedUrlSet();

    crawlRule = new MockCrawlRule();
    crawlRule.addUrlToCrawl(startUrl);
    spec = new SpiderCrawlSpec(startUrls, startUrls, crawlRule, 1);
    crawler = new TestableFollowLinkCrawler(mau, spec, aus);
    ((BaseCrawler)crawler).daemonPermissionCheckers =
      ListUtil.list(new MyMockPermissionChecker(1));

    mau.setCrawlSpec(spec);
    mau.setLinkExtractor("text/html", extractor);
    Properties p = new Properties();
    p.setProperty(FollowLinkCrawler.PARAM_DEFAULT_RETRY_DELAY, "0");
    p.setProperty(FollowLinkCrawler.PARAM_MIN_RETRY_DELAY, "0");
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }

  public void testFlcThrowsForNullAu() {
    try {
      crawler = new TestableFollowLinkCrawler(null, spec, new MockAuState());
      fail("Constructing a FollowLinkCrawler with a null ArchivalUnit"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testFlcThrowsForNullCrawlSpec() {
    try {
      crawler = new TestableFollowLinkCrawler(mau, null, new MockAuState());
      fail("Calling makeTestableFollowLinkCrawler with a null CrawlSpec"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testFlcThrowsForNullAuState() {
    try {
      crawler = new TestableFollowLinkCrawler(mau, spec, null);
      fail("Calling makeTestableFollowLinkCrawler with a null AuState"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testReturnsProperType() {
    try {
      crawler.getType();
      fail("Calling getType() ," +
	   " which should not be implemented in FollowLinkCrawler");
    } catch (UnsupportedOperationException usoe) {
    }
  }

  public void testMakeUrlCacher() {
    mau.addUrl(startUrl);
    crawler.makeUrlCacher(startUrl);
    MyMockUrlCacher mmuc = mau.lastMmuc;
    assertNull(mmuc.proxyHost);
  }

  public void testMakeUrlCacherProxy() {
    Properties p = new Properties();
    p.put(FollowLinkCrawler.PARAM_PROXY_ENABLED, "true");
    p.put(FollowLinkCrawler.PARAM_PROXY_HOST, "pr.wub");
    p.put(FollowLinkCrawler.PARAM_PROXY_PORT, "27");
    ConfigurationUtil.addFromProps(p);

    crawler.setCrawlConfig(ConfigManager.getCurrentConfig());
    mau.addUrl(startUrl);
    crawler.makeUrlCacher(startUrl);
    MyMockUrlCacher mmuc = mau.lastMmuc;
    assertEquals("pr.wub", mmuc.proxyHost);
    assertEquals(27, mmuc.proxyPort);
  }

  public void testReturnsTrueWhenCrawlSuccessful() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    mau.addUrl(startUrl, false, true);
    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1));
    mau.addUrl(url1, false, true);

    assertTrue(doCrawl0(crawler));
   }

  //Fetch startUrl, extractor will return a single url that already exists
  //we should only cache startUrl
  public void testDoesNotCacheExistingFile() {
    String url1="http://www.example.com/blah.html";

    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, true, true);
    mau.addUrl(url1, true, true);

    assertTrue(crawler.doCrawl());

    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testHandlesRedirects() {
    String url1="http://www.example.com/blah.html";

    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, false, true);
    CIProperties props = new CIProperties();
    props.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    props.put(CachedUrl.PROPERTY_CONTENT_URL,
	      "http://www.example.com/extra_level/");
    mau.addUrl(url1, false, true, props);

    assertTrue(crawler.doCrawl());

    Set expected = SetUtil.set(startUrl);
    Set expectedSrcUrls = SetUtil.set("http://www.example.com/extra_level/");
    assertEquals(expectedSrcUrls, extractor.getSrcUrls());
  }

  CIProperties fromArgs(String prop, String val) {
    CIProperties props = new CIProperties();
    props.put(prop, val);
    return props;
  }

  public void testParseCharsetOff() {
    ConfigurationUtil.addFromArgs(FollowLinkCrawler.PARAM_PARSE_USE_CHARSET,
				  "false");
    MockLinkExtractor ext2 = new MockLinkExtractor();
    mau.setLinkExtractor("audio/inaudible", ext2);
    String url1="http://www.example.com/one.html";
    String url2="http://www.example.com/two.html";
    String url3="http://www.example.com/three.html";

    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1, url2, url3));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, false, true);
    mau.addUrl(url1, false, true, fromArgs(CachedUrl.PROPERTY_CONTENT_TYPE,
					   "text/html"));
    mau.addUrl(url2, false, true, fromArgs(CachedUrl.PROPERTY_CONTENT_TYPE,
					   "text/html;charset=utf-8"));
    mau.addUrl(url3, false, true, fromArgs(CachedUrl.PROPERTY_CONTENT_TYPE,
					   "audio/inaudible"));

    assertTrue(crawler.doCrawl());

    Set exp =
      SetUtil.set(ListUtil.list(mau, null, Constants.DEFAULT_ENCODING,
				"http://www.example.com/one.html"),
		  ListUtil.list(mau, null, Constants.DEFAULT_ENCODING,
				"http://www.example.com/two.html"));
    assertSameElements(exp, extractor.getArgs());

     exp =
      SetUtil.set(ListUtil.list(mau, null, Constants.DEFAULT_ENCODING,
				"http://www.example.com/three.html"));
    assertSameElements(exp, ext2.getArgs());
  }

  public void testParseCharsetOn() {
    ConfigurationUtil.addFromArgs(FollowLinkCrawler.PARAM_PARSE_USE_CHARSET,
				  "true");
    MockLinkExtractor ext2 = new MockLinkExtractor();
    mau.setLinkExtractor("audio/inaudible", ext2);
    String url1="http://www.example.com/one.html";
    String url2="http://www.example.com/two.html";
    String url3="http://www.example.com/three.html";

    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1, url2, url3));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, false, true);
    mau.addUrl(url1, false, true, fromArgs(CachedUrl.PROPERTY_CONTENT_TYPE,
					   "text/html"));
    mau.addUrl(url2, false, true, fromArgs(CachedUrl.PROPERTY_CONTENT_TYPE,
					   "text/html;charset=utf-8"));
    mau.addUrl(url3, false, true, fromArgs(CachedUrl.PROPERTY_CONTENT_TYPE,
					   "audio/inaudible"));

    assertTrue(crawler.doCrawl());

    Set exp =
      SetUtil.set(ListUtil.list(mau, null, Constants.DEFAULT_ENCODING,
				"http://www.example.com/one.html"),
		  ListUtil.list(mau, null, "utf-8",
				"http://www.example.com/two.html"));
    assertSameElements(exp, extractor.getArgs());

     exp =
      SetUtil.set(ListUtil.list(mau, null, Constants.DEFAULT_ENCODING,
				"http://www.example.com/three.html"));
    assertSameElements(exp, ext2.getArgs());
  }

  public void testReturnsFalseWhenFailingUnretryableExceptionThrown() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    mau.addUrl(startUrl, false, true);
    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1));
    MyMockUnretryableCacheException exception =
       new MyMockUnretryableCacheException("Test exception");
    mau.addUrl(url1, exception, DEFAULT_RETRY_TIMES);
    crawlRule.addUrlToCrawl(url1);

    assertFalse(doCrawl0(crawler));
  }

  public void testReturnsFalseWhenIOExceptionThrown() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    mau.addUrl(startUrl, false, true);
    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1));
    mau.addUrl(url1, new ExpectedIOException("Test exception"),
               DEFAULT_RETRY_TIMES);
    crawlRule.addUrlToCrawl(url1);

    assertFalse(doCrawl0(crawler));
  }

  public void testReturnsTrueWhenNonFailingUnretryableExceptionThrown() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    mau.addUrl(startUrl, false, true);
    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1));
    MyMockCacheException exception =
      new MyMockCacheException("Test exception");
    mau.addUrl(url1, exception, DEFAULT_RETRY_TIMES);
    crawlRule.addUrlToCrawl(url1);

    assertTrue(doCrawl0(crawler));
  }

  public void testReturnsRetriesWhenRetryableExceptionThrown() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    mau.addUrl(startUrl, false, true);
    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1));
    MyMockRetryableCacheException exception =
      new MyMockRetryableCacheException("Test exception");
    mau.addUrl(url1, exception, DEFAULT_RETRY_TIMES-1);
    crawlRule.addUrlToCrawl(url1);

    doCrawl0(crawler);
    Set expected = SetUtil.set(startUrl, url1);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testExitsWhenPermissionExceptionThrown() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    String url2="http://www.example.com/blah2.html";
    String url3="http://www.example.com/blah3.html";

    mau.addUrl(startUrl, false, true);

    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1,
								     url2,
								     url3));
    MyMockRetryableCacheException exception =
      new MyMockRetryableCacheException("Test exception");
    mau.addUrl(url1);
    crawlRule.addUrlToCrawl(url1);

    mau.addUrl(url2, new CacheException.PermissionException(),
	       DEFAULT_RETRY_TIMES+1);
    crawlRule.addUrlToCrawl(url2);

    mau.addUrl(url3);
    crawlRule.addUrlToCrawl(url3);

    assertFalse(doCrawl0(crawler));
  }


  public void testReturnsDoesntRetryWhenUnretryableExceptionThrown() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    mau.addUrl(startUrl, false, true);
    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1));

    // extractor.addUrlsToReturn(startUrl, SetUtil.set(url1));
    MyMockUnretryableCacheException exception =
      new MyMockUnretryableCacheException("Test exception");
    mau.addUrl(url1, exception, DEFAULT_RETRY_TIMES-1);
    crawlRule.addUrlToCrawl(url1);

    doCrawl0(crawler);
    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cus.getCachedUrls());
    //fail("fail to check what is in the cus : " + cus.getCachedUrls().toString() );
  }

  public void testRetryNumSetByParam() {
    int retryNum = DEFAULT_RETRY_TIMES + 3;
    assertTrue("Test is worthless unless retryNum is greater than "
	       +"DEFAULT_RETRY_TIMES", retryNum > DEFAULT_RETRY_TIMES);
    ConfigurationUtil.addFromArgs(PARAM_RETRY_TIMES, String.valueOf(retryNum));
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    mau.addUrl(startUrl, false, true);
    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1));
    mau.addUrl(url1,
	       new MyMockRetryableCacheException("Test exception"),
	       retryNum-1);
    crawlRule.addUrlToCrawl(url1);

    crawler.doCrawl();
    Set expected = SetUtil.set(startUrl, url1);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testRetryCountInException() {
    int retryNum = DEFAULT_RETRY_TIMES + 3;
    assertTrue("Test is worthless unless retryNum is greater than "
	       +"DEFAULT_RETRY_TIMES", retryNum > DEFAULT_RETRY_TIMES);
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    mau.addUrl(startUrl, false, true);
    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1));
    mau.addUrl(url1,
	       new MyMockRetryableCacheException("Test exception",
						 retryNum, 0),
	       retryNum-1);
    crawlRule.addUrlToCrawl(url1);

    crawler.doCrawl();
    Set expected = SetUtil.set(startUrl, url1);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testMaxRetryCountInException() {
    int MAX_RETRIES = BaseCrawler.DEFAULT_MAX_RETRY_COUNT;
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    mau.addUrl(startUrl, false, true);
    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1));
    mau.addUrl(url1,
	       new MyMockRetryableCacheException("Test exception",
						 MAX_RETRIES + 5, 0),
	       MAX_RETRIES-1);
    crawlRule.addUrlToCrawl(url1);

    crawler.doCrawl();
    Set expected = SetUtil.set(startUrl, url1);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testGetRetryCount() {
    int maxRetries = BaseCrawler.DEFAULT_MAX_RETRY_COUNT;
    CacheException ce;

    ce = new MyMockRetryableCacheException("Test exception",
					   maxRetries + 5,
					   0);
    assertEquals(maxRetries, crawler.getRetryCount(ce));

    maxRetries = BaseCrawler.DEFAULT_MAX_RETRY_COUNT + 47;

    ConfigurationUtil.addFromArgs(BaseCrawler.PARAM_MAX_RETRY_COUNT,
				  String.valueOf(maxRetries));
    crawler.setCrawlConfig(ConfigManager.getCurrentConfig());

    ce = new MyMockRetryableCacheException("Test exception",
					   maxRetries - 7,
					   0);
    assertEquals(maxRetries - 7, crawler.getRetryCount(ce));

    ce = new MyMockRetryableCacheException("Test exception",
					   maxRetries + 5,
					   0);
    assertEquals(maxRetries, crawler.getRetryCount(ce));

    ce = new MyMockRetryableCacheException("Test exception");
    assertEquals(BaseCrawler.DEFAULT_DEFAULT_RETRY_COUNT,
		 crawler.getRetryCount(ce));

    ConfigurationUtil.addFromArgs(BaseCrawler.PARAM_DEFAULT_RETRY_COUNT, "7");
    crawler.setCrawlConfig(ConfigManager.getCurrentConfig());

    ce = new MyMockRetryableCacheException("Test exception");
    assertEquals(7, crawler.getRetryCount(ce));
  }

  public void testGetRetryDelay() {
    long minRetryDelay = BaseCrawler.DEFAULT_MIN_RETRY_DELAY;
    CacheException ce;

    ce = new MyMockRetryableCacheException("Test exception",
					   0,
					   minRetryDelay - 5);
    assertEquals(minRetryDelay, crawler.getRetryDelay(ce));

    minRetryDelay = BaseCrawler.DEFAULT_MIN_RETRY_DELAY - 7;
    ConfigurationUtil.addFromArgs(BaseCrawler.PARAM_MIN_RETRY_DELAY,
				  String.valueOf(minRetryDelay));
    crawler.setCrawlConfig(ConfigManager.getCurrentConfig());

    ce = new MyMockRetryableCacheException("Test exception",
					   0,
					   minRetryDelay + 3);
    assertEquals(minRetryDelay + 3, crawler.getRetryDelay(ce));

    ce = new MyMockRetryableCacheException("Test exception",
					   0,
					   minRetryDelay - 2);
    assertEquals(minRetryDelay, crawler.getRetryDelay(ce));

    ConfigurationUtil.setCurrentConfigFromProps(new Properties());
    crawler.setCrawlConfig(ConfigManager.getCurrentConfig());

    ce = new MyMockRetryableCacheException("Test exception");
    assertEquals(BaseCrawler.DEFAULT_DEFAULT_RETRY_DELAY,
		 crawler.getRetryDelay(ce));

    ConfigurationUtil.addFromArgs(BaseCrawler.PARAM_DEFAULT_RETRY_DELAY,
				  "765432");
    crawler.setCrawlConfig(ConfigManager.getCurrentConfig());

    ce = new MyMockRetryableCacheException("Test exception");
    assertEquals(765432, crawler.getRetryDelay(ce));
  }

  public void testCachesFailedFetches() {
    int retryNum = 3;
    ConfigurationUtil.addFromArgs(PARAM_RETRY_TIMES,
				  String.valueOf(retryNum));
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    String url2="http://www.example.com/blah2.html";
    String url3="http://www.example.com/blah3.html";
    mau.addUrl(startUrl, false, true);
    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1, url2, url3));
    mau.addUrl(url2,
	       new CacheException.UnretryableException("Test exception"),
	       retryNum);
    extractor.addUrlsToReturn(url2, SetUtil.set(url1));
    mau.addUrl(url3,
	       new CacheException.RetryableException("Test exception"),
	       retryNum);
    extractor.addUrlsToReturn(url3, SetUtil.set(url1));
    mau.addUrl(url1, new ExpectedIOException("Test exception"), retryNum);

    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);

    crawler.doCrawl();
    // IOException should not be retried
    assertEquals(1, cus.getNumCacheAttempts(url1));
    // UnretryableException should not be retried
    assertEquals(1, cus.getNumCacheAttempts(url2));
    // RetryableException should be retried
    assertEquals(retryNum, cus.getNumCacheAttempts(url3));
  }


  public void testReturnsTrueWhenNonFailingExceptionThrown() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    String url2="http://www.example.com/alpha.html";
    mau.addUrl(startUrl, false, true);
    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1,
								     url2));
    mau.addUrl(url1,
	       new CacheException.NoRetryDeadLinkException("Test exception"),
	       1);
    mau.addUrl(url2);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);

    assertTrue(crawler.doCrawl());
    Set expected = SetUtil.set(startUrl, url2);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testPluginThrowsRuntimeException() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    mau.addUrl(startUrl, false, true);
    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1));
    mau.addUrl(url1, new ExpectedRuntimeException("Test exception"), 1);
    crawlRule.addUrlToCrawl(url1);

    assertFalse(crawler.doCrawl());
  }

  public void testWillNotParseExistingPagesForUrlsIfParam() {
    ConfigurationUtil.addFromArgs(TestableFollowLinkCrawler.PARAM_REPARSE_ALL,
				  "false");

    String url1 = "http://www.example.com/link3.html";
    String url2 = "http://www.example.com/link4.html";
    startUrls = ListUtil.list(startUrl);
    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1));
    extractor.addUrlsToReturn(url1, SetUtil.set(url2));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, true, true);
    mau.addUrl(url1, true, true);
    mau.addUrl(url2);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);

    assertTrue(crawler.doCrawl());
    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testWillParseExistingPagesForUrlsIfParam() {
    setProperty(TestableFollowLinkCrawler.PARAM_REPARSE_ALL, "true");

    String url1 = "http://www.example.com/link3.html";
    String url2 = "http://www.example.com/link4.html";
    startUrls = ListUtil.list(startUrl);
    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1));
    extractor.addUrlsToReturn(url1, SetUtil.set(url2));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, true, true);
    mau.addUrl(url1, true, true);
    mau.addUrl(url2);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);

    assertTrue(crawler.doCrawl());
    Set expected = SetUtil.set(startUrl, url2);
    assertEquals(expected, cus.getCachedUrls());
  }

  private static final String CW_URL1 = "http://www.example.com/link1.html";
  private static final String CW_URL2 = "http://www.example.com/link2.html";
  private static final String CW_URL3 = "http://www.example.com/link3.html";

  private void setUpCrawlWindowTest(CrawlWindow myCrawlWindow) {
    spec.setCrawlWindow(myCrawlWindow);
    mau.setCrawlSpec(spec);
    crawler = new TestableFollowLinkCrawler(mau, spec, aus);

    mau.addUrl(startUrl);

    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(CW_URL1, CW_URL2, CW_URL3));
    mau.addUrl(CW_URL1);
    mau.addUrl(CW_URL2);
    mau.addUrl(CW_URL3);
    crawlRule.addUrlToCrawl(CW_URL1);
    crawlRule.addUrlToCrawl(CW_URL2);
    crawlRule.addUrlToCrawl(CW_URL3);

    ((BaseCrawler)crawler).daemonPermissionCheckers =
      ListUtil.list(new MyMockPermissionChecker(100));
    crawler.doCrawl();
  }

  public void testCrawlWindow() {
    setUpCrawlWindowTest(new MyMockCrawlWindow(3));
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();

    // only gets 2 urls because start url 's canCrawl check is
    Set expected = SetUtil.set(startUrl, CW_URL1);
    assertEquals(expected, cus.getCachedUrls());

    CrawlerStatus crawlStatus = crawler.getStatus();
    assertEquals(Crawler.STATUS_WINDOW_CLOSED, crawlStatus.getCrawlStatus());
  }

  public void testCrawlWindowFetchNothing() {
    setUpCrawlWindowTest(new MyMockCrawlWindow(0));
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    Set expected = new HashSet();
    assertEquals(expected, cus.getCachedUrls());
    CrawlerStatus crawlStatus = crawler.getStatus();
    assertEquals(Crawler.STATUS_WINDOW_CLOSED, crawlStatus.getCrawlStatus());
  }

  public void testCrawlWindowFetchOnePermissionPage() {
    setUpCrawlWindowTest(new MyMockCrawlWindow(1));
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    Set expected = new HashSet();
    assertEquals(expected, cus.getCachedUrls());
    CrawlerStatus crawlStatus = crawler.getStatus();
    assertEquals(Crawler.STATUS_WINDOW_CLOSED, crawlStatus.getCrawlStatus());
  }

  public void testOutsideOfWindowAfterGetUrlsToFollow() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();

    String url1= "http://www.example.com/link1.html";
    spec.setCrawlWindow(new MyMockCrawlWindow(0));
    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1));
    mau.addUrl(startUrl, false, true);
    mau.addUrl(url1);
    assertFalse(crawler.doCrawl());
  }

  public void testAborted1() {
    String url1= "http://www.example.com/link1.html";
    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1));
    mau.addUrl(startUrl, true, true);
    crawler.abortCrawl();
    assertFalse(crawler.doCrawl());
    assertEmpty(mcus.getCachedUrls());
  }

  public void testAborted2() {
    String url1= "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link4.html";
    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1,
								     url2));
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();

    mau.addUrl(startUrl, true, true);
    mau.addUrl(url1);
    mau.addUrl(url2);
    // make it abort after either url1 or url2 cached.  (Can't predict
    // order, so do both.)
    MyMockUrlCacher mmuc = (MyMockUrlCacher)mau.makeUrlCacher(url1);
    mmuc.abortCrawl = true;
    mmuc = (MyMockUrlCacher)mau.makeUrlCacher(url2);
    mmuc.abortCrawl = true;
    assertFalse(crawler.doCrawl());
    // should have cached startUrl and one of the others
    Set cached = cus.getCachedUrls();
    assertEquals(2, cached.size());
    assertTrue(cached.contains(startUrl));
  }

  private Set crawlUrls(List urls) {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl);
    Iterator it = urls.iterator();
    while (it.hasNext()) {
      String url = (String)it.next();
      mau.addUrl(url);
      crawlRule.addUrlToCrawl(url);
    }
    crawler.doCrawl();
    return cus.getCachedUrls();
  }

  public void testDoesCollectHttps() {
    //we will collect https eventually, it is not yet implemented though and
    //this test is to make sure an https url will not break the whole system

    String url1= "http://www.example.com/link1.html";
    String url2= "https://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1));
    extractor.addUrlsToReturn(url1, SetUtil.set(url1, url2, url3));
    assertEquals(SetUtil.set(startUrl, url1, url3),
		 crawlUrls(ListUtil.list(url1, url2, url3)));
  }

  public void testDoesCollectFtpAndGopher() {
    //we will collect ftp and gopher eventually, it is not yet implemented
    //though and this test is to make sure ftp gopher urls will not break
    //the whole system

    String url1= "http://www.example.com/link1.html";
    String url2= "ftp://www.example.com/link2.html";
    String url3= "gopher://www.example.com/link3.html";

    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1));
    extractor.addUrlsToReturn(url1, SetUtil.set(url1, url2, url3));
    assertEquals(SetUtil.set(startUrl, url1),
		 crawlUrls(ListUtil.list(url1, url2, url3)));
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
  }

  public void testDoesNotLoopOnSelfReferentialPage() {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1));
    extractor.addUrlsToReturn(url1, SetUtil.set(url1, url2, url3));
    assertEquals(SetUtil.set(startUrl, url1, url2, url3),
		 crawlUrls(ListUtil.list(url1, url2, url3)));
  }

  public void testDoesNotLoopOnSelfReferentialLoop() {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1, url2, url3, startUrl));
    extractor.addUrlsToReturn(url1, SetUtil.set(startUrl));
    assertEquals(SetUtil.set(startUrl, url1, url2, url3),
		 crawlUrls(ListUtil.list(url1, url2, url3)));
  }

  public void testCrawlListEmptyOnExit() {
    setProperty(TestableFollowLinkCrawler.PARAM_PERSIST_CRAWL_LIST, "true");
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1, url2, url3));
    crawlUrls(ListUtil.list(url1, url2, url3));
    assertEmpty(aus.getCrawlUrls());
  }

  public void testCrawlListPreservesUncrawledUrls() {
    setProperty(TestableFollowLinkCrawler.PARAM_PERSIST_CRAWL_LIST, "true");
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    spec.setCrawlWindow(new MyMockCrawlWindow(3)); //permission page & first URL
    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1, url2,
                                                                     url3));
    crawlUrls(ListUtil.list(url1, url2, url3));
    // assertEquals(SetUtil.set(url2, url3), aus.getCrawlUrls());
    // crawlUrls in AuState has to be a HashSet for historical reasons
    // so we can't predict the order in which the URLs will be crawled
    // so all we can do here is to check how many there are.
    assertEquals(2, aus.getCrawlUrls().size());
  }

  public void testCrawlListDoesntPreserveUncrawledUrlsIfParam() {
    setProperty(TestableFollowLinkCrawler.PARAM_PERSIST_CRAWL_LIST, "false");
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    spec.setCrawlWindow(new MyMockCrawlWindow(3));
    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1, url2,
                                                                     url3));
    crawlUrls(ListUtil.list(url1, url2, url3));
    assertEquals(SetUtil.set(), aus.getCrawlUrls());
  }

  public void testUpdatedCrawlListCalledForEachFetchIfParam() {
    setProperty(TestableFollowLinkCrawler.PARAM_PERSIST_CRAWL_LIST, "true");
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1, url2, url3));
    crawlUrls(ListUtil.list(url1, url2, url3));
    aus.assertUpdatedCrawlListCalled(3); //not called for startUrl
  }

  public void testUpdatedCrawlListCalledForEachFetch() {
    setProperty(TestableFollowLinkCrawler.PARAM_PERSIST_CRAWL_LIST, "false");
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1, url2, url3));
    crawlUrls(ListUtil.list(url1, url2, url3));
    aus.assertUpdatedCrawlListCalled(0); //not called for startUrl
  }

  public void testMyLinkExtractorCallback() {
    final String prefix = "http://www.example.com/"; // pseudo crawl rule
    MockArchivalUnit mau = new MockArchivalUnit() {
	// shouldBeCached() is true of anything starting with prefix
	public boolean shouldBeCached(String url) {
	  return StringUtil.startsWithIgnoreCase(url, prefix);
	}
	// siteNormalizeUrl() removes "SESSION/" from url
	public String siteNormalizeUrl(String url) {
	  return StringUtil.replaceString(url, "SESSION/", "");
	}
      };
    Set extractedUrls = new HashSet();
    TestableFollowLinkCrawler.MyLinkExtractorCallback mfuc =
      crawler.newFoundUrlCallback(Collections.EMPTY_SET,
					       extractedUrls, mau);
    mfuc.foundLink("http://www.example.com/foo.bar");
    mfuc.foundLink("http://www.example.com/SESSION/foo.bar");
    mfuc.foundLink("HTTP://www.example.com/SESSION/foo.bar");
    assertEquals(SetUtil.set("http://www.example.com/foo.bar"), extractedUrls);
    extractedUrls.clear();
    // illegal url gets added depending on path traversal action
    mfuc.foundLink("http://www.example.com/foo/../..");
    switch (CurrentConfig.getIntParam(UrlUtil.PARAM_PATH_TRAVERSAL_ACTION,
				      UrlUtil.DEFAULT_PATH_TRAVERSAL_ACTION)) {
    case UrlUtil.PATH_TRAVERSAL_ACTION_ALLOW:
      assertEquals(SetUtil.set("http://www.example.com/../"), extractedUrls);
      break;
    case UrlUtil.PATH_TRAVERSAL_ACTION_REMOVE:
      assertEquals(SetUtil.set("http://www.example.com/"), extractedUrls);
      break;
    case UrlUtil.PATH_TRAVERSAL_ACTION_THROW:
      assertEmpty(extractedUrls);
      break;
    }
  }

  public void testWatchdogPokedForEachFetchAfterGetUrlsToFollow() {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1, url2, url3));

    MockLockssWatchdog wdog = new MockLockssWatchdog();
    crawler.setWatchdog(wdog);
    crawlUrls(ListUtil.list(url1, url2, url3));
    wdog.assertPoked(3); //not counted for startUrl
  }

  //test that we don't cache a file that our crawl rules reject
  public void testDoesNotCacheFileWhichShouldNotBeCached() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, false, false);

    crawler.doCrawl();
    assertEquals(SetUtil.set(), cus.getCachedUrls());
  }

  private MockCachedUrlSet permissionPageTestSetup(List permissionPages,
                                                   int passPermissionCheck,
                                                   List urlsToCrawl,
                                                   MockArchivalUnit mmau) {
   //set plugin
    mmau.setPlugin(new MockPlugin(getMockLockssDaemon()));
    mmau.setAuId("permissionPage au");
    MockCachedUrlSet mcus = (MockCachedUrlSet)mmau.getAuCachedUrlSet();
    crawlRule = new MockCrawlRule();

    for (int ix=0; ix<urlsToCrawl.size(); ix++) {
      String curUrl = (String)urlsToCrawl.get(ix);
      mmau.addUrl(curUrl);
      crawlRule.addUrlToCrawl(curUrl);
    }

    //set Crawl spec
    spec = new SpiderCrawlSpec(urlsToCrawl, permissionPages, crawlRule, 1);
    mmau.setCrawlSpec(spec);
    //set Crawler
    crawler = new TestableFollowLinkCrawler(mmau, spec, new MockAuState());
    ((BaseCrawler)crawler).daemonPermissionCheckers = ListUtil.list(
        new MyMockPermissionChecker(passPermissionCheck));

    //set extractor
    mmau.setLinkExtractor("text/html", extractor);

    return mcus;
  }

  public void testMultiPermissionPageShouldPass(){
    String permissionUrl1 = "http://www.example.com/index.html";
    String permissionUrl2 = "http://www.foo.com/index.html";
    List permissionList = ListUtil.list(permissionUrl1,permissionUrl2);

    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.foo.com/link3.html";
    String url4= "http://www.foo.com/link4.html";
    List urls =
      ListUtil.list(url1,url2,url3,url4,permissionUrl1,permissionUrl2);

    MyMockArchivalUnit mau = new MyMockArchivalUnit();
    mau.setCrawlSpec(spec);

    MockCachedUrlSet cus = permissionPageTestSetup(permissionList,2,urls, mau);

    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1, url2, url3, url4));
    assertTrue(crawler.doCrawl());
    Set expected = SetUtil.fromList(urls);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testMultiPermissionPageShouldFailAsPermissionNotOk(){
    setProperty(BaseCrawler.PARAM_ABORT_ON_FIRST_NO_PERMISSION, "false");

    String permissionUrl1 = "http://www.example.com/index.html";
    String permissionUrl2 = "http://www.foo.com/index.html";
    List permissionList = ListUtil.list(permissionUrl1,permissionUrl2);

    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.foo.com/link3.html";
    String url4= "http://www.foo.com/link4.html";
    List urls =
      ListUtil.list(url1,url2,url3,url4,permissionUrl1,permissionUrl2);

    MyMockArchivalUnit mau = new MyMockArchivalUnit();
    mau.setCrawlSpec(spec);

    MockCachedUrlSet cus = permissionPageTestSetup(permissionList,1,urls, mau);

    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1, url2, url3, url4));
    assertFalse(crawler.doCrawl());
    Set expected = SetUtil.set(permissionUrl1, url1, url2);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testMultiPermissionPageShouldFailWithAbortWhilePermissionOtherThanOkParam(){
    setProperty(BaseCrawler.PARAM_ABORT_ON_FIRST_NO_PERMISSION, "false");

    String permissionUrl1 = "http://www.example.com/index.html";
    String permissionUrl2 = "http://www.foo.com/index.html";
    List permissionList = ListUtil.list(permissionUrl1,permissionUrl2);

    String url1= "http://www.example.com/link1.html";
    String url3= "http://www.foo.com/link3.html";
    List urls = ListUtil.list(url1, url3, permissionUrl1, permissionUrl2);

    MyMockArchivalUnit mau = new MyMockArchivalUnit();
    mau.setCrawlSpec(spec);
    MockCachedUrlSet cus = permissionPageTestSetup(permissionList, 1, urls, mau);

    setProperty(TestableFollowLinkCrawler.PARAM_ABORT_ON_FIRST_NO_PERMISSION,
		"true");

    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1,
                                                                     url3));
    assertFalse(crawler.doCrawl());
    Set expected = SetUtil.set(permissionUrl1);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testPermissionPageShouldFailAsFetchPermissionFailTwice(){
    ConfigurationUtil.addFromArgs(BaseCrawler.PARAM_ABORT_ON_FIRST_NO_PERMISSION,
				  "false",
				  BaseCrawler.PARAM_REFETCH_PERMISSIONS_PAGE,
				  "true");

    String permissionUrl1 = "http://www.example.com/index.html";
    String permissionUrl2 = "http://www.foo.com/index.html";
    List permissionList = ListUtil.list(permissionUrl1, permissionUrl2);

    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.foo.com/link2.html";
    List urls = ListUtil.list(url1, url2);

    mau.addUrl(permissionUrl1, new ExpectedIOException(), 2);
    mau.addUrl(permissionUrl2, false, true);

    MockCachedUrlSet cus = permissionPageTestSetup(permissionList,100,
						   urls, mau);

    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(urls);

    assertFalse(crawler.doCrawl());
    assertEquals(SetUtil.set(permissionUrl2, url2), cus.getCachedUrls());
  }

  public void testPermissionPageFailOnceAndOkAfterRefetch(){
    ConfigurationUtil.addFromArgs(BaseCrawler.PARAM_ABORT_ON_FIRST_NO_PERMISSION,
				  "false",
				  BaseCrawler.PARAM_REFETCH_PERMISSIONS_PAGE,
				  "true");

    String permissionUrl1 = "http://www.example.com/index.html";
    List permissionList = ListUtil.list(permissionUrl1);

    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    List urls = ListUtil.list(url1,url2);

    mau.addUrl(permissionUrl1, new ExpectedIOException(), 1);

    MockCachedUrlSet cus = permissionPageTestSetup(permissionList,100,
						   urls, mau);

    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1, url2));

    assertTrue(crawler.doCrawl());
    Set expected = SetUtil.set(url1,url2,permissionUrl1);
    assertEquals( expected, mcus.getCachedUrls());
  }

  public void testPermissionPageMissing(){
    String permissionUrl1 = "http://www.example.com/index.html";
    List permissionList = ListUtil.list(permissionUrl1);

    String url1 = "http://www.example.com/link1.html";
    String url3 = "http://www.foo.com/link3.html";
    List urls = ListUtil.list(url1,url3,permissionUrl1);

    MyMockArchivalUnit mau = new MyMockArchivalUnit();
    mau.setCrawlSpec(spec);
    MockCachedUrlSet cus =
        permissionPageTestSetup(permissionList, 1, urls, mau);

    ((TestableFollowLinkCrawler)crawler).setUrlsToFollow(ListUtil.list(url1, url3));
    assertFalse(crawler.doCrawl());
    Set expected = SetUtil.set(permissionUrl1, url1);
    assertEquals(expected, cus.getCachedUrls());
  }

  private static void setProperty(String prop, String value) {
    ConfigurationUtil.addFromArgs(prop, value);
  }

  boolean doCrawl0(BaseCrawler crawler) {
    crawler.setCrawlConfig(ConfigManager.getCurrentConfig());
    return crawler.doCrawl0();
  }

  private class MyMockArchivalUnit extends MockArchivalUnit {
    MyMockUrlCacher lastMmuc;

    protected MockUrlCacher makeMockUrlCacher(String url) {
      lastMmuc = new MyMockUrlCacher(url, this);
      return lastMmuc;
    }
  }


  private class MyMockCrawlWindow implements CrawlWindow {
    int numTimesToReturnTrue = 0;

    /**
     * Construct a MockCrawlWindow that can set number of time
     * to return true when canCrawl is called.
     *
     * @param numTimesToReturnTrue the number of time to return true
     * excluding fetching of permission pages and starting urls.
     *
     */
    public MyMockCrawlWindow(int numTimesToReturnTrue) {
      this.numTimesToReturnTrue = numTimesToReturnTrue;
    }

    public boolean canCrawl() {
      if (numTimesToReturnTrue > 0) {
	numTimesToReturnTrue--;
	return true;
      }
      return false;
    }

    public boolean canCrawl(Date date) {
      throw new UnsupportedOperationException("not implemented");
    }

  }


  private class MyMockUrlCacher extends MockUrlCacher {
    private boolean abortCrawl = false;
    String proxyHost = null;
    int proxyPort;

    public MyMockUrlCacher(String url, MockArchivalUnit au) {
      super(url, au);
    }

    public InputStream getUncachedInputStream() throws IOException {
      checkAbort();
      return super.getUncachedInputStream();
    }
    public int cache() throws IOException {
      checkAbort();
      //System.out.println("Caching for : " + super.getUrl());
      return super.cache();
    }
    private void checkAbort() {
      if (abortCrawl) {
	crawler.abortCrawl();
      }
    }
    public void setProxy(String proxyHost, int proxyPort) {
      this.proxyHost = proxyHost;
      this.proxyPort = proxyPort;
    }
  }

  private class MyMockRetryableCacheException
    extends CacheException.RetryableException {

    private int retryCount = -1;
    private long retryDelay = -1;

    public MyMockRetryableCacheException(String msg) {
      super(msg);
    }
    public MyMockRetryableCacheException(String msg,
					 int retryCount, long retryDelay) {
      this(msg);
      this.retryCount = retryCount;
      this.retryDelay = retryDelay;
    }
    public int getRetryCount() {
      return retryCount;
    }
    public long getRetryDelay() {
      return retryDelay;
    }
  }

  private class MyMockUnretryableCacheException
    extends CacheException.UnretryableException {
    public MyMockUnretryableCacheException(String msg) {
      super(msg);
    }
  }

  private class MyMockCacheException
    extends CacheException {
    public MyMockCacheException(String msg) {
      super(msg);
    }
    public void setFailing() {
      attributeBits.set(CacheException.ATTRIBUTE_FAIL);
    }
  }

  private class MyMockPermissionChecker implements PermissionChecker {
    int numPermissionGranted=0;

    MyMockPermissionChecker(int numPermissionGranted) {
      this.numPermissionGranted = numPermissionGranted;
    }

    public void setNumPermissionGranted(int num){
      numPermissionGranted = num;
    }

   /**
     * checkPermission
     *
     * @param reader Reader
     * @return boolean
     */
    public boolean checkPermission(Crawler.PermissionHelper pHelper,
				   Reader reader, String permissionUrl) {
        if (numPermissionGranted-- > 0) {
          return true;
        } else {
          return false;
        }

    }
  }

  private static class TestableFollowLinkCrawler extends FollowLinkCrawler {

    Set urlsToFollow = new HashSet();

    protected TestableFollowLinkCrawler(ArchivalUnit au,
					CrawlSpec spec, AuState aus){
      super(au, spec, aus);
      crawlStatus = new CrawlerStatus(au,
		    ((SpiderCrawlSpec)spec).getStartingUrls(),
		    null);
    }

    MyLinkExtractorCallback newFoundUrlCallback(Set parsedPages,
						Collection extractedUrls,
						ArchivalUnit au) {
      return new MyLinkExtractorCallback(parsedPages, extractedUrls, au);
    }

    protected boolean shouldFollowLink(){
      //always return true here
      return true;
    }

    /** suppress these actions */
    protected void doCrawlEndActions() {
    }

    /**
     * setUrlsToFollow set the urls set that will return when calling
     * getUrlsToFollow
     */
    protected void setUrlsToFollow(List urls) {
      urlsToFollow = new ListOrderedSet();
      urlsToFollow.addAll(urls);
    }

    protected Set getUrlsToFollow(){
      // assumed all the canCrawl return true in calling getUrlsToFollow;
      // thus when setting MyCrawlWindow, we can ignore the number of canCrawl()
      // called inside the getUrlsToFollow method.
      return urlsToFollow;
    }

    public int getType() {
      throw new UnsupportedOperationException("not implemented");
    }

    public String getTypeString() {
      return "Follow Link";
    }

    public boolean isWholeAU() {
      return false;
    }

  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestFollowLinkCrawler.class.getName()};
    junit.textui.TestRunner.main(testCaseList);
  }

}

