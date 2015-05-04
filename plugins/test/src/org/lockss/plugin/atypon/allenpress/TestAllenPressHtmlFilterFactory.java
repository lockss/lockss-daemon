/*
 * $Id:$
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

package org.lockss.plugin.atypon.allenpress;

import java.io.*;

import junit.framework.Test;

import org.htmlparser.NodeFilter;
import org.lockss.util.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.test.*;

public class TestAllenPressHtmlFilterFactory
  extends LockssTestCase {
  
  FilterFactory variantFact;
  ArchivalUnit au;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;
        
  private static final String PLUGIN_NAME = 
      "org.lockss.plugin.atypon.allenpress.AllenPressJournalsPlugin";
  
 
  
  protected ArchivalUnit createAu()
      throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_NAME,  AuConfig());
  }
  
  private Configuration AuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", "http://www.jgme.org/");
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
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    daemon = getMockLockssDaemon();
    pluginMgr = daemon.getPluginManager();
    au = createAu();
  }
  
  private static final String filteredStr = 
      "<div class=\"block\"></div>";
  
  private static final String referencesStr =
      "<div class=\"block\">" +
      "<table border=\"0\" class=\"references\"><tr><td class=\"refnumber\" id=\"i1949-8357-5-4-556-Hauer1\">1.</td><td valign=\"top\">" +
          "Authors,  <span class=\"NLM_etal\">et al</span>. " +
          "<span class=\"NLM_article-title\">Title</span>. " +
          "<span class=\"citation_source-journal\">JAMA</span>. " +
          "<span class=\"NLM_year\">2008</span>;300(10):" +
          "<span class=\"NLM_fpage\">xxx</span>–<span class=\"NLM_lpage\">1164</span>. " +
          "<script type=\"text/javascript\">genRefLink(16, 'i1949-8357-5-4-556-xxx1', '10.1001%2Fjama.300.10.xxx');" +
          "</script> </td></tr>" +
          "</table>" +
          "</div>";
  
  private static final String leftColumnStr =
      "<div class=\"block\">" +
          "<div id=\"leftColumn\">" +
          "<div class=\"panel_228\" id=\"journalNavPanel\">" +
          "    <div class=\"panelHeading\">" +
          "       <div class=\"left\"></div>" +
          "       <div class=\"content\">" +
          "                   <h3>Volume 5, Issue 4 <br/>(December 2013)</h3>" +
          "       </div>" +
          "       <div class=\"right\"></div>" +
          "    </div>" +
          "</div>" +
          "</div>" +
          "</div>";
  
  private static final String articleToolsStr =
      "<div class=\"block\">" +
          "<div class=\"article_tools\">" +
          "<table id=\"Table9\"><tr><td>foo</td></tr></table>" +
          "<ul id=\"articleToolsFormats\">" +
          "        <li>" +
          "            <a href=\"/doi/abs/10.4300/1949-8357-6.1.192\">" +
          "                        Citation" +
          "            </a>" +
          "        </li>" +
          "        <li>" +
          "           <a target=\"_blank\" href=\"/doi/pdf/10.4300/1949-8357-6.1.192\">" +
          "                PDF" +
          "           </a>" +
          "       </li>" +
          "        <li>" +
          "           <a class=\"errata\" href=\"/doi/full/10.4300/jgme-d-11-00269.1\">" +
          "                Original&nbsp;" +
          "           </a>" +
          "        </li>" +
          "</ul>" +
          "</div>" +
          "</div>";
      
  private static final String crawlArticleToolsFiltered =
      "<div class=\"block\">" +
          "<div class=\"article_tools\">" +
          "<table id=\"Table9\"><tr><td>foo</td></tr></table>" +
          "<ul id=\"articleToolsFormats\">" +
          "        <li>" +
          "            <a href=\"/doi/abs/10.4300/1949-8357-6.1.192\">" +
          "                        Citation" +
          "            </a>" +
          "        </li>" +
          "        <li>" +
          "           <a target=\"_blank\" href=\"/doi/pdf/10.4300/1949-8357-6.1.192\">" +
          "                PDF" +
          "           </a>" +
          "       </li>" +
          "        <li>" +
          "           " +
          "        </li>" +
          "</ul>" +
          "</div>" +
          "</div>";
          

  
  // Variant to test with Crawl Filter
  //HtmlNodeFilters.tagWithAttribute("div", "id", "leftColumn"),
  //HtmlNodeFilters.tagWithAttribute("a", "class", "errata"),
  //HtmlNodeFilters.tagWithAttribute("table",  "class", "references"),
  public static class TestCrawl extends TestAllenPressHtmlFilterFactory {
    public void testFiltering() throws Exception {
      variantFact = new AllenPressCrawlFilterFactory();
      doFilterTest(au, variantFact, referencesStr, 
          filteredStr);
      doFilterTest(au, variantFact, leftColumnStr, 
          filteredStr);
      doFilterTest(au, variantFact, articleToolsStr, 
          crawlArticleToolsFiltered);
    }    
  }

  // Variant to test with Hash Filter
  //HtmlNodeFilters.tagWithAttribute("div", "id", "leftColumn"),
  //HtmlNodeFilters.tagWithAttribute("div", "class", "article_tools"),
   public static class TestHash extends TestAllenPressHtmlFilterFactory {   
     public void testFiltering() throws Exception {
      variantFact = new AllenPressHtmlHashFilterFactory();
      doFilterTest(au, variantFact, leftColumnStr, filteredStr);
      doFilterTest(au, variantFact, articleToolsStr, filteredStr);
    }
  }
  
  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
      });
  }
  
}

