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

package org.lockss.clockss;

import java.io.*;
import java.util.*;

import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.daemon.*;
import org.lockss.state.*;
import org.lockss.plugin.*;
import org.lockss.crawler.*;

/**
 * Wrapper for UrlConnection, which performs CLOCKSS subscription
 * detection.
 */
public class ClockssUrlFetcher implements UrlFetcher {
  private static Logger log = Logger.getLogger("ClockssUrlCacher");

  private UrlFetcher uf;
  ArchivalUnit au;
  private ClockssSubscriptionProbe probe = null;

  public ClockssUrlFetcher(UrlFetcher uf) {
    this.uf = uf;
    au = uf.getArchivalUnit();
    if (AuUtil.getDaemon(au).isDetectClockssSubscription()) {
      probe = new ClockssSubscriptionProbe(au);
    }
  }

  public ArchivalUnit getArchivalUnit() {
    return uf.getArchivalUnit();
  }

  public String getUrl() {
    return uf.getUrl();
  }

  public void setConnectionPool(LockssUrlConnectionPool connectionPool) {
    uf.setConnectionPool(connectionPool);
  }

  public void setProxy(String proxyHost, int proxyPort) {
    uf.setProxy(proxyHost, proxyPort);
  }

  public void setLocalAddress(IPAddr addr) {
    uf.setLocalAddress(addr);
  }

  public void setFetchFlags(BitSet fetchFlags) {
    uf.setFetchFlags(fetchFlags);
  }

  public BitSet getFetchFlags() {
    return uf.getFetchFlags();
  }

  public void setRequestProperty(String key, String value) {
    uf.setRequestProperty(key, value);
  }

  public void setRedirectScheme(RedirectScheme scheme) {
    uf.setRedirectScheme(scheme);
  }

  public void setWatchdog(LockssWatchdog wdog) {
    uf.setWatchdog(wdog);
  }
  
  public LockssWatchdog getWatchdog() {
    return uf.getWatchdog();
  }

  public void setCrawlRateLimiter(CrawlRateLimiter crl) {
    uf.setCrawlRateLimiter(crl);
  }

  public void setPreviousContentType(String previousContentType) {
    uf.setPreviousContentType(previousContentType);
  }

  public FetchResult fetch() throws CacheException {
    if (probe == null) {
      AuState aus = AuUtil.getAuState(au);
      aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_NOT_MAINTAINED);
      ClockssParams mgr = AuUtil.getDaemon(au).getClockssParams();
      if (mgr.getClockssSubscriptionAddr() != null) {
        uf.setLocalAddress(mgr.getClockssSubscriptionAddr());
      }
      return uf.fetch();
    }
    FetchResult res;
    boolean update = false;
    boolean worked = false;
    probe.setupAddr(uf);		// setup first probe address
    try {
      res = uf.fetch();
      update = true;
      worked = true;
      return res;
    } catch (CacheException.PermissionException e) {
      update = true;
      if (probe.setupAddr(uf)) {	// setup second probe address
	uf.reset();
	res = uf.fetch();
	worked = true;
	return res;
      } else {
	throw e;
      }
    } finally {
      // update subscription status iff fetch worked or threw a
      // PermissionException
      if (update) probe.updateSubscriptionStatus(worked);
    }
  }

  public InputStream getUncachedInputStream() throws IOException {
    if (probe == null) {
      AuState aus = AuUtil.getAuState(au);
      aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_NOT_MAINTAINED);
      ClockssParams mgr = AuUtil.getDaemon(au).getClockssParams();
      if (mgr.getClockssSubscriptionAddr() != null) {
        uf.setLocalAddress(mgr.getClockssSubscriptionAddr());
      }
      return uf.getUncachedInputStream();
    }
    InputStream res = null;
    boolean update = false;
    probe.setupAddr(uf);
    try {
      res = uf.getUncachedInputStream();
      update = true;
      return res;
    } catch (CacheException.PermissionException e) {
      update = true;
      if (probe.setupAddr(uf)) {
	uf.reset();
	res = uf.getUncachedInputStream();
	return res;
      } else {
	throw e;
      }
    } finally {
      if (update) probe.updateSubscriptionStatus(res != null);
    }
  }

  public CIProperties getUncachedProperties() {
    return uf.getUncachedProperties();
  }

  public void reset() {
    uf.reset();
    if (probe != null) {
      probe.reset();
    }
  }

  public String toString() {
    return "[ClockssUrlFetcher: " + uf + "]";
  }

  public InputStream resetInputStream(InputStream input, String lastModified)
      throws IOException {
    return uf.resetInputStream(input, lastModified);
  }

  public void setUrlConsumerFactory(UrlConsumerFactory consumerFactory) {
    uf.setUrlConsumerFactory(consumerFactory);
  }
}
