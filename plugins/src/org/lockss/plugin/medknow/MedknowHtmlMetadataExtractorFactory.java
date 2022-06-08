/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

      /* access_url and publisher all checked by BaseArticleMetadata against tdb */
      am.putIfBetter(MetadataField.FIELD_PUBLISHER,MEDKNOW_PUBNAME);

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

      // add http to https transition handling
      HttpHttpsUrlHelper helper = new HttpHttpsUrlHelper(cu.getArchivalUnit(),
          ConfigParamDescr.BASE_URL.getKey());
      for (MetadataField field : Arrays.asList(MetadataField.DC_FIELD_IDENTIFIER,
          MetadataField.FIELD_ACCESS_URL)) {
        switch (field.getCardinality()) {
          case Multi:
            List<String> entries = am.getList(field);
            List<String> newEntries = new ArrayList<>();
            // deep copy the list so that we can clear it
            Iterator<String> iter = entries.iterator();
            while (iter.hasNext()) {
              String s = iter.next();
              newEntries.add(s);
            }
            // clear the original list, which will clear the ArticleMetadata List
            entries.clear();
            // normalize any urls found in the list.
            for (String each : newEntries) {
              // check if it is a url, not all entries are urls.
              if (each.contains("http")) {
                each = helper.normalize(each);
              }
            }
            am.put(field, newEntries.toString());
          case Single:
            String url = am.get(field);
            if (url != null) {
              url = helper.normalize(url);
              am.replace(field, url);
            }
        }
      }

      return am;
      
    } /** end extract */
    
  } /** end class MedknowHtmlMetadataExtractor */
  
} /** end class MedknowHtmlMetadataExtractorFactory */
 
