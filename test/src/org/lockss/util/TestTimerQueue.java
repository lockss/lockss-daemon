/*
 * $Id: TestTimerQueue.java,v 1.10 2006-11-27 06:34:00 tlipkis Exp $
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


  public void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated();
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    super.tearDown();
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

  // ensure that exceptions thrown by a timer callback doesn't cause
  // notification thread to exit
  public void testThrows() {
    setErrorIfTimerThrows(false);
    final SimpleQueue.Fifo q = new SimpleQueue.Fifo();
    TimerQueue.Callback cb = new TimerQueue.Callback() {
	public void timerExpired(Object cookie) {
	  q.put(cookie);
	  throw new ExpectedRuntimeException("thrown from timer event");
	}};
    TimerQueue.schedule(Deadline.in(500), cb, "foo");
    assertTrue(q.isEmpty());
    TimerQueue.schedule(Deadline.in(300), cb, "bar");
    assertTrue(q.isEmpty());
    TimeBase.step(501);
    assertEquals("bar", q.get(500));
    // if notification thread exited, this one won't have happened
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

  public void testCancel() {
    final SimpleQueue.Fifo q = new SimpleQueue.Fifo();
    // a request that we will cancel, which fails if it gets run
    Deadline dl = Deadline.in(100);
    long dlexp = dl.getExpirationTime();
    TimerQueue.Request req =
      TimerQueue.schedule(dl,
			  new TimerQueue.Callback() {
			    public void timerExpired(Object cookie) {
			      fail("This timer request was cancelled, " +
				   "so should not have run.");
			    }},
			  "foo");
    assertTrue(q.isEmpty());
    // a second request to wait for, to (more or less) ensure the one we
    // cancelled would have run by then.
    TimerQueue.schedule(Deadline.in(200),
			new TimerQueue.Callback() {
			  public void timerExpired(Object cookie) {
			    q.put(cookie);
			  }},
			"bar");
    assertTrue(q.isEmpty());
    TimerQueue.cancel(req);
    TimeBase.step(201);
    assertEquals("bar", q.get(500));
    // verify that the deadline didn't change
    assertEquals(dlexp, dl.getExpirationTime());
  }

  public void testChangeEarlier() {
    final SimpleQueue.Fifo q = new SimpleQueue.Fifo();
    Deadline d1 = Deadline.in(500);
    Deadline d2 = Deadline.in(300);
    TimerQueue.Callback cb = new TimerQueue.Callback() {
	public void timerExpired(Object cookie) {
	  q.put(cookie);
	}};
    TimerQueue.schedule(d1, cb, "dd1");
    TimerQueue.schedule(d2, cb, "dd2");
    assertEquals(null, q.get(20));
    d1.expireAt(200);
    assertEquals(null, q.get(20));
    TimeBase.step(201);
    assertEquals("dd1", q.get(500));
    TimeBase.step(100);
    assertEquals("dd2", q.get(500));
  }

  public void testChangeLater() {
    final SimpleQueue.Fifo q = new SimpleQueue.Fifo();
    Deadline d1 = Deadline.at(500);
    Deadline d2 = Deadline.at(300);
    TimerQueue.Callback cb = new TimerQueue.Callback() {
	public void timerExpired(Object cookie) {
	  q.put(cookie);
	}};
    TimerQueue.schedule(d1, cb, "dd1");
    TimerQueue.schedule(d2, cb, "dd2");
    assertEquals(null, q.get(20));
    d2.expireAt(1000);
    assertEquals(null, q.get(20));
    TimeBase.step(201);
    assertEquals(null, q.get(20));
    TimeBase.step(200);
    assertEquals(null, q.get(20));
    TimeBase.step(100);
    assertEquals("dd1", q.get(500));
    TimeBase.step(400);
    assertEquals(null, q.get(20));
    TimeBase.step(100);
    assertEquals("dd2", q.get(500));
  }

}
