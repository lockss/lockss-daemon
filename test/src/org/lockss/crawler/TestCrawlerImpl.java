/*
 * $Id: TestCrawlerImpl.java,v 1.3 2004-01-17 00:15:28 troberts Exp $
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

import java.io.*;
import java.util.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.plugin.*;

/**
 * This is the test class for org.lockss.crawler.CrawlerImpl
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */
public class TestCrawlerImpl extends LockssTestCase {
  private MockArchivalUnit mau = null;
  private List startUrls = null;

  private CrawlerImpl crawler = null;
  private CrawlSpec spec = null;

  public static final String EMPTY_PAGE = "";
  public static final String LINKLESS_PAGE = "Nothing here";

  public static final String startUrl = "http://www.example.com/index.html";
  private MockCrawlRule crawlRule;
  private MockAuState aus = new MockAuState();
  
  private MockContentParser parser = new MockContentParser();



  private static final String PARAM_RETRY_TIMES =
    Configuration.PREFIX + "CrawlerImpl.numCacheRetries";
  private static final int DEFAULT_RETRY_TIMES = 3;

  public static Class testedClasses[] = {
    org.lockss.crawler.CrawlerImpl.class
  };

  public static Class prerequisites[] = {
    TestCrawlRule.class
  };

  public void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated(10);

    mau = new MockArchivalUnit();
    mau.setPlugin(new MockPlugin());

    startUrls = ListUtil.list(startUrl);
    MockCachedUrlSet cus = new MyMockCachedUrlSet(mau, null);
    mau.setAuCachedUrlSet(cus);
    mau.setManifestPage(startUrl);
    crawlRule = new MockCrawlRule();
    crawlRule.addUrlToCrawl(startUrl);
    spec = new CrawlSpec(startUrls, crawlRule);
    crawler =
      CrawlerImpl.makeNewContentCrawler(mau, spec, aus);

