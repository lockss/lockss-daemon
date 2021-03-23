/*
 * $Id$
 */

/*

Copyright (c) 2019 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.sage;

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
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;
import org.lockss.test.*;

public class TestSageHtmlFilterFactory extends LockssTestCase {
  
  FilterFactory variantFact;
  ArchivalUnit mau;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;
  
  private static final String PLUGIN_ID = 
      "org.lockss.plugin.atypon.sage.ClockssSageAtyponJournalsPlugin";
  

  private static final String originalContent =
          "<!DOCTYPE html>" +
                  "<html>" +
                  "<head> head content</head>" +
                  "<body>" +
                  "<h1>h1 content</h1>" +
                  "<header>header section</header>" +
                  "<div class=\"issueSerialNavigation\">issueSerialNavigation content</div>" +
                  "<div class=\"articleJournalButtons\">articleJournalButtons content</div>" +
                  "<div class=\"toc-quick-links\">toc-quick-links content</div>" +
                  "<div class=\"journalNavInnerContainer\">journalNavInnerContainer content</div>" +
                  "<div class=\"journalHeaderOuterContainer\">journalHeaderOuterContainer content</div>" +
                  "<div class=\"issueBookNavPager\">issueBookNavPager content</div>" +
                  "<div class=\"abstractKeywords\">abstractKeywords content</div>" +
                  "<div class=\"journalHomeAlerts\">journalHomeAlerts content</div>" +
                  "<div class=\"totoplink\">totoplink content</div>" +
                  "<div id=\"journalSearchButton\">journalSearchButton content</div>" +
                  "<div class=\"doNotShow\">doNotShow content</div>" +
                  "<div id=\"article-tools\">article-tools content</div>" +
                  "<div class=\"publication-tabs-header\">publication-tabs-header content</div>" +
                  "<div class=\"articleAccessOptionsContainer\">articleAccessOptionsContainer content</div>" +
                  "<div class=\"eCommercePurchaseAccessWidgetContainer\">eCommercePurchaseAccessWidgetContainer content</div>" +
                  "<div class=\"offersList\">offersList content</div>" +
                  "<div id=\"relatedArticlesColumn\">relatedArticlesColumn content</div>" +
                  "<div class=\"citingArticles\">citingArticles content</div>" +
                  "<div class=\"articleTabContainer\">articleTabContainer content</div>" +
                  "<div class=\"publicationContentAuthors\">publicationContentAuthors content</div>" +
                  "<div class=\"purchaseArea\">purchaseArea content</div>" +
                  "<div class=\"tocAuthors\">tocAuthors content</div>" +
                  "<div class=\"showAbstract\">showAbstract content</div>" +
                  "<article>article content</article>" +
                  "<section>section content</section>" +
                  "<div class=\"tocDeliverFormatsLinks\">tocDeliverFormatsLinks content</div>" +
                  "<footer> footer content</footer>" +
                  "</body>" +
                  "</html>";

    private static final String originalContentFiltered =
		  " <!DOCTYPE html> <html> <body> <h1>h1 content </h1> <article>article content </article> <section>section content </section> </body> </html>";


    
    
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
  
  private static void doFilterTest(ArchivalUnit au, FilterFactory fact,
      String nameToHash, String expectedStr) 
          throws PluginException, IOException {
    InputStream actIn; 
    actIn = fact.createFilteredInputStream(au, 
        new StringInputStream(nameToHash), Constants.ENCODING_UTF_8);
    String filterdStr = StringUtil.fromInputStream(actIn);

    log.info("filteredStr = " + filterdStr);
    log.info("expectedStr = " + expectedStr);

    assertTrue(filterdStr.equals(expectedStr));
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
  public static class TestCrawl extends TestSageHtmlFilterFactory {
    public void testFiltering() throws Exception {
      variantFact = new BaseAtyponHtmlCrawlFilterFactory();
      assertEquals(true,true);
   //TODO
    }
  }
  
  // Variant to test with Hash Filter
   public static class TestHash extends TestSageHtmlFilterFactory {
     public void testFiltering() throws Exception {
       variantFact = new SageAtyponHtmlHashFilterFactory();
       doFilterTest(mau, variantFact, originalContent, originalContentFiltered);
     }
   }
   
  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
      });
  }
  
}

