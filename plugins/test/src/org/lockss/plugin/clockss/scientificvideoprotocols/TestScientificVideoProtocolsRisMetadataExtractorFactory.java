package org.lockss.plugin.clockss.scientificvideoprotocols;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataListExtractor;
import org.lockss.extractor.MetadataTarget;
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

public class TestScientificVideoProtocolsRisMetadataExtractorFactory extends LockssTestCase {

    static Logger log = Logger.getLogger(TestScientificVideoProtocolsRisMetadataExtractorFactory.class);

    private MockLockssDaemon theDaemon;
    private MockArchivalUnit mau;

    private static String BASE_URL = "http://www.source.org/";

    /*
     * Set up the metadata expected for each of the above tests
     */

    private static final String pdfUrl1 = "http://www.source.com/2019/10_32386.pdf";
    private static final String pdfUrl2 = "http://www.source.com/2019/10_32387.pdf";

    private static String risUrl1 = "http://www.source.com/2019/10_32386.ris";
    private static String risUrl2 = "http://www.source.com/2019/10_32388.ris";
    private static String risNoPdf3 = "http://www.source.com/2019/10_32389.ris";


    private static CIProperties risHeader = new CIProperties();
    private MockCachedUrl mcu;
    private FileMetadataExtractor me;
    private FileMetadataListExtractor mle;

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

        // the following is consistent across all tests; only content changes
        risHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "application/ris");

        // these aren't opened, so it doens't matter that they have the wrong header type
        mau.addUrl(pdfUrl1, true, true, risHeader);
        mau.addUrl(pdfUrl2, true, true, risHeader);
        // these are actually ris text files
        mau.addUrl(risUrl1, true, true, risHeader);
        mau.addUrl(risUrl2, true, true, risHeader);
        mau.addUrl(risNoPdf3, true, true, risHeader);

        me = new ScientificVideoProtocolsRisMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/plain");
        mle = new FileMetadataListExtractor(me);

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
        conf.put("year", "2019");
        return conf;
    }

    private static final String ris1 = "test_ris1.txt";
    private static final String ris2 = "test_ris2.txt";

    public void testFromRealRisFile() throws Exception {
        InputStream file_input = null;
        try {
            file_input = getResourceAsStream(ris1);
            String string_input = StringUtil.fromInputStream(file_input);
            IOUtil.safeClose(file_input);

            mcu = mau.addUrl(risUrl1, true, true, risHeader);
            mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
            // set up the content for this test
            mcu.setContent(string_input);
            mcu.setContentSize(string_input.length());

            List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
            assertNotEmpty(mdlist);
            assertEquals(1, mdlist.size());
            ArticleMetadata mdRecord = mdlist.get(0);
            assertNotNull(mdRecord);
            //log.info(mdRecord.ppString(2));

        } finally {
            IOUtil.safeClose(file_input);
        }

    }

    public void testRisNoPdfFile() throws Exception {
        InputStream file_input = null;
        try {
            file_input = getResourceAsStream(ris2);
            String string_input = StringUtil.fromInputStream(file_input);
            IOUtil.safeClose(file_input);

            mcu = mau.addUrl(risNoPdf3, true, true, risHeader);
            mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
            // set up the content for this test
            mcu.setContent(string_input);
            mcu.setContentSize(string_input.length());

            List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
            assertEmpty(mdlist);

        } finally {
            IOUtil.safeClose(file_input);
        }

    }
}



