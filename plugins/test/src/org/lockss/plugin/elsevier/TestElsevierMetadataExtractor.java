/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.elsevier;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;
import org.lockss.extractor.*;

public class TestElsevierMetadataExtractor extends LockssTestCase {
  static Logger log = Logger.getLogger("TestElsevierMetadataExtractor");

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  private ArchivalUnit eau;		// Elsevier AU
  private MockLockssDaemon theDaemon;
  private PluginManager pluginMgr;
  
  private static String PLUGIN_NAME =
    "org.lockss.plugin.elsevier.ClockssElsevierExplodedPlugin";

  private static String BASE_URL =
    "http://source.lockss.org/sourcefiles/elsevier-released/";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

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
    ArticleMetadataExtractor me =
      plugin.getArticleMetadataExtractor(MetadataTarget.Any(), eau);
    ArticleMetadataListExtractor mle =
      new ArticleMetadataListExtractor(me);
    int count = 0;
    Set<String> foundDoiSet = new HashSet<String>();
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
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), af);
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
