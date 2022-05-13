/*
 * $Id$
 */

/*

 Copyright (c) 2013-2016 Board of Trustees of Leland Stanford Jr. University,
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
import javax.servlet.http.*;
import org.mortbay.http.*;
import org.mortbay.jetty.servlet.*;
import com.github.ziplet.filter.compression.*;
import dk.itst.oiosaml.common.SAMLUtil;
import dk.itst.oiosaml.configuration.FileConfiguration;
import dk.itst.oiosaml.configuration.SAMLConfigurationFactory;
import dk.itst.oiosaml.sp.service.DispatcherServlet;
import dk.itst.oiosaml.sp.service.session.SessionDestroyListener;
import dk.itst.oiosaml.sp.service.util.Constants;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.account.*;
import org.lockss.subscription.SubscriptionManager;
import org.lockss.util.*;
import org.lockss.jetty.*;
import org.lockss.exporter.counter.CounterReportsManager;
import org.springframework.web.context.ContextLoaderListener;

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
  public static final String PASSWORD_PROPERTY_FILE =
          "/org/lockss/servlet/admin.props";

  // All of the params that are defined in terms of SUFFIX_XXX are accessed
  // via the generic mechanism in BaseServletManager.setConfig().  The
  // PARAM_XXX symbols for those params are here for the ParamDoc tools;
  // some are also used by other packages to set the params referenced here

  /** The port the admin UI listens on */
  public static final String PARAM_PORT = PREFIX + SUFFIX_PORT;
  public static final int DEFAULT_PORT = 8081;

  /** List of IP addresses to which to bind listen socket.  If not set,
   * server listens on all interfaces.  All listeners are on the same
   * port, given by the <tt>port</tt> parameter.  Changing this requires
   * daemon restart. */
  public static final String PARAM_BIND_ADDRS = PREFIX + SUFFIX_BIND_ADDRS;

  /** If true the admin UI will be started */
  public static final String PARAM_START = PREFIX + SUFFIX_START;
  public static final boolean DEFAULT_START = true;

  public static final String IP_ACCESS_PREFIX =
          PREFIX + SUFFIX_IP_ACCESS_PREFIX;
  /** List of IPs or subnets to allow */
  public static final String PARAM_IP_INCLUDE =
          IP_ACCESS_PREFIX + SUFFIX_IP_INCLUDE;
  /** List of IPs or subnets to reject */
  public static final String PARAM_IP_PLATFORM_SUBNET =
          IP_ACCESS_PREFIX + SUFFIX_IP_PLATFORM_SUBNET;

  /** Log accesses from forbidden IP addresses */
  public static final String PARAM_LOG_FORBIDDEN =
          PREFIX + SUFFIX_LOG_FORBIDDEN;
  public static final boolean DEFAULT_LOG_FORBIDDEN = true;

  /** Connect to named server with https if true */
  public static final String PARAM_USE_SSL = PREFIX + SUFFIX_USE_SSL;
  public static final boolean DEFAULT_USE_SSL = false;

  /** Name of managed keystore to use (see
   * org.lockss.keyMgr.keystore.<i>id</i>.name) */
  public static final String PARAM_SSL_KEYSTORE_NAME =
          PREFIX + SUFFIX_SSL_KEYSTORE_NAME;

  /** If set, http: connections to this port will be redirected to the
   * https: port */
  public static final String PARAM_SSL_REDIR_FROM =
          PREFIX + SUFFIX_SSL_REDIR_FROM;

  /** User authentication type: Basic or Form */
  public static final String PARAM_AUTH_TYPE = PREFIX + SUFFIX_AUTH_TYPE;
  public static final AuthType DEFAULT_AUTH_TYPE = AuthType.Basic;

  public static final String PARAM_RESOLVE_REMOTE_HOST =
          PREFIX + SUFFIX_RESOLVE_REMOTE_HOST;
  public static boolean DEFAULT_RESOLVE_REMOTE_HOST = true;

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
    mi.defaultResolveRemoteHost = DEFAULT_RESOLVE_REMOTE_HOST;
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

  static final Map<String, String> COMPRESSOR_DEFAULTS =
      new HashMap<String, String>();
  static {
    COMPRESSOR_DEFAULTS.put("compressionThreshold", "4096");
    COMPRESSOR_DEFAULTS.put("includeContentTypes",
            "text/html,text/xml,text/plain");
  }

  /** List of MIME-type prefixes for content that should be displayed
   * inline in a frame in ViewContent.  Content of other types will be
   * offered for download. */
  static final String PARAM_INFRAME_CONTENT_TYPES =
          PREFIX + "view.inFrameTypes";
  static final String DEFAULT_INFRAME_CONTENT_TYPES =
          "text;image;application/pdf;application/xhtml+xml";

  static final String OIOSAML_PREFIX = Configuration.PREFIX + "oiosaml.";

  /**
   * Default value of the OIOSAML operation configuration parameter.
   * <p>
   * <code>false</code> to disable, <code>true</code> to enable.
   */
  public static final String PARAM_OIOSAML_ENABLED =
      OIOSAML_PREFIX + "enabled";
  public static final boolean DEFAULT_OIOSAML_ENABLED = false;

  /**
   * OIOSAML home directory parent directory name.
   * <p>
   * Defaults to <code><i>daemon_tmpdir</i></code>.
   */
  public static final String PARAM_OIOSAML_HOMEDIR_PATH = OIOSAML_PREFIX
      + "homeDirectoryPath";

  /**
   * Default value of the OIOSAML home directory configuration parameter.
   */
  public static final String DEFAULT_OIOSAML_HOMEDIR = "oiosaml";

  /**
   * Default full path of the name of the OIOSAML home directory configuration
   * parameter.
   */
  public static final String DEFAULT_OIOSAML_HOMEDIR_PATH = "<tmpdir>/"
      + DEFAULT_OIOSAML_HOMEDIR;

  /**
   * OIOSAML configuration file name.
   * <p>
   * Defaults to <code><i>oiosaml-sp.properties</i></code>.
   */
  public static final String PARAM_OIOSAML_CONFIG_FILE = OIOSAML_PREFIX
      + "configFileName";

  /**
   * Default value of the OIOSAML configuration file name configuration
   * parameter.
   */
  public static final String DEFAULT_OIOSAML_CONFIG_FILE =
      SAMLUtil.OIOSAML_DEFAULT_CONFIGURATION_FILE;

  /**
   * OIOSAML protected URL mapping.
   * <p>
   * Defaults to <code><i>/*</i></code>.
   */
  public static final String PARAM_OIOSAML_PROTECTED_URLS = OIOSAML_PREFIX
      + "protectedUrls";

  /**
   * Default value of the OIOSAML protected URL mapping configuration parameter.
   */
  public static final String DEFAULT_OIOSAML_PROTECTED_URLS = "/*";

  // Descriptors for all admin servlets.

  protected static final ServletDescr SERVLET_HOME =
          new ServletDescr("UiHome",
          UiHome.class,
          "LOCKSS Administration",
          "Home",
          ServletDescr.LARGE_LOGO);
  protected static final ServletDescr SERVLET_EDIT_ACCOUNT =
          new ServletDescr("UserEditAccount",
          UserEditAccount.class,
          "My Account",
          (ServletDescr.IN_NAV),
          "Update account info") {
            public boolean isEnabled(LockssDaemon daemon) {
              AccountManager acctMgr = daemon.getAccountManager();
              return acctMgr != null && acctMgr.isEnabled();
            }
            public boolean isInNav(LockssServlet servlet) {
// 	if (servlet.doesUserHaveRole(LockssServlet.ROLE_USER_ADMIN)
// 	    && !servlet.doesUserHaveRole(LockssServlet.ROLE_DEBUG)) {
// 	  return false;
// 	}
              UserAccount acct = servlet.getUserAccount();
              return acct != null && acct.isEditable();
            }
            public String getNavHeading(LockssServlet servlet) {
              UserAccount acct = servlet.getUserAccount();
              if (acct != null) {
                return super.getNavHeading(servlet) + " (" + acct.getName() + ")";
              }
              return super.getNavHeading(servlet);
      }};

  protected static final ServletDescr SERVLET_EDIT_ACCOUNTS =
          new ServletDescr("AdminEditAccounts",
          AdminEditAccounts.class,
          "User Accounts",
          (ServletDescr.IN_NAV | ServletDescr.IN_UIHOME
          | ServletDescr.NEED_ROLE_USER_ADMIN),
          "Administer user accounts") {
            public boolean isEnabled(LockssDaemon daemon) {
              AccountManager acctMgr = daemon.getAccountManager();
              return acctMgr != null && acctMgr.isEnabled();
      }};

  protected static final ServletDescr SERVLET_BATCH_AU_CONFIG =
          new ServletDescr("BatchAuConfig",
          BatchAuConfig.class,
          "Journal Configuration",
          (ServletDescr.IN_NAV | ServletDescr.IN_UIHOME
          | ServletDescr.NEED_ROLE_AU_ADMIN),
          "Add or remove titles from this LOCKSS box");
  // XXXUI Development version
