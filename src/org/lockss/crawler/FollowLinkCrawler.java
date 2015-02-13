/*
 * $Id: FollowLinkCrawler.java,v 1.108.2.2 2014-12-27 03:27:44 tlipkis Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.collections.map.LRUMap;
import org.lockss.alert.Alert;
import org.lockss.config.Configuration;
import org.lockss.config.CurrentConfig;
import org.lockss.crawler.CrawlerStatus.ReferrerType;
import org.lockss.daemon.Crawler;
import org.lockss.daemon.PluginBehaviorException;
import org.lockss.daemon.PluginException;
import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.extractor.LinkExtractor;
import org.lockss.filter.FilterUtil;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.ContentValidationException;
import org.lockss.plugin.UrlFetcher;
import org.lockss.plugin.UrlFetcher.FetchResult;
import org.lockss.plugin.base.PassiveUrlConsumerFactory;
import org.lockss.state.AuState;
import org.lockss.state.SubstanceChecker;
import org.lockss.util.Constants;
import org.lockss.util.FifoQueue;
import org.lockss.util.HeaderUtil;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.PluginUtil;
import org.lockss.util.StringUtil;
import org.lockss.util.UrlUtil;
import org.lockss.util.urlconn.CacheException;

/**
 * A abstract class that implemented by NewContentCrawler and OaiCrawler
 * it has the follow link mechanism that used by NewContentCrawler
 * and OaiCrawler.
 */
public class FollowLinkCrawler extends BaseCrawler {

  static Logger logger = Logger.getLogger(FollowLinkCrawler.class);

  public static final String PREFIX = Configuration.PREFIX + "crawler.";

  /** Size of LRU cache of URLs excluded by the crawl rules */
  public static final String PARAM_EXCLUDED_CACHE_SIZE =
    PREFIX + "excludedCacheSize";
  public static final int DEFAULT_EXCLUDED_CACHE_SIZE = 1000;

  public static final String PARAM_REFETCH_DEPTH =
    PREFIX + "refetchDepth.au.<auid>";

  /** The maximum link depth that will be followed */
  public static final String PARAM_MAX_CRAWL_DEPTH =
    PREFIX + "maxCrawlDepth";
  public static final int DEFAULT_MAX_CRAWL_DEPTH = 1000;

  /** Store archive files in addition to exploding them */
  public static final String PARAM_STORE_ARCHIVES =
    PREFIX + "storeArchives";
  public static final boolean DEFAULT_STORE_ARCHIVES = false;

  /** Email address to which crawl-end reports will be sent */
  public static final String PARAM_CRAWL_END_REPORT_EMAIL =
    PREFIX + "crawlEndReportEmail";
  public static final String DEFAULT_CRAWL_END_REPORT_EMAIL = null;

  /** Hash algorithm used to report fixity in crawl end report */
  public static final String PARAM_CRAWL_END_REPORT_HASH_ALG =
    PREFIX + "crawlEndReportHashAlg";
  public static final String DEFAULT_CRAWL_END_REPORT_HASH_ALG = "SHA-1";

  /** If true, empty files will be refetched independent of depth unless
   * the plugin declares that empty files shouldn't be reported. */
  public static final String PARAM_REFETCH_EMPTY_FILES =
    PREFIX + "refetchEmptyFiles";
  public static final boolean DEFAULT_REFETCH_EMPTY_FILES = false;

  protected int maxDepth = DEFAULT_MAX_CRAWL_DEPTH;

  protected int hiDepth = 0;		// maximum depth seen
  protected int fqMaxLen = 0;		// maximum length of fetch queue
  protected double fqSumLen = 0.0;      // sum of fetch queue length samples
  protected int fqSamples = 0;		// number of fetch queue len samples
  protected int refetchDepth = -1;
  
  protected Map<String,CrawlUrlData> processedUrls;
  protected Map<String,CrawlUrlData> maxDepthUrls;
  protected boolean cachingStartUrls = false; //added to report an error when
                                              //not able to cache a starting Url
  
  protected String crawlEndReportEmail = DEFAULT_CRAWL_END_REPORT_EMAIL;
  protected String crawlEndReportHashAlg = DEFAULT_CRAWL_END_REPORT_HASH_ALG;
  protected SubstanceChecker subChecker;
  protected boolean isAbbreviatedCrawlTest = false;
  protected boolean crawlTerminated = false;
  protected boolean isRefetchEmptyFiles = false;
  protected boolean shouldFollowLink = true;
  
