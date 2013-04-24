/*
 * $Id: TestVoteBlockTallier.java,v 1.10 2013-04-24 21:51:54 barry409 Exp $
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

import org.lockss.test.*;
import org.lockss.util.*;

import org.lockss.config.*;
import org.lockss.hasher.HashBlock;
import org.lockss.poller.*;
import org.lockss.protocol.*;


public class TestVoteBlockTallier extends LockssTestCase {

  MockLockssDaemon daemon;
  private ParticipantUserData[] testPeers;
  private File tempDir;
  String tempDirPath;
  private byte[] testBytes = ByteArray.makeRandomBytes(20);

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
    setupPeers();
  }
  
  private void setupPeers() throws Exception {
    testPeers = new ParticipantUserData[10];
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
    PollSpec ps =
      new MockPollSpec(new MockCachedUrlSet(new MockCachedUrlSetSpec()),
		       null, null, Poll.V3_POLL);
    return new V3Poller(ps, daemon, null, key, 20000, "SHA-1");
  }

  VoteBlock makeVoteBlock() {
    VoteBlock vb = new VoteBlock("foo", VoteBlock.CONTENT_VOTE);
    vb.addVersion(0, 123, 0, 155, testBytes, testBytes, false);
    return vb;
  }

  V3Poller.HashIndexer makeHashIndexer(final int pollSize,
				       final int symmetricPollSize) {
    return new V3Poller.HashIndexer() {
      public byte[] getParticipantHash(HashBlock.Version version,
				       int participantIndex) {
	if (0 <= participantIndex && participantIndex < pollSize) {
	  return version.getHashes()[participantIndex];
	} else {
	  fail("participantIndex "+participantIndex+ " out of bounds");
	  return null;
	}
      }
      public byte[] getSymmetricHash(HashBlock.Version version,
				     int symmetricParticipantIndex) {
	if (0 <= symmetricParticipantIndex &&
	    symmetricParticipantIndex < symmetricPollSize) {
	  return version.getHashes()[pollSize + symmetricParticipantIndex];
	} else {
	  fail("symmetricParticipantIndex "+
	       symmetricParticipantIndex+" out of bounds: ");
	  return null;
	}	
      }
      public byte[] getPlainHash(HashBlock.Version version) {
	return version.getHashes()[pollSize + symmetricPollSize];
      }
    };
  }

  public void testConstructPollTally() {
    BlockTally tally = new BlockTally(5, 75);
    assertEquals(BlockTally.Result.NOQUORUM, tally.getTallyResult());
  }

  public void testVoteWithBlockTallyPollerHas() throws Exception {
    VoteBlockTallier voteBlockTallier;
    BlockTally tally;
    VoteBlockTallier.HashBlockComparer comparer =
      new VoteBlockTallier.HashBlockComparer() {
	public boolean compare(VoteBlock voteBlock, int participantIndex) {
	  return participantIndex == 0;
	}
      };

    voteBlockTallier = VoteBlockTallier.make(comparer);
    tally = new BlockTally(5, 75);
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteSpoiled(testPeers[0]);
    assertEquals("0/0/0/0", tally.votes());

    voteBlockTallier = VoteBlockTallier.make(comparer);
    tally = new BlockTally(5, 75);
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteMissing(testPeers[0]);
    assertEquals("0/1/1/0", tally.votes());

    voteBlockTallier = VoteBlockTallier.make(comparer);
    tally = new BlockTally(5, 75);
    voteBlockTallier.addTally(tally);
    voteBlockTallier.vote(makeVoteBlock(), testPeers[0], 0);
    assertEquals("1/0/0/0", tally.votes());
  }

  public void testVoteWithBlockTallyPollerDoesntHave() throws Exception {
    VoteBlockTallier voteBlockTallier;
    BlockTally tally;

    voteBlockTallier = VoteBlockTallier.make();
    tally = new BlockTally(5, 75);
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteSpoiled(testPeers[0]);
    assertEquals("0/0/0/0", tally.votes());

    voteBlockTallier = VoteBlockTallier.make();
    tally = new BlockTally(5, 75);
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteMissing(testPeers[0]);
    assertEquals("1/0/0/0", tally.votes());

    voteBlockTallier = VoteBlockTallier.make();
    tally = new BlockTally(5, 75);
    voteBlockTallier.addTally(tally);
    voteBlockTallier.vote(makeVoteBlock(), testPeers[0], 0);
    assertEquals("0/1/0/1", tally.votes());
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
    VoteBlockTallier.HashBlockComparer comparer =
      new VoteBlockTallier.HashBlockComparer() {
	public boolean compare(VoteBlock voteBlock, int participantIndex) {
	  return participantIndex == 0;
	}
      };

    voter = new ParticipantUserData();
    onceVoteCallback = new OnceVoteCallback(voteBlock, voter);
    voteBlockTallier = VoteBlockTallier.make(comparer, onceVoteCallback);
    tally = ParticipantUserData.voteTally;
    voteBlockTallier.addTally(tally);
    voteBlockTallier.vote(makeVoteBlock(), voter, 0);
    assertEquals("1/0/0/0/0/0", voter.getVoteCounts().votes());
    assertTrue(onceVoteCallback.called);

    voter = new ParticipantUserData();
    onceVoteCallback = new OnceVoteCallback(voteBlock, voter);
    voteBlockTallier = VoteBlockTallier.make(comparer, onceVoteCallback);
    tally = ParticipantUserData.voteTally;
    voteBlockTallier.addTally(tally);
    voteBlockTallier.vote(makeVoteBlock(), voter, 1);
    assertEquals("0/1/0/0/0/0", voter.getVoteCounts().votes());
    assertTrue(onceVoteCallback.called);

    voter = new ParticipantUserData();
    voteBlockTallier = VoteBlockTallier.make(comparer,
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

    voteBlockTallier = VoteBlockTallier.make(comparer, new FailVoteCallback());
    voter = new ParticipantUserData();
    tally = ParticipantUserData.voteTally;
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteSpoiled(voter);
    assertEquals("0/0/0/0/0/1", voter.getVoteCounts().votes());
  }
}
