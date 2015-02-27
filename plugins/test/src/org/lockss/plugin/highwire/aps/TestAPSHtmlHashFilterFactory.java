/*
 * $Id$
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

package org.lockss.plugin.highwire.aps;

import java.io.*;

import org.lockss.util.*;
import org.lockss.plugin.highwire.TestHighWireDrupalHtmlCrawlFilterFactory;
import org.lockss.test.*;

public class TestAPSHtmlHashFilterFactory extends TestHighWireDrupalHtmlCrawlFilterFactory {
  static String ENC = Constants.DEFAULT_ENCODING;
  
  private APSHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new APSHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }
  
  //  // Publisher adding/updating meta tags
  //  new TagNameFilter("head"),
  private static final String headHtml = "A<html><head>Title</head></HTML>9";
  private static final String headHtmlFiltered = "A<html></HTML>9";
  
  //  // remove ALL comments
  //  HtmlNodeFilters.comment(),
  private static final String comment = 
      "<!--[if lt IE 9]><script src=\"http://html5shiv.dd.com/svn/trunk/html5.js\">" +
          "</script><![endif]--> ";
  private static final String commentFiltered = " ";
  
  //  // No relevant content in header/footer
  //  new TagNameFilter("header"),
  //  new TagNameFilter("footer"),
  private static final String header = "A<div>\n" + 
      "<header id=\"section-header\" class=\"section section-header\">\n" + 
      "<div id=\"zone-user-wrapper\" class=\"zone-wrapper\"></div>\n" + 
      "</header>\n" + 
      "</div>9";
  private static final String headerFiltered = "A<div> </div>9";
  
  private static final String footer = "A<div> " + 
      "<footer id=\"section-footer\" class=\"section section-footer\">\n" + 
      "<div id=\"zone-postscript\" class=\"zone zone-postscript clearfix container-30\"></div>\n" +
      "</footer>\n" + 
      "</div>9";
  private static final String footerFiltered = "A<div> </div>9";
  
  //  // copyright statement may change
  //  HtmlNodeFilters.tagWithAttribute("ul", "class", "copyright-statement"),
  private static final String withCopyright = "A<html class=\"js\" lang=\"en\">\n" +
      "<ul class=\"copyright-statement\">gone<li class=\"fn\">Copyright © 2012 American Society</li>" +
      "</ul></html>9";
  private static final String withoutCopyright = "A<html class=\"js\" lang=\"en\"> </html>9";
  
  //// messages can appear arbitrarily
  //HtmlNodeFilters.tagWithAttributeRegex("div", "id", "messages"),
  private static final String messages = "A<div> " +
      "<div id=\"messages\">arbitrary text" +
      "</div></div>9";
  private static final String messagesFiltered = "A<div> </div>9";
  
  //// extras, prev/next pager and right sidebar may change
  //HtmlNodeFilters.tagWithAttributeRegex("div", "class", "cit-extra"),
  private static final String withCitExtra = "A<html class=\"js\" lang=\"en\">\nx" +
      "<div class=\"cit-extra\">y" +
      "<a href=\"/lookup/external-ref?access_num=10.1056/NEJM200005183422006&amp;link_type=DOI\" " +
      "class=\"cit-ref-sprinkles cit-ref-sprinkles-doi cit-ref-sprinkles-crossref\"><span>CrossRef</span></a>" +
      "<a href=\"/lookup/external-ref?access_num=10816188&amp;link_type=MED&amp;atom=" +
      "%2Fajpcell%2F302%2F1%2FC1.atom\" class=\"cit-ref-sprinkles cit-ref-sprinkles-medline\">" +
      "<span>Medline</span></a>" +
      "<a href=\"/lookup/external-ref?access_num=000087068200006&amp;link_type=ISI\" " +
      "class=\"cit-ref-sprinkles cit-ref-sprinkles-newisilink cit-ref-sprinkles-webofscience\">" +
      "<span>Web of Science</span></a>" +
      "</div></html>9";
  private static final String withoutCitExtra = "A<html class=\"js\" lang=\"en\"> x</html>9";
  
  //HtmlNodeFilters.tagWithAttributeRegex("div", "class", "pane-highwire-node-pager"),
  private static final String withPager = "A<html> " +
      "<div class=\"panel-pane pane-highwire-node-pager\" >X\n" + 
      "<div class=\"pane-content\">\n" + 
      "<div class=\"pager highwire-pager pager-mini clearfix highwire-node-pager " +
      "highwire-article-pager\"><span class=\"pager-prev\"><a href=\"/content/999/9/C91\" " +
      "title=\"Corrigendum\" rel=\"prev\" class=\"pager-link-prev link-icon\">" +
      "<i class=\"icon-circle-arrow-left\"></i> Previous</a></span><span class=\"pager-next\">" +
      "<a href=\"/content/999/9/C99\" title=\"Drive in the oviduct\" rel=\"next\" " +
      "class=\"pager-link-next link-icon link-icon-right\">Next <i class=\"icon-circle-arrow-right\">" +
      "</i></a></span></div>  </div>\n" + 
      "</div></html>9";
  private static final String withoutPager = "A<html> </html>9";
  
  //new TagNameFilter("script"),
  //new TagNameFilter("noscript"),
  private static final String withScript =
      "A<div>\n" +
      "<script type=\"text/javascript\">GA_googleFillSlot(\"tower_right_160x600\");</script>\n" +
      "<noscript type=\"text/javascript\">GA_googleFillSlot(\"tower_right_160x600\");</noscript>\n" +
      "</div>9";
  private static final String withoutScript =
      "A<div> </div>9";
  
  // HtmlNodeFilters.tagWithAttributeRegex("div", "class", "author-tooltip")
//  private static final String withToolTip =
//      "A<html>\n" +
//      "<div class=\"author-tooltip0-asdf\">tip here</div>" +
//      "</html>9";
//  private static final String withoutToolTip =
//      "A<html> </html>9";
  
  private static final String withAside = "<div id=\"page\">" +
      "A<aside>B\n" + 
      " <div class=\"panel-pane pane-service-links\">\n" + 
      "  <div class=\"pane-content\">\n" + 
      "    <div class=\"service-links\">" +
      "    </div>" +
      "  </div>\n" + 
      " </div>\n" + 
      "</aside>\n" +
      "9</div>";
  private static final String withoutAside = 
      "<div>A 9</div>";
  
  private static final String withForm = "<div id=\"page\">" +
      "A<aside>\n" + 
      " <div class=\"panel-pane pane-service-links\">\n" + 
      "  <div class=\"pane-content\">\n" + 
      "    <div class=\"service-links\">" +
      "    </div>" +
      "  </div>\n" + 
      " </div>\n" + 
      "</aside>\n" +
      "9</div>";
  private static final String withoutForm = 
      "<div>A 9</div>";
  
  
  //HtmlNodeFilters.tagWithAttributeRegex("div", "class", "^author-tooltip"),
  private static final String withToolTip = "A<html>\n" +
      "<div class=\"sidebar-right-wrapper grid-10 omega\">X\n" + 
      "<div class=\"panel-panel panel-region-sidebar-right\">\n" + 
      "<div class=\"inside\">" +
      "<div class=\"panel-pane pane-panels-mini " +
      "pane-jnl-iss-issue-arch-art pane-style-alt-content\" >\n" + 
      "</div></div></div></div>\n" +
      "</html>9";
  private static final String withoutToolTip = "A<html> </html>9";
  
  //HtmlNodeFilters.tagWithAttributeRegex("div", "class", "sidebar-right-wrapper"),
  private static final String withSidebar = "A<html> " +
      "<div class=\"sidebar-right-wrapper grid-10 omega\">X\n" + 
      "<div class=\"panel-panel panel-region-sidebar-right\">\n" + 
      "<div class=\"inside\">" +
      "<div class=\"panel-pane pane-panels-mini " +
      "pane-jnl-iss-issue-arch-art pane-style-alt-content\" >\n" + 
      "</div></div></div></div>\n" +
      "</html>9";
  private static final String withoutSidebar = "A<html> </html>9";
  
  
  @Override
  public void testFiltering() throws Exception {
    assertFilterToString(headHtml, headHtmlFiltered);
    assertFilterToString(comment, commentFiltered);
    assertFilterToString(header, headerFiltered);
    assertFilterToString(footer, footerFiltered);
    assertFilterToString(messages, messagesFiltered);
    assertFilterToString(withCopyright, withoutCopyright);
    assertFilterToString(withCitExtra, withoutCitExtra);
    assertFilterToString(withPager, withoutPager);
    assertFilterToString(withScript, withoutScript);
    assertFilterToString(withAside, withoutAside);
    assertFilterToString(withForm, withoutForm);
    
    assertFilterToString(withToolTip, withoutToolTip);
    assertFilterToString(withSidebar, withoutSidebar);
    // HtmlNodeFilters.tagWithAttributeRegex("a", "class", "hw-link"),
    
  }
  
  
  //Don't put the 2nd string through the filter - use it as a constant
  private void assertFilterToString(String orgString, String finalString) throws Exception {
    
    InputStream inA = fact.createFilteredInputStream(mau, new StringInputStream(orgString),
        Constants.DEFAULT_ENCODING);
    String filtered = StringUtil.fromInputStream(inA);
    assertEquals(filtered, finalString, filtered);
  }
  
}
