/*
 * $Id: BaseCrawler.java,v 1.32.12.1 2009-11-03 23:44:51 edwardsb1 Exp $
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

package org.lockss.crawler;

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;

import org.lockss.app.*;
import org.lockss.alert.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.state.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.clockss.*;

/**
 * The crawler.
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */
public abstract class BaseCrawler
       implements Crawler, PermissionMapSource, Crawler.PermissionHelper {
  /**
   * TODO
   * 1) write state to harddrive using whatever system we come up for for the
   * rest of LOCKSS
   * 2) check deadline and die if we run too long
   */

  static Logger logger = Logger.getLogger("BaseCrawler");

  public static final String PREFIX = Configuration.PREFIX + "crawler.";

  // See comments regarding connect timeouts in HttpClientUrlConnection
  public static final String PARAM_CONNECT_TIMEOUT =
    PREFIX + "timeout.connect";
  public static final long DEFAULT_CONNECT_TIMEOUT = 60 * Constants.SECOND;

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

  public static final String PARAM_EXPLODER_RETRY_COUNT =
    PREFIX + "exploderRetryCount";
  public static final int DEFAULT_EXPLODER_RETRY_COUNT = 3;

  /** Proxy crawls if true (except repair-from-cache) */
  public static final String PARAM_PROXY_ENABLED =
    PREFIX + "proxy.enabled";
  public static final boolean DEFAULT_PROXY_ENABLED = false;

  /** Proxy host for crawls (except repair-from-cache) */
  public static final String PARAM_PROXY_HOST =
    PREFIX + "proxy.host";

  /** Proxy port for crawls (except repair-from-cache) */
  public static final String PARAM_PROXY_PORT =
    PREFIX + "proxy.port";
  public static final int DEFAULT_PROXY_PORT = -1;

  public static final String PARAM_REFETCH_PERMISSIONS_PAGE =
    PREFIX + "storePermissionsRefetch";
  public static final boolean DEFAULT_REFETCH_PERMISSIONS_PAGE = false;

  public static final String PARAM_ABORT_ON_FIRST_NO_PERMISSION =
    PREFIX + "abortOnFirstNoPermission";
  public static final boolean DEFAULT_ABORT_ON_FIRST_NO_PERMISSION =
    true;

  public static final String PARAM_MIME_TYPE_PAUSE_AFTER_304 =
    PREFIX + "mimeTypePauseAfter304";
  public static final boolean DEFAULT_MIME_TYPE_PAUSE_AFTER_304 =
    false;

  protected ArchivalUnit au;

  protected LockssUrlConnectionPool connectionPool =
    new LockssUrlConnectionPool();

  protected CrawlerStatus crawlStatus = null;

  protected CrawlSpec spec = null;

  protected AuState aus = null;

  protected CrawlManager crawlMgr = null;

  protected boolean crawlAborted = false;

  protected LockssWatchdog wdog = null;

  protected boolean mimeTypePauseAfter304 = DEFAULT_MIME_TYPE_PAUSE_AFTER_304;

  protected abstract boolean doCrawl0();
  public abstract int getType();


  /** Return the type of crawl as a string
   * @return crawl type
   */
  protected abstract String getTypeString();

  protected PermissionChecker pluginPermissionChecker;
  protected List daemonPermissionCheckers = null;
  protected AlertManager alertMgr;

  protected String proxyHost = null;
  protected int proxyPort;

  protected PermissionMap permissionMap = null;

  protected String previousContentType;

  protected BaseCrawler(ArchivalUnit au, CrawlSpec spec, AuState aus) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null au");
    } else if (spec == null) {
      throw new IllegalArgumentException("Called with null spec");
    } else if (aus == null) {
      throw new IllegalArgumentException("Called with null aus");
    }
    this.au = au;
    this.spec = spec;
    this.aus = aus;

    /**
     There are two types of checkers: daemon and plug-in. The daemon ones are
     required, in that you must satisfy one of them to crawl a site.  The
     plug-in ones are optional, in that you don't need to specify any.
     However, you must satisfy each plug-in permission checker that is
     specified.

     The idea is that the plug-in can make the permission requirements more
     restrictive, but not less.
    */

    //Specified by the plug-in, this can be a null set.  We must satisfy
    //all of these to crawl a site.
    pluginPermissionChecker = spec.getPermissionChecker();

    alertMgr = getDaemon().getAlertManager();
  }

  protected LockssDaemon getDaemon() {
    return AuUtil.getDaemon(au);
  }

  protected void setCrawlManager(CrawlManager crawlMgr) {
    this.crawlMgr = crawlMgr;
  }

  protected void setCrawlConfig(Configuration config) {
    long connectTimeout = config.getTimeInterval(PARAM_CONNECT_TIMEOUT,
                                                 DEFAULT_CONNECT_TIMEOUT);
    long dataTimeout = config.getTimeInterval(PARAM_DATA_TIMEOUT,
                                              DEFAULT_DATA_TIMEOUT);
    connectionPool.setConnectTimeout(connectTimeout);
    connectionPool.setDataTimeout(dataTimeout);

    boolean proxyEnabled = config.getBoolean(PARAM_PROXY_ENABLED,
                                             DEFAULT_PROXY_ENABLED);
    if (proxyEnabled) {
      proxyHost = config.get(PARAM_PROXY_HOST);
      proxyPort = config.getInt(PARAM_PROXY_PORT, DEFAULT_PROXY_PORT);
      if (StringUtil.isNullString(proxyHost) || proxyPort <= 0) {
        proxyHost = null;
      }
    }
    String auProxySpec =
      AuUtil.getStringValue(AuUtil.getAuParamOrTitleDefault(au, ConfigParamDescr.CRAWL_PROXY), null);
    if ("DIRECT".equalsIgnoreCase(auProxySpec)) {
      if (proxyHost != null) {
        logger.info("AU overrides crawl proxy with DIRECT");
      }
      proxyHost = null;
    } else if (auProxySpec != null) {
      List<String> lst = StringUtil.breakAt(auProxySpec, ':', 3, false, true);
      if (lst.size() == 2) {
        try {
          String host = lst.get(0);
          int port = Integer.parseInt(lst.get(1));
          if (!StringUtil.isNullString(host) && port > 0) {
            proxyHost = host;
            proxyPort = port;
            proxyEnabled = true;
            logger.info("Using AU crawl_proxy: " + proxyHost + ":" + proxyPort);
          } else {
            logger.warning("Illegal AU crawl_proxy: " + auProxySpec);
          }
        } catch (NumberFormatException e) {
          logger.warning("Illegal AU crawl_proxy: " + auProxySpec);
        }
      } else {
        logger.warning("Illegal AU crawl_proxy: " + auProxySpec);
      }
    }
    if (proxyHost != null) {
      if (logger.isDebug()) logger.debug("Proxying through " + proxyHost
                                         + ":" + proxyPort);
    }
    mimeTypePauseAfter304 =
      config.getBoolean(PARAM_MIME_TYPE_PAUSE_AFTER_304,
                        DEFAULT_MIME_TYPE_PAUSE_AFTER_304);
  }

  List getDaemonPermissionCheckers() {
    if (daemonPermissionCheckers == null) {
      if (getDaemon().isClockss()) {
        daemonPermissionCheckers = new ClockssPermission().getCheckers();
      } else {
        daemonPermissionCheckers = new LockssPermission().getCheckers();
      }
    }
    return daemonPermissionCheckers;
  }

  public ArchivalUnit getAu() {
    return au;
  }

  public CrawlerStatus getStatus() {
    return crawlStatus;
  }

  public void abortCrawl() {
    crawlAborted = true;
  }

  /**
   * Main method of the crawler; it loops through crawling and caching
   * urls.
   *
   * @return true if no errors
   */
  public boolean doCrawl() {
    if (isWholeAU()) {
      aus.newCrawlStarted();
    }
    setCrawlConfig(ConfigManager.getCurrentConfig());
    // do this even if already aborted, so status doesn't get confused
    crawlStatus.signalCrawlStarted();
    try {
      if (crawlAborted) {
        //don't start an aborted crawl
        return aborted();
      }
      logger.info("Beginning crawl of "+au);
      boolean res = doCrawl0();
      if (!res && !crawlStatus.isCrawlError()) {
        crawlStatus.setCrawlStatus(Crawler.STATUS_ERROR);
      }
      if (crawlStatus.isCrawlError()) {
        alertMgr.raiseAlert(Alert.auAlert(Alert.CRAWL_FAILED, au),
                            getTypeString() + " Crawl failed: " +
                            crawlStatus.getCrawlErrorMsg());
      }
      if (isWholeAU()) {
        NodeManager nodeManager = getDaemon().getNodeManager(au);
        if (res) {
          nodeManager.newContentCrawlFinished(Crawler.STATUS_SUCCESSFUL, null);
          } else {
            nodeManager.newContentCrawlFinished(crawlStatus.getCrawlStatus(),
                                              crawlStatus.getCrawlErrorMsg());
        }         
      }
      return res;
    } catch (RuntimeException e) {
      logger.error("doCrawl0()", e);
      alertMgr.raiseAlert(Alert.auAlert(Alert.CRAWL_FAILED, au),
                          "Crawl of " + au.getName() +
                          "threw " + e.getMessage());
      if (isWholeAU()) {
        NodeManager nodeManager = getDaemon().getNodeManager(au);
        nodeManager.newContentCrawlFinished(Crawler.STATUS_ABORTED,
                                            e.getMessage());
      }
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

  protected boolean populatePermissionMap() {
      // get the permission list from crawl spec
    permissionMap = new PermissionMap(au, this, getDaemonPermissionCheckers(),
                                      pluginPermissionChecker);
    String perHost = au.getPerHostPermissionPath();
    if (perHost != null) {
      try {
        permissionMap.setPerHostPermissionPath(perHost);
      } catch (MalformedURLException e) {
        logger.error("Plugin error", e);
      }
    }
    return permissionMap.init();
  }

  /**
   * Try to reset the provided input stream, if we can't then return
   * new input stream for the given url
   */
  public BufferedInputStream resetInputStream(BufferedInputStream is,
                                              String url) throws IOException {
    try {
      is.reset();
    } catch (IOException e) {
      logger.debug("Couldn't reset input stream, so getting new one: " + e);
      is.close();
      UrlCacher uc = makeUrlCacher(url);
      uc.setRedirectScheme(UrlCacher.REDIRECT_SCHEME_FOLLOW_ON_HOST);
      is = new BufferedInputStream(uc.getUncachedInputStream());
//       crawlStatus.signalUrlFetched(uc.getUrl());

    }
    return is;
  }

  public void refetchPermissionPage(String permissionPage)
      throws IOException {
    // XXX can't reuse UrlCacher
    UrlCacher uc = makeUrlCacher(permissionPage);
    uc.setRedirectScheme(UrlCacher.REDIRECT_SCHEME_FOLLOW);
    updateCacheStats(uc.cache(), uc);
  }

  protected void updateCacheStats(int cacheResult, UrlCacher uc) {
    switch (cacheResult) {
    case UrlCacher.CACHE_RESULT_FETCHED:
      crawlStatus.signalUrlFetched(uc.getUrl());
      // XXX add getCachedProperties() to UrlCacher so don't have to create
      // CachedUrl (read props, open InputStream)
      {
        CachedUrl cu = uc.getCachedUrl();
        updateStatusMimeType(cu);
        if (cu.hasContent()) {
          crawlStatus.addContentBytesFetched(cu.getContentSize());
          previousContentType = cu.getContentType();
        }
        cu.release();
      }
      break;
    case UrlCacher.CACHE_RESULT_NOT_MODIFIED:
      crawlStatus.signalUrlNotModified(uc.getUrl());
      if (mimeTypePauseAfter304) {
        CachedUrl cu = uc.getCachedUrl();
        if (cu.hasContent()) {
          previousContentType = cu.getContentType();
        }
        cu.release();
      }
      break;
    }
  }

  /** All UrlCachers should be made via this method, so they get their
   * connection pool set. */
  public UrlCacher makeUrlCacher(String url) {
    UrlCacher uc = au.makeUrlCacher(url);
    if (getDaemon().isClockss()) {
      uc = new ClockssUrlCacher(uc);
    }
    uc.setConnectionPool(connectionPool);
    uc.setPermissionMapSource(this);
    uc.setWatchdog(wdog);
    if (previousContentType != null) {
      uc.setPreviousContentType(previousContentType);
      previousContentType = null;
    }
    return uc;
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

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[BaseCrawler: ");
    sb.append(au.toString());
    sb.append("]");
    return sb.toString();
  }

/**
 * Get the permission map from the crawler, creating it if it doesn't exist already
 */
  //PermissionMapSource method
  public PermissionMap getPermissionMap() {
    if (permissionMap == null) {
      populatePermissionMap();
    }
    return permissionMap;
  }

  protected boolean aborted() {
    logger.info("Crawl aborted: "+au);
    if (!crawlStatus.isCrawlError()) {
      crawlStatus.setCrawlStatus(Crawler.STATUS_ABORTED);
    }
    return false;
  }

  public CrawlerStatus getCrawlerStatus() {
    return crawlStatus;
  }

  protected void cacheWithRetries(UrlCacher uc) throws IOException {
    throw new UnsupportedOperationException("BaseCrawler doesn't do cacheWithRetries()");
  }
}
