/*
 * $Id: FollowLinkCrawler.java,v 1.42 2006-09-22 06:23:02 tlipkis Exp $
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
import java.net.*;
import java.io.*;
import org.apache.commons.collections.map.LRUMap;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.state.*;

/**
 * A abstract class that implemented by NewContentCrawler and OaiCrawler
 * it has the follow link mechanism that used by NewContentCrawler
 * and OaiCrawler.
 */
public abstract class FollowLinkCrawler extends BaseCrawler {

  static Logger logger = Logger.getLogger("FollowLinkCrawler");

  // Cache recent negative results from au.shouldBeCached().  This is set
  // to an LRUMsp when crawl is initialzed, it's initialized here to a
  // simple map for the sake of test code, which doesn't call
  // this.setCrawlConfig().  If we want to report all excluded URLs, this
  // can be changed to a simple Set.
  private Map excludedUrlCache = new HashMap();
  private Set failedUrls = new HashSet();
  private Set urlsToCrawl = Collections.EMPTY_SET;

  private static final String PARAM_RETRY_TIMES =
    Configuration.PREFIX + "BaseCrawler.numCacheRetries";
  private static final int DEFAULT_RETRY_TIMES = 3;

  public static final String PARAM_RETRY_PAUSE =
    Configuration.PREFIX + "BaseCrawler.retryPause";
  public static final long DEFAULT_RETRY_PAUSE = 10*Constants.SECOND;

  public static final String PARAM_REPARSE_ALL =
    Configuration.PREFIX + "BaseCrawler.reparse_all";
  public static final boolean DEFAULT_REPARSE_ALL = true;

  public static final String PARAM_PERSIST_CRAWL_LIST =
    Configuration.PREFIX + "BaseCrawler.persist_crawl_list";
  public static final boolean DEFAULT_PERSIST_CRAWL_LIST = false;

  public static final String PARAM_EXCLUDED_CACHE_SIZE =
    Configuration.PREFIX + "BaseCrawler.excluded_cache_size";
  public static final int DEFAULT_EXCLUDED_CACHE_SIZE = 1000;

  public static final String PARAM_REFETCH_DEPTH =
    Configuration.PREFIX + "crawler.refetchDepth.au.<auid>";

  public static final String PARAM_MAX_CRAWL_DEPTH =
    Configuration.PREFIX + "BaseCrawler.maxCrawlDepth";
  //testing max. crawl Depth of a site, subject to be changed
  public static final int DEFAULT_MAX_CRAWL_DEPTH = 1000;

  public static final String PARAM_CLEAR_DAMAGE_ON_FETCH =
    Configuration.PREFIX + "BaseCrawler.clearDamageOnFetch";
  public static final boolean DEFAULT_CLEAR_DAMAGE_ON_FETCH = true;

  public static final String PARAM_REFETCH_IF_DAMAGED =
    Configuration.PREFIX + "BaseCrawler.refetchIfDamaged";
  public static final boolean DEFAULT_REFETCH_IF_DAMAGED = true;

  private boolean alwaysReparse = DEFAULT_REPARSE_ALL;
  private boolean usePersistantList = DEFAULT_PERSIST_CRAWL_LIST;
  protected int maxDepth = DEFAULT_MAX_CRAWL_DEPTH;
  private int maxRetries = DEFAULT_RETRY_TIMES;
  protected int lvlCnt = 0;
  protected CachedUrlSet cus;
  protected Set parsedPages;
  protected Set extractedUrls;
  protected boolean cachingStartUrls = false; //added to report an error when
                                              //not able to cache a starting Url

  protected BitSet fetchFlags = new BitSet();


  public FollowLinkCrawler(ArchivalUnit au, CrawlSpec crawlSpec, AuState aus) {
    super(au, crawlSpec, aus);
  }

  /**
   * This method is implemented in NewContentCrawler and OaiCrawler.
   * It gives the different crawlers to have different mechanism to collect
   * those "initial" urls of a crawl. The method will also fetch those
   * "initial" urls into the cache.
   *
   * @return a set of urls to crawl for updated contents
   */
  protected abstract Set getUrlsToFollow();

  protected abstract boolean shouldFollowLink();

