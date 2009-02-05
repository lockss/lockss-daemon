/*
 * $Id: FollowLinkCrawler.java,v 1.73 2009-02-05 05:08:47 tlipkis Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.hasher.*;
import org.lockss.extractor.*;

/**
 * A abstract class that implemented by NewContentCrawler and OaiCrawler
 * it has the follow link mechanism that used by NewContentCrawler
 * and OaiCrawler.
 */
public abstract class FollowLinkCrawler extends BaseCrawler {

  static Logger logger = Logger.getLogger("FollowLinkCrawler");

  public static final String PREFIX = Configuration.PREFIX + "crawler.";

  public static final String PARAM_REPARSE_ALL =
    PREFIX + "reparseAll";
  public static final boolean DEFAULT_REPARSE_ALL = true;

  public static final String PARAM_PARSE_USE_CHARSET =
    PREFIX + "parseUseCharset";
  public static final boolean DEFAULT_PARSE_USE_CHARSET = true;

  public static final String PARAM_PERSIST_CRAWL_LIST =
    PREFIX + "persistCrawlList";
  public static final boolean DEFAULT_PERSIST_CRAWL_LIST = false;

  public static final String PARAM_EXCLUDED_CACHE_SIZE =
    PREFIX + "excludedCacheSize";
  public static final int DEFAULT_EXCLUDED_CACHE_SIZE = 1000;

  public static final String PARAM_REFETCH_DEPTH =
    PREFIX + "refetchDepth.au.<auid>";

  public static final String PARAM_MAX_CRAWL_DEPTH =
    PREFIX + "maxCrawlDepth";
  //testing max. crawl Depth of a site, subject to be changed
  public static final int DEFAULT_MAX_CRAWL_DEPTH = 1000;

  public static final String PARAM_CLEAR_DAMAGE_ON_FETCH =
    PREFIX + "clearDamageOnFetch";
  public static final boolean DEFAULT_CLEAR_DAMAGE_ON_FETCH = true;

  public static final String PARAM_REFETCH_IF_DAMAGED =
    PREFIX + "refetchIfDamaged";
  public static final boolean DEFAULT_REFETCH_IF_DAMAGED = true;

  public static final String PARAM_EXPLODE_ARCHIVES =
    PREFIX + "explodeArchives";
  public static final boolean DEFAULT_EXPLODE_ARCHIVES = true;

  public static final String PARAM_STORE_ARCHIVES =
    PREFIX + "storeArchives";
  public static final boolean DEFAULT_STORE_ARCHIVES = false;

  public static final String PARAM_CRAWL_END_REPORT_EMAIL =
    PREFIX + "crawlEndReportEmail";
  public static final String DEFAULT_CRAWL_END_REPORT_EMAIL = null;

  public static final String PARAM_CRAWL_END_REPORT_HASH_ALG =
    PREFIX + "crawlEndReportHashAlg";
  public static final String DEFAULT_CRAWL_END_REPORT_HASH_ALG = "SHA-1";

  private boolean alwaysReparse = DEFAULT_REPARSE_ALL;
  private boolean usePersistantList = DEFAULT_PERSIST_CRAWL_LIST;
  private boolean parseUseCharset = DEFAULT_PARSE_USE_CHARSET;

  protected int maxDepth = DEFAULT_MAX_CRAWL_DEPTH;

  protected int defaultRetries = DEFAULT_DEFAULT_RETRY_COUNT;
  protected int maxRetries = DEFAULT_MAX_RETRY_COUNT;
  protected long defaultRetryDelay = DEFAULT_DEFAULT_RETRY_DELAY;
  protected long minRetryDelay = DEFAULT_MIN_RETRY_DELAY;
  protected int exploderRetries = DEFAULT_EXPLODER_RETRY_COUNT;

  protected int lvlCnt = 0;
  protected CachedUrlSet cus;
  protected Set parsedPages;
  protected Set extractedUrls;
  protected boolean cachingStartUrls = false; //added to report an error when
                                              //not able to cache a starting Url
  protected BitSet fetchFlags = new BitSet();
  protected String exploderPattern = null;
  protected CrawlSpec crawlSpec = null;
  protected boolean explodeFiles = true;
  protected boolean storeArchive = false;  // XXX need to keep stub archive
  protected String crawlEndReportEmail = DEFAULT_CRAWL_END_REPORT_EMAIL;
  protected String crawlEndReportHashAlg = DEFAULT_CRAWL_END_REPORT_HASH_ALG;

