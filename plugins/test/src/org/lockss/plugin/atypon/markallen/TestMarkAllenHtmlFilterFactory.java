/*
 * $Id: TestMarkAllenHtmlFilterFactory.java,v 1.1 2014-10-29 21:12:02 ldoan Exp $
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

package org.lockss.plugin.atypon.markallen;

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

public class TestMarkAllenHtmlFilterFactory
  extends LockssTestCase {
  
  FilterFactory variantFact;
  ArchivalUnit maau;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;
        
  private static final String PLUGIN_NAME = 
      "org.lockss.plugin.atypon.markallen.ClockssMarkAllenPlugin";
  
  private static final String filteredStr = 
      "<div class=\"block\"></div>";

  // For crawl filtering

  // panel under pageHeader has link current toc
  // http://www.magonlinelibrary.com/toc/bjom/21/10
  private static final String withCurrentIssuePanel = 
      "<div class=\"block\">" +  
          "<section class=\"widget general-image none  \" id=\"b2c\">" +
          "<div class=\"wrapped \">" +
          "<div class=\"widget-body body body-none \">" +
          "<a href=\"http://www.base.com/doi/full/11.1111/jid.2014.111\">" +
          "<img src=\"/images/ad.gif\"></a></div>" +
          "</div></section>" +
          "</div>";
  
  // middle column ad of an article - 
  // all article tools except Download Citations
  // http://www.magonlinelibrary.com/doi/full/10.12968/bjom.2013.21.10.701
  private static final String withArticleToolsExceptDownloadCitation = 
      "<div class=\"block\">" +
          "<div class=\"articleTools\">" +
          "<ul class=\"linkList blockLinks separators centered\">" +
          "<li class=\"addToFavs\">" +
          "<a href=\"/add-fav-link1\">fav</a>" +
          "</li><li class=\"email\">" +
          "<a href=\"/email-link\">email</a>" +
          "</li>" +
          "<li class=\"downloadCitations\">" +
          "<a href=\"/action/showCitFormats?" +
          "doi=11.11111%2Fhid.2013.11.11.111\">Download citation</a>" +
          "</li>" +
          "<li class=\"trackCitations\">" +
          "<a href=\"/track-citation-link\">Track citations</a>" +
          "</li>" +
          "<li class=\"rightsLink\">" +
          "<a href=\"/permision-link\" class=\"rightslink\">permission</a>" +
          "</li>" +
          "</ul>" +
          "</div>" +
          "</div>";
  
  private static final String articleToolsFiltered = 
      "<div class=\"block\">" +
          "<div class=\"articleTools\">" +
          "<ul class=\"linkList blockLinks separators centered\">" +
          "<li class=\"downloadCitations\">" +
          "<a href=\"/action/showCitFormats?" +
          "doi=11.11111%2Fhid.2013.11.11.111\">Download citation</a>" +
          "</li>" +
          "</ul>" +
          "</div>" +
          "</div>";
  
  // toc or full text right column - most read
  private static final String withMostRead =
      "<div class=\"block\">" +
          "<div class=\"mostRead\">" +
          "<ul>" +
          "<li>" +
          "<div class=\"title\">" +
          "<a href=\"/doi/abs/10.12968/bjom.2013.21.6.454\">xyx1</a>" +
          "</div>" +
          "<div class=\"authors\"> </div>" +
          "<div class=\"volumeIssue\"></div>" +
          "</li>" +
          "<li>" +
          "<div class=\"title\">" +
          "<a href=\"/doi/abs/11.11111/jid.2009.11.1.11111\">xyx2</a>" +
          "</div>" +
          "<div class=\"authors\">Leap </div>" +
          "<div class=\"volumeIssue\"></div>" +
          "</li>" +
          "</ul>" +
          "</div>" +
          "</div>";
  
  // For hash filtering
  // top page ad and all other ads with class LiteratumAd
  private static final String withLiteratumAd =
      "<div class=\"block\">" +
          "<section class=\"widget literatumAd alignCenter  widget-none\" " +
          "id=\"bac8\">" +
          "<div class=\"wrapped 00_00\">" +
          "<div class=\"widget-body body body-none \">" +
          "<div class=\"pb-ad\">" +
          "<iframe width=\"728\" height=\"90\" frameborder=\"0\" " +
          "src=\"http://ad-link\">blah blah;</iframe>" +
          "</div></div></div>" +
          "</section>" +
          "</div>";
  
  // pageHeader - has links to current issue
  // <section class="widget pageHeader   widget-none" id="pageHeader">
  private static final String withPageHeader =
      "<div class=\"block\">" +
          "<section class=\"widget pageHeader \" id=\"pageHeader\">" +
          "<div class=\"widget-body body body-none \">" +
          "<div data-pb-dropzone=\"main\">" +
          "<div class=\"widget-body body body-none \">" +
          "<div class=\"width_1_2\">" +
          "<section class=\"widget general-image \" id=\"3f2\">" +
          "<div class=\"widget-body body body-none \">" +
          "<a href=\"/toc/jid/current\"><img src=\"/logos.png\"></a></div>" +
          "</section>" +
          "</div></div></div></div>" +
          "</section>" +
          "</div>";     
  
  // for toc - social media
  private static final String withSocialMedia =  
      "<div class=\"block\">" +
          "<section class=\"widget general-bookmark-share alignCenter " +
          "addthisborder widget-none\" id=\"d62\">" +
          "<div class=\"addthis_toolbox addthis_default_style\">" +
          "<a class=\"addthis_button_facebook\"></a>" +
          "<a class=\"addthis_button_twitter\"></a>" +
          "</div>" +
          "</section>" +
          "</div>";
  
  // from toc - access icon container 
  private static final String withAccessIconContainer =  
      "<div class=\"block\">" +
        "<td class=\"accessIconContainer\"><div></div></td>" +
      "</div>"; 
  
  // middle column ad of an article - all article tools with 
  // class literatumArticleToolsWidget except Download Citations
  // http://www.magonlinelibrary.com/doi/abs/10.12968/bjom.2013.21.10.701
  private static final String withArticleToolsWidgetsExceptDownloadCitation = 
      "<div class=\"block\">" +
          "<section class=\"widget literatumArticleToolsWidget\" id=\"5ce\">" +
          "<ul class=\"linkList blockLinks separators centered\">" +
          "<li class=\"addToFavs\"><a href=\"/action/fav-link\">Fav</a></li>" +
          "<li class=\"downloadCitations\">" +
          "<a href=\"/action/showCitFormats?" +
          "doi=11.11111%2Fjid.2013.111\">Download citation</a>" +
          "</li>" +
          "<li class=\"trackCitations\">" +
          "<a href=\"/action/addCitationAlert?" +
          "doi=11.11111%2Fjid.2013.11.11.711\">Track citations</a>" +
          "</li>" +
          "</ul>" +
          "</section>" +
          "</div>";
  
  private static final String ArticleToolsWidgetsFiltered = 
      "<div class=\"block\">" +
          "<section class=\"widget literatumArticleToolsWidget\" id=\"5ce\">" +
          "<ul class=\"linkList blockLinks separators centered\">" +
          "<li class=\"downloadCitations\">" +
          "<a href=\"/action/showCitFormats?" +
          "doi=11.11111%2Fjid.2013.111\">Download citation</a>" +
          "</li>" +
          "</ul>" +
          "</section>" +
          "</div>";
  
  // abs, full text and ref right column - most read 
  // http://www.magonlinelibrary.com/doi/full/10.12968/bjom.2013.21.10.688
  private static final String withLayoutTabs =  
      "<div class=\"block\">" +
          "<section class=\"widget layout-tabs none \" id=\"6e3\">" +
          "<div class=\"widget-body body body-none \">" +
          "<div class=\"tabs tabs-widget\">" +
          "<ul class=\"tab-nav\">" +
          "<li class=\"active \">" +
          "<a href=\"#69a\">Most Read</a>" +
          "</li>" +
          "</ul>" +
          "</div></div>" +
          "</section>" +
          "</div>";   
  
  // full text right colummn - several locations with ids
  // http://www.magonlinelibrary.com/doi/full/10.12968/bjom.2013.21.10.688
  private static final String withLayoutTwoColumns =  
      "<div class=\"block\">" +
          "<section class=\"widget layout-two-columns \" id=\"f92\">" +
          "<div class=\"widget-body body body-none \">" +
          "<div class=\"width_1_2\">" +
          "<div dropzone=\"left\" class=\"autoheight\">" +
          "</div></div>" +
          "<div class=\"width_1_2\">" +
          "<div dropzone=\"right\" class=\"autoheight\">" +
          "</div></div></div>" +
          "</section>" +
          "</div>";  
  
  // pageFooter
  private static final String withPageFooter =  
      "<div class=\"block\">" +
          "<section class=\"widget pageFooter \" id=\"pageFooter\">" +
          "<div class=\"widget-body body body-none \">" +
          "<div class=\"page-footer\">" +
          "<div data-pb-dropzone=\"main\">" +
          "<h3>ABOUT</h3>" +
          "<ul><li><a href=\"/page/privacy\">Site privacy</a></li></ul>" +
          "<div><div><div>" +
          "</section>" +
          "</div>";     
  

  
  protected ArchivalUnit createAu()
      throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_NAME,  maAuConfig());
  }
  
  private Configuration maAuConfig() {
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
    maau = createAu();
  }
  
  // Variant to test with Crawl Filter
  public static class TestCrawl extends TestMarkAllenHtmlFilterFactory {
    public void testFiltering() throws Exception {
      variantFact = new MarkAllenHtmlCrawlFilterFactory();
      doFilterTest(maau, variantFact, withCurrentIssuePanel, filteredStr);
      doFilterTest(maau, variantFact, 
          withArticleToolsExceptDownloadCitation, articleToolsFiltered);
      doFilterTest(maau, variantFact, withMostRead, filteredStr);
    }    
  }

  // Variant to test with Hash Filter
   public static class TestHash extends TestMarkAllenHtmlFilterFactory {   
     public void testFiltering() throws Exception {
      variantFact = new MarkAllenHtmlHashFilterFactory();
      doFilterTest(maau, variantFact, withLiteratumAd, filteredStr);
      doFilterTest(maau, variantFact, withPageHeader, filteredStr);
      doFilterTest(maau, variantFact, withCurrentIssuePanel, filteredStr);
      doFilterTest(maau, variantFact, withSocialMedia, filteredStr);
      doFilterTest(maau, variantFact, withAccessIconContainer, filteredStr);
      doFilterTest(maau, variantFact, 
          withArticleToolsWidgetsExceptDownloadCitation, 
          ArticleToolsWidgetsFiltered);
      doFilterTest(maau, variantFact, withLayoutTabs, filteredStr);
      doFilterTest(maau, variantFact, withLayoutTwoColumns, filteredStr);
      doFilterTest(maau, variantFact, withPageFooter, filteredStr);
    }
  }
  
  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
      });
  }
  
}

