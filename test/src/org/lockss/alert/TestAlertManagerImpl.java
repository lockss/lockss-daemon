/*
 * $Id: TestAlertManagerImpl.java,v 1.2 2004-08-02 03:04:13 tlipkis Exp $
 */

/*

Copyright (c) 2000-2004 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.alert;

import java.util.*;
import java.io.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.test.*;


/**
 * This is the test class for org.lockss.alert.AlertManager
 */

public class TestAlertManagerImpl extends LockssTestCase {
  static Logger log = Logger.getLogger("TestAlertManager");

  MockLockssDaemon daemon;
  MockAlertManagerImpl mgr;

  public void setUp() throws Exception {
    super.setUp();
    daemon = new MockLockssDaemon();
    mgr = new MockAlertManagerImpl();
    daemon.setAlertManager(mgr);
    mgr.initService(daemon);
    daemon.setDaemonInited(true);
    mgr.startService();
  }

  public void tearDown() throws Exception {
    mgr.stopService();
    daemon.stopDaemon();
    super.tearDown();
  }

  public void testFindMatchingActions() {
    Alert alert = new Alert("foo");
    MockAlertAction action1 = new MockAlertAction();
    MockAlertAction action2 = new MockAlertAction();
    MockAlertPattern patT = new MockAlertPattern(true);
    MockAlertPattern patF = new MockAlertPattern(false);
    AlertConfig conf =
      new AlertConfig(ListUtil.list(new AlertFilter(patT, action1),
				    new AlertFilter(patT, action1),
				    new AlertFilter(patF, action2)));
    Set actions = mgr.findMatchingActions(alert, conf.getFilters());
    assertEquals(SetUtil.set(action1), actions);
  }

  public void testMarshallConfig() throws Exception {
    AlertPattern pat1 = AlertPatterns.True();
    AlertPattern pat2 = AlertPatterns.False();
    AlertAction act1 = AlertActions.Mail("foo@bar");
    AlertFilter filt1 = new AlertFilter(pat1, act1);
    testMarshallConfig(new AlertConfig(ListUtil.list(filt1)));
  }

  public void testMarshallConfig(AlertConfig config) throws Exception {
    File file = FileTestUtil.tempFile("foo");
    mgr.storeAlertConfig(file, config);
    assertTrue(file.exists());
    AlertConfig c2 = (AlertConfig)mgr.load(file, AlertConfig.class);
//     assertEquals(config, c2);
  }

  public void config(boolean enable) {
    config(enable, 0, 0, 0);
  }

  public void config(boolean enable, long init, long incr, long max) {
    Properties p = new Properties();
    p.put(AlertManagerImpl.PARAM_ALERTS_ENABLED, enable ? "true" : "false");
    p.put(AlertManagerImpl.PARAM_DELAY_INITIAL, Long.toString(init));
    p.put(AlertManagerImpl.PARAM_DELAY_INCR, Long.toString(incr));
    p.put(AlertManagerImpl.PARAM_DELAY_MAX, Long.toString(max));
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }

  public void testRaiseSingleAlerts() throws Exception {
    log.debug("testRaiseAlert()");
    config(true);
    Alert a1 = new Alert("foo");
    a1.setAttribute(Alert.ATTR_IS_TIME_CRITICAL, true);
    MockAlertAction action = new MockAlertAction();
    action.setGroupable(true);
    AlertConfig conf =
      new AlertConfig(ListUtil.list(new AlertFilter(AlertPatterns.True(),
						    action)));
    mgr.suppressStore(true);
    mgr.updateConfig(conf);
    mgr.raiseAlert(a1);
    List recs = action.getAlerts();
    assertEquals(ListUtil.list(a1), recs);

    mgr.raiseAlert(a1);
    recs = action.getAlerts();
    assertEquals(ListUtil.list(a1, a1), recs);
  }

