/*
 * $Id: VoterStateTable.java,v 1.1 2005-04-19 03:08:32 smorabito Exp $
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
 * A PSM state table describing the V3 Voter
 * state transitions and their action implementations.
 */
public class VoterStateTable implements StateTable {

  /*
   * Event classes.
   */
  public static class Ok extends PsmEvent {}
  public static class ReceiptOk extends PsmEvent {}
  public static class RepairOk extends PsmEvent {}
  public static class PollProof extends PsmMsgEvent {
    PollProof() { super(); }
    PollProof(LcapMessage msg) { super(msg); }
  }
  public static class RepairRequest extends PsmMsgEvent {
    RepairRequest() { super(); }
    RepairRequest(LcapMessage msg) { super(msg); }
  }
  public static class Receipt extends PsmMsgEvent {
    Receipt() { super(); }
    Receipt(LcapMessage msg) { super(msg); }
  }

  /*
   * Event instances.
   */
  public static PsmEvent evtElse = PsmEvents.Else;
  public static PsmEvent evtError = PsmEvents.Error;
  public static PsmEvent evtOk = new Ok();
  public static PsmEvent evtReceiptOk = new ReceiptOk();
  public static PsmEvent evtRepairOk = new RepairOk();
  public static PollProof msgPollProof = new PollProof();
  public static RepairRequest msgRepairRequest = new RepairRequest();
  public static Receipt msgReceipt = new Receipt();

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

  private PsmAction verifyPollEffort = new PsmAction() {
      public PsmEvent run(PsmEvent evt, PsmInterp interp) {
	return handleVerifyPollEffort(evt, interp);
      }
    };

  private PsmAction provePollAck = new PsmAction() {
      public PsmEvent run(PsmEvent evt, PsmInterp interp) {
	return handleProvePollAck(evt, interp);
      }
    };

  private PsmAction sendPollAck = new PsmAction() {
      public PsmEvent run(PsmEvent evt, PsmInterp interp) {
	return handleSendPollAck(evt, interp);
      }
    };

  private PsmMsgAction receivedPollProof = new PsmMsgAction() {
      public PsmEvent runMsg(PsmMsgEvent evt, PsmInterp interp) {
	return handleReceivedPollProof(evt, interp);
      }
    };

  private PsmAction verifyPollProof = new PsmAction() {
      public PsmEvent run(PsmEvent evt, PsmInterp interp) {
	return handleVerifyPollProof(evt, interp);
      }
    };

  private PsmAction generateVote = new PsmAction() {
      public PsmEvent run(PsmEvent evt, PsmInterp interp) {
	return handleGenerateVote(evt, interp);
      }
    };

  private PsmAction sendVote = new PsmAction() {
      public PsmEvent run(PsmEvent evt, PsmInterp interp) {
	return handleSendVote(evt, interp);
      }
    };

  private PsmMsgAction receivedRepairRequest = new PsmMsgAction() {
      public PsmEvent runMsg(PsmMsgEvent evt, PsmInterp interp) {
	return handleReceivedRepairRequest(evt, interp);
      }
    };

  private PsmAction sendRepair = new PsmAction() {
      public PsmEvent run(PsmEvent evt, PsmInterp interp) {
	return handleSendRepair(evt, interp);
      }
    };


  private PsmMsgAction receivedReceipt = new PsmMsgAction() {
      public PsmEvent runMsg(PsmMsgEvent evt, PsmInterp interp) {
	return handleReceivedReceipt(evt, interp);
      }
    };

  private PsmAction processReceipt = new PsmAction() {
      public PsmEvent run(PsmEvent evt, PsmInterp interp) {
	return handleProcessReceipt(evt, interp);
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
		 new PsmResponse(evtOk, "VerifyingPollEffort"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("VerifyingPollEffort",
		 verifyPollEffort,
		 new PsmResponse(evtOk, "ProvingPollAck"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("ProvingPollAck",
		 provePollAck,
		 new PsmResponse(evtOk, "SendingPollAck"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("SendingPollAck",
		 sendPollAck,
		 new PsmResponse(evtOk, "WaitingPollProof"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("WaitingPollProof",
		 new PsmResponse(msgPollProof, receivedPollProof),
		 new PsmResponse(evtOk, "VerifyingPollProof"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("VerifyingPollProof",
		 verifyPollProof,
		 new PsmResponse(evtOk, "GeneratingVote"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("GeneratingVote",
		 generateVote,
		 new PsmResponse(evtOk, "SendingVote"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("SendingVote",
		 sendVote,
		 new PsmResponse(evtOk, "WaitingRepairRequestOrReceipt"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("WaitingRepairRequestOrReceipt",
		 new PsmResponse(msgRepairRequest, receivedRepairRequest),
		 new PsmResponse(msgReceipt, receivedReceipt),
		 new PsmResponse(evtRepairOk, "SendingRepair"),
		 new PsmResponse(evtReceiptOk, "ProcessingReceipt"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("SendingRepair",
		 sendRepair,
		 new PsmResponse(evtOk, "WaitingRepairRequestOrReceipt"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("ProcessingReceipt",
		 processReceipt,
		 new PsmResponse(evtOk, "Finalizing"),
		 new PsmResponse(evtElse, "Error")),
    new PsmState("Error",
		 error,
		 new PsmResponse(evtOk, "Finalizing")),
    new PsmState("Finalizing")
  };

  // Poller State Machine instance
  private PsmMachine m_stateMachine =
    new PsmMachine("Voter", states, "Initializing");

  // Interpreter reference
  private PsmInterp m_interp = null;

  // State object
  private UserData stateHolder = new UserData();

  private static Logger log = Logger.getLogger("VoterStateTable");

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
   * Handler implementations.
   */
  protected PsmEvent handleInitialize(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  protected PsmEvent handleVerifyPollEffort(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  protected PsmEvent handleProvePollAck(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  protected PsmEvent handleSendPollAck(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  protected PsmEvent handleReceivedPollProof(PsmMsgEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  protected PsmEvent handleVerifyPollProof(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  protected PsmEvent handleGenerateVote(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  protected PsmEvent handleSendVote(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  protected PsmEvent handleReceivedRepairRequest(PsmMsgEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtRepairOk;
  }

  protected PsmEvent handleSendRepair(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  protected PsmEvent handleReceivedReceipt(PsmMsgEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtReceiptOk;
  }

  protected PsmEvent handleProcessReceipt(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  protected PsmEvent handleError(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return evtOk;
  }

  /**
   * State object.
   *
   * XXX: This is just a stub at the moment.
   */
  protected static class UserData {
  }
}
