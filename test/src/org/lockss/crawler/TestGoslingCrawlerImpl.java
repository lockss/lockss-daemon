/*
 * $Id: TestGoslingCrawlerImpl.java,v 1.1 2002-11-27 00:25:45 troberts Exp $
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

/**
 * This is the test class for org.lockss.crawler.GoslingCrawlerImpl
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */


public class TestGoslingCrawlerImpl extends LockssTestCase {
  private MockArchivalUnit mau = null;
  private CrawlSpec spec = null;

  public static Class testedClasses[] = {
    org.lockss.crawler.GoslingCrawlerImpl.class
  };

  public static Class prerequisites[] = {
    TestCrawlRule.class
  };

  public TestGoslingCrawlerImpl(String msg) {
    super(msg);
  }

  public void setUp() {
    mau = new MockArchivalUnit();
    spec = new CrawlSpec(startUrl, null);
    MockCachedUrlSet cus = new MockCachedUrlSet(mau, null);
    mau.setAUCachedUrlSet(cus);
  }

  public void testDoCrawlThrowsForNullAU() {
    try {
      GoslingCrawlerImpl.doCrawl(null, new CrawlSpec("http://www.example.com", null));
      fail("Calling doCrawl with a null ArchivalUnit should throw "+
	   "an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }
  
  public void testDoCrawlThrowsForNullCrawlSpec() {
    try {
      GoslingCrawlerImpl.doCrawl(new MockArchivalUnit(), null);
      fail("Calling doCrawl with a null ArchivalUnit should throw "+
	   "an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }
  
  public static final String EMPTY_PAGE = "";
  public static final String LINKLESS_PAGE = "Nothing here";

  public static final String startUrl = 
    "http://www.example.com/lockss-index123.html";


  public void testDoCrawlOnePageNoLinks() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(LINKLESS_PAGE, startUrl);
    GoslingCrawlerImpl.doCrawl(mau, spec);
    Set cachedUrls = cus.getCachedUrls();
    assertEquals(1, cachedUrls.size());
    assertTrue(cachedUrls.contains(startUrl));
  }

  public void testDoCrawlHref() {
    singleTagCrawl("http://www.example.com/web_link.html",
		   "<a href=", "</a>");
  }

  public void testDoCrawlImage() {
    singleTagCrawl("http://www.example.com/web_link.jpg",
		   "<img src=", "</img>");
  }

  public void testDoCrawlFrame() {
    singleTagCrawl("http://www.example.com/web_link.html",
		   "<frame src=", "</frame>");
  }

  public void testDoCrawlLink() {
    singleTagCrawl("http://www.example.com/web_link.css",
		   "<link href=", "</link>");
  }

  public void testDoCrawlBody() {
    singleTagCrawl("http://www.example.com/web_link.jpg",
		   "<body background=", "</body>");
  }

  public void testDoCrawlTable() {
    singleTagCrawl("http://www.example.com/web_link.jpg",
		   "<table background=", "</table>");
  }

  public void testDoCrawlTc() {
    singleTagCrawl("http://www.example.com/web_link.jpg",
		   "<tc background=", "</tc>");
  }

  private void singleTagCrawl(String url, String openTag, String closeTag) {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();

    cus.addUrl(makeContent(url, openTag, closeTag), startUrl);
    cus.addUrl(LINKLESS_PAGE, url);
    GoslingCrawlerImpl.doCrawl(mau, spec);
    Set cachedUrls = cus.getCachedUrls();
    Set expected = SetUtil.set(url, startUrl);
    assertEquals(expected, cachedUrls);
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
    
    GoslingCrawlerImpl.doCrawl(mau, spec);
    Set expected = SetUtil.set(startUrl, url);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testMultipleLinks() {
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
    
    GoslingCrawlerImpl.doCrawl(mau, spec);
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
      "<a blah1=blah href=link2.html blah2=blah>link2#ref</a>"+
      "<a href=dir/link3.html>link3</a>";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url1);
    cus.addUrl(LINKLESS_PAGE, url2);
    cus.addUrl(LINKLESS_PAGE, url3);
    
    GoslingCrawlerImpl.doCrawl(mau, spec);
    Set expected = SetUtil.set(startUrl, url1, url2, url3);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testMultipleStartingUrls() {
    String[] urls = {
      "http://www.example.com/link1.html",
      "http://www.example.com/link2.html",
      "http://www.example.com/link3.html",
      startUrl
    };
    CrawlSpec spec = makeCrawlSpec(urls);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    for (int ix=0; ix<urls.length; ix++) {
      cus.addUrl(LINKLESS_PAGE, urls[ix]);
    }

    GoslingCrawlerImpl.doCrawl(mau, spec);
    Set expected = SetUtil.fromArray(urls);
    assertEquals(expected, cus.getCachedUrls());
  }

  private CrawlSpec makeCrawlSpec(String[] urls) {
    List list = new LinkedList();
    for (int ix=0; ix<urls.length; ix++) {
      list.add(urls[ix]);
    }
    return new CrawlSpec(list, null);
  }
}
