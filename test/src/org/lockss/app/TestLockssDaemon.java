/*
 * $Id: TestLockssDaemon.java,v 1.2 2003-11-19 08:46:46 tlipkis Exp $
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

package org.lockss.app;

import java.util.*;
import org.lockss.test.*;

/**
 * This is the test class for org.lockss.util.LockssDaemon
 */
public class TestLockssDaemon extends LockssTestCase {

  // load & init default manager
  public void testInitManagerNoParam() throws Exception {
    LockssDaemon daemon = new LockssDaemon(null);
    LockssDaemon.ManagerDesc d1 =
      new LockssDaemon.ManagerDesc("param", mockMgrName);
    LockssManager m1 = daemon.initManager(d1);
    assertTrue(((MockMgr)m1).isInited());
  }

  // configure alternate manager class
  public void testInitManagerParam() throws Exception {
    LockssDaemon daemon = new LockssDaemon(null);
    ConfigurationUtil.setFromArgs(LockssDaemon.MANAGER_PREFIX + "param",
				  mockMgrName);
    LockssDaemon.ManagerDesc d1 =
      new LockssDaemon.ManagerDesc("param", "not.found");
    LockssManager m1 = daemon.initManager(d1);
    assertTrue(((MockMgr)m1).isInited());
  }

  // if configured class not found, fall back to default class
  public void testInitManagerParamFallback() throws Exception {
    LockssDaemon daemon = new LockssDaemon(null);
    ConfigurationUtil.setFromArgs(LockssDaemon.MANAGER_PREFIX + "param",
				  "not.found.class");
    LockssDaemon.ManagerDesc d1 =
      new LockssDaemon.ManagerDesc("param", mockMgrName);
    LockssManager m1 = daemon.initManager(d1);
    assertTrue(((MockMgr)m1).isInited());
  }

  // fail if class not LockssManager
  public void testInitManagerNotManager() throws Exception {
    LockssDaemon daemon = new LockssDaemon(null);
    LockssDaemon.ManagerDesc d1 =
      new LockssDaemon.ManagerDesc("param", "java.lang.String");
    try {
      LockssManager m1 = daemon.initManager(d1);
      fail("initManager() shouldn't succeed on non-LockssManager class");
    } catch (ClassCastException e) {
    }
  }

  // Configured class not LockssManager shouldn't cause fallback to default
  public void testInitManagerParamNoFallbackIfNotManager() throws Exception {
    LockssDaemon daemon = new LockssDaemon(null);
    ConfigurationUtil.setFromArgs(LockssDaemon.MANAGER_PREFIX + "param",
				  "java.lang.String");
    LockssDaemon.ManagerDesc d1 =
      new LockssDaemon.ManagerDesc("param", mockMgrName);
    try {
      LockssManager m1 = daemon.initManager(d1);
      fail("initManager() shouldn't succeed on non-LockssManager class");
    } catch (ClassCastException e) {
    }
  }

  static final String mockMgrName = "org.lockss.app.TestLockssDaemon$MockMgr";
  static class MockMgr implements LockssManager {
    boolean isInited = false;

    public void initService(LockssDaemon daemon)
	throws LockssDaemonException {
      isInited = true;
    }

    public void startService() {
    }

    public void stopService() {
    }
    boolean isInited() {
      return isInited;
    }
  }

}
