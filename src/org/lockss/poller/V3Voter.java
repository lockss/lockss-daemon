/*
 * $Id: V3Voter.java,v 1.1.2.12 2004-10-07 02:17:05 dshr Exp $
 */

/*

Copyright (c) 2004 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.poller;

import java.io.*;
import java.security.*;

import org.lockss.daemon.*;
import org.lockss.effort.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;

// import org.mortbay.util.*;  // For B64 encoding stuff?

/**
 * <p>This classs represents the participation of this peer as a voter
 * in a V3 poll called by some other peer.</p>
 * <p>A V3Voter goes through the following stages. (1) generate and send
 * a PollAck message. (2) wait for a PollProof messge.  (3) generate and
 * send a Vote message.  (4) wait for a RepairRequest message.  (5)
 * generate and send a Repair message.  (6) wait for an EvaluationReceipt
 * message.  (7) process an EvaluationReceipt. (8) cleanup and exit.</p>
 * <p>Note that the poll is likely to spend a long time in state 4 awaiting
 * a RepairRequest and in state 6.  The wait in state 2 is likely to be
 * extended and the process of generating a Vote in state 2 is expensive.</p>
 * @author David Rosenthal
 * @version 1.0
 */

public class V3Voter extends V3Poll {

  public static final int STATE_INITIALIZING = 0;
  public static final int STATE_SENDING_POLL_ACK = 1;
  public static final int STATE_WAITING_POLL_PROOF = 2;
  public static final int STATE_SENDING_VOTE = 3;
  public static final int STATE_WAITING_REPAIR_REQ = 4;
  public static final int STATE_SENDING_REPAIR = 5;
  public static final int STATE_WAITING_RECEIPT = 6;
  public static final int STATE_PROCESS_RECEIPT = 7;
  public static final int STATE_FINALIZING = 8;
  private static final String[] stateName = {
    "Initializing",
    "SendingPollAck",
    "WaitingPollProof",
    "SendingVote",
    "WaitingRepairReq",
    "SendingRepair",
    "WaitingReceipt",
    "ProcessReceipt",
    "Finalizing",
  };

  static Logger log=Logger.getLogger("V3Voter");

  private int m_state = STATE_INITIALIZING;
  private EffortService theEffortService = null;
  /**
   * create a new poll called by some other peer
   *
   * @param pollspec the <code>PollSpec</code> on which this poll will operate
   * @param pm the pollmanager
   * @param orig the <code>PeerIdentity</code> of the caller
   * @param challenge a <code>byte[]</code> with the poller's nonce
   * @param duration the duration of the poll
   * @param hashAlg the hash algorithm to use
   */
  V3Voter(PollSpec pollspec,
	  PollManager pm,
	  PeerIdentity orig,
	  byte[] challenge,
	  long duration,
	  String hashAlg) {
    super(pollspec, pm, orig, challenge, duration, hashAlg);

    // XXX
    theEffortService = pm.getEffortService(pollspec);
  }

  // Implementations of abstract methods from V3Poll

  /**
   * receive a message that is part of this poll
   */
  synchronized void receiveMessage(LcapMessage msg) {
    if (!(msg instanceof V3LcapMessage)) {
      log.error(msg.toString() + " is not V3");
      return;
    }
    V3LcapMessage v3msg = (V3LcapMessage) msg;
    switch (m_state) {
    default:
      log.error("Bad state " + m_state);
      m_pollstate = ERR_IO; // XXX choose better
      stopPoll();
      break;
    case STATE_SENDING_POLL_ACK:
    case STATE_SENDING_VOTE:
    case STATE_SENDING_REPAIR:
    case STATE_PROCESS_RECEIPT:
    case STATE_FINALIZING:
      log.debug("Unexpected message " + msg.toString() + " in state " +
		stateName[m_state]);
      m_pollstate = ERR_IO; // XXX choose better
      stopPoll();
      break;
    case STATE_INITIALIZING:
      doPollMessage(v3msg);
      break;
    case STATE_WAITING_POLL_PROOF:
      doPollProofMessage(v3msg);
      break;
    case STATE_WAITING_REPAIR_REQ:
      doRepairReqMessage(v3msg);
      break;
    case STATE_WAITING_RECEIPT:
      doReceiptMessage(v3msg);
      break;
    }
  }

