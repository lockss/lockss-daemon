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

package org.lockss.plugin.atypon.seg;

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

public class TestSEGHtmlFilterFactory
  extends LockssTestCase {
  
  FilterFactory variantFact;
  ArchivalUnit variantAu;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;
        
  private static final String PLUGIN_NAME = 
      "org.lockss.plugin.atypon.seg.ClockssSEGPlugin";
  
  private static final String filteredStr = 
      "<div class=\"block\"></div>";

  // For crawl filtering
  // top right of issue toc - links to previous or next issue
  private static final String withPrevNextNav = 
      "<div class=\"block\">" +
          "<div id=\"prevNextNav\">" +
          "<div id=\"issueSearch\">" +
          "<form method=\"get\" action=\"/action/doSearch\">" +
          "<input type=\"text\" size=\"17\" value=\"\" name=\"AllField\">" +
          "<input type=\"hidden\" value=\"4\" name=\"issue\">" +
          "<input type=\"hidden\" value=\"xxxxx\" name=\"jcode\">" +
          "</div><a href=\"javascript:toggleSlide('issueSearch')\">" +
          "Search Issue</a> |" +
          "<img src=\"/templates/images/aa32.png\"> " +
          "<br>" +
          "<a href=\"/toc/xxxxxx/1/3\">Previous Issue</a>" +
          "<a href=\"/toc/xxxxxx/2/1\"> Next Issue</a>" +
          "</div>" +
          "</div>";
  
  // top right of article - links to previous or next article
  private static final String withArticleToolsNav =
      "<div class=\"block\">" +
          "<div class=\"stackContents\" id=\"articleToolsNav\">" +
          "<div class=\"stacked\" id=\"articleToolsPrev\">&lt;" +
          "<a class=\"articleToolsNav\" "+
          "href=\"/doi/abs/xx.xxxx/xxxx.185\">Previous Article</a>" +
          "</div>" +
          "<div class=\"stackedReverse\" id=\"articleToolsNext\">" +
          "<a class=\"articleToolsNav\" "+
          "href=\"/doi/abs/xx.xxxx/xxxx.215\">Next Article</a>&gt;" +
          "</div>" +
          "<div class=\"groupInfo\">" +
          "Volume a, Issue b (2012)" +
          "</div>" +
          "</div>" +
          "</div>";
  // top right of article - links to previous or next article
  private static final String articleNavFiltered =
      "<div class=\"block\">" +
          "<div class=\"stackContents\" id=\"articleToolsNav\">" +
          "<div class=\"stacked\" id=\"articleToolsPrev\">&lt;" +
          "</div>" +
          "<div class=\"stackedReverse\" id=\"articleToolsNext\">" +
          "&gt;</div>" +
          "<div class=\"groupInfo\">" +
          "Volume a, Issue b (2012)" +
          "</div>" +
          "</div>" +
          "</div>";
  
  // left column of an article - all except Download Citations
  // <div class="yui3-u yui3-u-1-4 leftColumn">
  private static final String withLeftColumnExceptDownloadCitation = 
      "<div class=\"block\">" +
          "<div class=\"yui3-u yui3-u-1-4 leftColumn\">" +
          "<div class=\"panel\"><h3>Article Tools</h3></div>" +
          "<div class=\"box-inner\">" +
          "<div><a href=\"/action/addFavoritePublication" +
          "?doi=99.9999%2FJid99.9.999\">Add to my favorites</a></div>" +
          "<div><a href=\"/action/showCitFormats" +
          "?doi=99.9999%2FJid99.9.999\">Download Citations</a></div>" +
          "<div><a href=\"/action/addCitationAlert" +
          "?doi=99.9999%2FJid99.9.999\">Track Citations</a></div>" +
          "</div>" +
          "<div class=\"panel\"><h3>Recommend &amp; Share</h3></div>" +
          "<div class=\"box-inner\">" +
          "<div class=\"social\"><a href=\"http://www.facebook.com/sh.php" +
          "?u=http%3a%2f%2fsegbase%2fdoi%2fabs%2f99.9999%2fJid99.9.999" +
          "&amp;t=blah+of+blah+blah+for+blah+blah\">Facebook</a></div>" +
          "</div>" +
          "<div id=\"sessionHistory\" class=\"panel panel_476\">" +
          "<div class=\"header\"><h3>Session History</h3></div>" +
          "<div class=\"sessionViewed\">" +
          "<div class=\"label\">Recently Viewed</div>" +
          "<ul class=\"sessionHistory\">" +
          "<li><a href=\"/doi/abs/99.9999/gabc2012-9999.9\">" +
          "The cow jumps over the moon</a></li>" +
          "</ul>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>";
  
  private static final String withoutLeftColumnExceptDownloadCitation = 
      "<div class=\"block\">" +
          "<div class=\"yui3-u yui3-u-1-4 leftColumn\">" +
          "<div class=\"box-inner\">" +
          "<div><a href=\"/action/showCitFormats" +
          "?doi=99.9999%2FJid99.9.999\">Download Citations</a></div>" +
          "</div>" +
          "</div>" +
          "</div>";
  
  // external links within References section
  private static final String withAbstractReferences = 
      "<div class=\"block\">" +
          "<div class=\"abstractReferences\">" +
          "<div class=\"sectionHeadingContainer\">" +
          "<ol>" +
          "<li class=\"reference oList\">Smith, and Maxwell, 2001. " +
          "Rewiew X, 22 pp. </li>" +
          "<li class=\"reference oList\">" +
          "<a href=\"www.estcp.org\">www.estcp.org</a>" +
          ", 101 pp." +
          "</li>" +
          "</ol>" +
          "</div>" +
          "</div>" +
          "</div>";      
  
  // external links within Acknowledgements and Case Studies sections
  private static final String withExtLink =
      "<div class=\"block\">" +      
          "<a class=\"ext-link\" title=\"External link, opens new window\" " +
          "target=\"_blank\" href=\"xxx/yyy\">xxx/yyy</a>" +
          "</div>";
  
  // For hash filtering
  // top right of article - links to previous or next article and Cited By
  private static final String withTypePublicationTools = 
      "<div class=\"block\">" +
          "<div id=\"widget0\" " +
          "class=\"widget type-publication-tools ui-helper-clearfix\">" +
          "<div class=\"stackContents\" id=\"articleToolsNav\">" +
          "<div class=\"stacked\" id=\"articleToolsPrev\">&lt;" +
          "<a class=\"articleToolsNav\" "+
          "href=\"/doi/abs/xx.xxxx/xxxx.185\">Previous Article</a>" +
          "</div>" +
          "<div class=\"stackedReverse\" id=\"articleToolsNext\">" +
          "<a class=\"articleToolsNav\" "+
          "href=\"/doi/abs/xx.xxxx/xxxx.215\">Next Article</a>&gt;" +
          "</div>" +
          "<div class=\"groupInfo\">" +
          "Volume a, Issue b (2012)" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>";
  
  // below type-publication-tools of an article, where ads are inserted
  private static final String withTypeAdPlaceholder = 
      "<div class=\"block\">" +
          "<div class=\"widget type-ad-placeholder ui-helper-clearfix\" " +
          "id=\"widget0\">" +
          "<div class=\"view\">" +
          "<div class=\"view-inner\">" +
          "<!-- placeholder id=null, description=Video Placeholder -->" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>";
  
  // from toc page - Cited count
  private static final String withTocCitation = 
      "<div class=\"block\">" +  
          "<div class=\"citation tocCitation\">JournalX 11" +
          "<span class=\"ciationPageRange\">pp 2-10 pages" +
          "</span><a class=\"ref doi\" " +
          "href=\"http://dx.doi.org/99.999/2013-X\">" +
          "http://dx.doi.org/99.9999/2013-X</a> | Cited <b>5</b> time</div>" +
          "</div>";
  
  private static final String withMainAd =
      "<div class=\"block\">" +
          "<div class=\"mainAd\">" +
          "<div><!-- placeholder id=null, description=Site Ad 1 -->" +
          "<div style=\"float:right;\">" +
          "<!-- load the list of ads -->" +
          "<script src=\"http://segads/ox/xxx.php?zone=29\" " +
          "type=\"text/javascript\"></script>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>";
  
    private static final String withFirstPage =
      "<div class=\"block\">" +
          "<div id=\"firstPage\"><a href=\"#\" class=\"firstPageLink\" " +
          "title=\"change image size\"><span class=\"msg\">" +
          "Click to change image size</span>" +
          "<img src=\"/imagesrc/xxx.png_v03\" alt=\"free\" " +
          "class=\"firstPageImage\"></a></div>" +
          "</div>";

  protected ArchivalUnit createAu()
      throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_NAME,  segAuConfig());
  }
  
  private Configuration segAuConfig() {
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
          		
  // Variant to test with Crawl Filter
  public static class TestCrawl
    extends TestSEGHtmlFilterFactory {
    public void setUp() throws Exception {
      super.setUp();
      tempDirPath = setUpDiskSpace();
      startMockDaemon();
      variantFact = new SEGHtmlCrawlFilterFactory();
      variantAu = createAu();
    }    
    public void testFiltering() throws Exception {
      doFilterTest(variantAu, variantFact, withPrevNextNav, filteredStr);
      doFilterTest(variantAu, variantFact, withArticleToolsNav, articleNavFiltered);      
      doFilterTest(variantAu, variantFact, 
          withLeftColumnExceptDownloadCitation, 
          withoutLeftColumnExceptDownloadCitation);
      doFilterTest(variantAu, variantFact, 
          withAbstractReferences, filteredStr);
      doFilterTest(variantAu, variantFact, withExtLink, filteredStr);
    }    
  }

  // Variant to test with Hash Filter
   public static class TestHash 
    extends TestSEGHtmlFilterFactory {   
    public void setUp() throws Exception {
      super.setUp();
      tempDirPath = setUpDiskSpace();
      startMockDaemon();
      variantFact = new SEGHtmlHashFilterFactory();
      variantAu = createAu();
    }
    public void testFiltering() throws Exception {
      doFilterTest(variantAu, variantFact, withPrevNextNav, filteredStr);
      doFilterTest(variantAu, variantFact, 
          withTypePublicationTools, filteredStr);
      doFilterTest(variantAu, variantFact, withTypeAdPlaceholder, filteredStr);
      doFilterTest(variantAu, variantFact, withTocCitation, filteredStr);
      doFilterTest(variantAu, variantFact, 
          withLeftColumnExceptDownloadCitation, 
          withoutLeftColumnExceptDownloadCitation);
      doFilterTest(variantAu, variantFact, withMainAd, filteredStr);
      doFilterTest(variantAu, variantFact, withFirstPage, filteredStr);
    }
  }
  
  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
      });
  }
  
}

