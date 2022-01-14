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
    conf.put("branch", "7");
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
    conf.put("year", "2021");
    return conf;
  }

  public void testArticleCountAndType() throws Exception {
    log.setLevel("debug3");
    PluginTestUtil.crawlSimAu(sau);
    // https://www.int-res.com/abstracts/dao/v143/p205-226/
    String pat_abs00 = "branch(\\d+)/00(\\d)file\\.html";
    String pat_abs0 = "branch(\\d+)/0([1-9]{2})file\\.html";
    String pat_abs = "branch(\\d+)/([1-9]{3})file\\.html";
    String rep1 = "abstracts/meps/v14$1/p$2-001";
    PluginTestUtil.copyAu(sau, nau, ".*\\.html$", pat_abs00, rep1);
    PluginTestUtil.copyAu(sau, nau, ".*\\.html$", pat_abs0, rep1);
    PluginTestUtil.copyAu(sau, nau, ".*\\.html$", pat_abs, rep1);
    // https://www.int-res.com/articles/feature/d143p205.pdf
    // https://www.int-res.com/articles/meps_oa/d143p005.pdf
    // https://www.int-res.com/articles/meps2021/143/d143p005.pdf
    String pdf_pat_feat = "branch([3-7])/00([2,7])file\\.pdf";
    String pdf_rep_feat = "articles/feature/m14$1p00$2.pdf";
    PluginTestUtil.copyAu(sau, nau, ".*\\.pdf$", pdf_pat_feat, pdf_rep_feat);
    String pdf_pat_oa = "branch([3-7])/00([1])file\\.pdf";
    String pdf_rep_oa = "articles/meps_oa/m14$1p00$2.pdf";
    PluginTestUtil.copyAu(sau, nau, ".*\\.pdf$", pdf_pat_oa, pdf_rep_oa);
    String pdf_pat_main = "branch([3-7])/(\\d\\d[0,3,4,5,6,8,9])file\\.pdf";
    String pdf_rep_main = "articles/meps2021/14$1/m14$1p$2.pdf";
    PluginTestUtil.copyAu(sau, nau, ".*\\.pdf$", pdf_pat_main, pdf_rep_main);

    // Remove some URLs
    int deleted = 0;
    for (CachedUrl cu : AuUtil.getCuIterable(nau)) {
      String url = cu.getUrl();
      log.info("url: " + url);
      // delete a few pdfs, and one html page
      if (url.endsWith("v143/p1-001") || url.endsWith("m143p001.pdf") || url.endsWith("m147p006.pdf") || url.endsWith("m145p003.pdf")) {
        deleteBlock(cu);
        ++deleted;
      }
    }
    log.info("DELETED = " + deleted);
    assertEquals(4, deleted);

    Iterator<ArticleFiles> it = nau.getArticleIterator();

    int count = 0;
    int countHtmlOnly = 0;
    int countPdf = 0;
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
        assertEquals(af.getRoleUrl(ArticleFiles.ROLE_ABSTRACT), fturl);
      }
      if (af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF) == fturl) {
        ++countPdf;
      }
    }
    log.debug3("Article count is " + count);
    assertEquals(34, count);
    //assertEquals(2, countHtmlOnly);
    //assertEquals(32, countPdf);
  }

  private void deleteBlock(CachedUrl cu) throws IOException {
    log.info("deleting " + cu.getUrl());
    cu.delete();
  }
}
