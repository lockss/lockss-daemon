/*
 * $Id: TestLockssThread.java,v 1.3.2.1 2004-02-12 03:08:28 tlipkis Exp $
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
nin this Software without prior written authorization from Stanford University.

*/

package org.lockss.daemon;

import java.util.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.test.*;


/**
 * Test class for org.lockss.daemon.LockssThread
 */

public class TestLockssThread extends LockssTestCase {
//   protected static Logger log = Logger.getLogger("TestLockssThread");

  LockssDaemon daemon;
  WatchdogService wdog;

  public void setUp() throws Exception {
    super.setUp();
    // LockssTestCase disables the thread watchdog by default.  Enable it
    // for these tests.
    enableThreadWatchdog();
    daemon = new MockLockssDaemon();
    wdog = daemon.getWatchdogService();
    TimeBase.setSimulated();

    startSem = new SimpleBinarySemaphore();
    stopSem = new SimpleBinarySemaphore();
    goOn = false;
    dogInterval = 0;
    stepTime = 0;
    triggerOnExit = false;
    threadHung = false;
    threadExited = false;
  }

  public void tearDown() throws Exception {
    wdog.stopService();
    TimeBase.setReal();
    super.tearDown();
  }

  private void config(String file, String interval) {
    config(new Properties(), file, interval);
  }

  private void config(Properties props, String file, String interval) {
    props.put(WatchdogService.PARAM_PLATFORM_WDOG_FILE, file);
    props.put(WatchdogService.PARAM_PLATFORM_WDOG_INTERVAL, interval);
    ConfigurationUtil.setCurrentConfigFromProps(props);
  }


  public void testGetIntervalFromParam() {
    Properties p = new Properties();
    p.put("org.lockss.thread.foo.watchdog.interval", "123");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    TestThread thr = new TestThread("Test");
    assertEquals(432, thr.getIntervalFromParam("foobar", 432));
    assertEquals(123, thr.getIntervalFromParam("foo", 432));
  }

  public void testGetPriorityFromParam() {
    Properties p = new Properties();
    p.put("org.lockss.thread.foo.priority", "1");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    TestThread thr = new TestThread("Test");
    assertEquals(432, thr.getPriorityFromParam("foobar", 432));
    assertEquals(1, thr.getPriorityFromParam("foo", 432));
  }

  // ensure LockssThread gets started correctly
  public void testStart() throws Exception {
    TestThread thr = new TestThread("Test");
    thr.start();
    if (!startSem.take(TIMEOUT_SHOULDNT)) {
      fail("Thread didn't start");
    }
  }

  public void testWaitRunning() throws Exception {
    TimeBase.setReal();
    TestThread thr = new TestThread("Test");
    assertFalse(thr.waitRunning(Deadline.EXPIRED));
    thr.start();
    assertTrue(thr.waitRunning(Deadline.in(TIMEOUT_SHOULDNT)));
  }

  // Thread updates watchdog frequently enough
  public void testDogNoHang() throws Exception {
    TestThread thr = new TestThread("Test");
    goOn = true;
    dogInterval = 2000;
    stepTime = 1000;
    thr.start();
    if (!startSem.take(TIMEOUT_SHOULDNT)) {
      fail("Thread didn't start");
    }
    if (!stopSem.take(TIMEOUT_SHOULDNT)) {
      fail("Thread didn't stop");
    }
    assertFalse(threadHung);
    // wait until thread exits, make sure it didn't trigger threadExited()
    thr.join(TIMEOUT_SHOULDNT);
    assertFalse(threadExited);
  }

  // Thread does not update watchdog frequently enough
  public void testDogHang() throws Exception {
    TestThread thr = new TestThread("Test");
    goOn = true;
    dogInterval = 2000;
    stepTime = 10000;
    thr.start();
    if (!startSem.take(TIMEOUT_SHOULDNT)) {
      fail("Thread didn't start");
    }
    if (!stopSem.take(TIMEOUT_SHOULDNT)) {
      fail("Thread didn't stop");
    }
    assertTrue(threadHung);
  }

  public void testTriggerOnExit() throws Exception {
    TestThread thr = new TestThread("Test");
    goOn = true;
    triggerOnExit = true;
    dogInterval = 20000;
    stepTime = 1000;
    thr.start();
    if (!startSem.take(TIMEOUT_SHOULDNT)) {
      fail("Thread didn't start");
    }
    if (!stopSem.take(TIMEOUT_SHOULDNT)) {
      fail("Thread didn't stop");
    }
    // wait until thread exits, make sure it triggered threadExited()
    thr.join(TIMEOUT_SHOULDNT);
    assertTrue(threadExited);
  }

  public void testTriggerOnExitNoInterval() throws Exception {
    TestThread thr = new TestThread("Test");
    goOn = true;
    triggerOnExit = true;
    dogInterval = 0;
    stepTime = 0;
    thr.start();
    if (!startSem.take(TIMEOUT_SHOULDNT)) {
      fail("Thread didn't start");
    }
    if (!stopSem.take(TIMEOUT_SHOULDNT)) {
      fail("Thread didn't stop");
    }
    // wait until thread exits, make sure it triggered threadExited()
    thr.join(TIMEOUT_SHOULDNT);
    assertTrue(threadExited);
  }

  SimpleBinarySemaphore startSem;
  SimpleBinarySemaphore stopSem;
  volatile boolean goOn;
  volatile long dogInterval;
  volatile long stepTime;
  volatile boolean triggerOnExit;
  volatile boolean threadHung;
  volatile boolean threadExited;

  private class TestThread extends LockssThread {

    private TestThread(String name) {
      super(name);
    }

    public void lockssRun() {

      if (dogInterval != 0) {
	startWDog(dogInterval);
      }
      if (triggerOnExit) {
	triggerWDogOnExit(true);
      }
      nowRunning();
      startSem.give();
      if (dogInterval != 0) {
	for (int ix = 0; ix < 10; ix++) {
	  if (stepTime != 0) {
	    TimeBase.step(stepTime);
	  }
	  if (dogInterval != 0) {
	    pokeWDog();
	  }
	  TimerUtil.guaranteedSleep(10);
	}
      }
      stopSem.give();
    }

    protected void threadHung() {
      threadHung = true;
    }

    protected void threadExited() {
      threadExited = true;
    }

  }

}
