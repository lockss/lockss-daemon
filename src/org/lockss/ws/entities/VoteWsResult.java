/*
 * $Id: VoteWsResult.java,v 1.1 2014-04-25 23:11:00 fergaloy-sf Exp $
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
 * Container for the information related to a vote that is the result of a
 * query.
 */
package org.lockss.ws.entities;

public class VoteWsResult {
  private String auName;
  private String callerId;
  private String voteStatus;
  private Long startTime;
  private Long deadline;
  private String voteKey;
  private Boolean isPollActive;
  private String currentState;
  private String errorDetail;
  private Long voteDeadline;
  private Long duration;
  private Long remainingTime;
  private Double agreementHint;
  private String pollerNonce;
  private String voterNonce;
  private String voterNonce2;
  private Boolean isSymmetricPoll;
  private Integer agreedUrlCount;
  private Integer disagreedUrlCount;
  private Integer pollerOnlyUrlCount;
  private Integer voterOnlyUrlCount;

  public String getAuName() {
    return auName;
  }
  public void setAuName(String auName) {
    this.auName = auName;
  }
  public String getCallerId() {
    return callerId;
  }
  public void setCallerId(String callerId) {
    this.callerId = callerId;
  }
  public String getVoteStatus() {
    return voteStatus;
  }
  public void setVoteStatus(String voteStatus) {
    this.voteStatus = voteStatus;
  }
  public Long getStartTime() {
    return startTime;
  }
  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }
  public Long getDeadline() {
    return deadline;
  }
  public void setDeadline(Long deadline) {
    this.deadline = deadline;
  }
  public String getVoteKey() {
    return voteKey;
  }
  public void setVoteKey(String voteKey) {
    this.voteKey = voteKey;
  }
  public Boolean getIsPollActive() {
    return isPollActive;
  }
  public void setIsPollActive(Boolean isPollActive) {
    this.isPollActive = isPollActive;
  }
  public String getCurrentState() {
    return currentState;
  }
  public void setCurrentState(String currentState) {
    this.currentState = currentState;
  }
  public String getErrorDetail() {
    return errorDetail;
  }
  public void setErrorDetail(String errorDetail) {
    this.errorDetail = errorDetail;
  }
  public Long getVoteDeadline() {
    return voteDeadline;
  }
  public void setVoteDeadline(Long voteDeadline) {
    this.voteDeadline = voteDeadline;
  }
  public Long getDuration() {
    return duration;
  }
  public void setDuration(Long duration) {
    this.duration = duration;
  }
  public Long getRemainingTime() {
    return remainingTime;
  }
  public void setRemainingTime(Long remainingTime) {
    this.remainingTime = remainingTime;
  }
  public Double getAgreementHint() {
    return agreementHint;
  }
  public void setAgreementHint(Double agreementHint) {
    this.agreementHint = agreementHint;
  }
  public String getPollerNonce() {
    return pollerNonce;
  }
  public void setPollerNonce(String pollerNonce) {
    this.pollerNonce = pollerNonce;
  }
  public String getVoterNonce() {
    return voterNonce;
  }
  public void setVoterNonce(String voterNonce) {
    this.voterNonce = voterNonce;
  }
  public String getVoterNonce2() {
    return voterNonce2;
  }
  public void setVoterNonce2(String voterNonce2) {
    this.voterNonce2 = voterNonce2;
  }
  public Boolean getIsSymmetricPoll() {
    return isSymmetricPoll;
  }
  public void setIsSymmetricPoll(Boolean isSymmetricPoll) {
    this.isSymmetricPoll = isSymmetricPoll;
  }
  public Integer getAgreedUrlCount() {
    return agreedUrlCount;
  }
  public void setAgreedUrlCount(Integer agreedUrlCount) {
    this.agreedUrlCount = agreedUrlCount;
  }
  public Integer getDisagreedUrlCount() {
    return disagreedUrlCount;
  }
  public void setDisagreedUrlCount(Integer disagreedUrlCount) {
    this.disagreedUrlCount = disagreedUrlCount;
  }
  public Integer getPollerOnlyUrlCount() {
    return pollerOnlyUrlCount;
  }
  public void setPollerOnlyUrlCount(Integer pollerOnlyUrlCount) {
    this.pollerOnlyUrlCount = pollerOnlyUrlCount;
  }
  public Integer getVoterOnlyUrlCount() {
    return voterOnlyUrlCount;
  }
  public void setVoterOnlyUrlCount(Integer voterOnlyUrlCount) {
    this.voterOnlyUrlCount = voterOnlyUrlCount;
  }

  @Override
  public String toString() {
    return "VoteWsResult [auName=" + auName + ", callerId=" + callerId
	+ ", voteStatus=" + voteStatus + ", startTime=" + startTime
	+ ", deadline=" + deadline + ", voteKey=" + voteKey + ", isPollActive="
	+ isPollActive + ", currentState=" + currentState + ", errorDetail="
	+ errorDetail + ", voteDeadline=" + voteDeadline + ", duration="
	+ duration + ", remainingTime=" + remainingTime + ", agreementHint="
	+ agreementHint + ", pollerNonce=" + pollerNonce + ", voterNonce="
	+ voterNonce + ", voterNonce2=" + voterNonce2 + ", isSymmetricPoll="
	+ isSymmetricPoll + ", agreedUrlCount=" + agreedUrlCount
	+ ", disagreedUrlCount=" + disagreedUrlCount + ", pollerOnlyUrlCount="
	+ pollerOnlyUrlCount + ", voterOnlyUrlCount=" + voterOnlyUrlCount + "]";
  }
}
