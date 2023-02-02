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

package org.lockss.plugin.nationalweatherassociation;

import java.io.IOException;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractorFactory;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.extractor.SimpleHtmlMetaTagMetadataExtractor;
import org.lockss.plugin.CachedUrl;

public class NationalWeatherAssociationHtmlMetadataExtractorFactory
  implements FileMetadataExtractorFactory {

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new NationalWeatherAssociationHtmlMetadataExtractor();
  }

  public static class NationalWeatherAssociationHtmlMetadataExtractor 
    extends SimpleHtmlMetaTagMetadataExtractor {

    // Map National Weather Association-specific HTML meta tag names 
    // to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      // <meta name="title" content="National Weather Association" />
      // journal title is set in BaseArticleMetadataExtractor 
      // using MetadataField.FIELD_PUBLICATION_TITLE, 
      // getting title from tdb, in this case, better journal title 
      // 'Journal of Operational Meteorology'

      // <meta name="creator" content="NWA IT Committee" />
      tagMap.put("creator", MetadataField.FIELD_AUTHOR);
      // <meta name="date.created" scheme="ISO8601" content="2007-01-18" />
      tagMap.put("date.created", MetadataField.FIELD_DATE);
      // <meta name="language" scheme="DCTERMS.RFC1766" content="EN-US" />
      tagMap.put("language", MetadataField.FIELD_LANGUAGE);
    }

    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl absCu)
	throws IOException {
      ArticleMetadata am = super.extract(target, absCu);
      am.cook(tagMap);
      return am;
    }
  }
}
