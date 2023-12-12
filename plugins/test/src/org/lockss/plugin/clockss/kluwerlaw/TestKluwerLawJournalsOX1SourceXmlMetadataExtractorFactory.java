package org.lockss.plugin.clockss.kluwerlaw;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorTest;
import org.lockss.plugin.clockss.isciencenotes.IScienceNotesXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.isciencenotes.TestCrossRefQuerySchemaXmlMetadataExtractor;
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

public class TestKluwerLawJournalsOX1SourceXmlMetadataExtractorFactory extends LockssTestCase {

    private static final Logger log = Logger.getLogger(TestKluwerLawJournalsOX1SourceXmlMetadataExtractorFactory.class);

    private MockLockssDaemon theDaemon;
    private MockArchivalUnit mau;

    private static String BASE_URL = "http://www.source.org/";
    private static final String xml_url = BASE_URL + "AILA/204497.ox1";
    private static final String pdf_url1 = BASE_URL + "AILA/204497.pdf";

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
        conf.put("year", "2016");
        return conf;
    }


    private static final String realXMLFile = "sample.ox1";

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

            FileMetadataExtractor me = new KluwerLawJournalsOX1SourceXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
            FileMetadataListExtractor mle =
                    new FileMetadataListExtractor(me);
            List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
            assertNotEmpty(mdlist);
            assertEquals(1, mdlist.size());

            ArticleMetadata AM = mdlist.get(0);
            assertNotNull(AM);

            assertEquals("Air and Space Law", AM.get(MetadataField.FIELD_PUBLICATION_TITLE));
            assertEquals("02011999", AM.get(MetadataField.FIELD_DATE));
            assertEquals("24", AM.get(MetadataField.FIELD_VOLUME));
            assertEquals(" 1", AM.get(MetadataField.FIELD_ISSUE));

        } finally {
            IOUtil.safeClose(file_input);
        }
    }
}
