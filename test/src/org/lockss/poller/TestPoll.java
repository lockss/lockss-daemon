/*
 * $Id: TestPoll.java,v 1.60 2003-06-20 22:34:54 claire Exp $
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
import java.security.*;
import java.util.*;
import java.net.*;
import gnu.regexp.*;
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.repository.LockssRepositoryServiceImpl;

/** JUnitTest case for class: org.lockss.poller.Poll */
public class TestPoll extends LockssTestCase {
  private static Logger log = Logger.getLogger("TestPoll");
  private static String[] rooturls = {
      "http://www.test.org",
      "http://www.test1.org", "http://www.test2.org"};
  private static String lwrbnd = "test1.doc";
  private static String uprbnd = "test3.doc";
  private static long testduration = Constants.DAY;

  protected ArchivalUnit testau =
      PollTestPlugin.PTArchivalUnit.createFromListOfRootUrls(rooturls);
  private IdentityManager idmgr;
  private MockLockssDaemon theDaemon;
  private ArrayList agree_entries = makeEntries(10, 50);
  private ArrayList disagree_entries = makeEntries(15, 57);
  private ArrayList dissenting_entries = makeEntries(7, 50);

  protected InetAddress testaddr;
  protected InetAddress testaddr1;
  protected LcapIdentity testID;
  protected LcapIdentity testID1;
  protected LcapMessage[] testmsg;
  protected VersionOnePoll[] testpolls;
  protected PollManager pollmanager;

  protected void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated();

