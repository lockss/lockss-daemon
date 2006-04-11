/*
 * $Id: CrawlManagerImpl.java,v 1.86 2006-04-11 08:33:33 tlipkis Exp $
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

  private static Logger logger = Logger.getLogger("CrawlManager");

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

  /** Min threads in crawler thread pool. */
  public static final String PARAM_CRAWLER_THREAD_POOL_MIN =
    PREFIX + "threadPool.min";
  static final int DEFAULT_CRAWLER_THREAD_POOL_MIN = 15;

  /** Max threads in crawler thread pool. */
  public static final String PARAM_CRAWLER_THREAD_POOL_MAX =
    PREFIX + "threadPool.max";
  static final int DEFAULT_CRAWLER_THREAD_POOL_MAX = 15;

  /** Max number of queued crawls.  Can be changed on the fly */
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

  /** Interval at which we check AUs to see if they need a new content
   * crawl. */
  public static final String PARAM_START_CRAWLS_INITIAL_DELAY =
    PREFIX + "startCrawlsInitialDelay";
  static final long DEFAULT_START_CRAWLS_INITIAL_DELAY = 2 * Constants.MINUTE;

  public static final String PARAM_MAX_REPAIR_RATE =
    PREFIX + "maxRepairRate";
  public static final String DEFAULT_MAX_REPAIR_RATE = "50/1d";

  public static final String PARAM_MAX_NEW_CONTENT_RATE =
    PREFIX + "maxNewContentRate";
  public static final String DEFAULT_MAX_NEW_CONTENT_RATE = "1/18h";

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
  private MultiMap runningCrawls = new MultiHashMap();

  private long contentCrawlExpiration;
  private long repairCrawlExpiration;
  private float percentRepairFromCache;
  private boolean crawlerEnabled = DEFAULT_CRAWLER_ENABLED;
  private boolean paramQueueEnabled = DEFAULT_CRAWLER_QUEUE_ENABLED;
  private int paramMinPoolSize = DEFAULT_CRAWLER_THREAD_POOL_MIN;
  private int paramMaxPoolSize = DEFAULT_CRAWLER_THREAD_POOL_MAX;
  private int paramPoolQueueSize = DEFAULT_CRAWLER_THREAD_POOL_QUEUE_SIZE;
  private long paramPoolKeepaliveTime = DEFAULT_CRAWLER_THREAD_POOL_KEEPALIVE;
  private long paramStartCrawlsInterval = DEFAULT_START_CRAWLS_INTERVAL;
  private long paramStartCrawlsInitialDelay =
    DEFAULT_START_CRAWLS_INITIAL_DELAY;
  private Map repairRateLimiters = new HashMap();
  private Map newContentRateLimiters = new HashMap();
  private AuEventHandler auEventHandler;

  PooledExecutor pool;
  BoundedLinkedQueue poolQueue;

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
      poolQueue = new BoundedLinkedQueue(paramPoolQueueSize);
      pool = new PooledExecutor(poolQueue, paramMaxPoolSize);
    } else {
      poolQueue = null;
      pool = new PooledExecutor(paramMaxPoolSize);
    }
    pool.setMinimumPoolSize(paramMinPoolSize);
    pool.setKeepAliveTime(paramPoolKeepaliveTime);
    pool.abortWhenBlocked();
    logger.debug2("Crawler thread pool min, max, queuelen: " +
		  pool.getMinimumPoolSize() + ", " +
		  pool.getMaximumPoolSize() + ", " +
		  (poolQueue != null ? poolQueue.capacity() : 0));
  }

  /**
   * stop the crawl manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
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
      paramMinPoolSize = config.getInt(PARAM_CRAWLER_THREAD_POOL_MIN,
				       paramMaxPoolSize);
      paramPoolKeepaliveTime =
	config.getTimeInterval(PARAM_CRAWLER_THREAD_POOL_KEEPALIVE,
			       DEFAULT_CRAWLER_THREAD_POOL_KEEPALIVE);
      if (pool != null) {
	pool.setMaximumPoolSize(paramMaxPoolSize);
	pool.setMinimumPoolSize(paramMinPoolSize);
	pool.setKeepAliveTime(paramPoolKeepaliveTime);
      }
      paramPoolQueueSize =
	config.getInt(PARAM_CRAWLER_THREAD_POOL_QUEUE_SIZE,
		      DEFAULT_CRAWLER_THREAD_POOL_QUEUE_SIZE);
      if (poolQueue != null && paramPoolQueueSize != poolQueue.capacity()) {
	poolQueue.setCapacity(paramPoolQueueSize);
      }

      paramStartCrawlsInitialDelay =
	config.getTimeInterval(PARAM_START_CRAWLS_INITIAL_DELAY,
			       DEFAULT_START_CRAWLS_INITIAL_DELAY);
      if (changedKeys.contains(PARAM_START_CRAWLS_INTERVAL)) {
	paramStartCrawlsInterval =
	  config.getTimeInterval(PARAM_START_CRAWLS_INTERVAL,
				 DEFAULT_START_CRAWLS_INTERVAL);
	if (paramStartCrawlsInterval > 0) {
	  if (isCrawlStarterEnabled && theApp.isAppRunning()) {
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
      if (changedKeys.contains(PARAM_HISTORY_MAX) ) {
	int cMax = config.getInt(PARAM_HISTORY_MAX, DEFAULT_HISTORY_MAX);
	cmStatus.setHistSize(cMax);
      }
    }
  }

  /**
   * Execute the runnable in a pool thread
   * @param run the Runnable to be run
   * @throws InterruptedException
   * @throws RuntimeException if no pool thread or queue space is available
   */
  void execute(Runnable run) {
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
	new CrawlRunner(crawler, cb, cookie, locks.values(), limiter);
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
		     PARAM_MAX_NEW_CONTENT_RATE,
		     DEFAULT_MAX_NEW_CONTENT_RATE);
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
    Crawler crawler = null;
    try {
      CrawlSpec spec = au.getCrawlSpec();
      crawler = makeNewContentCrawler(au, spec);
      CrawlRunner runner =
	new CrawlRunner(crawler, cb, cookie, SetUtil.set(lock), limiter);
      addToStatusList(crawler.getStatus());
      addToRunningCrawls(au, crawler);
      execute(runner);
    } catch (RuntimeException e) {
      logger.warning("Couldn't start/schedule " + au + " crawl: " +
		     e.toString());
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
	logger.error("Callback threw", e);
      }
    }
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

  public class CrawlRunner extends LockssRunnable {
    private Object cookie;
    private Crawler crawler;
    private CrawlManager.Callback cb;
    private Collection locks;
    private RateLimiter limiter;

    private CrawlRunner(Crawler crawler, CrawlManager.Callback cb,
			Object cookie, Collection locks, RateLimiter limiter) {
      super(crawler.toString());
      this.cb = cb;
      this.cookie = cookie;
      this.crawler = crawler;
      this.locks = locks;
      this.limiter = limiter;
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
	  if (limiter != null) {
	    limiter.event();
	  }
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
	removeFromRunningCrawls(crawler);
	cmStatus.incrFinished(crawlSuccessful);
	callCallback(cb, cookie, crawlSuccessful, crawler.getStatus());
      }
    }
  }

  // Crawl starter thread.

  private CrawlStarter crawlStarter = null;
  private boolean isCrawlStarterEnabled = false;

  public void enableCrawlStarter() {
    if (crawlStarter != null) {
      logger.warning("Crawl starter already running; stopping old one first");
      disableCrawlStarter();
    } else {
      logger.info("Starting crawl starter");
    }
    if (paramStartCrawlsInterval > 0) {
      crawlStarter = new CrawlStarter();
      new Thread(crawlStarter).start();
      isCrawlStarterEnabled = true;
    }
  }

  public void disableCrawlStarter() {
    if (crawlStarter != null) {
      logger.info("Stopping crawl starter");
      crawlStarter.stopCrawlStarter();
      crawlStarter = null;
    }
    isCrawlStarterEnabled = false;
  }

  private class CrawlStarter extends LockssRunnable {
    private boolean goOn = false;
    private Deadline timer;

    private CrawlStarter() {
      super("CrawlStarter");
    }

    public void lockssRun() {
      setPriority(PRIORITY_PARAM_CRAWLER, PRIORITY_DEFAULT_CRAWLER);
      goOn = true;
//       startWDog(WDOG_PARAM_CRAWL_STARTER, WDOG_DEFAULT_CRAWL_STARTER);
//       triggerWDogOnExit(true);

      try {
	Deadline.in(paramStartCrawlsInitialDelay).sleep();
      } catch (InterruptedException e) {
	// just wakeup and check for exit
      }
      while (goOn) {
// 	pokeWDog();
	startSomeCrawls();
	timer = Deadline.in(paramStartCrawlsInterval);
	if (goOn) {
	  try {
	    timer.sleep();
	  } catch (InterruptedException e) {
	    // just wakeup and check for exit
	  }
	}
      }
    }

    private void stopCrawlStarter() {
      goOn = false;
      interruptThread();
    }

    void forceRun() {
      if (timer != null) {
	timer.expire();
      }
    }
  }

  void startSomeCrawls() {
    if (poolQueue != null) {
      int n = poolQueue.capacity() - poolQueue.size();
      if (poolQueue.capacity() > poolQueue.size()) {
	logger.debug("Checking for AUs that need crawls");
	List aus = pluginMgr.getAllAus();
	List randAus = CollectionUtil.randomPermutation(aus);
	for (Iterator iter = randAus.iterator(); iter.hasNext(); ) {
	  ArchivalUnit au = (ArchivalUnit)iter.next();
	  if (shouldCrawlForNewContent(au)) {
	    CrawlManager.Callback rc = null;
	    if (au instanceof RegistryArchivalUnit) {
	      logger.debug("AU " + au.getName() +
			   " is a registry, adding callback.");
	      rc = new PluginManager.RegistryCallback(pluginMgr, au);
	    }
	    startNewContentCrawl(au, null, null, null);
	  }
	  if (poolQueue.capacity() == poolQueue.size()) {
	    break;
	  }
	}
      }
    }
  }

  boolean shouldCrawlForNewContent(ArchivalUnit au) {
    NodeManager mgr = theDaemon.getNodeManager(au);
    return au.shouldCrawlForNewContent(mgr.getAuState());
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
