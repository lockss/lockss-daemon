/*
 * $Id: TestLockssApp.java,v 1.1 2004-08-02 02:59:35 tlipkis Exp $
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
  LockssApp app;

  public void setUp() throws Exception {
    super.setUp();
    app = new MockLockssApp(null);
  }

  // load & init default manager
  public void testInitManagerNoParam() throws Exception {
    LockssApp.ManagerDesc d1 =
      new LockssApp.ManagerDesc("param", mockMgrName);
    LockssManager m1 = app.initManager(d1);
    assertTrue(((MockMgr)m1).isInited());
  }

  // configure alternate manager class
  public void testInitManagerParam() throws Exception {
    ConfigurationUtil.setFromArgs(LockssApp.MANAGER_PREFIX + "param",
				  mockMgrName);
    LockssApp.ManagerDesc d1 =
      new LockssApp.ManagerDesc("param", "not.found");
    LockssManager m1 = app.initManager(d1);
    assertTrue(((MockMgr)m1).isInited());
  }

  // if configured class not found, fall back to default class
  public void testInitManagerParamFallback() throws Exception {
    ConfigurationUtil.setFromArgs(LockssApp.MANAGER_PREFIX + "param",
				  "not.found.class");
    LockssApp.ManagerDesc d1 =
      new LockssApp.ManagerDesc("param", mockMgrName);
    LockssManager m1 = app.initManager(d1);
    assertTrue(((MockMgr)m1).isInited());
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

  static final String mockMgrName = "org.lockss.app.TestLockssApp$MockMgr";
  static class MockMgr implements LockssManager {
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
    };
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

  static class MockLockssApp extends LockssApp {
    MockLockssApp(List propUrls) {
      super(propUrls);
    }

    protected ManagerDesc[] getManagerDescs() {
      return null;
    }

  }


}
