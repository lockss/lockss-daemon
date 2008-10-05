/*
 * $Id: V3PollState.java,v 1.1 2007-01-23 21:44:36 smorabito Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.state;

import java.io.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.poller.v3.*;

/**
 * V3PollState holds the state of a single V3 Poll in this cache's history.
 */
public class V3PollState implements LockssSerializable {

  // Common to pollers and voters

  /** A unique ID identifying the poll. */
  private String key;
  /** A human-readable string representing the status of the poll */
  private String status;
  /** The ID of the Archival Unit on which the poll was conducted */ 
  private String auId;
  /** Time that the poll started */
  private long startTime = -1;
  /** Time that the poll ended */
  private long endTime = -1;
  /** The duration of the poll */
  private long duration = -1;
  /** The original deadline of the poll */
  private long deadline = -1;
  /** The original vote deadline of the poll */
  private long voteDeadline = -1;
  /** The nonce of the Poller */
  private byte[] pollerNonce;
  /** Provide a more detailed explanation if there was an error during the
   * poll, and the information is available.
   */
  private String errorDetail;
  /** True if the poll has ended in a non-successful state. */
  private boolean isErrored;
  
  // Used by voters

  /** the nonce of the Voter */
  private byte[] voterNonce;

  // Used by pollers

  /** The percent agreement found by the poll. */
  private double agreement = -1.0;
  /** A list of PeerIdentities that participated as voters in the poll */
  // JAVA15: List<PeerIdentity>
  private List participants = new ArrayList();
  /** Total number of URLs tallied during the poll. */
  private long totalUrlsTallied = 0;
  /** Repairs which are currently active. */
  // JAVA15: List<V3PollerStateBean.Repair>
  private List activeRepairs = new ArrayList();
  /** Repairs which have completed. */
  // JAVA15: List<V3PollerStateBean.Repair>
  private List completedRepairs = new ArrayList();

  // JAVA15: List<String>
  /** List of URLs that had agreement. */
  private List agreeingUrls = new ArrayList();
  /** List of URls for which the vote was lost. */
  private List disagreeingUrls = new ArrayList();
  /** List of URLs for which the vote was too close. */
  private List tooCloseUrls = new ArrayList();
  /** List of URLs which were not held by enough participants to 
   * achieve quorum */
  private List noQuorumUrls = new ArrayList();
  
  
  /** Constructor used by V3 Voter */
  public V3PollState(String pollKey, String auId, long duration,
                     long startTime, long pollDeadline,
                     byte[] pollerNonce, byte[] voterNonce) {
    this.key = pollKey;
    this.auId = auId;
    this.duration = duration;
    this.deadline = pollDeadline;
    this.pollerNonce = pollerNonce;
    this.voterNonce = voterNonce;
  }
  
  /** Constructor used by V3 Poller */
  
  public V3PollState() {}
  
  
  /**
   * Return the length of the poll.
   * 
   * @return If the poll is still active, the expected length of the poll.
   * If the poll has completed, the actual time the poll ran.
   */
  public long length() {
    if (endTime > -1) {
      // The poll has completed.
      return endTime - startTime;
    } else {
      // The poll is still active
      return duration;
    }
  }


  /**
   * @return the list of active repairs
   */
  public List getActiveRepairs() {
    return activeRepairs;
  }


  /**
   * @param activeRepairs the list of active repairs to set
   */
  public void setActiveRepairs(List activeRepairs) {
    this.activeRepairs = activeRepairs;
  }


  /**
    @return the list of agreeing URLs
   */
  public List getAgreeingUrls() {
    return agreeingUrls;
  }


  /**
   * @param agreeingUrls the agreeing URLs to set
   */
  public void setAgreeingUrls(List agreeingUrls) {
    this.agreeingUrls = agreeingUrls;
  }


  /**
   * @return the percent agreement for this poll, or -1 if the tallying
   * has not completed.
   */
  public double getAgreement() {
    return agreement;
  }


  /**
   * @param agreement the percent agreement for the poll.
   */
  public void setAgreement(double agreement) {
    this.agreement = agreement;
  }


  /**
   * @return the auId
   */
  public String getAuId() {
    return auId;
  }


  /**
   * @param auId the auId to set
   */
  public void setAuId(String auId) {
    this.auId = auId;
  }


  /**
   * @return the list of completed repairs.
   */
  public List getCompletedRepairs() {
    return completedRepairs;
  }


