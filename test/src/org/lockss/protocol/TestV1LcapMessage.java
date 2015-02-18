/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.DataInputStream;
import org.lockss.util.Logger;
import org.lockss.test.MockCachedUrlSetSpec;
import java.net.*;
import java.io.*;
import java.util.*;
import org.lockss.test.*;
import org.lockss.config.*;
import org.lockss.poller.*;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.util.*;
import org.lockss.app.LockssDaemon;

/** JUnitTest case for class: org.lockss.protocol.Message */
public class TestV1LcapMessage extends LockssTestCase {

  private static String urlstr = "http://www.example.com";
  private static byte[] testbytes = {1,2,3,4,5,6,7,8,9,10};
  private static String lwrbnd = "test1.doc";
  private static String uprbnd = "test3.doc";
  protected static String archivalID = "TestAU_1.0";

  private ArrayList testentries;
  protected IPAddr testaddr;
  protected PeerIdentity testID;
  protected V1LcapMessage testmsg;
  private LockssDaemon theDaemon;


  public void setUp() throws Exception {
    super.setUp();
    theDaemon = getMockLockssDaemon();
    String tempDirPath = null;
    try {
      tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    }
    catch (IOException ex) {
      fail("unable to create a temporary directory");
    }

    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    IdentityManager idmgr = theDaemon.getIdentityManager();
    idmgr.startService();
    try {
      testaddr = IPAddr.getByName("127.0.0.1");
      testID = idmgr.stringToPeerIdentity("127.0.0.1");
    }
    catch (UnknownHostException ex) {
      fail("can't open test host");
    }
    try {
      testmsg = new V1LcapMessage();
    }
    catch (IOException ex) {
      fail("can't create test message");
    }
    // assign the data
    testmsg.m_targetUrl = urlstr;
    testmsg.m_lwrBound = lwrbnd;
    testmsg.m_uprBound = uprbnd;

    testmsg.m_originatorID = testID;
    testmsg.m_hashAlgorithm = V1LcapMessage.getDefaultHashAlgorithm();
    testmsg.m_startTime = 123456789;
    testmsg.m_stopTime = 987654321;
    testmsg.m_multicast = false;
    testmsg.m_hopCount = 2;

    testmsg.m_challenge = testbytes;
    testmsg.m_verifier = testbytes;
    testmsg.m_hashed = testbytes;
    testmsg.m_opcode = V1LcapMessage.CONTENT_POLL_REQ;
    testmsg.m_entries = testentries = TestPoll.makeEntries(1, 25);
    testmsg.m_archivalID = archivalID;
    testmsg.m_pluginVersion = "PlugVer42";
  }

  private boolean hasSameEntries(List entries1, List entries2) {
    if(entries1.size() != entries2.size()) return false;

    for(int i=0; i< entries1.size(); i++) {
      PollTally.NameListEntry entry1 = (PollTally.NameListEntry)entries1.get(i);
      PollTally.NameListEntry entry2 = (PollTally.NameListEntry)entries2.get(i);
      if(!entry1.equals(entry2) ||
	 (entry1.hasContent != entry2.hasContent))
	return false;
    }
    return true;
  }

  public void testEntriesTranslation() {
    String encstr = testmsg.entriesToString(10000);
    ArrayList decoded = testmsg.stringToEntries(encstr);
    assertTrue(hasSameEntries(testmsg.m_entries,decoded));

    //test our entries remainder by artificially setting our size to very small
    encstr = testmsg.entriesToString(50);
    decoded = testmsg.stringToEntries(encstr);
    assertNotNull(decoded);
    assertNotNull(testmsg.m_lwrRem);
    assertNotNull(testmsg.m_uprRem);

    encstr = testmsg.entriesToString(0);
    decoded = testmsg.stringToEntries(encstr);
    assertNull(decoded);
    assertEquals(((PollTally.NameListEntry)testentries.get(0)).name,
		 testmsg.m_lwrRem);
    assertEquals(testmsg.m_uprBound, testmsg.m_uprRem);

  }

