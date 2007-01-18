/*
 * $Id: TestProxyManager.java,v 1.4 2007-01-18 02:28:09 tlipkis Exp $
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

package org.lockss.proxy;

import java.io.*;
import java.util.*;
import org.mortbay.http.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestProxyManager extends LockssTestCase {
  static Logger log = Logger.getLogger("TestProxyManager");

  private ProxyManager mgr;
  private MockLockssDaemon daemon;

  public void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();
    mgr = new MyProxyManager();
    mgr.initService(daemon);
    daemon.setProxyManager(mgr);
    mgr.startService();
    TimeBase.setSimulated();
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    mgr.stopService();
    super.tearDown();
  }

  public void testIsRepairRequest() throws Exception {
    HttpRequest req = new HttpRequest();
    req.setPath("foo/bar");
    req.setField(Constants.X_LOCKSS, ListUtil.list("foo",
						   Constants.X_LOCKSS_REPAIR,
						   "bar"));
    assertTrue(mgr.isRepairRequest(req));
    req.setField(Constants.X_LOCKSS, ListUtil.list("foo"));
    assertFalse(mgr.isRepairRequest(req));
  }

  public void testHostDownT() throws Exception {
    assertFalse(mgr.isHostDown("foo"));
    mgr.setHostDown("foo", true);
    assertTrue(mgr.isHostDown("foo"));
    TimeBase.step(ProxyManager.DEFAULT_HOST_DOWN_RETRY + 10);
    assertFalse(mgr.isHostDown("foo"));
  }

  public void testHostDownF() throws Exception {
    assertFalse(mgr.isHostDown("foo"));
    mgr.setHostDown("foo", false);
    assertFalse(mgr.isHostDown("foo"));
    mgr.setHostDown("foo", true);
    assertTrue(mgr.isHostDown("foo"));
    TimeBase.step(ProxyManager.DEFAULT_HOST_DOWN_RETRY + 10);
    assertFalse(mgr.isHostDown("foo"));
    mgr.setHostDown("foo", false);
    assertTrue(mgr.isHostDown("foo"));
  }

  public void testIsRecentlyAccessedUrlNotConfigured() throws Exception {
    ConfigurationUtil.setFromArgs(ProxyManager.PARAM_URL_CACHE_ENABLED,
				  "false");
    String url = "http://foo.bar/blecch";
    assertFalse(mgr.isRecentlyAccessedUrl(url));
    mgr.setRecentlyAccessedUrl(url);
    assertFalse(mgr.isRecentlyAccessedUrl(url));
  }

  public void testIsRecentlyAccessedUrl() throws Exception {
    ConfigurationUtil.setFromArgs(ProxyManager.PARAM_URL_CACHE_ENABLED,
				  "true",
				  ProxyManager.PARAM_URL_CACHE_DURATION,
				  "1000");
    String url1 = "http://foo.bar/blecch";
    String url2 = "http://foo.bar/froople";
    TimeBase.setSimulated(1000);
    assertFalse(mgr.isRecentlyAccessedUrl(url1));
    mgr.setRecentlyAccessedUrl(url1);
    assertTrue(mgr.isRecentlyAccessedUrl(url1));
    assertFalse(mgr.isRecentlyAccessedUrl(url2));
    TimeBase.step(500);
    assertTrue(mgr.isRecentlyAccessedUrl(url1));
    TimeBase.step(600);
    assertFalse(mgr.isRecentlyAccessedUrl(url1));
  }

  void setNoIndexMode(String val) {
    if (val != null) {
      ConfigurationUtil.setFromArgs(ProxyManager.PARAM_NO_MANIFEST_INDEX_RESPONSES,
				    val);
    } else {
      // used to remove exiting config
      ConfigurationUtil.setFromArgs("borkborkbork", "1");
    }
  }

  public void testShowManifestIndexForResponse() throws Exception {
    assertTrue(mgr.showManifestIndexForResponse(404));
    assertTrue(mgr.showManifestIndexForResponse(403));

    setNoIndexMode("all");

    assertFalse(mgr.showManifestIndexForResponse(404));
    assertFalse(mgr.showManifestIndexForResponse(403));

    setNoIndexMode("404");

    assertFalse(mgr.showManifestIndexForResponse(404));
    assertTrue(mgr.showManifestIndexForResponse(403));
    assertTrue(mgr.showManifestIndexForResponse(401));

    setNoIndexMode("404;401");

    assertFalse(mgr.showManifestIndexForResponse(404));
    assertTrue(mgr.showManifestIndexForResponse(403));
    assertFalse(mgr.showManifestIndexForResponse(401));

    setNoIndexMode(null);

    assertTrue(mgr.showManifestIndexForResponse(404));
    assertTrue(mgr.showManifestIndexForResponse(403));
    assertTrue(mgr.showManifestIndexForResponse(401));

  }

  class MyProxyManager extends ProxyManager {
    protected void startProxy() {
    }
  }

}
