/*
 * $Id: TestLockssDaemon.java,v 1.13 2007-05-23 02:26:54 tlipkis Exp $
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
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * This is the test class for org.lockss.util.LockssDaemon
 */
public class TestLockssDaemon extends LockssTestCase {

  public void testGetStartupOptions() throws Exception {
    // good options.
    String[] test1 = {"-p", "foo;bar;baz",
		      "-g", "test1-group"};
    String[] test2 = {"-p", "foo",
		      "-p", "bar",
		      "-p", "baz"};
    String[] test3 = {"-p", "foo;bar;baz",
		      "-p", "quux",
		      "-g", "test3-group"};
    String[] test4 = {"-p", "foo1;bar1;baz1",
		      "-p", "foo2;bar2;baz2"};


    // bad options (-p without argument, should be ignored)
    String[] test5 = {"-p", "foo",
		      "-p"};
    // bad options (-p without argument, should be ignored)
    String[] test6 = {"-g", "test6-group",
		      "-p"};
    // bad options (-g without argument, should be ignored)
    String[] test7 = {"-p", "foo",
		      "-g"};

    // good options using the old method (no -p or -g)
    String[] test8 = {"foo", "bar", "baz"};

    // Ensure that only one URL is chosen from a semicolon-separated
    // list of URLs
    LockssDaemon.StartupOptions opt1 =
      LockssDaemon.getStartupOptions(test1);
    assertNotNull(opt1.getGroupNames());
    assertEquals("test1-group", opt1.getGroupNames());
    List list1 = opt1.getPropUrls();
    assertNotNull(list1);
    assertEquals(1, list1.size());
    assertTrue("foo".equals(list1.get(0)) ||
	       "bar".equals(list1.get(0)) ||
	       "baz".equals(list1.get(0)));

    // Ensure that multiple prop URLs can be set with multiple "-p"
    // options.
    LockssDaemon.StartupOptions opt2 =
      LockssDaemon.getStartupOptions(test2);
    // Must be null!  No group specified.
    assertNull(opt2.getGroupNames());
    List list2 = opt2.getPropUrls();
    assertNotNull(list2);
    assertEquals(3, list2.size());
    assertEquals("foo", list2.get(0));
    assertEquals("bar", list2.get(1));
    assertEquals("baz", list2.get(2));

    // Ensure that only one URL is chosen from a semicolon-separated
    // list of URLs, and that additional -p parameters can be provided.
    LockssDaemon.StartupOptions opt3 =
      LockssDaemon.getStartupOptions(test3);
    assertNotNull(opt3.getGroupNames());
    assertEquals("test3-group", opt3.getGroupNames());
    List list3 = opt3.getPropUrls();
    assertNotNull(list3);
    assertEquals(2, list3.size());
    assertTrue("foo".equals(list3.get(0)) ||
	       "bar".equals(list3.get(0)) ||
	       "baz".equals(list3.get(0)));
    assertEquals("quux", list3.get(1));

    // Ensure that only one URL is chosen from each semicolon-separated
    // list of URLs
    LockssDaemon.StartupOptions opt4 =
      LockssDaemon.getStartupOptions(test4);
    assertNull(opt4.getGroupNames());
    List list4 = opt4.getPropUrls();
    assertNotNull(list4);
    assertEquals(2, list4.size());
    assertTrue("foo1".equals(list4.get(0)) ||
	       "bar1".equals(list4.get(0)) ||
	       "baz1".equals(list4.get(0)));
    assertTrue("foo2".equals(list4.get(1)) ||
	       "bar2".equals(list4.get(1)) ||
	       "baz2".equals(list4.get(1)));

    // Test some bad options.  Second -p should be ignored.
    LockssDaemon.StartupOptions opt5 =
      LockssDaemon.getStartupOptions(test5);
    assertNull(opt5.getGroupNames());
    List list5 = opt5.getPropUrls();
    assertEquals(1, list5.size());
    assertEquals("foo", list5.get(0));

    // -p should be ignored, no prop URLS.
    LockssDaemon.StartupOptions opt6 =
      LockssDaemon.getStartupOptions(test6);
    assertNotNull(opt6.getGroupNames());
    assertEquals("test6-group", opt6.getGroupNames());
    List list6 = opt6.getPropUrls();
    assertNotNull(list6);
    assertEquals(0, list6.size());

    // -g should be ignored, no group name.
    LockssDaemon.StartupOptions opt7 =
      LockssDaemon.getStartupOptions(test7);
    assertNull(opt7.getGroupNames());
    List list7 = opt7.getPropUrls();
    assertNotNull(list7);
    assertEquals(1, list7.size());
    assertEquals("foo", list7.get(0));

    // Compatibility with old startup options, no flags.
    LockssDaemon.StartupOptions opt8 =
      LockssDaemon.getStartupOptions(test8);
    assertNull(opt8.getGroupNames());
    List list8 = opt8.getPropUrls();
    assertNotNull(list8);
    assertEquals(3, list8.size());
    assertEquals("foo", list8.get(0));
    assertEquals("bar", list8.get(1));
    assertEquals("baz", list8.get(2));
  }

