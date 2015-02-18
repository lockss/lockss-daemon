/*
* $Id$
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

import java.util.*;

import org.lockss.util.*;
import org.lockss.protocol.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.daemon.ActivityRegulator;

/**
 * Mock vote tallier.
 * @author Claire Griffin
 * @version 1.0
 */

public class MockTallier implements Tallier {
  PollSpec m_pollspec;
  String m_pollKey;
  boolean m_isMyPoll;
  CachedUrlSet m_cus;
  ArchivalUnit m_au;
  int m_polltype;
  long m_startTime;
  long m_duration;
  List m_localEntries = null; // the local entries less the remaining RegExp
  List m_votedEntries = null; // entries which match the won votes in a poll
  ArrayList m_pollVotes;
  int m_error = 0;
  int m_result = 0;
  ActivityRegulator.Lock m_lock;

  public MockTallier() {
  }

  /**
   * Return the poll spec used by this poll
   * @return the PollSpec
   */
  public PollSpec getPollSpec() {
    return m_pollspec;
  }

  void setPollSpec(PollSpec pollspec) {
    m_pollspec = pollspec;
  }

  /**
   * return the unique key for the poll for this tally
   * @return a String representing the key
   */
  public String getPollKey() {
    return m_pollKey;
  }

  void setPollKey(String pollKey) {
    m_pollKey = pollKey;
  }

  /**
   * Returns true if the poll belongs to this Identity
   * @return true if this Identity
   */
  public boolean isMyPoll() {
    return m_isMyPoll;
  }

  public void setMyPoll(boolean isMyPoll) {
    m_isMyPoll = isMyPoll;
  }

  /**
   * the cached url set for this poll
   * @return a CachedUrlSet
   */
  public CachedUrlSet getCachedUrlSet() {
    return m_cus;
  }

  public void setCachedUrlSet(CachedUrlSet cus) {
    m_cus = cus;
  }

  /**
   * the archival unit for this poll
   * @return the ArchivalUnit
   */
  public ArchivalUnit getArchivalUnit() {
    return m_au;
  }

  public void setArchivalUnit(ArchivalUnit au) {
    m_au = au;
  }

  /**
   * Returns poll type constant - one of Poll.NamePoll, Poll.ContentPoll,
   * Poll.VerifyPoll
   * @return integer constant for this poll
   */
  public int getType() {
    return m_polltype;
  }

  public void setType(int pollType) {
    m_polltype = pollType;
  }

  /**
   * returns the poll start time
   * @return start time as a long
   */
  public long getStartTime() {
    return m_startTime;
  }

  /**
   * set the poll start time
   * @param startTime the time in ms at which we started the poll
   */
  public void setStartTime(long startTime) {
    m_startTime = startTime;
  }

  /**
   * returns the poll duration
   * @return the duration as a long
   */
  public long getDuration() {
    return m_duration;
  }

  public void setDuration(long duration) {
    m_duration = duration;
  }

  /**
   * return the votes cast in this poll
   * @return the list of votes
   */

  public List getPollVotes() {
    return Collections.unmodifiableList(m_pollVotes);
  }

  public void setPollVotes(ArrayList pollVotes) {
    m_pollVotes = pollVotes;
  }

  /**
   * return an interator for the set of entries tallied during the vote
   * @return the completed list of entries
   */
  public Iterator getCorrectEntries() {
    return m_votedEntries == null ? CollectionUtil.EMPTY_ITERATOR :
        m_votedEntries.iterator();
  }

  public void setCorrectEntries(List correctEntries) {
    m_votedEntries = correctEntries;
  }

  /**
   * return an interator for the set of entries we have locally
   * @return the list of entries
   */
  public Iterator getLocalEntries() {
    return m_localEntries == null ? CollectionUtil.EMPTY_ITERATOR :
        m_localEntries.iterator();
  }

  public void setLocalEntries(List localEntries) {
    m_localEntries = localEntries;
  }

  /**
   * get the error state for this poll
   * @return 0 == NOERR or one of the poll err conditions
   */
  public int getErr() {
    return m_error;
  }

  public void setErr(int error) {
    m_error = error;
  }

  /**
   * get the error as a string
   * @return the String representation of the error
   */
  public String getErrString() {
    String errString = null;
    return errString;
  }

  /**
   * get the current value of the poll tally status
   * @return the status
   */
  public int getTallyResult() {
    return m_result;
  }

  public String getStatusString() {
    String statusString = null;
    return statusString;
  }

  public void setTallyResult(int result) {
    m_result = result;
  }

  public ActivityRegulator.Lock getActivityLock() {
    return m_lock;
  }

  public void setActivityLock(ActivityRegulator.Lock lock) {
    m_lock = lock;
  }
}
