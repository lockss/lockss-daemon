/*
 * $Id: V3Poller.java,v 1.1.2.4 2004-10-07 02:17:05 dshr Exp $
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
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;
import org.lockss.effort.*;
import org.mortbay.util.B64Code;

/**
 * <p>This class represents the participation of this peer as the poller
 * in a poll.</p>
 * @author David Rosenthal
 * @version 1.0
 */

public class V3Poller extends V3Poll {

  public static final int STATE_INITIALIZING = 0;
  public static final int STATE_SENDING_POLL = 1;
  public static final int STATE_WAITING_POLL_ACK = 2;
  public static final int STATE_SENDING_POLL_PROOF = 3;
  public static final int STATE_WAITING_VOTE = 4;
  public static final int STATE_SENDING_REPAIR_REQ = 5;
  public static final int STATE_WAITING_REPAIR = 6;
  public static final int STATE_SENDING_RECEIPT = 7;
  public static final int STATE_FINALIZING = 8;
  private static final String[] stateName = {
    "Initializing",
    "SendingPoll",
    "WaitingPollAck",
    "SendingPollProof",
    "WaitingVote",
    "SendingRepairReq",
    "WaitingRepair",
    "SendingReceipt",
    "Finalizing",
  };

  private int m_state;
  private EffortService theEffortService = null;
  private byte[] m_challenge;

  static Logger log=Logger.getLogger("V3Poller");

  /**
   * create a new poll for a poll called by this peer
   *
   * @param pollspec the <code>PollSpec</code> on which this poll will operate
   * @param pm the <code>PollManager</code>
   * @param orig the <code>PeerIdentity</code> calling the poll - must be local
   * @param challenge a <code>byte[]</code> with the poller's nonce
   * @param duration the duration of the poll
   * @param hashAlg the hash algorithm to use
   */
  V3Poller(PollSpec pollspec, PollManager pm, PeerIdentity orig,
	   byte[] challenge, long duration, String hashAlg) {
    super(pollspec, pm, orig, challenge, duration, hashAlg);
    if (!orig.isLocalIdentity()) {
      log.error("Non-local caller for V3 poll: " + orig);
    }

    m_challenge = m_pollmanager.makeVerifier(duration);
    // XXX
    m_state = STATE_INITIALIZING;
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
    case STATE_SENDING_POLL:
    case STATE_SENDING_POLL_PROOF:
    case STATE_SENDING_REPAIR_REQ:
    case STATE_SENDING_RECEIPT:
    case STATE_FINALIZING:
      log.debug("Unexpected message " + msg.toString() + " in state " +
		stateName[m_state]);
      m_pollstate = ERR_IO; // XXX choose better
      stopPoll();
      break;
    case STATE_WAITING_POLL_ACK:
      doPollAckMessage(v3msg);
      break;
    case STATE_WAITING_VOTE:
      doVoteMessage(v3msg);
      break;
    case STATE_WAITING_REPAIR:
      doRepairMessage(v3msg);
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
    log.debug3("scheduling poll to complete by " + m_deadline);
    TimerQueue.schedule(m_deadline, new PollTimerCallback(), this);
    m_pollstate = PS_WAIT_HASH;
    //  XXX Choose the inner circle
    //  XXX while the inner circle isn't empty solicit vote from that peer
    //  XXX use callback to indicate poll is finished

    return;
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

  public int getPollState() {
    return m_state;
  }

  // End abstract methods of V3Poll

  protected void doPollAckMessage(V3LcapMessage msg) {
    if (msg.getOpcode() != V3LcapMessage.MSG_POLL_ACK) {
      log.warning("Expecting a Poll but got: " + msg.toString());
      m_pollstate = ERR_IO; // XXX choose better
      m_state = STATE_FINALIZING;
      stopPoll();
      return;
    }
    //  Verify the effort in the PollAck message
    EffortService.ProofCallback cb = new PollProofEffortCallback(m_pollmanager);
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
      m_state = STATE_SENDING_POLL_PROOF;
    } else {
      log.warning("could not schedule effort verification " + ep.toString() +
		  " for " + msg.toString());
      m_state = STATE_FINALIZING;
    }
    // XXX
  }

  protected void doVoteMessage(V3LcapMessage msg) {
    // XXX
  }

  protected void doRepairMessage(V3LcapMessage msg) {
    // XXX
  }

