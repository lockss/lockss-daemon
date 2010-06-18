/*
 * $Id: TestSpringerMetadataExtractorFactory.java,v 1.3 2010-06-18 21:15:31 thib_gc Exp $
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.plugin.springer;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.crawler.*;
import org.lockss.repository.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.plugin.simulated.*;
import org.lockss.plugin.springer.*;
import org.lockss.extractor.*;

public class TestSpringerMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger("TestSpringerMetadataExtractorFactory");

  private SimulatedArchivalUnit simau;	// Simulated AU to generate content
  private ArchivalUnit spau;		// Springer AU
  private MockLockssDaemon theDaemon;
  private static final int DEFAULT_FILESIZE = 3000;
  private static int fileSize = DEFAULT_FILESIZE;

  private static String PLUGIN_NAME =
    "org.lockss.plugin.springer.ClockssSpringerExplodedPlugin";

  private static String BASE_URL =
    "http://source.lockss.org/sourcefiles/springer-released/";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    ConfigurationUtil.setFromArgs(LockssRepositoryImpl.PARAM_CACHE_LOCATION,
				  tempDirPath,
				  "org.lockss.plugin.simulated.SimulatedContentGenerator.doSpringer",
				  "true");

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    PluginManager pluginMgr = theDaemon.getPluginManager();
    pluginMgr.setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    pluginMgr.startService();
    theDaemon.getCrawlManager();

    simau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
    spau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, springerAuConfig());
  }

  public void tearDown() throws Exception {
    simau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", BASE_URL);
    conf.put("depth", "1");
    conf.put("branch", "3");
    conf.put("numFiles", "7");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF +
				SimulatedContentGenerator.FILE_TYPE_XML));
    return conf;
  }

  Configuration springerAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("year", "2009");
    return conf;
  }

  public void testDOI() throws Exception {
    PluginTestUtil.crawlSimAu(simau);
    PluginTestUtil.copyAu(simau, spau);

    Plugin plugin = spau.getPlugin();
    String articleMimeType = "application/pdf";
    ArticleMetadataExtractor me =
      plugin.getArticleMetadataExtractor(null, spau);
    assertNotNull(me);
    assertTrue(""+me.getClass(),
	       me instanceof SpringerArticleIteratorFactory.SpringerArticleMetadataExtractor);
    int count = 0;
    for (Iterator<ArticleFiles> it = spau.getArticleIterator(); it.hasNext();){
      ArticleFiles af = it.next();
      assertNotNull(af);
      CachedUrl fcu = af.getFullTextCu();
      assertNotNull("full text CU", fcu);
      String contentType = fcu.getContentType();
      assertNotNull(contentType);
      assertTrue(contentType,
		 contentType.toLowerCase().startsWith(articleMimeType));
      log.debug("count " + count + " url " + fcu.getUrl() + " " + contentType);
      count++;
      ArticleMetadata md = me.extract(af);
      assertNotNull(md);
      String doi = md.getDOI();
      assertNotNull(doi);
      log.debug(fcu.getUrl() + " doi " + doi);
      String doi2 = md.getProperty(ArticleMetadata.KEY_DOI);
      assertTrue(doi2, doi2.startsWith(ArticleMetadata.PROTOCOL_DOI));
      assertEquals(doi, doi2.substring(ArticleMetadata.PROTOCOL_DOI.length()));
    }
    log.debug("Article count is " + count);
    assertEquals(28, count);
  }
}
