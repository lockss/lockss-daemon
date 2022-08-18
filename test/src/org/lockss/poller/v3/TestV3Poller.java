/*
 * $Id$
 */

/*

 Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.plugin.*;
import org.lockss.plugin.base.DefaultUrlCacher;
import org.lockss.protocol.*;
import org.lockss.util.*;
import org.lockss.poller.*;
import org.lockss.poller.v3.V3Serializer.*;
import org.lockss.test.*;
import org.lockss.hasher.*;
import org.lockss.state.*;

import static org.lockss.util.Constants.*;

public class TestV3Poller extends LockssTestCase {

  private MyIdentityManager idMgr;
  private MockLockssDaemon theDaemon;

  private PeerIdentity pollerId;

  private String tempDirPath;
  private MockArchivalUnit testau;
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
  private byte[][] voterNonce2s;
  
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
    this.voterNonce2s = makeNonces();
    this.pollAcks = makePollAckMessages();
    this.nominates = makeNominateMessages();
    this.votes = makeVoteMessages();
    this.repairs = makeRepairMessages();
    ConfigurationUtil.addFromArgs(PollManager.PARAM_MIN_TIME_BETWEEN_ANY_POLL,
				  "" + (1 * SECOND));
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
    V3Poller poller = makeV3Poller("testing poll key");
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

    V3Poller poller = makeV3Poller("testing poll key");
    PeerIdentityStatus status = idMgr.getPeerIdentityStatus(pid);
    status.setLastMessageTime(lastMsg);
    status.setLastPollInvitationTime(lastInvite);
    if (highestAgreement >= 0) {
      idMgr.signalPartialAgreement(pid, testau, highestAgreement);
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

  public void testParticipantSizesAll() throws Exception {
    // All symmetric participants
    V3Poller v3Poller = makeInittedV3Poller("foo");
    doTestParticipantSizes(v3Poller, initialPeers.size(), voters.length);
  }
  public void testParticipantSizesTwo() throws Exception {
    // 2 symmetric participants
    V3Poller v3Poller = makeInittedV3Poller("foo", 2);
    doTestParticipantSizes(v3Poller, 2, voters.length);
  }
  public void testParticipantSizesNone() throws Exception {
    // No symmetric participants
    V3Poller v3Poller = makeInittedV3Poller("foo", 0);
    doTestParticipantSizes(v3Poller, 0, voters.length);
  }
  public void testParticipantSizesNoVoters() throws Exception {
    // No participants
    Properties p = new Properties();
    // Set PARAM_V3_ENABLE_LOCAL_POLLS true
    p.setProperty(V3Poller.PARAM_V3_ENABLE_LOCAL_POLLS, "true");
    // Set PARAM_V3_ALL_LOCAL_POLLS true
    p.setProperty(V3Poller.PARAM_V3_ALL_LOCAL_POLLS, "true");
    ConfigurationUtil.addFromProps(p);

    V3Poller v3Poller = makeInittedV3Poller("foo", 0, 0);
    doTestParticipantSizes(v3Poller, 0, 0);
  }

  protected void doTestParticipantSizes(V3Poller v3Poller, int numSym,
					int numVoters) throws Exception {
    Map<PeerIdentity,ParticipantUserData> innerCircle =
      theParticipants(v3Poller);
    assertEquals(numVoters, innerCircle.size());
    List<ParticipantUserData> symmetricParticipants =
      symmetricParticipants(v3Poller);
    assertTrue(symmetricParticipants.size() == numSym);
  }

  public void testSubstanceChecker() throws Exception {
    V3Poller v3Poller = makeInittedV3Poller("foo", 0, 0);
    List<String> pats = ListUtil.list("foo");
    testau.setSubstanceUrlPatterns(RegexpUtil.compileRegexps(pats));
    SubstanceChecker sub = v3Poller.makeSubstanceChecker();
    assertNull(sub);
    MockPlugin mplug = (MockPlugin)testau.getPlugin();
    mplug.setFeatureVersionMap(MapUtil.map(Plugin.Feature.Substance, "2"));
    sub = v3Poller.makeSubstanceChecker();
    assertNotNull(sub);
    AuState aus = AuUtil.getAuState(testau);
    assertEquals(SubstanceChecker.State.Unknown, aus.getSubstanceState());

    v3Poller.updateSubstance(sub);
    assertEquals(SubstanceChecker.State.No, aus.getSubstanceState());

    sub.checkSubstance("http://foo");
    v3Poller.updateSubstance(sub);
    assertEquals(SubstanceChecker.State.Yes, aus.getSubstanceState());
  }

  public void testMakeHasher() throws Exception {
    V3Poller v3Poller = makeInittedV3Poller("foo", 0, 0);
    BlockHasher hasher = v3Poller.makeHasher(testau.getAuCachedUrlSet(), -1,
					     false, null);
    assertFalse("Hasher: " + hasher + " shouldn't be a SampledBlockHasher",
		hasher instanceof SampledBlockHasher);
    assertTrue(hasher.isExcludeSuspectVersions());

    hasher = v3Poller.makeHasher(testau.getAuCachedUrlSet(), -1,
				 true, null);
    assertFalse("Hasher: " + hasher + " shouldn't be a SampledBlockHasher",
		hasher instanceof SampledBlockHasher);
    assertTrue(hasher.isExcludeSuspectVersions());
  }

  public void testMakeHasherSampled() throws Exception {
    ConfigurationUtil.addFromArgs(V3Poller.PARAM_V3_ENABLE_POP_POLLS, "true",
				  V3Poller.PARAM_V3_ALL_POP_POLLS, "true",
				  V3Poller.PARAM_V3_MODULUS, "1000");
    V3Poller v3Poller = makeInittedV3Poller("foo", 0, 0);
    BlockHasher hasher = v3Poller.makeHasher(testau.getAuCachedUrlSet(), -1,
					     false, null);
    assertTrue("Hasher: " + hasher + " should be a SampledBlockHasher",
		hasher instanceof SampledBlockHasher);
    assertTrue(hasher.isExcludeSuspectVersions());

    // Make a repair hasher, shouldn't be sampled
    hasher = v3Poller.makeHasher(testau.getAuCachedUrlSet(), -1,
				 true, null);
    assertFalse("Hasher: " + hasher + " shouldn't be a SampledBlockHasher",
		hasher instanceof SampledBlockHasher);
    assertTrue(hasher.isExcludeSuspectVersions());
  }

  public void testMakeHasherNoExcludeSuspect() throws Exception {
    ConfigurationUtil.addFromArgs(V3Poller.PARAM_V3_EXCLUDE_SUSPECT_VERSIONS,
				  "false");
    V3Poller v3Poller = makeInittedV3Poller("foo", 0, 0);
    BlockHasher hasher = v3Poller.makeHasher(testau.getAuCachedUrlSet(), -1,
					     false, null);
    assertFalse("Hasher: " + hasher + " shouldn't be a SampledBlockHasher",
		hasher instanceof SampledBlockHasher);
    assertFalse(hasher.isExcludeSuspectVersions());
  }

  public void testInitHasherByteArraysAll() throws Exception {
    // All symmetric participants
    V3Poller v3Poller = makeInittedV3Poller("foo");
    doTestInitHasherByteArrays(v3Poller, initialPeers.size());
  }
  public void testInitHasherByteArraysTwo() throws Exception {
    // 2 symmetric participants
    V3Poller v3Poller = makeInittedV3Poller("foo", 2);
    doTestInitHasherByteArrays(v3Poller, 2);
  }
  public void testInitHasherByteArraysNone() throws Exception {
    // No symmetric participants
    V3Poller v3Poller = makeInittedV3Poller("foo", 0);
    doTestInitHasherByteArrays(v3Poller, 0);
  }

  protected void doTestInitHasherByteArrays(V3Poller v3Poller,
					    int numSym) throws Exception {
    Map<PeerIdentity,ParticipantUserData> innerCircle =
      theParticipants(v3Poller);
    List<ParticipantUserData> symmetricParticipants =
      symmetricParticipants(v3Poller);
    byte[][] initBytes = (byte[][])PrivilegedAccessor.
        invokeMethod(v3Poller, "initHasherByteArrays");
    // Expected size is number of inner circle peers hash plus number
    // of peers that request symmetric polls plus one for the plain.
    int expectedSize = innerCircle.size() + symmetricParticipants.size() + 1;
    assertEquals(initBytes.length, expectedSize);
    byte[][] compareBytes = new byte[expectedSize][];
    int ix = 0;
    for (Iterator<ParticipantUserData> it = innerCircle.values().iterator();
	 it.hasNext();) {
      ParticipantUserData proxy = it.next();
      compareBytes[ix++] =
        ByteArray.concat(proxy.getPollerNonce(), proxy.getVoterNonce());
    }
    for (Iterator<ParticipantUserData> it = symmetricParticipants.iterator();
	 it.hasNext();) {
      ParticipantUserData proxy = it.next();
      assertNotNull(proxy.getVoterNonce2());
      compareBytes[ix++] =
        ByteArray.concat(proxy.getPollerNonce(), proxy.getVoterNonce2());
    }
    compareBytes[ix++] =  new byte[0]; // Plain hash 
    for (ix = 0; ix < initBytes.length; ix++) {
      assertTrue("Index " + ix + " bytes mismatch",
		 Arrays.equals(initBytes[ix], compareBytes[ix]));
    }
  }

  public void testInitHasherDigestsAll() throws Exception {
    V3Poller v3Poller = makeInittedV3Poller("foo");
    doTestInitHasherDigests(v3Poller, initialPeers.size());
  }

  public void testInitHasherDigestsTwo() throws Exception {
    V3Poller v3Poller = makeInittedV3Poller("foo", 2);
    doTestInitHasherDigests(v3Poller, 2);
  }

  public void testInitHasherDigestsNone() throws Exception {
    V3Poller v3Poller = makeInittedV3Poller("foo", 0);
    doTestInitHasherDigests(v3Poller, 0);
  }

  public void doTestInitHasherDigests(V3Poller v3Poller,
				      int numSym) throws Exception {
    Map<PeerIdentity,ParticipantUserData> innerCircle =
      theParticipants(v3Poller);
    MessageDigest[] digests = (MessageDigest[])PrivilegedAccessor.
      invokeMethod(v3Poller, "initHasherDigests");
    List<ParticipantUserData> symmetricParticipants =
      symmetricParticipants(v3Poller);
    // Expected size is number of inner circle peers hash plus number
    // of peers that request symmetric polls plus one for the plain.
    int expectedSize = innerCircle.size() + symmetricParticipants.size() + 1;
    assertEquals(digests.length, expectedSize);
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
    return makeHashBlock(url, content, 5);
  }

  private HashBlock makeHashBlock(String url, String content, int nHashes)
      throws Exception {
    MockCachedUrl cu = new MockCachedUrl(url);
    HashBlock hb = new HashBlock(cu);
    addVersion(hb, content, nHashes);
    return hb;
  }

  private static int hbVersionNum = 1;

  private void addVersion(HashBlock block, String content) throws Exception {
    addVersion(block, content, 5);  // 4 voters plus plain hash
  }

  private void addVersion(HashBlock block, String content, int nHashes)
      throws Exception {
    MessageDigest[] digests = new MessageDigest[nHashes];
    for (int ix = 0; ix < nHashes; ix++) {
      digests[ix] = MessageDigest.getInstance("SHA1");
      digests[ix].update(content.getBytes());
    }
    block.addVersion(0, content.length(), 
                     0, content.length(), 
                     digests.length * content.length(), // total bytes hashed
                     digests, TestV3Poller.hbVersionNum++, null);    
  }
  
  private VoteBlock makeVoteBlock(String url) {
    VoteBlock vb = new VoteBlock(url);
    return vb;
  }
  
  private VoteBlock makeVoteBlock(String url, String... contents)
      throws Exception {
    VoteBlock vb = new VoteBlock(url);
    for (String content: contents) {
      addVersion(vb, content);
    }
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

  /*
   * XXX DSHR this should be in TestPollManager since that's
   * XXX where the code is now.
   */
  public void testCountLastPoRAgreePeers() throws Exception {
    TimeBase.setSimulated(1000L);
    V3Poller v3Poller = makeV3Poller("testing poll key");
    ArchivalUnit au = v3Poller.getAu();
    assertNotNull(au);
    assertTrue(au instanceof MockArchivalUnit);
    MockArchivalUnit mau = (MockArchivalUnit)au;
    AuState aus = AuUtil.getAuState(au);
    assertNotNull(aus);
    assertTrue(aus instanceof MockAuState);
    MockAuState maus = (MockAuState)aus;
    maus.setLastTopLevelPollTime(200L);
    assertEquals(200L, maus.getLastTopLevelPollTime());
    maus.setPollDuration(100L);
    assertEquals(100L, maus.getPollDuration());
    Map agreeMap = idMgr.getAgreed(mau);
    assertNotNull(agreeMap);
    assertEquals(0L, agreeMap.size());
    PeerIdentity p1 = findPeerIdentity("TCP:[127.0.0.1]:5009");
    assertFalse(p1.isLocalIdentity());
    PeerIdentity p2 = findPeerIdentity("TCP:[1.2.3.4]:5009");
    assertFalse(p2.isLocalIdentity());
    PeerIdentity p3 = findPeerIdentity("TCP:[1.2.3.7]:1111");
    assertFalse(p3.isLocalIdentity());
    PeerIdentity p4 = findPeerIdentity("TCP:[1.2.3.8]:1111");
    assertFalse(p4.isLocalIdentity());
    PeerIdentity p5 = findPeerIdentity("TCP:[4.5.6.2]:1111");
    assertFalse(p5.isLocalIdentity());
    // Add local ID - local poll finished at 900
    agreeMap.put(pollerId, 900L);
    assertEquals(0, pollmanager.countLastPoRAgreePeers(mau, maus));
    // Add result of PoP poll finished at 700
    agreeMap.put(p5, 700L);
    agreeMap.put(p4, 700L);
    assertEquals(0, pollmanager.countLastPoRAgreePeers(mau, maus));
    // Add result of PoR poll between 0 and 100
    agreeMap.put(p3, 10L);
    agreeMap.put(p2, 20L);
    agreeMap.put(p1, 30L);
    assertEquals(0, pollmanager.countLastPoRAgreePeers(mau, maus));
    // Add result of PoR poll between 100 and 200
    agreeMap.put(p3, 150L);
    agreeMap.put(p2, 160L);
    agreeMap.put(p1, 170L);
    assertEquals(3, pollmanager.countLastPoRAgreePeers(mau, maus));
    // Make it look like p1 subsequently agreed in a PoP poll
    agreeMap.put(p3, 500L);
    assertEquals(2, pollmanager.countLastPoRAgreePeers(mau, maus));
  }

  /*
   * XXX DSHR these should be in TestPollManager since that's
   * XXX where the code is now.
   */
  public void testChoosePollVariantPorOnly() throws Exception {
    testChoosePollVariant(false, false);
  }

  public void testChoosePollVariantPoRPoP() throws Exception {
    testChoosePollVariant(true, false);
  }

  public void testChoosePollVariantPoRLocal() throws Exception {
    testChoosePollVariant(false, true);
  }

  public void testChoosePollVariantPoRPoPLocal() throws Exception {
    testChoosePollVariant(true, true);
  }

  private V3Poller.PollVariant choosePollVariant(ArchivalUnit au) {
    long maxDelayBetweenPoR =
      PollManager.DEFAULT_MAX_DELAY_BETWEEN_POR_MULTIPLIER *
      PollManager.DEFAULT_TOPLEVEL_POLL_INTERVAL;
    return pollmanager.choosePollVariant(au, maxDelayBetweenPoR);
  }

  /*
   * The choosePollVariant() logic has the following cases:
   * A) Too soon to call poll - NoPoll
   * B) PoP poll forced - PoP
   * C) Local poll forced - Local
   * D) suspect versions found - PoR
   * E) content changed since last PoR - PoR
   * F) good agreement last PoR & too few repairers - PoP
   * G) good agreement last PoR & enough repairers - Local
   * H) poor agreement last PoR - PoR
   */
  public void testChoosePollVariant(boolean enablePoPPolls,
				    boolean enableLocalPolls) throws Exception {
    ConfigurationUtil.addFromArgs(V3Poller.PARAM_V3_ENABLE_POP_POLLS,
				  "" + enablePoPPolls,
				  V3Poller.PARAM_V3_ENABLE_LOCAL_POLLS,
				  "" + enableLocalPolls,
				  V3Poller.PARAM_THRESHOLD_REPAIRERS_LOCAL_POLLS,
				  "3",
				  PollManager.PARAM_MIN_AGREE_PEERS_LAST_POR_POLL,
				  "2");
    ConfigurationUtil.addFromArgs(BlockHasher.PARAM_ENABLE_LOCAL_HASH,
				  "true",
				  BlockHasher.PARAM_LOCAL_HASH_ALGORITHM,
				  BlockHasher.DEFAULT_LOCAL_HASH_ALGORITHM,
				  DefaultUrlCacher.PARAM_CHECKSUM_ALGORITHM,
				  BlockHasher.DEFAULT_LOCAL_HASH_ALGORITHM,
				  V3Poller.PARAM_V3_MODULUS, "2");
    TimeBase.setSimulated(1000L);
    V3Poller v3Poller = makeV3Poller("testing poll key");
    ArchivalUnit au = v3Poller.getAu();
    assertNotNull(au);
    assertTrue(au instanceof MockArchivalUnit);
    MockArchivalUnit mau = (MockArchivalUnit)au;
    AuState aus = AuUtil.getAuState(au);
    assertNotNull(aus);
    assertTrue(aus instanceof MockAuState);
    MockAuState maus = (MockAuState)aus;
    PollSpec ps = v3Poller.getPollSpec();
    assertNotNull(ps);
    Map agreeMap = idMgr.getAgreed(mau);
    assertNotNull(agreeMap);
    assertEquals(0L, agreeMap.size());
    PeerIdentity p1 = findPeerIdentity("TCP:[127.0.0.1]:5009");
    assertFalse(p1.isLocalIdentity());
    PeerIdentity p2 = findPeerIdentity("TCP:[1.2.3.4]:5009");
    assertFalse(p2.isLocalIdentity());
    PeerIdentity p3 = findPeerIdentity("TCP:[1.2.3.7]:1111");
    assertFalse(p3.isLocalIdentity());
    PeerIdentity p4 = findPeerIdentity("TCP:[1.2.3.8]:1111");
    assertFalse(p4.isLocalIdentity());
    PeerIdentity p5 = findPeerIdentity("TCP:[4.5.6.2]:1111");
    assertFalse(p5.isLocalIdentity());
    // First poll is PoR - case H
    assertEquals(V3Poller.PollVariant.PoR, choosePollVariant(au));
    // Now crawl and get content
    maus.setLastContentChange(100);
    maus.setLastCrawlTime(100);
    // Case A
    maus.setLastPollStart(1000L);
    assertEquals(V3Poller.PollVariant.NoPoll,
		 choosePollVariant(au));
    maus.setLastPollStart(0L);
    // Case E
    assertEquals(V3Poller.PollVariant.PoR, choosePollVariant(au));
    // Now poll but get disagreement
    maus.setLastTopLevelPollTime(200);
    maus.setPollDuration(100L);
    // Case H
    assertEquals(V3Poller.PollVariant.PoR, choosePollVariant(au));
    // Now get 2 agreements, repairer threshold is 3
    agreeMap.put(p3, 150L);
    agreeMap.put(p2, 160L);
    // We have to update the AuState too
    aus.setNumAgreePeersLastPoR(2);
    aus.setNumWillingRepairers(2);
    // Case F
    assertEquals(  enablePoPPolls
		   ? V3Poller.PollVariant.PoP : V3Poller.PollVariant.PoR,
		 choosePollVariant(au));
    // Add another agreement
    agreeMap.put(p1, 170L);
    aus.setNumAgreePeersLastPoR(3);
    aus.setNumWillingRepairers(3);
    // Case G
    assertEquals(  enableLocalPolls
		   ? V3Poller.PollVariant.Local : V3Poller.PollVariant.PoR,
		 choosePollVariant(au));
    // Now crawl again, but get no content
    maus.setLastCrawlTime(300);
    // Case G
    assertEquals(  enableLocalPolls
		   ? V3Poller.PollVariant.Local : V3Poller.PollVariant.PoR,
		 choosePollVariant(au));
    // Now crawl again, get content
    maus.setLastCrawlTime(300);
    maus.setLastContentChange(300);
    // Case E
    assertEquals(V3Poller.PollVariant.PoR,
		 choosePollVariant(au));
    // Now poll but get disagreement
    maus.setLastTopLevelPollTime(500);
    maus.setPollDuration(100L);
    aus.setNumAgreePeersLastPoR(0);
    // Case H
    assertEquals(V3Poller.PollVariant.PoR, choosePollVariant(au));
    // Now get 3 agreement, repairer threshold is 3
    agreeMap.put(p3, 450L);
    agreeMap.put(p2, 460L);
    agreeMap.put(p1, 460L);
    aus.setNumAgreePeersLastPoR(3);
    // Case G
    assertEquals(  enableLocalPolls
		   ? V3Poller.PollVariant.Local : V3Poller.PollVariant.PoR,
		 choosePollVariant(au));
    // Case D - XXX not yet implemented
    if (enableLocalPolls) {
      // Case C
      ConfigurationUtil.addFromArgs(V3Poller.PARAM_V3_ALL_LOCAL_POLLS, "true");
      assertEquals(V3Poller.PollVariant.Local,
		   choosePollVariant(au));
    }
    if (enablePoPPolls) {
      // Case B
      ConfigurationUtil.addFromArgs(V3Poller.PARAM_V3_ALL_POP_POLLS, "true");
      assertEquals(V3Poller.PollVariant.PoP,
		   choosePollVariant(au));
    }

  }

  public void testIsPeerEligible() throws Exception {
    V3Poller v3Poller = makeV3Poller("testing poll key");
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

  public void testGetAvailablePeers() throws Exception {
    PeerIdentity p1 = findPeerIdentity("TCP:[10.1.0.100]:9729");
    PeerIdentity p2 = findPeerIdentity("TCP:[10.1.0.101]:9729");

    DatedPeerIdSet noAuSet = pollmanager.getNoAuPeerSet(testau);
    synchronized (noAuSet) {
      noAuSet.add(p2);
    }	
    assertTrue(noAuSet.contains(p2));

    V3Poller v3Poller = makeV3Poller("testing poll key");
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
  
  public void testGetAvailablePeersInitialPeersOnly() throws Exception {
    ConfigurationUtil.addFromArgs(V3Poller.PARAM_ENABLE_DISCOVERY, "false");
    findPeerIdentity("TCP:[10.1.0.100]:9729");
    findPeerIdentity("TCP:[10.1.0.101]:9729");
    V3Poller v3Poller = makeV3Poller("testing poll key");
    assertNotNull(getAvailablePeers(v3Poller));
    assertEquals(6, getAvailablePeers(v3Poller).size());
  }
  
  public void testGetAvailablePeersDoesNotIncludeLocalIdentity() throws Exception {
    ConfigurationUtil.addFromArgs(V3Poller.PARAM_ENABLE_DISCOVERY, "false");
    // append our local config to the initial Peer List
    List initialPeersCopy = new ArrayList(initialPeers);
    initialPeersCopy.add(localPeerKey);
    
    ConfigurationUtil.addFromArgs(IdentityManagerImpl.PARAM_INITIAL_PEERS,
                                  StringUtil.separatedString(initialPeersCopy, ";"));
    
    V3Poller v3Poller = makeV3Poller("testing poll key");
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
    MyV3Poller poller = makeV3Poller("testing poll key");
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

    MyV3Poller poller = makeV3Poller("testing poll key");
    
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

    testau.setUrlPsllResultMap(PatternFloatMap.fromSpec(".*foo2.*,0.5"));
    V3Poller v3Poller = makeV3Poller("testing poll key");
    
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
    v3Poller.lockParticipants();
    // Finally, let's test.
    
    BlockTally tally;
    
    // The results expected are based on a quorum of 3.
    assertEquals(3, v3Poller.getQuorum());
    assertEquals(75, v3Poller.getVoteMargin());

    tally = v3Poller.tallyBlock(hashblocks[0]);
    assertEquals(BlockTally.Result.WON, tally.getTallyResult());
    assertSameElements(v3Poller.theParticipants.values(),
		       tally.getAgreeVoters());
    try {
      tally.getRepairVoters();
      fail("expected ShouldNotHappenException was not thrown.");
    } catch (ShouldNotHappenException ex) {
      // Expected
    }

    tally = v3Poller.tallyBlock(hashblocks[1]);
    assertEquals(BlockTally.Result.LOST_POLLER_ONLY_BLOCK,
		 tally.getTallyResult());
    assertSameElements(v3Poller.theParticipants.values(),
		       tally.getPollerOnlyBlockVoters());
    try {
      tally.getRepairVoters();
      fail("expected ShouldNotHappenException was not thrown.");
    } catch (ShouldNotHappenException ex) {
      // Expected
    }
    
    tally = v3Poller.tallyBlock(hashblocks[2]);
    assertEquals(BlockTally.Result.WON, tally.getTallyResult());
    assertSameElements(v3Poller.theParticipants.values(),
		       tally.getAgreeVoters());

    assertEquals("2/0/1/1/0/0",
		 v3Poller.theParticipants.get(id1).getVoteCounts().votes());
    assertEquals("2/0/1/1/0/0",
		 v3Poller.theParticipants.get(id2).getVoteCounts().votes());
    // This voter sees a "neither" URL, since neither it nor the
    // poller has foo2a.
    assertEquals("2/0/1/0/1/0",
		 v3Poller.theParticipants.get(id3).getVoteCounts().votes());

    assertEquals(0.66667, v3Poller.getPercentAgreement(), 0.001);
    assertEquals(0.8, v3Poller.getWeightedPercentAgreement(), 0.001);
  }
  
  public void testTallyBlocksSucceedsWithNoVersionVote() throws Exception {

    V3Poller v3Poller = makeV3Poller("testing poll key");
    
    PeerIdentity id1 = findPeerIdentity("TCP:[127.0.0.1]:8990");
    PeerIdentity id2 = findPeerIdentity("TCP:[127.0.0.1]:8991");
    PeerIdentity id3 = findPeerIdentity("TCP:[127.0.0.1]:8992");
    PeerIdentity id4 = findPeerIdentity("TCP:[127.0.0.1]:8993");
        
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
    
    VoteBlock [] voter4_voteblocks =
    {
      makeVoteBlock("http://test.com/foo1"), // voter3 votes "present"
      makeVoteBlock("http://test.com/foo2a", "content for foo2a"),
      makeVoteBlock("http://test.com/foo3", "content for foo3")
    };
    
    v3Poller.theParticipants.put(id1, makeParticipant(id1, v3Poller,
                                                      voter1_voteblocks));
    v3Poller.theParticipants.put(id2, makeParticipant(id2, v3Poller,
                                                      voter2_voteblocks));
    v3Poller.theParticipants.put(id3, makeParticipant(id3, v3Poller,
                                                      voter3_voteblocks));
    v3Poller.theParticipants.put(id4, makeParticipant(id4, v3Poller,
                                                      voter4_voteblocks));
    v3Poller.lockParticipants();
    // Finally, let's test.
    
    BlockTally tally;
    
    // The results expected are based on a quorum of 3.
    assertEquals(3, v3Poller.getQuorum());
    assertEquals(75, v3Poller.getVoteMargin());

    tally = v3Poller.tallyBlock(hashblocks[0]);
    assertEquals(BlockTally.Result.WON, tally.getTallyResult());
    // All but id4 should agree
    assertEquals(3, tally.getAgreeVoters().size());
    Collection<ParticipantUserData> disagree = tally.getDisagreeVoters();
    assertEquals(1, disagree.size());
    assertTrue(disagree.contains(v3Poller.theParticipants.get(id4)));
    try {
      tally.getRepairVoters();
      fail("expected ShouldNotHappenException was not thrown.");
    } catch (ShouldNotHappenException ex) {
      // Expected
    }

    tally = v3Poller.tallyBlock(hashblocks[1]);
    assertEquals(BlockTally.Result.LOST_POLLER_ONLY_BLOCK,
		 tally.getTallyResult());
    assertSameElements(v3Poller.theParticipants.values(),
		       tally.getPollerOnlyBlockVoters());
    try {
      tally.getRepairVoters();
      fail("expected ShouldNotHappenException was not thrown.");
    } catch (ShouldNotHappenException ex) {
      // Expected
    }
    
    tally = v3Poller.tallyBlock(hashblocks[2]);
    assertEquals(BlockTally.Result.WON, tally.getTallyResult());
    assertSameElements(v3Poller.theParticipants.values(),
		       tally.getAgreeVoters());

    // String is agree/disagree/pollerOnly/voterOnly/neither/spoiled
    assertEquals("2/0/1/1/0/0",
		 v3Poller.theParticipants.get(id1).getVoteCounts().votes());
    assertEquals("2/0/1/1/0/0",
		 v3Poller.theParticipants.get(id2).getVoteCounts().votes());
    // This voter sees a "neither" URL, since neither it nor the
    // poller has foo2a.
    assertEquals("2/0/1/0/1/0",
		 v3Poller.theParticipants.get(id3).getVoteCounts().votes());
    assertEquals("1/1/1/1/0/0",
		 v3Poller.theParticipants.get(id4).getVoteCounts().votes());

    assertEquals(0.66667, v3Poller.getPercentAgreement(), 0.01);
    assertEquals(0.66667, v3Poller.getWeightedPercentAgreement(), 0.01);
  }
  
  private Collection<String> publisherRepairUrls(V3Poller v3Poller) {
    PollerStateBean.RepairQueue repairQueue =
      v3Poller.getPollerStateBean().getRepairQueue();
    return repairQueue.getPendingPublisherRepairUrls();
  }
  
  private Collection<String> peerRepairUrls(V3Poller v3Poller) {
    PollerStateBean.RepairQueue repairQueue =
      v3Poller.getPollerStateBean().getRepairQueue();
    List<PollerStateBean.Repair> peerRepairs =
      repairQueue.getPendingPeerRepairs();
    Collection<String> peerRepairUrls = new ArrayList<String>();
    for (PollerStateBean.Repair repair: peerRepairs) {
      peerRepairUrls.add(repair.getUrl());
    }
    return peerRepairUrls;
  }

  public void testRequestRepair() throws Exception {
    MyV3Poller v3Poller;

    // 0% repair from cache: the repair goes to the publisher
    ConfigurationUtil.addFromArgs(V3Poller.PARAM_V3_REPAIR_FROM_CACHE_PERCENT,
				  "0");
    v3Poller = makeInittedV3Poller("foo");
    v3Poller.requestRepair("http://example.com",
			   theParticipants(v3Poller).values());

    assertSameElements(Arrays.asList("http://example.com"),
		       publisherRepairUrls(v3Poller));
    assertEmpty(peerRepairUrls(v3Poller));

    // 100% repair from cache: the repair goes to a peer
    ConfigurationUtil.addFromArgs(V3Poller.PARAM_V3_REPAIR_FROM_CACHE_PERCENT,
				  "100");
    v3Poller = makeInittedV3Poller("foo");
    v3Poller.requestRepair("http://example.com",
			   theParticipants(v3Poller).values());

    assertEmpty(publisherRepairUrls(v3Poller));
    assertSameElements(Arrays.asList("http://example.com"),
		peerRepairUrls(v3Poller));

    // 0% repair from cache, and no repairers: the repair goes to the
    // publisher
    ConfigurationUtil.addFromArgs(V3Poller.PARAM_V3_REPAIR_FROM_CACHE_PERCENT,
				  "0");
    v3Poller = makeInittedV3Poller("foo");
    v3Poller.requestRepair("http://example.com", Collections.EMPTY_LIST);    
    
    assertSameElements(Arrays.asList("http://example.com"),
		       publisherRepairUrls(v3Poller));
    assertEmpty(peerRepairUrls(v3Poller));

    // 100% repair from cache, BUT no repairers: the repair goes to
    // the publisher ANYWAY
    ConfigurationUtil.addFromArgs(V3Poller.PARAM_V3_REPAIR_FROM_CACHE_PERCENT,
				  "100");
    v3Poller = makeInittedV3Poller("foo");
    v3Poller.requestRepair("http://example.com",
			   Collections.EMPTY_LIST);
    
    assertSameElements(Arrays.asList("http://example.com"),
		       publisherRepairUrls(v3Poller));
    assertEmpty(peerRepairUrls(v3Poller));

  }

  public void testRequestRepairPubDown() throws Exception {
    MyV3Poller v3Poller;

    // Mark AU down.
    testau.setConfiguration(
      ConfigurationUtil.fromArgs(ConfigParamDescr.PUB_DOWN.getKey(), "true"));

    // 100% repair from cache
    ConfigurationUtil.addFromArgs(V3Poller.PARAM_V3_REPAIR_FROM_CACHE_PERCENT,
				  "100");
    v3Poller = makeInittedV3Poller("foo");
    v3Poller.requestRepair("http://example.com",
			   theParticipants(v3Poller).values());

    assertEmpty(publisherRepairUrls(v3Poller));
    assertSameElements(Arrays.asList("http://example.com"),
		peerRepairUrls(v3Poller));

    // 100% repair from cache, BUT no repairers.
    ConfigurationUtil.addFromArgs(V3Poller.PARAM_V3_REPAIR_FROM_CACHE_PERCENT,
				  "100");
    v3Poller = makeInittedV3Poller("foo");
    v3Poller.requestRepair("http://example.com",
			   Collections.EMPTY_LIST);
    
    assertEmpty(publisherRepairUrls(v3Poller));
    assertEmpty(peerRepairUrls(v3Poller));

    // 0% repair from cache; repair from cache anyway since pub down.
    ConfigurationUtil.addFromArgs(V3Poller.PARAM_V3_REPAIR_FROM_CACHE_PERCENT,
				  "0");
    v3Poller = makeInittedV3Poller("foo");
    v3Poller.requestRepair("http://example.com",
			   theParticipants(v3Poller).values());

    assertEmpty(publisherRepairUrls(v3Poller));
    assertSameElements(Arrays.asList("http://example.com"),
		peerRepairUrls(v3Poller));

    // 0% repair from cache, BUT no repairers.
    ConfigurationUtil.addFromArgs(V3Poller.PARAM_V3_REPAIR_FROM_CACHE_PERCENT,
				  "0");
    v3Poller = makeInittedV3Poller("foo");
    v3Poller.requestRepair("http://example.com",
			   Collections.EMPTY_LIST);
    
    assertEmpty(publisherRepairUrls(v3Poller));
    assertEmpty(peerRepairUrls(v3Poller));
  }

  public void testRequestRepairUseVersionCounts() throws Exception {
    MyV3Poller v3Poller;
    
    PeerIdentity id1 = findPeerIdentity("TCP:[127.0.0.1]:8990");
    PeerIdentity id2 = findPeerIdentity("TCP:[127.0.0.1]:8991");
    PeerIdentity id3 = findPeerIdentity("TCP:[127.0.0.1]:8992");
    PeerIdentity id4 = findPeerIdentity("TCP:[127.0.0.1]:8993");
        
    String [] urls_poller =
    { 
     "http://test.com/foo1"
    };
    
    HashBlock [] hashblocks =
    {
     makeHashBlock("http://test.com/foo1", "content for foo1HHH")
    };
    
    VoteBlock [] voter1_voteblocks =
    {
     makeVoteBlock("http://test.com/foo1", "content for foo1zzz",
		   "content for foo1")
    };
    
    VoteBlock [] voter2_voteblocks =
    {
     makeVoteBlock("http://test.com/foo1", "content for foo1yyy",
		   "content for foo1")
    };
    
    VoteBlock [] voter3_voteblocks =
    {
     makeVoteBlock("http://test.com/foo1", "content for foo1xxx",
		   "content for foo1")
    };
    
    VoteBlock [] voter4_voteblocks =
    {
     makeVoteBlock("http://test.com/foo1", "content for foo1www",
		   "content for foo1")
    };
    
    BlockTally tally;
    v3Poller = makeV3Poller("testing poll key");

    // The results expected are based on a quorum of 3.
    assertEquals(3, v3Poller.getQuorum());
    assertEquals(75, v3Poller.getVoteMargin());

    // Prefer the cache in all cases
    ConfigurationUtil.addFromArgs(V3Poller.PARAM_V3_REPAIR_FROM_CACHE_PERCENT,
				  "100");
    ConfigurationUtil.addFromArgs(V3Poller.PARAM_USE_VERSION_COUNTS, "true");

    v3Poller = makeV3Poller("testing poll key");
    
    v3Poller.theParticipants.put(id1, makeParticipant(id1, v3Poller,
                                                      voter1_voteblocks));
    v3Poller.theParticipants.put(id2, makeParticipant(id2, v3Poller,
                                                      voter2_voteblocks));
    v3Poller.theParticipants.put(id3, makeParticipant(id3, v3Poller,
                                                      voter3_voteblocks));
    v3Poller.theParticipants.put(id4, makeParticipant(id4, v3Poller,
                                                      voter4_voteblocks));
    v3Poller.lockParticipants();

    tally = v3Poller.tallyBlock(hashblocks[0]);
    assertEquals(BlockTally.Result.LOST, tally.getTallyResult());
    assertEmpty(tally.getRepairVoters());
    assertSameElements(theParticipants(v3Poller).values(),
		       tally.getDisagreeVoters());
    
    assertSameElements(Arrays.asList("http://test.com/foo1"),
		       publisherRepairUrls(v3Poller));
    assertEmpty(peerRepairUrls(v3Poller));

    ConfigurationUtil.addFromArgs(V3Poller.PARAM_USE_VERSION_COUNTS, "false");

    v3Poller = makeV3Poller("testing poll key");
    
    v3Poller.theParticipants.put(id1, makeParticipant(id1, v3Poller,
                                                      voter1_voteblocks));
    v3Poller.theParticipants.put(id2, makeParticipant(id2, v3Poller,
                                                      voter2_voteblocks));
    v3Poller.theParticipants.put(id3, makeParticipant(id3, v3Poller,
                                                      voter3_voteblocks));
    v3Poller.theParticipants.put(id4, makeParticipant(id4, v3Poller,
                                                      voter4_voteblocks));
    v3Poller.lockParticipants();
    tally = v3Poller.tallyBlock(hashblocks[0]);

    assertEquals(BlockTally.Result.LOST, tally.getTallyResult());
    assertEmpty(tally.getRepairVoters());
    assertSameElements(theParticipants(v3Poller).values(),
		       tally.getDisagreeVoters());
    assertEmpty(publisherRepairUrls(v3Poller));
    assertSameElements(Arrays.asList("http://test.com/foo1"),
		       peerRepairUrls(v3Poller));
  }

  public void testHeadVersionRepair() throws Exception {
    MyV3Poller v3Poller;
    
    PeerIdentity id1 = findPeerIdentity("TCP:[127.0.0.1]:8990");
    PeerIdentity id2 = findPeerIdentity("TCP:[127.0.0.1]:8991");
    PeerIdentity id3 = findPeerIdentity("TCP:[127.0.0.1]:8992");
    PeerIdentity id4 = findPeerIdentity("TCP:[127.0.0.1]:8993");
        
    String [] urls_poller =
    { 
     "http://test.com/foo1"
    };
    
    HashBlock [] hashblocks =
    {
     makeHashBlock("http://test.com/foo1", "content for foo1HHH")
    };
    
    VoteBlock [] voter1_voteblocks =
    {
     makeVoteBlock("http://test.com/foo1", "content for foo1zzz",
		   "content for foo1")
    };
    
    VoteBlock [] voter2_voteblocks =
    {
     makeVoteBlock("http://test.com/foo1", "content for foo1yyy",
		   "content for foo1")
    };
    
    VoteBlock [] voter3_voteblocks =
    {
     makeVoteBlock("http://test.com/foo1", "content for foo1xxx",
		   "content for foo1")
    };
    
    VoteBlock [] voter4_voteblocks =
    {
     makeVoteBlock("http://test.com/foo1", "content for foo1www",
		   "content for foo1")
    };
    
    BlockTally tally;
    v3Poller = makeV3Poller("testing poll key");

    // The results expected are based on a quorum of 3.
    assertEquals(3, v3Poller.getQuorum());
    assertEquals(75, v3Poller.getVoteMargin());

    // Prefer the cache in all cases
    ConfigurationUtil.addFromArgs(V3Poller.PARAM_V3_REPAIR_FROM_CACHE_PERCENT,
				  "100");
    ConfigurationUtil.addFromArgs(V3Poller.PARAM_USE_VERSION_COUNTS, "true");

    v3Poller = makeV3Poller("testing poll key");
    
    v3Poller.theParticipants.put(id1, makeParticipant(id1, v3Poller,
                                                      voter1_voteblocks));
    v3Poller.theParticipants.put(id2, makeParticipant(id2, v3Poller,
                                                      voter2_voteblocks));
    v3Poller.theParticipants.put(id3, makeParticipant(id3, v3Poller,
                                                      voter3_voteblocks));
    v3Poller.theParticipants.put(id4, makeParticipant(id4, v3Poller,
                                                      voter4_voteblocks));
    v3Poller.lockParticipants();

    tally = v3Poller.tallyBlock(hashblocks[0]);
    assertEquals(BlockTally.Result.LOST, tally.getTallyResult());
    assertEmpty(tally.getRepairVoters());
    assertSameElements(theParticipants(v3Poller).values(),
		       tally.getDisagreeVoters());
    
    // Repair from cache not possible; nobody has that head version.
    assertSameElements(Arrays.asList("http://test.com/foo1"),
		       publisherRepairUrls(v3Poller));
    assertEmpty(peerRepairUrls(v3Poller));

    // Change voter2 to have the head version be the popular version.
    VoteBlock[] exposed_content_voter2_voteblocks = {
      makeVoteBlock("http://test.com/foo1", "content for foo1",
		    "content for foo1yyy")
    };

    v3Poller = makeV3Poller("testing poll key");
    
    v3Poller.theParticipants.put(id1, makeParticipant(id1, v3Poller,
                                                      voter1_voteblocks));
    v3Poller.theParticipants.put(id2, makeParticipant(id2, v3Poller,
                                                      exposed_content_voter2_voteblocks));
    v3Poller.theParticipants.put(id3, makeParticipant(id3, v3Poller,
                                                      voter3_voteblocks));
    v3Poller.theParticipants.put(id4, makeParticipant(id4, v3Poller,
                                                      voter4_voteblocks));
    v3Poller.lockParticipants();
    

    tally = v3Poller.tallyBlock(hashblocks[0]);
    assertEquals(BlockTally.Result.LOST, tally.getTallyResult());
    assertSameElements(Arrays.asList(theParticipants(v3Poller).get(id2)), 
		       tally.getRepairVoters());
    assertSameElements(theParticipants(v3Poller).values(),
		       tally.getDisagreeVoters());
    
    // Repair from cache
    assertEmpty(publisherRepairUrls(v3Poller));
    assertSameElements(Arrays.asList("http://test.com/foo1"),
		       peerRepairUrls(v3Poller));
  }

  public void testBlockCompare() throws Exception {
//
//    V3Poller v3Poller = makeV3Poller("testing poll key");
//    
//    PeerIdentity id1 = findPeerIdentity("TCP:[127.0.0.1]:8990");
//    PeerIdentity id2 = findPeerIdentity("TCP:[127.0.0.1]:8991");
//    PeerIdentity id3 = findPeerIdentity("TCP:[127.0.0.1]:8992");
//    PeerIdentity id4 = findPeerIdentity("TCP:[127.0.0.1]:8993");
//    
//    String url = "http://www.test.com/example.txt";
//    
//    String n1v1 = "This is node 1, version 1.  It's the oldest.";
//    String n1v2 = "This is node 1, version 2.  It's slightly older.";
//    String n1v3 = "This is node 1, version 3.  This is the current version!";
//
//    // Our hash block only has v1 and v3, not v2
//    HashBlock hb1 = makeHashBlock(url);
//    addVersion(hb1, n1v1);
//    addVersion(hb1, n1v3);
//    UrlTallier.HashBlockComparer comparer = 
//      new UrlTallier.HashBlockComparer(hb1);
//
//    // Should agree on n1v1.
//    VoteBlock vb1 = makeVoteBlock(url);
//    addVersion(vb1, n1v1);
//    // NOTE: The participantIndex passed to compare is not relevent:
//    // All the nonces in the HashBlock are the same, so the expected
//    // hashes are the same for each participant.
//    assertTrue(comparer.compare(vb1, 0));
//
//    // Should agree on n1v1 and n1v3.
//    VoteBlock vb2 = makeVoteBlock(url);
//    addVersion(vb2, n1v1);
//    addVersion(vb2, n1v3);
//    assertTrue(comparer.compare(vb2, 1));
//    
//    // Should agree on n1v3.
//    VoteBlock vb3 = makeVoteBlock(url);
//    addVersion(vb3, n1v2);
//    addVersion(vb3, n1v3);
//    assertTrue(comparer.compare(vb3, 2));
//    
//    // Should not agree on any version, since the HashBlock doesn't
//    // have n1v2.
//    VoteBlock vb4 = makeVoteBlock(url);
//    addVersion(vb4, n1v2);
//    assertFalse(comparer.compare(vb4, 3));
  }
  
  public void testSignalAuEvent() throws Exception {
    MyV3Poller poller = makeV3Poller("testing poll key");
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
    @Override public void auContentChanged(AuEvent event, ArchivalUnit au,
					   AuEventHandler.ChangeInfo info) {
      changeEvents.add(info);
    }
  }

  // Tests that rely on what V3Poller.getPollerUrlTally() and pals return.
  private void tallyPollerUrl(V3Poller poller, UrlTallier urlTallier,
			      String url, HashBlock hashBlock) {
    VoteBlockTallier voteBlockTallier = poller.getPollerUrlTally(hashBlock);
    urlTallier.voteAllParticipants(url, voteBlockTallier);
  }

  private BlockTally tallyVoterUrl(V3Poller poller, UrlTallier urlTallier,
				   String url) {
    VoteBlockTallier voteBlockTallier = poller.getVoterUrlTally();
    urlTallier.voteAllParticipants(url, voteBlockTallier);
    return voteBlockTallier.getBlockTally();
  }

  public void testTallyPollerUrl() throws Exception {

    V3Poller v3Poller = makeSizedV3Poller("testing poll key", 3);
    
    PeerIdentity id1 = findPeerIdentity("TCP:[127.0.0.1]:8990");
    PeerIdentity id2 = findPeerIdentity("TCP:[127.0.0.1]:8991");
    PeerIdentity id3 = findPeerIdentity("TCP:[127.0.0.1]:8992");
    PeerIdentity id4 = findPeerIdentity("TCP:[127.0.0.1]:8994");
    
    HashBlock [] hashblocks = {
	makeHashBlock("http://test.com/foo1", "content for foo1"),
	makeHashBlock("http://test.com/foo2", "content for foo2"),
	makeHashBlock("http://test.com/foo3", "content for foo3"),
	makeHashBlock("http://test.com/foo4", "content for foo4")
      };

    VoteBlock [] voter1_voteblocks = {
	makeVoteBlock("http://test.com/foo1", "content for foo1"),
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo3", "content for foo3")
      };
    
    VoteBlock [] voter2_voteblocks = {
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo3", "content for foo3")
      };
    
    VoteBlock [] voter3_voteblocks = {
	makeVoteBlock("http://test.com/foo3", "content for foo3"),
	makeVoteBlock("http://test.com/foo4", "content for foo4")
      };

    List<ParticipantUserData> theParticipants =
      new ArrayList<ParticipantUserData>();

    theParticipants.add(makeParticipant(id1, v3Poller,
					voter1_voteblocks));
    theParticipants.add(makeParticipant(id2, v3Poller,
					voter2_voteblocks));
    theParticipants.add(makeParticipant(id3, v3Poller,
					voter3_voteblocks));
    UrlTallier urlTallier = new UrlTallier(theParticipants);
    assertEquals("http://test.com/foo1", urlTallier.peekUrl());
    tallyPollerUrl(v3Poller, urlTallier, "http://test.com/foo1", hashblocks[0]);
    assertEquals("http://test.com/foo2", urlTallier.peekUrl());
    tallyPollerUrl(v3Poller, urlTallier, "http://test.com/foo2", hashblocks[1]);
    assertEquals("http://test.com/foo3", urlTallier.peekUrl());
    tallyPollerUrl(v3Poller, urlTallier, "http://test.com/foo3", hashblocks[2]);
    assertEquals("http://test.com/foo4", urlTallier.peekUrl());
    tallyPollerUrl(v3Poller, urlTallier, "http://test.com/foo4", hashblocks[3]);
    assertEquals(null, urlTallier.peekUrl());
  }

  public void testTallyVoterUrl() throws Exception {

    V3Poller v3Poller = makeV3Poller("testing poll key");
    
    PeerIdentity id1 = findPeerIdentity("TCP:[127.0.0.1]:8990");
    PeerIdentity id2 = findPeerIdentity("TCP:[127.0.0.1]:8991");
    PeerIdentity id3 = findPeerIdentity("TCP:[127.0.0.1]:8992");

    VoteBlock [] voter1_voteblocks = {
	makeVoteBlock("http://test.com/foo1", "content for foo1"),
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo3", "content for foo3")
      };
    
    VoteBlock [] voter2_voteblocks = {
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo3", "content for foo3")
      };
    
    VoteBlock [] voter3_voteblocks = {
	makeVoteBlock("http://test.com/foo3", "content for foo3"),
	makeVoteBlock("http://test.com/foo4", "content for foo4")
      };

    BlockTally tally;
    
    List<ParticipantUserData> theParticipants =
      new ArrayList<ParticipantUserData>();

    theParticipants.add(makeParticipant(id1, v3Poller,
					voter1_voteblocks));
    theParticipants.add(makeParticipant(id2, v3Poller,
					voter2_voteblocks));
    theParticipants.add(makeParticipant(id3, v3Poller,
					voter3_voteblocks));

    UrlTallier urlTallier = new UrlTallier(theParticipants);
    assertEquals("http://test.com/foo1", urlTallier.peekUrl());
    tally = tallyVoterUrl(v3Poller, urlTallier, "http://test.com/foo1");
    // todo(bhayes): BlockTally needs to have a better interface, both
    // for testing and for use.
    assertEquals(tally.getVoterOnlyBlockVoters().size(), 1);
    // todo(bhayes): This seems incorrect; foo1 was only present at
    // one voter, but incrementTalliedBlocks will be called for each
    // voter.
    // assertEquals(1, tally.getTalliedVoters().size());
    assertEquals(1, tally.getVoterOnlyBlockVoters().size());
    assertContains(tally.getVoterOnlyBlockVoters(), theParticipants.get(0));
    assertEquals("http://test.com/foo2", urlTallier.peekUrl());

    tally = tallyVoterUrl(v3Poller, urlTallier, "http://test.com/foo2");
    assertEquals(tally.getVoterOnlyBlockVoters().size(), 2);
    // assertEquals(2, tally.getTalliedVoters().size());
    assertContains(tally.getVoterOnlyBlockVoters(), theParticipants.get(0));
    assertContains(tally.getVoterOnlyBlockVoters(), theParticipants.get(1));
    assertEquals("http://test.com/foo3", urlTallier.peekUrl());

    tally = tallyVoterUrl(v3Poller, urlTallier, "http://test.com/foo3");
    assertEquals(tally.getVoterOnlyBlockVoters().size(), 3);
    // assertEquals(3, tally.getTalliedVoters().size());
    assertContains(tally.getVoterOnlyBlockVoters(), theParticipants.get(0));
    assertContains(tally.getVoterOnlyBlockVoters(), theParticipants.get(1));
    assertContains(tally.getVoterOnlyBlockVoters(), theParticipants.get(2));
    assertEquals("http://test.com/foo4", urlTallier.peekUrl());

    tally = tallyVoterUrl(v3Poller, urlTallier, "http://test.com/foo4");
    assertEquals(tally.getVoterOnlyBlockVoters().size(), 1);
    // assertEquals(1, tally.getTalliedVoters().size());
    assertContains(tally.getVoterOnlyBlockVoters(), theParticipants.get(2));
    assertEquals(null, urlTallier.peekUrl());
  }

  public void testTallyVoterUrlNotPeek() throws Exception {

    V3Poller v3Poller = makeV3Poller("testing poll key");
    
    PeerIdentity id1 = findPeerIdentity("TCP:[127.0.0.1]:8990");
    PeerIdentity id2 = findPeerIdentity("TCP:[127.0.0.1]:8991");
    PeerIdentity id3 = findPeerIdentity("TCP:[127.0.0.1]:8992");

    VoteBlock [] voter1_voteblocks = {
	makeVoteBlock("http://test.com/foo1", "content for foo1"),
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
      };

    BlockTally tally;
    
    List<ParticipantUserData> theParticipants =
      new ArrayList<ParticipantUserData>();

    theParticipants.add(makeParticipant(id1, v3Poller,
					voter1_voteblocks));

    UrlTallier urlTallier = new UrlTallier(theParticipants);
    assertEquals("http://test.com/foo1", urlTallier.peekUrl());
    tallyVoterUrl(v3Poller, urlTallier, "http://test.com/foo1");
    assertEquals("http://test.com/foo2", urlTallier.peekUrl());
    try {
      // Call tallyVoterUrl with a url after the peekUrl
      tallyVoterUrl(v3Poller, urlTallier, "http://test.com/goo");
      fail("Expected IllegalArgumentException was not thrown.");
    } catch (IllegalArgumentException e) {
      // expected
    }
    try {
      // Call tallyVoterUrl with a null url
      tallyVoterUrl(v3Poller, urlTallier, null);
      fail("Expected IllegalArgumentException was not thrown.");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  public void testIteratorFileNotFound() throws Exception {
    // A VoteBlocks which supports nothing except iterator(), and that
    // throws FileNotFound.
    final class FileNotFoundVoteBlocks implements VoteBlocks {
      boolean thrown = false;
      public VoteBlocksIterator iterator() throws FileNotFoundException {
	// The test only calls iterator() once.
	assertFalse(thrown);
	thrown = true;
	throw new FileNotFoundException("Expected exception.");
      }
      public void addVoteBlock(VoteBlock b) throws IOException {
	throw new UnsupportedOperationException();
      }
      public InputStream getInputStream() throws IOException {
	throw new UnsupportedOperationException();
      }
      public VoteBlock getVoteBlock(String url) {
	throw new UnsupportedOperationException();
	}
      public int size() {
	throw new UnsupportedOperationException();
      }
      public long getEstimatedEncodedLength() {
	throw new UnsupportedOperationException();
      }
      public void release() {
	throw new UnsupportedOperationException();
      }
    };

    V3Poller v3Poller = makeV3Poller("testing poll key");

    PeerIdentity id1 = findPeerIdentity("TCP:[127.0.0.1]:8990");
    PeerIdentity id2 = findPeerIdentity("TCP:[127.0.0.1]:8991");

    VoteBlock [] voter1_voteblocks = {
	makeVoteBlock("http://test.com/foo1", "content for foo1"),
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
      };

    VoteBlock [] voter2_voteblocks = {
	makeVoteBlock("http://test.com/foo1", "content for foo1"),
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
      };
    
    List<ParticipantUserData> theParticipants =
      new ArrayList<ParticipantUserData>();

    ParticipantUserData participant1 =
      new ParticipantUserData(id1, v3Poller, null);

    FileNotFoundVoteBlocks vb = new FileNotFoundVoteBlocks();
    participant1.setVoteBlocks(vb);
    ParticipantUserData participant2 =
      makeParticipant(id2, v3Poller, voter2_voteblocks);

    theParticipants.add(participant1);
    theParticipants.add(participant2);

    assertFalse(vb.thrown);
    UrlTallier urlTallier = new UrlTallier(theParticipants);
    // The file wasn't found; check to make sure the voter is spoiled.
    assertTrue(vb.thrown);
    for (int urlNum = 1; urlNum <=2; urlNum++) {
      String url = "http://test.com/foo"+urlNum;
      assertEquals(url, urlTallier.peekUrl());
      BlockTally tally = tallyVoterUrl(v3Poller, urlTallier, url);
      assertSameElements(Arrays.asList(participant2),
			 tally.getVoterOnlyBlockVoters());
    }
    assertEquals(null, urlTallier.peekUrl());
  }

  public void testHashStatsTallier() throws Exception {
    V3Poller v3Poller = makeV3Poller("testing poll key");
    PeerIdentity id1 = findPeerIdentity("TCP:[127.0.0.1]:8990");
    ParticipantUserData participant =
      new ParticipantUserData(id1, v3Poller, null);

    VoteBlock vb = new VoteBlock("foo", VoteBlock.CONTENT_VOTE);
    byte[] testBytes = ByteArray.makeRandomBytes(20);
    vb.addVersion(0, 123, 0, 155, testBytes, testBytes, false);

    VoteBlockTallier.VoteCallback callback = V3Poller.makeHashStatsTallier();
    callback.vote(vb, participant);
    assertEquals(286, participant.getBytesHashed());
    assertEquals(155, participant.getBytesRead());
  }

  public void testVoteCounts() throws Exception {
    V3Poller v3Poller = makeV3Poller("testing poll key");
    PeerIdentity id1 = findPeerIdentity("TCP:[127.0.0.1]:8990");

    PatternFloatMap pfm = PatternFloatMap.fromSpec("url1.*,0.5;quarter,0.25");
    testau.setUrlPsllResultMap(pfm);
    PollerStateBean pollerState = v3Poller.getPollerStateBean();
    pollerState.setUrlResultWeightMap(pfm);

    ParticipantUserData participant =
      new ParticipantUserData(id1, v3Poller, null);

    ParticipantUserData.VoteCounts counts = participant.getVoteCounts();
    VoteBlockTallier.VoteBlockTally tally = ParticipantUserData.voteTally;
    tally.voteAgreed( participant, "url1");
    assertEquals("1/0/0/0/0/0", counts.votes());
    assertEquals(1.0, counts.getPercentAgreement(), .001);

    tally.voteDisagreed( participant, "url3");
    tally.voteDisagreed( participant, "url4");
    assertEquals("1/2/0/0/0/0", counts.votes());
    tally.voteAgreed( participant, "url3");
    assertEquals("2/2/0/0/0/0", counts.votes());

    assertEquals(.5, counts.getPercentAgreement(), .001);
    assertEquals(1.5, counts.getWeightedAgreedVotes(), .001);
    assertEquals(2.0, counts.getWeightedDisagreedVotes(), .001);
}

  public void testVoteCountsWithUrlLists() throws Exception {
    MyV3Poller v3Poller = makeV3Poller("testing poll key");
    v3Poller.setRecordPeerUrlLists(true);
    PeerIdentity id1 = findPeerIdentity("TCP:[127.0.0.1]:8990");

    PatternFloatMap pfm = PatternFloatMap.fromSpec("url1.*,0.5;quarter,0.25");
    testau.setUrlPsllResultMap(pfm);
    PollerStateBean pollerState = v3Poller.getPollerStateBean();
    pollerState.setUrlResultWeightMap(pfm);

    ParticipantUserData participant =
      new ParticipantUserData(id1, v3Poller, null);
    ParticipantUserData.VoteCounts counts = participant.getVoteCounts();
    VoteBlockTallier.VoteBlockTally tally = ParticipantUserData.voteTally;
    tally.voteAgreed( participant, "url1");
    assertEquals("1/0/0/0/0/0", counts.votes());
    assertEquals(1.0, counts.getPercentAgreement(), .001);
    tally.voteDisagreed( participant, "url3");
    tally.voteDisagreed( participant, "url4");
    assertEquals("1/2/0/0/0/0", counts.votes());
    tally.voteAgreed( participant, "url3");
    assertEquals("2/1/0/0/0/0", counts.votes());

    assertEquals(0.667, counts.getPercentAgreement(), .001);
    assertEquals(1.5, counts.getWeightedAgreedVotes(), .001);
    assertEquals(1.0, counts.getWeightedDisagreedVotes(), .001);
  }

  public void testRecordSymmetricHashes() throws Exception {

    V3Poller v3Poller = makeInittedV3Poller("testing poll key");
    
    int nh = 6+6+1;

    PeerIdentity id1 = findPeerIdentity("TCP:[127.0.0.1]:8990");
    PeerIdentity id2 = findPeerIdentity("TCP:[127.0.0.1]:8991");
    PeerIdentity id3 = findPeerIdentity("TCP:[127.0.0.1]:8992");
    PeerIdentity id4 = findPeerIdentity("TCP:[127.0.0.1]:8994");
    
    HashBlock [] hashblocks = {
      makeHashBlock("http://test.com/foo1", "content for foo1", nh),
      makeHashBlock("http://test.com/foo2", "content for foo2", nh),
      makeHashBlock("http://test.com/foo3", "content for foo3", nh),
      makeHashBlock("http://test.com/foo4", "content for foo4", nh)
    };

    addVersion(hashblocks[0], "abc", nh);
    addVersion(hashblocks[0], "defg", nh);

    addVersion(hashblocks[2], "1111", nh);
    addVersion(hashblocks[2], "22222", nh);
    addVersion(hashblocks[2], "4444444", nh);
    
    List<ParticipantUserData> parts = symmetricParticipants(v3Poller);
    VoteBlocks b0 = parts.get(0).getSymmetricVoteBlocks();
    assertEquals(0, b0.size());

    v3Poller.recordSymmetricHashes(hashblocks[0]);
    b0 = parts.get(0).getSymmetricVoteBlocks();
    assertEquals(1, b0.size());
    VoteBlock vb = b0.iterator().next();
    assertEquals(3, vb.size());

    v3Poller.recordSymmetricHashes(hashblocks[1]);
    v3Poller.recordSymmetricHashes(hashblocks[2]);
    v3Poller.recordSymmetricHashes(hashblocks[3]);

    b0 = parts.get(0).getSymmetricVoteBlocks();
    assertEquals(4, b0.size());

    List<VoteBlock> vbs = new ArrayList<VoteBlock>();
    for (VoteBlocksIterator iter = b0.iterator(); iter.hasNext();) {
      vbs.add(iter.next());
    }

    assertEquals(3, vbs.get(0).size());
    assertEquals(1, vbs.get(1).size());
    assertEquals(4, vbs.get(2).size());
    assertEquals(1, vbs.get(3).size());
  }

  private MySizedV3Poller makeSizedV3Poller(String key, int pollSize)
      throws Exception {
    PollSpec ps = new MockPollSpec(testau.getAuCachedUrlSet(), null, null,
                                   Poll.V3_POLL);
    return new MySizedV3Poller(ps, theDaemon, pollerId, key, 20000, "SHA-1",
			  pollSize);
  }

  private MyV3Poller makeV3Poller(String key) throws Exception {
    PollSpec ps = new MockPollSpec(testau.getAuCachedUrlSet(), null, null,
                                   Poll.V3_POLL);
    return new MyV3Poller(ps, theDaemon, pollerId, key, 20000, "SHA-1");
  }
  
  private MyV3Poller makeInittedV3Poller(String key) throws Exception {
    return makeInittedV3Poller(key, 6, voters.length);
  }

  private MyV3Poller makeInittedV3Poller(String key,
					   int numSym) throws Exception {
    return makeInittedV3Poller(key, numSym, voters.length);
  }
  private MyV3Poller makeInittedV3Poller(String key, int numSym,
					 int numVoters) throws Exception {
    PollSpec ps = new MockPollSpec(testau.getAuCachedUrlSet(), null, null,
                                   Poll.V3_POLL);
    MyV3Poller p = new MyV3Poller(ps, theDaemon, pollerId, key, 20000,
                                          "SHA-1");
    p.constructInnerCircle(numVoters);
    Map<PeerIdentity,ParticipantUserData> innerCircle = theParticipants(p);
    for (int ix = 0; ix < voters.length; ix++) {
      PeerIdentity pid = voters[ix];
      ParticipantUserData ud = innerCircle.get(pid);
      if (ud != null) {
	ud.setVoterNonce(voterNonces[ix]);
	if (ix < numSym) {
	  ud.setVoterNonce2(voterNonce2s[ix]);
	}
      }
    }
    p.lockParticipants();
    return p;
  }

  private Map<PeerIdentity,ParticipantUserData> 
    theParticipants(V3Poller v3Poller) throws Exception {
    return (Map<PeerIdentity,ParticipantUserData>)PrivilegedAccessor.
      getValue(v3Poller, "theParticipants");
  }

  private List<ParticipantUserData>
    symmetricParticipants(V3Poller v3Poller) throws Exception {
    return (List<ParticipantUserData>)PrivilegedAccessor.
      getValue(v3Poller, "symmetricParticipants");
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

    boolean isRecordPeerUrlLists = DEFAULT_RECORD_PEER_URL_LISTS;

    boolean isRecordPeerUrlLists() {
      return isRecordPeerUrlLists;
    }

    void setRecordPeerUrlLists(boolean val) {
      isRecordPeerUrlLists = val;
    }
  }
  
  private class MySizedV3Poller extends V3Poller {
    private final int pollSize;

    MySizedV3Poller(PollSpec spec, LockssDaemon daemon, PeerIdentity id,
	       String pollkey, long duration, String hashAlg, int pollSize)
        throws PollSerializerException {
      super(spec, daemon, id, pollkey, duration, hashAlg);
      this.pollSize = pollSize;
    }

    @Override
    public int getPollSize() {
      return pollSize;
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

    p.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    p.setProperty(IdentityManager.PARAM_LOCAL_V3_IDENTITY, localPeerKey);
    p.setProperty(ConfigManager.PARAM_NEW_SCHEDULER, "true");
    p.setProperty(IdentityManagerImpl.PARAM_INITIAL_PEERS,
                  StringUtil.separatedString(initialPeers, ";"));
    p.setProperty(V3Poller.PARAM_QUORUM, "3");
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
    MockNodeManager nodeManager = new MockNodeManager();
    theDaemon.setNodeManager(nodeManager, testau);
    MockAuState aus = new MockAuState(testau);
    nodeManager.setAuState(aus);
    pollmanager.startService();
  }

  static class MyIdentityManager extends IdentityManagerImpl {
    Map myMap = null;

    @Override
    public Map getAgreed(ArchivalUnit au) {
      if (myMap == null) {
	myMap = new HashMap();
      }
      return myMap;
    }
    
    @Override
    public List getCachesToRepairFrom(ArchivalUnit au) {
      if (myMap == null) {
	myMap = new HashMap();
      }
      return new ArrayList(myMap.keySet());
    }

    @Override
    public void storeIdentities() throws ProtocolException {
    }
  }

}
