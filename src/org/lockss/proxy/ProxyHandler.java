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
// Some portions of this code are:
// ========================================================================
// Copyright (c) 2003 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.lockss.proxy;

import java.io.*;
import java.net.*;
import java.net.HttpURLConnection;
import java.util.*;
import org.apache.commons.collections.SetUtils;
import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.httpclient.util.*;
import org.apache.commons.logging.Log;
import org.lockss.app.LockssDaemon;
import org.lockss.config.*;
import org.lockss.daemon.CuUrl;
import org.lockss.daemon.PluginBehaviorException;
import org.lockss.exporter.counter.CounterReportsRequestRecorder;
import org.lockss.exporter.counter.CounterReportsRequestRecorder.PublisherContacted;
import org.lockss.plugin.*;
import org.lockss.state.AuState;
import org.lockss.util.*;
import org.lockss.util.StringUtil;
import org.lockss.util.urlconn.*;
import org.lockss.servlet.ServletUtil;
import org.lockss.jetty.*;
import org.mortbay.http.*;
import org.mortbay.http.handler.AbstractHttpHandler;
import org.mortbay.log.LogFactory;
import org.mortbay.util.*;
import org.mortbay.util.URI;
import org.mortbay.html.*;

/* ------------------------------------------------------------ */
/** Proxy request handler.  A HTTP/1.1 Proxy with special handling for
 * content in a LOCKSS cache.  Uses the JVMs URL implementation or
 * LockssUrlConnection to make proxy requests.  Serves content out of cache
 * if not available from publisher.
 * <P>The HttpTunnel mechanism is also used to implement the CONNECT method.
 *
 * @author Greg Wilkins (gregw)
 * @author tal
 */
@SuppressWarnings("serial")
public class ProxyHandler extends AbstractHttpHandler {
  private static Logger log = Logger.getLogger("ProxyHandler");
  private static Log jlog = LogFactory.getLog(ProxyHandler.class);


  /** a GET of this path results in an index page of all AU manifest
   * pages */
  public static String MANIFEST_INDEX_URL_PATH = "/";

  /** Force the proxy to serve only locally cached content.  Mainly useful
   * in testing. */
  static final String PARAM_NEVER_PROXY =
      Configuration.PREFIX + "proxy.neverProxy";
  static final boolean DEFAULT_NEVER_PROXY = false;

  /** If true the audit proxy refrains from returning 404 until all AUs are
   * started - 503 is returned instead. */
  static final String PARAM_AUDIT_503_UNTIL_AUS_STARTED =
    Configuration.PREFIX + "proxy.audit503UntilAusStarted";
  static final boolean DEFAULT_AUDIT_503_UNTIL_AUS_STARTED = true;

  /** https requests are tunneled through CONNECT on same host so appear to
   * come from loopback addr.  To report correct request address, CONNECT
   * method records source addr in a map keyed by port number of the
   * loopback connection.  We don't know when we're done with an entry
   * because CONNECT connections from clients are reused, so used a fixed
   * size LRU map.  This is the maximum size. */
  static final String PARAM_LOOPBACK_CONNECT_MAP_MAX = 
    Configuration.PREFIX + "proxy.loopbackConnectMapMax";
  static final int DEFAULT_LOOPBACK_CONNECT_MAP_MAX = 100;

  private final Map<Integer,String> loopbackConnectMap;

  private LockssDaemon theDaemon = null;
  private PluginManager pluginMgr = null;
  private ProxyManager proxyMgr = null;
  private LockssUrlConnectionPool connPool = null;
  private LockssUrlConnectionPool quickFailConnPool = null;
  private String hostname;
  private boolean neverProxy = DEFAULT_NEVER_PROXY;
  private boolean auditProxy = false;
  private boolean auditIndex = false;
  private boolean audit503UntilAusStarted = DEFAULT_AUDIT_503_UNTIL_AUS_STARTED;
  private String connectHost;
  private int connectPort = -1;
  private int sslListenPort = -1;
  private String errorTemplate;

  private boolean isFailOver = false;
  private URI failOverTargetUri;

  ProxyHandler(LockssDaemon daemon) {
    theDaemon = daemon;
    pluginMgr = theDaemon.getPluginManager();
    // XXX This is wrong.  All proxy instances get manager for normal
    // proxy.  Each manager (audit, failover) should pass itself to the
    // handler it creates.  But is mostly only used by normal proxy, and
    // would require casting.
    proxyMgr = theDaemon.getProxyManager();
    Configuration config = ConfigManager.getCurrentConfig();
    neverProxy = config.getBoolean(PARAM_NEVER_PROXY, DEFAULT_NEVER_PROXY);
    audit503UntilAusStarted =
      config.getBoolean(PARAM_AUDIT_503_UNTIL_AUS_STARTED,
			DEFAULT_AUDIT_503_UNTIL_AUS_STARTED);

    hostname = PlatformUtil.getLocalHostname();

    int connectMapSize = config.getInt(PARAM_LOOPBACK_CONNECT_MAP_MAX,
				       DEFAULT_LOOPBACK_CONNECT_MAP_MAX);
    loopbackConnectMap =
      Collections.synchronizedMap(new LRUMap(connectMapSize));
  }

  ProxyHandler(LockssDaemon daemon, LockssUrlConnectionPool pool) {
    this(daemon);
    this.connPool = pool;
    this.quickFailConnPool = pool;
  }

  ProxyHandler(LockssDaemon daemon, LockssUrlConnectionPool pool,
               LockssUrlConnectionPool quickFailConnPool) {
    this(daemon, pool);
    this.quickFailConnPool = quickFailConnPool;
  }

  // Entry points from ProxyManager

  /** If set to true, will act like an audit proxy.  (Content will be
   * served only from the cache; requests will never be proxied, will serve
   * CLOCKSS unsubscribed content */
  public void setAuditProxy(boolean flg) {
    auditProxy = flg;
    setFromCacheOnly(flg);
  }

  /** If set to true, audit proxy will generate a manifest index just like
   * the normal proxy does */
  public void setAuditIndex(boolean flg) {
    auditIndex = flg;
  }

  /** If set to true, content will be served only from the cache; requests
   * will never be proxied */
  public void setFromCacheOnly(boolean flg) {
    neverProxy = flg;
  }

  /** Set the address to which CONNECT requests will connect, ignoring the
   * address in the request.  */
  public void setConnectAddr(String host, int port) {
    connectHost = host;
    connectPort = port;
    log.debug2("setConnectAddr(" + host + ", " + port + ")");
  }

  /** Set the SSL port on which this proxy is listening.  */
  public void setSslListenPort(int port) {
    sslListenPort = port;
  }

  /** Set the error template.  */
  public void setErrorTemplate(String template) {
    errorTemplate = template;
  }

  /** Set a target to use as a base (protocol, host, port) for all incoming
   * request URLs.  To be used to cause the proxy to serve locally cached
   * content in response to direct (non-proxy) GET requests. */
  public void setProxiedTarget(String target) {
    failOverTargetUri = new URI(target);
    isFailOver = true;
  }

