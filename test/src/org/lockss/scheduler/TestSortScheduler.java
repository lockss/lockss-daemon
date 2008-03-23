/*
 * $Id: TestSortScheduler.java,v 1.9 2008-03-23 00:53:52 tlipkis Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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
 * Test class for <code>org.lockss.scheduler.SortScheduler</code>
 */

public class TestSortScheduler extends LockssTestCase {
  static Logger log = Logger.getLogger("TestScheduler");
  static final List EMPTY_LIST = Collections.EMPTY_LIST;

  public void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated();
    ConfigurationUtil.setFromArgs(SortScheduler.PARAM_OVERHEAD_LOAD, "0");
    taskSet1();
    taskSet2();
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    super.tearDown();
  }

  public static void assertEquals(BackgroundTask expected,
				  BackgroundTask actual) {
    assertEquals(null, expected, actual);
  }

  public static void assertEquals(String message,
				  BackgroundTask expected,
				  BackgroundTask actual) {
    if (expected.getStart().equals(actual.getStart()) &&
	expected.getFinish().equals(actual.getFinish()) &&
	expected.getLoadFactor() == actual.getLoadFactor()) {
      return;
    }
    failNotEquals(message, expected, actual);
  }


  static StepTask taskBefore(long deadline, int duration) {
    return new MyMockStepTask(Deadline.at(0), Deadline.at(deadline),
			   duration, null, null);
  }

  static StepTask taskBetween(long minStart,
				 long deadlineAt,
				 int duration) {
    return new MyMockStepTask(Deadline.at(minStart),
			   Deadline.at(deadlineAt),
			   duration, null, null);
  }

  static StepTask taskBetween(String cookie,
				 long minStart,
				 long deadlineAt,
				 int duration) {
    return new MyMockStepTask(Deadline.at(minStart), Deadline.at(deadlineAt),
			   duration, null, cookie);
  }

  static BackgroundTask bTask(long start,
			      long stop,
			      double loadFactor) {
    return new BackgroundTask(Deadline.at(start),
			      Deadline.at(stop),
			      loadFactor, null);
  }

  /*
	100		200		300		400		500
	[----- t1 ------)
			[----- t2 ------)
					[----- t3 ------)
					[----- t3a------)
	[--------------------- t4 ----------------------)
		[------------- t5---------------)
								[-- t6 --)
		[--------- t7 ----------)
  */

  StepTask t1;
  StepTask t2;
  StepTask t3;
  StepTask t3a;
  StepTask t4;
  StepTask t5;
  StepTask t6;
  StepTask t7;

  void taskSet1() {
    t1 = taskBetween(100, 200, 50);
    t2 = taskBetween(200, 300, 50);
    t3 = taskBetween(300, 400, 50);
    t3a = taskBetween(300, 400, 50);
    t4 = taskBetween(100, 400, 50);
    t5 = taskBetween(150, 350, 50);
    t6 = taskBetween(450, 500, 50);
    t7 = taskBetween(150, 300, 50);
  }

  public void testIntervals() {
    SortScheduler sched =
      new SortScheduler(ListUtil.list(t1, t2, t2, t5, t6));
    sched.initIntervals();
    SortScheduler.SchedInterval intervals[] = sched.intervals;
    assertEquals(6, intervals.length);
    assertIntervalEquals(100, 150, intervals[0]);
    assertIntervalEquals(150, 200, intervals[1]);
    assertIntervalEquals(200, 300, intervals[2]);
    assertIntervalEquals(300, 350, intervals[3]);
    assertIntervalEquals(350, 450, intervals[4]);
    assertIntervalEquals(450, 500, intervals[5]);
  }

  public void testIntervalsLater() {
    TimeBase.setSimulated(150);
    SortScheduler sched =
      new SortScheduler(ListUtil.list(t1, t2, t2, t5, t6));
    sched.initIntervals();
    SortScheduler.SchedInterval intervals[] = sched.intervals;
    assertEquals(5, intervals.length);
    assertIntervalEquals(150, 200, intervals[0]);
    assertIntervalEquals(200, 300, intervals[1]);
    assertIntervalEquals(300, 350, intervals[2]);
    assertIntervalEquals(350, 450, intervals[3]);
    assertIntervalEquals(450, 500, intervals[4]);
  }

  public void testIntervalTaskLists() {
    SortScheduler sched =
      new SortScheduler(ListUtil.list(t1, t2, t3, t4, t5, t6));
    sched.initIntervals();
    SortScheduler.SchedInterval intervals[] = sched.intervals;
    assertEquals(7, intervals.length);

    assertIntervalEquals(100, 150, intervals[0]);
    assertEquals(tdSetFromT(SetUtil.set(t4, t1)),
		 theSet(intervals[0].competingTaskList));
    assertNull(intervals[0].endingTaskList);

    assertIntervalEquals(150, 200, intervals[1]);
    assertEquals(tdSetFromT(SetUtil.set(t1, t4, t5)),
		 theSet(intervals[1].competingTaskList));
    assertEquals(tdSetFromT(SetUtil.set(t1)),
		 theSet(intervals[1].endingTaskList));

    assertIntervalEquals(200, 300, intervals[2]);
    assertEquals(tdSetFromT(SetUtil.set(t2, t4, t5)),
		 theSet(intervals[2].competingTaskList));
    assertEquals(tdSetFromT(SetUtil.set(t2)),
		 theSet(intervals[2].endingTaskList));

    assertIntervalEquals(300, 350, intervals[3]);
    assertEquals(tdSetFromT(SetUtil.set(t3, t4, t5)),
		 theSet(intervals[3].competingTaskList));
    assertEquals(tdSetFromT(SetUtil.set(t5)),
		 theSet(intervals[3].endingTaskList));

    assertIntervalEquals(350, 400, intervals[4]);
    assertEquals(tdSetFromT(SetUtil.set(t3, t4)),
		 theSet(intervals[4].competingTaskList));
    assertEquals(tdSetFromT(SetUtil.set(t3, t4)),
		 theSet(intervals[4].endingTaskList));

    assertIntervalEquals(400, 450, intervals[5]);
    assertEquals(EMPTY_LIST, intervals[5].competingTaskList);
    assertNull(intervals[5].endingTaskList);

    assertIntervalEquals(450, 500, intervals[6]);
    assertEquals(tdSetFromT(SetUtil.set(t6)),
		 theSet(intervals[6].competingTaskList));
    assertEquals(tdSetFromT(SetUtil.set(t6)),
		 theSet(intervals[6].endingTaskList));
  }

  // ensure scheduling empty task list succeeds and creates a schedule
  public void testEmptySchedule() {
    SortScheduler scheduler = new SortScheduler();
    assertTrue(scheduler.createSchedule(Collections.EMPTY_LIST));
    assertEmpty(scheduler.getTasks());
    Schedule sched = scheduler.getSchedule();
    assertNotNull(sched);
    assertEmpty(sched.getEvents());
  }

  class MyMockBTS1 extends SortScheduler {
    int both[];
    int ix;

    MyMockBTS1(Collection tasks) {
      super(tasks);
      both = new int[tasks.size() * 2];
      ix = 0;
    }
    boolean scheduleIntervalRange(int first, int last) {
      both[ix++] = first;
      both[ix++] = last;
      return true;
    }
    int[] getBoth() {
      int res[] = new int[ix];
      System.arraycopy(both, 0, res, 0, ix);
      return res;
    }
  }

  public void testScheduleAll() {
    MyMockBTS1 sched = new MyMockBTS1(ListUtil.list(t1, t2, t6, t7));
    sched.initIntervals();
    SortScheduler.SchedInterval intervals[] = sched.intervals;
    assertEquals(5, intervals.length);
    assertIntervalEquals(100, 150, intervals[0]);
    assertEquals(tdSetFromT(SetUtil.set(t1)),
		 theSet(intervals[0].competingTaskList));
    assertIntervalEquals(150, 200, intervals[1]);
    assertEquals(tdSetFromT(SetUtil.set(t1, t7)),
		 theSet(intervals[1].competingTaskList));
    assertIntervalEquals(200, 300, intervals[2]);
    assertEquals(tdSetFromT(SetUtil.set(t2, t7)),
		 theSet(intervals[2].competingTaskList));
    assertIntervalEquals(300, 450, intervals[3]);
    assertEquals(Collections.EMPTY_SET,
		 theSet(intervals[3].competingTaskList));
    assertIntervalEquals(450, 500, intervals[4]);
    assertEquals(tdSetFromT(SetUtil.set(t6)),
		 theSet(intervals[4].competingTaskList));

    assertTrue("Failed to schedule", sched.scheduleAll());
    int exp[] = {0, 2, 4, 4};
    assertEquals(exp, sched.getBoth());
  }

  public void testSchedule() {
    SortScheduler sched = new SortScheduler(ListUtil.list(t1, t2, t6, t4));
    assertTrue(sched.createSchedule());
    sched.getSchedule();
  }

  public void testOneFits() {
    SortScheduler sched =
      new SortScheduler(ListUtil.list(taskBetween(100, 200, 100)));
    assertTrue(sched.createSchedule());
  }

  public void testOneTooLong() {
    SortScheduler sched =
      new SortScheduler(ListUtil.list(taskBetween(100, 200, 101)));
    assertFalse(sched.createSchedule());
  }

  public void testTwoFit() {
    SortScheduler sched =
      new SortScheduler(ListUtil.list(taskBetween(100, 200, 100),
					     taskBetween(200, 250, 50)));
    assertTrue(sched.createSchedule());
  }

  public void testCoincide() {
    SortScheduler sched =
      new SortScheduler(ListUtil.list(taskBetween(100, 200, 70),
					     taskBetween(100, 200, 30)));
    assertTrue(sched.createSchedule());
  }

  public void testSimpleOverlap() {
    SortScheduler sched =
      new SortScheduler(ListUtil.list(taskBetween(100, 200, 70),
					     taskBetween(110, 140, 30)));
    assertTrue(sched.createSchedule());
  }

  public void testSchedulePacked() {
    TimeBase.setSimulated(150);
    SortScheduler sched =
      new SortScheduler(ListUtil.list(t1, t2, t3, t4, t5, t6));
    assertTrue(sched.createSchedule());
    sched.getSchedule();
  }


  /*
	100		200		300		400		500
 80	[------------------------- h1 ---------------------------)
 35		[------------- h2---------------)
 35		[--------- h3 ----------)
 50					[----- h4 ------)
 50					[----- h5 ------)
 67	[----- h6 ------)
 33	    [- h7 --)
  */

  StepTask h1;
  StepTask h2;
  StepTask h3;
  StepTask h4;
  StepTask h5;
  StepTask h6;
  StepTask h6a;
  StepTask h7;

  BackgroundTask b1;
  BackgroundTask b2;
  BackgroundTask b3;
  StepTask h4a;
  StepTask h4b;
  StepTask h5a;

  void taskSet2() {
    h1 = taskBetween("h1", 100, 450, 80);
    h2 = taskBetween("h2", 150, 350, 35);
    h3 = taskBetween("h3", 150, 300, 35);
    h4 = taskBetween("h4", 300, 400, 50);
    h5 = taskBetween("h5", 300, 400, 50);
    h6 = taskBetween("h6", 100, 200, 67);
    h6a = taskBetween("h6", 100, 200, 62);
    h7 = taskBetween("h7", 133, 166, 33);

    b1 = bTask(300, 400, .1);
    b2 = bTask(300, 400, .2);
    b3 = bTask(300, 400, .8);
    h4a = taskBetween("h4a", 300, 400, (int)(50 * .9));
    h4b = taskBetween("h4b", 300, 400, (int)(50 * .8));
    h5a = taskBetween("h5a", 300, 400, (int)(50 * .2));
  }

  Schedule.Chunk chunk(StepTask task, long start,
		       long finish, int run) {
    return chunk(task, start, finish, run, true);
  }

  Schedule.Chunk chunk(StepTask task, long start,
		       long finish, int run, boolean taskEnd) {

    Schedule.Chunk chunk =
      new Schedule.Chunk(task, Deadline.at(start), Deadline.at(finish), run);
    if (taskEnd) {
      chunk.setTaskEnd();
    }
    return chunk;
  }

  Schedule.BackgroundEvent bevent(BackgroundTask task, int start,
				  Schedule.EventType event) {
    return new Schedule.BackgroundEvent(task, Deadline.at(start), event);
  }

  // ensure background event whose start time has passed gets events generated
  public void testPastBackStart() {
    BackgroundTask b1 = bTask(100, 300, .1);
    TimeBase.setSimulated(103);
    SortScheduler sched =
      new SortScheduler(ListUtil.list(b1));
    assertTrue(sched.createSchedule());
    Schedule s = sched.getSchedule();
    List sh1 = ListUtil.list(bevent(b1, 103, Schedule.EventType.START),
			     bevent(b1, 300, Schedule.EventType.FINISH));
    assertEquals(sh1, s.getEvents());
  }

  public void testPastBackEnd() {
    BackgroundTask b1 = bTask(100, 300, .1);
    TimeBase.setSimulated(500);
    SortScheduler sched =
      new SortScheduler(ListUtil.list(b1));
    assertFalse(sched.createSchedule());
  }

  public void testBackHintEmptySchedule() {
    BackgroundTask b1 = bTask(100, 300, .9);
    SortScheduler sched = new SortScheduler(Collections.EMPTY_LIST);
    assertSame(b1, sched.scheduleHint(b1));
  }

  public void testBackHintBefore() {
    BackgroundTask b1 = bTask(200, 300, .9);
    SortScheduler sched = new SortScheduler(ListUtil.list(b1));
    assertTrue(sched.createSchedule());

    BackgroundTask b = bTask(100, 200, .9);
    assertSame(b, sched.scheduleHint(b));
    assertEquals(bTask(100, 200, .9), sched.scheduleHint(b));
  }

  public void testBackHintAfter() {
    BackgroundTask b1 = bTask(200, 300, .9);
    BackgroundTask b2 = bTask(400, 600, .9);
    SortScheduler sched = new SortScheduler(ListUtil.list(b1, b2));
    assertTrue(sched.createSchedule());

    BackgroundTask b = bTask(600, 900, .9);
    assertSame(b, sched.scheduleHint(b));
    assertEquals(bTask(600, 900, .9), sched.scheduleHint(b));
  }

  public void testBackHintDuring() {
    BackgroundTask b1 = bTask(200, 300, .4);
    BackgroundTask b2 = bTask(400, 600, .5);
    SortScheduler sched = new SortScheduler(ListUtil.list(b1, b2));
    assertTrue(sched.createSchedule());

    BackgroundTask b = bTask(250, 900, .5);
    assertSame(b, sched.scheduleHint(b));
    assertEquals(bTask(250, 900, .5), sched.scheduleHint(b));
  }

  public void testBackHintBetween() {
    BackgroundTask b1 = bTask(200, 300, .4);
    BackgroundTask b2 = bTask(400, 600, .5);
    SortScheduler sched = new SortScheduler(ListUtil.list(b1, b2));
    assertTrue(sched.createSchedule());

    BackgroundTask b = bTask(250, 350, .5);
    assertSame(b, sched.scheduleHint(b));
    assertEquals(bTask(250, 350, .5), sched.scheduleHint(b));
  }

  public void testBackHintDelayed() {
    BackgroundTask b1 = bTask(200, 300, .4);
    BackgroundTask b2 = bTask(400, 600, .5);
    SortScheduler sched = new SortScheduler(ListUtil.list(b1, b2));
    assertTrue(sched.createSchedule());

    BackgroundTask b = bTask(100, 201, .7);
    assertSame(b, sched.scheduleHint(b));
    assertEquals(bTask(600, 701, .7), sched.scheduleHint(b));
  }

  public void testBackHintDelayDueToMaxLoad() {
    ConfigurationUtil.addFromArgs(SortScheduler.PARAM_MAX_BACKGROUND_LOAD,
				  "25");
    BackgroundTask b1 = bTask(100, 200, .5);
    BackgroundTask b2 = bTask(200, 300, .5);
    List tasklist = ListUtil.list(b1, b2);
    SortScheduler sched = new SortScheduler();
    assertTrue(sched.createSchedule(tasklist));

    BackgroundTask b = bTask(100, 201, .7);
    assertSame(b, sched.scheduleHint(b));
    assertEquals(bTask(400, 501, .7), sched.scheduleHint(b));

    // do it again to ensure scheduler is reusable
    assertTrue(sched.createSchedule(tasklist));
    b = bTask(100, 201, .7);
    assertSame(b, sched.scheduleHint(b));
    assertEquals(bTask(400, 501, .7), sched.scheduleHint(b));
  }

  public void testBackHintDelayDueToMaxLoadBetween() {
    ConfigurationUtil.addFromArgs(SortScheduler.PARAM_MAX_BACKGROUND_LOAD,
				  "25");
    BackgroundTask b1 = bTask(100, 200, .5);
    BackgroundTask b2 = bTask(200, 300, .5);
    BackgroundTask b3 = bTask(5000, 5100, .5);
    SortScheduler sched = new SortScheduler(ListUtil.list(b1, b2, b3));
    assertTrue(sched.createSchedule());

    BackgroundTask b = bTask(100, 201, .7);
    assertSame(b, sched.scheduleHint(b));
    assertEquals(bTask(400, 501, .7), sched.scheduleHint(b));
  }

  public void testBackHintUnaffectedByMaxLoad() {
    ConfigurationUtil.addFromArgs(SortScheduler.PARAM_MAX_BACKGROUND_LOAD,
				  "50");
    BackgroundTask b1 = bTask(100, 200, .5);
    BackgroundTask b2 = bTask(200, 300, .5);
    SortScheduler sched = new SortScheduler(ListUtil.list(b1, b2));
    assertTrue(sched.createSchedule());

    BackgroundTask b = bTask(100, 201, .7);
    assertSame(b, sched.scheduleHint(b));
    assertEquals(bTask(300, 401, .7), sched.scheduleHint(b));
  }

  public void testHard1() {
    SortScheduler sched =
      new SortScheduler(ListUtil.list(h6, h7));
    assertTrue(sched.createSchedule());

    List sh1 = ListUtil.list(chunk(h6, 100, 133, 33, false),
			     chunk(h7, 133, 166, 33),
			     chunk(h6, 166, 200, 34));

    Schedule s = sched.getSchedule();
    assertEquals(sh1, s.getEvents());
    assertEmpty(s.getOverrunTasks());
  }

  public void testUnschedTasks() {
    BackgroundTask b1 = bTask(100, 300, .1);
    StepTask ov = taskBetween("ov", 100, 300, 20);
    ov.timeUsed = 40;
    assertTrue(ov.hasOverrun());
    TimeBase.setSimulated(103);
    SortScheduler sched =
      new SortScheduler(ListUtil.list(h1, b1, ov));
    assertTrue(sched.createSchedule());
    Schedule s = sched.getSchedule();
    assertEquals(SetUtil.set(ov), s.getOverrunTasks());

  }


  public void testTooLate() {
    TimeBase.setSimulated(200);
    SortScheduler sched =
      new SortScheduler(ListUtil.list(h3, h6, h7));
    assertFalse(sched.createSchedule());
  }

  public void testHard2() {
    List sh1 = ListUtil.list(
			     chunk(h6, 100, 133, 33, false),
			     chunk(h7, 133, 166, 33),
			     chunk(h6, 166, 200, 34),
			     chunk(h3, 200, 235, 35),
			     chunk(h2, 235, 270, 35),
			     chunk(h1, 270, 300, 30, false),
			     chunk(h4, 300, 350, 50),
			     chunk(h5, 350, 400, 50),
			     chunk(h1, 400, 450, 50)
			     );
    SortScheduler sched =
      new SortScheduler(ListUtil.list(h1, h2, h3, h4, h5, h6, h7));
    assertTrue(sched.createSchedule());
    assertEquals(sh1, sched.getSchedule().getEvents());
  }

  public void testHardLater() {
    TimeBase.setSimulated(105);
    SortScheduler sched0 =
      new SortScheduler(ListUtil.list(h1, h2, h3, h4, h5, h6, h7));
    assertFalse(sched0.createSchedule());

    SortScheduler sched =
      new SortScheduler(ListUtil.list(h1, h2, h3, h4, h5, h6a, h7));
    assertTrue(sched.createSchedule());

    List sh1 = ListUtil.list(
			     chunk(h6a, 105, 133, 28, false),
			     chunk(h7, 133, 166, 33),
			     chunk(h6a, 166, 200, 34),
			     chunk(h3, 200, 235, 35),
			     chunk(h2, 235, 270, 35),
			     chunk(h1, 270, 300, 30, false),
			     chunk(h4, 300, 350, 50),
			     chunk(h5, 350, 400, 50),
			     chunk(h1, 400, 450, 50)
			     );
    assertEquals(sh1, sched.getSchedule().getEvents());
  }

  public void testImpossible1() {
    SortScheduler sched =
      new SortScheduler(ListUtil.list(h1, h2, h3, h4, h5, h6, h7,
					     taskBetween(100, 450, 1)));
    assertFalse(sched.createSchedule());
  }

  // test background tasks

  // total loadfactor > 1
  public void testBImpossible1() {
    SortScheduler sched =
      new SortScheduler(ListUtil.list(b1, b2, b3));
    assertFalse(sched.createSchedule());
  }

  public void testAlreadyAccepted() {
    SortScheduler sched;
    StepTask h1 = taskBetween("h1", 100, 700, 500);
    StepTask h2 = taskBetween("h2", 500, 600, 100);
    StepTask h3 = taskBetween("h4", 100, 300, 100);
    StepTask h4 = taskBetween("h4", 100, 400, 100);
    sched = new SortScheduler(ListUtil.list(h1, h2, h3));
    assertFalse(sched.createSchedule());
    sched = new SortScheduler(ListUtil.list(h1, h2));
    assertTrue(sched.createSchedule());
    sched.acceptTasks();
    // h3 doesn't fit, ensure fails
    sched = new SortScheduler(ListUtil.list(h1, h2, h3));
    assertFalse(sched.createSchedule());
    // but succeeds if h3 had already been accepted
    h3.setAccepted(true);
    sched = new SortScheduler(ListUtil.list(h1, h2, h3));
    assertTrue(sched.createSchedule());
  }

  // full schedule plus background task
  public void testBImpossible2() {
    SortScheduler sched =
      new SortScheduler(ListUtil.list(h1, h2, h3, h4, h5, h6, h7, b1));
    assertFalse(sched.createSchedule());
  }

  public void testBHard2() {
    Schedule.Event exparr[] = {
      chunk(h6, 100, 133, 33, false),
      chunk(h7, 133, 166, 33),
      chunk(h6, 166, 200, 34),
      chunk(h3, 200, 235, 35),
      chunk(h2, 235, 270, 35),
      chunk(h1, 270, 300, 30, false),
      bevent(b1, 300, Schedule.EventType.START),
      chunk(h4b, 300, 344, 40),
      chunk(h5, 344, 400, 50),
      bevent(b1, 400, Schedule.EventType.FINISH),
      chunk(h1, 400, 450, 50)
    };
    List sh1 = ListUtil.fromArray(exparr);

    SortScheduler sched =
      new SortScheduler(ListUtil.list(h1, h2, h3, h4b, h5, h6, h7, b1));
    sched.initIntervals();
    assertTrue(sched.createSchedule());
    assertEquals(sh1, sched.getSchedule().getEvents());
  }

  public void testBImpossible3() {
    SortScheduler sched =
      new SortScheduler(ListUtil.list(h1, h2, h3, h4b, h5, h6, h7, b2));
    assertFalse(sched.createSchedule());
  }

  public void testEmpty() {
    SortScheduler sched = new SortScheduler(EMPTY_LIST);
    assertTrue(sched.createSchedule());
    assertEquals(EMPTY_LIST, sched.getSchedule().getEvents());
  }

  public void testNull() {
    try {
      SortScheduler sched = new SortScheduler(null);
      sched.createSchedule();
      fail("Scheduler with null task list should throw NullPointerException");
    } catch (NullPointerException e) {
      // expected
    }
  }
  public void testIllTask() {
    try {
      SortScheduler sched =
	new SortScheduler(ListUtil.list(taskBetween(450, 100, 10)));
      sched.createSchedule();
      fail("task with improper window should throw IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }


  private void assertIntervalEquals(long begin, long end,
				    SortScheduler.SchedInterval intrvl) {
    assertEquals("begin", begin, deadlineVal(intrvl.getLB()));
    assertEquals("end", end, deadlineVal(intrvl.getUB()));
  }

  long deadlineVal(Object x) {
    return ((Deadline)x).getExpirationTime();
  }

  protected Set theSet(Collection x) {
    return SetUtil.theSet(x);
  }

  Set tdSetFromT(Set taskSet) {
    Set res = new HashSet();
    for (Iterator iter = taskSet.iterator(); iter.hasNext(); ) {
      SchedulableTask t = (SchedulableTask)iter.next();
      res.add(new SortScheduler.TaskData(t));
    }
    return res;
  }

  static class MyMockStepTask extends StepTask {
    public MyMockStepTask(Deadline earliestStart,
			Deadline latestFinish,
			long estimatedDuration,
			TaskCallback callback,
			Object cookie) {
      super(earliestStart, latestFinish, estimatedDuration, callback, cookie);
    }
    public int step(int n) {
      return 0;
    }
  }
}
