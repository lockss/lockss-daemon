/*
 * $Id: FollowLinkCrawler.java,v 1.1 2004-09-27 23:02:43 dcfok Exp $
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
import java.net.*;
import java.io.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.state.*;

/**
 * A abstract class that implemented by NewContentCrawler and OaiCrawler
 * it has the follow link mechanism that used by NewContentCrawler 
 * and OaiCrawler.
 */
public abstract class FollowLinkCrawler extends CrawlerImpl {

  private static Logger logger = Logger.getLogger("FollowLinkCrawler");
  private Set failedUrls = new HashSet();

  private static final String PARAM_RETRY_TIMES =
    Configuration.PREFIX + "CrawlerImpl.numCacheRetries";
  private static final int DEFAULT_RETRY_TIMES = 3;

  public static final String PARAM_RETRY_PAUSE =
    Configuration.PREFIX + "CrawlerImpl.retryPause";
  public static final long DEFAULT_RETRY_PAUSE = 10*Constants.SECOND;

  public static final String PARAM_REPARSE_ALL =
    Configuration.PREFIX + "CrawlerImpl.reparse_all";
  public static final boolean DEFAULT_REPARSE_ALL = true;

  public static final String PARAM_PERSIST_CRAWL_LIST =
    Configuration.PREFIX + "CrawlerImpl.persist_crawl_list";
  public static final boolean DEFAULT_PERSIST_CRAWL_LIST = false;

  public static final String PARAM_REFETCH_DEPTH =
    Configuration.PREFIX + "crawler.refetchDepth.au.<auid>";

  public static final String PARAM_MAX_CRAWL_DEPTH =
    Configuration.PREFIX + "CrawlerImpl.maxCrawlDepth";
  //testing max. crawl Depth of a site, subject to be changed
  public static final int DEFAULT_MAX_CRAWL_DEPTH = 1000;

  public static final String PARAM_ABORT_WHILE_PERMISSION_OTHER_THAN_OK =
    Configuration.PREFIX + "CrawlerImpl.abortWhilePermissionOtherThanOk";
  public static final boolean DEFAULT_ABORT_WHILE_PERMISSION_OTHER_THAN_OK = false;

  private PermissionMap permissionMap = new PermissionMap();

  private boolean alwaysReparse = DEFAULT_REPARSE_ALL;
  private boolean usePersistantList = DEFAULT_PERSIST_CRAWL_LIST;
  protected int maxDepth = DEFAULT_MAX_CRAWL_DEPTH;
  private int maxRetries = DEFAULT_RETRY_TIMES;
  protected int lvlCnt = 0;
  protected CachedUrlSet cus;
  protected Set parsedPages;
  protected Set extractedUrls;

  public FollowLinkCrawler(ArchivalUnit au, CrawlSpec spec, AuState aus) {
    super(au, spec, aus);
    crawlStatus = new Crawler.Status(au, spec.getStartingUrls(), getType());
  }

  /**
   * This method is implemented in NewContentCrawler and OaiCrawler.
   * It gives the different crawlers to have different mechanism to collect
   * those "initial" urls of a crawl.
   *
   * @return a set of urls to crawl for updated contents
   */
  protected abstract Set getLinks();

  protected void setCrawlConfig(Configuration config) {
    super.setCrawlConfig(config);
    alwaysReparse = config.getBoolean(PARAM_REPARSE_ALL, DEFAULT_REPARSE_ALL);
    usePersistantList = config.getBoolean(PARAM_PERSIST_CRAWL_LIST,
					  DEFAULT_PERSIST_CRAWL_LIST);
    maxDepth = config.getInt(PARAM_MAX_CRAWL_DEPTH, DEFAULT_MAX_CRAWL_DEPTH);
    maxRetries = config.getInt(PARAM_RETRY_TIMES, DEFAULT_RETRY_TIMES);
  }

