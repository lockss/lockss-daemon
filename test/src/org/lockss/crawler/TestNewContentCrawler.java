/*
 * $Id: TestNewContentCrawler.java,v 1.21 2004-09-22 02:45:00 tlipkis Exp $
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
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.test.*;
import org.lockss.state.*;
import org.lockss.util.urlconn.*;

public class TestNewContentCrawler extends LockssTestCase {

  private MockArchivalUnit mau = null;
  private CrawlSpec spec = null;
  private MockAuState aus = new MockAuState();
  private static List testUrlList = ListUtil.list("http://example.com");
  private MockCrawlRule crawlRule = null;
  private String startUrl = "http://www.example.com/index.html";
  private List startUrls = ListUtil.list(startUrl);
  private CrawlerImpl crawler = null;
  private MockContentParser parser = new MockContentParser();

  private static final String PARAM_RETRY_TIMES =
    Configuration.PREFIX + "CrawlerImpl.numCacheRetries";
  private static final int DEFAULT_RETRY_TIMES = 3;

  public void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated(10);

    getMockLockssDaemon().getAlertManager();

    mau = new MockArchivalUnit();
    mau.setPlugin(new MockPlugin());

    startUrls = ListUtil.list(startUrl);
    MockCachedUrlSet cus = new MyMockCachedUrlSet(mau, null);
    mau.setAuCachedUrlSet(cus);
    crawlRule = new MockCrawlRule();
    crawlRule.addUrlToCrawl(startUrl);
    spec = new CrawlSpec(startUrls, startUrls, crawlRule, 1);
    crawler = new NewContentCrawler(mau, spec, aus);
    ((CrawlerImpl)crawler).lockssCheckers = ListUtil.list(new MyMockPermissionChecker(1));

    mau.setParser(parser);
    Properties p = new Properties();
    p.setProperty(NewContentCrawler.PARAM_RETRY_PAUSE, "0");
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }

  public void testMnccThrowsForNullAu() {
    try {
      crawler = new NewContentCrawler(null, spec, new MockAuState());
      fail("Constructing a NewContentCrawler with a null ArchivalUnit"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testMnccThrowsForNullCrawlSpec() {
    try {
      crawler = new NewContentCrawler(mau, null, new MockAuState());
      fail("Calling makeNewContentCrawler with a null CrawlSpec"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testMnccThrowsForNullAuState() {
    try {
      crawler = new NewContentCrawler(mau, spec, null);
      fail("Calling makeNewContentCrawler with a null AuState"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testReturnsProperType() {
    assertEquals(Crawler.NEW_CONTENT, crawler.getType());
  }

  public void testMakeUrlCacher() {
    MyMockCachedUrlSet cus = (MyMockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl);
    crawler.makeUrlCacher(cus, startUrl);
    MyMockUrlCacher mmuc = cus.lastMmuc;
    assertNull(mmuc.proxyHost);
  }

  public void testMakeUrlCacherProxy() {
    ConfigurationUtil.setFromArgs(NewContentCrawler.PARAM_PROXY_HOST, "pr.wub",
				  NewContentCrawler.PARAM_PROXY_PORT, "27");
    crawler.setCrawlConfig(ConfigManager.getCurrentConfig());
    MyMockCachedUrlSet cus = (MyMockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl);
    crawler.makeUrlCacher(cus, startUrl);
    MyMockUrlCacher mmuc = cus.lastMmuc;
    assertEquals("pr.wub", mmuc.proxyHost);
    assertEquals(27, mmuc.proxyPort);
  }

  //Will try to fetch startUrl, content parser will return no urls,
  //so we should only cache the start url
  public void testDoCrawlOnePageNoLinks() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl);
    assertTrue(crawler.doCrawl());
    Set cachedUrls = cus.getCachedUrls();
    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cachedUrls);
  }

  //Fetch startUrl, parser will return a single url that already exists
  //we should only cache startUrl
  public void testDoesNotCacheExistingFile() {
    String url1="http://www.example.com/blah.html";

    parser.setUrlToReturn(url1);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl, true, true);
    cus.addUrl(url1, true, true);

    assertTrue(crawler.doCrawl());

    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testMultipleStartingUrls() {
    List urls = ListUtil.list("http://www.example.com/link1.html",
			      "http://www.example.com/link2.html",
			      "http://www.example.com/link3.html",
			      startUrl);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    for (int ix=0; ix<urls.size(); ix++) {
      String curUrl = (String)urls.get(ix);
      cus.addUrl(curUrl);
      crawlRule.addUrlToCrawl(curUrl);
    }


    spec = new CrawlSpec(urls, ListUtil.list(startUrl), crawlRule, 1);
    crawler = new NewContentCrawler(mau, spec, new MockAuState());
    ((CrawlerImpl)crawler).lockssCheckers = ListUtil.list(new MyMockPermissionChecker(1));

    mau.setParser(parser);

    assertTrue(crawler.doCrawl());
    Set expected = SetUtil.fromList(urls);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testReturnsFalseWhenFailingUnretryableExceptionThrown() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    cus.addUrl(startUrl, false, true);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
    MyMockUnretryableCacheException exception =
       new MyMockUnretryableCacheException("Test exception");
    cus.addUrl(url1, exception, DEFAULT_RETRY_TIMES);
    crawlRule.addUrlToCrawl(url1);
    
    assertFalse(crawler.doCrawl0());
  }

  public void testReturnsFalseWhenIOExceptionThrown() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    cus.addUrl(startUrl, false, true);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
    cus.addUrl(url1, new IOException("Test exception"), DEFAULT_RETRY_TIMES);
    crawlRule.addUrlToCrawl(url1);

    assertFalse(crawler.doCrawl0());
  }

  public void testReturnsTrueWhenCrawlSuccessful() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    cus.addUrl(startUrl, false, true);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
    cus.addUrl(url1, false, true);

    assertTrue(crawler.doCrawl0());
   }

  public void testReturnsTrueWhenNonFailingUnretryableExceptionThrown() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    cus.addUrl(startUrl, false, true);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
    MyMockCacheException exception =
      new MyMockCacheException("Test exception");
    cus.addUrl(url1, exception, DEFAULT_RETRY_TIMES);
    crawlRule.addUrlToCrawl(url1);

    assertTrue(crawler.doCrawl0());
  }

  public void testReturnsRetriesWhenRetryableExceptionThrown() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    cus.addUrl(startUrl, false, true);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
    MyMockRetryableCacheException exception =
      new MyMockRetryableCacheException("Test exception");
    cus.addUrl(url1, exception, DEFAULT_RETRY_TIMES-1);
    crawlRule.addUrlToCrawl(url1);

    crawler.doCrawl0();
    Set expected = SetUtil.set(startUrl, url1);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testReturnsDoesntRetryWhenUnretryableExceptionThrown() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    cus.addUrl(startUrl, false, true);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
    MyMockUnretryableCacheException exception =
      new MyMockUnretryableCacheException("Test exception");
    cus.addUrl(url1, exception, DEFAULT_RETRY_TIMES-1);
    crawlRule.addUrlToCrawl(url1);

    crawler.doCrawl0();
    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testRetryNumSetByParam() {
    int retryNum = DEFAULT_RETRY_TIMES + 3;
    assertTrue("Test is worthless unless retryNum is greater than "
	       +"DEFAULT_RETRY_TIMES", retryNum > DEFAULT_RETRY_TIMES);
    Properties p = new Properties();
    p.setProperty(PARAM_RETRY_TIMES, String.valueOf(retryNum));
    p.setProperty(NewContentCrawler.PARAM_RETRY_PAUSE, "0");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    cus.addUrl(startUrl, false, true);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
    cus.addUrl(url1,
	       new MyMockRetryableCacheException("Test exception"),
	       retryNum-1);
    crawlRule.addUrlToCrawl(url1);

    crawler.doCrawl();
    Set expected = SetUtil.set(startUrl, url1);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testCachesFailedFetches() {
    int retryNum = 3;
    Properties p = new Properties();
    p.setProperty(PARAM_RETRY_TIMES, String.valueOf(retryNum));
    p.setProperty(NewContentCrawler.PARAM_RETRY_PAUSE, "0");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    MyMockCachedUrlSet cus = (MyMockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    String url2="http://www.example.com/blah2.html";
    String url3="http://www.example.com/blah3.html";
    cus.addUrl(startUrl, false, true);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1, url2, url3));
    cus.addUrl(url2,
	       new CacheException.UnretryableException("Test exception"),
	       retryNum);
    parser.addUrlSetToReturn(url2, SetUtil.set(url1));
    cus.addUrl(url3,
	       new CacheException.RetryableException("Test exception"),
	       retryNum);
    parser.addUrlSetToReturn(url3, SetUtil.set(url1));
    cus.addUrl(url1, new IOException("Test exception"), retryNum);

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
    cus.addUrl(startUrl, false, true);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
    cus.addUrl(url1, new FileNotFoundException("Test exception"), 0);
    crawlRule.addUrlToCrawl(url1);

    assertTrue(crawler.doCrawl());
  }

  public void testPluginThrowsRuntimeException() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    cus.addUrl(startUrl, false, true);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
    cus.addUrl(url1, new ExpectedRuntimeException("Test exception"), 0);
    crawlRule.addUrlToCrawl(url1);

    assertFalse(crawler.doCrawl());
  }

  public void testGetStatusStartUrls() {
    Crawler.Status crawlStatus = crawler.getStatus();
    assertEquals(startUrls, crawlStatus.getStartUrls());
  }

  public void testGetStatusCrawlDone() {
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html";
    String url3 = "http://www.example.com/link3.html";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1, url2, url3));
    cus.addUrl(url1);
    cus.addUrl(url2);
    cus.addUrl(url3);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);

    long expectedStart = TimeBase.nowMs();
    crawler.doCrawl();
    long expectedEnd = TimeBase.nowMs();
    Crawler.Status crawlStatus = crawler.getStatus();
    assertEquals(expectedStart, crawlStatus.getStartTime());
    assertEquals(expectedEnd, crawlStatus.getEndTime());
    assertEquals(5, crawlStatus.getNumFetched());
    assertEquals(4, crawlStatus.getNumParsed());
  }

  public void testGetStatusIncomplete() {
    assertEquals(Crawler.STATUS_INCOMPLETE,
		 crawler.getStatus().getCrawlStatus());
  }

  public void testGetStatusSuccessful() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl);
    crawler.doCrawl();
    assertEquals(Crawler.STATUS_SUCCESSFUL,
		 crawler.getStatus().getCrawlStatus());
  }

  public void testGetStatusError() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    cus.addUrl(startUrl, false, true);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
    cus.addUrl(url1, new IOException("Test exception"), DEFAULT_RETRY_TIMES);
    crawlRule.addUrlToCrawl(url1);

    crawler.doCrawl();
    assertEquals(Crawler.STATUS_ERROR,
		 crawler.getStatus().getCrawlStatus());
  }

  public void testCrawlWindow() {
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html";
    String url3 = "http://www.example.com/link3.html";

    CrawlSpec spec = new CrawlSpec(startUrl, crawlRule);
    spec.setCrawlWindow(new MockCrawlWindowThatCountsDown(3));
    mau.setCrawlSpec(spec);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1, url2, url3));
    cus.addUrl(url1);
    cus.addUrl(url2);
    cus.addUrl(url3);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);


    crawler = new NewContentCrawler(mau, spec, aus);
//     crawler = new NewContentCrawler(mau, spec, new MockAuState());
    ((CrawlerImpl)crawler).lockssCheckers = ListUtil.list(new MyMockPermissionChecker(100));

    mau.setParser(parser);
    crawler.doCrawl();
    // only gets 2 urls because start url is fetched twice (manifest & parse)
    Set expected = SetUtil.set(startUrl, url1);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testOverwritesStartingUrlsOneLevel() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl, true, true);

    assertTrue(crawler.doCrawl());
    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cus.getCachedUrls());
  }


  public void testOverwritesStartingUrlsMultipleLevels() {
    spec = new CrawlSpec(startUrls, startUrls, crawlRule, 2);
    crawler = new NewContentCrawler(mau, spec, new MockAuState());
    ((CrawlerImpl)crawler).lockssCheckers = ListUtil.list(new MyMockPermissionChecker(1));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/dir/link3.html";
    String url4= "http://www.example.com/dir/link9.html";

    mau.setParser(parser);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1, url2, url3));
    parser.addUrlSetToReturn(url1, SetUtil.set(url4));

    cus.addUrl(startUrl, true, true);
    cus.addUrl(url1, true, true);
    cus.addUrl(url2, true, true);
    cus.addUrl(url3, true, true);
    cus.addUrl(url4, true, true);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);
    crawlRule.addUrlToCrawl(url4);

    assertTrue(crawler.doCrawl());
    Set expected = SetUtil.set(startUrl, url1, url2, url3);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testWillNotParseExistingPagesForUrlsIfParam() {
    Properties p = new Properties();
    p.setProperty(NewContentCrawler.PARAM_REPARSE_ALL, "false");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    String url1 = "http://www.example.com/link3.html";
    String url2 = "http://www.example.com/link4.html";
    startUrls = ListUtil.list(startUrl);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
    parser.addUrlSetToReturn(url1, SetUtil.set(url2));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl, true, true);
    cus.addUrl(url1, true, true);
    cus.addUrl(url2);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);

    assertTrue(crawler.doCrawl());
    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testWillParseExistingPagesForUrlsIfParam() {
    setProperty(NewContentCrawler.PARAM_REPARSE_ALL, "true");

    String url1 = "http://www.example.com/link3.html";
    String url2 = "http://www.example.com/link4.html";
    startUrls = ListUtil.list(startUrl);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
    parser.addUrlSetToReturn(url1, SetUtil.set(url2));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl, true, true);
    cus.addUrl(url1, true, true);
    cus.addUrl(url2);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);

    assertTrue(crawler.doCrawl());
    Set expected = SetUtil.set(startUrl, url2);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testOutsideOfWindow1() {
    String url1= "http://www.example.com/link1.html";
    spec.setCrawlWindow(new MyMockCrawlWindow(0));
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl, true, true);
    assertFalse(crawler.doCrawl());
  }

  public void testOutsideOfWindow2() {
    String url1= "http://www.example.com/link1.html";
    spec.setCrawlWindow(new MyMockCrawlWindow(1));
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl, true, true);
    cus.addUrl(url1);
    assertFalse(crawler.doCrawl());
  }

  public void testAborted1() {
    String url1= "http://www.example.com/link1.html";
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl, true, true);
    crawler.abortCrawl();
    assertFalse(crawler.doCrawl());
    assertEmpty(cus.getCachedUrls());
  }

  public void testAborted2() {
    String url1= "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link4.html";
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1, url2));
    MyMockCachedUrlSet cus = (MyMockCachedUrlSet)mau.getAuCachedUrlSet();

    cus.addUrl(startUrl, true, true);
    cus.addUrl(url1);
    cus.addUrl(url2);
    // make it abort after either url1 or url2 cached.  (Can't predict
    // order, so do both.)
    MyMockUrlCacher mmuc = (MyMockUrlCacher)cus.makeUrlCacher(url1);
    mmuc.abortCrawl = true;
    mmuc = (MyMockUrlCacher)cus.makeUrlCacher(url2);
    mmuc.abortCrawl = true;
    assertFalse(crawler.doCrawl());
    // should have cached startUrl and one of the others
    Set cached = cus.getCachedUrls();
    assertEquals(2, cached.size());
    assertTrue(cached.contains(startUrl));
  }

  private Set crawlUrls(Set urls) {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl);
    //cus.addUrl(startUrl);
    Iterator it = urls.iterator();
    while (it.hasNext()) {
      String url = (String)it.next();
      cus.addUrl(url);
      crawlRule.addUrlToCrawl(url);
    }
    crawler.doCrawl();
    return cus.getCachedUrls();
  }

  public void testDoesCollectHttps() {
    String url1= "http://www.example.com/link1.html";
    String url2= "https://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";


    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
    parser.addUrlSetToReturn(url1, SetUtil.set(url1, url2, url3));
    assertEquals(SetUtil.set(startUrl, url1, url3),
		 crawlUrls(SetUtil.set(url1, url2, url3)));
  }

  public void testDoesCollectFtpAndGopher() {
    String url1= "http://www.example.com/link1.html";
    String url2= "ftp://www.example.com/link2.html";
    String url3= "gopher://www.example.com/link3.html";


    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
    parser.addUrlSetToReturn(url1, SetUtil.set(url1, url2, url3));
    assertEquals(SetUtil.set(startUrl, url1),
		 crawlUrls(SetUtil.set(url1, url2, url3)));
  }

  public void testDoesNotLoopOnSelfReferentialPage() {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";


    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
    parser.addUrlSetToReturn(url1, SetUtil.set(url1, url2, url3));
    assertEquals(SetUtil.set(startUrl, url1, url2, url3),
		 crawlUrls(SetUtil.set(url1, url2, url3)));
  }

  public void testDoesNotLoopOnSelfReferentialLoop() {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    parser.addUrlSetToReturn(startUrl,
			     SetUtil.set(url1, url2, url3, startUrl));
    parser.addUrlSetToReturn(url1, SetUtil.set(startUrl));
    assertEquals(SetUtil.set(startUrl, url1, url2, url3),
		 crawlUrls(SetUtil.set(url1, url2, url3)));
  }

  public void testCrawlListEmptyOnExit() {
    setProperty(NewContentCrawler.PARAM_PERSIST_CRAWL_LIST, "true");
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1, url2, url3));
    crawlUrls(SetUtil.set(url1, url2, url3));
    assertEmpty(aus.getCrawlUrls());
  }

  public void testCrawlListPreservesUncrawledUrls() {
    setProperty(NewContentCrawler.PARAM_PERSIST_CRAWL_LIST, "true");
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    spec.setCrawlWindow(new MyMockCrawlWindow(1));
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1, url2, url3));
    crawlUrls(SetUtil.set(url1, url2, url3));
    assertEquals(SetUtil.set(url2, url3), aus.getCrawlUrls());
  }

  public void testCrawlListDoesntPreserveUncrawledUrlsIfParam() {
    setProperty(NewContentCrawler.PARAM_PERSIST_CRAWL_LIST, "false");
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    spec.setCrawlWindow(new MyMockCrawlWindow(2));
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1, url2, url3));
    crawlUrls(SetUtil.set(url1, url2, url3));
    assertEquals(SetUtil.set(), aus.getCrawlUrls());
  }

  public void testUpdatedCrawlListCalledForEachFetchIfParam() {
    setProperty(NewContentCrawler.PARAM_PERSIST_CRAWL_LIST, "true");
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1, url2, url3));
    crawlUrls(SetUtil.set(url1, url2, url3));
    aus.assertUpdatedCrawlListCalled(3); //not called for startUrl
  }

  public void testUpdatedCrawlListCalledForEachFetch() {
    setProperty(NewContentCrawler.PARAM_PERSIST_CRAWL_LIST, "false");
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1, url2, url3));
    crawlUrls(SetUtil.set(url1, url2, url3));
    aus.assertUpdatedCrawlListCalled(0); //not called for startUrl
  }

  public void testMyFoundUrlCallback() {
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
    NewContentCrawler.MyFoundUrlCallback mfuc =
      new NewContentCrawler.MyFoundUrlCallback(Collections.EMPTY_SET,
					       extractedUrls, mau);
    mfuc.foundUrl("http://www.example.com/foo.bar");
    mfuc.foundUrl("http://www.example.com/SESSION/foo.bar");
    mfuc.foundUrl("HTTP://www.example.com/SESSION/foo.bar");
    assertEquals(SetUtil.set("http://www.example.com/foo.bar"), extractedUrls);
    extractedUrls.clear();
    // illegal url should not be added
    mfuc.foundUrl("http://www.example.com/foo/../..");
    assertEmpty(extractedUrls);
  }

  public void testWatchdogPokedForEachFetch() {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1, url2, url3));

    MockLockssWatchdog wdog = new MockLockssWatchdog();
    crawler.setWatchdog(wdog);
    crawlUrls(SetUtil.set(url1, url2, url3));
    wdog.assertPoked(4);
  }

  //test that we don't cache a file that our crawl rules reject
  public void testDoesNotCacheFileWhichShouldNotBeCached() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl, false, false);

    crawler.doCrawl();
    assertEquals(0, cus.getCachedUrls().size());
  }

  public void testMultiPermissionPageShouldPass(){
    String permissionUrl1 = "http://www.example.com/index.html";
    String permissionUrl2 = "http://www.foo.com/index.html";
    List permissionList = ListUtil.list(permissionUrl1,permissionUrl2);

    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.foo.com/link3.html";
    String url4= "http://www.foo.com/link4.html";
    List urls = ListUtil.list(url1,url2,url3,url4,permissionUrl1,permissionUrl2);

    MockCachedUrlSet cus = permissionPageTestSetup(permissionList,2,urls, new MockPlugin());

    assertTrue(crawler.doCrawl());
    Set expected = SetUtil.fromList(urls);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testMultiPermissionPageShouldFailAsPermissionNotOk(){
    String permissionUrl1 = "http://www.example.com/index.html";
    String permissionUrl2 = "http://www.foo.com/index.html";
    List permissionList = ListUtil.list(permissionUrl1,permissionUrl2);

    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.foo.com/link3.html";
    String url4= "http://www.foo.com/link4.html";
    List urls = ListUtil.list(url1,url2,url3,url4,permissionUrl1,permissionUrl2);

    MockCachedUrlSet cus = permissionPageTestSetup(permissionList,1,urls, new MockPlugin());

    assertFalse(crawler.doCrawl());
    Set expected = SetUtil.set(permissionUrl1, url1, url2);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testMultiPermissionPageShouldFailWithAbortWhilePermissionOtherThanOkParam(){
    String permissionUrl1 = "http://www.example.com/index.html";
    String permissionUrl2 = "http://www.foo.com/index.html";
    List permissionList = ListUtil.list(permissionUrl1,permissionUrl2);

    String url1= "http://www.example.com/link1.html";
    String url3= "http://www.foo.com/link3.html";
    List urls = ListUtil.list(url1,url3,permissionUrl1,permissionUrl2);

    MockCachedUrlSet cus = permissionPageTestSetup(permissionList,1,urls, new MockPlugin());

    setProperty(NewContentCrawler.PARAM_ABORT_WHILE_PERMISSION_OTHER_THAN_OK,""+true);

    assertFalse(crawler.doCrawl());
    Set expected = SetUtil.set(permissionUrl1);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testPermissionPageShouldFailAsFetchPermissionFailTwice(){
    String permissionUrl1 = "http://www.example.com/index.html";
    List permissionList = ListUtil.list(permissionUrl1);

    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    List urls = ListUtil.list(url1,url2,permissionUrl1);

    HashMap hMap = new HashMap();
    // makeUrlCacher in MyMockPlugin will get call 2 time, before a
    // uc.cache() is called
    hMap.put((String) permissionUrl1.toLowerCase(),new Integer(4));

    MockCachedUrlSet cus = permissionPageTestSetup(permissionList,2,urls, new MyMockPlugin(hMap));

    setProperty(CrawlerImpl.PARAM_REFETCH_PERMISSIONS_PAGE,""+true);

    assertFalse(crawler.doCrawl());
    assertEquals(0,cus.getCachedUrls().size());
  }

  public void testPermissionPageFailOnceAndOkAfterRefetch(){
    String permissionUrl1 = "http://www.example.com/index.html";
    List permissionList = ListUtil.list(permissionUrl1);

    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    List urls = ListUtil.list(url1,url2,permissionUrl1);

    HashMap hMap = new HashMap();
    // makeUrlCacher in MyMockPlugin will get call 2 time, before a
    // uc.cache() is called
    hMap.put((String) permissionUrl1.toLowerCase(),new Integer(2));

    MockCachedUrlSet cus = permissionPageTestSetup(permissionList,2,urls, new MyMockPlugin(hMap));

    setProperty(CrawlerImpl.PARAM_REFETCH_PERMISSIONS_PAGE, ""+true);

    assertTrue(crawler.doCrawl());
    Set expected = SetUtil.fromList(urls);
    assertEquals( expected, cus.getCachedUrls());
  }

  public void testPermissionPageMissing(){
    String permissionUrl1 = "http://www.example.com/index.html";
    List permissionList = ListUtil.list(permissionUrl1);

    String url1= "http://www.example.com/link1.html";
    String url3= "http://www.foo.com/link3.html";
    List urls = ListUtil.list(url1,url3,permissionUrl1);

    MockCachedUrlSet cus =
        permissionPageTestSetup(permissionList, 1, urls, new MockPlugin());

    assertFalse(crawler.doCrawl());
    Set expected = SetUtil.set(permissionUrl1, url1);
    assertEquals(expected, cus.getCachedUrls());
  }

  private MockCachedUrlSet permissionPageTestSetup(List permissionPages,
      int passPermissionCheck, List urlsToCrawl, MockPlugin mp) {
    MockArchivalUnit mmau = new MockArchivalUnit();

    //mmau.setNumPermissionGranted(passPermissionCheck);

    //set plugin
    mmau.setPlugin(mp);

    //make cus , set Cus
    MockCachedUrlSet cus = new MyMockCachedUrlSet(mmau, null);
    mmau.setAuCachedUrlSet(cus);
    crawlRule = new MockCrawlRule();

    for (int ix=0; ix<urlsToCrawl.size(); ix++) {
      String curUrl = (String)urlsToCrawl.get(ix);
      cus.addUrl(curUrl);
      crawlRule.addUrlToCrawl(curUrl);
    }

    //set Crawl spec
    spec = new CrawlSpec(urlsToCrawl, permissionPages, crawlRule, 1);

    //set Crawler
    crawler = new NewContentCrawler(mmau, spec, new MockAuState());
    ((CrawlerImpl)crawler).lockssCheckers = ListUtil.list(
        new MyMockPermissionChecker(passPermissionCheck));

    //set parser
    mmau.setParser(parser);

    return cus;
  }

  private static void setProperty(String prop, String value) {
    Properties p = new Properties();
    p.setProperty(prop, value);
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }

  private class MyMockPlugin extends MockPlugin {

    int numExceptionThrow=0;
    HashMap hMap=null;

    public MyMockPlugin(HashMap hMap) {
      super();
      this.hMap = hMap;
      //System.out.println("The hMap content:" + this.hMap.toString());
    }

    public UrlCacher makeUrlCacher(CachedUrlSet owner, String url) {
      MyMockUrlCacher muc = new MyMockUrlCacher(url,(MockCachedUrlSet) owner);

      Integer int_numThrow = (Integer) hMap.get( (String) url.toLowerCase() );
      if (int_numThrow != null) {
	numExceptionThrow = int_numThrow.intValue();
	if (numExceptionThrow > 0) {
	  //System.out.println("hMap before setException = " +  hMap.toString());
	  muc.setCachingException(new IOException(),1);
	  numExceptionThrow--;

	  hMap.put(url.toLowerCase(), new Integer(numExceptionThrow) );

	  //testing
	  //System.out.println("hMap after setException = " +  hMap.toString());

	}
      }

      muc.setupCachedUrl("");
      return muc;
    }
  }

  private class MyMockCrawlWindow implements CrawlWindow {
    int numTimesToReturnTrue = 0;

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

  private class MyMockCachedUrlSet extends MockCachedUrlSet {
    MyMockUrlCacher lastMmuc;

    public MyMockCachedUrlSet(MockArchivalUnit owner, CachedUrlSetSpec spec) {
      super(owner, spec);
    }

    protected MockUrlCacher makeMockUrlCacher(String url,
 					      MockCachedUrlSet parent) {
      lastMmuc = new MyMockUrlCacher(url, parent);
      return lastMmuc;
    }

  }

  private class MyMockUrlCacher extends MockUrlCacher {
    private boolean abortCrawl = false;
    String proxyHost = null;
    int proxyPort;

    public MyMockUrlCacher(String url, MockCachedUrlSet cus) {
      super(url, cus);
    }
    public InputStream getUncachedInputStream() {
      checkAbort();
      return new StringInputStream("");
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
    public MyMockRetryableCacheException(String msg) {
      super(msg);
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

  private class MyMockPermissionChecker implements PermissionChecker{
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
    public boolean checkPermission(Reader reader) {
        if (numPermissionGranted-- > 0) {
          return true;
        } else {
          return false;
        }

    }
  }
  private class MockCrawlWindowThatCountsDown implements CrawlWindow {
    int counter;

    public MockCrawlWindowThatCountsDown(int counter) {
      this.counter = counter;
    }

    public int getCurrentCount() {
      return counter;
    }

    public boolean canCrawl() {
      if (counter > 0) {
        counter--;
        return true;
      }
      return false;
    }

    public boolean canCrawl(Date serverDate) {
      return canCrawl();
    }

    public boolean crawlIsPossible() {
      return true;
    }
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestNewContentCrawler.class.getName()};
    junit.textui.TestRunner.main(testCaseList);
  }

}

