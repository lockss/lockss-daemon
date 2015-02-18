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
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.poller.v3.ParticipantUserData;
import org.lockss.poller.v3.PollerStateBean;
import org.lockss.poller.v3.PollerStateBean.Repair;
import org.lockss.poller.v3.V3Poller;
import org.lockss.protocol.psm.PsmInterp;
import org.lockss.protocol.psm.PsmState;
import org.lockss.util.TimeBase;
import org.lockss.ws.entities.ParticipantWsResult;
import org.lockss.ws.entities.PollWsResult;
import org.lockss.ws.entities.RepairWsResult;

/**
 * Container for the information that is used as the source for a query related
 * to polls.
 */
public class PollWsSource extends PollWsResult {
  private V3Poller poll;

  private boolean auIdPopulated = false;
  private boolean auNamePopulated = false;
  private boolean participantCountPopulated = false;
  private boolean pollStatusPopulated = false;
  private boolean talliedUrlCountPopulated = false;
  private boolean talliedUrlsPopulated = false;
  private boolean hashErrorCountPopulated = false;
  private boolean errorUrlsPopulated = false;
  private boolean completedRepairCountPopulated = false;
  private boolean completedRepairsPopulated = false;
  private boolean percentAgreementPopulated = false;
  private boolean startTimePopulated = false;
  private boolean deadlinePopulated = false;
  private boolean pollKeyPopulated = false;
  private boolean pollVariantPopulated = false;
  private boolean errorDetailPopulated = false;
  private boolean additionalInfoPopulated = false;
  private boolean voteDeadlinePopulated = false;
  private boolean durationPopulated = false;
  private boolean remainingTimePopulated = false;
  private boolean endTimePopulated = false;
  private boolean agreedUrlCountPopulated = false;
  private boolean agreedUrlsPopulated = false;
  private boolean disagreedUrlCountPopulated = false;
  private boolean disagreedUrlsPopulated = false;
  private boolean noQuorumUrlCountPopulated = false;
  private boolean noQuorumUrlsPopulated = false;
  private boolean tooCloseUrlCountPopulated = false;
  private boolean tooCloseUrlsPopulated = false;
  private boolean activeRepairCountPopulated = false;
  private boolean activeRepairsPopulated = false;
  private boolean bytesHashedCountPopulated = false;
  private boolean bytesReadCountPopulated = false;
  private boolean quorumPopulated = false;
  private boolean participantsPopulated = false;

  private boolean isPoll;

  public PollWsSource(ArchivalUnit au) {
    setAuId(au.getAuId());
    auIdPopulated = true;

    setAuName(au.getName());
    auNamePopulated = true;

    participantCountPopulated = true;

    setPollStatus("Pending");
    pollStatusPopulated = true;

    talliedUrlCountPopulated = true;
    talliedUrlsPopulated = true;
    hashErrorCountPopulated = true;
    errorUrlsPopulated = true;
    completedRepairCountPopulated = true;
    completedRepairsPopulated = true;
    percentAgreementPopulated = true;
    startTimePopulated = true;
    deadlinePopulated = true;
    pollKeyPopulated = true;
    pollVariantPopulated = true;
    errorDetailPopulated = true;
    additionalInfoPopulated = true;
    voteDeadlinePopulated = true;
    durationPopulated = true;
    remainingTimePopulated = true;
    endTimePopulated = true;
    agreedUrlCountPopulated = true;
    agreedUrlsPopulated = true;
    disagreedUrlCountPopulated = true;
    disagreedUrlsPopulated = true;
    noQuorumUrlCountPopulated = true;
    noQuorumUrlsPopulated = true;
    tooCloseUrlCountPopulated = true;
    tooCloseUrlsPopulated = true;
    activeRepairCountPopulated = true;
    activeRepairsPopulated = true;
    bytesHashedCountPopulated = true;
    bytesReadCountPopulated = true;
    quorumPopulated = true;
    participantsPopulated = true;

    isPoll = false;
  }

  public PollWsSource(V3Poller poll) {
    this.poll = poll;
    isPoll = true;
  }

  @Override
  public String getAuId() {
    if (!auIdPopulated) {
      if (isPoll) {
	setAuId(poll.getAu().getAuId());
      }

      auIdPopulated = true;
    }

    return super.getAuId();
  }

  @Override
  public String getAuName() {
    if (!auNamePopulated) {
      if (isPoll) {
	setAuName(poll.getAu().getName());
      }

      auNamePopulated = true;
    }

    return super.getAuName();
  }

