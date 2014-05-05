/*
 * $Id: ParticipantWsResult.java,v 1.1.2.2 2014-05-05 17:32:30 wkwilson Exp $
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

/**
 * Container for the information related to a poll participant that is the
 * result of a query.
 */
package org.lockss.ws.entities;

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

  public String getPeerId() {
    return peerId;
  }
  public void setPeerId(String peerId) {
    this.peerId = peerId;
  }
  public String getPeerStatus() {
    return peerStatus;
  }
  public void setPeerStatus(String peerStatus) {
    this.peerStatus = peerStatus;
  }
  public Boolean getHasVoted() {
    return hasVoted;
  }
  public void setHasVoted(Boolean hasVoted) {
    this.hasVoted = hasVoted;
  }
  public Float getPercentAgreement() {
    return percentAgreement;
  }
  public void setPercentAgreement(Float percentAgreement) {
    this.percentAgreement = percentAgreement;
  }
  public Long getAgreedVoteCount() {
    return agreedVoteCount;
  }
  public void setAgreedVoteCount(Long agreedVoteCount) {
    this.agreedVoteCount = agreedVoteCount;
  }
  public Long getDisagreedVoteCount() {
    return disagreedVoteCount;
  }
  public void setDisagreedVoteCount(Long disagreedVoteCount) {
    this.disagreedVoteCount = disagreedVoteCount;
  }
  public Long getPollerOnlyVoteCount() {
    return pollerOnlyVoteCount;
  }
  public void setPollerOnlyVoteCount(Long pollerOnlyVoteCount) {
    this.pollerOnlyVoteCount = pollerOnlyVoteCount;
  }
  public Long getVoterOnlyVotecount() {
    return voterOnlyVotecount;
  }
  public void setVoterOnlyVotecount(Long voterOnlyVotecount) {
    this.voterOnlyVotecount = voterOnlyVotecount;
  }
  public Long getBytesHashed() {
    return bytesHashed;
  }
  public void setBytesHashed(Long bytesHashed) {
    this.bytesHashed = bytesHashed;
  }
  public Long getBytesRead() {
    return bytesRead;
  }
  public void setBytesRead(Long bytesRead) {
    this.bytesRead = bytesRead;
  }
  public String getCurrentState() {
    return currentState;
  }
  public void setCurrentState(String currentState) {
    this.currentState = currentState;
  }
  public Long getLastStateChange() {
    return lastStateChange;
  }
  public void setLastStateChange(Long lastStateChange) {
    this.lastStateChange = lastStateChange;
  }
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
