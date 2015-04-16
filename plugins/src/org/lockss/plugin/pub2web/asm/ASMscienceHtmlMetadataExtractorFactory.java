/*
 * $Id:$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.asm;

import java.io.*;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


public class ASMscienceHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType)
          throws PluginException {
    return new ASMscienceHtmlMetadataExtractor();
  }
  
  /*
   * available from the html abstract article landing page
   * <meta name="author" 
   * <meta name="citation_abstract" 
   * <meta name="citation_abstract_html_url" 
   * <meta name="citation_author" 
   * <meta name="citation_author_institution" 
   * <meta name="citation_doi" 
   * <meta name="citation_issn" 
   * <meta name="citation_issue" 
   * <meta name="citation_journal_title" 
   * <meta name="citation_publication_date" 
   * <meta name="citation_publisher" 
   * <meta name="citation_reference" 
   * <meta name="citation_title" 
   * <meta name="citation_volume" 
   * <meta name="dc.creator" 
   * <meta name="dc.date" 
   * <meta name="dc.identifier" 
   * <meta name="dc.publisher" 
   * <meta name="dc.title" 
   * <meta name="dc.type" 
   * <meta name="description" 
   */
  public static class ASMscienceHtmlMetadataExtractor 
    implements FileMetadataExtractor {
    
    // Map HighWire HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_date",  MetadataField.FIELD_DATE);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_authors", new MetadataField(
          MetadataField.FIELD_AUTHOR, MetadataField.splitAt(";")));
      tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);

      tagMap.put("dc.date", MetadataField.FIELD_DATE);
      tagMap.put("dc.date", MetadataField.DC_FIELD_DATE);
      tagMap.put("dc.title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("dc.title", MetadataField.DC_FIELD_TITLE);
      tagMap.put("dc.creator", MetadataField.FIELD_AUTHOR);
      tagMap.put("dc.creator", MetadataField.DC_FIELD_CREATOR);
      tagMap.put("dc.identifier", MetadataField.DC_FIELD_IDENTIFIER);
      tagMap.put("dc.type", MetadataField.DC_FIELD_TYPE);
    }

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {
      ArticleMetadata am =
          new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);
      // leave this method here in case we need to make modifications 
      // note that access.url isn't set to allow for default full_text_cu value (pdf)
      emitter.emitMetadata(cu, am);
    }
    
  }
}