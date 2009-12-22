/*
* $Id: MockPoll.java,v 1.7 2009-12-22 02:19:43 tlipkis Exp $
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
package org.lockss.test;

import org.lockss.protocol.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.poller.*;

/**
 * @author Claire Griffin
 * @version 1.0
 */

public class MockPoll implements Poll {
  int m_version;
  PollTally m_tally;
  LcapMessage m_msg; // The message which triggered the poll
  PollSpec m_pollspec;
  String m_key; // the string we used to id this poll
  Deadline m_deadline; // when election is over
  boolean m_isMine;

  public MockPoll() {

  }

  public void startPoll() {

  }

  public void stopPoll() {

  }

  public void abortPoll() {
  }

  public boolean isSubpollRunning() {
    return false;
  }

  /**
   * Returns true if the poll belongs to this Identity
   * @return true if  we called the poll
   */
  public boolean isMyPoll() {
    return m_isMine;
  }

  /**
   * set to true if we started this poll, false otherwise
   * @param isMine true if we started the poll.
   */
  public void setIsMyPoll(boolean isMine) {
    m_isMine = isMine;
  }

  /**
   * Return the poll spec used by this poll
   * @return the PollSpec
   */
  public PollSpec getPollSpec() {
    return m_pollspec;
  }

  /**
   * set the poll spec used to define the poll
   * @param pollspec the poll spec used to define the poll
   */
  public void setPollSpec(PollSpec pollspec) {
    m_pollspec = pollspec;
  }

  /**
   * Return the cached url set.
   */
  public CachedUrlSet getCachedUrlSet() {
    return m_pollspec.getCachedUrlSet();
  }

  /* Return the version of the protocol in use
   * @return the protocol version
   */
  public int getVersion() {
    return m_version;
  }

  /**
   * set the poll protocol version
   * @param version the poll version
   */
  public void setVersion(int version) {
    m_version = version;
  }

  /**
   * get the message used to define this Poll
   * @return <code>Message</code>
   */
  public LcapMessage getMessage() {
    return m_msg;
  }

  /**
   * set the message which triggered this poll
   * @param msg the LcapMessage for this poll
   */
  public void setMessage(LcapMessage msg) {
    m_msg = msg;
  }

  /**
   * get the poll identifier key
   * @return the key as a String
   */
  public String getKey() {
    return m_key;
  }

  /**
   * set the poll id key
   * @param key the string representing the key of the poll
   */
  public void setKey(String key) {
    m_key = key;
  }

  /**
   * Return the poll's deadline
   * @return the Deadline object for this poll.
   */
  public Deadline getDeadline() {
    return m_deadline;
  }

  /**
   * set the deadline by which the poll must complete.
   * @param deadline the Deadline for this poll
   */
  public void setDeadline(Deadline deadline) {
    m_deadline = deadline;
  }

  /**
   * get the PollTally for this Poll
   * @return VoteTally for this poll
   */
  public PollTally getVoteTally() {
    return m_tally;
  }

  /**
   * set the voteTally for this poll.
   * @param tally the PollTally for this poll
   */
  public void setVoteTally(PollTally tally) {
    m_tally = tally;
  }

  /**
   * get the type of the poll.
   */
  public int getType() {
    return 0;
  }

  public ArchivalUnit getAu() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public String getStatusString() {
    throw new UnsupportedOperationException("Not implemented");
  }

}
