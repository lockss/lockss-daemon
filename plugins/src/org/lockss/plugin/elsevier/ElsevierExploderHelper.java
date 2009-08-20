/*
 * $Id: ElsevierExploderHelper.java,v 1.7.26.1 2009-08-20 23:44:50 dshr Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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
 * Documentation of the Elsevier format is at:
 * http://info.sciencedirect.com/techsupport/sdos/effect41.pdf
 * http://info.sciencedirect.com/techsupport/sdos/sdos30.pdf
 *
 * This ExploderHelper encapsulates knowledge about the way
 * Elsevier delivers source files.  They come as TAR
 * archives containing additions to a directory tree whose
 * layers are:
 *
 * 1. <code>${JOURNAL_ID}</code> JOURNAL_ID is the ISSN (or an
 *    ISSN-like string) without the dash. The tar file is named
 *    ${JOURNAL_ID}.tar
 *
 * 2. <code>${ISSUE_ID}</code> ISSUE_ID is string unique name for the
 *    issue within the journal.
 *
 * 3. <code>${ARTICLE_ID}</code> ARTICLE_ID is a similar string naming
 *    the article.
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
 * <code>http://elsevier.clockss.org/JOU=${JOURNAL_ID}/
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
  private static final int ISS_INDEX = 0;
  private static final int ART_INDEX = 1;
  private static final String BASE_URL = "http://elsevier.clockss.org/";
  static final int endOfBase = 0;
  static final int minimumPathLength = 3;
  static Logger logger = Logger.getLogger("ElsevierExploderHelper");
  private static final String[] ignoreMe = {
    "checkmd5.fil",
  };
    private static final String extension = ".tar";

  public ElsevierExploderHelper() {
  }

  public void process(ArchiveEntry ae) {
    String issn = archiveNameToISSN(ae);
    if (issn == null) {
      ae.setRestOfUrl(null);
      return;
    }
    // The base URL contains the ISSN from the archive name
    String baseUrl = BASE_URL + issn + "/";
    // Parse the name
    String fullName = ae.getName();
    String[] pathElements = fullName.split("/");
    if (pathElements.length < minimumPathLength) {
      for (int i = 0; i < ignoreMe.length; i++) {
	if (fullName.toLowerCase().endsWith(ignoreMe[i])) {
	  ae.setBaseUrl(baseUrl);
	  ae.setRestOfUrl(null);
	  logger.debug("Path " + fullName + " ignored");
	  return;
	}
      }
      logger.warning("Path " + fullName + " too short");
      return;
    }
    for (int i = 0; i < endOfBase; i++) {
      baseUrl += pathElements[i] + "/";
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
        "issue #" + pathElements[ISS_INDEX] +
	" art #" + pathElements[ART_INDEX] + "</a></li>\n";
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
	      issn);
    ae.setAuProps(props);
  }

  private boolean checkISSN(String s) {
    boolean ret = true;
    // XXX
    // The ISSN is 7 digits followed by either a digit or X
    // The last digit is a check digit as described here:
    // http://en.wikipedia.org/wiki/ISSN
    return ret;
  }

  private String archiveNameToISSN(ArchiveEntry ae) {
    String ret = null;
    String an = ae.getArchiveName();
    if (an != null) {
      // The ISSN is the part of an from the last / to the .tar
	int slash = an.lastIndexOf("/");
      int dot = an.lastIndexOf(extension);
      if (slash > 0 && dot > slash) {
	String maybe = an.substring(slash + 1, dot);
	if (checkISSN(maybe)) {
	  ret = maybe;
	  logger.debug3("ISSN: " + ret);
	} else {
          logger.warning("Bad ISSN in archive name " + an);
	}
      } else {
	logger.warning("Malformed archive name " + an);
      }
    } else {
      logger.error("Null archive name");
    }
    return ret;
  }
}
