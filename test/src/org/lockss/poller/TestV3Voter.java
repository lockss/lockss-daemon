/*
 * $Id: TestV3Voter.java,v 1.1.2.6 2004-11-24 23:37:47 dshr Exp $
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

/** JUnitTest case for class: org.lockss.poller.V3Voter */
public class TestV3Voter extends LockssTestCase {
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
  protected LcapMessage[] testV3msg = new LcapMessage[msgType.length];;
  protected V3Poll[] testV3polls;
  protected PollSpec[] testSpec = new PollSpec[msgType.length];
  protected PollManager pollmanager;

  protected void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated();

    initRequiredServices();

    testau.setPlugin(new MyMockPlugin());

    initTestPeerIDs();
    initTestMsg();
    initTestPolls();
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
    TimeBase.setReal();
    for(int i=0; i<testV3msg.length; i++) {
      if (testV3msg[i] != null)
	pollmanager.removePoll(testV3msg[i].getKey());
    }
    theDaemon.getPollManager().stopService();
    theDaemon.getPluginManager().stopService();
    super.tearDown();
  }

  // Tests

  public void testInitialVoterState() {
    assertTrue(testV3polls[0] instanceof V3Voter);
    assertEquals("Poll " + testV3polls[0] + " should be in Initializing",
		 V3Voter.STATE_INITIALIZING,
		 testV3polls[0].getPollState());
  }

  public void testVoterDuration() {
    V3Voter poll = (V3Voter) testV3polls[0];
    long duration = testV3msg[0].getDuration();
    final int numSteps = 10;
    long step = (duration - 1) / numSteps;
    String key = poll.getKey();
    assertTrue(step > 1);
    assertEquals("Poll " + poll + " should be in Initializing",
		 V3Voter.STATE_INITIALIZING,
		 poll.getPollState());
    for (int i = 0; i < numSteps; i++) {
      assertTrue("Poll " + poll + " should be active",
		 pollmanager.isPollActive(key));
      assertFalse("Poll " + poll + " should not be closed",
		  pollmanager.isPollClosed(key));
      assertFalse("Poll " + poll + " should not be suspended",
		  pollmanager.isPollSuspended(key));
      TimeBase.step(step);
    }
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    TimeBase.step(step);
    assertFalse("Poll " + poll + " should not be active",
		pollmanager.isPollActive(key));
    assertTrue("Poll " + poll + " should be closed",
	       pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    PollTally tally = poll.getVoteTally();
    assertNotNull(tally);
    List votes = tally.getPollVotes();
    assertTrue(votes.isEmpty());
  }

  public void testNormalVoterStateTransitions() {
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
    
    //  Check starting conditions
    V3Voter poll = (V3Voter)testV3polls[0];
    String key = poll.getKey();
    assertEquals("Poll " + poll + " should be in Initializing",
		 V3Voter.STATE_INITIALIZING,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    TimeBase.step(1000);
    //  Receive a Poll message, go to SendingPollAck
    try {
      pollmanager.handleIncomingMessage(testV3msg[0]);
    } catch (IOException ex) {
      fail("Message " + testV3msg[0].toString() + " threw " + ex);
    }
    assertEquals("Poll " + poll + " should be in SendingPollAck",
		 V3Voter.STATE_VERIFYING_POLL_EFFORT,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in SendingPollAck",
		 V3Voter.STATE_PROVING_POLL_ACK,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in WaitingPollProof",
		 V3Voter.STATE_WAITING_POLL_PROOF,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    {
      // Check that the poll sent a MSG_POLL_ACK
      MockLcapStreamRouter router =
	(MockLcapStreamRouter)theDaemon.getStreamRouterManager();
      Deadline dl = Deadline.in(10);
      V3LcapMessage msg = router.getSentMessage(dl);
      assertNotNull(msg);
      assertEquals(msg.getOpcode(), V3LcapMessage.MSG_POLL_ACK);
      assertTrue("Send queue should be empty after PollAck",
		 router.sendQueueEmpty());
    }
    //  Receive a PollProof message, go to SendingVote
    try {
      pollmanager.handleIncomingMessage(testV3msg[2]);
    } catch (IOException ex) {
      fail("Message " + testV3msg[2].toString() + " threw " + ex);
    }
    assertEquals("Poll " + poll + " should be in SendingVote",
		 V3Voter.STATE_VERIFYING_POLL_PROOF,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in SendingVote",
		 V3Voter.STATE_GENERATING_VOTE,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in WaitingRepairReq",
		 V3Voter.STATE_WAITING_REPAIR_REQ,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    {
      // Check that the poll sent a MSG_VOTE
      MockLcapStreamRouter router =
	(MockLcapStreamRouter)theDaemon.getStreamRouterManager();
      Deadline dl = Deadline.in(10);
      V3LcapMessage msg = router.getSentMessage(dl);
      assertNotNull(msg);
      assertEquals(msg.getOpcode(), V3LcapMessage.MSG_VOTE);
      assertTrue("Send queue should be empty after PollAck",
		 router.sendQueueEmpty());
    }
    //  Receive a RepairReq message, go to SendingRepair
    try {
      pollmanager.handleIncomingMessage(testV3msg[4]);
    } catch (IOException ex) {
      fail("Message " + testV3msg[4].toString() + " threw " + ex);
    }
    assertEquals("Poll " + poll + " should be in SendingRepair",
		 V3Voter.STATE_SENDING_REPAIR,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    // And after a while go back to WaitingRepairReq
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in WaitingRepairReq",
		 V3Voter.STATE_WAITING_REPAIR_REQ,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    //  Receive an EvaluationReceipt, go to ProcessReceipt
    try {
      pollmanager.handleIncomingMessage(testV3msg[6]);
    } catch (IOException ex) {
      fail("Message " + testV3msg[6].toString() + " threw " + ex);
    }
    assertEquals("Poll " + poll + " should be in ProcessReceipt",
		 V3Voter.STATE_PROCESS_RECEIPT,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    // And after a while go to Finalizing
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in Finalizing",
		 V3Voter.STATE_FINALIZING,
		 poll.getPollState());
    assertFalse("Poll " + poll + " should not be active",
		pollmanager.isPollActive(key));
    assertTrue("Poll " + poll + " should be closed",
	       pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
  }

  public void testVoterBadMessageOrderOne() {
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
    
    //  Check starting conditions
    V3Voter poll = (V3Voter)testV3polls[0];
    String key = testV3polls[0].getKey();
    assertEquals("Poll " + poll + " should be in Initializing",
		 V3Voter.STATE_INITIALIZING,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    TimeBase.step(1000);
    //  Receive a PollProof message, go to Error
    try {
      pollmanager.handleIncomingMessage(testV3msg[2]);
    } catch (IOException ex) {
      fail("Message " + testV3msg[2].toString() + " threw " + ex);
    }
    assertEquals("Poll " + poll + " should be in Finalizing",
		 V3Voter.STATE_FINALIZING,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be in error",
	       poll.isErrorState());
    assertFalse("Poll " + poll + " should not be active",
		pollmanager.isPollActive(key));
    assertTrue("Poll " + poll + " should be closed",
	       pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
  }

  public void testVoterBadMessageOrderTwo() {
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
    
    //  Check starting conditions
    V3Voter poll = (V3Voter) testV3polls[0];
    String key = testV3polls[0].getKey();
    assertEquals("Poll " + poll + " should be in Initializing",
		 V3Voter.STATE_INITIALIZING,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    TimeBase.step(1000);
    //  Receive a Poll message, go to SendingPollAck
    try {
      pollmanager.handleIncomingMessage(testV3msg[0]);
    } catch (IOException ex) {
      fail("Message " + testV3msg[0].toString() + " threw " + ex);
    }
    assertEquals("Poll " + poll + " should be in SendingPollAck",
		 V3Voter.STATE_VERIFYING_POLL_EFFORT,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in SendingPollAck",
		 V3Voter.STATE_PROVING_POLL_ACK,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in WaitingPollProof",
		 V3Voter.STATE_WAITING_POLL_PROOF,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    {
      // Check that the poll sent a MSG_POLL_ACK
      MockLcapStreamRouter router =
	(MockLcapStreamRouter)theDaemon.getStreamRouterManager();
      Deadline dl = Deadline.in(10);
      V3LcapMessage msg = router.getSentMessage(dl);
      assertNotNull(msg);
      assertEquals(msg.getOpcode(), V3LcapMessage.MSG_POLL_ACK);
      assertTrue("Send queue should be empty after PollAck",
		 router.sendQueueEmpty());
    }
    //  Receive a RepairReq message, go to Error
    try {
      pollmanager.handleIncomingMessage(testV3msg[4]);
    } catch (IOException ex) {
      fail("Message " + testV3msg[4].toString() + " threw " + ex);
    }
    assertEquals("Poll " + poll + " should be in Finalizing",
		 V3Voter.STATE_FINALIZING,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be in error",
	       poll.isErrorState());
    assertFalse("Poll " + poll + " should not be active",
		pollmanager.isPollActive(key));
    assertTrue("Poll " + poll + " should be closed",
	       pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
  }

  public void testVoterBadMessageOrderThree() {
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
    
    //  Check starting conditions
    V3Voter poll = (V3Voter) testV3polls[0];
    String key = testV3polls[0].getKey();
    assertEquals("Poll " + poll + " should be in Initializing",
		 V3Voter.STATE_INITIALIZING,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    TimeBase.step(1000);
    //  Receive a Poll message, go to SendingPollAck
    try {
      pollmanager.handleIncomingMessage(testV3msg[0]);
    } catch (IOException ex) {
      fail("Message " + testV3msg[0].toString() + " threw " + ex);
    }
    assertEquals("Poll " + poll + " should be in SendingPollAck",
		 V3Voter.STATE_VERIFYING_POLL_EFFORT,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in SendingPollAck",
		 V3Voter.STATE_PROVING_POLL_ACK,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in WaitingPollProof",
		 V3Voter.STATE_WAITING_POLL_PROOF,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    {
      // Check that the poll sent a MSG_POLL_ACK
      MockLcapStreamRouter router =
	(MockLcapStreamRouter)theDaemon.getStreamRouterManager();
      Deadline dl = Deadline.in(10);
      V3LcapMessage msg = router.getSentMessage(dl);
      assertNotNull(msg);
      assertEquals(msg.getOpcode(), V3LcapMessage.MSG_POLL_ACK);
      assertTrue("Send queue should be empty after PollAck",
		 router.sendQueueEmpty());
    }
    //  Receive a PollProof message, go to SendingVote
    try {
      pollmanager.handleIncomingMessage(testV3msg[2]);
    } catch (IOException ex) {
      fail("Message " + testV3msg[2].toString() + " threw " + ex);
    }
    assertEquals("Poll " + poll + " should be in SendingVote",
		 V3Voter.STATE_VERIFYING_POLL_PROOF,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in SendingVote",
		 V3Voter.STATE_GENERATING_VOTE,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in WaitingRepairReq",
		 V3Voter.STATE_WAITING_REPAIR_REQ,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    {
      // Check that the poll sent a MSG_VOTE
      MockLcapStreamRouter router =
	(MockLcapStreamRouter)theDaemon.getStreamRouterManager();
      Deadline dl = Deadline.in(10);
      V3LcapMessage msg = router.getSentMessage(dl);
      assertNotNull(msg);
      assertEquals(msg.getOpcode(), V3LcapMessage.MSG_VOTE);
      assertTrue("Send queue should be empty after PollAck",
		 router.sendQueueEmpty());
    }
    //  Receive a RepairReq message, go to SendingRepair
    try {
      pollmanager.handleIncomingMessage(testV3msg[4]);
    } catch (IOException ex) {
      fail("Message " + testV3msg[4].toString() + " threw " + ex);
    }
    assertEquals("Poll " + poll + " should be in SendingRepair",
		 V3Voter.STATE_SENDING_REPAIR,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    // And after a while go back to WaitingRepairReq
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in WaitingRepairReq",
		 V3Voter.STATE_WAITING_REPAIR_REQ,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    //  Receive a Poll message, go to Error
    try {
      pollmanager.handleIncomingMessage(testV3msg[0]);
    } catch (IOException ex) {
      fail("Message " + testV3msg[0].toString() + " threw " + ex);
    }
    assertEquals("Poll " + poll + " should be in Finalizing",
		 V3Voter.STATE_FINALIZING,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be in error",
	       poll.isErrorState());
    assertFalse("Poll " + poll + " should not be active",
		pollmanager.isPollActive(key));
    assertTrue("Poll " + poll + " should be closed",
	       pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
  }

  public void testBadEffortOnPoll() {
    //  Set up effort service stuff
    MockEffortService es = (MockEffortService)theDaemon.getEffortService();
    es.setGenerateProofResult(true);
    es.setVerifyProofResult(false);  //  NB - effort will not verify
    es.setProofDuration(400);
    es.setProofException(null);
    es.setGenerateVoteResult(true);
    es.setVerifyVoteResult(true);
    es.setAgreeVoteResult(true);
    es.setVoteDuration(400);
    es.setVoteException(null);
    
    //  Check starting conditions
    V3Voter poll = (V3Voter)testV3polls[0];
    String key = testV3polls[0].getKey();
    assertEquals("Poll " + poll + " should be in Initializing",
		 V3Voter.STATE_INITIALIZING,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    TimeBase.step(1000);
    //  Receive a Poll message, go to Error
    try {
      pollmanager.handleIncomingMessage(testV3msg[0]);
    } catch (IOException ex) {
      fail("Message " + testV3msg[0].toString() + " threw " + ex);
    }
    assertEquals("Poll " + poll + " should be in SendingPollAck",
		 V3Voter.STATE_VERIFYING_POLL_EFFORT,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in Finalizing",
		 V3Voter.STATE_FINALIZING,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be in error",
	       poll.isErrorState());
    assertFalse("Poll " + poll + " should not be active",
		pollmanager.isPollActive(key));
    assertTrue("Poll " + poll + " should be closed",
	       pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
  }

  public void testBadPollProofEffort() {
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
    
    //  Check starting conditions
    V3Voter poll = (V3Voter)testV3polls[0];
    String key = poll.getKey();
    assertEquals("Poll " + poll + " should be in Initializing",
		 V3Voter.STATE_INITIALIZING,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    TimeBase.step(1000);
    //  Receive a Poll message, go to SendingPollAck
    try {
      pollmanager.handleIncomingMessage(testV3msg[0]);
    } catch (IOException ex) {
      fail("Message " + testV3msg[0].toString() + " threw " + ex);
    }
    assertEquals("Poll " + poll + " should be in SendingPollAck",
		 V3Voter.STATE_VERIFYING_POLL_EFFORT,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in SendingPollAck",
		 V3Voter.STATE_PROVING_POLL_ACK,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in WaitingPollProof",
		 V3Voter.STATE_WAITING_POLL_PROOF,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    {
      // Check that the poll sent a MSG_POLL_ACK
      MockLcapStreamRouter router =
	(MockLcapStreamRouter)theDaemon.getStreamRouterManager();
      Deadline dl = Deadline.in(10);
      V3LcapMessage msg = router.getSentMessage(dl);
      assertNotNull(msg);
      assertEquals(msg.getOpcode(), V3LcapMessage.MSG_POLL_ACK);
      assertTrue("Send queue should be empty after PollAck",
		 router.sendQueueEmpty());
    }
    //  Receive a PollProof message, go to SendingVote
    es.setVerifyProofResult(false);
    try {
      pollmanager.handleIncomingMessage(testV3msg[2]);
    } catch (IOException ex) {
      fail("Message " + testV3msg[2].toString() + " threw " + ex);
    }
    assertEquals("Poll " + poll + " should be in SendingVote",
		 V3Voter.STATE_VERIFYING_POLL_PROOF,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in Finalizing",
		 V3Voter.STATE_FINALIZING,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be in error",
	       poll.isErrorState());
    assertFalse("Poll " + poll + " should not be active",
		pollmanager.isPollActive(key));
    assertTrue("Poll " + poll + " should be closed",
	       pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
  }

  public void testExceptionOnPollEffortVerification() {
    //  Set up effort service stuff
    MockEffortService es = (MockEffortService)theDaemon.getEffortService();
    es.setGenerateProofResult(true);
    es.setVerifyProofResult(true);
    es.setProofDuration(400);
    es.setProofException(new IOException("Test exception"));
    es.setGenerateVoteResult(true);
    es.setVerifyVoteResult(true);
    es.setAgreeVoteResult(true);
    es.setVoteDuration(400);
    es.setVoteException(null);
    
    //  Check starting conditions
    V3Voter poll = (V3Voter)testV3polls[0];
    String key = poll.getKey();
    assertEquals("Poll " + poll + " should be in Initializing",
		 V3Voter.STATE_INITIALIZING,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    TimeBase.step(1000);
    //  Receive a Poll message, go to SendingPollAck
    try {
      pollmanager.handleIncomingMessage(testV3msg[0]);
    } catch (IOException ex) {
      fail("Message " + testV3msg[0].toString() + " threw " + ex);
    }
    assertEquals("Poll " + poll + " should be in SendingPollAck",
		 V3Voter.STATE_VERIFYING_POLL_EFFORT,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    stepTimeUntilPollStateChanges(poll);
    //  Should have thrown
    assertEquals("Poll " + poll + " should be in Finalizing",
		 V3Voter.STATE_FINALIZING,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be in error",
	       poll.isErrorState());
    assertFalse("Poll " + poll + " should not be active",
		pollmanager.isPollActive(key));
    assertTrue("Poll " + poll + " should be closed",
	       pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
  }

  public void testExceptionOnPollAckEffortGeneration() {
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
    
    //  Check starting conditions
    V3Voter poll = (V3Voter)testV3polls[0];
    String key = poll.getKey();
    assertEquals("Poll " + poll + " should be in Initializing",
		 V3Voter.STATE_INITIALIZING,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    TimeBase.step(1000);
    //  Receive a Poll message, go to SendingPollAck
    try {
      pollmanager.handleIncomingMessage(testV3msg[0]);
    } catch (IOException ex) {
      fail("Message " + testV3msg[0].toString() + " threw " + ex);
    }
    assertEquals("Poll " + poll + " should be in SendingPollAck",
		 V3Voter.STATE_VERIFYING_POLL_EFFORT,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    es.setProofException(new IOException("Test exception"));
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in SendingPollAck",
		 V3Voter.STATE_PROVING_POLL_ACK,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in Finalizing",
		 V3Voter.STATE_FINALIZING,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be in error",
	       poll.isErrorState());
    assertFalse("Poll " + poll + " should not be active",
		pollmanager.isPollActive(key));
    assertTrue("Poll " + poll + " should be closed",
	       pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
  }

  public void testExceptionOnPollProofEffortVerification() {
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
    
    //  Check starting conditions
    V3Voter poll = (V3Voter)testV3polls[0];
    String key = poll.getKey();
    assertEquals("Poll " + poll + " should be in Initializing",
		 V3Voter.STATE_INITIALIZING,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    TimeBase.step(1000);
    //  Receive a Poll message, go to SendingPollAck
    try {
      pollmanager.handleIncomingMessage(testV3msg[0]);
    } catch (IOException ex) {
      fail("Message " + testV3msg[0].toString() + " threw " + ex);
    }
    assertEquals("Poll " + poll + " should be in SendingPollAck",
		 V3Voter.STATE_VERIFYING_POLL_EFFORT,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in SendingPollAck",
		 V3Voter.STATE_PROVING_POLL_ACK,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in WaitingPollProof",
		 V3Voter.STATE_WAITING_POLL_PROOF,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    {
      // Check that the poll sent a MSG_POLL_ACK
      MockLcapStreamRouter router =
	(MockLcapStreamRouter)theDaemon.getStreamRouterManager();
      Deadline dl = Deadline.in(10);
      V3LcapMessage msg = router.getSentMessage(dl);
      assertNotNull(msg);
      assertEquals(msg.getOpcode(), V3LcapMessage.MSG_POLL_ACK);
      assertTrue("Send queue should be empty after PollAck",
		 router.sendQueueEmpty());
    }
    //  Receive a PollProof message, go to SendingVote
    es.setProofException(new Exception("bad verify"));
    try {
      pollmanager.handleIncomingMessage(testV3msg[2]);
    } catch (IOException ex) {
      fail("Message " + testV3msg[2].toString() + " threw " + ex);
    }
    assertEquals("Poll " + poll + " should be in SendingVote",
		 V3Voter.STATE_VERIFYING_POLL_PROOF,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in Finalizing",
		 V3Voter.STATE_FINALIZING,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be in error",
	       poll.isErrorState());
    assertFalse("Poll " + poll + " should not be active",
		pollmanager.isPollActive(key));
    assertTrue("Poll " + poll + " should be closed",
	       pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
  }

  public void testExceptionOnVoteGeneration() {
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
    
    //  Check starting conditions
    V3Voter poll = (V3Voter)testV3polls[0];
    String key = poll.getKey();
    assertEquals("Poll " + poll + " should be in Initializing",
		 V3Voter.STATE_INITIALIZING,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    TimeBase.step(1000);
    //  Receive a Poll message, go to SendingPollAck
    try {
      pollmanager.handleIncomingMessage(testV3msg[0]);
    } catch (IOException ex) {
      fail("Message " + testV3msg[0].toString() + " threw " + ex);
    }
    assertEquals("Poll " + poll + " should be in SendingPollAck",
		 V3Voter.STATE_VERIFYING_POLL_EFFORT,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in SendingPollAck",
		 V3Voter.STATE_PROVING_POLL_ACK,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in WaitingPollProof",
		 V3Voter.STATE_WAITING_POLL_PROOF,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    {
      // Check that the poll sent a MSG_POLL_ACK
      MockLcapStreamRouter router =
	(MockLcapStreamRouter)theDaemon.getStreamRouterManager();
      Deadline dl = Deadline.in(10);
      V3LcapMessage msg = router.getSentMessage(dl);
      assertNotNull(msg);
      assertEquals(msg.getOpcode(), V3LcapMessage.MSG_POLL_ACK);
      assertTrue("Send queue should be empty after PollAck",
		 router.sendQueueEmpty());
    }
    //  Receive a PollProof message, go to SendingVote
    es.setVoteException(new Exception("bad vote generation"));
    try {
      pollmanager.handleIncomingMessage(testV3msg[2]);
    } catch (IOException ex) {
      fail("Message " + testV3msg[2].toString() + " threw " + ex);
    }
    assertEquals("Poll " + poll + " should be in SendingVote",
		 V3Voter.STATE_VERIFYING_POLL_PROOF,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in SendingVote",
		 V3Voter.STATE_GENERATING_VOTE,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + poll + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
    stepTimeUntilPollStateChanges(poll);
    assertEquals("Poll " + poll + " should be in Finalizing",
		 V3Voter.STATE_FINALIZING,
		 poll.getPollState());
    assertTrue("Poll " + poll + " should be in error",
	       poll.isErrorState());
    assertFalse("Poll " + poll + " should not be active",
		pollmanager.isPollActive(key));
    assertTrue("Poll " + poll + " should be closed",
	       pollmanager.isPollClosed(key));
    assertFalse("Poll " + poll + " should not be suspended",
		pollmanager.isPollSuspended(key));
  }


  //  Support methods

  private void initRequiredServices() {
    theDaemon = new MockLockssDaemon();
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
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    p.setProperty(ConfigManager.PARAM_NEW_SCHEDULER, "false");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    idmgr = theDaemon.getIdentityManager();
    idmgr.startService();
    //theDaemon.getSchedService().startService();
    theDaemon.getHashService().startService();
    {
      // Make two FifoQueue objects
      FifoQueue q1 = new FifoQueue();
      FifoQueue q2 = new FifoQueue();
      assertNotNull(q1);
      assertNotNull(q2);
      // Use the two to create a MockLcapStreamRouter that has no
      // partner,  so sent messages go into the bitbucket and
      // received messages have to be simulated via the receiveMessage()
      // method.
      LcapStreamRouter myRouter = new MockLcapStreamRouter(q1, q2);
      theDaemon.setStreamRouterManager(myRouter);
    }
    theDaemon.getStreamRouterManager().startService();
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

  private void initTestMsg() throws Exception {
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
    log.debug("Duration is " + duration);
    byte[] challenge = pollmanager.makeVerifier(duration);

    for (int i= 0; i<testV3msg.length; i++) {
      testSpec[i] = spec;
      testV3msg[i] =
	V3LcapMessage.makeRequestMsg(spec,
				     null, // XXX entries not needed
				     challenge,
				     msgType[i],
				     duration,
				     testID1, "SHA-1");
      assertNotNull(testV3msg[i]);
      log.debug("Made " + testV3msg[i] + " from " + spec);
    }

  }

  private void initTestPolls() throws Exception {
    testV3polls = new V3Poll[1];
    for (int i = 0; i < testV3polls.length; i++) {
      log.debug3("initTestPolls: V3 " + i);
      BasePoll p = pollmanager.makePoll(testSpec[i],
					testV3msg[i].getDuration(),
					testV3msg[i].getChallenge(),
					testV3msg[i].getVerifier(),
					testV3msg[i].getOriginatorID(),
					testV3msg[i].getHashAlgorithm());
      log.debug("initTestPolls: V3 " + i + " returns " + p);
      switch (i) {
      case 0:
	assertTrue(p instanceof V3Voter);
	break;
      default:
	assertNull(p);
	break;
      }
      testV3polls[i] = (V3Poll)p;
      log.debug3("initTestPolls: " + i + " " + p.toString());
    }
  }

  private void stepTimeUntilPollStateChanges(V3Voter poll) {
    // XXX
    int oldState = poll.getPollState();
    int newState = -1;
    long startTime = TimeBase.nowMs();
    do {
      TimeBase.step(100);
    } while ((newState = poll.getPollState()) == oldState);
    log.debug("Change from " + poll.getPollStateName(oldState) +
	      " to " + poll.getPollStateName(newState) +
	      " after " + (TimeBase.nowMs() - startTime) + "ms");
  }

  public class MyMockPlugin extends MockPlugin {
    public CachedUrlSet makeCachedUrlSet(ArchivalUnit owner,
					 CachedUrlSetSpec cuss) {
      return new PollTestPlugin.PTCachedUrlSet((MockArchivalUnit)owner, cuss);
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
