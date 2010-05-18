/*
 * $Id: TestV3Poller.java,v 1.33 2010-05-18 06:15:37 tlipkis Exp $
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
import org.lockss.protocol.IdentityManager.IdentityAgreement;
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

  private MyIdentityManager idMgr;
  private MockLockssDaemon theDaemon;

  private PeerIdentity pollerId;

  private String tempDirPath;
  private ArchivalUnit testau;
  private PollManager pollmanager;
  private HashService hashService;
  private PluginManager pluginMgr;

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
    this.pollerId = findPeerIdentity(localPeerKey);
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

  PeerIdentity findPeerIdentity(String key) throws Exception {
    PeerIdentity pid = idMgr.findPeerIdentity(key);
    // hack to ensure it's created
    idMgr.findLcapIdentity(pid, pid.getIdString());
    return pid;
  }

  private PeerIdentity[] makeVoters(List keys) throws Exception {
    PeerIdentity[] ids = new PeerIdentity[keys.size()];
    int idIndex = 0;
    for (Iterator it = keys.iterator(); it.hasNext(); ) {
      PeerIdentity pid = findPeerIdentity((String)it.next());
      PeerIdentityStatus status = idMgr.getPeerIdentityStatus(pid);
      ids[idIndex++] = pid;
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

  double invitationWeight(long lastInvite, long lastMsg)
      throws Exception {
    String id = "tcp:[1.2.3.4]:4321";
    V3Poller poller = makeV3Poller("key");
    PeerIdentity pid = findPeerIdentity(id);
    idMgr.findLcapIdentity(pid, id);
    PeerIdentityStatus status = idMgr.getPeerIdentityStatus(pid);
    status.setLastMessageTime(lastMsg);
    status.setLastPollInvitationTime(lastInvite);
    return poller.weightResponsiveness(status);
  }

  String w1 = "tcp:[1.2.3.4]:4321";
  String w2 = "tcp:[1.2.3.4]:4322";

  String atRiskEntry(ArchivalUnit au, String pidkey) throws Exception {
    return atRiskEntry(au, findPeerIdentity(pidkey));
  }

  String atRiskEntry(ArchivalUnit au, PeerIdentity pid) {
    return testau.getAuId() + "," + pid.getIdString();
  }

  private IdentityAgreement getIda(PeerIdentity pid) {
    return idMgr.findTestIdentityAgreement(pid, testau);
  }

  double invitationWeight(String pidkey, long lastInvite, long lastMsg)
      throws Exception {
    return invitationWeight(findPeerIdentity(pidkey), lastInvite, lastMsg);
  }

  double invitationWeight(String pidkey, long lastInvite,
			  long lastMsg, float highestAgreement)
      throws Exception {
    return invitationWeight(findPeerIdentity(pidkey), lastInvite, lastMsg,
			    highestAgreement);
  }

  double invitationWeight(PeerIdentity pid,
			  long lastInvite, long lastMsg)
      throws Exception {
    return invitationWeight(pid, lastInvite, lastMsg, 0.1f);
  }

  double invitationWeight(PeerIdentity pid, long lastInvite,
			  long lastMsg, float highestAgreement)
      throws Exception {
    ConfigurationUtil.addFromArgs(V3Poller.PARAM_AT_RISK_AU_INSTANCES,
				  atRiskEntry(testau, w2),
				  V3Poller.PARAM_INVITATION_WEIGHT_AT_RISK,
				  "3.0");

    V3Poller poller = makeV3Poller("key");
    PeerIdentityStatus status = idMgr.getPeerIdentityStatus(pid);
    status.setLastMessageTime(lastMsg);
    status.setLastPollInvitationTime(lastInvite);
    if (highestAgreement >= 0) {
      IdentityAgreement ida = getIda(pid);
      ida.setPercentAgreement(highestAgreement);
    }
    return poller.invitationWeight(status);
  }

  public void testInvitationWeight() throws Exception {
    // default age curve: [10d,1.0],[30d,0.1],[40d,0.01]
    assertEquals(1.0, invitationWeight(w1, -1, -1));
    // w2 is listed as having this AU at risk
    assertEquals(3.0, invitationWeight(w2, -1, -1));
    // With high agreement, invitationWeightAlreadyRepairable kicks in (.5)
    assertEquals(0.5, invitationWeight(w1, -1, -1, .9f));
    assertEquals(1.5, invitationWeight(w2, -1, -1, .9f));
  }

  public void testInvitationWeightAgeCurve() throws Exception {
    // default is [10d,1.0],[30d,0.1],[40d,0.01]
    double r1 = .01*90.0/16.0;
    double r2 = .01*9.0/20.0;

    assertEquals(1.0, invitationWeight(-1, -1));
    assertEquals(1.0, invitationWeight(-1, 0));
    assertEquals(1.0, invitationWeight(0, -1));
    assertEquals(1.0, invitationWeight(0, 0));
    assertEquals(1.0, invitationWeight(1, 1));
    assertEquals(1.0, invitationWeight(10, 1));
    assertEquals(1.0, invitationWeight(1, 10));
    
    assertEquals(1.0, invitationWeight(1*DAY, 0), .01);
    assertEquals(1.0, invitationWeight(4*DAY, 0), .01);
    assertEquals(1.0, invitationWeight(44*DAY, 40*DAY), .01);
    assertEquals(.94, invitationWeight(5*DAY, 0), .01);
    assertEquals(1.0-r1, invitationWeight(5*DAY, 0), .02);
    assertEquals(.94, invitationWeight(105*DAY, 100*DAY), .01);
    assertEquals(.55, invitationWeight(112*DAY, 100*DAY), .01);
    assertEquals(.10, invitationWeight(120*DAY, 100*DAY), .01);
    assertEquals(.01, invitationWeight(140*DAY, 100*DAY), .01);

    ConfigurationUtil.addFromArgs(V3Poller.PARAM_INVITATION_WEIGHT_AGE_CURVE,
				  "[1w,1.0],[20w,.1]");
    assertEquals(1.0, invitationWeight(1*WEEK, 0), .01);
    assertEquals(0.1, invitationWeight(20*WEEK, 0), .01);
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
  
  public void testIsPeerEligible() throws Exception {
    V3Poller v3Poller = makeV3Poller("key");
    assertFalse(v3Poller.isPeerEligible(pollerId));
    PeerIdentity p1 = findPeerIdentity("TCP:[127.0.0.1]:5009");
    PeerIdentity p2 = findPeerIdentity("TCP:[1.2.3.4]:5009");
    PeerIdentity p3 = findPeerIdentity("TCP:[1.2.3.7]:1111");
    PeerIdentity p4 = findPeerIdentity("TCP:[1.2.3.8]:1111");
    PeerIdentity p5 = findPeerIdentity("TCP:[4.5.6.2]:1111");

    assertTrue(v3Poller.isPeerEligible(p1));
    assertTrue(v3Poller.isPeerEligible(p2));
    assertTrue(v3Poller.isPeerEligible(p3));
    assertTrue(v3Poller.isPeerEligible(p4));
    assertTrue(v3Poller.isPeerEligible(p5));
    ConfigurationUtil.addFromArgs(V3Poller.PARAM_NO_INVITATION_SUBNETS,
				  "1.2.3.4/30;4.5.6.2");

    assertTrue(v3Poller.isPeerEligible(p1));
    assertFalse(v3Poller.isPeerEligible(p2));
    assertFalse(v3Poller.isPeerEligible(p3));
    assertTrue(v3Poller.isPeerEligible(p4));
    assertFalse(v3Poller.isPeerEligible(p5));
  }


  Collection getAvailablePeers(V3Poller v3Poller) {
    return v3Poller.getAvailablePeers().keySet();
  }

  public void testgetAvailablePeers() throws Exception {
    PeerIdentity p1 = findPeerIdentity("TCP:[10.1.0.100]:9729");
    PeerIdentity p2 = findPeerIdentity("TCP:[10.1.0.101]:9729");

    DatedPeerIdSet noAuSet = pollmanager.getNoAuPeerSet(testau);
    synchronized (noAuSet) {
      noAuSet.add(p2);
    }	
    assertTrue(noAuSet.contains(p2));

    V3Poller v3Poller = makeV3Poller("key");
    Collection avail = getAvailablePeers(v3Poller);
    log.info("avail: " + avail);
	     
    assertTrue(avail.contains(p1));
    assertFalse(avail.contains(p2));

    Set exp = new HashSet();
    exp.add(p1);
    for (PeerIdentity pid : voters) {
      exp.add(pid);
    }
    assertEquals(exp, avail);
  }
  
  public void testgetAvailablePeersInitialPeersOnly() throws Exception {
    ConfigurationUtil.addFromArgs(V3Poller.PARAM_ENABLE_DISCOVERY, "false");
    findPeerIdentity("TCP:[10.1.0.100]:9729");
    findPeerIdentity("TCP:[10.1.0.101]:9729");
    V3Poller v3Poller = makeV3Poller("key");
    assertNotNull(getAvailablePeers(v3Poller));
    assertEquals(6, getAvailablePeers(v3Poller).size());
  }
  
  public void testgetAvailablePeersDoesNotIncludeLocalIdentity() throws Exception {
    ConfigurationUtil.addFromArgs(V3Poller.PARAM_ENABLE_DISCOVERY, "false");
    // append our local config to the initial Peer List
    List initialPeersCopy = new ArrayList(initialPeers);
    initialPeersCopy.add(localPeerKey);
    
    ConfigurationUtil.addFromArgs(IdentityManagerImpl.PARAM_INITIAL_PEERS,
                                  StringUtil.separatedString(initialPeersCopy, ";"));
    
    V3Poller v3Poller = makeV3Poller("key");
    assertNotNull(getAvailablePeers(v3Poller));
    // Sanity check
    assertTrue(findPeerIdentity(localPeerKey).isLocalIdentity());
    // Should NOT be included in reference list
    assertEquals(6, getAvailablePeers(v3Poller).size());
    assertFalse(getAvailablePeers(v3Poller).contains(findPeerIdentity(localPeerKey)));
  }

  public List<PeerIdentity> makeAdditionalPeers() throws Exception {
    PeerIdentity[] morePeers = {
     findPeerIdentity("TCP:[127.0.0.1]:5000"),
     findPeerIdentity("TCP:[127.0.0.1]:5001"),
     findPeerIdentity("TCP:[127.0.0.1]:5002"),
     findPeerIdentity("TCP:[127.0.0.1]:5003"),
     findPeerIdentity("TCP:[127.0.0.1]:5004"),
     findPeerIdentity("TCP:[127.0.0.1]:5005"),
     findPeerIdentity("TCP:[127.0.0.1]:5006"),
     findPeerIdentity("TCP:[127.0.0.1]:5007"),
     findPeerIdentity("TCP:[127.0.0.1]:5008"),
     findPeerIdentity("TCP:[127.0.0.1]:5009"),
    };
    return ListUtil.fromArray(morePeers);
  }

  public void testCountParticipatingPeers() throws Exception {
    MyV3Poller poller = makeV3Poller("testkey");
    List<String> somePeers =
      ListUtil.list(initialPeers.get(0),
		    initialPeers.get(1),
		    initialPeers.get(2));

    List<PeerIdentity> participatingPeers = pidsFromPeerNames(somePeers);

    for (PeerIdentity pid : participatingPeers) {
      ParticipantUserData participant = poller.addInnerCircleVoter(pid);
      // make it look like it's participating
      participant.setStatus(V3Poller.PEER_STATUS_ACCEPTED_POLL);
    }
    assertEquals(3, poller.countParticipatingPeers());
  }

  public Collection findMorePeersToInvite(int quorum,
					  double invitationMult)
      throws Exception {
    Properties p = new Properties();
    p.setProperty(V3Poller.PARAM_QUORUM, ""+quorum);
    p.setProperty(V3Poller.PARAM_INVITATION_SIZE_TARGET_MULTIPLIER,
		  ""+invitationMult);
    ConfigurationUtil.addFromProps(p);

    MyV3Poller poller = makeV3Poller("testkey");
    
    List<String> somePeers =
      ListUtil.list(initialPeers.get(0),
		    initialPeers.get(1),
		    initialPeers.get(2));

    List<PeerIdentity> allPeers = pidsFromPeerNames(initialPeers);
    allPeers.addAll(makeAdditionalPeers());
    List<PeerIdentity> participatingPeers = pidsFromPeerNames(somePeers);

    for (PeerIdentity pid : participatingPeers) {
      ParticipantUserData participant = poller.addInnerCircleVoter(pid);
      // make it look like it's participating
      participant.setStatus(V3Poller.PEER_STATUS_ACCEPTED_POLL);
    }

    Collection more = poller.findNPeersToInvite(quorum);
    assertTrue(more + " isn't disjoint with " + participatingPeers,
	       CollectionUtil.isDisjoint(more, participatingPeers));
    assertTrue(allPeers + " doesn't contain all of " + more,
	       allPeers.containsAll(more));
    return more;
  }

  public void testFindMore1() throws Exception {
    assertEquals(2, findMorePeersToInvite(2, 1).size());
  }
  public void testFindMore2() throws Exception {
    assertEquals(4, findMorePeersToInvite(2, 2).size());
  }
  public void testFindMore3() throws Exception {
    assertEquals(6, findMorePeersToInvite(3, 2).size());
  }
  public void testFindMore4() throws Exception {
    assertEquals(10, findMorePeersToInvite(10, 1).size());
  }
  public void testFindMore5() throws Exception {
    assertEquals(13, findMorePeersToInvite(10, 2).size());
  }

  List<PeerIdentity> pidsFromPeerNames(Collection<String> names)
      throws Exception {
    List<PeerIdentity> res = new ArrayList();
    for (String name : names) {
      res.add(findPeerIdentity(name));
    }
    return res;
  }

  public void testTallyBlocksSucceedsOnExtraFileEdgeCase() throws Exception {

    V3Poller v3Poller = makeV3Poller("key");
    
    PeerIdentity id1 = findPeerIdentity("TCP:[127.0.0.1]:8990");
    PeerIdentity id2 = findPeerIdentity("TCP:[127.0.0.1]:8991");
    PeerIdentity id3 = findPeerIdentity("TCP:[127.0.0.1]:8992");
        
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

    V3Poller v3Poller = makeV3Poller("key");
    
    PeerIdentity id1 = findPeerIdentity("TCP:[127.0.0.1]:8990");
    PeerIdentity id2 = findPeerIdentity("TCP:[127.0.0.1]:8991");
    PeerIdentity id3 = findPeerIdentity("TCP:[127.0.0.1]:8992");
    PeerIdentity id4 = findPeerIdentity("TCP:[127.0.0.1]:8993");
    
    
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
    V3Poller v3Poller = makeV3Poller("key");
    
    PeerIdentity id1 = findPeerIdentity("TCP:[127.0.0.1]:8990");
    PeerIdentity id2 = findPeerIdentity("TCP:[127.0.0.1]:8991");
    PeerIdentity id3 = findPeerIdentity("TCP:[127.0.0.1]:8992");
    PeerIdentity id4 = findPeerIdentity("TCP:[127.0.0.1]:8993");
    
    
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
  
  public void testSignalAuEvent() throws Exception {
    MyV3Poller poller = makeV3Poller("testkey");
    pluginMgr.registerAuEventHandler(new MyAuEventHandler());
    List<String> urls = ListUtil.list("url1", "foo2");
    List<PollerStateBean.Repair> rep = new ArrayList<PollerStateBean.Repair>();
    for (String u : urls) {
      rep.add(new PollerStateBean.Repair(u));
    }
    poller.setCompletedRepairs(rep);
    assertEquals(0, changeEvents.size());
    poller.signalAuEvent();
    assertEquals(1, changeEvents.size());
    AuEventHandler.ChangeInfo ci = changeEvents.get(0);
    assertEquals(AuEventHandler.ChangeInfo.Type.Repair, ci.getType());
    assertTrue(ci.isComplete());
    assertEquals(2, ci.getNumUrls());
    assertNull(ci.getMimeCounts());
    assertEquals(urls, ci.getUrls());
  }

  List<AuEventHandler.ChangeInfo> changeEvents = new ArrayList();

  class MyAuEventHandler extends AuEventHandler.Base {
    public void auContentChanged(ArchivalUnit au,
				 AuEventHandler.ChangeInfo info) {
      changeEvents.add(info);
    }
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
    private List<PollerStateBean.Repair> repairs;

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

    void setCompletedRepairs(List<PollerStateBean.Repair> repairs) {
      this.repairs = repairs;
    }

    @Override
    public List getCompletedRepairs() {
      if (repairs != null) {
	return repairs;
      }
      return super.getCompletedRepairs();
    }
  }

  private void initRequiredServices() throws Exception {
    pollmanager = theDaemon.getPollManager();
    hashService = theDaemon.getHashService();

    pluginMgr = theDaemon.getPluginManager();

    tempDir = getTempDir();
    tempDirPath = tempDir.getAbsolutePath();
    System.setProperty("java.io.tmpdir", tempDirPath);

    Properties p = new Properties();
    p.setProperty(IdentityManagerImpl.PARAM_ENABLE_V1, "false");
    p.setProperty(LcapDatagramComm.PARAM_ENABLED, "false");

    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    p.setProperty(IdentityManager.PARAM_LOCAL_V3_IDENTITY, localPeerKey);
    p.setProperty(ConfigManager.PARAM_NEW_SCHEDULER, "true");
    p.setProperty(IdentityManagerImpl.PARAM_INITIAL_PEERS,
                  StringUtil.separatedString(initialPeers, ";"));
    p.setProperty(V3Poller.PARAM_QUORUM, "3");
    p.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    p.setProperty(V3Poller.PARAM_STATE_PATH, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(p);
    idMgr = new MyIdentityManager();
    theDaemon.setIdentityManager(idMgr);
    idMgr.initService(theDaemon);
    idMgr.startService();
    theDaemon.getSchedService().startService();
    hashService.startService();
    theDaemon.getDatagramRouterManager().startService();
    theDaemon.getRouterManager().startService();
    theDaemon.getSystemMetrics().startService();
    theDaemon.getActivityRegulator(testau).startService();
    theDaemon.setNodeManager(new MockNodeManager(), testau);
    pollmanager.startService();
  }

  static class MyIdentityManager extends IdentityManagerImpl {
    IdentityAgreement findTestIdentityAgreement(PeerIdentity pid,
						ArchivalUnit au) {
      Map map = findAuAgreeMap(au);
      synchronized (map) {
	return findPeerIdentityAgreement(map, pid);
      }
    }

    public void storeIdentities() throws ProtocolException {
    }
  }

}
