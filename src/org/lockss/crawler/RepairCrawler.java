/*
 * $Id: RepairCrawler.java,v 1.67 2007-08-30 09:55:43 smorabito Exp $
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
import java.io.*;

import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.protocol.*;
import org.lockss.proxy.ProxyManager;
import org.lockss.plugin.*;
import org.lockss.state.*;

/**
 * Repair crawler that can do the repair crawl in 4 modes.
 * <ul>
 * <li>1. repair from other caches only
 * <li>2. repair from publisher only
 * <li>3. repair from other caches first. If repair fails in trying
 *        a certain number of caches, it will try repair from the publisher
 * <li>4. repair from publisher first. If repair fails, it will try repair from other caches
 * </ul>
 * <p>
 * In mode 1,3,4, the number of other caches the repair crawler will try from can be change
 * by setting PARAM_NUM_RETRIES_FROM_CACHES in properties.
 * <p>
 * e.g.
 * <br>
 * Properties p = new Properties();<br>
 * p.setProperty(RepairCrawler.PARAM_NUM_RETRIES_FROM_CACHES, ""+2); <br>
 * ConfigurationUtil.setCurrentConfigFromProps(p); <br>
 * <br>
 * That will set the retry limit to 2.
 */
public class RepairCrawler extends BaseCrawler {

  private static Logger logger = Logger.getLogger("RepairCrawler");

  private IdentityManager idMgr = null;

  protected Collection repairUrls = null;

  /**
   * Sets this to true in properties and repair will be done from other caches only
   */
  public static final String PARAM_FETCH_FROM_OTHER_CACHES_ONLY =
    Configuration.PREFIX + "crawler.fetch_from_other_caches_only";
  public static final boolean DEFAULT_FETCH_FROM_OTHER_CACHES_ONLY = false;

  /**
   * Sets this to true in properties and repair will be done from publisher only
   */
  public static final String PARAM_FETCH_FROM_PUBLISHER_ONLY =
    Configuration.PREFIX + "crawler.fetch_from_publisher_only";
  public static final boolean DEFAULT_FETCH_FROM_PUBLISHER_ONLY = true;

  /**
   * Sets this in properties to limit the number of caches it will try in repair mode 1,3 and 4.
   */
  public static final String PARAM_NUM_RETRIES_FROM_CACHES =
    Configuration.PREFIX + "crawler.num_retries_from_caches";
  public static final int DEFAULT_NUM_RETRIES_FROM_CACHES = 5; //XXX subject to change

  /**
   * Force repair-from-cache to fetch from this address.  Used in testing
   * when running multiple daemons on one machine.
   */
  public static final String PARAM_REPAIR_FROM_CACHE_ADDR =
    Configuration.PREFIX + "crawler.repair_from_cache_addr";

  /**
   * Sets this to true will cause repairs to require permission
   */
  public static final String PARAM_REPAIR_NEEDS_PERMISSION =
    Configuration.PREFIX + "crawler.repair_needs_permission";
  public static final boolean DEFAULT_REPAIR_NEEDS_PERMISSION = false;

  public static final String PARAM_MAX_REPAIRS_OUTSIDE_WINDOW =
    Configuration.PREFIX + "crawler.maxRepairsOutsideWindow";
  public static final int DEFAULT_MAX_REPAIRS_OUTSIDE_WINDOW = 0;



  /** Poller requires fetched URLs to be kept in status */
  public static final String FORCE_RECORD_STATUS_URLS = "fetched";

  boolean fetchCache = DEFAULT_FETCH_FROM_OTHER_CACHES_ONLY;
  boolean fetchPublisher = DEFAULT_FETCH_FROM_PUBLISHER_ONLY;
  boolean repairNeedsPermission;
  int numCacheRetries = DEFAULT_NUM_RETRIES_FROM_CACHES;
//   int numPubRetries = DEFAULT_NUM_RETRIES_FROM_PUBLISHER;
  String repairFromCacheAddr = null;

  int maxRepairsOutsideWindow;

  private float percentFetchFromCache = 0;

  public RepairCrawler(ArchivalUnit au, CrawlSpec spec,
		       AuState aus, Collection repairUrls,
		       float percentFetchFromCache) {
    super(au, spec, aus);
    if (repairUrls.size() == 0) {
      throw new IllegalArgumentException("Called with empty repairUrls list");
    }
    this.repairUrls = repairUrls;
    this.percentFetchFromCache = percentFetchFromCache;

    crawlStatus = new CrawlerStatus(au, repairUrls, getTypeString(),
				    FORCE_RECORD_STATUS_URLS);
  }

