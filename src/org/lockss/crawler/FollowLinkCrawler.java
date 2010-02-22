/*
 * $Id: FollowLinkCrawler.java,v 1.77.6.1 2010-02-22 06:41:12 tlipkis Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.filter.*;
import org.lockss.extractor.*;
import org.lockss.alert.Alert;

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

  protected int hiDepth = 0;		// maximum depth seen
  protected int fqMaxLen = 0;		// maximum length of fetch queue
  protected int fqSumLen = 0;		// sum of  fetch queue length samples
  protected int fqSamples = 0;		// number of fetch queue len samples

  protected int defaultRetries = DEFAULT_DEFAULT_RETRY_COUNT;
  protected int maxRetries = DEFAULT_MAX_RETRY_COUNT;
  protected long defaultRetryDelay = DEFAULT_DEFAULT_RETRY_DELAY;
  protected long minRetryDelay = DEFAULT_MIN_RETRY_DELAY;
  protected int exploderRetries = DEFAULT_EXPLODER_RETRY_COUNT;

  protected CachedUrlSet cus;
  protected Map<String,CrawlUrlData> processedUrls;
  protected Map<String,CrawlUrlData> maxDepthUrls;
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
  protected CrawlQueue fetchQueue;
  protected Comparator<CrawlUrl> urlOrderComparator;

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
  protected abstract Collection<String> getUrlsToFollow();

  protected abstract boolean shouldFollowLink();

  protected abstract int getRefetchDepth();

  protected void setCrawlConfig(Configuration config) {
    super.setCrawlConfig(config);
    alwaysReparse = config.getBoolean(PARAM_REPARSE_ALL, DEFAULT_REPARSE_ALL);
    usePersistantList = config.getBoolean(PARAM_PERSIST_CRAWL_LIST,
					  DEFAULT_PERSIST_CRAWL_LIST);

    // Do *not* requre that maxDepth be greater than refetchDepth.  Plugin
    // writers set refetchDepth high to mean infinite.
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
    processedUrls = new HashMap<String,CrawlUrlData>();
    maxDepthUrls = new HashMap<String,CrawlUrlData>();

    //XXX short term hack to work around populatePermissionMap not
    //indicating when a crawl window is the problem
    if (!withinCrawlWindow()) {
      crawlStatus.setCrawlStatus(Crawler.STATUS_WINDOW_CLOSED);
      return aborted();
    }

    if (!populatePermissionMap()) {
      return aborted();
    }

    try {
      urlOrderComparator = au.getCrawlUrlComparator();
    } catch (PluginException e) {
      logger.error("Plugin CrawlUrlComparatorFactory error, using breadth-first", e);
    }

    // get the Urls to follow from either NewContentCrawler or OaiCrawler
    try {
      fetchQueue = new CrawlQueue(urlOrderComparator);
      for (String url : getUrlsToFollow()) {
	CrawlUrlData curl = newCrawlUrlData(url, 1);
	addToQueue(curl, fetchQueue, crawlStatus);
      }
    } catch (RuntimeException e) {
      logger.warning("Unexpected exception, should have been caught lower", e);
      if (!crawlStatus.isCrawlError()) {
	crawlStatus.setCrawlStatus(Crawler.STATUS_ERROR);
      }
      abortCrawl();
    }

    if (logger.isDebug3()) logger.debug3("Start URLs: " + fetchQueue );
    if (crawlAborted) {
      return aborted();
    }

    if (usePersistantList) {
      // PERSIST load fetchQueue here
    }

    while (!fetchQueue.isEmpty() && !crawlAborted) {
      // check crawl window during crawl
      if (!withinCrawlWindow()) {
	crawlStatus.setCrawlStatus(Crawler.STATUS_WINDOW_CLOSED);
	return false;
      }
      if (logger.isDebug3()) logger.debug3("Fetch queue: " + fetchQueue);
      int len = fetchQueue.size();
      fqMaxLen = Math.max(fqMaxLen, len);
      fqSumLen += len;
      fqSamples += 1;

      CrawlUrlData curl = fetchQueue.remove();
      if (logger.isDebug3()) logger.debug3("Removed from queue: " + curl);
      hiDepth = Math.max(hiDepth, curl.getDepth());
      String url = curl.getUrl();

      crawlStatus.removePendingUrl(url);
      try {
	if (!fetchAndParse(curl, fetchQueue,
			   processedUrls, maxDepthUrls, alwaysReparse)) {
	  // If failed, make sure an error has been set
	  if (!crawlStatus.isCrawlError()) {
	    logger.warning("fetchAndParse() failed, didn't set error status: "
			   + curl);
	    crawlStatus.setCrawlStatus(Crawler.STATUS_ERROR);
	  }
	}
      } catch (RuntimeException e) {
	if (crawlAborted) {
	  logger.debug("Expected exception while aborting crawl: " + e);
	  return aborted();
	}
	logger.warning("Unexpected exception processing: " + url, e);
	crawlStatus.setCrawlStatus(Crawler.STATUS_ERROR);
	crawlStatus.signalErrorForUrl(url, e.toString());
      }
      if (usePersistantList) {
	// PERSIST save state here
// 	  aus.updatedCrawlUrls(false);
      }

    }
    if (!maxDepthUrls.isEmpty()) {
      String msg = "Site depth exceeds max crawl depth (" + maxDepth + ")";
      logger.error(msg + ". Stopped crawl of " + au.getName());
      logger.debug("Too deep URLs: " + maxDepthUrls);
      crawlStatus.setCrawlStatus(Crawler.STATUS_ERROR, msg);
    } else {
      logger.info("Crawled depth = " + (hiDepth));
    }
    logger.debug("Max queue len: " + fqMaxLen + ", avg: "
		 + Math.round(((double)fqSumLen) / ((double)fqSamples)));
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

  // Overridable for testing
  protected CrawlUrlData newCrawlUrlData(String url, int depth) {
    return new CrawlUrlData(url, depth);
  }

  void addToQueue(CrawlUrlData curl,
			 CrawlQueue queue,
			 CrawlerStatus cstat) {
    try {
      queue.add(curl);
      cstat.addPendingUrl(curl.getUrl());
    } catch (RuntimeException e) {
      logger.error("URL comparator error", e);
      cstat.signalErrorForUrl(curl.getUrl(),
			      "URL comparator error, can't add to queue: "
			      + curl.getUrl() + ": " + e.getMessage());
      cstat.setCrawlStatus(Crawler.STATUS_PLUGIN_ERROR);
      // PriorityBuffer can't recover from comparator error, so this must
      // abort.
      abortCrawl();
    }      
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

  protected boolean fetchAndParse(CrawlUrlData curl,
				  CrawlQueue fetchQueue,
				  Map<String,CrawlUrlData> processedUrls,
				  Map<String,CrawlUrlData> maxDepthUrls,
				  boolean reparse) {

    String url = curl.getUrl();

    //makeUrlCacher needed to handle connection pool
    UrlCacher uc = makeUrlCacher(url);

    // Fetch URL if it has no content already or its depth is within the
    // refetch depth
    if (curl.getDepth() <= getRefetchDepth()
	|| !uc.getCachedUrl().hasContent()) {
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
	  curl.setFetched(true);
	}
      } catch (CacheException.RepositoryException ex) {
	// Failed.  Don't try this one again during this crawl.
	failedUrls.add(uc.getUrl());
	curl.setFailedFetch(true);
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
	curl.setFailedFetch(true);
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
	  curl.setFailedFetch(true);
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
	processedUrls.put(uc.getUrl(), curl);
	return true;
      }
    }

    // don't parse if not following links
    if (!shouldFollowLink()) {
      return (!crawlStatus.isCrawlError());
    }

    // parse the page
    try {
      if (!processedUrls.containsKey(uc.getUrl())) {
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
		// Might be reparsing with new content (if depth reduced
		// below refetch depth); clear any existing children
		curl.clearChildren();
		String charset = getCharset(cu);
		in = FilterUtil.getCrawlFilteredStream(au, in, charset,
						       cu.getContentType());
		extractor.extractUrls(au, in, charset,
				      PluginUtil.getBaseUrl(cu),
				      new MyLinkExtractorCallback(au, curl,
								  fetchQueue,
								  processedUrls,
								  maxDepthUrls));
		// done adding children, trim to size
		curl.trimChildren();
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
	    processedUrls.put(uc.getUrl(), curl);
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
      curl.setFailedParse(true);
      processedUrls.put(uc.getUrl(), curl);
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

  protected boolean checkGloballyExcludedUrl(ArchivalUnit au, String url) {
    if (crawlMgr != null) {
      if (crawlMgr.isGloballyExcludedUrl(au, url)) {
	crawlStatus.signalErrorForUrl(url, "Excluded (probable recursion)");
	String msg = "URL excluded (probable recursion): " + url;
	logger.siteWarning(msg);
	if (alertMgr != null) {
	  alertMgr.raiseAlert(Alert.auAlert(Alert.CRAWL_EXCLUDED_URL, au), msg);
	}
	return true;
      }
    }
    return false;
  }

  private LinkExtractor getLinkExtractor(CachedUrl cu) {
    ArchivalUnit au = cu.getArchivalUnit();
    return au.getLinkExtractor(cu.getContentType());
  }

  // It is expected that a new instance of this class is created for each
  // page parsed
  class MyLinkExtractorCallback implements LinkExtractor.Callback {
    CrawlUrlData curl;
    Map<String,CrawlUrlData> processedUrls;
    Map<String,CrawlUrlData> maxDepthUrls;
    CrawlQueue fetchQueue;
    ArchivalUnit au;
    Set foundUrls = new HashSet();	// children of this node
    CrawlUrlData.ReducedDepthHandler rdh = new ReducedDepthHandler();

    public MyLinkExtractorCallback(ArchivalUnit au,
				   CrawlUrlData curl,
				   CrawlQueue fetchQueue,
				   Map<String,CrawlUrlData> processedUrls,
				   Map<String,CrawlUrlData> maxDepthUrls) {
      this.au = au;
      this.curl = curl;
      this.fetchQueue = fetchQueue;
      this.processedUrls = processedUrls;
      this.maxDepthUrls = maxDepthUrls;
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
	if (normUrl.equals(curl.getUrl())) {
	  if (logger.isDebug3()) logger.debug3("Self reference to " + url);
	  return;
	}
	if (foundUrls.contains(normUrl)) {
	  // for simplicity and efficiency, CrawlUrlData doesn't allow children
	  // to be added redundantly, so prevent it here.
	  if (logger.isDebug3()) logger.debug3("Redundant child: " + normUrl);
	  return;
	}
	foundUrls.add(normUrl);

	CrawlUrlData child = null;
	if ((child = processedUrls.get(normUrl)) != null) {
	  if (logger.isDebug2())
	    logger.debug2("Already processed url: " + child);
	} else if ((child = fetchQueue.get(normUrl)) != null) {
	  if (logger.isDebug3())
	    logger.debug3("Already queued url: " + child);
	} else if ((child = maxDepthUrls.get(normUrl)) != null) {
	  if (logger.isDebug3())
	    logger.debug3("Already too-deep url: " + child);
	} else if (excludedUrlCache.containsKey(normUrl)
	       || failedUrls.contains(normUrl)) {
	  // au.shouldBeCached() is expensive, don't call it if we already
	  // know the answer
	  if (logger.isDebug3())
	    logger.debug3("Already failed or excluded url: " + normUrl);
	  return;
	} else {
	  if (au.shouldBeCached(normUrl)) {
 	    if (checkGloballyExcludedUrl(au, normUrl)) {
	      if (logger.isDebug2()) {
		logger.debug2("Globally excluded url: "+normUrl);
	      }
	      return;
	    } else {
	      if (logger.isDebug2()) {
		logger.debug2("Included url: "+normUrl);
	      }
	      child = newCrawlUrlData(normUrl, curl.getDepth() + 1);
	      if (child.getDepth() > maxDepth) {
		maxDepthUrls.put(normUrl, child);
	      } else {
		addToFetchQueue(child);
	      }
	    }
	  } else {
	    if (logger.isDebug2()) {
	      logger.debug2("Excluded url: "+normUrl);
	    }
	    crawlStatus.signalUrlExcluded(normUrl);
	    excludedUrlCache.put(normUrl, "");
	  }
	}
	if (child != null) {
	  curl.addChild(child, rdh);
	}
      } catch (MalformedURLException e) {
	//XXX what exactly does this log want to tell?
	logger.warning("Normalizing", e);
      } catch (PluginBehaviorException e) {
	logger.warning("Normalizing", e);
      }
    }

    void addToFetchQueue(CrawlUrlData curl) {
      addToQueue(curl, fetchQueue, crawlStatus);
    }

    /** Called whenever the depth of an already-known child node is reduced
     * (due to discovering that it's a child of a node shallower than any
     * existing parents). */
    class ReducedDepthHandler implements CrawlUrlData.ReducedDepthHandler {
      public void depthReduced(CrawlUrlData curl, int from, int to) {
	if (logger.isDebug3())
	  logger.debug3("depthReduced("+from+","+to+"): "+curl);
	if (from > maxDepth && to <= maxDepth) {
	  // If previously beyond max craw depth, is now eligible to be fetched
	  CrawlUrlData tooDeepUrl = maxDepthUrls.remove(curl.getUrl());
	  if (tooDeepUrl != curl) {
	    logger.warning("Previously too deep " + tooDeepUrl
			   + " != no longer too deep " + curl);
	  }
	  if (logger.isDebug2()) logger.debug2("Rescued from too deep: " +curl);
	  addToFetchQueue(curl);
	} else if (to <= maxDepth &&
		   from > getRefetchDepth() &&
		   to <= getRefetchDepth()) {
	  // If previously beyond refetch depth and has already been processed
	  // and not fetched, requeue to now be fetched
	  CrawlUrlData processedCurl = processedUrls.get(curl.getUrl());
	  if (processedCurl != null && !processedCurl.isFetched()) {
	    if (processedCurl != curl) {
	      logger.warning("Previously processed " + processedCurl
			     + " != now within refetch depth " + curl);
	    }
	    processedUrls.remove(curl.getUrl());
	    addToFetchQueue(curl);
	    if (logger.isDebug2()) logger.debug2("Requeued for fetch: " + curl);
	  }
	}
      }
    }
  }

}
