/*
 * $Id: TestProxyManager.java,v 1.1 2005-02-02 00:15:39 tlipkis Exp $
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

    ConfigurationUtil.setFromArgs(ProxyManager.PARAM_LOCKSS_USER_AGENT_IMPLIES_REPAIR, "false");
    req.setField("user-agent", "LOCKSS cache");
    assertFalse(mgr.isRepairRequest(req));
    ConfigurationUtil.setFromArgs(ProxyManager.PARAM_LOCKSS_USER_AGENT_IMPLIES_REPAIR, "true");
    assertTrue(mgr.isRepairRequest(req));
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

  class MyProxyManager extends ProxyManager {
    protected void startProxy() {
    }
  }

}
