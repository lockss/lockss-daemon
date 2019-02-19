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
  
  
  private static final String manifestContent = 
		  "<!DOCTYPE html>" +
				  "<html>" +
				  "<head>" +
				  "    <title>Crime &amp; Delinquency 2017 CLOCKSS Manifest Page</title>" +
				  "    <meta charset=\"UTF-8\" />" +
				  "</head>" +
				  "<body>" +
				  "<h1>Crime &amp; Delinquency 2017 CLOCKSS Manifest Page</h1>" +
				  "<ul>" +
				  "    <li><a href=\"/toc/cadc/63/14\">December 2017 (Vol. 63 Issue 14 Page 1807-1967)</a></li>" +
				  "    " +
				  "    <li><a href=\"/toc/cadc/63/13\">December 2017 (Vol. 63 Issue 13 Page 1655-1803)</a></li>" +
				  "</ul>" +
				  "<p>" +
				  "    <img src=\"http://www.lockss.org/images/LOCKSS-small.gif\" height=\"108\" width=\"108\" alt=\"LOCKSS logo\"/>" +
				  "    CLOCKSS system has permission to ingest, preserve, and serve this Archival Unit." +
				  "</p>" +
				  "</body>" +
				  "</html>";

  
  // sage removes tags 
  private static final String manifestHashFiltered = 
				  " December 2017 (Vol. 63 Issue 14 Page 1807-1967) " +
				  "December 2017 (Vol. 63 Issue 13 Page 1655-1803) ";
  private static final String tocContent = 
		  "<div> " + 
		  "<span class=\"TocHeading\"><legend class=\"tocListTitle\"><h1>Table of Contents</h1></legend> " + 
		  "</span><span><h2 class=\" currentIssue\"> </h2> </span> " + 
	      "</div> " +
	      "<h3 class=\"tocListHeader\"> Volume 63, Issue 8, July 2017 </h3> " + 
		  "<div class=\"tocListtDropZone2\" data-pb-dropzone=\"tocListtDropZone2\"> </div> " + 
		  "<div class=\"tocContent\"> " + 
		  "<!--totalCount6--><!--modified:1525656417000-->" +
		  "<h2 class=\"tocHeading\">" +
		  "<a class=\"header-aware-toc-section-anchor\" name=\"sage_toc_section_Articles\"></a>" +
		  "<div class=\"subject heading-1\">Articles</div>" +
		  "</h2>" +
		  "<table border=\"0\" width=\"100%\" class=\"articleEntry\">" +
		  "<tr>" +
		  "<td valign=\"top\">" +
		  "<span class=\"ArticleType\">Articles</span>" +
		  "<div class=\"art_title linkable\">" +
		  "<a data-item-name=\"click-article-title\" class=\"ref nowrap\" href=\"/doi/full/10.1177/0011128715574977\">" +
		  "<h3 class=\"heading-title\">" +
		  "<span class=\"hlFld-Title\">Assault of Police</span>" +
		  "</h3>" +
		  "</a>" +
		  "</div>" +
		  "</td>" +
		  "</tr>" +
		  "</table></div>";

  private static final String tocContentFiltered = 
		  " Articles Assault of Police ";
  
  private static final String articleAccessDenialContent =
    "<div class=\"accessDenialDropZone1\" data-pb-dropzone=\"accessDenialDropZone1\">" +
    "Hello World" +
    "</div>" +
    "<div class=\"accessDenialDropZone2\" data-pb-dropzone=\"accessDenialDropZone2\">" +
    "Hello Kitty" +
    "</div>" +
    "<div id=\"accessDenialWidget\">" +
    "Hello Sailor" +
    "</div>" ;

  private static final String articleAccessDenialContentFiltered = "" ;
 
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
       doFilterTest(mau, variantFact, manifestContent, manifestHashFiltered);
       doFilterTest(mau, variantFact, tocContent, tocContentFiltered);       
       doFilterTest(mau, variantFact, articleAccessDenialContent, articleAccessDenialContentFiltered);       
     }
   }
   
  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
      });
  }
  
}

