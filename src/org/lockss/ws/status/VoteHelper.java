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

package org.lockss.ws.status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.lockss.app.LockssDaemon;
import org.lockss.poller.PollManager;
import org.lockss.poller.v3.V3Voter;
import org.lockss.util.Logger;
import org.lockss.ws.entities.VoteWsResult;

/**
 * Helper of the DaemonStatus web service implementation of vote queries.
 */
public class VoteHelper {
  /**
   * The fully-qualified name of the class of the objects used as source in a
   * query.
   */
  static String SOURCE_FQCN = VoteWsSource.class.getCanonicalName();

  /**
   * The fully-qualified name of the class of the objects returned by the query.
   */
  static String RESULT_FQCN = VoteWsResult.class.getCanonicalName();

  //
  // Property names used in vote queries.
  //
  static String AU_ID = "auId";
  static String AU_NAME = "auName";
  static String CALLER_ID = "callerId";
  static String VOTE_STATUS = "voteStatus";
  static String START_TIME = "startTime";
  static String DEADLINE = "deadline";
  static String VOTE_KEY = "voteKey";
  static String IS_POLL_ACTIVE = "isPollActive";
  static String CURRENT_STATE = "currentState";
  static String ERROR_DETAIL = "errorDetail";
  static String VOTE_DEADLINE = "voteDeadline";
  static String DURATION = "duration";
  static String REMAINING_TIME = "remainingTime";
  static String AGREEMENT_HINT = "agreementHint";
  static String POLLER_NONCE = "pollerNonce";
  static String VOTER_NONCE = "voterNonce";
  static String VOTER_NONCE_2 = "voterNonce2";
  static String IS_SYMMETRIC_POLL = "isSymmetricPoll";
  static String AGREED_URL_COUNT = "agreedUrlCount";
  static String DISAGREED_URL_COUNT = "disagreedUrlCount";
  static String POLLER_ONLY_URL_COUNT = "pollerOnlyUrlCount";
  static String VOTER_ONLY_URL_COUNT = "voterOnlyUrlCount";

  /**
   * All the property names used in vote queries.
   */
  @SuppressWarnings("serial")
  static final Set<String> PROPERTY_NAMES = new HashSet<String>() {
    {
      add(AU_ID);
      add(AU_NAME);
      add(CALLER_ID);
      add(VOTE_STATUS);
      add(START_TIME);
      add(DEADLINE);
      add(VOTE_KEY);
      add(IS_POLL_ACTIVE);
      add(CURRENT_STATE);
      add(ERROR_DETAIL);
      add(VOTE_DEADLINE);
      add(DURATION);
      add(REMAINING_TIME);
      add(AGREEMENT_HINT);
      add(POLLER_NONCE);
      add(VOTER_NONCE);
      add(VOTER_NONCE_2);
      add(IS_SYMMETRIC_POLL);
      add(AGREED_URL_COUNT);
      add(DISAGREED_URL_COUNT);
      add(POLLER_ONLY_URL_COUNT);
      add(VOTER_ONLY_URL_COUNT);
    }
  };

  private static Logger log = Logger.getLogger(VoteHelper.class);

  /**
   * Provides the universe of vote-related objects used as the source for a
   * query.
   * 
   * @return a List<VoteWsProxy> with the universe.
   */
  List<VoteWsSource> createUniverse() {
    final String DEBUG_HEADER = "createUniverse(): ";

    // Get the poll manager.
    PollManager pollManager =
	(PollManager)LockssDaemon.getManager(LockssDaemon.POLL_MANAGER);

    // Get all the polls.
    Collection<V3Voter> allVotes = pollManager.getV3Voters();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "allVotes.size() = " + allVotes.size());

    // Initialize the universe.
    List<VoteWsSource> universe = new ArrayList<VoteWsSource>(allVotes.size());

    // Loop through all the votes.
    for (V3Voter vote : allVotes) {
      // Add the object initialized with this vote to the universe of objects.
      universe.add(new VoteWsSource(vote));
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "universe.size() = " + universe.size());
    return universe;
  }

  /**
   * Provides a printable copy of a collection of vote-related query results.
   * 
   * @param results
   *          A Collection<VoteWsResult> with the query results.
   * @return a String with the requested printable copy.
   */
  String nonDefaultToString(Collection<VoteWsResult> results) {
    StringBuilder builder = new StringBuilder("[");
    boolean isFirst = true;

    // Loop through through all the results in the collection.
    for (VoteWsResult result : results) {
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
   * Provides a printable copy of a vote-related query result.
   * 
   * @param result
   *          A VoteWsResult with the query result.
   * @return a String with the requested printable copy.
   */
  private String nonDefaultToString(VoteWsResult result) {
    StringBuilder builder = new StringBuilder("VoteWsResult [");
    boolean isFirst = true;

    if (result.getAuId() != null) {
      builder.append("auId=").append(result.getAuId());
      isFirst = false;
    }

    if (result.getAuName() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("auName=").append(result.getAuName());
    }

    if (result.getCallerId() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("callerId=").append(result.getCallerId());
    }

    if (result.getVoteStatus() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("voteStatus=").append(result.getVoteStatus());
    }

    if (result.getStartTime() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("startTime=").append(result.getStartTime());
    }

    if (result.getDeadline() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("deadline=").append(result.getDeadline());
    }

    if (result.getVoteKey() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("voteKey=").append(result.getVoteKey());
    }

    if (result.getIsPollActive() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("isPollActive=").append(result.getIsPollActive());
    }

    if (result.getCurrentState() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("currentState=").append(result.getCurrentState());
    }

    if (result.getErrorDetail() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("errorDetail=").append(result.getErrorDetail());
    }

    if (result.getVoteDeadline() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("voteDeadline=").append(result.getVoteDeadline());
    }

    if (result.getDuration() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("duration=").append(result.getDuration());
    }

    if (result.getRemainingTime() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("remainingTime=").append(result.getRemainingTime());
    }

    if (result.getAgreementHint() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("agreementHint=").append(result.getAgreementHint());
    }

    if (result.getPollerNonce() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("pollerNonce=").append(result.getPollerNonce());
    }

    if (result.getVoterNonce() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("voterNonce=").append(result.getVoterNonce());
    }

    if (result.getVoterNonce2() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("voterNonce2=").append(result.getVoterNonce2());
    }

    if (result.getIsSymmetricPoll() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("isSymmetricPoll=").append(result.getIsSymmetricPoll());
    }

    if (result.getAgreedUrlCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("agreedUrlCount=").append(result.getAgreedUrlCount());
    }

    if (result.getDisagreedUrlCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("disagreedUrlCount=")
      .append(result.getDisagreedUrlCount());
    }

    if (result.getPollerOnlyUrlCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("pollerOnlyUrlCount=")
      .append(result.getPollerOnlyUrlCount());
    }

    if (result.getVoterOnlyUrlCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("voterOnlyUrlCount=")
      .append(result.getVoterOnlyUrlCount());
    }

    return builder.append("]").toString();
  }

}
