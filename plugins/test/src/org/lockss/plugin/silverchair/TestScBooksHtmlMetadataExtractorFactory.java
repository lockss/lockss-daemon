package org.lockss.plugin.silverchair;

import java.util.ArrayList;
import java.util.List;

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

public class TestScBooksHtmlMetadataExtractorFactory extends LockssTestCase

  {

    static Logger log = Logger.getLogger(TestScBooksHtmlMetadataExtractorFactory.class);

    private MockLockssDaemon theDaemon;
    private SimulatedArchivalUnit sau; // Simulated AU to generate content
    private ArchivalUnit scbAu; // books AU

    private static String PLUGIN_NAME = "org.lockss.plugin.silverchair.SilverchairBooksPlugin";
    private static String BASE_URL = "http://www.basejournal.org/";
    private static String SIM_ROOT = BASE_URL + "bj/";
    private static String HEAD_TAG_DATA =
      "<head id=\"Head1\">\n" +
      "    <title> SPIE | Digital Converters for Image Sensors</title>\n" +
      "<meta name=\"WT.ti\" content=\"DL:_eBooks:_Detail_Intro_Page\" />\n" +
      "<meta name=\"WT.cg_n\" content=\"MainDrillDown:Digital_Library;ProductTypeDrillDown:Digital_Library;;;\" />\n" +
      "<meta name=\"WT.cg_s\" content=\"Main:Browse_Content;Product:SPIE_Press;;;\" />\n" +
      "<meta name=\"DCSext.auth\" content=\"True\" />\n" +
      "<meta name=\"DCSext.mobilesite\" content=\"No\" />\n" +
      "</head>\n";
    private static String BODY_TAG_DATA =
      "<body class=\"SCM6 book\">\n" +
      "    <h1 id=\"scm6MainContent_hfH1\" style=\"display:none;\">Digital Converters for Image Sensors</h1>\n" +
      "    <div class=\"mainContainer\">\n" +
      "     <div class=\"Heading\">\n" +
      "        <span id=\"scm6MainContent_lblBookTitle\">Digital Converters for Image Sensors</span>\n" +
      "        </div>\n" +
      "<div class=\"AuthorSection clearfix\">\n" +
      "<div class=\"Authorinfo\">\n" +
      "<span id=\"scm6MainContent_lblBookSubTitle\"></span></div>\n" +
      "<div class=\"Authorinfo\">\n" +
      "<span id=\"scm6MainContent_lblAuthors\">Author(s):&nbsp;&nbsp;&nbsp;&nbsp;J. Scott Tyo; Andrey Alenin</span></div>\n" +
      " <div class=\"Authorinfo\">\n" +
      "<span id=\"scm6MainContent_lblDate\">Published:&nbsp;&nbsp;&nbsp;2015</span></div>\n" +
      "<div class=\"Authorinfo\"  >\n" +
      "<span id=\"scm6MainContent_lblDOI\">DOI:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;10.1117/3.1000639</span></div>\n" +
      "<div class=\"Authorinfo\">\n" +
      "<span id=\"scm6MainContent_lblCitation\">eISBN:&nbsp;9781628413908&nbsp;&nbsp;|&nbsp;&nbsp;Print&nbsp;ISBN13:&nbsp;9781628413892</span></div>\n" +
      "<div class=\"Description\">Description: \n" +
      "<span id=\"scm6MainContent_lblDiscription\"><p class=\"para\">\n" +
      "This book is intended for image sensor professionals.\n" +
      "</p></span></div>\n" +
      "            </div>\n" +
      "</div>\n" +
      "</body>\n";

    public void setUp() throws Exception {
      super.setUp();
      String tempDirPath = setUpDiskSpace();

      theDaemon = getMockLockssDaemon();
      theDaemon.getAlertManager();
      theDaemon.getPluginManager().setLoadablePluginsReady(true);
      theDaemon.setDaemonInited(true);
      theDaemon.getPluginManager().startService();
      theDaemon.getCrawlManager();

      sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class,	simAuConfig(tempDirPath));
      scbAu = PluginTestUtil.createAndStartAu(PLUGIN_NAME, auConfig());
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
      conf.put("depth", "1");
      conf.put("branch", "2");
      conf.put("numFiles", "2");
      conf.put("fileTypes",""	+ (SimulatedContentGenerator.FILE_TYPE_PDF + SimulatedContentGenerator.FILE_TYPE_HTML));
      conf.put("default_article_mime_type", "text/html");
      return conf;
    }

    /**
     * Configuration method.
     * @return
     */
    Configuration auConfig() {
      Configuration conf = ConfigManager.newConfiguration();
      conf.put("base_url", BASE_URL);
      conf.put("resource_id","399");
      return conf;
    }

    // the metadata that should be extracted
    ArrayList<String> goodAuthors = new ArrayList<String>();

    private static String Author1 = "J. Scott Tyo";
    private static String Author2 = "Andrey Alenin";
    private static String GoodDate = "2015";
    private static String GoodTitle = "Digital Converters for Image Sensors";
    private static String GoodDOI =  "10.1117/3.1000639";
    private static String GoodEISBN = "9781628413908";
    private static String GoodISBN = "9781628413892";
    private static String GoodDescription =  "This book is intended for image sensor professionals.";
    private static String GoodPublisher = "SPIE";

    String goodContent =
      "<html> " +  HEAD_TAG_DATA + BODY_TAG_DATA + "</html>";

    public void testExtractFromGoodContent() throws Exception {
      goodAuthors.add(Author1);
      goodAuthors.add(Author2);

      String url = "http://www.example.com/bj/vol/";
      MockCachedUrl cu = new MockCachedUrl(url, scbAu);
      cu.setContent(goodContent);
      cu.setContentSize(goodContent.length());
      cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
      FileMetadataExtractor me = new ScBooksHtmlMetadataExtractorFactory.ScBooksHtmlMetadataExtractor();
      FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
      assertNotEmpty(mdlist);
      ArticleMetadata md = mdlist.get(0);
      assertNotNull(md);
   //   assertEquals(GoodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
      assertEquals(GoodTitle, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
      assertEquals(GoodDate, md.get(MetadataField.FIELD_DATE));
      assertEquals(goodAuthors, md.getList(MetadataField.FIELD_AUTHOR));
      assertEquals(GoodDOI, md.get(MetadataField.FIELD_DOI));
      assertEquals(GoodEISBN, md.get(MetadataField.FIELD_EISBN));
      assertEquals(GoodISBN, md.get(MetadataField.FIELD_ISBN));
      assertEquals(GoodDescription, md.get(MetadataField.FIELD_ABSTRACT));
    }

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

      public SimulatedContentGenerator getContentGenerator(Configuration cf, String fileRoot) {
        return new MySimulatedContentGenerator(fileRoot);
      }

    }

    /**
     * Inner class to create a html source code simulated content
     *
     */
    public static class MySimulatedContentGenerator extends	SimulatedContentGenerator {
      protected MySimulatedContentGenerator(String fileRoot) {
        super(fileRoot);
      }

    }

  }
