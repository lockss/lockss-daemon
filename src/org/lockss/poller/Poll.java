/*
* $Id: Poll.java,v 1.7 2002-11-08 19:14:55 claire Exp $
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

import org.lockss.daemon.*;
import org.lockss.hasher.*;
import org.lockss.protocol.*;
import org.lockss.plugin.*;
import java.util.*;
import org.lockss.util.*;
import java.security.MessageDigest;
import java.io.*;
import java.net.InetAddress;
import gnu.regexp.*;

/**
 * <p>Abstract base class for all poll objects.</p>
 * @author Claire Griffin
 * @version 1.0
 */

public abstract class Poll implements Runnable {
  static final String HASH_ALGORITHM = "SHA-1";
  static final int DEFAULT_QUORUM = 3;
  static final int DEFAULT_DURATION = 6*3600*1000;
  static final int DEFAULT_VOTEDELAY = DEFAULT_DURATION/2;
  static final int DEFAULT_VOTERANGE = DEFAULT_DURATION/4;

  static HashMap thePolls = new HashMap();
  static Logger log=Logger.getLogger("Poll",Logger.LEVEL_DEBUG);

  Message m_msg;          // The message which triggered the poll
  int m_quorum;           // The caller's quorum value
  int m_quorumWt;
  int m_agree;            // The # of votes we've heard that agree with us
  int m_agreeWt;
  int m_disagree;         // The # of votes we've heard that disagree with us
  int m_disagreeWt;

  ArchivalUnit m_arcUnit; // the url as an archival unit
  CachedUrlSet m_urlSet;  // the cached url set retrieved from the archival unit
  String m_url;           // the url for this poll
  String m_regExp;        // the regular expression for the poll

  byte[] m_challenge;     // The caller's challenge string
  byte[] m_secret;        // Our secret - where does this come from???
  byte[] m_verifier;      // Our verifier string - hash of secret
  byte[] m_hash;          // Our hash of challenge, verifier and content(S)

  Deadline m_voteTime;    // when to vote
  ProbabilisticTimer m_deadline;    // when election is over

  Identity m_caller;       // who called the poll
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
   * needed to create this poll.
   */
  Poll(Message msg) {
    m_msg = msg;
    // clear history
    m_agree = 0;
    m_agreeWt = 0;
    m_disagree = 0;
    m_disagreeWt = 0;
    m_quorum = 0;
    m_quorumWt = 0;

    m_createTime = System.currentTimeMillis();

    // now copy the msg elements we need
    m_url = msg.getTargetUrl();
    m_regExp = msg.getRegExp();
    m_arcUnit = Plugin.findArchivalUnit(m_url);
    try {
      m_urlSet = m_arcUnit.makeCachedUrlSet(m_url, m_regExp);
      m_hashTime = m_urlSet.estimatedHashDuration();
    }
    catch (REException ex) {
      log.error("invalid RegExpression: " + m_regExp, ex);
    }
    m_deadline = new ProbabilisticTimer(msg.getDuration());
    m_challenge = msg.getChallenge();

    m_verifier = makeVerifier(this);
    m_caller = msg.getOriginID();
    m_voteChecked = false;
    m_voted = false;
    m_voteCheckers = new HashMap();
    m_key = makeKey(m_url, m_regExp, m_msg.getOpcode());
    if(thePolls.get(m_key) == null) {
      thePolls.put(m_key, this);
    }

  }

