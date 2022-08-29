/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.*;
import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.lockss.alert.Alert;
import org.lockss.app.LockssDaemon;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.daemon.OpenUrlResolver.OpenUrlInfo;
import org.lockss.daemon.OpenUrlResolver.OpenUrlInfo.ResolvedTo;
import org.lockss.exporter.biblio.BibliographicItem;
import org.lockss.exporter.counter.*;
import org.lockss.exporter.counter.CounterReportsRequestRecorder.PublisherContacted;
import org.lockss.plugin.*;
import org.lockss.plugin.AuUtil.AuProxyInfo;
import org.lockss.plugin.PluginManager.CuContentReq;
import org.lockss.plugin.base.BaseUrlFetcher;
import org.lockss.proxy.ProxyManager;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.state.AuState;
import org.lockss.util.*;
import org.lockss.util.CloseCallbackInputStream.DeleteFileOnCloseInputStream;
import org.lockss.util.urlconn.*;
import org.mortbay.html.*;
import org.mortbay.http.*;

/** ServeContent servlet displays cached content with links rewritten.
 */

/* PathInfo option not fully implemented.

 * Links can be rewritten (i.e., original URLs can be embedded in
 * ServeContent URLs) in two ways:<ul>
 *
 * <li>http://.../ServeContent?url=<i>url-encoded-original-url</i></li>
 * <li>http://.../ServeContent/<i>original-url</i></li>
 * </ul>
 *
 * The first is always used when additional arguments (<i>eg</i> auid,
 * version) must be included.  The second (enabled by setting {@value
 * PARAM_REWRITE_STYLE} to <tt>PathInfo</tt>) is needed for sites where
 * rewritten URLs on the page are examined by code on the page (<i>eg</i>
 * PDF.js, which looks for a "file" query arg in the iframe's URL).
 */
@SuppressWarnings("serial")
public class ServeContent extends LockssServlet {

  private static final Logger log = Logger.getLogger(ServeContent.class);

  /** Prefix for this server's config tree */
  public static final String PREFIX = Configuration.PREFIX + "serveContent.";

  /** Determines action taken when a requested file is not cached locally,
   * and it's not available from the publisher.  "Not available" means any
   * of:
   * <ul><li>neverProxy is true,</li>
   * <li>the publisher's site did not respond</li>
   * <li>the publisher returned a response code other than 200</li>
   * <li>the publisher's site did not respond recently and
   *     proxy.hostDownAction is set to HOST_DOWN_NO_CACHE_ACTION_504</li>
   * </ul>
   * Can be set to one of:
   *  <ul>
   *   <li><tt>Error_404</tt>: Return a 404.</li>
   *   <li><tt>HostAuIndex</tt>: Generate an index of all AUs with content
   *     on the same host.</li>
   *   <li><tt>AuIndex</tt>: Generate an index of all AUs.</li>
   *   <li><tt>Redirect</tt>: Respond with a redirect to the publisher iff
   *     it isn't known to be down, else same as HostAuIndex.</li>
   *   <li><tt>AlwaysRedirect</tt>: Respond with a redirect to the
   *     publisher.</li>
   *  </ul>
   *  This option does not affect requests that contain a version parameter.
   */
  public static final String PARAM_MISSING_FILE_ACTION =
      PREFIX + "missingFileAction";
  public static final MissingFileAction DEFAULT_MISSING_FILE_ACTION =
      MissingFileAction.HostAuIndex;

  /** Determines when to follow redirect returned by publisher.
   * Can be set to one of:
   *  <ul>
   *   <li><tt>Never</tt></li>
   *   <li><tt>Always</tt></li>
   *   <li><tt>Missing</tt>: Only if URL is not present in cache.</li>
   *  </ul>
   */
  public static final String PARAM_FOLLOW_PUBLISHER_REDIRECT =
      PREFIX + "followPublisherRedirect";
  public static final FollowPublisherRedirectCondition DEFAULT_FOLLOW_PUBLISHER_REDIRECT = FollowPublisherRedirectCondition.Missing;

  /** The log level at which to log all content server accesses.
   * To normally log all content accesses (proxy or ServeContent), set to
   * <tt>info</tt>.  To disable set to <tt>none</tt>. */
  static final String PARAM_ACCESS_LOG_LEVEL = PREFIX + "accessLogLevel";
  static final String DEFAULT_ACCESS_LOG_LEVEL = "info";

  /** If true all content accesses raise an alert. */
  static final String PARAM_ACCESS_ALERTS_ENABLED =
    PREFIX + "accessAlertsEnabled";
  static final boolean DEFAULT_ACCESS_ALERTS_ENABLED = false;

  /** Determines action taken when a requested file is not cached locally,
   * and it's not available from the publisher.  "Not available" means any
   * of
   * <ul><li>neverProxy is true,</li>
   * <li>the publisher's site did not respond</li>
   * <li>the publisher returned a response code other than 200</li>
   * <li>the publisher's site did not respond recently and
   *     proxy.hostDownAction is set to HOST_DOWN_NO_CACHE_ACTION_504</li>
   * </ul> */
  public static enum MissingFileAction {
    /** Return a 404 */
    Error_404,
    /** Generate an index of all AUs with content on the same host. */
    HostAuIndex,
    /** Generate an index of all AUs. */
    AuIndex,
    /** Respond with a redirect to the publisher iff it isn't known to be
     * down, else same as HostAuIndex. */
    Redirect,
    /** Respond with a redirect to the publisher. */
    AlwaysRedirect,
  }

  /** Determines when to follow redirect returned by publisher.
   * Can be set to one of:
   *  <ul>
   *   <li><tt>Never</tt></li>
   *   <li><tt>Always</tt></li>
   *   <li><tt>Missing</tt>: Only if URL is not present in cache.</li>
   *  </ul>
   */
  public static enum FollowPublisherRedirectCondition {
    Always,
    Never,
    Missing,
  }

  /** Determines how original URLs are represented in ServeContent URLs.
   */
  public static enum RewriteStyle {
    /** Encode the original URL as a query arg in the ServeContent URL */
    QueryArg,
    /** Append the original URL to the ServeContent URL as extra path info. */
    PathInfo,
  }

  /** Determines how original URLs are represented in ServeContent URLs.
   * Can be set to one of:
   *  <ul>
   *   <li><tt>QueryArg</tt>: The original URL is encoded as a query arg
   *   in the ServeContent URL.</li>
   *   <li><tt>PathInfo</tt>: The original URL is appended to the
   *   ServeContent URL as extra path info.
   */
  public static final String PARAM_REWRITE_STYLE =
      PREFIX + "rewriteStyle";
  public static final RewriteStyle DEFAULT_REWRITE_STYLE =
      RewriteStyle.QueryArg;

  /** If true, rewritten links will be absolute
   * (http://host:port/ServeContent?url=...).  If false, relative
   * (/ServeContent?url=...). */
  public static final String PARAM_ABSOLUTE_LINKS =
      PREFIX + "absoluteLinks";
  public static final boolean DEFAULT_ABSOLUTE_LINKS = true;

  /**
   * If true, link rewriting in Memento responses will behave the same as link
   * rewriting in non-Memento responses; if false, links in Memento responses
   * will not be rewritten.
   */
  public static final String PARAM_REWRITE_MEMENTO_RESPONSES =
      PREFIX + "rewriteMementoResponses";
  public static final boolean DEFAULT_REWRITE_MEMENTO_RESPONSES = false;

  /** If true, the url arg to ServeContent will be minimally encoded before
   * being looked up. */
  static final String PARAM_MINIMALLY_ENCODE_URLS =
      PREFIX + "minimallyEncodeUrlArg";
  static final boolean DEFAULT_MINIMALLY_ENCODE_URLS = true;

  /** When rewriting a page that was redirected elsewhere, use the final
   * URL in the redirect chain as the base URL. */
  static final String PARAM_USE_REDIRECTED_BASE_URL =
      PREFIX + "useRedirectedBaseUrl";
  static final boolean DEFAULT_USE_REDIRECTED_BASE_URL = true;

  /** If true, the url arg to ServeContent will be normalized before being
   * looked up. */
  public static final String PARAM_NORMALIZE_URL_ARG =
      PREFIX + "normalizeUrlArg";
  public static final boolean DEFAULT_NORMALIZE_URL_ARG = true;

  /** If true, the url forwarded to the publisher is that of the CU, i.e.,
   * after plugin-specific normalization. */
  public static final String PARAM_NORMALIZE_FORWARDED_URL =
      PREFIX + "normalizeForwardedUrl";
  public static final boolean DEFAULT_NORMALIZE_FORWARDED_URL = true;

  /** Include in index AUs in listed plugins.  Set only one of
   * PARAM_INCLUDE_PLUGINS or PARAM_EXCLUDE_PLUGINS */
  public static final String PARAM_INCLUDE_PLUGINS =
      PREFIX + "includePlugins";
  public static final List<String> DEFAULT_INCLUDE_PLUGINS =
      Collections.emptyList();

  /** Exclude from index AUs in listed plugins.  Set only one of
   * PARAM_INCLUDE_PLUGINS or PARAM_EXCLUDE_PLUGINS */
  public static final String PARAM_EXCLUDE_PLUGINS =
      PREFIX + "excludePlugins";
  public static final List<String> DEFAULT_EXCLUDE_PLUGINS =
      Collections.emptyList();

  /** If true, Include internal AUs (plugin registries) in index */
  public static final String PARAM_INCLUDE_INTERNAL_AUS =
      PREFIX + "includeInternalAus";
  public static final boolean DEFAULT_INCLUDE_INTERNAL_AUS = false;

