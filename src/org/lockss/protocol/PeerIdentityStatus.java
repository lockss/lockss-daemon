/*
 * $Id: PeerIdentityStatus.java,v 1.1 2007-06-28 07:14:23 smorabito Exp $
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
  
  // The total number of messages exchanged with this peer.
  private long totalMessages = 0;

  // The time that the peer last participated with us as a poller.
  private long lastPollerTime;

  // The time that the peer last participated with us as a voter.
  private long lastVoterTime;

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
   * Signal the receipt of a message from the given peer.  As a side effect,
   * the total number of messages, the last message opcode, and the last message
   * time are all updated.
   * 
   * @param msg The message received.
   */
  public void messageReceived(LcapMessage msg) {
    messageReceived(msg.getOpcode());
  }

  /**
   * Signal the receipt of a message from the given peer.  As a side effect,
   * the total number of messages, the last message opcode, and the last message
   * time are all updated.
   * 
   * @param msgOpCode The opcode of the last message received.
   */
  public void messageReceived(int msgOpCode) {
    totalMessages++;
    setLastMessageOpCode(msgOpCode);
    setLastMessageTime(TimeBase.nowMs());
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

}
