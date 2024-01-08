/*
 * $Id$
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

import java.io.*;
import java.util.*;

import org.lockss.plugin.*;
import org.lockss.plugin.base.DefaultUrlCacher;
import org.lockss.protocol.V3LcapMessage.PollNak;
import org.lockss.protocol.psm.*;
import org.lockss.protocol.*;
import org.lockss.state.*;
import org.lockss.util.*;

public class PollerActions {

  private static final Logger log = Logger.getLogger("PollerActions");

  @ReturnEvents("evtOk")
  public static PsmEvent handleProveIntroEffort(PsmEvent evt,
                                                PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    if (!ud.isPollActive()) return V3Events.evtError;
    log.debug2("Proving intro effort for poll ID " + ud.getKey());
    // XXX: Generate real poll intro effort, TBD
    byte[] proof = ByteArray.makeRandomBytes(20);
    ud.setIntroEffortProof(proof);
    return V3Events.evtOk;
  }

  /**
   * Send an invitation to participate in this poll.
   * 
   * @param evt  The event that triggered this state.
   * @param interp  The state machine interpreter.
   * @return  The next event.
   */
  @ReturnEvents("evtOk,evtError")
  @SendMessages("msgPoll")
  public static PsmEvent handleSendPoll(PsmEvent evt, PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    if (!ud.isPollActive()) return V3Events.evtError;
    int mod = ud.getPoller().getSampleModulus();
    // For auditing purposes, log the poller nonce.
    log.info("Inviting peer " + ud.getVoterId() + " into poll " +
             ud.getKey() + " with poller nonce " +
             ByteArray.toBase64(ud.getPollerNonce()) + " modulus " + mod);
    try {
      V3LcapMessage msg = ud.makeMessage(V3LcapMessage.MSG_POLL);
      msg.setPollerNonce(ud.getPollerNonce());
      if (mod != 0) {
	byte[] sampleNonce = ud.getPoller().getSampleNonce();
	if (sampleNonce != null && sampleNonce.length > 0) {
	  msg.setModulus(mod);
	  msg.setSampleNonce(sampleNonce);
	} else {
	  log.error("Modulus: " + mod + " but no sample nonce");
	}
      }
      msg.setEffortProof(ud.getIntroEffortProof());
      msg.setVoteDuration(ud.getPoller().getVoteDuration());
      msg.setExpiration(ud.getPoller().getPollMsgExpiration());
      msg.setRetryMax(1);
      ud.sendMessageTo(msg, ud.getVoterId());
      ud.setStatus(V3Poller.PEER_STATUS_WAITING_POLL_ACK);
      // Signal that the peer has been invited.
      IdentityManager idMgr = ud.getPoller().getIdentityManager();
      PeerIdentityStatus status = idMgr.getPeerIdentityStatus(ud.getVoterId());
      if (status != null) {
        status.invitedPeer();
      }
    } catch (IOException ex) {
      log.error("Unable to send message: ", ex);
      return V3Events.evtError;
    }
    return V3Events.evtOk;
  }

  @ReturnEvents("evtOk,evtDeclinePoll")
  public static PsmEvent handleReceivePollAck(PsmMsgEvent evt,
                                              PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    if (!ud.isPollActive()) return V3Events.evtError;
    V3LcapMessage msg = (V3LcapMessage) evt.getMessage();
    log.debug2("Received poll ACK from voter " + ud.getVoterId() + " in poll "
               + ud.getKey());
    // XXX: If either of these is null, the voter is declining to
    // participate in the poll.  But effort is not implemented yet,
    // so for now we're just checking the voter nonce.
    byte[] voterPollAckEffortProof = msg.getEffortProof();
    byte[] voterNonce = msg.getVoterNonce();
    if (voterNonce == null || voterNonce.length == 0) {
      // The peer must send a reason for declining the poll.
      log.info("Peer " + ud.getVoterId() + " sent no voter nonce, "
               + "declining to participate in poll " + ud.getKey()
               + ".  Reason: " + msg.getNak());
      ud.setPollNak(msg.getNak());
      return V3Events.evtDeclinePoll;
    } else {
      ud.setVoterNonce(voterNonce);
      ud.setStatus(V3Poller.PEER_STATUS_ACCEPTED_POLL);
      log.info("Peer " + ud.getVoterId() + " accepted invitation for poll " + 
               ud.getKey() + " and sent voter nonce " +
               ByteArray.toBase64(voterNonce));
      byte[] voterNonce2 = msg.getVoterNonce2();
      if (voterNonce2 != null && voterNonce2.length > 0) {
	log.info("Peer " + ud.getVoterId() + " also sent voter nonce 2 " +
               ByteArray.toBase64(voterNonce2));
	ud.enableSymmetricPoll(voterNonce2);
      }
      // Update the peer status.
      PeerIdentityStatus status = ud.getPoller().getIdentityManager().
                                  getPeerIdentityStatus(msg.getOriginatorId());
      if (status != null) {
        status.joinedPoll();
      }
      
      return V3Events.evtOk;
    }
  }
  
  @ReturnEvents("evtFinalize")
  public static PsmEvent handleDeclinePoll(PsmEvent evt, PsmInterp interp) {
    // Remove the participant from the poll.
    ParticipantUserData ud = getUserData(interp);
    if (!ud.isPollActive()) return V3Events.evtFinalize;
    IdentityManager idMgr = ud.getPoller().getIdentityManager();
    PeerIdentity peer = ud.getVoterId();
    PollNak nak = ud.getPollNak();
    ud.setStatus(V3Poller.PEER_STATUS_DECLINED_POLL, nak.toString());
    ud.removeParticipant(nak);
    log.debug("Peer declined poll: " + peer + ", " + nak.toString());
    idMgr.getPeerIdentityStatus(peer).rejectedPoll(ud.getPollNak());
    return V3Events.evtFinalize;
  }

  @ReturnEvents("evtOk")
  public static PsmEvent handleVerifyPollAckEffort(PsmEvent evt,
                                                   PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    log.debug2("Verifying poll ACK effort for voter " + ud.getVoterId()
              + " in poll " + ud.getKey());
    // XXX: Grab the poll ack effort proof from the Voter State and
    // verify it. Return appropriate event, TBD.
    return V3Events.evtOk;
  }

  @ReturnEvents("evtOk")
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

  @ReturnEvents("evtOk,evtError")
  @SendMessages("msgPollProof")
  public static PsmEvent handleSendPollProof(PsmEvent evt, PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    if (!ud.isPollActive()) return V3Events.evtError;
    log.debug2("Sending poll effort proof for voter " + ud.getVoterId()
               + " in poll " + ud.getKey());
    try {
      V3LcapMessage msg = ud.makeMessage(V3LcapMessage.MSG_POLL_PROOF);
      msg.setEffortProof(ud.getRemainingEffortProof());
      msg.setExpiration(ud.getPoller().getPollMsgExpiration());
      msg.setRetryMax(1);
      ud.sendMessageTo(msg, ud.getVoterId());
    } catch (IOException ex) {
      log.error("Unable to send message: ", ex);
      return V3Events.evtError;
    }
    return V3Events.evtOk;
  }

  @ReturnEvents("evtOk")
  public static PsmEvent handleReceiveNominate(PsmMsgEvent evt,
                                               PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    if (!ud.isPollActive()) return V3Events.evtError;
    V3LcapMessage msg = (V3LcapMessage) evt.getMessage();
    List nominees = msg.getNominees();
    log.debug2("Received outer circle nominations from voter " + ud.getVoterId()
               + " in poll " + ud.getKey());
    ud.setStatus(V3Poller.PEER_STATUS_NOMINATED);
    ud.nominatePeers(nominees);
    return V3Events.evtOk;
  }

  @ReturnEvents("evtOk,evtError")
  @SendMessages("msgVoteRequest")
  public static PsmEvent handleSendVoteRequest(PsmEvent evt,
                                               PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    if (!ud.isPollActive()) return V3Events.evtError;
    V3LcapMessage msg = ud.makeMessage(V3LcapMessage.MSG_VOTE_REQ);
    msg.setExpiration(ud.getPoller().getPollMsgExpiration());
    msg.setRetryMax(1);
    log.debug2("Sending vote request to voter " + ud.getVoterId() + " in poll "
               + ud.getKey());
    // XXX: Implement multiple-vote-message functionality
    try {
      ud.sendMessageTo(msg, ud.getVoterId());
      ud.setStatus(V3Poller.PEER_STATUS_WAITING_VOTE);
    } catch (IOException ex) {
      log.error("Unable to send message: ", ex);
      return V3Events.evtError;
    }
    return V3Events.evtOk;
  }

  @ReturnEvents("evtOk,evtError,evtFinalize")
  public static PsmEvent handleReceiveVote(PsmMsgEvent evt, PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    if (!ud.isPollActive()) return V3Events.evtError;
    V3LcapMessage msg = (V3LcapMessage) evt.getMessage();
    PollNak nak = msg.getNak();
    if (nak != null) {
      ud.setPollNak(nak);
      log.debug2("Received nak (" + nak + ") instead of vote from "
		 + ud.getVoterId() + " in poll " + ud.getKey());
      ud.setStatus(V3Poller.PEER_STATUS_DROPPED_OUT, nak.toString());
      ud.removeParticipant();
      return V3Events.evtFinalize;
    }
    log.debug2("Received vote from " + ud.getVoterId() + " in poll "
               + ud.getKey());
    ud.setStatus(V3Poller.PEER_STATUS_VOTED);
    ud.setVoteComplete(msg.isVoteComplete());
    ud.setVoteBlocks(msg.getVoteBlocks());
    return V3Events.evtOk;
  }

  @ReturnEvents("evtWait")
  public static PsmEvent handleTallyVote(PsmEvent evt, PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    log.debug2("Tallying vote from voter " + ud.getVoterId() + " in poll "
               + ud.getKey());
    ud.tallyVoter();
    return V3Events.evtWait;
  }

  @ReturnEvents("evtOk,evtError")
  public static PsmEvent handleReceiveRepair(PsmMsgEvent evt, PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    if (!ud.isPollActive()) return V3Events.evtError;
    V3LcapMessage msg = (V3LcapMessage)evt.getMessage();
    PeerIdentity pid = msg.getOriginatorId();
    log.debug2("Received repair from voter "
               + pid + " in poll "
               + msg.getKey() + " for URL "
               + msg.getTargetUrl());
    // Apply the repair
    String repairTarget = msg.getTargetUrl();
    try {
      CIProperties props = msg.getRepairProperties();
      if (props == null) {
        log.error("Warning:  No CIProperties included with repair " +
                  "for block " + repairTarget);
        return V3Events.evtError;
      }
      props.setProperty(CachedUrl.PROPERTY_REPAIR_FROM, pid.getIdString());
      props.setProperty(CachedUrl.PROPERTY_REPAIR_DATE,
			Long.toString(TimeBase.nowMs()));
      UrlData urlData = 
          new UrlData(msg.getRepairDataInputStream(), props, repairTarget);
      ArchivalUnit au = ud.getCachedUrlSet().getArchivalUnit();
      UrlCacher uc = au.makeUrlCacher(urlData);

      // Suppress content validation when storing repair from peer
      BitSet repairFetchFlags = uc.getFetchFlags();
      repairFetchFlags.set(UrlCacher.SUPPRESS_CONTENT_VALIDATION);
      uc.setFetchFlags(repairFetchFlags);

      uc.storeContent();
      // Might be a new stem for this AU
      AuState aus = AuUtil.getAuState(au);
      aus.addCdnStem(UrlUtil.getUrlPrefix(repairTarget));

      ud.getPoller().receivedRepair(msg.getTargetUrl());
    } catch (IOException ex) {
      log.error("Error attempting to store repair", ex);
      return V3Events.evtError;
    } finally {
      msg.delete();
    }
    return V3Events.evtOk;
  }

  @ReturnEvents("evtOk,evtError")
  @SendMessages("msgReceipt")
  public static PsmEvent handleSendReceipt(PsmEvent evt, PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    // XXX: Prove receipt effort, TBD. This should probably be in its own state,
    // something like ProveReceiptEffort.
    byte[] proof = ByteArray.makeRandomBytes(20);
    ud.setReceiptEffortProof(proof);
    // Send the message.
    log.debug2("Sending evaluation receipt to voter " + ud.getVoterId()
               + " in poll " + ud.getKey());
    V3LcapMessage msg = ud.makeMessage(V3LcapMessage.MSG_EVALUATION_RECEIPT);
    msg.setEffortProof(ud.getReceiptEffortProof());
    msg.setAgreementHint(ud.getPercentAgreement());
    if (ud.getPoller().hasResultWeightMap()) {
      msg.setWeightedAgreementHint(ud.getWeightedPercentAgreement());
    }
    msg.setExpiration(ud.getPoller().getPollExpiration());
    msg.setRetryMax(1);
    if (ud.isSymmetricPoll() &&
	(ud.getPoller().getAuState().getSubstanceState() !=
	 SubstanceChecker.State.No))  {
      byte[] nonce2 = ud.getVoterNonce2();
      VoteBlocks symmetricVoteBlocks = ud.getSymmetricVoteBlocks();
      msg.setVoterNonce2(nonce2);
      msg.setVoteBlocks(symmetricVoteBlocks);
    }
    try {
      ud.sendMessageTo(msg, ud.getVoterId());
      ud.setStatus(V3Poller.PEER_STATUS_COMPLETE);
    } catch (IOException ex) {
      log.error("Unable to send message: ", ex);
      return V3Events.evtError;
    }
    return V3Events.evtOk;
  }

  @ReturnEvents("evtNoOp")
  public static PsmEvent handleFinalize(PsmEvent evt, PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    ud.getPoller().voterFinished(ud);
    return V3Events.evtNoOp;
  }

  @ReturnEvents("evtOk")
  public static PsmEvent handleError(PsmEvent evt, PsmInterp interp) {
    ParticipantUserData ud = getUserData(interp);
    ud.setStatus(V3Poller.PEER_STATUS_ERROR);
    return V3Events.evtOk;
  }

  /* Convenience method to save typing the cast */
  private static ParticipantUserData getUserData(PsmInterp interp) {
    return (ParticipantUserData)interp.getUserData();
  }
}
