/*
 * $Id: PollTally.java,v 1.15 2003-06-17 01:09:30 aalto Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

import gnu.regexp.*;
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
 * PollTally is a struct-like class which maintains the current state of
 * votes within a poll.
 */
public class PollTally {
  public static final int STATE_POLLING = 0;
  public static final int STATE_ERROR = 1;
  public static final int STATE_NOQUORUM = 2;
  public static final int STATE_RESULTS_TOO_CLOSE = 3;
  public static final int STATE_RESULTS_UNTRUSTED = 4;
  public static final int STATE_WON = 5;
  public static final int STATE_LOST = 6;
  public static final int STATE_UNVERIFIED = 7;
  public static final int STATE_VERIFIED = 8;
  public static final int STATE_DISOWNED = 9;
  public static final int STATE_SUSPENDED = 10;

  PollSpec pollSpec;
  String key;
  Poll poll;
  int type;
  long startTime;
  long duration;
  int numAgree;     // The # of votes that agree with us
  int numDisagree;  // The # of votes that disagree with us
  int wtAgree;      // The weight of the votes that agree with us
  int wtDisagree;   // The weight of the votes that disagree with us
  int quorum;       // The # of votes needed to have a quorum
  int status;
  ArrayList pollVotes;
  String hashAlgorithm; // the algorithm used to hash this poll
  long m_createTime;       // poll creation time

  List localEntries = null;  // the local entries less the remaining RegExp
  List votedEntries = null;  // entries which match the won votes in a poll
  private Deadline replayDeadline = null;
  private Iterator replayIter = null;
  private ArrayList originalVotes = null;
  private static IdentityManager idManager = null;

  static Logger log=Logger.getLogger("PollTally");

  PollTally(int type, long startTime, long duration, int numAgree,
            int numDisagree, int wtAgree, int wtDisagree, int quorum,
            String hashAlgorithm) {
    this.type = type;
    this.startTime = startTime;
    this.duration = duration;
    this.numAgree = numAgree;
    this.numDisagree = numDisagree;
    this.wtAgree = wtAgree;
    this.wtDisagree = wtDisagree;
    this.quorum = quorum;
    pollVotes = new ArrayList(quorum * 2);
    this.hashAlgorithm = hashAlgorithm;
  }

  PollTally(Poll owner, int type, long startTime, long duration, int quorum,
            String hashAlgorithm) {
    this(type, startTime, duration, 0, 0, 0, 0, quorum, hashAlgorithm);
    poll = owner;
    pollSpec = poll.getPollSpec();
    idManager = poll.idMgr;
    key = poll.getKey();
  }

  public String toString() {
    StringBuffer sbuf = new StringBuffer();
    sbuf.append("[Tally:");
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


  /**
   * return the unique key for the poll for this tally
   * @return a String representing the key
   */
  public String getPollKey() {
    return key;
  }

  /**
   * Returns true if the poll belongs to this Identity
   * @return true if this Identity
   */
  public boolean isMyPoll() {
    return poll.isMyPoll();
  }

  /**
   * Return the poll spec used by this poll
   * @return the PollSpec
   */
  public PollSpec getPollSpec() {
    return pollSpec;
  }

  public CachedUrlSet getCachedUrlSet() {
    return poll.m_cus;
  }

  public ArchivalUnit getArchivalUnit()  {
    return getCachedUrlSet().getArchivalUnit();
  }

  /**
   * Returns poll type constant - one of Poll.NamePoll, Poll.ContentPoll,
   * Poll.VerifyPoll
   * @return integer constant for this poll
   */
  public int getType() {
    return type;
  }

  /**
   * returns the poll start time
   * @return start time as a long
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * returns the poll duration
   * @return the duration as a long
   */
  public long getDuration() {
    return duration;
  }

  /**
   * return the votes cast in this poll
   * @return the list of votes
   */

  public List getPollVotes() {
    return Collections.unmodifiableList(pollVotes);
  }

  /**
   * return an interator for the set of entries tallied during the vote
   * @return the completed list of entries
   */
  public Iterator getCorrectEntries() {
    return votedEntries == null ? CollectionUtil.EMPTY_ITERATOR :
        votedEntries.iterator();
  }

  /**
   * return an interator for the set of entries we have locally
   * @return the list of entries
   */
  public Iterator getLocalEntries() {
    return localEntries == null ? CollectionUtil.EMPTY_ITERATOR :
        localEntries.iterator();
  }


  public boolean isErrorState() {
    return poll.m_pollstate < 0;
  }

  public boolean isInconclusiveState() {
    switch(status) {
      case STATE_NOQUORUM:
      case STATE_RESULTS_UNTRUSTED:
      case STATE_RESULTS_TOO_CLOSE:
        return true;
      default:
        return false;
    }
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
      case Poll.ERR_SCHEDULE_HASH:
        return "Hasher Busy";
      case Poll.ERR_HASHING:
        return "Error hashing";
      case Poll.ERR_IO:
        return "Error I/0";
      default:
        return "Undefined";
    }
  }

