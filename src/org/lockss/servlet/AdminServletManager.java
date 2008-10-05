/*
 * $Id: AdminServletManager.java,v 1.4 2008-08-17 08:48:00 tlipkis Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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
import com.planetj.servlet.filter.compression.*;

/**
 * Local UI servlet starter
 */
public class AdminServletManager extends BaseServletManager {
  private static Logger log = Logger.getLogger("ServletMgr");

  /** Prefix for this server's config tree */
  public static final String PREFIX = Configuration.PREFIX + "ui.";
  /** Jetty server name */
  public static final String SERVER_NAME = "Admin";
  /** Auth realm */
  public static final String UI_REALM = "LOCKSS Admin";
  /** File holding debug user passwd */
  public static final String PASSWORD_PROPERTY_FILE = "admin.props";

  // All of the params that are defined in terms of SUFFIX_XXX are accessed
  // via the generic mechanism in BaseServletManager.setConfig().  The
  // PARAM_XXX symbols for those params are here for the ParamDoc tools;
  // some are also used by other packages to set the params referenced here
  public static final String PARAM_PORT = PREFIX + SUFFIX_PORT;
  public static final int DEFAULT_PORT = 8081;

  public static final String PARAM_START = PREFIX + SUFFIX_START;
  public static final boolean DEFAULT_START = true;

  public static final String IP_ACCESS_PREFIX =
    PREFIX + SUFFIX_IP_ACCESS_PREFIX;
  public static final String PARAM_IP_INCLUDE =
    IP_ACCESS_PREFIX + SUFFIX_IP_INCLUDE;
  public static final String PARAM_IP_PLATFORM_SUBNET =
    IP_ACCESS_PREFIX + SUFFIX_IP_PLATFORM_SUBNET;

  public static final String PARAM_LOG_FORBIDDEN =
    PREFIX + SUFFIX_LOG_FORBIDDEN;
  public static final boolean DEFAULT_LOG_FORBIDDEN = true;

  public static final String PARAM_403_MSG = PREFIX + SUFFIX_403_MSG;
  public static final String DEFAULT_403_MSG =
    "Access to the admin UI is not allowed from this IP address (%IP%)";

  public static final String PARAM_ENABLE_DEBUG_USER =
    PREFIX + SUFFIX_ENABLE_DEBUG_USER;
  public static final boolean DEFAULT_ENABLE_DEBUG_USER = true;

  /** Path to directory holding daemon logs */
  public static final String PARAM_LOGDIR =
    Configuration.PREFIX +  "platform.logdirectory";

  /** Admin UI requires user auth */
  public static final boolean DO_USER_AUTH = true;

  protected ManagerInfo getManagerInfo() {
    ManagerInfo mi = new ManagerInfo();
    mi.prefix = PREFIX;
    mi.serverName = SERVER_NAME;
    mi.defaultStart = DEFAULT_START;
    mi.defaultPort = DEFAULT_PORT;
    mi.default403Msg = DEFAULT_403_MSG;
    mi.doAuth = DO_USER_AUTH;
    mi.authRealm = UI_REALM;
    mi.defaultEnableDebugUser = DEFAULT_ENABLE_DEBUG_USER;
    mi.defaultLogForbidden = DEFAULT_LOG_FORBIDDEN;
    mi.debugUserFile = PASSWORD_PROPERTY_FILE;
    return mi;
  }

  /** Absolute path to directory in which configured platform ISO images
   * are stored */
  public static final String PARAM_ISODIR =
    Configuration.PREFIX +  "platform.isodirectory";

  public static final String PARAM_CONTACT_ADDR = PREFIX + "contactEmail";
  static final String DEFAULT_CONTACT_ADDR = "contactnotset@notset";

  public static final String PARAM_HELP_URL = PREFIX + "helpUrl";
  static final String DEFAULT_HELP_URL =
    "http://www.lockss.org/lockss/Cache_Help";

  /** If set, fetches of the UI root (http://cache:8081/) will be
   * redirected to this path (on same host and port) instead of serving the
   * index page of the root context.  This is used to replace the UI home
   * with a servlet. */
  public static final String PARAM_REDIRECT_ROOT = PREFIX + "redirectRoot";
  public static final String DEFAULT_REDIRECT_ROOT = "Home";

  /** PluginConfig servlet is visible if true.  Set for PLNs, etc. */
  static final String PARAM_ALLOW_PLUGIN_CONFIG = PREFIX + "allowPluginConfig";
  static final boolean DEFAULT_ALLOW_PLUGIN_CONFIG = false;

  static final String COMPRESSOR_PREFIX = PREFIX + "compressor.";

