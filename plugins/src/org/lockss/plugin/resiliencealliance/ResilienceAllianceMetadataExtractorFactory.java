package org.lockss.plugin.resiliencealliance;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;

public class ResilienceAllianceMetadataExtractorFactory implements FileMetadataExtractorFactory {

    private static final Logger log = Logger.getLogger(ResilienceAllianceMetadataExtractorFactory.class);

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new ResilenceAllianceMetadataExtractor();
    }

    public static class ResilenceAllianceMetadataExtractor
            extends SimpleHtmlMetaTagMetadataExtractor {
        /*
        <meta name="dc.publisher" content="The Resilience Alliance" />
        <meta name="dc.identifier" content="doi:10.5751/ACE-01293-140101" />
        <meta name="dc.language" scheme="RFC1766" content="en" />
        <meta name="dc.title" lang="en" content="What land use better preserves taxonomic and functional diversity of birds in a grassland biome?" />
        <meta name="dc.date" scheme="W3CDTF" content="2019-02-13" />
        <meta name="dc.source" content="Avian Conservation and Ecology, Published online: Feb 13, 2019  | doi:10.5751/ACE-01293-140101" />
        <meta name="dc.rights" content="&#169; 2019 by the author(s)" />
        <meta name="dc.creator" content="Anahí Vaccaro" />
        <meta name="dc.creator" content="Julieta Filloy" />
        <meta name="dc.creator" content="M. Bellocq" />

        <meta name="citaton_title" content="What land use better preserves taxonomic and functional diversity of birds in a grassland biome?" />
        <meta name="citation_doi" content="doi:10.5751/ACE-01293-140101" />
        <meta name="citation_publication_date" content="2019/02/13" />
        <meta name="citation_issn" content="1712-6568" />
        <meta name="citation_journal_title" content="Avian Conservation and Ecology" />
        <meta name="citation_volume" content="14" />
        <meta name="citation_issue" content="1" />
        <meta name="citation_pdf_url" content="http://www.ace-eco.org/vol14/iss1/art1/ACE-ECO-2018-1293.pdf" />
        <meta name="citation_author" content="Vaccaro, Anahí" />
        <meta name="citation_author" content="Filloy, Julieta" />
        <meta name="citation_author" content="Bellocq, M." />
         */
        private static MultiMap tagMap = new MultiValueMap();

        static {
            tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
            tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
            tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
            tagMap.put("citation_journal_title", MetadataField. FIELD_PUBLICATION_TITLE);
            tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
            tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
            tagMap.put("citation_doi", MetadataField.FIELD_DOI);
        }

        @Override
        public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
                throws IOException {
            log.debug3("Fei: ResilienceAllianceMetadataExtractorFactory");
            ArticleMetadata am = super.extract(target, cu);
            am.cook(tagMap);
            return am;
        }
    }
}

