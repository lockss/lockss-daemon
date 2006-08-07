/*
 * $Id: TestAuTreeWalkManager.java,v 1.11 2006-08-07 18:47:48 tlipkis Exp $
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

package org.lockss.state;

import java.io.*;
import java.util.*;
import org.lockss.app.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.scheduler.*;
import org.lockss.plugin.*;

public class TestAuTreeWalkManager extends LockssTestCase {
  private static Logger log = Logger.getLogger("TestAuTreeWalkManager");

  static Boolean TRUE = Boolean.TRUE;
  static Boolean FALSE = Boolean.FALSE;

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau = null;
  private MyMockAuTreeWalkManager autwm;
  private MyMockAuTreeWalkManager.MyMockTreeWalker walker;
  private MyMockSchedService sched;
  private MockNodeManager nodeMgr;

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = getMockLockssDaemon();
    stdConfig();

    mau = new MockArchivalUnit();
    MockPlugin mp = new MockPlugin();
    mp.initPlugin(theDaemon);
    mau.setPlugin(mp);
    theDaemon.getPluginManager();
    PluginTestUtil.registerArchivalUnit(mau);

    theDaemon.getTreeWalkManager().startService();

    nodeMgr = new MockNodeManager();
    theDaemon.setNodeManager(nodeMgr, mau);

    autwm = new MyMockAuTreeWalkManager(mau);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  void stdConfig() {
    Properties p = new Properties();
    p.put(TreeWalkManager.PARAM_TREEWALK_THREAD_POOL_MIN, "2");
    p.put(TreeWalkManager.PARAM_TREEWALK_THREAD_POOL_MAX, "6");
    p.put(TreeWalkManager.PARAM_TREEWALK_INTERVAL_MIN, "12h");
    p.put(TreeWalkManager.PARAM_TREEWALK_INTERVAL_MAX, "36h");
    p.put(TreeWalkManager.PARAM_TREEWALK_START_DELAY,  "1s");
    p.put(TreeWalkManager.PARAM_TREEWALK_INITIAL_ESTIMATE, "11000");
    p.put(TreeWalkManager.PARAM_TREEWALK_ESTIMATE_GROWTH_MULTIPLIER, "150");
    p.put(TreeWalkManager.PARAM_TREEWALK_ESTIMATE_PADDING_MULTIPLIER, "125");
    p.put(TreeWalkManager.PARAM_TREEWALK_SLEEP_INTERVAL, "1s");
    p.put(TreeWalkManager.PARAM_TREEWALK_LOAD_FACTOR, "90");
    p.put(TreeWalkManager.PARAM_TREEWALK_MIN_DURATION, "10s");
    p.put(TreeWalkManager.PARAM_TREEWALK_MAX_FUTURE_SCHED, "3w");
    p.put(TreeWalkManager.PARAM_TREEWALK_SCHED_FAIL_RETRY, "2h");
    p.put(TreeWalkManager.PARAM_TREEWALK_SCHED_FAIL_INCREMENT, "10m");
    p.put(TreeWalkManager.PARAM_TREEWALK_EXECUTE_FAIL_RETRY, "1h");
    p.put(TreeWalkManager.WDOG_PARAM_TREEWALK, "10h");

    ConfigurationUtil.setCurrentConfigFromProps(p);
  }

  void startMockSched() {
    sched = new MyMockSchedService();
    theDaemon.setSchedService(sched);
  }

  void startSched() {
    SchedService s = theDaemon.getSchedService();
    s.startService();
  }

  public void testCantSchedule() {
    TimeBase.setSimulated(1000);
    autwm.initService(theDaemon);
    // MyMockSchedService.scheduleTask() fails by default
    startMockSched();
    autwm.startService();		// should schedule task
    BackgroundTask task = autwm.curTask;
    assertNull(task);
    // Make scheduleTask() succeed
    sched.schedResults = ListUtil.list(TRUE);
    // sched fail retry interval
    TimeBase.step(2 * Constants.HOUR);
    // task should now be scheduled
    task = autwm.curTask;
    assertNotNull(task);
  }

  public void testCantScheduleAtFirst() {
    TimeBase.setSimulated(1000);
    autwm.initService(theDaemon);
    startMockSched();
    // make first scheduling attempt fail, 2nd succeed
    sched.schedResults = ListUtil.list(FALSE, TRUE);
    autwm.startService();		// should schedule task
    BackgroundTask task = autwm.curTask;
    assertNotNull(task);
    assertEquals(TimeBase.nowMs() + Constants.SECOND + 10 * Constants.MINUTE,
		 task.getStart().getExpirationTime());
  }

  public void testRunWalker() {
    TimeBase.setSimulated(1000);
    autwm.initService(theDaemon);
    autwm.walkWaitSem.take();
    startSched();
    autwm.result = true;
    autwm.didFullTreewalk = true;
    autwm.startService();		// should schedule task
    BackgroundTask task = autwm.curTask;
    assertNotNull(task);
    assertFalse(task.isFinished());
    TimeBase.step(1001);		// allow task to start
    assertTrue(autwm.walkStartSem.take(TIMEOUT_SHOULDNT));
    TimeBase.step(10000);
    autwm.walkWaitSem.give();
    assertTrue(autwm.walkDoneSem.take(TIMEOUT_SHOULDNT));
    assertEquals(1, autwm.numWalks);
    assertTrue(autwm.deferredSem.take(TIMEOUT_SHOULDNT));
    // task should be marked finished by the time doDeferredAction() runs
    assertTrue(task.isFinished());
    // wait for lockssRun to finish
    assertTrue(autwm.lockssRunDoneSem.take(TIMEOUT_SHOULDNT));
    // now it should have scheduled a new task
    assertNotNull(autwm.curTask);
    assertNotEquals(task, autwm.curTask);
    // for at least interval.min from now
    assertTrue(autwm.curTask.getStart().getExpirationTime() >=
	       TimeBase.nowMs() + 12 * Constants.HOUR);
    // and estimate should now be 1.25 times the 10 seconds that one took
    assertEquals(12500, autwm.treeWalkEstimate);
  }

  public void testRunWalkerNoFullTreealk() {
    TimeBase.setSimulated(1000);
    autwm.initService(theDaemon);
    autwm.walkWaitSem.take();
    startSched();
    autwm.result = true;
    autwm.didFullTreewalk = false;
    autwm.startService();		// should schedule task
    BackgroundTask task = autwm.curTask;
    assertNotNull(task);
    assertFalse(task.isFinished());
    TimeBase.step(1001);		// allow task to start
    assertTrue(autwm.walkStartSem.take(TIMEOUT_SHOULDNT));
    TimeBase.step(2000);
    autwm.walkWaitSem.give();
    assertTrue(autwm.walkDoneSem.take(TIMEOUT_SHOULDNT));
    assertEquals(1, autwm.numWalks);
    assertTrue(autwm.deferredSem.take(TIMEOUT_SHOULDNT));
    // task should be marked finished by the time doDeferredAction() runs
    assertTrue(task.isFinished());
    // wait for lockkRun to finish
    assertTrue(autwm.lockssRunDoneSem.take(TIMEOUT_SHOULDNT));
    // now it should have scheduled a new task
    assertNotNull(autwm.curTask);
    assertNotEquals(task, autwm.curTask);
    // for at least interval.min from now
    assertTrue(autwm.curTask.getStart().getExpirationTime() >=
	       TimeBase.nowMs() + 12 * Constants.HOUR);
    // and estimate should still be 3
    assertEquals(11000, autwm.treeWalkEstimate);
  }

  public void testRunWalkerNoTreewalk() {
    TimeBase.setSimulated(1000);
    autwm.initService(theDaemon);
    startSched();
    autwm.result = false;
    autwm.startService();		// should schedule task
    BackgroundTask task = autwm.curTask;
    assertNotNull(task);
    assertFalse(task.isFinished());
    TimeBase.step(1001);		// allow task to start
    assertTrue(autwm.walkDoneSem.take(TIMEOUT_SHOULDNT));
    assertEquals(1, autwm.numWalks);
    // wait for lockkRun to finish
    assertTrue(autwm.lockssRunDoneSem.take(TIMEOUT_SHOULDNT));
    // task should be marked finished by now
    assertTrue(task.isFinished());
    // now it should have scheduled a new task
    assertNotNull(autwm.curTask);
    assertNotEquals(task, autwm.curTask);
    // for exactly an hour from now
    assertEquals(TimeBase.nowMs() + Constants.HOUR,
		 autwm.curTask.getStart().getExpirationTime());
  }

  public void testStartPoolThreadThrowsRuntimeException() {
    TimeBase.setSimulated(1000);
    // cause startThread to get a RuntimeException
    autwm = new MyMockAuTreeWalkManager(mau) {
	protected void executeRunner(TreeWalkRunner runner) {
	  throw new ExpectedRuntimeException();
	}};

    autwm.initService(theDaemon);
    startSched();
    autwm.startService();		// should schedule task
    BackgroundTask task = autwm.curTask;
    assertNotNull(task);
    assertFalse(task.isFinished());
    TimeBase.step(1001);
    // wait until start evant has completed.
    assertTrue(autwm.eventStartSem.take(TIMEOUT_SHOULDNT));
    // no walk should have happened (bad test, might not have anyway yet)
    assertEquals(0, autwm.numWalks);
    // a different task should now be scheduled
    assertNotNull(autwm.curTask);
    assertNotEquals(task, autwm.curTask);
    assertEquals(TimeBase.nowMs() + Constants.HOUR,
		 autwm.curTask.getStart().getExpirationTime());
  }

  public void testWalkerRunsTooLong() {
    TimeBase.setSimulated(1000);
    autwm.initService(theDaemon);
    autwm.walkWaitSem.take();
    startSched();
    autwm.startService();		// should schedule task
    BackgroundTask task = autwm.curTask;
    assertNotNull(task);
    assertFalse(task.isFinished());
    TimeBase.step(1001);		// allow task to start
    // wait until start evant has completed.
    assertTrue(autwm.eventStartSem.take(TIMEOUT_SHOULDNT));
    assertTrue(autwm.walkStartSem.take(TIMEOUT_SHOULDNT));
    TimeBase.step(12000);
    // wait until finish evant has completed.
    assertTrue(autwm.eventFinishSem.take(TIMEOUT_SHOULDNT));
    // walker should have been told to abort
    assertTrue(autwm.aborted);
  }

  class MyMockAuTreeWalkManager extends AuTreeWalkManager {
    SimpleBinarySemaphore eventStartSem = new SimpleBinarySemaphore();
    SimpleBinarySemaphore eventFinishSem = new SimpleBinarySemaphore();

    // these are used by the mock walker, but since the walker isn't
    // created until the task actually runs it's easier to put them here,
    // and make the walker an inner class so it can access them
    SimpleBinarySemaphore walkStartSem = new SimpleBinarySemaphore();
    SimpleBinarySemaphore walkDoneSem = new SimpleBinarySemaphore();
    SimpleBinarySemaphore walkWaitSem = new SimpleBinarySemaphore();
    SimpleBinarySemaphore deferredSem = new SimpleBinarySemaphore();

    SimpleBinarySemaphore lockssRunDoneSem = new SimpleBinarySemaphore();

    MyMockTreeWalker walker;
    boolean didFullTreewalk = false;
    Deadline finishBy;
    boolean aborted;
    boolean didDeferred;
    boolean result;
    int numWalks = 0;

    MyMockAuTreeWalkManager(ArchivalUnit au) {
      super(au);
      walkWaitSem.give();
    }

    public synchronized void taskEvent(SchedulableTask task,
				       Schedule.EventType event) {
      super.taskEvent(task, event);
      if (event == Schedule.EventType.START) {
	eventStartSem.give();
      } else if (event == Schedule.EventType.FINISH) {
	eventFinishSem.give();
      }
    }

    protected TreeWalkRunner newRunner(ArchivalUnit au, BackgroundTask task) {
      return new MyMockTreeWalkRunner(au, task);
    }

    protected TreeWalker newWalker(LockssDaemon daemon, ArchivalUnit au) {
      walker = new MyMockTreeWalker(daemon, au);
      return walker;
    }

    MyMockTreeWalker getWalker() {
      return walker;
    }

    class MyMockTreeWalkRunner extends TreeWalkRunner {
      public MyMockTreeWalkRunner(ArchivalUnit au, BackgroundTask task) {
	super(au, task);
      }

      public void lockssRun() {
	super.lockssRun();
	lockssRunDoneSem.give();
      }
    }

    class MyMockTreeWalker implements TreeWalker {
      LockssWatchdog wdog;
      ArchivalUnit au;

      MyMockTreeWalker(LockssDaemon daemon, ArchivalUnit au) {
	this.au = au;
      }

      public void setWDog(LockssWatchdog wdog) {
	this.wdog = wdog;
      }

      public boolean didFullTreewalk() {
	return didFullTreewalk;
      }

      public boolean doTreeWalk(Deadline time) {
	walkStartSem.give();
	walkWaitSem.take();
	finishBy = time;
	numWalks++;
	walkDoneSem.give();
	return result;
      }

      public void abort() {
	aborted = true;
      }

      public void doDeferredAction() {
	didDeferred = true;
	deferredSem.give();
      }
    }
  }

  class MyMockSchedService extends SchedService {
    List schedResults;

    MyMockSchedService() {
    }

    public void startService() {
    }

    public void stopService() {
    }

    public boolean scheduleTask(SchedulableTask task) {
      if (schedResults != null && !schedResults.isEmpty()) {
	return ((Boolean)schedResults.remove(0)).booleanValue();
      }
      return false;
    }

    public boolean isTaskSchedulable(SchedulableTask task) {
      return false;
    }

    public BackgroundTask scheduleHint(BackgroundTask task) {
      return null;
    }

    public boolean isIdle() {
      return false;
    }
  }


//   public void testTreeWalkShouldntStartIfOneJustRan() throws IOException {
//     String configString = "org.lockss.treewalk.interval=10d";
//     ConfigurationUtil.setCurrentConfigFromString(configString);
//     treeWalkHandler.doTreeWalk();
//     treeWalkHandler.callPollIfNecessary();
//     assertFalse(treeWalkHandler.timeUntilTreeWalkStart() <= 0);
//   }

//   public void testTreeWalkShouldStartIfIntervalElapsed() throws IOException {
//     String configString = "org.lockss.treewalk.interval=100";
//     ConfigurationUtil.setCurrentConfigFromString(configString);
//     treeWalkHandler.doTreeWalk();
//     treeWalkHandler.callPollIfNecessary();

//     TimerUtil.guaranteedSleep(100);

//     long tutws = treeWalkHandler.timeUntilTreeWalkStart();
//     assertTrue(tutws + " > 0", tutws <= 0);
//   }

//   public void testAverageTreeWalkDuration() {
//     // starts with default estimate
//     assertEquals(treeWalkHandler.DEFAULT_TREEWALK_INITIAL_ESTIMATE,
//                  treeWalkHandler.getEstimatedTreeWalkDuration());
//     treeWalkHandler.updateEstimate(100);
//     // first value gets set to MIN_TREEWALK_ESTIMATE, since to small
//     assertEquals(treeWalkHandler.MIN_TREEWALK_ESTIMATE,
//                  treeWalkHandler.getEstimatedTreeWalkDuration());
//     treeWalkHandler.updateEstimate(treeWalkHandler.MIN_TREEWALK_ESTIMATE + 200);
//     // no averaging, so just padded by 1.25
//     long expectedL = (long)((treeWalkHandler.MIN_TREEWALK_ESTIMATE + 200) *
//         treeWalkHandler.DEFAULT_TREEWALK_ESTIMATE_PADDING_MULTIPLIER);
//     assertEquals(expectedL, treeWalkHandler.getEstimatedTreeWalkDuration());
//   }
}
