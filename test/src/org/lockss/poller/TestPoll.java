package org.lockss.poller;

import java.io.*;
import java.security.*;
import java.util.*;
import gnu.regexp.*;
import org.lockss.daemon.*;
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.lockss.test.FileUtil;
import org.lockss.test.*;

/** JUnitTest case for class: org.lockss.poller.Poll */
public class TestPoll extends LockssTestCase {
  private static String[] rooturls = {
      "http://www.test.org",
      "http://www.test1.org", "http://www.test2.org"};
  private static String lwrbnd = "test1.doc";
  private static String uprbnd = "test3.doc";
  private static long testduration = 60 * 60 * 60 * 1000; /* 60 min */

  protected static ArchivalUnit testau;
  private static IdentityManager idmgr;
  private static MockLockssDaemon daemon = new MockLockssDaemon(null);
  private String[] agree_entries = makeEntries(10, 50);
  private String[] disagree_entries = makeEntries(15, 57);
  private String[] dissenting_entries = makeEntries(7, 50);

 static {
    testau = PollTestPlugin.PTArchivalUnit.createFromListOfRootUrls(rooturls);
    daemon.getPluginManager().registerArchivalUnit(testau);
    TestIdentityManager.configParams("/tmp/iddb", "src/org/lockss/protocol");
    idmgr = daemon.getIdentityManager();
  }

  protected InetAddress testaddr;
  protected InetAddress testaddr1;
  protected LcapIdentity testID;
  protected LcapIdentity testID1;
  protected LcapMessage[] testmsg;
  protected Poll[] testpolls;
  static protected PollManager pollmanager = daemon.getPollManager();

  public TestPoll(String _name) {
    super(_name);
  }

  /** setUp method for test case
   * @throws Exception
   */
  protected void setUp() throws Exception {
    super.setUp();
    daemon.getHashService().startService();
    try {
      testaddr = InetAddress.getByName("127.0.0.1");
      testID = idmgr.findIdentity(testaddr);
      testaddr1 = InetAddress.getByName("123.3.4.5");
      testID1 = idmgr.findIdentity(testaddr1);
    }
    catch (UnknownHostException ex) {
      fail("can't open test host");
    }
    try {
      testmsg = new LcapMessage[3];

      for (int i = 0; i < 3; i++) {
        testmsg[i] = LcapMessage.makeRequestMsg(
            rooturls[i],
            lwrbnd,
            uprbnd,
            agree_entries,
            pollmanager.generateRandomBytes(),
            pollmanager.generateRandomBytes(),
            LcapMessage.NAME_POLL_REQ + (i * 2),
            testduration,
            testID,
            testau.getPluginId());
      }
    }
    catch (IOException ex) {
      fail("can't create test message" + ex.toString());
    }

    try {
      testpolls = new Poll[3];
      for (int i = 0; i < 3; i++) {
        testpolls[i] = pollmanager.makePoll(testmsg[i]);
      }
    }
    catch (IOException ex) {
      fail("can't create test poll" + ex.toString());
    }
  }

  /** tearDown method for test case
   * @throws Exception if removePoll failed
   */
  public void tearDown() throws Exception {
    daemon.getHashService().stopService();
    for (int i = 0; i < 3; i++) {
      pollmanager.removePoll(testpolls[i].m_key);
    }
    super.tearDown();
  }

  /** test for method scheduleVote(..) */
  public void testScheduleVote() {
    Poll p = testpolls[1];
    p.scheduleVote();
    assertNotNull(p.m_voteTime);
    assertTrue(p.m_voteTime.getRemainingTime()
               < p.m_deadline.getRemainingTime());
  }

  /** test for method checkVote(..) */
  public void testCheckVote() {
    LcapMessage msg = null;
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
    Poll p = null;
    try {
      p = createCompletedPoll(msg, 8,2);
    }
    catch (Exception ex2) {
    }
    LcapIdentity id = idmgr.findIdentity(msg.getOriginAddr());

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
    Poll p = testpolls[0];
    LcapMessage msg = p.getMessage();
    p.m_tally.addVote(new Vote(msg, false));
    p.m_tally.addVote(new Vote(msg, false));
    p.m_tally.addVote(new Vote(msg, false));
    assertEquals(0, p.m_tally.numAgree);
    assertEquals(0, p.m_tally.wtAgree);
    assertEquals(3, p.m_tally.numDisagree);
    assertEquals(1500, p.m_tally.wtDisagree);

    p = testpolls[1];
    msg = p.getMessage();
    p.m_tally.addVote(new Vote(msg, true));
    p.m_tally.addVote(new Vote(msg, true));
    p.m_tally.addVote(new Vote(msg, true));
    assertEquals(3, p.m_tally.numAgree);
    assertEquals(1500, p.m_tally.wtAgree);
    assertEquals(0, p.m_tally.numDisagree);
    assertEquals(0, p.m_tally.wtDisagree);
  }


