/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.emeraldgroup;

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

public class TestEmeraldGroupHtmlFilterFactory extends LockssTestCase {
  
  FilterFactory variantFact;
  ArchivalUnit eau;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;
        
  private static final String PLUGIN_ID = 
      "org.lockss.plugin.atypon.emeraldgroup.EmeraldGroupPlugin";
  
  private static final String filteredStr = 
      "<div class=\"block\"></div>";
      
  // toc, abs, full - panel under breadcrubs with Current Issue,
  // Available Issues, Most Cited, Most Read, Alerts RSS, Add to favorites
  // http://www.emeraldinsight.com/toc/aaaj/26/8
  private static final String withCurrIssue =
      "<div class=\"block\">" +
          "<ul>" +
          "<li id=\"currIssue\">" +
          "<a href=\"http://www.example.com/toc/jia/28/2\">Current Issue</a>" +
          "</li>" +
          "<li id=\"allIssues\">" +
          "<li id=\"mostCited\">" +
          "<li id=\"mostRead\">" +
          "</ul>" +
          "</div>";  
  private static final String currIssueFilteredStr =
      "<div class=\"block\">" +
          "<ul>" +
          "<li id=\"allIssues\">" +
          "<li id=\"mostCited\">" +
          "<li id=\"mostRead\">" +
          "</ul>" +
          "</div>";   
  
  // toc, abs, full - previous/next issue/article
  private static final String withLiteratumBookIssueNavigation =
      "<div class=\"block\">" +      
          "<div id=\"4a1\" " +
          "class=\"widget literatumBookIssueNavigation widget-compact-all\">" +
          "<div class=\"widget-body body body-none body-compact-all\">" +
          "<div class=\"prev\">" +
          "<a href=\"/doi/full/11.1111/jid-1358\">Previous Article</a>" +
          "</div>" +
          "<div class=\"next\">" +
          "<a href=\"/doi/full/11.1111/jid-1264\">Next Article</a>" +
          "</div></div>" +
          "</div>" +
          "</div>";
  
  // toc, abs, full -  right column
  // there are 2 data-pb-dropzone="right", one of them is part of the top ad
  // it's not unique tag, but I think it's OK for Emerald
  // http://www.emeraldinsight.com/toc/aaaj/26/8  
  private static final String withDataPbDropzoneRight =
      "<div class=\"block\">" +
          "<div class=\"pb-autoheight\" data-pb-dropzone=\"right\">" +
          "<div id=\"f84\" class=\"layout-tabs widget-compact-all\">" +
          "<div class=\"widget-body body body-none body-compact-all\">" +
          "<div class=\"tabs tabs-widget\">" +
          "<ul class=\"tab-nav\">" +
          "<li class=\"active\"><a href=\"#f59\">Most Read</a></li>" +
          "</ul>" +
          "</div></div></div></div></div>";
  
  // for testcrawl
  // from abs - all Articles Options and Tools except Download Citation   
  private static final String withArticleOptionsExceptDownloadCitation1 =
      "<div class=\"block\">" +  
          "<div class=\"options\">" +
          "<div class=\"subtitle\">Article Options and Tools</div>" +
          "<ul class=\"tools3\">" +
          "<li class=\"addToFav\"></li>" +
          "<li class=\"citations\">" +
          "<a class=\"ref nowrap\" target=\"_blank\" " +
          "href=\"/action/showCitFormats?doi=11.1111%2Fjid-1360\">" +
          "Download Citation</a>" +
          "</li>" +
          "<li class=\"citationAlert\"></li>" +
          "<li class=\"permissions\"></li>" +
          "</ul>" +
          "</div>" +
          "</div>";  
  // id tag also got filtered
    private static final String articleOptionsFilteredStr1 = 
      "<div class=\"block\">" +  
          "<div class=\"options\">" +
          "<ul class=\"tools3\">" +
          "<li class=\"citations\">" +
          "<a class=\"ref nowrap\" target=\"_blank\" " +
          "href=\"/action/showCitFormats?doi=11.1111%2Fjid-1360\">" +
          "Download Citation</a>" +
          "</li>" +
          "</ul>" +
          "</div>" +
          "</div>";
    
