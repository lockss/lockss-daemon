/*
 * $Id: VoteBlockTallier.java,v 1.7.24.1 2013-06-08 22:25:01 dshr Exp $
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
import java.io.IOException;

import org.lockss.hasher.HashBlock;
import org.lockss.protocol.VoteBlock;
import org.lockss.protocol.VoteBlocks;
import org.lockss.util.Logger;
import org.lockss.daemon.ShouldNotHappenException;


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

  // Null if the poller does not have this URL.
  private final HashBlockComparer comparer;
  private final HashBlock hashBlock;
  private final Collection<VoteBlockTally> tallies;
  private boolean votingStarted = false;

  private static final Logger log = Logger.getLogger("VoteBlockTallier");

  /**
   * Make a tallier for a URL the poller does not have.
   */
  public VoteBlockTallier() {
    this.hashBlock = null;
    this.comparer = null;
    tallies = new ArrayList<VoteBlockTally>();
  }

  /**
   * Make a tallier for a URL the poller has.
   */
  public VoteBlockTallier(HashBlock hashBlock) {
    this.hashBlock = hashBlock;
    this.comparer = new HashBlockComparerImpl(hashBlock);
    tallies = new ArrayList<VoteBlockTally>();
  }

  /**
   * Make a tallier for testing
   */
  public VoteBlockTallier(HashBlockComparer comparer) {
    this.hashBlock = null;
    this.comparer = comparer;
    tallies = new ArrayList<VoteBlockTally>();
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

  // This should be done by using dispatch and a subclass.
  private boolean pollerHas() {
    return comparer != null;
  }

  /**
   * Vote using all the versions of the voter's VoteBlock.
   */
  public void vote(VoteBlock voteBlock, ParticipantUserData id,
		   int participantIndex) {
    votingStarted = true;
    id.addHashStats(voteBlock);
    if (pollerHas()) {
      if (comparer.compare(voteBlock, participantIndex)) {
	voteAgreed(id);
	if (id.isLocalPoll()) {
	  if (participantIndex != 0) {
	    throw new ShouldNotHappenException("Local index not 0");
	  }
	  // The content agrees with the stored hash
	  localAgreed(id, voteBlock);
	}
      } else {
	voteDisagreed(id);
	if (id.isLocalPoll()) {
	  if (participantIndex != 0) {
	    throw new ShouldNotHappenException("Local index not 0");
	  }
	  // The content disagrees with the stored hash
	  localDisagreed(id, voteBlock);
	}
      }
    } else {
      voteVoterOnly(id);
      if (id.isLocalPoll()) {
	if (participantIndex != 0) {
	  throw new ShouldNotHappenException("Local index not 0");
	}
	// There is a stored hash but no content
	localMissing(id, voteBlock);
      }
    }
  }

  /**
   * Vote that the URL is missing.
   */
  public void voteMissing(String url, ParticipantUserData id,
		   int participantIndex) {
    votingStarted = true;
    if (pollerHas()) {
      votePollerOnly(id);
      if (id.isLocalPoll()) {
	if (participantIndex != 0) {
	  throw new ShouldNotHappenException("Local index not 0: " + url);
	}
	// There is content but no stored hash
	localNoHash(id, url);
      }
    } else {
      voteNeither(id);
      if (id.isLocalPoll()) {
	// There is neither content nor a stored hash - how did we get here?
	throw new ShouldNotHappenException("Neither content nor stored hash: " +
					   url);
      }
    }
  }

  /**
   * The voter is unable to cast a meaningful vote.
   * @param id the Voter's user data
   */
  public void voteSpoiled(ParticipantUserData id) {
    votingStarted = true;
    log.debug3(id+"  spoiled");
    for (VoteBlockTally tally: tallies) {
      tally.voteSpoiled(id);
    }
  }

  /**
   * The voter and the poller agreed.
   * @param id the Voter's user data
   */
  void voteAgreed(ParticipantUserData id) {
    log.debug3(id+"  agreed");
    for (VoteBlockTally tally: tallies) {
      tally.voteAgreed(id);
    }
  }

  /**
   * The voter and the poller disagreed.
   * @param id the Voter's user data
   */
  void voteDisagreed(ParticipantUserData id) {
    log.debug3(id+"  disagreed");
    for (VoteBlockTally tally: tallies) {
      tally.voteDisagreed(id);
    }
  }

  /**
   * Only the poller had this URL
   * @param id the Voter's user data
   */
  void votePollerOnly(ParticipantUserData id) {
    log.debug3(id+"  didn't have");
    for (VoteBlockTally tally: tallies) {
      tally.votePollerOnly(id);
    }
  }

  /**
   * Only the voter had this URL
   * @param id the Voter's user data
   */
  void voteVoterOnly(ParticipantUserData id) {
    log.debug3(id+"  voterOnly");
    for (VoteBlockTally tally: tallies) {
      tally.voteVoterOnly(id);
    }
  }

  /**
   * Neither the voter nor the poller had this URL,
   * but some other voter did.
   * @param id the Voter's user data
   */
  void voteNeither(ParticipantUserData id) {
    log.debug3(id+"  neither have");
    for (VoteBlockTally tally: tallies) {
      tally.voteNeither(id);
    }
  }

  /**
   * In a local poll, the content and the stored hash agreed
   * @param id the Voter's user data
   * @param voteBlock the vote block with this URL's stored hash
   */
  void localAgreed(ParticipantUserData id, VoteBlock voteBlock) {
    // XXX
    // Copy the voteBlock to the new VoteBlocks
    VoteBlocks vbs = id.getLocalVoteBlocks();
    try {
      vbs.addVoteBlock(voteBlock);
    } catch (IOException ex) {
      // XXX what to do?
    }
  }

  /**
   * In a local poll, the content and the stored hash disagreed
   * @param id the Voter's user data
   * @param voteBlock the vote block with this URL's stored hash
   */
  void localDisagreed(ParticipantUserData id, VoteBlock voteBlock) {
    // XXX
    // Copy the voteBlock to the new VoteBlocks
    VoteBlocks vbs = id.getLocalVoteBlocks();
    try {
      vbs.addVoteBlock(voteBlock);
    } catch (IOException ex) {
      // XXX what to do?
    }
  }

  /**
   * In a local poll, there was a stored hash but no content
   * @param id the Voter's user data
   * @param voteBlock the vote block with this URL's stored hash
   */
  void localMissing(ParticipantUserData id, VoteBlock voteBlock) {
    // XXX
    // Copy the voteBlock to the new VoteBlocks
    VoteBlocks vbs = id.getLocalVoteBlocks();
    try {
      vbs.addVoteBlock(voteBlock);
    } catch (IOException ex) {
      // XXX what to do?
    }
  }

  /**
   * In a local poll, there was content but no stored hash
   * @param id the Voter's user data
   * @param url the URL of the content without a stored hash
   */
  void localNoHash(ParticipantUserData id, String url) {
    // XXX
    // Create a new VoteBlock and add it to the VoteBlocks
    VoteBlocks vbs = id.getLocalVoteBlocks();
    try {
      vbs.addVoteBlock(makeVoteBlock(id, url));
    } catch (IOException ex) {
      // XXX what to do?
    }
  }

  /**
   * In a local poll there was content but no stored hash. Create
   * and fill out a VoteBlock for all versions of this URL.
   * @param id the Voter's user data
   * @param url the URL of the content without a stored hash
   */
  VoteBlock makeVoteBlock(ParticipantUserData id, String url) {
    VoteBlock ret = new VoteBlock(url);
    // XXX actually should have a VoteBlock constructor that takes a HashBlock
    return ret;
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
