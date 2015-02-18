/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
