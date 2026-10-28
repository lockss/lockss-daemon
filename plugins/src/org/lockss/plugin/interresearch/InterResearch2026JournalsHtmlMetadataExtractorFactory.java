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

package org.lockss.plugin.interresearch;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;

public class InterResearch2026JournalsHtmlMetadataExtractorFactory
    implements FileMetadataExtractorFactory {

  private static final Logger log =
      Logger.getLogger(InterResearch2026JournalsHtmlMetadataExtractorFactory.class);

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
      throws PluginException {
    return new InterResearch2026JournalsHtmlMetadataExtractor();
  }

  public static class InterResearch2026JournalsHtmlMetadataExtractor
      implements FileMetadataExtractor {

    private static final MultiMap tagMap = new MultiValueMap();


    static {
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
      tagMap.put("citation_journal_publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
    }

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException, PluginException {

      ArticleMetadata am = new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);

      ArchivalUnit au = cu.getArchivalUnit();

      // Volume check: landing-page URLs carry no volume, so a page from another
      // volume can end up in this AU (e.g. via a Previous/Next link the crawl
      // filter missed). Only emit when citation_volume matches this AU's volume.
      String auVolume = getAuVolume(au);
      String pageVolume = am.get(MetadataField.FIELD_VOLUME);

      log.debug3("Volume check " + cu.getUrl() + ": citation_volume = "
                 + pageVolume + ", AU volume = " + auVolume);

      if (auVolume == null || pageVolume == null
          || !auVolume.trim().equalsIgnoreCase(pageVolume.trim())) {
        log.debug("Not emitting metadata, volume mismatch for " + cu.getUrl()
                  + ": citation_volume = " + pageVolume
                  + ", AU volume = " + auVolume);
        return;
      }

      // Point access.url at the PDF when it was collected; otherwise the
      // landing page (BaseArticleMetadataExtractor's default) stays.
      String pdfUrl = am.getRaw("citation_pdf_url");
      if (pdfUrl != null) {
        CachedUrl pdfCu = au.makeCachedUrl(pdfUrl);
        try {
          if (pdfCu != null && pdfCu.hasContent()) {
            am.replace(MetadataField.FIELD_ACCESS_URL, pdfUrl);
          }
        } finally {
          if (pdfCu != null) {
            pdfCu.release();
          }
        }
      }

      emitter.emitMetadata(cu, am);
    }

    /**
     * The AU's volume: TDB volume if there is a TDB entry, else the
     * definitional volume_name param (always set).
     */
    private static String getAuVolume(ArchivalUnit au) {
      TdbAu tdbAu = au.getTdbAu();
      if (tdbAu != null && tdbAu.getVolume() != null) {
        return tdbAu.getVolume();
      }
      return au.getConfiguration().get("volume_name");
    }
  }
}