  public void testStorePropsWithLargeNumberOfEntries() throws IOException {
    int original_maxsize = testmsg.m_maxSize;
    testmsg.m_entries = TestPoll.makeEntries(1,20000);
    testmsg.storeProps();

    assertNotNull(testmsg.m_lwrRem);
    assertNotNull(testmsg.m_uprRem);
    assertNotEquals(original_maxsize, testmsg.m_maxSize);
  }

  public void testNoOpMessageCreation() throws Exception {
    V1LcapMessage noop_msg = null;

    noop_msg = V1LcapMessage.makeNoOpMsg(testID, testbytes);

    // now check the fields we expect to be valid
    assertTrue(testID == noop_msg.m_originatorID);
    assertEquals(V1LcapMessage.NO_OP, noop_msg.m_opcode);
    assertEquals(testbytes, noop_msg.m_verifier);
  }

  public void testNoOpMessageToString() throws IOException {
    V1LcapMessage noop_msg = V1LcapMessage.makeNoOpMsg(testID, testbytes);

    noop_msg.toString();
  }

  public void testNoOpEncoding() throws Exception {

    byte[] msgbytes = new byte[0];
    V1LcapMessage noop_msg = null;

    noop_msg = V1LcapMessage.makeNoOpMsg(testID, testbytes);
    msgbytes = noop_msg.encodeMsg();

    V1LcapMessage msg = new V1LcapMessage(msgbytes);
    // now test to see if we got back what we started with
    assertTrue(testID == msg.m_originatorID);
    assertEquals(V1LcapMessage.NO_OP, msg.m_opcode);
    assertEquals(testbytes, msg.m_verifier);
  }

  public void testReplyMessageCreation() throws Exception {
    V1LcapMessage rep_msg = null;

    rep_msg = V1LcapMessage.makeReplyMsg(testmsg,
					 testbytes,
					 testbytes,
					 testentries,
					 V1LcapMessage.CONTENT_POLL_REP,
					 100000,
					 testID);

    // now test to see if we got back what we expected

    assertTrue(testID == rep_msg.m_originatorID);
    assertEquals(V1LcapMessage.CONTENT_POLL_REP, rep_msg.m_opcode);
    assertEquals(testmsg.m_hashAlgorithm, rep_msg.m_hashAlgorithm);
    assertEquals(testmsg.m_pluginVersion, rep_msg.m_pluginVersion);
    // TODO: figure out how to test time
    assertFalse(rep_msg.m_multicast);
    assertEquals(2, rep_msg.m_hopCount);

    assertEquals(testmsg.m_challenge, rep_msg.m_challenge);
    assertEquals(testmsg.m_verifier, rep_msg.m_verifier);
    assertEquals(testmsg.m_hashed, rep_msg.m_hashed);
    assertTrue(hasSameEntries(rep_msg.m_entries,testentries));
  }

  public void testRequestMessageCreation() throws Exception {
    V1LcapMessage req_msg = null;
    PollSpec spec =
      new MockPollSpec(archivalID, urlstr, lwrbnd, uprbnd, "Plug42",
		       Poll.V1_CONTENT_POLL);
    req_msg = V1LcapMessage.makeRequestMsg(spec,
					   testentries,
					   testbytes,
					   testbytes,
					   V1LcapMessage.CONTENT_POLL_REQ,
					   100000,
					   testID);
    assertEquals(spec.getProtocolVersion(), 1);
    assertEquals(1, req_msg.getProtocolVersion());
    assertEquals("Plug42", req_msg.getPluginVersion());
    assertTrue(testID == req_msg.m_originatorID);
    assertEquals(V1LcapMessage.CONTENT_POLL_REQ, req_msg.m_opcode);
    assertFalse(req_msg.m_multicast);
    assertEquals(archivalID, req_msg.m_archivalID);
    assertEquals(testbytes, req_msg.m_challenge);
    assertEquals(testbytes, req_msg.m_verifier);
    assertNull(req_msg.m_hashed);
    assertTrue(hasSameEntries(req_msg.m_entries,testentries));
    assertEquals(lwrbnd, req_msg.m_lwrBound);
    assertEquals(uprbnd, req_msg.m_uprBound);
  }

