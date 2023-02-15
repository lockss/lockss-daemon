package org.lockss.plugin.gigascience;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;

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
                GigaScienceAPIHelper = (SourceXmlSchemaHelper) new GigaScienceAPIHelper();
                log.debug3("Setup GigaScienceAPIHelper Metadata Extractor");
            }
            return GigaScienceAPIHelper;
        }
        
        @Override
        protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
                                       CachedUrl cu, ArticleMetadata thisAM) {

            log.debug3("in GigaScienceAPI  postCookProcess");
            
            thisAM.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_FILE);
            thisAM.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_FILE);

            // Add a custom map to the generic am table 
            // Allow a child to override FileType
            Map<String, String> FILE_MAP = new HashMap<String,String>();

            //default is "file"
            FILE_MAP.put("FileType", MetadataField.ARTICLE_TYPE_FILE);
            // default is base filename
            FILE_MAP.put("FileIdentifier", getFileIdentifier(cu));
            FILE_MAP.put("FileSizeBytes", getFileSize(cu));
            FILE_MAP.put("FileMime", getFileMime(cu));
            // default is no additional k-v pairs; child can add specific items
            thisAM.putRaw(MetadataField.FIELD_MD_MAP.getKey(), FILE_MAP);

        }

        protected String getFileIdentifier(CachedUrl cu) {
            return FilenameUtils.getBaseName(cu.getUrl());
        }

        protected String getFileSize(CachedUrl cu) {
            long content_size = cu.getContentSize();
            return Long.toString(content_size);

        }
        
        protected String getFileMime(CachedUrl cu) {
            String content_mime = cu.getContentType();
            // needed after the getContentType
            AuUtil.safeRelease(cu);
            return content_mime;

        }

    }
}
