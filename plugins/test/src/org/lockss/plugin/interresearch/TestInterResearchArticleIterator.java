package org.lockss.plugin.interresearch;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.config.Tdb;
import org.lockss.config.TdbAu;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.Logger;
import org.lockss.util.TypedEntryMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TestInterResearchArticleIterator extends LockssTestCase {
  private static final Logger log = Logger.getLogger("TestInterResearchArticleIterator.class");

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  private ArchivalUnit nau;		// Nature AU
  private MockLockssDaemon theDaemon;
  private static final int DEFAULT_FILESIZE = 3000;
  private static int fileSize = DEFAULT_FILESIZE;

  private static String PLUGIN_NAME =
      "org.lockss.plugin.interresearch.ClockssInterResearchPlugin";

  private static String JOURNAL_ID = "meps";
  private static String BASE_URL = "https://www.int-res.com/";
  private static int YEAR = 2021;
  private static String VOLUME = "43-47";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    ConfigurationUtil.addFromUrl(getResource("test.xml"));
    Tdb tdb = ConfigManager.getCurrentConfig().getTdb();
    TdbAu tdbau1 = tdb.getTdbAusLikeName( "").get(0);
    nau = PluginTestUtil.createAndStartAu(tdbau1);

    assertNotNull(nau);
    TypedEntryMap auConfig =  nau.getProperties();
    assertEquals(BASE_URL, auConfig.getString(ConfigParamDescr.BASE_URL.getKey()));
    assertEquals(JOURNAL_ID, auConfig.getString(ConfigParamDescr.JOURNAL_ID.getKey()));
    assertEquals(YEAR, auConfig.getInt(ConfigParamDescr.YEAR.getKey()));

    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));

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

  public void testArticleCountAndType() throws Exception {
    log.setLevel("debug3");
    PluginTestUtil.crawlSimAu(sau);
    // https://www.int-res.com/abstracts/dao/v143/p205-226/
    String pat_abs00 = "branch(\\d)/00(\\d)file\\.html";
    String pat_abs0 = "branch(\\d)/0([1-9]{2})file\\.html";
    String pat_abs = "branch(\\d)/([1-9]{3})file\\.html";
    String rep1 = "abstracts/meps/v4$1/p$2-001";
    PluginTestUtil.copyAu(sau, nau, "branch([3,5])/.*\\.html$", pat_abs00, rep1);
    PluginTestUtil.copyAu(sau, nau, "branch([3,5])/.*\\.html$", pat_abs0, rep1);
    PluginTestUtil.copyAu(sau, nau, "branch([3,5])/.*\\.html$", pat_abs, rep1);
    String rep2 = "abstracts/meps/v4$1/n2/p$2-001";
    PluginTestUtil.copyAu(sau, nau, "branch([4,6,7])/.*\\.html$", pat_abs00, rep2);
    PluginTestUtil.copyAu(sau, nau, "branch([4,6,7])/.*\\.html$", pat_abs0, rep2);
    PluginTestUtil.copyAu(sau, nau, "branch([4,6,7])/.*\\.html$", pat_abs, rep2);
    // https://www.int-res.com/articles/feature/d143p205.pdf
    // https://www.int-res.com/articles/meps_oa/d143p005.pdf
    // https://www.int-res.com/articles/meps2021/143/d143p005.pdf
    String pdf_pat_feat = "branch([3-7])/00([2,7])file\\.pdf";
    String pdf_rep_feat = "articles/feature/m04$1p00$2.pdf";
    PluginTestUtil.copyAu(sau, nau, ".*\\.pdf$", pdf_pat_feat, pdf_rep_feat);
    String pdf_pat_oa = "branch([3-7])/00([1])file\\.pdf";
    String pdf_rep_oa = "articles/meps_oa/m04$1p00$2.pdf";
    PluginTestUtil.copyAu(sau, nau, ".*\\.pdf$", pdf_pat_oa, pdf_rep_oa);
    String pdf_pat_main = "branch([3-7])/(\\d\\d[0,3,4,5,6,8,9])file\\.pdf";
    String pdf_rep_main = "articles/meps2021/4$1/m04$1p$2.pdf";
    PluginTestUtil.copyAu(sau, nau, ".*\\.pdf$", pdf_pat_main, pdf_rep_main);

    // Remove some URLs
    int deleted = 0;
    for (CachedUrl cu : AuUtil.getCuIterable(nau)) {
      String url = cu.getUrl();
      // log.info("url: " + url);
      // delete a few pdfs, and one html page
      if (url.endsWith("m043p001.pdf") || url.endsWith("m047p006.pdf") || url.endsWith("m045p003.pdf")) {
        deleteBlock(cu);
        ++deleted;
      }
    }
    assertEquals(3, deleted);

    // get the articleiterator, overriding the default purpose from "article" to any to ensure that
    // additional (i.e. all) aspects are processed.
    // see: src/org/lockss/plugin/SubTreeArticleIteratorBuilder.java:492
    Iterator<ArticleFiles> it = nau.getArticleIterator(new MetadataTarget(MetadataTarget.PURPOSE_ANY));

    int count = 0;
    int countHtmlOnly = 0;
    int countPdf = 0;
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      CachedUrl ftcu = af.getFullTextCu();
      String fturl = ftcu.getUrl();
      assertNotNull(ftcu);
      String contentType = ftcu.getContentType();
      //log.debug3("count " + count + " FullTextCU " + fturl + " content type: " + contentType);
      count++;
      // The full text CU will be the PDF unless one doesn't exist.
      // we deleted 3 pdfs above
      if (af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF) == null) {
        ++countHtmlOnly;
        assertEquals(af.getRoleUrl(ArticleFiles.ROLE_ABSTRACT), fturl);
      }
      if (af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF) == fturl) {
        ++countPdf;
      }
    }
    log.debug3("Article count is " + count);
    assertEquals(35, count);
    assertEquals(3, countHtmlOnly);
    assertEquals(32, countPdf);
  }

  private void deleteBlock(CachedUrl cu) throws IOException {
    log.debug3("deleting " + cu.getUrl());
    cu.delete();
  }
}
