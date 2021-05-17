package org.lockss.plugin.springer;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.io.FileUtils;
import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.*;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.w3c.dom.Document;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class TestSpringerJatsXmlXPathMetadataExtractor extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestSpringerJatsXmlXPathMetadataExtractor.class);

    private static final Pattern schemaAPATTERN = Pattern.compile("[^/]+\\.xml(\\.Meta)?$");

    private static String BaseUrl = "http://source.host.org/sourcefiles/springer/";
    private static String Directory = "2019_04";

    private static String getXmlFileContent(String fname) {
        String xmlContent = "";
        try {
            String currentDirectory = System.getProperty("user.dir");
            String pathname = currentDirectory +
                    "/plugins/test/src/org/lockss/plugin/springer/" + fname;
            xmlContent = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
        } catch(IOException e) {
            log.error(e.getMessage(), e);
        }
        return xmlContent;
    }

    public void testExtractArticleXmlSchema() throws Exception {

        String fname = "springer_source_plugin_jats_journal_test_file.xml.Meta";
        String journalXml = getXmlFileContent(fname);
        String xml_url = BaseUrl + Directory + "/" + fname;

        FileMetadataExtractor me = new TestSpringerJatsXmlXPathMetadataExtractor.MyJatsPublishingSourceXmlMetadataExtractor();
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = extractFromContent(xml_url, "text/xml", journalXml, mle);
        assertNotEmpty(mdlist);
        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);
        assertEquals("10.1038/s41371-019-0217-8", md.get(MetadataField.FIELD_DOI));
        assertEquals("2019", md.get(MetadataField.FIELD_DATE));
        assertEquals("29", md.get(MetadataField.FIELD_END_PAGE));
        assertEquals("1", md.get(MetadataField.FIELD_START_PAGE));
        assertEquals("Abstracts from the 2019 Annual Scientific Meeting of the British and Irish Hypertension Society (BIHS)", md.get(MetadataField.FIELD_ARTICLE_TITLE));

    }

    public void testExtractArticleXmlMetaSchema() throws Exception {

        String fname = "springer_source_plugin_jats_journal_test_file.xml";
        String journalXml = getXmlFileContent(fname);
        String xml_url = BaseUrl + Directory + "/" + fname;

        FileMetadataExtractor me = new TestSpringerJatsXmlXPathMetadataExtractor.MyJatsPublishingSourceXmlMetadataExtractor();
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = extractFromContent(xml_url, "text/xml", journalXml, mle);
        assertNotEmpty(mdlist);
        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);
        assertEquals("10.1038/s41371-019-0217-8", md.get(MetadataField.FIELD_DOI));
        assertEquals("2019", md.get(MetadataField.FIELD_DATE));
        assertEquals("29", md.get(MetadataField.FIELD_END_PAGE));
        assertEquals("1", md.get(MetadataField.FIELD_START_PAGE));
        assertEquals("0950-9240", md.get(MetadataField.FIELD_ISSN));
        assertEquals("1476-5527", md.get(MetadataField.FIELD_EISSN));
        assertEquals("33", md.get(MetadataField.FIELD_VOLUME));
        assertEquals("Suppl 1", md.get(MetadataField.FIELD_ISSUE));
        assertEquals("Abstracts from the 2019 Annual Scientific Meeting of the British and Irish Hypertension Society (BIHS)", md.get(MetadataField.FIELD_ARTICLE_TITLE));

    }

    public void testExtractBookXmlSchema() throws Exception {

        String fname = "springer_source_plugin_bits_book_test_file.xml";
        String journalXml = getXmlFileContent(fname);
        String xml_url = BaseUrl + Directory + "/" + fname;

        FileMetadataExtractor me = new TestSpringerJatsXmlXPathMetadataExtractor.MyJatsPublishingSourceXmlMetadataExtractor();
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = extractFromContent(xml_url, "text/xml", journalXml, mle);
        assertNotEmpty(mdlist);
        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);

        assertEquals("Stahlbetonerhaltung", md.get(MetadataField.FIELD_PUBLICATION_TITLE));
        assertEquals("10.1007/978-3-658-27709-3", md.get(MetadataField.FIELD_DOI));
        assertEquals("978-3-658-27708-6", md.get(MetadataField.FIELD_ISBN));
        assertEquals("978-3-658-27709-3", md.get(MetadataField.FIELD_EISBN));
        assertEquals("Wietek, Bernhard", md.get(MetadataField.FIELD_AUTHOR));
        assertEquals("2019", md.get(MetadataField.FIELD_DATE));

    }

    public void testExtractBookSerieXmlSchema() throws Exception {
        String fname = "springer_source_plugin_bits_book_serie_test_file.xml";
        String journalXml = getXmlFileContent(fname);

        String xml_url = BaseUrl + Directory + "/" + fname;
        FileMetadataExtractor me = new TestSpringerJatsXmlXPathMetadataExtractor.MyJatsPublishingSourceXmlMetadataExtractor();
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = extractFromContent(xml_url, "text/xml", journalXml, mle);
        assertNotEmpty(mdlist);
        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);

        assertEquals("Springer New York", md.get(MetadataField.FIELD_PUBLISHER));
        assertEquals("Steroid Receptors", md.get(MetadataField.FIELD_PUBLICATION_TITLE));
        assertEquals("10.1007/978-1-4939-1346-6", md.get(MetadataField.FIELD_DOI));
        assertEquals("978-1-4939-1345-9", md.get(MetadataField.FIELD_ISBN));
        assertEquals("978-1-4939-1346-6", md.get(MetadataField.FIELD_EISBN));
        assertEquals("Castoria, Gabriella", md.get(MetadataField.FIELD_AUTHOR));
        assertEquals("2014", md.get(MetadataField.FIELD_DATE));

    }

    // Set up a test version of a source extractor in order to define/control
    // a basic schema for testing
    private  class MyJatsPublishingSourceXmlMetadataExtractor extends SourceXmlMetadataExtractorFactory.SourceXmlMetadataExtractor {

        Logger log = Logger.getLogger(MyJatsPublishingSourceXmlMetadataExtractor.class);

        private SourceXmlSchemaHelper JatsPublishingHelper = null;
        private  SourceXmlSchemaHelper BitsPublishingHelper = null;

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
            throw new ShouldNotHappenException("This version of the schema setup cannot be used for this plugin");
        }

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu, Document xmlDoc) {
            String url = cu.getUrl();
            // book is using BITS format
            if ((url != null) && url.indexOf("book") > -1) {
                if (BitsPublishingHelper == null) {
                    BitsPublishingHelper = new SpringerBitsPublishingSchemaHelper();
                }
                return BitsPublishingHelper;
            } else {
                // journal  material is using JATS format
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