  /** Files smaller than this will be rewritten into an internal buffer so
   * that the rewritten size can be determined and sent in a
   * Content-Length: header.  Larger files will be served without
   * Content-Length: */
  public static final String PARAM_MAX_BUFFERED_REWRITE =
      PREFIX + "maxBufferedRewrite";
  public static final int DEFAULT_MAX_BUFFERED_REWRITE = 64 * 1024;

  /** If true, never forward request nor redirect to publisher */
  public static final String PARAM_NEVER_PROXY = PREFIX + "neverProxy";
  public static final boolean DEFAULT_NEVER_PROXY = false;
  static final String PARAM_PROCESS_FORMS = PREFIX + "handleFormPost";
  static final boolean DEFAULT_PROCESS_FORMS = false;

  // future param
  public static final String DEFAULT_404_CANDIDATES_MSG =
    "Possibly related content may be found "
    + "in the following Archival Units" ;

  private static MissingFileAction missingFileAction =
      DEFAULT_MISSING_FILE_ACTION;
  private static FollowPublisherRedirectCondition followPublisherRedirect =
    DEFAULT_FOLLOW_PUBLISHER_REDIRECT;
  private static boolean absoluteLinks = DEFAULT_ABSOLUTE_LINKS;
  private static RewriteStyle rewriteStyle = DEFAULT_REWRITE_STYLE;
  private static boolean rewriteMementoResponses = DEFAULT_REWRITE_MEMENTO_RESPONSES;
  private static boolean normalizeUrl = DEFAULT_NORMALIZE_URL_ARG;
  private static boolean normalizeForwardedUrl =
    DEFAULT_NORMALIZE_FORWARDED_URL;
  private static boolean minimallyEncodeUrl = DEFAULT_MINIMALLY_ENCODE_URLS;
  private static boolean useRedirectedBaseUrl = DEFAULT_USE_REDIRECTED_BASE_URL;
  private static List<String> excludePlugins = DEFAULT_EXCLUDE_PLUGINS;
  private static List<String> includePlugins = DEFAULT_INCLUDE_PLUGINS;
  private static boolean includeInternalAus = DEFAULT_INCLUDE_INTERNAL_AUS;
  private static int maxBufferedRewrite = DEFAULT_MAX_BUFFERED_REWRITE;
  private static boolean neverProxy = DEFAULT_NEVER_PROXY;
  private static int paramAccessLogLevel = -1;
  private static boolean paramAccessAlertsEnabled =
    DEFAULT_ACCESS_ALERTS_ENABLED;
  private static boolean processForms = DEFAULT_PROCESS_FORMS;
  private static String candidates404Msg = DEFAULT_404_CANDIDATES_MSG;
  private static int loginCheckerBufSize =
    BaseUrlFetcher.DEFAULT_LOGIN_CHECKER_MARK_LIMIT;


  private ArchivalUnit au;
  private ArchivalUnit explicitAu;
  private String url;
  private String cuUrl;	// CU's url (might differ from incoming url due to
			// normalizaton)
  private String baseUrl; // The base URL to use for resolving relative
			  // links when rewriting.  If redirected, this is
			  // the URL from which the content was served.
  private String versionStr; // non-null iff handling a (possibly-invalid)
			     // Memento request
  private CachedUrl cu;
  private boolean enabledPluginsOnly;
  private String accessLogInfo;
  private AccessLogType requestType = AccessLogType.None;

  private PluginManager pluginMgr;
  private ProxyManager proxyMgr;
  private OpenUrlResolver openUrlResolver;

  // don't hold onto objects after request finished
  protected void resetLocals() {
    accessLogInfo = null;
    requestType = AccessLogType.None;
    cu = null;
    cuUrl = null;
    baseUrl = null;
    url = null;
    versionStr = null;
    au = null;
    explicitAu = null;
    super.resetLocals();
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    LockssDaemon daemon = getLockssDaemon();
    pluginMgr = daemon.getPluginManager();
    proxyMgr = daemon.getProxyManager();
    openUrlResolver = new OpenUrlResolver(daemon);
  }

  /** Called by ServletUtil.setConfig() */
  static void setConfig(Configuration config,
                        Configuration oldConfig,
                        Configuration.Differences diffs) {
    if (diffs.contains(PREFIX)) {
      try {
        String accessLogLevel = config.get(PARAM_ACCESS_LOG_LEVEL,
            DEFAULT_ACCESS_LOG_LEVEL);
        paramAccessLogLevel = Logger.levelOf(accessLogLevel);
      } catch (RuntimeException e) {
        log.error("Couldn't set access log level", e);
        paramAccessLogLevel = -1;
      }
      paramAccessAlertsEnabled =
	config.getBoolean(PARAM_ACCESS_ALERTS_ENABLED,
			  DEFAULT_ACCESS_ALERTS_ENABLED);
      missingFileAction =
          (MissingFileAction)config.getEnum(MissingFileAction.class,
              PARAM_MISSING_FILE_ACTION,
              DEFAULT_MISSING_FILE_ACTION);
      followPublisherRedirect =
	(FollowPublisherRedirectCondition)
	config.getEnum(FollowPublisherRedirectCondition.class,
		       PARAM_FOLLOW_PUBLISHER_REDIRECT,
		       DEFAULT_FOLLOW_PUBLISHER_REDIRECT);
      excludePlugins = config.getList(PARAM_EXCLUDE_PLUGINS,
          DEFAULT_EXCLUDE_PLUGINS);
      includePlugins = config.getList(PARAM_INCLUDE_PLUGINS,
          DEFAULT_INCLUDE_PLUGINS);
      if (!includePlugins.isEmpty() && !excludePlugins.isEmpty()) {
        log.warning("Both " + PARAM_INCLUDE_PLUGINS + " and " +
                    PARAM_EXCLUDE_PLUGINS + " are set, ignoring " +
                    PARAM_EXCLUDE_PLUGINS);
      }
      includeInternalAus = config.getBoolean(PARAM_INCLUDE_INTERNAL_AUS,
          DEFAULT_INCLUDE_INTERNAL_AUS);
      absoluteLinks = config.getBoolean(PARAM_ABSOLUTE_LINKS,
          DEFAULT_ABSOLUTE_LINKS);
      rewriteStyle = (RewriteStyle)config.getEnum(RewriteStyle.class,
						  PARAM_REWRITE_STYLE,
						  DEFAULT_REWRITE_STYLE);
      normalizeUrl = config.getBoolean(PARAM_NORMALIZE_URL_ARG,
          DEFAULT_NORMALIZE_URL_ARG);
      normalizeForwardedUrl = config.getBoolean(PARAM_NORMALIZE_FORWARDED_URL,
          DEFAULT_NORMALIZE_FORWARDED_URL);

      minimallyEncodeUrl = config.getBoolean(PARAM_MINIMALLY_ENCODE_URLS,
          DEFAULT_MINIMALLY_ENCODE_URLS);
      useRedirectedBaseUrl =
	config.getBoolean(PARAM_USE_REDIRECTED_BASE_URL,
			  DEFAULT_USE_REDIRECTED_BASE_URL);
      neverProxy = config.getBoolean(PARAM_NEVER_PROXY,
          DEFAULT_NEVER_PROXY);
      maxBufferedRewrite = config.getInt(PARAM_MAX_BUFFERED_REWRITE,
          DEFAULT_MAX_BUFFERED_REWRITE);
      rewriteMementoResponses =
          config.getBoolean(PARAM_REWRITE_MEMENTO_RESPONSES,
              DEFAULT_REWRITE_MEMENTO_RESPONSES);
      processForms = config.getBoolean(PARAM_PROCESS_FORMS,
          DEFAULT_PROCESS_FORMS);
    }
    // XXX this is an inconsistent use of this param
    loginCheckerBufSize =
      config.getInt(BaseUrlFetcher.PARAM_LOGIN_CHECKER_MARK_LIMIT,
		    BaseUrlFetcher.DEFAULT_LOGIN_CHECKER_MARK_LIMIT);

  }

