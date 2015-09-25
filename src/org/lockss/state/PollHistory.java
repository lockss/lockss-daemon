/*
 * $Id$
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


package org.lockss.state;

import java.util.*;

/**
 * PollHistory contains the information for a completed poll.  It extends
 * PollState but ignores 'getDeadline()' (returns null).
 */
public class PollHistory extends PollState {
  long duration;
  Collection votes;

  /**
   * Empty constructor used for marshalling.  Needed to create the sub-class
   * PollHistoryBean.
   */
  public PollHistory() {
    super( -1, null, null, -1, 0, null, false);
    duration = 0;
    votes = new ArrayList();
  }

  /**
   * Standard constructor to create a PollHistory.
   * @param type int
   * @param lwrBound String
   * @param uprBound String
   * @param status int
   * @param startTime long
   * @param duration long
   * @param votes Collection
   * @param ourPoll boolean
   */
  PollHistory(int type, String lwrBound, String uprBound, int status,
              long startTime,
              long duration, Collection votes, boolean ourPoll) {
    super(type, lwrBound, uprBound, status, startTime, null, ourPoll);
    this.duration = duration;
    this.votes = votes;
  }

  /**
   * Convenience constructor to create a PollHistory from a PollState.
   * @param state PollState
   * @param duration long
   * @param votes Collection
   */
  PollHistory(PollState state, long duration, Collection votes) {
    this(state.type, state.lwrBound, state.uprBound, state.status,
         state.startTime, duration, votes, state.ourPoll);
  }

  /**
   * Returns the duration the poll took.
   * @return the duration in ms
   */
  public long getDuration() {
    return duration;
  }

  /**
   * Returns an iterator of Votes.
   * @return an Iterator of Vote objects.
   */
  public Iterator getVotes() {
    return (new ArrayList(votes)).iterator();
  }
}
