/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.proxy;

import java.util.*;
import org.mortbay.http.*;

import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.alert.*;
import org.lockss.jetty.*;
import org.lockss.servlet.*;

/** LOCKSS proxy manager, starts main proxy.
 */
public class ProxyManager extends BaseProxyManager {
  public static final String SERVER_NAME = "Proxy";
  private static Logger log = Logger.getLogger("Proxy");

  public static final String PREFIX = Configuration.PREFIX + "proxy.";
  public static final String PARAM_START = PREFIX + "start";
  public static final boolean DEFAULT_START = true;

  public static final String PARAM_PORT = PREFIX + "port";
  public static final int DEFAULT_PORT = 9090;

  /** List of IP addresses to which to bind listen socket.  If not set,
   * server listens on all interfaces.  All listeners must be on the same
   * port, given by the <tt>port</tt> parameter.  Changing this requires
   * daemon restart. */
  public static final String PARAM_BIND_ADDRS = PREFIX + "bindAddrs";

  /** Proxy SSL listen port */
  public static final String PARAM_SSL_PORT = PREFIX + "sslPort";
  public static final int DEFAULT_SSL_PORT = -1;

  /** List of IP addresses to which to bind the SSL listen socket.  If not
   * set, server listens on all interfaces.  All listeners must be on the
   * same port, given by the <tt>sslPort</tt> parameter.  Change requires
   * proxy restart. */
  public static final String PARAM_SSL_BIND_ADDRS = PREFIX + "sslBindAddrs";
  public static final List DEFAULT_SSL_BIND_ADDRS = ListUtil.list("127.0.0.1");

  /** Host that CONNECT requests should connect to, in lieu of the one
   * specified in the request.  Intended to be pointed to the Proxy's
   * SSL listener. */
  static final String PARAM_CONNECT_ADDR =
    Configuration.PREFIX + "proxy.connectAddr";
  static final String DEFAULT_CONNECT_ADDR = "127.0.0.1";

  /** Name of managed keystore to use (see
   * org.lockss.keyMgr.keystore.<i>id</i>.name) */
  public static final String PARAM_SSL_KEYSTORE_NAME =
    PREFIX + "sslKeystoreName";

  public static final String IP_ACCESS_PREFIX = PREFIX + "access.ip.";
  public static final String PARAM_IP_INCLUDE = IP_ACCESS_PREFIX + "include";
  public static final String PARAM_IP_EXCLUDE = IP_ACCESS_PREFIX + "exclude";
  public static final String PARAM_IP_PLATFORM_SUBNET =
    IP_ACCESS_PREFIX + IpAccessControl.SUFFIX_PLATFORM_ACCESS;
  public static final String PARAM_LOG_FORBIDDEN =
    IP_ACCESS_PREFIX + "logForbidden";
  public static final boolean DEFAULT_LOG_FORBIDDEN = true;

  /** The log level at which to log all proxy accesses.  To normally log
   * all content accesses (proxy or ServeContent), set to <tt>info</tt>.
   * To disable set to <tt>none</tt>. */
  static final String PARAM_ACCESS_LOG_LEVEL = PREFIX + "accessLogLevel";
  static final String DEFAULT_ACCESS_LOG_LEVEL = "info";

  /** If true all content accesses raise an alert. */
  static final String PARAM_ACCESS_ALERTS_ENABLED =
    PREFIX + "accessAlertsEnabled";
  static final boolean DEFAULT_ACCESS_ALERTS_ENABLED = false;

  /** Log the start of proxy requests if true. */
  static final String PARAM_LOG_REQUEST_START = PREFIX + "logRequestStart";
  static final boolean DEFAULT_LOG_REQUEST_START = false;

  /** If true, exclude requests with the LOCKSS user agent from COUNTER
   * statistics. */
  static final String PARAM_EXCLUDE_LOCKSS_USER_AGENT_FROM_COUNTER =
    PREFIX + "excludeLockssUserAgentFromCounter";
  static final boolean DEFAULT_EXCLUDE_LOCKSS_USER_AGENT_FROM_COUNTER = true;