  public void testMessageEncoding() throws Exception {
    byte[] msgbytes = new byte[0];
    testmsg.storeProps();
    msgbytes = testmsg.encodeMsg();

    V1LcapMessage msg = new V1LcapMessage(msgbytes);
    // now test to see if we got back what we started with
    assertTrue(testID == msg.m_originatorID);
    assertEquals(V1LcapMessage.CONTENT_POLL_REQ, msg.m_opcode);
    assertFalse(msg.m_multicast);
    assertEquals(2, msg.m_hopCount);

    assertEquals(testbytes, msg.m_challenge);
    assertEquals(testbytes, msg.m_verifier);
    assertEquals(testbytes, msg.m_hashed);
    assertTrue(hasSameEntries(msg.m_entries,testentries));
    assertEquals(lwrbnd, msg.m_lwrBound);
    assertEquals(uprbnd, msg.m_uprBound);
    assertEquals("PlugVer42", msg.getPluginVersion());
  }

  public void testMessageForwarding() throws Exception {
    byte[] msgbytes = new byte[0];
    testmsg.storeProps();
    msgbytes = testmsg.encodeMsg();
    V1LcapMessage msg = new V1LcapMessage(msgbytes);
    assertEquals(2, msg.m_hopCount);
    msg.setHopCount(3);
    msg = new V1LcapMessage(msg.encodeMsg());
    // now test to see if we got back what we started with
    assertEquals(3, msg.m_hopCount);
    assertTrue(testID == msg.m_originatorID);
    assertEquals(V1LcapMessage.CONTENT_POLL_REQ, msg.m_opcode);
    assertFalse(msg.m_multicast);

    assertEquals(testbytes, msg.m_challenge);
    assertEquals(testbytes, msg.m_verifier);
    assertEquals(testbytes, msg.m_hashed);
    assertTrue(hasSameEntries(msg.m_entries,testentries));
    assertEquals(lwrbnd, msg.m_lwrBound);
    assertEquals(uprbnd, msg.m_uprBound);
    assertEquals("PlugVer42", msg.getPluginVersion());
  }

  public void testMessageEncodingHandlesAllowableNulls() throws Exception {
    testmsg.m_entries = null;
    testmsg.m_lwrBound = null;
    testmsg.m_uprBound = null;
    testmsg.m_lwrRem = null;
    testmsg.m_uprRem = null;
    testmsg.storeProps();
  }

  public void testMessageDecodingHandlesAllowableNulls() throws IOException {
    testmsg.m_entries = null;
    testmsg.m_lwrBound = null;
    testmsg.m_uprBound = null;
    testmsg.m_lwrRem = null;
    testmsg.m_uprRem = null;
    testmsg.storeProps();
    byte[] msgbytes = testmsg.encodeMsg();

    V1LcapMessage msg = null;
    msg = new V1LcapMessage(msgbytes);

    // now make sure we're still null
    assertNull(msg.m_entries);
    assertNull(msg.m_lwrBound);
    assertNull(msg.m_uprBound);
    assertNull(msg.m_lwrRem);
    assertNull(msg.m_uprRem);
  }

  public void testHopCount() {
    int max = V1LcapMessage.MAX_HOP_COUNT_LIMIT;
    testmsg.setHopCount(0);
    assertEquals(0, testmsg.getHopCount());
    testmsg.setHopCount(-40);
    assertEquals(0, testmsg.getHopCount());
    testmsg.setHopCount(max);
    assertEquals(max, testmsg.getHopCount());
    testmsg.setHopCount(max + 1);
    assertEquals(max, testmsg.getHopCount());
    testmsg.setHopCount(max - 1);
    assertEquals(max - 1, testmsg.getHopCount());
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestV1LcapMessage.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
