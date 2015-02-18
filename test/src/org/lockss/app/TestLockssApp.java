/*
 * $Id$
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
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * This is the test class for org.lockss.util.LockssApp
 */
public class TestLockssApp extends LockssTestCase {
  MyMockLockssApp app;

  public void setUp() throws Exception {
    super.setUp();
    app = new MyMockLockssApp(null);
  }

  // load & init default manager
  public void testInitManagerNoParam() throws Exception {
    LockssApp.ManagerDesc d1 =
      new LockssApp.ManagerDesc("param", mockMgrName);
    LockssManager m1 = app.initManager(d1);
    assertTrue(((MyMockMgr)m1).isInited());
  }

  // configure alternate manager class
  public void testInitManagerParam() throws Exception {
    ConfigurationUtil.setFromArgs(LockssApp.MANAGER_PREFIX + "param",
				  mockMgrName);
    LockssApp.ManagerDesc d1 =
      new LockssApp.ManagerDesc("param", "not.found");
    LockssManager m1 = app.initManager(d1);
    assertTrue(((MyMockMgr)m1).isInited());
  }

  // if configured class not found, fall back to default class
  public void testInitManagerParamFallback() throws Exception {
    ConfigurationUtil.setFromArgs(LockssApp.MANAGER_PREFIX + "param",
				  "not.found.class");
    LockssApp.ManagerDesc d1 =
      new LockssApp.ManagerDesc("param", mockMgrName);
    LockssManager m1 = app.initManager(d1);
    assertTrue(((MyMockMgr)m1).isInited());
  }

  // fail if class not LockssManager
  public void testInitManagerNotManager() throws Exception {
    LockssApp.ManagerDesc d1 =
      new LockssApp.ManagerDesc("param", "java.lang.String");
    try {
      LockssManager m1 = app.initManager(d1);
      fail("initManager() shouldn't succeed on non-LockssManager class");
    } catch (ClassCastException e) {
    }
  }

  // Configured class not LockssManager shouldn't cause fallback to default
  public void testInitManagerParamNoFallbackIfNotManager() throws Exception {
    ConfigurationUtil.setFromArgs(LockssApp.MANAGER_PREFIX + "param",
				  "java.lang.String");
    LockssApp.ManagerDesc d1 =
      new LockssApp.ManagerDesc("param", mockMgrName);
    try {
      LockssManager m1 = app.initManager(d1);
      fail("initManager() shouldn't succeed on non-LockssManager class");
    } catch (ClassCastException e) {
    }
  }

  public void testInitManagers() throws Exception {
    LockssApp.ManagerDesc[] descrs = {
      new LockssApp.ManagerDesc("mgr_1", MockMgr1.class.getName()),
      new LockssApp.ManagerDesc("mgr_2", MockMgr2.class.getName()) {
      public boolean shouldStart() {
	return false;
      }},
      new LockssApp.ManagerDesc("mgr_3", MockMgr3.class.getName()),
    };
    app.setDescrs(descrs);

    app.initManagers();

    MockLockssManager mgr1 = (MockLockssManager)LockssApp.getManager("mgr_1");
    assertTrue(mgr1 instanceof MockMgr1);
    assertEquals(1, mgr1.inited);
    assertEquals(1, mgr1.started);
    assertEquals(0, mgr1.stopped);
    MockLockssManager mgr3 = (MockLockssManager)LockssApp.getManager("mgr_3");
    assertTrue(mgr3 instanceof MockMgr3);
    assertEquals(1, mgr3.inited);
    assertEquals(1, mgr3.started);
    assertEquals(0, mgr3.stopped);
    try {
      LockssApp.getManager("mgr_2");
      fail("mgr_2 shouldn't have been created");
    } catch (IllegalArgumentException e) {
    }

    app.stop();
    assertEquals(1, mgr1.stopped);
    assertEquals(1, mgr3.stopped);
  }

  static final String mockMgrName = MyMockMgr.class.getName();
  static class MyMockMgr implements LockssManager {
    boolean isInited = false;

    public void initService(LockssApp app)
	throws LockssAppException {
      isInited = true;
    }

    public void startService() {
    }

    public void stopService() {
    }
    public LockssApp getApp() {
      throw new UnsupportedOperationException("Not implemented");
    }

    boolean isInited() {
      return isInited;
    }
  }

  static class Event {
    Object caller;
    String event;
    Object arg;
    Event(Object caller, String event, Object arg) {
      this.caller = caller;
      this.event = event;
      this.arg = arg;
    }
    Event(Object caller, String event) {
      this(caller, event, null);
    }
    public String toString() {
      return "[Ev: " + caller + "." + event + "(" + arg + ")";
    }
    public boolean equals(Object o) {
      if (o instanceof Event) {
	Event oe = (Event)o;
	return caller == oe.caller && arg == oe.arg &&
	  StringUtil.equalStrings(event, oe.event);
      }
      return false;
    }
  }
  List events;

  static class MyMockLockssApp extends LockssApp {
    ManagerDesc[] descrs = null;

    MyMockLockssApp(List propUrls) {
      super(propUrls);
    }

    protected ManagerDesc[] getManagerDescs() {
      return descrs;
    }

    void setDescrs(ManagerDesc[] descrs) {
      this.descrs = descrs;
    }
  }

  static class MockLockssManager implements LockssManager {
    int inited = 0;
    int started = 0;
    int stopped = 0;

    public void initService(LockssApp app) throws LockssAppException {
      inited++;
    }

    public void startService() {
      started++;
    }

    public void stopService() {
      stopped++;
    }

    public LockssApp getApp() {
      return null;
    }
  }

  static class MockMgr1 extends MockLockssManager {
  }

  static class MockMgr2 extends MockLockssManager {
  }

  static class MockMgr3 extends MockLockssManager {
  }
}