  protected boolean isInCache() {
    boolean res =  (cu != null) && cu.hasContent();
    if (res && explicitAu != null) {
      pluginMgr.promoteAuInSearchSets(explicitAu);
    }
    return res;
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

  protected boolean isNeverProxy() {
    return neverProxy ||
           !StringUtil.isNullString(getParameter("noproxy"));
  }

  protected boolean isNeverProxyForAu(ArchivalUnit au) {
    return isNeverProxy() || ((au != null) && AuUtil.isPubDown(au));
  }

  /** Pages generated by this servlet are static and cachable */
  @Override
  protected boolean mayPageBeCached() {
    return true;
  }

  enum AccessLogType { None, Url, Doi, OpenUrl };

  void logAccess(String msg) {
    if (paramAccessLogLevel >= 0) {
      msg += " in " + StringUtil.timeIntervalToString(reqElapsedTime());

      switch (requestType) {
        case None:
          logAccess(url, msg);
          break;
        case Url:
          logAccess("URL: " + url, msg);
          break;
        case Doi:
          logAccess("DOI: " + ((accessLogInfo==null) ? "" : accessLogInfo)
                    + " resolved to URL: " + url, msg);
          break;
        case OpenUrl:
          logAccess("OpenUrl: " + ((accessLogInfo==null) ? "" : accessLogInfo)
                    + " resolved to URL: " + url, msg);
          break;
      }
    }
  }

  void logAccess(String url, String msg) {
    String logmsg = "Content access from " + req.getRemoteAddr() + ": " +
      url + ": " + msg;
    if (paramAccessLogLevel >= 0) {
      log.log(paramAccessLogLevel, logmsg);
    }
    if (paramAccessAlertsEnabled) {
      alertMgr.raiseAlert(Alert.cacheAlert(Alert.CONTENT_ACCESS), logmsg);
    }
  }

  String present(boolean isInCache, String msg) {
    return isInCache ? "present, " + msg : "not present, " + msg;
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
    accessLogInfo = null;

    enabledPluginsOnly =
        !"no".equalsIgnoreCase(getParameter("filterPlugins"));

    String auid = null;
    String pathInfo = req.getPathInfo();

    // If true, redirect auid-only request to OpenURL for that content.
    // (This is what the Serve AU link has historically done.)  If false,
    // serve just the access page(s) for the specific AU
    boolean useOpenUrlForAuid = false;

    if (pathInfo != null && pathInfo.length() >= 1) {
      String query = req.getQueryString();
      if (query != null) {
	url = pathInfo.substring(1) + "?" + query;
      } else {
	url = pathInfo.substring(1);
      }
    } else {
      url = getParameter("url");
      auid = getParameter("auid");
      useOpenUrlForAuid = Boolean.parseBoolean(getParameter("use_openurl"));
    }
    versionStr = getParameter("version");

    au = explicitAu = null;		// redundant, just making sure

    if (!StringUtil.isNullString(url)) {
      if (StringUtil.isNullString(auid)) {
        if (isMementoRequest()) {

          // Error, because Memento requests must have auid.
          resp.sendError(
              HttpServletResponse.SC_BAD_REQUEST,
              "Requests containing a \"version\" parameter must also include " +
              "\"auid\" and \"url\" parameters; \"auid\" is missing.");
          return;
        }
      } else {
        explicitAu = pluginMgr.getAuFromId(auid);
        au = explicitAu;
      }
      if (log.isDebug2()) log.debug2("Url req, raw: " + url);
      // handle html-encoded URLs with characters like &amp;
      // that can appear as links embedded in HTML pages
      url = StringEscapeUtils.unescapeHtml4(url);
      requestType = AccessLogType.Url;
      //this is a partial replicate of proxy_handler post logic here
      // TODO check mime type and use that to determine what should be sent ctg
      if (HttpRequest.__POST.equals(req.getMethod()) && processForms) {
        log.debug2("POST request found!");
        FormUrlHelper helper = new FormUrlHelper(url.toString());
        Enumeration en = req.getParameterNames();
        while (en.hasMoreElements()) {
          String name = (String)en.nextElement();
          // filter out LOCKSS specific params
          if (   !"url".equals(name)
              && !"auid".equals(name)
              && !"version".equals(name)) {
            String vals[] = req.getParameterValues(name);
            for (String val : vals) {
              helper.add(name, val);
            }
          }
        }
        helper.sortKeyValues();
        org.mortbay.util.URI postUri =
            new org.mortbay.util.URI(helper.toEncodedString());
	log.debug2("POST URL: " + postUri);
        // We only want to override the post request by proxy if we cached it during crawling.
        CachedUrl cu = pluginMgr.findCachedUrl(postUri.toString());
        if (cu != null) {
          url = postUri.toString();
          if (log.isDebug2()) log.debug2("Setting url to:" + url);
        }
      }
      if (minimallyEncodeUrl) {
        String unencUrl = url;
        url = UrlUtil.minimallyEncodeUrl(url);
        if (!url.equals(unencUrl)) {
          log.debug2("Encoded " + unencUrl + " to " + url);
        }
      }
      if (normalizeUrl) {
        String normUrl;
        if (au != null) {
          try {
            normUrl = UrlUtil.normalizeUrl(url, au);
          } catch (PluginBehaviorException e) {
            log.warning("Couldn't site-normalize URL: " + url, e);
            normUrl = UrlUtil.normalizeUrl(url);
          }
        } else {
          normUrl = UrlUtil.normalizeUrl(url);
        }
        if (normUrl != url) {
          log.debug2("Normalized " + url + " to " + normUrl);
          url = normUrl;
        }
      }
      handleUrlRequest();
      return;
    }

    /*
     * Memento requests must provide URL; OpenURL requests are not Memento
     * requests, and vice versa.
     */
    if (isMementoRequest()) {
      resp.sendError(
          HttpServletResponse.SC_BAD_REQUEST,
          "Requests containing a \"version\" parameter must also include " +
          "\"auid\" and \"url\" parameters.");
      return;
    }

    // perform special handling for an OpenUrl
    try {
      OpenUrlInfo resolved = OpenUrlResolver.OPEN_URL_INFO_NONE;

      // If any params, pass them all to OpenUrl resolver
      Map<String,String> pmap = getParamsAsMap();
      if (!pmap.isEmpty()) {
        if (log.isDebug3()) log.debug3("Resolving OpenUrl: " + pmap);
        resolved = openUrlResolver.resolveOpenUrl(pmap);
	log.debug3("Resolved to: " + resolved);
        requestType = AccessLogType.OpenUrl;
      }

      // if there are multiple results, present choices
      if (resolved.size() > 1) {
        handleMultiOpenUrlInfo(resolved);
        return;
      }

      // if there is only one result, present it
      url = resolved.getResolvedUrl();
      if (   (url != null)
          || (resolved.getResolvedTo() != ResolvedTo.NONE)) {
        // record type of access for logging
        accessLogInfo = resolved.getResolvedTo().toString();
        handleOpenUrlInfo(resolved);
        return;
      }

      if (!useOpenUrlForAuid) {
	if (serveFromAuid(auid)) {
	  return;
	}

      }
      // redirect to the OpenURL corresponding to the specified auid;
      // ensures that the corresponding OpenURL is available to the client.
      if (auid != null) {
        String openUrlQueryString =
            openUrlResolver.getOpenUrlQueryForAuid(auid);
        if (openUrlQueryString != null) {
          StringBuffer sb = req.getRequestURL();
          sb.append("?");
          sb.append(openUrlQueryString);
          resp.sendRedirect(sb.toString());
          return;
        }
	// If open URL resolution fails fall back to first start page
	if (serveFromAuid(auid)) {
	  return;
	}
      }

      log.debug3("Unknown request");
    } catch (RuntimeException ex) {
      log.warning("Couldn't handle unknown request", ex);
    }
    // Maybe should display a message here if URL is unknown format.  But
    // this is also the default case for the bare ServeContent URL, which
    // should generate an index with no message.
    displayIndexPage();
    requestType = AccessLogType.None;
    logAccess("200 index page");
  }

  boolean serveFromAuid(String auid) throws IOException {
    au = pluginMgr.getAuFromId(auid);
    if (au != null) {
      Collection<String> starts = au.getAccessUrls();
      if (!starts.isEmpty()) {
	// look for a start URL with content
	for (String startUrl : starts) {
	  CachedUrl scu = au.makeCachedUrl(startUrl);
	  if (scu.hasContent()) {
	    url = startUrl;
	    handleUrlRequest();
	    return true;
	  }
	}
	// if none found, use first start URL
	url = starts.iterator().next();
	handleUrlRequest();
	return true;
      }
    }
    return false;
  }

  /**
   * Handle request for specified publisher URL. If the request includes a
   * version parameter, then serve content from the cache only and don't
   * rewrite. Otherwise, serve from publisher if possible and allowed by daemon
   * options, and cache if necessary, rewriting links either way.
   *
   * @throws IOException if cannot handle URL request.
   */
  protected void handleUrlRequest() throws IOException {
    log.debug2("url: " + url);
    log.debug2("is " + (isMementoRequest() ? "" : "not ") + "a Memento request.");
    try {
      // Get the CachedUrl for the URL, only if it has content.
      if (au != null) {
        cu = au.makeCachedUrl(url);
        if (isMementoRequest()) {

          // Replace CU with the historical CU; similar to ViewContent:
          try {
            CachedUrl newCu = getHistoricalCu(cu, versionStr);
            if (newCu != cu) {
              AuUtil.safeRelease(cu);
              cu = newCu;
            }

            // Add the optional Memento-Datetime header.
            CIProperties props = cu.getProperties();
            String lastMod   = props.getProperty(cu.PROPERTY_LAST_MODIFIED);
            String fetchTime = props.getProperty(cu.PROPERTY_FETCH_TIME);
            resp.setHeader("Memento-Datetime",
                (StringUtil.isNullString(lastMod)) ? fetchTime
                                                   : lastMod);
          } catch (VersionNotFoundException e) {
	    /*
	     * 404 error.  Should not use handleMissingUrlRequest, because it
	     * would tell the user that the URL param is not stored on this
	     * LOCKSS box, which might not be true, and we don't need to give
	     * the user a menu in response to a nonexistent version number.
	     */
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                "This LOCKSS box does not have a version " + versionStr + " of " + url + " for the requested AU.");
            AuUtil.safeRelease(cu);
            logAccess("version not present, 404");
            return;
          } catch (NumberFormatException e) {

            // 400 error.
            String message = "Couldn't parse version string: " + versionStr;
            logAccess(message);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
            AuUtil.safeRelease(cu);
            return;
          } // Not catching RuntimeException, which already results in a 500 response.

        }
      } else if (!isMementoRequest()) {
        // Find a CU with content if possible.  If none, find an AU where
        // it would fit so can rewrite content from publisher if necessary.
        cu = pluginMgr.findCachedUrl(url, CuContentReq.PreferContent);
        if (cu != null) {
	  cuUrl = cu.getUrl();
          au = cu.getArchivalUnit();
          if (log.isDebug3()) log.debug3("cu: " + cu + " au: " + au);
        }
      } else {
	/*
	 * This is a Memento request, and the AU param was provided, but we
	 * didn't find an AU with that AU ID. 404 error; should not let this
	 * pass through to handleMissingURlRequest, because we don't want
	 * Memento requests to result in redirects to the publisher.
	 */
        resp.sendError(HttpServletResponse.SC_NOT_FOUND,
            "This LOCKSS box does not have (any versions of) the requested AU.");
        AuUtil.safeRelease(cu);
        logAccess("AU not present, 404");
        return;
      }
      if (cu != null && cu.hasContent() && useRedirectedBaseUrl) {
	baseUrl = PluginUtil.getBaseUrl(cu);
      } else {
	baseUrl = url;
      }
      if (au != null) {
        handleAuRequest();
      } else {
        handleMissingUrlRequest(url, PubState.Unknown);
      }
    } catch (IOException e) {
      log.warning("Handling URL: " + url + " throws ", e);
      throw e;
    } finally {
      AuUtil.safeRelease(cu);
    }
  }

