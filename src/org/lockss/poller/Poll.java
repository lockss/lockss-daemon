/*
* $Id: Poll.java,v 1.11 2002-11-16 02:31:28 claire Exp $
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
import org.lockss.daemon.*;
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;

/**
 * <p>Abstract base class for all poll objects.</p>
 * @author Claire Griffin
 * @version 1.0
 */

public abstract class Poll {
  static final int DEFAULT_QUORUM = 3;
  static final int DEFAULT_DURATION = 6*3600*1000;
  static final int DEFAULT_VOTEDELAY = DEFAULT_DURATION/2;
  static final int DEFAULT_VOTERANGE = DEFAULT_DURATION/4;

  static Logger log=Logger.getLogger("Poll",Logger.LEVEL_DEBUG);

  LcapMessage m_msg;          // The message which triggered the poll
  int m_quorum = 0;           // The caller's quorum value
  int m_quorumWt = 0;         // The quorum weights
  int m_agree = 0;            // The # of votes we've heard that agree with us
  int m_agreeWt = 0;          // the sum of the the agree weights
  int m_disagree = 0;         // The # of votes we've heard that disagree with us
  int m_disagreeWt = 0;       // the sum of the disagree weights

  ArchivalUnit m_arcUnit; // the url as an archival unit
  CachedUrlSet m_urlSet;  // the cached url set retrieved from the archival unit
  String m_url;           // the url for this poll
  String m_regExp;        // the regular expression for the poll

  byte[] m_challenge;     // The caller's challenge string
  byte[] m_verifier;      // Our verifier string - hash of secret
  byte[] m_hash;          // Our hash of challenge, verifier and content(S)

  Deadline m_voteTime;    // when to vote
  Deadline m_deadline;    // when election is over

  LcapIdentity m_caller;       // who called the poll
  boolean m_voted;         // true if we've voted
  int m_replyOpcode = -1;  // opcode used to reply to poll
  long m_hashTime;         // an estimate of the time it will take to hash
  int m_counting;          // the number of polls currently active
  long m_createTime;       // poll creation time
  boolean m_voteChecked;   // have we voted on this specific message????
  String m_key;            // the string we use to store this poll

  /**
   * create a new poll from a message
   *
   * @param msg the <code>Message</code> which contains the information
   * @param urlSet the CachedUrlSet on which this poll will operate
   * needed to create this poll.
   */
  Poll(LcapMessage msg, CachedUrlSet urlSet) {
    m_msg = msg;
    m_urlSet = urlSet;

    m_createTime = System.currentTimeMillis();

    // now copy the msg elements we need
    m_url = msg.getTargetUrl();
    m_regExp = msg.getRegExp();
    m_arcUnit = m_urlSet.getArchivalUnit();
    m_hashTime = m_urlSet.estimatedHashDuration();
    m_deadline = new Deadline(msg.getDuration());
    m_challenge = msg.getChallenge();
    m_verifier = PollManager.makeVerifier();
    m_caller = msg.getOriginID();
    m_voteChecked = false;
    m_voted = false;
    m_key = PollManager.makeKey(m_challenge);

  }

  abstract void receiveMessage(LcapMessage msg);



  public String toString() {
    StringBuffer sb = new StringBuffer(m_url);
    sb.append(" ");
    sb.append(m_regExp);
    sb.append(" ");
    sb.append(m_msg.getOpcode());

    return sb.toString();
  }


  /**
   * schedule the hash for this poll.
   * @param timer the Deadline by which we must complete
   * @param key the Object which will be returned from the hasher. Always the
   * message which triggered the hash
   * @param callback the hashing callback to use on return
   * @return true if hash successfully completed.
   */
  abstract boolean scheduleHash(Deadline timer, Object key,
                                HashService.Callback callback);

  /**
   * schedule a vote by a poll.  we've already completed the hash so we're
   * only interested in how long we have remaining.
   */
  void scheduleVote() {
    long time_remaining = m_deadline.getRemainingTime();
    long vote_delay = time_remaining/2;
    long vote_dev = time_remaining/4;

    m_voteTime = new ProbabilisticTimer(vote_delay, vote_dev);
    TimerQueue.schedule(m_voteTime, new VoteTimerCallback(), this);
  }

