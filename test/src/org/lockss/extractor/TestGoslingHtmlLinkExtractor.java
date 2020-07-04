/*
 * $Id$
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.extractor;

import org.lockss.plugin.ArchivalUnit;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.ListUtil;
import org.lockss.util.SetUtil;
import org.lockss.util.TypedEntryMap;

import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class TestGoslingHtmlLinkExtractor extends LockssTestCase {

  public static final String startUrl = "http://www.example.com/index.html";
  static String ENC = Constants.DEFAULT_ENCODING;

  GoslingHtmlLinkExtractor extractor = null;
  MyLinkExtractorCallback cb = null;

  MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    mau = new MockArchivalUnit();
    extractor = new GoslingHtmlLinkExtractor();
    cb = new MyLinkExtractorCallback();
  }

  public void testThrowsOnNullInputStream() throws IOException {
    try {
      extractor.extractUrls(mau, null, ENC, "http://www.example.com/",
			    new MyLinkExtractorCallback());
      fail("Calling extractUrls with a null InputStream should have thrown");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testThrowsOnNullSourceUrl() throws IOException {
    try {
      extractor.extractUrls(mau, new StringInputStream("Blah"), ENC,
			    null, new MyLinkExtractorCallback());
      fail("Calling extractUrls with a null CachedUrl should have thrown");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testThrowsOnNullCallback() throws IOException {
    try {
      extractor.extractUrls(mau, new StringInputStream("blah"), ENC,
			  "http://www.example.com/", null);
      fail("Calling extractUrls with a null callback should have thrown");
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

  public void testParsesImage() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
                         "<img src=", "</img>");
    // test parssing the tag with attriutes before the link
    singleTagShouldParse("http://www.example.com/web_link.jpg",
                         "<img\nwidth='280' hight='90' src=", "</img>");
  }

  public void testDoesNotParsesImageData() throws Exception {
    String data_uri = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA" +
        "AAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO" +
        "9TXL0Y4OHwAAAABJRU5ErkJggg==";
    String src =
        "<img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA" +
            "AAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO" +
            "9TXL0Y4OHwAAAABJRU5ErkJggg==\" alt=\"Red dot\" />";
    singleTagShouldNotParse(data_uri, "<img src=","</img>");
    assertEquals(SetUtil.set(), parseSingleSource(src));
  }

  public void testParsesDataUri() throws Exception {
    String url= "http://www.example.com/link3.html";
    String data_uri = "data:text/html;charset=utf-8," +
        "%3Ca+href%3D%22http%3A%2F%2Fwww.example.com%2Flink3.html%22%3Elink3%3C%2Fa%3E";
    String source =
        "<html><head><title>Test</title></head><body>"+
            "<a href=\"" + data_uri + "\">link3</a>";
    MockArchivalUnit mau = new MockArchivalUnit();
    mau.setLinkExtractor("text/html", new GoslingHtmlLinkExtractor());
    MockCachedUrl mcu = new MockCachedUrl("http://www.example.com", mau);
    mcu.setContent(source);
    cb.reset();
    extractor.extractUrls(mau, new StringInputStream(source), ENC,
        "http://www.example.com", cb);
    assertEquals(SetUtil.set(url), cb.getFoundUrls());
  }

  public void testParsesIFrame() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
                         "<iframe src=", "</iframe>");
    // test parssing the tag with attriutes before the link
    singleTagShouldParse("http://www.example.com/web_link.jpg",
                         "<iframe\nwidth='280' hight='90' src=", "</iframe>");
  }

  public void testParsesEmbed() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
			 "<embed src=", "</embed>");
  }

  public void testParsesApplet() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
                         "<applet code=", "</applet>");
  }

  public void testParsesArea() throws IOException {
     singleTagShouldParse("http://www.example.com/web_link.shtml",
			 "<area href=", "</area>");
     singleTagShouldParse("http://www.example.com/web_link.shtml",
                          "<area shape='rect' coords='279,481,487' href=",
                          "</area>");
  }

  public void testParsesObject() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
 			 "<object codebase=", "</object>");
  }

  public void testParsesOptionPositive() throws IOException {
    TypedEntryMap pMap = new TypedEntryMap();
    pMap.setMapElement("html-parser-select-attrs", ListUtil.list("value"));
    mau.setPropertyMap(pMap);
    singleTagShouldParse("http://www.example.com/web_link.jpg",
                         "<option  value=", "</option>", mau);
    singleTagShouldParse("http://www.example.com/web_link.jpg",
                         "<option a=b value=", "</option>", mau);
  }

  public void testParsesOptionNegative() throws IOException {
    singleTagShouldNotParse("http://www.example.com/web_link.jpg",
                            "<option  value=", "</option>");
    singleTagShouldNotParse("http://www.example.com/web_link.jpg",
                            "<option a=b value=", "</option>");
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

    extractor.extractUrls(mau, new StringInputStream(source), ENC,
			  startUrl, cb);

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
    singleTagShouldParse("http://www.example.com/web_link.css",
			 "<link rel=\"stylesheet\" type=\"text/css\" media=\"screen\"  href=", "</link>");
  }
  
  public void testStyleAttr() throws IOException {
    extractor = new GoslingHtmlLinkExtractor();

    String url= "http://www.example.com/back.png";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<span class=\"foo\" " +
      "style=\"background: url('" + url + "') no-repeat 0px -64px;\" />";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testDoCrawlStyleAbsolute() throws IOException {
    performDoCrawlStyle("<style>",
                        "http://www.example.com/",
                        "http://www.example.com/");
  }

  public void testDoCrawlStyleRelative() throws IOException {
    performDoCrawlStyle("<style>",
                        "",
                        "http://www.example.com/");
  }

  public void testDoCrawlStyleWithTypeAttributeAbsolute() throws IOException {
    performDoCrawlStyle("<style type=\"text/css\">",
                        "http://www.example.com/",
                        "http://www.example.com/");
  }

  public void testDoCrawlStyleWithTypeAttributeRelative() throws IOException {
    performDoCrawlStyle("<style type=\"text/css\">",
                        "",
                        "http://www.example.com/");
  }

  // ensure that scanning continues after a nested parser throws an error
  // XXX Need to cause an IOException while reading CSS from the stream
  public void xxxtestDoCrawlStyleError() throws IOException {
    String url1= "http://example.com/blah1.html";
    String url2= "http://example.com/blah2.html";
    String url3= "http://example.com/blah3.html";
    String source =
      "<html><head>"+
      "<style type=\"text/css\">\n" +
      "<!--\n" +
      "@import url(\'" + url1 + "\');\n" + // ensure css parser got invoked
      "foo {bgcolor: #FFFF};" +		   // and that this causes an error
      "@import url(\'" + url2 + "\');\n" + // so that this one isn't found
      "-->\n" +
      "  </style>\n" +
      "<a href=" + url3 + "></a>" +	// and this one is
      "</head></html>";
    assertEquals(SetUtil.set(url1, url3), parseSingleSource(source));
  }

  protected void performDoCrawlStyle(String openingStyleTag,
                                     String givenPrefix,
                                     String expectedPrefix)
      throws IOException {
    String url1 = "foo1.css";
    String url2 = "foo2.css";
    String url3 = "foo3.css";
    String url4 = "foo4.css";
    String url5 = "img5.gif";
    String url6 = "img6.gif";

    String source =
      "<html>\n" +
      " <head>\n" +
      "  <title>Test</title>\n" +
      "  " + openingStyleTag + "\n" +
      "<!--\n" +
      "@import url(\'" + givenPrefix + url1 + "\');\n" +
      "@import url(\"" + givenPrefix + url2 + "\");\n" +
      "@import \'" + givenPrefix + url3 + "\';\n" +
      "@import \"" + givenPrefix + url4 + "\";\n" +
      "foo {\n" +
      " bar: url(\'" + givenPrefix + url5 + "\');\n" +
      " baz: url(\"" + givenPrefix + url6 + "\");\n" +
      "}\n" +
      "/* Comment */" +
      "-->\n" +
      "  </style>\n" +
      " </head>\n" +
      " <body>\n" +
      "  <p>Fake content</p>\n" +
      " </body>\n" +
      "</html>\n";

    

    assertEquals(SetUtil.set(expectedPrefix + url1,
                             expectedPrefix + url2,
                             expectedPrefix + url3,
                             expectedPrefix + url4,
                             expectedPrefix + url5,
                             expectedPrefix + url6),
                 parseSingleSource(source));
  }

  public void testDoCrawlBody() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
		   "<body background=", "</body>");
  }

  /**
   * @see <a href="http://msdn.microsoft.com/workshop/author/dhtml/reference/properties/background_2.asp">
   *      Microsoft extension: <code>background</code> attribute for
   *      <code>table</code>, <code>td</code> and <code>th</code></a>
   */
  public void testDoCrawlTable() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
		   "<table background=", "</table>");
  }

  /**
   * @see <a href="http://msdn.microsoft.com/workshop/author/dhtml/reference/properties/background_2.asp">
   *      Microsoft extension: <code>background</code> attribute for
   *      <code>table</code>, <code>td</code> and <code>th</code></a>
   */
  public void testDoCrawlTd() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
                   "<td background=", "</td>");
  }

  /**
   * @see <a href="http://msdn.microsoft.com/workshop/author/dhtml/reference/properties/background_2.asp">
   *      Microsoft extension: <code>background</code> attribute for
   *      <code>table</code>, <code>td</code> and <code>th</code></a>
   */
  public void testDoCrawlTh() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
                   "<th background=", "</th>");
  }

  public void testDoCrawlScript() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
		   "<script src=", "</script>");
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

