/*
 * $Id: BlockTally.java,v 1.20 2012-03-09 20:51:45 barry409 Exp $
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
import org.lockss.poller.*;
import org.lockss.protocol.PeerIdentity;
import org.lockss.protocol.VoteBlock;
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

  private final HashBlockComparer comparer;

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

  // Also used for testing.
  BlockTally(HashBlockComparer comparer) {
    this.comparer = comparer;
  }

  public BlockTally() {
    this((HashBlockComparer)null);
  }

  public BlockTally(HashBlock hashBlock) {
    this(new HashBlockComparerImpl(hashBlock));
  }

  private boolean pollerHas() {
    return comparer != null;
  }

  public Result getTallyResult(int quorum, int voteMargin) {
    Result result;
    int agree = agreeVoters.size();
    int disagree = disagreeVoters.size();
    int pollerOnlyBlocks = pollerOnlyBlockVoters.size();
    int voterOnlyBlocks = voterOnlyBlockVoters.size();

    if (agree + disagree < quorum) {
      result = Result.NOQUORUM;
    } else if (!isWithinMargin(voteMargin)) { 
      result = Result.TOO_CLOSE;
    } else if (pollerOnlyBlocks >= quorum) {
      result = Result.LOST_POLLER_ONLY_BLOCK;
    } else if (voterOnlyBlocks >= quorum) {
      result = Result.LOST_VOTER_ONLY_BLOCK;
    } else if (agree > disagree) {
      result = Result.WON;
    } else {
      result = Result.LOST;
    }
    return result;
  }

  public void voteSpoiled(PeerIdentity id) {
    // Don't track these for now.
    log.debug3(id+"  spoiled");
  }

  public void vote(VoteBlock voteBlock, PeerIdentity id, int participantIndex) {
    if (pollerHas()) {
      if (comparer.compare(voteBlock, participantIndex)) {
	log.debug3(id+"  agreed");
	addAgreeVoter(id);
      } else {
	log.debug3(id+"  disagreed");
	addDisagreeVoter(id);
      }
    } else {
      log.debug3(id+"  voterOnly");
      addVoterOnlyBlockVoter(id);
    }
  }

  public void voteMissing(PeerIdentity id) {
    log.debug3(id+"  didn't have");
    if (pollerHas()) {
      addPollerOnlyBlockVoter(id);
    } else {
      // The poller and this voter both do not have the URL, so
      // tally is "agree".
      addAgreeVoter(id);
    }
  }

  void addDisagreeVoter(PeerIdentity id) {
    disagreeVoters.add(id);
  }

  public Collection<PeerIdentity> getDisagreeVoters() {
    return disagreeVoters;
  }

  void addAgreeVoter(PeerIdentity id) {
    // todo(bhayes): For versioned voting, this will have to record
    // which versions the poller and voter agreed on.
    agreeVoters.add(id);
  }

  public Collection<PeerIdentity> getAgreeVoters() {
    return agreeVoters;
  }

  void addPollerOnlyBlockVoter(PeerIdentity id) {
    pollerOnlyBlockVoters.add(id);
    disagreeVoters.add(id);
  }

  public Collection<PeerIdentity> getPollerOnlyBlockVoters() {
    return pollerOnlyBlockVoters;
  }

  void addVoterOnlyBlockVoter(PeerIdentity id) {
    voterOnlyBlockVoters.add(id);
    disagreeVoters.add(id);
  }

  public Collection<PeerIdentity> getVoterOnlyBlockVoters() {
    return voterOnlyBlockVoters;
  }

  /**
   * @return All of the non-spoiled voters.
   */
  public Collection<PeerIdentity> getTalliedVoters() {
    Collection<PeerIdentity> tallied = new ArrayList(agreeVoters);
    tallied.addAll(disagreeVoters);
    return Collections.unmodifiableCollection(tallied);
  }

  /**
   * @return The subset of the voters who agree on at least one
   * version with the poller.
   */
  public Collection<PeerIdentity> getVersionAgreedVoters() {
    if (pollerHas()) {
      return Collections.unmodifiableCollection(agreeVoters);
    } else {
      // If the poller didn't have it, nobody agreed on a version.
      return Collections.EMPTY_LIST;
    }
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


  static interface HashBlockComparer {
    public boolean compare(VoteBlock voteBlock, int participantIndex);
  }

  /**
   * <p>Encapsulate the comparing of {@link HashBlock} and {@link
   * VoteBlock} objects.</p>
   */
  static class HashBlockComparerImpl implements HashBlockComparer {
    private final HashBlock.Version[] hbVersions;

    /**
     * <p>Create a {@link HashBlockComparer} for the given {@link
     * HashBlock}.</p>
     */
    HashBlockComparerImpl(HashBlock hashBlock) {
      hbVersions = hashBlock.getVersions();
      for (int versionIndex = 0; versionIndex < hbVersions.length;
	   versionIndex++) {
	HashBlock.Version hbVersion = hbVersions[versionIndex];
	if (hbVersion.getHashError() != null) {
	  // Only log the hash error in the poller at
	  // initialize. There's probably a more detailed error in the
	  // log.
	  log.warning("HashBlock version "+versionIndex+" had hashing error.");
	} 
      }
    }

    /**
     * <p>Compare the given {@link VoteBlock} to the {@link HashBlock}
     * provided at construction.</p>
     * @return true iff any version of the participant's hash matches
     * any version of the poller's expected hashes for that
     * participant.
     */
    public boolean compare(VoteBlock voteBlock, int participantIndex) {
      // Note: At worst, n*m byte[] compares to notice a non-match. In
      // reality: "versioned voting" needs to be improved, and the
      // "any match of any version" condition needs to be refined.
      for (HashBlock.Version hbVersion : hbVersions) {
	if (hbVersion.getHashError() == null) {
	  byte[] hbHash = hbVersion.getHashes()[participantIndex];
	  VoteBlock.Version[] vbVersions = voteBlock.getVersions();
	  for (int versionIndex = 0; versionIndex < vbVersions.length;
	       versionIndex++) {
	    VoteBlock.Version vbVersion = vbVersions[versionIndex];
	    if (vbVersion.getHashError()) {
	      log.warning("Voter version "+versionIndex+
			  " had a hashing error.");
	    } else {
	      byte[] vbHash = vbVersion.getHash();
	      if (Arrays.equals(hbHash, vbHash)) {
		return true;
	      }
	    }
	  }
	}
      }
      return false;
    }
  }
}
