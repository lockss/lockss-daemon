/*
 * $Id$
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

import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.hasher.HashBlock;
import org.lockss.hasher.HashResult;
import org.lockss.protocol.VoteBlock;
import org.lockss.util.Logger;

/**
 * Representation of the tally of many votes on an individual
 * URL. This class encapsulates what it means to compare a VoteBlock
 * to a HashBlock.
 */
public class VoteBlockTallier implements UrlTallier.VoteCallback {

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

  /** A callback for when VoteBlockTallier.vote() is called. */
  public interface VoteCallback {
    /**
     * When VoteBlockTallier.vote() is called, VoteCallback.vote()
     * will be called.
     */
    public void vote(VoteBlock voteBlock, ParticipantUserData id);
  }

  // Null if the poller does not have this URL.
  private final HashBlockVoteBlockComparerFactory comparerFactory;

  private final Collection<VoteBlockTally> tallies =
    new ArrayList<VoteBlockTally>();
  private BlockTally blockTally;
  // todo(bhayes): This points to a need for a Builder. This class is
  // bi-modal, and a Builder/Tallier split would be an improvement for
  // other reasons as well.
  private boolean votingStarted = false;

  private static final Logger log = Logger.getLogger("VoteBlockTallier");

  private VoteBlockTallier(HashBlockVoteBlockComparerFactory comparerFactory) {
    this.comparerFactory = comparerFactory;
  }

  // local short-cut to make for shorter lines
  private static HashBlockVoteBlockComparerFactory
    makeComparerFactory(HashBlock hashBlock,
			V3Poller.HashIndexer hashIndexer) {
    return HashBlockVoteBlockComparerFactory.
      makeFactory(hashBlock, hashIndexer);
  }

  /**
   * Make a tallier for a URL the poller does not have.
   * @param voteCallbacks Each VoteCallback will have its vote()
   * method called when this VoteBlockTallier's vote() method is
   * called.
   */
  public static VoteBlockTallier make(VoteCallback... voteCallbacks) {
    return make((HashBlockVoteBlockComparerFactory)null, voteCallbacks);
  }

  /**
   * Make a tallier for a URL the poller has.
   * @param voteCallbacks Each VoteCallback will have its vote()
   * method called when this VoteBlockTallier's vote() method is
   * called.
   */
  public static VoteBlockTallier make(HashBlock hashBlock,
				      V3Poller.HashIndexer hashIndexer,
				      VoteCallback... voteCallbacks) {
    return make(makeComparerFactory(hashBlock, hashIndexer),
		voteCallbacks);
  }

  /**
   * Make a tallier for a URL the poller has, when doing repair.
   */
  public static VoteBlockTallier makeForRepair(HashBlock hashBlock,
					       V3Poller.HashIndexer hashIndexer) {
    return makeForRepair(makeComparerFactory(hashBlock, hashIndexer));
  }

  // package level for testing, rather than private
  static VoteBlockTallier 
    makeForRepair(HashBlockVoteBlockComparerFactory comparerFactory) {
    return new VoteBlockTallier(comparerFactory);
  }

  // package level for testing, rather than private
  static VoteBlockTallier
    make(HashBlockVoteBlockComparerFactory comparerFactory,
	 final VoteCallback... voteCallbacks) {
    if (voteCallbacks.length == 0) {
      return new VoteBlockTallier(comparerFactory);
    }
    return new VoteBlockTallier(comparerFactory) {
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

  public void addBlockTally(BlockTally blockTally) {
    addTally(blockTally);
    this.blockTally = blockTally;
  }

  public BlockTally getBlockTally() {
    return this.blockTally;
  }

  // This could be done by using dispatch and a subclass.
  private boolean pollerHas() {
    return comparerFactory != null;
  }

  /**
   * Vote using all the versions of the voter's VoteBlock.
   */
  public void vote(VoteBlock voteBlock, ParticipantUserData id,
		   int participantIndex) {
    votingStarted = true;
    if (pollerHas()) {
      VoteBlockComparer comparer = comparerFactory.forIndex(participantIndex);
      if (comparer.sharesVersion(voteBlock)) {
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
}
