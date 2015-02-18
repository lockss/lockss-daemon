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
 * Helper of the DaemonStatus web service implementation of peer queries.
 */
package org.lockss.ws.status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.lockss.app.LockssDaemon;
import org.lockss.poller.Poll;
import org.lockss.protocol.IdentityManager;
import org.lockss.protocol.PeerIdentity;
import org.lockss.protocol.PeerIdentityStatus;
import org.lockss.util.Logger;
import org.lockss.ws.entities.PeerWsResult;

public class PeerHelper {
  /**
   * The fully-qualified name of the class of the objects used as source in a
   * query.
   */
  static String SOURCE_FQCN = PeerWsSource.class.getCanonicalName();

  /**
   * The fully-qualified name of the class of the objects returned by the query.
   */
  static String RESULT_FQCN = PeerWsResult.class.getCanonicalName();

  //
  // Property names used in peer queries.
  //
  static String PEER_ID = "peerId";
  static String LAST_MESSAGE = "lastMessage";
  static String MESSAGE_TYPE = "messageType";
  static String MESSAGE_COUNT = "messageCount";
  static String LAST_POLL = "lastPoll";
  static String LAST_VOTE = "lastVote";
  static String LAST_INVITATION = "lastInvitation";
  static String INVITATION_COUNT = "invitationCount";
  static String POLLS_CALLED = "pollsCalled";
  static String VOTES_CAST = "votesCast";
  static String POLLS_REJECTED = "pollsRejected";
  static String NAK_REASON = "nakReason";
  static String GROUPS = "groups";
  static String PLATFORM_GROUP_MATCH = "platformGroupMatch";

  /**
   * All the property names used in peer queries.
   */
  @SuppressWarnings("serial")
  static final Set<String> PROPERTY_NAMES = new HashSet<String>() {
    {
      add(PEER_ID);
      add(LAST_MESSAGE);
      add(MESSAGE_TYPE);
      add(MESSAGE_COUNT);
      add(LAST_POLL);
      add(LAST_VOTE);
      add(LAST_INVITATION);
      add(INVITATION_COUNT);
      add(POLLS_CALLED);
      add(VOTES_CAST);
      add(POLLS_REJECTED);
      add(NAK_REASON);
      add(GROUPS);
      add(PLATFORM_GROUP_MATCH);
    }
  };

  private static Logger log = Logger.getLogger(PeerHelper.class);

  /**
   * Provides the universe of peer-related objects used as the source for a
   * query.
   * 
   * @return a List<PeerWsProxy> with the universe.
   */
  List<PeerWsSource> createUniverse() {
    final String DEBUG_HEADER = "createUniverse(): ";
    
    // Get identity manager.
    IdentityManager idMgr =
	(IdentityManager)LockssDaemon.getManager(LockssDaemon.IDENTITY_MANAGER);

    boolean includeV1 = false;

    try {
      includeV1 = (idMgr.getLocalPeerIdentity(Poll.V1_PROTOCOL) != null);
    } catch (IllegalArgumentException e) {
      // Ignore.
    }

    // Get all the peers.
    List<PeerIdentityStatus> allPeers = ((IdentityManager)LockssDaemon
	.getManager(LockssDaemon.IDENTITY_MANAGER)).getPeerIdentityStatusList();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "allPeers.size() = " + allPeers.size());

    // Initialize the universe.
    List<PeerWsSource> universe = new ArrayList<PeerWsSource>(allPeers.size());

    // Loop through all the peers.
    for (PeerIdentityStatus status : allPeers) {
      PeerIdentity pid = status.getPeerIdentity();

      if (!pid.isLocalIdentity() && (includeV1 || pid.isV3())) {
	// Add the object initialized with this peer to the universe of objects.
	universe.add(new PeerWsSource(status));
      }
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "universe.size() = " + universe.size());
    return universe;
  }

  /**
   * Provides a printable copy of a collection of peer-related query results.
   * 
   * @param results
   *          A Collection<PeerWsResult> with the query results.
   * @return a String with the requested printable copy.
   */
  String nonDefaultToString(Collection<PeerWsResult> results) {
    StringBuilder builder = new StringBuilder("[");
    boolean isFirst = true;

    // Loop through through all the results in the collection.
    for (PeerWsResult result : results) {
      // Handle the first result differently.
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append(nonDefaultToString(result));
    }

    // Add this result to the printable copy.
    return builder.append("]").toString();
  }

  /**
   * Provides a printable copy of a peer-related query result.
   * 
   * @param result
   *          A PeerWsResult with the query result.
   * @return a String with the requested printable copy.
   */
  private String nonDefaultToString(PeerWsResult result) {
    StringBuilder builder = new StringBuilder("PeerWsResult [");
    boolean isFirst = true;

    if (result.getPeerId() != null) {
      builder.append("peerId=").append(result.getPeerId());
      isFirst = false;
    }

    if (result.getLastMessage() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("lastMessage=").append(result.getLastMessage());
    }

    if (result.getMessageType() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("messageType=").append(result.getMessageType());
    }

    if (result.getMessageCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("messageCount=").append(result.getMessageCount());
    }

    if (result.getLastPoll() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("lastPoll=").append(result.getLastPoll());
    }

    if (result.getLastVote() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("lastVote=").append(result.getLastVote());
    }

    if (result.getLastInvitation() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("lastInvitation=").append(result.getLastInvitation());
    }

    if (result.getInvitationCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("invitationCount=").append(result.getInvitationCount());
    }

    if (result.getPollsCalled() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("pollsCalled=").append(result.getPollsCalled());
    }

    if (result.getVotesCast() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("votesCast=").append(result.getVotesCast());
    }

    if (result.getPollsRejected() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("pollsRejected=").append(result.getPollsRejected());
    }

    if (result.getNakReason() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("nakReason=").append(result.getNakReason());
    }

    if (result.getGroups() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("groups=").append(result.getGroups());
    }

    if (result.getPlatformGroupMatch() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("platformGroupMatch=")
      .append(result.getPlatformGroupMatch());
    }

    return builder.append("]").toString();
  }
}
