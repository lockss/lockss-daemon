/*
 * $Id: BlockTally.java,v 1.23 2012-03-14 22:20:21 barry409 Exp $
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

import org.lockss.hasher.HashBlock;
import org.lockss.protocol.VoteBlock;
import org.lockss.util.Logger;


/**
 * Representation of the tally for an individual vote block.
 */
public class BlockTally<T> implements VoteBlockTallier.VoteBlockTally<T> {

  public enum Result {
    NOQUORUM("No Quorum"),
    TOO_CLOSE("Too Close"),
    LOST("Lost"),
    LOST_POLLER_ONLY_BLOCK("Lost - Poller-only Block"),
    LOST_VOTER_ONLY_BLOCK("Lost - Voter-only Block"),
    WON("Won");

    final String printString;
    Result(String printString) {
      this.printString = printString;
    }
  }

  private static final Logger log = Logger.getLogger("BlockTally");

  // package level, for testing, and access by BlockTally.
  // List of voters with whom the poller agrees.
  final Collection<T> agreeVoters = new ArrayList<T>();
  // List of voters with whom the poller disagrees.
  final Collection<T> disagreeVoters = new ArrayList<T>();
  // List of voters who do not have a block that the poller does.
  final Collection<T> pollerOnlyVoters = new ArrayList<T>();
  // List of voters who have an block that the poller does not.
  final Collection<T> voterOnlyVoters = new ArrayList<T>();

  // Interface methods to springboard to our internal methods.
  public void voteSpoiled(T id) {}
  public void voteAgreed(T id) {
    addAgreeVoter(id);
  }
  public void voteDisagreed(T id) {
    addDisagreeVoter(id);
  }
  public void votePollerOnly(T id) {
    addPollerOnlyVoter(id);
  }
  public void voteVoterOnly(T id) {
    addVoterOnlyVoter(id);
  }
  public void voteNeither(T id) {
    // todo(bhayes): This is questionable.
    addAgreeVoter(id);
  }

  /**
   * @return a String representing the votes by category, separated by "/":
   * agree/disagree/pollerOnly/voterOnly
   */
  String votes() {
    return agreeVoters.size() + "/" + disagreeVoters.size() + "/" +
      pollerOnlyVoters.size() + "/" + voterOnlyVoters.size();
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

    if (numVotes() < quorum) {
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

  public Collection<T> getAgreeVoters() {
    return Collections.unmodifiableCollection(agreeVoters);
  }

  public Collection<T> getDisagreeVoters() {
    return Collections.unmodifiableCollection(disagreeVoters);
  }

  public Collection<T> getPollerOnlyBlockVoters() {
    return Collections.unmodifiableCollection(pollerOnlyVoters);
  }

  public Collection<T> getVoterOnlyBlockVoters() {
    return Collections.unmodifiableCollection(voterOnlyVoters);
  }

  private int numVotes() {
    return agreeVoters.size() + disagreeVoters.size();
  }

  /**
   * @return if the result is a landslide.
   */
  boolean isWithinMargin(int voteMargin) {
    int numAgree = agreeVoters.size();
    int numDisagree = disagreeVoters.size();
    double numVotes = numVotes();
    double actualMargin;

    if (numAgree > numDisagree) {
      actualMargin = (double) numAgree / numVotes;
    } else {
      actualMargin = (double) numDisagree / numVotes;
    }

    if (actualMargin * 100 < voteMargin) {
      return false;
    }
    return true;
  }

  void addAgreeVoter(T id) {
    agreeVoters.add(id);
  }

  void addDisagreeVoter(T id) {
    disagreeVoters.add(id);
  }

  void addPollerOnlyVoter(T id) {
    disagreeVoters.add(id);
    pollerOnlyVoters.add(id);
  }

  void addVoterOnlyVoter(T id) {
    disagreeVoters.add(id);
    voterOnlyVoters.add(id);
  }
}
