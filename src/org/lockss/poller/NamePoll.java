/*
* $Id: NamePoll.java,v 1.5 2002-11-07 07:40:26 claire Exp $
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
import java.util.Hashtable;
import java.util.Arrays;
import gnu.regexp.*;
import org.lockss.util.ProbabilisticTimer;

/**
 * @author Claire Griffin
 * @version 1.0
 */

public class NamePoll extends Poll {
  Hashtable our_expansion;  // Our expansion of the page set
  Hashtable all_expansion;  // Each vote's expansion
  Hashtable m_voterState;
  int m_namesSent;
  int m_seq;

  public NamePoll(Message msg) {
    super(msg);
    our_expansion = new Hashtable();
    all_expansion = new Hashtable();
    m_voterState = new Hashtable();
    m_namesSent = 0;
    m_replyOpcode = Message.NAME_POLL_REP;
    m_seq++;
    m_thread = new Thread(this, "NamePoll-" + m_seq);
  }

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
 /* we need to extract all of the relevant info */

    thePolls.remove(m_key);
    //recordTally(m_urlset, this, yes, no, yesWt, noWt, m_replyOpcode);
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

    MessageDigest hasher = null;
    try {
      hasher = MessageDigest.getInstance(HASH_ALGORITHM);
    } catch (NoSuchAlgorithmException ex) {
      return false;
    }
    hasher.update(C, 0, C.length);
    hasher.update(V, 0, V.length);

    return HashService.hashNames(urlSet,
                                 hasher,
                                 timer,
                                 new HashCallback(),
                                 this);
 }

  static class NPVoteChecker extends VoteChecker {

    NPVoteChecker(Poll poll, Message msg, CachedUrlSet urlSet, long hashTime) {
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

       // make sure our vote will actually matter
        int vote_margin =  m_poll.m_agree - m_poll.m_disagree;
        if(vote_margin > m_poll.m_quorum)  {
          m_poll.log.info(m_poll.m_key + " " +  vote_margin + " lead is enough");
          return;
        }

        // are we too busy
        if((m_poll.m_counting - 1) > m_poll.m_quorum)  {
          m_poll.log.info(m_poll.m_key + " too busy to count " + m_poll.m_counting + " votes");
          return;
        }

        // do we have time to complete the hash
        int votes = m_poll.m_agree + m_poll.m_disagree + m_poll.m_counting - 1;
        long duration = m_msg.getDuration();
        if (votes > 0 && duration < m_hashTime) {
          m_poll.log.info(m_poll.m_key + " no time to hash vote " + duration + ":" + m_hashTime);
          return;
        }
        m_poll.scheduleHash(m_poll.m_challenge, m_poll.m_verifier, m_urlSet,
                            new ProbabilisticTimer(duration));
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