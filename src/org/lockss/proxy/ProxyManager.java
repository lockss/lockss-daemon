/*
 * $Id: ProxyManager.java,v 1.9 2003-04-17 21:47:30 tal Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.jetty.*;
import org.mortbay.util.*;
import org.mortbay.http.*;
import org.mortbay.http.handler.*;

/* ------------------------------------------------------------ */
/** LOCKSS proxy manager.
 */
public class ProxyManager extends JettyManager {

  private static Logger log = Logger.getLogger("Proxy");
  public static final String PREFIX = Configuration.PREFIX + "proxy.";
  public static final String PARAM_START = PREFIX + "start";
  public static final String PARAM_PORT = PREFIX + "port";

  public static final String IP_ACCESS_PREFIX = PREFIX + "access.ip.";
  public static final String PARAM_IP_INCLUDE = IP_ACCESS_PREFIX + "include";
  public static final String PARAM_IP_EXCLUDE = IP_ACCESS_PREFIX + "exclude";
  public static final String PARAM_LOG_FORBIDDEN =
    IP_ACCESS_PREFIX + "logForbidden";

  public static final String PARAM_PLATFORM_ACCESS_SUBNET =
    Configuration.PARAM_PLATFORM_ACCESS_SUBNET;

  private int port;
  private boolean start;
  private String includeIps;
  private String excludeIps;
  private boolean logForbidden;
  private IpAccessHandler accessHandler;

  /* ------- LockssManager implementation ------------------ */
  /**
   * start the proxy.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    if (start) {
      super.startService();
      startProxy();
    }
  }

  /**
   * stop the plugin manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    // XXX undo whatever we did in start proxy.

    super.stopService();
  }

  protected void setConfig(Configuration config, Configuration prevConfig,
			   Set changedKeys) {
    super.setConfig(config, prevConfig, changedKeys);
    port = config.getInt(PARAM_PORT, 9090);
    start = config.getBoolean(PARAM_START, true);
    if (changedKeys.contains(PARAM_IP_INCLUDE) ||
	changedKeys.contains(PARAM_IP_EXCLUDE) ||
	changedKeys.contains(PARAM_PLATFORM_ACCESS_SUBNET) ||
	changedKeys.contains(PARAM_LOG_FORBIDDEN)) {
      includeIps = config.get(PARAM_IP_INCLUDE,
			      config.get(PARAM_PLATFORM_ACCESS_SUBNET, ""));
      excludeIps = config.get(PARAM_IP_EXCLUDE, "");
      logForbidden = config.getBoolean(PARAM_LOG_FORBIDDEN, false);
      log.debug("Installing new ip filter: incl: " + includeIps +
		", excl: " + excludeIps);
      setIpFilter();
    }
  }

  void setIpFilter() {
    if (accessHandler != null) {
      try {
	IpFilter filter = new IpFilter();
	filter.setFilters(includeIps, excludeIps, ';');
	accessHandler.setFilter(filter);
      } catch (IpFilter.MalformedException e) {
	log.warning("Malformed IP filter, filters not changed", e);
      }
      accessHandler.setLogForbidden(logForbidden);
      accessHandler.setAllowLocal(true);
    }
  }

  /** Start a Jetty handler for the proxy */
  public void startProxy() {
    try {
      // Create the server
      HttpServer server = new HttpServer();

      // Create a port listener
      HttpListener listener = server.addListener(new InetAddrPort(port));

      // Create a context
      HttpContext context = server.getContext(null, "/");

      // IpAccessHandler is first
      accessHandler = new IpAccessHandler("Proxy");
      setIpFilter();
      context.addHandler(accessHandler);

      // Add a proxy handler to the context
      HttpHandler handler = new org.lockss.proxy.ProxyHandler(getDaemon());
      context.addHandler(handler);

      // Add a CuResourceHandler to handle requests for locally cached
      // content that the proxy handler modified and passed on.
      context.setBaseResource(new CuUrlResource());
      ResourceHandler rHandler = new CuResourceHandler();
//       rHandler.setDirAllowed(false);
//       rHandler.setPutAllowed(false);
//       rHandler.setDelAllowed(false);
//       rHandler.setAcceptRanges(true);
      context.addHandler(rHandler);
      // Requests shouldn't get this far, so dump them
      context.addHandler(new org.mortbay.http.handler.DumpHandler());

      // Start the http server
      server.start ();
    } catch (Exception e) {
      log.error("Couldn't start proxy", e);
    }
  }
}
