/*
 * $Id: PollState.java,v 1.17 2003-04-15 01:27:00 aalto Exp $
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

import java.util.Iterator;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.util.Deadline;
import org.lockss.poller.PollSpec;

/**
 * PollState contains the state information for a poll current to a node.
 * There may be more than one active poll per node.
 */
public class PollState implements Comparable {
  public static final int SCHEDULED = 1;
  public static final int RUNNING = 2;
  public static final int REPAIRING = 4;
  public static final int WON = 8;
  public static final int LOST = 16;
  public static final int REPAIRED = 32;
  public static final int UNREPAIRABLE = 64;
  public static final int ERR_SCHEDULE_HASH = 128;
  public static final int ERR_HASHING = 256;
  public static final int ERR_NO_QUORUM = 512;
  public static final int ERR_IO = 1024;
  public static final int ERR_UNDEFINED = 2048;

  int type;
  String lwrBound;
  String uprBound;
  int status;
  long startTime;
  Deadline deadline;
  boolean ourPoll;

  // for marshalling only
  PollState() { }
  PollState(PollStateBean bean) {
    this.type = bean.getType();
    this.lwrBound = bean.getLwrBound();
    this.uprBound = bean.getUprBound();
    this.status = bean.getStatus();
    this.startTime = bean.getStartTime();
    this.deadline = Deadline.restoreDeadlineAt(bean.getDeadlineTime());
    this.ourPoll = bean.getOurPoll();
  }

  PollState(int type, String lwrBound, String uprBound, int status,
            long startTime, Deadline deadline, boolean ourPoll) {
    this.type = type;
    this.lwrBound = lwrBound;
    this.uprBound = uprBound;
    this.status = status;
    this.startTime = startTime;
    this.deadline = deadline;
    this.ourPoll = ourPoll;
  }

  /**
   * Returns the poll type.
   * @return an int representing the type
   * @see org.lockss.poller.Poll
   */
  public int getType() {
    return type;
  }

  /**
   * Returns the poll type as a string
   * @return the String representing the current poll type.
   */
  public String getTypeString() {
    switch (type) {
      case 0:
        return "Name Poll";
      case 1:
        return "Content Poll";
      default:
        return "Undefined";
    }
  }

  /**
   * Returns the lower boundary for a ranged match this poll.
   * @return the lower bound or null
   */
  public String getLwrBound() {
    return lwrBound;
  }

  /**
   * Returns the upper boundary for a ranged match this poll.
   * @return the upper bound or null
   */
  public String getUprBound() {
    return uprBound;
  }

  public String getRangeString() {
    if ((lwrBound!=null) && (lwrBound.equals(PollSpec.SINGLE_NODE_LWRBOUND))) {
      return "single node";
    }
    if(lwrBound != null || uprBound != null) {
      return "[" + lwrBound + " - " + uprBound + "]";
    }
    return "none";
  }

  /**
   * Returns the status of the poll.
   * @return an int representing the current status
   */
  public int getStatus() {
    return status;
  }

  /**
   * Returns true if this poll was started by us
   * @return true if this is our poll
   */
  public boolean getOurPoll() {
    return ourPoll;
  }

  /**
   * return the status of the poll as a string.
   * @return the string representing the current status
   */
  public String getStatusString() {
    switch(status) {
      case SCHEDULED:
        return "Scheduled";
      case RUNNING:
        return "Running";
      case REPAIRING:
        return "Repairing";
      case WON:
        return "Won";
      case LOST:
        return "Lost";
      case REPAIRED:
        return "Repaired";
      case UNREPAIRABLE:
        return "Unrepairable";
      case ERR_SCHEDULE_HASH:
        return "Error scheduling hash";
      case ERR_HASHING:
        return "Error hashing";
      case ERR_NO_QUORUM:
        return "Error no quorum";
      case ERR_IO:
        return "Error I/0";
      case ERR_UNDEFINED:
        return "Undefined error";
      default:
        return "Undefined";
    };
  }

  /**
   * Returns the start time of the poll.
   * @return the start time in ms
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * Returns the deadline for the poll (absolute).
   * @return the deadline in ms
   */
  public Deadline getDeadline() {
    return deadline;
  }

  /**
   * Returns true if the poll is in an active state.
   * @return true if active
   */
  public boolean isActive() {
    return ((status==RUNNING) ||
            (status==REPAIRING) ||
            (status==SCHEDULED));
  }

  /**
   * returns true if the poll ended with an error state
   * @return true if error
   */
  public boolean isErrorState() {
    return ((status==ERR_SCHEDULE_HASH) ||
            (status==ERR_HASHING) ||
            (status==ERR_NO_QUORUM) ||
            (status==ERR_IO) ||
            (status==ERR_UNDEFINED));
  }

  public int compareTo(Object obj) {
    if (obj instanceof PollState) {
      PollState ps2 = (PollState) obj;
      StringBuffer myKey = new StringBuffer();
      myKey.append(type);
      if(lwrBound != null) {
        myKey.append(":");
        myKey.append(lwrBound);
      }
      if(uprBound != null) {
        myKey.append(":");
        myKey.append(uprBound);
      }
      StringBuffer otherKey = new StringBuffer();
      otherKey.append(ps2.getType());
      if(ps2.getLwrBound() != null) {
        otherKey.append(":");
        otherKey.append(ps2.getLwrBound());
      }
      if(ps2.getUprBound() != null) {
        otherKey.append(":");
        otherKey.append(ps2.getUprBound());
      }
      return myKey.toString().compareTo(otherKey.toString());
    }
    else {
      throw new UnsupportedOperationException(
          "Comparing a PollState to a non-PollState object");
    }
  }

  public boolean equals(Object obj) {
    if (obj instanceof PollState) {
      return compareTo(obj) == 0;
    }
    else {
      return false;
    }
  }

}
