/*
* $Id: V1ContentPoll.java,v 1.4 2004-09-28 08:53:16 tlipkis Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.protocol.*;
import org.lockss.util.*;
import org.mortbay.util.B64Code;
import org.lockss.plugin.*;

/**
 * class which represents a version one content poll
 * @author Claire Griffin
 * @version 1.0
 */
public class V1ContentPoll extends V1Poll {

  V1ContentPoll(LcapMessage msg, PollSpec pollspec, PollManager pm) {
    super(msg, pollspec, pm);
    m_replyOpcode = LcapMessage.CONTENT_POLL_REP;
    m_tally = new V1PollTally(this,
                              CONTENT_POLL,
                              m_createTime,
                              msg.getDuration(),
                              V1PollFactory.getQuorum(),
                              msg.getHashAlgorithm());
  }


  /**
   * handle a message which may be a incoming vote
   * @param msg the Message to handle
   */
  void receiveMessage(LcapMessage msg) {
    int opcode = msg.getOpcode();

    if(opcode == LcapMessage.CONTENT_POLL_REP) {
      startVoteCheck(msg);
    }
  }


  /**
   * schedule the hash for this poll.
   * @param hasher the MessageDigest used to hash the content
   * @param timer the Deadline by which we must complete
   * @param key the Object which will be returned from the hasher. Always the
   * message which triggered the hash
   * @param callback the hashing callback to use on return
   * @return true if hash successfully completed.
   */
  boolean scheduleHash(MessageDigest hasher, Deadline timer, Object key,
                                HashService.Callback callback) {
    HashService hs = m_pollmanager.getHashService();
    return hs.hashContent( m_cus, hasher, timer, callback, key);
  }


  /**
   * start the hash required for a vote cast in this poll
   * @param msg the LcapMessage containing the vote we're going to check
   */
  void startVoteCheck(LcapMessage msg) {
    super.startVoteCheck();

    if (shouldCheckVote(msg)) {
      Vote vote = new Vote(msg, false);

      MessageDigest hasher = getInitedHasher(msg.getChallenge(),
                                             msg.getVerifier());

      if (!scheduleHash(hasher, m_hashDeadline, vote,
			new VoteHashCallback())) {
        log.info(m_key + " no time to hash vote by " + m_hashDeadline);
        stopVoteCheck();
      }
    }
  }

}
