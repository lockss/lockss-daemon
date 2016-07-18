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

package org.lockss.daemon;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.plugin.PluginManager;
import org.lockss.util.*;

/**
 * A singleton class which provides access to various system calculations.
 */
public class SystemMetrics
  extends BaseLockssDaemonManager implements ConfigurableManager {
  static final String PREFIX = Configuration.PREFIX + "metrics.";

  /**
   * The interval at which memory usage should be logged, or 0 to disable
   */
  public static final String PARAM_MEM_LOG_INTERVAL =
    PREFIX + "logMem.interval";
  static final long DEFAULT_MEM_LOG_INTERVAL = 0;

  private static Logger logger = Logger.getLogger("SystemMetrics");

  private PluginManager pluginMgr;
  private TimerQueue.Request req;
  private long memLogInterval = DEFAULT_MEM_LOG_INTERVAL;

  public void startService() {
    super.startService();
    pluginMgr = getDaemon().getPluginManager();
    resetConfig();
  }

  public void setConfig(Configuration newConfig,
			Configuration prevConfig,
			Configuration.Differences changedKeys) {
    if (changedKeys.contains(PARAM_MEM_LOG_INTERVAL)) {
      memLogInterval = newConfig.getTimeInterval(PARAM_MEM_LOG_INTERVAL,
						 DEFAULT_MEM_LOG_INTERVAL);
      if (memLogInterval > 0) {
	doMemLog();
      }
    }
  }

  public synchronized void stopService() {
    if (req != null) {
      TimerQueue.cancel(req);
    }
    super.stopService();
  }

  void schedMemLog() {
    if (req != null) {
      TimerQueue.cancel(req);
    }
    if (memLogInterval > 0) {
      req = TimerQueue.schedule(Deadline.in(memLogInterval),
				memLogCallback, null);
    }
  }

  // Memory logger TimerQueue callback.
  private TimerQueue.Callback memLogCallback =
    new TimerQueue.Callback() {
      public void timerExpired(Object cookie) {
	doMemLog();
      }
      public String toString() {
	return "Memory logger";
      }
    };

  // Log the current memory usage
  private void doMemLog() {
    Runtime rt = Runtime.getRuntime();
    long tot = rt.totalMemory();
    long free = rt.freeMemory();
    long max = rt.maxMemory();
    logger.info("Memory Used: " + K(tot - free) +
		", Free: " + K(free) +
		", Total: " + K(tot) +
		", Max: " + K(max) +
		( (pluginMgr == null) ? "" :
		  (", " + pluginMgr.getAllAus().size() + " AUs")));
    if (logger.isDebug2()) {
      logger.debug2("String Pools:\n" + StringPool.allStats());
    }
    schedMemLog();
  }

  private String K(long n) {
    return StringUtil.sizeToString(n);
  }
}
