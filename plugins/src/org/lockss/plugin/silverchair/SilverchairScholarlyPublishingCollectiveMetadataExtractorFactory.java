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

package org.lockss.plugin.silverchair;

import java.io.IOException;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractorFactory;
import org.lockss.extractor.JsoupTagExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataField.Cardinality;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;
import org.lockss.util.TypedEntryMap;

public class SilverchairScholarlyPublishingCollectiveMetadataExtractorFactory implements FileMetadataExtractorFactory {
    private static final Logger log = Logger.getLogger(SilverchairScholarlyPublishingCollectiveMetadataExtractorFactory.class);
    protected static final String KEY_PUBLICATION_ABBREV = "publication.abbrev";
  
    protected static final MetadataField FIELD_PUBLICATION_ABBREV = new MetadataField(KEY_PUBLICATION_ABBREV, Cardinality.Single);
  
    protected static final MultiMap cookMap = new MultiValueMap(); // see static initializer
    static {
        // All below seen in ACCP, ACP, AMA, APA
        cookMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
        cookMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
        // replacement title for proceedings
        cookMap.put("citation_conference_title", MetadataField.FIELD_PUBLICATION_TITLE);
        cookMap.put("citation_journal_abbrev", FIELD_PUBLICATION_ABBREV);
        cookMap.put("citation_issn", MetadataField.FIELD_ISSN);
        cookMap.put("citation_volume", MetadataField.FIELD_VOLUME);
        cookMap.put("citation_issue", MetadataField.FIELD_ISSUE);
        cookMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
        cookMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
        cookMap.put("citation_doi", MetadataField.FIELD_DOI);
        cookMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
        cookMap.put("citation_date", MetadataField.FIELD_DATE);
        // replacement date for proceedings
        cookMap.put("citation_publication_date", MetadataField.FIELD_DATE);
        cookMap.put("citation_author", MetadataField.FIELD_AUTHOR);
        cookMap.put("citation_keyword", MetadataField.FIELD_KEYWORDS);
        // addition for proceedings
        cookMap.put("citation_pdf_url", MetadataField.DC_FIELD_IDENTIFIER);
        // Portland, Rockefeller, Geoscience world those use CommonTheme need the next line
        cookMap.put("citation_pdf_url", MetadataField.FIELD_ACCESS_URL);
    }

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target, String contentType) throws PluginException {
        return new JsoupTagExtractor(contentType) {
        @Override
        public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
            throws IOException, PluginException {
                ArticleMetadata am = super.extract(target, cu);
                am.putRaw("extractor.type", "HTML");
                am.cook(cookMap);
                ArchivalUnit au = cu.getArchivalUnit();

                // Use the volume name from the ArticleMetadata
                String foundVolume = am.get(MetadataField.FIELD_VOLUME);
        
                // If we got nothing, just return, we can't validate further
                if (StringUtils.isEmpty(foundVolume)) {
                    log.debug3("Vol was empty. Can't validate so returning null. ");
                    return null;
                }

                // Check VOLUME
                TypedEntryMap tfProps = au.getProperties();
                String AU_volume = tfProps.getString(ConfigParamDescr.VOLUME_NAME.getKey());
                if( (AU_volume == null) || !AU_volume.equals(foundVolume)){
                    log.debug3("Did not pass volume check : foundVolume = " + foundVolume + ", AU_volume = " + AU_volume + ". Returning null.");
                    return null;
                }else{
                    log.debug3("Volume check passed : foundVolume = " + foundVolume + ", AU_volume = " + AU_volume);
                };

                String url = am.get(MetadataField.FIELD_ACCESS_URL);
                if (url == null || url.isEmpty() || !au.makeCachedUrl(url).hasContent()) {
                  url = cu.getUrl();
                }
                // this will have the correct protocol for this AU
                am.replace(MetadataField.FIELD_ACCESS_URL, url);
                return am;
            }
        };
    }
}
