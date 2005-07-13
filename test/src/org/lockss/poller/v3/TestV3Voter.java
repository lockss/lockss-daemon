/*
 * $Id: TestV3Voter.java,v 1.1 2005-07-13 07:53:05 smorabito Exp $
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
import org.lockss.config.ConfigManager;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;
import org.lockss.poller.*;
import org.lockss.test.*;
import org.lockss.repository.LockssRepositoryImpl;
import org.mortbay.util.B64Code;

public class TestV3Voter extends LockssTestCase {

  private IdentityManager idmgr;
  private MockLockssDaemon theDaemon;

  private PeerIdentity pollerId;
  private PeerIdentity voterId;

  private String tempDirPath;
  private ArchivalUnit testau;
  private PollManager pollmanager;

  private V3LcapMessage msgPollProof;
  private V3LcapMessage msgVoteRequest;
  private V3LcapMessage msgRepairRequest;
  private V3LcapMessage msgReceipt;

  private static String[] rootV3urls = {
    "http://www.test.org",
  };

  public void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated();

    initRequiredServices();

    this.pollerId = idmgr.stringToPeerIdentity("127.0.0.1");
    this.voterId = idmgr.stringToPeerIdentity("10.0.0.1");

    this.msgPollProof = makePollProofMsg();
    this.msgVoteRequest = makeVoteReqMsg();
    this.msgRepairRequest = makeRepairReqMsg();
    this.msgReceipt = makeReceiptMsg();
  }

  public V3LcapMessage makePollProofMsg() {
    V3LcapMessage msg =
      new V3LcapMessage(V3LcapMessage.MSG_POLL_PROOF, pollerId,
			"http://www.test.org",
			123456789, 987654321,
			ByteArray.makeRandomBytes(20));
    msg.setEffortProof(ByteArray.makeRandomBytes(20));
    return msg;
  }

  public V3LcapMessage makeVoteReqMsg() {
    V3LcapMessage msg =
      new V3LcapMessage(V3LcapMessage.MSG_VOTE_REQ, pollerId,
			"http://www.test.org",
			123456789, 987654321,
			ByteArray.makeRandomBytes(20));
    return msg;
  }

  public V3LcapMessage makeRepairReqMsg() {
    V3LcapMessage msg =
      new V3LcapMessage(V3LcapMessage.MSG_REPAIR_REQ, pollerId,
			"http://www.test.org",
			123456789, 987654321,
			ByteArray.makeRandomBytes(20));
    return msg;
  }

  public V3LcapMessage makeReceiptMsg() {
    V3LcapMessage msg =
      new V3LcapMessage(V3LcapMessage.MSG_EVALUATION_RECEIPT, pollerId,
			"http://www.test.org",
			123456789, 987654321,
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

  public void testPoll() throws Exception {
    PollSpec ps =
      new MockPollSpec(testau, "http://www.test.org", null, null, Poll.CONTENT_POLL);
    byte[] challenge = ByteArray.makeRandomBytes(20);
    V3Voter v3Voter =
      new V3Voter(ps, pollerId,
		  String.valueOf(B64Code.encode(challenge)), "SHA-1");
    v3Voter.startPoll();

    // We are playing the part of the poller, scripting expected
    // messages.

    // Send a PollProof message
    v3Voter.handleMessage(msgPollProof);

    // Send a VoteRequset message.
    v3Voter.handleMessage(msgVoteRequest);

    // Send a few RepairRequest message
    v3Voter.handleMessage(msgRepairRequest);
    v3Voter.handleMessage(msgRepairRequest);
    v3Voter.handleMessage(msgRepairRequest);

    // Send a Receipt message
    v3Voter.handleMessage(msgReceipt);

    v3Voter.stopPoll();
  }

  private void initRequiredServices() {
    theDaemon = getMockLockssDaemon();
    pollmanager = theDaemon.getPollManager();

    theDaemon.getPluginManager();
    testau = PollTestPlugin.PTArchivalUnit.createFromListOfRootUrls(rootV3urls);
    PluginTestUtil.registerArchivalUnit(testau);

    String tempDirPath = null;
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
    p.setProperty(ConfigManager.PARAM_NEW_SCHEDULER, "false");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    idmgr = theDaemon.getIdentityManager();
    idmgr.startService();
    //theDaemon.getSchedService().startService();
    theDaemon.getHashService().startService();
    theDaemon.getDatagramRouterManager().startService();
    theDaemon.getRouterManager().startService();
    theDaemon.getSystemMetrics().startService();
    theDaemon.getActivityRegulator(testau).startService();
    theDaemon.setNodeManager(new MockNodeManager(), testau);
    pollmanager.startService();
  }
}
