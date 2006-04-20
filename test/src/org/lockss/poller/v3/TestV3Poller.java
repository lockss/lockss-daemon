/*
 * $Id: TestV3Poller.java,v 1.9.2.1 2006-04-20 07:13:48 smorabito Exp $
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
  
  private File tempDir;

  private static final String BASE_URL = "http://www.test.org/";

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
      VoteBlock vb = new VoteBlock(urls[ix], 1024, 0, 1024, 0,
                                   ByteArray.makeRandomBytes(20),
                                   ByteArray.makeRandomBytes(20),
                                   VoteBlock.CONTENT_VOTE);
      voteBlocks.add(vb);
    }
  }

  public void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated();
    this.tempDir = getTempDir();
    this.testau = setupAu();
    initRequiredServices();
    this.pollerId = idmgr.stringToPeerIdentity("127.0.0.1");
    this.voters = makeVoters(4);
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
    MockPlugin plug = new MockPlugin();
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

  private PeerIdentity[] makeVoters(int count) throws Exception {
    PeerIdentity[] ids = new PeerIdentity[count];
    for (int i = 0; i < count; i++) {
      ids[i] = idmgr.stringToPeerIdentity("10.1.0." + (i+1));
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
                          tempDir);
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
                                            tempDir);
      msg.setNominees(ListUtil.list("10.0." + i + ".1",
                                    "10.0." + i + ".2",
                                    "10.0." + i + ".3",
                                    "10.0." + i + ".4"));
      msgs[i] = msg;
    }
    return msgs;
  }

  private V3LcapMessage[] makeVoteMessages() {
    V3LcapMessage[] msgs = new V3LcapMessage[voters.length];
    for (int i = 0; i < voters.length; i++) {
      V3LcapMessage msg = new V3LcapMessage("auid", "key", "1",
                                            ByteArray.makeRandomBytes(20),
                                            ByteArray.makeRandomBytes(20),
                                            V3LcapMessage.MSG_VOTE,
                                            987654321,
                                            voters[i],
                                            tempDir);
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
                                            tempDir);
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
    TimeBase.setReal();
    super.tearDown();
  }

  /* Test for a specific bug fix. */
  public void testNullNomineesShouldntThrow() throws Exception {
    V3Poller v3Poller = makeInittedV3Poller("foo");
    try {
      v3Poller.nominatePeers(voters[2], null);
    } catch (NullPointerException ex) {
      fail("Should not have caused NullPointerException");
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

  private MyMockV3Poller makeInittedV3Poller(String key) throws Exception {
    PollSpec ps = new MockPollSpec(testau.getAuCachedUrlSet(), null, null,
                                   Poll.V3_POLL);
    MyMockV3Poller p = new MyMockV3Poller(ps, theDaemon, pollerId, key, 20000,
                                          "SHA-1", voters);
    p.constructInnerCircle(voters.length);
    Map innerCircle = (Map)PrivilegedAccessor.getValue(p, "theParticipants");
    for (int ix = 0; ix < voters.length; ix++) {
      PeerIdentity pid = voters[ix];
      ParticipantUserData ud = (ParticipantUserData) innerCircle.get(pid);
      ud.setVoterNonce(voterNonces[ix]);
    }
    return p;
  }

  private class MyMockV3Poller extends V3Poller {
    // For testing:  Hashmap of voter IDs to V3LcapMessages.
    private Map sentMsgs = Collections.synchronizedMap(new HashMap());
    private Map semaphores = new HashMap();
    private PeerIdentity[] mockVoters;

    MyMockV3Poller(PollSpec spec, LockssDaemon daemon, PeerIdentity id,
               String pollkey, long duration, String hashAlg,
               PeerIdentity[] voters)
        throws PollSerializerException {
      super(spec, daemon, id, pollkey, duration, hashAlg);
      this.mockVoters = voters;
    }

    Collection getReferenceList() {
      return ListUtil.fromArray(voters);
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

  private void initRequiredServices() {
    theDaemon = getMockLockssDaemon();
    pollmanager = theDaemon.getPollManager();
    hashService = theDaemon.getHashService();

    theDaemon.getPluginManager();

    tempDirPath = null;
    try {
      tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    }
    catch (IOException ex) {
      fail("unable to create a temporary directory");
    }

    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    p.setProperty(ConfigManager.PARAM_NEW_SCHEDULER, "true");
    p.setProperty(V3Poller.PARAM_MIN_POLL_SIZE, "4");
    p.setProperty(V3Poller.PARAM_MAX_POLL_SIZE, "4");
    p.setProperty(V3Poller.PARAM_QUORUM, "3");
    p.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
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
