/*
 * $Id$
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
import org.lockss.test.*;


/**
 * Test class for <code>org.lockss.util.Deadline</code>
 */

public class TestDeadline extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.util.Deadline.class
  };
  static final int MAX_DURATION = 9999999;

  public TestDeadline(String msg) {
    super(msg);
  }

  protected void tearDown() throws Exception {
    TimeBase.setReal();
    super.tearDown();
  }

  private long getDuration(Deadline d) throws Exception {
    Long i = (Long)PrivilegedAccessor.invokeMethod(d, "getDuration");
    return i.longValue();
  }

  public void testDuration() throws Exception {
    TimeBase.setSimulated();
    Random random = new Random();
    // with no range, duration should always be the same
    for (int ix = 0; ix < 5; ix++) {
      int r = random.nextInt(MAX_DURATION);
      Deadline p = Deadline.in(r);
      assertEquals(r, getDuration(p));
    }
    // with a range, duration should be within the right range, and
    // should not always be the same.
    for (int ix = 0; ix < 5; ix++) {
      int r = random.nextInt(MAX_DURATION);
      Deadline p0 = Deadline.inRandomRange(r - 1000, r + 1000);
      boolean differs = false;
      for (int rpt = 0; rpt < 10; rpt++) {
	Deadline p = Deadline.inRandomRange(r - 1000, r + 1000);
	long pd = getDuration(p);
	assertTrue(pd > (r - 10000) && pd < (r + 10000));
	if (getDuration(p0) != pd) {
	  differs = true;
	  break;
	}
      }
      if (!differs) {
	fail("10 instances of Deadline.inRandomRange(" + (r - 1000) +
	     ", " + (r + 1000) + ")" +
	     " all had the same duration: " + getDuration(p0));
      }
    }
  }

  public void testBefore() {
    TimeBase.setSimulated();
    Deadline p1 = Deadline.in(100);
    Deadline p2 = Deadline.in(200);
    assertFalse(p1.before(p1));
    assertTrue(p1.before(p2));
    assertFalse(p2.before(p1));
  }

  public void testCompareTo() {
    TimeBase.setSimulated();
    Deadline p1 = Deadline.in(100);
    Deadline p2 = Deadline.in(200);
    Deadline p3 = Deadline.in(200);
    assertTrue(p1.compareTo(p1) == 0);
    assertTrue(p2.compareTo(p3) == 0);
    assertTrue(p1.compareTo(p2) < 0);
    assertTrue(p2.compareTo(p1) > 0);
  }

  public void testEquals() {
    TimeBase.setSimulated();
    Deadline p1 = Deadline.in(100);
    Deadline p2 = Deadline.in(200);
    Deadline p3 = Deadline.in(200);
    assertTrue(p1.equals(p1));
    assertTrue(p2.equals(p3));
    assertTrue(p3.equals(p2));
    assertFalse(p1.equals(p2));
    assertFalse(p2.equals(p1));
    assertFalse(p1.equals("foo"));
  }

  public void testHashCode() {
    TimeBase.setSimulated();
    Deadline p1 = Deadline.in(200);
    Deadline p2 = Deadline.in(200);
    assertTrue(p1.hashCode() == p2.hashCode());
  }

  public void testMinus() {
    TimeBase.setSimulated();
    Deadline p1 = Deadline.in(100);
    Deadline p2 = Deadline.in(200);
    assertEquals(0, p1.minus(p1));
    assertEquals(100, p2.minus(p1));
    assertEquals(-100, p1.minus(p2));
  }

  public void testMAX() {
    TimeBase.setSimulated();
    Deadline never = Deadline.MAX;
    try {
      never.expire();
      fail("Constant deadline allowed modification");
    } catch (UnsupportedOperationException e) {
    }
    try {
      never.expireIn(20);
      fail("Constant deadline allowed modification");
    } catch (UnsupportedOperationException e) {
    }
    try {
      never.expireAt(40);
      fail("Constant deadline allowed modification");
    } catch (UnsupportedOperationException e) {
    }
    try {
      never.later(45);
      fail("Constant deadline allowed modification");
    } catch (UnsupportedOperationException e) {
    }
    try {
      never.sooner(45);
      fail("Constant deadline allowed modification");
    } catch (UnsupportedOperationException e) {
    }
    assertFalse(never.expired());
  }

  public void testEXPIRED() {
    TimeBase.setSimulated();
    Deadline never = Deadline.EXPIRED;
    try {
      never.expire();
      fail("Constant deadline allowed modification");
    } catch (UnsupportedOperationException e) {
    }
    try {
      never.expireIn(20);
      fail("Constant deadline allowed modification");
    } catch (UnsupportedOperationException e) {
    }
    try {
      never.expireAt(40);
      fail("Constant deadline allowed modification");
    } catch (UnsupportedOperationException e) {
    }
    try {
      never.later(45);
      fail("Constant deadline allowed modification");
    } catch (UnsupportedOperationException e) {
    }
    try {
      never.sooner(45);
      fail("Constant deadline allowed modification");
    } catch (UnsupportedOperationException e) {
    }
    assertTrue(never.expired());
  }

  public void testEarliest() {
    TimeBase.setSimulated();
    Deadline p1 = Deadline.in(100);
    Deadline p2 = Deadline.in(200);
    assertEquals(Deadline.in(100), Deadline.earliest(p1, p2));
    assertEquals(Deadline.in(100), Deadline.earliest(p2, p1));
    assertEquals(Deadline.in(100), Deadline.earliest(p1, p1));
    assertEquals(Deadline.in(100), Deadline.earliest(Deadline.MAX, p1));
  }

  public void testLatest() {
    TimeBase.setSimulated();
    Deadline p1 = Deadline.in(100);
    Deadline p2 = Deadline.in(200);
    assertEquals(Deadline.in(200), Deadline.latest(p1, p2));
    assertEquals(Deadline.in(200), Deadline.latest(p2, p1));
    assertEquals(Deadline.in(100), Deadline.latest(p1, p1));
    assertEquals(Deadline.MAX, Deadline.latest(Deadline.MAX, p1));
  }

  public void testGetSleepTime() {
    Deadline t = Deadline.in(0);
    assertTrue(t.expired());
    assertTrue(t.getSleepTime() > 0);
  }

  public void testSleep() {
    Interrupter intr = null;
    try {
      Date start = new Date();
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      Deadline t = Deadline.in(100);
      while (!t.expired()) {
	try {
	  t.sleep();
	} catch (InterruptedException e) {
	  if (intr.did()) throw e;
	}
      }
      long delay = TimerUtil.timeSince(start);
      if (delay < 80) {
	fail("sleep(100) returned early in " + delay);
      }
      assertTrue(t.expired());
      intr.cancel();
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	fail("sleep(100) failed to return within 2 seconds");
      }
    }
  }

  public void testFaster() {
    Interrupter intr = null;
    DoLater doer = null;
    try {
      final Deadline t = Deadline.inRandomRange(TIMEOUT_SHOULDNT + 950,
						TIMEOUT_SHOULDNT + 1050);
      Date start = new Date();
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      doer = new DoLater(100) {
	  protected void doit() {
	    t.sooner(TIMEOUT_SHOULDNT + 800);
	  }
	};
      doer.start();
      while (!t.expired()) {
	try {
	  t.sleep();
	} catch (InterruptedException e) {
	  if (intr.did()) throw e;
	}
      }
      long delay = TimerUtil.timeSince(start);
      if (delay < 130) {
	fail("sleep(950, 1050), faster(800) returned early in " + delay);
      }
      intr.cancel();
      doer.cancel();
    } catch (InterruptedException e) {
    } finally {
      assertTrue("sleep(950, 1050) returned before doer did faster()",
		 doer.did());
      assertTrue("sleep(950, 1050) failed to return within 2 seconds",
		 !intr.did());
    }
  }

  public void testSlower() {
    TimeBase.setSimulated();
    Deadline t = Deadline.in(200);
    t.later(300);
    assertEquals(500, t.getRemainingTime());
  }

  public void testForceExpire() {
    Interrupter intr = null;
    Expirer expr = null;
    try {
      Date start = new Date();
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      Deadline t = Deadline.inRandomRange(TIMEOUT_SHOULDNT + 1450,
					  TIMEOUT_SHOULDNT + 1550);
      expr = expireIn(100, t);
      while (!t.expired()) {
	try {
	  t.sleep();
	} catch (InterruptedException e) {
	  if (intr.did()) throw e;
	}
      }
      long delay = TimerUtil.timeSince(start);
      if (delay < 80) {
	fail("sleep(1450, 1550) expired early in " + delay);
      }
      intr.cancel();
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	fail("sleep(1450, 1550) failed to expire within 2 seconds");
      }
    }
  }

  Deadline called = null;

  public void testCallback() {
    Deadline.Callback cb = new Deadline.Callback() {
	public void changed(Deadline deadline) {
	  called = deadline;
	}};
    Deadline d1 = Deadline.in(10000);
    Deadline d2 = Deadline.in(5000);
    d1.registerCallback(cb);
    assertSame(null, called);
    d2.expire();
    assertSame(null, called);
    d1.expire();
    assertSame(d1, called);
  }

  public void testInterruptedCallback() {
    Deadline.InterruptCallback cb = new Deadline.InterruptCallback();
    Expirer expr = null;
    Deadline d = Deadline.in(100);
    try {
      d.registerCallback(cb);
      Date start = new Date();
      // expire the deadline at about the same time it's supposed to go
      // off.  If there's a race condition in the interrupt handling,
      // hopefully this will occasionally trigger it
      expr = expireIn(99, d);
      d.sleep();
    } catch (InterruptedException e) {
    } finally {
      cb.disable();
      d.unregisterCallback(cb);
    }
    assertFalse(Thread.currentThread().isInterrupted());
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

}
