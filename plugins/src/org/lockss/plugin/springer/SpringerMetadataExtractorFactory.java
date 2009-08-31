/*
 * $Id: SpringerMetadataExtractorFactory.java,v 1.1 2009-08-31 16:31:03 dshr Exp $
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

package org.lockss.plugin.springer;
import java.io.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

public class SpringerMetadataExtractorFactory
    implements MetadataExtractorFactory {
  static Logger log = Logger.getLogger("SpringerMetadataExtractorFactory");
  private static final Map<String, String> tagMap =
    new HashMap<String, String>();
  static {
    tagMap.put("articledoi", Metadata.KEY_DOI);
  };

  /**
   * Create a MetadataExtractor
   * @param contentType the content type type from which to extract URLs
   */
  public MetadataExtractor createMetadataExtractor(String contentType)
      throws PluginException {
    String mimeType = HeaderUtil.getMimeTypeFromContentType(contentType);
    if ("application/pdf".equalsIgnoreCase(mimeType)) {
      return new SpringerMetadataExtractor();
    }
    return null;
  }

  private static final String part1 = "/BodyRef/PDF";
  private static final String part2 = "\\.pdf";
  private static final String regex = ".*" + part1 + "/.*" + part2;

  public class SpringerMetadataExtractor extends SimpleXmlMetadataExtractor {

    public SpringerMetadataExtractor() {
      super(tagMap);
    }

    public Metadata extract(CachedUrl cu) throws IOException {
      // cu points to a file whose name is .../main.pdf
      // but the metadata we want is in a file whose name is .../main.xml
      Metadata ret = null;
      String pdfUrl = cu.getUrl();
      if (pdfUrl.matches(regex)) {
	String xmlUrl =
	  pdfUrl.replaceFirst(part1, "").replaceFirst(part2, ".xml.Meta");
	CachedUrl xmlCu = cu.getArchivalUnit().makeCachedUrl(xmlUrl);
	if (xmlCu != null && xmlCu.hasContent()) {
	  ret = super.extract(xmlCu);
	  // Springer doesn't prefix the DOI in dc.Identifier with doi:
	  String content = ret.getProperty(Metadata.KEY_DOI);
	  if (content != null && !content.startsWith(Metadata.PROTOCOL_DOI)) {
	    ret.setProperty(Metadata.KEY_DOI, Metadata.PROTOCOL_DOI + content);
	  }
	} else {
	  if (xmlCu == null) {
	    log.debug("xmlCu is null");
	  } else {
	    log.debug(xmlCu.getUrl() + " no content");
	  }
	}
      } else {
	log.debug(pdfUrl + " doesn't match " + regex);
      }
      return ret;
    }
  }
}