  public static final String PARAM_COMPRESSOR_ENABLED =
    COMPRESSOR_PREFIX + "enabled";
  public static final boolean DEFAULT_COMPRESSOR_ENABLED = true;

  /** See documentation of pjl-comp-filter's CompressingFilter for legal
      keys and values.  Defaults: <code><br>compressionThreshold =
      4096<br>includeContentTypes = text/html,text/xml,text/plain</code> */
  static final String PARAM_DOC_ONLY = COMPRESSOR_PREFIX + "<key>=<val>";

  static final Map<String,String> COMPRESSOR_DEFAULTS = new HashMap();
  static {
    COMPRESSOR_DEFAULTS.put("compressionThreshold", "4096");
    COMPRESSOR_DEFAULTS.put("includeContentTypes",
			    "text/html,text/xml,text/plain");
  }

  static final String PARAM_INFRAME_CONTENT_TYPES =
    PREFIX + "view.inFrameTypes";
  static final String DEFAULT_INFRAME_CONTENT_TYPES =
    "text;image;application/pdf";


  // Descriptors for all admin servlets.

  protected static final ServletDescr SERVLET_HOME =
    new ServletDescr("UiHome",
		     UiHome.class,
                     "Cache Administration",
		     "Home",
		     ServletDescr.LARGE_LOGO);
  protected static final ServletDescr SERVLET_BATCH_AU_CONFIG =
    new ServletDescr("BatchAuConfig",
		     BatchAuConfig.class,
                     "Journal Configuration",
                     (ServletDescr.IN_NAV | ServletDescr.IN_UIHOME
		      | ServletDescr.ADMIN_ONLY),
                     "Add or remove titles from this cache");
  protected static final ServletDescr SERVLET_AU_CONFIG =
    new ServletDescr("AuConfig",
		     AuConfig.class,
                     "Manual Journal Configuration",
		     (ServletDescr.IN_UIHOME | ServletDescr.ADMIN_ONLY),
                     "Manually edit single AU configuration");
  protected static final ServletDescr SERVLET_ADMIN_ACCESS_CONTROL =
    new ServletDescr("AdminIpAccess",
		     AdminIpAccess.class,
                     "Admin Access Control",
                     (ServletDescr.IN_NAV | ServletDescr.IN_UIHOME
		      | ServletDescr.ADMIN_ONLY),
                     "Control access to the administrative UI");
  protected static final ServletDescr SERVLET_PROXY_ACCESS_CONTROL =
    new ServletDescr("ProxyIpAccess",
		     ProxyIpAccess.class,
                     "Content Access Control",
                     (ServletDescr.IN_NAV | ServletDescr.IN_UIHOME
		      | ServletDescr.ADMIN_ONLY),
                     "Control access to the preserved content");
  protected static final ServletDescr SERVLET_PROXY_AND_CONTENT =
    new ServletDescr("ProxyAndContent",
		     ProxyAndContent.class,
                     "Content Access Options",
                     (ServletDescr.IN_NAV | ServletDescr.IN_UIHOME
		      | ServletDescr.ADMIN_ONLY),
                     "Configure the audit proxy and the ICP server.");
  protected static final ServletDescr SERVLET_PROXY_INFO =
    new ServletDescr("ProxyConfig",
		     ProxyConfig.class,
                     "Proxy Info",
                     "info/ProxyInfo",
                     ServletDescr.IN_NAV | ServletDescr.IN_UIHOME,
                     "Info for configuring browsers and proxies"
                     + "<br>"
                     + "to access preserved content on this cache");
  protected static final ServletDescr SERVLET_PLUGIN_CONFIG =
    new ServletDescr("PluginConfig",
		     PluginConfig.class,
                     "Plugin Configuration",
                     (ServletDescr.IN_NAV | ServletDescr.IN_UIHOME
		      | ServletDescr.ADMIN_ONLY),
                     "Manage plugin repositories, title databases") {
      public boolean isInNav(LockssServlet servlet) {
	return CurrentConfig.getBooleanParam(PARAM_ALLOW_PLUGIN_CONFIG,
					     DEFAULT_ALLOW_PLUGIN_CONFIG);
      }
      public boolean isInUiHome(LockssServlet servlet) {
	return isInNav(servlet);
      }};
  protected static final ServletDescr SERVLET_DAEMON_STATUS =
    new ServletDescr("DaemonStatus",
		     DaemonStatus.class,
                     "Daemon Status",
                     ServletDescr.IN_NAV | ServletDescr.IN_UIHOME,
                     "Status of cache contents and operation");
  public static final ServletDescr SERVLET_DISPLAY_CONTENT =
    new ServletDescr("ViewContent",
		     ViewContent.class,
                     "View Content");
  public static final ServletDescr SERVLET_SERVE_CONTENT =
    new ServletDescr("ServeContent",
		     ServeContent.class,
                     "Serve Content");
  public static final ServletDescr SERVLET_LIST_OBJECTS =
    new ServletDescr("ListObjects",
		     ListObjects.class,
                     "List Objests");
  protected static final ServletDescr SERVLET_HASH_CUS =
    new ServletDescr("HashCUS",
		     HashCUS.class,
                     "Hash CUS",
                     ServletDescr.IN_NAV | ServletDescr.DEBUG_ONLY);
  protected static final ServletDescr LINK_LOGS =
    new ServletDescr(null,
		     null,
                     "Logs",
                     "log",
                     ServletDescr.IN_NAV | ServletDescr.DEBUG_ONLY);
  // Link to ISOs only appears if there are actually any ISOs
  protected static final ServletDescr LINK_ISOS =
    new ServletDescr(null,
		     null,
                     "ISOs",
                     "iso",
                     (ServletDescr.IN_NAV | ServletDescr.IN_UIHOME
		      | ServletDescr.ADMIN_ONLY),
		     "Download configured platform CD image") {
      public boolean isInNav(LockssServlet servlet) {
	ServletManager mgr = servlet.getServletManager();
	return (mgr instanceof AdminServletManager) &&
	  ((AdminServletManager)mgr).hasIsoFiles();
      }
      public boolean isInUiHome(LockssServlet servlet) {
	return isInNav(servlet);
      }};
  protected static final ServletDescr SERVLET_THREAD_DUMP =
    new ServletDescr("ThreadDump",
		     ThreadDump.class,
                     "Thread Dump",
                     ServletDescr.IN_NAV | ServletDescr.DEBUG_ONLY);
  protected static final ServletDescr SERVLET_RAISE_ALERT =
    new ServletDescr("RaiseAlert",
		     RaiseAlert.class,
                     "Raise Alert",
                     ServletDescr.DEBUG_ONLY | ServletDescr.ADMIN_ONLY);
  protected static final ServletDescr SERVLET_DEBUG_PANEL =
    new ServletDescr("DebugPanel",
		     DebugPanel.class,
                     "Debug Panel",
		     ServletDescr.ADMIN_ONLY);
  protected static final ServletDescr LINK_CONTACT =
    new ServletDescr(null,
		     null,
                     "Contact Us",
		     mailtoUrl(DEFAULT_CONTACT_ADDR),
                     ServletDescr.IN_NAV | ServletDescr.PATH_IS_URL);
  protected static final ServletDescr LINK_HELP =
    new ServletDescr(null,
		     null,
                     "Help", DEFAULT_HELP_URL,
                     ServletDescr.PATH_IS_URL | ServletDescr.IN_NAV | ServletDescr.IN_UIHOME,
                     "Online help, FAQs, credits");

