/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.wageningen;

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

public class TestWageningenJournalsHtmlFilterFactory extends LockssTestCase {
  
  FilterFactory variantFact;
  ArchivalUnit wau;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;
        
  private static final String PLUGIN_ID = 
      "org.lockss.plugin.atypon.wageningen.ClockssWageningenJournalsPlugin";
  
  private static final String filteredStr = 
      "<div class=\"block\"></div>";
  
  // breadcrumbs
  private static final String withBreadcrumbs =
      "<div class=\"block\">" +
      "<ul class=\"breadcrumbs\">" +
      "<li class=\"\"><a href=\"/\">Home</a>" +
      "<span class=\"divider\">></span></li>" +
      "<li class=\"\"><a href=\"/loi/jid\">ABC Journals</a>" +
      "<span class=\"divider\">></span></li>" +
      "<li class=\"\"><a href=\"/loi/jid\">issues</a>" +
      "<span class=\"divider\">></span></li>" +
      "<li class=\"\"><a href=\"/toc/jid/26/1\">Volume 9, Issue 9</a>" +
      "<span class=\"divider\">></span></li>" +
      "<li class=\"truncate\">The development of blah</li>" +
      "</ul>" +
      "</div>";  
  
  private static final String withAriaRelevant =
      "<div class=\"block\">" +
      "<div class=\"tabs tabs-widget\" aria-relevant=\"additions\" " +
      "aria-atomic=\"true\" aria-live=\"polite\">" +
      "<div class=\"tab-content\">" +
      "<div id=\"19a\" class=\"tab-pane active\">" +
      "<div data-pb-dropzone-name=\"Most Read\" " +
      "data-pb-dropzone=\"tab-59a\">" +
      "<div id=\"753\" class=\"widget literatumMostReadWidget none " +
      "widget-none widget-compact-all\">" +
      "<div class=\"widget-body body body-none body-compact-all\">" +
      "<section class=\"popular\">" +
      "<div class=\"mostRead\">" +
      "<ul>" +
      "<li><div class=\"title\">" +
      "<a href=\"/doi/abs/11.1111/jid.22.2.3\">Canada blah blah</a>" +
      "</div>" +
      "<div class=\"authors\">" +
      "<div class=\"contrib\">" +
      "<span class=\"authors\">Tim Ra</span>" +
      "</div></div></ul>" +
      "</div></section>" +
      "</div></div></div></div></div></div>" +
      "</div>";
      
  private static final String withRelatedContent =
      "<div class=\"block\">" +      
      "<div class=\"tab tab-pane\" id=\"relatedContent\"></div>" +
      "</div>";
  
  // for testcrawl
  // from abs - all Articles Options and Tools except Download Citation   
  private static final String withArticleToolsExceptDownloadCitation1 =
      "<div class=\"block\">" +
      "<div class=\"articleTools\">" +
      "<ul class=\"linkList blockLinks separators centered\">" +
      "<li class=\"addToFavs\"><a href=\"/linktoaddfav\">Add to Fav</a></li>" +
      "<li class=\"email\"><a href=\"/linktoemail\">Email friends</a></li>" +
      "<li class=\"downloadCitations\">" +
      "<a href=\"/action/showCitFormats?doi=10.3828%2Fjid.2013.2\">" +
      "Send to Citation Mgr</a>" +
      "</li></ul></div>" +
      "</div>";
  
  // id tag also got filtered
  private static final String articleToolsFilteredStr1 = 
      "<div class=\"block\">" +
      "<div class=\"articleTools\">" +
      "<ul class=\"linkList blockLinks separators centered\">" +
      "<li class=\"downloadCitations\">" +
      "<a href=\"/action/showCitFormats?doi=10.3828%2Fjid.2013.2\">" +
      "Send to Citation Mgr</a>" +
      "</li></ul></div>" +
      "</div>";
    
  private static final String withPublicationToolContainer =
      "<div class=\"block\">" +
      "<div class=\"widget tocListWidget none  widget-none  " +
      "widget-compact-all\" id=\"3f3\">" +
      "<div class=\"widget-body body body-none  body-compact-all\">" +
      "<fieldset class=\"tocListWidgetContainer\">" +
      "<div class=\"publicationToolContainer\">" +
      "<div class=\"publicationToolCheckboxContainer\">" +
      "<input type=\"checkbox\" name=\"markall\" id=\"markall\" " +
      "onclick=\"onClickMarkAll(frmAbs,'')\"><span>Select All</span>" +
      "</div>" +
      "<div class=\"publicationTooldropdownContainer\">" +
      "<span class=\"dropdownLabel\">" +
      "For selected items:" +
      "</span>" +
      "<select name=\"articleTool\" class=\"items-choice " +
      "publicationToolSelect\" title=\"Article Tools\">" +
      "<option value=\"\">Please select</option>" +
      "<option value=\"/addfav\">Add to Favorites</option>" +
      "<option value=\"/trackcit\">Track Citation</option>" +
      "<option value=\"/showcit\">Download Citation</option>" +
      "<option value=\"/emailto\">Email</option>" +
      "</select>" +
      "</div>" +
      "</div>" +
      "</fieldset>" +
      "</div>" +
      "</div>" +
      "</div>";
  
