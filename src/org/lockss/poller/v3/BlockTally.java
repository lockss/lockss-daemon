/*
 * $Id: BlockTally.java,v 1.6 2006-01-12 03:13:30 smorabito Exp $
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
public class BlockTally {

  public static final int RESULT_HASHING = 0;
  public static final int RESULT_NOQUORUM = 1;
  public static final int RESULT_TOO_CLOSE = 2;
  public static final int RESULT_LOST = 3;
  public static final int RESULT_LOST_EXTRA_BLOCK = 4;
  public static final int RESULT_LOST_MISSING_BLOCK = 5;
  public static final int RESULT_WON = 6;
  public static final int RESULT_REPAIRED = 7;

  // List of voters with whom we agree
  private List agreeVoters = new ArrayList();
  // List of voters with whom we disagree
  private List disagreeVoters = new ArrayList();
  // List of voters who we believe have an extra block.
  private List extraBlockVoters = new ArrayList();
  // List of voters who we believe are missing a block that we have.
  private List missingBlockVoters = new ArrayList();

  private int result;
  private int quorum;

  public BlockTally(int quorum) {
    this.reset();
  }

  public String getStatusString() {
    switch (result) {
    case RESULT_HASHING:
      return "Hashing";
    case RESULT_NOQUORUM:
      return "No Quorum";
    case RESULT_TOO_CLOSE:
      return "Too Close";
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
    int missingBlocks = missingBlockVoters.size();

    if (agree + disagree < quorum) {
      result = RESULT_NOQUORUM;
    }
    else if (agree > disagree) {
      result = RESULT_WON;
    }
    // XXX: Margins!
    else if (agree < disagree) {
      if (extraBlocks > quorum) {
        result = RESULT_LOST_EXTRA_BLOCK;
      } else if (missingBlocks > quorum) {
        result = RESULT_LOST_MISSING_BLOCK;
      } else {
        result = RESULT_LOST;
      }
    }
  }

  public int getTallyResult() {
    return result;
  }

  public void reset() {
    result = RESULT_HASHING;
    disagreeVoters.clear();
    agreeVoters.clear();
    missingBlockVoters.clear();
    extraBlockVoters.clear();
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

  public List getExtraBlockVoters() {
    return extraBlockVoters;
  }

  public void addMissingBlockVoter(PeerIdentity id) {
    missingBlockVoters.add(id);
    disagreeVoters.add(id);
  }

  public List getMissingBlockVoters() {
    return missingBlockVoters;
  }
}
