/*
 * $Id: ProxyHandler.java,v 1.21 2004-02-27 04:28:49 tlipkis Exp $
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
// Portions are:
// ========================================================================
// Copyright (c) 2003 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id: ProxyHandler.java,v 1.21 2004-02-27 04:28:49 tlipkis Exp $
// ========================================================================

package org.lockss.proxy;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import org.mortbay.http.*;
import org.mortbay.http.handler.*;
import org.mortbay.util.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/* ------------------------------------------------------------ */
/** Proxy request handler.
 * A HTTP/1.1 Proxy.  This implementation uses the JVMs URL implementation to
 * make proxy requests.
 * <P>The HttpTunnel mechanism is also used to implement the CONNECT method.
 * 
 * @version $Id: ProxyHandler.java,v 1.21 2004-02-27 04:28:49 tlipkis Exp $
 * @author Greg Wilkins (gregw)
 */
public class ProxyHandler extends AbstractHttpHandler
{
  private static Logger log = Logger.getLogger("Proxy");
  private LockssDaemon theDaemon = null;
  private PluginManager pluginMgr = null;
  private LockssUrlConnectionPool connPool = null;

  ProxyHandler(LockssDaemon daemon) {
    theDaemon = daemon;
    pluginMgr = theDaemon.getPluginManager();
  }

  ProxyHandler(LockssDaemon daemon, LockssUrlConnectionPool pool) {
    this(daemon);
    this.connPool = pool;
  }

    protected Set _proxyHostsWhiteList;
    protected Set _proxyHostsBlackList;
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
    /** Set of allowed CONNECT ports.
     */
    protected HashSet _allowedConnectPorts = new HashSet();
    {
        _allowedConnectPorts.add(new Integer(80));
        _allowedConnectPorts.add(new Integer(8000));
        _allowedConnectPorts.add(new Integer(8080));
        _allowedConnectPorts.add(new Integer(8888));
        _allowedConnectPorts.add(new Integer(443));
        _allowedConnectPorts.add(new Integer(8443));
    }

    /* ------------------------------------------------------------ */
    /** Get proxy host white list.
     * @return Array of hostnames and IPs that are proxied,
     * or an empty array if all hosts are proxied.
     */
    public String[] getProxyHostsWhiteList()
    {
        if (_proxyHostsWhiteList==null||_proxyHostsWhiteList.size()==0)
            return new String[0];
        
        String [] hosts = new String[_proxyHostsWhiteList.size()];
        hosts=(String[])_proxyHostsWhiteList.toArray(hosts);
        return hosts;
    }
    
    /* ------------------------------------------------------------ */
    /** Set proxy host white list.
     * @param hosts Array of hostnames and IPs that are proxied, 
     * or null if all hosts are proxied.
     */
    public void setProxyHostsWhiteList(String[] hosts)
    {
        if (hosts==null || hosts.length==0)
            _proxyHostsWhiteList=null;
        else
        {
            _proxyHostsWhiteList=new HashSet();
            for (int i=0;i<hosts.length;i++)
                if (hosts[i]!=null && hosts[i].trim().length()>0)
                    _proxyHostsWhiteList.add(hosts[i]);
        }
    }

    /* ------------------------------------------------------------ */
    /** Get proxy host black list.
     * @return Array of hostnames and IPs that are NOT proxied.
     */
    public String[] getProxyHostsBlackList()
    {
        if (_proxyHostsBlackList==null||_proxyHostsBlackList.size()==0)
            return new String[0];
        
        String [] hosts = new String[_proxyHostsBlackList.size()];
        hosts=(String[])_proxyHostsBlackList.toArray(hosts);
        return hosts;
    }
    
    /* ------------------------------------------------------------ */
    /** Set proxy host black list.
     * @param hosts Array of hostnames and IPs that are NOT proxied. 
     */
    public void setProxyHostsBlackList(String[] hosts)
    {
        if (hosts==null || hosts.length==0)
            _proxyHostsBlackList=null;
        else
        {
            _proxyHostsBlackList=new HashSet();
            for (int i=0;i<hosts.length;i++)
                if (hosts[i]!=null && hosts[i].trim().length()>0)
                    _proxyHostsBlackList.add(hosts[i]);
        }
    }


    
    /* ------------------------------------------------------------ */
    public int getTunnelTimeoutMs()
    {
        return _tunnelTimeoutMs;
    }
    