  /**
   * Given a CachedUrl and a string representation of a version number, returns
   * that version of the CachedUrl. Has no side effects within this instance.
   *
   * @param cachedUrl the CU that a version of is being requested
   * @param verStr string representation of the integer version being requested
   * @returns the requested version of the given cached URL
   * @throws NumberFormatException if verStr does not represent an integer
   * @throws VersionNotFoundException if cachedUrl lacks the requested version
   * @throws RuntimeException
   */
  private static CachedUrl getHistoricalCu(CachedUrl cachedUrl, String verStr)
      throws NumberFormatException, VersionNotFoundException, RuntimeException {
    CachedUrl result;
    int version = Integer.parseInt(verStr);
    int curVer = cachedUrl.getVersion();
    if (version == curVer) {
      result = cachedUrl;
    } else {
      CachedUrl verCu = cachedUrl.getCuVersion(version);
      if (verCu == null || !verCu.hasContent()) {
        throw new VersionNotFoundException();
      }
      result = verCu;
    }
    return result;
  }

  private static class VersionNotFoundException extends Exception {}

  /**
   * Redirect to the current URL. Uses response redirection
   * unless the URL has a reference, in which case it does
   * client side redirection.
   *
   * @throws IOException if "UTF-8" encoding is not supported
   */
  protected void redirectToUrl() throws IOException {
    // display cached page if it has content
    String ref = null;
    try {
      ref = new URL(url).getRef();
    } catch (MalformedURLException ex) {
      log.warning("Handling URL: " + url + " throws ", ex);
    }

    if (ref == null) {
      resp.sendRedirect(url);
    } else {
      // redirect because URL includes a reference: '#'
      // that can only be interpreted by the browser
      String plainUrl = url.substring(0, url.length()-ref.length()-1);
      StringBuffer sb = new StringBuffer();
      sb.append("url=");
      sb.append(URLEncoder.encode(plainUrl, "UTF-8"));
      if (au != null) {
        sb.append("&auid=" + au.getAuId());
      }
      sb.append("#" + ref);
      String suffix = sb.toString();

      String srvUrl = absoluteLinks
                      ? srvAbsURL(myServletDescr(), suffix)
                      : srvURL(myServletDescr(), suffix);

      Page p = new Page();
      p.addHeader(
          "<meta HTTP-EQUIV=\"REFRESH\" content=\"0,url=" + srvUrl + "\">");
      writePage(p);
    }
  }

  /**
   * Handler for missing OpenURL requests displays synthetic TOC
   * for level returned by OpenURL resolver and offers a link to
   * the URL at the publisher site.
   *
   * @param multiInfo the OpenUrlInfo from the OpenUrl resolver
   * @throws IOException if an IO error occurs
   */
  protected void handleMultiOpenUrlInfo(OpenUrlInfo multiInfo)
      throws IOException {
    Block detailBlock = new Block(Block.Center);
    Table headerTable = new Table(0,"");
    headerTable.addHeading("Found Matching Content from Multiple Sources",
                           "colspan=3");
    detailBlock.add(headerTable);

    Table detailTable = new Table(0, "cellspacing=6 cellpadding=0");
    detailTable.addHeading("Publisher", "style=\"text-align:left\"");
    detailTable.addHeading("Title", "style=\"text-align:left\"");
    detailTable.addHeading("OpenURL", "style=\"text-align:left\"");

    // display publisher, title, and link for each info result
    for (OpenUrlInfo info : multiInfo) {
      BibliographicItem bibItem = info.getBibliographicItem();
      String openUrlQuery = info.getOpenUrlQuery();
      if (openUrlQuery != null) {
        detailTable.newRow();
        detailTable.newCell();
        String publisherName = (bibItem == null)
                               ? null : bibItem.getPublisherName();
        if (publisherName != null) detailTable.add(publisherName);
        detailTable.newCell();
        String title = (bibItem == null)
                       ? null : bibItem.getPublicationTitle();
        if (title != null) detailTable.add(title);
        detailTable.newCell();
        detailTable.add(srvLink(myServletDescr(), openUrlQuery, openUrlQuery));
      }
    }

    detailBlock.add(detailTable);

    Page page = newPage();
    page.add(detailBlock);
    endPage(page);
  }

  /**
   * Handle request for the page specified OpenUrlInfo
   * that is returned from OpenUrlResolver.
   *
   * @param info the OpenUrlInfo from the OpenUrl resolver
   * @throws IOException if an IO error happens
   */
  protected void handleOpenUrlInfo(OpenUrlInfo info) throws IOException {
    log.debug2("resolvedTo: " + info.getResolvedTo() + " url: " + url);
    try {
      // If we resolved to a URL, get the CachedUrl
      if (url != null) {
        if (au != null) {
          // AU specified explicitly
          cu = au.makeCachedUrl(url);
        } else {
          // Find a CU with content if possible.  If none, find an AU where
          // it would fit so can rewrite content from publisher if necessary.
          cu = pluginMgr.findCachedUrl(url, CuContentReq.PreferContent);
          if (cu != null) {
            if (log.isDebug3()) log.debug3("cu: " + cu);
            au = cu.getArchivalUnit();
          }
        }
      }

      if (cu != null) {
        // display cached page if it has content
        String ref = null;
        try {
          ref = new URL(url).getRef();
        } catch (MalformedURLException ex) {
          log.warning("Handling URL: " + url + " throws ", ex);
        }

        if (ref != null) {
          redirectToUrl();
        } else {
          // handle urls without a reference normally
          handleAuRequest();
        }

        return;
      }

      handleMissingOpenUrlRequest(info, PubState.Unknown);

    } catch (IOException e) {
      log.warning("Handling URL: "
                  + ((url == null) ? "url" : url) + " throws ", e);
      throw e;
    } finally {
      AuUtil.safeRelease(cu);
    }
  }

  /**
   * Handle request for content that belongs to one of our AUs, whether or not
   * we have content for that URL.  If this request contains a version param,
   * serve it from cache with a Memento-Datetime header and no
   * link-rewriting.  For requests without a version param, rewrite links,
   * and serve from publisher if publisher provides it and the daemon options
   * allow it; otherwise, try to serve from cache.
   *
   * @throws IOException for IO errors
   */
  protected void handleAuRequest() throws IOException {
    String host = UrlUtil.getHost(url);
    boolean isInCache = isInCache();
    boolean isHostDown = proxyMgr.isHostDown(host);
    PublisherContacted pubContacted =
	CounterReportsRequestRecorder.PublisherContacted.FALSE;

    if (isNeverProxyForAu(au) || isMementoRequest()) {
      if (isInCache) {
        serveFromCache();
        logAccess("200 from cache");
        // Record the necessary information required for COUNTER reports.
	recordRequest(url, pubContacted, 200);
      } else {
	/*
	 * We don't want to redirect to the publisher, so pass KnownDown below
	 * in order to ensure that. It's true that we might be lying, because
	 * the publisher might be up.
	 */
        handleMissingUrlRequest(url, PubState.KnownDown);
      }
      return;
    }

    LockssUrlConnectionPool connPool = proxyMgr.getNormalConnectionPool();

    if (!isInCache && isHostDown) {
      switch (proxyMgr.getHostDownAction()) {
        case ProxyManager.HOST_DOWN_NO_CACHE_ACTION_504:
          handleMissingUrlRequest(url, PubState.RecentlyDown);
          return;
        case ProxyManager.HOST_DOWN_NO_CACHE_ACTION_QUICK:
          connPool = proxyMgr.getQuickConnectionPool();
          break;
        default:
        case ProxyManager.HOST_DOWN_NO_CACHE_ACTION_NORMAL:
          connPool = proxyMgr.getNormalConnectionPool();
          break;
      }
    }

    // Send request to publisher
    LockssUrlConnection conn = null;
    PubState pstate = PubState.Unknown;
    try {
      conn = openLockssUrlConnection(connPool);

      // set proxy for connection if specified
      AuProxyInfo info = AuUtil.getAuProxyInfo(au);
      String proxyHost = info.getHost();
      int proxyPort = info.getPort();
      if (!StringUtil.isNullString(proxyHost) && (proxyPort > 0)) {
        try {
          conn.setProxy(info.getHost(), info.getPort());
        } catch (UnsupportedOperationException ex) {
          log.warning(  "Unsupported connection request proxy: "
                        + proxyHost + ":" + proxyPort);
        }
      }
      conn.execute();
      pubContacted = CounterReportsRequestRecorder.PublisherContacted.TRUE;
    } catch (IOException ex) {
      if (log.isDebug3()) log.debug3("conn.execute", ex);

      // mark host down if connection timed out
      if (ex instanceof LockssUrlConnection.ConnectionTimeoutException) {
        proxyMgr.setHostDown(host, isInCache);
      } else {
	pubContacted = CounterReportsRequestRecorder.PublisherContacted.TRUE;
      }
      pstate = PubState.KnownDown;

      // tear down connection
      IOUtil.safeRelease(conn);
      conn = null;
    }

    int response = 0;
    try {
      if (conn != null) {
	response = conn.getResponseCode();
        if (log.isDebug2())
          log.debug2("response: " + response + " " + conn.getResponseMessage());
        if (response == HttpResponse.__200_OK) {
          // If publisher responds with content, serve it to user
          // XXX Should check for a login page here
          try {
            serveFromPublisher(conn);
            logAccess(present(isInCache, "200 from publisher"));
            // Record the necessary information required for COUNTER reports.
	    recordRequest(url, pubContacted, response);
            return;
          } catch (CacheException.PermissionException ex) {
            logAccess("login exception: " + ex.getMessage());
            pstate = PubState.NoContent;
          }
        } else {
          pstate = PubState.NoContent;
        }
      }
    } finally {
      // ensure connection is closed
      IOUtil.safeRelease(conn);
    }

    // Either failed to open connection or got non-200 response.
    if (isInCache) {
      serveFromCache();
      logAccess("present, 200 from cache");
      // Record the necessary information required for COUNTER reports.
      recordRequest(url, pubContacted, response);
    } else {
      log.debug2("No content for: " + url);
      // return 404 with index
      handleMissingUrlRequest(url, pstate);
    }
  }

