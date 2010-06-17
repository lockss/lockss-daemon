/*
 * $Id: ElsevierXmlMetadataExtractorFactory.java,v 1.1 2010-06-17 18:41:27 tlipkis Exp $
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

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

public class ElsevierXmlMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  public FileMetadataExtractor
    createFileMetadataExtractor(String contentType)
      throws PluginException {
    return new ElsevierXmlMetadataExtractor();
  }

  public static class ElsevierXmlMetadataExtractor
    extends SimpleXmlMetadataExtractor {
    static Logger log = Logger.getLogger("ElsevierXmlMetadataExtractor");

    private static final Map<String, String> tagMap =
      new HashMap<String, String>();
    static {
      tagMap.put("ce:doi", "dc.Identifier");
    };

    public ElsevierXmlMetadataExtractor() {
      super(tagMap);
    }

    public Metadata extract(CachedUrl cu) throws IOException {
      try {
	Metadata ret = super.extract(cu);
	// Elsevier doesn't prefix the DOI in dc.Identifier with doi:
	String content = ret.getProperty("dc.Identifier");
	if (content != null) {
	  ret.putDOI(content);
	}
	return ret;
      } finally {
	AuUtil.safeRelease(cu);
      }	
    }
  }
}
