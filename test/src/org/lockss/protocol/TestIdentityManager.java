/*
 * $Id: TestIdentityManager.java,v 1.28 2004-09-20 14:20:40 dshr Exp $
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

package org.lockss.protocol;

import java.io.File;
import java.util.*;
import java.net.UnknownHostException;
import org.lockss.util.*;
import org.lockss.test.*;

/** JUnitTest case for class: org.lockss.protocol.IdentityManager */
public class TestIdentityManager extends LockssTestCase {
  int testReputation;
  Object testIdKey;
//  private static String urlstr = "http://www.test.org";
//  private static String lwrbnd = "test1.doc";
//  private static String uprbnd = "test3.doc";
//  private static byte[] testbytes = {1,2,3,4,5,6,7,8,9,10};
//  private static String[] testentries = {"test1.doc",
//                                         "test2.doc", "test3.doc"};

  private MockLockssDaemon theDaemon;
  private IdentityManager idmgr;
  PeerIdentity peer1;
  PeerIdentity peer2;
  PeerIdentity peer3;

  public void setUp() throws Exception {
    super.setUp();

    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.1.2.3");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    theDaemon = new MockLockssDaemon();
    idmgr = theDaemon.getIdentityManager();
    idmgr.startService();

    testReputation = IdentityManager.INITIAL_REPUTATION;

  }

  public void tearDown() throws Exception {
    if (idmgr != null) {
      idmgr.stopService();
      idmgr = null;
    }
    super.tearDown();
  }

  /** test for method stringToPeerIdentity **/
  public void testStringToPeerIdentity() {
    try {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    assertTrue(peer1 != null);
    assertTrue(peer1 == idmgr.stringToPeerIdentity("127.0.0.1"));
    peer2 = idmgr.stringToPeerIdentity("127.0.0.2");
    assertTrue(peer2 != null);
    assertNotEquals(peer1, peer2);
    peer3 = idmgr.stringToPeerIdentity("127.0.0.2:300");
    assertTrue(peer3 != null);
    assertNotEquals(peer3, peer2);
    assertNotEquals(peer3, peer1);
    } catch (UnknownHostException uhe) {
      fail(uhe.toString());
    }
  }

  /** test for method ipAddrToPeerIdentity **/
  public void testIPAddrToPeerIdentity() {
    IPAddr ip1 = null;
    IPAddr ip2 = null;
    IPAddr ip3 = null;

    try {
      ip1 = IPAddr.getByName("127.0.0.1");
      ip2 = IPAddr.getByName("127.0.0.2");
      ip3 = IPAddr.getByName("127.0.0.3");
    peer1 = idmgr.ipAddrToPeerIdentity(ip1);
    assertTrue(peer1 != null);
    assertTrue(peer1 == idmgr.stringToPeerIdentity("127.0.0.1"));
    peer2 = idmgr.ipAddrToPeerIdentity(ip2);
    assertTrue(peer2 != null);
    assertNotEquals(peer1, peer2);
    assertTrue(peer2 == idmgr.stringToPeerIdentity("127.0.0.2"));
    peer3 = idmgr.ipAddrToPeerIdentity(ip2, 300);
    assertTrue(peer3 != null);
    assertNotEquals(peer3, peer2);
    assertNotEquals(peer3, peer1);
    assertTrue(peer3 == idmgr.stringToPeerIdentity("127.0.0.2:300"));
    } catch (UnknownHostException uhe) {
      fail(uhe.toString());
    }
  }

  /** test for method getLocalIdentity(..) */
  public void testGetLocalIdentity() {
  }