  protected void setCrawlConfig(Configuration config) {
    super.setCrawlConfig(config);
    alwaysReparse = config.getBoolean(PARAM_REPARSE_ALL, DEFAULT_REPARSE_ALL);
    usePersistantList = config.getBoolean(PARAM_PERSIST_CRAWL_LIST,
					  DEFAULT_PERSIST_CRAWL_LIST);
    maxDepth = config.getInt(PARAM_MAX_CRAWL_DEPTH, DEFAULT_MAX_CRAWL_DEPTH);
    maxRetries = config.getInt(PARAM_RETRY_TIMES, DEFAULT_RETRY_TIMES);

    excludedUrlCache =
      new LRUMap(config.getInt(PARAM_EXCLUDED_CACHE_SIZE,
			       DEFAULT_EXCLUDED_CACHE_SIZE));


    fetchFlags = new BitSet();
    if (config.getBoolean(PARAM_CLEAR_DAMAGE_ON_FETCH,
 			  DEFAULT_CLEAR_DAMAGE_ON_FETCH)) {
      fetchFlags.set(UrlCacher.CLEAR_DAMAGE_FLAG);
    }
    if (config.getBoolean(PARAM_REFETCH_IF_DAMAGED,
 			  DEFAULT_REFETCH_IF_DAMAGED)) {
      fetchFlags.set(UrlCacher.REFETCH_IF_DAMAGE_FLAG);
    }
  }

  /**
   * One can update the Max. Crawl Depth before calling doCrawl10().
   * Currently used only for "Not Follow Link" mode in OaiCrawler
   *
   * XXX  This method should go away after serious testing of the new
   * implemenated getUrlsTOFollow() in OaiCrawler
   *
   * @param newMax the new max. crawl depth
   */
  protected void setMaxDepth(int newMax){
    logger.debug3("changing max crawl depth from " + maxDepth + " to " + newMax);
    maxDepth = newMax;
  }