  // Cache recent negative results from au.shouldBeCached().  This is set
  // to an LRUMsp when crawl is initialzed, it's initialized here to a
  // simple map for the sake of test code, which doesn't call
  // this.setCrawlConfig().  If we want to report all excluded URLs, this
  // can be changed to a simple Set.
  private Map excludedUrlCache = new HashMap();
  private Set failedUrls = new HashSet();
  protected Set urlsToCrawl = Collections.EMPTY_SET;

  public FollowLinkCrawler(ArchivalUnit au, CrawlSpec crawlSpec, AuState aus) {
    super(au, crawlSpec, aus);
    exploderPattern = crawlSpec.getExploderPattern();
    this.crawlSpec = crawlSpec;
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


    defaultRetries = config.getInt(PARAM_DEFAULT_RETRY_COUNT,
				   DEFAULT_DEFAULT_RETRY_COUNT);
    maxRetries = config.getInt(PARAM_MAX_RETRY_COUNT,
			       DEFAULT_MAX_RETRY_COUNT);
    defaultRetryDelay = config.getLong(PARAM_DEFAULT_RETRY_DELAY,
				       DEFAULT_DEFAULT_RETRY_DELAY);
    minRetryDelay = config.getLong(PARAM_MIN_RETRY_DELAY,
				   DEFAULT_MIN_RETRY_DELAY);
    exploderRetries = config.getInt(PARAM_EXPLODER_RETRY_COUNT,
				   DEFAULT_EXPLODER_RETRY_COUNT);

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
    parseUseCharset = config.getBoolean(PARAM_PARSE_USE_CHARSET,
					DEFAULT_PARSE_USE_CHARSET);
    explodeFiles = config.getBoolean(PARAM_EXPLODE_ARCHIVES,
					DEFAULT_EXPLODE_ARCHIVES);
    storeArchive = config.getBoolean(PARAM_STORE_ARCHIVES,
					DEFAULT_STORE_ARCHIVES);

    crawlEndReportEmail = config.get(PARAM_CRAWL_END_REPORT_EMAIL,
				     DEFAULT_CRAWL_END_REPORT_EMAIL);
    crawlEndReportHashAlg = config.get(PARAM_CRAWL_END_REPORT_HASH_ALG,
				       DEFAULT_CRAWL_END_REPORT_HASH_ALG);

  }