    /* ------------------------------------------------------------ */
    /** Tunnel timeout.
     * IE on win2000 has connections issues with normal timeout handling.
     * This timeout should be set to a low value that will expire to allow IE to
     * see the end of the tunnel connection.
     * /
    public void setTunnelTimeoutMs(int ms)
    {
        _tunnelTimeoutMs = ms;
    }
    
    /* ------------------------------------------------------------ */
  public void handle(String pathInContext,
		     String pathParams,
		     HttpRequest request,
		     HttpResponse response)
      throws HttpException, IOException {
    URI uri = request.getURI();
        
    // Is this a CONNECT request?
    if (HttpRequest.__CONNECT.equalsIgnoreCase(request.getMethod())) {
      response.setField(HttpFields.__Connection,"close"); // XXX Needed for IE????
      handleConnect(pathInContext,pathParams,request,response);
      return;
    }
        
    if (log.isDebug3()) {
      log.debug3("pathInContext="+pathInContext);
      log.debug3("URI="+uri);
    }

    String urlString = uri.toString();
    CachedUrl cu = pluginMgr.findMostRecentCachedUrl(urlString);
    if (log.isDebug2()) {
      log.debug2("cu: " + cu);
    }
    boolean isRepairRequest =
      org.lockss.util.StringUtil.equalStrings(request.getField("user-agent"),
					      LockssDaemon.getUserAgent());
    if (cu != null && cu.hasContent()) {
      serveFromCache(pathInContext, pathParams, request,
		     response, cu);
      return;
    }

    if (isRepairRequest) {
      // This should never happen, as it should have been caught by the
      // ProcyAccessHandler.  But we never want to forward repair request
      // from another LOCKSS cache, so we check here just to make sure.
      response.sendError(HttpResponse.__404_Not_Found);
      request.setHandled(true);
      return; 
    }

    if (UrlUtil.isHttpUrl(urlString)) {
      if ("get".equalsIgnoreCase(request.getMethod())) {
	doLockss(pathInContext, pathParams, request, response, urlString);
	return;
      }
    }
    doSun(pathInContext, pathParams, request, response);
  }

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
            
      Code.debug("PROXY URL=",url);

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
      boolean xForwardedFor=false;
      boolean hasContent=false;
      Enumeration enum = request.getFieldNames();

      while (enum.hasMoreElements()) {
	// XXX could be better than this!
	String hdr=(String)enum.nextElement();

	if (_DontProxyHeaders.containsKey(hdr))
	  continue;

	if (connectionHdr!=null && connectionHdr.indexOf(hdr)>=0)
	  continue;

	if (HttpFields.__ContentType.equals(hdr))
	  hasContent=true;

	xForwardedFor |=
	  HttpFields.__XForwardedFor.equalsIgnoreCase(hdr);

	Enumeration vals = request.getFieldValues(hdr);
	while (vals.hasMoreElements()) {
	  String val = (String)vals.nextElement();
	  if (val!=null) {
//                         connection.addRequestProperty(hdr,val);
		  connection.setRequestProperty(hdr, val);
	  }
	}
      }

