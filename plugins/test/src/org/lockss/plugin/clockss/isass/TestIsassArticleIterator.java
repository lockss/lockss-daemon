package org.lockss.plugin.clockss.isass;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.test.ArticleIteratorTestCase;
import org.lockss.test.ConfigurationUtil;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;

public class TestIsassArticleIterator extends ArticleIteratorTestCase {

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content

  private final String PLUGIN_NAME = "org.lockss.plugin.clockss.isass.ClockssIsassSourcePlugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  private final String BASE_URL = "http://clockss-ingest.lockss.org/sourcefiles/ijssurgery-released/";
  private final String YEAR = "2016";
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
      BASE_URL_KEY, BASE_URL,
      YEAR_KEY, YEAR);
  private static final int DEFAULT_FILESIZE = 3000;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    au = createAu();
    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));

  }

  public void tearDown() throws Exception {
    sau.deleteContentTree();
    super.tearDown();
  }

  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return
        PluginTestUtil.createAndStartAu(PLUGIN_NAME,  AU_CONFIG);
  }

  Configuration simAuConfig(String rootPath) {
    ConfigurationUtil.addFromArgs(
        SimulatedContentGenerator.CONFIG_PREFIX + "doZipFile",
        "true");
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put(BASE_URL_KEY, BASE_URL);
    conf.put("depth", "2");
    conf.put("branch", "4");
    conf.put("numFiles", "5");
    conf.put("fileTypes",
        "" + (SimulatedContentGenerator.FILE_TYPE_XML |
            SimulatedContentGenerator.FILE_TYPE_PDF ));
    conf.put("binFileSize", ""+DEFAULT_FILESIZE);
    return conf;
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);

    // Should match these
    // new pattern
    assertMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/ijssurgery-released/2016/SPECIAL_DELIVERY_ijss_25_3_2022.zip!/ijss_11_2.xml.zip/IJSS-11-14444-4009.xml");
    // old pattern
    assertMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/ijssurgery-released/2016/IJSS-1-01.zip!/IJSS-1-2006-0002-RR.xml");

    // Should NOT match these
    // new pattern pdf
    assertNotMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/ijssurgery-released/SPECIAL_DELIVERY_ijss_25_3_2022.zip!/ijss_11_2.pdf.zip/IJSS-11-14444-4009.pdf");
    // old pattern pdf
    assertNotMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/ijssurgery-released/2016/IJSS-1-01.zip!/IJSS-1-2006-0002-RR.pdf");
    // new pattern non-article xml
    assertNotMatchesRE(pat, "https://clockss-test.lockss.org/sourcefiles/ijssurgery-released/2022/Archive%20for%20CLOCKSS/SPECIAL_DELIVERY_ijss_25_3_2022.zip!/ijss_15_6.peripherals.zip/meta_issue.xml");
  }



  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    /*
     *  Go through the simulated content you just crawled and modify the results to emulate
     *  what you would find in a "real" crawl with this plugin:
    */
    // xml old and new
    // BASE_URL + YEAR + "/IJSS-1-01.zip!/IJSS-1-2006-0002-RR.xml",
    String pat1 = "content.zip!/branch(\\d+)/(\\d+file)\\.xml";
    String rep1 = YEAR + "/IJSS-$1.zip!/IJSS-$1-" + YEAR + "-$2.xml";
    PluginTestUtil.copyAu(sau, au, ".*\\.zip", pat1, rep1);
    // BASE_URL + YEAR + "/Archive%20for%20CLOCKSS/SPECIAL_DELIVERY_ijss_25_3_2022.zip!/ijss_11_1.xml.zip/IJSS-11-14444-4001.xml",
    String pat2 = "content.zip!/branch(\\d+)/branch(\\d+)/(\\d+file)\\.xml";
    String rep2 = YEAR + "/Archive%20for%20CLOCKSS/DELIVERY_ijss_" + YEAR + ".zip!/ijss_$1_$2.xml.zip/IJSS-$1-$3.xml";
    PluginTestUtil.copyAu(sau, au, ".*\\.zip", pat2, rep2);
    // pdf old and new
    // BASE_URL + YEAR + "/IJSS-1-01.zip!/IJSS-1-2006-0002-RR.pdf",
    String pat3 = "content.zip!/branch(\\d+)/(\\d+file)\\.pdf";
    String rep3 = YEAR + "/IJSS-$1.zip!/IJSS-$1-" + YEAR + "-$2.pdf";
    PluginTestUtil.copyAu(sau, au, ".*\\.zip", pat3, rep3);
    String pat4 = "content.zip!/branch(\\d+)/branch(\\d+)/(\\d+file)\\.pdf";
    String rep4 = YEAR + "/Archive%20for%20CLOCKSS/DELIVERY_ijss_" + YEAR + ".zip!/ijss_$1_$2.pdf.zip/IJSS-$1-$3.pdf";
    PluginTestUtil.copyAu(sau, au, ".*\\.zip", pat4, rep4);

    for (CachedUrl cu : AuUtil.getCuIterable(au)) {
      String url = cu.getUrl();
      log.info("cu!: " + url);
    }
    Iterator<ArticleFiles> it = au.getArticleIterator(MetadataTarget.Any());
    int count = 0;
    int countFullText = 0;
    int countMetadata = 0;
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      log.info("Af: " + af.toString());
      count ++;
      CachedUrl cu = af.getFullTextCu();
      if ( cu != null) {
        String url = cu.getUrl();
        String contentType = cu.getContentType();
        log.debug("countFullText " + count + " url " + url + " " + contentType);
        ++countFullText;
      }
      cu = af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA);
      if (cu != null) {
        ++countMetadata; //could be ris file or abstract
      }
    }

    // 20 possible articles 
    //     count & countFullText - 1 for RIS only (branch1, file1)
    //     countMetadata - 2 (pdf only branch1, file 5)
    log.debug("Article count is " + count);
    assertEquals(14, count); //20 (5 x 4 branches; minus branch1, file1 which only has a ris version
    assertEquals(14, countFullText); // current builder counts abstract as full text if all there is
    assertEquals(13, countMetadata); // if you have an articlefiles and either ris or abstract
  }

}
