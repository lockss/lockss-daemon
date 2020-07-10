package org.lockss.plugin.clockss.peercommunityin;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;


public class PeerCommunityInXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
    private static final Logger log = Logger.getLogger(PeerCommunityInXmlMetadataExtractorFactory.class);

    private static SourceXmlSchemaHelper CrossRefHelper = null;

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new PeerCommunityXmlMetadataExtractor();
    }

    public class PeerCommunityXmlMetadataExtractor extends SourceXmlMetadataExtractor {

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
            // Once you have it, just keep returning the same one. It won't change.
            if (CrossRefHelper != null) {
                return CrossRefHelper;
            }
            CrossRefHelper = new PeerCommunityInCrossRefQuerySchemaHelper();
            return CrossRefHelper;
        }
    }
}