  static void setHelpUrl(String url) {
    LINK_HELP.path = url;
  }

  static void setContactAddr(String addr) {
    LINK_CONTACT.path = mailtoUrl(addr);
  }

  static String mailtoUrl(String addr) {
    return "mailto:" + addr;
  }

  // All servlets must be listed here (even if not in nav table).
  // Order of descrs determines order in nav table.
  static final ServletDescr servletDescrs[] = {
     SERVLET_HOME,
     SERVLET_BATCH_AU_CONFIG,
     SERVLET_AU_CONFIG,
     SERVLET_PLUGIN_CONFIG,
     SERVLET_ADMIN_ACCESS_CONTROL,
     SERVLET_PROXY_ACCESS_CONTROL,
     SERVLET_PROXY_AND_CONTENT,
     SERVLET_PROXY_INFO,
     SERVLET_DAEMON_STATUS,
     SERVLET_DISPLAY_CONTENT,
     SERVLET_SERVE_CONTENT,
     SERVLET_LIST_OBJECTS,
     SERVLET_HASH_CUS,
     LINK_LOGS,
     LINK_ISOS,
     SERVLET_THREAD_DUMP,
     SERVLET_RAISE_ALERT,
     SERVLET_DEBUG_PANEL,
     LINK_CONTACT,
     LINK_HELP,
  };

  public ServletDescr[] getServletDescrs() {
    return servletDescrs;
  }


  private String logdir;
  private String redirectRootTo = DEFAULT_REDIRECT_ROOT;
  protected String isodir;
  private LockssResourceHandler rootResourceHandler;
  private List inFrameContentTypes;
  private boolean hasIsoFiles = false;
  private boolean compressorEnabled = DEFAULT_COMPRESSOR_ENABLED;

