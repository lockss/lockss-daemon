/*
* $Id: VerifyPoll.java,v 1.18 2002-12-03 03:35:59 claire Exp $
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
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;
import org.mortbay.util.B64Code;


/**
 * @author Claire Griffin
 * @version 1.0
 */
class VerifyPoll extends Poll {

  public VerifyPoll(LcapMessage msg, CachedUrlSet urlSet) {
    super(msg, urlSet);
    m_replyOpcode = LcapMessage.VERIFY_POLL_REP;
    m_quorum = 1;
  }



  /**
   * will request a verify at random based on some percentage.
   * @param msg the Message to use to request verification
   * @param prob the percentage of time you should verify
   * @throws IOException thrown by ProbbilisticChoice
   */
  protected static void randomRequestVerify(LcapMessage msg, double prob)
      throws IOException {
     if (ProbabilisticChoice.choose(prob)) {
      requestVerify(msg);
    }
  }

  /**
   * handle a message which may be a incoming vote
   * @param msg the Message to handle
   */
  void receiveMessage(LcapMessage msg) {
    log.debug("receiving verify message" + msg.toString());
    int opcode = msg.getOpcode();
     if(!m_caller.isLocalIdentity()) {
      startVote(msg);
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
                                Object key, HashService.Callback callback) {
    return true;
  }



  /**
   * start the poll.  set a deadline in which to actually verify the message.
   */
  void startPoll() {
    log.debug("Starting new verify poll:" + m_key);

    Deadline pt = Deadline.at(m_deadline.getExpirationTime());

    TimerQueue.schedule(pt, new PollTimerCallback(), this);
  }


  /**
   * finish the poll once the deadline has expired
   */
  void stopPoll() {
    // if we didn't call the poll
    if(!m_caller.isLocalIdentity()) {
      try {
        // send our reply message
        replyVerify(m_msg);
      }
      catch (IOException ex) {
        m_pollstate = ERR_IO;
      }
    }
    super.stopPoll();
  }


  /**
   * tally the poll results
   */
  protected void tally()  {
    super.tally();
    log.info(m_msg.toString() + " tally " + toString());
    LcapIdentity id = m_caller;
    if ((m_agree + m_disagree) < 1) {
      id.voteNotVerify();
    } else if (m_agree > 0 && m_disagree == 0) {
      id.voteVerify();
    } else {
      id.voteDisown();
    }
  }


  private void performHash(LcapMessage msg) {
    int weight = msg.getOriginID().getReputation();
    byte[] challenge = msg.getChallenge();
    byte[] hashed = msg.getHashed();
    MessageDigest hasher = PollManager.getHasher();
    // check this vote verification hashed in the message should
    // hash to the challenge, which is the verifier of the poll
    // thats being verified

    hasher.update(hashed, 0, hashed.length);
    byte[] HofHashed = hasher.digest();
    if(Arrays.equals(challenge, HofHashed))  {
      log.debug("verify vote disagreed");
      m_agree++;
      m_agreeWt += weight;
      //handleAgreeVote(msg);
    }
    else  {
      log.debug("verify vote agreed.");
      m_disagree++;
      m_disagreeWt += weight;
      //handleDisagreeVote(msg);
    }

  }

  private static void requestVerify(LcapMessage msg) throws IOException {
    String url = new String(msg.getTargetUrl());
    String regexp = new String(msg.getRegExp());

    log.debug("Calling a verify poll...");

    LcapMessage reqmsg = LcapMessage.makeRequestMsg(url,
        regexp,
        new String[0],
        msg.getGroupAddress(),
        msg.getTimeToLive(),
        msg.getVerifier(),
        PollManager.makeVerifier(),
        LcapMessage.VERIFY_POLL_REQ,
        msg.getDuration(),
        LcapIdentity.getLocalIdentity());

    LcapIdentity originator = msg.getOriginID();
    log.debug("sending our verification request to " + originator.toString());
    //LcapComm.sendMessage(reqmsg, Plugin.findArchivalUnit(url));
    LcapComm.sendMessageTo(reqmsg, Plugin.findArchivalUnit(url), originator);

    log.debug("Creating a local poll instance...");
    Poll poll = PollManager.findPoll(reqmsg);
    poll.m_pollstate = PS_WAIT_TALLY;
  }

  private void replyVerify(LcapMessage msg) throws IOException  {
    String url = new String(msg.getTargetUrl());
    byte[] secret = PollManager.getSecret(msg.getChallenge());
    if(secret == null) {
      log.error("Verify poll reply failed.  Unable to find secret for: "
                + B64Code.encode(msg.getChallenge()));
      return;
    }
    byte[] verifier = PollManager.makeVerifier();
    LcapMessage repmsg = LcapMessage.makeReplyMsg(msg,
        secret,
        verifier,
        LcapMessage.VERIFY_POLL_REP,
        msg.getDuration(),
        LcapIdentity.getLocalIdentity());

    LcapIdentity originator = msg.getOriginID();
    log.debug("sending our verification reply to " + originator.toString());
    //LcapComm.sendMessage(repmsg, Plugin.findArchivalUnit(url));
    LcapComm.sendMessageTo(repmsg, Plugin.findArchivalUnit(url), originator);

  }

  private void startVote(LcapMessage msg) {
    log.debug("Starting new verify vote:" + m_key);
    super.startVote();
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
        log.debug("Just sent verification.");
        stopVote();
      }
    }
  }

}
