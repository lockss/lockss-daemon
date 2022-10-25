package org.lockss.plugin.clockss.frontiers;

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
import java.util.Iterator;
import java.util.List;

public class TestFrontiersBooksCrossrefXmlMetadataExtractorFactory extends LockssTestCase {

    private static final Logger log = Logger.getLogger(TestFrontiersBooksXmlMetadataExtractor.class);

    private MockLockssDaemon theDaemon;
    private MockArchivalUnit mau;

    private static String PLUGIN_NAME = "org.lockss.plugin.clockss.frontiers.ClockssFrontiersBooksDirSourcePlugin";
    private static String BASE_URL = "http://www.source.org/";
    private static final String xml_url = BASE_URL + "Ebooks.xml";
    //  //978-2-88919-000-3, 978-2-88919-001-0
    private static final String pdf1 = BASE_URL + "9782889459483.pdf";
    private static final String pdf2 = BASE_URL + "9782889459483.PDF";

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
        conf.put("directory", "2022_01");
        return conf;
    }


    private static final String realXMLFile = "FrontiersBooksCrossRefSample.xml";

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
            // these aren't opened, so it doens't matter that they have the wrong header type
            mau.addUrl(pdf1, true, true, xmlHeader);
            mau.addUrl(pdf2, true, true, xmlHeader);

            mcu.setContent(string_input);
            mcu.setContentSize(string_input.length());
            mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

            FileMetadataExtractor me = new FrontiersBooksCrossrefXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
            FileMetadataListExtractor mle =
                    new FileMetadataListExtractor(me);
            List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
            assertNotEmpty(mdlist);
            assertEquals(1, mdlist.size());

            ArticleMetadata md = mdlist.get(0);
            assertNotNull(md);

            assertEquals("9782889459483", md.get(MetadataField.FIELD_ISBN));
            assertEquals("10.3389/978-2-88945-948-3", md.get(MetadataField.FIELD_DOI));
            assertEquals("Advancing Psychological Methods Across Borders", md.get(MetadataField.FIELD_PUBLICATION_TITLE));
            assertEquals("2020", md.get(MetadataField.FIELD_DATE));
        } finally {
            IOUtil.safeClose(file_input);
        }

    }

}

