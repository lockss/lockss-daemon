/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.bmp;

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

public class BMPAbstractMetadataExtractorFactory implements FileMetadataExtractorFactory {
    static Logger log = Logger.getLogger(BMPAbstractMetadataExtractorFactory.class);

    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target, String contentType) throws PluginException {
        return new BMPAbstractMetadataExtractor();
    }

    public static class BMPAbstractMetadataExtractor 
    extends SimpleHtmlMetaTagMetadataExtractor {

    private static MultiMap tagMap = new MultiValueMap();
    
    static {
        tagMap.put("citation_author", MetadataField.FIELD_AUTHOR); 
        tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
        tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
        tagMap.put("citation_issue", MetadataField.FIELD_ISSUE); 
        tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
        tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
        tagMap.put("citation_doi", MetadataField.FIELD_DOI);
        tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
        tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
        tagMap.put("citation_language", MetadataField.FIELD_LANGUAGE);
        tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
        tagMap.put("citation_keywords", MetadataField.FIELD_KEYWORDS);
    }

    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
      throws IOException {
        ArticleMetadata am = super.extract(target, cu);
        ArchivalUnit au = cu.getArchivalUnit();
        TitleConfig tc = au.getTitleConfig();
        if(tc != null){
            TdbAu tdbAu = tc.getTdbAu();
            if(tdbAu != null){
                String issn = tdbAu.getIssn();
                if(issn != null){
                    am.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_JOURNALARTICLE);
                    am.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_JOURNAL);
                }
            }
        }
        am.cook(tagMap);
        return am;
      }
    }
}
