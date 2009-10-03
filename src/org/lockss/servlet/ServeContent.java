/*
 * $Id: ServeContent.java,v 1.20.2.1 2009-10-03 02:54:24 tlipkis Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.List;
import java.net.*;
import java.text.*;
import org.apache.commons.collections.*;
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
import org.lockss.rewriter.*;

/** ServeContent servlet displays cached content with links
 *  rewritten.
 */
public class ServeContent extends LockssServlet {
  static final Logger log = Logger.getLogger("ServeContent");

  /** Prefix for this server's config tree */
  public static final String PREFIX = Configuration.PREFIX + "serveContent.";

  /** Return 404 for missing files */
  public static final int MISSING_FILE_ACTION_404 = 1;
  /** Forward requests for missing file to origin server.  (Not
   * implemented) */
  public static final int MISSING_FILE_ACTION_FORWARD_REQUEST = 3;

  /** Determines actions when a URL is requested that is not in the cache.
   * One of <code>Error_404</code>, <code>HostAuIndex</code>,
   * <code>AuIndex</code>, <code>ForwardRequest</code>. */
  public static final String PARAM_MISSING_FILE_ACTION =
    PREFIX + "missingFileAction";
  public static final MissingFileAction DEFAULT_MISSING_FILE_ACTION =
    MissingFileAction.HostAuIndex;;

  public enum MissingFileAction {
    Error_404,
      HostAuIndex,
      AuIndex,
      ForwardRequest}

  /** If true, rewritten links will be absolute
   * (http:/host:port/ServeContent?url=...).  If false, relative
   * (/ServeContent?url=...).  NodeFilterHtmlLinkRewriterFactory may
   * create bogus doubly-rewritten links if false. */
  public static final String PARAM_ABSOLUTE_LINKS =
    PREFIX + "absoluteLinks";
  public static final boolean DEFAULT_ABSOLUTE_LINKS = true;

  /** If true, the url arg to ServeContent will be normalized before being
   * looked up. */
  public static final String PARAM_NORMALIZE_URL_ARG =
    PREFIX + "normalizeUrlArg";
  public static final boolean DEFAULT_NORMALIZE_URL_ARG = true;

  /** Include in index AUs in listed plugins.  Set only one of
   * PARAM_INCLUDE_PLUGINS or PARAM_EXCLUDE_PLUGINS */
  public static final String PARAM_INCLUDE_PLUGINS =
    PREFIX + "includePlugins";

  public static final List DEFAULT_INCLUDE_PLUGINS = Collections.EMPTY_LIST;

  /** Exclude from index AUs in listed plugins.  Set only one of
   * PARAM_INCLUDE_PLUGINS or PARAM_EXCLUDE_PLUGINS */
  public static final String PARAM_EXCLUDE_PLUGINS =
    PREFIX + "excludePlugins";

  public static final List DEFAULT_EXCLUDE_PLUGINS = Collections.EMPTY_LIST;

  /** If true, Include internal AUs (plugin registries) in index */
  public static final String PARAM_INCLUDE_INTERNAL_AUS =
    PREFIX + "includeInternalAus";

  public static final boolean DEFAULT_INCLUDE_INTERNAL_AUS = false;

  private static MissingFileAction missingFileAction =
    DEFAULT_MISSING_FILE_ACTION;
  private static boolean absoluteLinks = DEFAULT_ABSOLUTE_LINKS;
  private static boolean normalizeUrl = DEFAULT_NORMALIZE_URL_ARG;
  private static List excludePlugins = DEFAULT_EXCLUDE_PLUGINS;
  private static List includePlugins = DEFAULT_INCLUDE_PLUGINS;
  private static boolean includeInternalAus = DEFAULT_INCLUDE_INTERNAL_AUS;

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
  private boolean enabledPluginsOnly;