  protected boolean doCrawl0() {
    if (crawlAborted) {
      return aborted();
    }
    logger.info("Beginning depth " + maxDepth + " crawl of " + au);
    crawlStatus.signalCrawlStarted();
    crawlStatus.addSource("Publisher");
    cus = au.getAuCachedUrlSet();
    parsedPages = new HashSet();

    //XXX short term hack to work around populatePermissionMap not 
    //indicating when a crawl window is the problem
    if (!withinCrawlWindow()) {
      crawlStatus.setCrawlError(Crawler.STATUS_WINDOW_CLOSED);
      abortCrawl();
    } 

    if (!populatePermissionMap()) {
      return aborted();
    }

    urlsToCrawl = Collections.EMPTY_SET;

    // get the Urls to follow from either NewContentCrawler or OaiCrawler
    extractedUrls = getUrlsToFollow();
    logger.debug3("Urls extracted from getUrlsToFollow() : "
		  + extractedUrls.toString() );

    if (crawlAborted) {
        return aborted();
    }

    if (usePersistantList) {
      urlsToCrawl = aus.getCrawlUrls();
      urlsToCrawl.addAll(extractedUrls);
      extractedUrls.clear();
    } else {
      urlsToCrawl = extractedUrls;
    }

    while (lvlCnt <= maxDepth && !urlsToCrawl.isEmpty() && !crawlAborted) {

      logger.debug2("Crawling at level " + lvlCnt);
      extractedUrls = new HashSet(); // level (N+1)'s Urls

      while (!urlsToCrawl.isEmpty() && !crawlAborted) {
 	String nextUrl = (String)CollectionUtil.getAnElement(urlsToCrawl);

	logger.debug3("Trying to process " + nextUrl);

	// check crawl window during crawl
	if (!withinCrawlWindow()) {
	  crawlStatus.setCrawlError(Crawler.STATUS_WINDOW_CLOSED);
	  return false;
	}
	boolean crawlRes = false;
	try {
	  crawlRes = fetchAndParse(nextUrl, extractedUrls,
				   parsedPages, false, alwaysReparse);
	} catch (RuntimeException e) {
	  if (crawlAborted) {
	    logger.debug("Expected exception while aborting crawl: " + e);
	    return aborted();
	  }
	  logger.warning("Unexpected exception in crawl", e);
	}
	urlsToCrawl.remove(nextUrl);
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

    if (!urlsToCrawl.isEmpty() && !crawlAborted) {
      //when there are more Url to crawl in  new content crawl or follow link moded oai crawl
      logger.error("Site depth exceeds max. crawl depth. Stopped Crawl of " +
		   au.getName() + " at depth " + lvlCnt);
      crawlStatus.setCrawlError("Site depth exceeded max. crawl depth");
      logger.debug("urlsToCrawl contains: " + urlsToCrawl);
    }
    logger.info("Crawled depth = " + lvlCnt);

    if (crawlAborted) {
      return aborted();
    }

    if (crawlStatus.getCrawlError() != null) {
      logger.info("Finished crawl (errors) of "+au.getName());
      logger.debug2("Error status = " + crawlStatus.getCrawlError());
    } else {
      logger.info("Finished crawl of "+au.getName());
    }

    doCrawlEndActions();
    return (crawlStatus.getCrawlError() == null);
  }

  /** Separate method for easy overridability in unit tests, where
   * necessary environment may not be set up */
  protected void doCrawlEndActions() {
    // Recompute the content tree size.  This can take a while, so do it
    // now in background (crawl) thread since it's likely to be necessary, to
    // make it more likely to be already computed when accessed from the UI.
    AuUtil.getAuContentSize(au);
    AuUtil.getAuDiskUsage(au);
  }

  protected boolean withinCrawlWindow() {
    if ((spec!=null) && (!spec.inCrawlWindow())) {
      logger.info("Crawl canceled: outside of crawl window");
      return false;
    }
    return true;
  }

  /** We always want our UrlCacher to store all redirected copies */
  public UrlCacher makeUrlCacher(String url) {
    UrlCacher uc = super.makeUrlCacher(url);
    uc.setRedirectScheme(UrlCacher.REDIRECT_SCHEME_STORE_ALL_IN_SPEC);
    uc.setPermissionMapSource(this);
    if (proxyHost != null) {
      uc.setProxy(proxyHost, proxyPort);
    }

    uc.setFetchFlags(fetchFlags);
    return uc;
  }

  protected boolean fetchAndParse(String url, Collection extractedUrls,
				  Set parsedPages, boolean fetchIfChanged,
				  boolean reparse) {

    String error = null;
    logger.debug3("Dequeued url from list: "+url);

    //makeUrlCacher needed to handle connection pool
    UrlCacher uc = makeUrlCacher(url);

    // don't cache if already cached, unless overwriting
    if (fetchIfChanged || !uc.getCachedUrl().hasContent()) {

//       if (!fetch(uc, error)){
// 	return false;
//       }
      try {
	if (failedUrls.contains(uc.getUrl())) {
	  //skip if it's already failed
	  logger.debug3("Already failed to cache "+uc+". Not retrying.");
	} else {

	  // checking the crawl permission of the url's host
	  if (!permissionMap.hasPermission(uc.getUrl())) {
	    if (crawlStatus.getCrawlError() == null) {
	      crawlStatus.setCrawlError("No permission to collect " + url);
	    }
	    return false;
	  }

	  cacheWithRetries(uc, maxRetries);
	}
      } catch (CacheException.RepositoryException ex) {
	// Failed.  Don't try this one again during this crawl.
	failedUrls.add(uc.getUrl());
	logger.error("Repository error with "+uc, ex);
	crawlStatus.signalErrorForUrl(uc.getUrl(),
				      "Can't store page: " + ex.getMessage());
 	error = Crawler.STATUS_REPO_ERR;
      } catch (CacheException ex) {
	// Failed.  Don't try this one again during this crawl.
	failedUrls.add(uc.getUrl());
	crawlStatus.signalErrorForUrl(uc.getUrl(), ex.getMessage());
	if (ex.isAttributeSet(CacheException.ATTRIBUTE_FAIL)) {
	  logger.error("Problem caching "+uc+". Continuing", ex);
	  error = Crawler.STATUS_FETCH_ERROR;
	} else {
	  logger.warning(uc+" not found on publisher's site", ex);
	}
	if (ex.isAttributeSet(CacheException.ATTRIBUTE_FATAL)) {
	  logger.error("Found a fatal error with "+uc+". Aborting crawl");
	  crawlAborted = true;
	  return false;
	}
      } catch (Exception ex) {
	if (crawlAborted) {
	  logger.debug("Expected exception while aborting crawl: " + ex);
	} else {
	  failedUrls.add(uc.getUrl());
	  crawlStatus.signalErrorForUrl(uc.getUrl(), ex.toString());
	  //XXX not expected
	  logger.error("Unexpected Exception during crawl, continuing", ex);
	  error = Crawler.STATUS_FETCH_ERROR;
	}
      }
    } else {
      if (wdog != null) {
	wdog.pokeWDog();
      }
      if (!reparse) {
	logger.debug2(uc+" exists, not reparsing");
	parsedPages.add(uc.getUrl());
	return true;
      }
    }

    // parse the page
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
	    parser.parseForUrls(cu.openForReading(),
				PluginUtil.getBaseUrl(cu),
				new MyFoundUrlCallback(parsedPages,
						       extractedUrls, au));
	    if (extractedUrls.remove(url)){
	      logger.debug3("Removing self reference in "+url+" from the extracted list");
	    }
	    crawlStatus.signalUrlParsed(uc.getUrl());
	  }
	  parsedPages.add(uc.getUrl());
	}
	cu.release();
      }
    } catch (IOException ioe) {
      //XXX handle this better.  Requeue?
      logger.error("Problem parsing "+uc+". Ignoring", ioe);
      error = Crawler.STATUS_FETCH_ERROR;
    }
    logger.debug3("Removing from parsing list: "+uc.getUrl());
    return (error == null);
  }