  // attributes separated by 1 space
  private static final String publicationToolContainerFilteredStr =
      " <div class=\"widget tocListWidget none widget-none " +
      "widget-compact-all\" >" +
      " <div class=\"widget-body body body-none body-compact-all\">" +
      " <fieldset class=\"tocListWidgetContainer\">" +
      " </fieldset>" +
      " </div>" +
      " </div>";
  
  private static final String withArticleMetaDrop =
       "<div class=\"widget literatumPublicationContentWidget none  " +
      "widget-none\" id=\"1ca\">" +
      "<div class=\"articleMetaDrop publicationContentDropZone\" " +
      "data-pb-dropzone=\"articleMetaDropZone\">" +
      "</div>" +
      "</div>" +
      "</div>";
  
  private static final String articleMetaDropFilteredStr =
      " <div class=\"widget literatumPublicationContentWidget none " +
      "widget-none\" >" +
      " </div>";
            
   private static final String withArticleToolsExceptDownloadCitation2 =
       "<div class=\"block\">" +  
       "<section class=\"widget literatumArticleToolsWidget none " +
       "margin-bottom-15px widget-regular  widget-border-toggle\" " +
       "id=\"3a3\">" +
       "<div class=\"articleTools\">" +
       "<ul class=\"linkList blockLinks separators centered\">" +
       "<li class=\"addToFavs\"><a href=\"/linktoaddfav\">" +
       "Add to Fav</a></li>" +
       "<li class=\"email\"><a href=\"/linktoemail\">Email friends</a></li>" +
       "<li class=\"downloadCitations\">" +
       "<a href=\"/action/showCitFormats?doi=11.1111%jid.2013.2\">" +
       "Send to Citation Mgr</a>" +
       "</li></ul></div>" +
       "</section>" +
       "</div>";
  
  private static final String articleToolsFilteredStr2 = 
      " <section class=\"widget literatumArticleToolsWidget none " +
      "margin-bottom-15px widget-regular widget-border-toggle\" >" +
      " <div class=\"articleTools\">" +
      " <ul class=\"linkList blockLinks separators centered\">" +
      " <li class=\"downloadCitations\">" +
      " <a href=\"/action/showCitFormats?doi=11.1111%jid.2013.2\">" +
      "Send to Citation Mgr </a> </li> </ul> </div> </section>";
  
  private static final String tooltipHtml =
      "<div class=\"literatumPublicationContentWidget\">" +
      "<a class=\"entryAuthor\" href=\"/author/Ya%2C+X\"> X. Ya</a>" +
      "<a href=\"#d484e149\" class=\"tooltipTrigger infoIcon\">" +
      "<span class=\"ui-helper-hidden-accessible\">Related information</span>" +
      "</a><div id=\"d484e149\" class=\"ui-helper-hidden-accessible\">" +
      "1Laboratory of Study, " +
      "College of Study, " +
      "China University, Beijing 100083, China P.R.<br></div>, " +
      "</div>";
  private static final String tooltipFiltered = 
      " <div class=\"literatumPublicationContentWidget\">" +
      " <a class=\"entryAuthor\" href=\"/author/Ya%2C+X\"> X. Ya </a>" +
      ", " +
      "</div>";
  
