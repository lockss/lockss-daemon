package org.lockss.plugin.gigascience;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;

public class GigaScienceAPIXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
    private static final Logger log = Logger.getLogger(GigaScienceAPIXmlMetadataExtractorFactory.class);

    private static SourceXmlSchemaHelper GigaScienceAPIHelper = null;

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new GigaScienceAPIHtmlMetadataExtractor();
    }

    public class GigaScienceAPIHtmlMetadataExtractor extends SourceXmlMetadataExtractor {

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
            if (GigaScienceAPIHelper== null) {
                GigaScienceAPIHelper = new GigaScienceAPIHelper();
                log.debug3("Setup GigaScienceAPIHelper Metadata Extractor");
            }
            return GigaScienceAPIHelper;
        }
        
        @Override
        protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
                                       CachedUrl cu, ArticleMetadata thisAM) {

            log.debug3("in GigaScienceAPI  postCookProcess");

        }
    }
}
