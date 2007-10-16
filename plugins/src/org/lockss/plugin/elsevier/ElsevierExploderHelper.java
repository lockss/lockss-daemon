/*
 * $Id: ElsevierExploderHelper.java,v 1.3 2007-10-16 23:47:25 dshr Exp $
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
import org.lockss.crawler.Exploder;

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
  private static final String BASE_URL = "http://elsevier.clockss.org/";
  static final int endOfBase = 1;
  static final int minimumPathLength = 3;
  static Logger logger = Logger.getLogger("ElsevierExploderHelper");

  public ElsevierExploderHelper() {
  }

  public void process(ArchiveEntry ae) {
    String baseUrl = BASE_URL;
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
    CIProperties headerFields =
      Exploder.syntheticHeaders(baseUrl + restOfUrl, ae.getSize());
    logger.debug(ae.getName() + " mapped to " +
		 baseUrl + " plus " + restOfUrl);
    logger.debug2(baseUrl + restOfUrl + " props " + headerFields);
    ae.setBaseUrl(baseUrl);
    ae.setRestOfUrl(restOfUrl);
    ae.setHeaderFields(headerFields);
    if (restOfUrl.endsWith(".pdf")) {
      // Add a link to the article to the journal TOC page at
      // ${JOURNAL_ID}/index.html
      Hashtable addText = new Hashtable();
      String journalTOC = baseUrl + "index.html";
      String link = "<li><a href=\"" + baseUrl + restOfUrl + "\">" +
	"art #" + pathElements[ART_INDEX] + "</a></li>\n";
      logger.debug3("journalTOC " + journalTOC + " link " + link);
      ae.addTextTo(journalTOC, link);
    } else if (restOfUrl.endsWith(".xml")) {
      // XXX it would be great to be able to get the DOI from the
      // XXX metadata files and put it in the text here
    }
    CIProperties props = new CIProperties();
    props.put(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    props.put(ConfigParamDescr.PUBLISHER_NAME.getKey(),
	      "Elsevier");
    props.put(ConfigParamDescr.JOURNAL_ISSN.getKey(),
	      pathElements[JOU_INDEX]);
    ae.setAuProps(props);
  }

}
