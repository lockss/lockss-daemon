/*
 * $Id: TinyUi.java,v 1.2.2.1 2004-07-19 08:29:15 tlipkis Exp $
 */

/*

Copyright (c) 2000-2004 Board of Trustees of Leland Stanford Jr. University,
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
import org.mortbay.http.*;
import org.mortbay.http.handler.*;
import org.mortbay.jetty.servlet.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.mortbay.html.*;

/**
 * Starts a tiny context running a tiny servlet that says the cache isn't
 * up, and displays an error mesage from the context.
 */
public class TinyUi extends JettyManager {
  private static Logger log = Logger.getLogger("TinyUi");


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
  public static final boolean DEFAULT_LOG_FORBIDDEN = true;

  public static final String PARAM_USER_AUTH = PREFIX + "access.auth";
  public static final boolean DEFAULT_USER_AUTH = true;

  public static final boolean DEFAULT_START = true;
  public static final int DEFAULT_PORT = 8081;

  public static final String PARAM_PLATFORM_USERNAME =
    Configuration.PLATFORM + "ui.username";
  public static final String PARAM_PLATFORM_PASSWORD =
    Configuration.PLATFORM + "ui.password";


  private HttpServer server;

  private int port = DEFAULT_PORT;
  private boolean start;
  private String includeIps;
  private String excludeIps;
  private boolean doAuth = DEFAULT_USER_AUTH;
  private HashUserRealm realm;
  private String platUser;
  private String platPass;
  private String[] tinyData;

  java.util.List accessHandlers = new ArrayList();

  public TinyUi() {
  }

  public TinyUi(String[] tinyData) {
    this.tinyData = tinyData;
  }

  /** Start servlets  */
  public void startTiny() {
    log.debug("Starting");
    Configuration config = ConfigManager.getCurrentConfig();
    setConfig(config, ConfigManager.EMPTY_CONFIGURATION, config.keySet());
    startServlets();
  }

  public void stopTiny() {
    log.debug("Stopping");
    try {
      if (server != null) {
	server.stop();
	server = null;
      }
    } catch (InterruptedException e) {
      log.warning("Interrupted while stopping server");
    }
  }

