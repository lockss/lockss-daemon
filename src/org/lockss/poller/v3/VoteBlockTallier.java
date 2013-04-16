/*
 * $Id: VoteBlockTallier.java,v 1.9 2013-04-16 22:57:28 barry409 Exp $
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

import org.lockss.hasher.HashBlock;
import org.lockss.protocol.VoteBlock;
import org.lockss.util.Logger;


/**
 * Representation of the tally of many votes on an individual
 * URL. This class encapsulates what it means to compare a VoteBlock
 * to a HashBlock.
 */
public class VoteBlockTallier {

  // todo(bhayes): There should be an enum of these things. But that
  // leads to EnumMap<Vote, Long> with boxed longs.
  interface VoteBlockTally {
    public void voteAgreed(ParticipantUserData id);
    public void voteDisagreed(ParticipantUserData id);
    public void votePollerOnly(ParticipantUserData id);
    public void voteVoterOnly(ParticipantUserData id);
    public void voteNeither(ParticipantUserData id);
    public void voteSpoiled(ParticipantUserData id);
  }

  interface HashBlockComparer {
    public boolean compare(VoteBlock voteBlock, int participantIndex);
  }

  /** A callback for when VoteBlockTallier.vote() is called. */
  public interface VoteCallback {
    /**
     * When VoteBlockTallier.vote() is called, VoteCallback.vote()
     * will be called.
     */
    public void vote(VoteBlock voteBlock, ParticipantUserData id);
  }

  // Null if the poller does not have this URL.
  private final HashBlockComparer comparer;
  private final Collection<VoteBlockTally> tallies =
    new ArrayList<VoteBlockTally>();
  private boolean votingStarted = false;

  private static final Logger log = Logger.getLogger("VoteBlockTallier");

  private VoteBlockTallier(HashBlockComparer comparer) {
    this.comparer = comparer;
  }

  /**
   * Make a tallier for a URL the poller does not have.
   * @param voteCallbacks Each VoteCallback will have its vote()
   * method called when this VoteBlockTallier's vote() method is
   * called.
   */
  public static VoteBlockTallier make(VoteCallback... voteCallbacks) {
    return make((HashBlockComparer)null, voteCallbacks);
  }

  /**
   * Make a tallier for a URL the poller has.
   * @param voteCallbacks Each VoteCallback will have its vote()
   * method called when this VoteBlockTallier's vote() method is
   * called.
   */
  public static VoteBlockTallier make(HashBlock hashBlock,
				      VoteCallback... voteCallbacks) {
    return make(new HashBlockComparerImpl(hashBlock), voteCallbacks);
  }

  /**
   * Make a tallier for a URL the poller has, when doing repair.
   */
  public static VoteBlockTallier makeForRepair(HashBlock hashBlock) {
    return makeForRepair(new HashBlockComparerImpl(hashBlock));
  }

  // package level for testing, rather than private
  static VoteBlockTallier makeForRepair(HashBlockComparer comparer) {
    return new VoteBlockTallier(comparer);
  }

  // package level for testing, rather than private
  static VoteBlockTallier make(HashBlockComparer comparer,
			       final VoteCallback... voteCallbacks) {
    if (voteCallbacks.length == 0) {
      return new VoteBlockTallier(comparer);
    }
    return new VoteBlockTallier(comparer) {
      // In addition to tallying, inform the ParticipantUserData block
      // about each vote.
      @Override public void vote(VoteBlock voteBlock, ParticipantUserData id,
				 int participantIndex) {
	for (VoteCallback voteCallback: voteCallbacks) {
	  voteCallback.vote(voteBlock, id);
	}
	super.vote(voteBlock, id, participantIndex);
      }
    };
  }

  /**
   * Add a tally which will be informed of each vote.
   */
  public void addTally(VoteBlockTally tally) {
    if (votingStarted) {
      throw new IllegalStateException("tally added after voting started.");
    }
    tallies.add(tally);
  }

  // This could be done by using dispatch and a subclass.
  private boolean pollerHas() {
    return comparer != null;
  }

  /**
   * Vote using all the versions of the voter's VoteBlock.
   */
  public void vote(VoteBlock voteBlock, ParticipantUserData id,
		   int participantIndex) {
    votingStarted = true;
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
  public void voteMissing(ParticipantUserData id) {
    votingStarted = true;
    if (pollerHas()) {
      votePollerOnly(id);
    } else {
      voteNeither(id);
    }
  }

  /**
   * The voter is unable to cast a meaningful vote.
   */
  public void voteSpoiled(ParticipantUserData id) {
    votingStarted = true;
    log.debug3(id+"  spoiled");
    for (VoteBlockTally tally: tallies) {
      tally.voteSpoiled(id);
    }
  }

  void voteAgreed(ParticipantUserData id) {
    log.debug3(id+"  agreed");
    for (VoteBlockTally tally: tallies) {
      tally.voteAgreed(id);
    }
  }

  void voteDisagreed(ParticipantUserData id) {
    log.debug3(id+"  disagreed");
    for (VoteBlockTally tally: tallies) {
      tally.voteDisagreed(id);
    }
  }

  void votePollerOnly(ParticipantUserData id) {
    log.debug3(id+"  didn't have");
    for (VoteBlockTally tally: tallies) {
      tally.votePollerOnly(id);
    }
  }

  void voteVoterOnly(ParticipantUserData id) {
    log.debug3(id+"  voterOnly");
    for (VoteBlockTally tally: tallies) {
      tally.voteVoterOnly(id);
    }
  }

  void voteNeither(ParticipantUserData id) {
    log.debug3(id+"  neither have");
    for (VoteBlockTally tally: tallies) {
      tally.voteNeither(id);
    }
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
	  // todo(bhayes): the participantIndex could be removed if
	  // HashBlock could return versions keyed by ParticipantUserData.
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
