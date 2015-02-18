/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.bepress;

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

public class TestDigitalCommonsRepositoryHtmlFilterFactory
  extends LockssTestCase {
  
  FilterFactory variantFact;
  ArchivalUnit variantAu;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;
        
  private static final String PLUGIN_NAME =
      "org.lockss.plugin.bepress.DigitalCommonsRepositoryPlugin";
  
  private static final String filteredStr = 
      "<div class=\"block\"></div>";
        
  private static final String withFirstAndSecond = 
      "<div class=\"block\">"
          + "<div class=\"lockss_year_or_keyword_heading\">"
          + "<h4 id=\"year_or_keyword_heading\">Administrative Agency Documents</h4>"
	  + "<p class=\"pdf\"><a href=\"http://blah\">PDF</a></p>"
	  + "<p><a href=\"http://blah122\">A Data</a></p>"
	  + "</div>"
	  + "<div class=\"lockss_other_year_or_other_keyword_heading\">"
	  + "<h4 id=\"other_year_or_other_keyword_heading\">Archival Materials</h4>" 
	  + "<p class=\"pdf\"><a href=\"http://statistics\">PDF</a></p>"
	  + "</div>"
	  + "</div>";

  private static final String withoutSecond = 
      "<div class=\"block\">"
          + "<div class=\"lockss_year_or_keyword_heading\">"
          + "<h4 id=\"year_or_keyword_heading\">Administrative Agency Documents</h4>"
	  + "<p class=\"pdf\"><a href=\"http://blah\">PDF</a></p>"
	  + "<p><a href=\"http://blah122\">A Data</a></p>"
	  + "</div>"
	  + "</div>";
    
  private static final String withHeader = 
      "<div class=\"block\">"
          + "<div id =\"header\">"
          + "<div id=\"logo\">"
          + "<a href=\"http://www.university.edu/\" id=\"xyz1111\">"
          + "<img title=\"logo.gif\" src=\"/xyximages/aaa.gif\"></a>"
          + "</div></div>"
          + "</div>";
  
  private static final String withBreadcrumb = 
      "<div class=\"block\">"
          + "<div id=\"breadcrumb\">"
          + "<div class=\"crumbs\"><p>"
          + "<a href=\"http://base.edu\">Home</a>"
          + "&gt; <a href=\"http://base.edu/xxx\">AAA College</a>"
          + "</div></div>"
          + "</div>";
  
  private static final String withSidebar = 
      "<div class=\"block\">"
          + "<div id=\"sidebar\">"
          + "<a href=\"http://base.edu/do/search\">Advanced Search</a>"
          + "<ul><li>email</li></ul>"
          + "<ul><li>browse</li></ul>"
          + "</div>"
          + "</div>";
  
  private static final String withPager = 
      "<div class=\"block\">"
        + "<ul id=\"pager\">"
        + "<li>&lt; <a href=\"http://base.edu/xxx/73\">Previous</a></li>"
        + "<li><a href=\"http://base.edu/xxx/54\">Next</a> &gt;</li>"
        + "<li>&nbsp;</li></ul>"
        + "</div>";
  
  private static final String withFooter = 
      "<div class=\"block\">"
        + "<div id=\"footer\">"
        + "<div id=\"network\">"
	+ "<p class=\"nw\">Network</p>"
	+ "</div>"
	+ "<em>Digital</em>"
	+ "</div>"
        + "</div>";
  
  private static final String withLockssProbe = 
      "<div class=\"block\">"
        + "<link href=\"http://base.edu/xyz\" lockss-probe=\"true\">"
        + "</div>";
  
   private static final String withScript =
     "<div class=\"block\">"
       + "<script type=\"text/javascript\">"
       + "var _gaq = _gaq || [];"
       + "blah blah;"
       + "</script>"
       + "</div>";
 
  private static final String withComments =
      "<div class=\"block\">"
        + "<!-- comment comment comment -->"
        + "</div>";
    
  private static final String withStylesheet =
      "<div class=\"block\">"
        + "<link rel=\"stylesheet\" href=\"/aaa.css\" type=\"text/css\">"
        + "<link rel=\"stylesheet\" href=\"/bbb.css\" type=\"text/css\">"
        + "</div>";
  
  private static final String withNofollow =
      "<div class=\"block\">"
        + "<a rel=\"nofollow\" href=\"http://net.blah?pub=NyYy\">Follow</a>"
        + "</div>";
  
  private static final String withIncludeIn =
      "<div class=\"block\">"
        + "<div id=\"beta-disciplines\" class=\"aside\">"
        + "<h4>Included in</h4>"
	+ "<a href=\"http://net.com/blah\">XYX</a>"
	+ "</div>"
        + "</div>";
  
  private static final String withSocialMedia =
      "<div class=\"block\">"
        + "<div id=\"share\" class=\"aside\">"
        + "<h4>Share</h4>"
        + "<p class=\"addthis_toolbox addthis_default_style\">"
        + "<a><span>facebook</span></a>"
        + "<a><span>twitter</span></a>"
        + "</p></div>"
        + "</div>";
  
  private static final String withZ3988 =
      "<div class=\"block\">"
        + "<span class=\"Z3988\" date=2013-02-06\">SSS</span>"
        + "</div>";
  
  private static final String withSkiplink =
      "<div class=\"block\">"
        + "<a href=\"#main\" class=\"skiplink\""
        + "accesskey=\"99\">Skip to my lou</a>"
        + "</div>";
  
  private static final String withNavigation =
      "<div class=\"block\">"
        + "<div id=\"navigation\">"
        + "<ul id=\"yyy\">"
        + "<li id=\"tone\"><a href=\"http://base.edu/blah\">"
        + "<span id=\"uuu\">account</span></a></li>"
        + "</ul></div>"
        + "</div>";


  protected ArchivalUnit createAu()
      throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_NAME,  dcpAuConfig());
  }
  
  private Configuration dcpAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", "http://www.example.com/");
    conf.put("collection_type", "abctype");
    conf.put("collection", "abc");
    conf.put("collection_heading", "year_or_keyword_heading");
    return conf;
  }
  
  private static void doFilterTest(ArchivalUnit au, 
      FilterFactory fact, String nameToHash, String afterFilteringStr) 
          throws PluginException, IOException {
    InputStream actIn; 
    actIn = fact.createFilteredInputStream(au, 
        new StringInputStream(nameToHash), Constants.DEFAULT_ENCODING);
    assertEquals(afterFilteringStr, StringUtil.fromInputStream(actIn));
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
    extends TestDigitalCommonsRepositoryHtmlFilterFactory {
    public void setUp() throws Exception {
      super.setUp();
      tempDirPath = setUpDiskSpace();
      startMockDaemon();
      variantFact = new DigitalCommonsRepositoryHtmlCrawlFilterFactory();
      variantAu = createAu();
    }    
    public void testFiltering() throws Exception {
      doFilterTest(variantAu, variantFact, withFirstAndSecond, withoutSecond);
      doFilterTest(variantAu, variantFact, withHeader, filteredStr);
      doFilterTest(variantAu, variantFact, withBreadcrumb, filteredStr);
      doFilterTest(variantAu, variantFact, withSidebar, filteredStr);
      doFilterTest(variantAu, variantFact, withPager, filteredStr);
      doFilterTest(variantAu, variantFact, withFooter, filteredStr);
      doFilterTest(variantAu, variantFact, withLockssProbe, filteredStr);
    }    
  }

  // Variant to test with Hash Filter
   public static class TestHash 
    extends TestDigitalCommonsRepositoryHtmlFilterFactory {   
    public void setUp() throws Exception {
      super.setUp();
      tempDirPath = setUpDiskSpace();
      startMockDaemon();
      variantFact = new DigitalCommonsRepositoryHtmlHashFilterFactory();
      variantAu = createAu();
    }
    public void testFiltering() throws Exception {
      doFilterTest(variantAu, variantFact, withScript, filteredStr);
      doFilterTest(variantAu, variantFact, withComments, filteredStr);
      doFilterTest(variantAu, variantFact, withStylesheet, filteredStr);
      doFilterTest(variantAu, variantFact, withHeader, filteredStr);
      doFilterTest(variantAu, variantFact, withBreadcrumb, filteredStr);
      doFilterTest(variantAu, variantFact, withSidebar, filteredStr);
      doFilterTest(variantAu, variantFact, withPager, filteredStr);
      doFilterTest(variantAu, variantFact, withFooter, filteredStr);
      doFilterTest(variantAu, variantFact, withNofollow, filteredStr);
      doFilterTest(variantAu, variantFact, withIncludeIn, filteredStr);
      doFilterTest(variantAu, variantFact, withSocialMedia, filteredStr);
      doFilterTest(variantAu, variantFact, withZ3988, filteredStr);
      doFilterTest(variantAu, variantFact, withSkiplink, filteredStr);
      doFilterTest(variantAu, variantFact, withNavigation, filteredStr);
    }
  }
  
  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
      });
  }
  
}

