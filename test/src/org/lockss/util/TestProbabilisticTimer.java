/*
 * $Id: TestProbabilisticTimer.java,v 1.3 2002-09-19 21:03:31 tal Exp $
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
import org.lockss.test.*;


/**
 * Test class for <code>org.lockss.util.ProbabilisticTimer</code>
 */

public class TestProbabilisticTimer extends TestCase {
  public static Class testedClasses[] = {
    org.lockss.util.ProbabilisticTimer.class
  };
  static final int MAX_DURATION = 9999999;

  public TestProbabilisticTimer(String msg) {
    super(msg);
  }

  public void testDuration() {
    Random random = new Random();
    // with no range, duration should always be the same
    for (int ix = 0; ix < 5; ix++) {
      int r = random.nextInt(MAX_DURATION);
      ProbabilisticTimer p = new ProbabilisticTimer(r);
      assertEquals(r, p.getDuration());
    }
    // with a range, duration should be within the right range, and
    // should not always be the same.
    for (int ix = 0; ix < 5; ix++) {
      int r = random.nextInt(MAX_DURATION);
      ProbabilisticTimer p0 = new ProbabilisticTimer(r, 1000.0);
      boolean differs = false;
      for (int rpt = 0; rpt < 10; rpt++) {
	ProbabilisticTimer p = new ProbabilisticTimer(r, 1000.0);
	long pd = p.getDuration();
	assertTrue(pd > (r - 10000) && pd < (r + 10000));
	if (p0.getDuration() != pd) {
	  differs = true;
	}
      }
      if (!differs) {
	fail("10 instances of new ProbabilisticTimer(" + r + ", 100.0)" +
	     " all had the same duration: " + p0.getDuration());
      }
    }
  }

  public void testCompare() {
    ProbabilisticTimer p1 = new ProbabilisticTimer(100);
    ProbabilisticTimer p2 = new ProbabilisticTimer(200);
    assertTrue(!p1.isShorterThan(p1));
    assertTrue(p1.isShorterThan(p2));
    assertTrue(!p2.isShorterThan(p1));
  }    

  public void testSleepUntil() {
    DoLater.Interrupter intr = null;
    try {
      Date start = new Date();
      intr = DoLater.interruptMeIn(2000);
      ProbabilisticTimer t = new ProbabilisticTimer(500, 50);
      assertTrue(!t.expired());
      t.sleepUntil();
      long delay = TimerUtil.timeSince(start);
      if (delay < 200) {
	fail("sleepUntil(500, 50) returned early in " + delay);
      }
      assertTrue(t.expired());
      if (delay > 800) {
	fail("sleepUntil(500, 50) returned late in " + delay);
      }
    } finally {
      intr.cancel();
      if (intr.did()) {
	fail("sleepUntil(500, 50) failed to return within 2 seconds");
      }
    }
  }

  public void testFaster() {
    DoLater.Interrupter intr = null;
    DoLater doer = null;
    try {
      final ProbabilisticTimer t = new ProbabilisticTimer(1000, 50);
      Date start = new Date();
      intr = DoLater.interruptMeIn(2000);
      doer = new DoLater(200) {
	  protected void doit() {
	    t.faster(500);
	  }
	};
      doer.start();
      t.sleepUntil();
      long delay = TimerUtil.timeSince(start);
      if (delay < 300) {
	fail("sleepUntil(1000, 50), faster(500) returned early in " + delay);
      }
      if (delay > 700) {
	fail("sleepUntil(1000, 50), faster(500) returned late in " + delay);
      }
    } finally {
      intr.cancel();
      doer.cancel();
      if (!doer.did()) {
	fail("sleepUntil(1000, 50) returned before doer did faster()");
      }
      if (intr.did()) {
	fail("sleepUntil(1000, 50) failed to return within 2 seconds");
      }
    }
  }

  public void testSlower() {
    DoLater.Interrupter intr = null;
    try {
      ProbabilisticTimer t = new ProbabilisticTimer(200, 5);
      t.slower(300);
      Date start = new Date();
      intr = DoLater.interruptMeIn(2000);
      t.sleepUntil();
      long delay = TimerUtil.timeSince(start);
      if (delay < 300) {
	fail("sleepUntil(200, 5), slower(300) returned early in " + delay);
      }
      if (delay > 700) {
	fail("sleepUntil(200, 5), slower(300) returned late in " + delay);
      }
    } finally {
      intr.cancel();
      if (intr.did()) {
	fail("sleepUntil(200, 5) failed to return within 2 seconds");
      }
    }
  }

  public void testForceExpire() {
    DoLater.Interrupter intr = null;
    Expirer expr = null;
    try {
      Date start = new Date();
      intr = DoLater.interruptMeIn(2000);
      ProbabilisticTimer t = new ProbabilisticTimer(1500, 50);
      expr = expireIn(500, t);
      t.sleepUntil();
      long delay = TimerUtil.timeSince(start);
      if (delay < 400) {
	fail("sleepUntil(1500, 50) expired early in " + delay);
      }
      if (delay > 800) {
	fail("sleepUntil(1500, 50) expired late in " + delay);
      }
    } finally {
      intr.cancel();
      if (intr.did()) {
	fail("sleepUntil(1500, 50) failed to expire within 2 seconds");
      }
    }
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

}
