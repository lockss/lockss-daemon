/*
 * $Id: CrawlManagerImpl.java,v 1.1 2003-02-05 22:40:42 troberts Exp $
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


/**
 * This is the interface for the object which will sit between the crawler
 * and the rest of the world.  It mediates the different crawl types.
 */
public class CrawlManagerImpl implements CrawlManager {
  /**
   * ToDo:
   * 1)make a crawl to the AU to decide if I should do a new content crawl
   * 2)handle background crawls
   * 3)handle repair crawls
   * 4)check for conflicting crawl types
   * 5)check crawl schedule rules
   */

  /**
   * Schedules a repair crawl and calls cb.signalRepairAttemptCompleted
   * when done.
   * @param url URL that needs to be repaired
   * @param cb callback to talk to when repair attempt is done
   * @param cookie object that the callback needs to understand which
   * repair we're referring to.
   */
  public void scheduleRepair(ArchivalUnit au, URL url, 
			     CrawlCallback cb, Object cookie){
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

  public boolean canTreeWalkStart(ArchivalUnit au, 
				  CrawlCallback cb, Object cookie){
    if (au == null) {
      throw new IllegalArgumentException("Called with null AU");
    }

    if (shouldCrawlForNewContent()) { //XXX get from au
      scheduleNewContentCrawl(au, cb, cookie);
      return false;
    }
    return true;
  }

  public boolean shouldRecrawl(ArchivalUnit au, NodeState ns) {
    //XXX implement
    return false;
  }

  private boolean shouldCrawlForNewContent() {
    return true;
  }

  private void scheduleNewContentCrawl(ArchivalUnit au, 
				       CrawlCallback cb, Object cookie) {
    CrawlThread crawlThread = 
      new CrawlThread(au, au.getNewContentCrawlUrls(), 
		      true, Deadline.NEVER, cb, cookie);
    crawlThread.start();
  }

  private void triggerCrawlCallbacks(Vector callbacks) {
    if (callbacks != null) {
      Iterator it = callbacks.iterator();
      while (it.hasNext()) {
	CrawlCallback cb = (CrawlCallback) it.next();
	cb.signalCrawlAttemptCompleted(true, null);
      }
    }
  }

//   public void addNewContentCrawlCallback(CrawlCallback cb,
// 					 ArchivalUnit au, Object cookie) {
//     Vector callbacks = (Vector) newContentCallbacks.get(au);
//     if (callbacks == null) {
//       callbacks = new Vector();
//       newContentCallbacks.put(au, callbacks);
//     }
//     callbacks.add(cb);
//   }

  public class CrawlThread extends Thread {
    private ArchivalUnit au;
    private List urls;
    private boolean followLinks;
    private Deadline deadline;
    private CrawlCallback cb;
    private Object cookie;

    private CrawlThread(ArchivalUnit au, List urls, 
			boolean followLinks, Deadline deadline,
			CrawlCallback cb, Object cookie) {
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
