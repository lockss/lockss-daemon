/*
* $Id: Poll.java,v 1.61 2003-04-02 06:44:12 claire Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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


import gnu.regexp.*;
import org.mortbay.util.B64Code;
import org.lockss.daemon.*;
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;
import org.lockss.state.PollHistory;
import org.lockss.state.NodeManager;
import org.lockss.daemon.status.*;


/**
 * <p>Abstract base class for all poll objects.</p>
 * @author Claire Griffin
 * @version 1.0
 */

public abstract class Poll implements Serializable {
  public static final int NAME_POLL = 0;
  public static final int CONTENT_POLL = 1;
  public static final int VERIFY_POLL = 2;

  public static final String[] PollName = {"Name", "Content", "Verify"};

  static final String PARAM_AGREE_VERIFY = Configuration.PREFIX +
      "poll.agreeVerify";
  static final String PARAM_DISAGREE_VERIFY = Configuration.PREFIX +
      "poll.disagreeVerify";
  static final String PARAM_MARGIN = Configuration.PREFIX +
      "poll.margin";

  static final int DEFAULT_MARGIN = 60;
  static final int DEFAULT_AGREE_VERIFY = 20;
  static final int DEFAULT_DISAGREE_VERIFY = 90;
  static final String[] ERROR_STRINGS = {"Poll Complete","Hash Schedule Error",
      "Hashing Error", "No Quroum", "IO Error"
  };
  public static final int ERR_SCHEDULE_HASH = -1;
  public static final int ERR_HASHING = -2;
  public static final int ERR_NO_QUORUM = -3;
  public static final int ERR_IO = -4;

  static final int PS_INITING = 0;
  static final int PS_WAIT_HASH = 1;
  static final int PS_WAIT_VOTE = 2;
  static final int PS_WAIT_TALLY = 3;
  static final int PS_COMPLETE = 4;

  static Logger log=Logger.getLogger("Poll");

  LcapMessage m_msg;      // The message which triggered the poll

  double m_agreeVer = 0;     // the max percentage of time we will verify
  double m_disagreeVer = 0;  // the max percentage of time we will verify
  double m_margin = 0;    // the margin by which we must win or lose
  CachedUrlSet m_cus;     // the cached url set retrieved from the archival unit
  PollSpec m_pollspec;
  byte[] m_challenge;     // The caller's challenge string
  byte[] m_verifier;      // Our verifier string - hash of secret
  byte[] m_hash;          // Our hash of challenge, verifier and content(S)

  Deadline m_voteTime;    // when to vote
  Deadline m_deadline;    // when election is over

  LcapIdentity m_caller;   // who called the poll
  int m_replyOpcode = -1;  // opcode used to reply to poll
  long m_hashTime;         // an estimate of the time it will take to hash
  long m_createTime;       // poll creation time
  String m_key;            // the string we used to id this poll
  int m_pollstate;         // one of state constants above
  int m_pendingVotes = 0;  // the number of votes waiting to be tallied

  PollTally m_tally;         // the vote tallier
  PollManager m_pollmanager; // the pollmanager which should be used by this poll.
  IdentityManager idMgr;

  /**
   * create a new poll from a message
   *
   * @param msg the <code>Message</code> which contains the information
   * @param pollspec the PollSpec on which this poll will operate
   * needed to create this poll.
   * @param pm the pollmanager
   */
  Poll(LcapMessage msg, PollSpec pollspec, PollManager pm) {
    m_pollmanager = pm;
    idMgr = pm.getIdentityManager();
    m_msg = msg;
    m_pollspec = pollspec;
    m_cus = pollspec.getCachedUrlSet();

    // now copy the msg elements we need
    m_hashTime = m_cus.estimatedHashDuration();
    m_deadline = Deadline.in(msg.getDuration());
    m_challenge = msg.getChallenge();
    m_verifier = m_pollmanager.makeVerifier();
    m_caller = idMgr.findIdentity(msg.getOriginAddr());
    m_key = msg.getKey();

    m_createTime = TimeBase.nowMs();

    // make a new vote tally
    m_tally = new PollTally(this, -1, m_createTime, msg.getDuration(),
                            pm.getQuorum(), msg.getHashAlgorithm());
    m_pollstate = PS_INITING;
    getConfigValues();
  }


  /**
   * create a human readable string representation of this poll
   * @return a String
   */
  public String toString() {
    StringBuffer sb = new StringBuffer("[Poll: ");
    sb.append("url set:");
    sb.append(" ");
    sb.append(m_cus.toString());
    sb.append(" ");
    sb.append(m_msg.getOpcodeString());
    sb.append(" key:");
    sb.append(m_key);
    sb.append("]");
    return sb.toString();
  }

  /**
   * Returns true if the poll belongs to this Identity
   * @return true if  we called the poll
   */
  public boolean isMyPoll() {
    return idMgr.isLocalIdentity(m_caller);
  }

