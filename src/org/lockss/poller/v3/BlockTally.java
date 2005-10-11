/*
 * $Id: BlockTally.java,v 1.4 2005-10-11 05:45:39 tlipkis Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.poller.v3;

import java.util.*;

import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.util.*;

/**
 * Representation of the tally for an individual vote block.
 */
public class BlockTally extends PollTally {
  public static final int RESULT_NEED_MORE_BLOCKS = 10;

  private List agreeVoters;
  private List disagreeVoters;
  private List needBlocksFrom;

  private V3Poller poll;

  public BlockTally(V3Poller owner, long startTime, long duration,
                    int wtAgree, int wtDisagree, int quorum,
                    String hashAlgorithm) {
    super(Poll.V3_POLL, startTime, duration, 0, 0, wtAgree, wtDisagree,
          quorum, hashAlgorithm);
    this.poll = owner;
    this.pollSpec = owner.getPollSpec();
    this.needBlocksFrom = new ArrayList();
  }

  // XXX: Refactor Tally into Status and Tally objects.
  public int getErr() {
    return 0;
  }

  // XXX: Refactor Tally into Status and Tally objects.
  public String getErrString() {
    return "N/A";
  }

  public String getStatusString() {
    // TODO Refactor
    switch (result) {
    case RESULT_ERROR:
      return getErrString();
    case RESULT_NOQUORUM:
      return "No Quorum";
    case RESULT_UNTRUSTED:
      return "Untrusted Peers";
    case RESULT_TOO_CLOSE:
      return "Too Close";
    case RESULT_WON:
      if(replayDeadline != null) {
        return "Repaired";
      }
      return "Won";
    case RESULT_LOST:
      return "Lost";
    case RESULT_UNVERIFIED:
      return "Unverified";
    case RESULT_VERIFIED:
      return "Verified";
    case RESULT_DISOWNED:
      return "Disowned";
    case RESULT_NEED_MORE_BLOCKS:
      return "Need More Blocks";
    default:
      return "Active";
    }
  }

  public BasePoll getPoll() {
    return poll;
  }

  public void addNeedBlocksFromPeer(PeerIdentity id) {
    this.needBlocksFrom.add(id);
  }

  // XXX: Margins!
  public void tallyVotes() {
    int agree = agreeVoters.size();
    int disagree = disagreeVoters.size();

    if (needBlocksFrom.size() > 0) {
      result = RESULT_NEED_MORE_BLOCKS;
      return;
    }

    if (agree + disagree < quorum) {
      result = RESULT_NOQUORUM;
    }
    else if (agree > disagree) {
      result = RESULT_WON;
    }
    else if (agree < disagree) {
      result = RESULT_LOST;
    }
  }

  public boolean stateIsActive() {
    return (result == RESULT_POLLING);
  }

  public boolean stateIsFinished() {
    return (result != RESULT_POLLING);
  }

  public boolean stateIsSuspended() {
    // TODO Auto-generated method stub
    return false;
  }

  public void setStateSuspended() {
    // TODO Auto-generated method stub
  }

  public void replayVoteCheck(Vote vote, Deadline deadline) {
    // TODO Auto-generated method stub
  }

  public void adjustReputation(PeerIdentity voterID, int repDelta) {
    // TODO Auto-generated method stub
  }

  public int getTallyResult() {
    return result;
  }

  // V3 Specific Methods.

  public void reset() {
    result = RESULT_POLLING;
    disagreeVoters = null;
    agreeVoters = null;
    needBlocksFrom.clear();
  }

  public List getNeedBlocksFrom() {
    return needBlocksFrom;
  }

  public void setDisagreeVoters(List disagreeVoters) {
    this.disagreeVoters = disagreeVoters;
  }

  public List getDisagreeVoters() {
    return disagreeVoters;
  }

  public void setAgreeVoters(List agreeVoters) {
    this.agreeVoters = agreeVoters;
  }

  public List getAgreeVoters() {
    return agreeVoters;
  }

}
