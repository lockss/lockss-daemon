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

import org.lockss.poller.v3.V3Voter;
import org.lockss.poller.v3.VoterUserData;
import org.lockss.protocol.psm.PsmInterp;
import org.lockss.protocol.psm.PsmState;
import org.lockss.util.ByteArray;
import org.lockss.util.TimeBase;
import org.lockss.ws.entities.VoteWsResult;

/**
 * Container for the information that is used as the source for a query related
 * to votes.
 */
public class VoteWsSource extends VoteWsResult {
  private V3Voter vote;

  private boolean auIdPopulated = false;
  private boolean auNamePopulated = false;
  private boolean callerIdPopulated = false;
  private boolean voteStatusPopulated = false;
  private boolean startTimePopulated = false;
  private boolean deadlinePopulated = false;
  private boolean voteKeyPopulated = false;
  private boolean isPollActivePopulated = false;
  private boolean currentStatePopulated = false;
  private boolean errorDetailPopulated = false;
  private boolean voteDeadlinePopulated = false;
  private boolean durationPopulated = false;
  private boolean remainingTimePopulated = false;
  private boolean agreementHintPopulated = false;
  private boolean pollerNoncePopulated = false;
  private boolean voterNoncePopulated = false;
  private boolean voterNonce2Populated = false;
  private boolean isSymmetricPollPopulated = false;
  private boolean agreedUrlCountPopulated = false;
  private boolean disagreedUrlCountPopulated = false;
  private boolean pollerOnlyUrlCountPopulated = false;
  private boolean voterOnlyUrlCountPopulated = false;

  private VoterUserData voterUserData;
  private boolean voterUserDataPopulated = false;

  public VoteWsSource(V3Voter vote) {
    this.vote = vote;
  }

  @Override
  public String getAuId() {
    if (!auIdPopulated) {
      setAuId(vote.getAu().getAuId());

      auIdPopulated = true;
    }

    return super.getAuId();
  }

  @Override
  public String getAuName() {
    if (!auNamePopulated) {
      setAuName(vote.getAu().getName());

      auNamePopulated = true;
    }

    return super.getAuName();
  }

  @Override
  public String getCallerId() {
    if (!callerIdPopulated) {
      setCallerId(vote.getPollerId().getIdString());

      callerIdPopulated = true;
    }

    return super.getCallerId();
  }

  @Override
  public String getVoteStatus() {
    if (!voteStatusPopulated) {
      setVoteStatus(vote.getStatusString());

      voteStatusPopulated = true;
    }

    return super.getVoteStatus();
  }

  @Override
  public Long getStartTime() {
    if (!startTimePopulated) {
      setStartTime(Long.valueOf(vote.getCreateTime()));

      startTimePopulated = true;
    }

    return super.getStartTime();
  }

  @Override
  public Long getDeadline() {
    if (!deadlinePopulated) {
      setDeadline(Long.valueOf(vote.getDeadline().getExpirationTime()));

      deadlinePopulated = true;
    }

    return super.getDeadline();
  }

  @Override
  public String getVoteKey() {
    if (!voteKeyPopulated) {
	setVoteKey(vote.getKey());

      voteKeyPopulated = true;
    }

    return super.getVoteKey();
  }

  @Override
  public Boolean getIsPollActive() {
    if (!isPollActivePopulated) {
      setIsPollActive(Boolean.valueOf(vote.isPollActive()));

      isPollActivePopulated = true;
    }

    return super.getIsPollActive();
  }

  @Override
  public String getCurrentState() {
    if (!currentStatePopulated) {
      PsmInterp interp = vote.getPsmInterp();

      if (interp != null) {
	PsmState state = interp.getCurrentState();

	if (state != null) {
	  setCurrentState(state.getName());
	}
      }

      currentStatePopulated = true;
    }

    return super.getCurrentState();
  }

  @Override
  public String getErrorDetail() {
    if (!errorDetailPopulated) {
      setErrorDetail(getVoterUserData().getErrorDetail());

      errorDetailPopulated = true;
    }

    return super.getErrorDetail();
  }

  @Override
  public Long getVoteDeadline() {
    if (!voteDeadlinePopulated) {
      setVoteDeadline(Long.valueOf(vote.getVoteDeadline().getExpirationTime()));

      voteDeadlinePopulated = true;
    }

    return super.getVoteDeadline();
  }

