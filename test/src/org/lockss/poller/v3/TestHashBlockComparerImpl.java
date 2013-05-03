/*
 * $Id: TestHashBlockComparerImpl.java,v 1.1 2013-05-03 20:21:31 barry409 Exp $
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

  V3Poller.HashIndexer makeHashIndexer(final HashBlock hashBlock) {
    return new V3Poller.HashIndexer() {
      public HashResult getParticipantHash(HashBlock.Version version,
					   int participantIndex) {
	byte[][] hashes = version.getHashes();
	return HashResult.make(hashes[participantIndex]);
      }
      public HashResult getSymmetricHash(HashBlock.Version version,
					 int symmetricParticipantIndex) {
	// Symmetric hash values are not used.
	fail();
	return null;
      }
      public HashResult getPlainHash(HashBlock.Version version) {
	byte[][] hashes = version.getHashes();
	return HashResult.make(hashes[hashes.length-1]);
      }
    };
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
}
