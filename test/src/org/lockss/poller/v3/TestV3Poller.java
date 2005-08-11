/*
 * $Id: TestV3Poller.java,v 1.2 2005-08-11 06:33:18 tlipkis Exp $
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
import org.lockss.test.*;
import org.lockss.hasher.HashService;
import org.lockss.repository.LockssRepositoryImpl;
import org.mortbay.util.B64Code;

public class TestV3Poller extends LockssTestCase {

  private IdentityManager idmgr;
  private MockLockssDaemon theDaemon;

  private PeerIdentity pollerId;

  private String tempDirPath;
  private ArchivalUnit testau;
  private PollManager pollmanager;

  private PeerIdentity[] voters;
  private V3LcapMessage[] pollAcks;
  private V3LcapMessage[] nominates;
  private V3LcapMessage[] votes;
  private V3LcapMessage[] repairs;

  private static String[] rootV3urls = {
    "http://www.test.org",
    "http://www.test1.org",
    "http://www.test2.org"
  };

  private static List voteBlocks;
  static {
    voteBlocks = ListUtil.list(new VoteBlock("file1", 1024, 0, 1024, 0,
					     ByteArray.makeRandomBytes(20),
					     ByteArray.makeRandomBytes(20),
					     ByteArray.makeRandomBytes(20)),
			       new VoteBlock("file2", 1024, 0, 1024, 0,
					     ByteArray.makeRandomBytes(20),
					     ByteArray.makeRandomBytes(20),
					     ByteArray.makeRandomBytes(20)),
			       new VoteBlock("file3", 1024, 0, 1024, 0,
					     ByteArray.makeRandomBytes(20),
					     ByteArray.makeRandomBytes(20),
					     ByteArray.makeRandomBytes(20)),
			       new VoteBlock("file4", 1024, 0, 1024, 0,
					     ByteArray.makeRandomBytes(20),
					     ByteArray.makeRandomBytes(20),
					     ByteArray.makeRandomBytes(20)));
  }

  public void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated();

    initRequiredServices();

    this.pollerId = idmgr.stringToPeerIdentity("127.0.0.1");

    this.voters    = makeVoters(4);

    this.pollAcks  = makePollAckMessages();
    this.nominates = makeNominateMessages();
    this.votes     = makeVoteMessages();
    this.repairs   = makeRepairMessages();
  }

  private PeerIdentity[] makeVoters(int count) throws Exception {
    PeerIdentity[] ids = new PeerIdentity[count];
    for (int i = 0; i < count; i++) {
      ids[i] = idmgr.stringToPeerIdentity("10.1.0." + (i+1));
    }
    return ids;
  }

  private V3LcapMessage[] makePollAckMessages() {
    V3LcapMessage[] msgs = new V3LcapMessage[voters.length];
    for (int i = 0; i < voters.length; i++) {
      msgs[i] = new V3LcapMessage(V3LcapMessage.MSG_POLL_ACK, voters[i],
				  "http://www.test.org",
				  123456789, 987654321,
				  ByteArray.makeRandomBytes(20));
    }
    return msgs;
  }

  private V3LcapMessage[] makeNominateMessages() {
    V3LcapMessage[] msgs = new V3LcapMessage[voters.length];
    for (int i = 0; i < voters.length; i++) {
      V3LcapMessage msg = new V3LcapMessage(V3LcapMessage.MSG_NOMINATE, voters[i],
					    "http://www.test.org",
					    123456789, 987654321,
					    ByteArray.makeRandomBytes(20));
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
      V3LcapMessage msg = new V3LcapMessage(V3LcapMessage.MSG_VOTE, voters[i],
					    "http://www.test.org",
					    123456789, 987654321,
					    ByteArray.makeRandomBytes(20));
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
      V3LcapMessage msg = new V3LcapMessage(V3LcapMessage.MSG_REPAIR_REP, voters[i],
					    "http://www.test.org",
					    123456789, 987654321,
					    ByteArray.makeRandomBytes(20));
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

  public void testNonRepairPoll() throws Exception {
    PollSpec ps =
      new MockPollSpec(testau, "http://www.test.org", null, null, Poll.CONTENT_POLL);
    byte[] challenge = ByteArray.makeRandomBytes(20);

    final MyV3Poller v3Poller =
      new MyV3Poller(ps, theDaemon, pollerId,
		     challenge,
		     String.valueOf(B64Code.encode(challenge)),
		     20000, "SHA-1");
    v3Poller.startPoll();

    for (int i = 0; i < voters.length; i++ ) {
      // Expect to receive a Poll message
      V3LcapMessage poll = v3Poller.getSentMessage(voters[i]);
      assertNotNull(poll);
      assertEquals(poll.getOpcode(), V3LcapMessage.MSG_POLL);
      
      // Send a PollAck message.
      v3Poller.handleMessage(pollAcks[i]);
      
      // Expect to receive a PollProof message
      V3LcapMessage pollProof = v3Poller.getSentMessage(voters[i]);
      assertNotNull(pollProof);
      assertEquals(pollProof.getOpcode(), V3LcapMessage.MSG_POLL_PROOF);
      
      // Send a Nominate message
      v3Poller.handleMessage(nominates[i]);
      
      // Expect to receive a VoteRequest message
      V3LcapMessage voteRequest = v3Poller.getSentMessage(voters[i]);
      assertNotNull(voteRequest);
      assertEquals(voteRequest.getOpcode(), V3LcapMessage.MSG_VOTE_REQ);
      
      // Send a Vote message.
      v3Poller.handleMessage(votes[i]);
    }

    // For this test, the tally is in the poller's favor for each
    // block.  Nothing needs repairs.

    for (int i = 0; i < voters.length; i++) {
      // Expect to receive an Evaluation Receipt
      V3LcapMessage rcpt = v3Poller.getSentMessage(voters[i]);
      assertNotNull(rcpt);
      assertEquals(rcpt.getOpcode(), V3LcapMessage.MSG_EVALUATION_RECEIPT);
    }

    // All state machines should be in their final states now.
    assertTrue(v3Poller.allStateMachinesFinal());
  }

  private SimpleBinarySemaphore sem =
    new SimpleBinarySemaphore();

  public class MyV3Poller extends V3Poller {
    private MyMockHashService hashService;

    // For testing:  Hashmap of voter IDs to V3LcapMessages.
    private Map sentMsgs = Collections.synchronizedMap(new HashMap());
    private Map semaphores = new HashMap();

    MyV3Poller(PollSpec spec, LockssDaemon daemon, PeerIdentity id,
	       byte[] challenge, String pollkey, long duration,
	       String hashAlg) {
      super(spec, daemon, id, challenge, pollkey, duration, hashAlg);
      try {
          hashService = new MyMockHashService();
      } catch (NoSuchAlgorithmException ex) {
          throw new RuntimeException(ex.getMessage()); // Fail the test
      }
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

    protected void scheduleHash() {
      hashService.scheduleHash(new HashingCompleteCallback());
    }

    public V3LcapMessage getSentMessage(PeerIdentity voter) {
      V3LcapMessage ret = null;
      SimpleBinarySemaphore sem = (SimpleBinarySemaphore)semaphores.get(voter);
      sem.take(5000); // Really shouldn't take this long
      return (V3LcapMessage)sentMsgs.get(voter);
    }

    public boolean allStateMachinesFinal() {
      for (Iterator iter = m_stateMachines.values().iterator(); iter.hasNext(); ) {
	PsmInterp interp = (PsmInterp) iter.next();
	if (!interp.isFinalState()) {
	  return false;
	}
      }
      return true;
    }
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
    p.setProperty(ConfigManager.PARAM_NEW_SCHEDULER, "true");
    p.setProperty(V3Poller.PARAM_MIN_POLL_SIZE, "4");
    p.setProperty(V3Poller.PARAM_MAX_POLL_SIZE, "4");
    p.setProperty(BlockTally.PARAM_QUORUM, "3");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    idmgr = theDaemon.getIdentityManager();
    idmgr.startService();
    theDaemon.getSchedService().startService();
    theDaemon.getHashService().startService();
    theDaemon.getDatagramRouterManager().startService();
    theDaemon.getRouterManager().startService();
    theDaemon.getSystemMetrics().startService();
    theDaemon.getActivityRegulator(testau).startService();
    theDaemon.setNodeManager(new MockNodeManager(), testau);
    pollmanager.startService();
  }
}

/** 
 * XXX: A mock "Hash Service" (but not a real implementation of
 * HashService) used for simple testing during development.  Replace
 * with a proper mock HashService implementation when possible
 */
class MyMockHashService {
  MessageDigest digest;
  
  MyMockHashService() throws NoSuchAlgorithmException {
    digest = MessageDigest.getInstance("SHA-1");
  }

  public boolean scheduleHash(final V3Poller.HashingCompleteCallback callback) {
      Runnable r = new Runnable() {
	  public void run() {
	    try {
	      // Simulate spending some time on something, but not much.
	      digest.update("this is some text to hash.".getBytes());
	      Thread.sleep(100);
	      // Block 1 completed
	      callback.blockFinished(null, "testfile1", digest, null);
	      // Block 2 completed
	      callback.blockFinished(null, "testfile2", digest, null);
	      // Full hash completed
	      callback.hashingFinished(null, null, null, null);
	    } catch (InterruptedException ignore) {;}
	  }
	};
      Thread hashingThread = new Thread(r);
      hashingThread.start();
      return true;
  }
}
