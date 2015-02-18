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

import java.io.*;
import java.util.*;
import java.security.*;
import org.lockss.app.*;
import org.lockss.config.ConfigManager;
import org.lockss.hasher.HashResult;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.protocol.IdentityManager.IdentityAgreement;
import org.lockss.protocol.MockPeerIdentity;
import org.lockss.protocol.psm.*;
import org.lockss.util.*;
import org.lockss.poller.*;
import org.lockss.poller.v3.V3Serializer.*;
import org.lockss.test.*;
import org.lockss.hasher.*;
import org.lockss.repository.LockssRepositoryImpl;
import org.mortbay.util.B64Code;

import static org.lockss.util.Constants.*;


public class TestVersionCounts extends LockssTestCase {

  private final String localPeerKey = "TCP:[127.0.0.1]:9729";
  private MockLockssDaemon daemon;
  private PeerIdentity pollerId;

  PeerIdentity id1;
  PeerIdentity id2;
  PeerIdentity id3;
  
  V3Poller v3Poller;
  
  ParticipantUserData participant1;
  ParticipantUserData participant2;
  ParticipantUserData participant3;
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    daemon = getMockLockssDaemon();
    pollerId = findPeerIdentity(localPeerKey);

    id1 = new MockPeerIdentity("TCP:[127.0.0.1]:8990");
    id2 = new MockPeerIdentity("TCP:[127.0.0.1]:8991");
    id3 = new MockPeerIdentity("TCP:[127.0.0.1]:8992");

    v3Poller = makeV3Poller("testing poll key");

