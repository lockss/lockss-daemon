/*
 * $Id: MockCrawlManager.java,v 1.1 2003-02-12 23:57:37 aalto Exp $
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

  public boolean canTreeWalkStart(ArchivalUnit au, AuState aus,
				  CrawlManager.Callback cb, Object cookie) {
    if (aus.getLastCrawlTime() <= 0) {
      scheduleNewContentCrawl(au, cb, cookie);
      return false;
    }
    return true;
  }

  public boolean shouldRecrawl(ArchivalUnit au, NodeState ns) {
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
