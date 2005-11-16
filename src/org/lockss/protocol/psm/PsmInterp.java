/*
* $Id: PsmInterp.java,v 1.13 2005-11-16 07:44:09 smorabito Exp $
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

import java.util.*;
import org.lockss.util.*;

/**
 * The state machine interpreter.
 */
public class PsmInterp {
  static Logger log = Logger.getLogger("PsmInterp");

  private PsmMachine machine;
  private Object userData;
  private PsmState curState;
  private int maxChainedEvents = 10;
  private StateTimer timer;
  private boolean isWaiting;
  private int curEventNum;
  private PsmInterpStateBean stateBean;
  private Checkpointer checkpointer;

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
    init();
    stateBean = new PsmInterpStateBean();
    enterState(machine.getInitialState(), PsmEvents.Start, maxChainedEvents);
  }

  /** Resume execution of the machine at the state indicated by
   * resumeStateBean.  The resumable state will receive a PsmEvents.Resume
   * event. */
  public synchronized void resume(PsmInterpStateBean resumeStateBean)
      throws PsmException {
    init();
    stateBean = resumeStateBean;
    String name = stateBean.getLastResumableStateName();
    if (name == null) {
      throw new PsmException.IllegalResumptionState("No saved state");
    }
    PsmState state = machine.getState(name);
    if (state == null) {
      throw new PsmException.IllegalResumptionState("Not found: " + name);
    }
    if (!state.isResumable()) {
      throw new PsmException.IllegalResumptionState("Not resumable: " + name);
    }
    enterState(state, PsmEvents.Resume, maxChainedEvents);
  }

  private void init() {
    if (curState != null) {
      throw new IllegalStateException("already started or resumed");
    }
    isWaiting = false;
  }

  /** Process an event generated from outside the state machine (such as
   * message recept).  Perform any entry action, follow transitions until
   * machine waits. */
  public synchronized void handleEvent(PsmEvent event) throws PsmException {
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
    PsmAction entryAction = newState.getEntryAction();
    performAction(entryAction, triggerEvent, eventCtr);
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
    String name = state.getName();
    if (!name.equals(stateBean.getLastResumableStateName())) {
      stateBean.setLastResumableStateName(name);
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
