/*
* $Id: Poll.java,v 1.31 2002-12-21 01:15:45 aalto Exp $
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


/**
 * <p>Abstract base class for all poll objects.</p>
 * @author Claire Griffin
 * @version 1.0
 */

public abstract class Poll implements Serializable {
  static final String PARAM_QUORUM = Configuration.PREFIX + "poll.quorum";
  static final String PARAM_AGREE_VERIFY = Configuration.PREFIX +
      "poll.agreeVerify";
  static final String PARAM_DISAGREE_VERIFY = Configuration.PREFIX +
      "poll.disagreeVerify";

  static final int DEFAULT_QUORUM = 5;
  static final int DEFAULT_AGREE_VERIFY = 20;
  static final int DEFAULT_DISAGREE_VERIFY = 90;

  static final int ERR_SCHEDULE_HASH = -1;
  static final int ERR_HASHING = -2;
  static final int ERR_NO_QUORUM = -3;
  static final int ERR_IO = -4;

  static final int PS_INITING = 0;
  static final int PS_WAIT_HASH = 1;
  static final int PS_WAIT_VOTE = 2;
  static final int PS_WAIT_TALLY = 3;
  static final int PS_COMPLETE = 4;

  static Logger log=Logger.getLogger("Poll");

  LcapMessage m_msg;      // The message which triggered the poll

  double m_agreeVer = 0;     // the max percentage of time we will verify
  double m_disagreeVer = 0;  // the max percentage of time we will verify

  ArchivalUnit m_arcUnit; // the url as an archival unit
  CachedUrlSet m_urlSet;  // the cached url set retrieved from the archival unit
  String m_url;           // the url for this poll
  String m_regExp;        // the regular expression for the poll

  byte[] m_challenge;     // The caller's challenge string
  byte[] m_verifier;      // Our verifier string - hash of secret
  byte[] m_hash;          // Our hash of challenge, verifier and content(S)

  Deadline m_voteTime;    // when to vote
  Deadline m_deadline;    // when election is over

  LcapIdentity m_caller;   // who called the poll
  int m_replyOpcode = -1;  // opcode used to reply to poll
  long m_hashTime;         // an estimate of the time it will take to hash
  long m_createTime;       // poll creation time
  String m_key;            // the string we use to store this poll
  int m_pollstate;         // one of state constants above
  int m_pendingVotes;      // the number of votes waiting to be tallied
  VoteTally m_tally;       // the vote tallier
  PollManager m_pollmanager; // the pollmanager which should be used by this poll.

  /**
   * create a new poll from a message
   *
   * @param msg the <code>Message</code> which contains the information
   * @param urlSet the CachedUrlSet on which this poll will operate
   * needed to create this poll.
   * @param pm the pollmanager
   */
  Poll(LcapMessage msg, CachedUrlSet urlSet, PollManager pm) {
    /* initialize with our parameters */
    m_agreeVer = ((double)Configuration.getIntParam(PARAM_AGREE_VERIFY,
        DEFAULT_AGREE_VERIFY)) / 100;
    m_disagreeVer = ((double)Configuration.getIntParam(PARAM_DISAGREE_VERIFY,
        DEFAULT_DISAGREE_VERIFY)) / 100;
    m_pollmanager = pm;
    m_msg = msg;
    m_urlSet = urlSet;

    m_createTime = TimeBase.nowMs();
    m_tally = new VoteTally(msg.getOpcode(),msg.getDuration());

    // now copy the msg elements we need
    m_url = msg.getTargetUrl();
    m_regExp = msg.getRegExp();
    m_arcUnit = m_urlSet.getArchivalUnit();
    m_hashTime = m_urlSet.estimatedHashDuration();
    m_deadline = Deadline.in(msg.getDuration());
    m_challenge = msg.getChallenge();
    m_verifier = m_pollmanager.makeVerifier();
    m_caller = msg.getOriginID();
    m_key = m_pollmanager.makeKey(m_challenge);
    m_pollstate = PS_INITING;
  }


