/*
 * $Id: TestBinarySemaphore.java,v 1.4 2002-11-19 23:31:29 tal Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;

import java.util.*;
import java.io.*;
import junit.framework.TestCase;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;


/**
 * This is the test class for org.lockss.util.TestBinarySemaphore
 *
 */

public class TestBinarySemaphore extends LockssTestCase{
  public static Class testedClasses[] = {
    org.lockss.util.BinarySemaphore.class
  };

  private static final int FAIL_TIMEOUT = 2000;

  public TestBinarySemaphore(String msg) {
    super(msg);
  }

  /** Giver gives a semaphore in a while */
  class Giver extends DoLater {
    BinarySemaphore sem;

    Giver(long waitMs, BinarySemaphore sem) {
      super(waitMs);
      this.sem = sem;
    }

    protected void doit() {
      sem.give();
    }
  }

  /** Give the smeaphore in a while */
  private Giver giveIn(long ms, BinarySemaphore sem) {
    Giver g = new Giver(ms, sem);
    g.start();
    return g;
  }

  /** Expirer expires a timer in a while */
  class Expirer extends DoLater {
    Deadline timer;
    Expirer(long waitMs, Deadline timer) {
      super(waitMs);
      this.timer = timer;
    }

    protected void doit() {
      timer.expire();
    }
  }

  /** Expire the timer in a while */
  private Expirer expireIn(long ms, Deadline timer) {
    Expirer e = new Expirer(ms, timer);
    e.start();
    return e;
  }

  public void testNoWait1Empty() {
    BinarySemaphore sem = new BinarySemaphore();
    Interrupter intr = null;
    try {
      intr = interruptMeIn(FAIL_TIMEOUT);
      if (sem.take(Deadline.in(0))) {
	fail("take(0) of empty semaphore returned true");
      }
      intr.cancel();
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	fail("take(0) of empty didn't return");
      }
    }
  }

  public void testNoWait1Full() {
    BinarySemaphore sem = new BinarySemaphore();
    sem.give();
    Interrupter intr = null;
    try {
      intr = interruptMeIn(FAIL_TIMEOUT);
      if (!sem.take(Deadline.in(0))) {
	fail("take(0) of full semaphore returned false");
      }
      intr.cancel();
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	fail("take(0) of full didn't return");
      }
    }
  }

  public void testNoWait2Empty() {
    BinarySemaphore sem = new BinarySemaphore();
    Interrupter intr = null;
    try {
      intr = interruptMeIn(FAIL_TIMEOUT);
      if (sem.take(null)) {
	fail("take(0) of empty semaphore returned true");
      }
      intr.cancel();
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	fail("take(0) of empty didn't return");
      }
    }
  }

  public void testNoWait2Full() {
    BinarySemaphore sem = new BinarySemaphore();
    sem.give();
    Interrupter intr = null;
    try {
      intr = interruptMeIn(FAIL_TIMEOUT);
      if (!sem.take(null)) {
	fail("take(0) of full semaphore returned false");
      }
      intr.cancel();
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	fail("take(0) of full didn't return");
      }
    }
  }

  public void testTimedEmpty() {
    BinarySemaphore sem = new BinarySemaphore();
    Interrupter intr = null;
    Deadline t = Deadline.in(500);
    Date start = new Date();
    try {
      intr = interruptMeIn(FAIL_TIMEOUT);
      if (sem.take(t)) {
	fail("take(500) of empty semaphore returned true");
      }
      long delay = TimerUtil.timeSince(start);
      System.out.println("take returned in " + delay);
      if (delay < 200) {
	fail("take(500) of empty returned early in " + delay);
      }
      if (delay > 900) {
	System.out.println("pre fail 1");
	fail("take(500) of empty returned late in " + delay);
	System.out.println("post fail 1");
      }
      intr.cancel();
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	System.out.println("pre fail 2");
	fail("take(" + t + ") of empty failed to timeout in " +
	     TimerUtil.timeSince(start));
	System.out.println("post fail 2");
      }
    }
  }

  public void testTimedEmpty2() {
    BinarySemaphore sem = new BinarySemaphore();
    Interrupter intr = null;
    Deadline t = Deadline.in(500);
    intr = interruptMeIn(FAIL_TIMEOUT);
    Date start = new Date();
    try {
    if (sem.take(t)) {
      fail("take(500) of empty semaphore returned true");
    }
    long delay = TimerUtil.timeSince(start);
    System.out.println("take returned in " + delay);
    if (intr.did()) {
      System.out.println("pre fail intr");
      fail("take(" + t + ") of empty failed to timeout in " +
	   TimerUtil.timeSince(start));
      System.out.println("post fail intr");
    }
    if (delay < 200) {
      fail("take(500) of empty returned early in " + delay);
    }
    if (delay > 900) {
      System.out.println("pre fail 1");
      fail("take(500) of empty returned late in " + delay);
      System.out.println("post fail 1");
    }
    } catch (InterruptedException e) {
    } finally {
      intr.cancel();
    }
  }

  public void testTimedEmptyForcedExpire() {
    BinarySemaphore sem = new BinarySemaphore();
    Interrupter intr = null;
    Expirer expr = null;
    try {
      Date start = new Date();
      intr = interruptMeIn(FAIL_TIMEOUT);
      Deadline t = Deadline.in(2000);
      expr = expireIn(500, t);
      if (sem.take(t)) {
	fail("take(2000) of empty returned true");
      }
      long delay = TimerUtil.timeSince(start);
      if (delay < 400) {
	fail("take(2000) of empty returned early in " + delay);
      }
      if (delay > 800) {
	fail("take(2000) of empty expire()d late in " + delay);
      }
      expr.cancel();
      intr.cancel();
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	fail("take(2000) of empty failed to timeout");
      }
    }
  }

  public void testTimedFull() {
    BinarySemaphore sem = new BinarySemaphore();
    sem.give();
    Interrupter intr = null;
    try {
      Date start = new Date();
      intr = interruptMeIn(FAIL_TIMEOUT);
      if (!sem.take(Deadline.in(1000))) {
	fail("take(1000) of full returned false");
      }
      long delay = TimerUtil.timeSince(start);
      if (delay > 750) {
	fail("take(1000) of full timed out in " + delay);
      }
      intr.cancel();
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	fail("take(1000) of full failed to return");
      }
    }
  }

  public void testTimedGive() {
    BinarySemaphore sem = new BinarySemaphore();
    Interrupter intr = null;
    try {
      Date start = new Date();
      intr = interruptMeIn(FAIL_TIMEOUT);
      giveIn(500, sem);
      if (!sem.take(Deadline.in(1000))) {
	fail("take(1000) of semaphore given() after 500 returned false");
      }
      long delay = TimerUtil.timeSince(start);
      if (delay < 500) {
	fail("take(1000), given in 500, returned early in " + delay);
      }
      if (delay > 750) {
	fail("take(1000), given in 500, timed out in " + delay );
      }
      intr.cancel();
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	fail("take(1000), given in 500, neither returned nor timed out");
      }
    }
  }
}

