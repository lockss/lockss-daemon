/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.servlet;

import java.io.*;
import java.net.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
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
 * up, and displays an error mesage from the context.  This is run before
 * the application is running, so must not rely on any services.
 */
public class TinyUi extends BaseServletManager {
  private static Logger log = Logger.getLogger("TinyUi");

  public static final String PREFIX = Configuration.PREFIX + "tinyUi.";
  public static final String SERVER_NAME = "TinyUI";

  public static final boolean DEFAULT_START = true;
  public static final int DEFAULT_PORT = 8081;
  public static final boolean DO_USER_AUTH = true;

  private String[] tinyData;

  public TinyUi() {
    super(SERVER_NAME);
  }

  public TinyUi(String[] tinyData) {
    this();
    this.tinyData = tinyData;
  }

  /** Entry point to start tiny UI without general daemon startup  */
  public void startTiny() {
    log.debug("Starting");
    Configuration config = ConfigManager.getCurrentConfig();
    setConfig(config, ConfigManager.EMPTY_CONFIGURATION,
	      config.differences(null));  // all differences
    isInited = true;
    startServlets();
  }

  public void stopTiny() {
    log.debug("Stopping");
    stopServer();
  }

  protected ManagerInfo getManagerInfo() {
    ManagerInfo mi = new ManagerInfo();
    mi.prefix = PREFIX;
    mi.serverName = SERVER_NAME;
    mi.defaultStart = DEFAULT_START;
    mi.defaultPort = DEFAULT_PORT;
    mi.doAuth = DO_USER_AUTH;
    mi.doFilterIpAccess = false;
    mi.authRealm = AdminServletManager.UI_REALM;
    mi.defaultLogForbidden = AdminServletManager.DEFAULT_ENABLE_DEBUG_USER;
    mi.defaultEnableDebugUser = true;
    mi.debugUserFile = AdminServletManager.PASSWORD_PROPERTY_FILE;
    return mi;
  }

  void setIpFilter(IpAccessHandler ah) {
    super.setIpFilter(ah);
    ah.setLogForbidden(true);
  }

  public ServletDescr[] getServletDescrs() {
    return new ServletDescr[0];
  }

  public ServletDescr findServletDescr(Object o) {
    return null;
  }

  @Override
  protected UserRealm newUserRealm() {
    return new MDHashUserRealm(mi.authRealm);
  }

  private MDHashUserRealm getRealm() {
    return (MDHashUserRealm)realm;
  }

  protected void installUsers() {
    installDebugUser();
    installPlatformUser();
//     installLocalUsers();
  }

  @Override
  protected void installDebugUser() {
    if (enableDebugUser) {
      try {
	log.debug("passwd props file: " + mi.debugUserFile);
 	URL propsUrl = this.getClass().getResource(mi.debugUserFile);
 	if (propsUrl != null) {
 	  log.debug("passwd props file: " + propsUrl);
	  getRealm().load(propsUrl.toString());
 	}
       } catch (IOException e) {
 	log.warning("Error loading " + mi.debugUserFile, e);
      }
    }
  }

  @Override
  // Manually install password set by platform config.
  protected void installPlatformUser() {
    // Use platform config as real config hasn't been loaded yet
    Configuration platConfig = ConfigManager.getPlatformConfig();
    String platUser = platConfig.get(PARAM_PLATFORM_USERNAME);
    String platPass = platConfig.get(PARAM_PLATFORM_PASSWORD);

    if (!StringUtil.isNullString(platUser) &&
	!StringUtil.isNullString(platPass)) {
      getRealm().put(platUser, platPass);
      getRealm().addUserToRole(platUser, LockssServlet.ROLE_USER_ADMIN);
    }
  }

  protected void configureContexts(HttpServer server) {
    try {
      setupTinyContext(server);
      setupImageContext(server);
    } catch (Exception e) {
      log.warning("Couldn't start tiny UI contexts", e);
    }
  }

  void setupTinyContext(HttpServer server) throws MalformedURLException {
    HttpContext context = makeContext(server, "/");

    // add handlers in the order they should be tried.

    // user authentication handler
    setContextAuthHandler(context, realm);

    // Add a servlet handler for TinyServlet
    ServletHandler handler = new ServletHandler();
    handler.addServlet("Tiny", "/", TinyServlet.class.getName());
    context.addHandler(handler);

    // NotFoundHandler
    context.addHandler(new NotFoundHandler());

    //       context.addHandler(new DumpHandler());
  }

  void setupImageContext(HttpServer server) throws MalformedURLException {
    HttpContext context = makeContext(server, "/images");

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
  HttpContext makeContext(HttpServer server, String path) {
    HttpContext context = super.makeContext(server, path);
    context.setAttribute("TinyData", tinyData);
    return context;
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
      page.title("LOCKSS box");
      page.addHeader("");

      Table table = new Table(0, "cellspacing=0 cellpadding=0 align=center");
      table.newRow();
      table.newCell("valign=top align=center");
      table.add(new Link(Constants.LOCKSS_HOME_URL,
			 ServletUtil.IMAGE_LOGO_LARGE));

      Composite b = new Font(1, true);
      b.add("<br>This LOCKSS box");
      String name = PlatformUtil.getLocalHostname();
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
