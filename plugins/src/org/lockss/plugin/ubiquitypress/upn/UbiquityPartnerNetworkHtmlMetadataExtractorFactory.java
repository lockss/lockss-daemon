/*

Copyright (c) 2000-2025, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.ubiquitypress.upn;

import java.io.*;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

public class UbiquityPartnerNetworkHtmlMetadataExtractorFactory implements
    FileMetadataExtractorFactory {
  
  static Logger log = Logger.getLogger(UbiquityPartnerNetworkHtmlMetadataExtractorFactory.class);
  static Pattern doiPat = Pattern.compile("^10\\.[0-9]+/");

  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    
    return new UbiquityPartnerNetworkHtmlMetaTagMetadataExtractor();
    
  } // createFileMetadataExtractor

  public static class UbiquityPartnerNetworkHtmlMetaTagMetadataExtractor 
    extends SimpleHtmlMetaTagMetadataExtractor {
    
    private static MultiMap tagMap = new MultiValueMap();

    //https://www.gewina-studium.nl/articles/10.18352/studium.10199/
    
    static {
      
      tagMap.put("DC.Format", MetadataField.DC_FIELD_FORMAT);
      tagMap.put("DC.Language", MetadataField.DC_FIELD_LANGUAGE);
      tagMap.put("DC.Title", MetadataField.DC_FIELD_TITLE);
      //Some UPN Articles don't have DOIs so check the format of DC.Identifier before labeling as a DOI in the metadata. 
      //tagMap.put("DC.Identifier", MetadataField.FIELD_DOI);
      tagMap.put("DC.Date", MetadataField.DC_FIELD_DATE);
      // In case DC.Date is null, use DC.Date.issued, like in  https://www.gewina-studium.nl/articles/10.18352/studium.10199/
      tagMap.put("DC.Date.issued", MetadataField.DC_FIELD_DATE);
      tagMap.put("DC.Publisher", MetadataField.DC_FIELD_PUBLISHER);
      tagMap.put("DC.Publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put("DC.Contributor", MetadataField.DC_FIELD_CONTRIBUTOR);
      tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      //tagMap.put("citation_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
      tagMap.put("citation_authors",
          new MetadataField(MetadataField.FIELD_AUTHOR,
              MetadataField.splitAt(";")));
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_volume", MetadataField.DC_FIELD_CITATION_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_issue", MetadataField.DC_FIELD_CITATION_ISSUE);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_public_url", MetadataField.FIELD_ACCESS_URL);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      
    }
    
    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
      throws IOException {
      
      log.debug3("Metadata - cachedurl cu:" + cu.getUrl());
      
      ArticleMetadata am = super.extract(target, cu);
      am.cook(tagMap);
      String url = am.get(MetadataField.FIELD_ACCESS_URL);
      String doi = am.getRaw("DC.Identifier");
      if(doi != null){
        Matcher doiMat = doiPat.matcher(doi);
        if(doiMat.find()){
          am.put(MetadataField.FIELD_DOI, doi);
        }
      }
      ArchivalUnit au = cu.getArchivalUnit();
      if (url == null || url.isEmpty() || !au.makeCachedUrl(url).hasContent()) {
        url = cu.getUrl();
      }
      am.replace(MetadataField.FIELD_ACCESS_URL,
                 AuUtil.normalizeHttpHttpsFromBaseUrl(au, url));
      return am;
      
    }
    
  }
  
}
 
