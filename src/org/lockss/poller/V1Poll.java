/*
* $Id: V1Poll.java,v 1.21 2004-12-07 05:17:52 tlipkis Exp $
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

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;

import org.mortbay.util.B64Code;


/**
 * <p>Abstract base class for all version one poll objects.</p>
 * @author Claire Griffin
 * @version 1.0
 */

public abstract class V1Poll extends BasePoll {
  static final String PARAM_AGREE_VERIFY = Configuration.PREFIX +
      "poll.agreeVerify";
  static final String PARAM_DISAGREE_VERIFY = Configuration.PREFIX +
      "poll.disagreeVerify";
  static final String PARAM_VOTE_MARGIN = Configuration.PREFIX +
      "poll.voteMargin";
  static final String PARAM_TRUSTED_WEIGHT = Configuration.PREFIX +
      "poll.trustedWeight";

  static final int DEFAULT_VOTE_MARGIN = 75;
  static final int DEFAULT_TRUSTED_WEIGHT = 350;
  static final int DEFAULT_AGREE_VERIFY = 10;
  static final int DEFAULT_DISAGREE_VERIFY = 80;
  double m_agreeVer = 0;     // the max percentage of time we will verify
  double m_disagreeVer = 0;  // the max percentage of time we will verify
  byte[] m_challenge;     // The caller's challenge string
  byte[] m_verifier;      // Our verifier string - hash of secret
  byte[] m_hash;          // Our hash of challenge, verifier and content(S)

  Deadline m_voteTime;    // when to vote
  Deadline m_hashDeadline; // when our hashes must finish by

  int m_replyOpcode = -1;  // opcode used to reply to poll
  long m_hashTime;         // an estimate of the time it will take to hash
  int m_pendingVotes = 0;  // the number of votes waiting to be tallied
  V1PollTally m_tally;

  /**
   * create a new poll
   *
   * @param pollspec the PollSpec on which this poll will operate
   * needed to create this poll.
   * @param pm the pollmanager
   * @param orig the identity of the originator of poll
   * @param challenge the poll challenge
   * @param duration the poll duration
   */
  V1Poll(PollSpec pollspec,
	 PollManager pm,
	 PeerIdentity orig,
	 byte[] challenge,
	 long duration) {
    super(pollspec, pm, orig, challengeToKey(challenge), duration);

    // now copy the msg elements we need
    m_hashTime = m_cus.estimatedHashDuration();
    if(pollspec.getPollType() != Poll.VERIFY_POLL) {
      m_hashDeadline =
          Deadline.at(m_deadline.getExpirationTime() - Constants.MINUTE);
    }

    m_challenge = challenge;
    m_verifier = pm.makeVerifier(duration);

    log.debug("I think poll "+ challengeToKey(m_challenge)
	      +" will take me this long to hash "+
	      StringUtil.timeIntervalToString(m_hashTime));
    m_createTime = TimeBase.nowMs();
    getConfigValues();
  }

  public static String challengeToKey(byte[] challenge) {
    return String.valueOf(B64Code.encode(challenge));
  }

  /**
   * get the VoteTally for this Poll
   * @return VoteTally for this poll
   */
  public PollTally getVoteTally() {
    return m_tally;
  }

  /**
   * create a human readable string representation of this poll
   * @return a String
   */
  public String toString() {
    String pollType = "Unk";
    if (this instanceof V1NamePoll)
      pollType = "Name";
    if (this instanceof V1ContentPoll)
      pollType = "Content";
    if (this instanceof V1VerifyPoll)
      pollType = "Verify";
    StringBuffer sb = new StringBuffer("[Poll: ");
    sb.append(pollType);
    sb.append(" url set:");
    sb.append(" ");
    sb.append(m_cus.toString());
    sb.append(" ");
    if (m_msg != null) {
      sb.append(m_msg.getOpcodeString());
    }
    sb.append(" key:");
    sb.append(m_key);
    sb.append("]");
    return sb.toString();
  }

  void getConfigValues() {
    /* initialize with our parameters */
    m_agreeVer = ((double)Configuration.getIntParam(PARAM_AGREE_VERIFY,
        DEFAULT_AGREE_VERIFY)) / 100;
    m_disagreeVer = ((double)Configuration.getIntParam(PARAM_DISAGREE_VERIFY,
        DEFAULT_DISAGREE_VERIFY)) / 100;

  }

  /**
   * schedule the hash for this poll.
   * @param hasher the MessageDigest used to hash the content
   * @param timer the Deadline by which we must complete
   * @param key the Object which will be returned from the hasher. Always the
   * message which triggered the hash
   * @param callback the hashing callback to use on return
   * @return true if hash successfully completed.
   */
  abstract boolean scheduleHash(MessageDigest hasher, Deadline timer,
                                Object key,
                                HashService.Callback callback);
  /**
   * schedule a vote by a poll.  we've already completed the hash so we're
   * only interested in how long we have remaining.
   */
  void scheduleVote() {
    if(m_voteTime.expired()) {
      voteInPoll();
    }
    else {
      log.debug("Waiting until " + m_voteTime.toString() + " to vote");
      TimerQueue.schedule(m_voteTime, new VoteTimerCallback(), this);
    }
  }