  /**
   * @return true iff the user is requesting a particular version of the content
   */
  private boolean isMementoRequest() {
    return !StringUtil.isNullString(versionStr);
  }

  /**
   * Record the request in COUNTER if appropriate
   */
  void recordRequest(String url,
		     CounterReportsRequestRecorder.PublisherContacted contacted,
		     int publisherCode) {
    if (proxyMgr.isCounterCountable(req.getHeader(HttpFields.__UserAgent))) {
      CounterReportsRequestRecorder.getInstance().recordRequest(url, contacted,
	  publisherCode, null);
    }
  }

  /**
   * Serve the content for the specified CU from the cache.
   *
   * @throws IOException if cannot read content
   */
  protected void serveFromCache() throws IOException {
    CIProperties props = cu.getProperties();
    String cuLastModified = props.getProperty(CachedUrl.PROPERTY_LAST_MODIFIED);
    String ifModifiedSince = req.getHeader(HttpFields.__IfModifiedSince);
    String ctype;

    if (ifModifiedSince != null && cuLastModified != null) {
      try {
        if (!HeaderUtil.isEarlier(ifModifiedSince, cuLastModified)) {
          ctype = cu.getContentType();
          String mimeType = HeaderUtil.getMimeTypeFromContentType(ctype);
          if (log.isDebug3()) {
            log.debug3( "Cached content not modified for: " + url
                        + " mime type=" + mimeType
                        + " size=" + cu.getContentSize()
                        + " cu=" + cu);
          }
          resp.setStatus(HttpResponse.__304_Not_Modified);
          return;
        }
      } catch (org.apache.commons.httpclient.util.DateParseException e) {
        // ignore error, serve file
        log.warning("Handling ifModifiedSince: " + ifModifiedSince
                    + "or cuLastModified: " + cuLastModified + " throws ", e);
      }
    }

    ctype = cu.getContentType();
    String mimeType = HeaderUtil.getMimeTypeFromContentType(ctype);

    if (log.isDebug3()) {
      log.debug3( "Serving cached content for: " + url
                  + " mime type=" + mimeType
                  + " size=" + cu.getContentSize()
                  + " cu=" + cu);
    }
    resp.setContentType(ctype);

    // If no Content-Disposition, set as inline content with name
    String cdisp = props.getProperty("Content-Disposition");
    if (cdisp == null) {
      String fname =
        ObjectUtils.defaultIfNull(ServletUtil.getContentOriginalFilename(cu, true),
                                  "UnnamedContent");
      cdisp = "inline; filename=" + fname;
    }
    resp.setHeader("Content-Disposition", cdisp);

    if (cuLastModified != null) {
      resp.setHeader(HttpFields.__LastModified, cuLastModified);
    }

    AuState aus = AuUtil.getAuState(au);
    if (!aus.isOpenAccess()) {
      resp.setHeader(HttpFields.__CacheControl, "private");
    }

    // Add a header to the response to identify content from LOCKSS cache
    resp.setHeader(Constants.X_LOCKSS, Constants.X_LOCKSS_FROM_CACHE);

    // Indicate the AU the content came from
    resp.setHeader(Constants.X_LOCKSS_FROM_AUID, au.getAuId());

    // rewrite content from cache
    CharsetUtil.InputStreamAndCharset isc = CharsetUtil.getCharsetStream(cu);
    handleRewriteInputStream(isc.getInStream(), mimeType,
			     isc.getCharset(), cu.getContentSize());
  }

  /**
   * Return the input stream for this connection, after first determining
   * that it is not to a login page. Be sure to close the returned input
   * stream when done with it.
   *
   * @param conn the connection
   * @return the input stream
   * @throws IOException if error getting input stream
   * @throws CacheException.PermissionException if the connection is to a
   * login page or there was an error checking for a login page
   */
  private InputStream getInputStream(LockssUrlConnection conn)
      throws CacheException.PermissionException, IOException {
    // get the input stream from the connection
    InputStream input = conn.getResponseInputStream();
    input = checkLoginPage(input, conn);
    return input;
  }

  /**
   * Check whether the input stream is to a publisher's html permission page.
   * Throws a CacheExcepiton.PermissionException if so; otherwise returns an
   * input stream positioned at the same position. The original input stream
   * will be closed if a new one is created.
   *
   * @param input an input stream
   * @param conn the url connection from which to get response info
   * @return an input stream positioned at the same position as the original
   *  one if the original stream is not a login page
   * @throws CacheException.PermissionException if the connection is to a
   *  login page or the LoginPageChecker for the plugin reported an error
   * @throws IOException if IO error while checking the stream
   */
  private InputStream checkLoginPage(InputStream input,
				     LockssUrlConnection conn)
      throws CacheException.PermissionException, IOException {

    LoginPageChecker checker = au.getLoginPageChecker();
    if (checker == null) {
      return input;
    }
    // only check for HTML login pages to avoid doing unnecessary
    // work for other document types; publishers typically return
    // a 200 response code and an HTML page with a login form
    String ctype = conn.getResponseContentType();
    String mimeType = HeaderUtil.getMimeTypeFromContentType(ctype);
    if (!Constants.MIME_TYPE_HTML.equalsIgnoreCase(mimeType)) {
      return input;
    }

    InputStream oldInput = input;
    // copy page to tmp file to allow login page checker to read it
    DeferredTempFileOutputStream dos =
      new DeferredTempFileOutputStream(loginCheckerBufSize);
    try {
      StreamUtil.copy(input, dos);
    } finally {
      IOUtil.safeClose(oldInput);
      IOUtil.safeClose(dos);
    }
    // Get response headers in form required by login page checker
    CIProperties headers = ciPropsFromConnResponse(conn);

    try {
      input = dos.getInputStream();
    } catch (IOException e) {
      dos.deleteTempFile();
      throw e;
    }
    // Decompress if necessary before invoking checker
    // XXX Refactor to decompress higher up so don't have to repeat for rewriter
    String contentEncoding = conn.getResponseContentEncoding();
    if (contentEncoding != null) {
      if (log.isDebug3())
	log.debug3("Wrapping for login page checker Content-Encoding: " +
		   contentEncoding);
      InputStream uncin =
	StreamUtil.getUncompressedInputStreamOrFallback(input,
							contentEncoding,
							url);
      if (uncin != input) {
	// Stream is no longer encoded, and we don't know its length
	// (not that the login page checker is likely to care)
	headers.remove(HttpFields.__ContentEncoding);
	headers.remove(HttpFields.__ContentLength);
	input = uncin;
      }
    }
    // create reader for input stream

    // guessCharsetFromStream() requires markable stream
    input = StreamUtil.getResettableInputStream(input);
    String charset = CharsetUtil.guessCharsetFromStream(input,ctype);

    try {
      Reader reader = new InputStreamReader(input, charset);

      if (checker.isLoginPage(headers, reader)) {
	IOUtil.safeClose(input);
	input = null;
	dos.deleteTempFile();
	throw new CacheException.PermissionException("Found a login page");
      }
    } catch (PluginException e) {
      CacheException.PermissionException ex =
	new CacheException.PermissionException("Error checking login page");
      ex.initCause(e);
      IOUtil.safeClose(input);
      input = null;
      dos.deleteTempFile();
      throw ex;
    } finally {
      if (input == null) {
	// delete the temp file immediately if not returning an input
	// stream open on it
	dos.deleteTempFile();
      } else {
	IOUtil.safeClose(input);
	return dos.getDeleteOnCloseInputStream();
      }
    }

    return input;
  }

  CIProperties ciPropsFromConnResponse(LockssUrlConnection conn) {
    CIProperties res = new CIProperties();
    for (int i = 0; true; i++) {
      String key = conn.getResponseHeaderFieldKey(i);
      String val = conn.getResponseHeaderFieldVal(i);
      if ((key == null) && (val == null)) {
	break;
      }
      res.put(key, val);
    }
    return res;
  }


