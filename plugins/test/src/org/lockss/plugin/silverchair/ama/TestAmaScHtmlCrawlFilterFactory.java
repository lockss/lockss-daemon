/*
 * $Id$
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair.ama;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestAmaScHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;
  
  private AmaScHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new AmaScHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  
  private static final String withHeader = "<div id=\"page\">" +
      "<section class=\"master-header\">\n" + 
      "        \n" + 
      "<nav id=\"nav\" class=\"nav\">\n" + 
      "    <div class=\"nav-journal-level\">\n" + 
      "    </div>\n" + 
      "    <div class=\"nav-journals\">\n" + 
      "        <h2 id=\"BodyContent_Header_JournalHeader\" class=\"nav-heading\">Journals</h2>\n" + 
      "    </div>\n" + 
      "    <div class=\"nav-network-other\">\n" + 
      "    </div>\n" + 
      "</nav>\n" + 
      "</section>\n" +
      "</div>";
  
  private static final String withoutHeader = "<div id=\"page\">" +
      "\n" +
      "</div>";
  
  private static final String withFooter = "<div id=\"page\">" +
      "<section id=\"secFooterControl\" class=\"master-footer\">\n" + 
      "<div class=\"footer-wrap\">\n" + 
      "    <footer class=\"footer\">\n" + 
      "    </footer>\n" + 
      "</div>\n" + 
      "</section>\n" +
      "</div>";
  
  private static final String withoutFooter = "<div id=\"page\">" +
      "\n" +
      "</div>";
  
  private static final String withRefList = "<div class=\"article-full-text\">\n" +
      "<a class=\"article-section-id-anchor\" id=\"126815515\"></a>\n" + 
      "<div class=\"references\">" +
      "<div class=\"reference\"><a class=\"reference-number\" id=\"ier160003r1\">1.</a>" +
      "<div class=\"reference-content\">Selby\n" + 
      "        &nbsp;JV, Fireman\n" + 
      "        &nbsp;BH, Swain\n" + 
      "</div></div></div>" + 
      "</div>";
  
  private static final String withoutRefList = "<div class=\"article-full-text\">\n" +
      "<a class=\"article-section-id-anchor\" id=\"126815515\"></a>\n" +
      "</div>";
  
  private static final String withSidebar = "<div id=\"page\">" +
      "<div class=\"sidebar-right-wrapper grid-10 omega\">\n" +
      "</div>\n" +
      "</div>";
  
  private static final String withoutSidebar = "<div id=\"page\">" +
      "\n" +
      "</div>";
  
  private static final String withAside = "<div id=\"page\">" +
      "<aside>\n" + 
      "</aside>\n" +
      "</div>";
  
  private static final String withoutAside = "<div id=\"page\">" +
      "\n" +
      "</div>";
  
  private static final String withBreadcrumb = "<div id=\"page\">" +
      "<div id=\"breadcrumb\">\n" + 
      "<ul class=\"breadcrumbs\">\n" + 
      "</ul></div>\n" + 
      "</div>";
  
  private static final String withoutBreadcrumb = "<div id=\"page\">" +
      "\n" +
      "</div>";
  
  private static final String withNav = "<div id=\"page\">" +
      "<nav class=\"menubar-nav\">" +
      "<ul class=\"nice-menu nice-menu-down\" id=\"nice-menu-1\" role=\"menu\">" +
      "</ul></nav>" +
      "</div>";
  
  private static final String withoutNav = "<div id=\"page\">" +
      "\n" +
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
    
  }
  
}
