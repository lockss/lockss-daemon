/*
 * $Id: TestGoslingHtmlParser.java,v 1.14.2.1 2004-04-17 20:00:23 tlipkis Exp $
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
public class TestGoslingHtmlParser extends LockssTestCase {

  public static final String startUrl = "http://www.example.com/index.html";

  GoslingHtmlParser parser = null;
  MyFoundUrlCallback cb = null;

  public void setUp() throws Exception {
    super.setUp();
    MockArchivalUnit mau = new MockArchivalUnit();
    parser = new GoslingHtmlParser();
    cb = new MyFoundUrlCallback();
  }

  public void testThrowsOnNullCachedUrl() throws IOException {
    try {
      parser.parseForUrls(null, new MyFoundUrlCallback());
      fail("Calling parseForUrls with a null CachedUrl should have thrown");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testThrowsOnNullCallback() throws IOException {
    try {
      parser.parseForUrls(new MockCachedUrl("http://www.example.com/"), null);
      fail("Calling parseForUrls with a null FoundUrlCallback should have thrown");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testParsesHref() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.html",
  			 "<a href=", "</a>");
  }

  public void testParsesHrefWithTab() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.html",
  			 "<a\thref=", "</a>");
  }

  public void testParsesHrefWithCarriageReturn() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.html",
  			 "<a\rhref=", "</a>");
  }

  public void testParsesHrefWithNewLine() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.html",
  			 "<a\nhref=", "</a>");
  }

  public void testDoCrawlImage() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
			 "<img src=", "</img>");
  }

  public void testDoCrawlImageWithSrcInAltTag() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
			 "<img alt=src src=", "</img>");
    singleTagShouldParse("http://www.example.com/web_link.jpg",
			 "<img alt = src src=", "</img>");
  }

  public void testDoCrawlImageWithSrcInAltTagAfterSrcProper()
      throws IOException {
    String url= "http://www.example.com/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<img src="+url+" alt=src>link3</a>";

    MockCachedUrl mcu = new MockCachedUrl(startUrl);
    mcu.setContent(source);

    parser.parseForUrls(mcu, cb);

    Set expected = SetUtil.set(url);
    assertEquals(expected, cb.getFoundUrls());
  }

  public void testDoCrawlFrame() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.html",
			 "<frame src=", "</frame>");
  }

  public void testDoCrawlLink() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.css",
			 "<link href=", "</link>");
  }

  public void testDoCrawlBody() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
		   "<body background=", "</body>");
  }

  public void testDoCrawlTable() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
		   "<table background=", "</table>");
  }

  public void testDoCrawlTc() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
		   "<tc background=", "</tc>");
  }

  public void testDoCrawlWithEqualsInUrl() throws IOException {
    singleTagShouldParse(
        "http://www.example.com/acs/a/toc.select?in_coden=jcisd8&in_volume=43",
        "<a href=", "</a>");
  }

  public void testDoCrawlWithLineBreakBeforeTag() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.html",
                         "<a\nhref=", "</a");
  }

  public void testDoCrawlWithAmpInUrl() throws IOException {
    singleTagShouldParse("http://www.example.com?pageid=pid&amp;parentid=parid&amp",
                         "<a href=", "</a");
  }


  private void singleTagShouldParse(String url,
 				    String startTag, String endTag)
      throws IOException {
    singleTagParse(url, startTag, endTag, true);
  }

  private void singleTagShouldNotParse(String url,
 				    String startTag, String endTag)
      throws IOException {
    singleTagParse(url, startTag, endTag, false);
  }

  private void singleTagParse(String url, String startTag,
			      String endTag, boolean shouldParse)
  throws IOException {
    MockCachedUrl mcu = new MockCachedUrl("http://www.example.com");
    String content = makeContent(url, startTag, endTag);
    mcu.setContent(content);

    MyFoundUrlCallback cb = new MyFoundUrlCallback();
    parser.parseForUrls(mcu, cb);

    if (shouldParse) {
      Set expected = SetUtil.set(url);
      assertEquals("Misparsed: "+content, expected, cb.getFoundUrls());
    } else {
      Set expected = SetUtil.set();
      assertEquals("Misparsed: "+content, expected, cb.getFoundUrls());
    }
  }


  public void testDoNotCrawlBadA() throws IOException {
    String[] badTags = {
      "<a harf=",
      "<a hre=",
      "<a hrefe=",
      "<al href="
    };
    checkBadTags(badTags, "</a>");
  }

  public void testDoNotCrawlBadFrameTag() throws IOException {
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

  public void testDoNotCrawlBadImgTag() throws IOException {
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

  public void testDoNotCrawlBadLinkTag() throws IOException {
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

  public void testDoNotCrawlBadBodyTag() throws IOException {
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

  public void testDoNotCrawlBadScriptTag() throws IOException {
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

  public void testDoNotCrawlBadTableTag() throws IOException {
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

  public void testDoNotCrawlBadTcTag() throws IOException {
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

  public void testCollectsHttps() throws IOException {
    String url = "https://www.example.com/link3.html";
    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=\""+url+"\">link3</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testGetAttribute() throws IOException {
    // no value found
    assertEquals(null, parser.getAttributeValue("href", "a bar=foo"));
    assertEquals(null, parser.getAttributeValue("href", "a href"));
    assertEquals(null, parser.getAttributeValue("href", "a href="));
    assertEquals(null, parser.getAttributeValue("href", "a href= "));
    // find proper attribute
    assertEquals("foo", parser.getAttributeValue("tag", "a tag=foo tag=bar"));
    assertEquals("bar", parser.getAttributeValue("tag", "a ta=foo tag=bar"));
    assertEquals("bar", parser.getAttributeValue("tag", "a xy=foo\n tag=bar"));
    // whitespace
    assertEquals("foo", parser.getAttributeValue("href", "a href=foo"));
    assertEquals("foo", parser.getAttributeValue("href", "a href =foo"));
    assertEquals("foo", parser.getAttributeValue("href", "a href = foo"));
    assertEquals("foo", parser.getAttributeValue("href", "a href = foo\n"));
    assertEquals("foo", parser.getAttributeValue("href", "a href= foo"));
    assertEquals("foo", parser.getAttributeValue("href", "a href\t  = \n foo"));
    // quoted strings & whitespace
    assertEquals("foo", parser.getAttributeValue("href", "a href=\"foo\""));
    assertEquals("foo", parser.getAttributeValue("href", "a href=\"foo\""));
    assertEquals("fo o", parser.getAttributeValue("href", "a href  =\"fo o\""));
    assertEquals("fo'o", parser.getAttributeValue("href", "a href=  \"fo'o\""));
    assertEquals("foo", parser.getAttributeValue("href", "a href  =\"foo\""));
    assertEquals("foo", parser.getAttributeValue("href", "a href='foo'"));
    assertEquals("foo", parser.getAttributeValue("href", "a href='foo'"));
    assertEquals("fo o", parser.getAttributeValue("href", "a href  ='fo o'"));
    assertEquals("fo\"o", parser.getAttributeValue("href", "a href=  'fo\"o'"));
    assertEquals("foo", parser.getAttributeValue("href", "a href  ='foo'"));
    // empty quoted strings
    assertEquals("", parser.getAttributeValue("href", "a href=\"\""));
    assertEquals("", parser.getAttributeValue("href", "a href=''"));
    // dangling quoted strings
    assertEquals("", parser.getAttributeValue("href", "a href=\""));
    assertEquals("xy", parser.getAttributeValue("href", "a href=\"xy"));
  }

  public void testEmptyAttribute() throws IOException {
    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=>link3</a>";
    assertEquals(SetUtil.set(), parseSingleSource(source));
  }

  //parser now should return urls for protocols we don't know.  Crawler will
  //ignore them.
  public void testParseUnknownProtocol() throws IOException {
    String url = "badprotocol://www.example.com/link3.html";
    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=\""+url+"\">link3</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testParsesFileWithQuotedUrls() throws IOException {
    String url= "http://www.example.com/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=\"http://www.example.com/link3.html\">link3</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }
//   public void testDoHrefInAnchorJavascript() throws IOException {
//     String url= "http://www.example.com/link3.html";
//     String url2 = "http://www.example.com/link2.html";
//     String url3 = "http://www.example.com/link1.html";

//     String source =
//       "<html><head><title>Test</title></head><body>"+
//       "<a href = javascript:newWindow('http://www.example.com/link3.html')</a>"
//     + "<a href = javascript:popup('http://www.example.com/link2.html')</a>"
//     + "<img src = javascript:popup('" + url3 + "') </img>";
//     assertEquals(SetUtil.set(url, url2), parseSingleSource(source));
//   }

  public void testInterpretatesBase() throws IOException {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example2.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<base href=http://www.example.com>"+
      "<a href=link1.html>link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<base href=http://www.example2.com>"+
      "<a href=link2.html>link2</a>"+
      "<base href=http://www.example.com>"+
      "<a href=link3.html>link3</a>";
    assertEquals(SetUtil.set(url1, url2, url3), parseSingleSource(source));
  }

  public void testInterpretatesBadBase() throws IOException {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example2.com/link2.html";
    String url3= "http://www.example2.com/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<base href=http://www.example.com>"+
      "<a href=link1.html>link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<base href=http://www.example2.com>"+
      "<a href=link2.html>link2</a>"+
      "<base href=www.example.com>"+
      "<a href=link3.html>link3</a>";
    assertEquals(SetUtil.set(url1, url2, url3), parseSingleSource(source));
  }

  public void testSkipsComments() throws IOException {
    String url= "http://www.example.com/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<!--<a href=http://www.example.com/link1.html>link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a href=http://www.example.com/link2.html>link2</a>-->"+
      "<a href=http://www.example.com/link3.html>link3</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testKeepsSpaceInUrl() throws IOException {
    String url= "http://www.example.com/link%20with%20space.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=\"http://www.example.com/link with space.html\">Link</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testIgnoresNewLineInUrl() throws IOException {
    String url= "http://www.example.com/linkwithspace.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=\"http://www.example.com/link\nwith\nspace.html\">Link</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testIgnoresNewLineInField() throws IOException {
    String url= "http://www.example.com/link.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<img\nsrc=\"http://www.example.com/link.html\">Link</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testIgnoresCRInUrl() throws IOException {
    String url= "http://www.example.com/linkwithspace.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=\"http://www.example.com/link\rwith\rspace.html\">Link</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testKeepsDoubleQuoteInUrl() throws IOException {
    String url= "http://www.example.com/link%22with%22quotes.html";
    
    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href='http://www.example.com/link\"with\"quotes.html'>Link</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }
  
  public void testKeepsSingleQuoteInUrl() throws IOException {
    String url= "http://www.example.com/link'with'quotes.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=\""+url+"\">Link</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testMultipleLinks() throws IOException {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href="+url1+">link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a href="+url2+">link2</a>"+
      "<a href="+url3+">link3</a>";

    assertEquals(SetUtil.set(url1, url2, url3), parseSingleSource(source));
  }

  public void testRelativeLinksLocationTagsAndMultipleKeys()
      throws IOException {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/dir/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=link1.html>link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a blah1=blah href=link2.html#ref blah2=blah>link2</a>"+
      "<a href=dir/link3.html>link3</a>";

    assertEquals(SetUtil.set(url1, url2, url3), parseSingleSource(source));
  }

  public void testHttpEquiv() throws IOException {
    String url1= "http://example.com/blah.html";
    String source = 
      "<html><head>"
      +"<meta http-equiv=\"refresh\" "
      +"content=\"0; url=http://example.com/blah.html\">"
      +"</head></html>";

    assertEquals(SetUtil.set(url1), parseSingleSource(source));
  }

  //tests that we are only parsing out the URL when the
  // http-equiv header is "refresh"
  public void testHttpEquiv2() throws IOException {
    String url1= "http://example.com/blah.html";
    String source = 
      "<html><head>"+
      "<meta http-equiv=\"blah\" "
      +"content=\"0; url=http://example.com/blah.html\">"+
      "</head></html>";

    assertEquals(SetUtil.set(), parseSingleSource(source));
  }

  private Set parseSingleSource(String source) throws IOException {
    MockCachedUrl mcu = new MockCachedUrl("http://www.example.com");
    mcu.setContent(source);
    
    parser.parseForUrls(mcu, cb);
    
    return cb.getFoundUrls();
  }


  public void testRelativeLinksWithSameName() throws IOException {
    String url1= "http://www.example.com/branch1/index.html";
    String url2= "http://www.example.com/branch2/index.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=branch1/index.html>link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a href=branch2/index.html>link2</a>";

    MockCachedUrl mcu = new MockCachedUrl("http://www.example.com");
    mcu.setContent(source);

    parser.parseForUrls(mcu, cb);

    Set expected = SetUtil.set(url1, url2);
    assertEquals(expected, cb.getFoundUrls());
  }

  public void testRelativeLinksUseRedirectedToAsBase() throws IOException {
    String url1= "http://www.example.com/extra_level/branch1/index.html";
    String url2= "http://www.example.com/extra_level/branch2/index.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=branch1/index.html>link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a href=branch2/index.html>link2</a>";

    MockCachedUrl mcu = new MockCachedUrl("http://www.example.com");
    CIProperties props = new CIProperties();
    props.put(CachedUrl.PROPERTY_CONTENT_URL,
	      "http://www.example.com/extra_level/");
    mcu.setContent(source);
    mcu.setProperties(props);

    parser.parseForUrls(mcu, cb);

    Set expected = SetUtil.set(url1, url2);
    assertEquals(expected, cb.getFoundUrls());
  }


  private void checkBadTags(String[] badTags, String closeTag)
      throws IOException {
    String url = "http://www.example.com/web_link.html";
    for (int ix=0; ix<badTags.length; ix++) {
      singleTagShouldNotParse(url, badTags[ix], closeTag);
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

  private class MyFoundUrlCallback implements ContentParser.FoundUrlCallback {
    Set foundUrls = new HashSet();

    public void foundUrl(String url) {
      foundUrls.add(url);
    }

    public Set getFoundUrls() {
      return foundUrls;
    }
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestGoslingHtmlParser.class.getName()};
    junit.textui.TestRunner.main(testCaseList);
  }
}