  /**
   * Serve content from publisher.
   *
   * @param conn the connection
   * @throws IOException if cannot read content
   */
  protected void serveFromPublisher(LockssUrlConnection conn)
      throws IOException {
    String ctype = conn.getResponseContentType();
    String mimeType = HeaderUtil.getMimeTypeFromContentType(ctype);
    log.debug2(  "Serving publisher content for: " + url
                 + " mime type=" + mimeType + " size=" + conn.getResponseContentLength());


    // copy connection response headers to servlet response
    for (int i = 0; true; i++) {
      String key = conn.getResponseHeaderFieldKey(i);
      String val = conn.getResponseHeaderFieldVal(i);
      if ((key == null) && (val == null)) {
        break;
      }
      if ((key == null) || (val == null)) {
        // XXX Here to ensure complete compatibility with previous
        // code. Not sure it's necessary or desirable.
        continue;
      }
      // Content-Encoding processed below
      // Don't copy the following headers:
      if (HttpFields.__ContentEncoding.equalsIgnoreCase(key) ||
          HttpFields.__KeepAlive.equalsIgnoreCase(key) ||
          HttpFields.__Connection.equalsIgnoreCase(key)) {
        continue;
      }
      if (HttpFields.__ContentType.equalsIgnoreCase(key)) {
        // Must use setContentType() for Content-Type, both to replace the
        // default that LockssServlet stored, and to ensure proper charset
        // processing
        resp.setContentType(val);
      } else {
        resp.addHeader(key, val);
      }
    }

    resp.addHeader(HttpFields.__Via,
        proxyMgr.makeVia(getMachineName(), reqURL.getPort()));

    String contentEncoding = conn.getResponseContentEncoding();
    long responseContentLength = conn.getResponseContentLength();

    // get input stream and encoding
    InputStream respStrm = getInputStream(conn);

    LinkRewriterFactory lrf = getLinkRewriterFactory(mimeType);
    if (lrf != null) {
      // we're going to rewrite, must deal with content encoding.
      if (contentEncoding != null) {
        if (log.isDebug2())
          log.debug2("Wrapping Content-Encoding: " + contentEncoding);
        // XXX If/when upstream compression is again supported, if
        // uncompressing fails, should act as if there's no rewriter.
        // (Currently tries to rewrite probably-compressed stream,
        // which is then recompressed by servlet response filter.)
	InputStream uncResp =
	  StreamUtil.getUncompressedInputStreamOrFallback(respStrm,
							  contentEncoding,
							  url);
	if (uncResp != respStrm) {
	  // Uncompressed response is not encoded, and we don't know its length
	  contentEncoding = null;
	  responseContentLength = -1;
	  respStrm = uncResp;
	}
      }
    }
    if (contentEncoding != null) {
      resp.setHeader(HttpFields.__ContentEncoding, contentEncoding);
    }

    // Indicate the AU the content was rewritten for, even though it isn't
    // cached locally
    resp.setHeader(Constants.X_LOCKSS_REWRITTEN_FOR_AUID, au.getAuId());

    String charset = HeaderUtil.getCharsetOrDefaultFromContentType(ctype);
    BufferedInputStream bufRespStrm = new BufferedInputStream(respStrm);
    charset = CharsetUtil.guessCharsetFromStream(bufRespStrm,charset);
    handleRewriteInputStream(bufRespStrm, mimeType, charset,
        responseContentLength);
  }

  // Patterm to extract url query arg from Referer string
  String URL_ARG_REGEXP = "url=([^&]*)";
  Pattern URL_ARG_PAT = Pattern.compile(URL_ARG_REGEXP);


  protected LockssUrlConnection openLockssUrlConnection(LockssUrlConnectionPool
                                                            pool)
      throws IOException {

    boolean isInCache = isInCache();
    String ifModified = null;
    String referer = null;

    String fwdUrl;
    if (cuUrl != null && normalizeForwardedUrl) {
      fwdUrl = cuUrl;
    } else {
      fwdUrl = url;
    }
    LockssUrlConnection conn = UrlUtil.openConnection(fwdUrl, pool);

    // check connection header
    String connectionHdr = req.getHeader(HttpFields.__Connection);
    if (connectionHdr!=null &&
        (connectionHdr.equalsIgnoreCase(HttpFields.__KeepAlive)||
         connectionHdr.equalsIgnoreCase(HttpFields.__Close)))
      connectionHdr=null;

    switch (followPublisherRedirect) {
    case Always:
      conn.setFollowRedirects(true);
      break;
    case Never:
      conn.setFollowRedirects(false);
      break;
    case Missing:
      conn.setFollowRedirects(!isInCache);
      break;
    }

    // copy request headers into new request
    for (Enumeration en = req.getHeaderNames();
         en.hasMoreElements(); ) {
      String hdr=(String)en.nextElement();

      if (connectionHdr!=null && connectionHdr.indexOf(hdr)>=0) continue;

      if (isInCache) {
        if (HttpFields.__IfModifiedSince.equalsIgnoreCase(hdr)) {
          ifModified = req.getHeader(hdr);
          continue;
        }
      }

      if (HttpFields.__Referer.equalsIgnoreCase(hdr)) {
        referer = req.getHeader(hdr);
        continue;
      }

      // Suppress Accept-Encoding: header as it may specify an
      // encoding we don't understand, which would prevent us from
      // rewriting.  In order to fetch compressed content from
      // publisher, must either compute intersection of client's
      // Accept-Encoding and what we understand, or send our normal
      // list and always uncompress.
      if (HttpFields.__AcceptEncoding.equalsIgnoreCase(hdr)) {
        continue;
      }

      // copy request headers to connection
      Enumeration vals = req.getHeaders(hdr);
      while (vals.hasMoreElements()) {
        String val = (String)vals.nextElement();
        if (val!=null) {
          conn.addRequestProperty(hdr, val);
        }
      }
    }

    // If the user sent an if-modified-since header, use it unless the
    // cache file has a later last-modified
    if (isInCache) {
      CIProperties cuprops = cu.getProperties();
      String cuLast = cuprops.getProperty(CachedUrl.PROPERTY_LAST_MODIFIED);
      if (log.isDebug3()) {
        log.debug3("ifModified: " + ifModified);
        log.debug3("cuLast: " + cuLast);
      }
      try {
        ifModified = HeaderUtil.later(ifModified, cuLast);
      } catch (DateParseException e) {
        // preserve user's header if parse failure
        // ignore error, serve file
        log.warning("Handling ifModifiedSince: " + ifModified
                    + "or cuLastModified: " + cuLast + " throws ", e);
      }
    }

    if (ifModified != null) {
      conn.setRequestProperty(HttpFields.__IfModifiedSince, ifModified);
    }

    if (referer != null) {
      referer = getRealReferer(referer);
      log.debug2("Sending referer: " + referer);
      conn.setRequestProperty(HttpFields.__Referer, referer);
    }

    // send address of original requester
    conn.addRequestProperty(HttpFields.__XForwardedFor,
        req.getRemoteAddr());
    conn.addRequestProperty(HttpFields.__Via,
        proxyMgr.makeVia(getMachineName(),
            reqURL.getPort()));

    String cookiePolicy = proxyMgr.getCookiePolicy();
    if (cookiePolicy != null &&
        !cookiePolicy.equalsIgnoreCase(ProxyManager.COOKIE_POLICY_DEFAULT)) {
      conn.setCookiePolicy(cookiePolicy);
    }

    return conn;
  }

  String getRealReferer(String referer) {
    // If the Referer: is a ServeContent URL then the real referring page
    // is in the url query arg.
    if (referer != null) {
      try {
        URI refUri = new URI(referer);
	String refPath = refUri.getPath();
	String servletPath = myServletDescr().getPath();
        if (refPath.endsWith(servletPath)) {
          String rawquery = refUri.getRawQuery();
          if (log.isDebug3()) log.debug3("rawquery: " + rawquery);
          if (!StringUtil.isNullString(rawquery))  {
            Matcher m1 = URL_ARG_PAT.matcher(rawquery);
            if (m1.find()) {
              referer = UrlUtil.decodeUrl(m1.group(1));
            }
          }
        } else if (refPath.startsWith("/" + servletPath + "/")) {
	  referer = refPath.substring(servletPath.length() + 2);
          String rawquery = refUri.getRawQuery();
          if (!StringUtil.isNullString(rawquery))  {
	    referer = referer + "?" + rawquery;
	  }
	}
      } catch (URISyntaxException e) {
        log.siteWarning("Can't perse Referer:, ignoring: " + referer);
      }

    }
    return referer;
  }

  LinkRewriterFactory getLinkRewriterFactory(String mimeType) {
    try {
      if (StringUtil.isNullString(getParameter("norewrite"))) {
        return au.getLinkRewriterFactory(mimeType);
      }
    } catch (Exception e) {
      log.error("Error getting LinkRewriterFactory: " + e);
    }
    return null;
  }

  protected void handleRewriteInputStream(InputStream original,
                                          String mimeType,
                                          String charset,
                                          long length) throws IOException {
    handleRewriteInputStream(getLinkRewriterFactory(mimeType),
        original, mimeType, charset, length);
  }

  protected void handleRewriteInputStream(LinkRewriterFactory lrf,
                                          InputStream original,
                                          String mimeType,
                                          String charset,
                                          long length) throws IOException {
    InputStream rewritten = original;
    OutputStream outStr = null;
    try {
      if (lrf == null || (isMementoRequest() && !rewriteMementoResponses)) {
        // No rewriting, set length and copy
	if (log.isDebug3()) {
	  if (lrf == null) {
	    log.debug2("Not rewriting, no LinkRewriterFactory: " + url);
	  } else {
	    log.debug2("Not rewriting, memento request: " + url);
	  }
	}
        setContentLength(length);
        outStr = resp.getOutputStream();
        StreamUtil.copy(original, outStr);
      } else {
	if (log.isDebug2()) {
	  log.debug2("Rewriting: " + url);
	}
        try {
	  if (baseUrl == null) {
	    baseUrl = url;
	  }
	  if (!baseUrl.equals(url)) {
	    log.debug("Rewriting " + url + " using base URL " + baseUrl);
	  }
          rewritten =
              lrf.createLinkRewriter(mimeType,
                  au,
                  original,
                  charset,
                  baseUrl,
		  makeLinkTransform());
        } catch (PluginException e) {
          log.error("Can't create link rewriter, not rewriting", e);
        }
        // If the rewritten stream knows the charset used to encode it,
        // send that in the response in place of the original file's
        // charset.
        if (rewritten instanceof EncodedThing) {
	  // Note; getCharset() looks at the output stream so will cause
	  // the parser and transform to be invoked here, not where the
	  // stream is read below
          String rewrittenCharset = ((EncodedThing)rewritten).getCharset();
          log.debug3("rewrittenCharset: " + rewrittenCharset);
          if (!StringUtil.isNullString(rewrittenCharset)) {
            resp.setCharacterEncoding(rewrittenCharset);
          }
        }
        if (length >= 0 && length <= maxBufferedRewrite) {
          // if small file rewrite to temp buffer to find length before
          // sending.
          ByteArrayOutputStream baos =
              new ByteArrayOutputStream((int)(length * 1.1 + 100));
          long bytes = StreamUtil.copy(rewritten, baos);
          setContentLength(bytes);
          outStr = resp.getOutputStream();
          baos.writeTo(outStr);
        } else {
          outStr = resp.getOutputStream();
          StreamUtil.copy(rewritten, outStr);
        }
      }
    } finally {
      IOUtil.safeClose(outStr);
      IOUtil.safeClose(original);
      IOUtil.safeClose(rewritten);
    }
  }

