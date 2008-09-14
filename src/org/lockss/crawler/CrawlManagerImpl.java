/*
 * $Id: CrawlManagerImpl.java,v 1.123 2008-09-14 06:04:18 tlipkis Exp $
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
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.collections.*;
import org.apache.commons.collections.map.*;
import org.apache.commons.collections.bag.HashBag; // needed to disambiguate
import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.oro.text.regex.*;
import EDU.oswego.cs.dl.util.concurrent.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.state.NodeState;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.alert.*;
import org.lockss.state.*;
import org.lockss.plugin.*;
import org.lockss.plugin.exploded.*;

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

  /** Thread pool on-demand choice mode.  If true, crawl starter thread
      blocks in execute until a thread is ready, then chooses the best next
      crawl.  Only takes effect at startup. */
  public static final String PARAM_USE_ODC = PREFIX + "threadPool.onDemand";
  static final boolean DEFAULT_USE_ODC = true;

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

  // ODC params

  static String ODC_PREFIX = PREFIX + "odc.";

  /** Max interval between recalculating crawl queue order */
  public static final String PARAM_REBUILD_CRAWL_QUEUE_INTERVAL =
    ODC_PREFIX + "queueRecalcInterval";
  static final long DEFAULT_REBUILD_CRAWL_QUEUE_INTERVAL = Constants.HOUR;

  /** Interval after new AU creation to recalc queue.  Should be large
   * enough that it only happens once when a batch of AUs is finished. */
  public static final String PARAM_QUEUE_RECALC_AFTER_NEW_AU =
    ODC_PREFIX + "queueRecalcAfterNewAu";
  static final long DEFAULT_QUEUE_RECALC_AFTER_NEW_AU = 1 * Constants.MINUTE;

  /** Interval to sleep when queue empty, before recalc. */
  public static final String PARAM_QUEUE_EMPTY_SLEEP =
    ODC_PREFIX + "queueEmptySleep";
  static final long DEFAULT_QUEUE_EMPTY_SLEEP = 15 * Constants.MINUTE;

  /** Size of queue of unshared rate AUs. */
  public static final String PARAM_UNSHARED_QUEUE_MAX =
    ODC_PREFIX + "unsharedQueueMax";
  static final int DEFAULT_UNSHARED_QUEUE_MAX = 5;

  /** Size of queue of shared rate AUs. */
  public static final String PARAM_SHARED_QUEUE_MAX =
    ODC_PREFIX + "sharedQueueMax";
  static final int DEFAULT_SHARED_QUEUE_MAX = 5;

  /** Min number of threads available to AUs with unshared rate limiters */
  public static final String PARAM_FAVOR_SHARED_RATE_THREADS =
    ODC_PREFIX + "favorSharedRateThreads";
  static final int DEFAULT_FAVOR_SHARED_RATE_THREADS =
    DEFAULT_CRAWLER_THREAD_POOL_MAX - 1;

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

  /** Don't start crawl if window will close before this interval */
  public static final String PARAM_MIN_WINDOW_OPEN_FOR =
    PREFIX + "minWindowOpenFor";
  public static final long DEFAULT_MIN_WINDOW_OPEN_FOR = 15 * Constants.MINUTE;

  /** If true, give priority to crawls that were running when daemon died */
  public static final String PARAM_RESTART_AFTER_CRASH =
  PREFIX + "restartAfterCrash";
  public static final boolean DEFAULT_RESTART_AFTER_CRASH = true;

  /** Number of most recent crawls for which status will be available.
   * This must be larger than the thread pool + queue size or status table
   * will be incomplete.  */
  static final String PARAM_HISTORY_MAX =
    PREFIX + "historySize";
  static final int DEFAULT_HISTORY_MAX = 500;

  /** Regexp matching URLs we never want to collect.  Intended to stop
   * runaway crawls by catching recursive URLS */
  static final String PARAM_EXCLUDE_URL_PATTERN =
    PREFIX + "globallyExcludedUrlPattern";
  static final String DEFAULT_EXCLUDE_URL_PATTERN = null;

  static final String WDOG_PARAM_CRAWLER = "Crawler";
  static final long WDOG_DEFAULT_CRAWLER = 2 * Constants.HOUR;

  static final String PRIORITY_PARAM_CRAWLER = "Crawler";
  static final int PRIORITY_DEFAULT_CRAWLER = Thread.NORM_PRIORITY - 1;

  public static final String CRAWL_STATUS_TABLE_NAME = "crawl_status_table";
  public static final String CRAWL_URLS_STATUS_TABLE =
                                    "crawl_urls";
  public static final String SINGLE_CRAWL_STATUS_TABLE =
                                    "single_crawl_status_table";

  private PluginManager pluginMgr;
  private AlertManager alertMgr;

  //Tracking crawls for the status info
  private CrawlManagerStatus cmStatus;
  // Map AU to all active crawlers (new content and repair)
  private MultiMap runningCrawls = new MultiValueMap();
  // AUs running new content crawls
  private Set runningNCCrawls = new HashSet();
  protected Bag runningRateKeys = new HashBag();

  private long contentCrawlExpiration;
  private long repairCrawlExpiration;
  private float percentRepairFromCache;
  private boolean crawlerEnabled = DEFAULT_CRAWLER_ENABLED;
  private boolean paramQueueEnabled = DEFAULT_CRAWLER_QUEUE_ENABLED;
  private int paramMaxPoolSize = DEFAULT_CRAWLER_THREAD_POOL_MAX;
  private boolean paramOdc = DEFAULT_USE_ODC;
  private int paramPoolQueueSize = DEFAULT_CRAWLER_THREAD_POOL_QUEUE_SIZE;
  private int paramPoolMaxQueueSize =
    DEFAULT_CRAWLER_THREAD_POOL_MAX_QUEUE_SIZE;
  private long paramPoolKeepaliveTime = DEFAULT_CRAWLER_THREAD_POOL_KEEPALIVE;
  private long paramStartCrawlsInterval = DEFAULT_START_CRAWLS_INTERVAL;
  private long paramStartCrawlsInitialDelay =
    DEFAULT_START_CRAWLS_INITIAL_DELAY;
  private long paramMinWindowOpenFor = DEFAULT_MIN_WINDOW_OPEN_FOR;
  private boolean paramRestartAfterCrash = DEFAULT_RESTART_AFTER_CRASH;
  private Pattern globallyExcludedUrlPattern;

  private int histSize = DEFAULT_HISTORY_MAX;

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

    paramOdc = CurrentConfig.getBooleanParam(PARAM_USE_ODC, DEFAULT_USE_ODC);

    cmStatus = new CrawlManagerStatus(histSize);
    cmStatus.setOdc(paramOdc);

    pluginMgr = getDaemon().getPluginManager();
    alertMgr = getDaemon().getAlertManager();
    StatusService statusServ = getDaemon().getStatusService();
    statusServ.registerStatusAccessor(CRAWL_STATUS_TABLE_NAME,
				      new CrawlManagerStatusAccessor(this));
    statusServ.registerOverviewAccessor(CRAWL_STATUS_TABLE_NAME,
					new CrawlManagerStatusAccessor.CrawlOverview(this));
    statusServ.registerStatusAccessor(CRAWL_URLS_STATUS_TABLE,
				      new CrawlUrlsStatusAccessor(this));
    statusServ.registerStatusAccessor(SINGLE_CRAWL_STATUS_TABLE,
                                      new SingleCrawlStatusAccessor(this));     
    // register our AU event handler
    auEventHandler = new AuEventHandler.Base() {
	public void auDeleted(ArchivalUnit au) {
	  cancelAuCrawls(au);
	}
	public void auCreated(ArchivalUnit au) {
	  rebuildQueueSoon();
	}
      };
    pluginMgr.registerAuEventHandler(auEventHandler);

    if (!paramOdc && paramQueueEnabled) {
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
    if (paramOdc) {
      pool.waitWhenBlocked();
    } else {
      pool.abortWhenBlocked();
    }
    logger.debug2("Crawler thread pool min, max, queuelen: " +
		  pool.getMinimumPoolSize() + ", " +
		  pool.getMaximumPoolSize() + ", " +
		  (poolQueue != null ? poolQueue.capacity() : 0));
    if (paramOdc || paramStartCrawlsInterval > 0) {
      enableCrawlStarter();
    }
  }

  /**
   * stop the crawl manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    shuttingDown = true;
    disableCrawlStarter();
    if (pool != null) {
      pool.shutdownNow();
    }
    if (auEventHandler != null) {
      pluginMgr.unregisterAuEventHandler(auEventHandler);
      auEventHandler = null;
    }
    // checkpoint here
    StatusService statusServ = getDaemon().getStatusService();
    if (statusServ != null) {
      statusServ.unregisterStatusAccessor(CRAWL_STATUS_TABLE_NAME);
      statusServ.unregisterOverviewAccessor(CRAWL_STATUS_TABLE_NAME);
      statusServ.unregisterStatusAccessor(CRAWL_URLS_STATUS_TABLE);
      statusServ.unregisterStatusAccessor(SINGLE_CRAWL_STATUS_TABLE);
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

      paramQueueEmptySleep = config.getTimeInterval(PARAM_QUEUE_EMPTY_SLEEP,
						    DEFAULT_QUEUE_EMPTY_SLEEP);
      paramQueueRecalcAfterNewAu =
	config.getTimeInterval(PARAM_QUEUE_RECALC_AFTER_NEW_AU,
			       DEFAULT_QUEUE_RECALC_AFTER_NEW_AU);

      paramUnsharedQueueMax = config.getInt(PARAM_UNSHARED_QUEUE_MAX,
					    DEFAULT_UNSHARED_QUEUE_MAX);
      paramSharedQueueMax = config.getInt(PARAM_SHARED_QUEUE_MAX,
					  DEFAULT_SHARED_QUEUE_MAX);
      unsharedRateReqs.setMaxSize(paramUnsharedQueueMax);
      sharedRateReqs.setTreeSetSize(paramSharedQueueMax);

      paramFavorSharedRateThreads =
	config.getInt(PARAM_FAVOR_SHARED_RATE_THREADS,
		      DEFAULT_FAVOR_SHARED_RATE_THREADS);

      paramRebuildCrawlQueueInterval =
	config.getTimeInterval(PARAM_REBUILD_CRAWL_QUEUE_INTERVAL,
			       DEFAULT_REBUILD_CRAWL_QUEUE_INTERVAL);
      paramMinWindowOpenFor =
	config.getTimeInterval(PARAM_MIN_WINDOW_OPEN_FOR,
			       DEFAULT_MIN_WINDOW_OPEN_FOR);
      paramRestartAfterCrash =
	config.getBoolean(PARAM_RESTART_AFTER_CRASH,
			  DEFAULT_RESTART_AFTER_CRASH);
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

      if (changedKeys.contains(PARAM_EXCLUDE_URL_PATTERN)) {
	setExcludedUrlPattern(config.get(PARAM_EXCLUDE_URL_PATTERN,
					 DEFAULT_EXCLUDE_URL_PATTERN),
			      DEFAULT_EXCLUDE_URL_PATTERN);
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
	histSize = config.getInt(PARAM_HISTORY_MAX, DEFAULT_HISTORY_MAX);
	if (cmStatus != null) {
	  cmStatus.setHistSize(histSize);
	}
      }
    }
  }

  public boolean isCrawlerEnabled() {
    return crawlerEnabled;
  }

  public boolean isGloballyExcludedUrl(ArchivalUnit au, String url) {
    if (globallyExcludedUrlPattern == null) {
      return false;
    }
    boolean isExcluded =
      RegexpUtil.getMatcher().contains(url, globallyExcludedUrlPattern);
    if (isExcluded) {
      String msg = "URL excluded (possible recursion): " + url;
      logger.siteWarning(msg);
      if (alertMgr != null) {
	alertMgr.raiseAlert(Alert.auAlert(Alert.CRAWL_EXCLUDED_URL, au), msg);
      }
    }
    return isExcluded;
  }

  void setExcludedUrlPattern(String pat, String defaultPat) {
    if (pat != null) {
      int flags =
	Perl5Compiler.READ_ONLY_MASK | Perl5Compiler.CASE_INSENSITIVE_MASK;
      try {
	globallyExcludedUrlPattern = RegexpUtil.getCompiler().compile(pat,
								      flags);
	logger.info("Global exclude pattern: " + pat);
	return;
      } catch (MalformedPatternException e) {
	logger.error("Illegal global exclude pattern: " + pat, e);
	if (defaultPat != null && !defaultPat.equals(pat)) {
	  try {
	    globallyExcludedUrlPattern =
	      RegexpUtil.getCompiler().compile(defaultPat, flags);
	    logger.info("Using default global exclude pattern: " + defaultPat);
	    return;
	  } catch (MalformedPatternException e2) {
	    logger.error("Illegal default global exclude pattern: "
			 + defaultPat,
			 e2);
	  }
	}
      }
    }
    globallyExcludedUrlPattern = null;
    logger.debug("No global exclude pattern");
  }


  static final Runnable NULL_RUNNER = new Runnable() {
      public void run() {}
    };

  /**
   * Execute the runnable in a pool thread
   * @param run the Runnable to be run
   * @throws RuntimeException if no pool thread or queue space is available
   */
  protected void execute(Runnable run) throws InterruptedException {
    pool.execute(run);
    if (logger.isDebug3()) logger.debug3("Queued/started " + run);
    if (paramOdc) {
      if (logger.isDebug3()) logger.debug3("waiting");
      pool.execute(NULL_RUNNER);
      if (logger.isDebug3()) logger.debug3("waited");
    }
  }

  private void addToRunningCrawls(ArchivalUnit au, Crawler crawler) {
    synchronized (runningCrawls) {
      runningCrawls.put(au, crawler);
      if (crawler.isWholeAU()) {
	runningNCCrawls.add(au);
	cmStatus.setRunningNCCrawls(new ArrayList(runningNCCrawls));
	Object key = au.getFetchRateLimiterKey();
	if (key != null) {
	  runningRateKeys.add(key);
	}
      }      
    }
    synchronized (highPriorityCrawlRequests) {
      highPriorityCrawlRequests.remove(au);
    }
  }

  private void removeFromRunningCrawls(Crawler crawler) {
    if (crawler != null) {
      ArchivalUnit au = crawler.getAu();
      synchronized (runningCrawls) {
	runningCrawls.remove(au, crawler);
	if (crawler.isWholeAU()) {
	  runningNCCrawls.remove(au);
	  cmStatus.setRunningNCCrawls(new ArrayList(runningNCCrawls));
	  Object key = au.getFetchRateLimiterKey();
	  if (key != null) {
	    runningRateKeys.remove(key);
	    startOneWait.expire();
	  }
	}      
      }
    }
  }

  void cancelAuCrawls(ArchivalUnit au) {
    removeAuFromQueues(au);
    synchronized(runningCrawls) {
      Collection<Crawler> crawls = (Collection) runningCrawls.get(au);
      if (crawls != null) {
	for (Crawler crawler : crawls) {
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

  public RateLimiter getNewContentRateLimiter(ArchivalUnit au) {
    if (pluginMgr.isInternalAu(au)) return null;
    return getRateLimiter(au, newContentRateLimiters,
			  PARAM_MAX_NEW_CONTENT_RATE,
			  DEFAULT_MAX_NEW_CONTENT_RATE);
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
      cmStatus.addCrawlStatus(crawler.getStatus());
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
    ActivityRegulator ar = getDaemon().getActivityRegulator(au);
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

  private boolean isRunningNCCrawl(ArchivalUnit au) {
    synchronized (runningCrawls) {
      return runningNCCrawls.contains(au);
    }
  }

  public boolean isEligibleForNewContentCrawl(ArchivalUnit au) {
    if (isRunningNCCrawl(au)) {
      return false;
    }
    if (au instanceof ExplodedArchivalUnit) {
      logger.debug("Can't crawl ExplodedArchivalUnit");
      return false;
    }
    CrawlSpec spec;
    try {
      spec = au.getCrawlSpec();
    } catch (RuntimeException e) {
      // not clear this can ever happen in real use, but some tests force
      // getCrawlSpec() to throw
      logger.error("Couldn't get CrawlSpec: " + au, e);
      return false;
    }
    if (spec != null && !windowOkToStart(spec.getCrawlWindow())) {
      logger.debug3("Not crawlable: crawl window: " + au);
      return false;
    }
    RateLimiter limiter = getNewContentRateLimiter(au);
    if (limiter != null && !limiter.isEventOk()) {
      logger.debug3("Not crawlable: rate limiter: " + au);
      return false;
    }
    return true;
  }

  public void startNewContentCrawl(ArchivalUnit au, CrawlManager.Callback cb,
				   Object cookie, ActivityRegulator.Lock lock) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null AU");
    }
    if (!crawlerEnabled) {
      logger.warning("Crawler disabled, not crawling: " + au);
      callCallback(cb, cookie, false, null);
      return;
    }
    CrawlReq req;
    try {
      req = new CrawlReq(au, cb, cookie, lock);
      req.setPriority(1);
    } catch (RuntimeException e) {
      logger.error("Couldn't create CrawlReq: " + au, e);
      callCallback(cb, cookie, false, null);
      return;
    }
    if (paramOdc) {
      enqueueHighPriorityCrawl(req);
    } else {
      handReqToPool(req);
    }
  }

  void handReqToPool(CrawlReq req) {
    ArchivalUnit au = req.au;
    CrawlManager.Callback cb = req.cb;
    Object cookie = req.cookie;
    ActivityRegulator.Lock lock = req.lock;

    if (!isEligibleForNewContentCrawl(au)) {
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
    CrawlSpec spec = au.getCrawlSpec();
    Crawler crawler = null;
    CrawlRunner runner = null;
    try {
      crawler = makeNewContentCrawler(au, spec);
      runner = new CrawlRunner(crawler, spec, cb, cookie, SetUtil.set(lock),
			       getNewContentRateLimiter(au),
			       newContentStartRateLimiter);
      // To avoid race, must add to running crawls before starting
      // execution
      addToRunningCrawls(au, crawler);
      if (paramOdc) {
	// Add status first.  execute might not return for a long time, and
	// we're expecting this crawl to be accepted.
	cmStatus.addCrawlStatus(crawler.getStatus());
	execute(runner);
	return;
      } else {
	// Add to status only if successfully queued or started.  (No
	// race here; appearance in status might be delayed.)
	execute(runner);
	cmStatus.addCrawlStatus(crawler.getStatus());
	return;
      }
    } catch (InterruptedException e) {
      if (!isShuttingDown()) {
	// thrown by pool if can't execute (pool & queue full, or pool full
	// and no queue.  In on-demand mode should throw only on shutdown.)
	String crawlerRunner =
	  (crawler == null ? "no crawler" : crawler.toString()) + " " +
	  (runner == null ? "no runner" : runner.toString());
	if (e.getMessage() != null &&
	    e.getMessage().endsWith("Pool is blocked")) {
	  logger.warning("Couldn't start/schedule " + au + " crawl: " +
			 e.toString() + " " + crawlerRunner);
	} else {
	  logger.warning("Couldn't start/schedule " + au + " crawl"  + " " +
			 crawlerRunner, e);
	}
      }
      logger.debug2("Freeing crawl lock");
      lock.expire();
      removeFromRunningCrawls(crawler);
      callCallback(cb, cookie, false, null);
      return;
    } catch (RuntimeException e) {
      String crawlerRunner =
	(crawler == null ? "no crawler" : crawler.toString()) + " " +
	(runner == null ? "no runner" : runner.toString());
      logger.error("Unexpected error attempting to start/schedule " + au +
		   " crawl"  + " " + crawlerRunner, e);
      logger.debug2("Freeing crawl lock");
      lock.expire();
      removeFromRunningCrawls(crawler);
      callCallback(cb, cookie, false, null);
      return;
    }
  }

  private ActivityRegulator.Lock getNewContentLock(ArchivalUnit au) {
    ActivityRegulator ar = getDaemon().getActivityRegulator(au);
    return ar.getAuActivityLock(ActivityRegulator.NEW_CONTENT_CRAWL,
			      contentCrawlExpiration);
  }

  //method that calls the callback and catches any exception
  private static void callCallback(CrawlManager.Callback cb, Object cookie,
				   boolean successful, CrawlerStatus status) {
    if (cb != null) {
      try {
	cb.signalCrawlAttemptCompleted(successful, cookie, status);
      } catch (Exception e) {
	logger.error("Crawl callback threw", e);
      }
    }
  }

  protected Crawler makeNewContentCrawler(ArchivalUnit au, CrawlSpec spec) {
    //check CrawlSpec if it is Oai Type then create OaiCrawler Instead of NewContentCrawler
    if (spec instanceof OaiCrawlSpec) {
      logger.debug("Creating OaiCrawler for " + au);
      OaiCrawler oc = new OaiCrawler(au, spec, AuUtil.getAuState(au));
      oc.setCrawlManager(this);
      return oc;
    } else {
      logger.debug("Creating NewContentCrawler for " + au);
      NewContentCrawler nc =
	new NewContentCrawler(au, spec, AuUtil.getAuState(au));
      nc.setCrawlManager(this);
      return nc;
    }
  }

  protected Crawler makeRepairCrawler(ArchivalUnit au,
				      CrawlSpec spec,
				      Collection  repairUrls,
				      float percentRepairFromCache) {
    RepairCrawler rc = new RepairCrawler(au, spec, AuUtil.getAuState(au),
					 repairUrls, percentRepairFromCache);
    rc.setCrawlManager(this);
    return rc;
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

    public String toString() {
      return "[CrawlRunner: " + crawler.getAu() + "]";
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
	  crawler.getStatus().setCrawlStatus(Crawler.STATUS_ABORTED,
					     "Crawler disabled");
	  nowRunning();
	  // exit immediately
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

	  if (!crawlSuccessful && spec != null && !spec.inCrawlWindow()) {
	    // If aborted due to crawl window, undo the charge against its
	    // rate limiter so it can start again when window opens
	    if (auRateLimiter != null) {
	      auRateLimiter.unevent();
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
	CrawlerStatus cs = crawler.getStatus();
	cmStatus.touchCrawlStatus(cs);
	callCallback(cb, cookie, crawlSuccessful, cs);
	if (cs != null) cs.sealCounters();
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
	  if (paramOdc) {
	    startOneWait.expireIn(paramStartCrawlsInitialDelay);
	    cmStatus.setNextCrawlStarter(startOneWait);
	    startOneWait.sleep();
	  } else {
	    waitUntilAusStarted();
	    logger.debug3("AUs started");
	    Deadline initial = Deadline.in(paramStartCrawlsInitialDelay);
	    cmStatus.setNextCrawlStarter(initial);
	    initial.sleep();
	  }
	} catch (InterruptedException e) {
	  // just wakeup, check for exit and start running
	}
      }
      while (goOn) {
// 	pokeWDog();
	try {
	  if (paramOdc) {
	    startOneCrawl();
	  } else {
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
	    cmStatus.setNextCrawlStarter(null);
	  }
	} catch (InterruptedException e) {
	  // just wakeup and check for exit
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

  // Separate so can override for testing
  void waitUntilAusStarted() throws InterruptedException {
    getDaemon().waitUntilAusStarted();
  }

  // Separate so can override for testing
  boolean areAusStarted() {
    return pluginMgr.areAusStarted();
  }

  static Object UNSHARED_RATE_KEY = new Object();

  long paramRebuildCrawlQueueInterval = DEFAULT_REBUILD_CRAWL_QUEUE_INTERVAL;
  long paramQueueRecalcAfterNewAu = DEFAULT_QUEUE_RECALC_AFTER_NEW_AU;
  long paramQueueEmptySleep = DEFAULT_QUEUE_EMPTY_SLEEP;
  int paramUnsharedQueueMax = DEFAULT_UNSHARED_QUEUE_MAX;
  int paramSharedQueueMax = DEFAULT_SHARED_QUEUE_MAX;
  int paramFavorSharedRateThreads = DEFAULT_FAVOR_SHARED_RATE_THREADS;

  Deadline timeToRebuildCrawlQueue = Deadline.in(0);
  Deadline startOneWait = Deadline.in(0);
  Map<ArchivalUnit,CrawlReq> highPriorityCrawlRequests = new ListOrderedMap();
  Comparator CPC = new CrawlPriorityComparator();

  Object queueLock = new Object();	// lock for sharedRateReqs and
					// unsharedRateReqs
  MultiCrawlPriorityMap sharedRateReqs =
    new MultiCrawlPriorityMap(paramSharedQueueMax);
  BoundedTreeSet unsharedRateReqs =
    new BoundedTreeSet(paramUnsharedQueueMax, CPC);

  class MultiCrawlPriorityMap extends MultiValueMap {
    MultiCrawlPriorityMap(final int maxAus) {
      super(new HashMap(), new org.apache.commons.collections.Factory() {
	  public Object create() {
	    return new BoundedTreeSet(maxAus, CPC);
	  }});
    }
    public void setTreeSetSize(int maxAus) {
      for (Map.Entry ent : (Collection<Map.Entry>)entrySet()) {
	BoundedTreeSet ts = (BoundedTreeSet)ent.getValue();
	ts.setMaxSize(maxAus);
      }
    }
  }

  boolean windowOkToStart(CrawlWindow window) {
    if (window == null) return true;
    if (!window.canCrawl()) return false;
    Date soon = new Date(TimeBase.nowMs() + paramMinWindowOpenFor);
    return window.canCrawl(soon);
  }

  // Force queues to be rebuilt. Overkill, but easy and this hardly
  // ever happens
  void removeAuFromQueues(ArchivalUnit au) {
    synchronized (highPriorityCrawlRequests) {
      highPriorityCrawlRequests.remove(au);
    }
    synchronized (queueLock) {
      unsharedRateReqs.clear();
      sharedRateReqs.clear();
    }
  }

  CrawlReq nextReq() throws InterruptedException {
    boolean rebuilt = false;

    if (timeToRebuildCrawlQueue.expired()) {
      rebuildCrawlQueue();
      rebuilt = true;
    }
    CrawlReq res = nextReqFromBuiltQueue();
    if (res != null) {
      return res;
    }
    if (!rebuilt) {
      rebuildCrawlQueue();
    }
    return nextReqFromBuiltQueue();
  }

  CrawlReq nextReqFromBuiltQueue() {
    Collection runKeys = copyRunKeys();
    synchronized (queueLock) {
      if (logger.isDebug3()) {
	logger.debug3("nextReqFromBuiltQueue(), " +
		      sharedRateReqs.size() + " shared, " +
		      unsharedRateReqs.size() + " unshared, " +
		      " runKeys: " + runKeys);
      }
      // preferentially start those with shared rate limiters, but give
      // unshared a minimum number of threads

      BoundedTreeSet finalSort = new BoundedTreeSet(1, CPC);
      for (Iterator iter = sharedRateReqs.entrySet().iterator();
	   iter.hasNext();) {
	Map.Entry ent = (Map.Entry)iter.next();
	Object rateKey = ent.getKey();
	if (runKeys.contains(rateKey)) {
	  continue;
	}
	CrawlReq req = (CrawlReq)((TreeSet)ent.getValue()).first();
	finalSort.add(req);
      }

      if (!unsharedRateReqs.isEmpty() &&
	  (  finalSort.isEmpty() ||
	     runKeys.size() >= paramFavorSharedRateThreads ||
	     ((CrawlReq)unsharedRateReqs.first()).isHiPri())) {
	CrawlReq req = (CrawlReq)unsharedRateReqs.first();
	finalSort.add(req);
      }
      if (finalSort.isEmpty()) {
	if (logger.isDebug3()) {
	  logger.debug3("nextReqFromBuiltQueue(): null, " +
			sharedRateReqs.size() + " shared");
	}
	return null;
      }
      CrawlReq bestReq = (CrawlReq)finalSort.first();
      if (bestReq.rateKey != null) {
	sharedRateReqs.remove(bestReq.rateKey, bestReq);
      } else {
	unsharedRateReqs.remove(bestReq);
      }
      return bestReq;
    }
  }

  Collection copyRunKeys() {
    synchronized (runningCrawls) {
      return new HashBag(runningRateKeys);
    }
  }

  public Collection<CrawlReq> getPendingQueue() {
    Collection runKeys = copyRunKeys();
    TreeSet<CrawlReq> finalSort = new TreeSet(CPC);
    synchronized (queueLock) {
      for (Iterator iter = sharedRateReqs.entrySet().iterator();
	   iter.hasNext();) {
	Map.Entry ent = (Map.Entry)iter.next();
	Object rateKey = ent.getKey();
	if (runKeys.contains(rateKey)) {
	  // mark it somehow
	}
	finalSort.addAll((TreeSet)ent.getValue());
      }
      finalSort.addAll(unsharedRateReqs);
    }
    return finalSort;
  }

  void enqueueHighPriorityCrawl(CrawlReq req) {
    logger.debug("enqueueHighPriorityCrawl(" + req.au + ")");
    synchronized (highPriorityCrawlRequests) {
      highPriorityCrawlRequests.put(req.au, req);
    }
    timeToRebuildCrawlQueue.expire();
    startOneWait.expire();
  }

  public void rebuildQueueSoon() {
    // Don't push forward if already expired.
    if (!timeToRebuildCrawlQueue.expired()) {
      timeToRebuildCrawlQueue.expireIn(paramQueueRecalcAfterNewAu);
    }
    if (!startOneWait.expired()) {
      startOneWait.expireIn(paramQueueRecalcAfterNewAu);
    }
  }

  void rebuildCrawlQueue() {
    timeToRebuildCrawlQueue.expireIn(paramRebuildCrawlQueueInterval);
    long startTime = TimeBase.nowMs();
    rebuildCrawlQueue0();
    logger.debug("rebuildCrawlQueue(): "+
		 (TimeBase.nowMs() - startTime)+"ms");
  }

  void rebuildCrawlQueue0() {
    int ausWantCrawl = 0;
    int ausEligibleCrawl = 0;
    synchronized (queueLock) {
      unsharedRateReqs.clear();
      sharedRateReqs.clear();
      for (ArchivalUnit au : (areAusStarted()
			      ? pluginMgr.getAllAus()
			      : getHighPriorityAus())) {
	try {
	  CrawlReq req;
	  synchronized (highPriorityCrawlRequests) {
	    req = highPriorityCrawlRequests.get(au);
	  }
	  if ((req != null || shouldCrawlForNewContent(au))) {
	    ausWantCrawl++;
	    if (isEligibleForNewContentCrawl(au)) {
	      ausEligibleCrawl++;
	      if (req == null) {
		req = new CrawlReq(au);
	      }
	      Object rateKey = au.getFetchRateLimiterKey();
	      if (rateKey == null) {
		unsharedRateReqs.add(req);
	      } else {
		sharedRateReqs.put(rateKey, req);
	      }
	    }
	  }
	} catch (RuntimeException e) {
	  logger.warning("Checking for crawlworthiness: " + au.getName(), e);
	  // ignore AU if it caused an error
	}
      }
    }
    cmStatus.setWaitingCount(ausWantCrawl);
    cmStatus.setEligibleCount(ausEligibleCrawl);
  }

  List<ArchivalUnit> getHighPriorityAus() {
    synchronized (highPriorityCrawlRequests) {
      return new ArrayList(highPriorityCrawlRequests.keySet());
    }
  }


  /** Orders AUs (wrapped in CrawlReq) by crawl priority:<ol>
   * <li>Explicit request priority
   * <li>Plugin registry AUs
   * <li>Crawl window reopened
   * <li>Least recent crawl attempt
   * <li>Least recent crawl success
   * </ol>
   */
  class CrawlPriorityComparator implements Comparator {
    // Comparator should not reference NodeManager, etc., else all sorted
    // collection insertions, etc. must be protected against
    // NoSuchAuException
    public int compare(Object o1, Object o2) {
      CrawlReq r1 = (CrawlReq)o1;
      CrawlReq r2 = (CrawlReq)o2;
      ArchivalUnit au1 = r1.au;
      ArchivalUnit au2 = r2.au;
      AuState aus1 = r1.aus;
      AuState aus2 = r2.aus;
      return new CompareToBuilder()
	.append(-r1.priority, -r2.priority)
	.append(!(au1 instanceof RegistryArchivalUnit),
		!(au2 instanceof RegistryArchivalUnit))
	.append(previousResultOrder(aus1.getLastCrawlResult()),
		previousResultOrder(aus2.getLastCrawlResult()))
	.append(aus1.getLastCrawlAttempt(), aus2.getLastCrawlAttempt())
	.append(aus1.getLastCrawlTime(), aus2.getLastCrawlTime())
// 	.append(au1.toString(), au2.toString())
	.append(System.identityHashCode(r1), System.identityHashCode(r2))
	.toComparison();
    }

    int previousResultOrder(int crawlResult) {
      final int DEFAULT = 2;
      switch (crawlResult) {
      case Crawler.STATUS_WINDOW_CLOSED:
	return 0;
      case Crawler.STATUS_RUNNING_AT_CRASH: 
	return paramRestartAfterCrash ? 1 : DEFAULT;
      default: return DEFAULT;
      }
    }
  }


  boolean startOneCrawl() throws InterruptedException {
    startOneWait.expireIn(paramQueueEmptySleep);
    if (crawlerEnabled) {
      CrawlReq req = nextReq();
      if (req != null) {
	startCrawl(req);
	return true;
      }
    }
    cmStatus.setNextCrawlStarter(startOneWait);
    while (!startOneWait.expired()) {
      try {
	startOneWait.sleep();
      } catch (InterruptedException e) {
	// just wakeup and check
      }
    }
    return false;
  }

  // Each invocation of startSomeCrawls() tries to fill queue with AUs that
  // need a crawl.  The same random iterator is used across multiple
  // invocations to ensure we examine all AUs before starting over with a
  // new random order.

  private Iterator crawlStartIter = null;

  void startSomeCrawls() {
    if (crawlerEnabled && (poolQueue != null)) {
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
    try {
      if (shouldCrawlForNewContent(au)) {
	startCrawl(au);
      }
    } catch (IllegalArgumentException e) {
      // XXX When NoSuchAuException is created, this should catch that
      logger.warning("AU disappeared: " + au.getName());
    }
  }


  void startCrawl(ArchivalUnit au) {
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

  void startCrawl(CrawlReq req) {
    ArchivalUnit au = req.au;
    try {
      if (pluginMgr.isRegistryAu(au) && req.cb == null) {
	if (logger.isDebug3()) {
	  logger.debug3("Adding callback to registry AU: " + au.getName());
	}
	req.setCb(new PluginManager.RegistryCallback(pluginMgr, au));
      }
      // doesn't return until thread available for next request
      handReqToPool(req);
    } catch (RuntimeException e) {
      logger.warning("Starting crawl: " + au.getName(), e);
    }
  }

  boolean shouldCrawlForNewContent(ArchivalUnit au) {
    try {
      boolean res = au.shouldCrawlForNewContent(AuUtil.getAuState(au));
      if (logger.isDebug3()) logger.debug3("Should " + (res ? "" : " not ") +
					   "crawl " + au);
      return res;
    } catch (IllegalArgumentException e) {
      // XXX When NoSuchAuException is created, this should catch that
      logger.warning("AU disappeared: " + au.getName());
    }
    return false;
  }

  private static class FailingCallbackWrapper
    implements CrawlManager.Callback {
    CrawlManager.Callback cb;
    public FailingCallbackWrapper(CrawlManager.Callback cb) {
      this.cb = cb;
    }
    public void signalCrawlAttemptCompleted(boolean success,
					    Object cookie,
					    CrawlerStatus status) {
      callCallback(cb, cookie, false, null);
    }
  }

  /** Return the StatusSource */
  public StatusSource getStatusSource() {
    return this;
  }

  //CrawlManager.StatusSource methods

  public CrawlManagerStatus getStatus() {
    return cmStatus;
  }

}
