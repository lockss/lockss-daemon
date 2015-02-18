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
import org.lockss.test.*;

public class TestAPSHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;
  
  private APSHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new APSHtmlCrawlFilterFactory();
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
    
    // node pager
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withPager),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutPager, a);
    
    // aside
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withAside),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutAside, a);
    
  }
  
}
