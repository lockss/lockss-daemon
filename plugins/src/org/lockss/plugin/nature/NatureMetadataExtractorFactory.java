/*
 * $Id: NatureMetadataExtractorFactory.java,v 1.2 2009-06-01 23:48:25 dshr Exp $
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

package org.lockss.plugin.nature;
import java.io.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

public class NatureMetadataExtractorFactory
    implements MetadataExtractorFactory {
  static Logger log = Logger.getLogger("NatureMetadataExtractorFactory");
  /**
   * Create a MetadataExtractor
   * @param contentType the content type type from which to extract URLs
   */
  public MetadataExtractor createMetadataExtractor(String contentType)
      throws PluginException {
    String mimeType = HeaderUtil.getMimeTypeFromContentType(contentType);
    if ("text/html".equalsIgnoreCase(mimeType)) {
      return new NatureMetadataExtractor();
    }
    return null;
  }
  public class NatureMetadataExtractor extends SimpleMetaTagMetadataExtractor {

    public NatureMetadataExtractor() {
    }
    String[] natureField = {
      "dc.creator",
    };
    String[] dublinCoreField = {
      "dc.Contributor",
    };

    public Metadata extract(CachedUrl cu) throws IOException {
      Metadata ret = super.extract(cu);
      for (int i = 0; i < natureField.length; i++) {
	String content = ret.getProperty(natureField[i]);
	if (content != null) {
	  ret.setProperty(dublinCoreField[i], content);
	}
      }
      return ret;
    }
  }
}
