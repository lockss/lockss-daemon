/*
 * $Id: TestV3Poll.java,v 1.1.2.23 2004-12-12 22:46:19 dshr Exp $
 */

/*

Copyright (c) 2004 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.poller;

import java.io.*;
import java.util.*;
import java.net.*;
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.config.*;
import org.lockss.effort.*;
import org.lockss.repository.LockssRepositoryImpl;

/** JUnitTest case for class: org.lockss.poller.V3Poll */
public class TestV3Poll extends LockssTestCase {
  private static Logger log = Logger.getLogger("TestV3Poll");
  private static String[] rootV3urls = {
    "http://www.test.net",
    "http://www.test1.net", "http://www.test2.net"};
  private static String lwrbnd = "test1.doc";
  private static String uprbnd = "test3.doc";
  private static long testduration = Constants.DAY;

  protected MockArchivalUnit testau;
  private IdentityManager idmgr;
  private MockLockssDaemon theDaemon;

  protected PeerIdentity testID;
  protected PeerIdentity testID1;
  protected int[] msgType = {
    V3LcapMessage.MSG_POLL,
    V3LcapMessage.MSG_POLL_ACK,
    V3LcapMessage.MSG_POLL_PROOF,
    V3LcapMessage.MSG_VOTE,
    V3LcapMessage.MSG_REPAIR_REQ,
    V3LcapMessage.MSG_REPAIR_REP,
    V3LcapMessage.MSG_EVALUATION_RECEIPT,
  };
  protected V3Voter voter;
  protected V3Poller poller;
  protected PollSpec pollSpec;
  protected PollManager pollmanager;
  protected MockLcapStreamRouter router;
  protected MyMessageHandler handler = null;
  protected Hashtable pollHash = new Hashtable();

  protected void setUp() throws Exception {
    super.setUp();
    pollHash = new Hashtable();
    TimeBase.setSimulated();

    initRequiredServices();

    testau.setPlugin(new MyMockPlugin());

    initTestPeerIDs();
  }

  /** tearDown method for test case
   * @throws Exception if removePoll failed
   */
  public void tearDown() throws Exception {
    pollmanager.stopService();
    theDaemon.getLockssRepository(testau).stopService();
    theDaemon.getHashService().stopService();
    theDaemon.getStreamRouterManager().stopService();
    theDaemon.getSystemMetrics().stopService();
    pollmanager.removePoll(voter.getKey());
    pollmanager.removePoll(poller.getKey());
    theDaemon.getPollManager().stopService();
    theDaemon.getPluginManager().stopService();
    TimeBase.setReal();
    super.tearDown();
  }

  // Tests

  public void testPollDuration() {
    try {
      initTestPoll();
    } catch (Exception ex) {
      fail("initTestPoll threw " + ex.toString());
    }
    //  The two halves of the poll have been created but
    //  no time has elapsed
    assertTrue(voter instanceof V3Voter);
    assertEquals("Poll " + voter + " should be in Initializing",
		 V3Voter.STATE_INITIALIZING,
		 voter.getPollState());
    assertTrue(poller instanceof V3Poller);
    assertEquals("Poll " + poller + " should be in Initializing",
		 V3Poller.STATE_INITIALIZING,
		 poller.getPollState());
    Thread.yield();
    long duration = poller.getDeadline().getRemainingTime();
    String key = poller.getKey();
    final int numSteps = 10;
    long step = (duration - 1) / numSteps;
    assertTrue("Duration " + duration + " means step " + step, step > 1);
    log.debug("Duration " + duration + " means step " + step);
    //  XXX should test state of voter too
    assertEquals("Poll " + poller + " should be in Initializing",
		 V3Poller.STATE_INITIALIZING,
		 poller.getPollState());
    assertEquals("Poll " + voter + " should be in Initializing",
		 V3Poller.STATE_INITIALIZING,
		 poller.getPollState());
    Thread.yield();
    for (int i = 0; i < numSteps; i++) {
      Thread.yield();
      assertTrue("Poll " + poller + " should be active",
		 pollmanager.isPollActive(key));
      assertFalse("Poll " + poller + " should not be closed",
		  pollmanager.isPollClosed(key));
      assertFalse("Poll " + poller + " should not be suspended",
		  pollmanager.isPollSuspended(key));
      assertTrue("Poll " + voter + " should be active",
		 pollmanager.isPollActive(key));
      assertFalse("Poll " + voter + " should not be closed",
		  pollmanager.isPollClosed(key));
      assertFalse("Poll " + voter + " should not be suspended",
		  pollmanager.isPollSuspended(key));
      log.debug("Step " + i);
      Thread.yield();
      TimeBase.step(step);
      Thread.yield();
    }
    Thread.yield();
    assertTrue("Poll " + poller + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poller + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poller + " should not be suspended",
		pollmanager.isPollSuspended(key));
    assertTrue("Poll " + voter + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + voter + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + voter + " should not be suspended",
		pollmanager.isPollSuspended(key));
    log.debug("Penultimate step ");
    TimeBase.step(step);
    Thread.yield();
    assertFalse("Poll " + poller + " should not be active",
		pollmanager.isPollActive(key));
    assertTrue("Poll " + poller + " should be closed",
	       pollmanager.isPollClosed(key));
    assertFalse("Poll " + poller + " should not be suspended",
		pollmanager.isPollSuspended(key));
    assertFalse("Poll " + voter + " should not be active",
		pollmanager.isPollActive(key));
    assertTrue("Poll " + voter + " should be closed",
	       pollmanager.isPollClosed(key));
    assertFalse("Poll " + voter + " should not be suspended",
		pollmanager.isPollSuspended(key));
    log.debug("About to tally");
    PollTally tally = poller.getVoteTally();
    assertNotNull(tally);
    List votes = tally.getPollVotes();
    assertTrue(votes.isEmpty());
  }

