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

package org.lockss.scheduler;

import java.util.*;
import org.lockss.util.*;
import org.lockss.test.*;


/**
 * Test class for <code>org.lockss.scheduler.Schedule</code>
 */

public class TestSchedule extends LockssTestCase {
  static Logger log = Logger.getLogger("TestBTScheduler");

  public void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated();
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    super.tearDown();
  }

  public void testGetOverrunTasks() {
    List tasks = ListUtil.list(null, null);
    Schedule sched = new Schedule(null);
    assertNull(sched.getOverrunTasks());
    sched = new Schedule(null, null);
    assertNull(sched.getOverrunTasks());
    sched = new Schedule(null, tasks);
    assertEquals(tasks, sched.getOverrunTasks());
  }

  public void testGetEvents() {
    List events = ListUtil.list(null, null);
    Schedule sched = new Schedule(events);
    assertEquals(events, sched.getEvents());
  }

  public void testGetFirstEvent() {
    Schedule sched = new Schedule(Collections.EMPTY_LIST);
    assertNull(sched.getFirstEvent());
  }

  public void testRemoveEvent() {
    Schedule.Event e1 =
      new Schedule.BackgroundEvent(new MyMockBackgroundTask(), Deadline.at(3),
				   Schedule.EventType.FINISH);

    Schedule.Event e2 =
      new Schedule.Chunk(new MyMockStepTask(), Deadline.at(5),
			 Deadline.at(10), 8);

    // test depends on this
    assertNotEquals(e1, e2);
    List events = ListUtil.list(e1, e2);
    Schedule sched = new Schedule(events);
    assertEquals(events, sched.getEvents());
    assertTrue(sched.removeEvent(e2));
    assertEquals(ListUtil.list(e1), sched.getEvents());
    assertFalse(sched.removeEvent(e2));
    assertTrue(sched.removeEvent(e1));
    assertEmpty(sched.getEvents());
  }

  public void testRemoveFirstEvent() {
    Schedule.Event e1 =
      new Schedule.BackgroundEvent(new MyMockBackgroundTask(), Deadline.at(3),
				   Schedule.EventType.FINISH);

    Schedule.Event e2 =
      new Schedule.Chunk(new MyMockStepTask(), Deadline.at(5),
			 Deadline.at(10), 8);

    // test depends on this
    assertNotEquals(e1, e2);
    List events = ListUtil.list(e1, e2);
    Schedule sched = new Schedule(events);
    assertEquals(events, sched.getEvents());
    assertFalse(sched.removeFirstEvent(e2));
    assertEquals(events, sched.getEvents());
    assertTrue(sched.removeFirstEvent(e1));
    assertEquals(ListUtil.list(e2), sched.getEvents());
    assertTrue(sched.removeFirstEvent(e2));
    assertEmpty(sched.getEvents());
  }

  public void testBackgroundEvent() {
    Deadline d1 = Deadline.at(5);
    BackgroundTask task = new MyMockBackgroundTask();
    Schedule.BackgroundEvent event =
      new Schedule.BackgroundEvent(task, d1, Schedule.EventType.FINISH);
    assertTrue(event.isBackgroundEvent());
    assertEquals(d1, event.getStart());
    assertEquals(Schedule.EventType.FINISH, event.getType());
    assertEquals(task, event.getTask());
    event.toString();
  }

  public void testChunk() {
    Deadline d1 = Deadline.at(5);
    Deadline d2 = Deadline.at(10);
    StepTask task = new MyMockStepTask();
    Schedule.Chunk chunk = new Schedule.Chunk(task, d1, d2, 8);
    assertEquals(d1, chunk.getStart());
    assertEquals(d2, chunk.getFinish());
    assertEquals(new Interval(d1, d2), chunk.getInterval());
    assertEquals(8, chunk.getRunTime());
    assertFalse(chunk.isBackgroundEvent());
    chunk.toString();
  }

  public void testChunkEquals() {
    StepTask task = new MyMockStepTask();
    Schedule.Chunk chunk1 =
      new Schedule.Chunk(task, Deadline.at(5), Deadline.at(10), 8);
    Schedule.Chunk chunk2 =
      new Schedule.Chunk(task, Deadline.at(5), Deadline.at(10), 8);
    assertEquals(chunk1, chunk2);
    assertNotEquals(chunk1,
		    new Schedule.Chunk(new MyMockStepTask(),
				       Deadline.at(5), Deadline.at(10), 8));
    assertNotEquals(chunk1,
		    new Schedule.Chunk(task,
				       Deadline.at(6), Deadline.at(10), 8));
    assertNotEquals(chunk1,
		    new Schedule.Chunk(task,
				       Deadline.at(5), Deadline.at(11), 8));
    assertNotEquals(chunk1,
		    new Schedule.Chunk(task,
				       Deadline.at(5), Deadline.at(10), 7));
  }

  public void testBackgroundEventEquals() {
    BackgroundTask task = new MyMockBackgroundTask();
    Schedule.BackgroundEvent event1 =
      new Schedule.BackgroundEvent(task, Deadline.at(5), Schedule.EventType.FINISH);
    Schedule.BackgroundEvent event2 =
      new Schedule.BackgroundEvent(task, Deadline.at(5), Schedule.EventType.FINISH);
    assertEquals(event1, event2);
    assertNotEquals(event1,
		    new Schedule.BackgroundEvent(new MyMockBackgroundTask(),
						 Deadline.at(5),
						 Schedule.EventType.FINISH));
    assertNotEquals(event1,
		    new Schedule.BackgroundEvent(task,
						 Deadline.at(6),
						 Schedule.EventType.FINISH));
  }

  private static class MyMockStepTask extends StepTask {
    MyMockStepTask() {
      super(Deadline.EXPIRED, Deadline.MAX, 0, null, null);
    }
    public int step(int n) {
      return 0;
    }
  }

  private static class MyMockBackgroundTask extends BackgroundTask {
    MyMockBackgroundTask() {
      super(Deadline.EXPIRED, Deadline.MAX, .5, null);
    }
  }
}
