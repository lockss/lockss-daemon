/*
* $Id: V3Poller.java,v 1.1.2.1 2004-09-30 01:06:16 dshr Exp $
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

import org.mortbay.util.*;  // For B64 encoding stuff?

/**
 * <p>This class represents the participation of this peer as the poller
 * in a poll.</p>
 * @author David Rosenthal
 * @version 1.0
 */

public class V3Poller extends V3Poll {

  private static final int STATE_INITIALIZING = 0;
  private static final int STATE_SENDING_POLL = 1;
  private static final int STATE_WAITING_POLL_ACK = 2;
  private static final int STATE_SENDING_POLL_PROOF = 3;
  private static final int STATE_WAITING_VOTE = 4;
  private static final int STATE_SENDING_REPAIR_REQ = 5;
  private static final int STATE_WAITING_REPAIR = 6;
  private static final int STATE_SENDING_RECEIPT = 7;
  private static final int STATE_FINALIZING = 8;
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

    // XXX
  }

  // Implementations of abstract methods from V3Poll

  /**
   * receive a message that is part of this poll
   */
  void receiveMessage(LcapMessage msg) {
    // XXX
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
    sb.append(" ");
    sb.append(m_msg.getOpcodeString());
    sb.append(" key:");
    sb.append(m_key);
    sb.append("]");
    return sb.toString();
  }

  class PollHashCallback implements HashService.Callback {

    /**
     * Called to indicate that hashing the content or names of a
     * <code>CachedUrlSet</code> object has succeeded, if <code>e</code>
     * is null,  or has failed otherwise.
     * @param urlset  the <code>CachedUrlSet</code> being hashed.
     * @param cookie  used to disambiguate callbacks.
     * @param hasher  the <code>MessageDigest</code> object that
     *                contains the hash.
     * @param e       the exception that caused the hash to fail.
     */
    public void hashingFinished(CachedUrlSet urlset,
                                Object cookie,
                                MessageDigest hasher,
                                Exception e) {
      if(m_pollstate != PS_WAIT_HASH) {
        log.debug("hasher returned with pollstate: " + m_pollstate);
        return;
      }
      boolean hash_completed = e == null ? true : false;

      if(hash_completed)  {
        byte[] m_hash  = hasher.digest();  // XXX
        log.debug2("Hash on " + urlset + " complete: "+
                  String.valueOf(B64Code.encode(m_hash)));
        m_pollstate = PS_WAIT_VOTE;
        // XXX
      }
      else {
        log.debug("Poll hash failed : " + e.getMessage());
        m_pollstate = ERR_HASHING;
      }
    }
  }


  class VoteHashCallback implements HashService.Callback {

    /**
     * Called to indicate that hashing the content or names of a
     * <code>CachedUrlSet</code> object has succeeded, if <code>e</code>
     * is null,  or has failed otherwise.
     * @param urlset  the <code>CachedUrlSet</code> being hashed.
     * @param cookie  used to disambiguate callbacks.
     * @param hasher  the <code>MessageDigest</code> object that
     *                contains the hash.
     * @param e       the exception that caused the hash to fail.
     */
    public void hashingFinished(CachedUrlSet urlset,
                                Object cookie,
                                MessageDigest hasher,
                                Exception e) {
      boolean hash_completed = e == null ? true : false;

      if(hash_completed)  {
        byte[] out_hash = hasher.digest();
        Vote vote = (Vote)cookie;
        // checkVote(out_hash, vote);
        // stopVoteCheck();
      }
      else {
        log.info("vote hash failed with exception:" + e.getMessage());
      }
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
