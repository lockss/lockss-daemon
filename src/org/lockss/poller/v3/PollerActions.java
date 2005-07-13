/*
 * $Id: PollerActions.java,v 1.3 2005-07-13 07:53:06 smorabito Exp $
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

import java.util.*;

import org.lockss.protocol.psm.*;
import org.lockss.protocol.*;
import org.lockss.util.*;

public class PollerActions {

  private static final Logger log = Logger.getLogger("PollerActions");

  public static PsmEvent handleInitialize(PsmEvent evt, PsmInterp interp) {
    // XXX: This state is probably not necessary.
    log.debug("Initializing poller");
    return V3Events.evtOk;
  }

  public static PsmEvent handleProveIntroEffort(PsmEvent evt, PsmInterp interp) {
    V3VoterState vs = getVoterState(interp);
    log.debug("Proving intro effort for poll ID " + vs.getPollKey());
    // XXX: Generate real poll intro effort, TBD
    byte[] proof = ByteArray.makeRandomBytes(20);
    vs.setIntroEffortProof(proof);
    return V3Events.evtOk;
  }

  public static PsmEvent handleSendPoll(PsmEvent evt, PsmInterp interp) {
    V3VoterState vs = getVoterState(interp);
    log.debug("Sending poll to participant " + vs.getVoterId() +
	      " in poll " + vs.getPollKey());
    vs.getPoller().sendMessageTo(V3LcapMessageFactory.makePollMsg(vs),
				 vs.getVoterId());
    return V3Events.evtOk;
  }

  public static PsmEvent handleReceivePollAck(PsmMsgEvent evt, PsmInterp interp) {
    V3VoterState vs = getVoterState(interp);
    log.debug("Received poll ACK from voter " + vs.getVoterId() +
	      " in poll " + vs.getPollKey());
    // XXX: Store the poll ack in the Voter State, TBD.
    return V3Events.evtOk;
  }

  public static PsmEvent handleVerifyPollAckEffort(PsmEvent evt, PsmInterp interp) {
    V3VoterState vs = getVoterState(interp);
    log.debug("Verifying poll ACK effort for voter " + vs.getVoterId() +
	      " in poll " + vs.getPollKey());
    // XXX: Grab the poll ack effort proof from the Voter State and
    // verify it.  Return appropriate event, TBD.
    return V3Events.evtOk;
  }

  public static PsmEvent handleProveRemainingEffort(PsmEvent evt,
						    PsmInterp interp) {
    V3VoterState vs = getVoterState(interp);
    log.debug("Proving remaining effort for voter " + vs.getVoterId() +
	      " in poll " + vs.getPollKey());
    // XXX: Generate remaining effort, TBD
    byte[] proof = ByteArray.makeRandomBytes(20);
    vs.setRemainingEffortProof(proof);
    return V3Events.evtOk;
  }

  public static PsmEvent handleSendPollProof(PsmEvent evt, PsmInterp interp) {
    V3VoterState vs = getVoterState(interp);
    log.debug("Sending poll effort proof for voter " + vs.getVoterId() +
	      " in poll " + vs.getPollKey());
    vs.getPoller().sendMessageTo(V3LcapMessageFactory.makePollProofMsg(vs),
				 vs.getVoterId());
    return V3Events.evtOk;
  }

  public static PsmEvent handleReceiveNominate(PsmMsgEvent evt, PsmInterp interp) {
    V3VoterState vs = getVoterState(interp);
    V3LcapMessage msg = (V3LcapMessage)evt.getMessage();
    log.debug("Received outer circle nominations from voter " + vs.getVoterId() +
	      " in poll " + vs.getPollKey());
    List nominees = msg.getNominees();
    vs.setNominees(nominees);
    vs.getHandler().handleNominate(vs.getVoterId(), nominees);
    return V3Events.evtOk;
  }

  public static PsmEvent handleSendVoteRequest(PsmEvent evt, PsmInterp interp) {
    V3VoterState vs = getVoterState(interp);
    log.debug("Sending vote request to voter " + vs.getVoterId() +
	      " in poll " + vs.getPollKey());
    V3LcapMessage msg = V3LcapMessageFactory.makeVoteRequestMsg(vs);

    // XXX: Implement multiple-vote-message functionality
    
    vs.getPoller().sendMessageTo(msg, vs.getVoterId());
    return V3Events.evtOk;
  }

  public static PsmEvent handleReceiveVote(PsmMsgEvent evt, PsmInterp interp) {
    V3VoterState vs = getVoterState(interp);
    V3LcapMessage msg = (V3LcapMessage)evt.getMessage();
    log.debug("Received vote from voter " + vs.getVoterId() +
	      " in poll " + vs.getPollKey());
    // XXX: Stuff the participant's vote into the voter state object. This
    //      needs improvement.
    vs.setVote(msg);
    return V3Events.evtOk;
  }

  public static PsmEvent handleTallyVote(PsmEvent evt, PsmInterp interp) {
    V3VoterState vs = getVoterState(interp);
    log.debug("Tallying vote from voter " + vs.getVoterId() +
	      " in poll " + vs.getPollKey());
    vs.getHandler().handleTally(vs.getVoterId());
    return V3Events.evtWaitTallyComplete;
  }


  public static PsmEvent handleProveRepairEffort(PsmEvent evt, PsmInterp interp) {
    V3VoterState vs = getVoterState(interp);
    log.debug("Proving repair effort for voter " + vs.getVoterId() +
	      " in poll " + vs.getPollKey());
    // XXX: Generate real repair effort proof, TBD
    byte[] proof = ByteArray.makeRandomBytes(20);
    vs.setRepairEffortProof(proof);
    return V3Events.evtOk;
  }

  public static PsmEvent handleSendRepairRequest(PsmEvent evt, PsmInterp interp) {
    V3VoterState vs = getVoterState(interp);
    log.debug("Sending repair request to voter " + vs.getVoterId() +
	      " in poll " + vs.getPollKey());
    vs.getPoller().sendMessageTo(V3LcapMessageFactory.makeRepairRequestMsg(vs),
				 vs.getVoterId());
    return V3Events.evtOk;
  }

  public static PsmEvent handleReceiveRepair(PsmMsgEvent evt, PsmInterp interp) {
    // XXX: Implement.
    V3VoterState vs = getVoterState(interp);
    V3LcapMessage msg = (V3LcapMessage)evt.getMessage();
    log.debug("Received repair from voter " + vs.getVoterId() +
	      " in poll " + vs.getPollKey());
    return V3Events.evtOk;
  }

  public static PsmEvent handleProcessRepair(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    V3VoterState vs = getVoterState(interp);
    log.debug("Processing repair from voter " + vs.getVoterId() +
	      " in poll " + vs.getPollKey());
    return V3Events.evtOk;
  }

  public static PsmEvent handleSendReceipt(PsmEvent evt, PsmInterp interp) {
    V3VoterState vs = getVoterState(interp);
    // XXX: Prove receipt effort, TBD.  This should probably be in its own state,
    // something like ProveReceiptEffort.
    byte[] proof = ByteArray.makeRandomBytes(20);
    vs.setReceiptEffortProof(proof);
    // Send the message.
    log.debug("Sending evaluation receipt to voter " + vs.getVoterId() +
	      " in poll " + vs.getPollKey());
    vs.getPoller().sendMessageTo(V3LcapMessageFactory.makeEvaluationReceiptMsg(vs),
				 vs.getVoterId());
    return V3Events.evtOk;
  }

  public static PsmEvent handleError(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    V3VoterState vs = getVoterState(interp);
    log.debug("Error from voter " + vs.getVoterId() +
	      " in poll " + vs.getPollKey());
    return V3Events.evtOk;
  }

  private static V3VoterState getVoterState(PsmInterp interp) {
    return (V3VoterState)interp.getUserData();
  }
}
