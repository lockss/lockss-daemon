/*
 * $Id: TestPsmInterp.java,v 1.4 2005-03-18 09:09:21 smorabito Exp $
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
import org.lockss.test.*;
import org.lockss.util.*;
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

  private static PsmMsgEvent RcvMsgA = new RcvMsgA();
  private static PsmMsgEvent RcvMsgA1 = new RcvMsgA1();
  private static PsmMsgEvent RcvMsgB = new RcvMsgB();
  private static PsmMsgEvent RcvMsgC = new RcvMsgC();

  // Store message instance into message events
  private static LcapMessage lmA = makeMsg("msgA");
  private static LcapMessage lmB = makeMsg("msgB");
  private static LcapMessage lmC = makeMsg("msgC");

  static {
    RcvMsgA.setMessage(lmA);
    RcvMsgA1.setMessage(lmA);
    RcvMsgB.setMessage(lmB);
    RcvMsgC.setMessage(lmC);
  }

  public void testNullConstructorArgs() {
    PsmState[] states = {
      new PsmState("Start", new PsmResponse(Ok, "Start")),
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

  // Can't call init() twice
  public void testInitTwice() {
    PsmState[] states = {
      new PsmState("Start"),
    };
    PsmMachine mach = new PsmMachine("M1", states, "Start");
    MyInterp interp = new MyInterp(mach, null);
    interp.init();
    try {
      interp.init();
      fail("Second call to init() should throw");
    } catch (IllegalStateException e) {
    }
  }

  // Can't call handleEvent() before init()
  public void testNoInit() {
    PsmState[] states = {
      new PsmState("Start", new PsmResponse(Ok)),
    };
    PsmMachine mach = new PsmMachine("M1", states, "Start");
    MyInterp interp = new MyInterp(mach, null);
    try {
      interp.handleEvent(Ok);
      fail("handleEvent() before init() should throw");
    } catch (IllegalStateException e) {
    }
  }

  // Loop in Start state
  public void testLoopInit() {
    PsmState[] statesLoopInit = {
      new PsmState("Start", new MyAction(Sched),
		   new PsmResponse(Sched, new MyAction(Sched))),
    };
    PsmMachine mach = new PsmMachine("M1", statesLoopInit, "Start");
    MyInterp interp = new MyInterp(mach, null);
    try {
      interp.init();
      fail("Should threw if state loop");
    } catch (PsmException.MaxChainedEvents e) {
    }
  }

  // Loop between two states, in init()
  public void testLoopTwo() {
    PsmState[] statesLoopTwo = {
      new PsmState("Start", new MyAction(Sched),
		   new PsmResponse(Sched, "two")),
      new PsmState("two", new MyAction(SendOk),
		   new PsmResponse(SendOk, "Start")),
    };
    PsmMachine mach = new PsmMachine("M1", statesLoopTwo, "Start");
    PsmInterp interp = new MyInterp(mach, null);
    try {
      interp.init();
      fail("Should threw if state loop");
    } catch (PsmException.MaxChainedEvents e) {
    }
  }

  // Loop between two states, in handleEvent()
  public void testLoopHandle() {
    PsmState[] statesLoopTwo = {
      new PsmState("Start",
		   new PsmResponse(Sched, "one")),
      new PsmState("one", new MyAction(SendOk),
		   new PsmResponse(SendOk, "two")),
      new PsmState("two", new MyAction(SendOk),
		   new PsmResponse(SendOk, "one")),
    };
    PsmMachine mach = new PsmMachine("M1", statesLoopTwo, "Start");
    PsmInterp interp = new MyInterp(mach, null);
    interp.init();
    try {
      interp.handleEvent(Sched);
      fail("Should threw if state loop");
    } catch (PsmException.MaxChainedEvents e) {
    }
  }

  // RuntimeException in action should become PsmException.ActionError
  public void testActionError() {
    PsmState[] states = {
      new PsmState("Start", new ThrowAction(new RuntimeException("abcd"))),
    };
    PsmMachine mach = new PsmMachine("M1", states, "Start");
    MyInterp interp = new MyInterp(mach, null);
    try {
      interp.init();
      fail("Interp should threw if action throws");
    } catch (PsmException.ActionError e) {
      assertEquals("abcd", e.getNestedException().getMessage());
    }
  }

  PsmAction nullAction = new PsmAction() {
      public PsmEvent run(PsmEvent event, PsmInterp interp) {
	return null;
      }};

  // Actions are not allowed to return a null Event
  public void testNullEntryAction() {
    PsmState[] states = {
      new PsmState("Start", nullAction),
    };
    PsmMachine mach = new PsmMachine("M1", states, "Start");
    MyInterp interp = new MyInterp(mach, null);
    try {
      interp.init();
      fail("Interp should threw if action returns null");
    } catch (PsmException.NullEvent e) {
    }
  }

  public void testNullRespAction() {
    PsmState[] states = {
      new PsmState("Start",
		   new PsmResponse(Else, nullAction)),
    };
    PsmMachine mach = new PsmMachine("M1", states, "Start");
    MyInterp interp = new MyInterp(mach, null);
    interp.init();
    try {
      interp.handleEvent(Ok);
      fail("Interp should threw if action returns null");
    } catch (PsmException.NullEvent e) {
    }
  }

  // Simple state machine for next few tests
  PsmState[] states1 = {
    new PsmState("Start", new MyAction(Sched),
		 new PsmResponse(Sched),
		 new PsmResponse(NotSched, "Error"),
		 new PsmResponse(TaskComplete, "Send"),
		 new PsmResponse(TaskError, "Error")),
    new PsmState("Send", new MyAction(SendOk),
		 new PsmResponse(SendOk, "WaitVote"),
		 new PsmResponse(Else, "Error")),
    new PsmState("WaitVote",
		 new PsmResponse(RcvMsgA, "Done"),
		 new PsmResponse(Else, "Error")),
    new PsmState("Error"),
    new PsmState("Done"),
  };

  // Next few tests record all interpreter events (action, transitions,
  // etc) using eventMonitor hook then check against expected events.

  public void testSimple1() {
    PsmMachine mach = new PsmMachine("M1", states1, "Start");
    MyInterp interp = new MyInterp(mach, null);
    assertFalse(interp.isFinalState());
    interp.init();
    assertFalse(interp.isFinalState());
    ER[] exp1 = {
      new ER(null, PsmEvents.Start, null, states1[0]),
      new ER(states1[0], null, states1[0].getEntryAction(), null),
      new ER(states1[0], Sched, null, null),
    };
    assertIsomorphic(exp1, interp.events);
    interp.clear();
    interp.handleEvent(NotSched);
    ER[] exp2 = {
      new ER(states1[0], NotSched, null, states1[3]),
      new ER(states1[3], null, null, null),
    };
    assertIsomorphic(exp2, interp.events);
  }

  public void testSimple2() {
    PsmMachine mach = new PsmMachine("M1", states1, "Start");
    MyInterp interp = new MyInterp(mach, null);
    interp.init();
    ER[] exp1 = {
      new ER(null, PsmEvents.Start, null, states1[0]),
      new ER(states1[0], null, states1[0].getEntryAction(), null),
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
      new ER(states1[1], null, states1[1].getEntryAction(), null),
      new ER(states1[1], SendOk, null, states1[2]),
      new ER(states1[2], null, null, null),
    };
    assertIsomorphic(exp3, interp.events);
  }

  PsmState[] states2 = {
    new PsmState("Start", new MyAction(SendOk),
		 new PsmResponse(SendOk, "WaitVote"),
		 new PsmResponse(Else, "Error")),
    new PsmState("WaitVote",
		 new PsmResponse(RcvMsgA, "WaitVote"),
		 new PsmResponse(RcvMsgB, "Done"),
		 new PsmResponse(Else, "Error")),
    new PsmState("Done"),
    new PsmState("Error"),
  };

  // Feed interpreter events from list until it reaches final state
  void runEvents(PsmInterp interp, List events) {
    interp.init();
    while (!interp.isFinalState()){
      interp.handleEvent((PsmEvent)events.remove(0));
    }
  }

  // Run states2 machine until final state through two paths, check
  // interpreter events

  public void testUntilFinal() {
    PsmMachine mach = new PsmMachine("M1", states2, "Start");
    MyInterp interp = new MyInterp(mach, null);
    runEvents(interp, ListUtil.list(RcvMsgA, RcvMsgA, RcvMsgB));
    ER[] exp1 = {
      new ER(null, PsmEvents.Start, null, states2[0]),
      new ER(states2[0], null, states2[0].getEntryAction(), null),
      new ER(states2[0], SendOk, null, states2[1]),
      new ER(states2[1], null, null, null),
      new ER(states2[1], RcvMsgA, null, states2[1]),
      new ER(states2[1], null, null, null),
      new ER(states2[1], RcvMsgA, null, states2[1]),
      new ER(states2[1], null, null, null),
      new ER(states2[1], RcvMsgB, null, states2[2]),
      new ER(states2[2], null, null, null),
    };
    assertIsomorphic(exp1, interp.events);
  }

  public void testUntilFinalError() {
    PsmMachine mach = new PsmMachine("M1", states2, "Start");
    MyInterp interp = new MyInterp(mach, null);
    runEvents(interp, ListUtil.list(RcvMsgA, RcvMsgA, RcvMsgC));
    ER[] exp1 = {
      new ER(null, PsmEvents.Start, null, states2[0]),
      new ER(states2[0], null, states2[0].getEntryAction(), null),
      new ER(states2[0], SendOk, null, states2[1]),
      new ER(states2[1], null, null, null),
      new ER(states2[1], RcvMsgA, null, states2[1]),
      new ER(states2[1], null, null, null),
      new ER(states2[1], RcvMsgA, null, states2[1]),
      new ER(states2[1], null, null, null),
      new ER(states2[1], RcvMsgC, null, states2[3]),
      new ER(states2[3], null, null, null),
    };
    assertIsomorphic(exp1, interp.events);
  }

  // A machine with some message actions, which record the message received
  PsmState[] states3 = {
    new PsmState("Start", new MyAction(SendOk),
		 new PsmResponse(SendOk, "WaitVote"),
		 new PsmResponse(Else, "Error")),
    new PsmState("WaitVote",
		 new PsmResponse(RcvMsgA, new MyMsgAction(MsgOk)),
		 new PsmResponse(RcvMsgB, new MyMsgAction(MsgDone)),
		 new PsmResponse(MsgOk),
		 new PsmResponse(MsgDone, "Done"),
		 new PsmResponse(Else, "Error")),
    new PsmState("Done"),
    new PsmState("Error"),
  };

  // Feed the machine message events, check that message action received
  // the proper messages.
  public void testRcvMsg() {
    PsmMachine mach = new PsmMachine("M1", states3, "Start");
    final List msgs = new ArrayList();
    MyInterp interp = new MyInterp(mach, new MessageRecorder() {
	public void record(LcapMessage msg) { msgs.add(msg); }});
    runEvents(interp, ListUtil.list(RcvMsgA, RcvMsgA, RcvMsgB));
    assertEquals(ListUtil.list(lmA, lmA, lmB), msgs);
  }

  // A more functional test.  Creates a user object in which actions track
  // their progress.  Schedules a "computation" using the TimerQueue to
  // generate the TaskComplete event.

  // User objext tracks user events
  static class TestObj {
    long computeTime = 0;
    long timeout = 0;
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
	interp.handleEvent(TaskComplete);
      }};
      
  // Action schedules "computation", returns success (Sched event)
  PsmAction schedAction = new PsmAction() {
      public PsmEvent run(PsmEvent event, PsmInterp interp) {
	TestObj obj = (TestObj)interp.getUserData();
	// start a state timeout if requested
	if (obj.timeout != 0) {
	  obj.event("set timeout");
	  interp.setCurrentStateTimeout(obj.timeout);
	}
	// schedule a computation if requested, else signal NotSched event
	if (obj.computeTime != 0) {
	  TimerQueue.schedule(Deadline.in(obj.computeTime), tcb4, interp);
	  obj.event("sched");
	  return Sched;
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
    new PsmState("WaitCompute",
		 new PsmResponse(TaskComplete, "AlmostDone"),
		 new PsmResponse(Timeout, "GiveUp"),
		 new PsmResponse(Else, "Error")),
    new PsmState("AlmostDone", doneAction,
		 new PsmResponse(Else, "Done")),
    new PsmState("Done").succeed(),
    new PsmState("Error").fail(),
    new PsmState("GiveUp").fail(),
  };

  public void testCallback() {
    PsmMachine mach = new PsmMachine("M1", states4, "Start");
    TestObj obj = new TestObj();
    obj.computeTime = 100;
    MyInterp interp = new MyInterp(mach, obj);
    interp.init();
    // This machine doesn't need any more outside events to finish, so just
    // wait for it
    Interrupter intr = null;
    try {
      intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      while (!interp.isFinalState()) {
	TimerUtil.sleep(10);
      };
      assertTrue(interp.getFinalState().isSucceed());
      assertEquals("Done", interp.getFinalState().getName());
      assertEquals(ListUtil.list("sched", "taskcomplete", "done"),
		   obj.userEvents);
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	fail("testCallback machine didn't reach final state");
      }
    }
  }

  public void testTimeoutDoesnt() {
    PsmMachine mach = new PsmMachine("M1", states4, "Start");
    TestObj obj = new TestObj();
    obj.timeout = 5000;
    obj.computeTime = 10;
    MyInterp interp = new MyInterp(mach, obj);
    interp.init();
    // This machine doesn't need any more outside events to finish, so just
    // wait for it
    Interrupter intr = null;
    try {
      intr = interruptMeIn(Math.max(TIMEOUT_SHOULDNT, 10 * Constants.SECOND),
				    true);
      while (!interp.isFinalState()) {
	TimerUtil.sleep(10);
      };
      intr.cancel();
      assertEquals("Done", interp.getFinalState().getName());
      assertEquals(ListUtil.list("set timeout", "sched",
				 "taskcomplete", "done"),
		   obj.userEvents);
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	fail("testTimeout machine didn't reach final state");
      }
    }
  }

  public void testTimeoutDoes() {
    PsmMachine mach = new PsmMachine("M1", states4, "Start");
    TestObj obj = new TestObj();
    obj.timeout = 100;
    obj.computeTime = 5 * Constants.SECOND;
    MyInterp interp = new MyInterp(mach, obj);
    interp.init();
    // This machine doesn't need any more outside events to finish, so just
    // wait for it
    Interrupter intr = null;
    try {
      intr = interruptMeIn(Math.max(TIMEOUT_SHOULDNT, 10 * Constants.SECOND),
				    true);
      while (!interp.isFinalState()) {
	TimerUtil.sleep(10);
      };
      intr.cancel();
      assertTrue(interp.getFinalState().isFail());
      assertEquals("GiveUp", interp.getFinalState().getName());
      assertEquals(ListUtil.list("set timeout", "sched"),
		   obj.userEvents);
    } catch (InterruptedException e) {
    } finally {
      if (intr.did()) {
	fail("testTimeout machine didn't reach final state");
      }
    }
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

    public MyInterp(PsmMachine stateMachine, Object userData) {
      super(stateMachine, userData);
    }

    protected void eventMonitor(PsmState curState, PsmEvent event,
				PsmAction action, PsmState newState) {
      events.add(new ER(curState, event, action, newState));
    }
    public void clear() {
      events.clear();
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
