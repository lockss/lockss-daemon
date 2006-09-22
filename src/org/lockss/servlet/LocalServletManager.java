/*
 * $Id: LocalServletManager.java,v 1.20 2006-09-22 06:26:09 tlipkis Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.jetty.*;
import org.mortbay.http.*;
import org.mortbay.http.handler.*;
import org.mortbay.jetty.servlet.*;

/**
 * Local UI servlet starter
 */
public class LocalServletManager extends BaseServletManager {
  public static final String SERVER_NAME = "LocalUI";
  private static Logger log = Logger.getLogger("ServletMgr");

  static final String PREFIX = Configuration.PREFIX + "admin.";

  /** Absolute path to directory in which configured platform ISO images
   * are stored */
  public static final String PARAM_ISODIR =
    Configuration.PREFIX +  "platform.isodirectory";

  static final String PARAM_CONTACT_ADDR = PREFIX + "contactEmail";
  static final String DEFAULT_CONTACT_ADDR = "contactnotset@notset";

  static final String PARAM_HELP_URL = PREFIX + "helpUrl";
  static final String DEFAULT_HELP_URL =
    "http://www.lockss.org/lockss/Cache_Help";

  /** If set, fetches of the UI root (http://cache:8081/) will be
   * redirected to this path (on same host and port) instead of serving the
   * index page of the root context.  This is used to replace the UI home
   * with a servlet. */
  public static final String PARAM_REDIRECT_ROOT = PREFIX + "redirectRoot";
  public static final String DEFAULT_REDIRECT_ROOT = null;

  static final String PARAM_INFRAME_CONTENT_TYPES =
    PREFIX + "view.inFrameTypes";
  static final String DEFAULT_INFRAME_CONTENT_TYPES =
    "text;image;application/pdf";


  private String redirectRootTo = DEFAULT_REDIRECT_ROOT;
  protected String isodir;
  private LockssResourceHandler rootResourceHandler;
  private List inFrameContentTypes;
  private boolean hasIsoFiles = false;

  public LocalServletManager() {
    super(SERVER_NAME);
  }

  public void setConfig(Configuration config, Configuration prevConfig,
			Configuration.Differences changedKeys) {
    super.setConfig(config, prevConfig, changedKeys);
    isodir = config.get(PARAM_ISODIR);
    if (changedKeys.contains(PARAM_REDIRECT_ROOT)) {
      redirectRootTo = config.get(PARAM_REDIRECT_ROOT, DEFAULT_REDIRECT_ROOT);
      if (rootResourceHandler != null) {
	setRedirectRootTo(rootResourceHandler,
			  (StringUtil.isNullString(redirectRootTo)
			   ? null : redirectRootTo));
      }
    }
    if (changedKeys.contains(PREFIX)) {
      LockssServlet.setContactAddr(config.get(PARAM_CONTACT_ADDR,
					      DEFAULT_CONTACT_ADDR));
      LockssServlet.setHelpUrl(config.get(PARAM_HELP_URL, DEFAULT_HELP_URL));

      if (changedKeys.contains(PARAM_INFRAME_CONTENT_TYPES)) {
	inFrameContentTypes = config.getList(PARAM_INFRAME_CONTENT_TYPES);
	if (inFrameContentTypes == null || inFrameContentTypes.isEmpty()) {
	  inFrameContentTypes =
	    StringUtil.breakAt(DEFAULT_INFRAME_CONTENT_TYPES, ';', 0, true);
	}
      }
    }
  }

  private void setRedirectRootTo(LockssResourceHandler rh, String redTo) {
    rootResourceHandler.setRedirectRootTo(StringUtil.isNullString(redTo)
					  ? null : redTo);
  }

  List inFrameContentTypes() {
    return inFrameContentTypes;
  }

  /** Return true iff there are any platform ISO files to point to */
  public boolean hasIsoFiles() {
    return hasIsoFiles;
  }

  public void startServlets() {
    try {
      // Create the server
      HttpServer server = new HttpServer();

      // Create a port listener
      server.addListener(new org.mortbay.util.InetAddrPort(port));

      setupAuthRealm();

      configureAdminContexts(server);

      // Start the http server
      startServer(server, port);
    } catch (Exception e) {
      log.warning("Couldn't start servlets", e);
    }
  }