  /** If true, all incoming URLs will be minimally encoded */
  static final String PARAM_MINIMALLY_ENCODE_URLS =
    PREFIX + "minimallyEncodeUrls";
  static final boolean DEFAULT_MINIMALLY_ENCODE_URLS = true;

  /** If true, when content is served from the cache, the response headers
   * originally served with the content will be included in the response
   * (except for those normally not forwarded by proxies). */
  public static final String PARAM_COPY_STORED_RESPONSE_HEADERS =
    PREFIX + "copyStoredResponseHeaderss";
  public static final boolean DEFAULT_COPY_STORED_RESPONSE_HEADERS = true;

  /** If true, audit-related properties generated by LOCKSS will be
   * included in responses: Local checksum, Repair-From, Repair-Date. */
  public static final String PARAM_INCLUDE_LOCKSS_AUDIT_PROPS =
    PREFIX + "includeLockssAuditProperties";
  public static final boolean DEFAULT_INCLUDE_LOCKSS_AUDIT_PROPS = false;

  /** Determines whether the proxy interprets a {@value
   * org.lockss.util.Constants#X_LOCKSS_LOCAL_ADDRESS} request header.
   * <ul><li>If unset or null, the header is ignored if present,</li>
   * <li>If <tt>*</tt> or <tt>ANY</tt>, any local address is allowed,</li>
   * <li>else should be a list of allowed local addresses</li></ul>
   */
  public static final String PARAM_ALLOW_BIND_LOCAL_ADDRESSES =
    PREFIX + "allowBindLocalAddresses";
  public static final List<String> DEFAULT_ALLOW_BIND_LOCAL_ADDRESSES = null;

  /** If true, successive accesses to recently accessed content on the
   * cache does not trigger a request to the publisher */
  static final String PARAM_URL_CACHE_ENABLED = PREFIX + "urlCache.enabled";
  static final boolean DEFAULT_URL_CACHE_ENABLED = false;

  /** Duration during which successive accesses to a recently accessed URL
   * does not trigger a request to the publisher */
  static final String PARAM_URL_CACHE_DURATION = PREFIX + "urlCache.duration";
  static final long DEFAULT_URL_CACHE_DURATION = 1 * Constants.HOUR;

  /** The amount of time after which the "down" status of a host is
   * cleared, so that a request will once again cause a connection
   * attempt */
  static final String PARAM_HOST_DOWN_RETRY = PREFIX + "hostDownRetry";
  static final long DEFAULT_HOST_DOWN_RETRY = 10 * Constants.MINUTE;

  /** Cookie policy in effect for proxy connections to origin server. {@see
   * org.lockss.util.Constants}.  Set to <code>default</code> to use
   * daemon's default cookie policy, {@see
   * HttpClientUrlConnection#PARAM_COOKIE_POLICY}. */
  static final String PARAM_COOKIE_POLICY = PREFIX + "cookiePolicy";
  static final String DEFAULT_COOKIE_POLICY = "ignore";
  /** Set {@see #PARAM_COOKIE_POLICY} to this to have proxy use daemon's
   * default cookie policy, {@see
   * HttpClientUrlConnection#PARAM_COOKIE_POLICY}. */
  public static final String COOKIE_POLICY_DEFAULT = "default";

  public static final int HOST_DOWN_NO_CACHE_ACTION_504 = 1;
  public static final int HOST_DOWN_NO_CACHE_ACTION_QUICK = 2;
  public static final int HOST_DOWN_NO_CACHE_ACTION_NORMAL = 3;
  public static final int HOST_DOWN_NO_CACHE_ACTION_DEFAULT =
    HOST_DOWN_NO_CACHE_ACTION_NORMAL;

  /** For hosts believed to be down, this controls what we do with requests
   * for content not in the cache.  <ul><li><code>1</code>: Return
   * an immediate error.  <li><code>2</code>: Attempt to contact using
   * the quick-timeout connection pool.  <li><code>3</code>: Attempt
   * to contact normally.</ul>
   */
  static final String PARAM_HOST_DOWN_ACTION = PREFIX + "hostDownAction";
  static final int DEFAULT_HOST_DOWN_ACTION =
    HOST_DOWN_NO_CACHE_ACTION_DEFAULT;

