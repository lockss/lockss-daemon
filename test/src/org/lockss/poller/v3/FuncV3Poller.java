/*
 * $Id: FuncV3Poller.java,v 1.17 2008-01-27 06:46:04 tlipkis Exp $
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
import java.net.*;
import java.util.*;

import org.mortbay.util.*;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.config.Configuration.*;
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.poller.v3.V3Serializer.*;
import org.lockss.protocol.*;
import org.lockss.protocol.BlockingStreamComm.*;
import org.lockss.protocol.LcapDatagramRouter.MessageHandler;
import org.lockss.protocol.LcapStreamComm.*;
import org.lockss.protocol.psm.*;
import org.lockss.repository.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.util.Queue;

import sun.security.krb5.internal.*;

/**
 * Functional tests for the V3Poller.
 */
public class FuncV3Poller extends LockssTestCase {

  private IdentityManager idmgr;
  private MockLockssDaemon theDaemon;
  private HashService hashService;

  private PeerIdentity pollerId;

  private String tempDirPath;
  private ArchivalUnit testau;
  private PollManager pollmanager;

  private PeerIdentity[] voters;
  private V3LcapMessage[] pollAcks;
  private V3LcapMessage[] nominates;
  private V3LcapMessage[] votes;
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
    BASE_URL + "file3.html",
    BASE_URL + "file4.html"
  };

  private static List voteBlocks;
  static {
    voteBlocks = new ArrayList();
    for (int ix = 2; ix < urls.length; ix++) {
      VoteBlock vb = V3TestUtils.makeVoteBlock(urls[ix]); 
      voteBlocks.add(vb);
    }
  }

  public void setUp() throws Exception {
    super.setUp();
    tempDir = getTempDir();
    tempDirPath = tempDir.getAbsolutePath();
    System.setProperty("java.io.tmpdir", tempDirPath);
    startDaemon();
    getMockLockssDaemon().getPsmManager().startService();
  }

  public void tearDown() throws Exception {
    stopDaemon();
    super.tearDown();
  }

  private void startDaemon() throws Exception {
    TimeBase.setSimulated();
    this.testau = setupAu();
    theDaemon = getMockLockssDaemon();
    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    p.setProperty(ConfigManager.PARAM_NEW_SCHEDULER, "true");
    p.setProperty(V3Poller.PARAM_MAX_POLL_DURATION, "6m");
    p.setProperty(V3Poller.PARAM_QUORUM, "3");
    p.setProperty(LcapStreamComm.PARAM_ENABLED, "true");
    p.setProperty(LcapDatagramComm.PARAM_ENABLED, "false");
    p.setProperty(IdentityManager.PARAM_LOCAL_V3_IDENTITY,
		  "TCP:[127.0.0.1]:3456");
    p.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    p.setProperty(IdentityManagerImpl.PARAM_INITIAL_PEERS,
                  "TCP:[127.0.0.2]:3456,TCP:[127.0.0.3]:3456,"
                  + "TCP:[127.0.0.4]:3456,TCP:[127.0.0.5]:3456,"
                  + "TCP:[127.0.0.6]:3456,TCP:[127.0.0.7]:3456");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    idmgr = theDaemon.getIdentityManager();
    pollmanager = theDaemon.getPollManager();
    hashService = theDaemon.getHashService();
    theDaemon.setStreamCommManager(new MyMockStreamCommManager(theDaemon));
    theDaemon.setDatagramRouterManager(new MyMockLcapDatagramRouter());
    theDaemon.setRouterManager(new MyMockLcapRouter());
    theDaemon.setNodeManager(new MockNodeManager(), testau);
    theDaemon.setPluginManager(new MyMockPluginManager(theDaemon, testau));
    theDaemon.setDaemonInited(true);
    theDaemon.getSchedService().startService();
    theDaemon.getActivityRegulator(testau).startService();
    idmgr.startService();
    hashService.startService();
    pollmanager.startService();
    this.pollerId = idmgr.stringToPeerIdentity("127.0.0.1");
    this.voters = makeVoters(4);
    this.pollerNonces = makeNonces();
    this.voterNonces = makeNonces();
    this.pollAcks = makePollAckMessages();
    this.nominates = makeNominateMessages();
    this.votes = makeVoteMessages();
  }

  private void stopDaemon() throws Exception {
    theDaemon.getPollManager().stopService();
    theDaemon.getPluginManager().stopService();
    theDaemon.getActivityRegulator(testau).stopService();
    theDaemon.getSystemMetrics().stopService();
    theDaemon.getRouterManager().stopService();
    theDaemon.getDatagramRouterManager().stopService();
    theDaemon.getHashService().stopService();
    theDaemon.getSchedService().stopService();
    theDaemon.getIdentityManager().stopService();
    theDaemon.setDaemonInited(false);
    this.testau = null;
    TimeBase.setReal();
  }

  private MockArchivalUnit setupAu() {
    MockArchivalUnit mau = new MockArchivalUnit();
    mau.setAuId("mock");
    MockPlugin plug = new MockPlugin();
    mau.setPlugin(plug);
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.setEstimatedHashDuration(3000);
    List files = new ArrayList();
    for (int ix = 2; ix < urls.length; ix++) {
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
      msgs[i] = new V3LcapMessage("auid", "key", "3", pollerNonces[i],
                                  voterNonces[i], V3LcapMessage.MSG_POLL_ACK,
                                  123456789, voters[i], tempDir, theDaemon);
    }
    return msgs;
  }

  private V3LcapMessage[] makeNominateMessages() {
    V3LcapMessage[] msgs = new V3LcapMessage[voters.length];
    for (int i = 0; i < voters.length; i++) {
      V3LcapMessage msg = new V3LcapMessage("auid", "key", "3",
                                            pollerNonces[i], voterNonces[i],
                                            V3LcapMessage.MSG_NOMINATE,
                                            123456789, voters[i], tempDir,
                                            theDaemon);
      msg.setNominees(ListUtil.list("10.0." + i + ".1",
				    "10.0." + i + ".2",
				    "10.0." + i + ".3",
				    "10.0." + i + ".4"));
      msgs[i] = msg;
    }
    return msgs;
  }

  private V3LcapMessage[] makeVoteMessages() throws IOException {
    V3LcapMessage[] msgs = new V3LcapMessage[voters.length];
    for (int i = 0; i < voters.length; i++) {
      V3LcapMessage msg = new V3LcapMessage("auid", "key", "3",
                                            pollerNonces[i], voterNonces[i],
                                            V3LcapMessage.MSG_VOTE, 123456789,
                                            voters[i], tempDir, theDaemon);
      for (Iterator it = voteBlocks.iterator(); it.hasNext(); ) {
	msg.addVoteBlock((VoteBlock)it.next());
      }
      msgs[i] = msg;
    }
    return msgs;
  }
  
  
  public void testStub() {
    // This file is in need of a total overhaul.  Until then, this
    // stub method is here to let builds proceed normally.
  }

