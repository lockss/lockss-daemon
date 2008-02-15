/*
* $Id: PsmInterp.java,v 1.16 2008-02-15 09:13:36 tlipkis Exp $
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
package org.lockss.protocol.psm;

import java.util.*;
import org.lockss.daemon.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.util.Queue;

/**
 * The state machine interpreter.
 */
public class PsmInterp {
  static Logger log = Logger.getLogger("PsmInterp");

  private PsmMachine machine;
  private Object userData;
  private boolean initted = false;
  private PsmState curState;
  private long lastStateChange = -1;
  private int maxChainedEvents = 10;
  private StateTimer timer;
  private boolean isWaiting;
  private int curEventNum;
  private PsmInterpStateBean stateBean;
  private Checkpointer checkpointer;
  private FifoQueue eventQueue = new FifoQueue();
  private PsmManager psmMgr;
  private LockssRunnable runner;
  private boolean threaded;
  private Thread runnerThread;
  private Object runnerLock = new Object();
  private Object waitMonitor = new Object();
  private String name;

  /** Create a state machine interpreter that will run the specified state
   * machine.  The newly created machine should be either {@link
   * #start()}ed or {@link #resume(PsmInterpStateBean)}d.
   * @param stateMachine the state machine
   * @param userData arbitrary user object for use by state machine's
   * actions
   */
  public PsmInterp(PsmMachine stateMachine, Object userData) {
    if (stateMachine == null)
      throw new NullPointerException("null stateMachine");
    this.machine = stateMachine;
    this.userData = userData;
  }

  public PsmInterp(PsmManager mgr, PsmMachine stateMachine, Object userData) {
    this(stateMachine, userData);
    this.psmMgr = mgr;
  }

  /** Set the name, used in toString() and thread name */
  public void setName(String name) {
    this.name = name;
  }

  /** Set the Checkpointer that will be called whenever the machine enters
   * a resumable state */
  public void setCheckpointer(Checkpointer ch) {
    checkpointer = ch;
  }

  /** Return the user object associated with the running state machine */
  public Object getUserData() {
    return userData;
  }

  /** Enter the start state of the state machine, performing any entry
   * action and following transitions until machine waits..  Must be called
   * before processing events. */
  public synchronized void start() throws PsmException {
    checkThread();
    start0();
    enterState(machine.getInitialState(), PsmEvents.Start, maxChainedEvents);
  }

  private void start0() {
    init();
    stateBean = new PsmInterpStateBean();
  }

  /** Resume execution of the machine at the state indicated by
   * resumeStateBean.  The resumable state will receive a PsmEvents.Resume
   * event. */
  public synchronized void resume(PsmInterpStateBean resumeStateBean)
      throws PsmException {
    checkThread();
    enterState(resume0(resumeStateBean), PsmEvents.Resume, maxChainedEvents);
  }

  private PsmState resume0(PsmInterpStateBean resumeStateBean)
      throws PsmException {
    init();
    stateBean = resumeStateBean;
    String resumeStateName = stateBean.getLastResumableStateName();
    if (resumeStateName == null) {
      throw new PsmException.IllegalResumptionState("No saved state");
    }
    PsmState state = machine.getState(resumeStateName);
    if (state == null) {
      throw new PsmException.IllegalResumptionState("Not found: " +
						    resumeStateName);
    }
    if (!state.isResumable()) {
      throw new PsmException.IllegalResumptionState("Not resumable: " +
						    resumeStateName);
    }
    return state;
  }

  private void init() {
    if (initted) {
      throw new IllegalStateException("already started or resumed");
    }
    isWaiting = false;
    initted = true;
  }

  /** Process an event synchronously, generally one generated within a
   * state machine action.  Perform any entry action, follow transitions
   * until machine waits. */
  public synchronized void handleEvent(PsmEvent event) throws PsmException {
    checkThread();
    isWaiting = false;
    handleEvent1(event, maxChainedEvents);
  }

  /** Return true if the machine is in a final state */
  public boolean isFinalState() {
    return curState != null && curState.isFinal();
  }

  /** Return the final state iff it has been reached, else null */
  public PsmState getFinalState() {
    return isFinalState() ? curState : null;
  }

  public PsmState getCurrentState() {
    return curState;
  }

  public long getLastStateChange() {
    return lastStateChange;
  }

  /** Return true if the machine is waiting for an event. */
  public boolean isWaiting() {
    return isWaiting;
  }