  protected void setCrawlConfig(Configuration config) {
    super.setCrawlConfig(config);
    fetchCache = config.getBoolean(PARAM_FETCH_FROM_OTHER_CACHES_ONLY,
				   DEFAULT_FETCH_FROM_OTHER_CACHES_ONLY);
    fetchPublisher =
      config.getBoolean(PARAM_FETCH_FROM_PUBLISHER_ONLY,
			DEFAULT_FETCH_FROM_PUBLISHER_ONLY);
    numCacheRetries = config.getInt(PARAM_NUM_RETRIES_FROM_CACHES,
					DEFAULT_NUM_RETRIES_FROM_CACHES);
//     numPubRetries = config.getInt(PARAM_NUM_RETRIES_FROM_PUBLISHER,
// 				  DEFAULT_NUM_RETRIES_FROM_PUBLISHER);
    repairFromCacheAddr = config.get(PARAM_REPAIR_FROM_CACHE_ADDR);
    repairNeedsPermission = config.getBoolean(PARAM_REPAIR_NEEDS_PERMISSION,
                                              DEFAULT_REPAIR_NEEDS_PERMISSION);
    maxRepairsOutsideWindow = config.getInt(PARAM_MAX_REPAIRS_OUTSIDE_WINDOW,
                                            DEFAULT_MAX_REPAIRS_OUTSIDE_WINDOW);

  }

  public String getTypeString() {
    return "Repair";
  }

  public boolean isWholeAU() {
    return false;
  }

  public int getType() {
    return Crawler.REPAIR;
  }

  protected Iterator getStartingUrls() {
    return repairUrls.iterator();
  }

  /** Create a UrlCacher that follows redirects in crawl spec, but stores
   * only the one node we requested.  This should *not* override
   * super.makeUrlCacher(), as that is called from other places in the
   * crawl (e.g, PermissionMap), which don't want the special options for
   * fetching repairs */
  protected UrlCacher makeRepairUrlCacher(String url) {
    UrlCacher uc = makeUrlCacher(url);
    uc.setRedirectScheme(UrlCacher.REDIRECT_SCHEME_FOLLOW_IN_SPEC);
    BitSet fetchFlags = new BitSet();
    fetchFlags.set(UrlCacher.REFETCH_FLAG);
    uc.setFetchFlags(fetchFlags);
    return uc;
  }

  protected boolean doCrawl0() {
    boolean windowClosed = false;
//     logger.info("Beginning crawl of "+au);
//     crawlStatus.signalCrawlStarted();

    if (!populatePermissionMap()) {
      return aborted();
    }

    Iterator it = getStartingUrls();

    int numRepairedOutsideCrawlWindow = 0;

    while (it.hasNext() && !crawlAborted) {
      String url = (String)it.next();
      //catch and warn if there's a url in the start urls
      //that we shouldn't cache
      // check crawl window during crawl
      if (!spec.inCrawlWindow()) {
	if (numRepairedOutsideCrawlWindow >= maxRepairsOutsideWindow) {
	  logger.debug("Crawl canceled: outside of crawl window");
	  windowClosed = true;
	  // break from while loop
	  break;
	} else {
	  logger.debug("Outside of crawl window, but repairing "
	               + numRepairedOutsideCrawlWindow + " more URLs.");
	  numRepairedOutsideCrawlWindow++;
	}
      }
      if (!spec.isIncluded(url)) {
	if (url.charAt(url.length()-1) != '/') {
	  String newUrl = url+'/';
	  if (spec.isIncluded(newUrl)) {
	    url = newUrl;
	  }
	}
      }
      if (spec.isIncluded(url)) {
	String crawlRes = null;
	try {
	  crawlRes = doCrawlLoop(url);
	} catch (RuntimeException e) {
	  logger.warning("Unexpected exception in crawl", e);
	  crawlRes = Crawler.STATUS_ERROR;
	  crawlStatus.signalErrorForUrl(url, "Unexpected Exception");
	}
	if (crawlRes != null) {
	  if (crawlStatus.getCrawlError() == null) {
	    crawlStatus.setCrawlError(crawlRes);
	  }
	}
      } else {
	logger.warning("Called with a starting url we aren't suppose to "+
		       "cache: "+url);
      }
    }
    if (crawlAborted) {
      return aborted();
    } else if (windowClosed) {
      // unsuccessful crawl if window closed
      crawlStatus.setCrawlError(Crawler.STATUS_WINDOW_CLOSED);
    }
    if (crawlStatus.getCrawlError() != null) {
      logger.info("Finished crawl (errors) of "+au);
    } else {
      logger.info("Finished crawl of "+au);
    }
    return (crawlStatus.getCrawlError() == null);
  }