//  protected static final ServletDescr SERVLET_BATCH_AU_CONFIG_NEW =
//          new ServletDescr("BatchAuConfigNew",
//          BatchAuConfigNew.class,
//          "Journal Configuration (New UI)",
//          (ServletDescr.IN_NAV | ServletDescr.IN_UIHOME
//          | ServletDescr.NEED_ROLE_AU_ADMIN),
//          "Add or remove titles from this LOCKSS box");
  protected static final ServletDescr SERVLET_AU_CONFIG =
          new ServletDescr("AuConfig",
          AuConfig.class,
          "Manual Journal Configuration",
          (ServletDescr.IN_UIHOME | ServletDescr.NEED_ROLE_AU_ADMIN),
          "Manually edit single AU configuration");
  protected static final ServletDescr SERVLET_ADMIN_ACCESS_CONTROL =
          new ServletDescr("AdminIpAccess",
          AdminIpAccess.class,
          "Admin Access Control",
          (ServletDescr.IN_NAV | ServletDescr.IN_UIHOME
          | ServletDescr.NEED_ROLE_USER_ADMIN),
          "Control access to the administrative UI");
  protected static final ServletDescr SERVLET_PROXY_ACCESS_CONTROL =
          new ServletDescr("ProxyIpAccess",
          ProxyIpAccess.class,
          "Content Access Control",
          (ServletDescr.IN_NAV | ServletDescr.IN_UIHOME
          | ServletDescr.NEED_ROLE_CONTENT_ADMIN),
          "Control access to the preserved content");
  protected static final ServletDescr SERVLET_PROXY_AND_CONTENT =
          new ServletDescr("ProxyAndContent",
          ProxyAndContent.class,
          "Content Access Options",
          (ServletDescr.IN_NAV | ServletDescr.IN_UIHOME
          | ServletDescr.NEED_ROLE_CONTENT_ADMIN),
          "Configure the audit proxy and the ICP server");
  protected static final ServletDescr SERVLET_PROXY_INFO =
          new ServletDescr("ProxyConfig",
          ProxyConfig.class,
          "Proxy Info",
          "info/ProxyInfo",
          ServletDescr.IN_NAV | ServletDescr.IN_UIHOME,
          "Info for configuring browsers and proxies"
          + "<br>"
          + "to access preserved content on this LOCKSS box");
  protected static final ServletDescr SERVLET_EXPERT_CONFIG =
          new ServletDescr("ExpertConfig",
          ExpertConfig.class,
          "Expert Config",
          (ServletDescr.IN_NAV
          | ServletDescr.NEED_ROLE_USER_ADMIN),
          "Allows arbitrary local configuration");
  protected static final ServletDescr SERVLET_PLUGIN_CONFIG =
          new ServletDescr("PluginConfig",
          PluginConfig.class,
          "Plugin Configuration",
          (ServletDescr.IN_NAV | ServletDescr.IN_UIHOME
          | ServletDescr.NEED_ROLE_AU_ADMIN),
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
          "Status of LOCKSS box contents and operation");
  // XXXUI Development version
