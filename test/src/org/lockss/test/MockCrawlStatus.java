/*
 * $Id: MockCrawlStatus.java,v 1.2 2003-12-23 00:40:27 tlipkis Exp $
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
import org.lockss.plugin.ArchivalUnit;

public class MockCrawlStatus extends Crawler.Status {
  private static final int UNDEFINED_TYPE = -1;

  int crawlStatus = 0;
  
  public MockCrawlStatus(int type) {
    super(null, null, type);
  }

  public MockCrawlStatus() {
    this(UNDEFINED_TYPE);
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }

  public void setCrawlStatus(int crawlStatus) {
    this.crawlStatus = crawlStatus;
  }

  public int getCrawlStatus() {
    return crawlStatus;
  }

  public void setNumFetched(int numFetched) {
    this.numFetched = numFetched;
  }

  public void setNumParsed(int numParsed) {
    this.numParsed = numParsed;
  }

  public void setAu(ArchivalUnit au) {
    this.au = au;
  }

  public void setType(int type) {
    if (type == UNDEFINED_TYPE) {
      throw new IllegalStateException("Called with an undefined type set");
    }
    this.type = type;
  }
}
