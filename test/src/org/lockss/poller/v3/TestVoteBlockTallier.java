/*
 * $Id$
 */

/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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
import java.security.MessageDigest;

import org.lockss.test.*;
import org.lockss.util.*;

import org.lockss.config.*;
import org.lockss.hasher.HashBlock;
import org.lockss.hasher.HashResult;
import org.lockss.poller.*;
import org.lockss.protocol.*;


public class TestVoteBlockTallier extends LockssTestCase {

  MockLockssDaemon daemon;
  private ParticipantUserData[] testPeers;
  private File tempDir;
  String tempDirPath;
  private byte[][] nonces = new byte[10][];

  private void setupNonces() throws Exception {
    for (int ix = 0; ix < nonces.length; ix++) {
      nonces[ix] = ByteArray.makeRandomBytes(20);
    }
  }

  public void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();
    tempDir = getTempDir();
    tempDirPath = tempDir.getAbsolutePath();
    Properties p = new Properties();
    p.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    p.setProperty(V3Poller.PARAM_STATE_PATH, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(p);
    IdentityManager idMgr = new V3TestUtils.NoStoreIdentityManager();
    daemon.setIdentityManager(idMgr);
    idMgr.initService(daemon);
    MockPlugin mp = new MockPlugin(daemon);
    mp.initPlugin(daemon);
    MockArchivalUnit mau = new MockArchivalUnit(mp);
    assertNotNull(mau.getPlugin());
    assertNotNull(mau.getPlugin().getDaemon());
    MockNodeManager nodeMgr = new MockNodeManager();
    daemon.setNodeManager(nodeMgr, mau);
    setupNonces();
    setupPeers();
  }
  
  private void setupPeers() throws Exception {
    testPeers = new ParticipantUserData[nonces.length];
    V3Poller poller = makeV3Poller("pollkey");
    for (int ix = 1; ix <= testPeers.length; ix++) {
      String id = String.format("TCP:[127.0.0.%d]:9729", ix);
      PeerIdentity pid = V3TestUtils.findPeerIdentity(daemon, id);
      ParticipantUserData ud = new ParticipantUserData(pid, poller, tempDir);
      testPeers[ix - 1] = ud;
    }
  }

