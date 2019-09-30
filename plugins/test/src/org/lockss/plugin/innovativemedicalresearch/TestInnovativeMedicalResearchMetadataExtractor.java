package org.lockss.plugin.innovativemedicalresearch;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
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
import org.lockss.util.Constants;
import org.lockss.util.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.*;

public class TestInnovativeMedicalResearchMetadataExtractor extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestInnovativeMedicalResearchMetadataExtractor.class);

    private static String BaseUrl = "http://source.host.org/sourcefiles/innovative/";
    private static String Directory = "2019";

    private static String getXmlFileContent(String fname) {
        String xmlContent = "";
        try {
            String currentDirectory = System.getProperty("user.dir");
            String pathname = currentDirectory +
                    "/plugins/test/src/org/lockss/plugin/innovativemedicalresearch/" + fname;
            xmlContent = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
        } catch(IOException e) {
            e.printStackTrace();
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
        assertEquals("10.31083/j.jmcm.2018.02.012", md.get(MetadataField.FIELD_DOI));
        assertEquals("2018", md.get(MetadataField.FIELD_DATE));
        assertEquals("114", md.get(MetadataField.FIELD_END_PAGE));
        assertEquals("107", md.get(MetadataField.FIELD_START_PAGE));
        assertEquals("2617-5282", md.get(MetadataField.FIELD_ISSN));
        assertEquals(null, md.get(MetadataField.FIELD_EISSN));
        assertEquals("1", md.get(MetadataField.FIELD_VOLUME));
        assertEquals("2", md.get(MetadataField.FIELD_ISSUE));
        assertEquals("Jiriys, George Ginini", md.get(MetadataField.FIELD_AUTHOR));
        assertEquals("Low Intensity Pulsed Ultrasound for Accelerating Distraction Osteogenesis: A Systematic Review of Experimental Animal Studies", md.get(MetadataField.FIELD_ARTICLE_TITLE));
        assertEquals("1545898240238-1078607897.pdf", md.getRaw(JatsPublishingSchemaHelper.JATS_article_related_pdf));

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
