package org.lockss.poller;

import java.util.*;

import org.lockss.plugin.*;

public interface Tallier {
  /**
   * return the unique key for the poll for this tally
   * @return a String representing the key
   */
  public String getPollKey();
  /**
   * Returns true if the poll belongs to this Identity
   * @return true if this Identity
   */
  public boolean isMyPoll();

  /**
   * Return the poll spec used by this poll
   * @return the PollSpec
   */
  public PollSpec getPollSpec();

  /**
   * the cached url set for this poll
   * @return a CachedUrlSet
   */
  public CachedUrlSet getCachedUrlSet();

  /**
   * the archival unit for this poll
   * @return the ArchivalUnit
   */
  public ArchivalUnit getArchivalUnit();

  /**
   * Returns poll type constant
   * @return integer constant for this poll
   */
  public int getType();

  /**
   * returns the poll start time
   * @return start time as a long
   */
  public long getStartTime();

  /**
   * returns the poll duration
   * @return the duration as a long
   */
  public long getDuration();

  /**
   * return the votes cast in this poll
   * @return the list of votes
   */

  public List getPollVotes();

  /**
   * return an interator for the set of entries tallied during the vote
   * @return the completed list of entries
   */
  public Iterator getCorrectEntries();

  /**
   * return an interator for the set of entries we have locally
   * @return the list of entries
   */
  public Iterator getLocalEntries();

  /**
   * get the error state for this poll
   * @return 0 == NOERR or one of the poll err conditions
   */
  public int getErr();

  /**
   * get the error as a string
   * @return the String representation of the error
   */
  public String getErrString();


  /**
   * get the current value of the poll tally status
   * @return the status
   */
  public int getStatus();

  /**
   * get the current value of the poll tally status
   * @return the String representation of status
   */
  public String getStatusString();
}