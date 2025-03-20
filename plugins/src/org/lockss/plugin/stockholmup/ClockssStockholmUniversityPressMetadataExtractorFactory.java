/*

Copyright (c) 2000-2025, Board of Trustees of Leland Stanford Jr. University

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
package org.lockss.plugin.stockholmup;

import org.lockss.extractor.FileMetadataExtractor;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;

import java.io.IOException;

public class ClockssStockholmUniversityPressMetadataExtractorFactory implements FileMetadataExtractorFactory {
    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new ClockssStockholmUniversityPressMetadataExtractor();
    }

    /*
        <meta name="citation_publisher" content="Stockholm University Press" />
        <meta name="citation_journal_title" content="Journal of Home Language Research" />
        <meta name="citation_issn" content="2537-7043" />
        <meta name="citation_volume" content="4" />
        <meta name="citation_issue" content="1" />
        <meta name="citation_title" content="Expert Evaluation on Urgent Research on Heritage Language Education: A Comparative Study in Germany, Italy, the Netherlands, Portugal and Spain" />
        <meta name="citation_doi" content="10.16993/jhlr.35" />
        <meta name="citation_year" content="2021" />
        <meta name="citation_online_date" content="2021-04-23" />
        <meta name="citation_public_url" content="https://stockholmup.clockss.org/stockholmup/jhlr/4/1/35/index.html" />
        <meta name="citation_pdf_url" content="https://stockholmup.clockss.org/stockholmup/jhlr/4/1/35/article_pdf/35.pdf" />
        <meta name="citation_author" content="Barbara Gross" />
     */
    
    public static class ClockssStockholmUniversityPressMetadataExtractor
            extends SimpleHtmlMetaTagMetadataExtractor {
        private static MultiMap tagMap = new MultiValueMap();
        static {
            tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
            tagMap.put("citation_journal_title", MetadataField.FIELD_JOURNAL_TITLE);
            tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
            tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
            tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
            tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
            tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
            tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
            tagMap.put("citation_doi", MetadataField.FIELD_DOI);
            tagMap.put("citation_year", MetadataField.FIELD_DATE);
            tagMap.put("citation_pdf_url", MetadataField.FIELD_ACCESS_URL);
            tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
            tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
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
