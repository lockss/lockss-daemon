/*
 * $Id: ServletManager.java,v 1.19 2003-04-22 17:59:57 tal Exp $
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

import java.io.*;
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

  public static final String UI_REALM = "LOCKSS Admin";
  public static final String PASSWORD_PROPERTY_FILE = "admin.props";

  public static final String PREFIX = Configuration.PREFIX + "ui.";
  public static final String PARAM_START = PREFIX + "start";
  public static final String PARAM_PORT = PREFIX + "port";

  public static final String IP_ACCESS_PREFIX = PREFIX + "access.ip.";
  public static final String PARAM_IP_INCLUDE = IP_ACCESS_PREFIX + "include";
  public static final String PARAM_IP_EXCLUDE = IP_ACCESS_PREFIX + "exclude";
  public static final String PARAM_LOG_FORBIDDEN =
    IP_ACCESS_PREFIX + "logForbidden";

  public static final String PARAM_USER_AUTH = PREFIX + "access.auth";

  public static final String PARAM_LOGDIR =
    Configuration.PREFIX +  "platform.logdirectory";

  public static final boolean DEFAULT_START = true;
  public static final int DEFAULT_PORT = 8081;

  private static Logger log = Logger.getLogger("ServletMgr");

  private static String textMimes[] = {
    "out", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
  };

  private HttpServer server;

  private int port;
  private boolean start;
  private String includeIps;
  private String excludeIps;
  private boolean logForbidden;
  private boolean doAuth;
  private String logdir;
  private UserRealm realm;

  List accessHandlers = new ArrayList();

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
    logdir = config.get(PARAM_LOGDIR);
    doAuth = config.getBoolean(PARAM_USER_AUTH, true);

    if (changedKeys.contains(PARAM_IP_INCLUDE) ||
	changedKeys.contains(PARAM_IP_EXCLUDE) ||
	changedKeys.contains(PARAM_LOG_FORBIDDEN)) {
      includeIps = config.get(PARAM_IP_INCLUDE, "");
      excludeIps = config.get(PARAM_IP_EXCLUDE, "");
      logForbidden = config.getBoolean(PARAM_LOG_FORBIDDEN, false);
      log.debug("Installing new ip filter: incl: " + includeIps +
		", excl: " + excludeIps);
      setIpFilters();
    }
  }

  void setIpFilters() {
    for (Iterator iter = accessHandlers.iterator(); iter.hasNext(); ) {
      setIpFilter((IpAccessHandler)iter.next());
    }
  }

  void setIpFilter(IpAccessHandler ah) {
    try {
      IpFilter filter = new IpFilter();
      filter.setFilters(includeIps, excludeIps, ';');
      ah.setFilter(filter);
    } catch (IpFilter.MalformedException e) {
      log.warning("Malformed IP filter, filters not changed", e);
    }
    ah.setLogForbidden(logForbidden);
    ah.setAllowLocal(true);
  }

  public void startServlets() {
    try {
      // Create the server
      server = new HttpServer();

      // Create a port listener
      HttpListener listener = server.addListener(new InetAddrPort(port));

      // create auth realm
      if (doAuth) {
	URL propsUrl = this.getClass().getResource(PASSWORD_PROPERTY_FILE);
	if (propsUrl != null) {
	  log.debug("passwd props file: " + propsUrl);
	  realm = new HashUserRealm(UI_REALM, propsUrl.toString());
	} else {
	  log.warning("Passwd file not found, not authenticating users.");
	}
      }

      configureDebugServlets();
//       configureAdminServlets();

      // Start the http server
      server.start ();
    } catch (Exception e) {
      log.warning("Couldn't start servlets", e);
    }
  }

  private void setContextAuthHandler(HttpContext context, UserRealm realm) {
    if (realm != null) {
      context.setRealm(realm);
      context.setAuthenticator(new BasicAuthenticator());
      context.addHandler(new SecurityHandler());
      context.addSecurityConstraint("/",
				    new SecurityConstraint("Admin", "*"));
    }
  }

  public void configureAdminServlets() {
    try {
      // Create a context
      HttpContext context = server.getContext(null, "/admin/*");

      // In this environment there is no point in consuming memory with
      // cached resources
      context.setMaxCachedFileSize(0);

      // Create a servlet container
      ServletHandler handler = new ServletHandler();

      // Admin servlet
      handler.addServlet("Admin", "/Admin", "org.lockss.servlet.Admin");

      context.addHandler(handler);

    } catch (Exception e) {
      log.error("Couldn't start debug servlets", e);
    }
  }

  public void configureDebugServlets() {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    try {
      if (true || logdir != null) {
	// Create a context
	HttpContext logContext = server.getContext("/log/");
	logContext.setAttribute("LockssDaemon", theDaemon);
	// In this environment there is no point in consuming memory with
	// cached resources
	logContext.setMaxCachedFileSize(0);

	// Now add handlers in the order they should be tried.

	// IpAccessHandler is first
	addAccessHandler(logContext);

	// then user authentication handler
	setContextAuthHandler(logContext, realm);

	// log dir resource
	String logdirname = (logdir != null) ? logdir : ".";
	URL logResourceUrl=new URL("file", null,
				   new File(logdirname).getAbsolutePath());
	log.debug("Log Resource URL: " + logResourceUrl);
	logContext.setResourceBase(logResourceUrl.toString());
	ResourceHandler logRHandler = new LockssResourceHandler();
	//       rHandler.setDirAllowed(false);
	//       rHandler.setPutAllowed(false);
	//       rHandler.setDelAllowed(false);
	//       rHandler.setAcceptRanges(true);
	logContext.addHandler(logRHandler);
	for (int ix = 0; ix < textMimes.length; ix++) {
	  logContext.setMimeMapping(textMimes[ix], "text/plain");
	}
// 	logContext.setMimeMapping("gz", "text/gzip");
// 	logContext.setTypeEncoding("text/gzip", "x-gzip");

	// NotFoundHandler
	logContext.addHandler(new NotFoundHandler());
      }

      HttpContext context = server.getContext("/");

      // Give servlets a way to find the daemon instance
      context.setAttribute("LockssDaemon", theDaemon);

      // In this environment there is no point in consuming memory with
      // cached resources
      context.setMaxCachedFileSize(0);
//       context.setErrorPage("500", "images/");
//       log.debug("Error page URL: " + context.getErrorPage("500"));
//       log.debug("Error page URL: " + context.getErrorPage());

      // Now add handlers in the order they should be tried.

      // IpAccessHandler is first
      addAccessHandler(context);

      // then user authentication handler
      setContextAuthHandler(context, realm);

      // Create a servlet container
      ServletHandler handler = new ServletHandler();

      // Request dump servlet
      handler.addServlet("Dump", "/Dump", "org.mortbay.servlet.Dump");
      // Daemon status servlet
      handler.addServlet("DaemonStatus", "/DaemonStatus",
			 "org.lockss.servlet.DaemonStatus");
      handler.addServlet("ThreadDump", "/ThreadDump",
			 "org.lockss.servlet.ThreadDump");
      context.addHandler(handler);

      // ResourceHandler should come after servlets
      // find the htdocs directory, set as resource base
      URL resourceUrl=loader.getResource("org/lockss/htdocs/");
      log.debug("Resource URL: " + resourceUrl);

      context.setResourceBase(resourceUrl.toString());
      ResourceHandler rHandler = new LockssResourceHandler();
//       rHandler.setDirAllowed(false);
//       rHandler.setPutAllowed(false);
//       rHandler.setDelAllowed(false);
//       rHandler.setAcceptRanges(true);
      context.addHandler(rHandler);

      // NotFoundHandler
      context.addHandler(new NotFoundHandler());

//       context.addHandler(new DumpHandler());


    } catch (Exception e) {
      log.warning("Couldn't start debug servlets", e);
    }
  }

  void addAccessHandler(HttpContext context) {
    IpAccessHandler ah = new IpAccessHandler("UI");
    setIpFilter(ah);
    context.addHandler(ah);
    accessHandlers.add(ah);
  }

}
