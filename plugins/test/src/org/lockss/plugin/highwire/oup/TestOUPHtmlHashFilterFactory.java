/*
 * $Id$
 */

/*

 Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire.oup;

import java.io.*;

import org.lockss.util.*;
import org.lockss.plugin.highwire.TestHighWireDrupalHtmlCrawlFilterFactory;
import org.lockss.test.*;

public class TestOUPHtmlHashFilterFactory extends TestHighWireDrupalHtmlCrawlFilterFactory {
  static String ENC = Constants.DEFAULT_ENCODING;
  
  private OUPHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new OUPHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }
  
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
  
  // HtmlNodeFilters.tagWithAttributeRegex("div", "class", "sidebar-right-wrapper"),
  private static final String withSidebar = "A<html> " +
      "<div class=\"sidebar-right-wrapper grid-10 omega\">X\n" + 
      "<div class=\"panel-panel panel-region-sidebar-right\">\n" + 
      "<div class=\"inside\">" +
      "<div class=\"panel-pane pane-panels-mini " +
      "pane-jnl-iss-issue-arch-art pane-style-alt-content\" >\n" + 
      "</div></div></div></div>\n" +
      "</html>9";
  private static final String withoutSidebar = "A<html> </html>9";
  
  // HtmlNodeFilters.tagWithAttributeRegex("div", "class", "content-header"),
  private static final String withHeader = "A<html>" +
      "<div class=\"content-header\">xx</div>" +
      "</HTML>9";
  private static final String withoutHeader = "A<html></HTML>9";
  
  // HtmlNodeFilters.tagWithAttributeRegex("div", "class", "(citing|related)-articles?"),
  private static final String withArticles = "A<html>\n" +
      "<div class=\"citing-article\">X</div>\n" +
      "<div class=\"citing-articles\">X</div>\n" +
      "<div class=\"related-article\">X</div>\n" +
      "</html>9";
  private static final String withoutArticles = "A<html> </html>9";
  
  // HtmlNodeFilters.tagWithAttributeRegex("div", "class", "-(keywords|by-author|eletters)"),
  private static final String withKeywords = "A<div>\n" + 
      "<div class=\"w-keywords\">X</div>\n" +
      "<div class=\" w-by-author\">X</div>\n" +
      "<div class=\"related w-eletters\">X</div>\n" +
      "</div>9";
  private static final String withoutKeywords = "A<div> </div>9";
  
  // HtmlNodeFilters.tagWithAttribute("div", "class", "panel-separator"),
  private static final String withSep = "A<div>\n" +
      "<div class=\"panel-separator\"> </div>" + 
      "</div>9";
  private static final String withoutSep = "A<div> </div>9";
  
  // OUP author section kept changing formating and spacing
  //  HtmlNodeFilters.allExceptSubtree(
  //      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "highwire-article-citation"),
  //      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "highwire-cite-title")),
  private static final String withCite = "<div id=\"page\">" +
      "A<div class=\"panel-pane pane-highwire-article-citation\">\n" + 
      "  <div class=\"title-access-wrapper\">G\n" + 
      "<div class=\"highwire-cite-title\">Sodium: a randomized double-blind placebo-controlled study</div>" +
      "  </div>H\n" + 
      "  <div class=\"line-height-s\">I\n" + 
      "  <span class=\"highwire-cite-authors add-author-link-processed\">J" +
      "  <span class=\"highwire-citation-authors\">" +
      "  <span data-delta=\"0\" class=\"highwire-citation-author first article-author-popup-processed\">" +
      "  <span class=\"nlm-given-names\">G.K.</span> " +
      "  <span class=\"nlm-surname\">Chester</span> K" +
      "  </span></span></span></span>L\n" + 
      "  </div>\n" + 
      "  </div>\n" + 
      "9</div>";
  // Keeps the subtree that includes highwire-cite-title with all the nested divs (sans attributes)
  private static final String withoutCite = "<div>" +
      "A<div><div>" +
      "<div>Sodium: a randomized double-blind placebo-controlled study</div>" +
      "</div></div> " +
      "9</div>";
  
  
  @Override
  public void testFiltering() throws Exception {
    assertFilterToString(header, headerFiltered);
    assertFilterToString(footer, footerFiltered);
    
    assertFilterToString(withSidebar, withoutSidebar);
    assertFilterToString(withHeader, withoutHeader);
    assertFilterToString(withArticles, withoutArticles);
    assertFilterToString(withKeywords, withoutKeywords);
    assertFilterToString(withSep, withoutSep);
    assertFilterToString(withCite, withoutCite);
  }
  
  
  //Don't put the 2nd string through the filter - use it as a constant
  private void assertFilterToString(String orgString, String finalString) throws Exception {
    
    InputStream inA = fact.createFilteredInputStream(mau, new StringInputStream(orgString),
        Constants.DEFAULT_ENCODING);
    String filtered = StringUtil.fromInputStream(inA);
    assertEquals(filtered, finalString, filtered);
  }
  
}
