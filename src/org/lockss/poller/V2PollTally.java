/*
 * $Id: V2PollTally.java,v 1.5 2004-09-20 14:20:37 dshr Exp $
 */

/*

Copyright (c) 2003 Board of Trustees of Leland Stanford Jr. University,
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

import org.mortbay.util.B64Code;
import org.lockss.daemon.*;
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;
import org.lockss.state.PollHistory;
import org.lockss.state.NodeManager;
import org.lockss.daemon.status.*;

/**
 * V2PollTally is a struct-like class which maintains the current
 * state of votes within a V2Poll.
 * @author David Rosenthal
 * @version 1.0
 */
public class V2PollTally extends PollTally {
  // XXX Change these for V2
  private static final int STATE_POLLING = 0;
  private static final int STATE_ERROR = 1;
  private static final int STATE_NOQUORUM = 2;
  private static final int STATE_RESULTS_TOO_CLOSE = 3;
  private static final int STATE_RESULTS_UNTRUSTED = 4;
  private static final int STATE_WON = 5;
  private static final int STATE_LOST = 6;
  private static final int STATE_UNVERIFIED = 7;
  private static final int STATE_VERIFIED = 8;
  private static final int STATE_DISOWNED = 9;
  private static final int STATE_SUSPENDED = 10;

  V2Poll poll;
  double voteMargin; // XXX
  double trustedWeight; // XXX

  static Logger log=Logger.getLogger("V2PollTally");

  V2PollTally(int type, long startTime, long duration, int numAgree,
            int numDisagree, int wtAgree, int wtDisagree, int quorum,
            String hashAlgorithm) {
    super(type, startTime, duration, numAgree, numDisagree, wtAgree,
	  wtDisagree, quorum, hashAlgorithm);
    log.debug3("First V2PollTally constructor type " + type + " - " +
		toString());
  }

  V2PollTally(V2Poll owner, int type, long startTime,
		      long duration, int quorum, String hashAlgorithm) {
    this(type, startTime, duration, 0, 0, 0, 0, quorum, hashAlgorithm);
    poll = owner;
    log.debug3("Second V2PollTally constructor type " + type + " - " + toString());
    pollSpec = poll.getPollSpec();
    idManager = poll.idMgr;
    key = poll.getKey();
  }

  public String toString() {
    StringBuffer sbuf = new StringBuffer();
    sbuf.append("[Tally-v2:");
    sbuf.append(" type:" + type);
    sbuf.append("-(" + key);
    sbuf.append(") agree:" + numAgree);
    sbuf.append("-wt-" + wtAgree);
    sbuf.append(" disagree:" + numDisagree);
    sbuf.append("-wt-" + wtDisagree);
    sbuf.append(" quorum:" + quorum);
    sbuf.append(" status:" + getStatusString());
    sbuf.append("]");
    return sbuf.toString();
  }

  boolean isErrorState() {
    return poll.m_pollstate < 0;
  }

  public int getTallyResult() {
    return result;
  }

  /**
   * get the error state for this poll
   * @return 0 == NOERR or one of the poll err conditions
   */
  public int getErr() {
    if(isErrorState()) {
      return poll.m_pollstate;
    }
    return 0;
  }

  public String getErrString() {
    switch(poll.m_pollstate) {
      case BasePoll.ERR_SCHEDULE_HASH:
        return "Hasher Busy";
      case BasePoll.ERR_HASHING:
        return "Error hashing";
      case BasePoll.ERR_IO:
        return "Error I/0";
      default:
        return "Undefined";
    }
  }

  public String getStatusString() {
    switch (result) {
      default:
        return "No quorum"; // XXX

    }
  }

  BasePoll getPoll() {
    return poll;
  }

  void tallyVotes() {
    result = STATE_NOQUORUM; // XXX
  }

  void verifyTally() {
    throw new UnsupportedOperationException();
  }

  boolean isLeadEnough() {
    return (numAgree - numDisagree) > quorum;
  }

  boolean haveQuorum() {
    return numAgree + numDisagree >= quorum;
  }

  void setVoteMargin(double margin) {
    voteMargin = margin;
  }


  boolean isWithinMargin() {
    double num_votes = numAgree + numDisagree;
    double req_margin = voteMargin;
    double act_margin;

    if (numAgree > numDisagree) {
      act_margin = (double) numAgree / num_votes;
    }
    else {
      act_margin = (double) numDisagree / num_votes;
    }
    if (act_margin < req_margin) {
      log.warning("Poll results too close.  Required vote margin is " +
                req_margin + ". This poll's margin is " + act_margin);
      return false;
    }
    return true;
  }

  private boolean isTrustedResults() {

    return (numDisagree == 0 ||
	    (wtDisagree/numDisagree >= trustedWeight));
  }


  void adjustReputation(PeerIdentity voterID, int repDelta) {
    // XXX should not need this
    throw new UnsupportedOperationException();
  }

  void addVote(Vote vote, PeerIdentity id, boolean isLocal) {
    throw new UnsupportedOperationException();
  }


  /**
   * replay all of the votes in a previously held poll.
   * @param deadline the deadline by which the replay must be complete
   */
  public void startReplay(Deadline deadline) {
    throw new UnsupportedOperationException();
  }

  /**
   * replay a previously checked vote
   * @param vote the vote to recheck
   * @param deadline the deadline by which the check must complete
   */

  void replayVoteCheck(Vote vote, Deadline deadline) {
    throw new UnsupportedOperationException();
  }

  /**
   * True if the poll is active
   * @return true if the poll is active
   */
  boolean stateIsActive() {
    return (result == STATE_POLLING);
  }

  /**
   * True if the poll has finshed
   * @return true if the poll has finished
   */
  boolean stateIsFinished() {
    return (false);
  }


  /**
   * True if the poll is suspended
   * @return true if the poll is suspended
   */
  boolean stateIsSuspended() {
    return (result == STATE_SUSPENDED);
  }

  /**
   * Set the poll state to suspended
   */
  void setStateSuspended() {
    result = STATE_SUSPENDED;
  }

}
