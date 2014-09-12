/*
 * $Id: TestSEGHtmlFilterFactory.java,v 1.2 2014-09-12 19:58:13 ldoan Exp $
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
  
  // Errata section
  private static final String withSecErrata = 
      "<div class=\"block\">" +
          "<div id=\"sec_Errata\" class=\"articleGrouping\">" +
          "<div class=\"header innerSubject\"><input type=\"checkbox\" " +
          "onclick=\"markid('Errata');\" id=\"markallErrata\" " +
          "class=\"marksec\"><h3><div class=\"subject\">Errata</div></h3>" +
          "<a href=\"#top\" class=\"gooo\">" +
          "<img src=\"/imagessrc/booo.gif\"></a></div><div id=\"Errata\">  " +
          "<div class=\"nono\" id=\"erratum1\">" +
          "<table class=\"articleEntry\">" +
          "<tbody><tr><td width=\"18\" valign=\"top\" align=\"left\">" +
          "<input type=\"chb\" onclick=\"chacha('erratum1', 'ss', 'nn')\" " +
          "value=\"99.9999/2013-1009-ERRATUM.1\" " +
          "name=\"doi\" id=\"erratum1\"><br>" +
          "<img src=\"/imagessrc/access_full.gif\" " +
          "alt=\"full access\" title=\"full access\" " +
          "class=\"accessIcon\"></td><td class=\"toggle\"></td>" +
          "<td valign=\"top\"><a href=\"/doi/abs/99.9999/erratum.1\" " +
          "class=\"ref nowrap\"></a>" +
          "<div class=\"date\">pub date: 31 Oct 2013</div>" +
          "<a href=\"/doi/abs/99.9999/erratum.1\">Citation</a> | " +
          "<a href=\"/doi/full/99.9999/erratum.1\">Full Text</a> | " +
          "<a href=\"/doi/pdf/99.9999/erratum.1\" title=\"tittle open\" " +
          "pdf\">PDF (26 KB)</a> " +
          "| <a href=\"/doi/pdfplus/99.9999/erratum.1\" title=\"title open\"" +
          "pdfplus\">PDF w/Links (28 KB)</a>&nbsp;</td></tr>" +
          "</tbody></table></div> </div></div>" +
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
  
  // external links with short-legend of a figure
  private static final String withShortLegend =
      "<div class=\"block\">" +
          "<div class=\"short-legend\">" +
          "<p class=\"first last\">" +
          "<span class=\"captionLabel\">Figure 6.</span>" +
          "<a href=\"http://library.example.org/external_link\">extlink</a>" +
          "</p" +
          "</div>" +
          "</div>";         
  
  // external links within Acknowledgements and Case Studies sections
  private static final String withExtLink =
      "<div class=\"block\">" +      
          "<a class=\"ext-link\" title=\"External link, opens new window\" " +
          "target=\"_blank\" href=\"xxx/yyy\">xxx/yyy</a>" +
          "</div>";
  
  // For hash filtering
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
      doFilterTest(variantAu, variantFact, withArticleToolsNav, filteredStr);
      doFilterTest(variantAu, variantFact, 
          withLeftColumnExceptDownloadCitation, 
          withoutLeftColumnExceptDownloadCitation);
      doFilterTest(variantAu, variantFact, withSecErrata, filteredStr);
      doFilterTest(variantAu, variantFact, 
          withAbstractReferences, filteredStr);
      doFilterTest(variantAu, variantFact, withExtLink, filteredStr);
      doFilterTest(variantAu, variantFact, withShortLegend, filteredStr);
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
      doFilterTest(variantAu, variantFact, withArticleToolsNav, filteredStr);
      doFilterTest(variantAu, variantFact, 
          withLeftColumnExceptDownloadCitation, 
          withoutLeftColumnExceptDownloadCitation);
      doFilterTest(variantAu, variantFact, withMainAd, filteredStr);
    }
  }
  
  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
      });
  }
  
}