  private static final String toolsTab = 
      "<div class=\"widget-body body body-none \">" +
          "<div class=\"tabs tabs-widget \" aria-live=\"polite\" aria-atomic=\"true\" aria-relevant=\"additions\">" +
          "<ul class=\"tab-nav\">" +
          "<li class=\"active\">" +
          "<a href=\"\" title=\"show Tools\">Tools</a>" +
          "</li>" +
          "<li class=\"\">" +
          "<a href=\"\" title=\"show Most Read\">Most Read</a>" +
          "</li>" +
          "<li class=\"\">" +
          "<a href=\"\" title=\"show Most Cited\">Most Cited</a>" +
          "</li>" +
          "</ul>" +
          "<div class=\"tab-content\">" +
          "<div class=\"tab-pane active\" id=\"\">" +
          "<div class=\"tab-pane-content\" data-pb-dropzone=\"\" data-pb-dropzone-name=\"Tools\">" +
          "<div class=\"widget layout-one-column none  widget-regular  widget-border-toggle widget-compact-all\" id=\"\"  >" +
          "<div class=\"wrapped \" >" +
          "<div class=\"widget-body body body-regular  body-border-toggle body-compact-all\">" +
          "<div class=\"pb-columns row-fluid\">" +
          "<div class=\"width_1_1\">" +
          "<div data-pb-dropzone=\"center\">" +
          "<div class=\"widget literatumAlertsWidget none  widget-none  widget-compact-vertical\" id=\"\"  >" +
          "<div class=\"wrapped \" >" +
          "<div class=\"widget-body body body-none  body-compact-vertical\">" +
          "<div class=\"alertsWidget\">" +
          "<h3>Site Tools</h3>" +
          "<div class=\"alerts-body\">" +
          "<div class=\"boxlink\" id=\"\">" +
          "<a href=\"/action/doUpdateAlertSettings?action=addJournal\">Sign up for e-alerts</a>" +
          "</div>" +
          "<div class=\"boxlink\" id=\"\">" +
          "<a href=\"/action/showFeed?type=etoc&feed=rss&jc=qas\"><img src=\"\" alt=\"RSS\" width=\"36\" height=\"14\" align=\"top\" /></a>" +
          "<a href=\"/help?context=rss\" onclick=\"popupHelp(this.href); return false;\" target=\"_blank\">" +
          "(What is this?)" +
          "</a>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "<div class=\"widget literatumArticleToolsWidget alignLeft  widget-none  widget-compact-all widget-rounded\" id=\"\"  >" +
          "<div class=\"wrapped \" >" +
          "<div class=\"widget-body body body-none  body-compact-all body-rounded\">" +
          "<div class=\"articleTools\">" +
          "<ul class=\"linkList blockLinks separators centered\">" +
          "<li class=\"addToFavs\">" +
          "<a href=\"/personalize/addFavoritePublication?doi=10.3920%2FQAS2014.0457\">Add to Favorites</a>" +
          "</li>" +
          "<li class=\"email\">" +
          "<a href=\"/action/showMailPage?doi=10.3920\">Email a Friend</a>" +
          "</li>" +
          "<li class=\"downloadCitations\">" +
          "<a href=\"/action/showCitFormats?doi=10.3920%2FQAS2014.0457\">Download Citation Data</a>" +
          "</li>" +
          "<li class=\"trackCitations\">" +
          "<a href=\"/action/addCitationAlert?doi=10.3920%2FQAS2014.0457\">Track Citations</a>" +
          "</li>" +
          "</ul>" +
          "</div" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "<div class=\"widget general-html none  widget-none  widget-compact-horizontal\" id=\"\"  >" +
          "<div class=\"wrapped \" >" +
          "<div class=\"widget-body body body-none  body-compact-horizontal\"><script src=\"\"></script>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "<div class=\"tab-pane \" id=\"\">" +
          "<div class=\"tab-pane-content\" data-pb-dropzone=\"\" data-pb-dropzone-name=\"Most Read\">" +
          "<div class=\"widget literatumMostReadWidget none  widget-none\" id=\"\"  >" +
          "<div class=\"wrapped \" >" +
          "<div class=\"widget-body body body-none \"><section class=\"popular\">" +
          "<div class=\"mostRead  \">" +
          "<a href=\"/action/showMostReadArticles?journalCode=qas\" class=\"more\">See More</a>" +
          "</div>" +
          "</section" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "<div class=\"tab-pane \" id=\"\">" +
          "<div class=\"tab-pane-content\" data-pb-dropzone=\"\" data-pb-dropzone-name=\"Most Cited\">" +
          "<div class=\"widget literatumMostCitedWidget none  widget-none\" id=\"\"  >" +
          "<div class=\"wrapped \" >" +
          "<div class=\"widget-body body body-none \"><section class=\"popular\">" +
          "</section" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>";
// remove everything except for the tree to the one piece we need  
  private static final String toolsTabCrawlFiltered =
      "<div class=\"widget-body body body-none \">" +
          "<div class=\"tabs tabs-widget \" aria-live=\"polite\" aria-atomic=\"true\" aria-relevant=\"additions\">" +
          "<div class=\"tab-content\">" +
          "<div class=\"tab-pane active\" id=\"\">" +
          "<div class=\"tab-pane-content\" data-pb-dropzone=\"\" data-pb-dropzone-name=\"Tools\">" +
          "<div class=\"widget layout-one-column none  widget-regular  widget-border-toggle widget-compact-all\" id=\"\"  >" +
          "<div class=\"wrapped \" >" +
          "<div class=\"widget-body body body-regular  body-border-toggle body-compact-all\">" +
          "<div class=\"pb-columns row-fluid\">" +
          "<div class=\"width_1_1\">" +
          "<div data-pb-dropzone=\"center\">" +
          "<div class=\"widget literatumArticleToolsWidget alignLeft  widget-none  widget-compact-all widget-rounded\" id=\"\"  >" +
          "<div class=\"wrapped \" >" +
          "<div class=\"widget-body body body-none  body-compact-all body-rounded\">" +
          "<div class=\"articleTools\">" +
          "<ul class=\"linkList blockLinks separators centered\">" +
          "<li class=\"downloadCitations\"><a href=\"/action/showCitFormats?doi=10.3920%2FQAS2014.0457\">Download Citation Data</a>" +
          "</li>" +
          "</ul>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>";

// ?? use this block when manifest pages are up  
//    private static final String manifestList =
//        "<ul>" +
//            "<li>" +
//            "<a href=\"http://www.example.com/toc/abcj/123/4\">" +
//            "2012 (Vol. 123 Issue 4 Page 456-789)</a>" +
//            "</li>" +
//            "</ul>";
//    private static final String manifestListFilteredStr =
//        "<a href=\"http://www.example.com/toc/abcj/123/4\">" +
//            "2012 (Vol. 123 Issue 4 Page 456-789)</a>";
//    
//    private static final String nonManifestList1 =
//        "<ul class=\"breadcrumbs\">" +
//            "<li>" +
//            "<a href=\"/toc/abcj/123/4\">Volume 123, Issue 4</a>" +
//            "</li>" +
//            "</ul>";
//  private static final String nonManifestList1FilteredStr = "";
//    
//  private static final String nonManifestList2 =
//      "<ul>" +
//          "<li id=\"forthcomingIssue\">" +
//          "<a href=\"/toc/abcj/123/5\">EarlyCite</a>" +
//          "</li>" +
//          "<li id=\"currIssue\">" +
//          "<a href=\"/toc/abcj/199/1\">Current Issue</a>" +
//          "</li>" +
//          "</ul>";
//  private static final String nonManifestList2FilteredStr = "";
    
