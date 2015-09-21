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

import java.util.*;

import org.lockss.util.*;
import org.lockss.config.Configuration;

/** Audit proxy manager, starts a proxy that serves only content from the
 * cache, useful for auditing the content.
 */
public class AuditProxyManager extends BaseProxyManager {
  public static final String SERVER_NAME = "AuditProxy";
  private static Logger log = Logger.getLogger("AuditProxy");

  public static final String PREFIX = Configuration.PREFIX + "proxy.audit.";
  /** Start audit proxy if true. */
  public static final String PARAM_START = PREFIX + "start";
  public static final boolean DEFAULT_START = false;

  /** If true, audit proxy will generate a manifest index along with a 404
   * error, just like the normal proxy does */
  public static final String PARAM_INDEX = PREFIX + "index";
  public static final boolean DEFAULT_INDEX = false;

  /** Audit proxy listen port */
  public static final String PARAM_PORT = PREFIX + "port";

  /** List of IP addresses to which to bind listen socket.  If not set,
   * server listens on all interfaces.  All listeners must be on the same
   * port, given by the <tt>port</tt> parameter.  Change requires
   * proxy restart. */
  public static final String PARAM_BIND_ADDRS = PREFIX + "bindAddrs";

  /** Audit proxy SSL listen port */
  public static final String PARAM_SSL_PORT = PREFIX + "sslPort";
  public static final int DEFAULT_SSL_PORT = -1;

  /** List of IP addresses to which to bind the SSL listen socket.  If not
   * set, server listens on all interfaces.  All listeners must be on the
   * same port, given by the <tt>sslPort</tt> parameter.  Change requires
   * proxy restart. */
  public static final String PARAM_SSL_BIND_ADDRS = PREFIX + "sslBindAddrs";
  public static final List DEFAULT_SSL_BIND_ADDRS = ListUtil.list("127.0.0.1");

  /** Host that CONNECT requests should connect to, in lieu of the one
   * specified in the request.  Intended to be pointed to the AuditProxy's
   * SSL listener. */
  static final String PARAM_CONNECT_ADDR = PREFIX + "connectAddr";
  static final String DEFAULT_CONNECT_ADDR = "127.0.0.1";

  /** Name of managed keystore to use (see
   * org.lockss.keyMgr.keystore.<i>id</i>.name) */
  public static final String PARAM_SSL_KEYSTORE_NAME =
    PREFIX + "sslKeystoreName";

  protected boolean auditIndex = DEFAULT_INDEX;

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
      start = config.getBoolean(PARAM_START, DEFAULT_START);
      auditIndex = config.getBoolean(PARAM_INDEX, DEFAULT_INDEX);
      sslPort = config.getInt(PARAM_SSL_PORT, DEFAULT_SSL_PORT);
      bindAddrs = config.getList(PARAM_BIND_ADDRS, Collections.EMPTY_LIST);
      sslBindAddrs = config.getList(PARAM_SSL_BIND_ADDRS,
				    DEFAULT_SSL_BIND_ADDRS);

      sslKeystoreName = config.get(PARAM_SSL_KEYSTORE_NAME);
      setConnectAddr(config.get(PARAM_CONNECT_ADDR, DEFAULT_CONNECT_ADDR),
		     sslPort);

      if (start) {
	if (getDaemon().isDaemonRunning()) {
	  startProxy();
	}
      } else if (isServerRunning()) {
	stopProxy();
      }
    }
  }

  // Proxy handler for auditing doesn't make outgoing connections, doesn't
  // need connection pool
  protected org.lockss.proxy.ProxyHandler makeProxyHandler() {
    org.lockss.proxy.ProxyHandler handler =
      new org.lockss.proxy.ProxyHandler(getDaemon());
    handler.setAuditProxy(true);
    handler.setAuditIndex(auditIndex);
    handler.setErrorTemplate(errorTemplate);
    handler.setConnectAddr(connectHost, connectPort);
    if (sslPort > 0) {
      handler.setSslListenPort(sslPort);
    }
    if (tunnelTimeout >= 0) {
      handler.setTunnelTimeoutMs(tunnelTimeout);
    }
    return handler;
  }
}
