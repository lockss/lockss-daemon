/*
 * $Id: SpringerExploderHelper.java,v 1.1.2.1 2007-09-11 19:14:53 dshr Exp $
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

package org.lockss.plugin.springer;

import java.util.*;
import org.lockss.daemon.ExploderHelper;
import org.lockss.daemon.ArchiveEntry;
import org.lockss.util.*;

/**
 * This ExploderHelper encapsulates knowledge about the way
 * Springer delivers source files.  They come as a ZIP
 * archive containing additions to a directory tree whose
 * layers are:
 *
 * 1. <code>PUB=${PUBLISHER}</code>
 *
 * 2. <code>JOU=${JOURNAL_ID}</code>
 *
 * 3. <code>VOL=${VOLUME_ID}</code>
 *
 * 4. <code>ISU=${ISSUE_ID}</code>
 *
 * 5. <code>ART=${YEAR}_${ARTICLE_ID}</code>
 * This directory contains files called
 * <code>${JOURNAL_ID}_${YEAR}_Article_${ARTICLE_ID}.xml</code> and
 * <code>${JOURNAL_ID}_${YEAR}_Article_${ARTICLE_ID}.xml.Meta</code>
 * and a directory tree whose layers are:
 *
 * 6. BodyRef
 *
 * 7. PDF
 * This directory contains 
 * <code>${JOURNAL_ID}_${YEAR}_Article_${ARTICLE_ID}.pdf</code>
 *
 * This class maps this into base URLs that look like:
 *
 * <code>http://www.springer.com/CLOCKSS/PUB=${PUBLISHER}/JOU=${JOURNAL_ID}/
 *
 * and the rest of the url inside the AU is the rest of the name of the entry.
 * It synthesizes suitable header fields for the files based on their
 * extensions.
 *
 * If the input ArchiveEntry contains a name matching this pattern the
 * baseUrl, restOfUrl, headerFields fields are set.  Otherwise,
 * they are left null.
 */
public class SpringerExploderHelper implements ExploderHelper {
  static final String[] tags = { "PUB=", "JOU=", "VOL=", "ISU=", "ART=" };
  static final int endOfBase = 1;
  static final int minimumPathLength = 5;
  static Logger logger = Logger.getLogger("SpringerExploderHelper");

  SpringerExploderHelper() {
  }

  public void process(ArchiveEntry ae) {
    String baseUrl = "http://www.springer.com/CLOCKSS/";
    // Parse the name
    String[] pathElements = ae.getName().split("/");
    if (pathElements.length < minimumPathLength) {
      logger.warning("Path " + ae.getName() + " too short");
      return;
    }
    for (int i = 0; i <= endOfBase; i++) {
      if (pathElements[i].startsWith(tags[i])) {
	baseUrl += pathElements[i] + "/";
      } else {
	logger.warning("Element " + i + " of " + ae.getName() +
		       " should be " + tags[i]);
	return;
      }
    }
    String restOfUrl = "";
    for (int i = (endOfBase + 1); i < pathElements.length ; i++) {
      restOfUrl += pathElements[i];
      if ((i + 1) < pathElements.length) {
	restOfUrl += "/";
      }
    }
    CIProperties headerFields = new CIProperties();
    String fileName = pathElements[pathElements.length-1];
    if (fileName.endsWith(".pdf")) {
      headerFields.setProperty("Content-Type", "application/pdf");
    } else if (fileName.endsWith(".xml")) {
      headerFields.setProperty("Content-Type", "text/xml");
    } else if (fileName.endsWith(".xml.Meta")) {
      headerFields.setProperty("Content-Type", "text/xml");
    }
    headerFields.setProperty("Content-Length",
			     Long.toString(ae.getSize()));
    logger.debug(ae.getName() + " mapped to " +
		 baseUrl + " plus " + restOfUrl);
    for (Enumeration e = headerFields.propertyNames();
	 e.hasMoreElements(); ) {
      String key = (String)e.nextElement();
      String value = (String)headerFields.get(key);
      logger.debug(key + "=" + value);
    }
    ae.setBaseUrl(baseUrl);
    ae.setRestOfUrl(restOfUrl);
    ae.setHeaderFields(headerFields);
  }
}