  /**
   * check the hash result obtained by the hasher with one stored in the
   * originating method.
   * @param hashResult byte array containing the result of the hasher
   * @param msg the original Message.
   */
  void checkVote(byte[] hashResult, LcapMessage msg)  {
    byte[] hashed = msg.getHashed();
    if(Arrays.equals(hashed, hashResult)) {
      handleAgreeVote(msg);
    }
    else {
      handleDisagreeVote(msg);
    }
  }


  /**
   * handle an agree vote
   * @param msg the <code>Message</code> that we agree with
   */
  void handleAgreeVote(LcapMessage msg) {
    int weight = msg.getOriginID().getReputation();
    synchronized (this) {
      m_agree++;
      m_agreeWt += weight;
    }

    log.info("I agree with " + msg.toString() + " rep " + weight);
    PollManager.rememberVote(msg, true);
    if (!msg.isLocal()) {
      try {
        // TODO: calculate the actual percentage
        VerifyPoll.randomRequestVerify(msg,50);
      }
      catch (IOException ex) {
        log.debug("attempt to verify random failed.");
      }
    }
  }

  /**
   * handle a disagree vote
   * @param msg the <code>Message</code> that we disagree with
   */
  void handleDisagreeVote(LcapMessage msg) {
    int weight = msg.getOriginID().getReputation();
    boolean local = msg.isLocal();

    synchronized (this) {
      m_disagree++;
      m_disagreeWt += weight;
    }
    if (local) {
      log.error("I disagree with myself about" + msg.toString()
                + " rep " + weight);
    }
    else {
      log.info("I disagree with" + msg.toString() + " rep " + weight);
    }
    PollManager.rememberVote(msg, false);
    if (!local) {
      try {
        // TODO: calculate the actual percentage
        VerifyPoll.randomRequestVerify(msg, 50);
      }
      catch (IOException ex) {
        log.debug("attempt to verify random failed.");
      }
    }
  }

  /**
   * tally the poll results
   */
  protected void tally() {
    int yes;
    int no;
    int yesWt;
    int noWt;
    synchronized (this) {
      yes = m_agree;
      no = m_disagree;
      yesWt = m_agreeWt;
      noWt = m_disagreeWt;
    }
    PollManager.removePoll(m_key);
    PollManager.rememberTally(m_arcUnit, this, yes, no, yesWt, noWt, m_replyOpcode);
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
      LcapComm.sendMessage(msg,m_arcUnit);
    }
    catch(IOException ioe) {

    }
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
   * start the poll.
   */
  void startPoll() {
    ProbabilisticTimer pt = new ProbabilisticTimer(m_msg.getDuration());
    if(!scheduleHash( pt, m_msg, new PollHashCallback())) {
      // TODO: terminate this poll
      return;
    }
    TimerQueue.schedule(m_deadline, new PollTimerCallback(), this);
  }

  /**
   * cast a vote in the current poll
   */
  void voteInPoll() {
    //we only vote if we don't already have a quorum
    if((m_agree - m_disagree) <= m_quorum) {
      vote();
      m_voted = true;
    }
  }

  /**
   * finish the poll once the deadline has expired
   */
  void stopPoll() {
    // prevent any further activity on this poll by recording the challenge
    // and dropping any further packets that match it.
    PollManager.closeThePoll(m_urlSet, m_challenge);

    // if we have a quorum, record the results
    if((m_agree + m_disagree) >= m_quorum) {
      tally();
    }
    else { // we don't have a quorum
      /// XXX we need to notify that we failed and why;
    }
  }

  void startVote() {
  }

  void stopVote() {
    m_counting--;
  }


  MessageDigest getHasher() {
    MessageDigest hasher = null;
    try {
      hasher = MessageDigest.getInstance(PollManager.HASH_ALGORITHM);
    } catch (NoSuchAlgorithmException ex) {
      log.error("Unable to run - no hasher");
    }

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
      boolean hash_completed = e == null ? true : false;

      if(hash_completed)  {
        m_hash  = hasher.digest();
        LcapMessage msg = (LcapMessage)cookie;
        checkVote(m_hash, msg);
        scheduleVote();
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
      voteInPoll();
    }
  }

  class PollTimerCallback implements TimerQueue.Callback {
    /**
     * Called when the timer expires.
     * @param cookie  data supplied by caller to schedule()
     */
    public void timerExpired(Object cookie) {
      stopPoll();
    }
  }

}