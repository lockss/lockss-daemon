/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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
    String tempDirPath = setUpDiskSpace();
    ConfigurationUtil.addFromArgs("org.lockss.plugin.simulated.SimulatedContentGenerator.doSpringer",
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
      plugin.getArticleMetadataExtractor(MetadataTarget.Any, spau);
    assertNotNull(me);

    ArticleMetadataListExtractor mle =
      new ArticleMetadataListExtractor(me);
    int count = 0;
    Set foundDoiSet = new HashSet();
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
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, af);
      assertNotEmpty(mdlist);
      ArticleMetadata md = mdlist.get(0);
      assertNotNull(md);
      String doi = md.get(MetadataField.FIELD_DOI);
      log.debug(fcu.getUrl() + " doi " + doi);
      assertTrue(MetadataUtil.isDoi(doi));
      foundDoiSet.add(doi);
    }
    log.debug("Article count is " + count);
    assertEquals(28, count);
    assertEquals(SetUtil.set("10.0001/1-3", "10.0004/1-2", "10.0004/1-3",
			     "10.0001/1-2", "10.0004/1-1", "10.0001/1-1",
			     "10.0001/0-0", "10.0006/1-3", "10.0006/1-2",
			     "10.0003/1-3", "10.0003/1-2", "10.0003/1-1",
			     "10.0006/1-1", "10.0003/0-0", "10.0005/1-3",
			     "10.0007/0-0", "10.0002/0-0", "10.0005/1-2",
			     "10.0005/1-1", "10.0005/0-0", "10.0007/1-1",
			     "10.0007/1-2", "10.0007/1-3", "10.0006/0-0",
			     "10.0002/1-1", "10.0002/1-3", "10.0002/1-2",
			     "10.0004/0-0"),
		 foundDoiSet);
  }
}