  // Cache recent negative results from au.shouldBeCached().  This is set
  // to an LRUMsp when crawl is initialized, it's initialized here to a
  // simple map for the sake of test code, which doesn't call
  // this.setCrawlConfig().  If we want to report all excluded URLs, this
  // can be changed to a simple Set.
  private Map excludedUrlCache = new HashMap();
  private Set<String> failedUrls = new HashSet<String>();
    
  protected CrawlQueue fetchQueue;
  protected Queue<CrawlUrlData> permissionProbeUrls;
  protected FifoQueue parseQueue;
  protected Comparator<CrawlUrl> urlOrderComparator;

  public FollowLinkCrawler(ArchivalUnit au, AuState aus) {
    super(au, aus);
    crawlStatus = new CrawlerStatus(au, au.getStartUrls(),
        getTypeString());
    try {
      urlOrderComparator = au.getCrawlUrlComparator();
    } catch (PluginException e) {
      logger.error("Plugin CrawlUrlComparatorFactory error, using breadth-first", e);
    }
    fetchQueue = new CrawlQueue(urlOrderComparator);
    parseQueue = new FifoQueue();
    permissionProbeUrls = new LinkedList<CrawlUrlData>();
  }

  /** Return true if crawler should follow links from collected files */
  protected boolean shouldFollowLink() {
    return shouldFollowLink;
  }
  
  public boolean isWholeAU() {
    return true;
  }
  
  public Crawler.Type getType() {
    return Crawler.Type.NEW_CONTENT;
  }

  /** Return true if crawler should fail if any start URL(s) can't be
   * fetched */
  protected boolean isFailOnStartUrlError() {
    return getCrawlSeed().isFailOnStartUrlError();
  }

  protected int getRefetchDepth() {
    if (refetchDepth == -1) {
      if (req != null) {
        int reqDepth = req.getRefetchDepth();
        if (reqDepth >= 0) {
          refetchDepth = reqDepth;
          return refetchDepth;
        }
      }   
      int refetchDepth0 = au.getRefetchDepth();
      String key = StringUtil.replaceString(PARAM_REFETCH_DEPTH,
              "<auid>", au.getAuId());
      refetchDepth = CurrentConfig.getIntParam(key, refetchDepth0);
      if (refetchDepth != refetchDepth0) {
        logger.info("Crawl spec refetch depth (" + refetchDepth0 +
            ") overridden by parameter (" + refetchDepth + ")");
      }
    }
    return refetchDepth;
  }

  protected void setCrawlConfig(Configuration config) {
    super.setCrawlConfig(config);
    
    // Do *not* require that maxDepth be greater than refetchDepth.  Plugin
    // writers set refetchDepth high to mean infinite.
    maxDepth = config.getInt(PARAM_MAX_CRAWL_DEPTH, DEFAULT_MAX_CRAWL_DEPTH);

    excludedUrlCache =
      new LRUMap(config.getInt(PARAM_EXCLUDED_CACHE_SIZE,
			       DEFAULT_EXCLUDED_CACHE_SIZE));

    crawlEndReportEmail = config.get(PARAM_CRAWL_END_REPORT_EMAIL,
				     DEFAULT_CRAWL_END_REPORT_EMAIL);
    crawlEndReportHashAlg = config.get(PARAM_CRAWL_END_REPORT_HASH_ALG,
				       DEFAULT_CRAWL_END_REPORT_HASH_ALG);

    isRefetchEmptyFiles =
      config.getBoolean(PARAM_REFETCH_EMPTY_FILES, DEFAULT_REFETCH_EMPTY_FILES)
      && !isIgnoredException(AuUtil.mapException(au,
          null,
          new ContentValidationException.EmptyFile(),
          null));
  }
 

