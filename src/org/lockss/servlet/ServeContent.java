/*
 * $Id: ServeContent.java,v 1.9 2008-07-16 00:12:09 tlipkis Exp $
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

package org.lockss.servlet;

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.text.*;
import org.mortbay.http.*;
import org.mortbay.html.*;
import org.mortbay.servlet.MultiPartRequest;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.jetty.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.filter.*;
import org.lockss.state.*;

/** ServeContent servlet displays cached content using Javascript
 *  link re-writing courtesy of WERA.
 */
public class ServeContent extends LockssServlet {
  static final Logger log = Logger.getLogger("ServeContent");

  /** Prefix for this server's config tree */
  public static final String PREFIX = Configuration.PREFIX + "serveContent.";

  /** Return 404 for missing files */
  public static final int MISSING_FILE_ACTION_404 = 1;
  /** Display error page (200 response) for missing files */
  public static final int MISSING_FILE_ACTION_DISPLAY_ERROR = 2;
  /** Forward requests for missing file to origin server.  (Not
   * implemented) */
  public static final int MISSING_FILE_ACTION_FORWARD_REQUEST = 3;

  /** Jetty server name */
  public static final String PARAM_MISSING_FILE_ACTION =
    PREFIX + "missingFileAction";

  public static final int DEFAULT_MISSING_FILE_ACTION =
    MISSING_FILE_ACTION_404;

  private String action;
  private String verbose;
  private String url;
  private String doi;
  private String issn;
  private String volume;
  private String issue;
  private String spage;
  private String ctype;
  private CachedUrl cu;
  private long clen;

  private PluginManager pluginMgr;
  private AdminServletManager srvltMgr;

  // If we can't resolve a DOI, here is where to send it
  private static final String DOI_LOOKUP_URL = "http://dx.doi.org/";
  // If we can't resolve an OpenURL, here is where to send it
  // XXX find the place to send it
  private static final String OPENURL_LOOKUP_URL = "http://www.lockss.org/";

  // don't hold onto objects after request finished
  protected void resetLocals() {
    cu = null;
    url = null;
    doi = null;
    issn = null;
    volume = null;
    issue = null;
    spage = null;
    ctype = null;
    super.resetLocals();
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    pluginMgr = getLockssDaemon().getPluginManager();
    try {
      srvltMgr =
	(AdminServletManager) getLockssDaemon().getServletManager();
    } catch (RuntimeException e) {
      log.warning("Can't find AdminServletManager", e);
    }
  }

  /**
   * Handle a request
   * @throws IOException
   */
  public void lockssHandleRequest() throws IOException {
    if (!pluginMgr.areAusStarted()) {
      displayNotStarted();
      return;
    }
    verbose = getParameter("verbose");
    url = getParameter("url");
    if (!StringUtil.isNullString(url)) {
      handleUrlRequest();
      return;
    }
    doi = getParameter("doi");
    if (!StringUtil.isNullString(doi)) {
      handleDoiRequest();
      return;
    }
    issn = getParameter("issn");
    volume = getParameter("volume");
    issue = getParameter("issue");
    spage = getParameter("spage");
    log.debug("issn " + issn + " volume " + volume + " issue " + issue + " spage " + spage);
    if (!StringUtil.isNullString(issn) &&
	!StringUtil.isNullString(volume) &&
	!StringUtil.isNullString(issue) &&
	!StringUtil.isNullString(spage)) {
      handleOpenUrlRequest();
      return;
    }
    displayIndexPage();
  }

  protected void handleUrlRequest() throws IOException {
    log.debug("url " + url);
    // Get the CachedUrl for the URL, only if it has content.
    cu = pluginMgr.findCachedUrl(url, true);
    if (cu == null || !cu.hasContent()) {
      log.debug(url + " not found");
      handleMissingUrlRequest(url);
      return;
    }
    handleCuRequest();
  }

  protected void handleCuRequest() {
    clen = cu.getContentSize();
    try {
      CIProperties props = cu.getProperties();
      ctype = props.getProperty(CachedUrl.PROPERTY_CONTENT_TYPE);
      log.debug(url + " type " + ctype + " size " + clen);
      displayContent();
    } finally {
      cu.release();
    }
  }