  protected String doCrawlLoop(String url) {
    String error = null;
    logger.debug2("Dequeued url from list: "+url);

    // don't cache if already cached, unless overwriting
    try {
      if (wdog != null) {
	wdog.pokeWDog();
      }

      //4 cases to choose where to repair from
      if (fetchCache && fetchPublisher) {
	logger.error("Contradicting parameters! both PARAM_FETCH_FROM_OTHER_CACHE_ONLY " +
		     "and PARAM_FETCH_FROM_PUBLISHER_ONLY flag is true.");
	error = Crawler.STATUS_FETCH_ERROR;
      } else if (fetchCache) { //(1) other caches only
	try {
	  logger.debug3("Trying to fetch from caches only");
	  if (CurrentConfig.getCurrentConfig().containsKey(PARAM_NUM_RETRIES_FROM_CACHES)){
	    // try only from some caches
	    fetchFromSomeCache(url, numCacheRetries);
	  } else {
	    // try "all" possible caches
	    fetchFromSomeCache(url, Integer.MAX_VALUE);
	  }
	} catch (LockssUrlConnection.CantProxyException e){
	  logger.warning("Failed to fetch from caches", e);
	  error = Crawler.STATUS_FETCH_ERROR;
	}
      } else if (fetchPublisher){ //(2) publisher only
	try {
	  logger.debug3("Trying to fetch from publisher only");
	  fetchFromPublisher(url);
	} catch (CacheException e) {
	  logger.warning(url+" not found on publisher's site",e);
	  error = Crawler.STATUS_FETCH_ERROR;
	}
      } else if (shouldFetchFromCache()) { //(3) caches then publisher
	try {
	  logger.debug3("Trying to fetch from caches");
	  //it will try the publisher if it fails to cache
	  //from other caches after the default # of retries
	  fetchFromSomeCache(url, numCacheRetries);
	} catch (LockssUrlConnection.CantProxyException e) {
	  logger.debug3("Failed, so trying to fetch from publisher");
	  try {
	    fetchFromPublisher(url);
	  } catch (CacheException ex) {
	    logger.warning(url+" not found on publisher's site",e);
	    error = Crawler.STATUS_FETCH_ERROR;
	  }
	}
      } else { //(4) publisher then caches
	try {
	  logger.debug3("Trying to fetch from publisher");
	  fetchFromPublisher(url);
	} catch (CacheException e){
	  logger.warning(url+" not found on publisher's site",e);
	  logger.debug3("Trying to fetch from other caches");
	  try {
	    fetchFromSomeCache(url, numCacheRetries);
	  } catch (LockssUrlConnection.CantProxyException ex){
	    logger.warning("Failed to fetch from caches", e);
	    error = Crawler.STATUS_FETCH_ERROR;
	  }
	}
      }
      if (error != null) {
	crawlStatus.signalErrorForUrl(url, error);
      }

    } catch (CacheException e) {
      if (e.isAttributeSet(CacheException.ATTRIBUTE_FAIL)) {
	logger.error("Problem caching "+url+". Ignoring", e);
	error = Crawler.STATUS_FETCH_ERROR;
      } else {
	logger.warning(url+" not found on publisher's site", e);
      }
    } catch (LockssUrlConnection.CantProxyException e){
      logger.warning("Failed to fetch from caches", e);
    } catch (IOException e) {
      //XXX not expected
      logger.critical("Unexpected IOException during crawl", e);
      error = Crawler.STATUS_FETCH_ERROR;
    }
    return error;
  }

  private boolean shouldFetchFromCache() {
    logger.debug3("Checking if we should fetch from a cache, probability "
		  + percentFetchFromCache);
    return AuUtil.isPubDown(au) ||
      ProbabilisticChoice.choose(percentFetchFromCache);
  }

