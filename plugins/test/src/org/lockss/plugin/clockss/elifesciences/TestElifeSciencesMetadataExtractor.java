package org.lockss.plugin.clockss.elifesciences;

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

public class TestElifeSciencesMetadataExtractor extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestElifeSciencesMetadataExtractor.class);

    private static String BaseUrl = "http://source.host.org/sourcefiles/elifesciences/";
    private static String Directory = "2020";

    public void testExtractArticleXmlSchema() throws Exception {

        String fname = "sample_test_jats.xml";
        String journalXml = getResourceContent(fname);
        assertNotNull(journalXml);

        String xml_url = BaseUrl + Directory + "/" + fname;

        FileMetadataExtractor me = new MyJatsPublishingSourceXmlMetadataExtractor();
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = extractFromContent(xml_url, "text/xml", journalXml, mle);
        assertNotEmpty(mdlist);
        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);
        assertEquals("10.7554/eLife.47946", md.get(MetadataField.FIELD_DOI));
        assertEquals("2020", md.get(MetadataField.FIELD_DATE));
        assertEquals(null, md.get(MetadataField.FIELD_END_PAGE));
        assertEquals(null, md.get(MetadataField.FIELD_START_PAGE));
        assertEquals(null, md.get(MetadataField.FIELD_ISSN));
        assertEquals("2050-084X", md.get(MetadataField.FIELD_EISSN));
        assertEquals("9", md.get(MetadataField.FIELD_VOLUME));
        assertEquals(null, md.get(MetadataField.FIELD_ISSUE));
        assertEquals("Tuerkova, Alzbeta", md.get(MetadataField.FIELD_AUTHOR));
        assertEquals("Effect of helical kink in antimicrobial peptides on membrane pore formation", md.get(MetadataField.FIELD_ARTICLE_TITLE));

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
                JatsPublishingHelper = new JatsPublishingSchemaHelper();
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

