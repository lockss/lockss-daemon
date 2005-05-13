/*
 * $Id: ProbePermissionChecker.java,v 1.3 2005-05-13 17:42:29 troberts Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
import java.net.*;

import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.crawler.*;

/**
 * This Permission checker looks for a probe (a URL) which is identified by 
 * being in a link tag with a specific attribute.  It will then pass that URL
 * to another specified permission checker
 */

public class ProbePermissionChecker implements PermissionChecker {
  String probeUrl = null; 
  PermissionChecker checker;

  private static Logger logger = Logger.getLogger("ProbePermissionChecker");

  ArchivalUnit au;

  public ProbePermissionChecker(PermissionChecker checker, ArchivalUnit au) {
    if (checker == null) {
      throw new NullPointerException("Called with null permission checker");
    } else if (au == null) {
      throw new NullPointerException("Called with null archival unit");
    }
    this.au = au;
    this.checker = checker;
  }

  public boolean checkPermission(Reader inputReader, String permissionUrl) {
    CustomHtmlParser parser = new CustomHtmlParser();
    try {
      parser.parseForUrls(inputReader, permissionUrl,
			  new MyFoundUrlCallback());
    } catch (IOException ex) {
      logger.error("Exception trying to parse permission url "+permissionUrl,
		   ex);
      return false;
    }
    if (probeUrl != null) {
      //XXX this is wrong
      //We need something like HighWireLoginPageChecker that gets called here 
      Reader reader = au.makeCachedUrl(probeUrl).openForReading();
      return checker.checkPermission(reader, probeUrl);
    }
    return false;
  }


  private class CustomHtmlParser extends GoslingHtmlParser {
    private static final String LOCKSSPROBE = "lockss-probe";

    protected String parseLink(StringBuffer link)
	throws MalformedURLException {
      String returnStr = null;

      switch (link.charAt(0)) {
        case 'l': //<link href=blah.css>
        case 'L':
	  if (beginsWithTag(link, LINKTAG)) {
	    returnStr = getAttributeValue(HREF, link);
	    String probeStr = getAttributeValue(LOCKSSPROBE, link);
	    if (probeStr == null || !"true".equalsIgnoreCase(probeStr)) {
	      returnStr = null;
	    }
	  }
	  break;
        default:
	  return null;
      }
      
      if (returnStr != null) {
	logger.debug2("Generating url from: " + srcUrl
		      + " and " + returnStr);
	try {
	  if (baseUrl == null) {
	    baseUrl = new URL(srcUrl);
	  }
	  returnStr = resolveUri(baseUrl, returnStr);
	} catch (MalformedURLException e) {
	  logger.debug("Couldn't resolve URL, base: \"" + srcUrl +
		       "\", link: \"" + returnStr + "\"",
		       e);
	  return null;
	}
	logger.debug2("Parsed: " + returnStr);
      }
      return returnStr;
    }
  }
  
  private class MyFoundUrlCallback implements ContentParser.FoundUrlCallback {
    public MyFoundUrlCallback() {
    }

    public void foundUrl(String url) {
      probeUrl = url;
    }
  }
}
