/*
 * $Id: V1PollTally.java,v 1.10 2004-08-09 23:54:04 clairegriffin Exp $
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

import java.security.*;
import java.util.*;

import org.lockss.alert.*;
import org.lockss.daemon.*;
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;

/**
 * V1PollTally is a struct-like class which maintains the current
 * state of votes within a V1Poll.
 */
public class V1PollTally extends PollTally {
  private static final int STATE_SUSPENDED = 10;
  private static final int STATE_TALLIED = 11;

  double voteMargin = 0;    // the margin by which we must win or lose
  double trustedWeight = 0;// the min ave. weight of the winners, when we lose.
  V1Poll poll;

  static Logger log=Logger.getLogger("V1PollTally");

  V1PollTally(int type, long startTime, long duration, int numAgree,
            int numDisagree, int wtAgree, int wtDisagree, int quorum,
            String hashAlgorithm) {
    super(type, startTime, duration, numAgree, numDisagree, wtAgree,
	  wtDisagree, quorum, hashAlgorithm);
    log.debug3("First V1PollTally constructor type " + type + " - " +
		toString());
  }

  V1PollTally(V1Poll owner, int type, long startTime,
		      long duration, int quorum, String hashAlgorithm) {
    this(type, startTime, duration, 0, 0, 0, 0, quorum, hashAlgorithm);
    poll = owner;
    log.debug3("Second V1PollTally constructor type " + type + " - " + toString());
    pollSpec = poll.getPollSpec();
    idManager = poll.idMgr;
    key = poll.getKey();
    trustedWeight = (double)Configuration.getIntParam(V1Poll.PARAM_TRUSTED_WEIGHT,
        V1Poll.DEFAULT_TRUSTED_WEIGHT);
    voteMargin = ((double)Configuration.getIntParam(V1Poll.PARAM_VOTE_MARGIN,
        V1Poll.DEFAULT_VOTE_MARGIN)) / 100;
  }

  public String toString() {
    StringBuffer sbuf = new StringBuffer();
    sbuf.append("[Tally-v1:");
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

  public boolean isErrorState() {
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
      case RESULT_ERROR:
        return getErrString();
      case RESULT_NOQUORUM:
        return "No Quorum";
      case RESULT_UNTRUSTED:
          return "Untrusted Peers";
      case RESULT_TOO_CLOSE:
        return "Too Close";
      case RESULT_WON:
        if(replayDeadline != null) {
          return "Repaired";
        }
        return "Won";
      case RESULT_LOST:
        return "Lost";
      case RESULT_UNVERIFIED:
        return "Unverified";
      case RESULT_VERIFIED:
        return "Verified";
      case RESULT_DISOWNED:
        return "Disowned";
      default:
        return "Active";

    }
  }

  BasePoll getPoll() {
    return poll;
  }

  private boolean isTrustedResults() {

    return (numDisagree == 0 ||
            (wtDisagree/numDisagree >= trustedWeight));
  }

  void tallyVotes() {
    log.debug3("checking for tally of verify poll results.");
    if(type == Poll.VERIFY_POLL) {
      verifyTally();
      return;
    }
    log.debug3("tallying name or content poll results.");
    // if it's an error
    if (isErrorState()) {
      result = RESULT_ERROR;
    }
    else if (!haveQuorum()) {
      result = RESULT_NOQUORUM;
    }
    else if (!isWithinMargin()) {
      result = RESULT_TOO_CLOSE;
    }
    else {
      boolean won = numAgree > numDisagree;
      if (!won && !isTrustedResults()) {
        result = RESULT_UNTRUSTED;
      }
      else {
        result = won ? RESULT_WON : RESULT_LOST;
      }
    }
    log.debug3("V1PollTally.tallyVotes() " + poll.toString());
    log.debug3("agree " + numAgree + " disagree " + numDisagree +
		" status " + result);
    if((type == Poll.NAME_POLL) && (result != RESULT_WON)) {
      log.debug2("lost a name poll, building poll list");
      ((V1NamePoll)poll).buildPollLists(pollVotes.iterator());
      log.debug3("V1PollTally.tallyVotes() 5");
    }
    log.debug3("V1PollTally.tallyVotes() 6");
  }

  void verifyTally() {
    if(isErrorState()) {
      result = RESULT_ERROR;
    }
    else if(poll.isMyPoll()) {
      if (!haveQuorum()) {
        result = RESULT_UNVERIFIED;
      } else if (numAgree > 0 && numDisagree == 0) {
        result = RESULT_VERIFIED;
      } else {
        result = RESULT_DISOWNED;
      }
    }
    else {
      result = RESULT_VERIFIED;
    }
  }

  boolean isLeadEnough() {
    return (numAgree - numDisagree) > quorum;
  }

  boolean haveQuorum() {
    return numAgree + numDisagree >= quorum;
  }

