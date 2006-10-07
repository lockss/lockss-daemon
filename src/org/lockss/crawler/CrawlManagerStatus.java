/*
 * $Id: CrawlManagerStatus.java,v 1.33 2006-10-07 07:16:22 tlipkis Exp $
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
import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.collections.OrderedMapIterator;

import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/** Reflects overall status of CrawlManager, contains history of
 * Crawler.Status objects for individual crawls */
public class CrawlManagerStatus {

  private LRUMap statusMap;
  private int successful = 0;
  private int failed = 0;
  private Deadline nextCrawlStarter;

  /** Create CrawlManagerStatus with specified fixed size history */
  public CrawlManagerStatus(int histSize) {
    this.statusMap = new LRUMap(histSize);
  }

  public synchronized void setHistSize(int histMax) {
    LRUMap newmap = new LRUMap(histMax);
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

  /** Return a list of Crawler.Status for each crawl in the history. */
  public synchronized List getCrawlStatusList() {
    List res = new ArrayList(statusMap.size());
    for (OrderedMapIterator iter = statusMap.orderedMapIterator();
	 iter.hasNext(); ) {
      iter.next();
      res.add(iter.getValue());
    }
    return res;
  }

  /** Add a crawl status object to the history */
  public synchronized void addCrawlStatus(Crawler.Status status) {
    statusMap.put(status.getKey(), status);
  }

  /** Retrieve the Crawler.Status object with the given key */
  public synchronized Crawler.Status getCrawlStatus(String key) {
    return (Crawler.Status)statusMap.get(key);
  }

  public void incrFinished(boolean success) {
    if (success) {
      successful++;
    } else {
      failed++;
    }
  }

  public int getSuccessCount() {
    return successful;
  }

  public int getFailedCount() {
    return failed;
  }
}
