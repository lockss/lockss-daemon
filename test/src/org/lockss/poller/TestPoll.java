/*
 * $Id: TestPoll.java,v 1.78 2004-09-13 04:02:24 dshr Exp $
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
import java.net.*;
import org.lockss.app.*;
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
  private static String[] rootV2urls = {
      "http://www.test.net",
      "http://www.test1.net", "http://www.test2.net"};
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

  protected IPAddr testaddr;
  protected IPAddr testaddr1;
  protected LcapIdentity testID;
  protected LcapIdentity testID1;
  protected LcapMessage[] testV1msg;
  protected LcapMessage[] testV2msg;
  protected V1Poll[] testV1polls;
  protected V2Poll[] testV2polls;
  protected PollManager pollmanager;

  protected void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated();

    initRequiredServices();

    testau.setPlugin(new MyMockPlugin());

    initTestAddr();
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
    theDaemon.getRouterManager().stopService();
    theDaemon.getSystemMetrics().stopService();
    TimeBase.setReal();
    for(int i=0; i<testV1msg.length; i++) {
      if (testV1msg[i] != null)
	pollmanager.removePoll(testV1msg[i].getKey());
    }
    for(int i=0; i<testV2msg.length; i++) {
      if (testV2msg[i] != null)
	pollmanager.removePoll(testV2msg[i].getKey());
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
    LcapMessage msg = null;
    log.debug3("starting testCheeckVote");
    msg = LcapMessage.makeReplyMsg(testV1polls[0].getMessage(),
				   pollmanager.generateRandomBytes(),
				   pollmanager.generateRandomBytes(),
				   null,
				   LcapMessage.NAME_POLL_REP,
				   testduration,
				   testID);
    log.debug3("testCheeckVote 2");
    V1Poll p = null;
    p = createCompletedPoll(theDaemon, testau, msg, 8,2);
    assertTrue(p instanceof V1NamePoll);
    log.debug3("testCheeckVote 3");
    assertNotNull(p);
    LcapIdentity id = idmgr.findIdentity(msg.getOriginatorID());
    assertNotNull(id);
    assertNotNull(p.m_tally);
    int rep = p.m_tally.wtAgree + id.getReputation();

    // good vote check

    p.checkVote(msg.getHashed(), new Vote(msg, false));
    assertEquals(9, p.m_tally.numAgree);
    assertEquals(2, p.m_tally.numDisagree);
    assertEquals(rep, p.m_tally.wtAgree);

    rep = p.m_tally.wtDisagree + id.getReputation();

    // bad vote check
    p.checkVote(pollmanager.generateRandomBytes(), new Vote(msg, false));
    assertEquals(9, p.m_tally.numAgree);
    assertEquals(3, p.m_tally.numDisagree);
    assertEquals(rep, p.m_tally.wtDisagree);
  }

  /** test for method tally(..) */
  public void testTally() {
    V1Poll p = testV1polls[0];
    LcapMessage msg = p.getMessage();
    LcapIdentity id = idmgr.findIdentity(msg.getOriginatorID());
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
    assertEquals(Tallier.RESULT_LOST, np.m_tally.getTallyResult());

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
    p.m_hash = pollmanager.generateRandomBytes();
    try {
      p.castOurVote();
    }
    catch (IllegalStateException e) {
      // the socket isn't inited and should squack
    }
    p.m_pollstate = BasePoll.PS_COMPLETE;
  }

  /** test for method voteInPoll(..) */
  public void testVoteInPoll() {
    V1Poll p = testV1polls[1];
    p.m_tally.quorum = 10;
    p.m_tally.numAgree = 5;
    p.m_tally.numDisagree = 2;
    p.m_tally.wtAgree = 2000;
    p.m_tally.wtDisagree = 200;
    p.m_hash = pollmanager.generateRandomBytes();
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
    p.m_pollstate = BasePoll.PS_COMPLETE;
  }

  public void testStartPoll() {
    V1Poll p = testV1polls[0];
    p.startPoll();
    assertEquals(BasePoll.PS_WAIT_HASH, p.m_pollstate);
    p.m_pollstate = BasePoll.PS_COMPLETE;
  }

  public void testScheduleOurHash() {
    V1Poll p = testV1polls[0];
    p.m_pollstate = BasePoll.PS_WAIT_HASH;
    // no time has elapsed - so we should be able to schedule our hash
    assertTrue(p.scheduleOurHash());
    // half the time has elapsed so we should be able to schedule our hash
    TimeBase.step(p.m_deadline.getRemainingTime()/2);
    assertTrue(p.scheduleOurHash());

    // all of the time has elapsed we should not be able to schedule our hash
    TimeBase.step(p.m_deadline.getRemainingTime()- 1000);
    assertFalse(p.scheduleOurHash());
    p.m_pollstate = BasePoll.PS_COMPLETE;

  }

  /** test for method stopPoll(..) */
  public void testStopPoll() {
    V1Poll p = testV1polls[1];
    p.m_tally.quorum = 10;
    p.m_tally.numAgree = 7;
    p.m_tally.numDisagree = 3;
    p.m_pollstate = BasePoll.PS_WAIT_TALLY;
    p.stopPoll();
    assertTrue(p.m_pollstate == BasePoll.PS_COMPLETE);
    p.startPoll();
    assertTrue(p.m_pollstate == BasePoll.PS_COMPLETE);
  }

  /** test for method startVoteCheck(..) */
  public void testStartVote() {
    V1Poll p = testV1polls[0];
    p.m_pendingVotes = 3;
    p.startVoteCheck();
    assertEquals(4, p.m_pendingVotes);
    p.m_pollstate = BasePoll.PS_COMPLETE;
  }

  /** test for method stopVote(..) */
  public void testStopVote() {
    V1Poll p = testV1polls[1];
    p.m_pendingVotes = 3;
    p.stopVoteCheck();
    assertEquals(2, p.m_pendingVotes);
    p.m_pollstate = BasePoll.PS_COMPLETE;
  }

  private V1NamePoll makeCompletedNamePoll(int numAgree,
					   int numDisagree,
					   int numDissenting)
      throws Exception {
    V1NamePoll np = null;
    LcapMessage agree_msg = null;
    LcapMessage disagree_msg1 = null;
    LcapMessage disagree_msg2 = null;

    Plugin plugin = testau.getPlugin();
    PollSpec spec =
      new MockPollSpec(testau,
		       rootV1urls[0],null,null);
    ((MockCachedUrlSet)spec.getCachedUrlSet()).setHasContent(false);
    LcapMessage poll_msg =
      LcapMessage.makeRequestMsg(spec,
				 null,
				 pollmanager.generateRandomBytes(),
				 pollmanager.generateRandomBytes(),
				 LcapMessage.NAME_POLL_REQ,
				 testduration,
				 testID);

    // make our poll
    np = (V1NamePoll) pollmanager.createPoll(poll_msg, spec);

    // generate agree vote msg
    agree_msg = LcapMessage.makeReplyMsg(poll_msg,
					 pollmanager.generateRandomBytes(),
					 poll_msg.getVerifier(),
					 agree_entries,
					 LcapMessage.NAME_POLL_REP,
					 testduration, testID);

    // generate a disagree vote msg
    disagree_msg1 = LcapMessage.makeReplyMsg(poll_msg,
					     pollmanager.generateRandomBytes(),
					     pollmanager.generateRandomBytes(),
					     disagree_entries,
					     LcapMessage.NAME_POLL_REP,
					     testduration, testID1);
    // generate a losing disagree vote msg
    disagree_msg2 = LcapMessage.makeReplyMsg(poll_msg,
					     pollmanager.generateRandomBytes(),
					     pollmanager.generateRandomBytes(),
					     dissenting_entries,
					     LcapMessage.NAME_POLL_REP,
					     testduration, testID1);


    // add our vote
    LcapMessage msg = np.getMessage();
    LcapIdentity id = idmgr.findIdentity(msg.getOriginatorID());
    np.m_tally.addVote(np.makeNameVote(msg, true), id, true);

    // add the agree votes
    id =idmgr.findIdentity(agree_msg.getOriginatorID());
    for(int i = 0; i < numAgree; i++) {
      np.m_tally.addVote(np.makeNameVote(agree_msg, true), id, false);
    }

    // add the disagree votes
    id =idmgr.findIdentity(disagree_msg1.getOriginatorID());
    for(int i = 0; i < numDisagree; i++) {
      np.m_tally.addVote(np.makeNameVote(disagree_msg1, false), id , false);
    }

    // add dissenting disagree vote
    id =idmgr.findIdentity(disagree_msg2.getOriginatorID());
    for(int i = 0; i < numDissenting; i++) {
      np.m_tally.addVote(np.makeNameVote(disagree_msg2, false), id, false);
    }
    np.m_pollstate = BasePoll.PS_COMPLETE;
    np.m_tally.tallyVotes();
    return np;
  }

  public static V1Poll createCompletedPoll(LockssDaemon daemon,
					   ArchivalUnit au,
					   LcapMessage testmsg,
					   int numAgree,
					   int numDisagree)
      throws Exception {
    log.debug("daemon = " + daemon);
    CachedUrlSetSpec cusSpec = null;
    if ((testmsg.getLwrBound()!=null) &&
        (testmsg.getLwrBound().equals(PollSpec.SINGLE_NODE_LWRBOUND))) {
      cusSpec = new SingleNodeCachedUrlSetSpec(testmsg.getTargetUrl());
    } else {
      cusSpec = new RangeCachedUrlSetSpec(testmsg.getTargetUrl(),
                                       testmsg.getLwrBound(),
                                       testmsg.getUprBound());
    }
    Plugin plugin = au.getPlugin();
    CachedUrlSet cus = plugin.makeCachedUrlSet(au, cusSpec);
    log.debug3("createCompletedPoll 1");
    PollSpec spec = new PollSpec(cus);
    log.debug3("createCompletedPoll 1");
    ((MockCachedUrlSet)spec.getCachedUrlSet()).setHasContent(false);
    log.debug3("createCompletedPoll 1");
    BasePoll pp = daemon.getPollManager().createPoll(testmsg, spec);
    log.debug3("createCompletedPoll 1");
    assertNotNull(pp);
    assertTrue(pp instanceof V1Poll);
    V1Poll p = (V1Poll) pp;
    p.m_tally.quorum = numAgree + numDisagree;
    p.m_tally.numAgree = numAgree;
    p.m_tally.numDisagree = numDisagree;
    p.m_tally.wtAgree = 2000;
    p.m_tally.wtDisagree = 200;
    p.m_tally.localEntries = makeEntries(1,3);
    p.m_tally.votedEntries = makeEntries(1,5);
    p.m_tally.votedEntries.remove(1);
    p.m_pollstate = BasePoll.PS_COMPLETE;
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
    theDaemon = new MockLockssDaemon();
    pollmanager = theDaemon.getPollManager();

    theDaemon.getPluginManager();
    testau = PollTestPlugin.PTArchivalUnit.createFromListOfRootUrls(rootV1urls);
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
    //theDaemon.getSchedService().startService();
    theDaemon.getHashService().startService();
    theDaemon.getRouterManager().startService();
    theDaemon.getSystemMetrics().startService();
    theDaemon.getActivityRegulator(testau).startService();
    theDaemon.setNodeManager(new MockNodeManager(), testau);
    pollmanager.startService();
  }

  private void initTestAddr() {
    try {
      testaddr = IPAddr.getByName("127.0.0.1");
      testID = theDaemon.getIdentityManager().findIdentity(testaddr, 0);
      testaddr1 = IPAddr.getByName("1.1.1.1");
      testID1 = theDaemon.getIdentityManager().findIdentity(testaddr1, 0);
    }
    catch (UnknownHostException ex) {
      fail("can't open test host");
    }
  }

  private void initTestMsg() throws Exception {
    testV1msg = new LcapMessage[3];

    for (int i= 0; i<testV1msg.length; i++) {
      PollSpec spec = new MockPollSpec(testau, rootV1urls[i],
				       lwrbnd, uprbnd);
      ((MockCachedUrlSet)spec.getCachedUrlSet()).setHasContent(false);
      int opcode = LcapMessage.NAME_POLL_REQ + (i * 2);
      testV1msg[i] =
	LcapMessage.makeRequestMsg(spec,
				   agree_entries,
				   pollmanager.generateRandomBytes(),
				   pollmanager.generateRandomBytes(),
				   opcode,
				   pollmanager.calcDuration(opcode,spec.getCachedUrlSet()),
				   testID);
    }

    testV2msg = new LcapMessage[2];

    for (int i= 0; i<testV2msg.length; i++) {
      PollSpec spec = new MockPollSpec(testau, rootV2urls[i],
				       lwrbnd, uprbnd);
      ((MockCachedUrlSet)spec.getCachedUrlSet()).setHasContent(false);
      // XXX - should have specific way to make V2 messages
      // XXX - should have separate opcodes for V2 messages
      int opcode = LcapMessage.NAME_POLL_REQ + (i * 2);
      testV2msg[i] =
	LcapMessage.makeRequestMsg(spec,
				   agree_entries,
				   pollmanager.generateRandomBytes(),
				   pollmanager.generateRandomBytes(),
				   opcode,
				   pollmanager.calcDuration(opcode,spec.getCachedUrlSet()),
				   testID);
      testV2msg[i].setPollVersion(2);
    }
  }

  private void initTestPolls() throws Exception {
    testV1polls = new V1Poll[testV1msg.length];
    for (int i = 0; i < testV1polls.length; i++) {
      log.debug3("initTestPolls: V1 " + i);
      BasePoll p = pollmanager.makePoll(testV1msg[i]);
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
    testV2polls = new V2Poll[testV2msg.length];
    for (int i = 0; i < testV2msg.length; i++) {
      log.debug3("initTestPolls: V2 " + i);
      BasePoll p = pollmanager.makePoll(testV2msg[i]);
      assertTrue(p instanceof V2Poll);
      switch (i) {
      case 0:
	assertTrue(p instanceof V2NamePoll);
	break;
      case 1:
	assertTrue(p instanceof V2ContentPoll);
	break;
      }
      testV2polls[i] = (V2Poll)p;
      assertNotNull(testV2polls[i]);
      log.debug3("initTestPolls: " + i + " " + p.toString());
    }
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
        TestPoll.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
