/*
 * $Id: TestTaskRunner.java,v 1.16 2006-10-20 18:41:37 thib_gc Exp $
 */

/*
n
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

import java.io.*;
import java.util.*;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;


/**
 * Test class for <code>org.lockss.scheduler.TaskRunner</code>
 */

public class TestTaskRunner extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.scheduler.TaskRunner.class
  };

  static Logger log = Logger.getLogger("TestTaskRunner");
  private MyMockTaskRunner tr;
  private SchedFact fact;
  private List removedChunks;
  private List removedTasks;

  public void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated();
    removedChunks = new ArrayList();
    removedTasks = new ArrayList();
    fact = new SchedFact(null);
    tr = new MyMockTaskRunner(fact);
    tr.initService(getMockLockssDaemon());
    tr.startService();
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    tr.stopService();
    super.tearDown();
  }

  StepTask task(long start, long end, long duration) {
    return new StepperTask(Deadline.at(start), Deadline.at(end),
			   duration, null, null, new MyMockStepper());
  }

  StepTask task(long start, long end, long duration, TaskCallback cb) {
    return new StepperTask(Deadline.at(start), Deadline.at(end),
			   duration, cb, null, new MyMockStepper());
  }

  StepTask task(long start, long end, long duration,
		TaskCallback cb, Stepper stepper) {
    return new StepperTask(Deadline.at(start), Deadline.at(end),
			   duration, cb, null, stepper);
  }

  BackgroundTask btask(long start, long end, double loadFactor,
		      TaskCallback cb) {
    return new BackgroundTask(Deadline.at(start), Deadline.at(end),
			   loadFactor, cb);
  }

  // make a Chunk for the task
  Schedule.Chunk chunk(StepTask task) {
    return new Schedule.Chunk(task, task.getEarliestStart(),
			      task.getLatestFinish(),
			      task.curEst());
  }

  // make a BackgroundEvent for the task
  Schedule.BackgroundEvent bEvent(BackgroundTask task,
				  Schedule.EventType event) {
    return new Schedule.BackgroundEvent(task,
					(event == Schedule.EventType.START
					 ? task.getStart() : task.getFinish()),
					event);
  }

  // make a Schedule with one chunk per task
  Schedule sched(List tasks) {
    List events = new ArrayList();
    for (Iterator iter = tasks.iterator(); iter.hasNext(); ) {
      Object obj = iter.next();
      if (obj instanceof Schedule.Event) {
	events.add(obj);
      } else {
	SchedulableTask task = (SchedulableTask)obj;
	if (task.isBackgroundTask()) {
	  events.add(bEvent((BackgroundTask)task, Schedule.EventType.START));
	} else {
	  events.add(chunk((StepTask)task));
	}
      }
    }
    Schedule s = new Schedule(events);
    return s;
  }

  // ensure addToSchedule returns false if (Mock)Scheduler returns false
  public void testAddToScheduleFail() {
    fact.setResult(null);
    StepTask t1 = task(100, 200, 50);
    assertFalse(tr.addToSchedule(t1));
    assertEmpty(tr.getAcceptedTasks());
  }

  // ensure addToSchedule updates structures if (Mock)Scheduler returns true
  public void testAddToScheduleOk() {
    StepTask t1 = task(100, 200, 50);
    StepTask t2 = task(100, 200, 100);
    Schedule sched = sched(ListUtil.list(t1, t2));
    fact.setResult(sched);
    assertTrue(tr.addToSchedule(t1));
    assertIsomorphic(ListUtil.list(t1), fact.scheduler.tasks);
    fact.setResult(sched);
    assertTrue(tr.addToSchedule(t2));
    assertEquals(SetUtil.set(t1, t2), SetUtil.theSet(fact.scheduler.tasks));
    assertEquals(sched, tr.getCurrentSchedule());
    assertEquals(SetUtil.set(t1, t2), SetUtil.theSet(tr.getAcceptedTasks()));
  }

  // Now with task dropping on

  // only one try, because no tasks to drop
  public void testAddToScheduleFailNothingToDrop() {
    ConfigurationUtil.setFromArgs(TaskRunner.PARAM_DROP_TASK_MAX, "2");
    fact.setResult(null);
    StepTask t1 = task(100, 200, 50);
    assertFalse(tr.addToSchedule(t1));
    assertEmpty(tr.getAcceptedTasks());
    assertEquals(1, fact.createArgs.size());
  }

  // one task to drop, two failed schedule tries
  public void testAddToScheduleFailNoCleanup() {
    ConfigurationUtil.setFromArgs(TaskRunner.PARAM_DROP_TASK_MAX, "10",
				  TaskRunner.PARAM_MIN_CLEANUP_INTERVAL, "0");
    StepTask t1 = task(100, 200, 50);
    StepTask t2 = task(100, 200, 100);
    Schedule sched = sched(ListUtil.list(t1));
    fact.setResult(sched);
    assertTrue(tr.addToSchedule(t1));
    assertIsomorphic(ListUtil.list(t1), fact.scheduler.tasks);
    assertEquals(SetUtil.set(t1), SetUtil.theSet(tr.getAcceptedTasks()));

    assertFalse(tr.addToSchedule(t2));
    assertEquals(ListUtil.list(ListUtil.list(t1),
			       ListUtil.list(t1, t2),
			       ListUtil.list(t1)),
		 fact.createArgs);
    assertEquals(SetUtil.set(t1), SetUtil.theSet(fact.scheduler.tasks));
    assertEquals(sched, tr.getCurrentSchedule());
    assertEquals(SetUtil.set(t1), SetUtil.theSet(tr.getAcceptedTasks()));
  }

  // one task not ready to start yet, so not droppable
  public void testAddToScheduleFailNoDroppable() {
    log.debug("testAddToScheduleOkAfterDrops()");
    ConfigurationUtil.setFromArgs(TaskRunner.PARAM_DROP_TASK_MAX, "10",
				  TaskRunner.PARAM_MIN_CLEANUP_INTERVAL, "0");
    StepTask t1 = task(100, 200, 50);
    StepTask t2 = task(100, 200, 100);
    Schedule sched1 = sched(ListUtil.list(t1));
    Schedule sched2 = sched(ListUtil.list(t2));
    fact.setResults(ListUtil.list(sched1, null, null, sched2, sched2));
    assertTrue(tr.addToSchedule(t1));
    assertIsomorphic(ListUtil.list(t1), fact.scheduler.tasks);
    assertEquals(SetUtil.set(t1), SetUtil.theSet(tr.getAcceptedTasks()));

    assertFalse(tr.addToSchedule(t2));
    assertEquals(ListUtil.list(ListUtil.list(t1),
			       ListUtil.list(t1, t2),
			       ListUtil.list(t1)),
		 fact.createArgs);
    assertEquals(SetUtil.set(t1), SetUtil.theSet(fact.scheduler.tasks));
    assertFalse(t1.isDropped());
    assertEquals(sched1, tr.getCurrentSchedule());
    assertEquals(SetUtil.set(t1), SetUtil.theSet(tr.getAcceptedTasks()));
    assertEmpty(SetUtil.theSet(tr.getOverrunTasks()));
  }

  // one task to drop, succeeds after dropping it
  public void testAddToScheduleOkAfterDrops() {
    log.debug("testAddToScheduleOkAfterDrops()");
    ConfigurationUtil.setFromArgs(TaskRunner.PARAM_DROP_TASK_MAX, "10",
				  TaskRunner.PARAM_MIN_CLEANUP_INTERVAL, "0");
    StepTask t1 = task(100, 200, 50);
    StepTask t2 = task(100, 200, 100);
    Schedule sched1 = sched(ListUtil.list(t1));
    Schedule sched2 = sched(ListUtil.list(t2));
    fact.setResults(ListUtil.list(sched1, null, null, sched2, sched2));
    TimeBase.step(101);
    assertTrue(tr.addToSchedule(t1));
    assertIsomorphic(ListUtil.list(t1), fact.scheduler.tasks);
    assertEquals(SetUtil.set(t1), SetUtil.theSet(tr.getAcceptedTasks()));

    assertTrue(tr.addToSchedule(t2));
    assertEquals(ListUtil.list(ListUtil.list(t1),
			       ListUtil.list(t1, t2),
			       ListUtil.list(t1),
			       Collections.EMPTY_SET,
			       ListUtil.list(t1, t2)),
		 fact.createArgs);
    assertEquals(SetUtil.set(t1, t2), SetUtil.theSet(fact.scheduler.tasks));
    assertTrue(t1.isDropped());
    assertEquals(sched2, tr.getCurrentSchedule());
    assertEquals(SetUtil.set(t1, t2), SetUtil.theSet(tr.getAcceptedTasks()));
    assertEquals(SetUtil.set(t1), SetUtil.theSet(tr.getOverrunTasks()));
  }


  public void testIsTaskSchedulable() {
    fact.setResult(null);
    StepTask t1 = task(100, 200, 50);
    assertFalse(tr.isTaskSchedulable(t1));
    fact.setResult(sched(ListUtil.list(t1)));
    assertTrue(tr.isTaskSchedulable(t1));
  }

  public void testFindChunkTaskToRun() {
    assertFalse(tr.findTaskToRun());
    StepTask t1 = task(100, 200, 100);
    StepTask t2 = task(100, 300, 50);

    Schedule s = sched(ListUtil.list(t1, t2));
    fact.setResults(s, s);
    assertTrue(tr.addToSchedule(t1));
    assertTrue(tr.addToSchedule(t2));
    assertFalse(tr.findTaskToRun());
    assertEquals(Deadline.at(100), tr.runningDeadline);

    TimeBase.setSimulated(101);
    assertTrue(tr.findTaskToRun());
    assertEquals(t1, tr.runningTask);
    assertEquals(t1.getLatestFinish(), tr.runningDeadline);
    assertEquals(s.getEvents().get(0), tr.runningChunk);
  }

  public void testFindRunnableChunk() {
    assertFalse(tr.findTaskToRun());
    StepTask t1 = task(100, 200, 100);
    StepTask t2 = task(10, 300, 50);
    Schedule.Chunk c1 = new Schedule.Chunk(t1, Deadline.at(100),
					   Deadline.at(200), 100);
    Schedule.Chunk c2 = new Schedule.Chunk(t2, Deadline.at(200),
					   Deadline.at(300), 100);
    Schedule s = new Schedule(ListUtil.list(c1, c2));
    fact.setResults(s, s);
    assertTrue(tr.addToSchedule(t1));
    assertTrue(tr.addToSchedule(t2));
    assertFalse(tr.findTaskToRun());
    assertEquals(Deadline.at(100), tr.runningDeadline);
    TimeBase.setSimulated(11);
    assertTrue(tr.findTaskToRun());
    assertEquals(t2, tr.runningTask);
    assertEquals(c2, tr.runningChunk);
    assertEquals(Deadline.at(100), tr.runningDeadline);
    assertEquals(s.getEvents().get(1), tr.runningChunk);
  }

  public void testFindOverrunTaskToRun() {
    assertFalse(tr.findTaskToRun());
    StepTask t1 = task(100, 200, 100);
    Schedule s = sched(ListUtil.list(t1));
    fact.setResult(s);
    assertTrue(tr.addToSchedule(t1));
    assertFalse(tr.findTaskToRun());
    assertEquals(Deadline.at(100), tr.runningDeadline);

    StepTask t2 = task(0, 300, 50);
    tr.addOverrunner(t2);
    assertTrue(tr.findTaskToRun());
    assertEquals(t2, tr.runningTask);
    assertEquals(Deadline.at(100), tr.runningDeadline);
    assertNull(tr.runningChunk);
  }

  public void testFindTaskToRunRemovesExpiredChunks() {
    assertFalse(tr.findTaskToRun());
    StepTask t1 = task(100, 200, 100);
    StepTask t2 = task(100, 300, 50);
    StepTask texp1 = task(0, 0, 50);
    StepTask texp2 = task(0, 0, 50);

    Schedule s = sched(ListUtil.list(texp1, texp2, t1, t2));
    fact.setResults(s, s);
    assertTrue(tr.addToSchedule(t1));
    assertTrue(tr.addToSchedule(t2));
    assertFalse(tr.findTaskToRun());
    assertEquals(2, removedChunks.size());
    assertEquals(SetUtil.set(texp1, texp2),
		 SetUtil.set(((Schedule.Chunk)removedChunks.get(0)).getTask(),
			     ((Schedule.Chunk)removedChunks.get(1)).getTask()));
  }

  public void testFindTaskToRunRemovesExpiredOverrunners() {
    assertFalse(tr.findTaskToRun());
    StepTask t1 = task(100, 200, 100);
    StepTask t2 = task(100, 300, 50);
    StepTask texp1 = task(0, 0, 50);
    StepTask texp2 = task(0, 0, 49);

    Schedule s = sched(ListUtil.list(t1, t2));
    fact.setResults(s, s);
    assertTrue(tr.addToSchedule(t1));
    assertTrue(tr.addToSchedule(t2));
    tr.addOverrunner(texp1);
    tr.addOverrunner(texp2);

    // if this fails, it might be because the sorted list/set is treating
    // sort-order equivalence as object equality, which we don't want
    assertEquals(2, tr.getOverrunTasks().size());
    assertFalse(tr.findTaskToRun());
    assertEquals(0, removedChunks.size());
    assertEquals(2, removedTasks.size());
    assertEquals(SetUtil.set(texp1, texp2),
		 SetUtil.set((StepTask)removedTasks.get(0),
			     (StepTask)removedTasks.get(1)));
  }

  public void testRemoveChunk() {
    StepTask t1 = task(100, 200, 100);
    Schedule s = sched(ListUtil.list(t1));
    fact.setResult(s);
    assertTrue(tr.addToSchedule(t1));
    assertIsomorphic(ListUtil.list(t1), tr.getAcceptedTasks());

    Schedule.Chunk chunk = (Schedule.Chunk)s.getEvents().get(0);
    assertTrue(tr.getCurrentSchedule().getEvents().contains(chunk));
    tr.removeChunk(chunk);
    assertFalse(tr.getCurrentSchedule().getEvents().contains(chunk));
  }

  // This should generate an impossible state log, and leave the task in
  // acceptedTasks
  public void testRemoveChunkTaskEnd() {
    final List finished = new ArrayList();
    StepTask t1 = task(100, 200, 100, new TaskCallback() {
	public void taskEvent(SchedulableTask task, Schedule.EventType event) {
	  if (log.isDebug2()) {
	    log.debug2("testRemoveChunkTaskEnd event " + event);
	  }
	  if (event == Schedule.EventType.FINISH) {
	    finished.add(task);
	  }
	}});
    Schedule s = sched(ListUtil.list(t1));
    fact.setResult(s);
    assertTrue(tr.addToSchedule(t1));

    Schedule.Chunk chunk = (Schedule.Chunk)s.getEvents().get(0);
    assertTrue(tr.getCurrentSchedule().getEvents().contains(chunk));
    chunk.setTaskEnd();
    t1.setFinished(); // avoids impossible task state warning in removeTask()
    tr.removeChunk(chunk);
    assertFalse(tr.getCurrentSchedule().getEvents().contains(chunk));
    assertEmpty(tr.getAcceptedTasks());
    assertIsomorphic(ListUtil.list(t1), finished);
  }

  // remove task-ending chunk, past task deadline, s.b. Timeout error.
  public void testRemoveChunkTaskEndTimeout() {
    final List finished = new ArrayList();
    StepTask t1 = task(100, 200, 100, new TaskCallback() {
	public void taskEvent(SchedulableTask task, Schedule.EventType event) {
	  if (log.isDebug2()) {
	    log.debug2("testRemoveChunkTaskEndTimeout callback");
	  }
	  if (event == Schedule.EventType.FINISH) {
	    finished.add(task);
	  }
	}});
    Schedule s = sched(ListUtil.list(t1));
    fact.setResult(s);
    assertTrue(tr.addToSchedule(t1));

    Schedule.Chunk chunk = (Schedule.Chunk)s.getEvents().get(0);
    assertTrue(tr.getCurrentSchedule().getEvents().contains(chunk));
    chunk.setTaskEnd();
    TimeBase.setSimulated(201);
    tr.removeChunk(chunk);
    assertFalse(tr.getCurrentSchedule().getEvents().contains(chunk));
    assertSame(t1, finished.get(0));
    assertNotNull(t1.e);
    assertTrue(t1.e.toString(), t1.e instanceof SchedService.Timeout);
    assertEmpty(tr.getAcceptedTasks());
  }

  // remove overrunnable task-ending chunk, before deadline,
  public void testRemoveChunkTaskEndOver() {
    final List finished = new ArrayList();
    StepTask t1 = task(100, 200, 100, new TaskCallback() {
	public void taskEvent(SchedulableTask task, Schedule.EventType event) {
	  if (log.isDebug2()) {
	    log.debug2("testRemoveChunkTaskEndOver callback");
	  }
	  if (event == Schedule.EventType.FINISH) {
	    finished.add(task);
	  }
	}});
    t1.setOverrunAllowed(true);
    Schedule s = sched(ListUtil.list(t1));
    fact.setResult(s);
    assertTrue(tr.addToSchedule(t1));

    Schedule.Chunk chunk = (Schedule.Chunk)s.getEvents().get(0);
    assertTrue(tr.getCurrentSchedule().getEvents().contains(chunk));
    chunk.setTaskEnd();
    tr.removeChunk(chunk);
    assertFalse(tr.getCurrentSchedule().getEvents().contains(chunk));
    assertEmpty(finished);
    assertIsomorphic(ListUtil.list(t1), tr.getAcceptedTasks());
    assertIsomorphic(SetUtil.set(t1), tr.getOverrunTasks());
  }


  // Background event record
  class BERec {
    Deadline when;
    BackgroundTask task;
    Schedule.EventType event;
    BERec(Deadline when, BackgroundTask task, Schedule.EventType event) {
      this.when = when;
      this.task = task;
      this.event = event;
    }
    BERec(long when, BackgroundTask task, Schedule.EventType event) {
      this.when = Deadline.at(when);
      this.task = task;
      this.event = event;
    }
    public boolean equals(Object obj) {
      if (obj instanceof BERec) {
	BERec o = (BERec)obj;
	return when.equals(o.when) &&
	  task.equals(o.task) &&
	  event == o.event;
      }
      return false;
    }
    public String toString() {
      return "[BERec: " + event + ", " + when + ", " + task + "]";
    }
  }


  public void testBackground() {
    final List rec = new ArrayList();
    TaskCallback cb = new TaskCallback() {
	public void taskEvent(SchedulableTask task, Schedule.EventType event) {
	  rec.add(new BERec(Deadline.in(0), (BackgroundTask)task, event));
	}};
    assertFalse(tr.findTaskToRun());
    BackgroundTask t1 = btask(100, 200, .1, cb);
    BackgroundTask t2 = btask(100, 300, .2, cb);
    BackgroundTask t3 = btask(150, 200, .4, cb);

    Schedule s = sched(ListUtil.list(bEvent(t1, Schedule.EventType.START),
				     bEvent(t2, Schedule.EventType.START),
				     bEvent(t3, Schedule.EventType.START),
				     bEvent(t1, Schedule.EventType.FINISH),
				     bEvent(t3, Schedule.EventType.FINISH),
				     bEvent(t2, Schedule.EventType.FINISH)));
    fact.setResults(ListUtil.list(s, s, s));

    assertTrue(tr.addToSchedule(t1));
    assertTrue(tr.addToSchedule(t2));
    assertTrue(tr.addToSchedule(t3));
    assertEquals(3, tr.getAcceptedTasks().size());
    assertIsomorphic(ListUtil.list(t1, t2, t3), tr.getAcceptedTasks());
    assertFalse(tr.findTaskToRun());
    assertEquals(0, rec.size());
    assertEquals(0, tr.getBackgroundLoadFactor(), .005);
    assertEquals(Deadline.at(100), tr.runningDeadline);

    TimeBase.setSimulated(101);
    assertFalse(tr.findTaskToRun());
    assertEquals(2, rec.size());
    assertEquals(.3, tr.getBackgroundLoadFactor(), .005);
    TimeBase.setSimulated(151);
    assertFalse(tr.findTaskToRun());
    assertEquals(3, rec.size());
    assertEquals(.7, tr.getBackgroundLoadFactor(), .005);
    assertEquals(3, tr.getAcceptedTasks().size());
    TimeBase.setSimulated(201);
    assertFalse(tr.findTaskToRun());
    assertEquals(5, rec.size());
    assertEquals(.2, tr.getBackgroundLoadFactor(), .005);
    assertEquals(1, tr.getAcceptedTasks().size());
    t2.taskIsFinished();
    TimeBase.setSimulated(202);
    assertFalse(tr.findTaskToRun());
    assertEquals(6, rec.size());
    assertEquals(0, tr.getBackgroundLoadFactor(), .005);
    assertEquals(0, tr.getAcceptedTasks().size());
    TimeBase.setSimulated(301);
    assertFalse(tr.findTaskToRun());
    assertEquals(6, rec.size());
    assertEquals(0, tr.getBackgroundLoadFactor(), .005);
    List exp = ListUtil.list(new BERec(101, t1, Schedule.EventType.START),
			     new BERec(101, t2, Schedule.EventType.START),
			     new BERec(151, t3, Schedule.EventType.START),
			     new BERec(201, t1, Schedule.EventType.FINISH),
			     new BERec(201, t3, Schedule.EventType.FINISH),
			     new BERec(201, t2, Schedule.EventType.FINISH));
    assertEquals(exp, rec);
  }


  public void testRunStepsOneTaskAndCallback() {
    final List finished = new ArrayList();
    TaskCallback cb = new TaskCallback() {
	public void taskEvent(SchedulableTask task, Schedule.EventType event) {
	  if (event == Schedule.EventType.FINISH) {
	    finished.add(task);
	  }
	}};

    StepTask t1 = task(100, 200, 100, cb, new MyMockStepper(10, -10));
    Schedule s = sched(ListUtil.list(t1));
    fact.setResult(s);
    assertTrue(tr.addToSchedule(t1));
    TimeBase.setSimulated(101);
    assertTrue(tr.findTaskToRun());
    Interrupter intr = null;
    try {
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      tr.runSteps(new MutableBoolean(true), null);
      intr.cancel();
    } catch (Exception e) {
      log.error("runSteps threw:", e);
    } finally {
      if (intr.did()) {
	fail("runSteps looped");
      }
    }
    assertSame(t1, finished.get(0));
    assertNull(t1.e);
  }

  public void testRunStepsWithOverrunDisallowed() {
    StepTask t1 = task(100, 300, 100, null, new MyMockStepper(15, -10));
    //    t1.setOverrunAllowed(true);
    StepTask t2 = task(150, 250, 100, null, new MyMockStepper(10, -10));
    Schedule s = sched(ListUtil.list(t1, t2));
    fact.setResults(s, s);
    assertTrue(tr.addToSchedule(t1));
    assertTrue(tr.addToSchedule(t2));
    TimeBase.setSimulated(101);
    Interrupter intr = null;
    try {
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      while(tr.findTaskToRun()) {
	tr.runSteps(new MutableBoolean(true), null);
      }
      intr.cancel();
    } catch (Exception e) {
      log.error("runSteps threw:", e);
    } finally {
      if (intr.did()) {
	fail("runSteps looped");
      }
    }
    assertEquals(SetUtil.set(t1, t2), SetUtil.theSet(removedTasks));
    assertTrue(t1.e.toString(), t1.e instanceof SchedService.Overrun);
  }

  private void newTr(MyMockTaskRunner newTr) {
    if (tr != null) {
      tr.stopService();
    }
    tr = newTr;
    tr.initService(getMockLockssDaemon());
    tr.startService();
  }

  public void testRunStepsWithOverrunAllowed() {
    StepTask t1 = task(100, 500, 30, null, new MyMockStepper(15, -10));
    t1.setOverrunAllowed(true);
    StepTask t2 = task(150, 250, 100, null, new MyMockStepper(10, -10));
    newTr(new MyMockTaskRunner(new TaskRunner.SchedulerFactory () {
	public Scheduler createScheduler() {
	  return new SortScheduler();
	}}));
    assertTrue(tr.addToSchedule(t1));
    assertTrue(tr.addToSchedule(t2));
    TimeBase.setSimulated(101);
    assertTrue(tr.findTaskToRun());
    Interrupter intr = null;
    try {
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      while(tr.findTaskToRun()) {
	tr.runSteps(new MutableBoolean(true), null);
      }
      intr.cancel();
    } catch (Exception e) {
      log.error("runSteps threw:", e);
    } finally {
      if (intr.did()) {
	fail("runSteps looped");
      }
    }
    assertNull(t1.e);
    assertTrue(t1.hasOverrun());
  }


  // test resched with overrun task doesn't lose task.
  public void testRunStepsWithOverrunAllowedPlusResched() {
    StepTask t1 = task(100, 500, 30, null, new MyMockStepper(15, -10));
    t1.setOverrunAllowed(true);
    StepTask t2 = task(150, 250, 100, null, new MyMockStepper(10, -10));
    newTr(new MyMockTaskRunner(new TaskRunner.SchedulerFactory () {
	public Scheduler createScheduler() {
	  return new SortScheduler();
	}}));
    assertTrue(tr.addToSchedule(t1));
    assertEmpty(tr.getOverrunTasks());
    TimeBase.setSimulated(101);
    assertTrue(tr.findTaskToRun());
    t1.timeUsed = 1000;
    assertTrue(t1.hasOverrun());
    assertEmpty(tr.getOverrunTasks());
    assertTrue(tr.addToSchedule(t2));
    assertEquals(SetUtil.set(t1), SetUtil.theSet(tr.getOverrunTasks()));
  }

  public void testStepperThrows() {
    final List finished = new ArrayList();
    TaskCallback cb = new TaskCallback() {
	public void taskEvent(SchedulableTask task, Schedule.EventType event) {
	  if (event == Schedule.EventType.FINISH) {
	    finished.add(task);
	  }
	}};

    MyMockStepper stepper = new MyMockStepper(10, -10);
    stepper.setWhenToThrow(5);
    StepTask t1 = task(100, 200, 100, cb, stepper);
    Schedule s = sched(ListUtil.list(t1));
    fact.setResult(s);
    assertTrue(tr.addToSchedule(t1));
    TimeBase.setSimulated(101);
    assertTrue(tr.findTaskToRun());
    Interrupter intr = null;
    try {
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      tr.runSteps(new MutableBoolean(true), null);
      intr.cancel();
    } catch (Exception e) {
      log.error("runSteps threw:", e);
    } finally {
      if (intr.did()) {
	fail("runSteps looped");
      }
    }
    assertSame(t1, finished.get(0));
    assertTrue(t1.e instanceof ExpectedRuntimeException);
    assertEquals(5, stepper.nSteps);
  }

  public void testNotifyThread() {
    final List rec = new ArrayList();
    final SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    tr.setImmediateNotify(false);

    TaskCallback cb = new TaskCallback() {
	public void taskEvent(SchedulableTask task, Schedule.EventType event) {
	  rec.add(new BERec(Deadline.in(0), (BackgroundTask)task, event));
	  sem.give();
	}};
    BackgroundTask t1 = btask(100, 200, .1, cb);
    BackgroundTask t2 = btask(100, 300, .2, cb);

    tr.notify(t1, Schedule.EventType.START);
    tr.notify(t1, Schedule.EventType.FINISH);
    // 2nd finish event should not cause another callback
    tr.notify(t1, Schedule.EventType.FINISH);
    tr.notify(t2, Schedule.EventType.START);

    Interrupter intr = null;
    try {
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      while (rec.size() < 3) {
	sem.take();
      }
      assertEquals(ListUtil.list(
				 new BERec(0, t1, Schedule.EventType.START),
				 new BERec(0, t1, Schedule.EventType.FINISH),
				 new BERec(0, t2, Schedule.EventType.START)),
		   rec);
      intr.cancel();
    } finally {
      if (intr.did()) {
	fail("Notifier didn't run callbacks");
      }
    }
  }

  class MyMockTaskRunner extends TaskRunner {
    private boolean doImmediateNotify = true;

    MyMockTaskRunner(TaskRunner.SchedulerFactory fact) {
      super(fact);
    }

    void removeChunk(Schedule.Chunk chunk) {
      removedChunks.add(chunk);
      super.removeChunk(chunk);
    }

    void removeTask(StepTask task) {
      removedTasks.add(task);
      super.removeTask(task);
    }
    // Most of the tests in this file were written when task event
    // notification was done synchronously in TaskRunner.  Reproduce that
    // behavior here for simplicity.
    void notify(SchedulableTask task, Schedule.EventType eventType) {
      if (doImmediateNotify) {
	task.callback.taskEvent(task, eventType);
      } else {
	super.notify(task, eventType);
      }
    }

    void setImmediateNotify(boolean immediate) {
      doImmediateNotify = immediate;
    }
  }

  class SchedFact implements TaskRunner.SchedulerFactory {
    List results;
    MyMockScheduler scheduler;
    List createArgs = new ArrayList();

    public SchedFact(Schedule resultSchedule) {
      this.results = ListUtil.list(resultSchedule);
    }

    public SchedFact() {
      this(null);
    }

    public void setResult(Schedule resultSchedule) {
      this.results = ListUtil.list(resultSchedule);
    }

    public void setResults(List results) {
      this.results = results;
    }

    public void setResults(Schedule s1, Schedule s2) {
      setResults(ListUtil.list(s1, s2));
    }

    public Scheduler createScheduler() {
      scheduler = new MyMockScheduler(results);
      return scheduler;
    }

    class MyMockScheduler implements Scheduler {
      List results;
      Collection tasks;
      Schedule lastSched = null;

      MyMockScheduler(List results) {
	this.results = results;
      }

      public boolean createSchedule(Collection tasks) {
	log.debug("createSchedule(" + tasks + ")");
	this.tasks = tasks;
	createArgs.add(tasks);
	if (results == null || results.isEmpty()) {
	  lastSched = null;
	} else {
	  lastSched = (Schedule)results.remove(0);
	}
	return lastSched != null;
      }

      public Schedule getSchedule() {
	log.info("getSchedule(): " + lastSched);
	return lastSched;
      }

      public Collection getTasks() {
	return tasks;
      }
    }
  }

  class MyMockStepper implements Stepper {
    int nSteps = 1;			// not finished by default
    int eachStepTime = 0;
    int whenToThrow = -1;

    MyMockStepper() {
    }

    /** Make a stepper that repeats n times: wait until time elapsed, or
     * advance simulated time,
     * @param nSteps number of steps to execute before isFinished returns
     * true.
     * @param eachStepTime if >0, ms to sleep on each step.  if <0, step
     * @returns some measure of amount of work done.
     * simulated time by abs(eachStepTime) on each step.
     */
    MyMockStepper(int nSteps, int eachStepTime) {
      this.nSteps = nSteps;
      this.eachStepTime = eachStepTime;
    }

    public int computeStep(int metric) {
      int work = 0;
      if (nSteps == whenToThrow) {
	throw new ExpectedRuntimeException("Hash step throw test");
      }
      if (nSteps-- > 0) {
	if (eachStepTime > 0) {
	  Deadline time = Deadline.in(eachStepTime);
	  while (!time.expired()) {
	    try {
	      Thread.sleep(1);
	    }catch (InterruptedException e) {
	      throw new RuntimeException(e.toString());
	    }
	    work++;
	  }
	} else {
	  work = -eachStepTime;
	  TimeBase.step(work);
	  try {
	    Thread.sleep(1);
	  } catch (InterruptedException e) {
	    throw new RuntimeException(e.toString());
	  }
	}
      }
      return work;
    }

    public boolean isFinished() {
      return nSteps <= 0;
    }

    void setFinished() {
      nSteps = 0;
    }

    void setWhenToThrow(int step) {
      whenToThrow = step;
    }
  }
}
