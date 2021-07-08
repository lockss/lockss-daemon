/*

Copyright (c) 2000-2020 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.crawler;

import java.io.IOException;
import java.net.*;
import java.util.*;

import org.lockss.app.*;
import org.lockss.alert.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.UrlFetcher.FetchResult;
import org.lockss.protocol.*;
import org.lockss.state.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.clockss.*;

/**
 * The base crawler extended by repair crawler and follow link crawler contains
 * proxy, retry and basic crawling logic.
 *
 * @ParamCategory Crawler
 */
public abstract class BaseCrawler implements Crawler {
  static Logger logger = Logger.getLogger(BaseCrawler.class);

  public static final String PREFIX = Configuration.PREFIX + "crawler.";

  // See comments regarding connect timeouts in HttpClientUrlConnection
  /** Amount of time the crawler will wait for a server to open a
   * connection.  One or two minutes is generally sufficient; it's unusual
   * for a functioning server to take longer than that to open a
   * connection.  A large connect timeout will cause crawl attempts of down
   * or unreachable servers to take a long time to fail. */
  public static final String PARAM_CONNECT_TIMEOUT =
    PREFIX + "timeout.connect";
  public static final long DEFAULT_CONNECT_TIMEOUT = 60 * Constants.SECOND;

  /** Amount of time that may elapse without any data being received on an
   * open connection, before the crawler will give up.  Should generally be
   * much larger than the connect timeout.  The fact that the connection
   * was opened indicates that the server is up (or was up recently), and
   * busy servers or complicated transactions may legitimately take a long
   * time to begin sending data. */
  public static final String PARAM_DATA_TIMEOUT =
    PREFIX + "timeout.data";
  public static final long DEFAULT_DATA_TIMEOUT = 30 * Constants.MINUTE;
  
  public static final String PARAM_DEFAULT_RETRY_COUNT =
    PREFIX + "retryCount";
  public static final int DEFAULT_DEFAULT_RETRY_COUNT = 3;

  public static final String PARAM_DEFAULT_RETRY_DELAY =
    PREFIX + "retryDelay";
  public static final long DEFAULT_DEFAULT_RETRY_DELAY = 10 * Constants.SECOND;

  public static final String PARAM_MAX_RETRY_COUNT =
    PREFIX + "maxRetryCount";
  public static final int DEFAULT_MAX_RETRY_COUNT = 10;

  public static final String PARAM_MIN_RETRY_DELAY =
    PREFIX + "minRetryDelay";
  public static final long DEFAULT_MIN_RETRY_DELAY = 1 * Constants.SECOND;
  
  public static final String PARAM_PERMISSION_BUF_MAX =
      Configuration.PREFIX + "permissionBuf.max";
  public static final int DEFAULT_PERMISSION_BUF_MAX = 512 * 1024;

  /** If true, an error fetching a permission page will cause the crawl to
   * fail with No Permission, even if the plugin supplies alternate
   * permission pages for that host.  If false, fetch errors will be soft
   * and it will go on the the next permission page (if any). */
  public static final String PARAM_FAIL_ON_PERMISSION_ERROR =
      Configuration.PREFIX + "failOnPermissionError";
  public static final boolean DEFAULT_FAIL_ON_PERMISSION_ERROR = false;

  /** The source address for crawler connections, or null to use the
   * machine's primary IP address.  Allows multiple daemons on a machine
   * with multiple IP addresses to crawl from those different addresses.
   * Takes precedence over org.lockss.crawler.crawlFromLocalAddr */
  public static final String PARAM_CRAWL_FROM_ADDR = PREFIX + "crawlFromAddr";

  /** If true, use the local identity address as the the source address for
   * crawler connections.  Ignored if org.lockss.crawler.crawlFromAddr is
   * set. */
  public static final String PARAM_CRAWL_FROM_LOCAL_ADDR =
    PREFIX + "crawlFromLocalAddr";
  public static final boolean DEFAULT_CRAWL_FROM_LOCAL_ADDR = false;