  /**
   * make a new poll of the type defined by the incoming message.
   * @param msg <code>Message</code> to use for
   * @return a new Poll object of the required type
   * @throws IOException if message opcode is unknown or if new poll would
   * conflict with currently running poll.
   */
  public static Poll makePoll(Message msg) throws IOException {
    Poll ret_poll;

    ArchivalUnit arcunit = checkForConflicts(msg);
    if(arcunit != null) {
      throw new Message.ProtocolException(msg +
          " conflicts with " + arcunit +
          " in makeElection()");
    }
    switch(msg.getOpcode()) {
      case Message.CONTENT_POLL_REP:
      case Message.CONTENT_POLL_REQ:
        ret_poll = new ContentPoll(msg);
        break;
      case Message.NAME_POLL_REP:
      case Message.NAME_POLL_REQ:
        ret_poll = new NamePoll(msg);
        break;
      case Message.VERIFY_POLL_REP:
      case Message.VERIFY_POLL_REQ:
        ret_poll = new VerifyPoll(msg);
        break;
      default:
        throw new Message.ProtocolException("Unknown opcode:" +
        msg.getOpcode());
    }
    return ret_poll;

  }
  /**
   * make an election by sending a request packet.  This is only
   * called from the tree walk. The poll remains pending in the
   * @param url the String representing the url for this poll
   * @param regexp the String representing the regexp for this poll
   * @param opcode the poll message opcode
   * @param timeToLive the time to live for the new message
   * @param grpAddr the InetAddress used to construct the message
   * @param duration the time this poll has to run
   * @param voteRange the probabilistic vote range
   * @throws IOException thrown if Message construction fails.
   */
  public static void createPoll(String url,
                                String regexp,
                                int opcode,
                                int timeToLive,
                                InetAddress grpAddr,
                                long duration,
                                long voteRange) throws IOException {
    if (voteRange > (duration/4))
      voteRange = duration/4;
    ProbabilisticTimer deadline = new ProbabilisticTimer(duration, voteRange);
    long timeUntilCount = deadline.getRemainingTime();
    byte[] C = makeVerifier(null);
    byte[] V = makeVerifier(null);
    Message msg = Message.makeRequestMsg(url,regexp,null,grpAddr,
        (byte)timeToLive,
        C,V,opcode,timeUntilCount,
        Identity.getLocalIdentity());
    ArchivalUnit conflict = checkForConflicts(msg);
    if (conflict != null)
      throw new Message.ProtocolException(makeKey(url,regexp,opcode) +
          " conflicts with " + conflict +
          " in createElection()");
    log.debug("send" +  msg.toString());
    LcapComm.sendMessage(msg,Plugin.findArchivalUnit(url));
  }

  /**
   * Find the poll defined by the <code>Message</code>.  If the poll
   * does not exist this will create a new poll
   * @param msg <code>Message</code>
   * @return <code>Poll</code> which matches the message opcode.
   * @throws IOException if message opcode is unknown or if new poll would
   * conflict with currently running poll.
   * @see <code>Poll.createPoll</code>
   */
  public static Poll findPoll(Message msg) throws IOException {
    String key = makeKey(msg.getTargetUrl(),msg.getRegExp(),msg.getOpcode());
    Poll ret = (Poll)thePolls.get(key);
    if(ret == null) {
      ret = makePoll(msg);
    }
    return ret;
  }

  /**
   * handle an incoming message packet.  This will create a poll if
   * one is not already running. It will then call recieveMessage on
   * the poll.  This was moved from node state which kept track of the polls
   * running in the node.  This will need to be moved or amended to support this.
   * @param msg the message used to generate the poll
   * @throws IOException thrown if the poll was unsucessfully created
   */
  public static void handleMessage(Message msg) throws IOException {
    Poll p = findPoll(msg);
    if (p == null) {
      log.error("Unable to create poll for Message: " + msg.toString());
      throw new Message.ProtocolException("Failed to create poll for "
          + msg.toString());
    }
    // XXX - we need to notify someone that this poll is running in this node!!!
    p.receiveMessage(msg, p.m_deadline.getRemainingTime());
  }


