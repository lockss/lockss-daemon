/*
 * $Id: TestIdentityManager.java,v 1.32 2004-12-06 21:58:54 tlipkis Exp $
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
import org.lockss.daemon.status.*;
import org.lockss.poller.*;
import org.lockss.repository.*;
import org.lockss.test.*;

/** Test cases for org.lockss.protocol.IdentityManager that assume the
 * IdentityManager has been initialized.  See TestIdentityManagerInit for
 * more IdentityManager tests. */
public class TestIdentityManager extends LockssTestCase {
  Object testIdKey;

  private MockLockssDaemon theDaemon;
  private TestableIdentityManager idmgr;
  PeerIdentity peer1;
  PeerIdentity peer2;
  PeerIdentity peer3;

  private static final String LOCAL_IP = "127.1.2.3";

  public void setUp() throws Exception {
    super.setUp();

    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties p = new Properties();
    p.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, LOCAL_IP);
    ConfigurationUtil.setCurrentConfigFromProps(p);

    theDaemon = getMockLockssDaemon();
    idmgr = new TestableIdentityManager();
    idmgr.initService(theDaemon);
    theDaemon.setIdentityManager(idmgr);
    idmgr.startService();
  }

  public void tearDown() throws Exception {
    if (idmgr != null) {
      idmgr.stopService();
      idmgr = null;
    }
    super.tearDown();
  }

  /** test for method stringToPeerIdentity **/
  public void testStringToPeerIdentity() throws Exception {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    assertNotNull(peer1);
    assertSame(peer1, idmgr.stringToPeerIdentity("127.0.0.1"));
    peer2 = idmgr.stringToPeerIdentity("127.0.0.2");
    assertNotNull(peer2);
    assertNotEquals(peer1, peer2);
    peer3 = idmgr.stringToPeerIdentity("127.0.0.2:300");
    assertNotNull(peer3);
    assertNotEquals(peer3, peer2);
    assertNotEquals(peer3, peer1);
  }

  // XXX this should go away
  public void testStringToPeerIdentityNull() throws Exception {
    peer1 = idmgr.stringToPeerIdentity(null);
    assertTrue(peer1.isLocalIdentity());
    assertSame(peer1, idmgr.getLocalPeerIdentity(Poll.V1_POLL));
  }

  /** test for method ipAddrToPeerIdentity **/
  public void testIPAddrToPeerIdentity() throws Exception {
    IPAddr ip1 = IPAddr.getByName("127.0.0.1");
    IPAddr ip2 = IPAddr.getByName("127.0.0.2");
    IPAddr ip3 = IPAddr.getByName("127.0.0.3");

    peer1 = idmgr.ipAddrToPeerIdentity(ip1);
    assertNotNull(peer1);
    assertSame(peer1, idmgr.ipAddrToPeerIdentity(ip1));
    assertSame(peer1, idmgr.stringToPeerIdentity("127.0.0.1"));
    peer2 = idmgr.ipAddrToPeerIdentity(ip2);
    assertNotNull(peer2);
    assertNotEquals(peer1, peer2);
    assertSame(peer2, idmgr.stringToPeerIdentity("127.0.0.2"));
    peer3 = idmgr.ipAddrToPeerIdentity(ip2, 300);
    assertNotNull(peer3);
    assertNotEquals(peer3, peer2);
    assertNotEquals(peer3, peer1);
    assertSame(peer3, idmgr.stringToPeerIdentity("127.0.0.2:300"));
  }

  // XXX this should go away
  public void testIPAddrToPeerIdentityNull() throws Exception {
    peer1 = idmgr.ipAddrToPeerIdentity(null);
    assertTrue(peer1.isLocalIdentity());
    assertSame(peer1, idmgr.getLocalPeerIdentity(Poll.V1_POLL));
  }

  public void testIdentityToIPAddr() throws Exception {
    IPAddr ip1 = IPAddr.getByName("127.0.0.1");
    IPAddr ip2 = IPAddr.getByName("127.0.0.2");
    peer1 = idmgr.ipAddrToPeerIdentity(ip1);
    peer2 = idmgr.ipAddrToPeerIdentity(ip2);

    assertEquals(ip1, idmgr.identityToIPAddr(peer1));
    assertEquals(ip2, idmgr.identityToIPAddr(peer2));
  }

  public void testIdentityToIPAddrV3() throws Exception {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1:23");
    try {
      idmgr.identityToIPAddr(peer1);
      fail("identityToIPAddr(V3PeerId) should throw");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testGetLocalIdentity() {
    peer1 = idmgr.getLocalPeerIdentity(Poll.V1_POLL);
    assertTrue(peer1.isLocalIdentity());
    assertTrue(idmgr.isLocalIdentity(peer1));
    String key = peer1.getIdString();
    assertTrue(idmgr.isLocalIdentity(key));
  }

  public void testGetLocalIdentityIll() {
    try {
      peer1 = idmgr.getLocalPeerIdentity(Poll.MAX_POLL_VERSION + 32);
      fail("getLocalPeerIdentity(" + (Poll.MAX_POLL_VERSION + 32) +
	   ") should throw");
    } catch (IllegalArgumentException e) {
    }
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
  public void testAgreeWithVote() throws Exception {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    int rep = idmgr.getReputation(peer1);
    idmgr.changeReputation(peer1, IdentityManager.AGREE_VOTE);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.AGREE_VOTE],
                    idmgr.getReputation(peer1));
  }

  /** test for method disagreeWithVote(..) */
  public void testDisagreeWithVote() throws Exception {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    int rep = idmgr.getReputation(peer1);
    idmgr.changeReputation(peer1,IdentityManager.DISAGREE_VOTE);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.DISAGREE_VOTE],
                    idmgr.getReputation(peer1));
  }

  /** test for method callInternalPoll(..) */
  public void testCallInternalPoll() throws Exception {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    int rep = idmgr.getReputation(peer1);
    idmgr.changeReputation(peer1,IdentityManager.CALL_INTERNAL);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.CALL_INTERNAL],
                    idmgr.getReputation(peer1));
  }

  /** test for method spoofDetected(..) */
  public void testSpoofDetected() throws Exception {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    int rep = idmgr.getReputation(peer1);
    idmgr.changeReputation(peer1,IdentityManager.SPOOF_DETECTED);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.SPOOF_DETECTED],
                    idmgr.getReputation(peer1));
  }

  /** test for method replayDected(..) */
  public void testReplayDected() throws Exception {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    int rep = idmgr.getReputation(peer1);
    idmgr.changeReputation(peer1,IdentityManager.REPLAY_DETECTED);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.REPLAY_DETECTED],
                    idmgr.getReputation(peer1));
  }

  /** test for method acttackDetected(..) */
  public void testAttackDetected() throws Exception {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    int rep = idmgr.getReputation(peer1);
    idmgr.changeReputation(peer1,IdentityManager.ATTACK_DETECTED);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.ATTACK_DETECTED],
                    idmgr.getReputation(peer1));
  }

  /** test for method voteNotVerify(..) */
  public void testVoteNotVerify() throws Exception {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    int rep = idmgr.getReputation(peer1);
    idmgr.changeReputation(peer1,IdentityManager.VOTE_NOTVERIFIED);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.VOTE_NOTVERIFIED],
                    idmgr.getReputation(peer1));
  }

  /** test for method voteVerify(..) */
  public void testVoteVerify() throws Exception {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    int rep = idmgr.getReputation(peer1);
    idmgr.changeReputation(peer1,IdentityManager.VOTE_VERIFIED);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.VOTE_VERIFIED],
                    idmgr.getReputation(peer1));
  }

  /** test for method voteDisown(..) */
  public void testVoteDisown() throws Exception {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    int rep = idmgr.getReputation(peer1);
    idmgr.changeReputation(peer1,IdentityManager.VOTE_DISOWNED);
    checkReputation(rep, idmgr.reputationDeltas[IdentityManager.VOTE_DISOWNED],
                    idmgr.getReputation(peer1));
  }

  public void testStoreIdentities() throws Exception {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    peer2 = idmgr.stringToPeerIdentity("127.0.0.2");
    peer3 = idmgr.stringToPeerIdentity("127.0.0.3");

    idmgr.storeIdentities();
  }

  public void testSetAgreedThrowsOnNullAu() throws Exception {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    try {
      idmgr.signalAgreed(peer1, null);
      fail("Should have thrown on a null au");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testSetAgreedThrowsOnNullId() throws Exception {
    MockArchivalUnit mau = new MockArchivalUnit();
    try {
      idmgr.signalAgreed(null, mau);
      fail("Should have thrown on a null au");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testSetDisagreedThrowsOnNullAu() throws Exception {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    try {
      idmgr.signalDisagreed(peer1, null);
      fail("Should have thrown on a null au");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testSetDisagreedThrowsOnNullId() throws Exception {
    MockArchivalUnit mau = new MockArchivalUnit();
    try {
      idmgr.signalDisagreed(null, mau);
      fail("Should have thrown on a null au");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testGetAgreeThrowsOnNullAu() throws Exception {
    try {
      idmgr.getAgreed(null);
      fail("Should have thrown on a null au");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testGetAgreedNoneSet() throws Exception {
    MockArchivalUnit mau = new MockArchivalUnit();
    theDaemon.setHistoryRepository(new MockHistoryRepository(), mau);
    assertNull(idmgr.getAgreed(mau));
  }

  public void testGetAgreed() throws Exception {
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
  }

  public void testDisagreeDoesntRemove() throws Exception {
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
  }

  public void testGetCachesToRepairFromThrowsOnNullAu()
      throws Exception {
    try {
      idmgr.getCachesToRepairFrom(null);
      fail("Should have thrown on a null au");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testGetCachesToRepairFromNoneSet() throws Exception {
    MockArchivalUnit mau = new MockArchivalUnit();
    theDaemon.setHistoryRepository(new MockHistoryRepository(), mau);
    assertNull(idmgr.getCachesToRepairFrom(mau));
  }

  public void testCachesToRepairFrom() throws Exception {
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
  }

  public void testAgreeUpdatesTime() throws Exception {
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
  }

  public void testDisagreeNullsMap() throws Exception {
    TimeBase.setSimulated(10);
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");

    MockArchivalUnit mau = new MockArchivalUnit();
    theDaemon.setHistoryRepository(new MockHistoryRepository(), mau);
    idmgr.signalAgreed(peer1, mau);

    idmgr.signalDisagreed(peer1, mau);

    Map expected = new HashMap();

    assertNull(idmgr.getCachesToRepairFrom(mau));
  }

  public void testDisagreeRemoves() throws Exception {
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
  }

  public void testMultipleAus() throws Exception {
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
  }

  public void testIdentityAgreementEquals() {
    IdentityManager.IdentityAgreement a1 = new MyIdentityAgreement("id1");
    IdentityManager.IdentityAgreement a2 = new MyIdentityAgreement("id1");
    IdentityManager.IdentityAgreement a3 = new MyIdentityAgreement("id3");
    assertEquals(a1, a2);
    assertNotEquals(a1, a3);
    a1.setLastAgree(8);
    assertNotEquals(a1, a2);
    a2.setLastAgree(8);
    assertEquals(a1, a2);
    a1.setLastDisagree(12);
    assertNotEquals(a1, a2);
    a2.setLastDisagree(12);
    assertEquals(a1, a2);
  }    

  public void testIdentityAgreementHash() {
    IdentityManager.IdentityAgreement a1 = new MyIdentityAgreement("id1");
    IdentityManager.IdentityAgreement a2 = new MyIdentityAgreement("id1");
    assertEquals(a1.hashCode(), a2.hashCode());
    a1.setLastAgree(8);
    a2.setLastAgree(8);
    a1.setLastDisagree(12);
    a2.setLastDisagree(12);
    assertEquals(a1.hashCode(), a2.hashCode());
  }    

  static class MyIdentityAgreement extends IdentityManager.IdentityAgreement {
    MyIdentityAgreement(String id) {
      super();
      setId(id);
    }
  }

  public void testStoreIdentityAgreement() throws Exception {
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
  }

  public void testLoadIdentityAgreement() throws Exception {
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
  }
  /**
   * Tests that the IP address info fed to the IdentityManagerStatus object
   * looks like an IP address (x.x.x.x)
   */

  public void testStatusInterface() throws Exception {
    peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
    peer2 = idmgr.stringToPeerIdentity("127.0.0.2");

    MockArchivalUnit mau = new MockArchivalUnit();

    idmgr.signalAgreed(peer1, mau);
    idmgr.signalAgreed(peer2, mau);
    
    Map idMap = idmgr.getIdentityMap();
    Set expectedAddresses = new HashSet();
    expectedAddresses.add("127.0.0.1");
    expectedAddresses.add("127.0.0.2");
    expectedAddresses.add(LOCAL_IP);

    for (Iterator iter = idMap.values().iterator();
	 iter.hasNext();) {
      LcapIdentity id = (LcapIdentity)iter.next();
      assertTrue(expectedAddresses.contains(id.getPeerIdentity().getIdString()));
    }
    
    assertEquals(expectedAddresses.size(), idMap.size()); //2 above,plus me
  }

  private class TestableIdentityManager extends IdentityManager {
    Map identities = null;

    public Map getIdentityMap() {
      return identities;
    }

    protected IdentityManagerStatus makeStatusAccessor(Map identities) {
      this.identities = identities;
      return new MockIdentityManagerStatus();
    }
  }

  private static class MockIdentityManagerStatus
    extends IdentityManagerStatus {
    public MockIdentityManagerStatus() {
      super(null);
    }
    public String getDisplayName() {
      throw new UnsupportedOperationException();
    }
    
    public void populateTable(StatusTable table) {
      throw new UnsupportedOperationException();
    }

    public boolean requiresKey() {
      throw new UnsupportedOperationException();
    }
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestIdentityManager.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
