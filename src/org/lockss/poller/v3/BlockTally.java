/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.hasher.HashBlock;
import org.lockss.hasher.HashResult;
import org.lockss.protocol.VoteBlock;
import org.lockss.util.ByteArray;
import org.lockss.util.Logger;


/**
 * Representation of the tally for an individual vote block.
 */
public class BlockTally implements VoteBlockTallier.VoteBlockTally {

  // Note: These result categories make more sense when the
  // publisher's content for the URL never changes.
  /**
   * The result of a poll on a particular block or URL. 
   */
  public enum Result {
    /** Not enough voters to draw a conclusion. Note that while the
     * poll may have a quorum, if one or more voters may have
     * unreadable VoteBlocks, and the result for this block may not have
     * a quorum. */
    NOQUORUM("No Quorum"),
    /**   
     * No other result applies. The conditions are too complex to
     * state succinctly.
     */   
    TOO_CLOSE("Too Close"),
    /** 
     * The poller has this block [otherwise this would be
     * LOST_VOTER_ONLY] and there is not a landslide of voters with no
     * content, but not a sufficient agreement for WON.
     */
    LOST("Lost"),
    /** The poller has this block, and a landslide of voters do not.
     */
    LOST_POLLER_ONLY_BLOCK("Lost - Poller-only Block"),
    /** The poller does not have this this block, but a landslide of
     * voters have some content for it. */
    LOST_VOTER_ONLY_BLOCK("Lost - Voter-only Block"),
    /** For a landslide of voters, some version the poller has
     * collected matches some version the voter has collected.  Note:
     * This does not mean that any particular version has "lots of
     * copies".
     */
    WON("Won");

    final String printString;
    Result(String printString) {
      this.printString = printString;
    }
  }

  private static Collection<HashResult>
    pollerHashes(HashBlock hashBlock,
		 V3Poller.HashIndexer hashIndexer) {
    Collection<HashResult> pollerHashes = new HashSet<HashResult>();

    for (HashBlock.Version hbVersion: hashBlock.getVersions()) {
      if (hbVersion.getHashError() == null) {
	HashResult plainHash = hashIndexer.getPlainHash(hbVersion);
	pollerHashes.add(plainHash);
      }
    }
    return pollerHashes;
  }

  private static final Logger log = Logger.getLogger("BlockTally");

  // package level, for testing, and access by BlockTally.
  // List of voters with whom the poller agrees.
  final Collection<ParticipantUserData> agreeVoters =
    new ArrayList<ParticipantUserData>();
  // List of voters with whom the poller disagrees.
  final Collection<ParticipantUserData> disagreeVoters =
    new ArrayList<ParticipantUserData>();
  // List of voters who do not have a block that the poller does.
  final Collection<ParticipantUserData> pollerOnlyVoters =
    new ArrayList<ParticipantUserData>();
  // List of voters who have an block that the poller does not.
  final Collection<ParticipantUserData> voterOnlyVoters =
    new ArrayList<ParticipantUserData>();

  final private VersionCounts versionCounts = VersionCounts.make();

  final private int quorum;
  final private int voteMargin;
  final private Collection<HashResult> pollerHashes;

  public BlockTally(int quorum, int voteMargin) {
    this(quorum, voteMargin, Collections.EMPTY_SET);
  }

  public BlockTally(int quorum, int voteMargin,
		    HashBlock hashBlock, V3Poller.HashIndexer hashIndexer) {
    this(quorum, voteMargin, pollerHashes(hashBlock, hashIndexer));
  }

  public BlockTally(int quorum, int voteMargin,
		    Collection<HashResult> pollerHashes) {
    this.quorum = quorum;
    this.voteMargin = voteMargin;
    this.pollerHashes = pollerHashes;
  }

