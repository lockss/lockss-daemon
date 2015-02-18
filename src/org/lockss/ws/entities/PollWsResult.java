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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Container for the information related to a poll that is the result of a
 * query.
 */
public class PollWsResult {
  private String auId;
  private String auName;
  private Integer participantCount;
  private String pollStatus;
  private Integer talliedUrlCount;
  private List<String> talliedUrls;
  private Integer hashErrorCount;
  private Map<String, String> errorUrls;
  private Integer completedRepairCount;
  private List<RepairWsResult> completedRepairs;
  private Float percentAgreement;
  private Long startTime;
  private Long deadline;
  private String pollKey;
  private String pollVariant;
  private String errorDetail;
  private String additionalInfo;
  private Long voteDeadline;
  private Long duration;
  private Long remainingTime;
  private Long endTime;
  private Integer agreedUrlCount;
  private Set<String> agreedUrls;
  private Integer disagreedUrlCount;
  private Set<String> disagreedUrls;
  private Integer noQuorumUrlCount;
  private Set<String> noQuorumUrls;
  private Integer tooCloseUrlCount;
  private Set<String> tooCloseUrls;
  private Integer activeRepairCount;
  private List<RepairWsResult> activeRepairs;
  private Long bytesHashedCount;
  private Long bytesReadCount;
  private Integer quorum;
  private List<ParticipantWsResult> participants;

  /**
   * Provides the Archival Unit identifier.
   * 
   * @return a String with the identifier.
   */
  public String getAuId() {
    return auId;
  }
  public void setAuId(String auId) {
    this.auId = auId;
  }

  /**
   * Provides the Archival Unit name.
   * 
   * @return a String with the name.
   */
  public String getAuName() {
    return auName;
  }
  public void setAuName(String auName) {
    this.auName = auName;
  }

  /**
   * Provides the participant count.
   * 
   * @return an Integer with the count.
   */
  public Integer getParticipantCount() {
    return participantCount;
  }
  public void setParticipantCount(Integer participantCount) {
    this.participantCount = participantCount;
  }

  /**
   * Provides the poll status.
   * 
   * @return a String with the status.
   */
  public String getPollStatus() {
    return pollStatus;
  }
  public void setPollStatus(String pollStatus) {
    this.pollStatus = pollStatus;
  }

  /**
   * Provides the count of tallied URLs.
   * 
   * @return an Integer with the count.
   */
  public Integer getTalliedUrlCount() {
    return talliedUrlCount;
  }
  public void setTalliedUrlCount(Integer talliedUrlCount) {
    this.talliedUrlCount = talliedUrlCount;
  }

  /**
   * Provides the tallied URLs.
   * 
   * @return a List<String> with the tallied URLs.
   */
  public List<String> getTalliedUrls() {
    return talliedUrls;
  }
  public void setTalliedUrls(List<String> talliedUrls) {
    this.talliedUrls = talliedUrls;
  }

  /**
   * Provides the count of hash errors.
   * 
   * @return an Integer with the count.
   */
  public Integer getHashErrorCount() {
    return hashErrorCount;
  }
  public void setHashErrorCount(Integer hashErrorCount) {
    this.hashErrorCount = hashErrorCount;
  }

  /**
   * Provides the data on URLs with errors.
   * 
   * @return a Map<String, String> with the data on URLs with errors.
   */
  public Map<String, String> getErrorUrls() {
    return errorUrls;
  }
  public void setErrorUrls(Map<String, String> errorUrls) {
    this.errorUrls = errorUrls;
  }

  /**
   * Provides the count of completed repairs.
   * 
   * @return an Integer with the count.
   */
  public Integer getCompletedRepairCount() {
    return completedRepairCount;
  }
  public void setCompletedRepairCount(Integer completedRepairCount) {
    this.completedRepairCount = completedRepairCount;
  }

  /**
   * Provides the data on completed repairs.
   * 
   * @return a List<RepairWsResult> with the data on completed repairs.
   */
  public List<RepairWsResult> getCompletedRepairs() {
    return completedRepairs;
  }
  public void setCompletedRepairs(List<RepairWsResult> completedRepairs) {
    this.completedRepairs = completedRepairs;
  }

  /**
   * Provides the poll agreement percentage.
   * 
   * @return a Float with the poll agreement percentage.
   */
  public Float getPercentAgreement() {
    return percentAgreement;
  }
  public void setPercentAgreement(Float percentAgreement) {
    this.percentAgreement = percentAgreement;
  }

