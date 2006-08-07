/*
 * $Id: CrawlManagerImpl.java,v 1.97 2006-08-07 18:47:49 tlipkis Exp $
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
import org.apache.commons.collections.map.*;
import EDU.oswego.cs.dl.util.concurrent.*;
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

// ToDo:
// 1)handle background crawls
// 2)check for conflicting crawl types
// 3)check crawl schedule rules

public class CrawlManagerImpl extends BaseLockssDaemonManager
    implements CrawlManager, CrawlManager.StatusSource, ConfigurableManager {

  static Logger logger = Logger.getLogger("CrawlManager");

  public static final String PREFIX = Configuration.PREFIX + "crawler.";

  /**
   * The expiration deadline for a new content crawl, in ms.
   */
  public static final String PARAM_NEW_CONTENT_CRAWL_EXPIRATION =
    PREFIX + "new_content.expiration";
  private static final long DEFAULT_NEW_CONTENT_CRAWL_EXPIRATION =
    10 * Constants.DAY;

  /**
   * The expiration deadline for a repair crawl, in ms.
   */
  public static final String PARAM_REPAIR_CRAWL_EXPIRATION =
      PREFIX + "repair.expiration";
  private static final long DEFAULT_REPAIR_CRAWL_EXPIRATION =
    5 * Constants.DAY;

  public static final String PARAM_REPAIR_FROM_CACHE_PERCENT =
      PREFIX + "repair.repair_from_cache_percent";
  public static final float DEFAULT_REPAIR_FROM_CACHE_PERCENT = 0;

  /** Set false to prevent all crawl activity */
  public static final String PARAM_CRAWLER_ENABLED =
    PREFIX + "enabled";
  static final boolean DEFAULT_CRAWLER_ENABLED = true;

  /** Use thread pool and queue if true, start threads directly if false.
   * Only takes effect at startup. */
  public static final String PARAM_CRAWLER_QUEUE_ENABLED =
    PREFIX + "queue.enabled";
  static final boolean DEFAULT_CRAWLER_QUEUE_ENABLED = true;

  /** Max threads in crawler thread pool. */
  public static final String PARAM_CRAWLER_THREAD_POOL_MAX =
    PREFIX + "threadPool.max";
  static final int DEFAULT_CRAWLER_THREAD_POOL_MAX = 15;

  /** Max size of crawl queue, cannot be changed except at startup */
  public static final String PARAM_CRAWLER_THREAD_POOL_MAX_QUEUE_SIZE =
    PREFIX + "threadPool.maxQueueSize";
  static final int DEFAULT_CRAWLER_THREAD_POOL_MAX_QUEUE_SIZE = 200;

  /** Max number of queued crawls; can be changed on the fly up to the max
   * set by {@link #PARAM_CRAWLER_THREAD_POOL_MAX_QUEUE_SIZE} */
  public static final String PARAM_CRAWLER_THREAD_POOL_QUEUE_SIZE =
    PREFIX + "threadPool.queueSize";
  static final int DEFAULT_CRAWLER_THREAD_POOL_QUEUE_SIZE = 100;

  /** Duration after which idle threads will be terminated..  -1 = never */
  public static final String PARAM_CRAWLER_THREAD_POOL_KEEPALIVE =
    PREFIX + "threadPool.keepAlive";
  static final long DEFAULT_CRAWLER_THREAD_POOL_KEEPALIVE =
    2 * Constants.MINUTE;

  /** Interval at which we check AUs to see if they need a new content
   * crawl. */
  public static final String PARAM_START_CRAWLS_INTERVAL =
    PREFIX + "startCrawlsInterval";
  static final long DEFAULT_START_CRAWLS_INTERVAL = 1 * Constants.HOUR;

  /** Initial delay after AUs started before crawl starter first runs. */
  public static final String PARAM_START_CRAWLS_INITIAL_DELAY =
    PREFIX + "startCrawlsInitialDelay";
  static final long DEFAULT_START_CRAWLS_INITIAL_DELAY = 2 * Constants.MINUTE;

  /** Maximum rate at which we will start repair crawls for any particular
   * AU */
  public static final String PARAM_MAX_REPAIR_RATE =
    PREFIX + "maxRepairRate";
  public static final String DEFAULT_MAX_REPAIR_RATE = "50/1d";

  /** Maximum rate at which we will start new content crawls for any
   * particular AU */
  public static final String PARAM_MAX_NEW_CONTENT_RATE =
    PREFIX + "maxNewContentRate";
  public static final String DEFAULT_MAX_NEW_CONTENT_RATE = "1/18h";

  /** Maximum rate at which we will start any new content crawl, to keep
   * multiple crawls from starting at exactly the same time and all
   * fatching in synch.  Should be one event per less than a second,
   * relatively prime to fetch delay. */
  public static final String PARAM_NEW_CONTENT_START_RATE =
    PREFIX + "newContentStartRate";
  public static final String DEFAULT_NEW_CONTENT_START_RATE = "1/730";

  /** Number of most recent crawls for which status will be available.
   * This must be at larger than the thread pool + queue size or status
   * table will be incomplete.  */
  static final String PARAM_HISTORY_MAX =
    PREFIX + "historySize";
  static final int DEFAULT_HISTORY_MAX = 500;

  static final String WDOG_PARAM_CRAWLER = "Crawler";
  static final long WDOG_DEFAULT_CRAWLER = 2 * Constants.HOUR;

  static final String PRIORITY_PARAM_CRAWLER = "Crawler";
  static final int PRIORITY_DEFAULT_CRAWLER = Thread.NORM_PRIORITY - 1;

  private static final String CRAWL_STATUS_TABLE_NAME = "crawl_status_table";
  private static final String SINGLE_CRAWL_STATUS_TABLE_NAME =
    "single_crawl_status";

  private PluginManager pluginMgr;

  //Tracking crawls for the status info
  private CrawlManagerStatus cmStatus =
    new CrawlManagerStatus(DEFAULT_HISTORY_MAX);
  private MultiMap runningCrawls = new MultiValueMap();

  private long contentCrawlExpiration;
  private long repairCrawlExpiration;
  private float percentRepairFromCache;
  private boolean crawlerEnabled = DEFAULT_CRAWLER_ENABLED;
  private boolean paramQueueEnabled = DEFAULT_CRAWLER_QUEUE_ENABLED;
  private int paramMaxPoolSize = DEFAULT_CRAWLER_THREAD_POOL_MAX;
  private int paramPoolQueueSize = DEFAULT_CRAWLER_THREAD_POOL_QUEUE_SIZE;
  private int paramPoolMaxQueueSize =
    DEFAULT_CRAWLER_THREAD_POOL_MAX_QUEUE_SIZE;
  private long paramPoolKeepaliveTime = DEFAULT_CRAWLER_THREAD_POOL_KEEPALIVE;
  private long paramStartCrawlsInterval = DEFAULT_START_CRAWLS_INTERVAL;
  private long paramStartCrawlsInitialDelay =
    DEFAULT_START_CRAWLS_INITIAL_DELAY;
  private Map repairRateLimiters = new HashMap();
  private Map newContentRateLimiters = new HashMap();
  private RateLimiter newContentStartRateLimiter;

  private AuEventHandler auEventHandler;

  PooledExecutor pool;
  BoundedPriorityQueue poolQueue;

  /**
   * start the crawl manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    super.startService();

    pluginMgr = theDaemon.getPluginManager();
    StatusService statusServ = theDaemon.getStatusService();
    CrawlManagerStatusAccessor cmStatusAcc =
      new CrawlManagerStatusAccessor(this);
    statusServ.registerStatusAccessor(CRAWL_STATUS_TABLE_NAME, cmStatusAcc);
    statusServ.registerStatusAccessor(SINGLE_CRAWL_STATUS_TABLE_NAME,
				      new SingleCrawlStatus(cmStatusAcc));
    // register our AU event handler
    auEventHandler = new AuEventHandler.Base() {
	public void auDeleted(ArchivalUnit au) {
	  cancelAuCrawls(au);
	}};
    pluginMgr.registerAuEventHandler(auEventHandler);

    if (paramQueueEnabled) {
      poolQueue = new BoundedPriorityQueue(paramPoolQueueSize,
					   new CrawlQueueComparator());
      pool = new PooledExecutor(poolQueue, paramMaxPoolSize);
    } else {
      poolQueue = null;
      pool = new PooledExecutor(paramMaxPoolSize);
    }
    // Thread pool favors queueing once min threads exist, so must set min
    // threads equal to max threads
    pool.setMinimumPoolSize(paramMaxPoolSize);
    pool.setKeepAliveTime(paramPoolKeepaliveTime);
    pool.abortWhenBlocked();
    logger.debug2("Crawler thread pool min, max, queuelen: " +
		  pool.getMinimumPoolSize() + ", " +
		  pool.getMaximumPoolSize() + ", " +
		  (poolQueue != null ? poolQueue.capacity() : 0));
    if (paramStartCrawlsInterval > 0) {
      enableCrawlStarter();
    }
  }

  /**
   * stop the crawl manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    disableCrawlStarter();
    if (pool != null) {
      pool.shutdownNow();
    }
    if (auEventHandler != null) {
      pluginMgr.unregisterAuEventHandler(auEventHandler);
      auEventHandler = null;
    }
    // checkpoint here
    StatusService statusServ = theDaemon.getStatusService();
    if (statusServ != null) {
      statusServ.unregisterStatusAccessor(CRAWL_STATUS_TABLE_NAME);
    }
    super.stopService();
  }

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    if (changedKeys.contains(PREFIX)) {

      contentCrawlExpiration =
	config.getTimeInterval(PARAM_NEW_CONTENT_CRAWL_EXPIRATION,
			       DEFAULT_NEW_CONTENT_CRAWL_EXPIRATION);
      repairCrawlExpiration =
	config.getTimeInterval(PARAM_REPAIR_CRAWL_EXPIRATION,
			       DEFAULT_REPAIR_CRAWL_EXPIRATION);

      percentRepairFromCache =
	config.getPercentage(PARAM_REPAIR_FROM_CACHE_PERCENT,
			     DEFAULT_REPAIR_FROM_CACHE_PERCENT);
      crawlerEnabled =
	config.getBoolean(PARAM_CRAWLER_ENABLED,
			  DEFAULT_CRAWLER_ENABLED);

      paramQueueEnabled =
	config.getBoolean(PARAM_CRAWLER_QUEUE_ENABLED,
			  DEFAULT_CRAWLER_QUEUE_ENABLED);
      paramMaxPoolSize = config.getInt(PARAM_CRAWLER_THREAD_POOL_MAX,
				       DEFAULT_CRAWLER_THREAD_POOL_MAX);
      paramPoolKeepaliveTime =
	config.getTimeInterval(PARAM_CRAWLER_THREAD_POOL_KEEPALIVE,
			       DEFAULT_CRAWLER_THREAD_POOL_KEEPALIVE);
      if (pool != null) {
	pool.setMaximumPoolSize(paramMaxPoolSize);
	pool.setMinimumPoolSize(paramMaxPoolSize);
	pool.setKeepAliveTime(paramPoolKeepaliveTime);
      }
      paramPoolQueueSize =
	config.getInt(PARAM_CRAWLER_THREAD_POOL_QUEUE_SIZE,
		      DEFAULT_CRAWLER_THREAD_POOL_QUEUE_SIZE);
      paramPoolMaxQueueSize =
	config.getInt(PARAM_CRAWLER_THREAD_POOL_MAX_QUEUE_SIZE,
		      DEFAULT_CRAWLER_THREAD_POOL_MAX_QUEUE_SIZE);
      if (poolQueue != null && paramPoolQueueSize != poolQueue.capacity()) {
// 	poolQueue.setCapacity(paramPoolQueueSize);
      }

      paramStartCrawlsInitialDelay =
	config.getTimeInterval(PARAM_START_CRAWLS_INITIAL_DELAY,
			       DEFAULT_START_CRAWLS_INITIAL_DELAY);
      if (changedKeys.contains(PARAM_START_CRAWLS_INTERVAL)) {
	paramStartCrawlsInterval =
	  config.getTimeInterval(PARAM_START_CRAWLS_INTERVAL,
				 DEFAULT_START_CRAWLS_INTERVAL);
	if (paramStartCrawlsInterval > 0) {
	  if (theApp.isAppRunning()) {
	    enableCrawlStarter();
	  }
	} else {
	  disableCrawlStarter();
	}
      }

      if (changedKeys.contains(PARAM_MAX_REPAIR_RATE)) {
	resetRateLimiters(config, repairRateLimiters,
			  PARAM_MAX_REPAIR_RATE,
			  DEFAULT_MAX_REPAIR_RATE);
      }
      if (changedKeys.contains(PARAM_MAX_NEW_CONTENT_RATE)) {
	resetRateLimiters(config, newContentRateLimiters,
			  PARAM_MAX_NEW_CONTENT_RATE,
			  DEFAULT_MAX_NEW_CONTENT_RATE);
      }
      if (changedKeys.contains(PARAM_NEW_CONTENT_START_RATE)) {
	newContentStartRateLimiter =
	  RateLimiter.getConfiguredRateLimiter(config,
					       newContentStartRateLimiter,
					       PARAM_NEW_CONTENT_START_RATE,
					       DEFAULT_NEW_CONTENT_START_RATE);
      }
      if (changedKeys.contains(PARAM_HISTORY_MAX) ) {
	int cMax = config.getInt(PARAM_HISTORY_MAX, DEFAULT_HISTORY_MAX);
	cmStatus.setHistSize(cMax);
      }
    }
  }

  /**
   * Execute the runnable in a pool thread
   * @param run the Runnable to be run
   * @throws RuntimeException if no pool thread or queue space is available
   */
  protected void execute(Runnable run) {
    try {
      pool.execute(run);
      if (logger.isDebug3()) logger.debug3("Queued/started " + run);
    } catch (InterruptedException e) {
      logger.warning("Unexpectedly interrupted", e);
      throw new RuntimeException("Unexpectedly interrupted", e);
    }
  }

  private void addToRunningCrawls(ArchivalUnit au, Crawler crawler) {
    synchronized (runningCrawls) {
      runningCrawls.put(au, crawler);
    }
  }

  private void removeFromRunningCrawls(Crawler crawler) {
    if (crawler != null) {
      synchronized (runningCrawls) {
	runningCrawls.remove(crawler.getAu(), crawler);
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
				     String paramName,
				     String dfault) {
    RateLimiter limiter;
    synchronized (rateLimiterMap) {
      limiter = (RateLimiter)rateLimiterMap.get(au);
      if (limiter == null) {
	limiter =
	  RateLimiter.getConfiguredRateLimiter(ConfigManager.getCurrentConfig(),
					       null,
					       paramName, dfault);
	rateLimiterMap.put(au, limiter);
      }
    }
    return limiter;
  }

  /** Reset the parameters of all the rate limiters in the map. */
  private void resetRateLimiters(Configuration config,
				 Map rateLimiterMap,
				 String paramName,
				 String dfault) {
    synchronized (rateLimiterMap) {
      for (Iterator iter = rateLimiterMap.entrySet().iterator();
	   iter.hasNext(); ) {
	Map.Entry entry = (Map.Entry)iter.next();
	RateLimiter limiter = (RateLimiter)entry.getValue();
	RateLimiter newLimiter =
	  RateLimiter.getConfiguredRateLimiter(config, limiter,
					       paramName, dfault);
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
					 PARAM_MAX_REPAIR_RATE,
					 DEFAULT_MAX_REPAIR_RATE);
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
    Crawler crawler = null;
    try {
      if (locks.size() < urls.size()) {
	cb = new FailingCallbackWrapper(cb);
      }
      crawler = makeRepairCrawler(au, au.getCrawlSpec(),
				  locks.keySet(), percentRepairFromCache);
      CrawlRunner runner =
	new CrawlRunner(crawler, null, cb, cookie, locks.values(), limiter);
      addToStatusList(crawler.getStatus());
      addToRunningCrawls(au, crawler);
      new Thread(runner).start();
    } catch (RuntimeException re) {
      logger.error("Couldn't start repair crawl thread", re);
      logger.debug("Freeing repair locks...");
      Iterator lockIt = locks.values().iterator();
      while (lockIt.hasNext()) {
	ActivityRegulator.Lock deadLock =
	  (ActivityRegulator.Lock)lockIt.next();
	deadLock.expire();
      }
      lock.expire();
      removeFromRunningCrawls(crawler);
      callCallback(cb, cookie, false, null);
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
        if (logger.isDebug3()) logger.debug3("Locked "+url);
      } else {
        if (logger.isDebug3()) logger.debug3("Couldn't lock "+url);
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
    // check crawl window and rate limiter before obtaining lock
    CrawlSpec spec;
    try {
      spec = au.getCrawlSpec();
    } catch (RuntimeException e) {
      // not clear this can ever happen in real use, but some tests force
      // getCrawlSpec() to throw
      logger.error("Couldn't get CrawlSpec: " + au, e);
      callCallback(cb, cookie, false, null);
      return;
    }
    if (spec != null && !spec.inCrawlWindow()) {
      logger.debug("Not starting new content crawl due to crawl window: "
		   + au);
      callCallback(cb, cookie, false, null);
      return;
    }
    RateLimiter limiter =
      getRateLimiter(au, newContentRateLimiters,
		     PARAM_MAX_NEW_CONTENT_RATE,
		     DEFAULT_MAX_NEW_CONTENT_RATE);
    if (!limiter.isEventOk()) {
      logger.debug("Not starting new content crawl due to rate limiter: "
		   + au);
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
      logger.debug("Not starting new content crawl due to activity lock: "
		   + au);
      callCallback(cb, cookie, false, null);
      return;
    }
    Crawler crawler = null;
    try {
      crawler = makeNewContentCrawler(au, spec);
      CrawlRunner runner =
	new CrawlRunner(crawler, spec, cb, cookie, SetUtil.set(lock),
			limiter, newContentStartRateLimiter);
      // To avoid race, must add to running crawls before starting
      // execution
      addToRunningCrawls(au, crawler);
      execute(runner);
      // Add to status only if successfully queued or started.  (No race
      // here; appearance in status might be delayed.)
      addToStatusList(crawler.getStatus());
    } catch (RuntimeException e) {
      // thrown by pool if can't execute (pool & queue full, or poll full
      // and no queue)
      if (e.getMessage() != null &&
	  e.getMessage().endsWith("Pool is blocked")) {
	logger.warning("Couldn't start/schedule " + au + " crawl: " +
		       e.toString());
      } else {
	logger.warning("Couldn't start/schedule " + au + " crawl", e);
      }
      logger.debug("Freeing crawl lock");
      lock.expire();
      removeFromRunningCrawls(crawler);
      callCallback(cb, cookie, false, null);
    }
  }

  private ActivityRegulator.Lock getNewContentLock(ArchivalUnit au) {
    ActivityRegulator ar = theDaemon.getActivityRegulator(au);
    return ar.getAuActivityLock(ActivityRegulator.NEW_CONTENT_CRAWL,
			      contentCrawlExpiration);
  }

  //method that calls the callback and catches any exception
  private static void callCallback(CrawlManager.Callback cb, Object cookie,
				   boolean successful, Crawler.Status status) {
    if (cb != null) {
      try {
	cb.signalCrawlAttemptCompleted(successful, null, cookie, status);
      } catch (Exception e) {
	logger.error("Crawl callback threw", e);
      }
    }
  }

  protected Crawler makeNewContentCrawler(ArchivalUnit au, CrawlSpec spec) {
    //check CrawlSpec if it is Oai Type then create OaiCrawler Instead of NewContentCrawler
    if (spec instanceof OaiCrawlSpec) {
      logger.debug("Creating OaiCrawler for " + au);
      return new OaiCrawler(au, spec, AuUtil.getAuState(au));
    } else {
      logger.debug("Creating NewContentCrawler for " + au);
      return new NewContentCrawler(au, spec, AuUtil.getAuState(au));
    }
  }

  protected Crawler makeRepairCrawler(ArchivalUnit au,
				      CrawlSpec spec,
				      Collection  repairUrls,
				      float percentRepairFromCache) {
    return new RepairCrawler(au, spec, AuUtil.getAuState(au),
			     repairUrls, percentRepairFromCache);
  }

  private static int createIndex = 0;

  public class CrawlRunner extends LockssRunnable {
    private Object cookie;
    private Crawler crawler;
    private CrawlManager.Callback cb;
    private Collection locks;
    private RateLimiter auRateLimiter;
    private RateLimiter startRateLimiter;
    private CrawlSpec spec;
    private int sortOrder;

    private CrawlRunner(Crawler crawler, CrawlSpec spec,
			CrawlManager.Callback cb,
			Object cookie, Collection locks,
			RateLimiter auRateLimiter) {
      this(crawler, spec, cb, cookie, locks, auRateLimiter, null);
    }

    private CrawlRunner(Crawler crawler, CrawlSpec spec,
			CrawlManager.Callback cb,
			Object cookie, Collection locks,
			RateLimiter auRateLimiter,
			RateLimiter startRateLimiter) {
      super(crawler.toString());
      this.cb = cb;
      this.cookie = cookie;
      this.crawler = crawler;
      this.locks = locks;
      this.auRateLimiter = auRateLimiter;
      this.startRateLimiter = startRateLimiter;
      this.spec = spec;
      // queue in order created
      this.sortOrder = ++createIndex;
      if (crawler.getAu() instanceof RegistryArchivalUnit) {
	// except for registry AUs, which always come first
	sortOrder = -sortOrder;
      }
    }

    public Crawler getCrawler() {
      return crawler;
    }

    public int getSortOrder() {
      return sortOrder;
    }

    public void lockssRun() {
      //pull out of thread
      boolean crawlSuccessful = false;
      if (logger.isDebug3()) logger.debug3("Runner started");
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
	  // don't record event if crawl is going to abort immediately
	  if (spec == null || spec.inCrawlWindow()) {
	    if (auRateLimiter != null) {
	      auRateLimiter.event();
	    }
	  }
	  nowRunning();

	  if (startRateLimiter != null) {
	    // Use RateLimiter to ensure at least a small amount of time
	    // between crawl starts, so they don't start out doing their
	    // fetches in synch.  This imposes an arbitrary ordering on
	    // crawl threads that are ready to start.
	    synchronized (startRateLimiter) {
	      startRateLimiter.waitUntilEventOk();
	      instrumentBeforeStartRateLimiterEvent(crawler);
	      startRateLimiter.event();
	      if (logger.isDebug3()) {
		logger.debug3("Proceeding from start rate limiter");
	      }
	    }
	  }
	  crawlSuccessful = crawler.doCrawl();

	  if (crawler.isWholeAU()) {
	    if (crawlSuccessful) {
	      NodeManager nodeManager =
		theDaemon.getNodeManager(crawler.getAu());
	      nodeManager.newContentCrawlFinished();
	    }
	  }
	}
      } catch (InterruptedException ignore) {
	// no action
      } finally {
        // free all locks, regardless of exceptions
        if (locks != null) {
	  try {
	    for (Iterator lockIt = locks.iterator(); lockIt.hasNext(); ) {
	      ActivityRegulator.Lock lock =
		(ActivityRegulator.Lock)lockIt.next();
	      lock.expire();
	    }
	  } catch (RuntimeException e) {
	    logger.warning("Couldn't free locks", e);
	  }
        }
	removeFromRunningCrawls(crawler);
	cmStatus.incrFinished(crawlSuccessful);
	callCallback(cb, cookie, crawlSuccessful, crawler.getStatus());
      }
    }
  }

  // For testing only.  See TestCrawlManagerImpl
  protected void instrumentBeforeStartRateLimiterEvent(Crawler crawler) {
  }

  // Crawl starter thread.

  private CrawlStarter crawlStarter = null;
  private boolean isCrawlStarterEnabled = false;

  public void enableCrawlStarter() {
    if (crawlStarter != null) {
      logger.debug("Crawl starter already running; stopping old one first");
      disableCrawlStarter();
    }
    if (paramStartCrawlsInterval > 0) {
      logger.info("Starting crawl starter");
      crawlStarter = new CrawlStarter();
      new Thread(crawlStarter).start();
      isCrawlStarterEnabled = true;
    } else {
      logger.info("Crawl starter not enabled");
    }
  }

  public void disableCrawlStarter() {
    if (crawlStarter != null) {
      logger.info("Stopping crawl starter");
      crawlStarter.stopCrawlStarter();
      crawlStarter.waitExited(Deadline.in(Constants.SECOND));
      crawlStarter = null;
    }
    isCrawlStarterEnabled = false;
  }

  public boolean isCrawlStarterEnabled() {
    return isCrawlStarterEnabled;
  }

  /** Orders CrawlRunners according to the sort order they specify */
  static class CrawlQueueComparator implements Comparator {
    public int compare(Object a, Object b) {
      CrawlManagerImpl.CrawlRunner ra = (CrawlManagerImpl.CrawlRunner)a;
      CrawlManagerImpl.CrawlRunner rb = (CrawlManagerImpl.CrawlRunner)b;
      return ra.getSortOrder() - rb.getSortOrder();
    }
  }

  private class CrawlStarter extends LockssRunnable {
    private volatile boolean goOn = true;

    private CrawlStarter() {
      super("CrawlStarter");
    }

    public void lockssRun() {
      setPriority(PRIORITY_PARAM_CRAWLER, PRIORITY_DEFAULT_CRAWLER);
      // Crawl start interval is configurable, so watchdog timeout would
      // have to be also.  Crawl starter is so simple; not sure it really
      // needs a watchdog.
//       startWDog(WDOG_PARAM_CRAWL_STARTER, WDOG_DEFAULT_CRAWL_STARTER);
      triggerWDogOnExit(true);

      if (goOn) {
	try {
	  logger.debug("Waiting until AUs started");
	  theDaemon.waitUntilAusStarted();
	  Deadline initial = Deadline.in(paramStartCrawlsInitialDelay);
	  cmStatus.setNextCrawlStarter(initial);
	  initial.sleep();
	} catch (InterruptedException e) {
	  // just wakeup and check for exit
	}
      }
      while (goOn) {
// 	pokeWDog();
	startSomeCrawls();
	Deadline timer = Deadline.in(paramStartCrawlsInterval);
	cmStatus.setNextCrawlStarter(timer);
	if (goOn) {
	  try {
	    timer.sleep();
	  } catch (InterruptedException e) {
	    // just wakeup and check for exit
	  }
	}
      }
      if (!goOn) {
	triggerWDogOnExit(false);
      }
    }

    private void stopCrawlStarter() {
      goOn = false;
      interruptThread();
    }
  }

  // Each invocation of startSomeCrawls() tries to fill queue with AUs that
  // need a crawl.  The same random iterator is used across multiple
  // invocations to ensure we examine all AUs before starting over with a
  // new random order.

  private Iterator crawlStartIter = null;

  void startSomeCrawls() {
    if (poolQueue != null) {
      if (poolQueue.size() < paramPoolQueueSize) {
	logger.debug("Checking for AUs that need crawls");
	// get a new iterator if don't have one or if have exhausted
	// previous one
	if (crawlStartIter == null || !crawlStartIter.hasNext()) {
	  crawlStartIter = pluginMgr.getRandomizedAus().iterator();
	}
	for (Iterator iter = pluginMgr.getAllRegistryAus().iterator();
	     iter.hasNext() && poolQueue.size() < paramPoolQueueSize; ) {
	  ArchivalUnit au = (ArchivalUnit)iter.next();
	  possiblyStartCrawl(au);
	}
	while (crawlStartIter.hasNext() &&
	       poolQueue.size() < paramPoolQueueSize) {
	  ArchivalUnit au = (ArchivalUnit)crawlStartIter.next();
	  if (!pluginMgr.isInternalAu(au)) {
	    possiblyStartCrawl(au);
	  }
	}
      }
    }
  }

  void possiblyStartCrawl(ArchivalUnit au) {
    if (logger.isDebug3()) logger.debug3("checking au: " + au.getAuId());
    if (shouldCrawlForNewContent(au)) {
      CrawlManager.Callback rc = null;
      if (au instanceof RegistryArchivalUnit) {
	if (logger.isDebug3()) {
	  logger.debug3("Adding callback to registry AU: " + au.getName());
	}
	rc = new PluginManager.RegistryCallback(pluginMgr, au);
      }
      // Activity lock prevents AUs with pending crawls from being
      // queued twice.  If ActivityRegulator goes away some other
      // mechanism will be needed.
      startNewContentCrawl(au, rc, null, null);
    }
  }

  boolean shouldCrawlForNewContent(ArchivalUnit au) {
    return au.shouldCrawlForNewContent(AuUtil.getAuState(au));
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

  //CrawlManager.StatusSource methods

  private void addToStatusList(Crawler.Status status) {
    cmStatus.addCrawl(status);
  }

  public CrawlManagerStatus getStatus() {
    return cmStatus;
  }

}
