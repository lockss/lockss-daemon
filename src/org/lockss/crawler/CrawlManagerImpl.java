/*
 * $Id: CrawlManagerImpl.java,v 1.25 2003-04-18 20:56:48 aalto Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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
   * ToDo:
   * 1)make a crawl to the AU to decide if I should do a new content crawl
   * 2)handle background crawls
   * 3)handle repair crawls
   * 4)check for conflicting crawl types
   * 5)check crawl schedule rules
   */
  private static final String CRAWL_STATUS_TABLE_NAME = "crawl_status_table";

  private static final long DEFAULT_NEW_CONTENT_CRAWL_EXPIRATION =
      5 * Constants.DAY;
  private static final long DEFAULT_REPAIR_CRAWL_EXPIRATION = 5 * Constants.DAY;

  private Map newContentCrawls = new HashMap();
  private static ActivityRegulator regulator;
  private Set activeCrawls = new HashSet();

  private long contentCrawlExpiration;
  private long repairCrawlExpiration;
  private static Logger logger = Logger.getLogger("CrawlManagerImpl");


  /**
   * start the plugin manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    super.startService();
    regulator = theDaemon.getActivityRegulator();

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
    contentCrawlExpiration = newConfig.getTimeInterval(
        PARAM_NEW_CONTENT_CRAWL_EXPIRATION,
        DEFAULT_NEW_CONTENT_CRAWL_EXPIRATION);
    repairCrawlExpiration = newConfig.getTimeInterval(
        PARAM_REPAIR_CRAWL_EXPIRATION, DEFAULT_REPAIR_CRAWL_EXPIRATION);
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
			     CrawlManager.Callback cb, Object cookie){
    //XXX check to make sure no other crawls are running and queue if they are
    if (au == null) {
      throw new IllegalArgumentException("Called with null AU");
    } if (url == null) {
      throw new IllegalArgumentException("Called with null URL");
    }

    // check with regulator and start repair
    if (regulator.startCusActivity(ActivityRegulator.REPAIR_CRAWL,
                                   au.makeCachedUrlSet(
        new SingleNodeCachedUrlSetSpec(url.toString())),
                                   repairCrawlExpiration)) {
      CrawlThread crawlThread =
          new CrawlThread(au, ListUtil.list(url.toString()),
                          false, Deadline.MAX, ListUtil.list(cb), cookie);
      crawlThread.start();
    } else {
      logger.debug3("Repair aborted due to activity lock.");
    }
  }

  public boolean isCrawlingAU(ArchivalUnit au, CrawlManager.Callback cb,
				  Object cookie){
    if (au == null) {
      throw new IllegalArgumentException("Called with null AU");
    }
    logger.debug3("Checking to see if we should do a new content crawl on "+
		  au);
    NodeManager nodeManager = theDaemon.getNodeManager(au);
    synchronized (activeCrawls) {
      if (activeCrawls.contains(au)) {
	logger.debug3("Already crawling "+ au
		      +", not starting new content crawl");
	return true;
      }
      if (au.shouldCrawlForNewContent(nodeManager.getAuState())) {
	logger.debug3("No crawls for "+au+", scheduling new content crawl");
	activeCrawls.add(au);
	scheduleNewContentCrawl(au, cb, cookie);
	return true;
      }
    }
    return false;
  }

  public void startNewContentCrawl(ArchivalUnit au, CrawlManager.Callback cb,
                                   Object cookie) {
    logger.debug3("Scheduling new content crawl for AU '" + au.getName() + "'");
    if (regulator.startAuActivity(ActivityRegulator.NEW_CONTENT_CRAWL,
                                  au, contentCrawlExpiration)) {
      activeCrawls.add(au);
      scheduleNewContentCrawl(au, cb, cookie);
    } else {
      logger.debug2("Couldn't schedule new content crawl due to activity lock.");
    }
  }

  public boolean shouldRecrawl(ArchivalUnit au, NodeState ns) {
    //XXX implement
    return false;
  }

  private void scheduleNewContentCrawl(ArchivalUnit au,
				       CrawlManager.Callback cb,
				       Object cookie) {
    List callBackList =
      ListUtil.list(new UpdateNewCrawlTimeCB(theDaemon.getNodeManager(au)));
    if (cb != null) {
      callBackList.add(cb);
    }

    CrawlThread crawlThread =
      new CrawlThread(au, au.getNewContentCrawlUrls(),
		      true, Deadline.MAX, callBackList, cookie);
    crawlThread.start();
    addNewContentCrawl(au, crawlThread.getCrawler());
  }

  private void addNewContentCrawl(ArchivalUnit au, Crawler crawler) {
    synchronized (newContentCrawls) {
      List crawlsForAu = (List)newContentCrawls.get(au.getAUId());
      if (crawlsForAu == null) {
	crawlsForAu = new ArrayList();
	newContentCrawls.put(au.getAUId(), crawlsForAu);
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

  public class CrawlThread extends Thread {
    private ArchivalUnit au;
    private List urls;
    private boolean followLinks;
    private Deadline deadline;
    private List callbacks;
    private Object cookie;
    private Crawler crawler;

    private CrawlThread(ArchivalUnit au, List urls,
			boolean followLinks, Deadline deadline,
			List callbacks, Object cookie) {
      super(au.toString());
      this.au = au;
      this.urls = urls;
      this.followLinks = followLinks;
      this.deadline = deadline;
      this.callbacks = callbacks;
      this.cookie = cookie;
      crawler = new GoslingCrawlerImpl(au, urls, followLinks);
    }

    public void run() {
      crawler.doCrawl(deadline);
      activeCrawls.remove(au);

      // if followLinks is true, assume it's a new content crawl
      // free activity regulator so polls can resume
      if (followLinks) {
        regulator.auActivityFinished(ActivityRegulator.NEW_CONTENT_CRAWL, au);
      } else {
        // otherwise, assume it's a repair crawl
        String url = (String)urls.get(0);
        regulator.cusActivityFinished(ActivityRegulator.REPAIR_CRAWL,
                                      au.makeCachedUrlSet(
            new SingleNodeCachedUrlSetSpec(url)));
      }

      if (callbacks != null) {
        Iterator it = callbacks.iterator();
        while (it.hasNext()) {
          CrawlManager.Callback cb = (CrawlManager.Callback) it.next();
          cb.signalCrawlAttemptCompleted(true, cookie);
        }
      }
    }
    public Crawler getCrawler() {
      return crawler;
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
    private static final String START_TIME_COL_NAME = "start";
    private static final String END_TIME_COL_NAME = "end";
    private static final String NUM_URLS_PARSED = "num_urls_parsed";
    private static final String NUM_URLS_FETCHED = "num_urls_fetched";

    private List colDescs =
      ListUtil.list(
		    new ColumnDescriptor(AU_COL_NAME, "Journal Volume",
					 ColumnDescriptor.TYPE_STRING),
		    new ColumnDescriptor(START_TIME_COL_NAME, "Start Time",
					 ColumnDescriptor.TYPE_DATE),
		    new ColumnDescriptor(END_TIME_COL_NAME, "End Time",
					 ColumnDescriptor.TYPE_DATE),
		    new ColumnDescriptor(NUM_URLS_FETCHED, "Num URLs fetched",
					 ColumnDescriptor.TYPE_INT),
		    new ColumnDescriptor(NUM_URLS_PARSED, "Num URLs parsed",
					 ColumnDescriptor.TYPE_INT)
		    );



    private List getRows(String key) {
      List rows = new ArrayList();
      if (key == null) {
	return getAllNewContentCrawls();
      }

      synchronized(newContentCrawls) {
	List crawlsForAu = (List) newContentCrawls.get(key);
	if (crawlsForAu != null) {
	  Iterator it = crawlsForAu.iterator();
	  while (it.hasNext()) {
	    rows.add(makeRow((Crawler) it.next()));
	  }
	}
      }
      return rows;
    }

    private List getAllNewContentCrawls() {
      List rows = new ArrayList();
      synchronized(newContentCrawls) {
	Iterator keys = newContentCrawls.keySet().iterator();
	while (keys.hasNext()) {
	  List crawls = (List)newContentCrawls.get((String)keys.next());
	  Iterator it = crawls.iterator();
	  while (it.hasNext()) {
	    rows.add(makeRow((Crawler)it.next()));
	  }
	}
      }
      return rows;
    }

    private Map makeRow(Crawler crawler) {
      Map row = new HashMap();
      row.put(AU_COL_NAME, crawler.getAU().getName());
      row.put(START_TIME_COL_NAME, makeNullOrLong(crawler.getStartTime()));
      row.put(END_TIME_COL_NAME, makeNullOrLong(crawler.getEndTime()));
      row.put(NUM_URLS_FETCHED, new Long(crawler.getNumFetched()));
      row.put(NUM_URLS_PARSED, new Long(crawler.getNumParsed()));
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