    // includeNodes has <div class="literatumTocWidget">
    // exludeNodes has <div class="toc-actions">
    private static final String withTocActions = 
        "<div class=\"block\">" +  
            "<div class=\"literatumTocWidget\">" +
            "<div class=\"toc-actions\">" +
            "<div class=\"pull-left\">" +
            "<ul class=\"linkList blockLinks verticalMarginless\">" +
            "<li><a class=\"toolsLinks\" href=\"javascript:submitArticles(" +
            "document.frmAbs, '/action/addCitationAlert');\"> " +
            "Track Citations </a></li>" +
            "<li><a class=\"toolsLinks\" href=\"javascript:submitArticles(" +
            "document.frmAbs, '/action/showMailPage');\"> " +
            "Email to a Friend </a></li>" +
            "</ul></div></div>" +
            "</div>" +
            "</div>";
    private static final String tocActionsFilteredStr =
            "<div class=\"literatumTocWidget\">" +
            "</div>";
    
    // includeNodes has <div class="literatumPublicationContentWidget"
    // excludeNodes has <a class="rightslink"
    // http://www.emeraldinsight.com/doi/abs/10.1108/AAAJ-05-2013-1360
    private static final String withRightsLink =
        "<div class=\"block\">" +  
            "<div id=\"d2f\" " +
            "class=\"widget literatumPublicationContentWidget none " +
            "paddingLeft5 paddingRight5 widget-none widget-compact-all\">" +
            "<ul class=\"tools3\">" +
            "<li class=\"addToFav\"></li>" +
            "<li class=\"permissions\">" +
            "<a class=\"rightslink\" href=\"/servlet/linkout?" +
            "type=rightslink&url=rpt%3Dn%26pageCount2013\">" +
            "<img alt=\"Reprints & Permissions\" " +
            "src=\"/imagessrc/rightsLink.png\"></a></li>" +
            "</ul>" +
            "</div>" +
            "</div>";
    // id attribute also got filtered
    private static final String rightsLinkFilteredStr =
        "<div " +
            "class=\"widget literatumPublicationContentWidget none " +
            "paddingLeft5 paddingRight5 widget-none widget-compact-all\">" +
            "<ul class=\"tools3\">" +
            "<li class=\"addToFav\"></li>" +
            "<li class=\"permissions\"></li>" +
            "</ul>" +
            "</div>";  
    
    // includeNodes has <div class="literatumPublicationContentWidget"
    // excludeNodes has <div class="downloadsCount">
    private static final String withDownloadsCount =
        "<div class=\"block\">" +
            "<div id=\"d2f\" " +
            "class=\"widget literatumPublicationContentWidget none " +
            "paddingLeft5 paddingRight5 widget-none widget-compact-all\">" +
            "<div class=\"downloadsCount\">" +
            "<dt>Downloads: </dt>" +
            "<dd>The fulltext of this document has been " +
            "downloaded 429 times since 2013</dd>" +
            "</div>" +
            "</div>" +
            "</div>";

    private static final String downloadsCountFilteredStr =
        "<div class=\"widget literatumPublicationContentWidget none " +
            "paddingLeft5 paddingRight5 widget-none widget-compact-all\">" +
            "</div>";  
    
    private static final String withSectionJumpTo =
        "<div class=\"block\">" +
            "<div id=\"d\" class=\"widget literatumPublicationContentWidget " +
            "none widget-none widget-compact-all\">" +
            "<div class=\"sectionInfo\">" +
            "<div class=\"sectionHeading\">" +
            "<h5>2. Literature review</h5>" +
            "</div>" +
            "<div class=\"sectionJumpTo\">" +
            "<span class=\"fulltext\">Section:</span>" +
            "<select class=\"fulltextdd\" onchange=\"GoTo(this, 'self')\" " +
            "name=\"select23\">" +
            "<option selected=\"#\" value=\"#\">Choose</option>" +
            "<option value=\"#\">Top of page</option>" +
            "<option value=\"#_i6\">1. Introduction</option>" +
            "<option value=\"\">2. Literature review <<</option>" +
            "</select>" +
            "</div>" +
            "</div>" +
            "</div>" +
            "</div>" +
            "</div>";
    