  public void closeIdleConnections(long idleTime) {
    log.debug2("Closing idle connections");
    closeIdleConnections(quickFailConnPool, idleTime);
    closeIdleConnections(connPool, idleTime);
  }

  private void closeIdleConnections(LockssUrlConnectionPool pool,
                                    long idleTime) {
    try {
      pool.closeIdleConnections(0);
    } catch (RuntimeException e) {
      log.warning("closeIdleConnections: ", e);
    }
  }

  /** Create a Via header value:
   * 1.1 thishost:port (LOCKSS/Jetty)
   */
  String makeVia(HttpRequest req) {
    int port = 0;
    try {
      port = req.getHttpConnection().getServerPort();
    } catch (Exception ignore) {
    }
    return proxyMgr.makeVia(hostname, port);
  }

  protected int _tunnelTimeoutMs=250;

  /* ------------------------------------------------------------ */
  /** Map of leg by leg headers (not end to end).
   * Should be a set, but more efficient string map is used instead.
   */
  protected StringMap _DontProxyHeaders = new StringMap();
  {
    Object o = new Object();
    _DontProxyHeaders.setIgnoreCase(true);
    _DontProxyHeaders.put(HttpFields.__ProxyConnection,o);
    _DontProxyHeaders.put(HttpFields.__Connection,o);
    _DontProxyHeaders.put(HttpFields.__KeepAlive,o);
    _DontProxyHeaders.put(HttpFields.__TransferEncoding,o);
    _DontProxyHeaders.put(HttpFields.__TE,o);
    _DontProxyHeaders.put(HttpFields.__Trailer,o);
    _DontProxyHeaders.put(HttpFields.__ProxyAuthorization,o);
    _DontProxyHeaders.put(HttpFields.__ProxyAuthenticate,o);
    _DontProxyHeaders.put(HttpFields.__Upgrade,o);
  }

  /* ------------------------------------------------------------ */
  /**  Map of allows schemes to proxy
   * Should be a set, but more efficient string map is used instead.
   */
  protected StringMap _ProxySchemes = new StringMap();
  {
    Object o = new Object();
    _ProxySchemes.setIgnoreCase(true);
    _ProxySchemes.put(HttpMessage.__SCHEME,o);
    _ProxySchemes.put(HttpMessage.__SSL_SCHEME,o);
    _ProxySchemes.put("ftp",o);
  }

  /* ------------------------------------------------------------ */
  public int getTunnelTimeoutMs() {
    return _tunnelTimeoutMs;
  }

  /* ------------------------------------------------------------ */
  /** Tunnel timeout.
   * IE on win2000 has connections issues with normal timeout handling.
   * This timeout should be set to a low value that will expire to allow IE to
   * see the end of the tunnel connection.
   */
  public void setTunnelTimeoutMs(long ms) {
    _tunnelTimeoutMs = (int)ms;
  }

   /* ------------------------------------------------------------ */
  public void handle(String pathInContext,
                     String pathParams,
                     HttpRequest request,
                     HttpResponse response)
      throws HttpException, IOException {
    try {
      handle0(pathInContext, pathParams, request, response);
    } catch (HttpException e) {
      throw e;
    } catch (org.mortbay.http.EOFException e) {
      throw e;
    } catch (Exception e) {
      log.warning("URL: " + request.getURI(), e);

//       httpResponse.getHttpConnection().forceClose();
      if (!response.isCommitted()) {
        response.sendError(HttpResponse.__500_Internal_Server_Error,
            e.toString());
      } else {
        log.warning("Response already committed");
      }
    } catch (Error e) {
      log.warning("URL: " + request.getURI(), e);

//       httpResponse.getHttpConnection().forceClose();
      if (!response.isCommitted()) {
        response.sendError(HttpResponse.__500_Internal_Server_Error,
            e.toString());
      } else {
        log.warning("Response already committed");
      }
    } finally {
//       response.complete();
    }
  }


