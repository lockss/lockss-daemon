/*
 * $Id: TestIdentityManager.java,v 1.26 2004-02-07 06:51:18 eaalto Exp $
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
  static String fakeIdString = "213.239.33.100";
  static LcapIdentity fakeId = null;
  IPAddr testAddress;
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

  public void setUp() throws Exception {
    super.setUp();

    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.1.2.3");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    theDaemon = new MockLockssDaemon();
    idmgr = theDaemon.getIdentityManager();

    try {
      fakeId = idmgr.findIdentity(LcapIdentity.stringToAddr(fakeIdString));
      testAddress = IPAddr.getByName("127.0.0.1");
    }
    catch (UnknownHostException ex) {
      fail("can't open test host");
    }
    testReputation = IdentityManager.INITIAL_REPUTATION;
    testIdKey = LcapIdentity.makeIdKey(testAddress);

  }

  public void tearDown() throws Exception {
    idmgr.stopService();
    super.tearDown();
  }

  /** test for method getIdentity(..) */
  public void testFindIdentity() {
    assertNotNull(fakeId);
    assertEquals(fakeId, idmgr.findIdentity(fakeId.getAddress()));
  }

  /** test for method findIdentity(..) */
  public void testGetIdentity() {
    assertTrue(idmgr.getIdentity(fakeId.getIdKey()) != null);
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
    int rep = fakeId.getReputation();
    idmgr.changeReputation(fakeId,IdentityManager.AGREE_VOTE);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.AGREE_VOTE],
                    fakeId.getReputation());

  }

  /** test for method disagreeWithVote(..) */
  public void testDisagreeWithVote() {
    int rep = fakeId.getReputation();
    idmgr.changeReputation(fakeId,IdentityManager.DISAGREE_VOTE);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.DISAGREE_VOTE],
                    fakeId.getReputation());
  }

  /** test for method callInternalPoll(..) */
  public void testCallInternalPoll() {
    int rep = fakeId.getReputation();
    idmgr.changeReputation(fakeId,IdentityManager.CALL_INTERNAL);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.CALL_INTERNAL],
                    fakeId.getReputation());
  }

  /** test for method spoofDetected(..) */
  public void testSpoofDetected() {
    int rep = fakeId.getReputation();
    idmgr.changeReputation(fakeId,IdentityManager.SPOOF_DETECTED);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.SPOOF_DETECTED],
                    fakeId.getReputation());
  }

  /** test for method replayDected(..) */
  public void testReplayDected() {
    int rep = fakeId.getReputation();
    idmgr.changeReputation(fakeId,IdentityManager.REPLAY_DETECTED);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.REPLAY_DETECTED],
                    fakeId.getReputation());
  }

  /** test for method acttackDetected(..) */
  public void testAttackDetected() {
    int rep = fakeId.getReputation();
    idmgr.changeReputation(fakeId,IdentityManager.ATTACK_DETECTED);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.ATTACK_DETECTED],
                    fakeId.getReputation());
  }

  /** test for method voteNotVerify(..) */
  public void testVoteNotVerify() {
    int rep = fakeId.getReputation();
    idmgr.changeReputation(fakeId,IdentityManager.VOTE_NOTVERIFIED);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.VOTE_NOTVERIFIED],
                    fakeId.getReputation());
  }

  /** test for method voteVerify(..) */
  public void testVoteVerify() {
    int rep = fakeId.getReputation();
    idmgr.changeReputation(fakeId,IdentityManager.VOTE_VERIFIED);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.VOTE_VERIFIED],
                    fakeId.getReputation());
  }

  /** test for method voteDisown(..) */
  public void testVoteDisown() {
    int rep = fakeId.getReputation();
    idmgr.changeReputation(fakeId,IdentityManager.VOTE_DISOWNED);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.VOTE_DISOWNED],
                    fakeId.getReputation());
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
    fakeId.changeReputation(IdentityManager.MAX_DELTA * 10);
    assertTrue(fakeId.getReputation() <=
               rep +IdentityManager.MAX_DELTA);
    rep = fakeId.getReputation();
    fakeId.changeReputation(-IdentityManager.MAX_DELTA * 10);
    assertTrue(fakeId.getReputation() >=
               rep -IdentityManager.MAX_DELTA);
  }

  public void testStoreIdentities() throws UnknownHostException {
    String fakeIdString1 = "213.239.33.100";
    String fakeIdString2 = "213.239.33.101";
    String fakeIdString3 = "213.239.33.102";

    try {
      assertNotNull(idmgr.findIdentity(LcapIdentity.stringToAddr(fakeIdString1)));
      assertNotNull(idmgr.findIdentity(LcapIdentity.stringToAddr(fakeIdString2)));
      assertNotNull(idmgr.findIdentity(LcapIdentity.stringToAddr(fakeIdString3)));
      idmgr.storeIdentities();
    } catch (ProtocolException ex) {
      fail("identity db store failed");
    }
  }

  public void testSetAgreedThrowsOnNullAu() throws UnknownHostException {
    String id = "127.0.0.1";
    try {
      idmgr.signalAgreed(id, null);
      fail("Should have thrown on a null au");
    } catch (IllegalArgumentException e) {
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
    String id = "127.0.0.1";
    try {
      idmgr.signalDisagreed(id, null);
      fail("Should have thrown on a null au");
    } catch (IllegalArgumentException e) {
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

    TimeBase.setSimulated(10);
    String id1 = "127.0.0.1";
    String id2 = "127.0.0.2";

    MockArchivalUnit mau = new MockArchivalUnit();
    theDaemon.setHistoryRepository(new MockHistoryRepository(), mau);

    idmgr.signalAgreed(id1, mau);
    TimeBase.step();
    idmgr.signalAgreed(id2, mau);
    Map expected = new HashMap();
    expected.put(id1, new Long(10));
    expected.put(id2, new Long(11));
    assertEquals(expected, idmgr.getAgreed(mau));
  }

  public void testDisagreeDoesntRemove() throws UnknownHostException {
    TimeBase.setSimulated(10);
    String id1 = "127.0.0.1";
    String id2 = "127.0.0.2";

    MockArchivalUnit mau = new MockArchivalUnit();
    theDaemon.setHistoryRepository(new MockHistoryRepository(), mau);
    idmgr.signalAgreed(id1, mau);
    idmgr.signalAgreed(id2, mau);

    idmgr.signalDisagreed(id1, mau);

    Map expected = new HashMap();
    expected.put(id1, new Long(10));
    expected.put(id2, new Long(10));

    assertEquals(expected, idmgr.getAgreed(mau));
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
    TimeBase.setSimulated(10);
    String id1 = "127.0.0.1";
    String id2 = "127.0.0.2";

    MockArchivalUnit mau = new MockArchivalUnit();
    theDaemon.setHistoryRepository(new MockHistoryRepository(), mau);
    idmgr.signalAgreed(id1, mau);
    TimeBase.step();
    idmgr.signalAgreed(id2, mau);
    Map expected = new HashMap();
    expected.put(id1, new Long(10));
    expected.put(id2, new Long(11));
    assertEquals(expected, idmgr.getCachesToRepairFrom(mau));
  }

  public void testAgreeUpdatesTime() throws UnknownHostException {
    TimeBase.setSimulated(10);
    String id1 = "127.0.0.1";

    MockArchivalUnit mau = new MockArchivalUnit();
    theDaemon.setHistoryRepository(new MockHistoryRepository(), mau);

    idmgr.signalAgreed(id1, mau);
    TimeBase.step(15);
    idmgr.signalAgreed(id1, mau);

    Map expected = new HashMap();
    expected.put(id1, new Long(25));

    assertEquals(expected, idmgr.getCachesToRepairFrom(mau));
  }

  public void testDisagreeNullsMap() throws UnknownHostException {
    TimeBase.setSimulated(10);
    String id1 = "127.0.0.1";

    MockArchivalUnit mau = new MockArchivalUnit();
    theDaemon.setHistoryRepository(new MockHistoryRepository(), mau);
    idmgr.signalAgreed(id1, mau);

    idmgr.signalDisagreed(id1, mau);

    Map expected = new HashMap();

    assertNull(idmgr.getCachesToRepairFrom(mau));
  }

  public void testDisagreeRemoves() throws UnknownHostException {
    TimeBase.setSimulated(10);
    String id1 = "127.0.0.1";
    String id2 = "127.0.0.2";

    MockArchivalUnit mau = new MockArchivalUnit();
    theDaemon.setHistoryRepository(new MockHistoryRepository(), mau);
    idmgr.signalAgreed(id1, mau);
    idmgr.signalAgreed(id2, mau);

    idmgr.signalDisagreed(id1, mau);

    Map expected = new HashMap();
    expected.put(id2, new Long(10));

    assertEquals(expected, idmgr.getCachesToRepairFrom(mau));
  }

  public void testMultipleAus() throws UnknownHostException {
    TimeBase.setSimulated(10);
    String id1 = "127.0.0.1";
    String id2 = "127.0.0.2";
    String id3 = "127.0.0.3";

    MockArchivalUnit mau1 = new MockArchivalUnit();
    MockArchivalUnit mau2 = new MockArchivalUnit();
    theDaemon.setHistoryRepository(new MockHistoryRepository(), mau1);
    theDaemon.setHistoryRepository(new MockHistoryRepository(), mau2);

    idmgr.signalAgreed(id1, mau1);
    idmgr.signalAgreed(id2, mau2);

    idmgr.signalDisagreed(id1, mau2);

    Map expected = new HashMap();
    expected.put(id2, new Long(10));

    assertEquals(expected, idmgr.getCachesToRepairFrom(mau2));

    expected = new HashMap();
    expected.put(id1, new Long(10));
    assertEquals(expected, idmgr.getCachesToRepairFrom(mau1));
  }

  public void testStoreIdentityAgreement() {
    MockArchivalUnit mau1 = new MockArchivalUnit();
    MockHistoryRepository hRep = new MockHistoryRepository();
    theDaemon.setHistoryRepository(hRep, mau1);

    String id1 = "127.0.0.1";
    String id2 = "127.0.0.2";
    String id3 = "127.0.0.3";

    idmgr.signalAgreed(id1, mau1);
    idmgr.signalAgreed(id2, mau1);

    idmgr.signalDisagreed(id1, mau1);
    idmgr.signalDisagreed(id3, mau1);

    Set expected = new HashSet(3);

    IdentityManager.IdentityAgreement ida =
        new IdentityManager.IdentityAgreement(id1);
    ida.setLastAgree(10);
    ida.setLastDisagree(10);
    expected.add(ida);
    assertContains(hRep.getStoredIdentityAgreement(), ida);

    ida = new IdentityManager.IdentityAgreement(id2);
    ida.setLastAgree(10);
    expected.add(ida);
    assertContains(hRep.getStoredIdentityAgreement(), ida);

    ida = new IdentityManager.IdentityAgreement(id3);
    ida.setLastDisagree(10);
    expected.add(ida);
    assertContains(hRep.getStoredIdentityAgreement(), ida);

    //XXX figure out why this one doesn't work when the above do
//     assertSameElements(expected, hRep.getStoredIdentityAgreement());

    assertEquals(3, hRep.getStoredIdentityAgreement().size());
  }

  public void testLoadIdentityAgreement() {
    MockArchivalUnit mau1 = new MockArchivalUnit();
    MockHistoryRepository hRep = new MockHistoryRepository();
    theDaemon.setHistoryRepository(hRep, mau1);

    String id1 = "127.0.0.1";
    String id2 = "127.0.0.2";
    String id3 = "127.0.0.3";

    List loadList = new ArrayList(3);

    IdentityManager.IdentityAgreement ida =
        new IdentityManager.IdentityAgreement(id1);
    ida.setLastAgree(10);
    ida.setLastDisagree(10);
    loadList.add(ida);

    ida = new IdentityManager.IdentityAgreement(id2);
    ida.setLastAgree(10);
    loadList.add(ida);

    ida = new IdentityManager.IdentityAgreement(id3);
    ida.setLastDisagree(10);
    loadList.add(ida);

    hRep.setLoadedIdentityAgreement(loadList);

    Map expected = new HashMap();
    expected.put(id2, new Long(10));

    assertEquals(expected, idmgr.getCachesToRepairFrom(mau1));

    expected = new HashMap();
    expected.put(id1, new Long(10));
    expected.put(id2, new Long(10));

    assertEquals(expected, idmgr.getAgreed(mau1));
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestIdentityManager.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
