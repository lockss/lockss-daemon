/*
 * $Id$
 */

/*

Copyright (c) 2007-2011 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseExploderHelper;
import org.lockss.crawler.Exploder;

/**
 * This ExploderHelper encapsulates knowledge about the way
 * Springer delivers source files.  They come as a ZIP
 * archive containing additions to a directory tree whose
 * layers are:
 *
 * 1. <code>PUB=${PUBLISHER}</code> As of 11/24/09 this layer removed
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
 * <code>http://springer.clockss.org/JOU=${JOURNAL_ID}/
 *
 * and the rest of the url inside the AU is the rest of the name of the entry.
 * It synthesizes suitable header fields for the files based on their
 * extensions.
 *
 * If the input ArchiveEntry contains a name matching this pattern the
 * baseUrl, restOfUrl, headerFields fields are set.  Otherwise,
 * they are left null.
 */
public class SpringerExploderHelper extends BaseExploderHelper {
  private static final String BASE_URL_STEM = "http://springer.clockss.org/";
  static final String[] tags = { "JOU=", "VOL=", "ISU=", "ART=" };
  private static final String PUB_FLAG = "PUB=";
  private static final String PUB_NAME = "Springer";
  private static final int JOU_INDEX = 0;
  private static final int VOL_INDEX = 1;
  private static final int ISU_INDEX = 2;
  private static final int ART_INDEX = 3;
  static final int endOfBase = 0;
  static final int minimumPathLength = 4;
  static Logger logger = Logger.getLogger("SpringerExploderHelper");

  public SpringerExploderHelper() {
  }

  public void process(ArchiveEntry ae) {
    String baseUrl = BASE_URL_STEM;
    // Parse the name
    String entryName = ae.getName();
    // Remove PUB= prefix
    if (entryName.startsWith(PUB_FLAG)) {
      int firstSlash = entryName.indexOf("/");
      if (firstSlash > 0) {
	entryName = entryName.substring(firstSlash+1);
      } else {
	logger.warning("Path " + entryName + " malformaeed");
	return;
      }
    }
    String[] pathElements = entryName.split("/");
    if (pathElements.length < minimumPathLength) {
      logger.warning("Path " + entryName + " too short");
      return;
    }
    for (int i = 0; i < pathElements.length; i++) {
      logger.debug3("pathElements[" + i + "] = " + pathElements[i]);
    }
    for (int i = 0; i <= endOfBase; i++) {
      if (pathElements[i].startsWith(tags[i])) {
	baseUrl += pathElements[i] + "/";
      } else {
	logger.warning("Element " + i + " of " + entryName +
		       " should be " + tags[i]);
	return;
      }
    }
    String restOfUrl = "";
    for (int i = (endOfBase + 1); i < pathElements.length ; i++) {
      if (i <= ART_INDEX) {
	if (!pathElements[i].startsWith(tags[i])) {
	  logger.warning("Element " + i + " of " + entryName +
			 " should be " + tags[i]);
	  return;
	}
      }
      restOfUrl += pathElements[i];
      if ((i + 1) < pathElements.length) {
	restOfUrl += "/";
      }
    }
    CIProperties headerFields = Exploder.syntheticHeaders(baseUrl + restOfUrl,
							  ae.getSize());
    logger.debug(entryName + " mapped to " +
		 baseUrl + " plus " + restOfUrl);
    logger.debug3(baseUrl + restOfUrl + " props " + headerFields);
    ae.setBaseUrl(baseUrl);
    ae.setRestOfUrl(restOfUrl);
    ae.setHeaderFields(headerFields);
    if (restOfUrl.endsWith(".pdf")) {
      // XXX should have issue TOC
      // Now add a link for the URL to the volume TOC page at
      // baseUrl + /VOL=bletch/index.html
      Hashtable addText = new Hashtable();
      String volTOC = baseUrl + pathElements[VOL_INDEX] + "/index.html";
      String link = "<li><a href=\"" + baseUrl + restOfUrl + "\">" +
	"art #" + pathElements[ART_INDEX].substring(4) + "</a></li>\n";
      logger.debug3("volTOC = " + volTOC + " link " + link);
      ae.addTextTo(volTOC, link);
      // Now add a link to the volume TOC page to the journal TOC at
      // baseUrl + index.html
      String journalTOC = baseUrl + "index.html";
      link = "<li><a href=\"" + volTOC + "\">" +
	"vol #" + pathElements[VOL_INDEX].substring(4) + "</a></li>\n";
      logger.debug3("journalTOC = " + journalTOC + " link " + link);
      ae.addTextTo(journalTOC, link);
    } else if (restOfUrl.endsWith(".xml")) {
      // XXX it would be great to be able to get the DOI from the
      // XXX metadata files and put it in the text here
    }
    CIProperties props = new CIProperties();
    props.put(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    props.put(ConfigParamDescr.PUBLISHER_NAME.getKey(),
	      PUB_NAME);
    props.put(ConfigParamDescr.JOURNAL_ISSN.getKey(),
	      pathElements[JOU_INDEX].substring(4));
    props.put(ConfigParamDescr.YEAR.getKey(),
	      pathElements[ART_INDEX].substring(4,8));
    ae.setAuProps(props);
  }
}