  // Minimal poller to satisfy tests
  private V3Poller makeV3Poller(String key) throws Exception {
    MockArchivalUnit mau = new MockArchivalUnit();
    mau.setAuId("mock");
    mau.getPlugin().initPlugin(daemon);
    PollSpec ps =
      new MockPollSpec(mau, "http://www.example.com/",
		       null, null, Poll.V3_POLL);
    MockNodeManager nodeMgr = new MockNodeManager();
    daemon.setNodeManager(nodeMgr, mau);
    return new V3Poller(ps, daemon, null, key, 20000, "SHA-1");
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

  VoteBlock makeVoteBlock() throws Exception {
    return makeVoteBlock(nonces[0], "aaa", "bbb", "ccc");
  }

  VoteBlock makeVoteBlockDisagree() throws Exception {
    return makeVoteBlock(nonces[0], "xxx", "yyy", "zzz");
  }

  VoteBlock makeVoteBlockNoVersions() throws Exception {
    return makeVoteBlock(nonces[0]);
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

  private HashBlock makeHashBlock() throws Exception {
    return makeHashBlock("foo", "aaa", "bbb", "ccc");
  }

  private HashBlock makeHashBlockNoVersions() throws Exception {
    return makeHashBlock("foo");
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

  public HashBlockVoteBlockComparerFactory
      makeComparerFactory() throws Exception {
    return makeComparerFactory(makeHashBlock());
  }

  public HashBlockVoteBlockComparerFactory
      makeComparerFactoryNoVersions() throws Exception {
    return makeComparerFactory(makeHashBlockNoVersions());
  }

  public HashBlockVoteBlockComparerFactory
      makeComparerFactory(HashBlock hashBlock) throws Exception {
    return HashBlockVoteBlockComparerFactory.
      makeFactory(hashBlock, makeHashIndexer(hashBlock));
  }

  public void testConstructPollTally() {
    BlockTally tally = new BlockTally(5, 75);
    assertEquals(BlockTally.Result.NOQUORUM, tally.getTallyResult());
  }

  public void testVoteWithBlockTallyPollerHas() throws Exception {
    VoteBlockTallier voteBlockTallier;
    BlockTally tally;

    // tally.votes is: agree/disagree/pollerOnly/VoterOnly

    voteBlockTallier = VoteBlockTallier.make(makeComparerFactory());
    tally = new BlockTally(5, 75);
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteSpoiled(testPeers[0]);
    assertEquals("0/0/0/0", tally.votes());

    voteBlockTallier = VoteBlockTallier.make(makeComparerFactory());
    tally = new BlockTally(5, 75);
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteMissing(testPeers[0]);
    assertEquals("0/1/1/0", tally.votes());

    voteBlockTallier = VoteBlockTallier.make(makeComparerFactory());
    tally = new BlockTally(5, 75);
    voteBlockTallier.addTally(tally);
    voteBlockTallier.vote(makeVoteBlock(), testPeers[0], 0);
    assertEquals("1/0/0/0", tally.votes());

    voteBlockTallier = VoteBlockTallier.make(makeComparerFactory());
    tally = new BlockTally(5, 75);
    voteBlockTallier.addTally(tally);
    voteBlockTallier.vote(makeVoteBlockDisagree(), testPeers[0], 0);
    assertEquals("0/1/0/0", tally.votes());

    // VoteBlock with no versions is disagree.
    voteBlockTallier = VoteBlockTallier.make(makeComparerFactory());
    tally = new BlockTally(5, 75);
    voteBlockTallier.addTally(tally);
    voteBlockTallier.vote(makeVoteBlockNoVersions(), testPeers[0], 0);
    assertEquals("0/1/0/0", tally.votes());
  }

  public void testVoteWithBlockTallyPollerDoesntHave() throws Exception {
    VoteBlockTallier voteBlockTallier;
    BlockTally tally;

    // tally.votes is: agree/disagree/pollerOnly/VoterOnly

    voteBlockTallier = VoteBlockTallier.make();
    tally = new BlockTally(5, 75);
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteSpoiled(testPeers[0]);
    assertEquals("0/0/0/0", tally.votes());

    voteBlockTallier = VoteBlockTallier.make();
    tally = new BlockTally(5, 75);
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteMissing(testPeers[0]);
    // Both poller and voter missing is "agree"
    assertEquals("1/0/0/0", tally.votes());

    // VoterBlock with versions is disagree and poller-only
    voteBlockTallier = VoteBlockTallier.make();
    tally = new BlockTally(5, 75);
    voteBlockTallier.addTally(tally);
    voteBlockTallier.vote(makeVoteBlock(), testPeers[0], 0);
    assertEquals("0/1/0/1", tally.votes());

    // VoterBlock with no versions is still disagree and poller-only
    voteBlockTallier = VoteBlockTallier.make();
    tally = new BlockTally(5, 75);
    voteBlockTallier.addTally(tally);
    voteBlockTallier.vote(makeVoteBlockNoVersions(), testPeers[0], 0);
    assertEquals("0/1/0/1", tally.votes());
  }

  // Tests for the Poller having a URL with no versions.
  public void testVoteWithBlockTallyPollerNoVersions() throws Exception {
    VoteBlockTallier voteBlockTallier;
    BlockTally tally;

    // tally.votes is: agree/disagree/pollerOnly/VoterOnly

    voteBlockTallier = VoteBlockTallier.make(makeComparerFactoryNoVersions());
    tally = new BlockTally(5, 75);
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteSpoiled(testPeers[0]);
    assertEquals("0/0/0/0", tally.votes());

    voteBlockTallier = VoteBlockTallier.make(makeComparerFactoryNoVersions());
    tally = new BlockTally(5, 75);
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteMissing(testPeers[0]);
    assertEquals("0/1/1/0", tally.votes());

    // The VoteBlock is disagree, if VoteBlock has versions.
    voteBlockTallier = VoteBlockTallier.make(makeComparerFactoryNoVersions());
    tally = new BlockTally(5, 75);
    voteBlockTallier.addTally(tally);
    voteBlockTallier.vote(makeVoteBlock(), testPeers[0], 0);
    assertEquals("0/1/0/0", tally.votes());

    // The VoteBlock is disagree, if VoteBlock has no versions.
    voteBlockTallier = VoteBlockTallier.make(makeComparerFactoryNoVersions());
    tally = new BlockTally(5, 75);
    voteBlockTallier.addTally(tally);
    voteBlockTallier.vote(makeVoteBlockNoVersions(), testPeers[0], 0);
    assertEquals("0/1/0/0", tally.votes());
  }

  class FailVoteCallback implements VoteBlockTallier.VoteCallback {
    @Override public void vote(VoteBlock voteBlock, ParticipantUserData id) {
      fail("vote() called unexpectedly");
    }
  }

  class OnceVoteCallback implements VoteBlockTallier.VoteCallback {
    final VoteBlock voteBlock;
    final ParticipantUserData id;
    boolean called = false;

    OnceVoteCallback(VoteBlock voteBlock, ParticipantUserData id) {
      this.voteBlock = voteBlock;
      this.id = id;
    }
    @Override public void vote(VoteBlock voteBlock, ParticipantUserData id) {
      try {
	assertEquals(called, false);
	assertEquals(this.voteBlock, voteBlock);
	assertEquals(this.id, id);
      } finally {
	called = true;
      }
    }
  }

  public void testVoteCallback() throws Exception {
    ParticipantUserData voter;
    VoteBlock voteBlock = makeVoteBlock();
    VoteBlockTallier voteBlockTallier;
    OnceVoteCallback onceVoteCallback;
    VoteBlockTallier.VoteBlockTally tally;

    voter = new ParticipantUserData();
    onceVoteCallback = new OnceVoteCallback(voteBlock, voter);
    voteBlockTallier = VoteBlockTallier.make(makeComparerFactory(),
					     onceVoteCallback);
    tally = ParticipantUserData.voteTally;
    voteBlockTallier.addTally(tally);
    voteBlockTallier.vote(makeVoteBlock(), voter, 0);
    assertEquals("1/0/0/0/0/0", voter.getVoteCounts().votes());
    assertTrue(onceVoteCallback.called);

    voter = new ParticipantUserData();
    onceVoteCallback = new OnceVoteCallback(voteBlock, voter);
    voteBlockTallier = VoteBlockTallier.make(makeComparerFactory(),
					     onceVoteCallback);
    tally = ParticipantUserData.voteTally;
    voteBlockTallier.addTally(tally);
    voteBlockTallier.vote(makeVoteBlock(), voter, 1);
    assertEquals("0/1/0/0/0/0", voter.getVoteCounts().votes());
    assertTrue(onceVoteCallback.called);

    voter = new ParticipantUserData();
    voteBlockTallier = VoteBlockTallier.make(makeComparerFactory(),
					     new FailVoteCallback());
    tally = ParticipantUserData.voteTally;
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteMissing(voter);
    assertEquals("0/0/1/0/0/0", voter.getVoteCounts().votes());

    voter = new ParticipantUserData();
    onceVoteCallback = new OnceVoteCallback(voteBlock, voter);
    voteBlockTallier = VoteBlockTallier.make(onceVoteCallback);
    tally = ParticipantUserData.voteTally;
    voteBlockTallier.addTally(tally);
    voteBlockTallier.vote(makeVoteBlock(), voter, 0);
    assertEquals("0/0/0/1/0/0", voter.getVoteCounts().votes());
    assertTrue(onceVoteCallback.called);

    voteBlockTallier = VoteBlockTallier.make(makeComparerFactory(),
					     new FailVoteCallback());
    voter = new ParticipantUserData();
    tally = ParticipantUserData.voteTally;
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteSpoiled(voter);
    assertEquals("0/0/0/0/0/1", voter.getVoteCounts().votes());
  }
}
