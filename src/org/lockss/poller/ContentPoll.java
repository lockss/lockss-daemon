/*
* $Id: ContentPoll.java,v 1.3 2002-10-24 23:18:14 claire Exp $
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


  public void run()  {
    if(m_msg.isLocal())	 {
      if(m_voteChecked)  {
        log.debug(m_key + " local replay ignored");
        return;
      }
      m_voteChecked = true;
    }
    // make sure we have the right poll
    byte[] C = m_msg.getChallenge();

    if(C.length != m_challenge.length)  {
      log.debug(m_key + " challenge length mismatch");
      return;
    }

    if(!Arrays.equals(C, m_challenge))  {
      log.debug(m_key + " challenge mismatch");
      return;
    }

    // make sure our vote will actually matter
    int vote_margin =  m_agree - m_disagree;
    if(vote_margin > m_quorum)  {
      log.info(m_key + " " +  vote_margin + " lead is enough");
      return;
    }

    // are we too busy
    if((m_counting - 1)	> m_quorum)  {
      log.info(m_key + " too busy to count " + m_counting + " votes");
      return;
    }

    // do we have time to complete the hash
    int votes = m_agree + m_disagree + m_counting - 1;
    long duration = m_msg.getDuration();
    if (votes > 0 && duration < m_hashTime) {
      log.info(m_key + " no time to hash vote " + duration + ":" + m_hashTime);
      return;
    }
    scheduleHash();

    try {
      m_thread.sleep(duration);
    }
    catch (InterruptedException ex) {
			// we've completed the hash and need to scheduele our vote
			if(!m_deadline.expired()) {
				chooseVoteTime();
				while(!m_voteTime.expired()) {
          try {
            m_thread.sleep(1000);
          }
          catch (InterruptedException ex2) {
						// this is always an abort.
          }
				}
			}
    }

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
    thePolls.remove(m_key);
    //recordTally(m_arcUnit, this, yes, no, yesWt, noWt, m_replyOpcode);
  }

  void checkVote()  {
    byte[] H = m_msg.getHashed();
    if(Arrays.equals(H, m_hash)) {
      handleDisagreeVote(m_msg);
    }
    else {
      handleAgreeVote(m_msg);
    }
  }

  boolean scheduleHash() {
    MessageDigest hasher = null;
    CachedUrlSet urlset = null;
    try {
      hasher = MessageDigest.getInstance(HASH_ALGORITHM);
    } catch (NoSuchAlgorithmException ex) {
      return false;
    }
    hasher.update(m_challenge, 0, m_challenge.length);
    hasher.update(m_verifier, 0, m_verifier.length);
    return HashService.hashContent( m_urlSet, hasher, m_deadline,
                                    new HashCallback(), m_verifier);
  }


}