package org.lockss.plugin.clockss.casalini;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorTest;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestCasaliniLibriMarcXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestCasaliniLibriMarcXmlMetadataExtractorFactory.class);

    private static String BaseUrl = "http://source.host.org/sourcefiles/casalini/";
    private static String Directory = "2019";
    private static String pdfUrl1 = BaseUrl + Directory + "/2000_4_2194804.pdf";
    private static String pdfUrl2 = BaseUrl + Directory + "/2002_4_2194812.pdf";

    private static String getXmlFileContent(String fname) {
        String xmlContent = "";
        try {
            String currentDirectory = System.getProperty("user.dir");
            String pathname = currentDirectory +
                    "/plugins/test/src/org/lockss/plugin/clockss/casalini/" + fname;
            xmlContent = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return xmlContent;
    }


    public void testExtractArticleXmlSchema() throws Exception {

        String fname = "Marc21SampleArticle.xml";
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


        FileMetadataExtractor me =  new MyCasaliniLibriMarcXmlMetadataExtractorFactory().
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
        assertEquals("Psicoterapia e scienze umane", md.get(MetadataField.FIELD_PUBLICATION_TITLE));
        assertEquals("Casalini Libri", md.get(MetadataField.FIELD_PUBLISHER));
        assertEquals("2000", md.get(MetadataField.FIELD_DATE));
    }

    private class MyCasaliniLibriMarcXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {

        private SourceXmlSchemaHelper CasaliniHelper = null;

        @Override
        public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                                 String contentType)
                throws PluginException {
            return new CasaliniMarcXmlMetadataExtractor();
        }

        public class CasaliniMarcXmlMetadataExtractor extends SourceXmlMetadataExtractor {

            @Override
            protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
                if (CasaliniHelper == null) {
                    CasaliniHelper = new CasaliniMarcXmlSchemaHelper();
                }
                return CasaliniHelper;
            }

            // It is not clear which one can be used as "volume" of the PDF file, we use the above "4"
            // we also assume it is single digit number between 1-9
            @Override
            protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper,
                                                                    CachedUrl cu,
                                                                    ArticleMetadata oneAM) {

                String yearNum = "";
                String pdfFilePath = "";
                String volumeNum = "1";
                ArrayList<String> returnList = new ArrayList<String>();
                Boolean volumeNumFound = false;

                if (oneAM.getRaw(CasaliniMarcXmlSchemaHelper.PDF_FILE_YEAR) != null) {
                    yearNum = oneAM.getRaw(CasaliniMarcXmlSchemaHelper.PDF_FILE_YEAR).replace(".", "");
                } else {
                    log.debug3("yearNum is empty");
                }

                String fileNum = oneAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_file);
                String cuBase = FilenameUtils.getFullPath(cu.getUrl());

                if (oneAM.getRaw(CasaliniMarcXmlSchemaHelper.PDF_FILE_VOLUME) != null) {
                    String volumeString = oneAM.getRaw(CasaliniMarcXmlSchemaHelper.PDF_FILE_VOLUME);
                    int lastComma = volumeString.lastIndexOf(",");
                    if (lastComma > -1) {
                        volumeNum = volumeString.substring((lastComma - 1), lastComma);
                        volumeNumFound = true;
                        pdfFilePath = cuBase + yearNum + "_" + volumeNum + "_" + fileNum + ".pdf";
                        returnList.add(pdfFilePath);
                        log.debug3("Found volume number, building PDF file with  filename - " + fileNum + ", volume - " + volumeNum + ", year - " + yearNum);
                    }
                }
                if (!volumeNumFound) {
                    ArrayList<Integer> volumes = new ArrayList<>();
                    volumes.add(1);
                    volumes.add(2);
                    volumes.add(3);
                    volumes.add(4);

                    for (int volume : volumes) {
                        pdfFilePath = cuBase + yearNum + "_" + volume + "_" + fileNum + ".pdf";
                        returnList.add(pdfFilePath);
                        log.debug3("Could not find volume number from xml, building PDF file with filename - " + fileNum + ", with possible guessed volume - " + volumeNum + ", year - " + yearNum);
                    }
                }

                return returnList;
            }

            @Override
            protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
                                           CachedUrl cu, ArticleMetadata thisAM) {


                if (thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_author) != null) {
                    String author = thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_author);
                    thisAM.put(MetadataField.FIELD_AUTHOR, author.replace(".", ""));
                }

                if (thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_pub_date) != null) {
                    String MARC_pub_date = thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_pub_date);
                    thisAM.put(MetadataField.FIELD_DATE, MARC_pub_date.replace(".", ""));
                }

                if (thisAM.getRaw(CasaliniMarcXmlSchemaHelper.PUBLICATION_TITLE) != null) {
                    String MARC_publication_title = thisAM.getRaw(CasaliniMarcXmlSchemaHelper.PUBLICATION_TITLE);

                    if (MARC_publication_title.indexOf(".") > -1) {
                        thisAM.put(MetadataField.FIELD_PUBLICATION_TITLE, MARC_publication_title.substring(0, MARC_publication_title.indexOf(".")));
                    } else {
                        thisAM.put(MetadataField.FIELD_PUBLICATION_TITLE, MARC_publication_title);
                    }


                    if (thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_title) != null) {
                        String MARC_title1 = thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_title);
                        String MARC_title2 = thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_title2);

                        String MARC_title = MARC_title1;

                        if (MARC_title2 != null) {
                            MARC_title = MARC_title1 +  MARC_title2;
                            thisAM.put(MetadataField.FIELD_ARTICLE_TITLE, MARC_title);
                        }
                        thisAM.put(MetadataField.FIELD_ARTICLE_TITLE, MARC_title);
                    }
                }

                if (thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_doi) != null) {
                    thisAM.put(MetadataField.FIELD_DOI, thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_doi));
                }

                String publisherName = "Casalini Libri";

                TdbAu tdbau = cu.getArchivalUnit().getTdbAu();
                if (tdbau != null) {
                    publisherName =  tdbau.getPublisherName();
                }

                thisAM.put(MetadataField.FIELD_PUBLISHER, publisherName);
            }
        }
    }
}

