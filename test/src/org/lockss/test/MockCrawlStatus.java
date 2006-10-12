/*
 * $Id: MockCrawlStatus.java,v 1.12 2006-10-12 22:38:23 adriz Exp $
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
import java.util.*;

public class MockCrawlStatus extends Crawler.Status {
  private static final int UNDEFINED_TYPE = -1;

  String crawlStatus = null;
  boolean crawlEndSignaled = false;
  long numParsed = 0;
  long numPending = 0;
  long numFetched = 0;
  long numErrors = 0;
  long numNotModified = 0;
  long numExcluded = 0;

  Collection urlsFetched = null;
  Collection urlsParsed = null;
  Collection urlsPending = null;
  Collection urlsNotModified = null;
  Collection urlsExcluded = null;
  Map errorUrls = null;


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

  public void setCrawlStatus(String crawlStatus) {
    this.crawlStatus = crawlStatus;
  }

  public String getCrawlStatus() {
    return crawlStatus;
  }

  public void setNumFetched(int numFetched) {
    this.numFetched = numFetched;
  }

  public long getNumFetched() {
    if (urlsFetched != null) {
      return urlsFetched.size();
    }
    return numFetched;
  }

  public void setNumExcluded(int numExcluded) {
    this.numExcluded = numExcluded;
  }

  public long getNumExcluded() {
    if (urlsExcluded != null) {
      return urlsExcluded.size();
    }
    return numExcluded;
  }

  public Collection getUrlsExcluded() {
    return urlsExcluded;
  }

  public void setUrlsWithErrors(Map errorUrls) {
    this.errorUrls = errorUrls;
  }

  public void setNumUrlsWithErrors(int num) {
    this.numErrors = num;
  }

  public void setUrlsNotModified(Collection urlsNotModified) {
    this.urlsNotModified = urlsNotModified;
  }

  public void setUrlsExcluded(Collection urlsExcluded) {
    this.urlsExcluded = urlsExcluded;
  }

  public Collection getUrlsParsed() {
    return urlsParsed;
  }

  public void setUrlsParsed(Collection urlsParsed) {
    this.urlsParsed = urlsParsed;
  }

  // allow test-mock set/get for pending urls
  public Collection getUrlsPending() {
    return urlsPending;
  }

  public void setUrlsPending(Collection urlsPending) {
    this.urlsPending = urlsPending;
  }

  public Collection getUrlsNotModified() {
    return urlsNotModified;
  }

  public void setNumNotModified(int numNotModified) {
    this.numNotModified = numNotModified;
  }

  public long getNumNotModified() {
    if (urlsNotModified != null) {
      return urlsNotModified.size();
    }
    return numNotModified;
  }

  public long getNumUrlsWithErrors() {
    if (errorUrls != null) {
      return urlsWithErrors.size();
    }
    return numErrors;
  }

  public long getNumParsed() {
    if (urlsParsed != null) {
      return urlsParsed.size();
    }
    return numParsed;
  }

  public long getNumPending() {
    if (urlsPending != null) {
      return urlsPending.size();
    }
    return numPending;
  }

  public Map getUrlsWithErrors() {
    return errorUrls;
  }

  public void setUrlsFetched(Collection urlsFetched) {
    this.urlsFetched = urlsFetched;
  }

  public Collection getUrlsFetched() {
    return urlsFetched;
  }

  public void setNumParsed(int numParsed) {
    this.numParsed = numParsed;
  }

  public void setNumPending(int numPending) { // set for pending
    this.numPending = numPending;
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
}