  public void testNormalPollWithOneVote() {
    log.debug("Starting testNormalPollWithOneVote()");
    try {
      initTestPoll();
    } catch (Exception ex) {
      fail("initTestPoll threw " + ex.toString());
    }
    Thread.yield();
    //  Set up effort service stuff
    MockEffortService es = (MockEffortService)theDaemon.getEffortService();
    es.setGenerateProofResult(true);
    es.setVerifyProofResult(true);
    es.setProofDuration(400);
    es.setProofException(null);
    es.setGenerateVoteResult(true);
    es.setVerifyVoteResult(true);
    es.setAgreeVoteResult(true);
    es.setVoteDuration(400);
    es.setVoteException(null);
    Thread.yield();
    //  The two halves of the poll have been created but
    //  no time has elapsed
    assertTrue(voter instanceof V3Voter);
    assertEquals("Poll " + voter + " should be in Initializing",
		 V3Voter.STATE_INITIALIZING,
		 voter.getPollState());
    assertTrue(poller instanceof V3Poller);
    assertEquals("Poll " + poller + " should be in Initializing",
		 V3Poller.STATE_INITIALIZING,
		 poller.getPollState());
    String key = poller.getKey();
    List peers = new ArrayList();
    peers.add(testID1);
    Thread.yield();
    poller.solicitVotesFrom(peers);
    Thread.yield();
    int steps = 100;
    while (poller.getPollState() != V3Poller.STATE_FINALIZING ||
	   voter.getPollState() != V3Voter.STATE_FINALIZING) {
      Thread.yield();
      stepTimeUntilPollStateChanges(poller, voter, "testing");
      log.debug("poller state " + poller.getPollStateName(poller.getPollState()) +
		" voter state " + voter.getPollStateName(voter.getPollState()));
      steps--;
      assertTrue("Too many steps", steps > 0);
      Thread.yield();
    }
    PollTally pollerTally = poller.getVoteTally();
    assertEquals(pollerTally.getTallyResult(), Tallier.RESULT_NOQUORUM);
    PollTally voterTally = poller.getVoteTally();
    assertEquals(voterTally.getTallyResult(), Tallier.RESULT_NOQUORUM);
  }

  //  Support methods

