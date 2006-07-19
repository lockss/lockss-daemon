/*
 * $Id: CrawlManagerStatus.java,v 1.32 2006-07-19 00:47:00 tlipkis Exp $
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

import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/** Reflects overall status of CrawlManager, contains list of individual
 * Crawler.Status objects */
public class CrawlManagerStatus {
  private HistoryList crawlList;
  private int successful = 0;
  private int failed = 0;
  private Deadline nextCrawlStarter;

  public CrawlManagerStatus(int histSize) {
    this.crawlList = new HistoryList(histSize);
  }

  public void setHistSize(int histMax) {
    synchronized (crawlList) {
      crawlList.setMax(histMax);
    }
  }

  public void setNextCrawlStarter(Deadline nextCrawlStarter) {
    this.nextCrawlStarter = nextCrawlStarter;
  }

  public Deadline getNextCrawlStarter() {
    return nextCrawlStarter;
  }

  public List getCrawlStatusList() {
    synchronized (crawlList) {
      return new ArrayList(crawlList);
    }
  }

  public void addCrawl(Crawler.Status status) {
    synchronized (crawlList) {
      crawlList.add(status);
    }
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

