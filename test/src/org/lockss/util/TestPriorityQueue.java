/*
 * $Id: TestPriorityQueue.java,v 1.3 2002-11-20 19:43:12 tal Exp $
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
import org.lockss.util.*;
import org.lockss.test.*;


/**
 * Test class for org.lockss.util.PriorityQueue
 */

public class TestPriorityQueue extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.util.PriorityQueue.class
  };

  String O1 = "foo";
  Integer O2 = new Integer(42);

  public TestPriorityQueue(String msg) {
    super(msg);
  }

  /** Putter puts something onto a queue in a while */
  class Putter extends DoLater {
    PriorityQueue queue;
    Object obj;

    Putter(long waitMs, PriorityQueue queue, Object obj) {
      super(waitMs);
      this.queue = queue;
      this.obj = obj;
    }

    protected void doit() {
      queue.put(obj);
    }
  }

  /** Put something onto a queue in a while */
  private Putter putIn(long ms, PriorityQueue queue, Object obj) {
    Putter p = new Putter(ms, queue, obj);
    p.start();
    return p;
  }

  public void testEmpty() {
    PriorityQueue q = new PriorityQueue();
    assertTrue(q.isEmpty());
  }

  public void testEmptyGet() {
    PriorityQueue q = new PriorityQueue();
    Interrupter intr = null;
    try {
      intr = interruptMeIn(500);
      q.get(timer(10000));
      intr.cancel();
    } catch (InterruptedException e) {
    } finally {
      if (!intr.did()) {
	fail("get() of empty q returned");
      }
    }
  }

  public void testNotEmpty() {
    PriorityQueue q = new PriorityQueue();
    q.put(O1);
    Interrupter intr = null;
    try {
      intr = interruptMeIn(1000);
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
    PriorityQueue q = new PriorityQueue();
    Interrupter intr = null;
    try {
      Date start = new Date();
      intr = interruptMeIn(2000);
      putIn(200, q, O1);
      assertSame(O1, q.get(timer(10000)));
      intr.cancel();
      if (TimerUtil.timeSince(start) < 200) {
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
    PriorityQueue q = new PriorityQueue();
    q.put(O1);
    assertTrue(!q.isEmpty());
    q.remove(O1);
    assertTrue(q.isEmpty());
  }

  public void testNoWaitEmpty() {
    PriorityQueue q = new PriorityQueue();
    Interrupter intr = null;
    try {
      intr = interruptMeIn(1000);
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
    PriorityQueue q = new PriorityQueue();
    Interrupter intr = null;
    try {
      Date start = new Date();
      intr = interruptMeIn(2000);
      assertEquals(null, q.get(timer(300)));
      long delay = TimerUtil.timeSince(start);
      if (delay < 300) {
	fail("get(300) returned early");
      }
      if (delay > 1000) {
	fail("get(300) returned late");
      }
      intr.cancel();
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	fail("get(300) of empty failed to timeout");
      }
    }
  }

  public void testPeek() {
    PriorityQueue q = new PriorityQueue();
    assertEquals(null, q.peek());
    q.put("b");
    assertEquals("b", q.peek());
    q.put("a");
    assertEquals("a", q.peek());
    q.put("d");
    assertEquals("a", q.peek());
  }

  public void testOrder1() {
    PriorityQueue q = new PriorityQueue();
    Interrupter intr = null;
    try {
      intr = interruptMeIn(2000);
      List l = new LinkedList();
      String ord[] = {"a", "b", "c", "d"};
      q.put("b");
      q.put("a");
      q.put("d");
      q.put("c");
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

  public void testOrder2() {
    PriorityQueue q = new PriorityQueue(new Comparator() {
	public int compare(Object o1, Object o2) {
	  String s1 = (String)o1;
	  String s2 = (String)o2;
	  return s2.compareTo(s1);
	}
      });
    Interrupter intr = null;
    try {
      intr = interruptMeIn(2000);
      List l = new LinkedList();
      String ord[] = {"d", "c", "b", "a"};
      q.put("b");
      q.put("a");
      q.put("d");
      q.put("c");
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

  private Deadline timer(int msec) {
    return Deadline.in(msec);
  }
}
