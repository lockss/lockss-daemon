/*
 * $Id: TestGoslingCrawlerImpl.java,v 1.34 2004-01-09 01:16:32 troberts Exp $
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
 * This is the test class for org.lockss.crawler.GoslingCrawlerImpl
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */
public class TestGoslingCrawlerImpl extends LockssTestCase {
  private MockArchivalUnit mau = null;
  private List startUrls = null;

  private GoslingCrawlerImpl crawler = null;
  private CrawlSpec spec = null;

  public static final String EMPTY_PAGE = "";
  public static final String LINKLESS_PAGE = "Nothing here";

  public static final String startUrl = "http://www.example.com/index.html";
  private MockCrawlRule crawlRule;
  private MockAuState aus = new MockAuState();

  private static final String PARAM_RETRY_TIMES =
    Configuration.PREFIX + "GoslingCrawlerImpl.numCacheRetries";
  private static final int DEFAULT_RETRY_TIMES = 3;

  public static Class testedClasses[] = {
    org.lockss.crawler.GoslingCrawlerImpl.class
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
      GoslingCrawlerImpl.makeNewContentCrawler(mau, spec, aus);

    Properties p = new Properties();
    p.setProperty(GoslingCrawlerImpl.PARAM_RETRY_PAUSE, "0");
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }

