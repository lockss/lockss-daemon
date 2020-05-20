package org.lockss.plugin.clockss.eastview;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorTest;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.CIProperties;
import org.lockss.util.Constants;
import org.lockss.util.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestEastviewMarcXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestEastviewMarcXmlMetadataExtractorFactory.class);


    private static String BaseUrl = "http://source.host.org/sourcefiles/easeview/";
    private static String Directory = "2019";
    private static String pdfUrl1 = BaseUrl + Directory + "/1275770BO.pdf";
    private static String pdfUrl2 = BaseUrl + Directory + "/1275773BO.pdf";

    private static String getXmlFileContent(String fname) {
        String xmlContent = "";
        try {
            String currentDirectory = System.getProperty("user.dir");
            String pathname = currentDirectory +
                    "/plugins/test/src/org/lockss/plugin/clockss/eastview/" + fname;
            xmlContent = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return xmlContent;
    }

    public void testExtractArticleXmlSchema() throws Exception {
        assertNotNull("placeholder");
    }

    /*
    
    public void testExtractArticleXmlSchema() throws Exception {

        String fname = "TestEastviewMarcSample.xml";
        String journalXml = getXmlFileContent(fname);
        assertNotNull(journalXml);

        String xml_url = BaseUrl + Directory + "/" + fname;

        CIProperties xmlHeader = new CIProperties();
        xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
        MockArchivalUnit mau = new MockArchivalUnit();

        MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
        mcu.setContent(journalXml);
        mcu.setContentSize(journalXml.length());
        mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

        // Now add two PDF files so they can be "found"
        // these aren't opened, so it doens't matter that they have the wrong header type
        mau.addUrl(pdfUrl1, true, true, xmlHeader);
        mau.addUrl(pdfUrl2, true, true, xmlHeader);


        FileMetadataExtractor me =  new MyEastViewMarcXmlMetadataExtractorFactory().
                createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
        assertNotEmpty(mdlist);
        assertEquals(1, mdlist.size());
        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);
        assertEquals("10.1400/64564", md.get(MetadataField.FIELD_DOI));
        assertEquals("Romano, Cesare.", md.get(MetadataField.FIELD_AUTHOR));
        assertEquals("37", md.get(MetadataField.FIELD_END_PAGE));
        assertEquals("1", md.get(MetadataField.FIELD_START_PAGE));
        assertEquals("Il piccolo Hans e la fobia del professor Freud.", md.get(MetadataField.FIELD_PUBLICATION_TITLE));
        assertEquals("Franco Angeli,", md.get(MetadataField.FIELD_PUBLISHER));
        assertEquals("2000.", md.get(MetadataField.FIELD_DATE));
    }

    private class MyEastViewMarcXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {

        private SourceXmlSchemaHelper EastViewHelper = null;

        @Override
        public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                                 String contentType)
                throws PluginException {
            return new EastViewMarcXmlMetadataExtractor();
        }

        public class EastViewMarcXmlMetadataExtractor extends SourceXmlMetadataExtractor {

            @Override
            protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
                if (EastViewHelper == null) {
                    EastViewHelper = new EastviewMarcXmlSchemaHelper();
                }
                return EastViewHelper;
            }

            // It is not clear which one can be used as "volume" of the PDF file, we use the above "4"
            // we also assume it is single digit number between 1-9
            @Override
            protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper,
                                                                    CachedUrl cu,
                                                                    ArticleMetadata oneAM) {

                ArrayList<String> returnList = new ArrayList<String>();

                String fileNum = oneAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_pdf);
                String cuBase = FilenameUtils.getFullPath(cu.getUrl());

                String pdfFilePath = cuBase + fileNum + ".pdf";
                returnList.add(pdfFilePath);

                return returnList;
            }

            @Override
            protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
                                           CachedUrl cu, ArticleMetadata thisAM) {


                if (thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_author) != null) {
                    String author = thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_author);
                    thisAM.put(MetadataField.FIELD_AUTHOR, author.replace(".", ""));
                }
            }
        }
    }*/
}

