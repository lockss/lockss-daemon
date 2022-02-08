/*

Copyright (c) 2000-2021 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.List;
import java.security.Principal;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.collections.*;
import org.apache.commons.collections.bidimap.*;
import org.apache.commons.collections.iterators.*;
import org.mortbay.html.*;
import org.mortbay.http.*;
import org.mortbay.servlet.MultiPartRequest;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.account.*;
import org.lockss.protocol.*;
import org.lockss.jetty.*;
import org.lockss.alert.*;
import org.lockss.servlet.ServletUtil.LinkWithExplanation;
import org.lockss.util.*;

import org.xnap.commons.i18n.I18n;

/** Abstract base class for LOCKSS servlets
 */
// SingleThreadModel causes servlet instances to be assigned to only a
// single thread (request) at a time.
public abstract class LockssServlet extends HttpServlet
  implements SingleThreadModel {
  
  private static final Logger log = Logger.getLogger(LockssServlet.class);

  /** A gettext-commons I18n object usable by all servlets. The object is
   * cached per-package (internally to the gettext-commons library). */
  protected static I18n i18n = I18nUtil.getI18n(LockssServlet.class);

  /** Available footnote citation styles.  Bracket: footnote number is
   * enclosed in brackets; SeparatedBracket: footnote number is enclosed in
   * brackets and multiple citations are space-separated; Plain: footnote
   * numbers are undecorated, and space-separated */
  public enum CitationStyle { Bracket, SeparatedBracket, Plain }

  // Constants
  static final String PARAM_LOCAL_IP = Configuration.PREFIX + "localIPAddress";

  /** Response charset */
  static final String PARAM_RESPONSE_CHARSET =
    Configuration.PREFIX + "ui.responseCharset";
  static final String DEFAULT_RESPONSE_CHARSET = "UTF-8";

  static final String PARAM_PLATFORM_VERSION =
    Configuration.PREFIX + "platform.version";

  /** Inactive HTTP session (cookie) timeout */
  static final String PARAM_UI_SESSION_TIMEOUT =
    Configuration.PREFIX + "ui.sessionTimeout";
  static final long DEFAULT_UI_SESSION_TIMEOUT = 2 * Constants.DAY;

  /** Maximum size of uploaded file accepted */
  static final String PARAM_MAX_UPLOAD_FILE_SIZE =
    Configuration.PREFIX + "ui.maxUploadFileSize";
  static final int DEFAULT_MAX_UPLOAD_FILE_SIZE = 500000;

  /** The warning string to display when the UI is disabled. */
  static final String PARAM_UI_WARNING =
    Configuration.PREFIX + "ui.warning";

  /** Footnote citation style.  Bracket: footnote numbers are enclosed in
   * brackets; SeparatedBracket: footnote numbers are enclosed in brackets
   * and multiple citations are space-separated; Plain: footnote numbers
   * are undecorated, and space-separated */
  static final String PARAM_CITATION_STYLE =
    Configuration.PREFIX + "ui.citationStyle";
  static final CitationStyle DEFAULT_CITATION_STYLE = CitationStyle.Plain;

  // session keys
  static final String SESSION_KEY_OBJECT_ID = "obj_id";
  static final String SESSION_KEY_OBJ_MAP = "obj_map";
  public static final String SESSION_KEY_RUNNING_SERVLET = "running_servlet";
  public static final String SESSION_KEY_REQUEST_HOST = "request_host";

  // Name given to form element whose value is the action that should be
  // performed when the form is submitted.  (Not always the submit button.)
  public static final String ACTION_TAG = "lockssAction";
  
  // Name of the parameters defining which Tab needs to be loaded
  // There values are a character between A and Z
  public static final String TAB_START_TAG = "start";
  public static final String TAB_END_TAG = "end";

  public static final String JAVASCRIPT_RESOURCE =
    "org/lockss/htdocs/admin.js";

  private static final String DOCTYPE =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\" \"http://www.w3.org/TR/REC-html40/loose.dtd\">";

  public static final String ATTR_INCLUDE_SCRIPT = "IncludeScript";
  public static final String ATTR_ALLOW_ROLES = "AllowRoles";

  /** User may configure admin access (add/delete/modify users, set admin
   * access list) */
  public static final String ROLE_USER_ADMIN = "userAdminRole";

  /** User may configure content access (set content access list) */
  public static final String ROLE_CONTENT_ADMIN = "contentAdminRole";

  /** User may change AU configuration (add/delete content) */
  public static final String ROLE_AU_ADMIN = "auAdminRole";

  /** User may access content) */
  public static final String ROLE_CONTENT_ACCESS = "accessContentRole";

  public static final String ROLE_DEBUG = "debugRole";

  protected ServletContext context;

  private LockssApp theApp = null;
  private ServletManager servletMgr;
  private AccountManager acctMgr;
  protected AlertManager alertMgr;

  // Request-local storage.  Convenient, but requires servlet instances
  // to be single threaded, and must ensure reset them to avoid carrying
  // over state between requests.
  protected HttpServletRequest req;
  protected HttpServletResponse resp;
  protected URL reqURL;
  protected HttpSession session;
  private String adminDir = null;
  protected String clientAddr;	// client addr, even if no param
  protected String localAddr;
  protected MultiPartRequest multiReq;
  protected long reqStartTime;

  private Vector footnotes;
  private int footNumber;
  private int tabindex;
  ServletDescr _myServletDescr = null;
  private String myName = null;

  // number submit buttons sequentially so unit tests can find them
  protected int submitButtonNumber = 0;

  /** Run once when servlet loaded. */
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    context = config.getServletContext();
    theApp =
      (LockssApp)context.getAttribute(ServletManager.CONTEXT_ATTR_LOCKSS_APP);
    servletMgr =
      (ServletManager)context.getAttribute(ServletManager.CONTEXT_ATTR_SERVLET_MGR);
    if (theApp instanceof LockssDaemon) {
      acctMgr = getLockssDaemon().getAccountManager();
      alertMgr = getLockssDaemon().getAlertManager();
    }
  }

  public ServletManager getServletManager() {
    return servletMgr;
  }

  protected ServletDescr[] getServletDescrs() {
    return servletMgr.getServletDescrs();
  }

  /** Servlets must implement this method. */
  protected abstract void lockssHandleRequest()
      throws ServletException, IOException;

  /** Common request handling. */
  public void service(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resetState();
    HttpSession session = req.getSession(false);
    try {
      this.req = req;
      this.resp = resp;
      if (log.isDebug()) {
	logParams();
      }
      resp.setCharacterEncoding(CurrentConfig.getParam(PARAM_RESPONSE_CHARSET,
                                                       DEFAULT_RESPONSE_CHARSET));
      resp.setContentType("text/html");

      if (!mayPageBeCached()) {
	resp.setHeader("pragma", "no-cache");
	resp.setHeader("Cache-control", "no-cache");
      }

      reqURL = new URL(UrlUtil.getRequestURL(req));
      clientAddr = getLocalIPAddr();

      // check that current user has permission to run this servlet
      if (!isServletAllowed(myServletDescr())) {
	displayWarningInLieuOfPage("You are not authorized to use " +
				   myServletDescr().heading);
	return;
      }

      // check whether servlet is disabled
      String reason =
	ServletUtil.servletDisabledReason(myServletDescr().getServletName());
      if (reason != null) {
	displayWarningInLieuOfPage("This function is disabled. " + reason);
	return;
      }
      if (session != null) {
	session.setAttribute(SESSION_KEY_RUNNING_SERVLET,
			     getHeading());
	String reqHost = req.getRemoteHost();
	String forw = req.getHeader(HttpFields.__XForwardedFor);
	if (!StringUtil.isNullString(forw)) {
	  reqHost += " (proxies for " + forw + ")";
	}
	session.setAttribute(SESSION_KEY_REQUEST_HOST, reqHost);
      }
      reqStartTime = TimeBase.nowMs();
      lockssHandleRequest();
    } catch (ServletException e) {
      log.error("Servlet threw", e);
      throw e;
    } catch (IOException e) {
      log.error("Servlet threw", e);
      throw e;
    } catch (RuntimeException e) {
      log.error("Servlet threw", e);
      throw e;
    } finally {
      if (session != null) {
	session.setAttribute(SESSION_KEY_RUNNING_SERVLET, null);
	session.setAttribute(LockssFormAuthenticator.__J_AUTH_ACTIVITY,
			     TimeBase.nowMs());
      }
      resetMyLocals();
      resetLocals();
    }
  }

  protected long reqStartTime() {
    return reqStartTime;
  }

  protected long reqElapsedTime() {
    return TimeBase.msSince(reqStartTime);
  }

  protected void resetState() {
    multiReq = null;
    footNumber = 0;
    submitButtonNumber = 0;
    tabindex = 1;
    statusMsg = null;
    errMsg = null;
    isFramed = false;
  }

  protected void resetLocals() {
  }

  protected void resetMyLocals() {
    // Don't hold on to stuff forever
    req = null;
    resp = null;
    session = null;
    reqURL = null;
    adminDir = null;
    localAddr = null;
    footnotes = null;
    _myServletDescr = null;
    myName = null;
    multiReq = null;
  }

  /** Return true if generated page may be cached (e.g., by browser).
   * Default is false as most servlets generate dynamic results */
  protected boolean mayPageBeCached() {
    return false;
  }

  /** Set the session timeout to the configured value */
  protected void setSessionTimeout(HttpSession session) {
    Configuration config = CurrentConfig.getCurrentConfig();
    setSessionTimeout(session,
		      config.getTimeInterval(PARAM_UI_SESSION_TIMEOUT,
					     DEFAULT_UI_SESSION_TIMEOUT));
  }

  /** Set the session timeout */
  protected void setSessionTimeout(HttpSession session, long time) {
    session.setMaxInactiveInterval((int)(time  / Constants.SECOND));
  }

  /** Get the current session, creating it if necessary (and set the
   * timeout if so) */
  protected HttpSession getSession() {
    if (session == null) {
      session = req.getSession(true);
      if (session.isNew()) {
	setSessionTimeout(session);
      }
    }
    return session;
  }

  /** Return true iff a session has already been established */
  protected boolean hasSession() {
    return req.getSession(false) != null;
  }

  /** Get an unused ID string for storing an object in the session */
  protected String getNewSessionObjectId() {
    HttpSession session = getSession();
    synchronized (session) {
      Integer id = (Integer)getSession().getAttribute(SESSION_KEY_OBJECT_ID);
      if (id == null) {
	id = new Integer(1);
      }
      session.setAttribute(SESSION_KEY_OBJECT_ID,
			   new Integer(id.intValue() + 1));
      return id.toString();
    }
  }    

  /** Get the object associated with the ID in the session */
  protected Object getSessionIdObject(String id) {
    HttpSession session = getSession();
    synchronized (session) {
      BidiMap map = (BidiMap)session.getAttribute(SESSION_KEY_OBJ_MAP);
      if (map == null) {
	return null;
      }
      return map.getKey(id);
    }
  }

  /** Get the String associated with the ID in the session */
  protected String getSessionIdString(String id) {
    return (String)getSessionIdObject(id);
  }

  /** Get the ID with which the object is associated in the session,
   * creating a new ID if the object doesn't already have one.  */
  protected String getSessionObjectId(Object obj) {
    HttpSession session = getSession();
    BidiMap map;
    synchronized (session) {
      map = (BidiMap)session.getAttribute(SESSION_KEY_OBJ_MAP);
      if (map == null) {
	map = new DualHashBidiMap();
	session.setAttribute(SESSION_KEY_OBJ_MAP, map);
      }
    }
    synchronized (map) {
      String id = (String)map.get(obj);
      if (id == null) {
	id = getNewSessionObjectId();
	map.put(obj, id);
      }
      return id;
    }
  }

  // Return descriptor of running servlet
  protected ServletDescr myServletDescr() {
    if (_myServletDescr == null) {
      _myServletDescr = servletMgr.findServletDescr(this);
    }
    return _myServletDescr;
  }

  // By default, servlet heading is in descr.  Override method to
  // compute other heading
  protected String getHeading(ServletDescr d) {
    if (d == null) return "Unknown Servlet";
    return d.heading;
  }

  protected String getHeading() {
    return getHeading(myServletDescr());
  }

  protected String getLocalIPAddr() {
    if (localAddr == null) {
      try {
	IPAddr localHost = IPAddr.getLocalHost();
	localAddr = localHost.getHostAddress();
      } catch (UnknownHostException e) {
	// shouldn't happen
	log.error("LockssServlet: getLocalHost: " + e.toString());
	return "???";
      }
    }
    return localAddr;
  }

  // Return IP addr used by LCAP.  If specified by (misleadingly named)
  // localIPAddress prop, might not really be our address (if we are
  // behind NAT).
  String getLcapIPAddr() {
    String ip = CurrentConfig.getParam(PARAM_LOCAL_IP);
    if (ip == null || ip.length() <= 0)  {
      return getLocalIPAddr();
    }
    return ip;
  }

  protected String getRequestHost() {
    return reqURL.getHost();
  }

  protected String getMachineName() {
    return PlatformUtil.getLocalHostname();
  }

  protected String getMachineIpAddr() {
    return CurrentConfig.getParam(ConfigManager.PARAM_PLATFORM_IP_ADDRESS);
  }

