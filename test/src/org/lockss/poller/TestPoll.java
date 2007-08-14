/*
 * $Id: TestPoll.java,v 1.99 2007-08-14 03:10:26 smorabito Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.app.*;
import org.lockss.config.ConfigManager;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.repository.LockssRepositoryImpl;

/** JUnitTest case for class: org.lockss.poller.Poll */
public class TestPoll extends LockssTestCase {
  private static Logger log = Logger.getLogger("TestPoll");
  private static String[] rootV1urls = {
    "http://www.test.org",
    "http://www.test1.org", "http://www.test2.org"};
  private static String lwrbnd = "test1.doc";
  private static String uprbnd = "test3.doc";
  private static long testduration = Constants.DAY;

  //   protected ArchivalUnit testau =
  //       PollTestPlugin.PTArchivalUnit.createFromListOfRootUrls(rootV1urls);
  protected MockArchivalUnit testau;
  private IdentityManager idmgr;
  private MockLockssDaemon theDaemon;
  private ArrayList agree_entries = makeEntries(10, 50);
  private ArrayList disagree_entries = makeEntries(15, 57);
  private ArrayList dissenting_entries = makeEntries(7, 50);

  protected PeerIdentity testID;
  protected PeerIdentity testID1;
  protected V1LcapMessage[] testV1msg;
  protected V1Poll[] testV1polls;
  protected PollManager pollmanager;

  protected void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated();

    initRequiredServices();

