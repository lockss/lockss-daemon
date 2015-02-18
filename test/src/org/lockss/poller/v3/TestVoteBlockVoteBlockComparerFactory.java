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


public class TestVoteBlockVoteBlockComparerFactory extends LockssTestCase {

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

  public void testCompare() throws Exception {
    VoteBlock ourVoteBlock = makeVoteBlock(nonce, "aaa", "bbb", "ccc");
    VoteBlockComparer comparer =
      VoteBlockVoteBlockComparerFactory.make(ourVoteBlock);

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

    voteBlock = makeVoteBlock(nonce);
    assertFalse(comparer.sharesVersion(voteBlock));
  }

  public void testCompareNoVoterVersions() throws Exception {
    VoteBlock ourVoteBlock = makeVoteBlock(nonce);
    VoteBlockComparer comparer =
      VoteBlockVoteBlockComparerFactory.make(ourVoteBlock);

    VoteBlock voteBlock;

    voteBlock = makeVoteBlock(nonce, "aaa", "bbb", "ccc");
    assertFalse(comparer.sharesVersion(voteBlock));

    voteBlock = makeVoteBlock(nonce, "aaa");
    assertFalse(comparer.sharesVersion(voteBlock));

    voteBlock = makeVoteBlock(nonce, "aaa", "aaa", "aaa");
    assertFalse(comparer.sharesVersion(voteBlock));

    voteBlock = makeVoteBlock(nonce);
    assertFalse(comparer.sharesVersion(voteBlock));
  }

  public void testCompareLocalHashError() throws Exception {
    VoteBlock ourVoteBlock = makeVoteBlock(nonce, "aaa", "bbb", "ccc");
    setHashError(ourVoteBlock, 1);
    VoteBlockComparer comparer =
      VoteBlockVoteBlockComparerFactory.make(ourVoteBlock);
    
    VoteBlock voteBlock;
    voteBlock = makeVoteBlock(nonce, "aaa");
    assertTrue(comparer.sharesVersion(voteBlock));

    // This was clobbered.
    voteBlock = makeVoteBlock(nonce, "bbb");
    assertFalse(comparer.sharesVersion(voteBlock));

    voteBlock = makeVoteBlock(nonce, "ccc");
    assertTrue(comparer.sharesVersion(voteBlock));
  }

  public void testCompareLocalIllegalPlainBytes() throws Exception {
    byte[] emptyBytes = ByteArray.EMPTY_BYTE_ARRAY;
    VoteBlock ourVoteBlock;
    VoteBlockComparer comparer;
    VoteBlock voteBlock;

    ourVoteBlock = makeVoteBlock(nonce, "aaa", "bbb", "ccc");
    comparer = VoteBlockVoteBlockComparerFactory.make(ourVoteBlock);
    voteBlock = makeVoteBlock(nonce, "aaa");
    assertTrue(comparer.sharesVersion(voteBlock));

    // smash the plain hash. Trying to use this bogus VoteBlock throws
    // on construction. This won't happen unless the hasher is broken.
    ourVoteBlock = makeVoteBlock(nonce, "aaa");
    setPlainHash(ourVoteBlock, 0, null);
    try {
      VoteBlockVoteBlockComparerFactory.make(ourVoteBlock);
      fail();
    } catch (HashResult.IllegalByteArray ex) {
      // expected
    }

    setPlainHash(ourVoteBlock, 0, emptyBytes);
    try {
      VoteBlockVoteBlockComparerFactory.make(ourVoteBlock);
      fail();
    } catch (HashResult.IllegalByteArray ex) {
      // expected
    }
  }

  public void testCompareLocalIllegalChallengeBytes() throws Exception {
    byte[] emptyBytes = ByteArray.EMPTY_BYTE_ARRAY;
    VoteBlock ourVoteBlock;
    VoteBlockComparer comparer;

    ourVoteBlock = makeVoteBlock(nonce, "aaa", "bbb", "ccc");
    comparer = VoteBlockVoteBlockComparerFactory.make(ourVoteBlock);

    // Damage the "bbb" vote block's challenge hash.
    setChallengeHash(ourVoteBlock, 1, null);
    comparer = VoteBlockVoteBlockComparerFactory.make(ourVoteBlock);
    // If we don't touch the damaged version, we don't throw.
    assertTrue(comparer.sharesVersion(makeVoteBlock(nonce, "aaa")));
    assertTrue(comparer.sharesVersion(makeVoteBlock(nonce, "ccc")));

    try {
      comparer.sharesVersion(makeVoteBlock(nonce, "bbb"));
      fail();
    } catch (HashResult.IllegalByteArray ex) {
      // expected
    }

    // Smash with emptyBytes
    setChallengeHash(ourVoteBlock, 0, emptyBytes);
    comparer = VoteBlockVoteBlockComparerFactory.make(ourVoteBlock);
    try {
      comparer.sharesVersion(makeVoteBlock(nonce, "bbb"));
      fail();
    } catch (HashResult.IllegalByteArray ex) {
      // expected
    }
  }
}
