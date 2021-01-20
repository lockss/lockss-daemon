package org.lockss.plugin.clockss.msptm;

import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.PubMedSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Document;

public class MSPTMMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {

    static Logger log = Logger.getLogger(MSPTMMetadataExtractorFactory.class);

    private static SourceXmlSchemaHelper PubMedHelper = null;

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new JatsPublishingSourceXmlMetadataExtractor();
    }

    public class JatsPublishingSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
            throw new ShouldNotHappenException("This version of the schema setup cannot be used for this plugin");
        }

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu, Document xmlDoc) {
            if (PubMedHelper == null) {
                PubMedHelper = new PubMedSchemaHelper();
            }
            return PubMedHelper;
        }

        @Override
        protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
                                       CachedUrl cu, ArticleMetadata thisAM) {
            
			String publisherName = "Malaysian Society of Parasitology and Tropical Medicine";

			TdbAu tdbau = cu.getArchivalUnit().getTdbAu();
			if (tdbau != null) {
				publisherName =  tdbau.getPublisherName();
			}

			thisAM.put(MetadataField.FIELD_PUBLISHER, publisherName);

        }
    }
}