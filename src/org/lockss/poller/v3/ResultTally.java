/*
 * $Id: ResultTally.java,v 1.1 2012-03-13 18:29:17 barry409 Exp $
 */

/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.protocol.PeerIdentity;

/**
 * A tally of the voters to decide on the final outcome of a vote on a URL.
 */
class ResultTally implements BlockTally.VoteTally {

  // package level, for testing, and access by BlockTally.
  // List of voters with whom the poller agrees.
  final Collection<PeerIdentity> agreeVoters =
    new ArrayList<PeerIdentity>();
  // List of voters with whom the poller disagrees.
  final Collection<PeerIdentity> disagreeVoters =
    new ArrayList<PeerIdentity>();
  // List of voters who do not have a block that the poller does.
  final Collection<PeerIdentity> pollerOnlyVoters =
    new ArrayList<PeerIdentity>();
  // List of voters who have an block that the poller does not.
  final Collection<PeerIdentity> voterOnlyVoters =
    new ArrayList<PeerIdentity>();

  public void voteSpoiled(PeerIdentity id) {}
  public void voteAgreed(PeerIdentity id) {
    addAgreeVoter(id);
  }
  public void voteDisagreed(PeerIdentity id) {
    addDisagreeVoter(id);
  }
  public void voteVoterOnly(PeerIdentity id) {
    addVoterOnlyVoter(id);
  }
  public void votePollerOnly(PeerIdentity id) {
    addPollerOnlyVoter(id);
  }
  public void voteNeither(PeerIdentity id) {
    addAgreeVoter(id);
  }

  /**
   * @return the result of the tally.
   */
  BlockTally.Result getTallyResult(int quorum, int voteMargin) {
    BlockTally.Result result;
    int agree = agreeVoters.size();
    int disagree = disagreeVoters.size();
    int pollerOnly = pollerOnlyVoters.size();
    int voterOnly = voterOnlyVoters.size();

    if (agree + disagree < quorum) {
      result = BlockTally.Result.NOQUORUM;
    } else if (!isWithinMargin(voteMargin)) { 
      result = BlockTally.Result.TOO_CLOSE;
    } else if (pollerOnly >= quorum) {
      result = BlockTally.Result.LOST_POLLER_ONLY_BLOCK;
    } else if (voterOnly >= quorum) {
      result = BlockTally.Result.LOST_VOTER_ONLY_BLOCK;
    } else if (agree > disagree) {
      result = BlockTally.Result.WON;
    } else {
      result = BlockTally.Result.LOST;
    }
    return result;
  }

  /**
   * @return if the result is a landslide.
   */
  boolean isWithinMargin(int voteMargin) {
    int numAgree = agreeVoters.size();
    int numDisagree = disagreeVoters.size();
    double num_votes = numAgree + numDisagree;
    double act_margin;

    if (numAgree > numDisagree) {
      act_margin = (double) numAgree / num_votes;
    } else {
      act_margin = (double) numDisagree / num_votes;
    }

    if (act_margin * 100 < voteMargin) {
      return false;
    }
    return true;
  }

  void addAgreeVoter(PeerIdentity id) {
    agreeVoters.add(id);
  }

  void addDisagreeVoter(PeerIdentity id) {
    disagreeVoters.add(id);
  }

  void addPollerOnlyVoter(PeerIdentity id) {
    pollerOnlyVoters.add(id);
    disagreeVoters.add(id);
  }

  void addVoterOnlyVoter(PeerIdentity id) {
    voterOnlyVoters.add(id);
    disagreeVoters.add(id);
  }
}
