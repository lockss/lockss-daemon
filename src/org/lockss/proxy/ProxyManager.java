/*
 * $Id: ProxyManager.java,v 1.2 2003-03-14 22:00:10 tal Exp $
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
import org.lockss.daemon.*;
import org.lockss.jetty.*;
import org.mortbay.util.*;
import org.mortbay.http.*;

/* ------------------------------------------------------------ */
/** LOCKSS proxy manager.
 */
public class ProxyManager extends JettyManager {

  public static final String PREFIX = Configuration.PREFIX + "proxy.";
  public static final String PARAM_START = PREFIX + "start";
  public static final String PARAM_PORT = PREFIX + "port";

  private static LockssDaemon theDaemon = null;
  private static ProxyManager theManager = null;

  private int port;
  private boolean start;

  /* ------- LockssManager implementation ------------------ */
  /**
   * init the plugin manager.
   * @param daemon the LockssDaemon instance
   * @throws LockssDaemonException if we already instantiated this manager
   * @see org.lockss.app.LockssManager#initService(LockssDaemon daemon)
   */
  public void initService(LockssDaemon daemon) throws LockssDaemonException {
    if(theManager == null) {
      theDaemon = daemon;
      theManager = this;
    }
    else {
      throw new LockssDaemonException("Multiple Instantiation.");
    }
  }

  /**
   * start the proxy.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    Configuration.registerConfigurationCallback(new Configuration.Callback() {
	public void configurationChanged(Configuration oldConfig,
					 Configuration newConfig,
					 Set changedKeys) {
	  setConfig(newConfig, oldConfig);
	}
      });
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

    theManager = null;
  }

  private void setConfig(Configuration config, Configuration oldConfig) {
    port = config.getInt(PARAM_PORT, 9090);
    start = config.getBoolean(PARAM_START, true);
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

      // Create a servlet container
      HttpHandler handler = new ProxyHandler(theDaemon);
      context.addHandler(handler);

      // Start the http server
      server.start ();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
