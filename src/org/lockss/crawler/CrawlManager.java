/*
 * $Id: CrawlManager.java,v 1.26 2006-02-23 06:43:37 tlipkis Exp $
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

import org.lockss.daemon.*;
import org.lockss.app.*;
import org.lockss.plugin.*;

/**
 * This is the interface for the object which will sit between the crawler
 * and the rest of the world.  It mediates the different crawl types.
 */
public interface CrawlManager {
  /**
   * Schedules a repair crawl and calls cb.signalRepairAttemptCompleted
   * when done.  If lock is null, attempts to get its own.  If multiple locks
   * are needed, gets
   *
   * @param au ArchivalUnit that the crawl manager should check
   * @param urls url Strings that need to be repaired
   * @param cb callback to talk to when repair attempt is done
   * @param cookie object that the callback needs to understand which
   * repair we're referring to.
   * @param lock the activity lock (can be null)
   */
  public void startRepair(ArchivalUnit au, Collection urls,
			  CrawlManager.Callback cb, Object cookie,
                          ActivityRegulator.Lock lock);

  /**
   * Starts a new content crawl.  If lock is null, attempts to get its own.
   *
   * @param au ArchivalUnit that the crawl manager should check
   * @param cb callback to be called when the crawler is done with the AU,
   * if not now
   * @param cookie cookie for the callback
   * @param lock the activity lock (can be null)
   */
  public void startNewContentCrawl(ArchivalUnit au, CrawlManager.Callback cb,
                                   Object cookie, ActivityRegulator.Lock lock);


  public interface Callback {
    /**
     * Called when the crawl is completed
     * @param success whether the crawl was successful or not
     * @param urlsFetched URLs sucessfully fetched
     * @param cookie object used by callback to designate which crawl
     * attempt this is
     */
    public void signalCrawlAttemptCompleted(boolean success, Set urlsFetched,
					    Object cookie,
					    Crawler.Status status);
  }

  public interface StatusSource {
//     /**
//      * return a collection of ArchivalUnits that have crawl history (either
//      * active or completed crawls)
//      * @return collection of AUs that have crawl history (either active or
//      * completed crawls)
//      */
//     public Collection getActiveAus();

    /**
     * Return a list of all the Crawler.Status objects for existing crawls
     */
    public List getCrawlStatusList();

    /**
     * return a <code>Collection</code> of {@link CrawlManagerStatus}
     * objects for any repair crawls for <code>au</code>
     *
     * @param auid id of ArchivalUnit to get {@link CrawlManagerStatus} objects
     * for any repair crawls for
     * @return <code>Collection</code> of {@link CrawlManagerStatus}
     * objects for <code>au</code>
     */
//     public Collection getCrawlStatus(String auid);

    /** Return the dameon instance */
    public LockssDaemon getDaemon();
  }
}
