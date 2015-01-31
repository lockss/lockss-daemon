/*
 * $Id: TestCYSHtmlFilterFactory.java,v 1.2 2015-01-31 20:18:37 ldoan Exp $
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

package org.lockss.plugin.atypon.cys;

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

public class TestCYSHtmlFilterFactory extends LockssTestCase {
  
  FilterFactory variantFact;
  ArchivalUnit cau;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;
  
  private static final String PLUGIN_NAME = 
      "org.lockss.plugin.atypon.cys.ClockssCYSPlugin";
  
  private static final String filteredStr = 
      "<div class=\"block\"></div>";
  
  private static final String withHead =
      "<div class=\"block\">" +
      "<head>" +
      "<meta content=\"text/html; charset=UTF-8\">" +
      "<title> The Golden title - XYZ Journal </title>" +
      "<link href=\"/templates/jsp/style.css\" rel=\"stylesheet\">" +
      "<script async=\"\" src=\"//www.google-analytics.com/analytics.js\">" +
      "<script src=\"/jsquery/jquery-1.6.1.min.js\" type=\"text/javascript\">" +
      "<link href=\"http://purl.org/DC/elements/1.0/\" rel=\"schema.DC\">" +
      "<meta content=\"The Golden title 1 \" name=\"dc.Title\">" +
      "<meta content=\"XYZ Journal\" name=\"citation_journal_title\">" +
      "</head>" +
      "</div>";
  
  private static final String withNavWrapper =
      "<div class=\"block\">"
          + "<div id=\"nav-wrapper\">"
          + "<div>"
          + "<ul id=\"nav-left\"><li class=\"first\">"
          + "<li><a href=\"/journal/xxxx\">Home</a></li>"
          + "<li><a href=\"/page/aboutxxxx\">About Xxxx</a></li>"
          + "</ul>"
          + "</div>"
          + "<ul id=\"nav-right\">"
          + "<li><a class=\"language\" href=\"/action/blah\">Other language</a>"
          + "</li></ul></div>"
          + "</div>";
  
  private static final String withBreadcrumbs =
      "<div class=\"block\">" +
          "<div id=\"breadcrumbs\">" +
          "<span class=\" page_breadcrumbs\">" +
          "<a href=\"/\">Home</a>" +
          ">" +
          "<a href=\"/action/journalLink\">Journals</a>" +
          ">" +
          "<a href=\"/journal/homeLinks\">Canadian Young Scientist Journal</a>" +
          ">" +
          "<a href=\"/listofissueslink\">List of Issues</a>" +
          "> Volume 2013, Number 2, September 2013" +
          "</span>" +
          "</div>" +
          "</div>";
  private static final String withSidebarLeft = 
      "<div class=\"block\">"
          + "<div id=\"sidebar-left\">"
          + "<a href=\"/journal/xxx\">"
          + "<img src=\"/blah/covergifs/xxx/cover.jpg\"></a>"
          + "<div class=\"ads\">"
          + "</div><ul style=\"margin-top: -10px;\" id=\"left-menu\"><li>"
          + "<div class=\"header-bar header-gray\">Browse the journal</div><ul>"
          + "<li><a href=\"/action/doi/full/11.11111/xxxx-2013-005\">"
          + "Archives</a></li>"
          + "<li><a href=\"/action/recommendation/doi/full/11.11111"
          + "/xxxx-2013-005\">Recommend to public</a></li>"
          + "</ul></li></ul><div></div></div>"
          + "</div>";
  
  // toc - table of contents box with full issue pdf,
  // previous/next issue, current issue - no unique name found
  // http://www.cysjournal.ca/toc/cysj/2013/2
  private static final String withFullListIssues =
      "<div class=\"block\">"
          + "<div class=\"box-pad border-gray margin-bottom clearfix\">"
          + "<class=\"float-right\">"
          + "<h1>Table of Contents</h1>"
          + "<img src=\"/imagehome/cover.jpg\">"
          + "<div class=\"journal-details\">"
          + "<a class=\"btn-article-items \""
          + "href=\"/doi/pdf/11.11111/xxxx2013-2\">"
          + "</a></div></div>"
          + "</div>";
  
  // abs, full - previous/toc/next article
  // http://www.cysjournal.ca/doi/full/10.13034/cysj-2013-006
  private static final String withPreviousNextArticle = 
      "<div class=\"block\">" +
          "<a class=\"white-link-right\" title=\"Previous Article\" " +
          "href=\"/doi/full/11.11111/jid-2013-001\"> Ç Previous</a>" +
          "<a class=\"white-link-right\" " +
          "href=\"http://www.xxx.com/toc/jid/2013/2\"> TOC </a>" +
          "<a class=\"white-link-right\" title=\"Next Article\" " +
          "href=\"/doi/full/11.11111/jid-2013-005\"> Next È </a>" +
          "</div>";
  
  private static final String withSpiderTrap =
      "<div class=\"block\">"
          + "<span id=\"hide\"><a href=\"/doi/pdf/10.xxxx/9999-9999.99999\">"
          + "<!-- Spider trap link --></a></span>"
          + "</div>";

  private static final String withLeaderBoard =
      "<div class=\"block\">" +
          "<div class=\"leaderboard\">" +
          "<a href=\"/action/clickThrough?id=blah\">" +
          "<img src=\"/imgsrc/Bond_Banner.jpg\" alt=\"Bond Banner\"></a>" +
          "</div>" +
          "</div>";
  
  private static final String withTopBarWrapper =
      "<div class=\"block\">"
          + "<div id=\"top-bar-wrapper\">"
          + "<div id=\"top-bar-1\">"
          + "<div class=\"headerAd\"><img src=\"/yyy/logo.gif\">"
          + "</div></div></div>"
          + "</div>";
  
  private static final String withBanner =
       "<div class=\"block\">" +
          "<div class=\"banner\">" +
          "<h1> XYZ Journal</h1>" +
          "</div>" +
          "</div>";
      
  private static final String withAllSidebarRightExceptDownloadCitation = 
      "<div class=\"block\">"
          + "<div id=\"sidebar-right\">"
          + "<div class=\"article-tools\">"
          + "<ul class=\"article-tools-list\">"
          + "<li><a title=\"Download Citation\" class=\"icon-citation\""
          + "href=\"/action/showCitFormats?blah\">Download Citation</a></li>"
          + "</ul></div>"
          + "<div class=\"socialMedia\">"
          + "<ul id=\"social-media\">"
          + "<li><a class=\"icon-citeulike\""
          + "href=\"http://www.citeulike.org/blah\">CiteULike</a></li>"
          + "</ul></div></div>"
          + "</div>";
  
  private static final String sidebarRightFiltered = 
      "<div class=\"block\">"
          + "<div id=\"sidebar-right\">"
          + "<div class=\"article-tools\">"
          + "<ul class=\"article-tools-list\">"
          + "<li><a title=\"Download Citation\" class=\"icon-citation\""
          + "href=\"/action/showCitFormats?blah\">Download Citation</a></li>"
          + "</ul></div></div>" 
          + "</div>"; 
  
  protected ArchivalUnit createAu()
      throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_NAME,  cysAuConfig());
  }
  
  private Configuration cysAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", "http://www.example.com/");
    conf.put("journal_id", "abc");
    conf.put("volume_name", "99");
    return conf;
  }
  
  private static void doFilterTest(ArchivalUnit au, 
      FilterFactory fact,String nameToHash, String expectedStr) 
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
    cau = createAu();
  }  
          		
  // Variant to test with Crawl Filter
  public static class TestCrawl extends TestCYSHtmlFilterFactory {
    public void testFiltering() throws Exception {
      variantFact = new CYSHtmlCrawlFilterFactory();
      doFilterTest(cau, variantFact, withSidebarLeft, filteredStr);
      doFilterTest(cau, variantFact, withPreviousNextArticle, filteredStr);
      doFilterTest(cau, variantFact, withFullListIssues, filteredStr);
      doFilterTest(cau, variantFact, withSpiderTrap, filteredStr);
    }    
  }

  // Variant to test with Hash Filter
   public static class TestHash extends TestCYSHtmlFilterFactory {   
    public void testFiltering() throws Exception {
      variantFact = new CYSHtmlHashFilterFactory();
      doFilterTest(cau, variantFact, withHead, filteredStr);
      doFilterTest(cau, variantFact, withLeaderBoard, filteredStr);
      doFilterTest(cau, variantFact, withTopBarWrapper, filteredStr);
      doFilterTest(cau, variantFact, withBanner, filteredStr);
      doFilterTest(cau, variantFact, withNavWrapper, filteredStr);
      doFilterTest(cau, variantFact, withBreadcrumbs, filteredStr);
      doFilterTest(cau, variantFact, withSidebarLeft, filteredStr);
      doFilterTest(cau, variantFact, withAllSidebarRightExceptDownloadCitation, 
                   sidebarRightFiltered);
      doFilterTest(cau, variantFact, withFullListIssues, filteredStr);
      doFilterTest(cau, variantFact, withPreviousNextArticle, filteredStr);
      doFilterTest(cau, variantFact, withSpiderTrap, filteredStr);
    }
  }
  
  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
      });
  }
  
}