      // Proxy headers
      connection.setRequestProperty("Via","1.1 (LOCKSS/jetty)");
      if (!xForwardedFor) {
//                 connection.addRequestProperty(HttpFields.__XForwardedFor,
//                                               request.getRemoteAddr());
	connection.setRequestProperty(HttpFields.__XForwardedFor,
				      request.getRemoteAddr());

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
	InputStream in=request.getInputStream();
	if (hasContent) {
	  connection.setDoOutput(true);
	  IO.copy(in,connection.getOutputStream());
	}
                
	// Connect
	connection.connect();    
      } catch (Exception e) {
	Code.ignore(e);
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
	  Code.ignore(e);
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
      response.setField("Via","1.1 (LOCKSS/jetty)");

      // Handled
      request.setHandled(true);
      if (proxy_in!=null)
	IO.copy(proxy_in,response.getOutputStream());
            
    } catch (Exception e) {
      Code.warning(e.toString());
      Code.ignore(e);
      if (!response.isCommitted())
	response.sendError(HttpResponse.__400_Bad_Request);
    }
  }

  void doLockss(String pathInContext,
		String pathParams,
		HttpRequest request,
		HttpResponse response,
		String urlString) throws IOException {

    try {
      LockssUrlConnection conn =
	UrlUtil.openConnection(LockssUrlConnection.METHOD_PROXY,
			       urlString, connPool);

      // XXX
      conn.setFollowRedirects(false);

      // check connection header
      String connectionHdr = request.getField(HttpFields.__Connection);
      if (connectionHdr!=null &&
	  (connectionHdr.equalsIgnoreCase(HttpFields.__KeepAlive)||
	   connectionHdr.equalsIgnoreCase(HttpFields.__Close)))
	connectionHdr=null;

      // copy request headers into new request
      boolean xForwardedFor=false;
      boolean hasContent=false;

      for (Enumeration enum = request.getFieldNames();
	   enum.hasMoreElements(); ) {
	String hdr=(String)enum.nextElement();

	if (_DontProxyHeaders.containsKey(hdr)) continue;

	if (connectionHdr!=null && connectionHdr.indexOf(hdr)>=0) continue;

	if (HttpFields.__ContentType.equals(hdr)) hasContent=true;

	xForwardedFor |= HttpFields.__XForwardedFor.equalsIgnoreCase(hdr);

	//       if (canServeLocally)
	if (HttpFields.__IfModifiedSince.equalsIgnoreCase(hdr)) {
	}

	Enumeration vals = request.getFieldValues(hdr);
	while (vals.hasMoreElements()) {
	  String val = (String)vals.nextElement();
	  if (val!=null) {
	    conn.addRequestProperty(hdr, val);
	  }
	}
      }

      // Proxy-specifix headers
      conn.setRequestProperty("Via","1.1 (LOCKSS/jetty)");
      if (!xForwardedFor) {
	conn.setRequestProperty(HttpFields.__XForwardedFor,
				request.getRemoteAddr());

      }
//       // a little bit of cache control
//       String cache_control = request.getField(HttpFields.__CacheControl);
//       if (cache_control!=null &&
// 	  (cache_control.indexOf("no-cache")>=0 ||
// 	   cache_control.indexOf("no-store")>=0))
// 	connection.setUseCaches(false);

//       // customize Connection
//       customizeConnection(pathInContext,pathParams,request,connection);

// 	connection.setDoInput(true);
//      // do input thang!
// 	InputStream in=request.getInputStream();
// 	if (hasContent) {
// 	  connection.setDoOutput(true);
// 	  IO.copy(in,connection.getOutputStream());
// 	}

//    httpclient way
//     if (method instanceof EntityEnclosingMethod) {
//       EntityEnclosingMethod emethod = (EntityEnclosingMethod) method;
//       emethod.setRequestBody(conn.getInputStream());
//     }
                
      // Send the request

      int code=HttpResponse.__500_Internal_Server_Error;
      conn.execute();

      InputStream proxy_in = null;

      // handle status codes etc.
                
// 	proxy_in = http.getErrorStream();
//       if (proxy_in==null) {
// 	try {proxy_in=connection.getInputStream();}
// 	catch (Exception e) {
// 	  Code.ignore(e);
// 	  proxy_in = http.getErrorStream();
// 	}
//       }

      code=conn.getResponseCode();
      response.setStatus(code);
      response.setReason(conn.getResponseMessage());

      proxy_in = conn.getResponseInputStream();
            
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
      response.setField("Via","1.1 (LOCKSS/jetty)");

      // Handled
      request.setHandled(true);
      if (proxy_in!=null) {
	IO.copy(proxy_in,response.getOutputStream());
      }            
    } catch (Exception e) {
      log.error("doLockss error:", e);
      if (!response.isCommitted())
	response.sendError(HttpResponse.__400_Bad_Request);
    }
  }
    
    /* ------------------------------------------------------------ */
    public void handleConnect(String pathInContext,
                              String pathParams,
                              HttpRequest request,
                              HttpResponse response)
        throws HttpException, IOException
    {
        URI uri = request.getURI();
        
        try
        {
            Code.debug("CONNECT: ",uri);
            InetAddrPort addrPort=new InetAddrPort(uri.toString());

            if (isForbidden(HttpMessage.__SSL_SCHEME,addrPort.getHost(),addrPort.getPort(),false))
            {
                sendForbid(request,response,uri);
            }
            else
            {
                Socket socket = new Socket(addrPort.getInetAddress(),addrPort.getPort());

                // XXX - need to setup semi-busy loop for IE.
                int timeoutMs=30000;
		if (_tunnelTimeoutMs > 0)
                {
                    socket.setSoTimeout(_tunnelTimeoutMs);
		    Object maybesocket = request.getHttpConnection().getConnection();
		    try
                    {
			Socket s = (Socket) maybesocket;
                        timeoutMs=s.getSoTimeout();
			s.setSoTimeout(_tunnelTimeoutMs);
		    }
                    catch (Exception e)
                    {
			Code.ignore(e);
		    }
		}
                
                customizeConnection(pathInContext,pathParams,request,socket);
                request.getHttpConnection().setHttpTunnel(new HttpTunnel(socket,timeoutMs));
                response.setStatus(HttpResponse.__200_OK);
                response.setContentLength(0);
                request.setHandled(true);
            }
        }
        catch (Exception e)
        {
            Code.ignore(e);
            response.sendError(HttpResponse.__500_Internal_Server_Error);
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
        throws IOException
    {
    }
    
        
    /* ------------------------------------------------------------ */
    /** Customize proxy URL connection.
     * Method to allow derived handlers to customize the connection.
     */
    protected void customizeConnection(String pathInContext,
                                       String pathParams,
                                       HttpRequest request,
                                       URLConnection connection)
        throws IOException
    {
    }
    
    
    /* ------------------------------------------------------------ */
    /** Is URL Proxied.
     * Method to allow derived handlers to select which URIs are proxied and
     * to where.
     * @param uri The requested URI, which should include a scheme, host and port.
     * @return The URL to proxy to, or null if the passed URI should not be proxied.
     * The default implementation returns the passed uri if isForbidden() returns true.
     */
    protected URL isProxied(URI uri)
        throws MalformedURLException
    {
        // Is this a proxy request?
        if (isForbidden(uri))
            return null;
        
        // OK return URI as untransformed URL.
        return new URL(uri.toString());
    }
    

    /* ------------------------------------------------------------ */
    /** Is URL Forbidden.
     * 
     * @return True if the URL is not forbidden. Calls isForbidden(scheme,host,port,true);
     */
    protected boolean isForbidden(URI uri)
    {
        String scheme=uri.getScheme();
        String host=uri.getHost();
        int port = uri.getPort();
        return isForbidden(scheme,host,port,true);
    }
    

    /* ------------------------------------------------------------ */
    /** Is scheme,host & port Forbidden.
     *
     * @param scheme A scheme that mast be in the proxySchemes StringMap.
     * @param host A host that must pass the white and black lists
     * @param port A port that must in the allowedConnectPorts Set
     * @param openNonPrivPorts If true ports greater than 1024 are allowed.
     * @return  True if the request to the scheme,host and port is not forbidden.
     */
    protected boolean isForbidden(String scheme,
                                  String host,
                                  int port,
                                  boolean openNonPrivPorts)
    {
        // Check port
        Integer p = new Integer(port);
        if (port >0 && !_allowedConnectPorts.contains(p))
        {
            if (!openNonPrivPorts || port<=1024)
                return true;
        }

        // Must be a scheme that can be proxied.
        if (scheme==null || !_ProxySchemes.containsKey(scheme))
            return true;

        // Must be in any defined white list
        if (_proxyHostsWhiteList!=null && !_proxyHostsWhiteList.contains(host))
            return true;

        // Must not be in any defined black list
        if (_proxyHostsBlackList!=null && _proxyHostsBlackList.contains(host))
            return true;

        return false;
    }
    
    /* ------------------------------------------------------------ */
    /** Send Forbidden.
     * Method called to send forbidden response. Default implementation calls
     * sendError(403)
     */
    protected void sendForbid(HttpRequest request, HttpResponse response, URI uri)
        throws IOException
    {
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
    response.setField("X-LOCKSS", "from-cache");
    if (log.isDebug2()) {
      log.debug2("serveFromCache(" + request + ")");
    }
    // Return without handling the request, next in chain is
    // LockssResourceHandler.  (There must be a better way to do this.)
  }
}
