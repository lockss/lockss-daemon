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

package org.lockss.plugin.highwire;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestHighWireJCoreHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;
  
  private HighWireJCoreHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new HighWireJCoreHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  
  private static final String withHeader = "<div id=\"page\">" +
      "<header id=\"section-header\" class=\"section section-header\">" +
      "<div class=\"zone-wrapper clearfix zone-wrapper-twotone-bg\" id=\"zone-user-wrapper\">  \n" + 
      "    <div class=\"content clearfix user-menu-dropdown user-menu-dropdown-logo-only\">\n" + 
      "<ul id=\"nice-menu-2\" class=\"nice-menu nice-menu-down sf-js-enabled\">" +
      "<li class=\"menu-644 menuparent first odd last\">" +
      "<a href=\"http://www.the-aps.org/mm/Publications/Journals\">Other Journals from APS</a>" +
      "<ul style=\"display: none; visibility: hidden;\">" +
      "<li class=\"menu-668 menu-path-ajpcellphysiologyorg- first odd \">" +
      "<a title=\"\" href=\"http://ajpcell.physiology.org/\">AJP-Cell</a></li>\n" + 
      "<li class=\"menu-645 menu-path-ajpendophysiologyorg-  even \">" +
      "<a title=\"\" href=\"http://ajpendo.physiology.org/\">AJP-Endo</a></li>\n" + 
      "<li class=\"menu-646 menu-path-ajpgiphysiologyorg-  odd \">" +
      "<a href=\"http://ajpgi.physiology.org/\">AJP-GI</a></li>\n" + 
      "<li class=\"menu-647 menu-path-ajpheartphysiologyorg-  even \">" +
      "<a href=\"http://ajpheart.physiology.org/\">AJP-Heart</a></li>\n" + 
      "<li class=\"menu-648 menu-path-ajplungphysiologyorg-  odd \">" +
      "<a href=\"http://ajplung.physiology.org/\">AJP-Lung</a></li>\n" + 
      "<li class=\"menu-649 menu-path-ajpreguphysiologyorg-  even \">" +
      "<a href=\"http://ajpregu.physiology.org/\">AJP-Regu</a></li>\n" + 
      "<li class=\"menu-652 menu-path-advanphysiologyorg-  odd \">" +
      "<a href=\"http://advan.physiology.org/\">Advances</a></li>\n" + 
      "<li class=\"menu-653 menu-path-japphysiologyorg-  even \">" +
      "<a href=\"http://jap.physiology.org/\">JAPPL</a></li>\n" + 
      "<li class=\"menu-654 menu-path-jnphysiologyorg-  odd \">" +
      "<a title=\"\" href=\"http://jn.physiology.org/\">JN</a></li>\n" + 
      "<li class=\"menu-655 menu-path-physiolgenomicsphysiologyorg-  even \">" +
      "<a href=\"http://physiolgenomics.physiology.org/\">PG</a></li>\n" + 
      "<li class=\"menu-656 menu-path-physrevphysiologyorg-  odd \">" +
      "<a href=\"http://physrev.physiology.org/\">PRV</a></li>\n" + 
      "</ul></li>\n" + 
      "</ul>\n" + 
      "    </div>\n" + 
      "</div>\n" + 
      "</header>\n" +
      "</div>";
  
  private static final String withoutHeader = "<div id=\"page\">" +
      "\n" +
      "</div>";
  
  private static final String withFooter = "<div id=\"page\">" +
      "<footer class=\"section section-footer\" id=\"section-footer\">\n" + 
      "<section id=\"block-menu-menu-navigate\" class=\"block block-menu odd\">\n" + 
      "  <div class=\"block-inner clearfix\">\n" + 
      "    <div class=\"content clearfix\">\n" + 
      "<ul class=\"menu\">" +
      "<li class=\"first leaf\"><a href=\"/content/current\">Current Issue</a></li>\n" + 
      "<li class=\"leaf\"><a href=\"/content/early/recent\">Articles in Press</a></li>\n" + 
      "<li class=\"leaf\"><a href=\"/content/by/year\">Archives</a></li>\n" + 
      "<li class=\"leaf\"><a href=\"/content/feedback\">Feedback</a></li>\n" + 
      "<li class=\"last leaf\"><a href=\"/alerts\">Personal Alerts</a></li>\n" + 
      "</ul>" +
      "    </div>\n" + 
      "  </div>\n" + 
      "</section>\n" + 
      "</footer>\n" +
      "</div>";
  
  private static final String withoutFooter = "<div id=\"page\">" +
      "\n" +
      "</div>";
  
  private static final String withRefList = "<div class=\"article fulltext-view \">\n" + 
      "<span id=\"related-urls\"></span>\n" + 
      "<div id=\"ref-list-1\" class=\"section ref-list\"><h2>REFERENCES</h2>" +
      "<ol class=\"cit-list ref-use-labels\">" +
      "<li><span class=\"ref-label\">1.</span>" +
      "<a id=\"ref-1\" title=\"View reference 1. in text\"" +
      " href=\"#xref-ref-1-1\" class=\"rev-xref-ref\">↵</a>\n" + 
      "\n" + 
      "<div id=\"cit-305.1.E1.1\" class=\"cit ref-cit ref-journal\">" +
      "<div class=\"cit-metadata\">" +
      "<ol class=\"cit-auth-list\">" +
      "<li><span class=\"cit-auth\"><span class=\"cit-name-surname\">Singh</span>" +
      "  <span class=\"cit-name-given-names\">P</span></span>, " +
      "</li>" +
      "</ol>" +
      "<cite>. <span class=\"cit-article-title\">Postgravid obesity</span>." +
      " <abbr class=\"cit-jnl-abbrev\">Obesity (Silver Spring)</abbr>" +
      " <span class=\"cit-vol\">1</span>: <span class=\"cit-fpage\">7</span>&ndash;" +
      "<span class=\"cit-lpage\">8</span>," +
      " <span class=\"cit-pub-date\">2010</span>." +
      "</cite></div>" +
      "<div class=\"cit-extra\">" +
      "<a class=\"cit-ref-sprinkles cit-ref-sprinkles-doi cit-ref-sprinkles-crossref\"" +
      " href=\"/lookup/external-ref?access_num=10.1038/oby.2010.215&amp;link_type=DOI\">" +
      "<span>CrossRef</span></a>" +
      "<a class=\"cit-ref-sprinkles cit-ref-sprinkles-ijlink\"" +
      " href=\"/lookup/ijlink?linkType=ABST&amp;journalCode=diacare&amp;resid=32/6/1076&amp;" +
      "atom=%2Fajpendo%2F305%2F1%2FE1.atom\"><span>" +
      "<span class=\"cit-reflinks-abstract\">Abstract</span>" +
      "<span class=\"cit-sep cit-reflinks-variant-name-sep\">/</span>" +
      "<span class=\"cit-reflinks-full-text\">" +
      "<span class=\"free-full-text\">FREE </span>Full Text</span></span></a>" +
      "</div></div>" +
      "</li>" +
      "</div>" +
      "</div>";
  
  private static final String withoutRefList = "<div class=\"article fulltext-view \">\n" +
      "<span id=\"related-urls\"></span>\n" + 
      "</div>";
  
  private static final String withSidebar = "<div id=\"page\">\n" +
      "<div class=\"sidebar-right-wrapper grid-10 omega\">\n" +
      "      <div class=\"panel-panel panel-region-sidebar-right\">\n" +
      "        <div class=\"inside\"><div class=\"panel-pane pane-panels-mini\">\n" +
      "    <div class=\"minipanel-dialog-wrapper\">" +
      "<div class=\"minipanel-dialog-link-link\">" +
      "<a title=\"Share this\" href=\"/\" class=\".minipanel-dialog-link-link\">" +
      "About the Cover</a>" +
      "</div>" +
      "     </div>" +
      "        </div></div>\n" +
      "      </div><a href=\"/content/347/bmj.f52.full.pdf\" title=\"PDF\" class=\"pdf-link\">PDF</a>\n" +
      "XXX\n" +
      "<div id=\"fig-data-supplementary-materials\" class=\"group frag-supplementary-material\">\n" + 
      "  <div class=\"fig-data-title-jump clearfix\">\n" + 
      "    <h3 class=\"fig-data-group-title\">Supplementary Materials</h3>\n" + 
      "  </div>\n" + 
      "  <div class=\"item-list\">\n" + 
      "    <ul class=\"fig-data-list clearfix\" id=\"fragments-supplementary-material\">\n" + 
      "      <li class=\"first last\">\n" + 
      "        <div class=\"element-fig-data clearfix supplementary-material-caption\">\n" + 
      "          <div class=\"highwire-markup\">\n" + 
      "            <div>\n" + 
      "              <div class=\"supplementary-material-expansion\" id=\"DC1\">\n" + 
      "                <span class=\"highwire-journal-article-marker-start\"></span>\n" + 
      "                <span class=\"supplementary-material-label\">Supplemental Figures&nbsp;1-4</span>\n" + 
      "                <span class=\"inline-linked-media-wrapper\"><span id=\"DC1\" class=\"inline-linked-media\">\n" + 
      "                    <a href=\"/content/btr/2/4/398/DC1/embed/media-1.pdf?download=true\" class=\"\" data-icon-position=\"\" data-hide-link-title=\"0\">\n" + 
      "                    <i class=\"icon-download-alt\"></i>[S2452302X1730133X_mmc1.pdf]</a>\n" + 
      "                </span></span>\n" + 
      "              <span class=\"highwire-journal-article-marker-end\"></span>\n" + 
      "              </div>\n" + 
      "              <span id=\"related-urls\"></span>\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "        </div>\n" + 
      "      </li>\n" + 
      "    </ul>\n" + 
      "  </div>\n" + 
      "</div>" +
      "" +
      "</div>\n" +
      "</div>";
  
  private static final String withoutSidebar = "<div id=\"page\">" +
      "\n" +
      "<div class=\"sidebar-right-wrapper grid-10 omega\">" +
      "<a href=\"/content/347/bmj.f52.full.pdf\" title=\"PDF\" class=\"pdf-link\">PDF</a>" +
      "<div id=\"fig-data-supplementary-materials\" class=\"group frag-supplementary-material\">\n" + 
      "  <div class=\"fig-data-title-jump clearfix\">\n" + 
      "    <h3 class=\"fig-data-group-title\">Supplementary Materials</h3>\n" + 
      "  </div>\n" + 
      "  <div class=\"item-list\">\n" + 
      "    <ul class=\"fig-data-list clearfix\" id=\"fragments-supplementary-material\">\n" + 
      "      <li class=\"first last\">\n" + 
      "        <div class=\"element-fig-data clearfix supplementary-material-caption\">\n" + 
      "          <div class=\"highwire-markup\">\n" + 
      "            <div>\n" + 
      "              <div class=\"supplementary-material-expansion\" id=\"DC1\">\n" + 
      "                <span class=\"highwire-journal-article-marker-start\"></span>\n" + 
      "                <span class=\"supplementary-material-label\">Supplemental Figures&nbsp;1-4</span>\n" + 
      "                <span class=\"inline-linked-media-wrapper\"><span id=\"DC1\" class=\"inline-linked-media\">\n" + 
      "                    <a href=\"/content/btr/2/4/398/DC1/embed/media-1.pdf?download=true\" class=\"\" data-icon-position=\"\" data-hide-link-title=\"0\">\n" + 
      "                    <i class=\"icon-download-alt\"></i>[S2452302X1730133X_mmc1.pdf]</a>\n" + 
      "                </span></span>\n" + 
      "              <span class=\"highwire-journal-article-marker-end\"></span>\n" + 
      "              </div>\n" + 
      "              <span id=\"related-urls\"></span>\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "        </div>\n" + 
      "      </li>\n" + 
      "    </ul>\n" + 
      "  </div>\n" + 
      "</div>" +
      "</div>\n" +
      "</div>";
  
  private static final String withPager = "<div id=\"page\">" +
      "<div class=\"panel-pane pane-highwire-node-pager\">\n" + 
      "  <div class=\"pane-content\">\n" + 
      "    <div class=\"pager highwire-pager pager-mini clearfix highwire-node-pager " +
      "highwire-article-pager\">" +
      "<span class=\"pager-prev\">" +
      "<a class=\"pager-link-prev link-icon\" rel=\"prev\" title=\"receptor neurons\"" +
      " href=\"/content/4/1/E1\">" +
      "<i class=\"icon-circle-arrow-left\"></i> Previous</a></span>" +
      "<span class=\"pager-next\">" +
      "<a class=\"pager-link-next link-icon link-icon-right\" rel=\"next\" title=\"insulin\"" +
      " href=\"/content/5/1/E1\">Next <i class=\"icon-circle-arrow-right\"></i></a></span>" +
      "</div>  </div>\n" + 
      "  </div>\n" +
      "</div>";
  
  private static final String withoutPager = "<div id=\"page\">" +
      "\n" +
      "</div>";
  
  private static final String withAside = "<div id=\"page\">" +
      "<aside>\n" + 
      " <div class=\"panel-pane pane-service-links\">\n" + 
      "  <div class=\"pane-content\">\n" + 
      "    <div class=\"service-links\">" +
      "    </div>" +
      "  </div>\n" + 
      " </div>\n" + 
      "</aside>\n" +
      "</div>";
  
  private static final String withoutAside = "<div id=\"page\">" +
      "\n" +
      "</div>";
  
  private static final String withBreadcrumb = "<div id=\"page\">" +
      "<div id=\"breadcrumb\">\n" + 
      "<ul class=\"breadcrumbs\">\n" + 
      "<li class=\"first crumb\"><a href=\"/\">Home</a></li>\n" + 
      "<li class=\"last crumb\"><a href=\"http://www.journal.org/content/119/9\">March 1, 2012; Journal: 119 (9)</a></li>\n" + 
      "</ul></div>\n" + 
      "</div>";
  
  private static final String withoutBreadcrumb = "<div id=\"page\">" +
      "\n" +
      "</div>";
  
  private static final String withNav = "<div id=\"page\">" +
      "<nav class=\"menubar-nav\">" +
      "<ul class=\"nice-menu nice-menu-down\" id=\"nice-menu-1\" role=\"menu\">" +
      "<li class=\"menu-616 menu-path-front first odd\" role=\"menuitem\">" +
      " <a href=\"/\" class=\"\" data-icon-position=\"\" data-hide-link-title=\"0\">Home</a></li>\n" + 
      "<li class=\"menu-617 menuparent  menu-path-content-current  even\" role=\"menuitem\">" +
      " <a href=\"/content/current\" data-icon-position=\"\" data-hide-link-title=\"0\">Content</a></li>\n" + 
      "</ul></nav>\n" +
      "</div>";
  
  private static final String withoutNav = "<div id=\"page\">" +
      "\n" +
      "</div>";
  
  private static final String withPrevNext =
      "<div class=\"panel-pane pane-highwire-node-pager\">\n" + 
      "  <div class=\"pane-content\">\n" + 
      "    <div class=\"pager highwire-pager pager-mini clearfix highwire-node-pager highwire-issue-pager\">\n" +
      "<span class=\"pager-prev\">\n" +
      "<a href=\"/content/99/6\" title=\"JACC: : 99 (6)\" rel=\"prev\" class=\"pager-link-prev link-icon\">\n" +
      "<i class=\"icon-circle-arrow-left\"></i> <span class=\"title\">Previous</span></a></span>\n" +
      "<span class=\"pager-next\">\n" +
      "<a href=\"/content/100/1\" title=\"JACC: : 100 (1)\" rel=\"next\" class=\"pager-link-next link-icon-right link-icon\">\n" +
      "<span class=\"title\">Next</span> <i class=\"icon-circle-arrow-right\"></i></a></span>\n" +
      "    </div>\n" +
      "  </div>\n" + 
      "</div>";
  
  private static final String withoutPrevNext =
      "<div class=\"panel-pane pane-highwire-node-pager\">\n" + 
      "  <div class=\"pane-content\">\n" + 
      "    <div class=\"pager highwire-pager pager-mini clearfix highwire-node-pager highwire-issue-pager\">\n" + 
      "<span class=\"pager-prev\">\n" + 
      "<a href=\"/content/99/6\" title=\"JACC: : 99 (6)\" rel=\"prev\" class=\"pager-link-prev link-icon\">\n" + 
      "<i class=\"icon-circle-arrow-left\"></i> <span class=\"title\">Previous</span></a></span>\n" + 
      "<span class=\"pager-next\">\n" + 
      "<a href=\"/content/100/1\" title=\"JACC: : 100 (1)\" rel=\"next\" class=\"pager-link-next link-icon-right link-icon\">\n" + 
      "<span class=\"title\">Next</span> <i class=\"icon-circle-arrow-right\"></i></a></span>\n" + 
      "    </div>\n" + 
      "  </div>\n" + 
      "</div>";
  
  /*
   */
  public void testFiltering() throws Exception {
    InputStream inA;
    String a;
    
    // header
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withHeader),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutHeader, a);
    
    // footer
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withFooter),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutFooter, a);
    
    // ref list
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withRefList),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutRefList, a);
    
    // sidebar
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withSidebar),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutSidebar, a);
    
    // node pager
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withPager),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    // assertEquals(withoutPager, a); // XXX no longer filtering pager in Drupal parent class
    
    // aside
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withAside),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutAside, a);
    
    // breadcrumb
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withBreadcrumb),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutBreadcrumb, a);
    
    // nav
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withNav),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutNav, a);
    
    // prev/next
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withPrevNext),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutPrevNext, a);
    
  }
  
}
