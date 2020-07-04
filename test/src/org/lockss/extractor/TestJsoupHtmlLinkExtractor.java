/*
 * $Id$
 */

/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.test.StringInputStream;
import org.lockss.util.*;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;
import java.util.Set;

public class TestJsoupHtmlLinkExtractor extends LockssTestCase {
  public static final String startUrl = "http://www.example.com/index.html";
  static String ENC = Constants.DEFAULT_ENCODING;
  private MockArchivalUnit m_mau;
  private JsoupHtmlLinkExtractor m_extractor;
  private MyLinkExtractorCallback m_callback;
  private static final String HTTP = "http";
  private static final String HEADER = "header";
  private static final String CONTENT = "content";
  private static final String END_OF_INPUT = "\\Z";
  private static final String NEWLINE = System.getProperty("line.separator");

  public void setUp() throws Exception {
    super.setUp();
    m_mau = new org.lockss.test.MockArchivalUnit();
    m_extractor = new JsoupHtmlLinkExtractor(false, true, null, null);
    m_callback = new MyLinkExtractorCallback();
  }

  public void testExtractUrls() throws Exception {

  }

  public void testRegisterTagExtractor() throws Exception {

  }

  // XXX Disabled.  Needs assertions and not to fetch from network
  public void xxxtestCharsetChange() throws Exception {
    String test = "http://www.pensoft.net/journals/neobiota/issue/11/";
    URL url = new URL(test);
    MyLinkExtractorCallback callback = new MyLinkExtractorCallback();
    String[] expectedResolvedIssues = {
      "http://www.pensoft.net/journals/neobiota/issue/12/",
      "http://www.pensoft.net/journals/neobiota/issue/13/",
      "http://www.pensoft.net/journals/neobiota/issue/14/",
      "http://www.pensoft.net/journals/neobiota/issue/15/"
    };
    java.util.List<String> issues = ListUtil.fromArray(expectedResolvedIssues);

    m_extractor.extractUrls(m_mau, url.openStream(), ENC, test, callback);
    Set<String> urls = callback.getFoundUrls();
    for(String a_url : urls)
    {
      if(a_url.contains("article")) {
        try {
          UrlUtil.openInputStream(a_url);
        }
        catch(java.io.IOException ioe) {
          fail("crawl of url: " + a_url + ":"+ ioe.getMessage());
        }
      }
    }
  }

  public void testThrowsOnNullInputStream() throws Exception {
    try {
      m_extractor.extractUrls(m_mau, null, ENC, "http://www.example.com/",
                              new MyLinkExtractorCallback());
      fail("Calling extractUrls with a null InputStream should have thrown");
    }
    catch(IllegalArgumentException iae) {
    }
  }

  public void testThrowsOnNullSourceUrl() throws Exception {
    StringInputStream sis = null;
    try {
      sis = new StringInputStream("Blah");
      m_extractor.extractUrls(m_mau, sis, ENC, null,
                              new MyLinkExtractorCallback());
      fail("Calling extractUrls with a null CachedUrl should have thrown");
    }
    catch(IllegalArgumentException iae) {

    }
    finally {
      if(sis != null) {
        sis.close();
      }
    }
  }

  public void testThrowsOnNullCallback() throws Exception {
    StringInputStream sis = null;
    try {
      sis = new StringInputStream("blah");
      m_extractor.extractUrls(m_mau, sis, ENC, "http://www.example.com/",
                              null);
      fail("Calling extractUrls with a null callback should have thrown");
    }
    catch(IllegalArgumentException iae) {

    }
    finally {
      if(sis != null) {
        sis.close();
      }
    }
  }

  public void testParsesHref() throws Exception {
    singleTagShouldParse("http://www.example.com/web_link.html",
                         "<a href=", "</a>");
  }

  public void testParsesHrefWithTab() throws Exception {
    singleTagShouldParse("http://www.example.com/web_link.html",
                         "<a\thref=", "</a>");
  }

  public void testParsesHrefWithCarriageReturn() throws Exception {
    singleTagShouldParse("http://www.example.com/web_link.html",
                         "<a\rhref=", "</a>");
  }

  public void testParsesHrefWithNewLine() throws Exception {
    singleTagShouldParse("http://www.example.com/web_link.html",
                         "<a\nhref=", "</a>");
  }

