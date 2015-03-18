/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the \"Software\"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.plugin.atypon.endocrinesociety;

import java.io.*;

import junit.framework.Test;

import org.lockss.util.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.test.*;

public class TestEndocrineSocietyHtmlFilterFactory
  extends LockssTestCase {
  
  FilterFactory variantFact;
  ArchivalUnit esau;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;
        
  private static final String PLUGIN_NAME = 
      "org.lockss.plugin.atypon.endocrinesociety.ClockssEndocrineSocietyPlugin";
  
  private static final String filteredStr = 
      "<div class=\"block\"></div>";

  // test for pdf and pdfplus file size
  // is in TestBaseAtyponHtmlHashFilterFactory since the html is similar
  // <a class="ref nowrap pdf" target="_blank" 
  // title="Opens new window" 
  // href="/doi/pdf/10.1210/en.2013-1182">PDF (52 KB)</a>
 
  // top right of issue toc - links to previous or next issue
  private static final String withNavJournal = 
      "<div class=\"block\">" +
          "<div class=\"widget general-html none nav-journal " +
          "widget-none  widget-compact-all\" id=\"d97a\">  " +
          "<div class=\"widget-body body body-none  body-compact-all\">" +
          "<div class=\"nav-journal\">" +
          "<ul>" +
          "<li><a href=\"/toc/jid/0/0\">early</a></li>" +
          "<li><a href=\"/toc/jid/current\">curren</a></li>" +
          "<li><a href=\"/loi/jid\">past</a></li>" +
          "<li><a href=\"/page/jid/about\">About</a></li>" +
          "</ul>" +
          "</div></div> +" +
          "</div>" +
          "</div>";
  
  private static final String withLiteratumBreadcrumbs =
      "<div class=\"block\">" +
          "<div id=\"de5\" class=\"widget literatumBreadcrumbs none " +
          "breadcrumbs widget-none widget-compact-all\">" +
          "<div class=\"wrapped \">" +
          "<div class=\"widget-body body body-none body-compact-all\">" +
          "<nav>" +
          "<ul class=\"breadcrumbs\">" +
          "<li class=\"\">" +
          "<a href=\"/\">Home</a>" +
          "<span class=\"divider\">></span>" +
          "</li>" +
          "<li class=\"\">" +
          "<a href=\"/journal/jid\">Endocrine Reviews</a>" +
          "<span class=\"divider\">></span>" +
          "</li>" +
          "<li class=\"\">" +
          "<a href=\"/loi/jid\">List of Issues</a>" +
          "<span class=\"divider\">></span>" +
          "</li>" +
          "<li class=\"\"> Volume 999, Issue 999 </li>" +
          "</ul>" +
          "</nav>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>";
 
  // from toc - access icon container 
  private static final String withAccessIconContainer =  
      "<div class=\"block\">" +
        "<td class=\"accessIconContainer\"><div></div></td>" +
      "</div>"; 
  
  private static final String withMathJaxMessage =
      "<div class=\"block\">" +  
          "<div id=\"MathJax_Message\" style=\"display: none;\"></div>" +
          "</div>";
  
  private static final String withSectionJumpTo =
      "<div class=\"block\">" +        
          "<div class=\"sectionJumpTo\">" +
          "<div class=\"sectionLabel\">Section:</div>" +
          "<form style=\"margin-bottom:0\">" +
          "<select class=\"full\" onchange=\"GoTo(this, 's')\" name=\"s23\">" +
          "<option selected=\"#\" value=\"#\">Choose</option>" +
          "<option value=\"\">Abstract <<</option>" +
          "<option value=\"#_i1\">I. intro</option>" +
          "<option value=\"#_i2\">II. abc ...</option>" +
          "<option value=\"#_i30\">XII. dfe</option>" +
          "<option value=\"#_i33\">ref</option>" +
          "<option value=\"#citart1\">CITING ARTICLES</option>" +
          "</select>" +
          "</form>" +
          "</div>" +
          "</div>" ;
  
  // from toc - top panel with 'subscribe'
  private static final String withGutterless =  
      "<div class=\"block\">" +
          "<div class=\"pb-columns row-fluid gutterless\">" +
          "<div class=\"widget general-image none  widget-none  " +
          "widget-compact-all\" id=\"5bd\">" +
          "<div class=\"widget-body body body-none  body-compact-all\">" +
          "<a href=\"/journal/jid\"><img src=\"/imagesrc/header.jpg\"></a>" +
          "</div>" +
          "</div>" +
          "<div class=\"widget general-image alignRight " +
          "subsButtonHead widget-none  widget-compact-all\" id=\"7ae\">" +
          "<div class=\"widget-body body body-none  body-compact-all\">" +
          "<a href=\"http://www.example.com/e781\">" +
          "<img src=\"/imagesrc/subscribe.png\"></a></div>" +
          "</div>" +
          "</div>" +
          "</div>";
          
  // top right of issue toc - links to previous or next issue
  private static final String withLiteratumBookIssueNavigation = 
      "<div class=\"block\">" +
          "<div class=\"widget literatumBookIssueNavigation none  " +
          "widget-none\" id=\"xxx\">" +
          "<div class=\"wrapped \">" +
          "<div class=\"widget-body body body-none \">" +
          "<div class=\"pager issueBookNavPager\">" +
          "<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">" +
          "<tbody><tr><td class=\"journalNavLeftTd\">" +
          "<div class=\"prev placedLeft\">" +
          "<a href=\"/toc/yyyy/154/3\">Previous Issue</a></div></td>" +
          "<td class=\"journalNavRightTd\">" +
          "<div class=\"next\"><a href=\"/toc/endo/154/5\">Next Issue</a>" +
          "</div></td></tr>" +
          "</tbody>" +
          "</table>" +
          "</div></div></div>" +
          "</div>" +
          "</div>";
  
  private static final String withFreeGif =
      "<div class=\"block\">" +
          "<img src=\"/imagessrc/free.gif\">" +
          "</div>";
  
  private static final String withCitedby =
      "<div class=\"block\">" + 
          "<ul><li>" +
          "<a href=\"/doi/citedby/11.1111/jid.20129999\"> Cited by </a>" +
          "</li></ul>" +
          "</div>"; 
  private static final String withoutCitedby =
      "<div class=\"block\">" + 
          "<ul></ul>" +
          "</div>";   
  
  // right column of an article - all except Download Citations
  private static final String withRightColumnExceptDownloadCitation = 
      "<div class=\"block\">" +
          "<div class=\"widget literatumRightSidebar none right-sidebar " +
          "widget-none\" id=\"pageRightSidebar\">" +
          "<div class=\"sidebar sidebar-right\">" +
          "<div data-pb-dropzone=\"main\">" +
          "<div class=\"widget general-widget\" id=\"xxx\">" +
          "<div class=\"widget-body\"><a href=\"/act/s\">Authen</a></div>" +
          "</div>" +
          "<div class=\"widget-box\" id=\"yyy\">" +
          "<div class=\"body-box \">" +
          "<div class=\"artTools\">" +
          "<ul class=\"linkList\">" +
          "<li class=\"title\">Article/Chapter Tools</li>" +
          "<li class=\"downloadCitations\">" +
          "<a href=\"/action/showCitFormats?doi=11.1111%2Fen.2012-1111\">" +
          "Download Citation</a>" +
          "</li>" +
          "<li class=\"traCitations\">" +
          "<a href=\"/action/addCitAlert?doi=11.1111%2Fen.2012-1111\">Ta</a>" +
          "</li>" +
          "</ul>" +
          "</div></div></div></div></div></div>" +
          "</div>";
  
  // 'id' attributes also removed
  private static final String rightColumnFiltered = 
      "<div class=\"block\">" +
      "<div class=\"widget literatumRightSidebar none right-sidebar " +
      "widget-none\" >" +
      "<div class=\"sidebar sidebar-right\">" +
      "<div data-pb-dropzone=\"main\">" +
      "<div class=\"widget-box\" >" +
      "<div class=\"body-box \">" +
      "<div class=\"artTools\">" +
      "<ul class=\"linkList\">" +
      "<li class=\"downloadCitations\">" +
      "<a href=\"/action/showCitFormats?doi=11.1111%2Fen.2012-1111\">" +
      "Download Citation</a>" +
      "</li>" +
      "</ul>" +
      "</div></div></div></div></div></div>" +
      "</div>";
  
  // related content near Erratum
  // http://press.endocrine.org/toc/endo/154/10
  private static final String withRelatedLayer =
      "<div class=\"block\">" +
          "<div class=\"relatedLayer\"><div class=\"category\">" +
          "<h3>Original Article</h3>" +
          "<ul><li><a href=\"/doi/abs/11.1111/en.2012-1111\">" +
          "XXX Article title</a></li></ul>" +
          "</div></div>" +
          "</div>";
  
  // related content from Related tab of Errata full text
  // http://press.endocrine.org/doi/full/10.1210/en.2013-1802
  private static final String withRelatedContent =
      "<div class=\"block\">" +
          "<div class=\"tab tab-pane active\" id=\"relatedContent\">" +
          "<div class=\"category\">" +
          "<h3>Original Article</h3>" +
          "<ul><li>" +
          "<a href=\"/doi/abs/11.1111/en.2012-1111\">XXX Artitle title</a>" +
          "</li></ul>" +
          "</div></div>" +
          "</div>";
  
  // external links within Table figures or middle of a paragraph
  // ex: http://press.endocrine.org/doi/full/10.1210/en.2012-2254
  //     http://press.endocrine.org/doi/full/10.1210/en.2012-1768
  //     http://press.endocrine.org/doi/full/10.1210/en.2012-1820
  private static final String withExtLink =
      "<div class=\"block\">" +
          "<a href=\"NM_000001\" class=\"ext-link\" target=\"_blank\" " +
          "title=\"External link\">NM_000001</a>" +
           "</div>";
  
  // pageheader
  private static final String withPageHeader =  
      "<div class=\"block\">" +
          "<div class=\"widget pageHeader none header widget-none  " +
          "widget-compact-all\" id=\"pageHeader\">" +
          "<div class=\"widget-body body body-none  body-compact-all\">" +
          "<div class=\"page-header\">" +
          "<div class=\"widget doubleClickAd alignCenter leaderBoard " +
          "widget-none  widget-compact-all\" id=\"1e3\">" +
          "</div>" +
          "</div></div>" +
          "</div>" +
          "</div>";        
  
  // pageFooter
  private static final String withPageFooter =  
      "<div class=\"block\">" +
          "<div class=\"widget pageFooter none container-footer " +
          "widget-none  widget-compact-all\" id=\"pageFooter\">" +
          "<div class=\"widget-body body body-none  body-compact-all\">" +
          "<div class=\"page-footer\">" +
          "<div class=\"copyright\">" +
          "<p>&copy; 2014 Endocrine Society</p>" +
          "</div></div></div>" +
          "</div>" +
          "</div>"; 
  
  // showCitFormats - Support and Help block
  // http://press.endocrine.org/action/showCitFormats?doi=10.1210%2Fjc.2013-1811
  private static final String withTwoColumnRightDropZoneColor =  
      "<div class=\"block\">" +
          "<div class=\"twoColumnRight twoColumnRightDropZoneColor \">" +
          "<div data-pb-dropzone=\"right\">" +
          "<div id=\"pageRightSidebar\" class=\"wid literatumRightSidebar " +
          "none right-sidebar widget-none\">" +
          "<div class=\"widget-body body-none body-separator body-shadow " +
          "body-border-toggle body-rounded body-alternate\">" +
          "<ul>" +
          "<li><a href=\"http://example.com/page/firefox-pdf-issues\">" +
          "Opening PDFs with Firefox</a></li>" +
          "<li><a href=\"http://example.com/page/login-issues\">" +
          "Sign In and Authentication</a></li>" +
          "</ul>" +
          "</div></div></div></div>" +
          "</div>";
               
  private static final String withArticleMetaDrop =
      "<div class=\"block\">" +  
          "<div class=\"articleMetaDrop publicationContentDropZone\" " +
          "data-pb-dropzone=\"articleMetaDropZone\">" +
          "<div id=\"3ab\" class=\"widget gwidget-compact-vertical\">" +
          "<div class=\"wrapped \">" +
          "<div class=\"widget-body body body-none body-compact-vertical\">" +
          "<p><a href=\"/doi/11.1111/en.2014-1111\" " +
          "title=\"Blah corrigendum in the journal\">" +
          "<span style=\"color: red;\">Corrigendum</span></a></p>" +
          "</div></div></div></div>" +
          "</div>";     
  
  protected ArchivalUnit createAu()
      throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_NAME,  esAuConfig());
  }
  
  private Configuration esAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", "http://www.example.com/");
    conf.put("journal_id", "abc");
    conf.put("volume_name", "99");
    return conf;
  }
  
  private static void doFilterTest(ArchivalUnit au, 
      FilterFactory fact, String nameToHash, String expectedStr) 
          throws PluginException, IOException {
    InputStream actIn; 
    actIn = fact.createFilteredInputStream(au, 
        new StringInputStream(nameToHash), Constants.DEFAULT_ENCODING);
    assertEquals(expectedStr, StringUtil.fromInputStream(actIn));
  }
  
  public void startMockDaemon() {
    daemon = getMockLockssDaemon();
    pluginMgr = daemon.getPluginManager();
    pluginMgr.setLoadablePluginsReady(true);
    daemon.setDaemonInited(true);
    pluginMgr.startService();
    daemon.getAlertManager();
    daemon.getCrawlManager();
  }
  
  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = setUpDiskSpace();
    startMockDaemon();
    esau = createAu();
  }
  
  // Variant to test with Crawl Filter
  public static class TestCrawl extends TestEndocrineSocietyHtmlFilterFactory {
    public void testFiltering() throws Exception {
      variantFact = new EndocrineSocietyHtmlCrawlFilterFactory();
      doFilterTest(esau, variantFact, withLiteratumBookIssueNavigation, 
          filteredStr);
      doFilterTest(esau, variantFact, withRelatedLayer, filteredStr);      
      doFilterTest(esau, variantFact, withRelatedContent, filteredStr);      
      doFilterTest(esau, variantFact, withExtLink, filteredStr);      
    }    
  }

  // Variant to test with Hash Filter
   public static class TestHash extends TestEndocrineSocietyHtmlFilterFactory {   
     public void testFiltering() throws Exception {
      variantFact = new EndocrineSocietyHtmlHashFilterFactory();
      doFilterTest(esau, variantFact, withPageHeader, filteredStr);
      doFilterTest(esau, variantFact, withNavJournal, filteredStr);
      doFilterTest(esau, variantFact, withAccessIconContainer, filteredStr);
      doFilterTest(esau, variantFact, withGutterless, filteredStr);
      doFilterTest(esau, variantFact, withRightColumnExceptDownloadCitation, 
                   rightColumnFiltered);
      doFilterTest(esau, variantFact, withPageFooter, filteredStr);
      doFilterTest(esau, variantFact, withLiteratumBreadcrumbs, filteredStr);
      doFilterTest(esau, variantFact, withLiteratumBookIssueNavigation, 
                   filteredStr);
      doFilterTest(esau, variantFact, withFreeGif, filteredStr);
      doFilterTest(esau, variantFact, withMathJaxMessage, filteredStr);
      doFilterTest(esau, variantFact, withCitedby, withoutCitedby); 
      doFilterTest(esau, variantFact, withSectionJumpTo, filteredStr);
      doFilterTest(esau, variantFact, withTwoColumnRightDropZoneColor, 
                   filteredStr);
      doFilterTest(esau, variantFact, withArticleMetaDrop, filteredStr);
    }
  }
  
  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
      });
  }
  
}