  public AdminServletManager() {
    super(SERVER_NAME);
  }

  public void setConfig(Configuration config, Configuration prevConfig,
			Configuration.Differences changedKeys) {
    super.setConfig(config, prevConfig, changedKeys);
    isodir = config.get(PARAM_ISODIR);
    logdir = config.get(PARAM_LOGDIR);
    if (changedKeys.contains(PREFIX)) {
      if (changedKeys.contains(PARAM_REDIRECT_ROOT)) {
	redirectRootTo = config.get(PARAM_REDIRECT_ROOT,
				    DEFAULT_REDIRECT_ROOT);
	if (rootResourceHandler != null) {
	  setRedirectRootTo(rootResourceHandler,
			    (StringUtil.isNullString(redirectRootTo)
			     ? null : redirectRootTo));
	}
      }
      setContactAddr(config.get(PARAM_CONTACT_ADDR,
				DEFAULT_CONTACT_ADDR));
      setHelpUrl(config.get(PARAM_HELP_URL, DEFAULT_HELP_URL));
      compressorEnabled = config.getBoolean(PARAM_COMPRESSOR_ENABLED,
					    DEFAULT_COMPRESSOR_ENABLED);
      if (changedKeys.contains(PARAM_INFRAME_CONTENT_TYPES)) {
	inFrameContentTypes = config.getList(PARAM_INFRAME_CONTENT_TYPES);
	if (inFrameContentTypes == null || inFrameContentTypes.isEmpty()) {
	  inFrameContentTypes =
	    StringUtil.breakAt(DEFAULT_INFRAME_CONTENT_TYPES, ';', 0, true);
	}
      }
      startOrStop();
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

  protected void installUsers(MDHashUserRealm realm) {
    installDebugUser(realm);
    installPlatformUser(realm);
    installGlobalUsers(realm);
    installLocalUsers(realm);
  }

  protected void configureContexts(HttpServer server) {
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

    // Create a servlet container.  WebApplicationHandler is a
    // ServletHandler that can apply filters (e.g., compression) around
    // servlets.
    WebApplicationHandler handler = new WebApplicationHandler();
    addCompressionFilter(handler);

    // Request dump servlet
    handler.addServlet("Dump", "/Dump", "org.mortbay.servlet.Dump");

    // Add all servlets in descrs array
    addServlets(getServletDescrs(), handler);

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

  void addCompressionFilter(WebApplicationHandler handler) {
    Configuration config = ConfigManager.getCurrentConfig();
    if (config.getBoolean(PARAM_COMPRESSOR_ENABLED,
			  DEFAULT_COMPRESSOR_ENABLED)) {
      String filterName = "CompressingFilter";
      FilterHolder holder =
	handler.defineFilter(filterName, CompressingFilter.class.getName());
      // Set default compressor params unless in config
      Configuration compressorConfig = config.getConfigTree(COMPRESSOR_PREFIX);
      for (Map.Entry<String,String> ent : COMPRESSOR_DEFAULTS.entrySet()) {
	String key = ent.getKey();
	if (compressorConfig.get(key) == null) {
	  holder.put(key, ent.getValue());
	}
      }
      // Set compressor params from config
      for (Iterator iter = compressorConfig.nodeIterator(); iter.hasNext(); ) {
	String key = (String)iter.next();
	String val = compressorConfig.get(key);
	holder.put(key, val);
      }
      handler.addFilterPathMapping("/*", filterName, Dispatcher.__DEFAULT);
    }
  }

//   void setupImageContext(HttpServer server) throws MalformedURLException {
//     HttpContext context = makeContext(server, "/images");

//     // add handlers in the order they should be tried.

//     // ResourceHandler for /images dir
//     // find the htdocs directory, set as resource base
//     ClassLoader loader = Thread.currentThread().getContextClassLoader();
//     URL resourceUrl=loader.getResource("org/lockss/htdocs/images/");
//     log.debug("Images resource URL: " + resourceUrl);

//     context.setResourceBase(resourceUrl.toString());
//     LockssResourceHandler rHandler = new LockssResourceHandler(getDaemon());
//     context.addHandler(rHandler);

//     // NotFoundHandler
//     context.addHandler(new NotFoundHandler());
//   }

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

  protected void setupLogContext(HttpServer server, UserRealm realm,
				 String contextPath, String logdir)
      throws MalformedURLException {
    setupDirContext(server, realm, contextPath, logdir, null);
  }
}
