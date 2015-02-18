/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.daemon.ActivityRegulator;
import org.lockss.plugin.*;
import org.lockss.state.AuState;

public class CrawlReq {
  ArchivalUnit au;
  CrawlManager.Callback cb;
  Object cookie;
  ActivityRegulator.Lock lock;
  AuState aus = null;
  Object rateKey;
  int priority = 0;
  int refetchDepth = -1;

  public CrawlReq(ArchivalUnit au) {
    this(au, null, null, null);
  }

  public CrawlReq(ArchivalUnit au, CrawlManager.Callback cb,
	   Object cookie, ActivityRegulator.Lock lock) {
    this.au = au;
    this.cb = cb;
    this.cookie = cookie;
    this.lock = lock;
    this.aus = AuUtil.getAuState(au);
    this.rateKey = au.getFetchRateLimiterKey();
  }

  public void setPriority(int val) {
    priority = val;
  }

  public void setCb(CrawlManager.Callback cb) {
    this.cb = cb;
  }

  public ArchivalUnit getAu() {
    return au;
  }

  public CrawlManager.Callback getCb() {
    return cb;
  }

  public Object getCookie() {
    return cookie;
  }

  public ActivityRegulator.Lock getLock() {
    return lock;
  }

  public AuState getAuState() {
    return aus;
  }

  public Object getRateKey() {
    return rateKey;
  }

  public int getPriority() {
    return priority;
  }

  public boolean isHiPri() {
    return priority > 0;
  }

  public void setRefetchDepth(int val) {
    refetchDepth = val;
  }

  public int getRefetchDepth() {
    return refetchDepth;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[CrawlReq: ");
    sb.append(au);
    sb.append(", pri: ");
    sb.append(priority);
    sb.append(", cb: ");
    sb.append(cb);
    if (refetchDepth >= 0) {
      sb.append(", depth: ");
      sb.append(refetchDepth);
    }
    sb.append("]");
    return sb.toString();
  }
}
