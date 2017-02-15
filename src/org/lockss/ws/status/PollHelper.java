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
import org.lockss.plugin.ArchivalUnit;
import org.lockss.poller.PollManager;
import org.lockss.poller.v3.V3Poller;
import org.lockss.util.Logger;
import org.lockss.ws.entities.PollWsResult;

/**
 * Helper of the DaemonStatus web service implementation of poll queries.
 */
public class PollHelper {
  /**
   * The fully-qualified name of the class of the objects used as source in a
   * query.
   */
  static String SOURCE_FQCN = PollWsSource.class.getCanonicalName();

  /**
   * The fully-qualified name of the class of the objects returned by the query.
   */
  static String RESULT_FQCN = PollWsResult.class.getCanonicalName();

  //
  // Property names used in poll queries.
  //
  static String AU_ID = "auId";
  static String AU_NAME = "auName";
  static String PARTICIPANT_COUNT = "participantCount";
  static String POLL_STATUS = "pollStatus";
  static String TALLIED_URL_COUNT = "talliedUrlCount";
  static String TALLIED_URLS = "talliedUrls";
  static String HASH_ERROR_COUNT = "hashErrorCount";
  static String ERROR_URLS = "errorUrls";
  static String COMPLETED_REPAIR_COUNT = "completedRepairCount";
  static String COMPLETED_REPAIRS = "completedRepairs";
  static String PERCENTAGE_AGREEMENT = "percentAgreement";
  static String START_TIME = "startTime";
  static String DEADLINE = "deadline";
  static String POLL_KEY = "pollKey";
  static String POLL_VARIANT = "pollVariant";
  static String ERROR_DETAIL = "errorDetail";
  static String ADDITIONAL_INFO = "additionalInfo";
  static String VOTE_DEADLINE = "voteDeadline";
  static String DURATION = "duration";
  static String REMAINING_TIME = "remainingTime";
  static String END_TIME = "endTime";
  static String AGREED_URL_COUNT = "agreedUrlCount";
  static String AGREED_URLS = "agreedUrls";
  static String DISAGREED_URL_COUNT = "disagreedUrlCount";
  static String DISAGREED_URLS = "disagreedUrls";
  static String NO_QUORUM_URL_COUNT = "noQuorumUrlCount";
  static String NO_QUORUM_URLS = "noQuorumUrls";
  static String TOO_CLOSE_URL_COUNT = "tooCloseUrlCount";
  static String TOO_CLOSE_URLS = "tooCloseUrls";
  static String ACTIVE_REPAIR_COUNT = "activeRepairCount";
  static String ACTIVE_REPAIRS = "activeRepairs";
  static String BYTES_HASHED_COUNT = "bytesHashedCount";
  static String BYTES_READ_COUNT = "bytesReadCount";
  static String QUORUM = "quorum";
  static String PARTICIPANTS = "participants";

  /**
   * All the property names used in poll queries.
   */
  @SuppressWarnings("serial")
  static final Set<String> PROPERTY_NAMES = new HashSet<String>() {
    {
      add(AU_ID);
      add(AU_NAME);
      add(PARTICIPANT_COUNT);
      add(POLL_STATUS);
      add(TALLIED_URL_COUNT);
      add(TALLIED_URLS);
      add(HASH_ERROR_COUNT);
      add(ERROR_URLS);
      add(COMPLETED_REPAIR_COUNT);
      add(COMPLETED_REPAIRS);
      add(PERCENTAGE_AGREEMENT);
      add(START_TIME);
      add(DEADLINE);
      add(POLL_KEY);
      add(POLL_VARIANT);
      add(ERROR_DETAIL);
      add(ADDITIONAL_INFO);
      add(VOTE_DEADLINE);
      add(DURATION);
      add(REMAINING_TIME);
      add(END_TIME);
      add(AGREED_URL_COUNT);
      add(AGREED_URLS);
      add(DISAGREED_URL_COUNT);
      add(DISAGREED_URLS);
      add(NO_QUORUM_URL_COUNT);
      add(NO_QUORUM_URLS);
      add(TOO_CLOSE_URL_COUNT);
      add(TOO_CLOSE_URLS);
      add(ACTIVE_REPAIR_COUNT);
      add(ACTIVE_REPAIRS);
      add(BYTES_HASHED_COUNT);
      add(BYTES_READ_COUNT);
      add(QUORUM);
      add(PARTICIPANTS);
    }
  };

  private static Logger log = Logger.getLogger(PollHelper.class);

