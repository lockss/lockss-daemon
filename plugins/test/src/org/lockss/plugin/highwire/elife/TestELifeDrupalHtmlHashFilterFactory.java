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

package org.lockss.plugin.highwire.elife;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestELifeDrupalHtmlHashFilterFactory extends LockssTestCase {
//  private static String ENC = Constants.DEFAULT_ENCODING;
  
  private ELifeDrupalHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new ELifeDrupalHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }
  
  // Publisher adding/updating meta tags
  // new TagNameFilter("head"),
  private static final String headHtml = "<html><head>Title\n</head></HTML>";
  private static final String headHtmlFiltered = "";
  
  // No relevant content in header/footer
  // new TagNameFilter("header"),
  // new TagNameFilter("footer"),
  private static final String header = "<div>A \n" + 
      "<header id=\"section-header\" class=\"section section-header\">\n" + 
      "<div id=\"zone-user-wrapper\" class=\"zone-wrapper\"></div>\n" + 
      "</header>\n9" + 
      "</div>";
  private static final String headerFiltered = "A 9";
  
  private static final String footer = "<div>A \n" + 
      "<footer id=\"section-footer\" class=\"section section-footer\">\n" + 
      "<div id=\"zone-postscript\" class=\"zone zone-postscript clearfix container-30\"></div>\n" +
      "</footer>9 \n" + 
      "</div>";
  private static final String footerFiltered = "A 9 ";
  
  // new TagNameFilter("script"),
  private static final String withScript =
      "<div>" +
      "<script type=\"text/javascript\">GA_googleFillSlot(\"tower_right_160x600\");</script>" +
      "</div>";
  private static final String withoutScript = "";
  
  private static final String withRRHeader = "<div id=\"page\">" +
      "<div id=\"region-responsive-header\" class=\"region-responsive-header\">\n" + 
      "  <div class=\"region-inner region-responsive-header-inner\">\n" + 
      "    <div id=\"block-panels-mini-jnl-elife-responsive-bar\" class=\"block\">\n" + 
      "  <div class=\"block-inner clearfix\">\n" + 
      "                \n" + 
      "    <div class=\"content clearfix\">\n" + 
      "      <div id=\"responsive_bar\" class=\"panel-display clearfix\">\n" + 
      "</div>\n" + 
      "  </div>\n" + 
      "  </div>\n" + 
      "</div>  </div>\n" + 
      "</div> " +
      "</div>";
  
  private static final String withoutRRHeader = " ";
  
  // HtmlNodeFilters.tagWithAttribute("div", "id", "zone-header-wrapper"),
  private static final String zheader = "<div> \n" +
      "<div id=\"zone-header-wrapper\" class=\"zone-wrapper zone-header-wrapper clearfix\">" +
      "</div>" +
      "</div>";
  private static final String zheaderFiltered = " ";
  
  // HtmlNodeFilters.tagWithAttribute("div", "class", "page_header"),
  private static final String pheader = "<div>" +
      "<div class=\"page_header\" role=\"banner\">\n" + 
      "<a class=\"header__identity\" href=\"/\">\n" + 
      "<img alt=\"eLife\" src=\"http://dex3165296d6d.cloudfront.net/images/elife-identity-header.jpg\">\n" + 
      "</a>\n" + 
      "</div>\n " + 
      "</div>";
  private static final String pheaderFiltered = " ";
  
  // HtmlNodeFilters.tagWithAttribute("ul", "class", "elife-article-categories"),
  private static final String artcat = "<div> \nA" +
      "<ul class=\"elife-article-categories\">\n" + 
      "<li class=\"first\"><a class=\"category-display-channel\" href=\"/category/research-article\">Research article</a></li>\n" + 
      "<li><a class=\"category-heading\" href=\"/category/genes-and-chromosomes\">Genes and chromosomes</a></li>\n" + 
      "<li><a class=\"keyword\" href=\"/browse?keys=%22gene%20regulation%22\">gene regulation</a></li>\n" + 
      "<li class=\"last\"><a class=\"keyword\" href=\"/browse?keys=%22Mouse%22\">Mouse</a></li>\n" + 
      "</ul>" +
      "</div>";
  private static final String artcatFiltered = " A";
  
  // HtmlNodeFilters.tagWithAttributeRegex("div", "class", "elife-reflink-links-wrapper"),
  private static final String withRef = "<div id=\"page\">" +
      "<div id=\"references\">" +
      "<div class=\"elife-reflink-links-wrapper\">" +
      "<span class=\"elife-reflink-link life-reflink-link-doi\">" +
      "<a target=\"_blank\" href=\"/lookup/external-ref/doi?access_num=10&amp;link_type=DOI\">" +
      "CrossRef</a></span><span class=\"elife-reflink-link life-reflink-link-medline\">" +
      "<a target=\"_blank\" href=\"/lookup/external-ref/medline?access_num=1&amp;link_type=MED\">" +
      "PubMed</a></span></div>" +
      "</div>9" +
      "</div>";
  
  private static final String withoutRef = "9";
  
  // HtmlNodeFilters.tagWithAttributeRegex("div", "class", "sidebar-wrapper"),
  private static final String withSidebar = "<div> \n" +
      "<div class=\"sidebar-wrapper grid-9 omega\">\n" + 
      "      <div class=\"panel-panel panel-region-sidebar-lens\">\n" + 
      "        <div class=\"panel-pane pane-elife-article-lens-icon hidden-small\">\n" + 
      "        </div>\n" + 
      "      </div>\n" + 
      "</div>\n" + 
      "</div>";
  private static final String withoutSidebar = " ";
  
//  private static final String attrib = "<div>" +
//  		"<div id=\"foo-categories\">stuff\n" + 
//      "</div></div>";
//  private static final String attribFiltered = "stuff ";
  
  
  public void testFiltering() throws Exception {
    // from parent
    assertFilterToString(headHtml, headHtmlFiltered);
    assertFilterToString(header, headerFiltered);
    assertFilterToString(footer, footerFiltered);
    assertFilterToString(withScript, withoutScript);
    // from crawl filter
    assertFilterToString(withRRHeader, withoutRRHeader);
    assertFilterToString(withRef, withoutRef);
    assertFilterToString(withSidebar, withoutSidebar);
    
    assertFilterToString(zheader, zheaderFiltered);
    assertFilterToString(pheader, pheaderFiltered);
    assertFilterToString(artcat, artcatFiltered);
//    assertFilterToString(attrib, attribFiltered);
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
