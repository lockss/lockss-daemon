/*
 * $Id$
 */

/*

 Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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
import org.apache.oro.text.regex.*;

import org.lockss.app.*;
import org.lockss.alert.Alert;
import org.lockss.alert.AlertManager;
import org.lockss.crawler.PermissionRecord.PermissionStatus;
import org.lockss.daemon.*;
import org.lockss.daemon.Crawler;
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
  static Logger logger = Logger.getLogger(PermissionMap.class);

  private ArchivalUnit au;
  private HashMap<String,PermissionRecord> permissionAtUrl;
  private PermissionUrlConsumerFactory pucFactory;
  private CrawlerStatus crawlStatus;
  private Crawler.CrawlerFacade crawlFacade;
  private AlertManager alertMgr;
  private String perHostPermissionPath;
  private Collection<String> pUrls;
  private Collection<PermissionChecker> daemonPermissionCheckers;
  private Collection<PermissionChecker> pluginPermissionCheckers;
  private List<Pattern> pluginPermittedHostPatterns;
  
  public PermissionMap(Crawler.CrawlerFacade crawlFacade,
      Collection<PermissionChecker> daemonPermissionCheckers,
      Collection<PermissionChecker> pluginPermissionCheckers,
      Collection<String> permUrls) {
    if (crawlFacade == null) {
      throw new IllegalArgumentException("Called with null crawler facade");
    }
    pUrls = permUrls;
    this.daemonPermissionCheckers = daemonPermissionCheckers;
    if(pluginPermissionCheckers != null) {
      this.pluginPermissionCheckers = pluginPermissionCheckers;
    }
    permissionAtUrl = new HashMap<String,PermissionRecord>();
    crawlStatus = crawlFacade.getCrawlerStatus();
    this.crawlFacade = crawlFacade;
    this.au = crawlFacade.getAu();
    this.pucFactory = new PermissionUrlConsumerFactory(this);
    try {
      pluginPermittedHostPatterns = au.makePermittedHostPatterns();
    } catch (ArchivalUnit.ConfigurationException e) {
      logger.error("Malformed plugin permitted host pattern", e);
      pluginPermittedHostPatterns = null;
    }
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
  private PermissionRecord get(String url) throws MalformedURLException{
    String host = UrlUtil.getHost(url).toLowerCase();
    
    PermissionRecord res = permissionAtUrl.get(host);
    if (res == null) {
      if (crawlFacade.isGloballyPermittedHost(host)) {
	logger.debug2("Creating globally permitted host PermissionRecord: "
		      + url);
	res = createRecord(url);
	res.setStatus(PermissionStatus.PERMISSION_OK);
	return res;
      }
      if (isPluginPermittedHost(host)) {
	if (crawlFacade.isAllowedPluginPermittedHost(host)) {
	  logger.debug2("Creating plugin permitted host PermissionRecord: "
			+ url);
	  res = createRecord(url);
	  res.setStatus(PermissionStatus.PERMISSION_OK);
	  return res;
	} else {
	  logger.warning("Plugin permitted host not allowed by " +
			 CrawlManagerImpl.PARAM_ALLOWED_PLUGIN_PERMITTED_HOSTS +
			 ": " + url);
	}
      }
      String perHostPath;
      if ((perHostPath = getPerHostPermissionPath()) != null) {
	// if no PermissionRecord for this host, but we have a permission
	// path to use on "unknown" hosts, create a record for it
	String permUrl = UrlUtil.resolveUri(UrlUtil.getUrlPrefix(url),
					    perHostPath);
	logger.debug2("Creating per-host PermissionRecord: " + permUrl);
	res = createRecord(permUrl);
      }
    }
    return res;
  }

  boolean isPluginPermittedHost(String host) {
    if (pluginPermittedHostPatterns != null) {
      return RegexpUtil.isMatch(host, pluginPermittedHostPatterns);
    }
    return false;
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
  public PermissionStatus getStatus(String url) throws MalformedURLException{
    PermissionRecord pr = get(url);
    if (pr == null) {
      return PermissionStatus.PERMISSION_MISSING;
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
  public boolean populate() {
    Map<String, List<String>> hostMap = new HashMap<String, List<String>>();
    for (String permissionPage : pUrls) {
      try {
        String host = UrlUtil.getHost(permissionPage).toLowerCase();
        if(!hostMap.containsKey(host)) {
          ArrayList<String> pageList = new ArrayList<String>();
          pageList.add(permissionPage);
          hostMap.put(host, pageList);
        } else {
          hostMap.get(host).add(permissionPage);
        }
      } catch (MalformedURLException e){
        logger.error("Malformed permission page URL: " + permissionPage);
        crawlStatus.signalErrorForUrl(permissionPage, 
            "Malformed permission page URL");
        return false;
      }
    }
    
    if(perHostPermissionPath != null) {
      try {
        for(Map.Entry<String, List<String>> hostEntry : hostMap.entrySet()) {
          List<String> permUrls = hostEntry.getValue();
          permUrls.add(UrlUtil.resolveUri(
              UrlUtil.getUrlPrefix(permUrls.get(0)),
              perHostPermissionPath));
        }
      } catch(MalformedURLException e) {
        logger.error("Malformed permission page using permission path"
            + perHostPermissionPath);
      }
    }
    logger.info("Checking permission for " + au + " at " + pUrls);
    
    for(Map.Entry<String, List<String>> hostEntry : hostMap.entrySet()) {
      String host = hostEntry.getKey();
      if(!checkPermissionOnHost(hostEntry.getValue(), host)) {
        logger.info("Aborting because no permission on " + host);
        return false;
      }
    }
    //if a bad permission page caused an abort ignore because we have
    //permission
    crawlStatus.setCrawlStatus(Crawler.STATUS_ACTIVE);
    return true;
  }
  
  /**
   * Check permission for a host using a list of permission urls
   */
  private boolean checkPermissionOnHost(List<String> permUrls, String host) {
    for (String pUrl : permUrls) {
      PermissionRecord oldRec = permissionAtUrl.get(host);
      if (oldRec != null) {
        if (oldRec.getStatus() == PermissionStatus.PERMISSION_OK) {
          logger.warning("Already found permission on " + host +
              ", skipping permission page " + pUrl);
          break;
        } else {
          logger.debug("Previous permission page on " + host +
              ", had no permission, trying " + pUrl);
        }
      }
      try {
        PermissionRecord rec = createRecord(pUrl);
        probe(rec);
        switch (rec.getStatus()) {
        case PERMISSION_OK:
          logger.debug3("Permission granted on host: " +
              rec.getHost());
          crawlStatus.signalUrlFetched(pUrl);
          return true;
        case PERMISSION_CRAWL_WINDOW_CLOSED:
          logger.debug3("Crawl Window closed, aborting crawl");
          crawlStatus.setCrawlStatus(Crawler.STATUS_WINDOW_CLOSED);
          return false;
        case PERMISSION_NOT_OK:
        case PERMISSION_FETCH_FAILED:
        case PERMISSION_NOT_IN_CRAWL_SPEC:
        case PERMISSION_MISSING:
        case PERMISSION_REPOSITORY_ERROR:
        case PERMISSION_UNCHECKED:
        default:
          break;
        }
      } catch (MalformedURLException e){
        logger.error("Malformed permission page URL: " + pUrl);
        crawlStatus.signalErrorForUrl(pUrl, 
            "Malformed permission page URL");
      }
    }
    return false;
  }
  
  /**
   * Probe a permission page: check crawl spec, window, etc. then fetch and
   * check page
   * @param rec the PermissionRecord of the page to probe
   * @return a PermissionRecord.PERMISSION_XXX status code
   */
  PermissionStatus probe(PermissionRecord rec) {
    if (getDaemon().isDetectClockssSubscription()) {
      return clockssProbe(rec);
    } else {
      return probe0(rec);
    }
  }

  // CLOCKSS subscription logic.  Should be refactored into separate class.
  PermissionStatus clockssProbe(PermissionRecord rec) {
    PermissionStatus res = probe0(rec);
    if (rec.getStatus() == PermissionStatus.PERMISSION_NOT_OK) {
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
      if (rec.getStatus() == PermissionStatus.PERMISSION_NOT_OK) {
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

  PermissionStatus probe0(PermissionRecord rec) {
    String pUrl = rec.getUrl();
    logger.debug("Checking for permission on " + pUrl);
    try {
      if (!au.shouldBeCached(pUrl)) {
        logger.error("Permission page not within CrawlSpec: "+ pUrl);
        rec.setStatus(PermissionStatus.PERMISSION_NOT_IN_CRAWL_SPEC);
        String msg = "Permission page not within CrawlSpec";
        crawlStatus.signalErrorForUrl(pUrl, msg,
                                      Crawler.STATUS_PLUGIN_ERROR, msg);
      } else if (!au.inCrawlWindow()) {
        logger.debug("Crawl window closed, aborting permission check.");
        crawlStatus.setCrawlStatus(Crawler.STATUS_WINDOW_CLOSED);
        rec.setStatus(PermissionStatus.PERMISSION_CRAWL_WINDOW_CLOSED);
      } else {
        // fetch the page and check for the permission statement
        UrlFetcher uf = crawlFacade.makePermissionUrlFetcher(pUrl);
        uf.setUrlConsumerFactory(pucFactory);
        //fetch might set the crawl status
        uf.fetch();
        if (rec.getStatus() == PermissionStatus.PERMISSION_NOT_OK) {
          logger.siteError("No permission statement at " + pUrl);
          signalErrorForUrlNoOverride(pUrl,
              CrawlerStatus.NO_PERM_STATMENT_ERR_MSG,
              Crawler.STATUS_NO_PUB_PERMISSION);

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
      rec.setStatus(PermissionStatus.PERMISSION_REPOSITORY_ERROR);
      crawlStatus.signalErrorForUrl(pUrl,
            "Can't store page: " + ex.getMessage(),
            Crawler.STATUS_REPO_ERR);
    } catch (CacheException ex) {
      logger.siteError("CacheException reading permission page", ex);
      rec.setStatus(PermissionStatus.PERMISSION_FETCH_FAILED);
      crawlStatus.signalErrorForUrl(pUrl, ex.getMessage(),
            Crawler.STATUS_NO_PUB_PERMISSION,
            CrawlerStatus.UNABLE_TO_FETCH_PERM_ERR_MSG);
    } catch (Exception ex) {
      logger.error("Exception reading permission page at " + pUrl, ex);
      rec.setStatus(PermissionStatus.PERMISSION_FETCH_FAILED);
      crawlStatus.signalErrorForUrl(pUrl, ex.getMessage(),
            Crawler.STATUS_FETCH_ERROR);
      raiseAlert(Alert.auAlert(Alert.PERMISSION_PAGE_FETCH_ERROR, au).
     setAttribute(Alert.ATTR_TEXT,
            "The LOCKSS permission page at " + pUrl +
            "\ncould not be fetched. " +
            "The error was:\n" + ex.getMessage() + "\n"));
    } finally {
      if(rec.getStatus() == PermissionStatus.PERMISSION_UNCHECKED) {
        logger.error("Permission page unchecked");
        rec.setStatus(PermissionStatus.PERMISSION_FETCH_FAILED);
        signalErrorForUrlNoOverride(pUrl, "Permission check failed",
            Crawler.STATUS_NO_PUB_PERMISSION);
        raiseAlert(Alert.auAlert(Alert.PERMISSION_PAGE_FETCH_ERROR, au).
            setAttribute(Alert.ATTR_TEXT,
                "The LOCKSS permission page at " + pUrl +
                "\ncould not be checked \n"));
      }
    }
    return rec.getStatus();
  }
  
  private void signalErrorForUrlNoOverride(String url, 
                                           String urlMsg, 
                                           int status) {
    if(crawlStatus.getErrorInfoForUrl(url) == null) {
      crawlStatus.signalErrorForUrl(url, urlMsg, status);
    }
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
      String msg = "Malformed permission page url";
      logger.error(msg + ": " + url);
      crawlStatus.signalErrorForUrl(url, msg + ": " + url,
                                    Crawler.STATUS_PLUGIN_ERROR,
                                    msg);
      return false;
    }
    PermissionStatus stat;
    String pUrl = null;
    if (rec != null) {
      stat = rec.getStatus();
      pUrl = rec.getUrl();
    } else {
      stat = PermissionStatus.PERMISSION_MISSING;
    }
    switch (stat) {
      case PERMISSION_OK:
        return true;
      case PERMISSION_NOT_OK:
        logger.siteError("No permission statement on manifest page: " + pUrl);
        crawlStatus.setCrawlStatus(Crawler.STATUS_NO_PUB_PERMISSION,
				   "No permission statement on manifest page.");
        return false;
      case PERMISSION_MISSING:
        logger.error("Plugin error: no permission page specified for host of: "
		     + url);
        if (crawlStatus.getErrorForUrl(pUrl) == null) {
          crawlStatus.signalErrorForUrl(url, "No permission URL for this host");
        } 
        crawlStatus.setCrawlStatus(Crawler.STATUS_NO_PUB_PERMISSION,
				                   "Plugin error (missing permission URL)");
        return false;
      case PERMISSION_NOT_IN_CRAWL_SPEC:
        String err1 = "Permission page not in crawl spec";
        logger.error(err1 + ": " + url);
        if (crawlStatus.getErrorForUrl(pUrl) == null) {
          crawlStatus.signalErrorForUrl(url, err1);
        }   
        crawlStatus.setCrawlStatus(Crawler.STATUS_PLUGIN_ERROR, err1);
        return false;
      case PERMISSION_UNCHECKED:
        // per-host permission record creted on the fly
        logger.debug3("Permission unchecked for host: " + pUrl);
        //fetch page then recurse once
        probe(rec);
        return hasPermission(url, false);
      case PERMISSION_FETCH_FAILED:
        if (retryIfFailed) {
          logger.siteWarning("Failed to fetch permission page, retrying: " +
              pUrl);
	        probe(rec);
          return hasPermission(url, false);
        } else {
          logger.siteError("Can't fetch permission page on second attempt: " +
              pUrl);
          if (crawlStatus.getErrorForUrl(pUrl) == null) {
            crawlStatus.signalErrorForUrl(pUrl,
                "Cannot fetch permission page "
                + "on the second attempt");
          }
          crawlStatus.setCrawlStatus(Crawler.STATUS_NO_PUB_PERMISSION,
				       "Cannot fetch permission page.");
          return false;
        }
      case PERMISSION_CRAWL_WINDOW_CLOSED:
        logger.debug("Couldn't fetch permission page, " +
            "because crawl window was closed");
        crawlStatus.setCrawlStatus(Crawler.STATUS_WINDOW_CLOSED);
        return false;
      case PERMISSION_REPOSITORY_ERROR:
        logger.error("Error trying to store: " + pUrl);
        String msg = "Repository error storing permission page";
        if (crawlStatus.getErrorForUrl(pUrl) == null) {
          crawlStatus.signalErrorForUrl(pUrl, msg);
        }
        crawlStatus.setCrawlStatus(Crawler.STATUS_REPO_ERR, msg);
        return false;
      default :
        logger.error("Unknown Permission Status! Shouldn't happen");
      return false;
    }
  }

  public void setPerHostPermissionPath(String absolutePath)
      throws MalformedURLException {
    if (!absolutePath.startsWith("/")) {
      throw new MalformedURLException(
          "Per-host permission path must begin with slash: " + absolutePath);
    }
    perHostPermissionPath = absolutePath;
  }

  public String getPerHostPermissionPath() {
    return perHostPermissionPath;
  }

  static class IgnoreCloseInputStream extends FilterInputStream {
    public IgnoreCloseInputStream(InputStream stream) {
      super(stream);
    }
    public void close() throws IOException {
      // ignore
    }
  }

  //PermissionUrlConsumer calls these
  public Collection<PermissionChecker> getDaemonPermissionCheckers() {
    return daemonPermissionCheckers;
  }
  
  public Collection<PermissionChecker> getPluginPermissionCheckers() {
    return pluginPermissionCheckers;
  }
  
  public void setPermissionResult(String url, PermissionStatus status) {
    try {
      PermissionRecord rec = get(url);
      if(rec != null) {
        rec.setStatus(status);
      }
    } catch (MalformedURLException e) {
      logger.error("Malformed permission page url: " + url);
      if (crawlStatus.getErrorForUrl(url) == null) {
        crawlStatus.signalErrorForUrl(url, "Malformed permission page url");
      }
      crawlStatus.setCrawlStatus(Crawler.STATUS_PLUGIN_ERROR,
         "Malformed permission page url");
    }
  }
}
