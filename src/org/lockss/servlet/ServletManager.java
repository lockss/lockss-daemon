/*
 * $Id: ServletManager.java,v 1.3 2003-03-14 22:00:10 tal Exp $
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

package org.lockss.servlet;

import java.util.*;
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.jetty.*;
import org.mortbay.util.*;
import org.mortbay.http.*;
import org.mortbay.jetty.servlet.*;

/**
 * Servlet starter
 */
public class ServletManager extends JettyManager {

  public static final String PREFIX = Configuration.PREFIX + "ui.";
  public static final String PARAM_START = PREFIX + "start";
  public static final String PARAM_PORT = PREFIX + "port";

  private static Logger log = Logger.getLogger("ServletMgr");

  private static ServletManager theManager = null;
  private static LockssDaemon theDaemon = null;

  private Map servlets = new HashMap();
  private HttpServer server;

  private int port;
  private boolean start;

  public ServletManager() {
  }

  /* ------- LockssManager implementation ------------------ */
  /**
   * init the servlet manager.
   * @param daemon the LockssDaemon instance
   * @throws LockssDaemonException if we already instantiated this manager
   * @see LockssManager#initService(LockssDaemon)
   */
  public void initService(LockssDaemon daemon) throws LockssDaemonException {
//     super.initService();
    if (theManager == null) {
      theDaemon = daemon;
      theManager = this;
    } else {
      throw new LockssDaemonException("Multiple Instantiation.");
    }
  }

  /**
   * start the servlet manager.
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
      startServlets();
    }
  }

  /**
   * stop the servlet manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    // tk - checkpoint if nec.
    try {
      server.stop();
    } catch (InterruptedException e) {
      log.info("Interrupted while stopping server");
    }
    theManager = null;
  }

  private void setConfig(Configuration config, Configuration oldConfig) {
    port = config.getInt(PARAM_PORT, 8081);
    start = config.getBoolean(PARAM_START, true);
  }

  public void startServlets() {
    try {
      // Create the server
      server = new HttpServer();

      // Create a port listener
      HttpListener listener = server.addListener(new InetAddrPort(port));

      configureDebugServlets();
//       configureAdminServlets();

      // Start the http server
      server.start ();
    } catch (Exception e) {
      log.error("Couldn't start servlets", e);
    }
  }

  public void configureAdminServlets() {
    try {
      // Create a context
      HttpContext context = server.getContext(null, "/admin/*");

      // Create a servlet container
      ServletHandler handler = new ServletHandler();

      // Admin servlet
      handler.addServlet("Admin", "/Admin", "org.lockss.servlet.Admin");

      context.addHttpHandler(handler);

    } catch (Exception e) {
      log.error("Couldn't start debug servlets", e);
    }
  }

  public void configureDebugServlets() {
    try {
      // Create a context
      HttpContext context = server.getContext(null, "/");
      context.setAttribute("LockssDaemon", theDaemon);

      // Create a servlet container
      ServletHandler handler = new ServletHandler();

      // Request dump servlet
      handler.addServlet("Dump", "/Dump", "org.mortbay.servlet.Dump");
      // Daemon status servlet
      handler.addServlet("DaemonStatus", "/DaemonStatus",
			 "org.lockss.servlet.DaemonStatus");

      context.addHttpHandler(handler);

    } catch (Exception e) {
      log.error("Couldn't start debug servlets", e);
    }
  }
}