  @Override
  public Integer getParticipantCount() {
    if (!participantCountPopulated) {
      if (isPoll) {
	setParticipantCount(Integer.valueOf(poll.getPollSize()));
      }

      participantCountPopulated = true;
    }

    return super.getParticipantCount();
  }

  @Override
  public String getPollStatus() {
    if (!pollStatusPopulated) {
      if (isPoll) {
	setPollStatus(poll.getStatusString());
      }

      pollStatusPopulated = true;
    }

    return super.getPollStatus();
  }

  @Override
  public Integer getTalliedUrlCount() {
    if (!talliedUrlCountPopulated) {
      if (isPoll) {
	setTalliedUrlCount(Integer.valueOf(poll.getTalliedUrls().size()));
      }

      talliedUrlCountPopulated = true;
    }

    return super.getTalliedUrlCount();
  }

  @Override
  public List<String> getTalliedUrls() {
    if (!talliedUrlsPopulated) {
      if (isPoll) {
	setTalliedUrls((List<String>)poll.getTalliedUrls());
      }

      talliedUrlsPopulated = true;
    }

    return super.getTalliedUrls();
  }

  @Override
  public Integer getHashErrorCount() {
    if (!hashErrorCountPopulated) {
      if (isPoll) {
	if (poll.getErrorUrls() != null) {
	  setHashErrorCount(Integer.valueOf(poll.getErrorUrls().size()));
	} else {
	  setHashErrorCount(Integer.valueOf(0));
	}
      }

      hashErrorCountPopulated = true;
    }

    return super.getHashErrorCount();
  }

  @Override
  public Map<String, String> getErrorUrls() {
    if (!errorUrlsPopulated) {
      if (isPoll) {
	setErrorUrls(poll.getErrorUrls());
      }

      errorUrlsPopulated = true;
    }

    return super.getErrorUrls();
  }

  @Override
  public Integer getCompletedRepairCount() {
    if (!completedRepairCountPopulated) {
      if (isPoll) {
	setCompletedRepairCount(Integer
	    .valueOf(poll.getCompletedRepairs().size()));
      }

      completedRepairCountPopulated = true;
    }

    return super.getCompletedRepairCount();
  }

  @Override
  public List<RepairWsResult> getCompletedRepairs() {
    if (!completedRepairsPopulated) {
      if (isPoll) {
	List<RepairWsResult> results =
	    new ArrayList<RepairWsResult>(poll.getCompletedRepairs().size());

	for (PollerStateBean.Repair repair : poll.getCompletedRepairs()) {
	  RepairWsResult result = new RepairWsResult();
	  result.setUrl(repair.getUrl());

	  if (repair.getRepairFrom() != null) {
	    result.setPeerId(repair.getRepairFrom().getIdString());
	  }

	  results.add(result);
	}

	setCompletedRepairs(results);
      }

      completedRepairsPopulated = true;
    }

    return super.getCompletedRepairs();
  }

  @Override
  public Float getPercentAgreement() {
    if (!percentAgreementPopulated) {
      if (isPoll) {
	if (poll.getStatus() == V3Poller.POLLER_STATUS_COMPLETE) {
	  setPercentAgreement(poll.getPercentAgreement());
	}
      }

      percentAgreementPopulated = true;
    }

    return super.getPercentAgreement();
  }

  @Override
  public Long getStartTime() {
    if (!startTimePopulated) {
      if (isPoll) {
	setStartTime(Long.valueOf(poll.getCreateTime()));
      }

      startTimePopulated = true;
    }

    return super.getStartTime();
  }

  @Override
  public Long getDeadline() {
    if (!deadlinePopulated) {
      if (isPoll) {
	setDeadline(Long.valueOf(poll.getDeadline().getExpirationTime()));
      }

      deadlinePopulated = true;
    }

    return super.getDeadline();
  }

  @Override
  public String getPollKey() {
    if (!pollKeyPopulated) {
      if (isPoll) {
	setPollKey(poll.getKey());
      }

      pollKeyPopulated = true;
    }

    return super.getPollKey();
  }

  @Override
  public String getPollVariant() {
    if (!pollVariantPopulated) {
      if (isPoll) {
	setPollVariant(poll.getPollVariant().toString());
      }

      pollVariantPopulated = true;
    }

    return super.getPollVariant();
  }