  protected void setConfig(Configuration config, Configuration prevConfig,
			   Set changedKeys) {
    super.setConfig(config, prevConfig, changedKeys);
    port = config.getInt(PARAM_PORT, DEFAULT_PORT);
    start = config.getBoolean(PARAM_START, DEFAULT_START);
    doAuth = config.getBoolean(PARAM_USER_AUTH, DEFAULT_USER_AUTH);
    platUser = config.get(PARAM_PLATFORM_USERNAME);
    platPass = config.get(PARAM_PLATFORM_PASSWORD);

    if (changedKeys.contains(PARAM_IP_INCLUDE) ||
	changedKeys.contains(PARAM_IP_EXCLUDE) ||
	changedKeys.contains(PARAM_LOG_FORBIDDEN)) {
      includeIps = config.get(PARAM_IP_INCLUDE, "");
      excludeIps = config.get(PARAM_IP_EXCLUDE, "");
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
    ah.setLogForbidden(true);
    ah.setAllowLocal(true);
  }

  // Manually install password set by platform config.
  // XXX Doesn't handle roles, will need to be integrated with daemon
  // password setting mechanism
  private void setConfiguredPasswords(HashUserRealm realm) {
    if (!StringUtil.isNullString(platUser) &&
	!StringUtil.isNullString(platPass)) {
      realm.put(platUser, platPass);
    }
  }

  public void startServlets() {
    try {
      // Create the server
      server = new HttpServer();

      // Create a port listener
      HttpListener listener =
	server.addListener(new org.mortbay.util.InetAddrPort(port));

      // create auth realm
      if (doAuth) {
	try {
	  URL propsUrl = this.getClass().getResource(PASSWORD_PROPERTY_FILE);
	  if (propsUrl != null) {
	    log.debug("passwd props file: " + propsUrl);
	    realm = new HashUserRealm(UI_REALM, propsUrl.toString());
	  }
	} catch (IOException e) {
	  log.warning("Error loading admin.props", e);
	}
	if (realm == null) {
	  realm = new HashUserRealm(UI_REALM);
	}
	setConfiguredPasswords(realm);
	if (realm.isEmpty()) {
	  log.warning("No users created, tiny UI is effectively disabled.");
	}
      }

      configureTinyContexts();

      // Start the http server
      startServer();
    } catch (Exception e) {
      log.warning("Couldn't start servlets", e);
    }
  }

  long[] delayTime = {10 * Constants.SECOND, 60 * Constants.SECOND, 0};

  boolean startServer() {
    try {
      for (int ix = 0; ix < delayTime.length; ix++) {
	try {
	  server.start ();
	  return true;
	} catch (org.mortbay.util.MultiException e) {
	  log.debug("multi", e);
	  log.debug("first", e.getException(0));
	  log.warning("Couldn't start server, will retry in " +
		      StringUtil.timeIntervalToString(delayTime[ix]));
	  Deadline.in(delayTime[ix]).sleep();
	}
      }
    } catch (Exception e) {
      log.warning("Couldn't start servlets", e);
    }
    return false;
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

  public void configureTinyContexts() {
    try {
      setupTinyContext();
      setupImageContext();

    } catch (Exception e) {
      log.warning("Couldn't start admin UI contexts", e);
    }
  }



  void setupTinyContext() throws MalformedURLException {
    HttpContext context = makeContext("/");

    // add handlers in the order they should be tried.

    // user authentication handler
    setContextAuthHandler(context, realm);

    // Create a servlet container
    ServletHandler handler = new ServletHandler();

    // Request dump servlet
    handler.addServlet("Tiny", "/", TinyServlet.class.getName());
    context.addHandler(handler);

    // NotFoundHandler
    context.addHandler(new NotFoundHandler());

    //       context.addHandler(new DumpHandler());
  }

  void setupImageContext() throws MalformedURLException {
    HttpContext context = makeContext("/images");

    // add handlers in the order they should be tried.

    // ResourceHandler for /images dir
    // find the htdocs directory, set as resource base
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    URL resourceUrl=loader.getResource("org/lockss/htdocs/images/");
    log.debug("Images resource URL: " + resourceUrl);

    context.setResourceBase(resourceUrl.toString());
    ResourceHandler rHandler = new ResourceHandler();
    rHandler.setDirAllowed(false);
    context.addHandler(rHandler);

    // NotFoundHandler
    context.addHandler(new NotFoundHandler());
  }

  // common context setup
  // adds IpAccessHandler as all contexts want it
  // doesn't add AuthHandler as not all contexts want it
  HttpContext makeContext(String path) {
    HttpContext context = server.getContext(path);
    context.setAttribute("TinyData", tinyData);
    // In this environment there is no point in consuming memory with
    // cached resources
    context.setMaxCachedFileSize(0);

    // IpAccessHandler is always first handler
//     addAccessHandler(context);
    return context;
  }

  void addAccessHandler(HttpContext context) {
    IpAccessHandler ah = new IpAccessHandler("UI");
//     setIpFilter(ah);
    context.addHandler(ah);
    accessHandlers.add(ah);
  }


  public static class TinyServlet extends HttpServlet {
    private ServletContext context;
    private String[] tinyData;

    public void init(ServletConfig config) throws ServletException {
      super.init(config);
      context = config.getServletContext();
      tinyData = (String[])context.getAttribute("TinyData");
    }

    public void doGet(HttpServletRequest request,
		      HttpServletResponse response)
	throws ServletException, IOException {
      Page page= new Page();
      page.title("LOCKSS cache");
      page.addHeader("");

      Table table = new Table(0, "cellspacing=0 cellpadding=0 align=center");
      table.newRow();
      table.newCell("valign=top align=center");
      table.add(new Link(Constants.LOCKSS_HOME_URL,
			 LockssServlet.IMAGE_LOGO_LARGE));
      table.add(LockssServlet.IMAGE_TM);

      Composite b = new Font(1, true);
      b.add("<br>This LOCKSS cache");
      Configuration pc = Configuration.getPlatformConfig();
      String name = Configuration.getPlatformHostname();
      if (name != null) {
	b.add(" (");
	b.add(name);
	b.add(")");
      }
      b.add(" has not started because ");
      b.add("it is unable to load configuration data.<br>");
      if (tinyData[0] != null) {
	b.add(tinyData[0]);
      }
      table.newRow();
      table.newCell("valign=top align=left");
      table.add(b);
      page.add(table);
      response.setContentType("text/html");
      response.setHeader("Pragma", "no-cache");
      response.setHeader("Cache-Control", "no-cache,no-store");
      Writer writer=response.getWriter();
      page.write(writer);
      writer.flush();
    }
  }
}
