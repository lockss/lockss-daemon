/*
 * $Id$
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.jetty.*;
import org.lockss.proxy.*;
import org.mortbay.http.*;
import org.mortbay.http.handler.*;
import org.mortbay.jetty.servlet.*;
import com.github.ziplet.filter.compression.*;

/**
 * Content servlet starter
 */
public class ContentServletManager
  extends BaseServletManager {
  private static Logger log = Logger.getLogger("ServletMgr");

  /** Prefix for this server's config tree */
  public static final String PREFIX = Configuration.PREFIX + "contentui.";
  /** Jetty server name */
  public static final String SERVER_NAME = "Content";

  // All of the params that are defined in terms of SUFFIX_XXX are accessed
  // via the generic mechanism in BaseServletManager.setConfig().  The
  // PARAM_XXX symbols for those params are here for the ParamDoc tools;
  // some are also used by other packages to set the params referenced here
  public static final String PARAM_PORT = PREFIX + SUFFIX_PORT;
  public static final int DEFAULT_PORT = 8080;

  /** List of IP addresses to which to bind listen socket.  If not set,
   * server listens on all interfaces.  All listeners are on the same
   * port, given by the <tt>port</tt> parameter.  Changing this requires
   * daemon restart. */
  public static final String PARAM_BIND_ADDRS = PREFIX + SUFFIX_BIND_ADDRS;

  public static final String PARAM_START = PREFIX + SUFFIX_START;
  public static final boolean DEFAULT_START = false;

  public static final String ACCESS_PREFIX = ProxyManager.IP_ACCESS_PREFIX;

  public static final boolean DEFAULT_LOG_FORBIDDEN =
    ProxyManager.DEFAULT_LOG_FORBIDDEN;

  public static final String PARAM_403_MSG = PREFIX + SUFFIX_403_MSG;
  public static final String DEFAULT_403_MSG =
    "Access to the content on this LOCKSS box is not allowed from your IP address (%IP%)";

  /** Content UI does not require user auth */
  public static final boolean DO_USER_AUTH = false;

  protected ManagerInfo getManagerInfo() {
    ManagerInfo mi = new ManagerInfo();
    mi.prefix = PREFIX;
    mi.accessPrefix = ACCESS_PREFIX;
    mi.serverName = SERVER_NAME;
    mi.defaultStart = DEFAULT_START;
    mi.defaultPort = DEFAULT_PORT;
    mi.default403Msg = DEFAULT_403_MSG;
    mi.doAuth = DO_USER_AUTH;
    mi.defaultLogForbidden = DEFAULT_LOG_FORBIDDEN;
    return mi;
  }

  static final String PARAM_HELP_URL = PREFIX + "helpUrl";
  static final String DEFAULT_HELP_URL = "NoHelpYet";

  /** If set, fetches of the content root (http://cache:8080/) will be
   * redirected to this path (on same host and port) instead of serving the
   * index page of the root context. */
  public static final String PARAM_REDIRECT_ROOT = PREFIX + "redirectRoot";
  public static final String DEFAULT_REDIRECT_ROOT = "ServeContent";

  /** Display only the ServeContent servlet  */
  public static final String PARAM_CONTENT_ONLY = PREFIX + "contentOnly";
  public static final boolean DEFAULT_CONTENT_ONLY = false;

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

  // Descriptors for all content servlets.

  // ServeContent with no nav table
  public static final ServletDescr SERVLET_SERVE_CONTENT_NO_NAV =
    new ServletDescr("ServeContent",
		     ServeContent.class,
                     "Serve Content",
                     ServletDescr.NO_NAV_TABLE);

  public static final ServletDescr SERVLET_SERVE_CONTENT =
    new ServletDescr("ServeContent",
		     ServeContent.class,
                     "Serve Content",
                     ServletDescr.IN_NAV);
  public static final ServletDescr SERVLET_LIST_OBJECTS =
    new ServletDescr("ListObjects",
		     ListObjects.class,
                     "List Objects");
  protected static final ServletDescr LINK_HELP =
    new ServletDescr(null,
		     null,
                     "Help", DEFAULT_HELP_URL,
                     ServletDescr.PATH_IS_URL | ServletDescr.IN_NAV | ServletDescr.IN_UIHOME,
                     "Online help, FAQs, credits");

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

  static void setHelpUrl(String url) {
    LINK_HELP.path = url;
  }

  static String mailtoUrl(String addr) {
    return "mailto:" + addr;
  }

  // 
  static final ServletDescr servletDescrsNoNav[] = {
    SERVLET_SERVE_CONTENT_NO_NAV,
  };

  // All servlets must be listed here (even if not in nav table).
  // Order of descrs determines order in nav table.
  static final ServletDescr servletDescrs[] = {
      SERVLET_SERVE_CONTENT,
      SERVLET_LIST_HOLDINGS,
      //SERVLET_OPENURL_QUERY,
      SERVLET_LIST_OBJECTS,
      LINK_HELP,
  };

  public ServletDescr[] getServletDescrs() {
    if (CurrentConfig.getBooleanParam(PARAM_CONTENT_ONLY,
				      DEFAULT_CONTENT_ONLY)) {
      return servletDescrsNoNav;
    } else {
      return servletDescrs;
    }
  }

  private String redirectRootTo = DEFAULT_REDIRECT_ROOT;
  private LockssResourceHandler rootResourceHandler;
  private boolean compressorEnabled = DEFAULT_COMPRESSOR_ENABLED;

  public ContentServletManager() {
    super(SERVER_NAME);
  }

  public void setConfig(Configuration config, Configuration prevConfig,
			Configuration.Differences changedKeys) {
    super.setConfig(config, prevConfig, changedKeys);
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
      setHelpUrl(config.get(PARAM_HELP_URL, DEFAULT_HELP_URL));
      compressorEnabled = config.getBoolean(PARAM_COMPRESSOR_ENABLED,
					    DEFAULT_COMPRESSOR_ENABLED);
      startOrStop();
    }
  }

  private void setRedirectRootTo(LockssResourceHandler rh, String redTo) {
    rootResourceHandler.setRedirectRootTo(StringUtil.isNullString(redTo)
					  ? null : redTo);
  }

  protected void installUsers() {
  }

  protected void configureContexts(HttpServer server) {
    try {
      setupContentContext(server);
    } catch (Exception e) {
      log.warning("Couldn't create content UI contexts", e);
    }
  }

  void setupContentContext(HttpServer server) throws MalformedURLException {
    HttpContext context = makeContext(server, "/");

    // add handlers in the order they should be tried.

    // Create a servlet container.  WebApplicationHandler is a
    // ServletHandler that can apply filters (e.g., compression) around
    // servlets.
    WebApplicationHandler handler = new WebApplicationHandler();
    addCompressionFilter(handler);

//     // Request dump servlet
//     handler.addServlet("Dump", "/Dump", "org.mortbay.servlet.Dump");

    // Add all servlets in descrs array
    addServlets(getServletDescrs(), handler);

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

//     context.addHandler(new DumpHandler());
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
}
