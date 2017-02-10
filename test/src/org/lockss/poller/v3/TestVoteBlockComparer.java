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

import org.lockss.test.*;
import java.security.MessageDigest;

import org.lockss.hasher.HashResult;
import org.lockss.protocol.VoteBlock;
import org.lockss.util.ByteArray;


/**
 * Test VoteBlockComparer with a general getNoncedForPlain function.
 */
public class TestVoteBlockComparer extends LockssTestCase {

  private byte[] nonce = ByteArray.makeRandomBytes(20);

  VoteBlock makeVoteBlock(byte[] nonce, String... contentVersions)
      throws Exception {
    VoteBlock vb = new VoteBlock("foo", VoteBlock.CONTENT_VOTE);
    for (String content: contentVersions) {
      MessageDigest digest = MessageDigest.getInstance("SHA1");
      digest.update(content.getBytes());
      byte[] plainHash = digest.digest();
      digest = MessageDigest.getInstance("SHA1");
      digest.update(nonce);
      digest.update(content.getBytes());
      byte[] noncedHash = digest.digest();
      vb.addVersion(0, 123, 0, 155, plainHash, noncedHash, false);
    }
    return vb;
  }

  void setHashError(VoteBlock voteBlock, int versionIndex) {
    voteBlock.getVersions()[versionIndex].setHashError(true);
  }

  void setChallengeHash(VoteBlock voteBlock, int versionIndex, byte[] bytes) {
    voteBlock.getVersions()[versionIndex].setChallengeHash(bytes);
  }

  void setPlainHash(VoteBlock voteBlock, int versionIndex, byte[] bytes) {
    voteBlock.getVersions()[versionIndex].setPlainHash(bytes);
  }

  public VoteBlockComparer makeVoteBlockComparer(byte[] nonce,
						 String... contentVersions) 
  throws Exception {
    final Map<HashResult, HashResult> plainMap =
      new HashMap<HashResult, HashResult>();
    for (String content: contentVersions) {
      MessageDigest digest = MessageDigest.getInstance("SHA1");
      digest.update(content.getBytes());
      byte[] plainHash = digest.digest();
      digest = MessageDigest.getInstance("SHA1");
      digest.update(nonce);
      digest.update(content.getBytes());
      byte[] noncedHash = digest.digest();
      plainMap.put(HashResult.make(plainHash),
			HashResult.make(noncedHash));
    }
    
    VoteBlockComparer.NoncedForPlain noncedForPlain =
      new VoteBlockComparer.NoncedForPlain() {
	public HashResult getNoncedForPlain(HashResult plainHash) {
	  return plainMap.get(plainHash);
	}
      };
    return new VoteBlockComparer(noncedForPlain);
  }

  public void testCompare() throws Exception {
    VoteBlockComparer comparer =
      makeVoteBlockComparer(nonce, "aaa", "bbb", "ccc");
    VoteBlock voteBlock;

    voteBlock = makeVoteBlock(nonce, "xxx", "yyy", "zzz");
    assertFalse(comparer.sharesVersion(voteBlock));

    voteBlock = makeVoteBlock(nonce, "zzz");
    assertFalse(comparer.sharesVersion(voteBlock));

    voteBlock = makeVoteBlock(nonce, "aaa", "bbb", "ccc");
    assertTrue(comparer.sharesVersion(voteBlock));

    voteBlock = makeVoteBlock(nonce, "aaa");
    assertTrue(comparer.sharesVersion(voteBlock));

    voteBlock = makeVoteBlock(nonce, "bbb");
    assertTrue(comparer.sharesVersion(voteBlock));

    voteBlock = makeVoteBlock(nonce, "ccc");
    assertTrue(comparer.sharesVersion(voteBlock));

    voteBlock = makeVoteBlock(nonce, "aaa", "aaa", "aaa");
    assertTrue(comparer.sharesVersion(voteBlock));
  }

  public void testCompareVoterHashError() throws Exception {
    VoteBlockComparer comparer =
      makeVoteBlockComparer(nonce, "aaa", "bbb", "ccc");
    VoteBlock voteBlock;

    voteBlock = makeVoteBlock(nonce, "aaa", "bbb", "ccc");
    assertTrue(comparer.sharesVersion(voteBlock));

    // We match, even if two of the versions threw.
    setHashError(voteBlock, 0);
    assertTrue(comparer.sharesVersion(voteBlock));
    setHashError(voteBlock, 2);
    assertTrue(comparer.sharesVersion(voteBlock));

    // We don't match when all versions threw.
    setHashError(voteBlock, 1);
    assertFalse(comparer.sharesVersion(voteBlock));
  }

  public void testMatchPlainMismatchNonced() throws Exception {
    VoteBlockComparer comparer =
      makeVoteBlockComparer(nonce, "aaa", "bbb", "ccc");
    VoteBlock voteBlock;

    voteBlock = makeVoteBlock(nonce, "aaa");
    // Smash the voteBlock's nonced hash.
    VoteBlock.Version vbVersion = voteBlock.getVersions()[0];
    byte[] noncedHash = vbVersion.getHash();

    assertTrue(comparer.sharesVersion(voteBlock));
    noncedHash[0]++;
    assertFalse(comparer.sharesVersion(voteBlock));
  }

  public void testMismatchPlainMatchNonced() throws Exception {
    VoteBlockComparer comparer =
      makeVoteBlockComparer(nonce, "aaa", "bbb", "ccc");
    VoteBlock voteBlock;

    voteBlock = makeVoteBlock(nonce, "aaa");
    // Smash the voteBlock's plain hash.
    VoteBlock.Version vbVersion = voteBlock.getVersions()[0];
    byte[] plainHash = vbVersion.getPlainHash();

    assertTrue(comparer.sharesVersion(voteBlock));
    plainHash[0]++;
    assertFalse(comparer.sharesVersion(voteBlock));
  }

  public void testCompareVoterIllegalBytes() throws Exception {
    byte[] emptyBytes = ByteArray.EMPTY_BYTE_ARRAY;
    VoteBlockComparer comparer =
      makeVoteBlockComparer(nonce, "aaa", "bbb", "ccc");
    VoteBlock voteBlock;

    voteBlock = makeVoteBlock(nonce, "aaa");
    assertTrue(comparer.sharesVersion(voteBlock));

    // No match, but the issue with the voter having no hash error and
    // illegal nonced bytes is smothered.
    setChallengeHash(voteBlock, 0, null);
    assertFalse(comparer.sharesVersion(voteBlock));
    setChallengeHash(voteBlock, 0, emptyBytes);
    assertFalse(comparer.sharesVersion(voteBlock));

    // re-set voteBlock and smash the plain hash. Also no match, and
    // smothered error.
    voteBlock = makeVoteBlock(nonce, "aaa");
    setPlainHash(voteBlock, 0, null);
    assertFalse(comparer.sharesVersion(voteBlock));
    setPlainHash(voteBlock, 0, emptyBytes);
    assertFalse(comparer.sharesVersion(voteBlock));
  }
 }