  /**
   * start the poll.
   */
  void startPoll() {
    if (m_pollstate != PS_INITING) {
      m_pollstate = ERR_IO; // XXX choose better
      stopPoll();
      return;
    }
    // XXX
    log.debug3("scheduling poll to complete by " + m_deadline);
    TimerQueue.schedule(m_deadline, new PollTimerCallback(), this);
    m_pollstate = PS_WAIT_HASH;

  }

  /**
   * finish the poll once the deadline has expired. we update our poll record
   * and prevent any more activity in this poll.
   */
  void stopPoll() {
    if(isErrorState()) {
      log.debug("poll stopped with error: " + ERROR_STRINGS[ -m_pollstate]);
    }
    else {
      m_pollstate = BasePoll.PS_COMPLETE;
    }
    m_pollmanager.closeThePoll(m_key);
    log.debug3("closed the poll:" + m_key);
  }

  // End abstract methods of V3Poll

  // Methods that process messages and potentially change state

  private void doPollMessage(V3LcapMessage msg) {
    if (msg.getOpcode() != V3LcapMessage.MSG_POLL) {
      log.warning("Expecting a Poll but got: " + msg.toString());
      m_pollstate = ERR_IO; // XXX choose better
      m_state = STATE_FINALIZING;
      stopPoll();
      return;
    }
    //  First apply admission control
    if (!passAdmissionControl(msg)) {
      log.warning("Message rejected by admission control: " + msg.toString());
      m_pollstate = ERR_IO; // XXX choose better
      m_state = STATE_FINALIZING;
      stopPoll();
      return;
    }
    //  Second verify the effort in the Poll message
    EffortService.ProofCallback cb = new PollAckEffortCallback(m_pollmanager);
    EffortService.Proof ep = null;
    EffortService es = null;
    if (false) {
      // XXX
      ep = msg.getEffortProof();
      es = ep.getEffortService();
    } else {
      // XXX
      es = theEffortService;
      ep = es.makeProof();
    }
    Deadline timer = msg.getDeadline();
    Serializable cookie = msg.getKey();
    if (es.verifyProof(ep, timer, cb, cookie)) {
      // effort verification for Poll successfuly scheduled
      log.debug("Scheduled verification callback in " +
		timer.getRemainingTime() + " for " + ((String)cookie));
      m_state = STATE_SENDING_POLL_ACK;
    } else {
      log.warning("could not schedule effort verification " + ep.toString() +
		  " for " + msg.toString());
      m_state = STATE_FINALIZING;
    }
  }

  private boolean passAdmissionControl(V3LcapMessage msg) {
    //  XXX
    return true;
  }

  private void doPollProofMessage(V3LcapMessage msg) {
    if (msg.getOpcode() != V3LcapMessage.MSG_POLL_PROOF) {
      log.warning("Expecting a PollProof but got: " + msg.toString());
      m_pollstate = ERR_IO; // XXX choose better
      m_state = STATE_FINALIZING;
      stopPoll();
      return;
    }
    //  Start by verifying the effort proof in the message
    EffortService.ProofCallback cb =
      new VoteEffortCallback(m_pollmanager);
    EffortService.Proof ep = null;
    EffortService es = null;
    if (false) {
      // XXX
      ep = msg.getEffortProof();
      es = ep.getEffortService();
    } else {
      // XXX
      es = theEffortService;
      ep = es.makeProof();
    }
    Deadline timer = msg.getDeadline();
    Serializable cookie = msg.getKey();
    if (es.verifyProof(ep, timer, cb, cookie)) {
      // effort verification for PollProof successfuly scheduled
      log.debug("Scheduled verification callback in " +
		timer.getRemainingTime() + " for " + ((String)cookie));
      m_state = STATE_SENDING_VOTE;
    } else {
      log.warning("could not schedule effort verification " + ep.toString() +
		  " for " + msg.toString());
      m_state = STATE_FINALIZING;
    }
  }

  private void doRepairReqMessage(V3LcapMessage msg) {
    switch (msg.getOpcode()) {
    default:
      log.warning("Expecting a RepairReq but got: " + msg.toString());
      m_pollstate = ERR_IO; // XXX choose better
      m_state = STATE_FINALIZING;
      stopPoll();
      return;
    case V3LcapMessage.MSG_EVALUATION_RECEIPT:
      doReceiptMessage(msg);
      return;
    case V3LcapMessage.MSG_REPAIR_REQ:
      break;
    }
    long delay = 500; // XXX
    TimerQueue.schedule(Deadline.in(delay), new RepairTimerCallback(), this);
    // XXX actually supply repair
    m_state = STATE_SENDING_REPAIR;
  }

