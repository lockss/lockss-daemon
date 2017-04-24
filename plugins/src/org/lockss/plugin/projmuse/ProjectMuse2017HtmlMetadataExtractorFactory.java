/*
 * $Id:  $
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.projmuse;

import java.io.*;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


public class ProjectMuse2017HtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType)
          throws PluginException {
    return new ProjectMuseHtmlMetadataExtractor();
  }
  
  public static class ProjectMuseHtmlMetadataExtractor 
    implements FileMetadataExtractor {
    
    // Map HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_online_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_fulltext_html_url", MetadataField.FIELD_ACCESS_URL);
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
    }
    
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {
      ArticleMetadata am = 
        new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);
      String url = am.get(MetadataField.FIELD_ACCESS_URL);
      ArchivalUnit au = cu.getArchivalUnit();
      if (url == null || url.isEmpty() || !au.makeCachedUrl(url).hasContent()) {
        url = cu.getUrl();
      }
      am.replace(MetadataField.FIELD_ACCESS_URL,
                 AuUtil.normalizeHttpHttpsFromBaseUrl(au, url));
      emitter.emitMetadata(cu, am);
    }
  }
}