/*
 * $Id$
 */
/*

 Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire;

import java.util.Set;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.Constants;
import org.lockss.util.SetUtil;


public class TestHighWireDrupalHtmlLinkExtractorFactory extends LockssTestCase {

  private HighWireDrupalHtmlLinkExtractorFactory fact;
  private LinkExtractor m_extractor;
  private MyLinkExtractorCallback m_callback;
  static String ENC = Constants.DEFAULT_ENCODING;
  private MockArchivalUnit m_mau;
  private final String HW_BASE_URL = "http://ajp.highwire.org/";
  private static final String NODE_ID = "111111";
  
  private static final String citation = "<html><head><title>Test Title</title>" +
      "<link href=\"/node/" + NODE_ID + "\" rel=\"shortlink\">" +
      "<div>" +
      "</head><body></body>" +
      "</html>";
  
  private static final String toc = "<html><head><title>Test Title</title>" +
      "</head><body>" +
      "<ul class=\"toc-section\">" +
      "  <li class=\"toc-item first last odd\"><div class=\"toc-citation\">" +
      "      <div rel=\"/highwire/article_citation_preview/" + NODE_ID + "\" data-node-nid=\"" + NODE_ID + 
      "\" title=\"&lt;a href=&quot;http://ajprenal.physiology.org/content/304/1/F1&quot;" +
      " class=&quot;highwire-cite-linked-title&quot;&gt;&lt;div class=&quot;highwire-cite-title&quot;&gt;" +
      "Renal&lt;/div&gt;&lt;/a&gt;\" class=\"highwire-article-citation tooltip-enable" +
      " highwire_article_citation_tooltip-processed\"><div class=\"highwire-cite" +
      " highwire-citation-jnl-aps-toc-citation clearfix\">\n" + 
      "      <div class=\"highwire-cite-access\"><i title=\"Open Access\" class=\"highwire-access-icon" +
      " highwire-access-icon-open-access open-access icon-unlock-alt\"></i></div>\n" + 
      "      <a class=\"highwire-cite-linked-title\" href=\"http://ajprenal.physiology.org/content/304/1/F1\">" +
      "<div class=\"highwire-cite-title\">Renal</div></a>  \n" + 
      "      <div class=\"highwire-cite-authors\"></div>\n" + 
      "      <div class=\"highwire-cite-metadata\"></div>\n" + 
      "  </div></div></div></li></ul>" +
      "</body>" +
      "</html>";
  
  public static final String basicLinksHtml = "      <ul>" +
      "  <li><a href=\"http://ajp.highwire.org/\">AIAA ASSOCIATION WEBSITE</a></li>" +
      "  <li id=\"cartItem\"><a href=\"http://ajp.highwire.org/IframeOneColumn.aspx?id=4191\">VIEW CART (<span id=\"cartItemCount\">0</span>)</a></li>" +
      "  <li><a href=\"http://ajp.highwire.org/SecondaryTwoColumn.aspx?id=255\">JOIN</a></li>" +
      "  <li><a href=\"http://ajp.highwire.org/IframeOneColumn.aspx?id=3411\">LOGIN</a></li>" +
      "</ul>";
  
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    m_mau = new MockArchivalUnit();
    m_callback = new MyLinkExtractorCallback();
    fact = new HighWireDrupalHtmlLinkExtractorFactory();
    m_extractor = fact.createLinkExtractor("html");
    
  }
  Set<String> expectedUrls;
  
  public void testBasicLinks() throws Exception {
    expectedUrls = SetUtil.set(
        "http://ajp.highwire.org/",
        "http://ajp.highwire.org/IframeOneColumn.aspx?id=4191",
        "http://ajp.highwire.org/SecondaryTwoColumn.aspx?id=255",
        "http://ajp.highwire.org/IframeOneColumn.aspx?id=3411");
    
    Set<String> result_strings = parseSingleSource(basicLinksHtml);
    
    assertEquals(4, result_strings.size());
    
    for (String url : result_strings) {
      log.debug3("URL: " + url);
      assertTrue(expectedUrls.contains(url));
    }
  }
  
  
  // this is copied directly from the Jsoup test to make sure that our class extension
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
  
  // and once we get to the content page, will we handle stuff as expected? 
  public void testCitations() throws Exception {
    String expectedUrl = HW_BASE_URL + "highwire/citation/" + NODE_ID + "/ris";
    Set<String> result_strings = parseSingleSource(citation);
    
    assertEquals(2, result_strings.size());
    assertTrue(result_strings.contains(expectedUrl));
  }
  
  private Set<String> parseSingleSource(String source)
      throws Exception {
    String srcUrl = HW_BASE_URL + "content/1/2/c3";
    String mimeType = "html";
    LinkExtractor ue = new JsoupHtmlLinkExtractor();
    m_mau.setLinkExtractor(mimeType, ue);
    MockCachedUrl mcu = new org.lockss.test.MockCachedUrl(srcUrl, m_mau);
    mcu.setContent(source);
    
    m_callback.reset();
    m_extractor.extractUrls(m_mau,
        new org.lockss.test.StringInputStream(source), ENC,
        srcUrl, m_callback);
    return m_callback.getFoundUrls();
  }
  
  // and once we get to the toc page, will we handle stuff as expected? 
  public void testToc() throws Exception {
    String expectedUrl = HW_BASE_URL + "highwire/article_citation_preview/" + NODE_ID;
    Set<String> result_strings = parseToc(toc);
    
    assertEquals(2, result_strings.size());
    assertTrue(result_strings.contains(expectedUrl));
  }
  
  private Set<String> parseToc(String source)
      throws Exception {
    String srcUrl = HW_BASE_URL + "content/1/2.toc";
    String mimeType = "html";
    LinkExtractor ue = new JsoupHtmlLinkExtractor();
    m_mau.setLinkExtractor(mimeType, ue);
    MockCachedUrl mcu = new org.lockss.test.MockCachedUrl(srcUrl, m_mau);
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
    
    @Override
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
