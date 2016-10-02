/*
 * $Id$
 */

/*

Copyright (c) 2001-2014 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;

import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.plugin.*;

public class TestHtmlParserLinkExtractor extends LockssTestCase {
  public static final String startUrl = "http://www.example.com/index.html";
  static String ENC = Constants.DEFAULT_ENCODING;

  HtmlParserLinkExtractor m_extractor = null;
  MyLinkExtractorCallback cb = null;

  MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    mau = new MockArchivalUnit();
    m_extractor = new HtmlParserLinkExtractor();
    cb = new MyLinkExtractorCallback();
  }

  public void testThrowsOnNullInputStream() throws IOException {
    try {
      m_extractor.extractUrls(mau, null, ENC, "http://www.example.com/",
			      new MyLinkExtractorCallback());
      fail("Calling extractUrls with a null InputStream should have thrown");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testThrowsOnNullSourceUrl() throws IOException {
    StringInputStream sis = null;
    try {
      sis = new StringInputStream("Blah");
      m_extractor.extractUrls(mau, sis, ENC, null,
			      new MyLinkExtractorCallback());
      fail("Calling extractUrls with a null CachedUrl should have thrown");
    } catch (IllegalArgumentException iae) {

    } finally {
      if (sis != null)
	sis.close();
    }
  }

  public void testThrowsOnNullCallback() throws IOException {
    StringInputStream sis = null;
    try {
      sis = new StringInputStream("blah");
      m_extractor.extractUrls(mau, sis, ENC, "http://www.example.com/",
			      null);
      fail("Calling extractUrls with a null callback should have thrown");
    } catch (IllegalArgumentException iae) {

    } finally {
      if (sis != null)
	sis.close();
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
			 "<area shape='rect' coords='279,481,487' href=", "</area>");
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
    String url = "http://www.example.com/link3.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<img src=" + url + " alt=src>link3</a>";

    MockCachedUrl mcu = new MockCachedUrl(startUrl);
    mcu.setContent(source);

    m_extractor.extractUrls(mau, new StringInputStream(source), ENC,
			    startUrl, cb);

    Set<String> expected = new HashSet<String>();
    expected.add(url);
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
			 "<link rel=\"stylesheet\" type=\"text/css\" media=\"screen\"  href=",
			 "</link>");
  }

  public void testDoCrawlStyleAbsolute() throws IOException {
    performDoCrawlStyle("<style>", "http://www.example.com/",
			"http://www.example.com/");
  }

  public void testDoCrawlStyleRelative() throws IOException {
    performDoCrawlStyle("<style>", "", "http://www.example.com/");
  }

  public void testDoCrawlStyleWithTypeAttributeAbsolute() throws IOException {
    performDoCrawlStyle("<style type=\"text/css\">",
			"http://www.example.com/", "http://www.example.com/");
  }

  public void testDoCrawlStyleWithTypeAttributeRelative() throws IOException {
    performDoCrawlStyle("<style type=\"text/css\">", "",
			"http://www.example.com/");
  }

  public void testDoCrawlStyleAbsoluteShort() throws IOException {
    performDoCrawlStyleShort("<style>", "http://www.example.com/",
			     "http://www.example.com/");
  }


  // ensure that scanning continues after a nested parser throws an error
  // XXX Need to cause an IOException while reading CSS from the stream
  public void xxxtestDoCrawlStyleError() throws IOException {
    String url1 = "http://example.com/blah1.html";
    String url2 = "http://example.com/blah2.html";
    String url3 = "http://example.com/blah3.html";
    String source = "<html><head>" + "<style type=\"text/css\">\n"
      + "<!--\n" + "@import url(\'" + url1 + "\');\n" + // ensure css
      // parser
      // got
      // invoked
      "foo {bgcolor: #FFFF};" + // and that this causes an error
      "@import url(\'" + url2 + "\');\n" + // so that this one isn't
      // found
      "-->\n" + "  </style>\n" + "<a href=" + url3 + "></a>" + // and
      // this
      // one
      // is
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
			     expectedPrefix + url3, expectedPrefix + url4, expectedPrefix
			     + url5, expectedPrefix + url6),
		 parseSingleSource(source));
  }

  // style attr is conditioonal on "url(" in string; ensure <style> tag isn't.

  // This test is currently ineffective because HtmlParserLinkExtractor
  // also invokes GoslingHtmlLinkExtractor
  protected void performDoCrawlStyleShort(String openingStyleTag,
					  String givenPrefix,
					  String expectedPrefix)
      throws IOException {
    String url3 = "foo3.css";

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

  public void testDoCrawlBody() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
			 "<body background=", "</body>");
  }

  /**
   * @see <a
   *      href="http://msdn.microsoft.com/workshop/author/dhtml/reference/properties/background_2.asp">
   *      Microsoft extension: <code>background</code> attribute for
   *      <code>table</code>, <code>td</code> and <code>th</code></a>
   */
  public void testDoCrawlTable() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
			 "<table background=", "</table>");
  }

  /**
   * @see <a
   *      href="http://msdn.microsoft.com/workshop/author/dhtml/reference/properties/background_2.asp">
   *      Microsoft extension: <code>background</code> attribute for
   *      <code>table</code>, <code>td</code> and <code>th</code></a>
   */
  public void testDoCrawlTd() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.jpg",
			 "<td background=", "</td>");
  }

  /**
   * @see <a
   *      href="http://msdn.microsoft.com/workshop/author/dhtml/reference/properties/background_2.asp">
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
    singleTagShouldParse("http://www.example.com/acs/a/toc.select?in_coden=jcisd8&in_volume=43",
			 "<a href=", "</a>");
  }

  public void testDoCrawlWithLineBreakBeforeTag() throws IOException {
    singleTagShouldParse("http://www.example.com/web_link.html",
			 "<a\nhref=", "</a");
  }

  // XXX this looks like it was testing the incorrect behavior
  // public void testDoCrawlWithAmpInUrl() throws IOException {
  // singleTagShouldParse("http://www.example.com?pageid=pid&amp;parentid=parid&amp",
  // "<a href=", "</a");
  // }

  private void singleTagShouldParse(String url, String startTag, String endTag)
      throws IOException {
    singleTagShouldParse(url, startTag, endTag, null);
  }

  private void singleTagShouldParse(String url, String startTag,
				    String endTag, ArchivalUnit au)
      throws IOException {
    singleTagParse(url, startTag, endTag, au, true);
  }

  private void singleTagShouldNotParse(String url, String startTag,
				       String endTag) throws IOException {
    singleTagShouldNotParse(url, startTag, endTag, null);
  }

  private void singleTagShouldNotParse(String url, String startTag,
				       String endTag, ArchivalUnit au)
      throws IOException {
    singleTagParse(url, startTag, endTag, au, false);
  }

  private void singleTagParse(String url, String startTag, String endTag,
			      ArchivalUnit au, boolean shouldParse)
      throws IOException {
    MockCachedUrl mcu = new MockCachedUrl("http://www.example.com");
    String content = makeContent(url, startTag, endTag);
    mcu.setContent(content);

    MyLinkExtractorCallback cb = new MyLinkExtractorCallback();
    m_extractor.extractUrls(mau, new StringInputStream(content), ENC,
			    "http://www.example.com", cb);

    if (shouldParse) {
      Set<String> expected = new HashSet<String>();
      expected.add(url);
      assertEquals("Misparsed: " + content, expected, cb.getFoundUrls());
    } else {
      Set<String> expected = new HashSet<String>();
      assertEquals("Misparsed: " + content, expected, cb.getFoundUrls());
    }
  }

  public void testDoNotCrawlBadA() throws IOException {
    String[] badTags = { "<a harf=", "<a hre=", "<a hrefe=", "<al href=" };
    checkBadTags(badTags, "</a>");
  }

  public void testDoNotCrawlBadFrameTag() throws IOException {
    String[] badTags = { "<fram src=", "<framea src=", "<framr src=",
			 "<frame sr=", "<frame srcr=", "<frame sra=" };
    checkBadTags(badTags, "</frame>");
  }

  public void testDoNotCrawlBadImgTag() throws IOException {
    String[] badTags = { "<im src=", "<imga src=", "<ime src=", "<img sr=",
			 "<img srcr=", "<img sra=" };
    checkBadTags(badTags, "</frame>");
  }

  public void testDoNotCrawlBadLinkTag() throws IOException {
    String[] badTags = { "<lin href=", "<linkf href=", "<lino href=",
			 "<link hre=", "<link hrefr=", "<link hrep=" };
    checkBadTags(badTags, "</link>");
  }

  public void testDoNotCrawlBadBodyTag() throws IOException {
    String[] badTags = { "<bod background=", "<bodyk background=",
			 "<bodp background=", "<body backgroun=",
			 "<body backgrounyl=",
			 "<body backgrounj=" };
    checkBadTags(badTags, "</body>");
  }

  public void testDoNotCrawlBadScriptTag() throws IOException {
    String[] badTags = { "<scrip src=", "<scriptl src=", "<scripo src=",
			 "<script sr=", "<script srcu=", "<script srp=" };
    checkBadTags(badTags, "</script>");
  }

  public void testDoNotCrawlBadTableTag() throws IOException {
    String[] badTags = { "<tabl background=", "<tablea background=",
			 "<tablu background=", "<table backgroun=",
			 "<table backgroundl=", "<table backgrouno=" };
    checkBadTags(badTags, "</table>");
  }

  public void testDoNotCrawlBadTdTag() throws IOException {
    String[] badTags = { "<t background=", "<tdl background=",
			 "<ta background=", "<td backgroun=",
			 "<td backgroundl=",
			 "<td backgrouno=" };
    checkBadTags(badTags, "</td>");
  }

  public void testDoNotCrawlBadThTag() throws IOException {
    String[] badTags = { "<t background=", "<thl background=",
			 "<ta background=", "<th backgroun=",
			 "<th backgroundl=",
			 "<th backgrouno=" };
    checkBadTags(badTags, "</th>");
  }

  // Behavior currently depends on Java version, do disabled. Crawler
  // excludes everything but http anyway.
  public void donttestCollectsHttps() throws IOException {
    String url = "https://www.example.com/link3.html";
    String source = "<html><head><title>Test</title></head><body>"
      + "<a href=\"" + url + "\">link3</a>";
    // empty in 1.3, no https stream handler
    assertEmpty(parseSingleSource(source));
    // 1.4 has one
    // assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testEmptyAttribute() throws IOException {
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

  // Extractor does not return urls for unknown protocols.
  public void testParseUnknownProtocol() throws IOException {
    String url = "badprotocol://www.example.com/link3.html";
    String source = "<html><head><title>Test</title></head><body>"
      + "<a href=\"" + url + "\">link3</a>";
    assertEmpty(parseSingleSource(source));
  }

  public void testParsesFileWithQuotedUrls() throws IOException {
    String url = "http://www.example.com/link3.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<a href=\"http://www.example.com/link3.html\">link3</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  // public void testDontParseJSByDefault() throws IOException {
  // String url3 = "http://www.example.com/link1.html";
  //
  // String source =
  // "<html><head><title>Test</title></head><body>"+
  // "<a href = javascript:newWindow('http://www.example.com/link3.html')</a>"
  // + "<a href = javascript:popup('http://www.example.com/link2.html')</a>"
  // + "<img src = javascript:popup('" + url3 + "') </img>";
  // assertEquals(SetUtil.set(), parseSingleSource(source));
  // }

  // public void testDontParseMailto() throws IOException {
  // String source =
  // "<html><head><title>Test</title></head><body>"+
  // "<a href = mailto:user@example.com</a>";
  // assertEquals(SetUtil.set(), parseSingleSource(source));
  // }

  /**
   * Included to test a chunk of HighWire HTML that we're not parsing
   * correctly
   */
  public void testParseHWPDF() throws IOException {
    // Properties p = new Properties();
    // p.setProperty(GoslingHtmlLinkExtractor.PARAM_PARSE_JS, "true");
    // ConfigurationUtil.setCurrentConfigFromProps(p);
    // extractor = new GoslingHtmlLinkExtractor();

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

  public void testResolvesHtmlEntities() throws IOException {
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

  public void testInterpretsBaseTag() throws IOException {
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html";
    String url3 = "http://www.example.com/link3.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<base href=http://www.example.com>"
      + "<a href=link1.html>link1</a>"
      + "Filler, with <b>bold</b> tags and<i>others</i>"
      + "<base href=http://www.example2.com>"
      + "<a href=link2.html>link2</a>"
      + "<base href=http://www.example.com>"
      + "<a href=link3.html>link3</a>";
    assertIsomorphic(SetUtil.set(url1, url2, url3), parseSingleSource(source));
  }

  // Relative URLs before a malforned base tag should be extracted, as well
  // as any absolute URLs after the malformed base tag
  // TODO This test looks like it should find url2, but it isn't in the result
  // set
  public void testInterpretsMalformedBaseTag() throws IOException {
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

  public void testIgnoresNullHrefInBaseTag() throws IOException {
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

  public void testIgnoresEmptyHrefInBaseTag() throws IOException {
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

  public void testSkipsComments() throws IOException {
    String url = "http://www.example.com/link3.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<!--<a href=http://www.example.com/link1.html>link1</a>"
      + "Filler, with <b>bold</b> tags and<i>others</i>"
      + "<a href=http://www.example.com/link2.html>link2</a>-->"
      + "<a href=http://www.example.com/link3.html>link3</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testSkipsMalformedComments() throws IOException {
    // NOTE(vibhor): The test html doesn't evaluate to a valid html. Thus it
    // isn't valid in case of html parser.

    // String url= "http://www.example.com/link3.html";
    //
    // String source =
    // "<html><head><title>Test</title></head><body>"+
    // "<!--<a href=http://www.example.com/link1.html>link1</a>"+
    // "Filler, with <b>bold</b> tags and<i>others</i>"+
    // "<a href=http://www.example.com/link2.html>link2</a>--!>"+
    // "<a href=http://www.example.com/link3.html>link3</a>";
    // assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testSkipsScriptTags() throws IOException {
    String url = "http://www.example.com/link3.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<script>"
      + "<a href=http://www.example.com/link1.html>link1</a>"
      + "Filler, with <b>bold</b> tags and<i>others</i>"
      + "<a href=http://www.example.com/link2.html>link2</a>"
      + "</script>"
      + "<a href=http://www.example.com/link3.html>link3</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testSkipsScriptTagsAllTheWay() throws IOException {
    String url = "http://www.example.com/link3.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<script>"
      + "<a href=http://www.example.com/link1.html>link1</a>"
      + "Filler, with <b>bold</b> tags and<i>others</i>"
      + "<a href=http://www.example.com/link2.html" + "</script>"
      + "<a href=http://www.example.com/link3.html>link3</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  private void doScriptSkipTest(String openScript, String closeScript)
      throws IOException {
    doScriptSkipTest(openScript, closeScript, null);
  }

  private void doScriptSkipTest(String openScript, String closeScript,
				String failMsg) throws IOException {
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

  // TODO Unused, figure out if we need this
  // private String mkStr(char kar, int num) {
  // StringBuffer sb = new StringBuffer(num);
  // for (int ix=0; ix<num; ix++) {
  // sb.append(kar);
  // }
  // return sb.toString();
  // }

  public void testSkipsScriptTagsIgnoreCase() throws IOException {
    doScriptSkipTest("<ScRipt>", "</sCripT>");
  }

  public void testKeepsSpaceInUrl() throws IOException {
    String url = "http://www.example.com/link%20with%20space.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<a href=\"http://www.example.com/link with space.html\">Link</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testIgnoresNewLineInUrl() throws IOException {
    String url = "http://www.example.com/linkwithspace.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<a href=\"http://www.example.com/link\nwith\nspace.html\">Link</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testIgnoresNewLineInField() throws IOException {
    String url = "http://www.example.com/link.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<img\nsrc=\"http://www.example.com/link.html\">Link</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testIgnoresCRInUrl() throws IOException {
    String url = "http://www.example.com/linkwithspace.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<a href=\"http://www.example.com/link\rwith\rspace.html\">Link</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testKeepsDoubleQuoteInUrl() throws IOException {
    String url = "http://www.example.com/link%22with%22quotes.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<a href='http://www.example.com/link\"with\"quotes.html'>Link</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testKeepsSingleQuoteInUrl() throws IOException {
    String url = "http://www.example.com/link'with'quotes.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<a href=\"" + url + "\">Link</a>";
    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  public void testMultipleLinks() throws IOException {
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
      throws IOException {
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

  // public void testHttpEquiv() throws IOException {
  // String url1= "http://example.com/blah.html";
  // String source =
  // "<html><head>"
  // +"<meta http-equiv=\"refresh\" "
  // +"content=\"0; url=http://example.com/blah.html\">"
  // +"</head></html>";
  //
  // assertEquals(SetUtil.set(url1), parseSingleSource(source));
  //
  // source =
  // "<html><head>"
  // +"<meta http-equiv=\"refresh\" "
  // +"content=\"0;url=http://example.com/blah.html\">"
  // +"</head></html>";
  //
  // assertEquals(SetUtil.set(url1), parseSingleSource(source));
  // }

  // tests that we are only parsing out the URL when the
  // http-equiv header is "refresh"
  public void testHttpEquiv2() throws IOException {
    String source = "<html><head>" + "<meta http-equiv=\"blah\" "
      + "content=\"0; url=http://example.com/blah.html\">"
      + "</head></html>";

    assertEquals(SetUtil.set(), parseSingleSource(source));
  }

  private Set<String> parseSingleSource(String source) throws IOException {
    MockArchivalUnit mau = new MockArchivalUnit();
    LinkExtractor ue = new RegexpCssLinkExtractor();
    mau.setLinkExtractor("text/css", ue);
    MockCachedUrl mcu = new MockCachedUrl("http://www.example.com", mau);
    mcu.setContent(source);

    cb.reset();
    m_extractor.extractUrls(mau, new StringInputStream(source), ENC,
			    "http://www.example.com", cb);
    return cb.getFoundUrls();
  }

  public void testRelativeLinksWithSameName() throws IOException {
    String url1 = "http://www.example.com/branch1/index.html";
    String url2 = "http://www.example.com/branch2/index.html";

    String source = "<html><head><title>Test</title></head><body>"
      + "<a href=branch1/index.html>link1</a>"
      + "Filler, with <b>bold</b> tags and<i>others</i>"
      + "<a href=branch2/index.html>link2</a>";

    MockCachedUrl mcu = new MockCachedUrl("http://www.example.com");
    mcu.setContent(source);

    m_extractor.extractUrls(mau, new StringInputStream(source), ENC,
			    "http://www.example.com", cb);

    Set<String> expected = new HashSet<String>();
    Collections.addAll(expected, url1, url2);
    assertEquals(expected, cb.getFoundUrls());
  }

  public void testRelativeLinksWithLeadingSlash() throws IOException {
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

    m_extractor.extractUrls(mau, new StringInputStream(source), ENC,
			    "http://www.example.com/blah/", cb);

    Set<String> expected = new HashSet<String>();
    Collections.addAll(expected, url1, url2, url3, url4, url5);
    assertEquals(expected, cb.getFoundUrls());
  }

  public void testProtocolNeutralLinksHttp() throws Exception {
    String url1= "http://sample2.com/foo/bar.x";
    String url2= "http://sample3.com/bar/bar.y";
    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href=\"//sample2.com/foo/bar.x\">link1</a>"+
      "<a href=\"//sample3.com/bar/bar.y\">link1</a>";

    m_extractor.extractUrls(mau, new StringInputStream(source), ENC,
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

    m_extractor.extractUrls(mau, new StringInputStream(source), ENC,
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

    m_extractor.extractUrls(mau, new StringInputStream(source), ENC,
			    "http://www.example.com/blah/", cb);

    assertEquals(SetUtil.set(url1, url2), cb.getFoundUrls());
  }


  /*Forms test cases
  // based upon highwire test case
  public void testFormOneHiddenAttribute() throws IOException {
  String url1 = "http://www.example.com/bioone/cgi/;F2?filename=jci116136F2.ppt";

  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/;F2\" method=\"get\">"
  + "<input type=\"submit\" value=\"Blah\"><INPUT TYPE=\"hidden\" NAME=\"filename\" VALUE=\"jci116136F2.ppt\"></form>";
  assertEquals(SetUtil.set(url1), parseSingleSource(source));
  }
	
  public void testFormOneHiddenAttributeWithoutName() throws IOException {
  String url1 = "http://www.example.com/bioone/cgi/;F2";

  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/;F2\" method=\"get\">"
  + "<input type=\"submit\" value=\"Blah\"><INPUT TYPE=\"hidden\" VALUE=\"jci116136F2.ppt\"></form>";
  assertEquals(SetUtil.set(url1), parseSingleSource(source));

  }
	
  public void testFormOneHiddenAttributeWithBlankName() throws IOException {
  String url1 = "http://www.example.com/bioone/cgi/;F2";

  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/;F2\" method=\"get\">"
  + "<input type=\"submit\" value=\"Blah\"><INPUT TYPE=\"hidden\" NAME=\"\" VALUE=\"jci116136F2.ppt\"></form>";
  assertEquals(SetUtil.set(url1), parseSingleSource(source));

  }

  // based upon highwire test case
  public void testFormTwoHiddenAttribute() throws IOException {
  String url1 = "http://www.example.com/bioone/cgi/;F2?filename=jci116136F2.ppt&gender=male";

  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/;F2\" method=\"get\">"
  + "<input type=\"submit\" value=\"Blah\">"
  + "<INPUT TYPE=\"hidden\" NAME=\"filename\" VALUE=\"jci116136F2.ppt\">"
  + "<InpUt name=\"gender\" tYpe=\"hidden\" value=\"male\"> </form>";
  assertEquals(SetUtil.set(url1), parseSingleSource(source));

  }

  // based upon highwire test case
  public void testFormManyHiddenAttribute() throws IOException {
  String url1 = "http://www.example.com/cgi/powerpoint/pediatrics;103/1/SE1/203/F4?image_path=%2Fcontent%2Fpediatrics%2Fvol103%2Fissue1%2Fimages%2Flarge%2Fpe01t0183004.jpeg&caption=No+Caption+Found&citation=Plsek%2C+P.+E.+Pediatrics+1999%3B103%3Ae203&copyright=1999+American+Academy+of+Pediatrics&filename=pediatricsv103i1pe203F4.ppt&ppt_download=true&id=103%2F1%2FSE1%2F203%2FF4&redirect_url=http%3A%2F%2Fpediatrics.aappublications.org%2Fcgi%2Fcontent%2Ffull%2F103%2F1%2FSE1%2F203%2FF4&site_name=pediatrics&notes_text=&generate_file=Download+Image+to+PowerPoint";

  String source = "<html><head><title>Test</title></head><body>"
  + "	  <FORM METHOD=GET ACTION=\"/cgi/powerpoint/pediatrics;103/1/SE1/203/F4\">"
  + "<INPUT TYPE=\"hidden\" NAME=\"image_path\" VALUE=\"%2Fcontent%2Fpediatrics%2Fvol103%2Fissue1%2Fimages%2Flarge%2Fpe01t0183004.jpeg\">"
  + "<INPUT TYPE=\"hidden\" NAME=\"caption\" VALUE=\"No+Caption+Found\">"
  + "<INPUT TYPE=\"hidden\" NAME=\"citation\" VALUE=\"Plsek%2C+P.+E.+Pediatrics+1999%3B103%3Ae203\">"
  + "<INPUT TYPE=\"hidden\" NAME=\"copyright\" VALUE=\"1999+American+Academy+of+Pediatrics\">"
  + "<INPUT TYPE=\"hidden\" NAME=\"filename\" VALUE=\"pediatricsv103i1pe203F4.ppt\">"
  + "<INPUT TYPE=\"hidden\" NAME=\"ppt_download\" VALUE=\"true\">"
  + "<INPUT TYPE=\"hidden\" NAME=\"id\" VALUE=\"103/1/SE1/203/F4\">"
  + "<INPUT TYPE=\"hidden\" NAME=\"redirect_url\" VALUE=\"http%3A%2F%2Fpediatrics.aappublications.org%2Fcgi%2Fcontent%2Ffull%2F103%2F1%2FSE1%2F203%2FF4\">"
  + "<INPUT TYPE=\"hidden\" NAME=\"site_name\" VALUE=\"pediatrics\">"
  + "<INPUT TYPE=\"hidden\" NAME=\"notes_text\" VALUE=\"\">"
  + "<INPUT TYPE=\"submit\" NAME=\"generate_file\" VALUE=\"Download Image to PowerPoint\">"
  + "</FORM>";
  assertEquals(SetUtil.set(url1), parseSingleSource(source));

  }

  public void testTwoFormsOneHiddenAttribute() throws IOException {
  String url1 = "http://www.example.com/bioone/cgi/;F2?filename=blah1.ppt";
  String url2 = "http://www.example.com/biotwo/cgi/;F2?filename=blah2.ppt";

  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/;F2\" method=\"get\">"
  + "<input type=\"submit\" value=\"Blah\"><INPUT TYPE=\"hidden\" NAME=\"filename\" VALUE=\"blah1.ppt\"></form>"
  + "<form action=\"http://www.example.com/biotwo/cgi/;F2\" method=\"get\">"
  + "<input type=\"submit\" value=\"Blah\"><INPUT TYPE=\"hidden\" NAME=\"filename\" VALUE=\"blah2.ppt\"></form>";

  assertEquals(SetUtil.set(url1, url2), parseSingleSource(source));

  }

  public void testPOSTForm() throws IOException {
  // For a POST form, we treat it the same way as a GET form but normalize (alpha sorted parameters) the url 
  // before storing. There is a corresponding logic in the proxy handler where a post request is assembled into
  // a normalized get request before serving.
  String url = "http://www.example.com/form/post?aArg=aVal&bArg=bVal";
  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"/form/post\" method=\"post\">"
  + "<input type=\"submit\" value=\"Blah\" />"
  + "<input type=\"hidden\" name=\"bArg\" value=\"bVal\" />"
  + "<input type=\"hidden\" name=\"aArg\" value=\"aVal\" />"
  + "</form>";

  assertEquals(SetUtil.set(url), parseSingleSource(source));
  }
	
  // a submit form of type get should return a single url
  public void testSubmitOnlyForm() throws IOException {
  String url1 = "http://www.example.com/bioone/cgi/;F2?submitName=Blah";

  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/;F2\" method=\"get\">"
  + "<input name=\"submitName\" type=\"submit\" value=\"Blah\"></form>";
  assertEquals(SetUtil.set(url1), parseSingleSource(source));

  }

  // url > 256 characters after the slash succeeds (as long as long URLrepository is active)
  public void testTooLongFormUrl() throws IOException {
  StringBuffer long_value_builder = new StringBuffer();
  String prefix = ";F2?filename=j";
  for (int i = 0; i < (256 - prefix.length()); i++) {
  long_value_builder.append("a");
  }
  String long_value = long_value_builder.toString();
  String url1 = "http://www.example.com/bioone/cgi/" + prefix
  + long_value;

  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/;F2\" method=\"get\">"
  + "<input type=\"submit\" value=\"Blah\"><INPUT TYPE=\"hidden\" NAME=\"filename\" VALUE=\"j"
  + long_value + "\"></form>";
  assertEquals(SetUtil.set(url1), parseSingleSource(source));
  }

  // url = 255 characters after the slash passes
  public void testMaxLengthFormUrl() throws IOException {
  StringBuffer long_value_builder = new StringBuffer();
  String prefix = ";F2?filename=j";
  for (int i = 0; i < (255 - prefix.length()); i++) {
  long_value_builder.append("a");
  }
  String long_value = long_value_builder.toString();
  String url1 = "http://www.example.com/bioone/cgi/" + prefix
  + long_value;

  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/;F2\" method=\"get\">"
  + "<input type=\"submit\" value=\"Blah\"><INPUT TYPE=\"hidden\" NAME=\"filename\" VALUE=\"j"
  + long_value + "\"></form>";
  assertEquals(SetUtil.set(url1), parseSingleSource(source));
  }

  // no submit button => no URL returned
  public void testEmptyForm() throws IOException {

  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
  + "</form>";
  assertEquals(SetUtil.set(), parseSingleSource(source));

  }

  public void testEmptySelect() throws IOException {
  Set<String> expectedResults = new HashSet<String>();
  expectedResults.add("http://www.example.com/bioone/cgi/?odd=world");
  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
  + "<select name=\"hello_name\"></select>"
  + "<input type=\"hidden\" name=\"odd\" value=\"world\" />"
  + "<input type=\"submit\"/>" + "</form></html>";
  assertEquals(expectedResults, parseSingleSource(source));
  }
	
  public void testSelectWithoutName() throws IOException {
  Set<String> expectedResults = new HashSet<String>();
  expectedResults
  .add("http://www.example.com/bioone/cgi/?odd=world");
  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
  + "<select><option value=\"hello_val\" />hello</option></select>"
  + "<input type=\"hidden\" name=\"odd\" value=\"world\" />"
  + "<input type=\"submit\"/>" + "</form></html>";
  assertEquals(expectedResults, parseSingleSource(source));
  }
	
  public void testSelectWithBlankName() throws IOException {
  Set<String> expectedResults = new HashSet<String>();
  expectedResults
  .add("http://www.example.com/bioone/cgi/?odd=world");
  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
  + "<select name=\"\"><option value=\"hello_val\" />hello</option></select>"
  + "<input type=\"hidden\" name=\"odd\" value=\"world\" />"
  + "<input type=\"submit\"/>" + "</form></html>";
  assertEquals(expectedResults, parseSingleSource(source));
  }
	

  public void testOneOption() throws IOException {
  Set<String> expectedResults = new HashSet<String>();
  expectedResults
  .add("http://www.example.com/bioone/cgi/?hello_name=hello_val&odd=world");
  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
  + "<select name=\"hello_name\"><option value=\"hello_val\" />hello</option></select>"
  + "<input type=\"hidden\" name=\"odd\" value=\"world\" />"
  + "<input type=\"submit\"/>" + "</form></html>";
  assertEquals(expectedResults, parseSingleSource(source));
  }

  public void testTwoOptions() throws IOException {
  Set<String> expectedResults = new HashSet<String>();
  expectedResults
  .add("http://www.example.com/bioone/cgi/?hello_name=hello_val&odd=world");
  expectedResults
  .add("http://www.example.com/bioone/cgi/?hello_name=world_val&odd=world");
  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
  + "<select name=\"hello_name\">"
  + "<option value=\"hello_val\" />hello</option>"
  + "<option value=\"world_val\" />world</option>" + "</select>"
  + "<input type=\"hidden\" name=\"odd\" value=\"world\" />"
  + "<input type=\"submit\"/>" + "</form></html>";
  assertEquals(expectedResults, parseSingleSource(source));
  }

  public void testThreeOptions() throws IOException {
  Set<String> expectedResults = new HashSet<String>();
  expectedResults
  .add("http://www.example.com/bioone/cgi/?hello_name=hello_val&odd=world");
  expectedResults
  .add("http://www.example.com/bioone/cgi/?hello_name=world_val&odd=world");
  expectedResults
  .add("http://www.example.com/bioone/cgi/?hello_name=goodbye_val&odd=world");
  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
  + "<select name=\"hello_name\">"
  + "<option value=\"hello_val\" />hello</option>"
  + "<option value=\"world_val\" />world</option>"
  + "<option value=\"goodbye_val\" />goodbye</option>"
  + "</select>"
  + "<input type=\"hidden\" name=\"odd\" value=\"world\" />"
  + "<input type=\"submit\"/>" + "</form></html>";
  assertEquals(expectedResults, parseSingleSource(source));
  }

  public void testTwoSelect() throws IOException {
  Set<String> expectedResults = new HashSet<String>();
  expectedResults
  .add("http://www.example.com/bioone/cgi/?hello_name=hello_val&numbers_name=one_val&odd=world");
  expectedResults
  .add("http://www.example.com/bioone/cgi/?hello_name=world_val&numbers_name=one_val&odd=world");
  expectedResults
  .add("http://www.example.com/bioone/cgi/?hello_name=hello_val&numbers_name=two_val&odd=world");
  expectedResults
  .add("http://www.example.com/bioone/cgi/?hello_name=world_val&numbers_name=two_val&odd=world");
  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
  + "<select name=\"hello_name\">"
  + "<option value=\"hello_val\" />hello</option>"
  + "<option value=\"world_val\" />world</option>" + "</select>"
  + "<select name=\"numbers_name\">"
  + "<option value=\"one_val\" />one</option>"
  + "<option value=\"two_val\" />two</option>" + "</select>"
  + "<input type=\"hidden\" name=\"odd\" value=\"world\" />"
  + "<input type=\"submit\"/>" + "</form></html>";
  assertEquals(expectedResults, parseSingleSource(source));
  }

  public void testTwoSelectWithOneSelectUnnamed() throws IOException {
  Set<String> expectedResults = new HashSet<String>();
  expectedResults
  .add("http://www.example.com/bioone/cgi/?hello_name=hello_val&odd=world");
  expectedResults
  .add("http://www.example.com/bioone/cgi/?hello_name=world_val&odd=world");
  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
  + "<select name=\"hello_name\">"
  + "<option value=\"hello_val\" />hello</option>"
  + "<option value=\"world_val\" />world</option>" + "</select>"
  + "<select>" + "<option value=\"one_val\" />one</option>"
  + "<option value=\"two_val\" />two</option>" + "</select>"
  + "<input type=\"hidden\" name=\"odd\" value=\"world\" />"
  + "<input type=\"submit\"/>" + "</form></html>";
  assertEquals(expectedResults, parseSingleSource(source));
  }

  public void testTwoSelectWithOneSelectUnnamedReversedOrder()
  throws IOException {
  Set<String> expectedResults = new HashSet<String>();
  expectedResults
  .add("http://www.example.com/bioone/cgi/?hello_name=hello_val&odd=world");
  expectedResults
  .add("http://www.example.com/bioone/cgi/?hello_name=world_val&odd=world");
  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
  + "<select>" + "<option value=\"one_val\" />one</option>"
  + "<option value=\"two_val\" />two</option>" + "</select>"
  + "<select name=\"hello_name\">"
  + "<option value=\"hello_val\" />hello</option>"
  + "<option value=\"world_val\" />world</option>" + "</select>"
  + "<input type=\"hidden\" name=\"odd\" value=\"world\" />"
  + "<input type=\"submit\"/>" + "</form></html>";
  assertEquals(expectedResults, parseSingleSource(source));
  }

  public void testOneRadioOneValue() throws IOException {
  Set<String> expectedResults = new HashSet<String>();
  expectedResults.add("http://www.example.com/bioone/cgi/?arg=val");
  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
  + "<input type=\"radio\" name=\"arg\" value=\"val\" />"
  + "<input type=\"submit\"/>" + "</form></html>";
  assertEquals(expectedResults, parseSingleSource(source));
  }
	
  public void testOneRadioOneValueWithoutName() throws IOException {
  Set<String> expectedResults = new HashSet<String>();
  expectedResults.add("http://www.example.com/bioone/cgi/");
  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
  + "<input type=\"radio\" value=\"val\" />"
  + "<input type=\"submit\"/>" + "</form></html>";
  assertEquals(expectedResults, parseSingleSource(source));
  }
	
  public void testOneRadioOneValueWithBlankName() throws IOException {
  Set<String> expectedResults = new HashSet<String>();
  expectedResults.add("http://www.example.com/bioone/cgi/");
  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
  + "<input type=\"radio\" name=\"\" value=\"val\" />"
  + "<input type=\"submit\"/>" + "</form></html>";
  assertEquals(expectedResults, parseSingleSource(source));
  }

  public void testOneRadioMultipleValues() throws IOException {
  Set<String> expectedResults = new HashSet<String>();
  expectedResults.add("http://www.example.com/bioone/cgi/?arg=val1");
  expectedResults.add("http://www.example.com/bioone/cgi/?arg=val2");
  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
  + "<input type=\"radio\" name=\"arg\" value=\"val1\" />"
  + "<input type=\"radio\" name=\"arg\" value=\"val2\" />"
  + "<input type=\"submit\"/>" + "</form></html>";
  assertEquals(expectedResults, parseSingleSource(source));
  }

  public void testTwoRadioMultipleValues() throws IOException {
  Set<String> expectedResults = new HashSet<String>();
  expectedResults
  .add("http://www.example.com/bioone/cgi/?arg1=val11&arg2=val21");
  expectedResults
  .add("http://www.example.com/bioone/cgi/?arg1=val11&arg2=val22");
  expectedResults
  .add("http://www.example.com/bioone/cgi/?arg1=val12&arg2=val21");
  expectedResults
  .add("http://www.example.com/bioone/cgi/?arg1=val12&arg2=val22");
  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
  + "<input type=\"radio\" name=\"arg1\" value=\"val11\" />"
  + "<input type=\"radio\" name=\"arg1\" value=\"val12\" />"
  + "<input type=\"radio\" name=\"arg2\" value=\"val21\" />"
  + "<input type=\"radio\" name=\"arg2\" value=\"val22\" />"
  + "<input type=\"submit\"/>" + "</form></html>";
  assertEquals(expectedResults, parseSingleSource(source));
  }

  public void testRadioWithoutName() throws IOException {
  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
  + "<input type=\"radio\" value=\"val\" />"
  + "<input type=\"submit\"/>" + "</form></html>";
  assertEquals(SetUtil.set("http://www.example.com/bioone/cgi/"),
  parseSingleSource(source));
  }

  public void testOneCheckboxOneValue() throws IOException {
  Set<String> expectedResults = new HashSet<String>();
  expectedResults.add("http://www.example.com/bioone/cgi/?arg=val");
  expectedResults.add("http://www.example.com/bioone/cgi/?arg=");
  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
  + "<input type=\"checkbox\" name=\"arg\" value=\"val\" />"
  + "<input type=\"submit\"/>" + "</form></html>";
  assertEquals(expectedResults, parseSingleSource(source));
  }
	
  public void testOneCheckboxOneValueWithoutName() throws IOException {
  Set<String> expectedResults = new HashSet<String>();
  expectedResults.add("http://www.example.com/bioone/cgi/");
  expectedResults.add("http://www.example.com/bioone/cgi/");
  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
  + "<input type=\"checkbox\" value=\"val\" />"
  + "<input type=\"submit\"/>" + "</form></html>";
  assertEquals(expectedResults, parseSingleSource(source));
  }
	
  public void testOneCheckboxOneValueWithBlankName() throws IOException {
  Set<String> expectedResults = new HashSet<String>();
  expectedResults.add("http://www.example.com/bioone/cgi/");
  expectedResults.add("http://www.example.com/bioone/cgi/");
  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
  + "<input type=\"checkbox\" name=\"\" value=\"val\" />"
  + "<input type=\"submit\"/>" + "</form></html>";
  assertEquals(expectedResults, parseSingleSource(source));
  }

  public void testOneCheckboxMultipleValues() throws IOException {
  Set<String> expectedResults = new HashSet<String>();
  expectedResults
  .add("http://www.example.com/bioone/cgi/?arg=val1&arg=val2");
  expectedResults.add("http://www.example.com/bioone/cgi/?arg=&arg=val2");
  expectedResults.add("http://www.example.com/bioone/cgi/?arg=val1&arg=");
  expectedResults.add("http://www.example.com/bioone/cgi/?arg=&arg=");
  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
  + "<input type=\"checkbox\" name=\"arg\" value=\"val1\" />"
  + "<input type=\"checkbox\" name=\"arg\" value=\"val2\" />"
  + "<input type=\"submit\"/>" + "</form></html>";
  assertEquals(expectedResults, parseSingleSource(source));
  }

  public void testMultipleCheckboxesWithDefaultValues() throws IOException {
  Set<String> expectedResults = new HashSet<String>();
  expectedResults
  .add("http://www.example.com/bioone/cgi/?arg1=on&arg2=on");
  expectedResults.add("http://www.example.com/bioone/cgi/?arg1=&arg2=on");
  expectedResults.add("http://www.example.com/bioone/cgi/?arg1=on&arg2=");
  expectedResults.add("http://www.example.com/bioone/cgi/?arg1=&arg2=");
  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
  + "<input type=\"checkbox\" name=\"arg1\" />"
  + "<input type=\"checkbox\" name=\"arg2\" />"
  + "<input type=\"submit\"/>" + "</form></html>";
  assertEquals(expectedResults, parseSingleSource(source));
  }

  // Add any new input types supported to this test case as well.
  public void testAllFormInputs() throws IOException {
  Set<String> expectedResults = new HashSet<String>();
  expectedResults
  .add("http://www.example.com/bioone/cgi/?hello_name=hello_val&odd=world&radio=rval1&checkbox=on");
  expectedResults
  .add("http://www.example.com/bioone/cgi/?hello_name=world_val&odd=world&radio=rval1&checkbox=on");
  expectedResults
  .add("http://www.example.com/bioone/cgi/?hello_name=hello_val&odd=world&radio=rval2&checkbox=on");
  expectedResults
  .add("http://www.example.com/bioone/cgi/?hello_name=world_val&odd=world&radio=rval2&checkbox=on");
  expectedResults
  .add("http://www.example.com/bioone/cgi/?hello_name=hello_val&odd=world&radio=rval1&checkbox=");
  expectedResults
  .add("http://www.example.com/bioone/cgi/?hello_name=world_val&odd=world&radio=rval1&checkbox=");
  expectedResults
  .add("http://www.example.com/bioone/cgi/?hello_name=hello_val&odd=world&radio=rval2&checkbox=");
  expectedResults
  .add("http://www.example.com/bioone/cgi/?hello_name=world_val&odd=world&radio=rval2&checkbox=");
  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
  + "<select name=\"hello_name\">"
  + "<option value=\"hello_val\" />hello</option>"
  + "<option value=\"world_val\" />world</option>" + "</select>"
  + "<input type=\"hidden\" name=\"odd\" value=\"world\" />"
  + "<input type=\"radio\" name=\"radio\" value=\"rval1\" />"
  + "<input type=\"radio\" name=\"radio\" value=\"rval2\" />"
  + "<input type=\"checkbox\" name=\"checkbox\" />"
  + "<input type=\"submit\"/>" + "</form></html>";
  assertEquals(expectedResults, parseSingleSource(source));
  }
	
  public void testFormInsideForm() throws IOException {
  String source = "<html><head><title>Test</title></head><body>"
  + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
  + "<input type=\"submit\"/>"
  + "<input type=\"hidden\" name=\"arg1\" value=\"value1\" />"
  + "<form action=\"http://www.example.com/biotwo/cgi/\" method=\"get\">"
  + "<input type=\"hidden\" name=\"arg2\" value=\"value2\"/>"
  + "<input type=\"submit\"/>" + "</form></html>";
  assertEquals(SetUtil.set("http://www.example.com/bioone/cgi/?arg1=value1&arg2=value2"), parseSingleSource(source));
  }

  // NOTE: The test below is supposed to test the max num url restriction of 1000000 but it takes about 16-17s for the
  // Form iterator to iterate through 1000000 urls.
  //
  //	public void testTooManyCheckBoxes() throws IOException {
  //		String source = "<html><head><title>Test</title></head><body>"
  //			+ "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
  //			+ "<input type=\"checkbox\" name=\"cb1\" />"
  //			+ "<input type=\"checkbox\" name=\"cb2\" />"
  //			+ "<input type=\"checkbox\" name=\"cb3\" />"
  //			+ "<input type=\"checkbox\" name=\"cb4\" />"
  //			+ "<input type=\"checkbox\" name=\"cb5\" />"
  //			+ "<input type=\"checkbox\" name=\"cb6\" />"
  //			+ "<input type=\"checkbox\" name=\"cb7\" />"
  //			+ "<input type=\"checkbox\" name=\"cb8\" />"
  //			+ "<input type=\"checkbox\" name=\"cb9\" />"
  //			+ "<input type=\"checkbox\" name=\"cb10\" />"
  //			+ "<input type=\"checkbox\" name=\"cb11\" />"
  //			+ "<input type=\"checkbox\" name=\"cb12\" />"
  //			+ "<input type=\"checkbox\" name=\"cb13\" />"
  //			+ "<input type=\"checkbox\" name=\"cb14\" />"
  //			+ "<input type=\"checkbox\" name=\"cb15\" />"
  //			+ "<input type=\"checkbox\" name=\"cb16\" />"
  //			+ "<input type=\"checkbox\" name=\"cb17\" />"
  //			+ "<input type=\"checkbox\" name=\"cb18\" />"
  //			+ "<input type=\"checkbox\" name=\"cb19\" />"
  //			+ "<input type=\"checkbox\" name=\"cb20\" />"
  //			+ "<input type=\"submit\"/>" + "</form></html>";
  //		assertEquals(1000000, parseSingleSource(source).size());
  //	}
	
  */
  private void checkBadTags(String[] badTags, String closeTag)
      throws IOException {
    String url = "http://www.example.com/web_link.html";
    for (int ix = 0; ix < badTags.length; ix++) {
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

  private static class MyLinkExtractorCallback implements
						 LinkExtractor.Callback {
    Set<String> foundUrls = new HashSet<String>();

    public void foundLink(String url) {
      foundUrls.add(url);
    }

    public Set<String> getFoundUrls() {
      return foundUrls;
    }

    public void reset() {
      foundUrls = new HashSet<String>();
    }
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestHtmlParserLinkExtractor.class.getName() };
    junit.textui.TestRunner.main(testCaseList);
  }

}
