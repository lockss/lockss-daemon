/*
 * $Id: TestNewContentCrawler.java,v 1.67 2008-03-26 04:51:06 tlipkis Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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
import java.net.*;
import java.io.*;

import org.apache.commons.collections.set.ListOrderedSet;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.test.*;
import org.lockss.state.*;
import org.lockss.util.urlconn.*;
import org.lockss.extractor.*;

import static org.lockss.crawler.BaseCrawler.PARAM_DEFAULT_RETRY_COUNT;
import static org.lockss.crawler.BaseCrawler.DEFAULT_DEFAULT_RETRY_COUNT;

/**
 * Tests for the new content crawler.
 */
public class TestNewContentCrawler extends LockssTestCase {

  protected MyMockArchivalUnit mau = null;
  protected MockCachedUrlSet mcus = null;
  protected CrawlSpec spec = null;
  protected MockAuState aus = new MockAuState();
  protected static List testUrlList = ListUtil.list("http://example.com");
  protected MockCrawlRule crawlRule = null;
  protected String startUrl = "http://www.example.com/index.html";
  protected String permissionPage = "http://www.example.com/permission.html";
  protected List startUrls;
  protected BaseCrawler crawler = null;
  protected MockLinkExtractor extractor = new MockLinkExtractor();


  public void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated(10);

    getMockLockssDaemon().getAlertManager();

    mau = newMyMockArchivalUnit();
    mau.setPlugin(new MockPlugin(getMockLockssDaemon()));
    mau.setAuId("MyMockTestAu");
    startUrls = ListUtil.list(startUrl);
    mcus = (MockCachedUrlSet)mau.getAuCachedUrlSet();

