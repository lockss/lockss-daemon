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

import org.lockss.hasher.HashResult;

import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.poller.v3.V3Poller;
import org.lockss.protocol.VoteBlock;
import org.lockss.util.Logger;

/**
 * Create {@link VoteBlockComparer} instances from {@link VoteBlock}
 * instances. This is used in symmetric polls.
 * Use:
 *
 * VoteBlockComparer comparer = VoteBlockVoteBlockComparerFactory.make(voteBlock);
 */
public final class VoteBlockVoteBlockComparerFactory {
  private static final Logger log = Logger.getLogger(V3Poller.class);

  /**
   * Build a Map from the HashResult for each version in the VoteBlock
   * to that VoteBlock.Version. The lookup by HashResult is fast, and
   * HashResult instances are created on demand by getNoncedForPlain.
   */
  private static Map<HashResult, VoteBlock.Version>
    makePlainMap(VoteBlock voteBlock) {

    Map<HashResult, VoteBlock.Version> plainMap =
      new HashMap<HashResult, VoteBlock.Version>();
    VoteBlock.Version[] vbVersions = voteBlock.getVersions();
    for (int versionIndex = 0; versionIndex < vbVersions.length;
	 versionIndex++) {
      VoteBlock.Version vbVersion = vbVersions[versionIndex];
      if (vbVersion.getHashError()) {
	// Only log the hash error in the VoteBlock at
	// initialize. There's probably a more detailed error in the
	// log.
	log.warning("VoteBlock version "+versionIndex+" had hashing error.");
      } else {
	HashResult plainHash = HashResult.make(vbVersion.getPlainHash());
	
	// We could check for a plainHash already in the map, and
	// check that all the nonced hashes match. If they do not,
	// something odd is happening in the hasher. But it's not
	// worth it.
	plainMap.put(plainHash, vbVersion);
      }
    }
    return plainMap;
  }

  private final Map<HashResult, VoteBlock.Version> plainMap;

  private VoteBlockVoteBlockComparerFactory(
    Map<HashResult, VoteBlock.Version> plainMap) {

    this.plainMap = plainMap;
  }

  /**
   * @param voteBlock A {@link VoteBlock}, with plain and nonced
   * hashes for each {@link HashBloock.Version}.
   * @return An instance. See {@link #make} for use.
   */
  public static VoteBlockVoteBlockComparerFactory
      makeFactory(VoteBlock voteBlock) {
    Map<HashResult, VoteBlock.Version> plainMap = makePlainMap(voteBlock);
    return new VoteBlockVoteBlockComparerFactory(plainMap);
  }

  /**
   * A private implementation of VoteBlockComparer.NoncedForPlain.
   * HashResult instances are created on demand; semantic errors in
   * the local VoteBlock passed in at initialization [which could have
   * been created by a defective Hasher] are not caught, but are
   * thrown and intended to stop the poll.
   */
  private static class NoncedForPlainImpl
    implements VoteBlockComparer.NoncedForPlain {

    private final Map<HashResult, VoteBlock.Version> plainMap;
    private NoncedForPlainImpl(Map<HashResult, VoteBlock.Version> plainMap) {
      this.plainMap = plainMap;
    }

    public HashResult getNoncedForPlain(HashResult plainHash) {
      VoteBlock.Version version = plainMap.get(plainHash);
      if (version == null) {
	return null;
      }
      return HashResult.make(version.getHash());
    }
  }

  /**
   * Create a VoteBlockComparer.
   * @return A {@link VoteBlockComparer} instance to compare the
   * {@link VoteBlock} provided at construction.
   */
  public VoteBlockComparer make() {
    NoncedForPlainImpl noncedForPlain = new NoncedForPlainImpl(plainMap);
    return new VoteBlockComparer(noncedForPlain);
  }

  /**
   * Make a VoteBlockComparer for a VoteBlock. The VoteBlock is
   * assumed to come from the local cache, and errors encountered
   * processing it may throw exceptions which should end the poll.
   * This is a short-cut for <code>makeFactory(voteBlock).make()</code>
   * @param voteBlock The local cache's VoteBlock.
   * @return A {@link VoteBlockComparer} instance.
   */
  public static VoteBlockComparer make(VoteBlock voteBlock) {
    return makeFactory(voteBlock).make();
  }
}