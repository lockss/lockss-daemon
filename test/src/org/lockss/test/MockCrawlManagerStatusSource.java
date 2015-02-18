/*
 * $Id$
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

package org.lockss.test;
// import java.lang.*;
import java.util.*;
import org.lockss.app.*;
import org.lockss.plugin.*;
import org.lockss.crawler.*;

public class MockCrawlManagerStatusSource
    implements CrawlManager.StatusSource {
  private List crawlStatusList;
  private LockssDaemon daemon;
  private CrawlManagerStatus cmStatus = null;

  public MockCrawlManagerStatusSource(LockssDaemon daemon) {
    this.daemon = daemon;
  }

//   public Collection getActiveAus() {
//     return activeAus;
//   }

//   public void setActiveAus(Collection activeAus) {
//     this.activeAus = activeAus;
//   }

//   public Collection getCrawlStatus(String auid) {
//     return (Collection) map.get(auid);
//   }

//   public void setCrawlStatus(Collection crawlStatus, String auid) {
//     map.put(auid, crawlStatus);
//   }

  public CrawlManagerStatus getStatus() {
    if (cmStatus != null) return cmStatus;
    return new MyCrawlManagerStatus(crawlStatusList);
  }

  public void setStatus(CrawlManagerStatus status) {
    this.cmStatus = status;
  }

  public void setCrawlStatusList(List crawlStatusList) {
    this.crawlStatusList = crawlStatusList;
  }

  public LockssDaemon getDaemon() {
    return daemon;
  }

  public boolean isCrawlerEnabled() {
    return true;
  }

  public Collection<CrawlReq> getPendingQueue() {
    return new ArrayList();
  }

  static class MyCrawlManagerStatus extends CrawlManagerStatus {
    List clist;
    MyCrawlManagerStatus(List clist) {
      super(2);
      this.clist = clist;
    }
    public List getCrawlerStatusList() {
      if (clist != null) return clist;
      return super.getCrawlerStatusList();
    }
  }
}
