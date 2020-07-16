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