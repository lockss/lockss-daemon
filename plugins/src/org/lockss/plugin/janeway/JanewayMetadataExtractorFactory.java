package org.lockss.plugin.janeway;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;

import java.io.IOException;

public class JanewayMetadataExtractorFactory implements FileMetadataExtractorFactory {

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new JanewayMetadataExtractor();
    }

    public static class JanewayMetadataExtractor
            extends SimpleHtmlMetaTagMetadataExtractor {
        private static MultiMap tagMap = new MultiValueMap();

        static {
            tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
            tagMap.put("citation_date", MetadataField.FIELD_DATE);
            tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
            tagMap.put("citation_journal_title", MetadataField. FIELD_PUBLICATION_TITLE);
            tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
            tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
            tagMap.put("citation_doi", MetadataField.FIELD_DOI);
            tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
            tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
            tagMap.put("citation_pdf_url", MetadataField.FIELD_ACCESS_URL);
            tagMap.put("citation_abstract_html_url", MetadataField.FIELD_ABSTRACT);
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

