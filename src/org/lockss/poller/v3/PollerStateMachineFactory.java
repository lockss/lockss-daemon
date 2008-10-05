/*
 * $Id: PollerStateMachineFactory.java,v 1.10 2008-02-15 09:10:56 tlipkis Exp $
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

package org.lockss.poller.v3;

import org.lockss.protocol.psm.*;

public class PollerStateMachineFactory implements PsmMachine.Factory {

  /**
   * Obtain a PsmMachine for the Poller state table.
   *
   * @return A PsmMachine representing the Poller state table.
   * @param actionClass A class containing static handler methods for the state
   *          machine to call.
   */
  public PsmMachine getMachine(Class actionClass) {
    return new PsmMachine("Poller", makeStates(actionClass), "ProveIntroEffort");
  }

  /**
   * Mapping of states to response handlers.
   */
  private static PsmState[] makeStates(Class actionClass) {
    PsmState[] states = {
        new PsmState("ProveIntroEffort",
                     new PsmMethodAction(actionClass, "handleProveIntroEffort"),
                     new PsmResponse(V3Events.evtOk, "SendPoll")).setResumable(true),
        new PsmState("SendPoll", new PsmMethodAction(actionClass,
                                                    "handleSendPoll"),
                     new PsmResponse(V3Events.evtOk, "WaitPollAck")),
        new PsmState("WaitPollAck",
                     PsmWait.FOREVER,
                     new PsmResponse(V3Events.msgPollAck,
                                     new PsmMethodMsgAction(actionClass,
                                                           "handleReceivePollAck")),
                     new PsmResponse(V3Events.evtOk, "VerifyPollAckEffort"),
                     new PsmResponse(V3Events.evtDeclinePoll,
                                     new PsmMethodAction(actionClass,
                                                         "handleDeclinePoll")),
                     new PsmResponse(V3Events.evtFinalize,
                                     "FinalizePoller")).setResumable(true),
                              
        new PsmState("VerifyPollAckEffort",
                     new PsmMethodAction(actionClass,
                                        "handleVerifyPollAckEffort"),
                     new PsmResponse(V3Events.evtOk, "ProveRemainingEffort")).setResumable(true),
        new PsmState("ProveRemainingEffort",
                     new PsmMethodAction(actionClass,
                                        "handleProveRemainingEffort"),
                     new PsmResponse(V3Events.evtOk, "SendPollProof")).setResumable(true),
        new PsmState("SendPollProof",
                     new PsmMethodAction(actionClass, "handleSendPollProof"),
                     new PsmResponse(V3Events.evtOk, "WaitNominate")),
        new PsmState("WaitNominate",
                     PsmWait.FOREVER,
                     new PsmResponse(V3Events.msgNominate,
                                     new PsmMethodMsgAction(actionClass,
                                                           "handleReceiveNominate")),
                     new PsmResponse(V3Events.evtOk, "SendVoteRequest")).setResumable(true),
        new PsmState("SendVoteRequest",
                     new PsmMethodAction(actionClass, "handleSendVoteRequest"),
                     new PsmResponse(V3Events.evtOk, "WaitVote")),
        new PsmState("WaitVote",
                     PsmWait.FOREVER,
                     new PsmResponse(V3Events.msgVote,
                                     new PsmMethodMsgAction(actionClass,
                                                           "handleReceiveVote")),
                     new PsmResponse(V3Events.evtError, "FinalizePoller"),
                     new PsmResponse(V3Events.evtOk, "TallyVote")).setResumable(true),
        new PsmState("TallyVote", new PsmMethodAction(actionClass,
                                                      "handleTallyVote"),
                     new PsmResponse(V3Events.msgRepair,
                                     new PsmMethodMsgAction(actionClass,
                                                            "handleReceiveRepair")),
                     new PsmResponse(V3Events.evtWait, PsmWait.FOREVER),
                     new PsmResponse(V3Events.evtVoteIncomplete,
				     "SendVoteRequest"),
                     new PsmResponse(V3Events.evtVoteComplete, "SendReceipt"),
                     new PsmResponse(V3Events.evtOk, "TallyVote")).setResumable(true),
        new PsmState("SendReceipt", new PsmMethodAction(actionClass,
                                                       "handleSendReceipt"),
                     new PsmResponse(V3Events.evtOk, "FinalizePoller")),
        new PsmState("FinalizePoller")
    };

    return states;
  }
}