  @Override
  public String getErrorDetail() {
    if (!errorDetailPopulated) {
      if (isPoll) {
	setErrorDetail(poll.getPollerStateBean().getErrorDetail());
      }

      errorDetailPopulated = true;
    }

    return super.getErrorDetail();
  }

  @Override
  public String getAdditionalInfo() {
    if (!additionalInfoPopulated) {
      if (isPoll) {
	setAdditionalInfo(poll.getPollerStateBean().getAdditionalInfo());
      }

      additionalInfoPopulated = true;
    }

    return super.getAdditionalInfo();
  }

  @Override
  public Long getVoteDeadline() {
    if (!voteDeadlinePopulated) {
      if (isPoll) {
	if (!poll.isLocalPoll()) {
	  setVoteDeadline(Long.valueOf(poll.getVoteDeadline()));
	}
      }

      voteDeadlinePopulated = true;
    }

    return super.getVoteDeadline();
  }

  @Override
  public Long getDuration() {
    if (!durationPopulated) {
      if (isPoll) {
	setDuration(Long.valueOf(poll.getDuration()));
      }

      durationPopulated = true;
    }

    return super.getDuration();
  }

  @Override
  public Long getRemainingTime() {
    if (!remainingTimePopulated) {
      if (isPoll) {
	if (poll.isPollActive()) {
	  long remain =
	      TimeBase.msUntil(poll.getDeadline().getExpirationTime());

	  if (remain >= 0) {
	    setRemainingTime(Long.valueOf(remain));
	  }
	}
      }

      remainingTimePopulated = true;
    }

    return super.getRemainingTime();
  }

  @Override
  public Long getEndTime() {
    if (!endTimePopulated) {
      if (isPoll) {
	if (!poll.isPollActive()
	    && !poll.getDeadline().equals(poll.getEndTime())) {
	  setEndTime(Long.valueOf(poll.getEndTime()));
	}
      }

      endTimePopulated = true;
    }

    return super.getEndTime();
  }

  @Override
  public Integer getAgreedUrlCount() {
    if (!agreedUrlCountPopulated) {
      if (isPoll) {
	int count = poll.getAgreedUrls().size();

	if (count > 0) {
	  setAgreedUrlCount(Integer.valueOf(count));
	}
      }

      agreedUrlCountPopulated = true;
    }

    return super.getAgreedUrlCount();
  }

  @Override
  public Set<String> getAgreedUrls() {
    if (!agreedUrlsPopulated) {
      if (isPoll) {
	setAgreedUrls((Set<String>)poll.getAgreedUrls());
      }

      agreedUrlsPopulated = true;
    }

    return super.getAgreedUrls();
  }

  @Override
  public Integer getDisagreedUrlCount() {
    if (!disagreedUrlCountPopulated) {
      if (isPoll) {
	int count = poll.getDisagreedUrls().size();

	if (count > 0) {
	  setDisagreedUrlCount(Integer.valueOf(count));
	}
      }

      disagreedUrlCountPopulated = true;
    }

    return super.getDisagreedUrlCount();
  }

  @Override
  public Set<String> getDisagreedUrls() {
    if (!disagreedUrlsPopulated) {
      if (isPoll) {
	setDisagreedUrls((Set<String>)poll.getDisagreedUrls());
      }

      disagreedUrlsPopulated = true;
    }

    return super.getDisagreedUrls();
  }

  @Override
  public Integer getNoQuorumUrlCount() {
    if (!noQuorumUrlCountPopulated) {
      if (isPoll) {
	int count = poll.getNoQuorumUrls().size();

	if (count > 0) {
	  setNoQuorumUrlCount(Integer.valueOf(count));
	}
      }

      noQuorumUrlCountPopulated = true;
    }

    return super.getNoQuorumUrlCount();
  }

  @Override
  public Set<String> getNoQuorumUrls() {
    if (!noQuorumUrlsPopulated) {
      if (isPoll) {
	setNoQuorumUrls((Set<String>)poll.getNoQuorumUrls());
      }

      noQuorumUrlsPopulated = true;
    }

    return super.getNoQuorumUrls();
  }

  @Override
  public Integer getTooCloseUrlCount() {
    if (!tooCloseUrlCountPopulated) {
      if (isPoll) {
	int count = poll.getTooCloseUrls().size();

	if (count > 0) {
	  setTooCloseUrlCount(Integer.valueOf(count));
	}
      }

      tooCloseUrlCountPopulated = true;
    }

    return super.getTooCloseUrlCount();
  }

