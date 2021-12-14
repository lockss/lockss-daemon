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

package org.lockss.test;

import java.util.*;
import java.io.*;
import junit.framework.TestCase;
import org.lockss.daemon.*;
import org.lockss.test.*;


/**
 * Test class for org.lockss.test.TestSimpleQueue
 */

public class TestSimpleQueue extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.test.SimpleQueue.class
  };

  String O1 = "foo";
  Integer O2 = new Integer(42);

  public TestSimpleQueue(String msg) {
    super(msg);
  }

  /** Putter puts something onto a queue in a while */
  class Putter extends DoLater {
    SimpleQueue.Fifo queue;
    Object obj1;
    Object obj2;

    Putter(long waitMs, SimpleQueue.Fifo queue, Object obj) {
      this(waitMs, queue, obj, null);
    }

    Putter(long waitMs, SimpleQueue.Fifo queue, Object obj1, Object obj2) {
      super(waitMs);
      this.queue = queue;
      this.obj1 = obj1;
      this.obj2 = obj2;
    }

    protected void doit() {
      queue.put(obj1);
      if (obj2 != null) {
	queue.put(obj2);
      }
    }
  }

  /** Put something onto a queue in a while */
  private Putter putIn(long ms, SimpleQueue.Fifo queue, Object obj) {
    Putter p = new Putter(ms, queue, obj);
    p.start();
    return p;
  }

  /** Put two things onto a queue in a while */
  private Putter putIn(long ms, SimpleQueue.Fifo queue,
		       Object obj1, Object obj2) {
    Putter p = new Putter(ms, queue, obj1, obj2);
    p.start();
    return p;
  }

  public void testEmpty() {
    SimpleQueue.Fifo fifo = new SimpleQueue.Fifo();
    assertTrue(fifo.isEmpty());
    Interrupter intr = null;
    try {
      intr = interruptMeIn(TIMEOUT_SHOULD);
      if (fifo.get() != null) {
	fail("get() of empty fifo returned true");
      }
      intr.cancel();
    } finally {
      if (!intr.did()) {
	fail("get() of empty returned");
      }
    }
  }

  public void testNotEmpty() {
    SimpleQueue.Fifo fifo = new SimpleQueue.Fifo();
    fifo.put(O1);
    assertFalse(fifo.isEmpty());
    Interrupter intr = null;
    try {
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      assertSame(O1, fifo.get());
      intr.cancel();
    } finally {
      if (intr.did()) {
	fail("timed out");
      }
    }
  }

  public void testPut() {
    SimpleQueue.Fifo fifo = new SimpleQueue.Fifo();
    Interrupter intr = null;
    try {
      Date start = new Date();
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      putIn(200, fifo, O1);
      assertSame(O1, fifo.get());
      if (TimerUtil.timeSince(start) < 200) {
	fail("get() returned before put()");
      }
      intr.cancel();
    } finally {
      if (intr.did()) {
	fail("put() didn't cause get() to return");
      }
    }
  }

  public void testNoWaitEmpty() {
    SimpleQueue.Fifo fifo = new SimpleQueue.Fifo();
    Interrupter intr = null;
    try {
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      assertEquals(null, fifo.get(0));
      intr.cancel();
    } finally {
      if (intr.did()) {
	fail("get(0) of empty didn't return");
      }
    }
  }

  public void testTimedEmpty() {
    SimpleQueue.Fifo fifo = new SimpleQueue.Fifo();
    Interrupter intr = null;
    try {
      Date start = new Date();
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      assertEquals(null, fifo.get(100));
      long delay = TimerUtil.timeSince(start);
      if (delay < 80) {
	fail("get(100) returned early in " + delay);
      }
      intr.cancel();
    } finally {
      if (intr.did()) {
	fail("get(100) of empty failed to timeout");
      }
    }
  }

  public void testQueue() {
    SimpleQueue.Fifo fifo = new SimpleQueue.Fifo();
    Interrupter intr = null;
    try {
      putIn(200, fifo, O1, O2);
      Date start = new Date();
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      assertSame(O1, fifo.get());
      assertSame(O2, fifo.get());
      long delay = TimerUtil.timeSince(start);
      if (delay < 200) {
	fail("get() returned early in " + delay);
      }
      assertEquals(null, fifo.get(20));
      intr.cancel();
    } finally {
      if (intr.did()) {
	fail("get(20) of empty failed to timeout");
      }
    }
  }
}
