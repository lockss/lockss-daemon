/*
 * $Id: CreativeCommonsV3PermissionChecker.java,v 1.1 2007-08-08 22:45:27 dshr Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.crawler.*;
import org.lockss.extractor.*;

/**
 * This Permission checker looks for a link tag with the rel="license"
 * attribute and verifies that the linked-to URL is one of the
 * appropriate Creative Commons V3 license pages.
 */

public class CreativeCommonsV3PermissionChecker implements PermissionChecker {

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
      logger.debug3("pHelper is "+pHelper.toString());
      au = pHelper.makeUrlCacher(permissionUrl).getArchivalUnit();
      logger.debug3("AU is "+au.toString());
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
      return true;
    }
    return false;
  }

  private static class CustomHtmlLinkExtractor
    extends GoslingHtmlLinkExtractor {

    private static final String REL = "rel";
    private static final String LICENSE = "license";
    private static final String VALIDURLSTEM =
      "http://creativecommons.org/licenses/";
    private static final String[] VALIDLICENSETYPE = {
      "by/", "by-sa/", "by-nc/", "by-nd/", "by-nc-sa/", "by-nc-nd/",
    };
    private static final String VALIDLICENSEVERSION = "3.0";

    protected String extractLinkFromTag(StringBuffer link, ArchivalUnit au,
					LinkExtractor.Callback cb) {
      String returnStr = null;

      switch (link.charAt(0)) {
        case 'l': //<link href="blah" rel="license">
        case 'L':
        case 'a': //<a href="blah" rel="license">
        case 'A':
	  logger.debug3("Looking for license in "+link);
	  if (beginsWithTag(link, LINKTAG) || beginsWithTag(link, ATAG)) {
	    String relStr = getAttributeValue(REL, link);
	    if (LICENSE.equalsIgnoreCase(relStr)) {
	      // This tag has the rel="license" attribute
	      String licenseURL = getAttributeValue(HREF, link);
	      if (licenseURL == null) {
	        break;
	      }
	      logger.debug2("Found a license: "+licenseURL);
	      licenseURL = licenseURL.toLowerCase();
	      if (!licenseURL.startsWith(VALIDURLSTEM)) {
	        break;
	      }
	      // And it links to a CreativeCommons license URL
	      String licenseType = licenseURL.substring(VALIDURLSTEM.length());
	      if (licenseType == null) {
	        break;
	      }
	      logger.debug2("Found a license type: "+licenseType);
	      for (int i = 0; i < VALIDLICENSETYPE.length; i++) {
	        if (licenseType.startsWith(VALIDLICENSETYPE[i])) {
		  String version = licenseType.substring(VALIDLICENSETYPE[i].length());
		  if (version.startsWith(VALIDLICENSEVERSION)) {
		    returnStr = licenseURL;
		    logger.debug("Found a CC V3 license in "+returnStr);
		  }
                }
	      }
	    }
	  }
	  break;
        default:
	  return null;
      }
      return returnStr;
    }
  }

  private class MyLinkExtractorCallback implements LinkExtractor.Callback {
    public MyLinkExtractorCallback() {
    }

    public void foundLink(String url) {
      if (licenseUrl != null) {
	logger.warning("Multiple license URLs found on manifest page.  " +
			"Old: "+licenseUrl+" New: "+url);
      }
      licenseUrl = url;
    }
  }
}
