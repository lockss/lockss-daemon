package org.lockss.poller;

import org.lockss.protocol.*;
import org.lockss.util.*;

public interface Poll {
  public static final String[] PollName = { "Name", "Content", "Verify"};

  public static final int NAME_POLL = 0;

  public static final int CONTENT_POLL = 1;

  public static final int VERIFY_POLL = 2;

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
}