package org.lockss.plugin.clockss.innovativepublication;

import org.lockss.daemon.PluginException;
import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
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

public class TestInnovativePublicationJatsXmlXPathMetadataExtractor extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestInnovativePublicationJatsXmlXPathMetadataExtractor.class);

    private static String BaseUrl = "http://source.host.org/sourcefiles/innovativepublication/";
    private static String Directory = "2019_04";

    public void testExtractArticleXmlSchema() throws Exception {

        String fname = "sample_jats_metadata.xml";
        String journalXml = getResourceContent(fname);

        String xml_url = BaseUrl + Directory + "/" + fname;

        FileMetadataExtractor me = new MyInnovativePublicationJatsXmlXPathMetadataExtractory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");

        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = extractFromContent(xml_url, "text/xml", journalXml, mle);
        assertNotEmpty(mdlist);
        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);
        assertEquals("2019", md.get(MetadataField.FIELD_DATE));
        assertEquals("2394-6792", md.get(MetadataField.FIELD_ISSN));
        assertEquals("6", md.get(MetadataField.FIELD_VOLUME));
        assertEquals("4", md.get(MetadataField.FIELD_ISSUE));
        assertEquals("Butale, Pradip", md.get(MetadataField.FIELD_AUTHOR));
        assertEquals("Lipid profile in arthritides", md.get(MetadataField.FIELD_ARTICLE_TITLE));

    }

    private class MyInnovativePublicationJatsXmlXPathMetadataExtractory extends SourceXmlMetadataExtractorFactory {

        private  SourceXmlSchemaHelper InnovativeJatsXmlHelper = null;

        @Override
        public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                                 String contentType)
                throws PluginException {
            return new InnovativePublicationMetadataExtractor();
        }

        public class InnovativePublicationMetadataExtractor extends SourceXmlMetadataExtractor {

            @Override
            protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
                throw new ShouldNotHappenException("This version of the schema setup cannot be used for this plugin");
            }

            @Override
            protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu, Document doc) {

                if (InnovativeJatsXmlHelper == null) {
                    InnovativeJatsXmlHelper = new JatsPublishingSchemaHelper();
                }
                return InnovativeJatsXmlHelper;
            }

            @Override
            protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
                                                                    ArticleMetadata oneAM) {

                String pdfPath = "";
                String url_string = cu.getUrl();
                List<String> returnList = new ArrayList<String>();
                //XML and PDF are located inside the same directory in most cases
                //http://content5.lockss.org/sourcefiles/innovativepublication-released/2019/IJCA/2019/volume%206/issue%204/IJCA%206-4-478-480.pdf
                //http://content5.lockss.org/sourcefiles/innovativepublication-released/2019/IJCA/2019/volume%206/issue%204/IJCA%206-4-481-487.xml
                if (url_string.indexOf(".xml") > -1) {
                    pdfPath = url_string.replace(".xml", ".pdf");
                    ArchivalUnit B_au = cu.getArchivalUnit();
                    CachedUrl fileCu;
                    fileCu = B_au.makeCachedUrl(pdfPath);
                    log.debug3("Check for existence of " + pdfPath);
                    if(fileCu != null && (fileCu.hasContent())) {
                        log.debug3("pdfPath is " + pdfPath);
                        returnList.add(pdfPath);
                    } else {
                        log.debug3("no matching PDF found, use xml file instead " + pdfPath);
                        returnList.add(url_string);
                    }
                }
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
}

