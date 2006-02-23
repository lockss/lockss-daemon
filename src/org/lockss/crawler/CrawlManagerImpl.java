/*
 * $Id: CrawlManagerImpl.java,v 1.85 2006-02-23 06:43:37 tlipkis Exp $
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
import org.apache.commons.collections.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.state.NodeState;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.state.*;
import org.lockss.plugin.*;

/**
 * This is the interface for the object that will sit between the crawler
 * and the rest of the world.  It mediates the different crawl types.
 */
public class CrawlManagerImpl extends BaseLockssDaemonManager
    implements CrawlManager, CrawlManager.StatusSource, ConfigurableManager {

  /**
   * The expiration deadline for a new content crawl, in ms.
   */
  public static final String PARAM_NEW_CONTENT_CRAWL_EXPIRATION =
      Configuration.PREFIX + "crawler.new_content.expiration";

  /**
   * The expiration deadline for a repair crawl, in ms.
   */
  public static final String PARAM_REPAIR_CRAWL_EXPIRATION =
      Configuration.PREFIX + "crawler.repair.expiration";

  public static final String PARAM_REPAIR_FROM_CACHE_PERCENT =
      Configuration.PREFIX + "crawler.repair.repair_from_cache_percent";

  /** Set false to prevent all crawl activity */
  public static final String PARAM_CRAWLER_ENABLED =
    Configuration.PREFIX + "crawler.enabled";
  static final boolean DEFAULT_CRAWLER_ENABLED = true;

  static final String WDOG_PARAM_CRAWLER = "Crawler";
  static final long WDOG_DEFAULT_CRAWLER = 2 * Constants.HOUR;

  static final String PRIORITY_PARAM_CRAWLER = "Crawler";
  static final int PRIORITY_DEFAULT_CRAWLER = Thread.NORM_PRIORITY - 1;

  /**
   * ToDo:
   * 1)handle background crawls
   * 2)check for conflicting crawl types
   * 3)check crawl schedule rules
   */
  private static final String CRAWL_STATUS_TABLE_NAME = "crawl_status_table";
  private static final String SINGLE_CRAWL_STATUS_TABLE_NAME =
    "single_crawl_status";

  private static final long DEFAULT_NEW_CONTENT_CRAWL_EXPIRATION =
    10 * Constants.DAY;
  private static final long DEFAULT_REPAIR_CRAWL_EXPIRATION =
    5 * Constants.DAY;

  public static final float DEFAULT_REPAIR_FROM_CACHE_PERCENT = 0;

  static final String MAX_REPAIR_RATE_PREFIX =
    Configuration.PREFIX + "crawler.maxRapairRate.";
  public static final String PARAM_MAX_REPAIR_CRAWLS_PER_INTERVAL =
    MAX_REPAIR_RATE_PREFIX + "crawls";
  public static final int DEFAULT_MAX_REPAIR_CRAWLS_PER_INTERVAL = 50;
  public static final String PARAM_MAX_REPAIR_CRAWLS_INTERVAL =
    MAX_REPAIR_RATE_PREFIX + "interval";
  public static final long DEFAULT_MAX_REPAIR_CRAWLS_INTERVAL = Constants.DAY;

  static final String MAX_NEW_CONTENT_RATE_PREFIX =
    Configuration.PREFIX + "crawler.maxNewContentRate.";
  public static final String PARAM_MAX_NEW_CONTENT_CRAWLS_PER_INTERVAL =
    MAX_NEW_CONTENT_RATE_PREFIX + "crawls";
  public static final int DEFAULT_MAX_NEW_CONTENT_CRAWLS_PER_INTERVAL = 1;
  public static final String PARAM_MAX_NEW_CONTENT_CRAWLS_INTERVAL =
    MAX_NEW_CONTENT_RATE_PREFIX + "interval";
  public static final long DEFAULT_MAX_NEW_CONTENT_CRAWLS_INTERVAL =
    18 * Constants.HOUR;
  /** Number of most recent crawls for which status will be available */
  static final String PARAM_HISTORY_MAX =
    Configuration.PREFIX + "crawler.historySize";
  static final int DEFAULT_HISTORY_MAX = 500;

  //Tracking crawls for the status info
//   private MultiMap crawlHistory = new MultiHashMap();
  private HistoryList crawlList = new HistoryList(DEFAULT_HISTORY_MAX);

  private MultiMap runningCrawls = new MultiHashMap();


  private long contentCrawlExpiration;
  private long repairCrawlExpiration;
  private float percentRepairFromCache;
  private boolean crawlerEnabled = DEFAULT_CRAWLER_ENABLED;
  private static Logger logger = Logger.getLogger("CrawlManager");
  private Map repairRateLimiters = new HashMap();
  private Map newContentRateLimiters = new HashMap();
  private AuEventHandler auEventHandler;

  /**
   * start the plugin manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    super.startService();

    StatusService statusServ = theDaemon.getStatusService();
    CrawlManagerStatus cmStatus = new CrawlManagerStatus(this);
    statusServ.registerStatusAccessor(CRAWL_STATUS_TABLE_NAME, cmStatus);
    statusServ.registerStatusAccessor(SINGLE_CRAWL_STATUS_TABLE_NAME,
				      new SingleCrawlStatus(cmStatus));
    // register our AU event handler
    auEventHandler = new AuEventHandler.Base() {
	public void auDeleted(ArchivalUnit au) {
	  cancelAuCrawls(au);
	}};
    theDaemon.getPluginManager().registerAuEventHandler(auEventHandler);

  }

  /**
   * stop the plugin manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    if (auEventHandler != null) {
      theDaemon.getPluginManager().unregisterAuEventHandler(auEventHandler);
      auEventHandler = null;
    }
    // checkpoint here
    StatusService statusServ = theDaemon.getStatusService();
    if (statusServ != null) {
      statusServ.unregisterStatusAccessor(CRAWL_STATUS_TABLE_NAME);
    }
    super.stopService();
  }

  public void setConfig(Configuration newConfig, Configuration prevConfig,
			Configuration.Differences changedKeys) {
    contentCrawlExpiration =
      newConfig.getTimeInterval(PARAM_NEW_CONTENT_CRAWL_EXPIRATION,
				DEFAULT_NEW_CONTENT_CRAWL_EXPIRATION);
    repairCrawlExpiration =
      newConfig.getTimeInterval(PARAM_REPAIR_CRAWL_EXPIRATION,
				DEFAULT_REPAIR_CRAWL_EXPIRATION);

    percentRepairFromCache =
      newConfig.getPercentage(PARAM_REPAIR_FROM_CACHE_PERCENT,
			      DEFAULT_REPAIR_FROM_CACHE_PERCENT);
    crawlerEnabled =
      newConfig.getBoolean(PARAM_CRAWLER_ENABLED,
			   DEFAULT_CRAWLER_ENABLED);
    if (changedKeys.contains(MAX_REPAIR_RATE_PREFIX)) {
      resetRateLimiters(newConfig, repairRateLimiters,
 			PARAM_MAX_REPAIR_CRAWLS_PER_INTERVAL,
 			DEFAULT_MAX_REPAIR_CRAWLS_PER_INTERVAL,
 			PARAM_MAX_REPAIR_CRAWLS_INTERVAL,
 			DEFAULT_MAX_REPAIR_CRAWLS_INTERVAL);
    }
    if (changedKeys.contains(MAX_NEW_CONTENT_RATE_PREFIX)) {
      resetRateLimiters(newConfig, newContentRateLimiters,
 			PARAM_MAX_NEW_CONTENT_CRAWLS_PER_INTERVAL,
 			DEFAULT_MAX_NEW_CONTENT_CRAWLS_PER_INTERVAL,
 			PARAM_MAX_NEW_CONTENT_CRAWLS_INTERVAL,
 			DEFAULT_MAX_NEW_CONTENT_CRAWLS_INTERVAL);

    }
    if (changedKeys.contains(PARAM_HISTORY_MAX) ) {
      int cMax = newConfig.getInt(PARAM_HISTORY_MAX, DEFAULT_HISTORY_MAX);
      synchronized (crawlList) {
	crawlList.setMax(cMax);
      }
    }
  }

  void cancelAuCrawls(ArchivalUnit au) {
    synchronized(runningCrawls) {
      Collection crawls = (Collection) runningCrawls.get(au);
      if (crawls != null) {
	Iterator it = crawls.iterator();
	while (it.hasNext()) {
	  Crawler crawler = (Crawler)it.next();
	  crawler.abortCrawl();
	}
      }
    }
  }

  /** Return the rate limiter for the au in the rateLimiterMap, creating it
   * with appropriate parameters if it does not exist. */
  private RateLimiter getRateLimiter(ArchivalUnit au,
				     Map rateLimiterMap,
				     String maxEventsParam,
				     int maxEvantDefault,
				     String intervalParam,
				     long intervalDefault) {
    RateLimiter limiter;
    synchronized (rateLimiterMap) {
      limiter = (RateLimiter)rateLimiterMap.get(au);
      if (limiter == null) {
	limiter =
	  RateLimiter.getConfiguredRateLimiter(ConfigManager.getCurrentConfig(),
					       null,
					       maxEventsParam, maxEvantDefault,
					       intervalParam, intervalDefault);
	rateLimiterMap.put(au, limiter);
      }
    }
    return limiter;
  }

  /** Reset the parameters of all the rate limiters in the map. */
  private void resetRateLimiters(Configuration config,
				 Map rateLimiterMap,
				 String maxEventsParam,
				 int maxEvantDefault,
				 String intervalParam,
				 long intervalDefault) {
    synchronized (rateLimiterMap) {
      for (Iterator iter = rateLimiterMap.entrySet().iterator();
	   iter.hasNext(); ) {
	Map.Entry entry = (Map.Entry)iter.next();
	RateLimiter limiter = (RateLimiter)entry.getValue();
	RateLimiter newLimiter =
	  RateLimiter.getConfiguredRateLimiter(config, limiter,
					       maxEventsParam, maxEvantDefault,
					       intervalParam, intervalDefault);
	entry.setValue(newLimiter);
      }
    }
  }

  public void startRepair(ArchivalUnit au, Collection urls,
			  CrawlManager.Callback cb, Object cookie,
                          ActivityRegulator.Lock lock) {
    //XXX check to make sure no other crawls are running and queue if they are
    if (au == null) {
      throw new IllegalArgumentException("Called with null AU");
    }
    if (urls == null) {
      throw new IllegalArgumentException("Called with null URL");
    }
    // check rate limiter before obtaining locks
    RateLimiter limiter = getRateLimiter(au, repairRateLimiters,
					 PARAM_MAX_REPAIR_CRAWLS_PER_INTERVAL,
					 DEFAULT_MAX_REPAIR_CRAWLS_PER_INTERVAL,
					 PARAM_MAX_REPAIR_CRAWLS_INTERVAL,
					 DEFAULT_MAX_REPAIR_CRAWLS_INTERVAL);
    if (!limiter.isEventOk()) {
      logger.debug("Repair aborted due to rate limiter.");
      callCallback(cb, cookie, false, null);
      return;
    }
    // check with regulator and start repair
    Map locks = getRepairLocks(au, urls, lock);
    if (locks.isEmpty()) {
      logger.debug("Repair aborted due to activity lock.");
      callCallback(cb, cookie, false, null);
      return;
    }
    limiter.event();
    try {
      if (locks.size() < urls.size()) {
	cb = new FailingCallbackWrapper(cb);
      }
      Crawler crawler =
	makeRepairCrawler(au, au.getCrawlSpec(),
			  locks.keySet(), percentRepairFromCache);
      CrawlThread crawlThread =
	new CrawlThread(crawler, cb, cookie, locks.values());
      addToStatusList(crawler.getStatus());
//       crawlHistory.put(au.getAuId(), crawler.getStatus());
      synchronized (runningCrawls) {
	runningCrawls.put(au, crawler);
	crawlThread.start();
      }
    } catch (RuntimeException re) {
      logger.debug("Freeing repair locks...");
      Iterator lockIt = locks.values().iterator();
      while (lockIt.hasNext()) {
	ActivityRegulator.Lock deadLock =
	  (ActivityRegulator.Lock)lockIt.next();
	deadLock.expire();
      }
      lock.expire();
      throw re;
    }
  }

  private Map getRepairLocks(ArchivalUnit au, Collection urlStrs,
                             ActivityRegulator.Lock mainLock) {
    Map locks = new HashMap();
    ActivityRegulator ar = theDaemon.getActivityRegulator(au);
    String mainCusUrl = "";
    if ((mainLock!=null) && (mainLock.getCachedUrlSet()!=null)) {
      mainCusUrl = mainLock.getCachedUrlSet().getUrl();
    }

    for (Iterator it = urlStrs.iterator(); it.hasNext();) {
      String url = (String)it.next();
      ActivityRegulator.Lock lock;

      if (url.equals(mainCusUrl)) {
        mainLock.setNewActivity(ActivityRegulator.REPAIR_CRAWL,
                                repairCrawlExpiration);
        lock = mainLock;
      } else {
        lock = ar.getCusActivityLock(createSingleNodeCachedUrlSet(au, url),
                                     ActivityRegulator.REPAIR_CRAWL,
                                     repairCrawlExpiration);
      }
      if (lock != null) {
        locks.put(url, lock);
        logger.debug3("Locked "+url);
      } else {
        logger.debug3("Couldn't lock "+url);
      }
    }
    return locks;
  }

  private static CachedUrlSet createSingleNodeCachedUrlSet(ArchivalUnit au,
							   String url) {
    return au.makeCachedUrlSet(new SingleNodeCachedUrlSetSpec(url));
  }

  public void startNewContentCrawl(ArchivalUnit au, CrawlManager.Callback cb,
                                   Object cookie, ActivityRegulator.Lock lock) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null AU");
    }
    // check rate limiter before obtaining lock
    RateLimiter limiter =
      getRateLimiter(au, newContentRateLimiters,
		     PARAM_MAX_NEW_CONTENT_CRAWLS_PER_INTERVAL,
		     DEFAULT_MAX_NEW_CONTENT_CRAWLS_PER_INTERVAL,
		     PARAM_MAX_NEW_CONTENT_CRAWLS_INTERVAL,
		     DEFAULT_MAX_NEW_CONTENT_CRAWLS_INTERVAL);
    if (!limiter.isEventOk()) {
      logger.debug("New content aborted due to rate limiter.");
      callCallback(cb, cookie, false, null);
      return;
    }
    if ((lock==null) || (lock.isExpired())) {
      lock = getNewContentLock(au);
    } else {
      lock.setNewActivity(ActivityRegulator.NEW_CONTENT_CRAWL,
                          contentCrawlExpiration);
    }
    if (lock == null) {
      logger.debug("Couldn't schedule new content crawl due "+
		   "to activity lock.");
      callCallback(cb, cookie, false, null);
      return;
    }
    limiter.event();
    try {
      scheduleNewContentCrawl(au, cb, cookie, lock);
    } catch (Exception e) {
      logger.error("scheduleNewContentCrawl threw", e);
    }
  }

  private ActivityRegulator.Lock getNewContentLock(ArchivalUnit au) {
    ActivityRegulator ar = theDaemon.getActivityRegulator(au);
    return ar.getAuActivityLock(ActivityRegulator.NEW_CONTENT_CRAWL,
			      contentCrawlExpiration);
  }

  public boolean shouldRecrawl(ArchivalUnit au, NodeState ns) {
    //XXX move to AU
    return false;
  }

  //method that calls the callback and catches any exception
  private static void callCallback(CrawlManager.Callback cb, Object cookie,
				   boolean successful, Crawler.Status status) {
    if (cb != null) {
      try {
	cb.signalCrawlAttemptCompleted(successful, null, cookie, status);
      } catch (Exception e) {
	logger.error("Callback threw", e);
      }
    }
  }

  private void scheduleNewContentCrawl(ArchivalUnit au,
                                       CrawlManager.Callback cb, Object cookie,
                                       ActivityRegulator.Lock lock) {
    CrawlThread crawlThread;
    try {
      CrawlSpec spec = au.getCrawlSpec();
      Crawler crawler = makeNewContentCrawler(au, spec);
      crawlThread = new CrawlThread(crawler, cb, cookie, SetUtil.set(lock));
      addToStatusList(crawler.getStatus());
//       crawlHistory.put(au.getAuId(), crawler.getStatus());
      synchronized (runningCrawls) {
        runningCrawls.put(au, crawler);
      }
      crawlThread.start();
    } catch (RuntimeException re) {
      logger.warning("Error starting crawl, freeing crawl lock", re);
      lock.expire();
      callCallback(cb, cookie, false, null);
      throw re;
    }
    crawlThread.waitRunning();
  }

  protected Crawler makeNewContentCrawler(ArchivalUnit au, CrawlSpec spec) {
    NodeManager nodeManager = theDaemon.getNodeManager(au);
    //check CrawlSpec if it is Oai Type then create OaiCrawler Instead of NewContentCrawler
    if (spec instanceof OaiCrawlSpec) {
      logger.debug("creating OaiCrawler : AU = " + au.toString());
      return new OaiCrawler(au, spec, nodeManager.getAuState());
    } else {
      logger.debug("creating NewContentCrawler : AU = " + au.toString());
      return new NewContentCrawler(au, spec, nodeManager.getAuState());
    }
  }

  protected Crawler makeRepairCrawler(ArchivalUnit au,
				      CrawlSpec spec,
				      Collection  repairUrls,
				      float percentRepairFromCache) {
    NodeManager nodeManager = theDaemon.getNodeManager(au);
    return new RepairCrawler(au, spec, nodeManager.getAuState(),
			     repairUrls, percentRepairFromCache);
  }

  public class CrawlThread extends LockssThread {
    private Object cookie;
    private Crawler crawler;
    private CrawlManager.Callback cb;
    private Collection locks;

    private CrawlThread(Crawler crawler, CrawlManager.Callback cb,
			Object cookie, Collection locks) {
      super(crawler.toString());
      this.cb = cb;
      this.cookie = cookie;
      this.crawler = crawler;
      this.locks = locks;
    }

    public void lockssRun() {
      //pull out of thread
      boolean crawlSuccessful = false;
      try {
	if (!crawlerEnabled) {
	  nowRunning();
	  try {
	    Deadline.in(Constants.HOUR).sleep();
	  } catch (InterruptedException e) {
	  }
	} else {
	  setPriority(PRIORITY_PARAM_CRAWLER, PRIORITY_DEFAULT_CRAWLER);
	  crawler.setWatchdog(this);
	  startWDog(WDOG_PARAM_CRAWLER, WDOG_DEFAULT_CRAWLER);
	  nowRunning();

	  crawlSuccessful = crawler.doCrawl();

	  if (crawler.isWholeAU()) {
	    if (crawlSuccessful) {
	      NodeManager nodeManager =
		theDaemon.getNodeManager(crawler.getAu());
	      nodeManager.newContentCrawlFinished();
	    }
	  }
	}
      } finally {
        // free all locks, regardless of exceptions
        if (locks != null) {
	  try {
	    Iterator lockIt = locks.iterator();
	    while (lockIt.hasNext()) {
	      // loop through expiring all locks
	      ActivityRegulator.Lock lock =
		(ActivityRegulator.Lock)lockIt.next();
	      lock.expire();
	    }
	  } catch (Exception e) {
	    logger.warning("Threw freeing locks", e);
	  }
        }
	synchronized(runningCrawls) {
	  runningCrawls.remove(crawler.getAu(), crawler);
	}
	callCallback(cb, cookie, crawlSuccessful, crawler.getStatus());
      }
    }
  }

  private static class FailingCallbackWrapper
    implements CrawlManager.Callback {
    CrawlManager.Callback cb;
    public FailingCallbackWrapper(CrawlManager.Callback cb) {
      this.cb = cb;
    }
    public void signalCrawlAttemptCompleted(boolean success, Set urlsFetched,
					    Object cookie,
					    Crawler.Status status) {
      callCallback(cb, cookie, false, null);
    }
  }

  //CrawlManager.StartSource methods
//   public Collection getActiveAus() {
//     return crawlHistory.keySet();
//   }

//   public Collection getCrawlStatus(String auid) {
//     Collection returnColl = (Collection)crawlHistory.get(auid);
//     return returnColl != null ? returnColl : Collections.EMPTY_LIST;
//   }

  private void addToStatusList(Crawler.Status status) {
    synchronized (crawlList) {
      crawlList.add(status);
    }
  }

  public List getCrawlStatusList() {
    synchronized (crawlList) {
      return new ArrayList(crawlList);
    }
  }

}
