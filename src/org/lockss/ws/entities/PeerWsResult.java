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

/**
 * Container for the information related to a peer that is the result of a
 * query.
 */
public class PeerWsResult {
  private String peerId;
  private Long lastMessage;
  private String messageType;
  private Long messageCount;
  private Long lastPoll;
  private Long lastVote;
  private Long lastInvitation;
  private Long invitationCount;
  private Long pollsCalled;
  private Long votesCast;
  private Long pollsRejected;
  private String nakReason;
  private List<String> groups;
  private Boolean platformGroupMatch;

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
   * Provides the timestamp of the last message.
   * 
   * @return a Long with the timestamp as the number of milliseconds since the
   *         beginning of 1970.
   */
  public Long getLastMessage() {
    return lastMessage;
  }
  public void setLastMessage(Long lastMessage) {
    this.lastMessage = lastMessage;
  }

  /**
   * Provides the message type.
   * 
   * @return a String with the message type.
   */
  public String getMessageType() {
    return messageType;
  }
  public void setMessageType(String messageType) {
    this.messageType = messageType;
  }

  /**
   * Provides the message count.
   * 
   * @return a Long with the count.
   */
  public Long getMessageCount() {
    return messageCount;
  }
  public void setMessageCount(Long messageCount) {
    this.messageCount = messageCount;
  }

  /**
   * Provides the timestamp of the last poll.
   * 
   * @return a Long with the timestamp as the number of milliseconds since the
   *         beginning of 1970.
   */
  public Long getLastPoll() {
    return lastPoll;
  }
  public void setLastPoll(Long lastPoll) {
    this.lastPoll = lastPoll;
  }

  /**
   * Provides the timestamp of the last vote.
   * 
   * @return a Long with the timestamp as the number of milliseconds since the
   *         beginning of 1970.
   */
  public Long getLastVote() {
    return lastVote;
  }
  public void setLastVote(Long lastVote) {
    this.lastVote = lastVote;
  }

  /**
   * Provides the timestamp of the last invitation.
   * 
   * @return a Long with the timestamp as the number of milliseconds since the
   *         beginning of 1970.
   */
  public Long getLastInvitation() {
    return lastInvitation;
  }
  public void setLastInvitation(Long lastInvitation) {
    this.lastInvitation = lastInvitation;
  }

  /**
   * Provides the invitation count.
   * 
   * @return a Long with the count.
   */
  public Long getInvitationCount() {
    return invitationCount;
  }
  public void setInvitationCount(Long invitationCount) {
    this.invitationCount = invitationCount;
  }

  /**
   * Provides the count of polls called.
   * 
   * @return a Long with the count.
   */
  public Long getPollsCalled() {
    return pollsCalled;
  }
  public void setPollsCalled(Long pollsCalled) {
    this.pollsCalled = pollsCalled;
  }

  /**
   * Provides the count of votes cast.
   * 
   * @return a Long with the count.
   */
  public Long getVotesCast() {
    return votesCast;
  }
  public void setVotesCast(Long votesCast) {
    this.votesCast = votesCast;
  }

  /**
   * Provides the count of polls rejected.
   * 
   * @return a Long with the count.
   */
  public Long getPollsRejected() {
    return pollsRejected;
  }
  public void setPollsRejected(Long pollsRejected) {
    this.pollsRejected = pollsRejected;
  }

  /**
   * Provides the reason for a NAK message.
   * 
   * @return a String with the reason.
   */
  public String getNakReason() {
    return nakReason;
  }
  public void setNakReason(String nakReason) {
    this.nakReason = nakReason;
  }

  /**
   * Provides the groups.
   * 
   * @return a List<String> with the groups.
   */
  public List<String> getGroups() {
    return groups;
  }
  public void setGroups(List<String> groups) {
    this.groups = groups;
  }

  /**
   * Provides an indication of whether there is a platform group match.
   * 
   * @return a Boolean with the indication.
   */
  public Boolean getPlatformGroupMatch() {
    return platformGroupMatch;
  }
  public void setPlatformGroupMatch(Boolean platformGroupMatch) {
    this.platformGroupMatch = platformGroupMatch;
  }

  @Override
  public String toString() {
    return "PeerWsResult [peerId=" + peerId + ", lastMessage=" + lastMessage
	+ ", messageType=" + messageType + ", messageCount=" + messageCount
	+ ", lastPoll=" + lastPoll + ", lastVote=" + lastVote
	+ ", lastInvitation=" + lastInvitation + ", invitationCount="
	+ invitationCount + ", pollsCalled=" + pollsCalled + ", votesCast="
	+ votesCast + ", pollsRejected=" + pollsRejected + ", nakReason="
	+ nakReason + ", groups=" + groups + ", platformGroupMatch="
	+ platformGroupMatch + "]";
  }
}
