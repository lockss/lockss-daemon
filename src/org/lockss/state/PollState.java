/*
 * $Id: PollState.java,v 1.14 2003-04-02 23:29:19 tal Exp $
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

  // for marshalling only
  PollState() { }
  PollState(PollStateBean bean) {
    this.type = bean.getType();
    this.lwrBound = bean.getLwrBound();
    this.uprBound = bean.getUprBound();
    this.status = bean.getStatus();
    this.startTime = bean.getStartTime();
    this.deadline = Deadline.restoreDeadlineAt(bean.getDeadlineTime());
  }

  PollState(int type, String lwrBound, String uprBound, int status,
            long startTime,
            Deadline deadline) {
    this.type = type;
    this.lwrBound = lwrBound;
    this.uprBound = uprBound;
    this.status = status;
    this.startTime = startTime;
    this.deadline = deadline;
  }

  /**
   * Returns the poll type.
   * @return an int representing the type
   * @see org.lockss.protocol.LcapMessage
   */
  public int getType() {
    return type;
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

  /**
   * Returns the status of the poll.
   * @return an int representing the current status
   */
  public int getStatus() {
    return status;
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
