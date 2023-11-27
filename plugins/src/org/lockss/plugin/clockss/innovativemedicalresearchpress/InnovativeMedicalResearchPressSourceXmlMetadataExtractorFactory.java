package org.lockss.plugin.clockss.innovativemedicalresearchpress;

import org.apache.commons.io.FilenameUtils;
import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

public class InnovativeMedicalResearchPressSourceXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
    static Logger log = Logger.getLogger(InnovativeMedicalResearchPressSourceXmlMetadataExtractorFactory.class);

    private static SourceXmlSchemaHelper JatsPublishingHelper = null;

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new JatsPublishingSourceXmlMetadataExtractor();
    }

    public class JatsPublishingSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

        /*
         * This setUpSchema shouldn't be called directly
         * but for safety, just use the CU to figure out which schema to use.
         *
         */
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
            TdbAu tdbau = cu.getArchivalUnit().getTdbAu();
            String year = null;

            List<String> returnList = new ArrayList<String>();

            if (tdbau != null) {
                year = tdbau.getParam("year");

                log.debug3("InnovativeMedicalResearchPress: bucket year = " + year);

                if (year != null && (year.contains("2020") || year.contains("2021") || year.contains("2022"))) {
                    log.debug3("InnovativeMedicalResearchPress: old buckets of 2020, 2021, 2022 bucket, year = " + year);

                    //XML and PDF are located inside the same directory but using different naming convension.
                    //XML is named as 2617-5282-1-2-115.xml
                    //PDF is referenced inside each xml as an element like below
                    //<related-article related-article-type="pdf" specific-use="online">1555335526845-369563756.pdf</related-article>
                    //<related-article related-article-type="pdf" specific-use="online">1545898248855-1818078335.pdf</related-article>
                    if (url_string.indexOf(".xml") > -1) {
                        String md_url = cu.getUrl();
                        String cuBase = FilenameUtils.getFullPath(md_url);

                        String filenameValue = oneAM.getRaw(JatsPublishingSchemaHelper.JATS_article_related_pdf);

                        if (filenameValue != null) {
                            log.debug3("InnovativeMedicalResearchPress: PDF file path from old schema is : " + cuBase + filenameValue);
                            returnList.add(cuBase + filenameValue);
                        }
                    }
                } else {
                    //"2023" bucket is a mixed bucket, it has 8 files using old schema and the rest using new
                    //"new" means one-pdf-one-xml matching starting from 2023
                    String xml_url = cu.getUrl();
                    String pdf_url = xml_url.replace(".xml", ".pdf");
                    log.debug3("InnovativeMedicalResearchPress: PDF file path from new schema is : " + pdf_url);
                    returnList.add(pdf_url);
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