/*
 * $Id: ServeContent.java,v 1.32 2011-05-26 07:55:00 tlipkis Exp $
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

package org.lockss.servlet;

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.collections.*;
import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.lang.StringEscapeUtils;
import org.mortbay.http.*;
import org.mortbay.html.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.LockssUrlConnection;
import org.lockss.util.urlconn.LockssUrlConnectionPool;
import org.lockss.app.LockssDaemon;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.proxy.ProxyManager;
import org.lockss.state.*;
import org.lockss.rewriter.*;

/** ServeContent servlet displays cached content with links
 *  rewritten.
 */
@SuppressWarnings("serial")
public class ServeContent extends LockssServlet {
  static final Logger log = Logger.getLogger("ServeContent");

  /** Prefix for this server's config tree */
  public static final String PREFIX = Configuration.PREFIX + "serveContent.";

  /** Determines actions when a URL is requested that is not in the cache.
   * One of <code>Error_404</code>, <code>HostAuIndex</code>,
   * <code>AuIndex</code>, <code>Redirect</code>. */
  public static final String PARAM_MISSING_FILE_ACTION =
    PREFIX + "missingFileAction";
  public static final MissingFileAction DEFAULT_MISSING_FILE_ACTION =
    MissingFileAction.HostAuIndex;;

  public static enum MissingFileAction {
    Error_404,
      HostAuIndex,
      AuIndex,
      Redirect,
      AlwaysRedirect,
      }
  
  /** If true, rewritten links will be absolute
   * (http://host:port/ServeContent?url=...).  If false, relative
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

  /** If true, never forward request to publisher */
  public static final String PARAM_NEVER_PROXY = PREFIX + "neverProxy";
  public static final boolean DEFAULT_NEVER_PROXY = false;

  private static MissingFileAction missingFileAction =
    DEFAULT_MISSING_FILE_ACTION;
  private static boolean absoluteLinks = DEFAULT_ABSOLUTE_LINKS;
  private static boolean normalizeUrl = DEFAULT_NORMALIZE_URL_ARG;
  private static List<String> excludePlugins = DEFAULT_EXCLUDE_PLUGINS;
  private static List<String> includePlugins = DEFAULT_INCLUDE_PLUGINS;
  private static boolean includeInternalAus = DEFAULT_INCLUDE_INTERNAL_AUS;
  private static int maxBufferedRewrite = DEFAULT_MAX_BUFFERED_REWRITE;
  private static boolean neverProxy = DEFAULT_NEVER_PROXY;


  private ArchivalUnit au;
  private String url;
  private CachedUrl cu;
  private boolean enabledPluginsOnly;

  private PluginManager pluginMgr;
  private ProxyManager proxyMgr;
  private OpenUrlResolver openUrlResolver;

