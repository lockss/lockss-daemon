/*

Copyright (c) 2000-2020, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.copernicus;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.config.TdbAu;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.extractor.FileMetadataExtractor.Emitter;
import org.lockss.plugin.*;


public class CopernicusHtmlMetadataExtractorFactory 
implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(CopernicusHtmlMetadataExtractorFactory.class);

  public FileMetadataExtractor 
  createFileMetadataExtractor(MetadataTarget target, String contentType)
      throws PluginException {
    return new CopernicusHtmlMetadataExtractor();
  }

  public static class CopernicusHtmlMetadataExtractor 
  implements FileMetadataExtractor {

    // Map Google Scholar HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("citaton_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_authors",
          new MetadataField(MetadataField.FIELD_AUTHOR,
              MetadataField.splitAt(";")));
      tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);

      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);

      tagMap.put("citation_journal_title", MetadataField.FIELD_JOURNAL_TITLE);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);

    }
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException, PluginException {
      ArticleMetadata am =
          new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);
      emitter.emitMetadata(cu, am);
    }
  }
}