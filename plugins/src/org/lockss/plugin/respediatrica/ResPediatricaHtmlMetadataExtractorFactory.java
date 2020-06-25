/*
 * $Id:$
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

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
