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

  public TestPoll(String _name) {
    super(_name);
  }

  /** setUp method for test case */
  protected void setUp() {
    testID = LcapIdentity.getLocalIdentity();
    try {
      testmsg = new LcapMessage[3];

      for(int i= 0; i<3; i++) {
        testmsg[i] =  LcapMessage.makeRequestMsg(
          rooturls[i],
          regexp,
          testentries,
          testaddr,
          (byte)5,
          PollManager.generateRandomBytes(),
          PollManager.generateRandomBytes(),
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
        testpolls[i] = PollManager.makePoll(testmsg[i]);
      }
    }
    catch (IOException ex) {
      fail("can't create test poll" + ex.toString());
    }
  }

  /** tearDown method for test case */
  protected void tearDown() {
    for(int i= 0; i< 3; i++) {
      PollManager.removePoll(testpolls[i].m_key);
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
    // good vote check
    p.checkVote(msg.getHashed(), msg);
    assertEquals(1, p.m_agree);
    assertEquals(500, p.m_agreeWt);

    // bad vote check
    p.checkVote(PollManager.generateRandomBytes(), msg);
    assertEquals(1, p.m_disagree);
    assertEquals(500, p.m_disagreeWt);
  }

  /** test for method handleAgreeVote(..) */
  public void testHandleAgreeVote() {
    Poll p = testpolls[1];
    LcapMessage msg = p.getMessage();
    p.handleAgreeVote(msg);
    assertEquals(1, p.m_agree);
  }

  /** test for method handleDisagreeVote(..) */
  public void testHandleDisagreeVote() {
    Poll p = testpolls[1];
    LcapMessage msg = p.getMessage();
    p.handleDisagreeVote(msg);
    assertEquals(1, p.m_disagree);
  }

  /** test for method tally(..) */
  public void testTally() {
    Poll p = testpolls[0];
    LcapMessage msg = p.getMessage();
    p.handleDisagreeVote(msg);
    p.handleDisagreeVote(msg);
    p.handleDisagreeVote(msg);
    p.tally();
    assertEquals(0, p.m_agree);
    assertEquals(0, p.m_agreeWt);
    assertEquals(3, p.m_disagree);
    assertEquals(1500, p.m_disagreeWt);

    p = testpolls[1];
    msg = p.getMessage();
    p.handleAgreeVote(msg);
    p.handleAgreeVote(msg);
    p.handleAgreeVote(msg);
    p.tally();
    assertEquals(3, p.m_agree);
    assertEquals(1500, p.m_agreeWt);
    assertEquals(0, p.m_disagree);
    assertEquals(0, p.m_disagreeWt);
   }

  /** test for method vote(..) */
  public void testVote() {
    Poll p = testpolls[1];
    //p.vote();
  }

  /** test for method startPoll(..) */
  public void testStartPoll() {
  }

  /** test for method voteInPoll(..) */
  public void testVoteInPoll() {
    Poll p = testpolls[0];
    p.m_quorum = 10;
    p.m_agree = 5;
    p.m_disagree = 2;
    p.m_agreeWt = 2000;
    p.m_disagreeWt = 200;

    //p.voteInPoll();

    p.m_agree = 20;
    //p.voteInPoll();

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
    p.m_counting = 3;
    p.startVote();
    assertEquals(4, p.m_counting);

  }

  /** test for method stopVote(..) */
  public void testStopVote() {
    Poll p = testpolls[1];
    p.m_counting = 3;
    p.stopVote();
    assertEquals(2,p.m_counting);
  }


  /** Executes the test case
   * @param argv array of Strings containing command line arguments
   * */
  public static void main(String[] argv) {
    String[] testCaseList = {TestPoll.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
