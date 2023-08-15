/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.oapen;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;

public class OAPENBooksMetadataExtractorFactory implements FileMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(OAPENBooksMetadataExtractorFactory.class);

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
          throws PluginException {
    return new Emerald2020MetadataExtractor();
  }

  /*
    view-source:https://library.oapen.org/handle/20.500.12657/41485?show=full

    <meta name="DC.creator" content="Villinger, Rahel" />
    <meta name="DCTERMS.dateAccepted" content="2020-08-25T12:16:45Z" scheme="DCTERMS.W3CDTF" />
    <meta name="DCTERMS.available" content="2020-08-25T12:16:45Z" scheme="DCTERMS.W3CDTF" />
    <meta name="DCTERMS.issued" content="2018" scheme="DCTERMS.W3CDTF" />
    <meta name="DC.identifier" content="ONIX_20200825_9783835391154_49" />
    <meta name="DC.identifier" content="https://library.oapen.org/handle/20.500.12657/41485" scheme="DCTERMS.URI" />
    <meta name="DC.language" content="German" />
    <meta name="DC.subject" content="bic Book Industry Communication::A The arts::AB The arts: general issues::ABA Theory of art" />
    <meta name="DC.subject" content="bic Book Industry Communication::D Literature &amp; literary studies::DS Literature: history &amp; criticism::DSA Literary theory" />
    <meta name="DC.subject" content="Kant" />
    <meta name="DC.subject" content="Imagination" />
    <meta name="DC.subject" content="Tiere" />
    <meta name="DC.title" content="Kant und die Imagination der Tiere" />
    <meta name="DC.type" content="book" />
    <meta content="Kant" name="citation_keywords">
    <meta content="Imagination" name="citation_keywords">
    <meta content="Tiere" name="citation_keywords">
    <meta content="2018" name="citation_publication_date">
    <meta content="" name="citation_fulltext_world_readable">
    <meta content="9783835391154" name="citation_isbn">
    <meta content="Kant und die Imagination der Tiere" name="citation_title">
    <meta content="Konstanz University Press" name="citation_publisher">
    <meta content="Villinger, Rahel" name="citation_author">
    <meta content="2020-08-25T12:16:45Z" name="citation_online_date">
    <meta content="10.13039/501100001711" name="citation_funder_id">
    <meta content="monograph" name="citation_book_type">
    <meta content="German" name="citation_language">
    <meta content="10.18148/kops/352-2-1m3czbpi9a5b70" name="citation_doi">
    <meta content="10BP12_185527" name="citation_grant_number">
    <meta content="https://library.oapen.org/bitstream/20.500.12657/41485/1/9783835391154.pdf" name="citation_pdf_url">
    <meta content="Schweizerischer Nationalfonds zur F&ouml;rderung der Wissenschaftlichen Forschung" name="citation_funder">
   */

  public static class Emerald2020MetadataExtractor
          implements FileMetadataExtractor {
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_isbn", MetadataField.FIELD_ISBN);
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_title", MetadataField.FIELD_PUBLICATION_TITLE);
    }

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter) throws IOException, PluginException {
      ArticleMetadata am = new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);

      if (am.isEmpty()) {
        return;
      }

      emitter.emitMetadata(cu, am);
    }
  }
}
