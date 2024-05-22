/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.lbnl;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;

public class NamesforLifeHtmlMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(NamesforLifeHtmlMetadataExtractorFactory.class);

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                               String contentType)
      throws PluginException {
    return new NamesforLifeOaiHtmlMetadataExtractor();
  }

  /*
    <meta name="DCTERMS.rightsHolder" content="NamesforLife" />
      <meta name="DCTERMS.rights" content="All Rights Reserved." />
      <meta name="DCTERMS.type" content="article" />
      <meta property="og:type" content="article" />
      <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no" />
      <meta http-equiv="X-UA-Compatible" content="IE=edge" />
      <title>Taxonomy of the species Pedobacter nototheniae K&auml;mpfer et al. 2020</title>
      <link rel="canonical" href="https://doi.org/10.1601/tx.35745">
      <meta name="DESCRIPTION" content="Taxonomy of the species Pedobacter nototheniae K&auml;mpfer et al. 2020">
      <meta name="KEYWORDS" content="Pedobacter nototheniae,prokaryote,taxon,taxonomy,article,abstract,micropublication">
      <meta name="DCTERMS.title" content="Taxonomy of the species Pedobacter nototheniae K&auml;mpfer et al. 2020">
      <meta name="DCTERMS.identifier" content="10.1601/tx.35745">
      <meta content="text/html" name="DCTERMS.format">
      <meta content="en" name="DCTERMS.language">
      <meta content="NamesforLife, LLC" name="DCTERMS.publisher">
      <meta name="DCTERMS.dateCopyrighted" content="2019-10-30">
      <meta id="datePublished" name="datePublished" content="2019-10-30">
      <meta id="dateModified" name="dateModified" content="2019-10-30">
   */

  public static class NamesforLifeOaiHtmlMetadataExtractor
    extends SimpleHtmlMetaTagMetadataExtractor {
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("DCTERMS.identifier", MetadataField.FIELD_DOI);
      tagMap.put("DCTERMS.title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("datePublished", MetadataField.FIELD_DATE);

    }

    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
    throws IOException {
      ArticleMetadata am = super.extract(target, cu);
      am.cook(tagMap);

      String originalUrl = am.get(MetadataField.FIELD_ACCESS_URL);
      String url = "";

      log.debug3("access_url === " + originalUrl);

      ArchivalUnit au = cu.getArchivalUnit();
      if (originalUrl == null || originalUrl.isEmpty() || !au.makeCachedUrl(originalUrl).hasContent()) {
        url = cu.getUrl();
        log.debug3("access_url failed === " + url);
      }

      log.debug3("access_url repalced with === " + originalUrl);

      //Still use the originalUrl here, since the PDF link has some strange extra string at the end
      am.replace(MetadataField.FIELD_ACCESS_URL, originalUrl);
      return am;
    }
  }
}