  private void initRequiredServices() {
    theDaemon = new MockLockssDaemon();
    // Make a FifoQueue object
    FifoQueue q1 = new FifoQueue();
    assertNotNull(q1);
    // Use it to create a MockLcapStreamRouter object
    // in loop-back mode
    router = new MockLcapStreamRouter(q1, null);
    // Register handlers
    handler = new MyMessageHandler("TestV3Poll.handler");
    router.registerMessageHandler(handler);
    router.startService();
    theDaemon.setStreamRouterManager(router);
    pollmanager = theDaemon.getPollManager();
    theDaemon.setEffortService((EffortService) new MockEffortService());

    theDaemon.getPluginManager();
    testau = PollTestPlugin.PTArchivalUnit.createFromListOfRootUrls(rootV3urls);
    PluginUtil.registerArchivalUnit(testau);

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
    p.setProperty(IdentityManager.PARAM_LOCAL_V3_PORT, "9999");
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    p.setProperty(ConfigManager.PARAM_NEW_SCHEDULER, "false");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    idmgr = theDaemon.getIdentityManager();
    idmgr.startService();
    //theDaemon.getSchedService().startService();
    theDaemon.getHashService().startService();
    theDaemon.getSystemMetrics().startService();
    theDaemon.getEffortService().startService();
    theDaemon.getActivityRegulator(testau).startService();
    theDaemon.setNodeManager(new MockNodeManager(), testau);
    pollmanager.startService();
  }

  private void initTestPeerIDs() {
    try{
      testID = idmgr.stringToPeerIdentity("127.0.0.1");
      testID1 = idmgr.stringToPeerIdentity("1.1.1.1");
    } catch (IdentityManager.MalformedIdentityKeyException ex) {
      fail("can't open test host:" + ex);
    }
    assertTrue(testID.isLocalIdentity());
    assertFalse(testID1.isLocalIdentity());

  }

  private void initTestPoll() throws Exception {
    BasePoll p;
    PollFactory ppf = pollmanager.getPollFactory(3);
    assertNotNull("PollFactory should not be null", ppf);
    assertTrue(ppf instanceof V3PollFactory);
    V3PollFactory pf = (V3PollFactory)ppf;
    PollSpec spec = new MockPollSpec(testau, rootV3urls[0],
				     lwrbnd, uprbnd, Poll.CONTENT_POLL,
				     Poll.V3_POLL);
    assertEquals(spec.getPollType(), Poll.CONTENT_POLL);
    assertEquals(spec.getPollVersion(), Poll.V3_POLL);
    ((MockCachedUrlSet)spec.getCachedUrlSet()).setHasContent(false);
    long duration = pf.calcDuration(Poll.CONTENT_POLL,
				    spec.getCachedUrlSet(),
				    pollmanager);
    // XXX
    duration = 12000;
    log.debug("Duration is " + duration);
    byte[] challenge = pollmanager.makeVerifier(duration);
    pollSpec = spec;
    // Make the voter
    p = pollmanager.makePoll(pollSpec,
			     duration,
			     challenge,
			     null,
			     testID1,
			     "SHA-1");
    log.debug("initTestPoll: V3 voter returns " + p);
    assertTrue(p instanceof V3Voter);
    voter = (V3Voter) p;
    log.debug3("initTestPoll: voter " + p.toString());
    // Make the poller
    byte[] pollerChallenge = pollmanager.makeVerifier(duration);
    p = pollmanager.makePoll(pollSpec,
			     duration,
			     pollerChallenge,
			     null,
			     testID,
			     "SHA-1");
    log.debug("initTestPoll: V3 poller returns " + p);
    assertTrue(p instanceof V3Poller);
    poller = (V3Poller) p;
    // If sender is testID, recipient is voter
    pollHash.put(testID, voter);
    // If sender is testID1, recipient is poller
    pollHash.put(testID1, poller);
    log.debug3("initTestPoll: poller " + p.toString());
  }

