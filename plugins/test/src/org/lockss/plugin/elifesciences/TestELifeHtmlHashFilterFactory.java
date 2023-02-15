/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