  public void configureAdminContexts(HttpServer server) {
    try {
      if (true || logdir != null) {
	// Create context for serving log files and directory
	setupLogContext(server, realm, "/log/", logdir);
      }
      if (isodir != null) {
	// Create context for serving ISO files and directory
	FilenameFilter filt = new FileExtensionFilter(".iso");
	setupDirContext(server, realm, "/iso/", isodir, filt);
	String[] isofiles = new File(isodir).list(filt);
	if (isofiles != null) {
	  log.debug("isofiles: " + ListUtil.fromArray(isofiles));
	  hasIsoFiles = isofiles.length != 0;
	}
      }
      // info currently has same auth as /, but could be different
      setupInfoContext(server);

      setupAdminContext(server);

      // no separate image context for now.  (Use if want different
      // access control or auth from / context
      // setupImageContext(server);

    } catch (Exception e) {
      log.warning("Couldn't create admin UI contexts", e);
    }
  }

  void setupAdminContext(HttpServer server) throws MalformedURLException {
    HttpContext context = makeContext(server, "/");

    // add handlers in the order they should be tried.

    // user authentication handler
    setContextAuthHandler(context, realm);

    // Create a servlet container
    ServletHandler handler = new ServletHandler();

    // Request dump servlet
    handler.addServlet("Dump", "/Dump", "org.mortbay.servlet.Dump");

    handler.addServlet("Home", "/Home",
		       "org.lockss.servlet.UiHome");
    handler.addServlet("BatchAuConfig", "/BatchAuConfig",
		       "org.lockss.servlet.BatchAuConfig");
    handler.addServlet("JournalConfig", "/AuConfig",
		       "org.lockss.servlet.AuConfig");
    handler.addServlet("DaemonStatus", "/DaemonStatus",
		       "org.lockss.servlet.DaemonStatus");
    handler.addServlet("ViewContent", "/ViewContent",
		       "org.lockss.servlet.ViewContent");
    handler.addServlet("AdminIpAccess", "/AdminIpAccess",
		       "org.lockss.servlet.AdminIpAccess");
    handler.addServlet("ProxyIpAccess", "/ProxyIpAccess",
		       "org.lockss.servlet.ProxyIpAccess");
    handler.addServlet("ProxyAndContent", "/ProxyAndContent",
                       "org.lockss.servlet.ProxyAndContent");
    handler.addServlet("Hash CUS", "/HashCUS",
		       "org.lockss.servlet.HashCUS");
    handler.addServlet("Raise Alert", "/RaiseAlert",
		       "org.lockss.servlet.RaiseAlert");
    handler.addServlet("Debug Panel", "/DebugPanel",
		       "org.lockss.servlet.DebugPanel");
    handler.addServlet("ThreadDump", "/ThreadDump",
		       "org.lockss.servlet.ThreadDump");
    addServletIfAvailable(handler, "Api", "/Api",
			  "org.lockss.uiapi.servlet.Api");
    context.addHandler(handler);

    // ResourceHandler should come after servlets
    // find the htdocs directory, set as resource base
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    URL resourceUrl=loader.getResource("org/lockss/htdocs/");
    log.debug("Resource URL: " + resourceUrl);

    context.setResourceBase(resourceUrl.toString());
    rootResourceHandler = new LockssResourceHandler(getDaemon());
    rootResourceHandler.setDirAllowed(false);
    setRedirectRootTo(rootResourceHandler, redirectRootTo);
    //       rHandler.setAcceptRanges(true);
    context.addHandler(rootResourceHandler);

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
    LockssResourceHandler rHandler = new LockssResourceHandler(getDaemon());
    context.addHandler(rHandler);

    // NotFoundHandler
    context.addHandler(new NotFoundHandler());
  }

  void setupInfoContext(HttpServer server) {
    HttpContext context = makeContext(server, "/info");

    // add handlers in the order they should be tried.

    // user authentication handler
    setContextAuthHandler(context, realm);

    // Create a servlet container
    ServletHandler handler = new ServletHandler();
    handler.addServlet("ProxyInfo", "/ProxyInfo",
		       "org.lockss.servlet.ProxyConfig");
    context.addHandler(handler);

    // NotFoundHandler
    context.addHandler(new NotFoundHandler());
  }

  // common context setup
  // adds IpAccessHandler as all contexts want it
  // doesn't add AuthHandler as not all contexts want it
  HttpContext makeContext(HttpServer server, String path) {
    HttpContext context = server.getContext(path);
    context.setAttribute("LockssApp", theApp);
    // In this environment there is no point in consuming memory with
    // cached resources
    context.setMaxCachedFileSize(0);

    // IpAccessHandler is always first handler
    addAccessHandler(context);
    return context;
  }

}
