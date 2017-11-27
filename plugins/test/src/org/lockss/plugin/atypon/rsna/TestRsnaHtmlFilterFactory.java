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

package org.lockss.plugin.atypon.rsna;

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

public class TestRsnaHtmlFilterFactory extends LockssTestCase {
  
  
  FilterFactory variantFact;
  ArchivalUnit mau;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;
        
  private static final String PLUGIN_ID = 
      "org.lockss.plugin.atypon.rsna.RsnaJournalsPlugin";
  
  
  private static final String artLinkContent = 
      "<article class=\"article\">" +
          "<p class=\"fulltext\"></p>" +
          "<p>" +
          "<b>Originally published in:</b>" +
          "</p>" +
          "<p  xmlns:oasis=\"http://www.niso.org/\">Radiology 2016;279(3):" +
          "<a class=\"ext-link\" href=\"http://pubs.rsna.org/doi/abs/10.1148/radiol.2015151256\" target=\"_blank\">" +
          "827–837</a> DOI:10.1148/radiol.2015151256</p>" +
          "<b>Erratum in:</b>" +
          "<p>Radiology 2016;280(1):328 DOI:10.1148/radiol.2016164016</p>" +
          "<!-- /fulltext content -->" +
          "        </article>";      


  private static final String artLinkContentFiltered = 
      "<article class=\"article\">" +
          "<p class=\"fulltext\"></p>" +
          "<p>" +
          "<b>Originally published in:</b>" +
          "</p>" +
          "<p  xmlns:oasis=\"http://www.niso.org/\">Radiology 2016;279(3):" +
          " DOI:10.1148/radiol.2015151256</p>" +
          "<b>Erratum in:</b>" +
          "<p>Radiology 2016;280(1):328 DOI:10.1148/radiol.2016164016</p>" +
          "<!-- /fulltext content -->" +
          "        </article>";      


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
  public static class TestCrawl extends TestRsnaHtmlFilterFactory {
    public void testFiltering() throws Exception {
      variantFact = new RsnaHtmlCrawlFilterFactory();
      doFilterTest(mau, variantFact, artLinkContent, artLinkContentFiltered); 
          
    }    
  }

  // Variant to test with Hash Filter
   public static class TestHash extends TestRsnaHtmlFilterFactory {   
     public void testFiltering() throws Exception {
      variantFact = new RsnaHtmlHashFilterFactory();
 
     }
  }
  
  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
      });
  }
  
}