  void getConfigValues() {
    /* initialize with our parameters */
    m_agreeVer = ((double)Configuration.getIntParam(PARAM_AGREE_VERIFY,
        DEFAULT_AGREE_VERIFY)) / 100;
    m_disagreeVer = ((double)Configuration.getIntParam(PARAM_DISAGREE_VERIFY,
        DEFAULT_DISAGREE_VERIFY)) / 100;

    m_margin = ((double)Configuration.getIntParam(PARAM_MARGIN,
        DEFAULT_MARGIN)) / 100;

  }

  abstract void receiveMessage(LcapMessage msg);


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
                                Serializable key,
                                HashService.Callback callback);

  /**
   * schedule a vote by a poll.  we've already completed the hash so we're
   * only interested in how long we have remaining.
   */
  void scheduleVote() {
    m_voteTime = Deadline.atRandomBefore(m_deadline);
    log.debug("Waiting until at most " + m_deadline.toString() + " to vote");
    TimerQueue.schedule(m_voteTime, new VoteTimerCallback(), this);
    m_pollstate = PS_WAIT_VOTE;
  }


  /**
   * check the hash result obtained by the hasher with one stored in the
   * originating message.
   * @param hashResult byte array containing the result of the hasher
   * @param vote the Vote to check.
   */
  void checkVote(byte[] hashResult, Vote vote)  {
    byte[] hashed = vote.getHash();
    log.debug3("Checking "+
              String.valueOf(B64Code.encode(hashResult))+
              " against "+
              vote.getHashString());

    boolean agree = vote.setAgreeWithHash(hashResult);
    LcapIdentity id = idMgr.findIdentity(vote.getIDAddress());
    m_tally.addVote(vote, id, idMgr.isLocalIdentity(id));

    if(!idMgr.isLocalIdentity(vote.getIDAddress())) {
      randomVerify(vote, agree);
    }
  }

  /**
   * randomly verify a vote.  The random percentage is determined by
   * agreement and reputation of the voter.
   * @param vote the Vote to check
   * @param isAgreeVote true if this vote agreed with ours, false otherwise
   */
  void randomVerify(Vote vote, boolean isAgreeVote) {
    LcapIdentity id = idMgr.findIdentity(vote.getIDAddress());
    int max = idMgr.getMaxReputaion();
    int weight = id.getReputation();
    double verify;
    if(isAgreeVote) {
      verify = ((double)(max - weight)) / max * m_agreeVer;
    }
    else {
      verify = ((double)weight) / max * m_disagreeVer;
    }
    try {
      if(ProbabilisticChoice.choose(verify)) {
        m_pollmanager.requestVerifyPoll(m_pollspec, m_tally.duration, vote);
      }
    }
    catch (IOException ex) {
      log.debug("attempt to verify vote failed:", ex);
    }
  }

  /**
   * cast our vote for this poll
   */
  void castOurVote() {
    LcapMessage msg;
    LcapIdentity local_id = idMgr.getLocalIdentity();
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
    if(m_pollstate != PS_INITING)
      return;
    Deadline pt = Deadline.in(m_msg.getDuration());
    MessageDigest hasher = getInitedHasher(m_challenge, m_verifier);
    m_pollstate = PS_WAIT_HASH;
    if(!scheduleHash(hasher, pt, m_msg, new PollHashCallback())) {
      m_pollstate = ERR_SCHEDULE_HASH;
      log.debug("couldn't schedule our hash:" + m_msg.getDuration()
                + " stopping poll");
      stopPoll();
      return;
    }
    log.debug3("scheduling poll to complete by " + m_deadline);
    TimerQueue.schedule(m_deadline, new PollTimerCallback(), this);
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
    if(!isErrorState()) {
      m_pollstate = m_tally.haveQuorum() ? PS_COMPLETE : ERR_NO_QUORUM;
    }
    if(isErrorState()) {
      log.debug("poll stopped with error: " + ERROR_STRINGS[ -m_pollstate]);
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
    LcapIdentity voter = idMgr.findIdentity(msg.getOriginAddr());

    // make sure we haven't already voted
    if(m_tally.hasVoted(voter)) {
      log.warning("Ignoring multiple vote from " + voter);
    }

    // make sure our vote will actually matter
    if(m_tally.isLeadEnough())  {
      log.info(m_key + " lead is enough");
      return false;
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

  /**
   * Return the poll spec used by this poll
   * @return the PollSpec
   */
  public PollSpec getPollSpec() {
    return m_pollspec;
  }

  /**
   * get the message used to define this Poll
   * @return <code>Message</code>
   */
  public LcapMessage getMessage() {
    return m_msg;
  }

  /**
   * get the VoteTally for this Poll
   * @return VoteTally for this poll
   */
  public PollTally getVoteTally() {
    return m_tally;
  }

  public String getKey() {
    return m_key;
  }

  double getMargin() {
    return m_margin;
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
        log.debug("Hash on " + urlset + " complete: "+
                  String.valueOf(B64Code.encode(m_hash)));
        scheduleVote();
      }
      else {
        log.debug("Hash failed : " + e.getMessage());
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