  ServletUtil.LinkTransform makeLinkTransform() {
    switch (rewriteStyle) {
    case QueryArg:
    default:
      return new ServletUtil.LinkTransform() {
	public String rewrite(String url) {
	  if (absoluteLinks) {
	    return srvAbsURL(myServletDescr(),
			     "url=" + url);
	  } else {
	    return srvURL(myServletDescr(),
			  "url=" + url);
	  }
	}
      };
    case PathInfo:
      return new ServletUtil.LinkTransform() {
	public String rewrite(String url) {
	  if (absoluteLinks) {
	    return srvAbsURL(myServletDescr()) + "/" + url;
	  } else {
	    return srvURL(myServletDescr()) + "/" + url;
	  }
	}
      };
    }
  }


  private void setContentLength(long length) {
    if (length >= 0) {
      if (length <= Integer.MAX_VALUE) {
        resp.setContentLength((int)length);
      } else {
        resp.setHeader(HttpFields.__ContentLength, Long.toString(length));
      }
    }
  }

  // Ensure we don't redirect if neverProxy is true
  MissingFileAction getMissingFileAction(PubState pstate) {
    switch (missingFileAction) {
      case Redirect:
        if (isNeverProxy() || !pstate.mightHaveContent()) {
          return DEFAULT_MISSING_FILE_ACTION;
        } else {
          return missingFileAction;
        }
      case AlwaysRedirect:
        if (isNeverProxy()) {
          return DEFAULT_MISSING_FILE_ACTION;
        } else {
          return missingFileAction;
        }
      default:
        return missingFileAction;
    }
  }

  // format used for a row label cell (e.g. Publisher:).
  static final private String labelfmt =
      "style=\"font-weight:bold; padding-right:20px;\"";

  // format used for a row
  static final private String rowfmt = "valign=\"top\"";


  /**
   * Handler for missing OpenURL requests displays synthetic TOC
   * for level returned by OpenURL resolver and offers a link to
   * the URL at the publisher site.
   *
   * @param info the OpenUrlInfo from the OpenUrl resolver
   * @param pstate the pub state
   * @throws IOException if an IO error occurs
   */
  protected void handleMissingOpenUrlRequest(OpenUrlInfo info, PubState pstate)
      throws IOException {

    // handle non-cached url according to missingFileAction
    MissingFileAction missingFileAction = getMissingFileAction(pstate);
    switch (missingFileAction) {
      case Redirect:
      case AlwaysRedirect:
        if (url != null) {
          redirectToUrl();
          logAccess("not configured, 302 redirect to pub");
          return;
        }
        // fall through if au is down
      case Error_404:
      case HostAuIndex:
      case AuIndex:
      default:
        // fall through to offer link to publisher url
        break;
    }

    // build block with message specific to resolved-to level
    Block block = new Block(Block.Center);
    ResolvedTo resolvedTo = info.getResolvedTo();
    BibliographicItem bibliographicItem = info.getBibliographicItem();
    String proxySpec = info.getProxySpec();
    String proxyMsg = StringUtil.isNullString(proxySpec) ? "."
                                                         : " by proxying through '" + proxySpec + "'.";

    Table table = new Table(0, "");

    switch (resolvedTo) {
      case PUBLISHER:

        // display publisher page
        if (bibliographicItem == null) {
          block.add("<h2>Found requested publisher</h2>");
        } else {
          block.add("<h2>");
          block.add("Publisher: '");
          block.add(bibliographicItem.getPublisherName());
          block.add("'");
          block.add("</h2>");
        }

        addLink(table,
            "Additional publisher information available at the publisher.",
            "Selecting link takes you away from this LOCKSS box.");

        logAccess("404 to publisher page");
        break;

      case TITLE:
        // display title page
        if (bibliographicItem == null) {
          block.add("<h2>Found requested title</h2>");
        } else {
          block.add("<h2>");
          block.add("Publication: '");
          block.add(bibliographicItem.getPublicationTitle());
          block.add("'");
          block.add("</h2>");

          addPublisher(table, bibliographicItem);
          addIsbnOrIssn(table, bibliographicItem);
        }

        addLink(table,
            "Additional title information available at the publisher" + proxyMsg,
            "Selecting link takes you away from this LOCKSS box.");

        logAccess("404 title page");
        break;

      case VOLUME:
        // display volume page
        if (bibliographicItem == null) {
          block.add("<h2>Found requested volume</h2>");
        } else {
          block.add("<h2>");
          block.add("Volume: '");
          block.add(bibliographicItem.getName());
          block.add("'");
          block.add("</h2>");

          addPublisher(table, bibliographicItem);
          addIsbnOrIssn(table, bibliographicItem);
          addVolumeAndYear(table, bibliographicItem);
        }

        addLink(table,
            "Volume is available at the publisher" + proxyMsg,
            "Selecting link takes you away from this LOCKSS box.");


        // display volume page
        logAccess("404 volume page");
        break;

      case ISSUE:
        // display issue page
        if (bibliographicItem == null) {
          block.add("<h2>Found requested issue</h2>");
        } else {
          block.add("<h2>");
          block.add("Issue: '");
          block.add(bibliographicItem.getName());
          block.add("'");
          block.add("</h2>");

          addPublisher(table, bibliographicItem);
          addIsbnOrIssn(table, bibliographicItem);
          addVolumeAndYear(table, bibliographicItem);

          String issue = bibliographicItem.getIssue();
          if (issue != null) {
            table.newRow(rowfmt);
            table.newCell(labelfmt);
            table.add("<b>Issue:</b>");
            table.newCell();
            table.add(issue);
          }
        }

        addLink(table,
            "Issue is available at the publisher" + proxyMsg,
            "Selecting link takes you away from this LOCKSS box.");

        logAccess("404 issue page");
        break;

      case CHAPTER:
        // display chapter page
        if (bibliographicItem == null) {
          block.add("<h2>Found requested chapter</h2>");
        } else {
          block.add("<h2>");
          block.add("Chapter: '");
          block.add(bibliographicItem.getName());
          block.add("'");
          block.add("</h2>");

          addPublisher(table, bibliographicItem);
          addIsbnOrIssn(table, bibliographicItem);
          addVolumeAndYear(table, bibliographicItem);

          String chapter = bibliographicItem.getIssue();
          if (chapter != null) {
            table.newRow(rowfmt);
            table.newCell(labelfmt);
            table.add("Chapter:");
            table.newCell();
            table.add(chapter);
          }
        }

        addLink(table,
            "Chapter is available at the publisher" + proxyMsg,
            "Selecting link takes you away from this LOCKSS box.");


        logAccess("404 chapter page");
        break;

      case ARTICLE:
        // display article page
        if (bibliographicItem == null) {
          block.add("<h2>Found requested article</h2>");
        } else {
          block.add("<h2>");
          block.add("Article: '");
          block.add(bibliographicItem.getName());
          block.add("'");
          block.add("</h2>");

          addPublisher(table, bibliographicItem);
          addIsbnOrIssn(table, bibliographicItem);
          addVolumeAndYear(table, bibliographicItem);

          String issue = bibliographicItem.getIssue();
          if (issue != null) {
            table.newRow(rowfmt);
            table.newCell(labelfmt);
            table.add("Issue:");
            table.newCell();
            table.add(issue);
          }
        }

        addLink(table,
            "Article is available at the publisher" + proxyMsg,
            "Selecting link takes you away from this LOCKSS box.");

        logAccess("404 article page");
        break;

      default:
        // display other page for ResolvedTo.OTHER
        block.add("<h2>Found requested item</h2>");

        addLink(table,
            "Item is available at the publisher" + proxyMsg,
            "Selecting link takes you away from this LOCKSS box.");


        logAccess("404 other page");
        break;
    }

    block.add(table) ;
    block.add("<br/><br/>");

    switch (missingFileAction) {
      case AuIndex:
        displayIndexPage(pluginMgr.getAllAus(),
            HttpResponse.__404_Not_Found,
            block,
            "The LOCKSS box has the following Archival Units");
        logAccess("not present, 404 with index");
        break;
      case Redirect:
      case AlwaysRedirect:
      case Error_404:
      case HostAuIndex:
      default:
        Collection<ArchivalUnit> candidateAus = Collections.emptyList();
        if (bibliographicItem != null) {
          candidateAus = getCandidateAus(bibliographicItem);
        } else if (url != null) {
          try {
            candidateAus = pluginMgr.getCandidateAus(url);
          } catch (MalformedURLException ex) {
            // ignore error, serve file
            log.warning("Handling URL: " + url + " throws ", ex);
          }
        }

        if (candidateAus != null && !candidateAus.isEmpty()) {
          displayIndexPage(candidateAus,
              HttpResponse.__404_Not_Found,
              block,
              candidates404Msg);
        } else {
          displayIndexPage(Collections.<ArchivalUnit> emptyList(),
              HttpResponse.__404_Not_Found,
              block,
              null);
          logAccess("not present, 404");
        }
        break;
    }
  }

