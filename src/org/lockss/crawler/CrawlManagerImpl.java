/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.collections.*;
import org.apache.commons.collections4.map.*;
import org.apache.commons.collections.bag.HashBag; // needed to disambiguate
import org.apache.oro.text.regex.*;

import EDU.oswego.cs.dl.util.concurrent.*;

import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.daemon.Crawler;
import org.lockss.daemon.status.*;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.alert.*;
import org.lockss.state.*;
import org.lockss.plugin.*;
import org.lockss.plugin.exploded.*;
import org.lockss.plugin.AuEvent;

/**
 * Manages crawl queues, starts crawls.
 *
 * @ParamCategory Crawler
 *
 * @ParamCategoryDoc Crawler Controls both the scheduling of crawls and
 * the execution of individual crawls.
 */
public class CrawlManagerImpl extends BaseLockssDaemonManager
    implements CrawlManager, CrawlManager.StatusSource, ConfigurableManager {

  private static final Logger logger = Logger.getLogger("CrawlManager");

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

  /** Set false to prevent the crawl starter from starting queued crawls.
   * Allows queues to be built, which {@value #PARAM_CRAWLER_ENABLED}
   * doesn't. */
  public static final String PARAM_CRAWL_STARTER_ENABLED =
    PREFIX + "starterEnabled";
  static final boolean DEFAULT_CRAWL_STARTER_ENABLED = true;

  /** Use thread pool and queue if true, start threads directly if false.
   * Only takes effect at startup. */
  public static final String PARAM_CRAWLER_QUEUE_ENABLED =
    PREFIX + "queue.enabled";
  static final boolean DEFAULT_CRAWLER_QUEUE_ENABLED = true;

  /** Max threads in crawler thread pool. Does not include repair crawls,
   * which are limited only by the number of running polls. */
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

  static final String ODC_PREFIX = PREFIX + "odc.";

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
  static final int DEFAULT_UNSHARED_QUEUE_MAX = 50;

  /** Size of queue of shared rate AUs. */
  public static final String PARAM_SHARED_QUEUE_MAX =
    ODC_PREFIX + "sharedQueueMax";
  static final int DEFAULT_SHARED_QUEUE_MAX = 50;

  /** Min number of threads available to AUs with unshared rate limiters */
  public static final String PARAM_FAVOR_UNSHARED_RATE_THREADS =
    ODC_PREFIX + "favorUnsharedRateThreads";
  static final int DEFAULT_FAVOR_UNSHARED_RATE_THREADS = 1;

  enum CrawlOrder {CrawlDate, CreationDate};

  /** Determines how the crawl queues are sorted.  <code>CrawlDate</code>:
   * By recency of previous crawl attempt, etc. (Attempts to give all AUs
   * an equal chance to crawl as often as they want.);
   * <code>CreationDate</code>: by order in which AUs were
   * created. (Attempts to synchronize crawls of AU across machines to
   * optimize for earliest polling.) */
  public static final String PARAM_CRAWL_ORDER = PREFIX + "crawlOrder";
  public static final CrawlOrder DEFAULT_CRAWL_ORDER = CrawlOrder.CrawlDate;

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

  /** Maximum rate at which we will start new content crawls for any
   * particular plugin registry */
  public static final String PARAM_MAX_PLUGIN_REGISTRY_NEW_CONTENT_RATE =
    PREFIX + "maxPluginRegistryNewContentRate";
  public static final String DEFAULT_MAX_PLUGIN_REGISTRY_NEW_CONTENT_RATE =
    "1/2h";

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

  /** Map of AUID regexp to priority.  If set, AUs are assigned the
   * corresponding priority of the first regexp that their AUID matches.
   * Priority must be an integer; priorities <= -10000 mean "do not crawl
   * matching AUs", priorities <= -20000 mean "abort running crawls of
   * matching AUs".  */
  @Deprecated
  static final String PARAM_CRAWL_PRIORITY_AUID_MAP =
    PREFIX + "crawlPriorityAuidMap";
  static final List DEFAULT_CRAWL_PRIORITY_AUID_MAP = null;

  /** Maps AU patterns to crawl priority.  Keys are XPath expressions (see
   * {@link org.lockss.util.AuXpathMatcher).  If set, AU's crawl priority
   * is the value associated with the first matching XPath.  Priority must
   * be an integer; priorities <= -10000 mean "do not crawl matching AUs",
   * priorities <= -20000 mean "abort running crawls of matching AUs".  */
  static final String PARAM_CRAWL_PRIORITY_AU_MAP =
    PREFIX + "crawlPriorityAuMap";
  static final List DEFAULT_CRAWL_PRIORITY_AU_MAP = null;

  public static final int MIN_CRAWL_PRIORITY = -10000;
  public static final int ABORT_CRAWL_PRIORITY = -20000;

  /** Map of concurrent pool name to pool size.  By default all pools are
   * of size 1; to enable 2 concurrent crawls in pool FOO, add FOO,2 to
   * this list. */
  static final String PARAM_CONCURRENT_CRAWL_LIMIT_MAP = PREFIX +
    "concurrentCrawlLimitMap";
  static final List DEFAULT_CONCURRENT_CRAWL_LIMIT_MAP = null;

  /** Regexp matching URLs we never want to collect.  Intended to stop
   * runaway crawls by catching recursive URLS */
  public static final String PARAM_EXCLUDE_URL_PATTERN =
    PREFIX + "globallyExcludedUrlPattern";
  static final String DEFAULT_EXCLUDE_URL_PATTERN = null;

  /** List of regexps matching hosts from which collection is permitted
   * without explicit permission on the host.  Intended for distribution
   * sites for standard vss, js, etc. libraries. */
  public static final String PARAM_PERMITTED_HOSTS =
    PREFIX + "globallyPermittedHosts";
  static final List<String> DEFAULT_PERMITTED_HOSTS = Collections.EMPTY_LIST;

  /** List of regexps matching hosts from which plugins are allowed to
   * permit collection without explicit permission on the host.  I.e., this
   * is a filter on what plugins are allowed to permit via
   * au_permitted_host_pattern */
  public static final String PARAM_ALLOWED_PLUGIN_PERMITTED_HOSTS =
    PREFIX + "allowedPluginPermittedHosts";
  static final List<String> DEFAULT_ALLOWED_PLUGIN_PERMITTED_HOSTS =
    Collections.EMPTY_LIST;

  // Defined here because it makes more sense for the name to be associated
  // with the crawler even though it's currently used in BaseUrlFetcher.
  /** Headers that should be added to all HTTP requests from the
   * crawler. */
  public static final String PARAM_REQUEST_HEADERS =
    PREFIX + "httpRequestHeaders";
  public static final List<String> DEFAULT_REQUEST_HEADERS = null;

  static final String WDOG_PARAM_CRAWLER = "Crawler";
  static final long WDOG_DEFAULT_CRAWLER = 2 * Constants.HOUR;

  static final String PRIORITY_PARAM_CRAWLER = "Crawler";
  static final int PRIORITY_DEFAULT_CRAWLER = Thread.NORM_PRIORITY - 1;

  public static final String CRAWL_STATUS_TABLE_NAME = "crawl_status_table";
  public static final String CRAWL_URLS_STATUS_TABLE =
                                    "crawl_urls";
  public static final String SINGLE_CRAWL_STATUS_TABLE =
                                    "single_crawl_status_table";

  protected PluginManager pluginMgr;
  private AlertManager alertMgr;

  //Tracking crawls for the status info
  private CrawlManagerStatus cmStatus;

  // Lock for structures updated when a crawl starts or ends
  Object runningCrawlersLock = new Object();

  // Maps pool key to record of all crawls active in that pool
  // Synchronized on runningCrawlersLock
  private Map<String,PoolCrawlers> poolMap = new HashMap<String,PoolCrawlers>();
  // Number of AUs needing crawl in each pool.  Used to determine if/when
  // it's worth rebuilding the queues
  private Map<String,MutableInt> poolEligible =
    new HashMap<String,MutableInt>();

  // AUs running new content crawls
  // Synchronized on runningCrawlersLock
  private Set<ArchivalUnit> runningNCCrawls = new HashSet<ArchivalUnit>();

  private long contentCrawlExpiration;
  private long repairCrawlExpiration;
  private float percentRepairFromCache;
  private boolean crawlerEnabled = DEFAULT_CRAWLER_ENABLED;
  private boolean crawlStarterEnabled = DEFAULT_CRAWL_STARTER_ENABLED;
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
  
  /** Note that these are Apache ORO Patterns, not Java Patterns */
  private Pattern globallyExcludedUrlPattern;
  
  private List<Pattern> globallyPermittedHostPatterns = Collections.EMPTY_LIST;;
  private List<Pattern> allowedPluginPermittedHosts;

  private PatternIntMap crawlPriorityAuidMap = PatternIntMap.EMPTY;
  private AuXpathFloatMap crawlPriorityAuMap = AuXpathFloatMap.EMPTY;

  private Map<String,Integer> concurrentCrawlLimitMap;

  private int histSize = DEFAULT_HISTORY_MAX;

  private RateLimiter.LimiterMap repairRateLimiters =
    new RateLimiter.LimiterMap(PARAM_MAX_REPAIR_RATE,
			       DEFAULT_MAX_REPAIR_RATE);
  private RateLimiter.LimiterMap newContentRateLimiters =
    new RateLimiter.LimiterMap(PARAM_MAX_NEW_CONTENT_RATE,
			       DEFAULT_MAX_NEW_CONTENT_RATE);
  private RateLimiter.LimiterMap pluginRegistryNewContentRateLimiters =
    new RateLimiter.LimiterMap(PARAM_MAX_PLUGIN_REGISTRY_NEW_CONTENT_RATE,
			       DEFAULT_MAX_PLUGIN_REGISTRY_NEW_CONTENT_RATE);
  private RateLimiter newContentStartRateLimiter;

  private AuEventHandler auCreateDestroyHandler;

  PooledExecutor pool;
  BoundedPriorityQueue poolQueue;

  /**
   * start the crawl manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    super.startService();

    LockssDaemon daemon = getDaemon();
      
    pluginMgr = daemon.getPluginManager();
    alertMgr = daemon.getAlertManager();

    paramOdc = CurrentConfig.getBooleanParam(PARAM_USE_ODC, DEFAULT_USE_ODC);
    cmStatus = new CrawlManagerStatus(histSize);
    cmStatus.setOdc(paramOdc);

    StatusService statusServ = daemon.getStatusService();
    statusServ.registerStatusAccessor(CRAWL_STATUS_TABLE_NAME,
				      new CrawlManagerStatusAccessor(this));
    statusServ.registerOverviewAccessor(CRAWL_STATUS_TABLE_NAME,
					new CrawlManagerStatusAccessor.CrawlOverview(this));
    statusServ.registerStatusAccessor(CRAWL_URLS_STATUS_TABLE,
				      new CrawlUrlsStatusAccessor(this));
    statusServ.registerStatusAccessor(SINGLE_CRAWL_STATUS_TABLE,
                                      new SingleCrawlStatusAccessor(this));     
    // register our AU event handler
    auCreateDestroyHandler = new AuEventHandler.Base() {
	@Override public void auDeleted(AuEvent event, ArchivalUnit au) {
	  auEventDeleted(event, au);
	}
	@Override public void auCreated(AuEvent event, ArchivalUnit au) {
	  auEventCreated(event, au);
	}
      };
    pluginMgr.registerAuEventHandler(auCreateDestroyHandler);

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
    if (auCreateDestroyHandler != null) {
      pluginMgr.unregisterAuEventHandler(auCreateDestroyHandler);
      auCreateDestroyHandler = null;
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
      crawlStarterEnabled =
	config.getBoolean(PARAM_CRAWL_STARTER_ENABLED,
			  DEFAULT_CRAWL_STARTER_ENABLED);

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

      paramFavorUnsharedRateThreads =
	config.getInt(PARAM_FAVOR_UNSHARED_RATE_THREADS,
		      DEFAULT_FAVOR_UNSHARED_RATE_THREADS);

      paramCrawlOrder = (CrawlOrder)config.getEnum(CrawlOrder.class,
						   PARAM_CRAWL_ORDER,
						   DEFAULT_CRAWL_ORDER);

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

      boolean processAborts = false;
      if (changedKeys.contains(PARAM_CRAWL_PRIORITY_AUID_MAP)) {
	installCrawlPriorityAuidMap(config.getList(PARAM_CRAWL_PRIORITY_AUID_MAP,
						   DEFAULT_CRAWL_PRIORITY_AUID_MAP));

	processAborts = true;
      }
      if (changedKeys.contains(PARAM_CRAWL_PRIORITY_AU_MAP)) {
	installCrawlPriorityAuMap(config.getList(PARAM_CRAWL_PRIORITY_AU_MAP,
						   DEFAULT_CRAWL_PRIORITY_AU_MAP));

	processAborts = true;
      }
      if (processAborts && areAusStarted()) {
	processAbortPriorities();
	rebuildQueueSoon();
      }

      if (changedKeys.contains(PARAM_CONCURRENT_CRAWL_LIMIT_MAP)) {
	concurrentCrawlLimitMap =
	  makeCrawlPoolSizeMap(config.getList(PARAM_CONCURRENT_CRAWL_LIMIT_MAP,
					      DEFAULT_CONCURRENT_CRAWL_LIMIT_MAP));
	resetCrawlPoolSizes();
      }
      if (changedKeys.contains(PARAM_START_CRAWLS_INTERVAL) ||
	  changedKeys.contains(PARAM_CRAWL_STARTER_ENABLED)) {
	paramStartCrawlsInterval =
	  config.getTimeInterval(PARAM_START_CRAWLS_INTERVAL,
				 DEFAULT_START_CRAWLS_INTERVAL);
	if (crawlStarterEnabled && paramStartCrawlsInterval > 0) {
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

      if (changedKeys.contains(PARAM_PERMITTED_HOSTS)) {
	setGloballyPermittedHostPatterns(config.getList(PARAM_PERMITTED_HOSTS,
							DEFAULT_PERMITTED_HOSTS));
      }

      if (changedKeys.contains(PARAM_ALLOWED_PLUGIN_PERMITTED_HOSTS)) {
	setAllowedPluginPermittedHostPatterns(config.getList(PARAM_ALLOWED_PLUGIN_PERMITTED_HOSTS,
							     DEFAULT_ALLOWED_PLUGIN_PERMITTED_HOSTS));
      }

      if (changedKeys.contains(PARAM_MAX_REPAIR_RATE)) {
	repairRateLimiters.resetRateLimiters(config);
      }
      if (changedKeys.contains(PARAM_MAX_NEW_CONTENT_RATE)) {
	newContentRateLimiters.resetRateLimiters(config);
      }
      if (changedKeys.contains(PARAM_MAX_PLUGIN_REGISTRY_NEW_CONTENT_RATE)) {
	pluginRegistryNewContentRateLimiters.resetRateLimiters(config);
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
    return RegexpUtil.getMatcher().contains(url, globallyExcludedUrlPattern);
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

  public boolean isGloballyPermittedHost(String host) {
    for (Pattern pat : globallyPermittedHostPatterns) {
      if (RegexpUtil.getMatcher().contains(host, pat)) {
	return true;
      }
    }
    return false;
  }

  void setGloballyPermittedHostPatterns(List<String> pats) {
    if (pats == null) {
      globallyPermittedHostPatterns = Collections.EMPTY_LIST;
      logger.debug("No global exclude patterns");
    } else {
      // not using RegexpUtil.compileRegexps() so one bad pattern doesn't
      // prevent others from taking effect
      int flags =
	Perl5Compiler.READ_ONLY_MASK | Perl5Compiler.CASE_INSENSITIVE_MASK;
      Perl5Compiler comp = RegexpUtil.getCompiler();
      List<Pattern> res = new ArrayList<Pattern>(pats.size());
      for (String pat : pats) {
	try {
	  res.add(comp.compile(pat, flags));
	} catch (MalformedPatternException e) {
	  logger.error("Illegal globally permitted host pattern: " + pat, e);
	}
      }
      globallyPermittedHostPatterns = res;
      logger.info("Globally permitted host patterns: " + res);
    }
  }

  public boolean isAllowedPluginPermittedHost(String host) {
    for (Pattern pat : allowedPluginPermittedHosts) {
      if (RegexpUtil.getMatcher().contains(host, pat)) {
	return true;
      }
    }
    return false;
  }

  void setAllowedPluginPermittedHostPatterns(List<String> pats) {
    if (pats == null) {
      allowedPluginPermittedHosts = Collections.EMPTY_LIST;
      logger.debug("No allowed plugin permitted hosts");
    } else {
      // not using RegexpUtil.compileRegexps() so one bad pattern doesn't
      // prevent others from taking effect
      int flags =
	Perl5Compiler.READ_ONLY_MASK | Perl5Compiler.CASE_INSENSITIVE_MASK;
      Perl5Compiler comp = RegexpUtil.getCompiler();
      List<Pattern> res = new ArrayList<Pattern>(pats.size());
      for (String pat : pats) {
	try {
	  res.add(comp.compile(pat, flags));
	} catch (MalformedPatternException e) {
	  logger.error("Illegal allowed plugin permitted host pattern: " + pat,
		       e);
	}
      }
      allowedPluginPermittedHosts = res;
      logger.info("Allowed plugin permitted host patterns: " + res);
    }
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

  /** Info about all the crawls running in a crawl pool.  Ensures that they
   * all use the same set of rate limiters.  All methods that access the
   * crawlers or rate limiters must be called within a <code>synchronized
   * (runningCrawlersLock) block.</code> */
  class PoolCrawlers {
    String poolKey;
    int max;
    Set<CrawlRateLimiter> crls = new HashSet<CrawlRateLimiter>();
    Map<Crawler,CrawlRateLimiter> crlMap =
      new HashMap<Crawler,CrawlRateLimiter>();
    boolean isShared = false;

    PoolCrawlers(String key) {
      this.poolKey = key;
      setMax();
    }

    void setMax() {
      max = getCrawlPoolSize(poolKey);
    }

    void setShared() {
      isShared = true;
    }

    boolean isShared() {
      return isShared;
    }

    /** Add a crawler and assign it a CrawlRateLimiter from the available
     * pool */
    void addCrawler(Crawler crawler) {
      if (crlMap.containsKey(crawler)) {
	logger.warning("Adding redundant crawler: " + crawler.getAu() + ", " +
		       crawler, new Throwable());
	return;
      }
      CrawlRateLimiter crl;
      if (crls.size() < max) {
	crl = newCrawlRateLimiter(crawler.getAu());
	crls.add(crl);
      } else {
	crl = chooseCrawlRateLimiter(crawler);
      }
      crl.addCrawler(crawler);
      crlMap.put(crawler, crl);
    }

    boolean isEmpty() {
      return crlMap.isEmpty();
    }

    /** Return collection of all active crawlers in this pool.  Result
     * must be obtained and used inside <code>synchronized
     * (runningCrawlersLock)</code> */
    Collection<Crawler> getCrawlers() {
      return crlMap.keySet();
    }

    /** Remove a crawler, inform the CrawlRateLimiter */
    void removeCrawler(Crawler crawler) {
      CrawlRateLimiter crl = crlMap.get(crawler);
      if (crl != null) {
	crl.removeCrawler(crawler);
      } else {
	logger.error("Stopping crawler with no crl: " + crawler);
      }
      crlMap.remove(crawler);
    }

    /** New content crawls prefer a crl that's not in use by any other new
     * content crawls.  (One should always exist, unless pool size changed
     * between nextReqFromBuiltQueue() and now.)  Repair crawls get crl
     * with minimum use count.
     */
    CrawlRateLimiter chooseCrawlRateLimiter(Crawler crawler) {
      CrawlRateLimiter res = null;
      for (CrawlRateLimiter crl : crls) {
	if (crawler.isWholeAU() && crl.getNewContentCount() != 0) {
	  continue;
	}
	if (res == null || crl.getCrawlerCount() < res.getCrawlerCount()) {
	  res = crl;
	}
      }
      if (res == null) {
	// This can happen if the pool size is changing (due to a config
	// update) such that nextReqFromBuiltQueue() sees a different size
	// than has yet been communicated to the crawl pools by our config
	// callback.  The temporary consequences of causing this crawl
	// start to fail seem better than allowing it to proceed with a
	// (needlessly) shared rate limiter).
	throw new IllegalStateException("No crl available for: " + crawler
					+ " in pool: " + poolKey);
      }
      return res;
    }
  }

  protected CrawlRateLimiter newCrawlRateLimiter(ArchivalUnit au) {
    return CrawlRateLimiter.Util.forAu(au);
  }

  protected String getPoolKey(ArchivalUnit au) {
    String pool = au.getFetchRateLimiterKey();
    if (pool == null) {
      pool = au.getAuId();
    }
    return pool;
  }

  void resetCrawlPoolSizes() {
    synchronized (runningCrawlersLock) {
      for (PoolCrawlers pc : poolMap.values()) {
	pc.setMax();
      }
    }
  }

  protected void addToRunningCrawls(ArchivalUnit au, Crawler crawler) {
    synchronized (runningCrawlersLock) {
      String pool = getPoolKey(au);
      logger.debug3("addToRunningCrawls: " + au + " to: " + pool);
      PoolCrawlers pc = poolMap.get(pool);
      if (pc == null) {
	pc = new PoolCrawlers(pool);
	if (au.getFetchRateLimiterKey() != null) {
	  pc.setShared();
	}
	poolMap.put(pool, pc);
      }
      pc.addCrawler(crawler);
      // It's possible for an AU's crawl pool to change (e.g., if the title
      // DB is updated).  Ensure we the now-current pool when we remove the
      // crawler later.
      crawler.setCrawlPool(pool);
      if (crawler.isWholeAU()) {
	setRunningNCCrawl(au, true);
      }      
    }
    synchronized (highPriorityCrawlRequests) {
      highPriorityCrawlRequests.remove(au.getAuId());
    }
  }

  protected void removeFromRunningCrawls(Crawler crawler) {
    logger.debug3("removeFromRunningCrawls: " + crawler);
    if (crawler != null) {
      ArchivalUnit au = crawler.getAu();
      synchronized (runningCrawlersLock) {
	String pool = crawler.getCrawlPool();
	PoolCrawlers pc = poolMap.get(pool);
	pc.removeCrawler(crawler);
	if (pc.isEmpty()) {
	  poolMap.remove(pool);
	}
	if (crawler.isWholeAU()) {
	  setRunningNCCrawl(au, false);
	  startOneWait.expire();
	}      
      }
      cmStatus.removeCrawlerStatusIfPending(crawler.getCrawlerStatus());
    }
  }

  void setRunningNCCrawl(ArchivalUnit au, boolean val) {
    if (val) {
      runningNCCrawls.add(au);
    } else {
      runningNCCrawls.remove(au);
    }
    cmStatus.setRunningNCCrawls(new ArrayList(runningNCCrawls));
  }

  protected boolean isRunningNCCrawl(ArchivalUnit au) {
    synchronized (runningCrawlersLock) {
      return runningNCCrawls.contains(au);
    }
  }

  public CrawlRateLimiter getCrawlRateLimiter(Crawler crawler) {
    synchronized (runningCrawlersLock) {
      PoolCrawlers pc = poolMap.get(crawler.getCrawlPool());
      if (pc == null) {
        return null;
      }
      CrawlRateLimiter res = pc.crlMap.get(crawler);
      if (res == null) {
        throw new RuntimeException("No CrawlRateLimiter for: " + crawler);
      }
      return res;
    }
  }

  void auEventDeleted(AuEvent event, ArchivalUnit au) {
    switch (event.getType()) {
    case RestartDelete:
      // Don't remove if being deleted due to plugin reload, but clear
      // objects that will be re-instantiated
      CrawlReq req = highPriorityCrawlRequests.get(au.getAuId());
      if (req != null) {
	req.auDeleted();
      }
      break;
    default:
      removeAuFromQueues(au);
    }
    synchronized(runningCrawlersLock) {
      for (PoolCrawlers pc : poolMap.values()) {
	for (Crawler crawler : pc.getCrawlers()) {
	  if (au == crawler.getAu()) {
	    crawler.abortCrawl();
            // If it hadn't actually started yet, ensure status removed
            cmStatus.removeCrawlerStatusIfPending(crawler.getCrawlerStatus());
	  }
	}
      }
    }
    // Notify CrawlerStatus objects to discard any pointer to this AU
    for (CrawlerStatus status : cmStatus.getCrawlerStatusList()) {
      status.auDeleted(au);
    }
  }

  void auEventCreated(AuEvent event, ArchivalUnit au) {
    // Check whether this AU was on the high priority queue when it was
    // deactivated.  (Should be necessary only for RestartCreate but cheap
    // to do always.)
    CrawlReq req = highPriorityCrawlRequests.get(au.getAuId());
    if (req != null) {
      logger.debug2("Refresh: " + au + ", " + AuUtil.getAuState(au));
      req.refresh(au, AuUtil.getAuState(au));
    }

    rebuildQueueSoon();
  }

  /** For all running crawls, collect those whose AU's priority is <=
   * ABORT_CRAWL_PRIORITY, then abort all those crawls  */
  void processAbortPriorities() {
    List<Crawler> abortCrawlers = new ArrayList<Crawler>();
    for (PoolCrawlers pc : poolMap.values()) {
      for (Crawler crawler : pc.getCrawlers()) {
	ArchivalUnit au = crawler.getAu();
        int pri = getAuPriority(au, ABORT_CRAWL_PRIORITY);
	if (pri <= ABORT_CRAWL_PRIORITY) {
	  abortCrawlers.add(crawler);
	}
      }
    }
    synchronized(runningCrawlersLock) {
      for (Crawler crawler : abortCrawlers) {
	logger.info("Aborting crawl: " + crawler.getAu());
	crawler.abortCrawl();
      }
    }
  }

  // Overridable for testing
  protected boolean isInternalAu(ArchivalUnit au) {
    return pluginMgr.isInternalAu(au);
  }

  public RateLimiter getNewContentRateLimiter(ArchivalUnit au) {
    if (isInternalAu(au)) {
      return pluginRegistryNewContentRateLimiters.getRateLimiter(au);
    } else {
      return newContentRateLimiters.getRateLimiter(au);
    }
  }

  /** Set up crawl priority map. */
  void installCrawlPriorityAuidMap(List<String> patternPairs) {
    if (patternPairs == null) {
      logger.debug("Installing empty crawl priority map");
      crawlPriorityAuidMap = PatternIntMap.EMPTY;
    } else {
      try {
	crawlPriorityAuidMap = new PatternIntMap(patternPairs);
	logger.debug("Installing crawl priority map: " + crawlPriorityAuidMap);
      } catch (IllegalArgumentException e) {
	logger.error("Illegal crawl priority map, ignoring", e);
	logger.error("Crawl priority map unchanged, still: " +
		     crawlPriorityAuidMap);
      }
    }
  }

  void installCrawlPriorityAuMap(List<String> patternPairs) {
    if (patternPairs == null) {
      logger.debug("Installing empty crawl priority au map");
      crawlPriorityAuMap = AuXpathFloatMap.EMPTY;
    } else {
      try {
	crawlPriorityAuMap = new AuXpathFloatMap(patternPairs);
	logger.debug("Installing crawl priority au map: " + crawlPriorityAuMap);
      } catch (IllegalArgumentException e) {
	logger.error("Illegal crawl priority au map, ignoring", e);
	logger.error("Crawl priority au map unchanged, still: " +
		     crawlPriorityAuMap);
      }
    }
  }

  /** Set up crawl pool size map. */
  Map<String,Integer> makeCrawlPoolSizeMap(Collection<String> pairs) {
    if (pairs != null) {
      Map<String,Integer> map = new HashMap();
      for (String pair : pairs) {
	List<String> onePair = StringUtil.breakAt(pair, ",");
	if (onePair.size() != 2) {
	  logger.error("Malformed pool,size pair, ignored: " + pair);
	  continue;
	}
	String pool = onePair.get(0);
	try {
	  int size = Integer.parseInt(onePair.get(1));
	  logger.info("Crawl pool " + pool + ", size " + size);
	  map.put(pool, size);
	} catch (NumberFormatException e) {
	  logger.error("Illegal crawl pool size, ignored: " + pool + ", "
		       + onePair.get(1), e);
	}
      }
      return map;
    }
    return null;
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
    RateLimiter limiter = repairRateLimiters.getRateLimiter(au);
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
      crawler = makeRepairCrawler(au, locks.keySet());
      CrawlRunner runner =
	new CrawlRunner(crawler, cb, cookie, locks.values(), limiter);
      cmStatus.addCrawlStatus(crawler.getCrawlerStatus());
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
      cmStatus.removeCrawlerStatusIfPending(crawler.getCrawlerStatus());
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

  /** Used to convey to clients the reason an AU is ineligible to be
   * crawled (for logging/display) */
  public static class NotEligibleException extends Exception {
    public NotEligibleException(String msg) {
      super(msg);
    }

    public static class RateLimiter extends NotEligibleException {
      public RateLimiter(String msg) {
	super(msg);
      }
    }
  }

  public void checkEligibleToQueueNewContentCrawl(ArchivalUnit au)
      throws NotEligibleException {
    if (isRunningNCCrawl(au)) {
      throw new NotEligibleException("AU is crawling now.");
    }
    if (au instanceof ExplodedArchivalUnit) {
      throw new NotEligibleException("Can't crawl ExplodedArchivalUnit");
    }
    RateLimiter limiter = getNewContentRateLimiter(au);
    if (limiter != null && !limiter.isEventOk()) {
      throw new NotEligibleException.RateLimiter("Exceeds crawl-start rate: " +
						 limiter.getRate());
    }
  }

  public void checkEligibleForNewContentCrawl(ArchivalUnit au)
      throws NotEligibleException {
    if (!windowOkToStart(au.getCrawlWindow())) {
      throw new NotEligibleException("Crawl window is closed");
    }
    checkEligibleToQueueNewContentCrawl(au);
  }

  public boolean isEligibleForNewContentCrawl(ArchivalUnit au) {
    try {
      checkEligibleForNewContentCrawl(au);
      return true;
    } catch (NotEligibleException e) {
      return false;
    }
  }

  public void startNewContentCrawl(ArchivalUnit au, CrawlManager.Callback cb,
				   Object cookie, ActivityRegulator.Lock lock) {
    startNewContentCrawl(au, 0, cb, cookie, lock);
  }

  public void startNewContentCrawl(ArchivalUnit au, int priority,
				   CrawlManager.Callback cb,
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
      req.setPriority(priority);
    } catch (RuntimeException e) {
      logger.error("Couldn't create CrawlReq: " + au, e);
      callCallback(cb, cookie, false, null);
      return;
    }
    startNewContentCrawl(req, lock);
  }


  public void startNewContentCrawl(CrawlReq req, ActivityRegulator.Lock lock) {
    if (req.getAu() == null) {
      throw new IllegalArgumentException("Called with null AU");
    }
    if (!crawlerEnabled) {
      logger.warning("Crawler disabled, not crawling: " + req.getAu());
      callCallback(req.getCb(), req.getCookie(), false, null);
      return;
    }
    if (paramOdc) {
      enqueueHighPriorityCrawl(req);
    } else {
      if (!isEligibleForNewContentCrawl(req.getAu())) {
        callCallback(req.getCb(), req.getCookie(), false, null);
        return;
      }
      handReqToPool(req);
    }
  }

  // Prevent the warning below from being output excessively, in case issue
  // 5335 still isn't fixed.
  long msgCounter = 0;

  void handReqToPool(CrawlReq req) {
    if (!req.isActive()) {
      if (msgCounter++ < 100) {
	logger.warning("Inactive req: " + req);
      } else if ((msgCounter % 1000000) == 0) {
	logger.warning("Inactive req (rpt " + msgCounter + "): " + req);
      }
      removeAuFromQueues(req.getAuId()); // insurance
      return;
    }

    ArchivalUnit au = req.getAu();
    CrawlManager.Callback cb = req.cb;
    Object cookie = req.cookie;
    ActivityRegulator.Lock lock = req.lock;

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
    CrawlRunner runner = null;
    try {
      crawler = makeFollowLinkCrawler(au);
      crawler.setCrawlReq(req);
      runner = new CrawlRunner(crawler, cb, cookie, SetUtil.set(lock),
			       getNewContentRateLimiter(au),
			       newContentStartRateLimiter);
      // To avoid race, must add to running crawls before starting
      // execution
      addToRunningCrawls(au, crawler);
      if (paramOdc) {
	// Add status first.  execute might not return for a long time, and
	// we're expecting this crawl to be accepted.
	cmStatus.addCrawlStatus(crawler.getCrawlerStatus());
	execute(runner);
	return;
      } else {
	// Add to status only if successfully queued or started.  (No
	// race here; appearance in status might be delayed.)
	execute(runner);
	cmStatus.addCrawlStatus(crawler.getCrawlerStatus());
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
      cmStatus.removeCrawlerStatusIfPending(crawler.getCrawlerStatus());
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

  protected Crawler makeFollowLinkCrawler(ArchivalUnit au) {
    logger.debug("Creating FollowLinkCrawler for " + au);
    FollowLinkCrawler nc =
    		new FollowLinkCrawler(au, AuUtil.getAuState(au));
    nc.setCrawlManager(this);
    return nc;
  }

  protected Crawler makeRepairCrawler(ArchivalUnit au,
				      Collection<String>  repairUrls) {
    RepairCrawler rc = new RepairCrawler(au, AuUtil.getAuState(au),
					 repairUrls);
    rc.setCrawlManager(this);
    return rc;
  }

  static String makeThreadName(Crawler crawler) {
    return AuUtil.getThreadNameFor(getThreadNamePrefix(crawler),
				   crawler.getAu());
  }

  static String getThreadNamePrefix(Crawler crawler) {
    return crawler.getType().toString() + " Crawl";
  }

  private static int createIndex = 0;

  public class CrawlRunner extends LockssRunnable {
    private Object cookie;
    private Crawler crawler;
    private CrawlManager.Callback cb;
    private Collection locks;
    private RateLimiter auRateLimiter;
    private RateLimiter startRateLimiter;
    private int sortOrder;

    private CrawlRunner(Crawler crawler,
			CrawlManager.Callback cb,
			Object cookie, Collection locks,
			RateLimiter auRateLimiter) {
      this(crawler, cb, cookie, locks, auRateLimiter, null);
    }

    private CrawlRunner(Crawler crawler,
			CrawlManager.Callback cb,
			Object cookie, Collection locks,
			RateLimiter auRateLimiter,
			RateLimiter startRateLimiter) {
      super(makeThreadName(crawler));
      this.cb = cb;
      this.cookie = cookie;
      this.crawler = crawler;
      this.locks = locks;
      this.auRateLimiter = auRateLimiter;
      this.startRateLimiter = startRateLimiter;
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
	  crawler.getCrawlerStatus().setCrawlStatus(Crawler.STATUS_ABORTED,
					     "Crawler disabled");
	  nowRunning();
	  // exit immediately
	} else {
	  setPriority(PRIORITY_PARAM_CRAWLER, PRIORITY_DEFAULT_CRAWLER);
	  crawler.setWatchdog(this);
	  startWDog(WDOG_PARAM_CRAWLER, WDOG_DEFAULT_CRAWLER);
	  // don't record event if crawl is going to abort immediately
	  if (crawler.getAu().inCrawlWindow()) {
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

	  if (!crawlSuccessful && !crawler.getAu().inCrawlWindow()) {
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
	CrawlerStatus cs = crawler.getCrawlerStatus();
	cmStatus.touchCrawlStatus(cs);
	signalAuEvent(crawler, cs);
	// must call callback before sealing counters.  V3Poller relies
	// on fetched URL list
	callCallback(cb, cookie, crawlSuccessful, cs);
	if (cs != null) cs.sealCounters();
	setThreadName(getThreadNamePrefix(crawler) + ": idle");
      }
    }
  }

  private void signalAuEvent(Crawler crawler, CrawlerStatus cs) {
    final ArchivalUnit au = crawler.getAu();
    final AuEventHandler.ChangeInfo chInfo = new AuEventHandler.ChangeInfo();
    Collection<String> mimeTypes = cs.getMimeTypes();
    if (mimeTypes != null) {
      Map<String,Integer> mimeCounts = new HashMap<String,Integer>();
      for (String mimeType : mimeTypes) {
        mimeCounts.put(mimeType, cs.getMimeTypeCtr(mimeType).getCount());
      }
      chInfo.setMimeCounts(mimeCounts);
    }
    int num = cs.getNumFetched();
    chInfo.setNumUrls(num);
    if (crawler.isWholeAU()) {
      chInfo.setType(AuEventHandler.ChangeInfo.Type.Crawl);
    } else {
      chInfo.setType(AuEventHandler.ChangeInfo.Type.Repair);
      chInfo.setUrls(cs.getUrlsFetched());
    }
    chInfo.setAu(au);
    chInfo.setComplete(!cs.isCrawlError());
    pluginMgr.applyAuEvent(new PluginManager.AuEventClosure() {
			     public void execute(AuEventHandler hand) {
			       hand.auContentChanged(new AuEvent(AuEvent.Type.
			                                         ContentChanged,
			                                         false),
						     au, chInfo);
			     }
			   });
  }

  // For testing only.  See TestCrawlManagerImpl
  protected void instrumentBeforeStartRateLimiterEvent(Crawler crawler) {
  }

  // Crawl starter thread.

  private CrawlStarter crawlStarter = null;
  private boolean isCrawlStarterRunning = false;

  public boolean isCrawlStarterEnabled() {
    return crawlStarterEnabled;
  }

  public void enableCrawlStarter() {
    if (crawlStarter != null) {
      logger.debug("Crawl starter already running; stopping old one first");
      disableCrawlStarter();
    }
    if (crawlStarterEnabled && paramStartCrawlsInterval > 0) {
      logger.info("Starting crawl starter");
      crawlStarter = new CrawlStarter();
      new Thread(crawlStarter).start();
      isCrawlStarterRunning = true;
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
    isCrawlStarterRunning = false;
  }

  public boolean isCrawlStarterRunning() {
    return isCrawlStarterRunning;
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
	  if (paramOdc) {
	    startOneWait.expireIn(paramStartCrawlsInitialDelay);
	    cmStatus.setNextCrawlStarter(startOneWait);
	    startOneWait.sleep();
	  } else {
	    logger.debug("Waiting until AUs started");
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
    // may be called before service is started (from setConfig())
    return pluginMgr != null && pluginMgr.areAusStarted();
  }

  static String UNSHARED_RATE_KEY = "Un-SharED";

  long paramRebuildCrawlQueueInterval = DEFAULT_REBUILD_CRAWL_QUEUE_INTERVAL;
  long paramQueueRecalcAfterNewAu = DEFAULT_QUEUE_RECALC_AFTER_NEW_AU;
  long paramQueueEmptySleep = DEFAULT_QUEUE_EMPTY_SLEEP;
  int paramUnsharedQueueMax = DEFAULT_UNSHARED_QUEUE_MAX;
  int paramSharedQueueMax = DEFAULT_SHARED_QUEUE_MAX;
  int paramFavorUnsharedRateThreads = DEFAULT_FAVOR_UNSHARED_RATE_THREADS;
  CrawlOrder paramCrawlOrder = DEFAULT_CRAWL_ORDER;

  Deadline timeToRebuildCrawlQueue = Deadline.in(0);
  Deadline startOneWait = Deadline.in(0);
  Map<String,CrawlReq> highPriorityCrawlRequests = new ListOrderedMap();
  Comparator CPC = new CrawlPriorityComparator();

  Object queueLock = new Object();	// lock for sharedRateReqs and
					// unsharedRateReqs
  MultiCrawlPriorityMap sharedRateReqs = new MultiCrawlPriorityMap();
  BoundedTreeSet unsharedRateReqs =
    new BoundedTreeSet(paramUnsharedQueueMax, CPC);

  class MultiCrawlPriorityMap extends MultiValueMap {
    MultiCrawlPriorityMap() {
      super(new HashMap(), new org.apache.commons.collections4.Factory() {
	  public Object create() {
	    return new BoundedTreeSet(paramSharedQueueMax, CPC);
	  }});
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
    removeAuFromQueues(au.getAuId()); 
 }

  // Force queues to be rebuilt. Overkill, but easy and this hardly
  // ever happens
  void removeAuFromQueues(String auid) {
    synchronized (highPriorityCrawlRequests) {
      highPriorityCrawlRequests.remove(auid);
    }
    forceQueueRebuild();
  }

  private void forceQueueRebuild() {
    timeToRebuildCrawlQueue.expire();
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
    if (!rebuilt && isWorthRebuildingQueue()) {
      rebuildCrawlQueue();
    }
    return nextReqFromBuiltQueue();
  }

  // true if any pool that now has unused crawl capacity, has additional
  // AUs that weren't added to its queue at the last queue rebuild.
   boolean isWorthRebuildingQueue() {
    Bag runKeys = copySharedRunKeys();
    for (Map.Entry<String,MutableInt> ent : poolEligible.entrySet()) {
      int additionalEligible = ent.getValue().intValue();
      if (additionalEligible <= 0) {
	continue;
      }
      String rateKey = ent.getKey();
      if (rateKey == UNSHARED_RATE_KEY) {
	logger.debug2("Pool " + rateKey + ", addtl eligible: " +
		      additionalEligible);
	return true;
      } else {
	logger.debug2("Pool " + rateKey +
		      ", size " + getCrawlPoolSize(rateKey) +
		      ", running " + runKeys.getCount(rateKey) +
		      ", addtl eligible: " + additionalEligible);
	if (getCrawlPoolSize(rateKey) > (runKeys.getCount(rateKey))) {
	  return true;
	}
      }
    }
    return false;
  }

  CrawlReq nextReqFromBuiltQueue() {
    Bag runKeys = copySharedRunKeys();
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
	Map.Entry<String,TreeSet> ent = (Map.Entry)iter.next();
	String rateKey = ent.getKey();
	int poolSize = getCrawlPoolSize(rateKey);
	if (logger.isDebug3()) {
	  logger.debug3("Rate key: " + rateKey + ", pool: " + poolSize +
			", current: " + runKeys.getCount(rateKey));
	}
	if (runKeys.getCount(rateKey) >= poolSize) {
	  continue;
	}
	CrawlReq req = (CrawlReq)ent.getValue().first();
	if (logger.isDebug3()) logger.debug3("Adding to final sort: " + req);
	finalSort.add(req);
      }

      if (!unsharedRateReqs.isEmpty() &&
	  (  finalSort.isEmpty() ||
	     runKeys.size() >= (paramMaxPoolSize -
				paramFavorUnsharedRateThreads) ||
	     ((CrawlReq)unsharedRateReqs.first()).isHiPri())) {
	CrawlReq req = (CrawlReq)unsharedRateReqs.first();
	if (logger.isDebug3()) logger.debug3("Adding to final sort: " + req);
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
      sharedRateReqs.removeMapping(bestReq.getRateKey(), bestReq);
      unsharedRateReqs.remove(bestReq);
      logger.debug3("nextReqFromBuiltQueue: " + bestReq);
      return bestReq;
    }
  }

  int getCrawlPoolSize(String key) {
    if (concurrentCrawlLimitMap != null
	&& concurrentCrawlLimitMap.containsKey(key)) {
      return concurrentCrawlLimitMap.get(key);
    } else {
      return 1;
    }
  }

  Bag copySharedRunKeys() {
    return copyRunKeys(true);
  }

  Bag copyRunKeys() {
    return copyRunKeys(false);
  }

  Bag copyRunKeys(boolean sharedOnly) {
    synchronized (runningCrawlersLock) {
      Bag res = new HashBag();
      for (Map.Entry<String,PoolCrawlers> ent : poolMap.entrySet()) {
	PoolCrawlers pc = ent.getValue();
	if (sharedOnly && !pc.isShared()) {
	  continue;
	}
	int sum = 0;
	for (CrawlRateLimiter crl : pc.crls) {
	  sum += crl.getNewContentCount();
	}
	res.add(ent.getKey(), sum);
      }
      return res;
    }
  }

  public Collection<CrawlReq> getPendingQueue() {
    Collection runKeys = copyRunKeys();
    TreeSet<CrawlReq> finalSort = new TreeSet(CPC);
    synchronized (highPriorityCrawlRequests) {
      finalSort.addAll(highPriorityCrawlRequests.values());
    }
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
    logger.debug("enqueueHighPriorityCrawl(" + req.getAu() + ")");
    synchronized (highPriorityCrawlRequests) {
      highPriorityCrawlRequests.put(req.getAuId(), req);
    }
    forceQueueRebuild();
    startOneWait.expire();
  }

  public void rebuildQueueSoon() {
    if (startOneWait.expired()) {
      // If already time to run ensure queue gets rebuilt
      forceQueueRebuild();
    } else {
      // Don't push forward if already expired.
      if (!timeToRebuildCrawlQueue.expired()) {
	timeToRebuildCrawlQueue.expireIn(paramQueueRecalcAfterNewAu);
      }
      startOneWait.expireIn(paramQueueRecalcAfterNewAu);
    }
  }

  void rebuildCrawlQueue() {
    timeToRebuildCrawlQueue.expireIn(paramRebuildCrawlQueueInterval);
    long startTime = TimeBase.nowMs();
    rebuildCrawlQueue0();
    logger.debug("rebuildCrawlQueue(): "+
		 StringUtil.timeIntervalToString(TimeBase.msSince(startTime)));
  }

  void rebuildCrawlQueue0() {
    int ausWantCrawl = 0;
    int ausEligibleCrawl = 0;
    synchronized (queueLock) {
      poolEligible.clear();
      unsharedRateReqs.clear();
      unsharedRateReqs.setMaxSize(paramUnsharedQueueMax);
      sharedRateReqs.clear();
      for (ArchivalUnit au : (areAusStarted()
			      ? pluginMgr.getAllAus()
			      : getHighPriorityAus())) {
	try {
	  CrawlReq req;
	  synchronized (highPriorityCrawlRequests) {
	    req = highPriorityCrawlRequests.get(au.getAuId());
	    if (req != null && !req.isActive()) {
	      logger.warning("Found inactive req on queue: " + req);
	      continue;
	    }
	  }
	  if ((req != null || shouldCrawlForNewContent(au))) {
	    ausWantCrawl++;
	    if (isEligibleForNewContentCrawl(au)) {
	      if (req == null) {
		req = new CrawlReq(au);
		setReqPriority(req);
	      }
	      if (req.priority > MIN_CRAWL_PRIORITY) {
		ausEligibleCrawl++;
		String rateKey = req.getRateKey();
		if (rateKey == null) {
		  unsharedRateReqs.add(req);
		  incrPoolEligible(UNSHARED_RATE_KEY);
		  if (logger.isDebug3()) {
		    logger.debug3("Added to queue: null, " + req);
		  }
		} else {
		  sharedRateReqs.put(rateKey, req);
		  incrPoolEligible(rateKey);
		  if (logger.isDebug3()) {
		    logger.debug3("Added to pool queue: " + rateKey +
				  ", " + req);
		  }
		}
	      }
	    }
	  }
	} catch (RuntimeException e) {
	  logger.warning("Checking for crawlworthiness: " + au.getName(), e);
	  // ignore AU if it caused an error
	}
      }
    }
    adjustEligibleCounts();
    cmStatus.setWaitingCount(ausWantCrawl);
    cmStatus.setEligibleCount(ausEligibleCrawl);
  }

  void incrPoolEligible(String pool) {
    MutableInt n = poolEligible.get(pool);
    if (n == null) {
      n = new MutableInt();
      poolEligible.put(pool, n);
    }
    n.add(1);
  }

  // called within synchronized (queueLock) {...}

  private void adjustEligibleCounts() {
    for (Map.Entry<String,MutableInt> ent : poolEligible.entrySet()) {
      String poolKey = ent.getKey();
      if (poolKey == UNSHARED_RATE_KEY) {
	ent.getValue().subtract(unsharedRateReqs.getMaxSize());
      } else {
	Set poolQueue = (Set)sharedRateReqs.get(poolKey);
	if (poolQueue != null) {
	  ent.getValue().subtract(poolQueue.size());
	}
      }
      logger.debug2("Additional Eligible: " + poolKey + ": " + ent.getValue());
    }
  }

  List<ArchivalUnit> getHighPriorityAus() {
    synchronized (highPriorityCrawlRequests) {
      List<ArchivalUnit> res = new ArrayList<ArchivalUnit>();
      for (CrawlReq req : highPriorityCrawlRequests.values()) {
	if (req.isActive()) {
	  res.add(req.getAu());
	}
      }
      return res;
    }
  }

  void setReqPriority(CrawlReq req) {
    int pri = getAuPriority(req.getAu());
    if (pri != 0) {
      if (logger.isDebug3()) {
	logger.debug3("Crawl priority: " + pri + ": " + req.getAu().getName());
      }
      req.setPriority(pri);
    }
  }

  public int getAuPriority(ArchivalUnit au) {
    return getAuPriority(au, Integer.MAX_VALUE);
  }

  public int getAuPriority(ArchivalUnit au, int maxPri) {
    return Math.round(crawlPriorityAuMap.getMatch(au, crawlPriorityAuidMap.getMatch(au.getAuId(), 0, maxPri), (float)maxPri));
  }

  /** Orders AUs (wrapped in CrawlReq) by crawl priority:<ol>
   * <li>Explicit request priority
   * <li>Plugin registry AUs
   * <li>Crawl window reopened
   * <li>if crawlOrder==CreationDate:<ol>
   * <li>AU Creation date
   * <li>AUID
   * </ol>
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

      if (r1 == r2) {
	return 0;
      }
      // An AU may be deleted at any point here.  Avoid NPEs by getting all
      // info before inactive check.
      ArchivalUnit au1 = r1.getAu();
      ArchivalUnit au2 = r2.getAu();
      AuState aus1 = r1.getAuState();
      AuState aus2 = r2.getAuState();

      // ensure reqs representing inactive AUs sort last
      if (!r1.isActive()) {
	if (!r2.isActive()) {
	  // doesn't matter, but must return consistent order
	  return r1.getAuId().compareTo(r2.getAuId());
	} else {
	  return 1;
	}
      } else {
	if (!r2.isActive()) {
	  return -1;
	}
      }

      CompareToBuilder ctb = 
	new CompareToBuilder()
	.append(-r1.priority, -r2.priority)
	.append(!(au1 instanceof RegistryArchivalUnit),
		!(au2 instanceof RegistryArchivalUnit))
	.append(previousResultOrder(aus1.getLastCrawlResult()),
		previousResultOrder(aus2.getLastCrawlResult()));
      switch (paramCrawlOrder) {
      case CreationDate:
	ctb.append(aus1.getAuCreationTime(), aus2.getAuCreationTime());
	ctb.append(au1.getAuId(), au2.getAuId());
	break;
      case CrawlDate:
      default:
	ctb
	  .append(aus1.getLastCrawlAttempt(), aus2.getLastCrawlAttempt())
	  .append(aus1.getLastCrawlTime(), aus2.getLastCrawlTime())
// 	  .append(au1.toString(), au2.toString())
	  .append(System.identityHashCode(r1), System.identityHashCode(r2));
	break;
      }
      return ctb.toComparison();
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
	  if (!isInternalAu(au)) {
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
    // Activity lock prevents AUs with pending crawls from being
    // queued twice.  If ActivityRegulator goes away some other
    // mechanism will be needed.
    startNewContentCrawl(au, rc, null, null);
  }

  void startCrawl(CrawlReq req) {
    ArchivalUnit au = req.getAu();
    try {
      // doesn't return until thread available for next request
      handReqToPool(req);
    } catch (RuntimeException e) {
      logger.warning("Starting crawl: " + au.getName(), e);
    }
  }

  boolean shouldCrawlForNewContent(ArchivalUnit au) {
    try {
      boolean res = au.shouldCrawlForNewContent(AuUtil.getAuState(au));
      if (logger.isDebug3()) logger.debug3("Should " + (res ? "" : "not ") +
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
