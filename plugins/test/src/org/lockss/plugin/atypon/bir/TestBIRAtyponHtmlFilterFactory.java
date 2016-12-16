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

package org.lockss.plugin.atypon.bir;

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

public class TestBIRAtyponHtmlFilterFactory extends LockssTestCase {
  
  FilterFactory variantFact;
  ArchivalUnit bau;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;

  
  // BIR now removes all tags after filtering based on tag information 
  private static final String PLUGIN_ID = 
      "org.lockss.plugin.atypon.bir.ClockssBIRAtyponPlugin";
  
  // BIR now removes all tags after filtering based on tag information 
  private static final String filteredStr = 
//      " <div class=\"block\"> </div>";
      " ";
  private static final String filteredCrawlStr = 
         "<div class=\"block\"></div>";
    
  private static final String withMenuXml =
      "<div class=\"block\">" +
          "<div class=\"widget menuXml none  widget-compact-all\" id=\"xx\">" +
          "<ul class=\"shadow primaryNav\">" +
          "<li><a class=\"expander\" href=\"#\">Journals</a><ul>" +
          "<li class=\"\"><a href=\"/toc/bjr/current\">BJR</a></li>" +
          "<li class=\"\"><a href=\"/toc/dmfr/current\">DMFR</a></li>" +
          "<li class=\"\"><a href=\"/toc/img/current\">Imaging</a></li>" +
          "<li class=\"\"><a href=\"/page/archcollec\">Archi Collec</a></li>" +
          "</ul></li></ul></div>" +
          "</div>";  

  private static final String withRelatedLayer = 
      "<div class=\"block\">" +  
            "<div class=\"relatedLayer\">" +
            "<div class=\"category\">" +
            "<h3>Original Article</h3>" +
            "<ul><li>" +
            "<a href=\"/doi/abs/11.1111/00001Y.000001\">" +
            "Assessment abc</a></li></ul></div></div>" +
            "</div>";
  
  private static final String withRelatedContent = 
      "<div class=\"block\">" +  
          "<div class=\"tab tab-pane\" id=\"relatedContent\">" +
          "<div class=\"category\">" +
          "<h3>Original Article</h3>" +
          "<ul><li>" +
          "<a href=\"/doi/abs/11.1111/jid.2007.11\">Diagnostic abc</a>" +
          "</li></ul></div></div>" +
          "</div>";
  
  private static final String withLiteratumAd =  
      "<div class=\"block\">" +
          "<div class=\"widget literatumAd none  widget-none\" id=\"526\">" +
          "<div class=\"wrapped\">" +
          "<div class=\"widget-body body body-none \"><div class=\"pb-ad\">" +
          "<a href=\"/action/ad\"\"><img src=\"/adsrc/advert.jpg\"></a>" +
          "</div></div></div></div>" +
          "</div>";
  
  private static final String withPageHeader =
      "<div class=\"block\">" +  
          "<div class=\"widget pageHeader\" id=\"pageHeader\">" +
          "<div class=\"page-header\">" +
          "<div data-pb-dropzone=\"main\">" +
          "</div></div></div>" +
          "</div>";
    
  private static final String withPageFooter =
      "<div class=\"block\">" + 
          "<div class=\"widget pageFooter\" id=\"pageFooter\">" +
          "<div class=\"widget-body body body-none  body-compact\">" +
          "<div class=\"page-footer\">" +
          "<div data-pb-dropzone=\"main\">" +
          "</div></div></div></div>" +
          "</div>";

  private static final String withLogoImage =
      "<div class=\"block\">" + 
          "<div class=\"widget general-image alignRight\" id=\"xx\">" +
          "<div class=\"wrapped \" >" +
          "<div class=\"widget-body body body-none \">" +
          "<img src=\"foo.png\"/>" +
          "</div></div></div>" +
          "</div>";  
  
  private static final String withAccessIconContainer =  
      "<div class=\"block\">" +
          "<td class=\"accessIconContainer\"><div>" +
          "<img src=\"/imagessrc/access_free.gif\" alt=\"Free Access\"" +
          "title=\"Free Access\" class=\"accessIcon freeAccess\"></div></td>" +
          "</div>"; 
  
  private static final String withFreeGif =
      "<div class=\"block\">" +
          "<img src=\"/imagessrc/free.gif\">" +
          "</div>";
  
