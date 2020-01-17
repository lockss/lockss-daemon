package org.lockss.plugin.wroclawmedicaluniversity;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.atypon.BaseAtyponHtmlMetadataExtractorFactory;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockCachedUrl;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.Logger;

import java.util.Arrays;
import java.util.List;

public class TestWroclawMedicalUniversityMetadataExtractorFactory extends LockssTestCase {

    static Logger log = Logger.getLogger("TestWroclawMedicalUniversityMetadataExtractorFactory");

    private MockLockssDaemon theDaemon;
    private SimulatedArchivalUnit sau; // simulated au to generate content
    private ArchivalUnit au; // asce au
    private static String PLUGIN_NAME = "org.lockss.plugin.wroclawmedicaluniversity.ClockssWroclawMedicalUniversityJournalsPlugin";
    private static String BASE_URL = "http://www.wydawnictwo.umed.wroc.pl/";
    private final String YEAR = "2019";
    private final String VOLUME_NAME = "56";

    public void setUp() throws Exception {
        super.setUp();
        String tempDirPath = setUpDiskSpace();

        theDaemon = getMockLockssDaemon();
        theDaemon.getAlertManager();
        theDaemon.getPluginManager().setLoadablePluginsReady(true);
        theDaemon.setDaemonInited(true);
        theDaemon.getPluginManager().startService();
        theDaemon.getCrawlManager();

        au = PluginTestUtil.createAndStartAu(PLUGIN_NAME, auConfig());
    }

    public void tearDown() throws Exception {
        //sau.deleteContentTree();
        theDaemon.stopDaemon();
        super.tearDown();
    }

    // Configuration method. 
    Configuration auConfig() {
        Configuration conf = ConfigManager.newConfiguration();
        conf.put("base_url", BASE_URL);
        conf.put("year", YEAR);
        conf.put("volume_name", VOLUME_NAME);
        return conf;
    }

    // a chunk of html source code from the publisher's site from where the 
    // metadata should be extracted
    String goodContent =
            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
            "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
            "<html>" +
            "<head>" +
            "<link rel=\"schema.DC\" href=\"http://purl.org/DC/elements/1.0/\"></link>" +
           "<meta name=\"citation_title\" content=\"Does metabolic control affect salivary adipokines in type 2 diabetes mellitus?\">\n" +
           "<meta name=\"citation_journal_title\" content=\"Dental and Medical Problems\">\n" +
           "<meta name=\"citation_issn\" content=\"1644-387X\">\n" +
           "<meta name=\"citation_publisher\" content=\"Wroclaw Medical University\">\n" +
           "<meta name=\"citation_issue\" content=\"1\">\n" +
           "<meta name=\"citation_volume\" content=\"56\">\n" +
           "<meta name=\"citation_firstpage\" content=\"11\">\n" +
           "<meta name=\"citation_lastpage\" content=\"20\">\n" +
           "<meta name=\"citation_date\" content=\"2019\">\n" +
           "<meta name=\"citation_doi\" content=\"10.17219/dmp/103417\">\n" +
           "<meta name=\"citation_author\" content=\"Teke, Elif\">\n" +
           "</head>" +
           "</html>";

    public void testExtractFromGoodContent() throws Exception {
        // HTML file which the metadata extracted from.
        String url = "http://www.dmp.umed.wroc.pl/en/article/2019/56/1/11/";
        MockCachedUrl cu = new MockCachedUrl(url, au);
        cu.setContent(goodContent);
        cu.setContentSize(goodContent.length());
        cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
        FileMetadataExtractor me =
                new WroclawMedicalUniversityMetadataExtractorFactory.WroclawMedicalUniversityHtmlMetadataExtractor();

        FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
        assertNotEmpty(mdlist);
        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);

        assertEquals("Teke, Elif", md.get(MetadataField.FIELD_AUTHOR));
        assertEquals("2019", md.get(MetadataField.FIELD_DATE));
        assertEquals("Does metabolic control affect salivary adipokines in type 2 diabetes mellitus?", md.get(MetadataField.FIELD_ARTICLE_TITLE));
        assertEquals("Dental and Medical Problems", md.get(MetadataField. FIELD_PUBLICATION_TITLE));
        assertEquals("56", md.get(MetadataField.FIELD_VOLUME));
        assertEquals("1", md.get(MetadataField.FIELD_ISSUE));
        assertEquals("11", md.get(MetadataField.FIELD_START_PAGE));
        assertEquals("20", md.get(MetadataField.FIELD_END_PAGE));
        assertEquals("10.17219/dmp/103417", md.get(MetadataField.FIELD_DOI));
        assertEquals("1644-387X", md.get(MetadataField.FIELD_ISSN));

        assertNotEquals("Lastname, Firstname", md.get(MetadataField.FIELD_AUTHOR));
        assertNotEquals("2018", md.get(MetadataField.FIELD_DATE));
        assertNotEquals("563", md.get(MetadataField.FIELD_VOLUME));
        assertNotEquals("13", md.get(MetadataField.FIELD_ISSUE));
        assertNotEquals("15", md.get(MetadataField.FIELD_START_PAGE));
        assertNotEquals("56", md.get(MetadataField.FIELD_END_PAGE));
        assertNotEquals("10.17245/dmp/103417", md.get(MetadataField.FIELD_DOI));
        assertNotEquals("1644387", md.get(MetadataField.FIELD_ISSN));

    }

}

