/*
 * $Id: BlockTally.java,v 1.17 2011-12-06 23:26:08 barry409 Exp $
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
import org.lockss.config.*;

/**
 * Representation of the tally for an individual vote block.
 */
public class BlockTally {
  // todo(bhayes): PeerIdentity is less useful than
  // ParticipantUserData. But that would be harder to test. Make
  // BlockTally<T>, and then we can test it with T. BlockTally never
  // tries to look into the T, just counts them.

  // todo(bhayes): Make this an Enumeration.
  public static final int RESULT_HASHING = 0; // Not used
  public static final int RESULT_NOQUORUM = 1;
  public static final int RESULT_TOO_CLOSE = 2;
  public static final int RESULT_TOO_CLOSE_POLLER_ONLY_BLOCK = 3; // Not used
  public static final int RESULT_TOO_CLOSE_VOTER_ONLY_BLOCK = 4; // Not used
  public static final int RESULT_LOST = 5;
  public static final int RESULT_LOST_POLLER_ONLY_BLOCK = 6;
  public static final int RESULT_LOST_VOTER_ONLY_BLOCK = 7;
  public static final int RESULT_WON = 8;
  public static final int RESULT_REPAIRED = 9; // Not used

  // List of voters with whom we agree
  private Collection<PeerIdentity> agreeVoters =
    new ArrayList<PeerIdentity>();
  // List of voters with whom we disagree
  private Collection<PeerIdentity> disagreeVoters =
    new ArrayList<PeerIdentity>();
  // List of voters who we believe do not have a block that we do.
  private Collection<PeerIdentity> pollerOnlyBlockVoters =
    new ArrayList<PeerIdentity>();
  // List of voters who we believe have an block that we do not.
  private Collection<PeerIdentity> voterOnlyBlockVoters =
    new ArrayList<PeerIdentity>();

  private static final Logger log = Logger.getLogger("BlockTally");

  public BlockTally() {}
  
  public static String getStatusString(int result) {
    switch (result) {
    case RESULT_HASHING:
      return "Hashing";
    case RESULT_NOQUORUM:
      return "No Quorum";
    case RESULT_TOO_CLOSE:
      return "Too Close";
    case RESULT_TOO_CLOSE_POLLER_ONLY_BLOCK:
      return "Too Close - Poller-only Block";
    case RESULT_TOO_CLOSE_VOTER_ONLY_BLOCK:
      return "Too Close - Voter-only Block";
    case RESULT_LOST:
      return "Lost";
    case RESULT_LOST_POLLER_ONLY_BLOCK:
      return "Lost - Poller-only Block";
    case RESULT_LOST_VOTER_ONLY_BLOCK:
      return "Lost - Voter-only Block";
    case RESULT_WON:
      return "Won";
    case RESULT_REPAIRED:
      return "Repaired";
    default:
      return "Unknown";
    }
  }

  public int getTallyResult(int quorum, int voteMargin) {
    int result;
    int agree = agreeVoters.size();
    int disagree = disagreeVoters.size();
    int pollerOnlyBlocks = pollerOnlyBlockVoters.size();
    int voterOnlyBlocks = voterOnlyBlockVoters.size();

    if (agree + disagree < quorum) {
      result = RESULT_NOQUORUM;
    } else if (!isWithinMargin(voteMargin)) { 
      result = RESULT_TOO_CLOSE;
    } else if (pollerOnlyBlocks >= quorum) {
      result = RESULT_LOST_POLLER_ONLY_BLOCK;
    } else if (voterOnlyBlocks >= quorum) {
      result = RESULT_LOST_VOTER_ONLY_BLOCK;
    } else if (agree > disagree) {
      result = RESULT_WON;
    } else {
      result = RESULT_LOST;
    }
    return result;
  }

  public void addDisagreeVoter(PeerIdentity id) {
    disagreeVoters.add(id);
  }

  public Collection<PeerIdentity> getDisagreeVoters() {
    return disagreeVoters;
  }

  public void addAgreeVoter(PeerIdentity id) {
    // todo(bhayes): For versioned voting, this will have to record
    // which versions the poller and voter agreed on.
    agreeVoters.add(id);
  }

  public Collection<PeerIdentity> getAgreeVoters() {
    return agreeVoters;
  }

  public void addPollerOnlyBlockVoter(PeerIdentity id) {
    pollerOnlyBlockVoters.add(id);
    disagreeVoters.add(id);
  }

  public Collection<PeerIdentity> getPollerOnlyBlockVoters() {
    return pollerOnlyBlockVoters;
  }

  public void addVoterOnlyBlockVoter(PeerIdentity id) {
    voterOnlyBlockVoters.add(id);
    disagreeVoters.add(id);
  }

  public Collection<PeerIdentity> getVoterOnlyBlockVoters() {
    return voterOnlyBlockVoters;
  }

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
}