  /** A semicolon-separated list of response codes for which the manifest
   * index should <b>not</b> be generated, or <code>all</code> to disable
   * manifest indices for all server responses.  (Index is always generated
   * if connection error (timeout, refuse, etc.) */
  static final String PARAM_NO_MANIFEST_INDEX_RESPONSES =
    PREFIX + "noManifestIndexResponses";
  static final String DEFAULT_NO_MANIFEST_INDEX_RESPONSES = "301;302;303;307";

  /** A semicolon-separated list of HTTP methods that the proxy should
   * never allow */
  static final String PARAM_DISALLOWED_METHODS =
    PREFIX + "disallowedMethods";
  static final List DEFAULT_DISALLOWED_METHODS = Collections.EMPTY_LIST;

  /** Maximum total outgoing connections to publisher sites, from Proxy or
   * ServeContent.  Should be larger than max total Proxy or ServeContent
   * threads */
  public static final String PARAM_PROXY_MAX_TOTAL_CONN =
    PREFIX + "connectionPool.max";
  public static final int DEFAULT_PROXY_MAX_TOTAL_CONN = 20;

  /** Maximum per-host outgoing connections to publisher sites, from Proxy
   * or ServeContent.  Should be larger than max total Proxy or
   * ServeContent threads */
  public static final String PARAM_PROXY_MAX_CONN_PER_HOST =
    PREFIX + "connectionPool.maxPerHost";
  public static final int DEFAULT_PROXY_MAX_CONN_PER_HOST = 10;

  // See comments regarding connect timeouts in HttpClientUrlConnection
  public static final String PARAM_PROXY_CONNECT_TIMEOUT =
    PREFIX + "timeout.connect";
  public static final long DEFAULT_PROXY_CONNECT_TIMEOUT =
    1 * Constants.MINUTE;
  public static final String PARAM_PROXY_DATA_TIMEOUT =
    PREFIX + "timeout.data";
  public static final long DEFAULT_PROXY_DATA_TIMEOUT =
    30 * Constants.MINUTE;

  public static final String PARAM_PROXY_QUICK_CONNECT_TIMEOUT =
    PREFIX + "quickTimeout.connect";
  public static final long DEFAULT_PROXY_QUICK_CONNECT_TIMEOUT =
    15 * Constants.SECOND;
  public static final String PARAM_PROXY_QUICK_DATA_TIMEOUT =
    PREFIX + "quickTimeout.data";
  public static final long DEFAULT_PROXY_QUICK_DATA_TIMEOUT =
    5  * Constants.MINUTE;

  public static final String PARAM_CLOSE_IDLE_CONNECTION_INTERVAL =
    PREFIX + "closeIdleConnections.interval";
  public static final long DEFAULT_CLOSE_IDLE_CONNECTION_INTERVAL =
    10 * Constants.MINUTE;

  public static final String PARAM_CLOSE_IDLE_CONNECTION_IDLE_TIME =
    PREFIX + "closeIdleConnections.idleTime";
  public static final long DEFAULT_CLOSE_IDLE_CONNECTION_IDLE_TIME =
    10 * Constants.MINUTE;

  /** Content Re-Writing Support - GIF to PNG */
  public static final String PARAM_REWRITE_GIF_PNG =
    PREFIX + "contentRewrite.gifToPng";
  public static final boolean DEFAULT_REWRITE_GIF_PNG =
    false;

  protected String getServerName() {
    return SERVER_NAME;
  }

