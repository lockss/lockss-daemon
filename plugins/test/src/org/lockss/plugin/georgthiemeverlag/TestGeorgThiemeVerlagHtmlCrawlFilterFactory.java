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

package org.lockss.plugin.georgthiemeverlag;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestGeorgThiemeVerlagHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;
  
  private GeorgThiemeVerlagHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;
  
  public void setUp() throws Exception {
    super.setUp();
    fact = new GeorgThiemeVerlagHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  
  private static final String withHeader = "<div id=\"page\">\n" +
      "<header id=\"pageHeader\">\n" + 
      "<div id=\"topHeaderBar\">\n" + 
      "<ul role=\"navigation\" id=\"metaNavigation\">\n" + 
      "<li class=\"langSwitch2German\">\n" + 
      "<a href=\"/ejournals/html/10.1055/s-0032-1321495?locale=de&amp;LgSwitch=1\">DE</a>\n" + 
      "</li>\n" + 
      "<li class=\"langSwitch2English\">\n" + 
      "<span>EN</span>\n" + 
      "</li>\n" + 
      "<li>\n" + 
      "<a href=\"/ejournals/home.html\">Home</a>\n" + 
      "</li>\n" + 
      "</ul>\n" + 
      "</div>\n" + 
      "<div id=\"middleHeaderBar\">\n" + 
      "</div>\n" + 
      "<div id=\"bottomHeaderBar\">\n" + 
      "</div>\n" + 
      "<div id=\"institutionName\">CLOCKSS system has permission to ingest, preserve," +
      " and serve this Archival Unit.</div>\n" + 
      "</header>" +
      "</div>";
  
  private static final String withoutHeader = "<div id=\"page\">\n" +
      "</div>";
  
  private static final String withFooter = "<div id=\"page\">\n" +
      "<footer>\n" + 
      "<div id=\"pageEnd\">\n" + 
      "<a href=\"#top\">Top of Page</a>\n" + 
      "</div>\n" + 
      "<div class=\"clearfix\">\n" + 
      "<div style=\"margin-left:40px\" id=\"footerCenter\">Georg Thieme | " +
      "<a href=\"/ejournals/impressum\">Impressum</a> | " +
      "<a href=\"/ejournals/datenschutz\">Privacy</a>\n" + 
      "</div>\n" + 
      "<div id=\"footerRight\"></div>\n" + 
      "</div>\n" + 
      "</footer>" +
      "</div>";
  
  private static final String withoutFooter = "<div id=\"page\">\n" +
      "</div>";
  
  // div id="navPanel"
  // ul id="overviewNavigation" from issue toc
  private static final String withNav = "<div id=\"page\">\n" +
      "<div id=\"navPanel\">\n" + 
      "<div onclick=\"tabletSlideMenu.toggle()\" id=\"navPanelHandle\"></div>\n" + 
      "<div id=\"navPanelContent\">\n" + 
      "<div id=\"cover\">\n" + 
      "<img width=\"171\" height=\"220\" border=\"0\" class=\"journalCover small\"" +
      " src=\"/media/10.1055-s-00000009/cover_big.jpg\"></div>\n" + 
      "<div id=\"leftNavigation\">\n" + 
      "<ul class=\"linkList\">\n" + 
      "<li>\n" + 
      "<a href=\"/ejournals/issue/10.1055/s-002-23898\">Table of Contents</a>\n" + 
      "</li>\n" + 
      "<li>\n" + 
      "<a href=\"/ejournals/journal/10.1055/s-00000009\">Current Issue</a>\n" + 
      "</li>\n" + 
      "<li>\n" + 
      "<a href=\"/ejournals/sampleIssue/10.1055/s-00000009\">Sample Issue (01/2014)</a>\n" + 
      "</li>\n" + 
      "</ul>\n" + 
      "</div>\n" + 
      "</div>\n" + 
      "</div>" +
      "<ul class=\"tabBar\" id=\"overviewNavigation\">\n" + 
      "<li class=\"tab eFirst\">\n" + 
      "<a href=\"/ejournals/issue/eFirst/10.1055/s-00000009\">eFirst</a>\n" + 
      "</li>\n" + 
      "<li class=\"tab active\">\n" + 
      "<a href=\"/ejournals/journal/10.1055/s-00000009\">Issue</a>\n" + 
      "</li>\n" + 
      "<li class=\"tab\">\n" + 
      "<a href=\"/ejournals/topten/10.1055/s-00000009\">Most Read</a>\n" + 
      "</li>\n" + 
      "</ul>" +
      "</div>";
  
  private static final String withoutNav = "<div id=\"page\">\n" +
      "</div>";
  
  // ul class="literaturliste"
  private static final String withRefList = "<div id=\"page\">\n" +
      "<ul class=\"literaturliste\">\n" + 
      "      \n" + 
      "<a name=\"N67441\" id=\"N67441\"></a>\n" + 
      "<li>\n" + 
      "<h3>References</h3>\n" + 
      "</li>\n" + 
      "\n" + 
      "<li>\n" + 
      "<a name=\"JR11M0235-1\" id=\"JR11M0235-1\"></a><strong>1</strong> Author C," +
      ";  International Group. " +
      "<a href=\"/ejournals/linkout/10.1055/s-0032-1321495/id/JF11M0111-1\">" +
      "Characteristics of development programs</a>. Pediatrics 2004; 1: e5-e6 </li>\n" + 
      "</ul>" + 
      "</div>";
  
  private static final String withoutRefList = "<div id=\"page\">\n" +
      "</div>";
  
  // div id="adSidebarBottom"  div id="adSidebar"
  private static final String withSidebar = "<div id=\"page\">\n" +
      "<div id=\"adSidebarLeft\">\n" + 
      "<script src=\"https://adfarm1.adition.com/js?wp_id=568835\"></script>\n" + 
      "</div>" +
      "</div>";
  
  private static final String withoutSidebar = "<div id=\"page\">\n" +
      "</div>";
  
  // div class="pageFunctions"
  private static final String withPage = "<div id=\"page\">\n" +
      "<div class=\"pageFunctions\">\n" + 
      "<a target=\"_blank\" title=\"Subscribe to RSS\" class=\"functionItem\"" +
      " id=\"setRSSLink\" href=\"/rss/thieme/en/10.1055-s-00000009.xml\"></a>" +
      "<a title=\"Recommend Issue\" class=\"functionItem\" id=\"emailLink\"" +
      " href=\"/ejournals/recommend/issue/10.1055/s-002-23898\"></a>" +
      "<a title=\"Subscribe to Alert Service\" class=\"functionItem\"" +
      " id=\"setAlertLink\" href=\"/ejournals/alerts\"></a>\n" + 
      "</div>" +
      "</div>";
  
  private static final String withoutPage = "<div id=\"page\">\n" +
      "</div>";
  
  // Erratum with link to original article in another AU
  // div class="relatedArticles"  from toc page
  // div class="toggleMenu articleToggleMenu"  from article page
  private static final String withOffAULinks = "<div id=\"page\">\n" +
      "<div class=\"relatedArticles\">\n" + 
      "<div class=\"toggleButton\">\n" + 
      "<a title=\"Original Article\" href=\"javascript:void(0)\">Original Article</a>\n" + 
      "<div class=\"toggleContent\">\n" + 
      "<div class=\"wrapper\">\n" + 
      "<div class=\"articleListing\">\n" + 
      "<div class=\"listItem scientific\">\n" + 
      "<span class=\"articleCategories\"> </span><span class=\"authors\">Author," +
      " Emmanuel: </span><a class=\"articleTitle\"" +
      " href=\"/ejournals/abstract/10.1055/s-0029-1243370\">Neonate: Monitoring</a>" +
      "<span class=\"articleSource\">American Journal of Perinatology; Issue 01, 2010</span>\n" + 
      "<div class=\"articleOptions\">\n" + 
      "<input type=\"hidden\" value=\"ai10.1055/s-0029-1243370_10\" name=\"ai\">\n" + 
      "<ul class=\"splitButton accessOptions open\" id=\"ai10.1055/s-0029\">\n" + 
      "<li class=\"label\">\n" + 
      "<span>Full Text</span>\n" + 
      "</li>\n" + 
      "<li class=\"option\">\n" + 
      "<a href=\"/ejournals/html/10.1055/s-0029-1243370\">HTML</a>\n" + 
      "</li>\n" + 
      "<li class=\"option\">\n" + 
      "<a class=\"anchorb\" href=\"/ejournals/pdf/10.1055/s-0029-1243370.pdf\">PDF (140 kb)</a>\n" + 
      "</li>\n" + 
      "</ul>\n" + 
      "</div>\n" + 
      "</div>\n" + 
      "</div>\n" + 
      "</div>\n" + 
      "</div>\n" + 
      "</div>\n" + 
      "</div>" +
      "" +
      "<div class=\"toggleMenu articleToggleMenu\">\n" + 
      "<div class=\"toggleButton\">Further Information<div class=\"toggleContent\">\n" + 
      "<div class=\"wrapper twoCols\">\n" + 
      "<div class=\"col\">\n" + 
      "        \n" + 
      "<a name=\"N65771\" id=\"N65771\"></a>\n" + 
      "<h3>Address for correspondence and reprint requests</h3>\n" + 
      "\n" + 
      "<a name=\"CO2910erratum-1\" id=\"CO2910erratum-1\"></a>\n" + 
      "<div>Emmanuel Author, M.D., Ph.D. </div>\n" + 
      "<div>Email: <a href=\"mailto:foo@auth.gr\" class=\"anchorb\">foo@auth.gr</a>\n" + 
      "</div>\n" + 
      "\n" + 
      "</div>\n" + 
      "<div class=\"col\">\n" + 
      "<h3>Publication History</h3>\n" + 
      "<p>Publication Date:<br>18 October 2012 (online)</p>\n" + 
      "</div>\n" + 
      "</div>\n" + 
      "</div>\n" + 
      "</div>\n" + 
      "<div class=\"toggleButton\">\n" + 
      "<a title=\"Original Article\" href=\"javascript:void(0)\">Original Article</a>\n" + 
      "<div class=\"toggleContent\">\n" + 
      "<div class=\"wrapper\">\n" + 
      "<div class=\"articleListing\">\n" + 
      "<div class=\"listItem scientific\">\n" + 
      "<span class=\"articleCategories\"> </span>" +
      "<span class=\"authors\">Author: </span><a class=\"articleTitle\"" +
      " href=\"/ejournals/abstract/10.1055/s-0029-124111\">Neonate: Monitoring</a>" +
      "<span class=\"articleSource\">American Journal of Perinatology; Issue 05, 2010</span>\n" + 
      "<div class=\"articleOptions\">\n" + 
      "<input type=\"hidden\" value=\"ai10.1055/s-0029\" name=\"ai\">\n" + 
      "<ul class=\"splitButton accessOptions open\" id=\"ai10.1055/s-0029\">\n" + 
      "<li class=\"label\">\n" + 
      "<span>Full Text</span>\n" + 
      "</li>\n" + 
      "<li class=\"option\">\n" + 
      "<a href=\"/ejournals/html/10.1055/s-0029-1243370\">HTML</a>\n" + 
      "</li>\n" + 
      "<li class=\"option\">\n" + 
      "<a class=\"anchorb\" href=\"/ejournals/pdf/10.1055/s-0029-1243370.pdf\">PDF (140 kb)</a>\n" + 
      "</li>\n" + 
      "</ul>\n" + 
      "</div>\n" + 
      "</div>\n" + 
      "</div>\n" + 
      "</div>\n" + 
      "</div>\n" + 
      "</div>\n" + 
      "</div>" +
      "" +
      "<a class=\"anchorc\" href=\"/ejournals/abstract/10.1055/s-2007-965356\">" +
      "DOI 10.1055/s-2007-965356</a>" + 
      "</div>";
  
  private static final String withoutOffAULinks = "<div id=\"page\">\n" +
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
    
    // footer
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withNav),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutNav, a);
    
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
    
    // page functions
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withPage),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutPage, a);
    
    // off page links
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withOffAULinks),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutOffAULinks, a);
    
  }
  
}
