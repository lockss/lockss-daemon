/*
 * $Id: ServeContent.java,v 1.26 2011-01-25 00:50:11 pgust Exp $
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
  
  /** Timeout parameter for connecting to publisher */
  public static final String PARAM_PUBLISHER_TIMEOUT = PREFIX + "publisherConnectionTimeout";
  
  /** Default timeout value for connecting to publisher (milliseconds) */
  public static final int DEFAULT_PUBLISHER_TIMEOUT = 500; // milliseconds


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

  public static final List<String> DEFAULT_INCLUDE_PLUGINS = Collections.emptyList();

  /** Exclude from index AUs in listed plugins.  Set only one of
   * PARAM_INCLUDE_PLUGINS or PARAM_EXCLUDE_PLUGINS */
  public static final String PARAM_EXCLUDE_PLUGINS =
    PREFIX + "excludePlugins";

  public static final List<String> DEFAULT_EXCLUDE_PLUGINS = Collections.emptyList();

  /** If true, Include internal AUs (plugin registries) in index */
  public static final String PARAM_INCLUDE_INTERNAL_AUS =
    PREFIX + "includeInternalAus";

  public static final boolean DEFAULT_INCLUDE_INTERNAL_AUS = false;

  private static MissingFileAction missingFileAction =
    DEFAULT_MISSING_FILE_ACTION;
  private static boolean absoluteLinks = DEFAULT_ABSOLUTE_LINKS;
  private static boolean normalizeUrl = DEFAULT_NORMALIZE_URL_ARG;
  private static List<String> excludePlugins = DEFAULT_EXCLUDE_PLUGINS;
  private static List<String> includePlugins = DEFAULT_INCLUDE_PLUGINS;
  private static boolean includeInternalAus = DEFAULT_INCLUDE_INTERNAL_AUS;

  private String verbose;
  private ArchivalUnit au;
  private String url;
  private String ctype;
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
    ctype = null;
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
//PJG  
//boolean fromOpenUrl = false;

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
//PJG
//fromOpenUrl = false;

    verbose = getParameter("verbose");
    url = getParameter("url");
    String auid = getParameter("auid");
    if (!StringUtil.isNullString(url)) {
      if (!StringUtil.isNullString(auid)) {
	au = pluginMgr.getAuFromId(auid);
      }
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
	  log.debug(url + " normalized to " + normUrl);
	  url = normUrl;
	}
      }
      handleUrlRequest();
      return;
    }
    
