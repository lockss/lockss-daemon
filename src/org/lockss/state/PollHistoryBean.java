/*
 * $Id: PollHistoryBean.java,v 1.4 2002-12-21 01:15:45 aalto Exp $
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


package org.lockss.state;

import java.util.*;
import org.lockss.protocol.LcapIdentity;
import org.lockss.poller.Vote;

/**
 * PollHistoryBean is a settable version of PollHistory used purely for
 * marshalling purposes.
 */
public class PollHistoryBean extends PollHistory {
  public Collection voteBeans;

  public PollHistoryBean() {
    voteBeans = new ArrayList();
  }

  PollHistoryBean(PollHistory history) {
    super(history.type, history.regExp, history.status, history.startTime,
          history.duration, history.votes);
    voteBeans = new ArrayList();
    convertVotesToVoteBeans();
  }

  PollHistory getPollHistory() {
    convertVoteBeansToVotes();
    return new PollHistory(type, regExp, status, startTime, duration, votes);
  }

  /**
   * Sets the type.
   * @param type the new type
   */
  public void setType(int type) {
    super.type = type;
  }

  /**
   * Sets the regular expression.
   * @param regExp the new regexp
   */
  public void setRegExp(String regExp) {
    super.regExp = regExp;
  }

  /**
   * Sets the status.
   * @param status the new status
   */
  public void setStatus(int status) {
    super.status = status;
  }

  /**
   * Sets the start time.
   * @param startTime the new start time
   */
  public void setStartTime(long startTime) {
    super.startTime = startTime;
  }

  /**
   * Sets the duration.
   * @param duration the new duration
   */
  public void setDuration(long duration) {
    super.duration = duration;
  }

  /**
   * Gets the voteBeans collection.
   * @return the voteBeans Collection
   */
  public Collection getVoteBeans() {
    return voteBeans;
  }

  /**
   * Sets the voteBeans collection.
   * @param voteBeans the new Collection
   */
  public void setVoteBeans(Collection voteBeans) {
    this.voteBeans = voteBeans;
  }

  /**
   * Populates the list of votebeans from the votes list.  This is used purely
   * for marshalling purposes, since the Vote objects cannot be marshalled.
   */
  private void convertVotesToVoteBeans() {
    voteBeans = new ArrayList(votes.size());
    Iterator voteIter = votes.iterator();
    while (voteIter.hasNext()) {
      Vote vote = (Vote)voteIter.next();
      voteBeans.add(new VoteBean(vote));
    }
  }

  /**
   * Populates the vote list from the list of VoteBeans.  This is used purely
   * for marshalling purposes, since the unmarshalled VoteBean objects must be
   * converted back into Votes.
   */
  private void convertVoteBeansToVotes() {
    votes = new ArrayList(voteBeans.size());
    Iterator beanIter = voteBeans.iterator();
    while (beanIter.hasNext()) {
      VoteBean bean = (VoteBean)beanIter.next();
      votes.add(bean.getVote());
    }
  }

  public Iterator getVotes() {
    convertVoteBeansToVotes();
    return super.getVotes();
  }
}