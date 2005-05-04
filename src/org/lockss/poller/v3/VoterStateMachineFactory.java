/*
 * $Id: VoterStateMachineFactory.java,v 1.1 2005-05-04 19:04:11 smorabito Exp $
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

import org.lockss.protocol.psm.*;

public class VoterStateMachineFactory {

  /**
   * Obtain a PsmMachine for the Voter state table.
   *
   * @return A PsmMachine representing the Voter state table.
   * @param actionClass A class containing static handler methods
   *                    for the state machine to call.
   */
  public static PsmMachine getMachine(Class actionClass) {
    return new PsmMachine("Voter",
			  makeStates(actionClass),
			  "Initialize");
  }

  /**
   * Mapping of states to response handlers.
   */
  private static PsmState[] makeStates(Class actionClass) {
    PsmState[] states = {
      new PsmState("Initialize",
		   new PsmMethodAction(actionClass, "handleInitialize"),
		   new PsmResponse(V3Events.evtOk, "VerifyPollEffort"),
		   new PsmResponse(V3Events.evtElse, "Error")),
      new PsmState("VerifyPollEffort",
		   new PsmMethodAction(actionClass, "handleVerifyPollEffort"),
		   new PsmResponse(V3Events.evtOk, "ProvePollAck"),
		   new PsmResponse(V3Events.evtElse, "Error")),
      new PsmState("ProvePollAck",
		   new PsmMethodAction(actionClass, "handleProvePollAck"),
		   new PsmResponse(V3Events.evtOk, "SendPollAck"),
		   new PsmResponse(V3Events.evtElse, "Error")),
      new PsmState("SendPollAck",
		   new PsmMethodAction(actionClass, "handleSendPollAck"),
		   new PsmResponse(V3Events.evtOk, "WaitPollProof"),
		   new PsmResponse(V3Events.evtElse, "Error")),
      new PsmState("WaitPollProof",
		   new PsmResponse(V3Events.msgPollProof,
				   new PsmMethodMsgAction(actionClass,
							  "handleReceivedPollProof")),
		   new PsmResponse(V3Events.evtOk, "VerifyPollProof"),
		   new PsmResponse(V3Events.evtElse, "Error")),
      new PsmState("VerifyPollProof",
		   new PsmMethodAction(actionClass, "handleVerifyPollProof"),
		   new PsmResponse(V3Events.evtOk, "GenerateVote"),
		   new PsmResponse(V3Events.evtElse, "Error")),
      new PsmState("GenerateVote",
		   new PsmMethodAction(actionClass, "handleGenerateVote"),
		   new PsmResponse(V3Events.evtOk, "SendVote"),
		   new PsmResponse(V3Events.evtElse, "Error")),
      new PsmState("SendVote",
		   new PsmMethodAction(actionClass, "handleSendVote"),
		   new PsmResponse(V3Events.evtOk, "WaitRepairRequestOrReceipt"),
		   new PsmResponse(V3Events.evtElse, "Error")),
      new PsmState("WaitRepairRequestOrReceipt",
		   new PsmResponse(V3Events.msgRepairRequest,
				   new PsmMethodMsgAction(actionClass,
							  "handleReceivedRepairRequest")),
		   new PsmResponse(V3Events.msgReceipt,
				   new PsmMethodMsgAction(actionClass,
							  "handleReceivedReceipt")),
		   new PsmResponse(V3Events.evtRepairOk, "SendRepair"),
		   new PsmResponse(V3Events.evtReceiptOk, "ProcessReceipt"),
		   new PsmResponse(V3Events.evtElse, "Error")),
      new PsmState("SendRepair",
		   new PsmMethodAction(actionClass, "handleSendRepair"),
		   new PsmResponse(V3Events.evtOk, "WaitRepairRequestOrReceipt"),
		   new PsmResponse(V3Events.evtElse, "Error")),
      new PsmState("ProcessReceipt",
		   new PsmMethodAction(actionClass, "handleProcessReceipt"),
		   new PsmResponse(V3Events.evtOk, "Finalize"),
		   new PsmResponse(V3Events.evtElse, "Error")),
      new PsmState("Error",
		   new PsmMethodAction(actionClass, "handleError"),
		   new PsmResponse(V3Events.evtOk, "Finalize")),
      new PsmState("Finalize")
    };
    return states;
  }
}
