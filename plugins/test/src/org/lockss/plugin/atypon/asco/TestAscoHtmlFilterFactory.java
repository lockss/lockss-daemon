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

package org.lockss.plugin.atypon.asco;

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

public class TestAscoHtmlFilterFactory extends LockssTestCase {
  
  FilterFactory variantFact;
  ArchivalUnit mau;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;
        
  private static final String PLUGIN_ID = 
      "org.lockss.plugin.atypon.asco.ClockssAscoJournalsPlugin";
  
  private static final String filteredCrawlStr = 
      "<div class=\"block\"></div>";
  private static final String filteredStr = 
      " <div class=\"block\"> </div>";
  
  private static final String abstractContent = 
      "<div class=\"hlFld-Abstract\">" +
          "<p class=\"fulltext\">" +
          "<a target=\"_blank\" href=\"/doi/abs/10.1200/jco.2012.44.2806\"></a>" +
          "<a href=\"/test/link/doi/here\"/>" +
          "</p>" +
          "</div>";
  private static final String abstractContentFiltered = 
      "<div class=\"hlFld-Abstract\">" +
          "<p class=\"fulltext\">" +
          "<a href=\"/test/link/doi/here\"/>" +
          "</p>" +
          "</div>";  

  private static final String fullTextContent = 
      "<div class=\"hlFld-Fulltext\">" +
      "<p>" +
      "<i>Journal of Clinical Oncology</i> (<i>JCO</i>) attracts some stuff." +
      "<ol class=\"NLM_list-list_type-order\">" +
      "<li>" +
      "<p class=\"inline\">" +
      "<li>Original article: <a target=\"_blank\" href=\"/doi/abs/10.1200/jco.2012.44.2806\">" +
      "</a>" +
      "</li>" +
      "</p>" +
      "</li>" +
      "</ol>" +
      "<a href=\"/test/link/doi/here\"/>" +
      "</p>" +
      "</div>";
  private static final String fullTextContentFiltered = 
      "<div class=\"hlFld-Fulltext\">" +
      "<p>" +
      "<i>Journal of Clinical Oncology</i> (<i>JCO</i>) attracts some stuff." +
      "<ol class=\"NLM_list-list_type-order\">" +
      "<li>" +
      "<p class=\"inline\">" +
      "<li>Original article: " +
      "</li>" +
      "</p>" +
      "</li>" +
      "</ol>" +
      "<a href=\"/test/link/doi/here\"/>" +
      "</p>" +
      "</div>";

    
  protected ArchivalUnit createAu()
      throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_ID, ascoAuConfig());
  }
  
  private Configuration ascoAuConfig() {
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
    mau = createAu();
  }
  
  // Variant to test with Crawl Filter
  public static class TestCrawl extends TestAscoHtmlFilterFactory {
    public void testFiltering() throws Exception {
      variantFact = new AscoHtmlCrawlFilterFactory();
      doFilterTest(mau, variantFact, fullTextContent, fullTextContentFiltered); 
      doFilterTest(mau, variantFact, abstractContent, abstractContentFiltered); 
          
    }    
  }

  // Variant to test with Hash Filter
   public static class TestHash extends TestAscoHtmlFilterFactory {   
     public void testFiltering() throws Exception {
      variantFact = new AscoHtmlHashFilterFactory();
 
     }
  }
  
  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
      });
  }
  
}

