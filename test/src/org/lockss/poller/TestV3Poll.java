/*
 * $Id: TestV3Poll.java,v 1.1.2.26 2004-12-16 23:30:44 dshr Exp $
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

  protected PeerIdentity[] voterID = null;
  String[] voterNames = {
    "1.1.1.1",
    "2.2.2.2",
    "3.3.3.3",
    "4.4.4.4",
    "5.5.5.5",
    "6.6.6.6",
    "7.7.7.7",
    "8.8.8.8",
    "9.9.9.9",
  };
  protected PeerIdentity pollerID;
  protected int[] msgType = {
    V3LcapMessage.MSG_POLL,
    V3LcapMessage.MSG_POLL_ACK,
    V3LcapMessage.MSG_POLL_PROOF,
    V3LcapMessage.MSG_VOTE,
    V3LcapMessage.MSG_REPAIR_REQ,
    V3LcapMessage.MSG_REPAIR_REP,
    V3LcapMessage.MSG_EVALUATION_RECEIPT,
  };
  protected V3Voter[] voter = null;
  protected V3Poller poller;
  protected PollSpec pollSpec;
  protected PollManager pollmanager;
  protected MockLcapStreamRouter router;
  protected MyMessageHandler handler = null;
  protected Hashtable pollHash = new Hashtable();
  protected Hashtable peerHash = new Hashtable();

  protected void setUp() throws Exception {
    super.setUp();
    pollHash = new Hashtable();
    TimeBase.setSimulated();

    log.debug("initRequiredServices()");
    initRequiredServices();

    testau.setPlugin(new MyMockPlugin());

    log.debug("initTestPeerIDs()");
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
    if (voter != null) {
      for (int i = 0; i < voter.length; i++) {
	if (voter[i] != null) {
	  pollmanager.removePoll(voter[i].getKey());
	}
      }
      voter = null;
    }
    if (poller != null) {
      pollmanager.removePoll(poller.getKey());
    }
    theDaemon.getPollManager().stopService();
    theDaemon.getPluginManager().stopService();
    TimeBase.setReal();
    super.tearDown();
  }

  // Tests

  public void testPollDuration() {
    log.debug("testPollDuration()");
    try {
      initTestPoll(1);
    } catch (Exception ex) {
      fail("initTestPoll threw " + ex.toString());
    }
    //  The two halves of the poll have been created but
    //  no time has elapsed
    assertTrue(voter[0] instanceof V3Voter);
    assertEquals("Poll " + voter[0] + " should be in Initializing",
		 V3Voter.STATE_INITIALIZING,
		 voter[0].getPollState());
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
    assertEquals("Poll " + voter[0] + " should be in Initializing",
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
      assertTrue("Poll " + voter[0] + " should be active",
		 pollmanager.isPollActive(key));
      assertFalse("Poll " + voter[0] + " should not be closed",
		  pollmanager.isPollClosed(key));
      assertFalse("Poll " + voter[0] + " should not be suspended",
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
    assertTrue("Poll " + voter[0] + " should be active",
	       pollmanager.isPollActive(key));
    assertFalse("Poll " + voter[0] + " should not be closed",
		pollmanager.isPollClosed(key));
    assertFalse("Poll " + voter[0] + " should not be suspended",
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
    assertFalse("Poll " + voter[0] + " should not be active",
		pollmanager.isPollActive(key));
    assertTrue("Poll " + voter[0] + " should be closed",
	       pollmanager.isPollClosed(key));
    assertFalse("Poll " + voter[0] + " should not be suspended",
		pollmanager.isPollSuspended(key));
    log.debug("About to tally");
    PollTally tally = poller.getVoteTally();
    assertNotNull(tally);
    List votes = tally.getPollVotes();
    assertTrue(votes.isEmpty());
  }

  public void testNormalPollWithOneAgreeVote() {
    log.debug("Starting testNormalPollWithOneAgreeVote()");
    try {
      initTestPoll(1);
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
    pollHash.put(voterID[0], voter[0]);
    //  The two halves of the poll have been created but
    //  no time has elapsed
    assertTrue(voter[0] instanceof V3Voter);
    assertEquals("Poll " + voter[0] + " should be in Initializing",
		 V3Voter.STATE_INITIALIZING,
		 voter[0].getPollState());
    assertTrue(poller instanceof V3Poller);
    assertEquals("Poll " + poller + " should be in Initializing",
		 V3Poller.STATE_INITIALIZING,
		 poller.getPollState());
    String key = poller.getKey();
    List peers = new ArrayList();
    peers.add(voterID[0]);
    Thread.yield();
    poller.solicitVotesFrom(peers);
    Thread.yield();
    int steps = 100;
    while (poller.getPollState() != V3Poller.STATE_FINALIZING ||
	   voter[0].getPollState() != V3Voter.STATE_FINALIZING) {
      Thread.yield();
      if (!stepTimeUntilPollStateChanges(poller, voter, "testing")) {
	break;
      }
      log.debug("poller state " + poller.getPollStateName(poller.getPollState()) +
		" voter state " + voter[0].getPollStateName(voter[0].getPollState()));
      steps--;
      assertTrue("Too many steps", steps > 0);
      Thread.yield();
    }
    PollTally pollerTally = poller.getVoteTally();
    assertEquals(pollerTally.getTallyResult(), Tallier.RESULT_NOQUORUM);
    PollTally voterTally = poller.getVoteTally();
    assertEquals(voterTally.getTallyResult(), Tallier.RESULT_NOQUORUM);
  }

  public void testNormalPollWithOneDisagreeVote() {
    log.debug("Starting testNormalPollWithOneDisagreeVote()");
    try {
      initTestPoll(1);
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
    es.setAgreeVoteResult(false);
    es.setVoteDuration(400);
    es.setVoteException(null);
    Thread.yield();
    pollHash.put(voterID[0], voter[0]);
    //  The two halves of the poll have been created but
    //  no time has elapsed
    assertTrue(voter[0] instanceof V3Voter);
    assertEquals("Poll " + voter[0] + " should be in Initializing",
		 V3Voter.STATE_INITIALIZING,
		 voter[0].getPollState());
    assertTrue(poller instanceof V3Poller);
    assertEquals("Poll " + poller + " should be in Initializing",
		 V3Poller.STATE_INITIALIZING,
		 poller.getPollState());
    String key = poller.getKey();
    List peers = new ArrayList();
    peers.add(voterID[0]);
    Thread.yield();
    poller.solicitVotesFrom(peers);
    Thread.yield();
    int steps = 100;
    while (poller.getPollState() != V3Poller.STATE_FINALIZING ||
	   voter[0].getPollState() != V3Voter.STATE_FINALIZING) {
      Thread.yield();
      if (!stepTimeUntilPollStateChanges(poller, voter, "testing")) {
	break;
      }
      log.debug("poller state " + poller.getPollStateName(poller.getPollState()) +
		" voter state " + voter[0].getPollStateName(voter[0].getPollState()));
      steps--;
      assertTrue("Too many steps", steps > 0);
      Thread.yield();
    }
    PollTally pollerTally = poller.getVoteTally();
    assertEquals(pollerTally.getTallyResult(), Tallier.RESULT_NOQUORUM);
    PollTally voterTally = poller.getVoteTally();
    assertEquals(voterTally.getTallyResult(), Tallier.RESULT_NOQUORUM);
  }

  public void testNormalPollWithSevenAgreeVotes() {
    log.debug("Starting testNormalPollWithSevenAgreeVotes()");
    try {
      initTestPoll(7);
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
    List peers = new ArrayList();
    for (int i = 0; i < voter.length && voter[i] != null; i++) {
      assertTrue(voter[i] instanceof V3Voter);
      assertEquals("Poll " + voter[i] + " should be in Initializing",
		   V3Voter.STATE_INITIALIZING,
		   voter[i].getPollState());
      peers.add(voterID[i]);
    }
    assertTrue(poller instanceof V3Poller);
    assertEquals("Poll " + poller + " should be in Initializing",
		 V3Poller.STATE_INITIALIZING,
		 poller.getPollState());
    Thread.yield();
    poller.solicitVotesFrom(peers);
    String key = poller.getKey();
    int steps = 200;
    while (poller.getDeadline().getRemainingTime() > 0) {
      log.debug("about to step time");
      Thread.yield();
      if (!stepTimeUntilPollStateChanges(poller, voter, "testing")) {
	log.debug("stepTimeUntilPollStateChanges returned false");
	break;
      }
      log.debug("poller state " +
		poller.getPollStateName(poller.getPollState()));
      for (int i = 0; i < voter.length; i++) {
	log.debug("voter " + i + " state " +
		  voter[i].getPollStateName(voter[i].getPollState()));
      }
      steps--;
      assertTrue("Too many steps", steps > 0);
      Thread.yield();
    }
    if (false) while (stepTimeUntilPollStateChanges(poller, voter, "waiting out")) {
      steps--;
      assertTrue("Too many steps", steps > 0);
      Thread.yield();
    }      
    PollTally pollerTally = poller.getVoteTally();
    assertEquals(pollerTally.getTallyResult(), Tallier.RESULT_WON);
  }

  //  Support methods

  private void initRequiredServices() {
    theDaemon = new MockLockssDaemon();
    if (false) {
      theDaemon.setIdentityManager(new MockIdentityManager());
    }
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
    log.debug("initTestPeerIDs()");
    voterID = new PeerIdentity[voterNames.length];
    for (int i = 0; i < voterNames.length; i++) {
      try{
	log.debug("make peer id " + i + " IP " + voterNames[i]);
	voterID[i] = idmgr.stringToPeerIdentity(voterNames[i]);
      } catch (IdentityManager.MalformedIdentityKeyException ex) {
	fail("can't open test host:" + ex);
      }
      assertFalse(voterID[i].isLocalIdentity());
    }
    try{
      pollerID = idmgr.stringToPeerIdentity("127.0.0.1");
    } catch (IdentityManager.MalformedIdentityKeyException ex) {
      fail("can't open test host:" + ex);
    }
    assertTrue(pollerID.isLocalIdentity());
  }

  private void initTestPoll(int numVoters) throws Exception {
    log.debug("initTestPoll()");
    voter = new V3Voter[numVoters];
    for (int i = 0; i < voter.length; i++) {
      voter[i] = null;
    }
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
    duration = 50000;
    log.debug("Duration is " + duration);
    byte[] challenge = pollmanager.makeVerifier(duration);
    pollSpec = spec;
    // Make the poller
    byte[] pollerChallenge = pollmanager.makeVerifier(duration);
    p = pollmanager.makePoll(pollSpec,
			     duration,
			     pollerChallenge,
			     null,
			     pollerID,
			     "SHA-1");
    log.debug("initTestPoll: V3 poller returns " + p + " peer " + pollerID);
    assertTrue(p instanceof V3Poller);
    poller = (V3Poller) p;
    for (int i = 0; i < voter.length; i++) {
      log.debug("make voter ID " + i + " IP " + voterID[i]);
      // Make a voter
      p = pollmanager.makePoll(pollSpec,
			       duration-10000,
			       challenge,
			       challenge,  // XXX how we signal to make a Voter
			       pollerID,
			       "SHA-1");
      log.debug("initTestPoll: V3 voter returns " + p + " peer " + voterID[i]);
      assertTrue(p instanceof V3Voter);
      voter[i] = (V3Voter) p;
      peerHash.put(voter[i], voterID[i]);
      pollHash.put(voterID[i], voter[i]);
      log.debug3("initTestPoll: voter " + p.toString());
    }
    peerHash.put(poller, pollerID);
    pollHash.put(pollerID, poller);
    log.debug3("initTestPoll: poller " + p.toString());
  }


  private boolean stepTimeUntilPollStateChanges(V3Poller poller,
					     V3Voter[] voter, String s) {
    int oldPollerState = poller.getPollState();
    int newPollerState = -1;
    int[] oldVoterState = new int[voter.length];
    for (int i = 0; i < oldVoterState.length; i++) {
      oldVoterState[i] = voter[i].getPollState();
    }
    if (oldPollerState == V3Poller.STATE_FINALIZING) {
      boolean goOn = false;
      for (int i = 0; i < oldVoterState.length; i++) {
	if (oldVoterState[i] != V3Voter.STATE_FINALIZING) {
	  log.debug("poll " + i + " still active " + voter[i]);
	  goOn = true;
	}
      }
      if (!goOn) {
	log.debug("Both sides are Finalizing");
	return false;
      }
    }
    int newVoterState = -1;
    long startTime = TimeBase.nowMs();
    boolean changed = false;
    log.debug(s + " poller " + poller + " voter " + voter);
    do {
      TimeBase.step(100);
      log.debug3("Step poller " +
		 poller.getPollStateName(poller.getPollState()) +
		 " voter[0] " +
		 voter[0].getPollStateName(voter[0].getPollState()));
      if ((newPollerState = poller.getPollState()) != oldPollerState) {
	changed = true;
	log.debug("Change from " + poller.getPollStateName(oldPollerState) +
		  " to " + poller.getPollStateName(newPollerState) +
		  " after " + (TimeBase.nowMs() - startTime) + "ms");
      } else {
	for (int i = 0; i < voter.length; i++) {
	  if ((newVoterState = voter[i].getPollState()) != oldVoterState[i]) {
	    changed = true;
	    log.debug("Change from " +
		      voter[i].getPollStateName(oldVoterState[i]) +
		      " to " + voter[i].getPollStateName(newVoterState) +
		      " after " + (TimeBase.nowMs() - startTime) + "ms");
	    break;
	  }
	}
      }
    } while (!changed);
    return true;
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
      PeerIdentity destination = msg.getDestinationID();
      assertNotNull(sender);
      assertNotNull(destination);
      assertNotNull(pollHash);
      V3Poll recipient = (V3Poll) pollHash.get(destination);
      assertNotNull(recipient);
      log.debug("Handing " + msg + " to peer " +
		(peerHash.get(recipient)).toString() + " poll " + recipient);
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
