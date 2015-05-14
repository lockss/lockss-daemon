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
import java.io.*;

import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.config.*;
import org.lockss.crawler.BaseCrawler.BaseCrawlerFacade;
import org.lockss.daemon.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
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

  protected Collection<String> repairUrls = null;

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

  boolean repairNeedsPermission;
//   int numPubRetries = DEFAULT_NUM_RETRIES_FROM_PUBLISHER;
  int maxRepairsOutsideWindow;

  public RepairCrawler(ArchivalUnit au, AuState aus,
      Collection<String> repairUrls) {
    super(au, aus);
    if (repairUrls.size() == 0) {
      throw new IllegalArgumentException("Called with empty repairUrls list");
    }
    this.repairUrls = repairUrls;

    crawlStatus = new CrawlerStatus(au, repairUrls, getTypeString(),
				    FORCE_RECORD_STATUS_URLS);
  }

  protected void setCrawlConfig(Configuration config) {
    super.setCrawlConfig(config);
//     numPubRetries = config.getInt(PARAM_NUM_RETRIES_FROM_PUBLISHER,
// 				  DEFAULT_NUM_RETRIES_FROM_PUBLISHER);
    repairNeedsPermission = config.getBoolean(PARAM_REPAIR_NEEDS_PERMISSION,
                                              DEFAULT_REPAIR_NEEDS_PERMISSION);
    maxRepairsOutsideWindow = config.getInt(PARAM_MAX_REPAIRS_OUTSIDE_WINDOW,
                                            DEFAULT_MAX_REPAIRS_OUTSIDE_WINDOW);

  }

  public boolean isWholeAU() {
    return false;
  }

  public Crawler.Type getType() {
    return Crawler.Type.REPAIR;
  }

  protected Iterator<String> getStartingUrls() {
    return repairUrls.iterator();
  }

  /** Create a UrlFetcher that follows redirects in crawl spec, but stores
   * only the one node we requested.  This should *not* override
   * super.makeUrlCacher(), as that is called from other places in the
   * crawl (e.g, PermissionMap), which don't want the special options for
   * fetching repairs */
  protected UrlFetcher makeRepairUrlFetcher(String url) {
    UrlFetcher uf = makeUrlFetcher(url);
    BitSet fetchFlags = uf.getFetchFlags();
    fetchFlags.set(UrlCacher.REFETCH_FLAG);
    uf.setFetchFlags(fetchFlags);
    return uf;
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
      if (!withinCrawlWindow()) {
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
      if (!au.shouldBeCached(url)) {
        if (url.charAt(url.length()-1) != '/') {
          String newUrl = url+'/';
          if (au.shouldBeCached(newUrl)) {
            url = newUrl;
          }
        }
      }
      if (au.shouldBeCached(url)) {
        try {
          doCrawlLoop(url);
        } catch (RuntimeException e) {
          logger.warning("Unexpected exception in crawl", e);
          crawlStatus.signalErrorForUrl(url,
              "Unexpected error: " + e.getMessage(),
              Crawler.STATUS_ERROR,
              "Unexpected error");
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
      crawlStatus.setCrawlStatus(Crawler.STATUS_WINDOW_CLOSED);
    }
    if (crawlStatus.isCrawlError()) {
      logger.info("Unfinished crawl of " + au.getName() + ", " +
		  crawlStatus.getCrawlErrorMsg());
    } else {
      logger.info("Finished crawl of "+au.getName());
    }
    return (!crawlStatus.isCrawlError());
  }

  protected void doCrawlLoop(String url) {
    logger.debug2("Dequeued url from list: "+url);

    // don't cache if already cached, unless overwriting
    try {
      pokeWDog();
      try {
        logger.debug3("Trying to fetch from publisher only");
        fetchFromPublisher(url);
      } catch (CacheException e) {
        logger.warning(url+" not found on publisher's site: " + e);
        crawlStatus.signalErrorForUrl(url, e.getMessage(),
            Crawler.STATUS_FETCH_ERROR,
            e.getMessage());
      }

    } catch (CacheException e) {
      if (e.isAttributeSet(CacheException.ATTRIBUTE_FAIL)) {
        logger.error("Problem caching "+url+". Ignoring", e);
        crawlStatus.setCrawlStatus(Crawler.STATUS_FETCH_ERROR);
      } else {
        logger.warning(url+" not found on publisher's site", e);
      }
//     } catch (LockssUrlConnection.CantProxyException e){
//       logger.warning("Failed to fetch from caches", e);
    } catch (IOException e) {
      //CRAWLSTATUS not expected
      logger.critical("Unexpected IOException during crawl", e);
      crawlStatus.setCrawlStatus(Crawler.STATUS_FETCH_ERROR, e.toString());
    }
  }

  protected void fetchFromPublisher(String url) throws IOException {
    if (repairNeedsPermission) {
      if (!permissionMap.hasPermission(url)) {
        if (!crawlStatus.isCrawlError()) {
          crawlStatus.setCrawlStatus(Crawler.STATUS_NO_PUB_PERMISSION,
				     "No permission to collect " + url);
        }
        return;
      }
    }
    UrlFetcher uf = makeRepairUrlFetcher(url);
    
    updateCacheStats(uf.fetch(), new CrawlUrlData(url, 1));
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
  
  protected CrawlerFacade getCrawlerFacade() {
    if(facade == null) {
      facade = new RepairCrawlerFacade(this);
    }
    return facade;
  };
  
  public static class RepairCrawlerFacade extends BaseCrawler.BaseCrawlerFacade{
    public RepairCrawlerFacade(BaseCrawler crawler) {
      super(crawler);
    }
    
    @Override
    public void addToFailedUrls(String url) {
      //Do nothing. RepairCrawler does not care about the failed urls
    }
  }

}
