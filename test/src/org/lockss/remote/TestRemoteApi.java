/*
 * $Id: TestRemoteApi.java,v 1.2 2004-01-12 06:20:26 tlipkis Exp $
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

package org.lockss.remote;

import java.io.*;
import java.util.*;
import junit.framework.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.repository.*;

/**
 * Test class for org.lockss.remote.RemoteApi
 */
public class TestRemoteApi extends LockssTestCase {

  static final String AUID1 = "AUID_1";
  static final String PID1 = "PID_1";

  MockLockssDaemon daemon;
  MockPluginManager mpm;
  RemoteApi rapi;

  public void setUp() throws Exception {
    super.setUp();

    daemon = getMockLockssDaemon();
    mpm = new MockPluginManager();
    mpm.mockInit();
    daemon.setPluginManager(mpm);
    rapi = new RemoteApi();
    daemon.setRemoteApi(rapi);
    daemon.setDaemonInited(true);
    rapi.initService(daemon);
    rapi.startService();
  }

  public void tearDown() throws Exception {
    rapi.stopService();
    daemon.stopDaemon();
    super.tearDown();
  }

  public void testFindAuProxy() {
    MockArchivalUnit mau1 = new MockArchivalUnit();
    AuProxy aup = rapi.findAuProxy(mau1);
    assertNotNull(aup);
    assertSame(mau1, aup.getAu());
    assertSame(aup, rapi.findAuProxy(mau1));
    ArchivalUnit mau2 = mpm.getAuFromId(AUID1);
    assertNotNull(mau2);
    AuProxy aup2b = rapi.findAuProxy(mau2);
    AuProxy aup2a = rapi.findAuProxy(AUID1);
    assertNotNull(aup2a);
    assertSame(aup2a, aup2b);
  }

  public void testMapAus() {
    MockArchivalUnit mau1 = new MockArchivalUnit();
    ArchivalUnit mau2 = mpm.getAuFromId(AUID1);
    List mapped = rapi.mapAusToProxies(ListUtil.list(mau1, mau2));
    assertEquals(2, mapped.size());
    assertNotNull(mapped.get(0));
    assertNotNull(mapped.get(1));
    assertSame(rapi.findAuProxy(mau1), (AuProxy)mapped.get(0));
    assertSame(rapi.findAuProxy(mau2), (AuProxy)mapped.get(1));
  }

  public void testFindPluginProxy() throws Exception {
    MockPlugin mp1 = new MockPlugin();
    PluginProxy pp1 = rapi.findPluginProxy(mp1);
    assertNotNull(pp1);
    assertSame(mp1, pp1.getPlugin());
    assertSame(pp1, rapi.findPluginProxy(mp1));
    Plugin mp2 = mpm.getPlugin(mpm.pluginKeyFromId(PID1));
    assertNotNull(mp2);
    PluginProxy pp2b = rapi.findPluginProxy(mp2);
    PluginProxy pp2a = rapi.findPluginProxy(PID1);
    assertNotNull(pp2a);
    assertSame(pp2a, pp2b);
  }

  public void testMapPlugins() {
    MockPlugin mp1 = new MockPlugin();
    Plugin mp2 = mpm.getPlugin(mpm.pluginKeyFromId(PID1));
    List mapped = rapi.mapPluginsToProxies(ListUtil.list(mp1, mp2));
    assertEquals(2, mapped.size());
    assertNotNull(mapped.get(0));
    assertNotNull(mapped.get(1));
    assertSame(rapi.findPluginProxy(mp1), (PluginProxy)mapped.get(0));
    assertSame(rapi.findPluginProxy(mp2), (PluginProxy)mapped.get(1));
  }

  class MockPluginManager extends PluginManager {
    void mockInit() {
      MockArchivalUnit mau1 = new MockArchivalUnit();
      mau1.setAuId(AUID1);
      putAuInMap(mau1);
      MockPlugin mp1 = new MockPlugin();
      mp1.setPluginId(PID1);
      setPlugin(pluginKeyFromId(mp1.getPluginId()), mp1);
    }
  }
}
