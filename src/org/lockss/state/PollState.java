/*
 * $Id: PollState.java,v 1.5 2002-12-14 00:14:09 aalto Exp $
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
import org.lockss.daemon.CachedUrlSet;
import org.lockss.util.Deadline;

/**
 * PollState contains the state information for a poll current to a node.
 * There may be more than one active poll per node.
 */
public class PollState {
  public static final int SCHEDULED = 1;
  public static final int RUNNING = 2;
  public static final int REPAIRING = 4;
  public static final int WON = 8;
  public static final int REPAIRED = 16;
  public static final int UNREPAIRABLE = 32;

  int type;
  String regExp;
  int status;
  long startTime;
  Deadline deadline;

  PollState(int type, String regExp, int status, long startTime,
            Deadline deadline) {
    this.type = type;
    this.regExp = regExp;
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
   * Returns the regular expression for this poll.
   * @return the regexp or null
   */
  public String getRegExp() {
    return regExp;
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

}