  //Tests for makeNewContentCrawler (mncc)
  public void testMnccThrowsForNullAu() {
    try {
      crawler =
	GoslingCrawlerImpl.makeNewContentCrawler(null, spec,
						 new MockAuState());
      fail("Calling makeNewContentCrawler with a null ArchivalUnit"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testMnccThrowsForNullCrawlSpec() {
    try {
      crawler =
	GoslingCrawlerImpl.makeNewContentCrawler(mau, null, new MockAuState());
      fail("Calling makeNewContentCrawler with a null CrawlSpec"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testMnccThrowsForNullAuState() {
    try {
      crawler =
	GoslingCrawlerImpl.makeNewContentCrawler(mau, spec, null);
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
  public void testDoCrawlOnePageNoCache() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String startUrl2 = "http://www.example2.com/index.html";
    cus.addUrl(LINKLESS_PAGE, startUrl2);
    crawler.doCrawl(Deadline.MAX);
    Set cachedUrls = cus.getCachedUrls();
    // assert doesn't cache crawl permission page
    assertEquals(0, cachedUrls.size());
  }

  public void testDoCrawlOnePageNoLinks() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(LINKLESS_PAGE, startUrl);
    crawler.doCrawl(Deadline.MAX);
    Set cachedUrls = cus.getCachedUrls();
    // assert doesn't cache crawl permission page
    assertEquals(1, cachedUrls.size());
    assertTrue(cachedUrls.contains(startUrl));
  }


  public void testDoCrawlHref() {
    singleTagShouldCrawl("http://www.example.com/web_link.html",
			 "<a href=", "</a>");
  }

  public void testDoCrawlImage() {
    singleTagShouldCrawl("http://www.example.com/web_link.jpg",
			 "<img src=", "</img>");
  }

  public void testDoCrawlImageWithSrcInAltTag() {
    singleTagShouldCrawl("http://www.example.com/web_link.jpg",
			 "<img alt=src src=", "</img>");
    singleTagShouldCrawl("http://www.example.com/web_link.jpg",
			 "<img alt = src src=", "</img>");
  }

  public void testDoCrawlImageWithSrcInAltTagAfterSrcProper() {
    String url= "http://www.example.com/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<img src="+url+" alt=src>link3</a>";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url);
    crawlRule.addUrlToCrawl(url);

    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.set(startUrl, url);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testDoCrawlFrame() {
    singleTagShouldCrawl("http://www.example.com/web_link.html",
		   "<frame src=", "</frame>");
  }

  public void testDoCrawlLink() {
    singleTagShouldCrawl("http://www.example.com/web_link.css",
		   "<link href=", "</link>");
  }

  public void testDoCrawlBody() {
    singleTagShouldCrawl("http://www.example.com/web_link.jpg",
		   "<body background=", "</body>");
  }

  public void testDoCrawlTable() {
    singleTagShouldCrawl("http://www.example.com/web_link.jpg",
		   "<table background=", "</table>");
  }

  public void testDoCrawlTc() {
    singleTagShouldCrawl("http://www.example.com/web_link.jpg",
		   "<tc background=", "</tc>");
  }

  public void testDoCrawlWithEqualsInUrl() {
    singleTagShouldCrawl(
        "http://www.example.com/acs/a/toc.select?in_coden=jcisd8&in_volume=43",
        "<a href=", "</a>");
  }

  public void testDoCrawlWithLineBreakBeforeTag() {
    singleTagShouldCrawl("http://www.example.com/web_link.html",
                         "<a\nhref=", "</a");
  }

  public void testDoNotCrawlWithHttpsLink() {
    singleTagShouldNotCrawl("https://www.example.com/web_link.html", "<a\nhref=", "</a");
  }

  public void testDoCrawlWithAmpInUrl() {
    singleTagShouldCrawl("http://www.example.com?pageid=pid&amp;parentid=parid&amp",
                         "<a href=", "</a");
  }
  public void testDoNotCrawlBadA() {
    String[] badTags = {
      "<a harf=",
      "<a hre=",
      "<a hrefe=",
      "<al href="
    };
    checkBadTags(badTags, "</a>");
  }

  private void checkBadTags(String[] badTags, String closeTag) {
    String url = "http://www.example.com/web_link.html";
    for (int ix=0; ix<badTags.length; ix++) {
      singleTagShouldNotCrawl(url, badTags[ix], closeTag);
    }
  }

  public void testDoNotCrawlBadFrameTag() {
    String[] badTags = {
      "<fram src=",
      "<framea src=",
      "<framr src=",
      "<frame sr=",
      "<frame srcr=",
      "<frame sra="
    };
    checkBadTags(badTags, "</frame>");
  }

  public void testDoNotCrawlBadImgTag() {
    String[] badTags = {
      "<im src=",
      "<imga src=",
      "<ime src=",
      "<img sr=",
      "<img srcr=",
      "<img sra="
    };
    checkBadTags(badTags, "</frame>");
  }

  public void testDoNotCrawlBadLinkTag() {
    String[] badTags = {
      "<lin href=",
      "<linkf href=",
      "<lino href=",
      "<link hre=",
      "<link hrefr=",
      "<link hrep="
    };
    checkBadTags(badTags, "</link>");
  }

  public void testDoNotCrawlBadBodyTag() {
    String[] badTags = {
      "<bod background=",
      "<bodyk background=",
      "<bodp background=",
      "<body backgroun=",
      "<body backgrounyl=",
      "<body backgrounj="
    };
    checkBadTags(badTags, "</body>");
  }

  public void testDoNotCrawlBadScriptTag() {
    String[] badTags = {
      "<scrip src=",
      "<scriptl src=",
      "<scripo src=",
      "<script sr=",
      "<script srcu=",
      "<script srp="
    };
    checkBadTags(badTags, "</script>");
  }

  public void testDoNotCrawlBadTableTag() {
    String[] badTags = {
      "<tabl background=",
      "<tablea background=",
      "<tablu background=",
      "<table backgroun=",
      "<table backgroundl=",
      "<table backgrouno="
    };
    checkBadTags(badTags, "</table>");
  }

  public void testDoNotCrawlBadTcTag() {
    String[] badTags = {
      "<t background=",
      "<tcl background=",
      "<ta background=",
      "<tc backgroun=",
      "<tc backgroundl=",
      "<tc backgrouno="
    };
    checkBadTags(badTags, "</table>");
  }

  private void singleTagShouldCrawl(String url,
				    String openTag,
				    String closeTag) {
    singleTagCrawl(url, openTag, closeTag, true);
  }

  private void singleTagShouldNotCrawl(String url,
				       String openTag,
				       String closeTag) {
    singleTagCrawl(url, openTag, closeTag, false);
  }

  private void singleTagCrawl(String url,
			      String openTag,
			      String closeTag,
			      boolean shouldCache) {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();

    String content = makeContent(url, openTag, closeTag);
    cus.addUrl(content, startUrl);
    cus.addUrl(LINKLESS_PAGE, url);
    crawlRule.addUrlToCrawl(url);
    crawler.doCrawl(Deadline.MAX);
    Set cachedUrls = cus.getCachedUrls();
    if (shouldCache) {
      Set expected = SetUtil.set(url, startUrl);
      assertEquals("Miscrawled: "+content, expected, cachedUrls);
    } else {
      Set expected = SetUtil.set(startUrl);
      assertEquals("Miscrawled: "+content, expected, cachedUrls);
    }
  }

  private String makeContent(String url, String openTag, String closeTag) {
    StringBuffer sb = new StringBuffer(100);
    sb.append("<html><head><title>Test</title></head><body>");
    sb.append(openTag);
    sb.append(url);
    sb.append(">");
    sb.append(closeTag);
    sb.append("</body></html>");
    return sb.toString();
  }

  public void testDoesNotCacheExistingFile() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    cus.addUrl("<a href="+url1+">test</a>", startUrl, true, true);
    cus.addUrl(LINKLESS_PAGE, url1, true, true);

    crawler.doCrawl(Deadline.MAX);

    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testDoesNotCacheFileWhichShouldNotBeCached() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(LINKLESS_PAGE, startUrl, false, false);

    crawler.doCrawl(Deadline.MAX);
    assertEquals(0, cus.getCachedUrls().size());
  }

  public void testParsesFileWithCharsetAfterContentType() {
    String url = "http://www.example.com/link1.html";
    String content = makeContent(url, "<a href=", "</a>");
    Properties props = new Properties();
    props.setProperty("content-type", "text/html; charset=US-ASCII");


    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(content, startUrl, false, true, props);
    cus.addUrl(LINKLESS_PAGE, url);
    crawlRule.addUrlToCrawl(url);

    crawler.doCrawl(Deadline.MAX);

    Set expected = SetUtil.set(startUrl, url);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testParsesFileWithCapitilizedContentType() {
    String url = "http://www.example.com/link1.html";
    String content = makeContent(url, "<a href=", "</a>");
    Properties props = new Properties();
    props.setProperty("content-type", "TEXT/HTML");


    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(content, startUrl, false, true, props);
    cus.addUrl(LINKLESS_PAGE, url);
    crawlRule.addUrlToCrawl(url);

    crawler.doCrawl(Deadline.MAX);

    Set expected = SetUtil.set(startUrl, url);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testDoesNotParseBadContentType() {
    String url = "http://www.example.com/link1.html";
    String content = makeContent(url, "<a href=", "</a>");

    Properties props = new Properties();
    props.setProperty("content-type", "text/xml");


    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(content, startUrl, false, true, props);
    cus.addUrl(LINKLESS_PAGE, url);

    crawler.doCrawl(Deadline.MAX);

    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cus.getCachedUrls());
  }


  public void testParsesFileWithQuotedUrls() {
    String url= "http://www.example.com/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=\"http://www.example.com/link3.html\">link3</a>";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url);
    crawlRule.addUrlToCrawl(url);

    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.set(startUrl, url);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testSkipsComments() {
    String url= "http://www.example.com/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<!--<a href=http://www.example.com/link1.html>link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a href=http://www.example.com/link2.html>link2</a>-->"+
      "<a href=http://www.example.com/link3.html>link3</a>";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url);
    crawlRule.addUrlToCrawl(url);

    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.set(startUrl, url);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testMultipleLinks() {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href="+url1+">link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a href="+url2+">link2</a>"+
      "<a href="+url3+">link3</a>";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url1);
    cus.addUrl(LINKLESS_PAGE, url2);
    cus.addUrl(LINKLESS_PAGE, url3);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);

    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.set(startUrl, url1, url2, url3);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testRelativeLinksLocationTagsAndMultipleKeys() {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/dir/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=link1.html>link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a blah1=blah href=link2.html#ref blah2=blah>link2</a>"+
      "<a href=dir/link3.html>link3</a>";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url1);
    cus.addUrl(LINKLESS_PAGE, url2);
    cus.addUrl(LINKLESS_PAGE, url3);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);

    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.set(startUrl, url1, url2, url3);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testRelativeLinksWithSameName() {
    String url1= "http://www.example.com/branch1/index.html";
    String url2= "http://www.example.com/branch2/index.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=branch1/index.html>link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a href=branch2/index.html>link2</a>";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url1);
    cus.addUrl(LINKLESS_PAGE, url2);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);

    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.set(startUrl, url1, url2);
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
      cus.addUrl(LINKLESS_PAGE, curUrl);
      crawlRule.addUrlToCrawl(curUrl);
    }

    spec = new CrawlSpec(urls, crawlRule);
    crawler =
      GoslingCrawlerImpl.makeNewContentCrawler(mau, spec, new MockAuState());
    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.fromList(urls);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testOverwritesStartingUrlsOneLevel() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(LINKLESS_PAGE, startUrl, true, true);

    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testOverwritesStartingUrlsMultipleLevels() {
    spec = new CrawlSpec(startUrls, crawlRule, 2);
    crawler =
      GoslingCrawlerImpl.makeNewContentCrawler(mau, spec, new MockAuState());

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/dir/link3.html";
    String url4= "http://www.example.com/dir/link9.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href="+url1+">link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a blah1=blah href="+url2+">link2</a>"+
      "<a href="+url3+">link3</a>";

    String source2 =
      "<html><head><title>Test</title></head><body>"+
      "<a href="+url4+">link1</a>";

    cus.addUrl(source, startUrl, true, true);
    cus.addUrl(LINKLESS_PAGE, url1, true, true);
    cus.addUrl(source2, url2, true, true);
    cus.addUrl(LINKLESS_PAGE, url3, true, true);
    cus.addUrl(LINKLESS_PAGE, url4, true, true);
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
    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href="+url1+">link3</a>";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(source, startUrl, true, true);
    cus.addUrl("<a href="+url2+">link4</a>", url1, true, true);
    cus.addUrl(LINKLESS_PAGE, url2);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);

    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testWillNotCrawlExpiredDeadline() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(LINKLESS_PAGE, startUrl, false, true);

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
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url1);
    cus.addUrl(LINKLESS_PAGE, url2);
    cus.addUrl(LINKLESS_PAGE, url3);

    crawler.doCrawl(Deadline.in(2));
    Set expected = SetUtil.set(startUrl, url1);
//    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testDoesNotLoopOnSelfReferentialPage() {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href="+url1+">link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a href="+url2+">link2</a>"+
      "<a href="+url3+">link3</a>"+
      "<a href="+startUrl+">start page</a>";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url1);
    cus.addUrl(LINKLESS_PAGE, url2);
    cus.addUrl(LINKLESS_PAGE, url3);
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
    cus.addUrl(source1, startUrl);
    cus.addUrl(source2, url1);
    cus.addUrl(LINKLESS_PAGE, url2);
    cus.addUrl(LINKLESS_PAGE, url3);
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
    cus.addUrl("<a href="+url1+">test</a>", startUrl, false, true);
    cus.addUrl(LINKLESS_PAGE, url1, false, true);

