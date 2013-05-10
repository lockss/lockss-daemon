/*
 * $Id: TestHashBlockComparerImpl.java,v 1.3 2013-05-10 18:26:57 barry409 Exp $
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

import java.io.*;
import java.util.*;

import org.lockss.test.*;
import java.security.MessageDigest;
import org.lockss.util.*;

import org.lockss.config.*;
import org.lockss.hasher.HashBlock;
import org.lockss.hasher.HashResult;
import org.lockss.poller.*;
import org.lockss.protocol.*;


public class TestHashBlockComparerImpl extends LockssTestCase {

  // The poll has 10 voters
  private byte[][] nonces = new byte[10][];

  public void setUp() throws Exception {
    super.setUp();
    setupNonces();
  }
  
  private void setupNonces() throws Exception {
    for (int ix = 0; ix < nonces.length; ix++) {
      nonces[ix] = ByteArray.makeRandomBytes(20);
    }
  }
  
  private HashBlock makeHashBlock(String url) {
    MockCachedUrl cu = new MockCachedUrl(url);
    return new HashBlock(cu);
  }
  
  private HashBlock makeHashBlock(String url, String... contentVersions)
      throws Exception {
    HashBlock hb = makeHashBlock(url);
    for (String contentVersion : contentVersions) {
      addVersion(hb, contentVersion);
    }
    return hb;
  }

  private void addVersion(HashBlock block, String content) 
      throws Exception {
    addVersion(block, content, null);
  }

  private void addVersion(HashBlock block, String content, Throwable hashError) 
      throws Exception {
    MessageDigest[] digests = new MessageDigest[nonces.length + 1];
    for (int i = 0; i < nonces.length; i++) {
      digests[i] = MessageDigest.getInstance("SHA1");
      digests[i].update(nonces[i]);
      digests[i].update(content.getBytes());
    }
    digests[nonces.length] = MessageDigest.getInstance("SHA1");
    digests[nonces.length].update(content.getBytes());
    
    int hbVersionNum = block.getVersions().length;
    block.addVersion(0, content.length(), 
                     0, content.length(),
		     digests.length * content.length(), // total bytes hashed
                     digests, hbVersionNum, hashError);
  }

  VoteBlock makeVoteBlock(int participantIndex, String... contentVersions)
      throws Exception {
    byte[] nonce = nonces[participantIndex];
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

  class MyHashIndexer implements V3Poller.HashIndexer {
    final HashBlock hashBlock;
    
    MyHashIndexer(HashBlock hashBlock) {
      this.hashBlock = hashBlock;
    }

    @Override public HashResult getParticipantHash(HashBlock.Version version,
						   int participantIndex) {
      byte[][] hashes = version.getHashes();
      return HashResult.make(hashes[participantIndex]);
    }
    @Override public HashResult getSymmetricHash(HashBlock.Version version,
					     int symmetricParticipantIndex) {
      // Symmetric hash values are not used.
      fail();
      return null;
    }
    @Override public HashResult getPlainHash(HashBlock.Version version) {
      byte[][] hashes = version.getHashes();
      return HashResult.make(hashes[hashes.length-1]);
    }

    // Helpers for tests that want to smash hashes in HashBlock.Version.
    public void setChallengeHash(HashBlock.Version version,
				 int participantIndex,
				 byte[] bytes) {
      byte[][] hashes = version.getHashes();
      hashes[participantIndex] = bytes;
    }

    public void setPlainHash(HashBlock.Version version, byte[] bytes) {
      byte[][] hashes = version.getHashes();
      hashes[hashes.length-1] = bytes;
    }
  }

  MyHashIndexer makeHashIndexer(HashBlock hashBlock) {
    return new MyHashIndexer(hashBlock);
  }

  public void testConstruct() throws Exception {
    HashBlock hashBlock = makeHashBlock("http://example.com/foo",
					"aaa", "bbb", "ccc");
    V3Poller.HashIndexer hashIndexer = makeHashIndexer(hashBlock);
    new HashBlockComparerImpl(hashBlock, hashIndexer);
  }

  public void testCompare() throws Exception {
    HashBlock hashBlock = makeHashBlock("http://example.com/foo",
					"aaa", "bbb", "ccc");
    V3Poller.HashIndexer hashIndexer = makeHashIndexer(hashBlock);
    HashBlockComparerImpl comparer =
      new HashBlockComparerImpl(hashBlock, hashIndexer);
    
    VoteBlock voteBlock;
    voteBlock = makeVoteBlock(2, "xxx", "yyy", "zzz");
    assertFalse(comparer.compare(voteBlock, 2));

    voteBlock = makeVoteBlock(2, "zzz");
    assertFalse(comparer.compare(voteBlock, 2));

    voteBlock = makeVoteBlock(2, "aaa", "bbb", "ccc");
    assertTrue(comparer.compare(voteBlock, 2));

    voteBlock = makeVoteBlock(2, "aaa");
    assertTrue(comparer.compare(voteBlock, 2));

    voteBlock = makeVoteBlock(2, "bbb");
    assertTrue(comparer.compare(voteBlock, 2));

    voteBlock = makeVoteBlock(2, "ccc");
    assertTrue(comparer.compare(voteBlock, 2));

    voteBlock = makeVoteBlock(2, "aaa", "aaa", "aaa");
    assertTrue(comparer.compare(voteBlock, 2));
  }

  public void testComparePollerHashError() throws Exception {
    HashBlock hashBlock = makeHashBlock("http://example.com/foo",
					"aaa");
    // The "bbb" version threw.
    addVersion(hashBlock, "bbb", new Throwable("Hash error!"));
    addVersion(hashBlock, "ccc");
    V3Poller.HashIndexer hashIndexer = makeHashIndexer(hashBlock);
    HashBlockComparerImpl comparer =
      new HashBlockComparerImpl(hashBlock, hashIndexer);
    
    VoteBlock voteBlock;
    voteBlock = makeVoteBlock(2, "aaa");
    assertTrue(comparer.compare(voteBlock, 2));

    // The bbb version isn't visible.
    voteBlock = makeVoteBlock(2, "bbb");
    assertFalse(comparer.compare(voteBlock, 2));

    voteBlock = makeVoteBlock(2, "ccc");
    assertTrue(comparer.compare(voteBlock, 2));
  }

  public void testCompareVoterHashError() throws Exception {
    HashBlock hashBlock = makeHashBlock("http://example.com/foo",
					"aaa", "bbb", "ccc");
    V3Poller.HashIndexer hashIndexer = makeHashIndexer(hashBlock);
    HashBlockComparerImpl comparer =
      new HashBlockComparerImpl(hashBlock, hashIndexer);
    
    VoteBlock voteBlock;
    voteBlock = makeVoteBlock(2, "aaa", "bbb", "ccc");
    assertTrue(comparer.compare(voteBlock, 2));

    // We match, even if two of the versions threw.
    setHashError(voteBlock, 0);
    assertTrue(comparer.compare(voteBlock, 2));
    setHashError(voteBlock, 2);
    assertTrue(comparer.compare(voteBlock, 2));

    // We don't match when all versions threw.
    setHashError(voteBlock, 1);
    assertFalse(comparer.compare(voteBlock, 2));
  }

  public void testMatchPlainMismatchNonced() throws Exception {
    HashBlock hashBlock = makeHashBlock("http://example.com/foo", "aaa");
    V3Poller.HashIndexer hashIndexer = makeHashIndexer(hashBlock);
    HashBlockComparerImpl comparer =
      new HashBlockComparerImpl(hashBlock, hashIndexer);
    
    VoteBlock voteBlock;
    voteBlock = makeVoteBlock(2, "aaa");
    // Smash the voteBlock's nonced hash.
    VoteBlock.Version vbVersion = voteBlock.getVersions()[0];
    byte[] noncedHash = vbVersion.getHash();

    assertTrue(comparer.compare(voteBlock, 2));
    noncedHash[0]++;
    assertFalse(comparer.compare(voteBlock, 2));
  }

  public void testMismatchPlainMatchNonced() throws Exception {
    HashBlock hashBlock = makeHashBlock("http://example.com/foo", "aaa");
    V3Poller.HashIndexer hashIndexer = makeHashIndexer(hashBlock);
    HashBlockComparerImpl comparer =
      new HashBlockComparerImpl(hashBlock, hashIndexer);
    
    VoteBlock voteBlock;
    voteBlock = makeVoteBlock(2, "aaa");
    // Smash the voteBlock's plain hash.
    VoteBlock.Version vbVersion = voteBlock.getVersions()[0];
    byte[] plainHash = vbVersion.getPlainHash();

    assertTrue(comparer.compare(voteBlock, 2));
    plainHash[0]++;
    assertFalse(comparer.compare(voteBlock, 2));
  }

  public void testCompareVoterIllegalBytes() throws Exception {
    byte[] emptyBytes = {};
    HashBlock hashBlock = makeHashBlock("http://example.com/foo",
					"aaa", "bbb", "ccc");
    V3Poller.HashIndexer hashIndexer = makeHashIndexer(hashBlock);
    HashBlockComparerImpl comparer =
      new HashBlockComparerImpl(hashBlock, hashIndexer);
    
    VoteBlock voteBlock;
    voteBlock = makeVoteBlock(2, "aaa");
    assertTrue(comparer.compare(voteBlock, 2));

    // No match, but the issue with the voter having no hash error and
    // illegal nonced bytes is smothered.
    setChallengeHash(voteBlock, 0, null);
    assertFalse(comparer.compare(voteBlock, 2));
    setChallengeHash(voteBlock, 0, emptyBytes);
    assertFalse(comparer.compare(voteBlock, 2));

    // re-set voteBlock and smash the plain hash. Also no match, and
    // smothered error.
    voteBlock = makeVoteBlock(2, "aaa");
    setPlainHash(voteBlock, 0, null);
    assertFalse(comparer.compare(voteBlock, 2));
    setPlainHash(voteBlock, 0, emptyBytes);
    assertFalse(comparer.compare(voteBlock, 2));
  }

  public void testComparePollerIllegalPlainBytes() throws Exception {
    byte[] emptyBytes = {};
    HashBlock hashBlock = makeHashBlock("http://example.com/foo",
					"aaa", "bbb", "ccc");
    MyHashIndexer hashIndexer;
    hashIndexer = makeHashIndexer(hashBlock);

    // smash the plain hash. Can't even create a HashBlockComparerImpl.
    hashIndexer.setPlainHash(hashBlock.getVersions()[0], null);
    try {
      new HashBlockComparerImpl(hashBlock, hashIndexer);
      fail();
    } catch (HashResult.IllegalByteArray e) {
      // expected.
    }

    hashIndexer.setPlainHash(hashBlock.getVersions()[0], emptyBytes);
    try {
      new HashBlockComparerImpl(hashBlock, hashIndexer);
      fail();
    } catch (HashResult.IllegalByteArray e) {
      // expected.
    }
  }

  public void testComparePollerIllegalChallengeBytes() throws Exception {
    // If the plain hashes don't match, illegal bytes in the challenge
    // hash aren't detected.
    byte[] emptyBytes = {};
    VoteBlock voteBlock = makeVoteBlock(2, "xxx");

    for (int i = 0; i < 3; i++) {
      HashBlock hashBlock = makeHashBlock("http://example.com/foo",
					  "aaa", "bbb", "ccc");
      HashBlock.Version hbVersion = hashBlock.getVersions()[i];

      MyHashIndexer hashIndexer = makeHashIndexer(hashBlock);
      HashBlockComparerImpl comparer =
	new HashBlockComparerImpl(hashBlock, hashIndexer);

      hashIndexer.setChallengeHash(hbVersion, 2, null);
      assertFalse(comparer.compare(voteBlock, 2));

      hashIndexer.setChallengeHash(hbVersion, 2, emptyBytes);
      assertFalse(comparer.compare(voteBlock, 2));
    }

    // Clobbering a version which does match will throw.
    voteBlock = makeVoteBlock(2, "bbb");
    HashBlock hashBlock = makeHashBlock("http://example.com/foo",
					"aaa", "bbb", "ccc");
    HashBlock.Version hbVersion = hashBlock.getVersions()[1];
    
    MyHashIndexer hashIndexer = makeHashIndexer(hashBlock);
    HashBlockComparerImpl comparer =
      new HashBlockComparerImpl(hashBlock, hashIndexer);
    assertTrue(comparer.compare(voteBlock, 2));

    hashIndexer.setChallengeHash(hbVersion, 2, null);
    try {
      comparer.compare(voteBlock, 2);
      fail();
    } catch (HashResult.IllegalByteArray e) {
      // expected.
    }

    hashIndexer.setChallengeHash(hbVersion, 2, emptyBytes);
    try {
      comparer.compare(voteBlock, 2);
      fail();
    } catch (HashResult.IllegalByteArray e) {
      // expected.
    }
  }
}
