/*
 * $Id: CrawlerImpl.java,v 1.21.2.2 2004-07-19 22:33:02 dcfok Exp $
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
import java.net.URL;
import org.lockss.daemon.*;
import org.lockss.state.*;
import org.lockss.alert.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;

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

  public static final String PARAM_REFETCH_PERMISSIONS_PAGE =
    Configuration.PREFIX + "crawler.storePermissionsRefetch";
  public static final boolean DEFAULT_REFETCH_PERMISSIONS_PAGE = false;

  // Max amount we'll buffer up to avoid refetching the permissions page
  static final int PERM_BUFFER_MAX = 16 * 1024;

  protected ArchivalUnit au;

  protected LockssUrlConnectionPool connectionPool =
    new LockssUrlConnectionPool();

  protected Crawler.Status crawlStatus = null;

  protected int numUrlsFetched = 0;

  protected CrawlSpec spec = null;

  protected AuState aus = null;

  protected boolean crawlAborted = false;

  protected LockssWatchdog wdog = null;

  protected abstract boolean doCrawl0();
  public abstract int getType();

  protected AlertManager alertMgr;

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
    connectionPool = new LockssUrlConnectionPool();

    long connectTimeout =
      Configuration.getTimeIntervalParam(PARAM_CONNECT_TIMEOUT,
					 DEFAULT_CONNECT_TIMEOUT);
    long dataTimeout =
      Configuration.getTimeIntervalParam(PARAM_DATA_TIMEOUT,
					 DEFAULT_DATA_TIMEOUT);
    connectionPool.setConnectTimeout(connectTimeout);
    connectionPool.setDataTimeout(dataTimeout);
    alertMgr = (AlertManager)org.lockss.app.LockssDaemon.getManager(org.lockss.app.LockssDaemon.ALERT_MANAGER);
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
    try {
      return doCrawl0();
    } finally {
      crawlStatus.signalCrawlEnded();
    }
  }

  int crawlPermission(String permissionPage) {
    
    int crawl_ok = PermissionRecord.PERMISSION_UNCHECKED;
    String err = Crawler.STATUS_PUB_PERMISSION;
    CachedUrlSet ownerCus = au.getAuCachedUrlSet();

    // fetch and cache the permission pages
    UrlCacher uc = makeUrlCacher(ownerCus, permissionPage);
    logger.debug("Checking for permissions on " + permissionPage);
    uc.setRedirectScheme(UrlCacher.REDIRECT_SCHEME_FOLLOW);
    try {
      if (!au.shouldBeCached(permissionPage)) {
// 	alertMgr.raiseAlert(Alert.auAlert(Alert.PERMISSION_PAGE_FETCH_ERROR,
// 					  au).
// 			    setAttribute(ATTR_TEXT, "Permission page " +
// 					 permissionPage +
// 					 " is not within the crawl spec"));
	logger.warning("Permission page not within CrawlSpec");
      } else if ((au.getCrawlSpec()!=null) && !au.getCrawlSpec().canCrawl()) {
	logger.debug("Couldn't start crawl due to crawl window.");
	err = Crawler.STATUS_WINDOW_CLOSED;
      } else {
	// check for the permission on the page without storing
	InputStream is = new BufferedInputStream(uc.getUncachedInputStream());
	try {
	  // allow us to reread contents if reasonable size
	  is.mark(PERM_BUFFER_MAX);
	  // set the reader to our default encoding
	  //XXX try to extract encoding from source
	  Reader reader =
	    new InputStreamReader(is, Constants.DEFAULT_ENCODING);
	  if (!au.checkCrawlPermission(reader)) {
	    logger.error("No crawl permission on " + permissionPage);
	    crawl_ok = PermissionRecord.PERMISSION_NOT_OK;
	    alertMgr.raiseAlert(Alert.auAlert(Alert.NO_CRAWL_PERMISSION,
					      au).
				setAttribute(Alert.ATTR_TEXT,
					     "The page at " + permissionPage +
					     "\ndoes not contain the " +
					     "LOCKSS permission statement.\n" +
					     "No collection was done."));
	  } else {
	    if (Configuration.getBooleanParam(PARAM_REFETCH_PERMISSIONS_PAGE,
					      DEFAULT_REFETCH_PERMISSIONS_PAGE)) {
	      logger.debug3("Permission granted. Caching permission page.");
	      storePermissionPage(ownerCus, permissionPage);
	      crawl_ok = PermissionRecord.PERMISSION_OK;
	      if (crawlStatus.getCrawlError() == err){
		crawlStatus.setCrawlError(null);
	      }
	    } else {
	      try {
		logger.debug3("Permission granted. Storing permission page.");
		is.reset();
		uc.storeContent(is, uc.getUncachedProperties());
		crawl_ok = PermissionRecord.PERMISSION_OK;
		if (crawlStatus.getCrawlError() == err){
		  crawlStatus.setCrawlError(null);
		}
	      } catch (IOException e) {
		logger.debug("Couldn't store from existing stream, refetching", e);
		storePermissionPage(ownerCus, permissionPage);
		crawl_ok = PermissionRecord.PERMISSION_OK;
		if (crawlStatus.getCrawlError() == err){
		  crawlStatus.setCrawlError(null);
		}
	      }
	    }
	  }
	} finally {
	  is.close();
	}
      }
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

  void storePermissionPage(CachedUrlSet ownerCus, String permissionPage)
      throws IOException {
    // XXX can't reuse UrlCacher
    UrlCacher uc = makeUrlCacher(ownerCus, permissionPage);
    uc.setRedirectScheme(UrlCacher.REDIRECT_SCHEME_FOLLOW);
    uc.cache();
  }

  /** All UrlCachers should be made via this method, so they get their
   * connection pool set. */
  protected UrlCacher makeUrlCacher(CachedUrlSet cus, String url) {
    ArchivalUnit au = cus.getArchivalUnit();
    Plugin plugin = au.getPlugin();
    UrlCacher uc = plugin.makeUrlCacher(cus, url);
    uc.setConnectionPool(connectionPool);
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
