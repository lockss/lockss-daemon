/*
 * $Id$
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

package org.lockss.plugin.bmc;

import java.io.*;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.extractor.MetadataField.Extractor;
import org.lockss.plugin.*;

public class BMCPluginHtmlMetadataExtractorFactory implements
    FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("BMCPluginMetadataExtractorFactory");

  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new BMCPluginHtmlMetadataExtractor();
  }

  public static class BMCPluginHtmlMetadataExtractor 
    extends SimpleHtmlMetaTagMetadataExtractor {

    // Map BMC-specific HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      //general pattern for capturing start and end page number. 
      // tagMap.put("dc.creator", MetadataField.DC_FIELD_CONTRIBUTOR);
      // <meta name="citation_authors"
      // content="G Poelmans, J K Buitelaar, D L Pauls, B Franke" />
      //tagMap.put("citation_authors", new MetadataField(
      //    MetadataField.FIELD_AUTHOR, MetadataField.splitAt(";")));
      tagMap.put("citation_author", new MetadataField(MetadataField.FIELD_AUTHOR, MetadataField.splitAt(",")));
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_journal_title", MetadataField.FIELD_JOURNAL_TITLE);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);    
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_date", MetadataField.FIELD_DATE);
      tagMap.put("prism.issn", MetadataField.FIELD_ISSN);
      tagMap.put("dc.title", MetadataField.FIELD_JOURNAL_TITLE); 
      tagMap.put("dc.creator", MetadataField.DC_FIELD_CREATOR); 
      tagMap.put("dc.type", MetadataField.DC_FIELD_TYPE); 
      tagMap.put("dc.source", MetadataField.DC_FIELD_SOURCE); 
      tagMap.put("dc.date", MetadataField.DC_FIELD_DATE); 
      tagMap.put("dc.identifier", MetadataField.DC_FIELD_IDENTIFIER); 
      tagMap.put("dc.publisher", MetadataField.DC_FIELD_PUBLISHER); 
      tagMap.put("dc.rights", MetadataField.DC_FIELD_RIGHTS); 
      tagMap.put("dc.format", MetadataField.DC_FIELD_FORMAT); 
      tagMap.put("dc.language", MetadataField.DC_FIELD_LANGUAGE);
      /*
       tagMap.put("citation_mjid", new MetadataField(
         MetadataField.FIELD_PROPRIETARY_IDENTIFIER, 
         MetadataField.extract("^([^;]+);", 1)));
       */
    }

    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
      throws IOException {
      ArticleMetadata am = super.extract(target, cu);
      am.cook(tagMap);
      return am;
    }
  }
}
 
