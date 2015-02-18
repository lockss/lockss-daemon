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

public class TestELifeDrupalHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;
  
  private ELifeDrupalHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new ELifeDrupalHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  
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
      "</div>" +
      "</div>";
  
  private static final String withoutRRHeader = "<div id=\"page\">" +
      "</div>";
  
  private static final String withRef = "<div id=\"page\">" +
      "<div id=\"references\">" +
      "<div class=\"elife-reflink-links-wrapper\">" +
      "<span class=\"elife-reflink-link life-reflink-link-doi\">" +
      "<a target=\"_blank\" href=\"/lookup/external-ref/doi?access_num=10&amp;link_type=DOI\">" +
      "CrossRef</a></span><span class=\"elife-reflink-link life-reflink-link-medline\">" +
      "<a target=\"_blank\" href=\"/lookup/external-ref/medline?access_num=1&amp;link_type=MED\">" +
      "PubMed</a></span></div>" +
      "</div>" +
      "</div>";
  
  private static final String withoutRef = "<div id=\"page\">" +
      "</div>";
  
  // HtmlNodeFilters.tagWithAttributeRegex("div", "class", "sidebar-wrapper"),
  private static final String withSidebar = "<div>\n" +
      "<div class=\"sidebar-wrapper grid-9 omega\">\n" + 
      "      <div class=\"panel-panel panel-region-sidebar-lens\">\n" + 
      "        <div class=\"panel-pane pane-elife-article-lens-icon hidden-small\">\n" + 
      "        </div>\n" + 
      "      </div>\n" + 
      "</div>\n" + 
      "</div>";
  private static final String withoutSidebar = "<div>\n" + 
      "\n" + 
      "</div>";
  
  private static final String withCorr = "<div id=\"page\">" +
      "<div class=\"elife-article-corrections\">" +
      "<span class=\"elife-article-correction\"><a href=\"/content\">This article has been corrected</a>" +
      "</span></div>" +
      "</div>";
  private static final String withoutCorr = "<div id=\"page\">" +
      "</div>";
  
  
  public void testFiltering() throws Exception {
    InputStream inA;
    String a;
    
    // "div", "id", "region-responsive-header"
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withRRHeader),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutRRHeader, a);
    
    // "div", "id", "references"
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withRef),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutRef, a);
    
    // div", "class", "sidebar-wrapper"
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withSidebar),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutSidebar, a);
    
    // "div", "class", "elife-article-corrections"
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withCorr),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutCorr, a);
   
  }
  
}
