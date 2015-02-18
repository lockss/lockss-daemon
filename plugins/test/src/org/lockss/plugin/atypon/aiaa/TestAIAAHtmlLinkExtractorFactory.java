/*
 * $Id$
 */
/*

 Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of his software and associated documentation files (the "Software"), to deal
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

package org.lockss.plugin.atypon.aiaa;

import java.util.HashSet;
import java.util.Set;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.plugin.atypon.BaseAtyponUrlNormalizer;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.RegexpCssLinkExtractor;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.Constants;
import org.lockss.util.SetUtil;


public class TestAIAAHtmlLinkExtractorFactory extends LockssTestCase {
  UrlNormalizer normalizer = new BaseAtyponUrlNormalizer();

  private AIAAHtmlLinkExtractorFactory fact;
  private LinkExtractor m_extractor;
  private MyLinkExtractorCallback m_callback;
  static String ENC = Constants.DEFAULT_ENCODING;
  private MockArchivalUnit m_mau;
  private final String AIAA_BASE_URL = "http://arc.aiaa.org/";
  private static final String DOI_START = "11.1111";
  private static final String DOI_END = "TEST";

  private static final String citationForm=
      "<html><head><title>Test Title</title></head><body>" +
          "<div>" +
          "<div>" +
          "   <br/>" +
          "   <!-- download options -->" +
          "   <form action=\"/action/downloadCitation\" id=\"downloadCitation\" name=\"frmCitmgr\" method=\"post\" target=\"_self\">" +
          "    <input type=\"hidden\" name=\"doi\" value=\"" + DOI_START + "/" + DOI_END + "\" />" +
          "    <input type=\"hidden\" name=\"downloadFileName\" value=\"apha_ajph99_969\" />" +
          "   <input type=\"hidden\" name=\"direct\" value=\"true\" />" +
          "   <table summary=\"\">" +
          "   <input type='hidden' name='include' value='cit'/>" +
          "   </tr>" +
          "   <input id=\"submit\" type='submit' name='submit' value='Download article citation data' onclick=\"onCitMgrSubmit()\"/>" +
          "   </table></form>" +
          "</div>" +
          "</body>" +
          "</html>";

  public static final String basicLinksHtml =
      "      <ul>" +
          "  <li><a href=\"https://www.aiaa.org/\">AIAA ASSOCIATION WEBSITE</a></li>" +
          "  <li id=\"cartItem\"><a href=\"https://www.aiaa.org/IframeOneColumn.aspx?id=4191\">VIEW CART (<span id=\"cartItemCount\">0</span>)</a></li>" +
          "  <li><a href=\"https://www.aiaa.org/SecondaryTwoColumn.aspx?id=255\">JOIN</a></li>" +
          "  <li><a href=\"https://www.aiaa.org/IframeOneColumn.aspx?id=3411&URL_Success=http%3A%2F%2Farc.aiaa.org%2Fdoi%2Fabs%2F10.2514%2F3.43630\">LOGIN</a></li>" +
          "</ul>";

  public static final String javascriptLinksHtml = 
      "      <ul class=\"linkList blockLinks separators centered\">" +
          "  <li id=\"addToFavs\">" +
          "      <a href=\"javascript:submitArticles(document.frmAbs, '/personalize/addFavoriteArticle','Please check at least one article.')\">Add to Favorites</a>" +
          " </li>" +
          "" +
          "      <a href=\"javascript:submitArticles(document.frmAbs, '/action/showMailPage','Please check at least one article.')\">Email</a>" +
          " </li>" +
          " <li id=\"downloadCit\">" +
          "      <a href=\"javascript:submitArticles(document.frmAbs, '/action/showCitFormats','Please check at least one article.')\">Download to Citation Manager</a>" +
          "  </li>" +
          "  <li id=\"trackCit\"  title=\"\"> " +
          "     <a href=\"javascript:submitArticles(document.frmAbs, '/action/addCitationRss','Please check at least one article.')\">Track Citations</a>" +
          "  </li>" +
          "</ul>";

  @Override
  public void setUp() throws Exception {
    super.setUp();
    m_mau = new MockArchivalUnit();
    m_callback = new MyLinkExtractorCallback();
    fact = new AIAAHtmlLinkExtractorFactory();
    m_extractor = fact.createLinkExtractor("html");

  }
  Set<String> expectedUrls;

  public void testBasicLinks() throws Exception {
    expectedUrls = SetUtil.set(
        "https://www.aiaa.org/",
        "https://www.aiaa.org/IframeOneColumn.aspx?id=4191",
        "https://www.aiaa.org/SecondaryTwoColumn.aspx?id=255",
        "https://www.aiaa.org/IframeOneColumn.aspx?id=3411&URL_Success=http%3A%2F%2Farc.aiaa.org%2Fdoi%2Fabs%2F10.2514%2F3.43630");

    Set<String> result_strings = parseSingleSource(basicLinksHtml);

    assertEquals(4, result_strings.size());

    for (String url : result_strings) {
      log.debug3("URL: " + url);
      assertTrue(expectedUrls.contains(url));
    }
  }

  public void testJavascriptLinks() throws Exception {
    Set<String> result_strings = parseSingleSource(javascriptLinksHtml);

    assertEquals(1, result_strings.size());
    for (String url : result_strings) {
      log.debug("URL: " + url);
      assertEquals("http://arc.aiaa.org/action/showCitFormats?doi=" + DOI_START + "/" + DOI_END, url);
    }
  }

  // this is copied directory from the Jsoup test to make sure that our class extension
  // hasn't broken fallback behavior
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

  // and once we get to the showCitFormats page, will we handle stuff as expected? 
  public void testCitationsForm() throws Exception {
    UrlNormalizer normalizer = new BaseAtyponUrlNormalizer();
    String expectedUrl = AIAA_BASE_URL + "action/downloadCitation?doi=" + DOI_START + "%2F" + DOI_END + "&format=ris&include=cit";
    String norm_url = null;
    Set<String> result_strings = parseSingleSource(citationForm);
    
    assertEquals(1, result_strings.size());

    for (String url : result_strings) {
      norm_url = normalizer.normalizeUrl(url, m_mau);
    }
    assertEquals(expectedUrl,norm_url);
  }

  private Set<String> parseSingleSource(String source)
      throws Exception {
    String srcUrl = AIAA_BASE_URL + "doi/abs/" + DOI_START + "/" + DOI_END;
    MockArchivalUnit m_mau = new MockArchivalUnit();
    LinkExtractor ue = new RegexpCssLinkExtractor();
    m_mau.setLinkExtractor("text/css", ue);
    MockCachedUrl mcu =
        new org.lockss.test.MockCachedUrl(AIAA_BASE_URL + "doi/abs/" + DOI_START + "/" + DOI_END, m_mau);
    mcu.setContent(source);

    m_callback.reset();
    m_extractor.extractUrls(m_mau,
        new org.lockss.test.StringInputStream(source), ENC,
        srcUrl, m_callback);
    return m_callback.getFoundUrls();
  }

  private static class MyLinkExtractorCallback implements
  LinkExtractor.Callback {

    Set<String> foundUrls = new java.util.HashSet<String>();

    public void foundLink(String url) {
      foundUrls.add(url);
    }

    public Set<String> getFoundUrls() {
      return foundUrls;
    }

    public void reset() {
      foundUrls = new java.util.HashSet<String>();
    }
  }

}