  // Interface VoteBlockTally methods to springboard to our internal
  // methods.
  @Override public void voteSpoiled(ParticipantUserData id) {}
  @Override public void voteAgreed(ParticipantUserData id) {
    addAgreeVoter(id);
  }
  @Override public void voteDisagreed(ParticipantUserData id) {
    addDisagreeVoter(id);
  }
  @Override public void votePollerOnly(ParticipantUserData id) {
    addPollerOnlyVoter(id);
  }
  @Override public void voteVoterOnly(ParticipantUserData id) {
    addVoterOnlyVoter(id);
  }
  @Override public void voteNeither(ParticipantUserData id) {
    // Leads to WON if a landslide of voters do not have the URL when
    // the poller also does not have it.
    addAgreeVoter(id);
  }

  public VoteBlockTallier.VoteCallback getVoteCallback() {
    return versionCounts;
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
  BlockTally.Result getTallyResult() {
    BlockTally.Result result;
    int landslideMinimum = landslideMinimum();

    if (numVotes() < quorum) {
      result = BlockTally.Result.NOQUORUM;
    } else if (agreeVoters.size() >= landslideMinimum) {
      result = BlockTally.Result.WON;
    } else if (disagreeVoters.size() >= landslideMinimum) {
      // Which kind of LOST?
      if (pollerOnlyVoters.size() >= landslideMinimum) {
	result = BlockTally.Result.LOST_POLLER_ONLY_BLOCK;
      } else if (voterOnlyVoters.size() >= landslideMinimum) {
	result = BlockTally.Result.LOST_VOTER_ONLY_BLOCK;
      } else {
	result = BlockTally.Result.LOST;
      }
    } else {
      result = BlockTally.Result.TOO_CLOSE;
    }
    return result;
  }

  /**
   * @return A Collection of ParticipantUserData including only those
   * with head versions which have at least the minimum support
   * required to have WON if the poller had had that version. The
   * Collection may be empty.
   */
  public Collection<ParticipantUserData> getRepairVoters() {
    switch (getTallyResult()) {
    case LOST:
    case LOST_VOTER_ONLY_BLOCK:
      return versionCounts.getRepairCandidates(landslideMinimum(),
					       pollerHashes).keySet();
    default:
      throw new ShouldNotHappenException(
	"Called with inappropriate tally result "+getTallyResult());
    }
  }

  public Collection<ParticipantUserData> getSortedRepairCandidates(int minReplicas) {
    return versionCounts.getSortedRepairCandidates(minReplicas);
  }

  public Collection<ParticipantUserData> getAgreeVoters() {
    return Collections.unmodifiableCollection(agreeVoters);
  }

  public Collection<ParticipantUserData> getDisagreeVoters() {
    return Collections.unmodifiableCollection(disagreeVoters);
  }

  public Collection<ParticipantUserData> getPollerOnlyBlockVoters() {
    return Collections.unmodifiableCollection(pollerOnlyVoters);
  }

  public Collection<ParticipantUserData> getVoterOnlyBlockVoters() {
    return Collections.unmodifiableCollection(voterOnlyVoters);
  }

  private int numVotes() {
    return agreeVoters.size() + disagreeVoters.size();
  }

  /**
   * @param numVotes The number of votes submitted.
   * @param voteMargin The percentage (times 100) of submitted votes
   * required for a landslide.
   * @return The number of votes required to be considered a
   * landslide.
   */
  public static int landslideMinimum(int numVotes, int voteMargin) {
    return (int)Math.ceil(((double)voteMargin / 100) * numVotes);
  }

  /**
   * @return the minimum number of voters required for a landslide.
   */
  int landslideMinimum() {
    return landslideMinimum(numVotes(), voteMargin);
  }

  void addAgreeVoter(ParticipantUserData id) {
    agreeVoters.add(id);
  }

  void addDisagreeVoter(ParticipantUserData id) {
    disagreeVoters.add(id);
  }

  void addPollerOnlyVoter(ParticipantUserData id) {
    disagreeVoters.add(id);
    pollerOnlyVoters.add(id);
  }

  void addVoterOnlyVoter(ParticipantUserData id) {
    disagreeVoters.add(id);
    voterOnlyVoters.add(id);
  }

  boolean isVoterOnly() {
    return disagreeVoters.size() == voterOnlyVoters.size();
  }

  public String toString() {
    return "[BlockTally: a/d/po/vo: " + votes() + "]";
  }
}