  @Override
  public Set<String> getTooCloseUrls() {
    if (!tooCloseUrlsPopulated) {
      if (isPoll) {
	setTooCloseUrls((Set<String>)poll.getTooCloseUrls());
      }

      tooCloseUrlsPopulated = true;
    }

    return super.getTooCloseUrls();
  }

  @Override
  public Integer getActiveRepairCount() {
    if (!activeRepairCountPopulated) {
      if (isPoll) {
	int count = poll.getActiveRepairs().size();

	if (count > 0) {
	  setActiveRepairCount(Integer.valueOf(count));
	}
      }

      activeRepairCountPopulated = true;
    }

    return super.getActiveRepairCount();
  }

  @Override
  public List<RepairWsResult> getActiveRepairs() {
    if (!activeRepairsPopulated) {
      if (isPoll) {
	List<Repair> repairs = poll.getActiveRepairs();
	List<RepairWsResult> results =
	    new ArrayList<RepairWsResult>(repairs.size());

	for (Repair repair : repairs) {
	  RepairWsResult result = new RepairWsResult();
	  result.setUrl(repair.getUrl());
	  result.setPeerId(repair.getRepairFrom().getIdString());
	  results.add(result);
	}

	setActiveRepairs(results);
      }

      activeRepairsPopulated = true;
    }

    return super.getActiveRepairs();
  }

  @Override
  public Long getBytesHashedCount() {
    if (!bytesHashedCountPopulated) {
      if (isPoll) {
	if (poll.isEnableHashStats()) {
	  setBytesHashedCount(Long.valueOf(poll.getBytesHashed()));
	}
      }

      bytesHashedCountPopulated = true;
    }

    return super.getBytesHashedCount();
  }

  @Override
  public Long getBytesReadCount() {
    if (!bytesReadCountPopulated) {
      if (isPoll) {
	if (poll.isEnableHashStats()) {
	  setBytesReadCount(Long.valueOf(poll.getBytesRead()));
	}
      }

      bytesReadCountPopulated = true;
    }

    return super.getBytesReadCount();
  }

  @Override
  public Integer getQuorum() {
    if (!quorumPopulated) {
      if (isPoll) {
	setQuorum(poll.getQuorum());
      }

      quorumPopulated = true;
    }

    return super.getQuorum();
  }

  @Override
  public List<ParticipantWsResult> getParticipants() {
    if (!participantsPopulated) {
      if (isPoll) {
	List<ParticipantWsResult> results =
	    new ArrayList<ParticipantWsResult>();

	for (ParticipantUserData voter : poll.getParticipants()) {
	  results.add(createParticipant(voter, false));
	}

	for (ParticipantUserData voter : poll.getExParticipants()) {
	  results.add(createParticipant(voter, true));
	}

	setParticipants(results);
      }

      participantsPopulated = true;
    }

    return super.getParticipants();
  }

  private ParticipantWsResult createParticipant(ParticipantUserData voter,
      boolean isExParticipant) {
    ParticipantWsResult result = new ParticipantWsResult();

    result.setPeerId(voter.getVoterId().getIdString());
    result.setPeerStatus(voter.getStatusString());
    result.setHasVoted(Boolean.valueOf(voter.hasVoted()));

    if (voter.hasVoted()) {
      ParticipantUserData.VoteCounts voteCounts = voter.getVoteCounts();

      result.setPercentAgreement(Float
	  .valueOf(voteCounts.getPercentAgreement()));
      result.setAgreedVoteCount(Long.valueOf(voteCounts.getAgreedVotes()));
      result.setDisagreedVoteCount(Long
	  .valueOf(voteCounts.getDisagreedVotes()));
      result.setPollerOnlyVoteCount(Long
	  .valueOf(voteCounts.getPollerOnlyVotes()));
      result.setVoterOnlyVotecount(Long
	  .valueOf(voteCounts.getVoterOnlyVotes()));
      result.setBytesHashed(Long.valueOf(voter.getBytesHashed()));
      result.setBytesRead(Long.valueOf(voter.getBytesRead()));
    }

    PsmInterp interp = voter.getPsmInterp();

    if (interp != null) {
      PsmState state = interp.getCurrentState();

      if (state != null) {
	result.setCurrentState(state.getName());

	long when = interp.getLastStateChange();

	if (when > 0) {
	  result.setLastStateChange(Long.valueOf(when));
	}
      }
    }	

    result.setIsExParticipant(Boolean.valueOf(isExParticipant));

    return result;
  }
}
