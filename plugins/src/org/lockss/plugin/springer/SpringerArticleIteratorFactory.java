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

package org.lockss.plugin.springer;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.extractor.*;
import org.lockss.daemon.PluginException;

public class SpringerArticleIteratorFactory
  implements ArticleIteratorFactory,
	     ArticleMetadataExtractorFactory {
  static Logger log = Logger.getLogger("SpringerArticleIterator");

  /*
   * The Springer URL structure means that the metadata for an article
   * is at a URL like
   * http://springer.clockss.org/PUB=./JOU=./VOL=./ISU=./ART=./..xml.Meta
   * and the PDF at
   * http://springer.clockss.org/PUB=./JOU=./VOL=./ISU=./ART=./BodyRef/PDF/..pdf
   */
//   protected Pattern pat = Pattern.compile(".*\\.xml\\.Meta$",
// 					  Pattern.CASE_INSENSITIVE);
  Pattern pat = null;

  private static final String part1 = "/BodyRef/PDF";
  private static final String part2 = "\\.pdf";
  private static final String regex = ".*" + part1 + "/.*" + part2;


  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
						      MetadataTarget target)
      throws PluginException {
    return new SubTreeArticleIterator(au,
				      new SubTreeArticleIterator.Spec()
				      .setTarget(target)) {
      protected ArticleFiles createArticleFiles(CachedUrl cu) {
	ArticleFiles res = new ArticleFiles();
	res.setFullTextCu(cu);
	// cu points to a file whose name is ....pdf
	// but the metadata we want is in a file whose name is ....xml.Meta
	String pdfUrl = cu.getUrl();
	if (pdfUrl.matches(regex)) {
	  String xmlUrl =
	    pdfUrl.replaceFirst(part1, "").replaceFirst(part2, ".xml.Meta");
	  CachedUrl xmlCu = cu.getArchivalUnit().makeCachedUrl(xmlUrl);
	  if (xmlCu == null || !xmlCu.hasContent()) {
	    if (xmlCu == null) {
	      log.debug2("xmlCu is null");
	    } else {
	      log.debug2(xmlCu.getUrl() + " no content");
	    }
	    xmlUrl = 
	      pdfUrl.replaceFirst(part1, "").replaceFirst(part2, ".xml.meta");
	    xmlCu = cu.getArchivalUnit().makeCachedUrl(xmlUrl);
	  }
	  try {
	    if (xmlCu != null && xmlCu.hasContent()) {
	      String mimeType =
		HeaderUtil.getMimeTypeFromContentType(xmlCu.getContentType());
	      if ("text/xml".equalsIgnoreCase(mimeType)) {
		res.setRoleCu("xml", xmlCu);
	      } else {
		log.debug2("xml.meta wrong mime type: " + mimeType + ": "
			   + xmlCu.getUrl());
	      }
	    } else {
	      if (xmlCu == null) {
		log.debug2("xmlCu is null");
	      } else {
		log.debug2(xmlCu.getUrl() + " no content");
	      }
	    }
	  } finally {
	    AuUtil.safeRelease(xmlCu);
	  }
	} else {
	  log.debug2(pdfUrl + " doesn't match " + regex);
	}
	if (log.isDebug3()) {
	  log.debug3("Iter: " + res);
	}
	return res;
      }
    };
  }

  public ArticleMetadataExtractor
    createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor("xml");
  }

}