  public void testNamePollTally() {
    NamePoll np;
    // test a name poll we won
    np = makeCompletedNamePoll(4,1,0);
    assertEquals(5, np.m_tally.numAgree);
    assertEquals(1, np.m_tally.numDisagree);
    assertTrue(np.m_tally.didWinPoll());

    // test a name poll we lost with a dissenting vote
    np = makeCompletedNamePoll(2,4,1);

    assertEquals(3, np.m_tally.numAgree);
    assertEquals(5, np.m_tally.numDisagree);
    assertTrue(!np.m_tally.didWinPoll());

    // build a master list
    np.buildPollLists(np.m_tally.pollVotes.iterator());

    // these should be different since we lost the poll
    assertTrue(!Arrays.equals(np.m_tally.localEntries, np.m_tally.votedEntries));

    // the expected "correct" set is in our disagree msg
    assertTrue(Arrays.equals(disagree_entries, np.m_tally.votedEntries));

  }



  /** test for method vote(..) */
  public void testVote() {
    Poll p = testpolls[1];
    p.m_hash = pollmanager.generateRandomBytes();
    try {
      p.vote();
    }
    catch (IllegalStateException e) {
      // the socket isn't inited and should squack
    }
  }

  /** test for method voteInPoll(..) */
  public void testVoteInPoll() {
    Poll p = testpolls[0];
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

  }

  /** test for method stopPoll(..) */
  public void testStopPoll() {
    Poll p = testpolls[1];
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
    Poll p = testpolls[0];
    p.m_pendingVotes = 3;
    p.startVoteCheck();
    assertEquals(4, p.m_pendingVotes);
  }

  /** test for method stopVote(..) */
  public void testStopVote() {
    Poll p = testpolls[1];
    p.m_pendingVotes = 3;
    p.stopVoteCheck();
    assertEquals(2, p.m_pendingVotes);
  }

  private NamePoll makeCompletedNamePoll(int numAgree,
                                     int numDisagree,
                                     int numDissenting) {
    NamePoll np = null;
    LcapMessage agree_msg = null;
    LcapMessage disagree_msg1 = null;
    LcapMessage disagree_msg2 = null;

    try {
      LcapMessage poll_msg = LcapMessage.makeRequestMsg(
          rooturls[0],
          null,
          null,
          null,
          pollmanager.generateRandomBytes(),
          pollmanager.generateRandomBytes(),
          LcapMessage.NAME_POLL_REQ,
          testduration,
          testID,
          testau.getPluginId());

      CachedUrlSet cus = testau.makeCachedUrlSet(poll_msg.getTargetUrl(),
                                                 poll_msg.getLwrBound(),
                                                 poll_msg.getUprBound());
      // make our poll
      np = (NamePoll) pollmanager.createPoll(poll_msg, cus);

      // generate agree vote msg
      agree_msg = LcapMessage.makeReplyMsg(poll_msg, poll_msg.getHashed(),
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
    np.m_tally.addVote(np.makeVote(np.getMessage(), true));

    // add the agree votes
    for(int i = 0; i < numAgree; i++) {
      np.m_tally.addVote(np.makeVote(agree_msg, true));
    }

    // add the disagree votes
    for(int i = 0; i < numDisagree; i++) {
      np.m_tally.addVote(np.makeVote(disagree_msg1, false));
    }

    // add dissenting disagree vote
    for(int i = 0; i < numDissenting; i++) {
      np.m_tally.addVote(np.makeVote(disagree_msg2, false));
    }

    return np;
  }


  public static Poll createCompletedPoll(LcapMessage testmsg, int numAgree,
                                         int numDisagree) throws Exception {
    testau = PollTestPlugin.PTArchivalUnit.createFromListOfRootUrls(rooturls);
    daemon.getPluginManager().registerArchivalUnit(testau);
    CachedUrlSet cus = testau.makeCachedUrlSet(testmsg.getTargetUrl(),
                                               testmsg.getLwrBound(),
                                               testmsg.getUprBound());
    Poll p = daemon.getPollManager().createPoll(testmsg, cus);
    p.m_tally.quorum = numAgree + numDisagree;
    p.m_tally.numAgree = numAgree;
    p.m_tally.numDisagree = numDisagree;
    p.m_tally.wtAgree = 2000;
    p.m_tally.wtDisagree = 200;
    p.m_tally.localEntries = new String[] {
        "entry 1", "entry 2", "entry 5"};
    p.m_tally.votedEntries = new String[] {
        "entry 1", "entry 3", "entry 4", "entry 5"};
    p.m_pollstate = Poll.PS_COMPLETE;
    return p;
  }


  public static String[] makeEntries(int firstEntry, int lastEntry) {
    int numEntries = lastEntry - firstEntry;
    String[] ret_arry = new String[numEntries];

    for(int i=0; i < numEntries; i++) {
      ret_arry[i] = "testentry" + (firstEntry + i) + ".html";
    }

    return ret_arry;
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