  private PluginManager pluginMgr;

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
  }

  /** Called by ServletUtil.setConfig() */
  static void setConfig(Configuration config,
			Configuration oldConfig,
			Configuration.Differences diffs) {
    if (diffs.contains(PREFIX)) {
      missingFileAction =
	(MissingFileAction)config.getEnum(MissingFileAction.class,
					  PARAM_MISSING_FILE_ACTION,
					  DEFAULT_MISSING_FILE_ACTION);
      excludePlugins = config.getList(PARAM_EXCLUDE_PLUGINS,
				      DEFAULT_EXCLUDE_PLUGINS);
      includePlugins = config.getList(PARAM_INCLUDE_PLUGINS,
				      DEFAULT_INCLUDE_PLUGINS);
      includeInternalAus = config.getBoolean(PARAM_INCLUDE_INTERNAL_AUS,
					     DEFAULT_INCLUDE_INTERNAL_AUS);
      absoluteLinks = config.getBoolean(PARAM_ABSOLUTE_LINKS,
					DEFAULT_ABSOLUTE_LINKS);
      normalizeUrl = config.getBoolean(PARAM_NORMALIZE_URL_ARG,
					DEFAULT_NORMALIZE_URL_ARG);
    }
  }

  private boolean isIncludedAu(ArchivalUnit au) {
    String pluginId = au.getPlugin().getPluginId();
    if (!includePlugins.isEmpty()) {
      return includePlugins.contains(pluginId);
    }
    if (!excludePlugins.isEmpty()) {
      return !excludePlugins.contains(pluginId);
    }
    return true;
  }

  /** Pages generated by this servlet are static and cachable */
  @Override
  protected boolean mayPageBeCached() {
    return true;
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
    enabledPluginsOnly =
      !"no".equalsIgnoreCase(req.getParameter("filterPlugins"));

    verbose = getParameter("verbose");
    url = getParameter("url");
    if (!StringUtil.isNullString(url)) {
      if (normalizeUrl) {
	String normUrl = UrlUtil.normalizeUrl(url);
	if (normUrl != url) {
	  log.debug(url + " normalized to " + normUrl);
	  url = normUrl;
	}
      }
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
    try {
      // Get the CachedUrl for the URL, only if it has content.
      cu = pluginMgr.findCachedUrl(url, true);
      if (cu != null && cu.hasContent()) {
	handleCuRequest();
      } else {
	log.debug(url + " not found");
	handleMissingUrlRequest(url);
      }
    } catch (IOException e) {
      log.warning("Handling " + url + " throws ", e);
    } finally {
      if (cu != null) {
	cu.release();
      }
    }
  }

  // CU is known to exist and have content
  protected void handleCuRequest() throws IOException {
    CIProperties props = cu.getProperties();
    String cuLastModified = props.getProperty(CachedUrl.PROPERTY_LAST_MODIFIED);

    String ifModifiedSince = req.getHeader(HttpFields.__IfModifiedSince);
    if (ifModifiedSince != null) {
      try {
	if (!HeaderUtil.isEarlier(ifModifiedSince, cuLastModified)) {
	  resp.setStatus(HttpResponse.__304_Not_Modified);
	  return;
	}
      } catch (org.apache.commons.httpclient.util.DateParseException e) {
	// ignore error, serve file
      }
    }

    ctype = props.getProperty(CachedUrl.PROPERTY_CONTENT_TYPE);
    String mimeType = HeaderUtil.getMimeTypeFromContentType(ctype);
    log.debug2(url + " type " + ctype + " size " + cu.getContentSize());
    resp.setContentType(ctype);
    resp.setHeader(HttpFields.__LastModified, cuLastModified);
    AuState aus = AuUtil.getAuState(cu.getArchivalUnit());
    if (!aus.isOpenAccess()) {
      resp.setHeader(HttpFields.__CacheControl, "private");
    }    
    Writer outWriter = null;
    Reader original = cu.openForReading();
    Reader rewritten = original;
    try {
      outWriter = resp.getWriter();
      LinkRewriterFactory lrf = cu.getLinkRewriterFactory();
      if (!StringUtil.isNullString(getParameter("norewrite"))) {
	log.info("Not rewriting " + url);
	lrf = null;
      }
      if (lrf != null) {
	try {
	  rewritten =
	    lrf.createLinkRewriterReader(mimeType,
					 cu.getArchivalUnit(),
					 original,
					 cu.getEncoding(),
					 url,
					 new ServletUtil.LinkTransform() {
					   public String rewrite(String url) {
					     if (absoluteLinks) {
					       return srvAbsURL(myServletDescr(),
								"url=" + url);
					     } else {
					       return srvURL(myServletDescr(),
							     "url=" + url);
					     }
					   }});
	} catch (PluginException e) {
	  log.error("Can't create link rewriter " + e.toString());
	}
      }
      long bytes = StreamUtil.copy(rewritten, outWriter);
      if (bytes <= Integer.MAX_VALUE) {
	  resp.setContentLength((int)bytes);
      }
    } finally {
      IOUtil.safeClose(outWriter);
      IOUtil.safeClose(original);
      IOUtil.safeClose(rewritten);
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
    switch (missingFileAction) {
    case Error_404:
      resp.sendError(HttpServletResponse.SC_NOT_FOUND,
		     missingUrl + " is not preserved on this LOCKSS box");
      break;
    case HostAuIndex:
      Collection candidateAus = pluginMgr.getCandidateAus(missingUrl);
      if (candidateAus != null && !candidateAus.isEmpty()) {
	displayIndexPage(candidateAus,
			 HttpResponse.__404_Not_Found,
			 "Requested URL ( " + missingUrl
			 + " ) is not preserved on this LOCKSS box.  "
			 + "Possibly related content may be found "
			 + "in the following Archival Units");
      } else {
	resp.sendError(HttpServletResponse.SC_NOT_FOUND,
		       missingUrl + " is not preserved on this LOCKSS box");
      }
      break;
    case AuIndex:
      displayIndexPage(pluginMgr.getAllAus(),
		       HttpResponse.__404_Not_Found,
		       "Requested URL ( " + missingUrl
		       + " ) is not preserved on this LOCKSS box.");
      break;
    case ForwardRequest:
      // Easiest way to do this is probably to return without handling the
      // request and add a proxy handler to the context.
      resp.sendError(HttpServletResponse.SC_NOT_FOUND, err); // placeholder
      break;
    }
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
    displayIndexPage(pluginMgr.getAllAus(), -1, null);
  }

  void displayIndexPage(Collection<ArchivalUnit> auList,
			int result,
			String header)
    throws IOException {
    Predicate pred;
    boolean offerUnfilteredList = false;
    if (enabledPluginsOnly) {
      pred = PredicateUtils.andPredicate(enabledAusPred, allAusPred);
      offerUnfilteredList = areAnyExcluded(auList, enabledAusPred);
    } else {
      pred = allAusPred;
    }
    Page page = newPage();

    if (areAllExcluded(auList, pred) && !offerUnfilteredList) {
      ServletUtil.layoutExplanationBlock(page,
					 "No content has been preserved on this LOCKSS box");
    } else {
      // Layout manifest index w/ URLs pointing to this servlet
      Element ele =
	ServletUtil.manifestIndex(pluginMgr,
				  auList,
				  pred,
				  header,
				  new ServletUtil.ManifestUrlTransform() {
				    public Object transformUrl(String url) {
				      return srvLink(myServletDescr(),
						     url,
						     "url=" + url);
				    }},
				  true);
      page.add(ele);
      if (offerUnfilteredList) {
	Block centeredBlock = new Block(Block.Center);
	centeredBlock.add("<br>");
	centeredBlock.add("Other possibly relevant content has not yet been "
			  + "certified for use with ServeContent and may not "
			  + "display correctly.  Click ");
	Properties args = getParamsAsProps();
	args.put("filterPlugins", "no");
	centeredBlock.add(srvLink(myServletDescr(), "here", args));
	centeredBlock.add(" to see the complete list.");
	page.add(centeredBlock);
      }
    }
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
    if (result > 0) {
      resp.setStatus(result);
    }
  }

  boolean areAnyExcluded(Collection<ArchivalUnit> auList, Predicate pred) {
    for (ArchivalUnit au : auList) {
      if (!pred.evaluate(au)) {
	return true;
      }
    }
    return false;
  }

  boolean areAllExcluded(Collection<ArchivalUnit> auList, Predicate pred) {
    for (ArchivalUnit au : auList) {
      if (pred.evaluate(au)) {
	return false;
      }
    }
    return true;
  }

  // true of non-registry AUs, or all AUs if includeInternalAus
  Predicate allAusPred = new Predicate() {
      public boolean evaluate(Object obj) {
	return includeInternalAus
	  || !pluginMgr.isInternalAu((ArchivalUnit)obj);
      }};

  // true of AUs belonging to plugins included by includePlugins and
  // excludePlugins
  Predicate enabledAusPred = new Predicate() {
      public boolean evaluate(Object obj) {
	return isIncludedAu((ArchivalUnit)obj);
      }};
}
