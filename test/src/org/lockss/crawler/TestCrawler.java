package org.lockss.crawler;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.Properties;
import java.net.URL;
import java.net.MalformedURLException;
import junit.framework.TestCase;
import org.lockss.daemon.CachedUrlSet;
import org.lockss.daemon.CachedUrl;
import org.lockss.test.MockCachedUrlSet;
import org.lockss.test.MockCachedUrl;
import org.lockss.test.StringInputStream;

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

/**
 * This is the test class for org.lockss.crawler.Crawler
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */


public class TestCrawler extends TestCase{
  public TestCrawler(String msg){
    super(msg);
  }

  public void testCreateInitialListNullCachedUrlSet(){
    assertEquals(0, (Crawler.createInitialList(null)).size());
  }

  public void testCreateInitialListEmptyCachedUrlSet(){
    String[] urls = {};
    CachedUrlSet cus = 
      MockCachedUrlSet.createFromListOfRootUrls(urls);
    assertEquals(0, (Crawler.createInitialList(cus)).size());
  }

  public void testCreateInitialListOneUrlInCachedUrlSet(){
    String[] urls = {"http://test.org/"};
    CachedUrlSet cus = 
      MockCachedUrlSet.createFromListOfRootUrls(urls);
    checkUrlsMatchCachedUrlSet(urls, cus);
  }
  
  public void testCreateInitialListOneMultipleInCachedUrlSet(){
    String[] urls = {"http://test.org/", 
		     "http://test2.org/", "http://test3.org/"};
    CachedUrlSet cus = 
      MockCachedUrlSet.createFromListOfRootUrls(urls);
    checkUrlsMatchCachedUrlSet(urls, cus);
  }
  
  private void checkUrlsMatchCachedUrlSet(String[] urls, CachedUrlSet cus){
    List initList = Crawler.createInitialList(cus);
    assertEquals(urls.length, initList.size());
    for (int ix=0; ix<urls.length; ix++){
      assertTrue("initList didn't contain "+urls[ix],
		 initList.contains(urls[ix]));
    }
  }

  public void testAddUrlsToListNullInputStream()
  throws IOException{
    Vector list = new Vector();
    CachedUrl cu = new MockCachedUrl(null);
    Crawler.addUrlsToList(cu, list);
    assertEquals(0, list.size());
  }

  public void testAddUrlsToListOneHrefInputStream()
  throws IOException{
    Vector list = new Vector();
    String source = "<html><head>"+
      "<title>Test</title></head>"+
      "<body><a href=\"http://www.test.org/\"></body></html>";
    StringInputStream strStream = new StringInputStream(source);
    MockCachedUrl cu = new MockCachedUrl("http://www.test.org/index.html");
    cu.setCachedInputStream(strStream);

    Properties prop = new Properties();
    prop.setProperty("content-type", "text/html");
    cu.setCachedProperties(prop);

    Crawler.addUrlsToList(cu, list);
    assertTrue("List didn't contain http://www.test.org/",
	       list.contains("http://www.test.org/"));
    assertEquals(1, list.size());
  }

  public void testExtractLinkHandlesNoLink() 
      throws IOException, MalformedURLException{
    StringReader strReader = 
      new StringReader("blah blah blah");
    assertNull(Crawler.ExtractNextLink(strReader, null));
  }

  public void testExtractLinkGetsOneLink()
      throws IOException, MalformedURLException{
    StringReader strReader = 
      new StringReader("This is a <a href=http://www.test.org>test</a> of the parser");
    assertEquals("http://www.test.org", Crawler.ExtractNextLink(strReader, null));
  }

  public void testExtractLinkGetsMultipleLink()
      throws IOException, MalformedURLException{
    String testStr = 
      "<body background=backg1.gif>"+
      "This is a <a href=http://www.test.org>test</a> of the parser"+
      "It needs to have multiple diferent types of links, like this"+
      "<img src=picture.gif> and this <table background=backg.jpg></table></body>";
    URL srcUrl = new URL("http://www.test.org");
    StringReader strReader = new StringReader(testStr);
    assertEquals("http://www.test.org/backg1.gif", Crawler.ExtractNextLink(strReader, srcUrl));
    assertEquals("http://www.test.org", 
		 Crawler.ExtractNextLink(strReader, srcUrl));
    assertEquals("http://www.test.org/picture.gif", 
		 Crawler.ExtractNextLink(strReader, srcUrl));
    assertEquals("http://www.test.org/backg.jpg", 
		 Crawler.ExtractNextLink(strReader, srcUrl));
    assertNull(Crawler.ExtractNextLink(strReader, srcUrl));
  }

