/*
* $Id: Poll.java,v 1.81 2003-06-20 22:34:51 claire Exp $
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


import gnu.regexp.*;
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

public abstract class Poll implements Serializable {
  public static final int NAME_POLL = 0;
  public static final int CONTENT_POLL = 1;
  public static final int VERIFY_POLL = 2;

  public static final String[] PollName = {"Name", "Content", "Verify"};

  static final String PARAM_AGREE_VERIFY = Configuration.PREFIX +
      "poll.agreeVerify";
  static final String PARAM_DISAGREE_VERIFY = Configuration.PREFIX +
      "poll.disagreeVerify";
  static final String PARAM_VOTE_MARGIN = Configuration.PREFIX +
      "poll.voteMargin";
  static final String PARAM_TRUSTED_WEIGHT = Configuration.PREFIX +
      "poll.trustedWeight";

  static final int DEFAULT_VOTE_MARGIN = 75;
  static final int DEFAULT_TRUSTED_WEIGHT = 350;
  static final int DEFAULT_AGREE_VERIFY = 10;
  static final int DEFAULT_DISAGREE_VERIFY = 80;
  static final String[] ERROR_STRINGS = {"Poll Complete","Hasher Busy",
      "Hashing Error", "IO Error"
  };
  public static final int ERR_SCHEDULE_HASH = -1;
  public static final int ERR_HASHING = -2;
  public static final int ERR_IO = -3;

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
  long m_createTime;       // poll creation time

  PollTally m_tally;         // the vote tallier
  String m_key;            // the string we used to id this poll
  int m_pollstate;         // one of state constants above
  Deadline m_deadline;    // when election is over
  
  /**
   * create a new poll from a message
   *
   * @param msg the <code>Message</code> which contains the information
   * @param pollspec the PollSpec on which this poll will operate
   * needed to create this poll.
   * @param pm the pollmanager
   */
  Poll(LcapMessage msg, PollSpec pollspec, PollManager pm) {
    m_pollmanager = pm;
    idMgr = pm.getIdentityManager();
    m_caller = idMgr.findIdentity(msg.getOriginAddr());
    m_msg = msg;
    m_pollspec = pollspec;
    m_cus = pollspec.getCachedUrlSet();
    m_createTime = TimeBase.nowMs();
    m_key = msg.getKey();
    m_tally = null;
    m_deadline = Deadline.in(msg.getDuration());
    m_pollstate = PS_INITING;

  }

  abstract public String toString();

  /**
   * Returns true if the poll belongs to this Identity
   * @return true if  we called the poll
   */
  public boolean isMyPoll() {
    return idMgr.isLocalIdentity(m_caller);
  }

  abstract void getConfigValues();

  abstract void receiveMessage(LcapMessage msg);


  /**
   * schedule the hash for this poll.
   * @param hasher the MessageDigest used to hash the content
   * @param timer the Deadline by which we must complete
   * @param key the Object which will be returned from the hasher. Always the
   * message which triggered the hash
   * @param callback the hashing callback to use on return
   * @return true if hash successfully completed.
   */
  abstract boolean scheduleHash(MessageDigest hasher, Deadline timer,
                                Serializable key,
                                HashService.Callback callback);

  /**
   * schedule a vote by a poll.  we've already completed the hash so we're
   * only interested in how long we have remaining.
   */
  abstract void scheduleVote();

  /**
   * check the hash result obtained by the hasher with one stored in the
   * originating message.
   * @param hashResult byte array containing the result of the hasher
   * @param vote the Vote to check.
   */
  abstract void checkVote(byte[] hashResult, Vote vote);

  /**
   * randomly verify a vote.  The random percentage is determined by
   * agreement and reputation of the voter.
   * @param vote the Vote to check
   * @param isAgreeVote true if this vote agreed with ours, false otherwise
   * @return true if we called a verify poll, false otherwise.
   */
  abstract boolean randomVerify(Vote vote, boolean isAgreeVote);

  /**
   * cast our vote for this poll
   */
  abstract void castOurVote();

  /**
   * start the poll.
   */
  abstract void startPoll();

  /**
   * attempt to schedule our hash.  This will try 3 times to get a deadline
   * that will is successfully scheduled
   * @return boolean true if we sucessfully schedule hash; false otherwise.
   */
  abstract boolean scheduleOurHash();

  /**
   * cast our vote in the current poll
   */
  abstract void voteInPoll();


  /**
   * is our poll currently in an error condition
   * @return true if the poll state is an error value
   */
  abstract boolean isErrorState();

  /**
   * finish the poll once the deadline has expired. we update our poll record
   * and prevent any more activity in this poll.
   */
  abstract void stopPoll();

  /**
   * prepare to check a vote in a poll.  This should check any conditions that
   *  might make running a vote check unneccessary.
   * @param msg the message which is triggering the poll
   * @return boolean true if the poll should run, false otherwise
   */
  abstract boolean shouldCheckVote(LcapMessage msg);

  /**
   * start the hash required for a vote cast in this poll
   */
  abstract void startVoteCheck();

  /**
   * stop and record a vote cast in this poll
   */
  abstract void stopVoteCheck();

  /**
   * are there too many votes waiting to be tallied
   * @return true if we have a quorum worth of votes already pending
   */
  abstract boolean tooManyPending();

  abstract Vote copyVote(Vote vote, boolean agree);

  /**
   * Return the poll spec used by this poll
   * @return the PollSpec
   */
  public PollSpec getPollSpec() {
    return m_pollspec;
  }

  /* Return the version of the protocol in use
   * @return the protocol version
   */
   public int getVersion() {
     return m_pollspec.getVersion();
   }

  /**
   * get the message used to define this Poll
   * @return <code>Message</code>
   */
  public LcapMessage getMessage() {
    return m_msg;
  }

  /**
   * get the VoteTally for this Poll
   * @return VoteTally for this poll
   */
  public PollTally getVoteTally() {
    return m_tally;
  }

  public String getKey() {
    return m_key;
  }

  public String getErrorString() {
    if(isErrorState()) {
      return m_tally.getErrString();
    }
    return "No Error";
  }

  abstract double getMargin();

  
  /**
   * Return a hasher preinited with the challenge and verifier
   * @param challenge the challenge bytes
   * @param verifier the verifier bytes
   * @return a MessageDigest
   */
  abstract MessageDigest getInitedHasher(byte[] challenge, byte[] verifier);

  public Deadline getDeadline() {
    return m_deadline;
  }

}
