/*
 * $Id: V3Poller.java,v 1.1.2.13 2004-10-29 19:43:06 dshr Exp $
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
import java.util.*;

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
  //  The poll uses these states to collect votes
  public static final int STATE_CHOOSING_NEXT_VOTER = 1;
  public static final int STATE_PROVING_INTRO_EFFORT = 2;
  public static final int STATE_SENDING_POLL = 3;
  public static final int STATE_WAITING_POLL_ACK = 4;
  public static final int STATE_VERIFYING_POLL_ACK_EFFORT = 5;
  public static final int STATE_PROVING_REMAINING_EFFORT = 6;
  public static final int STATE_SENDING_POLL_PROOF = 7;
  public static final int STATE_WAITING_VOTE = 8;
  //  When they are all collected it uses these states to
  //  verify the votes and obtain repairs
  public static final int STATE_CHOOSING_NEXT_VOTE = 9;
  public static final int STATE_VERIFYING_VOTE_EFFORT = 10;
  public static final int STATE_PROVING_REPAIR_EFFORT = 11;
  public static final int STATE_VERIFYING_VOTE = 12;
  public static final int STATE_SENDING_REPAIR_REQ = 13;
  public static final int STATE_WAITING_REPAIR = 14;
  //  Then it sends receipts and cleans up
  public static final int STATE_TALLYING = 15;
  public static final int STATE_SENDING_RECEIPT = 16;
  public static final int STATE_FINALIZING = 17;
  private static final String[] stateName = {
    "Initializing",
    "ChoosingNextvoter",
    "ProvingIntroEffort",
    "SendingPoll",
    "WaitingPollAck",
    "VerifyPollAckEffort",
    "ProvingRemainingEffort",
    "SendingPollProof",
    "WaitingVote",
    "ChoosingNextVote",
    "VerifyingVoteEffort",
    "GeneratingRepairEffort",
    "VerifyVote",
    "SendingRepairReq",
    "WaitingRepair",
    "Tallying",
    "SendingReceipt",
    "Finalizing",
  };

  private int m_state;
  private EffortService theEffortService = null;
  private byte[] m_challenge;
  private EffortService.Proof m_repairEffort = null;

  static Logger log=Logger.getLogger("V3Poller");

  private List ballotBox;  // The collected but unverified votes
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

    // m_challenge = m_pollmanager.makeVerifier(duration);
    m_challenge = challenge;
    // XXX
    m_state = STATE_INITIALIZING;
    theEffortService = pm.getEffortService(pollspec);
    ballotBox = new ArrayList();
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
      log.debug("Unexpected message " + msg.toString() + " in state " +
		getPollStateName(m_state));
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
      m_pollmanager.closeThePoll(m_key);
      log.debug3("closed the poll:" + m_key);
    }
    else {
      m_pollstate = BasePoll.PS_COMPLETE;
      verifyNextVote(m_key);
    }
    // Start verifying votes
  }

  public int getPollState() {
    return m_state;
  }

  // End abstract methods of V3Poll

  private void changePollState(int state) {
    log.debug("Change state from " + stateName[m_state] +
	      " to " + stateName[state]);
    m_state = state;
  }

  public String getPollStateName(int state) {
    if (state < 0 || state >= stateName.length) {
      return "bad state";
    } else {
      return stateName[state];
    }
  }

  protected void doPollAckMessage(V3LcapMessage msg) {
    Serializable cookie = msg.getKey();
    if (msg.getOpcode() != V3LcapMessage.MSG_POLL_ACK) {
      log.warning("Expecting a Poll but got: " + msg.toString());
      nextVoter(cookie, false);
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
    if (es.verifyProof(ep, timer, cb, cookie)) {
      // effort verification for Poll successfuly scheduled
      log.debug("Scheduled verification callback before " +
		timer.getRemainingTime() + " for " + ((String)cookie));
      changePollState(STATE_VERIFYING_POLL_ACK_EFFORT);
    } else {
      log.warning("could not schedule effort verification " + ep.toString() +
		  " for " + msg.toString());
      nextVoter(cookie, true);
    }
    // XXX
  }

  protected void doVoteMessage(V3LcapMessage msg) {
    Serializable cookie = msg.getKey();
    if (msg.getOpcode() != V3LcapMessage.MSG_VOTE) {
      log.debug("Expect " + V3LcapMessage.MSG_VOTE + " got " + msg.getOpcode());
      log.warning("Expecting a Vote but got: " + msg.toString());
      nextVoter(cookie, false);
      return;
    }
    ballotBox.add(msg);
    nextVoter(cookie, false);
  }

  protected void doRepairMessage(V3LcapMessage msg) {
    // XXX
  }

  protected void verifyNextVote(Serializable cookie) {
    if (ballotBox.isEmpty()) {
      //  No more votes to verify - finish the poll
      changePollState(STATE_FINALIZING);
      m_pollmanager.closeThePoll(m_key);
      log.debug3("closed the poll:" + m_key);
      return;
    }
    V3LcapMessage msg = (V3LcapMessage)ballotBox.get(0);
    //  Verify the effort in this Vote message
    EffortService.ProofCallback cb = new VoteEffortCallback(m_pollmanager);
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
    if (es.verifyProof(ep, timer, cb, cookie)) {
      // effort verification for Vote successfuly scheduled
      log.debug("Scheduled verification callback before " +
		timer.getRemainingTime() + " for " + ((String)cookie));
      changePollState(STATE_VERIFYING_VOTE_EFFORT);
    } else {
      log.warning("could not schedule effort verification " + ep.toString() +
		  " for " + msg.toString());
      pauseBeforeVerifyNextVote(cookie);
    }
    // XXX
  }

  protected void pauseBeforeVerifyNextVote(Serializable cookie) {
    //  XXX move vote at head of list to tail, sleep
    V3LcapMessage msg = (V3LcapMessage)ballotBox.remove(0);
    ballotBox.add(msg);
    Deadline pauseDeadline = Deadline.in(10000);
    TimerQueue.schedule(pauseDeadline, new PauseTimerCallback(), cookie);
  }

  //  XXX - stuff for initial testing
  private List m_voterRoll = null;
  private PeerIdentity m_currentVoter = null;

  protected void solicitVotesFrom(List peers) {
    if (peers.size() == 0) {
      log.error("No voters to solicit from");
      // XXX throw?
    }
    m_voterRoll = peers;
    Object[] voters = peers.toArray();
    if (!(voters[0] instanceof PeerIdentity)) {
      log.error("Voter is not a PeerIdentity");
      // XXX throw?
    }
    m_currentVoter = (PeerIdentity) voters[0];
    // XXX this is mock stuff for initial testing
    EffortService.ProofCallback cb = new PollEffortCallback(m_pollmanager);
    EffortService es = theEffortService;
    EffortService.Proof pollProof = es.makeProof();
    long duration = 1000000;
    Deadline timer = Deadline.in(duration);
    Serializable cookie = challengeToKey(m_challenge);
    if (es.proveEffort(pollProof, timer, cb, cookie)) {
      log.debug("Scheduled generation callback before " +
		timer.getRemainingTime() + " for " + ((String)cookie));
      changePollState(STATE_PROVING_INTRO_EFFORT);
    } else {
      log.warning("could not schedule effort generation " +
		  pollProof.toString() + " for " + cookie);
      nextVoter(cookie, true);
    }
  }

  protected void nextVoter(Object cookie, boolean addBack) {
    changePollState(STATE_CHOOSING_NEXT_VOTER);
    log.debug("nextVoter(" + cookie + ")");
    if (m_voterRoll.isEmpty()) {
      log.error("Voter roll empty after receipt");
      changePollState(STATE_FINALIZING);
      if(m_pollstate != PS_COMPLETE) {
	stopPoll();
      }
    } else {
      Object obj = m_voterRoll.remove(0);
      if (addBack) {
	m_voterRoll.add(obj);
      }
      log.debug("voter roll returns a " + obj.getClass().getName());
      if ((PeerIdentity)obj == null) {
	log.error("Voter roll doesn't deliver peer id after receipt");
	changePollState(STATE_FINALIZING);
	if(m_pollstate != PS_COMPLETE) {
	  stopPoll();
	}
      } else if (m_voterRoll.isEmpty()) {
	changePollState(STATE_CHOOSING_NEXT_VOTE);
	if(m_pollstate != PS_COMPLETE) {
	  stopPoll();
	}
      } else {
	solicitVotesFrom(m_voterRoll);
      }
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
	nextVoter(cookie, true);
      } else {
	log.debug("PollEffortProofCallback: " + ((String) cookie));
	changePollState(STATE_SENDING_POLL);
	Properties props = null;
	//  XXX put effort etc into Properties for message
	if (false) try {
	  PollSpec pollspec = null; // XXX
	  LcapMessage msg =
	    V3LcapMessage.makeRequestMsg(pollspec, props, m_challenge,
					 V3LcapMessage.MSG_POLL,
					 m_deadline.getRemainingTime(),
					 m_callerID);
	  pollMgr.sendMessageTo(msg, m_cus.getArchivalUnit(), m_currentVoter);
	  changePollState(STATE_WAITING_POLL_ACK);
	} catch (IOException ex) {
	  log.error("sending message to " + m_currentVoter.toString() +
		    " threw " + ex.toString());
	  nextVoter(cookie, false);
	} else {
	  changePollState(STATE_WAITING_POLL_ACK);
	}
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
      changePollState(STATE_FINALIZING);
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
	nextVoter(cookie, true);
      } else {
	log.debug("PollProofEffortProofCallback: " + ((String) cookie));
	changePollState(STATE_SENDING_POLL_PROOF);
	// XXX send the proof and the message
	changePollState(STATE_WAITING_VOTE);
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
	nextVoter(cookie, true);
	return;
      }
      if (!ep.isVerified()) {
	log.debug("PollAck effort verification failed");
	nextVoter(cookie, false);
	return;
      }
      //  Poll effort verified,  now generate effort for reply
      log.debug("PollProof effort verification succeeds with " +
		timer.getRemainingTime() + " to go");
      EffortService es = ep.getEffortService();
      //  XXX should get spec for proof from message
      EffortService.Proof pollProof = es.makeProof();
      if (es.proveEffort(pollProof, timer, this, cookie)) {
	log.debug("Scheduled generation callback before " +
		  timer.getRemainingTime() + " for " + ((String)cookie));
	changePollState(STATE_PROVING_REMAINING_EFFORT);
      } else {
	log.warning("could not schedule effort generation " +
		    pollProof.toString() + " for " + cookie);
	nextVoter(cookie, true);
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
     * the Repair message is complete.
     * @param ep the EffortProof in question
     * @param cookie used to disambiguate callbacks
     * @param e the exception that caused the effort proof to fail
     */
    public void generationFinished(EffortService.Proof ep,
				   Deadline timer,
				   Serializable cookie,
				   Exception e) {
      if (e != null) {
	log.debug("VoteEffortProofCallback: " + ((String) cookie) +
		  " threw " + e);
	// We assume there's something bad about the vote we just
	// failed to verify
	ballotBox.remove(0);
	verifyNextVote(cookie);
      } else {
	log.debug("VoteEffortProofCallback: " + ((String) cookie));
	m_repairEffort = ep;
	// Now verify the vote
	EffortService es = ep.getEffortService();
	EffortService.Vote vote = null;
	if (false) {
	  //  XXX vote = msg.getVote();
	} else {
	  vote = es.makeVote();
	}
	EffortService.VoteCallback cb =
	  new VoteVerificationCallback(m_pollmanager);
	if (es.verifyVote(vote, timer, cb, cookie)) {
	  log.debug("Scheduled vote verification callback before " +
		    timer.getRemainingTime() + " for " + ((String)cookie));
	  changePollState(STATE_VERIFYING_VOTE);
	} else {
	  log.warning("could not schedule vote verification generation " +
		      vote.toString() + " for " + cookie);
	  pauseBeforeVerifyNextVote(cookie);
	}
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
	log.debug("Vote effort verification threw: " + e);
	ballotBox.remove(0);
	verifyNextVote(cookie);
	return;
      }
      if (!ep.isVerified()) {
	log.debug("Vote effort verification failed");
	ballotBox.remove(0);
	verifyNextVote(cookie);
	return;
      }
      //  Vote effort verified,  now generate effort for repair request
      log.debug("Vote effort verification succeeds with " +
		timer.getRemainingTime() + " to go");
      EffortService es = ep.getEffortService();
      //  XXX should get spec for proof from message
      EffortService.Proof pollProof = es.makeProof();
      if (es.proveEffort(pollProof, timer, this, cookie)) {
	log.debug("Scheduled generation callback before " +
		  timer.getRemainingTime() + " for " + ((String)cookie));
	changePollState(STATE_PROVING_REPAIR_EFFORT);
      } else {
	log.warning("could not schedule effort generation " +
		    pollProof.toString() + " for " + cookie);
	pauseBeforeVerifyNextVote(cookie);
      }
      return;
    }
  }

  class VoteVerificationCallback implements EffortService.VoteCallback {
    private PollManager pollMgr = null;
    VoteVerificationCallback(PollManager pm) {
      pollMgr = pm;
    }
    /**
     * Called to indicate generation of a Vote for
     * the Vote message is complete.
     * @param ep the Vote in question
     * @param cookie used to disambiguate callbacks
     * @param e the exception that caused the effort proof to fail
     */
    public void generationFinished(EffortService.Vote vote,
				   Deadline timer,
				   Serializable cookie,
				   Exception e) {
      log.debug("VoteVerificationCallback: " + ((String) cookie) +
		" threw " + e + " should not happen");
      changePollState(STATE_FINALIZING);
      m_pollstate = ERR_IO; // XXX choose better
      stopPoll();
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
      ballotBox.remove(0);
      if (e != null) {
	log.debug("Vote effort verification threw: " + e);
	verifyNextVote(cookie);
	return;
      }
      if (!vote.isValid()) {
	log.debug("Vote deemed invalid");
	verifyNextVote(cookie);
	return;
      }
      log.debug("Vote: " + (String) cookie +
		(vote.isAgreement() ? " " : " dis") + "agree");
      changePollState(STATE_TALLYING);
      if (m_tally != null) {
	if (vote.isAgreement()) {
	  m_tally.agree();
	} else {
	  m_tally.disagree();
	}
      } else {
	log.error("Can't get poll tally for " + (String)cookie);
      }
      changePollState(STATE_SENDING_RECEIPT);
      Deadline receiptDeadline = Deadline.in(400);
      TimerQueue.schedule(receiptDeadline, new ReceiptTimerCallback(), cookie);
      return;
    }
  }

  //  XXX temporary
  class ReceiptTimerCallback implements TimerQueue.Callback {
    /**
     * Called when the timer expires.
     * @param cookie  data supplied by caller to schedule()
     */
    public void timerExpired(Object cookie) {
      log.debug("ReceiptTimerCallback for " + cookie);
      verifyNextVote((Serializable)cookie);
    }
  }

  //  Used to implement a pause when effort or vote verification
  //  can't be scheduled
  class PauseTimerCallback implements TimerQueue.Callback {
    /**
     * Called when the timer expires.
     * @param cookie  data supplied by caller to schedule()
     */
    public void timerExpired(Object cookie) {
      log.debug("PauseTimerCallback for " + cookie);
      verifyNextVote((Serializable)cookie);
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