  /**
   * Provides the poll start timestamp.
   * 
   * @return a Long with the timestamp as the number of milliseconds since the
   *         beginning of 1970.
   */
  public Long getStartTime() {
    return startTime;
  }
  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }

  /**
   * Provides the poll deadline timestamp.
   * 
   * @return a Long with the timestamp as the number of milliseconds since the
   *         beginning of 1970.
   */
  public Long getDeadline() {
    return deadline;
  }
  public void setDeadline(Long deadline) {
    this.deadline = deadline;
  }

  /**
   * Provides the poll key.
   * 
   * @return a String with the key.
   */
  public String getPollKey() {
    return pollKey;
  }
  public void setPollKey(String pollKey) {
    this.pollKey = pollKey;
  }

  /**
   * Provides the poll variant.
   * 
   * @return a String with the variant.
   */
  public String getPollVariant() {
    return pollVariant;
  }
  public void setPollVariant(String pollVariant) {
    this.pollVariant = pollVariant;
  }

  /**
   * Provides the poll error detail.
   * 
   * @return a String with the error detail.
   */
  public String getErrorDetail() {
    return errorDetail;
  }
  public void setErrorDetail(String errorDetail) {
    this.errorDetail = errorDetail;
  }

  /**
   * Provides the poll additional information.
   * 
   * @return a String with the additional information.
   */
  public String getAdditionalInfo() {
    return additionalInfo;
  }
  public void setAdditionalInfo(String additionalInfo) {
    this.additionalInfo = additionalInfo;
  }

  /**
   * Provides the vote deadline timestamp.
   * 
   * @return a Long with the timestamp as the number of milliseconds since the
   *         beginning of 1970.
   */
  public Long getVoteDeadline() {
    return voteDeadline;
  }
  public void setVoteDeadline(Long voteDeadline) {
    this.voteDeadline = voteDeadline;
  }

  /**
   * Provides the poll duration.
   * 
   * @return a Long with the duration in milliseconds.
   */
  public Long getDuration() {
    return duration;
  }
  public void setDuration(Long duration) {
    this.duration = duration;
  }

  /**
   * Provides the poll remaining time.
   * 
   * @return a Long with the remaining time in milliseconds.
   */
  public Long getRemainingTime() {
    return remainingTime;
  }
  public void setRemainingTime(Long remainingTime) {
    this.remainingTime = remainingTime;
  }

  /**
   * Provides the poll end timestamp.
   * 
   * @return a Long with the timestamp as the number of milliseconds since the
   *         beginning of 1970.
   */
  public Long getEndTime() {
    return endTime;
  }
  public void setEndTime(Long endTime) {
    this.endTime = endTime;
  }

  /**
   * Provides the count of URLs in agreement.
   * 
   * @return an Integer with the count.
   */
  public Integer getAgreedUrlCount() {
    return agreedUrlCount;
  }
  public void setAgreedUrlCount(Integer agreedUrlCount) {
    this.agreedUrlCount = agreedUrlCount;
  }

  /**
   * Provides the URLs in agreement.
   * 
   * @return a Set<String> with the URLs.
   */
  public Set<String> getAgreedUrls() {
    return agreedUrls;
  }
  public void setAgreedUrls(Set<String> agreedUrls) {
    this.agreedUrls = agreedUrls;
  }

  /**
   * Provides the count of URLs in disagreement.
   * 
   * @return an Integer with the count.
   */
  public Integer getDisagreedUrlCount() {
    return disagreedUrlCount;
  }
  public void setDisagreedUrlCount(Integer disagreedUrlCount) {
    this.disagreedUrlCount = disagreedUrlCount;
  }

  /**
   * Provides the URLs in disagreement.
   * 
   * @return a Set<String> with the URLs.
   */
  public Set<String> getDisagreedUrls() {
    return disagreedUrls;
  }
  public void setDisagreedUrls(Set<String> disagreedUrls) {
    this.disagreedUrls = disagreedUrls;
  }

  /**
   * Provides the count of URLs with no quorum.
   * 
   * @return an Integer with the count.
   */
  public Integer getNoQuorumUrlCount() {
    return noQuorumUrlCount;
  }
  public void setNoQuorumUrlCount(Integer noQuorumUrlCount) {
    this.noQuorumUrlCount = noQuorumUrlCount;
  }

  /**
   * Provides the URLs with no quorum.
   * 
   * @return a Set<String> with the URLs.
   */
  public Set<String> getNoQuorumUrls() {
    return noQuorumUrls;
  }
  public void setNoQuorumUrls(Set<String> noQuorumUrls) {
    this.noQuorumUrls = noQuorumUrls;
  }

  /**
   * Provides the count of URLs that are too close.
   * 
   * @return an Integer with the count.
   */
  public Integer getTooCloseUrlCount() {
    return tooCloseUrlCount;
  }
  public void setTooCloseUrlCount(Integer tooCloseUrlCount) {
    this.tooCloseUrlCount = tooCloseUrlCount;
  }

  /**
   * Provides the URLs that are too close.
   * 
   * @return a Set<String> with the URLs.
   */
  public Set<String> getTooCloseUrls() {
    return tooCloseUrls;
  }
  public void setTooCloseUrls(Set<String> tooCloseUrls) {
    this.tooCloseUrls = tooCloseUrls;
  }

  /**
   * Provides the count of active repairs.
   * 
   * @return an Integer with the count.
   */
  public Integer getActiveRepairCount() {
    return activeRepairCount;
  }
  public void setActiveRepairCount(Integer activeRepairCount) {
    this.activeRepairCount = activeRepairCount;
  }

  /**
   * Provides the active repairs.
   * 
   * @return a List<RepairWsResult> with the active repairs.
   */
  public List<RepairWsResult> getActiveRepairs() {
    return activeRepairs;
  }
  public void setActiveRepairs(List<RepairWsResult> activeRepairs) {
    this.activeRepairs = activeRepairs;
  }

  /**
   * Provides the count of bytes hashed.
   * 
   * @return a Long with the count.
   */
  public Long getBytesHashedCount() {
    return bytesHashedCount;
  }
  public void setBytesHashedCount(Long bytesHashedCount) {
    this.bytesHashedCount = bytesHashedCount;
  }

  /**
   * Provides the count of bytes read.
   * 
   * @return a Long with the count.
   */
  public Long getBytesReadCount() {
    return bytesReadCount;
  }
  public void setBytesReadCount(Long bytesReadCount) {
    this.bytesReadCount = bytesReadCount;
  }

  /**
   * Provides the quorum.
   * 
   * @return an Integer with the quorum.
   */
  public Integer getQuorum() {
    return quorum;
  }
  public void setQuorum(Integer quorum) {
    this.quorum = quorum;
  }

  /**
   * Provides the poll participants.
   * 
   * @return a List<ParticipantWsResult> with the participants.
   */
  public List<ParticipantWsResult> getParticipants() {
    return participants;
  }
  public void setParticipants(List<ParticipantWsResult> participants) {
    this.participants = participants;
  }

  @Override
  public String toString() {
    return "PollWsResult [auId=" + auId + ", auName=" + auName
	+ ", participantCount=" + participantCount + ", pollStatus="
	+ pollStatus + ", talliedUrlCount=" + talliedUrlCount + ", talliedUrls="
	+ talliedUrls + ", hashErrorCount=" + hashErrorCount + ", errorUrls="
	+ errorUrls + ", completedRepairCount=" + completedRepairCount
	+ ", completedRepairs=" + completedRepairs + ", percentAgreement="
	+ percentAgreement + ", startTime=" + startTime + ", deadline="
	+ deadline + ", pollKey=" + pollKey + ", pollVariant=" + pollVariant
	+ ", errorDetail=" + errorDetail + ", additionalInfo=" + additionalInfo
	+ ", voteDeadline=" + voteDeadline + ", duration=" + duration
	+ ", remainingTime=" + remainingTime + ", endTime=" + endTime
	+ ", agreedUrlCount=" + agreedUrlCount + ", agreedUrls=" + agreedUrls
	+ ", disagreedUrlCount=" + disagreedUrlCount + ", disagreedUrls="
	+ disagreedUrls + ", noQuorumUrlCount=" + noQuorumUrlCount
	+ ", noQuorumUrls=" + noQuorumUrls + ", tooCloseUrlCount="
	+ tooCloseUrlCount + ", tooCloseUrls=" + tooCloseUrls
	+ ", activeRepairCount=" + activeRepairCount + ", activeRepairs="
	+ activeRepairs + ", bytesHashedCount=" + bytesHashedCount
	+ ", bytesReadCount=" + bytesReadCount + ", quorum=" + quorum
	+ ", participants=" + participants + "]";
  }
}