//XXX this looks like it was testing the incorrect behavior
//   public void testDoCrawlWithAmpInUrl() throws IOException {
//     singleTagShouldParse("http://www.example.com?pageid=pid&amp;parentid=parid&amp",
//                          "<a href=", "</a");
//   }


  private void singleTagShouldParse(String url,
 				    String startTag, String endTag)
      throws IOException {
    singleTagShouldParse(url, startTag, endTag, null);
  }

  private void singleTagShouldParse(String url,
 				    String startTag, String endTag,
 				    ArchivalUnit au)
      throws IOException {
    singleTagParse(url, startTag, endTag, au, true);
  }

  private void singleTagShouldNotParse(String url,
                                       String startTag, String endTag)
      throws IOException {
    singleTagShouldNotParse(url, startTag, endTag, null);
  }

  private void singleTagShouldNotParse(String url,
                                       String startTag, String endTag,
                                       ArchivalUnit au)
      throws IOException {
    singleTagParse(url, startTag, endTag, au, false);
  }

  private void singleTagParse(String url, String startTag,
			      String endTag, ArchivalUnit au,
			      boolean shouldParse)
      throws IOException {
    MockCachedUrl mcu = new MockCachedUrl("http://www.example.com");
    String content = makeContent(url, startTag, endTag);
    mcu.setContent(content);

    MyLinkExtractorCallback cb = new MyLinkExtractorCallback();
    extractor.extractUrls(mau, new StringInputStream(content), ENC,
			"http://www.example.com", cb);

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

  public void testDoNotCrawlBadTdTag() throws IOException {
    String[] badTags = {
      "<t background=",
      "<tdl background=",
      "<ta background=",
      "<td backgroun=",
      "<td backgroundl=",
      "<td backgrouno="
    };
    checkBadTags(badTags, "</td>");
  }

  public void testDoNotCrawlBadThTag() throws IOException {
    String[] badTags = {
      "<t background=",
      "<thl background=",
      "<ta background=",
      "<th backgroun=",
      "<th backgroundl=",
      "<th backgrouno="
    };
    checkBadTags(badTags, "</th>");
  }

  // Behavior currently depends on Java version, do disabled.  Crawler
  // excludes everything but http anyway.
  public void donttestCollectsHttps() throws IOException {
    String url = "https://www.example.com/link3.html";
    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=\""+url+"\">link3</a>";
    // empty in 1.3, no https stream handler
    assertEmpty(parseSingleSource(source));
    // 1.4 has one
//     assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testGetAttribute() throws IOException {
    // no value found
    assertEquals(null, extractor.getAttributeValue("href", "a bar=foo"));
    assertEquals(null, extractor.getAttributeValue("href", "a href"));
    assertEquals(null, extractor.getAttributeValue("href", "a href="));
    assertEquals(null, extractor.getAttributeValue("href", "a href= "));
    // find proper attribute
    assertEquals("foo", extractor.getAttributeValue("tag", "a tag=foo tag=bar"));
    assertEquals("bar", extractor.getAttributeValue("tag", "a ta=foo tag=bar"));
    assertEquals("bar", extractor.getAttributeValue("tag", "a xy=foo\n tag=bar"));
    // whitespace
    assertEquals("foo", extractor.getAttributeValue("href", "a href=foo"));
    assertEquals("foo", extractor.getAttributeValue("href", "a href =foo"));
    assertEquals("foo", extractor.getAttributeValue("href", "a href = foo"));
    assertEquals("foo", extractor.getAttributeValue("href", "a href = foo\n"));
    assertEquals("foo", extractor.getAttributeValue("href", "a href= foo"));
    assertEquals("foo", extractor.getAttributeValue("href", "a href\t  = \n foo"));
    // quoted strings & whitespace
    assertEquals("foo", extractor.getAttributeValue("href", "a href=\"foo\""));
    assertEquals("foo", extractor.getAttributeValue("href", "a href=\"foo\""));
    assertEquals("fo o", extractor.getAttributeValue("href", "a href  =\"fo o\""));
    assertEquals("fo'o", extractor.getAttributeValue("href", "a href=  \"fo'o\""));
    assertEquals("foo", extractor.getAttributeValue("href", "a href  =\"foo\""));
    assertEquals("foo", extractor.getAttributeValue("href", "a href='foo'"));
    assertEquals("foo", extractor.getAttributeValue("href", "a href='foo'"));
    assertEquals("fo o", extractor.getAttributeValue("href", "a href  ='fo o'"));
    assertEquals("fo\"o", extractor.getAttributeValue("href", "a href=  'fo\"o'"));
    assertEquals("foo", extractor.getAttributeValue("href", "a href  ='foo'"));
    // empty quoted strings
    assertEquals("", extractor.getAttributeValue("href", "a href=\"\""));
    assertEquals("", extractor.getAttributeValue("href", "a href=''"));
    // dangling quoted strings
    assertEquals("", extractor.getAttributeValue("href", "a href=\""));
    assertEquals("xy", extractor.getAttributeValue("href", "a href=\"xy"));
    assertEquals("/cgi/reprint/21/1/2.pdf",
                 extractor.getAttributeValue("href",
                                          "a target=\"_self\" href=\"/cgi/reprint/21/1/2.pdf\" onclick=\"cancelLoadPDF()\""));

  }

  public void testEmptyAttribute() throws IOException {
    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=>link3</a>";
    assertEquals(SetUtil.set(), parseSingleSource(source));
  }

  // Extractor does not return urls for unknown protocols.
  public void testParseUnknownProtocol() throws IOException {
    String url = "badprotocol://www.example.com/link3.html";
    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=\""+url+"\">link3</a>";
    assertEmpty(parseSingleSource(source));
  }

  public void testParsesFileWithQuotedUrls() throws IOException {
    String url= "http://www.example.com/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=\"http://www.example.com/link3.html\">link3</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testDontParseJSByDefault() throws IOException {
    String url3 = "http://www.example.com/link1.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href = javascript:newWindow('http://www.example.com/link3.html')</a>"
    + "<a href = javascript:popup('http://www.example.com/link2.html')</a>"
    + "<img src = javascript:popup('" + url3 + "') </img>";
    assertEquals(SetUtil.set(), parseSingleSource(source));
  }

  public void testParseJSIfConf() throws IOException {
    Properties p = new Properties();
    p.setProperty(GoslingHtmlLinkExtractor.PARAM_PARSE_JS, "true");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    extractor = new GoslingHtmlLinkExtractor();

    String url= "http://www.example.com/link3.html";
    String url2 = "http://www.example.com/link2.html";
    String url3 = "http://www.example.com/link1.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href = javascript:newWindow('http://www.example.com/link3.html')</a>"
    + "<a href = javascript:popup('http://www.example.com/link2.html')</a>"
    + "<img src = javascript:popup('" + url3 + "') </img>";
    assertEquals(SetUtil.set(url, url2, url3), parseSingleSource(source));
  }

  public void testDontParseMailto() throws IOException {
    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href = mailto:user@example.com</a>";
    assertEquals(SetUtil.set(), parseSingleSource(source));
  }

  /**
   * Included to test a chunk of HighWire HTML that we're not parsing correctly
   */
  public void testParseHWPDF() throws IOException {
//     Properties p = new Properties();
//     p.setProperty(GoslingHtmlLinkExtractor.PARAM_PARSE_JS, "true");
//     ConfigurationUtil.setCurrentConfigFromProps(p);
//     extractor = new GoslingHtmlLinkExtractor();

    String url= "http://www.example.com/cgi/reprint/21/1/2.pdf";

    String source =
      "<table cellspacing=\"0\" cellpadding=\"10\" width=\"250\" border=\"0\">" +
      "<tr><td align=center bgcolor=\"#DBDBDB\">\n\n	" +
      "<font face=\"verdana,arial,helvetica,sans-serif\">" +
      "<strong><font size=+1>Automatic download</font><br>\n	" +
      "<font size=\"-1\">[<a target=\"_self\" href=\"/cgi/reprint/21/1/2.pdf\" " +
      "onclick=\"cancelLoadPDF()\">Begin manual download</a>]</strong></font>\n";
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

//   public void testNormalizeHash() throws MalformedURLException {
//     assertEquals("http://www.bioone.org/bioone/?request=get-toc&issn=0044-7447&volume=32&issue=1",
// 		 UrlUtil.normalizeUrl("http://www.bioone.org/bioone/?request=get-toc&#38;issn=0044-7447&#38;volume=32&issue=1"));
//   }


  public void testResolvesHtmlEntities()
      throws IOException {
    String url1=
      "http://www.example.com/bioone/?"+
      "request=get-toc&issn=0044-7447&volume=32&issue=1";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=http://www.example.com/bioone/?"+
      "request=get-toc&#38;issn=0044-7447&#38;volume=32&issue=1>link1</a>";
    assertEquals(SetUtil.set(url1), parseSingleSource(source));

    // ensure character entities processed before rel url resolution
    source =
      "<html><head><title>Test</title></head><body>"+
      "<base href=http://www.example.com/foo/bar>"+
      "<a href=&#46&#46/xxx>link1</a>";
    assertEquals(SetUtil.set("http://www.example.com/xxx"),
		 parseSingleSource(source));
  }

  public void testInterpretsBaseTag() throws IOException {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
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

  public void testAlternateDocUrl() throws Exception {
    String url1 = "http://www.example.com/x/y/link1.html";
    String url2 = "http://www.example.com/x/y/link2.html";
    String url3 = "http://www.example.com/x/y/link3.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<a href=link1.html>link1</a>"
      + "Filler, with <b>bold</b> tags and<i>others</i>"
      + "<a href=link2.html>link2</a>"
      + "<a href=link3.html>link3</a>";
    assertEquals(SetUtil.set(url1, url2, url3),
		 parseSingleSource("http://www.example.com/x/y/", source));
  }

  public void testAbsBaseTag() throws Exception {
    String url0 = "http://www.example.com/link0.html";
    String url1 = "http://www.example222.com/link1.html";
    String url2 = "http://www.example222.com/link2.html";
    String url3 = "http://www.example222.com/link3.html";

    String source = "<html><head><title>Test</title></head><body>"
      // <base> should appliy to whole document, including links before it
      // documenting that GoslingHtmlLinkExtractor doesn't do this correctly
      + "<a href=link0.html>link1</a>"
      + "<base href=http://www.example222.com>"
      + "<a href=link1.html>link1</a>"
      + "Filler, with <b>bold</b> tags and<i>others</i>"
      // only the first base tag should take effect
      + "<base href=http://www.example2.com>"
      + "<a href=link2.html>link2</a>"
      + "<base href=http://www.example3.com>"
      + "<a href=link3.html>link3</a>";
    assertEquals(SetUtil.set(url0, url1, url2, url3),
		 parseSingleSource(source));
  }

  public void testRelBaseTag1() throws Exception {
    String url0 = "http://www.example.com/link0.html";
    String url1 = "http://www.example.com/foo/link1.html";
    String url2 = "http://www.example.com/foo/link2.html";
    String url3 = "http://www.example.com/foo/link3.html";

    String source = "<html><head><title>Test</title></head><body>"
      // <base> should appliy to whole document, including links before it
      // documenting that GoslingHtmlLinkExtractor doesn't do this correctly
      + "<a href=link0.html>link1</a>"
      + "<base href=foo/>"
      + "<a href=link1.html>link1</a>"
      + "Filler, with <b>bold</b> tags and<i>others</i>"
      // only the first base tag should take effect
      + "<base href=http://www.example2.com>"
      + "<a href=link2.html>link2</a>"
      + "<base href=http://www.example3.com>"
      + "<a href=link3.html>link3</a>";
    assertEquals(SetUtil.set(url0, url1, url2, url3),
		 parseSingleSource(source));
  }

  public void testRelBaseTag2() throws Exception {
    String url0 = "http://www.example.com/x/y/link0.html";
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html";
    String url3 = "http://www.example.com/link3.html";

    String source = "<html><head><title>Test</title></head><body>"
      // <base> should appliy to whole document, including links before it
      // documenting that GoslingHtmlLinkExtractor doesn't do this correctly
      + "<a href=link0.html>link1</a>"
      + "<base href=/>"
      + "<a href=link1.html>link1</a>"
      + "Filler, with <b>bold</b> tags and<i>others</i>"
      + "<a href=link2.html>link2</a>"
      + "<a href=link3.html>link3</a>";
    assertEquals(SetUtil.set(url0, url1, url2, url3),
		 parseSingleSource("http://www.example.com/x/y/", source));
  }

  //Relative URLs before a malforned base tag should be extracted, as well
  //as any absolute URLs after the malformed base tag
  public void testInterpretsMalformedBaseTag() throws IOException {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example2.com/link3.html";
    String url4= "http://www.example.com/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<base href=http://www.example.com>"+
      "<a href=link1.html>link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<base href=javascript:www.example2.com>"+
      "<a href=link2.html>link2</a>"+
      "<base href=www.example.com>"+
      "<a href=http://www.example2.com/link3.html>link3</a>"+
      "<base href=http://www.example3.com>"+
      "<a href=link3.html>link4</a>";
    assertEquals(SetUtil.set(url1, url2, url3,url4),
        parseSingleSource(source));
  }

  public void testIgnoresNullHrefInBaseTag() throws IOException {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=link1.html>link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<base blah=blah>"+
      "<a href=link2.html>link2</a>"+
      "<a href=link3.html>link3</a>";
    assertEquals(SetUtil.set(url1, url2, url3), parseSingleSource(source));
  }

  public void testIgnoresEmptyHrefInBaseTag() throws IOException {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=link1.html>link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<base href=\"\" blah=blah>"+
      "<a href=link2.html>link2</a>"+
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

  public void testSkipsMalformedComments() throws IOException {
    String url= "http://www.example.com/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<!--<a href=http://www.example.com/link1.html>link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a href=http://www.example.com/link2.html>link2</a>--!>"+
      "<a href=http://www.example.com/link3.html>link3</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testSkipsScriptTags() throws IOException {
    String url= "http://www.example.com/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<script>" +
      "<a href=http://www.example.com/link1.html>link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a href=http://www.example.com/link2.html>link2</a>" +
      "</script>"+
      "<a href=http://www.example.com/link3.html>link3</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testSkipsScriptTagsAllTheWay() throws IOException {
    String url= "http://www.example.com/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<script>" +
      "<a href=http://www.example.com/link1.html>link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a href=http://www.example.com/link2.html" +
      "</script>"+
      "<a href=http://www.example.com/link3.html>link3</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }


  private void doScriptSkipTest(String openScript, String closeScript)
  	throws IOException {
    doScriptSkipTest(openScript, closeScript, null);
  }

  private void doScriptSkipTest(String openScript, String closeScript,
                                String failMsg)
  	throws IOException {
    String url= "http://www.example.com/link3.html";
    String src =
      "<html><head><title>Test</title></head><body>"+
      openScript +
      "<a href=http://www.example.com/link1.html>link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a href=http://www.example.com/link2.html>link2</a>" +
      closeScript +
      "<a href=http://www.example.com/link3.html>link3</a>";
    assertEquals(failMsg, SetUtil.set(url), parseSingleSource(src));
  }

  private String mkStr(char kar, int num) {
    StringBuffer sb = new StringBuffer(num);
    for (int ix=0; ix<num; ix++) {
      sb.append(kar);
    }
    return sb.toString();
  }

  public void testSkipsScriptTagsWhiteSpace() throws IOException {
    Properties p = new Properties();
    p.setProperty(GoslingHtmlLinkExtractor.PARAM_BUFFER_CAPACITY, "90");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    extractor = new GoslingHtmlLinkExtractor();

    for (int ix=1; ix < 200; ix += 5) {
      String whiteSpace = mkStr(' ', ix);
      doScriptSkipTest("<script"+whiteSpace+">", "</script>",
                       "Failed during iteration "+ix);
      doScriptSkipTest("<"+whiteSpace+"script>", "</script>",
                       "Failed during iteration "+ix);
      doScriptSkipTest("<script>", "<"+whiteSpace+"/script>",
                       "Failed during iteration "+ix);
      doScriptSkipTest("<script"+whiteSpace+"blah=blah>", "</script>",
                       "Failed during iteration "+ix);
//      doScriptSkipTest("<script>", "</script"+whiteSpace+">",
//                       "Failed during iteration "+ix);
    }
  }


  public void testSkipsScriptTagsIgnoreCase() throws IOException {
    doScriptSkipTest("<ScRipt>", "</sCripT>");
  }

  public void testSkipsScriptTagsSpansRing() throws IOException {
    Properties p = new Properties();
    p.setProperty(GoslingHtmlLinkExtractor.PARAM_BUFFER_CAPACITY, "90");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    extractor = new GoslingHtmlLinkExtractor();

    doScriptSkipTest("<script>", "</script>");
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
    String url2= "http://www.example.com/link2.html#ref";
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

    source =
      "<html><head>"
      +"<meta http-equiv=\"refresh\" "
      +"content=\"0;url=http://example.com/blah.html\">"
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

  private Set<String> parseSingleSource(String source) throws IOException {
    return parseSingleSource("http://www.example.com", source);
  }

  private Set<String> parseSingleSource(String docPath, String source)
      throws IOException {
    MockArchivalUnit mau = new MockArchivalUnit();
    LinkExtractor ue = new RegexpCssLinkExtractor();
    mau.setLinkExtractor("text/css", ue);
    MockCachedUrl mcu = new MockCachedUrl(docPath, mau);
    mcu.setContent(source);

    cb.reset();
    extractor.extractUrls(mau, new StringInputStream(source), ENC,
			  docPath, cb);
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

    extractor.extractUrls(mau, new StringInputStream(source), ENC,
			  "http://www.example.com", cb);

    Set expected = SetUtil.set(url1, url2);
    assertEquals(expected, cb.getFoundUrls());
  }

  public void testRelativeLinksWithLeadingSlash() throws IOException {
    String url1= "http://www.example.com/blah/branch1/index.html";
    String url2= "http://www.example.com/blah/branch2/index.html";
    String url3= "http://www.example.com/journals/american_imago/toc/aim60.1.html";
    String url4= "http://www.example.com/css/foo.css";
    String url5= "http://www.example.com/javascript/bar.js";
    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href= branch1/index.html>link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a href=\" branch2/index.html\">link2</a>"+
      "<a href =\" /journals/american_imago/toc/aim60.1.html\">" +
      "<link rel=\"stylesheet\" href=\"/css/foo.css\" >"+
      "<script type=\"text/javascript\" src=\"/javascript/bar.js\"></script>"+
      "Number 1, Spring 2003</a>";

    MockCachedUrl mcu = new MockCachedUrl("http://www.example.com/blah/");
    mcu.setContent(source);

    extractor.extractUrls(mau, new StringInputStream(source), ENC,
			  "http://www.example.com/blah/", cb);

    Set expected = SetUtil.set(url1, url2, url3, url4, url5);
    assertEquals(expected, cb.getFoundUrls());
  }

  public void testProtocolNeutralLinksHttp() throws Exception {
    String url1= "http://sample2.com/foo/bar.x";
    String url2= "http://sample3.com/bar/bar.y";
    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=\"//sample2.com/foo/bar.x\">link1</a>"+
      "<a href=\"//sample3.com/bar/bar.y\">link1</a>";

    extractor.extractUrls(mau, new StringInputStream(source), ENC,
			  "http://www.example.com/blah/", cb);
    assertEquals(SetUtil.set(url1, url2), cb.getFoundUrls());
  }

  public void testProtocolNeutralLinksHttps() throws Exception {
    String url1= "https://sample2.com/foo/bar.x";
    String url2= "https://sample3.com/bar/bar.y";
    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=\"//sample2.com/foo/bar.x\">link1</a>"+
      "<a href=\"//sample3.com/bar/bar.y\">link1</a>";

    extractor.extractUrls(mau, new StringInputStream(source), ENC,
			  "https://www.example.com/blah/", cb);
    assertEquals(SetUtil.set(url1, url2), cb.getFoundUrls());
  }

  public void testProtocolNeutralLinksBase() throws IOException {
    String url1= "http://sample2.com/foo/bar.x";
    String url2= "https://sample2.com/foo/bar.x";
    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=\"//sample2.com/foo/bar.x\">link1</a>"+
      "<base href=https://www.example.com>"+
      "<a href=\"//sample2.com/foo/bar.x\">link1</a>";

    extractor.extractUrls(mau, new StringInputStream(source), ENC,
			  "http://www.example.com/blah/", cb);

    assertEquals(SetUtil.set(url1, url2), cb.getFoundUrls());
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

  private class MyLinkExtractorCallback implements LinkExtractor.Callback {
    Set foundUrls = new HashSet();

    public void foundLink(String url) {
      foundUrls.add(url);
    }

    public Set getFoundUrls() {
      return foundUrls;
    }

    public void reset() {
      foundUrls = new HashSet();
    }
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestGoslingHtmlLinkExtractor.class.getName()};
    junit.textui.TestRunner.main(testCaseList);
  }
}
