/*
 * $Id: BlockTally.java,v 1.22 2012-03-13 23:41:01 barry409 Exp $
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
import org.lockss.util.*;
import org.lockss.config.*;

/**
 * Representation of the tally for an individual vote block.
 */
public class BlockTally<T> {

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

  interface VoteTally<U> {
    public void voteSpoiled(U id);
    public void voteAgreed(U id);
    public void voteDisagreed(U id);
    public void voteVoterOnly(U id);
    public void votePollerOnly(U id);
    public void voteNeither(U id);
  }

  // Null if the poller does not have this URL.
  private final HashBlockComparer comparer;

  final UserDataTally<T> userDataTally = new UserDataTally<T>();
  final ResultTally<T> resultTally = new ResultTally<T>();

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
  public void vote(VoteBlock voteBlock, T id, int participantIndex) {
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
  public void voteMissing(T id) {
    if (pollerHas()) {
      votePollerOnly(id);
    } else {
      voteNeither(id);
    }
  }

  /**
   * The voter is unable to cast a meaningful vote.
   */
  public void voteSpoiled(T id) {
    log.debug3(id+"  spoiled");
    userDataTally.voteSpoiled(id);
    resultTally.voteSpoiled(id);
  }

  void voteAgreed(T id) {
    log.debug3(id+"  agreed");
    userDataTally.voteAgreed(id);
    resultTally.voteAgreed(id);
  }

  void voteDisagreed(T id) {
    log.debug3(id+"  disagreed");
    userDataTally.voteDisagreed(id);
    resultTally.voteDisagreed(id);
  }

  void voteVoterOnly(T id) {
    log.debug3(id+"  voterOnly");
    userDataTally.voteVoterOnly(id);
    resultTally.voteVoterOnly(id);
  }

  void votePollerOnly(T id) {
    log.debug3(id+"  didn't have");
    userDataTally.votePollerOnly(id);
    resultTally.votePollerOnly(id);
  }

  void voteNeither(T id) {
    log.debug3(id+"  neither have");
    userDataTally.voteNeither(id);
    resultTally.voteNeither(id);
  }

  /**
   * @return
   */
  public Collection<T> getTalliedVoters() {
    return Collections.unmodifiableCollection(userDataTally.talliedVoters);
  }

  /**
   * @return 
   */
  public Collection<T> getTalliedAgreeVoters() {
    return Collections.unmodifiableCollection(userDataTally.talliedAgreeVoters);
  }

  public Result getTallyResult(int quorum, int voteMargin) {
    return resultTally.getTallyResult(quorum, voteMargin);
  }

  public Collection<T> getAgreeVoters() {
    return Collections.unmodifiableCollection(resultTally.agreeVoters);
  }

  public Collection<T> getDisagreeVoters() {
    return Collections.unmodifiableCollection(resultTally.disagreeVoters);
  }

  public Collection<T> getPollerOnlyBlockVoters() {
    return Collections.unmodifiableCollection(resultTally.pollerOnlyVoters);
  }

  public Collection<T> getVoterOnlyBlockVoters() {
    return Collections.unmodifiableCollection(resultTally.voterOnlyVoters);
  }

  /**
   * @return The subset of the voters who agree on at least one
   * version with the poller.
   */
  public Collection<T> getVersionAgreedVoters() {
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
  public Collection<T> getNoVersionAgreedVoters() {
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