  protected boolean doCrawl0() {
    if (isAborted()) {
      return aborted(ABORTED_BEFORE_START_MSG);
    }
    logger.info("Beginning crawl, refetch depth: " + getRefetchDepth() +
        ", max depth: " + maxDepth + " " +
        (shouldFollowLink() ? "" : "(no follow) ") +
        "of " + au);
    crawlStatus.addSource("Publisher");
    crawlStatus.setRefetchDepth(getRefetchDepth());
    processedUrls = new HashMap<String,CrawlUrlData>();
    maxDepthUrls = new HashMap<String,CrawlUrlData>();

    // Enable no-substance-collected detection if so configured and
    // supported by plugin.
    subChecker = new SubstanceChecker(au);
    if (subChecker.isEnabledFor(SubstanceChecker.CONTEXT_CRAWL)) {
      logger.debug2("Checking AU for substance during crawl");
      int threshold = AuUtil.getSubstanceTestThreshold(au);
      if (threshold >= 0) {
        subChecker.setSubstanceMin(threshold);
        isAbbreviatedCrawlTest = true;
        logger.debug("Performing abbreviated crawl test, threshold: " +
            threshold);
      } else {
        isAbbreviatedCrawlTest = false;
      }
    } else {
      subChecker = null;
      logger.debug3("Not checking AU for substance during crawl");
    }

    if (!populatePermissionMap()) {
      if(!crawlStatus.isCrawlError()) {
        crawlStatus.setCrawlStatus(Crawler.STATUS_NO_PUB_PERMISSION,
                                   "Unable to populate permission");
      }
      return false;
    } else if (permissionProbeUrls != null) {
      for(CrawlUrlData pud : permissionProbeUrls) {
        UrlFetcher uf = makePermissionUrlFetcher(pud.getUrl());
        if(!au.storeProbePermission()) {
          uf.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
        }
        try {
          if(uf.fetch() != FetchResult.FETCHED) {
            crawlStatus.setCrawlStatus(Crawler.STATUS_NO_PUB_PERMISSION);
            return false;
          } else {
            updateCacheStats(FetchResult.FETCHED, pud);
          }
        } catch (CacheException e) {
          crawlStatus.setCrawlStatus(Crawler.STATUS_NO_PUB_PERMISSION, e.getMessage());
          return false;
        }
      }
    }

    // get the Urls to follow 
    try {
      enqueueStartUrls();
    } catch (RuntimeException e) {
      logger.warning("Unexpected exception, should have been caught lower", e);
      if (!crawlStatus.isCrawlError()) {
        crawlStatus.setCrawlStatus(Crawler.STATUS_ERROR);
      }
      abortCrawl();
    } catch (OutOfMemoryError e) {
      // daemon may keep running after this, so make sure crawl doesn't
      // appear to be successful
      //logger.error("Crawl aborted", e);
      if (!crawlStatus.isCrawlError()) {
        crawlStatus.setCrawlStatus(Crawler.STATUS_ERROR);
      }
      throw e;
    } catch (ConfigurationException e) {
      logger.error("Unable to start crawl due to invalid configuration" + e);
      abortCrawl();
    } catch (PluginException e) {
      logger.error("Unable to start crawl due to invalid plugin parameters" + e);
      abortCrawl();
    }

    if (logger.isDebug3()) logger.debug3("Start URLs: " + fetchQueue );
    if (isAborted()) {
      return aborted(ABORTED_BEFORE_START_MSG);
    }

    while (!fetchQueue.isEmpty() && !(isAborted() || crawlTerminated)) {
      // check crawl window during crawl
      if (!withinCrawlWindow()) {
        crawlStatus.setCrawlStatus(Crawler.STATUS_WINDOW_CLOSED);
        crawlStatus.setDepth(hiDepth);
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
        if (!fetch(curl)) {
          if (!crawlStatus.isCrawlError()) {
            logger.warning("fetch() failed, didn't set error status: "
                + curl);
            crawlStatus.setCrawlStatus(Crawler.STATUS_ERROR);
            crawlStatus.signalErrorForUrl(url, "Failed to fetch url", Crawler.STATUS_ERROR);
          }
        }
      } catch (RuntimeException e) {
        if (isAborted()) {
          logger.debug("Expected exception while aborting crawl: " + e);
          crawlStatus.setDepth(hiDepth);
          return aborted(e.getMessage());
        }
        logger.warning("Unexpected exception processing: " + url, e);
        crawlStatus.signalErrorForUrl(url, e.toString(), Crawler.STATUS_ERROR);
      }
      
      while(!parseQueue.isEmpty()) {
        try {
          CrawlUrlData parseCurl = (CrawlUrlData) parseQueue.peek();
          if(isAborted()) {
            return aborted();
          }
          parseQueue.remove(parseCurl);
          parse(parseCurl);
          processedUrls.put(parseCurl.getUrl(), parseCurl);
        } catch (RuntimeException e) {
          logger.warning("Unexpected exception parsing: " + url, e);
          crawlStatus.signalErrorForUrl(url, e.toString(), Crawler.STATUS_ERROR);
        }
      }
      if (isAborted()) {
        return aborted();
      }
    }
    
    crawlStatus.setDepth(hiDepth);
    if (!maxDepthUrls.isEmpty()) {
      String msg = "Site depth exceeds max crawl depth (" + maxDepth + ")";
      logger.error(msg + ". Stopped crawl of " + au.getName());
      logger.debug("Too deep URLs: " + maxDepthUrls);
      crawlStatus.setCrawlStatus(Crawler.STATUS_ERROR, msg);
    } else {
      logger.info("Crawled depth = " + (hiDepth) +
          ", fetched " + crawlStatus.getContentBytesFetched() +
          " bytes in " + crawlStatus.getNumFetched() + " files");
    }
    logger.debug("Max queue len: " + fqMaxLen + ", avg: "
        + Math.round((fqSumLen) / ((double)fqSamples)));
    
    if (subChecker != null) {
      switch (subChecker.hasSubstance()) {
      case No:
        String msg =
            "No files containing substantial content were collected.";
        if (!aus.hasNoSubstance()) {
          // Alert only on transition to no substance from other than no
          // substance.
          // XXX should raise this alert only if at least one new file (not
          // just new version) was collected
          if (alertMgr != null) {
            alertMgr.raiseAlert(Alert.auAlert(Alert.CRAWL_NO_SUBSTANCE, au),
                msg);
          }
	  logger.siteWarning("" + au + ": " + msg);
        }
	// update AuState unconditionally to record possible FeatureVersion
	// change
	aus.setSubstanceState(SubstanceChecker.State.No);
        break;
      case Yes:
        if (isAbbreviatedCrawlTest) {
          if (subChecker.getSubstanceCnt() >= subChecker.getSubstanceMin()) {
            logger.debug("Abbreviated crawl test succeeded");
            crawlStatus.setCrawlStatus(Crawler.STATUS_CRAWL_TEST_SUCCESSFUL);
          } else {
            logger.debug("Abbreviated crawl test failed");
            crawlStatus.setCrawlStatus(Crawler.STATUS_CRAWL_TEST_FAIL);
          }
        }
        aus.setSubstanceState(SubstanceChecker.State.Yes);
        break;
      default:
      }
    }

    if (crawlStatus.isCrawlError()) {
      logger.info("Unfinished crawl of " + au.getName() + ", " +
		  crawlStatus.getCrawlErrorMsg());
    } else {
      logger.info("Finished crawl of "+au.getName());
    }
    
    if(isAborted()) {
      return aborted();
    }
    doCrawlEndActions();
    return (!crawlStatus.isCrawlError());
  }

