/*
 * $Id: BlockTally.java,v 1.21 2012-03-13 18:29:17 barry409 Exp $
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

  interface VoteTally {
    public void voteSpoiled(PeerIdentity id);
    public void voteAgreed(PeerIdentity id);
    public void voteDisagreed(PeerIdentity id);
    public void voteVoterOnly(PeerIdentity id);
    public void votePollerOnly(PeerIdentity id);
    public void voteNeither(PeerIdentity id);
  }

  // Null if the poller does not have this URL.
  private final HashBlockComparer comparer;

  final UserDataTally userDataTally = new UserDataTally();
  final ResultTally resultTally = new ResultTally();

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

  // This should be done by using dispatch and a subclass.
  private boolean pollerHas() {
    return comparer != null;
  }

  /**
   * Vote using all the versions of the voter's VoteBlock.
   */
  public void vote(VoteBlock voteBlock, PeerIdentity id, int participantIndex) {
    if (pollerHas()) {
      if (comparer.compare(voteBlock, participantIndex)) {
	voteAgreed(id);
      } else {
	voteDisagreed(id);
      }
    } else {
      voteVoterOnly(id);
    }
  }

  /**
   * Vote that the URL is missing.
   */
  public void voteMissing(PeerIdentity id) {
    if (pollerHas()) {
      votePollerOnly(id);
    } else {
      voteNeither(id);
    }
  }

  /**
   * The voter is unable to cast a meaningful vote.
   */
  public void voteSpoiled(PeerIdentity id) {
    log.debug3(id+"  spoiled");
    userDataTally.voteSpoiled(id);
    resultTally.voteSpoiled(id);
  }

  void voteAgreed(PeerIdentity id) {
    log.debug3(id+"  agreed");
    userDataTally.voteAgreed(id);
    resultTally.voteAgreed(id);
  }

  void voteDisagreed(PeerIdentity id) {
    log.debug3(id+"  disagreed");
    userDataTally.voteDisagreed(id);
    resultTally.voteDisagreed(id);
  }

  void voteVoterOnly(PeerIdentity id) {
    log.debug3(id+"  voterOnly");
    userDataTally.voteVoterOnly(id);
    resultTally.voteVoterOnly(id);
  }

  void votePollerOnly(PeerIdentity id) {
    log.debug3(id+"  didn't have");
    userDataTally.votePollerOnly(id);
    resultTally.votePollerOnly(id);
  }

  void voteNeither(PeerIdentity id) {
    log.debug3(id+"  neither have");
    userDataTally.voteNeither(id);
    resultTally.voteNeither(id);
  }

  /**
   * @return
   */
  public Collection<PeerIdentity> getTalliedVoters() {
    return Collections.unmodifiableCollection(userDataTally.talliedVoters);
  }

  /**
   * @return 
   */
  public Collection<PeerIdentity> getTalliedAgreeVoters() {
    return Collections.unmodifiableCollection(userDataTally.talliedAgreeVoters);
  }

  public Result getTallyResult(int quorum, int voteMargin) {
    return resultTally.getTallyResult(quorum, voteMargin);
  }

  public Collection<PeerIdentity> getAgreeVoters() {
    return Collections.unmodifiableCollection(resultTally.agreeVoters);
  }

  public Collection<PeerIdentity> getDisagreeVoters() {
    return Collections.unmodifiableCollection(resultTally.disagreeVoters);
  }

  public Collection<PeerIdentity> getPollerOnlyBlockVoters() {
    return Collections.unmodifiableCollection(resultTally.pollerOnlyVoters);
  }

  public Collection<PeerIdentity> getVoterOnlyBlockVoters() {
    return Collections.unmodifiableCollection(resultTally.voterOnlyVoters);
  }

  /**
   * @return The subset of the voters who agree on at least one
   * version with the poller.
   */
  public Collection<PeerIdentity> getVersionAgreedVoters() {
    if (pollerHas()) {
      return getAgreeVoters();
    } else {
      // If the poller didn't have it, nobody agreed on a version.
      return Collections.EMPTY_LIST;
    }
  }

  /**
   * @return If the poller has some the URL, the subset of the voters
   * who have versions and agree on no version with the poller; if the
   * poller does not have the URL, this is always EMPTY, and the
   * voters are tallied as either voterOnly or agree.
   */
  public Collection<PeerIdentity> getNoVersionAgreedVoters() {
    if (pollerHas()) {
      return getDisagreeVoters();
    } else {
      // If the poller didn't have it, nobody disagreed on a version;
      // it's either voterOnly or agree.
      return Collections.EMPTY_LIST;
    }
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