  /**
   * create a human readable string representation of this poll
   * @return a String
   */
  public String toString() {
    StringBuffer sb = new StringBuffer("[Poll: ");
    sb.append("url set:");
    sb.append(" ");
    sb.append(m_urlSet.toString());
    sb.append(" ");
    sb.append(m_msg.getOpcodeString());
    sb.append(" key:");
    sb.append(m_key);
    sb.append("]");
    return sb.toString();
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
   * @param msg the original Message.
   */
  void checkVote(byte[] hashResult, LcapMessage msg)  {
    byte[] hashed = msg.getHashed();
    log.debug("Checking "+
              String.valueOf(B64Code.encode(hashResult))+
              " against "+
              String.valueOf(B64Code.encode(hashed)));

    boolean agree = Arrays.equals(hashed, hashResult);
    m_tally.addVote(new Vote(msg, agree));
    if(!msg.isLocal()) {
      randomVerify(msg, agree);
    }
  }

  /**
   * randomly verify a vote.  The random percentage is determined by
   * agreement and reputation of the voter.
   * @param msg the LcapMessage to check
   * @param isAgreeVote true if this vote agreed with ours, false otherwise
   */
  void randomVerify(LcapMessage msg, boolean isAgreeVote) {
    LcapIdentity id = msg.getOriginID();
    int max = id.getMaxReputaion();
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
        m_pollmanager.requestVerifyPoll(msg);
      }
    }
    catch (IOException ex) {
      log.debug("attempt to verify vote failed:", ex);
    }
  }

  /**
   * tally the poll results
   */
  protected void tally() {
    //NodeManager.updatePollResults(m_urlSet, m_tally);
  }

  /**
   * cast our vote for this poll
   */
  void vote() {
    LcapMessage msg;
    LcapIdentity local_id = LcapIdentity.getLocalIdentity();
    long remainingTime = m_deadline.getRemainingTime();
    try {
      msg = LcapMessage.makeReplyMsg(m_msg, m_hash, m_verifier, m_replyOpcode,
                                     remainingTime, local_id);
      log.debug("vote:" + msg.toString());
      m_pollmanager.sendMessage(msg,m_arcUnit);
    }
    catch(IOException ex) {
      //XXX how serious is this
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
    if(!scheduleHash(hasher, pt, m_msg, new PollHashCallback())) {
      m_pollstate = ERR_SCHEDULE_HASH;
      log.debug("couldn't schedule hash - stopping poll");
      stopPoll();
      return;
    }
    TimerQueue.schedule(m_deadline, new PollTimerCallback(), this);
    m_pollstate = PS_WAIT_HASH;
  }

  /**
   * cast our vote in the current poll
   */
  void voteInPoll() {
    //we don't vote if we're winning by a landslide
    if(!m_tally.isLeadEnough()) {
      vote();
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
    m_pollstate = m_tally.haveQuorum() ? PS_COMPLETE : ERR_NO_QUORUM;

    if(isErrorState()) {
      // XXX - notify node manager of failed poll
      m_pollstate = PS_COMPLETE;
    }
    else if(m_pollstate == PS_WAIT_TALLY){
      tally();
    }
    m_pollmanager.closeThePoll(m_key);
    log.debug("closed the poll:" + m_key);
  }

  /**
   * start the hash required for a vote cast in this poll
   */
  void startVote() {
    m_pendingVotes++;
  }

  /**
   * stop and record a vote cast in this poll
   */
  void stopVote() {
    m_pendingVotes--;
  }


  boolean tooManyPending() {
    return m_pendingVotes > m_tally.quorum + 1;
  }

  /**
   * get the message used to define this Poll
   * @return <code>Message</code>
   */
  LcapMessage getMessage() {
    return m_msg;
  }

  /**
   * get the Archival Unit used by this poll.
   * @return the <code>ArchivalUnit</code>
   */
  ArchivalUnit getArchivalUnit() {
    return m_arcUnit;
  }

  /**
   * get the Cached Url Set on which this poll is running
   * @return CachedUrlSet for this poll
   */
  CachedUrlSet getCachedUrlSet() {
    return m_urlSet;
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
    log.debug("hashing: C[" +String.valueOf(B64Code.encode(challenge)) + "] "
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
      if(m_pollstate != PS_WAIT_HASH)
        return;
      boolean hash_completed = e == null ? true : false;

      if(hash_completed)  {
        m_hash  = hasher.digest();
        log.debug("Hash on " + urlset + " complete: "+
                  String.valueOf(B64Code.encode(m_hash)));
        scheduleVote();
      }
      else {
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
        LcapMessage msg = (LcapMessage)cookie;
        checkVote(out_hash, msg);
      }
    }
  }

  class VoteTimerCallback implements TimerQueue.Callback {
    /**
     * Called when the timer expires.
     * @param cookie  data supplied by caller to schedule()
     */
    public void timerExpired(Object cookie) {
      log.debug("VoteTimerCallback called, checking if I should vote");
      if(m_pollstate == PS_WAIT_VOTE) {
        log.debug("I should vote");
        voteInPoll();
        log.debug("Just voted");
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


  /**
   * VoteTally is a struct-like class which maintains the current state of
   * votes within a poll.
   */
  public class VoteTally {
    public int type;
    public long startTime;
    public long duration;
    public int numYes;       // The # of votes that agree with us
    public int numNo;        // The # of votes that disagree with us
    public int wtYes;        // The weight of the votes that agree with us
    public int wtNo;         // The weight of the votes that disagree with us
    public int quorum;       // The # of votes needed to have a quorum
    public ArrayList pollVotes;
    public String hashAlgorithm; // the algorithm used to hash this poll

    VoteTally(int type, long startTime, long duration, int numYes,
              int numNo, int wtYes, int wtNo) {
      this.type = type;
      this.startTime = startTime;
      this.duration = duration;
      this.numYes = numYes;
      this.numNo = numNo;
      this.wtYes = wtYes;
      this.wtNo = wtNo;
      quorum = Configuration.getIntParam(PARAM_QUORUM, DEFAULT_QUORUM);
      pollVotes = new ArrayList(quorum * 2);
      hashAlgorithm = m_msg.getHashAlgorithm();
    }

    VoteTally(int type, long duration) {
      this(type, m_createTime, duration, 0, 0, 0, 0);
    }

    /**
     * did we win or lose the last poll.
     * @return true iff only if number of agree votes exceeds the number of
     * disagree votes and the reputation of the ids we agreed with >= with those
     * we disagreed with. false if we had and error or disagree votes exceed
     * agree votes.
     */
    public boolean didWinPoll() {
      if(!isErrorState()) {
        return (numYes > numNo) && (wtYes >= wtNo);
      }
      return false;
    }

    /**
     * get the error state for this poll
     * @return 0 == NOERR or one of the poll err conditions
     */
    public int getErr() {
      if(isErrorState()) {
        return m_pollstate;
      }
      return 0;
    }

    boolean isLeadEnough() {
      return (numYes - numNo) > quorum;
    }

    boolean haveQuorum() {
      return numYes + numNo >= quorum;
    }


    void addVote(Vote vote) {
      LcapIdentity id = vote.getIdentity();
      int weight = id.getReputation();

      synchronized (this) {
        if(vote.isAgreeVote()) {
          numYes++;
          wtYes += weight;
          log.info("I agree with " + vote + " rep " + weight);
        }
        else {
          numNo++;
          wtNo += weight;
          if (id.isLocalIdentity()) {
            log.error("I disagree with myself about " + vote + " rep " + weight);
          }
          else {
            log.info("I disagree with " + vote + " rep " + weight);
          }

        }
      }
      pollVotes.add(vote);
    }
  }
}
