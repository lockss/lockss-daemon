/*
 * $Id: TestNatureArticleIteratorFactory.java,v 1.1 2010-06-17 18:41:27 tlipkis Exp $
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

package org.lockss.plugin.nature;

import java.io.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.repository.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

public class TestNatureArticleIteratorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger("TestNatureArticleIteratorFactory");

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  private ArchivalUnit nau;		// Nature AU
  private MockLockssDaemon theDaemon;
  private static final int DEFAULT_FILESIZE = 3000;
  private static int fileSize = DEFAULT_FILESIZE;

  private static String PLUGIN_NAME =
    "org.lockss.plugin.nature.ClockssNaturePublishingGroupPlugin";

  private static String BASE_URL = "http://www.nature.com/";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    ConfigurationUtil.setFromArgs(LockssRepositoryImpl.PARAM_CACHE_LOCATION,
				  tempDirPath);
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
    nau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, natureAuConfig());
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
    conf.put("depth", "2");
    conf.put("branch", "2");
    conf.put("numFiles", "7");
    conf.put("fileTypes", "" + SimulatedContentGenerator.FILE_TYPE_HTML);
    conf.put("binFileSize", ""+fileSize);
    return conf;
  }

  Configuration natureAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_id", "aps");
    conf.put("volume_name", "2");
    conf.put("year", "2008");
    return conf;
  }

  public void testArticleCountAndType(String articleMimeType,
				      boolean isDefaultTarget,
				      int expCount)
      throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    String pat = "branch(\\d+)/branch(\\d+)/(\\d+file\\.html)";
    String rep = "aps/journal/v$1/n$2/full/$3";
    PluginTestUtil.copyAu(sau, nau, null, pat, rep);

    Iterator<ArticleFiles> it =
      isDefaultTarget
      ? nau.getArticleIterator()
      : nau.getArticleIterator(new MetadataTarget().setFormat(articleMimeType));
    int count = 0;
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      CachedUrl cu = af.getFullTextCu();
      assertNotNull(cu);
      String contentType = cu.getContentType();
      assertTrue(contentType,
		 contentType.toLowerCase().startsWith(articleMimeType));
      log.debug("count " + count + " url " + cu.getUrl() + " " + contentType);
      count++;
    }
    log.debug("Article count is " + count);
    assertEquals(expCount, count);
  }

  public void testArticleCountAndDefaultType() throws Exception {
    testArticleCountAndType("text/html", true, 14);
  }

  public void testArticleCountAndPdf() throws Exception {
    testArticleCountAndType("application/pdf", false, 0);
  }

}
