/*
 * $Id: CrawlManagerImpl.java,v 1.72.2.1 2004-10-14 07:53:08 tlipkis Exp $
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
import org.apache.commons.collections.*;
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
  public static final String PARAM_ALLOW_CRAWLS =
    Configuration.PREFIX + "crawler.allowCrawls";
  static final boolean DEFAULT_ALLOW_CRAWLS = true;

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

  private static final long DEFAULT_NEW_CONTENT_CRAWL_EXPIRATION =
    10 * Constants.DAY;
  private static final long DEFAULT_REPAIR_CRAWL_EXPIRATION =
    5 * Constants.DAY;

  public static final float DEFAULT_REPAIR_FROM_CACHE_PERCENT = 0;


  //Tracking crawls for the status info
  private MultiMap crawlHistory = new MultiHashMap();

  private MultiMap runningCrawls = new MultiHashMap();


  private long contentCrawlExpiration;
  private long repairCrawlExpiration;
  private float percentRepairFromCache;
  private boolean allowCrawls = DEFAULT_ALLOW_CRAWLS;
  private static Logger logger = Logger.getLogger("CrawlManager");


  /**
   * start the plugin manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    super.startService();

    StatusService statusServ = theDaemon.getStatusService();
    statusServ.registerStatusAccessor(CRAWL_STATUS_TABLE_NAME,
				      new CrawlManagerStatus(this));
  }

  /**
   * stop the plugin manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
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
    allowCrawls =
      newConfig.getBooleanParam(PARAM_ALLOW_CRAWLS, DEFAULT_ALLOW_CRAWLS);
  }

  public void cancelAuCrawls(ArchivalUnit au) {
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

    // check with regulator and start repair
    Map locks = getRepairLocks(au, urls, lock);
    if (locks.size() > 0) {
      try {
        if (locks.size() < urls.size()) {
          cb = new FailingCallbackWrapper(cb);
        }
        Crawler crawler =
            makeRepairCrawler(au, au.getCrawlSpec(),
                              locks.keySet(), percentRepairFromCache);
        CrawlThread crawlThread =
            new CrawlThread(crawler, cb, cookie, locks.values());
        crawlHistory.put(au.getAuId(), crawler.getStatus());
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
    } else {
      logger.debug("Repair aborted due to activity lock.");
      try {
	cb.signalCrawlAttemptCompleted(false, cookie);
      } catch (Exception e) {
	logger.error("Callback threw", e);
      }
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
    Plugin plugin = au.getPlugin();
    return plugin.makeCachedUrlSet(au, new SingleNodeCachedUrlSetSpec(url));
  }

  public void startNewContentCrawl(ArchivalUnit au, CrawlManager.Callback cb,
                                   Object cookie, ActivityRegulator.Lock lock) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null AU");
    }
    if ((lock==null) || (lock.isExpired())) {
      lock = getNewContentLock(au);
    } else {
      lock.setNewActivity(ActivityRegulator.NEW_CONTENT_CRAWL,
                          contentCrawlExpiration);
    }
    if (lock != null) {
      try {
	scheduleNewContentCrawl(au, cb, cookie, lock);
      } catch (Exception e) {
	logger.error("scheduleNewContentCrawl threw", e);
      }
    } else {
      logger.debug("Couldn't schedule new content crawl due "+
		   "to activity lock.");
      callCallback(cb, cookie, false);
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
				   boolean successful) {
    if (cb != null) {
      try {
	cb.signalCrawlAttemptCompleted(successful, cookie);
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
      crawlHistory.put(au.getAuId(), crawler.getStatus());
      synchronized (runningCrawls) {
        runningCrawls.put(au, crawler);
      }
      crawlThread.start();
    } catch (RuntimeException re) {
      logger.warning("Error starting crawl, freeing crawl lock", re);
      lock.expire();
      callCallback(cb, cookie, false);
      throw re;
    }
    crawlThread.waitRunning();
  }

  protected Crawler makeNewContentCrawler(ArchivalUnit au, CrawlSpec spec) {
    NodeManager nodeManager = theDaemon.getNodeManager(au);
    return new NewContentCrawler(au, spec, nodeManager.getAuState());
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
	if (!allowCrawls) {
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

	  if (crawler.getType() == Crawler.NEW_CONTENT) {
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
	callCallback(cb, cookie, crawlSuccessful);
      }
    }
  }

  private static class FailingCallbackWrapper
    implements CrawlManager.Callback {
    CrawlManager.Callback cb;
    public FailingCallbackWrapper(CrawlManager.Callback cb) {
      this.cb = cb;
    }
    public void signalCrawlAttemptCompleted(boolean success, Object cookie) {
      callCallback(cb, cookie, false);
    }
  }

  //CrawlManager.StartSource methods
  public Collection getActiveAus() {
    return crawlHistory.keySet();
  }

  public Collection getCrawlStatus(String auid) {
    Collection returnColl = (Collection)crawlHistory.get(auid);
    return returnColl != null ? returnColl : Collections.EMPTY_LIST;
  }
}
