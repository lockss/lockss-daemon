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
import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

public class RSCBooksHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory{
  
  private static final Logger log = Logger.getLogger(RSCBooksHtmlMetadataExtractorFactory.class);
  
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType) throws PluginException {
    return new RSCBooksHtmlMetadataExtractor();
  }
  
  /**
   * One of the articles used to get the html source for this plugin is:
   * http://pubs.rsc.org/en/content/chapter/bk9781849739917-00327/978-1-84973-991-7
   */
  
  public static class RSCBooksHtmlMetadataExtractor extends SimpleHtmlMetaTagMetadataExtractor {
    
    // Map RSC-specific HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    
    static {
      tagMap.put("DC.Language", MetadataField.DC_FIELD_LANGUAGE);
      tagMap.put("DC.Title", MetadataField.DC_FIELD_TITLE);
      tagMap.put("DC.Identifier", MetadataField.DC_FIELD_IDENTIFIER);
      tagMap.put("DC.issued", MetadataField.DC_FIELD_DATE);
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
    
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
      am.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_BOOKCHAPTER);
      am.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_BOOK);
      am.put(MetadataField.FIELD_ISBN, cu.getUrl().substring(1 + cu.getUrl().lastIndexOf('/')));
      
      // Pick up some information from the TDB if not in the cooked data
      TdbAu tdbau = cu.getArchivalUnit().getTdbAu(); // returns null if titleConfig is null 
      if (tdbau != null) {
        if (am.get(MetadataField.FIELD_PUBLICATION_TITLE) == null) {
          String pub_title = tdbau.getName();
          if (pub_title != null) {
            am.put(MetadataField.FIELD_PUBLICATION_TITLE, pub_title);
          }
        }
      }
      
      // Finally, check the access.url and MAKE SURE that it is in the AU
      // or put it to a value that is
      String potential_access_url;
      if ((potential_access_url = am.get(MetadataField.FIELD_ACCESS_URL)) != null) {
        CachedUrl potential_cu = cu.getArchivalUnit().makeCachedUrl(potential_access_url);
        if ( (potential_cu == null) || (!potential_cu.hasContent()) ) {
          //Not in this AU; set to cu
          am.replace(MetadataField.FIELD_ACCESS_URL, cu.getUrl());
        }
      } else {
        am.replace(MetadataField.FIELD_ACCESS_URL, cu.getUrl());
      }
      // normalize the url
      am.replace(MetadataField.FIELD_ACCESS_URL,
          AuUtil.normalizeHttpHttpsFromBaseUrl(
              cu.getArchivalUnit(),
              am.get(MetadataField.FIELD_ACCESS_URL)
          )
      );
      return am;
    } // extract
  }
}
