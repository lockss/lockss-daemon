package org.lockss.protocol;

import java.io.IOException;
import java.util.Random;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.lockss.daemon.Configuration;
import org.lockss.daemon.TestConfiguration;
import org.lockss.util.*;
import org.lockss.test.*;

/** JUnitTest case for class: org.lockss.protocol.IdentityManager */
public class TestIdentityManager extends LockssTestCase {
  static String fakeIdString = "213.239.33.100";
  static LcapIdentity fakeId = null;
  InetAddress testAddress;
  int testReputation;
  Object testIdKey;
  private static String urlstr = "http://www.test.org";
  private static String lwrbnd = "test1.doc";
  private static String uprbnd = "test3.doc";
  private static byte[] testbytes = {1,2,3,4,5,6,7,8,9,10};
  private static String[] testentries = {"test1.doc",
                                         "test2.doc", "test3.doc"};
  private static String pluginid = "testplugin 1.0";

  private static MockLockssDaemon daemon = new MockLockssDaemon(null);

  static {
    configParams("/tmp/iddb", "src/org/lockss/protocol");
  }
  private static IdentityManager idmgr = daemon.getIdentityManager();

  public TestIdentityManager(String _name) {
    super(_name);
  }

  protected void setUp() throws Exception {
    super.setUp();
    try {
      fakeId = idmgr.findIdentity(LcapIdentity.stringToAddr(fakeIdString));
      testAddress = InetAddress.getByName("127.0.0.1");
    }
    catch (UnknownHostException ex) {
      fail("can't open test host");
    }
    testReputation = IdentityManager.INITIAL_REPUTATION;
    testIdKey = LcapIdentity.makeIdKey(testAddress);
  }

  /** test for method getIdentity(..) */
  public void testFindIdentity() {
    try {
      assertNotNull(fakeId);
      assertEquals(fakeId, idmgr.findIdentity(fakeId.getAddress()));
    }
    catch (UnknownHostException ex) {
      fail("Invalid host:" + fakeId);
    }

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

  public static void configParams(String dbDir, String mapDir) {
    configParams(dbDir, mapDir, null);
  }
  public static void configParams(String dbDir, String mapDir, String addParams) {
    String s = IdentityManager.PARAM_IDDB_DIR + "=" + dbDir;
    String s2 = IdentityManager.PARAM_IDDB_MAP_DIR + "=" + mapDir;
    String configStr = s + "\n" + s2;
    if (addParams!=null) {
      configStr += "\n" + addParams;
    }

    try {
      TestConfiguration.setCurrentConfigFromString(configStr);
    } catch (IOException ex) {
      fail("Unable to initialize configuration parameters.");
    }
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestIdentityManager.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
