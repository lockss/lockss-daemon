package org.lockss.plugin.associationforcomputingmachinery;

import org.apache.commons.io.FileUtils;
import org.lockss.daemon.PluginException;
import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorTest;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.plugin.springer.TestSpringerJatsXmlXPathMetadataExtractor;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.w3c.dom.Document;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class TestACMJatsXmlXPathMetadataExtractor extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestACMJatsXmlXPathMetadataExtractor.class);

    private static final Pattern schemaAPATTERN = Pattern.compile("[^/]+\\.xml$");

    private static String BaseUrl = "http://source.host.org/sourcefiles/springer/";
    private static String Directory = "2019_04";

    private static String getXmlFileContent(String fname) {
        String xmlContent = "";
        try {
            String currentDirectory = System.getProperty("user.dir");
            String pathname = currentDirectory +
                    "/plugins/test/src/org/lockss/plugin/associationforcomputingmachinery/" + fname;
            xmlContent = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);


        } catch(IOException e) {
            log.error(e.getMessage(), e);
        }
        return xmlContent;
    }

    public void testExtractArticleXmlSchema() throws Exception {

        String fname = "acm_source_plugin_journal_jats_test_file.xml";
        String journalXml = getXmlFileContent(fname);

        String xml_url = BaseUrl + Directory + "/" + fname;

        FileMetadataExtractor me = new TestACMJatsXmlXPathMetadataExtractor.MyACMJatsPublishingSourceXmlMetadataExtractor();
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = extractFromContent(xml_url, "text/xml", journalXml, mle);
        assertNotEmpty(mdlist);
        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);
        assertEquals("10.1145/3169128", md.get(MetadataField.FIELD_DOI));
        assertEquals("2017", md.get(MetadataField.FIELD_DATE));
        assertEquals("45", md.get(MetadataField.FIELD_END_PAGE));
        assertEquals("34", md.get(MetadataField.FIELD_START_PAGE));
        assertEquals("1072-5520", md.get(MetadataField.FIELD_ISSN));
        assertEquals("1558-3449", md.get(MetadataField.FIELD_EISSN));
        assertEquals("25", md.get(MetadataField.FIELD_VOLUME));
        assertEquals("1", md.get(MetadataField.FIELD_ISSUE));
        assertEquals("Light, Ann", md.get(MetadataField.FIELD_AUTHOR));
        assertEquals("Special Topic: Taking Action in a Changing World", md.get(MetadataField.FIELD_ARTICLE_TITLE));

    }

    public void testExtractConferenceXmlSchema() throws Exception {

        String fname = "acm_source_plugin_conferences_bits_test_file.xml";
        String journalXml = getXmlFileContent(fname);

        String xml_url = BaseUrl + Directory + "/" + fname;

        FileMetadataExtractor me = new TestACMJatsXmlXPathMetadataExtractor.MyACMJatsPublishingSourceXmlMetadataExtractor();
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = extractFromContent(xml_url, "text/xml", journalXml, mle);
        assertNotEmpty(mdlist);
        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);

        assertEquals("Proceedings of the 2019 on International Conference on Multimedia Retrieval", md.get(MetadataField.FIELD_PUBLICATION_TITLE));
        assertEquals("10.1145/3323873.3325009", md.get(MetadataField.FIELD_DOI));
        assertEquals("Strezoski, Gjorgji", md.get(MetadataField.FIELD_AUTHOR));
        assertEquals("2019", md.get(MetadataField.FIELD_DATE));
        assertEquals("ACM Conferences", md.get(MetadataField.FIELD_PUBLISHER));
        assertEquals("78", md.get( MetadataField.FIELD_START_PAGE));
        assertEquals("86", md.get(MetadataField.FIELD_END_PAGE));
    }

    private class MyACMJatsPublishingSourceXmlMetadataExtractor extends SourceXmlMetadataExtractorFactory.SourceXmlMetadataExtractor {

        private SourceXmlSchemaHelper JatsPublishingHelper = null;
        private  SourceXmlSchemaHelper BitsPublishingHelper = null;

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
            throw new ShouldNotHappenException("This version of the schema setup cannot be used for this plugin");
        }

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu, Document xmlDoc) {
            String url = cu.getUrl();
            // acm  conferences is using BITS format
            if ((url != null) && url.indexOf("conferences") > -1) {
                log.info("Setup Bits schema helper for url " + url);
                if (BitsPublishingHelper == null) {
                    BitsPublishingHelper = new ACMBitsPublishingSchemaHelper();
                }
                return BitsPublishingHelper;
            } else {
                log.info("Setup Jats schema helper for url " + url);
                // acm other material is using JATS format
                if (JatsPublishingHelper == null) {
                    JatsPublishingHelper = new JatsPublishingSchemaHelper();
                }
                return JatsPublishingHelper;
            }
        }

        @Override
        protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
                                                                ArticleMetadata oneAM) {

            String url_string = cu.getUrl();
            List<String> returnList = new ArrayList<String>();
            returnList.add(url_string);

            return returnList;
        }

        @Override
        protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
                                       CachedUrl cu, ArticleMetadata thisAM) {

            //If we didn't get a valid date value, use the copyright year if it's there
            if (thisAM.get(MetadataField.FIELD_DATE) == null) {
                if (thisAM.getRaw(JatsPublishingSchemaHelper.JATS_date) != null) {
                    thisAM.put(MetadataField.FIELD_DATE, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_date));
                } else {// last chance
                    thisAM.put(MetadataField.FIELD_DATE, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_edate));
                }
            }
        }
    }
}
