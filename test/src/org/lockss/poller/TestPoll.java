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
  private static long testduration = Constants.HOUR;

  protected ArchivalUnit testau = PollTestPlugin.PTArchivalUnit.createFromListOfRootUrls(rooturls);
  private IdentityManager idmgr;
  //XXX fix to use non-statically
  private static MockLockssDaemon theDaemon;
  private String[] agree_entries = makeEntries(10, 50);
  private String[] disagree_entries = makeEntries(15, 57);
  private String[] dissenting_entries = makeEntries(7, 50);

  protected InetAddress testaddr;
  protected InetAddress testaddr1;
  protected LcapIdentity testID;
  protected LcapIdentity testID1;
  protected LcapMessage[] testmsg;
  protected Poll[] testpolls;
  protected PollManager pollmanager;

  protected void setUp() throws Exception {
    super.setUp();

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
    for(int i=0; i<3; i++) {
      pollmanager.removePoll(testmsg[i].getKey());
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
      p = createCompletedPoll(theDaemon, testau, msg, 8,2);
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
    assertFalse(np.m_tally.didWinPoll());

    // build a master list
    np.buildPollLists(np.m_tally.pollVotes.iterator());

    // these should be different since we lost the poll
    assertFalse(Arrays.equals(np.m_tally.localEntries, np.m_tally.votedEntries));

    // the expected "correct" set is in our disagree msg
    assertTrue(Arrays.equals(disagree_entries, np.m_tally.votedEntries));

  }



  /** test for method vote(..) */
  public void testVote() {
    Poll p = testpolls[1];
    p.m_hash = pollmanager.generateRandomBytes();
    try {
      p.castOurVote();
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
      PollSpec spec =
	new PollSpec(testau.getPluginId(),
		     testau.getAUId(),
		     rooturls[0],null,null,
		     testau.makeCachedUrlSet(rooturls[0],null,null));
      LcapMessage poll_msg = LcapMessage.makeRequestMsg(
          spec,
          null,
          pollmanager.generateRandomBytes(),
          pollmanager.generateRandomBytes(),
          LcapMessage.NAME_POLL_REQ,
          testduration,
          testID);

      // make our poll
      np = (NamePoll) pollmanager.createPoll(poll_msg, spec);

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

    return np;
  }


  public static Poll createCompletedPoll(LockssDaemon daemon,
                                         ArchivalUnit au,
					 LcapMessage testmsg, int numAgree,
                                         int numDisagree) throws Exception {
    log.debug("daemon = " + daemon);
    CachedUrlSet cus = au.makeCachedUrlSet(testmsg.getTargetUrl(),
                                               testmsg.getLwrBound(),
                                               testmsg.getUprBound());
    PollSpec spec = new PollSpec(cus);
    Poll p = daemon.getPollManager().createPoll(testmsg, spec);
    p.m_tally.quorum = numAgree + numDisagree;
    p.m_tally.numAgree = numAgree;
    p.m_tally.numDisagree = numDisagree;
    p.m_tally.wtAgree = 2000;
    p.m_tally.wtDisagree = 200;
    p.m_tally.localEntries = new String[] {
        "/testentry1.html", "/testentry2.html", "/testentry3.html" };
    p.m_tally.votedEntries = new String[] {
        "/testentry1.html", "/testentry3.html",
        "/testentry4.html", "/testentry5.html"};
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
    TestIdentityManager.configParams(tempDirPath + "iddb",
                                     "src/org/lockss/protocol", cacheStr);
    idmgr = theDaemon.getIdentityManager();
    theDaemon.getHashService().startService();
    theDaemon.getLockssRepositoryService().startService();
    theDaemon.getRouterManager().startService();

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
        PollSpec spec = new PollSpec(testau.getPluginId(),
                                     testau.getAUId(),
                                     rooturls[i],lwrbnd, uprbnd,
                                     testau.makeCachedUrlSet(rooturls[i],
                                                             lwrbnd,uprbnd));
        testmsg[i] =  LcapMessage.makeRequestMsg(
          spec,
          agree_entries,
          pollmanager.generateRandomBytes(),
          pollmanager.generateRandomBytes(),
          LcapMessage.NAME_POLL_REQ + (i * 2),
          testduration,
          testID);
      }
    }
    catch (IOException ex) {
      fail("can't create test message" + ex.toString());
    }
  }

  private void initTestPolls() {
    try {
     testpolls = new Poll[3];
     for (int i = 0; i < 3; i++) {
       testpolls[i] = pollmanager.makePoll(testmsg[i]);
       assertNotNull(testpolls[i]);
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
