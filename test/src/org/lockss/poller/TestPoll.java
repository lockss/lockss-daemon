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
import junit.framework.TestCase;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.lockss.test.FileUtil;

/** JUnitTest case for class: org.lockss.poller.Poll */
public class TestPoll extends TestCase {
  private static String[] rooturls = {"http://www.test.org",
    "http://www.test1.org", "http://www.test2.org"};
  private static String regexp = "*.doc";
  private static long testduration = 5 * 60 *60 *1000; /* 5 min */

  private static String[] testentries = {"test1.doc", "test2.doc", "test3.doc"};
  protected static ArchivalUnit testau;
  static {
    testau = PollTestPlugin.PTArchivalUnit.createFromListOfRootUrls(rooturls);
    org.lockss.plugin.Plugin.registerArchivalUnit(testau);
  }

  protected InetAddress testaddr;
  protected LcapIdentity testID;
  protected LcapMessage[] testmsg;
  protected Poll[] testpolls;
  protected PollManager pollmanager;

  public TestPoll(String _name) {
    super(_name);
  }

  /** setUp method for test case */
  protected void setUp() {
    //HashService.start();
    pollmanager = PollManager.getPollManager();
    try {
      testaddr = InetAddress.getByName("127.0.0.1");
      testID = IdentityManager.getIdentityManager().getIdentity(testaddr);
    }
    catch (UnknownHostException ex) {
      fail("can't open test host");
    }
    try {
      testmsg = new LcapMessage[3];

      for(int i= 0; i<3; i++) {
        testmsg[i] =  LcapMessage.makeRequestMsg(
        rooturls[i],
        regexp,
        testentries,
        testaddr,
        (byte)5,
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

    try {
      testpolls = new Poll[3];
      for(int i=0; i< 3; i++) {
        testpolls[i] = pollmanager.makePoll(testmsg[i]);
      }
    }
    catch (IOException ex) {
      fail("can't create test poll" + ex.toString());
    }
  }

  /** tearDown method for test case */
  protected void tearDown() {
    for(int i= 0; i< 3; i++) {
      pollmanager.removePoll(testpolls[i].m_key);
    }
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
    Poll p = testpolls[0];
    LcapMessage msg = p.getMessage();
    LcapIdentity id = msg.getOriginID();
    int rep = id.getReputation();

    // good vote check
    try {
      p.checkVote(msg.getHashed(), msg);
    }
    catch(IllegalStateException ex) {
      // unitialized comm
    }

    assertEquals(1, p.m_tally.numAgree);
    assertEquals(rep, p.m_tally.wtAgree);
    assertTrue(rep <= id.getReputation());

    rep = id.getReputation();
    // bad vote check
    try {
      p.checkVote(pollmanager.generateRandomBytes(), msg);
    }
    catch(IllegalStateException ex) {
      // unitialized comm
    }
    assertEquals(1, p.m_tally.numDisagree);
    assertEquals(rep, p.m_tally.wtDisagree);
    assertTrue(rep >= id.getReputation());
  }


  /** test for method tally(..) */
  public void testTally() {
    Poll p = testpolls[0];
    LcapMessage msg = p.getMessage();
    p.m_tally.addVote(new Vote(msg,false));
    p.m_tally.addVote(new Vote(msg,false));
    p.m_tally.addVote(new Vote(msg,false));
    assertEquals(0, p.m_tally.numAgree);
    assertEquals(0, p.m_tally.wtAgree);
    assertEquals(3, p.m_tally.numDisagree);
    assertEquals(1500, p.m_tally.wtDisagree);

    p = testpolls[1];
    msg = p.getMessage();
    p.m_tally.addVote(new Vote(msg,true));
    p.m_tally.addVote(new Vote(msg,true));
    p.m_tally.addVote(new Vote(msg,true));
    assertEquals(3, p.m_tally.numAgree);
    assertEquals(1500, p.m_tally.wtAgree);
    assertEquals(0, p.m_tally.numDisagree);
    assertEquals(0, p.m_tally.wtDisagree);
  }

  /** test for method vote(..) */
  public void testVote() {
    Poll p = testpolls[1];
    p.m_hash = pollmanager.generateRandomBytes();
    try {
      p.vote();
    }
    catch(IllegalStateException e) {
      // the socket isn't inited and should squack
    }
  }

  /** test for method startPoll(..) */
  public void testStartPoll() {
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
    catch(IllegalStateException e) {
      // the socket isn't inited and should squack
    }

    p.m_tally.numAgree = 20;
    try {
      p.voteInPoll();
    }
    catch(NullPointerException npe) {
      // the socket isn't inited and should squack
    }

  }

  /** test for method stopPoll(..) */
  public void testStopPoll() {
    Poll p = testpolls[1];
    p.m_pollstate = Poll.PS_WAIT_TALLY;
    p.stopPoll();
    assertTrue(p.m_pollstate == Poll.PS_COMPLETE);
    p.startPoll();
    assertTrue(p.m_pollstate == Poll.PS_COMPLETE);
  }

  /** test for method startVote(..) */
  public void testStartVote() {
    Poll p = testpolls[0];
    p.m_pendingVotes = 3;
    p.startVote();
    assertEquals(4, p.m_pendingVotes);
  }

  /** test for method stopVote(..) */
  public void testStopVote() {
    Poll p = testpolls[1];
    p.m_pendingVotes = 3;
    p.stopVote();
    assertEquals(2,p.m_pendingVotes);
  }


  /** Executes the test case
   * @param argv array of Strings containing command line arguments
   * */
  public static void main(String[] argv) {
    String[] testCaseList = {TestPoll.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}