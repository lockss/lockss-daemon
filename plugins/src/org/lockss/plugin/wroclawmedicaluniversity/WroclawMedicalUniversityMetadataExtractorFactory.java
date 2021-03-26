package org.lockss.plugin.wroclawmedicaluniversity;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.HttpHttpsUrlHelper;

import java.io.IOException;

public class WroclawMedicalUniversityMetadataExtractorFactory implements FileMetadataExtractorFactory {

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new WroclawMedicalUniversityHtmlMetadataExtractor();
    }

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
            // Http to Https conversion stuff
            HttpHttpsUrlHelper helper = new HttpHttpsUrlHelper(cu.getArchivalUnit(),
                ConfigParamDescr.BASE_URL.getKey(),
                "base_url");
            String url = am.get(MetadataField.FIELD_ACCESS_URL);
            if (url != null) {
                url = helper.normalize(url);
                am.replace(MetadataField.FIELD_ACCESS_URL, url);
            }
            //
            return am;
        }
    }
}

