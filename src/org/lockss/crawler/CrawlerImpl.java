/*
 * $Id: CrawlerImpl.java,v 1.34 2004-10-06 23:52:54 clairegriffin Exp $
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

package org.lockss.crawler;

import java.io.*;
import java.util.*;

import org.lockss.alert.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.state.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/**
 * The crawler.
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */
public abstract class CrawlerImpl implements Crawler {
  /**
   * TODO
   * 1) write state to harddrive using whatever system we come up for for the
   * rest of LOCKSS
   * 2) check deadline and die if we run too long
   */

  private static Logger logger = Logger.getLogger("CrawlerImpl");

  // See comments regarding connect timeouts in HttpClientUrlConnection
  public static final String PARAM_CONNECT_TIMEOUT =
    Configuration.PREFIX + "crawler.timeout.connect";
  public static long DEFAULT_CONNECT_TIMEOUT = 60 * Constants.SECOND;

  public static final String PARAM_DATA_TIMEOUT =
    Configuration.PREFIX + "crawler.timeout.data";
  public static long DEFAULT_DATA_TIMEOUT = 30 * Constants.MINUTE;

  /** Proxy host for crawls (except repair-from-cache) */
  public static final String PARAM_PROXY_HOST =
    Configuration.PREFIX + "crawler.proxy.host";

  /** Proxy port for crawls (except repair-from-cache) */
  public static final String PARAM_PROXY_PORT =
    Configuration.PREFIX + "crawler.proxy.port";
  public static final int DEFAULT_PROXY_PORT = -1;

  public static final String PARAM_REFETCH_PERMISSIONS_PAGE =
    Configuration.PREFIX + "crawler.storePermissionsRefetch";
  public static final boolean DEFAULT_REFETCH_PERMISSIONS_PAGE = false;

  public static final String LOCKSS_PERMISSION_STRING =
  "LOCKSS system has permission to collect, preserve, and serve this Archival Unit";

  // Max amount we'll buffer up to avoid refetching the permissions page
  static final int PERM_BUFFER_MAX = 16 * 1024;

  protected ArchivalUnit au;

  protected LockssUrlConnectionPool connectionPool =
    new LockssUrlConnectionPool();

  protected Crawler.Status crawlStatus = null;

  protected CrawlSpec spec = null;

  protected AuState aus = null;

  protected boolean crawlAborted = false;

  protected LockssWatchdog wdog = null;

  protected abstract boolean doCrawl0();
  public abstract int getType();

  protected ArrayList permissionCheckers = new ArrayList();
  protected List lockssCheckers = null;
  protected AlertManager alertMgr;

  protected String proxyHost = null;
  protected int proxyPort;

  protected CrawlerImpl(ArchivalUnit au, CrawlSpec spec, AuState aus) {
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
    lockssCheckers = new LockssPermission().getCheckers();
    permissionCheckers.addAll(spec.getPermissionCheckers());

    alertMgr = (AlertManager)org.lockss.app.LockssDaemon.getManager(org.lockss.app.LockssDaemon.ALERT_MANAGER);
  }

  protected void setCrawlConfig(Configuration config) {
    long connectTimeout = config.getTimeInterval(PARAM_CONNECT_TIMEOUT,
						 DEFAULT_CONNECT_TIMEOUT);
    long dataTimeout = config.getTimeInterval(PARAM_DATA_TIMEOUT,
					      DEFAULT_DATA_TIMEOUT);
    connectionPool.setConnectTimeout(connectTimeout);
    connectionPool.setDataTimeout(dataTimeout);

    proxyHost = config.get(PARAM_PROXY_HOST);
    proxyPort = config.getInt(PARAM_PROXY_PORT, DEFAULT_PROXY_PORT);
    if (StringUtil.isNullString(proxyHost) || proxyPort <= 0) {
      proxyHost = null;
    } else {
      if (logger.isDebug()) logger.debug("Proxying through " + proxyHost
					 + ":" + proxyPort);
    }
  }

  public ArchivalUnit getAu() {
    return au;
  }

  public Crawler.Status getStatus() {
    return crawlStatus;
  }

  public void abortCrawl() {
    crawlAborted = true;
  }

