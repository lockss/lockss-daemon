/*
 * $Id: HighWireMetadataExtractorFactory.java,v 1.3 2009-10-09 00:41:36 thib_gc Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire;
import java.io.*;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

public class HighWireMetadataExtractorFactory
    implements MetadataExtractorFactory {
  static Logger log = Logger.getLogger("HighWireMetadataExtractorFactory");
  /**
   * Create a MetadataExtractor
   * @param contentType the content type type from which to extract URLs
   */
  public MetadataExtractor createMetadataExtractor(String contentType)
      throws PluginException {
    String mimeType = HeaderUtil.getMimeTypeFromContentType(contentType);
    if ("text/html".equalsIgnoreCase(mimeType)) {
      return new HighWireMetadataExtractor();
    }
    return null;
  }
  public class HighWireMetadataExtractor extends SimpleMetaTagMetadataExtractor {

    public HighWireMetadataExtractor() {
    }

    public Metadata extract(CachedUrl cu) throws IOException {
      final String reprintPrefix = "/cgi/reprint/";
      final String reprintframedPrefix = "/cgi/reprintframed/";
      
      Metadata ret = null;
      String reprintUrl = cu.getUrl();
      if (reprintUrl.contains(reprintPrefix)) {
        String reprintframedUrl = reprintUrl.replaceFirst(reprintPrefix, reprintframedPrefix);
        CachedUrl reprintframedCu = cu.getArchivalUnit().makeCachedUrl(reprintframedUrl);
        try {
          if (reprintframedCu != null && reprintframedCu.hasContent()) {
            ret = super.extract(reprintframedCu);
            // HighWire doesn't prefix the DOI in dc.Identifier with doi:
            String content = ret.getProperty(Metadata.KEY_DOI);
            if (content != null && !content.startsWith(Metadata.PROTOCOL_DOI)) {
              ret.setProperty(Metadata.KEY_DOI, Metadata.PROTOCOL_DOI
                      + content);
            }
          }
        }
        finally {
          AuUtil.safeRelease(reprintframedCu);
        }        
      }
      
      return ret;
    }
  }
}
