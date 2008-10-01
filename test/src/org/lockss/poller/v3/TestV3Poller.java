/*
 * $Id: TestV3Poller.java,v 1.27.8.3 2008-10-01 23:34:45 tlipkis Exp $
 */

/*

 Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;
import org.lockss.util.*;
import org.lockss.poller.*;
import org.lockss.poller.v3.FuncV3Poller.MyV3Poller;
import org.lockss.poller.v3.V3Serializer.*;
import org.lockss.test.*;
import org.lockss.hasher.*;
import org.lockss.repository.LockssRepositoryImpl;
import org.mortbay.util.B64Code;

import static org.lockss.util.Constants.*;


public class TestV3Poller extends LockssTestCase {

  private IdentityManager idmgr;
  private MockLockssDaemon theDaemon;

  private PeerIdentity pollerId;

  private String tempDirPath;
  private ArchivalUnit testau;
  private PollManager pollmanager;
  private HashService hashService;

  private PeerIdentity[] voters;
  private V3LcapMessage[] pollAcks;
  private V3LcapMessage[] nominates;
  private V3LcapMessage[] votes;
  private V3LcapMessage[] repairs;

  private byte[][] pollerNonces;
  private byte[][] voterNonces;
  
  private String localPeerKey = "TCP:[127.0.0.1]:9729";
  
  private File tempDir;

  private static final String BASE_URL = "http://www.test.org/";
  
  private List initialPeers =
    ListUtil.list("TCP:[10.1.0.1]:9729", "TCP:[10.1.0.2]:9729",
                  "TCP:[10.1.0.3]:9729", "TCP:[10.1.0.4]:9729",
                  "TCP:[10.1.0.5]:9729", "TCP:[10.1.0.6]:9729");

  private static String[] urls = {
    "lockssau:",
    BASE_URL,
    BASE_URL + "index.html",
    BASE_URL + "file1.html",
    BASE_URL + "file2.html",
    BASE_URL + "branch1/",
    BASE_URL + "branch1/index.html",
    BASE_URL + "branch1/file1.html",
    BASE_URL + "branch1/file2.html",
    BASE_URL + "branch2/",
    BASE_URL + "branch2/index.html",
    BASE_URL + "branch2/file1.html",
    BASE_URL + "branch2/file2.html",
  };

  private static List voteBlocks;
  static {
    voteBlocks = new ArrayList();
    for (int ix = 0; ix < urls.length; ix++) {
      VoteBlock vb = V3TestUtils.makeVoteBlock(urls[ix]); 
      voteBlocks.add(vb);
    }
  }

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = getMockLockssDaemon();
    TimeBase.setSimulated();
    this.tempDir = getTempDir();
    this.testau = setupAu();
    initRequiredServices();
    setupRepo(testau);
    this.pollerId = idmgr.stringToPeerIdentity(localPeerKey);
    this.voters = makeVoters(initialPeers);
    this.pollerNonces = makeNonces();
    this.voterNonces = makeNonces();
    this.pollAcks = makePollAckMessages();
    this.nominates = makeNominateMessages();
    this.votes = makeVoteMessages();
    this.repairs = makeRepairMessages();
  }

  private MockArchivalUnit setupAu() {
    MockArchivalUnit mau = new MockArchivalUnit();
    mau.setAuId("mock");
    MockPlugin plug = new MockPlugin(theDaemon);
    mau.setPlugin(plug);
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.setEstimatedHashDuration(1000);
    List files = new ArrayList();
    for (int ix = 0; ix < urls.length; ix++) {
      MockCachedUrl cu = (MockCachedUrl)mau.addUrl(urls[ix], false, true);
      // Add mock file content.
      cu.setContent("This is content for CUS file " + ix);
      files.add(cu);
    }
    cus.setHashItSource(files);
    cus.setFlatItSource(files);
    return mau;
  }
  
  private void setupRepo(ArchivalUnit au) throws Exception {
    MockLockssRepository repo = new MockLockssRepository("/foo", au);
    for (int ix =  0; ix < urls.length; ix++) {
      repo.createNewNode(urls[ix]);
    }
    ((MockLockssDaemon)theDaemon).setLockssRepository(repo, au);
  }

  private PeerIdentity[] makeVoters(List keys) throws Exception {
    PeerIdentity[] ids = new PeerIdentity[keys.size()];
    int idIndex = 0;
    for (Iterator it = keys.iterator(); it.hasNext(); ) {
      ids[idIndex++] = idmgr.findPeerIdentity((String)it.next());
    }
    return ids;
  }

  private byte[][] makeNonces() {
    byte[][] nonces = new byte[voters.length][];
    for (int ix = 0; ix < voters.length; ix++) {
      nonces[ix] = ByteArray.makeRandomBytes(20);
    }
    return nonces;
  }

  private V3LcapMessage[] makePollAckMessages() {
    V3LcapMessage[] msgs = new V3LcapMessage[voters.length];
    for (int i = 0; i < voters.length; i++) {
      msgs[i] = 
        new V3LcapMessage("auid", "key", "1",
                          ByteArray.makeRandomBytes(20),
                          ByteArray.makeRandomBytes(20),
                          V3LcapMessage.MSG_POLL_ACK,
                          987654321,
                          voters[i],
                          tempDir, theDaemon);
    }
    return msgs;
  }

  private V3LcapMessage[] makeNominateMessages() {
    V3LcapMessage[] msgs = new V3LcapMessage[voters.length];
    for (int i = 0; i < voters.length; i++) {
      V3LcapMessage msg = new V3LcapMessage("auid", "key", "1",
                                            ByteArray.makeRandomBytes(20),
                                            ByteArray.makeRandomBytes(20),
                                            V3LcapMessage.MSG_NOMINATE,
                                            987654321,
                                            voters[i],
                                            tempDir, theDaemon);
      msg.setNominees(ListUtil.list("TCP:[10.0." + i + ".1]:9729",
                                    "TCP:[10.0." + i + ".2]:9729",
                                    "TCP:[10.0." + i + ".3]:9729",
                                    "TCP:[10.0." + i + ".4]:9729"));
      msgs[i] = msg;
    }
    return msgs;
  }

  private V3LcapMessage[] makeVoteMessages() throws IOException {
    V3LcapMessage[] msgs = new V3LcapMessage[voters.length];
    for (int i = 0; i < voters.length; i++) {
      V3LcapMessage msg = new V3LcapMessage("auid", "key", "1",
                                            ByteArray.makeRandomBytes(20),
                                            ByteArray.makeRandomBytes(20),
                                            V3LcapMessage.MSG_VOTE,
                                            987654321,
                                            voters[i],
                                            tempDir, theDaemon);
      for (Iterator it = voteBlocks.iterator(); it.hasNext(); ) {
        msg.addVoteBlock((VoteBlock)it.next());
      }
      msgs[i] = msg;
    }
    return msgs;
  }

  private V3LcapMessage[] makeRepairMessages() {
    V3LcapMessage[] msgs = new V3LcapMessage[voters.length];
    for (int i = 0; i < voters.length; i++) {
      V3LcapMessage msg = new V3LcapMessage("auid", "key", "1",
                                            ByteArray.makeRandomBytes(20),
                                            ByteArray.makeRandomBytes(20),
                                            V3LcapMessage.MSG_REPAIR_REP,
                                            987654321,
                                            voters[i],
                                            tempDir, theDaemon);
      msgs[i] = msg;
    }
    return msgs;
  }

  public void tearDown() throws Exception {
    theDaemon.getLockssRepository(testau).stopService();
    theDaemon.getHashService().stopService();
    theDaemon.getDatagramRouterManager().stopService();
    theDaemon.getRouterManager().stopService();
    theDaemon.getSystemMetrics().stopService();
    theDaemon.getPollManager().stopService();
    TimeBase.setReal();
    super.tearDown();
  }

  /* Test for a specific bug fix. */
  public void testNullNomineesShouldntThrow() throws Exception {
    V3Poller v3Poller = makeInittedV3Poller("foo");
    try {
      v3Poller.nominatePeers(voters[2], null);
    } catch (NullPointerException ex) {
      fail("Should not have caused NullPointerException", ex);
    }
  }

  public void testInitHasherByteArrays() throws Exception {
    V3Poller v3Poller = makeInittedV3Poller("foo");
    Map innerCircle =
      (Map)PrivilegedAccessor.getValue(v3Poller, "theParticipants");
    assertEquals(innerCircle.size(), voters.length);
    byte[][] initBytes =
      (byte[][])PrivilegedAccessor.invokeMethod(v3Poller, "initHasherByteArrays");
    assertEquals(initBytes.length, innerCircle.size() + 1); // one for plain hash
    byte[][] compareBytes = new byte[innerCircle.size() + 1][];
    compareBytes[0] = new byte[0]; // Plain hash
    int ix = 1;
    for (Iterator it = innerCircle.values().iterator(); it.hasNext();) {
      ParticipantUserData proxy = (ParticipantUserData)it.next();
      compareBytes[ix++] =
        ByteArray.concat(proxy.getPollerNonce(), proxy.getVoterNonce());
    }
    for (int i = 0; i < initBytes.length; i++) {
      assertTrue(Arrays.equals(initBytes[i], compareBytes[i]));
    }
  }

  public void testInitHasherDigests() throws Exception {
    V3Poller v3Poller = makeInittedV3Poller("foo");
    Map innerCircle =
      (Map)PrivilegedAccessor.getValue(v3Poller, "theParticipants");
    assertEquals(innerCircle.size(), voters.length);
    MessageDigest[] digests =
      (MessageDigest[])PrivilegedAccessor.invokeMethod(v3Poller, "initHasherDigests");
    assertEquals(digests.length, innerCircle.size() + 1); // one for plain hash
    for (int i = 0; i < digests.length; i++) {
      assertNotNull("Digest " + i + " unexpectedly null.", digests[i]);
      assertEquals("SHA-1", digests[i].getAlgorithm());
    }
  }
  
  public void testChoosePeers() throws Exception {
    V3Poller p = makeV3Poller("testkey");
    
    PeerIdentity[] allPeers =
    {
     idmgr.findPeerIdentity("TCP:[127.0.0.1]:5000"),
     idmgr.findPeerIdentity("TCP:[127.0.0.1]:5001"),
     idmgr.findPeerIdentity("TCP:[127.0.0.1]:5002"),
     idmgr.findPeerIdentity("TCP:[127.0.0.1]:5003"),
     idmgr.findPeerIdentity("TCP:[127.0.0.1]:5004"),
     idmgr.findPeerIdentity("TCP:[127.0.0.1]:5005"),
     idmgr.findPeerIdentity("TCP:[127.0.0.1]:5006"),
     idmgr.findPeerIdentity("TCP:[127.0.0.1]:5007"),
     idmgr.findPeerIdentity("TCP:[127.0.0.1]:5008"),
     idmgr.findPeerIdentity("TCP:[127.0.0.1]:5009"),
    };
    
    PeerIdentity[] alreadySelected =
    {    
     allPeers[0],
     allPeers[1],
     allPeers[2],
     allPeers[3]
    };
    
    Collection unselectedPeers =
      p.choosePeers(ListUtil.fromArray(allPeers),
                    ListUtil.fromArray(alreadySelected),
                    allPeers.length);
    
    assertEquals(6, unselectedPeers.size());
    
    assertFalse("List should not contain peer " + allPeers[0],
                unselectedPeers.contains(allPeers[0]));
    assertFalse("List should not contain peer " + allPeers[1],
                unselectedPeers.contains(allPeers[1]));
    assertFalse("List should not contain peer " + allPeers[2],
                unselectedPeers.contains(allPeers[2]));
    assertFalse("List should not contain peer " + allPeers[3],
                unselectedPeers.contains(allPeers[3]));
      
  }

  public Collection findMorePeersToInvite(int quorum,
					  int extraParticipants,
					  int extraInvitations)
      throws Exception {
    Properties p = new Properties();
    p.setProperty(V3Poller.PARAM_QUORUM, ""+quorum);
    p.setProperty(V3Poller.PARAM_INVITATION_SIZE, "1");
    p.setProperty(V3Poller.PARAM_EXCEED_QUORUM_BY_INVITATIONS,
		  ""+extraInvitations);
    p.setProperty(V3Poller.PARAM_EXCEED_QUORUM_BY_PARTICIPANTS,
		  ""+extraParticipants);
    ConfigurationUtil.addFromProps(p);

    MyV3Poller poller = makeV3Poller("testkey");
    
//     List<PeerIdentity> allPeers =
//       ListUtil.fromArray(new PeerIdentity[] {
// 	idmgr.findPeerIdentity("TCP:[127.0.0.1]:5000"),
// 	idmgr.findPeerIdentity("TCP:[127.0.0.1]:5001"),
// 	idmgr.findPeerIdentity("TCP:[127.0.0.1]:5002"),
// 	idmgr.findPeerIdentity("TCP:[127.0.0.1]:5003"),
// 	idmgr.findPeerIdentity("TCP:[127.0.0.1]:5004"),
// 	idmgr.findPeerIdentity("TCP:[127.0.0.1]:5005"),
// 	idmgr.findPeerIdentity("TCP:[127.0.0.1]:5006"),
// 	idmgr.findPeerIdentity("TCP:[127.0.0.1]:5007"),
// 	idmgr.findPeerIdentity("TCP:[127.0.0.1]:5008"),
// 	idmgr.findPeerIdentity("TCP:[127.0.0.1]:5009"),
//       });
    
    List<String> somePeers =
      ListUtil.list(initialPeers.get(0),
		    initialPeers.get(1),
		    initialPeers.get(2));

    List<PeerIdentity> allPeers = pidsFromPeerNames(initialPeers);
    List<PeerIdentity> participatingPeers = pidsFromPeerNames(somePeers);

    for (PeerIdentity pid : participatingPeers) {
      ParticipantUserData participant = poller.addInnerCircleVoter(pid);
      // make it look like it's participating
      participant.setStatus(V3Poller.PEER_STATUS_ACCEPTED_POLL);
    }

    Collection more = poller.findMorePeersToInvite();
    assertTrue(more + " isn't disjoint with " + participatingPeers,
	       CollectionUtil.isDisjoint(more, participatingPeers));
    assertTrue(allPeers + " doesn't contain all of " + more,
	       allPeers.containsAll(more));
    return more;
  }

  public void testFindMore1() throws Exception {
    assertEquals(0, findMorePeersToInvite(2, 1, 2).size());
  }
  public void testFindMore2() throws Exception {
    assertEquals(1, findMorePeersToInvite(3, 1, 1).size());
  }
  public void testFindMore3() throws Exception {
    assertEquals(2, findMorePeersToInvite(3, 1, 2).size());
  }
  public void testFindMore4() throws Exception {
    assertEquals(2, findMorePeersToInvite(4, 2, 1).size());
  }
  public void testFindMore5() throws Exception {
    assertEquals(3, findMorePeersToInvite(4, 2, 2).size());
  }
  public void testFindMore6() throws Exception {
    assertEquals(3, findMorePeersToInvite(5, 2, 3).size());
  }

  List<PeerIdentity> pidsFromPeerNames(Collection<String> names) {
    List<PeerIdentity> res = new ArrayList();
    for (String name : names) {
      res.add(idmgr.findPeerIdentity(name));
    }
    return res;
  }

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
    MessageDigest[] digests = new MessageDigest[5];  // 1 plain hash, plus 4 voters
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
                     digests, TestV3Poller.hbVersionNum++, null);    
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
  
  private void addVersion(VoteBlock block, String content) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA1");
    md.update(content.getBytes());
    byte[] hash = md.digest();
    block.addVersion(0, content.length(), 
                     0, content.length(),
                     hash, hash, false);
  }
  
  private ParticipantUserData makeParticipant(PeerIdentity id,
                                              V3Poller poller,
                                              VoteBlock [] votes) 
      throws Exception {
    byte[] pollerNonce = ByteArray.makeRandomBytes(20);
    ParticipantUserData ud = new ParticipantUserData(id, poller, tempDir);
    ud.setPollerNonce(pollerNonce);
    VoteBlocks vb = new DiskVoteBlocks(tempDir);
    for (int i = 0; i < votes.length; i++) {
      vb.addVoteBlock(votes[i]);
    }
    ud.setVoteBlocks(vb);
    return ud;
  }
  
  public void testShouldIncludePeer() throws Exception {
    V3Poller v3Poller = makeV3Poller("key");
    assertFalse(v3Poller.shouldIncludePeer(pollerId));
    PeerIdentity p1 = idmgr.findPeerIdentity("TCP:[127.0.0.1]:5009");
    PeerIdentity p2 = idmgr.findPeerIdentity("TCP:[1.2.3.4]:5009");
    PeerIdentity p3 = idmgr.findPeerIdentity("TCP:[1.2.3.7]:1111");
    PeerIdentity p4 = idmgr.findPeerIdentity("TCP:[1.2.3.8]:1111");
    PeerIdentity p5 = idmgr.findPeerIdentity("TCP:[4.5.6.2]:1111");

    assertTrue(v3Poller.shouldIncludePeer(p1));
    assertTrue(v3Poller.shouldIncludePeer(p2));
    assertTrue(v3Poller.shouldIncludePeer(p3));
    assertTrue(v3Poller.shouldIncludePeer(p4));
    assertTrue(v3Poller.shouldIncludePeer(p5));
    ConfigurationUtil.addFromArgs(V3Poller.PARAM_NO_INVITATION_SUBNETS,
				  "1.2.3.4/30;4.5.6.2");

    assertTrue(v3Poller.shouldIncludePeer(p1));
    assertFalse(v3Poller.shouldIncludePeer(p2));
    assertFalse(v3Poller.shouldIncludePeer(p3));
    assertTrue(v3Poller.shouldIncludePeer(p4));
    assertFalse(v3Poller.shouldIncludePeer(p5));
  }


  public void testGetReferenceList() throws Exception {
    V3Poller v3Poller = makeV3Poller("key");
    assertNotNull(v3Poller.getReferenceList());
    assertEquals(6, v3Poller.getReferenceList().size());
    IdentityManager idMgr = theDaemon.getIdentityManager();
    idMgr.findPeerIdentity("TCP:[10.1.0.100]:9729");
    assertEquals(7, v3Poller.getReferenceList().size());
    idMgr.findPeerIdentity("TCP:[10.1.0.101]:9729");
    assertEquals(8, v3Poller.getReferenceList().size());
  }
  
  public void testGetReferenceListInitialPeersOnly() throws Exception {
    ConfigurationUtil.addFromArgs(V3Poller.PARAM_ENABLE_DISCOVERY, "false");
    V3Poller v3Poller = makeV3Poller("key");
    assertNotNull(v3Poller.getReferenceList());
    assertEquals(6, v3Poller.getReferenceList().size());
    IdentityManager idMgr = theDaemon.getIdentityManager();
    idMgr.findPeerIdentity("TCP:[10.1.0.100]:9729");
    assertEquals(6, v3Poller.getReferenceList().size());
    idMgr.findPeerIdentity("TCP:[10.1.0.101]:9729");
    assertEquals(6, v3Poller.getReferenceList().size());
  }
  
  public void testGetReferenceListDoesNotIncludeLocalIdentity() throws Exception {
    ConfigurationUtil.addFromArgs(V3Poller.PARAM_ENABLE_DISCOVERY, "false");
    // append our local config to the initial Peer List
    List initialPeersCopy = new ArrayList(initialPeers);
    initialPeersCopy.add(localPeerKey);
    
    ConfigurationUtil.addFromArgs(IdentityManagerImpl.PARAM_INITIAL_PEERS,
                                  StringUtil.separatedString(initialPeersCopy, ";"));
    
    V3Poller v3Poller = makeV3Poller("key");
    assertNotNull(v3Poller.getReferenceList());
    IdentityManager idMgr = theDaemon.getIdentityManager();
    // Sanity check
    assertTrue(idMgr.findPeerIdentity(localPeerKey).isLocalIdentity());
    // Should NOT be included in reference list
    assertEquals(6, v3Poller.getReferenceList().size());
    assertFalse(v3Poller.getReferenceList().contains(idMgr.findPeerIdentity(localPeerKey)));
  }
  
  public void testTallyBlocksSucceedsOnExtraFileEdgeCase() throws Exception {
    IdentityManager idMgr = theDaemon.getIdentityManager();

    V3Poller v3Poller = makeV3Poller("key");
    
    PeerIdentity id1 = idMgr.findPeerIdentity("TCP:[127.0.0.1]:8990");
    PeerIdentity id2 = idMgr.findPeerIdentity("TCP:[127.0.0.1]:8991");
    PeerIdentity id3 = idMgr.findPeerIdentity("TCP:[127.0.0.1]:8992");
        
    String [] urls_poller =
    { 
     "http://test.com/foo1",
     "http://test.com/foo2",
     "http://test.com/foo3"
    };
    
    HashBlock [] hashblocks =
    {
     makeHashBlock("http://test.com/foo1", "content for foo1"),
     makeHashBlock("http://test.com/foo2", "content for foo2"),
     makeHashBlock("http://test.com/foo3", "content for foo3")
    };
    
    VoteBlock [] voter1_voteblocks =
    {
     makeVoteBlock("http://test.com/foo1", "content for foo1"),
     makeVoteBlock("http://test.com/foo2a", "content for foo2a"),
     makeVoteBlock("http://test.com/foo3", "content for foo3")
    };
    
    VoteBlock [] voter2_voteblocks =
    {
     makeVoteBlock("http://test.com/foo1", "content for foo1"),
     makeVoteBlock("http://test.com/foo2a", "content for foo2a"),
     makeVoteBlock("http://test.com/foo3", "content for foo3")
    };
    
    VoteBlock [] voter3_voteblocks =
    {
     makeVoteBlock("http://test.com/foo1", "content for foo1"),
     makeVoteBlock("http://test.com/foo3", "content for foo3")
    };
    
    v3Poller.theParticipants.put(id1, makeParticipant(id1, v3Poller,
                                                      voter1_voteblocks));
    v3Poller.theParticipants.put(id2, makeParticipant(id2, v3Poller,
                                                      voter2_voteblocks));
    v3Poller.theParticipants.put(id3, makeParticipant(id3, v3Poller,
                                                      voter3_voteblocks));
    
    // Finally, let's test.
    
    BlockTally tally;
    
    tally = new BlockTally(2); // Quorum = 3
    v3Poller.tallyBlock(hashblocks[0], tally);
    assertEquals(BlockTally.RESULT_WON, tally.result);
    
    tally = new BlockTally(2); // Quorum = 3
    v3Poller.tallyBlock(hashblocks[1], tally);
    assertEquals(BlockTally.RESULT_LOST_EXTRA_BLOCK, tally.result);
    
    tally = new BlockTally(2); // Quorum = 3
    v3Poller.tallyBlock(hashblocks[2], tally);
    assertEquals(BlockTally.RESULT_WON, tally.result);
  }
  
  public void testCheckBlockWin() throws Exception {
    IdentityManager idMgr = theDaemon.getIdentityManager();

    V3Poller v3Poller = makeV3Poller("key");
    
    PeerIdentity id1 = idMgr.findPeerIdentity("TCP:[127.0.0.1]:8990");
    PeerIdentity id2 = idMgr.findPeerIdentity("TCP:[127.0.0.1]:8991");
    PeerIdentity id3 = idMgr.findPeerIdentity("TCP:[127.0.0.1]:8992");
    PeerIdentity id4 = idMgr.findPeerIdentity("TCP:[127.0.0.1]:8993");
    
    
    String url = "http://www.test.com/example.txt";
    
    String n1v1 = "This is node 1, version 1.  It's the oldest.";
    String n1v2 = "This is node 2, version 2.  It's slightly older.";
    String n1v3 = "This is node 1, version 3.  This is the current version!";

    // Our hash block only has v1 and v3
    HashBlock hb1 = makeHashBlock(url);
    addVersion(hb1, n1v1);
    addVersion(hb1, n1v3);

    BlockTally blockTally = new BlockTally(3);  // quorum of 3

    // Should agree on n1v1.
    VoteBlock vb1 = makeVoteBlock(url);
    addVersion(vb1, n1v1);
    assertDoesNotContain(blockTally.getAgreeVoters(), id1);
    v3Poller.compareBlocks(id1, 1, vb1, hb1, blockTally);
    assertContains(blockTally.getAgreeVoters(), id1);

    // Should agree on n1v3.
    VoteBlock vb2 = makeVoteBlock(url);
    addVersion(vb2, n1v1);
    addVersion(vb2, n1v3);
    assertDoesNotContain(blockTally.getAgreeVoters(), id3);
    v3Poller.compareBlocks(id2, 2, vb2, hb1, blockTally);
    assertContains(blockTally.getAgreeVoters(), id2);
    
    // Should agree on n1v3.
    VoteBlock vb3 = makeVoteBlock(url);
    addVersion(vb3, n1v2);
    addVersion(vb3, n1v3);
    assertDoesNotContain(blockTally.getAgreeVoters(), id3);
    v3Poller.compareBlocks(id3, 3, vb3, hb1, blockTally);
    assertContains(blockTally.getAgreeVoters(), id3);
    
    // Should not agree on any version.
    VoteBlock vb4 = makeVoteBlock(url);
    addVersion(vb4, n1v2);
    assertDoesNotContain(blockTally.getAgreeVoters(), id4);
    v3Poller.compareBlocks(id4, 4, vb4, hb1, blockTally);
    assertDoesNotContain(blockTally.getAgreeVoters(), id4);
    
    blockTally.tallyVotes();
    
    assertEquals(blockTally.getTallyResult(), BlockTally.RESULT_WON);
  }
  
  public void testCheckBlockLose() throws Exception {
    IdentityManager idMgr = theDaemon.getIdentityManager();

    V3Poller v3Poller = makeV3Poller("key");
    
    PeerIdentity id1 = idMgr.findPeerIdentity("TCP:[127.0.0.1]:8990");
    PeerIdentity id2 = idMgr.findPeerIdentity("TCP:[127.0.0.1]:8991");
    PeerIdentity id3 = idMgr.findPeerIdentity("TCP:[127.0.0.1]:8992");
    PeerIdentity id4 = idMgr.findPeerIdentity("TCP:[127.0.0.1]:8993");
    
    
    String url = "http://www.test.com/example.txt";
    
    String n1v1 = "This is node 1, version 1.  It's the oldest.";
    String n1v2 = "This is node 2, version 2.  It's slightly older.";
    String n1v3 = "This is node 1, version 3.  This is the current version!";

    // Our hash block only has v1 and v3
    HashBlock hb1 = makeHashBlock(url);
    addVersion(hb1, n1v1);
    addVersion(hb1, n1v3);

    BlockTally blockTally = new BlockTally(3);  // quorum of 3

    // Should agree on n1v1.
    VoteBlock vb1 = makeVoteBlock(url);
    addVersion(vb1, n1v1);
    assertDoesNotContain(blockTally.getAgreeVoters(), id1);
    v3Poller.compareBlocks(id1, 1, vb1, hb1, blockTally);
    assertContains(blockTally.getAgreeVoters(), id1);

    // Should not agree on any version.
    VoteBlock vb2 = makeVoteBlock(url);
    addVersion(vb2, n1v2);
    assertDoesNotContain(blockTally.getAgreeVoters(), id3);
    v3Poller.compareBlocks(id2, 2, vb2, hb1, blockTally);
    assertDoesNotContain(blockTally.getAgreeVoters(), id2);
    
    // Should not agree on any version.
    VoteBlock vb3 = makeVoteBlock(url);
    addVersion(vb3, n1v2);
    assertDoesNotContain(blockTally.getAgreeVoters(), id3);
    v3Poller.compareBlocks(id3, 3, vb3, hb1, blockTally);
    assertDoesNotContain(blockTally.getAgreeVoters(), id3);
    
    // Should not agree on any version.
    VoteBlock vb4 = makeVoteBlock(url);
    addVersion(vb4, n1v2);
    assertDoesNotContain(blockTally.getAgreeVoters(), id4);
    v3Poller.compareBlocks(id4, 4, vb4, hb1, blockTally);
    assertDoesNotContain(blockTally.getAgreeVoters(), id4);
    
    blockTally.tallyVotes();
    
    assertEquals(BlockTally.RESULT_LOST, blockTally.getTallyResult());
  }
  
  double inviteProb(long lastInvite, long lastMsg)
      throws Exception {
    String id = "tcp:[1.2.3.4]:4321";
    V3Poller poller = makeV3Poller("key");
    IdentityManager idMgr = theDaemon.getIdentityManager();
    PeerIdentity pid = idMgr.findPeerIdentity(id);
    idMgr.findLcapIdentity(pid, id);
    PeerIdentityStatus status = idMgr.getPeerIdentityStatus(pid);
    status.setLastMessageTime(lastMsg);
    status.setLastPollInvitationTime(lastInvite);
    return poller.inviteProb(status);
  }

  public void testInviteProb() throws Exception {
    // default is [4d,100],[20d,10],[40d,1]
    double r1 = .01*90.0/16.0;
    double r2 = .01*9.0/20.0;

    assertEquals(1.0, inviteProb(-1, -1));
    assertEquals(1.0, inviteProb(-1, 0));
    assertEquals(1.0, inviteProb(0, -1));
    assertEquals(1.0, inviteProb(0, 0));
    assertEquals(1.0, inviteProb(1, 1));
    assertEquals(1.0, inviteProb(10, 1));
    assertEquals(1.0, inviteProb(1, 10));
    
    assertEquals(1.0, inviteProb(1*DAY, 0));
    assertEquals(1.0, inviteProb(4*DAY, 0));
    assertEquals(1.0, inviteProb(44*DAY, 40*DAY));
    assertEquals(.94375, inviteProb(5*DAY, 0), .00001);
    assertEquals(1.0-r1, inviteProb(5*DAY, 0), .00001);
    assertEquals(.94375, inviteProb(105*DAY, 100*DAY), .00001);
    assertEquals(.55, inviteProb(112*DAY, 100*DAY), .001);
    assertEquals(.10, inviteProb(120*DAY, 100*DAY), .001);
    assertEquals(.01, inviteProb(140*DAY, 100*DAY), .001);

    ConfigurationUtil.addFromArgs(V3Poller.PARAM_INVITATION_WEIGHT_AGE_CURVE,
				  "[1w,1.0],[20w,.1]");
    assertEquals(1.0, inviteProb(1*WEEK, 0));
    assertEquals(0.1, inviteProb(20*WEEK, 0), .001);
  }
  
  private MyV3Poller makeV3Poller(String key) throws Exception {
    PollSpec ps = new MockPollSpec(testau.getAuCachedUrlSet(), null, null,
                                   Poll.V3_POLL);
    return new MyV3Poller(ps, theDaemon, pollerId, key, 20000, "SHA-1");
  }
  
  private MyV3Poller makeInittedV3Poller(String key) throws Exception {
    PollSpec ps = new MockPollSpec(testau.getAuCachedUrlSet(), null, null,
                                   Poll.V3_POLL);
    MyV3Poller p = new MyV3Poller(ps, theDaemon, pollerId, key, 20000,
                                          "SHA-1");
    p.constructInnerCircle(voters.length);
    Map innerCircle = (Map)PrivilegedAccessor.getValue(p, "theParticipants");
    for (int ix = 0; ix < voters.length; ix++) {
      PeerIdentity pid = voters[ix];
      ParticipantUserData ud = (ParticipantUserData) innerCircle.get(pid);
      if (ud != null) {
	ud.setVoterNonce(voterNonces[ix]);
      }
    }
    return p;
  }

  private class MyV3Poller extends V3Poller {
    // For testing:  Hashmap of voter IDs to V3LcapMessages.
    private Map sentMsgs = Collections.synchronizedMap(new HashMap());
    private Map semaphores = new HashMap();

    MyV3Poller(PollSpec spec, LockssDaemon daemon, PeerIdentity id,
                   String pollkey, long duration, String hashAlg)
        throws PollSerializerException {
      super(spec, daemon, id, pollkey, duration, hashAlg);
    }
    
    public void sendMessageTo(V3LcapMessage msg, PeerIdentity to) {
      sentMsgs.put(to, msg);
      SimpleBinarySemaphore sem = (SimpleBinarySemaphore)semaphores.get(to);
      if (sem == null) {
        sem = new SimpleBinarySemaphore();
        semaphores.put(to, sem);
      }
      sem.give();
    }

    public V3LcapMessage getSentMessage(PeerIdentity voter) {
      SimpleBinarySemaphore sem = (SimpleBinarySemaphore)semaphores.get(voter);
      if (sem == null) {
        fail ("Message never sent!");
      }
      sem.take(5000); // Really shouldn't take this long
      return (V3LcapMessage)sentMsgs.get(voter);
    }
  }

  private void initRequiredServices() throws Exception {
    pollmanager = theDaemon.getPollManager();
    hashService = theDaemon.getHashService();

    theDaemon.getPluginManager();

    tempDir = getTempDir();
    tempDirPath = tempDir.getAbsolutePath();
    System.setProperty("java.io.tmpdir", tempDirPath);

    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    p.setProperty(IdentityManager.PARAM_LOCAL_V3_IDENTITY, localPeerKey);
    p.setProperty(ConfigManager.PARAM_NEW_SCHEDULER, "true");
    p.setProperty(IdentityManagerImpl.PARAM_INITIAL_PEERS,
                  StringUtil.separatedString(initialPeers, ";"));
    p.setProperty(V3Poller.PARAM_QUORUM, "3");
    p.setProperty(V3Poller.PARAM_INVITATION_SIZE, "6");
    p.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    p.setProperty(V3Serializer.PARAM_V3_STATE_LOCATION, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(p);
    idmgr = theDaemon.getIdentityManager();
    idmgr.startService();
    theDaemon.getSchedService().startService();
    hashService.startService();
    theDaemon.getDatagramRouterManager().startService();
    theDaemon.getRouterManager().startService();
    theDaemon.getSystemMetrics().startService();
    theDaemon.getActivityRegulator(testau).startService();
    theDaemon.setNodeManager(new MockNodeManager(), testau);
    pollmanager.startService();
  }
}
