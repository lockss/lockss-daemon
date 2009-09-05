/*
 * $Id: SageIngentaExploderHelper.java,v 1.4.20.2 2009-09-05 18:03:02 dshr Exp $
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

package org.lockss.plugin.sage;

import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.crawler.Exploder;

/**
 * This ExploderHelper encapsulates knowledge about the way
 * Sage delivers Ingenta source files.  Each issue comes as
 * a ZIP archive called, for example, aub_2005_13_2.zip where
 * <code>aub</code> is a letter code for the journal
 * <code>2005</code> is the year
 * <code>13</code> is the volume
 * <code>2</code> is the issue
 * The files in the ZIP archive are called, for example:
 * <code>10.1177_09675507050130020301.pdf</code>
 * <code>10.1177_09675507050130020301.xml</code>
 * where the DOI for the article is <code>10.1177/09675507050130020301</code>
 * and the ISSN for the journal is <code>0967-5507</code>.  This is a book
 * review.  There are also files, for example:
 * <code>10.1191_0967550705ab021oa.xml</code> and
 * <code>10.1191_0967550705ab021oa.pdf</code> which are articles.
 *
 * This class maps this into base URLs that look like:
 * <!-- FIXME -->
 * <code>http://sage.clockss.org/${ISSN}/${VOL}/${ISSUE}/${FILE_NAME}</code>
 *
 * It synthesizes suitable header fields for the files based on their
 * extensions.
 *
 * If the input ArchiveEntry contains a name matching this pattern the
 * baseUrl, restOfUrl, headerFields fields are set.  Otherwise,
 * they are left null.
 */
public class SageIngentaExploderHelper implements ExploderHelper {
  protected static final int ISSUE_AND_EXTENSION_INDEX = 3;
  protected static final int INDEX_VOLUME = 2;
  protected static final int INDEX_YEAR = 1;
  private static final String BASE_URL_STEM = "http://sage.clockss.org/";

  static Logger logger = Logger.getLogger("SageIngentaExploderHelper");

  public SageIngentaExploderHelper() {
  }

  /*
   * On entry we have the following fields set in the ArchiveEntry;
   * getName() - the name of the file, with no path
   * getSize() - number of bytes in file
   * getTime() - time of the file
   * getExploder() - the Exploder in use
   */

  public void process(ArchiveEntry ae) {
    String baseUrlStem = BASE_URL_STEM;
    /*
     * First parse the archiveUrl to get the year/volume/issue
     * the URL ends with something like aub_2005_13_2.zip
     */
    String[] urlElements = ae.getExploder().getArchiveUrl().split("_");
    if (urlElements.length < 4) {
      logger.warning("Short URL: " + ae.getExploder().getArchiveUrl());
      return;
    }
    String year = urlElements[INDEX_YEAR];
    String volume = urlElements[INDEX_VOLUME];
    String issue = urlElements[ISSUE_AND_EXTENSION_INDEX].split("\\.")[0];
    if (year == null || year.length() != 4 ||
	volume == null || volume.length() == 0 ||
	issue == null || issue.length() == 0) {
      logger.warning("Bad URL parse: " + year + ":" + volume + ":" + issue);
    }
    /*
     * Next parse the file name to get the DOI and ISSN
     */
    String entryName = ae.getName();
    String[] entryElements = entryName.split("_");
    String issn = entryElements[1].substring(0,8);
    // String doi = entryName.substring(0,entryName.lastIndexOf(".")-1);
    // Set baseUrl
    String baseUrl = baseUrlStem + issn + "/";
    // Set restOfUrl
    String restOfUrl = year + "/" + entryName;
    // Synthesize header fields
    CIProperties headerFields = Exploder.syntheticHeaders(baseUrl + restOfUrl,
							  ae.getSize());
    logger.debug(ae.getName() + " mapped to " +
		 baseUrl + " plus " + restOfUrl);
    logger.debug3(baseUrl + restOfUrl + " props " + headerFields);
    ae.setBaseUrl(baseUrl);
    ae.setRestOfUrl(restOfUrl);
    ae.setHeaderFields(headerFields);
    //  Should create TOC pages here with addText
    CIProperties props = new CIProperties();
    props.put(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    props.put(ConfigParamDescr.PUBLISHER_NAME.getKey(),
	      "SAGE Publications");
    props.put(ConfigParamDescr.JOURNAL_ISSN.getKey(),
	      issn);
    props.put(ConfigParamDescr.YEAR.getKey(),
	      year);
    ae.setAuProps(props);
  }
}
