/*
 * $Id: TestAbstractAlertAction.java,v 1.2 2004-09-21 21:24:55 dshr Exp $
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

package org.lockss.alert;

import java.io.*;
import java.util.*;
import java.net.*;
import java.text.*;
import org.lockss.app.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * This is the test class for org.lockss.alert.AbstractAlertAction
 */
public class TestAbstractAlertAction extends LockssTestCase {
  private static Logger log = Logger.getLogger("TestAbstractAlertAction");

  MyMockAlertAction action;

  public void setUp() throws Exception {
    super.setUp();
    action = new MyMockAlertAction();
  }

  public void testSimple() {
    assertFalse(action.isGroupable());
    assertEquals(0, action.getMaxPendTime());
  }

  public void testRecordList() {
    List exp = ListUtil.list(new Alert("a"), new Alert("b"));
    action.record(null, exp);
    assertEquals(exp, action.getAlerts());
  }

  static class MyMockAlertAction extends AbstractAlertAction {
    List list = new ArrayList();

    public void record(LockssDaemon daemon, Alert alert) {
      list.add(alert);
    }

    List getAlerts() {
      return list;
    }
  }
}