    assertTrue(crawler.doCrawl(Deadline.MAX));
   }

  public void testReturnsFalseWhenExceptionThrown() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    cus.addUrl("<a href="+url1+">test</a>", startUrl, false, true);
    cus.addUrl(url1, new IOException("Test exception"), DEFAULT_RETRY_TIMES);
    crawlRule.addUrlToCrawl(url1);

    assertFalse(crawler.doCrawl(Deadline.MAX));
  }

  public void testReturnsRetriesWhenExceptionThrown() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    cus.addUrl("<a href="+url1+">test</a>", startUrl, false, true);
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
    p.setProperty(GoslingCrawlerImpl.PARAM_RETRY_PAUSE, "0");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    cus.addUrl("<a href="+url1+">test</a>", startUrl, false, true);
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
    p.setProperty(GoslingCrawlerImpl.PARAM_RETRY_PAUSE, "0");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    MyMockCachedUrlSet cus = (MyMockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    String url2="http://www.example.com/blah2.html";
    String url3="http://www.example.com/blah3.html";
    cus.addUrl("<a href="+url1+">test</a><a href="+url2
	       +">test</a><a href="+url3+">test</a>", startUrl, false, true);
    cus.addUrl("<a href="+url1+">test</a>", url2, false, true);
    cus.addUrl("<a href="+url1+">test</a>", url3, false, true);
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
    cus.addUrl("<a href="+url1+">test</a>", startUrl, false, true);
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

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href="+url1+">link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a href="+url2+">link2</a>"+
      "<a href="+url3+">link3</a>";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url1);
    cus.addUrl(LINKLESS_PAGE, url2);
    cus.addUrl(LINKLESS_PAGE, url3);
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
    singleTagShouldCrawl("http://www.example.com/web_link.html",
			 "<a href=", "</a>");
    assertEquals(Crawler.STATUS_SUCCESSFUL,
		 crawler.getStatus().getCrawlStatus());
  }

  public void testGetStatusError() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    cus.addUrl("<a href="+url1+">test</a>", startUrl, false, true);
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
	GoslingCrawlerImpl.makeRepairCrawler(null, spec, aus, testUrlList);
      fail("Calling makeRepairCrawler with a null ArchivalUnit"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testMrcThrowsForNullSpec() {
    try {
      crawler =
	GoslingCrawlerImpl.makeRepairCrawler(mau, null, aus, testUrlList);
      fail("Calling makeRepairCrawler with a null CrawlSpec"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testMrcThrowsForNullList() {
    try {
      crawler =
	GoslingCrawlerImpl.makeRepairCrawler(mau, spec, aus, null);
      fail("Calling makeRepairCrawler with a null repair list"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testMrcThrowsForEmptyList() {
    try {
      crawler =
	GoslingCrawlerImpl.makeRepairCrawler(mau, spec, aus, ListUtil.list());
      fail("Calling makeRepairCrawler with a empty repair list"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testRepairCrawlCallsForceCache() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String repairUrl = "http://example.com/forcecache.html";
    cus.addUrl(LINKLESS_PAGE, repairUrl);
    crawlRule.addUrlToCrawl(repairUrl);

    List repairUrls = ListUtil.list(repairUrl);
    spec = new CrawlSpec(startUrls, crawlRule, 1);
    crawler = GoslingCrawlerImpl.makeRepairCrawler(mau, spec, aus, repairUrls);

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

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href="+url1+">link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a href="+url2+">link2</a>"+
      "<a href="+repairUrl2+">link3</a>";

    cus.addUrl(source, repairUrl1);
    cus.addUrl(LINKLESS_PAGE, repairUrl2);
    cus.addUrl(LINKLESS_PAGE, url1);
    cus.addUrl(LINKLESS_PAGE, url2);
    crawlRule.addUrlToCrawl(repairUrl1);
    crawlRule.addUrlToCrawl(repairUrl2);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);

    List repairUrls = ListUtil.list(repairUrl1, repairUrl2);
    spec = new CrawlSpec(startUrls, crawlRule, 1);
    crawler = GoslingCrawlerImpl.makeRepairCrawler(mau, spec, aus, repairUrls);

    crawler.doCrawl(Deadline.MAX);

    Set cachedUrls = cus.getForceCachedUrls();
    assertSameElements(repairUrls, cachedUrls);
  }

  public void testCrawlWindow() {
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html";
    String url3 = "http://www.example.com/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href="+url1+">link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a href="+url2+">link2</a>"+
      "<a href="+url3+">link3</a>";

    CrawlSpec spec = new CrawlSpec(startUrl, crawlRule);
    spec.setCrawlWindow(new MockCrawlWindowThatCountsDown(3));
    mau.setCrawlSpec(spec);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url1);
    cus.addUrl(LINKLESS_PAGE, url2);
    cus.addUrl(LINKLESS_PAGE, url3);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);


    crawler =
      GoslingCrawlerImpl.makeNewContentCrawler(mau, spec, new MockAuState());
    crawler.doCrawl(Deadline.MAX);
    // only gets 2 urls because start url is fetched twice (manifest & parse)
    Set expected = SetUtil.set(startUrl, url1);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testCrawlListEmptyOnExit() {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href="+url1+">link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a href="+url2+">link2</a>"+
      "<a href="+url3+">link3</a>";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url1);
    cus.addUrl(LINKLESS_PAGE, url2);
    cus.addUrl(LINKLESS_PAGE, url3);
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

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=http://www.example.com/link1.html>link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a href=http://www.example.com/link2.html>link2</a>"+
      "<a href=http://www.example.com/link3.html>link3</a>";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url1);
    cus.addUrl(LINKLESS_PAGE, url2);
    cus.addUrl(LINKLESS_PAGE, url3);
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

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=http://www.example.com/link1.html>link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a href=http://www.example.com/link2.html>link2</a>"+
      "<a href=http://www.example.com/link3.html>link3</a>";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url1);
    cus.addUrl(LINKLESS_PAGE, url2);
    cus.addUrl(LINKLESS_PAGE, url3);
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
   String[] testCaseList = {TestGoslingCrawlerImpl.class.getName()};
   junit.textui.TestRunner.main(testCaseList);
  }
}
