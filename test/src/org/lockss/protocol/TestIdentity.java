package org.lockss.protocol;

import java.net.*;
import org.lockss.util.Logger;
import java.util.Random;
import org.mortbay.util.B64Code;
import junit.framework.TestCase;
import java.io.*;

/** JUnitTest case for class: org.lockss.protocol.Identity */
public class TestIdentity extends TestCase {

  static String fakeIdString = "123.145.167";
  static LcapIdentity fakeId = new LcapIdentity(fakeIdString);
  InetAddress testAddress;
  int testPort;
  int testReputation;
  Object testIdKey;
  LcapMessage testMsg= null;
  private static String urlstr = "http://www.test.org";
  private static String regexp = "*.doc";
  private static byte[] testbytes = {1,2,3,4,5,6,7,8,9,10};
  private static String[] testentries = {"test1.doc", "test2.doc", "test3.doc"};

  public TestIdentity(String _name) {
    super(_name);
  }

  /** setUp method for test case */
  protected void setUp() {
    try {
      testAddress = InetAddress.getByName("127.0.0.1");
    }
    catch (UnknownHostException ex) {
      fail("can't open test host");
    }
    testPort = 0;
    testReputation = LcapIdentity.INITIAL_REPUTATION;
    testIdKey = LcapIdentity.makeIdKey(testAddress,testPort);
    try {
      testMsg = LcapMessage.makeRequestMsg(urlstr,
                                       regexp,
                                       testentries,
                                       testAddress,
                                       (byte)5,
                                       testbytes,
                                       testbytes,
                                       LcapMessage.CONTENT_POLL_REQ,
                                       100000,
                                       fakeId);
    }
    catch (Exception ex) {
      fail("message request creation failed.");
    }
  }

  /** test for method getIdentity(..) */
  public void testGetIdentity() {
    LcapIdentity id1 = LcapIdentity.getIdentity(testAddress,testPort);
    assertTrue(id1 != null);
    // try and get the identity we just added
    LcapIdentity id2 = LcapIdentity.findIdentity(id1.m_idKey);
    assertTrue(id2.isEqual(id1));
  }

  /** test for method getLocalIdentity(..) */
  public void testGetLocalIdentity() {
  }

  /** test for method isLocalIdentity(..) */
  public void testIsLocalIdentity() {
  }

  /** test for method isEqual(..) */
  public void testIsEqual() {
    LcapIdentity id1 = LcapIdentity.getIdentity(testAddress,testPort);
    LcapIdentity id2 = LcapIdentity.findIdentity(id1.m_idKey);
    assertEquals((String)id1.m_idKey,(String)id2.m_idKey);
  }

  /** test for method toString(..) */
  public void testToString() {
    String s = fakeId.toString();
    assertTrue(s.equals((String)fakeId.m_idKey));
  }

  /** test for method toHost(..) */
  public void testToHost() {
  }

  void checkReputation(int orgRep, int maxDelta, int curRep) {
    if(maxDelta > 0) {
      assertTrue(curRep <= orgRep + maxDelta);
    }
    else if(maxDelta < 0) {
      assertTrue(curRep >= orgRep + maxDelta);
    }
  }
// -----------------------------------------------------

/** test for method agreeWithVote(..) */
  public void testAgreeWithVote() {
    int rep = fakeId.getReputation();
    fakeId.agreeWithVote();
    checkReputation(rep, LcapIdentity.AGREE_DELTA, fakeId.getReputation());

  }

  /** test for method disagreeWithVote(..) */
  public void testDisagreeWithVote() {
    int rep = fakeId.getReputation();
    fakeId.disagreeWithVote();
    checkReputation(rep, LcapIdentity.DISAGREE_DELTA, fakeId.getReputation());
  }

  /** test for method callInternalPoll(..) */
  public void testCallInternalPoll() {
    int rep = fakeId.getReputation();
    fakeId.callInternalPoll();
    checkReputation(rep, LcapIdentity.CALL_INTERNAL_DELTA, fakeId.getReputation());
  }