  // AU specific manager tests

  public void testStartAuManager() throws Exception {
    LockssDaemon daemon = new TestAuLockssDaemon();
    Configuration config = ConfigManager.newConfiguration();
    MockArchivalUnit mau1 = new MockArchivalUnit();
    mau1.setAuId("mau1");
    MockArchivalUnit mau2 = new MockArchivalUnit();
    mau2.setAuId("mau2");

    TestAuMgr.clearEvents();
    daemon.startOrReconfigureAuManagers(mau1, config);

    TestAuMgr1 mgr11 = (TestAuMgr1)daemon.getAuManager("MgrKey1", mau1);
    TestAuMgr2 mgr21 = (TestAuMgr2)daemon.getAuManager("MgrKey2", mau1);
    assertSame(mau1, mgr11.getAu());
    assertSame(mau1, mgr21.getAu());
    assertEquals(ListUtil.list(mgr11), daemon.getAuManagersOfType("MgrKey1"));
    assertEquals(ListUtil.list(mgr21), daemon.getAuManagersOfType("MgrKey2"));
    Event[] exp1 = {
      new Event(mgr11, "initService", daemon),
      new Event(mgr21, "initService", daemon),
      new Event(mgr11, "setAuConfig", config),
      new Event(mgr21, "setAuConfig", config),
      new Event(mgr11, "startService", null),
      new Event(mgr21, "startService", null),
    };
    assertIsomorphic(exp1, TestAuMgr.getEvents());
    Configuration config2 = ConfigManager.newConfiguration();
    config2.put("1", "2");
    TestAuMgr.clearEvents();
    daemon.startOrReconfigureAuManagers(mau1, config2);
    Event[] exp2 = {
      new Event(mgr11, "setAuConfig", config2),
      new Event(mgr21, "setAuConfig", config2),
    };
    assertIsomorphic(exp2, TestAuMgr.getEvents());

    try {
      daemon.getAuManager("MgrKey1", mau2);
    } catch (IllegalArgumentException e) {
    }

    daemon.startOrReconfigureAuManagers(mau2, config2);
    TestAuMgr1 mgr12 = (TestAuMgr1)daemon.getAuManager("MgrKey1", mau2);
    TestAuMgr2 mgr22 = (TestAuMgr2)daemon.getAuManager("MgrKey2", mau2);
    assertSame(mau2, mgr12.getAu());
    assertSame(mau2, mgr22.getAu());
    assertEquals(SetUtil.set(mgr11, mgr12),
		 SetUtil.theSet(daemon.getAuManagersOfType("MgrKey1")));
    assertEquals(SetUtil.set(mgr21, mgr22),
		 SetUtil.theSet(daemon.getAuManagersOfType("MgrKey2")));
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

  class TestAuLockssDaemon extends LockssDaemon {
    ManagerDesc[] testAuManagerDescs = {
      new ManagerDesc("MgrKey1", TestAuMgr1Factory.class.getName()),
      new ManagerDesc("MgrKey2", TestAuMgr2Factory.class.getName()),
    };

    TestAuLockssDaemon() {
      super(null);
    }

    protected ManagerDesc[] getAuManagerDescs() {
      return testAuManagerDescs;
    }
  }

  static class TestAuMgr implements LockssAuManager {
    static List events;
    static void clearEvents() {
      events = new ArrayList();
    }
    static List getEvents() {
      return events;
    }
    private ArchivalUnit au;
    TestAuMgr(ArchivalUnit au) {
      this.au = au;
    }
    ArchivalUnit getAu() {
      return au;
    }
    protected void setConfig(Configuration newConfig,
			     Configuration prevConfig,
			     Configuration.Differences changedKeys) {
    }
    public void initService(LockssApp app) {
      events.add(new Event(this, "initService", app));
    }
    public void startService() {
      events.add(new Event(this, "startService"));
    }

    public void stopService() {
      events.add(new Event(this, "stopService"));
    }
    public LockssApp getApp() {
      throw new UnsupportedOperationException("Not implemented");
    }

    public void setAuConfig(Configuration auConfig) {
      events.add(new Event(this, "setAuConfig", auConfig));
    }
  }
  static class TestAuMgr1 extends TestAuMgr {
    TestAuMgr1(ArchivalUnit au) {
      super(au);
    }
  }
  static class TestAuMgr1Factory implements LockssAuManager.Factory {
    public LockssAuManager createAuManager(ArchivalUnit au) {
      return new TestAuMgr1(au);
    }
  }
  static class TestAuMgr2 extends TestAuMgr {
    TestAuMgr2(ArchivalUnit au) {
      super(au);
    }
  }
  static class TestAuMgr2Factory implements LockssAuManager.Factory {
    public LockssAuManager createAuManager(ArchivalUnit au) {
      return new TestAuMgr2(au);
    }
  }

}
