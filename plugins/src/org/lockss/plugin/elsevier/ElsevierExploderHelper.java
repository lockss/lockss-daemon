/*
 * $Id: ElsevierExploderHelper.java,v 1.1.2.3 2007-09-20 04:15:52 dshr Exp $
 */

/*

Copyright (c) 2007 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

/**
 * This ExploderHelper encapsulates knowledge about the way
 * Elsevier delivers source files.  They come as TAR
 * archives containing additions to a directory tree whose
 * layers are:
 *
 * 1. <code>${JOURNAL_ID}</code> JOURNAL_ID is the ISSN without the dash.
 *
 * 2. <code>${ARTICLE_ID}</code> ARTICLE_ID is a number (I think it is unique)
 * This directory contains files called
 * - *.pdf PDF
 * - *.raw ASCII
 * - *.sgm SGML
 * - *.gif figures etc
 * - *.jpg images etc.
 * - *.xml
 * - stripin.toc see Appendix 2
 * - checkmd5.fil md5 sums for files
 *
 * This class maps this into base URLs that look like:
 *
 * <code>http://www.elsevier.com/CLOCKSS/JOU=${JOURNAL_ID}/
 *
 * and the rest of the url inside the AU is the rest of the name of the entry.
 * It synthesizes suitable header fields for the files based on their
 * extensions.
 *
 * If the input ArchiveEntry contains a name matching this pattern the
 * baseUrl, restOfUrl, headerFields fields are set.  Otherwise,
 * they are left null.
 */
public class ElsevierExploderHelper implements ExploderHelper {
  private static final int JOU_INDEX = 0;
  private static final int ART_INDEX = 1;
  static final int endOfBase = 1;
  static final int minimumPathLength = 3;
  static Logger logger = Logger.getLogger("ElsevierExploderHelper");
  private static final String[] extensions = {
    ".pdf",
    ".raw",
    ".sgm",
    ".gif",
    ".jpg",
    ".xml",
    ".toc",
    ".fil",
    ".sml",
  };
  private static final String[] mimeType = {
    "application/pdf",
    "text/plain",
    "application/sgml",
    "image/gif",
    "image/jpeg",
    "application/xml",
    "text/plain", // XXX check
    "text/plain", // XXX check
    "application/sgml",
  };
  private HashMap mimeMap = null;
  

  public ElsevierExploderHelper() {
    mimeMap = new HashMap();
    for (int i = 0; i < extensions.length; i++) {
      mimeMap.put(extensions[i], mimeType[i]);
    }
  }

  public void process(ArchiveEntry ae) {
    String baseUrl = "http://www.elsevier.com/CLOCKSS/";
    // Parse the name
    String[] pathElements = ae.getName().split("/");
    if (pathElements.length < minimumPathLength) {
      logger.warning("Path " + ae.getName() + " too short");
      return;
    }
    for (int i = 0; i < endOfBase; i++) {
      try {
	int journal = Integer.parseInt(pathElements[i]);
	baseUrl += pathElements[i] + "/";
      } catch (NumberFormatException e) {
	logger.warning("Element " + i + " of " + ae.getName() +
		       " should be an integer");
	return;
      }
    }
    String restOfUrl = "";
    for (int i = endOfBase; i < pathElements.length ; i++) {
      restOfUrl += pathElements[i];
      if ((i + 1) < pathElements.length) {
	restOfUrl += "/";
      }
    }
    CIProperties headerFields = new CIProperties();
    String fileName = pathElements[pathElements.length-1];
    String contentType = mimeTypeOf(fileName);
    if (contentType != null) {
      headerFields.setProperty("Content-Type", contentType);
      headerFields.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE,
			       contentType);
    }
    headerFields.setProperty("Content-Length",
			     Long.toString(ae.getSize()));
    headerFields.setProperty(CachedUrl.PROPERTY_NODE_URL,
			     baseUrl + restOfUrl);
    logger.debug(ae.getName() + " mapped to " +
		 baseUrl + " plus " + restOfUrl);
    for (Enumeration e = headerFields.propertyNames();
	 e.hasMoreElements(); ) {
      String key = (String)e.nextElement();
      String value = (String)headerFields.get(key);
      logger.debug(key + " = " + value);
    }
    ae.setBaseUrl(baseUrl);
    ae.setRestOfUrl(restOfUrl);
    ae.setHeaderFields(headerFields);
    if (fileName.endsWith(".pdf")) {
      // Add a link to the article to the journal TOC page at
      // ${JOURNAL_ID}/index.html
      Hashtable addText = new Hashtable();
      String journalTOC = baseUrl + "/index.html";
      String link = "<li><a href=\"" + baseUrl + restOfUrl + "\">" +
	"art #" + pathElements[ART_INDEX] + "</a></li>\n";
      logger.debug3("journalTOC " + journalTOC + " link " + link);
      ae.addTextTo(journalTOC, link);
    } else if (fileName.endsWith(".xml")) {
      // XXX it would be great to be able to get the DOI from the
      // XXX metadata files and put it in the text here
    }
    CIProperties props = new CIProperties();
    props.put(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    props.put(ConfigParamDescr.PUBLISHER_NAME.getKey(),
	      "Elsevier");
    props.put(ConfigParamDescr.JOURNAL_ID.getKey(),
	      pathElements[JOU_INDEX]);
    ae.setAuProps(props);
  }

  private String mimeTypeOf(String filename) {
    String res = "text/plain";
    int ix = filename.lastIndexOf(".");
    if (ix > 0) {
      String mt = (String)mimeMap.get(filename.substring(ix));
      if (mt !=null) {
	res = mt;
      }
    }
    logger.debug(filename + " mime-type " + res);
    return (res);
  }
}
