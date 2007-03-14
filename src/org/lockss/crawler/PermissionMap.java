/*
 * $Id: PermissionMap.java,v 1.18 2007-03-14 05:52:48 tlipkis Exp $
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
import java.util.*;

import org.lockss.app.*;
import org.lockss.alert.Alert;
import org.lockss.alert.AlertManager;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.state.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.CacheException;

import java.io.*;
import java.net.MalformedURLException;

/**
 * Creates and maintains a map from permission page url to
 * PermissionRecord, which record the permissions we found at that url
 */
public class PermissionMap {
  static Logger logger = Logger.getLogger("PermissionMap");

  private ArchivalUnit au;
  private HashMap permissionAtUrl;
  private List daemonPermissionCheckers;
  private PermissionChecker pluginPermissionChecker;

  private CrawlerStatus crawlStatus;
  private Crawler.PermissionHelper pHelper;
  private AlertManager alertMgr;

  public PermissionMap(ArchivalUnit au, Crawler.PermissionHelper pHelper,
                       List daemonPermissionCheckers,
		       PermissionChecker pluginPermissionChecker) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null AU");
    } else if (pHelper == null) {
      throw new IllegalArgumentException("Called with null crawler");
    }
    permissionAtUrl = new HashMap();
    crawlStatus = pHelper.getCrawlStatus();
    this.pHelper = pHelper;
    this.au = au;
    this.daemonPermissionCheckers = daemonPermissionCheckers;
    this.pluginPermissionChecker = pluginPermissionChecker;
  }

  protected PermissionRecord createRecord(String pUrl)
      throws MalformedURLException {
    String host = UrlUtil.getHost(pUrl).toLowerCase();
    PermissionRecord rec = new PermissionRecord(pUrl, host);
    permissionAtUrl.put(host, rec);
    return rec;
  }

  /**
   * Get the PermissionRecord for the URL's host
   *
   * @param url URL that specifies host of desired PermissionRecord
   * @return PermissionRecord for the host
   */
  public PermissionRecord get(String url) throws MalformedURLException{
    String key = UrlUtil.getHost(url).toLowerCase();
    return (PermissionRecord)permissionAtUrl.get(key);
  }

  /**
   * Get the URL of a host's permission page from a url
   *
   * @param url a url
   * @return the host's permission url of the given url
   */
  protected String getPermissionUrl(String url) throws MalformedURLException{
    PermissionRecord pr = get(url);
    if (pr == null) {
      return null;
    }
    return pr.getUrl();
  }

  /**
   * Get the host's permission status from a url
   *
   * @param url a url
   * @return the host's permission status of the given url
   */
  public int getStatus(String url) throws MalformedURLException{
    PermissionRecord pr = get(url);
    if (pr == null) {
      return PermissionRecord.PERMISSION_MISSING;
    }
    return pr.getStatus();
  }

  private void raiseAlert(Alert alert) {
    try {
      if (alertMgr == null) {
	alertMgr = AuUtil.getDaemon(au).getAlertManager();
      }
      alertMgr.raiseAlert(alert);
    } catch (RuntimeException e) {
      logger.error("Couldn't raise alert", e);
    }
  }

  private LockssDaemon getDaemon() {
    return AuUtil.getDaemon(au);
  }

  /**
   * Populate the map with a PermissionRecord for each permission URL
   * supplied by the AU.
   * @return true if the crawl should proceed; <i>ie</i>, all permission
   * pages grant permission.
   */
  public boolean init() {
    List pUrls = au.getCrawlSpec().getPermissionPages();
    boolean abortOnFirstNoPermission =
      CurrentConfig.getBooleanParam(BaseCrawler.PARAM_ABORT_ON_FIRST_NO_PERMISSION,
                                    BaseCrawler.DEFAULT_ABORT_ON_FIRST_NO_PERMISSION);

    logger.info("Checking permission for " + au + " at " + pUrls);
    for (Iterator iter = pUrls.iterator(); iter.hasNext(); ) {
      String permissionPage = (String)iter.next();
      try {
	PermissionRecord rec = createRecord(permissionPage);
	probe(rec);
	switch (rec.getStatus()) {
	case PermissionRecord.PERMISSION_OK:
	  logger.debug3("Permission granted on host: " +
			rec.getHost());
	  break;
	case PermissionRecord.PERMISSION_NOT_OK:
	case PermissionRecord.PERMISSION_FETCH_FAILED:
	case PermissionRecord.PERMISSION_NOT_IN_CRAWL_SPEC:
	  if (abortOnFirstNoPermission) {
	    logger.info("Aborting because no permission at " + permissionPage);
	    return false;
	  }
	  break;
	}
      } catch (MalformedURLException e){
	logger.error("Malformed permission page URL: " + permissionPage);
	crawlStatus.setCrawlError("Malformed permission page url");
	return false;
      }
    }
    // If we're not insisting on success on the first pass, reset any error
    // we might have encountered
    crawlStatus.setCrawlError(null);
    return true;
  }

  /**
   * Main entry point from crawler, etc. to check whether we have
   * permission to collect the given URL.
   * @param url the url we want to collect.
   * @return if there is a permission that grants permission for the URL to
   * be crawled.
   */
  public boolean hasPermission(String url) {
    return hasPermission(url, true);
  }

  /**
   * Check whether we have permission to collect the given URL.
   * @param url the url we want to collect.
   * @param retryIfFailed true to force rechecking a permission
   * page that we couldn't initially fetch
   * @return if there is a permission that grants permission for the URL to
   * be crawled.
   */
  private boolean hasPermission(String url, boolean retryIfFailed) {
    logger.debug3("Checking permission for "+url);
    PermissionRecord rec;
    try {
      rec = get(url);
    } catch (MalformedURLException e) {
      logger.error("Can't check permission for URL: " + url + ", " + e);
      crawlStatus.setCrawlError("Malformed Url: " + url);
      return false;
    }
    String pUrl = rec.getUrl();
    switch (rec.getStatus()) {
      case PermissionRecord.PERMISSION_OK:
        return true;
      case PermissionRecord.PERMISSION_NOT_OK:
        logger.error("No permission statement on manifest page: " + pUrl);
        crawlStatus.setCrawlError("No permission statement on manifest page.");
        return false;
      case PermissionRecord.PERMISSION_MISSING:
	String err0 = "No permission page specified for host of: "+ url;
        logger.error(err0);
        crawlStatus.setCrawlError(err0);
        return false;
      case PermissionRecord.PERMISSION_NOT_IN_CRAWL_SPEC:
	String err1 = "Permission page not in crawl spec: "+ url;
        logger.error(err1);
        crawlStatus.setCrawlError(err1);
        return false;
      case PermissionRecord.PERMISSION_UNCHECKED:
        // shouldn't happen
        logger.error("Permission unchecked for host: " + pUrl);
        // fall through, re-fetch permission like PERMISSION_FETCH_FAILED
      case PermissionRecord.PERMISSION_CRAWL_WINDOW_CLOSED:
	logger.debug("Couldn't fetch permission page, " +
			"because crawl window was closed");
	crawlStatus.setCrawlError("Crawl window closed.");
	return false;
      case PermissionRecord.PERMISSION_FETCH_FAILED:
        if (retryIfFailed) {
	  logger.siteWarning("Failed to fetch permission page, retrying: " +
			     pUrl);
          // refetch page then recurse once
	  probe(rec);
          return hasPermission(url, false);
        } else {
          logger.siteError("Can't fetch permission page on second attempt: " +
			   pUrl);
          crawlStatus.setCrawlError("Cannot fetch permission page.");
 	  if (crawlStatus.getErrorForUrl(pUrl) == null) {
	    crawlStatus.signalErrorForUrl(pUrl,
					  "Cannot fetch permission page " +
					  "on the second attempt");
	  }
          return false;
        }
      case PermissionRecord.PERMISSION_REPOSITORY_ERROR:
        logger.error("Error trying to store: " + pUrl);
        crawlStatus.setCrawlError("Repository error");
	if (crawlStatus.getErrorForUrl(pUrl) == null) {
	  crawlStatus.signalErrorForUrl(pUrl, "Repository error");
	}
        return false;
      default :
        logger.error("Unknown Permission Status! Shouldn't happen");
      return false;
    }
  }

  /**
   * Probe a permission page: check crawl spec, window, etc. then fetch and
   * check page
   * @param rec the PermissionRecord of the page to probe
   * @return a PermissionRecord.PERMISSION_XXX status code
   */
  int probe(PermissionRecord rec) {
    if (getDaemon().isClockss()) {
      return clockssProbe(rec);
    } else {
      return probe0(rec);
    }
  }

  // CLOCKSS subscription logic.  Should be refactored into separate class.
  int clockssProbe(PermissionRecord rec) {
    int res = probe0(rec);
    if (rec.getStatus() == PermissionRecord.PERMISSION_NOT_OK) {
      // If the permission page doesn't contain a permission statement,
      // and we got it from the institution's IP address, try again from
      // the CLOCKSS address; we might get different content.
      AuState aus = AuUtil.getAuState(au);
      if (aus.getClockssSubscriptionStatus() == AuState.CLOCKSS_SUB_YES) {
	aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_NO);
	res = probe0(rec);
      }
      // if we still didn't find permission, reset the subscription state
      // to inaccessible
      if (rec.getStatus() == PermissionRecord.PERMISSION_NOT_OK) {
	switch (aus.getClockssSubscriptionStatus()) {
	case AuState.CLOCKSS_SUB_UNKNOWN:
	  logger.error("Impossible CLOCKSS subscription state: UNKNOWN");
	  // fall through to set inaccessible
	case AuState.CLOCKSS_SUB_YES:
	case AuState.CLOCKSS_SUB_NO:
	  aus.setClockssSubscriptionStatus(AuState. CLOCKSS_SUB_INACCESSIBLE);
	  break;
	case AuState.CLOCKSS_SUB_INACCESSIBLE:
	  break;
	}
      }
    }
    return res;
  }

  int probe0(PermissionRecord rec) {
    String pUrl = rec.getUrl();
    String crawlErr = Crawler.STATUS_NO_PUB_PERMISSION;
    logger.debug("Probing for permission on " + pUrl);
    int status = rec.getStatus();
    try {
      if (!au.shouldBeCached(pUrl)) {
        logger.error("Permission page not within CrawlSpec: "+ pUrl);
        crawlErr = Crawler.STATUS_PLUGIN_ERROR;
	status = PermissionRecord.PERMISSION_NOT_IN_CRAWL_SPEC;
	crawlStatus.signalErrorForUrl(pUrl,
				      "Permission page not within CrawlSpec");
      } else if (!au.getCrawlSpec().inCrawlWindow()) {
        logger.debug("Crawl window closed, aborting permission check.");
        crawlErr = Crawler.STATUS_WINDOW_CLOSED;
	status = PermissionRecord.PERMISSION_CRAWL_WINDOW_CLOSED;
      } else {
        // fetch the ppage and check for the permission statement
	UrlCacher uc = pHelper.makeUrlCacher(pUrl);
        if (fetchAndCheck(uc, crawlStatus)) {
          status = PermissionRecord.PERMISSION_OK;
        } else {
          logger.siteError("No permission statement at " + pUrl);
          status = PermissionRecord.PERMISSION_NOT_OK;

          raiseAlert(Alert.auAlert(Alert.NO_CRAWL_PERMISSION, au).
		     setAttribute(Alert.ATTR_TEXT,
				  "The page at " + pUrl +
				  "\ndoes not contain a " +
				  "LOCKSS permission statement.\n" +
				  "No collection was done."));
        }
      }
    } catch (CacheException.RepositoryException ex) {
      logger.error("RepositoryException storing permission page", ex);
      // XXX should be an alert here
      status = PermissionRecord.PERMISSION_REPOSITORY_ERROR;
      crawlStatus.signalErrorForUrl(pUrl,
				    "Can't store page: " + ex.getMessage());
      crawlErr = Crawler.STATUS_REPO_ERR;
    } catch (CacheException ex) {
      logger.error("CacheException reading permission page", ex);
      status = PermissionRecord.PERMISSION_FETCH_FAILED;
      crawlStatus.signalErrorForUrl(pUrl, ex.getMessage());
      crawlErr = "Cannot fetch permission page.";
    } catch (Exception ex) {
      logger.error("Exception reading permission page", ex);
      status = PermissionRecord.PERMISSION_FETCH_FAILED;
      crawlStatus.signalErrorForUrl(pUrl, ex.toString());
      raiseAlert(Alert.auAlert(Alert.PERMISSION_PAGE_FETCH_ERROR, au).
		 setAttribute(Alert.ATTR_TEXT,
			      "The LOCKSS permission page at " + pUrl +
			      "\ncould not be fetched. " +
			      "The error was:\n" + ex.getMessage() + "\n"));
    }

    if (status != PermissionRecord.PERMISSION_OK) {
      crawlStatus.setCrawlError(crawlErr);
    }
    rec.setStatus(status);
    return status;
  }

  /**
   * Fetch the permission page and check for all required permission
   * objects.
   *
   * @param uc a UrlCacher for the permission page URL
   * @param crawlStatus
   * @return true iff all required permission checkers were satisfied.
   */
  private boolean fetchAndCheck(UrlCacher uc, CrawlerStatus crawlStatus)
      throws IOException {

    String pUrl = uc.getUrl();
    PermissionChecker checker;
    // fetch and cache the permission page
    uc.setRedirectScheme(UrlCacher.REDIRECT_SCHEME_FOLLOW_ON_HOST);

    BufferedInputStream is =
      new BufferedInputStream(uc.getUncachedInputStream());
    crawlStatus.signalUrlFetched(uc.getUrl());
    boolean needPermission = true;
    try {
      // check the lockss checkers and find at least one checker that matches
      for (Iterator it = daemonPermissionCheckers.iterator(); it.hasNext(); ) {
	// allow us to reread contents if reasonable size
        is.mark(BaseCrawler.PERM_BUFFER_MAX);
        checker = (PermissionChecker) it.next();
        Reader reader = new InputStreamReader(is, Constants.DEFAULT_ENCODING);
        if (checker.checkPermission(pHelper, reader, pUrl)) {
          logger.debug3("Found permission on "+checker);
          needPermission = false;
          break; //we just need one permission to be sucessful here
        } else {
          logger.debug3("Didn't find permission on "+checker);
          is = pHelper.resetInputStream(is, pUrl);
        }
      }
      // if we didn't find at least one required lockss permission - fail.
      if(needPermission) {
        logger.error("No (C)LOCKSS crawl permission on " + pUrl);
        is.close();
        return false;
      }

      //either the pluginPermissionCheckers will need this
      //or the storeContent call will
      is = pHelper.resetInputStream(is, pUrl);

      is.mark(BaseCrawler.PERM_BUFFER_MAX);
      Reader reader = new InputStreamReader(is, Constants.DEFAULT_ENCODING);
      if (pluginPermissionChecker != null) {
	if (!pluginPermissionChecker.checkPermission(pHelper, reader, pUrl)) {
	  logger.error("No plugin crawl permission on " + pUrl);
	  is.close();
	  return false;
	} else {
	  is = pHelper.resetInputStream(is, pUrl);
	}
      }

      if (CurrentConfig.getBooleanParam(BaseCrawler.PARAM_REFETCH_PERMISSIONS_PAGE,
                                        BaseCrawler.DEFAULT_REFETCH_PERMISSIONS_PAGE)) {
        logger.debug3("Permission granted. Caching permission page.");
        pHelper.refetchPermissionPage(pUrl);
      } else {
        uc.storeContent(is, uc.getUncachedProperties());
	CachedUrl cu = uc.getCachedUrl();
	if (cu != null && cu.hasContent()) {
	  crawlStatus.addContentBytesFetched(cu.getContentSize());
	}
      }
    } finally {
      IOUtil.safeClose(is);
    }

    return true;
  }
}
