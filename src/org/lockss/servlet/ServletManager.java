/*
 * $Id: ServletManager.java,v 1.7 2003-04-04 08:41:06 tal Exp $
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

import java.net.*;
import java.util.*;
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.jetty.*;
import org.mortbay.util.*;
import org.mortbay.http.*;
import org.mortbay.http.handler.*;
import org.mortbay.jetty.servlet.*;

/**
 * Servlet starter
 */
public class ServletManager extends JettyManager {

  public static final String PREFIX = Configuration.PREFIX + "ui.";
  public static final String PARAM_START = PREFIX + "start";
  public static final String PARAM_PORT = PREFIX + "port";

  public static final boolean DEFAULT_START = true;
  public static final int DEFAULT_PORT = 8081;

  private static Logger log = Logger.getLogger("ServletMgr");

  private HttpServer server;

  private int port;
  private boolean start;

  public ServletManager() {
  }

  /* ------- LockssManager implementation ------------------ */

  /** Start servlets  */
  public void startService() {
    if (start) {
      super.startService();
      startServlets();
    }
  }

  /** Stop servlets  */
  public void stopService() {
    try {
      if (server != null) {
	server.stop();
      }
    } catch (InterruptedException e) {
      log.warning("Interrupted while stopping server");
    }
    super.stopService();
  }

  protected void setConfig(Configuration config, Configuration prevConfig,
			   Set changedKeys) {
    super.setConfig(config, prevConfig, changedKeys);
    port = config.getInt(PARAM_PORT, DEFAULT_PORT);
    start = config.getBoolean(PARAM_START, DEFAULT_START);
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
      log.warning("Couldn't start servlets", e);
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
//       context.setErrorPage("500", "images/");
//       log.debug("Error page URL: " + context.getErrorPage("500"));
//       log.debug("Error page URL: " + context.getErrorPage());
      // Give servlets a way to find the daemon instance
      context.setAttribute("LockssDaemon", theDaemon);

      // find the htdocs directory, set as resource base
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      URL resourceUrl=loader.getResource("org/lockss/htdocs/");
      log.debug("Resource URL: " + resourceUrl);

      context.setResourceBase(resourceUrl.toString());
      context.addHandler(new ResourceHandler());

      // Create a servlet container
      ServletHandler handler = new ServletHandler();

      // Request dump servlet
      handler.addServlet("Dump", "/Dump", "org.mortbay.servlet.Dump");
      // Daemon status servlet
      handler.addServlet("DaemonStatus", "/DaemonStatus",
			 "org.lockss.servlet.DaemonStatus");

      context.addHttpHandler(handler);
      context.addHttpHandler(new NotFoundHandler());
//       context.addHttpHandler(new DumpHandler());


    } catch (Exception e) {
      log.warning("Couldn't start debug servlets", e);
    }
  }
}