  /**
   * Main method of the crawler; it loops through crawling and caching
   * urls.
   *
   * @param deadline when to terminate by
   * @return true if no errors
   */
  public boolean doCrawl() {
    setCrawlConfig(ConfigManager.getCurrentConfig());
    if (crawlAborted) {
      //don't start an aborted crawl
      return false;
    } else try {
      logger.info("Beginning crawl of "+au);
      crawlStatus.signalCrawlStarted();
      return doCrawl0();
    } finally {
      crawlStatus.signalCrawlEnded();
    }
  }

  /**
   * Method used by subclasses to check crawl permission
   *
   * @return PERMISSION_UNCHECKED if we can't get the permission page,
   * otherwise PERMISSION_OK if there is an appropriate permission
   * statement on the specified page, PERMISSION_NOT_OK otherwise
   * @param permissionPage string representation of the URL of the permission
   * page
   */

  int crawlPermission(String permissionPage) {

    int crawl_ok = PermissionMap.PERMISSION_UNCHECKED;
    String err = Crawler.STATUS_PUB_PERMISSION;
    logger.debug("Checking for permissions on " + permissionPage);
    try {
      if (!au.shouldBeCached(permissionPage)) {
// 	alertMgr.raiseAlert(Alert.auAlert(Alert.PERMISSION_PAGE_FETCH_ERROR,
// 					  au).
// 			    setAttribute(ATTR_TEXT, "Permission page " +
// 					 permissionPage +
// 					 " is not within the crawl spec"));
        logger.warning("Permission page not within CrawlSpec");
      }
      else if ( (au.getCrawlSpec() != null) && !au.getCrawlSpec().canCrawl()) {
        logger.debug("Couldn't start crawl due to crawl window.");
        err = Crawler.STATUS_WINDOW_CLOSED;
      }
      else {
        if(checkPermission(permissionPage)) {
          crawl_ok = PermissionMap.PERMISSION_OK;
           if (crawlStatus.getCrawlError() == err) {
             crawlStatus.setCrawlError(null);
           }
        }
        else {
          logger.error("No crawl permission on " + permissionPage);
          crawl_ok = PermissionMap.PERMISSION_NOT_OK;
          alertMgr.raiseAlert(Alert.auAlert(Alert.NO_CRAWL_PERMISSION,
                                            au).
                              setAttribute(Alert.ATTR_TEXT,
                                           "The page at " + permissionPage +
                                           "\ndoes not contain the " +
                                           "LOCKSS permission statement.\n" +
                                           "No collection was done."));

        }
      }
    }
    catch (Exception ex) {
      logger.error("Exception reading permission page", ex);
      alertMgr.raiseAlert(Alert.auAlert(Alert.PERMISSION_PAGE_FETCH_ERROR, au).
                          setAttribute(Alert.ATTR_TEXT,
                                       "The LOCKSS permission page at " +
                                       permissionPage +
                                       "\ncould not be fetched. " +
                                       "The error was:\n" +
                                       ex.getMessage() + "\n"));
      crawl_ok = PermissionMap.FETCH_PERMISSION_FAILED;
    }

    if (crawl_ok != PermissionMap.PERMISSION_OK) {
      crawlStatus.setCrawlError(err);
    }
    return crawl_ok;
  }

