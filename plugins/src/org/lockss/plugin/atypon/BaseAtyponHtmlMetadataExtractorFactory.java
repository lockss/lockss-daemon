/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon;

import java.io.*;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


public class BaseAtyponHtmlMetadataExtractorFactory 
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(BaseAtyponHtmlMetadataExtractorFactory.class);

  public FileMetadataExtractor 
    createFileMetadataExtractor(MetadataTarget target, String contentType)
      throws PluginException {
    return new BaseAtyponHtmlMetadataExtractor();
  }

  public static class BaseAtyponHtmlMetadataExtractor 
    implements FileMetadataExtractor {

    // Map Google Scholar HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("dc.Identifier", MetadataField.FIELD_DOI);
      tagMap.put("dc.Identifier", MetadataField.DC_FIELD_IDENTIFIER);
      
      tagMap.put("dc.Date", MetadataField.FIELD_DATE);
      tagMap.put("dc.Date", MetadataField.DC_FIELD_DATE);
      
      tagMap.put("dc.Creator",
          new MetadataField(MetadataField.FIELD_AUTHOR,
              MetadataField.splitAt(";")));
      tagMap.put("dc.Creator", MetadataField.DC_FIELD_CREATOR);
      
      tagMap.put("dc.Title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("dc.Title", MetadataField.DC_FIELD_TITLE);
      
      tagMap.put("dc.Publisher", MetadataField.DC_FIELD_PUBLISHER);
      // 3/6/15 - remove cooking the dc.publisher as FIELD_PUBLISHER
      // the value tends to be variable and a better result will 
      // come from the TDB file if this isn't set
      //tagMap.put("dc.Publisher", MetadataField.FIELD_PUBLISHER);
      
      tagMap.put("dc.Subject", MetadataField.DC_FIELD_SUBJECT);
      tagMap.put("dc.Subject", 
          new MetadataField(MetadataField.FIELD_KEYWORDS,
              MetadataField.splitAt(";")));
      
      tagMap.put("dc.Description", MetadataField.DC_FIELD_DESCRIPTION);
      tagMap.put("dc.Type", MetadataField.DC_FIELD_TYPE);      
      tagMap.put("dc.Format", MetadataField.DC_FIELD_FORMAT);
      tagMap.put("dc.Language", MetadataField.DC_FIELD_LANGUAGE);
      tagMap.put("dc.Rights", MetadataField.DC_FIELD_RIGHTS);
      tagMap.put("dc.Coverage",MetadataField.DC_FIELD_COVERAGE);
      tagMap.put("dc.Source", MetadataField.DC_FIELD_SOURCE);
    }
    
    
    
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {
      ArticleMetadata am = 
        new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
 
      am.cook(tagMap);
      /* 
       * if, due to overcrawl, we got to a page that didn't have anything
       * valid, eg "this page not found" html page
       * don't emit empty metadata (because defaults would get put in
       * Must do this after cooking, because it checks size of cooked info
       */
      if (am.isEmpty()) {
        return;
      }
      
      // Only emit if this item is likely to be from this AU
      // protect against counting overcrawled articles
      ArchivalUnit au = cu.getArchivalUnit();
      if (!BaseAtyponMetadataUtil.metadataMatchesTdb(au, am)) {
        return;
      }
      
      /*
       * Fill in DOI, publisher, other information available from
       * the URL or TDB 
       * CORRECT the access.url if it is not in the AU
       */
      BaseAtyponMetadataUtil.completeMetadata(cu, am);
      
      // If we've gotten this far, emit
      emitter.emitMetadata(cu, am);

    }
    
    protected MultiMap getTagMap() {
      return tagMap;
    }
  }
}