//   String getMachineName0() {
//     if (myName == null) {
//       // Return the canonical name of the interface the request was aimed
//       // at.  (localIPAddress prop isn't necessarily right here, as it
//       // might be the address of a NAT that we're behind.)
//       String host = reqURL.getHost();
//       try {
// 	IPAddr localHost = IPAddr.getByName(host);
// 	String ip = localHost.getHostAddress();
// 	myName = getMachineName(ip);
//       } catch (UnknownHostException e) {
// 	// shouldn't happen
// 	log.error("getMachineName", e);
// 	return host;
//       }
//     }
//     return myName;
//   }

//   String getMachineName(String ip) {
//     try {
//       IPAddr inet = IPAddr.getByName(ip);
//       return inet.getHostName();
//     } catch (UnknownHostException e) {
//       log.warning("getMachineName", e);
//     }
//     return ip;
//   }

  // return IP given name or IP
  protected String getMachineIP(String name) {
    try {
      IPAddr inet = IPAddr.getByName(name);
      return inet.getHostAddress();
    } catch (UnknownHostException e) {
      return null;
    }
  }

  boolean isServletLinkInNav(ServletDescr d) {
    return !isThisServlet(d) || linkMeInNav();
  }

  boolean isThisServlet(ServletDescr d) {
    return d == myServletDescr();
  }

  /** servlets may override this to determine whether they should be
   * a link in nav table */
  protected boolean linkMeInNav() {
    return false;
  }

  boolean isLargeLogo() {
    return myServletDescr().isLargeLogo();
  }

  // user predicates
  String getUsername() {
    Principal user = req.getUserPrincipal();
    return user != null ? user.toString() : null;
  }

  protected UserAccount getUserAccount() {
    if (acctMgr != null) {
      return acctMgr.getUser(getUsername());
    }
    return AccountManager.NOBODY_ACCOUNT;
  }

  protected boolean isDebugUser() {
    return doesUserHaveRole(ROLE_DEBUG);
  }

  protected boolean doesUserHaveRole(String role) {
    if ((req.isUserInRole(role) || req.isUserInRole(ROLE_USER_ADMIN))
	&& !hasNoRoleParsm(role)) {
      return true;
    }
    return hasTestRole(role);
  }

  static Map<String,String> noRoleParams = new HashMap<String,String>();
  static {
    noRoleParams.put(ROLE_USER_ADMIN, "noadmin");
    noRoleParams.put(ROLE_CONTENT_ADMIN, "nocontent");
    noRoleParams.put(ROLE_AU_ADMIN, "noau");
    noRoleParams.put(ROLE_CONTENT_ACCESS, "noaccess");
    noRoleParams.put(ROLE_DEBUG, "nodebug");
  }

  protected boolean hasNoRoleParsm(String roleName) {
    String noRoleParam = noRoleParams.get(roleName);
    return (noRoleParam != null &&
	    !StringUtil.isNullString(req.getParameter(noRoleParam)));
  }

  protected boolean hasTestRole(String role) {
    // Servlet test harness puts roles in context
    List roles = (List)context.getAttribute(ATTR_ALLOW_ROLES);
    return roles != null && (roles.contains(role)
			     || roles.contains(ROLE_USER_ADMIN));
  }


  protected boolean isServletRunnable(ServletDescr d) {
    return isServletAllowed(d) && isServletEnabled(d);
  }

  protected boolean isServletAllowed(ServletDescr d) {
    if (d.needsUserAdminRole() && !doesUserHaveRole(ROLE_USER_ADMIN))
      return false;
    if (d.needsContentAdminRole() && !doesUserHaveRole(ROLE_CONTENT_ADMIN))
      return false;
    if (d.needsAuAdminRole() && !doesUserHaveRole(ROLE_AU_ADMIN))
      return false;
    if (d.needsContentAccessRole() && !doesUserHaveRole(ROLE_CONTENT_ACCESS))
      return false;
    return true;
  }

  protected boolean isServletEnabled(ServletDescr d) {
    return d.isEnabled(getLockssDaemon());
  }

  protected boolean isServletDisplayed(ServletDescr d) {
    if (!isServletRunnable(d)) return false;
    if (d.needsDebugRole() && !doesUserHaveRole(ROLE_DEBUG))
      return false;
    return true;
  }

  protected boolean isServletInNav(ServletDescr d) {
    if (d.cls == ServletDescr.UNAVAILABLE_SERVLET_MARKER) return false;
    return d.isInNav(this) && isServletDisplayed(d);
  }

  // Called when a servlet doesn't get the parameters it expects/needs
  protected void paramError() throws IOException {
    // FIXME: As of 2006-03-15 this method and its only caller checkParam() are not called from anywhere
    PrintWriter wrtr = resp.getWriter();
    Page page = new Page();
    // add referer, params, msg to contact lockss unless from old bookmark
    // or manually entered url
    page.add("Parameter error");
    page.write(wrtr);
  }

  // return true iff error
  protected boolean checkParam(boolean ok, String msg) throws IOException {
    if (ok) return false;
    log.error(myServletDescr().getPath() + ": " + msg);
    paramError();
    return true;
  }

  /** Construct servlet URL
   */
  String srvURL(ServletDescr d) {
    return srvURL((String)null, d, null);
  }

  /** Construct servlet URL with params
   */
  String srvURL(ServletDescr d, String params) {
    return srvURL((String)null, d, params);
  }

  /** Construct servlet URL with params
   */
  String srvURL(ServletDescr d, Properties params) {
    return srvURL(d, concatParams(params));
  }

  /** Construct servlet absolute URL.
   */
  String srvAbsURL(ServletDescr d) {
    return srvURL(getRequestHost(), d, null);
  }

  /** Construct servlet absolute URL, with params as necessary.
   */
  String srvAbsURL(ServletDescr d, String params) {
    return srvURL(getRequestHost(), d, params);
  }

  /** Construct servlet URL, with params as necessary.  Avoid generating a
   *  hostname different from that used in the original request, or
   *  browsers will prompt again for login
   */
  String srvURL(String host, ServletDescr d, String params) {
    return srvURLFromStem(srvUrlStem(host), d, params);
  }

  String srvURL(PeerIdentity peer, ServletDescr d, String params) {
    return srvURLFromStem(peer.getUiUrlStem(reqURL.getPort()), d, params);
  }

  /** Construct servlet URL, with params as necessary.  Avoid generating a
   *  hostname different from that used in the original request, or
   *  browsers will prompt again for login
   */
  String srvURLFromStem(String stem, ServletDescr d, String params) {
    if (d.isPathIsUrl()) {
      return d.getPath();
    }
    StringBuilder sb = new StringBuilder(80);
    if (stem != null) {
      sb.append(stem);
      if (stem.charAt(stem.length() - 1) != '/') {
	sb.append('/');
      }
    } else {
      // ensure absolute path even if no scheme/host/port
      sb.append('/');
    }
    sb.append(d.getPath());
    if (params != null) {
      sb.append('?');
      sb.append(params);
    }
    return sb.toString();
  }

  String srvUrlStem(String host) {
    if (host == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    sb.append(reqURL.getProtocol());
    sb.append("://");
    sb.append(host);
    sb.append(':');
    sb.append(reqURL.getPort());
    return sb.toString();
  }

  /** Return a link to a servlet */
  String srvLink(ServletDescr d, String text) {
    return srvLink(d, text, (String)null);
  }

  /** Return a link to a servlet with params */
  String srvLink(ServletDescr d, String text, String params) {
    return new Link(srvURL(d, params),
		    (text != null ? text : d.heading)).toString();
  }

  /** Return a link to a servlet with params */
  String srvLink(ServletDescr d, String text, Properties params) {
    return new Link(srvURL(d, params),
		    text).toString();
  }

  /** Return a link to a servlet with params */
  String srvLinkWithId(ServletDescr d, String text,
		       String id, Properties params) {
    Link link = new Link(srvURL(d, params), text);
    if (!StringUtil.isNullString(id)) {
      link.attribute("id", id);
    }
    return link.toString();
  }

  /** Return an absolute link to a servlet with params */
  String srvAbsLink(ServletDescr d, String text, Properties params) {
    return srvAbsLink(d, text, concatParams(params));
  }

  /** Return an absolute link to a servlet with params */
  String srvAbsLink(ServletDescr d, String text, String params) {
    return new Link(srvAbsURL(d, params),
		    (text != null ? text : d.heading)).toString();
  }

  /** Return an absolute link to a servlet with params */
  String srvAbsLink(String host, ServletDescr d, String text, String params) {
    return new Link(srvURL(host, d, params),
		    (text != null ? text : d.heading)).toString();
  }

  /** Return an absolute link to a servlet with params */
  String srvAbsLink(PeerIdentity peer, ServletDescr d, String text,
		    String params) {
    return new Link(srvURL(peer, d, params),
		    (text != null ? text : d.heading)).toString();
  }

  /** Return text as a link iff isLink */
  String conditionalSrvLink(ServletDescr d, String text, String params,
			    boolean isLink) {
    if (isLink) {
      return srvLink(d, text, params);
    } else {
      return text;
    }
  }

  /** Return text as a link iff isLink */
  String conditionalSrvLink(ServletDescr d, String text, boolean isLink) {
    return conditionalSrvLink(d, text, null, isLink);
  }

  /** Concatenate params for URL string */
  static String concatParams(String p1, String p2) {
    if (StringUtil.isNullString(p1)) {
      return p2;
    }
    if (StringUtil.isNullString(p2)) {
      return p1;
    }
    return p1 + "&" + p2;
  }

  /** Concatenate params for URL string */
  String concatParams(Properties props) {
    if (props == null) {
      return null;
    }
    java.util.List list = new ArrayList();
    for (Iterator iter = props.keySet().iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      String val = props.getProperty(key);
      if (!StringUtil.isNullString(val)) {
	list.add(key + "=" + urlEncode(val));
      }
    }
    return StringUtil.separatedString(list, "&");
  }

  String modifyParams(String key, String val) {
    Properties props = getParamsAsProps();
    props.setProperty(key, val);
    return concatParams(props);
  }

  /** Return the request parameters as a Properties.  Only the first value
   * of multivalued parameters is included. */
  Properties getParamsAsProps() {
    Properties props = new Properties();
    for (Enumeration en = req.getParameterNames(); en.hasMoreElements(); ) {
      String name = (String)en.nextElement();
      props.setProperty(name, req.getParameter(name));
    }
    return props;
  }

  /** Return the request parameters as a Map<String,String>.  Only the
   * first value of multivalued parameters is included. */
  Map<String,String> getParamsAsMap() {
    Map<String,String> map = new HashMap<String,String>();
    for (Enumeration en = req.getParameterNames(); en.hasMoreElements(); ) {
      String name = (String)en.nextElement();
      map.put(name, req.getParameter(name));
    }
    return map;
  }

  protected String urlEncode(String param) {
    return UrlUtil.encodeUrl(param);
  }

  protected String getRequestKey() {
    String key = req.getPathInfo();
    if (key != null && key.startsWith("/")) {
      return key.substring(1);
    }
    return key;
  }

  /** Common page setup. */
  protected Page newPage() {
    // Compute heading
    String heading = getHeading();
    if (heading == null) {
      heading = "Box Administration";
    }

    // Create page and layout header
    Page page = ServletUtil.doNewPage(getPageTitle(), isFramed());
    Iterator inNavIterator;
    if (myServletDescr().hasNoNavTable()) {
      inNavIterator = CollectionUtil.EMPTY_ITERATOR;
    } else {
      inNavIterator = new FilterIterator(
        new ObjectArrayIterator(getServletDescrs()),
        new Predicate() {
          public boolean evaluate(Object obj) {
            return isServletInNav((ServletDescr)obj);
          }
        });
    }
    ServletUtil.layoutHeader(this,
                             page,
                             heading,
                             isLargeLogo(),
                             getMachineName(),
                             getMachineIpAddr(),
                             getLockssApp().getStartDate(),
                             inNavIterator);
    String warnMsg = servletMgr.getWarningMsg();
    if (warnMsg != null) {
      Composite warning = new Composite();
      warning.add("<center><font color=red size=+1>");
      warning.add(warnMsg);
      warning.add("</font></center><br>");
      page.add(warning);
    }
    return page;
  }

  protected Page addBarePageHeading(Page page) {
// FIXME: Move the following fragment elsewhere
// It causes the doctype statement to appear in the middle,
// after the <body> tag.
    page.add("<!doctype html public \"-//w3c//dtd html 4.0 transitional//en\">");
//     page.addHeader("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">");
//     page.addHeader("<meta http-equiv=\"content-type\" content=\"text/html;charset=ISO-8859-1\">");
    page.addHeader("<link rel=\"shortcut icon\" href=\"/favicon.ico\" type=\"image/x-icon\" />");
    return page;
  }

  private boolean isFramed = false;

  protected String errMsg;

  protected String statusMsg;

  protected boolean isFramed() {
    return isFramed;
  }

  protected void setFramed(boolean v) {
    isFramed = v;
  }

  protected String getPageTitle() {
    StringBuilder sb = new StringBuilder();
    if (ServletUtil.isHostNameInTitle()) {
      sb.append(getMachineName());
    } else {
      sb.append("LOCKSS");
    }
    String heading = getTitleHeading();
    if (heading != null) {
      sb.append(": ");
      sb.append(heading);
    }
    return sb.toString();
  }

  /** By default, servlet-specific part of page title is the same as its
   * page heading. */
  protected String getTitleHeading() {
    return getHeading();
  }

  /** Return a button that invokes the javascript submit routine with the
   * specified action */
  protected Element submitButton(String label, String action) {
    return submitButton(label, action, null, null);
  }

  /** Return a button that invokes javascript when clicked. */
  Input jsButton(String label, String js) {
    Input btn = new Input("button", null);
    btn.attribute("value", label);
    setTabOrder(btn);
    btn.attribute("onClick", js);
    return btn;
  }

  /** Return a button that invokes the javascript submit routine with the
   * specified action, first storing the value in the specified form
   * prop. */
  protected Element submitButton(String label, String action,
				 String prop, String value) {
    StringBuilder sb = new StringBuilder(40);
    sb.append("lockssButton(this, '");
    sb.append(action);
    sb.append("'");
    if (prop != null && value != null) {
      sb.append(", '");
      sb.append(prop);
      sb.append("', '");
      sb.append(value);
      sb.append("'");
    }
    sb.append(")");
    Input btn = jsButton(label, sb.toString());
    btn.attribute("id", "lsb." + (++submitButtonNumber));
    return btn;
  }

  /** Return a (possibly labelled) checkbox.
   * @param label appears to right of checkbox if non null
   * @param value value included in result set if box checked
   * @param key form key to which result set is assigned
   * @param checked if true, box is initially checked
   * @return a checkbox Element
   */
  Element checkBox(String label, String value, String key, boolean checked) {
    Input in = new Input(Input.Checkbox, key, value);
    if (checked) {
      in.check();
    }
    setTabOrder(in);
    if (StringUtil.isNullString(label)) {
      return in;
    } else {
      Composite c = new Composite();
      c.add(in);
      c.add(" ");
      c.add(label);
      return c;
    }
  }

  /** Return a labelled rasio button
   * @param label label to right of circle, and form value if checked
   * @param key form key to which value is assigned
   * @param checked if true, is initially checked
   * @return a readio button Element
   */
  protected Element radioButton(String label, String key, boolean checked) {
    return radioButton(label, label, key, checked);
  }

  /** Return a labelled rasio button
   * @param label appears to right of circle if non null
   * @param value value assigned to key if box checked
   * @param key form key to which value is assigned
   * @param checked if true, is initially checked
   * @return a readio button Element
   */
  protected Element radioButton(String label, String value,
			       String key, boolean checked) {
    Composite c = new Composite();
    Input in = new Input(Input.Radio, key, value);
    if (checked) {
      in.check();
    }
    setTabOrder(in);
    c.add(in);
    c.add(label);
    return c;
  }

  /** Add html tags to grey the text if isGrey is true */
  protected String greyText(String txt, boolean isGrey) {
    if (!isGrey) {
      return txt;
    }
    return "<font color=gray>" + txt + "</font>";
  }

  /** Set this element next in the tab order.  Returns the element for
   * easier nesting in expressions. */
  protected Element setTabOrder(Element ele) {
    ele.attribute("tabindex", tabindex++);
    return ele;
  }

  /**
   * Increment and return the tab index so that it can be applied to an element
   * which is not a Jetty Element.
   */
  protected int getNextTabIndex() {
    return tabindex++;
  }

  /** Store a footnote, assign it a number, return html for footnote
   * reference.  If footnote in null or empty, no footnote is added and an
   * empty string is returned.  Footnote numbers get turned into links;
   * <b>Do not put the result of addFootnote inside a link!</b>.  */
  protected String addFootnote(String s) {
    return addFootnote(s, false);
  }

  /** Store a footnote, assign it a number, return html for footnote
   * reference.  If footnote in null or empty, no footnote is added and an
   * empty string is returned.  Footnote numbers get turned into links;
   * <b>Do not put the result of addFootnote inside a link!</b>.  */
    protected String addFootnote(String s, boolean withLeadingSpace) {
    if (s == null || s.length() == 0) {
      return "";
    }
    if (footNumber == 0) {
      if (footnotes == null) {
	footnotes = new Vector(10, 10);
      } else {
	footnotes.removeAllElements();
      }
    }
    int n = footnotes.indexOf(s);
    if (n < 0) {
      n = footNumber++;
      footnotes.addElement(s);
    }
    StringBuilder sb = new StringBuilder();
    sb.append("<sup><font size=-1>");
    CitationStyle citationStyle =
      (CitationStyle)
      ConfigManager.getCurrentConfig().getEnum(CitationStyle.class,
                                               PARAM_CITATION_STYLE,
                                               DEFAULT_CITATION_STYLE);
    switch (citationStyle) {
    case SeparatedBracket:
      if (withLeadingSpace) {
        sb.append("&nbsp;");
      }
    case Bracket:
      sb.append("<a href=#foottag");
      sb.append((n+1));
      sb.append(">[");
      sb.append((n+1));
      sb.append("]");
      break;
    case Plain:
    default:
      if (withLeadingSpace) {
        sb.append("&nbsp;");
      }
      sb.append("<a href=#foottag");
      sb.append((n+1));
      sb.append(">");
      sb.append((n+1));
    }
    sb.append("</a></font></sup>");
    return sb.toString();
//     return "<sup><font size=-1><a href=#foottag" + (n+1) + ">" +
//       (n+1) + "</a></font></sup>";
  }

  /** Add javascript to page.  Normally adds a link to the script file, but
   * can be told to include the script directly in the page, to accomodate
   * unit testing of individual servlets, when other fetches won't work. */
  protected void addJavaScript(Composite comp) {
    String include = (String)context.getAttribute(ATTR_INCLUDE_SCRIPT);
    if (StringUtil.isNullString(include)) {
      linkToJavaScript(comp);
    } else {
      includeJavaScript0(comp);
    }
  }

  private void includeJavaScript0(Composite comp) {
    Script script = new Script(getJavascript());
    comp.add(script);
  }

  private void linkToJavaScript(Composite comp) {
    Script script = new Script("");
    script.attribute("src", "admin.js");
    comp.add(script);
  }

  private static String jstext = null;

  private static synchronized String getJavascript() {
    if (jstext == null) {
    InputStream istr = null;
      try {
	ClassLoader loader = Thread.currentThread().getContextClassLoader();
	istr = loader.getResourceAsStream(JAVASCRIPT_RESOURCE);
	jstext = StringUtil.fromInputStream(istr);
	istr.close();
      } catch (Exception e) {
	log.error("Can't load javascript", e);
      } finally {
    	IOUtil.safeClose(istr);
      }
    }
    return jstext;
  }

  /** Display a message in lieu of the normal page
   */
  protected void displayMsgInLieuOfPage(String msg) throws IOException {
    // TODO: Look at HTML
    Page page = newPage();
    Composite warning = new Composite();
    warning.add(msg);
    warning.add("<br>");
    page.add(warning);
    layoutFooter(page);
    page.write(resp.getWriter());
  }

  /** Display a warning in red, in lieu of the normal page
   */
  protected void displayWarningInLieuOfPage(String msg) throws IOException {
    displayMsgInLieuOfPage("<center><font color=red size=+1>" + msg +
			   "</font></center>");
  }

  /** Display "The cache isn't ready yet, come back later"
   */
  protected void displayNotStarted() throws IOException {
    displayWarningInLieuOfPage("This LOCKSS box is still starting.  Please "
			       + srvLink(myServletDescr(), "try again",
					 getParamsAsProps())
			       + " in a moment.");
  }

  protected void endPage(Page page) throws IOException {
    layoutFooter(page);
    writePage(page);
  }

  protected void endPageNoFooter(Page page) throws IOException {
    ServletUtil.addNotes(page,
			 (footnotes == null ? null : footnotes.iterator()));
    writePage(page);
  }

  protected void writePage(Page page) throws IOException {
    if ("please".equalsIgnoreCase(req.getHeader("X-Lockss-Result"))) {
      boolean success = (errMsg == null);
      log.debug3("X-Lockss-Result: " + (success ? "Ok" : "Fail"));
      resp.setHeader("X-Lockss-Result", success ? "Ok" : "Fail");
    }
    PrintWriter wrtr = resp.getWriter();
    wrtr.println(DOCTYPE);
    page.write(wrtr);
  }

  public MultiPartRequest getMultiPartRequest()
      throws FormDataTooLongException, IOException {
    int maxUpload = CurrentConfig.getIntParam(PARAM_MAX_UPLOAD_FILE_SIZE,
					      DEFAULT_MAX_UPLOAD_FILE_SIZE);
    return getMultiPartRequest(maxUpload);
  }

  public MultiPartRequest getMultiPartRequest(int maxLen)
      throws FormDataTooLongException, IOException {
    if (req.getContentType() == null ||
	!req.getContentType().startsWith("multipart/form-data")) {
      return null;
    }
    if (req.getContentLength() > maxLen) {
      throw new FormDataTooLongException(req.getContentLength() + " bytes, " +
					 maxLen + " allowed");
    }
    MultiPartRequest multi = new MultiPartRequest(req);
    if (log.isDebug2()) {
      String[] parts = multi.getPartNames();
      log.debug3("Multipart request, " + parts.length + " parts");
      if (log.isDebug3()) {
	for (int p = 0; p < parts.length; p++) {
	  String name = parts[p];
	  String cont = multi.getString(parts[p]);
	  log.debug3(name + ": " + cont);
	}
      }
    }
    multiReq = multi;
    return multi;
  }

  public String getParameter(String name) {
    String val = req.getParameter(name);
    if (val == null && multiReq != null) {
      val = multiReq.getString(name);
    }
    if (val == null) {
      return null;
    }
    val = StringUtils.strip(val, " \t");
//     if (StringUtil.isNullString(val)) {
    if ("".equals(val)) {
      return null;
    }
    return val;
  }

  protected void layoutFooter(Page page) {
    ServletUtil.doLayoutFooter(page,
                              (footnotes == null ? null : footnotes.iterator()),
                              getLockssApp().getVersionInfo());
    if (footnotes != null) {
      footnotes.removeAllElements();
    }
  }

  /** Return the app instance.
   */
  protected LockssApp getLockssApp() {
    return theApp;
  }

  /** Return the daemon instance, assumes that the servlet is running in
   * the daemon.
   * @throws ClassCastException if the servlet is running in an app other
   * than the daemon
   */
  protected LockssDaemon getLockssDaemon() {
    return (LockssDaemon)theApp;
  }

  protected void logParams() {
    Enumeration en = req.getParameterNames();
    while (en.hasMoreElements()) {
      String name = (String)en.nextElement();
      String vals[];
      String dispval;
      if (StringUtil.indexOfIgnoreCase(name, "passw") >= 0) {
	dispval = req.getParameter(name).length() == 0 ? "" : "********";
      } else if (log.isDebug2()
		 && (vals = req.getParameterValues(name)).length > 1) {
	dispval = StringUtil.separatedString(vals, ", ");
      } else {
	dispval = req.getParameter(name);
      }
      log.debug(name + " = " + dispval);
    }
  }

  /** Convenience method */
  protected String encodeText(String s) {
    return HtmlUtil.encode(s, HtmlUtil.ENCODE_TEXT);
  }

  /** Convenience method */
  protected String encodeTextArea(String s) {
    return HtmlUtil.encode(s, HtmlUtil.ENCODE_TEXTAREA);
  }

  /** Convenience method */
  protected String encodeAttr(String s) {
    return HtmlUtil.encode(s, HtmlUtil.ENCODE_ATTR);
  }

  /** Create message and error message block
   * @param composite TODO*/
  protected void layoutErrorBlock(Composite composite) {
    if (errMsg != null || statusMsg != null) {
      ServletUtil.layoutErrorBlock(composite, errMsg, statusMsg);
    }
  }


  /**
   * Sends the browser a response with the given status code and a brief page
   * displaying the given message. Although sendShortMessage might seem to
   * duplicate resp.sendError(int status, String msg), resp.sendError
   * incorrectly sets the status code description text to its second argument.
   * So the HTTP status becomes something like "400 Here is a my long
   * descriptive error message" instead of "400 Bad Request".
   *
   * @param statusCode status code to use with the response
   * @param message message to display on the error page and quote in the
   * 	      debug log
   * @throws java.io.IOException if the page cannot be written to the response
   */
  protected void sendShortMessage(int statusCode, String message)
      throws IOException {
    resp.setStatus(statusCode);
    if (log.isDebug()) {
      log.debug("Unsuccessful Service request, response status " +
          statusCode + ": " + message);
    }
    oneLineMessagePage(message);
  }

  /**
   * Sends the browser a page with the given line of text and the LOCKSS
   * boilerplate.  Does not set the status code.
   *
   * @param message the message to display
   * @throws java.io.IOException if the page cannot be written to the response
   */
  protected void oneLineMessagePage(String message) throws IOException {
    Page page = newPage();
    Composite comp = new Composite();
    comp.add(message);
    page.add(comp);
    endPage(page);
  }

  /** Exception thrown if multipart form data is longer than the
   * caller-supplied max */
  public static class FormDataTooLongException extends Exception {
    public FormDataTooLongException(String message) {
      super(message);
    }
  }

  /**
   * Adds the required CSS file locations to the page header.
   * 
   * @param page
   *          A Page representing the HTML page.
   */
  protected void addCssLocations(Page page) {
    page.add(new StyleLink("/css/lockss-new.css"));
    page.add(new StyleLink("/css/jquery-ui-1.8.css"));
  }

  /**
   * Adds the required jQuery JavaScript file locations to the page header.
   * 
   * @param page
   *          A Page representing the HTML page.
   */
  protected void addJQueryLocations(Page page) {
    addJavaScriptLocation(page, "js/jquery-1.6.2.js");
    addJavaScriptLocation(page, "js/jquery-ui.min-1.8.js");
    addJavaScriptLocation(page, "js/auDetails-new.js");
    addJavaScriptLocation(page, "js/jquery.tristate.js");
    addJavaScriptLocation(page, "js/jquery.shiftclick.js");
  }

  protected void addJSXLocation(Page page, String jsxLocation) {
    Script jsxScript = new Script("");
    jsxScript.attribute("src", jsxLocation);
    jsxScript.attribute("type", "text/babel");
    page.add(jsxScript);
  }

  /**
   * Adds the required ReactJS JavaScript file locations to the page header.
   *
   * @param page
   *          A Page representing the HTML page.
   */
  protected void addReactJSLocations(Page page) {
    addJavaScriptLocation(page, "js/babel-6.26.0.min.js");
    addJavaScriptLocation(page, "js/react-17.0.2.js");
    addJavaScriptLocation(page, "js/react-dom-17.0.2.js");
  }

  /**
   * Adds a JavaScript file location to the page header.
   * 
   * @param page
   *          A Page representing the HTML page.
   * @param jsLocation
   *          A String with the location of the JavaScript file.
   */
  protected void addJavaScriptLocation(Page page, String jsLocation) {
    Script ajaxScript = new Script("");
    ajaxScript.attribute("src", jsLocation);
    ajaxScript.attribute("type", "text/javascript");
    page.add(ajaxScript);
  }

  /**
   * <p>Makes a new link with explanation.</p>
   * <p>The resulting link is always enabled.</p>
   * @param descr    The link's servlet descriptor.
   * @param linkText The text appearing in the link.
   * @param action   The action associated with the servlet descriptor
   *                 (can be null).
   * @param expl     The explanation associated with the link.
   * @return A {@link LinkWithExplanation} corresponding to the servlet
   *         descriptor (optionally with an action), showing the given
   *         text and explanation.
   */
  protected LinkWithExplanation getMenuDescriptor(ServletDescr descr,
      						  String linkText,
      						  String action,
      						  String expl) {
    return getMenuDescriptor(descr, linkText, action, expl, true);
  }

  /**
   * <p>Makes a new link with explanation.</p>
   * @param descr      The link's servlet descriptor.
   * @param linkText   The text appearing in the link.
   * @param linkAction The action associated with the servlet descriptor
   *                   (can be null).
   * @param linkExpl   The explanation associated with the link.
   * @param enabled    Whether or not the link is actually enabled.
   * @return A {@link LinkWithExplanation} corresponding to the servlet
   *         descriptor (optionally with an action), showing the given
   *         text and explanation; the link is enabled or disabled
   *         according to the parameter.
   */
  protected LinkWithExplanation getMenuDescriptor(ServletDescr descr,
      						  String linkText,
      						  String linkAction,
      						  String linkExpl,
      						  boolean enabled) {
    return new LinkWithExplanation(
      enabled ? srvLink(descr, linkText, linkAction)
	  : ServletUtil.gray(linkText), linkExpl);
  }

  /**
   * Adds a JavaScript file location to a form.
   * 
   * @param form
   *          A Form representing the HTML form.
   * @param jsLocation
   *          A String with the location of the JavaScript file.
   */
  protected void addFormJavaScriptLocation(Form form, String jsLocation) {
    Script ajaxScript = new Script("");
    ajaxScript.attribute("src", jsLocation);
    ajaxScript.attribute("type", "text/javascript");
    form.add(ajaxScript);
  }
}
