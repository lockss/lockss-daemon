/*
 * $Id: CrawlManagerImpl.java,v 1.5 2003-02-10 23:46:00 troberts Exp $
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
import org.lockss.state.NodeState;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.state.*;


/**
 * This is the interface for the object which will sit between the crawler
 * and the rest of the world.  It mediates the different crawl types.
 */
public class CrawlManagerImpl implements CrawlManager, LockssManager {
  /**
   * ToDo:
   * 1)make a crawl to the AU to decide if I should do a new content crawl
   * 2)handle background crawls
   * 3)handle repair crawls
   * 4)check for conflicting crawl types
   * 5)check crawl schedule rules
   */
  static private CrawlManagerImpl theManager = null;
  static private LockssDaemon theDaemon = null;
  /**
   * init the plugin manager.
   * @param daemon the LockssDaemon instance
   * @throws LockssDaemonException if we already instantiated this manager
   * @see org.lockss.app.LockssManager#initService(LockssDaemon daemon)
   */
  public void initService(LockssDaemon daemon) throws LockssDaemonException {
    if(theManager == null) {
      theDaemon = daemon;
      theManager = this;
    }
    else {
      throw new LockssDaemonException("Multiple Instantiation.");
    }
  }

  /**
   * start the plugin manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
  }

  /**
   * stop the plugin manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    // checkpoint here
    theManager = null;
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
    if (au == null) {
      throw new IllegalArgumentException("Called with null AU");
    } if (url == null) {
      throw new IllegalArgumentException("Called with null URL");
    }


    CrawlThread crawlThread =
      new CrawlThread(au, ListUtil.list(url.toString()),
		      false, Deadline.NEVER, cb, cookie);
    crawlThread.start();
  }

  public boolean canTreeWalkStart(ArchivalUnit au, AuState aus,
				  CrawlManager.Callback cb, Object cookie){
    if (au == null) {
      throw new IllegalArgumentException("Called with null AU");
    }

    if (au.shouldCrawlForNewContent(aus)) { //XXX get from au
      scheduleNewContentCrawl(au, cb, cookie);
      return false;
    }
    return true;
  }

  public boolean shouldRecrawl(ArchivalUnit au, NodeState ns) {
    //XXX implement
    return false;
  }

  private void scheduleNewContentCrawl(ArchivalUnit au,
				       CrawlManager.Callback cb,
				       Object cookie) {
    CrawlThread crawlThread =
      new CrawlThread(au, au.getNewContentCrawlUrls(),
		      true, Deadline.NEVER, cb, cookie);
    crawlThread.start();
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
    private CrawlManager.Callback cb;
    private Object cookie;

    private CrawlThread(ArchivalUnit au, List urls,
			boolean followLinks, Deadline deadline,
			CrawlManager.Callback cb, Object cookie) {
      super(au.toString());
      this.au = au;
      this.urls = urls;
      this.followLinks = followLinks;
      this.deadline = deadline;
      this.cb = cb;
      this.cookie = cookie;
    }

    public void run() {
      Crawler crawler = new GoslingCrawlerImpl();
      crawler.doCrawl(au, urls, followLinks, deadline);
      if (cb != null) {
	cb.signalCrawlAttemptCompleted(true, cookie);
      }
    }
  }
}
