package org.lockss.plugin.clockss.iniesteres;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.iniestares.IniestaresXmlMetadataExtractorFactory;
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

public class TestIniestaresXmlMetadataExtractorFactory extends LockssTestCase {

    private static final Logger log = Logger.getLogger(TestIniestaresXmlMetadataExtractorFactory.class);

    private MockLockssDaemon theDaemon;
    private MockArchivalUnit mau;

    private static String PLUGIN_NAME = "org.lockss.plugin.clockss.iniestares.ClockssIniestaresSourcePlugin";
    private static String BASE_URL = "http://www.source.org/";
    private static String DIRECTORY = "2016";
    private static String ISSUE_URL = BASE_URL + DIRECTORY + "/pdoc/pdoc_17-1/";
    private static final String xml_url = ISSUE_URL + "pdoc_17-1.xml";
    private static final String filenum1 = "luxx_2016_0019_0001_0005_0013";
    private static final String pdf1_url = ISSUE_URL +  "pdf/" + filenum1 + ".pdf";
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
     * @return
     */
    Configuration auConfig() {
        Configuration conf = ConfigManager.newConfiguration();
        conf.put("base_url", BASE_URL);
        conf.put("year", DIRECTORY);
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
            mau.addUrl(pdf1_url, true, true, xmlHeader);
            mcu.setContent(string_input);
            mcu.setContentSize(string_input.length());
            mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

            FileMetadataExtractor me = new IniestaresXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
            FileMetadataListExtractor mle =
                    new FileMetadataListExtractor(me);
            List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
            assertNotEmpty(mdlist);
            // only one of the two records had a matching pdf
            assertEquals(1, mdlist.size());

            // check each returned md against expected values
            Iterator<ArticleMetadata> mdIt = mdlist.iterator();
            ArticleMetadata mdRecord = null;
            while (mdIt.hasNext()) {
                mdRecord = (ArticleMetadata) mdIt.next();
                compareMetadata(mdRecord);
            }
        }finally {
            IOUtil.safeClose(file_input);
        }

    }

    String[] authorlist = new String[] {"Hamilton, Alexander", "Burr, Aaron"};
    // quick and dirty, there are only two records
    private void compareMetadata(ArticleMetadata AM) {
        assertEquals("publication title", AM.get(MetadataField.FIELD_PUBLICATION_TITLE));
        assertEquals("2022-5-28", AM.get(MetadataField.FIELD_DATE));
        assertEquals("publisher name", AM.get(MetadataField.FIELD_PUBLISHER));
        assertEquals("75", AM.get(MetadataField.FIELD_VOLUME));
        assertEquals("4", AM.get(MetadataField.FIELD_ISSUE));
        assertEquals("0004-0614", AM.get(MetadataField.FIELD_ISSN));
        assertEquals("10.56434/j.arch.esp.urol.20227504.44", AM.get(MetadataField.FIELD_DOI));
        assertEquals("Oncologic outcomes and predictive factors for progression in pT3a renal cell carcinoma", AM.get(MetadataField.FIELD_ARTICLE_TITLE));
    }
}

