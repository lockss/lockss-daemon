/*
 * $Id: TestElsevierMetadataExtractor.java,v 1.1 2010-06-17 18:41:27 tlipkis Exp $
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

package org.lockss.plugin.elsevier;

import java.io.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.repository.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;
import org.lockss.extractor.*;

public class TestElsevierMetadataExtractor extends LockssTestCase {
  static Logger log = Logger.getLogger("TestElsevierMetadataExtractor");

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  private ArchivalUnit eau;		// Elsevier AU
  private MockLockssDaemon theDaemon;
  private PluginManager pluginMgr;
  private static final int DEFAULT_FILESIZE = 3000;
  private static int fileSize = DEFAULT_FILESIZE;

  private static String PLUGIN_NAME =
    "org.lockss.plugin.elsevier.ClockssElsevierExplodedPlugin";

  private static String BASE_URL =
    "http://source.lockss.org/sourcefiles/elsevier-released/";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    ConfigurationUtil.setFromArgs(LockssRepositoryImpl.PARAM_CACHE_LOCATION,
				  tempDirPath);

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    pluginMgr = theDaemon.getPluginManager();
    pluginMgr.setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    pluginMgr.startService();
    theDaemon.getCrawlManager();

    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
    eau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, elsevierAuConfig());
  }

  public void tearDown() throws Exception {
    sau.deleteContentTree();
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
//     conf.put("default_article_mime_type", "application/pdf");
    return conf;
  }

  Configuration elsevierAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("year", "2009");
    return conf;
  }

  public void testDOI() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    PluginTestUtil.copyAu(sau, eau);

    Plugin plugin = eau.getPlugin();
    String articleMimeType = "application/pdf";
    ArticleMetadataExtractor me = plugin.getArticleMetadataExtractor(null, eau);
    assertTrue(""+me,
	       me instanceof ElsevierArticleIteratorFactory.ElsevierArticleMetadataExtractor);
    int count = 0;
    for (Iterator<ArticleFiles> it = eau.getArticleIterator(); it.hasNext(); ) {
      ArticleFiles af = it.next();
      assertNotNull(af);
      CachedUrl fcu = af.getFullTextCu();
      assertNotNull("full text CU", fcu);
      String contentType = fcu.getContentType();
      log.debug("count " + count + " url " + fcu.getUrl() + " " + contentType);
      assertTrue(contentType.toLowerCase().startsWith(articleMimeType));
      CachedUrl xcu = af.getRoleCu("xml");
      assertNotNull("role CU (xml)", xcu);
      contentType = xcu.getContentType();
      assertTrue("XML cu is " + contentType + " (" + xcu + ")",
		 contentType.toLowerCase().startsWith("text/xml"));
      count++;
      Metadata md = me.extract(af);
      assertNotNull(md);
      String doi = md.getDOI();
      assertNotNull(doi);
      log.debug(fcu.getUrl() + " doi " + doi);
      String doi2 = md.getProperty(Metadata.KEY_DOI);
      assertTrue(doi2.startsWith(Metadata.PROTOCOL_DOI));
      assertEquals(doi, doi2.substring(Metadata.PROTOCOL_DOI.length()));
    }
    log.debug("Article count is " + count);
    assertEquals(28, count);
  }

}
