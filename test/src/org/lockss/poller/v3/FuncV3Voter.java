/*
 * $Id: FuncV3Voter.java,v 1.11 2006-09-25 02:16:47 smorabito Exp $
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

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.config.Configuration.*;
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.poller.v3.V3Serializer.*;
import org.lockss.protocol.*;
import org.lockss.repository.*;
import org.lockss.test.*;
import org.lockss.util.*;

public class FuncV3Voter extends LockssTestCase {

  private IdentityManager idmgr;
  private MockLockssDaemon theDaemon;

  private PeerIdentity pollerId;
  private PeerIdentity voterId;

  private String tempDirPath;
  private ArchivalUnit testau;
  private PollManager pollmanager;
  private HashService hashService;

  private V3LcapMessage msgPoll;
  private V3LcapMessage msgPollProof;
  private V3LcapMessage msgVoteRequest;
  private V3LcapMessage msgRepairRequest;
  private V3LcapMessage msgReceipt;
  private File tempDir;
  
  private long msgDeadline = 0L;

  private static final String BASE_URL = "http://www.test.org/";

  private static String[] urls = {
    "lockssau:",
    BASE_URL,
    BASE_URL + "index.html",
    BASE_URL + "file1.html",
    BASE_URL + "file2.html",
    BASE_URL + "file3.html",
    BASE_URL + "file4.html",
  };

  public void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated();
    msgDeadline = TimeBase.nowMs() + 100000;
    tempDir = getTempDir();
    tempDirPath = tempDir.getAbsolutePath() + File.separator;
    startDaemon();
  }

  public void tearDown() throws Exception {
    stopDaemon();
    TimeBase.setReal();
    super.tearDown();
  }

  private void startDaemon() throws Exception {
    this.testau = setupAu();
    theDaemon = getMockLockssDaemon();
    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    p.setProperty(ConfigManager.PARAM_NEW_SCHEDULER, "true");
    p.setProperty(V3Poller.PARAM_MIN_POLL_SIZE, "4");
    p.setProperty(V3Poller.PARAM_MAX_POLL_SIZE, "4");
    p.setProperty(V3PollFactory.PARAM_POLL_DURATION_MIN, "5m");
    p.setProperty(V3PollFactory.PARAM_POLL_DURATION_MAX, "6m");
    p.setProperty(V3Poller.PARAM_QUORUM, "3");
    p.setProperty(LcapStreamComm.PARAM_ENABLED, "true");
    p.setProperty(LcapDatagramComm.PARAM_ENABLED, "false");
    p.setProperty(V3Poller.PARAM_V3_MESSAGE_DIR, tempDirPath);
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
    this.msgPoll = makePollMsg();
    this.msgPollProof = makePollProofMsg();
    this.msgVoteRequest = makeVoteReqMsg();
    this.msgRepairRequest = makeRepairReqMsg();
    this.msgReceipt = makeReceiptMsg();
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
    cus.setEstimatedHashDuration(1000);
    List files = new ArrayList();
    for (int ix = 2; ix < urls.length; ix++) {
      MockCachedUrl cu = (MockCachedUrl)mau.addUrl(urls[ix], false, true);
      // Add mock file content.
      cu.setContent("This is content for CUSasdfasdfasdfadsfasdsassdfafile " + ix);
      files.add(cu);
    }
    cus.setHashItSource(files);
    cus.setFlatItSource(files);
    return mau;
  }

  public V3LcapMessage makePollMsg() {
    V3LcapMessage msg =
      new V3LcapMessage("auid", "key", "3", ByteArray.makeRandomBytes(20),
                        ByteArray.makeRandomBytes(20),
                        V3LcapMessage.MSG_POLL,
                        msgDeadline, pollerId, tempDir);
    msg.setEffortProof(ByteArray.makeRandomBytes(20));
    return msg;
  }

  public V3LcapMessage makePollProofMsg() {
    V3LcapMessage msg =
      new V3LcapMessage("auid", "key", "3", ByteArray.makeRandomBytes(20),
                        ByteArray.makeRandomBytes(20),
                        V3LcapMessage.MSG_POLL_PROOF,
                        msgDeadline, pollerId, tempDir);
    msg.setEffortProof(ByteArray.makeRandomBytes(20));
    return msg;
  }

  public V3LcapMessage makeVoteReqMsg() {
    V3LcapMessage msg =
      new V3LcapMessage("auid", "key", "3", ByteArray.makeRandomBytes(20),
                        ByteArray.makeRandomBytes(20),
                        V3LcapMessage.MSG_VOTE_REQ,
                        msgDeadline, pollerId, tempDir);
    return msg;
  }

  public V3LcapMessage makeRepairReqMsg() {
    V3LcapMessage msg =
      new V3LcapMessage("auid", "key", "3", ByteArray.makeRandomBytes(20),
                        ByteArray.makeRandomBytes(20),
                        V3LcapMessage.MSG_REPAIR_REQ,
                        msgDeadline, pollerId, tempDir);
    return msg;
  }

  public V3LcapMessage makeReceiptMsg() {
    V3LcapMessage msg =
      new V3LcapMessage("auid", "key", "3", ByteArray.makeRandomBytes(20),
                        ByteArray.makeRandomBytes(20),
                        V3LcapMessage.MSG_EVALUATION_RECEIPT,
                        msgDeadline, pollerId, tempDir);
    return msg;
  }

  public void testNonRepairPoll() throws Exception {
    PollSpec ps = new PollSpec(testau.getAuCachedUrlSet(), null, null,
                               Poll.V1_CONTENT_POLL);
    byte[] introEffortProof = ByteArray.makeRandomBytes(20);
    MyMockV3Voter voter =
      new MyMockV3Voter(ps, theDaemon, pollerId,
                        "this_is_my_pollkey",
                        introEffortProof,
                        ByteArray.makeRandomBytes(20),
                        msgDeadline, "SHA-1");

    voter.startPoll();

    voter.receiveMessage(msgPoll);

    V3LcapMessage pollAck = voter.getSentMessage();
    assertNotNull(pollAck);
    assertEquals(pollAck.getOpcode(), V3LcapMessage.MSG_POLL_ACK);

    voter.receiveMessage(msgPollProof);

    V3LcapMessage nominate = voter.getSentMessage();
    assertNotNull(nominate);
    assertEquals(nominate.getOpcode(), V3LcapMessage.MSG_NOMINATE);

    voter.receiveMessage(msgVoteRequest);

    V3LcapMessage vote = voter.getSentMessage();
    assertNotNull(vote);
    assertEquals(vote.getOpcode(), V3LcapMessage.MSG_VOTE);

    /*
    voter.handleMessage(msgRepairRequest);
    voter.handleMessage(msgRepairRequest);
    voter.handleMessage(msgRepairRequest);
    */

    voter.receiveMessage(msgReceipt);
  }

  private class MyMockV3Voter extends V3Voter {
    private FifoQueue sentMessages = new FifoQueue();

    public MyMockV3Voter(PollSpec spec, LockssDaemon daemon, PeerIdentity orig,
                         String key, byte[] introEffortProof,
                         byte[] pollerNonce, long duration, String hashAlg)
        throws PollSerializerException {
      super(spec, daemon, orig, key, introEffortProof, pollerNonce,
            duration, hashAlg);
    }

    public void sendMessageTo(V3LcapMessage msg, PeerIdentity id) {
      this.sentMessages.put(msg);
    }

    public V3LcapMessage getSentMessage() throws InterruptedException {
      log.debug2("Waiting for next message...");
      V3LcapMessage msg = (V3LcapMessage)sentMessages.get(Deadline.in(200));
      log.debug2("Got message: " + msg);
      return msg;
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

    public CachedUrlSet findCachedUrlSet(PollSpec spec) {
      return au.getAuCachedUrlSet();
    }

    public CachedUrlSet findCachedUrlSet(String auId) {
      return au.getAuCachedUrlSet();
    }
  }
}
