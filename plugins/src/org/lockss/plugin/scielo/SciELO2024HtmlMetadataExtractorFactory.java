/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.scielo;

import java.io.IOException;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.extractor.SimpleHtmlMetaTagMetadataExtractor;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.hindawi.Hindawi2020HtmlMetadataExtractorFactory;
import org.lockss.plugin.medknow.MedknowHtmlMetadataExtractorFactory.MedknowHtmlMetadataExtractor;
import org.lockss.util.Logger;

import dk.itst.oiosaml.sp.metadata.IdpMetadata.Metadata;

public class SciELO2024HtmlMetadataExtractorFactory implements FileMetadataExtractor {

    static Logger log = Logger.getLogger(SciELO2024HtmlMetadataExtractorFactory.class);

    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target, String contentType) throws PluginException {
        return new SciELO2024HtmlMetadataExtractor();
    }

    public static class SciELO2024HtmlMetadataExtractor 
    extends SimpleHtmlMetaTagMetadataExtractor {

    private static MultiMap tagMap = new MultiValueMap();
    
    static {
        tagMap.put("citation_author", MetadataField.FIELD_AUTHOR); //multiple authors- how is this handled? 
        //tagMap.put("citation_author_affiliation",????);
        //tagMap.put("citation_author_orcid", ?????); No available metadatafield, so will not be added
        tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
        tagMap.put("citation_journal_abbrev", MetadataField.DC_FIELD_IDENTIFIER); //check that this is correct
        tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
        tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
        tagMap.put("citation_number", MetadataField.FIELD_ITEM_NUMBER); //chcek that this is correct
        tagMap.put("citation_pdf_url", MetadataField.FIELD_ACCESS_URL); //do I use both pdf and xml URL?
        tagMap.put("citation_xml_url", MetadataField.FIELD_ACCESS_URL);
        tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
        tagMap.put("citation_doi", MetadataField.FIELD_DOI);
        //tagMap.put("citation_fulltext_world_readable", ?????);
        tagMap.put("citation_issn", MetadataField.FIELD_ISSN); //there are two metadata tags that are labeled 'issn' but one is for online
        tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
        tagMap.put("citation_language", MetadataField.FIELD_LANGUAGE);
        tagMap.put("citation_abstract", MetadataField.FIELD_ABSTRACT);
        tagMap.put("citation_article_type", MetadataField.FIELD_ARTICLE_TYPE);
        tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
        tagMap.put("citation_keywords", MetadataField.FIELD_KEYWORDS);
            
    }

    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
      throws IOException {
      }
    
}
