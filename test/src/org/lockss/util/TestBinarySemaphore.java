/*
 * $Id: TestBinarySemaphore.java,v 1.9 2006-04-05 22:08:28 tlipkis Exp $
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

public class TestBinarySemaphore extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.util.BinarySemaphore.class
  };

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
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
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
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
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
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
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
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
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
    Deadline t = Deadline.in(100);
    Date start = new Date();
    try {
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      if (sem.take(t)) {
	fail("take(100) of empty semaphore returned true");
      }
      long delay = TimerUtil.timeSince(start);
      System.out.println("take returned in " + delay);
      if (delay < 80) {
	fail("take(100) of empty returned early in " + delay);
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

  public void testTimedEmptyForcedExpire() {
    BinarySemaphore sem = new BinarySemaphore();
    Interrupter intr = null;
    Expirer expr = null;
    try {
      Date start = new Date();
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      Deadline t = Deadline.in(2000);
      expr = expireIn(100, t);
      if (sem.take(t)) {
	fail("take(2000) of empty returned true");
      }
      long delay = TimerUtil.timeSince(start);
      if (delay < 80) {
	fail("take(2000) of empty returned early in " + delay);
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
      intr = interruptMeIn(TIMEOUT_SHOULDNT * 2, true);
      Date start = new Date();
      boolean res = sem.take(Deadline.in(TIMEOUT_SHOULDNT));
      long delay = TimerUtil.timeSince(start);
      if (!res) {
	fail("take(" +TIMEOUT_SHOULDNT+ ") of full returned false in "+delay);
      }
      if (delay > TIMEOUT_SHOULDNT) {
	fail("take(" + TIMEOUT_SHOULDNT + ") of full correctly returned TRUE, but not until timer expired in " + delay);
      }
      intr.cancel();
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	fail("take(" + TIMEOUT_SHOULDNT + ") of full failed to return");
      }
    }
  }

  public void testTimedGive() {
    BinarySemaphore sem = new BinarySemaphore();
    Interrupter intr = null;
    try {
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      giveIn(100, sem);
      Date start = new Date();
      if (!sem.take(Deadline.in(1000))) {
	fail("take(1000) of semaphore given() after 100 returned false");
      }
      long delay = TimerUtil.timeSince(start);
      if (delay < 80) {
	fail("take(1000), given in 100, returned early in " + delay);
      }
      // bogus - it returned true; ok if it took longer than expected
//       if (delay > 750) {
// 	fail("take(1000), given in 100, timed out in " + delay );
//       }
      intr.cancel();
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	fail("take(1000), given in 100, neither returned nor timed out");
      }
    }
  }
}

