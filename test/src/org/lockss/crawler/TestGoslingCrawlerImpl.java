/*
 * $Id: TestGoslingCrawlerImpl.java,v 1.11 2003-04-02 23:28:37 tal Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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
import java.net.*;
import junit.framework.TestCase;
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
  private List urlList = null;
  private GoslingCrawlerImpl crawler = null;

  public static final String EMPTY_PAGE = "";
  public static final String LINKLESS_PAGE = "Nothing here";

  public static final String startUrl = 
    "http://www.example.com/index.html";


  public static Class testedClasses[] = {
    org.lockss.crawler.GoslingCrawlerImpl.class
  };

  public static Class prerequisites[] = {
    TestCrawlRule.class
  };

  public TestGoslingCrawlerImpl(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated(10);

    mau = new MockArchivalUnit();
    mau.setPauseCallback(new expireDeadlineCallback());

    urlList = ListUtil.list(startUrl);
    MockCachedUrlSet cus = new MockCachedUrlSet(mau, null);
    mau.setAUCachedUrlSet(cus);
    crawler = new GoslingCrawlerImpl(mau, urlList, true);
  }

  public void testThrowsForNullAU() {
    try {
      crawler = 
	new GoslingCrawlerImpl(null, ListUtil.list("http://www.example.com"),
			       false);
      fail("Trying to construct a GoslingCrawlerImpl with a null ArchivalUnit"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }
  
  public void testThrowsForNullStartUrlList() {
    try {
      crawler = new GoslingCrawlerImpl(new MockArchivalUnit(), null, false);

      fail("Trying to construct a GoslingCrawlerImpl with a null list of "
	   +"starting urls should throw an IllegalArgumentException");
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
  
  public void testDoCrawlOnePageNoLinks() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(LINKLESS_PAGE, startUrl);
    crawler.doCrawl(Deadline.MAX);
    Set cachedUrls = cus.getCachedUrls();
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

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url);
    
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
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();

    String content = makeContent(url, openTag, closeTag);
    cus.addUrl(content, startUrl);
    cus.addUrl(LINKLESS_PAGE, url);
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
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    cus.addUrl("<a href="+url1+">test</a>", startUrl, true, true);
    cus.addUrl(LINKLESS_PAGE, url1, true, true);

    crawler.doCrawl(Deadline.MAX);

    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cus.getCachedUrls());
   }

  public void testDoesNotCacheFileWhichShouldNotBeCached() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(LINKLESS_PAGE, startUrl, false, false);

    crawler.doCrawl(Deadline.MAX);
    assertEquals(0, cus.getCachedUrls().size());
  }


  public void testParsesFileWithCharsetAfterContentType() {
    String url = "http://www.example.com/link1.html";
    String content = makeContent(url, "<a href=", "</a>");
    Properties props = new Properties();
    props.setProperty("content-type", "text/html; charset=US-ASCII");


    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(content, startUrl, false, true, props);
    cus.addUrl(LINKLESS_PAGE, url);

    crawler.doCrawl(Deadline.MAX);

    Set expected = SetUtil.set(startUrl, url);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testParsesFileWithCapitilizedContentType() {
    String url = "http://www.example.com/link1.html";
    String content = makeContent(url, "<a href=", "</a>");
    Properties props = new Properties();
    props.setProperty("content-type", "TEXT/HTML");


    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(content, startUrl, false, true, props);
    cus.addUrl(LINKLESS_PAGE, url);

    crawler.doCrawl(Deadline.MAX);

    Set expected = SetUtil.set(startUrl, url);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testDoesNotParseBadContentType() {
    String url = "http://www.example.com/link1.html";
    String content = makeContent(url, "<a href=", "</a>");

    Properties props = new Properties();
    props.setProperty("content-type", "text/xml");


    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
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

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url);
    
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

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url);
    
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

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url1);
    cus.addUrl(LINKLESS_PAGE, url2);
    cus.addUrl(LINKLESS_PAGE, url3);
    
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

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url1);
    cus.addUrl(LINKLESS_PAGE, url2);
    cus.addUrl(LINKLESS_PAGE, url3);
    
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

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url1);
    cus.addUrl(LINKLESS_PAGE, url2);
    
    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.set(startUrl, url1, url2);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testMultipleStartingUrls() {
    List urls = ListUtil.list("http://www.example.com/link1.html",
			      "http://www.example.com/link2.html",
			      "http://www.example.com/link3.html",
			      startUrl);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    for (int ix=0; ix<urls.size(); ix++) {
      cus.addUrl(LINKLESS_PAGE, (String)urls.get(ix));
    }

    crawler = new GoslingCrawlerImpl(mau, urls, true);
    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.fromList(urls);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testOverwritesStartingUrls() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(LINKLESS_PAGE, startUrl, true, true);

    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testDoesNotFollowLinksWhenToldNotTo() {
    String url= "http://www.example.com/link3.html";

    String source = 
      "<html><head><title>Test</title></head><body>"+
      "<a href="+url+">link3</a>";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url);
    
    crawler = new GoslingCrawlerImpl(mau, urlList, false);
    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testDoesNotFollowLinksWhenToldNotToMultipleLinks() {
    String excludeUrl = "http://www.example.com/link3.html";
    String includeUrl = "http://www.example.com/link2.html";
    urlList = ListUtil.list(startUrl, includeUrl);
    String source = 
      "<html><head><title>Test</title></head><body>"+
      "<a href="+excludeUrl+">link3</a>";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, excludeUrl);
    cus.addUrl(LINKLESS_PAGE, includeUrl);
    
    crawler = new GoslingCrawlerImpl(mau, urlList, false);
    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.set(startUrl, includeUrl);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testWillParseExistingPagesForUrls() {
    String url1 = "http://www.example.com/link3.html";
    String url2 = "http://www.example.com/link4.html";
    urlList = ListUtil.list(startUrl);
    String source = 
      "<html><head><title>Test</title></head><body>"+
      "<a href="+url1+">link3</a>";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(source, startUrl, true, true);
    cus.addUrl("<a href="+url2+">link4</a>", url1, true, true);
    cus.addUrl(LINKLESS_PAGE, url2);
    
    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.set(url2, startUrl);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testWillNotCrawlExpiredDeadline() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
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

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url1);
    cus.addUrl(LINKLESS_PAGE, url2);
    cus.addUrl(LINKLESS_PAGE, url3);
    
    crawler.doCrawl(Deadline.in(2));
    Set expected = SetUtil.set(startUrl, url1);
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

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url1);
    cus.addUrl(LINKLESS_PAGE, url2);
    cus.addUrl(LINKLESS_PAGE, url3);
    
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


    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(source1, startUrl);
    cus.addUrl(source2, url1);
    cus.addUrl(LINKLESS_PAGE, url2);
    cus.addUrl(LINKLESS_PAGE, url3);
    
    crawler.doCrawl(Deadline.MAX);
    Set expected = SetUtil.set(startUrl, url1, url2, url3);
    assertEquals(expected, cus.getCachedUrls());
  }
  
  public void testGetStatusCrawlNotStarted() {
    assertEquals(-1, crawler.getStartTime());
    assertEquals(-1, crawler.getEndTime());
    assertEquals(0, crawler.getNumFetched());
    assertEquals(0, crawler.getNumParsed());
  }

  public void testGetStatusCrawlDone() {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    String source = 
      "<html><head><title>Test</title></head><body>"+
      "<a href="+url1+">link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a href="+url2+">link2</a>"+
      "<a href="+url3+">link3</a>";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url1);
    cus.addUrl(LINKLESS_PAGE, url2);
    cus.addUrl(LINKLESS_PAGE, url3);
    
    long expectedStart = TimeBase.nowMs();
    crawler.doCrawl(Deadline.MAX);
    long expectedEnd = TimeBase.nowMs();
    assertEquals(expectedStart, crawler.getStartTime());
    assertEquals(expectedEnd, crawler.getEndTime());
    assertEquals(4, crawler.getNumFetched());
    assertEquals(4, crawler.getNumParsed());
  }

  private class expireDeadlineCallback implements MockObjectCallback {
    public expireDeadlineCallback() {
    }
   
    public void callback() {
      TimeBase.step();
    }
  }
}
