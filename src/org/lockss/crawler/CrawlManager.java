/*
 * $Id: CrawlManager.java,v 1.15 2003-07-02 00:55:02 troberts Exp $
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

import java.util.Collection;
import org.lockss.daemon.*;
import org.lockss.state.*;
import org.lockss.plugin.*;

/**
 * This is the interface for the object which will sit between the crawler
 * and the rest of the world.  It mediates the different crawl types.
 */
public interface CrawlManager {
  /**
   * Schedules a repair crawl and calls cb.signalRepairAttemptCompleted
   * when done.
   * @param au ArchivalUnit that the crawl manager should check
   * @param urls url Strings that need to be repaired
   * @param cb callback to talk to when repair attempt is done
   * @param cookie object that the callback needs to understand which
   * repair we're referring to.
   */
  public void startRepair(ArchivalUnit au, Collection urls,
			  CrawlManager.Callback cb, Object cookie);

  /**
   * Starts a new content crawl
   *
   * @param au ArchivalUnit that the crawl manager should check
   * @param cb callback to be called when the crawler is done with the AU,
   * if not now
   * @param cookie cookie for the callback
   */
  public void startNewContentCrawl(ArchivalUnit au, CrawlManager.Callback cb,
                                   Object cookie);


  public interface Callback {
    /**
     * Called when the crawl is completed
     * @param success whether the crawl was successful or not
     * @param cookie object used by callback to designate which crawl
     * attempt this is
     */
    public void signalCrawlAttemptCompleted(boolean success, Object cookie);
  }

  public interface StatusSource {
    /**
     * return a collection of ArchivalUnits that have crawl history (either
     * active or completed crawls)
     * @return collection of AUs that have crawl history (either active or
     * completed crawls)
     */
    public Collection getActiveAUs();

    /**
     * return a <code>Collection</code> of {@link Crawler}s doing repair
     * crawls for <code>au</code>
     *
     * @param au {@link ArchivalUnit} to get {@link Crawler}s doing repair
     * crawls for
     * @return <code>Collection</code> of {@link Crawler}s for <code>au</code>
     */
    public Collection getCrawls(String auid);
  }
}
