/*
* $Id: Poll.java,v 1.9 2002-11-14 03:58:09 claire Exp $
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

public abstract class Poll implements Runnable {
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
  ProbabilisticTimer m_deadline;    // when election is over

  LcapIdentity m_caller;       // who called the poll
  boolean m_voted;         // true if we've voted
  int m_replyOpcode = -1;  // opcode used to reply to poll
  long m_hashTime;         // an estimate of the time it will take to hash
  int m_counting;          // the number of polls currently active
  long m_createTime;       // poll creation time
  boolean m_voteChecked;   // have we voted on this specific message????
  Thread m_thread;         // the thread for this poll
  String m_key;            // the string we use to store this poll
  HashMap m_voteCheckers;  // the vote checkers for this poll

  /**
   * create a new poll from a message
   *
   * @param msg the <code>Message</code> which contains the information
   * @param urlSet the CachedUrlSet on which this poll will operate
   * needed to create this poll.
   */
  Poll(LcapMessage msg, CachedUrlSet urlSet) {
    m_msg = msg;

    m_createTime = System.currentTimeMillis();

    // now copy the msg elements we need
    m_url = msg.getTargetUrl();
    m_regExp = msg.getRegExp();
    m_urlSet = urlSet;
    m_arcUnit = m_urlSet.getArchivalUnit();
    m_hashTime = m_urlSet.estimatedHashDuration();
    m_deadline = new ProbabilisticTimer(msg.getDuration());
    m_challenge = msg.getChallenge();
    m_verifier = PollManager.makeVerifier();
    m_caller = msg.getOriginID();
    m_voteChecked = false;
    m_voted = false;
    m_voteCheckers = new HashMap();
    m_key = PollManager.makeKey(m_url, m_regExp, msg.getOpcode());

  }

  /**
   * handle a message which may be a incoming vote
   * @param msg the Message to handle
   * @param hashTime the time available for a hash
   */
  public synchronized void receiveMessage(LcapMessage msg, long hashTime) {
    VoteChecker vc = null;
    int opcode = msg.getOpcode();

    switch(opcode) {
      case LcapMessage.CONTENT_POLL_REP:
        vc = new ContentPoll.CPVoteChecker(this, msg, m_urlSet, hashTime);
        break;
      case LcapMessage.NAME_POLL_REP:
        vc = new NamePoll.NPVoteChecker(this, msg, m_urlSet, hashTime);
        break;
      case LcapMessage.VERIFY_POLL_REP:
        vc = new VerifyPoll.VPVoteChecker(this,msg,m_urlSet, hashTime);
        break;
    }
    // start the poll and increment our poll counter
    if(vc != null) {
      m_counting++;
      vc.start();
    }
  }


  /**
   * start the poll thread and run this poll.
   */
  public void startPoll() {
    m_thread.start();
  }

  public String toString() {
    StringBuffer sb = new StringBuffer(m_url);
    sb.append(" ");
    sb.append(m_regExp);
    sb.append(" ");
    sb.append(m_msg.getOpcode());

    return sb.toString();
  }

  String okToRun() {
    String ret = null;

    if(m_deadline.expired()) {
      ret = "aborting because deadline expired";
    }
    return ret;
  }

  /**
   * run method for this thread
   */
  public void run() {
    Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
   /* check to see if we should run this poll period */
    String runErr = okToRun();

    if(runErr != null) {
      log.debug(runErr);
      abort();
      return;
    }

    scheduleHash(m_challenge, m_verifier, m_urlSet,m_deadline, this);

    try {
    /* wait for timer to elapse */
      while(!m_deadline.expired()) {
        if(!m_voted && (m_voteTime != null)) {
          // check to see if we should vote
          if(m_voteTime.expired()) {
            //we only vote if we don't already have a quorum
            if((m_agree - m_disagree) <= m_quorum) {
              m_voted = true;
              vote();
            }
          }
        }
        try {
          m_thread.sleep(60 * 1000);
        }
        catch(InterruptedException ie) {
        }
      }

      endPoll();
    }
    catch(IOException ioe) {
      log.error("election failed: " + ioe.toString());
    }
  }


  /**
   * schedule the hash for this poll.
   * @param C the challenge
   * @param V the verifier
   * @param urlSet the cachedUrlSet
   * @param timer the probabilistic timer
   * @param key the Object which will be returned from the hasher, either the
   * poll or the VoteChecker.
   * @return true if hash successfully completed.
   */
  abstract boolean scheduleHash(byte[] C, byte[] V, CachedUrlSet urlSet,
                                ProbabilisticTimer timer, Object key);

  /**
   * prepare to run a poll.  This should check any conditions that might
   * make running a poll unneccessary.
   * @param msg the message which is triggering the poll
   * @return boolean true if the poll should run, false otherwise
   */
  abstract boolean preparePoll(LcapMessage msg);

  /**
   * check the hash result obtained by the hasher with one stored in the
   * originating method.
   * @param hashResult byte array containing the result of the hasher
   * @param msg the original Message.
   */
  void checkVote(byte[] hashResult, LcapMessage msg)  {
    byte[] H = msg.getHashed();
    if(Arrays.equals(H, hashResult)) {
      handleAgreeVote(msg);
    }
    else {
      handleDisagreeVote(msg);
    }
  }

  /**
   * expire any deadlines and stop this thread
   */
  void abort()  {
    log.info(m_key + " aborted");
    PollManager.removePoll(m_key);
    if(m_voteTime != null) {
      m_voteTime.expire();
    }
    if(m_deadline != null) {
      m_deadline.expire();
    }
  }

  // pre-vote actions

  /**
   * choose a vote time which in the range of now and the end of the
   * election.  This is called after we've performed our hash so we
   * don't have to worry about time to complete the hash.
   */
  void chooseVoteTime() {
    // XXX - this needs to be replaced with something better
    long v_mean = m_msg.getDuration()/2;
    long v_stddev = m_msg.getDuration()/5;

    m_voteTime = new ProbabilisticTimer(v_mean,v_stddev);

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
    rememberVote(msg, true);
    if (!msg.isLocal()) {
      try {
        VerifyPoll.randomRequestVerify(msg,50);
      }
      catch (IOException ex) {
        log.debug("attempt to verify random failed.");
      }
    }
    m_thread.interrupt();

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
    } else {
      log.info("I disagree with" + msg.toString() + " rep " + weight);
    }
    rememberVote(msg, false);
    if (!local) {
      try {
        VerifyPoll.randomRequestVerify(msg, 50);
      }
      catch (IOException ex) {
        log.debug("attempt to verify random failed.");
      }
    }
    m_thread.interrupt();
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

    //recordTally(m_arcUnit, this, yes, no, yesWt, noWt, m_replyOpcode);
  }

  /**
   * cast our vote for this poll
   * @throws IOException if there was no message which triggered this poll.
   */
  void vote() throws IOException {
    LcapMessage msg;
    LcapIdentity local_id = LcapIdentity.getLocalIdentity();
    if(m_msg == null) {
      throw new ProtocolException("no trigger!!!");

    }
    long remainingTime = m_deadline.getRemainingTime();
    msg = LcapMessage.makeReplyMsg(m_msg, m_hash, m_verifier, m_replyOpcode,
                               remainingTime, local_id);
    log.info("vote:" + msg.toString());
    LcapComm.sendMessage(msg,m_arcUnit);
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

  void closeThePoll(CachedUrlSet urlSet, byte[] challenge)  {
    // XXX implement this
  }

  void rememberVote(LcapMessage msg, boolean vote) {
    // XXX implement this

  }


  /**
   * finish the poll once the deadline has expired
   * @throws ProtocolException
   */
  private void endPoll() throws ProtocolException {
    // prevent any further activity on this poll by recording the challenge
    // and dropping any further packets that match it.
    closeThePoll(m_urlSet, m_challenge);

    // if we have a quorum, record the results
    if((m_agree + m_disagree) >= m_quorum) {
      tally();
    }
    else { // we don't have a quorum
      throw new ProtocolException("no quorum: " + m_arcUnit);
    }
  }

  class HashCallback implements HashService.Callback {
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
        if(cookie instanceof Poll) {
          Poll p = (Poll) cookie;
          p.m_hash = out_hash;
          checkVote(out_hash, p.getMessage());
        }
        else if (cookie instanceof VoteChecker) {
          checkVote(out_hash,((VoteChecker)cookie).m_msg);
        }
      }

    }

  }

  static class VoteChecker extends Thread {
    LcapMessage m_msg;
    CachedUrlSet m_urlSet;
    Poll m_poll;
    boolean m_keepGoing;
    MessageDigest m_hasher;
    long m_hashTime;

    VoteChecker(Poll poll, LcapMessage msg, CachedUrlSet urlSet, long hashTime) {
      try {
        m_hasher = MessageDigest.getInstance(PollManager.HASH_ALGORITHM);
        m_poll = poll;
        m_msg = msg;
        m_urlSet = urlSet;
        m_keepGoing = true;
        m_hashTime = hashTime;
        poll.m_voteCheckers.put(this, this);

      } catch (java.security.NoSuchAlgorithmException e) {
        log.debug("SHA-1 ", e);
        m_poll = null;
        m_msg = msg;
        m_urlSet = urlSet;
        m_keepGoing = false;
      }
    }


    public void run() {}


    public synchronized void die() {
      m_keepGoing = false;
      this.interrupt();
    }


    LcapMessage getMessage() {
      return(m_msg);
    }
  }

}