  /** test for method spoofDetected(..) */
  public void testSpoofDetected() {
    int rep = fakeId.getReputation();
    fakeId.spoofDetected();
    checkReputation(rep, LcapIdentity.SPOOF_DETECTED, fakeId.getReputation());
  }

  /** test for method replayDected(..) */
  public void testReplayDected() {
    int rep = fakeId.getReputation();
    fakeId.replayDetected();
    checkReputation(rep, LcapIdentity.REPLAY_DETECTED, fakeId.getReputation());
  }

  /** test for method acttackDetected(..) */
  public void testActtackDetected() {
    int rep = fakeId.getReputation();
    fakeId.attackDetected();
    checkReputation(rep, LcapIdentity.ATTACK_DETECTED, fakeId.getReputation());
  }

  /** test for method voteNotVerify(..) */
  public void testVoteNotVerify() {
    int rep = fakeId.getReputation();
    fakeId.voteNotVerify();
    checkReputation(rep, LcapIdentity.VOTE_NOT_VERIFIED, fakeId.getReputation());
  }

  /** test for method voteVerify(..) */
  public void testVoteVerify() {
    int rep = fakeId.getReputation();
    fakeId.voteVerify();
    checkReputation(rep, LcapIdentity.VOTE_VERIFIED, fakeId.getReputation());
  }

  /** test for method voteDisown(..) */
  public void testVoteDisown() {
    int rep = fakeId.getReputation();
    fakeId.voteDisown();
    checkReputation(rep, LcapIdentity.VOTE_DISOWNED, fakeId.getReputation());
  }

  /** test for method changeReputation(..) */
  public void testChangeReputation() {
    // test simple change
    int rep = fakeId.getReputation();
    fakeId.changeReputation(50);
    assertTrue(rep+50 >= fakeId.getReputation());
    fakeId.changeReputation(-150);
    rep = fakeId.getReputation();
    assertTrue(rep-150 <= fakeId.getReputation());

    // test upper and lower bounds
    rep = fakeId.getReputation();
    fakeId.changeReputation(LcapIdentity.MAX_REPUTATION_DELTA * 10);
    assertTrue(fakeId.getReputation() <= rep +LcapIdentity.MAX_REPUTATION_DELTA);
    rep = fakeId.getReputation();
    fakeId.changeReputation(-LcapIdentity.MAX_REPUTATION_DELTA * 10);
    assertTrue(fakeId.getReputation() >= rep - LcapIdentity.MAX_REPUTATION_DELTA);
  }

  /** test for method rememberActive(..) */
  public void testRememberActive() {
    long inc_pkts = fakeId.m_incrPackets;
    long tot_pkts = fakeId.m_totalPackets;

    fakeId.rememberActive(false,testMsg);
    assertEquals(inc_pkts + 1, fakeId.m_incrPackets);
    assertEquals(tot_pkts + 1, fakeId.m_totalPackets);

  }

  /** test for method rememberValidOriginator(..) */
  public void testRememberValidOriginator() {
    long val_originator = fakeId.m_origPackets;
    fakeId.rememberValidOriginator(testMsg);
    assertEquals(val_originator + 1, fakeId.m_origPackets);
  }

  /** test for method rememberValidForward(..) */
  public void testRememberValidForward() {
    long val_forward = fakeId.m_forwPackets;
    fakeId.rememberValidForward(testMsg);
    assertEquals(val_forward + 1, fakeId.m_forwPackets);
  }

  /** test for method rememberDuplicate(..) */
  public void testRememberDuplicate() {
    long duplicates = fakeId.m_duplPackets;
    fakeId.rememberDuplicate(testMsg);
    assertEquals(duplicates + 1, fakeId.m_duplPackets);
  }

  /** Executes the test case */
  public static void main(String[] argv) {
    String[] testCaseList = {TestIdentity.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}