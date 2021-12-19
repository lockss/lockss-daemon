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
import org.lockss.util.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TestEastviewMarcXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestEastviewMarcXmlMetadataExtractorFactory.class);


    private static String BaseUrl = "http://source.host.org/sourcefiles/easeview/";
    private static String Directory = "2019";
    private static String pdfUrl1 = BaseUrl + Directory + "/1275770BO.pdf";
    private static String pdfUrl2 = BaseUrl + Directory + "/1275773BO.pdf";

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
        assertEquals("author name", md.get(MetadataField.FIELD_AUTHOR));
        assertEquals("Soedinennye Shtaty Ameriki ", md.get(MetadataField.FIELD_PUBLICATION_TITLE));
        assertEquals("publisher name", md.get(MetadataField.FIELD_PUBLISHER));
        assertEquals("2018", md.get(MetadataField.FIELD_DATE));
        assertEquals("9789850822628", md.get(MetadataField.FIELD_ISBN));

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
                    thisAM.put(MetadataField.FIELD_AUTHOR, author.replace(":", ""));
                }

                if (thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_pub_date) != null) {
                    String pub_date = thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_pub_date);
                    thisAM.put(MetadataField.FIELD_DATE, pub_date.replace(".", ""));
                }

                if (thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_publisher) != null) {
                    String publisher = thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_publisher);
                    thisAM.put(MetadataField.FIELD_PUBLISHER, publisher.replace(":", ""));
                }

                if (thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_title) != null) {
                    String title = thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_title);
                    thisAM.put(MetadataField.FIELD_PUBLICATION_TITLE, title.replace(":", ""));
                }
            }
        }
    }
}

