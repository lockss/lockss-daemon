package org.lockss.protocol;

import java.io.*;
import java.net.*;
import java.util.Random;
import org.mortbay.util.B64Code;
import junit.framework.TestCase;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;


/** JUnitTest case for class: org.lockss.protocol.Identity */
public class TestLcapIdentity extends TestCase {

  static String fakeIdString = "127.0.0.1";
  static LcapIdentity fakeId = null;
  InetAddress testAddress;
  int testReputation;
  Object testIdKey;
  LcapMessage testMsg= null;
  private static String urlstr = "http://www.test.org";
  private static String regexp = "*.doc";
  private static String pluginid = "testplugin 1.0";
  private static byte[] testbytes = {1,2,3,4,5,6,7,8,9,10};
  private static String[] testentries = {"test1.doc",
                                         "test2.doc", "test3.doc"};
  private static MockLockssDaemon daemon = new MockLockssDaemon(null);
  private static IdentityManager idmgr = daemon.getIdentityManager();

  public TestLcapIdentity(String _name) {
    super(_name);
  }

  /** setUp method for test case */
  protected void setUp() {
    try {
      fakeId = new LcapIdentity(InetAddress.getByName(fakeIdString));
      testAddress = InetAddress.getByName("127.0.0.1");
    }
    catch (UnknownHostException ex) {
      fail("can't open test host");
    }
    testReputation = IdentityManager.INITIAL_REPUTATION;
    testIdKey = LcapIdentity.makeIdKey(testAddress);
    try {
      testMsg = LcapMessage.makeRequestMsg(urlstr,
          regexp,
          testentries,
          testbytes,
          testbytes,
          LcapMessage.CONTENT_POLL_REQ,
          100000,
          fakeId,
          pluginid);
    }
    catch (Exception ex) {
      fail("message request creation failed.");
    }
  }

  /** test for method getIdentity(..) */
  public void testGetIdentity() {
    LcapIdentity id1 = idmgr.getIdentity(testAddress);
    assertTrue(id1 != null);
    // try and get the identity we just added
    LcapIdentity id2 = idmgr.findIdentity(id1.m_idKey);
    assertTrue(id2.isEqual(id1));
  }

  /** test for method getLocalIdentity(..) */
  public void testGetLocalIdentity() throws IOException {
    String host = "1.2.3.4";
    String prop = "org.lockss.localIPAddress="+host;
    TestConfiguration.
      setCurrentConfigFromUrlList(ListUtil.list(FileUtil.urlOfString(prop)));

    LcapIdentity ident = idmgr.getLocalIdentity();
    assertEquals(host, ident.toHost());
    assertNotNull(ident.getAddress());
    assertEquals(host, ident.getAddress().getHostAddress());
  }

  /** test for method isLocalIdentity(..) */
  public void testIsLocalIdentity() {
  }

  /** test for method isEqual(..) */
  public void testIsEqual() {
    LcapIdentity id1 = idmgr.getIdentity(testAddress);
    LcapIdentity id2 = idmgr.findIdentity(id1.m_idKey);
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
    String[] testCaseList = {TestLcapIdentity.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