  /**
   * @param completedRepairs the completedRepairs to set
   */
  public void setCompletedRepairs(List completedRepairs) {
    this.completedRepairs = completedRepairs;
  }
  
  /**
   * @param A repair to add to the list of completed repairs.
   */
  public void addCompletedRepair(PollerStateBean.Repair r) {
    completedRepairs.add(r);
  }

  /**
   * @return the deadline
   */
  public long getDeadline() {
    return deadline;
  }

  /**
   * @param deadline the deadline to set
   */
  public void setDeadline(long deadline) {
    this.deadline = deadline;
  }

  /**
   * @return The Vote deadline
   */
  public long getVoteDeadline() {
    return voteDeadline;
  }
  
  /**
   * @param deadline The Vote deadline
   */
  public void setVoteDeadline(long deadline) {
    this.voteDeadline = deadline;
  }

  /**
   * @return the disagreeingUrls
   */
  public List getDisagreeingUrls() {
    return disagreeingUrls;
  }


  /**
   * @param disagreeingUrls the disagreeingUrls to set
   */
  public void setDisagreeingUrls(List disagreeingUrls) {
    this.disagreeingUrls = disagreeingUrls;
  }

  /**
   * @param The URL to add to the list of disagreeing URLs.
   */
  public void addDisagreeingUrl(String url) {
    this.disagreeingUrls.add(url);
  }

  /**
   * @return the duration
   */
  public long getDuration() {
    return duration;
  }


  /**
   * @param duration the duration to set
   */
  public void setDuration(long duration) {
    this.duration = duration;
  }


  /**
   * @return the errorDetail
   */
  public String getErrorDetail() {
    return errorDetail;
  }


  /**
   * @param errorDetail the errorDetail to set
   */
  public void setErrorDetail(String errorDetail) {
    this.errorDetail = errorDetail;
  }


  /**
   * @return true if the poll is in an error state.
   */
  public boolean isErrored() {
    return isErrored;
  }


  /**
   * @param err True if the poll is in an error state.
   */
  public void setErrored(boolean isErrored) {
    this.isErrored = isErrored;
  }


  /**
   * @return the poll key.
   */
  public String getKey() {
    return key;
  }


  /**
   * @param key the poll key to set
   */
  public void setKey(String key) {
    this.key = key;
  }


  /**
   * @return the list of No Quorum URLs.
   */
  public List getNoQuorumUrls() {
    return noQuorumUrls;
  }


  /**
   * @param the list of No Quorum URLs.
   */
  public void setNoQuorumUrls(List noQuorumUrls) {
    this.noQuorumUrls = noQuorumUrls;
  }
  
  /**
   * @param A URL to add to the list of No Quorum URLs.
   */
  public void addNoQuorumUrl(String url) {
    noQuorumUrls.add(url);
  }

  /**
   * @return the participants
   */
  public List getParticipants() {
    return participants;
  }


  /**
   * @param participants the participants
   */
  public void setParticipants(List participants) {
    this.participants = participants;
  }


  /**
   * @return the ending time of the poll.
   */
  public long getEndTime() {
    return endTime;
  }


  /**
   * @param pollEndTime the ending time of the poll.
   */
  public void setEndTime(long pollEndTime) {
    this.endTime = pollEndTime;
  }


  /**
   * @return the start time of the poll.
   */
  public long getStartTime() {
    return startTime;
  }


  /**
   * @param the start time of the poll.
   */
  public void setStartTime(long pollStartTime) {
    this.startTime = pollStartTime;
  }


  /**
   * @return a human-readable string indicating thed status of the poll.
   */
  public String getStatus() {
    return status;
  }


  /**
   * @param status a human-readable string indicating thed status of the poll.
   */
  public void setStatus(String status) {
    this.status = status;
  }


  /**
   * @return the list of URLs that are Too Close.
   */
  public List getTooCloseUrls() {
    return tooCloseUrls;
  }


  /**
   * @param tooCloseUrls the list of URLs that are Too Close.
   */
  public void setTooCloseUrls(List tooCloseUrls) {
    this.tooCloseUrls = tooCloseUrls;
  }
  
  public void addTooCloseUrl(String url) {
    this.tooCloseUrls.add(url);
  }


  /**
   * @return the number of URLs that have been tallied.
   */
  public long getTotalUrlsTallied() {
    return totalUrlsTallied;
  }


  /**
   * @param totalUrlsTallied the number of URLs that have been tallied.
   */
  public void setTotalUrlsTallied(long totalUrlsTallied) {
    this.totalUrlsTallied = totalUrlsTallied;
  }

}