    participant1 = new ParticipantUserData(id1, v3Poller, null);
    participant2 = new ParticipantUserData(id2, v3Poller, null);
    participant3 = new ParticipantUserData(id3, v3Poller, null);
  }

  // The number of random bytes in a pretend nonce
  private static final int NONCE_LENGTH = 20;

  private HashBlock makeHashBlock(String url) {
    MockCachedUrl cu = new MockCachedUrl(url);
    return new HashBlock(cu);
  }
  
  private HashBlock makeHashBlock(String url, String content)
      throws Exception {
    MockCachedUrl cu = new MockCachedUrl(url);
    HashBlock hb = new HashBlock(cu);
    addVersion(hb, content);
    return hb;
  }

  private static int hbVersionNum = 1;

  private void addVersion(HashBlock block, String content) throws Exception {
    // 1 plain hash, plus 4 voters
    MessageDigest[] digests = new MessageDigest[5];  
    // fake "Plain Hash"
    digests[0] = MessageDigest.getInstance("SHA1");
    digests[0].update(content.getBytes());
    // fake "Nonced Hash" for voter 1
    digests[1] = MessageDigest.getInstance("SHA1");
    digests[1].update(content.getBytes());
    // fake "Nonced Hash" for voter 2
    digests[2] = MessageDigest.getInstance("SHA1");
    digests[2].update(content.getBytes());
    // fake "Nonced Hash" for voter 3
    digests[3] = MessageDigest.getInstance("SHA1");
    digests[3].update(content.getBytes());
    // fake "Nonced Hash" for voter 4
    digests[4] = MessageDigest.getInstance("SHA1");
    digests[4].update(content.getBytes());
    
    block.addVersion(0, content.length(), 
                     0, content.length(),
		     digests.length * content.length(), // total bytes hashed
                     digests, hbVersionNum++, null);    
  }
  
  private VoteBlock makeVoteBlock(String url) {
    VoteBlock vb = new VoteBlock(url);
    return vb;
  }
  
  private VoteBlock makeVoteBlock(String url, String content)
      throws Exception {
    VoteBlock vb = new VoteBlock(url);
    addVersion(vb, content);
    return vb;
  }
  
  private byte[] addVersion(VoteBlock block, String content) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA1");
    md.update(content.getBytes());
    byte[] hash = md.digest();
    block.addVersion(0, content.length(), 
                     0, content.length(),
                     hash, hash, false);
    return hash;
  }

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
  
  private class MyV3Poller extends V3Poller {
    MyV3Poller(PollSpec ps, PeerIdentity id,
	       String pollkey, long duration, String hashAlg)
        throws PollSerializerException {
      super(ps, daemon, id, pollkey, duration, hashAlg);
    }
  }

  PeerIdentity findPeerIdentity(String key) throws Exception {
    return V3TestUtils.findPeerIdentity(daemon, key);
  }

  public void testConstruct() throws Exception {
    VersionCounts versionCounts = VersionCounts.make();
    Map<ParticipantUserData, HashResult> repairCandidates;
    repairCandidates = versionCounts.getRepairCandidates(0);
    assertEmpty(repairCandidates);
  }

  public void testMinimumSupport() throws Exception {
    VoteBlock vb1 = makeVoteBlock("http://test.com/foo1");
    byte[] hash1 = addVersion(vb1, "content 1 for foo1");
    byte[] hash2 = addVersion(vb1, "content 2 for foo1");

    VoteBlock vb2 = makeVoteBlock("http://test.com/foo1");
    addVersion(vb2, "content 1 for foo1");
    addVersion(vb2, "content 2 for foo1");

    VoteBlock vb3 = makeVoteBlock("http://test.com/foo1");
    addVersion(vb3, "content 1 for foo1");
    addVersion(vb3, "content 2 for foo1");

    VersionCounts versionCounts = VersionCounts.make();

    versionCounts.vote(vb1, participant1);
    versionCounts.vote(vb2, participant2);
    versionCounts.vote(vb3, participant3);

    Map<ParticipantUserData, HashResult> repairCandidates;
    repairCandidates = versionCounts.getRepairCandidates(2);
    assertSameElements(SetUtil.set(participant1, participant2, participant3),
		       repairCandidates.keySet());
  }

  public void testMultipleIdenticalVersions() throws Exception {
    VersionCounts versionCounts = VersionCounts.make();

    VoteBlock vb1 = makeVoteBlock("http://test.com/foo1");
    byte[] hash1 = addVersion(vb1, "content 1 for foo1");
    byte[] hash2 = addVersion(vb1, "content 2 for foo1");

    VoteBlock vb2 = makeVoteBlock("http://test.com/foo1");
    addVersion(vb2, "content 1 for foo1");
    addVersion(vb2, "content 1 for foo1");
    addVersion(vb2, "content 1 for foo1");
    addVersion(vb2, "content 1 for foo1");
    addVersion(vb2, "content 2 for foo1");

    VoteBlock vb3 = makeVoteBlock("http://test.com/foo1");
    addVersion(vb3, "content 1 for foo1");
    addVersion(vb3, "content 2 for foo1");
    addVersion(vb3, "content 2 for foo1");
    addVersion(vb3, "content 2 for foo1");
    addVersion(vb3, "content 2 for foo1");

    versionCounts.vote(vb1, participant1);
    versionCounts.vote(vb2, participant2);
    versionCounts.vote(vb3, participant3);

    Map<ParticipantUserData, HashResult> repairCandidates;
    repairCandidates = versionCounts.getRepairCandidates(2);
    assertSameElements(SetUtil.set(participant1, participant2, participant3),
		       repairCandidates.keySet());

    // With only three candidates, no version should reach a threshold
    // of 4, unless counting multiples is wrong.
    repairCandidates = versionCounts.getRepairCandidates(4);
    assertEmpty(repairCandidates.keySet());
  }

  public void testHeadNotPopular() throws Exception {
    VersionCounts versionCounts = VersionCounts.make();

    VoteBlock vb1 = makeVoteBlock("http://test.com/foo1");
    byte[] hash1 = addVersion(vb1, "content 1 for foo1");
    byte[] hash2 = addVersion(vb1, "content 2 for foo1");

    VoteBlock vb2 = makeVoteBlock("http://test.com/foo1");
    addVersion(vb2, "content 1 for foo1");
    addVersion(vb2, "content 2 for foo1");

    VoteBlock vb3 = makeVoteBlock("http://test.com/foo1");
    addVersion(vb3, "content 3 for foo1");
    addVersion(vb3, "content 2 for foo1");

    versionCounts.vote(vb1, participant1);
    versionCounts.vote(vb2, participant2);
    versionCounts.vote(vb3, participant3);

    Map<ParticipantUserData, HashResult> repairCandidates;
    repairCandidates = versionCounts.getRepairCandidates(0);
    assertSameElements(SetUtil.set(participant1, participant2, participant3),
		       repairCandidates.keySet());

    repairCandidates = versionCounts.getRepairCandidates(1);
    assertSameElements(SetUtil.set(participant1, participant2, participant3),
		       repairCandidates.keySet());

    repairCandidates = versionCounts.getRepairCandidates(2);
    assertSameElements(SetUtil.set(participant1, participant2),
		       repairCandidates.keySet());

    repairCandidates = versionCounts.getRepairCandidates(3);
    assertEmpty(repairCandidates.keySet());
  }

  public void testHeadNotAllowed() throws Exception {
    VersionCounts versionCounts = VersionCounts.make();

    VoteBlock vb1 = makeVoteBlock("http://test.com/foo1");
    byte[] hash1 = addVersion(vb1, "content 1 for foo1");
    byte[] hash2 = addVersion(vb1, "content 2 for foo1");

    versionCounts.vote(vb1, participant1);

    Map<ParticipantUserData, HashResult> repairCandidates;
    repairCandidates = versionCounts.getRepairCandidates(0);
    assertSameElements(SetUtil.set(participant1), repairCandidates.keySet());

    // Same, but with an excluded version that doesn't matter.
    repairCandidates = versionCounts.getRepairCandidates(0,
					SetUtil.set(HashResult.make(hash2)));
    assertSameElements(SetUtil.set(participant1), repairCandidates.keySet());

    // Same, but with an excluded version that does matter
    repairCandidates = versionCounts.getRepairCandidates(0,
					SetUtil.set(HashResult.make(hash1)));
    assertEmpty(repairCandidates);
  }

  public void testSortedRepairCandidates() throws Exception {
    VersionCounts versionCounts = VersionCounts.make();

    VoteBlock vb1 = makeVoteBlock("http://test.com/foo1");
    addVersion(vb1, "content 1 for foo1");

    VoteBlock vb2 = makeVoteBlock("http://test.com/foo1");
    addVersion(vb2, "content 2 for foo1");

    VoteBlock vb3 = makeVoteBlock("http://test.com/foo1");
    addVersion(vb3, "content 3 for foo1");
    addVersion(vb3, "content 2 for foo1");

    versionCounts.vote(vb1, participant1);
    versionCounts.vote(vb2, participant2);
    versionCounts.vote(vb3, participant3);

    Map<Integer, Collection<ParticipantUserData>> repairCandidates;

    repairCandidates = versionCounts.getSortedRepairCandidatesMap(2);
    assertEquals(SetUtil.set(2), repairCandidates.keySet());
    assertSameElements(SetUtil.set(participant2),
		       repairCandidates.get(2));
    assertEquals(ListUtil.list(participant2),
		 versionCounts.getSortedRepairCandidates(2));

    repairCandidates = versionCounts.getSortedRepairCandidatesMap(1);
    assertIsomorphic(ListUtil.list(2, 1), repairCandidates.keySet());
    assertSameElements(SetUtil.set(participant2),
		       repairCandidates.get(2));
    assertSameElements(SetUtil.set(participant1, participant3),
		       repairCandidates.get(1));

    List<ParticipantUserData> lst = versionCounts.getSortedRepairCandidates(1);
    assertTrue(""+lst, (lst.equals(ListUtil.list(participant2,
						 participant1,
						 participant3)) ||
			lst.equals(ListUtil.list(participant2,
						 participant3,
						 participant1))));

    assertEmpty(versionCounts.getSortedRepairCandidatesMap(4));
    assertEmpty(versionCounts.getSortedRepairCandidates(4));

  }
}
