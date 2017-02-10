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

package org.lockss.state;

/**
 * Simple class to allow marshalling of PollState instances.  Adds necessary
 * accessors.
 */
public class PollStateBean extends PollState {
  long deadlineTime;

  /**
   * Empty constructor to allow bean creation during unmarshalling.
   */
  public PollStateBean() {
  }

  /**
   * Constructor to create the bean from a PollState prior to marshalling.
   * @param poll the PollState
   */
  public PollStateBean(PollState poll) {
    this.type = poll.getType();
    this.lwrBound = poll.getLwrBound();
    this.uprBound = poll.getUprBound();
    this.status = poll.getStatus();
    this.startTime = poll.getStartTime();
    this.deadlineTime = poll.getDeadline().getExpirationTime();
    this.ourPoll = poll.isOurPoll();
  }

  /**
   * Sets the poll type.
   * @param theType the new type
   * @see org.lockss.protocol.LcapMessage
   */
  public void setType(int theType) {
    type = theType;
  }

  /**
   * Sets the lower boundary for a ranged match this poll.
   * @param theLwrBnd the new bound
   */
  public void setLwrBound(String theLwrBnd) {
    lwrBound = theLwrBnd;
  }

  /**
   * Sets the upper boundary for a ranged match this poll.
   * @param theUprBnd the new bound
   */
  public void setUprBound(String theUprBnd) {
    uprBound = theUprBnd;
  }

  /**
   * Sets the status of the poll.
   * @param theStatus the new status
   */
  public void setStatus(int theStatus) {
    status = theStatus;
  }

  /**
   * Sets the start time of the poll.
   * @param theTime the new startTime
   */
  public void setStartTime(long theTime) {
    startTime = theTime;
  }

  /**
   * Returns the deadline time of the poll, a long.
   * @return a long
   */
  public long getDeadlineTime() {
    return deadlineTime;
  }

  /**
   * Sets the deadline long for the poll (absolute).
   * @param theDeadline a long
   */
  public void setDeadlineTime(long theDeadline) {
    deadlineTime = theDeadline;
  }

  /**
   * Sets the poll owner boolean
   * @param ourPollFlag true iff our poll
   */
  public void setOurPoll(boolean ourPollFlag) {
    ourPoll = ourPollFlag;
  }

  /**
   * Identical to 'isOurPoll()'.  Used for marshalling.
   * @return true iff our poll
   */
  public boolean getOurPoll() {
    return ourPoll;
  }
}
