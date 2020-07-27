package org.lockss.plugin.clockss.apma;

import org.lockss.daemon.PluginException;
import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.*;
import org.lockss.util.Logger;
import org.w3c.dom.Document;

public class AmericanPodiatricMedicalAssociationXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {

    static Logger log = Logger.getLogger(AmericanPodiatricMedicalAssociationXmlMetadataExtractorFactory.class);

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
                JatsPublishingHelper = new AmericanPodiatricMedicalAssociationSchemaHelper();
            }
            return JatsPublishingHelper;
        }
    }
}