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
   
  // test for pdf and pdfplus file size
  // is in TestBaseAtyponHtmlHashFilterFactory since the html is similar
  // <a class="ref nowrap pdfplus" target="_blank" title="Opens new window" 
  // href="/doi/pdfplus/10.12968/bjom.2013.21.10.692">PDF Plus (672 KB)</a>
  
  // institution banner
  // http://www.magonlinelibrary.com/doi/ref/10.12968/bjom.2013.21.10.701
  private static final String withLiteratumInstitutionBanner =
      "<div class=\"block\">" + 
          "<div id=\"da3\" class=\"widget literatumInstitutionBanner none slogan widget-none\">" +
          "<div class=\"wrapped \">" +
          "<div class=\"widget-body body body-none \">" +
          "<div class=\"welcome\"> </div>" +
          "</div></div></div>" +
          "</div>";

  // from toc - ad panel has link to other issue 
  // http://www.magonlinelibrary.com/toc/bjom/21/10
  private static final String withGenericSlideshow =
      "<div class=\"block\">" +  
          "<div class=\"widget genericSlideshow none  widget-none  widget-compact-all\" id=\"b98\">" +
          "<div class=\"widget-body body body-none  body-compact-all\">" +
          "<div class=\"slides\">" +
          "<div class=\"widget-body body body-none \"><img src=\"/rawimage/jid-22-10.gif\"</div>" +
          "<p><a href=\"http://www.magonlinelibrary.com/doi/full/10.12968/bjom.2014.22.10.694\" id=\"jidlink\">read</a></p>" +
          "</div></div></div>" +
          "</div>";  
  
  // toc, abs, full, text and ref right column - most read 
  // http://www.magonlinelibrary.com/doi/full/10.12968/bjom.2013.21.10.688
  private static final String withLiteratumMostReadWidget =
      "<div class=\"block\">" +
          "<div id=\"7e1\" class=\"widget literatumMostReadWidget alignLeft " +
          "widget-none widget-compact-all\">" +
          "<div class=\"widget-body body body-none body-compact-all\">" +
          "<section class=\"popular\">" +
          "<div class=\"mostRead\">" +
          "<ul><li><div class=\"title\">" +
          "<a href=\"/doi/abs/10.12968/bjom.2013.21.6.454\">Effective communication in midwifery</a>" +
          "</div></li></ul></div></section>" +
          "</div></div>" +
          "</div>";
  
  // top page ad and all other ads with class LiteratumAd
  private static final String withLiteratumAd =
      "<div class=\"block\">" +
          "<div class=\"widget literatumAd alignCenter  widget-none\" " +
          "id=\"bac8\">" +
          "<div class=\"wrapped 00_00\">" +
          "<div class=\"widget-body body body-none \">" +
          "<div class=\"pb-ad\">" +
          "<iframe width=\"728\" height=\"90\" frameborder=\"0\" " +
          "src=\"http://ad-link\">blah blah;</iframe>" +
          "</div></div></div></div>" +
          "</div>";
  
  // pageHeader - has links to current issue
  // <section class="widget pageHeader   widget-none" id="pageHeader">
  private static final String withPageHeader =
      "<div class=\"block\">" +
          "<div class=\"widget pageHeader \" id=\"pageHeader\">" +
          "<div class=\"widget-body body body-none \">" +
          "<div data-pb-dropzone=\"main\">" +
          "<div class=\"widget-body body body-none \">" +
          "<div class=\"width_1_2\">" +
          "<div class=\"widget general-image \" id=\"3f2\">" +
          "<div class=\"widget-body body body-none \">" +
          "<a href=\"/toc/jid/current\"><img src=\"/logos.png\"></a></div>" +
          "</div></div></div></div></div></div>" +
          "</div>";     
  
  // for toc - social media
  private static final String withSocialMedia =  
      "<div class=\"block\">" +
          "<div class=\"widget general-bookmark-share alignCenter " +
          "addthisborder widget-none\" id=\"d62\">" +
          "<div class=\"addthis_toolbox addthis_default_style\">" +
          "<a class=\"addthis_button_facebook\"></a>" +
          "<a class=\"addthis_button_twitter\"></a>" +
          "</div></div>" +
          "</div>";
  
  // from toc - access icon container 
  private static final String withAccessIconContainer =  
      "<div class=\"block\">" +
        "<td class=\"accessIconContainer\"><div></div></td>" +
      "</div>"; 
  
  // middle column ad of an article - all article tools with 
  // class literatumArticleToolsWidget except Download Citations
  // http://www.magonlinelibrary.com/doi/abs/10.12968/bjom.2013.21.10.701
  private static final String withArticleToolsWidgetExceptDownloadCitation = 
      "<div class=\"block\">" +
          "<div class=\"widget literatumArticleToolsWidget\" id=\"5ce\">" +
          "<ul class=\"linkList blockLinks separators centered\">" +
          "<li class=\"addToFavs\"><a href=\"/action/fav-link\">Fav</a></li>" +
          "<li class=\"downloadCitations\">" +
          "<a href=\"/action/showCitFormats?" +
          "doi=11.11111%2Fjid.2013.111\">Download citation</a></li>" +
          "<li class=\"trackCitations\">" +
          "<a href=\"/action/addCitationAlert?" +
          "doi=11.11111%2Fjid.2013.11.11.711\">Track citations</a></li>" +
          "</ul></div>" +
          "</div>";
  
  private static final String ArticleToolsWidgetFiltered = 
      "<div class=\"block\">" +
          "<div class=\"widget literatumArticleToolsWidget\" >" +
          "<ul class=\"linkList blockLinks separators centered\">" +
          "<li class=\"downloadCitations\">" +
          "<a href=\"/action/showCitFormats?" +
          "doi=11.11111%2Fjid.2013.111\">Download citation</a></li></ul>" +
          "</div>" +
          "</div>";  
   
  // from full text - Downloaded count
  // http://www.magonlinelibrary.com/doi/full/10.12968/bjom.2013.21.10.692
  private static final String withLiteratumContentItemDownloadCount = 
     "<div class=\"block\">" +
         "<div class=\"widget literatumContentItemDownloadCount widget-box\" id=\"33f\">" +
         "<div class=\"widget-body\">Downloaded 22 times</div></div>" +
         "</div>";
  
  // pageFooter
  private static final String withPageFooter =  
      "<div class=\"block\">" +
          "<div class=\"widget pageFooter \" id=\"pageFooter\">" +
          "<div class=\"widget-body body body-none \">" +
          "<div class=\"page-footer\">" +
          "<div data-pb-dropzone=\"main\">" +
          "<h3>ABOUT</h3>" +
          "<ul><li><a href=\"/page/privacy\">Site privacy</a></li></ul>" +
          "</div></div></div></div>" +
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
      doFilterTest(maau, variantFact, withGenericSlideshow, filteredStr); 
      doFilterTest(maau, variantFact, withLiteratumMostReadWidget, filteredStr);    
    }    
  }

  // Variant to test with Hash Filter
   public static class TestHash extends TestMarkAllenHtmlFilterFactory {   
     public void testFiltering() throws Exception {
      variantFact = new MarkAllenHtmlHashFilterFactory();
      doFilterTest(maau, variantFact, withLiteratumInstitutionBanner, 
                   filteredStr);
      doFilterTest(maau, variantFact, withLiteratumAd, filteredStr);
      doFilterTest(maau, variantFact, withPageHeader, filteredStr);
      doFilterTest(maau, variantFact, withGenericSlideshow, filteredStr); 
      doFilterTest(maau, variantFact, withSocialMedia, filteredStr);
      doFilterTest(maau, variantFact, withAccessIconContainer, filteredStr);
      doFilterTest(maau, variantFact, 
                   withArticleToolsWidgetExceptDownloadCitation, 
                   ArticleToolsWidgetFiltered);
      doFilterTest(maau, variantFact, withLiteratumContentItemDownloadCount, 
                   filteredStr);
      doFilterTest(maau, variantFact, withLiteratumMostReadWidget, filteredStr);
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

