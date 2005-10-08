/*
 * $Id: CrawlerImpl.java,v 1.48 2005-10-08 02:06:32 troberts Exp $
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
import java.net.*;

import org.lockss.alert.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.state.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/**
 * The crawler.
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */
public abstract class CrawlerImpl implements Crawler, PermissionMapSource {
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
  public static final long DEFAULT_CONNECT_TIMEOUT = 60 * Constants.SECOND;

  public static final String PARAM_DATA_TIMEOUT =
    Configuration.PREFIX + "crawler.timeout.data";
  public static final long DEFAULT_DATA_TIMEOUT = 30 * Constants.MINUTE;

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

  public static final String PARAM_ABORT_WHILE_PERMISSION_OTHER_THAN_OK =
    Configuration.PREFIX + "CrawlerImpl.abortWhilePermissionOtherThanOk";
  public static final boolean DEFAULT_ABORT_WHILE_PERMISSION_OTHER_THAN_OK = false;

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

  protected ArrayList pluginPermissionCheckers = new ArrayList();
  protected List daemonPermissionCheckers = null;
  protected AlertManager alertMgr;

  protected String proxyHost = null;
  protected int proxyPort;

  protected PermissionMap permissionMap = null;

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

    /**
     There are two types of checkers: daemon and plug-in. The daemon ones are
     required, in that you must satisfy one of them to crawl a site.  The
     plug-in ones are optional, in that you don't need to specify any.
     However, you must satisfy each plug-in permission checker that is
     specified.

     The idea is that the plug-in can make the permission requirements more
     restrictive, but not less.
    */

    //At least one of these checkers must satisfied for us to crawl a site
    daemonPermissionCheckers = new LockssPermission().getCheckers();

    //Specified by the plug-in, this can be a null set.  We must satisfy 
    //all of these to crawl a site.
    pluginPermissionCheckers.addAll(spec.getPermissionCheckers());

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