    private static final String sectionJumpToFilteredStr =
        "<div class=\"widget literatumPublicationContentWidget " +
            "none widget-none widget-compact-all\">" +
            "<div class=\"sectionInfo\">" +
            "<div class=\"sectionHeading\">" +
            "<h5>2. Literature review</h5>" +
            "</div>" +
            "</div>" +
            "</div>";
    
    // for testhash
    // includeNodes has <div class="literatumPublicationContentWidget"
    // excludeNodes hash <div class="options">
    // from abs - all Articles Options and Tools except Download Citation   
    // http://www.emeraldinsight.com/doi/full/10.1108/AAAJ-03-2013-1264
    private static final String withArticleOptionsExceptDownloadCitation2 =
        "<div class=\"block\">" +  
            "<div id=\"d2f\" " +
            "class=\"widget literatumPublicationContentWidget none " +
            "paddingLeft5 paddingRight5 widget-none widget-compact-all\">" +
            "<div class=\"options\">" +
            "<div class=\"subtitle\">Article Options and Tools</div>" +
            "<ul class=\"tools3\">" +
            "<li class=\"addToFav\"></li>" +
            "<li class=\"citations\">" +
            "<a class=\"ref nowrap\" target=\"_blank\" " +
            "href=\"/action/showCitFormats?doi=11.1111%2Fjid-1360\">" +
            "Download Citation</a>" +
            "</li>" +
            "<li class=\"citationAlert\"></li>" +
            "<li class=\"permissions\"></li>" +
            "</ul>" +
            "</div>" +
            "</div>" +
            "</div>";  
    // id tag also got filtered
    private static final String articleOptionsFilteredStr2 = 
        "<div class=\"widget literatumPublicationContentWidget none " +
            "paddingLeft5 paddingRight5 widget-none widget-compact-all\">" +
            "<div class=\"options\">" +
            "<ul class=\"tools3\">" +
            "<li class=\"citations\">" +
            "<a class=\"ref nowrap\" target=\"_blank\" " +
            "href=\"/action/showCitFormats?doi=11.1111%2Fjid-1360\">" +
            "Download Citation</a>" +
            "</li>" +
            "</ul>" +
            "</div>" +
            "</div>";
    
    // random class Z3988
    private static final String withRandomClassZ3988 =
        "<div class=\"block\">" +
            "<div id=\"d2f\" " +
            "class=\"widget literatumPublicationContentWidget none " +
            "paddingLeft5 paddingRight5 widget-none widget-compact-all\">" +
            "<span class=\"Z3988\" title=\"rubbish_blah_blah_link\"></span>" +
            "</div>" +
            "</div>";  
    
    private static final String randomClassZ3988FilteredStr =
        "<div class=\"widget literatumPublicationContentWidget none " +
            "paddingLeft5 paddingRight5 widget-none widget-compact-all\">" +
            "</div>"; 
    
    private static final String withRefCitation =
        "<div class=\"block\">" +
            "<div id=\"d2f\" " +
            "class=\"widget literatumPublicationContentWidget none " +
            "paddingLeft5 paddingRight5 widget-none widget-compact-all\">" +
            "<div class=\"citation\">" +
            "<span class=\"NLM_string-name\">ABC, J.R.</span>" +
             "and" +
             "<span class=\"NLM_string-name\">DEF, A.L.</span>" +
             "(" +
             "<span class=\"NLM_year\">2006</span>" +
             "), " +
             "<i>journal title</i>" +
             ", Vol. 81 No. 3, pp." +
             "<span class=\"NLM_fpage\">563</span>" +
             "‐" +
             "<span class=\"NLM_lpage\">94</span>" +
             "." +
             "<a onclick=\"newWindow(this.href);return false\" " +
             "href=\"/servlet/linkout?suffix=b38&dbid=12\">[CrossRef]</a>" +
             "," +
             "<a onclick=\"newWindow(this.href);return false\" " +
             "href=\"/servlet/linkout?suffix=b38&dbid=128\">[ISI]</a>" +
             "<a target=\"_blank\" title=\"Infotrieve: blah.\" " +
             "href=\"https://www.example.com/link\">[Infotrieve]</a>" +
             "</div>" +
             "</div>" +
             "</div>";
    
