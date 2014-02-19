/*
 * $Id: TestHighWireDrupalHtmlFilterFactory.java,v 1.1 2014-02-19 22:37:23 etenbrink Exp $
 */

/*

 Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestHighWireDrupalHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;
  
  private HighWireDrupalHtmlFilterFactory fact;
  private MockArchivalUnit mau;
  
  public void setUp() throws Exception {
    super.setUp();
    fact = new HighWireDrupalHtmlFilterFactory();
    mau = new MockArchivalUnit();
  }
  
  //  // Publisher adding/updating meta tags
  //  new TagNameFilter("head"),
  private static final String headHtml = "<html><head>Title</head></HTML>";
  private static final String headHtmlFiltered = "<html></HTML>";
  
  //  // remove ALL comments
  //  HtmlNodeFilters.comment(),
  private static final String comment = 
      "<!--[if lt IE 9]><script src=\"http://html5shiv.dd.com/svn/trunk/html5.js\">" +
          "</script><![endif]-->\n";
  private static final String commentFiltered = " ";
  
  //  // No relevant content in header/footer
  //  new TagNameFilter("header"),
  //  new TagNameFilter("footer"),
  private static final String header = "<div>\n" + 
      "<header id=\"section-header\" class=\"section section-header\">\n" + 
      "<div id=\"zone-user-wrapper\" class=\"zone-wrapper\"></div>\n" + 
      "</header>\n" + 
      "</div>";
  private static final String headerFiltered = "<div> </div>";
  
  private static final String footer = "<div>\n" + 
      "<footer id=\"section-footer\" class=\"section section-footer\">\n" + 
      "<div id=\"zone-postscript\" class=\"zone zone-postscript clearfix container-30\"></div>\n" +
      "</footer>\n" + 
      "</div>";
  private static final String footerFiltered = "<div> </div>";
  
  //  // copyright statement may change
  //  HtmlNodeFilters.tagWithAttribute("ul", "class", "copyright-statement"),
  private static final String withCopyright = "</div>\n" +
      "<ul class=\"copyright-statement\"><li class=\"fn\">Copyright © 2012 American Society</li>" +
      "</ul><div>";
  private static final String withoutCopyright = "</div> <div>";
  
  //// messages can appear arbitrarily
  //HtmlNodeFilters.tagWithAttributeRegex("div", "id", "messages"),
  private static final String messages = "<div>\n" +
      "<div id=\"messages\">arbitrary text" +
      "</div></div>";
  private static final String messagesFiltered = "<div> </div>";
  
  //// extras, prev/next pager and right sidebar may change
  //HtmlNodeFilters.tagWithAttributeRegex("div", "class", "cit-extra"),
  private static final String withCitExtra = "</div>\n" +
      "<div class=\"cit-extra\">" +
      "<a href=\"/lookup/external-ref?access_num=10.1056/NEJM200005183422006&amp;link_type=DOI\" " +
      "class=\"cit-ref-sprinkles cit-ref-sprinkles-doi cit-ref-sprinkles-crossref\"><span>CrossRef</span></a>" +
      "<a href=\"/lookup/external-ref?access_num=10816188&amp;link_type=MED&amp;atom=" +
      "%2Fajpcell%2F302%2F1%2FC1.atom\" class=\"cit-ref-sprinkles cit-ref-sprinkles-medline\">" +
      "<span>Medline</span></a>" +
      "<a href=\"/lookup/external-ref?access_num=000087068200006&amp;link_type=ISI\" " +
      "class=\"cit-ref-sprinkles cit-ref-sprinkles-newisilink cit-ref-sprinkles-webofscience\">" +
      "<span>Web of Science</span></a>" +
      "</div><div>";
  private static final String withoutCitExtra = "</div>\n<div>";
  
  //HtmlNodeFilters.tagWithAttributeRegex("div", "class", "pane-highwire-node-pager"),
  private static final String withPager = "</div>\n" +
      "<div class=\"panel-pane pane-highwire-node-pager\" >\n" + 
      "<div class=\"pane-content\">\n" + 
      "<div class=\"pager highwire-pager pager-mini clearfix highwire-node-pager " +
      "highwire-article-pager\"><span class=\"pager-prev\"><a href=\"/content/999/9/C91\" " +
      "title=\"Corrigendum\" rel=\"prev\" class=\"pager-link-prev link-icon\">" +
      "<i class=\"icon-circle-arrow-left\"></i> Previous</a></span><span class=\"pager-next\">" +
      "<a href=\"/content/999/9/C99\" title=\"Drive in the oviduct\" rel=\"next\" " +
      "class=\"pager-link-next link-icon link-icon-right\">Next <i class=\"icon-circle-arrow-right\">" +
      "</i></a></span></div>  </div>\n" + 
      "</div><div>";
  private static final String withoutPager = "</div>\n<div>";
  
  //HtmlNodeFilters.tagWithAttributeRegex("div", "class", "sidebar-right-wrapper"),
  private static final String withSidebar = "</div>\n" +
      "<div class=\"sidebar-right-wrapper grid-10 omega\">\n" + 
      "<div class=\"panel-panel panel-region-sidebar-right\">\n" + 
      "<div class=\"inside\">" +
      "<div class=\"panel-pane pane-panels-mini " +
      "pane-jnl-iss-issue-arch-art pane-style-alt-content\" >\n" + 
      "</div></div></div></div>\n" +
      "<div>";
  private static final String withoutSidebar = "</div>\n<div>";
  
  //new TagNameFilter("script"),
  //new TagNameFilter("noscript"),
  private static final String withScript =
      "<div>" +
      "<script type=\"text/javascript\">GA_googleFillSlot(\"tower_right_160x600\");</script>" +
      "<noscript type=\"text/javascript\">GA_googleFillSlot(\"tower_right_160x600\");</noscript>" +
      "</div>";
  private static final String withoutScript =
      "<div></div>";
  
  public void testFiltering() throws Exception {
    assertFilterToString(headHtml, headHtmlFiltered);
    assertFilterToString(comment, commentFiltered);
    assertFilterToString(header, headerFiltered);
    assertFilterToString(footer, footerFiltered);
    assertFilterToString(messages, messagesFiltered);
    
    assertFilterToSame(withCopyright, withoutCopyright);
    assertFilterToSame(withCitExtra, withoutCitExtra);
    assertFilterToSame(withPager, withoutPager);
    assertFilterToSame(withSidebar, withoutSidebar);
    assertFilterToSame(withScript, withoutScript);
    
  }
  
  private void assertFilterToSame(String str1, String str2) throws Exception {
    
    InputStream inA = fact.createFilteredInputStream(mau, new StringInputStream(str1),
        Constants.DEFAULT_ENCODING);
    InputStream inB = fact.createFilteredInputStream(mau, new StringInputStream(str2),
        Constants.DEFAULT_ENCODING);
    String actual = StringUtil.fromInputStream(inA);
    String expected = StringUtil.fromInputStream(inB);
    assertEquals(actual, expected, actual);
  }
  
  //Don't put the 2nd string through the filter - use it as a constant
  private void assertFilterToString(String orgString, String finalString) throws Exception {
    
    InputStream inA = fact.createFilteredInputStream(mau, new StringInputStream(orgString),
        Constants.DEFAULT_ENCODING);
    String filtered = StringUtil.fromInputStream(inA);
    assertEquals(filtered, finalString, filtered);
  }
  
}