  protected ArchivalUnit createAu()
      throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_ID,  wageningenAuConfig());
  }
  
  private Configuration wageningenAuConfig() {
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
    //for debug
    //String actualStr = StringUtil.fromInputStream(actIn);
    //assertEquals(expectedStr, actualStr);
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
    wau = createAu();
  }
  
  // Variant to test with Crawl Filter
  public static class TestCrawl 
    extends TestWageningenJournalsHtmlFilterFactory {
    public void testFiltering() throws Exception {
      variantFact = new WageningenJournalsHtmlCrawlFilterFactory();
      doFilterTest(wau, variantFact, withBreadcrumbs, filteredStr); 
      doFilterTest(wau, variantFact, withAriaRelevant, filteredStr); 
      doFilterTest(wau, variantFact, withRelatedContent, filteredStr);      
      doFilterTest(wau, variantFact, withArticleToolsExceptDownloadCitation1, 
          articleToolsFilteredStr1);      
      doFilterTest(wau, variantFact, toolsTab, 
          toolsTabCrawlFiltered);      
    }    
  }

  // Variant to test with Hash Filter
  public static class TestHash 
    extends TestWageningenJournalsHtmlFilterFactory {   
    public void testFiltering() throws Exception {
      variantFact = new WageningenJournalsHtmlHashFilterFactory();
      doFilterTest(wau, variantFact, withPublicationToolContainer, 
                   publicationToolContainerFilteredStr);         
      doFilterTest(wau, variantFact, withArticleMetaDrop,
                   articleMetaDropFilteredStr);         
      doFilterTest(wau, variantFact, withArticleToolsExceptDownloadCitation2, 
                   articleToolsFilteredStr2);
      doFilterTest(wau, variantFact, tooltipHtml, tooltipFiltered);
//      doFilterTest(eau, variantFact, manifestList, 
//                   manifestListFilteredStr);
//      doFilterTest(eau, variantFact, nonManifestList1, 
//                   nonManifestList1FilteredStr);
//      doFilterTest(eau, variantFact, nonManifestList2, 
//                   nonManifestList2FilteredStr);
    }
  }
  
  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
      });
  }
  
}