  public void testExtractLinkGetsMixedCase()
      throws IOException, MalformedURLException{
    String testStr = 
      "This is a <a HREF=http://www.test.org>test</a> of the parser";
    URL srcUrl = new URL("http://www.test.org");
    StringReader strReader = new StringReader(testStr);
    assertEquals("http://www.test.org", 
		 Crawler.ExtractNextLink(strReader, srcUrl));
  }

  /*
   * tests to write
   * 1)skiping crap in a script
   * 2)skip comments
   */

  public void testParseLinkReturnsNullForBadStrings() throws MalformedURLException{
    StringBuffer link = new StringBuffer("/table");
    URL srcUrl = new URL("http://www.test.org");
    assertNull(Crawler.ParseLink(link, srcUrl));
  }

  public void testParseLinkParsesQuotes() throws MalformedURLException{
    StringBuffer link = new StringBuffer("a href=\"http://www.test.org/test/test2\"");
    URL srcUrl = new URL("http://www.test.org");
    assertEquals("http://www.test.org/test/test2", 
		 Crawler.ParseLink(link, srcUrl));
  }

  public void testParseLinkParsesHref() throws MalformedURLException{
    StringBuffer link = new StringBuffer("a href=http://www.test.org/test/test2");
    URL srcUrl = new URL("http://www.test.org");
    assertEquals("http://www.test.org/test/test2", 
		 Crawler.ParseLink(link, srcUrl));
  }

  public void testParseLinkParsesHrefRelativeLink() throws MalformedURLException{
    StringBuffer link = new StringBuffer("a href=test/test2");
    URL srcUrl = new URL("http://www.test.org");
    assertEquals("http://www.test.org/test/test2", 
		 Crawler.ParseLink(link, srcUrl));
  }

  public void testParseLinkParsesHrefWithHash() throws MalformedURLException{
    StringBuffer link = new StringBuffer("a href=test/test2#section1");
    URL srcUrl = new URL("http://www.test.org");
    assertEquals("http://www.test.org/test/test2", 
		 Crawler.ParseLink(link, srcUrl));
  }

  public void testParseLinkParsesImg() throws MalformedURLException{
    StringBuffer link = 
      new StringBuffer("img src=http://www.test.org/test/test2.gif");
    URL srcUrl = new URL("http://www.test.org");
    assertEquals("http://www.test.org/test/test2.gif", 
		 Crawler.ParseLink(link, srcUrl));
  }

  public void testParseLinkParsesImgRelativeLink() throws MalformedURLException{
    StringBuffer link = new StringBuffer("img src=test/test2.gif");
    URL srcUrl = new URL("http://www.test.org");
    assertEquals("http://www.test.org/test/test2.gif", 
		 Crawler.ParseLink(link, srcUrl));
  }

  public void testParseLinkParsesFrame() throws MalformedURLException{
    StringBuffer link = 
      new StringBuffer("frame src=http://www.test.org/test/test2.html");
    URL srcUrl = new URL("http://www.test.org");
    assertEquals("http://www.test.org/test/test2.html", 
		 Crawler.ParseLink(link, srcUrl));
  }

  public void testParseLinkParsesFrameRelativeLink() throws MalformedURLException{
    StringBuffer link = new StringBuffer("frame src=test/test2.html");
    URL srcUrl = new URL("http://www.test.org");
    assertEquals("http://www.test.org/test/test2.html", 
		 Crawler.ParseLink(link, srcUrl));
  }

  public void testParseLinkParsesLink() throws MalformedURLException{
    StringBuffer link = 
      new StringBuffer("link href=http://www.test.org/test/test2.css");
    URL srcUrl = new URL("http://www.test.org");
    assertEquals("http://www.test.org/test/test2.css", 
		 Crawler.ParseLink(link, srcUrl));
  }

  public void testParseLinkParsesLinkRelativeLink() throws MalformedURLException{
    StringBuffer link = new StringBuffer("link href=test/test2.css");
    URL srcUrl = new URL("http://www.test.org");
    assertEquals("http://www.test.org/test/test2.css", 
		 Crawler.ParseLink(link, srcUrl));
  }

