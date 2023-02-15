/*
 * $Id$
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
import org.apache.commons.collections.map.*;
import org.apache.commons.collections.OrderedMapIterator;

import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/** Reflects overall status of CrawlManager, contains history of
 * CrawlerStatus objects for individual crawls */
public class CrawlManagerStatus {

  private LRUMap statusMap;
  private int successful = 0;
  private int failed = 0;
  private Deadline nextCrawlStarter;
  private int ausWantCrawl;
  private int ausEligibleCrawl;
  private boolean isOdc = false;
  private Collection runningNCCrawls = Collections.EMPTY_LIST;


  /** Return an LRUMap that prevents deletion of active crawls */
  static LRUMap makeLRUMap(int histSize) {
    // scanUntilRemovable must be true to prevent map from slowly growing
    // without bound
    return new LRUMap(histSize, true) {
      protected boolean removeLRU(AbstractLinkedMap.LinkEntry entry) {
	return !((CrawlerStatus)entry.getValue()).isCrawlActive();
      }};
  }


  /** Create CrawlManagerStatus with specified fixed size history */
  public CrawlManagerStatus(int histSize) {
    this.statusMap = makeLRUMap(histSize);
  }

  public synchronized void setHistSize(int histMax) {
    LRUMap newmap = makeLRUMap(histMax);
    for (OrderedMapIterator iter = statusMap.orderedMapIterator();
	 iter.hasNext(); ) {
      newmap.put(iter.next(), iter.getValue());
    }
    statusMap = newmap;
  }

  public void setNextCrawlStarter(Deadline nextCrawlStarter) {
    this.nextCrawlStarter = nextCrawlStarter;
  }

  public Deadline getNextCrawlStarter() {
    return nextCrawlStarter;
  }

  /** Return a list of CrawlerStatus for each crawl in the history. */
  public synchronized List<CrawlerStatus> getCrawlerStatusList() {
    List res = new ArrayList(statusMap.size());
    for (OrderedMapIterator iter = statusMap.orderedMapIterator();
	 iter.hasNext(); ) {
      iter.next();
      res.add(iter.getValue());
    }
    return res;
  }

  /** Add a crawl status object to the history */
  public synchronized void addCrawlStatus(CrawlerStatus status) {
    statusMap.put(status.getKey(), status);
  }

  /** Retrieve the CrawlerStatus object with the given key */
  public synchronized CrawlerStatus getCrawlerStatus(String key) {
    return (CrawlerStatus)statusMap.get(key);
  }

  /** Remove the given CrawlerStatus iff it is in PENDING state */
  public synchronized void removeCrawlerStatusIfPending(CrawlerStatus status) {
    if (status.getCrawlStatus() == Crawler.STATUS_QUEUED) {
      statusMap.remove(status.getKey());
    }
  }

  /** Move the CrawlerStatus to the least-recently-used position */
  public synchronized void touchCrawlStatus(String key) {
    statusMap.get(key);
  }

  /** Move the CrawlerStatus to the least-recently-used position */
  public synchronized void touchCrawlStatus(CrawlerStatus status) {
    statusMap.get(status.getKey());
  }

  public void setRunningNCCrawls(Collection coll) {
    runningNCCrawls = coll;
  }

  public boolean isRunningNCCrawl(ArchivalUnit au) {
    return runningNCCrawls.contains(au);
  }

  public void incrFinished(boolean success) {
    if (success) {
      successful++;
    } else {
      failed++;
    }
  }

  public boolean isOdc() {
    return isOdc;
  }

  void setOdc(boolean val) {
    isOdc = val;
  }

  public int getSuccessCount() {
    return successful;
  }

  public int getFailedCount() {
    return failed;
  }

  public int getWaitingCount() {
    return ausWantCrawl;
  }

  public int getEligibleCount() {
    return ausEligibleCrawl;
  }

  public void setWaitingCount(int val) {
    ausWantCrawl = val;
  }

  public void setEligibleCount(int val) {
    ausEligibleCrawl = val;
  }
}
