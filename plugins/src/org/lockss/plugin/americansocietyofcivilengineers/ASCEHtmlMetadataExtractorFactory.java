/*
 * $Id: ASCEHtmlMetadataExtractorFactory.java,v 1.1 2013-04-02 21:16:22 ldoan Exp $
 */

/*

 Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.americansocietyofcivilengineers;

import java.io.*;
import java.util.*;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

/*
 * Extracts metadata from each article.
 */
public class ASCEHtmlMetadataExtractorFactory implements
    FileMetadataExtractorFactory {

  static Logger log = Logger.getLogger(ASCEHtmlMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new ASCEHtmlMetadataExtractor();
  }

  public static class ASCEHtmlMetadataExtractor 
    extends SimpleHtmlMetaTagMetadataExtractor {
    // Map ASCE-specific HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("dc.Title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("dc.Publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put("dc.Date", MetadataField.FIELD_DATE);
      tagMap.put("dc.Format", MetadataField.FIELD_FORMAT);
      tagMap.put("dc.Language", MetadataField.FIELD_LANGUAGE);
      tagMap.put("dc.Coverage", MetadataField.FIELD_COVERAGE);
      tagMap.put("keywords",
          new MetadataField(MetadataField.FIELD_KEYWORDS,
              MetadataField.splitAt(",")));
    }
    
    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
      throws IOException {
      log.debug3("Metadata - cachedurl cu:" + cu.getUrl());
      ArticleMetadata am = super.extract(target, cu);
      am.cook(tagMap);
      // raw metadata:
      // dc.identifier: [http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2010;volume=7;issue=2;spage=61;epage=65;aulast=Gupta;type=0; 0189-6725]
      // extract the muti-value for this field, check if one of the values
      // is valid issn, then set the issn metadata in cooked metadata list.
      List<String> creatorList = am.getRawList("dc.Creator");
      for (String creator : creatorList) {
        am.put(MetadataField.FIELD_AUTHOR, creator);
      }
      List<String> idList = am.getRawList("dc.Identifier");
      for (String id : idList) {
        if (id.contains("10.")) {
          am.put(MetadataField.FIELD_DOI, id);
          break;
        }
      }
      return am;
    }
  }
}
 
