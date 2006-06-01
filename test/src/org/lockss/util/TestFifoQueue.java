/*
 * $Id: TestFifoQueue.java,v 1.2 2006-06-01 23:57:09 tlipkis Exp $
 */

/*

Copyright (c) 2000-2004 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.util.*;
import org.lockss.test.*;


/**
 * Test class for org.lockss.util.FifoQueue
 */

public class TestFifoQueue extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.util.FifoQueue.class
  };

  String O1 = "foo";
  Integer O2 = new Integer(42);

  public TestFifoQueue(String msg) {
    super(msg);
  }

  /** Putter puts something onto a queue in a while */
  class Putter extends DoLater {
    FifoQueue queue;
    Object obj;

    Putter(long waitMs, FifoQueue queue, Object obj) {
      super(waitMs);
      this.queue = queue;
      this.obj = obj;
    }

    protected void doit() {
      queue.put(obj);
    }
  }

  /** Put something onto a queue in a while */
  private Putter putIn(long ms, FifoQueue queue, Object obj) {
    Putter p = new Putter(ms, queue, obj);
    p.start();
    return p;
  }

  public void testEmpty() {
    FifoQueue q = new FifoQueue();
    assertTrue(q.isEmpty());
  }

  public void testEmptyGet() {
    FifoQueue q = new FifoQueue();
    Interrupter intr = null;
    try {
      intr = interruptMeIn(TIMEOUT_SHOULD);
      q.get(timer(10000));
      intr.cancel();
    } catch (InterruptedException e) {
    } finally {
      assertTrue("get() of empty q returned",
		 intr.did());
    }
  }

  public void testNotEmpty() {
    FifoQueue q = new FifoQueue();
    q.put(O1);
    Interrupter intr = null;
    try {
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      assertSame(O1, q.get(timer(10000)));
      intr.cancel();
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	fail("get timed out");
      }
    }
  }

  public void testPut() {
    FifoQueue q = new FifoQueue();
    Interrupter intr = null;
    try {
      Date start = new Date();
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      putIn(100, q, O1);
      assertSame(O1, q.get(timer(10000)));
      intr.cancel();
      if (TimerUtil.timeSince(start) < 80) {
	fail("get() returned before put()");
      }
      assertTrue(q.isEmpty());
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	fail("put() didn't cause get() to return");
      }
    }
  }

  public void testRemove() {
    FifoQueue q = new FifoQueue();
    q.put(O1);
    assertFalse(q.isEmpty());
    q.remove(O1);
    assertTrue(q.isEmpty());
  }

  public void testNoWaitEmpty() {
    FifoQueue q = new FifoQueue();
    Interrupter intr = null;
    try {
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      assertEquals(null, q.get(timer(0)));
      intr.cancel();
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	fail("get(0) of empty didn't return");
      }
    }
  }

  public void testTimedEmpty() {
    FifoQueue q = new FifoQueue();
    Interrupter intr = null;
    try {
      Date start = new Date();
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      assertEquals(null, q.get(timer(100)));
      long delay = TimerUtil.timeSince(start);
      if (delay < 80) {
	fail("get(100) returned early in " + delay);
      }
      intr.cancel();
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	fail("get(100) of empty failed to timeout");
      }
    }
  }

  public void testPeek() {
    FifoQueue q = new FifoQueue();
    assertEquals(null, q.peek());
    q.put("b");
    assertEquals("b", q.peek());
    q.put("a");
    q.put("d");
    assertEquals("b", q.peek());
    Interrupter intr = null;
    try {
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      assertEquals("b", q.get(timer(100)));
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	fail("get(100) of empty failed to timeout");
      }
    }
    assertEquals("a", q.peek());
  }

  public void testGet() {
    FifoQueue q = new FifoQueue();
    Interrupter intr = null;
    try {
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      List l = new LinkedList();
      String ord[] = {"a", "b", "c", "d"};
      q.put("a");
      q.put("b");
      q.put("c");
      q.put("d");
      assertEquals(4, q.size());
      while (!q.isEmpty()) {
	l.add(q.get(timer(0)));
      }
      intr.cancel();
      assertIsomorphic(ord, l);
      assertTrue(q.isEmpty());
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	fail("get(0) of full timed out");
      }
    }
  }

  public void testThrowsInterruptedException() {
    FifoQueue q = new FifoQueue();
    try {
      interruptMeIn(100);
      Date start = new Date();
      Object obj = q.get(timer(TIMEOUT_SHOULDNT));
      long delay = TimerUtil.timeSince(start);
      fail("get(" + TIMEOUT_SHOULDNT +
	   ") of empty interrupted queue returned "+ obj + " in " + delay);
    } catch (InterruptedException e) {
    }
  }

  private Deadline timer(int msec) {
    return Deadline.in(msec);
  }
}
