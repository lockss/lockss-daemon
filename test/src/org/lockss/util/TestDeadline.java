/*
 * $Id: TestDeadline.java,v 1.5 2002-12-16 00:59:01 tal Exp $
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

  private long getDuration(Deadline d) throws Exception {
    Long i = (Long)PrivilegedAccessor.invokeMethod(d, "getDuration");
    return i.longValue();
  }

  public void testDuration() throws Exception {
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

  public void testCompare() {
    Deadline p1 = Deadline.in(100);
    Deadline p2 = Deadline.in(200);
    assertTrue(!p1.before(p1));
    assertTrue(p1.before(p2));
    assertTrue(!p2.before(p1));
  }    

  public void testSleep() {
    Interrupter intr = null;
    try {
      Date start = new Date();
      intr = interruptMeIn(TIMEOUT_SHOULDNT);
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
      if (delay > 800) {
	fail("sleep(100) returned late in " + delay);
      }
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
      final Deadline t = Deadline.inRandomRange(950, 1050);
      Date start = new Date();
      intr = interruptMeIn(TIMEOUT_SHOULDNT);
      doer = new DoLater(100) {
	  protected void doit() {
	    t.sooner(800);
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
      if (delay > 900) {
	fail("sleep(950, 1050), faster(800) returned late in " + delay);
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
    TimeBase.setReal();
  }

  public void testForceExpire() {
    Interrupter intr = null;
    Expirer expr = null;
    try {
      Date start = new Date();
      intr = interruptMeIn(TIMEOUT_SHOULDNT);
      Deadline t = Deadline.inRandomRange(1450, 1550);
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
      if (delay > 800) {
	fail("sleep(1450, 1550) expired late in " + delay);
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