  /**
   * checkPermission check the permission page for all of the required permission
   * objects.
   *
   * @param permissionPage String
   * @param checker PermissionChecker
   * @return boolean iff permission was found for each object on the page.
   */
  private boolean checkPermission(String permissionPage) throws
      IOException {

    PermissionChecker checker;
    // fetch and cache the permission page
    CachedUrlSet ownerCus = au.getAuCachedUrlSet();
    UrlCacher uc = makeUrlCacher(ownerCus, permissionPage);
    uc.setRedirectScheme(UrlCacher.REDIRECT_SCHEME_FOLLOW_ON_HOST);

    InputStream is = new BufferedInputStream(uc.getUncachedInputStream());
    crawlStatus.signalUrlFetched();
    // allow us to reread contents if reasonable size
    boolean needPermission = true;
    try {
      // check the lockss checkers and find at least one checker that matches
      for (Iterator it = lockssCheckers.iterator(); it.hasNext() && needPermission; ) {
        is.mark(PERM_BUFFER_MAX);
        checker = (PermissionChecker) it.next();
        Reader reader = new InputStreamReader(is, Constants.DEFAULT_ENCODING);
        if (checker.checkPermission(reader)) {
          needPermission = false;
          try {
            is.reset();
          }
          catch (IOException e) {
            is.close();
            uc = makeUrlCacher(ownerCus, permissionPage);
            uc.setRedirectScheme(UrlCacher.REDIRECT_SCHEME_FOLLOW_ON_HOST);
            is = new BufferedInputStream(uc.getUncachedInputStream());
	    crawlStatus.signalUrlFetched();
          }
        }
      }
      // if we didn't find at least one required lockss permission - fail.
      if(needPermission) {
        logger.error("No LOCKSS crawl permission on " + permissionPage);
        is.close();
        return false;
      }
      // now check for the required permission from the plugin
      for (Iterator it = permissionCheckers.iterator(); it.hasNext(); ) {
        is.mark(PERM_BUFFER_MAX);
        checker = (PermissionChecker) it.next();
        Reader reader = new InputStreamReader(is, Constants.DEFAULT_ENCODING);
        if (!checker.checkPermission(reader)) {
          logger.error("No plugin crawl permission on " + permissionPage);
          is.close();
          return false;
        }
        else {
          try {
            is.reset();
          }
          catch (IOException e) {
            is.close();
            uc = makeUrlCacher(ownerCus, permissionPage);
            uc.setRedirectScheme(UrlCacher.REDIRECT_SCHEME_FOLLOW_ON_HOST);
            is = new BufferedInputStream(uc.getUncachedInputStream());
	    crawlStatus.signalUrlFetched();
          }
        }
      }
      if (Configuration.getBooleanParam(PARAM_REFETCH_PERMISSIONS_PAGE,
                                        DEFAULT_REFETCH_PERMISSIONS_PAGE)) {
        logger.debug3("Permission granted. Caching permission page.");
        storePermissionPage(au.getAuCachedUrlSet(), permissionPage);
      }
      else {
        uc.storeContent(is, uc.getUncachedProperties());
      }

    }
    finally {
      try {
        is.close();
      }
      catch (IOException ignore) {
      }
    }

    return true;
  }

  void storePermissionPage(CachedUrlSet ownerCus, String permissionPage)
      throws IOException {
    // XXX can't reuse UrlCacher
    UrlCacher uc = makeUrlCacher(ownerCus, permissionPage);
    uc.setRedirectScheme(UrlCacher.REDIRECT_SCHEME_FOLLOW);
    updateCacheStats(uc.cache());
  }

  protected void updateCacheStats(int cacheResult) {
    switch (cacheResult) {
    case UrlCacher.CACHE_RESULT_FETCHED:
      crawlStatus.signalUrlFetched();
      break;
    case UrlCacher.CACHE_RESULT_NOT_MODIFIED:
      crawlStatus.signalUrlNotModified();
      break;
    }
  }

  /** All UrlCachers should be made via this method, so they get their
   * connection pool set. */
  protected UrlCacher makeUrlCacher(CachedUrlSet cus, String url) {
    ArchivalUnit au = cus.getArchivalUnit();
    UrlCacher uc = au.makeUrlCacher(cus, url);
    uc.setConnectionPool(connectionPool);
    //uc.setPermissionMap(permission
    return uc;
  }

  // For now only follow http: links
  public static boolean isSupportedUrlProtocol(String url) {
    return StringUtil.startsWithIgnoreCase(url, "http://");
  }

  // Was this.  If it's going to explicitly check for http://, there's
  // no point in creating the URL.
//   protected static boolean isSupportedUrlProtocol(String url) {
//     try {
//       URL ur = new URL(url);
//       // some 1.4 machines will allow this, so we explictly exclude it for now.
//       if (StringUtil.startsWithIgnoreCase(ur.toString(), "http://")) {
//         return true;
//       }
//     }
//     catch (Exception ex) {
//     }
//     return false;
//   }

  public void setWatchdog(LockssWatchdog wdog) {
    this.wdog = wdog;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[CrawlerImpl: ");
    sb.append(au.toString());
    sb.append("]");
    return sb.toString();
  }
}
