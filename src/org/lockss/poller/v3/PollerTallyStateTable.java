/*
 * $Id: PollerTallyStateTable.java,v 1.1 2005-04-19 03:08:32 smorabito Exp $
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
 * A PSM state table describing the V3 Poller tallying
 * state transitions and their action implementations.
 */
public class PollerTallyStateTable implements StateTable {

  /*
   * Event classes.
   */
  public static class Ok extends PsmEvent {}
  public static class RepairOk extends PsmEvent {}
  public static class RepairNeeded extends PsmEvent {}
  public static class RepairNotNeeded extends PsmEvent {}
  public static class Repair extends PsmMsgEvent {
    Repair() { super(); }
    Repair(LcapMessage msg) { super(msg); }
  }

  /*
   * Event instances.
   */
  public static PsmEvent evtElse = PsmEvents.Else;
  public static PsmEvent evtError = PsmEvents.Error;
  public static PsmEvent evtOk = new Ok();
  public static PsmEvent evtRepairOk = new RepairOk();
  public static PsmEvent evtRepairNeeded = new RepairNeeded();
  public static PsmEvent evtRepairNotNeeded = new RepairNotNeeded();
  public static Repair msgRepair = new Repair();

  /*
   * Entry action definitions.
   *
   * (Note that these are not static classes, and that their
   * implementations simply call corresponding methods on the outer
   * class.  This is intentional, so that their behavior can be more
   * easily altered for unit testing.
   */
  private  PsmAction initialize = new PsmAction() {
      public PsmEvent run(PsmEvent evt, PsmInterp interp) {
	return handleInitialize(evt, interp);
      }
    };

  private PsmAction verifyVoteEffort = new PsmAction() {
      public PsmEvent run(PsmEvent evt, PsmInterp interp) {
	return handleVerifyVoteEffort(evt, interp);
      }
    };

  private PsmAction tally = new PsmAction() {
      public PsmEvent run(PsmEvent evt, PsmInterp interp) {
	return handleTally(evt, interp);
      }
    };

  private PsmAction proveRepairEffort = new PsmAction() {
      public PsmEvent run(PsmEvent evt, PsmInterp interp) {
	return handleProveRepairEffort(evt, interp);
      }
    };

  private PsmAction sendRepairRequest = new PsmAction() {
      public PsmEvent run(PsmEvent evt, PsmInterp interp) {
	return handleSendRepairRequest(evt, interp);
      }
    };

  private PsmMsgAction receivedRepair = new PsmMsgAction() {
      public PsmEvent runMsg(PsmMsgEvent evt, PsmInterp interp) {
	return handleReceivedRepair(evt, interp);
      }
    };

  private PsmAction sendReceipt = new PsmAction() {
      public PsmEvent run(PsmEvent evt, PsmInterp interp) {
	return handleSendReceipt(evt, interp);
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
		 new PsmResponse(evtOk, "VerifyingVoteEffort"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("VerifyingVoteEffort",
		 verifyVoteEffort,
		 new PsmResponse(evtOk, "Tallying"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("Tallying",
		 tally,
		 new PsmResponse(evtRepairNeeded, "ProvingRepairEffort"),
		 new PsmResponse(evtRepairNotNeeded, "SendingReceipt"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("ProvingRepairEffort",
		 proveRepairEffort,
		 new PsmResponse(evtOk, "SendingRepairRequest"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("SendingRepairRequest",
		 sendRepairRequest,
		 new PsmResponse(evtOk, "WaitingRepair"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("WaitingRepair",
		 new PsmResponse(msgRepair, receivedRepair),
		 new PsmResponse(evtRepairOk, "SendingReceipt"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("SendingReceipt",
		 sendReceipt,
		 new PsmResponse(evtOk, "Finalizing"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("Error",
		 error,
		 new PsmResponse(evtOk, "Finalizing")),
    new PsmState("Finalizing")
  };

  // Poller State Machine instance
  private PsmMachine m_stateMachine =
    new PsmMachine("PollerTally", states, "Initializing");

  // Interpreter reference
  private PsmInterp m_interp = null;

  // State object
  private UserData stateHolder = new UserData();

  private static Logger log = Logger.getLogger("PollerTallyStateTable");

  /**
   * Handler implementations.
   */

  protected PsmEvent handleInitialize(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  protected PsmEvent handleVerifyVoteEffort(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  protected PsmEvent handleTally(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  protected PsmEvent handleProveRepairEffort(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  protected PsmEvent handleSendRepairRequest(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  protected PsmEvent handleReceivedRepair(PsmMsgEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtRepairOk;
  }

  protected PsmEvent handleSendReceipt(PsmEvent evt, PsmInterp interp) {
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