  /** Arrange for a Timeout event to be raised in duration milliseconds if
   * no events have been processed before then. */
  void setCurrentStateTimeout(long duration) {
    if (timer != null) {
      timer.resetTo(duration);
    } else {
      Deadline when = Deadline.in(duration);
      timer = new StateTimer(when);
      timer.start();
    }
  }

  // XXX does this need to be public?
  synchronized void checkStateTimer() {
    if (timer != null) {
      log.debug2("Cancelling state timer");
      timer.maybeCancel();
    }
  }

  private void handleEvent1(PsmEvent event, int eventCtr) throws PsmException {
    if (curState == null) {
      throw new IllegalStateException("Not started or resumed");
    }
    if (event == null) {
      throw new NullPointerException("Null event");
    }
    if (eventCtr-- <= 0) {
      throw new
	PsmException.MaxChainedEvents("Exceeded maximum chained event count;" +
				 " possible loop: " + curState + ", " + event);
    }
    curEventNum++;
    checkStateTimer();
    PsmResponse resp = curState.getResponse(event);
    if (log.isDebug2()) log.debug2("Event: " + event + ": " + resp);
    if (resp == null) {
      throw new PsmException.UnknownEvent("State " + curState +
					  " has no response matching " +
					  event);
    }
    if (resp.isTransition()) {
      String newStateName = resp.getNewState();
      PsmState newState = machine.getState(newStateName);
      enterState(newState, event, eventCtr);
    } else {
      PsmAction action = resp.getAction();
      performAction(action, event, eventCtr);
    }
  }

  private void handleQueuedEvent(Req req) throws PsmException {
    PsmEvent event = req.event;
    if (event == PsmEvents.Start) {
      enterState(machine.getInitialState(), event, maxChainedEvents);
    } else if (event == PsmEvents.Resume) {
      enterState(req.state, event, maxChainedEvents);
    } else {
      handleEvent(event);
    }
  }

  private void performAction(PsmAction action, PsmEvent triggerEvent,
			     int eventCtr)
    throws PsmException {
  if (log.isDebug2()) log.debug2("Action: " + action);
    eventMonitor(curState, triggerEvent, action, null);
    PsmEvent event;
    try {
      // XXX Potential deadlock here because we are calling an action in a
      // synchronized block.  If the action accesses some other
      // synchronized object while another thread, holding that object's
      // lock, calls handleEvent or other synchronized method of this
      // class, a deadlock will result.  Can't release our lock first as
      // we're in the middle of a transition.
      event = action.run(triggerEvent, this);
    } catch (RuntimeException e) {
      throw new PsmException.ActionError("Action: " + action, e);
    }
    if (event == null) {
      throw new PsmException.NullEvent("Action: " + action + " returned null");
    } else if (event instanceof PsmWaitEvent) {
      if (!action.isWaitAction()) {
	throw new PsmException.IllegalEvent("Non-wait action: " + action +
					    " returned wait event: " + event);
      }
      handleWait((PsmWaitEvent)event);
    } else {
      handleEvent1(event, eventCtr);
    }
  }

  private void enterState(PsmState newState, PsmEvent triggerEvent,
			  int eventCtr)
      throws PsmException {
    if (log.isDebug2()) log.debug2("Enter state: " + newState.getName());
    if (newState.isResumable()) {
      checkpoint(newState);
    }
    eventMonitor(curState, triggerEvent, null, newState);
    curState = newState;
    lastStateChange = TimeBase.nowMs();
    PsmAction entryAction = newState.getEntryAction();
    performAction(entryAction, triggerEvent, eventCtr);
    if (eventQueue.isEmpty()) {
      synchronized(waitMonitor) {
	waitMonitor.notifyAll();
      }
    }
  }

  /** If the wait event has a timeout value start a timer before waiting */
  private void handleWait(PsmWaitEvent waitEvent) {
    long timeout = waitEvent.getTimeout();
    log.debug2("Wait " + timeout);
    if (timeout > 0) {
      setCurrentStateTimeout(timeout);
    }
    isWaiting = true;
  }

  private void startRunner() throws InterruptedException {
    synchronized (runnerLock) {
      if (runner != null) return;
      runner = new Runner(eventQueue);
      psmMgr.execute(runner);
    }
  }

  public String toString() {
    return "[PsmInterp: " + name + "]";
  }

  // Asynch mode.  Interpreter runs in its own thread.

  /** If true, warnings will be issued if events are signalled outside the
   * interpreter's thread */
  public void setThreaded(boolean val) {
    threaded = val;
  }