  @Override
  public Long getDuration() {
    if (!durationPopulated) {
      setDuration(Long.valueOf(vote.getDuration()));

      durationPopulated = true;
    }

    return super.getDuration();
  }

  @Override
  public Long getRemainingTime() {
    if (!remainingTimePopulated) {
      long remain = TimeBase.msUntil(vote.getDeadline().getExpirationTime());

      if (remain >= 0) {
	setRemainingTime(Long.valueOf(remain));
      }

      remainingTimePopulated = true;
    }

    return super.getRemainingTime();
  }

  @Override
  public Double getAgreementHint() {
    if (!agreementHintPopulated) {
      if (vote.getStatus() == V3Voter.STATUS_COMPLETE
	  && getVoterUserData().hasReceivedHint()) {
	setAgreementHint(voterUserData.getAgreementHint());
      }

      agreementHintPopulated = true;
    }

    return super.getAgreementHint();
  }

  @Override
  public String getPollerNonce() {
    if (!pollerNoncePopulated) {
      setPollerNonce(ByteArray.toBase64(vote.getPollerNonce()));

      pollerNoncePopulated = true;
    }

    return super.getPollerNonce();
  }

  @Override
  public String getVoterNonce() {
    if (!voterNoncePopulated) {
      setVoterNonce(ByteArray.toBase64(vote.getVoterNonce()));

      voterNoncePopulated = true;
    }

    return super.getVoterNonce();
  }

  @Override
  public String getVoterNonce2() {
    if (!voterNonce2Populated) {
      if (getIsSymmetricPoll().booleanValue()) {
	setVoterNonce2(ByteArray.toBase64(vote.getVoterNonce()));
      }

      voterNonce2Populated = true;
    }

    return super.getVoterNonce2();
  }

  @Override
  public Boolean getIsSymmetricPoll() {
    if (!isSymmetricPollPopulated) {
      setIsSymmetricPoll(Boolean.valueOf(getVoterUserData().isSymmetricPoll()));

      isSymmetricPollPopulated = true;
    }

    return super.getIsSymmetricPoll();
  }

  @Override
  public Integer getAgreedUrlCount() {
    if (!agreedUrlCountPopulated) {
      if (getIsSymmetricPoll().booleanValue()
	  && vote.getStatus() == V3Voter.STATUS_COMPLETE) {
	setAgreedUrlCount(Integer.valueOf(getVoterUserData().getNumAgreeUrl()));
      }

      agreedUrlCountPopulated = true;
    }

    return super.getAgreedUrlCount();
  }

  @Override
  public Integer getDisagreedUrlCount() {
    if (!disagreedUrlCountPopulated) {
      if (getIsSymmetricPoll().booleanValue()
	  && vote.getStatus() == V3Voter.STATUS_COMPLETE) {
	setDisagreedUrlCount(Integer
	    .valueOf(getVoterUserData().getNumDisagreeUrl()));
      }

      disagreedUrlCountPopulated = true;
    }

    return super.getDisagreedUrlCount();
  }

  @Override
  public Integer getPollerOnlyUrlCount() {
    if (!pollerOnlyUrlCountPopulated) {
      if (getIsSymmetricPoll().booleanValue()
	  && vote.getStatus() == V3Voter.STATUS_COMPLETE) {
	setPollerOnlyUrlCount(Integer
	    .valueOf(getVoterUserData().getNumPollerOnlyUrl()));
      }

      pollerOnlyUrlCountPopulated = true;
    }

    return super.getPollerOnlyUrlCount();
  }

  @Override
  public Integer getVoterOnlyUrlCount() {
    if (!voterOnlyUrlCountPopulated) {
      if (getIsSymmetricPoll().booleanValue()
	  && vote.getStatus() == V3Voter.STATUS_COMPLETE) {
	setVoterOnlyUrlCount(Integer
	    .valueOf(getVoterUserData().getNumVoterOnlyUrl()));
      }

      voterOnlyUrlCountPopulated = true;
    }

    return super.getVoterOnlyUrlCount();
  }

  private VoterUserData getVoterUserData() {
    if (!voterUserDataPopulated) {
      voterUserData = vote.getVoterUserData();

      voterUserDataPopulated = true;
    }

    return voterUserData;
  }
}
