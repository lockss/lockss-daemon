/*
 * $Id$
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;

/**
 * PollTally is a struct-like class which maintains the current state of
 * votes within a poll.
 */
public abstract class PollTally implements Tallier {
  protected PollSpec pollSpec;
  protected String key;
  protected int type;
  protected long startTime;
  protected long duration;
  protected int numAgree;     // The # of votes that agree with us
  protected int numDisagree;  // The # of votes that disagree with us
  protected int wtAgree;      // The weight of the votes that agree with us
  protected int wtDisagree;   // The weight of the votes that disagree with us
  protected int quorum;       // The # of votes needed to have a quorum
  protected int result;
  protected ArrayList pollVotes;
  protected String hashAlgorithm; // the algorithm used to hash this poll
  protected long createTime;       // poll creation time

  protected List localEntries = null;  // the local entries less the remaining RegExp
  protected List votedEntries = null;  // entries which match the won votes in a poll
  protected Deadline replayDeadline = null;
  protected Iterator replayIter = null;
  protected ArrayList originalVotes = null;
  protected IdentityManager idManager = null;
  protected ActivityRegulator.Lock activityLock;

  static Logger log = Logger.getLogger("PollTally");

  public PollTally(int type, long startTime, long duration, int numAgree,
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
    this.pollVotes = new ArrayList(quorum * 2);
    this.hashAlgorithm = hashAlgorithm;
  }

  public int getType() {
    return type;
  }

  public String getPollKey() {
    return key;
  }

  public boolean isMyPoll() {
    return getPoll().isMyPoll();
  }

  public PollSpec getPollSpec() {
    return pollSpec;
  }

  public CachedUrlSet getCachedUrlSet() {
    return getPoll().getCachedUrlSet();
  }

  public ArchivalUnit getArchivalUnit()  {
    return getCachedUrlSet().getArchivalUnit();
  }

  public long getStartTime() {
    return startTime;
  }

  public long getDuration() {
    return duration;
  }

  public List getPollVotes() {
    return Collections.unmodifiableList(pollVotes);
  }


  public Iterator getCorrectEntries() {
    return votedEntries == null ? CollectionUtil.EMPTY_ITERATOR :
        votedEntries.iterator();
  }

  public Iterator getLocalEntries() {
    return localEntries == null ? CollectionUtil.EMPTY_ITERATOR :
        localEntries.iterator();
  }

  public ActivityRegulator.Lock getActivityLock() {
    return activityLock;
  }

  public void setActivityLock(ActivityRegulator.Lock newLock) {
    activityLock = newLock;
  }

  abstract public int getErr();
  abstract public String getErrString();
  abstract public String getStatusString();

  /**
   * return the poll for which we are acting as a tally
   * @return the Poll.
   */
  abstract public BasePoll getPoll();

  /**
   * tally the votes for this poll
   */
  abstract public void tallyVotes();

  /**
   * True if the poll is active
   * @return true if the poll is active
   */
  abstract public boolean stateIsActive();

  /**
   * True if the poll has finshed
   * @return true if the poll has finished
   */
  abstract public boolean stateIsFinished();

  /**
   * True if the poll is suspended
   * @return true if the poll is suspended
   */
  abstract public boolean stateIsSuspended();

  /**
   * Set the poll state to suspended
   */
  abstract public void setStateSuspended();

  /**
   * Determine if the voter with a given ID has voted
   * @param voterID the PeerIdentity of the voter to check
   * @return true if a vote can be found for this Identity.
   */
  boolean hasVoted(PeerIdentity voterID) {
    Iterator it = pollVotes.iterator();
    while(it.hasNext()) {
      Vote vote = (Vote) it.next();
      if(voterID == vote.getVoterIdentity()) {
        return true;
      }
    }
    return false;
  }

  /**
   * replay all of the votes in a previously held poll.
   * @param deadline the deadline by which the replay must be complete
   */
  void startReplay(Deadline deadline) {
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

  /**
   * replay the next vote in a replay poll
   */
  void replayNextVote() {
    if(replayIter == null) {
      log.warning("Call to replay a poll vote without call to replay all");
    }
    BasePoll poll = getPoll();
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
  public abstract void replayVoteCheck(Vote vote, Deadline deadline);

  /**
   * adjust the reputation of a user after running a verify poll.
   * @param voterID the PeerIdentity of the voter to adjust
   * @param repDelta the amount by which to adjust the reputation.
   */
  public abstract void adjustReputation(PeerIdentity voterID, int repDelta);

  /**
   * Description: a class for the entries returned in a Name poll
   */
  public static class NameListEntry implements LockssSerializable {
    public boolean hasContent;
    public String name;

    public NameListEntry(boolean hasContent, String name) {
      this.hasContent = hasContent;
      this.name = name;
    }

    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("[NameListEntry:");
      sb.append(name);
      sb.append(" - ");
      sb.append(hasContent ? "has content" : "no content");
      sb.append("]");
      return sb.toString();
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



