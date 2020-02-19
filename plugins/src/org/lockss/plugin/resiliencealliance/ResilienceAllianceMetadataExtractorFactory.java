package org.lockss.plugin.resiliencealliance;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;

import java.io.IOException;

public class ResilienceAllianceMetadataExtractorFactory implements FileMetadataExtractorFactory {

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new ResilenceAllianceMetadataExtractor();
    }

    public static class ResilenceAllianceMetadataExtractor
            extends SimpleHtmlMetaTagMetadataExtractor {
        private static MultiMap tagMap = new MultiValueMap();
        static {
            tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
            tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
            tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
            tagMap.put("citation_journal_title", MetadataField. FIELD_PUBLICATION_TITLE);
            tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
            tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);

            tagMap.put("dc.identifier", MetadataField.DC_FIELD_IDENTIFIER);
            tagMap.put("dc.publisher", MetadataField.DC_FIELD_PUBLISHER);
            tagMap.put("dc.language", MetadataField.DC_FIELD_LANGUAGE);
            tagMap.put("dc.title", MetadataField.DC_FIELD_TITLE);
            tagMap.put("dc.date", MetadataField.DC_FIELD_DATE);
            tagMap.put("dc.source", MetadataField.DC_FIELD_SOURCE);
            tagMap.put("dc.creator", MetadataField.DC_KEY_CREATOR);
        }

        @Override
        public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
                throws IOException {
            ArticleMetadata am = super.extract(target, cu);
            am.cook(tagMap);
            return am;
        }
    }
}