  private long paramHostDownRetryTime = DEFAULT_HOST_DOWN_RETRY;
  private int paramHostDownAction = HOST_DOWN_NO_CACHE_ACTION_DEFAULT;
  private String paramCookiePolicy = null;
  private FixedTimedMap hostsDown = new FixedTimedMap(paramHostDownRetryTime);
  private Set hostsEverDown = new HashSet();
  private FixedTimedMap urlCache;
  private boolean paramUrlCacheEnabled = DEFAULT_URL_CACHE_ENABLED;
  private long paramUrlCacheDuration = DEFAULT_URL_CACHE_DURATION;
  private long paramCloseIdleConnectionsInterval =
    DEFAULT_CLOSE_IDLE_CONNECTION_INTERVAL;
  private long paramCloseIdleConnectionsIdleTime =
    DEFAULT_CLOSE_IDLE_CONNECTION_IDLE_TIME;
  private Set noManifestIndexResponses = new HashSet();
  private Set disallowedMethods = Collections.EMPTY_SET;
  private TimerQueue.Request closeTimer;
  private LockssUrlConnectionPool connPool = null;
  private LockssUrlConnectionPool quickConnPool = null;
  private int paramAccessLogLevel = -1;
  private boolean paramAccessAlertsEnabled = DEFAULT_ACCESS_ALERTS_ENABLED;
  private boolean paramMinimallyEncodeUrls = DEFAULT_MINIMALLY_ENCODE_URLS;
  private boolean paramLogReqStart = DEFAULT_LOG_REQUEST_START;
  private boolean paramExcludeLockssUserAgentFromCounter =
    DEFAULT_EXCLUDE_LOCKSS_USER_AGENT_FROM_COUNTER;
  private boolean paramIncludeLockssAuditProperties =
    DEFAULT_INCLUDE_LOCKSS_AUDIT_PROPS;
  private boolean paramCopyStoredResponseHeaders =
    DEFAULT_COPY_STORED_RESPONSE_HEADERS;
  private List<String> paramAllowBindLocalAddresses =
    DEFAULT_ALLOW_BIND_LOCAL_ADDRESSES;


