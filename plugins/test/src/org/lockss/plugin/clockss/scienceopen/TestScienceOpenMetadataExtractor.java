package org.lockss.plugin.clockss.scienceopen;

import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataListExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorTest;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.w3c.dom.Document;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TestScienceOpenMetadataExtractor extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestScienceOpenMetadataExtractor.class);

    private static String BaseUrl = "http://source.host.org/sourcefiles/scienceopen/";
    private static String Directory = "2020";

    private String getXmlFileContent(String fname) {
        String xmlContent = "";
        InputStream file_input = null;

        try {
            file_input = getResourceAsStream(fname);
            xmlContent = StringUtil.fromInputStream(file_input);
            IOUtil.safeClose(file_input);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            IOUtil.safeClose(file_input);
        }
        return xmlContent;
    }

    public void testExtractArticleXmlSchema() throws Exception {

        String fname = "sample_test_jats.xml";
        String journalXml = getXmlFileContent(fname);
        assertNotNull(journalXml);

        String xml_url = BaseUrl + Directory + "/" + fname;

        FileMetadataExtractor me = new MyJatsPublishingSourceXmlMetadataExtractor();
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = extractFromContent(xml_url, "text/xml", journalXml, mle);
        assertNotEmpty(mdlist);
        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);
        assertEquals("10.14293/S2199-1006.1.SOR-LIFE.AAC0E6.v2", md.get(MetadataField.FIELD_DOI));
        assertEquals("2014", md.get(MetadataField.FIELD_DATE));
        assertEquals("10", md.get(MetadataField.FIELD_END_PAGE));
        assertEquals("1", md.get(MetadataField.FIELD_START_PAGE));
        assertEquals(null, md.get(MetadataField.FIELD_ISSN));
        assertEquals("2199-1006", md.get(MetadataField.FIELD_EISSN));
        assertEquals("0", md.get(MetadataField.FIELD_VOLUME));
        assertEquals("0", md.get(MetadataField.FIELD_ISSUE));
        assertEquals("Ellina, Maria-Ioanna", md.get(MetadataField.FIELD_AUTHOR));
        assertEquals("ScienceOpen Research", md.get(MetadataField.FIELD_PUBLICATION_TITLE));
        assertEquals("Epidermal growth factor/epidermal growth factor receptor signaling axis is a significant regulator of the proteasome expression and activity in colon cancer cells", md.get(MetadataField.FIELD_ARTICLE_TITLE));

    }

    public void testExtractArticleXmlSchemaFromNonStandardJATS() throws Exception {

        String fname = "sample_test_non_standard_jats.xml";
        String journalXml = getXmlFileContent(fname);
        assertNotNull(journalXml);

        String xml_url = BaseUrl + Directory + "/" + fname;

        FileMetadataExtractor me = new MyJatsPublishingSourceXmlMetadataExtractor();
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = extractFromContent(xml_url, "text/xml", journalXml, mle);
        assertNotEmpty(mdlist);
        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);
        assertEquals("10.14236/ewic/EVAC18.26", md.get(MetadataField.FIELD_DOI));
        assertEquals("2018", md.get(MetadataField.FIELD_DATE));
        assertEquals("8", md.get(MetadataField.FIELD_END_PAGE));
        assertEquals("1", md.get(MetadataField.FIELD_START_PAGE));
        //assertEquals("1477-9358", md.get(MetadataField.FIELD_ISSN));   handled in post cook process
        assertEquals(null, md.get(MetadataField.FIELD_EISSN));
        //assertEquals("BCS Learning & Development", md.get(MetadataField.FIELD_PUBLICATION_TITLE));     handled in post cook process


    }

    private class MyJatsPublishingSourceXmlMetadataExtractor extends SourceXmlMetadataExtractorFactory.SourceXmlMetadataExtractor {

        private SourceXmlSchemaHelper JatsPublishingHelper = null;

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
            throw new ShouldNotHappenException("This version of the schema setup cannot be used for this plugin");
        }

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu, Document xmlDoc) {
            if (JatsPublishingHelper == null) {
                JatsPublishingHelper = new ScienceOpenSchemaHelper();
            }
            return JatsPublishingHelper;
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

