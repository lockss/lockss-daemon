/*
 * $Id: RSC2014HtmlMetadataExtractorFactory.java 39864 2015-02-18 09:10:24Z thib_gc $
 */

/*

 Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
