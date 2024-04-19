/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.europeanmathematicalsociety.api;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;

/*
  https://ems.press/books/elm/232

  <meta name="citation_title" content="An Introduction to Singular Stochastic PDEs"/>
  <meta name="citation_author" content="Nils Berglund"/>
  <meta name="citation_publication_date" content="2022/04/19"/>
  <meta name="citation_issn" content="2523-5176"/>
  <meta name="citation_issn" content="2523-5184"/>
  <meta name="citation_isbn" content="978-3-98547-014-3"/>
  <meta name="citation_isbn" content="978-3-98547-514-8"/>
  <meta name="citation_doi" content="10.4171/elm/34"/>
  <meta name="citation_pdf_url" content="https://ems.press/content/book-files/23815"/>
 */

public class EuropeanMathematicalSocietyBooksHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  
  private static final Logger log = Logger.getLogger(EuropeanMathematicalSocietyBooksHtmlMetadataExtractorFactory.class);
  
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
        String contentType)
      throws PluginException {
    return new EuropeanMathematicalSocietyHtmlMetadataExtractor();
  }
  
  public static class EuropeanMathematicalSocietyHtmlMetadataExtractor extends SimpleHtmlMetaTagMetadataExtractor {
    
    // Map HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_pdf_url", MetadataField.FIELD_ACCESS_URL);
      // The following is/are for books
      tagMap.put("citation_isbn", MetadataField.FIELD_ISBN);
    }
    
    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
        throws IOException {
      ArticleMetadata am = super.extract(target, cu);

      am.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_BOOKVOLUME);
      am.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_BOOK);

      am.cook(tagMap);
      return am;
    }
  }
}