  /** Proxy crawls if true */
  public static final String PARAM_PROXY_ENABLED =
    PREFIX + "proxy.enabled";
  public static final boolean DEFAULT_PROXY_ENABLED = false;

  /** Proxy host for crawls */
  public static final String PARAM_PROXY_HOST =
    PREFIX + "proxy.host";

  /** Proxy port for crawls */
  public static final String PARAM_PROXY_PORT =
    PREFIX + "proxy.port";
  public static final int DEFAULT_PROXY_PORT = -1;
  
  /** If true, send Referer header when referring URL is known */
  public static final String PARAM_SEND_REFERRER = PREFIX + "sendReferrer";
  public static final boolean DEFAULT_SEND_REFERRER = true;
  
  public static final String ABORTED_BEFORE_START_MSG = "Crawl aborted before start";
  
  protected int streamResetMax = DEFAULT_PERMISSION_BUF_MAX;
  protected int maxRetries = DEFAULT_MAX_RETRY_COUNT;
  protected long minRetryDelay = DEFAULT_MIN_RETRY_DELAY;
  protected boolean sendReferrer = DEFAULT_SEND_REFERRER;
  protected Set<String> origStems;
  protected Set<String> cdnStems;
  
  public enum StorePermissionScheme {Legacy, StoreAllInSpec};

  /**  the crawl queues are sorted.  <code>CrawlDate</code>:
   * By recency of previous crawl attempt, etc. (Attempts to give all AUs
   * an equal chance to crawl as often as they want.);
   * <code>CreationDate</code>: by order in which AUs were
   * created. (Attempts to synchronize crawls of AU across machines to
   * optimize for earliest polling.) */

  /** Determines how permission pages are stored.  <code>Legacy</code>:
   * Permission pages are stored only at the last URL of any redirect
   * chain, probe permission pages aren't stored (unless otherwise
   * encountered in the crawl).  Redirects are followed on host and not
   * checked against the crawl rules.  <code>StoreAllInSpec</code>:
   * Permission pages and probe permission pages are stored at all URLs in
   * a redirect chain, as with normally fetched pages.  Redirects are
   * followed only if in the crawl spec.  In order to serve AUs to another
   * LOCKSS box, which will check permission, this should be true.
   */
  public static final String PARAM_STORE_PERMISSION_SCHEME =
    PREFIX + "storePermissionScheme";
  public static final StorePermissionScheme DEFAULT_STORE_PERMISSION_SCHEME =
    StorePermissionScheme.Legacy;
  
  public static final String PARAM_MIME_TYPE_PAUSE_AFTER_304 =
    PREFIX + "mimeTypePauseAfter304";
  public static final boolean DEFAULT_MIME_TYPE_PAUSE_AFTER_304 =
    false;

  public static final String PARAM_THROW_IF_RATE_LIMITER_NOT_USED =
    PREFIX + "throwIfRateLimiterNotUsed";
  public static final boolean DEFAULT_THROW_IF_RATE_LIMITER_NOT_USED =
    true;

  /** If the per-AU proxy spec is invalid (can't be parsed), abort the
   * crawl.  If false the crawl proceeds using the global proxy if any. */
  public static final String PARAM_ABORT_ON_INVALID_PROXY =
    PREFIX + "abortOnInvalidProxy";
  public static final boolean DEFAULT_ABORT_ON_INVALID_PROXY = true;

  protected boolean mimeTypePauseAfter304 = 
      DEFAULT_MIME_TYPE_PAUSE_AFTER_304;
  protected StorePermissionScheme paramStorePermissionScheme =
      DEFAULT_STORE_PERMISSION_SCHEME;
  protected boolean paramFailOnPermissionError =
      DEFAULT_FAIL_ON_PERMISSION_ERROR;
  protected boolean throwIfRateLimiterNotUsed =
      DEFAULT_THROW_IF_RATE_LIMITER_NOT_USED;
  
