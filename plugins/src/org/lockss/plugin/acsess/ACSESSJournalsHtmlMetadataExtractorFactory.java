/*

 * $Id$
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

package org.lockss.plugin.acsess;

import java.io.*;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

public class ACSESSJournalsHtmlMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  
  static Logger log = Logger.getLogger(
      ACSESSJournalsHtmlMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new ACSESSJournalsHtmlMetadataExtractor();
  }

  public static class ACSESSJournalsHtmlMetadataExtractor 
    extends SimpleHtmlMetaTagMetadataExtractor {
    
    // Map ACSESS Journals HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();

    /*
      <meta content="text/html" name="DC.Format"/>
      <meta content="en" name="DC.Language"/>
      <meta content="Protecting bee health through integrated pest, crop, and landscape management" name="DC.Title"/>
      <meta content="10.2134/cs2014-47-3-1" name="DC.Identifier"/>
      <meta content="--" name="DC.Date"/>
      <meta content="American Society of Agronomy" name="DC.Publisher"/>
      <meta content="Tanner Ehmke" name="DC.Contributor"/>
      <meta content="Crops and Soils" name="citation_journal_title"/>
      <meta content="1940-3372" name="citation_issn"/>
      <meta content="https://dl.sciencesocieties.org/publications/cns/pdfs/47/3/4" name="citation_pdf_url"/>
      <meta content="" name="citation_fulltext_world_readable"/>
      <meta content="Ehmke, Tanner" name="citation_author"/>
      <meta content="" name="citation_author_institution"/>
      <meta content="Protecting bee health through integrated pest, crop, and landscape management" name="citation_title"/>
      <meta content="2014/5-6/01" name="citation_publication_date"/>
      <meta content="47" name="citation_volume"/>
      <meta content="3" name="citation_issue"/>
      <meta content="4" name="citation_firstpage"/>
      <meta content="11" name="citation_lastpage"/>
      <meta content="47/3/4" name="citation_id"/>
      <meta content="CNS;47/3/4" name="citation_mjid"/>
      <meta content="10.2134/cs2014-47-3-1" name="citation_doi"/>
    */
    static {
      tagMap.put("DC.Format", MetadataField.DC_FIELD_FORMAT);
      tagMap.put("DC.Language", MetadataField.DC_FIELD_LANGUAGE);
      tagMap.put("DC.Identifier", MetadataField.DC_FIELD_IDENTIFIER);
      tagMap.put("DC.Publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put("DC.Contributor", MetadataField.DC_FIELD_CONTRIBUTOR);
      tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);  
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
    } // static
    
    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
      throws IOException {
      log.debug3("Metadata - cachedurl cu:" + cu.getUrl());
      ArticleMetadata am = super.extract(target, cu);
      am.cook(tagMap);
      return am;
    }
    
  }
  
}
 