    private static final String refCitationFilteredStr =
        "<div class=\"widget literatumPublicationContentWidget none " +
            "paddingLeft5 paddingRight5 widget-none widget-compact-all\">" +
            "</div>";   

    private static final String manifestList =
        "<ul>" +
            "<li>" +
            "<a href=\"http://www.example.com/toc/abcj/123/4\">" +
            "2012 (Vol. 123 Issue 4 Page 456-789)</a>" +
            "</li>" +
            "</ul>";
    private static final String manifestListFilteredStr =
        "<a href=\"http://www.example.com/toc/abcj/123/4\">" +
            "2012 (Vol. 123 Issue 4 Page 456-789)</a>";
    
    private static final String nonManifestList1 =
        "<ul class=\"breadcrumbs\">" +
            "<li>" +
            "<a href=\"/toc/abcj/123/4\">Volume 123, Issue 4</a>" +
            "</li>" +
            "</ul>";
  private static final String nonManifestList1FilteredStr = "";
    
  private static final String nonManifestList2 =
      "<ul>" +
          "<li id=\"forthcomingIssue\">" +
          "<a href=\"/toc/abcj/123/5\">EarlyCite</a>" +
          "</li>" +
          "<li id=\"currIssue\">" +
          "<a href=\"/toc/abcj/199/1\">Current Issue</a>" +
          "</li>" +
          "</ul>";
  private static final String nonManifestList2FilteredStr = "";
    
  protected ArchivalUnit createAu()
      throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_ID,  emeraldAuConfig());
  }
  
  private Configuration emeraldAuConfig() {
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
    // for debug
    // String actualStr = StringUtil.fromInputStream(actIn);
    // assertEquals(expectedStr, actualStr);
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
    eau = createAu();
  }
  
  // Variant to test with Crawl Filter
  public static class TestCrawl extends TestEmeraldGroupHtmlFilterFactory {
    public void testFiltering() throws Exception {
      variantFact = new EmeraldGroupHtmlCrawlFilterFactory();
      doFilterTest(eau, variantFact, withCurrIssue, currIssueFilteredStr); 
      doFilterTest(eau, variantFact, withLiteratumBookIssueNavigation, 
                     filteredStr); 
      doFilterTest(eau, variantFact, withDataPbDropzoneRight, filteredStr);      
      doFilterTest(eau, variantFact, withArticleOptionsExceptDownloadCitation1, 
          articleOptionsFilteredStr1);      
    }    
  }

  // Variant to test with Hash Filter
   public static class TestHash extends TestEmeraldGroupHtmlFilterFactory {   
     public void testFiltering() throws Exception {
      variantFact = new EmeraldGroupHtmlHashFilterFactory();
      doFilterTest(eau, variantFact, withTocActions, tocActionsFilteredStr);         
      doFilterTest(eau, variantFact, withRightsLink, rightsLinkFilteredStr);         
      doFilterTest(eau, variantFact, withDownloadsCount, 
                   downloadsCountFilteredStr); 
      doFilterTest(eau, variantFact, withSectionJumpTo, 
                   sectionJumpToFilteredStr);
      doFilterTest(eau, variantFact, withArticleOptionsExceptDownloadCitation2, 
          articleOptionsFilteredStr2);
      doFilterTest(eau, variantFact, withRandomClassZ3988, 
                   randomClassZ3988FilteredStr);
      doFilterTest(eau, variantFact, withRefCitation, refCitationFilteredStr);
      doFilterTest(eau, variantFact, manifestList, 
                   manifestListFilteredStr);
      doFilterTest(eau, variantFact, nonManifestList1, 
                   nonManifestList1FilteredStr);
      doFilterTest(eau, variantFact, nonManifestList2, 
                   nonManifestList2FilteredStr);
     }
  }
  
  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
      });
  }
  
}

