/*
 * $Id$
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

package org.lockss.proxy;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.ssl.KeyManagerFactory;

import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.util.StringUtil;
import org.lockss.util.urlconn.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.jetty.*;
import org.lockss.alert.AlertManager;
import org.mortbay.util.*;
import org.mortbay.http.*;
import org.mortbay.http.handler.*;

/** Abstract base class for LOCKSS proxy managers.
 */
public abstract class BaseProxyManager extends JettyManager {

  private static Logger log = Logger.getLogger("BaseProxy");

  public static final String PARAM_403_MSG =
    Configuration.PREFIX + "proxy.403Msg";
  public static final String DEFAULT_403_MSG =
    "Access to content in this LOCKSS box is not allowed from your IP address (%IP%)";

  /** Filename of template for proxy's 404 error page */
  public static final String PARAM_ERROR_TEMPLATE =
    Configuration.PREFIX + "proxy.errorTemplate";
  static final String DEFAULT_ERROR_TEMPLATE = "errorpagetemplate.html";

  /** Read timeout for tunnel (CONNECT) socket */
  public static final String PARAM_TUNNEL_TIMEOUT =
    Configuration.PREFIX + "proxy.tunnelTimeout";
  static final long DEFAULT_TUNNEL_TIMEOUT = 30 * Constants.MINUTE;

  protected AlertManager alertMgr;
  protected int port;
  protected int sslPort = -1;
  protected String sslKeystoreName;
  protected boolean start = false;
  protected String includeIps;
  protected String excludeIps;
  protected boolean logForbidden;
  protected IpAccessHandler accessHandler;
  protected ProxyHandler handler;
  protected LockssKeyStoreManager keystoreMgr;
  private String _403Msg;
  protected List<String> bindAddrs;
  protected List<String> sslBindAddrs;
  protected String connectHost;
  protected int connectPort;
  protected String errorTemplate;
  protected long tunnelTimeout = DEFAULT_TUNNEL_TIMEOUT;

