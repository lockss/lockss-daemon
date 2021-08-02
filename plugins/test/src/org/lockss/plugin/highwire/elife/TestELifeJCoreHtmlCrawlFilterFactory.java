/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.highwire.elife;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestELifeJCoreHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;
  
  private ELifeJCoreHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new ELifeJCoreHtmlCrawlFilterFactory();
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
  
  // <div class="panel-pane pane-elife-article-criticalrelation">
  private static final String withRelation = "<div id=\"page\">" + 
      "<div class=\"panel-pane pane-elife-article-criticalrelation\">\n" + 
      " <div class=\"highwire-markup\">\n" + 
      "  <div class=\"section inner\">\n" + 
      "   <ol class=\"critical-relation-list\"><li class=\"critical-relation first last\">" +
      "    <h3 class=\"critical-relation__title\">" +
      "     <a href=\"/content/3/e03756\">\n" + 
      "      <div>Complexin Ca<sup>2+</sup>-triggered fusion</div>\n" + 
      "     </a>" +
      "    </h3>\n" + 
      "   </li>\n</ol>" +
      "  </div>\n" + 
      " </div>\n" + 
      "</div>" + 
      "</div>";
  private static final String withoutRelation = "<div id=\"page\">" +
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
    
    // "div", "class", "elife-article-criticalrelation"
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withRelation),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutRelation, a);
    
  }
  
}
