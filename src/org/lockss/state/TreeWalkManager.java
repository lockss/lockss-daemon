/*
 * $Id: TreeWalkManager.java,v 1.5 2006-10-06 20:11:53 tlipkis Exp $
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

package org.lockss.state;

import java.util.*;
import EDU.oswego.cs.dl.util.concurrent.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;

/**
 * TreeWalkManager is the center of the treewalk managers.  It manages the
 * treewalk config parameters and thread pool.
 */
public class TreeWalkManager
  extends BaseLockssManager implements ConfigurableManager {

  private static Logger log = Logger.getLogger("TreeWalkManager");

  public static final String PREFIX = Configuration.PREFIX + "treewalk.";

  /** Min threads in treewalk thread pool */
  public static final String PARAM_TREEWALK_THREAD_POOL_MIN =
    PREFIX + "threadPool.min";
  static final int DEFAULT_TREEWALK_THREAD_POOL_MIN = 2;

  /** Max threads in treewalk thread pool */
  public static final String PARAM_TREEWALK_THREAD_POOL_MAX =
    PREFIX + "threadPool.max";
  static final int DEFAULT_TREEWALK_THREAD_POOL_MAX = 6;

  /** Duration after which idle threads will be terminated..  -1 = never */
  public static final String PARAM_TREEWALK_THREAD_POOL_KEEPALIVE =
    PREFIX + "threadPool.keepAlive";
  static final long DEFAULT_TREEWALK_THREAD_POOL_KEEPALIVE =
    30 * Constants.MINUTE;



  /** Approximate interval between treewalks.
   * @deprecated replaced by interval.min and interval.max
   */
  public static final String PARAM_TREEWALK_INTERVAL = PREFIX + "interval";
  static final long DEFAULT_TREEWALK_INTERVAL = Constants.HOUR;

  /** Min interval between treewalks. */
  public static final String PARAM_TREEWALK_INTERVAL_MIN =
    PREFIX + "interval.min";
  static final long DEFAULT_TREEWALK_INTERVAL_MIN = 12 * Constants.HOUR;

  /** Max targeted interval between treewalks - it may be longer than that. */
  public static final String PARAM_TREEWALK_INTERVAL_MAX =
    PREFIX + "interval.max";
  static final long DEFAULT_TREEWALK_INTERVAL_MAX = 36 * Constants.HOUR;

  /** Additional delay before first treewalk */
  public static final String PARAM_TREEWALK_START_DELAY =
    PREFIX + "start.delay";
  static final long DEFAULT_TREEWALK_START_DELAY = Constants.MINUTE;

  /** Initial treewalk duration estimate. */
  public static final String PARAM_TREEWALK_INITIAL_ESTIMATE =
    PREFIX + "initial.estimate";
  static final long DEFAULT_TREEWALK_INITIAL_ESTIMATE = 30 * Constants.MINUTE;
  static final long MIN_TREEWALK_ESTIMATE = 10 * Constants.SECOND;

  /** Factor by which to increase duration estimate if not long enough */
  public static final String PARAM_TREEWALK_ESTIMATE_GROWTH_MULTIPLIER =
      PREFIX + "estimate.growth.multiplier";
  static final double DEFAULT_TREEWALK_ESTIMATE_GROWTH_MULTIPLIER = 1.5;

  /** Factor by which to pad duration estimate when scheduling */
  public static final String PARAM_TREEWALK_ESTIMATE_PADDING_MULTIPLIER =
      PREFIX + "estimate.padding.multiplier";
  static final double DEFAULT_TREEWALK_ESTIMATE_PADDING_MULTIPLIER = 1.25;

  /** Occasional sleep interval, used to maintain proper CPU load factor */
  public static final String PARAM_TREEWALK_SLEEP_INTERVAL =
      PREFIX + "sleep.interval";
  static final long DEFAULT_TREEWALK_SLEEP_INTERVAL = Constants.SECOND;
  static final long MIN_TREEWALK_SLEEP_INTERVAL = 500;

  /** Target CPU load factor (assumes uses 100% when running) */
  public static final String PARAM_TREEWALK_LOAD_FACTOR =
    PREFIX + "load.factor";
  static final double DEFAULT_TREEWALK_LOAD_FACTOR = .90;

  /** Min scheduled treewalk task duration */
  public static final String PARAM_TREEWALK_MIN_DURATION =
    PREFIX + "minDuration";
  static final long DEFAULT_TREEWALK_MIN_DURATION = 10 * Constants.SECOND;

  /** Max distance in future to attempt to schedule treewalk.  If can't
   * schedule before this, sleep for a while then try again */
  public static final String PARAM_TREEWALK_MAX_FUTURE_SCHED =
    PREFIX + "maxFutureSched";
  static final long DEFAULT_TREEWALK_MAX_FUTURE_SCHED = 3 * Constants.WEEK;

  /** Time to sleep before trying schedule loop again, if failed to
   * schedule before maxFutureSched */
  public static final String PARAM_TREEWALK_SCHED_FAIL_RETRY =
    PREFIX + "schedFailRetry";
  static final long DEFAULT_TREEWALK_SCHED_FAIL_RETRY = 1 * Constants.HOUR;

  /** Amount to push back task start time each time through scheduling loop */
  public static final String PARAM_TREEWALK_SCHED_FAIL_INCREMENT =
    PREFIX + "schedFailIncrement";
  static final long DEFAULT_TREEWALK_SCHED_FAIL_INCREMENT = 1 * Constants.HOUR;

  /** Min delay before retrying treewalk if it failed to execute
   * (<i>eg</i>, because it couldn't acquire the activity lock) */
  public static final String PARAM_TREEWALK_EXECUTE_FAIL_RETRY =
    PREFIX + "executeFailRetry";
  static final long DEFAULT_TREEWALK_EXECUTE_FAIL_RETRY = 1 * Constants.HOUR;

  /** V1 treewalk mode: <code>full</code> (default), pollstart */
  public static final String PARAM_TREEWALK_V1_MODE = PREFIX + "v1.mode";
  static final String DEFAULT_TREEWALK_V1_MDOE = "full";

  /** V3 treewalk mode: <code>full</code> (default), pollstart */
  public static final String PARAM_TREEWALK_V3_MODE = PREFIX + "v3.mode";
  static final String DEFAULT_TREEWALK_V3_MDOE = "full";

  static final String WDOG_PARAM_TREEWALK = "TreeWalk";
  static final long WDOG_DEFAULT_TREEWALK = 30 * Constants.MINUTE;

  static final String PRIORITY_PARAM_TREEWALK = "TreeWalk";
  static final int PRIORITY_DEFAULT_TREEWALK = -1;

  // Config parameter values.  For convenience and efficiency these are all
  // collected here and accessed directly by AuTreeWalkManager,
  // V1TreeWalkImpl, etc.

  int paramMinPoolSize = DEFAULT_TREEWALK_THREAD_POOL_MIN;
  int paramMaxPoolSize = DEFAULT_TREEWALK_THREAD_POOL_MAX;
  long paramPoolKeepaliveTime = DEFAULT_TREEWALK_THREAD_POOL_KEEPALIVE;
  long paramTreeWalkIntervalMin = DEFAULT_TREEWALK_INTERVAL_MIN;
  long paramTreeWalkIntervalMax = DEFAULT_TREEWALK_INTERVAL_MAX;
  long paramStartDelay = DEFAULT_TREEWALK_START_DELAY;
  long paramInitialEstimate = DEFAULT_TREEWALK_INITIAL_ESTIMATE;
  double paramEstGrowth = DEFAULT_TREEWALK_ESTIMATE_GROWTH_MULTIPLIER;
  double paramEstPadding = DEFAULT_TREEWALK_ESTIMATE_PADDING_MULTIPLIER;
  long paramMinSchedDuration = DEFAULT_TREEWALK_MIN_DURATION;
  long paramMaxFutureSched = DEFAULT_TREEWALK_MAX_FUTURE_SCHED;
  long paramSchedFailRetryTime = DEFAULT_TREEWALK_SCHED_FAIL_RETRY;
  long paramSchedFailIncrement = DEFAULT_TREEWALK_SCHED_FAIL_INCREMENT;
  long paramExecuteFailRetryTime = DEFAULT_TREEWALK_EXECUTE_FAIL_RETRY;
  long paramSleepInterval = DEFAULT_TREEWALK_SLEEP_INTERVAL;
  double paramLoadFactor = DEFAULT_TREEWALK_LOAD_FACTOR;
  String paramV1Mode = DEFAULT_TREEWALK_V1_MDOE;
  String paramV3Mode = DEFAULT_TREEWALK_V3_MDOE;

  PooledExecutor pool;

  public void startService() {
    super.startService();
    // sets the managers
    pool = new PooledExecutor(paramMaxPoolSize);
    pool.setMinimumPoolSize(paramMinPoolSize);
    pool.setKeepAliveTime(paramPoolKeepaliveTime);
    log.debug2("Treewalk thread pool min, max: " +
	      pool.getMinimumPoolSize() + ", " + pool.getMaximumPoolSize());
    pool.abortWhenBlocked();
  }

  /** Stop all running treewalks */
  public void stopService() {
    pool.shutdownNow();
    super.stopService();
  }

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    if (changedKeys.contains(PREFIX)) {
      paramMinPoolSize = config.getInt(PARAM_TREEWALK_THREAD_POOL_MIN,
				       DEFAULT_TREEWALK_THREAD_POOL_MIN);
      paramMaxPoolSize = config.getInt(PARAM_TREEWALK_THREAD_POOL_MAX,
				       DEFAULT_TREEWALK_THREAD_POOL_MAX);
      paramPoolKeepaliveTime =
	config.getTimeInterval(PARAM_TREEWALK_THREAD_POOL_KEEPALIVE,
			       DEFAULT_TREEWALK_THREAD_POOL_KEEPALIVE);

      // duplicate old behavior if only old param is set.  XXX remove this
      if (config.containsKey(PARAM_TREEWALK_INTERVAL) &&
	  !config.containsKey(PARAM_TREEWALK_INTERVAL_MIN) &&
	  !config.containsKey(PARAM_TREEWALK_INTERVAL_MAX)) {
	paramTreeWalkIntervalMin =
	  config.getTimeInterval(PARAM_TREEWALK_INTERVAL,
				 DEFAULT_TREEWALK_INTERVAL);
	paramTreeWalkIntervalMax = (long) (paramTreeWalkIntervalMin * 1.4);
      } else {
	paramTreeWalkIntervalMin =
	  config.getTimeInterval(PARAM_TREEWALK_INTERVAL_MIN,
				 DEFAULT_TREEWALK_INTERVAL_MIN);
	paramTreeWalkIntervalMax =
	  config.getTimeInterval(PARAM_TREEWALK_INTERVAL_MAX,
				 DEFAULT_TREEWALK_INTERVAL_MAX);
      }
      paramStartDelay =
	config.getTimeInterval(PARAM_TREEWALK_START_DELAY,
			       DEFAULT_TREEWALK_START_DELAY);
      paramInitialEstimate =
	config.getTimeInterval(PARAM_TREEWALK_INITIAL_ESTIMATE,
			       DEFAULT_TREEWALK_INITIAL_ESTIMATE);
      paramEstGrowth =
	config.getPercentage(PARAM_TREEWALK_ESTIMATE_GROWTH_MULTIPLIER,
			     DEFAULT_TREEWALK_ESTIMATE_GROWTH_MULTIPLIER);
      paramEstPadding =
	config.getPercentage(PARAM_TREEWALK_ESTIMATE_PADDING_MULTIPLIER,
			     DEFAULT_TREEWALK_ESTIMATE_PADDING_MULTIPLIER);
      paramMinSchedDuration =
	config.getTimeInterval(PARAM_TREEWALK_MIN_DURATION,
			       DEFAULT_TREEWALK_MIN_DURATION);
      paramMaxFutureSched =
	config.getTimeInterval(PARAM_TREEWALK_MAX_FUTURE_SCHED,
			       DEFAULT_TREEWALK_MAX_FUTURE_SCHED);
      paramSchedFailRetryTime =
	config.getTimeInterval(PARAM_TREEWALK_SCHED_FAIL_RETRY,
			       DEFAULT_TREEWALK_SCHED_FAIL_RETRY);
      paramSchedFailIncrement =
	config.getTimeInterval(PARAM_TREEWALK_SCHED_FAIL_INCREMENT,
			       DEFAULT_TREEWALK_SCHED_FAIL_INCREMENT);
      paramExecuteFailRetryTime =
	config.getTimeInterval(PARAM_TREEWALK_EXECUTE_FAIL_RETRY,
			       DEFAULT_TREEWALK_EXECUTE_FAIL_RETRY);
      paramSleepInterval =
	Math.min(MIN_TREEWALK_SLEEP_INTERVAL,
		 config.getTimeInterval(PARAM_TREEWALK_SLEEP_INTERVAL,
					DEFAULT_TREEWALK_SLEEP_INTERVAL));
      paramLoadFactor = config.getPercentage(PARAM_TREEWALK_LOAD_FACTOR,
					     DEFAULT_TREEWALK_LOAD_FACTOR);
      paramV1Mode = config.get(PARAM_TREEWALK_V1_MODE,
			       DEFAULT_TREEWALK_V1_MDOE);
      paramV3Mode = config.get(PARAM_TREEWALK_V3_MODE,
			       DEFAULT_TREEWALK_V3_MDOE);
    }
  }

  /**
   * Execute the runnable in a pool thread
   * @param run the Runnable to be run
   * @throws InterruptedException
   * @throws RuntimeException if no pool thread is available
   */
  void execute(Runnable run) throws InterruptedException {
    pool.execute(run);
  }

}
