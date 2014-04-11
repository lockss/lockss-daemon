/*
 * $Id: TestHighWireDrupalHtmlLinkExtractorFactory.java,v 1.1 2014-04-11 22:45:23 etenbrink Exp $
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
  
  private static final String citation= "<html><head><title>Test Title</title>" +
      "<link href=\"/node/" + NODE_ID + "\" rel=\"shortlink\">" +
      "<div>" +
      "</head><body></body>" +
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
