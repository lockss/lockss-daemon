/*
* $Id: BasePoll.java,v 1.19 2005-12-01 01:54:44 smorabito Exp $
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

import org.lockss.protocol.*;

/**
 * <p>Abstract base class for all poll objects.</p>
 * @author Claire Griffin
 * @version 1.0
 */

public abstract class BasePoll implements Poll {

  protected BasePoll() {
  }

  public boolean isSubpollRunning() {
    return false;
  }

  /**
   * Set the message that triggered this poll.
   *
   * @param msg
   */
  public abstract void setMessage(LcapMessage msg);

  /**
   * Return the time that this poll was created.
   */
  abstract public long getCreateTime();

  /**
   * Return the identity of the caller of this poll.
   *
   * @return PeerIdentity representing the identity of the caller of this poll
   */
  abstract public PeerIdentity getCallerID();

  /**
   * Recieve and incoming message from the PollManager
   * @param msg the incoming msg containing a vote for this poll
   */
  abstract protected void receiveMessage(LcapMessage msg);

  /**
   * Is our poll currently in an error state
   * @return true if the poll state is an error value
   */
  abstract protected boolean isErrorState();

  /**
   * Is this poll currently active?
   *
   * @return True if the poll is active.
   */
  abstract protected boolean isPollActive();

  /**
   * Is this poll currently active?
   *
   * @return True if the poll is complete.
   */
  abstract protected boolean isPollCompleted();

}
