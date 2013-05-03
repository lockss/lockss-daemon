/*
 * $Id: HashBlockComparerImpl.java,v 1.1 2013-05-03 20:21:31 barry409 Exp $
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

import org.lockss.hasher.HashBlock;
import org.lockss.hasher.HashResult;
import org.lockss.protocol.VoteBlock;
import org.lockss.util.Logger;


/**
 * <p>Encapsulate the comparing of {@link HashBlock} and {@link
 * VoteBlock} objects.</p>
 */
class HashBlockComparerImpl implements VoteBlockTallier.HashBlockComparer {
  private final HashBlock.Version[] hbVersions;
  private final V3Poller.HashIndexer hashIndexer;

  private static final Logger log = Logger.getLogger("HashBlockComparerImpl");

  /**
   * <p>Create a {@link HashBlockComparer} for the given {@link
   * HashBlock}.</p>
   */
  HashBlockComparerImpl(HashBlock hashBlock,
			V3Poller.HashIndexer hashIndexer) {
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
    this.hashIndexer = hashIndexer;
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
	HashResult hbHash = hashIndexer.getParticipantHash(hbVersion,
							   participantIndex);
	VoteBlock.Version[] vbVersions = voteBlock.getVersions();
	for (int versionIndex = 0; versionIndex < vbVersions.length;
	     versionIndex++) {
	  VoteBlock.Version vbVersion = vbVersions[versionIndex];
	  if (vbVersion.getHashError()) {
	    log.warning("Voter version "+versionIndex+
			" had a hashing error.");
	  } else {
	    HashResult vbHash = HashResult.make(vbVersion.getHash());
	    if (hbHash.equals(vbHash)) {
	      return true;
	    }
	  }
	}
      }
    }
    return false;
  }
}
