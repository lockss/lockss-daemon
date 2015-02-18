/*
 * $Id$
 */

/*

  Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.hasher.HashResult;
import org.lockss.util.Logger;


/**
 * Create {@link VoteBlockComparer} instances from {@link HashBlock}
 * instances. This is used in standard polls.
 * Use:
 *
 * HashBlockVoteBlockComparerFactory factory =
 *    HashBlockVoteBlockComparerFactory.makeFactory(hashBlock, hashIndexer);
 * VoteBlockComparer comparer = factory.forIndex(participantIndex);
 */
public final class HashBlockVoteBlockComparerFactory {
  private static final Logger log = Logger.getLogger(V3Poller.class);

  /**
   * Build a Map from the HashResult for each version in the HashBlock
   * to that HashBlock.Version. The lookup by HashResult is fast, and
   * HashResult instances are created on demand by getNoncedForPlain.
   */
  private static Map<HashResult, HashBlock.Version>
    makePlainMap(HashBlock hashBlock,
		 V3Poller.HashIndexer hashIndexer) {

    Map<HashResult, HashBlock.Version> plainMap =
      new HashMap<HashResult, HashBlock.Version>();
    HashBlock.Version[] hbVersions = hashBlock.getVersions();
    for (int versionIndex = 0; versionIndex < hbVersions.length;
	 versionIndex++) {
      HashBlock.Version hbVersion = hbVersions[versionIndex];
      if (hbVersion.getHashError() != null) {
	// Only log the hash error in the HashBlock at
	// initialize. There's probably a more detailed error in the
	// log.
	log.warning("HashBlock version "+versionIndex+" had hashing error.");
      } else {
	HashResult plainHash = hashIndexer.getPlainHash(hbVersion);
	
	// We could check for a plainHash already in the map, and
	// check that all the nonced hashes match. If they do not,
	// something odd is happening in the hasher. But it's not
	// worth it.
	plainMap.put(plainHash, hbVersion);
      }
    }
    return plainMap;
  }

  private final Map<HashResult, HashBlock.Version> plainMap;
  private final V3Poller.HashIndexer hashIndexer;  

  private HashBlockVoteBlockComparerFactory(
    Map<HashResult, HashBlock.Version> plainMap,
    V3Poller.HashIndexer hashIndexer) {

    this.plainMap = plainMap;
    this.hashIndexer = hashIndexer;
  }

  /**
   * @param hashBlock A {@link HashBlock}, with plain and nonced
   * hashes for each {@link HashBloock.Version} and perhaps several
   * different voters.
   * @param hashIndexer A helper to map from a poll's participant
   * indexes to entries in the {@link HashBlock}.
   * @return An instance. See {@link #forIndex} for use.
   */
  public static HashBlockVoteBlockComparerFactory
      makeFactory(HashBlock hashBlock, V3Poller.HashIndexer hashIndexer) {
    Map<HashResult, HashBlock.Version> plainMap =
      makePlainMap(hashBlock, hashIndexer);
    return new HashBlockVoteBlockComparerFactory(plainMap, hashIndexer);
  }

  /**
   * A private implementation of VoteBlockComparer.NoncedForPlain.
   * HashResult instances are created on demand; semantic errors in
   * the local VoteBlock passed in at initialization [which could have
   * been created by a defective Hasher] are not caught, but are
   * thrown and intended to stop the poll.
   */
  private class NoncedForPlainImpl implements VoteBlockComparer.NoncedForPlain {
    private final int participantIndex;

    NoncedForPlainImpl(int participantIndex) {
      this.participantIndex = participantIndex;
    }

    public HashResult getNoncedForPlain(HashResult plainHash) {
      HashBlock.Version version = plainMap.get(plainHash);
      if (version == null) {
	return null;
      }
      return hashIndexer.getParticipantHash(version, participantIndex);
    }
  }

  /**
   * Create a VoteBlockComparer specific to the given participantIndex.
   * @param participantIndex The poller's index of the participant.
   * @return A {@link VoteBlockComparer} instance to compare the
   * indicated participant's hashes in the {@link HashBlock} provided
   * at construction.
   */
  public VoteBlockComparer forIndex(int participantIndex) {
    NoncedForPlainImpl noncedForPlain =
      new NoncedForPlainImpl(participantIndex);
    return new VoteBlockComparer(noncedForPlain);
  }
}