  public void testRaiseDelayedAlerts1() throws Exception {
    TimeBase.setSimulated(1000);
    log.debug("testRaiseAlert()");
    config(true, 100, 200, 500);
    Alert a1 = new Alert("foo");
    a1.setAttribute(Alert.ATTR_IS_TIME_CRITICAL, false);
    MockAlertAction action = new MockAlertAction();
    action.setGroupable(true);
    action.setMaxPend(10000);
    AlertConfig conf =
      new AlertConfig(ListUtil.list(new AlertFilter(AlertPatterns.True(),
						    action)));
    mgr.suppressStore(true);
    mgr.updateConfig(conf);
    mgr.raiseAlert(a1);
    assertEquals(ListUtil.list(a1), action.getAlerts());

    TimeBase.step(10);
    mgr.raiseAlert(a1);
    assertEquals(ListUtil.list(a1), action.getAlerts());
    TimeBase.step(10);
    assertEquals(ListUtil.list(a1), action.getAlerts());
    TimeBase.step(200);
    assertEquals(ListUtil.list(a1, a1), action.getAlerts());
  }

  public void testRaiseDelayedAlerts2() throws Exception {
    TimeBase.setSimulated(1000);
    log.debug("testRaiseAlert()");
    config(true, 100, 200, 500);
    Alert a1 = new Alert("foo");
    a1.setAttribute(Alert.ATTR_IS_TIME_CRITICAL, false);
    MockAlertAction action = new MockAlertAction();
    action.setGroupable(true);
    action.setMaxPend(10000);
    AlertConfig conf =
      new AlertConfig(ListUtil.list(new AlertFilter(AlertPatterns.True(),
						    action)));
    mgr.suppressStore(true);
    mgr.updateConfig(conf);
    mgr.raiseAlert(a1);
    assertEquals(ListUtil.list(a1), action.getAlerts());

    TimeBase.step(10);
    mgr.raiseAlert(a1);
    assertEquals(ListUtil.list(a1), action.getAlerts());
    int cnt = 0;
    while (cnt < (500 - 10) / 90) {
      TimeBase.step(90);
      mgr.raiseAlert(a1);
      assertEquals(ListUtil.list(a1), action.getAlerts());
      cnt++;
    }
    TimeBase.step(90);
    assertEquals(2, action.getAlerts().size());
    Object o1 = action.getAlerts().get(1);
    assertTrue("Second set of recorded actions should be a list",
	       o1 instanceof List);
    List l1 = (List)o1;
    assertEquals(cnt+1, l1.size());
  }

  class MockAlertPattern implements AlertPattern {
    boolean match;			// determines result
    Alert alert;		   // records the alert we were called with
    MockAlertPattern(boolean match) {
      this.match = match;
    }
    public boolean isMatch(Alert alert) {
      this.alert = alert;
      return match;
    }
    Alert getAlert() {
      return alert;
    }
  }
  class MockAlertAction implements AlertAction {
    boolean isGroupable;
    List list = new ArrayList();
    long maxPend = Constants.WEEK;

    public void record(LockssDaemon daemon, Alert alert) {
      list.add(alert);
    }

    public void record(LockssDaemon daemon, List alerts) {
      list.add(alerts);
    }

    public boolean isGroupable() {
      return isGroupable;
    }

    public void setGroupable(boolean isGroupable) {
      this.isGroupable = isGroupable;
    }

    public long getMaxPendTime() {
      return maxPend;
    }

    void setMaxPend(long maxPend) {
      this.maxPend = maxPend;
    }

//     public boolean equals(Object obj);
//     public int hashCode();

    List getAlerts() {
      return list;
    }
  }

  class MockAlertManagerImpl extends AlertManagerImpl {
    boolean suppressStore = false;
    void storeAlertConfig(File file, AlertConfig alertConfig)
	throws Exception {
      if (!suppressStore) {
	super.storeAlertConfig(file, alertConfig);
      }
    }
    void suppressStore(boolean suppress) {
      this.suppressStore = suppress;
    }
  }

}
