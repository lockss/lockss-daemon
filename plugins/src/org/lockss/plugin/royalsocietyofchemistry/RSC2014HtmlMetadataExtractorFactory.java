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

package org.lockss.plugin.royalsocietyofchemistry;

import java.io.IOException;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

public class RSC2014HtmlMetadataExtractorFactory implements FileMetadataExtractorFactory{
  
  private static final Logger log = Logger.getLogger(RSC2014HtmlMetadataExtractorFactory.class);
  
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType) throws PluginException {
    return new RSC2014HtmlMetadataExtractor();
  }	
  
  /**
   * One of the articles used to get the html source for this plugin is:
   * http://pubs.rsc.org/en/content/articlelanding/2008/gc/b805436c
   */
  
  public static class RSC2014HtmlMetadataExtractor extends SimpleHtmlMetaTagMetadataExtractor {
    
    // Map RSC-specific HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    
    static {
      tagMap.put("DC.Language", MetadataField.DC_FIELD_LANGUAGE);
      tagMap.put("DC.Title", MetadataField.DC_FIELD_TITLE);
      tagMap.put("DC.Identifier", MetadataField.DC_FIELD_IDENTIFIER);
      tagMap.put("DC.issued", MetadataField.DC_FIELD_DATE);
      tagMap.put("DC.Publisher", MetadataField.DC_FIELD_PUBLISHER);
      tagMap.put("DC.Contributor", MetadataField.DC_FIELD_CONTRIBUTOR);
      tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      // citation_fulltext_html_url is not collected, so use metadata URL
      // tagMap.put("citation_fulltext_html_url", MetadataField.FIELD_ACCESS_URL);
    } // static
    
    /**
     * Extract metadata from the cached URL
     * @param cu The cached URL to extract the metadata from
     */
    
    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
      throws IOException {
      
      log.debug3("Metadata - cachedurl cu:" + cu.getUrl());
      
      ArticleMetadata am = super.extract(target, cu);
      am.cook(tagMap);

      // set url, and normalize it. normalization should be redundant, as the cachedUrl should already be normalized,
      // but just in case.
      am.replace(
          MetadataField.FIELD_ACCESS_URL,
          AuUtil.normalizeHttpHttpsFromBaseUrl(cu.getArchivalUnit(), cu.getUrl())
      );
      return am;
    } // extract
    
  }
}