  private static final String withPublicationTooldropdownContainer =
      "<div class=\"block\">" +
          "<div class=\"publicationTooldropdownContainer\">" +
          "<select name=\"articleTool\" " +
          "class=\"items-choice publicationToolSelect\" " +
          "title=\"Article Tools\">" +
          "<option value=\"\">Please select</option>" +
          "<option value=\"/action/addFav\">Add to Favourites</option>" +
          "<option value=\"/action/addCitAlert\">Track Citation</option>" +
          "<option value=\"/action/showMulAbs\">View Abstracts</option>" +
          "<option value=\"/action/showCitFormats\">Download Cit</option>" +
          "<option value=\"/action/showMailPage\">Email</option>" +
          "</select>" +
          "</div>" +
          "</div>";
  
  private static final String withLiteratumBookIssueNavigation =
      "<div class=\"block\">" +                                                                                                                                                              
          "<div class=\"widget literatumBookIssueNavigation\" id=\"843\">" +                                               
          "<div class=\"widget-body body body-none \">" +
          "<div class=\"pager issueBookNavPager\">" +                                                                          
          "<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">" +                                                                                                      
          "<tr><td class=\"journalNavLeftTd\">" +                                                                                                                         
          "<div class=\"prev placedLeft\">" +                                                                                                                 
          "<a href=\"/toc/jid/13/2\">&nbsp;</a>" +                                                                                                                                        
          "</div></td></tr></table>" +                                                                                                                                                        
          "</div></div></div>" +                                                                                                                                                          
          "</div>"; 
  
  private static final String withSocialMedia =
      "<div class=\"block\">" +  
          "<div class=\"widget general-bookmark-share none\" id=\"xx\">" +
          "<div class=\"addthis_toolbox addthis_default_style\">" +
          "<a class=\"addthis_button_email\"></a>" +
          "<a class=\"addthis_button_facebook\"></a>" +
          "<a class=\"addthis_button_twitter\"></a>" +
          "<a class=\"addthis_button_linkedin\"></a>" +
          "<a class=\"addthis_button_google_plusone_share\"></a>" +
          "<a class=\"addthis_button_compact\"></a>" +
          "</div></div>" +
          "</div>";
  
     private static final String withImpactFactorBlock =
        "<div class=\"block\">" + 
            "<div id=\"656\" class=\"widget layout-one-column none " +
            "widget-regular widget-border-toggle\">" +
            "<div class=\"widget-body body body-regular body-border-toggle\">" +
            "<div class=\"pb-columns row-fluid\">" +
            "<div class=\"width_1_1\">" +
            "<div data-pb-dropzone=\"center\">" +
            "<p>Issues per year:</p>" +
            "<p>ISSN:</p>" +
            "<p>eISSN:</p>" +
            "<p>Citation:</p>" +
            "<p>2013 Impact Factor: </p>" +
            "<p>5-year Impact Factor: </p>" +
            "</div></div></div></div></div>"+
            "</div>";
     
  private static final String withMathJaxMessage =
      "<div class=\"block\">" +  
          "<div id=\"MathJax_Message\" style=\"display: none;\"></div>" +
          "</div>";
      
    private static final String withArticleToolsWidgetExceptDownloadCitation =  
      "<div class=\"block\">" +  
          "<div class=\"widget literatumArticleToolsWidget\" id=\"594\">" +
          "<div class=\"articleTools\">" +
          "<ul class=\"blockLinks\">" +
          "<li class=\"addToFavs\"><a href=\"/action/fav-link\">Fav</a></li>" +
          "<li class=\"downloadCitations\"><a href=\"/action/showCitFormats?" +
          "doi=1.11111%2Fjid.2013.111\">Download Citation</a></li>" +
          "</ul></div></div>" +
          "</div>";
    
    private static final String articleToolsWidgetFiltered = 
            " Download Citation ";
    
    private static final String withCitedby =
      "<div class=\"block\">" + 
          "<ul><li>" +
          "<a href=\"/doi/citedby/11.1111/jid.20129999\"> Cited by </a>" +
          "</li></ul>" +
          "</div>"; 

    private static final String withSectionJumpTo =
        "<div class=\"block\">" +
            "<div class=\"sectionJumpTo\">" +
            "<div class=\"sectionLabel\">Section:</div>" +
            "<form style=\"margin-bottom:0\">" +
            "<select class=\"ful\" onchange=\"GoTo(this, 'sf')\" name=\"s\">" +
            "</form>" +
            "<div class=\"nextPrevSec\">" +
            "<a class=\"up\" href=\"#_i0\">" +
            "<img width=\"11\" border=\"0\" hspace=\"5\" height=\"9\" " +
            "alt=\"Previous section\" src=\"/imagesrc/aup.gif\"></a>" +
            "<a class=\"down\" href=\"#citart1\">" +
            "<img width=\"11\" border=\"0\" hspace=\"5\" height=\"9\" " +
            "alt=\"Next section\" src=\"/imagesrc/adown.gif\"></a>" +
            "</div>" +
            "</div>" +
            "</div>";
    
    
    private static final String mostReadHtml = 
        "<body>" +
            "<div class=\"widget literatumMostReadWidget none  widget-none  widget-border-toggle\" id=\"86d4f007-456e-4949-a310-442574085a02\"  >" +
            "<div class=\"wrapped \" >" +
            "<div class=\"widget-body body body-none  body-border-toggle\"><section class=\"popular\">" +
            "<div class=\"mostRead  \">" +
            "<h1 class=\"topContentTitle\">" +                                                                                                            
            "Most read articles" +                                                                                                                  
            "</h1><ul><li>art2></li><li>art2</li></ul>" +
            "</div></section></div></div></div>BOO</body>";
    
