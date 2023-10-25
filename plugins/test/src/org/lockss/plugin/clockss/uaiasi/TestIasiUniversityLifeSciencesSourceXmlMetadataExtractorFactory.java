package org.lockss.plugin.clockss.uaiasi;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.CIProperties;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

import java.io.InputStream;
import java.util.List;

public class TestIasiUniversityLifeSciencesSourceXmlMetadataExtractorFactory extends LockssTestCase {

    private static final Logger log = Logger.getLogger(TestIasiUniversityLifeSciencesSourceXmlMetadataExtractorFactory.class);

    private MockLockssDaemon theDaemon;
    private MockArchivalUnit mau;

    private static String BASE_URL = "http://www.source.org/";
    private static final String xml_url = BASE_URL + "2021-1.xml";
    private static final String pdf_url1 = BASE_URL + "/2021-1/JALSE1-21-10-1.pdf";

    public void setUp() throws Exception {
        super.setUp();
        setUpDiskSpace(); // you need this to have startService work properly...

        theDaemon = getMockLockssDaemon();
        mau = new MockArchivalUnit();

        theDaemon.getAlertManager();
        theDaemon.getPluginManager().setLoadablePluginsReady(true);
        theDaemon.setDaemonInited(true);
        theDaemon.getPluginManager().startService();
        theDaemon.getCrawlManager();
        mau.setConfiguration(auConfig());

    }

    public void tearDown() throws Exception {
        theDaemon.stopDaemon();
        super.tearDown();
    }

    /**
     * Configuration method.
     *
     * @return
     */
    Configuration auConfig() {
        Configuration conf = ConfigManager.newConfiguration();
        conf.put("base_url", BASE_URL);
        conf.put("directory", "2021");
        return conf;
    }


    private static final String realXMLFile = "sample.xml";

    public void testFromJatsPublishingXMLFile() throws Exception {
        InputStream file_input = null;
        try {
            file_input = getResourceAsStream(realXMLFile);
            String string_input = StringUtil.fromInputStream(file_input);
            IOUtil.safeClose(file_input);

            CIProperties xmlHeader = new CIProperties();
            xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
            MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
            // Now add all the pdf files in our AU since we check for them before emitting
            mau.addUrl(pdf_url1, true, true, xmlHeader);

            mcu.setContent(string_input);
            mcu.setContentSize(string_input.length());
            mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

            FileMetadataExtractor me = new IasiUniversityLifeSciencesSourceXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
            FileMetadataListExtractor mle =
                    new FileMetadataListExtractor(me);
            List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
            assertNotEmpty(mdlist);
            assertEquals(1, mdlist.size());

            ArticleMetadata AM = mdlist.get(0);
            assertNotNull(AM);

            //assertEquals("Air and Space Law", AM.get(MetadataField.FIELD__TITLE));
            assertEquals("2021-6-25", AM.get(MetadataField.FIELD_DATE));
            assertEquals("185", AM.get(MetadataField.FIELD_VOLUME));
            assertEquals("1", AM.get(MetadataField.FIELD_ISSUE));
            assertEquals("2784-0360", AM.get(MetadataField.FIELD_EISSN));
            assertEquals("2784-0379", AM.get(MetadataField.FIELD_ISSN));

        } finally {
            IOUtil.safeClose(file_input);
        }
    }
}
