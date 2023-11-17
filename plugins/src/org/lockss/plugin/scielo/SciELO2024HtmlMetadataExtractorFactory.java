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
import java.util.List;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.daemon.TitleConfig;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractorFactory;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.extractor.SimpleHtmlMetaTagMetadataExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

public class SciELO2024HtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {

    static Logger log = Logger.getLogger(SciELO2024HtmlMetadataExtractorFactory.class);

    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target, String contentType) throws PluginException {
        return new SciELO2024HtmlMetadataExtractor();
    }

    public static class SciELO2024HtmlMetadataExtractor 
    extends SimpleHtmlMetaTagMetadataExtractor {

    private static MultiMap tagMap = new MultiValueMap();
    
    static {
        tagMap.put("citation_author", MetadataField.FIELD_AUTHOR); 
        //tagMap.put("citation_author_affiliation",????); No available metadatafield, so will not be added
        //tagMap.put("citation_author_orcid", ?????); No available metadatafield, so will not be added
        tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
        //tagMap.put("citation_journal_abbrev", ????); No available metadatafield, so will not be added
        tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
        tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
        tagMap.put("citation_number", MetadataField.FIELD_ISSUE); 
        tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
        tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
        tagMap.put("citation_doi", MetadataField.FIELD_DOI);
        //tagMap.put("citation_fulltext_world_readable", ?????); No available metadatafield, so will not be added
        tagMap.put("citation_issn", MetadataField.FIELD_ISSN); //there are two metadata tags that are labeled 'issn' but one is for online
        tagMap.put("citation_eissn", MetadataField.FIELD_EISSN); //as of 2023, SciELO has 2 metatags of citation_issn
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
        ArticleMetadata am = super.extract(target, cu);
        //Trying to fix up both issn metatags so that one will be mapped to FIELD_ISSN and the other to FIELD_EISSN
        //get issn from au, compare with issn from metadata
        List<String> listOfIssns = am.getRawList("citation_issn");
        if(listOfIssns != null && listOfIssns.size() > 1){
            ArchivalUnit au = cu.getArchivalUnit();
            TitleConfig tc = au.getTitleConfig();
            if(tc != null){
                TdbAu tdbAu = tc.getTdbAu();
                if(tdbAu != null){
                    am.putRaw("citation_issn", (String)null);
                    String issn = tdbAu.getIssn();
                    String eissn = tdbAu.getEissn();
                    if(issn != null){
                        am.putRaw("citation_issn", issn);
                    }
                    if(eissn != null){
                        am.putRaw("citation_eissn", eissn);
                    }
                }
            }
        }
        am.cook(tagMap);
        return am;
      }
}
}