//  protected static final ServletDescr SERVLET_DAEMON_STATUS_NEW =
//          new ServletDescr("DaemonStatusNew",
//          DaemonStatusNew.class,
//          "Daemon Status (New UI)",
//          ServletDescr.IN_NAV | ServletDescr.IN_UIHOME,
//          "Status of LOCKSS box contents and operation");
  protected static final ServletDescr SERVLET_DISPLAY_CONTENT_STATUS =
          new ServletDescr("DisplayContentStatus",
          DisplayContentStatus.class,
          "Display Content Status",
          ServletDescr.IN_NAV | ServletDescr.IN_UIHOME,
          "Status of LOCKSS box contents");
  protected static final ServletDescr SERVLET_DISPLAY_CONTENT_TAB =
          new ServletDescr("DisplayContentTab",
          DisplayContentTab.class,
          "Display Content Tab");
  public static final ServletDescr SERVLET_DISPLAY_CONTENT =
          new ServletDescr("ViewContent",
          ViewContent.class,
          "View Content",
          ServletDescr.NEED_ROLE_CONTENT_ACCESS);
  public static final ServletDescr SERVLET_ADD_CONTENT =
          new ServletDescr("AddContent",
          AddContent.class,
          "Add Content",
          ServletDescr.NEED_ROLE_CONTENT_ACCESS);
  public static final ServletDescr SERVLET_ADD_CONTENT_TAB =
          new ServletDescr("AddContentTab",
          AddContentTab.class,
          "Add Content Tab",
          ServletDescr.NEED_ROLE_CONTENT_ACCESS);
  // XXXUI New servlet
  public static final ServletDescr SERVLET_SERVE_CONTENT =
          new ServletDescr("ServeContent",
          ServeContent.class,
          "Serve Content",
          ServletDescr.NEED_ROLE_CONTENT_ACCESS | ServletDescr.WILDCARD_PATH);
  public static final ServletDescr SERVLET_TIME_GATE =
      new ServletDescr("TimeGateService",
		       TimeGateService.class,
		       "Time Gate",
		       ServletDescr.NEED_ROLE_CONTENT_ACCESS);
  public static final ServletDescr SERVLET_TIME_MAP =
      new ServletDescr("TimeMapService",
		       TimeMapService.class,
		       "Time Map",
		       ServletDescr.NEED_ROLE_CONTENT_ACCESS);
  public static final ServletDescr SERVLET_EXPORT_CONTENT =
          new ServletDescr("ExportContent",
          ExportContent.class,
          "Export Content",
          (ServletDescr.NEED_ROLE_CONTENT_ACCESS),
          "Export preserved content as Zip, WARC, etc.") {
            public boolean isEnabled(LockssDaemon daemon) {
              return CurrentConfig.getBooleanParam(ExportContent.PARAM_ENABLE_EXPORT,
                      ExportContent.DEFAULT_ENABLE_EXPORT);
      }};
  public static final ServletDescr SERVLET_MIGRATE_CONTENT =
    new ServletDescr("MigrateContent",
      MigrateContent.class,
      "Migrate V1 Content",
      (ServletDescr.NEED_ROLE_CONTENT_ACCESS | ServletDescr.IN_NAV),
      "Migrate V1 Content to V2 Repository") {
      public boolean isEnabled(LockssDaemon daemon) {
        return CurrentConfig.getBooleanParam(MigrateContent.PARAM_ENABLE_MIGRATION,
          MigrateContent.DEFAULT_ENABLE_MIGRATION);
      }};
  public static final ServletDescr SERVLET_LIST_OBJECTS =
          new ServletDescr("ListObjects",
          ListObjects.class,
          "List Objects");
  protected static final ServletDescr SERVLET_HASH_CUS =
          new ServletDescr("HashCUS",
          HashCUS.class,
          "Hash CUS",
          ServletDescr.NEED_ROLE_DEBUG
          | ServletDescr.NEED_ROLE_CONTENT_ACCESS);
  
  protected static final ServletDescr SERVLET_LIST_HOLDINGS =
          new ServletDescr("TitleList",
          ListHoldings.class,
          "Title List",
          "Titles",
          (ServletDescr.IN_NAV | ServletDescr.IN_UIHOME),
          "List title metadata") {
            public boolean isEnabled(LockssDaemon daemon) {
              return CurrentConfig.getBooleanParam(ListHoldings.PARAM_ENABLE_HOLDINGS,
                      ListHoldings.DEFAULT_ENABLE_HOLDINGS);
      }};

  /*protected static final ServletDescr SERVLET_OPENURL_QUERY =
   new ServletDescr("OpenUrlQuery",
   OpenUrlQuery.class,
   "OpenURL Query",
   "OpenUrlQuery",
   (ServletDescr.IN_NAV | ServletDescr.IN_UIHOME),
   "Perform an OpenURL query against the holdings.") {
   public boolean isEnabled(LockssDaemon daemon) {
   return CurrentConfig.getBooleanParam(OpenUrlQuery.PARAM_ENABLE_QUERY,
   OpenUrlQuery.DEFAULT_ENABLE_QUERY);
   }};*/

  protected static final ServletDescr LINK_LOGS =
          new ServletDescr(null,
          null,
          "Logs",
          "log",
          ServletDescr.IN_NAV
          | ServletDescr.NEED_ROLE_DEBUG
          | ServletDescr.NEED_ROLE_CONTENT_ACCESS);
  protected static final ServletDescr LINK_EXPORTS =
          new ServletDescr(null,
          null,
          "Exports",
          "export",
          ServletDescr.NEED_ROLE_CONTENT_ACCESS);
  // Link to ISOs only appears if there are actually any ISOs
  protected static final ServletDescr LINK_ISOS =
          new ServletDescr(null,
          null,
          "ISOs",
          "iso",
          (ServletDescr.IN_NAV | ServletDescr.IN_UIHOME
          | ServletDescr.NEED_ROLE_USER_ADMIN),
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
          ServletDescr.IN_NAV | ServletDescr.NEED_ROLE_DEBUG);
  protected static final ServletDescr SERVLET_RAISE_ALERT =
          new ServletDescr("RaiseAlert",
          RaiseAlert.class,
          "Raise Alert",
          ServletDescr.NEED_ROLE_DEBUG | ServletDescr.NEED_ROLE_USER_ADMIN);
  protected static final ServletDescr SERVLET_DEBUG_PANEL =
          new ServletDescr("DebugPanel",
          DebugPanel.class,
          "Debug Panel",
          (ServletDescr.IN_NAV | ServletDescr.NEED_ROLE_DEBUG
          | ServletDescr.NEED_ROLE_AU_ADMIN));
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
  
  protected static final ServletDescr LINK_LOGOUT =
          new ServletDescr("LoginForm",
          null,
          "Logout",
          "LoginForm?logout=true",
          ServletDescr.IN_NAV) {
            public boolean isInNav(LockssServlet servlet) {
              ServletManager mgr = servlet.getServletManager();
              return (mgr.getAuthenticator() instanceof LockssFormAuthenticator);
      }};

  protected static final ServletDescr LOGIN_FORM =
          new ServletDescr("LoginForm",
          LoginForm.class,
          "LOCKSS Administration",
          ServletDescr.NO_NAV_TABLE | ServletDescr.LARGE_LOGO);
		     
  protected static final ServletDescr SERVLET_COUNTER_REPORTS =
      new ServletDescr("CounterReportsServlet",
                       CounterReportsServlet.class,
                       "COUNTER Reports",
		       "CounterReports",
		       (ServletDescr.IN_NAV | ServletDescr.IN_UIHOME),
		       "COUNTER Report generator") {
	public boolean isEnabled(LockssDaemon daemon) {
	  CounterReportsManager mgr = daemon.getCounterReportsManager();
	  return mgr != null && mgr.isReady();
	}};

  protected static final ServletDescr SERVLET_CXF_WEB_SERVICES =
      new ServletDescr("CXFServlet",
                       CXFServlet.class,
                       "JAX-WS CXF Servlet",
		       "ws/*",
		       0,
	               "JAX-WS CXF Web Services");

  protected static final ServletDescr SERVLET_SUB_MANAGEMENT =
      new ServletDescr("SubscriptionManagement",
		       SubscriptionManagement.class,
		       "Title Subscription Management",
		       (ServletDescr.NEED_ROLE_AU_ADMIN),
		       "Subscribe or unsubscribe to individual titles") {
	public boolean isEnabled(LockssDaemon daemon) {
	  try {
	    SubscriptionManager mgr = daemon.getSubscriptionManager();
	    return mgr != null && mgr.isReady();
	  } catch (Exception e) {
	    return false;
	  }
	}};

  protected static final ServletDescr SERVLET_MD_MONITOR =
      new ServletDescr("MetadataMonitor",
	  	       MetadataMonitor.class,
	  	       "Metadata Monitor",
	  	       ServletDescr.NEED_ROLE_DEBUG,
	  	       "Metadata Monitor");

  protected static final ServletDescr SERVLET_MD_CONTROL =
      new ServletDescr("MetadataControl",
	  	       MetadataControl.class,
	  	       "Metadata Control",
	  	       ServletDescr.NEED_ROLE_AU_ADMIN,
	  	       "Metadata Control");

  protected static final ServletDescr SERVLET_OIOSAML =
      new ServletDescr("SAMLDispatcherServlet",
	  		DispatcherServlet.class,
                       "OIOSAML Dispatcher Servlet",
		       "saml/*",
		       0,
	               "OIOSAML") {
	public boolean isEnabled(LockssDaemon daemon) {
          return CurrentConfig.getBooleanParam(PARAM_OIOSAML_ENABLED,
              DEFAULT_OIOSAML_ENABLED);
	}};

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
    SERVLET_ADD_CONTENT,
    SERVLET_ADD_CONTENT_TAB,
    SERVLET_SERVE_CONTENT,
    SERVLET_EXPORT_CONTENT,
    SERVLET_MIGRATE_CONTENT,
    SERVLET_LIST_OBJECTS,
    SERVLET_DEBUG_PANEL,
    SERVLET_EXPERT_CONFIG,
    SERVLET_LIST_HOLDINGS,
    SERVLET_COUNTER_REPORTS,
    //SERVLET_OPENURL_QUERY,
    SERVLET_TIME_GATE,
    SERVLET_TIME_MAP,
    LINK_LOGS,
    LINK_ISOS,
    LINK_EXPORTS,
    SERVLET_THREAD_DUMP,
    SERVLET_RAISE_ALERT,
    SERVLET_HASH_CUS,
    LINK_CONTACT,
    SERVLET_EDIT_ACCOUNT,
    SERVLET_EDIT_ACCOUNTS,
    SERVLET_SUB_MANAGEMENT,
    LINK_HELP,
    LINK_LOGOUT,
    LOGIN_FORM,
    SERVLET_CXF_WEB_SERVICES,
    SERVLET_MD_MONITOR,
    SERVLET_MD_CONTROL,
    SERVLET_OIOSAML
  };

  // XXXUI List of servlets to show in new UI: parallel main list but with new versions
  static final ServletDescr servletDescrsNew[] = {
    SERVLET_HOME,
//    SERVLET_BATCH_AU_CONFIG_NEW,
    SERVLET_AU_CONFIG,
    SERVLET_PLUGIN_CONFIG,
    SERVLET_ADMIN_ACCESS_CONTROL,
    SERVLET_PROXY_ACCESS_CONTROL,
    SERVLET_PROXY_AND_CONTENT,
    SERVLET_PROXY_INFO,
//    SERVLET_DAEMON_STATUS_NEW,
    SERVLET_DISPLAY_CONTENT_STATUS,
    SERVLET_DISPLAY_CONTENT_TAB,
    SERVLET_DISPLAY_CONTENT,
    SERVLET_ADD_CONTENT,
    SERVLET_ADD_CONTENT_TAB,
    SERVLET_SERVE_CONTENT,
    SERVLET_TIME_GATE,
    SERVLET_TIME_MAP,
    SERVLET_EXPORT_CONTENT,
    SERVLET_MIGRATE_CONTENT,
    SERVLET_LIST_OBJECTS,
    SERVLET_DEBUG_PANEL,
    SERVLET_EXPERT_CONFIG,
    SERVLET_LIST_HOLDINGS,
    SERVLET_COUNTER_REPORTS,
    //SERVLET_OPENURL_QUERY,
    SERVLET_TIME_GATE,
    SERVLET_TIME_MAP,
    LINK_LOGS,
    LINK_ISOS,
    LINK_EXPORTS,
    SERVLET_THREAD_DUMP,
    SERVLET_RAISE_ALERT,
    SERVLET_HASH_CUS,
    LINK_CONTACT,
    SERVLET_EDIT_ACCOUNT,
    SERVLET_EDIT_ACCOUNTS,
    SERVLET_SUB_MANAGEMENT,
    LINK_HELP,
    LINK_LOGOUT,
    LOGIN_FORM,
    SERVLET_CXF_WEB_SERVICES,
    SERVLET_MD_MONITOR,
    SERVLET_MD_CONTROL,
    SERVLET_OIOSAML

  };
  // XXXUI List of servlets to show in transitional UI: combine main list with new versions
  static final ServletDescr servletDescrsTransitional[] = {
    SERVLET_HOME,
    SERVLET_BATCH_AU_CONFIG,
//    SERVLET_BATCH_AU_CONFIG_NEW,
    SERVLET_AU_CONFIG,
    SERVLET_PLUGIN_CONFIG,
    SERVLET_ADMIN_ACCESS_CONTROL,
    SERVLET_PROXY_ACCESS_CONTROL,
    SERVLET_PROXY_AND_CONTENT,
    SERVLET_PROXY_INFO,
    SERVLET_DAEMON_STATUS,
//    SERVLET_DAEMON_STATUS_NEW,
    SERVLET_DISPLAY_CONTENT_STATUS,
    SERVLET_DISPLAY_CONTENT_TAB,
    SERVLET_DISPLAY_CONTENT,
    SERVLET_SERVE_CONTENT,
    SERVLET_ADD_CONTENT,
    SERVLET_ADD_CONTENT_TAB,
    SERVLET_TIME_GATE,
    SERVLET_TIME_MAP,
    SERVLET_EXPORT_CONTENT,
    SERVLET_MIGRATE_CONTENT,
    SERVLET_LIST_OBJECTS,
    SERVLET_DEBUG_PANEL,
    SERVLET_EXPERT_CONFIG,
    SERVLET_LIST_HOLDINGS,
    SERVLET_COUNTER_REPORTS,
    //SERVLET_OPENURL_QUERY,
    LINK_LOGS,
    LINK_ISOS,
    LINK_EXPORTS,
    SERVLET_THREAD_DUMP,
    SERVLET_RAISE_ALERT,
    SERVLET_HASH_CUS,
    LINK_CONTACT,
    SERVLET_EDIT_ACCOUNT,
    SERVLET_EDIT_ACCOUNTS,
    SERVLET_SUB_MANAGEMENT,
    LINK_HELP,
    LINK_LOGOUT,
    LOGIN_FORM,
    SERVLET_CXF_WEB_SERVICES,
    SERVLET_MD_MONITOR,
    SERVLET_MD_CONTROL,
    SERVLET_OIOSAML
  };

  // XXXUI Show the transitional or new UI if param is enabled
  public ServletDescr[] getServletDescrs() {
    boolean newui = CurrentConfig.getBooleanParam(PARAM_ENABLE_NEW_UI,
            DEFAULT_ENABLE_NEW_UI);
    boolean transui = CurrentConfig.getBooleanParam(PARAM_ENABLE_TRANSITIONAL_UI,
            DEFAULT_ENABLE_TRANSITIONAL_UI);
    return transui ? servletDescrsTransitional : newui ? servletDescrsNew : servletDescrs;
  }

  // XXXUI Override to init new servlets too
  @Override
  protected void initDescrs() {
    for (ServletDescr d : getServletDescrs()) {
      if (d.cls != null && d.cls != ServletDescr.UNAVAILABLE_SERVLET_MARKER) {
        servletToDescr.put(d.cls, d);
      }
    }
    for (ServletDescr d : servletDescrsNew) {
      if (d.cls != null && d.cls != ServletDescr.UNAVAILABLE_SERVLET_MARKER) {
        servletToDescr.put(d.cls, d);
      }
    }
  }

  private String logdir;
  private File exportdir;
  private String redirectRootTo = DEFAULT_REDIRECT_ROOT;
  protected String isodir;
  private LockssResourceHandler rootResourceHandler;
  private List inFrameContentTypes;
  private boolean hasIsoFiles = false;
  private boolean compressorEnabled = DEFAULT_COMPRESSOR_ENABLED;
  private boolean exportEnabled = ExportContent.DEFAULT_ENABLE_EXPORT;
  private boolean migrateEnabled = MigrateContent.DEFAULT_ENABLE_MIGRATION;
  private boolean oiosamlEnabled = DEFAULT_OIOSAML_ENABLED;
  private File oiosamlHomeDir = null;
  private String oiosamlConfigFileName = DEFAULT_OIOSAML_CONFIG_FILE;
  private String oiosamlProtectedUrls = null;


  public AdminServletManager() {
    super(SERVER_NAME);
  }

  public void setConfig(Configuration config, Configuration prevConfig,
          Configuration.Differences changedKeys) {
    final String DEBUG_HEADER = "setConfig(): ";
    super.setConfig(config, prevConfig, changedKeys);
    isodir = config.get(PARAM_ISODIR);
    logdir = config.get(PARAM_LOGDIR);
    if (changedKeys.contains(ExportContent.PREFIX)) {
      String path = config.get(ExportContent.PARAM_EXPORT_PATH);
      if (StringUtil.isNullString(path)) {
        String tmpdir = config.get(ConfigManager.PARAM_TMPDIR);
        exportdir = new File(tmpdir, ExportContent.DEFAULT_EXPORT_DIR);
      } else {
        exportdir = new File(path);
      }
      if (!exportdir.exists()) {
        if (!FileUtil.ensureDirExists(exportdir)) {
          log.error("Could not create export directory " + exportdir);
        }
      }
    }
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
      exportEnabled = config.getBoolean(ExportContent.PARAM_ENABLE_EXPORT,
              ExportContent.DEFAULT_ENABLE_EXPORT);
      oiosamlEnabled = config.getBoolean(PARAM_OIOSAML_ENABLED,
          DEFAULT_OIOSAML_ENABLED);

      if (oiosamlEnabled) {
	// Specify the configured base directory for the OIOSAML home directory.
	oiosamlHomeDir = new File(config.get(PARAM_OIOSAML_HOMEDIR_PATH,
	    getDefaultTempRootDirectory()), DEFAULT_OIOSAML_HOMEDIR);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "oiosamHomeDir = '"
	    + oiosamlHomeDir.getAbsolutePath() + "'.");

	if (!oiosamlHomeDir.exists()) {
	  boolean oiosamlHomeDirCreated = oiosamlHomeDir.mkdirs();
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER
	      + "oiosamlHomeDirCreated = " + oiosamlHomeDirCreated);
	}

	oiosamlConfigFileName = config.get(PARAM_OIOSAML_CONFIG_FILE,
	    DEFAULT_OIOSAML_CONFIG_FILE);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "oiosamlConfigFileName = '" + oiosamlConfigFileName + "'.");

	// Specify the URLs protected by OIOSAML.
	oiosamlProtectedUrls = config.get(PARAM_OIOSAML_PROTECTED_URLS,
	   DEFAULT_OIOSAML_PROTECTED_URLS);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "oiosamlProtectedUrls = '"
	    + oiosamlProtectedUrls + "'.");
      }

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

  /** Return the directory in which to put generated export files */
  public File getExportDir() {
    return exportdir;
  }

  protected void installUsers() {
    installDebugUser();
    installPlatformUser();
    installGlobalUsers();
    installLocalUsers();
  }

  protected void configureContexts(HttpServer server) {
    try {
      if (true || logdir != null) {
        // Create context for serving log files and directory
        setupLogContext(server, realm, "/log/", logdir);
      }
      if (exportEnabled) {
        // Create context for serving exported files and directory
        setupDirContext(server, realm, "/export/", exportdir.toString(), null);
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
//       setupInfoContext(server);

      setupAdminContext(server);

      // no separate image context for now.  (Use if want different
      // access control or auth from / context
      // setupImageContext(server);

    } catch (Exception e) {
      log.warning("Couldn't create admin UI contexts", e);
    }
  }

  void setupAdminContext(HttpServer server) throws MalformedURLException {
    final String DEBUG_HEADER = "setupAdminContext(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    HttpContext context = makeContext(server, "/");

    // add handlers in the order they should be tried.

    // user authentication handler
    setContextAuthHandler(context, realm);
    
    // Specify the Spring configuration location for the CXF web services.
    context.setInitParameter("contextConfigLocation", "WEB-INF/beans.xml");

    // Create a servlet container.  ContextListenerWebApplicationHandler is a
    // WebApplicationHandler that allows the registration of servlet context
    // listeners, something that the Jetty 5 WebApplicationHandler ignores,
    // which prevents Apache CXF web services from working.
    // WebApplicationHandler is a ServletHandler that can apply filters
    // (e.g., compression) around servlets.
    ContextListenerWebApplicationHandler handler = makeWebAppHandler(context);
    addCompressionFilter(handler);

    // Initialize the OIOSAML system.
    if (oiosamlEnabled) {
      initializeOiosaml(handler);
    }

    // Add the Spring context listener needed for CXF web services.
    handler.addEventListener(new ContextLoaderListener());

    // Request dump servlet
    handler.addServlet("Dump", "/Dump", "org.mortbay.servlet.Dump");

    // Add all servlets in descrs array
    addServlets(getServletDescrs(), handler);
    // XXXUI add the new servlets too
    //addServlets(servletDescrsNew, handler);

    handler.addServlet("ProxyInfo", "/info/ProxyInfo",
            "org.lockss.servlet.ProxyConfig");
    addServletIfAvailable(handler, "Api", "/Api",
            "org.lockss.uiapi.servlet.Api");
    context.addHandler(handler);

    // ResourceHandler should come after servlets
    // find the htdocs directory, set as resource base
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    URL resourceUrl=loader.getResource("org/lockss/htdocs/");
    log.debug("Resource URL: " + resourceUrl);

    context.setResourceBase(resourceUrl.toString());
    if (redirectRootTo != null) {
      Map redirMap = new HashMap();
      redirMap.put("/", redirectRootTo);
      context.setAttribute(CONTEXT_ATTR_RESOURCE_REDIRECT_MAP, redirMap);
    }

    handler.addServlet("Resource", "/",
            "org.lockss.servlet.LockssResourceServlet");

//     // NotFoundHandler
//     context.addHandler(new NotFoundHandler());

    //       context.addHandler(new DumpHandler());

    if (log.isDebug3())
      log.debug3("handler.getFilters() = " + handler.getFilters());
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  void addCompressionFilter(WebApplicationHandler handler) {
    Configuration config = ConfigManager.getCurrentConfig();
    if (compressorEnabled) {
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
      // Workaround for parseDouble() bug
      handler.defineFilter("BuggyDoubleFilter",
              CompressingFilterWrapper.class.getName());
      handler.addFilterPathMapping("/*", "BuggyDoubleFilter",
              Dispatcher.__DEFAULT);

      handler.addFilterPathMapping("/*", filterName, Dispatcher.__DEFAULT);
    }
  }

  private void initializeOiosaml(WebApplicationHandler handler) {
    final String DEBUG_HEADER = "initializeOiosaml(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    try {
      // The OIOSAML configuration file.
      File configFile = new File(oiosamlHomeDir, oiosamlConfigFileName);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "configFile = "
	  + configFile.getAbsolutePath());

      // Write the OIOSAML configuration file if it does not exist.
      if (!configFile.exists()) {
	FileOutputStream fos = null;

	try {
	  fos = new FileOutputStream(configFile);
	  fos.write("# Properties used by OIOSAML\n".getBytes());
	} finally {
	  fos.close();
	}
      }

      // Notify the OIOSAML system of the configuration file location.
      // OIOSAML expects to receive this information via JNDI entries in the
      // web.xml file. Unfortunately, the version of Jetty being used does not
      // seem to be able to use JNDI properly, so we use the
      // setInitConfiguration() method of FileConfiguration as a workaround.
      Map<String, String> params = new HashMap<String, String>();
      params.put(Constants.INIT_OIOSAML_FILE, configFile.getCanonicalPath());

      ((FileConfiguration)SAMLConfigurationFactory.getConfiguration()).
      setInitConfiguration(params);

      // Add the OIOSAML filter.
      addOiosamlFilter(handler);

      // Add the OIOSAML context listener.
      handler.addEventListener(new SessionDestroyListener());
    } catch (Exception e) {
      log.error("Exception caught initializing OIOSAML: ", e);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  private void addOiosamlFilter(WebApplicationHandler handler) {
    final String DEBUG_HEADER = "addOiosamlFilter(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "oiosamlProtectedUrls = '"
	+ oiosamlProtectedUrls + "'.");

    String filterName = "OiosamlFilter";
    handler.defineFilter(filterName, LockssOiosamlSpFilter.class.getName());
    handler.addFilterPathMapping(oiosamlProtectedUrls, filterName,
	Dispatcher.__DEFAULT);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
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

//   void setupInfoContext(HttpServer server) {
//     HttpContext context = makeContext(server, "/info");

//     // add handlers in the order they should be tried.

//     // user authentication handler
//     setContextAuthHandler(context, realm);

//     // Create a servlet container
//     ServletHandler handler = new WebApplicationHandler();
//     handler.addServlet("j_security_check", "/j_security_check",
// 		       "org.mortbay.servlet.Dump");

//     handler.addServlet("ProxyInfo", "/ProxyInfo",
// 		       "org.lockss.servlet.ProxyConfig");
//     context.addHandler(handler);

//     // NotFoundHandler
//     context.addHandler(new NotFoundHandler());
//   }

  protected void setupLogContext(HttpServer server, UserRealm realm,
          String contextPath, String logdir)
          throws MalformedURLException {
    setupDirContext(server, realm, contextPath, logdir, null);
  }

  public Collection<UserSession> getUserSessions() {
    if (sessionMgr != null) {
      return sessionMgr.getUserSessions();
    }
    return null;
  }

  public Collection<HttpSession> getZombieSessions() {
    if (sessionMgr != null) {
      return sessionMgr.getZombieSessions();
    }
    return null;
  }
}