  public void setConfig(Configuration config, Configuration prevConfig,
			Configuration.Differences changedKeys) {
    super.setConfig(config, prevConfig, changedKeys);
    if (changedKeys.contains(PREFIX)) {
      includeIps = config.get(PARAM_IP_INCLUDE, "");
      excludeIps = config.get(PARAM_IP_EXCLUDE, "");
      logForbidden = config.getBoolean(PARAM_LOG_FORBIDDEN,
				       DEFAULT_LOG_FORBIDDEN);
      log.debug("Installing new ip filter: incl: " + includeIps +
		", excl: " + excludeIps);
      setIpFilter();
      // access log is called by other components (ServeContent) so
      // configure even if not starting proxy.  Should be moved elsewhere
      try {
	String accessLogLevel = config.get(PARAM_ACCESS_LOG_LEVEL,
					   DEFAULT_ACCESS_LOG_LEVEL);
	paramAccessLogLevel = Logger.levelOf(accessLogLevel);
      } catch (RuntimeException e) {
	log.error("Couldn't set access log level", e);
	paramAccessLogLevel = -1;
      }	  
      paramAccessAlertsEnabled =
	config.getBoolean(PARAM_ACCESS_ALERTS_ENABLED,
			  DEFAULT_ACCESS_ALERTS_ENABLED);
      paramLogReqStart = config.getBoolean(PARAM_LOG_REQUEST_START,
					   DEFAULT_LOG_REQUEST_START);

      paramExcludeLockssUserAgentFromCounter =
	config.getBoolean(PARAM_EXCLUDE_LOCKSS_USER_AGENT_FROM_COUNTER,
			  DEFAULT_EXCLUDE_LOCKSS_USER_AGENT_FROM_COUNTER);

      port = config.getInt(PARAM_PORT, DEFAULT_PORT);
      start = config.getBoolean(PARAM_START, DEFAULT_START);

      paramMinimallyEncodeUrls =
	config.getBoolean(PARAM_MINIMALLY_ENCODE_URLS,
			  DEFAULT_MINIMALLY_ENCODE_URLS);
      paramIncludeLockssAuditProperties =
	config.getBoolean(PARAM_INCLUDE_LOCKSS_AUDIT_PROPS,
			  DEFAULT_INCLUDE_LOCKSS_AUDIT_PROPS);
      paramAllowBindLocalAddresses =
	config.getList(PARAM_ALLOW_BIND_LOCAL_ADDRESSES,
		       DEFAULT_ALLOW_BIND_LOCAL_ADDRESSES);
      paramCopyStoredResponseHeaders =
	config.getBoolean(PARAM_COPY_STORED_RESPONSE_HEADERS,
			  DEFAULT_COPY_STORED_RESPONSE_HEADERS);
      paramCloseIdleConnectionsInterval =
	config.getTimeInterval(PARAM_CLOSE_IDLE_CONNECTION_INTERVAL,
			       DEFAULT_CLOSE_IDLE_CONNECTION_INTERVAL);
      paramCloseIdleConnectionsIdleTime =
	config.getTimeInterval(PARAM_CLOSE_IDLE_CONNECTION_IDLE_TIME,
			       DEFAULT_CLOSE_IDLE_CONNECTION_IDLE_TIME);
      paramHostDownRetryTime =
	config.getTimeInterval(PARAM_HOST_DOWN_RETRY,
			       DEFAULT_HOST_DOWN_RETRY);
      synchronized (hostsDown) {
	hostsDown.setInterval(paramHostDownRetryTime);
      }
      paramHostDownAction = config.getInt(PARAM_HOST_DOWN_ACTION,
					  DEFAULT_HOST_DOWN_ACTION);
      paramCookiePolicy = config.get(PARAM_COOKIE_POLICY,
				     DEFAULT_COOKIE_POLICY);

      paramUrlCacheEnabled = config.getBoolean(PARAM_URL_CACHE_ENABLED,
					       DEFAULT_URL_CACHE_ENABLED);
      if (paramUrlCacheEnabled) {
	paramUrlCacheDuration =
	  config.getTimeInterval(PARAM_URL_CACHE_DURATION,
				 DEFAULT_URL_CACHE_DURATION);
	if (urlCache == null) {
	  urlCache = new FixedTimedMap(paramUrlCacheDuration);
	} else {
	  synchronized (urlCache) {
	    urlCache.setInterval(paramUrlCacheDuration);
	  }
	}
      }
      List<String> dis = config.getList(PARAM_DISALLOWED_METHODS,
					DEFAULT_DISALLOWED_METHODS);
      Set disSet = new HashSet();
      for (String str : dis) {
	disSet.add(str.toUpperCase());
      }
      disallowedMethods = disSet;

      String noindex = config.get(PARAM_NO_MANIFEST_INDEX_RESPONSES,
				  DEFAULT_NO_MANIFEST_INDEX_RESPONSES);
      if (StringUtil.isNullString(noindex)) {
	noManifestIndexResponses = new HashSet();
      } else if ("all".equalsIgnoreCase(noindex)) {
	noManifestIndexResponses = new UniversalSet();
      } else {
	Set noManIndSet = new HashSet();
	List lst = StringUtil.breakAt(noindex,
				      Constants.LIST_DELIM_CHAR,
				      0, true);
	for (Iterator iter = lst.iterator(); iter.hasNext(); ) {
	  String str = (String)iter.next();
	  try {
	    noManIndSet.add(new Integer(str));
	  } catch (NumberFormatException e) {
	    log.warning("Invalid response code in " +
			PARAM_NO_MANIFEST_INDEX_RESPONSES +
			": " + str + ", ignored");
	  }
	}
	noManifestIndexResponses = noManIndSet;
      }

      sslPort = config.getInt(PARAM_SSL_PORT, DEFAULT_SSL_PORT);
      bindAddrs = config.getList(PARAM_BIND_ADDRS, Collections.EMPTY_LIST);
      sslBindAddrs = config.getList(PARAM_SSL_BIND_ADDRS,
				    DEFAULT_SSL_BIND_ADDRS);

      sslKeystoreName = config.get(PARAM_SSL_KEYSTORE_NAME);
      setConnectAddr(config.get(PARAM_CONNECT_ADDR, DEFAULT_CONNECT_ADDR),
		     sslPort);

      if (start) {
	if (!isServerRunning() && getDaemon().isDaemonRunning()) {
	  startProxy();
	}
      } else if (isServerRunning()) {
	stopProxy();
      }
    }
  }

  protected void startProxy() {
    stopCloseTimer();
    super.startProxy();
    startCloseTimer();
  }

