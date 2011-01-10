/*
 * $Id: SpringerXmlMetadataExtractorFactory.java,v 1.3 2011-01-10 09:18:09 tlipkis Exp $
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

  private static final String VOLUME_START = "VolumeIDStart";
  private static final String VOLUME_END = "VolumeIDEnd";
  private static final String ISSUE_START = "IssueIDStart";
  private static final String ISSUE_END = "IssueIDEnd";

  private static final Map tagMap = new HashMap();
  static {
    tagMap.put("articledoi", ListUtil.list(MetadataField.DC_FIELD_IDENTIFIER,
					   MetadataField.FIELD_DOI));
    tagMap.put("JournalPrintISSN", MetadataField.KEY_ISSN);
    tagMap.put("ArticleFirstPage", MetadataField.KEY_START_PAGE);
  };

  static List tags = ListUtil.list(VOLUME_START, VOLUME_END,
				   ISSUE_START, ISSUE_END);
  static {
    tags.addAll(tagMap.keySet());
  }

  public static class SpringerXmlMetadataExtractor
    extends SimpleXmlMetadataExtractor {

    public SpringerXmlMetadataExtractor() {
      super(tags);
    }

    public ArticleMetadata extract(CachedUrl xmlCu) throws IOException {
      ArticleMetadata am = super.extract(xmlCu);
      am.cook(tagMap);
      // Springer doesn't prefix the DOI in dc.Identifier with doi:

      String start = am.getRaw(VOLUME_START);
      String end = am.getRaw(VOLUME_END);
      if (start != null) {
	if (end != null) {
	  if (end.equals(start)) {
	    log.debug3(xmlCu.getUrl() + " " + VOLUME_START + "=" + start);
	    am.put(MetadataField.FIELD_VOLUME, start);
	  } else {
	    log.debug3(xmlCu.getUrl() + " " + VOLUME_START + " " + start +
		       " " + VOLUME_END + end);
	    am.put(MetadataField.FIELD_VOLUME, start + "-" + end);
	  }
	} else {
	  log.debug3(xmlCu.getUrl() + " " + VOLUME_START + " " + start);
	  am.put(MetadataField.FIELD_VOLUME, start);
	}
      } else {
	log.debug3(xmlCu.getUrl() + " no " + VOLUME_START);
      }
      start = am.getRaw(ISSUE_START);
      end = am.getRaw(ISSUE_END);
      if (start != null) {
	if (end != null) {
	  if (end.equals(start)) {
	    log.debug3(xmlCu.getUrl() + " " + ISSUE_START + "=" + start);
	    am.put(MetadataField.FIELD_ISSUE, start);
	  } else {
	    log.debug3(xmlCu.getUrl() + " " + ISSUE_START + " " + start +
		       " " + ISSUE_END + end);
	    am.put(MetadataField.FIELD_ISSUE, start + "-" + end);
	  }
	} else {
	  log.debug3(xmlCu.getUrl() + " " + ISSUE_START + " " + start);
	  am.put(MetadataField.FIELD_ISSUE, start);
	}
      } else {
	log.debug3(xmlCu.getUrl() + " no " + ISSUE_START);
      }
      return am;
    }
  }
}
