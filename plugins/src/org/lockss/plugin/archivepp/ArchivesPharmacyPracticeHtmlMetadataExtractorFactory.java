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

package org.lockss.plugin.archivepp;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;

/*
  https://archivepp.com/article/authorship-criteria-and-ethical-requirements-for-publishing-in-archives-of-pharmacy-practice

  <meta name="DC.Date.created" scheme="ISO8601" content="2019-10-26T07:37:20+00:00" />
  <meta name="DC.Date.dateSubmitted" scheme="ISO8601" content="2019-10-26T07:37:20+00:00" />
  <meta name="DC.Date.issued" scheme="ISO8601" content="2019-10-26T07:37:20+00:00" />
  <meta name="DC.Date.modified" scheme="ISO8601" content="2019-10-26T07:37:20+00:00" />
  <meta name="DC.Description" xml:lang="en" content="" />
  <meta name="DC.Format" scheme="IMT" content="html" />
  <meta name="DC.Format" scheme="IMT" content="pdf" />
  <meta name="DC.Identifier" content="1"/>
  <meta name="DC.Identifier.pageNumber" content="1-1" />
  <meta name="DC.Identifier.DOI" content="" />
  <meta name="DC.Identifier.URI" content="https://archivepp.com/article/authorship-criteria-and-ethical-requirements-for-publishing-in-archives-of-pharmacy-practice" />
  <meta name="DC.Language" scheme="ISO639-1" content="en" />
  <meta name="DC.Source" content="Archives of Pharmacy Practice"/>
  <meta name="DC.Source.ISSN" content="2320-5210" />
  <meta name="DC.Source.Issue" content="1-2010" />
  <meta name="DC.Source.URI" content="https://archivepp.com/journal/archive" />
  <meta name="DC.Source.Volume" content="1" />
  <meta name="DC.Title" content="Authorship criteria and Ethical Requirements for Publishing in Archives of Pharmacy Practice" />
  <meta name="gs_meta_revision" content="1.1" />
  <meta name="citation_journal_title" content="Archives of Pharmacy Practice" />
  <meta name="citation_issn" content="2320-5210" />
  <meta name="citation_author" content="Tahir Mehmood Khan" />
  <meta name="citation_title" content="Authorship criteria and Ethical Requirements for Publishing in Archives of Pharmacy Practice" />
  <meta name="citation_date" content="2010"/>
  <meta name="citation_publication_date" content="2010" />
  <meta name="citation_volume" content="1" />
  <meta name="citation_issue" content="1-2010" />
  <meta name="citation_firstpage" content="1"/>
  <meta name="citation_lastpage" content="1"/>
  <meta name="citation_doi" content="" />
  <meta name="citation_abstract_html_url" content="https://archivepp.com/article/authorship-criteria-and-ethical-requirements-for-publishing-in-archives-of-pharmacy-practice" />
  <meta name="citation_language" content="en" />
  <meta name="citation_keywords" xml:lang="en" content="" />
  <meta name="citation_pdf_url" content="https://archivepp.com/storage/models/article/ljBbbET4WSruDmEha9SeMlUpAonZNERUqpKOttXw9pX3H159Wx6O2k6Oqy21/authorship-criteria-and-ethical-requirements-for-publishing-in-archives-of-pharmacy-practice.pdf" />
  <meta name="citation_fulltext_html_url" content="https://archivepp.com/article/authorship-criteria-and-ethical-requirements-for-publishing-in-archives-of-pharmacy-practice?html" />
*/

public class ArchivesPharmacyPracticeHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  
  private static final Logger log = Logger.getLogger(ArchivesPharmacyPracticeHtmlMetadataExtractorFactory.class);
  
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
      log.debug3("Inside extract method");

      am.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_JOURNALARTICLE);
      am.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_JOURNAL);


      am.cook(tagMap);
      return am;
    }
  }
}