  /**
   * Provides the universe of poll-related objects used as the source for a
   * query.
   * 
   * @return a List<PollWsProxy> with the universe.
   */
  List<PollWsSource> createUniverse() {
    final String DEBUG_HEADER = "createUniverse(): ";

    // Get the poll manager.
    PollManager pollManager =
	(PollManager)LockssDaemon.getManager(LockssDaemon.POLL_MANAGER);

    // Get all the polls.
    Collection<V3Poller> allPolls = pollManager.getV3Pollers();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "allPolls.size() = " + allPolls.size());

    // Initialize the universe.
    List<PollWsSource> universe = new ArrayList<PollWsSource>(allPolls.size());

    // Loop through all the polls.
    for (V3Poller poll : allPolls) {
      // Add the object initialized with this poll to the universe of objects.
      universe.add(new PollWsSource(poll));
    }

    // Loop through all the Archival Units in the queue to poll.
    for (ArchivalUnit au : pollManager.getPendingQueueAus()) {
      // Add the object initialized with this poll to the universe of objects.
      universe.add(new PollWsSource(au));

    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "universe.size() = " + universe.size());
    return universe;
  }

  /**
   * Provides a printable copy of a collection of poll-related query results.
   * 
   * @param results
   *          A Collection<PollWsResult> with the query results.
   * @return a String with the requested printable copy.
   */
  String nonDefaultToString(Collection<PollWsResult> results) {
    StringBuilder builder = new StringBuilder("[");
    boolean isFirst = true;

    // Loop through through all the results in the collection.
    for (PollWsResult result : results) {
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
   * Provides a printable copy of a poll-related query result.
   * 
   * @param result
   *          A PollWsResult with the query result.
   * @return a String with the requested printable copy.
   */
  private String nonDefaultToString(PollWsResult result) {
    StringBuilder builder = new StringBuilder("PollWsResult [");
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

    if (result.getParticipantCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("participantCount=").append(result.getParticipantCount());
    }

    if (result.getPollStatus() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("pollStatus=").append(result.getPollStatus());
    }

    if (result.getTalliedUrlCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("talliedUrlCount=").append(result.getTalliedUrlCount());
    }

    if (result.getTalliedUrls() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("talliedUrls=").append(result.getTalliedUrls());
    }

    if (result.getHashErrorCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("hashErrorCount=").append(result.getHashErrorCount());
    }

    if (result.getErrorUrls() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("errorUrls=").append(result.getErrorUrls());
    }

    if (result.getCompletedRepairCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("completedRepairCount=")
      .append(result.getCompletedRepairCount());
    }

    if (result.getCompletedRepairs() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("completedRepairs=").append(result.getCompletedRepairs());
    }

    if (result.getPercentAgreement() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("percentAgreement=").append(result.getPercentAgreement());
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

    if (result.getPollKey() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("pollKey=").append(result.getPollKey());
    }

    if (result.getPollVariant() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("pollVariant=").append(result.getPollVariant());
    }

    if (result.getErrorDetail() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("errorDetail=").append(result.getErrorDetail());
    }

    if (result.getAdditionalInfo() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("additionalInfo=").append(result.getAdditionalInfo());
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

    if (result.getEndTime() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("endTime=").append(result.getEndTime());
    }

    if (result.getAgreedUrlCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("agreedUrlCount=").append(result.getAgreedUrlCount());
    }

    if (result.getAgreedUrls() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("agreedUrls=").append(result.getAgreedUrls());
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

    if (result.getDisagreedUrls() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("disagreedUrls=").append(result.getDisagreedUrls());
    }

    if (result.getNoQuorumUrlCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("noQuorumUrlCount=").append(result.getNoQuorumUrlCount());
    }

    if (result.getNoQuorumUrls() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("noQuorumUrls=").append(result.getNoQuorumUrls());
    }

    if (result.getTooCloseUrlCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("tooCloseUrlCount=").append(result.getTooCloseUrlCount());
    }

    if (result.getTooCloseUrls() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("tooCloseUrls=").append(result.getTooCloseUrls());
    }

    if (result.getActiveRepairCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("activeRepairCount=")
      .append(result.getActiveRepairCount());
    }

    if (result.getActiveRepairs() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("activeRepairs=").append(result.getActiveRepairs());
    }

    if (result.getBytesHashedCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("bytesHashedCount=").append(result.getBytesHashedCount());
    }

    if (result.getBytesReadCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("bytesReadCount=").append(result.getBytesReadCount());
    }

    if (result.getQuorum() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("quorum=").append(result.getQuorum());
    }

    if (result.getParticipants() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("participants=").append(result.getParticipants());
    }

    return builder.append("]").toString();
  }
}