  public int getStatus() {
    return status;
  }

  public String getStatusString() {
    switch (status) {
      case STATE_ERROR:
        return getErrString();
      case STATE_NOQUORUM:
        return "No Quorum";
      case STATE_RESULTS_UNTRUSTED:
          return "Untrusted Peers";
      case STATE_RESULTS_TOO_CLOSE:
        return "Too Close";
      case STATE_WON:
        if(replayDeadline != null) {
          return "Repaired";
        }
        return "Won";
      case STATE_LOST:
        return "Lost";
      case STATE_UNVERIFIED:
        return "Unverified";
      case STATE_VERIFIED:
        return "Verified";
      case STATE_DISOWNED:
        return "Disowned";
      default:
        return "Active";

    }
  }

  void tallyVotes() {
    if(type == Poll.VERIFY_POLL) {
      verifyTally();
      return;
    }
    // if it's an error
    if (isErrorState()) {
      status = STATE_ERROR;
    }
    else if (!haveQuorum()) {
      status = STATE_NOQUORUM;
    }
    else if (!isWithinMargin()) {
      status = STATE_RESULTS_TOO_CLOSE;
    }
    else {
      boolean won = numAgree > numDisagree;
      if (!won && !isTrustedResults()) {
        status = STATE_RESULTS_UNTRUSTED;
      }
      else {
        status = won ? STATE_WON : STATE_LOST;
      }
    }
  }

  void verifyTally() {
    if(isErrorState()) {
      status = STATE_ERROR;
    }
    else if(poll.isMyPoll()) {
      if (!haveQuorum()) {
        status = STATE_UNVERIFIED;
      } else if (numAgree > 0 && numDisagree == 0) {
        status = STATE_VERIFIED;
      } else {
        status = STATE_DISOWNED;
      }
    }
    else {
      status = STATE_VERIFIED;
    }
  }

  boolean isLeadEnough() {
    return (numAgree - numDisagree) > quorum;
  }

  boolean haveQuorum() {
    return numAgree + numDisagree >= quorum;
  }

  boolean isWithinMargin() {
    double num_votes = numAgree + numDisagree;
    double req_margin = poll.getMargin();
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

  public boolean isTrustedResults() {

    return wtDisagree/numDisagree >= poll.m_trustedWeight;
  }


  boolean hasVoted(LcapIdentity voterID) {
    Iterator it = pollVotes.iterator();
    while(it.hasNext()) {
      Vote vote = (Vote) it.next();
      if(voterID.isEqual(vote.getIDAddress())) {
        return true;
      }
    }
    return false;
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

  public static class NameListEntry {
    public boolean hasContent;
    public String name;

    public NameListEntry(boolean hasContent, String name) {
      this.hasContent = hasContent;
      this.name = name;
    }

    /**
     * Overrides Object.equals().
     * Returns true if the obj is the same object and the names are the same
     * @param obj the Object to compare
     * @return the hashcode
     */
    public boolean equals(Object obj) {
      if (obj instanceof NameListEntry) {
        NameListEntry entry = (NameListEntry) obj;
        return name.equals(entry.name);
      }
      return false;
    }

    /**
     * Overrides Object.hashCode().
     * Returns the hash of the strings
     * @return the hashcode
     */
    public int hashCode() {
      return name.hashCode();
    }
  }

}