  public void testParsesImage() throws Exception {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
                         "<img src=", "</img>");
    // test parssing the tag with attriutes before the link
    singleTagShouldParse("http://www.example.com/web_link.jpg",
                         "<img\nwidth='280' hight='90' src=", "</img>");
  }

  public void testParsesImageData() throws Exception {
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
    mau.setLinkExtractor("text/html", new JsoupHtmlLinkExtractor(false,false,
        null, null));
    MockCachedUrl mcu = new MockCachedUrl("http://www.example.com", mau);
    mcu.setContent(source);
    m_callback.reset();
    m_extractor.extractUrls(mau, new StringInputStream(source), ENC,
        "http://www.example.com", m_callback);
    assertEquals(SetUtil.set(url), m_callback.getFoundUrls());
  }

  public void testParsesEmbed() throws Exception {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
                         "<embed src=", "</embed>");
  }

  public void testParsesApplet() throws Exception {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
                         "<applet code=", "</applet>");
  }

  public void testParsesArea() throws Exception {
    singleTagShouldParse("http://www.example.com/web_link.shtml",
                         "<area href=", "</area>");
    singleTagShouldParse("http://www.example.com/web_link.shtml",
                         "<area shape='rect' coords='279,481,487' href=",
                         "</area>");
  }

  public void testParsesObject() throws Exception {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
                         "<object codebase=", "</object>");
  }

  public void testParsesOptionPositive() throws Exception {
    TypedEntryMap pMap = new TypedEntryMap();
    pMap.setMapElement("html-parser-select-attrs", ListUtil.list("value"));
    m_mau.setPropertyMap(pMap);
    singleTagShouldNotParse("http://www.example.com/web_link.jpg",
                            "<option  value=", "</option>", m_mau);
    singleTagShouldNotParse("http://www.example.com/web_link.jpg",
                            "<option a=b value=", "</option>", m_mau);
  }

  public void testParsesOptionNegative() throws Exception {
    singleTagShouldNotParse("http://www.example.com/web_link.jpg",
                            "<option  value=", "</option>");
    singleTagShouldNotParse("http://www.example.com/web_link.jpg",
                            "<option a=b value=", "</option>");
  }

  public void testDoCrawlImageWithSrcInAltTag() throws Exception {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
                         "<img alt=src src=", "</img>");
    singleTagShouldParse("http://www.example.com/web_link.jpg",
                         "<img alt = src src=", "</img>");
  }

  public void testDoCrawlImageWithSrcInAltTagAfterSrcProper()
    throws Exception {
    String url = "http://www.example.com/link3.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<img src=" + url + " alt=src>link3</a>";

    MockCachedUrl mcu = new MockCachedUrl(startUrl);
    mcu.setContent(source);

    m_extractor.extractUrls(m_mau, new StringInputStream(source), ENC,
                            startUrl, m_callback);

    java.util.Set<String> expected = new java.util.HashSet<String>();
    expected.add(url);
    assertEquals(expected, m_callback.getFoundUrls());
  }

  public void testDoCrawlFrame() throws Exception {
    String url1 = "http://www.example.com/menu.html";
    String url2 = "http://www.example.com/content.html";
    String source = "<html><head></head><frameset>" +
      "<frame src=\""+ url1 + "\"></frame>" +
      "<frame src=\""+ url2 +"\"></frameset></html>";

    assertIsomorphic(SetUtil.set(url1, url2), parseSingleSource(source));

  }

  public void testAnchorTagWithClass() throws Exception {
    String source =
      "<html><head><title>Test Title</title></head><body>" +
      "<div class=\"holder\">" +
      "<a title=\"Open Figure Viewer\" onclick=\"showFigures(this,event); " +
      "return false;\" href=\"JavaScript:void(0);\" class=\"thumbnail\">" +
      "</a>" +
      "</div></body></html>";
    assertEmpty(parseSingleSource(source));
  }

  public void testDoCrawlLink() throws Exception {
    singleTagShouldParse("http://www.example.com/web_link.css",
                         "<link href=", "</link>");
    singleTagShouldParse(
                          "http://www.example.com/web_link.css",
                          "<link rel=\"stylesheet\" type=\"text/css\" media=\"screen\"  href=",
                          "</link>");
  }

  public void testDoCrawlStyleAbsolute() throws Exception {
    performDoCrawlStyle("<style>", "http://www.example.com/",
                        "http://www.example.com/");
  }

  public void testDoCrawlStyleRelative() throws Exception {
    performDoCrawlStyle("<style>", "", "http://www.example.com/");
  }

  public void testDoCrawlStyleWithTypeAttributeAbsolute() throws Exception {
    performDoCrawlStyle("<style type=\"text/css\">",
                        "http://www.example.com/", "http://www.example.com/");
  }

  public void testDoCrawlStyleWithTypeAttributeRelative() throws Exception {
    performDoCrawlStyle("<style type=\"text/css\">", "",
                        "http://www.example.com/");
  }

  public void testDoCrawlStyleAbsoluteShort() throws Exception {
    performDoCrawlStyleShort("<style>", "http://www.example.com/",
                             "http://www.example.com/");
  }


  protected void performDoCrawlStyle(String openingStyleTag,
                                     String givenPrefix, String expectedPrefix)
    throws Exception {
    String url1 = "foo1.css";
    String url2 = "foo2.css";
    String url3 = "foo3.css";
    String url4 = "foo4.css";
    String url5 = "img5.gif";
    String url6 = "img6.gif";

    String source = "<html>\n" + " <head>\n" + "  <title>Test</title>\n"
      + "  "
      + openingStyleTag
      + "\n"
      + "<!--\n"
      + "@import url(\'"
      + givenPrefix
      + url1
      + "\');\n"
      + "@import url(\""
      + givenPrefix
      + url2
      + "\");\n"
      + "@import \'"
      + givenPrefix
      + url3
      + "\';\n"
      + "@import \""
      + givenPrefix
      + url4
      + "\";\n"
      + "foo {\n"
      + " bar: url(\'"
      + givenPrefix
      + url5
      + "\');\n"
      + " baz: url(\""
      + givenPrefix
      + url6
      + "\");\n"
      + "}\n"
      + "/* Comment */"
      + "-->\n"
      + "  </style>\n"
      + " </head>\n"
      + " <body>\n"
      + "  <p>Fake content</p>\n"
      + " </body>\n"
      + "</html>\n";

    assertEquals(SetUtil.set(expectedPrefix + url1, expectedPrefix + url2,
                             expectedPrefix + url3, expectedPrefix + url4,
                             expectedPrefix + url5, expectedPrefix + url6),
                    parseSingleSource(source));
  }

  // style attr is conditioonal on "url(" in string; ensure <style> tag isn't.
  protected void performDoCrawlStyleShort(String openingStyleTag,
					  String givenPrefix,
					  String expectedPrefix)
      throws Exception {
    String url1 = "foo1.css";
    String url2 = "foo2.css";
    String url3 = "foo3333.css";
    String url4 = "foo4.css";
    String url5 = "img5.gif";
    String url6 = "img6.gif";

    String source = "<html>\n" + " <head>\n" + "  <title>Test</title>\n"
      + "  "
      + openingStyleTag
      + "\n"
      + "<!--\n"
      + "@import \'"
      + givenPrefix
      + url3
      + "\';\n"
      + "}\n"
      + "/* Comment */"
      + "-->\n"
      + "  </style>\n"
      + " </head>\n"
      + " <body>\n"
      + "  <p>Fake content</p>\n"
      + " </body>\n"
      + "</html>\n";

    assertEquals(SetUtil.set(expectedPrefix + url3),
		 parseSingleSource(source));
  }

  public void testDoCrawlBody() throws Exception {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
                            "<body background=", "</body>");
  }

  /**
   * @see <a
   *      href="http://msdn.microsoft.com/workshop/author/dhtml/reference/properties/background_2.asp">
   *      Microsoft extension: <code>background</code> attribute for
   *      <code>table</code>, <code>td</code> and <code>th</code></a>
   */
  public void testDoCrawlTable() throws Exception {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
                         "<table background=", "</table>");
  }

  /**
   * @see <a
   *      href="http://msdn.microsoft.com/workshop/author/dhtml/reference/properties/background_2.asp">
   *      Microsoft extension: <code>background</code> attribute for
   *      <code>table</code>, <code>td</code> and <code>th</code></a>
   */
  public void testDoCrawlTd() throws Exception {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
                         "<table> <td background=", "</td></table>");
  }

  /**
   * @see <a
   *      href="http://msdn.microsoft.com/workshop/author/dhtml/reference/properties/background_2.asp">
   *      Microsoft extension: <code>background</code> attribute for
   *      <code>table</code>, <code>td</code> and <code>th</code></a>
   */
  public void testDoCrawlTh() throws Exception {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
                         "<table><th background=", "</th></table>");
  }

  public void testDoCrawlScript() throws Exception {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
                         "<script src=", "</script>");
  }

  public void testDoCrawlWithEqualsInUrl() throws Exception {
    singleTagShouldParse(
      "http://www.example.com/acs/a/toc.select?in_coden=jcisd8&in_volume=43",
      "<a href=", "</a>");
  }

  public void testDoCrawlWithLineBreakBeforeTag() throws Exception {
    singleTagShouldParse("http://www.example.com/web_link.html",
                         "<a\nhref=", "</a");
  }

  private void singleTagShouldParse(String url, String startTag, String endTag)
    throws Exception {
    singleTagShouldParse(url, startTag, endTag, null);
  }

  private void singleTagShouldParse(String url, String startTag,
                                    String endTag, ArchivalUnit au) throws Exception {
    singleTagParse(url, startTag, endTag, au, true);
  }

  private void singleTagShouldNotParse(String url, String startTag,
                                       String endTag) throws Exception {
    singleTagShouldNotParse(url, startTag, endTag, null);
  }

  private void singleTagShouldNotParse(String url, String startTag,
                                       String endTag, ArchivalUnit au) throws Exception {
    singleTagParse(url, startTag, endTag, au, false);
  }

  private void singleTagParse(String url, String startTag, String endTag,
                              ArchivalUnit au, boolean shouldParse) throws Exception {
    MockCachedUrl mcu = new MockCachedUrl("http://www.example.com");
    String content = makeContent(url, startTag, endTag);
    mcu.setContent(content);

    MyLinkExtractorCallback m_callback = new MyLinkExtractorCallback();
    m_extractor.extractUrls(m_mau, new StringInputStream(content), ENC,
                            "http://www.example.com", m_callback);

    if(shouldParse) {
      java.util.Set<String> expected = new java.util.HashSet<String>();
      expected.add(url);
      assertEquals("Misparsed: " + content, expected,
                   m_callback.getFoundUrls());
    }
    else {
      java.util.Set<String> expected = new java.util.HashSet<String>();
      assertEquals("Misparsed: " + content, expected,
                   m_callback.getFoundUrls());
    }
  }

  public void testDoNotCrawlBadA() throws Exception {
    String[] badTags = {"<a harf=", "<a hre=", "<a hrefe=", "<al href="};
    checkBadTags(badTags, "</a>");
  }

  public void testDoNotCrawlBadFrameTag() throws Exception {
    String[] badTags = {"<fram src=", "<framea src=", "<framr src=",
                        "<frame sr=", "<frame srcr=", "<frame sra="};
    checkBadTags(badTags, "</frame>");
  }

  public void testDoNotCrawlBadImgTag() throws Exception {
    String[] badTags = {"<im src=", "<imga src=", "<ime src=", "<img sr=",
                        "<img srcr=", "<img sra="};
    checkBadTags(badTags, "</frame>");
  }

  public void testDoNotCrawlBadLinkTag() throws Exception {
    String[] badTags = {"<lin href=", "<linkf href=", "<lino href=",
                        "<link hre=", "<link hrefr=", "<link hrep="};
    checkBadTags(badTags, "</link>");
  }

  public void testDoNotCrawlBadBodyTag() throws Exception {
    String[] badTags = {"<bod background=", "<bodyk background=",
                        "<bodp background=", "<body backgroun=",
                        "<body backgrounyl=", "<body backgrounj="};
    checkBadTags(badTags, "</body>");
  }

  public void testDoNotCrawlBadScriptTag() throws Exception {
    String[] badTags = {"<scrip src=", "<scriptl src=", "<scripo src=",
                        "<script sr=", "<script srcu=", "<script srp="};
    checkBadTags(badTags, "</script>");
  }

  public void testDoNotCrawlBadTableTag() throws Exception {
    String[] badTags = {"<tabl background=", "<tablea background=",
                        "<tablu background=", "<table backgroun=",
                        "<table backgroundl=", "<table backgrouno="};
    checkBadTags(badTags, "</table>");
  }

  public void testDoNotCrawlBadTdTag() throws Exception {
    String[] badTags = {"<t background=", "<tdl background=",
                        "<ta background=", "<td backgroun=", "<td backgroundl=",
                        "<td backgrouno="};
    checkBadTags(badTags, "</td>");
  }

  public void testDoNotCrawlBadThTag() throws Exception {
    String[] badTags = {"<t background=", "<thl background=",
                        "<ta background=", "<th backgroun=", "<th backgroundl=",
                        "<th backgrouno="};
    checkBadTags(badTags, "</th>");
  }

  public void testEmptyAttribute() throws Exception {
    String source = "<html><head><title>Test</title></head><body>"
      + "<a href=>link3</a>";
    assertEquals(SetUtil.set(), parseSingleSource(source));
  }

  public void testStyleAttribute() throws Exception {
    String url1 = "http://www.example.com/link3.html";
    String url2 = "http://www.example.com/backg.png";

    String source = "<html><head><title>Test</title></head><body>"
      + "<span class=\"foo\" "
      + "style=\"background: url('/backg.png') no-repeat 0px -64px;\" />";
    assertEquals(SetUtil.set(url2), parseSingleSource(source));

    String source2 = "<html><head><title>Test</title></head><body>"
      + "<a href=\"http://www.example.com/link3.html\" " 
      + "style=\"background: url('/backg.png');\">link3</a>";
    assertEquals(SetUtil.set(url1, url2), parseSingleSource(source2));
  }

  public void testParseUnknownProtocol() throws Exception {
    String url = "badprotocol://www.example.com/link3.html";
    String source = "<html><head><title>Test</title></head><body>"
      + "<a href=\"" + url + "\">link3</a>";
    assertEmpty(parseSingleSource(source));
  }

  public void testParsesFileWithQuotedUrls() throws Exception {
    String url = "http://www.example.com/link3.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<a href=\"http://www.example.com/link3.html\">link3</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testDontParseJSByDefault() throws Exception {
    String url3 = "http://www.example.com/link1.html";

    String source =
      "<html><head><title>Test</title></head><body>" +
        "<a href = javascript:newWindow('http://www.example.com/link3.html')</a>"
        + "<a href = javascript:popup('http://www.example.com/link2.html')</a>"
        + "<img src = javascript:popup('" + url3 + "') </img></body></html>";
    assertEquals(SetUtil.set(), parseSingleSource(source));
  }

  public void testDontParseMailto() throws Exception {
    String source =
      "<html><head><title>Test</title></head><body>" +
        "<a href = mailto:user@example.com</a>";
    assertEquals(SetUtil.set(), parseSingleSource(source));
  }
  public void testParseVideoTags() throws Exception {
    String url1="http://www.example.com/forrest_gump.mp4";
    String url2="http://www.example.com/forrest_gump.ogg";
    String url3="http://www.example.com/subtitles_en.vtt";
    String url4="http://www.example.com/subtitles_no.vtt";

    String source = "<video width=\"320\" height=\"240\" controls>\n"
        + "  <source src=\"forrest_gump.mp4\" type=\"video/mp4\">\n"
        + "  <source src=\"forrest_gump.ogg\" type=\"video/ogg\">\n"
        + "  <track src=\"subtitles_en.vtt\" kind=\"subtitles\" srclang=\"en\" label=\"English\">\n"
        + "  <track src=\"subtitles_no.vtt\" kind=\"subtitles\" srclang=\"no\" label=\"Norwegian\">\n"
        + "</video>";
    assertEquals(SetUtil.set(url1,url2,url3, url4), parseSingleSource(source));

  }
  /**
   * Included to test a chunk of HighWire HTML that we're not parsing
   * correctly
   */
  public void testParseHWPDF() throws Exception {
    // Properties p = new Properties();
    // p.setProperty(GoslingHtmlLinkExtractor.PARAM_PARSE_JS, "true");
    // ConfigurationUtil.setCurrentConfigFromProps(p);
    // m_extractor = new GoslingHtmlLinkExtractor();

    String url = "http://www.example.com/cgi/reprint/21/1/2.pdf";

    String source = "<table cellspacing=\"0\" cellpadding=\"10\" width=\"250\" border=\"0\">"
      + "<tr><td align=center bgcolor=\"#DBDBDB\">\n\n	"
      + "<font face=\"verdana,arial,helvetica,sans-serif\">"
      + "<strong><font size=+1>Automatic download</font><br>\n	"
      + "<font size=\"-1\">[<a target=\"_self\" href=\"/cgi/reprint/21/1/2.pdf\" "
      + "onclick=\"cancelLoadPDF()\">Begin manual download</a>]</strong></font>\n";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  // public void testDoHrefInAnchorJavascript() throws IOException {
  // String url= "http://www.example.com/link3.html";
  // String url2 = "http://www.example.com/link2.html";
  // String url3 = "http://www.example.com/link1.html";

  // String source =
  // "<html><head><title>Test</title></head><body>"+
  // "<a href = javascript:newWindow('http://www.example.com/link3.html')</a>"
  // + "<a href = javascript:popup('http://www.example.com/link2.html')</a>"
  // + "<img src = javascript:popup('" + url3 + "') </img>";
  // assertEquals(SetUtil.set(url, url2), parseSingleSource(source));
  // }

  // public void testNormalizeHash() throws MalformedURLException {
  // assertEquals("http://www.bioone.org/bioone/?request=get-toc&issn=0044-7447&volume=32&issue=1",
  // UrlUtil.normalizeUrl("http://www.bioone.org/bioone/?request=get-toc&#38;issn=0044-7447&#38;volume=32&issue=1"));
  // }

  public void testResolvesHtmlEntities() throws Exception {
    String url1 = "http://www.example.com/bioone/?"
      + "request=get-toc&issn=0044-7447&volume=32&issue=1";

    String source = "<html><head><title>Test</title></head><body>"
      + "<a href=http://www.example.com/bioone/?"
      + "request=get-toc&#38;issn=0044-7447&#38;volume=32&issue=1>link1</a>";
    assertEquals(SetUtil.set(url1), parseSingleSource(source));

    // ensure character entities processed before rel url resolution
    source = "<html><head><title>Test</title></head><body>"
      + "<base href=http://www.example.com/foo/bar>"
      + "<a href=&#46&#46/xxx>link1</a>";
    assertEquals(SetUtil.set("http://www.example.com/xxx"),
                 parseSingleSource(source));
  }

  public void testInterpretsBaseTag() throws Exception {
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html";
    String url3 = "http://www.example.com/link3.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<base href=http://www.example.com>"
      + "<a href=link1.html>link1</a>"
      + "Filler, with <b>bold</b> tags and<i>others</i>"
      + "<base href=http://www.example2.com>"
      + "<a href=link2.html>link2</a>"
      + "<base href=http://www.example3.com>"
      + "<a href=link3.html>link3</a>";
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
    String url0 = "http://www.example222.com/link0.html";
    String url1 = "http://www.example222.com/link1.html";
    String url2 = "http://www.example222.com/link2.html";
    String url3 = "http://www.example222.com/link3.html";

    String source = "<html><head><title>Test</title></head><body>"
      // <base> applies to whole document, including links before it
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
    String url0 = "http://www.example.com/foo/link0.html";
    String url1 = "http://www.example.com/foo/link1.html";
    String url2 = "http://www.example.com/foo/link2.html";
    String url3 = "http://www.example.com/foo/link3.html";

    String source = "<html><head><title>Test</title></head><body>"
      // <base> applies to whole document, including links before it
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
    String url0 = "http://www.example.com/link0.html";
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html";
    String url3 = "http://www.example.com/link3.html";

    String source = "<html><head><title>Test</title></head><body>"
      // <base> applies to whole document, including links before it
      + "<a href=link0.html>link1</a>"
      + "<base href=/>"
      + "<a href=link1.html>link1</a>"
      + "Filler, with <b>bold</b> tags and<i>others</i>"
      + "<a href=link2.html>link2</a>"
      + "<a href=link3.html>link3</a>";
    assertEquals(SetUtil.set(url0, url1, url2, url3),
		 parseSingleSource("http://www.example.com/x/y/", source));
  }

  // Relative URLs before a malforned base tag should be extracted, as well
  // as any absolute URLs after the malformed base tag
  // set
  public void testInterpretsMalformedBaseTag() throws Exception {
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html";
    String url3 = "http://www.example2.com/link3.html";
    String url4 = "http://www.example.com/link3.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<base href=http://www.example.com>"
      + "<a href=link1.html>link1</a>"
      + "Filler, with <b>bold</b> tags and<i>others</i>"
      + "<base href=javascript:www.example2.com>"
      + "<a href=link2.html>link2</a>"
      + "<base href=www.example.com>"
      + "<a href=http://www.example2.com/link3.html>link3</a>"
      + "<base href=http://www.example3.com>"
      + "<a href=link3.html>link4</a>";
    assertIsomorphic(SetUtil.set(url1, url2, url3, url4),
                        parseSingleSource(source));
  }

  public void testIgnoresNullHrefInBaseTag() throws Exception {
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html";
    String url3 = "http://www.example.com/link3.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<a href=link1.html>link1</a>"
      + "Filler, with <b>bold</b> tags and<i>others</i>"
      + "<base blah=blah>" + "<a href=link2.html>link2</a>"
      + "<a href=link3.html>link3</a>";
    assertEquals(SetUtil.set(url1, url2, url3), parseSingleSource(source));
  }

  public void testIgnoresEmptyHrefInBaseTag() throws Exception {
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html";
    String url3 = "http://www.example.com/link3.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<a href=link1.html>link1</a>"
      + "Filler, with <b>bold</b> tags and<i>others</i>"
      + "<base href=\"\" blah=blah>" + "<a href=link2.html>link2</a>"
      + "<a href=link3.html>link3</a>";
    assertEquals(SetUtil.set(url1, url2, url3), parseSingleSource(source));
  }

  public void testSkipsComments() throws Exception {
    String url = "http://www.example.com/link3.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<!--<a href=http://www.example.com/link1.html>link1</a>"
      + "Filler, with <b>bold</b> tags and<i>others</i>"
      + "<a href=http://www.example.com/link2.html>link2</a>-->"
      + "<a href=http://www.example.com/link3.html>link3</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testSkipsMalformedComments() throws Exception {
    //NOTE(vibhor): The test html doesn't evaluate to a valid html. Thus it
    //isn't valid in case of html parser.

    String url = "http://www.example.com/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>" +
        "<!--<a href=http://www.example.com/link1.html>link1</a>" +
        "Filler, with <b>bold</b> tags and<i>others</i>" +
        "<a href=http://www.example.com/link2.html>link2</a>--!>" +
        "<a href=http://www.example.com/link3.html>link3</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testSkipsScriptTags() throws Exception {
    String url = "http://www.example.com/link3.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<script>"
      + "<a href=http://www.example.com/link1.html>link1</a>"
      + "Filler, with <b>bold</b> tags and<i>others</i>"
      + "<a href=http://www.example.com/link2.html>link2</a>"
      + "</script>"
      + "<a href=http://www.example.com/link3.html>link3</a></body></html>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testSkipsScriptTagsAllTheWay() throws Exception {
    String url = "http://www.example.com/link3.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<script>"
      + "<a href=http://www.example.com/link1.html>link1</a>"
      + "Filler, with <b>bold</b> tags and<i>others</i>"
      + "<a href=http://www.example.com/link2.html" + "</script>"
      + "<a href=http://www.example.com/link3.html>link3</a></body></html>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  private void doScriptSkipTest(String openScript, String closeScript)
    throws Exception {
    doScriptSkipTest(openScript, closeScript, null);
  }

  private void doScriptSkipTest(String openScript, String closeScript,
                                String failMsg) throws Exception {
    String url = "http://www.example.com/link3.html";
    String src = "<html><head><title>Test</title></head><body>"
      + openScript
      + "<a href=http://www.example.com/link1.html>link1</a>"
      + "Filler, with <b>bold</b> tags and<i>others</i>"
      + "<a href=http://www.example.com/link2.html>link2</a>"
      + closeScript
      + "<a href=http://www.example.com/link3.html>link3</a>";
    assertEquals(failMsg, SetUtil.set(url), parseSingleSource(src));
  }

  public void testSkipsScriptTagsIgnoreCase() throws Exception {
    doScriptSkipTest("<ScRipt>", "</sCripT>");
  }

  public void testKeepsSpaceInUrl() throws Exception {
    String url = "http://www.example.com/link%20with%20space.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<a href=\"http://www.example.com/link with space.html\">Link</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testIgnoresNewLineInUrl() throws Exception {
    String url = "http://www.example.com/linkwithspace.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<a href=\"http://www.example.com/link\nwith\nspace.html\">Link</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testIgnoresNewLineInField() throws Exception {
    String url = "http://www.example.com/link.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<img\nsrc=\"http://www.example.com/link.html\">Link</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testIgnoresCRInUrl() throws Exception {
    String url = "http://www.example.com/linkwithspace.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<a href=\"http://www.example.com/link\rwith\rspace.html\">Link</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testKeepsDoubleQuoteInUrl() throws Exception {
    String url = "http://www.example.com/link%22with%22quotes.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<a href='http://www.example.com/link\"with\"quotes.html'>Link</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testKeepsSingleQuoteInUrl() throws Exception {
    String url = "http://www.example.com/link'with'quotes.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<a href=\"" + url + "\">Link</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testMultipleLinks() throws Exception {
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html";
    String url3 = "http://www.example.com/link3.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<a href=" + url1 + ">link1</a>"
      + "Filler, with <b>bold</b> tags and<i>others</i>" + "<a href="
      + url2 + ">link2</a>" + "<a href=" + url3 + ">link3</a>";

    assertEquals(SetUtil.set(url1, url2, url3), parseSingleSource(source));
  }

  public void testRelativeLinksLocationTagsAndMultipleKeys()
    throws Exception {
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html#ref";
    String url3 = "http://www.example.com/dir/link3.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<a href=link1.html>link1</a>"
      + "Filler, with <b>bold</b> tags and<i>others</i>"
      + "<a blah1=blah href=link2.html#ref blah2=blah>link2</a>"
      + "<a href=dir/link3.html>link3</a>";

    assertEquals(SetUtil.set(url1, url2, url3), parseSingleSource(source));
  }

  public void testHttpEquiv() throws Exception {
    String url1 = "http://example.com/blah.html";
    String source =
      "<html><head>"
        + "<meta http-equiv=\"refresh\" "
        + "content=\"0; url=http://example.com/blah.html\">"
        + "</head></html>";

    assertEquals(SetUtil.set(url1), parseSingleSource(source));

    source =
      "<html><head>"
        + "<meta http-equiv=\"refresh\" "
        + "content=\"0;url=http://example.com/blah.html\">"
        + "</head></html>";

    assertEquals(SetUtil.set(url1), parseSingleSource(source));
  }

  // tests that we are only parsing out the URL when the
  // http-equiv header is "refresh"
  public void testHttpEquiv2() throws Exception {
    String source = "<html><head>" + "<meta http-equiv=\"blah\" "
      + "content=\"0; url=http://example.com/blah.html\">"
      + "</head></html>";

    assertEquals(SetUtil.set(), parseSingleSource(source));
  }

  private java.util.Set<String> parseSingleSource(String source)
      throws Exception {
    return parseSingleSource("http://www.example.com", source);
  }

  private java.util.Set<String> parseSingleSource(String docPath, String source)
      throws Exception {
    MockArchivalUnit m_mau = new MockArchivalUnit();
    LinkExtractor ue = new RegexpCssLinkExtractor();
    m_mau.setLinkExtractor("text/css", ue);
    MockCachedUrl mcu = new MockCachedUrl(docPath, m_mau);
    mcu.setContent(source);

    m_callback.reset();
    m_extractor.extractUrls(m_mau, new StringInputStream(source), ENC,
                            docPath, m_callback);
    return m_callback.getFoundUrls();
  }

  public void testRelativeLinksWithSameName() throws Exception {
    String url1 = "http://www.example.com/branch1/index.html";
    String url2 = "http://www.example.com/branch2/index.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<a href=branch1/index.html>link1</a>"
      + "Filler, with <b>bold</b> tags and<i>others</i>"
      + "<a href=branch2/index.html>link2</a>";

    MockCachedUrl mcu = new MockCachedUrl("http://www.example.com");
    mcu.setContent(source);

    m_extractor.extractUrls(m_mau, new StringInputStream(source), ENC,
                               "http://www.example.com", m_callback);

    java.util.Set<String> expected = new java.util.HashSet<String>();
    java.util.Collections.addAll(expected, url1, url2);
    assertEquals(expected, m_callback.getFoundUrls());
  }

  public void testRelativeLinksWithLeadingSlash() throws Exception {
    String url1 = "http://www.example.com/blah/branch1/index.html";
    String url2 = "http://www.example.com/blah/branch2/index.html";
    String url3 = "http://www.example.com/journals/american_imago/toc/aim60.1.html";
    String url4 = "http://www.example.com/css/foo.css";
    String url5 = "http://www.example.com/javascript/bar.js";
    String source = "<html><head><title>Test</title></head><body>"
      + "<a href= branch1/index.html>link1</a>"
      + "Filler, with <b>bold</b> tags and<i>others</i>"
      + "<a href=\" branch2/index.html\">link2</a>"
      + "<a href =\" /journals/american_imago/toc/aim60.1.html\">"
      + "<link rel=\"stylesheet\" href=\"/css/foo.css\" >"
      + "<script type=\"text/javascript\" src=\"/javascript/bar.js\"></script>"
      + "Number 1, Spring 2003</a>";

    MockCachedUrl mcu = new MockCachedUrl("http://www.example.com/blah/");
    mcu.setContent(source);

    m_extractor.extractUrls(m_mau, new StringInputStream(source), ENC,
                            "http://www.example.com/blah/", m_callback);

    java.util.Set<String> expected = new java.util.HashSet<String>();
    java.util.Collections.addAll(expected, url1, url2, url3, url4, url5);
    assertEquals(expected, m_callback.getFoundUrls());
  }

  public void testProtocolNeutralLinksHttp() throws Exception {
    String url1= "http://sample2.com/foo/bar.x";
    String url2= "http://sample3.com/bar/bar.y";
    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=\"//sample2.com/foo/bar.x\">link1</a>"+
      "<a href=\"//sample3.com/bar/bar.y\">link1</a>";

    m_extractor.extractUrls(m_mau, new StringInputStream(source), ENC,
			    "http://www.example.com/blah/", m_callback);
    assertEquals(SetUtil.set(url1, url2), m_callback.getFoundUrls());
  }

  public void testProtocolNeutralLinksHttps() throws Exception {
    String url1= "https://sample2.com/foo/bar.x";
    String url2= "https://sample3.com/bar/bar.y";
    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=\"//sample2.com/foo/bar.x\">link1</a>"+
      "<a href=\"//sample3.com/bar/bar.y\">link1</a>";

    m_extractor.extractUrls(m_mau, new StringInputStream(source), ENC,
			    "https://www.example.com/blah/", m_callback);
    assertEquals(SetUtil.set(url1, url2), m_callback.getFoundUrls());
  }

  private String getPageHeader(URL fURL) throws IOException{
    StringBuilder result = new StringBuilder();

    URLConnection connection = null;

    connection = fURL.openConnection();

    //not all headers come in key-value pairs - sometimes the key is
    //null or an empty String
    int headerIdx = 0;
    String headerKey = null;
    String headerValue = null;
    while ( (headerValue = connection.getHeaderField(headerIdx)) != null ) {
      headerKey = connection.getHeaderFieldKey(headerIdx);
      if (headerKey != null && headerKey.length()>0) {
        result.append(headerKey);
        result.append(" : ");
      }
      result.append(headerValue);
      result.append(NEWLINE);
      headerIdx++;
    }
    return result.toString();
  }

  private String getPageContent(URL fURL) throws IOException {
    String result = null;
    URLConnection connection = null;
    connection =  fURL.openConnection();
    Scanner scanner = new Scanner(connection.getInputStream());
    scanner.useDelimiter(END_OF_INPUT);
    result = scanner.next();
    return result;
  }

  private void checkBadTags(String[] badTags, String closeTag)
    throws Exception {
    String url = "http://www.example.com/web_link.html";
    for(final String badTag : badTags) {
      singleTagShouldNotParse(url, badTag, closeTag);
    }
  }

  private String makeContent(String url, String openTag, String closeTag) {
    StringBuilder sb = new StringBuilder(100);
    sb.append("<html><head><title>Test</title></head><body>");
    sb.append(openTag);
    sb.append(url);
    sb.append(">");
    sb.append(closeTag);
    sb.append("</body></html>");
    return sb.toString();
  }

  private static class MyLinkExtractorCallback implements
                                               LinkExtractor.Callback {
    java.util.Set<String> foundUrls = new java.util.HashSet<String>();

    public void foundLink(String url) {
      foundUrls.add(url);
    }

    public java.util.Set<String> getFoundUrls() {
      return foundUrls;
    }

    public void reset() {
      foundUrls = new java.util.HashSet<String>();
    }
  }
}