  /**
   * Add publisher to the table.
   *
   * @param table the table
   * @param bibliographicItem the bibliographic item
   */
  private void addPublisher(
      Table table, BibliographicItem bibliographicItem) {
    table.newRow(rowfmt);
    table.newCell(labelfmt);
    table.add("Publisher:");
    table.newCell();
    table.add(bibliographicItem.getPublisherName());
  }

  /**
   * Add isbn or issn information to the table.
   *
   * @param table the Table
   * @param bibliographicItem the bibliographic item
   */
  private void addIsbnOrIssn(
      Table table, BibliographicItem bibliographicItem) {
    String printIsbn = bibliographicItem.getIsbn();
    String eIsbn = bibliographicItem.getEisbn();
    String printIssn = bibliographicItem.getIssn();
    String eIssn = bibliographicItem.getEissn();
    if (printIsbn != null || eIsbn != null) {
      if (printIsbn != null) {
        table.newRow(rowfmt);
        table.newCell(labelfmt);
        table.add("ISBN:");
        table.newCell();
        table.add(printIsbn);
      }
      if (eIsbn != null) {
        table.newRow(rowfmt);
        table.newCell(labelfmt);
        table.add("eISBN:");
        table.newCell();
        table.add(eIsbn);
      }
    } else if (printIssn != null || eIssn != null) {
      if (printIssn != null) {
        table.newRow(rowfmt);
        table.newCell(labelfmt);
        table.add("ISSN:");
        table.newCell();
        table.add(printIssn);
      }
      if (eIssn != null) {
        table.newRow(rowfmt);
        table.newCell(labelfmt);
        table.add("eISSN:");
        table.newCell();
        table.add(eIssn);
      }
    }
  }

  /**
   * Add volume and year information to the table.
   *
   * @param table the Table
   * @param bibliographicItem the bibliographic item
   */
  private void addVolumeAndYear(
      Table table, BibliographicItem bibliographicItem) {
    String volume = bibliographicItem.getVolume();
    String year = bibliographicItem.getYear();

    if (volume != null) {
      table.newRow(rowfmt);
      table.newCell(labelfmt);
      table.add("Volume:");
      table.newCell();
      table.add(volume);
    }
    if (year != null) {
      table.newRow(rowfmt);
      table.newCell(labelfmt);
      table.add("Year:");
      table.newCell();
      table.add(year);
    }
  }

  /**
   * Add link to publisher url and additional info
   * to the table if the AU for the URL is not down.
   *
   * @param table the Table
   * @param additionalInfo additional info to appear in the footnote
   */
  private void addLink(
      Table table, String... additionalInfo ) {
    if (url != null) {
      table.newRow(rowfmt);
      table.newCell(labelfmt);
      table.add("URL:");
      if (additionalInfo != null) {
        for (String info : additionalInfo) {
          table.add(addFootnote(info));
        }
      }
      table.newCell();
      Link link = new Link(url,url);
      table.add(link);
    }
  }

  /**
   * Get candidate AUs that match the specified bibliographic item's
   * issn, isbn, or publisher and title fields
   *
   * @param bibliographicItem the bibliographic item
   * @return a collection of candidate AUs
   */
  protected Collection<ArchivalUnit> getCandidateAus(
      BibliographicItem bibliographicItem) {
    Collection<ArchivalUnit> candidateAus =
      new TreeSet<ArchivalUnit>(new AuOrderComparator());

    Tdb tdb = ConfigManager.getCurrentConfig().getTdb();
    String isbn = bibliographicItem.getIsbn();
    String issn = bibliographicItem.getIssn();
    Collection<TdbAu> tdbaus = Collections.emptySet();

    if (isbn != null) {
      // get TdbAus for books by thieir ISBN
      tdbaus = tdb.getTdbAusByIsbn(isbn);

    } else if (issn != null) {
      // get TdbAus for journals by thieir ISBN
      TdbTitle tdbtitle = tdb.getTdbTitleByIssn(issn);
      if (tdbtitle != null) {
        tdbaus = tdbtitle.getTdbAus();
      }

    } else {
      // get TdbAus by publisher and/or title
      String title = bibliographicItem.getPublicationTitle();
      String publisher = bibliographicItem.getPublisherName();
      TdbPublisher tdbpublisher = tdb.getTdbPublisher(publisher);
      if (title != null) {
        // get AUs for specified title
        Collection<TdbTitle> tdbtitles;
        if (tdbpublisher != null) {
          tdbtitles = tdbpublisher.getTdbTitlesByName(title);
        } else {
          tdbtitles = tdb.getTdbTitlesByName(title);
        }
        tdbaus = new ArrayList<TdbAu>();
        for (TdbTitle tdbtitle : tdbtitles) {
          tdbaus.addAll(tdbtitle.getTdbAus());
        }
      } else if (tdbpublisher != null) {
        // get AUs for specified publisher
        Iterator<TdbAu> tdbAuItr = tdbpublisher.tdbAuIterator();
        tdbaus = new ArrayList<TdbAu>();
        while (tdbAuItr.hasNext()) {
          TdbAu tdbau = tdbAuItr.next();
          tdbaus.add(tdbau);
        }
      }
    }

    // get preserved AUs corresponding to TdbAus found
    for (TdbAu tdbau : tdbaus) {
      ArchivalUnit au = TdbUtil.getAu(tdbau);
      if (au != null && TdbUtil.isAuPreserved(au)) {
        candidateAus.add(au);
      }
    }
    return candidateAus;
  }

  protected void handleMissingUrlRequest(String missingUrl, PubState pstate)
      throws IOException {
    String missing =
        missingUrl + ((au != null) ? " in AU: " + au.getName() : "");

    Block block = new Block(Block.Center);
    // display publisher page
    block.add("<p>The requested URL is not preserved on this LOCKSS box. ");
    block.add("Select link");
    block.add(addFootnote(
        "Selecting publisher link takes you away from this LOCKSS box."));
    block.add(" to view it at the publisher:</p>");
    block.add("<a href=\"" + missingUrl + "\">" + missingUrl + "</a><br/><br/>");

    switch (getMissingFileAction(pstate)) {
      case Error_404:
        resp.sendError(HttpServletResponse.SC_NOT_FOUND,
            missing + " is not preserved on this LOCKSS box");
        logAccess("not present, 404");
        break;
      case Redirect:
      case AlwaysRedirect:
        redirectToUrl();
        break;
      case HostAuIndex:
        Collection<ArchivalUnit> candidateAus = Collections.emptyList();
        try {
            candidateAus = pluginMgr.getCandidateAus(missingUrl);
        } catch (MalformedURLException ex) {
          // ignore error, serve file
          log.warning("Handling URL: " + url + " throws ", ex);
        }
        if (candidateAus != null && !candidateAus.isEmpty()) {
          displayIndexPage(candidateAus,
              HttpResponse.__404_Not_Found,
              block,
              candidates404Msg);
          logAccess("not present, 404 with index");
        } else {
          displayIndexPage(Collections.<ArchivalUnit>emptyList(),
              HttpResponse.__404_Not_Found,
              block,
              null);
          logAccess("not present, 404");
        }
        break;
      case AuIndex:
        displayIndexPage(pluginMgr.getAllAus(),
            HttpResponse.__404_Not_Found,
            block,
            null);
        logAccess("not present, 404 with index");
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
    endPage(page);
  }

  void displayIndexPage() throws IOException {
    displayIndexPage(pluginMgr.getAllAus(), -1, (Element)null, (String)null);
  }

  void displayIndexPage(Collection<ArchivalUnit> auList,
                        int result,
                        String headerText)
      throws IOException {
    displayIndexPage(auList, result, (Element)null, headerText);
  }

  void displayIndexPage(Collection<ArchivalUnit> auList,
                        int result,
                        Element headerElement,
                        String headerText)
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

    if (headerElement != null) {
      page.add(headerElement);
    }

    Block centeredBlock = new Block(Block.Center);

    if (areAllExcluded(auList, pred) && !offerUnfilteredList) {
      ServletUtil.layoutExplanationBlock(centeredBlock,
          "No matching content has been preserved on this LOCKSS box");
    } else {
      // Layout manifest index w/ URLs pointing to this servlet
      Element ele =
          ServletUtil.manifestIndex(pluginMgr,
              auList,
              pred,
              headerText,
              new ServletUtil.ManifestUrlTransform() {
                public Object transformUrl(String url,
                                           ArchivalUnit au){
                  Properties query =
                      PropUtil.fromArgs("url", url);
                  if (au != null) {
                    query.put("auid", au.getAuId());
                  }
                  return srvLink(myServletDescr(),
                      url, query);
                }},
              true);
      centeredBlock.add(ele);
      if (offerUnfilteredList) {
        centeredBlock.add("<br>");
        centeredBlock.add("Other possibly relevant content has not yet been "
                          + "certified for use with ServeContent and may not "
                          + "display correctly.  Click ");
        Properties args = getParamsAsProps();
        args.put("filterPlugins", "no");
        centeredBlock.add(srvLink(myServletDescr(), "here", args));
        centeredBlock.add(" to see the complete list.");
      }
    }
    page.add(centeredBlock);
    if (result > 0) {
      resp.setStatus(result);
    }
    endPage(page);
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

  static enum PubState {
    KnownDown,
    RecentlyDown,
    NoContent,
    Unknown() {
      public boolean mightHaveContent() {
        return true;
      }
    };
    public boolean mightHaveContent() {
      return false;
    }
  }
}
