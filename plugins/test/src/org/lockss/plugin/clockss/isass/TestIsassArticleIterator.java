package org.lockss.plugin.clockss.isass;

import org.lockss.app.LockssApp;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.crawler.Exploder;
import org.lockss.crawler.FollowLinkCrawler;
import org.lockss.crawler.FuncZipExploder;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.test.ArticleIteratorTestCase;
import org.lockss.test.ConfigurationUtil;
import org.lockss.util.CIProperties;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
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

    Properties props = new Properties();
    props.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    props.setProperty("org.lockss.plugin.simulated.SimulatedContentGenerator.doZipFile", "true");
    props.setProperty(FollowLinkCrawler.PARAM_STORE_ARCHIVES, "true");
    ConfigurationUtil.setCurrentConfigFromProps(props);

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

    assertNotMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/ijssurgery-released/SPECIAL_DELIVERY_ijss_25_3_2022.zip!/ijss_11_2.pdf.zip/IJSS-11-14444-4009.pdf");
    assertMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/ijssurgery-released/2016/SPECIAL_DELIVERY_ijss_25_3_2022.zip!/ijss_11_2.xml.zip/IJSS-11-14444-4009.xml");
    assertMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/ijssurgery-released/2016/IJSS-1-01.zip!/IJSS-1-2006-0002-RR.xml");
    assertNotMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/ijssurgery-released/2016/IJSS-1-01.zip!/IJSS-1-2006-0002-RR.pdf");
    assertNotMatchesRE(pat, "https://clockss-test.lockss.org/sourcefiles/ijssurgery-released/2022/Archive%20for%20CLOCKSS/SPECIAL_DELIVERY_ijss_25_3_2022.zip!/ijss_15_6.peripherals.zip/meta_issue.xml");
  }



  public void testCreateArticleFiles() throws Exception {
    // 1 depth, 4 branches, 5 files, but removing some later in test

    sau.setExploderPattern(".zip$");
    sau.setExploderHelper(new FuncZipExploder.MyExploderHelper("bad"));
    PluginTestUtil.crawlSimAu(sau);
    /*
     *  Go through the simulated content you just crawled and modify the results to emulate
     *  what you would find in a "real" crawl with this plugin:
     *  <base_url>/48/<page#>/2011/art#file.{html, pdf & ris}
    */
    String pat1 = "branch(\\d+)/(\\d+file)\\.xml";
    String rep1 = YEAR + "/directory.zip/$2.xml";
    //PluginTestUtil.copyAu(sau, au, ".*\\.xml", pat1, rep1);
    String pat3 = "branch(\\d+)/branch(\\d+)/(\\d+file)\\.xml";
    String rep3 = YEAR + "/DIR/Dir2.zip/ISSAJ-$1$2.xml.zip/$3.xml";
    //PluginTestUtil.copyAu(sau, au, ".*\\.xml", pat3, rep3);
    String pat2 = "branch(\\d+)/(\\d+file)\\.pdf";
    String rep2 = YEAR + "/directory.zip/$2.pdf";
    //PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pat2, rep2);
    String pat4 = "branch(\\d+)/branch(\\d+)/(\\d+file)\\.pdf";
    String rep4 = YEAR + "/DIR/Dir2.zip!/ISSAJ-$1$2.pdf.zip/$3.pdf";
    //PluginTestUtil.copyAu(sau, au, ".*\\.pdf", pat4, rep4);

    // Remove some of the URLs just created to make test more robust
    // Remove files art1file.html and art2file.ris in each branch (and there are 4 branches)
    // Remove art3file.pdf in only one of the branches
    for (String url : Arrays.asList(
        BASE_URL + YEAR + "/IJSS-1-01.zip!/IJSS-1-2006-0002-RR.xml",
        BASE_URL + YEAR + "/IJSS-1-01.zip!/IJSS-1-2006-0002-RR.pdf",
        BASE_URL + YEAR + "/IJSS-1-01.zip!/IJSS-1-2006-0004-RR.pdf",
        BASE_URL + YEAR + "/IJSS-1-01.zip!/IJSS-1-2006-0004-RR.xml",
        BASE_URL + YEAR + "/Archive%20for%20CLOCKSS/SPECIAL_DELIVERY_ijss_25_3_2022.zip!/ijss_11_1.xml.zip/IJSS-11-14444-4001.xml",
        BASE_URL + YEAR + "/Archive%20for%20CLOCKSS/SPECIAL_DELIVERY_ijss_25_3_2022.zip!/ijss_15_6.peripherals.zip/meta_issue.xml"
      )) {

      CachedUrl cu = au.makeCachedUrl(url);
    }

    for (CachedUrl cu : AuUtil.getCuIterable(au)) {
      String url = cu.getUrl();
      log.info(url);
    }
    Iterator<ArticleFiles> it = au.getArticleIterator(MetadataTarget.Any());
    int count = 0;
    int countFullText = 0;
    int countMetadata = 0;
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      log.info(af.toString());
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

  private void deleteBlock(CachedUrl cu) throws IOException {
    log.info("deleting " + cu.getUrl());
    cu.delete();
  }
}
