/*
 * $Id: PermissionMap.java,v 1.7 2005-12-01 23:28:01 troberts Exp $
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
import java.util.*;

import org.lockss.alert.Alert;
import org.lockss.alert.AlertManager;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.daemon.Crawler.Status;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.CacheException;
import org.lockss.util.urlconn.CacheException.RepositoryException;

import java.io.*;
import java.net.MalformedURLException;

/**
 * The object that figures out whether we have permission to harvest a given
 * URL or not
 *
 */

public class PermissionMap {
  private HashMap hMap;
  private List daemonPermissionCheckers;
  private PermissionChecker pluginPermissionChecker;
  private ArchivalUnit au;

  private Crawler.Status crawlStatus;

  private AlertManager alertMgr = (AlertManager)org.lockss.app.LockssDaemon.getManager(org.lockss.app.LockssDaemon.ALERT_MANAGER);


  private Crawler.PermissionHelper pHelper;

  static Logger logger = Logger.getLogger("PermissionMap");

  public PermissionMap(ArchivalUnit au, Crawler.PermissionHelper pHelper,
                       List daemonPermissionCheckers) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null AU");
    } else if (pHelper == null) {
      throw new IllegalArgumentException("Called with null crawler");
    }
    hMap = new HashMap();
    crawlStatus = pHelper.getCrawlStatus();
    this.pHelper = pHelper;
    this.au = au;
    this.daemonPermissionCheckers = daemonPermissionCheckers;
  }

  /**
   * Put a object to a hashmap using the lowercased host name of permissionUrl as the key.
   * The object contains the host's permission url and permission status.
   *
   * @param permissionUrl the host's permission url
   * @param status the host's permission status
   */
  protected void putStatus(String permissionUrl, int status)
        throws MalformedURLException {
    hMap.put(UrlUtil.getHost(permissionUrl).toLowerCase(), new PermissionRecord(permissionUrl,status));
  }

  /**
   * Get a PermissionRecord from host name's url as the key
   *
   * @param url the host's url
   * @return PermissionRecord of the host of url
   */
  public PermissionRecord get(String url) throws MalformedURLException{
    return (PermissionRecord) hMap.get(UrlUtil.getHost(url).toLowerCase());
  }

  /**
   * Get the host's permission url from a url
   *
   * @param url a url
   * @return the host's permission url of the given url
   */
  protected String getPermissionUrl(String url) throws MalformedURLException{
    PermissionRecord pr = get(url);
    if (pr == null) {
      return null;
    }
    return pr.getPermissionUrl();
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
    return pr.getPermissionStatus();
  }

  /**
   * Check the permission of each url in the permission list, then save the result
   * in the permission map.
   *
   * @return if all permission pages grant permission to crawl
   */
  public boolean init() {
    List permissionList = au.getCrawlSpec().getPermissionPages();
    boolean abortWhilePermissionOtherThanOk =
      CurrentConfig.getBooleanParam(BaseCrawler.PARAM_ABORT_WHILE_PERMISSION_OTHER_THAN_OK,
                                    BaseCrawler.DEFAULT_ABORT_WHILE_PERMISSION_OTHER_THAN_OK);

    logger.info("Checking permission on host(s) of " + au);
    Iterator permissionUrls = permissionList.iterator();
    while (permissionUrls.hasNext()) {
      String permissionPage = (String)permissionUrls.next();
      // it is the real thing that do the checking of permission, crawlPermission dwell in BaseCrawler.java
      int permissionStatus = crawlPermission(pHelper, permissionPage);
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
        putStatus(permissionPage, permissionStatus);
      } catch (MalformedURLException e){
        //XXX should catch this inside the permissionMap ?
        logger.error("The permissionPage's URL is Malformed : "+ permissionPage);
        crawlStatus.setCrawlError("Malformed permission page url");
      }
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
  public boolean checkHostPermission(String url,
                                     boolean permissionFailedRetry,
                                     Crawler.Status crawlStatus,
                                     BaseCrawler crawler) {
    int urlPermissionStatus = -1;
    String urlPermissionUrl = null;
    logger.debug3("Checking permission for "+url);
    try {
      urlPermissionStatus = getStatus(url);
      urlPermissionUrl = getPermissionUrl(url);
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
            putStatus(urlPermissionUrl, crawlPermission(crawler, urlPermissionUrl));
          } catch (MalformedURLException e){
            //XXX can we handle this better by centralizing the check of MalformedURL ?
            logger.error("Malformed urlPermissionUrl :" + urlPermissionUrl, e);
            crawlStatus.setCrawlError("MalFormedUrl :" + urlPermissionUrl);
            return false;
          }
          return checkHostPermission(url, false, crawlStatus, crawler);
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
   * checkPermission check the permission page for all of the required permission
   * objects.
   *
   * @param permissionPage String
   * @param crawlStatus TODO
   * @return boolean iff permission was found for each object on the page.
   */
  private boolean checkPermission(String permissionPage,
                                  Status crawlStatus) throws IOException {

    PermissionChecker checker;
    // fetch and cache the permission page
    UrlCacher uc = pHelper.makeUrlCacher(permissionPage);
    uc.setRedirectScheme(UrlCacher.REDIRECT_SCHEME_FOLLOW_ON_HOST);

    BufferedInputStream is =
      new BufferedInputStream(uc.getUncachedInputStream());
    crawlStatus.signalUrlFetched(uc.getUrl());
    // allow us to reread contents if reasonable size
    boolean needPermission = true;
    try {
      // check the lockss checkers and find at least one checker that matches
      for (Iterator it = daemonPermissionCheckers.iterator(); it.hasNext(); ) {
        is.mark(BaseCrawler.PERM_BUFFER_MAX);
        checker = (PermissionChecker) it.next();
        Reader reader = new InputStreamReader(is, Constants.DEFAULT_ENCODING);
        if (checker.checkPermission(reader, permissionPage)) {
          logger.debug3("Found permission on "+checker);
          needPermission = false;
          break; //we just need one permission to de sucessful here
        } else {
          logger.debug3("Didn't find permission on "+checker);
          is = pHelper.resetInputStream(is, permissionPage);
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
      is = pHelper.resetInputStream(is, permissionPage);

      is.mark(BaseCrawler.PERM_BUFFER_MAX);
      Reader reader = new InputStreamReader(is, Constants.DEFAULT_ENCODING);
      if (pluginPermissionChecker != null
          && !pluginPermissionChecker.checkPermission(reader, permissionPage)) {
        logger.error("No plugin crawl permission on " + permissionPage);
        is.close();
        return false;
      } else {
        is = pHelper.resetInputStream(is, permissionPage);
      }

      if (CurrentConfig.getBooleanParam(BaseCrawler.PARAM_REFETCH_PERMISSIONS_PAGE,
                                        BaseCrawler.DEFAULT_REFETCH_PERMISSIONS_PAGE)) {
        logger.debug3("Permission granted. Caching permission page.");
        pHelper.refetchPermissionPage(permissionPage);
      } else {
        uc.storeContent(is, uc.getUncachedProperties());
      }
    } finally {
      IOUtil.safeClose(is);
    }

    return true;
  }



  /**
   * Method used by subclasses to check crawl permission
   *
   * @param permissionPage string representation of the URL of the permission
   * page
   * @return PERMISSION_UNCHECKED if we can't get the permission page,
   * otherwise PERMISSION_OK if there is an appropriate permission
   * statement on the specified page, PERMISSION_NOT_OK otherwise
   */

  private int crawlPermission(Crawler.PermissionHelper pHelper,
                              String permissionPage) {

    int crawl_ok = PermissionRecord.PERMISSION_UNCHECKED;
    String err = Crawler.STATUS_NO_PUB_PERMISSION;
    logger.debug("Checking for permissions on " + permissionPage);
    try {
      if (!au.shouldBeCached(permissionPage)) {
        logger.warning("Permission page not within CrawlSpec: "+permissionPage);
      } else if (!au.getCrawlSpec().inCrawlWindow()) {
        logger.debug("Couldn't start crawl due to crawl window.");
        err = Crawler.STATUS_WINDOW_CLOSED;
      } else {
        // go off to fetch the url and check for the permission statement
        if(checkPermission(permissionPage, crawlStatus)) {
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
    } catch (RepositoryException ex) {
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
}
