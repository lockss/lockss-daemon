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

package org.lockss.plugin.spandidos;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.atypon.BaseAtyponHtmlMetadataExtractorFactory;
import org.lockss.util.Logger;
import org.lockss.util.TypedEntryMap;

import java.io.IOException;

public class Spandidos2020HtmlMetadataExtractorFactory
  implements FileMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(Spandidos2020HtmlMetadataExtractorFactory.class);


  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                               String contentType)
      throws PluginException {
    return new SpandidosHtmlMetadataExtractor();
  }

  public static class SpandidosHtmlMetadataExtractor implements FileMetadataExtractor {

    private static MultiMap tagMap = new MultiValueMap();

    static {
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_journal_title", MetadataField. FIELD_PUBLICATION_TITLE);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      tagMap.put("citation_isbn", MetadataField.FIELD_ISBN);
      tagMap.put("citation_abstract_html_url", MetadataField.FIELD_ACCESS_URL);
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put("keywords", MetadataField.FIELD_KEYWORDS);

      tagMap.put(MetadataField.ARTICLE_TYPE_JOURNALARTICLE, MetadataField.FIELD_ARTICLE_TYPE);
      tagMap.put(MetadataField.PUBLICATION_TYPE_JOURNAL, MetadataField.FIELD_PUBLICATION_TYPE);
    }


    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter) throws IOException, PluginException {

      ArticleMetadata am =
              new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);

      am.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_JOURNALARTICLE);
      am.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_JOURNAL);

      am.cook(tagMap);

      if (am.isEmpty()) {
        return;
      }

      ArchivalUnit au = cu.getArchivalUnit();
      String metadataVolume = am.get(MetadataField.FIELD_VOLUME);
      log.debug3("Spandidos metadataVolume = " + metadataVolume);
      if (!StringUtils.isEmpty(metadataVolume)) {
        // Get the AU's volume name from the AU properties. This must be set
        TypedEntryMap tfProps = au.getProperties();
        String AU_volume = tfProps.getString(ConfigParamDescr.VOLUME_NAME.getKey());

        log.debug3("Spandidos AU_volume = " + AU_volume);
        Boolean isInAu =  ( (AU_volume != null) && (AU_volume.equals(metadataVolume)));

        if (isInAu) {
          log.debug3("Spandidos AU_volume = " + AU_volume + ", metadataVolume = " + metadataVolume);
          emitter.emitMetadata(cu, am);
        } else {
          log.debug3("Spandidos AU_volume = " + AU_volume + ", metadataVolume = " + metadataVolume);
        }
      }
    }
  }
}
