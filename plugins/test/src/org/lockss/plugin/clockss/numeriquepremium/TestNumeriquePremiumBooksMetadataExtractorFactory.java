package org.lockss.plugin.clockss.numeriquepremium;

import org.apache.commons.io.FileUtils;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorTest;
import org.lockss.plugin.clockss.aiaa.AIAAXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.aiaa.TestAIAAXmlMetadataExtractor;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

public class TestNumeriquePremiumBooksMetadataExtractorFactory extends LockssTestCase {

    private static final Logger log = Logger.getLogger(TestNumeriquePremiumBooksMetadataExtractorFactory.class);

    private MockLockssDaemon theDaemon;
    private MockArchivalUnit mau;

    private static String BASE_URL = "http://www.source.org/";
    private static final String xml_article_url = BASE_URL + "2017/PUR_24_files/9782753510180/9782753510180.xml";
    private static final String pdf_url = BASE_URL + "2017/PUR_24_files/9782753510180/9782753510180.pdf";

    public void setUp() throws Exception {
        super.setUp();
        setUpDiskSpace(); 
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
        conf.put("directory", "2017");
        return conf;
    }



    private static final String realXMLFile1 = "sample.xml";

    public void testFromArticleXMLFile() throws Exception {
        InputStream file_input = null;
        try {
            file_input = getResourceAsStream(realXMLFile1);
            String string_input = StringUtil.fromInputStream(file_input);
            IOUtil.safeClose(file_input);

            CIProperties xmlHeader = new CIProperties();
            xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
            MockCachedUrl mcu = mau.addUrl(xml_article_url, true, true, xmlHeader);
            // Now add all the pdf files in our AU since we check for them before emitting
            mau.addUrl(pdf_url, true, true, xmlHeader);

            mcu.setContent(string_input);
            mcu.setContentSize(string_input.length());
            mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

            FileMetadataExtractor me = new NumeriquePremiumBooksMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
            FileMetadataListExtractor mle =
                    new FileMetadataListExtractor(me);
            List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
            assertNotEmpty(mdlist);
            assertEquals(1, mdlist.size());

            // check each returned md against expected values
            Iterator<ArticleMetadata> mdIt = mdlist.iterator();
            ArticleMetadata md = null;
            while (mdIt.hasNext()) {
                md = (ArticleMetadata) mdIt.next();
                assertEquals("9782753510180", md.get(MetadataField.FIELD_ISBN));
                assertEquals("9782753522657", md.get(MetadataField.FIELD_EISBN));
                assertEquals("Tomic, Sacha", md.get(MetadataField.FIELD_AUTHOR));
                assertEquals("Aux origines de la chimie organique", md.get(MetadataField.FIELD_ARTICLE_TITLE));
                assertEquals("PUR", md.get(MetadataField.FIELD_PUBLISHER));
                assertEquals("01-01-2010", md.get(MetadataField.FIELD_DATE));
            }
        }finally {
            IOUtil.safeClose(file_input);
        }

    }

}