/*
* $Id: BasePoll.java,v 1.4 2004-07-23 16:44:10 tlipkis Exp $
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

package org.lockss.poller;

import java.io.*;
import java.security.*;
import java.util.*;


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
 * <p>Abstract base class for all poll objects.</p>
 * @author Claire Griffin
 * @version 1.0
 */

public abstract class BasePoll implements Poll, Serializable {

  static final String[] ERROR_STRINGS = {"Poll Complete","Hasher Busy",
      "Hashing Error", "IO Error"
  };

  static final int PS_INITING = 0;
  static final int PS_WAIT_HASH = 1;
  static final int PS_WAIT_VOTE = 2;
  static final int PS_WAIT_TALLY = 3;
  static final int PS_COMPLETE = 4;

  static Logger log=Logger.getLogger("Poll");

  LcapMessage m_msg;      // The message which triggered the poll
  CachedUrlSet m_cus;     // the cached url set from the archival unit
  PollSpec m_pollspec;
  LcapIdentity m_caller;   // who called the poll
  PollManager m_pollmanager; // the pollmanager for this poll.
  IdentityManager idMgr;
  long m_createTime;        // poll creation time

  String m_key;             // the string we used to id this poll
  int m_pollstate;          // one of state constants above
  Deadline m_deadline;      // when election is over

  /**
   * create a new poll from a message
   *
   * @param msg the <code>Message</code> which contains the information
   * @param pollspec the PollSpec on which this poll will operate
   * needed to create this poll.
   * @param pm the pollmanager
   */
  BasePoll(LcapMessage msg, PollSpec pollspec, PollManager pm) {
    m_pollmanager = pm;
    idMgr = pm.getIdentityManager();
    m_caller = idMgr.findIdentity(msg.getOriginAddr());
    m_msg = msg;
    m_pollspec = pollspec;
    m_cus = pollspec.getCachedUrlSet();
    m_createTime = TimeBase.nowMs();
    m_key = msg.getKey();
    m_deadline = Deadline.in(msg.getDuration());
    m_pollstate = PS_INITING;
  }


  /**
   * Returns true if the poll belongs to this Identity
   * @return true if  we called the poll
   */
  public boolean isMyPoll() {
    return idMgr.isLocalIdentity(m_caller);
  }

  /**
   * Return the poll spec used by this poll
   * @return the PollSpec
   */
  public PollSpec getPollSpec() {
    return m_pollspec;
  }

  /** Return the poll version of this poll
   * @return the poll version
   */
  public int getVersion() {
    return m_pollspec.getPollVersion();
  }

  /**
   * get the message used to define this Poll
   * @return <code>Message</code>
   */
  public LcapMessage getMessage() {
    return m_msg;
  }

  /**
   * get the poll identifier key
   * @return the key as a String
   */
  public String getKey() {
    return m_key;
  }

  /**
   * Return the poll's deadline
   * @return the Deadline object for this poll.
   */
  public Deadline getDeadline() {
    return m_deadline;
  }

  /**
   * get the PollTally for this Poll
   * @return VoteTally for this poll
   */
  abstract public PollTally getVoteTally();

  /**
   * Recieve and incoming message from the PollManager
   * @param msg the incoming msg containing a vote for this poll
   */
  abstract void receiveMessage(LcapMessage msg);

  /**
   * start the poll.
   */
  abstract void startPoll();


  /**
   * Is our poll currently in an error state
   * @return true if the poll state is an error value
   */
  abstract boolean isErrorState();

  /**
   * Stop the poll when our deadline expired or our poll has ended in error.
   */
  abstract void stopPoll();

  /**
   * Make a copy of the values in a vote changing only whether we agree or
   * disagree when replaying a vote in a replay poll.
   * @param vote the vote to copy
   * @param agree the Boolean representing the value to set our vote to.
   * @return the newly created Vote
   */
  abstract Vote copyVote(Vote vote, boolean agree);

}