  public void testParseLinkParsesBody() throws MalformedURLException{
    StringBuffer link = 
      new StringBuffer("body background=http://www.test.org/test/test2.gif");
    URL srcUrl = new URL("http://www.test.org");
    assertEquals("http://www.test.org/test/test2.gif", 
		 Crawler.ParseLink(link, srcUrl));
  }

  public void testParseLinkParsesBodyRelativeLink() throws MalformedURLException{
    StringBuffer link = new StringBuffer("body background=test/test2.gif");
    URL srcUrl = new URL("http://www.test.org");
    assertEquals("http://www.test.org/test/test2.gif", 
		 Crawler.ParseLink(link, srcUrl));
  }

  public void testParseLinkParsesScript() throws MalformedURLException{
    StringBuffer link = 
      new StringBuffer("script src=http://www.test.org/test/test2.html");
    URL srcUrl = new URL("http://www.test.org");
    assertEquals("http://www.test.org/test/test2.html", 
		 Crawler.ParseLink(link, srcUrl));
  }

  public void testParseLinkParsesScriptRelativeLink() throws MalformedURLException{
    StringBuffer link = new StringBuffer("script src=test/test2.html");
    URL srcUrl = new URL("http://www.test.org");
    assertEquals("http://www.test.org/test/test2.html", 
		 Crawler.ParseLink(link, srcUrl));
  }

  public void testParseLinkParsesTd() throws MalformedURLException{
    StringBuffer link = 
      new StringBuffer("td background=http://www.test.org/test/test2.gif");
    URL srcUrl = new URL("http://www.test.org");
    assertEquals("http://www.test.org/test/test2.gif", 
		 Crawler.ParseLink(link, srcUrl));
  }

  public void testParseLinkParsesTdRelativeLink() throws MalformedURLException{
    StringBuffer link = new StringBuffer("td background=test/test2.gif");
    URL srcUrl = new URL("http://www.test.org");
    assertEquals("http://www.test.org/test/test2.gif", 
		 Crawler.ParseLink(link, srcUrl));
  }

  public void testParseLinkParsesTable() throws MalformedURLException{
    StringBuffer link = 
      new StringBuffer("table background=http://www.test.org/test/test2.gif");
    URL srcUrl = new URL("http://www.test.org");
    assertEquals("http://www.test.org/test/test2.gif", 
		 Crawler.ParseLink(link, srcUrl));
  }

  public void testParseLinkParsesTableRelativeLink() throws MalformedURLException{
    StringBuffer link = new StringBuffer("table background=test/test2.gif");
    URL srcUrl = new URL("http://www.test.org");
    assertEquals("http://www.test.org/test/test2.gif", 
		 Crawler.ParseLink(link, srcUrl));
  }

  public void testParseLinkParsesLinksWithMultipleKeys() 
      throws MalformedURLException{
    StringBuffer link = 
      new StringBuffer("body background=test/test2.gif text=white");
    URL srcUrl = new URL("http://www.test.org");
    assertEquals("http://www.test.org/test/test2.gif", 
		 Crawler.ParseLink(link, srcUrl));
  }


  public void testDoOneCrawlCycleOneLink(){
    Vector list = new Vector();
    String source = "<html><head>"+
      "<title>Test</title></head>"+
      "<body><a href=\"http://www.test.org/\"></body></html>";
    
    StringInputStream strStream = new StringInputStream(source);
    MockCachedUrl cu = new MockCachedUrl("http://www.test.org/index.html");
    cu.setUncachedInputStream(strStream);
    cu.setShouldBeCached(true);
    Properties prop = new Properties();
    prop.setProperty("content-type", "text/html");
    cu.setUncachedProperties(prop);


    Crawler.doOneCrawlCycle(cu, list);
    assertTrue("List didn't contain http://www.test.org/",
	       list.contains("http://www.test.org/"));
    assertEquals(1, list.size());
    
  }

  public void testDoOneCrawlCycleDoesNotAssLinksForExistingFile(){
    Vector list = new Vector();
    String source = "<html><head>"+
      "<title>Test</title></head>"+
      "<body><a href=\"http://www.test.org/\"></body></html>";
    
    StringInputStream fileStream = new StringInputStream(source);
    StringInputStream httpStream = new StringInputStream(source);
    MockCachedUrl cu = new MockCachedUrl("http://www.test.org/index.html");
    cu.setCachedInputStream(fileStream);
    cu.setUncachedInputStream(httpStream);
    cu.setShouldBeCached(true);
    cu.setExists(true);

    Crawler.doOneCrawlCycle(cu, list);
    assertEquals(0, list.size());
  }
}