  /* ------- LockssManager implementation ------------------ */
  /**
   * start the proxy.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    super.startService();
    alertMgr = getDaemon().getAlertManager();
    keystoreMgr = getDaemon().getKeystoreManager();
    if (start) {
      startProxy();
    }
  }

  /**
   * stop the plugin manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    stopProxy();
    super.stopService();
  }

  protected LockssDaemon getDaemon() {
    return (LockssDaemon)getApp();
  }

  public void setConfig(Configuration config, Configuration prevConfig,
			Configuration.Differences changedKeys) {
    super.setConfig(config, prevConfig, changedKeys);
    _403Msg = config.get(PARAM_403_MSG, DEFAULT_403_MSG);
    if (changedKeys.contains(PARAM_ERROR_TEMPLATE)) {
      setErrorTemplate(config.get(PARAM_ERROR_TEMPLATE,
				  DEFAULT_ERROR_TEMPLATE));
    }
    if (changedKeys.contains(PARAM_TUNNEL_TIMEOUT)) {
      tunnelTimeout = config.getTimeInterval(PARAM_TUNNEL_TIMEOUT,
					     DEFAULT_TUNNEL_TIMEOUT);
      if (handler != null) {
	handler.setTunnelTimeoutMs(tunnelTimeout);
      }
    }
  }

  protected void setConnectAddr(String host, int port) {
    connectHost = host;
    connectPort = port;
  }

  void setIpFilter() {
    if (accessHandler != null) {
      try {
	IpFilter filter = new IpFilter();
	filter.setFilters(includeIps, excludeIps);
	accessHandler.setFilter(filter);
      } catch (IpFilter.MalformedException e) {
	log.warning("Malformed IP filter, filters not changed", e);
      }
      accessHandler.setLogForbidden(logForbidden);
      accessHandler.setAllowLocal(true);
      accessHandler.set403Msg(_403Msg);
    }
  }

  void setErrorTemplate(String fileOrResource) {
    String tmpl = null;
    try {
      tmpl = readErrorTemplate(fileOrResource);
    } catch (IOException e) {
      log.warning("Error reading error template", e);
    }
    if (tmpl == null && !fileOrResource.equals(DEFAULT_ERROR_TEMPLATE)) {
      try {
	tmpl = readErrorTemplate(DEFAULT_ERROR_TEMPLATE);
      } catch (IOException e) {
	log.warning("Error reading error template", e);
      }
    }
    if (tmpl == null) {
      tmpl = "Missing error template";
    }
    errorTemplate = tmpl;
    if (handler != null) {
      handler.setErrorTemplate(tmpl);
    }
  }

  String readErrorTemplate(String fileOrResource) throws IOException {
    File file = new File(fileOrResource);
    if (file.exists()) {
      return StringUtil.fromInputStream(new FileInputStream(file));
    }
    InputStream is = getClass().getResourceAsStream(DEFAULT_ERROR_TEMPLATE);
    if (is != null) {
      try {
	return StringUtil.fromInputStream(is);
      } finally {
	IOUtil.safeClose(is);
      }
    }
    return null;
  }

  protected void addListeners(HttpServer server) {
    KeyManagerFactory kmf = null;
    if (sslPort > 0) {
      if (sslKeystoreName == null) {
	log.error("No keystore configured for service " + 
		  getServerName() + ", not starting SSL server");
      } else {
	kmf = keystoreMgr.getKeyManagerFactory(sslKeystoreName);
	if (kmf == null) {
	  log.error("Keystore " + sslKeystoreName +
		    " not found, not starting " +
		    getServerName() + " SSL server");
	}
      }
    }

    if (bindAddrs.isEmpty()) {
      try {
	addListener(server, null, port);
      } catch (UnknownHostException e) {
	log.critical("UnknownHostException with null host, not starting "
		     + getServerName() + " server");
      }
    } else {
      for (String host : bindAddrs) {
	try {
	  addListener(server, host, port);
	} catch (UnknownHostException e) {
	  log.critical("Bind addr " + host +
		       " not found, " + getServerName() +
		       " not listening on that address");
	}
      }
    }
    if (sslPort > 0) {
      if (sslBindAddrs.isEmpty()) {
	try {
	  addSslListener(server, null, sslPort, kmf);
	} catch (UnknownHostException e) {
	  log.critical("UnknownHostException with null host, not starting "
		       + getServerName() + " SSL server");
	}
      } else {
	for (String host : sslBindAddrs) {
	  try {
	    addSslListener(server, host, sslPort, kmf);
	  } catch (UnknownHostException e) {
	    log.critical("Bind addr " + host +
			 " not found, " + getServerName() +
			 " not listening (SSL) on that address");
	  }
	}
      }
    }
  }

  protected void addListener(HttpServer server,
			     String host, int port)
      throws UnknownHostException {
    HttpListener listener =
      new SocketListener(new org.mortbay.util.InetAddrPort(host,port));
    server.addListener(listener);
  }

  protected void addSslListener(HttpServer server,
				String host, int port,
				KeyManagerFactory kmf)
      throws UnknownHostException {
    if (kmf == null) {
      throw new IllegalArgumentException("KeyManagerFactory must not be null");
    }
    LockssSslListener lsl =
      new LockssSslListener(new org.mortbay.util.InetAddrPort(host, port));
    lsl.setKeyManagerFactory(kmf);
    server.addListener(lsl);
  }

  /** Start a Jetty handler for the proxy.  May be called redundantly, or
   * to change ports.  */
  protected void startProxy() {
    log.debug("StartProxy");
    if (isRunningOnPort(port)) {
      return;
    }
    if (isServerRunning()) {
      stopProxy();
    }
    try {
      // Create the server
      HttpServer server = new HttpServer();

      addListeners(server);

      // Create a context
      HttpContext context = server.getContext(null, "/");

      context.setAttribute(HttpContext.__ErrorHandler,
			   new LockssErrorHandler("proxy"));

      // In this environment there is no point in consuming memory with
      // cached resources
      context.setMaxCachedFileSize(0);

      // ProxyAccessHandler must be first
      accessHandler = new ProxyAccessHandler(getDaemon(), "Proxy");
      setIpFilter();
      context.addHandler(accessHandler);

      // Add a proxy handler to the context
      handler = makeProxyHandler();
      context.addHandler(handler);

      // Add a CuResourceHandler to handle requests for locally cached
      // content that the proxy handler modified and passed on.
      context.setBaseResource(new CuUrlResource());
      LockssResourceHandler rHandler = new CuResourceHandler(getDaemon());
      rHandler.setDirAllowed(false);
//       rHandler.setAcceptRanges(true);
      context.addHandler(rHandler);
      // Requests shouldn't get this far, so dump them
      context.addHandler(new org.mortbay.http.handler.DumpHandler());

      // Start the http server
      startServer(server, port, sslPort);
    } catch (Exception e) {
      log.error("Couldn't start proxy", e);
    }
  }

  protected void stopProxy() {
    stopServer();
  }

  protected abstract org.lockss.proxy.ProxyHandler makeProxyHandler();
}