//  public void testRestorePoll() throws Exception {
//    PollSpec pollspec = new PollSpec(testau.getAuCachedUrlSet(),
//                                     Poll.V3_POLL);
//    Poll p1 = pollmanager.callPoll(pollspec);
//    assertNotNull(p1);
//    assertEquals(1, pollmanager.getV3Pollers().size());
//    stopDaemon();
//    assertEquals(0, pollmanager.getV3Pollers().size());
//    startDaemon();
//    assertEquals(1, pollmanager.getV3Pollers().size());
//    Poll p2 = pollmanager.getPoll(p1.getKey());
//    assertNotNull(p2);
//    assertEquals(p2.getKey(), p1.getKey());
//    V3Poller p1V3 = (V3Poller)p1;
//    V3Poller p2V3 = (V3Poller)p2;
//    assertEquals(p1V3.getCallerID(), p2V3.getCallerID());
//    assertEquals(p1V3.getDeadline(), p2V3.getDeadline());
//    assertEquals(p1V3.getDuration(), p2V3.getDuration());
//    assertEquals(p1V3.getStatusString(), p2V3.getStatusString());
//    assertEquals(p1V3.theParticipants.size(), p2V3.theParticipants.size());
//    V3TestUtil.assertEqualPollerStateBeans(p1V3.getPollerStateBean(),
//                                           p2V3.getPollerStateBean());
//    for (Iterator it = p1V3.theParticipants.keySet().iterator(); it.hasNext(); ) {
//      PeerIdentity voter = (PeerIdentity)it.next();
//      ParticipantUserData ud1 =
//        (ParticipantUserData)p1V3.theParticipants.get(voter);
//      ParticipantUserData ud2 =
//        (ParticipantUserData)p2V3.theParticipants.get(voter);
//      V3TestUtil.assertEqualParticipantUserData(ud1, ud2);
//    }
//    pollmanager.cancelAuPolls(testau);
//  }

  public class MyV3Poller extends V3Poller {
    // For testing:  Hashmap of voter IDs to V3LcapMessages.
    private Map sentMsgs = Collections.synchronizedMap(new HashMap());
    private Map semaphores = new HashMap();
    private PeerIdentity[] mockVoters;

    MyV3Poller(PollSpec spec, LockssDaemon daemon, PeerIdentity id,
	       String pollkey, long duration, String hashAlg,
               PeerIdentity[] mockVoters)
        throws PollSerializerException {
      super(spec, daemon, id, pollkey, duration, hashAlg);
      this.mockVoters = mockVoters;
    }

    Collection getReferenceList() {
      return ListUtil.fromArray(mockVoters);
    }

    public void nominatePeers(PeerIdentity voter, List nominees) {
      // do nothing.
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

    protected boolean scheduleHash() {
      return true;
    }
    
    /**
     * Overridden to just agree.
     */
    protected void compareBlocks(PeerIdentity voter,
                                 byte[] theirHash, byte[] ourHash,
                                 BlockTally tally) {
      for (Iterator iter = theParticipants.values().iterator(); iter.hasNext();) {
        tally.addAgreeVoter(voter);
      }
      tally.tallyVotes();
    }
  }

  class MyMockLcapRouter extends LcapRouter {
    public void registerMessageHandler(org.lockss.protocol.LcapRouter.MessageHandler handler) {
    }

    public void send(V1LcapMessage msg, ArchivalUnit au) throws IOException {
    }

    public void sendTo(V1LcapMessage msg, ArchivalUnit au, PeerIdentity id)
        throws IOException {
    }

    public void sendTo(V3LcapMessage msg, PeerIdentity id) throws IOException {
    }

    public void setConfig(Configuration config, Configuration oldConfig,
                          Differences changedKeys) {
    }

    public void startService() {
    }

    public void stopService() {
    }

    public void unregisterMessageHandler(org.lockss.protocol.LcapRouter.MessageHandler handler) {
    }
  }

  class MyMockLcapDatagramRouter extends LcapDatagramRouter {
    public void registerMessageHandler(MessageHandler handler) {
    }
    public void send(V1LcapMessage msg, ArchivalUnit au)
        throws IOException {
    }
    public void sendTo(V1LcapMessage msg, ArchivalUnit au, PeerIdentity id)
        throws IOException {
    }
    public void setConfig(Configuration config, Configuration oldConfig,
                          Differences changedKeys) {
    }
    public void startService() {
    }
    public void stopService() {
    }
    public void unregisterMessageHandler(MessageHandler handler) {
    }
  }

  class MyMockStreamCommManager extends BlockingStreamComm {
    private LockssDaemon theDaemon;

    public MyMockStreamCommManager(LockssDaemon daemon) {
      this.theDaemon = daemon;
    }
    public void sendTo(PeerMessage msg, PeerIdentity id,
                       RateLimiter limiter) throws IOException {
      log.debug("sendTo: id=" + id);
    }
    public void setConfig(Configuration config, Configuration prevConfig,
                          Differences changedKeys) {
    }
    public PeerMessage newPeerMessage() {
      throw new UnsupportedOperationException("Not implemented");
    }
    public PeerMessage newPeerMessage(int estSize) {
      throw new UnsupportedOperationException("Not implemented");
    }
    public void registerMessageHandler(int protocol, MessageHandler handler) {
      log.debug("MockStreamCommManager: registerMessageHandler");
    }
    public void unregisterMessageHandler(int protocol) {
      log.debug("MockStreamCommManager: unregisterMessageHandler");
    }
    public void startService() {
      log.debug("MockStreamCommManager: startService()");
    }
    public void stopService() {
      log.debug("MockStreamCommManager: stopService()");
    }
    public LockssDaemon getDaemon() {
      return theDaemon;
    }
    public void initService(LockssApp app) throws LockssAppException {
      log.debug("MockStreamCommManager: initService(app)");
    }
    public void initService(LockssDaemon daemon) throws LockssAppException {
      log.debug("MockStreamCommManager: initService(daemon)");
    }
    public LockssApp getApp() {
      log.debug("MockStreamCommManager: getApp()");
      return null;
    }
    protected boolean isAppInited() {
      return true;
    }
    protected void resetConfig() {
      log.debug("MockStreamCommManager: resetConfig()");
    }

  }

  class MyMockPluginManager extends PluginManager {
    ArchivalUnit au;
    LockssDaemon daemon;

    public MyMockPluginManager(LockssDaemon daemon, ArchivalUnit au) {
      this.daemon = daemon;
      this.au = au;
    }

    public LockssDaemon getDaemon() {
      return daemon;
    }

    public CachedUrlSet findCachedUrlSet(String auId) {
      return au.getAuCachedUrlSet();
    }
  }
}
