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

package org.lockss.plugin.georgthiemeverlag;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


/**
 * Thin class to extract DOI from filename
 * There are some PDF only articles like 
 * https://www.thieme-connect.de/ejournals/pdf/10.1055/s-0032-1321390.pdf
 * see TOC https://www.thieme-connect.de/ejournals/issue/10.1055/s-002-23551
 * 
 * Note: It appears that a hidden abstract is available for PDF only
 * see https://www.thieme-connect.de/ejournals/abstract/10.1055/s-0032-1321389
 * Therefore another approach would be to create subclass of GoslingHtmlLinkExtractor
 * like GeorgThiemeVerlagHtmlLinkExtractor, whose only purpose would be to add an
 * abstract url for all pdf links, just in case it existed
 */
public class GeorgThiemeVerlagPdfMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(GeorgThiemeVerlagPdfMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
      throws PluginException {
    return new GeorgThiemeVerlagPdfMetadataExtractor();
  }
  
  public static class GeorgThiemeVerlagPdfMetadataExtractor
    extends SimpleFileMetadataExtractor {
    
    protected static final Pattern DOI_PAT = 
        Pattern.compile("/pdf/(10[.][^/]*/[^.]*)[.]pdf");
    
    private static MultiMap tagMap = new MultiValueMap();
    static {
      //<meta name="citation_doi" content="10.1055/s-0...
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
    }
    
    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
        throws IOException {
      ArticleMetadata am = new ArticleMetadata();
      Matcher m = DOI_PAT.matcher(cu.getUrl());
      if (m.find()) {
        String doi = m.group(1);
        am.putRaw("citation_doi", doi);
        am.cook(tagMap);
      }
      // PD-440
      // am.replace(MetadataField.FIELD_PUBLISHER, "Georg Thieme Verlag KG");
      return am;
    }
  }
}
