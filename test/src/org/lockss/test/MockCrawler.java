/*
 * $Id: MockCrawler.java,v 1.14 2006-11-14 19:21:28 tlipkis Exp $
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

import java.util.Collection;
import org.lockss.util.Deadline;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.crawler.*;

public class MockCrawler extends NullCrawler {
  ArchivalUnit au;
  Collection urls;
  boolean followLinks;
  boolean doCrawlCalled = false;
  Deadline deadline = null;
  boolean crawlSuccessful = true;
  int type = -1;
  boolean isWholeAU = false;
  long startTime = -1;
  long endTime = -1;
  long numFetched = -1;
  long numParsed = -1;


  CrawlerStatus status = null;

  boolean wasAborted = false;


  public void abortCrawl() {
    wasAborted = true;
  }

  public boolean wasAborted() {
    return wasAborted;
  }

  public void setCrawlSuccessful(boolean crawlSuccessful) {
    this.crawlSuccessful = crawlSuccessful;
  }

  public boolean doCrawl() {
    doCrawlCalled = true;
    return crawlSuccessful;
  }

  public Deadline getDeadline() {
    return deadline;
  }

  public boolean doCrawlCalled() {
    return doCrawlCalled;
  }

  public void setDoCrawlCalled(boolean val) {
    doCrawlCalled = val;
  }

  public ArchivalUnit getAu() {
    return au;
  }

  public void setAu(ArchivalUnit au) {
    this.au = au;
  }

  public void setUrls(Collection urls) {
    this.urls = urls;
  }

  public void setFollowLinks(boolean followLinks) {
    this.followLinks = followLinks;
  }

  public void setType(int type) {
    this.type = type;
  }

  public int getType() {
    return type;
  }

  public void setIsWholeAU(boolean val) {
    isWholeAU = val;
  }

  public boolean isWholeAU() {
    return isWholeAU;
  }

  public Collection getStartUrls() {
    return urls;
  }

  public void setStartTime(long time) {
    startTime = time;
  }

//   public void setEndTime(long time) {
//     endTime = time;
//   }

//   public void setNumFetched(long num) {
//     numFetched = num;
//   }

//   public void setNumParsed(long num) {
//     numParsed = num;
//   }

  public long getStartTime() {
    return startTime;
  }

//   public long getEndTime() {
//     return endTime;
//   }

//   public long getNumFetched() {
//     return numFetched;
//   }

//   public long getNumParsed() {
//     return numParsed;
//   }

  public void setStatus(CrawlerStatus status) {
    this.status = status;
  }

  public CrawlerStatus getStatus() {
    if (status == null) {
      status = new MockCrawlStatus();
    }
    return status;
  }

}