  protected void stopProxy() {
    stopCloseTimer();
    super.stopProxy();
  }

  private void startCloseTimer() {
    closeTimer =
      TimerQueue.schedule(Deadline.in(paramCloseIdleConnectionsInterval),
			  paramCloseIdleConnectionsInterval,
			  timerCallback, null);
  }

  private void stopCloseTimer() {
    if (closeTimer != null) {
      TimerQueue.cancel(closeTimer);
      closeTimer = null;
    }
  }
    
  private void closeIdleConnections() {
    if (handler != null) {
      handler.closeIdleConnections(paramCloseIdleConnectionsIdleTime);
    }
  }

  // TimerQueue callback singleton.
  private TimerQueue.Callback timerCallback =
    new TimerQueue.Callback() {
      public void timerExpired(Object cookie) {
	closeIdleConnections();
      }
      public String toString() {
	return "Proxy socket cleanup";
      }
    };

  public int getHostDownAction() {
    return paramHostDownAction;
  }

  public String getCookiePolicy() {
    return paramCookiePolicy;
  }

  /** @return the proxy port */
  public int getProxyPort() {
    return port;
  }

  // Proxy handler gets two connection pools, one to proxy normal request,
  // and one with short timeouts for checking with publisher before serving
  // content from cache.
  protected org.lockss.proxy.ProxyHandler makeProxyHandler() {
    setupConnectionPools();
    org.lockss.proxy.ProxyHandler handler =
      new org.lockss.proxy.ProxyHandler(getDaemon(), connPool, quickConnPool);
    handler.setErrorTemplate(errorTemplate);
    handler.setConnectAddr(connectHost, connectPort);
    if (sslPort > 0) {
      handler.setSslListenPort(sslPort);
    }
    return handler;
  }

  private void setupConnectionPools() {
    LockssUrlConnectionPool norm = new LockssUrlConnectionPool();
    LockssUrlConnectionPool quick = new LockssUrlConnectionPool();
    Configuration conf = ConfigManager.getCurrentConfig();

    int tot = conf.getInt(PARAM_PROXY_MAX_TOTAL_CONN,
			  DEFAULT_PROXY_MAX_TOTAL_CONN);
    int perHost = conf.getInt(PARAM_PROXY_MAX_CONN_PER_HOST,
			      DEFAULT_PROXY_MAX_CONN_PER_HOST);

    norm.setMultiThreaded(tot, perHost);
    quick.setMultiThreaded(tot, perHost);
    norm.setConnectTimeout
      (conf.getTimeInterval(PARAM_PROXY_CONNECT_TIMEOUT,
			    DEFAULT_PROXY_CONNECT_TIMEOUT));
    norm.setDataTimeout
      (conf.getTimeInterval(PARAM_PROXY_DATA_TIMEOUT,
			    DEFAULT_PROXY_DATA_TIMEOUT));
    quick.setConnectTimeout
      (conf.getTimeInterval(PARAM_PROXY_QUICK_CONNECT_TIMEOUT,
			    DEFAULT_PROXY_QUICK_CONNECT_TIMEOUT));
    quick.setDataTimeout
      (conf.getTimeInterval(PARAM_PROXY_QUICK_DATA_TIMEOUT,
			    DEFAULT_PROXY_QUICK_DATA_TIMEOUT));
    connPool = norm;
    quickConnPool = quick;
  }

  public LockssUrlConnectionPool getNormalConnectionPool() {
    if (connPool == null) {
      setupConnectionPools();
    }
    return connPool;
  }

  public LockssUrlConnectionPool getQuickConnectionPool() {
    if (quickConnPool == null) {
      setupConnectionPools();
    }
    return quickConnPool;
  }


  static final String LOCKSS_VIA_VERSION = "1.1";
  static final String LOCKSS_VIA_COMMENT = "(LOCKSS/jetty)";

  /** Create a Via header value:
   * 1.1 thishost:port (LOCKSS/Jetty)
   */
  public String makeVia(String host, int port) {
    StringBuilder sb = new StringBuilder();
    sb.append(LOCKSS_VIA_VERSION);
    sb.append(" ");
    sb.append(host);
    if (port != 0) {
      sb.append(":");
      sb.append(port);
    }
    sb.append(" ");
    sb.append(LOCKSS_VIA_COMMENT);
    return sb.toString();
  }

