/*
 * $Id: HashBlockComparerImpl.java,v 1.3 2013-05-10 18:26:57 barry409 Exp $
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
import org.lockss.protocol.VoteBlock;
import org.lockss.util.Logger;


/**
 * <p>Encapsulate the comparing of {@link HashBlock} and {@link
 * VoteBlock} objects.</p>
 */
class HashBlockComparerImpl implements VoteBlockTallier.HashBlockComparer {
  /** A map from the poller's plain hash to a Version with that hash. */
  private final Map<HashResult, HashBlock.Version> plainMap;
  /** The index mapper for this poll. */
  private final V3Poller.HashIndexer hashIndexer;

  private static final Logger log = Logger.getLogger(VoteBlockTallier.class);

  private HashBlockComparerImpl(Map<HashResult, HashBlock.Version> plainMap,
				V3Poller.HashIndexer hashIndexer) {
    this.plainMap = plainMap;
    this.hashIndexer = hashIndexer;
  }

  /**
   * <p>Create a {@link HashBlockComparer} for the given {@link
   * HashBlock}.</p>
   * @param hashBlock The {@link HashBlock} with the poller's versions
   * of the file and various hashes of that version.
   * @param hashIndexer The {@link V3Poller.HashIndexer} to map a
   * voter's index into the collection of hashes in <code>hashBlock</code>.
   * @throws {@link HashResult.IllegalByteArray} if any version in
   * <code>hashBlock</code> has no hash error, and an unacceptable
   * value for its plain hash. This should not happen, and indicates a
   * defect in the hasher.
   */
  HashBlockComparerImpl(HashBlock hashBlock,
			V3Poller.HashIndexer hashIndexer) {
    this(makePlainMap(hashBlock, hashIndexer), hashIndexer);
  }

  // If the HashBlock does not have a hashing error but does not have
  // valid bytes, this class may raise IllegalByteArray. That should
  // not happen unless there is a bug in the hasher code. No attempt
  // is made to catch it at this level, and the poll will terminate.

  // If the VoteBlock does not have a hashing error but does not have
  // valid bytes, the voter may be a Black Hat trying to cause
  // mayhem. We catch and recover from that situation.

  // Make a Map from the plain hash of each version to that version.
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
    // todo(bhayes): Move logging of PARAM_LOG_UNIQUE_VERSIONS to here?
    return plainMap;
  }

  /**
   * <p>Compare the given {@link VoteBlock} to the {@link HashBlock}
   * provided at construction.</p>
   * @param voteBlock The vote returned by the voter.
   * @param participantIndex The voter's index in this poll.
   * @return true iff some version of the participant's hash matches
   * any version of the poller's hashes for that participant.
   * @throws {@link HashResult.IllegalByteArray} if the {@link
   * HashBlock} supplied at construction is found to have a version
   * with no hashing error, but a disallowed value for some nonced
   * hash. This should not happen, and indicates a defect in the
   * hasher.
   */
  public boolean compare(VoteBlock voteBlock, int participantIndex) {
    VoteBlock.Version[] vbVersions = voteBlock.getVersions();
    for (int versionIndex = 0; versionIndex < vbVersions.length;
	 versionIndex++) {
      VoteBlock.Version vbVersion = vbVersions[versionIndex];
      if (vbVersion.getHashError()) {
	log.warning("Voter "+participantIndex+" version "+versionIndex+
		    " had a hashing error.");
      } else {
	// Look up the version's plain hash in the map of the poller's
	// known plain hashes.
	HashResult voterPlainHash;
	try {
	  voterPlainHash = HashResult.make(vbVersion.getPlainHash());
	} catch (HashResult.IllegalByteArray e) {
	  log.warning("Voter "+participantIndex+" version "+versionIndex+
		      " had an IllegalByteArray plain hash.");
	  continue;
	}
	HashBlock.Version hbVersion = plainMap.get(voterPlainHash);
	if (hbVersion != null) {
	  HashResult pollerNoncedHash =
	    hashIndexer.getParticipantHash(hbVersion, participantIndex);
	  try {
	    if (pollerNoncedHash.equalsBytes(vbVersion.getHash())) {
	      return true;
	    }
	  } catch (HashResult.IllegalByteArray e) {
	    log.warning("Voter "+participantIndex+" version "+versionIndex+
			" had an IllegalByteArray.");
	    continue;
	  }
	  // The voter and poller match plain hash, but not nonced
	  // hash.
	  log.warning("Voter "+participantIndex+" version "+versionIndex+
		      " matched plain but not nonced hash.");
	}
      }
    }
    return false;
  }
}
