/*
 * $Id: MockCrawlManager.java,v 1.5 2003-03-03 19:36:54 troberts Exp $
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

package org.lockss.test;

import java.util.*;
import java.net.URL;
import org.lockss.app.*;
import org.lockss.state.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.crawler.CrawlManager;


/**
 * Mock implementation of the CrawlManager
 */
public class MockCrawlManager implements CrawlManager, LockssManager {
  public HashMap scheduledRepairs = new HashMap();
  public HashMap scheduledCrawls = new HashMap();
  public static final String SCHEDULED = "scheduled";

  public void initService(LockssDaemon daemon) throws LockssDaemonException { }
  public void startService() { }
  public void stopService() {
    scheduledRepairs = new HashMap();
    scheduledCrawls = new HashMap();
  }

  public void scheduleRepair(ArchivalUnit au, URL url,
			     CrawlManager.Callback cb, Object cookie) {
    scheduledRepairs.put(url.toString(), SCHEDULED);
  }

  /**
   * Currently returns true if last crawl time > 0, schedules new content crawl
   * and returns false otherwise.
   * @param au the ArchivalUnit
   * @param aus the AuState
   * @param cb the Callback
   * @param cookie the cookie
   * @return true if can start
   */
  public boolean canTreeWalkStart(ArchivalUnit au, 
				  CrawlManager.Callback cb, Object cookie) {
    scheduleNewContentCrawl(au, cb, cookie);
    return false;
  }

  /**
   * Currently returns true iff last crawl time less than 0.
   * @param au the ArchivalUnit
   * @param ns the NodeState
   * @return true if last crawl time < 0.
   */
  public boolean shouldRecrawl(ArchivalUnit au, NodeState ns) {
    if (ns.getCrawlState().getStartTime() < 0) {
      return true;
    }
    return false;
  }

  public String getUrlStatus(String url) {
    return (String)scheduledRepairs.get(url);
  }

  public String getAuStatus(ArchivalUnit au) {
    return (String)scheduledCrawls.get(au);
  }

  public Iterator getScheduledUrlRepairs() {
    return scheduledRepairs.keySet().iterator();
  }

  public Iterator getScheduledAuCrawls() {
    return scheduledCrawls.keySet().iterator();
  }

  private void scheduleNewContentCrawl(ArchivalUnit au,
				       CrawlManager.Callback cb,
				       Object cookie) {
    scheduledCrawls.put(au, SCHEDULED);
  }
}