  // Overridable for testing
  protected void enqueueStartUrls() 
      throws ConfigurationException, PluginException {
    for (String url : getCrawlSeed().getStartUrls()) {
      CrawlUrlData curl = newCrawlUrlData(url, 1);
      curl.setStartUrl(true);
      logger.debug2("setStartUrl(" + curl + ")");
      addToQueue(curl, fetchQueue, crawlStatus);
    }
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
			      + curl.getUrl() + ": " + e.getMessage(),
			      Crawler.STATUS_PLUGIN_ERROR);
      // PriorityBuffer can't recover from comparator error, so this must
      // abort.
      abortCrawl();
    }
  }
  
  @Override
  protected void appendAlertInfo(StringBuilder sb) {
    sb.append(String.format("\n\nRefetch Depth: %d\n"
        + "Max Depth: %d\nActual Depth: %d",
			  getRefetchDepth(), maxDepth, crawlStatus.getDepth()));
    if (proxyHost != null) {
      sb.append("\nProxy: ");
      sb.append(proxyHost);
      sb.append(":");
      sb.append(proxyPort);
    }
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

  protected boolean fetch(CrawlUrlData curl) {
    String url = curl.getUrl();
    UrlFetcher fetcher;
    UrlFetcher.FetchResult res;
    // Fetch URL if it has no content already or its depth is within the
    // refetch depth
    CachedUrl cu;
    if (curl.getDepth() <= getRefetchDepth() ||
        !(cu = au.makeCachedUrl(url)).hasContent() ||
        (isRefetchEmptyFiles && cu.getContentSize() == 0)) {
        if (failedUrls.contains(url)) {
          //skip if it's already failed
          logger.debug3("Already failed to cache "+url+". Not retrying.");
          return true;
        } else {
          // checking the crawl permission of the url's host
          if (!permissionMap.hasPermission(url)) {
            if (!crawlStatus.isCrawlError()) {
              crawlStatus.setCrawlStatus(Crawler.STATUS_NO_PUB_PERMISSION,
                  "No permission to collect " + url);
              failedUrls.add(url);
            }
            return false;
          }
          fetcher = makeUrlFetcher(url);
          try {
            res = fetcher.fetch();
            updateCacheStats(res, curl);
            if (res == FetchResult.NOT_FETCHED) {
              if(curl.isStartUrl() && isFailOnStartUrlError()) {
                // fail if cannot fetch a StartUrl
                String msg = "Failed to cache start url: "+ curl.getUrl();
                logger.error(msg);
                crawlStatus.setCrawlStatus(Crawler.STATUS_FETCH_ERROR);
                return false;
              }
            } else {
              checkSubstanceCollected(au.makeCachedUrl(url));
            }
          } catch (CacheException ex) {
            //XXX: we have a fatal exception, but we need to store it
            crawlStatus.signalErrorForUrl(url, ex);
            crawlStatus.setCrawlStatus(Crawler.STATUS_FETCH_ERROR);
            return false;
          }   
          parseQueue.put(curl);
          return true;
        }
    } else {
      // If didn't fetch, check for existing substance file
      checkSubstanceCollected(au.makeCachedUrl(url));
      parseQueue.put(curl);
      return true;
    }
  }
  
  protected void parse(CrawlUrlData curl){
    // don't parse if not following links
    if (shouldFollowLink()) {
      try {
        if (!processedUrls.containsKey(curl.getUrl())) {
          CachedUrl cu = au.makeCachedUrl(curl.getUrl());
          logger.debug3("Parsing "+ cu);
          
          try {
            if (cu.hasContent()) {
              LinkExtractor extractor = getLinkExtractor(cu);
              if (extractor != null) {
                //IOException if the CU can't be read
                InputStream in = null;
                try {
                  pokeWDog();
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
                  crawlStatus.signalUrlParsed(curl.getUrl());
                } catch (PluginException e) {
                  logger.error("Plugin LinkExtractor error", e);
                  crawlStatus.signalErrorForUrl(curl.getUrl(),
                      "Plugin LinkExtractor error: " +
                          e.getMessage(),
                          Crawler.STATUS_PLUGIN_ERROR);
                } finally {
                  IOUtil.safeClose(in);
                }
              }
            }
          } finally {
            cu.release();
          }
        }
      } catch (CacheException ex) {
        crawlStatus.signalErrorForUrl(curl.getUrl(), ex);
        if (ex.isAttributeSet(CacheException.ATTRIBUTE_FATAL)) {
          logger.error("Fatal error parsing "+curl, ex);
          abortCrawl();
        } else if (ex.isAttributeSet(CacheException.ATTRIBUTE_FAIL)) {
          logger.siteError("Couldn't parse "+curl+". continuing", ex);
          crawlStatus.setCrawlStatus(Crawler.STATUS_EXTRACTOR_ERROR);
        } else {
          logger.siteWarning("Couldn't parse "+curl+". ignoring error", ex);
        }
        curl.setFailedParse(true);
        processedUrls.put(curl.getUrl(), curl);
      } catch (IOException ioe) {
        logger.error("Problem parsing "+curl+". Ignoring", ioe);
        crawlStatus.signalErrorForUrl(curl.getUrl(), ioe.getMessage(),
            Crawler.STATUS_FETCH_ERROR);
      }
      logger.debug3("Removing from parsing list: "+ curl.getUrl());
    }
  }

  // Callers are all local and know that we release the CU
  private void checkSubstanceCollected(CachedUrl cu) {
    try {
      if (subChecker != null) {
        subChecker.checkSubstance(cu);
        if (isAbbreviatedCrawlTest &&
            subChecker.getSubstanceCnt() >= subChecker.getSubstanceMin()) {
          crawlStatus.setCrawlStatus(Crawler.STATUS_CRAWL_TEST_SUCCESSFUL);
          crawlTerminated = true;
        }
      }
    } finally {
      AuUtil.safeRelease(cu);
    }
  }

  private String getCharset(CachedUrl cu) {
    String res = null;
    res = HeaderUtil.getCharsetFromContentType(cu.getContentType());
    if (res == null) {
      res = Constants.DEFAULT_ENCODING;
    }
    return res;
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
	// The same URLs may be found repeatedly.  Ensure each is processed
	// only once.  Both CrawlUrlData and CrawlerStatus.signalReferrer()
	// assume no redundant calls.
	if (foundUrls.contains(normUrl)) {
	  if (logger.isDebug3()) logger.debug3("Redundant child: " + normUrl);
	  return;
	}
	foundUrls.add(normUrl);

	CrawlUrlData child = null;
	if ((child = processedUrls.get(normUrl)) != null) {
	  if (logger.isDebug2())
	    logger.debug2("Already processed url: " + child);
	  signalReferrer(normUrl, ReferrerType.Included);
	} else if ((child = fetchQueue.get(normUrl)) != null) {
	  if (logger.isDebug3())
	    logger.debug3("Already queued url: " + child);
	  signalReferrer(normUrl, ReferrerType.Included);
	} else if ((child = maxDepthUrls.get(normUrl)) != null) {
	  if (logger.isDebug3())
	    logger.debug3("Already too-deep url: " + child);
	  signalReferrer(normUrl, ReferrerType.Included);
	} else if (excludedUrlCache.containsKey(normUrl)) {
	  // au.shouldBeCached() is expensive, don't call it if we already
	  // know the answer
	  if (logger.isDebug3())
	    logger.debug3("Already excluded url: " + normUrl);
	  signalReferrer(normUrl, ReferrerType.Excluded);
	  return;
	} else if (failedUrls.contains(normUrl)) {
	    // ditto
	  if (logger.isDebug3())
	    logger.debug3("Already failed to fetch url: " + normUrl);
	  signalReferrer(normUrl, ReferrerType.Included);
	  return;
	} else {
	  if (au.shouldBeCached(normUrl)) {
 	    if (checkGloballyExcludedUrl(au, normUrl)) {
	      if (logger.isDebug2()) {
		logger.debug2("Globally excluded url: "+normUrl);
	      }
	      signalReferrer(normUrl, ReferrerType.Excluded);
	      return;
	    } else {
	      if (logger.isDebug2()) {
		logger.debug2("Included url: "+normUrl);
	      }
	      signalReferrer(normUrl, ReferrerType.Included);
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
	    signalReferrer(normUrl, ReferrerType.Excluded);
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

    void signalReferrer(String url, ReferrerType rt) {
      crawlStatus.signalReferrer(url, curl.getUrl(), rt);
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

  protected CrawlerFacade getCrawlerFacade() {
    if(facade == null) {
      facade = new FollowLinkCrawlerFacade(this, failedUrls,
          permissionProbeUrls, fetchQueue, parseQueue);
    }
    return facade;
  }
  
  public static class FollowLinkCrawlerFacade extends BaseCrawler.BaseCrawlerFacade {
    protected Set<String> failedUrls;
    protected Queue<CrawlUrlData> permissionProbeUrls;
    protected CrawlQueue fetchQueue;
    protected FifoQueue parseQueue;
    
    public FollowLinkCrawlerFacade(FollowLinkCrawler crawler,
        Set<String> failedUrls, Queue<CrawlUrlData> permissionProbeUrls,
        CrawlQueue fetchQueue, FifoQueue parseQueue) {
      super(crawler);
      this.failedUrls = failedUrls;
      this.permissionProbeUrls = permissionProbeUrls;
      this.fetchQueue = fetchQueue;
      this.parseQueue = parseQueue;
    }

    @Override
    public void addToFailedUrls(String url) {
      failedUrls.add(url);
    }

    @Override
    public void addToFetchQueue(CrawlUrlData curl) {
      try {
        ((FollowLinkCrawler)crawler).addToQueue(curl, 
            fetchQueue, getCrawlerStatus());
      } catch (ClassCastException e) {
        throw new ShouldNotHappenException(
            "FollowLinkCrawlerFacade should only "
            + "be built with a FollowLinkCrawler", e);
      }
    }

    @Override
    public void addToParseQueue(CrawlUrlData curl) {
      parseQueue.put(curl);
    }

    @Override
    public void addToPermissionProbeQueue(String probeUrl) {
      permissionProbeUrls.add(new CrawlUrlData(probeUrl, 0));
    }

  }
}