  // don't hold onto objects after request finished
  protected void resetLocals() {
    cu = null;
    url = null;
    au = null;
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
      missingFileAction =
	(MissingFileAction)config.getEnum(MissingFileAction.class,
					  PARAM_MISSING_FILE_ACTION,
					  DEFAULT_MISSING_FILE_ACTION);
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
      normalizeUrl = config.getBoolean(PARAM_NORMALIZE_URL_ARG,
					DEFAULT_NORMALIZE_URL_ARG);
      neverProxy = config.getBoolean(PARAM_NEVER_PROXY,
				     DEFAULT_NEVER_PROXY);
      maxBufferedRewrite = config.getInt(PARAM_MAX_BUFFERED_REWRITE,
					 DEFAULT_MAX_BUFFERED_REWRITE);
    }
  }

  protected boolean isInCache() {
    return (cu != null) && cu.hasContent();
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
      !"no".equalsIgnoreCase(getParameter("filterPlugins"));

    url = getParameter("url");
    String auid = getParameter("auid");
    if (!StringUtil.isNullString(url)) {
      if (!StringUtil.isNullString(auid)) {
	au = pluginMgr.getAuFromId(auid);
      }
      if (log.isDebug3()) log.debug3("Url req, raw: " + url);
      // handle html-encoded URLs with characters like &amp;
      // that can appear as links embedded in HTML pages
      url = StringEscapeUtils.unescapeHtml(url);
      
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
	  log.debug2(url + " normalized to " + normUrl);
	  url = normUrl;
	}
      }
      handleUrlRequest();
      return;
    }
    
    // perform special handling for an OpenUrl
    try {
      String doi = getParameter("doi");
      if (!StringUtil.isNullString(doi)) {
        // transform convenience representation of doi to OpenURL form
        // (ignore other parameters)
        if (log.isDebug3()) log.debug3("Resolving DOI: " + doi);
        url = openUrlResolver.resolveFromDOI(doi);
      } else {
	// pass all params to Open Url resolver
	Map<String,String> pmap = getParamsAsMap();
        if (log.isDebug3()) log.debug3("Resolving OpenUrl: " + pmap);
        url = openUrlResolver.resolveOpenUrl(pmap);
      }
      if (!StringUtil.isNullString(url)) {
        log.debug2("Resolved OpenUrl to: " + url);
        handleUrlRequest();
        return;
      }
      log.debug3("Request is not an OpenUrl");
    } catch (RuntimeException ex) {
      log.warning("Couldn't handle OpenUrl", ex);
    }
    // Maybe should display a message here if URL is unknown format.  But
    // this is also the default case for the bare ServeContent URL, which
    // should generate an index with no message.
    displayIndexPage();
  }

  /**
   * Handle request for specified publisher URL.  If content
   * is in cache, use its AU and CU in case content is not
   * available from the publisher.  Otherwise, redirect to the
   * publisher URL without rewriting the content.
   * 
   * @throws IOException if cannot handle URL request.
   */
  protected void handleUrlRequest() throws IOException {
    log.debug2("url: " + url);
    try {
      // Get the CachedUrl for the URL, only if it has content.
      if (au != null) {
	cu = au.makeCachedUrl(url);
      } else {
	// Find CU if belongs to any configured AU even if has no content,
	// so can rewrite from publisher
	cu = pluginMgr.findCachedUrl(url, false);
	if (cu != null) {
	  if (log.isDebug3()) log.debug3("cu: " + cu);
	  au = cu.getArchivalUnit();
	}
      }
      if (au != null) {
        handleAuRequest();
      } else if (!isNeverProxy()) {
        log.debug2("Content not cached: redirecting to " + url);
        resp.sendRedirect(url);
      } else {
	handleMissingUrlRequest(url, PubState.Unknown);
      }
    } catch (IOException e) {
      log.warning("Handling " + url + " throws ", e);
      throw e;
    } finally {
      AuUtil.safeRelease(cu);
    }
  }

  // Patterm to extract url query arg from Referer string
  String URL_ARG_REGEXP = "url=([^&]*)";
  Pattern URL_ARG_PAT = Pattern.compile(URL_ARG_REGEXP);


  protected LockssUrlConnection openLockssUrlConnection(LockssUrlConnectionPool pool)
    throws IOException {

    boolean isInCache = isInCache();
    String ifModified = null;
    String referer = null;
    
    LockssUrlConnection conn = UrlUtil.openConnection(url, pool);

    // check connection header
    String connectionHdr = req.getHeader(HttpFields.__Connection);
    if (connectionHdr!=null &&
        (connectionHdr.equalsIgnoreCase(HttpFields.__KeepAlive)||
         connectionHdr.equalsIgnoreCase(HttpFields.__Close)))
      connectionHdr=null;

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

      // XXX Conceivably should suppress Accept-Encoding: header if it
      // specifies an encoding we don't understand, as that would prevent
      // us from rewriting.

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
      }
    }

    if (ifModified != null) {
      conn.setRequestProperty(HttpFields.__IfModifiedSince, ifModified);
    }

    // If the Referer: is a ServeContent URL then the real referring page
    // is in the url query arg.
    if (referer != null) {
      try {
	URI refUri = new URI(referer);
	if (refUri.getPath().endsWith(myServletDescr().getPath())) {
	  String rawquery = refUri.getRawQuery();
	  if (log.isDebug3()) log.debug3("rawquery: " + rawquery);
	  if (!StringUtil.isNullString(rawquery))  {
	    Matcher m1 = URL_ARG_PAT.matcher(rawquery);
	    if (m1.find()) {
	      referer = UrlUtil.decodeUrl(m1.group(1));
	    }
	  }
	}
      } catch (URISyntaxException e) {
	log.siteWarning("Can't perse Referer:, ignoring: " + referer);
      }

      log.debug2("Sending referer: " + referer);
      conn.setRequestProperty(HttpFields.__Referer, referer);
    }
    // send address of original requester
    conn.addRequestProperty(HttpFields.__XForwardedFor,
                            req.getRemoteAddr());

    String cookiePolicy = proxyMgr.getCookiePolicy();
    if (cookiePolicy != null &&
	!cookiePolicy.equalsIgnoreCase(ProxyManager.COOKIE_POLICY_DEFAULT)) {
      conn.setCookiePolicy(cookiePolicy);
    }
    
    return conn;
  }
  
  /**
   * Handle request for content that belongs to one of our AUs, whether or
   * not we have content for that URL.  Serve content either from publisher
   * (if it's up and has newer content than ours) or from cache (if
   * have content).
   * 
   * @throws IOException for IO errors
   */
  protected void handleAuRequest() throws IOException {
    String host = UrlUtil.getHost(url);
    boolean isInCache = isInCache();
    boolean isHostDown = proxyMgr.isHostDown(host);

    if (isNeverProxy()) {
      if (isInCache) {
	serveFromCache();
      } else {
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
      conn.execute();
    } catch (IOException ex) {
      if (log.isDebug3()) log.debug3("conn.execute", ex);

      // mark host down if connection timed out
      if (ex instanceof LockssUrlConnection.ConnectionTimeoutException) {
        proxyMgr.setHostDown(host, isInCache);
      }
      pstate = PubState.KnownDown;

      // tear down connection
      safeClose(conn);
      conn = null;
    }
    
    try {
      if (conn != null) {
        int response = conn.getResponseCode();
        if (log.isDebug2())
	  log.debug2("response: " + response + " " + conn.getResponseMessage());
        if (response == HttpResponse.__200_OK) {
          // If publisher responds with content, serve it to user
	  // XXX Possibly should check for a login page here
          serveFromPublisher(conn);
          return;
        } else {
	  pstate = PubState.NoContent;
	}
      }
    } finally {
      // ensure connection is closed
      safeClose(conn);
    }
    
    // Either failed to open connection or got non-200 response.
    if (isInCache) {
      serveFromCache();
    } else {
      log.debug2("No content for: " + url);
      // return 404 with index
      handleMissingUrlRequest(url, pstate);
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
          ctype = props.getProperty(CachedUrl.PROPERTY_CONTENT_TYPE);
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
      }
    }

    ctype = props.getProperty(CachedUrl.PROPERTY_CONTENT_TYPE);
    String mimeType = HeaderUtil.getMimeTypeFromContentType(ctype);
    String charset = HeaderUtil.getCharsetFromContentType(ctype);
    if (log.isDebug3()) {
      log.debug3( "Serving cached content for: " + url
		  + " mime type=" + mimeType
		  + " size=" + cu.getContentSize()
		  + " cu=" + cu);
    }
    resp.setContentType(ctype);
    if (cuLastModified != null) {
      resp.setHeader(HttpFields.__LastModified, cuLastModified);
    }
    
    AuState aus = AuUtil.getAuState(au);
    if (!aus.isOpenAccess()) {
      resp.setHeader(HttpFields.__CacheControl, "private");
    }   
    
    // Add a header to the response to identify content from LOCKSS cache
    resp.setHeader(Constants.X_LOCKSS, Constants.X_LOCKSS_FROM_CACHE);

    // rewrite content from cache
    handleRewriteInputStream(cu.getUnfilteredInputStream(),
			     mimeType, charset, cu.getContentSize());
  }
  
  /**
   * Serve content from publisher.
   * 
   * @param conn the connection
   * @throws IOException if cannot read content
   */
  protected void serveFromPublisher(LockssUrlConnection conn) throws IOException {
    String ctype = conn.getResponseContentType();
    String mimeType = HeaderUtil.getMimeTypeFromContentType(ctype);
    log.debug2(  "Serving publisher content for: " + url
               + " mime type=" + mimeType + " size=" + conn.getResponseContentLength());
    

    // copy connection response headers to servlet response
    int h = 0;
    String hdr = conn.getResponseHeaderFieldKey(h);
    String val = conn.getResponseHeaderFieldVal(h);
    while ((hdr != null) || (val != null)) {

      if (  (hdr!=null) && (val!=null) 
	    // Don't copy the following headers:
	    // Content-Encoding conditionally copied below
         && !HttpFields.__ContentEncoding.equalsIgnoreCase(hdr)
         && !HttpFields.__KeepAlive.equalsIgnoreCase(hdr)
         && !HttpFields.__Connection.equalsIgnoreCase(hdr)) {
        resp.addHeader(hdr, val);
      }
      h++;
      hdr=conn.getResponseHeaderFieldKey(h);
      val=conn.getResponseHeaderFieldVal(h);
    }
    
    // get input stream and encoding
    InputStream respStrm = conn.getResponseInputStream();
    String charset = HeaderUtil.getCharsetFromContentType(ctype);
    String contentEncoding = conn.getResponseContentEncoding();

    LinkRewriterFactory lrf = getLinkRewriterFactory(mimeType);
    if (lrf != null) {
      // we're going to rewrite, must deal with content encoding.
      if (contentEncoding != null) {
	if (log.isDebug2())
	  log.debug2("Wrapping Content-Encoding: " + contentEncoding);
	try {
	  respStrm =
	    StreamUtil.getUncompressedInputStream(respStrm, contentEncoding);
	  // Rewritten response is not encoded
	  contentEncoding = null;
	} catch (UnsupportedEncodingException e) {
	  log.warning("Unsupported Content-Encoding: " + contentEncoding +
		      ", not rewriting " + url);
	  lrf = null;
	}

      }
    }
    if (contentEncoding != null) {
      resp.setHeader(HttpFields.__ContentEncoding, contentEncoding);
    }

    handleRewriteInputStream(respStrm, mimeType, charset,
			     conn.getResponseContentLength());
  }

  protected void safeClose(LockssUrlConnection conn) {
    // close connection once done
    if (conn != null) {
      try {
        conn.release();
      } catch (Exception e) {}
    }
    
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
      if (lrf == null) {
	// No rewriting, set length and copy
	setContentLength(length);
	outStr = resp.getOutputStream();
	StreamUtil.copy(original, outStr);
      } else {
        try {
          rewritten =
            lrf.createLinkRewriter(mimeType,
                                   au,
                                   original,
                                   charset,
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
          log.error("Can't create link rewriter, not rewriting", e);
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

  protected void handleMissingUrlRequest(String missingUrl, PubState pstate)
      throws IOException {
    String missing =
      missingUrl + ((au != null) ? " in AU: " + au.getName() : "");
    switch (getMissingFileAction(pstate)) {
    case Error_404:
      resp.sendError(HttpServletResponse.SC_NOT_FOUND,
		     missing + " is not preserved on this LOCKSS box");
      break;
    case Redirect:
    case AlwaysRedirect:
      resp.sendRedirect(url);
      break;
    case HostAuIndex:
      Collection candidateAus = pluginMgr.getCandidateAus(missingUrl);
      if (candidateAus != null && !candidateAus.isEmpty()) {
	displayIndexPage(candidateAus,
			 HttpResponse.__404_Not_Found,
			 "Requested URL ( " + missing
			 + " ) is not preserved on this LOCKSS box.  "
			 + "Possibly related content may be found "
			 + "in the following Archival Units");
      } else {
	resp.sendError(HttpResponse.__404_Not_Found,
		       missing + " is not preserved on this LOCKSS box");
      }
      break;
    case AuIndex:
      displayIndexPage(pluginMgr.getAllAus(),
		       HttpResponse.__404_Not_Found,
		       "Requested URL ( " + missing
		       + " ) is not preserved on this LOCKSS box.");
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