  private void doPollsWithPeers(V3LcapMessage[] testV3msg,
				V3Poller poller, V3Voter[] voters,
				List agree, List disagree, List invalid) {
    String key = poller.getKey();
    List peers = new ArrayList();
    int numAgree = 0;
    int numDisagree = 0;
    int numInvalid = 0;
    MockEffortService es = (MockEffortService)theDaemon.getEffortService();
    if (invalid != null) {
      peers.addAll(invalid);
      numInvalid = invalid.size();
    }
    if (disagree != null) {
      peers.addAll(disagree);
      numDisagree = disagree.size();
    }
    if (agree != null) {
      peers.addAll(agree);
      numAgree = agree.size();
    }
    int totalPeers = peers.size();
    poller.solicitVotesFrom(peers);
    for (int i = totalPeers; i > 0; i--) {

      // Decide to solicit a vote from testID[1],  go to SendingPoll
      assertEquals("Poll " + poller + " vote " + i + " should be in ProvingIntroEffort",
		   V3Poller.STATE_PROVING_INTRO_EFFORT,
		   poller.getPollState());
      assertTrue("Poll " + poller + " vote " + i + " should be active",
		 pollmanager.isPollActive(key));
      assertFalse("Poll " + poller + " vote " + i + " should not be closed",
		  pollmanager.isPollClosed(key));
      assertFalse("Poll " + poller + " vote " + i + " should not be suspended",
		  pollmanager.isPollSuspended(key));
      stepTimeUntilPollStateChanges(poller, voters[i], "Generating a MSG_POLL for " + i);
      // And after a while go to WaitingPollAck
      assertEquals("Poll " + poller + " vote " + i + " should be in WaitingPollAck",
		   V3Poller.STATE_WAITING_POLL_ACK,
		   poller.getPollState());
      assertTrue("Poll " + poller + " vote " + i + " should be active",
		 pollmanager.isPollActive(key));
      assertFalse("Poll " + poller + " vote " + i + " should not be closed",
		  pollmanager.isPollClosed(key));
      assertFalse("Poll " + poller + " vote " + i + " should not be suspended",
		  pollmanager.isPollSuspended(key));
      {
	// Check that the poll sent a MSG_POLL
	MockLcapStreamRouter router =
	  (MockLcapStreamRouter)theDaemon.getStreamRouterManager();
	Deadline dl = Deadline.in(10);
	V3LcapMessage msg = router.getSentMessage(dl);
	assertNotNull(msg);
	assertEquals(msg.getOpcode(), V3LcapMessage.MSG_POLL);
	assertTrue("Send queue should be empty after Poll",
		   router.sendQueueEmpty());
      }
      TimeBase.step(500);
      log.debug("Receive a MSG_POLL_ACK for " + i);
      //  Receive a PollAck message, go toVerifyingPollAckEffort
      try {
	pollmanager.handleIncomingMessage(testV3msg[1]);
      } catch (IOException ex) {
	fail("Message " + testV3msg[1].toString() + " threw " + ex);
      }
      assertEquals("Poll " + poller + " vote " + i + " should be in VerifyingPollAckEffort",
		   V3Poller.STATE_VERIFYING_POLL_ACK_EFFORT,
		   poller.getPollState());
      assertTrue("Poll " + poller + " vote " + i + " should be active",
		 pollmanager.isPollActive(key));
      assertFalse("Poll " + poller + " vote " + i + " should not be closed",
		  pollmanager.isPollClosed(key));
      assertFalse("Poll " + poller + " vote " + i + " should not be suspended",
		  pollmanager.isPollSuspended(key));
      stepTimeUntilPollStateChanges(poller, voters[i], "Verifying a MSG_POLL_ACK for " + i);
      assertEquals("Poll " + poller + " vote " + i + " should be in ProvingRemainingEffort",
		   V3Poller.STATE_PROVING_REMAINING_EFFORT,
		   poller.getPollState());
      assertTrue("Poll " + poller + " vote " + i + " should be active",
		 pollmanager.isPollActive(key));
      assertFalse("Poll " + poller + " vote " + i + " should not be closed",
		  pollmanager.isPollClosed(key));
      assertFalse("Poll " + poller + " vote " + i + " should not be suspended",
		  pollmanager.isPollSuspended(key));
      stepTimeUntilPollStateChanges(poller, voters[i], "Generating a MSG_POLL_PROOF for " + i);
      //  Eventually go to WaitingVote
      {
	// Check that the poll sent a MSG_POLL_PROOF
	MockLcapStreamRouter router =
	  (MockLcapStreamRouter)theDaemon.getStreamRouterManager();
	Deadline dl = Deadline.in(10);
	V3LcapMessage msg = router.getSentMessage(dl);
	assertNotNull(msg);
	assertEquals("Poll " + poller + " vote " + i + " sent " + msg + " not PollProof",
		     msg.getOpcode(), V3LcapMessage.MSG_POLL_PROOF);
	assertTrue("Send queue should be empty after PollProof",
		   router.sendQueueEmpty());
      }
      assertEquals("Poll " + poller + " vote " + i + " should be in WaitingVote",
		   V3Poller.STATE_WAITING_VOTE,
		   poller.getPollState());
      assertTrue("Poll " + poller + " vote " + i + " should be active",
		 pollmanager.isPollActive(key));
      assertFalse("Poll " + poller + " vote " + i + " should not be closed",
		  pollmanager.isPollClosed(key));
      assertFalse("Poll " + poller + " vote " + i + " should not be suspended",
		  pollmanager.isPollSuspended(key));
      //  Receive a Vote message, go to SendingPoll for next voter
      //  unless no more voters in which case go to SendingRepairRequest
      log.debug("Receive a MSG_VOTE for " + i);
      try {
	pollmanager.handleIncomingMessage(testV3msg[3]);
      } catch (IOException ex) {
	fail("Message " + testV3msg[3].toString() + " threw " + ex);
      }
      assertTrue("Poll " + poller + " vote " + i + " should be active",
		 pollmanager.isPollActive(key));
      assertFalse("Poll " + poller + " vote " + i + " should not be closed",
		  pollmanager.isPollClosed(key));
      assertFalse("Poll " + poller + " vote " + i + " should not be suspended",
		  pollmanager.isPollSuspended(key));
      if (i > 1) {
	assertEquals("Poll " + poller + " vote " + i + " should be in ProvingIntroEffort",
		     V3Poller.STATE_PROVING_INTRO_EFFORT,
		     poller.getPollState());
      } else {
	assertEquals("Poll " + poller + " vote " + i + " should be in ChoosingNextVote",
		     V3Poller.STATE_CHOOSING_NEXT_VOTE,
		     poller.getPollState());
      }
      assertTrue(peers.size() == i-1);
    }
    for (int i = totalPeers; i > 0; i--) {
      log.debug("Tallying vote " + i);
      if ((totalPeers - i) < numInvalid) {
	es.setVerifyVoteResult(false);
      } else {
	es.setVerifyVoteResult(true);
      }
      if ((totalPeers - i) < (numInvalid + numDisagree)) {
	es.setAgreeVoteResult(false);
      } else {
	es.setAgreeVoteResult(true);
      }
      stepTimeUntilPollStateChanges(poller, voters[i], "Verifying vote effort for " + i);
      assertEquals("Poll " + poller + " vote " + i + " should be in VerifyingVoteEffort",
		   V3Poller.STATE_VERIFYING_VOTE_EFFORT,
		   poller.getPollState());
      assertTrue("Poll " + poller + " vote " + i + " should be active",
		 pollmanager.isPollActive(key));
      assertFalse("Poll " + poller + " vote " + i + " should not be closed",
		  pollmanager.isPollClosed(key));
      assertFalse("Poll " + poller + " vote " + i + " should not be suspended",
		  pollmanager.isPollSuspended(key));
      stepTimeUntilPollStateChanges(poller, voters[i], "Proving repair effort for " + i);
      assertEquals("Poll " + poller + " vote " + i + " should be in ProvingRepairEffort",
		   V3Poller.STATE_PROVING_REPAIR_EFFORT,
		   poller.getPollState());
      assertTrue("Poll " + poller + " vote " + i + " should be active",
		 pollmanager.isPollActive(key));
      assertFalse("Poll " + poller + " vote " + i + " should not be closed",
		  pollmanager.isPollClosed(key));
      assertFalse("Poll " + poller + " vote " + i + " should not be suspended",
		  pollmanager.isPollSuspended(key));
      stepTimeUntilPollStateChanges(poller, voters[i], "Verifying vote for " + i);
      assertEquals("Poll " + poller + " vote " + i + " should be in VerifyingVote",
		   V3Poller.STATE_VERIFYING_VOTE,
		   poller.getPollState());
      assertTrue("Poll " + poller + " vote " + i + " should be active",
		 pollmanager.isPollActive(key));
      assertFalse("Poll " + poller + " vote " + i + " should not be closed",
		  pollmanager.isPollClosed(key));
      assertFalse("Poll " + poller + " vote " + i + " should not be suspended",
		  pollmanager.isPollSuspended(key));
      if ((totalPeers - i) < numInvalid) {
	stepTimeUntilPollStateChanges(poller, voters[i], "Vote " + i + " invalid");
	assertEquals("Poll " + poller + " vote " + i + " should be in state VerifyingVoteEffort",
		     V3Poller.STATE_CHOOSING_NEXT_VOTE,
		     poller.getPollState());
      } else {
	stepTimeUntilPollStateChanges(poller, voters[i], "Vote " + i + " valid - make receipt");
	assertEquals("Poll " + poller + " vote " + i + " should be in SendingReceipt",
		     V3Poller.STATE_SENDING_RECEIPT,
		     poller.getPollState());
	assertTrue("Poll " + poller + " vote " + i + " should not be active",
		   pollmanager.isPollActive(key));
	assertFalse("Poll " + poller + " vote " + i + " should be closed",
		    pollmanager.isPollClosed(key));
	assertFalse("Poll " + poller + " vote " + i + " should not be suspended",
		    pollmanager.isPollSuspended(key));
	log.debug("XXX before fourth step 500");
	stepTimeUntilPollStateChanges(poller, voters[i], "Going back for another vote");
	if (i > 1) {
	  assertEquals("Poll " + poller + " vote " + i + " should be in VerifyingVoteEffort",
		       V3Poller.STATE_CHOOSING_NEXT_VOTE,
		       poller.getPollState());
	  assertTrue("Poll " + poller + " vote " + i + " should be active",
		     pollmanager.isPollActive(key));
	  assertFalse("Poll " + poller + " vote " + i + " should not be closed",
		      pollmanager.isPollClosed(key));
	  assertFalse("Poll " + poller + " vote " + i + " should not be suspended",
		      pollmanager.isPollSuspended(key));
	  log.debug("XXX before fifth step 500");
	}
      }
    }
  }

