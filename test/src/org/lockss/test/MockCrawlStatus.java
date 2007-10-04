/*
 * $Id: MockCrawlStatus.java,v 1.17 2007-10-04 04:06:16 tlipkis Exp $
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
import org.lockss.daemon.Crawler;
import org.lockss.crawler.*;
import org.lockss.plugin.ArchivalUnit;
import java.util.*;

public class MockCrawlStatus extends CrawlerStatus {

  int crawlStatus = -1;
  String crawlStatusString = null;
  boolean crawlEndSignaled = false;


  public MockCrawlStatus(String type) {
    super(null, null, type);
  }

  public MockCrawlStatus() {
    this(null);
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }

  public void setCrawlStatus(int crawlStatus) {
    setCrawlStatus(crawlStatus, getDefaultMessage(crawlStatus));
  }

  public void setCrawlStatus(int crawlStatus, String crawlStatusString) {
    this.crawlStatus = crawlStatus;
    this.crawlStatusString = crawlStatusString;
  }

  public String getCrawlStatusMsg() {
    return crawlStatusString;
  }

  public int getCrawlStatus() {
    return crawlStatus;
  }

  public void setNumFetched(int numFetched) {
    fetched = new MyUrlCount(numFetched);
  }

  public void setUrlsFetched(List urlsFetched) {
    fetched = new MyUrlCount(urlsFetched);
  }

  public void setNumExcluded(int numExcluded) {
    excluded = new MyUrlCount(numExcluded);
  }

  public void setUrlsExcluded(List urlsExcluded) {
    excluded = new MyUrlCount(urlsExcluded);
  }

  public void setNumUrlsWithErrors(int num) {
    errors = new MyUrlCount(num);
  }

  public void setUrlsWithErrors(Map errorUrls) {
    errors = new MyUrlCount(errorUrls);
  }

  public void setNumNotModified(int numNotModified) {
    notModified = new MyUrlCount(numNotModified);
  }

  public void setUrlsNotModified(List urlsNotModified) {
    notModified = new MyUrlCount(urlsNotModified);
  }

  public void setNumParsed(int numParsed) {
    parsed = new MyUrlCount(numParsed);
  }

  public void setUrlsParsed(List urlsParsed) {
    parsed = new MyUrlCount(urlsParsed);
  }

  public void setNumPending(int numPending) { // set for pending
    pending = new MyUrlCount(numPending);
  }

  public void setUrlsPending(List urlsPending) {
    pending = new MyUrlCount(urlsPending);
  }

  public void setAu(ArchivalUnit au) {
    this.au = au;
  }

  public void signalCrawlEnded() {
    super.signalCrawlEnded();
    crawlEndSignaled = true;
  }

  public boolean crawlEndSignaled() {
    return crawlEndSignaled;
  }

  public void setType(String type) {
    if (type == null) {
      throw new IllegalStateException("Called with null type");
    }
    this.type = type;
  }

  public static class MyUrlCount extends CrawlerStatus.UrlCount {
    int cnt = 0;
    List lst;
    Map map;
    MyUrlCount(int num) {
      this.cnt = num;
    }
    MyUrlCount(List lst) {
      this.lst = lst;
    }
    MyUrlCount(Map map) {
      this.map = map;
    }

    public int getCount() {
      if (lst != null) return lst.size();
      if (map != null) return map.size();
      return cnt;
    }
    public boolean hasList() {
      return true;
    }
    public boolean hasMap() {
      return true;
    }
    public List getList() {
      return lst != null ? lst : Collections.EMPTY_LIST;
    }
    public Map getMap() {
      return map != null ? map : Collections.EMPTY_MAP;
    }
    public UrlCount seal() {
      return this;
    }
  }
}
