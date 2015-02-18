/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.crawler.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.extractor.*;

/**
 * This Permission checker looks for a probe (a URL) which is identified by
 * being in a link tag with a specific attribute.  It will then pass that URL
 * to another specified permission checker
 */

public class ProbePermissionChecker implements PermissionChecker {
  private static final Logger logger = 
      Logger.getLogger(ProbePermissionChecker.class);
  protected String probeUrl = null;
  protected ArchivalUnit au;
  protected CrawlerStatus crawlStatus;

  

  public ProbePermissionChecker() {
  }
  
  /** @deprecated use the no arg version */
  public ProbePermissionChecker(ArchivalUnit au) {
  }

  // For compatibility with plugins that supply a LoginPageChecker (which
  // is not used)
  /** @deprecated use the no arg version */
  public ProbePermissionChecker(LoginPageChecker checker, ArchivalUnit au) {
  }
  
  public boolean checkPermission(Crawler.PermissionHelper crawlFacade,
      Reader inputReader, String permissionUrl) {
    return checkPermission0((CrawlerFacade)crawlFacade, inputReader, permissionUrl);
  }
  
  public boolean checkPermission(CrawlerFacade crawlFacade,
				 Reader inputReader, String permissionUrl) {
    Crawler.PermissionHelper pHelper = (Crawler.PermissionHelper)crawlFacade;
    return checkPermission(pHelper, inputReader, permissionUrl);
  }
  
  private boolean checkPermission0(CrawlerFacade crawlFacade,
                                 Reader inputReader, String permissionUrl) {
    au = crawlFacade.getAu();
    crawlStatus = crawlFacade.getCrawlerStatus();
    probeUrl = null;
    CustomHtmlLinkExtractor extractor = new CustomHtmlLinkExtractor();
    logger.debug3("Checking permission on "+permissionUrl);
    try {
      // XXX ReaderInputStream needed until PermissionChecker changed to
      // take InputStream instead of Reader
      extractor.extractUrls(au, new ReaderInputStream(inputReader), null,
			       permissionUrl, new MyLinkExtractorCallback());
    } catch (IOException ex) {
      logger.error("Exception trying to parse permission url " + permissionUrl,
		   ex);
      crawlStatus.signalErrorForUrl(permissionUrl, 
                                    "Exception trying to parse permission url "
                                        + permissionUrl);
      return false;
    }
    if (probeUrl != null) {
      if (au.shouldBeCached(probeUrl)) {
        crawlFacade.addToPermissionProbeQueue(probeUrl);
        return true;
      } else {
        String errorMsg = "Probe url: " + probeUrl + " outside of crawl spec counting as no"
            + " permission on " + permissionUrl;
        logger.warning(errorMsg);
        crawlStatus.signalErrorForUrl(permissionUrl, errorMsg);
        return false;
      }
    } else {
      logger.warning("Unable to find probe url on " + permissionUrl);
      crawlStatus.signalErrorForUrl(permissionUrl, "Unable to find probe url on " + permissionUrl);
      return false;
    }
  }


  private static class CustomHtmlLinkExtractor
    extends GoslingHtmlLinkExtractor {

    private static final String LOCKSSPROBE = "lockss-probe";

    protected String extractLinkFromTag(StringBuffer link, ArchivalUnit au,
					LinkExtractor.Callback cb) {
      String returnStr = null;

      switch (link.charAt(0)) {
        case 'l': //<link href=blah.css>
        case 'L':
	  logger.debug3("Looking for probe in "+link);
	  if (beginsWithTag(link, LINKTAG)) {
	    returnStr = getAttributeValue(HREF, link);
	    String probeStr = getAttributeValue(LOCKSSPROBE, link);
	    if (!"true".equalsIgnoreCase(probeStr)) {
	      returnStr = null;
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
      if (probeUrl != null) {
	logger.warning("Multiple probe URLs found on manifest page.  " +
			"Old: "+probeUrl+" New: "+url);
      }
      probeUrl = url;
    }
  }
}
