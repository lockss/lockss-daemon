/*
 * $Id: TestTimerQueue.java,v 1.3 2002-12-15 00:13:16 tal Exp $
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
 * Test class for org.lockss.util.TimerQueue
 */

public class TestTimerQueue extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.util.TimerQueue.class
  };
//   protected static Logger log = Logger.getLogger("TestTimer");

  String O1 = "foo";
  Integer O2 = new Integer(42);

  public TestTimerQueue(String msg) {
    super(msg);
  }


  public void setUp() {
    TimeBase.setSimulated();
  }

  public void tearDown() {
    TimeBase.setReal();
  }

  public void testQueue() {
    final SimpleQueue.Fifo q = new SimpleQueue.Fifo();
    TimerQueue.Callback cb = new TimerQueue.Callback() {
	public void timerExpired(Object cookie) {
	  q.put(cookie);
	}};
    TimerQueue.schedule(Deadline.in(500), cb, "foo");
    assertTrue(q.isEmpty());
    TimerQueue.schedule(Deadline.in(300), cb, "bar");
    assertTrue(q.isEmpty());
    TimeBase.step(501);
    assertEquals("bar", q.get(500));
    assertEquals("foo", q.get(500));
  }

  public void testExpire() {
    final SimpleQueue.Fifo q = new SimpleQueue.Fifo();
    Deadline d1 = Deadline.in(500);
    TimerQueue.Callback cb = new TimerQueue.Callback() {
	public void timerExpired(Object cookie) {
	  q.put(cookie);
	}};
    TimerQueue.schedule(d1, cb, "foo");
    TimeBase.step(100);
    assertEquals(null, q.get(20));
    TimerQueue.schedule(Deadline.in(300), cb, "bar");
    assertEquals(null, q.get(20));
    d1.expire();
    assertEquals("foo", q.get(500));
    TimeBase.step(501);
    assertEquals("bar", q.get(500));
  }
}
