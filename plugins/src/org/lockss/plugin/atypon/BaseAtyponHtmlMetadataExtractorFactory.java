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

package org.lockss.plugin.atypon;

import java.io.*;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.plugin.clockss.aimsciences.AimsCrossrefXmlMetadataExtractorFactory;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


public class BaseAtyponHtmlMetadataExtractorFactory 
  implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(BaseAtyponHtmlMetadataExtractorFactory.class);

  public FileMetadataExtractor 
    createFileMetadataExtractor(MetadataTarget target, String contentType)
      throws PluginException {
    return new BaseAtyponHtmlMetadataExtractor();
  }

  public static class BaseAtyponHtmlMetadataExtractor 
    implements FileMetadataExtractor {

    // Map Google Scholar HTML meta tag names to cooked metadata fields
    //NOTE - so far no books support HTML meta tags so we can assume journal
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
      //Adding this one especially for Sage to filter out overcrawlled content which belongs to other volume
      tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
    }
    
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {

      // NOTE: MarkAllen plugins Override this extract  method and then calls it via super.extract() after
      //       performing additional checks on Date and Doi.

      log.info("---------BaseAtyponHtmlMetadataExtractor-------");

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

      HttpHttpsUrlHelper helper = new HttpHttpsUrlHelper(cu.getArchivalUnit(),
          ConfigParamDescr.BASE_URL.getKey(),
          "base_url");
      String url = am.get(MetadataField.FIELD_ACCESS_URL);

      if (url != null) {
        url = helper.normalize(url);
        am.replace(MetadataField.FIELD_ACCESS_URL, url);
      }
      // If we've gotten this far, emit
      emitter.emitMetadata(cu, am);

    }
    
    protected MultiMap getTagMap() {
      return tagMap;
    }
  }
}