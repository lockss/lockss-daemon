/*
 * $Id: RepairCrawler.java,v 1.29 2004-09-27 22:39:15 smorabito Exp $
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
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.protocol.*;
import org.lockss.proxy.ProxyManager;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
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
public class RepairCrawler extends CrawlerImpl {
  
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
  public static final boolean DEFAULT_FETCH_FROM_PUBLISHER_ONLY = false;

  /**
   * Sets this in properties to limit the number of caches it will try in repair mode 1,3 and 4.
   */
  public static final String PARAM_NUM_RETRIES_FROM_CACHES =
    Configuration.PREFIX + "crawler.num_retries_from_caches";
  public static final int DEFAULT_NUM_RETRIES_FROM_CACHES = 5; //XXX subject to change

  boolean fetchCache = DEFAULT_FETCH_FROM_OTHER_CACHES_ONLY;
  boolean fetchPublisher = DEFAULT_FETCH_FROM_PUBLISHER_ONLY;
  int numCacheRetries = DEFAULT_NUM_RETRIES_FROM_CACHES;
//   int numPubRetries = DEFAULT_NUM_RETRIES_FROM_PUBLISHER;

  private float percentFetchFromCache = 0;

  public RepairCrawler(ArchivalUnit au, CrawlSpec spec,
		       AuState aus, Collection repairUrls,
		       float percentFetchFromCache) {
    super(au, spec, aus);
    if (repairUrls == null) {
      throw new IllegalArgumentException("Called with null repairUrls");
    } else if (repairUrls.size() == 0) {
      throw new IllegalArgumentException("Called with empty repairUrls list");
    }
    this.repairUrls = repairUrls;
    this.percentFetchFromCache = percentFetchFromCache;

    crawlStatus = new Crawler.Status(au, repairUrls, getType());
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
  }

  public int getType() {
    return Crawler.REPAIR;
  }

  protected Iterator getStartingUrls() {
    return repairUrls.iterator();
  }

  /** We always want our UrlCacher to follow redirects in crawl spec, but
   * store only the one node we requested. */
  protected UrlCacher makeUrlCacher(CachedUrlSet cus, String url) {
    UrlCacher uc = super.makeUrlCacher(cus, url);
    uc.setRedirectScheme(UrlCacher.REDIRECT_SCHEME_FOLLOW_IN_SPEC);
    uc.setForceRefetch(true);
    return uc;
  }

  protected boolean doCrawl0() {
    boolean windowClosed = false;
//     logger.info("Beginning crawl of "+au);
//     crawlStatus.signalCrawlStarted();
    CachedUrlSet cus = au.getAuCachedUrlSet();

    Iterator it = getStartingUrls();
    
    while (it.hasNext() && !crawlAborted) {
      String url = (String)it.next();
      //catch and warn if there's a url in the start urls
      //that we shouldn't cache
      // check crawl window during crawl
      if ((spec!=null) && (!spec.canCrawl())) {
	logger.debug("Crawl canceled: outside of crawl window");
	windowClosed = true;
	// break from while loop
	break;
      }
      if (spec.isIncluded(url)) {
	boolean crawlRes = false;
	try {
	  crawlRes = doCrawlLoop(url, cus);
	} catch (RuntimeException e) {
	  logger.warning("Unexpected exception in crawl", e);
	}
	if (!crawlRes) {
	  if (crawlStatus.getCrawlError() == null) {
	    crawlStatus.setCrawlError(Crawler.STATUS_ERROR);
	  }
	}
      } else {
	logger.warning("Called with a starting url we aren't suppose to "+
		       "cache: "+url);
      }
      if (windowClosed) {
	// break from for loop
	break;
      }
    }
    if (crawlAborted) {
      logger.info("Crawl aborted: "+au);
      if (crawlStatus.getCrawlError() == null) {
	crawlStatus.setCrawlError(Crawler.STATUS_INCOMPLETE);
      }
      return false;
    }
    if (windowClosed) {
      // unsuccessful crawl if window closed
      crawlStatus.setCrawlError(Crawler.STATUS_WINDOW_CLOSED);
    }
    if (crawlStatus.getCrawlError() != null) {
      logger.info("Finished crawl (errors) of "+au);
    } else {
      logger.info("Finished crawl of "+au);
    }
    
    if (au instanceof BaseArchivalUnit) {
      BaseArchivalUnit bau = (BaseArchivalUnit)au;
      long cacheHits = bau.getCrawlSpecCacheHits();
      long cacheMisses = bau.getCrawlSpecCacheMisses();
      double per = ((float)cacheHits /
		    ((float)cacheHits + (float)cacheMisses));
      logger.info("Had "+cacheHits+" cache hits, with a percentage of "+(per*100));
    }
    return (crawlStatus.getCrawlError() == null);
  }

  protected boolean doCrawlLoop(String url, CachedUrlSet cus) {
    String error = null;
    logger.debug2("Dequeued url from list: "+url);
    UrlCacher uc = makeUrlCacher(cus, url);

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
	  if (Configuration.getCurrentConfig().containsKey(PARAM_NUM_RETRIES_FROM_CACHES)){
	    // try only from some caches
	    fetchFromSomeCache(uc, numCacheRetries);
	  } else {
	    // try "all" possible caches
	    fetchFromSomeCache(uc, Integer.MAX_VALUE);
	  }
	} catch (LockssUrlConnection.CantProxyException e){
	  logger.warning("Failed to fetch from caches", e);
	}
      } else if (fetchPublisher){ //(2) publisher only
	try {
	  logger.debug3("Trying to fetch from publisher only");
	  fetchFromPublisher(uc);
	} catch (CacheException e) {
	  logger.warning(uc+" not found on publisher's site",e);
	}
      } else if (shouldFetchFromCache()) { //(3) caches then publisher
	try {
	  logger.debug3("Trying to fetch from caches");
	  //it will try the publisher if it fails to cache 
	  //from other caches after the default # of retries
	  fetchFromSomeCache(uc, numCacheRetries);
	} catch (LockssUrlConnection.CantProxyException e) {
	  logger.debug3("Failed, so trying to fetch from publisher");
	  fetchFromPublisher(uc);
	}
      } else { //(4) publisher then caches
	try {
	  logger.debug3("Trying to fetch from publisher");
	  fetchFromPublisher(uc);
	} catch (CacheException e){
	  logger.warning(uc+" not found on publisher's site",e);
	  logger.debug3("Trying to fetch from other caches");
	  fetchFromSomeCache(uc, numCacheRetries);
	}
      }
      crawlStatus.signalUrlFetched();

    } catch (CacheException e) {
      if (e.isAttributeSet(CacheException.ATTRIBUTE_FAIL)) {
	logger.error("Problem caching "+uc+". Ignoring", e);
	error = Crawler.STATUS_FETCH_ERROR;
      } else {
	logger.warning(uc+" not found on publisher's site", e);
      }
    } catch (LockssUrlConnection.CantProxyException e){
      logger.warning("Failed to fetch from caches", e);
    } catch (IOException e) {
      //XXX not expected
      logger.critical("Unexpected IOException during crawl", e);
      error = Crawler.STATUS_FETCH_ERROR;
    }
    return (error == null);
  }

  private boolean shouldFetchFromCache() {
    return ProbabilisticChoice.choose(percentFetchFromCache);
  }
  
  protected void fetchFromSomeCache(UrlCacher uc, int numCacheRetries)
      throws IOException {
    IdentityManager idm = getIdentityManager();
    Map map = idm.getAgreed(au);
    if (map == null) {
      throw new LockssUrlConnection.CantProxyException("We don't have agree history with any caches");
    } 
    Set keySet = map.keySet();
    if (keySet == null) {
      logger.warning("Got a null keyset, this probably shouldn't happen");
      throw new LockssUrlConnection.CantProxyException("Couldn't get a cache we agree with");
    }
    Iterator it = keySet.iterator();
    int iz = 0;
    boolean repaired = false;
    while ((it.hasNext()) && (iz < numCacheRetries) && !repaired ){
      String cacheId = null;
      try {
	cacheId = (String) it.next();
	fetchFromCache(uc, cacheId);
	repaired = true;
      } catch (IOException e) {
	logger.warning(uc.getUrl() + " cannot be fetched from "+ cacheId, e);
      }
      iz++;
    }

    if (!repaired) {
      throw new LockssUrlConnection.CantProxyException(uc.getUrl() + " couldn't repair from other caches");
    }
  }

  protected void fetchFromCache(UrlCacher uc, String id)
      throws IOException {
    logger.debug2("Trying to fetch from "+id);
    uc.setProxy(id, getProxyPort());
    uc.setRequestProperty(Constants.X_LOCKSS, Constants.X_LOCKSS_REPAIR);
    try {
      cache(uc, id);
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

  protected void fetchFromPublisher(UrlCacher uc) throws IOException {
    if (proxyHost != null) {
      uc.setProxy(proxyHost, proxyPort);
    }
    uc.cache();
  }

  private IdentityManager getIdentityManager() {
    if (idMgr == null) {
      idMgr =
	(IdentityManager)LockssDaemon.getManager(LockssDaemon.IDENTITY_MANAGER);
    }
    return idMgr;
  }
}
