/*
 * $Id: AuditProxyManager.java,v 1.9.34.1 2009-11-03 23:44:52 edwardsb1 Exp $
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

import org.lockss.util.*;
import org.lockss.config.Configuration;

/** Audit proxy manager, starts a proxy that serves only content from the
 * cache, useful for auditing the content.
 */
public class AuditProxyManager extends BaseProxyManager {
  public static final String SERVER_NAME = "AuditProxy";
  private static Logger log = Logger.getLogger("AuditProxy");

  public static final String PREFIX = Configuration.PREFIX + "proxy.audit.";
  public static final String PARAM_START = PREFIX + "start";
  public static final boolean DEFAULT_START = false;

  public static final String PARAM_INDEX = PREFIX + "index";
  public static final boolean DEFAULT_INDEX = false;

  public static final String PARAM_PORT = PREFIX + "port";

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
    return handler;
  }
}
