package org.lockss.plugin.clockss.elifesciences;

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

public class ElifeSciencesSourceXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
    static Logger log = Logger.getLogger(ElifeSciencesSourceXmlMetadataExtractorFactory.class);

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
            String url = cu.getUrl();
            log.debug3("Setup Jats schema helper for url " + url);
            if (JatsPublishingHelper == null) {
                JatsPublishingHelper = new JatsPublishingSchemaHelper();
            }
            return JatsPublishingHelper;
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
