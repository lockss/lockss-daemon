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

package org.lockss.plugin.elsevier;

import java.io.*;
import java.util.*;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

public class ElsevierXmlMetadataExtractorFactory
  implements FileMetadataExtractorFactory {

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new ElsevierXmlMetadataExtractor();
  }

  public static class ElsevierXmlMetadataExtractor
    extends SimpleFileMetadataExtractor {
    static Logger log = Logger.getLogger("ElsevierXmlMetadataExtractor");

    private static MultiMap tagMap = new MultiValueMap();
    static {
      // Elsevier doesn't prefix the DOI in dc.Identifier with doi:
      tagMap.put("ce:doi", MetadataField.DC_FIELD_IDENTIFIER);
      tagMap.put("ce:doi", MetadataField.FIELD_DOI);
    };

    public ElsevierXmlMetadataExtractor() {
    }

    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
	throws IOException, PluginException {
      SimpleFileMetadataExtractor extr = new SimpleXmlMetadataExtractor(tagMap);
      ArticleMetadata am = extr.extract(target, cu);
      // extract metadata from BePress specific metadata tags
      am.cook(tagMap);
      return am;
    }
  }
}