//PJG
//fromOpenUrl = true;

    // perform special handling for an OpenUrl
    try {
      // copy request parameters to parameter map
      Map<String,String> params = new HashMap<String,String>();
      if (req.getParameter("doi") != null) {
        // transform convenience representation of doi to OpenURL form
        // (ignore other parameters)
        url = openUrlResolver.resolveFromDOI(req.getParameter("doi"));
      } else {
        // create parameter map for OpenUrl resolver
        @SuppressWarnings("unchecked")
        Iterator<Map.Entry<String,String[]>> itr = req.getParameterMap().entrySet().iterator();
        while (itr.hasNext()) {
          Map.Entry<String,String[]> entry = itr.next();
          String key = entry.getKey();
          String[] values = entry.getValue();
          if ((values != null) && (values.length >= 1)) {
            params.put(key, values[0]);
          }
        }
        url = openUrlResolver.resolveOpenUrl(params);
      }
      if (!StringUtil.isNullString(url)) {
        log.debug("Resolved OpenUrl to: " + url);
        handleUrlRequest();
        return;
      }
      log.debug("Request is not an OpenUrl");
    } catch (Throwable ex) {
      log.warning("Couldn't handle OpenUrl", ex);
    }
    
    displayIndexPage();
  }

  /**
   * Handle request for specified publisher URL.  If content
   * is in cache, use it's AU and CU in case content is not
   * available from the publisher.  Otherwise, redirect to the
   * publisher URL without rewriting the content.
   * 
   * @throws IOException if cannot handle URL request.
   */
  protected void handleUrlRequest() throws IOException {
    log.debug("url " + url);
    try {
      // Get the CachedUrl for the URL, only if it has content.
      if (au != null) {
	cu = au.makeCachedUrl(url);
      } else {
	cu = pluginMgr.findCachedUrl(url, true);
	if (cu != null) {
	  au = cu.getArchivalUnit();
	  if (!cu.hasContent()) {
	    cu.release();
	    cu = null;
	  }
	}
      }
//PJG
//if (au != null || fromOpenUrl) {
      if (au != null) {
        handleAuRequest();
      } else {
        log.debug("Content not cached: redirecting to " + url);
        resp.sendRedirect(url);
      }
    } catch (IOException e) {
      log.warning("Handling " + url + " throws ", e);
      throw e;
    } finally {
      if (cu != null) {
	cu.release();
      }
    }
  }

  /** 
   * Connection pool used by {@link #handleAuRequest()} for
   *  quick connection to publisher content.
   */
  private LockssUrlConnectionPool quickConnPool = null;
  
  /** 
   * Connection pool used by {@link #handleAuRequest()} for
   *  normal connection to publisher content.
   */
  private LockssUrlConnectionPool connPool = null;


  /**
   * Ensure that the connection pool for handling AU requests is initialized.
   */
  protected void ensureConnectionPool() {
    if (quickConnPool == null) {
      LockssUrlConnectionPool connPool = new LockssUrlConnectionPool();
      LockssUrlConnectionPool quickConnPool = new LockssUrlConnectionPool();
      Configuration conf = ConfigManager.getCurrentConfig();

      int tot = conf.getInt(ProxyManager.PARAM_PROXY_MAX_TOTAL_CONN,
                            ProxyManager.DEFAULT_PROXY_MAX_TOTAL_CONN);
      int perHost = conf.getInt(ProxyManager.PARAM_PROXY_MAX_CONN_PER_HOST,
                                ProxyManager.DEFAULT_PROXY_MAX_CONN_PER_HOST);

      connPool.setMultiThreaded(tot, perHost);
      quickConnPool.setMultiThreaded(tot, perHost);
      connPool.setConnectTimeout
        (conf.getTimeInterval(ProxyManager.PARAM_PROXY_CONNECT_TIMEOUT,
                              ProxyManager.DEFAULT_PROXY_CONNECT_TIMEOUT));
      connPool.setDataTimeout
        (conf.getTimeInterval(ProxyManager.PARAM_PROXY_DATA_TIMEOUT,
                              ProxyManager.DEFAULT_PROXY_DATA_TIMEOUT));
      quickConnPool.setConnectTimeout
        (conf.getTimeInterval(ProxyManager.PARAM_PROXY_QUICK_CONNECT_TIMEOUT,
                              ProxyManager.DEFAULT_PROXY_QUICK_CONNECT_TIMEOUT));
      quickConnPool.setDataTimeout
        (conf.getTimeInterval(ProxyManager.PARAM_PROXY_QUICK_DATA_TIMEOUT,
                              ProxyManager.DEFAULT_PROXY_QUICK_DATA_TIMEOUT));
    }
  }

  protected boolean isInCache() {
    return (cu != null) && cu.hasContent();
  }
  protected LockssUrlConnection openLockssUrlConnection(LockssUrlConnectionPool pool)
    throws IOException {

    boolean isInCache = isInCache();
    String ifModified = null;
    
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

      // copy request headers to connection
      Enumeration vals = req.getHeaders(hdr);
      while (vals.hasMoreElements()) {
        String val = (String)vals.nextElement();
        if (val!=null) {
          conn.addRequestProperty(hdr, val);
        }
      }
    }

    /* PJG: Comment out this block to always fetch newer cached content from publisher */
    // If the user sent an if-modified-since header, use it unless the
    // cache file has a later last-modified
    if (isInCache) {
      CIProperties cuprops = cu.getProperties();
      String cuLast = cuprops.getProperty(CachedUrl.PROPERTY_LAST_MODIFIED);
      if (log.isDebug3()) {
        log.debug3("ifModified: " + ifModified);
        log.debug3("cuLast: " + cuLast);
      }
      if (cuLast != null) {
        if (ifModified == null) {
          ifModified = cuLast;
        } else {
          try {
            if (HeaderUtil.isEarlier(ifModified, cuLast)) {
              ifModified = cuLast;
            }
          } catch (DateParseException e) {
            // preserve user's header if parse failure
          }
        }
      }
    }
    /* PJG */

    if (ifModified != null) {
      conn.setRequestProperty(HttpFields.__IfModifiedSince, ifModified);
    }

    // send address or original requester
    conn.addRequestProperty(HttpFields.__XForwardedFor,
                            req.getRemoteAddr());

    // copy cookies
    String cookiePolicy = proxyMgr.getCookiePolicy();
    String COOKIE_POLICY_DEFAULT = "default";
    if (cookiePolicy != null &&
        !cookiePolicy.equalsIgnoreCase(COOKIE_POLICY_DEFAULT)) {
      conn.setCookiePolicy(cookiePolicy);
    }
    
    return conn;
  }
  
  /**
   * Handle request for AU, where either CU is unknown or CU exists and has content.
   * Instance values {@link #url} and {@link #au} must be specified. If {@link #cu} is
   * specified, it will be used to fetch the cached content if it is not available 
   * from the publisher. 
   * 
   * @throws IOException for IO errors
   */
  protected void handleAuRequest() throws IOException {
    boolean isInCache = isInCache();
    String host = UrlUtil.getHost(url);
    LockssUrlConnection conn = null;
    try {
      // get connection to content from the publisher
      ensureConnectionPool();
      boolean useQuick =
        (isInCache ||
            (proxyMgr.isHostDown(host) &&
             (proxyMgr.getHostDownAction() ==
              ProxyManager.HOST_DOWN_NO_CACHE_ACTION_QUICK)));
      conn = openLockssUrlConnection(useQuick ? quickConnPool : connPool);
      conn.execute();
    } catch (IOException ex) {
      if (log.isDebug3()) log.debug3("conn.execute", ex);

      // mark host down if connection timed out
      if (ex instanceof LockssUrlConnection.ConnectionTimeoutException) {
        proxyMgr.setHostDown(host, isInCache);
      }
      
      // tear down connection
      safeClose(conn);
      conn = null;
    }
    
    int response = HttpResponse.__404_Not_Found;
    try {
      if (conn != null) {
        response = conn.getResponseCode();
        if (log.isDebug3()) log.debug3("response: " + response + " " + conn.getResponseMessage());
        if (response == HttpResponse.__200_OK) {  
          // get content from publisher through connection
          serveFromPublisher(conn);
          return;
        }
      }
    } finally {
      // ensure connection is closed
      safeClose(conn);
    }
    
    if (isInCache) {
      // serve content from cache if not available from publisher
      proxyMgr.setRecentlyAccessedUrl(url);
      serveFromCache();
    } else {
      // report not found if not in cache
      log.debug("Not serving cached content: response=" + response + " " + conn.getResponseMessage());
      resp.setStatus(response);
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

    if (ifModifiedSince != null) {
      try {
        if (!HeaderUtil.isEarlier(ifModifiedSince, cuLastModified)) {
          ctype = props.getProperty(CachedUrl.PROPERTY_CONTENT_TYPE);
          String mimeType = HeaderUtil.getMimeTypeFromContentType(ctype);
          log.debug(  "Cached content not modified for: " + url
                      + " mime type=" + mimeType
                      + " size=" + cu.getContentSize()
                      + " cu=" + cu);
          resp.setStatus(HttpResponse.__304_Not_Modified);
          return;
        }
      } catch (org.apache.commons.httpclient.util.DateParseException e) {
        // ignore error, serve file
      }
    }

    String encoding = cu.getEncoding();
    ctype = props.getProperty(CachedUrl.PROPERTY_CONTENT_TYPE);
    String mimeType = HeaderUtil.getMimeTypeFromContentType(ctype);
    log.debug(  "Serving cached content for: " + url
              + " mime type=" + mimeType
              + " size=" + cu.getContentSize()
              + " cu=" + cu);

    resp.setContentType(ctype);
    resp.setHeader(HttpFields.__LastModified, cuLastModified);
    
    // get content from repository if not available from publisher
    InputStream original = cu.getUnfilteredInputStream();

    AuState aus = AuUtil.getAuState(au);
    if (!aus.isOpenAccess()) {
      resp.setHeader(HttpFields.__CacheControl, "private");
    }   
    
    // Add a header to the response to identify content from LOCKSS cache
    resp.setHeader(Constants.X_LOCKSS, Constants.X_LOCKSS_FROM_CACHE);

    // rewrite original input stream from publisher or cache
    handleRewriteInputStream(original, mimeType, encoding);
  }
  
  /**
   * Serve content from publisher for connection.
   * 
   * @param conn the connection
   * @throws IOException if cannot read content
   */
  protected void serveFromPublisher(LockssUrlConnection conn) throws IOException {
    // get content from publisher
    ctype = conn.getResponseContentType();
    String mimeType = HeaderUtil.getMimeTypeFromContentType(ctype);
    log.debug2(  "Serving publisher content for: " + url
               + " mime type=" + mimeType + " size=" + conn.getResponseContentLength());
    resp.setContentType(ctype);
    
    // copy connection response headers to servlet response
    int h = 0;
    String hdr = conn.getResponseHeaderFieldKey(h);
    String val = conn.getResponseHeaderFieldVal(h);
    while ((hdr != null) || (val != null)) {
      if (  (hdr!=null) && (val!=null) 
         && !HttpFields.__KeepAlive.equalsIgnoreCase(hdr)
         && !HttpFields.__Connection.equalsIgnoreCase(hdr)) {
        resp.addHeader(hdr, val);
      }
      h++;
      hdr=conn.getResponseHeaderFieldKey(h);
      val=conn.getResponseHeaderFieldVal(h);
    }
    
    long lastModified = conn.getResponseLastModified();
    resp.setHeader(HttpFields.__LastModified, ""+lastModified);
    
    // get input stream and encoding
    InputStream original = conn.getResponseInputStream();
    String encoding = conn.getResponseContentEncoding();

    // rewrite original input stream from publisher or cache
    handleRewriteInputStream(original, mimeType, encoding);
  }

  protected void safeClose(LockssUrlConnection conn) {
    // close connection once done
    if (conn != null) {
      try {
        conn.release();
      } catch (Exception e) {}
    }
    
  }
  protected void handleRewriteInputStream(InputStream original, String mimeType, String encoding)  throws IOException {
    InputStream rewritten = original;
    OutputStream outStr = null;
    try {
      LinkRewriterFactory lrf = null;
      if ((cu != null) && StringUtil.isNullString(getParameter("norewrite"))) {
        lrf = cu.getLinkRewriterFactory();
      }
      if (lrf != null) {
        try {
          rewritten =
            lrf.createLinkRewriter(mimeType,
                                   au,
                                   original,
                                   encoding,
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
      }
      outStr = resp.getOutputStream();

      long bytes = StreamUtil.copy(rewritten, outStr);
      if (bytes <= Integer.MAX_VALUE) {
          resp.setContentLength((int)bytes);
      }
    } finally {
      IOUtil.safeClose(outStr);
      IOUtil.safeClose(original);
      IOUtil.safeClose(rewritten);
    }
  }

  protected void handleMissingUrlRequest(String missingUrl)
      throws IOException {
    String missing =
      missingUrl + ((au != null) ? " in AU: " + au.getName() : "");
    switch (missingFileAction) {
    case Error_404:
      resp.sendError(HttpServletResponse.SC_NOT_FOUND,
		     missing + " is not preserved on this LOCKSS box");
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
	resp.sendError(HttpServletResponse.SC_NOT_FOUND,
		       missing + " is not preserved on this LOCKSS box");
      }
      break;
    case AuIndex:
      displayIndexPage(pluginMgr.getAllAus(),
		       HttpResponse.__404_Not_Found,
		       "Requested URL ( " + missing
		       + " ) is not preserved on this LOCKSS box.");
      break;
    case ForwardRequest:
      // Easiest way to do this is probably to return without handling the
      // request and add a proxy handler to the context.
      resp.sendError(HttpServletResponse.SC_NOT_FOUND,
		     "URL " + missing + " not found"); // placeholder
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
}
