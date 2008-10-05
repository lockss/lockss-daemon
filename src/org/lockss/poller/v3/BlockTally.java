/*
 * $Id: BlockTally.java,v 1.14 2007-10-09 00:49:55 smorabito Exp $
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
  public static final int RESULT_TOO_CLOSE_EXTRA_BLOCK = 3;
  public static final int RESULT_TOO_CLOSE_MISSING_BLOCK = 4;
  public static final int RESULT_LOST = 5;
  public static final int RESULT_LOST_EXTRA_BLOCK = 6;
  public static final int RESULT_LOST_MISSING_BLOCK = 7;
  public static final int RESULT_WON = 8;
  public static final int RESULT_REPAIRED = 9;

  // List of voters with whom we agree
  private List agreeVoters = new ArrayList();
  // List of voters with whom we disagree
  private List disagreeVoters = new ArrayList();
  // List of voters who we believe do not have a block that we do.
  private List extraBlockVoters = new ArrayList();
  // List of voters who we believe have an block that we do not.
  // This is a map of URLs to peer identities.
  // JAVA5: Map<String,Set<PeerIdentity>>
  private Map missingBlockVoters = new HashMap();
  /** 
   * Deprecated in Daemon 1.23.
   * @deprecated
   */
  private LinkedHashMap votes = new LinkedHashMap();
  // Name of the missing block, if any.
  private String missingBlockUrl;

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
    extraBlockVoters = new ArrayList();
    missingBlockVoters = new HashMap();
    missingBlockUrl = null;
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
    case RESULT_TOO_CLOSE_EXTRA_BLOCK:
      return "Too Close - Extra Block";
    case RESULT_TOO_CLOSE_MISSING_BLOCK:
      return "Too Close - Missing Block";
    case RESULT_LOST:
      return "Lost";
    case RESULT_LOST_EXTRA_BLOCK:
      return "Lost - Extra Block";
    case RESULT_LOST_MISSING_BLOCK:
      return "Lost - Missing Block";
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
    int extraBlocks = extraBlockVoters.size();
    int missingBlocks = getAllMissingBlockVoters().size();

    if (agree + disagree < quorum) {
      result = RESULT_NOQUORUM;
    } else if (!isWithinMargin()) { 
      result = RESULT_TOO_CLOSE;
    } else if (extraBlocks >= quorum) {
      result = RESULT_LOST_EXTRA_BLOCK;
    } else if (missingBlocks >= quorum) {
      // Attempt to find the name of the missing block, if possible.
      String missingUrl = null;
      int maxMissingUrlCount = 0;
      for (Iterator iter = missingBlockVoters.keySet().iterator(); iter.hasNext(); ) {
        String url = (String)iter.next();
        Set s = (Set)missingBlockVoters.get(url);
        if (s.size() > maxMissingUrlCount) {
          maxMissingUrlCount = s.size();
          missingUrl = url;
        }
      }
      if (maxMissingUrlCount >= quorum) {
        log.debug("Found agreement on missing URL name: " + missingUrl);
        result = RESULT_LOST_MISSING_BLOCK;
        this.missingBlockUrl = missingUrl;
      } else {
        log.debug("Could not reach agreement on missing URL name: " + missingBlockVoters);
        result = RESULT_TOO_CLOSE_MISSING_BLOCK;
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

  public void addExtraBlockVoter(PeerIdentity id) {
    extraBlockVoters.add(id);
    disagreeVoters.add(id);
  }

  public Collection getExtraBlockVoters() {
    return extraBlockVoters;
  }

  /**
   * Return the name of the missing block, if any.
   */
  public String getMissingBlockUrl() {
    return missingBlockUrl;
  }

  public void addMissingBlockVoter(PeerIdentity id, String url) {
    Set voters;
    if ((voters = (Set)missingBlockVoters.get(url)) == null) {
      voters = new HashSet();
      missingBlockVoters.put(url, voters);
    }
    voters.add(id);
    disagreeVoters.add(id);
  }

  private Collection getAllMissingBlockVoters() {
    Set voters = new HashSet();
    for (Iterator iter = missingBlockVoters.values().iterator(); iter.hasNext(); ) {
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
  public Collection getMissingBlockVoters(String url) {
    return (Set)missingBlockVoters.get(url);
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
