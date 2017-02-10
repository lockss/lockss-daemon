/*
* $Id$
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

import java.util.*;

import org.lockss.plugin.*;

public interface Tallier {
  public static final int RESULT_POLLING = 0;
  public static final int RESULT_ERROR = 1;
  public static final int RESULT_NOQUORUM = 2;
  public static final int RESULT_TOO_CLOSE = 3;
  public static final int RESULT_UNTRUSTED = 4;
  public static final int RESULT_WON = 5;
  public static final int RESULT_LOST = 6;
  public static final int RESULT_UNVERIFIED = 7;
  public static final int RESULT_VERIFIED = 8;
  public static final int RESULT_DISOWNED = 9;

  /**
   * return the unique key for the poll for this tally
   * @return a String representing the key
   */
  public String getPollKey();

  /**
   * Returns true if the poll belongs to this Identity
   * @return true if this Identity owns the poll
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
   * @return the String representation of status
   */
  public String getStatusString();

  /**
   * return a constant reflecting the results of the poll tally.
   * @return an integer representing the results of the poll
   */
  public int getTallyResult();

}
