/*
 * $Id: ProxyHandler.java,v 1.54 2007-04-30 04:52:46 tlipkis Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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
// $Id: ProxyHandler.java,v 1.54 2007-04-30 04:52:46 tlipkis Exp $
// ========================================================================

package org.lockss.proxy;

import java.io.*;
import java.net.*;
import java.net.HttpURLConnection;
import java.util.*;

import org.apache.commons.httpclient.util.*;
import org.apache.commons.logging.Log;
import org.mortbay.http.*;
import org.mortbay.http.handler.AbstractHttpHandler;
import org.mortbay.log.LogFactory;
import org.mortbay.util.*;
import org.mortbay.util.URI;
import org.mortbay.html.*;

import org.lockss.app.LockssDaemon;
import org.lockss.config.*;
import org.lockss.daemon.CuUrl;
import org.lockss.plugin.*;
import org.lockss.state.AuState;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.servlet.ServletUtil;

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
public class ProxyHandler extends AbstractHttpHandler {
  private static Logger log = Logger.getLogger("ProxyHandler");
  private static Log jlog = LogFactory.getLog(ProxyHandler.class);


  static final String LOCKSS_VIA_VERSION = "1.1";
  static final String LOCKSS_VIA_COMMENT = "(LOCKSS/jetty)";
  /** a GET of this path results in an index page of all AU manifest
   * pages */
  public static String MANIFEST_INDEX_URL_PATH = "/";

  /** Force the proxy to serve only locally cached content.  Mainly useful
   * in testing. */
  static final String PARAM_NEVER_PROXY =
    Configuration.PREFIX + "proxy.neverProxy";
  static final boolean DEFAULT_NEVER_PROXY = false;

  private LockssDaemon theDaemon = null;
  private PluginManager pluginMgr = null;
  private ProxyManager proxyMgr = null;
  private LockssUrlConnectionPool connPool = null;
  private LockssUrlConnectionPool quickFailConnPool = null;
  private String hostname;
  private boolean neverProxy = DEFAULT_NEVER_PROXY;
  private boolean auditProxy = false;
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
    neverProxy = CurrentConfig.getBooleanParam(PARAM_NEVER_PROXY,
					       DEFAULT_NEVER_PROXY);
    hostname = PlatformUtil.getLocalHostname();
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

  /** If set to true, will act like an audit proxy.  (Content will be
   * served only from the cache; requests will never be proxied, will serve
   * CLOCKSS unsubscribed content */
  public void setAuditProxy(boolean flg) {
    auditProxy = flg;
    setFromCacheOnly(flg);
  }

  /** If set to true, content will be served only from the cache; requests
   * will never be proxied */
  public void setFromCacheOnly(boolean flg) {
    neverProxy = flg;
  }

  /** Set a target to use as a base (protocol, host, port) for all incoming
   * request URLs.  To be used to cause the proxy to serve locally cached
   * content in response to direct (non-proxy) GET requests. */
  public void setProxiedTarget(String target) {
    failOverTargetUri = new URI(target);
    isFailOver = true;
  }

  /** Create a Via header value:
   * 1.1 thishost:port (LOCKSS/Jetty)
   */
  String makeVia(HttpRequest req) {
    StringBuffer sb = new StringBuffer();
    sb.append(LOCKSS_VIA_VERSION);
    sb.append(" ");
    sb.append(hostname);
    try {
      int port = req.getHttpConnection().getServerPort();
      if (port != 0) {
	sb.append(":");
	sb.append(port);
      }
    } catch (Exception ignore) {
    }
    sb.append(" ");
    sb.append(LOCKSS_VIA_COMMENT);
    return sb.toString();
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
   * /
   public void setTunnelTimeoutMs(int ms) {
   _tunnelTimeoutMs = ms;
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
    URI uri = request.getURI();

    if (!proxyMgr.isMethodAllowed(request.getMethod())) {
      sendForbid(request,response,uri);
      return;
    }
      
    // Is this a CONNECT request?
    if (HttpRequest.__CONNECT.equals(request.getMethod())) {
      response.setField(HttpFields.__Connection,"close"); // XXX Needed for IE????
      handleConnect(pathInContext,pathParams,request,response);
      return;
    }

    if (log.isDebug3()) {
      log.debug3("pathInContext="+pathInContext);
      log.debug3("URI="+uri);
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
      return;
    }
    CachedUrl cu = pluginMgr.findCachedUrl(urlString);

    // Don't allow CLOCKSS to serve local content for unsubscribed AUs
    if (cu != null && theDaemon.isDetectClockssSubscription() && !auditProxy) {
      ArchivalUnit au = cu.getArchivalUnit();
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
	  (isInCache && proxyMgr.isHostDown(uri.getHost()))) {
	if (isInCache) {
	  if (isRepairRequest && log.isDebug()) {
	    log.debug("Serving repair to " + request.getRemoteAddr() + ", " + cu);
	  }
	  serveFromCache(pathInContext, pathParams, request,
			 response, cu);
	  return;
	} else {
	  // Don't forward request if it's a repair or we were told not to.
	  response.sendError(HttpResponse.__404_Not_Found);
	  request.setHandled(true);
	  return;
	}
      }
      if (!isInCache
	  && (proxyMgr.getHostDownAction() ==
	      ProxyManager.HOST_DOWN_NO_CACHE_ACTION_504)
	  && proxyMgr.isHostDown(uri.getHost())) {
	sendErrorPage(request, response, 504,
		      hostMsg("Can't connect to", uri.getHost(),
			      "Host not responding (cached status)"));
	return;
      }
      if (UrlUtil.isHttpUrl(urlString)) {
	if (HttpRequest.__GET.equals(request.getMethod())) {
	  doLockss(pathInContext, pathParams, request, response,
		   urlString, cu);
	  return;
	}
      }
      doSun(pathInContext, pathParams, request, response);
    } finally {
      AuUtil.safeRelease(cu);
    }
  }

  /** Proxy a connection using Java's native URLConection */
  void doSun(String pathInContext,
	     String pathParams,
	     HttpRequest request,
	     HttpResponse response) throws IOException {
    URI uri = request.getURI();
    try {
      // Do we proxy this?
      URL url=isProxied(uri);
      if (url==null) {
	if (isForbidden(uri))
	  sendForbid(request,response,uri);
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
      connection.setRequestProperty("Via", makeVia(request));
      connection.addRequestProperty(HttpFields.__XForwardedFor,
				    request.getRemoteAddr());
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
	InputStream in=request.getInputStream();
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
      response.addField("Via", makeVia(request));

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

  /** Proxy a connection using LockssUrlConnection */
  void doLockss(String pathInContext,
		String pathParams,
		HttpRequest request,
		HttpResponse response,
		String urlString,
		CachedUrl cu) throws IOException {

    boolean isInCache = cu != null && cu.hasContent();

    LockssUrlConnection conn = null;
    try {
      // If we recently served this url from the cache, don't check with
      // publisher for newer content.
      // XXX This needs to forward the request to the publisher (but not
      // wait for the result) so the publisher can count the access.
      if (isInCache && proxyMgr.isRecentlyAccessedUrl(urlString)) {
	serveFromCache(pathInContext, pathParams, request, response, cu);
	return;
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
	doSun(pathInContext, pathParams, request, response);
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
	      if (isEarlier(ifModified, cuLast)) {
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
      conn.addRequestProperty("Via", makeVia(request));
      conn.addRequestProperty(HttpFields.__XForwardedFor,
			      request.getRemoteAddr());

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
	}
	// if we get any error and it's in the cache, serve it from there
	if (isInCache) {
	  serveFromCache(pathInContext, pathParams, request, response, cu);
	} else {
	  // else generate an error page
	  sendProxyErrorPage(e, request, response);
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
	return;
      }

      Collection candidateAus = null;
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
	forwardResponse(request, response, conn);
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

  void forwardResponse(HttpRequest request, HttpResponse response,
		       LockssUrlConnection conn)
      throws IOException {
    // return response from server
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
    response.addField("Via", makeVia(request));

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

  boolean isEarlier(String datestr1, String datestr2)
      throws DateParseException {
    // common case, no conversion necessary
    if (datestr1.equalsIgnoreCase(datestr2)) return false;
    long d1 = DateUtil.parseDate(datestr1).getTime();
    long d2 = DateUtil.parseDate(datestr2).getTime();
    return d1 < d2;
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

    try {
      if(jlog.isDebugEnabled())jlog.debug("CONNECT: "+uri);
      InetAddrPort addrPort=new InetAddrPort(uri.toString());

      if (isForbidden(HttpMessage.__SSL_SCHEME, false)) {
	sendForbid(request,response,uri);
      } else {
	Socket socket =
	  new Socket(addrPort.getInetAddress(),addrPort.getPort());

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
	    LogSupport.ignore(jlog,e);
	  }
	}

	customizeConnection(pathInContext,pathParams,request,socket);
	request.getHttpConnection().setHttpTunnel(new HttpTunnel(socket,
								 timeoutMs));
	response.setStatus(HttpResponse.__200_OK);
	response.setContentLength(0);
	request.setHandled(true);
      }
    } catch (Exception e) {
      LogSupport.ignore(jlog,e);
      response.sendError(HttpResponse.__500_Internal_Server_Error,
			 e.getMessage());
    }
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
   * Add a Lockss-Cu: field to the request with the locksscu: url to serve
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
    request.setField("Lockss-Cu", CuUrl.fromCu(cu).toString());
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
			  HttpResponse response)
      throws IOException {
    URI uri = request.getURI();
    if (e instanceof java.net.UnknownHostException) {
      // DNS failure
      sendErrorPage(request, response, 502,
		    hostMsg("Can't connect to", uri.getHost(),
			    "Unknown host"));
      return;
    }
    if (e instanceof java.net.NoRouteToHostException) {
      sendErrorPage(request, response, 502,
		    hostMsg("Can't connect to", uri.getHost(),
			    "No route to host"));
      return;
    }
    if (e instanceof LockssUrlConnection.ConnectionTimeoutException) {
      sendErrorPage(request, response, 504,
		    hostMsg("Can't connect to", uri.getHost(),
			    "Host not responding"));
      return;
    }
    if (e instanceof java.net.ConnectException) {
      sendErrorPage(request, response, 502,
		    hostMsg("Can't connect to",
			    uri.getHost() +
			    (uri.getPort() != 80 ? (":" + uri.getPort()) : ""),
			    "Connection refused"));
      return;
    }
    if (e instanceof java.io.InterruptedIOException) {
      sendErrorPage(request, response, 504,
		    hostMsg("Timeout waiting for data from", uri.getHost(),
			    "Server not responding"));
      return;
    }
//     if (e instanceof java.io.IOException) {
    sendErrorPage(request, response, 502,
		  hostMsg("Error communicating with", uri.getHost(),
			  e.getMessage()));
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
		     int code, String msg, Collection candidateAus)
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


    response.setContentType(HttpFields.__TextHtml);
    ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer(2048);

    URI uri = request.getURI();
    String urlString = uri.toString();


    writer.write("<html>\n<head>\n<title>Error ");
    writer.write(errMsg);
    writer.write("</title>\n");

    writer.write("<body>\n");

    writer.write("<h2>Error (");
    writer.write(errMsg);
    writer.write(")</h2>");

    writer.write("<font size=+1>");
    writer.write("Unable to proxy request for URL: <b>");
    writer.write(HtmlUtil.htmlEncode(uri.toString()));
    writer.write("</b><br>");

    writer.write(msg);
    writer.write("</font>");
    writer.write("<br>");

    writeErrorAuIndex(writer, urlString, candidateAus);
    writeFooter(writer);
    writer.flush();
    response.setContentLength(writer.size());
    writer.writeTo(response.getOutputStream());
    writer.destroy();

    request.setHandled(true);
  }

  void writeErrorAuIndex(Writer writer, String urlString,
			 Collection candidateAus)
      throws IOException {
    if (true) {
      if (candidateAus != null && !candidateAus.isEmpty()) {
	writer.write("<br>This LOCKSS cache (" +
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
		 "\">LOCKSS proxy</a>, ");
    writer.write("<a href=\"http://jetty.mortbay.org/\">powered by Jetty</a>");
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
				Collection candidateAus,
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
