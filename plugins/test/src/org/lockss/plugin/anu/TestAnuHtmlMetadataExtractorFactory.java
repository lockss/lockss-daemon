package org.lockss.plugin.anu;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.plugin.simulated.SimulatedPlugin;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockCachedUrl;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.Logger;

import java.util.Arrays;
import java.util.List;

public class TestAnuHtmlMetadataExtractorFactory extends LockssTestCase {

  static Logger log = Logger.getLogger("TestAnuHtmlMetadataExtractorFactory");

  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau; // Simulated AU to generate content
  private ArchivalUnit bau; // Anu AU
  private static final String issnTemplate = "%1%2%3%1-%3%1%2%3";

  private static String PLUGIN_NAME = "org.lockss.plugin.anu.ClockssAnuPlugin";

  private static String BASE_URL = "https://press.anu.edu.au/";
  private static String SIM_ROOT = BASE_URL + "download/";
  /**
   * Inner class that where a number of Archival Units can be created
   *
   */
  public static class MySimulatedPlugin extends SimulatedPlugin {
    public ArchivalUnit createAu0(Configuration auConfig)
        throws ArchivalUnit.ConfigurationException {
      ArchivalUnit au = new SimulatedArchivalUnit(this);
      au.setConfiguration(auConfig);
      return au;
    }
  }
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    sau = PluginTestUtil.createAndStartSimAu(
        TestAnuHtmlMetadataExtractorFactory.MySimulatedPlugin.class,
        simAuConfig(tempDirPath)
    );
    bau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, AuConfig());
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
    conf.put("branch", "3");
    conf.put("numFiles", "7");
    conf.put("fileTypes",""	+ (SimulatedContentGenerator.FILE_TYPE_PDF + SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("default_article_mime_type", "application/html");
    return conf;
  }

  /**
   * Configuration method.
   * @return
   */
  Configuration AuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("year", "2000");
    conf.put("volume_name", "13");
    conf.put("journal_id", "xyzjn");
    return conf;
  }

  // the metadata that should be extracted
  String[] goodAuthors = new String[] {
      "G Poelmans", "J K Buitelaar", "D L Pauls", "B Franke" };
  String goodAuthor = "Blair Williams";
  String goodArticleTitle = "Where are the Great Women? A feminist analysis of Australian political biographies";

  // a chunk of html source code from the publisher's site from where the
  // metadata should be extracted, note the meta tags are useless, and the html tags are what we extract
  String goodContent =
    "<html class=\"\" lang=\"en\">" +
      "<head>\n" +
        "<meta charset=\"utf-8\">\n" +
        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
        "<title> - Australian Journal of Biography and History: No. 5, 2021 - ANU</title>\n" +
        "<meta name=\"dcterms.identifier\" content=\"http://press-files.anu.edu.au/downloads/index.php\">\n" +
        "<meta name=\"dcterms.description\" content=\"***description of this page***\">\n" +
        "<meta name=\"dcterms.subject\" content=\"***keywords describing the content of this page, separated with commas***\">\n" +
        "<meta name=\"dcterms.modified\" content=\"2019-04-11\">\n" +
        "<meta name=\"dcterms.creator\" content=\"\">\n" +
        "<meta name=\"dcterms.creator\" content=\"\">\n" +
        "<meta name=\"description\" content=\"***description of this page***\">\n" +
        "<meta name=\"keywords\" content=\"***keywords describing the content of this page, separated with commas***\">\n" +
        "<meta name=\"test\" content=\"test\">\n" +
        "<meta name=\"generator\" content=\"ANU.template GW2-4 | ANU.appid anu-php-4.20-20150814 | ANU.GMS 4.23_20160606 | ANU.inc_from style.anu.edu.au\">\n" +
        "<meta name=\"dcterms.publisher\" content=\"The Australian National University\">\n" +
        "<meta name=\"dcterms.publisher\" content=\"webmaster@anu.edu.au\">\n" +
      "</head>\n" +
      "<body>"+
        "<div class=\"doublewide\">"+
          "<div id=\"content-wrapper\" style=\"position: relative\">" +
            "<h1 class=\"Chapter-Title\"><a></a>Where are the Great Women? A feminist analysis of Australian political biographies</h1>" +
            "<p class=\"Chapter-Author\"><a></a>Blair Williams</p>" +
          "</div>" +
        "</div>" +
      "</body>" +
    "</html>"
  ;

  /**
   * Method that creates a simulated Cached URL from the source code provided by
   * the goodContent String. It then asserts that the metadata extracted, by using
   * the NatureHtmlMetadataExtractorFactory, match the metadata in the source code.
   * @throws Exception
   */
  public void testExtractFromGoodContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new AnuHtmlMetadataExtractorFactory.AnuHtmlMetadataExtractor("text/html");
    FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    log.info(md.toString());
    assertEquals(goodAuthor, md.get(MetadataField.FIELD_AUTHOR));
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
  }

}
