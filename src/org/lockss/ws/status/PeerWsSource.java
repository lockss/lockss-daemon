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

/**
 * Container for the information that is used as the source for a query related
 * to peers.
 */
package org.lockss.ws.status;

import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.lockss.config.ConfigManager;
import org.lockss.protocol.PeerIdentityStatus;
import org.lockss.protocol.V3LcapMessage;
import org.lockss.protocol.V3LcapMessage.PollNak;
import org.lockss.ws.entities.PeerWsResult;

public class PeerWsSource extends PeerWsResult {
  private PeerIdentityStatus piStatus;

  private boolean peerIdPopulated = false;
  private boolean lastMessagePopulated = false;
  private boolean messageTypePopulated = false;
  private boolean messageCountPopulated = false;
  private boolean lastPollPopulated = false;
  private boolean lastVotePopulated = false;
  private boolean lastInvitationPopulated = false;
  private boolean invitationCountPopulated = false;
  private boolean pollsCalledPopulated = false;
  private boolean votesCastPopulated = false;
  private boolean pollsRejectedPopulated = false;
  private boolean nakReasonPopulated = false;
  private boolean groupsPopulated = false;
  private boolean platformGroupMatchPopulated = false;

  private static List<String> platformGroups =
      (List<String>)ConfigManager.getPlatformGroupList();

  public PeerWsSource(PeerIdentityStatus piStatus) {
    this.piStatus = piStatus;
  }

  @Override
  public String getPeerId() {
    if (!peerIdPopulated) {
      setPeerId(piStatus.getPeerIdentity().getIdString());
      peerIdPopulated = true;
    }

    return super.getPeerId();
  }

  @Override
  public Long getLastMessage() {
    if (!lastMessagePopulated) {
      setLastMessage(Long.valueOf(piStatus.getLastMessageTime()));
      lastMessagePopulated = true;
    }

    return super.getLastMessage();
  }

  @Override
  public String getMessageType() {
    if (!messageTypePopulated) {
      int messageOpCode = piStatus.getLastMessageOpCode();

      if (messageOpCode >= V3LcapMessage.POLL_MESSAGES_BASE && messageOpCode
	  < (V3LcapMessage.POLL_MESSAGES.length
	      + V3LcapMessage.POLL_MESSAGES_BASE)) {
	setMessageType(V3LcapMessage
	    .POLL_MESSAGES[messageOpCode - V3LcapMessage.POLL_MESSAGES_BASE]
		+ " (" + messageOpCode + ")");
      } else {
	setMessageType("n/a");
      }

      messageTypePopulated = true;
    }

    return super.getMessageType();
  }

  @Override
  public Long getMessageCount() {
    if (!messageCountPopulated) {
      setMessageCount(Long.valueOf(piStatus.getTotalMessages()));
      messageCountPopulated = true;
    }

    return super.getMessageCount();
  }

  @Override
  public Long getLastPoll() {
    if (!lastPollPopulated) {
      setLastPoll(Long.valueOf(piStatus.getLastPollerTime()));
      lastPollPopulated = true;
    }

    return super.getLastPoll();
  }

  @Override
  public Long getLastVote() {
    if (!lastVotePopulated) {
      setLastVote(Long.valueOf(piStatus.getLastVoterTime()));
      lastVotePopulated = true;
    }

    return super.getLastVote();
  }

  @Override
  public Long getLastInvitation() {
    if (!lastInvitationPopulated) {
      setLastInvitation(Long.valueOf(piStatus.getLastPollInvitationTime()));
      lastInvitationPopulated = true;
    }

    return super.getLastInvitation();
  }

  @Override
  public Long getInvitationCount() {
    if (!invitationCountPopulated) {
      setInvitationCount(Long.valueOf(piStatus.getTotalPollInvitatioins()));
      invitationCountPopulated = true;
    }

    return super.getInvitationCount();
  }

  @Override
  public Long getPollsCalled() {
    if (!pollsCalledPopulated) {
      setPollsCalled(Long.valueOf(piStatus.getTotalPollerPolls()));
      pollsCalledPopulated = true;
    }

    return super.getPollsCalled();
  }

  @Override
  public Long getVotesCast() {
    if (!votesCastPopulated) {
      setVotesCast(Long.valueOf(piStatus.getTotalVoterPolls()));
      votesCastPopulated = true;
    }

    return super.getVotesCast();
  }

  @Override
  public Long getPollsRejected() {
    if (!pollsRejectedPopulated) {
      setPollsRejected(Long.valueOf(piStatus.getTotalRejectedPolls()));
      pollsRejectedPopulated = true;
    }

    return super.getPollsRejected();
  }

  @Override
  public String getNakReason() {
    if (!nakReasonPopulated) {
      PollNak pollNak = piStatus.getLastPollNak();

      if (pollNak != null) {
	setNakReason(pollNak.toString());
      }

      nakReasonPopulated = true;
    }

    return super.getNakReason();
  }

  @Override
  public List<String> getGroups() {
    if (!groupsPopulated) {
      setGroups((List<String>)piStatus.getGroups());
      groupsPopulated = true;
    }

    return super.getGroups();
  }

  @Override
  public Boolean getPlatformGroupMatch() {
    if (!platformGroupMatchPopulated) {
      List<String> peerGroups = getGroups();
      boolean match = peerGroups == null || peerGroups.isEmpty()
	  || CollectionUtils.containsAny(platformGroups, peerGroups);
      setPlatformGroupMatch(Boolean.valueOf(match));

      platformGroupMatchPopulated = true;
    }

    return super.getPlatformGroupMatch();
  }
}
