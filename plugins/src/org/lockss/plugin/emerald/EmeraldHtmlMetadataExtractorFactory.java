/*
 * $Id$
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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