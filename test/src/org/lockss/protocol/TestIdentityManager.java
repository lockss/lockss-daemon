/*
 * $Id: TestIdentityManager.java,v 1.22.16.1 2004-02-03 01:03:39 tlipkis Exp $
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

import java.io.IOException;
import java.util.*;
import java.net.UnknownHostException;
import org.lockss.daemon.Configuration;
import org.lockss.daemon.TestConfiguration;
import org.lockss.util.*;
import org.lockss.test.*;
import java.io.File;

/** JUnitTest case for class: org.lockss.protocol.IdentityManager */
public class TestIdentityManager extends LockssTestCase {
  static String fakeIdString = "213.239.33.100";
  static LcapIdentity fakeId = null;
  IPAddr testAddress;
  int testReputation;
  Object testIdKey;
  private static String urlstr = "http://www.test.org";
  private static String lwrbnd = "test1.doc";
  private static String uprbnd = "test3.doc";
  private static byte[] testbytes = {1,2,3,4,5,6,7,8,9,10};
  private static String[] testentries = {"test1.doc",
                                         "test2.doc", "test3.doc"};

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

  public void testGetMapping() {
    assertNotNull(idmgr.getMapping());
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
  public void testActtackDetected() {
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
    }
    catch (ProtocolException ex) {
      fail("identity db store failed");
    }
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestIdentityManager.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
