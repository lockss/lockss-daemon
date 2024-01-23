/*

Copyright (c) 2000-2024 Board of Trustees of Leland Stanford Jr. University,
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
    daemon = getMockLockssDaemon();
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
    daemonExitCode = -1;
  }

  public void tearDown() throws Exception {
    wdog.stopService();
    TimeBase.setReal();
    super.tearDown();
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
    int toolow = Thread.MIN_PRIORITY - 10;
    int toohigh = Thread.MAX_PRIORITY + 10;
    int justright = Thread.MIN_PRIORITY + 1;
    int dontchange = -1;
    TestThread thr = new TestThread("Test");

    ConfigurationUtil.setFromArgs("org.lockss.thread.foo.priority",
				  Integer.toString(toolow));
    assertEquals(Thread.MIN_PRIORITY, thr.getPriorityFromParam("foo",
								  9999));
    ConfigurationUtil.setFromArgs("org.lockss.thread.foo.priority",
				  Integer.toString(toohigh));
    assertEquals(Thread.MAX_PRIORITY, thr.getPriorityFromParam("foo",
								  9999));
    ConfigurationUtil.setFromArgs("org.lockss.thread.foo.priority",
				  Integer.toString(justright));
    assertEquals(justright, thr.getPriorityFromParam("foo", 9999));

    ConfigurationUtil.setFromArgs("org.lockss.thread.foo.priority",
				  Integer.toString(dontchange));
    assertEquals(dontchange, thr.getPriorityFromParam("foo", 9999));

    // not in config, use default
    assertEquals(justright, thr.getPriorityFromParam("foobar", justright));
    assertEquals(Thread.MIN_PRIORITY,
		 thr.getPriorityFromParam("foobar", toolow));
    assertEquals(Thread.MAX_PRIORITY,
		 thr.getPriorityFromParam("foobar", toohigh));
    assertEquals(dontchange,
		 thr.getPriorityFromParam("foobar", dontchange));
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

  public void testWaitExited() throws Exception {
    TimeBase.setReal();
    TestThread thr = new TestThread("Test");
    assertFalse(thr.waitRunning(Deadline.EXPIRED));
    runSem = new SimpleBinarySemaphore();
    thr.start();
    assertTrue(thr.waitRunning(Deadline.in(TIMEOUT_SHOULDNT)));
    assertTrue(thr.isAlive());
    assertFalse(thr.waitExited(Deadline.EXPIRED));
    runSem.give();
    assertTrue(thr.waitExited(Deadline.in(TIMEOUT_SHOULDNT)));
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
    ConfigurationUtil.addFromArgs(LockssThread.PARAM_THREAD_WDOG_HUNG_DUMP,
                                  "false");
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
    assertEquals(Constants.EXIT_CODE_THREAD_HUNG, daemonExitCode);
    assertEquals("Thread hung for 2000ms", daemonExitMsg);
  }

  // Same, but should produce a thread dump.
  //  (Normally disabled, as it doesn't automatically test anything more
  //   than the preceding test, and takes a couple seconds.  Used to
  //   manually check that thread dump is produced.)
  public void xtestDogHangThreadDump() throws Exception {
    Properties p = new Properties();
    p.put(LockssThread.PARAM_THREAD_WDOG_HUNG_DUMP, "true");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    TestThread thr = new TestThread("Test");
    goOn = true;
    dogInterval = 2000;
    stepTime = 10000;
    thr.start();
    if (!startSem.take(TIMEOUT_SHOULDNT)) {
      fail("Thread didn't start");
    }
    if (!stopSem.take(TIMEOUT_SHOULDNT * 2)) {
      fail("Thread didn't stop");
    }
    assertTrue(threadHung);
    assertEquals(Constants.EXIT_CODE_THREAD_HUNG, daemonExitCode);
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
    assertEquals(null, threadExitCause);
    assertTrue(threadExited);
    assertEquals(Constants.EXIT_CODE_THREAD_EXIT, daemonExitCode);
  }

  public void testTriggerOnError() throws Exception {
    TestThread thr = new TestThread("Test");
    goOn = true;
    triggerOnExit = true;
    dogInterval = 20000;
    stepTime = 1000;
    toThrow = new RuntimeException("Test RE");
    thr.start();
    if (!startSem.take(TIMEOUT_SHOULDNT)) {
      fail("Thread didn't start");
    }
    if (!stopSem.take(TIMEOUT_SHOULDNT)) {
      fail("Thread didn't stop");
    }
    // wait until thread exits, make sure it triggered threadExited()
    thr.join(TIMEOUT_SHOULDNT);
    assertEquals("java.lang.RuntimeException: Test RE",
		 threadExitCause.toString());
    assertTrue(threadExited);
    assertEquals(Constants.EXIT_CODE_THREAD_EXIT, daemonExitCode);
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
    assertEquals(Constants.EXIT_CODE_THREAD_EXIT, daemonExitCode);
    assertEquals("Thread exited", daemonExitMsg);
  }

  public void testTriggerOnOome() throws Exception {
    ConfigurationUtil.addFromArgs(LockssThread.PARAM_EXIT_DAEMON_ON_OOME, "true");
    TestThread thr = new TestThread("Test");
    goOn = true;
    triggerOnExit = false;
    dogInterval = 0;
    stepTime = 1000;
    toError = new OutOfMemoryError("Test OOME");
    thr.start();
    if (!startSem.take(TIMEOUT_SHOULDNT)) {
      fail("Thread didn't start");
    }
    if (!stopSem.take(TIMEOUT_SHOULDNT)) {
      fail("Thread didn't stop");
    }
    // wait until thread exits, make sure it triggered threadExited()
    thr.join(TIMEOUT_SHOULDNT);
    assertEquals(Constants.EXIT_CODE_THREAD_EXIT, daemonExitCode);
    assertEquals("Thread exited with OutOfMemoryError", daemonExitMsg);
  }

  SimpleBinarySemaphore startSem;
  SimpleBinarySemaphore stopSem;
  SimpleBinarySemaphore runSem;
  Error toError;
  RuntimeException toThrow;
  volatile boolean goOn;
  volatile long dogInterval;
  volatile long stepTime;
  volatile boolean triggerOnExit;
  volatile boolean threadHung;
  volatile boolean threadExited;
  volatile Throwable threadExitCause;
  volatile int daemonExitCode;
  volatile String daemonExitMsg;
  

  private class TestThread extends LockssThread {
    private boolean runSuper = false;

    private TestThread(String name) {
      super(name);
    }

    void setRunSuper(boolean flg) {
      runSuper = flg;
    }

    @Override
    public void lockssRun() {
      try {
	if (dogInterval != 0) {
	  startWDog(dogInterval);
	}
	if (triggerOnExit) {
	  triggerWDogOnExit(true);
	}
	nowRunning();
	startSem.give();
	if (toError != null) {
	  throw toError;
	}
	if (toThrow != null) {
	  throw toThrow;
	}
	if (runSem != null) {
	  runSem.take();
	}
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
      } finally {
	stopSem.give();
      }
    }

    @Override
    protected void exitDaemon(int exitCode, String msg) {
      daemonExitCode = exitCode;
      daemonExitMsg = msg;
    }

    @Override
    protected void threadHung() {
      threadHung = true;
      super.threadHung();
    }

    protected void threadExited(Throwable cause) {
      threadExited = true;
      threadExitCause = cause;
      super.threadExited(cause);
    }

  }

}