  private void doReceiptMessage(V3LcapMessage msg) {
    if (msg.getOpcode() != V3LcapMessage.MSG_EVALUATION_RECEIPT) {
      log.warning("Expecting an EvaluationReceipt but got: " + msg.toString());
      m_pollstate = ERR_IO; // XXX choose better
      m_state = STATE_FINALIZING;
      stopPoll();
      return;
    }
    long delay = 500; // XXX
    TimerQueue.schedule(Deadline.in(delay), new ReceiptTimerCallback(), this);
    // XXX do an EvaluationReceipt?
    m_state = STATE_PROCESS_RECEIPT;
  }

  public int getPollState() {
    return m_state;
  }

  /**
   * create a human readable string representation of this poll
   * @return a String
   */
  public String toString() {
    String pollType = "V3";
    StringBuffer sb = new StringBuffer("[Voter: ");
    sb.append(pollType);
    sb.append(" url set:");
    sb.append(" ");
    sb.append(m_cus.toString());
    if (m_msg != null) {
      sb.append(" ");
      sb.append(m_msg.getOpcodeString());
    }
    sb.append(" key: ");
    sb.append(m_key);
    sb.append(" to go ");
    sb.append(m_deadline.getRemainingTime());
    sb.append(" state: ");
    sb.append(stateName[m_state]);
    sb.append("]");
    return sb.toString();
  }

  class PollAckEffortCallback implements EffortService.ProofCallback {
    private PollManager pollMgr = null;
    PollAckEffortCallback(PollManager pm) {
      pollMgr = pm;
    }
    /**
     * Called to indicate generation of a proof of effort for
     * the PollAck message is complete.
     * @param ep the EffortProof in question
     * @param cookie used to disambiguate callbacks
     * @param e the exception that caused the effort proof to fail
     */
    public void generationFinished(EffortService.Proof ep,
				   Deadline timer,
				   Serializable cookie,
				   Exception e) {
      if (e != null) {
	log.debug("PollAckEffortProofCallback: " + ((String) cookie) +
		  " threw " + e);
	m_state = STATE_FINALIZING;
	m_pollstate = ERR_IO; // XXX choose better
	stopPoll();
      } else {
	log.debug("PollAckEffortProofCallback: " + ((String) cookie));
	m_state = STATE_WAITING_POLL_PROOF;
	// XXX send the proof and the message
      }
    }

    /**
     * Called to indicate verification of a proof of effort is complete.
     * @param ep the <code>EffortService.Proof</code> in question
     * @param cookie used to disambiguate callbacks
     * @param e the exception that caused the effort proof to fail
     */
    public void verificationFinished(EffortService.Proof ep,
				     Deadline timer,
				     Serializable cookie,
				     Exception e) {
      if (e != null) {
	log.debug("Poll effort verification threw: " + e);
	m_pollstate = ERR_IO; // XXX choose better
	m_state = STATE_FINALIZING;
	stopPoll();
	return;
      }
      if (!ep.isVerified()) {
	log.debug("Poll effort verification failed");
	m_pollstate = ERR_IO; // XXX choose better
	m_state = STATE_FINALIZING;
	stopPoll();
	return;
      }
      //  Poll effort verified,  now generate effort for reply
      log.debug("Poll effort verification succeeds with " +
		timer.getRemainingTime() + " to go");
      EffortService es = ep.getEffortService();
      //  XXX should get spec for proof from message
      EffortService.Proof pollAckProof = es.makeProof();
      if (es.proveEffort(pollAckProof, timer, this, cookie)) {
	log.debug("Scheduled generation callback in " +
		  timer.getRemainingTime() + " for " + ((String)cookie));
      } else {
	log.warning("could not schedule effort generation " +
		    pollAckProof.toString() + " for " + cookie);
	m_pollstate = ERR_IO; // XXX choose better
	m_state = STATE_FINALIZING;
	stopPoll();
      }
      return;
    }
  }

  class VoteEffortCallback implements EffortService.ProofCallback {
    private PollManager pollMgr = null;
    VoteEffortCallback(PollManager pm) {
      pollMgr = pm;
    }
    /**
     * Called to indicate generation of a proof of effort for
     * the Vote message is complete.
     * @param ep the EffortProof in question
     * @param cookie used to disambiguate callbacks
     * @param e the exception that caused the effort proof to fail
     */
    public void generationFinished(EffortService.Proof ep,
				   Deadline timer,
				   Serializable cookie,
				   Exception e) {
      log.error("PollAckEffortProofCallback: bad call " + ((String) cookie) +
		  " threw " + e);
      m_state = STATE_FINALIZING;
      m_pollstate = ERR_IO; // XXX choose better
      stopPoll();
    }

