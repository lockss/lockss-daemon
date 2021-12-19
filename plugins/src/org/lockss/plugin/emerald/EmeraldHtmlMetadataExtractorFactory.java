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

package org.lockss.plugin.emerald;

import java.io.*;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

public class EmeraldHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("EmeraldHtmlMetadataExtractorFactory");

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
  throws PluginException {
    return new EmeraldHtmlMetadataExtractor();
  }

  public static class EmeraldHtmlMetadataExtractor 
  implements FileMetadataExtractor {

    // Map Emerald's Google Scholar HTML meta tag names for journals to cooked metadata fields
    private static MultiMap journalTagMap = new MultiValueMap();
    static {
      journalTagMap.put("citation_doi", MetadataField.FIELD_DOI);
      journalTagMap.put("citation_date", MetadataField.FIELD_DATE);
      journalTagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      journalTagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      journalTagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      journalTagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      journalTagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      journalTagMap.put("citation_authors",
          new MetadataField(MetadataField.FIELD_AUTHOR,
              MetadataField.splitAt(";")));
      journalTagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      journalTagMap.put("citation_journal_title", MetadataField.FIELD_JOURNAL_TITLE);
      journalTagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
    }

    // Map Emerald's Google Scholar HTML meta tag names for books to cooked metadata fields
    private static MultiMap bookTagMap = new MultiValueMap();
    static {
      bookTagMap.put("citation_doi", MetadataField.FIELD_DOI);
      bookTagMap.put("citation_date", MetadataField.FIELD_DATE);
      bookTagMap.put("citation_volume", MetadataField.FIELD_JOURNAL_TITLE);
      bookTagMap.put("citation_issue", MetadataField.FIELD_VOLUME);
      bookTagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      bookTagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      bookTagMap.put("citation_authors",
          new MetadataField(MetadataField.FIELD_AUTHOR,
              MetadataField.splitAt(";")));
      bookTagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      bookTagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
    }

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
    throws IOException {
      ArticleMetadata am = 
        new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      MultiMap tm = journalTagMap; 
      ArchivalUnit au = cu.getArchivalUnit();

      if((au == null) || (au.getTitleConfig() == null))
        am.cook(tm); //am.cook(journalTagMap);
      else {       
        try {
          String type = cu.getArchivalUnit().getTitleConfig().getProperties().get("type");
          if (type == null) type = "journal";
          if((type.compareToIgnoreCase("book") == 0) || (type.compareToIgnoreCase("bookSeries") == 0))
            tm = bookTagMap; //am.cook(bookTagMap);
          else
            tm = journalTagMap; //am.cook(journalTagMap);
        } catch (Exception e) {
          log.warning("tdb Type not set for AU");
          // use the default(journal) tagmap
        } finally {
          log.debug3("extract: type=["+log+"]");
          am.cook(tm);
        }
      }

      emitter.emitMetadata(cu, am);
    }
  }
}