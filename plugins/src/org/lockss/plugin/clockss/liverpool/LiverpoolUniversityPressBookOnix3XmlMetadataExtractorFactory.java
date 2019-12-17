package org.lockss.plugin.clockss.liverpool;

import org.apache.commons.io.FilenameUtils;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.Onix3BooksSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

import java.util.ArrayList;
import java.util.List;

public class LiverpoolUniversityPressBookOnix3XmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
    private static final Logger log = Logger.getLogger(LiverpoolUniversityPressBookOnix3XmlMetadataExtractorFactory.class);

    private static SourceXmlSchemaHelper Onix3Helper = null;

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new LiverpoolOnix3XmlMetadataExtractor();
    }

    public class LiverpoolOnix3XmlMetadataExtractor extends SourceXmlMetadataExtractor {

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
            if (Onix3Helper == null) {
                Onix3Helper = new Onix3BooksSchemaHelper();
            }
            return Onix3Helper;
        }

        /*
         * (non-Javadoc)
         * Do not verify the existence of content. Just emit for each article/book
         * defined in the XML
         */
        @Override
        protected boolean preEmitCheck(SourceXmlSchemaHelper schemaHelper,
                                       CachedUrl cu, ArticleMetadata thisAM) {
            return true;
        }

        /*
         * (non-Javadoc)
         * WARC XML files are a little non-standard in that they store the actual access.url
         * location in the "self-uri" field for Jats and the proprietary ID field
         * for ONIX
         * set the access_url depending on the schema
         * set the publisher as well. It may get replaced by the TDB value
         */
        @Override
        protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
                                       CachedUrl cu, ArticleMetadata thisAM) {

            String access = thisAM.getRaw(Onix3BooksSchemaHelper.ONIX_idtype_proprietary);
            if (access != null) {
                thisAM.replace(MetadataField.FIELD_ACCESS_URL, access);
            }
            if (thisAM.get(MetadataField.FIELD_DATE)== null) {
                String copydate = thisAM.getRaw(Onix3BooksSchemaHelper.ONIX_copy_date);
                if (copydate != null) {
                    thisAM.put(MetadataField.FIELD_DATE,copydate);
                }
            }
        }

        /* In this case, use the RecordReference + .pdf for the matching file */
        @Override
        protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
                                                                ArticleMetadata oneAM) {


            String filenameValue = oneAM.getRaw(Onix3BooksSchemaHelper.ONIX_RR);
            String cuBase = FilenameUtils.getFullPath(cu.getUrl());
            String fullPathFile = UrlUtil.minimallyEncodeUrl(cuBase + filenameValue + ".pdf");
            List<String> returnList = new ArrayList<String>();
            returnList.add(fullPathFile);
            return returnList;
        }

    }
}

