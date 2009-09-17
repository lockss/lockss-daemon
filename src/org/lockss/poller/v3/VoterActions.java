/*
 * $Id: VoterActions.java,v 1.26 2009-09-17 02:53:38 tlipkis Exp $
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

import java.io.IOException;
import java.util.*;
import java.security.*;
import org.apache.commons.collections.CollectionUtils;

import org.lockss.config.ConfigManager;
import org.lockss.poller.PollManager;
import org.lockss.poller.PollManager.EventCtr;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;
import org.lockss.util.*;

public class VoterActions {
  private static final Logger log = Logger.getLogger("VoterActions");

  // Start participating in a V3 poll when a POLL message is received
  @ReturnEvents("evtOk")
  public static PsmEvent handleReceivePoll(PsmMsgEvent evt,
                                           PsmInterp interp) {
    V3LcapMessage msg = (V3LcapMessage)evt.getMessage();
    VoterUserData ud = getUserData(interp);
    ud.setDeadline(TimeBase.nowMs() + msg.getDuration());
    ud.setVoteDeadline(TimeBase.nowMs() + msg.getVoteDuration());
    return V3Events.evtOk;
  }

  @ReturnEvents("evtOk")
  public static PsmEvent handleVerifyPollEffort(PsmEvent evt, 
                                                PsmInterp interp) {
    // XXX: Implement effort service.
    return V3Events.evtOk;
  }

  @ReturnEvents("evtOk")
  public static PsmEvent handleProvePollAck(PsmEvent evt, PsmInterp interp) {
    VoterUserData ud = getUserData(interp);
    if (!ud.isPollActive()) return V3Events.evtError;
    // XXX: Implement effort service.
    //
    // If we don't want to participate, just send back a pollack
    // with null effort proof at this point.    
    byte[] pollAckEffort = ByteArray.makeRandomBytes(20);
    ud.setPollAckEffortProof(pollAckEffort);
    return V3Events.evtOk;
  }
  
  @ReturnEvents("evtOk,evtError")
  @SendMessages("msgPollAck")
  public static PsmEvent handleSendPollAck(PsmEvent evt, PsmInterp interp) {
    // Note:  Poller Group membership checking will have happened
    // before this point.  See V3PollFactory.
    
    VoterUserData ud = getUserData(interp);
    if (!ud.isPollActive()) return V3Events.evtError;
    
    V3LcapMessage msg = ud.makeMessage(V3LcapMessage.MSG_POLL_ACK);
    msg.setEffortProof(ud.getPollAckEffortProof());

    // Accept the poll and set status
    ud.setStatus(V3Voter.STATUS_ACCEPTED_POLL);
    msg.setVoterNonce(ud.getVoterNonce());
    msg.setExpiration(ud.getVoter().getHashStartTime());
    msg.setRetryMax(1);

    try {
      // Send message
      ud.sendMessageTo(msg, ud.getPollerId());
      log.debug2("Sent PollAck message to " + ud.getPollerId() + " in poll " 
                 + ud.getPollKey());
      getPollManager(ud).countEvent(EventCtr.Accepted);
      return V3Events.evtOk;
    } catch (Throwable t) {
      log.error("Unable to send message: ", t);
      return V3Events.evtError;
    }
  }

  @ReturnEvents("evtOk")
  public static PsmEvent handleReceivePollProof(PsmMsgEvent evt,
                                                PsmInterp interp) {
    VoterUserData ud = getUserData(interp);
    if (!ud.isPollActive()) return V3Events.evtError;
    V3LcapMessage msg = (V3LcapMessage)evt.getMessage();
    ud.setRemainingEffortProof(msg.getEffortProof());
    return V3Events.evtOk;
  }

  @ReturnEvents("evtOk")
  public static PsmEvent handleVerifyPollProof(PsmEvent evt,
                                               PsmInterp interp) {
    VoterUserData ud = getUserData(interp);
    if (!ud.isPollActive()) return V3Events.evtError;
    // XXX: Implement effort service
    // After effort has been proven, prepare to nominate some peers.
    ud.nominatePeers();
    return V3Events.evtOk;
  }

  @ReturnEvents("evtOk,evtError")
  @SendMessages("msgNominate")
  public static PsmEvent handleSendNominate(PsmEvent evt, PsmInterp interp) {
    VoterUserData ud = getUserData(interp);
    if (!ud.isPollActive()) return V3Events.evtError;
    V3LcapMessage msg = ud.makeMessage(V3LcapMessage.MSG_NOMINATE);
    msg.setNominees(ud.getNominees());
    msg.setExpiration(ud.getVoter().getHashStartTime());
    msg.setRetryMax(1);
    try {
      ud.sendMessageTo(msg, ud.getPollerId());
    } catch (IOException ex) {
      log.error("Unable to send message: ", ex);
      return V3Events.evtError;
    }
    return V3Events.evtOk;
  }

  @ReturnEvents("evtOk,evtError")
  public static PsmEvent handleGenerateVote(PsmEvent evt, PsmInterp interp) {
    VoterUserData ud = getUserData(interp);
    if (!ud.isPollActive()) return V3Events.evtError;
    try {
      if (ud.generateVote()) {
        ud.setStatus(V3Voter.STATUS_HASHING);
        return V3Events.evtOk;
      } else {
        return V3Events.evtError;
      }
    } catch (NoSuchAlgorithmException ex) {
      log.error("No such hashing algorithm: " + ex.getMessage());
      return V3Events.evtError;
    }
  }

  @ReturnEvents("evtReadyToVote,evtWait")
  public static PsmEvent handleReceiveVoteRequest(PsmMsgEvent evt,
                                                  PsmInterp interp) {
    VoterUserData ud = getUserData(interp);
    if (!ud.isPollActive()) return V3Events.evtError;
    // If we've finished hashing, vote, else wait
    ud.voteRequested(true);
    if (ud.hashingDone()) {
      return V3Events.evtReadyToVote;
    } else {
      return V3Events.evtWait;
    }
  }

  @ReturnEvents("evtReadyToVote,evtWait")
  public static PsmEvent handleHashingDone(PsmEvent evt, PsmInterp interp) {
    VoterUserData ud = getUserData(interp);
    if (!ud.isPollActive()) return V3Events.evtError;
    // If we've gotten a vote request, vote, else wait
    ud.hashingDone(true);
    if (ud.voteRequested()) {
      return V3Events.evtReadyToVote;
    } else {
      return V3Events.evtWait;
    }
  }

  static PollManager getPollManager(VoterUserData ud) {
    return ud.getVoter().getPollManager();
  }

  @ReturnEvents("evtOk,evtError")
  @SendMessages("msgVote")
  public static PsmEvent handleSendVote(PsmEvent evt, PsmInterp interp) {
    VoterUserData ud = getUserData(interp);
    if (!ud.isPollActive()) return V3Events.evtError;
    V3LcapMessage msg = ud.makeMessage(V3LcapMessage.MSG_VOTE);
    // XXX: Fix when multiple-message voting is supported.
    msg.setVoteComplete(true);
    msg.setVoteBlocks(ud.getVoteBlocks());
    long voteDeadline = ud.getVoteDeadline();
    msg.setExpiration(voteDeadline);

    CompoundLinearSlope voteRetryIntervalDurationCurve =
      getPollManager(ud).getVoteRetryIntervalDurationCurve();
    if (voteRetryIntervalDurationCurve  != null) {
      long voteRemaining = voteDeadline - TimeBase.nowMs();
      msg.setRetryInterval((long)voteRetryIntervalDurationCurve.getY(voteRemaining));
      msg.setRetryMax(3);
    } else {
      msg.setRetryMax(1);
    }
    // Actually cast our vote.
    try {
      ud.sendMessageTo(msg, ud.getPollerId());
      ud.setStatus(V3Voter.STATUS_VOTED);
      getPollManager(ud).countEvent(EventCtr.Voted);
    } catch (IOException ex) {
      log.error("Unable to send message: ", ex);
      return V3Events.evtError;
    }
    return V3Events.evtOk;
  }

  @ReturnEvents("evtRepairRequestOk,evtNoSuchRepair")
  public static PsmEvent handleReceiveRepairRequest(PsmMsgEvent evt,
                                                    PsmInterp interp) {
    VoterUserData ud = getUserData(interp);
    if (!ud.isPollActive()) return V3Events.evtNoSuchRepair;
    V3Voter voter = ud.getVoter();
    V3LcapMessage msg = (V3LcapMessage)evt.getMessage();
    String targetUrl = msg.getTargetUrl();
    CachedUrlSet cus = ud.getCachedUrlSet();
    if (cus.containsUrl(targetUrl) &&
        voter.serveRepairs(msg.getOriginatorId(), voter.getAu(), targetUrl)) {
      // I have this repair and I'm willing to serve it.
      log.debug2("Accepting repair request from " + ud.getPollerId() +
                 " for URL: " + targetUrl);
      ud.setRepairTarget(targetUrl);
      return V3Events.evtRepairRequestOk;
    } else {
      // I don't have this repair
      log.error("No repair available to serve for URL: " + targetUrl);
      return V3Events.evtNoSuchRepair;
    }
  }

  @ReturnEvents("evtOk,evtError")
  @SendMessages("msgRepair")
  public static PsmEvent handleSendRepair(PsmEvent evt, PsmInterp interp) {
    VoterUserData ud = getUserData(interp);
    if (!ud.isPollActive()) return V3Events.evtError;
    log.debug2("Sending repair to " + ud.getPollerId() + " for URL : " +
               ud.getRepairTarget());
    CachedUrl cu = null;
    try {
      V3LcapMessage msg = ud.makeMessage(V3LcapMessage.MSG_REPAIR_REP);
      ArchivalUnit au = ud.getCachedUrlSet().getArchivalUnit();
      cu = au.makeCachedUrl(ud.getRepairTarget());
      msg.setTargetUrl(ud.getRepairTarget());
      msg.setRepairDataLength(cu.getContentSize());
      msg.setRepairProps(cu.getProperties());
      msg.setInputStream(cu.getUnfilteredInputStream());
      msg.setExpiration(ud.getDeadline());
      msg.setRetryMax(1);
      ud.sendMessageTo(msg, ud.getPollerId());
    } catch (IOException ex) {
      log.error("Unable to send message: ", ex);
      return V3Events.evtError;
    } finally {
      AuUtil.safeRelease(cu);
    }
    return V3Events.evtOk;
  }

  @ReturnEvents("evtReceiptOk")
  public static PsmEvent handleReceiveReceipt(PsmMsgEvent evt, PsmInterp interp) {
    VoterUserData ud = getUserData(interp);
    getPollManager(ud).countEvent(EventCtr.ReceivedVoteReceipt);
    V3LcapMessage msg = (V3LcapMessage)evt.getMessage();
    double agreementHint = msg.getAgreementHint();
    log.debug3("Receipt agreement " + agreementHint);
    ud.setAgreementHint(agreementHint);
    // Remember the agreement hint in the agreement history for the AU
    PeerIdentity poller = ud.getPollerId();
    IdentityManager idmgr = ud.getVoter().getIdentityManager();
    idmgr.signalPartialAgreementHint(poller, ud.getVoter().getAu(),
				     (float) agreementHint);
    return V3Events.evtReceiptOk;
  }

  @ReturnEvents("evtOk")
  public static PsmEvent handleProcessReceipt(PsmEvent evt, PsmInterp interp) {
    VoterUserData ud = getUserData(interp);
    if (!ud.isPollActive()) return V3Events.evtOk;
    // XXX: Once the receipt is a bit more interesting, use it here.
    ud.getVoter().stopPoll(V3Voter.STATUS_COMPLETE);
    return V3Events.evtOk;
  }

  @ReturnEvents("evtOk")
  public static PsmEvent handleError(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  private static VoterUserData getUserData(PsmInterp interp) {
    return (VoterUserData)interp.getUserData();
  }
}
