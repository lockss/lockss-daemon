/*  $Id:  $
 
 Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,

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

package org.lockss.plugin.elifesciences;

import java.io.InputStream;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

public class TestELifeHtmlHashFilterFactory extends LockssTestCase {
  private ELifeHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new ELifeHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }
  
  
  // this tests <head>, <input>, <script>, <noscript>, <input>
  //      <div id="footer">, <div id="doubleclick">, <div id="header">, <div id="crossMark"
  //      <link rel="stylesheet"
  //      <!-- html comments --> , <button id="chat-widget">
  //       <button class="StickySideButton_left StickySideButton_left--feedback">Support</button>
  private static final String withStuffToHashOut =
      "<head prefix=\"og: http://ogp.me/ns#\" >" + 
      "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />" +
      "</head>" +
      "<body class=\"html not-front not-logged-in page-node page-node-493560 node-type-elife-article-ver\">" +
      "<script type=\"text/javascript\" src=\"http://hello.org/sites/stuff.js\"></script>" +
      "<header id=\"section-header\" class=\"section section-header\">" +
      "</header>" +
      "<p class=\"first-child\">" +
      "<footer id=\"section-footer\" class=\"section section-footer\">" +
      "</footer>" +
      "<div class=\"grid-31 prefix-2 region region-responsive-header\" id=\"region-responsive-header\">" +
      "</div>" +
      "<div class=\"sidebar-wrapper grid-9 omega\">" +
      "</div>" +
      "<div id=\"decision-letter\" class=\"collapsible-container\">" +
      "</div>" +
      "<div id=\"author-response\" class=\"collapsible-container\">" +
      "</div>" +
      "<div id=\"comments\" class=\"collapsible-container\">" +
      "</div>" +
      "<div id=\"references\" class=\"collapsible-container\">" +
      "</div>" +
      "<div id=\"zone-header-wrapper\" class=\"something-different\">" +
      "</div>" +
      "<div class=\"page_header\" role=\"banner\">" +
      "</div>" +
      "<ul class=\"elife-article-categories\">" +
      "</ul>" +
      "<div class=\"form-item hello-world-select form-item-jump\">" +
      "</div>" +
      "<div class=\"elife-article-corrections\">" +
      "</div>" +
      "<div class=\"elife-article-criticalrelation\">" +
      "</div>" +
      "<div id=\"content\">X" +
      "</div>" +
      "</body>";
  private static final String withoutStuffToHashOut =
      "<body>" + // attribute stripped class=...
      "<p>" + // attribute stripped class=...
      "<div>" + // attribute stripped class=\"elife-article-corrections\"
      "</div>" +
      "<div>X" +
      "</div>" +
      "</body>";
  
  private static final String withCommentSection=
      "<div class=\"panel-separator\"></div>" +
      "<div class=\"ctools-collapsible-container ctools-collapsible-processed\">" +
      "<span class=\"ctools-toggle\"></span>" +
      "<h2 class=\"pane-title ctools-collapsible-handle\">Comments</h2>" +
      "Some comment" +
      "</div>" +
      "<div>" +
      "<h2 class=\"pane-title ctools-collapsible-handle\">Comments</h2>"+
      "<div>" +
      "Blahblah"+
      "<div id=\"disqus_thread\"><noscript><p><a href=\"http://elifesciences.disqus.com/?url=http%3A%2F%2Felifesciences.org%2Fcontent%2F4%2Fe04316\">View the discussion thread.</a>"+
      "</p></noscript>" +
      "</div>" +
      "</div>" +
      "</div>" ;
  private static final String withoutCommentSection =
      "<div>" +
      "<h2 class=\"pane-title ctools-collapsible-handle\">Comments</h2>"+
      "<div>" +
      "Blahblah"+
      "</div>" +
      "</div>" ;
  
  /*
   * Compare with/without crossMark plus a whole bunch of other filters
   */

  public void testStuffToHashOut() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withStuffToHashOut), Constants.DEFAULT_ENCODING);
    String str = StringUtil.fromInputStream(actIn);

    assertEquals(withoutStuffToHashOut, str);
    
  }
  public void testCommentSection() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withCommentSection), Constants.DEFAULT_ENCODING);
    String str = StringUtil.fromInputStream(actIn);

    assertEquals(withoutCommentSection, str);
    
  }

}
