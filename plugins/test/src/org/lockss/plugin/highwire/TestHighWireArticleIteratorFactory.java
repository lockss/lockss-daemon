/*
 * $Id: TestHighWireArticleIteratorFactory.java,v 1.5 2010-06-17 18:41:27 tlipkis Exp $
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

package org.lockss.plugin.highwire;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.repository.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

public class TestHighWireArticleIteratorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger("TestHighWireArticleIteratorFactory");

  private static final int DEFAULT_FILESIZE = 3000;

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  private ArchivalUnit hau;		// HighWire AU
  private MockLockssDaemon theDaemon;
  private static int fileSize = DEFAULT_FILESIZE;

  private static String PLUGIN_NAME =
    "org.lockss.plugin.highwire.HighWireStrVolPlugin";

  private static String BASE_URL = "http://www.jhc.org/";
  private static String SIM_ROOT = BASE_URL + "cgi/reprint/";

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
    hau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, highWireAuConfig());
  }

  public void tearDown() throws Exception {
    sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", SIM_ROOT);
    conf.put("depth", "2");
    conf.put("branch", "2");
    conf.put("numFiles", "4");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF +
				SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("binFileSize", "7");
    return conf;
  }

  Configuration highWireAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("volume_name", "52");
    return conf;
  }

  public void testArticleCountAndType(String articleMimeType,
				      boolean isDefaultTarget,
				      int expCount)
      throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    PluginTestUtil.copyAu(sau, hau);
    PluginTestUtil.copyAu(sau, hau, ".*\\.html",
			  "cgi/reprint/", "cgi/reprintframed/");

    Plugin plugin = hau.getPlugin();
    Iterator<ArticleFiles> it =
      isDefaultTarget
      ? hau.getArticleIterator()
      : hau.getArticleIterator(new MetadataTarget().setFormat(articleMimeType));
    int count = 0;
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      assertNotNull(af);
      CachedUrl cu = af.getFullTextCu();
      assertNotNull(cu);
      String contentType = cu.getContentType();
      assertTrue(contentType,
		 contentType.toLowerCase().startsWith(articleMimeType));
      CachedUrl reprintFramedCu = af.getRoleCu("reprintFramed");
      if (isDefaultTarget) {
	assertNotNull("reprintFramed role is null", reprintFramedCu);
	String reprintFramedUrl = reprintFramedCu.getUrl();
	assertEquals("html", FileUtil.getExtension(reprintFramedUrl));
	assertEquals(reprintFramedUrl, cu.getUrl().replace("/reprint/",
							   "/reprintframed/"));
      } else {	
	assertNull("reprintFramed role is not null", reprintFramedCu);
      }
      log.debug("count " + count + " url " + cu.getUrl() + " " + contentType);
      count++;
    }
    log.debug("Article count is " + count);
    assertEquals(expCount, count);
  }

  public void testArticleCountAndDefaultType() throws Exception {
    testArticleCountAndType("text/html", true, 35);
  }

  public void testArticleCountAndPdfType() throws Exception {
    testArticleCountAndType("application/pdf", false, 28);
  }
}
