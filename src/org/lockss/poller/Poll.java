/*
* $Id: Poll.java,v 1.91 2005-10-11 05:45:39 tlipkis Exp $
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

import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;

public interface Poll {
  // Poll type names
  public static final String[] PollName = {
    "V1 Name", "V1 Content", "V1 Verify",
    "V3 Content", "V3 Header", "V3 Metadata"
  };

  // Poll type enum
  public static final int V1_NAME_POLL = 0;
  public static final int V1_CONTENT_POLL = 1;
  public static final int V1_VERIFY_POLL = 2;
  public static final int V3_POLL = 3;

  /**
   * Error was return by hasher while attempting schedule hash - this
   * usually indicates the hasher is busy but may mean hash time was invalid
   */
  public static final int ERR_SCHEDULE_HASH = -1;
  /**
   * Error was returned by hasher via the callback interface.
   */
  public static final int ERR_HASHING = -2;
  /**
   * Error occured when attempting to send a reply or request packet
   */
  public static final int ERR_IO = -3;
  /**
   * Returns true if the poll belongs to this Identity
   * @return true if  we called the poll
   */
  public boolean isMyPoll();

  /**
   * Return the poll spec used by this poll
   * @return the PollSpec
   */
  public PollSpec getPollSpec();

  /**
   * Return the CachedUrlSet for this poll.
   *
   * @return The CachedUrlSet.
   */
  public CachedUrlSet getCachedUrlSet();

  /* Return the version of the protocol in use
   * @return the protocol version
   */
  public int getVersion();

  /**
   * get the message used to define this Poll
   * @return <code>Message</code>
   */
  public LcapMessage getMessage();

  /**
   * get the poll identifier key
   * @return the key as a String
   */
  public String getKey();

  /**
   * Return the poll's deadline
   * @return the Deadline object for this poll.
   */
  public Deadline getDeadline();

  /**
   * get the PollTally for this Poll
   * @return VoteTally for this poll
   */
  public PollTally getVoteTally();

  /** Kludge for V1Name subpolls */
  public boolean isSubpollRunning();
}
