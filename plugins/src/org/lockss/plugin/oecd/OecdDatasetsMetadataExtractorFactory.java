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


package org.lockss.plugin.oecd;

import java.io.IOException;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.extractor.SimpleHtmlMetaTagMetadataExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

public class OecdDatasetsMetadataExtractorFactory extends OecdHtmlMetadataExtractorFactory{

    private static final Logger log = Logger.getLogger(OecdDatasetsMetadataExtractorFactory.class);

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(
        MetadataTarget target, String contentType)
        throws PluginException {
        return new OecdDatasetsHtmlMetadataExtractor();
    }

    public static class OecdDatasetsHtmlMetadataExtractor
    implements FileMetadataExtractor {

    private static MultiMap tagMap = new MultiValueMap();
        static {
            tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
            tagMap.put("dc.title", MetadataField.FIELD_ARTICLE_TITLE);
            tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
            tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
            tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
            tagMap.put("citation_abstract", MetadataField.FIELD_ABSTRACT);
            tagMap.put("description", MetadataField.FIELD_ABSTRACT);
            tagMap.put("citation_abstract_html_url", MetadataField.FIELD_ACCESS_URL);
            tagMap.put("citation_language", MetadataField.FIELD_LANGUAGE);
            tagMap.put("dc.identifier", MetadataField.DC_FIELD_IDENTIFIER);
            tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);

        }
        @Override
        public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
            throws IOException {
                ArticleMetadata am = new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
                am.cook(tagMap);

                ArchivalUnit au = cu.getArchivalUnit();
                TdbAu tdbau = au.getTdbAu();

                String provider;
                String publisher;

                if (tdbau != null) {
                    provider = tdbau.getProviderName();
                    publisher = tdbau.getPublisherName();

                    if (provider != null) {
                    am.put(MetadataField.FIELD_PROVIDER, provider);
                    }
                    if (publisher != null) {
                    am.put(MetadataField.FIELD_PUBLISHER, publisher);
                    }

                } else {
                    log.debug3("Inside OECD Datasets Metadata extractor, tdb is empty");
                }
                am.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_FILE);
                am.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.PUBLICATION_TYPE_FILE);
                emitter.emitMetadata(cu, am);
            }
    }
    
}
