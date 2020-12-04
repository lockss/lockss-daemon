/*

Copyright (c) 2000-2020, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.respediatrica;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;

public class ResPediatricaHtmlMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(ResPediatricaHtmlMetadataExtractorFactory.class);

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                               String contentType)
      throws PluginException {
    return new PediatricaOaiHtmlMetadataExtractor();
  }

  /*
  <meta xmlns="" name="citation_journal_title" content="" />
  <meta xmlns="" name="citation_journal_title_abbrev" content="" />
  <meta xmlns="" name="citation_publisher" content="" />
  <meta xmlns="" name="citation_title" content="Paraneoplastic Nephrotic Syndrome and Hodgkin’s Lymphoma: A Case Report" />
  <meta xmlns="" name="citation_publication_date" content="2018" />
  <meta xmlns="" name="citation_volume" content="8" />
  <meta xmlns="" name="citation_issue" content="2" />
  <meta xmlns="" name="citation_issn" content="2236-6814" />
  <meta xmlns="" name="citation_doi" content="10.25060/residpediatr-2018.v8n2-08" />
  <meta xmlns="" name="citation_fulltext_html_url" content="http://residenciapediatrica.com.br/detalhes/323" />
  <meta xmlns="" name="citation_pdf_url" content="http://residenciapediatrica.com.br//ExportarPDF/323/v8n2a08.pdf" />
  <meta xmlns="" name="citation_author" content="Dassi, Natalia" />
  <meta xmlns="" name="citation_author" content="Garcia, Clotilde" />
  <meta xmlns="" name="citation_author" content="Silva, Roberta" />
  <meta xmlns="" name="citation_author" content="Júnior, Cláudio" />

  <meta xmlns="" name="citation_firstpage" content="96" />
  <meta xmlns="" name="citation_lastpage" content="98" />
  <meta xmlns="" name="citation_id" content="10.25060/residpediatr-2018.v8n2-08" />
   */

  public static class PediatricaOaiHtmlMetadataExtractor
    extends SimpleHtmlMetaTagMetadataExtractor {
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_journal_title", MetadataField. FIELD_PUBLICATION_TITLE);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      tagMap.put("citation_isbn", MetadataField.FIELD_ISBN);
      tagMap.put("citation_fulltext_html_url", MetadataField.FIELD_ACCESS_URL);
      tagMap.put("citation_journal_title_abbrev", MetadataField.FIELD_PUBLICATION_TITLE);
    }

    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
    throws IOException {
      ArticleMetadata am = super.extract(target, cu);
      am.cook(tagMap);
      String url = am.get(MetadataField.FIELD_ACCESS_URL);
      ArchivalUnit au = cu.getArchivalUnit();
      if (url == null || url.isEmpty() || !au.makeCachedUrl(url).hasContent()) {
        url = cu.getUrl();
      }
      am.replace(MetadataField.FIELD_ACCESS_URL,url);
      return am;
    }
  }
}
