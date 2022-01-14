package org.lockss.plugin.interresearch;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.Logger;

import java.io.IOException;
import java.util.Iterator;

public class TestInterResearchArticleIterator extends LockssTestCase {
  private static final Logger log = Logger.getLogger("TestInterResearchArticleIterator.class");

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  private ArchivalUnit nau;		// Nature AU
  private MockLockssDaemon theDaemon;
  private static final int DEFAULT_FILESIZE = 3000;
  private static int fileSize = DEFAULT_FILESIZE;

  private static String PLUGIN_NAME =
      "org.lockss.plugin.interresearch.ClockssInterResearchPlugin";

  private static String BASE_URL = "https://www.int-res.com/";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
    nau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, AuConfig());

    ConfigurationUtil.addFromArgs(CachedUrl.PARAM_ALLOW_DELETE, "true");
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
    conf.put("branch", "4");
    conf.put("numFiles", "7");
    conf.put("fileTypes",
        "" + (  SimulatedContentGenerator.FILE_TYPE_HTML
            | SimulatedContentGenerator.FILE_TYPE_PDF));
    conf.put("binFileSize", ""+fileSize);
    return conf;
  }

  Configuration AuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_id", "meps");
    conf.put("num_issue_range", "143-147");
    conf.put("year", "2008");
    return conf;
  }

  public void testArticleCountAndType() throws Exception {
    log.setLevel("debug3");
    PluginTestUtil.crawlSimAu(sau);
    // https://www.int-res.com/abstracts/dao/v143/p205-226/
    String pat1 = "branch(\\d+)/(\\d+)file\\.html";
    String rep1 = "abstracts/meps/v$1/p$2-001.html";
    PluginTestUtil.copyAu(sau, nau, ".*\\.html$", pat1, rep1);
    // https://www.int-res.com/articles/feature/d143p205.pdf
    // https://www.int-res.com/articles/feature/d143p205.pdf
    String pat2 = "branch(\\d+)/(\\d+)file\\.pdf";
    String rep2 = "articles/feature/m$1p$2.pdf";
    PluginTestUtil.copyAu(sau, nau, ".*\\.pdf$", pat2, rep2);

    // Remove some URLs
    int deleted = 0;
    for (CachedUrl cu : AuUtil.getCuIterable(nau)) {
      String url = cu.getUrl();
      log.info("url: " + url);
      if (url.endsWith("v2/p001-001") || url.endsWith("m2p001.pdf")) {
        deleteBlock(cu);
        ++deleted;
      }
    }
    log.info("DELETED = " + deleted);
    //assertEquals(1, deleted);

    Iterator<ArticleFiles> it = nau.getArticleIterator();
    int count = 0;
    int countHtmlOnly = 0;
    int countPdfOnly = 0;
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      log.info(af.toString());
      CachedUrl ftcu = af.getFullTextCu();
      String fturl = ftcu.getUrl();
      assertNotNull(ftcu);
      String contentType = ftcu.getContentType();
      log.debug3("count " + count + " FullTextCU " + fturl + " content type: " + contentType);
      count++;
      // The full text CU will be the PDF unless one doesn't exist
      if (af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF) == null) {
        ++countHtmlOnly;
        assertEquals(af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_HTML), fturl);
      }
      if (af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF) == fturl) {
        ++countPdfOnly;
      }
    }
    log.debug3("Article count is " + count);
    //assertEquals(28, count);
    //assertEquals(4, countHtmlOnly);
    //assertEquals(24, countPdfOnly);
  }

  private void deleteBlock(CachedUrl cu) throws IOException {
    log.info("deleting " + cu.getUrl());
    cu.delete();
  }
}
