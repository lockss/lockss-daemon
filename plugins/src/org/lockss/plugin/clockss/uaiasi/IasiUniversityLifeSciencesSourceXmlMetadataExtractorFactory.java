package org.lockss.plugin.clockss.uaiasi;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FilenameUtils;

public class IasiUniversityLifeSciencesSourceXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {

    private static final Logger log = Logger.getLogger(IasiUniversityLifeSciencesSourceXmlMetadataExtractorFactory.class);

    private static SourceXmlSchemaHelper schemaHelper = null;

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new IasiUniversityLifeSciencesSourceXmlMetadataExtractor();
    }

    public static class IasiUniversityLifeSciencesSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
            // Once you have it, just keep returning the same one. It won't change.
            if ( schemaHelper != null) {
                return  schemaHelper;
            }
            schemaHelper = new IasiUniversityLifeSciencesSourceXmlSchemaHelper();

            log.debug3("Setup IasiUniversityLifeSciencesSourceXmlSchemaHelper");

            return  schemaHelper;
        }

        /* In this case, build up the filename from just the isbn13 value of the AM
         * with suffix either .pdf or .epub
         */
        @Override
        protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
                                                                ArticleMetadata oneAM) {


            String cuBase = FilenameUtils.getFullPath(cu.getUrl());
            List<String> returnList = new ArrayList<>();

            //https://clockss-test.lockss.org/sourcefiles/uaiasi-released/2023_01/2021/1/2021-1/JALSE-21-01.pdf
            //internal-pdf://2022-4/ALSE4-2022-05.pdf
            //cuBase = https://clockss-test.lockss.org/sourcefiles/uaiasi-released/2023_01/2021/1/, pdfPath = internal-pdf://2021-1/JALSE1-21-10-1.pdf

            String pdf_path = oneAM.getRaw(schemaHelper.getFilenameXPathKey());

            if (pdf_path != null) {
                pdf_path = cuBase + pdf_path.substring(pdf_path.lastIndexOf("//") + 1);
            }

            log.debug3("cuBase = " + cuBase + ", pdfPath = " + pdf_path);

            returnList.add(pdf_path);

            return returnList;

        }

        @Override
        protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
                                       CachedUrl cu, ArticleMetadata thisAM) {

            String PUBLISHER_NAME = "Iasi University of Life Sciences";


            String rawISSN = thisAM.getRaw(schemaHelper.getConsolidationXPathKey());

            if (rawISSN != null) {
                log.debug3("rawISSN = " + rawISSN);
                if (rawISSN.contains(",")) {
                    thisAM.put(MetadataField.FIELD_EISSN, rawISSN.substring(0, rawISSN.indexOf(",")).trim());
                    thisAM.put(MetadataField.FIELD_ISSN, rawISSN.substring(rawISSN.indexOf(",") +  1).trim());
                } else {
                    thisAM.put(MetadataField.FIELD_ISSN, rawISSN.substring(0, rawISSN.indexOf(",")).trim());
                }
            }

            thisAM.put(MetadataField.FIELD_PUBLISHER,PUBLISHER_NAME);
            thisAM.put(MetadataField.FIELD_PUBLICATION_TYPE,MetadataField.PUBLICATION_TYPE_JOURNAL);
            thisAM.put(MetadataField.FIELD_ARTICLE_TYPE,MetadataField.ARTICLE_TYPE_JOURNALARTICLE);
        }

    }
}

