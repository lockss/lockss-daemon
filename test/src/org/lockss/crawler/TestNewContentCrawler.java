/*
 * $Id: TestNewContentCrawler.java,v 1.5.2.1 2004-03-26 23:26:34 troberts Exp $
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
    crawler = new NewContentCrawler(mau, spec, aus);

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

  //Will try to fetch startUrl, content parser will return no urls,
  //so we should only cache the start url
  public void testDoCrawlOnePageNoLinks() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl);
    crawler.doCrawl();
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

    crawler.doCrawl();

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
    crawler = new NewContentCrawler(mau, spec, new MockAuState());

    mau.setParser(parser);

    crawler.doCrawl();
    Set expected = SetUtil.fromList(urls);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testOverwritesStartingUrlsOneLevel() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl, true, true);

    crawler.doCrawl();
    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cus.getCachedUrls());
  }


  public void testOverwritesStartingUrlsMultipleLevels() {
    spec = new CrawlSpec(startUrls, crawlRule, 2);
    crawler = new NewContentCrawler(mau, spec, new MockAuState());

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

    crawler.doCrawl();
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

    crawler.doCrawl();
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

    crawler.doCrawl();
    Set expected = SetUtil.set(startUrl, url2);
    assertEquals(expected, cus.getCachedUrls());
  }

  private Set crawlUrls(Set urls) {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(startUrl);
    cus.addUrl(startUrl);
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

    spec.setCrawlWindow(new MyMockCrawlWindow(1));
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

  private static void setProperty(String prop, String value) {
    Properties p = new Properties();
    p.setProperty(prop, value);
    ConfigurationUtil.setCurrentConfigFromProps(p);
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
    public MyMockCachedUrlSet(MockArchivalUnit owner, CachedUrlSetSpec spec) {
      super(owner, spec);
    }

    protected MockUrlCacher makeMockUrlCacher(String url,
 					      MockCachedUrlSet parent) {
      return new MyMockUrlCacher(url, parent);
    }

  }

  private class MyMockUrlCacher extends MockUrlCacher {
    public MyMockUrlCacher(String url, MockCachedUrlSet cus) {
      super(url, cus);
    }
    public InputStream getUncachedInputStream() {
      return new StringInputStream("");
    }
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestNewContentCrawler.class.getName()};
    junit.textui.TestRunner.main(testCaseList);
  }

}