  protected boolean populatePermissionMap() {
      // get the permission list from crawl spec
    permissionMap = new PermissionMap();
    List permissionList = spec.getPermissionPages();
    if (permissionList == null || permissionList.size() == 0) {
      logger.error("spec.getPermissionPages() return null list or nothing in the list!");
      crawlStatus.setCrawlError("Nothing in permission list");
      return false;
    }
    if (!checkPermissionList(permissionList)){
      return false;
    }
    return true;
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

    int crawl_ok = PermissionRecord.PERMISSION_UNCHECKED;
    String err = Crawler.STATUS_PUB_PERMISSION;
    logger.debug("Checking for permissions on " + permissionPage);
    try {
      if (!au.shouldBeCached(permissionPage)) {
// 	alertMgr.raiseAlert(Alert.auAlert(Alert.PERMISSION_PAGE_FETCH_ERROR,
// 					  au).
// 			    setAttribute(ATTR_TEXT, "Permission page " +
// 					 permissionPage +
// 					 " is not within the crawl spec"));
        logger.warning("Permission page not within CrawlSpec: "+permissionPage);
      }
      else if ( (au.getCrawlSpec() != null)
		&& !au.getCrawlSpec().inCrawlWindow()) {
        logger.debug("Couldn't start crawl due to crawl window.");
        err = Crawler.STATUS_WINDOW_CLOSED;
      }
      else {
	// go off to fetch the url and check for the permission statement
        if(checkPermission(permissionPage)) {
          crawl_ok = PermissionRecord.PERMISSION_OK;
           if (crawlStatus.getCrawlError() == err) {
             crawlStatus.setCrawlError(null);
           }
        } else {
          logger.error("No crawl permission on " + permissionPage);
          crawl_ok = PermissionRecord.PERMISSION_NOT_OK;
          alertMgr.raiseAlert(Alert.auAlert(Alert.NO_CRAWL_PERMISSION, au).
                              setAttribute(Alert.ATTR_TEXT,
                                           "The page at " + permissionPage +
                                           "\ndoes not contain the " +
                                           "LOCKSS permission statement.\n" +
                                           "No collection was done."));

        }
      }
    } catch (CacheException.RepositoryException ex) {
      logger.error("RepositoryException storing permission page", ex);
      // XXX should be an alert here
      crawl_ok = PermissionRecord.REPOSITORY_ERROR;
      err = Crawler.STATUS_REPO_ERR;
    } catch (Exception ex) {
      logger.error("Exception reading permission page", ex);
      alertMgr.raiseAlert(Alert.auAlert(Alert.PERMISSION_PAGE_FETCH_ERROR, au).
                          setAttribute(Alert.ATTR_TEXT,
                                       "The LOCKSS permission page at " +
                                       permissionPage +
                                       "\ncould not be fetched. " +
                                       "The error was:\n" +
                                       ex.getMessage() + "\n"));
      crawl_ok = PermissionRecord.FETCH_PERMISSION_FAILED;
    }

    if (crawl_ok != PermissionRecord.PERMISSION_OK) {
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
    UrlCacher uc = makeUrlCacher(permissionPage);
    uc.setRedirectScheme(UrlCacher.REDIRECT_SCHEME_FOLLOW_ON_HOST);

    InputStream is = new BufferedInputStream(uc.getUncachedInputStream());
    crawlStatus.signalUrlFetched(uc.getUrl());
    // allow us to reread contents if reasonable size
    boolean needPermission = true;
    try {
      // check the lockss checkers and find at least one checker that matches
      for (Iterator it = daemonPermissionCheckers.iterator(); it.hasNext() && needPermission; ) {
        is.mark(PERM_BUFFER_MAX);
        checker = (PermissionChecker) it.next();
        Reader reader = new InputStreamReader(is, Constants.DEFAULT_ENCODING);
        if (checker.checkPermission(reader, permissionPage)) {
	  logger.debug3("Found permission on "+checker);
          needPermission = false;
	  break; //we just need one permission to de sucessful here
	} else {
	  logger.debug3("Didn't find permission on "+checker);
	  is = resetInputStream(is, permissionPage);
        }
      }
      // if we didn't find at least one required lockss permission - fail.
      if(needPermission) {
        logger.error("No LOCKSS crawl permission on " + permissionPage);
        is.close();
        return false;
      }

      //either the pluginPermissionCheckers will need this
      //or the storeContent call will
      is = resetInputStream(is, permissionPage);

      // now check for the required permission from the plugin
      for (Iterator it = pluginPermissionCheckers.iterator(); it.hasNext(); ) {
        is.mark(PERM_BUFFER_MAX);
        checker = (PermissionChecker) it.next();
        Reader reader = new InputStreamReader(is, Constants.DEFAULT_ENCODING);
        if (!checker.checkPermission(reader, permissionPage)) {
          logger.error("No plugin crawl permission on " + permissionPage);
          is.close();
          return false;
        } else {
	  is = resetInputStream(is, permissionPage);
        }
      }
      if (Configuration.getBooleanParam(PARAM_REFETCH_PERMISSIONS_PAGE,
                                        DEFAULT_REFETCH_PERMISSIONS_PAGE)) {
        logger.debug3("Permission granted. Caching permission page.");
        storePermissionPage(au.getAuCachedUrlSet(), permissionPage);
      } else {
        uc.storeContent(is, uc.getUncachedProperties());
      }
    } finally {
      IOUtil.safeClose(is);
    }

    return true;
  }

  /**
   * Check the permission map to see if we have permission to crawl the given url.
   *
   * @param url the url that we are checking upon.
   * @param permissionFailedRetry true to refetch permission page if last fetch failed
   * @return if the url have permission to be crawled
   */
  protected boolean checkHostPermission(String url,
                                        boolean permissionFailedRetry) {
    int urlPermissionStatus = -1;
    String urlPermissionUrl = null;
    logger.debug3("Checking permission for "+url);
    try {
      urlPermissionStatus = permissionMap.getStatus(url);
      urlPermissionUrl = permissionMap.getPermissionUrl(url);
    } catch (MalformedURLException e) {
      logger.error("The url is malformed :" + url, e);
      crawlStatus.setCrawlError("Malformed Url: " + url);
      //there is no point go to the switch statement with MalformedURLException
      return false;
    }
    boolean printFailedWarning = true;
    switch (urlPermissionStatus) {
      case PermissionRecord.PERMISSION_MISSING:
        logger.warning("No permission page record on host: "+ url);
        crawlStatus.setCrawlError("No crawl permission page for host of " +
                                  url );
        // abort crawl here
        return false;
      case PermissionRecord.PERMISSION_OK:
        return true;
      case PermissionRecord.PERMISSION_NOT_OK:
        logger.error("No permission statement is found at: " +
                     urlPermissionUrl);
        crawlStatus.setCrawlError("No permission statement at: " + urlPermissionUrl);
        //abort crawl or skip all the url with this host ?
        //currently we just ignore urls with this host.
        return false;
      case PermissionRecord.PERMISSION_UNCHECKED:
        //should not be in this state as each permissionPage should be checked in the first iteration
        logger.warning("permission unchecked on host : "+ urlPermissionUrl);
        printFailedWarning = false;
        // fall through, re-fetch permission like FETCH_PERMISSION_FAILED
      case PermissionRecord.FETCH_PERMISSION_FAILED:
        if (printFailedWarning) {
          logger.warning("Failed to fetch permission page on host :" +
                         urlPermissionUrl);
        }
      if (permissionFailedRetry) {
        //refetch permission page
        logger.info("refetching permission page: " + urlPermissionUrl);
        try {
          permissionMap.putStatus(urlPermissionUrl,
                                  crawlPermission(urlPermissionUrl));
        } catch (MalformedURLException e){
          //XXX can we handle this better by centralizing the check of MalformedURL ?
          logger.error("Malformed urlPermissionUrl :" + urlPermissionUrl, e);
          crawlStatus.setCrawlError("MalFormedUrl :" + urlPermissionUrl);
          return false;
        }
        return checkHostPermission(url,false);
      } else {
        logger.error("Cannot fetch permission page on the second attempt : " + urlPermissionUrl);
        crawlStatus.setCrawlError("Cannot fetch permission page on the second attempt :" + urlPermissionUrl);
        //abort crawl or skip all the url with this host?
        //currently we just ignore urls with this host.
        crawlStatus.signalErrorForUrl(urlPermissionUrl,
                                      "Cannot fetch permission page " +
        "on the second attempt");
        return false;
      }
    case PermissionRecord.REPOSITORY_ERROR:
      logger.error("Repository error trying to store : "
                   + urlPermissionUrl);
      crawlStatus.setCrawlError("Repository error");
      crawlStatus.signalErrorForUrl(urlPermissionUrl, "Repository error");
      return false;
    default :
      logger.error("Unknown Permission Status! Something is going wrong!");
    return false;
    }
  }
  

  
  /**
   * Try to reset the provided input stream, if we can't then return 
   * new input stream for the given url
   */
  private InputStream resetInputStream(InputStream is, String url)
      throws IOException {
    try {
      is.reset();
    } catch (IOException e) {
      logger.debug("Couldn't reset input stream, so getting new one");
      is.close();
      UrlCacher uc = makeUrlCacher(url);
      uc.setRedirectScheme(UrlCacher.REDIRECT_SCHEME_FOLLOW_ON_HOST);
      is = new BufferedInputStream(uc.getUncachedInputStream());
      crawlStatus.signalUrlFetched(uc.getUrl());
    }
    return is;
  }

  /**
   * Check the permission of each url in the permission list, then save the result
   * in the permission map.
   *
   * @param permissionList permission pages url list of an AU
   * @return if all permission pages grant permission to crawl
   */
  protected boolean checkPermissionList(List permissionList) {
    boolean abortWhilePermissionOtherThanOk =
      Configuration.getBooleanParam(PARAM_ABORT_WHILE_PERMISSION_OTHER_THAN_OK,
				    DEFAULT_ABORT_WHILE_PERMISSION_OTHER_THAN_OK);

    logger.info("Checking permission on host(s) of " + au);
    Iterator permissionUrls = permissionList.iterator();
    while (permissionUrls.hasNext()) {
      String permissionPage = (String)permissionUrls.next();
      // it is the real thing that do the checking of permission, crawlPermission dwell in CrawlerImpl.java
      int permissionStatus = crawlPermission(permissionPage);
      // if permission status is something other than OK and the abortWhilePermissionOtherThanOk flag is on
       if (permissionStatus != PermissionRecord.PERMISSION_OK &&
	   abortWhilePermissionOtherThanOk) {
	logger.info("One or more host(s) of AU do not grant crawling permission - aborting crawl!");
	return false;
      }
      try {
	if (permissionStatus == PermissionRecord.PERMISSION_OK) {
	  logger.debug3("Permission granted on host: " + UrlUtil.getHost(permissionPage));
	}
	// set permissionMap
	permissionMap.putStatus(permissionPage, permissionStatus);
      } catch (MalformedURLException e){
	//XXX should catch this inside the permissionMap ?
	logger.error("The permissionPage's URL is Malformed : "+ permissionPage);
	crawlStatus.setCrawlError("Malformed permission page url");
      }
    }
    return true;
  }

  void storePermissionPage(CachedUrlSet ownerCus, String permissionPage)
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
      break;
    case UrlCacher.CACHE_RESULT_NOT_MODIFIED:
      crawlStatus.signalUrlNotModified(uc.getUrl());
      break;
    }
  }

  protected void logCrawlSpecCacheRate() {
    if (au instanceof BaseArchivalUnit) {
      BaseArchivalUnit bau = (BaseArchivalUnit)au;
      long cacheHits = bau.getCrawlSpecCacheHits();
      long cacheMisses = bau.getCrawlSpecCacheMisses();
      if (cacheHits == 0) {
	logger.info(cacheHits + "/" + cacheMisses + " cache hits");
      } else {
	float per = (float)cacheHits / (float)(cacheHits + cacheMisses);
	logger.info(cacheHits + "/" + cacheMisses + " cache hits (" +
		    Integer.toString(Math.round(per * 100)) + "%)");
      }
    }
  }

  /** All UrlCachers should be made via this method, so they get their
   * connection pool set. */
  protected UrlCacher makeUrlCacher(String url) {
    UrlCacher uc = au.makeUrlCacher(url);
    uc.setConnectionPool(connectionPool);
    uc.setPermissionMapSource(this);
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

  //PermissionMapSource method
  public PermissionMap getPermissionMap() {
    if (permissionMap == null) {
      populatePermissionMap();
    }
    return permissionMap;
  }
  
  protected boolean aborted() {
    logger.info("Crawl aborted: "+au);
    if (crawlStatus.getCrawlError() == null) {
      crawlStatus.setCrawlError(Crawler.STATUS_INCOMPLETE);
    }
    return false;
  }

}
