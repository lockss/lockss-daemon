/*
 * $Id: FailOverProxyManager.java,v 1.2 2004-10-18 03:38:12 tlipkis Exp $
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

package org.lockss.proxy;

import java.util.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.jetty.*;
//import org.mortbay.util.*;
import org.mortbay.http.*;
import org.mortbay.http.handler.*;

/** FailOver proxy serves content for direct (non-proxied) requests.  It
 * doesn't look like a proxy to the user, but it effectively is one, with
 * the proxied-for host and port explicitly configured.
 */
public class FailOverProxyManager extends BaseProxyManager {
  public static final String SERVER_NAME = "FailOverProxy";
  private static Logger log = Logger.getLogger("FailOverProxy");
  public static final String PREFIX = Configuration.PREFIX + "proxy.failover.";
  public static final String PARAM_START = PREFIX + "start";
  public static final boolean DEFAULT_START = false;

  public static final String PARAM_TARGET = PREFIX + "target";
  public static final String PARAM_PORT = PREFIX + "port";

  private String target;

  protected String getServerName() {
    return SERVER_NAME;
  }

  public void setConfig(Configuration config, Configuration prevConfig,
			Configuration.Differences changedKeys) {
    super.setConfig(config, prevConfig, changedKeys);
    if (changedKeys.contains(ProxyManager.PREFIX)) {
      includeIps = config.get(ProxyManager.PARAM_IP_INCLUDE, "");
      excludeIps = config.get(ProxyManager.PARAM_IP_EXCLUDE, "");
      logForbidden = config.getBoolean(ProxyManager.PARAM_LOG_FORBIDDEN,
				       ProxyManager.DEFAULT_LOG_FORBIDDEN);
      log.debug("Installing new ip filter: incl: " + includeIps +
		", excl: " + excludeIps);
      setIpFilter();
    }
    if (changedKeys.contains(PREFIX)) {
      port = config.getInt(PARAM_PORT, -1);
      target = config.get(PARAM_TARGET);
      start = config.getBoolean(PARAM_START, DEFAULT_START);
      if (start) {
	if (getDaemon().isDaemonRunning()) {
	  startProxy();
	}
      } else if (isServerRunning()) {
	stopProxy();
      }
    }
  }

  // Make a ProxyHandler for failover requests: give it proxied host and
  // port.  No need for a connection pool
  protected org.lockss.proxy.ProxyHandler makeProxyHandler() {
    org.lockss.proxy.ProxyHandler handler =
      new org.lockss.proxy.ProxyHandler(getDaemon());
    handler.setProxiedTarget(target);
    handler.setFromCacheOnly(true);
    return handler;
  }
}