    crawler.setParser(parser);
    Properties p = new Properties();
    p.setProperty(CrawlerImpl.PARAM_RETRY_PAUSE, "0");
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }

  //Tests for makeNewContentCrawler (mncc)
  public void testMnccThrowsForNullAu() {
    try {
      crawler =
	CrawlerImpl.makeNewContentCrawler(null, spec,
						 new MockAuState());
      fail("Calling makeNewContentCrawler with a null ArchivalUnit"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testMnccThrowsForNullCrawlSpec() {
    try {
      crawler =
	CrawlerImpl.makeNewContentCrawler(mau, null, new MockAuState());
      fail("Calling makeNewContentCrawler with a null CrawlSpec"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testMnccThrowsForNullAuState() {
    try {
      crawler =
	CrawlerImpl.makeNewContentCrawler(mau, spec, null);
      fail("Calling makeNewContentCrawler with a null CrawlSpec"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testDoCrawlThrowsForNullDeadline() {
    try {
      crawler.doCrawl(null);
      fail("Calling doCrawl with a null Deadline should throw "+
	   "an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }


  //Will try to fetch startUrl, content parser will return no urls,
  //so we should only cache the start url
  public void testDoCrawlOnePageNoLinks() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl);
    crawler.doCrawl(Deadline.MAX);
    Set cachedUrls = cus.getCachedUrls();
    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cachedUrls);
  }

//   //make work
//   public void testDoNotCrawlWithHttpsLink() {
//     String url = "https://www.example.com/web_link.html";

//     MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
//     cus.addUrl(startUrl);
//     parser.addUrlSetToReturn(startUrl, SetUtil.set(url));
//     cus.addUrl(url);
//     crawler.doCrawl(Deadline.MAX);
//     Set cachedUrls = cus.getCachedUrls();
//     Set expected = SetUtil.set(startUrl);
//     assertEquals(expected, cachedUrls);
//   }

  //Fetch startUrl, parser will return a single url that already exists
  //we should only cache startUrl
  public void testDoesNotCacheExistingFile() {
    String url1="http://www.example.com/blah.html";

    parser.setUrlToReturn(url1);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl, true, true);
    cus.addUrl(url1, true, true);

    crawler.doCrawl(Deadline.MAX);

    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cus.getCachedUrls());
  }

  //test that we don't cache a file that our crawl rules reject
  public void testDoesNotCacheFileWhichShouldNotBeCached() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl, false, false);

    crawler.doCrawl(Deadline.MAX);
    assertEquals(0, cus.getCachedUrls().size());
  }

  //test that we correctly parse content that has mime type
  //text/html; charset=US-ASCII
  public void testParsesFileWithCharsetAfterContentType() {
    String url = "http://www.example.com/link1.html";
    Properties props = new Properties();
    props.setProperty("content-type", "text/html; charset=US-ASCII");

    parser.setUrlToReturn(url);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl, false, true, props);
    cus.addUrl(url);
    crawlRule.addUrlToCrawl(url);

    crawler.doCrawl(Deadline.MAX);

    Set expected = SetUtil.set(startUrl, url);
    assertEquals(expected, cus.getCachedUrls());
  }

  //test that we're not case sensitive when checking content-type
  public void testParsesFileWithCapitilizedContentType() {
    String url = "http://www.example.com/link1.html";
    Properties props = new Properties();
    props.setProperty("content-type", "TEXT/HTML");


    parser.setUrlToReturn(url);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl, false, true, props);
    cus.addUrl(url);
    crawlRule.addUrlToCrawl(url);

    crawler.doCrawl(Deadline.MAX);

    Set expected = SetUtil.set(startUrl, url);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testDoesNotParseBadContentType() {
    String url = "http://www.example.com/link1.html";

    Properties props = new Properties();
    props.setProperty("content-type", "text/xml");


    parser.setUrlToReturn(url);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl, false, true, props);
    cus.addUrl(url);

    crawler.doCrawl(Deadline.MAX);

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


    spec = new CrawlSpec(urls, crawlRule);
    crawler =
      CrawlerImpl.makeNewContentCrawler(mau, spec, new MockAuState());
    
    crawler.setParser(parser);

    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.fromList(urls);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testOverwritesStartingUrlsOneLevel() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl, true, true);

    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testOverwritesStartingUrlsMultipleLevels() {
    spec = new CrawlSpec(startUrls, crawlRule, 2);
    crawler =
      CrawlerImpl.makeNewContentCrawler(mau, spec, new MockAuState());

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/dir/link3.html";
    String url4= "http://www.example.com/dir/link9.html";

    crawler.setParser(parser);
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

    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.set(startUrl, url1, url2, url3);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testWillNotParseExistingPagesForUrls() {
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

    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testWillNotCrawlExpiredDeadline() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl, false, true);

    Deadline deadline = Deadline.in(0);
    crawler.doCrawl(deadline);
    assertEquals(0, cus.getCachedUrls().size());
  }

  public void testCrawlingWithDeadlineThatExpires() {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=http://www.example.com/link1.html>link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a href=http://www.example.com/link2.html>link2</a>"+
      "<a href=http://www.example.com/link3.html>link3</a>";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    parser.addUrlSetToReturn(url1, SetUtil.set(url1, url2, url3));
    cus.addUrl(startUrl);
    cus.addUrl(url1);
    cus.addUrl(url2);
    cus.addUrl(url3);

    crawler.doCrawl(Deadline.in(2));
//     Set expected = SetUtil.set(startUrl, url1);
    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testAbortedCrawlDoesntStart() {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    parser.addUrlSetToReturn(url1, SetUtil.set(url1, url2, url3));
    cus.addUrl(startUrl);
    cus.addUrl(url1);
    cus.addUrl(url2);
    cus.addUrl(url3);

    crawler.abortCrawl();
    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.set();
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testDoesNotLoopOnSelfReferentialPage() {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
    parser.addUrlSetToReturn(url1, SetUtil.set(url1, url2, url3));
    cus.addUrl(startUrl);
    cus.addUrl(url1);
    cus.addUrl(url2);
    cus.addUrl(url3);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);

    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.set(startUrl, url1, url2, url3);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testDoesNotLoopOnSelfReferentialLoop() {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    String source1 =
      "<html><head><title>Test</title></head><body>"+
      "<a href="+url1+">link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a href="+url2+">link2</a>"+
      "<a href="+startUrl+">start page</a>"+
      "<a href="+url3+">link3</a>";

    String source2 =
      "<html><head><title>Test</title></head><body>"+
      "<a href="+startUrl+">link1</a>";


    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    parser.addUrlSetToReturn(startUrl,
			     SetUtil.set(url1, url2, url3, startUrl));
    parser.addUrlSetToReturn(url1, SetUtil.set(startUrl));
    cus.addUrl(startUrl);
    cus.addUrl(url1);
    cus.addUrl(url2);
    cus.addUrl(url3);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);

    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.set(startUrl, url1, url2, url3);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testReturnsTrueWhenCrawlSuccessful() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    cus.addUrl(startUrl, false, true);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
    cus.addUrl(url1, false, true);

    assertTrue(crawler.doCrawl(Deadline.MAX));
   }

  public void testReturnsFalseWhenExceptionThrown() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    cus.addUrl(startUrl, false, true);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
    cus.addUrl(url1, new IOException("Test exception"), DEFAULT_RETRY_TIMES);
    crawlRule.addUrlToCrawl(url1);

    assertFalse(crawler.doCrawl(Deadline.MAX));
  }

  public void testReturnsRetriesWhenExceptionThrown() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    cus.addUrl(startUrl, false, true);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
    cus.addUrl(url1, new IOException("Test exception"), DEFAULT_RETRY_TIMES-1);
    crawlRule.addUrlToCrawl(url1);

    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.set(startUrl, url1);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testRetryNumSetByParam() {
    int retryNum = DEFAULT_RETRY_TIMES + 3;
    assertTrue("Test is worthless unless retryNum is greater than "
	       +"DEFAULT_RETRY_TIMES", retryNum > DEFAULT_RETRY_TIMES);
    Properties p = new Properties();
    p.setProperty(PARAM_RETRY_TIMES, String.valueOf(retryNum));
    p.setProperty(CrawlerImpl.PARAM_RETRY_PAUSE, "0");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    cus.addUrl(startUrl, false, true);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
    cus.addUrl(url1, new IOException("Test exception"), retryNum-1);
    crawlRule.addUrlToCrawl(url1);

    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.set(startUrl, url1);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testCachesFailedFetches() {
    int retryNum = 3;
    Properties p = new Properties();
    p.setProperty(PARAM_RETRY_TIMES, String.valueOf(retryNum));
    p.setProperty(CrawlerImpl.PARAM_RETRY_PAUSE, "0");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    MyMockCachedUrlSet cus = (MyMockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    String url2="http://www.example.com/blah2.html";
    String url3="http://www.example.com/blah3.html";
    cus.addUrl(startUrl, false, true);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1, url2, url3));
    cus.addUrl(url2, false, true);
    parser.addUrlSetToReturn(url2, SetUtil.set(url1));
    cus.addUrl(url3, false, true);
    parser.addUrlSetToReturn(url3, SetUtil.set(url1));
    cus.addUrl(url1, new IOException("Test exception"), retryNum);

    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);

    crawler.doCrawl(Deadline.MAX);
    assertEquals(retryNum, cus.getNumCacheAttempts(url1));
  }


  public void testReturnsTrueWhenFileNotFoundExceptionThrown() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    cus.addUrl(startUrl, false, true);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
    cus.addUrl(url1, new FileNotFoundException("Test exception"), 0);
    crawlRule.addUrlToCrawl(url1);

    assertTrue(crawler.doCrawl(Deadline.MAX));
  }

  public void testGetStatusCrawlNotStarted() {
    Crawler.Status crawlStatus = crawler.getStatus();
    assertEquals(-1, crawlStatus.getStartTime());
    assertEquals(-1, crawlStatus.getEndTime());
    assertEquals(0, crawlStatus.getNumFetched());
    assertEquals(0, crawlStatus.getNumParsed());
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
    crawler.doCrawl(Deadline.MAX);
    long expectedEnd = TimeBase.nowMs();
    Crawler.Status crawlStatus = crawler.getStatus();
    assertEquals(expectedStart, crawlStatus.getStartTime());
    assertEquals(expectedEnd, crawlStatus.getEndTime());
    assertEquals(4, crawlStatus.getNumFetched());
    assertEquals(4, crawlStatus.getNumParsed());
  }

  public void testGetStatusIncomplete() {
    assertEquals(Crawler.STATUS_INCOMPLETE,
		 crawler.getStatus().getCrawlStatus());
  }

  public void testGetStatusSuccessful() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl);
    crawler.doCrawl(Deadline.MAX);
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

    crawler.doCrawl(Deadline.MAX);
    assertEquals(Crawler.STATUS_ERROR,
		 crawler.getStatus().getCrawlStatus());
  }


  private static List testUrlList = ListUtil.list("http://example.com");

  //Tests for makeRepairCrawler (mrc)
  public void testMrcThrowsForNullAu() {
    try {
      crawler =
	CrawlerImpl.makeRepairCrawler(null, spec, aus, testUrlList);
      fail("Calling makeRepairCrawler with a null ArchivalUnit"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testMrcThrowsForNullSpec() {
    try {
      crawler =
	CrawlerImpl.makeRepairCrawler(mau, null, aus, testUrlList);
      fail("Calling makeRepairCrawler with a null CrawlSpec"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testMrcThrowsForNullList() {
    try {
      crawler =
	CrawlerImpl.makeRepairCrawler(mau, spec, aus, null);
      fail("Calling makeRepairCrawler with a null repair list"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testMrcThrowsForEmptyList() {
    try {
      crawler =
	CrawlerImpl.makeRepairCrawler(mau, spec, aus, ListUtil.list());
      fail("Calling makeRepairCrawler with a empty repair list"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testRepairCrawlCallsForceCache() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String repairUrl = "http://example.com/forcecache.html";
    cus.addUrl(repairUrl);
    crawlRule.addUrlToCrawl(repairUrl);

    List repairUrls = ListUtil.list(repairUrl);
    spec = new CrawlSpec(startUrls, crawlRule, 1);
    crawler = CrawlerImpl.makeRepairCrawler(mau, spec, aus, repairUrls);

    crawler.doCrawl(Deadline.MAX);

    Set cachedUrls = cus.getForceCachedUrls();
    assertEquals(1, cachedUrls.size());
    assertTrue("cachedUrls: "+cachedUrls, cachedUrls.contains(repairUrl));
  }

  public void testRepairCrawlDoesntFollowLinks() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String repairUrl1 = "http://www.example.com/forcecache.html";
    String repairUrl2 = "http://www.example.com/link3.html";
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html";

    cus.addUrl(repairUrl1);
    parser.addUrlSetToReturn(repairUrl1, SetUtil.set(url1, url2, repairUrl2));
    cus.addUrl(repairUrl2);
    cus.addUrl(url1);
    cus.addUrl(url2);
    crawlRule.addUrlToCrawl(repairUrl1);
    crawlRule.addUrlToCrawl(repairUrl2);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);

    List repairUrls = ListUtil.list(repairUrl1, repairUrl2);
    spec = new CrawlSpec(startUrls, crawlRule, 1);
    crawler = CrawlerImpl.makeRepairCrawler(mau, spec, aus, repairUrls);

    crawler.doCrawl(Deadline.MAX);

    Set cachedUrls = cus.getForceCachedUrls();
    assertSameElements(repairUrls, cachedUrls);
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


    crawler =
      CrawlerImpl.makeNewContentCrawler(mau, spec, new MockAuState());
    crawler.setParser(parser);
    crawler.doCrawl(Deadline.MAX);
    // only gets 2 urls because start url is fetched twice (manifest & parse)
    Set expected = SetUtil.set(startUrl, url1);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testCrawlListEmptyOnExit() {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1, url2, url3));
    cus.addUrl(url1);
    cus.addUrl(url2);
    cus.addUrl(url3);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);

    crawler.doCrawl(Deadline.MAX);
    assertEmpty(aus.getCrawlUrls());
  }

  public void testCrawlListPreservesUncrawledUrls() {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1, url2, url3));
    cus.addUrl(url1);
    cus.addUrl(url2);
    cus.addUrl(url3);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);

    crawler.doCrawl(Deadline.in(2));
    //Set expected = SetUtil.set(startUrl, url1);
    Collection expected = SetUtil.set(url2, url3);
    assertSameElements(expected, aus.getCrawlUrls());
  }

  public void testUpdatedCrawlListCalledForEachFetch() {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1, url2, url3));
    cus.addUrl(url1);
    cus.addUrl(url2);
    cus.addUrl(url3);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);

    crawler.doCrawl(Deadline.MAX);
    //Set expected = SetUtil.set(startUrl, url1);
    aus.assertUpdatedCrawlListCalled(3); //not called for startUrl
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

  private class MyMockCachedUrlSet extends MockCachedUrlSet {
    public MyMockCachedUrlSet(MockArchivalUnit owner, CachedUrlSetSpec spec) {
      super(owner, spec);
    }
    protected MockUrlCacher makeMockUrlCacher(String url,
					      MockCachedUrlSet parent) {
      return new MockUrlCacherThatStepsTimebase(url, parent);
    }

  }

    private class MockUrlCacherThatStepsTimebase extends MockUrlCacher {
      public MockUrlCacherThatStepsTimebase(String url, MockCachedUrlSet cus) {
	super(url, cus);
    }
    public void cache() throws IOException {
      TimeBase.step();
      super.cache();
    }

    public InputStream getUncachedInputStream() {
      return new StringInputStream("");
    }
  }

  public static void main(String[] argv) {
   String[] testCaseList = {TestCrawlerImpl.class.getName()};
   junit.textui.TestRunner.main(testCaseList);
  }
}
