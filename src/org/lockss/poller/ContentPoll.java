/*
* $Id: ContentPoll.java,v 1.7 2002-11-08 19:14:55 claire Exp $
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
import java.security.*;
import org.lockss.hasher.*;
import java.io.*;
import gnu.regexp.*;
import java.util.Arrays;
import org.lockss.util.ProbabilisticTimer;

/**
 * class which represents a content poll
 * @author Claire Griffin
 * @version 1.0
 */

public class ContentPoll extends Poll implements Runnable {

  private static int seq = 0;

  ContentPoll(Message msg) throws IOException {
    super(msg);
    m_replyOpcode = Message.CONTENT_POLL_REP;
    seq++;

    m_thread =  new Thread(this, "Content Poll-" + seq);
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
    thePolls.remove(m_key);
    //recordTally(m_arcUnit, this, yes, no, yesWt, noWt, m_replyOpcode);
  }

  /**
   * prepare to run a poll.  This should check any conditions that might
   * make running a poll unneccessary.
   * @param msg the message which is triggering the poll
   * @return boolean true if the poll should run, false otherwise
   */
  boolean preparePoll(Message msg) {
    // Is this is a replay of a local packet?
    if (msg.isLocal()) {
      if (m_voteChecked) {
        log.debug(m_key + "local replay ignored");
        return false;
      }
      m_voteChecked = true;
    }
    // make sure we have the right poll
    byte[] C = msg.getChallenge();

    if(C.length != m_challenge.length)  {
      log.debug(m_key + " challenge length mismatch");
      return false;
    }

    if(!Arrays.equals(C, m_challenge))  {
      log.debug(m_key + " challenge mismatch");
      return false;
    }

    String key = makeKey(msg.getTargetUrl(),
                                msg.getRegExp(),
                                msg.getOpcode());
    if (!m_key.equals(key)) {
      log.debug(m_key + " target mismatch: " + key);
      return false;
    }
    // make sure our vote will actually matter
    int vote_margin =  m_agree - m_disagree;
    if(vote_margin > m_quorum)  {
      log.info(m_key + " " +  vote_margin + " lead is enough");
      return false;
    }

    // are we too busy
    if((m_counting - 1) > m_quorum)  {
      log.info(m_key + " too busy to count " + m_counting + " votes");
      return false;
    }

    // do we have time to complete the hash
    int votes = m_agree + m_disagree + m_counting - 1;
    long duration = msg.getDuration();
    if (votes > 0 && duration < m_hashTime) {
      log.info(m_key + " no time to hash vote " + duration + ":" + m_hashTime);
      return false;
    }
    return true;
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
  boolean scheduleHash(byte[] C, byte[] V, CachedUrlSet urlSet,
                       ProbabilisticTimer timer, Object key) {
    MessageDigest hasher = null;
    CachedUrlSet urlset = null;
    try {
      hasher = MessageDigest.getInstance(HASH_ALGORITHM);
    } catch (NoSuchAlgorithmException ex) {
      return false;
    }
    hasher.update(C, 0, C.length);
    hasher.update(V, 0, V.length);
    return HashService.hashContent( urlSet, hasher, timer,
                                    new HashCallback(), key);
  }

  static class CPVoteChecker extends VoteChecker {

    CPVoteChecker(Poll poll, Message msg, CachedUrlSet urlSet, long hashTime) {
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
        m_poll.scheduleHash(m_poll.m_challenge, m_poll.m_verifier, m_urlSet,
                            new ProbabilisticTimer(m_msg.getDuration()), this);
      } catch (Exception e) {
        m_poll.log.error(m_poll.m_key + "vote check fail", e);
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
