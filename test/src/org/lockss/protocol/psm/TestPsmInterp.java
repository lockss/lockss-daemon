/*
 * $Id: TestPsmInterp.java,v 1.13 2008-01-27 06:47:10 tlipkis Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.protocol.psm;

import java.io.*;
import java.util.*;

import org.lockss.app.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.util.Queue;
import org.lockss.protocol.*;


public class TestPsmInterp extends LockssTestCase {
  static Logger log = Logger.getLogger("TestPsmInterp");

  // Event classes.  RcvMsg events form hierarchy
  private static class Sched extends PsmEvent {}
  private static class NotSched extends PsmEvent {}
  private static class TaskComplete extends PsmEvent {}
  private static class TaskError extends PsmEvent {}
  private static class Ok extends PsmEvent {}
  private static class SendOk extends PsmEvent {}
  private static class SendNotOk extends PsmEvent {}
  private static class MsgOk extends PsmEvent {}
  private static class MsgDone extends PsmEvent {}
  private static class RcvMsgA extends PsmMsgEvent {}
  private static class RcvMsgA1 extends RcvMsgA {}
  private static class RcvMsgB extends PsmMsgEvent {}
  private static class RcvMsgC extends PsmMsgEvent {}

  // Event instances
  private static PsmEvent Else = PsmEvents.Else;
  private static PsmEvent Timeout = PsmEvents.Timeout;
  private static PsmEvent Sched = new Sched();
  private static PsmEvent NotSched = new NotSched();
  private static PsmEvent TaskComplete = new TaskComplete();
  private static PsmEvent TaskError = new TaskError();
  private static PsmEvent Ok = new Ok();
  private static PsmEvent SendOk = new SendOk();
  private static PsmEvent SendNotOk = new SendNotOk();
  private static PsmEvent MsgOk = new MsgOk();
  private static PsmEvent MsgDone = new MsgDone();

  // messages to go into message events
  private static LcapMessage lmA = makeMsg("msgA");
  private static LcapMessage lmB = makeMsg("msgB");
  private static LcapMessage lmC = makeMsg("msgC");

  private static PsmMsgEvent RcvMsgA = new RcvMsgA().withMessage(lmA);
  private static PsmMsgEvent RcvMsgA1 = new RcvMsgA1().withMessage(lmA);
  private static PsmMsgEvent RcvMsgB = new RcvMsgB().withMessage(lmB);
  private static PsmMsgEvent RcvMsgC = new RcvMsgC().withMessage(lmC);

  private PsmManager psmMgr;
  private SimpleQueue errs = new SimpleQueue.Fifo();

  public void setUp() throws Exception {
    super.setUp();
    initPsmManager();
  }

  public void tearDown() throws Exception {
    if (psmMgr != null) {
      psmMgr.stopService();
    }
    super.tearDown();
  }

  void initPsmManager() {
    psmMgr = getMockLockssDaemon().getPsmManager();
    psmMgr.startService();
  }

  public void testNullConstructorArgs() {
    PsmState[] states = {
      new PsmState("Start", PsmWait.FOREVER, new PsmResponse(Ok, "Start")),
    };
    PsmMachine mach1 = new PsmMachine("Test1", states, "Start");
    try {
      new PsmInterp(null, null);
      fail("null machine should throw");
    } catch (RuntimeException e) { }
    // null userData is ok
    PsmInterp interp = new PsmInterp(mach1, null);
    assertNull(interp.getUserData());
  }

  // Can't call start() twice
  public void testStartTwice() throws PsmException {
    PsmState[] states = {
      new PsmState("Start"),
    };
    PsmMachine mach = new PsmMachine("M1", states, "Start");
    MyInterp interp = new MyInterp(mach, null);
    interp.start();
    try {
      interp.start();
      fail("Second call to start() should throw");
    } catch (IllegalStateException e) {
    }
    try {
      interp.resume(null);
      fail("Resume after start() should throw");
    } catch (IllegalStateException e) {
    }
  }

  // Can't call handleEvent() before start()
  public void testNoStart() throws PsmException {
    PsmState[] states = {
      new PsmState("Start", PsmWait.FOREVER,
		   new PsmResponse(Ok, PsmWait.FOREVER)),
    };
    PsmMachine mach = new PsmMachine("M1", states, "Start");
    MyInterp interp = new MyInterp(mach, null);
    try {
      interp.handleEvent(Ok);
      fail("handleEvent() before start() should throw");
    } catch (IllegalStateException e) {
    }
  }

  // Illegal/unknown resumption state
  public void testIllResume() throws PsmException {
    PsmState[] states = {
      new PsmState("Start"),
    };
    PsmMachine mach = new PsmMachine("M1", states, "Start");
    MyInterp interp;

    interp = new MyInterp(mach, null);
    try {
      interp.resume(null);
      fail("resume(null) should throw NPE");
    } catch (NullPointerException e) {
    }

    interp = new MyInterp(mach, null);
    PsmInterpStateBean state = new PsmInterpStateBean();
    state.setLastResumableStateName(null);
    try {
      interp.resume(state);
      fail("resume(state) should throw on no saved state");
    } catch (PsmException.IllegalResumptionState e) {
    }

    interp = new MyInterp(mach, null);
    state.setLastResumableStateName("not-a-state");
    try {
      interp.resume(state);
      fail("resume(state) should throw on unknown state");
    } catch (PsmException.IllegalResumptionState e) {
    }
  }

  class EH implements PsmInterp.ErrorHandler {
    SimpleQueue errs;
    EH() {
      this.errs = new SimpleQueue.Fifo();
    }
    EH(SimpleQueue errs) {
      this.errs = errs;
    }
    public void handleError(PsmException e) {
      errs.put(e);
    }
    SimpleQueue getErrQueue() {
      return errs;
    }
  }

  // Can't call enqueueStart() twice
  public void testAsynchStartTwice() throws PsmException {
    PsmState[] states = {
      new PsmState("Start"),
    };
    PsmMachine mach = new PsmMachine("M1", states, "Start");
    MyInterp interp = newThreadedInterp(mach, null);
    interp.enqueueStart(new EH());
    try {
      interp.enqueueStart(new EH());
      fail("Second call to start() should throw");
    } catch (IllegalStateException e) {
    }
  }

  // Can't call handleEvent() before start()
  public void testAsynchEventNoStart() throws PsmException {
    PsmState[] states = {
      new PsmState("Start", PsmWait.FOREVER,
		   new PsmResponse(Ok, PsmWait.FOREVER)),
    };
    PsmMachine mach = new PsmMachine("M1", states, "Start");
    MyInterp interp = new MyInterp(mach, null);
    try {
      interp.handleEvent(Ok);
      fail("handleEvent() before start() should throw");
    } catch (IllegalStateException e) {
    }
  }

  // Illegal/unknown resumption state
  public void testAsynchIllResume() throws PsmException {
    PsmState[] states = {
      new PsmState("Start"),
    };
    PsmMachine mach = new PsmMachine("M1", states, "Start");
    MyInterp interp;

    interp = newThreadedInterp(mach, null);
    try {
      interp.enqueueResume(null, new EH(errs));
      fail("resume(null) should throw NPE");
    } catch (NullPointerException e) {
    }

    interp = newThreadedInterp(mach, null);
    PsmInterpStateBean state = new PsmInterpStateBean();
    state.setLastResumableStateName(null);
    try {
      interp.enqueueResume(state, new EH(errs));
      fail("resume(state) should throw on no saved state");
    } catch (PsmException.IllegalResumptionState e) {
    }

    interp = newThreadedInterp(mach, null);
    state.setLastResumableStateName("not-a-state");
    try {
      interp.enqueueResume(state, new EH(errs));
      fail("resume(state) should throw on unknown state");
    } catch (PsmException.IllegalResumptionState e) {
    }
  }

  // Loop in Start state
  public void testLoopStart() throws PsmException {
    PsmState[] statesLoopStart = {
      new PsmState("Start", new MyAction(Sched),
		   new PsmResponse(Sched, new MyAction(Sched))),
    };
    PsmMachine mach = new PsmMachine("M1", statesLoopStart, "Start");
    MyInterp interp = new MyInterp(mach, null);
    try {
      interp.start();
      fail("Should throw if state loop");
    } catch (PsmException.MaxChainedEvents e) {
    }
  }

  // Loop between two states, in start()
  public void testLoopTwo() throws PsmException {
    PsmState[] statesLoopTwo = {
      new PsmState("Start", new MyAction(Sched),
		   new PsmResponse(Sched, "two")),
      new PsmState("two", new MyAction(SendOk),
		   new PsmResponse(SendOk, "Start")),
    };
    PsmMachine mach = new PsmMachine("M1", statesLoopTwo, "Start");
    PsmInterp interp = new MyInterp(mach, null);
    try {
      interp.start();
      fail("Should throw if state loop");
    } catch (PsmException.MaxChainedEvents e) {
    }
  }

  // Loop between two states, in handleEvent()
  public void testLoopHandle() throws PsmException {
    PsmState[] statesLoopTwo = {
      new PsmState("Start", PsmWait.FOREVER,
		   new PsmResponse(Sched, "one")),
      new PsmState("one", new MyAction(SendOk),
		   new PsmResponse(SendOk, "two")),
      new PsmState("two", new MyAction(SendOk),
		   new PsmResponse(SendOk, "one")),
    };
    PsmMachine mach = new PsmMachine("M1", statesLoopTwo, "Start");
    PsmInterp interp = new MyInterp(mach, null);
    interp.start();
    try {
      interp.handleEvent(Sched);
      fail("Should throw if state loop");
    } catch (PsmException.MaxChainedEvents e) {
    }
  }

  MyInterp newThreadedInterp(PsmMachine mach, Object userData) {
    MyInterp interp = new MyInterp(psmMgr, mach, userData);
    interp.setThreaded(true);
    return interp;
  }

  // Loop in Start state
  public void testAsynchLoopStart() throws PsmException {
    PsmState[] statesLoopStart = {
      new PsmState("Start", new MyAction(Sched),
		   new PsmResponse(Sched, new MyAction(Sched))),
    };
    PsmMachine mach = new PsmMachine("M1", statesLoopStart, "Start");
    MyInterp interp = newThreadedInterp(mach, null);
    interp.enqueueStart(new EH(errs));
    assertTrue(errs.get(TIMEOUT_SHOULDNT)
	       instanceof PsmException.MaxChainedEvents);
  }

  // Loop between two states, in start()
  public void testAsynchLoopTwo() throws PsmException {
    PsmState[] statesLoopTwo = {
      new PsmState("Start", new MyAction(Sched),
		   new PsmResponse(Sched, "two")),
      new PsmState("two", new MyAction(SendOk),
		   new PsmResponse(SendOk, "Start")),
    };
    PsmMachine mach = new PsmMachine("M1", statesLoopTwo, "Start");
    MyInterp interp = newThreadedInterp(mach, null);
    interp.enqueueStart(new EH(errs));
    assertTrue(errs.get(TIMEOUT_SHOULDNT)
	       instanceof PsmException.MaxChainedEvents);
  }

  // Loop between two states, in handleEvent()
  public void testAsynchLoopHandle() throws PsmException {
    PsmState[] statesLoopTwo = {
      new PsmState("Start", PsmWait.FOREVER,
		   new PsmResponse(Sched, "one")),
      new PsmState("one", new MyAction(SendOk),
		   new PsmResponse(SendOk, "two")),
      new PsmState("two", new MyAction(SendOk),
		   new PsmResponse(SendOk, "one")),
    };
    PsmMachine mach = new PsmMachine("M1", statesLoopTwo, "Start");
    MyInterp interp = newThreadedInterp(mach, null);
    interp.enqueueStart(new EH(errs));
    assertTrue(errs.isEmpty());
    interp.enqueueEvent(Sched, new EH(errs));
    assertTrue(errs.get(TIMEOUT_SHOULDNT)
	       instanceof PsmException.MaxChainedEvents);
  }

  // RuntimeException in action should become PsmException.ActionError
  public void testActionError() throws PsmException {
    PsmState[] states = {
      new PsmState("Start", new ThrowAction(new RuntimeException("abcd"))),
    };
    PsmMachine mach = new PsmMachine("M1", states, "Start");
    MyInterp interp = new MyInterp(mach, null);
    try {
      interp.start();
      fail("Interp should throw if action throws");
    } catch (PsmException.ActionError e) {
      assertEquals("abcd", e.getCause().getMessage());
    }
  }

  PsmAction nullAction = new PsmAction() {
      public PsmEvent run(PsmEvent event, PsmInterp interp) {
	return null;
      }};

  // Actions are not allowed to return a null event
  public void testNullEntryAction() throws PsmException {
    PsmState[] states = {
      new PsmState("Start", nullAction),
    };
    PsmMachine mach = new PsmMachine("M1", states, "Start");
    MyInterp interp = new MyInterp(mach, null);
    try {
      interp.start();
      fail("Interp should throw if action returns null");
    } catch (PsmException.NullEvent e) {
    }
  }

  public void testNullRespAction() throws PsmException {
    PsmState[] states = {
      new PsmState("Start", PsmWait.FOREVER,
		   new PsmResponse(Else, nullAction)),
    };
    PsmMachine mach = new PsmMachine("M1", states, "Start");
    MyInterp interp = new MyInterp(mach, null);
    interp.start();
    try {
      interp.handleEvent(Ok);
      fail("Interp should throw if action returns null");
    } catch (PsmException.NullEvent e) {
    }
  }

  PsmAction illWaitAction = new PsmAction() {
      public PsmEvent run(PsmEvent event, PsmInterp interp) {
	return new PsmWaitEvent();
      }};

  // Normal actions are not allowed to return a wait event
  public void testIllWaitEvent() throws PsmException {
    PsmState[] states = {
      new PsmState("Start", illWaitAction),
    };
    PsmMachine mach = new PsmMachine("M1", states, "Start");
    MyInterp interp = new MyInterp(mach, null);
    try {
      interp.start();
      fail("Interp should throw if non-wait action returns wait event");
    } catch (PsmException.IllegalEvent e) {
    }
  }

  // Simple state machine for next few tests
  PsmState[] states1 = {
    new PsmState("Start", new MyAction(Sched),
		 new PsmResponse(Sched, PsmWait.FOREVER),
		 new PsmResponse(NotSched, "Error"),
		 new PsmResponse(TaskComplete, "Send"),
		 new PsmResponse(TaskError, "Error")),
    new PsmState("Send", new MyAction(SendOk),
		 new PsmResponse(SendOk, "WaitVote"),
		 new PsmResponse(Else, "Error")),
    new PsmState("WaitVote", PsmWait.FOREVER,
		 new PsmResponse(RcvMsgA, "Done"),
		 new PsmResponse(Else, "Error")),
    new PsmState("Error"),
    new PsmState("Done"),
  };

  // Next few tests record all interpreter events (action, transitions,
  // etc) using eventMonitor hook then check against expected events.

  public void testSimple1() throws PsmException {
    PsmMachine mach = new PsmMachine("M1", states1, "Start");
    MyInterp interp = new MyInterp(mach, null);
    assertFalse(interp.isFinalState());
    interp.start();
    assertFalse(interp.isFinalState());
    ER[] exp1 = {
      new ER(null, PsmEvents.Start, null, states1[0]),
      new ER(states1[0], PsmEvents.Start, states1[0].getEntryAction(), null),
      new ER(states1[0], Sched, null, null),
    };
    assertIsomorphic(exp1, interp.events);
    interp.clear();
    interp.handleEvent(NotSched);
    ER[] exp2 = {
      new ER(states1[0], NotSched, null, states1[3]),
      new ER(states1[3], NotSched, null, null),
    };
    assertIsomorphic(exp2, interp.events);
  }

  public void testSimple2() throws PsmException {
    PsmMachine mach = new PsmMachine("M1", states1, "Start");
    MyInterp interp = new MyInterp(mach, null);
    interp.start();
    ER[] exp1 = {
      new ER(null, PsmEvents.Start, null, states1[0]),
      new ER(states1[0], PsmEvents.Start, states1[0].getEntryAction(), null),
      new ER(states1[0], Sched, null, null),
    };
    assertIsomorphic(exp1, interp.events);
    interp.clear();
    interp.handleEvent(Sched);
    ER[] exp2 = {
      new ER(states1[0], Sched, null, null),
    };
    assertIsomorphic(exp2, interp.events);
    interp.handleEvent(TaskComplete);
    ER[] exp3 = {
      new ER(states1[0], Sched, null, null),
      new ER(states1[0], TaskComplete, null, states1[1]),
      new ER(states1[1], TaskComplete, states1[1].getEntryAction(), null),
      new ER(states1[1], SendOk, null, states1[2]),
      new ER(states1[2], SendOk, null, null),
    };
    assertIsomorphic(exp3, interp.events);
  }

  public void testAsynchSimple1() throws Exception {
    PsmMachine mach = new PsmMachine("M1", states1, "Start");
    MyInterp interp = newThreadedInterp(mach, null);
    assertFalse(interp.isFinalState());
    interp.enqueueStart(new EH(errs));
    assertTrue(interp.waitIdle(TIMEOUT_SHOULDNT));
    assertFalse(interp.isFinalState());
    ER[] exp1 = {
      new ER(null, PsmEvents.Start, null, states1[0]),
      new ER(states1[0], PsmEvents.Start, states1[0].getEntryAction(), null),
      new ER(states1[0], Sched, null, null),
    };
    assertIsomorphic(exp1, interp.events);
    interp.clear();
    interp.enqueueEvent(NotSched, new EH(errs));
    assertTrue(interp.waitIdle(TIMEOUT_SHOULDNT));
    ER[] exp2 = {
      new ER(states1[0], NotSched, null, states1[3]),
      new ER(states1[3], NotSched, null, null),
    };
    assertIsomorphic(exp2, interp.events);
    assertTrue(errs.isEmpty());
  }

  boolean actionTriggered = false;

  public void testAsynchSimple2() throws Exception {
    PsmMachine mach = new PsmMachine("M1", states1, "Start");
    MyInterp interp = newThreadedInterp(mach, null);
    interp.enqueueStart(new EH(errs));
    assertTrue(interp.waitIdle(TIMEOUT_SHOULDNT));
    ER[] exp1 = {
      new ER(null, PsmEvents.Start, null, states1[0]),
      new ER(states1[0], PsmEvents.Start, states1[0].getEntryAction(), null),
      new ER(states1[0], Sched, null, null),
    };
    assertIsomorphic(exp1, interp.events);
    interp.clear();
    interp.enqueueEvent(Sched, new EH(errs));
    assertTrue(interp.waitIdle(TIMEOUT_SHOULDNT));
    ER[] exp2 = {
      new ER(states1[0], Sched, null, null),
    };
    assertIsomorphic(exp2, interp.events);
    assertFalse(actionTriggered);
    interp.enqueueEvent(TaskComplete, new EH(errs),
			new PsmInterp.Action() {
			  public void eval() {
			    actionTriggered = true;
			  }});
    assertTrue(interp.waitIdle(TIMEOUT_SHOULDNT));
    assertTrue(actionTriggered);
    ER[] exp3 = {
      new ER(states1[0], Sched, null, null),
      new ER(states1[0], TaskComplete, null, states1[1]),
      new ER(states1[1], TaskComplete, states1[1].getEntryAction(), null),
      new ER(states1[1], SendOk, null, states1[2]),
      new ER(states1[2], SendOk, null, null),
    };
    assertIsomorphic(exp3, interp.events);
    assertTrue(errs.isEmpty());
  }

  class MyCheckpointer implements PsmInterp.Checkpointer {
    PsmInterpStateBean lastBean = null;
    int cnt = 0;
    public void checkpoint(PsmInterpStateBean resumeStateBean) {
      lastBean = resumeStateBean;
      cnt++;
    }
  }

  // Simple state machine for checkpoint/resume tests
  PsmState[] statesCheck = {
    new PsmState("Start", PsmWait.FOREVER,
		 new PsmResponse(Sched, "Yes")),
    new PsmState("Yes", PsmWait.FOREVER,
		 new PsmResponse(Sched, "No")).setResumable(true),
    new PsmState("No", PsmWait.FOREVER,
		 new PsmResponse(Sched, "Yes")),
    new PsmState("Done"),
  };

  public void testCheckpoint() throws PsmException {
    PsmMachine mach = new PsmMachine("M1", statesCheck, "Start");
    MyInterp interp = new MyInterp(mach, null);
    assertEmpty(interp.events);
    MyCheckpointer cptr = new MyCheckpointer();
    interp.setCheckpointer(cptr);
    interp.start();
    ER[] exp1 = {
      new ER(null, PsmEvents.Start, null, statesCheck[0]),
      new ER(statesCheck[0], PsmEvents.Start, null, null),
    };
    assertIsomorphic(exp1, interp.events);
    // no resumable states yet
    assertEquals(0, cptr.cnt);
    interp.clear();
    interp.handleEvent(Sched);
    ER[] exp2 = {
      new ER(statesCheck[0], Sched, null, statesCheck[1]),
      new ER(statesCheck[1], Sched, null, null),
    };
    assertIsomorphic(exp2, interp.events);
    // resumable state Yes entered
    assertEquals(1, cptr.cnt);
    assertEquals("Yes", cptr.lastBean.getLastResumableStateName());
    interp.clear();
    interp.handleEvent(Sched);
    ER[] exp3 = {
      new ER(statesCheck[1], Sched, null, statesCheck[2]),
      new ER(statesCheck[2], Sched, null, null),
    };
    assertIsomorphic(exp3, interp.events);
    // resumable state Yes reentered with no other intervening resumable state
    assertEquals(1, cptr.cnt);
    assertEquals("Yes", cptr.lastBean.getLastResumableStateName());
  }

  public void testResume() throws PsmException {
    PsmMachine mach = new PsmMachine("M1", statesCheck, "Start");
    MyInterp interp = new MyInterp(mach, null);
    assertEmpty(interp.events);
    MyCheckpointer cptr = new MyCheckpointer();
    interp.setCheckpointer(cptr);
    interp.start();
    interp.handleEvent(Sched);
    interp.handleEvent(Sched);
    // Should be in state No, with last resumable state Yes
    ER[] exp1 = {
      new ER(null, PsmEvents.Start, null, statesCheck[0]),
      new ER(statesCheck[0], PsmEvents.Start, null, null),
      new ER(statesCheck[0], Sched, null, statesCheck[1]),
      new ER(statesCheck[1], Sched, null, null),
      new ER(statesCheck[1], Sched, null, statesCheck[2]),
      new ER(statesCheck[2], Sched, null, null),
    };
    assertIsomorphic(exp1, interp.events);
    assertEquals(1, cptr.cnt);
    PsmInterpStateBean resumeBean = cptr.lastBean;
    assertEquals("Yes", resumeBean.getLastResumableStateName());

    PsmMachine mach2 = new PsmMachine("M1", statesCheck, "Start");
    MyInterp interp2 = new MyInterp(mach, null);

    interp2.resume(resumeBean);
    ER[] exp2 = {
      new ER(null, PsmEvents.Resume, null, statesCheck[1]),
      new ER(statesCheck[1], PsmEvents.Resume, null, null),
    };
    assertIsomorphic(exp2, interp2.events);

    try {
      interp2.resume(null);
      fail("Resume after resume() should throw");
    } catch (IllegalStateException e) {
    }
  }

  public void testAsynchCheckpoint() throws Exception {
    PsmMachine mach = new PsmMachine("M1", statesCheck, "Start");
    MyInterp interp = newThreadedInterp(mach, null);
    assertEmpty(interp.events);
    MyCheckpointer cptr = new MyCheckpointer();
    interp.setCheckpointer(cptr);
    interp.enqueueStart(new EH(errs));
    assertTrue(interp.waitIdle(TIMEOUT_SHOULDNT));
    ER[] exp1 = {
      new ER(null, PsmEvents.Start, null, statesCheck[0]),
      new ER(statesCheck[0], PsmEvents.Start, null, null),
    };
    assertIsomorphic(exp1, interp.events);
    // no resumable states yet
    assertEquals(0, cptr.cnt);
    interp.clear();
    interp.enqueueEvent(Sched, new EH(errs));
    assertTrue(interp.waitIdle(TIMEOUT_SHOULDNT));
    ER[] exp2 = {
      new ER(statesCheck[0], Sched, null, statesCheck[1]),
      new ER(statesCheck[1], Sched, null, null),
    };
    assertIsomorphic(exp2, interp.events);
    // resumable state Yes entered
    assertEquals(1, cptr.cnt);
    assertEquals("Yes", cptr.lastBean.getLastResumableStateName());
    interp.clear();
    interp.enqueueEvent(Sched, new EH(errs));
    assertTrue(interp.waitIdle(TIMEOUT_SHOULDNT));
    ER[] exp3 = {
      new ER(statesCheck[1], Sched, null, statesCheck[2]),
      new ER(statesCheck[2], Sched, null, null),
    };
    assertIsomorphic(exp3, interp.events);
    // resumable state Yes reentered with no other intervening resumable state
    assertEquals(1, cptr.cnt);
    assertEquals("Yes", cptr.lastBean.getLastResumableStateName());
    assertTrue(errs.isEmpty());
  }

  public void testAsynchResume() throws Exception {
    PsmMachine mach = new PsmMachine("M1", statesCheck, "Start");
    MyInterp interp = newThreadedInterp(mach, null);
    assertEmpty(interp.events);
    MyCheckpointer cptr = new MyCheckpointer();
    interp.setCheckpointer(cptr);
    interp.enqueueStart(new EH(errs));
    interp.enqueueEvent(Sched, new EH(errs));
    interp.enqueueEvent(Sched, new EH(errs));

    ER[] exp1 = {
      new ER(null, PsmEvents.Start, null, statesCheck[0]),
      new ER(statesCheck[0], PsmEvents.Start, null, null),
      new ER(statesCheck[0], Sched, null, statesCheck[1]),
      new ER(statesCheck[1], Sched, null, null),
      new ER(statesCheck[1], Sched, null, statesCheck[2]),
      new ER(statesCheck[2], Sched, null, null),
    };
    // Should get to state No, with last resumable state Yes
    while (!CollectionUtil.isIsomorphic(exp1, interp.events)) {
      assertTrue(interp.waitIdle(TIMEOUT_SHOULDNT));
    }
    assertIsomorphic(exp1, interp.events);
    assertEquals(1, cptr.cnt);
    PsmInterpStateBean resumeBean = cptr.lastBean;
    assertEquals("Yes", resumeBean.getLastResumableStateName());

    MyInterp interp2 = newThreadedInterp(mach, null);

    interp2.enqueueResume(resumeBean, new EH(errs));
    assertTrue(interp2.waitIdle(TIMEOUT_SHOULDNT));
    ER[] exp2 = {
      new ER(null, PsmEvents.Resume, null, statesCheck[1]),
      new ER(statesCheck[1], PsmEvents.Resume, null, null),
    };
    assertIsomorphic(exp2, interp2.events);
    assertTrue(errs.isEmpty());

    try {
      interp2.enqueueResume(resumeBean, new EH(errs));
      fail("Resume after resume() should throw");
    } catch (IllegalStateException e) {
    }
  }

  PsmState[] states2 = {
    new PsmState("Start", new MyAction(SendOk),
		 new PsmResponse(SendOk, "WaitVote"),
		 new PsmResponse(Else, "Error")),
    new PsmState("WaitVote", PsmWait.FOREVER,
		 new PsmResponse(RcvMsgA, "WaitVote"),
		 new PsmResponse(RcvMsgB, "Done"),
		 new PsmResponse(Else, "Error")),
    new PsmState("Done"),
    new PsmState("Error"),
  };

  // Feed interpreter events from list until it reaches final state
  void runEvents(PsmInterp interp, List events) throws PsmException {
    interp.start();
    while (!interp.isFinalState()){
      interp.handleEvent((PsmEvent)events.remove(0));
    }
  }

  // Feed interpreter all events from list
  void runAsynchEvents(PsmInterp interp, List events) throws PsmException {
    interp.enqueueStart(new EH(errs));
    while (!events.isEmpty()){
      interp.enqueueEvent((PsmEvent)events.remove(0), new EH(errs));
    }
  }

  // Run states2 machine until final state through two paths, check
  // interpreter events

  public void testUntilFinal() throws PsmException {
    PsmMachine mach = new PsmMachine("M1", states2, "Start");
    MyInterp interp = new MyInterp(mach, null);
    runEvents(interp, ListUtil.list(RcvMsgA, RcvMsgA, RcvMsgB));
    ER[] exp1 = {
      new ER(null, PsmEvents.Start, null, states2[0]),
      new ER(states2[0], PsmEvents.Start, states2[0].getEntryAction(), null),
      new ER(states2[0], SendOk, null, states2[1]),
      new ER(states2[1], SendOk, null, null),
      new ER(states2[1], RcvMsgA, null, states2[1]),
      new ER(states2[1], RcvMsgA, null, null),
      new ER(states2[1], RcvMsgA, null, states2[1]),
      new ER(states2[1], RcvMsgA, null, null),
      new ER(states2[1], RcvMsgB, null, states2[2]),
      new ER(states2[2], RcvMsgB, null, null),
    };
    assertIsomorphic(exp1, interp.events);
  }

  public void testUntilFinalError() throws PsmException {
    PsmMachine mach = new PsmMachine("M1", states2, "Start");
    MyInterp interp = new MyInterp(mach, null);
    runEvents(interp, ListUtil.list(RcvMsgA, RcvMsgA, RcvMsgC));
    ER[] exp1 = {
      new ER(null, PsmEvents.Start, null, states2[0]),
      new ER(states2[0], PsmEvents.Start, states2[0].getEntryAction(), null),
      new ER(states2[0], SendOk, null, states2[1]),
      new ER(states2[1], SendOk, null, null),
      new ER(states2[1], RcvMsgA, null, states2[1]),
      new ER(states2[1], RcvMsgA, null, null),
      new ER(states2[1], RcvMsgA, null, states2[1]),
      new ER(states2[1], RcvMsgA, null, null),
      new ER(states2[1], RcvMsgC, null, states2[3]),
      new ER(states2[3], RcvMsgC, null, null),
    };
    assertIsomorphic(exp1, interp.events);
  }

  public void testAsynchUntilFinal() throws Exception {
    PsmMachine mach = new PsmMachine("M1", states2, "Start");
    MyInterp interp = newThreadedInterp(mach, null);
    runAsynchEvents(interp, ListUtil.list(RcvMsgA, RcvMsgA, RcvMsgB));
    assertTrue(interp.waitFinal(TIMEOUT_SHOULDNT));
    ER[] exp1 = {
      new ER(null, PsmEvents.Start, null, states2[0]),
      new ER(states2[0], PsmEvents.Start, states2[0].getEntryAction(), null),
      new ER(states2[0], SendOk, null, states2[1]),
      new ER(states2[1], SendOk, null, null),
      new ER(states2[1], RcvMsgA, null, states2[1]),
      new ER(states2[1], RcvMsgA, null, null),
      new ER(states2[1], RcvMsgA, null, states2[1]),
      new ER(states2[1], RcvMsgA, null, null),
      new ER(states2[1], RcvMsgB, null, states2[2]),
      new ER(states2[2], RcvMsgB, null, null),
    };
    assertIsomorphic(exp1, interp.events);
    assertTrue(errs.isEmpty());
  }

  public void testAsynchUntilFinalError() throws Exception {
    PsmMachine mach = new PsmMachine("M1", states2, "Start");
    MyInterp interp = newThreadedInterp(mach, null);
    runAsynchEvents(interp, ListUtil.list(RcvMsgA, RcvMsgA, RcvMsgC));
    assertTrue(interp.waitFinal(TIMEOUT_SHOULDNT));
    ER[] exp1 = {
      new ER(null, PsmEvents.Start, null, states2[0]),
      new ER(states2[0], PsmEvents.Start, states2[0].getEntryAction(), null),
      new ER(states2[0], SendOk, null, states2[1]),
      new ER(states2[1], SendOk, null, null),
      new ER(states2[1], RcvMsgA, null, states2[1]),
      new ER(states2[1], RcvMsgA, null, null),
      new ER(states2[1], RcvMsgA, null, states2[1]),
      new ER(states2[1], RcvMsgA, null, null),
      new ER(states2[1], RcvMsgC, null, states2[3]),
      new ER(states2[3], RcvMsgC, null, null),
    };
    assertIsomorphic(exp1, interp.events);
    assertTrue(errs.isEmpty());
  }

  // A machine with some message actions, which record the message received
  PsmState[] states3 = {
    new PsmState("Start", new MyAction(SendOk),
		 new PsmResponse(SendOk, "WaitVote"),
		 new PsmResponse(Else, "Error")),
    new PsmState("WaitVote", PsmWait.FOREVER,
		 new PsmResponse(RcvMsgA, new MyMsgAction(MsgOk)),
		 new PsmResponse(RcvMsgB, new MyMsgAction(MsgDone)),
		 new PsmResponse(MsgOk, PsmWait.FOREVER),
		 new PsmResponse(MsgDone, "Done"),
		 new PsmResponse(Else, "Error")),
    new PsmState("Done"),
    new PsmState("Error"),
  };

  // Feed the machine message events, check that message action received
  // the proper messages.
  public void testRcvMsg() throws PsmException {
    PsmMachine mach = new PsmMachine("M1", states3, "Start");
    final List msgs = new ArrayList();
    MyInterp interp = new MyInterp(mach, new MessageRecorder() {
	public void record(LcapMessage msg) { msgs.add(msg); }});
    runEvents(interp, ListUtil.list(RcvMsgA, RcvMsgA, RcvMsgB));
    assertEquals(ListUtil.list(lmA, lmA, lmB), msgs);
  }

  // State machine to test simple timeout
  PsmState[] statesTime = {
    new PsmState("Start", new PsmWait(100),
		 new PsmResponse(Timeout, "Time"),
		 new PsmResponse(Else, "Error")),
    new PsmState("Time").succeed(),
    new PsmState("Error").fail(),
  };

  public void testSimpleTimeout() throws PsmException {
    PsmMachine mach = new PsmMachine("M1", statesTime, "Start");
    MyInterp interp = new MyInterp(mach, null);
    interp.start();
    // This machine doesn't need any more outside events to finish, so just
    // wait for it
    Interrupter intr = null;
    try {
      intr = interruptMeIn(Math.max(TIMEOUT_SHOULDNT, 10 * Constants.SECOND),
				    true);
      while (!interp.isFinalState()) {
	TimerUtil.sleep(10);
      }
      intr.cancel();
      assertEquals("Time", interp.getFinalState().getName());
      assertEquals(ListUtil.list("Start", "Time"), interp.states);
      long delta = interp.getStateTime(1) - interp.getStateTime(0);
      assertTrue("timeout occurred early in " + delta, delta >= 100);
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	fail("testSimpleTimeout machine didn't reach final state");
      }
    }
  }

  // A more functional test.  Creates a user object in which actions track
  // their progress.  Schedules a "computation" using the TimerQueue to
  // generate the TaskComplete event.  Because the "computation" runs in
  // another thread, it might finish immediately, before the action that
  // started it continues running.  This will affect the order of user
  // evants but should have no effect on the state machine.  Force this
  // situation with artificial delay to ensure it occurs in test.

  // User objext tracks user events
  static class TestObj {
    long computeTime = 0;
    long timeout = 0;
    long delay = 0;
    List userEvents = new ArrayList();
    void event(String event) {
      userEvents.add(event);
    }
  }

  // Timer callback records user event, generates TaskComplete event
  TimerQueue.Callback tcb4 = new TimerQueue.Callback() {
      public void timerExpired(Object cookie) {
	PsmInterp interp = (PsmInterp)cookie;
	TestObj obj = (TestObj)interp.getUserData();
	obj.event("taskcomplete");
	try {
	  interp.handleEvent(TaskComplete);
	} catch (PsmException e) {
	  log.warning("Unexpected error", e);
	}
      }};

  // Action schedules "computation", returns success (Sched event)
  PsmAction schedAction = new PsmAction() {
      public PsmEvent run(PsmEvent event, PsmInterp interp) {
	TestObj obj = (TestObj)interp.getUserData();
	// start a state timeout if requested
	// schedule a computation if requested, else signal NotSched event
	if (obj.computeTime != 0) {
	  TimerQueue.schedule(Deadline.in(obj.computeTime), tcb4, interp);
	  if (obj.delay != 0) {
	    // allow "computation" in timer thread to finish before proceding,
	    // to force user events to occur in unexpected order
	    TimerUtil.guaranteedSleep(obj.delay);
	  }
	  obj.event("sched");
	  if (obj.timeout != 0) {
	    return Sched.withUserVal(obj.timeout);
	  } else {
	    return Sched;
	  }
	} else {
	  obj.event("notsched");
	  return NotSched;
	}
      }};

  // Done action records user event
  PsmAction doneAction = new PsmAction() {
      public PsmEvent run(PsmEvent event, PsmInterp interp) {
	TestObj obj = (TestObj)interp.getUserData();
	obj.event("done");
	return Ok;
      }};

  // State machine to schedule then wait for a computation
  PsmState[] states4 = {
    new PsmState("Start", schedAction,
		 new PsmResponse(Sched, "WaitCompute"),
		 new PsmResponse(Else, "Error")),
    new PsmState("WaitCompute", PsmWait.TIMEOUT_IN_TRIGGER,
		 new PsmResponse(TaskComplete, "AlmostDone"),
		 new PsmResponse(Timeout, "GiveUp"),
		 new PsmResponse(Else, "Error")),
    new PsmState("AlmostDone", doneAction,
		 new PsmResponse(Else, "Done")),
    new PsmState("Done").succeed(),
    new PsmState("Error").fail(),
    new PsmState("GiveUp").fail(),
  };

  public void testCallback(long computeTime, long delay) throws PsmException {
    PsmMachine mach = new PsmMachine("M1", states4, "Start");
    TestObj obj = new TestObj();
    obj.computeTime = computeTime;
    obj.delay = delay;
    MyInterp interp = new MyInterp(mach, obj);
    interp.start();
    // This machine doesn't need any more outside events to finish, so just
    // wait for it
    Interrupter intr = null;
    try {
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      while (!interp.isFinalState()) {
	TimerUtil.sleep(10);
      }
      // user event "taskcomplete" happens in timer thread, could happen
      // before "sched"
      List okOrders =
	ListUtil.list(ListUtil.list("sched", "taskcomplete", "done"),
		      ListUtil.list("taskcomplete", "sched", "done"));
      assertTrue("expected one of:<"+okOrders+"> but was:<"+obj.userEvents+">",
		 okOrders.contains(obj.userEvents));
      // but state order should always be the same
      assertEquals(ListUtil.list("Start", "WaitCompute", "AlmostDone", "Done"),
		   interp.states);

      assertTrue(interp.getFinalState().isSucceed());
      assertEquals("Done", interp.getFinalState().getName());
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	fail("testCallback machine didn't reach final state");
      }
    }
  }

  public void testCallback() throws PsmException {
    // normal task
    testCallback(100, 0);
    // quick task
    testCallback(1, 0);
    // normal task probably completes after task sched completes
    testCallback(100, 10);
    // short task completes before task sched completes
    testCallback(1, 10);
  }

  public void testTimeout(long timeout, long computeTime, long delay,
			  List okEvents, List expectedStates) throws PsmException {
    PsmMachine mach = new PsmMachine("M1", states4, "Start");
    TestObj obj = new TestObj();
    obj.timeout = timeout;
    obj.computeTime = computeTime;
    obj.delay = delay;
    MyInterp interp = new MyInterp(mach, obj);
    interp.start();
    // This machine doesn't need any more outside events to finish, so just
    // wait for it
    Interrupter intr = null;
    try {
      intr = interruptMeIn(Math.max(TIMEOUT_SHOULDNT, 10 * Constants.SECOND),
				    true);
      while (!interp.isFinalState()) {
	TimerUtil.sleep(10);
      }
      intr.cancel();
      assertTrue("expected one of:<"+okEvents+"> but was:<"+obj.userEvents+">",
		 okEvents.contains(obj.userEvents));
      assertEquals(expectedStates, interp.states);
      assertEquals(expectedStates.get(expectedStates.size()-1),
		   interp.getFinalState().getName());
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	log.error("States: " + interp.states);
	log.error("User events: " + obj.userEvents);
	fail("testTimeout machine didn't reach final state");
      }
    }
  }

  public void testTimeout() throws PsmException {
    List timeoutStates =
      ListUtil.list("Start", "WaitCompute", "GiveUp");
    List timeoutEvents =
      ListUtil.list(ListUtil.list(/*"set timeout", */"sched"),
		    ListUtil.list(/*"set timeout", */"sched", "taskcomplete"));
    List noTimeoutStates =
      ListUtil.list("Start", "WaitCompute", "AlmostDone", "Done");
    List noTimeoutEvents =
      ListUtil.list(ListUtil.list(/*"set timeout", */"sched",
				  "taskcomplete", "done"),
		    ListUtil.list(/*"set timeout", */"taskcomplete",
				  "sched", "done"));
    // timeout occurs
    testTimeout(100, 5000, 0, timeoutEvents, timeoutStates);
    // timeout occurs before sched action returns
    testTimeout(1, 50, 10, timeoutEvents, timeoutStates);
    // timeout doesn't occur
    testTimeout(5000, 10, 0, noTimeoutEvents, noTimeoutStates);
    // timeout doesn't, occur, task completes before task sched completes
    testTimeout(5000, 1, 10, noTimeoutEvents, noTimeoutStates);
  }


  // Utility classes for everything above

  /** Event Record, records info passed to interpreter's eventMonitor
   * hook */
  static class ER {
    PsmState curState;
    PsmEvent event;
    PsmAction action;
    PsmState newState;

    ER(PsmState curState, PsmEvent event,
       PsmAction action, PsmState newState) {
      this.curState = curState;
      this.event = event;
      this.action = action;
      this.newState = newState;
    }

    public boolean equals(Object obj) {
      if (!(obj instanceof ER)) return false;
      ER o = (ER)obj;
      return
	(curState == null ? o.curState == null : curState.equals(o.curState))&&
	(event == null ? o.event == null : event.equals(o.event)) &&
	(action == null ? o.action == null : action.equals(o.action) )&&
	(newState == null ? o.newState == null : newState.equals(o.newState));
    }

    public String toString() {
      return ListUtil.list(curState, event, action, newState).toString();
    }
  }

  /** An action that does nothing by signal (return) events from a
   * predetermined list */
  static class MyAction extends PsmAction {
    List rets;
    PsmEvent lastEvent;

    MyAction(PsmEvent retEvent) {
      rets = ListUtil.list(retEvent);
    }
    public PsmEvent run(PsmEvent event, PsmInterp interp) {
      if (rets.isEmpty()) return lastEvent;
      return (lastEvent = (PsmEvent)rets.remove(0));
    }
  }

  /** A message action that calls a MessageRecorder to record the message
   * received, and returns predetermined events.  (It's easier for the user
   * to pass in a MessageRecorder that records on his data structure than
   * it is to find the instance of this action and extract the record from
   * it.) */
  static class MyMsgAction extends PsmMsgAction {
    List rets;
    PsmEvent lastEvent;

    MyMsgAction(PsmEvent retEvent) {
      rets = ListUtil.list(retEvent);
    }
    public PsmEvent runMsg(PsmMsgEvent event, PsmInterp interp) {
      Object obj = interp.getUserData();
      if (obj instanceof MessageRecorder) {
	((MessageRecorder)obj).record(event.getMessage());
      }
      if (rets.isEmpty()) return lastEvent;
      return (lastEvent = (PsmEvent)rets.remove(0));
    }
  }

  interface MessageRecorder {
    void record(LcapMessage msg);
  }

  /** An action that throws */
  static class ThrowAction extends PsmAction {
    RuntimeException e;

    ThrowAction(RuntimeException e) {
      this.e = e;
    }
    public PsmEvent run(PsmEvent event, PsmInterp interp) {
      throw e;
    }
  }

  /** Implements the eventMonitor hook of PsmInterp, records REs on local
   * list */
  static class MyInterp extends PsmInterp {
    List events = new ArrayList();
    List states = new ArrayList();
    List stateTimes = new ArrayList();

    public MyInterp(PsmMachine stateMachine, Object userData) {
      super(stateMachine, userData);
    }

    public MyInterp(PsmManager mgr, PsmMachine stateMachine, Object userData) {
      super(mgr, stateMachine, userData);
    }

    protected void eventMonitor(PsmState curState, PsmEvent event,
				PsmAction action, PsmState newState) {
      if (action != null && action.isWaitAction()) {
	// report wait events as null action
	action = null;
      }
      events.add(new ER(curState, event, action, newState));
      if (newState != null) {
	states.add(newState.getName());
	stateTimes.add(new Long(TimeBase.nowMs()));
      }
    }

    public void clear() {
      events.clear();
    }

    long getStateTime(int ix) {
      return ((Long)stateTimes.get(ix)).longValue();
    }
  }

  // Hack to make a simple LcapMessage, don't care about contents
  static MyLcapMessage makeMsg(String name) {
    try {
      return new MyLcapMessage(name);
    } catch (IOException e) {
      throw new RuntimeException(e.toString());
    }
  }
  static class MyLcapMessage extends V1LcapMessage {
    private String name;
    MyLcapMessage(String name) throws IOException {
      super();
      this.name = name;
    }
    public String toString() {
      return name;
    }
  }
}
