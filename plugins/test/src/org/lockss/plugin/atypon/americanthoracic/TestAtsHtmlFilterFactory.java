/*
 * $Id$
 */

/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.americanthoracic;

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

public class TestAtsHtmlFilterFactory extends LockssTestCase {
  
  FilterFactory variantFact;
  ArchivalUnit mau;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;
        
  private static final String PLUGIN_ID = 
      "org.lockss.plugin.atypon.americanthoracic.AtsJournalsPlugin";
  
  
  private static final String tocLinkContent = 
      "<div class=\"tocDeliverFormatsLinks\">" +                                                                                                                            
          "<td align=\"right\" valign=\"top\" width=\"18\" class=\"nowrap\">" +                                                                                                 
          "</td>" +                                                                                                                                                             
          "<a class=\"ref nowrap abs\" href=\"/doi/abs/10.1164/rccm.201510-1925ED\">First Page</a> | " +                                                                        
          "<a class=\"ref nowrap full\" href=\"/doi/full/10.1164/rccm.201510-1925ED\">Full Text</a> | " +                                                                       
          "<a class=\"ref nowrap pdf\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.1164/rccm.201510-1925ED\">PDF (444 KB)</a> | " +                         
          "<a href=\"/doi/abs/10.1164/rccm.201504-0760OC\"> related article</a> <div id=\"Absrccm2015101925ED\" class=\"previewViewSection tocPreview\">" +                     
          "<div class=\"closeButton\" onclick=\"showHideTocPublicationAbs('10.1164/rccm.201510-1925ED', 'Absrccm2015101925ED');\">" +                                           
          "</div>";

  private static final String tocLinkContentFiltered = 
      "<div class=\"tocDeliverFormatsLinks\">" +                                                                                                                            
          "<td align=\"right\" valign=\"top\" width=\"18\" class=\"nowrap\">" +                                                                                                 
          "</td>" +                                                                                                                                                             
          "<a class=\"ref nowrap abs\" href=\"/doi/abs/10.1164/rccm.201510-1925ED\">First Page</a> | " +                                                                        
          "<a class=\"ref nowrap full\" href=\"/doi/full/10.1164/rccm.201510-1925ED\">Full Text</a> | " +                                                                       
          "<a class=\"ref nowrap pdf\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.1164/rccm.201510-1925ED\">PDF (444 KB)</a> | " +                         
          " <div id=\"Absrccm2015101925ED\" class=\"previewViewSection tocPreview\">" +                     
          "<div class=\"closeButton\" onclick=\"showHideTocPublicationAbs('10.1164/rccm.201510-1925ED', 'Absrccm2015101925ED');\">" +                                           
          "</div>";  


  protected ArchivalUnit createAu()
      throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_ID, thisAuConfig());
  }
  
  private Configuration thisAuConfig() {
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
  public static class TestCrawl extends TestAtsHtmlFilterFactory {
    public void testFiltering() throws Exception {
      variantFact = new AtsHtmlCrawlFilterFactory();
      doFilterTest(mau, variantFact, tocLinkContent, tocLinkContentFiltered); 
          
    }    
  }

  // Variant to test with Hash Filter
   public static class TestHash extends TestAtsHtmlFilterFactory {   
     public void testFiltering() throws Exception {
      variantFact = new AtsHtmlHashFilterFactory();
 
     }
  }
  
  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
      });
  }
  
}