  protected boolean doCrawl0() {
    if (crawlAborted) {
      return aborted();
    }
    logger.info("Beginning depth " + maxDepth + " crawl " +
		(shouldFollowLink() ? "" : "(no follow) ") +
		"of " + au);
    crawlStatus.signalCrawlStarted();
    crawlStatus.addSource("Publisher");
    cus = au.getAuCachedUrlSet();
    parsedPages = new HashSet();

    //XXX short term hack to work around populatePermissionMap not
    //indicating when a crawl window is the problem
    if (!withinCrawlWindow()) {
      crawlStatus.setCrawlStatus(Crawler.STATUS_WINDOW_CLOSED);
      return aborted();
    }

    if (!populatePermissionMap()) {
      return aborted();
    }

    urlsToCrawl = Collections.EMPTY_SET;

    // get the Urls to follow from either NewContentCrawler or OaiCrawler
    try {
      extractedUrls = getUrlsToFollow();
    } catch (RuntimeException e) {
      logger.warning("Unexpected exception, should have been caught lower", e);
      if (!crawlStatus.isCrawlError()) {
	crawlStatus.setCrawlStatus(Crawler.STATUS_ERROR);
      }
      abortCrawl();
    }

    if (logger.isDebug3()) logger.debug3("Start URLs: " + extractedUrls );
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
      extractedUrls = newSet(); // level (N+1)'s Urls

      while (!urlsToCrawl.isEmpty() && !crawlAborted) {
 	String nextUrl = (String)CollectionUtil.getAnElement(urlsToCrawl);
	logger.debug3("Trying to process " + nextUrl);

	// check crawl window during crawl
	if (!withinCrawlWindow()) {
	  crawlStatus.setCrawlStatus(Crawler.STATUS_WINDOW_CLOSED);
	  return false;
	}
        crawlStatus.removePendingUrl(nextUrl);
	boolean crawlRes = false;
	try {
	  crawlRes = fetchAndParse(nextUrl, extractedUrls,
				   parsedPages, false, alwaysReparse);
	} catch (RuntimeException e) {
	  if (crawlAborted) {
	    logger.debug("Expected exception while aborting crawl: " + e);
	    return aborted();
	  }
	  logger.warning("Unexpected exception processing: " + nextUrl, e);
	  crawlStatus.signalErrorForUrl(nextUrl, e.toString());
	}
	urlsToCrawl.remove(nextUrl);
	if  (!crawlRes) {
	  if (!crawlStatus.isCrawlError()) {
	    crawlStatus.setCrawlStatus(Crawler.STATUS_ERROR);
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
      // If there are still unprocessed URLs, depth exceeds maxDepth
      String msg = "Site depth exceeds max crawl depth (" + maxDepth + ")";
      logger.error(msg + ". Stopped crawl of " + au.getName());
      logger.debug("urlsToCrawl contains: " + urlsToCrawl);
      crawlStatus.setCrawlStatus(Crawler.STATUS_ERROR, msg);
    }
    logger.info("Crawled depth = " + lvlCnt);

    if (crawlAborted) {
      return aborted();
    }

    if (crawlStatus.isCrawlError()) {
      logger.info("Unfinished crawl of " + au.getName() + ", " +
		  crawlStatus.getCrawlErrorMsg());
    } else {
      logger.info("Finished crawl of "+au.getName());
    }

    doCrawlEndActions();
    return (!crawlStatus.isCrawlError());
  }

  // Default Set impl overridden for some tests
  protected Set newSet() {
    return new HashSet();
  }

  /** Separate method for easy overridability in unit tests, where
   * necessary environment may not be set up */
  protected void doCrawlEndActions() {
    sendCrawlEndReport();
    // Cause the content size and disk usage to be calculated in a
    // background thread
    AuUtil.getAuContentSize(au, false);
    AuUtil.getAuDiskUsage(au, false);
  }

  private void sendCrawlEndReport() {
    if (!getDaemon().getPluginManager().isInternalAu(au)
	&& crawlEndReportEmail != null) {
      CrawlEndReport cer = new CrawlEndReport(getDaemon(), au);
      cer.setHashAlgorithm(crawlEndReportHashAlg);
      cer.sendCrawlEndReport(au, crawlEndReportEmail);
    }
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
    if (proxyHost != null) {
      uc.setProxy(proxyHost, proxyPort);
    }

    uc.setFetchFlags(fetchFlags);
    return uc;
  }

  protected boolean fetchAndParse(String url, Collection extractedUrls,
				  Set parsedPages, boolean fetchIfChanged,
				  boolean reparse) {

    logger.debug3("Dequeued url from list: "+url);

    //makeUrlCacher needed to handle connection pool
    UrlCacher uc = makeUrlCacher(url);

    // don't cache if already cached, unless overwriting
    if (fetchIfChanged || !uc.getCachedUrl().hasContent()) {

      try {
	if (failedUrls.contains(uc.getUrl())) {
	  //skip if it's already failed
	  logger.debug3("Already failed to cache "+uc+". Not retrying.");
	} else {

	  // checking the crawl permission of the url's host
	  if (!permissionMap.hasPermission(uc.getUrl())) {
	    if (!crawlStatus.isCrawlError()) {
	      crawlStatus.setCrawlStatus(Crawler.STATUS_NO_PUB_PERMISSION,
					 "No permission to collect " + url);
	    }
	    return false;
	  }

	  if (exploderPattern != null &&
	      RegexpUtil.isMatchRe(uc.getUrl(), exploderPattern)) {
	    // This url matches the pattern for archives to be exploded.
	    explodeArchive(uc);
	  } else {
	    cacheWithRetries(uc);
	  }
	}
      } catch (CacheException.RepositoryException ex) {
	// Failed.  Don't try this one again during this crawl.
	failedUrls.add(uc.getUrl());
	logger.error("Repository error with "+uc, ex);
	crawlStatus.signalErrorForUrl(uc.getUrl(),
				      "Can't store page: " + ex.getMessage());
 	crawlStatus.setCrawlStatus(Crawler.STATUS_REPO_ERR);
      } catch (CacheException.RedirectOutsideCrawlSpecException ex) {
	// Count this as an excluded URL
	crawlStatus.signalUrlExcluded(uc.getUrl());
	// and claim success, because false causes crawl to fail
	return true;
      } catch (CacheException ex) {
	// Failed.  Don't try this one again during this crawl.
	failedUrls.add(uc.getUrl());
	crawlStatus.signalErrorForUrl(uc.getUrl(), ex.getMessage());
	if (ex.isAttributeSet(CacheException.ATTRIBUTE_FAIL)) {
	  logger.siteError("Problem caching "+uc+". Continuing", ex);
	  crawlStatus.setCrawlStatus(Crawler.STATUS_FETCH_ERROR);
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
	  crawlStatus.setCrawlStatus(Crawler.STATUS_FETCH_ERROR);
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

    // don't parse if not following links
    if (!shouldFollowLink()) {
      return (!crawlStatus.isCrawlError());
    }

    // parse the page
    try {
      if (!parsedPages.contains(uc.getUrl())) {
	logger.debug3("Parsing "+uc);
	CachedUrl cu = uc.getCachedUrl();
	try {
	  //XXX quick fix; if-statement should be removed when we rework
	  //handling of error condition
	  if (cu.hasContent()) {
	    LinkExtractor extractor = getLinkExtractor(cu);
	    if (extractor != null) {
	      //IOException if the CU can't be read
	      InputStream in = null;
	      try {
		in = cu.getUnfilteredInputStream();
		extractor.extractUrls(au, in,
				      getCharset(cu),
				      PluginUtil.getBaseUrl(cu),
				      new MyLinkExtractorCallback(parsedPages,
								  extractedUrls,
								  au));
		if (extractedUrls.remove(url)){
		  crawlStatus.removePendingUrl(url);
		  logger.debug3("Removing self reference in " + url +
				" from the extracted list");
		}
		crawlStatus.signalUrlParsed(uc.getUrl());
	      } catch (PluginException e) {
		logger.error("Plugin LinkExtractor error", e);
		crawlStatus.signalErrorForUrl(uc.getUrl(),
					      "Plugin LinkExtractor error: " +
					      e.getMessage());
		crawlStatus.setCrawlStatus(Crawler.STATUS_PLUGIN_ERROR);
	      } finally {
		IOUtil.safeClose(in);
	      }
	    }
	    parsedPages.add(uc.getUrl());
	  }
	} finally {
	  cu.release();
	}
      }
    } catch (CacheException ex) {
      crawlStatus.signalErrorForUrl(uc.getUrl(), ex.getMessage());
      if (ex.isAttributeSet(CacheException.ATTRIBUTE_FATAL)) {
	logger.error("Fatal error parsing "+uc, ex);
	crawlAborted = true;
	return false;
      } else if (ex.isAttributeSet(CacheException.ATTRIBUTE_FAIL)) {
	logger.siteError("Couldn't parse "+uc+". continuing", ex);
	crawlStatus.setCrawlStatus(Crawler.STATUS_EXTRACTOR_ERROR);
      } else {
	logger.siteWarning("Couldn't parse "+uc+". ignoring error", ex);
      }
    } catch (IOException ioe) {
      crawlStatus.signalErrorForUrl(uc.getUrl(), ioe.getMessage());
      logger.error("Problem parsing "+uc+". Ignoring", ioe);
      crawlStatus.setCrawlStatus(Crawler.STATUS_FETCH_ERROR);
    }
    logger.debug3("Removing from parsing list: "+uc.getUrl());
    return (!crawlStatus.isCrawlError());
  }

  private String getCharset(CachedUrl cu) {
    String res = null;
    if (parseUseCharset) {
      res = HeaderUtil.getCharsetFromContentType(cu.getContentType());
    }
    if (res == null) {
      res = Constants.DEFAULT_ENCODING;
    }
    return res;
  }

  protected void cacheWithRetries(UrlCacher uc)
      throws IOException {
    int retriesLeft = -1;
    int totalRetries = -1;
    logger.debug2("Fetching " + uc.getUrl());
    while (true) {
      try {
	if (wdog != null) {
	  wdog.pokeWDog();
	}
	updateCacheStats(uc.cache(), uc);
	// success
	return;
      } catch (CacheException e) {
	if (!e.isAttributeSet(CacheException.ATTRIBUTE_RETRY)) {
	  throw e;
	}
	if (retriesLeft < 0) {
	  retriesLeft = getRetryCount(e);
	  totalRetries = retriesLeft;
	}
	if (logger.isDebug2()) {
	  logger.debug("Retryable (" + retriesLeft + ") exception caching "
		       + uc, e);
	} else {
	  logger.debug("Retryable (" + retriesLeft + ") exception caching "
		       + uc + ": " + e.toString());
	}
	if (--retriesLeft > 0) {
	  long delayTime = getRetryDelay(e);
	  Deadline wait = Deadline.in(delayTime);
	  logger.debug3("Waiting " +
			StringUtil.timeIntervalToString(delayTime) +
			" before retry");
	  while (!wait.expired()) {
	    try {
	      wait.sleep();
	    } catch (InterruptedException ie) {
	      // no action
	    }
	  }
	  uc = makeUrlCacher(uc.getUrl());

	} else {
	  if (cachingStartUrls) { //if cannot fetch anyone of StartUrls
	    logger.error("Failed to cache (" + totalRetries + ") start url: "
			 + uc.getUrl());
	    crawlStatus.setCrawlStatus(Crawler.STATUS_ERROR,
				       "Failed to cache start url: "+
				       uc.getUrl() );
	  } else {
	    logger.warning("Failed to cache (" + totalRetries + "), skipping: "
			 + uc.getUrl());
	  }
	  throw e;
	}
      }
    }
  }

  int getRetryCount(CacheException e) {
    int res = e.getRetryCount();
    if (res < 0) {
      res = defaultRetries;
    }
    return Math.min(res, maxRetries);
  }

  long getRetryDelay(CacheException e) {
    long delay = e.getRetryDelay();
    if (delay < 0) {
      delay = defaultRetryDelay;
    }
    return Math.max(delay, minRetryDelay);
  }

  protected Exploder getExploder(UrlCacher uc) {
    Exploder ret = null;
    String url = uc.getUrl();
    if (url.endsWith(".arc.gz")) {
      ret = new ArcExploder(uc, exploderRetries, crawlSpec, this,
			    explodeFiles, storeArchive);
    } else if (url.endsWith(".zip")) {
      ret = new ZipExploder(uc, exploderRetries, crawlSpec, this,
			    explodeFiles, storeArchive);
    } else if (url.endsWith(".tar")) {
      ret = new TarExploder(uc, exploderRetries, crawlSpec, this,
			    explodeFiles, storeArchive);
    }
    return ret;
  }

  protected void explodeArchive(UrlCacher uc)
      throws IOException {
    Exploder exploder = getExploder(uc);
    if (exploder != null) {
      exploder.explodeUrl();
    } else {
      logger.warning("No exploder for " + uc.getUrl());
      cacheWithRetries(uc);
    }
  }

  protected boolean isGloballyExcludedUrl(String url) {
    if (crawlMgr != null) {
      if (crawlMgr.isGloballyExcludedUrl(au, url)) {
	crawlStatus.signalErrorForUrl(url, "Excluded (probable recursion)");
	return true;
      }
    }
    return false;
  }

  private LinkExtractor getLinkExtractor(CachedUrl cu) {
    ArchivalUnit au = cu.getArchivalUnit();
    return au.getLinkExtractor(cu.getContentType());
  }

  class MyLinkExtractorCallback implements LinkExtractor.Callback {
    Set parsedPages;
    Collection extractedUrls;
    ArchivalUnit au;

    public MyLinkExtractorCallback(Set parsedPages, Collection extractedUrls,
				   ArchivalUnit au) {
      this.parsedPages = parsedPages;
      this.extractedUrls = extractedUrls;
      this.au = au;
    }

    /**
     * Check that we should cache this url and haven't already parsed it
     * @param url the url string, fully qualified (ie, not relative)
     */
    public void foundLink(String url) {
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
 	    if (isGloballyExcludedUrl(normUrl)) {
	      if (logger.isDebug2()) {
		logger.debug2("Globally excluded url: "+normUrl);
	      }
	    } else {
	      if (logger.isDebug2()) {
		logger.debug2("Included url: "+normUrl);
	      }
	      extractedUrls.add(normUrl);
	      crawlStatus.addPendingUrl(normUrl);
	    }
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
