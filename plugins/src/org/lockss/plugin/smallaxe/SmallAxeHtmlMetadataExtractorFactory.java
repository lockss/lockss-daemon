/*
 * $Id$
 */

/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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