    initRequiredServices();
    initTestAddr();
    initTestMsg();
    initTestPolls();
  }

  /** tearDown method for test case
   * @throws Exception if removePoll failed
   */
  public void tearDown() throws Exception {
    pollmanager.stopService();
    theDaemon.getHashService().stopService();
    theDaemon.getLockssRepositoryService().stopService();
    theDaemon.getRouterManager().stopService();
    theDaemon.getSystemMetrics().stopService();
    TimeBase.setReal();
    for(int i=0; i<3; i++) {
      pollmanager.removePoll(testmsg[i].getKey());
    }
    super.tearDown();
  }

  /** test for method scheduleVote(..) */
  public void testScheduleVote() {
    VersionOnePoll p = testpolls[1];
    assertTrue(p instanceof VersionOneContentPoll);
    log.warning("testScheduleVote 1");
    p.scheduleVote();
    log.warning("testScheduleVote 2");
    assertNotNull(p.m_voteTime);
    assertTrue(p.m_voteTime.getRemainingTime()
               < p.m_deadline.getRemainingTime());
    log.warning("at end of testScheduleVote");
  }

  /** test for method checkVote(..) */
  public void testCheckVote() {
    LcapMessage msg = null;
    log.warning("starting testCheeckVote");
    try {
      msg = LcapMessage.makeReplyMsg(
          testpolls[0].getMessage(),
          pollmanager.generateRandomBytes(),
          pollmanager.generateRandomBytes(),
          null,
          LcapMessage.NAME_POLL_REP,
          testduration,
          testID);
    }
    catch (IOException ex1) {
    }
    log.warning("testCheeckVote 2");
    VersionOnePoll p = null;
    try {
      p = createCompletedPoll(theDaemon, testau, msg, 8,2);
      assertTrue(p instanceof VersionOneNamePoll);
    }
    catch (Exception ex2) {
      assertFalse(true);
    }
    log.warning("testCheeckVote 3");
    assertNotNull(p);
    LcapIdentity id = idmgr.findIdentity(msg.getOriginAddr());
    assertNotNull(id);
    assertNotNull(p.m_tally);
    int rep = p.m_tally.wtAgree + id.getReputation();

    // good vote check
    try {
      p.checkVote(msg.getHashed(), new Vote(msg, false));
    }
    catch (IllegalStateException ex) {
      // unitialized comm
    }

    assertEquals(9, p.m_tally.numAgree);
    assertEquals(2, p.m_tally.numDisagree);
    assertEquals(rep, p.m_tally.wtAgree);

    rep = p.m_tally.wtDisagree + id.getReputation();
    // bad vote check
    try {
      p.checkVote(pollmanager.generateRandomBytes(), new Vote(msg, false));
    }
    catch (IllegalStateException ex) {
      // unitialized comm
    }
    assertEquals(9, p.m_tally.numAgree);
    assertEquals(3, p.m_tally.numDisagree);
    assertEquals(rep, p.m_tally.wtDisagree);
  }

  /** test for method tally(..) */
  public void testTally() {
    VersionOnePoll p = testpolls[0];
    LcapMessage msg = p.getMessage();
    LcapIdentity id = idmgr.findIdentity(msg.getOriginAddr());
    p.m_tally.addVote(new Vote(msg, false), id, false);
    p.m_tally.addVote(new Vote(msg, false), id, false);
    p.m_tally.addVote(new Vote(msg, false), id, false);
    assertEquals(0, p.m_tally.numAgree);
    assertEquals(0, p.m_tally.wtAgree);
    assertEquals(3, p.m_tally.numDisagree);
    assertEquals(1500, p.m_tally.wtDisagree);

    p = testpolls[1];
    msg = p.getMessage();
    p.m_tally.addVote(new Vote(msg, true), id, false);
    p.m_tally.addVote(new Vote(msg, true), id, false);
    p.m_tally.addVote(new Vote(msg, true), id, false);
    assertEquals(3, p.m_tally.numAgree);
    assertEquals(1500, p.m_tally.wtAgree);
    assertEquals(0, p.m_tally.numDisagree);
    assertEquals(0, p.m_tally.wtDisagree);
  }


  public void testNamePollTally() {
    VersionOneNamePoll np;
    // test a name poll we won
    np = makeCompletedNamePoll(4,1,0);
    assertEquals(5, np.m_tally.numAgree);
    assertEquals(1, np.m_tally.numDisagree);

    assertEquals(PollTally.STATE_WON, np.m_tally.getStatus());

    // test a name poll we lost with a dissenting vote
    np = makeCompletedNamePoll(1,8,1);

    assertEquals(2, np.m_tally.numAgree);
    assertEquals(9, np.m_tally.numDisagree);
    assertEquals(PollTally.STATE_LOST, np.m_tally.getStatus());

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
    VersionOnePoll p = testpolls[1];
    p.m_hash = pollmanager.generateRandomBytes();
    try {
      p.castOurVote();
    }
    catch (IllegalStateException e) {
      // the socket isn't inited and should squack
    }
    p.m_pollstate = Poll.PS_COMPLETE;
  }

  /** test for method voteInPoll(..) */
  public void testVoteInPoll() {
    VersionOnePoll p = testpolls[1];
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
    p.m_pollstate = Poll.PS_COMPLETE;
  }

  public void testStartPoll() {
    VersionOnePoll p = testpolls[0];
    p.startPoll();
    assertEquals(Poll.PS_WAIT_HASH, p.m_pollstate);
    p.m_pollstate = Poll.PS_COMPLETE;
  }

  public void testScheduleOurHash() {
    VersionOnePoll p = testpolls[0];
    p.m_pollstate = Poll.PS_WAIT_HASH;
    assertTrue(p.scheduleOurHash());
    TimeBase.step(p.m_deadline.getRemainingTime()/2);
    assertTrue(p.scheduleOurHash());
    TimeBase.step(p.m_deadline.getRemainingTime()- 1000);
    assertFalse(p.scheduleOurHash());
    p.m_pollstate = Poll.PS_COMPLETE;

  }

  /** test for method stopPoll(..) */
  public void testStopPoll() {
    VersionOnePoll p = testpolls[1];
    p.m_tally.quorum = 10;
    p.m_tally.numAgree = 7;
    p.m_tally.numDisagree = 3;
    p.m_pollstate = Poll.PS_WAIT_TALLY;
    p.stopPoll();
    assertTrue(p.m_pollstate == Poll.PS_COMPLETE);
    p.startPoll();
    assertTrue(p.m_pollstate == Poll.PS_COMPLETE);
  }

  /** test for method startVoteCheck(..) */
  public void testStartVote() {
    VersionOnePoll p = testpolls[0];
    p.m_pendingVotes = 3;
    p.startVoteCheck();
    assertEquals(4, p.m_pendingVotes);
    p.m_pollstate = Poll.PS_COMPLETE;
  }

  /** test for method stopVote(..) */
  public void testStopVote() {
    VersionOnePoll p = testpolls[1];
    p.m_pendingVotes = 3;
    p.stopVoteCheck();
    assertEquals(2, p.m_pendingVotes);
    p.m_pollstate = Poll.PS_COMPLETE;
  }

  private VersionOneNamePoll makeCompletedNamePoll(int numAgree,
                                     int numDisagree,
                                     int numDissenting) {
    VersionOneNamePoll np = null;
    LcapMessage agree_msg = null;
    LcapMessage disagree_msg1 = null;
    LcapMessage disagree_msg2 = null;

    try {
      PollSpec spec =
	new PollSpec(testau.getAUId(),
		     rooturls[0],null,null,
		     testau.makeCachedUrlSet(new RangeCachedUrlSetSpec(rooturls[0])));
    ((MockCachedUrlSet)spec.getCachedUrlSet()).setHasContent(false);
      LcapMessage poll_msg = LcapMessage.makeRequestMsg(
          spec,
          null,
          pollmanager.generateRandomBytes(),
          pollmanager.generateRandomBytes(),
          LcapMessage.NAME_POLL_REQ,
          testduration,
          testID);

      // make our poll
      np = (VersionOneNamePoll) pollmanager.createPoll(poll_msg, spec);

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

    }
    catch (IOException ex) {
      fail("unable to generate a name poll reply");
    }

    // add our vote
    LcapMessage msg = np.getMessage();
    LcapIdentity id = idmgr.findIdentity(msg.getOriginAddr());
    np.m_tally.addVote(np.makeVote(msg, true), id, true);

    // add the agree votes
    id =idmgr.findIdentity(agree_msg.getOriginAddr());
    for(int i = 0; i < numAgree; i++) {
      np.m_tally.addVote(np.makeVote(agree_msg, true), id, false);
    }

    // add the disagree votes
    id =idmgr.findIdentity(disagree_msg1.getOriginAddr());
    for(int i = 0; i < numDisagree; i++) {
      np.m_tally.addVote(np.makeVote(disagree_msg1, false), id , false);
    }

    // add dissenting disagree vote
    id =idmgr.findIdentity(disagree_msg2.getOriginAddr());
    for(int i = 0; i < numDissenting; i++) {
      np.m_tally.addVote(np.makeVote(disagree_msg2, false), id, false);
    }
    np.m_pollstate = Poll.PS_COMPLETE;
    np.m_tally.tallyVotes();
    return np;
  }


  public static VersionOnePoll createCompletedPoll(LockssDaemon daemon,
                                         ArchivalUnit au,
					 LcapMessage testmsg, int numAgree,
                                         int numDisagree) throws Exception {
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
    CachedUrlSet cus = au.makeCachedUrlSet(cusSpec);
    log.warning("createCompletedPoll 1");
    PollSpec spec = new PollSpec(cus);
    log.warning("createCompletedPoll 1");
    ((MockCachedUrlSet)spec.getCachedUrlSet()).setHasContent(false);
    log.warning("createCompletedPoll 1");
    Poll pp = daemon.getPollManager().createPoll(testmsg, spec);
    log.warning("createCompletedPoll 1");
    assertNotNull(pp);
    assertTrue(pp instanceof VersionOnePoll);
    VersionOnePoll p = (VersionOnePoll) pp;
    p.m_tally.quorum = numAgree + numDisagree;
    p.m_tally.numAgree = numAgree;
    p.m_tally.numDisagree = numDisagree;
    p.m_tally.wtAgree = 2000;
    p.m_tally.wtDisagree = 200;
    p.m_tally.localEntries = makeEntries(1,3);
    p.m_tally.votedEntries = makeEntries(1,5);
    p.m_tally.votedEntries.remove(1);
    p.m_pollstate = Poll.PS_COMPLETE;
    log.warning("poll " + p.toString());
    try {
      p.m_tally.tallyVotes();
    } catch (Exception e) {
      log.warning("createCompletedPoll: tallyVotes threw " + e.toString());
      throw e;
    }
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
    testau = PollTestPlugin.PTArchivalUnit.createFromListOfRootUrls(rooturls);
    PluginUtil.registerArchivalUnit(testau);

    String tempDirPath = null;
    try {
      tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    }
    catch (IOException ex) {
      fail("unable to create a temporary directory");
    }

    String cacheStr = LockssRepositoryServiceImpl.PARAM_CACHE_LOCATION +"=" +
        tempDirPath;
    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(LockssRepositoryServiceImpl.PARAM_CACHE_LOCATION,
		  tempDirPath);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    idmgr = theDaemon.getIdentityManager();
    theDaemon.getHashService().startService();
    theDaemon.getLockssRepositoryService().startService();
    theDaemon.getRouterManager().startService();
    theDaemon.getSystemMetrics().startService();
    theDaemon.setNodeManagerService(new MockNodeManagerService());
    theDaemon.setNodeManager(new MockNodeManager(),testau);
    pollmanager.startService();
  }

  private void initTestAddr() {
    try {
      testaddr = InetAddress.getByName("127.0.0.1");
      testID = theDaemon.getIdentityManager().findIdentity(testaddr);
      testaddr1 = InetAddress.getByName("1.1.1.1");
      testID1 = theDaemon.getIdentityManager().findIdentity(testaddr1);
    }
    catch (UnknownHostException ex) {
      fail("can't open test host");
    }
  }

  private void initTestMsg() {
    try {
      testmsg = new LcapMessage[3];

      for(int i= 0; i<3; i++) {
        CachedUrlSet cus = testau.makeCachedUrlSet(
            new RangeCachedUrlSetSpec(rooturls[i], lwrbnd, uprbnd));
        PollSpec spec = new PollSpec(testau.getAUId(),
                                     rooturls[i],lwrbnd, uprbnd,
                                     cus);
        ((MockCachedUrlSet)spec.getCachedUrlSet()).setHasContent(false);
        int opcode = LcapMessage.NAME_POLL_REQ + (i * 2);
        testmsg[i] =  LcapMessage.makeRequestMsg(
          spec,
          agree_entries,
          pollmanager.generateRandomBytes(),
          pollmanager.generateRandomBytes(),
          opcode,
          spec.calcDuration(opcode,cus,pollmanager),
          testID);
      }
    }
    catch (IOException ex) {
      fail("can't create test message" + ex.toString());
    }
  }

  private void initTestPolls() {
    try {
     testpolls = new VersionOnePoll[3];
     for (int i = 0; i < 3; i++) {
       log.warning("initTestPolls: " + i);
       Poll p = pollmanager.makePoll(testmsg[i]);
       assertTrue(p instanceof VersionOnePoll);
       switch (i) {
       case 0:
	 assertTrue(p instanceof VersionOneNamePoll);
	 break;
       case 1:
	 assertTrue(p instanceof VersionOneContentPoll);
	 break;
       case 2:
	 assertTrue(p instanceof VersionOneVerifyPoll);
	 break;
       }
       testpolls[i] = (VersionOnePoll)p;
       assertNotNull(testpolls[i]);
       log.warning("initTestPolls: " + i + " " + p.toString());
     }
   }
   catch (IOException ex) {
     fail("can't create test poll" + ex.toString());
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
