/*
 * $Id: FuncV3Voter.java,v 1.2 2005-10-04 22:57:36 tlipkis Exp $
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
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.poller.v3.V3Serializer.*;
import org.lockss.protocol.*;
import org.lockss.repository.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.util.Queue;

public class FuncV3Voter extends LockssTestCase {

  private IdentityManager idmgr;
  private MockLockssDaemon theDaemon;

  private PeerIdentity pollerId;
  private PeerIdentity voterId;

  private String tempDirPath;
  private ArchivalUnit testau;
  private PollManager pollmanager;
  private HashService hashService;

  private V3LcapMessage msgPollProof;
  private V3LcapMessage msgVoteRequest;
  private V3LcapMessage msgRepairRequest;
  private V3LcapMessage msgReceipt;
  
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
    this.testau = setupAu();
    initRequiredServices();

    this.pollerId = idmgr.stringToPeerIdentity("127.0.0.1");
    this.voterId = idmgr.stringToPeerIdentity("10.0.0.1");

    this.msgPollProof = makePollProofMsg();
    this.msgVoteRequest = makeVoteReqMsg();
    this.msgRepairRequest = makeRepairReqMsg();
    this.msgReceipt = makeReceiptMsg();
  }
  
  private MockArchivalUnit setupAu() {
    MockArchivalUnit mau = new MockArchivalUnit();
    mau.setAuId("mock");
    MockPlugin plug = new MockPlugin();
    mau.setPlugin(plug);
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
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

  public V3LcapMessage makePollProofMsg() {
    V3LcapMessage msg =
      new V3LcapMessage(V3LcapMessage.MSG_POLL_PROOF, pollerId,
			"http://www.test.org",
			123456789, 987654321,
			ByteArray.makeRandomBytes(20),
                        ByteArray.makeRandomBytes(20));
    msg.setEffortProof(ByteArray.makeRandomBytes(20));
    return msg;
  }

  public V3LcapMessage makeVoteReqMsg() {
    V3LcapMessage msg =
      new V3LcapMessage(V3LcapMessage.MSG_VOTE_REQ, pollerId,
			"http://www.test.org",
			123456789, 987654321,
			ByteArray.makeRandomBytes(20),
                        ByteArray.makeRandomBytes(20));
    return msg;
  }

  public V3LcapMessage makeRepairReqMsg() {
    V3LcapMessage msg =
      new V3LcapMessage(V3LcapMessage.MSG_REPAIR_REQ, pollerId,
			"http://www.test.org",
			123456789, 987654321,
			ByteArray.makeRandomBytes(20),
                        ByteArray.makeRandomBytes(20));
    return msg;
  }

  public V3LcapMessage makeReceiptMsg() {
    V3LcapMessage msg =
      new V3LcapMessage(V3LcapMessage.MSG_EVALUATION_RECEIPT, pollerId,
			"http://www.test.org",
			123456789, 987654321,
			ByteArray.makeRandomBytes(20),
                        ByteArray.makeRandomBytes(20));
    return msg;
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

  public void testNonRepairPoll() throws Exception {
    PollSpec ps = new PollSpec(testau.getAuCachedUrlSet(), null, null,
                               Poll.CONTENT_POLL);
    byte[] introEffortProof = ByteArray.makeRandomBytes(20);
    MyMockV3Voter voter =
      new MyMockV3Voter(ps, theDaemon, pollerId, 
                        "this_is_my_pollkey",
                        introEffortProof,
                        ByteArray.makeRandomBytes(20),
                        100000, "SHA-1");

    voter.startPoll();
    
    V3LcapMessage pollAck = voter.getSentMessage();
    assertNotNull(pollAck);
    assertEquals(pollAck.getOpcode(), V3LcapMessage.MSG_POLL_ACK);
    
    voter.handleMessage(msgPollProof);
    
    V3LcapMessage nominate = voter.getSentMessage();
    assertNotNull(nominate);
    assertEquals(nominate.getOpcode(), V3LcapMessage.MSG_NOMINATE);
    
    voter.handleMessage(msgVoteRequest);
    
    V3LcapMessage vote = voter.getSentMessage();
    assertNotNull(vote);
    assertEquals(vote.getOpcode(), V3LcapMessage.MSG_VOTE);

    /*
    voter.handleMessage(msgRepairRequest);
    voter.handleMessage(msgRepairRequest);
    voter.handleMessage(msgRepairRequest);
    */

    voter.handleMessage(msgReceipt);
  }
  
  private class MyMockV3Voter extends V3Voter {
    private Queue sentMessages = new FifoQueue();
    
    public MyMockV3Voter(PollSpec spec, LockssDaemon daemon, PeerIdentity orig,
                         String key, byte[] introEffortProof,
                         byte[] pollerNonce, long duration, String hashAlg)
        throws PollSerializerException {
      super(spec, daemon, orig, key, introEffortProof, pollerNonce,
            duration, hashAlg);
    }

    public void sendMessage(V3LcapMessage msg) {
      this.sentMessages.put(msg);
    }
    
    public V3LcapMessage getSentMessage() throws InterruptedException {
      log.debug2("Waiting for next message...");
      V3LcapMessage msg = (V3LcapMessage)sentMessages.get(Deadline.in(200));
      log.debug2("Got message: " + msg);
      return msg;
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
    p.setProperty(BlockTally.PARAM_QUORUM, "3");
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