  /**
   * check the hash result obtained by the hasher with one stored in the
   * originating message.
   * @param hashResult byte array containing the result of the hasher
   * @param vote the Vote to check.
   */
  void checkVote(byte[] hashResult, Vote vote)  {
    boolean verify = false;
    byte[] hashed = vote.getHash();
    log.debug3("Checking "+
              String.valueOf(B64Code.encode(hashResult))+
              " against "+
              vote.getHashString());

    boolean agree = vote.setAgreeWithHash(hashResult);
    PeerIdentity voterID = vote.getVoterIdentity();
    boolean isLocalVote = idMgr.isLocalIdentity(voterID);

    if(!isLocalVote) {
      verify = randomVerify(vote, agree);
    }
    // XXX addVote doesn't need both vote and voterID
    m_tally.addVote(vote, voterID, isLocalVote);
  }

  /**
   * randomly verify a vote.  The random percentage is determined by
   * agreement and reputation of the voter.
   * @param vote the Vote to check
   * @param isAgreeVote true if this vote agreed with ours, false otherwise
   * @return true if we called a verify poll, false otherwise.
   */
  boolean randomVerify(Vote vote, boolean isAgreeVote) {
    PeerIdentity id = vote.getVoterIdentity();
    int max = idMgr.getMaxReputation();
    int weight = idMgr.getReputation(id);
    double verify;
    boolean callVerifyPoll = false;

    if(isAgreeVote) {
      verify = ((double)(max - weight)) / max * m_agreeVer;
    }
    else {
      verify = (((double)weight) / (2*max)) * m_disagreeVer;
    }
    log.debug3("probability of verifying this vote = " + verify);
    try {
      if(ProbabilisticChoice.choose(verify)) {
        long remainingTime = m_deadline.getRemainingTime();
        long now = TimeBase.nowMs();
        long minTime = now + (remainingTime/2) - (remainingTime/4);
        long maxTime = now + (remainingTime/2) + (remainingTime/4);
        long duration = Deadline.atRandomRange(minTime, maxTime).getRemainingTime();
	log.debug("Calling a verify poll...");
	byte[] challenge = vote.getVerifier(); // challenge is the old verifier
	byte[] verifier = m_pollmanager.makeVerifier(duration);
	PollSpec pollspec = new PollSpec(m_pollspec, Poll.VERIFY_POLL);
	LcapMessage reqmsg =
	  LcapMessage.makeRequestMsg(pollspec,
				     null,
				     challenge,
				     verifier,
				     LcapMessage.VERIFY_POLL_REQ,
				     duration,
				     idMgr.getLocalPeerIdentity(Poll.V1_POLL));

	PeerIdentity originatorID = vote.getVoterIdentity();
	log.debug2("sending our verification request to " +
		   originatorID.toString());
	m_pollmanager.sendMessageTo(reqmsg,
				    m_cus.getArchivalUnit(),
				    originatorID);
	// we won't be getting this message so make sure we create our own poll
	BasePoll poll = m_pollmanager.makePoll(reqmsg);
        callVerifyPoll = true;
      }
    }
    catch (IOException ex) {
      log.debug("attempt to request verify poll failed ", ex);
    }
    return callVerifyPoll;
  }

  /**
   * cast our vote for this poll
   */
  void castOurVote() {
    if (m_msg == null) {
      log.error("no vote to cast for " + this);
      return;
    }
    LcapMessage msg;
    PeerIdentity local_id = idMgr.getLocalPeerIdentity(Poll.V1_POLL);
    long remainingTime = m_deadline.getRemainingTime();
    try {
      msg = LcapMessage.makeReplyMsg(m_msg, m_hash, m_verifier, null,
                                     m_replyOpcode, remainingTime, local_id);
      log.debug("vote:" + msg.toString());
      m_pollmanager.sendMessage(msg, m_cus.getArchivalUnit());
    }
    catch(IOException ex) {
      log.info("unable to cast our vote.", ex);
    }
  }

  /**
   * start the poll.
   */
  void startPoll() {
    if (m_pollstate != PS_INITING)
      return;
    m_pollstate = PS_WAIT_HASH;
    if (!scheduleOurHash()) {
      m_pollstate = ERR_SCHEDULE_HASH;
      log.debug("couldn't schedule our hash:" + m_voteTime + ", stopping poll.");
      stopPoll();
      return;
    }
    log.debug3("scheduling poll to complete by " + m_deadline);
    TimerQueue.schedule(m_deadline, new PollTimerCallback(), this);
  }

