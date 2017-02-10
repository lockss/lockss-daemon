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

import org.lockss.poller.v3.V3Poller;
import org.lockss.protocol.VoteBlock;
import org.lockss.protocol.VoteBlocks;
import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.util.Logger;

/**
 * An implementaion of {@link VoteBlockComparer}. This is used to
 * compare a poller's {@link VoteBlock} against a voter's {@link
 * VoteBlock} at a voter in a symmetric poll, and a pollers {@link
 * HashBlock} against a voter's {@link VoteBlock} at the poller.
 */
public final class VoteBlockComparer {

  private static final Logger log = Logger.getLogger(V3Poller.class);

  public interface NoncedForPlain {
    /**
     * @param A plain hash value.
     * @return A {@link HashResult} for the nonced hash given the
     * plain hash; returns null if no version has that plain hash.
     */
    public HashResult getNoncedForPlain(HashResult plainHash);
  }

  private final NoncedForPlain noncedForPlain;

  public VoteBlockComparer(NoncedForPlain noncedForPlain) {
    this.noncedForPlain = noncedForPlain;
  }

  /**
   * <p>Compare the given {@link VoteBlock} to the {@link HashBlock}
   * or {@link VoteBlock} provided at construction.</p>
   * @param voteBlock The vote returned by the other peer.
   * @return true iff some version of this peer's hash matches
   * any version of the other peer's hashes for that participant.
   * @throws {@link HashResult.IllegalByteArray} if the {@link
   * HashBlock} or {@link VoteBlock} supplied at construction is found
   * to have a version with no hashing error, but a disallowed value
   * for some nonced hash. This should not happen, and indicates a
   * defect in the local hasher.
   */
  public final boolean sharesVersion(VoteBlock voteBlock) {
    VoteBlock.Version[] theirVersions = voteBlock.getVersions();
    for (int versionIndex = 0; versionIndex < theirVersions.length;
	 versionIndex++) {
      VoteBlock.Version theirVersion = theirVersions[versionIndex];
      if (theirVersion.getHashError()) {
	log.warning("Other had a hashing error at version "+versionIndex);
      } else {
	// Look up the other's plain hash in the map of our known
	// plain hashes.
	HashResult otherPlainHash;
	try {
	  otherPlainHash = HashResult.make(theirVersion.getPlainHash());
	} catch (HashResult.IllegalByteArray e) {
	  log.warning("Other had an IllegalByteArray plain hash.");
	  continue;
	}
	HashResult ourNoncedHash =
	  noncedForPlain.getNoncedForPlain(otherPlainHash);
	if (ourNoncedHash != null) {
	  // We match plain hash; the normal course is that the nonced
	  // hashes will also match, and we'll return true.
	  try {
	    if (ourNoncedHash.equalsBytes(theirVersion.getHash())) {
	      return true;
	    }
	  } catch (HashResult.IllegalByteArray e) {
	    log.warning("Other's version "+versionIndex+
			" had an IllegalByteArray.");
	    continue;
	  }
	  // Both match plain hash, but not nonced hash.
	  log.warning("Matched plain but not nonced hash.");
	}
      }
    }
    return false;
  }
}