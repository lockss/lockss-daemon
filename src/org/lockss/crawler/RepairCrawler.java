/*
 * $Id: RepairCrawler.java,v 1.17 2004-03-11 09:40:43 tlipkis Exp $
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
import org.lockss.daemon.*;
import org.lockss.protocol.*;
import org.lockss.proxy.ProxyManager;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.state.*;

public class RepairCrawler extends CrawlerImpl {
  
  private static Logger logger = Logger.getLogger("RepairCrawler");

  private IdentityManager idMgr = null;

  protected Collection repairUrls = null;

  public static final String PARAM_FETCH_FROM_OTHER_CACHE =
      Configuration.PREFIX + "crawler.fetch_from_other_caches";

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
    return uc;
  }

  protected boolean doCrawl0() {
    boolean windowClosed = false;
    logger.info("Beginning crawl of "+au);
    crawlStatus.signalCrawlStarted();
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
	  if (crawlStatus.getCrawlError() == 0) {
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
    // unsuccessful crawl if window closed
    if (windowClosed) {
      crawlStatus.setCrawlError(Crawler.STATUS_WINDOW_CLOSED);
    }
    if (crawlStatus.getCrawlError() != 0) {
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
      logger.info("Had "+cacheHits+" cache hits, with a percentage of "+per);
    }
    return (crawlStatus.getCrawlError() == 0);
  }

  protected boolean doCrawlLoop(String url, CachedUrlSet cus) {
    int error = 0;
    logger.debug2("Dequeued url from list: "+url);
    UrlCacher uc = makeUrlCacher(cus, url);

    // don't cache if already cached, unless overwriting
    try {
      if (wdog != null) {
	wdog.pokeWDog();
      }
      if (shouldFetchFromCache()) {
	try {
	  logger.debug3("Trying to fetch from a cache");
	  fetchFromCache(uc);
	} catch (CantProxyException e) {
	  logger.debug3("Failed, so trying to fetch from publisher");
	  cache(uc);
	}
      } else {
	logger.debug3("Trying to fetch from publisher");
	cache(uc);
      }
      numUrlsFetched++;
    } catch (FileNotFoundException e) {
      logger.warning(uc+" not found on publisher's site");
    } catch (IOException ioe) {
      //XXX handle this better.  Requeue?
      logger.error("Problem caching "+uc+". Ignoring", ioe);
      error = Crawler.STATUS_FETCH_ERROR;
    }
    return (error == 0);
  }

  private boolean shouldFetchFromCache() {
    return ProbabilisticChoice.choose(percentFetchFromCache);
  }
  
  protected void fetchFromCache(UrlCacher uc) throws IOException {
    IdentityManager idm = getIdentityManager();
    Map map = idm.getAgreed(au);
    Iterator it = map.keySet().iterator();
    if (it.hasNext()) {
      fetchFromCache(uc, (String)it.next());
    }
  }

  protected void fetchFromCache(UrlCacher uc, String id)
      throws IOException {
    ProxyManager proxyMan = 
      (ProxyManager)LockssDaemon.getManager(LockssDaemon.PROXY_MANAGER);
    int proxyPort = proxyMan.getProxyPort();

    // XXX fix this to use BaseUrlCacher
    LockssUrlConnection conn = UrlUtil.openConnection(uc.getUrl(),
						      connectionPool);
    if (!conn.canProxy()) {
      throw new CantProxyException();
    }
    conn.setProxy(id, proxyPort);
    conn.setRequestProperty("user-agent", LockssDaemon.getUserAgent());
    conn.execute();

    logger.debug("Trying to fetch from "+id);
    uc.storeContent(conn.getResponseInputStream(),
		    getPropertiesFromConn(conn, uc.getUrl(), id));
  }

  // XXX fix this to use BaseUrlCacher
  private CIProperties getPropertiesFromConn(LockssUrlConnection conn,
					     String url, String id)
      throws IOException {
    CIProperties props = new CIProperties();
    // set header properties in which we have interest
    props.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE,
		      conn.getResponseContentType());
    props.setProperty(CachedUrl.PROPERTY_FETCH_TIME,
		      Long.toString(TimeBase.nowMs()));
    props.setProperty(CachedUrl.PROPERTY_ORIG_URL, url);
    props.setProperty(CachedUrl.PROPERTY_REPAIR_FROM, id);
    conn.storeResponseHeaderInto(props, CachedUrl.HEADER_PREFIX);
    String actualURL = conn.getActualUrl();
    if (!url.equals(actualURL)) {
      logger.info("setProperty(\"redirected-to\", " + actualURL + ")");
      props.setProperty(CachedUrl.PROPERTY_REDIRECTED_TO, actualURL);
    }
    return props;
  }

  private void cache(UrlCacher uc) throws IOException {
    try {
      uc.setForceRefetch(true);
      uc.cache();
    } catch (IOException e) {
      logger.debug("Exception when trying to cache "+uc, e);
    }
  }


  private IdentityManager getIdentityManager() {
    if (idMgr == null) {
      idMgr =
	(IdentityManager)LockssDaemon.getManager(LockssDaemon.IDENTITY_MANAGER);
    }
    return idMgr;
  }

  static class CantProxyException extends IOException {
  }
}
