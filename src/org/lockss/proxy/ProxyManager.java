/*
 * $Id: ProxyManager.java,v 1.43 2007-07-31 06:30:09 tlipkis Exp $
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

  public static final String IP_ACCESS_PREFIX = PREFIX + "access.ip.";
  public static final String PARAM_IP_INCLUDE = IP_ACCESS_PREFIX + "include";
  public static final String PARAM_IP_EXCLUDE = IP_ACCESS_PREFIX + "exclude";
  public static final String PARAM_IP_PLATFORM_SUBNET =
    IP_ACCESS_PREFIX + IpAccessControl.SUFFIX_PLATFORM_ACCESS;
  public static final String PARAM_LOG_FORBIDDEN =
    IP_ACCESS_PREFIX + "logForbidden";
  public static final boolean DEFAULT_LOG_FORBIDDEN = true;

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
  static final String COOKIE_POLICY_DEFAULT = "default";

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
  static final String DEFAULT_DISALLOWED_METHODS = HttpRequest.__CONNECT;

  public static final String PARAM_PROXY_MAX_TOTAL_CONN =
    PREFIX + "connectionPool.max";
  public static final int DEFAULT_PROXY_MAX_TOTAL_CONN = 15;

  public static final String PARAM_PROXY_MAX_CONN_PER_HOST =
    PREFIX + "connectionPool.maxPerHost";
  public static final int DEFAULT_PROXY_MAX_CONN_PER_HOST = 2;

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
  private Set noManifestIndexResponses = new HashSet();
  private Set disallowedMethods = new HashSet();


  public void setConfig(Configuration config, Configuration prevConfig,
			Configuration.Differences changedKeys) {
    super.setConfig(config, prevConfig, changedKeys);
    if (changedKeys.contains(PREFIX)) {
      port = config.getInt(PARAM_PORT, DEFAULT_PORT);
      start = config.getBoolean(PARAM_START, DEFAULT_START);

      if (start) {
	includeIps = config.get(PARAM_IP_INCLUDE, "");
	excludeIps = config.get(PARAM_IP_EXCLUDE, "");
	logForbidden = config.getBoolean(PARAM_LOG_FORBIDDEN,
					 DEFAULT_LOG_FORBIDDEN);
	log.debug("Installing new ip filter: incl: " + includeIps +
		  ", excl: " + excludeIps);
	setIpFilter();

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

	String noindex = config.get(PARAM_NO_MANIFEST_INDEX_RESPONSES,
				    DEFAULT_NO_MANIFEST_INDEX_RESPONSES);
	if (StringUtil.isNullString(noindex)) {
	  noManifestIndexResponses = new HashSet();
	} else if ("all".equalsIgnoreCase(noindex)) {
	  noManifestIndexResponses = new UniversalSet();
	} else {
	  Set newSet = new HashSet();
	  List lst = StringUtil.breakAt(noindex,
					Constants.LIST_DELIM_CHAR,
					0, true);
	  for (Iterator iter = lst.iterator(); iter.hasNext(); ) {
	    String str = (String)iter.next();
	    try {
	      newSet.add(new Integer(str));
	    } catch (NumberFormatException e) {
	      log.warning("Invalid response code in " +
			  PARAM_NO_MANIFEST_INDEX_RESPONSES +
			  ": " + str + ", ignored");
	    }
	  }
	  noManifestIndexResponses = newSet;
	}

	String disallowed = config.get(PARAM_DISALLOWED_METHODS,
				       DEFAULT_DISALLOWED_METHODS);
	if (StringUtil.isNullString(noindex)) {
	  disallowedMethods = new HashSet();
	} else {
	  Set newSet = new HashSet();
	  List lst = StringUtil.breakAt(disallowed,
					Constants.LIST_DELIM_CHAR,
					0, true);
	  for (Iterator iter = lst.iterator(); iter.hasNext(); ) {
	    String str = (String)iter.next();
	    newSet.add(str.toUpperCase());
	  }
	  disallowedMethods = newSet;
	}

	if (!isServerRunning() && getDaemon().isDaemonRunning()) {
	  startProxy();
	}
      } else if (isServerRunning()) {
	stopProxy();
      }
    }
  }

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
    LockssUrlConnectionPool connPool = new LockssUrlConnectionPool();
    LockssUrlConnectionPool quickConnPool = new LockssUrlConnectionPool();
    Configuration conf = ConfigManager.getCurrentConfig();

    int tot = conf.getInt(PARAM_PROXY_MAX_TOTAL_CONN,
			  DEFAULT_PROXY_MAX_TOTAL_CONN);
    int perHost = conf.getInt(PARAM_PROXY_MAX_CONN_PER_HOST,
			      DEFAULT_PROXY_MAX_CONN_PER_HOST);

    connPool.setMultiThreaded(tot, perHost);
    quickConnPool.setMultiThreaded(tot, perHost);
    connPool.setConnectTimeout
      (conf.getTimeInterval(PARAM_PROXY_CONNECT_TIMEOUT,
			    DEFAULT_PROXY_CONNECT_TIMEOUT));
    connPool.setDataTimeout
      (conf.getTimeInterval(PARAM_PROXY_DATA_TIMEOUT,
			    DEFAULT_PROXY_DATA_TIMEOUT));
    quickConnPool.setConnectTimeout
      (conf.getTimeInterval(PARAM_PROXY_QUICK_CONNECT_TIMEOUT,
			    DEFAULT_PROXY_QUICK_CONNECT_TIMEOUT));
    quickConnPool.setDataTimeout
      (conf.getTimeInterval(PARAM_PROXY_QUICK_DATA_TIMEOUT,
			    DEFAULT_PROXY_QUICK_DATA_TIMEOUT));
    return new org.lockss.proxy.ProxyHandler(getDaemon(),
					     connPool, quickConnPool);
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

  public boolean isIpAuthorized(String ip) {
    try {
      return accessHandler.isIpAuthorized(ip);
    }
    catch (Exception exc) {
      return false;
    }
  }

  public boolean showManifestIndexForResponse(int status) {
    return ! noManifestIndexResponses.contains(new Integer(status));
  }

  public boolean isMethodAllowed(String method) {
    return ! disallowedMethods.contains(method);
  }

}