  protected void fetchFromSomeCache(String url, int numCacheRetries)
      throws IOException {
    IdentityManager idm = getIdentityManager();
    Collection repairers = idm.getCachesToRepairFrom(au);
    if (repairers == null) {
      throw new LockssUrlConnection.CantProxyException("We don't have agree history with any caches");
    }
    int iz = 0;
    boolean repaired = false;
    for (Iterator it = repairers.iterator();
	 it.hasNext() && (iz < numCacheRetries); ) {
      PeerIdentity cacheId = (PeerIdentity)it.next();
      logger.debug3("Trying repair "+iz+
		    (numCacheRetries != Integer.MAX_VALUE
		     ? (" of "+numCacheRetries) : "")
		    +" from "+cacheId);
      if (idm.isLocalIdentity(cacheId)) {
	logger.debug("Got local peer identity, skipping");
	continue;
      }
      try {
	fetchFromCache(url, cacheId);
	repaired = true;
	break;
      } catch (IOException e) {
	logger.warning(url + " cannot be fetched from "+ cacheId, e);
      }
      iz++;
    }

    if (!repaired) {
      throw new LockssUrlConnection.CantProxyException(url + " couldn't repair from other caches");
    }
  }

  protected void fetchFromCache(String url, PeerIdentity id)
      throws IOException {
    UrlCacher uc = makeRepairUrlCacher(url);
    fetchFromCache(uc, id);
  }

  protected void fetchFromCache(UrlCacher uc, PeerIdentity id)
      throws IOException {
    logger.debug2("Trying to fetch from "+id);
    String addr = id.getIdString();
    if (!StringUtil.isNullString(repairFromCacheAddr)) {
      logger.debug2("But actually sending request to " + repairFromCacheAddr);
      addr = repairFromCacheAddr;
      uc.setRequestProperty(Constants.X_LOCKSS_REAL_ID, id.getIdString());
    }
    uc.setProxy(addr, getProxyPort());
    uc.setRequestProperty(Constants.X_LOCKSS, Constants.X_LOCKSS_REPAIR);
    try {
      updateCacheStats(cache(uc, addr), uc);
      crawlStatus.addSource(addr);
    } catch (IOException e) {
      logger.warning("Repair from cache failed", e);
      throw new LockssUrlConnection.CantProxyException(e.toString());
    }
  }

  private int cache(UrlCacher uc, String id) throws IOException {
    InputStream input = uc.getUncachedInputStream();
    try {
      CIProperties headers = uc.getUncachedProperties();
      if (headers == null) {
	String err = "Received null headers for url '" + uc.getUrl() + "'.";
	logger.error(err);
	throw new NullPointerException(err);
      } else {
	headers.setProperty(CachedUrl.PROPERTY_REPAIR_FROM, id);
	uc.storeContent(input, headers);
	return UrlCacher.CACHE_RESULT_FETCHED;
      }
    } finally {
      input.close();
    }
  }

  protected int getProxyPort(){
    ProxyManager proxyMan =
      (ProxyManager)LockssDaemon.getManager(LockssDaemon.PROXY_MANAGER);
    return proxyMan.getProxyPort();
  }

  protected void fetchFromPublisher(String url) throws IOException {
    UrlCacher uc = makeRepairUrlCacher(url);
    fetchFromPublisher(uc);
  }

  protected void fetchFromPublisher(UrlCacher uc) throws IOException {
    if (repairNeedsPermission) {
      if (!permissionMap.hasPermission(uc.getUrl())) {
        if (crawlStatus.getCrawlError() == null) {
          crawlStatus.setCrawlError("No permission to collect " + uc.getUrl());
        }
        return;
      }
    }
    if (proxyHost != null) {
      uc.setProxy(proxyHost, proxyPort);
    }
    updateCacheStats(uc.cache(), uc);
    crawlStatus.addSource("Publisher");
  }

  private IdentityManager getIdentityManager() {
    if (idMgr == null) {
      idMgr =
	(IdentityManager)LockssDaemon.getManager(LockssDaemon.IDENTITY_MANAGER);
    }
    return idMgr;
  }

  /**
   * If we're configured to need permission to repair, populate the permission
   * map, otherwise just return true
   */
  public boolean populatePermissionMap() {
    if (CurrentConfig.getBooleanParam(PARAM_REPAIR_NEEDS_PERMISSION,
                                      DEFAULT_REPAIR_NEEDS_PERMISSION)) {
      return super.populatePermissionMap();
    }
    return true;
  }

//  public PermissionMap getPermissionMap() {
//    if (permissionMap == null) {
//      populatePermissionMap();
////      permissionMap = new PermissionMap();
//    }
//    return permissionMap;
//  }

}
