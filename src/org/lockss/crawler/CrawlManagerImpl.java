/*
 * $Id: CrawlManagerImpl.java,v 1.38 2003-06-25 21:19:59 eaalto Exp $
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
import java.net.URL;
import java.util.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.state.NodeState;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.state.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;


/**
 * This is the interface for the object which will sit between the crawler
 * and the rest of the world.  It mediates the different crawl types.
 */
public class CrawlManagerImpl extends BaseLockssManager
    implements CrawlManager {

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

  /**
   * The priority that the crawl thread should run at
   */
  public static final String PARAM_PRIORITY =
      Configuration.PREFIX + "crawler.priority";

  /**
   * ToDo:
   * 1)make a crawl to the AU to decide if I should do a new content crawl
   * 2)handle background crawls
   * 3)handle repair crawls
   * 4)check for conflicting crawl types
   * 5)check crawl schedule rules
   */
  private static final String CRAWL_STATUS_TABLE_NAME = "crawl_status_table";

  private static final long DEFAULT_NEW_CONTENT_CRAWL_EXPIRATION =
    10 * Constants.DAY;
  private static final long DEFAULT_REPAIR_CRAWL_EXPIRATION =
    5 * Constants.DAY;
  private static final int DEFAULT_PRIORITY = Thread.NORM_PRIORITY-1;

  private Map newContentCrawls = new HashMap();
  private Map repairCrawls = new HashMap();
  private Set activeCrawls = new HashSet();

  private long contentCrawlExpiration;
  private long repairCrawlExpiration;
  private int crawlPriority = DEFAULT_PRIORITY;
  private static Logger logger = Logger.getLogger("CrawlManagerImpl");


  /**
   * start the plugin manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    super.startService();

    StatusService statusServ = theDaemon.getStatusService();
    statusServ.registerStatusAccessor(CRAWL_STATUS_TABLE_NAME, new Status());
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

  protected void setConfig(Configuration newConfig, Configuration prevConfig,
			   Set changedKeys) {
    contentCrawlExpiration =
      newConfig.getTimeInterval(PARAM_NEW_CONTENT_CRAWL_EXPIRATION,
				DEFAULT_NEW_CONTENT_CRAWL_EXPIRATION);
    repairCrawlExpiration =
      newConfig.getTimeInterval(PARAM_REPAIR_CRAWL_EXPIRATION,
				DEFAULT_REPAIR_CRAWL_EXPIRATION);

    crawlPriority = newConfig.getInt(PARAM_PRIORITY, DEFAULT_PRIORITY);
  }

  /**
   * Schedules a repair crawl and calls cb.signalRepairAttemptCompleted
   * when done.
   * @param au the AU being crawled
   * @param url URL that needs to be repaired
   * @param cb callback to talk to when repair attempt is done
   * @param cookie object that the callback needs to understand which
   * repair we're referring to.
   */
  public void scheduleRepair(ArchivalUnit au, URL url,
			     CrawlManager.Callback cb, Object cookie) {
    if (url == null) {
      throw new IllegalArgumentException("Called with null URL");
    }
    scheduleRepair(au, ListUtil.list(url), cb, cookie);
  }

  public void scheduleRepair(ArchivalUnit au, List urls,
			     CrawlManager.Callback cb, Object cookie) {
    //XXX check to make sure no other crawls are running and queue if they are
    if (au == null) {
      throw new IllegalArgumentException("Called with null AU");
    } 
    if (urls == null) {
      throw new IllegalArgumentException("Called with null URL");
    }

    List urlStrs = new ArrayList();
    for (Iterator it = urls.iterator(); it.hasNext();) {
      URL curUrl = (URL)it.next();
      urlStrs.add(curUrl.toString());
    }


    // check with regulator and start repair
    Map locks = getRepairLocks(au, urlStrs);
    if (locks != null && locks.size() > 0) {
      List urlsToRepair = makeList(locks.keySet());
      Crawler crawler = makeCrawler(au, urlsToRepair, Crawler.REPAIR, false);
      CrawlThread crawlThread = new CrawlThread(crawler, Deadline.MAX,
						ListUtil.list(cb),
						cookie);
      crawlThread.start();
      addCrawl(repairCrawls, au, crawler);
    } else {
      logger.debug3("Repair aborted due to activity lock.");
      cb.signalCrawlAttemptCompleted(false, cookie);
    }
  }

  private List makeList(Set strSet) {
    List list = new ArrayList(strSet.size());
    for (Iterator it = strSet.iterator(); it.hasNext();) {
      String str = (String)it.next();
      list.add(str);
    }
    return list;
  }

  private ActivityRegulator.Lock getRepairLock(ArchivalUnit au, String url) {
    ActivityRegulator ar = theDaemon.getActivityRegulator(au);
    return ar.startCusActivity(ActivityRegulator.REPAIR_CRAWL,
			       createSingleNodeCachedUrlSet(au, url),
			       repairCrawlExpiration);
  }

  private Map getRepairLocks(ArchivalUnit au, List urlStrs) {
    Map locks = new HashMap();
    ActivityRegulator ar = theDaemon.getActivityRegulator(au);
    for (Iterator it = urlStrs.iterator(); it.hasNext();) {
      String url = (String)it.next();
      ActivityRegulator.Lock lock =
	ar.startCusActivity(ActivityRegulator.REPAIR_CRAWL,
			    createSingleNodeCachedUrlSet(au, url),
			    repairCrawlExpiration);
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
                                   Object cookie) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null AU");
    }
    ActivityRegulator.Lock lock = getNewContentLock(au);
    if (lock != null) {
      activeCrawls.add(au);
      scheduleNewContentCrawl(au, cb, cookie, lock);
    } else {
      logger.debug2("Couldn't schedule new content crawl due "
		    +"to activity lock.");
      cb.signalCrawlAttemptCompleted(false, cookie);
    }
  }

  private ActivityRegulator.Lock getNewContentLock(ArchivalUnit au) {
    ActivityRegulator ar = theDaemon.getActivityRegulator(au);
    return ar.startAuActivity(ActivityRegulator.NEW_CONTENT_CRAWL,
			      contentCrawlExpiration);
  }

  public boolean shouldRecrawl(ArchivalUnit au, NodeState ns) {
    //XXX implement
    return false;
  }

  private void scheduleNewContentCrawl(ArchivalUnit au,
                                       CrawlManager.Callback cb, Object cookie,
                                       ActivityRegulator.Lock lock) {
    List callBackList =
      ListUtil.list(new UpdateNewCrawlTimeCB(theDaemon.getNodeManager(au)));
    if (cb != null) {
      callBackList.add(cb);
    }
    Crawler crawler = makeCrawler(au, au.getNewContentCrawlUrls(),
				  Crawler.NEW_CONTENT, true);
    CrawlThread crawlThread = new CrawlThread(crawler, Deadline.MAX,
					      callBackList, cookie);
    crawlThread.start();
    addCrawl(newContentCrawls, au, crawler);
  }

  private void addCrawl(Map crawlMap, ArchivalUnit au, Crawler crawler) {
    synchronized (crawlMap) {
      List crawlsForAu = (List)crawlMap.get(au.getAUId());
      if (crawlsForAu == null) {
	crawlsForAu = new ArrayList();
	crawlMap.put(au.getAUId(), crawlsForAu);
      }
      crawlsForAu.add(crawler);
    }
  }

  private void triggerCrawlCallbacks(Vector callbacks) {
    if (callbacks != null) {
      Iterator it = callbacks.iterator();
      while (it.hasNext()) {
	CrawlManager.Callback cb = (CrawlManager.Callback) it.next();
	cb.signalCrawlAttemptCompleted(true, null);
      }
    }
  }

  protected Crawler makeCrawler(ArchivalUnit au, List urls,
				int type, boolean followLinks) {
    return new GoslingCrawlerImpl(au, urls, type, followLinks);
  }


  public class CrawlThread extends Thread {
    private Deadline deadline;
    private List callbacks;
    private Object cookie;
    private Crawler crawler;

    private CrawlThread(Crawler crawler, Deadline deadline,
			List callbacks, Object cookie) {
      super(crawler.toString());
      this.deadline = deadline;
      this.callbacks = callbacks;
      this.cookie = cookie;
      this.crawler = crawler;
    }

    public void run() {
      if (crawlPriority > 0) {
	logger.debug("Setting crawl thread priority to "+crawlPriority);
	Thread.currentThread().setPriority(crawlPriority);
      } else {
	logger.warning("Crawl thread priority less than zero, not set: "
		       +crawlPriority);
      }
      boolean crawlSuccessful = crawler.doCrawl(deadline);
      ArchivalUnit au = crawler.getAU();
      activeCrawls.remove(au);

      // if followLinks is true, assume it's a new content crawl
      // free activity regulator so polls can resume
      ActivityRegulator ar = theDaemon.getActivityRegulator(au);

      if (crawler.getType() == Crawler.NEW_CONTENT) {
        ar.auActivityFinished(ActivityRegulator.NEW_CONTENT_CRAWL);
      } else {
	releaseRepairLocks(au, crawler.getStartUrls());
// 	String url = (String)crawler.getStartUrls().get(0); //XXX hack
//         ar.cusActivityFinished(ActivityRegulator.REPAIR_CRAWL,
// 			       createSingleNodeCachedUrlSet(au, url));
// 	url = (String)crawler.getStartUrls().get(1); //XXX hack
//         ar.cusActivityFinished(ActivityRegulator.REPAIR_CRAWL,
// 			       createSingleNodeCachedUrlSet(au, url));
      }

      doCallbacks(callbacks, crawlSuccessful, cookie);
    }
  }

  private void releaseRepairLocks(ArchivalUnit au, List urlStrs) {
    ActivityRegulator ar = theDaemon.getActivityRegulator(au);
    for (Iterator it = urlStrs.iterator(); it.hasNext();) {
      String curUrl = (String)it.next();
      ar.cusActivityFinished(ActivityRegulator.REPAIR_CRAWL,
			     createSingleNodeCachedUrlSet(au, curUrl));
    }  
  }

  private void doCallbacks(List callbacks, boolean crawlSucessful,
			   Object cookie) {
    if (callbacks != null) {
      Iterator it = callbacks.iterator();
      while (it.hasNext()) {
	CrawlManager.Callback cb = (CrawlManager.Callback) it.next();
	cb.signalCrawlAttemptCompleted(crawlSucessful, cookie);
      }
    }
  }

  private class UpdateNewCrawlTimeCB implements CrawlManager.Callback {
    NodeManager nodeManager;

    public UpdateNewCrawlTimeCB(NodeManager nodeManager) {
      this.nodeManager = nodeManager;
    }

    public void signalCrawlAttemptCompleted(boolean success, Object cookie) {
      if (success) {
	logger.debug3("Signaling nodeManager that the crawl sucessfully "
		      +"completed");
	nodeManager.newContentCrawlFinished();
      }
    }
  }


  private class Status implements StatusAccessor {

    private static final String AU_COL_NAME = "au";
    private static final String CRAWL_TYPE = "crawl_type";
    private static final String START_TIME_COL_NAME = "start";
    private static final String END_TIME_COL_NAME = "end";
    private static final String NUM_URLS_PARSED = "num_urls_parsed";
    private static final String NUM_URLS_FETCHED = "num_urls_fetched";
    private static final String NUM_CACHE_HITS = "num_cache_hits";
    private static final String CACHE_HITS_PERCENT = "cache_hits_percent";
    private static final String START_URLS = "start_urls";

    private List colDescs =
      ListUtil.list(
		    new ColumnDescriptor(AU_COL_NAME, "Journal Volume",
					 ColumnDescriptor.TYPE_STRING),
		    new ColumnDescriptor(CRAWL_TYPE, "Crawl Type",
					 ColumnDescriptor.TYPE_STRING),
		    new ColumnDescriptor(START_TIME_COL_NAME, "Start Time",
					 ColumnDescriptor.TYPE_DATE),
		    new ColumnDescriptor(END_TIME_COL_NAME, "End Time",
					 ColumnDescriptor.TYPE_DATE),
		    new ColumnDescriptor(NUM_URLS_FETCHED, "URLs fetched",
					 ColumnDescriptor.TYPE_INT),
		    new ColumnDescriptor(NUM_URLS_PARSED, "URLs parsed",
					 ColumnDescriptor.TYPE_INT),
		    new ColumnDescriptor(NUM_CACHE_HITS, "Cache hits",
					 ColumnDescriptor.TYPE_INT),
		    new ColumnDescriptor(CACHE_HITS_PERCENT, "percent",
					 ColumnDescriptor.TYPE_PERCENT),
		    new ColumnDescriptor(START_URLS, "starting url",
					 ColumnDescriptor.TYPE_STRING)
		    );


    private static final String NC_TYPE = "New Content";
    private static final String REPAIR_TYPE = "Repair";

    private List getRows(String key) {
      List rows = new ArrayList();
      if (key == null) {
	return getAllCrawls();
      }

      addCrawlsFromMap(key, newContentCrawls, NC_TYPE, rows);
      addCrawlsFromMap(key, repairCrawls, REPAIR_TYPE, rows);
      return rows;
    }

    private void addCrawlsFromMap(String key, Map crawlMap,
				  String type, List rows) {
      synchronized(crawlMap) {
	List crawlsForAu = (List) crawlMap.get(key);
	if (crawlsForAu != null) {
	  Iterator it = crawlsForAu.iterator();
	  while (it.hasNext()) {
	    rows.add(makeRow(type, (Crawler) it.next()));
	  }
	}
      }
    }

    private List getAllCrawls() {
      List list = new ArrayList();
      getAllCrawlsFromMap(list, NC_TYPE, newContentCrawls);
      getAllCrawlsFromMap(list, REPAIR_TYPE, repairCrawls);
      return list;
    }

    private void getAllCrawlsFromMap(List rows, String type, Map crawlMap) {
      synchronized(crawlMap) {
	Iterator keys = crawlMap.keySet().iterator();
	while (keys.hasNext()) {
	  List crawls = (List)crawlMap.get((String)keys.next());
	  Iterator it = crawls.iterator();
	  while (it.hasNext()) {
	    rows.add(makeRow(type, (Crawler)it.next()));
	  }
	}
      }
    }

    private Map makeRow(String type, Crawler crawler) {
      Map row = new HashMap();
      ArchivalUnit au = crawler.getAU();
      row.put(AU_COL_NAME, au.getName());
      row.put(CRAWL_TYPE, type);
      row.put(START_TIME_COL_NAME, makeNullOrLong(crawler.getStartTime()));
      row.put(END_TIME_COL_NAME, makeNullOrLong(crawler.getEndTime()));
      row.put(NUM_URLS_FETCHED, new Long(crawler.getNumFetched()));
      row.put(NUM_URLS_PARSED, new Long(crawler.getNumParsed()));
      row.put(START_URLS,
	      (ListUtil.listToString(crawler.getStartUrls(), "\n")));
      if (au instanceof BaseArchivalUnit) {
	BaseArchivalUnit bau = (BaseArchivalUnit)au;
	row.put(NUM_CACHE_HITS, new Long(bau.getCrawlSpecCacheHits()));
	double per = ((float)bau.getCrawlSpecCacheHits() /
		      ((float)bau.getCrawlSpecCacheHits() +
		       (float)bau.getCrawlSpecCacheMisses()));
	row.put(CACHE_HITS_PERCENT, new Double(per));
      }
      return row;
    }

    public boolean requiresKey() {
      return false;
    }

    public void populateTable(StatusTable table) {
      String key = table.getKey();
      table.setTitle("Crawl Status");
      table.setColumnDescriptors(colDescs);
      table.setRows(getRows(key));
    }

    /**
     * @param num the long
     * @return null if num <= 0, a Long version of num otherwise
     */
    private Long makeNullOrLong(long num) {
      return num <= 0 ? null : new Long(num);
    }
  }

}
