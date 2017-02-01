/*  $Id: TestBaseAtyponHtmlHashFilterFactory.java 39864 2015-02-18 09:10:24Z thib_gc $
 
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

package org.lockss.plugin.springer.link;

import java.io.InputStream;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

public class TestSpringerLinkHtmlHashFilterFactory extends LockssTestCase {
  private SpringerLinkHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new SpringerLinkHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }
  
  private static final String withWhiteSpace =
  "\n\n\n\n\n\n \n\n\n\n \n\n \n \n  ";

 
  private static final String withoutWhiteSpace =
      " ";
  
  // this tests <head>, <input>, <script>, <noscript>, <input>, <meta>, <link>
  //      <div id="footer">, <div id="doubleclick">, <div id="header">, <div id="crossMark"
  //      <link rel="stylesheet"
  //      <!-- html comments --> , <button id="chat-widget">
  //       <button class="StickySideButton_left StickySideButton_left--feedback">Support</button>
  private static final String withCrossMark =
      "<head></head>" +
      "<div id=\"footer\">hello world</div>" +
      "<noscript>hello world</noscript>" +
      "<script src=\"/static/js/test/test_v3.js\"></script>" +
      "<div id=\"doubleclick-ad\" class=\"banner-advert\">foo bar</div>" +
      "<div id=\"header\" role=\"banner\">refrigerator</div>" +
      "<meta property=\"og:image\" content=\"http://link.springer.com/pink-static/1223904338/images/png/SL-Square.png\"/>" +
      "<link rel=\"icon\" sizes=\"48x48\" type=\"image/png\" href=\"/pink-static/1223904338/images/favicon/favicon-48x48.png\">" +
      "<link rel=\"stylesheet\" media=\"print\" href=\"/static/201602081740-1149/css/print.css\"/>" +
      "<div id=\"content\">" +
      "<input id=\"leafContentType\" type=\"hidden\" value=\"Article\"/>" +
      "<div id=\"crossMark\" class=\"fulltext\">" +
      "  <a id=\"open-crossmark\" target=\"_blank\" href=\"https://crossmark.crossref.org/dialog/?doi=10.1007%2Fs00360-014-0804-5\" style=\"padding: 3px 0 13px 0;\"><img style=\"border: 0;\" id=\"crossmark-icon\" src=\"https://crossmark.crossref.org/images/crossmark_button.png\" /></a>" +
      "</div> " +
      "<!-- html comment -->" +
      "<button id=\"chat-widget\">Chatty Button</button>" +
      "<!--[if !(IE 8)]><!-->" +
      "<button class=\"StickySideButton_left StickySideButton_left--feedback\">Sticky</button>"+
      "</div> ";
  private static final String withoutCrossMark =
      "<div id=\"content\">" +
      " </div> ";
  private static final String withBodyAttr =
      "<body id=\"something\" class=\"company rd\" data-name=\"rd\">" +
      "<button id=\"chat-widget\">Chatty Button</button>" +
      "<div class=\"banner\">" +
      "<div class=\"banner-content \">" +
      "<p class=\"banner-content__message\">this is new.</p>" +
      "<a class=\"banner-content__link\" href=\"https://link\" title=\"Visit me\">Lookit!</a>" +
      "</div>" +
      "</div>" +
      "</body>";
  private static final String withoutBodyAttr =
      "<body id=\"something\" >" +
      "</body>";
  
  /*
   *  Compare Html and HtmlHashFiltered
   */
  public void testWhiteSpace() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withWhiteSpace), Constants.DEFAULT_ENCODING);
    String str = StringUtil.fromInputStream(actIn);
    assertEquals(withoutWhiteSpace, str);
  }
  
  /* 
   * Compare with/without the body attributes
   */
  public void testBodyAttrs() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withBodyAttr), Constants.DEFAULT_ENCODING);
    String str = StringUtil.fromInputStream(actIn);
    assertEquals(withoutBodyAttr, str);
  }
  
  /* 
   * Compare with/without the body attributes
   */
//  public void testOldNewSite() throws Exception {
//    InputStream newIn = fact.createFilteredInputStream(mau,
//    	getResourceAsStream("New_Springer_Same.html"), Constants.DEFAULT_ENCODING);
//    InputStream oldIn = fact.createFilteredInputStream(mau,
//        	getResourceAsStream("Old_Springer_Same.html"), Constants.DEFAULT_ENCODING);
//    
//    String newStr = StringUtil.fromInputStream(newIn);
//    String oldStr = StringUtil.fromInputStream(oldIn);
//    assertEquals(newStr, oldStr);
//  }
}
