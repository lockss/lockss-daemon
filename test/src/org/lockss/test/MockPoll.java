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

}