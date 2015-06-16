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

package org.lockss.crawler;
import java.util.*;
import java.net.*;
import java.io.*;

import org.apache.commons.collections.set.ListOrderedSet;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.crawler.TestFollowLinkCrawler.MyMockUrlFetcher;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.plugin.UrlFetcher.FetchResult;
import org.lockss.test.*;
import org.lockss.test.MockCrawler.MockCrawlerFacade;
import org.lockss.state.*;
import org.lockss.alert.*;
import org.lockss.util.urlconn.*;
import org.lockss.extractor.*;

import static org.lockss.crawler.BaseCrawler.PARAM_DEFAULT_RETRY_COUNT;
import static org.lockss.crawler.BaseCrawler.DEFAULT_DEFAULT_RETRY_COUNT;

/**
 * Tests for the new content crawler.
 */
public class TestFollowLinkCrawler2 extends LockssTestCase {

  protected MockLockssDaemon theDaemon;
  protected CrawlManagerImpl crawlMgr;
  protected MyMockArchivalUnit mau = null;
  protected MockCachedUrlSet mcus = null;
  protected MockAuState aus;
  protected static List testUrlList = ListUtil.list("http://example.com");
  protected MockCrawlRule crawlRule = null;
  protected String startUrl = "http://www.example.com/index.html";
  protected String permissionPage = "http://www.example.com/permission.html";
  protected List startUrls;
  protected MyFollowLinkCrawler crawler = null;
  protected MockLinkExtractor extractor = new MockLinkExtractor();
  protected CrawlerFacade cf;
  private MockPlugin plug;

  public void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated(10);

    theDaemon = getMockLockssDaemon();
    crawlMgr = new NoPauseCrawlManagerImpl();
    theDaemon.setCrawlManager(crawlMgr);
    crawlMgr.initService(theDaemon);
    
    theDaemon.getAlertManager();
    
    plug = new MockPlugin(getMockLockssDaemon());
    plug.initPlugin(getMockLockssDaemon());
    mau = newMyMockArchivalUnit();
    mau.setPlugin(plug);
    mau.setAuId("MyMockTestAu");
    aus = new MockAuState(mau);
    startUrls = ListUtil.list(startUrl);
    mcus = (MockCachedUrlSet)mau.getAuCachedUrlSet();

