/*
 * $Id: NewContentCrawler.java,v 1.27 2004-07-21 07:04:39 tlipkis Exp $
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
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.state.*;

public class NewContentCrawler extends CrawlerImpl {

  private static Logger logger = Logger.getLogger("NewContentCrawler");
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

  protected static HashMap permissionMap = new HashMap();
  
  private boolean alwaysReparse;
  private boolean usePersistantList;

  public NewContentCrawler(ArchivalUnit au, CrawlSpec spec, AuState aus) {
    super(au, spec, aus);
    crawlStatus = new Crawler.Status(au, spec.getStartingUrls(), getType());
  }

  public int getType() {
    return Crawler.NEW_CONTENT;
  }

  protected boolean doCrawl0() {
    if (crawlAborted) {
      return aborted();
    }
    alwaysReparse =
      Configuration.getBooleanParam(PARAM_REPARSE_ALL, DEFAULT_REPARSE_ALL);
    usePersistantList =
      Configuration.getBooleanParam(PARAM_PERSIST_CRAWL_LIST,
				    DEFAULT_PERSIST_CRAWL_LIST);

    int maxDepth = Configuration.getIntParam(PARAM_MAX_CRAWL_DEPTH,
					     DEFAULT_MAX_CRAWL_DEPTH);

    logger.info("Max. crawl depth is set to be " + maxDepth); 

    logger.info("Beginning crawl of "+au);
    crawlStatus.signalCrawlStarted();
    CachedUrlSet cus = au.getAuCachedUrlSet();
    Set parsedPages = new HashSet();

    Set extractedUrls = new HashSet();

    // get the permission list from AU
    List permissionList = au.getPermissionPages();
    if (permissionList == null || permissionList.size() == 0){
      logger.error("au.getPermissionPages() return null list or nothing in the list!");
      return aborted();
    }
    
    if (!checkPermissionList(permissionList)){
      return aborted();
    }

    int refetchDepth0 = spec.getRefetchDepth();
    String key = StringUtil.replaceString(PARAM_REFETCH_DEPTH,
					  "<auid>", au.getAuId());
    int refetchDepth = Configuration.getIntParam(key, refetchDepth0);
    if (refetchDepth != refetchDepth0) {
      logger.info("Crawl spec refetch depth (" + refetchDepth0 +
		  ") overridden by parameter (" + refetchDepth + ")");
    }

    //maxDepth should be greater than refetchDepth
    if (refetchDepth > maxDepth){ //it should not happen
      logger.error("Max. depth is set smaller than refetchDepth." +
		   " Abort Crawl of " + au);
      crawlStatus.setCrawlError("Max. Crawl depth too small");
      return aborted();
    }

    Iterator it = spec.getStartingUrls().iterator(); //getStartingUrls();
    for (int ix=0; ix<refetchDepth; ix++) {

      //don't use clear() or it will empty the iterator
      extractedUrls = new HashSet();

      while (it.hasNext() && !crawlAborted) {
	String url = (String)it.next();
	//catch and warn if there's a url in the start urls
	//that we shouldn't cache
	logger.debug3("Trying to process " +url);
	
        // check crawl window during crawl
	if (!withinCrawlWindow()) {
	  crawlStatus.setCrawlError(Crawler.STATUS_WINDOW_CLOSED);
	  return false;
	}

	if (parsedPages.contains(url)) {
	  continue;
	}

 	if (spec.isIncluded(url)) {
	  if (!fetchAndParse(url, extractedUrls, parsedPages,
			     cus, true, true)) {
	    if (crawlStatus.getCrawlError() == null) {
	      crawlStatus.setCrawlError(Crawler.STATUS_ERROR);
	    }
	  }
	} else if (ix == 0) {
	  logger.warning("Called with a starting url we aren't suppose to "+
			 "cache: "+url);
	}
      }
      it = extractedUrls.iterator();
    }

    //we don't alter the crawl list from AuState until we've enumerated the
    //urls that need to be recrawled.
    Collection urlsToCrawl; // Level (N)'s Urls

    if (usePersistantList) {
      urlsToCrawl = aus.getCrawlUrls();
      urlsToCrawl.addAll(extractedUrls);
      extractedUrls.clear();
    } else {
      urlsToCrawl = extractedUrls; 
    }

    int lvlCnt = refetchDepth; // count for what level (N) 
                               // from the root we are at

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
  
  private boolean checkPermissionList(List permissionList) {
    boolean abortWhilePermissionOtherThanOk = 
      Configuration.getBooleanParam(PARAM_ABORT_WHILE_PERMISSION_OTHER_THAN_OK, 
				    DEFAULT_ABORT_WHILE_PERMISSION_OTHER_THAN_OK);

    logger.info("Checking permission on host(s) of " + au);
    Iterator permissionUrls = permissionList.iterator();
    // for each url from the permissionUrls
    // check if the permission string is in the content of the url
    while (permissionUrls.hasNext()) {
      String permissionPage = (String)permissionUrls.next();
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
	permissionMap.put(UrlUtil.getHost(permissionPage).toLowerCase(),
			  new PermissionRecord(permissionPage,permissionStatus));
      } catch (MalformedURLException e){
	logger.error("The permissionPage's URL is Malformed : "+ permissionPage);
      }
    }
    return true;
  }

  private boolean aborted() {
    logger.info("Crawl aborted: "+au);
    if (crawlStatus.getCrawlError() == null) {
      crawlStatus.setCrawlError(Crawler.STATUS_INCOMPLETE);
    }
    return false;
  }

  private boolean withinCrawlWindow() {
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
	  if (!checkHostPermissionRecord(url)){
	    return false;
	  }
	  
	  cacheWithRetries(uc, Configuration.getIntParam(PARAM_RETRY_TIMES,
							 DEFAULT_RETRY_TIMES));
	  numUrlsFetched++;
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
	uc.cache(); //IOException if there is a caching problem

	crawlStatus.signalUrlFetched();
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

  //check if the url's host granted a permission
  private boolean checkHostPermissionRecord(String url){
  	PermissionRecord urlRecord = null;
	try {
	  urlRecord = (PermissionRecord) permissionMap.get(UrlUtil.getHost(url).toLowerCase());
	} catch (MalformedURLException e) {
	  logger.error("The url is malformed :" + url);
	} 
	if (urlRecord == null) {
	  logger.warning("No permission page record on host of "+ url);
	  crawlStatus.setCrawlError("No crawl permission page for host of " +
				    url );
	  // abort crawl here
	  return false;
	}
	// check if permission to crawl from the url's host is granted
	if (!permissionGrantedBeforeFetch(urlRecord,true)){
	  crawlStatus.setCrawlError("Crawl permission not found at "+
				    urlRecord.getPermissionUrl() );
	  return false;
	}
	return true;
  }

  //the policy of handling different permission status
  private boolean permissionGrantedBeforeFetch(PermissionRecord urlRecord ,boolean permissionFailedRetry){
    int urlPermissionStatus = urlRecord.getPermissionStatus();
    boolean printFailedWarning = true;
    switch (urlPermissionStatus) {
	case PermissionRecord.PERMISSION_OK:
	  return true;
	case PermissionRecord.PERMISSION_NOT_OK:
	  logger.error("Abort crawl. No permission statement is found on host : " + urlRecord.getPermissionUrl());
	  //abort crawl or skip all the page with this host ?
	  return false;
	case PermissionRecord.PERMISSION_UNCHECKED:
	  //should not be in this state as each permissionPage should be checked in the first iteration
	  logger.warning("permission unchecked on host : "+ urlRecord.getPermissionUrl()); 
	  // fall through, re-fetch permission like FETCH_PERMISSION_FAILED
	  printFailedWarning = false;
	case PermissionRecord.FETCH_PERMISSION_FAILED:
	  if (printFailedWarning) {
	    logger.warning("Fail to fetch permission page on host :" + urlRecord.getPermissionUrl());
	  }
	  if (permissionFailedRetry) {
	    //refetch permission page
	    logger.info("refetching permission page on host:" + urlRecord.getPermissionUrl());
	    urlRecord.setPermissionStatus(crawlPermission(urlRecord.getPermissionUrl()));
	    return permissionGrantedBeforeFetch(urlRecord,false);
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

  private static class MyFoundUrlCallback
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
      if (isSupportedUrlProtocol(url) && !parsedPages.contains(url)
	  && !extractedUrls.contains(url) && au.shouldBeCached(url)) {
	extractedUrls.add(url);
      }
    }
  }
}
