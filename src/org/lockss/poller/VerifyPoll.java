/*
* $Id: VerifyPoll.java,v 1.5 2002-11-07 07:40:26 claire Exp $
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

import org.lockss.daemon.CachedUrlSet;
import org.lockss.protocol.Message;
import java.io.IOException;
import org.lockss.util.ProbabilisticChoice;
import org.lockss.protocol.*;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.lockss.plugin.Plugin;
import org.lockss.util.ProbabilisticTimer;


/**
 * @author Claire Griffin
 * @version 1.0
 */
class VerifyPoll extends Poll implements Runnable {
  private static int m_seq = 0;

  public VerifyPoll(Message msg) {
    super(msg);
    m_replyOpcode = Message.VERIFY_POLL_REP;
    m_quorum = 1;
    m_seq++;
    m_thread = new Thread(this,"Verify Poll - "	+ m_seq);
  }

  /**
   * schedule the hash for this poll.
   * @param C the challenge
   * @param V the verifier
   * @param urlSet the cachedUrlSet
   * @param timer the probabilistic timer
   * @return true if hash successfully completed.
   */
  boolean scheduleHash(byte[] C, byte[] V, CachedUrlSet urlSet,
                       ProbabilisticTimer timer) {
    return true;
  }

  protected static void randomRequestVerify(Message msg, int pct)
      throws IOException {
    double prob = ((double) pct) / 100.0;
    if (ProbabilisticChoice.choose(prob))
      requestVerify(msg);
  }

  private static void requestVerify(Message msg) throws IOException {
    String url = new String(msg.getTargetUrl());
    String regexp = new String(msg.getRegExp());
    int opcode = Message.VERIFY_POLL_REQ;
    Poll p = null;
    byte[] verifier = makeVerifier(p);
    Message reqmsg = Message.makeRequestMsg(url,
        regexp,
        new String[0],
        msg.getGroupAddress(),
        msg.getTimeToLive(),
        msg.getVerifier(),
        verifier,
        opcode,
        msg.getDuration(),
        Identity.getLocalIdentity());
    Identity originator = msg.getOriginID();
    LcapComm.sendMessageTo(msg,Plugin.findArchivalUnit(url),originator);
    Poll poll = findPoll(reqmsg);
    poll.startPoll();
  }

  private void replyVerify(Message msg) throws IOException  {
    Poll p = null;
    byte[] secret = findSecret(msg);
    byte[] verifier = makeVerifier(p);
    Message repmsg = Message.makeReplyMsg(msg,
        secret,
        verifier,
        Message.VERIFY_POLL_REP,
        msg.getDuration(),
        Identity.getLocalIdentity());

    Identity originator = msg.getOriginID();
    LcapComm.sendMessageTo(repmsg,Plugin.findArchivalUnit(msg.getTargetUrl()),
                           originator);

  }

  protected void tally()  {
    int yes;
    int no;

    synchronized(this)  {
      yes = m_agree;
      no = m_disagree;
    }
    thePolls.remove(m_key);
    log.info(m_msg.toString() + " tally " + toString());
    Identity id = m_msg.getOriginID();
    if ((m_agree + m_disagree) < 1) {
      id.voteNotVerify();
    } else if (m_agree > 0 && m_disagree == 0) {
      id.voteVerify();
    } else {
      id.voteDisown();
    }
    //recordVote(m_urlset, Message.VERIFY_REQ, yes, no, agreeWt, disagreeWt);
  }

  void checkVote(byte[] hashResult, Message msg)  {
    byte[] H = msg.getHashed();
    if(Arrays.equals(H, hashResult)) {
      handleDisagreeVote(msg);
    }
    else {
      handleAgreeVote(msg);
    }
  }

  void checkVote()  {
    if(m_msg.isLocal())  {
      log.info("Ignoring our verify vote: " + m_key);
      return;
    }
    byte[] C = m_msg.getChallenge();
    if(C.length != m_challenge.length)  {
      log.error(m_key + ":challenge length mismatch.");
      return;
    }
    if(!Arrays.equals(C, m_msg.getChallenge()))  {
      log.error(m_key + ":challenge mismatch");
      return;
    }
    // check this vote verification H in the message should
    // hash to the challenge, which is the verifier of the poll
    // thats being verified
    byte[] H = m_msg.getHashed();
    MessageDigest hasher;
    try {
      hasher = MessageDigest.getInstance(HASH_ALGORITHM);
    }
    catch (NoSuchAlgorithmException ex) {
      log.error(m_key + "failed to find hash algorithm");
      return;
    }
    hasher.update(H,0,H.length);
    byte[] HofH = hasher.digest();
    if(!Arrays.equals(C, HofH))  {
      handleDisagreeVote(m_msg);
    }
    else  {
      handleAgreeVote(m_msg);
    }
  }

  public void run() {
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
        thePolls.remove(m_key);
      }
    } catch (OutOfMemoryError e) {
      System.exit(-11);
    } catch (IOException e) {
      log.error(m_key + " election failed " + e);
      abort();
    }
  }

  static class VPVoteChecker extends VoteChecker {

    VPVoteChecker(Poll poll, Message msg, CachedUrlSet urlSet, long hashTime) {
      super(poll, msg, urlSet, hashTime);
    }

    public void run() {
      if(!m_keepGoing) {
        return;
      }
      Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
      try {
        // Is this is a replay of a local packet?
        if (m_msg.isLocal()) {
          if (m_poll.m_voteChecked) {
            m_poll.log.debug(m_poll.m_key + "local replay ignored");
            return;
          }
          m_poll.m_voteChecked = true;
        }
        // make sure we have the right poll
        byte[] C = m_msg.getChallenge();

        if(C.length != m_poll.m_challenge.length)  {
          m_poll.log.debug(m_poll.m_key + " challenge length mismatch");
          return;
        }

        if(!Arrays.equals(C, m_poll.m_challenge))  {
          m_poll.log.debug(m_poll.m_key + " challenge mismatch");
          return;
        }

        //  Check this vote verification - the H in the packet should hash
        //  to the challenge,  which is the verifier of the vote that's
        //  being verified.
        MessageDigest hasher;
        try {
          hasher = MessageDigest.getInstance(HASH_ALGORITHM);
        }
        catch (NoSuchAlgorithmException ex) {
          log.error(m_poll.m_key + "failed to find hash algorithm");
          return;
        }

        byte [] H = m_msg.getHashed();
        hasher.update(H, 0, H.length);
        byte [] HofH = hasher.digest();
        if(!Arrays.equals(C, HofH))  {
          m_poll.handleDisagreeVote(m_msg);
        }
        else  {
          m_poll.handleAgreeVote(m_msg);
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