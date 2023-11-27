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

package org.lockss.plugin.smallaxe;

import java.io.*;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


/**
 * One of the articles used to get the html source for this plugin is:
 * http://smallaxe.net/sxarchipelagos/issue02/nou-toujou-la.html
 * 
 * 
 */
public class SmallAxeHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(SmallAxeHtmlMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
      throws PluginException {
    return new SmallAxeHtmlMetadataExtractor();
  }

  public static class SmallAxeHtmlMetadataExtractor 
    implements FileMetadataExtractor {
   
    private static MultiMap tagMap = new MultiValueMap();
    static {
      //<meta name="citation_doi" content="doi:10.7916/D8J394ZN"/>
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      //<meta name="citation_online_date" content="2017/10/21">
      // or
      //<meta name="citation_publication_date" content="2017/07/01">
      tagMap.put("citation_online_date", MetadataField.FIELD_DATE);
      //<meta name="citation_title" content="*Nou toujou la!* The Digital (After-)Life of Radio Haïti-Inter"/>
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      //<meta name="citation_issn" content="2473-2206"/>
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      //<meta name="citation_issue" content="2">
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      //<meta name="citation_author" content="Laura Wagner"/>
      tagMap.put("citation_author",MetadataField.FIELD_AUTHOR);
      //<meta name="citation_journal_title" content="sx archipelagos"/>
      tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
      //<meta name="citation_publisher" content="Small Axe Project"/>
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
      //<meta name="citation_language" content="en"/>
      tagMap.put("citation_language", MetadataField.FIELD_LANGUAGE);
      // not seen but possible in future
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      
    }
    
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {
      ArticleMetadata am = new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);
      // optionally add in any necessary post-cook correction/additions
      if (am.get(MetadataField.FIELD_DATE) == null) {
          String altDate = am.getRaw("citation_publication_date");
          if (altDate != null) {
              am.put(MetadataField.FIELD_DATE,altDate);
          }
      }
      emitter.emitMetadata(cu, am);
    }
    
  }
  
}
