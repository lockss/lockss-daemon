/*
 * $Id: WileyExploderHelper.java,v 1.5 2009-09-05 18:03:27 dshr Exp $
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

package org.lockss.plugin.wiley;

import java.util.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.crawler.Exploder;

/**
 * This ExploderHelper encapsulates knowledge about the way
 * Wiley delivers source files.  They come as a ZIP
 * archive containing additions to a directory tree whose
 * layers are:
 *
 * 1. <code>${JOURNAL_ID}</code> JOURNAL_ID is ISSN without dash
 *
 * 2. <code>${YEAR}</code> YEAR is 4 digits
 *
 * 3. <code>${VOLUME_ID}</code>
 *
 * 4. <code>${ISSUE_ID}</code>
 *
 * 5. <code>${JOURNAL_CODE}${ARTICLE_ID}</code> JOURNAL_CODE is three letters,
 *    ARTICLE_ID is 5 digits
 * This directory contains files called
 * <code>${ARTICLE_ID}_ftp.pdf</code> and
 * <code>${ARTICLE_ID}_ftp.sgm</code> and
 * and sub-directories:
 *
 * - directory equation
 * - directory image_a, contains aeqn${NUM_3DIGIT}.{tif,gif}
 * - directory image_m, contains mfig${NUM_3DIGIT}.{jpg,gif,tif}
 * - directory image_n, contains nfig${NUM_3DIGIT}.{tif,gif,jpg}
 * - directory image_t, contains tfig${NUM_3DIGIT}.{tif,gif,jpg}
 *
 * This class maps this into base URLs that look like:
 *
 * <code>http://www.wiley.com/CLOCKSS/${JOURNAL_ID}/
 *
 * and the rest of the url inside the AU is the rest of the name of the entry.
 * It synthesizes suitable header fields for the files based on their
 * extensions.
 *
 * If the input ArchiveEntry contains a name matching this pattern the
 * baseUrl, restOfUrl, headerFields fields are set.  Otherwise,
 * they are left null.
 */
public class WileyExploderHelper implements ExploderHelper {
  public static final String BASE_URL_STEM = "http://wiley.clockss.org/";
  private static final int JOU_INDEX = 0;
  private static final int YER_INDEX = 1;
  private static final int VOL_INDEX = 2;
  private static final int ISU_INDEX = 3;
  private static final int ART_INDEX = 4;
  static final int endOfBase = JOU_INDEX;
  static final int minimumPathLength = 5;
  static Logger logger = Logger.getLogger("WileyExploderHelper");

  public WileyExploderHelper() {
  }

  public void process(ArchiveEntry ae) {
    String baseUrl = BASE_URL_STEM;
    // Parse the name
    String[] pathElements = ae.getName().split("/");
    if (pathElements.length < minimumPathLength) {
      logger.warning("Path " + ae.getName() + " too short");
      return;
    }
    for (int i = 0; i <= endOfBase; i++) {
      baseUrl += pathElements[i] + "/";
    }
    String restOfUrl = "";
    for (int i = VOL_INDEX; i < pathElements.length ; i++) {
      restOfUrl += pathElements[i];
      if ((i + 1) < pathElements.length) {
	restOfUrl += "/";
      }
    }
    CIProperties headerFields = Exploder.syntheticHeaders(baseUrl + restOfUrl,
							  ae.getSize());
    logger.debug(ae.getName() + " mapped to " +
		 baseUrl + " plus " + restOfUrl);
    logger.debug3(baseUrl + restOfUrl + " props " + headerFields);
    ae.setBaseUrl(baseUrl);
    ae.setRestOfUrl(restOfUrl);
    ae.setHeaderFields(headerFields);
    if (restOfUrl.endsWith(".pdf")) {
      Hashtable addText = new Hashtable();
      // Add a link for the URL to the issue TOC page at
      // baseUrl + VOL_INDEX/ISU_INDEX/index.html
      String isuTOC = baseUrl + pathElements[VOL_INDEX] + "/" +
	pathElements[ISU_INDEX] + "/index.html";
      String link = "<li><a href=\"" + baseUrl + restOfUrl + "\">" +
	"art #" + pathElements[ART_INDEX] + "</a></li>\n";
      logger.debug3("isuTOC = " + isuTOC + " link " + link);
      ae.addTextTo(isuTOC, link);
      logger.debug3("isuTOC = " + isuTOC + " link " + link + " done");
      // Add a link to the issue TOC page to the volume TOC at
      // baseUrl + VOL_INDEX/index.html
      String volTOC = baseUrl + pathElements[VOL_INDEX] + "/index.html";
      link = "<li><a href=\"" + isuTOC + "\">" +
	"issue #" + pathElements[ISU_INDEX] + "</a></li>\n";
      logger.debug3("volTOC = " + volTOC + " link " + link);
      ae.addTextTo(volTOC, link);
      logger.debug3("volTOC = " + volTOC + " link " + link + " done");
      // Now add a link to the volume TOC page to the journal TOC at
      // baseUrl + index.html
      String journalTOC = baseUrl + "index.html";
      link = "<li><a href=\"" + volTOC + "\">" +
	"vol #" + pathElements[VOL_INDEX] + "</a></li>\n";
      logger.debug3("journalTOC = " + journalTOC + " link " + link);
      ae.addTextTo(journalTOC, link);
      logger.debug3("journalTOC = " + journalTOC + " link " + link + " done");
    }
    CIProperties props = new CIProperties();
    props.put(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    props.put(ConfigParamDescr.PUBLISHER_NAME.getKey(),
	      "Wiley");
    props.put(ConfigParamDescr.JOURNAL_ISSN.getKey(),
	      pathElements[JOU_INDEX]);
    props.put(ConfigParamDescr.YEAR.getKey(),
	      pathElements[YER_INDEX]);
    logger.debug3(baseUrl + restOfUrl + " AU props " + props);
    ae.setAuProps(props);
  }
}