    crawlRule = new MockCrawlRule();
    crawlRule.addUrlToCrawl(startUrl);
    crawlRule.addUrlToCrawl(permissionPage);
    mau.addUrl(permissionPage);
    mau.setStartUrls(startUrls);
    mau.setPermissionUrls(ListUtil.list(permissionPage));
    mau.setCrawlRule(crawlRule);
    mau.setRefetchDepth(1);
    crawlMgr.newCrawlRateLimiter(mau);
    crawler = new MyFollowLinkCrawler(mau, aus);
    crawler.setDaemonPermissionCheckers(
      ListUtil.list(new MockPermissionChecker(1)));
    mau.setLinkExtractor("*", extractor);
    Properties p = new Properties();
    p.setProperty(FollowLinkCrawler.PARAM_DEFAULT_RETRY_DELAY, "0");
    p.setProperty(FollowLinkCrawler.PARAM_MIN_RETRY_DELAY, "0");
    p.setProperty("org.lockss.log.FollowLinkCrawler.level", "3");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    cf = crawler.getCrawlerFacade();
  }

  MyMockArchivalUnit newMyMockArchivalUnit() {
    NodeManager nodeManager = new MockNodeManager();
    MyMockArchivalUnit mau = new MyMockArchivalUnit();
    getMockLockssDaemon().setNodeManager(nodeManager, mau);
    return mau;
  }

  public void testIsWholeAU() {
    assertTrue(crawler.isWholeAU());
  }

  public void testShouldFollowLink() {
    assertTrue(crawler.shouldFollowLink());
  }

  public void testRefetchDepth() {
    FollowLinkCrawler ncc1 = new FollowLinkCrawler(mau, aus);
    assertEquals(1, ncc1.getRefetchDepth());
    FollowLinkCrawler ncc2 = new FollowLinkCrawler(mau, aus);
    CrawlReq req = new CrawlReq(mau);
    ncc2.setCrawlReq(req);
    assertEquals(1, ncc2.getRefetchDepth());
    req.setRefetchDepth(39);
    FollowLinkCrawler ncc3 = new FollowLinkCrawler(mau, aus);
    ncc3.setCrawlReq(req);
    assertEquals(39, ncc3.getRefetchDepth());
  }

  //Will try to fetch startUrl, content parser will return no urls,
  //so we should only cache the start url
  public void testDoCrawlOnePageNoLinks() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl);
    assertTrue(crawler.doCrawl());
    Set cachedUrls = cus.getCachedUrls();
    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cachedUrls);
  }

  public void testPassesParamsToUrlFetcherDefault() {
    mau.addUrl(startUrl);
    MockUrlFetcher muf = (MockUrlFetcher)crawler.makeUrlFetcher(startUrl);
    BitSet fetchFlags = muf.getFetchFlags();
    assertFalse(fetchFlags.get(UrlCacher.REFETCH_FLAG));
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
    
    mau.setStartUrls(urls);
    mau.setPermissionUrls(ListUtil.list(permissionPage));
    mau.setCrawlRule(crawlRule);

    crawler = new MyFollowLinkCrawler(mau, aus);
    crawler.setDaemonPermissionCheckers(
        ListUtil.list(new MockPermissionChecker(1)));

    assertTrue(crawler.doCrawl());
    Set expected = SetUtil.fromList(urls);
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
    Set expected = SetUtil.set(startUrl, url1);
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
      SetUtil.set(startUrl, url1, url2, url3, url4);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testReturnsFalseWhenFailingUnretryableExceptionThrownOnStartUrl() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    MyMockUnretryableCacheException exception =
      new MyMockUnretryableCacheException("Test exception");
    mau.addUrl(startUrl, exception, DEFAULT_DEFAULT_RETRY_COUNT);

    assertFalse(doCrawl0(crawler));
    Set expected = SetUtil.set();
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testReturnsFalseWhenNonFailingUnretryableExceptionThrownOnStartUrl() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    MyMockCacheException exception =
      new MyMockCacheException("Test exception");
    mau.addUrl(startUrl, exception, DEFAULT_DEFAULT_RETRY_COUNT);

    assertFalse(doCrawl0(crawler));
    Set expected = SetUtil.set();
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testReturnsFalseWhenIOExceptionThrownOnStartUrl() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, new IOException("Test exception"), DEFAULT_DEFAULT_RETRY_COUNT);

    assertFalse(doCrawl0(crawler));
    Set expected = SetUtil.set();
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testReturnsFalseWhenOneOfStartUrlsFailedToBeFetched() {
    String startUrl2 = "http://www.foo.com/default.html";
    String permissionPage2 = "http://www.foo.com/permission.html";
    List permissionList = ListUtil.list(permissionPage, permissionPage2);
    mau.addUrl(permissionPage2);
    crawlRule.addUrlToCrawl(permissionPage2);

    List updatedStartUrls = ListUtil.list(startUrl2, startUrl);
    mau.setStartUrls(updatedStartUrls);
    mau.setPermissionUrls(permissionList);
    mau.setCrawlRule(crawlRule);

    crawler = new MyFollowLinkCrawler(mau, AuUtil.getAuState(mau));
    crawler.setDaemonPermissionCheckers(
        ListUtil.list(new MockPermissionChecker(2)));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    MyMockRetryableCacheException exception =
      new MyMockRetryableCacheException("Test exception");
    mau.addUrl(startUrl, exception, DEFAULT_DEFAULT_RETRY_COUNT);
    mau.addUrl(startUrl2, true, true);
    crawlRule.addUrlToCrawl(startUrl2);

    assertFalse(crawler.doCrawl());
    Set expected = SetUtil.set(startUrl2);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testReturnsDoesntRetryWhenUnretryableExceptionThrownOnStartUrl() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    MyMockUnretryableCacheException exception =
      new MyMockUnretryableCacheException("Test exception");
    mau.addUrl(startUrl, exception, DEFAULT_DEFAULT_RETRY_COUNT-1);

    assertFalse(doCrawl0(crawler));
    Set expected = SetUtil.set();
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testReturnsFalseWhenNonFailingExceptionThrownOnStartUrl() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, new CacheException.NoRetryDeadLinkException("Test exception"), 1);

    assertFalse(crawler.doCrawl());
  }

  public void testPluginThrowsRuntimeException() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, new ExpectedRuntimeException("Test exception"), 1);

    assertFalse(crawler.doCrawl());
  }

  public void testPluginThrowsOnPermissionFetch() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(permissionPage,
	       new ExpectedRuntimeException("Test exception (perm)"), 1);
    mau.addUrl(startUrl);

    assertFalse(crawler.doCrawl());
  }

  public void testPluginThrowsInGetLinkExtractor() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl);
    mau.getLinkExtractorThrows = new ExpectedRuntimeException("getLE()");

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
    CrawlerStatus crawlStatus = crawler.getCrawlerStatus();
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
    CrawlerStatus crawlStatus = crawler.getCrawlerStatus();
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
    MyCrawlerStatus crawlStatus = (MyCrawlerStatus)crawler.getCrawlerStatus();
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
    CrawlerStatus crawlStatus = crawler.getCrawlerStatus();
    assertEquals(startUrls, crawlStatus.getStartUrls());
  }

  public void testGetStatusCrawlDone() {
    // Prevent timestamps from causing alerts not to be equal
    TimeBase.setSimulated(1000);
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
    CrawlerStatus crawlStatus = crawler.getCrawlerStatus();
    assertEquals(expectedStart, crawlStatus.getStartTime());
    assertEquals(expectedEnd, crawlStatus.getEndTime());
    assertEquals(5, crawlStatus.getNumFetched());
    assertEquals(SetUtil.set(startUrl, permissionPage, url1, url2, url3),
		 SetUtil.theSet(crawlStatus.getUrlsFetched()));
    assertEquals(0, crawlStatus.getNumExcluded());
    assertEmpty(crawlStatus.getUrlsExcluded());
    assertEquals(4, crawlStatus.getNumParsed());
    assertEquals(1045, crawlStatus.getContentBytesFetched());
    assertEquals(SetUtil.set("Publisher"),
		 SetUtil.theSet(crawlStatus.getSources()));
    Alert al = Alert.auAlert(Alert.CRAWL_FINISHED, mau);
    al.setAttribute(Alert.ATTR_TEXT, alertTxtOk);
    assertEquals(ListUtil.list(al), crawler.alerts);
  }

  private String alertTxtOk =
    "Crawl finished successfully: 5 files fetched, 0 warnings\n\n" +
    "Refetch Depth: 1\n" +
    "Max Depth: 1000\n" +
    "Actual Depth: 2";

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
    CrawlerStatus crawlStatus = crawler.getCrawlerStatus();
    assertEquals(expectedStart, crawlStatus.getStartTime());
    assertEquals(expectedEnd, crawlStatus.getEndTime());
    assertEquals(5, crawlStatus.getNumFetched());
    assertEquals(SetUtil.set(startUrl,permissionPage, url1, url2, url3),
		 SetUtil.theSet(crawlStatus.getUrlsFetched()));
    assertEquals(1, crawlStatus.getNumExcluded());
    assertEquals(SetUtil.set(url4),
		 SetUtil.theSet(crawlStatus.getUrlsExcluded()));
    assertEquals(4, crawlStatus.getNumParsed());
    assertEquals(1045, crawlStatus.getContentBytesFetched());
    assertEquals(SetUtil.set("Publisher"),
		 SetUtil.theSet(crawlStatus.getSources()));
  }

  public void testReferrers() {
    ConfigurationUtil.addFromArgs(CrawlerStatus.PARAM_RECORD_URLS,
				  "all",
				  CrawlerStatus.PARAM_RECORD_REFERRERS_MODE,
				  "All");
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html";
    String url3 = "http://www.example.com/link3.html";
    String url4 = "http://www.example.com/link4.html";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl);
    extractor.addUrlsToReturn(startUrl,
			      SetUtil.set(url1));
    extractor.addUrlsToReturn(url1,
			      SetUtil.set(url1, url2));
    extractor.addUrlsToReturn(url2,
			      SetUtil.set(url3, url1));
    extractor.addUrlsToReturn(url3,
			      SetUtil.set(url3, url4, url2));
    mau.addUrl(url1).setContentSize(42);
    mau.addUrl(url2).setContentSize(3);;
    mau.addUrl(url3).setContentSize(1000);;
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);

    // Must create crawler after config set
    crawler = new MyFollowLinkCrawler(mau, aus);
    crawler.setDaemonPermissionCheckers(
      ListUtil.list(new MockPermissionChecker(1)));
    crawler.doCrawl();
    CrawlerStatus crawlStatus = crawler.getCrawlerStatus();
    assertEquals(ListUtil.list(startUrl, url2), crawlStatus.getReferrers(url1));
    assertEquals(ListUtil.list(url1, url3), crawlStatus.getReferrers(url2));
    assertEquals(ListUtil.list(url2), crawlStatus.getReferrers(url3));
    assertEquals(ListUtil.list(url3), crawlStatus.getReferrers(url4));
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
    CrawlerStatus crawlStatus = crawler.getCrawlerStatus();
    assertEquals(expectedStart, crawlStatus.getStartTime());
    assertEquals(expectedEnd, crawlStatus.getEndTime());
    assertEquals(5, crawlStatus.getNumFetched());
    assertEquals(SetUtil.set(url1, url2, url3, startUrl),
		 SetUtil.theSet(crawlStatus.getUrlsParsed()));
    assertEquals(4, crawlStatus.getNumParsed());
  }

  public void testGetStatusNotStarted() {
    assertEquals(Crawler.STATUS_QUEUED,
		 crawler.getCrawlerStatus().getCrawlStatus());
  }

  public void testGetStatusIncomplete() {
    crawler.getCrawlerStatus().signalCrawlStarted();
    assertEquals(Crawler.STATUS_ACTIVE,
		 crawler.getCrawlerStatus().getCrawlStatus());
  }

  public void testGetStatusSuccessful() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl);
    crawler.doCrawl();
    assertEquals(Crawler.STATUS_SUCCESSFUL,
		 crawler.getCrawlerStatus().getCrawlStatus());
  }

  private String alertTxtErr =
    "Crawl finished with error: Fetch error: 2 files fetched, 0 warnings, 1 error\n\n" +
    "Refetch Depth: 1\n" +
    "Max Depth: 1000\n" +
    "Actual Depth: 2";

  public void testGetStatusErrorStartUrl() {
    mau = newMyMockArchivalUnit();
    mau.setPlugin(new MockPlugin(getMockLockssDaemon()));
    mau.setAuId("MyMockTestAu");
    mcus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.setStartUrls(ListUtil.list(permissionPage));
    mau.setCrawlRule(crawlRule);
    
    crawler = new MyFollowLinkCrawler(mau, aus);

    mau.addUrl(permissionPage,
	       new CacheException.ExpectedNoRetryException("Test exception"),
 	       DEFAULT_DEFAULT_RETRY_COUNT);

    crawler.doCrawl();
    CrawlerStatus crawlStatus = crawler.getCrawlerStatus();

    Map expectedErrors = MapUtil.map(permissionPage, "Test exception");
    assertEquals(expectedErrors, crawlStatus.getUrlsWithErrors());
    assertEquals(1, crawlStatus.getNumUrlsWithErrors());
    assertEquals(Crawler.STATUS_NO_PUB_PERMISSION,
		 crawlStatus.getCrawlStatus());
    assertEquals(CrawlerStatus.UNABLE_TO_FETCH_PERM_ERR_MSG, 
		 crawlStatus.getCrawlStatusMsg());
  }

  public void testGetStatusPermissionError() {
    mau = newMyMockArchivalUnit();
    mau.setPlugin(new MockPlugin(getMockLockssDaemon()));
    mau.setAuId("MyMockTestAu");
    mcus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.setStartUrls(ListUtil.list(permissionPage));
    mau.setCrawlRule(crawlRule);
    
    crawler = new MyFollowLinkCrawler(mau, aus);

    mau.addUrl(permissionPage,
	       new CacheException.RepositoryException("Test exception"),
 	       DEFAULT_DEFAULT_RETRY_COUNT);

    crawler.doCrawl();
    CrawlerStatus crawlStatus = crawler.getCrawlerStatus();
    assertEquals(Crawler.STATUS_NO_PUB_PERMISSION, crawlStatus.getCrawlStatus());
    assertEquals(CrawlerStatus.UNABLE_TO_FETCH_PERM_ERR_MSG,
                 crawlStatus.getCrawlStatusMsg());
    Map expectedErrors = MapUtil.map(permissionPage,
				     "Test exception");
    assertEquals(expectedErrors, crawlStatus.getUrlsWithErrors());
    assertEquals(1, crawlStatus.getNumUrlsWithErrors());
  }

  public void testOverwritesSingleStartingUrlsOneLevel() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, true, true);

    assertTrue(crawler.doCrawl());
    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testOverwritesMultipleStartingUrlsOneLevel() {
    String startUrl2 = "http://www.foo.com/default.html";
    String permissionPage2 = "http://www.foo.com/default.html";
    List permissionList = ListUtil.list(permissionPage, permissionPage2);
    mau.addUrl(permissionPage2);
    crawlRule.addUrlToCrawl(permissionPage2);

    List updatedStartUrls = ListUtil.list(startUrl, startUrl2);
    mau.setStartUrls(updatedStartUrls);
    mau.setPermissionUrls(permissionList);
    mau.setCrawlRule(crawlRule);

    crawler = new MyFollowLinkCrawler(mau, aus);
    crawler.setDaemonPermissionCheckers(
        ListUtil.list(new MockPermissionChecker(2)));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, true, true);
    mau.addUrl(startUrl2, true, true);
    crawlRule.addUrlToCrawl(startUrl2);

    assertTrue(crawler.doCrawl());
    Set expected = SetUtil.set(startUrl, startUrl2);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testOverwritesStartingUrlsMultipleLevels() {
    mau.setStartUrls(startUrls);
    mau.setPermissionUrls(ListUtil.list(permissionPage));
    mau.setCrawlRule(crawlRule);
    mau.setRefetchDepth(2);

    crawler = new MyFollowLinkCrawler(mau, aus);
    crawler.setDaemonPermissionCheckers(
        ListUtil.list(new MockPermissionChecker(1)));

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
    Set expected = SetUtil.set(startUrl, url1, url2, url3);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testOverwritesMultipleStartingUrlsMultipleLevel() {
    String startUrl2 = "http://www.foo.com/default.html";
    String permissionPage2 = "http://www.foo.com/default.html";
    List permissionList = ListUtil.list(permissionPage, permissionPage2);
    mau.addUrl(permissionPage2);
    crawlRule.addUrlToCrawl(permissionPage2);

    List updatedStartUrls = ListUtil.list(startUrl, startUrl2);
    mau.setStartUrls(updatedStartUrls);
    mau.setPermissionUrls(permissionList);
    mau.setCrawlRule(crawlRule);
    mau.setRefetchDepth(2);

    crawler = new MyFollowLinkCrawler(mau, aus);
    crawler.setDaemonPermissionCheckers(
      ListUtil.list(new MockPermissionChecker(2)));

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
    Set expected = SetUtil.set(startUrl,
			       startUrl2, url1, url2, url3, url4, url5);
    assertEquals(expected, cus.getCachedUrls());
  }

  List permuteBy(List data, List<Integer> indices) {
    List res = new ArrayList(indices.size());
    for (int ix : indices) {
      res.add(data.get(ix));
    }
    return res;
  }

  public void testPriorityQueue(int refetchDepth,
				boolean hasContent,
				Comparator<CrawlUrl> cmp,
				List expFetched) {
    testPriorityQueue(refetchDepth, hasContent, cmp, expFetched, true);
  }

  String url7= "http://www.example.com/extra/aux.html";

  public void testPriorityQueue(int refetchDepth,
				boolean hasContent,
				Comparator<CrawlUrl> cmp,
				List expFetched,
				boolean expResult) {
    List permissionList = ListUtil.list(permissionPage);
    
    mau.setStartUrls(startUrls);
    mau.setPermissionUrls(permissionList);
    mau.setCrawlRule(crawlRule);
    mau.setRefetchDepth(refetchDepth);

    mau.setCrawlUrlComparator(cmp);


    crawler = new MyFollowLinkCrawler(mau, aus);
    crawler.setDaemonPermissionCheckers(
      ListUtil.list(new MockPermissionChecker(1)));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1= "http://www.example.com/issue1/toc";
    String url2= "http://www.example.com/issue1/art1.html";
    String url3= "http://www.example.com/issue1/art2.html";
    String url4= "http://www.example.com/issue2/toc";
    String url5= "http://www.example.com/issue2/art1.html";
    String url6= "http://www.example.com/issue2/art2.html";

    String url8= "http://www.example.com/images/img1.png";
    String url9= "http://www.example.com/images/img2.png";
    // url10 will be found both times url8 is parsed,so is the canary for
    // the child list not being cleared upon reparse
    String url10= "http://www.example.com/images/img3.png";

    List<String> urls = ListUtil.list(startUrl,
				      url1, url2, url3, url4, url5,
				      url6, url7, url8, url9, url10);
    extractor.addUrlsToReturn(startUrl, SetUtil.set(url1, url4));
    extractor.addUrlsToReturn(url1, ListUtil.list(url2, url3));
    extractor.addUrlsToReturn(url2, ListUtil.list(url7));
    extractor.addUrlsToReturn(url7, ListUtil.list(url8));
    extractor.addUrlsToReturn(url4, ListUtil.list(url5, url6, url8));
    extractor.addUrlsToReturn(url6, ListUtil.list(url9));
    extractor.addUrlsToReturn(url8, ListUtil.list(url10));
    // Add a cycle for good measure
    extractor.addUrlsToReturn(url9, ListUtil.list(url2));

    mau.addUrl(startUrl, false, true);
    for (String url : urls) {
      mau.addUrl(url, hasContent, true);
      crawlRule.addUrlToCrawl(url);
    }
    assertEquals(expResult, crawler.doCrawl());
    List actualFetched = crawler.getFetchedUrls();

    assertEquals(permuteBy(urls, expFetched), actualFetched);
  }

  public void testPriorityQueueBreadthFirst1() {
    testPriorityQueue(1, false, null,
		      ListUtil.list(0, 1, 4, 2, 3, 5, 6, 8, 7, 9, 10));
  }

  public void testPriorityQueueBreadthFirst3() {
    testPriorityQueue(3, false, null,
		      ListUtil.list(0, 1, 4, 2, 3, 5, 6, 8, 7, 9, 10));
  }

  public void testPriorityQueueBreadthFirstRecrawl1() {
    testPriorityQueue(1, true, null,
		      ListUtil.list(0));
  }

  public void testPriorityQueueBreadthFirstRecrawl3() {
    testPriorityQueue(3, true, null,
		      ListUtil.list(0, 1, 4, 2, 3, 5, 6, 8));
  }

  public void testPriorityQueueAlphabeticBreadthFirst1() {
    testPriorityQueue(1, false, new CrawlQueue.AlphabeticalBreadthFirstUrlComparator(),
		      ListUtil.list(0, 1, 4, 8, 2, 3, 5, 6, 7, 9, 10));
  }

  public void testPriorityQueueAlphabeticBreadthFirst3() {
    testPriorityQueue(3, false, new CrawlQueue.AlphabeticalBreadthFirstUrlComparator(),
		      ListUtil.list(0, 1, 4, 8, 2, 3, 5, 6, 7, 9, 10));
  }

  public void testPriorityQueueAlphabeticBreadthFirstRecrawl1() {
    testPriorityQueue(1, true, new CrawlQueue.AlphabeticalBreadthFirstUrlComparator(),
		      ListUtil.list(0));
  }

  public void testPriorityQueueAlphabeticBreadthFirstRecrawl3() {
    testPriorityQueue(3, true, new CrawlQueue.AlphabeticalBreadthFirstUrlComparator(),
		      ListUtil.list(0, 1, 4, 8, 2, 3, 5, 6));
  }

  public void testPriorityQueueAlphabetic1() {
    testPriorityQueue(1, false, new AlphabeticUrlOrderComparator(),
		      ListUtil.list(0, 1, 2, 7, 8, 10, 3, 4, 5, 6, 9));
  }

  public void testPriorityQueueAlphabetic3() {
    testPriorityQueue(3, false, new AlphabeticUrlOrderComparator(),
		      ListUtil.list(0, 1, 2, 7, 8, 10, 3, 4, 5, 6, 9));
  }

  public void testPriorityQueueAlphabeticRecrawl1() {
    testPriorityQueue(1, true, new AlphabeticUrlOrderComparator(),
		      ListUtil.list(0));
  }

  public void testPriorityQueueAlphabeticRecrawl3() {
    testPriorityQueue(3, true, new AlphabeticUrlOrderComparator(),
		      ListUtil.list(0, 1, 2, 3, 4, 8, 5, 6));
  }

  public void testPriorityQueueBreadthFirst1MaxDepth4() {
    ConfigurationUtil.addFromArgs(FollowLinkCrawler.PARAM_MAX_CRAWL_DEPTH,
				  "4");
    testPriorityQueue(1, false, null,
		      ListUtil.list(0, 1, 4, 2, 3, 5, 6, 8, 7, 9, 10));
  }

  public void testPriorityQueueBreadthFirst1MaxDepth3() {
    ConfigurationUtil.addFromArgs(FollowLinkCrawler.PARAM_MAX_CRAWL_DEPTH,
				  "3");
    testPriorityQueue(1, false, null,
		      ListUtil.list(0, 1, 4, 2, 3, 5, 6, 8),
		      false);
  }

  public void testPriorityQueueAlphabetic1MaxDepth4() {
    ConfigurationUtil.addFromArgs(FollowLinkCrawler.PARAM_MAX_CRAWL_DEPTH,
				  "4");
    testPriorityQueue(1, false, new AlphabeticUrlOrderComparator(),
		      ListUtil.list(0, 1, 2, 7, 3, 4, 8, 10, 5, 6, 9));
  }

  public void testPriorityQueueAlphabetic1MaxDepth3() {
    ConfigurationUtil.addFromArgs(FollowLinkCrawler.PARAM_MAX_CRAWL_DEPTH,
				  "3");
    testPriorityQueue(1, false, new AlphabeticUrlOrderComparator(),
		      ListUtil.list(0, 1, 2, 3, 4, 8, 5, 6),
		      false);
  }

  public void testPriorityQueueAlphabeticRecrawl1MaxDepth4() {
    ConfigurationUtil.addFromArgs(FollowLinkCrawler.PARAM_MAX_CRAWL_DEPTH,
				  "4");
    testPriorityQueue(1, true, new AlphabeticUrlOrderComparator(),
		      ListUtil.list(0));
  }

  public void testPriorityQueueAlphabeticRecrawl3MaxDepth4() {
    ConfigurationUtil.addFromArgs(FollowLinkCrawler.PARAM_MAX_CRAWL_DEPTH,
				  "4");
    testPriorityQueue(3, true, new AlphabeticUrlOrderComparator(),
		      ListUtil.list(0, 1, 2, 3, 4, 8, 5, 6));
  }

  public void testPriorityQueueAlphabeticRecrawl3MaxDepth3() {
    ConfigurationUtil.addFromArgs(FollowLinkCrawler.PARAM_MAX_CRAWL_DEPTH,
				  "3");
    testPriorityQueue(3, true, new AlphabeticUrlOrderComparator(),
		      ListUtil.list(0, 1, 2, 3, 4, 8, 5, 6),
		      false);
  }

  public void testPriorityQueueComparatorThrows() {
    AlphabeticUrlOrderComparator cmp = new AlphabeticUrlOrderComparator();
    cmp.setErrorUrl(url7);
    testPriorityQueue(3, false, cmp,
		      ListUtil.list(0, 1, 2),
		      false);
  }


  public void testPerHostPermissionOk() {
    mau.setStartUrls(startUrls);
    mau.setPermissionUrls(ListUtil.list(permissionPage));
    mau.setCrawlRule(crawlRule);

    crawler = new MyFollowLinkCrawler(mau, aus);
    MyMockPermissionChecker perm = new MyMockPermissionChecker();
    crawler.setDaemonPermissionCheckers(ListUtil.list(perm));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1= "http://www.example.com/link1.html";
    String url2= "http://ggg.example.com/link2.html";
    String url3= "http://www.example.com/dir/link3.html";
    String url4= "http://www.example.org/dir/link9.html";

    String perm1 = "http://ggg.example.com/perm.txt";
    String perm2 = "http://www.example.org/perm.txt";

    extractor.addUrlsToReturn(startUrl, SetUtil.set(url1, url2, url3));
    extractor.addUrlsToReturn(url1, SetUtil.set(url4));

    mau.addUrl(startUrl);
    mau.addUrl(url1);
    mau.addUrl(url2);
    mau.addUrl(url3);
    mau.addUrl(url4);

    mau.setPerHostPermissionPath("/perm.txt");

    mau.addUrl(perm1);
    mau.addUrl(perm2);

    perm.setResult(permissionPage, true);
    perm.setResult(perm1, true);
    perm.setResult(perm2, true);

    assertTrue(crawler.doCrawl());

    Set expected = SetUtil.set(startUrl, url1, url2,
			       url3, url4);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testPerHostPermissionMissing() {
    mau.setStartUrls(startUrls);
    mau.setPermissionUrls(ListUtil.list(permissionPage));
    mau.setCrawlRule(crawlRule);

    crawler = new MyFollowLinkCrawler(mau, aus);
    MyMockPermissionChecker perm = new MyMockPermissionChecker();
    crawler.setDaemonPermissionCheckers(ListUtil.list(perm));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1= "http://www.example.com/link1.html";
    String url2= "http://ggg.example.com/link2.html";
    String url3= "http://www.example.com/dir/link3.html";
    String url4= "http://www.example.org/dir/link9.html";

    String perm1 = "http://ggg.example.com/perm.txt";
    String perm2 = "http://www.example.org/perm.txt";

    extractor.addUrlsToReturn(startUrl, SetUtil.set(url1, url2, url3));
    extractor.addUrlsToReturn(url1, SetUtil.set(url4));

    mau.addUrl(startUrl);
    mau.addUrl(url1);
    mau.addUrl(url2);
    mau.addUrl(url3);
    mau.addUrl(url4);

    mau.setPerHostPermissionPath("/perm.txt");

    mau.addUrl(perm1);
    mau.addUrl(perm2);

    perm.setResult(permissionPage, true);
    perm.setResult(perm2, true);

    assertFalse(crawler.doCrawl());

    Set expected = SetUtil.set(startUrl, url1,
			       url3, url4);
    assertEquals(expected, cus.getCachedUrls());
  }



  static class MyMockPermissionChecker implements PermissionChecker {
    Map<String,Boolean> permResult = new HashMap<String,Boolean>();

    void setResult(String url, boolean result) {
      permResult.put(url, result);
    }

    MyMockPermissionChecker() {
    }

    public boolean checkPermission(Crawler.CrawlerFacade cf,
				   Reader reader, String permissionUrl) {
      Boolean result = permResult.get(permissionUrl);
      if (result == null) {
	return false;
      }
      return result;
    }
  }

  public void testCrawlWindow() {
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html";
    String url3 = "http://www.example.com/link3.html";

    mau.setCrawlWindow(new MyMockCrawlWindow(3));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl);
    extractor.addUrlsToReturn(startUrl, ListUtil.list(url1, url2, url3));
    mau.addUrl(url1);
    mau.addUrl(url2);
    mau.addUrl(url3);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);


    crawler = new MyFollowLinkCrawler(mau, aus);
    crawler.setDaemonPermissionCheckers(
      ListUtil.list(new MockPermissionChecker(100)));

    mau.setLinkExtractor("text/html", extractor);
    assertFalse(crawler.doCrawl());
    Set expected = SetUtil.set(startUrl, url1);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testOutsideOfWindow1() {
    String url1= "http://www.example.com/link1.html";
    mau.setCrawlWindow(new MyMockCrawlWindow(0));
    extractor.addUrlsToReturn(startUrl, SetUtil.set(url1));
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, true, true);
    assertFalse(crawler.doCrawl());
  }

  public void testOutsideOfWindow2() {
    String url1= "http://www.example.com/link1.html";
    mau.setCrawlWindow(new MyMockCrawlWindow(1));
    extractor.addUrlsToReturn(startUrl, SetUtil.set(url1));
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, true, true);
    mau.addUrl(url1);
    assertFalse(crawler.doCrawl());
  }

  private static void setProperty(String prop, String value) {
    ConfigurationUtil.addFromArgs(prop, value);
  }

  boolean doCrawl0(BaseCrawler crawler) {
    crawler.setCrawlConfig(ConfigManager.getCurrentConfig());
    return crawler.doCrawl0();
  }

  private class MyFollowLinkCrawler extends FollowLinkCrawler {
    List fetchedUrls = new ArrayList();
    List parsedUrls = new ArrayList();
    List<Alert> alerts = new ArrayList<Alert>();
    List<PermissionChecker> daemonPermissionCheckers;
    
    protected MyFollowLinkCrawler(ArchivalUnit au, AuState aus) {
      super(au, aus);
      crawlStatus = new MyCrawlerStatus(au, au.getStartUrls(),
			    null);
      setCrawlManager(TestFollowLinkCrawler2.this.crawlMgr);
    }

    public List getParsedUrls() {
      return parsedUrls;
    }

    // Ordered set makes results easier to check
    @Override
    protected Set newSet() {
      return new ListOrderedSet();
    }

    /** suppress these actions */
    @Override
    protected void doCrawlEndActions() {
    }

    /**
     * This is weird, but updateCacheStats is the only method 
     * that is only called when a file is fetched
     */
    @Override
    protected void updateCacheStats(FetchResult res, CrawlUrlData curl) {
      fetchedUrls.add(curl.getUrl());
      super.updateCacheStats(res, curl);
    }
    
    @Override
    protected void parse(CrawlUrlData curl) {
      parsedUrls.add(curl.getUrl());
      super.parse(curl);
    }
    
    List getFetchedUrls() {
      return fetchedUrls;
    }

    @Override
    protected CrawlUrlData newCrawlUrlData(String url, int depth) {
      return new MyCrawlUrlData(url, depth);
    }

    @Override
    protected void raiseAlert(Alert alert, String text) {
      if (text != null) {
	alert.setAttribute(Alert.ATTR_TEXT, text);
      }
      alerts.add(alert);
    }
    
    List<PermissionChecker> getDaemonPermissionCheckers() {
      if(daemonPermissionCheckers != null) {
        return daemonPermissionCheckers;
      } else {
        return super.getDaemonPermissionCheckers();
      }
    }
    
    public void setDaemonPermissionCheckers(List<PermissionChecker> pc) {
      this.daemonPermissionCheckers = pc;
    }

  }

  static class MyCrawlUrlData extends CrawlUrlData {
    public MyCrawlUrlData(String url, int depth) {
      super(url, depth);
    }

    // Cause an error if a child is added twice.  Ensures that the child
    // list is cleared before reparse.
    @Override
    public void addChild(CrawlUrlData child, ReducedDepthHandler rdh) {
      if (isChild(child)) {
	throw new IllegalStateException("Attempt to add existing child");
      }
      super.addChild(child, rdh);
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
    MyMockUrlFetcher lastMmuf;
    RuntimeException getLinkExtractorThrows = null;

    protected MockUrlFetcher makeMockUrlFetcher(MockCrawlerFacade mcf,
        String url) {
      lastMmuf = new TestFollowLinkCrawler().new 
          MyMockUrlFetcher(mcf, url);
      return lastMmuf;
    }

    public LinkExtractor getLinkExtractor(String mimeType) {
      if (getLinkExtractorThrows != null) {
	throw getLinkExtractorThrows;
      }
      return super.getLinkExtractor(mimeType);
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

  static class AlphabeticUrlOrderComparator implements Comparator<CrawlUrl> {
    String errorUrl;

    public int compare(CrawlUrl curl1, CrawlUrl curl2) {
      if (errorUrl != null && (errorUrl.equals(curl1.getUrl())
			       || errorUrl.equals(curl2.getUrl()))) {
	throw new ExpectedRuntimeException("Comparator exception");
      }
      return curl1.getUrl().compareTo(curl2.getUrl());
    }

    void setErrorUrl(String url) {
      errorUrl = url;
    }
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestFollowLinkCrawler2.class.getName()};
    junit.textui.TestRunner.main(testCaseList);
  }

}