  void checkThread() {
    if (threaded && runnerThread != Thread.currentThread()) {
      log.warning("Event signalled outside interpreter thread",
		  new RuntimeException("Stack trace"));
    }
  }

  void assertThreaded() {
    if (!threaded) {
      log.warning("Threaded method called in unthreaded mode",
		  new RuntimeException("Stack trace"));
    }
  }

  public boolean waitIdle(long timeout) throws InterruptedException {
    return waitIdle(Deadline.in(timeout));
  }

  public boolean waitIdle(Deadline timer) throws InterruptedException {
    return waitCondition(timer, new Condition() {
	public boolean evaluate() {
	  return eventQueue.isEmpty() && isWaiting;
	}
      });
  }

  public boolean waitFinal(long timeout) throws InterruptedException {
    return waitFinal(Deadline.in(timeout));
  }

  public boolean waitFinal(Deadline timer) throws InterruptedException {
    return waitCondition(timer, new Condition() {
	public boolean evaluate() {
	  return eventQueue.isEmpty() && curState.isFinal();
	}
      });
  }

  interface Condition {
    boolean evaluate();
  }

  private boolean waitCondition(Deadline timer, Condition test)
      throws InterruptedException {
    if (timer != null) {
      Deadline.InterruptCallback cb = new Deadline.InterruptCallback();
      try {
	timer.registerCallback(cb);
	while (!timer.expired()) {
	  long sleep = timer.getSleepTime();
	  synchronized (waitMonitor) {
	    if (test.evaluate()) {
	      return true;
	    }
	  }
	}
      } finally {
	cb.disable();
	timer.unregisterCallback(cb);
      }
    }
    return test.evaluate();
  }


  /** Enter the start state of the state machine, performing any entry
   * action and following transitions until machine waits..  Must be called
   * before processing events. */
  public void enqueueStart(ErrorHandler errHandler) throws PsmException {
    assertThreaded();
    start0();
    enqueueEvent(PsmEvents.Start, errHandler);
  }

  public void enqueueResume(PsmInterpStateBean resumeStateBean,
			     ErrorHandler errHandler)
      throws PsmException {
    enqueueReq(new Req(resume0(resumeStateBean),
		       PsmEvents.Resume,
		       errHandler));
  }

  /** Process an event asynchronously, generally one generated from outside
   * the state machine (such as message recept).  Perform any entry action,
   * follow transitions until machine waits. */
  public void enqueueEvent(PsmEvent event, ErrorHandler err) {
    enqueueEvent(event, err, null);
  }

  /** Process an event asynchronously, generally one generated from outside
   * the state machine (such as message recept).  Perform any entry action,
   * follow transitions until machine waits. */
  public void enqueueEvent(PsmEvent event, ErrorHandler err,
			   Action completionAction) {
    enqueueReq(new Req(event, err, completionAction));
  }

  private void enqueueReq(Req req) {
    assertThreaded();
    synchronized (runnerLock) {
      eventQueue.put(req);
      if (runner == null) {
	try {
	  startRunner();
	} catch (InterruptedException e) {
	  handleError(req,
		      new PsmException("Couldn't start interp runner", e));
	}
      }
    }
  }

  void handleError(Req req, PsmException e) {
    if (req.errHandler != null) {
      try {
	req.errHandler.handleError(e);
      } catch (Exception e2) {
	log.warning("Asynch error handler, handling", e);
	log.warning("Asynch error handler threw", e2);
      }
    } else {
      log.warning("No error handler for asynchronous error", e);
    }
  }

  class Req {
    PsmEvent event;
    PsmState state;
    ErrorHandler errHandler;
    Action completionAction;

    Req(PsmEvent event, ErrorHandler errHandler) {
      this(event, errHandler, null);
    }

    Req(PsmEvent event, ErrorHandler errHandler, Action completionAction) {
      this.event = event;
      this.errHandler = errHandler;
      this.completionAction = completionAction;
    }

    Req(PsmState state, PsmEvent event, ErrorHandler errHandler) {
      this(event, errHandler);
      this.state = state;
    }
  }

  public interface ErrorHandler {
    void handleError(PsmException e);
  }

  public interface Action {
    void eval();
  }

  public class Runner extends LockssRunnable {
    private Queue reqQueue;

    private Runner(Queue reqQueue) {
      super("PsmRunner");
      this.reqQueue = reqQueue;
    }

    public String toString() {
      return "[PsmRunner: " + name + "]";
    }