  /** test for method isLocalIdentity(..) */
  public void testIsLocalIdentity() {
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
    try {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    int rep = idmgr.getReputation(peer1);
    idmgr.changeReputation(peer1, IdentityManager.AGREE_VOTE);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.AGREE_VOTE],
                    idmgr.getReputation(peer1));
    } catch (UnknownHostException uhe) {
      fail(uhe.toString());
    }

  }

  /** test for method disagreeWithVote(..) */
  public void testDisagreeWithVote() {
    try {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    int rep = idmgr.getReputation(peer1);
    idmgr.changeReputation(peer1,IdentityManager.DISAGREE_VOTE);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.DISAGREE_VOTE],
                    idmgr.getReputation(peer1));
    } catch (UnknownHostException uhe) {
      fail(uhe.toString());
    }
  }

  /** test for method callInternalPoll(..) */
  public void testCallInternalPoll() {
    try {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    int rep = idmgr.getReputation(peer1);
    idmgr.changeReputation(peer1,IdentityManager.CALL_INTERNAL);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.CALL_INTERNAL],
                    idmgr.getReputation(peer1));
    } catch (UnknownHostException uhe) {
      fail(uhe.toString());
    }
  }

  /** test for method spoofDetected(..) */
  public void testSpoofDetected() {
    try {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    int rep = idmgr.getReputation(peer1);
    idmgr.changeReputation(peer1,IdentityManager.SPOOF_DETECTED);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.SPOOF_DETECTED],
                    idmgr.getReputation(peer1));
    } catch (UnknownHostException uhe) {
      fail(uhe.toString());
    }
  }

  /** test for method replayDected(..) */
  public void testReplayDected() {
    try {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    int rep = idmgr.getReputation(peer1);
    idmgr.changeReputation(peer1,IdentityManager.REPLAY_DETECTED);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.REPLAY_DETECTED],
                    idmgr.getReputation(peer1));
    } catch (UnknownHostException uhe) {
      fail(uhe.toString());
    }
  }

  /** test for method acttackDetected(..) */
  public void testAttackDetected() {
    try {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    int rep = idmgr.getReputation(peer1);
    idmgr.changeReputation(peer1,IdentityManager.ATTACK_DETECTED);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.ATTACK_DETECTED],
                    idmgr.getReputation(peer1));
    } catch (UnknownHostException uhe) {
      fail(uhe.toString());
    }
  }

  /** test for method voteNotVerify(..) */
  public void testVoteNotVerify() {
    try {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    int rep = idmgr.getReputation(peer1);
    idmgr.changeReputation(peer1,IdentityManager.VOTE_NOTVERIFIED);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.VOTE_NOTVERIFIED],
                    idmgr.getReputation(peer1));
    } catch (UnknownHostException uhe) {
      fail(uhe.toString());
    }
  }

  /** test for method voteVerify(..) */
  public void testVoteVerify() {
    try {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    int rep = idmgr.getReputation(peer1);
    idmgr.changeReputation(peer1,IdentityManager.VOTE_VERIFIED);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.VOTE_VERIFIED],
                    idmgr.getReputation(peer1));
    } catch (UnknownHostException uhe) {
      fail(uhe.toString());
    }
  }

  /** test for method voteDisown(..) */
  public void testVoteDisown() {
    try {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    int rep = idmgr.getReputation(peer1);
    idmgr.changeReputation(peer1,IdentityManager.VOTE_DISOWNED);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.VOTE_DISOWNED],
                    idmgr.getReputation(peer1));
    } catch (UnknownHostException uhe) {
      fail(uhe.toString());
    }
  }

  public void testStoreIdentities() throws UnknownHostException {
    try {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    peer2 = idmgr.stringToPeerIdentity("127.0.0.2");
    peer3 = idmgr.stringToPeerIdentity("127.0.0.3");

    try {
      idmgr.storeIdentities();
    } catch (ProtocolException ex) {
      fail("identity db store failed");
    }
    } catch (UnknownHostException uhe) {
      fail(uhe.toString());
    }
  }

  public void testSetAgreedThrowsOnNullAu() throws UnknownHostException {
    try {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    try {
      idmgr.signalAgreed(peer1, null);
      fail("Should have thrown on a null au");
    } catch (IllegalArgumentException e) {
    }
    } catch (UnknownHostException uhe) {
      fail(uhe.toString());
    }
  }

  public void testSetAgreedThrowsOnNullId() throws UnknownHostException {
    MockArchivalUnit mau = new MockArchivalUnit();
    try {
      idmgr.signalAgreed(null, mau);
      fail("Should have thrown on a null au");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testSetDisagreedThrowsOnNullAu() throws UnknownHostException {
    try {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    try {
      idmgr.signalDisagreed(peer1, null);
      fail("Should have thrown on a null au");
    } catch (IllegalArgumentException e) {
    }
    } catch (UnknownHostException uhe) {
      fail(uhe.toString());
    }
  }

  public void testSetDisagreedThrowsOnNullId() throws UnknownHostException {
    MockArchivalUnit mau = new MockArchivalUnit();
    try {
      idmgr.signalDisagreed(null, mau);
      fail("Should have thrown on a null au");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testGetAgreeThrowsOnNullAu() throws UnknownHostException {
    try {
      idmgr.getAgreed(null);
      fail("Should have thrown on a null au");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testGetAgreedNoneSet() throws UnknownHostException {
    MockArchivalUnit mau = new MockArchivalUnit();
    theDaemon.setHistoryRepository(new MockHistoryRepository(), mau);
    assertNull(idmgr.getAgreed(mau));
  }

  public void testGetAgreed() throws UnknownHostException {
    try {
    TimeBase.setSimulated(10);
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    peer2 = idmgr.stringToPeerIdentity("127.0.0.2");

    MockArchivalUnit mau = new MockArchivalUnit();
    theDaemon.setHistoryRepository(new MockHistoryRepository(), mau);

    idmgr.signalAgreed(peer1, mau);
    TimeBase.step();
    idmgr.signalAgreed(peer2, mau);
    Map expected = new HashMap();
    expected.put(peer1, new Long(10));
    expected.put(peer2, new Long(11));
    assertEquals(expected, idmgr.getAgreed(mau));
    } catch (UnknownHostException uhe) {
      fail(uhe.toString());
    }
  }

  public void testDisagreeDoesntRemove() throws UnknownHostException {
    try {
    TimeBase.setSimulated(10);
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    peer2 = idmgr.stringToPeerIdentity("127.0.0.2");

    MockArchivalUnit mau = new MockArchivalUnit();
    theDaemon.setHistoryRepository(new MockHistoryRepository(), mau);
    idmgr.signalAgreed(peer1, mau);
    idmgr.signalAgreed(peer2, mau);

    idmgr.signalDisagreed(peer1, mau);

    Map expected = new HashMap();
    expected.put(peer1, new Long(10));
    expected.put(peer2, new Long(10));

    assertEquals(expected, idmgr.getAgreed(mau));
    } catch (UnknownHostException uhe) {
      fail(uhe.toString());
    }
  }

  public void testGetCachesToRepairFromThrowsOnNullAu()
      throws UnknownHostException {
    try {
      idmgr.getCachesToRepairFrom(null);
      fail("Should have thrown on a null au");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testGetCachesToRepairFromNoneSet() throws UnknownHostException {
    MockArchivalUnit mau = new MockArchivalUnit();
    theDaemon.setHistoryRepository(new MockHistoryRepository(), mau);
    assertNull(idmgr.getCachesToRepairFrom(mau));
  }

  public void testCachesToRepairFrom() throws UnknownHostException {
    try {
    TimeBase.setSimulated(10);
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    peer2 = idmgr.stringToPeerIdentity("127.0.0.2");

    MockArchivalUnit mau = new MockArchivalUnit();
    theDaemon.setHistoryRepository(new MockHistoryRepository(), mau);
    idmgr.signalAgreed(peer1, mau);
    TimeBase.step();
    idmgr.signalAgreed(peer2, mau);
    Map expected = new HashMap();
    expected.put(peer1, new Long(10));
    expected.put(peer2, new Long(11));
    assertEquals(expected, idmgr.getCachesToRepairFrom(mau));
    } catch (UnknownHostException uhe) {
      fail(uhe.toString());
    }
  }

  public void testAgreeUpdatesTime() throws UnknownHostException {
    try {
    TimeBase.setSimulated(10);
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");

    MockArchivalUnit mau = new MockArchivalUnit();
    theDaemon.setHistoryRepository(new MockHistoryRepository(), mau);

    idmgr.signalAgreed(peer1, mau);
    TimeBase.step(15);
    idmgr.signalAgreed(peer1, mau);

    Map expected = new HashMap();
    expected.put(peer1, new Long(25));

    assertEquals(expected, idmgr.getCachesToRepairFrom(mau));
    } catch (UnknownHostException uhe) {
      fail(uhe.toString());
    }
  }

  public void testDisagreeNullsMap() throws UnknownHostException {
    try {
    TimeBase.setSimulated(10);
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");

    MockArchivalUnit mau = new MockArchivalUnit();
    theDaemon.setHistoryRepository(new MockHistoryRepository(), mau);
    idmgr.signalAgreed(peer1, mau);

    idmgr.signalDisagreed(peer1, mau);

    Map expected = new HashMap();

    assertNull(idmgr.getCachesToRepairFrom(mau));
    } catch (UnknownHostException uhe) {
      fail(uhe.toString());
    }
  }

  public void testDisagreeRemoves() throws UnknownHostException {
    try {
    TimeBase.setSimulated(10);
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    peer2 = idmgr.stringToPeerIdentity("127.0.0.2");

    MockArchivalUnit mau = new MockArchivalUnit();
    theDaemon.setHistoryRepository(new MockHistoryRepository(), mau);
    idmgr.signalAgreed(peer1, mau);
    idmgr.signalAgreed(peer2, mau);

    idmgr.signalDisagreed(peer1, mau);

    Map expected = new HashMap();
    expected.put(peer2, new Long(10));

    assertEquals(expected, idmgr.getCachesToRepairFrom(mau));
    } catch (UnknownHostException uhe) {
      fail(uhe.toString());
    }
  }

  public void testMultipleAus() throws UnknownHostException {
    try {
    TimeBase.setSimulated(10);
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    peer2 = idmgr.stringToPeerIdentity("127.0.0.2");
    peer3 = idmgr.stringToPeerIdentity("127.0.0.3");

    MockArchivalUnit mau1 = new MockArchivalUnit();
    MockArchivalUnit mau2 = new MockArchivalUnit();
    theDaemon.setHistoryRepository(new MockHistoryRepository(), mau1);
    theDaemon.setHistoryRepository(new MockHistoryRepository(), mau2);

    idmgr.signalAgreed(peer1, mau1);
    idmgr.signalAgreed(peer2, mau2);

    idmgr.signalDisagreed(peer1, mau2);

    Map expected = new HashMap();
    expected.put(peer2, new Long(10));

    assertEquals(expected, idmgr.getCachesToRepairFrom(mau2));

    expected = new HashMap();
    expected.put(peer1, new Long(10));
    assertEquals(expected, idmgr.getCachesToRepairFrom(mau1));
    } catch (UnknownHostException uhe) {
      fail(uhe.toString());
    }
  }

  public void testStoreIdentityAgreement() {
    try {
    MockArchivalUnit mau1 = new MockArchivalUnit();
    MockHistoryRepository hRep = new MockHistoryRepository();
    theDaemon.setHistoryRepository(hRep, mau1);

    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    peer2 = idmgr.stringToPeerIdentity("127.0.0.2");
    peer3 = idmgr.stringToPeerIdentity("127.0.0.3");

    idmgr.signalAgreed(peer1, mau1);
    idmgr.signalAgreed(peer2, mau1);

    idmgr.signalDisagreed(peer1, mau1);
    idmgr.signalDisagreed(peer3, mau1);

    Set expected = new HashSet(3);

    IdentityManager.IdentityAgreement ida =
        new IdentityManager.IdentityAgreement(peer1);
    ida.setLastAgree(10);
    ida.setLastDisagree(10);
    expected.add(ida);
    assertContains(hRep.getStoredIdentityAgreement(), ida);

    ida = new IdentityManager.IdentityAgreement(peer2);
    ida.setLastAgree(10);
    expected.add(ida);
    assertContains(hRep.getStoredIdentityAgreement(), ida);

    ida = new IdentityManager.IdentityAgreement(peer3);
    ida.setLastDisagree(10);
    expected.add(ida);
    assertContains(hRep.getStoredIdentityAgreement(), ida);

    //XXX figure out why this one doesn't work when the above do
//     assertSameElements(expected, hRep.getStoredIdentityAgreement());

    assertEquals(3, hRep.getStoredIdentityAgreement().size());
    } catch (UnknownHostException uhe) {
      fail(uhe.toString());
    }
  }

  public void testLoadIdentityAgreement() {
    try {
    MockArchivalUnit mau1 = new MockArchivalUnit();
    MockHistoryRepository hRep = new MockHistoryRepository();
    theDaemon.setHistoryRepository(hRep, mau1);

    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    peer2 = idmgr.stringToPeerIdentity("127.0.0.2");
    peer3 = idmgr.stringToPeerIdentity("127.0.0.3");

    List loadList = new ArrayList(3);

    IdentityManager.IdentityAgreement ida =
        new IdentityManager.IdentityAgreement(peer1);
    ida.setLastAgree(10);
    ida.setLastDisagree(10);
    loadList.add(ida);

    ida = new IdentityManager.IdentityAgreement(peer2);
    ida.setLastAgree(10);
    loadList.add(ida);

    ida = new IdentityManager.IdentityAgreement(peer3);
    ida.setLastDisagree(10);
    loadList.add(ida);

    hRep.setLoadedIdentityAgreement(loadList);

    Map expected = new HashMap();
    expected.put(peer2, new Long(10));

    assertEquals(expected, idmgr.getCachesToRepairFrom(mau1));

    expected = new HashMap();
    expected.put(peer1, new Long(10));
    expected.put(peer2, new Long(10));

    assertEquals(expected, idmgr.getAgreed(mau1));
    } catch (UnknownHostException uhe) {
      fail(uhe.toString());
    }
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestIdentityManager.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
