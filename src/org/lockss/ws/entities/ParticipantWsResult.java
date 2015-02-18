/*
 * $Id$
 */

/*

 Copyright (c) 2014 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.ws.entities;

/**
 * Container for the information related to a poll participant that is the
 * result of a query.
 */
public class ParticipantWsResult {
  private String peerId;
  private String peerStatus;
  private Boolean hasVoted;
  private Float percentAgreement;
  private Long agreedVoteCount;
  private Long disagreedVoteCount;
  private Long pollerOnlyVoteCount;
  private Long voterOnlyVotecount;
  private Long bytesHashed;
  private Long bytesRead;
  private String currentState;
  private Long lastStateChange;
  private Boolean isExParticipant;

  /**
   * Provides the peer identifier.
   * 
   * @return a String with the identifier.
   */
  public String getPeerId() {
    return peerId;
  }
  public void setPeerId(String peerId) {
    this.peerId = peerId;
  }

  /**
   * Provides the peer status.
   * 
   * @return a String with the status.
   */
  public String getPeerStatus() {
    return peerStatus;
  }
  public void setPeerStatus(String peerStatus) {
    this.peerStatus = peerStatus;
  }

  /**
   * Provides an indication of whether the participant has voted.
   * 
   * @return a Boolean with the indication.
   */
  public Boolean getHasVoted() {
    return hasVoted;
  }
  public void setHasVoted(Boolean hasVoted) {
    this.hasVoted = hasVoted;
  }

  /**
   * Provides the participant agreement percentage.
   * 
   * @return a Float with the agreement percentage.
   */
  public Float getPercentAgreement() {
    return percentAgreement;
  }
  public void setPercentAgreement(Float percentAgreement) {
    this.percentAgreement = percentAgreement;
  }

  /**
   * Provides the count of votes in agreement.
   * 
   * @return a Long with the count.
   */
  public Long getAgreedVoteCount() {
    return agreedVoteCount;
  }
  public void setAgreedVoteCount(Long agreedVoteCount) {
    this.agreedVoteCount = agreedVoteCount;
  }

  /**
   * Provides the count of votes in disagreement.
   * 
   * @return a Long with the count.
   */
  public Long getDisagreedVoteCount() {
    return disagreedVoteCount;
  }
  public void setDisagreedVoteCount(Long disagreedVoteCount) {
    this.disagreedVoteCount = disagreedVoteCount;
  }

  /**
   * Provides the count of votes as poller.
   * 
   * @return a Long with the count.
   */
  public Long getPollerOnlyVoteCount() {
    return pollerOnlyVoteCount;
  }
  public void setPollerOnlyVoteCount(Long pollerOnlyVoteCount) {
    this.pollerOnlyVoteCount = pollerOnlyVoteCount;
  }

  /**
   * Provides the count of votes as voter.
   * 
   * @return a Long with the count.
   */
  public Long getVoterOnlyVotecount() {
    return voterOnlyVotecount;
  }
  public void setVoterOnlyVotecount(Long voterOnlyVotecount) {
    this.voterOnlyVotecount = voterOnlyVotecount;
  }

  /**
   * Provides the count of bytes hashed.
   * 
   * @return a Long with the byte count.
   */
  public Long getBytesHashed() {
    return bytesHashed;
  }
  public void setBytesHashed(Long bytesHashed) {
    this.bytesHashed = bytesHashed;
  }

  /**
   * Provides the count of bytes read.
   * 
   * @return a Long with the byte count.
   */
  public Long getBytesRead() {
    return bytesRead;
  }
  public void setBytesRead(Long bytesRead) {
    this.bytesRead = bytesRead;
  }

  /**
   * Provides the participant current state.
   * 
   * @return a String with the current state.
   */
  public String getCurrentState() {
    return currentState;
  }
  public void setCurrentState(String currentState) {
    this.currentState = currentState;
  }

  /**
   * Provides the timestamp of the last state change.
   * 
   * @return a Long with the timestamp as the number of milliseconds since the
   *         beginning of 1970.
   */
  public Long getLastStateChange() {
    return lastStateChange;
  }
  public void setLastStateChange(Long lastStateChange) {
    this.lastStateChange = lastStateChange;
  }

  /**
   * Provides an indication of whether the peer is an ex-participant.
   * 
   * @return a Boolean with the indication.
   */
  public Boolean getIsExParticipant() {
    return isExParticipant;
  }
  public void setIsExParticipant(Boolean isExParticipant) {
    this.isExParticipant = isExParticipant;
  }

  @Override
  public String toString() {
    return "ParticipantWsResult [peerId=" + peerId + ", peerStatus="
	+ peerStatus + ", hasVoted=" + hasVoted + ", percentAgreement="
	+ percentAgreement + ", agreedVoteCount=" + agreedVoteCount
	+ ", disagreedVoteCount=" + disagreedVoteCount
	+ ", pollerOnlyVoteCount=" + pollerOnlyVoteCount
	+ ", voterOnlyVotecount=" + voterOnlyVotecount + ", bytesHashed="
	+ bytesHashed + ", bytesRead=" + bytesRead + ", currentState="
	+ currentState + ", lastStateChange=" + lastStateChange
	+ ", isExParticipant=" + isExParticipant + "]";
  }
}
