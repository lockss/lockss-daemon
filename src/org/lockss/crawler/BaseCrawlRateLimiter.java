/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.daemon.*;
import org.lockss.util.*;

/**
 * Common functionality for CrawlRateLimiter implementations.
 * This and subclasses are required to be thread safe
 * @ThreadSafe
 */
public abstract class BaseCrawlRateLimiter implements CrawlRateLimiter {
  static Logger log = Logger.getLogger("BaseCrawlRateLimiter");

  protected Set<Crawler> crawlers = new HashSet<Crawler>();
  protected int pauseCounter = 0;

  public BaseCrawlRateLimiter() {
  }

  /** Add a crawler to the list of those using this CrawlRateLimiter */
  public void addCrawler(Crawler c) {
    crawlers.add(c);
  }

  /** Remove a crawler from the list of those using this CrawlRateLimiter */
  public void removeCrawler(Crawler c) {
    crawlers.remove(c);
  }

  /** Return the number of crawlers actively using this crawl rate
   * limiter. */
  public int getCrawlerCount() {
    return crawlers.size();
  }

  /** Return the number of repair crawlers actively using this crawl rate
   * limiter. */
  public int getRepairCount() {
    int res = 0;
    for (Crawler c : crawlers) {
      if (!c.isWholeAU()) {
	res++;
      }
    }
    return res;
  }

  /** Return the number of new content crawlers actively using this crawl
   * rate limiter. */
  public int getNewContentCount() {
    int res = 0;
    for (Crawler c : crawlers) {
      if (c.isWholeAU()) {
	res++;
      }
    }
    return res;
  }

  /** Wait until it's time for the next fetch
   * @param url the url about to be fetched
   * @param previousContentType the MIME type or Content-Type of the
   * previous file fetched
   */
  public void pauseBeforeFetch(String url, String  previousContentType) {
    RateLimiter limiter = getRateLimiterFor(url, previousContentType);
    try {
      if (log.isDebug3()) log.debug3("Pausing: " + limiter.rateString());
      pauseCounter++;
      limiter.fifoWaitAndSignalEvent();
    } catch (InterruptedException ignore) {
      // no action
    }
  }

  /** Used to check that rate limiter is actually being invoked, */
  public int getPauseCounter() {
    return pauseCounter;
  }
}
