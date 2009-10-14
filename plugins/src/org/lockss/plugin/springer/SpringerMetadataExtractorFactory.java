/*
 * $Id: SpringerMetadataExtractorFactory.java,v 1.4 2009-10-14 21:43:07 dshr Exp $
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
  private static final String VOLUME_START = "LOCKSS.Springer.VolumeStart";
  private static final String VOLUME_END = "LOCKSS.Springer.VolumeEnd";
  private static final String ISSUE_START = "LOCKSS.Springer.IssueStart";
  private static final String ISSUE_END = "LOCKSS.Springer.IssueEnd";
  private static final Map<String, String> tagMap =
    new HashMap<String, String>();
  static {
    tagMap.put("articledoi", "dc.Identifier");
    tagMap.put("articledoi", Metadata.KEY_DOI);
    tagMap.put("JournalPrintISSN", Metadata.KEY_ISSN);
    tagMap.put("VolumeIDStart", VOLUME_START);
    tagMap.put("VolumeIDEnd", VOLUME_END);
    tagMap.put("IssueIDStart", ISSUE_START);
    tagMap.put("IssueIDEnd", ISSUE_END);
    tagMap.put("ArticleFirstPage", Metadata.KEY_START_PAGE);
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
      // cu points to a file whose name is ....pdf
      // but the metadata we want is in a file whose name is ....xml.Meta
      Metadata ret = null;
      String pdfUrl = cu.getUrl();
      if (pdfUrl.matches(regex)) {
	String doi = null;
	String xmlUrl =
	  pdfUrl.replaceFirst(part1, "").replaceFirst(part2, ".xml.Meta");
	CachedUrl xmlCu = cu.getArchivalUnit().makeCachedUrl(xmlUrl);
	if (xmlCu == null || !xmlCu.hasContent()) {
	  if (xmlCu == null) {
	    log.debug("xmlCu is null");
	  } else {
	    log.debug(xmlCu.getUrl() + " no content");
	  }
	  xmlUrl = 
	    pdfUrl.replaceFirst(part1, "").replaceFirst(part2, ".xml.meta");
	  xmlCu = cu.getArchivalUnit().makeCachedUrl(xmlUrl);
	}
	try {
	  if (xmlCu != null || xmlCu.hasContent()) {
	    ret = super.extract(xmlCu);
	    // Springer doesn't prefix the DOI in dc.Identifier with doi:
	    doi = ret.getProperty("dc.Identifier");
	    if (doi != null) {
	      ret.putDOI(doi);
	    }
	    String start = ret.get(VOLUME_START);
	    String end = ret.get(VOLUME_END);
	    if (start != null) {
	      if (end != null && end.equals(start)) {
		ret.putVolume(start);
	      } else {
		ret.putVolume(start + "-" + end);
	      }
	    }
	    start = ret.get(ISSUE_START);
	    end = ret.get(ISSUE_END);
	    if (start != null) {
	      if (end != null && end.equals(start)) {
		ret.putIssue(start);
	      } else {
		ret.putIssue(start + "-" + end);
	      }
	    }
	  } else {
	    if (xmlCu == null) {
	      log.debug("xmlCu is null");
	    } else {
	      log.debug(xmlCu.getUrl() + " no content");
	    }
	  }
	}
	finally {
	  AuUtil.safeRelease(xmlCu);
	}
      } else {
	log.debug(pdfUrl + " doesn't match " + regex);
      }
      return ret;
    }
  }
}
