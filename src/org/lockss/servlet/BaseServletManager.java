/*
 * $Id$
 */

/*

Copyright (c) 2004-2014 Board of Trustees of Leland Stanford Jr. University,
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
import javax.net.ssl.KeyManagerFactory;

import javax.servlet.*;
import javax.servlet.http.*;

import org.lockss.app.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.account.*;
import org.lockss.jetty.*;
import org.mortbay.http.*;
import org.mortbay.http.Authenticator;
import org.mortbay.http.BasicAuthenticator;
import org.mortbay.http.handler.*;
import org.mortbay.jetty.*;
import org.mortbay.jetty.servlet.*;

/**
 * Base class for servlet managers.
 * Note: this class may be used in an environment where the LOCKSS app is
 * not running (<i>e.g.</i>, for {@link org.lockss.servlet.TinyUi}), so it
 * must not rely on any non-static app services, nor any other managers.
 */
public abstract class BaseServletManager
  extends JettyManager implements ServletManager {

  static {
    // Suppress the Jetty version from being advertised in the response header
    System.setProperty("org.mortbay.http.Version.paranoid", "true");
  }

  private static Logger log = Logger.getLogger("ServletMgr");

  // Suffixes of config keys below org.lockss.<servlet>.  Config is
  // accessed through a generic mechanism in setConfig().  The PARAM_XXX
  // symbols following some of the suffixes are present only to generate
  // appropriate parameter documentation; they aren't used by the daemon.

  /** Prefix of doc-only parameters */
  public static final String DOC_PREFIX = Configuration.PREFIX + "<server>.";

  /** Enable new but unreleased user interface features for testing.
   * This is for testing and feedback only.*/
  public static final String PARAM_ENABLE_NEW_UI = Configuration.PREFIX + "ui.new";
  public static final boolean DEFAULT_ENABLE_NEW_UI = false;
  /** Enable the transitional version of the user interface. This will enable
   * new but unreleased UI features alongside current features.
   * This is for testing and feedback only. */
  public static final String PARAM_ENABLE_TRANSITIONAL_UI = Configuration.PREFIX + "ui.transitional";
  public static final boolean DEFAULT_ENABLE_TRANSITIONAL_UI = false;

  public static final String SUFFIX_START = "start";
  /** Start the named server */
  public static final String PARAM_START = DOC_PREFIX + SUFFIX_START;

  public static final String SUFFIX_PORT = "port";
  /** Listen port for named server */
  public static final String PARAM_PORT = DOC_PREFIX + SUFFIX_PORT;

  public static final String SUFFIX_BIND_ADDRS = "bindAddrs";
  /** List of IP addresses to which to bind listen socket.  If not set,
   * server listens on all interfaces.  All listeners must be on the same
   * port, given by the <tt>port</tt> parameter.  Changing this requires
   * daemon restart. */
  public static final String PARAM_BIND_ADDRS = DOC_PREFIX + SUFFIX_BIND_ADDRS;

  public static final String SUFFIX_TCP_KEEPALIVE = "keepAlive";
  /** Enable TCP keepalive if true. */
  public static final String PARAM_TCP_KEEPALIVE =
    DOC_PREFIX + SUFFIX_TCP_KEEPALIVE;
  public static final boolean DEFAULT_TCP_KEEPALIVE = false;

  // IP access list tree below org.lockss.<server>.access.ip
  public static final String SUFFIX_IP_ACCESS_PREFIX = "access.ip.";
  public static final String DOC_ACCESS_PREFIX =
    DOC_PREFIX + SUFFIX_IP_ACCESS_PREFIX;

  public static final String SUFFIX_IP_INCLUDE = "include";
  /** List of IPs or subnets to allow */
  public static final String PARAM_IP_INCLUDE =
    DOC_ACCESS_PREFIX + SUFFIX_IP_INCLUDE;

  public static final String SUFFIX_IP_EXCLUDE = "exclude";
  /** List of IPs or subnets to reject */
  public static final String PARAM_IP_EXCLUDE =
    DOC_ACCESS_PREFIX + SUFFIX_IP_EXCLUDE;

  public static final String SUFFIX_LOG_FORBIDDEN = "logForbidden";
  /** Log accesses from forbidden IP addresses */
  public static final String PARAM_LOG_FORBIDDEN =
    DOC_ACCESS_PREFIX + SUFFIX_LOG_FORBIDDEN;

//   public static final String SUFFIX_USER_AUTH = "access.auth";
//   /** Require user authentication for named server */
//   public static final String PARAM_USER_AUTH = DOC_PREFIX + SUFFIX_USER_AUTH;

  public static final String SUFFIX_USE_SSL = "useSsl";
  /** Connect to named server with https if true */
  public static final String PARAM_USE_SSL =
    DOC_PREFIX + SUFFIX_USE_SSL;
  public static final boolean DEFAULT_USE_SSL = false;

  public static final String SUFFIX_SSL_KEYSTORE_NAME = "sslKeystoreName";
  /** Name of managed keystore to use (see
   * org.lockss.keyMgr.keystore.<i>id</i>.name) */
  public static final String PARAM_SSL_KEYSTORE_NAME =
    DOC_PREFIX + SUFFIX_SSL_KEYSTORE_NAME;

  public static final String SUFFIX_SSL_REDIR_FROM = "sslRedirFromPort";
  /** HTTP Redirector to HTTPS */
  public static final String PARAM_SSL_REDIR_FROM =
    DOC_PREFIX + SUFFIX_SSL_REDIR_FROM;

  public static final String SUFFIX_DISABLE_SSL_SERVER_PROTOCOLS =
    "disableSslServerProtocols";
  /** SSL protocols to disable in HTTPS server sockets. */
  public static final String PARAM_DISABLE_SSL_SERVER_PROTOCOLS =
    DOC_PREFIX + SUFFIX_DISABLE_SSL_SERVER_PROTOCOLS;
  public static final List DEFAULT_DISABLE_SSL_SERVER_PROTOCOLS =
    ListUtil.list("SSLv3", "SSLv2Hello");

  public static final String SUFFIX_AUTH_TYPE = "authType";
  /** User authentication type: Basic or Form */
  public static final String PARAM_AUTH_TYPE = DOC_PREFIX + SUFFIX_AUTH_TYPE;
  public static final AuthType DEFAULT_AUTH_TYPE = AuthType.Basic;

  public static final String SUFFIX_FORM_LOGIN_URL = "formLoginUrl";
  /** Login page URL for Form authentication */
  public static final String PARAM_FORM_LOGIN_URL =
    DOC_PREFIX + SUFFIX_FORM_LOGIN_URL;
  public static final String DEFAULT_FORM_LOGIN_URL = "/LoginForm";

  public static final String SUFFIX_FORM_LOGIN_ERROR_URL = "formLoginErrorUrl";
  /** Login error page URL for Form authentication */
  public static final String PARAM_FORM_LOGIN_ERROR_URL =
    DOC_PREFIX + SUFFIX_FORM_LOGIN_ERROR_URL;
  public static final String DEFAULT_FORM_LOGIN_ERROR_URL =
    "/LoginForm?error=true";

  public static final String SUFFIX_MAX_LOGIN_INACTIVITY = "maxLoginInactivity";
  /** Interval after which inactive user must re-login (used only if no
      per-account inactivity timer) */
  public static final String PARAM_MAX_LOGIN_INACTIVITY =
    DOC_PREFIX + SUFFIX_MAX_LOGIN_INACTIVITY;
  public static long DEFAULT_MAX_LOGIN_INACTIVITY = -1;

  public static final String SUFFIX_RESOLVE_REMOTE_HOST = "resolveRemoteHost";
  /** True if should attempt to resolve remote host (request source IP) */
  public static final String PARAM_RESOLVE_REMOTE_HOST =
    DOC_PREFIX + SUFFIX_RESOLVE_REMOTE_HOST;

  public static final String SUFFIX_403_MSG = "403Msg";
  /** Message to include in 403 response */
  public static final String PARAM_403MSG = DOC_PREFIX + SUFFIX_403_MSG;

  public static final String SUFFIX_WARNING = "warning";
  /** String to display in red on ui pages. */
  public static final String PARAM_WARNING = DOC_PREFIX + SUFFIX_WARNING;

  public static final String SUFFIX_ENABLE_DEBUG_USER = "debugUser.enable";
  /** Enable the debug user on named server.  Daemon restart required. */
  public static final String PARAM_ENABLE_DEBUG_USER =
    DOC_PREFIX + SUFFIX_ENABLE_DEBUG_USER;

  // User login tree below org.lockss.<server>.users
  public static final String SUFFIX_USERS = "users";
  public static final String DOC_USERS_PREFIX =
    DOC_PREFIX + SUFFIX_USERS + ".";

  public static final String USER_PARAM_USER = "user";
  /** Username */
  public static final String PARAM_USER_PARAM_USER =
    DOC_USERS_PREFIX + USER_PARAM_USER;
  public static final String USER_PARAM_PWD = "password";
  /** Encrypted password */
  public static final String PARAM_USER_PARAM_PWD =
    DOC_USERS_PREFIX + USER_PARAM_PWD;
  public static final String USER_PARAM_ROLES = "roles";
  /** List of roles (Debug, Admin) */
  public static final String PARAM_USER_PARAM_ROLES =
    DOC_USERS_PREFIX + USER_PARAM_ROLES;


  public enum AuthType {Basic, Form}

  private static String textMimes[] = {
    "out", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
  };

  private static String[][] mimeMap = {
    {"warc", "application/warc"},
    {"arc", "application/arc"},
  };

  ManagerInfo mi;
  protected int port;
  protected List<String> bindAddrs;
  protected boolean enableKeepAlive;
  protected UserRealm realm;
  private boolean start;
  private String includeIps;
  private String excludeIps;
  private boolean logForbidden;
  private long maxLoginInactivity = DEFAULT_MAX_LOGIN_INACTIVITY;
  protected boolean enableDebugUser;
  protected boolean useSsl;
  protected boolean resolveRemoteHost;
  protected String warningMsg;
  protected String sslKeystoreName;
  protected int sslRedirFromPort;
  protected List<String> disableSslServerProtocols =
    DEFAULT_DISABLE_SSL_SERVER_PROTOCOLS;;
  protected AuthType authType = DEFAULT_AUTH_TYPE;
  protected String formLoginUrl = DEFAULT_FORM_LOGIN_URL;
  protected String formLoginErrorUrl = DEFAULT_FORM_LOGIN_ERROR_URL;
  protected Authenticator authenticator;

  private String _403Msg;

  List accessHandlers = new ArrayList();

  protected LockssSessionManager sessionMgr = new LockssSessionManager();
  protected AccountManager acctMgr;
  protected LockssKeyStoreManager keystoreMgr;

  public BaseServletManager(String serverName) {
    super(serverName);
  }

  /** Start servlets  */
  public void startService() {
    acctMgr = getDaemon().getAccountManager();
    keystoreMgr = getDaemon().getKeystoreManager();
    initDescrs();
    super.startService();
  }

  /** Stop servlets  */
  public void stopService() {
    stopServer();
    super.stopService();
  }

  protected LockssDaemon getDaemon() {
    return (LockssDaemon)getApp();
  }

  /** Return the ManagerInfo object with config info and defaults for servlet
   * manager */
  protected abstract ManagerInfo getManagerInfo();

  /** Return array of ServletDescr for all servlets managed by the servlet
   * manager */
  public abstract ServletDescr[] getServletDescrs();

  /** Install appropriate users for these servlets */
  protected abstract void installUsers();

  /** Create and configure contexts for this server */
  protected abstract void configureContexts(HttpServer server);


  public void setConfig(Configuration config, Configuration prevConfig,
			Configuration.Differences changedKeys) {
    super.setConfig(config, prevConfig, changedKeys);
    mi = getManagerInfo();
    String prefix = mi.prefix;
    if (changedKeys.contains(prefix)) {
      port = config.getInt(prefix + SUFFIX_PORT, mi.defaultPort);
      bindAddrs = config.getList(prefix + SUFFIX_BIND_ADDRS,
				 Collections.EMPTY_LIST);
      enableKeepAlive = config.getBoolean(prefix + SUFFIX_TCP_KEEPALIVE,
					  DEFAULT_TCP_KEEPALIVE);
      start = config.getBoolean(prefix + SUFFIX_START, mi.defaultStart);
      _403Msg = config.get(prefix + SUFFIX_403_MSG, mi.default403Msg);
      enableDebugUser = config.getBoolean(prefix + SUFFIX_ENABLE_DEBUG_USER,
					  mi.defaultEnableDebugUser);
      useSsl = config.getBoolean(mi.prefix + SUFFIX_USE_SSL, false);
      if (useSsl) {
 	sslKeystoreName = config.get(mi.prefix + SUFFIX_SSL_KEYSTORE_NAME);
	sslRedirFromPort = config.getInt(mi.prefix + SUFFIX_SSL_REDIR_FROM, -1);
	disableSslServerProtocols =
	  config.getList(mi.prefix + SUFFIX_DISABLE_SSL_SERVER_PROTOCOLS,
			 DEFAULT_DISABLE_SSL_SERVER_PROTOCOLS);
      }
      authType = (AuthType)config.getEnum(AuthType.class,
					  mi.prefix + SUFFIX_AUTH_TYPE,
					  DEFAULT_AUTH_TYPE);
      switch (authType) {
      case Form:
	formLoginUrl = config.get(mi.prefix + SUFFIX_FORM_LOGIN_URL,
				  DEFAULT_FORM_LOGIN_URL);
	formLoginErrorUrl = config.get(mi.prefix + SUFFIX_FORM_LOGIN_ERROR_URL,
				       DEFAULT_FORM_LOGIN_ERROR_URL);
	break;
      default:
      }
      maxLoginInactivity =
	config.getTimeInterval(mi.prefix + SUFFIX_MAX_LOGIN_INACTIVITY,
			       DEFAULT_MAX_LOGIN_INACTIVITY);
      resolveRemoteHost = config.getBoolean(mi.prefix
					    + SUFFIX_RESOLVE_REMOTE_HOST,
					    mi.defaultResolveRemoteHost);
      warningMsg = config.get(mi.prefix + SUFFIX_WARNING);
    }
    // Access control prefix not nec. related to prefix, don't nest inside
    // if (changedKeys.contains(prefix))

    String accessPrefix = mi.accessPrefix;
    if (mi.accessPrefix == null) {
      accessPrefix = prefix + SUFFIX_IP_ACCESS_PREFIX;
    }
    if (changedKeys.contains(accessPrefix)) {
      includeIps = config.get(accessPrefix + SUFFIX_IP_INCLUDE, "");
      excludeIps = config.get(accessPrefix + SUFFIX_IP_EXCLUDE, "");
      logForbidden = config.getBoolean(accessPrefix + SUFFIX_LOG_FORBIDDEN,
				       mi.defaultLogForbidden);
      log.debug("Installing new ip filter: incl: " + includeIps +
		", excl: " + excludeIps);
      setIpFilters();
    }
  }

  /** Return true iff the auth method is of a type that results in there
   * always being user sessions to display */
  public boolean hasUserSessions() {
    switch (authType) {
    case Form:
      return true;
    default:
      return false;
    }
  }

  public Authenticator getAuthenticator() {
    return authenticator;
  }

  public String getWarningMsg() {
    return warningMsg;
  }

  void startOrStop() {
    if (start) {
//       if (getDaemon().isDaemonInited()) {
      if (isInited()) {
	startServlets();
      }
    } else if (isServerRunning()) {
      stopServer();
    }
  }

  // Create mapping from servlet class to ServletDescr
  protected Hashtable servletToDescr = new Hashtable();

  protected void initDescrs() {
    for (ServletDescr d : getServletDescrs()) {
      if (d.cls != null && d.cls != ServletDescr.UNAVAILABLE_SERVLET_MARKER) {
	servletToDescr.put(d.cls, d);
      }
    }
  }

  public ServletDescr findServletDescr(Object o) {
    ServletDescr res = (ServletDescr)servletToDescr.get(o.getClass());
    if (res != null) return res;
    // if not in map, o might be an instance of a subclass of a servlet class
    // that's in the map.
    for (ServletDescr d : getServletDescrs()) {
      if (d.cls != null && d.cls.isInstance(o)) {
	// found a descr that describes a superclass.  Add actual class to map
	servletToDescr.put(o.getClass(), d);
	return d;
      }
    }
    return null;		// shouldn't happen
				// XXX do something better here
  }

  public ServletDescr findServletDescrFromPath(String path) {
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    for (ServletDescr d : getServletDescrs()) {
      if (d.getPath().equals(path)) {
	return d;
      }
    }
    return null;		// shouldn't happen
				// XXX do something better here
  }

  public void startServlets() {
    if (isRunningOnPort(port)) {
      return;
    }
    if (isServerRunning()) {
      stopServer();
    }
    try {
      // Create the server
      HttpServer server = new Server();
      if (resolveRemoteHost) {
	server.setResolveRemoteHost(true);
      }

      KeyManagerFactory kmf = null;
      if (useSsl) {
	kmf = keystoreMgr.getKeyManagerFactory(sslKeystoreName,
					       mi.serverName + " server");
	if (kmf == null) {
	  log.critical("Keystore " + sslKeystoreName +
		       " not found, not starting " + mi.serverName + " server");
	  return;
	}
      }

      addListeners(server, kmf);

      setupAuthRealm();

      configureContexts(server);

      // Start the http server
      startServer(server, port);
    } catch (Exception e) {
      log.warning("Couldn't start servlets", e);
    }
  }

  protected void addListeners(HttpServer server, KeyManagerFactory kmf) {
    if (bindAddrs.isEmpty()) {
      try {
	addListener(server, null, port, kmf);
      } catch (UnknownHostException e) {
	log.critical("UnknownHostException with null host, not starting "
		     + mi.serverName + " server");
      }
    } else {
      for (String host : bindAddrs) {
	try {
	  addListener(server, host, port, kmf);
	} catch (UnknownHostException e) {
	  log.critical("Bind addr " + host +
		       " not found, " + mi.serverName +
		       " not listening on that address");
	}
      }
    }
  }

  protected void addListener(HttpServer server,
			     String host, int port,
			     KeyManagerFactory kmf)
      throws UnknownHostException {
    HttpListener listener;
    // Create a port listener
    if (useSsl) {
      LockssSslListener lsl =
	new LockssSslListener(new org.mortbay.util.InetAddrPort(host, port)) {
	  @Override
	  public void handleConnection(Socket socket) throws IOException {
	    if (enableKeepAlive) {
	      socket.setKeepAlive(true);
	    }
	    super.handleConnection(socket);
	  }};
      lsl.setKeyManagerFactory(kmf);
      lsl.setDisableProtocols(disableSslServerProtocols);
      listener = lsl;

      if (sslRedirFromPort > 0) {
	// Setup redirect from insecure port
	log.debug("redir from: " + sslRedirFromPort);
	SocketListener redirListener =
	  new SocketListener(new org.mortbay.util.InetAddrPort(host,
							       sslRedirFromPort));
	redirListener.setIntegralPort(port);
	// 	  redirListener.setConfidentialPort(port);
	server.addListener(redirListener);
      }
    } else {
      listener =
	new SocketListener(new org.mortbay.util.InetAddrPort(host,port)) {
	  @Override
	  public void handleConnection(Socket socket) throws IOException {
	    if (enableKeepAlive) {
	      socket.setKeepAlive(true);
	    }
	    super.handleConnection(socket);
	  }};
    }
    server.addListener(listener);
  }

  // common context setup
  // adds IpAccessHandler as all contexts want it
  // doesn't add AuthHandler as not all contexts want it
  HttpContext makeContext(HttpServer server, String path) {
    HttpContext context = server.getContext(path);
    context.setAttribute(HttpContext.__ErrorHandler,
			 new LockssErrorHandler("daemon")); 
    context.setAttribute(CONTEXT_ATTR_LOCKSS_APP, theApp);
    context.setAttribute(CONTEXT_ATTR_SERVLET_MGR, this);
    // In this environment there is no point in consuming memory with
    // cached resources
    context.setMaxCachedFileSize(0);

    // IpAccessHandler is always first handler
    if (mi.doFilterIpAccess) {
      addAccessHandler(context);
    }
    return context;
  }

  void setIpFilters() {
    for (Iterator iter = accessHandlers.iterator(); iter.hasNext(); ) {
      setIpFilter((IpAccessHandler)iter.next());
    }
  }

  void setIpFilter(IpAccessHandler ah) {
    try {
      IpFilter filter = new IpFilter();
      filter.setFilters(includeIps, excludeIps);
      ah.setFilter(filter);
    } catch (IpFilter.MalformedException e) {
      log.warning("Malformed IP filter, filters not changed", e);
    }
    ah.setLogForbidden(logForbidden);
    ah.setAllowLocal(true);
    ah.set403Msg(_403Msg);
  }

  void setupAuthRealm() {
    if (mi.doAuth) {
      realm = newUserRealm();
      installUsers();
      if (acctMgr != null && acctMgr.getUsers().isEmpty()) {
	log.warning("No users created, " + mi.authRealm +
		    " is effectively disabled.");
      }
    }
  }

  protected UserRealm newUserRealm() {
    return new LockssUserRealm(mi.authRealm, acctMgr);
  }

  protected void installDebugUser() {
    if (enableDebugUser) {
      try {
	log.debug("passwd props file: " + mi.debugUserFile);
	URL propsUrl = this.getClass().getResource(mi.debugUserFile);
	if (propsUrl != null) {
	  log.debug("passwd props file: " + propsUrl);
	  log.debug("debugUserFile: " + mi.debugUserFile);
	  acctMgr.loadFromProps(mi.debugUserFile);
	}
      } catch (IOException e) {
	log.warning("Error loading " + mi.debugUserFile, e);
      }
    }
  }

  // Manually install password set by platform config.
  protected void installPlatformUser() {
    // Use platform config in case real config hasn't been loaded yet (when
    // used from TinyUI)
    Configuration platConfig = ConfigManager.getPlatformConfig();
    String platUser = platConfig.get(PARAM_PLATFORM_USERNAME);
    String platPass = platConfig.get(PARAM_PLATFORM_PASSWORD);
    acctMgr.installPlatformUser(platUser, platPass);
  }

  protected void installGlobalUsers() {
    // Install globally configured users
    // XXX disallow this on the platform
    installUsers(ConfigManager.getCurrentConfig().getConfigTree(mi.prefix + SUFFIX_USERS));
  }

  protected void installLocalUsers() {
    // Install locally configured users
//     installUsers(ConfigManager.getCurrentConfig().getConfigTree(PARAM_USERS));
  }

  protected void installUsers(Configuration users) {
    for (Iterator iter = users.nodeIterator(); iter.hasNext(); ) {
      Configuration oneUser = users.getConfigTree((String)iter.next());
      String user = oneUser.get(USER_PARAM_USER);
      String pwd = oneUser.get(USER_PARAM_PWD);
      String roles = oneUser.get(USER_PARAM_ROLES);
      if (!StringUtil.isNullString(user) &&
	  !StringUtil.isNullString(pwd)) {
	try {
	  UserAccount acct = acctMgr.addStaticUser(user, pwd);
	  if (!StringUtil.isNullString(roles)) {
	    acct.setRoles(roles);
	  }
	} catch (AccountManager.NotAddedException e) {
	  log.error(e.getMessage());
	}
      }
    }
  }

  protected void addAccessHandler(HttpContext context) {
    IpAccessHandler ah = new IpAccessHandler(serverName);
    setIpFilter(ah);
    context.addHandler(ah);
    accessHandlers.add(ah);
  }

  protected void setContextAuthHandler(HttpContext context, UserRealm realm) {
    if (realm != null) {
      context.setRealm(realm);
      switch (authType) {
      case Basic:
	log.info(mi.serverName + ", " + context.getName() + ": Using basic auth");
	context.setAuthenticator(new BasicAuthenticator());
	context.addHandler(new SecurityHandler());
	context.addSecurityConstraint("/",
				      newSecurityConstraint(mi.serverName,
							    "*"));
	break;
      case Form:
	log.info(mi.serverName + ", " + context.getName() + ": Using form auth");
	LockssFormAuthenticator fa = new LockssFormAuthenticator(getDaemon());
	fa.setLoginPage(formLoginUrl);
	fa.setErrorPage(formLoginErrorUrl);
	if (maxLoginInactivity > 0) {
	  fa.setMaxInactivity(maxLoginInactivity);
	}
	context.addSecurityConstraint("/",
				      newSecurityConstraint(mi.serverName,
							    "*"));
	context.setAuthenticator(fa);
	break;
      }
      authenticator = 	context.getAuthenticator();
    }
  }

  SecurityConstraint newSecurityConstraint(String name, String role) {
    SecurityConstraint sc = new SecurityConstraint(name, role);
    if (useSsl) {
      // Ensure all contexts insist on appropriate security
      sc.setDataConstraint(SecurityConstraint.DC_INTEGRAL);
    }
    return sc;
  }

  ContextListenerWebApplicationHandler makeWebAppHandler(HttpContext context) {

    // Use the ContextListenerWebApplicationHandler instead of Jetty's
    // WebApplicationHandler, which it extends. This is needed because the
    // Jetty 5 WebApplicationHandler ignores any ServletContextListener that it
    // is being told to notify and prevents Apache CXF web services from
    // working.
    ContextListenerWebApplicationHandler handler =
	new ContextListenerWebApplicationHandler();
    handler.setSessionManager(sessionMgr);
    context.setAttribute(CONTEXT_ATTR_SERVLET_HANDLER, handler);
    return handler;
  }

  protected void setupDirContext(HttpServer server, UserRealm realm,
				 String contextPath, String dir,
				 FilenameFilter filter)
      throws MalformedURLException {

    HttpContext context = makeContext(server, contextPath);

    // user authentication handler
    setContextAuthHandler(context, realm);

    WebApplicationHandler handler = makeWebAppHandler(context);
    context.addHandler(handler);
    handler.addServlet("Login", "/LoginForm", "org.lockss.servlet.LoginForm");
    // Log Dir resource
    String dirname = (dir != null) ? dir : "";
    URL url = new URL("file", null,
		      new File(dirname).getAbsolutePath());
    if (filter != null) {
      try {
	org.mortbay.util.Resource res =
	  new FilteredDirFileResource(url, filter);
	context.setBaseResource(res);
      } catch (IOException e) {
	throw new
	  MalformedURLException("Can't create " +
				"FilteredDirFileResource: " + e.toString());
      } catch (URISyntaxException e) {
	throw new
	  MalformedURLException("Can't create " +
				"FilteredDirFileResource: " + e.toString());
      }
    } else {
      context.setResourceBase(url.toString());
    }
    switch (authType) {
    case Basic:
      break;
    case Form:
      Map redirMap = new HashMap();
      redirMap.put(formLoginUrl + ".*", formLoginUrl);
      context.setAttribute(CONTEXT_ATTR_RESOURCE_REDIRECT_MAP, redirMap);
      break;
    }

    ServletHolder holder =
      handler.addServlet("Resource", "/",
			 "org.lockss.servlet.LockssResourceServlet");
    holder.put("dirAllowed", "true");

    for (int ix = 0; ix < textMimes.length; ix++) {
      context.setMimeMapping(textMimes[ix], "text/plain");
    }
    for (int ix = 0; ix < mimeMap.length; ix++) {
      context.setMimeMapping(mimeMap[ix][0], mimeMap[ix][1]);
    }

    // NotFoundHandler
    context.addHandler(new NotFoundHandler());
  }

  protected void addServlets(ServletDescr[] descrs, ServletHandler handler) {
    for (ServletDescr d : descrs) {
      Class cls = d.getServletClass();
      if (cls != null
	  && cls != ServletDescr.UNAVAILABLE_SERVLET_MARKER
	  && !d.isPathIsUrl()) {
	StringBuilder sb = new StringBuilder();
	sb.append("/");
	sb.append(d.getPath());
	if (d.isWildcardPath()) {
	  sb.append("/*");
	}
	String path = sb.toString();
	if (!d.isEnabled(getDaemon())) {
	  cls = ServletDescr.UNCONFIGURED_SERVLET_CLASS;
	}
	log.debug2("addServlet("+d.getServletName()+", "+path+
		   ", "+cls.getName()+")");
	handler.addServlet(d.getServletName(), path, cls.getName());
      }
    }
  }

  // Add a servlet if its class can be loaded.
  protected void addServletIfAvailable(ServletHandler handler, String name,
				       String pathSpec, String servletClass) {
    try {
      Class.forName(servletClass);
      handler.addServlet(name, pathSpec, servletClass);
    } catch (ClassNotFoundException e) {
      log.warning("Not starting servlet \"" + name +
		  "\", class not found: " + servletClass);
    }
  }

  /** Struct to hold particulars of concrete servlet managers needed for
   * generic processing.  Mostly config param defaults. */

  protected static class ManagerInfo {
    String prefix;
    String accessPrefix;		// if from different server
    String serverName;
    boolean defaultStart;
    int defaultPort;
    String default403Msg = "Access forbidden";
    boolean doAuth;
    boolean doFilterIpAccess = true;
    String authRealm;
    boolean defaultEnableDebugUser;
    boolean defaultLogForbidden;
    boolean defaultResolveRemoteHost;
    String debugUserFile;
  }

  public static class CompressingFilterWrapper implements Filter {
    public void init(final FilterConfig config) throws ServletException {
    }
    public void doFilter(ServletRequest request,
			 ServletResponse response,
			 FilterChain chain)
	throws IOException, ServletException {

      // Guard against Double.parseDouble() bug.  See
      // PlatformUtil.isBuggyDoubleString()
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      String acceptEncoding = httpRequest.getHeader("accept-encoding");
      if (acceptEncoding != null &&
	  PlatformUtil.isBuggyDoubleString(acceptEncoding)) {
	HttpServletResponse httpResponse = (HttpServletResponse) response;
	httpResponse.sendError(503, "Illegal number");
	return;
      }
      chain.doFilter(request, response);
    }
    public void destroy() {
    }
  }

}
