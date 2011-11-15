/*
 * $Id: BlockTally.java,v 1.15 2011-11-15 01:30:34 barry409 Exp $
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

  public static final int RESULT_HASHING = 0;
  public static final int RESULT_NOQUORUM = 1;
  public static final int RESULT_TOO_CLOSE = 2;
  public static final int RESULT_TOO_CLOSE_POLLER_ONLY_BLOCK = 3;
  public static final int RESULT_TOO_CLOSE_VOTER_ONLY_BLOCK = 4;
  public static final int RESULT_LOST = 5;
  public static final int RESULT_LOST_POLLER_ONLY_BLOCK = 6;
  public static final int RESULT_LOST_VOTER_ONLY_BLOCK = 7;
  public static final int RESULT_WON = 8;
  public static final int RESULT_REPAIRED = 9;

  // List of voters with whom we agree
  private List agreeVoters = new ArrayList();
  // List of voters with whom we disagree
  private List disagreeVoters = new ArrayList();
  // List of voters who we believe do not have a block that we do.
  private List pollerOnlyBlockVoters = new ArrayList();
  // List of voters who we believe have an block that we do not.
  // This is a map of URLs to peer identities.
  // JAVA5: Map<String,Set<PeerIdentity>>
  private Map voterOnlyBlockVoters = new HashMap();
  /** 
   * Deprecated in Daemon 1.23.
   * @deprecated
   */
  private LinkedHashMap votes = new LinkedHashMap();
  // Name of the voterOnly block, if any.
  private String voterOnlyBlockUrl;

  int result = RESULT_HASHING; // Always hashing when BlockTally is created.
  int quorum;
  double voteMargin;

  private static final Logger log = Logger.getLogger("BlockTally");

  public BlockTally(int quorum) {
    this.quorum = quorum;
    this.voteMargin =
      ((double)CurrentConfig.getIntParam(V3Poller.PARAM_V3_VOTE_MARGIN,
                                         V3Poller.DEFAULT_V3_VOTE_MARGIN)) / 100;
  }

  // Set back to initial state
  public void reset() {
    agreeVoters = new ArrayList();
    disagreeVoters = new ArrayList();
    pollerOnlyBlockVoters = new ArrayList();
    voterOnlyBlockVoters = new HashMap();
    voterOnlyBlockUrl = null;
    result = RESULT_HASHING;
  }
  
  public String getStatusString() {
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

  public void tallyVotes() {
    int agree = agreeVoters.size();
    int disagree = disagreeVoters.size();
    int pollerOnlyBlocks = pollerOnlyBlockVoters.size();
    int voterOnlyBlocks = getAllVoterOnlyBlockVoters().size();

    if (agree + disagree < quorum) {
      result = RESULT_NOQUORUM;
    } else if (!isWithinMargin()) { 
      result = RESULT_TOO_CLOSE;
    } else if (pollerOnlyBlocks >= quorum) {
      result = RESULT_LOST_POLLER_ONLY_BLOCK;
    } else if (voterOnlyBlocks >= quorum) {
      // Attempt to find the name of the missing block, if possible.
      String voterOnlyUrl = null;
      int maxVoterOnlyUrlCount = 0;
      for (Iterator iter = voterOnlyBlockVoters.keySet().iterator(); iter.hasNext(); ) {
        String url = (String)iter.next();
        Set s = (Set)voterOnlyBlockVoters.get(url);
        if (s.size() > maxVoterOnlyUrlCount) {
          maxVoterOnlyUrlCount = s.size();
          voterOnlyUrl = url;
        }
      }
      if (maxVoterOnlyUrlCount >= quorum) {
        log.debug("Found agreement on missing URL name: " + voterOnlyUrl);
        result = RESULT_LOST_VOTER_ONLY_BLOCK;
        this.voterOnlyBlockUrl = voterOnlyUrl;
      } else {
        log.debug("Could not reach agreement on missing URL name: " + voterOnlyBlockVoters);
        result = RESULT_TOO_CLOSE_VOTER_ONLY_BLOCK;
      }
    } else if (agree > disagree) {
      result = RESULT_WON;
    } else {
      result = RESULT_LOST;
    }
  }

  public int getTallyResult() {
    return result;
  }

  public void addDisagreeVoter(PeerIdentity id) {
    disagreeVoters.add(id);
  }

  public List getDisagreeVoters() {
    return disagreeVoters;
  }

  public void addAgreeVoter(PeerIdentity id) {
    agreeVoters.add(id);
  }

  public List getAgreeVoters() {
    return agreeVoters;
  }

  public void addPollerOnlyBlockVoter(PeerIdentity id) {
    pollerOnlyBlockVoters.add(id);
    disagreeVoters.add(id);
  }

  public Collection getPollerOnlyBlockVoters() {
    return pollerOnlyBlockVoters;
  }

  /**
   * Return the name of the missing block, if any.
   */
  public String getVoterOnlyBlockUrl() {
    return voterOnlyBlockUrl;
  }

  public void addVoterOnlyBlockVoter(PeerIdentity id, String url) {
    Set voters;
    if ((voters = (Set)voterOnlyBlockVoters.get(url)) == null) {
      voters = new HashSet();
      voterOnlyBlockVoters.put(url, voters);
    }
    voters.add(id);
    disagreeVoters.add(id);
  }

  private Collection getAllVoterOnlyBlockVoters() {
    Set voters = new HashSet();
    for (Iterator iter = voterOnlyBlockVoters.values().iterator(); iter.hasNext(); ) {
      Set s = (Set)iter.next();
      voters.addAll(s);
    }
    return voters;
  }

  boolean isWithinMargin() {
    int numAgree = agreeVoters.size();
    int numDisagree = disagreeVoters.size();
    double num_votes = numAgree + numDisagree;
    double req_margin = voteMargin;
    double act_margin;

    if (numAgree > numDisagree) {
      act_margin = (double) numAgree / num_votes;
    } else {
      act_margin = (double) numDisagree / num_votes;
    }

    if (act_margin < req_margin) {
      return false;
    }
    return true;
  }
  
  /**
   * Return a set of all peers that claim to have the specified URL.
   * @param url
   * @return a set of all peers that claim to have the specified URL.
   */
  public Collection getVoterOnlyBlockVoters(String url) {
    return (Set)voterOnlyBlockVoters.get(url);
  }

  /**
   * Deprecated in Daemon 1.23. 
   * @deprecated
   */
  public void addVoteForBlock(PeerIdentity id, VoteBlock vb) {
    throw new UnsupportedOperationException("No longer implemented.");
  }

  /**
   * Deprecated in Daemon 1.23. 
   * @deprecated
   */
  public LinkedHashMap getVotesForBlock() {
    throw new UnsupportedOperationException("No longer implemented.");
  }
}
