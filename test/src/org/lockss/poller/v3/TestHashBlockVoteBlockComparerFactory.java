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
import org.lockss.hasher.HashBlock;
import org.lockss.protocol.VoteBlock;
import org.lockss.util.ByteArray;


public class TestHashBlockVoteBlockComparerFactory extends LockssTestCase {

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

  public void testCompare() throws Exception {
    HashBlock ourHashBlock = makeHashBlock("http://example.com/foo",
					   "aaa", "bbb", "ccc");
    V3Poller.HashIndexer hashIndexer = makeHashIndexer(ourHashBlock);
    HashBlockVoteBlockComparerFactory factory =
      HashBlockVoteBlockComparerFactory.makeFactory(ourHashBlock, hashIndexer);
    for (int i = 0; i < nonces.length; i++) {
      VoteBlockComparer comparer = factory.forIndex(i);

      VoteBlock voteBlock;

      voteBlock = makeVoteBlock(nonces[i], "xxx", "yyy", "zzz");
      assertFalse(comparer.sharesVersion(voteBlock));

      voteBlock = makeVoteBlock(nonces[i], "zzz");
      assertFalse(comparer.sharesVersion(voteBlock));

      voteBlock = makeVoteBlock(nonces[i], "aaa", "bbb", "ccc");
      assertTrue(comparer.sharesVersion(voteBlock));

      voteBlock = makeVoteBlock(nonces[i], "aaa");
      assertTrue(comparer.sharesVersion(voteBlock));

      voteBlock = makeVoteBlock(nonces[i], "bbb");
      assertTrue(comparer.sharesVersion(voteBlock));

      voteBlock = makeVoteBlock(nonces[i], "ccc");
      assertTrue(comparer.sharesVersion(voteBlock));

      voteBlock = makeVoteBlock(nonces[i], "aaa", "aaa", "aaa");
      assertTrue(comparer.sharesVersion(voteBlock));

      // No versions in the VoteBlock is no shared version
      voteBlock = makeVoteBlock(nonces[i]);
      assertFalse(comparer.sharesVersion(voteBlock));
    }
  }

  public void testCompareNoPollerVersions() throws Exception {
    HashBlock ourHashBlock = makeHashBlock("http://example.com/foo");
    V3Poller.HashIndexer hashIndexer = makeHashIndexer(ourHashBlock);
    HashBlockVoteBlockComparerFactory factory =
      HashBlockVoteBlockComparerFactory.makeFactory(ourHashBlock, hashIndexer);
    for (int i = 0; i < nonces.length; i++) {
      VoteBlockComparer comparer = factory.forIndex(i);

      VoteBlock voteBlock;
      voteBlock = makeVoteBlock(nonces[i], "aaa", "bbb", "ccc");
      assertFalse(comparer.sharesVersion(voteBlock));

      voteBlock = makeVoteBlock(nonces[i], "aaa");
      assertFalse(comparer.sharesVersion(voteBlock));

      voteBlock = makeVoteBlock(nonces[i], "aaa", "aaa", "aaa");
      assertFalse(comparer.sharesVersion(voteBlock));

      voteBlock = makeVoteBlock(nonces[i]);
      assertFalse(comparer.sharesVersion(voteBlock));
    }
  }

  public void testCompareLocalHashError() throws Exception {
    HashBlock ourHashBlock = makeHashBlock("http://example.com/foo");
    addVersion(ourHashBlock, "aaa");
    addVersion(ourHashBlock, "bbb", new Throwable("Hash error"));
    addVersion(ourHashBlock, "ccc");
    
    V3Poller.HashIndexer hashIndexer = makeHashIndexer(ourHashBlock);
    HashBlockVoteBlockComparerFactory factory =
      HashBlockVoteBlockComparerFactory.makeFactory(ourHashBlock, hashIndexer);
    
    for (int i = 0; i < nonces.length; i++) {
      VoteBlockComparer comparer = factory.forIndex(i);

      VoteBlock voteBlock;
      voteBlock = makeVoteBlock(nonces[i], "aaa");
      assertTrue(comparer.sharesVersion(voteBlock));
      
      // This was clobbered.
      voteBlock = makeVoteBlock(nonces[i], "bbb");
      assertFalse(comparer.sharesVersion(voteBlock));

      voteBlock = makeVoteBlock(nonces[i], "ccc");
      assertTrue(comparer.sharesVersion(voteBlock));
    }
  }

