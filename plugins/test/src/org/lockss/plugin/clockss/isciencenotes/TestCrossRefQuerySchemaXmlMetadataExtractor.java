package org.lockss.plugin.clockss.isciencenotes;

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

public class TestCrossRefQuerySchemaXmlMetadataExtractor extends LockssTestCase {

    private static final Logger log = Logger.getLogger(TestCrossRefQuerySchemaXmlMetadataExtractor.class);

    private MockLockssDaemon theDaemon;
    private MockArchivalUnit mau;

    private static String BASE_URL = "http://www.source.org/";
    private static final String xml_url = BASE_URL + "2019/iSciNote2016/2016-1-iSciNote.xml";
    private static final String pdf_url1 = BASE_URL + "2019/iSciNote2016/2016-1-iSciNote.pdf";
    private static final String pdf_url2 = BASE_URL + "2019/iSciNote2016/2016-2-iSciNote.xml";

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


    private static final String realXMLFile = "crossref_query_schema_sample.xml";

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
            mau.addUrl(pdf_url2, true, true, xmlHeader);

            mcu.setContent(string_input);
            mcu.setContentSize(string_input.length());
            mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

            FileMetadataExtractor me = new IScienceNotesXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
            FileMetadataListExtractor mle =
                    new FileMetadataListExtractor(me);
            List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
            assertNotEmpty(mdlist);
            assertEquals(1, mdlist.size());

            ArticleMetadata AM = mdlist.get(0);
            assertNotNull(AM);

            assertEquals("iScience Notes", AM.get(MetadataField.FIELD_PUBLICATION_TITLE));
            assertEquals("2016-10-15", AM.get(MetadataField.FIELD_DATE));
            assertEquals("9", AM.get(MetadataField.FIELD_VOLUME));
            assertEquals("7", AM.get(MetadataField.FIELD_ISSUE));

        } finally {
            IOUtil.safeClose(file_input);
        }
    }
}