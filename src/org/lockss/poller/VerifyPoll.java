/*
* $Id: VerifyPoll.java,v 1.9 2002-11-14 03:58:09 claire Exp $
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


import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;


/**
 * @author Claire Griffin
 * @version 1.0
 */
class VerifyPoll extends Poll implements Runnable {
  private static int m_seq = 0;

  public VerifyPoll(LcapMessage msg, CachedUrlSet urlSet) {
    super(msg, urlSet);
    m_replyOpcode = LcapMessage.VERIFY_POLL_REP;
    m_quorum = 1;
    m_seq++;
    m_thread = new Thread(this,"Verify Poll - "	+ m_seq);
  }

  /**
   * run method for a verify poll.  Overrides the run method in
   * Poll.run.
   */
  public void run() {
    Thread.currentThread().setPriority(Thread.NORM_PRIORITY);

    try {
      if (m_caller == null) {   // we called this poll
        while(!m_deadline.expired()) {
          m_deadline.sleepUntil();
        }
        closeThePoll(m_urlSet, m_challenge);
        tally();
      }
      else {			// someone else called this poll
        replyVerify(m_msg);
        PollManager.removePoll(m_key);
      }
    } catch (OutOfMemoryError e) {
      System.exit(-11);
    } catch (IOException e) {
      log.error(m_key + " election failed " + e);
      abort();
    }
  }

  /**
   * will request a verify at random based on some percentage.
   * @param msg the Message to use to request verification
   * @param pct the percentage of time you should verify
   * @throws IOException thrown by ProbbilisticChoice
   */
  protected static void randomRequestVerify(LcapMessage msg, int pct)
      throws IOException {
    double prob = ((double) pct) / 100.0;
    if (ProbabilisticChoice.choose(prob)) {
      requestVerify(msg);
    }
  }

  /**
   * schedule the hash for this poll. Provided for completeness
   * this method always returns true and should never be called.
   * @param C the challenge
   * @param V the verifier
   * @param urlSet the cachedUrlSet
   * @param timer the probabilistic timer
   * @param key the Object which will be returned from the hasher, either the
   * poll or the VoteChecker.
   * @return true if hash successfully completed.
   */
  boolean scheduleHash(byte[] C, byte[] V, CachedUrlSet urlSet,
                       ProbabilisticTimer timer, Object key) {
    return true;
  }


  /**
   * tally the poll results
   */
  protected void tally()  {
    super.tally();
    log.info(m_msg.toString() + " tally " + toString());
    LcapIdentity id = m_msg.getOriginID();
    if ((m_agree + m_disagree) < 1) {
      id.voteNotVerify();
    } else if (m_agree > 0 && m_disagree == 0) {
      id.voteVerify();
    } else {
      id.voteDisown();
    }
  }

  /**
   * prepare to run a poll.  This should check any conditions that might
   * make running a poll unneccessary.  This will also actually perform the
   * hash and check the results.
   * @param msg the message which is triggering the poll
   * @return boolean true if the poll should run, false otherwise
   */
  boolean preparePoll(LcapMessage msg)  {
    if(msg.isLocal())  {
      log.info("Ignoring our verify vote: " + m_key);
      return false;
    }
    byte[] C = msg.getChallenge();
    if(C.length != m_challenge.length)  {
      log.error(m_key + ":challenge length mismatch.");
      return false;
    }
    if(!Arrays.equals(C, m_challenge))  {
      log.error(m_key + ":challenge mismatch");
      return false;
    }
    return true;
  }

  private boolean performHash(LcapMessage msg) {
    byte[] C = msg.getChallenge();
    byte[] H = msg.getHashed();
    MessageDigest hasher;
    // check this vote verification H in the message should
    // hash to the challenge, which is the verifier of the poll
    // thats being verified
    try {
      hasher = MessageDigest.getInstance(PollManager.HASH_ALGORITHM);
    }
    catch (NoSuchAlgorithmException ex) {
      log.error(m_key + "failed to find hash algorithm");
      return false;
    }
    hasher.update(H,0,H.length);
    byte[] HofH = hasher.digest();
    if(!Arrays.equals(C, HofH))  {
      handleDisagreeVote(msg);
    }
    else  {
      handleAgreeVote(msg);
    }
    return true;

  }

  private static void requestVerify(LcapMessage msg) throws IOException {
    String url = new String(msg.getTargetUrl());
    String regexp = new String(msg.getRegExp());
    int opcode = LcapMessage.VERIFY_POLL_REQ;

    byte[] verifier = PollManager.makeVerifier();
    LcapMessage reqmsg = LcapMessage.makeRequestMsg(url,
        regexp,
        new String[0],
        msg.getGroupAddress(),
        msg.getTimeToLive(),
        msg.getVerifier(),
        verifier,
        opcode,
        msg.getDuration(),
        LcapIdentity.getLocalIdentity());
    LcapIdentity originator = msg.getOriginID();
    LcapComm.sendMessageTo(msg,Plugin.findArchivalUnit(url),originator);
    Poll poll = PollManager.findPoll(reqmsg);
    poll.startPoll();
  }

  private void replyVerify(LcapMessage msg) throws IOException  {
    Poll p = null;
    byte[] secret = PollManager.getSecret(msg.getChallenge());
    byte[] verifier = PollManager.makeVerifier();
    LcapMessage repmsg = LcapMessage.makeReplyMsg(msg,
        secret,
        verifier,
        LcapMessage.VERIFY_POLL_REP,
        msg.getDuration(),
        LcapIdentity.getLocalIdentity());

    LcapIdentity originator = msg.getOriginID();
    LcapComm.sendMessageTo(repmsg,Plugin.findArchivalUnit(msg.getTargetUrl()),
                           originator);

  }

  static class VPVoteChecker extends VoteChecker {

    VPVoteChecker(Poll poll, LcapMessage msg, CachedUrlSet urlSet, long hashTime) {
      super(poll, msg, urlSet, hashTime);
    }

    public void run() {
      if(!m_keepGoing) {
        return;
      }
      Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
      try {
        if(!m_poll.preparePoll(m_msg)) {
          return;
        }
        if(!((VerifyPoll)m_poll).performHash(m_msg)) {
          return;
        }
      } catch (Exception e) {
        m_poll.log.error(m_poll.m_key + "vote check fail" + e);
      }
      finally {
        synchronized (m_poll) {
          m_poll.m_voteCheckers.remove(this);
          m_poll.m_counting--;
        }
      }
    }
  }

}