    /**
     * Called to indicate verification of a proof of effort is complete.
     * @param ep the <code>EffortService.Proof</code> in question
     * @param cookie used to disambiguate callbacks
     * @param e the exception that caused the effort proof to fail
     */
    public void verificationFinished(EffortService.Proof ep,
				     Deadline timer,
				     Serializable cookie,
				     Exception e) {
      if (e != null) {
	log.debug("PollProof effort verification threw: " + e);
	m_pollstate = ERR_IO; // XXX choose better
	m_state = STATE_FINALIZING;
	stopPoll();
	return;
      }
      if (!ep.isVerified()) {
	log.debug("PollProof effort verification failed");
	m_pollstate = ERR_IO; // XXX choose better
	m_state = STATE_FINALIZING;
	stopPoll();
	return;
      }
      //  PollProof effort verified,  now generate a vote to reply with
      log.debug("PollProof effort verification succeeds with " +
		timer.getRemainingTime() + " to go");
      EffortService es = ep.getEffortService();
      EffortService.VoteCallback cb =
	new VoteGenerationCallback(m_pollmanager);
      EffortService.Vote vote = null;
      if (false) {
	// XXX
	// vote = msg.getVoteSpec();
	es = vote.getEffortService();
      } else {
	// XXX
	vote = es.makeVote();
      }
      if (es.generateVote(vote, timer, cb, cookie)) {
	// vote generation successfuly scheduled
	log.debug("Scheduled vote callback for " + ((String)cookie));
      } else {
	log.warning("could not schedule effort proof " + vote.toString() +
		    " for " + cookie);
	m_state = STATE_FINALIZING;
	m_pollstate = ERR_IO; // XXX choose better
	stopPoll();
      }
      return;
    }
  }

  class VoteGenerationCallback implements EffortService.VoteCallback {
    private PollManager pollMgr = null;
    VoteGenerationCallback(PollManager pm) {
      pollMgr = pm;
    }
    /**
     * Called to indicate generation of a vote is complete.
     * @param vote the Vote in question
     * @param cookie used to disambiguate callbacks
     * @param e the exception that caused the effort proof to fail
     */
    public void generationFinished(EffortService.Vote vote,
				   Deadline timer,
				   Serializable cookie,
				   Exception e) {
      // XXX
      if (e != null) {
	log.debug("VoteGenerationCallback: " + ((String) cookie) +
		  " threw " + e);
	m_state = STATE_FINALIZING;
	m_pollstate = ERR_IO; // XXX choose better
	stopPoll();
      } else {
	log.debug("VoteGenerationCallback: " + ((String) cookie));
	m_state = STATE_WAITING_REPAIR_REQ;
      }
    }

    /**
     * Called to indicate verification of a vote is complete.
     * @param ep the <code>EffortService.Vote</code> in question
     * @param cookie used to disambiguate callbacks
     * @param e the exception that caused the effort proof to fail
     */
    public void verificationFinished(EffortService.Vote vote,
				     Deadline timer,
				     Serializable cookie,
				     Exception e) {
      // XXX
    }
  }

  class RepairTimerCallback implements TimerQueue.Callback {
    public void timerExpired(Object cookie) {
      if (m_state == STATE_SENDING_REPAIR) {
	m_state = STATE_WAITING_REPAIR_REQ;
      } else {
	log.error("Bad state in callback " + m_state);
      }
    }
  }

  class ReceiptTimerCallback implements TimerQueue.Callback {
    public void timerExpired(Object cookie) {
      if (m_state == STATE_PROCESS_RECEIPT) {
	m_state = STATE_FINALIZING;
	if(m_pollstate != PS_COMPLETE) {
	  stopPoll();
	}
		
      } else {
	log.error("Bad state in callback " + m_state);
      }
    }
  }

  class PollTimerCallback implements TimerQueue.Callback {
    /**
     * Called when the timer expires.
     * @param cookie  data supplied by caller to schedule()
     */
    public void timerExpired(Object cookie) {
      if(m_pollstate != PS_COMPLETE) {
        stopPoll();
      }
    }
  }


}
