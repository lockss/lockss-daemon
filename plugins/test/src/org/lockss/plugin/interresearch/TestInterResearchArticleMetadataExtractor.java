package org.lockss.plugin.interresearch;

import org.lockss.config.ConfigManager;
import org.lockss.config.Tdb;
import org.lockss.config.TdbAu;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlMetadataExtractorFactory;
import org.lockss.plugin.atypon.BaseAtyponRisMetadataExtractorFactory;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.CIProperties;
import org.lockss.util.Logger;
import org.lockss.util.TypedEntryMap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;

public class TestInterResearchArticleMetadataExtractor extends LockssTestCase {
  static Logger log = Logger.getLogger(TestInterResearchArticleMetadataExtractor.class);

  private MockLockssDaemon theDaemon;
  private ArchivalUnit bau1;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  private static String BASE_URL = "https://www.int-res.com/";

  // the metadata that should be extracted
  static String goodDate = "2021/02/25";
  static String[] goodAuthors = new String[] {"First M. Last", "First2 M2. Last2"};
  static String goodFormat = "text/HTML";
  static String goodTitle = "Title of Article";
  static String goodType = "research-article";
  static String goodPublisher = "Inter-Research";
  static String goodDOI = "10.3354/dao03578";

  static String goodJournal = "Title of Journal";
  static String goodStartPage = "205";
  static String goodEndPage = "226";
  static String goodVolume = "143";
  static String goodIssn = "0177-5103";
  static String doiURL = "http://dx.doi.org/" + goodDOI;
  private static final String ABS_URL =  BASE_URL + "abstracts/meps/v43/p205-226";

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace(); // you need this to have startService work properly...

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();


    // in this directory this is file "test_baseatypon.tdb" but it becomes xml
    ConfigurationUtil.addFromUrl(getResource("test.xml"));
    Tdb tdb = ConfigManager.getCurrentConfig().getTdb();

    TdbAu tdbau1 = tdb.getTdbAusLikeName("").get(0);
    bau1 = PluginTestUtil.createAndStartAu(tdbau1);
    assertNotNull(bau1);
    TypedEntryMap auConfig =  bau1.getProperties();
    assertEquals(BASE_URL, auConfig.getString(BASE_URL_KEY));
  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }


  String metadata =
      "<meta name=\"citation_title\" content=\"Title of Article\">\n" +
      "<meta name=\"citation_publication_date\" content=\"2021/02/25\">\n" +
      "<meta name=\"citation_journal_title\" content=\"Title of Journal\">\n" +
      "<meta name=\"citation_issn\" content=\"0177-5103\">\n" +
      "<meta name=\"citation_volume\" content=\"143\">\n" +
      "<meta name=\"citation_doi\" content=\"10.3354/dao03578\">\n" +
      "<meta name=\"citation_firstpage\" content=\"205\">\n" +
      "<meta name=\"citation_lastpage\" content=\"226\">\n" +
      "<meta name=\"citation_author\" content=\"First M. Last\">\n" +
      "<meta name=\"citation_author\" content=\"First2 M2. Last2\">\n" +
      "<meta name=\"citation_keywords\" content=\"Right whale; Health; Trauma; Reproduction; Stressor; Cumulative effects\">\n" +
      "<meta name=\"citation_journal_publisher\" content=\"Inter-Research\">\n" +
      "<meta name=\"citation_journal_abbreviation\" content=\"Dis Aquat Org\">\n" +
      "<meta name=\"citation_journal_abbreviation\" content=\"DAO\">\n" +
      "<meta name=\"citation_issn\" content=\"1616-1580\">\n" +
      "<meta name=\"citation_abstract_html_url\" content=\"https://www.int-res.com/abstracts/dao/v143/p205-226/\">\n" +
      "<meta name=\"citation_xml_url\" content=\"https://www.int-res.com/articles/xml/dao/143/d143p205.xml\">\n" +
      "<meta name=\"citation_pdf_url\" content=\"https://www.int-res.com/articles/feature/d143p205.pdf\">\n" +
      "<meta name=\"citation_author_email\" content=\"mmoore@whoi.edu\">";


  public void testExtractGoodHtmlContent() throws Exception {

    List<ArticleMetadata> mdlist = setupContentForAU(bau1, ABS_URL, metadata);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    log.info(md.toString());
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(Arrays.asList(goodAuthors), md.getList(MetadataField.FIELD_AUTHOR));
  }

  /* private support methods */
  private List<ArticleMetadata> setupContentForAU(ArchivalUnit au, String url,
                                                  String content) throws IOException, PluginException {
    FileMetadataExtractor me;

    InputStream input = null;
    CIProperties props = null;
    input = IOUtils.toInputStream(content, "utf-8");
    props = getContentHtmlProperties();
    me = new InterResearchHtmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/html");

    UrlData ud = new UrlData(input, props, url);
    UrlCacher uc = au.makeUrlCacher(ud);
    uc.storeContent();
    CachedUrl cu = uc.getCachedUrl();
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    return mle.extract(MetadataTarget.Any(), cu);
  }

  private CIProperties getContentHtmlProperties() {
    CIProperties cProps = new CIProperties();
    // the CU checks the X-Lockss-content-type, not the content-type to determine encoding
    cProps.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html; charset=UTF-8");
    cProps.put("Content-type",  "text/html; charset=UTF-8");
    return cProps;
  }

}