//   protected boolean fetch(UrlCacher uc, String error){
//     try {
//       if (failedUrls.contains(uc.getUrl())) {
// 	//skip if it's already failed
// 	logger.debug3("Already failed to cache "+uc+". Not retrying.");
//       } else {

// 	// checking the crawl permission of the url's host
// 	if (!hasPermission(uc.getUrl())){
// 	  if (crawlStatus.getCrawlError() == null) {
// 	    crawlStatus.setCrawlError("No permission to collect " + uc.getUrl());
// 	  }
// 	  return false;
// 	}
// 	cacheWithRetries(uc, maxRetries);
//       }
//     } catch (CacheException e) {
//       // Failed.  Don't try this one again during this crawl.
//       failedUrls.add(uc.getUrl());
//       if (e.isAttributeSet(CacheException.ATTRIBUTE_FAIL)) {
// 	logger.error("Problem caching "+uc+". Continuing", e);
// 	  error = Crawler.STATUS_FETCH_ERROR;
//       } else {
// 	logger.warning(uc+" not found on publisher's site", e);
//       }
//     } catch (Exception e) {
// 	failedUrls.add(uc.getUrl());
// 	//XXX not expected
// 	logger.error("Unexpected Exception during crawl, continuing", e);
// 	error = Crawler.STATUS_FETCH_ERROR;
//     }
//     return true;
//   }

  private void cacheWithRetries(UrlCacher uc, int maxTries)
      throws IOException {
    int retriesLeft = maxTries;
    logger.debug2("Fetching " + uc.getUrl());
    while (true) {
      try {
	if (wdog != null) {
	  wdog.pokeWDog();
	}
	updateCacheStats(uc.cache(), uc);
	return; //cache didn't throw
      } catch (CacheException.RetryableException e) {
	logger.debug("Exception when trying to cache "+uc, e);
	if (--retriesLeft > 0) {
	  long pauseTime =
            CurrentConfig.getTimeIntervalParam(PARAM_RETRY_PAUSE,
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
	  uc = makeUrlCacher(uc.getUrl());
	} else {
	  if (cachingStartUrls) { //if cannot fetch anyone of StartUrls
	    logger.error("Failed to cache " + maxTries +" times on start url " +
			 uc.getUrl() + " .Skipping it.");
	    crawlStatus.setCrawlError("Fail to cache start url: "+ uc.getUrl() );
	  } else {
	    logger.warning("Failed to cache "+ maxTries +" times.  Skipping "
			   + uc);
	  }
	  throw e;
	}
      }
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

  class MyFoundUrlCallback
    implements ContentParser.FoundUrlCallback {
    Set parsedPages;
    Collection extractedUrls;
    ArchivalUnit au;

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
	if (logger.isDebug3()) {
	  logger.debug3("Found "+url);
	  logger.debug3("Normalized to "+normUrl);
	}
	// au.shouldBeCached() is expensive, don't call it if we already
	// know the answer
	if (!parsedPages.contains(normUrl)
 	    && !extractedUrls.contains(normUrl)
 	    && !urlsToCrawl.contains(normUrl)
	    && !excludedUrlCache.containsKey(normUrl)
	    && !failedUrls.contains(normUrl)) {
	  if (au.shouldBeCached(normUrl)) {
	    if (logger.isDebug2()) {
	      logger.debug2("Included url: "+normUrl);
	    }
	    extractedUrls.add(normUrl);
	  } else {
	    if (logger.isDebug2()) {
	      logger.debug2("Excluded url: "+normUrl);
	    }
	    crawlStatus.signalUrlExcluded(normUrl);
	    excludedUrlCache.put(normUrl, "");
	  }
	} else if (logger.isDebug3()) {
	  if (extractedUrls.contains(normUrl)) {
	    logger.debug3("Already extracted url: " + normUrl);
	  }
	  if (parsedPages.contains(normUrl)) {
	    logger.debug3("Already processed url: " + normUrl);
	  }
	  if (failedUrls.contains(normUrl)) {
	    logger.debug3("Already failed url: " + normUrl);
	  }
	}
      } catch (MalformedURLException e) {
	//XXX what exactly does this log want to tell?
	logger.warning("Normalizing", e);
      } catch (PluginBehaviorException e) {
	logger.warning("Normalizing", e);
      }
    }
  }
}