  boolean canResolve() {
    double num_votes = numAgree + numDisagree;

    if(num_votes >= quorum * 2) {
      double act_margin;
      if (numAgree > numDisagree) {
        act_margin = (double) numAgree / num_votes;
      }
      else {
        act_margin = (double) numDisagree / num_votes;
      }
      if (act_margin < voteMargin) {
        return false;
      }
    }
    return true;
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
      String err = "Poll results too close.\nNumber Agrees Votes = " + numAgree +
          " Number Disagree Votes = " + numDisagree +
          " Required vote margin is " +
          req_margin + ". This poll's margin is " + act_margin;
      log.warning(err);
      poll.m_pollmanager.raiseAlert(Alert.auAlert(Alert.INCONCLUSIVE_POLL,
                                                  poll.m_cus.getArchivalUnit()).
                                    setAttribute(Alert.ATTR_TEXT, err));
      return false;
    }
    return true;
  }




  void adjustReputation(LcapIdentity voterID, int repDelta) {
    synchronized (this) {
      Iterator it = pollVotes.iterator();
      while (it.hasNext()) {
        Vote vote = (Vote) it.next();
        if (voterID.isEqual(vote.getIDAddress())) {
          if (vote.isAgreeVote()) {
            wtAgree += repDelta;
          }
          else {
            wtDisagree += repDelta;
          }
          return;
        }
      }
    }
  }

  void addVote(Vote vote, LcapIdentity id, boolean isLocal) {
    int weight = id.getReputation();

    synchronized (this) {
      if(vote.isAgreeVote()) {
        numAgree++;
        wtAgree += weight;
        log.debug("I agree with " + vote + " rep " + weight);
      }
      else {
        numDisagree++;
        wtDisagree += weight;
        if (isLocal) {
          log.error("I disagree with myself about " + vote + " rep " + weight);
        }
        else {
          log.debug("I disagree with " + vote + " rep " + weight);
        }
      }
    }
    synchronized(pollVotes) {
      pollVotes.add(vote);
    }
  }


  /**
   * replay all of the votes in a previously held poll.
   * @param deadline the deadline by which the replay must be complete
   */
  public void startReplay(Deadline deadline) {
    originalVotes = pollVotes;
    pollVotes = new ArrayList(originalVotes.size());
    replayIter =  originalVotes.iterator();
    replayDeadline = deadline;
    numAgree = 0;
    numDisagree = 0;
    wtAgree = 0;
    wtDisagree = 0;
    replayNextVote();
  }

  void replayNextVote() {
    if(replayIter == null) {
      log.warning("Call to replay a poll vote without call to replay all");
    }
    if(poll.isErrorState() || !replayIter.hasNext()) {
      replayIter = null;
      poll.stopPoll();
    }
    else {
      Vote vote = (Vote)replayIter.next();
      replayVoteCheck(vote, replayDeadline);
    }
  }


/**
 * replay a previously checked vote
 * @param vote the vote to recheck
 * @param deadline the deadline by which the check must complete
 */

void replayVoteCheck(Vote vote, Deadline deadline) {
  MessageDigest hasher = poll.getInitedHasher(vote.getChallenge(),
                                              vote.getVerifier());
  Vote newVote;

  if (!poll.scheduleHash(hasher, deadline, poll.copyVote(vote, vote.agree),
                         new ReplayVoteCallback())) {
    poll.m_pollstate = poll.ERR_SCHEDULE_HASH;
    log.debug("couldn't schedule hash - stopping replay poll");
  }
}

  /**
   * True if the poll is active
   * @return true if the poll is active
   */
  public boolean stateIsActive() {
    return (result == RESULT_POLLING);
  }

  /**
   * True if the poll has finshed
   * @return true if the poll has finished
   */
  public boolean stateIsFinished() {
    return (result != STATE_SUSPENDED && result != RESULT_POLLING);
  }


  /**
   * True if the poll is suspended
   * @return true if the poll is suspended
   */
  public boolean stateIsSuspended() {
    return (result == STATE_SUSPENDED);
  }

  /**
   * Set the poll state to suspended
   */
  public void setStateSuspended() {
    result = STATE_SUSPENDED;
  }

class ReplayVoteCallback implements HashService.Callback {
    /**
     * Called to indicate that hashing the content or names of a
     * <code>CachedUrlSet</code> object has succeeded, if <code>e</code>
     * is null,  or has failed otherwise.
     * @param urlset  the <code>CachedUrlSet</code> being hashed.
     * @param cookie  used to disambiguate callbacks.
     * @param hasher  the <code>MessageDigest</code> object that
     *                contains the hash.
     * @param e       the exception that caused the hash to fail.
     */
    public void hashingFinished(CachedUrlSet urlset,
                                Object cookie,
                                MessageDigest hasher,
                                Exception e) {
      boolean hash_completed = e == null ? true : false;

      if (hash_completed) {
        Vote v = (Vote) cookie;
        LcapIdentity id = idManager.findIdentity(v.getIDAddress());
        if (idManager.isLocalIdentity(id)) {
          poll.copyVote(v,true);
        }
        else {
          v.setAgreeWithHash(hasher.digest());
        }
        addVote(v, id, idManager.isLocalIdentity(id));
        replayNextVote();
      }
      else {
        log.warning("replay vote hash failed with exception:" + e.getMessage());
      }
    }
  }


}