  protected boolean doCrawl0() {
    if (crawlAborted) {
      return aborted();
    }
    logger.info("Beginning depth " + maxDepth + " crawl of " + au);
    crawlStatus.signalCrawlStarted();
    cus = au.getAuCachedUrlSet();
    parsedPages = new HashSet();

    // get the permission list from crawl spec
    List permissionList = spec.getPermissionPages();
    if (permissionList == null || permissionList.size() == 0){
      logger.error("spec.getPermissionPages() return null list or nothing in the list!");
      return aborted();
    }

    if (!checkPermissionList(permissionList)){
      return aborted();
    }

    extractedUrls = getLinks();

    if (crawlAborted) {
        return aborted();
    }

    //we don't alter the crawl list from AuState until we've enumerated the
    //urls that need to be recrawled.
    Collection urlsToCrawl; 

    if (usePersistantList) {
      urlsToCrawl = aus.getCrawlUrls();
      urlsToCrawl.addAll(extractedUrls);
      extractedUrls.clear();
    } else {
      urlsToCrawl = extractedUrls;
    }

    while (lvlCnt <= maxDepth && !urlsToCrawl.isEmpty() ) {

      logger.debug2("Crawling at level " + lvlCnt);
      extractedUrls = new HashSet(); // level (N+1)'s Urls

      while (!urlsToCrawl.isEmpty() && !crawlAborted) {
	String nextUrl = (String)CollectionUtil.removeElement(urlsToCrawl);

	logger.debug3("Trying to process " + nextUrl);

	// check crawl window during crawl
	if (!withinCrawlWindow()) {
	  crawlStatus.setCrawlError(Crawler.STATUS_WINDOW_CLOSED);
	  return false;
	}
	boolean crawlRes = false;
	try {
	  crawlRes = fetchAndParse(nextUrl, extractedUrls, parsedPages,
 	  			   cus, false, alwaysReparse);
	} catch (RuntimeException e) {
	  logger.warning("Unexpected exception in crawl", e);
	}
	if  (!crawlRes) {
	  if (crawlStatus.getCrawlError() == null) {
	    crawlStatus.setCrawlError(Crawler.STATUS_ERROR);
	  }
	}
	if (usePersistantList) {
	  aus.updatedCrawlUrls(false);
	}

      } // end of inner while

      urlsToCrawl = extractedUrls;
      lvlCnt++;
    } // end of outer while

    if (!urlsToCrawl.isEmpty()) {
      logger.error("Site depth exceeds max. crawl depth. Stopped Crawl of " + au.getName() +
		   " at depth " + (lvlCnt-1));
      crawlStatus.setCrawlError("Site depth exceeded max. crawl depth");
      logger.debug2("urlsToCrawl contains: " + urlsToCrawl);
      logger.info("Site depth = " + (lvlCnt-1));
    } else {
      logger.info("Site depth = "+ (lvlCnt-1));
    }
    
    if (crawlAborted) {
        return aborted();
    }

    if (crawlStatus.getCrawlError() != null) {
      logger.info("Finished crawl (errors) of "+au.getName());
      logger.debug3("Error status = " + crawlStatus.getCrawlError());
    } else {
      logger.info("Finished crawl of "+au.getName());
    }

    if (au instanceof BaseArchivalUnit) {
      BaseArchivalUnit bau = (BaseArchivalUnit)au;
      long cacheHits = bau.getCrawlSpecCacheHits();
      long cacheMisses = bau.getCrawlSpecCacheMisses();
      double per = ((float)cacheHits /
		  ((float)cacheHits + (float)cacheMisses));
      logger.info("Had "+cacheHits+" cache hits, with a percentage of "+ (per*100) );
    }

    return (crawlStatus.getCrawlError() == null);
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
      // it is the real think that do the checking of permission, crawlPermission dwell in CrawlerImpl.java
      int permissionStatus = crawlPermission(permissionPage);
      // if permission status is something other than OK and the abortWhilePermissionOtherThanOk flag is on
       if (permissionStatus != PermissionMap.PERMISSION_OK &&
	  abortWhilePermissionOtherThanOk) {
	logger.info("One or more host(s) of AU do not grant crawling permission - aborting crawl!");
	return false;
      }
      try {
	if (permissionStatus == PermissionMap.PERMISSION_OK) {
	  logger.debug3("Permission granted on host: " + UrlUtil.getHost(permissionPage));
	}
	// set permissionMap
	permissionMap.putStatus(permissionPage,permissionStatus);
      } catch (MalformedURLException e){
	logger.error("The permissionPage's URL is Malformed : "+ permissionPage);
      }
    }
    return true;
  }

  protected boolean aborted() {
    logger.info("Crawl aborted: "+au);
    if (crawlStatus.getCrawlError() == null) {
      crawlStatus.setCrawlError(Crawler.STATUS_INCOMPLETE);
    }
    return false;
  }

  protected boolean withinCrawlWindow() {
    if ((spec!=null) && (!spec.canCrawl())) {
      logger.info("Crawl canceled: outside of crawl window");
      return false;
    }
    return true;
  }

  /** We always want our UrlCacher to store all redirected copies */
  protected UrlCacher makeUrlCacher(CachedUrlSet cus, String url) {
    UrlCacher uc = super.makeUrlCacher(cus, url);
    uc.setRedirectScheme(UrlCacher.REDIRECT_SCHEME_STORE_ALL_IN_SPEC);
    uc.setPermissionMap(permissionMap);
    if (proxyHost != null) {
      uc.setProxy(proxyHost, proxyPort);
    }
    return uc;
  }

  protected boolean fetchAndParse(String url, Collection extractedUrls,
				Set parsedPages, CachedUrlSet cus,
				boolean fetchIfChanged, boolean reparse) {

    String error = null;
    logger.debug3("Dequeued url from list: "+url);

    //makeUrlCacher needed to handle connection pool
    UrlCacher uc = makeUrlCacher(cus, url);

    // don't cache if already cached, unless overwriting
    if (fetchIfChanged || !uc.getCachedUrl().hasContent()) {
      try {
	if (failedUrls.contains(uc.getUrl())) {
	  //skip if it's already failed
	  logger.debug3("Already failed to cache "+uc+". Not retrying.");
	} else {

	  // checking the crawl permission of the url's host
	  if (!checkHostPermission(url,true)){
	    if (crawlStatus.getCrawlError() == null) {
	      crawlStatus.setCrawlError("No permission to collect " + url);
	    }
	    return false;
	  }

	  cacheWithRetries(uc, maxRetries);
	}
      } catch (CacheException e) {
	// Failed.  Don't try this one again during this crawl.
	failedUrls.add(uc.getUrl());
	if (e.isAttributeSet(CacheException.ATTRIBUTE_FAIL)) {
	  logger.error("Problem caching "+uc+". Continuing", e);
	  error = Crawler.STATUS_FETCH_ERROR;
	} else {
	  logger.warning(uc+" not found on publisher's site", e);
	}
      } catch (Exception e) {
	failedUrls.add(uc.getUrl());
	//XXX not expected
	logger.error("Unexpected Exception during crawl, continuing", e);
	error = Crawler.STATUS_FETCH_ERROR;
      }
    } else {
      if (wdog != null) {
	wdog.pokeWDog();
      }
      if (!parsedPages.contains(uc.getUrl())) {
	logger.debug2(uc+" exists, not caching");
      }
      if (!reparse) {
	logger.debug2(uc+" exists, not reparsing");
	parsedPages.add(uc.getUrl());
	return true;
      }
    }
    try {
      if (!parsedPages.contains(uc.getUrl())) {
	logger.debug3("Parsing "+uc);
	CachedUrl cu = uc.getCachedUrl();

	//XXX quick fix; if-statement should be removed when we rework
	//handling of error condition
	if (cu.hasContent()) {
	  ContentParser parser = getContentParser(cu);
	  if (parser != null) {
	    //IOException if the CU can't be read
	    parser.parseForUrls(cu, new MyFoundUrlCallback(parsedPages,
							   extractedUrls, au));
	    if (extractedUrls.remove(url)){
	      logger.debug3("Removing self reference in "+url+" from the extracted list");
	    }
	  }

 	  crawlStatus.signalUrlParsed();
	  parsedPages.add(uc.getUrl());
	}
      }
    } catch (IOException ioe) {
      //XXX handle this better.  Requeue?
      logger.error("Problem parsing "+uc+". Ignoring", ioe);
      error = Crawler.STATUS_FETCH_ERROR;
    }
    logger.debug3("Removing from parsing list: "+uc.getUrl());
    return (error == null);
  }

  private void cacheWithRetries(UrlCacher uc, int maxTries)
      throws IOException {
    int retriesLeft = maxTries;
    logger.debug2("Fetching " + uc.getUrl());
    while (true) {
      try {
	if (wdog != null) {
	  wdog.pokeWDog();
	}
	updateCacheStats(uc.cache());
	return; //cache didn't throw
      } catch (CacheException.RetryableException e) {
	logger.debug("Exception when trying to cache "+uc, e);
	if (--retriesLeft > 0) {
	  long pauseTime =
	    Configuration.getTimeIntervalParam(PARAM_RETRY_PAUSE,
					       DEFAULT_RETRY_PAUSE);
	  Deadline pause = Deadline.in(pauseTime);
	  logger.debug3("Sleeping for " +
			StringUtil.timeIntervalToString(pauseTime));
	  while (!pause.expired()) {
	    try {
	      pause.sleep();
	    } catch (InterruptedException ie) {
	      // no action
	    }
	  }

	  //makeUrlCacher needed to handle connection pool
	  uc = makeUrlCacher(uc.getCachedUrlSet(), uc.getUrl());
	} else {
	  logger.warning("Failed to cache "+ maxTries +" times.  Skipping "
			 + uc);
	  throw e;
	}
      }
    }
  }

  /**
   * Check the permission map to see if we have permission to crawl the given url.
   *
   * @param url the url that we are checking upon.
   * @param permissionFailedRetry true to refetch permission page if last fetch failed
   * @return if the url have permission to be crawled
   */
  protected boolean checkHostPermission(String url ,boolean permissionFailedRetry) {
    int urlPermissionStatus = -1;
    String urlPermissionUrl = "";
    try {
      urlPermissionStatus = permissionMap.getStatus(url);
      urlPermissionUrl = permissionMap.getPermissionUrl(url);
    } catch (MalformedURLException e) {
      logger.error("The url is malformed :" + url);
    }
    boolean printFailedWarning = true;
    switch (urlPermissionStatus) {
        case PermissionMap.PERMISSION_MISSING:
	  logger.warning("No permission page record on host: "+ url);
	  crawlStatus.setCrawlError("No crawl permission page for host of " +
				    url );
	  // abort crawl here
	  return false;
        case PermissionMap.PERMISSION_OK:
	  return true;
	case PermissionMap.PERMISSION_NOT_OK:
	  logger.error("Abort crawl. No permission statement is found at: " +
		       urlPermissionUrl);
	  //abort crawl or skip all the page with this host ?
	  return false;
	case PermissionMap.PERMISSION_UNCHECKED:
	  //should not be in this state as each permissionPage should be checked in the first iteration
	  logger.warning("permission unchecked on host : "+ urlPermissionUrl);
	  // fall through, re-fetch permission like FETCH_PERMISSION_FAILED
	  printFailedWarning = false;
	case PermissionMap.FETCH_PERMISSION_FAILED:
	  if (printFailedWarning) {
	    logger.warning("Fail to fetch permission page on host :" + 
			   urlPermissionUrl);
	  }
	  if (permissionFailedRetry) {
	    //refetch permission page
	    logger.info("refetching permission page: " + urlPermissionUrl);
	    try {
	      permissionMap.putStatus(urlPermissionUrl,
				      crawlPermission(urlPermissionUrl));
	    } catch (MalformedURLException e){
	      logger.error("Malformed urlPermissionUrl", e);
	      return false;
	    }
	    return checkHostPermission(url,false);
	  } else {
	    //abort crawl or skip all the page with this host ?
	    logger.error("Abort crawl. Cannot fetch permission page");
	    return false;
	  }
	default :
	  logger.error("Unknown Permission Status! Something is going wrong!");
	  return false;
    }
  }

  private ContentParser getContentParser(CachedUrl cu) {
    CIProperties props = cu.getProperties();
    ArchivalUnit au = cu.getArchivalUnit();
    if (props != null) {
      String contentType = props.getProperty(CachedUrl.PROPERTY_CONTENT_TYPE);
      return au.getContentParser(contentType);
    }
    return null;
  }

  static class MyFoundUrlCallback
    implements ContentParser.FoundUrlCallback {
    Set parsedPages = null;
    Collection extractedUrls = null;
    ArchivalUnit au = null;

    public MyFoundUrlCallback(Set parsedPages, Collection extractedUrls,
			      ArchivalUnit au) {
      this.parsedPages = parsedPages;
      this.extractedUrls = extractedUrls;
      this.au = au;
    }

    /**
     * Check that we should cache this url and haven't already parsed it
     * @param url the url string, fully qualified (ie, not relative)
     */
    public void foundUrl(String url) {
      if (!isSupportedUrlProtocol(url)) {
	return;
      }
      try {
	String normUrl = UrlUtil.normalizeUrl(url, au);
	if (!parsedPages.contains(normUrl)
	    && !extractedUrls.contains(normUrl)
	    && au.shouldBeCached(normUrl)) {
	  extractedUrls.add(url);
	}
      } catch (MalformedURLException e) {
	logger.warning("Normalizing", e);
      } catch (PluginBehaviorException e) {
	logger.warning("Normalizing", e);
      }
    }
  }
}
