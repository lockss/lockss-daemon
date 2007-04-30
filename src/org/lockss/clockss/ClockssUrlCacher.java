/*
 * $Id: ClockssUrlCacher.java,v 1.6 2007-04-30 04:52:46 tlipkis Exp $
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

package org.lockss.clockss;

import java.io.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.daemon.*;
import org.lockss.state.*;
import org.lockss.plugin.*;
import org.lockss.crawler.PermissionMap;

/**
 * Wrapper for UrlConnection, which performs CLOCKSS subscription
 * detection.
 */
public class ClockssUrlCacher implements UrlCacher {
  private static Logger log = Logger.getLogger("ClockssUrlCacher");

  private UrlCacher uc;
  ArchivalUnit au;
  private ClockssSubscriptionProbe probe = null;

  public ClockssUrlCacher(UrlCacher uc) {
    this.uc = uc;
    au = uc.getArchivalUnit();
    if (AuUtil.getDaemon(au).isDetectClockssSubscription()) {
      probe = new ClockssSubscriptionProbe(au);
    }
  }

  public ArchivalUnit getArchivalUnit() {
    return uc.getArchivalUnit();
  }

  public String getUrl() {
    return uc.getUrl();
  }

  /** @deprecated */
  public CachedUrlSet getCachedUrlSet() {
    return uc.getCachedUrlSet();
  }

  public boolean shouldBeCached(){
    return uc.shouldBeCached();
  }

  public CachedUrl getCachedUrl() {
    return uc.getCachedUrl();
  }

  public void setConnectionPool(LockssUrlConnectionPool connectionPool) {
    uc.setConnectionPool(connectionPool);
  }

  public void setProxy(String proxyHost, int proxyPort) {
    uc.setProxy(proxyHost, proxyPort);
  }

  public void setLocalAddress(IPAddr addr) {
    uc.setLocalAddress(addr);
  }

  public void setFetchFlags(BitSet fetchFlags) {
    uc.setFetchFlags(fetchFlags);
  }

  public void setRequestProperty(String key, String value) {
    uc.setRequestProperty(key, value);
  }

  public void setRedirectScheme(RedirectScheme scheme) {
    uc.setRedirectScheme(scheme);
  }

  public void setWatchdog(LockssWatchdog wdog) {
    uc.setWatchdog(wdog);
  }

  public void storeContent(InputStream input, CIProperties headers)
      throws IOException {
    uc.storeContent(input, headers);
  }

  public int cache() throws IOException {
    if (probe == null) {
      AuState aus = AuUtil.getAuState(au);
      aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_NOT_MAINTAINED);
      ClockssParams mgr = AuUtil.getDaemon(au).getClockssParams();
      uc.setLocalAddress(mgr.getClockssSubscriptionAddr());
      return uc.cache();
    }
    int res;
    boolean update = false;
    boolean worked = false;
    probe.setupAddr(uc);		// setup first probe address
    try {
      res = uc.cache();
      update = true;
      worked = true;
      return res;
    } catch (CacheException.PermissionException e) {
      update = true;
      if (probe.setupAddr(uc)) {	// setup second probe address
	uc.reset();
	res = uc.cache();
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
      uc.setLocalAddress(mgr.getClockssSubscriptionAddr());
      return uc.getUncachedInputStream();
    }
    InputStream res = null;
    boolean update = false;
    probe.setupAddr(uc);
    try {
      res = uc.getUncachedInputStream();
      update = true;
      return res;
    } catch (CacheException.PermissionException e) {
      update = true;
      if (probe.setupAddr(uc)) {
	uc.reset();
	res = uc.getUncachedInputStream();
	return res;
      } else {
	throw e;
      }
    } finally {
      if (update) probe.updateSubscriptionStatus(res != null);
    }
  }

  public CIProperties getUncachedProperties() {
    return uc.getUncachedProperties();
  }

  public void reset() {
    uc.reset();
    if (probe != null) {
    probe.reset();
    }
  }

  public void setPermissionMapSource(PermissionMapSource pmSource) {
    uc.setPermissionMapSource(pmSource);
  }

  public String toString() {
    return "[ClockssUrlCacher: " + uc + "]";
  }
}
