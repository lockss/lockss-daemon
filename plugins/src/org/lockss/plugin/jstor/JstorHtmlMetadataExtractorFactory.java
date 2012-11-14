/*
 * $Id: JstorHtmlMetadataExtractorFactory.java,v 1.1 2012-11-14 23:03:59 wkwilson Exp $
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

package org.lockss.plugin.jstor;

import java.io.*;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


/*
 * <link rel="schema.DC" href="http://purl.org/DC/elements/1.0/" />
 * <meta name="dc.Title" content="Teaching Aids" />
 * <meta name="dc.Creator" content=" W. A. Bousfield " />
 * <meta name="dc.Creator" content="M. L." />
 * <meta name="dc.Publisher" content=" National Association of Biology Teachers " />
 * <meta name="dc.Date" scheme="WTN8601" content="Aug 19, 2008" />
 * <meta name="dc.Type" content="research-article" />
 * <meta name="dc.Format" content="text/HTML" />
 * <meta name="dc.Identifier" scheme="jstor" content="4436970" />
 * <meta name="dc.Identifier" scheme="legacy-jstor" content="AP007055 00027685 AP080011 08A00040" />
 * <meta name="dc.Language" content="EN" />
 * <meta name="dc.Coverage" content="world" />
 * <meta name="dc.Identifier" scheme="doi" content="10.1525/abt.2010.72.9.1" />
 */

public class JstorHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("JstorHtmlMetadataExtractorFactory");

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new JstorHtmlMetadataExtractor();
  }

  public static class JstorHtmlMetadataExtractor
    extends SimpleHtmlMetaTagMetadataExtractor {

    // Map HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("dc.Title", MetadataField.DC_FIELD_TITLE);
      tagMap.put("dc.Publisher", MetadataField.DC_FIELD_PUBLISHER);
      tagMap.put("dc.Creator", MetadataField.DC_FIELD_CREATOR);
      tagMap.put("dc.Date", MetadataField.DC_FIELD_DATE);
      tagMap.put("dc.Type", MetadataField.DC_FIELD_TYPE);
      tagMap.put("dc.Identifier", MetadataField.DC_FIELD_IDENTIFIER);
    }

    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
	throws IOException {
      ArticleMetadata am = super.extract(target, cu);
      am.cook(tagMap);
      return am;
    }
  }

}
