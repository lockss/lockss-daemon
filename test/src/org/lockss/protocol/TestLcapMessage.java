/*
 * $Id: TestLcapMessage.java,v 1.27 2003-06-20 22:34:54 claire Exp $
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

import java.io.DataInputStream;
import org.lockss.util.Logger;
import org.lockss.test.MockCachedUrlSetSpec;
import java.net.*;
import java.io.*;
import java.util.*;
import org.lockss.test.*;
import gnu.regexp.*;
import org.lockss.poller.TestPoll;
import org.lockss.poller.*;
import org.lockss.util.*;

/** JUnitTest case for class: org.lockss.protocol.Message */
public class TestLcapMessage extends LockssTestCase {

  private static String urlstr = "http://www.example.com";
  private static byte[] testbytes = {1,2,3,4,5,6,7,8,9,10};
  private static String lwrbnd = "test1.doc";
  private static String uprbnd = "test3.doc";
  private static ArrayList testentries;

  private static MockLockssDaemon daemon = new MockLockssDaemon(null);
  protected InetAddress testaddr;
  protected LcapIdentity testID;
  protected LcapMessage testmsg;
  protected static String archivalID = "TestAU_1.0";

  public TestLcapMessage(String _name) {
    super(_name);
  }


  public void setUp() throws Exception {
    super.setUp();
    try {
      testaddr = InetAddress.getByName("127.0.0.1");
    }
    catch (UnknownHostException ex) {
      fail("can't open test host");
    }
    try {
      testmsg = new LcapMessage();
    }
    catch (IOException ex) {
      fail("can't create test message");
    }
    // assign the data
    testmsg.m_targetUrl = urlstr;
    testmsg.m_lwrBound = lwrbnd;
    testmsg.m_uprBound = uprbnd;
    testID = new LcapIdentity(testaddr);

    testmsg.m_originAddr = testaddr;
    testmsg.m_hashAlgorithm = LcapMessage.getDefaultHashAlgorithm();
    testmsg.m_startTime = 123456789;
    testmsg.m_stopTime = 987654321;
    testmsg.m_multicast = false;
    testmsg.m_hopCount = 2;

    testmsg.m_ttl = 5;
    testmsg.m_challenge = testbytes;
    testmsg.m_verifier = testbytes;
    testmsg.m_hashed = testbytes;
    testmsg.m_opcode = LcapMessage.CONTENT_POLL_REQ;
    testmsg.m_entries = testentries = TestPoll.makeEntries(1, 25);
    testmsg.m_archivalID = archivalID;

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

  public void testNoOpMessageCreation() {
    LcapMessage noop_msg = null;

    try {
      noop_msg = LcapMessage.makeNoOpMsg(testID, testbytes);

    }
    catch(IOException ex) {
      fail("noop message creation failed");
    }

    // now check the fields we expect to be valid
    assertEquals(noop_msg.m_originAddr, testaddr);
    assertEquals(noop_msg.m_opcode,LcapMessage.NO_OP);
    assertEquals(noop_msg.m_verifier, testbytes);
  }

  public void testNoOpMessageToString() throws IOException {
    LcapMessage noop_msg = LcapMessage.makeNoOpMsg(testID, testbytes);

    noop_msg.toString();
  }

  public void testNoOpEncoding() {

    byte[] msgbytes = new byte[0];
    LcapMessage noop_msg = null;

    try {
      noop_msg = LcapMessage.makeNoOpMsg(testID, testbytes);
    }
    catch(IOException ex) {
      fail("noop message creation failed");
    }
    try {
      msgbytes = noop_msg.encodeMsg();
    }
    catch (IOException ex) {
      fail("encode failed!" + ex.toString());
    }

    try {
      LcapMessage msg = new LcapMessage(msgbytes);
      // now test to see if we got back what we started with
      assertEquals(testaddr, msg.m_originAddr);
      assertEquals(LcapMessage.NO_OP, msg.m_opcode);
      assertEquals(testbytes, msg.m_verifier);
    }
    catch (IOException ex) {
      fail("message decode failed");
    }
  }

  public void testReplyMessageCreation() {
    LcapMessage rep_msg = null;

    try {
      rep_msg = LcapMessage.makeReplyMsg(testmsg,
                                         testbytes,
                                         testbytes,
                                         testentries,
                                         LcapMessage.CONTENT_POLL_REP,
                                         100000,
                                         testID);
    }
    catch(IOException ex) {
      fail("message reply creation failed");
    }

    // now test to see if we got back what we expected

    assertEquals(rep_msg.m_originAddr, testaddr);
    assertEquals(rep_msg.m_ttl,5);
    assertEquals(rep_msg.m_opcode,LcapMessage.CONTENT_POLL_REP);
    assertEquals(rep_msg.m_hashAlgorithm,testmsg.m_hashAlgorithm);
    // TODO: figure out how to test time
    assertEquals(rep_msg.m_multicast ,false);
    assertEquals(2, rep_msg.m_hopCount);

    assertTrue(Arrays.equals(rep_msg.m_challenge,testmsg.m_challenge));
    assertTrue(Arrays.equals(rep_msg.m_verifier,testmsg.m_verifier));
    assertTrue(Arrays.equals(rep_msg.m_hashed,testmsg.m_hashed));
    assertTrue(hasSameEntries(rep_msg.m_entries,testentries));

  }

  public void testRequestMessageCreation() {
    LcapMessage req_msg = null;
    try {
      PollSpec spec = new PollSpec(archivalID,urlstr, lwrbnd, uprbnd,null);
      assertEquals(spec.getVersion(), 1);
      req_msg = LcapMessage.makeRequestMsg(spec,
                                           testentries,
                                           testbytes,
                                           testbytes,
                                           LcapMessage.CONTENT_POLL_REQ,
                                           100000,
                                           testID);
      assertEquals(req_msg.getVersion(), 1);
    }
    catch (Exception ex) {
      fail("message request creation failed.");
    }
    assertEquals(req_msg.m_originAddr, testaddr);
    assertEquals(req_msg.m_opcode,LcapMessage.CONTENT_POLL_REQ);
    assertEquals(req_msg.m_multicast ,false);
    assertEquals(req_msg.m_archivalID, archivalID);
    assertTrue(Arrays.equals(req_msg.m_challenge,testbytes));
    assertTrue(Arrays.equals(req_msg.m_verifier,testbytes));
    assertEquals(null,req_msg.m_hashed);
    assertTrue(hasSameEntries(req_msg.m_entries,testentries));
    assertEquals(req_msg.m_lwrBound, lwrbnd);
    assertEquals(req_msg.m_uprBound, uprbnd);

  }

  public void testMessageEncoding() {

    byte[] msgbytes = new byte[0];
    try {
      msgbytes = testmsg.encodeMsg();
    }
    catch (IOException ex) {
      fail("encode failed! " + ex.toString());
    }

    try {
      LcapMessage msg = new LcapMessage(msgbytes);
      // now test to see if we got back what we started with
      assertEquals(msg.m_originAddr, testaddr);
      assertEquals(msg.m_ttl,5);
      assertEquals(msg.m_opcode,LcapMessage.CONTENT_POLL_REQ);
      // TODO: figure out how to test time
      assertEquals(msg.m_multicast ,false);
      assertEquals(2, msg.m_hopCount);

      assertTrue(Arrays.equals(msg.m_challenge,testbytes));
      assertTrue(Arrays.equals(msg.m_verifier,testbytes));
      assertTrue(Arrays.equals(msg.m_hashed,testbytes));
      assertTrue(hasSameEntries(msg.m_entries,testentries));
      assertEquals(msg.m_lwrBound, lwrbnd);
      assertEquals(msg.m_uprBound, uprbnd);
    }
    catch (IOException ex) {
      fail("message decode failed");
    }
  }

  public void testMessageEncodingHandlesAllowableNulls(){
    testmsg.m_entries = null;
    testmsg.m_lwrBound = null;
    testmsg.m_uprBound = null;
    testmsg.m_lwrRem = null;
    testmsg.m_uprRem = null;
    try {
      testmsg.encodeMsg();
    }
    catch (IOException ex) {
      assertTrue("message encode with nulls failed!", true);
    }

  }

  public void testMessageDecodingHandlesAllowableNulls() throws IOException {
    testmsg.m_entries = null;
    testmsg.m_lwrBound = null;
    testmsg.m_uprBound = null;
    testmsg.m_lwrRem = null;
    testmsg.m_uprRem = null;
    byte[] msgbytes = testmsg.encodeMsg();

    LcapMessage msg = null;
    try {
      msg = new LcapMessage(msgbytes);
    }
    catch (IOException ex) {
      assertTrue("message decode with nulls failed!", true);
    }

    // now make sure we're still null
    assertNull(msg.m_entries);
    assertNull(msg.m_lwrBound);
    assertNull(msg.m_uprBound);
    assertNull(msg.m_lwrRem);
    assertNull(msg.m_uprRem);

  }

  public void testHopCount() {
    int max = LcapMessage.MAX_HOP_COUNT_LIMIT;
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
    String[] testCaseList = {TestLcapMessage.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}