    private static final String referencesTable = 
        "<table border=\"0\" class=\"references\">" +
            "<tr id=\"b1\">" +
            "<td class=\"refnumber\">1.</td>" +
            "<td valign=\"top\"> <span class=\"NLM_string-name\">Foo <span class=\"NLM_given-names\">BL</span>" +
            "</span>, <span class=\"NLM_string-name\">Smith <span class=\"NLM_given-names\">BS</span>" +
            "</span>. <span class=\"NLM_article-title\">Infection: prevention and management</span>. <i>Instr Course Lect</i> " +
            "<span class=\"NLM_year\">2012</span>; 61: " +
            "<span class=\"NLM_fpage\">411</span>–<span class=\"NLM_lpage\">19</span>.  " +
            "<a href=\"/servlet/linkout?suffix=b1&amp;dbid=8&amp;doi=10.1111%2Fbjr.foo&amp;key=x0\" onclick=\"newWindow(this.href);return false\">Medline</a>" +
            "</td>" +
            "</tr>" +
            "</table>";
    private static final String referencesTableFiltered = 
            " Foo BL" +
            " Smith BS" +
            " Infection: prevention and management" +
            " 2012 " +
            "411 19 ";

    private static final String mostReadHtmlFiltered = 
        " BOO ";   
    private static final String mostReadHtmlCrawlFiltered = 
        "<body>BOO</body>";   
    
  protected ArchivalUnit createAu()
      throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_ID,  arrsAuConfig());
  }
  
  private Configuration arrsAuConfig() {
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
    bau = createAu();
  }
  
  // Variant to test with Crawl Filter
  public static class TestCrawl extends TestBIRAtyponHtmlFilterFactory {
    public void testFiltering() throws Exception {
      variantFact = new BIRAtyponHtmlCrawlFilterFactory();
      doFilterTest(bau, variantFact, withMenuXml, filteredCrawlStr); 
      doFilterTest(bau, variantFact, withRelatedLayer, filteredCrawlStr);      
      doFilterTest(bau, variantFact, withRelatedContent, filteredCrawlStr);      
      doFilterTest(bau, variantFact, mostReadHtml, mostReadHtmlCrawlFiltered);      
    }    
  }

  // Variant to test with Hash Filter
   public static class TestHash extends TestBIRAtyponHtmlFilterFactory {   
     public void testFiltering() throws Exception {
      variantFact = new BIRAtyponHtmlHashFilterFactory();
        doFilterTest(bau, variantFact, withLiteratumAd, filteredStr); 
        doFilterTest(bau, variantFact, withPageHeader, filteredStr); 
        doFilterTest(bau, variantFact, withPageFooter, filteredStr); 
        doFilterTest(bau, variantFact, withLogoImage, filteredStr); 
        doFilterTest(bau, variantFact, withMenuXml, filteredStr); 
        doFilterTest(bau, variantFact, withAccessIconContainer, filteredStr); 
        doFilterTest(bau, variantFact, withFreeGif, filteredStr);
        doFilterTest(bau, variantFact, withPublicationTooldropdownContainer, 
                     filteredStr); 
        doFilterTest(bau, variantFact, withLiteratumBookIssueNavigation, 
                     filteredStr);         
        doFilterTest(bau, variantFact, withSocialMedia, filteredStr); 
        doFilterTest(bau, variantFact, withImpactFactorBlock, filteredStr); 
        doFilterTest(bau, variantFact, withMathJaxMessage, filteredStr);         
        doFilterTest(bau, variantFact, 
                     withArticleToolsWidgetExceptDownloadCitation, 
                     articleToolsWidgetFiltered);
        doFilterTest(bau, variantFact, withCitedby, filteredStr); 
        doFilterTest(bau, variantFact, withSectionJumpTo, filteredStr);         
        doFilterTest(bau, variantFact, mostReadHtml , mostReadHtmlFiltered);
        doFilterTest(bau, variantFact, referencesTable, referencesTableFiltered);
     }
  }
  
  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
      });
  }
  
}