  protected int pauseCounter = 0;
  protected ArchivalUnit au;
  protected LockssUrlConnectionPool connectionPool;
  protected CrawlerStatus crawlStatus;
  protected AuState aus;
  protected CrawlManager crawlMgr;
  protected AlertManager alertMgr;
  protected boolean crawlAborted = false;
  protected LockssWatchdog wdog;
  protected IPAddr crawlFromAddr;
  protected String proxyHost;
  protected int proxyPort;
  protected String proxyStatus;
  protected PermissionMap permissionMap;
  protected CrawlRateLimiter crl;
  protected String previousContentType;
  protected String crawlPoolKey;
  protected CrawlReq req;
  protected CrawlerFacade facade;
  protected CrawlSeed crawlSeed;

  protected BaseCrawler(ArchivalUnit au, AuState aus) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null au");
    } else if (aus == null) {
      throw new IllegalArgumentException("Called with null aus");
    }
    this.au = au;
    this.aus = aus;
    alertMgr = getDaemon().getAlertManager();
    connectionPool = new LockssUrlConnectionPool();
    origStems = new HashSet(au.getUrlStems());
    cdnStems = new HashSet();
  }
  
  protected abstract boolean doCrawl0();
  
  public abstract Crawler.Type getType();
  
  protected LockssDaemon getDaemon() {
    return AuUtil.getDaemon(au);
  }

  protected void setCrawlManager(CrawlManager crawlMgr) {
    this.crawlMgr = crawlMgr;
  }

  public void setCrawlReq(CrawlReq req) {
    this.req = req;
  }
  
  protected void setCrawlConfig(Configuration config) {
    maxRetries = config.getInt(PARAM_MAX_RETRY_COUNT,
        DEFAULT_MAX_RETRY_COUNT);
    minRetryDelay = config.getLong(PARAM_MIN_RETRY_DELAY,
        DEFAULT_MIN_RETRY_DELAY);
    sendReferrer = config.getBoolean(PARAM_SEND_REFERRER,
				     DEFAULT_SEND_REFERRER);

    streamResetMax =
      config.getInt(PARAM_PERMISSION_BUF_MAX, DEFAULT_PERMISSION_BUF_MAX);
    long connectTimeout = config.getTimeInterval(PARAM_CONNECT_TIMEOUT,
                                                 DEFAULT_CONNECT_TIMEOUT);
    long dataTimeout = config.getTimeInterval(PARAM_DATA_TIMEOUT,
                                              DEFAULT_DATA_TIMEOUT);
    connectionPool.setConnectTimeout(connectTimeout);
    connectionPool.setDataTimeout(dataTimeout);

    throwIfRateLimiterNotUsed =
        config.getBoolean(PARAM_THROW_IF_RATE_LIMITER_NOT_USED,
            DEFAULT_THROW_IF_RATE_LIMITER_NOT_USED);

    paramStorePermissionScheme =
        (StorePermissionScheme)config.getEnum(StorePermissionScheme.class,
					    PARAM_STORE_PERMISSION_SCHEME,
					    DEFAULT_STORE_PERMISSION_SCHEME);

    paramFailOnPermissionError =
      config.getBoolean(PARAM_FAIL_ON_PERMISSION_ERROR,
                        DEFAULT_FAIL_ON_PERMISSION_ERROR);

    String crawlFrom = config.get(PARAM_CRAWL_FROM_ADDR);
    if (StringUtil.isNullString(crawlFrom)) {
      if (config.getBoolean(PARAM_CRAWL_FROM_LOCAL_ADDR,
			    DEFAULT_CRAWL_FROM_LOCAL_ADDR)) {
        crawlFrom = config.get(IdentityManager.PARAM_LOCAL_IP);
      }
    }
    if (crawlFrom == null) {
      crawlFromAddr = null;
    } else {
      try {
        crawlFromAddr = IPAddr.getByName(crawlFrom);
      } catch (UnknownHostException e) {
        logger.error("crawlFromAddr (" + crawlFrom + 
            ") not found, so not used.", e);
        crawlFromAddr = null;
      }
    }

    AuUtil.AuProxyInfo aupinfo = AuUtil.getAuProxyInfo(au, config);
    proxyHost = aupinfo.getHost();
    proxyPort = aupinfo.getPort();
    if (aupinfo.isInvalidAuOverride()) {
      proxyStatus = "Invalid: " + aupinfo.getAuSpec();
      if (config.getBoolean(PARAM_ABORT_ON_INVALID_PROXY,
			    DEFAULT_ABORT_ON_INVALID_PROXY)) {
	logger.error("Invalid AU proxy: " + aupinfo.getAuSpec() +
		     ". Aborting");
	crawlStatus.setCrawlStatus(Crawler.STATUS_ERROR,
				   "Invalid AU proxy: " + aupinfo.getAuSpec());
	abortCrawl();
      } else {
	if (proxyHost != null) {
	  logger.warning("Invalid AU proxy: " + aupinfo.getAuSpec() +
			 ". Continuing with default proxy");
	} else {
	  logger.warning("Invalid AU proxy: " + aupinfo.getAuSpec() +
			 ". Continuing with no proxy");
	}
      }
    } else if (aupinfo.isAuOverride()) {
      if (aupinfo.getHost() == null) {
        logger.info("AU overrides crawl proxy with DIRECT");
	proxyStatus = "Direct";
      } else {
	proxyStatus = aupinfo.getHost() + ":" + aupinfo.getPort();
        if (logger.isDebug()) {
          logger.debug("Using AU crawl_proxy: " + proxyStatus);
        }
      }
    } else if (proxyHost != null) {
      proxyStatus = proxyHost + ":" + proxyPort;
      if (logger.isDebug()) logger.debug("Proxying through " + proxyStatus);
    }

    mimeTypePauseAfter304 =
      config.getBoolean(PARAM_MIME_TYPE_PAUSE_AFTER_304,
			DEFAULT_MIME_TYPE_PAUSE_AFTER_304);
  }

  /** Return the type of crawl as a string
   * @return crawl type
   */
  protected String getTypeString() {
    return getType().toString();
  }

  protected CrawlRateLimiter getCrawlRateLimiter() {
    if (crl == null) {
      if (crawlMgr != null) {
        crl = crawlMgr.getCrawlRateLimiter(this);
      } else {
        crl = CrawlRateLimiter.Util.forAu(au);
      }
    }
    return crl;
  }

  List<PermissionChecker> getDaemonPermissionCheckers() {
    if (getDaemon().isClockss()) {
      return new ClockssPermission().getCheckers();
    } else {
      return new LockssPermission().getCheckers();
    }
  }

  public ArchivalUnit getAu() {
    return au;
  }

  public CrawlerStatus getCrawlerStatus() {
    return crawlStatus;
  }

  public void abortCrawl() {
    crawlAborted = true;
  }

  public boolean isAborted(){
    return crawlAborted;
  }
  
  /**
   * Main method of the crawler; it loops through crawling and caching
   * urls.
   *
   * @return true if no errors
   */
  public boolean doCrawl() {
    if (isWholeAU()) {
      if (req != null && req.getRefetchDepth() > 0) {
	aus.deepCrawlStarted(req.getRefetchDepth());
      } else {
	aus.newCrawlStarted();
      }
    }
    setCrawlConfig(ConfigManager.getCurrentConfig());
    crawlStatus.setProxy(proxyStatus);
    if (req != null) {
      crawlStatus.setPriority(req.getPriority());
    }
    // do this even if already aborted, so status doesn't get confused
    crawlStatus.signalCrawlStarted();
    try {
      if (crawlAborted) {
        //don't start an aborted crawl
        return aborted(ABORTED_BEFORE_START_MSG);
      }
      logger.info("Beginning crawl of "+au);
      boolean res = doCrawl0();
      if (!res && !crawlStatus.isCrawlError()) {
        crawlStatus.setCrawlStatus(Crawler.STATUS_ERROR);
      }
      if (isWholeAU()) {
      	// Raise end of crawl alert
      	Alert alert;
      	StringBuilder sb = new StringBuilder();
      	sb.append("Crawl finished ");
      	if (res) {
      	  alert = Alert.CRAWL_FINISHED;
      	  sb.append("successfully: ");
      	} else {
      	  alert = Alert.CRAWL_FAILED;
      	  sb.append("with error: ");
      	  sb.append(crawlStatus.getCrawlErrorMsg());
      	  sb.append(": ");
      	}
      	sb.append(StringUtil.numberOfUnits(crawlStatus.getNumFetched(),
      					   "file"));
      	sb.append(" fetched, ");
      	int warn =
      	  crawlStatus.getNumUrlsWithErrorsOfSeverity(CrawlerStatus.Severity.Warning);
      	sb.append(StringUtil.numberOfUnits(warn, "warning"));
      	if (!res) {
      	  sb.append(", ");
      	  int err =
      	    crawlStatus.getNumUrlsWithErrorsOfSeverity(CrawlerStatus.Severity.Error) +
      	    crawlStatus.getNumUrlsWithErrorsOfSeverity(CrawlerStatus.Severity.Fatal);
      	  sb.append(StringUtil.numberOfUnits(err, "error"));
      	}
      	appendAlertInfo(sb);
      	raiseAlert(Alert.auAlert(alert, au), sb.toString());
      
      	if (res) {
      	  newContentCrawlFinished(Crawler.STATUS_SUCCESSFUL, null);
      	} else {
      	  newContentCrawlFinished(crawlStatus.getCrawlStatus(),
				  crawlStatus.getCrawlErrorMsg());
      	}
      }
      return res;
    } catch (RuntimeException e) {
      logger.error("doCrawl0()", e);
      raiseAlert(Alert.auAlert(Alert.CRAWL_FAILED, au),
          "Crawl of " + au.getName() +
          " threw " + e.getMessage());
      setThrownStatus(e);
      throw e;
    } catch (Error e) {
      logger.error("doCrawl0()", e);
      setThrownStatus(e);
      throw e;
    } finally {
      crawlStatus.signalCrawlEnded();
      if (connectionPool != null) {
      	try {
      	  connectionPool.closeIdleConnections(0);
      	  connectionPool = null;
      	} catch (RuntimeException e) {
      	  logger.warning("closeIdleConnections", e);
      	}
      }
    }
  }

  void newContentCrawlFinished(int status, String msg) {
    NodeManager nodeManager = getDaemon().getNodeManager(au);
    if (req != null && req.getRefetchDepth() > 0) {
      nodeManager.newContentCrawlFinished(status, null, req.getRefetchDepth());
    } else {
      nodeManager.newContentCrawlFinished(status, null);
    }
  }

  protected void raiseAlert(Alert alert, String text) {
    alertMgr.raiseAlert(alert, text);
  }

  // Override to add info to alert
  protected void appendAlertInfo(StringBuilder sb) {
  }

  void setThrownStatus(Throwable t) {
    if (!crawlStatus.isCrawlError()) {
      crawlStatus.setCrawlStatus(Crawler.STATUS_ABORTED,
				 "Aborted: " + t.getMessage()); 
    }
    if (isWholeAU()) {
      NodeManager nodeManager = getDaemon().getNodeManager(au);
      newContentCrawlFinished(Crawler.STATUS_ABORTED, t.getMessage());
    }
  }

  protected boolean populatePermissionMap() {
    Collection<String> permissionUrls = null;
    try {
      permissionUrls = getCrawlSeed().getPermissionUrls();
    } catch (ConfigurationException|PluginException|IOException e) {
      logger.error("Could not compute permission URLs", e);
      crawlStatus.setCrawlStatus(Crawler.STATUS_PLUGIN_ERROR, 
                                 "Plugin failed to provide permission URLs");
      return false;
    }

    permissionMap = new PermissionMap(getCrawlerFacade(),
                                      getDaemonPermissionCheckers(),
                                      au.makePermissionCheckers(),
                                      permissionUrls);
    String perHost = au.getPerHostPermissionPath();
    if (perHost != null) {
      try {
        permissionMap.setPerHostPermissionPath(perHost);
      }
      catch (MalformedURLException mue) {
        logger.error("Plugin error", mue);
        // XXX we currently just go on, should we do something else?
      }
    }
    permissionMap.setFailOnPermissionError(paramFailOnPermissionError);
    return permissionMap.populate();
  }
  
  /**
   * using the permission map check for permission on a given url
   * @param url
   * @return permission state and false is permission map is not initialized
   */
  public boolean hasPermission(String url) {
    if(permissionMap != null) {
      return permissionMap.hasPermission(url);
    }
    return false;
  }

  /** If this url was allowed due to globallyPermittedHosts or cdn host and
   * its stem isn't already contained in the AU's stems, add it to the
   * dynamic stem list */
  protected void updateCdnStems(String url) {
    try {
      String stem = UrlUtil.getUrlPrefix(url);
      if (!origStems.contains(stem) && !cdnStems.contains(stem)) {
	aus.addCdnStem(stem);
	cdnStems.add(stem);
      }
    } catch (MalformedURLException e) {
      logger.error("updateCdnStems(" + url + ")", e);
      // ignore
    }
  }

  protected void updateCacheStats(FetchResult res, CrawlUrlData curl) {
    // Paranoia - assert that the rate limiter was actually used
    CrawlRateLimiter crl = getCrawlRateLimiter();
    if(res != FetchResult.NOT_FETCHED &&
       pauseCounter == crl.getPauseCounter()) {
      logger.critical("CrawlRateLimiter not used after " + curl,
                      new Throwable());
      if (throwIfRateLimiterNotUsed) {
        throw new RuntimeException("CrawlRateLimiter not used");
      }
    }
    pauseCounter = crl.getPauseCounter();
    
    switch (res) {
    case FETCHED:
      crawlStatus.signalUrlFetched(curl.getUrl());
      {
        curl.setFetched(true);
        CachedUrl cu = au.makeCachedUrl(curl.getUrl());

        if (cu.hasContent()) {
          updateStatusMimeType(cu);
          crawlStatus.addContentBytesFetched(cu.getContentSize());
          previousContentType = cu.getContentType();
        }
        cu.release();
      }
      break;
    case FETCHED_NOT_MODIFIED:
      crawlStatus.signalUrlNotModified(curl.getUrl());
      curl.setFetched(true);
      if (mimeTypePauseAfter304) {
        CachedUrl cu = au.makeCachedUrl(curl.getUrl());
        if (cu.hasContent()) {
          previousContentType = cu.getContentType();
        }
        cu.release();
      }
      break;
    case NOT_FETCHED:
      curl.setFetched(false);
      break;
    default:
      throw new ShouldNotHappenException(
          "A switch on UrlFetcher.FetchResult hit an unkown value");
    }
  }

  protected boolean checkGloballyExcludedUrl(ArchivalUnit au, String url) {
    if (crawlMgr != null) {
      if (crawlMgr.isGloballyExcludedUrl(au, url)) {
        crawlStatus.signalErrorForUrl(url, "Excluded (probable recursion)");
        String msg = "URL excluded (probable recursion): " + url;
        logger.siteWarning(msg);
        if (alertMgr != null) {
          alertMgr.raiseAlert(Alert.auAlert(Alert.CRAWL_EXCLUDED_URL, au), msg);
        }
        return true;
      }
    }
    return false;
  }
  
  protected boolean isIgnoredException(Exception ex) {
    return (ex == null || ex instanceof CacheSuccess);
  }

  protected void reportInfoException(UrlCacher uc) {
    CacheException ex = uc.getInfoException();
    if (!isIgnoredException(ex)) {
      crawlStatus.signalErrorForUrl(uc.getUrl(), ex);
    }
  }

  public void setPreviousContentType(String previousContentType) {
    this.previousContentType = previousContentType;
  }
  
  // Overridable for testing
  protected CrawlUrlData newCrawlUrlData(String url, int depth) {
    return new CrawlUrlData(url, depth);
  }
  
  // Default Set impl overridden for some tests
  protected Set newSet() {
    return new HashSet();
  }
  
  protected boolean withinCrawlWindow() {
    return au.inCrawlWindow();
  }

  /** All UrlCachers should be made via this method, so they get their
   * watchdog set. */
  public UrlCacher makeUrlCacher(UrlData ud) {
    UrlCacher uc = au.makeUrlCacher(ud);
    uc.setWatchdog(wdog);
    return uc;
  }
  
  public UrlFetcher makeUrlFetcher(CrawlUrlData curl) {
    UrlFetcher uf = makeUrlFetcher(curl.getUrl());
    uf.setCrawlUrl(curl);
    if (sendReferrer && au.sendReferrer() && curl.getReferrer() != null) {
      uf.setRequestProperty(Constants.HTTP_REFERER, curl.getReferrer());
    }
    return uf;
  }

  /** All UrlFetchers should be made via this method, so they get their
   * connection pool set. */
  public UrlFetcher makeUrlFetcher(String url) {
    UrlFetcher uf = au.makeUrlFetcher(getCrawlerFacade(), url);
    if (getDaemon().isClockss()) {
      uf = new ClockssUrlFetcher(uf);
    }
    uf.setConnectionPool(connectionPool);
    if (previousContentType != null) {
      uf.setPreviousContentType(previousContentType);
    }
    CrawlRateLimiter crl = getCrawlRateLimiter();
    uf.setCrawlRateLimiter(crl);
    if (crawlFromAddr != null) {
      uf.setLocalAddress(crawlFromAddr);
    }
    uf.setRedirectScheme(UrlFetcher.REDIRECT_SCHEME_STORE_ALL_IN_SPEC);
    if (proxyHost != null) {
      uf.setProxy(proxyHost, proxyPort);
    }
    uf.setWatchdog(wdog);
    return uf;
  }
  
  public UrlFetcher makePermissionUrlFetcher(String url) {
    UrlFetcher uf = makeUrlFetcher(url);
    switch (paramStorePermissionScheme) {
    case Legacy:
      uf.setRedirectScheme(UrlFetcher.REDIRECT_SCHEME_FOLLOW_ON_HOST);
      break;
    case StoreAllInSpec:
      uf.setRedirectScheme(UrlFetcher.REDIRECT_SCHEME_STORE_ALL_IN_SPEC);
      break;
    }
    BitSet permFetchFlags = uf.getFetchFlags();
    permFetchFlags.set(UrlCacher.REFETCH_FLAG);
    permFetchFlags.set(UrlCacher.IS_PERMISSION_FETCH);
    uf.setFetchFlags(permFetchFlags);
    return uf;
  }
  
  /**  
   * update the crawl.status to keep record of urls 
   * found with different types of mime-types 
   */
  private void updateStatusMimeType(CachedUrl cu) {
    String conType = cu.getContentType();  
    if (conType != null) {      
      String mimeType = HeaderUtil.getMimeTypeFromContentType(conType);
      crawlStatus.signalMimeTypeOfUrl(mimeType, cu.getUrl()); 
    }
    return;
  }
  
  // Follow http: and https: links
  public static boolean isSupportedUrlProtocol(String url) {
    return StringUtil.startsWithIgnoreCase(url, "http://")
      || StringUtil.startsWithIgnoreCase(url, "https://");
  }

  public void setWatchdog(LockssWatchdog wdog) {
    this.wdog = wdog;
  }

  protected void pokeWDog() {
    if (wdog != null) {
      wdog.pokeWDog();
    }
  }
  
  protected boolean aborted() {
    return aborted(null);
  }
  
  protected boolean aborted(String msg) {
    logger.info("Crawl aborted: " + au);
    if(!crawlStatus.isCrawlError()) {
      crawlStatus.setCrawlStatus(Crawler.STATUS_ABORTED, msg);
    } 
    return false;
  }

  public void setCrawlPool(String key) {
    crawlPoolKey = key;
  }

  public String getCrawlPool() {
    return crawlPoolKey;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[BaseCrawler: ");
    sb.append(au.toString());
    sb.append("]");
    return sb.toString();
  }
  
  protected CrawlSeed getCrawlSeed() {
    if(crawlSeed == null) {
      crawlSeed = au.makeCrawlSeed(getCrawlerFacade());
    }
    return crawlSeed;
  }
  
  protected CrawlerFacade getCrawlerFacade() {
    if(facade == null) {
      facade = new BaseCrawlerFacade(this);
    }
    return facade;
  };

  public static class BaseCrawlerFacade 
  implements Crawler.CrawlerFacade, Crawler.PermissionHelper {
    BaseCrawler crawler;
    public BaseCrawlerFacade(BaseCrawler crawler) {
      this.crawler = crawler;
    }
    
    public CrawlerStatus getCrawlerStatus() {
      return crawler.getCrawlerStatus();
    }
    
    public ArchivalUnit getAu() {
      return crawler.getAu();
    }

    public UrlFetcher makeUrlFetcher(String url) {
      return crawler.makeUrlFetcher(url);
    }
    
    public UrlFetcher makePermissionUrlFetcher(String url) {
      return crawler.makePermissionUrlFetcher(url);
    }
    
    public UrlCacher makeUrlCacher(UrlData ud) {
      return crawler.makeUrlCacher(ud);
    }

    public void setPreviousContentType(String previousContentType) {
      crawler.setPreviousContentType(previousContentType);
    }
    
    public boolean isAborted() {
      return crawler.isAborted();
    }

    public boolean hasPermission(String url) {
      return crawler.hasPermission(url);
    }

    public void addToFailedUrls(String url) {
      throw new UnsupportedOperationException(
          "This crawler has no failed urls set");
    }

    public void addToFetchQueue(CrawlUrlData curl) {
      throw new UnsupportedOperationException(
          "This crawler has no fetch queue");
    }

    public void addToParseQueue(CrawlUrlData curl) {
      throw new UnsupportedOperationException(
          "This crawler has no parse queue");
    }

    public void addToPermissionProbeQueue(String probeUrl,
					  String referrerUrl) {
      throw new UnsupportedOperationException(
          "This crawler has no permission probe url set");      
    }

    public long getRetryDelay(CacheException ce) {
      long delay = ce.getRetryDelay();
      return Math.max(delay, crawler.minRetryDelay);
    }

    public int getRetryCount(CacheException ce) {
      int res = ce.getRetryCount();
      return Math.min(res, crawler.maxRetries);
    }

    public int permissonStreamResetMax() {
      return crawler.streamResetMax;
    }
  
    @Override
    public boolean isGloballyPermittedHost(String host) {
      // crawlMgr not always set up in tests
      return crawler.crawlMgr != null &&
	crawler.crawlMgr.isGloballyPermittedHost(host);
    }

    @Override
    public boolean isAllowedPluginPermittedHost(String host) {
      // crawlMgr not always set up in tests
      return crawler.crawlMgr != null &&
	crawler.crawlMgr.isAllowedPluginPermittedHost(host);
    }

    @Override
    public void updateCdnStems(String url) {
      crawler.updateCdnStems(url);
    }

    @Override
    public CrawlUrl addChild(CrawlUrl curl, String url) {
      CrawlUrlData curld = (CrawlUrlData)curl;
      CrawlUrlData child = new CrawlUrlData(url, curld.getDepth()+1);
      child.setReferrer(curl.getUrl());
      curld.addChild(child);
      return child;
    }
  }

}
