/*
 * $Id: PollerSolicitStateTable.java,v 1.1 2005-04-19 03:08:32 smorabito Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.poller.v3;

import org.lockss.util.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;

/**
 * A PSM state table describing the V3 Poller solicitation
 * state transitions and their action implementations.
 */
public class PollerSolicitStateTable implements StateTable {

  /*
   * Event classes.
   */
  public static class Ok extends PsmEvent {}
  public static class PollAck extends PsmMsgEvent {
    PollAck() { super(); }
    PollAck(LcapMessage msg) { super(msg); }
  }
  public static class Vote extends PsmMsgEvent {
    Vote() { super(); }
    Vote(LcapMessage msg) { super(msg); }
  }

  /*
   * Event instances.
   */
  public static PsmEvent evtElse = PsmEvents.Else;
  public static PsmEvent evtError = PsmEvents.Error;
  public static PsmEvent evtOk = new Ok();
  public static PollAck msgPollAck = new PollAck();
  public static Vote msgVote = new Vote();

  /*
   * Entry action definitions.
   *
   * (Note that these are not static instances, and that their
   * implementations simply call corresponding methods on the outer
   * class.  This is intentional, so that their behavior can be more
   * easily altered for unit testing.
   */
  private  PsmAction initialize = new PsmAction() {
      public PsmEvent run(PsmEvent evt, PsmInterp interp) {
	return handleInitialize(evt, interp);
      }
    };

  private PsmAction proveIntroEffort = new PsmAction() {
      public PsmEvent run(PsmEvent evt, PsmInterp interp) {
	return handleProveIntroEffort(evt, interp);
      }
    };

  private PsmAction sendPoll = new PsmAction() {
      public PsmEvent run(PsmEvent evt, PsmInterp interp) {
	return handleSendPoll(evt, interp);
      }
    };

  private PsmMsgAction receivedPollAck = new PsmMsgAction() {
      public PsmEvent runMsg(PsmMsgEvent evt, PsmInterp interp) {
	return handleReceivedPollAck(evt, interp);
      }
    };

  private PsmAction verifyPollAckEffort = new PsmAction() {
      public PsmEvent run(PsmEvent evt, PsmInterp interp) {
	return handleVerifyPollAckEffort(evt, interp);
      }
    };

  private PsmAction proveRemainingEffort = new PsmAction() {
      public PsmEvent run(PsmEvent evt, PsmInterp interp) {
	return handleProveRemainingEffort(evt, interp);
      }
    };

  private PsmAction sendPollProof = new PsmAction() {
      public PsmEvent run(PsmEvent evt, PsmInterp interp) {
	return handleSendPollProof(evt, interp);
      }
    };

  private PsmMsgAction receivedVote = new PsmMsgAction() {
      public PsmEvent runMsg(PsmMsgEvent evt, PsmInterp interp) {
	return handleReceivedVote(evt, interp);
      }
    };

  private PsmAction error = new PsmAction() {
      public PsmEvent run(PsmEvent evt, PsmInterp interp) {
	return handleError(evt, interp);
      }
    };


  /**
   * Mapping of states to response handlers.
   */
  private PsmState[] states = {
    new PsmState("Initializing",
		 initialize,
		 new PsmResponse(evtOk, "ProvingIntroEffort"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("ProvingIntroEffort",
		 proveIntroEffort,
		 new PsmResponse(evtOk, "SendingPoll"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("SendingPoll",
		 sendPoll,
		 new PsmResponse(evtOk, "WaitingPollAck"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("WaitingPollAck",
		 new PsmResponse(msgPollAck, receivedPollAck),
		 new PsmResponse(evtOk, "VerifyingPollAckEffort"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("VerifyingPollAckEffort",
		 verifyPollAckEffort,
		 new PsmResponse(evtOk, "ProvingRemainingEffort"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("ProvingRemainingEffort",
		 proveRemainingEffort,
		 new PsmResponse(evtOk, "SendingPollProof"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("SendingPollProof",
		 sendPollProof,
		 new PsmResponse(evtOk, "WaitingVote"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("WaitingVote",
		 new PsmResponse(msgVote, receivedVote),
		 new PsmResponse(evtOk, "Finalizing"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("Error",
		 error,
		 new PsmResponse(evtOk, "Finalizing")),
    new PsmState("Finalizing")
  };

  // Poller State Machine instance
  private PsmMachine m_stateMachine =
    new PsmMachine("PollerSolicit", states, "Initializing");

  // Interpreter reference
  private PsmInterp m_interp = null;

  // State object
  private UserData stateHolder = new UserData();

  private static Logger log = Logger.getLogger("PollerSolicitStateTable");

  /**
   * Handler implementations.
   */

  protected PsmEvent handleInitialize(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  protected PsmEvent handleProveIntroEffort(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  protected PsmEvent handleSendPoll(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  protected PsmEvent handleReceivedPollAck(PsmMsgEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  protected PsmEvent handleVerifyPollAckEffort(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  protected PsmEvent handleProveRemainingEffort(PsmEvent evt,
						PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  protected PsmEvent handleSendPollProof(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  protected PsmEvent handleReceivedVote(PsmMsgEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  protected PsmEvent handleError(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  /**
   * Implementations of the StateTable interface.
   */
  public PsmInterp getInterp() {
    if (m_interp == null) {
      m_interp = new PsmInterp(m_stateMachine, stateHolder);
    }
    return m_interp;
  }

  /**
   * State object.
   *
   * XXX: This is just a stub at the moment.
   */
  protected static class UserData {
  }
}