  /** Determine whether the request is from another LOCKSS cache asking for
   * a repair.  This is indicated by the including the string "Repair" as
   * (one of) the value(s) of the X-Lockss: header.
   */
  public boolean isRepairRequest(HttpRequest request) {
    Enumeration lockssFlagsEnum = request.getFieldValues(Constants.X_LOCKSS);
    if (lockssFlagsEnum != null) {
      while (lockssFlagsEnum.hasMoreElements()) {
	String val = (String)lockssFlagsEnum.nextElement();
	if (Constants.X_LOCKSS_REPAIR.equalsIgnoreCase(val)) {
	  return true;
	}
      }
    }
    return false;
  }

  public boolean isIncludeLockssAuditProps() {
    return paramIncludeLockssAuditProperties;
  }

  public boolean isCopyStoredResponseHeaders() {
    return paramCopyStoredResponseHeaders;
  }

  public List<String> getAllowedLocalAddress() {
    return paramAllowBindLocalAddresses;
  }

  /** Check whether the host is known to have been down recently */
  public boolean isHostDown(String host) {
    synchronized (hostsDown) {
      return hostsDown.containsKey(host);
    }
  }

  /** Remember that the host is down.
   * @param isInCache always done for content that's in the cache,
   * otherwise only if we've previously recorded this host down (which
   * means we have some of its content).
   */
  public void setHostDown(String host, boolean isInCache) {
    synchronized (hostsDown) {
      if (isInCache || hostsEverDown.contains(host)) {
	if (log.isDebug2()) log.debug2("Set host down: " + host);
	hostsDown.put(host, "");
	hostsEverDown.add(host);
      }
    }
  }

  /** Mark the url as having been accessed recently */
  public void setRecentlyAccessedUrl(String url) {
    if (paramUrlCacheEnabled && urlCache != null) {
      synchronized (urlCache) {
	urlCache.put(url, "");
      }
    }
  }

  /** Return true if the url has been accessed recently */
  public boolean isRecentlyAccessedUrl(String url) {
    if (paramUrlCacheEnabled && urlCache != null) {
      synchronized (urlCache) {
	return urlCache.containsKey(url);
      }
    } else {
      return false;
    }
  }

  /**
   * @return true iff this request should be counted in COUNTER statistics
   */
  public boolean isCounterCountable(String userAgent) {
    return !(paramExcludeLockssUserAgentFromCounter &&
	     StringUtil.equalStrings(LockssDaemon.getUserAgent(), userAgent));
  }

  public boolean isIpAuthorized(String ip) {
    try {
      return accessHandler.isIpAuthorized(ip);
    }
    catch (Exception exc) {
      return false;
    }
  }

  public boolean isMinimallyEncodeUrls() {
    return paramMinimallyEncodeUrls;
  }

  public boolean isLogReqStart() {
    return paramLogReqStart;
  }

  public boolean showManifestIndexForResponse(int status) {
    return ! noManifestIndexResponses.contains(new Integer(status));
  }

  public boolean isMethodAllowed(String method) {
    return ! disallowedMethods.contains(method);
  }



  public void logAccess(String method, String url, String msg,
			String remoteAddr, long reqElapsedTime) {
    if (reqElapsedTime >= 0) {
      msg += " in " + StringUtil.timeIntervalToString(reqElapsedTime);
    }
    logAccess(method, url, msg, remoteAddr);
  }

  public void logAccess(String method, String url, String msg,
			String remoteAddr) {
    String logmsg = "Proxy access from " + remoteAddr + ": " +
      method + " " + url + " : " + msg;
    if (paramAccessLogLevel >= 0) {
      log.log(paramAccessLogLevel, logmsg);
    }
    if (paramAccessAlertsEnabled) {
      alertMgr.raiseAlert(Alert.cacheAlert(Alert.CONTENT_ACCESS), logmsg);
    }
  }
}