  /**
   * handle a message which may be a incoming vote
   * @param msg the Message to handle
   * @param hashTime the time available for a hash
   */
  public synchronized void receiveMessage(Message msg, long hashTime) {
    VoteChecker vc = null;
    int opcode = msg.getOpcode();

    switch(opcode) {
      case Message.CONTENT_POLL_REP:
        vc = new ContentPoll.CPVoteChecker(this, msg, m_urlSet, hashTime);
        break;
      case Message.NAME_POLL_REP:
        vc = new NamePoll.NPVoteChecker(this, msg, m_urlSet, hashTime);
        break;
      case Message.VERIFY_POLL_REP:
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
   * get the message used to define this Poll
   * @return <code>Message</code>
   */
  public Message getMessage() {
    return m_msg;
  }

  /**
   * get the Archival Unit used by this poll.
   * @return the <code>ArchivalUnit</code>
   */
  public ArchivalUnit getArchivalUnit() {
    return m_arcUnit;
  }

  /**
   * start the poll thread and run this poll.
   */
  public void startPoll() {
    m_thread.start();
  }

  String okToRun() {
    String ret = null;

    if(m_deadline.expired()) {
      ret = "aborting because deadline expired";
    }
    //else if(!callPermitted(m_msg, m_urlSet)) {
    //	ret = "aborting because call not allowed";
    //}
    else if(m_caller != null) {
      // XXX we need to check for file time for a poll we
      // didn't initiate
      // if(pollIsInternal(m_url)) {
      //   m_caller.callInternalPoll();
      // }
      // if(!checkNodeTime(m_urlSet,m_deadline)) {
      //   ret = "aborting because file time to recent;
      // }
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

      // prevent any further activity on this poll by recording the challenge
      // and dropping any further packets that match it.
      closeThePoll(m_urlSet, m_challenge);
      // record the results
      if((m_agree + m_disagree) >= m_quorum) {
        tally();
      }
      else { // we don't have a quorum
        throw new Message.ProtocolException("no quorum: " + m_arcUnit);
      }
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
  abstract boolean preparePoll(Message msg);

  /**
   * check the hash result obtained by the hasher with one stored in the
   * originating method.
   * @param hashResult byte array containing the result of the hasher
   * @param msg the original Message.
   */
  void checkVote(byte[] hashResult, Message msg)  {
    byte[] H = msg.getHashed();
    if(Arrays.equals(H, hashResult)) {
      handleDisagreeVote(msg);
    }
    else {
      handleAgreeVote(msg);
    }
  }

  /**
   * expire any deadlines and stop this thread
   */
  void abort()  {
    log.info(m_key + " aborted");
    thePolls.remove(m_key);
    if(m_voteTime != null) {
      m_voteTime.expire();

    }
    if(m_deadline != null) {
      m_deadline.expire();

    }
  }

  // pre-vote actions
  /**
   * check for conflicts between the poll defined by the Message and any
   * currently exsiting poll.
   * @param msg the <code>Message</code> to check
   * @return the ArchivalUnit of the conflicting poll.
   */
  static ArchivalUnit checkForConflicts(Message msg) {
    ArchivalUnit  url_set = null;

    String url = msg.getTargetUrl();
    String regexp = msg.getRegExp();
    int    opcode = msg.getOpcode();
    boolean isRegExp = regexp != null;

    // verify polls are never conflicts
    if((opcode == Message.VERIFY_POLL_REP)||
       (opcode == Message.VERIFY_POLL_REQ)) {
      return null;

    }
    Iterator iter = thePolls.values().iterator();
    while(iter.hasNext()) {
      Poll p = (Poll)iter.next();

      Message p_msg = p.getMessage();
      String  p_url = p_msg.getTargetUrl();
      String  p_regexp = p_msg.getRegExp();
      int     p_opcode = p_msg.getOpcode();
      boolean p_isRegExp = p_regexp != null;

      //XXX this should be handled by something else
      boolean alongside = p_url.equals(url);
      boolean below = (p_url.startsWith(url) &&
                       url.endsWith(File.separator));
      boolean above = (url.startsWith(p_url) &&
                       p_url.endsWith(File.separator));

      // check for content poll conflicts
      if((opcode == Message.CONTENT_POLL_REP) ||
         (opcode == Message.CONTENT_POLL_REQ)) {

        if(alongside) { // only verify polls are allowed
          if((p_opcode == Message.VERIFY_POLL_REP)||
             (p_opcode == Message.VERIFY_POLL_REQ)) {
            continue;
          }
          return p.getArchivalUnit();
        }

        if(above || below) { // verify and regexp polls are allowed
          if((p_opcode == Message.VERIFY_POLL_REP)||
             (p_opcode == Message.VERIFY_POLL_REQ)) {
            continue;
          }
          if((p_opcode == Message.CONTENT_POLL_REP) ||
             (p_opcode == Message.CONTENT_POLL_REQ)) {
            if(p_isRegExp) {
              continue;
            }
          }
          return p.getArchivalUnit();
        }
      }

      // check for name poll conflicts
      if((opcode == Message.NAME_POLL_REP) ||
         (opcode == Message.NAME_POLL_REQ)) {
        if(alongside) { // only verify polls are allowed
          if((p_opcode == Message.VERIFY_POLL_REP)||
             (p_opcode == Message.VERIFY_POLL_REQ)) {
            continue;
          }
          else {
            return p.getArchivalUnit();
          }
        }
        if(above) { // only reg exp polls are allowed
          if((p_opcode == Message.CONTENT_POLL_REP) ||
             (p_opcode == Message.CONTENT_POLL_REQ)) {
            if(p_isRegExp) {
              continue;
            }
          }
          return p.getArchivalUnit();
        }

        if(below) { // always a conflict
          return p.getArchivalUnit();
        }
      }
    }

    return url_set;
  }


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
  void handleAgreeVote(Message msg) {
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
  void handleDisagreeVote(Message msg) {
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
   * tally the vote results and clean up.
   */
  void tally() {
    log.info(m_key + " tally: " + m_agree + "/" + m_agreeWt + " for " +
             m_disagree + "/" + m_disagreeWt + " against");
    abort();

  }

  /**
   * cast our vote for this poll
   * @throws IOException if there was no message which triggered this poll.
   */
  void vote() throws IOException {
    Message msg;
    Identity local_id = Identity.getLocalIdentity();
    if(m_msg == null) {
      throw new Message.ProtocolException("no trigger!!!");

    }
    long remainingTime = m_deadline.getRemainingTime();
    msg = Message.makeReplyMsg(m_msg, m_hash, m_verifier, m_replyOpcode,
                               remainingTime, local_id);
    log.info("vote:" + msg.toString());
    LcapComm.sendMessage(msg,m_arcUnit);
  }

  private static Random random = new Random();

  /**
   * make a verifier for the poll.  If the poll is null this
   * will simple return a hash of the verified bytes
   * @param p the Poll object to store the secret in. May be null.
   * @return  a byte[] containing a hash of the secret bytes.
   */
  static byte[] makeVerifier(Poll p) {
    byte[] v_bytes = null;
    byte[] v_secret = new byte[20];
    // generate random bytes
    random.nextBytes(v_secret);
    // hash them

    MessageDigest hasher;
    try {
      hasher = MessageDigest.getInstance(HASH_ALGORITHM);
    } catch (java.security.NoSuchAlgorithmException e) {
      return null;
    }
    hasher.update(v_secret, 0, v_secret.length);
    v_bytes = hasher.digest();

    if(p != null) {
      p.m_secret = v_secret;
    }

    return v_bytes;
  }


  void closeThePoll(CachedUrlSet urlSet, byte[] challenge)  {
    // XXX implement this
  }

  void rememberVote(Message msg, boolean vote) {
    // XXX implement this

  }

  byte[] findSecret(Message msg) {
    byte[] ret = new byte[0];
    // xxx implement this

    return ret;
  }

  static String makeKey(String sUrl, String sRegExp, int opcode) {
    return new String(sUrl + " " + sRegExp	+ " " + opcode);
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
    Message m_msg;
    CachedUrlSet m_urlSet;
    Poll m_poll;
    boolean m_keepGoing;
    MessageDigest m_hasher;
    long m_hashTime;

    VoteChecker(Poll poll, Message msg, CachedUrlSet urlSet, long hashTime) {
      try {
        m_hasher = MessageDigest.getInstance(HASH_ALGORITHM);
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


    Message getMessage() {
      return(m_msg);
    }
  }

}