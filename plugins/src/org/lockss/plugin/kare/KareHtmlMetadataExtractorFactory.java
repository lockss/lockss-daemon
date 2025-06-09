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

package org.lockss.plugin.kare;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;

public class KareHtmlMetadataExtractorFactory
    implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(KareHtmlMetadataExtractorFactory.class);

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
      throws PluginException {
    return new KareHtmlMetadataExtractor();
  }

  public static class KareHtmlMetadataExtractor
          implements FileMetadataExtractor {

    private static MultiMap tagMap = new MultiValueMap();

    /*
      <meta name="citation_title" content="A new treatment modality in piriformis syndrome: Ultrasound guided dry needling treatment">
      <meta name="citation_author" content="Bağcıer, Fatih">
      <meta name="citation_author" content="Tufanoğlu, Fatih Hakan">
      <meta name="citation_journal_title" content="Agri">
      <meta name="citation_volume" content="32">
      <meta name="citation_issue" content="3">
      <meta name="citation_firstpage" content="175">
      <meta name="citation_lastpage" content="176">
      <meta name="citation_language" content="en">
      <meta name="keywords" content="Dry needling, Piriformis sendromu, Ultrasound">
      <title>A new treatment modality in piriformis syndrome: Ultrasound guided dry needling treatment [Ağrı]</title>
     */
    static {
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_journal_title", MetadataField. FIELD_PUBLICATION_TITLE);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
      tagMap.put("citation_keywords", MetadataField.FIELD_KEYWORDS);
      tagMap.put("citation_journal_publisher", MetadataField.FIELD_PUBLISHER);
    }

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter) throws IOException, PluginException {
      ArticleMetadata am =
              new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);

      ArchivalUnit au = cu.getArchivalUnit();


      String tdbVolume = null;
      String citationVolume = null;

      TdbAu tdbau = cu.getArchivalUnit().getTdbAu();

      if (tdbau != null) {
        tdbVolume = tdbau.getVolume();
      }

      citationVolume = am.get(MetadataField.FIELD_VOLUME);

      log.debug3("Check metadata citationVolume = " + citationVolume + ", tdbVolume = " + tdbVolume);

      if (tdbVolume == null || citationVolume == null || !tdbVolume.equals(citationVolume)) {
        log.debug3("Emit metadata falied maching volume, citationVolume = " + citationVolume + ", tdbVolume = " + tdbVolume);
        return;
      }
      emitter.emitMetadata(cu, am);
    }
  }
}