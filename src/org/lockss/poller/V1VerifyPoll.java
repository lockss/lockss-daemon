/*
* $Id: V1VerifyPoll.java,v 1.7 2004-09-20 14:20:37 dshr Exp $
 */

/*
Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
import org.mortbay.util.B64Code;


/**
 * @author Claire Griffin
 * @version 1.0
 */
class V1VerifyPoll extends V1Poll {

  BasePoll originalPoll;

  public V1VerifyPoll(LcapMessage msg,
			      PollSpec pollspec, PollManager pm) {
    super(msg, pollspec, pm);
    m_replyOpcode = LcapMessage.VERIFY_POLL_REP;
    m_tally = new V1PollTally(this,
                              VERIFY_POLL,
                              m_createTime,
                              msg.getDuration(),
                              1,
                              msg.getHashAlgorithm());
    if(idMgr.isLocalIdentity(m_callerID)) {
       // if we've called the poll, we aren't going to vote
       // so we set our state to wait for a tally.
       m_pollstate = PS_WAIT_TALLY;
    }
    String key = String.valueOf(B64Code.encode(msg.getVerifier()));
    originalPoll = m_pollmanager.getPoll(key);
  }


  /**
   * handle a message which may be a incoming vote
   * @param msg the Message to handle
   */
  void receiveMessage(LcapMessage msg) {
    log.debug("receiving verify message" + msg.toString());
    int opcode = msg.getOpcode();
    if(opcode == LcapMessage.VERIFY_POLL_REP) {
      startVoteCheck(msg);
    }
  }


  /**
   * schedule the hash for this poll - this is a no-op provided for completeness.
   * @param hasher the MessageDigest used to hash the content
   * @param timer the Deadline by which we must complete
   * @param key the Object which will be returned from the hasher. Always the
   * message which triggered the hash
   * @param callback the hashing callback to use on return
   * @return true we never do anything here
   */
  boolean scheduleHash(MessageDigest hasher, Deadline timer,
                       Serializable key, HashService.Callback callback) {
    return true;
  }



  /**
   * start the poll.  set a deadline in which to actually verify the message.
   */
  void startPoll() {
    log.debug("Starting new verify poll:" + m_key);
    if(!idMgr.isLocalIdentity(m_callerID)) {
      long now = TimeBase.nowMs();
      long remainingTime = m_deadline.getRemainingTime();
      long minTime = now + (remainingTime / 2) - (remainingTime / 4);
      long maxTime = now + (remainingTime / 2) + (remainingTime / 4);
      m_voteTime = Deadline.atRandomRange(minTime, maxTime);
      log.debug("scheduling verify vote for " + m_voteTime);
      m_pollstate = PS_WAIT_VOTE;
      scheduleVote();

    }
    TimerQueue.schedule(m_deadline, new PollTimerCallback(), this);
 }


 /**
  * cast our vote in this poll
  */
 void voteInPoll() {
   if(m_pollstate != PS_WAIT_TALLY) {
     try {
       log.debug("sending our verify reply now.");
       // send our reply message
       sendVerifyReply(m_msg);
     }
     catch (IOException ex) {
       m_pollstate = ERR_IO;
     }
   }
   else {
     log.debug("waiting for tally - not sending reply.");
   }
 }

  private void performHash(LcapMessage msg) {
    PeerIdentity id = msg.getOriginatorID();
    int weight = idMgr.getReputation(id);
    byte[] challenge = msg.getChallenge();
    byte[] hashed = msg.getHashed();
    MessageDigest hasher = m_pollmanager.getHasher(msg);
    // check that vote verification hashed in the message should
    // hash to the challenge, which is the verifier of the poll
    // thats being verified

    hasher.update(hashed, 0, hashed.length);
    byte[] HofHashed = hasher.digest();
    boolean agree = Arrays.equals(challenge, HofHashed);
    if(isMyPoll())
      updateReputation(agree);
    m_tally.addVote(new Vote(msg, agree),
                    id, idMgr.isLocalIdentity(id));
  }

  /**
   * tally the poll results
   */
  private void updateReputation(boolean voteAgreed)  {
    log.info(m_msg.toString() + " tally " + toString());
    int oldRep = idMgr.getReputation(m_callerID);
    int agree = m_tally.numAgree;
    int disagree = m_tally.numDisagree;
    if(voteAgreed) {
      agree += 1;
    }
    else {
      disagree += 1;
    }
    if ((agree + disagree) < 1) {
      log.debug("vote failed to verify");
      idMgr.changeReputation(m_callerID, IdentityManager.VOTE_NOTVERIFIED);
    } else if (agree > 0 && disagree == 0) {
      log.debug("vote successfully verified");
      idMgr.changeReputation(m_callerID, IdentityManager.VOTE_VERIFIED);
    } else {
      log.debug("vote disowned.");
      idMgr.changeReputation(m_callerID, IdentityManager.VOTE_DISOWNED);
    }
    int newRep = idMgr.getReputation(m_callerID);
    if(originalPoll != null) {
      originalPoll.getVoteTally().adjustReputation(m_callerID, newRep -oldRep);
      log.debug("adjusted voter reputation in poll: " + originalPoll.getKey());
    }
  }

  private void sendVerifyReply(LcapMessage msg) throws IOException  {
    String url = new String(msg.getTargetUrl());
    ArchivalUnit au;
    String chal = String.valueOf(B64Code.encode(msg.getChallenge()));
    byte[] secret = m_pollmanager.getSecret(msg.getChallenge());
    if(secret == null) {
      log.error("Verify poll reply failed.  Unable to find secret for: "
                + chal);
      return;
    }
    byte[] verifier = m_pollmanager.makeVerifier(msg.getDuration());
    LcapMessage repmsg = LcapMessage.makeReplyMsg(msg,
        secret,
        verifier,
        null,
        LcapMessage.VERIFY_POLL_REP,
        msg.getDuration(),
        idMgr.getLocalPeerIdentity());

    PeerIdentity originator = msg.getOriginatorID();
    log.debug("sending our verification reply to " + originator.toString());
    PollSpec spec = new PollSpec(repmsg);
    au = spec.getCachedUrlSet().getArchivalUnit();
    m_pollmanager.sendMessageTo(repmsg, au, originator);
    log.debug("adding our check to poll");
    performHash(repmsg);
  }

  private void startVoteCheck(LcapMessage msg) {
    log.debug("Starting new verify vote:" + m_key);
    super.startVoteCheck();
    // schedule a hash/vote
    Deadline deadline = Deadline.atRandomBefore(m_deadline);
    log.debug("Waiting until at most " + deadline + " to verify");
    TimerQueue.schedule(deadline, new VerifyTimerCallback(), msg);
    m_pollstate = PS_WAIT_HASH;
  }

  class VerifyTimerCallback implements TimerQueue.Callback {
    /**
     * Called when the timer expires.
     * @param cookie  data supplied by caller to schedule()
     */
    public void timerExpired(Object cookie) {
      log.debug("VerifyTimerCallback called, checking if I should verify");
      if(m_pollstate == PS_WAIT_HASH) {
        log.debug("I should verify ");
        LcapMessage msg = (LcapMessage) cookie;
        performHash(msg);
        stopVoteCheck();
      }
    }
  }

}

