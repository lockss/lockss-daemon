/*
* $Id: VerifyPoll.java,v 1.25 2003-01-09 01:55:38 aalto Exp $
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

  public VerifyPoll(LcapMessage msg, CachedUrlSet urlSet, PollManager pm) {
    super(msg, urlSet, pm);
    m_replyOpcode = LcapMessage.VERIFY_POLL_REP;
    m_tally.quorum = 1;
    m_tally.type = VERIFY_POLL;
  }


  /**
   * handle a message which may be a incoming vote
   * @param msg the Message to handle
   */
  void receiveMessage(LcapMessage msg) {
    log.debug("receiving verify message" + msg.toString());
    int opcode = msg.getOpcode();
    if(opcode == LcapMessage.VERIFY_POLL_REP) {
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
		       Serializable key, HashService.Callback callback) {
    return true;
  }



  /**
   * start the poll.  set a deadline in which to actually verify the message.
   */
  void startPoll() {
    log.debug("Starting new verify poll:" + m_key);
    Deadline pt;

    if(m_msg.isLocal()) {
      pt = Deadline.in(m_deadline.getRemainingTime());
    }
    else {
      pt = Deadline.atRandomBefore(m_deadline);
    }
    TimerQueue.schedule(pt, new PollTimerCallback(), this);
  }


  /**
   * finish the poll once the deadline has expired
   */
  void stopPoll() {
    // if we didn't call the poll
    if(m_pollstate != PS_WAIT_TALLY) {
      try {
        // send our reply message
        replyVerify(m_msg);
      }
      catch (IOException ex) {
        m_pollstate = ERR_IO;
      }
    }
    // this will call tally
    super.stopPoll();
  }


  /**
   * tally the poll results
   */
  protected void tally()  {
    super.tally();
    log.info(m_msg.toString() + " tally " + toString());
    LcapIdentity id = m_caller;

    if ((m_tally.numAgree + m_tally.numDisagree) < 1) {
      log.debug("vote failed to verify");
      idMgr.changeReputation(id, IdentityManager.VOTE_NOTVERIFIED);
    } else if (m_tally.numAgree > 0 && m_tally.numDisagree == 0) {
      log.debug("vote sucessfully verified");
      idMgr.changeReputation(id, IdentityManager.VOTE_VERIFIED);
    } else {
      log.debug("vote disowned.");
      idMgr.changeReputation(id, IdentityManager.VOTE_DISOWNED);
    }
  }


  private void performHash(LcapMessage msg) {
    int weight = msg.getOriginID().getReputation();
    byte[] challenge = msg.getChallenge();
    byte[] hashed = msg.getHashed();
    MessageDigest hasher = m_pollmanager.getHasher(msg);
    // check this vote verification hashed in the message should
    // hash to the challenge, which is the verifier of the poll
    // thats being verified

    hasher.update(hashed, 0, hashed.length);
    byte[] HofHashed = hasher.digest();
    boolean agree = Arrays.equals(challenge, HofHashed);
    m_tally.addVote(new Vote(msg, agree));
  }


  private void replyVerify(LcapMessage msg) throws IOException  {
    String url = new String(msg.getTargetUrl());
    byte[] secret = m_pollmanager.getSecret(msg.getChallenge());
    if(secret == null) {
      log.error("Verify poll reply failed.  Unable to find secret for: "
                + B64Code.encode(msg.getChallenge()));
      return;
    }
    byte[] verifier = m_pollmanager.makeVerifier();
    LcapMessage repmsg = LcapMessage.makeReplyMsg(msg,
        secret,
        verifier,
        LcapMessage.VERIFY_POLL_REP,
        msg.getDuration(),
        idMgr.getLocalIdentity());

    LcapIdentity originator = msg.getOriginID();
    log.debug("sending our verification reply to " + originator.toString());
    m_pollmanager.sendMessageTo(repmsg, Plugin.findArchivalUnit(url), originator);
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
        stopVote();
      }
    }
  }

}