  //  XXX - stuff for initial testing
  private PeerIdentity voter = null;
  public void solicitVoteFrom(PeerIdentity candidate) {
      // XXX this is mock stuff for initial testing
    voter = candidate;
    EffortService.ProofCallback cb = new PollEffortCallback(m_pollmanager);
    EffortService es = theEffortService;
    EffortService.Proof pollProof = es.makeProof();
    long duration = 1000000;
    Deadline timer = Deadline.in(duration);
    Serializable cookie = challengeToKey(m_challenge);
    if (es.proveEffort(pollProof, timer, cb, cookie)) {
      log.debug("Scheduled generation callback in " +
		timer.getRemainingTime() + " for " + ((String)cookie));
      m_state = STATE_SENDING_POLL;
    } else {
      log.warning("could not schedule effort generation " +
		  pollProof.toString() + " for " + cookie);
      m_pollstate = ERR_IO; // XXX choose better
      m_state = STATE_FINALIZING;
      stopPoll();
    }
  }

  /**
   * create a human readable string representation of this poll
   * @return a String
   */
  public String toString() {
    // XXX should report state of poll here
    String pollType = "V3";
    StringBuffer sb = new StringBuffer("[Poller: ");
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
    if (m_state >= 0 && m_state < stateName.length) {
      sb.append(" state: ");
      sb.append(stateName[m_state]);
    } else {
      sb.append(" bad state " + m_state);
    }
    sb.append("]");
    return sb.toString();
  }

  class PollEffortCallback implements EffortService.ProofCallback {
    private PollManager pollMgr = null;
    PollEffortCallback(PollManager pm) {
      pollMgr = pm;
    }
    /**
     * Called to indicate generation of a proof of effort for
     * the Poll message is complete.
     * @param ep the EffortProof in question
     * @param cookie used to disambiguate callbacks
     * @param e the exception that caused the effort proof to fail
     */
    public void generationFinished(EffortService.Proof ep,
				   Deadline timer,
				   Serializable cookie,
				   Exception e) {
      if (e != null) {
	log.debug("PollEffortProofCallback: " + ((String) cookie) +
		  " threw " + e);
	m_state = STATE_FINALIZING;
	m_pollstate = ERR_IO; // XXX choose better
	stopPoll();
      } else {
	log.debug("PollEffortProofCallback: " + ((String) cookie));
	m_state = STATE_WAITING_POLL_ACK;
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
      log.debug("Poll effort verification should not happen");
      m_pollstate = ERR_IO; // XXX choose better
      m_state = STATE_FINALIZING;
      stopPoll();
      return;
    }
  }

  class PollProofEffortCallback implements EffortService.ProofCallback {
    private PollManager pollMgr = null;
    PollProofEffortCallback(PollManager pm) {
      pollMgr = pm;
    }
    /**
     * Called to indicate generation of a proof of effort for
     * the PollProof message is complete.
     * @param ep the EffortProof in question
     * @param cookie used to disambiguate callbacks
     * @param e the exception that caused the effort proof to fail
     */
    public void generationFinished(EffortService.Proof ep,
				   Deadline timer,
				   Serializable cookie,
				   Exception e) {
      if (e != null) {
	log.debug("PollProofEffortProofCallback: " + ((String) cookie) +
		  " threw " + e);
	m_state = STATE_FINALIZING;
	m_pollstate = ERR_IO; // XXX choose better
	stopPoll();
      } else {
	log.debug("PollProofEffortProofCallback: " + ((String) cookie));
	m_state = STATE_WAITING_VOTE;
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
	log.debug("PollAck effort verification threw: " + e);
	m_pollstate = ERR_IO; // XXX choose better
	m_state = STATE_FINALIZING;
	stopPoll();
	return;
      }
      if (!ep.isVerified()) {
	log.debug("PollAck effort verification failed");
	m_pollstate = ERR_IO; // XXX choose better
	m_state = STATE_FINALIZING;
	stopPoll();
	return;
      }
      //  Poll effort verified,  now generate effort for reply
      log.debug("PollProof effort verification succeeds with " +
		timer.getRemainingTime() + " to go");
      EffortService es = ep.getEffortService();
      //  XXX should get spec for proof from message
      EffortService.Proof pollProof = es.makeProof();
      if (es.proveEffort(pollProof, timer, this, cookie)) {
	log.debug("Scheduled generation callback in " +
		  timer.getRemainingTime() + " for " + ((String)cookie));
      } else {
	log.warning("could not schedule effort generation " +
		    pollProof.toString() + " for " + cookie);
	m_pollstate = ERR_IO; // XXX choose better
	m_state = STATE_FINALIZING;
	stopPoll();
      }
      return;
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
