/*
 * $Id: PollerActions.java,v 1.9 2006-03-01 02:50:14 smorabito Exp $
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

import java.io.*;
import java.util.*;

import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.UrlCacher;
import org.lockss.plugin.base.BaseUrlCacher;
import org.lockss.protocol.psm.*;
import org.lockss.protocol.*;
import org.lockss.util.*;

public class PollerActions {

  private static final Logger log = Logger.getLogger("PollerActions");

  public static PsmEvent handleProveIntroEffort(PsmEvent evt,
                                                PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    log.debug2("Proving intro effort for poll ID " + ud.getKey());
    // XXX: Generate real poll intro effort, TBD
    byte[] proof = ByteArray.makeRandomBytes(20);
    ud.setIntroEffortProof(proof);
    return V3Events.evtOk;
  }

  public static PsmEvent handleSendPoll(PsmEvent evt, PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    log.debug2("Sending poll to participant " + ud.getVoterId() + " in poll "
               + ud.getKey());
    try {
      ud.sendMessageTo(V3LcapMessageFactory.makePollMsg(ud), ud.getVoterId());
    } catch (IOException ex) {
      log.error("Unable to send message: ", ex);
      return V3Events.evtError;
    }
    return V3Events.evtOk;
  }

  public static PsmEvent handleReceivePollAck(PsmMsgEvent evt,
                                              PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    V3LcapMessage msg = (V3LcapMessage) evt.getMessage();
    log.debug2("Received poll ACK from voter " + ud.getVoterId() + " in poll "
               + ud.getKey());
    // XXX: If either of these is null, the voter is declining to
    // participate in the poll.  But effort is not implemented yet,
    // so for now we're just checking the voter nonce.
    byte[] voterPollAckEffortProof = msg.getEffortProof();
    byte[] voterNonce = msg.getVoterNonce();
    if (voterNonce == null) {
      log.debug("Peer " + ud.getVoterId() + " sent no voter nonce, "
                + "declining to participate in the poll.");
      // Remove the peer from the poll.
      ud.removeParticipant();
      return V3Events.evtError;
    } else {
      ud.setVoterNonce(voterNonce);
      return V3Events.evtOk;
    }
  }

  public static PsmEvent handleVerifyPollAckEffort(PsmEvent evt,
                                                   PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    log.debug2("Verifying poll ACK effort for voter " + ud.getVoterId()
              + " in poll " + ud.getKey());
    // XXX: Grab the poll ack effort proof from the Voter State and
    // verify it. Return appropriate event, TBD.
    return V3Events.evtOk;
  }

  public static PsmEvent handleProveRemainingEffort(PsmEvent evt,
                                                    PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);

    log.debug2("Proving remaining effort for voter " + ud.getVoterId()
               + " in poll " + ud.getKey());
    // XXX: Generate remaining effort, TBD
    byte[] proof = ByteArray.makeRandomBytes(20);
    ud.setRemainingEffortProof(proof);

    return V3Events.evtOk;
  }

  public static PsmEvent handleSendPollProof(PsmEvent evt, PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    log.debug2("Sending poll effort proof for voter " + ud.getVoterId()
               + " in poll " + ud.getKey());
    try {
      ud.sendMessageTo(V3LcapMessageFactory.makePollProofMsg(ud),
                       ud.getVoterId());
    } catch (IOException ex) {
      log.error("Unable to send message: ", ex);
      return V3Events.evtError;
    }
    return V3Events.evtOk;
  }

  public static PsmEvent handleReceiveNominate(PsmMsgEvent evt,
                                               PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    V3LcapMessage msg = (V3LcapMessage) evt.getMessage();
    List nominees = msg.getNominees();
    log.debug2("Received outer circle nominations from voter " + ud.getVoterId()
               + " in poll " + ud.getKey());
    ud.nominatePeers(nominees);
    return V3Events.evtOk;
  }

  public static PsmEvent handleReceiveVoterReadyToVote(PsmMsgEvent evt,
                                                       PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    V3LcapMessage msg = (V3LcapMessage) evt.getMessage();
    // XXX -- anything we need to verify here?
    return V3Events.evtOk;
  }

  public static PsmEvent handleSendVoteRequest(PsmEvent evt,
                                               PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    V3LcapMessage msg = V3LcapMessageFactory.makeVoteRequestMsg(ud);
    log.debug2("Sending vote request to voter " + ud.getVoterId() + " in poll "
               + ud.getKey());
    // XXX: Implement multiple-vote-message functionality
    try {
      ud.sendMessageTo(msg, ud.getVoterId());
    } catch (IOException ex) {
      log.error("Unable to send message: ", ex);
      return V3Events.evtError;
    }
    return V3Events.evtOk;
  }

  public static PsmEvent handleReceiveVote(PsmMsgEvent evt, PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    V3LcapMessage msg = (V3LcapMessage) evt.getMessage();
    log.debug2("Received vote from voter " + ud.getVoterId() + " in poll "
               + ud.getKey());
    ud.setVoteComplete(msg.isVoteComplete());
    ud.setVoteBlocks(msg.getVoteBlocks());
    return V3Events.evtOk;
  }

  public static PsmEvent handleTallyVote(PsmEvent evt, PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    log.debug2("Tallying vote from voter " + ud.getVoterId() + " in poll "
               + ud.getKey());
    ud.tallyVoter();
    return V3Events.evtWaitBlockComplete;
  }

  public static PsmEvent handleReceiveRepair(PsmMsgEvent evt, PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    V3LcapMessage msg = (V3LcapMessage)evt.getMessage();
    log.debug2("Received repair from voter "
               + msg.getOriginatorId() + " in poll "
               + msg.getKey() + " for URL "
               + msg.getTargetUrl());
    // Apply the repair
    String repairTarget = msg.getTargetUrl();
    UrlCacher uc =
      ud.getCachedUrlSet().getArchivalUnit().makeUrlCacher(repairTarget);
    try {
      CIProperties props = msg.getRepairProperties();
      if (props == null) {
        log.warning("Warning:  No CIProperties included with repair " +
                        "for block " + repairTarget);
        // This probably isn't right
        props = uc.getUncachedProperties();
      }
      uc.storeContent(msg.getRepairDataInputStream(),
                      props);
    } catch (IOException ex) {
      log.error("Error attempting to store repair", ex);
      return V3Events.evtError;
    }

    ud.getPoller().receivedRepair(msg.getTargetUrl(), msg.getOriginatorId());
    return V3Events.evtOk;
  }

  public static PsmEvent handleSendReceipt(PsmEvent evt, PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    // XXX: Prove receipt effort, TBD. This should probably be in its own state,
    // something like ProveReceiptEffort.
    byte[] proof = ByteArray.makeRandomBytes(20);
    ud.setReceiptEffortProof(proof);
    // Send the message.
    log.debug2("Sending evaluation receipt to voter " + ud.getVoterId()
               + " in poll " + ud.getKey());
    try {
      ud.sendMessageTo(V3LcapMessageFactory.makeEvaluationReceiptMsg(ud),
                       ud.getVoterId());
    } catch (IOException ex) {
      log.error("Unable to send message: ", ex);
      return V3Events.evtError;
    }
    return V3Events.evtOk;
  }

  public static PsmEvent handleError(PsmEvent evt, PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    ud.handleError();
    return V3Events.evtOk;
  }

  /* Convenience method to save typing the cast */
  private static ParticipantUserData getUserData(PsmInterp interp) {
    return (ParticipantUserData)interp.getUserData();
  }
}