    crawlRule = new MockCrawlRule();
    crawlRule.addUrlToCrawl(startUrl);
    crawlRule.addUrlToCrawl(permissionPage);
    mau.addUrl(permissionPage);
    spec = new SpiderCrawlSpec(startUrls, ListUtil.list(permissionPage),
                               crawlRule, 1);
    crawler = new MyNewContentCrawler(mau, spec, aus);
    ((BaseCrawler)crawler).daemonPermissionCheckers =
      ListUtil.list(new MockPermissionChecker(1));
    mau.setCrawlSpec(spec);
    mau.setLinkExtractor("*", extractor);
    Properties p = new Properties();
    p.setProperty(NewContentCrawler.PARAM_DEFAULT_RETRY_DELAY, "0");
    p.setProperty(FollowLinkCrawler.PARAM_MIN_RETRY_DELAY, "0");
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }

  MyMockArchivalUnit newMyMockArchivalUnit() {
    NodeManager nodeManager = new MockNodeManager();
    MyMockArchivalUnit mau = new MyMockArchivalUnit();
    getMockLockssDaemon().setNodeManager(nodeManager, mau);
    return mau;
  }

  public void testMnccThrowsForNullAu() {
    try {
      crawler = new MyNewContentCrawler(null, spec, new MockAuState());
      fail("Constructing a NewContentCrawler with a null ArchivalUnit"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testMnccThrowsForNullCrawlSpec() {
    try {
      crawler = new MyNewContentCrawler(mau, null, new MockAuState());
      fail("Calling makeNewContentCrawler with a null CrawlSpec"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testMnccThrowsForNullAuState() {
    try {
      crawler = new MyNewContentCrawler(mau, spec, null);
      fail("Calling makeNewContentCrawler with a null AuState"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testReturnsProperType() {
    assertEquals(Crawler.NEW_CONTENT, crawler.getType());
    assertEquals("New Content", crawler.getTypeString());
  }

  public void testIsWholeAU() {
    assertTrue(crawler.isWholeAU());
  }

  public void testShouldFollowLink() {
    assertTrue(((NewContentCrawler)crawler).shouldFollowLink());
  }

  //Will try to fetch startUrl, content parser will return no urls,
  //so we should only cache the start url
  public void testDoCrawlOnePageNoLinks() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl);
    assertTrue(crawler.doCrawl());
    Set cachedUrls = cus.getCachedUrls();
    Set expected = SetUtil.set(startUrl, permissionPage);
    assertEquals(expected, cachedUrls);
  }

  public void testPassesParamsToUrlCacherDefault() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl);

    assertTrue(crawler.doCrawl());

    MockUrlCacher uc = (MockUrlCacher)mau.makeUrlCacher(startUrl);
    BitSet fetchFlags = uc.getFetchFlags();
    assertFalse(fetchFlags.get(UrlCacher.REFETCH_FLAG));
    assertTrue(fetchFlags.get(UrlCacher.CLEAR_DAMAGE_FLAG));
    assertTrue(fetchFlags.get(UrlCacher.REFETCH_IF_DAMAGE_FLAG));
  }

  public void testPassesParamsToUrlCacherParamsNeg() {
    Properties p = new Properties();
    p.setProperty(FollowLinkCrawler.PARAM_REFETCH_IF_DAMAGED, "false");
    p.setProperty(FollowLinkCrawler.PARAM_CLEAR_DAMAGE_ON_FETCH, "false");
    ConfigurationUtil.addFromProps(p);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl);

    assertTrue(crawler.doCrawl());

    MockUrlCacher uc = (MockUrlCacher)mau.makeUrlCacher(startUrl);
    BitSet fetchFlags = uc.getFetchFlags();
    assertFalse(fetchFlags.get(UrlCacher.REFETCH_FLAG));
    assertFalse(fetchFlags.get(UrlCacher.CLEAR_DAMAGE_FLAG));
    assertFalse(fetchFlags.get(UrlCacher.REFETCH_IF_DAMAGE_FLAG));
  }

  public void testPassesParamsToUrlCacherParamsPos() {
    Properties p = new Properties();
    p.setProperty(FollowLinkCrawler.PARAM_REFETCH_IF_DAMAGED, "true");
    p.setProperty(FollowLinkCrawler.PARAM_CLEAR_DAMAGE_ON_FETCH, "true");
    ConfigurationUtil.addFromProps(p);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl);

    assertTrue(crawler.doCrawl());

    MockUrlCacher uc = (MockUrlCacher)mau.makeUrlCacher(startUrl);
    BitSet fetchFlags = uc.getFetchFlags();
    assertFalse(fetchFlags.get(UrlCacher.REFETCH_FLAG));
    assertTrue(fetchFlags.get(UrlCacher.CLEAR_DAMAGE_FLAG));
    assertTrue(fetchFlags.get(UrlCacher.REFETCH_IF_DAMAGE_FLAG));
  }

  public void testMultipleStartingUrls() {
    List urls = ListUtil.list("http://www.example.com/link1.html",
			      "http://www.example.com/link2.html",
			      "http://www.example.com/link3.html",
			      startUrl);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    for (int ix=0; ix<urls.size(); ix++) {
      String curUrl = (String)urls.get(ix);
      mau.addUrl(curUrl);
      crawlRule.addUrlToCrawl(curUrl);
    }

    spec = new SpiderCrawlSpec(urls, ListUtil.list(permissionPage), crawlRule, 1);
    crawler = new MyNewContentCrawler(mau, spec, new MockAuState());
    ((BaseCrawler)crawler).daemonPermissionCheckers = ListUtil.list(new MockPermissionChecker(1));

    assertTrue(crawler.doCrawl());
    Set expected = SetUtil.fromList(urls);
    expected.add(permissionPage);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testDoCrawlOnePageWithOneLinkSuccessful() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    mau.addUrl(startUrl, false, true);
    extractor.addUrlsToReturn(startUrl, SetUtil.set(url1));
    mau.addUrl(url1, false, true);
    crawlRule.addUrlToCrawl(url1);

    assertTrue(doCrawl0(crawler));
    Set expected = SetUtil.set(permissionPage, startUrl, url1);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testDoCrawlNotRefetchPages() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    String url2="http://www.example.com/blah2.html";
    String url3="http://www.example.com/blah3.html";
    String url4="http://www.example.com/blah4.html";
    mau.addUrl(startUrl, false, true);
    extractor.addUrlsToReturn(startUrl,
			      SetUtil.set(url1, url2, url3, url4));
    extractor.addUrlsToReturn(url1, SetUtil.set(url1, url2, url3, url4));
    extractor.addUrlsToReturn(url2, SetUtil.set(url1, url2, url3, url4));
    extractor.addUrlsToReturn(url3, SetUtil.set(url1, url2, url3, url4));
    extractor.addUrlsToReturn(url4, SetUtil.set(url1, url2, url3, url4));
    mau.addUrl(url1, false, true);
    mau.addUrl(url2, false, true);
    mau.addUrl(url3, false, true);
    mau.addUrl(url4, false, true);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);
    crawlRule.addUrlToCrawl(url4);

    assertTrue(doCrawl0(crawler));
    Set expected =
      SetUtil.set(permissionPage, startUrl, url1, url2, url3, url4);
    assertEquals(expected, cus.getCachedUrls());

    assertNumTimesCrawled(1, url1, cus);
    assertNumTimesCrawled(1, url2, cus);
    assertNumTimesCrawled(1, url3, cus);
    assertNumTimesCrawled(1, url4, cus);
    assertNumTimesCrawled(1, startUrl, cus);
  }

  public void assertNumTimesCrawled(int num, String url,
				    MockCachedUrlSet cus) {
    assertEquals("Crawled "+url+" an unexpected number of times.",
		 1, cus.getNumCacheAttempts(url));
  }

  public void testRecrawlNotRefetchPages() {
    SpecialLinkExtractorArchivalUnit mau =
      new SpecialLinkExtractorArchivalUnit(3);
    mau.setPlugin(new MockPlugin(getMockLockssDaemon()));
    mau.setAuId("MyMockTestAu");
    mau.addUrl(permissionPage);
    mau.setLinkExtractor("text/html", extractor);

    spec =
      new SpiderCrawlSpec(startUrls, ListUtil.list(permissionPage),
			  crawlRule, 90);
    mau.setCrawlSpec(spec);

    crawler = new MyNewContentCrawler(mau, spec, new MockAuState());
    ((BaseCrawler)crawler).daemonPermissionCheckers =
      ListUtil.list(new MockPermissionChecker(1));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    String url2="http://www.example.com/blah2.html";
    String url3="http://www.example.com/blah3.html";
    String url4="http://www.example.com/blah4.html";
    mau.addUrl(startUrl, false, true);
    extractor.addUrlsToReturn(startUrl,
			      SetUtil.set(url1, url2, url3, url4));
    extractor.addUrlsToReturn(url1, SetUtil.set(url1, url2, url3, url4));
    extractor.addUrlsToReturn(url2, SetUtil.set(url1, url2, url3, url4));
    extractor.addUrlsToReturn(url3, SetUtil.set(url1, url2, url3, url4));
    extractor.addUrlsToReturn(url4, SetUtil.set(url1, url2, url3, url4));
    mau.addUrl(url1, false, true);
    mau.addUrl(url2, false, true);
    mau.addUrl(url3, false, true);
    mau.addUrl(url4, false, true);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);
    crawlRule.addUrlToCrawl(url4);

    assertTrue(doCrawl0(crawler));
    Set expected =
      SetUtil.set(permissionPage, startUrl, url1, url2, url3, url4);
    assertEquals(expected, cus.getCachedUrls());

    assertNumTimesCrawled(1, url1, cus);
    assertNumTimesCrawled(1, url2, cus);
    assertNumTimesCrawled(1, url3, cus);
    assertNumTimesCrawled(1, url4, cus);
    assertNumTimesCrawled(1, startUrl, cus);
  }


  public void testReturnsFalseWhenFailingUnretryableExceptionThrownOnStartUrl() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    MyMockUnretryableCacheException exception =
      new MyMockUnretryableCacheException("Test exception");
    mau.addUrl(startUrl, exception, DEFAULT_DEFAULT_RETRY_COUNT);

    assertFalse(doCrawl0(crawler));
    Set expected = SetUtil.set(permissionPage);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testReturnsTrueWhenNonFailingUnretryableExceptionThrownOnStartUrl() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    MyMockCacheException exception =
      new MyMockCacheException("Test exception");
    mau.addUrl(startUrl, exception, DEFAULT_DEFAULT_RETRY_COUNT);

    assertTrue(doCrawl0(crawler));
    Set expected = SetUtil.set(permissionPage);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testReturnsFalseWhenIOExceptionThrownOnStartUrl() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, new IOException("Test exception"), DEFAULT_DEFAULT_RETRY_COUNT);

    assertFalse(doCrawl0(crawler));
    Set expected = SetUtil.set(permissionPage);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testReturnsRetriesWhenRetryableExceptionThrownOnStartUrl() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    MyMockRetryableCacheException exception =
      new MyMockRetryableCacheException("Test exception");
    mau.addUrl(startUrl, exception, DEFAULT_DEFAULT_RETRY_COUNT-1);
    String url1="http://www.example.com/blah.html";
    mau.addUrl(url1, false, true);
    extractor.addUrlsToReturn(startUrl, SetUtil.set(url1));
    crawlRule.addUrlToCrawl(url1);

    assertTrue(doCrawl0(crawler));
    Set expected = SetUtil.set(permissionPage, startUrl, url1);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testReturnsFalseWhenOneOfStartUrlsFailedToBeFetched() {
    String startUrl2 = "http://www.foo.com/default.html";
    String permissionPage2 = "http://www.foo.com/permission.html";
    List permissionList = ListUtil.list(permissionPage, permissionPage2);
    mau.addUrl(permissionPage2);
    crawlRule.addUrlToCrawl(permissionPage2);

    List updatedStartUrls = ListUtil.list(startUrl, startUrl2);
    spec = new SpiderCrawlSpec(updatedStartUrls, permissionList, crawlRule, 1);
    mau.setCrawlSpec(spec);
    crawler = new MyNewContentCrawler(mau, spec, new MockAuState());
    ((BaseCrawler)crawler).daemonPermissionCheckers = ListUtil.list(new MockPermissionChecker(2));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    MyMockRetryableCacheException exception =
      new MyMockRetryableCacheException("Test exception");
    mau.addUrl(startUrl, exception, DEFAULT_DEFAULT_RETRY_COUNT);
    mau.addUrl(startUrl2, true, true);
    crawlRule.addUrlToCrawl(startUrl2);

    assertFalse(crawler.doCrawl());
    Set expected = SetUtil.set(startUrl2, permissionPage, permissionPage2);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testReturnsDoesntRetryWhenUnretryableExceptionThrownOnStartUrl() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    MyMockUnretryableCacheException exception =
      new MyMockUnretryableCacheException("Test exception");
    mau.addUrl(startUrl, exception, DEFAULT_DEFAULT_RETRY_COUNT-1);

    assertFalse(doCrawl0(crawler));
    Set expected = SetUtil.set(permissionPage);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testRetryNumSetByParamOnStartUrl() {
    int retryNum = DEFAULT_DEFAULT_RETRY_COUNT + 3;
    assertTrue("Test is worthless unless retryNum is greater than "
	       +"DEFAULT_DEFAULT_RETRY_COUNT", retryNum > DEFAULT_DEFAULT_RETRY_COUNT);
    Properties p = new Properties();
    p.setProperty(PARAM_DEFAULT_RETRY_COUNT, String.valueOf(retryNum));
    p.setProperty(NewContentCrawler.PARAM_DEFAULT_RETRY_DELAY, "0");
    ConfigurationUtil.addFromProps(p);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl,
	       new MyMockRetryableCacheException("Test exception"),
	       retryNum-1);

    String url1="http://www.example.com/blah.html";
    mau.addUrl(url1, false, true);
    crawlRule.addUrlToCrawl(url1);
    extractor.addUrlsToReturn(startUrl, SetUtil.set(url1));

    assertTrue(crawler.doCrawl());
    Set expected = SetUtil.set(permissionPage, startUrl, url1);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testReturnsTrueWhenNonFailingExceptionThrownOnStartUrl() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, new CacheException.NoRetryDeadLinkException("Test exception"), 1);

    assertTrue(crawler.doCrawl());
  }

  public void testPluginThrowsRuntimeException() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, new ExpectedRuntimeException("Test exception"), 1);

    assertFalse(crawler.doCrawl());
  }

  /** test recording mime-types and urls during a crawl */
  public void testKeepMimeTypeUrl() {
    ConfigurationUtil.addFromArgs(CrawlerStatus.PARAM_RECORD_URLS, "mime",
				  CrawlerStatus.PARAM_KEEP_URLS, "mime");
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html";  
    String url3 = "http://www.example.com/link3.html";
    String url4 = "http://www.example.com/link4.html";
    mau.addUrlContype(startUrl, false, true, "text/html");
    mau.addUrlContype(url1, false, true, "text/html; charset=UTF-8");
    mau.addUrlContype(url2, false, true, "image/png");
    mau.addUrlContype(url3, false, true, "image/png");
    mau.addUrlContype(url4, false, true, "text/plain");
    extractor.addUrlsToReturn(startUrl,
			      SetUtil.set(url1, url2, url3, url4));   
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);
    crawlRule.addUrlToCrawl(url4);
    crawler.doCrawl();
    CrawlerStatus crawlStatus = crawler.getStatus();
    assertEquals(6, crawlStatus.getNumFetched());
    assertEmpty(crawlStatus.getUrlsOfMimeType("no-such-mimetype"));
    assertEquals(SetUtil.set(startUrl, url1),
		 SetUtil.theSet(crawlStatus.getUrlsOfMimeType("text/html")));
    assertEquals(2, crawlStatus.getNumUrlsOfMimeType("text/html"));   
    assertEquals(SetUtil.set(url2, url3),
		 SetUtil.theSet(crawlStatus.getUrlsOfMimeType("image/png")));
    assertEquals(2, crawlStatus.getNumUrlsOfMimeType("image/png"));   
    assertEquals(1, crawlStatus.getNumUrlsOfMimeType("text/plain"));   
  }

  /** test recording just the number of urls for mime-types during a crawl */
  public void testNotKeepUrlMimeType() {
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html";  
    String url3 = "http://www.example.com/link3.html";
    String url4 = "http://www.example.com/link4.html";
    mau.addUrlContype(startUrl, false, true, "bla-ba-type");
    mau.addUrlContype(url1, false, true, "bla-content-type");
    mau.addUrlContype(url2, false, true, "bla-content-type");
    mau.addUrlContype(url3, false, true, "bla-content-type");
    mau.addUrl(url4, false, true);        
    extractor.addUrlsToReturn(startUrl,
			      SetUtil.set(url1, url2, url3, url4));   
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);
    crawlRule.addUrlToCrawl(url4);
    ConfigurationUtil.addFromArgs(CrawlerStatus.PARAM_RECORD_URLS,
 				  "none");
    crawler.doCrawl();
    CrawlerStatus crawlStatus = crawler.getStatus();
    assertEquals(6, crawlStatus.getNumFetched());                             
    assertTrue(crawlStatus.getUrlsOfMimeType("bla-content-type").isEmpty());
    assertTrue(crawlStatus.getUrlsOfMimeType("text/html").isEmpty());
    assertTrue(crawlStatus.getUrlsOfMimeType("bla-ba-type").isEmpty());
    assertEquals(3, crawlStatus.getNumUrlsOfMimeType("bla-content-type"));  
    assertEquals(1, crawlStatus.getNumUrlsOfMimeType("bla-ba-type"));  
    assertEquals(1, crawlStatus.getNumUrlsOfMimeType("text/html"));  
  }
  
  /** test status update of pending urls   */
  public void testGetPendingUrls() {
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html";  
    String url3 = "http://www.example.com/link3.html";    
    String url4 = "http://www.example.com/link4.html";
    mau.addUrl(startUrl, false, true);          
    mau.addUrl(url1, true, true);               
    mau.addUrl(url2, true, true);
    mau.addUrl(url3, true, true);
    mau.addUrl(url4, true, true);   
    extractor.addUrlsToReturn(startUrl,
			      ListUtil.list(url1, url2, url3, url4));   
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);   
    crawlRule.addUrlToCrawl(url4);
    crawler.doCrawl();
    MyCrawlerStatus crawlStatus = (MyCrawlerStatus)crawler.getStatus();
    assertEquals(Crawler.STATUS_SUCCESSFUL, crawlStatus.getCrawlStatus());
    assertEquals(0, crawlStatus.getNumUrlsWithErrors());
    assertEquals(5, crawlStatus.getNumParsed());    
    assertEquals(0, crawlStatus.getNumPending());    
    assertEquals(ListUtil.fromArray(new String[] {
      "add", startUrl,
      "remove", startUrl,
      "add", url1, "add", url2, "add", url3, "add", url4,
      "remove", url1, "remove", url2, "remove", url3, "remove", url4}),
		 crawlStatus.pendingEvents);
  }

  public void testGetStatusStartUrls() {
    CrawlerStatus crawlStatus = crawler.getStatus();
    assertEquals(startUrls, crawlStatus.getStartUrls());
  }

  public void testGetStatusCrawlDone() {
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html";
    String url3 = "http://www.example.com/link3.html";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl);
    extractor.addUrlsToReturn(startUrl, SetUtil.set(url1, url2, url3));
    mau.addUrl(url1).setContentSize(42);
    mau.addUrl(url2).setContentSize(3);;
    mau.addUrl(url3).setContentSize(1000);;
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);

    ConfigurationUtil.addFromArgs(CrawlerStatus.PARAM_RECORD_URLS, "all");
    long expectedStart = TimeBase.nowMs();
    crawler.doCrawl();
    long expectedEnd = TimeBase.nowMs();
    CrawlerStatus crawlStatus = crawler.getStatus();
    assertEquals(expectedStart, crawlStatus.getStartTime());
    assertEquals(expectedEnd, crawlStatus.getEndTime());
    assertEquals(5, crawlStatus.getNumFetched());
    assertEquals(SetUtil.set(startUrl, url1, url2, url3, permissionPage),
		 SetUtil.theSet(crawlStatus.getUrlsFetched()));
    assertEquals(0, crawlStatus.getNumExcluded());
    assertEmpty(crawlStatus.getUrlsExcluded());
    assertEquals(4, crawlStatus.getNumParsed());
    assertEquals(1045, crawlStatus.getContentBytesFetched());
    assertEquals(SetUtil.set("Publisher"),
		 SetUtil.theSet(crawlStatus.getSources()));
  }

  public void testGetStatusCrawlDoneExcluded() {
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html";
    String url3 = "http://www.example.com/link3.html";
    String url4 = "http://www.example.com/link4.html";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl);
    extractor.addUrlsToReturn(startUrl,
			      SetUtil.set(url1, url2, url3, url4));
    mau.addUrl(url1).setContentSize(42);
    mau.addUrl(url2).setContentSize(3);;
    mau.addUrl(url3).setContentSize(1000);;
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);

    ConfigurationUtil.addFromArgs(CrawlerStatus.PARAM_RECORD_URLS, "all");
    long expectedStart = TimeBase.nowMs();
    crawler.doCrawl();
    long expectedEnd = TimeBase.nowMs();
    CrawlerStatus crawlStatus = crawler.getStatus();
    assertEquals(expectedStart, crawlStatus.getStartTime());
    assertEquals(expectedEnd, crawlStatus.getEndTime());
    assertEquals(5, crawlStatus.getNumFetched());
    assertEquals(SetUtil.set(startUrl, url1, url2, url3, permissionPage),
		 SetUtil.theSet(crawlStatus.getUrlsFetched()));
    assertEquals(1, crawlStatus.getNumExcluded());
    assertEquals(SetUtil.set(url4),
		 SetUtil.theSet(crawlStatus.getUrlsExcluded()));
    assertEquals(4, crawlStatus.getNumParsed());
    assertEquals(1045, crawlStatus.getContentBytesFetched());
    assertEquals(SetUtil.set("Publisher"),
		 SetUtil.theSet(crawlStatus.getSources()));
  }

  public void testGetStatusCrawlDoneNotModified() {
    System.err.println("TEST START");
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html";
    String url3 = "http://www.example.com/link3.html";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, true, true);
    extractor.addUrlsToReturn(startUrl, SetUtil.set(url1, url2, url3));
    mau.addUrl(url1);
    //     mau.addUrl(url1, true, true);
    mau.addUrl(url2);
    mau.addUrl(url3);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);

    long expectedStart = TimeBase.nowMs();
    crawler.doCrawl();
    long expectedEnd = TimeBase.nowMs();
    CrawlerStatus crawlStatus = crawler.getStatus();
    assertEquals(expectedStart, crawlStatus.getStartTime());
    assertEquals(expectedEnd, crawlStatus.getEndTime());
    assertEquals(4, crawlStatus.getNumFetched());
    assertEquals(1, crawlStatus.getNumNotModified());
    assertEquals(SetUtil.set(url1, url2, url3, permissionPage),
		 SetUtil.theSet(crawlStatus.getUrlsFetched()));
    assertEquals(ListUtil.list(startUrl),
		 crawlStatus.getUrlsNotModified());
    assertEquals(4, crawlStatus.getNumParsed());
  }

  public void testGetStatusCrawlDoneParsed() {
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html";
    String url3 = "http://www.example.com/link3.html";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl);
    extractor.addUrlsToReturn(startUrl, SetUtil.set(url1, url2, url3));
    mau.addUrl(url1);
    mau.addUrl(url2);
    mau.addUrl(url3);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);

    long expectedStart = TimeBase.nowMs();
    crawler.doCrawl();
    long expectedEnd = TimeBase.nowMs();
    CrawlerStatus crawlStatus = crawler.getStatus();
    assertEquals(expectedStart, crawlStatus.getStartTime());
    assertEquals(expectedEnd, crawlStatus.getEndTime());
    assertEquals(5, crawlStatus.getNumFetched());
    assertEquals(SetUtil.set(url1, url2, url3, startUrl),
		 SetUtil.theSet(crawlStatus.getUrlsParsed()));
    assertEquals(4, crawlStatus.getNumParsed());
  }

  public void testGetStatusNotStarted() {
    assertEquals(Crawler.STATUS_QUEUED,
		 crawler.getStatus().getCrawlStatus());
  }

  public void testGetStatusIncomplete() {
    crawler.getStatus().signalCrawlStarted();
    assertEquals(Crawler.STATUS_ACTIVE,
		 crawler.getStatus().getCrawlStatus());
  }

  public void testGetStatusSuccessful() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl);
    crawler.doCrawl();
    assertEquals(Crawler.STATUS_SUCCESSFUL,
		 crawler.getStatus().getCrawlStatus());
  }

  public void testGetStatusError() {
    String url1="http://www.example.com/blah.html";
    mau.addUrl(startUrl, false, true);
    extractor.addUrlsToReturn(startUrl, SetUtil.set(url1));
    mau.addUrl(url1, new IOException("Test exception"), DEFAULT_DEFAULT_RETRY_COUNT);
    crawlRule.addUrlToCrawl(url1);

    crawler.doCrawl();
    CrawlerStatus crawlStatus = crawler.getStatus();

    assertEquals(Crawler.STATUS_FETCH_ERROR,
		 crawlStatus.getCrawlStatus());
    assertEquals(1, crawlStatus.getNumUrlsWithErrors());
  }

  public void testGetStatusErrorStartUrl() {
    mau = newMyMockArchivalUnit();
    mau.setPlugin(new MockPlugin(getMockLockssDaemon()));
    mau.setAuId("MyMockTestAu");
    mcus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    spec = new SpiderCrawlSpec(ListUtil.list(permissionPage),
			       ListUtil.list(permissionPage), crawlRule, 1);
    mau.setCrawlSpec(spec);
    crawler = new MyNewContentCrawler(mau, spec, aus);

    mau.addUrl(permissionPage,
	       new CacheException.ExpectedNoRetryException("Test exception"),
 	       DEFAULT_DEFAULT_RETRY_COUNT);

    crawler.doCrawl();
    CrawlerStatus crawlStatus = crawler.getStatus();

    Map expectedErrors = MapUtil.map(permissionPage, "Test exception");
    assertEquals(expectedErrors, crawlStatus.getUrlsWithErrors());
    assertEquals(1, crawlStatus.getNumUrlsWithErrors());
    assertEquals(Crawler.STATUS_NO_PUB_PERMISSION,
		 crawlStatus.getCrawlStatus());
    assertEquals("Can't fetch permission page", 
		 crawlStatus.getCrawlStatusMsg());
  }

  public void testGetStatusRepoErrorStartUrl() {
    mau = newMyMockArchivalUnit();
    mau.setPlugin(new MockPlugin(getMockLockssDaemon()));
    mau.setAuId("MyMockTestAu");
    mcus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    spec = new SpiderCrawlSpec(ListUtil.list(permissionPage),
			       ListUtil.list(permissionPage), crawlRule, 1);
    crawler = new MyNewContentCrawler(mau, spec, aus);

    mau.setCrawlSpec(spec);
    mau.addUrl(permissionPage,
	       new CacheException.RepositoryException("Test exception"),
 	       DEFAULT_DEFAULT_RETRY_COUNT);

    crawler.doCrawl();
    CrawlerStatus crawlStatus = crawler.getStatus();

    assertEquals(Crawler.STATUS_REPO_ERR, crawlStatus.getCrawlStatus());
    assertEquals("Repository error", crawlStatus.getCrawlStatusMsg());
    Map expectedErrors = MapUtil.map(permissionPage,
				     "Can't store page: Test exception");
    assertEquals(expectedErrors, crawlStatus.getUrlsWithErrors());
    assertEquals(1, crawlStatus.getNumUrlsWithErrors());
  }

  public void testGetStatusRepoErrorNotStartUrl() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    mau.addUrl(startUrl, false, true);
    extractor.addUrlsToReturn(startUrl, SetUtil.set(url1));
    mau.addUrl(url1,
 	       new CacheException.RepositoryException("Test exception"),
  	       DEFAULT_DEFAULT_RETRY_COUNT);
    crawlRule.addUrlToCrawl(url1);
    assertFalse(crawler.doCrawl());
    CrawlerStatus crawlStatus = crawler.getStatus();

    assertEquals(Crawler.STATUS_REPO_ERR, crawlStatus.getCrawlStatus());
    assertEquals("Repository error", crawlStatus.getCrawlStatusMsg());
    Map expectedErrors = MapUtil.map(url1, "Can't store page: Test exception");
    assertEquals(expectedErrors, crawlStatus.getUrlsWithErrors());
    assertEquals(1, crawlStatus.getNumUrlsWithErrors());
  }

  public void testOverwritesSingleStartingUrlsOneLevel() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, true, true);

    assertTrue(crawler.doCrawl());
    Set expected = SetUtil.set(startUrl, permissionPage);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testOverwritesMultipleStartingUrlsOneLevel() {
    String startUrl2 = "http://www.foo.com/default.html";
    String permissionPage2 = "http://www.foo.com/default.html";
    List permissionList = ListUtil.list(permissionPage, permissionPage2);
    mau.addUrl(permissionPage2);
    crawlRule.addUrlToCrawl(permissionPage2);

    List updatedStartUrls = ListUtil.list(startUrl, startUrl2);
    spec = new SpiderCrawlSpec(updatedStartUrls, permissionList, crawlRule, 1);
    mau.setCrawlSpec(spec);

    crawler = new MyNewContentCrawler(mau, spec, new MockAuState());
    ((BaseCrawler)crawler).daemonPermissionCheckers = ListUtil.list(new MockPermissionChecker(2));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, true, true);
    mau.addUrl(startUrl2, true, true);
    crawlRule.addUrlToCrawl(startUrl2);

    assertTrue(crawler.doCrawl());
    Set expected = SetUtil.set(startUrl, startUrl2, permissionPage, permissionPage2);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testOverwritesStartingUrlsMultipleLevels() {
    spec = new SpiderCrawlSpec(startUrls, ListUtil.list(permissionPage), crawlRule, 2);
    crawler = new MyNewContentCrawler(mau, spec, new MockAuState());
    ((BaseCrawler)crawler).daemonPermissionCheckers = ListUtil.list(new MockPermissionChecker(1));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/dir/link3.html";
    String url4= "http://www.example.com/dir/link9.html";

    //    mau.setLinkExtractor(extractor);
    extractor.addUrlsToReturn(startUrl, SetUtil.set(url1, url2, url3));
    extractor.addUrlsToReturn(url1, SetUtil.set(url4));

    mau.addUrl(startUrl, true, true);
    mau.addUrl(url1, true, true);
    mau.addUrl(url2, true, true);
    mau.addUrl(url3, true, true);
    mau.addUrl(url4, true, true);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);
    crawlRule.addUrlToCrawl(url4);

    assertTrue(crawler.doCrawl());
    // Refetched depth is 2 thus, url4 is not cached as it already exists.
    Set expected = SetUtil.set(startUrl, permissionPage, url1, url2, url3);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testOverwritesMultipleStartingUrlsMultipleLevel() {
    String startUrl2 = "http://www.foo.com/default.html";
    String permissionPage2 = "http://www.foo.com/default.html";
    List permissionList = ListUtil.list(permissionPage, permissionPage2);
    mau.addUrl(permissionPage2);
    crawlRule.addUrlToCrawl(permissionPage2);

    List updatedStartUrls = ListUtil.list(startUrl, startUrl2);
    spec = new SpiderCrawlSpec(updatedStartUrls, permissionList, crawlRule, 2);
    mau.setCrawlSpec(spec);

    crawler = new MyNewContentCrawler(mau, spec, new MockAuState());
    ((BaseCrawler)crawler).daemonPermissionCheckers =
      ListUtil.list(new MockPermissionChecker(2));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/dir/link3.html";
    String url4= "http://www.foo.com/dir/link4.html";
    String url5= "http://www.foo.com/dir/link5.html";
    String url6= "http://www.foo.com/dir/link6.html";

    //    mau.setLinkExtractor(extractor);
    extractor.addUrlsToReturn(startUrl, SetUtil.set(url1, url2, url3));
    extractor.addUrlsToReturn(startUrl2, SetUtil.set(url4, url5));
    extractor.addUrlsToReturn(url4, SetUtil.set(url6));

    mau.addUrl(startUrl, true, true);
    mau.addUrl(startUrl2, true, true);
    mau.addUrl(url1, true, true);
    mau.addUrl(url2, true, true);
    mau.addUrl(url3, true, true);
    mau.addUrl(url4, true, true);
    mau.addUrl(url5, true, true);
    mau.addUrl(url6, true, true);
    crawlRule.addUrlToCrawl(startUrl2);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);
    crawlRule.addUrlToCrawl(url4);
    crawlRule.addUrlToCrawl(url5);
    crawlRule.addUrlToCrawl(url6);

    assertTrue(crawler.doCrawl());
    // url6 is not expected as refetch depth is 2 and it already exists.
    Set expected = SetUtil.set(permissionPage, permissionPage2, startUrl,
			       startUrl2, url1, url2, url3, url4, url5);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testCrawlWindow() {
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html";
    String url3 = "http://www.example.com/link3.html";

    spec.setCrawlWindow(new MyMockCrawlWindow(4));
    mau.setCrawlSpec(spec);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl);
    extractor.addUrlsToReturn(startUrl, ListUtil.list(url1, url2, url3));
    mau.addUrl(url1);
    mau.addUrl(url2);
    mau.addUrl(url3);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);


    crawler = new MyNewContentCrawler(mau, spec, aus);
    ((BaseCrawler)crawler).daemonPermissionCheckers =
      ListUtil.list(new MockPermissionChecker(100));

    mau.setLinkExtractor("text/html", extractor);
    assertFalse(crawler.doCrawl());
    Set expected = SetUtil.set(startUrl, permissionPage, url1);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testOutsideOfWindow1() {
    String url1= "http://www.example.com/link1.html";
    spec.setCrawlWindow(new MyMockCrawlWindow(0));
    extractor.addUrlsToReturn(startUrl, SetUtil.set(url1));
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, true, true);
    assertFalse(crawler.doCrawl());
  }

  public void testOutsideOfWindow2() {
    String url1= "http://www.example.com/link1.html";
    spec.setCrawlWindow(new MyMockCrawlWindow(1));
    extractor.addUrlsToReturn(startUrl, SetUtil.set(url1));
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, true, true);
    mau.addUrl(url1);
    assertFalse(crawler.doCrawl());
  }

  public void testDoesCollectHttpsOnStartingUrls() {
    //we will collect ftp, gopher https eventually,
    //it is not yet implemented though and this test
    //is to make sure urls in this protocols will not
    //break the whole system

    String startUrl = "https://www.example.com/index.html";
    String url1 = "https://www.example.com/link1.html";
    List startUrls = ListUtil.list(startUrl);

    //See if we're running in a version of java that throws on trying
    //to construct a https URL
    boolean httpsUrlThrows = false;
    try {
      URL url = new URL(startUrl);
    } catch (Exception e) {
      httpsUrlThrows = true;
    }


    spec = new SpiderCrawlSpec(startUrls,
			       ListUtil.list(permissionPage), crawlRule, 1);
    crawler = new MyNewContentCrawler(mau, spec, aus);
    ((BaseCrawler)crawler).daemonPermissionCheckers =
      ListUtil.list(new MockPermissionChecker(100));

    mau.addUrl(startUrl, false, true);
    crawlRule.addUrlToCrawl(startUrl);
    extractor.addUrlsToReturn(startUrl, SetUtil.set(url1));
    mau.addUrl(url1, false, true);
    crawlRule.addUrlToCrawl(url1);

    mau.setLinkExtractor("text/html", extractor);
    if (httpsUrlThrows) {
      assertFalse("Crawler shouldn't succeed when trying system can't construct a https URL", crawler.doCrawl());
    } else {
      assertTrue("Crawler should succeed when system can construct a https URL",
		 crawler.doCrawl());
    }

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    Set expectedSet = SetUtil.set(permissionPage);

    if (!httpsUrlThrows) {
      expectedSet.add(startUrl);
    }
    assertEquals(expectedSet, cus.getCachedUrls());
  }

  private static void setProperty(String prop, String value) {
    ConfigurationUtil.addFromArgs(prop, value);
  }

  boolean doCrawl0(BaseCrawler crawler) {
    crawler.setCrawlConfig(ConfigManager.getCurrentConfig());
    return crawler.doCrawl0();
  }

  private static class MyNewContentCrawler extends NewContentCrawler {
    protected MyNewContentCrawler(ArchivalUnit au, CrawlSpec spec,
				  AuState aus) {
      super(au, spec, aus);
      crawlStatus =
	new MyCrawlerStatus(au, ((SpiderCrawlSpec)spec).getStartingUrls(),
			    getTypeString());
    }

    // Ordered set makes results easier to check
    protected Set newSet() {
      return new ListOrderedSet();
    }

    /** suppress these actions */
    protected void doCrawlEndActions() {
    }
  }

  static class MyCrawlerStatus extends CrawlerStatus {
    List pendingEvents = new ArrayList();

    public MyCrawlerStatus(ArchivalUnit au, Collection startUrls,
			   String type) {
      super(au, startUrls, type);
    }

    public synchronized void addPendingUrl(String url) {
      super.addPendingUrl(url);
      pendingEvents.addAll(ListUtil.list("add", url));
    }

    public synchronized void removePendingUrl(String url) {
      super.removePendingUrl(url);
      pendingEvents.addAll(ListUtil.list("remove", url));
    }
  }

  private class SpecialLinkExtractorArchivalUnit extends MockArchivalUnit {
    int numTimesToParse;

    public SpecialLinkExtractorArchivalUnit(int numTimesToParse) {
      super();
      this.numTimesToParse = numTimesToParse;
    }

    public LinkExtractor getLinkExtractor(String mimeType) {
      if (numTimesToParse > 0) {
	numTimesToParse--;
	return extractor;
      }
      return null;
    }
  }

  protected class MyMockArchivalUnit extends MockArchivalUnit {
    MyMockUrlCacher lastMmuc;

    protected MockUrlCacher makeMockUrlCacher(String url) {
      lastMmuc = new MyMockUrlCacher(url, this);
      return lastMmuc;
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


  protected class MyMockUrlCacher extends MockUrlCacher {
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
    public void setFatal() {
      attributeBits.set(CacheException.ATTRIBUTE_FATAL);
    }
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestNewContentCrawler.class.getName()};
    junit.textui.TestRunner.main(testCaseList);
  }

}