  public void handle0(String pathInContext,
                      String pathParams,
                      HttpRequest request,
                      HttpResponse response)
      throws HttpException, IOException {
    InputStream reqBodyStrm = null;
    URI uri = request.getURI();
    long reqStartTime = TimeBase.nowMs();
    if (proxyMgr.isLogReqStart()) {
      log.info("Proxy handle url: " + uri);
    }

    if (!proxyMgr.isMethodAllowed(request.getMethod())) {
      sendForbid(request,response,uri);
      logAccess(request, "forbidden method: " + request.getMethod());
      return;
    }

    // Is this a CONNECT request?
    if (HttpRequest.__CONNECT.equals(request.getMethod())) {
      response.setField(HttpFields.__Connection,"close"); // XXX Needed for IE????
      handleConnect(pathInContext,pathParams,request,response);
      return;
    }

    // The URI in https proxy requests is generally host-relative.  Detect
    // this and replace the URI in the request with the absolute URI.  Do
    // this only for SSL connections, else accessing directly with browser
    // can cause a loop.
    HttpConnection conn = request.getHttpConnection();
    if (sslListenPort > 0 && sslListenPort == conn.getServerPort()) {
      if (uri.toString().startsWith("/")) {
	String root = request.getRootURL().toString();
	if (!StringUtil.isNullString(root)) {
	  String newUri = root.toLowerCase() + uri.toString();
	  log.debug("Relative URI: " + uri + " -> " + newUri);
	  uri.setURI(newUri);
	}
      }
    }

    if (log.isDebug3()) {
      log.debug3("pathInContext="+pathInContext);
      log.debug3("URI="+uri);
    }

    // Handling post requests specially.
    // During a crawl, we can store links from a POST form similar to a GET form.
    // See TestHtmlParserLinkExtractor::testPOSTForm.
    //
    // This logic is similar to logic in ServeContent.
    //TODO -- CTG: we need to determine the mime type and dispatch based on it

    // Avoid doing this if possible, as we need to buffer the request
    // in case we decide later to forward it
    String source = request.getField(Constants.X_LOCKSS_SOURCE);

    if (HttpRequest.__POST.equals(request.getMethod())
        && proxyMgr.isHandleFormPost()
        && !Constants.X_LOCKSS_SOURCE_PUBLISHER.equals(source)

        ) {
      log.debug3("POST request found!");
      // Get a map of the request parameters, and the complete body
      // InputStream which will be needed if we forward the request.
      MultiMap params = new MultiMap(16);
      reqBodyStrm = extractParametersTo(request, params);

      // Canonical order for form args assembled into a query string
      // is alphabetical
      Set<String> unsortedKeys = SetUtils.typedSet(params.keySet(), String.class);
      SortedSet<String> keys = new TreeSet<String>(unsortedKeys);

      FormUrlHelper helper = new FormUrlHelper(uri.toString());

      for (String k : keys) {
        helper.add((k), params.get(k).toString());
      }
      if (log.isDebug3()) {
        log.debug3("Overriding original URI to:" + helper.toEncodedString());
      }
      URI postUri = new URI(helper.toEncodedString());
      // We only want to override the post request by proxy if we cached it during crawling.
      CachedUrl cu = pluginMgr.findCachedUrl(postUri.toString());
      if (cu != null) {
        uri = postUri;
      }
    }

    if (isFailOver) {
      if (uri.getHost() == null && failOverTargetUri.getHost() != null) {
        uri.setHost(failOverTargetUri.getHost());
        uri.setPort(failOverTargetUri.getPort());
        uri.setScheme(failOverTargetUri.getScheme());
      }
      if (log.isDebug2()) log.debug2("Failover URI: " + uri);
    } else {
      // XXX what to do here?
    }

    String urlString = uri.toString();
    if (MANIFEST_INDEX_URL_PATH.equals(urlString)) {
      sendIndexPage(request, response);
      logAccess(request, "200 index page", TimeBase.msSince(reqStartTime));
      return;
    }
    String unencUrl = urlString;
    if (proxyMgr.isMinimallyEncodeUrls()) {
      urlString = UrlUtil.minimallyEncodeUrl(urlString);
      if (!urlString.equals(unencUrl)) {
        log.debug("Encoded " + unencUrl + " to " + urlString);
      }
    }

    // Does the URL point to a resolver rather than a
    // server?
/* PJG: DOIs now resolved by ServeContent
    String resolvedUrl = MetadataUtil.proxyResolver(urlString);
    if (resolvedUrl != null) {
      // Yes - send a redirect
      sendRedirect(request, response, resolvedUrl);
      logAccess(request, "302 redirect to resolved DOI");
      return;
    }
*/
    ArchivalUnit au;
    CachedUrl cu;
    String auid = request.getField(Constants.X_LOCKSS_AUID);
    if (!StringUtil.isNullString(auid)) {
      au = pluginMgr.getAuFromId(auid);
      if (au == null) {
	// Requested AU not found.  Return 412, or 503 during startup
	if (audit503UntilAusStarted && !theDaemon.areAusStarted()) {
	  // TODO - Guesstimate remaining time and add Retry-After header
	  String errmsg =
	    "This LOCKSS box is starting.  Please try again in a moment.";
	  response.sendError(HttpResponse.__503_Service_Unavailable, errmsg);
	  request.setHandled(true);
	  logAccess(request, "not present (no AU: " + auid + "), 503",
		    TimeBase.msSince(reqStartTime));
	} else {
	  response.sendError(HttpResponse.__412_Precondition_Failed,
			     "AU specified by " + Constants.X_LOCKSS_AUID +
			     " header not found: " + auid);
	  request.setHandled(true);
	  logAccess(request, "412 AU not found: " + auid,
		    TimeBase.msSince(reqStartTime));
	}
	return;
      }
      String normUrl = urlString;
      if (proxyMgr.isNormalizeAuidRequest()) {
	try {
	  normUrl = UrlUtil.normalizeUrl(urlString, au);
	} catch (PluginBehaviorException e) {
	  log.siteWarning("Normalizer error: " + urlString, e);
	}
      }
      cu = au.makeCachedUrl(normUrl);
    } else {
      cu = pluginMgr.findCachedUrl(urlString);
    }
    // Don't allow CLOCKSS to serve local content for unsubscribed AUs
    if (cu != null && theDaemon.isDetectClockssSubscription() && !auditProxy) {
      au = cu.getArchivalUnit();
      switch (AuUtil.getAuState(au).getClockssSubscriptionStatus()) {
        case AuState.CLOCKSS_SUB_UNKNOWN:
        case AuState.CLOCKSS_SUB_NO:
        case AuState.CLOCKSS_SUB_INACCESSIBLE:
          // If CLOCKSS unsubscribed content, forget that we have local copy
          cu = null;
          break;
        case AuState.CLOCKSS_SUB_YES:
          break;
      }
    }

    try {
      boolean isRepairRequest = proxyMgr.isRepairRequest(request);
      boolean isInCache = cu != null && cu.hasContent();

      if (log.isDebug2()) {
        log.debug2("cu: " + (isRepairRequest ? "(repair) " : "") + cu);
      }
      if (isRepairRequest || neverProxy ||
	  Constants.X_LOCKSS_SOURCE_CACHE.equals(source) ||
	  (isInCache && proxyMgr.isHostDown(uri.getHost()))) {
	if (isInCache) {
	  if (isRepairRequest && log.isDebug()) {
	    log.debug("Serving repair to " + request.getRemoteAddr() + ", " + cu);
	  }
	  serveFromCache(pathInContext, pathParams, request,
			 response, cu);
	  logAccess(request, "200 from cache", TimeBase.msSince(reqStartTime));
	  // Record the necessary information required for COUNTER reports.
	  CounterReportsRequestRecorder.getInstance().recordRequest(urlString,
	      CounterReportsRequestRecorder.PublisherContacted.FALSE, 200,
	      null);
	  return;
	} else {
	  // Not found on cache and told not to forward request
	  String errmsg = auditProxy
	    ? "Not found in LOCKSS box " + PlatformUtil.getLocalHostname()
	    : "Not found";
	  if (audit503UntilAusStarted && !theDaemon.areAusStarted()) {
	    // TODO - Guesstimate remaining time and add Retry-After header
	    errmsg =
	      "This LOCKSS box is starting.  Please try again in a moment.";
	    response.sendError(HttpResponse.__503_Service_Unavailable, errmsg);
	    request.setHandled(true);
	    logAccess(request, "not present, no forward, 503",
		      TimeBase.msSince(reqStartTime));
	  } else if (auditIndex) {
	    sendErrorPage(request,
			  response,
			  404, errmsg,
			  pluginMgr.getCandidateAus(urlString));
	    logAccess(request, "not present, no forward, 404 w/index",
		      TimeBase.msSince(reqStartTime));
	  } else {
	    response.sendError(HttpResponse.__404_Not_Found, errmsg);
	    request.setHandled(true);
	    logAccess(request, "not present, no forward, 404",
		      TimeBase.msSince(reqStartTime));
	  }
	  return;
	}
      }

      if (!isInCache
          && !Constants.X_LOCKSS_SOURCE_PUBLISHER.equals(source)
          && (proxyMgr.getHostDownAction() ==
              ProxyManager.HOST_DOWN_NO_CACHE_ACTION_504)
          && proxyMgr.isHostDown(uri.getHost())) {
        sendErrorPage(request, response, 504,
            hostMsg("Can't connect to", uri.getHost(),
                "Host not responding (cached status)"),
            pluginMgr.getCandidateAus(urlString));
        logAccess(request, "not present, host down, 504",
            TimeBase.msSince(reqStartTime));
        return;
      }
      if (UrlUtil.isHttpOrHttpsUrl(urlString)) {
        if (HttpRequest.__GET.equals(request.getMethod())) {
          doLockss(pathInContext, pathParams, request, response,
              urlString, cu, reqStartTime);
          return;
        }
      }
      doSun(pathInContext, pathParams, request, response, reqBodyStrm);
      logAccess(request, "unrecognized request type, forwarded",
          TimeBase.msSince(reqStartTime));
    } finally {
      AuUtil.safeRelease(cu);
    }
  }