  public void testCompareVoterHashError() throws Exception {
    HashBlock ourHashBlock = makeHashBlock("http://example.com/foo");
    addVersion(ourHashBlock, "aaa");
    addVersion(ourHashBlock, "bbb");
    addVersion(ourHashBlock, "ccc");
    
    V3Poller.HashIndexer hashIndexer = makeHashIndexer(ourHashBlock);
    HashBlockVoteBlockComparerFactory factory =
      HashBlockVoteBlockComparerFactory.makeFactory(ourHashBlock, hashIndexer);
    
    for (int i = 0; i < nonces.length; i++) {
      VoteBlockComparer comparer = factory.forIndex(i);

      VoteBlock voteBlock;
      voteBlock = makeVoteBlock(nonces[i], "aaa");
      assertTrue(comparer.sharesVersion(voteBlock));
      
      voteBlock = makeVoteBlock(nonces[i], "bbb");
      setHashError(voteBlock, 0);
      assertFalse(comparer.sharesVersion(voteBlock));

      voteBlock = makeVoteBlock(nonces[i], "ccc");
      assertTrue(comparer.sharesVersion(voteBlock));
    }
  }

  public void testCompareLocalIllegalPlainBytes() throws Exception {
    byte[] emptyBytes = ByteArray.EMPTY_BYTE_ARRAY;
    VoteBlockComparer comparer;
    VoteBlock voteBlock;
    HashBlock ourHashBlock = makeHashBlock("http://example.com/foo",
					   "aaa", "bbb", "ccc");
    MyHashIndexer hashIndexer = makeHashIndexer(ourHashBlock);

    HashBlockVoteBlockComparerFactory factory = 
      HashBlockVoteBlockComparerFactory.makeFactory(ourHashBlock, hashIndexer);
    for (int i = 0; i < nonces.length; i++) {
      comparer = factory.forIndex(i);
      voteBlock = makeVoteBlock(nonces[i], "aaa");
      assertTrue(comparer.sharesVersion(voteBlock));
    }

    // smash the plain hash. Trying to use this bogus HashBlock throws
    // on construction. This won't happen unless the hasher is broken.
    HashBlock.Version damagedVersion = ourHashBlock.getVersions()[1];
    hashIndexer.setPlainHash(damagedVersion, null);
    try {
      HashBlockVoteBlockComparerFactory.makeFactory(ourHashBlock, hashIndexer);
      fail();
    } catch (HashResult.IllegalByteArray ex) {
      // expected
    }

    hashIndexer.setPlainHash(damagedVersion, emptyBytes);
    try {
      HashBlockVoteBlockComparerFactory.makeFactory(ourHashBlock, hashIndexer);
      fail();
    } catch (HashResult.IllegalByteArray ex) {
      // expected
    }
  }

  public void testCompareLocalIllegalChallengeBytes() throws Exception {
    byte[] emptyBytes = ByteArray.EMPTY_BYTE_ARRAY;
    VoteBlockComparer comparer;
    VoteBlock voteBlock;
    HashBlockVoteBlockComparerFactory factory;
    HashBlock ourHashBlock = makeHashBlock("http://example.com/foo",
					   "aaa", "bbb", "ccc");
    int damaged = 4;
    MyHashIndexer hashIndexer = makeHashIndexer(ourHashBlock);

    // damage the "bbb" version.
    hashIndexer.setChallengeHash(ourHashBlock.getVersions()[1],
				 damaged, null);
    factory = HashBlockVoteBlockComparerFactory.makeFactory(
                ourHashBlock, hashIndexer);
    for (int i = 0; i < nonces.length; i++) {
      comparer = factory.forIndex(i);
      assertTrue(comparer.sharesVersion(makeVoteBlock(nonces[i], "aaa")));
      if (i != damaged) {
	// If we don't touch the damaged version, we don't throw.
	assertTrue(comparer.sharesVersion(makeVoteBlock(nonces[i], "bbb")));
      }
      assertTrue(comparer.sharesVersion(makeVoteBlock(nonces[i], "ccc")));
    }

    factory = HashBlockVoteBlockComparerFactory.makeFactory(
      ourHashBlock, hashIndexer);
    comparer = factory.forIndex(damaged);
    try {
      comparer.sharesVersion(makeVoteBlock(nonces[damaged], "bbb"));
      fail();
    } catch (HashResult.IllegalByteArray ex) {
      // expected
    }

    // Smash with emptyBytes
    hashIndexer.setChallengeHash(ourHashBlock.getVersions()[1],
				 damaged, emptyBytes);
    factory = HashBlockVoteBlockComparerFactory.makeFactory(
      ourHashBlock, hashIndexer);
    comparer = factory.forIndex(damaged);
    try {
      comparer.sharesVersion(makeVoteBlock(nonces[damaged], "bbb"));
      fail();
    } catch (HashResult.IllegalByteArray ex) {
      // expected
    }
  }
}