    public void lockssRun() {
      if (name != null) {
	setThreadName("PsmRunner: " + name);
      } else {
	setThreadName("PsmRunner");
      }
      runnerThread = Thread.currentThread();
      try {
	while (true) {
	  try {
	    Req req =
	      (Req)reqQueue.get(Deadline.in(psmMgr.getRunnerIdleTimeout()));
	    if (req != null) {
	      try {
		handleQueuedEvent(req);
		if (req.completionAction != null) {
		  req.completionAction.eval();
		}
	      } catch (PsmException e) {
		handleError(req, e);
	      }
	      // XXX handle RuntimeException?
	    } else {
	      synchronized (runnerLock) {
		if (reqQueue.isEmpty()) {
		  runner = null;
		  return;
		}
	      }
	    }
	  } catch (InterruptedException ignore) {
	    // no action
	  }
	}
      } finally {
	setThreadName("PsmRunner Idle");
	synchronized (runnerLock) {
	  if (runnerThread == Thread.currentThread()) {
	    runnerThread = null;
	  }
	  if (this == runner) {
	    runner = null;
	  }
	}
      }
    }
  }

  /** State timeout logic.  Refers to several members of PsmInterp. */
  private class StateTimer {
    private int timingEvent;
    private Deadline deadline;
    private TimerQueue.Request req;

    StateTimer(Deadline deadline) {
      this.deadline = deadline;
      this.timingEvent = getTimedEventNumber();
    }

    void resetTo(long duration) {
      this.timingEvent = getTimedEventNumber();
      deadline.expireIn(duration);
    }

    private int getTimedEventNumber() {
      // Need this if allow actions to call setCurrentStateTimeout() directly
//       if (!PsmInterp.this.isWaiting()) {
// 	return curEventNum + 1;
//       }
      return curEventNum;
    }

    /** Start the timer running */
    void start() {
      log.debug2("Setting timeout: " + deadline);
      req = TimerQueue.schedule(deadline, timerCb, this);
    }

    /** Cancel the if events have happened since the one we're timing */
    void maybeCancel() {
      if (timingEvent != curEventNum) {
	TimerQueue.cancel(req);
	timer = null;
      }
    }

    /** Send a Timeout event iff no events have been processed since the
     * timer was set.  If the event is not handled, log and ignore it */
    void timerExpired() {
      synchronized (PsmInterp.this) {
	timer = null;
	if (timingEvent == curEventNum) {
	  log.debug2("Signalling Timeout event");
	  try {
	    handleEvent(PsmEvents.Timeout);
	  } catch (PsmException.UnknownEvent e) {
	    log.warning("Timeout event not handled in state: " + curState);
	  } catch (PsmException e) {
	    log.error("In state: " + curState, e);
	    // XXX Need a field to keep track of error state of machine for
	    // errors that can't be thrown to user?
	  }
	} else {
	  log.debug2("Not signalling Timeout event, state changed.");
	}
      }
    }
  }

  private static final TimerQueue.Callback timerCb = new StateTimerCallback();

  static class StateTimerCallback implements TimerQueue.Callback {
    public void timerExpired(Object cookie) {
      log.debug2("StateTimerCallback: " + cookie);
      ((PsmInterp.StateTimer)cookie).timerExpired();
    }
    public String toString() {
      return "PSMStateTimer";
    }
  }


  /** Hook for subclasses to override if they want to monitor progress of
   * the state machine interpreter in detail.  Generally, event and action
   * are mutually exclusive, and performing an action and entering a state
   * are separate internal events.  This implementation does nothing.
   * @param curState the current or old state.  null only on the first
   * event, entering the start state.
   * @param event the event signalled, if any.
   * @param action the action being performed, if any
   * @param newState the new state about to be entered if any.
   */
  protected void eventMonitor(PsmState curState, PsmEvent event,
			      PsmAction action, PsmState newState) {
  }

  /** Update the state bean and call the checkpointer to checkpoint the
   * state */
  private void checkpoint(PsmState state) {
    String stateName = state.getName();
    if (!stateName.equals(stateBean.getLastResumableStateName())) {
      stateBean.setLastResumableStateName(stateName);
      if (checkpointer != null) {
	checkpointer.checkpoint(stateBean);
      }
    }
  }

  /** State checkpointer interface */
  public interface Checkpointer {
    /** Called whenever the machine enters a resumable state (before the
     * state actions are executed). */
    void checkpoint(PsmInterpStateBean resumeStateBean);
  }
}
