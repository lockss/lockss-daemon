/*
 * $Id: TestLcapIdentity.java,v 1.34 2006-01-12 00:48:38 tlipkis Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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
import java.net.*;
import org.mortbay.util.B64Code;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.poller.*;

import java.util.*;


/** JUnitTest case for class: org.lockss.protocol.Identity */
public class TestLcapIdentity extends LockssTestCase {

  static final String fakeIdString = "127.0.0.1";
  static final String urlstr = "http://www.test.org";
  static final String lwrbnd = "test1.doc";
  static final String uprbnd = "test3.doc";
  static final String archivalid = "testarchive 1.0";
  static final byte[] testbytes = {1,2,3,4,5,6,7,8,9,10};
  static final ArrayList testentries =
    (ArrayList)ListUtil.list(new PollTally.NameListEntry(true,"test1.doc"),
			     new PollTally.NameListEntry(true,"test2.doc"),
			     new PollTally.NameListEntry(true,"test3.doc"));

  LcapIdentity fakeId = null;
  IPAddr testAddress;
  int testReputation;
  PeerIdentity testID;
  LcapMessage testMsg= null;
  private MockLockssDaemon daemon;
  private IdentityManager idmgr;

  /** setUp method for test case */
  protected void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();
    String host = "1.2.3.4";
    String prop = "org.lockss.localIPAddress="+host;
    ConfigurationUtil.
      setCurrentConfigFromUrlList(ListUtil.list(FileTestUtil.urlOfString(prop)));
    idmgr = daemon.getIdentityManager();
    testID = idmgr.stringToPeerIdentity(fakeIdString);
    try {
      fakeId = new LcapIdentity(testID, fakeIdString);
      testAddress = IPAddr.getByName(fakeIdString);
    }
    catch (UnknownHostException ex) {
      fail("can't open test host");
    }
    testReputation = IdentityManager.INITIAL_REPUTATION;
    PollSpec spec = new MockPollSpec(archivalid, urlstr, lwrbnd, uprbnd,
				     Poll.V1_CONTENT_POLL);
    testMsg = V1LcapMessage.makeRequestMsg(spec,
					   testentries,
					   testbytes,
					   testbytes,
					   V1LcapMessage.CONTENT_POLL_REQ,
					   100000,
					   testID);
  }

  /** test for method toString(..) */
  public void testToString() {
    String s = fakeId.toString();
    assertTrue(s.equals((String)fakeId.m_idKey));
  }

  /** test for method rememberActive(..) */
  public void testRememberActive() {
    // XXX: Stubbed for V3
    if (testMsg instanceof V1LcapMessage) {
      long inc_pkts = fakeId.m_incrPackets;
      long tot_pkts = fakeId.m_totalPackets;

      fakeId.rememberActive(false,testMsg);
      assertEquals(inc_pkts + 1, fakeId.m_incrPackets);
      assertEquals(tot_pkts + 1, fakeId.m_totalPackets);
      String verifier = String.valueOf(B64Code.encode(((V1LcapMessage)testMsg).getVerifier()));
      Integer cnt = (Integer)fakeId.m_pktsThisInterval.get(verifier);
      assertNotNull(cnt);
      assertEquals(1, cnt.intValue());
    }
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

  public void testSerializationRoundtrip() throws Exception {
    ObjectSerializer serializer = new XStreamSerializer();
    ObjectSerializer deserializer = new XStreamSerializer(daemon);

    File temp1 = File.createTempFile("tmp", ".xml");
    temp1.deleteOnExit();
    PeerIdentity pidv1 = new PeerIdentity("12.34.56.78");
    LcapIdentity lid1 = new LcapIdentity(pidv1, "12.34.56.78");
    serializer.serialize(temp1, lid1);
    LcapIdentity back1 = (LcapIdentity)deserializer.deserialize(temp1);
    assertTrue(lid1.isEqual(back1));
    assertEquals(lid1.m_address.getAddress(),
                 back1.m_address.getAddress());

    File temp3 = File.createTempFile("tmp", ".xml");
    temp3.deleteOnExit();
    PeerIdentity pidv3 =
      new PeerIdentity(IDUtil.ipAddrToKey("87.65.43.21", "999"));
    LcapIdentity lid3 =
      new LcapIdentity(pidv3, IDUtil.ipAddrToKey("87.65.43.21", "999"));
    serializer.serialize(temp3, lid3);
    LcapIdentity back3 = (LcapIdentity)deserializer.deserialize(temp3);
    assertTrue(lid3.isEqual(back3));
    assertEquals(lid3.m_address.getAddress(),
                 back3.m_address.getAddress());
  }

  public void testFindLcapIdentity() throws Exception {
    PeerIdentity pidv1 = new PeerIdentity("12.34.56.78");
    LcapIdentity lid1 = idmgr.findLcapIdentity(pidv1, "12.34.56.78");
    LcapIdentity lid2 = idmgr.findLcapIdentity(pidv1, "12.34.56.78");
    assertSame(lid1, lid2);
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestLcapIdentity.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
