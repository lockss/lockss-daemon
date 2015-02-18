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

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * Manages the rate limiters in use by a crawl.  Implementations select
 * rate limiters based on URL or MIME type, or date/time, etc.
 */
public interface CrawlRateLimiter {
  /** Add a crawler to the list of those using this CrawlRateLimiter */
  public void addCrawler(Crawler c);

  /** Remove a crawler from the list of those using this CrawlRateLimiter */
  public void removeCrawler(Crawler c);

  /** Return the number of crawlers using this crawl rate limiter. */
  public int getCrawlerCount();

  /** Return the number of repair crawlers using this crawl rate
   * limiter. */
  public int getRepairCount();

  /** Return the number of new content crawlers using this crawl rate
   * limiter. */
  public int getNewContentCount();

  /** Return the RateLimiter on which to wait for the next fetch
   * @param url the url about to be fetched
   * @param previousContentType the MIME type or Content-Type of the
   * previous file fetched
   */
  public RateLimiter getRateLimiterFor(String url, String previousContentType);

  /** Wait until it's time for the next fetch
   * @param url the url about to be fetched
   * @param previousContentType the MIME type or Content-Type of the
   * previous file fetched
   */
  public void pauseBeforeFetch(String url, String previousContentType);

  /** Return the number of times this CrawlRateLimiter has been asked to
   * pause.  Used to check that rate limiter is actually being invoked. */
  public int getPauseCounter();

  public static class Util {
    public static CrawlRateLimiter forAu(ArchivalUnit au) {
      return forRli(au.getRateLimiterInfo());
    }

    public static CrawlRateLimiter forRli(RateLimiterInfo rli) {
      if (rli.getCond() != null) {
	return new ConditionalCrawlRateLimiter(rli);
      }
      return new FileTypeCrawlRateLimiter(rli);
    }
  }


}
