/*
 * $Id: CreativeCommonsV3PermissionChecker.java,v 1.2.60.1 2012-06-20 00:02:58 nchondros Exp $
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.state.*;
import org.lockss.crawler.*;
import org.lockss.extractor.*;

/**
 * This Permission checker recognizes all versions of the newer form of a
 * Creative Commons license, which consists of an A or LINK tag containing
 * the URL of a valid CC license page, and the REL attribute with the value
 * LICENSE.
 */
public class CreativeCommonsV3PermissionChecker extends BasePermissionChecker {

  private static Logger logger =
    Logger.getLogger("CreativeCommonsV3PermissionChecker");

  String licenseUrl;

  public CreativeCommonsV3PermissionChecker() {
  }

  public boolean checkPermission(Crawler.PermissionHelper pHelper,
				 Reader inputReader, String permissionUrl) {
    licenseUrl = null;
    logger.debug3("Checking permission on "+permissionUrl);
    if (permissionUrl == null) {
      return false;
    }
    ArchivalUnit au = null;
    if (pHelper != null) {
      au = pHelper.getAu();
      if (logger.isDebug3()) {
	logger.debug3("pHelper: " + pHelper);
	logger.debug3("AU: " + au);
      }
    }
    CustomHtmlLinkExtractor extractor = new CustomHtmlLinkExtractor();
    try {
      // XXX ReaderInputStream needed until PermissionChecker changed to
      // take InputStream instead of Reader
      extractor.extractUrls(au, new ReaderInputStream(inputReader), null,
			    permissionUrl, new MyLinkExtractorCallback());
    } catch (IOException ex) {
      logger.error("Exception trying to parse permission url "+permissionUrl,
		   ex);
      return false;
    }
    if (licenseUrl != null) {
      logger.debug3("Found licenseUrl "+licenseUrl);
      setAuAccessType(pHelper, AuState.AccessType.OpenAccess);
      return true;
    }
    return false;
  }

  private static class CustomHtmlLinkExtractor
    extends GoslingHtmlLinkExtractor {

    private static final String REL = "rel";
    private static final String LICENSE = "license";

    private static Pattern CC_LICENSE_PAT =
      Pattern.compile("http://creativecommons.org/licenses/([^/]+)/([^/]+).*",
		      Pattern.CASE_INSENSITIVE);

    private static final Set<String> VALIDLICENSETYPES =
      SetUtil.set("by", "by-sa", "by-nc", "by-nd", "by-nc-sa", "by-nc-nd");

    private static final Set<String> VALIDLICENSEVERSIONS =
      SetUtil.set("1.0", "2.0", "2.5", "3.0");

    protected String extractLinkFromTag(StringBuffer link, ArchivalUnit au,
					LinkExtractor.Callback cb) {
      String returnStr = null;

      switch (link.charAt(0)) {
        case 'l': //<link href="blah" rel="license">
        case 'L':
        case 'a': //<a href="blah" rel="license">
        case 'A':
	  if (logger.isDebug3()) {
	    logger.debug3("Looking for license in "+link);
	  }
	  if (beginsWithTag(link, LINKTAG) || beginsWithTag(link, ATAG)) {
	    String relStr = getAttributeValue(REL, link);
	    if (LICENSE.equalsIgnoreCase(relStr)) {
	      // This tag has the rel="license" attribute
	      String licenseURL = getAttributeValue(HREF, link);
	      if (licenseURL == null) {
	        break;
	      }
	      if (logger.isDebug2()) {
		logger.debug2("CC license URL: "+licenseURL);
	      }
	      Matcher mat = CC_LICENSE_PAT.matcher(licenseURL);
	      if (logger.isDebug2()) {
		logger.debug2("Match: " + mat.matches() + ": " + licenseURL);
	      }
	      if (mat.matches()) {
		String lic = mat.group(1).toLowerCase();
		String ver = mat.group(2);
		if (logger.isDebug2()) {
		  logger.debug2("lic: " + lic + ", ver: " + ver);
		}
		// Any combination of license terms and version is
		// currently acceptable.
		if (VALIDLICENSETYPES.contains(lic)
		    && VALIDLICENSEVERSIONS.contains(ver)) {
		  returnStr = licenseURL;
		}
	      }
	    }
	  }
	  break;
        default:
	  return null;
      }
      if (logger.isDebug2()) {
	logger.debug2("ret: " + returnStr);
      }
      return returnStr;
    }
  }

  private class MyLinkExtractorCallback implements LinkExtractor.Callback {
    public MyLinkExtractorCallback() {
    }

    public void foundLink(String url) {
      if (licenseUrl != null && !licenseUrl.equalsIgnoreCase(url)) {
	logger.warning("Multiple license URLs found on manifest page.  " +
		       "Old: "+licenseUrl+" New: "+url);
      }
      licenseUrl = url;
    }
  }
}
