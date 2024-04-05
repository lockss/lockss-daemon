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

package org.lockss.plugin.lsuia;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;

public class LSUIAHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {

  /*
    <meta name="citation_journal_title" content="Social and Legal Studios">
      <meta name="citation_journal_abbrev" content="SLS"/>
      <meta name="citation_issn" content="2617-4162">
      <meta name="citation_author" content="Qian Zhang">
      <meta name="citation_author" content="Daniyar Dzhumaliev">
      <meta name="citation_author" content="JingFei Qi">
      <meta name="citation_title" content="Development dilemma and solutions to online civil litigation in China: Kyrgyzstan experience">
      <meta name="citation_date" content="2023/09/27">
      <meta name="citation_volume" content="3">
      <meta name="citation_issue" content="6">
      <meta name="citation_firstpage" content="209">
      <meta name="citation_lastpage" content="221">
      <meta name="citation_abstract_html_url" content="https://sls-journal.com.ua/en/journals/tom-6-3-2023/dilema-rozvitku-ta-shlyakhi-virishennya-tsivilnikh-sudovikh-sprav-onlayn-u-kitayi-dosvid-kirgizstanu" />
      <meta name="citation_pdf_url" content="https://sls-journal.com.ua/web/uploads/pdf/Social and Legal Studios_Vol. 6_No. 3_2023_209-221.pdf">
      <meta name="citation_keywords" xml:lang="en" content="e-process"/>
   */

  protected static final MultiMap cookMap = new MultiValueMap(); // see static initializer
  static {

    cookMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put("citation_issn", MetadataField.FIELD_ISSN);
    cookMap.put("citation_volume", MetadataField.FIELD_VOLUME);
    cookMap.put("citation_issue", MetadataField.FIELD_ISSUE);
    cookMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
    cookMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
    cookMap.put("citation_doi", MetadataField.FIELD_DOI);
    cookMap.put("citation_date", MetadataField.FIELD_DATE);
    cookMap.put("citation_author", MetadataField.FIELD_AUTHOR);
    cookMap.put("citation_keywords", MetadataField.FIELD_KEYWORDS);
    cookMap.put("citation_pdf_url", MetadataField.FIELD_ACCESS_URL);
    cookMap.put("DC.Identifier.DOI",MetadataField.FIELD_DOI);
  }

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
          throws PluginException {
    return new JsoupTagExtractor(contentType) {
      @Override
      public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
              throws IOException, PluginException {
        ArticleMetadata am = super.extract(target, cu);
        am.cook(cookMap);

        return am;
      }
    };
  }
}
