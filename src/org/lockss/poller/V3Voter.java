/*
* $Id: V3Voter.java,v 1.1.2.1 2004-09-30 01:06:16 dshr Exp $
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

import org.mortbay.util.*;  // For B64 encoding stuff?

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


  private static final int STATE_INITIALIZING = 0;
  private static final int STATE_SENDING_POLL_ACK = 1;
  private static final int STATE_WAITING_POLL_PROOF = 2;
  private static final int STATE_SENDING_VOTE = 3;
  private static final int STATE_WAITING_REPAIR_REQ = 4;
  private static final int STATE_SENDING_REPAIR = 5;
  private static final int STATE_WAITING_RECEIPT = 6;
  private static final int STATE_PROCESS_RECEIPT = 7;
  private static final int STATE_FINALIZING = 8;
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
    if (m_pollstate != PS_INITING)
      return;
    // XXX
    if (true) {
      m_pollstate = ERR_SCHEDULE_HASH;
      log.debug("couldn't schedule our hash:" + m_deadline + ", stopping poll.");
      stopPoll();
      return;
    }
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
      //  XXX should abort poll?
      return;
    }
    EffortService.Callback cb = new PollAckEffortCallback();
    EffortService.Proof ep = msg.getEffortProof();
    EffortService es = ep.getEffortService();
    Deadline timer = msg.getDeadline();
    Serializable cookie = null; // XXX
    if (es.proveEffort(ep, timer, cb, cookie)) {
      // effort proof for Poll Ack successfuly scheduled
      m_state = STATE_SENDING_POLL_ACK;
    } else {
      log.warning("could not schedule effort proof " + ep.toString() +
		  " for " + msg.toString());
      m_state = STATE_FINALIZING;
    }
  }

  private void doPollProofMessage(V3LcapMessage msg) {
    if (msg.getOpcode() != V3LcapMessage.MSG_POLL_PROOF) {
      log.warning("Expecting a PollProof but got: " + msg.toString());
      //  XXX should abort poll?
      return;
    }
    // XXX do a PollProof?
    m_state = STATE_SENDING_VOTE;
  }

  private void doRepairReqMessage(V3LcapMessage msg) {
    switch (msg.getOpcode()) {
    default:
      log.warning("Expecting a Poll but got: " + msg.toString());
      //  XXX should abort poll?
      return;
    case V3LcapMessage.MSG_EVALUATION_RECEIPT:
      doReceiptMessage(msg);
      return;
    case V3LcapMessage.MSG_REPAIR_REQ:
      break;
    }
    // XXX actually supply repair
    m_state = STATE_SENDING_REPAIR;
  }

  private void doReceiptMessage(V3LcapMessage msg) {
    if (msg.getOpcode() != V3LcapMessage.MSG_EVALUATION_RECEIPT) {
      log.warning("Expecting an EvaluationReceipt but got: " + msg.toString());
      //  XXX should abort poll?
      return;
    }
    // XXX do an EvaluationReceipt?
    m_state = STATE_PROCESS_RECEIPT;
  }

  /**
   * create a human readable string representation of this poll
   * @return a String
   */
  public String toString() {
    // XXX should report state of poll here
    String pollType = "V3";
    StringBuffer sb = new StringBuffer("[Voter: ");
    sb.append(pollType);
    sb.append(" url set:");
    sb.append(" ");
    sb.append(m_cus.toString());
    sb.append(" ");
    sb.append(m_msg.getOpcodeString());
    sb.append(" key:");
    sb.append(m_key);
    sb.append("]");
    return sb.toString();
  }

  class PollAckEffortCallback implements EffortService.Callback {
    /**
     * Called to indicate generation of a proof of effort for
     * the PollAck message is complete.
     * @param ep the EffortProof in question
     * @param cookie used to disambiguate callbacks
     * @param e the exception that caused the effort proof to fail
     */
    public void generationFinished(EffortService.Proof ep,
				   Serializable cookie,
				   Exception e) {
      // XXX
    }

  }

  class VoteTimerCallback implements TimerQueue.Callback {
    /**
     * Called when the timer expires.
     * @param cookie  data supplied by caller to schedule()
     */
    public void timerExpired(Object cookie) {
      log.debug3("VoteTimerCallback called, checking if I should vote");
      if(m_pollstate == PS_WAIT_VOTE) {
        log.debug3("I should vote");
        // voteInPoll();
        log.debug("I just voted");
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
