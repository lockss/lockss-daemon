/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

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
package org.lockss.plugin.ojs3;

import java.io.IOException;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractorFactory;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.extractor.SimpleHtmlMetaTagMetadataExtractor;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.ojs3.Ojs3HtmlMetadataExtractorFactory.Ojs3HtmlMetadataExtractor;
import org.lockss.util.Logger;

public class Ojs3HtmlMetadataExtractorFactory2024 implements FileMetadataExtractorFactory{

    private static final Logger log = Logger.getLogger(Ojs3HtmlMetdataExtractorFactory2024.class);
    private static final SimpleHtmlMetaTagMetadataExtractor shtmmde = new SimpleHtmlMetaTagMetadataExtractor();
    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget arg0, String arg1) throws PluginException {
        return new Ojs3HtmlMetadataExtractor2024();
    }

    public static class Ojs3HtmlMetadataExtractor2024 implements FileMetadataExtractor{

        // Map HTML meta tag names to cooked metadata fields
        private static MultiMap tagMap = new MultiValueMap();
        static  {
        tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
        tagMap.put("citation_author",
            new MetadataField(MetadataField.FIELD_AUTHOR,
                            MetadataField.splitAt(";")));
        tagMap.put("citation_keywords",
            new MetadataField(MetadataField.FIELD_KEYWORDS,
                MetadataField.splitAt(";")));
        tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
        tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
        tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
        tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
        tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
        tagMap.put("citation_date", MetadataField.FIELD_DATE);
        tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
        tagMap.put("citation_doi",  MetadataField.FIELD_DOI);
        tagMap.put("citation_issn",  MetadataField.FIELD_ISSN);
        tagMap.put("citation_language",  MetadataField.FIELD_LANGUAGE);
        }

        @Override
        public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
            throws IOException, PluginException {
            
            ArticleMetadata am = shtmmde.extract(target, cu);
            if (am != null) {
                am.cook(tagMap);
                emitter.emitMetadata(cu, am);
            }
        }
    }
}
