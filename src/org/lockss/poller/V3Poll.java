/*
* $Id: V3Poll.java,v 1.1.2.4 2004-10-29 19:43:06 dshr Exp $
 */

/*

Copyright (c) 2004 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.daemon.*;
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;

import org.mortbay.util.*;  // For B64 encoding stuff?

/**
 * <p>V3 poll - there is only one type of poll in V3; we call it a content
 * poll but it serves the purpose of both content and name polls in V1.
 * However,  in V3 the behavior and results of the peer that called
 * the poll (poller) and the the peers that vote in it are very different.
 * So this generic V3Poll class has two subclasses: V3Poller and V3Voter.</p>
 * @author David Rosenthal
 * @version 1.0
 */

public abstract class V3Poll extends BasePoll {

  protected V3PollTally m_tally;
  protected int m_pendingVotes;
  protected byte[] m_challenge;

  /**
   * create a new poll from a message
   *
   * @param msg the <code>Message</code> which contains the information
   * @param pollspec the PollSpec on which this poll will operate
   * needed to create this poll.
   * @param pm the pollmanager
   */
    V3Poll(PollSpec pollspec, PollManager pm, PeerIdentity orig,
	   byte[] challenge, long duration, String hashAlg) {
	super(pollspec, pm, orig, challengeToKey(challenge), duration);

    // now copy the msg elements we need
    // XXX
    m_pendingVotes = 0;
    m_challenge = challenge;
    getConfigValues();
    m_tally = new V3PollTally(this, CONTENT_POLL, m_createTime,
			      duration, pollspec.getQuorum(), hashAlg);
  }

  // Implementations of abstract methods from BasePoll

  /**
   * get the VoteTally for this Poll
   * @return VoteTally for this poll
   */
  public PollTally getVoteTally() {
    return m_tally;
  }

  /**
   * receive a message that is part of this poll
   */
  abstract void receiveMessage(LcapMessage msg);

  /**
   * start the poll.
   */
  abstract void startPoll();

  /**
   * is our poll currently in an error condition
   * @return true if the poll state is an error value
   */
  boolean isErrorState() {
    return m_pollstate < 0;
  }

  /**
   * finish the poll once the deadline has expired. we update our poll record
   * and prevent any more activity in this poll.
   */
  abstract void stopPoll();

  Vote copyVote(Vote vote, boolean agree) {
    Vote v = new Vote(vote);
    v.agree = agree;
    return v;
  }

  // End abstract methods of BasePoll

  void getConfigValues() {
    /* initialize with our parameters */
    // XXX

  }

  public static String challengeToKey(byte[] challenge) {
    return String.valueOf(B64Code.encode(challenge));
  }

  public abstract int getPollState();
  public abstract String getPollStateName(int state);

  /**
   * Return a hasher preinited with the challenge and verifier
   * @param challenge the challenge bytes
   * @param verifier the verifier bytes
   * @return a MessageDigest
   */
  MessageDigest getInitedHasher(byte[] challenge, byte[] verifier) {
    MessageDigest hasher = m_pollmanager.getHasher(m_msg);
    hasher.update(challenge, 0, challenge.length);
    hasher.update(verifier, 0, verifier.length);
    log.debug3("hashing: C[" +String.valueOf(B64Code.encode(challenge)) + "] "
              +"V[" + String.valueOf(B64Code.encode(verifier)) + "]");
    return hasher;
  }

  public byte[] getChallenge() {
    return m_challenge;
  }

  public byte[] getVerifier() {
    return new byte[0];
  }

}
