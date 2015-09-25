/*
 * $Id$
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

  public V1VerifyPoll(PollSpec pollspec,
		      PollManager pm,
		      PeerIdentity orig,
		      byte[] challenge,
		      long duration,
		      String hashAlg,
		      byte[] verifier) {
    super(pollspec, pm, orig, challenge, duration);
    m_replyOpcode = V1LcapMessage.VERIFY_POLL_REP;
    m_tally = new V1PollTally(this,
			      V1_VERIFY_POLL,
			      m_createTime,
			      duration,
			      1,
			      hashAlg);
    if(idMgr.isLocalIdentity(m_callerID)) {
      // if we've called the poll, we aren't going to vote
      // so we set our state to wait for a tally.
      m_pollstate = PS_WAIT_TALLY;
    }
    originalPoll = m_pollmanager.getPoll(challengeToKey(verifier));
  }


  /**
   * handle a message which may be a incoming vote
   * @param msg the Message to handle
   */
  protected void receiveMessage(LcapMessage msg) {
    log.debug("receiving verify message" + msg.toString());
    int opcode = msg.getOpcode();
    if(opcode == V1LcapMessage.VERIFY_POLL_REP) {
      startVoteCheck(msg);
    }
  }


  /**
   * schedule the hash for this poll - this is a no-op provided for completeness.
   * @param digest the MessageDigest used to hash the content
   * @param timer the Deadline by which we must complete
   * @param key the Object which will be returned from the hasher. Always the
   * message which triggered the hash
   * @param callback the hashing callback to use on return
   * @return true we never do anything here
   */
  boolean scheduleHash(MessageDigest digest, Deadline timer,
		       Object key, HashService.Callback callback) {
    return true;
  }



  /**
   * start the poll.  set a deadline in which to actually verify the message.
   */
  public void startPoll() {
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
	sendVerifyReply((V1LcapMessage)m_msg);
      }
      catch (IOException ex) {
	m_pollstate = ERR_IO;
      }
    }
    else {
      log.debug("waiting for tally - not sending reply.");
    }
  }

  private void performHash(V1LcapMessage msg) {
    PeerIdentity id = msg.getOriginatorId();
    int weight = idMgr.getReputation(id);
    byte[] challenge = msg.getChallenge();
    byte[] hashed = msg.getHashed();
    V1PollFactory pf = (V1PollFactory)m_pollmanager.getPollFactory(msg);
    MessageDigest digest = pf.getMessageDigest(msg);
    // check that vote verification hashed in the message should
    // hash to the challenge, which is the verifier of the poll
    // thats being verified

    digest.update(hashed, 0, hashed.length);
    byte[] HofHashed = digest.digest();
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

  private void sendVerifyReply(V1LcapMessage msg) throws IOException  {
    ArchivalUnit au;
    V1PollFactory pf = (V1PollFactory)m_pollmanager.getPollFactory(msg);
    String chal = String.valueOf(B64Code.encode(msg.getChallenge()));
    byte[] secret = pf.getSecret(msg.getChallenge());
    if(secret == null) {
      log.error("Verify poll reply failed.  Unable to find secret for: "
		+ chal);
      return;
    }
    byte[] verifier = pf.makeVerifier(msg.getDuration());
    V1LcapMessage repmsg =
      V1LcapMessage.makeReplyMsg(msg, secret, verifier, null,
                                 V1LcapMessage.VERIFY_POLL_REP,
                                 msg.getDuration(),
                                 idMgr.getLocalPeerIdentity(Poll.V1_PROTOCOL));

    PeerIdentity originator = msg.getOriginatorId();
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

  /**
   * Return the type of the poll, Poll.V1_VERIFY_POLL
   */
  public int getType() {
    return Poll.V1_VERIFY_POLL;
  }

  public ArchivalUnit getAu() {
    return m_tally.getArchivalUnit();
  }

  public String getStatusString() {
    return m_tally.getStatusString();
  }

  public boolean isPollActive() {
    return m_tally.stateIsActive();
  }

  public boolean isPollCompleted() {
    return m_tally.stateIsFinished();
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
	V1LcapMessage msg = (V1LcapMessage) cookie;
	performHash(msg);
	stopVoteCheck();
      }
    }
  }

}