  /** Proxy a connection using Java's native URLConection */
  void doSun(String pathInContext,
             String pathParams,
             HttpRequest request,
             HttpResponse response,
             InputStream reqIn) throws IOException {
    URI uri = request.getURI();
    try {
      // Do we proxy this?
      URL url=isProxied(uri);
      if (url==null) {
        if (isForbidden(uri)) {
          sendForbid(request,response,uri);
	  logAccess(request, "forbidden method: " + request.getMethod());
	}
        return;
      }

      if(jlog.isDebugEnabled())jlog.debug("PROXY URL="+url);

      URLConnection connection = url.openConnection();
      connection.setAllowUserInteraction(false);

      // Set method
      HttpURLConnection http = null;
      if (connection instanceof HttpURLConnection) {
        http = (HttpURLConnection)connection;
        http.setRequestMethod(request.getMethod());
        http.setInstanceFollowRedirects(false);
      }

      // check connection header
      String connectionHdr = request.getField(HttpFields.__Connection);
      if (connectionHdr!=null &&
          (connectionHdr.equalsIgnoreCase(HttpFields.__KeepAlive)||
           connectionHdr.equalsIgnoreCase(HttpFields.__Close)))
        connectionHdr=null;

      // copy headers
      boolean hasContent=false;
      Enumeration en = request.getFieldNames();

      while (en.hasMoreElements()) {
        // XXX could be better than this!
        String hdr=(String)en.nextElement();

        if (_DontProxyHeaders.containsKey(hdr))
          continue;

        if (connectionHdr!=null && connectionHdr.indexOf(hdr)>=0)
          continue;

        if (HttpFields.__ContentType.equalsIgnoreCase(hdr))
          hasContent=true;

        Enumeration vals = request.getFieldValues(hdr);
        while (vals.hasMoreElements()) {
          String val = (String)vals.nextElement();
          if (val!=null) {
            connection.addRequestProperty(hdr,val);
          }
        }
      }

      // Proxy headers
      connection.addRequestProperty(HttpFields.__Via, makeVia(request));
      String reqAddr = getOrigReqAddr(request);
      if (!StringUtil.isNullString(reqAddr)) {
	connection.addRequestProperty(HttpFields.__XForwardedFor, reqAddr);
      }
      // a little bit of cache control
      String cache_control = request.getField(HttpFields.__CacheControl);
      if (cache_control!=null &&
          (cache_control.indexOf("no-cache")>=0 ||
           cache_control.indexOf("no-store")>=0))
        connection.setUseCaches(false);

      // customize Connection
      customizeConnection(pathInContext,pathParams,request,connection);

      try {
        connection.setDoInput(true);

        // do input thang!
        InputStream in = reqIn != null ? reqIn : request.getInputStream();
        if (hasContent) {
          connection.setDoOutput(true);
          IO.copy(in,connection.getOutputStream());
        }

        // Connect
        connection.connect();
      } catch (Exception e) {
        LogSupport.ignore(jlog,e);
      }

      InputStream proxy_in = null;

      // handler status codes etc.
      int code=HttpResponse.__500_Internal_Server_Error;
      if (http!=null) {
        proxy_in = http.getErrorStream();

        code=http.getResponseCode();
        response.setStatus(code);
        response.setReason(http.getResponseMessage());
      }

      if (proxy_in==null) {
        try {proxy_in=connection.getInputStream();}
        catch (Exception e) {
          LogSupport.ignore(jlog,e);
          proxy_in = http.getErrorStream();
        }
      }

      // clear response defaults.
      response.removeField(HttpFields.__Date);
      response.removeField(HttpFields.__Server);

      // set response headers
      int h=0;
      String hdr=connection.getHeaderFieldKey(h);
      String val=connection.getHeaderField(h);
      while(hdr!=null || val!=null) {
        if (hdr!=null && val!=null && !_DontProxyHeaders.containsKey(hdr))
          response.addField(hdr,val);
        h++;
        hdr=connection.getHeaderFieldKey(h);
        val=connection.getHeaderField(h);
      }
      response.addField(HttpFields.__Via, makeVia(request));

      // Handled
      request.setHandled(true);
      if (proxy_in!=null)
        IO.copy(proxy_in,response.getOutputStream());

    } catch (Exception e) {
      log.warning("doSun error", e);
      if (!response.isCommitted())
        response.sendError(HttpResponse.__400_Bad_Request,
            e.getMessage());
    }
  }

  boolean isPubNever(CachedUrl cu) {
    return cu != null && AuUtil.isPubNever(cu.getArchivalUnit());
  }

  ArchivalUnit getSpecifiedAu(HttpRequest request) {
    String auid = request.getField(Constants.X_LOCKSS_AUID);
    if (StringUtil.isNullString(auid)) {
      return null;
    }
    return pluginMgr.getAuFromId(auid);
  }

  /** Extract query args and form parameters from request URI and
   * body, and return an InputStream for the complete body. */
  // Adapted from org.mortbay.http.HttpRequest
  private InputStream extractParametersTo(HttpRequest request,
                                          MultiMap paramMap) {
    InputStream reqIn = null;
    ByteArrayOutputStream2 bout = null;
    int maxFormContentSize = proxyMgr.getMaxFormSize();

    // Handle query string
    String encoding = request.getCharacterEncoding();
    if (encoding == null) {
      // No encoding, so use the existing characters.
      encoding = org.mortbay.util.StringUtil.__ISO_8859_1;
      request.getURI().putParametersTo(paramMap);
    } else {
      // An encoding has been set, so reencode query string.
      String query = request.getURI().getQuery();
      if (query != null) UrlEncoded.decodeTo(query, paramMap, encoding);
    }

    // handle any content.
    String content_type = request.getField(HttpFields.__ContentType);
    if (content_type != null && content_type.length() > 0) {
      content_type = org.mortbay.util.StringUtil.asciiToLowerCase(content_type);
      content_type = HttpFields.valueParameters(content_type, null);

      if (HttpFields.__WwwFormUrlEncode.equalsIgnoreCase(content_type)
          && HttpRequest.__POST.equals(request.getMethod())) {
        int content_length = request.getIntField(HttpFields.__ContentLength);
        if (content_length == 0) {
          log.debug("No form content");
        } else {
          try {
            int max = content_length;
            if (maxFormContentSize > 0) {
              if (max < 0) {
                max = maxFormContentSize;
              } else if (max > maxFormContentSize) {
                throw new IllegalStateException("Form too large");
              }
            }

            // Read the content
            bout = new ByteArrayOutputStream2(max > 0 ? max : 4096);
            reqIn = request.getInputStream();

            // Copy to a byte array.
            // TODO - this is very inefficient and we could
            // save lots of memory by streaming this!!!!
            IO.copy(reqIn, bout, max);

            if (bout.size()==maxFormContentSize && reqIn.available()>0) {
              log.warning("Ignoring POST body larger than " + maxFormContentSize);
            } else {
              // Add form params to query params
              UrlEncoded.decodeTo(bout.toByteArray(), 0, bout.getCount(),
                                  paramMap, encoding);
            }
          } catch (org.mortbay.http.EOFException e) {
            LogSupport.ignore(jlog, e);
          } catch (IOException e) {
            log.warning("Error processing POST body", e);
          }
        }
      }
    }
    if (bout != null) {
      return new SequenceInputStream(new ByteArrayInputStream(bout.getBuf()),
                                     reqIn);
    } else if (reqIn != null) {
      return reqIn;
    } else {
      return request.getInputStream();
    }
  }

