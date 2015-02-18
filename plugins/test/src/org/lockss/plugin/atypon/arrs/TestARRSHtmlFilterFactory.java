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

package org.lockss.plugin.atypon.arrs;

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

public class TestARRSHtmlFilterFactory
  extends LockssTestCase {
  
  FilterFactory variantFact;
  ArchivalUnit arrsau;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;
        
  private static final String PLUGIN_ID = 
      "org.lockss.plugin.atypon.arrs.ARRSPlugin";
  
  private static final String filteredStr = 
      "<div class=\"block\"></div>";
  
  // test for pdf and pdfplus file size
  // is in TestBaseAtyponHtmlHashFilterFactory since the html is similar
  // <a class="ref nowrap" target="_blank" title="Opens new window" 
  // href="/doi/pdf/10.2214/AJR.12.9355">PDF (823 KB)</a>
  
  // from toc, abs, full - whole left sidebar
  // http://www.ajronline.org/doi/full/10.2214/AJR.12.10221
  private static final String withLeftSidebar = 
      "<div class=\"block\">" +
          "<div id=\"dropzone-Left-Sidebar\" >" +
          "<div id=\"widget1\" class=\"widget ui-helper-clearfix\">" +
          "<div id=\"journalNavPanel\">" +
          "<div class=\"issueNavigator\"></div>" +
          "</div></div>" +
          "<div id=\"widget2\" class=\"widget type-related-publications\">" +
          "<div class=\"relatedPubl\"><h3>Related title</h3></div>" +
          "<div class=\"relatedPubCits\">" +
          "<span class=\"relatedPubSearch\">Citing: </span>" +
          "<ul class=\"bullet\">" +
          "<li>" +
          "<div class=\"xxx\">" +
          "<a href=\"/linkout/xxx.11.11111\">google link</a>" +
          "</div></li></ul>" +
          "</div></div>" +
          "<div id=\"widget3\" class=\"widget type-ad-placeholder\">" +
          "<div class=\"view\"><div class=\"view-inner\"> </div>" +
          "</div></div>" +
          "</div>" +
          "</div>";
  
  // from abs, full - Previous Article|Next Article:
  // http://www.ajronline.org/doi/abs/10.2214/AJR.12.10039
  private static final String withArticleToolsNav = 
      "<div class=\"block\">" +
          "<div id=\"articleToolsNav\" class=\"stackContents\">" +
          "<a class=\"ref nowrap\" href=\"/doi/abs/11.1111/AJR.11.11111\">" +
          "<span>Ç prev article</span></a>" +
          "<a href=\"/doi/abs/11.1111/AJR.11.11111\">" +
          "<span>next art È</span></a>" +
          "</div>" +
          "</div>";
  
  // from abs, full - Recommended Articles
  // http://www.ajronline.org/doi/full/10.2214/AJR.12.9120
  private static final String withRecommendedArticles = 
      "<div class=\"block\">" +
          "<div id=\"widget1\" class=\"widget type-recommendedArticles\">" +
          "<div class=\"header \"><h3>rec art</h3></div>" +
          "<div class=\"recommendedArticles\">" +
          "<h4>Article tittle X</h4>" +
          "<ul>" +
          "</div>" +
          "</div>" +
          "</div>";
      
  // from toc - accessIcon  
  private static final String withAccessIcon = 
      "<div class=\"block\">" +
          "<img class=\"accessIcon fullAccess\" title=\"full\" " +
          "alt=\"full\" src=\"/imagessrc/accessfull.gif\">" +
          "</div>";
  
  // from toc - credit icon
  private static final String withCreditIcon = 
      "<div class=\"block\">" +
          "<img class=\"CMESAM\" src=\"/imagessrc/CREDITicon.gif\">" +
          "</div>";
  
  // from abs - share/email button below article title
  // http://www.ajronline.org/doi/full/10.2214/AJR.12.10221
  private static final String withArticleAdditionalLinks = 
      "<div class=\"block\">" +
          "<div class=\"articleAdditionalLinks\">" +
          "<a class=\"emailLink\" " +
          "href=\"/action/show?href=/doi/abs/11.1111/yyy.11.11111\">" +
          "<img alt=\"Share\" src=\"/imagessrc/email.gif\">Share</a>" +
          "</div>" +
          "</div>";
  
  // from abs, full - 'Choose' pulldown near References section
  // some page collected with 'CITING ARTICLES', some without
  // http://www.ajronline.org/doi/full/10.2214/AJR.12.9121
  private static final String withSectionHeading = 
      "<div class=\"block\">" +
          "<table class=\"sectionHeading\"" +
          "<tr>" +
          "<th>ABSTRACT</th>" +
          "<td class=\"sectionHeading\">" +
          "<form><select name=\"s\"" +
          "class=\"abc\" onchange=\"GoTo(this, 'self')\">" +
          "<option value=\"\" selected=\"\">Choose</option>" +
          "<option value=\"#\">top</option>" +
          "<option value=\"\">ABSTRACT&lt;&lt;</option>" +
          "<option value=\"#4\">conclusion</option>" +
          "<option value=\"#5\">references</option>" +
          "<option value=\"#citart1\">CITING</option></select>" +
          "</form></td>" +
          "</tr>" +
          "</table>" +
          "</div>";
  
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
    arrsau = createAu();
  }
  
  // Variant to test with Crawl Filter
  public static class TestCrawl extends TestARRSHtmlFilterFactory {
    public void testFiltering() throws Exception {
      variantFact = new ARRSHtmlCrawlFilterFactory();
      doFilterTest(arrsau, variantFact, withLeftSidebar, filteredStr); 
      doFilterTest(arrsau, variantFact, withArticleToolsNav, filteredStr); 
      doFilterTest(arrsau, variantFact, withRecommendedArticles, filteredStr);       
    }    
  }

  // Variant to test with Hash Filter
   public static class TestHash extends TestARRSHtmlFilterFactory {   
     public void testFiltering() throws Exception {
      variantFact = new ARRSHtmlHashFilterFactory();
      doFilterTest(arrsau, variantFact, withAccessIcon, filteredStr); 
      doFilterTest(arrsau, variantFact, withCreditIcon, filteredStr); 
      doFilterTest(arrsau, variantFact, withLeftSidebar, filteredStr); 
      doFilterTest(arrsau, variantFact, withArticleToolsNav, filteredStr); 
      doFilterTest(arrsau, variantFact, withRecommendedArticles, filteredStr); 
      doFilterTest(arrsau, variantFact, withArticleAdditionalLinks, 
                   filteredStr); 
      doFilterTest(arrsau, variantFact, withSectionHeading, filteredStr); 
    }
  }
  
  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
      });
  }
  
}