    testau.setPlugin(new MockPlugin());

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
    theDaemon.getDatagramRouterManager().stopService();
    theDaemon.getRouterManager().stopService();
    theDaemon.getSystemMetrics().stopService();
    TimeBase.setReal();
    for(int i=0; i<testV1msg.length; i++) {
      if (testV1msg[i] != null)
	pollmanager.removePoll(testV1msg[i].getKey());
    }
    super.tearDown();
  }

  /** test for method scheduleVote(..) */
  public void testScheduleVote() {
    V1Poll p = testV1polls[1];
    assertTrue(p instanceof V1ContentPoll);
    log.debug3("testScheduleVote 1");
    p.scheduleVote();
    log.debug3("testScheduleVote 2");
    assertNotNull(p.m_voteTime);
    assertTrue(p.m_voteTime.getRemainingTime()
               < p.m_deadline.getRemainingTime());
    log.debug3("at end of testScheduleVote");
  }

  /** test for method checkVote(..) */
  public void testCheckVote() throws Exception {
    V1LcapMessage msg = null;
    log.debug3("starting testCheeckVote");
    msg = V1LcapMessage.makeReplyMsg(testV1polls[0].getMessage(),
				     ByteArray.makeRandomBytes(20),
				     ByteArray.makeRandomBytes(20),
				     null,
				     V1LcapMessage.NAME_POLL_REP,
				     testduration,
				     testID);
    log.debug3("testCheeckVote 2");
    V1Poll p = null;
    p = createCompletedPoll(theDaemon, testau, msg, 8,2, pollmanager);
    assertTrue(p instanceof V1NamePoll);
    log.debug3("testCheeckVote 3");
    assertNotNull(p);
    PeerIdentity id = msg.getOriginatorId();
    assertNotNull(id);
    assertNotNull(p.m_tally);
    int rep = p.m_tally.wtAgree + idmgr.getReputation(id);

    // good vote check

    p.checkVote(msg.getHashed(), new Vote(msg, false));
    assertEquals(9, p.m_tally.numAgree);
    assertEquals(2, p.m_tally.numDisagree);
    assertEquals(rep, p.m_tally.wtAgree);

    rep = p.m_tally.wtDisagree + idmgr.getReputation(id);

    // bad vote check
    p.checkVote(ByteArray.makeRandomBytes(20), new Vote(msg, false));
    assertEquals(9, p.m_tally.numAgree);
    assertEquals(3, p.m_tally.numDisagree);
    assertEquals(rep, p.m_tally.wtDisagree);
  }

  /** test for method tally(..) */
  public void testTally() {
    V1Poll p = testV1polls[0];
    LcapMessage msg = p.getMessage();
    PeerIdentity id = msg.getOriginatorId();
    p.m_tally.addVote(new Vote(msg, false), id, false);
    p.m_tally.addVote(new Vote(msg, false), id, false);
    p.m_tally.addVote(new Vote(msg, false), id, false);
    assertEquals(0, p.m_tally.numAgree);
    assertEquals(0, p.m_tally.wtAgree);
    assertEquals(3, p.m_tally.numDisagree);
    assertEquals(1500, p.m_tally.wtDisagree);

    p = testV1polls[1];
    msg = p.getMessage();
    p.m_tally.addVote(new Vote(msg, true), id, false);
    p.m_tally.addVote(new Vote(msg, true), id, false);
    p.m_tally.addVote(new Vote(msg, true), id, false);
    assertEquals(3, p.m_tally.numAgree);
    assertEquals(1500, p.m_tally.wtAgree);
    assertEquals(0, p.m_tally.numDisagree);
    assertEquals(0, p.m_tally.wtDisagree);
  }

  public void testNamePollTally() throws Exception {
    V1NamePoll np;
    // test a name poll we won
    np = makeCompletedNamePoll(4,1,0);
    assertEquals(5, np.m_tally.numAgree);
    assertEquals(1, np.m_tally.numDisagree);
    assertEquals(Tallier.RESULT_WON, np.m_tally.getTallyResult());

    // test a name poll we lost with a dissenting vote
    np = makeCompletedNamePoll(1,8,1);

    assertEquals(2, np.m_tally.numAgree);
    assertEquals(9, np.m_tally.numDisagree);

    // build a master list
    np.buildPollLists(np.m_tally.pollVotes.iterator());

    // these should be different since we lost the poll
    assertFalse(CollectionUtil.isIsomorphic(np.m_tally.localEntries,
                                            np.m_tally.votedEntries));

    // the expected "correct" set is in our disagree msg
    assertTrue(CollectionUtil.isIsomorphic(disagree_entries,
                                           np.m_tally.votedEntries));
  }


  /** test for method vote(..) */
  public void testVote() {
    V1Poll p = testV1polls[1];
    p.m_hash = ByteArray.makeRandomBytes(20);
    try {
      p.castOurVote();
    }
    catch (IllegalStateException e) {
      // the socket isn't inited and should squack
    }
    p.m_pollstate = V1Poll.PS_COMPLETE;
  }

  /** test for method voteInPoll(..) */
  public void testVoteInPoll() {
    V1Poll p = testV1polls[1];
    p.m_tally.quorum = 10;
    p.m_tally.numAgree = 5;
    p.m_tally.numDisagree = 2;
    p.m_tally.wtAgree = 2000;
    p.m_tally.wtDisagree = 200;
    p.m_hash = ByteArray.makeRandomBytes(20);
    try {
      p.voteInPoll();
    }
    catch (IllegalStateException e) {
      // the socket isn't inited and should squack
    }

    p.m_tally.numAgree = 20;
    try {
      p.voteInPoll();
    }
    catch (NullPointerException npe) {
      // the socket isn't inited and should squack
    }
    p.m_pollstate = V1Poll.PS_COMPLETE;
  }

  public void testStartPoll() {
    V1Poll p = testV1polls[0];
    p.startPoll();
    assertEquals(V1Poll.PS_WAIT_HASH, p.m_pollstate);
    p.m_pollstate = V1Poll.PS_COMPLETE;
  }

  public void testScheduleOurHash() {
    V1Poll p = testV1polls[0];
    p.m_pollstate = V1Poll.PS_WAIT_HASH;
    // no time has elapsed - so we should be able to schedule our hash
    assertTrue(p.scheduleOurHash());
    // half the time has elapsed so we should be able to schedule our hash
    TimeBase.step(p.m_deadline.getRemainingTime()/2);
    assertTrue(p.scheduleOurHash());

    // all of the time has elapsed we should not be able to schedule our hash
    TimeBase.step(p.m_deadline.getRemainingTime()- 1000);
    assertFalse(p.scheduleOurHash());
    p.m_pollstate = V1Poll.PS_COMPLETE;

  }

  /** test for method stopPoll(..) */
  public void testStopPoll() {
    V1Poll p = testV1polls[1];
    p.m_tally.quorum = 10;
    p.m_tally.numAgree = 7;
    p.m_tally.numDisagree = 3;
    p.m_pollstate = V1Poll.PS_WAIT_TALLY;
    p.stopPoll();
    assertTrue(p.m_pollstate == V1Poll.PS_COMPLETE);
    p.startPoll();
    assertTrue(p.m_pollstate == V1Poll.PS_COMPLETE);
  }

  /** test for method startVoteCheck(..) */
  public void testStartVote() {
    V1Poll p = testV1polls[0];
    p.m_pendingVotes = 3;
    p.startVoteCheck();
    assertEquals(4, p.m_pendingVotes);
    p.m_pollstate = V1Poll.PS_COMPLETE;
  }

  /** test for method stopVote(..) */
  public void testStopVote() {
    V1Poll p = testV1polls[1];
    p.m_pendingVotes = 3;
    p.stopVoteCheck();
    assertEquals(2, p.m_pendingVotes);
    p.m_pollstate = V1Poll.PS_COMPLETE;
  }

  private V1NamePoll makeCompletedNamePoll(int numAgree,
					   int numDisagree,
					   int numDissenting)
      throws Exception {
    V1NamePoll np = null;
    V1LcapMessage agree_msg = null;
    V1LcapMessage disagree_msg1 = null;
    V1LcapMessage disagree_msg2 = null;

    Plugin plugin = testau.getPlugin();
    PollSpec spec =
      new MockPollSpec(testau,
		       rootV1urls[0],null,null, Poll.V1_NAME_POLL);
    ((MockCachedUrlSet)spec.getCachedUrlSet()).setHasContent(false);
    V1LcapMessage poll_msg =
      V1LcapMessage.makeRequestMsg(spec,
				   null,
				   ByteArray.makeRandomBytes(20),
				   ByteArray.makeRandomBytes(20),
				   V1LcapMessage.NAME_POLL_REQ,
				   testduration,
				   testID);

    // make our poll
    np = (V1NamePoll) new V1NamePoll(spec,
				     pollmanager,
				     poll_msg.getOriginatorId(),
				     poll_msg.getChallenge(),
				     poll_msg.getDuration(),
				     poll_msg.getHashAlgorithm());
    np.setMessage(poll_msg);

    // generate agree vote msg
    agree_msg = V1LcapMessage.makeReplyMsg(poll_msg,
					   ByteArray.makeRandomBytes(20),
					   poll_msg.getVerifier(),
					   agree_entries,
					   V1LcapMessage.NAME_POLL_REP,
					   testduration, testID);

    // generate a disagree vote msg
    disagree_msg1 = V1LcapMessage.makeReplyMsg(poll_msg,
					       ByteArray.makeRandomBytes(20),
					       ByteArray.makeRandomBytes(20),
					       disagree_entries,
					       V1LcapMessage.NAME_POLL_REP,
					       testduration, testID1);
    // generate a losing disagree vote msg
    disagree_msg2 = V1LcapMessage.makeReplyMsg(poll_msg,
					       ByteArray.makeRandomBytes(20),
					       ByteArray.makeRandomBytes(20),
					       dissenting_entries,
					       V1LcapMessage.NAME_POLL_REP,
					       testduration, testID1);


    // add our vote
    V1LcapMessage msg = (V1LcapMessage)(np.getMessage());
    PeerIdentity id = msg.getOriginatorId();
    np.m_tally.addVote(np.makeNameVote(msg, true), id, true);

    // add the agree votes
    id =agree_msg.getOriginatorId();
    for(int i = 0; i < numAgree; i++) {
      np.m_tally.addVote(np.makeNameVote(agree_msg, true), id, false);
    }

    // add the disagree votes
    id = disagree_msg1.getOriginatorId();
    for(int i = 0; i < numDisagree; i++) {
      np.m_tally.addVote(np.makeNameVote(disagree_msg1, false), id , false);
    }

    // add dissenting disagree vote
    id = disagree_msg2.getOriginatorId();
    for(int i = 0; i < numDissenting; i++) {
      np.m_tally.addVote(np.makeNameVote(disagree_msg2, false), id, false);
    }
    np.m_pollstate = V1Poll.PS_COMPLETE;
    np.m_tally.tallyVotes();
    return np;
  }
                                               
  public static V1Poll createCompletedPoll(LockssDaemon daemon,
					   ArchivalUnit au,
					   V1LcapMessage testmsg,
					   int numAgree,
					   int numDisagree,
					   PollManager pollmanager)
      throws Exception {
    log.debug("createCompletedPoll: au: " + au.toString() + " peer " +
	      testmsg.getOriginatorId() + " votes " + numAgree + "/" +
	      numDisagree);
    CachedUrlSetSpec cusSpec = null;
    if ((testmsg.getLwrBound()!=null) &&
        (testmsg.getLwrBound().equals(PollSpec.SINGLE_NODE_LWRBOUND))) {
      cusSpec = new SingleNodeCachedUrlSetSpec(testmsg.getTargetUrl());
    } else {
      cusSpec = new RangeCachedUrlSetSpec(testmsg.getTargetUrl(),
					  testmsg.getLwrBound(),
					  testmsg.getUprBound());
    }
    CachedUrlSet cus = au.makeCachedUrlSet(cusSpec);
    PollSpec spec = new PollSpec(cus, Poll.V1_CONTENT_POLL);
    ((MockCachedUrlSet)spec.getCachedUrlSet()).setHasContent(false);
    V1Poll p = null;
    if (testmsg.isContentPoll()) {
      p = new V1ContentPoll(spec,
			    pollmanager,
			    testmsg.getOriginatorId(),
			    testmsg.getChallenge(),
			    testmsg.getDuration(),
			    testmsg.getHashAlgorithm());
    } else if (testmsg.isNamePoll()) {
      p = new V1NamePoll(spec,
			 pollmanager,
			 testmsg.getOriginatorId(),
			 testmsg.getChallenge(),
			 testmsg.getDuration(),
			 testmsg.getHashAlgorithm());
    } else if (testmsg.isVerifyPoll()) {
      p = new V1VerifyPoll(spec,
			   pollmanager,
			   testmsg.getOriginatorId(),
			   testmsg.getChallenge(),
			   testmsg.getDuration(),
			   testmsg.getHashAlgorithm(),
			   testmsg.getVerifier());
    }
    assertNotNull(p);
    p.setMessage(testmsg);
    p.m_tally.quorum = numAgree + numDisagree;
    p.m_tally.numAgree = numAgree;
    p.m_tally.numDisagree = numDisagree;
    p.m_tally.wtAgree = 2000;
    p.m_tally.wtDisagree = 200;
    p.m_tally.localEntries = makeEntries(1,3);
    p.m_tally.votedEntries = makeEntries(1,5);
    p.m_tally.votedEntries.remove(1);
    p.m_pollstate = V1Poll.PS_COMPLETE;
    p.m_callerID = testmsg.getOriginatorId();
    log.debug3("poll " + p.toString());
    p.m_tally.tallyVotes();
    return p;
  }


  public static ArrayList makeEntries(int firstEntry, int lastEntry) {
    int numEntries = lastEntry - firstEntry + 1;
    ArrayList ret_arry = new ArrayList(numEntries);

    for(int i=0; i < numEntries; i++) {
      String name = "/testentry" + (firstEntry + i) + ".html";
      ret_arry.add(new PollTally.NameListEntry(i%2 == 1, name));
    }

    return ret_arry;
  }

  private void initRequiredServices() {
    theDaemon = getMockLockssDaemon();
    pollmanager = new LocalPollManager();
    pollmanager.initService(theDaemon);
    theDaemon.setPollManager(pollmanager);

    theDaemon.getPluginManager();
    testau = PollTestPlugin.PTArchivalUnit.createFromListOfRootUrls(rootV1urls);
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
    p.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    p.setProperty(ConfigManager.PARAM_NEW_SCHEDULER, "false");
    // XXX we need to disable verification of votes because the
    // voter isn't really there
    p.setProperty(V1Poll.PARAM_AGREE_VERIFY, "0");
    p.setProperty(V1Poll.PARAM_DISAGREE_VERIFY, "0");
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

  private void initTestPeerIDs() {
    try {
      testID = idmgr.stringToPeerIdentity("127.0.0.1");
      testID1 = idmgr.stringToPeerIdentity("1.1.1.1");
    }
    catch (IdentityManager.MalformedIdentityKeyException ex) {
      fail("can't open test host");
    }
  }

  private void initTestMsg() throws Exception {
    testV1msg = new V1LcapMessage[3];
    int[] pollType = {
      Poll.V1_NAME_POLL,
      Poll.V1_CONTENT_POLL,
      Poll.V1_VERIFY_POLL,
    };
    PollFactory ppf = pollmanager.getPollFactory(1);
    assertNotNull("PollFactory should not be null", ppf);
    // XXX V1 support mandatory
    assertTrue(ppf instanceof V1PollFactory);
    V1PollFactory pf = (V1PollFactory)ppf;

    for (int i= 0; i<testV1msg.length; i++) {
      PollSpec spec = new MockPollSpec(testau, rootV1urls[i],
				       lwrbnd, uprbnd, pollType[i]);
      log.debug("Created poll spec: " + spec);
      ((MockCachedUrlSet)spec.getCachedUrlSet()).setHasContent(false);
      int opcode = V1LcapMessage.NAME_POLL_REQ + (i * 2);
      long duration = -1;
      //  NB calcDuration is not applied to Verify polls.
      switch (opcode) {
      case V1LcapMessage.NAME_POLL_REQ:
      case V1LcapMessage.CONTENT_POLL_REQ:
	// this will attempt to schedule and can return -1
	duration = Math.max(pf.calcDuration(spec, pollmanager), 1000);
	break;
      case V1LcapMessage.VERIFY_POLL_REQ:
      case V1LcapMessage.VERIFY_POLL_REP:
        duration = 100000; // Arbitrary
	break;
      default:
	fail("Bad opcode " + opcode);
	break;
      }

      testV1msg[i] =
	V1LcapMessage.makeRequestMsg(spec,
				     agree_entries,
				     pf.makeVerifier(100000),
				     pf.makeVerifier(100000),
				     opcode,
				     duration,
				     testID);
      assertNotNull(testV1msg[i]);
    }

  }

  private void initTestPolls() throws Exception {
    testV1polls = new V1Poll[testV1msg.length];
    for (int i = 0; i < testV1polls.length; i++) {
      log.debug3("initTestPolls: V1 " + i);
      BasePoll p = pollmanager.makePoll(testV1msg[i]);
      assertNotNull(p);
      assertNotNull(p.getMessage());
      log.debug("initTestPolls: V1 " + i + " returns " + p);
      assertTrue(p instanceof V1Poll);
      switch (i) {
      case 0:
	assertTrue(p instanceof V1NamePoll);
	break;
      case 1:
	assertTrue(p instanceof V1ContentPoll);
	break;
      case 2:
	assertTrue(p instanceof V1VerifyPoll);
	break;
      }
      testV1polls[i] = (V1Poll)p;
      assertNotNull(testV1polls[i]);
      log.debug3("initTestPolls: " + i + " " + p.toString());
    }
  }

  static class LocalPollManager extends PollManager {
    // ignore message sends
    public void sendMessage(V1LcapMessage msg, ArchivalUnit au)
        throws IOException {
    }
  }

  /** Executes the test case
   * @param argv array of Strings containing command line arguments
   * */
  public static void main(String[] argv) {
    String[] testCaseList = {
      TestPoll.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