  protected void handleDoiRequest() throws IOException {
    log.debug("doi " + doi);
    // find the URL for the DOI
    url = Metadata.doiToUrl(doi);
    log.debug(doi + " = " + (url == null ? "null" : url));
    if (url == null) {
      handleMissingUrlRequest(DOI_LOOKUP_URL + doi);
    } else {
      handleUrlRequest();
    }
  }

  protected void handleOpenUrlRequest() throws IOException {
    String openUrl = issn + "/" + volume + "/" + issue + "/" + spage;
    log.debug("OpenUrl " + openUrl);
    // find the URL for the OpenURL
    url = Metadata.openUrlToUrl(openUrl);
    log.debug(openUrl + " = " + (url == null ? "null" : url));
    if (url == null) {
      handleMissingUrlRequest(OPENURL_LOOKUP_URL + openUrl);
    } else {
      handleUrlRequest();
    }
  }

  protected void handleMissingUrlRequest(String missingUrl)
      throws IOException {
    String err = "URL " + missingUrl + " not found";
    int noUrlAction = CurrentConfig.getIntParam(PARAM_MISSING_FILE_ACTION,
						DEFAULT_MISSING_FILE_ACTION);
    switch (noUrlAction) {
    case MISSING_FILE_ACTION_404:
      resp.sendError(HttpServletResponse.SC_NOT_FOUND,
		     missingUrl + "  not found on this LOCKSS box");
      break;
    case MISSING_FILE_ACTION_DISPLAY_ERROR:
      displayError("URL " + missingUrl + " not found");
      break;
    case MISSING_FILE_ACTION_FORWARD_REQUEST:
      // Easiest way to do this is probably to return without handling the
      // request and add a proxy handler to the context.
      resp.sendError(HttpServletResponse.SC_NOT_FOUND, err); // placeholder
      break;
    }
  }

  void displayContent() {
    if (log.isDebug3()) {
      log.debug3("url: " + url);
      log.debug3("ctype: " + ctype);
      log.debug3("clen: " + clen);
    }
    resp.setContentType(ctype);
    Writer outWriter = null;
    Reader rewritten = null;
    try {
      outWriter = resp.getWriter();
      rewritten = cu.openForReadingWithRewriting();
      long bytes = StreamUtil.copy(rewritten, outWriter);
      if (bytes <= Integer.MAX_VALUE) {
	  resp.setContentLength((int)bytes);
      }
    } catch (IOException e) {
      log.warning("Copying CU to HTTP stream", e);
    } finally {
      IOUtil.safeClose(outWriter);
      IOUtil.safeClose(rewritten);
    }
    cu.release();
  }

  void displayError(String error) throws IOException {
    Page page = newPage();
    Composite comp = new Composite();
    comp.add("<center><font color=red size=+1>");
    comp.add(error);
    comp.add("</font></center><br>");
    page.add(comp);
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
  }

  void displayIndexPage() throws IOException {
    Page page = newPage();
    // Sort list of AUs by au.getName()
    java.util.List auList = pluginMgr.getAllAus();
    Collections.sort(auList, new AuOrderComparator());

    for (Iterator iter = auList.iterator(); iter.hasNext(); ) {
      ArchivalUnit au = (ArchivalUnit)iter.next();
      if (pluginMgr.isInternalAu(au) || !(au instanceof BaseArchivalUnit)) {
	continue;
      }
      java.util.List permissions = au.getCrawlSpec().getPermissionPages();
      if (!permissions.isEmpty()) {
	page.add("<br>");
	boolean first = true;
	for (Iterator it = permissions.iterator(); it.hasNext(); ) {
	  String url = (String)it.next();
	  if (first) {
	    page.add("<a href=\"");
	    page.add("/ServeContent?url=" + url);
	    page.add("\">" + au.getName() + "</a>");
	    first = false;
	  } else {
	    page.add(" <a href=\"");
	    page.add("/ServeContent?url=" + url);
	    page.add("\">and</a>");
	  }
	}
	page.add(".\n");
      }
    }
    page.add("<br>");
    ServletUtil.writePage(resp, page);
  }

}