  /**
   * attempt to schedule our hash.  This will try 3 times to get a deadline
   * that will is successfully scheduled
   * @return boolean true if we successfully schedule hash; false otherwise.
   */
  boolean scheduleOurHash() {
    MessageDigest hasher = getInitedHasher(m_challenge, m_verifier);

    boolean scheduled = false;
    long now = TimeBase.nowMs();
    long remainingTime = m_deadline.getRemainingTime();
    long minTime = now + (remainingTime / 2) - (remainingTime / 4);
    long maxTime = now + (remainingTime / 2) + (remainingTime / 4);
    long lastHashTime = now + (m_hashTime * (m_tally.quorum + 1));

    for (int i = 0; !scheduled && i < 2; i++) {
      m_voteTime = Deadline.atRandomRange(minTime, maxTime);
      long curTime = m_voteTime.getExpirationTime();
      log.debug3("Trying to schedule our hash at " + m_voteTime);
      scheduled = scheduleHash(hasher, m_voteTime, m_msg, new PollHashCallback());
      if (!scheduled) {
        if (curTime < lastHashTime) {
          maxTime += curTime - minTime;
          minTime = curTime;
        }
        else {
          log.debug("Unable to schedule our hash before " + lastHashTime);
          break;
        }
      }
    }
    return scheduled;
  }

  /**
   * cast our vote in the current poll
   */
  void voteInPoll() {
    //we don't vote if we're winning by a landslide
    if(!m_tally.isLeadEnough()) {
      castOurVote();
    }
    m_pollstate = PS_WAIT_TALLY;
  }


  /**
   * is our poll currently in an error condition
   * @return true if the poll state is an error value
   */
  boolean isErrorState() {
    return m_pollstate < 0;
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
      m_pollstate = PS_COMPLETE;
    }
    m_pollmanager.closeThePoll(m_key);
    log.debug3("closed the poll:" + m_key);
  }

  /**
   * prepare to check a vote in a poll.  This should check any conditions that
   *  might make running a vote check unneccessary.
   * @param msg the message which is triggering the poll
   * @return boolean true if the poll should run, false otherwise
   */
  boolean shouldCheckVote(LcapMessage msg) {
    PeerIdentity voterID = msg.getOriginatorID();

    // make sure we haven't already voted
    if(m_tally.hasVoted(voterID)) {
      log.warning("Ignoring multiple vote from " + voterID);
      int oldRep = idMgr.getReputation(voterID);
      idMgr.changeReputation(voterID, IdentityManager.REPLAY_DETECTED);
      int newRep = idMgr.getReputation(voterID);
      m_tally.adjustReputation(voterID, newRep-oldRep);
      return false;
    }

    // make sure our vote will actually matter
    if(m_tally.isLeadEnough())  {
      log.info(m_key + " lead is enough");
      return false;
    }

    if(!m_tally.canResolve()) {
      log.info(m_key +
               " unable to resolve split poll, ignoring additional votes.");
    }
      // are we too busy
    if(tooManyPending())  {
      log.info(m_key + " too busy to count " + m_pendingVotes + " votes");
      return false;
    }

    return true;
  }

  /**
   * start the hash required for a vote cast in this poll
   */
  void startVoteCheck() {
    m_pendingVotes++;
    log.debug3("Number pending votes = " + m_pendingVotes);
  }

  /**
   * stop and record a vote cast in this poll
   */
  void stopVoteCheck() {
    m_pendingVotes--;
    log.debug3("Number pending votes = " + m_pendingVotes);
  }

  /**
   * are there too many votes waiting to be tallied
   * @return true if we have a quorum worth of votes already pending
   */
  boolean tooManyPending() {
    return m_pendingVotes > m_tally.quorum + 1;
  }

  Vote copyVote(Vote vote, boolean agree) {
    Vote v = new Vote(vote);
    v.agree = agree;
    return v;
  }

  /**
   * Return a hasher preinited with the challenge and verifier
   * @param challenge the challenge bytes
   * @param verifier the verifier bytes
   * @return a MessageDigest
   */
  MessageDigest getInitedHasher(byte[] challenge, byte[] verifier) {
    MessageDigest hasher = m_pollmanager.getHasher(m_msg);
    hasher.update(challenge, 0, challenge.length);
    hasher.update(verifier, 0, verifier.length);
    log.debug3("hashing: C[" +String.valueOf(B64Code.encode(challenge)) + "] "
              +"V[" + String.valueOf(B64Code.encode(verifier)) + "]");
    return hasher;
  }

  public byte[] getChallenge() {
    return m_challenge;
  }

  public byte[] getVerifier() {
    return m_verifier;
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
        m_hash  = hasher.digest();
        log.debug2("Hash on " + urlset + " complete: "+
                  String.valueOf(B64Code.encode(m_hash)));
        m_pollstate = PS_WAIT_VOTE;
        scheduleVote();
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
        checkVote(out_hash, vote);
        stopVoteCheck();
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
        voteInPoll();
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
