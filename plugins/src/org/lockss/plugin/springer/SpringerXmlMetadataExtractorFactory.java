/*
 * $Id: SpringerXmlMetadataExtractorFactory.java,v 1.1 2010-06-17 18:41:27 tlipkis Exp $
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

package org.lockss.plugin.springer;
import java.io.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

public class SpringerXmlMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("SpringerXmlMetadataExtractorFactory");

  public FileMetadataExtractor
    createFileMetadataExtractor(String contentType) throws PluginException {
    return new SpringerXmlMetadataExtractor();
  }

  private static final String VOLUME_START = "LOCKSS.Springer.VolumeStart";
  private static final String VOLUME_END = "LOCKSS.Springer.VolumeEnd";
  private static final String ISSUE_START = "LOCKSS.Springer.IssueStart";
  private static final String ISSUE_END = "LOCKSS.Springer.IssueEnd";
  private static final Map<String, String> tagMap =
    new HashMap<String, String>();
  static {
    tagMap.put("articledoi", "dc.Identifier");
    tagMap.put("JournalPrintISSN", Metadata.KEY_ISSN);
    tagMap.put("VolumeIDStart", VOLUME_START);
    tagMap.put("VolumeIDEnd", VOLUME_END);
    tagMap.put("IssueIDStart", ISSUE_START);
    tagMap.put("IssueIDEnd", ISSUE_END);
    tagMap.put("ArticleFirstPage", Metadata.KEY_START_PAGE);
  };


  public static class SpringerXmlMetadataExtractor
    extends SimpleXmlMetadataExtractor {

    public SpringerXmlMetadataExtractor() {
      super(tagMap);
    }

    public Metadata extract(CachedUrl xmlCu) throws IOException {
      Metadata ret = super.extract(xmlCu);
      // Springer doesn't prefix the DOI in dc.Identifier with doi:
      String doi = ret.getProperty("dc.Identifier");
      if (doi != null) {
	log.debug3(xmlCu.getUrl() + " DOI " + doi);
	ret.putDOI(doi);
      } else {
	log.debug3(xmlCu.getUrl() + " no DOI");
      }
      String start = ret.getProperty(VOLUME_START);
      String end = ret.getProperty(VOLUME_END);
      if (start != null) {
	if (end != null) {
	  if (end.equals(start)) {
	    log.debug3(xmlCu.getUrl() + " " + VOLUME_START + "=" + start);
	    ret.putVolume(start);
	  } else {
	    log.debug3(xmlCu.getUrl() + " " + VOLUME_START + " " + start +
		       " " + VOLUME_END + end);
	    ret.putVolume(start + "-" + end);
	  }
	} else {
	  log.debug3(xmlCu.getUrl() + " " + VOLUME_START + " " + start);
	  ret.putVolume(start);
	}
      } else {
	log.debug3(xmlCu.getUrl() + " no " + VOLUME_START);
      }
      start = ret.getProperty(ISSUE_START);
      end = ret.getProperty(ISSUE_END);
      if (start != null) {
	if (end != null) {
	  if (end.equals(start)) {
	    log.debug3(xmlCu.getUrl() + " " + ISSUE_START + "=" + start);
	    ret.putIssue(start);
	  } else {
	    log.debug3(xmlCu.getUrl() + " " + ISSUE_START + " " + start +
		       " " + ISSUE_END + end);
	    ret.putIssue(start + "-" + end);
	  }
	} else {
	  log.debug3(xmlCu.getUrl() + " " + ISSUE_START + " " + start);
	  ret.putIssue(start);
	}
      } else {
	log.debug3(xmlCu.getUrl() + " no " + ISSUE_START);
      }
      return ret;
    }
  }
}