  void logAccess(HttpRequest request, String msg) {
    logAccess(request, msg, -1);
  }

  void logAccess(HttpRequest request, String msg, long reqElapsedTime) {
    ArchivalUnit au = null;
    if ((au = getSpecifiedAu(request)) != null) {


      msg = "AU: " + au.getName() + " : " + msg;
    }
    proxyMgr.logAccess(request.getMethod(), request.getURI().toString(), msg,
		       getOrigReqAddr(request),
		       reqElapsedTime);
  }

  /**
   * Record the request in COUNTER if appropriate
   */
  void recordRequest(HttpRequest request,
		     String url,
		     CounterReportsRequestRecorder.PublisherContacted contacted,
		     int publisherCode) {
    if (proxyMgr.isCounterCountable(request.getField(HttpFields.__UserAgent))) {
      CounterReportsRequestRecorder.getInstance().recordRequest(url, contacted,
	  publisherCode, null);
    }
  }

  /** Proxy a connection using LockssUrlConnection */
  void doLockss(String pathInContext,
                String pathParams,
                HttpRequest request,
                HttpResponse response,
                String urlString,
                CachedUrl cu,
                long reqStartTime) throws IOException {
    boolean isInCache = cu != null && cu.hasContent();

    LockssUrlConnection conn = null;
    String source = request.getField(Constants.X_LOCKSS_SOURCE);
    boolean alwaysProxy = Constants.X_LOCKSS_SOURCE_PUBLISHER.equals(source);

    if (alwaysProxy) {
      if (isPubNever(cu)) {
        sendErrorPage(request, response, 504,
            hostMsg("Can't connect to", request.getURI().getHost(),
                "Not permitted by configuration"));
        return;
      } else {
        // Force following to forward to publisher
        isInCache = false;
      }
    }

    try {
      // If we recently served this url from the cache, don't check with
      // publisher for newer content.
      // XXX This needs to forward the request to the publisher (but not
      // wait for the result) so the publisher can count the access.
      if (isInCache && (proxyMgr.isRecentlyAccessedUrl(urlString)
			|| isPubNever(cu))) {
	if (log.isDebug2()) log.debug2("Nopub: " + cu.getUrl());
	serveFromCache(pathInContext, pathParams, request, response, cu);
	logAccess(request, "200 from cache", TimeBase.msSince(reqStartTime));
	// Record the necessary information required for COUNTER reports.
	recordRequest(request,
		      urlString,
		      CounterReportsRequestRecorder.PublisherContacted.FALSE,
		      200);
	return;
      }
      if (isPubNever(cu)) {
        if (isInCache) {
          // shouldn't happen, (isInCache && isPubNever) handled before
          // calling this method.
          log.error("Shouldn't happen, isInCache && isPubNever: " +
                    cu.getUrl());
        } else {
          Collection<ArchivalUnit> candidateAus =
              pluginMgr.getCandidateAus(urlString);
          if (candidateAus != null && !candidateAus.isEmpty()) {
            sendErrorPage(request, response, 404,
                "Host " + request.getURI().getHost() +
                " no longer has content",
                candidateAus);
            logAccess(request, "not present, pub_never, 404 with index",
                TimeBase.msSince(reqStartTime));
            return;
          } else {
            // what to do here?  No content, no candidate AUs, no pub.
            // send 404
            return;
          }
        }
      }
      boolean useQuick =
          (isInCache ||
           (proxyMgr.isHostDown(request.getURI().getHost()) &&
            (proxyMgr.getHostDownAction() ==
             ProxyManager.HOST_DOWN_NO_CACHE_ACTION_QUICK)));
      try {
        conn =
            UrlUtil.openConnection(LockssUrlConnection.METHOD_PROXY,
                UrlUtil.minimallyEncodeUrl(urlString),
                (useQuick ? quickFailConnPool : connPool));

        conn.setFollowRedirects(false);
      } catch (MalformedURLException e) {
        // HttpClient is persnickety about URLs; if it complains try
        // another way
        log.info("Malformed URL, trying doSun(): " + urlString);
        // XXX make this path display manifest index if erro resp?
        doSun(pathInContext, pathParams, request, response, null);
        return;
      }
      // check connection header
      String connectionHdr = request.getField(HttpFields.__Connection);
      if (connectionHdr!=null &&
          (connectionHdr.equalsIgnoreCase(HttpFields.__KeepAlive)||
           connectionHdr.equalsIgnoreCase(HttpFields.__Close)))
        connectionHdr=null;

      // copy request headers into new request
      boolean hasContent=false;
      String ifModified = null;

      for (Enumeration en = request.getFieldNames();
           en.hasMoreElements(); ) {
        String hdr=(String)en.nextElement();

        if (_DontProxyHeaders.containsKey(hdr)) continue;

        if (connectionHdr!=null && connectionHdr.indexOf(hdr)>=0) continue;

        if (HttpFields.__ContentType.equalsIgnoreCase(hdr)) hasContent=true;

	// Set local address if requested and allowed
        if (Constants.X_LOCKSS_LOCAL_ADDRESS.equalsIgnoreCase(hdr)) {
	  String localAddrStr = request.getField(hdr);
	  if (!StringUtil.isNullString(localAddrStr) &&
	      isAllowedLocalAddress(localAddrStr)) {
	    try {
	      IPAddr localAddr = IPAddr.getByName(localAddrStr);
	      log.debug2("Setting local addr to " + localAddr);
	      conn.setLocalAddress(localAddr);
	    } catch (UnknownHostException e) {
	      log.siteWarning("Illegal source address: " + localAddrStr, e);
	    }
	  }
	}

        if (isInCache) {
          if (HttpFields.__IfModifiedSince.equalsIgnoreCase(hdr)) {
            ifModified = request.getField(hdr);
            continue;
          }
        }

        Enumeration vals = request.getFieldValues(hdr);
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

      if (ifModified != null) {
        conn.setRequestProperty(HttpFields.__IfModifiedSince, ifModified);
      }

      // Proxy-specifix headers
      conn.addRequestProperty(HttpFields.__Via, makeVia(request));
      String reqAddr = getOrigReqAddr(request);
      if (!StringUtil.isNullString(reqAddr)) {
	conn.addRequestProperty(HttpFields.__XForwardedFor, reqAddr);
      }
      String cookiePolicy = proxyMgr.getCookiePolicy();
      if (cookiePolicy != null &&
          !cookiePolicy.equalsIgnoreCase(ProxyManager.COOKIE_POLICY_DEFAULT)) {
        conn.setCookiePolicy(cookiePolicy);
      }

      PublisherContacted pubContacted =
	  CounterReportsRequestRecorder.PublisherContacted.TRUE;

      // If we ever handle input, this is (more-or-less) the HttpClient way
      // to do it

      // if (method instanceof EntityEnclosingMethod) {
      //   EntityEnclosingMethod emethod = (EntityEnclosingMethod) method;
      //   emethod.setRequestBody(conn.getInputStream());
      // }

      // Send the request

      try {
        conn.execute();
      } catch (IOException e) {
        if (log.isDebug3()) log.debug3("conn.execute", e);

	// If connection timed out, remember host is down for a little while.
	// Remember this only for hosts whose content we hold.
	if (e instanceof LockssUrlConnection.ConnectionTimeoutException) {
	  proxyMgr.setHostDown(request.getURI().getHost(), isInCache);
	  pubContacted = CounterReportsRequestRecorder.PublisherContacted.FALSE;
	}
	// if we get any error and it's in the cache, serve it from there
	if (isInCache) {
	  serveFromCache(pathInContext, pathParams, request, response, cu);
	  // Record the necessary information required for COUNTER reports.
	  recordRequest(request,
	      		urlString,
	      		pubContacted,
	      		conn.getResponseCode());
	} else {
	  // else generate an error page
	  sendProxyErrorPage(e, request, response,
			     pluginMgr.getCandidateAus(urlString));
	}
	return;
      }
      // We got a response, should we prefer it to what's in the cache?
      if (isInCache && preferCacheOverPubResponse(cu, conn)) {
	// Remember that we served this URL from the cache, so that for a
	// while we can serve it quickly, without incurring the cost of
	// first checking with the publisher.

	// XXX It's likely that the policy for determining when (and for
	// how long) to put the URL in the recently-accessed-URL cache
	// should differ from the policy for determining whether to serve
	// the content from the cache.  E.g., if the publisher responds
	// with 200, we should perhaps cache the URL if the content
	// returned is the same as what we have (in case the publisher
	// doesn't support if-modified-since).

	proxyMgr.setRecentlyAccessedUrl(urlString);

	serveFromCache(pathInContext, pathParams, request,
		       response, cu);
	// Record the necessary information required for COUNTER reports.
	recordRequest(request, urlString, pubContacted, conn.getResponseCode());
	return;
      }

      Collection<ArchivalUnit> candidateAus = null;
      int code=conn.getResponseCode();
      if (proxyMgr.showManifestIndexForResponse(code)) {
        switch (code) {
          case HttpResponse.__200_OK:
            // XXX check for login page
          case HttpResponse.__304_Not_Modified:
            break;
          default:
            log.debug("Response: " + code + ", finding candidate AUs");
            candidateAus = pluginMgr.getCandidateAus(urlString);
        }
      }
      if (candidateAus != null && !candidateAus.isEmpty()) {
        forwardResponseWithIndex(request, response, candidateAus, conn);
      } else {
        forwardResponse(request, response, conn, reqStartTime);
      }
    } catch (Exception e) {
      log.error("doLockss error", e);
      if (!response.isCommitted())
        response.sendError(HttpResponse.__500_Internal_Server_Error,
            e.getMessage());
    } finally {
      safeReleaseConn(conn);
    }
  }

  boolean isLoopbackAddr(String addr) {
    if (addr == null) return false;
    return addr.equals("127.0.0.1") || addr.equals("::1");
  }

  boolean isAllowedLocalAddress(String addr) {
    java.util.List<String> allowed = proxyMgr.getAllowedLocalAddress();
    if (allowed == null || allowed.size() < 1) {
      return false;
    }
    String one = allowed.get(0);
    if ("*".equals(one) || "any".equalsIgnoreCase(one)) {
      return true;
    }
    return allowed.contains(addr);
  }

  void forwardResponse(HttpRequest request, HttpResponse response,
                       LockssUrlConnection conn, long reqStartTime)
      throws IOException {
    // return response from server
    logAccess(request, conn.getResponseCode() + " from publisher",
	      TimeBase.msSince(reqStartTime));

    response.setStatus(conn.getResponseCode());
    response.setReason(conn.getResponseMessage());

    InputStream proxy_in = conn.getResponseInputStream();

    // clear response defaults.
    response.removeField(HttpFields.__Date);
    response.removeField(HttpFields.__Server);

    // copy response headers
    for (int ix = 0; ; ix ++) {
      String hdr = conn.getResponseHeaderFieldKey(ix);
      String val = conn.getResponseHeaderFieldVal(ix);

      if (hdr==null && val==null) {
        break;
      }
      if (hdr!=null && val!=null && !_DontProxyHeaders.containsKey(hdr)) {
        response.addField(hdr,val);
      }
    }
    response.addField(HttpFields.__Via, makeVia(request));

    // Handled
    request.setHandled(true);
    if (proxy_in!=null) {
      IO.copy(proxy_in,response.getOutputStream());
    }
  }

  void safeReleaseConn(LockssUrlConnection conn) {
    if (conn != null) {
      try {
        conn.release();
      } catch (Exception e) {}
    }
  }

  // return true to pass the request along to the resource handler to
  // (conditionally) serve from the CachedUrl, false to return the server's
  // response to the user.
  boolean preferCacheOverPubResponse(CachedUrl cu, LockssUrlConnection conn) {
    if (cu == null || !cu.hasContent()) {
      return false;
    }
    int code=conn.getResponseCode();

    // Current policy is to serve from cache unless server supplied content.
    switch (code) {
      case HttpResponse.__200_OK:
        // XXX Should run the plugin's LoginPageChecker here, as we want to
        // serve out of cache in that case
        return false;
    }
    return true;
  }

  /* ------------------------------------------------------------ */
  public void handleConnect(String pathInContext,
                            String pathParams,
                            HttpRequest request,
                            HttpResponse response)
      throws HttpException, IOException {
    URI uri = request.getURI();

    if (connectHost == null || connectHost.length() == 0 ||
	connectPort <= 0) {
      // Not allowed
      sendForbid(request,response,uri);
      logAccess(request, "forbidden method: " + request.getMethod());
      return;
    }
    try {
      if (isForbidden(HttpMessage.__SSL_SCHEME, false)) {
	sendForbid(request,response,uri);
	logAccess(request, "forbidden scheme for CONNECT: " +
		  HttpMessage.__SSL_SCHEME);
      } else {
	Socket socket = new Socket(connectHost, connectPort);
	if (isLoopbackAddr(connectHost)) {
	  recordConnectSource(request, socket);
	}

	// XXX - need to setup semi-busy loop for IE.
	int timeoutMs=30000;
	if (_tunnelTimeoutMs > 0) {
	  socket.setSoTimeout(_tunnelTimeoutMs);
	  Object maybesocket = request.getHttpConnection().getConnection();
	  try {
	    Socket s = (Socket) maybesocket;
	    timeoutMs=s.getSoTimeout();
	    s.setSoTimeout(_tunnelTimeoutMs);
	  } catch (Exception e) {
	    log.warning("Couldn't set socket timeout", e);
	  }
	}

	customizeConnection(pathInContext,pathParams,request,socket);
	request.getHttpConnection().setHttpTunnel(new HttpTunnel(socket,
								 timeoutMs));
	logAccess(request, "200 redirected to " +
		  connectHost + ":" + connectPort);

	response.setStatus(HttpResponse.__200_OK);
	response.setContentLength(0);
	request.setHandled(true);
      }
    } catch (Exception e) {
      log.error("Error in CONNECT for " + uri, e);
      response.sendError(HttpResponse.__500_Internal_Server_Error,
			 e.getMessage());
    }


//     try {
//       if(jlog.isDebugEnabled())jlog.debug("CONNECT: "+uri);
//       InetAddrPort addrPort=new InetAddrPort(uri.toString());

//       if (isForbidden(HttpMessage.__SSL_SCHEME, false)) {
// 	sendForbid(request,response,uri);
//       } else {
// 	Socket socket =
// 	  new Socket(addrPort.getInetAddress(),addrPort.getPort());

// 	// XXX - need to setup semi-busy loop for IE.
// 	int timeoutMs=30000;
// 	if (_tunnelTimeoutMs > 0) {
// 	  socket.setSoTimeout(_tunnelTimeoutMs);
// 	  Object maybesocket = request.getHttpConnection().getConnection();
// 	  try {
// 	    Socket s = (Socket) maybesocket;
// 	    timeoutMs=s.getSoTimeout();
// 	    s.setSoTimeout(_tunnelTimeoutMs);
// 	  } catch (Exception e) {
// 	    LogSupport.ignore(jlog,e);
// 	  }
// 	}

// 	customizeConnection(pathInContext,pathParams,request,socket);
// 	request.getHttpConnection().setHttpTunnel(new HttpTunnel(socket,
// 								 timeoutMs));
// 	response.setStatus(HttpResponse.__200_OK);
// 	response.setContentLength(0);
// 	request.setHandled(true);
//       }
//     } catch (Exception e) {
//       LogSupport.ignore(jlog,e);
//       response.sendError(HttpResponse.__500_Internal_Server_Error,
// 			 e.getMessage());
//     }
  }

  /** Associate the remote address of a CONNECT request with the port we
   * use to open a loopback connection to our SSL listener */
  void recordConnectSource(HttpRequest request, Socket socket) {
    String reqAddr = request.getRemoteAddr();
    if (reqAddr != null) {
      loopbackConnectMap.put(socket.getLocalPort(), reqAddr);
      log.debug3("recorded loopback req addr: " + socket.getLocalPort() +
		": " + reqAddr);
    }
  }

  /** If this is a loopback connection opened by us in response to a
   * CONNECT, return the original requestor IP addr, else the actual
   * request addr. */
  String getOrigReqAddr(HttpRequest request) {
    String reqAddr = request.getRemoteAddr();
    if (!isLoopbackAddr(reqAddr)) {
      return reqAddr;
    }
    int reqPort = request.getHttpConnection().getRemotePort();
    if (reqPort > 0) {
      String res = loopbackConnectMap.get(reqPort);
      if (res != null) {
	log.debug3("lookup loopback req addr: " + reqPort + " = " + res);
	return res;
      }
    }
    log.debug3("lookup loopback req addr: " + reqPort + " = null");
    return reqAddr;
  }


  /* ------------------------------------------------------------ */
  /** Customize proxy Socket connection for CONNECT.
   * Method to allow derived handlers to customize the tunnel sockets.
   *
   */
  protected void customizeConnection(String pathInContext,
                                     String pathParams,
                                     HttpRequest request,
                                     Socket socket)
      throws IOException {
  }


  /* ------------------------------------------------------------ */
  /** Customize proxy URL connection.
   * Method to allow derived handlers to customize the connection.
   */
  protected void customizeConnection(String pathInContext,
                                     String pathParams,
                                     HttpRequest request,
                                     URLConnection connection)
      throws IOException {
  }


  /* ------------------------------------------------------------ */
  /** Is URL Proxied.  Method to allow derived handlers to select which
   * URIs are proxied and to where.
   * @param uri The requested URI, which should include a scheme, host and
   * port.
   * @return The URL to proxy to, or null if the passed URI should not be
   * proxied.  The default implementation returns the passed uri if
   * isForbidden() returns true.
   */
  protected URL isProxied(URI uri) throws MalformedURLException {
    // Is this a proxy request?
    if (isForbidden(uri))
      return null;

    // OK return URI as untransformed URL.
    return new URL(uri.toString());
  }


  /* ------------------------------------------------------------ */
  /** Is URL Forbidden.
   *
   * @return True if the URL is not forbidden. Calls
   * isForbidden(scheme,true);
   */
  protected boolean isForbidden(URI uri) {
    String scheme=uri.getScheme();
//     String host=uri.getHost();
//     int port = uri.getPort();
    return isForbidden(scheme,true);
  }


  /* ------------------------------------------------------------ */
  /** Is scheme,host & port Forbidden.
   *
   * @param scheme A scheme that mast be in the proxySchemes StringMap.
   * @param openNonPrivPorts If true ports greater than 1024 are allowed.
   * @return  True if the request to the scheme,host and port is not forbidden.
   */
  protected boolean isForbidden(String scheme, boolean openNonPrivPorts) {
    // Must be a scheme that can be proxied.
    if (scheme==null || !_ProxySchemes.containsKey(scheme))
      return true;

    return false;
  }

  /* ------------------------------------------------------------ */
  /** Send Forbidden.
   * Method called to send forbidden response. Default implementation calls
   * sendError(403)
   */
  protected void sendForbid(HttpRequest request, HttpResponse response,
                            URI uri)
      throws IOException {
    response.sendError(HttpResponse.__403_Forbidden,"Forbidden for Proxy");
  }


  /**
   * Add a X-Lockss-Cu: field to the request with the locksscu: url to serve
   * from the cache, then allow request to be passed on to a
   * LockssResourceHandler.
   * @param pathInContext the path
   * @param pathParams params
   * @param request the HttpRequest
   * @param response the HttpResponse
   * @param cu the CachedUrl
   * @throws HttpException
   * @throws IOException
   */
  // XXX Should this use jetty's request forwarding mechanism instead?
  private void serveFromCache(String pathInContext,
                              String pathParams,
                              HttpRequest request,
                              HttpResponse response,
                              CachedUrl cu)
      throws HttpException, IOException {

    // Save current state then make request editable
    int oldState = request.getState();
    request.setState(HttpMessage.__MSG_EDITABLE);
    request.setField(CuResourceHandler.REQUEST_CU_URL,
		     CuUrl.fromCu(cu).toString());
    request.setState(oldState);
    // Add a header to the response to identify content from LOCKSS cache
    response.setField(Constants.X_LOCKSS, Constants.X_LOCKSS_FROM_CACHE);
    if (log.isDebug2()) {
      log.debug2("serveFromCache(" + cu + ")");
    }
    // Return without handling the request, next in chain is
    // LockssResourceHandler.  (There must be a better way to do this.)
  }

  String hostMsg(String msg, String host, String reason) {
    host = HtmlUtil.htmlEncode(host);
    reason = HtmlUtil.htmlEncode(reason);
    return msg + " <b>" + host + "</b> : " + reason;
  }

  void sendProxyErrorPage(IOException e,
                          HttpRequest request,
                          HttpResponse response,
                          Collection<ArchivalUnit> candidateAus)
      throws IOException {
    URI uri = request.getURI();
    if (e instanceof java.net.UnknownHostException) {
      // DNS failure
      sendErrorPage(request, response, 502,
          hostMsg("Can't connect to", uri.getHost(),
              "Unknown host"),
          candidateAus);
      return;
    }
    if (e instanceof java.net.NoRouteToHostException) {
      sendErrorPage(request, response, 502,
          hostMsg("Can't connect to", uri.getHost(),
              "No route to host"),
          candidateAus);
      return;
    }
    if (e instanceof LockssUrlConnection.ConnectionTimeoutException) {
      sendErrorPage(request, response, 504,
          hostMsg("Can't connect to", uri.getHost(),
              "Host not responding"),
          candidateAus);
      return;
    }
    if (e instanceof java.net.ConnectException) {
      int port = uri.getPort() == 0 ? 80 : uri.getPort();
      sendErrorPage(request, response, 502,
          hostMsg("Can't connect to",
              uri.getHost() + ":" + port,
              "Connection refused"),
          candidateAus);
      return;
    }
    if (e instanceof java.io.InterruptedIOException) {
      sendErrorPage(request, response, 504,
          hostMsg("Timeout waiting for data from", uri.getHost(),
              "Server not responding"),
          candidateAus);
      return;
    }
//     if (e instanceof java.io.IOException) {
    sendErrorPage(request, response, 502,
        hostMsg("Error communicating with", uri.getHost(),
            e.getMessage()),
        candidateAus);
    return;
//     }
  }

  void sendErrorPage(HttpRequest request,
                     HttpResponse response,
                     int code, String msg)
      throws HttpException, IOException {
    sendErrorPage(request, response, code, msg, null);
  }

  void sendErrorPage(HttpRequest request,
                     HttpResponse response,
                     int code, String msg,
                     Collection<ArchivalUnit> candidateAus)
      throws HttpException, IOException {
    response.setStatus(code);
    Integer codeInt = new Integer(code);
    String respMsg = (String)HttpResponse.__statusMsg.get(codeInt);
    if (respMsg != null) {
      response.setReason(respMsg);
    } else {
      respMsg = "Unknown";
    }
    String errMsg = code + " " + respMsg;
    URI uri = request.getURI();
    String urlString = uri.toString();

    response.setContentType(HttpFields.__TextHtml);
    ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer(2048);

    String template = getErrorTemplate();
    SimpleWriterTemplateExpander t =
      new SimpleWriterTemplateExpander(template, writer);
    Map<String,String> tokVal =
      MapUtil.map("ErrorMessage", code + " " + respMsg,
		  "Url", HtmlUtil.htmlEncode(uri.toString()),
		  "Message", msg,
		  "LockssUrl", Constants.LOCKSS_HOME_URL,
		  "DaemonVersion", ConfigManager.getDaemonVersion().displayString(),
		  "Hostname", PlatformUtil.getLocalHostname());
    String token;
    while ((token = t.nextToken()) != null) {
      String val = tokVal.get(token);
      if (val != null) {
	writer.write(val);
      } else if (token.equals("AuIndex")) {
	writeErrorAuIndex(writer, urlString, candidateAus);
      } else {
	log.warning("Unknown token '" + token + "' in error template");
      }
    }

    writer.flush();
    response.setContentLength(writer.size());
    writer.writeTo(response.getOutputStream());
    writer.destroy();

    request.setHandled(true);
  }

  String getErrorTemplate() {
    return errorTemplate != null ? errorTemplate : "Missing error template";
  }

  void writeErrorAuIndex(Writer writer, String urlString,
                         Collection<ArchivalUnit> candidateAus)
      throws IOException {
    if (true) {
      if (candidateAus != null && !candidateAus.isEmpty()) {
        writer.write("<br>This LOCKSS box (" +
                     PlatformUtil.getLocalHostname() +
                     ") does not contain content for that URL, " +
                     "but it does contain possibly related content " +
                     "in the following Archival Units:\n");
        Element ele = ServletUtil.manifestIndex(theDaemon, candidateAus);
        ele.write(writer);
      }
    }
  }

  void writeFooter(Writer writer) throws IOException {
    writer.write("<p><i><small>");
    writer.write("<a href=\"" + Constants.LOCKSS_HOME_URL +
                 "\">LOCKSS proxy</a> " +
                 ConfigManager.getDaemonVersion().displayString() + " on " +
                 ConfigManager.getPlatformHostname());
    writer.write("</small></i></p>");
    writer.write("\n</body>\n</html>\n");
  }

  void sendIndexPage(HttpRequest request,
                     HttpResponse response)
      throws HttpException, IOException {
    try {
      response.setStatus(HttpResponse.__200_OK);
      //     response.setReason(respMsg);

      response.setContentType(HttpFields.__TextHtml);

      Element ele = ServletUtil.manifestIndex(theDaemon, hostname);
      Page page = new Page();
      page.add(ServletUtil.centeredBlock(ele));
      writePage(request, response, page);
    } catch (RuntimeException e) {
      log.error("sendIndexPage", e);
      throw e;
    }
  }

  void sendRedirect(HttpRequest request,
                    HttpResponse response,
                    String toUrl) throws IOException {
    try {
      log.debug("Redirecting to " + toUrl);
      response.sendRedirect(toUrl);
    } catch (RuntimeException e) {
      log.error("sendRedirect ", e);
    }
  }

  void writePage(HttpRequest request, HttpResponse response, Page page)
      throws HttpException, IOException {
    Writer wrtr = new OutputStreamWriter(response.getOutputStream());
    page.write(wrtr);
    wrtr.flush();
    //     response.setContentLength(wrtr.size());
    request.setHandled(true);
  }

  // Eventually this should display both the server's error response and
  // the local AU index
  void forwardResponseWithIndex(HttpRequest request, HttpResponse response,
                                Collection<ArchivalUnit> candidateAus,
                                LockssUrlConnection conn)
      throws HttpException, IOException {
    int code = conn.getResponseCode();
    String host = HtmlUtil.htmlEncode(request.getURI().getHost());
    String reason = conn.getResponseMessage();
    reason = HtmlUtil.htmlEncode(reason);
    String msg = "The server <b>" + host + "</b> responded with <b>"
                 + code + " " + reason + "</b>";
    sendErrorPage(request, response, code, msg, candidateAus);
  }
}
