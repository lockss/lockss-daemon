package org.lockss.plugin.blackquotidianrdf;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;

import java.io.IOException;

public class BlackQuotidianMetadataExtractorFactory implements FileMetadataExtractorFactory {

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new WroclawMedicalUniversityHtmlMetadataExtractor();
    }
    /*
    <meta name="description" content="1966 NCAA Basketball Championship - Texas Western vs. Kentucky" />
    <meta property="og:title" content="Black Quotidian: 1966 NCAA Basketball Championship - Texas Western vs. Kentucky" />
    <meta property="og:site_name" content="Black Quotidian: Everyday History in African-American Newspapers" />
    <meta property="og:url" content="http://blackquotidian.supdigital.org/bq/1966-ncaa-basketball-championship---texas-western-vs-kentucky" />
    <meta property="og:description" content="1966 NCAA Basketball Championship - Texas Western vs. Kentucky" />
    <meta property="og:image" content="http://blackquotidian.supdigital.org/bq/media/CD%20-%209-8-45%20-%20first%20page%20-%20color%201%20web.png" />
    <meta property="og:type" content="article" />
     */

    public static class WroclawMedicalUniversityHtmlMetadataExtractor
            extends SimpleHtmlMetaTagMetadataExtractor {
        private static MultiMap tagMap = new MultiValueMap();
        static {
            tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
            tagMap.put("citation_date", MetadataField.FIELD_DATE);
            tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
            tagMap.put("citation_journal_title", MetadataField. FIELD_PUBLICATION_TITLE);
            tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
            tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
            tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
            tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
            tagMap.put("citation_doi", MetadataField.FIELD_DOI);
            tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
            tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
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

