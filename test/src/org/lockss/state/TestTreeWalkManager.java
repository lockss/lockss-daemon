/*
 * $Id: TestTreeWalkManager.java,v 1.1 2004-08-21 06:52:49 tlipkis Exp $
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

package org.lockss.state;

import java.io.*;
import java.util.*;
import org.lockss.app.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

public class TestTreeWalkManager extends LockssTestCase {
  private MockArchivalUnit mau = null;
  private TreeWalkManager twm;

  private MockLockssDaemon theDaemon;
  Collection cookies;

  public void setUp() throws Exception {
    super.setUp();
    cookies = new ArrayList();
    theDaemon = getMockLockssDaemon();
    twm = new TreeWalkManager();
    twm.initService(theDaemon);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testConfig() throws Exception {
    ConfigurationUtil.setFromArgs("org.lockss.treewalk.interval", "100");
    assertEquals(100, twm.paramTreeWalkIntervalMin);
    assertEquals(140, twm.paramTreeWalkIntervalMax);
  }

  public void testExecuteOk() throws Exception {
    twm.startService();
    Runner r = new Runner();
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    r.postMe = sem;
    twm.execute(r);
    assertTrue("Runner didn't execute", sem.take(TIMEOUT_SHOULDNT));
  }

  public void testExecuteSeveral() throws Exception {
    ConfigurationUtil.setFromArgs(TreeWalkManager.
				  PARAM_TREEWALK_THREAD_POOL_MAX, "5");

    twm.startService();
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    twm.execute(new Runner("1", sem));
    twm.execute(new Runner("2", sem));
    twm.execute(new Runner("3", sem));
    twm.execute(new Runner("4", sem));
    assertCookies(sem, SetUtil.set("1", "2", "3", "4"));
  }

  public void testExecuteFail() throws Exception {
    ConfigurationUtil.setFromArgs(TreeWalkManager.
				  PARAM_TREEWALK_THREAD_POOL_MAX, "3");
    twm.startService();
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    Runner r = new Runner();
    r.waitFor = sem;
    twm.execute(r);
    twm.execute(r);
    twm.execute(r);
    try {
      twm.execute(r);
      fail("Fourth task should not have been accepted");
    } catch (RuntimeException e) {
    }
  }

  /** assert that all the cookies appear on the cookies list, failing if
   * TIMEOUT_SHOULDNT elapses before that happens */
  void assertCookies(SimpleBinarySemaphore sem, Collection expectedCookies) {
    while (cookies.size() < expectedCookies.size()) {
      if (! sem.take(TIMEOUT_SHOULDNT)) {
	break;
      }
    }
    assertEquals(SetUtil.theSet(expectedCookies), SetUtil.theSet(cookies));
  }

  synchronized void addCookie(Object cookie) {
    cookies.add(cookie);
  }

  class Runner implements Runnable {
    SimpleBinarySemaphore waitFor = null;
    SimpleBinarySemaphore postMe = null;
    Object cookie = null;

    Runner() {
    }
    Runner(Object cookie) {
      this.cookie = cookie;
    }
    Runner(Object cookie, SimpleBinarySemaphore postMe) {
      this(cookie);
      this.postMe = postMe;
    }
    public void run() {
      if (waitFor != null) waitFor.take();
      if (cookie != null) addCookie(cookie);
      if (postMe != null) postMe.give();
    }
  }
}
