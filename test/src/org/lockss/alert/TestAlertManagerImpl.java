/*
 * $Id: TestAlertManagerImpl.java,v 1.1 2004-07-12 06:09:41 tlipkis Exp $
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
  AlertManagerImpl mgr;

  public void setUp() throws Exception {
    super.setUp();
    daemon = new MockLockssDaemon();
    mgr = new AlertManagerImpl();
    daemon.setAlertManager(mgr);
    mgr.initService(daemon);
    daemon.setDaemonInited(true);
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

    public void record(LockssDaemon daemon, Alert alert) {
      list.add(alert);
    }

    public void record(LockssDaemon daemon, List alerts) {
      list.add(alerts);
    }

    public boolean isGroupable() {
      return isGroupable;
    }

    public long getMaxPendTime() {
      return 100;
    }

//     public boolean equals(Object obj);
//     public int hashCode();

    List getAlerts() {
      return list;
    }
  }

}
