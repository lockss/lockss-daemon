/**
 * $Id$
 */

/**

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

package org.lockss.plugin.medknow;

import java.io.*;
import java.util.*;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

/**
 * MedknowHtmlMetadataExtractorFactory extracts metadata from each article.
 * The first choice is the RIS file, but if that doesn't exist, it uses this one
 */
public class MedknowHtmlMetadataExtractorFactory implements
    FileMetadataExtractorFactory {
  
  private static final Logger log = Logger.getLogger(MedknowHtmlMetadataExtractorFactory.class);
  private static final String MEDKNOW_PUBNAME = "Medknow Publications";


  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    
    return new MedknowHtmlMetadataExtractor();
    
  } // createFileMetadataExtractor

  public static class MedknowHtmlMetadataExtractor 
    extends SimpleHtmlMetaTagMetadataExtractor {

    // Map Medknow-specific HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    
    static {
      
      tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
      tagMap.put("citation_authors",
          new MetadataField(MetadataField.FIELD_AUTHOR,
              MetadataField.splitAt(";")));
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_public_url", MetadataField.FIELD_ACCESS_URL);
      tagMap.put("citation_pdf_url", MetadataField.FIELD_ACCESS_URL);
      tagMap.put("DC.Format", MetadataField.DC_FIELD_FORMAT);
      tagMap.put("DC.Identifier", MetadataField.DC_FIELD_IDENTIFIER);
      tagMap.put("DC.Subject", MetadataField.DC_FIELD_SUBJECT);
      tagMap.put("DC.Description", MetadataField.DC_FIELD_DESCRIPTION);
            
    } // static

    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
      throws IOException {
      
      log.debug3("Metadata - cachedurl cu:" + cu.getUrl());
      
      ArticleMetadata am = super.extract(target, cu);
      am.cook(tagMap);
      
      /** raw metadata:
        dc.identifier: [http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2010;volume=7;issue=2;spage=61;epage=65;aulast=Gupta;type=0; 0189-6725]
        extract the muti-value for this field, check if one of the values
        is valid issn, then set the issn metadata in cooked metadata list. */
      List<String> idList = am.getRawList("DC.Identifier");
      for (String id : idList) {
        if (MetadataUtil.isIssn(id)) {
          am.put(MetadataField.FIELD_ISSN, id);
          break;
        }
      }
      
      /* access_url and publisher all checked by BaseArticleMetadata against tdb */
      am.putIfBetter(MetadataField.FIELD_PUBLISHER,MEDKNOW_PUBNAME);
      
      return am;
      
    } /** end extract */
    
  } /** end class MedknowHtmlMetadataExtractor */
  
} /** end class MedknowHtmlMetadataExtractorFactory */
 