  private void stepTimeUntilPollStateChanges(V3Poller poller,
					     V3Voter voter, String s) {
    int oldPollerState = poller.getPollState();
    int newPollerState = -1;
    int oldVoterState = voter.getPollState();
    int newVoterState = -1;
    long startTime = TimeBase.nowMs();
    log.debug(s + " poller " + poller + " voter " + voter);
    do {
	if (oldPollerState == V3Poller.STATE_FINALIZING &&
	    oldVoterState == V3Voter.STATE_FINALIZING) {
	    log.debug("Both sides are Finalizing");
	    return;
	}
	TimeBase.step(100);
	log.debug3("Step poller " +
		   poller.getPollStateName(poller.getPollState()) +
		   " voter " + voter.getPollStateName(voter.getPollState()));
    } while ((newPollerState = poller.getPollState()) == oldPollerState &&
	     (newVoterState = voter.getPollState()) == oldVoterState);
    if (newPollerState != oldPollerState) {
      log.debug("Change from " + poller.getPollStateName(oldPollerState) +
		" to " + poller.getPollStateName(newPollerState) +
		" after " + (TimeBase.nowMs() - startTime) + "ms");
    } else if (newVoterState != oldVoterState) {
      log.debug("Change from " + poller.getPollStateName(oldVoterState) +
		" to " + poller.getPollStateName(newVoterState) +
		" after " + (TimeBase.nowMs() - startTime) + "ms");
    }
  }

  public class MyMockPlugin extends MockPlugin {
    public CachedUrlSet makeCachedUrlSet(ArchivalUnit owner,
					 CachedUrlSetSpec cuss) {
      return new PollTestPlugin.PTCachedUrlSet((MockArchivalUnit)owner, cuss);
    }
  }

  public class MyMessageHandler implements LcapStreamRouter.MessageHandler {
    String handlerName = null;
    public MyMessageHandler(String n) {
      handlerName = n;
    }
    public void handleMessage(V3LcapMessage msg) {
      log.debug(handlerName + ": handleMessage(" + msg + ")");
      PeerIdentity sender = msg.getOriginatorID();
      assertNotNull(sender);
      assertNotNull(pollHash);
      V3Poll recipient = (V3Poll) pollHash.get(sender);
      assertNotNull(recipient);
      log.debug("Handing " + msg + " to " + recipient);
      recipient.receiveMessage(msg);
    }
  }

  /** Executes the test case
   * @param argv array of Strings containing command line arguments
   * */
  public static void main(String[] argv) {
    String[] testCaseList = {
      TestV3Poll.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
