/*
 * $Id: TestBinarySemaphore.java,v 1.1 2002-09-02 04:13:08 tal Exp $
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
 * @author  Thomas S. Robertson
 * @version 0.0
 */

public class TestBinarySemaphore extends TestCase{
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
    ProbabilisticTimer timer;
    Expirer(long waitMs, ProbabilisticTimer timer) {
      super(waitMs);
      this.timer = timer;
    }

    protected void doit() {
      timer.expire();
    }
  }

  /** Expire the timer in a while */
  private Expirer expireIn(long ms, ProbabilisticTimer timer) {
    Expirer e = new Expirer(ms, timer);
    e.start();
    return e;
  }

  public void testNoWait1Empty() {
    BinarySemaphore sem = new BinarySemaphore();
    DoLater.Interrupter intr = null;
    try {
      intr = DoLater.interruptMeIn(1000);
      if (sem.take(new ProbabilisticTimer(0))) {
	fail("take(0) of empty semaphore returned true");
      }
    } finally {
      intr.cancel();
      if (intr.did()) {
	fail("take(0) of empty didn't return");
      }
    }
  }

  public void testNoWait1Full() {
    BinarySemaphore sem = new BinarySemaphore();
    sem.give();
    DoLater.Interrupter intr = null;
    try {
      intr = DoLater.interruptMeIn(1000);
      if (!sem.take(new ProbabilisticTimer(0))) {
	fail("take(0) of full semaphore returned false");
      }
    } finally {
      intr.cancel();
      if (intr.did()) {
	fail("take(0) of full didn't return");
      }
    }
  }

  public void testNoWait2Empty() {
    BinarySemaphore sem = new BinarySemaphore();
    DoLater.Interrupter intr = null;
    try {
      intr = DoLater.interruptMeIn(1000);
      if (sem.take(null)) {
	fail("take(0) of empty semaphore returned true");
      }
    } finally {
      intr.cancel();
      if (intr.did()) {
	fail("take(0) of empty didn't return");
      }
    }
  }

  public void testNoWait2Full() {
    BinarySemaphore sem = new BinarySemaphore();
    sem.give();
    DoLater.Interrupter intr = null;
    try {
      intr = DoLater.interruptMeIn(1000);
      if (!sem.take(null)) {
	fail("take(0) of full semaphore returned false");
      }
    } finally {
      intr.cancel();
      if (intr.did()) {
	fail("take(0) of full didn't return");
      }
    }
  }

  public void testTimedEmpty() {
    BinarySemaphore sem = new BinarySemaphore();
    DoLater.Interrupter intr = null;
    try {
      Date start = new Date();
      intr = DoLater.interruptMeIn(2000);
      if (sem.take(new ProbabilisticTimer(500, 50))) {
	fail("take(500, 50) of empty semaphore returned true");
      }
      long delay = TimerUtil.timeSince(start);
      if (delay < 200) {
	fail("take(500, 50) of empty returned early in " + delay);
      }
      if (delay > 900) {
	fail("take(500, 50) of empty returned late in " + delay);
      }
    } finally {
      intr.cancel();
      if (intr.did()) {
	fail("take(500, 50) of empty failed to timeout");
      }
    }
  }

  public void testTimedEmptyForcedExpire() {
    BinarySemaphore sem = new BinarySemaphore();
    DoLater.Interrupter intr = null;
    Expirer expr = null;
    try {
      Date start = new Date();
      intr = DoLater.interruptMeIn(4000);
      ProbabilisticTimer t = new ProbabilisticTimer(2000, 50);
      expr = expireIn(500, t);
      if (sem.take(t)) {
	fail("take(2000, 50) of empty returned true");
      }
      long delay = TimerUtil.timeSince(start);
      if (delay < 400) {
	fail("take(2000, 50) of empty returned early in " + delay);
      }
      if (delay > 800) {
	fail("take(2000, 50) of empty expire()d late in " + delay);
      }
    } finally {
      expr.cancel();
      intr.cancel();
      if (intr.did()) {
	fail("take(2000, 50) of empty failed to timeout");
      }
    }
  }

  public void testTimedFull() {
    BinarySemaphore sem = new BinarySemaphore();
    sem.give();
    DoLater.Interrupter intr = null;
    try {
      Date start = new Date();
      intr = DoLater.interruptMeIn(2000);
      if (!sem.take(new ProbabilisticTimer(1000, 50))) {
	fail("take(1000, 50) of full returned false");
      }
      long delay = TimerUtil.timeSince(start);
      if (delay > 750) {
	fail("take(1000, 50) of full timed out in " + delay);
      }
    } finally {
      intr.cancel();
      if (intr.did()) {
	fail("take(1000, 50) of full failed to return");
      }
    }
  }

  public void testTimedGive() {
    BinarySemaphore sem = new BinarySemaphore();
    DoLater.Interrupter intr = null;
    try {
      Date start = new Date();
      intr = DoLater.interruptMeIn(2000);
      giveIn(500, sem);
      if (!sem.take(new ProbabilisticTimer(1000, 50))) {
	fail("take(1000, 50) of semaphore given() after 500 returned false");
      }
      long delay = TimerUtil.timeSince(start);
      if (delay < 500) {
	fail("take(1000, 50), given in 500, returned early in " + delay);
      }
      if (delay > 750) {
	fail("take(1000, 50), given in 500, timed out in " + delay );
      }
    } finally {
      intr.cancel();
      if (intr.did()) {
	fail("take(1000, 50), given in 500, neither returned nor timed out");
      }
    }
  }
}

