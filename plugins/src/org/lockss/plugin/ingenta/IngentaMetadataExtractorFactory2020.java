/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.ingenta;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IngentaMetadataExtractorFactory2020
  implements FileMetadataExtractorFactory {

  static Logger log = 
    Logger.getLogger(IngentaArticleIteratorFactory2020.class);

  private static Pattern doiPattern = Pattern.compile("info:doi/(.*)");
  private static Pattern issnPattern = Pattern.compile("urn:ISSN:(.*)");

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
          throws PluginException {
    return new IngentaMetadataExtractor();
  }

  /*
  <meta name="DC.title" content="Rhinorrhea as a Result of Alzheimer&#039;s Disease Treatment"/>
  <meta name="DC.type" content="Text"/>
  <meta name="DC.publisher" content="American Society of Consultant Pharmacists"/>
  <meta name="DC.creator" content="Vouri, Scott Martin"/>
  <meta name="DC.identifier" content="info:doi/10.4140/TCP.n.2020.148."/>
  <meta name="DCTERMS.issued" content="April 2020"/>
  <meta name="DCTERMS.bibliographicCitation" content="The Senior Care Pharmacist, 35, 4, 148-149(2)"/>
  <meta name="DCTERMS.isPartOf" content="urn:ISSN:2639-9636"/>
  <meta name="IC.identifier" content="ascp/tscp/2020/00000035/00000004"/>
  <meta name="CRAWLER.fullTextLink" content="https://api.ingentaconnect.com/content/ascp/tscp/2020/00000035/00000004?crawler=true"/>
   */

  public static class IngentaMetadataExtractor
          extends SimpleHtmlMetaTagMetadataExtractor {
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("DC.title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("DC.publisher", MetadataField.FIELD_PUBLICATION_TITLE);
      tagMap.put("DC.creator", MetadataField.FIELD_AUTHOR);
      tagMap.put("DC.identifier", new MetadataField(
              MetadataField.FIELD_DOI, MetadataField.extract(doiPattern,1)));
      tagMap.put("DCTERMS.isPartOf", new MetadataField(
              MetadataField.FIELD_ISSN, MetadataField.extract(issnPattern,1)));
      tagMap.put("DCTERMS.issued", MetadataField.FIELD_DATE);
      
    }

    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
            throws IOException {
      ArticleMetadata am = super.extract(target, cu);
      am.cook(tagMap);
      String url = cu.getUrl();

      String html_appendix = "?crawler=true&mimetype=text/html";
      String new_access_url = url;
      ArchivalUnit au = cu.getArchivalUnit();

      CachedUrl potential_cu = cu.getArchivalUnit().makeCachedUrl(new_access_url);

      if (url != null && !url.contains(html_appendix)) {
        new_access_url = url + "?crawler=true&mimetype=text/html";
      }

      if ( (potential_cu != null) && (potential_cu.hasContent()) ){
        if (am.get(MetadataField.FIELD_ACCESS_URL) == null) {
          am.put(MetadataField.FIELD_ACCESS_URL, new_access_url);
        } else {
          am.replace(MetadataField.FIELD_ACCESS_URL, new_access_url);
        }
      } 
      return am;
    }
  }
}