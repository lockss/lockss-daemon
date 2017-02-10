/*
 * $Id$
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

package org.lockss.protocol;

import java.util.*;
import org.lockss.protocol.V3LcapMessage.PollNak;
import org.lockss.util.*;

public class PeerIdentityStatus implements LockssSerializable {
  // The LCAP Identity of the peer
  private LcapIdentity lcapIdentity = null;

  // Timestamp of the most recently heard message from this peer.
  private long lastMessageTime = 0L;

  // The opcode of the most recently heard from this peer.
  // [JAVA15: Someday, this should be migrated to a typesafe enum]
  private int lastMessageOpCode = -1;
  
  // The number of polls this peer has participated in with us, in which
  // we were the poller, and they were the voter.
  private int totalVoterPolls = 0;
    
  // The number of polls this peer has participated in with us, in which
  // we were the VOTER, and they were the POLLER.
  private int totalPollerPolls = 0;
  
  // The number of poll requests this peer has rejected.
  private int totalRejectedPolls = 0;
  
  // The number of poll invitations we have sent to this peer.
  private long totalPollInvitations = 0;

  // The total number of messages exchanged with this peer.
  private long totalMessages = 0;

  // The time that the peer last participated with us as a poller.
  private long lastPollerTime;

  // The time that the peer last participated with us as a voter.
  private long lastVoterTime;
  
  // The last time that this peer rejected a poll from us.
  private long lastRejectionTime;
  
  // The last time that we tried to invite this peer into a poll.
  private long lastPollInvitationTime;
  
  // The PollNak code of the last rejection, if any.
  private PollNak lastPollNak;

  // Polling group(s) he last said he was in, and the last time he said
  // that
  private String groups;
  private long lastGroupTime;

  /**
   * Construct a new PeerIdentityStatus object.
   * 
   * @param lcapIdentity The LCAP Identity associated with this status object.
   */
  public PeerIdentityStatus(LcapIdentity lcapIdentity) {
    this.lcapIdentity = lcapIdentity;
  }
  
  /**
   * @return the LCAP identity for this peer.
   */
  public LcapIdentity getLcapIdentity() {
    return lcapIdentity;
  }

  /**
   * @return the PeerIdentity for this peer.
   */
  public PeerIdentity getPeerIdentity() {
    return lcapIdentity.getPeerIdentity();
  }

  /**
   * @param lcapIdentity the LCAP identity to set
   */
  public void setLcapIdentity(LcapIdentity lcapIdentity) {
    this.lcapIdentity = lcapIdentity;
  }

  /**
   * @return the time of the last message heard from this peer
   */
  public long getLastMessageTime() {
    return lastMessageTime;
  }

  /**
   * @param lastMessageTime the time of the last message heard from this peer.
   */
  public void setLastMessageTime(long lastMessageTime) {
    this.lastMessageTime = lastMessageTime;
  }

  /**
   * @return the opcode of the last message heard from this peer.
   */
  public int getLastMessageOpCode() {
    return lastMessageOpCode;
  }

  /**
   * @param lastMessageOpCode the type of the last message heard from this peer.
   */
  public void setLastMessageOpCode(int lastMessageOpCode) {
    this.lastMessageOpCode = lastMessageOpCode;
  }

  /**
   * @return the total number of polls in which this peer acted as a voter,
   * and the running daemon acted as the poller.
   */
  public int getTotalVoterPolls() {
    return totalVoterPolls;
  }

  /**
   * @param totalVoterPolls the total number of polls in which this peer
   * acted as a voter, and the running daemon acted as the poller.
   */
  public void setTotalVoterPolls(int totalVoterPolls) {
    this.totalVoterPolls = totalVoterPolls;
  }

  /**
   * @return the total number of polls in which this peer acted as the poller,
   * and the running daemon acted as the voter.
   */
  public int getTotalPollerPolls() {
    return totalPollerPolls;
  }

  /**
   * @param totalPollerPolls the total number of polls in which this peer
   * acted as the poller, and the running daemon acted as the voter.
   */
  public void setTotalPollerPolls(int totalPollerPolls) {
    this.totalPollerPolls = totalPollerPolls;
  }
  
  /**
   * @return The total number of poll requests rejected by this peer.
   */
  public int getTotalRejectedPolls() {
    return this.totalRejectedPolls;
  }
  
  /**
   * @param totalRejectedPolls The total number of poll requests rejected
   * by this peer.
   */
  public void setTotalRejectedPolls(int totalRejectedPolls) {
    this.totalRejectedPolls = totalRejectedPolls;
  }
  
  /**
   * @return The total number of polls this peer has been invited into.
   */
  public long getTotalPollInvitatioins() {
    return totalPollInvitations;
  }
  
  /**
   * @param totalInvitations The total number of polls this peer has been
   * invited into.
   */
  public void setTotalPollInvitations(long totalInvitations) {
    this.totalPollInvitations = totalInvitations;
  }

  /**
   * @return the total number of messages heard from this peer.
   */
  public long getTotalMessages() {
    return totalMessages;
  }

  /**
   * @param totalMessages the total number of messages heard from this peer.
   */
  public void setTotalMessages(long totalMessages) {
    this.totalMessages = totalMessages;
  }

  /**
   * @return the time the peer last acted as a poller.
   */
  public long getLastPollerTime() {
    return lastPollerTime;
  }

  /**
   * @param lastPollTime the time the peer last acted as a poller.
   */
  public void setLastPollerTime(long lastPollTime) {
    this.lastPollerTime = lastPollTime;
  }
  
  /**
   * @return the time the peer last acted as a voter.
   */
  public long getLastVoterTime() {
    return lastVoterTime;
  }

  /**
   * @param lastPollerTime the time the peer last acted as a voter.
   */
  public void setLastVoterTime(long lastVoteTime) {
    this.lastVoterTime = lastVoteTime;
  }
  
  /**
   * @return The last time that this peer rejected a poll request from us.
   */
  public long getLastRejectionTime() {
    return lastRejectionTime;
  }
  
  /**
   * @param lastRejectionTime The last time that this peer rejected a poll
   * request from us.
   */
  public void setLastRejectionTime(long lastRejectionTime) {
    this.lastRejectionTime = lastRejectionTime;
  }
  
  /**
   * @return The last time that we attempted to invite this peer into a poll.
   */
  public long getLastPollInvitationTime() {
    return lastPollInvitationTime;
  }

  /**
   * @return The last time that we attempted to invite this peer into a poll.
   */
  public void setLastPollInvitationTime(long pollInvitationTime) {
    this.lastPollInvitationTime = pollInvitationTime;
  }

  /**
   * @return The Poll NAK code for the last poll rejection.  Null if no
   * poll has ever been rejected.
   */
  public PollNak getLastPollNak() {
    return lastPollNak;
  }

  /**
   * @param lastPollNak The Poll NAK code for the last poll rejection.
   */
  public void setLastPollNak(PollNak lastPollNak) {
    this.lastPollNak = lastPollNak;
  }

  /**
   * @return The groups he last told us he's in
   */
  public List getGroups() {
    return StringUtil.breakAt(groups, Constants.GROUP_SEPARATOR);
  }

  /**
   * @param groups The groups he said he's in
   */
  public void setGroups(List groups) {
    this.groups = StringUtil.separatedString(groups,
					     Constants.GROUP_SEPARATOR);
  }

  /**
   * @return The last time he told us his groups
   */
  public long getLastGroupTime() {
    return lastGroupTime;
  }

  /**
   * @param lastGroupTime The last time he told us his groups
   */
  public void setLastGroupTime(long lastGroupTime) {
    this.lastGroupTime = lastGroupTime;
  }

  /**
   * Signal the receipt of a message from the given peer.  As a side effect,
   * the total number of messages, the last message opcode, and the last message
   * time are all updated.
   * 
   * @param msg The message received.
   */
  public void messageReceived(LcapMessage msg) {
    int msgOpCode = msg.getOpcode();
    totalMessages++;
    setLastMessageOpCode(msgOpCode);
    setLastMessageTime(TimeBase.nowMs());
    if (msg instanceof V3LcapMessage) {
      messageReceived((V3LcapMessage)msg);
    }
  }
  
  public void messageReceived(V3LcapMessage msg) {
    List groups = msg.getGroupList();
    if (groups != null) {
      setGroups(groups);
    }
  }
  
  /**
   * Signal that we have invited this peer to participate in a poll.
   */
  public void invitedPeer() {
    totalPollInvitations++;
    setLastPollInvitationTime(TimeBase.nowMs());
  }
  
  /**
   * Signal that this peer has participated in a poll with us as the poller.
   */
  public void calledPoll() {
    totalPollerPolls++;
    setLastPollerTime(TimeBase.nowMs());
  }
  
  /**
   * Signal that this peer has participated in a poll with us as the voter.
   */
  public void joinedPoll() {
    this.totalVoterPolls++;
    setLastVoterTime(TimeBase.nowMs());
  }
  
  /*
   * Signal that this peer has rejected a poll request from us.
   */
  public void rejectedPoll(PollNak reason) {
    this.totalRejectedPolls++;
    setLastPollNak(reason);
    setLastRejectionTime(TimeBase.nowMs());
  }

}
